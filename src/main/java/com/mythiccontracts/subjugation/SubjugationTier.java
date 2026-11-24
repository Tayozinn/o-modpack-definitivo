package com.mythiccontracts.subjugation;

/**
 * Categorias de entidades subjugáveis com requisitos de Reino Espiritual,
 * limiar de HP, custos, taxas base e cooldown em caso de falha na Disputa.
 */
public enum SubjugationTier {
    COMMON("Mob Comum", 1, 0.25F, 0.35F, 30.0F, 15.0F, 0.0F, 0.75F, 0.05F, 0.95F, 200, 45.0F, 40),
    ELITE("Elite", 3, 0.20F, 0.30F, 60.0F, 0.0F, 25.0F, 0.55F, 0.05F, 0.85F, 500, 85.0F, 60),
    MINI_BOSS("Mini-Boss", 5, 0.15F, 0.22F, 110.0F, 0.0F, 45.0F, 0.35F, 0.04F, 0.70F, 1200, 135.0F, 70),
    BOSS("Boss", 6, 0.10F, 0.15F, 180.0F, 0.0F, 70.0F, 0.20F, 0.04F, 0.50F, 2400, 190.0F, 80);

    private final String displayName;
    private final int minSpiritRealm;
    private final float baseHpThreshold;
    private final float throneBreakerHpThreshold;
    private final float qiCost;
    private final float staminaCost;
    private final float spiritualIntegrityCost;
    private final float baseSuccessRate;
    private final float bonusRatePerExtraRealm;
    private final float maxSuccessRateCap;
    private final int failureCooldownTicks;
    private final float willpower;
    private final int clashDurationTicks;

    SubjugationTier(
            String displayName,
            int minSpiritRealm,
            float baseHpThreshold,
            float throneBreakerHpThreshold,
            float qiCost,
            float staminaCost,
            float spiritualIntegrityCost,
            float baseSuccessRate,
            float bonusRatePerExtraRealm,
            float maxSuccessRateCap,
            int failureCooldownTicks,
            float willpower,
            int clashDurationTicks
    ) {
        this.displayName = displayName;
        this.minSpiritRealm = minSpiritRealm;
        this.baseHpThreshold = baseHpThreshold;
        this.throneBreakerHpThreshold = throneBreakerHpThreshold;
        this.qiCost = qiCost;
        this.staminaCost = staminaCost;
        this.spiritualIntegrityCost = spiritualIntegrityCost;
        this.baseSuccessRate = baseSuccessRate;
        this.bonusRatePerExtraRealm = bonusRatePerExtraRealm;
        this.maxSuccessRateCap = maxSuccessRateCap;
        this.failureCooldownTicks = failureCooldownTicks;
        this.willpower = willpower;
        this.clashDurationTicks = clashDurationTicks;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getMinSpiritRealm() {
        return this.minSpiritRealm;
    }

    public float getBaseHpThreshold() {
        return this.baseHpThreshold;
    }

    public float getThroneBreakerHpThreshold() {
        return this.throneBreakerHpThreshold;
    }

    public float getQiCost() {
        return this.qiCost;
    }

    public float getStaminaCost() {
        return this.staminaCost;
    }

    public float getSpiritualIntegrityCost() {
        return this.spiritualIntegrityCost;
    }

    public float getBaseSuccessRate() {
        return this.baseSuccessRate;
    }

    public float getBonusRatePerExtraRealm() {
        return this.bonusRatePerExtraRealm;
    }

    public float getMaxSuccessRateCap() {
        return this.maxSuccessRateCap;
    }

    public int getFailureCooldownTicks() {
        return this.failureCooldownTicks;
    }

    public float getWillpower() {
        return this.willpower;
    }

    public int getClashDurationTicks() {
        return this.clashDurationTicks;
    }
}
