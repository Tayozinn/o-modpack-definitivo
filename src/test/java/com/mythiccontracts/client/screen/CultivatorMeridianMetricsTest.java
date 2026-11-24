package com.mythiccontracts.client.screen;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.client.screen.CultivatorMeridianMetrics.EquilibriumState;
import com.mythiccontracts.client.screen.CultivatorMeridianMetrics.MeridianNode;
import com.mythiccontracts.client.screen.CultivatorMeridianMetrics.QiFlowState;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CultivatorMeridianMetricsTest {

    @Test
    void calculatesClampedProgressionPointsAndRatios() {
        assertEquals(1, CultivatorMeridianMetrics.calculateBodyPoints(1, 1));
        assertEquals(11, CultivatorMeridianMetrics.calculateBodyPoints(4, 2));
        assertEquals(21, CultivatorMeridianMetrics.calculateBodyPoints(7, 3));
        assertEquals(21, CultivatorMeridianMetrics.calculateSpiritPoints(99, 99));
        assertEquals(1, CultivatorMeridianMetrics.calculateSpiritPoints(0, 0));

        assertEquals(1.0F / 21.0F, CultivatorMeridianMetrics.calculateProgressRatio(1, 1), 0.0001F);
        assertEquals(1.0F, CultivatorMeridianMetrics.calculateProgressRatio(7, 3), 0.0001F);
    }

    @Test
    void evaluatesEquilibriumAndQiFlowCorrectly() {
        assertEquals(EquilibriumState.HARMONIC, CultivatorMeridianMetrics.evaluateEquilibrium(4, 3));
        assertEquals(EquilibriumState.HARMONIC, CultivatorMeridianMetrics.evaluateEquilibrium(3, 4));
        assertEquals(EquilibriumState.BODY_LEANING, CultivatorMeridianMetrics.evaluateEquilibrium(6, 3));
        assertEquals(EquilibriumState.SPIRIT_LEANING, CultivatorMeridianMetrics.evaluateEquilibrium(2, 5));

        assertEquals(QiFlowState.SHATTERED, CultivatorMeridianMetrics.evaluateQiFlow(true, true, 1.0F));
        assertEquals(QiFlowState.INFUSION_ACTIVE, CultivatorMeridianMetrics.evaluateQiFlow(false, true, 0.5F));
        assertEquals(QiFlowState.STABLE, CultivatorMeridianMetrics.evaluateQiFlow(false, false, 0.8F));
        assertEquals(QiFlowState.LOW, CultivatorMeridianMetrics.evaluateQiFlow(false, false, 0.1F));
    }

    @Test
    void meridianNodesAreInsideSilhouetteAndSymmetrical() {
        assertEquals(7, CultivatorMeridianMetrics.BODY_NODES.size());
        assertEquals(7, CultivatorMeridianMetrics.SPIRIT_NODES.size());

        for (int i = 0; i < 7; i++) {
            MeridianNode body = CultivatorMeridianMetrics.BODY_NODES.get(i);
            MeridianNode spirit = CultivatorMeridianMetrics.SPIRIT_NODES.get(i);

            assertEquals(i + 1, body.requiredRealmOrder());
            assertEquals(i + 1, spirit.requiredRealmOrder());

            assertTrue(body.x() >= 10 && body.x() <= 48);
            assertTrue(spirit.x() >= 52 && spirit.x() <= 90);
            assertTrue(body.y() >= 8 && body.y() <= 118);
            assertTrue(spirit.y() >= 8 && spirit.y() <= 118);

            assertEquals(100, body.x() + spirit.x());
            assertEquals(body.y(), spirit.y());
        }
    }

    @Test
    void coversAllFactionsRemnantsAndSevenTabs() {
        assertEquals(7, CultivatorTab.values().length);
        assertEquals(CultivatorTab.OVERVIEW, CultivatorTab.byIndex(0));
        assertEquals(CultivatorTab.FACTION, CultivatorTab.byIndex(6));

        for (FactionType faction : FactionType.values()) {
            var profile = CultivatorMeridianMetrics.getFactionProfile(faction);
            assertNotNull(profile);
            assertFalse(profile.tagline().isBlank());
            assertFalse(profile.bonusSummary().isBlank());
            assertFalse(profile.restrictionSummary().isBlank());
        }

        for (DivineRemnant remnant : DivineRemnant.values()) {
            var profile = CultivatorMeridianMetrics.getRemnantProfile(remnant);
            assertNotNull(profile);
            assertFalse(profile.archetype().isBlank());
            assertFalse(profile.mechanicSummary().isBlank());
        }
    }
}
