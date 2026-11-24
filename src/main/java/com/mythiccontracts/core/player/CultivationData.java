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

    private float maxStamina = DEFAULT_MAX_STAMINA;
    private float currentStamina = DEFAULT_MAX_STAMINA;
    private int staminaRegenDelayTicks = 0;

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
        this.currentStamina = wasDeath ? getMaxStamina() : Mth.clamp(other.currentStamina, 0.0F, getMaxStamina());
        this.staminaRegenDelayTicks = wasDeath ? 0 : other.staminaRegenDelayTicks;
    }
}
