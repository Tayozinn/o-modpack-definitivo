package com.mythiccontracts.skill.remnant;

import com.mythiccontracts.core.persistence.PlayerDataSerializer;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DivineResonanceBalanceTest {

    @Test
    void resistanceCapacityPowerAndCostScaleInverselyWithCultivation() {
        PlayerData novice = new PlayerData();
        EnergyService.recalculateMaxQi(novice);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_1, DivineResonanceBalance.getManifestationTier(novice));
        assertEquals(100.0F, DivineResonanceBalance.calculateMaxDivineResistance(novice), 0.01F);
        assertEquals(55.0F, DivineResonanceBalance.calculateDivineSkillCost(novice), 0.01F);
        assertEquals(0.60F, DivineResonanceBalance.getManifestationTier(novice).getPowerMultiplier(), 0.001F);

        PlayerData mid = new PlayerData();
        CultivationService.setRealmAndStageByOrder(mid, CultivationPath.BODY, 4, 2);
        CultivationService.setRealmAndStageByOrder(mid, CultivationPath.SPIRIT, 4, 2);
        EnergyService.recalculateMaxQi(mid);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_3, DivineResonanceBalance.getManifestationTier(mid));
        assertEquals(320.0F, DivineResonanceBalance.calculateMaxDivineResistance(mid), 0.01F);
        assertTrue(DivineResonanceBalance.calculateDivineSkillCost(mid) < 55.0F);

        PlayerData apex = new PlayerData();
        CultivationService.setRealmAndStageByOrder(apex, CultivationPath.BODY, 7, 3);
        CultivationService.setRealmAndStageByOrder(apex, CultivationPath.SPIRIT, 7, 3);
        EnergyService.recalculateMaxQi(apex);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_4, DivineResonanceBalance.getManifestationTier(apex));
        assertEquals(540.0F, DivineResonanceBalance.calculateMaxDivineResistance(apex), 0.01F);
        assertEquals(20.0F, DivineResonanceBalance.calculateDivineSkillCost(apex), 0.01F);
        assertEquals(1.45F, DivineResonanceBalance.getManifestationTier(apex).getPowerMultiplier(), 0.001F);
    }

    @Test
    void zeroingDivineResistanceTriggersOneMinuteOverloadAndRecoversAfterward() {
        PlayerData data = new PlayerData();
        EnergyService.recalculateMaxQi(data);

        // 1º uso no R1-1: 100 - 55 = 45 (sem sobrecarga)
        assertEquals(EnergyService.DivineSpendOutcome.SUCCESS, EnergyService.consumeDivineResistanceOrOverload(data));
        assertEquals(45.0F, data.getSkills().getCurrentDivineResistance(), 0.01F);
        assertFalse(data.getSkills().isDivineOverloaded());

        // 2º uso seguido: 45 - 55 -> zera e dispara Sobrecarga Divina (1200 ticks = 1 minuto)
        assertEquals(EnergyService.DivineSpendOutcome.SUCCESS_TRIGGERED_OVERLOAD, EnergyService.consumeDivineResistanceOrOverload(data));
        assertEquals(0.0F, data.getSkills().getCurrentDivineResistance(), 0.01F);
        assertTrue(data.getSkills().isDivineOverloaded());
        assertEquals(1200, data.getSkills().getDivineOverloadTicks());

        // Enquanto em sobrecarga, bloqueia novos usos e não regenera a barra
        assertEquals(EnergyService.DivineSpendOutcome.BLOCKED_OVERLOAD, EnergyService.consumeDivineResistanceOrOverload(data));
        for (int i = 0; i < 1199; i++) {
            assertFalse(EnergyService.tickAllEnergy(data).divineOverloadEnded());
        }
        assertEquals(1, data.getSkills().getDivineOverloadTicks());
        assertEquals(0.0F, data.getSkills().getCurrentDivineResistance(), 0.01F);

        // No tick 1200, termina a sobrecarga e recupera 35% da barra máxima
        assertTrue(EnergyService.tickAllEnergy(data).divineOverloadEnded());
        assertFalse(data.getSkills().isDivineOverloaded());
        assertEquals(35.0F, data.getSkills().getCurrentDivineResistance(), 0.01F);
    }

    @Test
    void divineResistanceAndOverloadPersistThroughNbtRoundTrip() {
        PlayerData data = new PlayerData();
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 3, 2);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 3, 2);
        EnergyService.recalculateMaxQi(data);
        data.getSkills().setCurrentDivineResistance(77.5F);
        data.getSkills().setDivineOverloadTicks(450);

        CompoundTag saved = PlayerDataSerializer.writeFull(data);
        PlayerData loaded = new PlayerData();
        PlayerDataSerializer.read(saved, loaded);

        assertEquals(data.getSkills().getMaxDivineResistance(), loaded.getSkills().getMaxDivineResistance(), 0.01F);
        assertEquals(77.5F, loaded.getSkills().getCurrentDivineResistance(), 0.01F);
        assertEquals(450, loaded.getSkills().getDivineOverloadTicks());
    }
}
