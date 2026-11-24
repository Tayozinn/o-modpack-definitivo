package com.mythiccontracts.qa;

import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.data.DatapackValidationService;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.faction.FactionRegistry;
import com.mythiccontracts.integration.CombatCompatService;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionBalanceRegressionTest {

    @AfterEach
    void restoreBuiltins() {
        RealmRegistry.resetToBuiltins();
        FactionRegistry.resetToBuiltins();
        ContractManager.resetToBuiltins();
        DivineSkillRegistry.resetToBuiltins();
    }

    @Test
    void bodyAndSpiritCurvesScaleMonotonicallyAcrossAll21Tiers() {
        PlayerData data = new PlayerData();
        double prevAtk = 0.0D;
        double prevHealthBonus = -1.0D;
        float prevInfMult = 0.0F;
        float prevSpiritFlat = 0.0F;
        float prevMaxQi = 0.0F;
        float prevQiRegen = 0.0F;
        float prevMaxStamina = 0.0F;
        float prevMaxIntegrity = 0.0F;
        float prevMaxDivine = 0.0F;
        float prevSkillCost = Float.MAX_VALUE;
        int prevCooldown = Integer.MAX_VALUE;

        for (int realm = 1; realm <= 7; realm++) {
            float qiRegen = CultivationBalance.calculateQiRegenPerSecond(realm, 1);
            int cooldown = CultivationBalance.calculateUniqueSkillCooldownTicks(realm);
            assertTrue(qiRegen > prevQiRegen, "Qi regen should strictly increase at realm " + realm);
            assertTrue(cooldown <= prevCooldown, "Unique skill cooldown should never increase at realm " + realm);
            prevCooldown = cooldown;

            for (int stage = 1; stage <= 3; stage++) {
                assertTrue(CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, realm, stage));
                assertTrue(CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, realm, stage));
                EnergyService.recalculateMaxQi(data);

                double atk = CultivationBalance.calculateBodyPassiveAttackBonus(realm, stage);
                double healthBonus = RealmRegistry.getRealmByOrder(CultivationPath.BODY, realm)
                        .getStageByNumber(stage)
                        .attributes()
                        .stream()
                        .filter(a -> a.attributeId().getPath().contains("max_health"))
                        .mapToDouble(a -> a.amount())
                        .findFirst()
                        .orElse(0.0D);
                float infMult = CultivationBalance.calculateBodyInfusionMultiplier(data);
                float spiritFlat = CultivationBalance.calculateSpiritInfusionFlatBonus(data);
                float maxQi = data.getCultivation().getMaxQiCapacity();
                float stageQiRegen = CultivationBalance.calculateQiRegenPerSecond(realm, stage);
                float maxStamina = VitalService.calculateMaxStamina(data);
                float maxIntegrity = SpiritualIntegrityService.calculateMaxSpiritualIntegrity(data);
                float maxDivine = DivineResonanceBalance.calculateMaxDivineResistance(data);
                float skillCost = DivineResonanceBalance.calculateDivineSkillCost(data);

                assertTrue(atk > prevAtk, "Attack must increase at R" + realm + "-" + stage);
                assertTrue(healthBonus >= prevHealthBonus, "Health bonus must be non-decreasing at R" + realm + "-" + stage);
                assertTrue(infMult > prevInfMult, "Body infusion mult must increase at R" + realm + "-" + stage);
                assertTrue(spiritFlat > prevSpiritFlat, "Spirit infusion flat must increase at R" + realm + "-" + stage);
                assertTrue(maxQi > prevMaxQi, "Max Qi must increase at R" + realm + "-" + stage);
                assertTrue(stageQiRegen > prevQiRegen, "Stage Qi regen must increase at R" + realm + "-" + stage);
                assertTrue(maxStamina > prevMaxStamina, "Max stamina must increase at R" + realm + "-" + stage);
                assertTrue(maxIntegrity > prevMaxIntegrity, "Max integrity must increase at R" + realm + "-" + stage);
                assertTrue(maxDivine > prevMaxDivine, "Max divine resistance must increase at R" + realm + "-" + stage);
                assertTrue(skillCost <= prevSkillCost, "Divine skill cost must not increase at R" + realm + "-" + stage);

                prevAtk = atk;
                prevHealthBonus = healthBonus;
                prevInfMult = infMult;
                prevSpiritFlat = spiritFlat;
                prevMaxQi = maxQi;
                prevQiRegen = stageQiRegen;
                prevMaxStamina = maxStamina;
                prevMaxIntegrity = maxIntegrity;
                prevMaxDivine = maxDivine;
                prevSkillCost = skillCost;
            }
        }

        assertEquals(31.5D, prevAtk, 1.0E-4D);
        assertEquals(124.0D, prevHealthBonus, 1.0E-4D);
        assertEquals(1.35F, prevInfMult, 1.0E-4F);
        assertEquals(63.0F, prevSpiritFlat, 1.0E-4F);
        assertEquals(390.0F, prevMaxQi, 1.0E-4F);
        assertEquals(260.0F, prevMaxStamina, 1.0E-4F);
        assertEquals(540.0F, prevMaxDivine, 1.0E-4F);
        assertEquals(20.0F, prevSkillCost, 1.0E-4F);
        assertEquals(110, prevCooldown);
    }

    @Test
    void divineSkillTiersAndCombatTriangleScalesStayWithinBounds() {
        for (DivineRemnant remnant : DivineRemnant.values()) {
            DivineSkillDefinition def = DivineSkillRegistry.getForRemnant(remnant);
            assertNotNull(def);
            assertTrue(def.enabled());
            assertTrue(def.baseDamage() > 0.0F);
            assertTrue(def.baseRadius() >= 4.0F);

            float prevTierPower = 0.0F;
            for (DivineResonanceBalance.ManifestationTier tier : DivineResonanceBalance.ManifestationTier.values()) {
                float power = (def.baseDamage() + def.primaryRealmScale() * 7.0F + def.secondaryScale() * 3.0F)
                        * tier.getPowerMultiplier();
                assertTrue(Float.isFinite(power) && power > prevTierPower);
                prevTierPower = power;
            }
        }

        assertTrue(CombatCompatService.FIREARM_BODY_SCALE < CombatCompatService.PROJECTILE_BODY_SCALE);
        assertTrue(CombatCompatService.PROJECTILE_BODY_SCALE < 1.0F);
        assertTrue(CombatCompatService.ARCANE_AOE_QI_COST_SCALE > 0.0F);
        assertTrue(CombatCompatService.ARCANE_AOE_QI_COST_SCALE < CombatCompatService.ARCANE_QI_COST_SCALE);
        assertTrue(CombatCompatService.ARCANE_QI_COST_SCALE < 1.0F);
        assertTrue(CombatCompatService.QI_RESONANT_WEAPON_COST_MULT < 1.0F);
    }

    @Test
    void allBuiltinDatapacksPassCrossValidationCleanly() {
        DatapackValidationService.ValidationReport report = DatapackValidationService.validateAll();
        assertTrue(report.isClean(), "Expected clean builtins, errors=" + report.errors() + " warnings=" + report.warnings());
        assertEquals(14, report.realmCount());
        assertEquals(5, report.factionCount());
        assertEquals(5, report.contractCount());
        assertEquals(4, report.skillCount());
    }
}
