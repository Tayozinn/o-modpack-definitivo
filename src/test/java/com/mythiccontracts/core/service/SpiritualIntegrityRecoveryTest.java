package com.mythiccontracts.core.service;

import com.mythiccontracts.contract.ContractDefinition;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpiritualIntegrityRecoveryTest {

    @AfterEach
    void restoreBuiltins() {
        SpiritualIntegrityBalanceConfig.resetToDefaults();
        ContractManager.resetToBuiltins();
    }

    @Test
    void quadraticRealmCurveAndStageScalingMatchApprovedSpec() {
        double[] expected = { 3.10, 5.48, 8.22, 11.32, 14.78, 18.60, 22.78 };
        PlayerData data = new PlayerData();
        for (int realm = 1; realm <= 7; realm++) {
            setSpirit(data, realm, 1);
            assertEquals(expected[realm - 1], SpiritualIntegrityService.calculateBaseOutOfCombatRegen(data), 0.02F,
                    "Mismatch at spirit realm R" + realm);
        }

        // +0.65/s por estagio espiritual acima do estagio 1
        setSpirit(data, 4, 3);
        assertEquals(11.32F + 2 * 0.65F, SpiritualIntegrityService.calculateBaseOutOfCombatRegen(data), 0.02F);
    }

    private static void setSpirit(PlayerData data, int realm, int stage) {
        data.getCultivation().setSpiritRealm(realm);
        data.getCultivation().setSpiritRealmId(new ResourceLocation("mythic_contracts", "spirit_realm_" + realm));
        data.getCultivation().setSpiritStage(stage);
    }

    @Test
    void threeStateMultipliersRespondToShatterCombatAndMeditation() {
        PlayerData data = new PlayerData();
        CultivationData cult = data.getCultivation();
        setSpirit(data, 1, 1);
        cult.setCurrentSpiritualIntegrity(50.0F);

        // 1. Shatter -> 0x
        cult.setSpiritShatterTicks(100);
        assertEquals(SpiritRegenState.BLOCKED_SHATTER, SpiritualIntegrityService.evaluateRegenState(data));
        assertEquals(0.0F, SpiritualIntegrityService.calculateStateMultiplier(data), 0.001F);

        // 2. Combate -> 0.40x
        cult.setSpiritShatterTicks(0);
        cult.setSpiritCombatCooldownTicks(100);
        assertEquals(SpiritRegenState.COMBAT_SLOW, SpiritualIntegrityService.evaluateRegenState(data));
        assertEquals(0.40F, SpiritualIntegrityService.calculateStateMultiplier(data), 0.001F);

        // 3. Fora de combate -> 1.00x
        cult.setSpiritCombatCooldownTicks(0);
        assertEquals(SpiritRegenState.RECOVERING, SpiritualIntegrityService.evaluateRegenState(data));
        assertEquals(1.00F, SpiritualIntegrityService.calculateStateMultiplier(data), 0.001F);

        // 4. Meditacao passiva (40t agachado parado) -> 2.20x
        cult.setStillMeditationTicks(40);
        assertEquals(SpiritRegenState.MEDITATING, SpiritualIntegrityService.evaluateRegenState(data));
        assertEquals(2.20F, SpiritualIntegrityService.calculateStateMultiplier(data), 0.001F);

        // 5. Cheio -> FULL
        cult.setCurrentSpiritualIntegrity(cult.getMaxSpiritualIntegrity());
        assertEquals(SpiritRegenState.FULL, SpiritualIntegrityService.evaluateRegenState(data));
    }

    @Test
    void antiExploitClampsCapFlatAndPercentageModifiers() {
        ResourceLocation opContractId = new ResourceLocation("mythic_contracts", "test_regen_cap");
        ContractDefinition opContract = new ContractDefinition(
                opContractId, "Teste Cap", "", Set.of(), Set.of(), List.of(),
                0.0D, -1, false, 1, 1, 0, 0, 0.0F, 0.0F, "",
                500.0F, // excede teto +50% do base
                0.50F
        );
        ContractManager.setLoadedContracts(Map.of(opContractId, opContract));

        PlayerData data = new PlayerData();
        setSpirit(data, 1, 1);
        data.getCultivation().setCurrentSpiritualIntegrity(40.0F);
        data.getContracts().addContract(opContractId, 0L, 100);

        float baseOut = SpiritualIntegrityService.calculateBaseOutOfCombatRegen(data);
        float maxWithFlatCap = baseOut * 1.50F;
        // Contrato da +50% pct (1.50x)
        assertEquals(maxWithFlatCap * 1.50F, SpiritualIntegrityService.calculateSpiritualRegenPerSecond(data), 0.02F);

        // Percentual clamped em [0.25x, 2.25x]: (1.0 + 0.50 - 0.75) = 0.75 e negativo adicional clamped
        ContractManager.setLoadedContracts(Map.of(opContractId, new ContractDefinition(
                opContractId, "Teste Cap Low", "", Set.of(), Set.of(), List.of(),
                0.0D, -1, false, 1, 1, 0, 0, 0.0F, 0.0F, "", 500.0F, -0.80F
        )));
        data.getCultivation().setTransientSpiritRegenPctModifier(-0.75F);
        assertEquals(maxWithFlatCap * 0.25F, SpiritualIntegrityService.calculateSpiritualRegenPerSecond(data), 0.02F);

        ContractManager.setLoadedContracts(Map.of(opContractId, opContract));
        data.getCultivation().setTransientSpiritRegenPctModifier(8.0F);
        assertEquals(maxWithFlatCap * 2.25F, SpiritualIntegrityService.calculateSpiritualRegenPerSecond(data), 0.05F);
    }

    @Test
    void energyTickAdvancesMeditationCounterAndDecrementsCombatCooldown() {
        PlayerData data = new PlayerData();
        CultivationData cult = data.getCultivation();
        cult.setCurrentSpiritualIntegrity(40.0F);
        cult.setSpiritCombatCooldownTicks(5);

        // Primeiros 4 ticks ainda estao sob cooldown de combate; apos 44 ticks atinge 40t parado
        for (int i = 0; i < 40; i++) {
            EnergyService.tickAllEnergy(data, false, true);
        }
        assertEquals(0, cult.getSpiritCombatCooldownTicks());
        assertEquals(36, cult.getStillMeditationTicks());

        for (int i = 0; i < 4; i++) {
            EnergyService.tickAllEnergy(data, false, true);
        }
        assertEquals(40, cult.getStillMeditationTicks());
        assertEquals(SpiritRegenState.MEDITATING, SpiritualIntegrityService.evaluateRegenState(data));
        assertTrue(cult.getCurrentSpiritualIntegrity() > 40.0F);
    }
}
