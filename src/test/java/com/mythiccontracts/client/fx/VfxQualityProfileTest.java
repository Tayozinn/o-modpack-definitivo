package com.mythiccontracts.client.fx;

import com.mythiccontracts.client.config.VisualQualityPreset;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VfxQualityProfileTest {

    @Test
    void scalesBudgetsAndDistancesFromLowThroughHigh() {
        VfxQualityProfile low = VfxQualityProfile.fromPreset(VisualQualityPreset.LOW, 1.0F);
        VfxQualityProfile med = VfxQualityProfile.fromPreset(VisualQualityPreset.MEDIUM, 1.0F);
        VfxQualityProfile high = VfxQualityProfile.fromPreset(VisualQualityPreset.HIGH, 1.0F);

        assertTrue(low.maxParticlesPerCast() < med.maxParticlesPerCast());
        assertTrue(med.maxParticlesPerCast() < high.maxParticlesPerCast());

        assertTrue(low.ringPoints() < med.ringPoints());
        assertTrue(med.ringPoints() < high.ringPoints());

        assertTrue(low.maxTargets() < med.maxTargets());
        assertTrue(med.maxTargets() < high.maxTargets());

        assertTrue(low.renderDistanceBlocks() < med.renderDistanceBlocks());
        assertTrue(med.renderDistanceBlocks() < high.renderDistanceBlocks());

        assertFalse(low.enableSecondaryArcs());
        assertTrue(med.enableSecondaryArcs());
        assertTrue(high.enableSecondaryArcs());
    }

    @Test
    void scalesSafelyWhenVanillaParticleSettingIsMinimal() {
        VfxQualityProfile all = VfxQualityProfile.fromPreset(VisualQualityPreset.HIGH, 1.0F);
        VfxQualityProfile minimal = VfxQualityProfile.fromPreset(VisualQualityPreset.LOW, 0.30F);

        assertTrue(minimal.maxParticlesPerCast() < all.maxParticlesPerCast());
        assertTrue(minimal.maxParticlesPerCast() >= 16);
        assertTrue(minimal.ringPoints() >= 8);
    }

    @Test
    void computesTierBillboardsBezierAndCircleRingGeometry() {
        VfxQualityProfile med = VfxQualityProfile.fromPreset(VisualQualityPreset.MEDIUM, 1.0F);
        assertEquals(0.0F, VfxQualityProfile.computeBillboardSize(ManifestationTier.TIER_1, med), 0.0001F);
        float t2 = VfxQualityProfile.computeBillboardSize(ManifestationTier.TIER_2, med);
        float t3 = VfxQualityProfile.computeBillboardSize(ManifestationTier.TIER_3, med);
        float t4 = VfxQualityProfile.computeBillboardSize(ManifestationTier.TIER_4, med);
        assertTrue(t2 > 0.0F && t3 > t2 && t4 > t3);

        Vec3 p0 = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 p1 = new Vec3(5.0D, 4.0D, 0.0D);
        Vec3 p2 = new Vec3(10.0D, 1.0D, 0.0D);
        Vec3 start = VfxQualityProfile.quadraticBezier(p0, p1, p2, 0.0F);
        Vec3 mid = VfxQualityProfile.quadraticBezier(p0, p1, p2, 0.5F);
        Vec3 end = VfxQualityProfile.quadraticBezier(p0, p1, p2, 1.0F);

        assertEquals(p0.x, start.x, 0.001D);
        assertEquals(p2.x, end.x, 0.001D);
        assertEquals(5.0D, mid.x, 0.001D);
        assertEquals(2.5D, mid.y, 0.001D);

        Vec3 center = new Vec3(10.0D, 64.0D, -4.0D);
        for (int i = 0; i < 12; i++) {
            Vec3 pt = VfxQualityProfile.circleRingPoint(center, 3.5D, i, 12, 0.0D);
            double dist = Math.hypot(pt.x - center.x, pt.z - center.z);
            assertEquals(3.5D, dist, 0.0001D);
            assertEquals(center.y, pt.y, 0.0001D);
        }
    }
}
