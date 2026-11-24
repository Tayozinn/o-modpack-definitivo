package com.mythiccontracts.core.service;

import net.minecraft.util.Mth;

/**
 * Configuração Data-Driven da recuperação da Integridade Espiritual
 * (carregada de {@code data/mythic_contracts/cultivation/spiritual_integrity_balance.json}).
 */
public record SpiritualIntegrityBalanceConfig(
        int combatCooldownTicks,
        float combatMultiplier,
        float outOfCombatMultiplier,
        float meditationMultiplier,
        int meditationStillTicks,
        float baseRegenR1,
        float linearRealmScale,
        float quadraticRealmScale,
        float stageScale,
        float maxFlatBonusRatio,
        float minPctMultiplier,
        float maxPctMultiplier,
        boolean interruptOnCombatDamage,
        boolean enableMeditationBonus
) {
    public static final SpiritualIntegrityBalanceConfig DEFAULT = new SpiritualIntegrityBalanceConfig(
            100,
            0.40F,
            1.00F,
            2.20F,
            40,
            3.10F,
            2.20F,
            0.18F,
            0.65F,
            0.50F,
            0.25F,
            2.25F,
            true,
            true
    );

    private static volatile SpiritualIntegrityBalanceConfig active = DEFAULT;

    public SpiritualIntegrityBalanceConfig {
        combatCooldownTicks = Mth.clamp(combatCooldownTicks, 0, 1200);
        combatMultiplier = Mth.clamp(combatMultiplier, 0.0F, 2.0F);
        outOfCombatMultiplier = Mth.clamp(outOfCombatMultiplier, 0.1F, 5.0F);
        meditationMultiplier = Mth.clamp(meditationMultiplier, 0.5F, 6.0F);
        meditationStillTicks = Mth.clamp(meditationStillTicks, 0, 600);
        baseRegenR1 = Math.max(0.1F, baseRegenR1);
        linearRealmScale = Math.max(0.0F, linearRealmScale);
        quadraticRealmScale = Math.max(0.0F, quadraticRealmScale);
        stageScale = Math.max(0.0F, stageScale);
        maxFlatBonusRatio = Mth.clamp(maxFlatBonusRatio, 0.0F, 2.0F);
        minPctMultiplier = Mth.clamp(minPctMultiplier, 0.05F, 1.0F);
        maxPctMultiplier = Mth.clamp(maxPctMultiplier, minPctMultiplier, 4.0F);
    }

    public static SpiritualIntegrityBalanceConfig getActive() {
        return active;
    }

    public static void setLoaded(SpiritualIntegrityBalanceConfig config) {
        active = config != null ? config : DEFAULT;
    }

    public static void resetToDefaults() {
        active = DEFAULT;
    }
}
