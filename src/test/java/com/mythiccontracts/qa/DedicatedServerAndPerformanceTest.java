package com.mythiccontracts.qa;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DedicatedServerAndPerformanceTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java/com/mythiccontracts");

    @Test
    void dedicatedServerPackagesContainZeroClientImportsAndSafeDistExecutors() throws IOException {
        try (var stream = Files.walk(JAVA_ROOT)) {
            List<Path> serverFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        String norm = p.toString().replace('\\', '/');
                        return !norm.contains("/client/") && !norm.endsWith("/network/ClientPacketHandler.java");
                    })
                    .toList();

            assertFalse(serverFiles.isEmpty());
            for (Path file : serverFiles) {
                String src = Files.readString(file, StandardCharsets.UTF_8);
                assertFalse(src.contains("import net.minecraft.client."),
                        "Client import leaked into server-visible file: " + file);
                assertFalse(src.contains("import com.mojang.blaze3d."),
                        "Blaze3D import leaked into server-visible file: " + file);
            }
        }

        // Method references inside DistExecutor can trigger server classloading via bootstrap args; enforce lambdas
        for (Path guarded : List.of(
                JAVA_ROOT.resolve("MythicContractsMod.java"),
                JAVA_ROOT.resolve("item/SubjugationSealItem.java")
        )) {
            String src = Files.readString(guarded, StandardCharsets.UTF_8);
            assertFalse(src.contains("ClientBootstrap::"),
                    "Unsafe method reference ClientBootstrap:: in " + guarded);
            assertFalse(src.contains("ClientPacketHandler::"),
                    "Unsafe method reference ClientPacketHandler:: in " + guarded);
        }
    }

    @Test
    void adminDebugCommandsRequirePermissionLevel2() throws IOException {
        String cmdSrc = Files.readString(JAVA_ROOT.resolve("command/ContractDebugCommand.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\\.requires\\(source -> source\\.hasPermission\\(2\\)\\)").matcher(cmdSrc);
        int count = 0;
        while (m.find()) {
            count++;
        }
        assertEquals(7, count, "Expected OP level 2 permission guard on all 7 mutating /contrato subcommands");
        assertTrue(cmdSrc.contains("Commands.literal(\"status\")"));
        assertTrue(cmdSrc.contains("Commands.literal(\"compat\")"));
    }

    @Test
    void energyTickHotLoopMeetsMultiplayerPerformanceBudgetAndMatchesStandaloneTicks() {
        RealmRegistry.resetToBuiltins();
        ContractManager.resetToBuiltins();

        // Equivalence check between EnergyService.tickAllEnergy and standalone services
        PlayerData combined = new PlayerData();
        PlayerData standalone = new PlayerData();
        for (PlayerData d : List.of(combined, standalone)) {
            CultivationService.setRealmAndStageByOrder(d, CultivationPath.BODY, 4, 2);
            CultivationService.setRealmAndStageByOrder(d, CultivationPath.SPIRIT, 4, 2);
            EnergyService.recalculateMaxQi(d);
            d.getCultivation().setCurrentStamina(40.0F);
            d.getCultivation().setCurrentSpiritualIntegrity(50.0F);
        }

        for (int t = 0; t < 60; t++) {
            boolean sprint = t < 20;
            EnergyService.tickAllEnergy(combined, sprint);
            EnergyService.recalculateMaxQi(standalone);
            VitalService.tickStamina(standalone, sprint);
            SpiritualIntegrityService.tickSpiritualIntegrity(standalone);
        }

        assertEquals(standalone.getCultivation().getCurrentStamina(), combined.getCultivation().getCurrentStamina(), 1.0E-4F);
        assertEquals(standalone.getCultivation().getMaxStamina(), combined.getCultivation().getMaxStamina(), 1.0E-4F);
        assertEquals(standalone.getCultivation().getCurrentSpiritualIntegrity(), combined.getCultivation().getCurrentSpiritualIntegrity(), 1.0E-4F);
        assertEquals(standalone.getCultivation().getMaxSpiritualIntegrity(), combined.getCultivation().getMaxSpiritualIntegrity(), 1.0E-4F);

        // Multiplayer stress benchmark: 50 players x 400 ticks = 20,000 player-ticks
        final int playerCount = 50;
        final int ticksPerPlayer = 400;
        PlayerData[] players = new PlayerData[playerCount];
        ResourceLocation veilOath = new ResourceLocation(MythicContractsMod.MOD_ID, "veil_resonance_oath");

        for (int i = 0; i < playerCount; i++) {
            PlayerData pd = new PlayerData();
            CultivationService.setRealmAndStageByOrder(pd, CultivationPath.BODY, (i % 7) + 1, (i % 3) + 1);
            CultivationService.setRealmAndStageByOrder(pd, CultivationPath.SPIRIT, ((i + 2) % 7) + 1, (i % 3) + 1);
            if (i % 2 == 0) {
                pd.getContracts().addContract(veilOath);
            }
            EnergyService.recalculateMaxQi(pd);
            pd.getCultivation().setManaInfusionActive(i % 3 == 0);
            if (i % 5 == 0) {
                pd.getSkills().setDivineOverloadTicks(DivineResonanceBalance.OVERLOAD_PENALTY_TICKS);
            }
            if (i % 7 == 0) {
                pd.getCultivation().setSpiritShatterTicks(SpiritualIntegrityService.SPIRIT_SHATTER_TICKS);
            }
            players[i] = pd;
        }

        // Warmup JIT
        for (int t = 0; t < 20; t++) {
            for (PlayerData pd : players) {
                EnergyService.tickAllEnergy(pd, (t & 1) == 0);
            }
        }

        long startNanos = System.nanoTime();
        for (int t = 0; t < ticksPerPlayer; t++) {
            boolean sprinting = (t % 4) == 0;
            for (PlayerData pd : players) {
                EnergyService.tickAllEnergy(pd, sprinting);
            }
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        System.out.println("BENCHMARK_20K_TICKS_MS=" + elapsedMs);

        assertTrue(elapsedMs < 1000L, "20,000 energy ticks exceeded 1000ms budget: " + elapsedMs + "ms");
        for (PlayerData pd : players) {
            assertTrue(Float.isFinite(pd.getCultivation().getCurrentQi()));
            assertTrue(Float.isFinite(pd.getCultivation().getCurrentStamina()));
            assertTrue(Float.isFinite(pd.getCultivation().getCurrentSpiritualIntegrity()));
            assertTrue(Float.isFinite(pd.getSkills().getCurrentDivineResistance()));
        }
    }
}
