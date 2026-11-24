package com.mythiccontracts.core.service;

import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritualDamageCategory;
import com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritualDamageResult;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmAttributeBonus;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VitalSystemsTest {

    @Test
    void bodyRealmsGrantExpectedMaxHealthBonusWithStableUuid() {
        RealmDefinition r1 = RealmRegistry.getRealmByOrder(CultivationPath.BODY, 1);
        RealmDefinition r7 = RealmRegistry.getRealmByOrder(CultivationPath.BODY, 7);
        assertNotNull(r1);
        assertNotNull(r7);

        ResourceLocation maxHealthId = new ResourceLocation("minecraft", "generic.max_health");

        // Reino 1-1: 0 bônus; Reino 1-2: +2.0; Reino 1-3: +4.0
        assertFalse(r1.getStageByNumber(1).attributes().stream().anyMatch(a -> a.attributeId().equals(maxHealthId)));
        RealmAttributeBonus r1s2Health = r1.getStageByNumber(2).attributes().stream()
                .filter(a -> a.attributeId().equals(maxHealthId))
                .findFirst()
                .orElseThrow();
        assertEquals(2.0D, r1s2Health.amount(), 0.001D);
        assertEquals(RealmRegistry.BODY_CULTIVATION_HEALTH_UUID, r1s2Health.fixedUuid());

        // Reino 7-3: 2 * 7 * (7 + 3 - 1) - 2 = 126 - 2 = +124.0
        RealmAttributeBonus r7s3Health = r7.getStageByNumber(3).attributes().stream()
                .filter(a -> a.attributeId().equals(maxHealthId))
                .findFirst()
                .orElseThrow();
        assertEquals(124.0D, r7s3Health.amount(), 0.001D);
        assertEquals(RealmRegistry.BODY_CULTIVATION_HEALTH_UUID, r7s3Health.fixedUuid());
    }

    @Test
    void vitalServiceScalesConsumesAndRecoversStamina() {
        PlayerData data = new PlayerData();
        EnergyService.recalculateMaxQi(data);
        assertEquals(100.0F, data.getCultivation().getMaxStamina(), 0.01F);

        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 7, 3);
        EnergyService.recalculateMaxQi(data);
        // 100 + 25*(7-1) + 5*(3-1) = 260
        assertEquals(260.0F, data.getCultivation().getMaxStamina(), 0.01F);
        data.getCultivation().setCurrentStamina(260.0F);

        // Sprint por 20 ticks (1s) consome 3.5 e aplica delay de 15 ticks
        for (int i = 0; i < 20; i++) {
            VitalService.tickStamina(data, true);
        }
        assertEquals(256.5F, data.getCultivation().getCurrentStamina(), 0.05F);
        assertEquals(15, data.getCultivation().getStaminaRegenDelayTicks());

        // Durante os primeiros 15 ticks parados, ainda aguarda delay
        for (int i = 0; i < 15; i++) {
            VitalService.tickStamina(data, false);
        }
        assertEquals(256.5F, data.getCultivation().getCurrentStamina(), 0.05F);

        // Após o delay, regenera até o máximo
        for (int i = 0; i < 20; i++) {
            VitalService.tickStamina(data, false);
        }
        assertEquals(260.0F, data.getCultivation().getCurrentStamina(), 0.05F);
    }

    @Test
    void spiritualIntegrityAbsorbsDamageTriggersShatterAndRecovers() {
        PlayerData data = new PlayerData();
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 2, 1);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 3, 2);
        EnergyService.recalculateMaxQi(data);
        // 100 + 40*(3-1) + 12*(2-1) + 10*(2-1) = 100 + 80 + 12 + 10 = 202
        assertEquals(202.0F, data.getCultivation().getMaxSpiritualIntegrity(), 0.01F);
        data.getCultivation().setCurrentSpiritualIntegrity(202.0F);
        data.getCultivation().setManaInfusionActive(true);

        // Dano físico comum (NONE) vai 100% para vida corporal
        SpiritualDamageResult physical = SpiritualIntegrityService.processIncomingDamage(data, 20.0F, SpiritualDamageCategory.NONE);
        assertEquals(20.0F, physical.remainingBodyDamage(), 0.01F);
        assertEquals(0.0F, physical.absorbedSpiritualDamage(), 0.01F);
        assertEquals(202.0F, data.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);

        // Dano arcano híbrido (30% integridade / 70% corpo)
        SpiritualDamageResult arcane = SpiritualIntegrityService.processIncomingDamage(data, 100.0F, SpiritualDamageCategory.HYBRID_ARCANE);
        assertEquals(70.0F, arcane.remainingBodyDamage(), 0.01F);
        assertEquals(30.0F, arcane.absorbedSpiritualDamage(), 0.01F);
        assertFalse(arcane.triggeredShatter());
        assertEquals(172.0F, data.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);

        // Dano espiritual puro massivo (50% de 400 = 200 > 172) -> absorve 172, sobra 228 pro corpo e dispara Colapso Espiritual
        SpiritualDamageResult shatter = SpiritualIntegrityService.processIncomingDamage(data, 400.0F, SpiritualDamageCategory.PURE_SPIRITUAL);
        assertTrue(shatter.triggeredShatter());
        assertEquals(172.0F, shatter.absorbedSpiritualDamage(), 0.01F);
        assertEquals(228.0F, shatter.remainingBodyDamage(), 0.01F);
        assertEquals(0.0F, data.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);
        assertTrue(data.getCultivation().isSpiritShattered());
        assertFalse(data.getCultivation().isManaInfusionActive());
        assertEquals(SpiritualIntegrityService.SPIRIT_SHATTER_TICKS, data.getCultivation().getSpiritShatterTicks());

        // Durante Colapso Espiritual, recebe vulnerabilidade +35% em dano espiritual puro
        SpiritualDamageResult vuln = SpiritualIntegrityService.processIncomingDamage(data, 100.0F, SpiritualDamageCategory.PURE_SPIRITUAL);
        assertEquals(135.0F, vuln.remainingBodyDamage(), 0.01F);

        // Após 400 ticks, recupera 25% da integridade máxima (202 * 0.25 = 50.5)
        for (int i = 0; i < 399; i++) {
            assertFalse(SpiritualIntegrityService.tickSpiritualIntegrity(data));
        }
        assertTrue(SpiritualIntegrityService.tickSpiritualIntegrity(data));
        assertFalse(data.getCultivation().isSpiritShattered());
        assertEquals(50.5F, data.getCultivation().getCurrentSpiritualIntegrity(), 0.05F);
    }

    @Test
    void deathResetsShatterAndRestoresVitalBarsWhileEndReturnPreservesThem() {
        PlayerData source = new PlayerData();
        EnergyService.recalculateMaxQi(source);
        source.getCultivation().setCurrentStamina(25.0F);
        source.getCultivation().setCurrentSpiritualIntegrity(10.0F);
        source.getCultivation().setSpiritShatterTicks(180);

        PlayerData endReturn = new PlayerData();
        endReturn.copyFrom(source, false);
        assertEquals(25.0F, endReturn.getCultivation().getCurrentStamina(), 0.01F);
        assertEquals(10.0F, endReturn.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);
        assertEquals(180, endReturn.getCultivation().getSpiritShatterTicks());

        PlayerData death = new PlayerData();
        death.copyFrom(source, true);
        assertEquals(source.getCultivation().getMaxStamina(), death.getCultivation().getCurrentStamina(), 0.01F);
        assertEquals(source.getCultivation().getMaxSpiritualIntegrity(), death.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);
        assertEquals(0, death.getCultivation().getSpiritShatterTicks());
    }
}
