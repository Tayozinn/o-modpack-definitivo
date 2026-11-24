package com.mythiccontracts.core.service;

import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;

/**
 * Serviço responsável pelo recurso vital físico: Resistência Corporal / Vigor (Stamina).
 * Escala com o Cultivo Corporal e permanece isolado da barra de Resistência Divina ([G]).
 */
public final class VitalService {

    public static final float SPRINT_DRAIN_PER_SECOND = 3.5F;
    public static final float MELEE_ATTACK_STAMINA_COST = 6.0F;
    public static final int SPRINT_REGEN_DELAY_TICKS = 15;
    public static final int MELEE_REGEN_DELAY_TICKS = 20;

    private VitalService() {
    }

    public static float calculateMaxStamina(PlayerData data) {
        if (data == null) {
            return CultivationData.DEFAULT_MAX_STAMINA;
        }
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY);
        int bodyStage = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
        return 100.0F
                + 25.0F * Math.max(0, bodyRealm.order() - 1)
                + 5.0F * Math.max(0, bodyStage - 1);
    }

    public static float calculateStaminaRegenPerSecond(PlayerData data) {
        if (data == null) {
            return 10.0F;
        }
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY);
        int bodyStage = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
        return 10.0F
                + 1.8F * Math.max(0, bodyRealm.order() - 1)
                + 0.6F * Math.max(0, bodyStage - 1);
    }

    public static void recalculateStaminaCapacity(PlayerData data) {
        if (data == null) {
            return;
        }
        data.getCultivation().setMaxStamina(calculateMaxStamina(data));
    }

    public static void consumeStamina(PlayerData data, float amount, int regenDelayTicks) {
        if (data == null || amount <= 0.0F) {
            return;
        }
        CultivationData cult = data.getCultivation();
        cult.setCurrentStamina(Math.max(0.0F, cult.getCurrentStamina() - amount));
        cult.setStaminaRegenDelayTicks(Math.max(cult.getStaminaRegenDelayTicks(), Math.max(0, regenDelayTicks)));
    }

    public static void tickStamina(PlayerData data, boolean sprinting) {
        tickStamina(data, sprinting, true);
    }

    public static void tickStamina(PlayerData data, boolean sprinting, boolean recalculateCapacity) {
        if (data == null) {
            return;
        }
        if (recalculateCapacity) {
            recalculateStaminaCapacity(data);
        }
        CultivationData cult = data.getCultivation();

        if (sprinting) {
            if (cult.getCurrentStamina() > 0.0F) {
                cult.setCurrentStamina(cult.getCurrentStamina() - (SPRINT_DRAIN_PER_SECOND / 20.0F));
                cult.setStaminaRegenDelayTicks(Math.max(cult.getStaminaRegenDelayTicks(), SPRINT_REGEN_DELAY_TICKS));
            }
            return;
        }

        int delay = cult.getStaminaRegenDelayTicks();
        if (delay > 0) {
            cult.setStaminaRegenDelayTicks(delay - 1);
            return;
        }

        if (cult.getCurrentStamina() < cult.getMaxStamina()) {
            float regenPerTick = calculateStaminaRegenPerSecond(data) / 20.0F;
            cult.setCurrentStamina(cult.getCurrentStamina() + regenPerTick);
        }
    }
}
