package com.mythiccontracts.skill.cultivation;

import net.minecraft.resources.ResourceLocation;

/**
 * Definição Data-Driven de uma Técnica de Cultivo R1–R7 (carregada de {@code data/<ns>/cultivation_skills/*.json}).
 */
public record CultivationSkillDefinition(
        ResourceLocation id,
        String displayName,
        SkillPathType pathType,
        CultivationSkillCategory category,
        int tierRealm,
        int hudColor,
        String mechanicSummary,
        float qiCost,
        float staminaCost,
        float spiritualIntegrityCost,
        int cooldownTicks,
        boolean usableDuringShatter,
        SkillUnlockRequirements requirements,
        boolean enabled
) {
    public CultivationSkillDefinition {
        displayName = (displayName != null && !displayName.isBlank()) ? displayName : id.getPath();
        pathType = pathType != null ? pathType : SkillPathType.BODY;
        category = category != null ? category : CultivationSkillCategory.OFFENSIVE;
        tierRealm = Math.max(1, Math.min(7, tierRealm));
        mechanicSummary = mechanicSummary != null ? mechanicSummary : "";
        qiCost = Math.max(0.0F, qiCost);
        staminaCost = Math.max(0.0F, staminaCost);
        spiritualIntegrityCost = Math.max(0.0F, spiritualIntegrityCost);
        cooldownTicks = Math.max(20, cooldownTicks);
        requirements = requirements != null ? requirements : SkillUnlockRequirements.NONE;
    }
}
