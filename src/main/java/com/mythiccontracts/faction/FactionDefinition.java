package com.mythiccontracts.faction;

import com.mythiccontracts.contract.ContractDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Definição Data-Driven de uma Facção ({@code data/<ns>/factions/<id>.json}).
 */
public record FactionDefinition(
        ResourceLocation id,
        String displayName,
        String tagline,
        int accentColor,
        float subjugationThreshold,
        String bonusSummary,
        String restrictionSummary,
        List<ContractDefinition.AttributeEntry> passiveModifiers,
        Map<ResourceLocation, String> defaultRelationships
) {
    public static final String MODIFIER_NAME_PREFIX = "mythic_contracts.faction.";

    public FactionDefinition {
        subjugationThreshold = Mth.clamp(subjugationThreshold, 0.05F, 1.0F);
        displayName = displayName != null ? displayName : (id != null ? id.getPath() : "Desconhecida");
        tagline = tagline != null ? tagline : "";
        bonusSummary = bonusSummary != null ? bonusSummary : "";
        restrictionSummary = restrictionSummary != null ? restrictionSummary : "";
        passiveModifiers = passiveModifiers != null ? List.copyOf(passiveModifiers) : List.of();
        defaultRelationships = defaultRelationships != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(defaultRelationships))
                : Map.of();
    }

    public UUID resolveModifierUuid(ContractDefinition.AttributeEntry entry) {
        if (entry.fixedUuid() != null) {
            return entry.fixedUuid();
        }
        String seed = "mythic_contracts.faction:" + this.id + ":" + entry.attributeId() + ":" + entry.operation().name();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public String resolveModifierName(ContractDefinition.AttributeEntry entry) {
        return MODIFIER_NAME_PREFIX + this.id.getNamespace() + "." + this.id.getPath() + "." + entry.attributeId().getPath();
    }
}
