package com.mythiccontracts.core.service;

import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmStageDefinition;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;

/**
 * Serviço central responsável por capacidades, regeneração e consumo de Qi,
 * Resistência Divina ([G]), Vigor (Stamina) e Integridade Espiritual.
 */
public final class EnergyService {

    public static final float MIN_QI_FOR_INFUSION = 5.0F;

    public enum DivineSpendOutcome {
        SUCCESS,
        SUCCESS_TRIGGERED_OVERLOAD,
        BLOCKED_OVERLOAD
    }

    public record EnergyTickResult(
            boolean qiDepleted,
            boolean divineOverloadEnded,
            boolean spiritShatterEnded
    ) {
    }

    private EnergyService() {
    }

    public static void recalculateMaxQi(PlayerData playerData) {
        if (playerData == null) {
            return;
        }
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(playerData, CultivationPath.BODY);
        RealmStageDefinition bodyStage = CultivationService.getCurrentStage(playerData, CultivationPath.BODY);
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(playerData, CultivationPath.SPIRIT);
        RealmStageDefinition spiritStage = CultivationService.getCurrentStage(playerData, CultivationPath.SPIRIT);

        float newMax = bodyRealm.qiBase() + bodyStage.qiBonus() + spiritRealm.qiBase() + spiritStage.qiBonus();
        playerData.getCultivation().setMaxQiCapacity(Math.max(1.0F, newMax));

        float maxDivine = DivineResonanceBalance.calculateMaxDivineResistance(playerData);
        playerData.getSkills().setMaxDivineResistance(maxDivine);

        VitalService.recalculateStaminaCapacity(playerData);
        SpiritualIntegrityService.recalculateSpiritualCapacity(playerData);
    }

    public static boolean canActivateInfusion(PlayerData playerData) {
        return playerData != null
                && !playerData.getCultivation().isSpiritShattered()
                && playerData.getCultivation().getCurrentQi() >= MIN_QI_FOR_INFUSION;
    }

    public static boolean consumeQi(PlayerData playerData, float amount) {
        if (playerData == null) {
            return false;
        }
        if (amount <= 0.0F) {
            return true;
        }
        CultivationData cult = playerData.getCultivation();
        if (cult.getCurrentQi() < amount) {
            return false;
        }
        cult.setCurrentQi(cult.getCurrentQi() - amount);
        return true;
    }

    public static void addQi(PlayerData playerData, float amount) {
        if (playerData == null || amount == 0.0F) {
            return;
        }
        CultivationData cult = playerData.getCultivation();
        cult.setCurrentQi(cult.getCurrentQi() + amount);
    }

    /**
     * Tenta canalizar o Resquício Divino consumindo da barra de Resistência Divina.
     * Se a barra zerar neste uso, a habilidade ainda é disparada mas dispara Sobrecarga Divina (60s).
     */
    public static DivineSpendOutcome consumeDivineResistanceOrOverload(PlayerData playerData) {
        if (playerData == null) {
            return DivineSpendOutcome.BLOCKED_OVERLOAD;
        }
        recalculateMaxQi(playerData);
        SkillData skills = playerData.getSkills();
        if (skills.isDivineOverloaded()) {
            return DivineSpendOutcome.BLOCKED_OVERLOAD;
        }
        if (skills.getCurrentDivineResistance() <= 0.0F) {
            skills.setDivineOverloadTicks(DivineResonanceBalance.OVERLOAD_PENALTY_TICKS);
            return DivineSpendOutcome.BLOCKED_OVERLOAD;
        }

        float cost = DivineResonanceBalance.calculateDivineSkillCost(playerData);
        float remaining = skills.getCurrentDivineResistance() - cost;
        if (remaining <= 0.0F) {
            skills.setCurrentDivineResistance(0.0F);
            skills.setDivineOverloadTicks(DivineResonanceBalance.OVERLOAD_PENALTY_TICKS);
            return DivineSpendOutcome.SUCCESS_TRIGGERED_OVERLOAD;
        }

        skills.setCurrentDivineResistance(remaining);
        return DivineSpendOutcome.SUCCESS;
    }

    public static boolean tickEnergy(PlayerData playerData) {
        return tickAllEnergy(playerData).qiDepleted();
    }

    public static EnergyTickResult tickAllEnergy(PlayerData playerData) {
        return tickAllEnergy(playerData, false);
    }

    public static EnergyTickResult tickAllEnergy(PlayerData playerData, boolean sprinting) {
        if (playerData == null) {
            return new EnergyTickResult(false, false, false);
        }
        recalculateMaxQi(playerData);

        // 1. Qi (Infusão V)
        CultivationData cult = playerData.getCultivation();
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(playerData, CultivationPath.SPIRIT);
        int spiritStageNum = CultivationService.getCurrentStageNumber(playerData, CultivationPath.SPIRIT);

        float prevQi = cult.getCurrentQi();
        float maxQi = cult.getMaxQiCapacity();
        boolean qiDepleted = false;

        if (cult.isManaInfusionActive()) {
            float drainPerSecond = Math.max(0.5F, spiritRealm.infusionDrainPerSecond());
            cult.setCurrentQi(prevQi - (drainPerSecond / 20.0F));
            if (cult.getCurrentQi() <= 0.0F) {
                cult.setCurrentQi(0.0F);
                cult.setManaInfusionActive(false);
                qiDepleted = true;
            }
        } else if (prevQi < maxQi && !cult.isSpiritShattered()) {
            float regenPerSecond = spiritRealm.qiRegenBonus()
                    + 0.5F * Math.max(0, spiritStageNum - 1)
                    + ContractManager.getTotalQiRegenBonus(playerData);
            cult.setCurrentQi(prevQi + (Math.max(0.5F, regenPerSecond) / 20.0F));
        }

        // 2. Resistência Divina (Resquícios G)
        SkillData skills = playerData.getSkills();
        boolean overloadEnded = false;
        int overloadTicks = skills.getDivineOverloadTicks();
        if (overloadTicks > 0) {
            int nextOverload = overloadTicks - 1;
            skills.setDivineOverloadTicks(nextOverload);
            if (nextOverload == 0) {
                skills.setCurrentDivineResistance(
                        skills.getMaxDivineResistance() * DivineResonanceBalance.OVERLOAD_RECOVERY_RATIO
                );
                overloadEnded = true;
            }
        } else if (skills.getCurrentDivineResistance() < skills.getMaxDivineResistance()) {
            float divineRegenPerTick = DivineResonanceBalance.calculateDivineRegenPerSecond(playerData) / 20.0F;
            skills.setCurrentDivineResistance(skills.getCurrentDivineResistance() + divineRegenPerTick);
        }

        // 3. Vigor (Stamina Física) — capacidade já recalculada no topo do tick
        VitalService.tickStamina(playerData, sprinting, false);

        // 4. Integridade Espiritual — capacidade já recalculada no topo do tick
        boolean spiritShatterEnded = SpiritualIntegrityService.tickSpiritualIntegrity(playerData, false);

        return new EnergyTickResult(qiDepleted, overloadEnded, spiritShatterEnded);
    }
}
