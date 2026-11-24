package com.mythiccontracts.skill.remnant;

import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DivinePhaseProgressionTest {

    @Test
    void noviceStartsContainedInPhaseOneOnly() {
        PlayerData novice = new PlayerData();
        EnergyService.recalculateMaxQi(novice);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_1, DivineResonanceBalance.getManifestationTier(novice));
        assertEquals(0.40F, DivineResonanceBalance.getManifestationTier(novice).getPowerMultiplier(), 0.001F);
        assertEquals(60.0F, DivineResonanceBalance.calculateDivineSkillCost(novice), 0.01F);
        assertEquals(1, DivineResonanceBalance.getUnlockedPhaseCount(novice));
        assertTrue(DivineResonanceBalance.isPhaseUnlocked(novice, 0));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(novice, 1));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(novice, 2));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(novice, 3));
    }

    @Test
    void antiRushFloorBlocksPhaseUpgradeWhenEitherPathStaysAtOrigin() {
        PlayerData bodyRushed = new PlayerData();
        CultivationService.setRealmAndStageByOrder(bodyRushed, CultivationPath.BODY, 6, 1);
        CultivationService.setRealmAndStageByOrder(bodyRushed, CultivationPath.SPIRIT, 1, 1);
        EnergyService.recalculateMaxQi(bodyRushed);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_1, DivineResonanceBalance.getManifestationTier(bodyRushed));
        assertEquals(1, DivineResonanceBalance.getUnlockedPhaseCount(bodyRushed));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(bodyRushed, 1));

        PlayerData spiritRushed = new PlayerData();
        CultivationService.setRealmAndStageByOrder(spiritRushed, CultivationPath.BODY, 1, 1);
        CultivationService.setRealmAndStageByOrder(spiritRushed, CultivationPath.SPIRIT, 6, 1);
        EnergyService.recalculateMaxQi(spiritRushed);

        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_1, DivineResonanceBalance.getManifestationTier(spiritRushed));
        assertEquals(1, DivineResonanceBalance.getUnlockedPhaseCount(spiritRushed));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(spiritRushed, 1));
    }

    @Test
    void phasesUnlockProgressivelyAcrossCultivationMilestones() {
        PlayerData data = new PlayerData();

        // Média abaixo de R2-3 (2.33 < 2.65) permanece na Fase I
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 2, 3);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 2, 1);
        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_1, DivineResonanceBalance.getManifestationTier(data));
        assertEquals(1, DivineResonanceBalance.getUnlockedPhaseCount(data));

        // Phase II: Score R2-3 (>= 2.65) + piso mínimo R2-1 (ex.: R3-2 + R2-1 ou R2-3 + R2-3)
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 3, 2);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 2, 1);
        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_2, DivineResonanceBalance.getManifestationTier(data));
        assertEquals(0.75F, DivineResonanceBalance.getManifestationTier(data).getPowerMultiplier(), 0.001F);
        assertEquals(2, DivineResonanceBalance.getUnlockedPhaseCount(data));
        assertTrue(DivineResonanceBalance.isPhaseUnlocked(data, 1));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(data, 2));

        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 2, 3);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 2, 3);
        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_2, DivineResonanceBalance.getManifestationTier(data));

        // Phase III: Score R4-2 (>= 4.30) + piso mínimo R3-2
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 5, 2);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 3, 2);
        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_3, DivineResonanceBalance.getManifestationTier(data));
        assertEquals(1.10F, DivineResonanceBalance.getManifestationTier(data).getPowerMultiplier(), 0.001F);
        assertEquals(3, DivineResonanceBalance.getUnlockedPhaseCount(data));
        assertTrue(DivineResonanceBalance.isPhaseUnlocked(data, 2));
        assertFalse(DivineResonanceBalance.isPhaseUnlocked(data, 3));

        // Phase IV: Score R6-1 (>= 6.00) + piso mínimo R5-1
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 7, 1);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 5, 1);
        assertEquals(DivineResonanceBalance.ManifestationTier.TIER_4, DivineResonanceBalance.getManifestationTier(data));
        assertEquals(1.45F, DivineResonanceBalance.getManifestationTier(data).getPowerMultiplier(), 0.001F);
        assertEquals(4, DivineResonanceBalance.getUnlockedPhaseCount(data));
        assertTrue(DivineResonanceBalance.isPhaseUnlocked(data, 3));
    }

    @Test
    void allSixteenDivinePhaseAbilitiesExistAndAreDistinct() {
        Set<String> seenNames = new HashSet<>();
        int totalCount = 0;

        for (DivineRemnant remnant : DivineRemnant.values()) {
            List<DivinePhaseAbility> abilities = DivinePhaseAbility.getAllFor(remnant);
            assertEquals(4, abilities.size());
            for (int phase = 0; phase < 4; phase++) {
                DivinePhaseAbility ability = DivinePhaseAbility.get(remnant, phase);
                assertNotNull(ability);
                assertEquals(remnant, ability.remnant());
                assertEquals(phase, ability.phaseIndex());
                assertEquals("H" + (phase + 1), ability.code());
                assertNotNull(ability.displayName());
                assertFalse(ability.displayName().isBlank());
                assertNotNull(ability.summary());
                assertFalse(ability.summary().isBlank());
                assertTrue(ability.cooldownMultiplier() > 0.0F);
                assertTrue(seenNames.add(ability.displayName()), "Duplicate divine ability name: " + ability.displayName());
                totalCount++;
            }
        }

        assertEquals(16, totalCount);
    }

    @Test
    void phaseFourUltimateConsumesSixtyFivePercentOfMaxDivineBar() {
        PlayerData data = new PlayerData();
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 6, 1);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 6, 1);
        EnergyService.recalculateMaxQi(data);

        float maxResistance = DivineResonanceBalance.calculateMaxDivineResistance(data);
        data.getSkills().setCurrentDivineResistance(maxResistance);

        float expectedCost = Math.round(maxResistance * 0.65F);
        float actualCost = DivineResonanceBalance.calculateDivinePhaseSkillCost(data, 3);
        assertEquals(expectedCost, actualCost, 0.01F);

        EnergyService.DivineSpendOutcome outcome = EnergyService.consumeDivineResistanceOrOverload(data, 3);
        assertEquals(EnergyService.DivineSpendOutcome.SUCCESS, outcome);
        assertFalse(data.getSkills().isDivineOverloaded());
        assertEquals(maxResistance - actualCost, data.getSkills().getCurrentDivineResistance(), 0.1F);
    }
}
