package com.mythiccontracts.cultivation.realm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

/**
 * Definição Data-Driven de um Reino de Cultivo (Corpo ou Espírito).
 */
public record RealmDefinition(
        ResourceLocation id,
        CultivationPath path,
        int order,
        String displayName,
        List<RealmStageDefinition> stages,
        RealmRequirements requirements,
        float qiBase,
        float qiRegenBonus,
        float infusionDrainPerSecond,
        float infusionDrainPerHit,
        float spiritualRegenBase
) {
    public RealmDefinition(
            ResourceLocation id,
            CultivationPath path,
            int order,
            String displayName,
            List<RealmStageDefinition> stages,
            RealmRequirements requirements,
            float qiBase,
            float qiRegenBonus,
            float infusionDrainPerSecond,
            float infusionDrainPerHit
    ) {
        this(
                id, path, order, displayName, stages, requirements,
                qiBase, qiRegenBonus, infusionDrainPerSecond, infusionDrainPerHit,
                -1.0F
        );
    }

    public RealmDefinition {
        order = Math.max(1, order);
        displayName = (displayName != null && !displayName.isBlank()) ? displayName : id.getPath();
        stages = stages != null ? Collections.unmodifiableList(stages) : List.of();
        requirements = requirements != null ? requirements : RealmRequirements.NONE;
        if (!Float.isFinite(spiritualRegenBase)) {
            spiritualRegenBase = -1.0F;
        }
    }

    public int getStageCount() {
        return Math.max(1, this.stages.size());
    }

    public RealmStageDefinition getStageByNumber(int oneBasedStage) {
        if (this.stages.isEmpty()) {
            return new RealmStageDefinition("base", "Base", 0.0F, 0.0F, 0.0F, List.of());
        }
        int index = Mth.clamp(oneBasedStage - 1, 0, this.stages.size() - 1);
        return this.stages.get(index);
    }

    public boolean isFinalStage(int oneBasedStage) {
        return oneBasedStage >= getStageCount();
    }
}
