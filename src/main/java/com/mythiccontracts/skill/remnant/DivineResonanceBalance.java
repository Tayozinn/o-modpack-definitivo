package com.mythiccontracts.skill.remnant;

import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import net.minecraft.util.Mth;

/**
 * Balanceamento da Evolução em 4 Fases dos Resquícios Divinos e da Barra de Resistência Divina
 * em função das camadas combinadas + piso mínimo de Cultivo Corporal e Espiritual.
 */
public final class DivineResonanceBalance {

    public static final int OVERLOAD_PENALTY_TICKS = 1200; // 60 segundos (1 minuto)
    public static final float OVERLOAD_RECOVERY_RATIO = 0.35F;

    public enum ManifestationTier {
        TIER_1("I", "Centelha Selada", 0.40F, -2.0D),
        TIER_2("II", "Eco Desperto", 0.75F, 0.0D),
        TIER_3("III", "Ressonância Plena", 1.10F, 1.0D),
        TIER_4("IV", "Encarnação", 1.45F, 2.0D);

        private final String numeral;
        private final String title;
        private final float powerMultiplier;
        private final double radiusOffset;

        ManifestationTier(String numeral, String title, float powerMultiplier, double radiusOffset) {
            this.numeral = numeral;
            this.title = title;
            this.powerMultiplier = powerMultiplier;
            this.radiusOffset = radiusOffset;
        }

        public String getNumeral() {
            return this.numeral;
        }

        public String getTitle() {
            return this.title;
        }

        public float getPowerMultiplier() {
            return this.powerMultiplier;
        }

        public double getRadiusOffset() {
            return this.radiusOffset;
        }
    }

    private DivineResonanceBalance() {
    }

    public static float calculateCombinedScore(PlayerData data) {
        if (data == null) {
            return 1.0F;
        }
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY);
        int bodyStage = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT);
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);

        float bodyScore = bodyRealm.order() + ((bodyStage - 1.0F) / Math.max(1, bodyRealm.getStageCount()));
        float spiritScore = spiritRealm.order() + ((spiritStage - 1.0F) / Math.max(1, spiritRealm.getStageCount()));
        return (bodyScore + spiritScore) * 0.5F;
    }

    /**
     * Calcula a Fase desbloqueada (I–IV) combinando média global + piso mínimo bilateral
     * (Corpo e Espírito) para evitar rush unilateral no início do jogo.
     */
    public static ManifestationTier getManifestationTier(PlayerData data) {
        if (data == null) {
            return ManifestationTier.TIER_1;
        }
        float score = calculateCombinedScore(data);
        int bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY).order();
        int bodyStage = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);

        if (score >= 6.00F && bodyRealm >= 5 && spiritRealm >= 5) {
            return ManifestationTier.TIER_4;
        }
        boolean floor3 = (bodyRealm > 3 || (bodyRealm == 3 && bodyStage >= 2))
                && (spiritRealm > 3 || (spiritRealm == 3 && spiritStage >= 2));
        if (score >= 4.30F && floor3) {
            return ManifestationTier.TIER_3;
        }
        if (score >= 2.65F && bodyRealm >= 2 && spiritRealm >= 2) {
            return ManifestationTier.TIER_2;
        }
        return ManifestationTier.TIER_1;
    }

    public static int getUnlockedPhaseCount(PlayerData data) {
        return getManifestationTier(data).ordinal() + 1;
    }

    public static boolean isPhaseUnlocked(PlayerData data, int phaseIndex) {
        return phaseIndex >= 0 && phaseIndex < getUnlockedPhaseCount(data);
    }

    public static String getPhaseUnlockLabel(int phaseIndex) {
        return switch (phaseIndex) {
            case 0 -> "R1-1";
            case 1 -> "R2-3 (Piso R2-1)";
            case 2 -> "R4-2 (Piso R3-2)";
            case 3 -> "R6-1 (Piso R5-1)";
            default -> "R1-1";
        };
    }

    /**
     * Capacidade máxima da barra exclusiva de Resistência Divina:
     * 100.0 (R1-1/R1-1) até 540.0+ (R7-3/R7-3+).
     */
    public static float calculateMaxDivineResistance(PlayerData data) {
        if (data == null) {
            return 100.0F;
        }
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY);
        int bodyStage = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT);
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);

        return 100.0F
                + 35.0F * Math.max(0, bodyRealm.order() - 1)
                + 35.0F * Math.max(0, spiritRealm.order() - 1)
                + 5.0F * (Math.max(0, bodyStage - 1) + Math.max(0, spiritStage - 1));
    }

    /**
     * Custo base decrescente de Resistência Divina conforme o cultivo sobe:
     * 60.0 (R1-1 — contenção inicial) até mínimo de 20.0 (R7-3+), ajustado pelo {@code cost_multiplier}.
     */
    public static float calculateDivineSkillCost(PlayerData data) {
        float score = calculateCombinedScore(data);
        float progress = Mth.clamp((score - 1.0F) / 6.666667F, 0.0F, 1.0F);
        float baseCost = Math.max(20.0F, Math.round(60.0F - 40.0F * progress));
        if (data == null) {
            return baseCost;
        }
        DivineSkillDefinition def = DivineSkillRegistry.getSkill(data.getSkills().getPrimaryEquippedSkill());
        return Math.max(5.0F, Math.round(baseCost * def.costMultiplier()));
    }

    /**
     * Custo específico por fase (H1–H4): H1 usa o custo base, H2 (+15%), H3 (+35%) e H4 consome 65% da barra máxima.
     */
    public static float calculateDivinePhaseSkillCost(PlayerData data, int phaseIndex) {
        float baseCost = calculateDivineSkillCost(data);
        return switch (Mth.clamp(phaseIndex, 0, 3)) {
            case 1 -> Math.max(24.0F, Math.round(baseCost * 1.15F));
            case 2 -> Math.max(30.0F, Math.round(baseCost * 1.35F));
            case 3 -> Math.max(40.0F, Math.round(calculateMaxDivineResistance(data) * 0.65F));
            default -> baseCost;
        };
    }

    /**
     * Regeneração passiva de Resistência Divina por segundo (fora de sobrecarga):
     * 3.0/s (R1-1) até 10.0/s+ (R7-3+).
     */
    public static float calculateDivineRegenPerSecond(PlayerData data) {
        float score = calculateCombinedScore(data);
        float extra = Math.max(0.0F, score - 1.0F) / 6.666667F;
        return 3.0F + 7.0F * extra;
    }
}
