package com.mythiccontracts.core.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Armazena o estado puro de Cultivo (Corpo, Espírito e Energia/Qi) de um jogador.
 * Regras de progressão e fórmulas de balanceamento ficam fora desta classe.
 */
public class CultivationData {

    public static final int DEFAULT_MIN_REALM = 1;
    public static final int DEFAULT_MAX_REALM = 7;
    public static final int DEFAULT_MIN_STAGE = 1;
    public static final int DEFAULT_MAX_STAGE = 3;
    public static final float DEFAULT_MAX_QI = 100.0F;
    public static final float DEFAULT_MAX_SPIRITUAL_INTEGRITY = 100.0F;
    public static final float DEFAULT_MAX_STAMINA = 100.0F;

    private ResourceLocation bodyRealmId = new ResourceLocation("mythic_contracts", "body_realm_1");
    private int bodyRealm = DEFAULT_MIN_REALM;
    private int bodyStage = DEFAULT_MIN_STAGE;

    private ResourceLocation spiritRealmId = new ResourceLocation("mythic_contracts", "spirit_realm_1");
    private int spiritRealm = DEFAULT_MIN_REALM;
    private int spiritStage = DEFAULT_MIN_STAGE;

    private float maxQiCapacity = DEFAULT_MAX_QI;
    private float currentQi = DEFAULT_MAX_QI;
    private boolean manaInfusionActive = false;

    private float maxSpiritualIntegrity = DEFAULT_MAX_SPIRITUAL_INTEGRITY;
    private float currentSpiritualIntegrity = DEFAULT_MAX_SPIRITUAL_INTEGRITY;
    private int spiritShatterTicks = 0;
    private int spiritCombatCooldownTicks = 0;
    private int stillMeditationTicks = 0;
    private int meditationBoostTicks = 0;
    private int clientSpiritRegenStateOrdinal = 4;

    private float maxStamina = DEFAULT_MAX_STAMINA;
    private float currentStamina = DEFAULT_MAX_STAMINA;
    private int staminaRegenDelayTicks = 0;

    // Timers ativos de Técnicas de Cultivo (tiqueados no servidor; resetam ao morrer)
    private int jadeBoneGuardTicks = 0;
    private int goldenCoreAegisTicks = 0;
    private int bloodFurnaceTicks = 0;
    private int spiritDomainTicks = 0;
    private int undyingReversalTicks = 0;
    private float transientSpiritRegenPctModifier = 0.0F;

    public ResourceLocation getBodyRealmId() {
        return this.bodyRealmId;
    }

    public void setBodyRealmId(ResourceLocation bodyRealmId) {
        if (bodyRealmId != null) {
            this.bodyRealmId = bodyRealmId;
        }
    }

    public int getBodyRealm() {
        return this.bodyRealm;
    }

    public void setBodyRealm(int bodyRealm) {
        this.bodyRealm = Math.max(DEFAULT_MIN_REALM, bodyRealm);
    }

    public int getBodyStage() {
        return this.bodyStage;
    }

    public void setBodyStage(int bodyStage) {
        this.bodyStage = Math.max(DEFAULT_MIN_STAGE, bodyStage);
    }

    public ResourceLocation getSpiritRealmId() {
        return this.spiritRealmId;
    }

    public void setSpiritRealmId(ResourceLocation spiritRealmId) {
        if (spiritRealmId != null) {
            this.spiritRealmId = spiritRealmId;
        }
    }

    public int getSpiritRealm() {
        return this.spiritRealm;
    }

    public void setSpiritRealm(int spiritRealm) {
        this.spiritRealm = Math.max(DEFAULT_MIN_REALM, spiritRealm);
    }

    public int getSpiritStage() {
        return this.spiritStage;
    }

    public void setSpiritStage(int spiritStage) {
        this.spiritStage = Math.max(DEFAULT_MIN_STAGE, spiritStage);
    }

    public float getMaxQiCapacity() {
        return Math.max(1.0F, this.maxQiCapacity);
    }

    public void setMaxQiCapacity(float maxQiCapacity) {
        this.maxQiCapacity = Math.max(1.0F, sanitize(maxQiCapacity, DEFAULT_MAX_QI));
        this.currentQi = Mth.clamp(this.currentQi, 0.0F, this.maxQiCapacity);
    }

    public float getCurrentQi() {
        return Mth.clamp(sanitize(this.currentQi, 0.0F), 0.0F, getMaxQiCapacity());
    }

    public void setCurrentQi(float currentQi) {
        this.currentQi = Mth.clamp(sanitize(currentQi, 0.0F), 0.0F, getMaxQiCapacity());
    }

    public boolean isManaInfusionActive() {
        return this.manaInfusionActive;
    }

    public void setManaInfusionActive(boolean manaInfusionActive) {
        this.manaInfusionActive = manaInfusionActive;
    }

    public float getMaxSpiritualIntegrity() {
        return Math.max(1.0F, this.maxSpiritualIntegrity);
    }

    public void setMaxSpiritualIntegrity(float maxSpiritualIntegrity) {
        this.maxSpiritualIntegrity = Math.max(1.0F, sanitize(maxSpiritualIntegrity, DEFAULT_MAX_SPIRITUAL_INTEGRITY));
        this.currentSpiritualIntegrity = Mth.clamp(this.currentSpiritualIntegrity, 0.0F, this.maxSpiritualIntegrity);
    }

    public float getCurrentSpiritualIntegrity() {
        return Mth.clamp(sanitize(this.currentSpiritualIntegrity, 0.0F), 0.0F, getMaxSpiritualIntegrity());
    }

    public void setCurrentSpiritualIntegrity(float currentSpiritualIntegrity) {
        this.currentSpiritualIntegrity = Mth.clamp(sanitize(currentSpiritualIntegrity, 0.0F), 0.0F, getMaxSpiritualIntegrity());
    }

    public int getSpiritShatterTicks() {
        return Math.max(0, this.spiritShatterTicks);
    }

    public void setSpiritShatterTicks(int spiritShatterTicks) {
        this.spiritShatterTicks = Math.max(0, spiritShatterTicks);
    }

    public boolean isSpiritShattered() {
        return this.spiritShatterTicks > 0;
    }

    public float getMaxStamina() {
        return Math.max(1.0F, this.maxStamina);
    }

    public void setMaxStamina(float maxStamina) {
        this.maxStamina = Math.max(1.0F, sanitize(maxStamina, DEFAULT_MAX_STAMINA));
        this.currentStamina = Mth.clamp(this.currentStamina, 0.0F, this.maxStamina);
    }

    public float getCurrentStamina() {
        return Mth.clamp(sanitize(this.currentStamina, 0.0F), 0.0F, getMaxStamina());
    }

    public void setCurrentStamina(float currentStamina) {
        this.currentStamina = Mth.clamp(sanitize(currentStamina, 0.0F), 0.0F, getMaxStamina());
    }

    private static float sanitize(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    public int getStaminaRegenDelayTicks() {
        return Math.max(0, this.staminaRegenDelayTicks);
    }

    public void setStaminaRegenDelayTicks(int staminaRegenDelayTicks) {
        this.staminaRegenDelayTicks = Math.max(0, staminaRegenDelayTicks);
    }

    public int getSpiritCombatCooldownTicks() {
        return Math.max(0, this.spiritCombatCooldownTicks);
    }

    public void setSpiritCombatCooldownTicks(int spiritCombatCooldownTicks) {
        this.spiritCombatCooldownTicks = Math.max(0, spiritCombatCooldownTicks);
    }

    public int getStillMeditationTicks() {
        return Math.max(0, this.stillMeditationTicks);
    }

    public void setStillMeditationTicks(int stillMeditationTicks) {
        this.stillMeditationTicks = Math.max(0, stillMeditationTicks);
    }

    public int getMeditationBoostTicks() {
        return Math.max(0, this.meditationBoostTicks);
    }

    public void setMeditationBoostTicks(int meditationBoostTicks) {
        this.meditationBoostTicks = Math.max(0, meditationBoostTicks);
    }

    public int getClientSpiritRegenStateOrdinal() {
        return Mth.clamp(this.clientSpiritRegenStateOrdinal, 0, 4);
    }

    public void setClientSpiritRegenStateOrdinal(int clientSpiritRegenStateOrdinal) {
        this.clientSpiritRegenStateOrdinal = Mth.clamp(clientSpiritRegenStateOrdinal, 0, 4);
    }

    public int getJadeBoneGuardTicks() {
        return Math.max(0, this.jadeBoneGuardTicks);
    }

    public void setJadeBoneGuardTicks(int jadeBoneGuardTicks) {
        this.jadeBoneGuardTicks = Math.max(0, jadeBoneGuardTicks);
    }

    public int getGoldenCoreAegisTicks() {
        return Math.max(0, this.goldenCoreAegisTicks);
    }

    public void setGoldenCoreAegisTicks(int goldenCoreAegisTicks) {
        this.goldenCoreAegisTicks = Math.max(0, goldenCoreAegisTicks);
    }

    public int getBloodFurnaceTicks() {
        return Math.max(0, this.bloodFurnaceTicks);
    }

    public void setBloodFurnaceTicks(int bloodFurnaceTicks) {
        this.bloodFurnaceTicks = Math.max(0, bloodFurnaceTicks);
    }

    public int getSpiritDomainTicks() {
        return Math.max(0, this.spiritDomainTicks);
    }

    public void setSpiritDomainTicks(int spiritDomainTicks) {
        this.spiritDomainTicks = Math.max(0, spiritDomainTicks);
    }

    public int getUndyingReversalTicks() {
        return Math.max(0, this.undyingReversalTicks);
    }

    public void setUndyingReversalTicks(int undyingReversalTicks) {
        this.undyingReversalTicks = Math.max(0, undyingReversalTicks);
    }

    public float getTransientSpiritRegenPctModifier() {
        return Float.isFinite(this.transientSpiritRegenPctModifier)
                ? Mth.clamp(this.transientSpiritRegenPctModifier, -0.75F, 1.0F)
                : 0.0F;
    }

    public void setTransientSpiritRegenPctModifier(float modifier) {
        this.transientSpiritRegenPctModifier = Float.isFinite(modifier)
                ? Mth.clamp(modifier, -0.75F, 1.0F)
                : 0.0F;
    }

    public void tickCombatBuffTimers() {
        if (this.jadeBoneGuardTicks > 0) this.jadeBoneGuardTicks--;
        if (this.goldenCoreAegisTicks > 0) this.goldenCoreAegisTicks--;
        if (this.bloodFurnaceTicks > 0) this.bloodFurnaceTicks--;
        if (this.spiritDomainTicks > 0) this.spiritDomainTicks--;
        if (this.undyingReversalTicks > 0) this.undyingReversalTicks--;
    }

    public void copyFrom(CultivationData other, boolean wasDeath) {
        if (other == null) {
            return;
        }
        this.bodyRealmId = other.bodyRealmId;
        this.bodyRealm = other.bodyRealm;
        this.bodyStage = other.bodyStage;
        this.spiritRealmId = other.spiritRealmId;
        this.spiritRealm = other.spiritRealm;
        this.spiritStage = other.spiritStage;
        this.maxQiCapacity = other.maxQiCapacity;
        this.maxSpiritualIntegrity = other.maxSpiritualIntegrity;
        this.maxStamina = other.maxStamina;
        this.manaInfusionActive = wasDeath ? false : other.manaInfusionActive;
        this.currentQi = wasDeath ? getMaxQiCapacity() : Mth.clamp(other.currentQi, 0.0F, getMaxQiCapacity());
        this.currentSpiritualIntegrity = wasDeath
                ? getMaxSpiritualIntegrity()
                : Mth.clamp(other.currentSpiritualIntegrity, 0.0F, getMaxSpiritualIntegrity());
        this.spiritShatterTicks = wasDeath ? 0 : other.spiritShatterTicks;
        this.spiritCombatCooldownTicks = wasDeath ? 0 : other.spiritCombatCooldownTicks;
        this.stillMeditationTicks = wasDeath ? 0 : other.stillMeditationTicks;
        this.meditationBoostTicks = wasDeath ? 0 : other.meditationBoostTicks;
        this.clientSpiritRegenStateOrdinal = wasDeath ? 4 : other.clientSpiritRegenStateOrdinal;
        this.currentStamina = wasDeath ? getMaxStamina() : Mth.clamp(other.currentStamina, 0.0F, getMaxStamina());
        this.staminaRegenDelayTicks = wasDeath ? 0 : other.staminaRegenDelayTicks;
        this.jadeBoneGuardTicks = wasDeath ? 0 : other.jadeBoneGuardTicks;
        this.goldenCoreAegisTicks = wasDeath ? 0 : other.goldenCoreAegisTicks;
        this.bloodFurnaceTicks = wasDeath ? 0 : other.bloodFurnaceTicks;
        this.spiritDomainTicks = wasDeath ? 0 : other.spiritDomainTicks;
        this.undyingReversalTicks = wasDeath ? 0 : other.undyingReversalTicks;
        this.transientSpiritRegenPctModifier = wasDeath ? 0.0F : other.transientSpiritRegenPctModifier;
    }
}
