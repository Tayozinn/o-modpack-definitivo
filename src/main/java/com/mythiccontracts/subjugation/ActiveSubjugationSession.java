package com.mythiccontracts.subjugation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Sessão ativa de Disputa de Poder Espiritual autoritativa no servidor.
 */
public final class ActiveSubjugationSession {

    public enum TickStatus {
        CONTINUING,
        REVOLT_TRIGGERED,
        SUCCEEDED,
        TIMEOUT_ROLL
    }

    private final UUID playerUuid;
    private final int targetEntityId;
    private final ResourceLocation targetTypeId;
    private final String targetName;
    private final SubjugationTier tier;
    private final boolean catalystUsed;
    private final float playerSoulPower;
    private final float baseSuccessChance;
    private final int maxDurationTicks;

    private float dominance;
    private int elapsedTicks = 0;
    private int revoltWindowTicks = 0;
    private int nextRevoltTick = SubjugationBalance.REVOLT_INTERVAL_TICKS;
    private int suppressedRevolts = 0;

    public ActiveSubjugationSession(
            UUID playerUuid,
            int targetEntityId,
            ResourceLocation targetTypeId,
            String targetName,
            SubjugationTier tier,
            boolean catalystUsed,
            float playerSoulPower,
            float baseSuccessChance
    ) {
        this.playerUuid = playerUuid;
        this.targetEntityId = targetEntityId;
        this.targetTypeId = targetTypeId;
        this.targetName = targetName != null ? targetName : "Entidade";
        this.tier = tier != null ? tier : SubjugationTier.COMMON;
        this.catalystUsed = catalystUsed;
        this.playerSoulPower = Math.max(1.0F, playerSoulPower);
        this.baseSuccessChance = Mth.clamp(baseSuccessChance, 0.05F, 0.99F);
        this.maxDurationTicks = this.tier.getClashDurationTicks();
        this.dominance = catalystUsed ? 65.0F : 35.0F;
    }

    public TickStatus tick(boolean infusionActive) {
        this.elapsedTicks++;
        if (this.revoltWindowTicks > 0) {
            this.revoltWindowTicks--;
            if (this.revoltWindowTicks == 0) {
                // Jogador não suprimiu o Pulso de Revolta dentro da janela
                this.dominance = Mth.clamp(this.dominance - 10.0F, 0.0F, 100.0F);
            }
        }

        float delta = SubjugationBalance.computeClashDominanceDeltaPerTick(this.playerSoulPower, this.tier, infusionActive);
        this.dominance = Mth.clamp(this.dominance + delta, 0.0F, 100.0F);

        if (this.dominance >= 100.0F) {
            return TickStatus.SUCCEEDED;
        }
        if (this.elapsedTicks >= this.maxDurationTicks) {
            return TickStatus.TIMEOUT_ROLL;
        }
        if (this.elapsedTicks >= this.nextRevoltTick && this.revoltWindowTicks == 0) {
            this.nextRevoltTick = this.elapsedTicks + SubjugationBalance.REVOLT_INTERVAL_TICKS;
            this.revoltWindowTicks = SubjugationBalance.REVOLT_WINDOW_TICKS;
            return TickStatus.REVOLT_TRIGGERED;
        }
        return TickStatus.CONTINUING;
    }

    public boolean trySuppressRevolt(boolean autoByInfusion) {
        if (this.revoltWindowTicks <= 0) {
            return false;
        }
        this.revoltWindowTicks = 0;
        this.suppressedRevolts++;
        this.dominance = Mth.clamp(this.dominance + (autoByInfusion ? 18.0F : 25.0F), 0.0F, 100.0F);
        return true;
    }

    public float computeFinalCaptureProbability() {
        float dominanceRatio = Mth.clamp(this.dominance / 100.0F, 0.0F, 1.0F);
        float suppressionBonus = Math.min(0.20F, this.suppressedRevolts * 0.07F);
        float blended = (this.baseSuccessChance * 0.45F) + (dominanceRatio * 0.55F) + suppressionBonus;
        return Mth.clamp(blended, 0.05F, 0.99F);
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public int getTargetEntityId() {
        return this.targetEntityId;
    }

    public ResourceLocation getTargetTypeId() {
        return this.targetTypeId;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public SubjugationTier getTier() {
        return this.tier;
    }

    public boolean isCatalystUsed() {
        return this.catalystUsed;
    }

    public float getDominance() {
        return this.dominance;
    }

    public float getDominanceRatio() {
        return Mth.clamp(this.dominance / 100.0F, 0.0F, 1.0F);
    }

    public boolean isInRevoltWindow() {
        return this.revoltWindowTicks > 0;
    }

    public int getElapsedTicks() {
        return this.elapsedTicks;
    }

    public int getMaxDurationTicks() {
        return this.maxDurationTicks;
    }
}
