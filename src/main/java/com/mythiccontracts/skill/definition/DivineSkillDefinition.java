package com.mythiccontracts.skill.definition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Definição Data-Driven de um Resquício Divino ({@code data/<ns>/skills/<id>.json}).
 */
public record DivineSkillDefinition(
        ResourceLocation id,
        String displayName,
        String deityOrigin,
        int hudColor,
        String archetype,
        String mechanicSummary,
        float baseDamage,
        float primaryRealmScale,
        float secondaryScale,
        float baseRadius,
        float costMultiplier,
        float cooldownMultiplier,
        float bossDamageBonusMultiplier,
        int minBodyRealm,
        int minSpiritRealm,
        boolean enabled
) {
    public DivineSkillDefinition {
        displayName = displayName != null ? displayName : (id != null ? id.getPath() : "Resquício");
        deityOrigin = deityOrigin != null ? deityOrigin : "";
        archetype = archetype != null ? archetype : "";
        mechanicSummary = mechanicSummary != null ? mechanicSummary : "";
        baseDamage = Math.max(0.0F, baseDamage);
        primaryRealmScale = Math.max(0.0F, primaryRealmScale);
        secondaryScale = Math.max(0.0F, secondaryScale);
        baseRadius = Mth.clamp(baseRadius, 1.0F, 32.0F);
        costMultiplier = Mth.clamp(costMultiplier, 0.1F, 5.0F);
        cooldownMultiplier = Mth.clamp(cooldownMultiplier, 0.1F, 5.0F);
        bossDamageBonusMultiplier = Mth.clamp(bossDamageBonusMultiplier, 0.5F, 4.0F);
        minBodyRealm = Math.max(1, minBodyRealm);
        minSpiritRealm = Math.max(1, minSpiritRealm);
    }
}
