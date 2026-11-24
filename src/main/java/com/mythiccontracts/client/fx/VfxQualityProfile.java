package com.mythiccontracts.client.fx;

import com.mythiccontracts.client.config.VisualQualityPreset;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Perfil puro de densidade e geometria VFX calculado no cliente conforme o preset
 * visual configurado e a configuração de partículas vanilla do jogador.
 * Sem dependências de renderização OpenGL para permitir testes unitários em JUnit 5.
 */
public record VfxQualityProfile(
        int ringPoints,
        int ringCount,
        int maxTargets,
        float billboardScaleMultiplier,
        float durationMultiplier,
        double renderDistanceBlocks,
        int maxParticlesPerCast,
        boolean enableSecondaryArcs
) {

    public static VfxQualityProfile fromPreset(VisualQualityPreset preset, float vanillaParticleScale) {
        VisualQualityPreset safePreset = preset != null ? preset : VisualQualityPreset.MEDIUM;
        float scale = Mth.clamp(vanillaParticleScale, 0.25F, 1.0F);

        int baseRingPoints;
        int ringCount;
        int maxTargets;
        float billboardMult;
        float durationMult;
        double renderDistance;
        int baseMaxParticles;
        boolean secondaryArcs;

        switch (safePreset) {
            case LOW -> {
                baseRingPoints = 12;
                ringCount = 1;
                maxTargets = 4;
                billboardMult = 0.60F;
                durationMult = 0.55F;
                renderDistance = 24.0D;
                baseMaxParticles = 36;
                secondaryArcs = false;
            }
            case HIGH -> {
                baseRingPoints = 34;
                ringCount = 3;
                maxTargets = 12;
                billboardMult = 1.15F;
                durationMult = 1.20F;
                renderDistance = 56.0D;
                baseMaxParticles = 148;
                secondaryArcs = true;
            }
            case MEDIUM -> {
                baseRingPoints = 22;
                ringCount = 2;
                maxTargets = 8;
                billboardMult = 0.90F;
                durationMult = 1.00F;
                renderDistance = 40.0D;
                baseMaxParticles = 84;
                secondaryArcs = true;
            }
            default -> throw new IllegalStateException("Unexpected preset: " + safePreset);
        }

        int finalRingPoints = Math.max(8, Math.round(baseRingPoints * scale));
        int finalBudget = Math.max(16, Math.round(baseMaxParticles * scale));
        int finalMaxTargets = Math.max(2, Math.round(maxTargets * (0.5F + 0.5F * scale)));

        return new VfxQualityProfile(
                finalRingPoints,
                ringCount,
                finalMaxTargets,
                billboardMult,
                durationMult,
                renderDistance,
                finalBudget,
                secondaryArcs
        );
    }

    public static Vec3 quadraticBezier(Vec3 p0, Vec3 p1, Vec3 p2, float t) {
        if (p0 == null || p1 == null || p2 == null) {
            return Vec3.ZERO;
        }
        float clampedT = Mth.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - clampedT;
        double x = (inv * inv) * p0.x + (2.0F * inv * clampedT) * p1.x + (clampedT * clampedT) * p2.x;
        double y = (inv * inv) * p0.y + (2.0F * inv * clampedT) * p1.y + (clampedT * clampedT) * p2.y;
        double z = (inv * inv) * p0.z + (2.0F * inv * clampedT) * p1.z + (clampedT * clampedT) * p2.z;
        return new Vec3(x, y, z);
    }

    public static Vec3 circleRingPoint(Vec3 center, double radius, int index, int total, double angleOffset) {
        Vec3 safeCenter = center != null ? center : Vec3.ZERO;
        int safeTotal = Math.max(1, total);
        double safeRadius = Math.max(0.0D, radius);
        double angle = angleOffset + ((2.0D * Math.PI * index) / safeTotal);
        return new Vec3(
                safeCenter.x + Math.cos(angle) * safeRadius,
                safeCenter.y,
                safeCenter.z + Math.sin(angle) * safeRadius
        );
    }

    public static float computeBillboardSize(ManifestationTier tier, VfxQualityProfile profile) {
        ManifestationTier safeTier = tier != null ? tier : ManifestationTier.TIER_1;
        float base = switch (safeTier) {
            case TIER_1 -> 0.0F;
            case TIER_2 -> 0.90F;
            case TIER_3 -> 1.40F;
            case TIER_4 -> 2.20F;
        };
        float mult = profile != null ? profile.billboardScaleMultiplier() : 1.0F;
        return base * mult;
    }
}
