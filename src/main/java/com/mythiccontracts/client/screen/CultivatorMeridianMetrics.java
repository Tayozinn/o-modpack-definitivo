package com.mythiccontracts.client.screen;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.faction.FactionDefinition;
import com.mythiccontracts.faction.FactionRegistry;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * View-model puro da Interface do Cultivador (sem dependências OpenGL/Minecraft client)
 * para cálculo de meridianos, equilíbrio Corpo×Espírito, fluxo de Qi e perfis confirmados de Facções/Resquícios.
 */
public final class CultivatorMeridianMetrics {

    public static final int MAX_PROGRESSION_POINTS = 21; // 7 reinos * 3 etapas
    public static final int SILHOUETTE_W = 100;
    public static final int SILHOUETTE_H = 126;
    public static final int YINYANG_CENTER_X = 50;
    public static final int YINYANG_CENTER_Y = 56;

    public record MeridianNode(int x, int y, int requiredRealmOrder, String label) {
    }

    public enum EquilibriumState {
        HARMONIC("Harmônico", 0xFF34D399),
        BODY_LEANING("Foco Yang (Corpo)", 0xFFF87171),
        SPIRIT_LEANING("Foco Yin (Espírito)", 0xFFC084FC);

        private final String defaultLabel;
        private final int color;

        EquilibriumState(String defaultLabel, int color) {
            this.defaultLabel = defaultLabel;
            this.color = color;
        }

        public String getDefaultLabel() {
            return this.defaultLabel;
        }

        public int getColor() {
            return this.color;
        }
    }

    public enum QiFlowState {
        SHATTERED("Colapso Espiritual", 0xFFF43F5E),
        INFUSION_ACTIVE("Fluxo Intenso [V]", 0xFF22D3EE),
        STABLE("Estável", 0xFF38BDF8),
        LOW("Baixo", 0xFFF59E0B);

        private final String defaultLabel;
        private final int color;

        QiFlowState(String defaultLabel, int color) {
            this.defaultLabel = defaultLabel;
            this.color = color;
        }

        public String getDefaultLabel() {
            return this.defaultLabel;
        }

        public int getColor() {
            return this.color;
        }
    }

    public record FactionProfile(
            FactionType type,
            int color,
            String tagline,
            String bonusSummary,
            String restrictionSummary
    ) {
    }

    public record RemnantProfile(
            DivineRemnant remnant,
            String archetype,
            String mechanicSummary
    ) {
    }

    /**
     * 7 nós Yang (Cultivo Corporal — lado esquerdo da silhueta, espelhados em torno de x=50).
     */
    public static final List<MeridianNode> BODY_NODES = List.of(
            new MeridianNode(41, 110, 1, "R1 Base"),
            new MeridianNode(41, 94, 2, "R2 Perna"),
            new MeridianNode(42, 72, 3, "R3 Dantian"),
            new MeridianNode(26, 72, 4, "R4 Palma"),
            new MeridianNode(26, 54, 5, "R5 Braço"),
            new MeridianNode(36, 42, 6, "R6 Ombro"),
            new MeridianNode(42, 16, 7, "R7 Coroa")
    );

    /**
     * 7 nós Yin (Cultivo Espiritual — lado direito da silhueta, espelho exato de BODY_NODES).
     */
    public static final List<MeridianNode> SPIRIT_NODES = List.of(
            new MeridianNode(59, 110, 1, "R1 Raiz"),
            new MeridianNode(59, 94, 2, "R2 Perna"),
            new MeridianNode(58, 72, 3, "R3 Núcleo"),
            new MeridianNode(74, 72, 4, "R4 Palma"),
            new MeridianNode(74, 54, 5, "R5 Braço"),
            new MeridianNode(64, 42, 6, "R6 Ombro"),
            new MeridianNode(58, 16, 7, "R7 Coroa")
    );

    private CultivatorMeridianMetrics() {
    }

    public static int calculateBodyPoints(int bodyRealm, int bodyStage) {
        return clampProgressPoints(bodyRealm, bodyStage);
    }

    public static int calculateSpiritPoints(int spiritRealm, int spiritStage) {
        return clampProgressPoints(spiritRealm, spiritStage);
    }

    public static float calculateProgressRatio(int realmOrder, int stageNumber) {
        return clampProgressPoints(realmOrder, stageNumber) / (float) MAX_PROGRESSION_POINTS;
    }

    private static int clampProgressPoints(int realm, int stage) {
        int r = Mth.clamp(realm, 1, 7);
        int s = Mth.clamp(stage, 1, 3);
        return Mth.clamp((r - 1) * 3 + s, 1, MAX_PROGRESSION_POINTS);
    }

    public static EquilibriumState evaluateEquilibrium(int bodyRealm, int spiritRealm) {
        int diff = bodyRealm - spiritRealm;
        if (Math.abs(diff) <= 1) {
            return EquilibriumState.HARMONIC;
        }
        return diff > 1 ? EquilibriumState.BODY_LEANING : EquilibriumState.SPIRIT_LEANING;
    }

    public static QiFlowState evaluateQiFlow(boolean spiritShattered, boolean infusionActive, float qiRatio) {
        if (spiritShattered) {
            return QiFlowState.SHATTERED;
        }
        if (infusionActive) {
            return QiFlowState.INFUSION_ACTIVE;
        }
        return qiRatio < 0.25F ? QiFlowState.LOW : QiFlowState.STABLE;
    }

    public static FactionProfile getFactionProfile(FactionType faction) {
        FactionType safe = faction != null ? faction : FactionType.NONE;
        FactionDefinition def = FactionRegistry.getForType(safe);
        return new FactionProfile(
                safe,
                def.accentColor(),
                def.tagline(),
                def.bonusSummary(),
                def.restrictionSummary()
        );
    }

    public static RemnantProfile getRemnantProfile(DivineRemnant remnant) {
        DivineRemnant safe = remnant != null ? remnant : DivineRemnant.TEZCATLIPOCA_STEP;
        DivineSkillDefinition def = DivineSkillRegistry.getForRemnant(safe);
        return new RemnantProfile(
                safe,
                def.archetype(),
                def.mechanicSummary()
        );
    }
}
