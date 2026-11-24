package com.mythiccontracts.integration;

import com.mythiccontracts.integration.CombatCompatService.AttackChannel;
import com.mythiccontracts.integration.CombatCompatService.InfusionProfile;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatCompatServiceTest {

    @AfterEach
    void tearDown() {
        CombatCompatService.clearAllState();
    }

    @Test
    void classifiesTaczBulletsIronsSpellsProjectilesAndMelee() {
        assertEquals(AttackChannel.FIREARM, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("tacz", "bullet"), false, false, false, false, true));
        assertEquals(AttackChannel.FIREARM, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("tacz", "bullet_ignore_armor"), false, false, false, false, true));
        assertEquals(AttackChannel.FIREARM, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("tacz", "bullet_void"), true, false, false, false, false));

        assertEquals(AttackChannel.ARCANE_SPELL, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("irons_spellbooks", "fire_magic"), false, true, false, false, false));
        assertEquals(AttackChannel.ARCANE_SPELL, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("irons_spellbooks", "eldritch_magic"), false, false, true, false, false));
        assertEquals(AttackChannel.ARCANE_SPELL, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("irons_spellbooks", "blood_cauldron"), false, false, false, false, false));

        assertEquals(AttackChannel.PROJECTILE, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("minecraft", "arrow"), false, false, false, false, true));
        assertEquals(AttackChannel.MELEE, CombatCompatService.classifyByKeyAndFlags(
                new ResourceLocation("minecraft", "player_attack"), false, false, false, false, false));
    }

    @Test
    void evaluatesInfusionProfilesAcrossCombatTriangle() {
        InfusionProfile standardMelee = CombatCompatService.evaluateInfusionProfile(AttackChannel.MELEE, false, false);
        assertEquals(1.0F, standardMelee.bodyBuffScale(), 1.0E-4F);
        assertEquals(1.0F, standardMelee.spiritBonusScale(), 1.0E-4F);
        assertEquals(1.0F, standardMelee.qiCostScale(), 1.0E-4F);
        assertTrue(standardMelee.consumesMeleeStamina());

        InfusionProfile resonantMelee = CombatCompatService.evaluateInfusionProfile(AttackChannel.MELEE, false, true);
        assertEquals(CombatCompatService.QI_RESONANT_WEAPON_COST_MULT, resonantMelee.qiCostScale(), 1.0E-4F);
        assertTrue(resonantMelee.consumesMeleeStamina());
        assertEquals("Ressonante", resonantMelee.channelLabel());

        InfusionProfile sweepMelee = CombatCompatService.evaluateInfusionProfile(AttackChannel.MELEE, true, false);
        assertEquals(CombatCompatService.SWEEP_MELEE_QI_COST_SCALE, sweepMelee.qiCostScale(), 1.0E-4F);
        assertFalse(sweepMelee.consumesMeleeStamina());

        InfusionProfile resonantSweep = CombatCompatService.evaluateInfusionProfile(AttackChannel.MELEE, true, true);
        assertEquals(
                CombatCompatService.SWEEP_MELEE_QI_COST_SCALE * CombatCompatService.QI_RESONANT_WEAPON_COST_MULT,
                resonantSweep.qiCostScale(),
                1.0E-4F
        );
        assertFalse(resonantSweep.consumesMeleeStamina());

        InfusionProfile firearm = CombatCompatService.evaluateInfusionProfile(AttackChannel.FIREARM, false, false);
        assertEquals(CombatCompatService.FIREARM_BODY_SCALE, firearm.bodyBuffScale(), 1.0E-4F);
        assertEquals(CombatCompatService.FIREARM_SPIRIT_SCALE, firearm.spiritBonusScale(), 1.0E-4F);
        assertEquals(CombatCompatService.FIREARM_QI_COST_SCALE, firearm.qiCostScale(), 1.0E-4F);
        assertFalse(firearm.consumesMeleeStamina());

        InfusionProfile arcaneSingle = CombatCompatService.evaluateInfusionProfile(AttackChannel.ARCANE_SPELL, false, false);
        assertEquals(CombatCompatService.ARCANE_BODY_SCALE, arcaneSingle.bodyBuffScale(), 1.0E-4F);
        assertEquals(CombatCompatService.ARCANE_SPIRIT_SCALE, arcaneSingle.spiritBonusScale(), 1.0E-4F);
        assertEquals(CombatCompatService.ARCANE_QI_COST_SCALE, arcaneSingle.qiCostScale(), 1.0E-4F);
        assertFalse(arcaneSingle.consumesMeleeStamina());

        InfusionProfile arcaneAoe = CombatCompatService.evaluateInfusionProfile(AttackChannel.ARCANE_SPELL, true, false);
        assertEquals(CombatCompatService.ARCANE_AOE_QI_COST_SCALE, arcaneAoe.qiCostScale(), 1.0E-4F);

        InfusionProfile projectile = CombatCompatService.evaluateInfusionProfile(AttackChannel.PROJECTILE, false, false);
        assertEquals(CombatCompatService.PROJECTILE_BODY_SCALE, projectile.bodyBuffScale(), 1.0E-4F);
        assertEquals(CombatCompatService.PROJECTILE_SPIRIT_SCALE, projectile.spiritBonusScale(), 1.0E-4F);
        assertEquals(CombatCompatService.PROJECTILE_QI_COST_SCALE, projectile.qiCostScale(), 1.0E-4F);
        assertFalse(projectile.consumesMeleeStamina());
    }

    @Test
    void debouncesBetterCombatMultiTargetMeleeStaminaAndTracksSameTickInfusion() {
        UUID playerId = UUID.randomUUID();

        // Sweep do Better Combat acertando 5 alvos no mesmo tick 100 consome Vigor apenas 1x
        assertTrue(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 100L));
        assertFalse(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 100L));
        assertFalse(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 100L));
        assertFalse(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 102L));
        assertTrue(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 103L));

        // 1o alvo no tick 100 retorna false (dreno completo); 2o+ alvos no mesmo tick retornam true (desconto sweep/AoE)
        assertFalse(CombatCompatService.registerInfusionHitAndCheckSameTick(playerId, 100L));
        assertTrue(CombatCompatService.registerInfusionHitAndCheckSameTick(playerId, 100L));
        assertTrue(CombatCompatService.registerInfusionHitAndCheckSameTick(playerId, 100L));
        assertFalse(CombatCompatService.registerInfusionHitAndCheckSameTick(playerId, 101L));

        CombatCompatService.clearPlayerState(playerId);
        assertTrue(CombatCompatService.tryConsumeMeleeStaminaWindow(playerId, 101L));
        assertFalse(CombatCompatService.registerInfusionHitAndCheckSameTick(playerId, 101L));
    }
}
