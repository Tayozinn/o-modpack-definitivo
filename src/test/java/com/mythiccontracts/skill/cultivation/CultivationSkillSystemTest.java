package com.mythiccontracts.skill.cultivation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.core.data.DatapackValidationService;
import com.mythiccontracts.core.player.PlayerData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CultivationSkillSystemTest {

    private static final Path SKILLS_DIR = Path.of("src/main/resources/data/mythic_contracts/cultivation_skills");

    @AfterEach
    void resetRegistry() {
        CultivationSkillRegistry.resetToBuiltins();
    }

    @Test
    void allTwelveCultivationSkillJsonsParseCleanly() throws IOException {
        List<Path> jsonFiles;
        try (Stream<Path> walk = Files.list(SKILLS_DIR)) {
            jsonFiles = walk.filter(p -> p.toString().endsWith(".json")).toList();
        }
        assertEquals(12, jsonFiles.size(), "Expected exactly 12 R1-R7 cultivation skill JSONs");

        for (Path file : jsonFiles) {
            String name = file.getFileName().toString().replace(".json", "");
            ResourceLocation fileId = new ResourceLocation("mythic_contracts", name);
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            CultivationSkillDefinition parsed = CultivationSkillReloadListener.parseSkill(fileId, root);
            assertNotNull(parsed);
            assertTrue(parsed.enabled(), "Skill JSON should be enabled: " + name);
            assertTrue(parsed.tierRealm() >= 1 && parsed.tierRealm() <= 7);
        }
    }

    @Test
    void martialSlotsUnlockAtR1R3AndR5() {
        PlayerData data = new PlayerData();
        setRealms(data, 1, 1);
        assertEquals(1, CultivationSkillService.getUnlockedMartialSlotCount(data));

        setRealms(data, 3, 1);
        assertEquals(2, CultivationSkillService.getUnlockedMartialSlotCount(data));

        setRealms(data, 2, 5);
        assertEquals(3, CultivationSkillService.getUnlockedMartialSlotCount(data));
    }

    @Test
    void validateUnlockEnforcesRequirementsAndThroneBreakerBypass() {
        CultivationSkillDefinition subSkill = CultivationSkillRegistry.getSkill(
                new ResourceLocation("mythic_contracts", "soul_subjugation"));
        assertNotNull(subSkill);

        PlayerData outsiderR1 = new PlayerData();
        setRealms(outsiderR1, 1, 1);
        outsiderR1.getCultivation().setMaxQiCapacity(200.0F);
        outsiderR1.getCultivation().setCurrentQi(200.0F);
        assertFalse(CultivationSkillService.validateUnlock(outsiderR1, subSkill).allowed());

        // Quebradores de Tronos ignoram requisito R2 na tecnica de subjugacao
        PlayerData throneBreaker = new PlayerData();
        throneBreaker.getFaction().setCurrentFactionId(
                new ResourceLocation("mythic_contracts", FactionType.QUEBRADORES_DE_TRONOS.getId()));
        setRealms(throneBreaker, 1, 1);
        throneBreaker.getCultivation().setMaxQiCapacity(200.0F);
        throneBreaker.getCultivation().setCurrentQi(200.0F);
        assertTrue(CultivationSkillService.validateUnlock(throneBreaker, subSkill).allowed());
    }

    private static void setRealms(PlayerData data, int bodyRealm, int spiritRealm) {
        data.getCultivation().setBodyRealm(bodyRealm);
        data.getCultivation().setBodyRealmId(new ResourceLocation("mythic_contracts", "body_realm_" + bodyRealm));
        data.getCultivation().setBodyStage(1);
        data.getCultivation().setSpiritRealm(spiritRealm);
        data.getCultivation().setSpiritRealmId(new ResourceLocation("mythic_contracts", "spirit_realm_" + spiritRealm));
        data.getCultivation().setSpiritStage(1);
    }

    @Test
    void datapackValidationPreservesDivineSkillCountInvariant() {
        DatapackValidationService.ValidationReport report = DatapackValidationService.validateAll();
        assertEquals(4, report.skillCount(), "ProgressionBalanceRegressionTest requires skillCount == 4 (Divine Remnants)");
        assertTrue(report.errors().isEmpty(), "Unexpected datapack validation errors: " + report.errors());
    }
}
