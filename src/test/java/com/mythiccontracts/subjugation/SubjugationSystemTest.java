package com.mythiccontracts.subjugation;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.core.player.PlayerData;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubjugationSystemTest {

    @Test
    void calculatesServantCapacityWithHardCapAtFive() {
        PlayerData data = new PlayerData();
        setSpiritRealm(data, 1);
        assertEquals(1, SubjugationBalance.calculateMaxBoundCapacity(data));

        setSpiritRealm(data, 3);
        assertEquals(2, SubjugationBalance.calculateMaxBoundCapacity(data));

        setSpiritRealm(data, 5);
        assertEquals(3, SubjugationBalance.calculateMaxBoundCapacity(data));

        setSpiritRealm(data, 7);
        assertEquals(4, SubjugationBalance.calculateMaxBoundCapacity(data));

        data.getFaction().setCurrentFactionId(new ResourceLocation("mythic_contracts", FactionType.QUEBRADORES_DE_TRONOS.getId()));
        data.getContracts().addContract(new ResourceLocation("mythic_contracts", "throne_breaker_seal"), 0L, 100);
        data.getSubjugation().setMaxCapacityBonus(3);
        assertEquals(SubjugationBalance.MAX_SERVANT_HARD_CAP, SubjugationBalance.calculateMaxBoundCapacity(data));
    }

    @Test
    void catalystGrantsHpThresholdBonusAndHalvesResourceCost() {
        PlayerData data = new PlayerData();
        setSpiritRealm(data, 6);
        float baseBossHp = SubjugationBalance.calculateEffectiveHpThreshold(data, SubjugationTier.BOSS, false);
        float catalystBossHp = SubjugationBalance.calculateEffectiveHpThreshold(data, SubjugationTier.BOSS, true);
        assertEquals(baseBossHp + 0.10F, catalystBossHp, 0.001F);
        assertEquals(1.0F, SubjugationBalance.calculateResourceCostScale(false), 0.001F);
        assertEquals(0.50F, SubjugationBalance.calculateResourceCostScale(true), 0.001F);
    }

    @Test
    void clashSessionManagesDominanceRevoltSuppressionAndSuccess() {
        ActiveSubjugationSession normal = new ActiveSubjugationSession(
                UUID.randomUUID(), 42, new ResourceLocation("minecraft", "ravager"), "Devastador",
                SubjugationTier.ELITE, false, 120.0F, 0.70F
        );
        assertEquals(35.0F, normal.getDominance(), 0.01F);

        ActiveSubjugationSession withCatalyst = new ActiveSubjugationSession(
                UUID.randomUUID(), 43, new ResourceLocation("minecraft", "warden"), "Warden",
                SubjugationTier.MINI_BOSS, true, 160.0F, 0.55F
        );
        assertEquals(65.0F, withCatalyst.getDominance(), 0.01F);

        boolean sawRevolt = false;
        for (int t = 0; t < SubjugationBalance.REVOLT_INTERVAL_TICKS; t++) {
            ActiveSubjugationSession.TickStatus status = normal.tick(false);
            if (status == ActiveSubjugationSession.TickStatus.REVOLT_TRIGGERED) {
                sawRevolt = true;
                break;
            }
        }
        assertTrue(sawRevolt, "Expected revolt pulse on tick 20");
        assertTrue(normal.isInRevoltWindow());

        float beforeManual = normal.getDominance();
        assertTrue(normal.trySuppressRevolt(false));
        assertFalse(normal.isInRevoltWindow());
        assertEquals(Math.min(100.0F, beforeManual + 25.0F), normal.getDominance(), 0.01F);

        // Avança até concluir 100% de dominância
        ActiveSubjugationSession.TickStatus finalStatus = ActiveSubjugationSession.TickStatus.CONTINUING;
        for (int i = 0; i < 120; i++) {
            finalStatus = normal.tick(true);
            if (normal.isInRevoltWindow()) {
                normal.trySuppressRevolt(true);
            }
            if (finalStatus == ActiveSubjugationSession.TickStatus.SUCCEEDED) {
                break;
            }
        }
        assertEquals(ActiveSubjugationSession.TickStatus.SUCCEEDED, finalStatus);
    }

    @Test
    void masteryGradesAndEffectiveReachScaleWithSpiritRealm() {
        PlayerData data = new PlayerData();
        setSpiritRealm(data, 1);
        assertEquals(1, SubjugationBalance.calculateMasteryGrade(data));
        assertEquals(10.0D, SubjugationBalance.calculateEffectiveReach(1), 0.001D);

        setSpiritRealm(data, 3);
        assertEquals(2, SubjugationBalance.calculateMasteryGrade(data));
        assertEquals(12.0D, SubjugationBalance.calculateEffectiveReach(2), 0.001D);

        setSpiritRealm(data, 5);
        assertEquals(3, SubjugationBalance.calculateMasteryGrade(data));

        setSpiritRealm(data, 7);
        assertEquals(4, SubjugationBalance.calculateMasteryGrade(data));
        assertEquals(14.0D, SubjugationBalance.calculateEffectiveReach(4), 0.001D);
    }

    private static void setSpiritRealm(PlayerData data, int realm) {
        data.getCultivation().setSpiritRealm(realm);
        data.getCultivation().setSpiritRealmId(new ResourceLocation("mythic_contracts", "spirit_realm_" + realm));
        data.getCultivation().setSpiritStage(1);
    }
}
