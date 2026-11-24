package com.mythiccontracts.contract;

import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Definição Data-Driven de um Contrato Mítico (carregada de {@code data/<ns>/contracts/<id>.json}).
 * Suporta campos v2 opcionais de requisitos (reino/reputação) e bônus vitais passivos.
 */
public record ContractDefinition(
        ResourceLocation id,
        String displayName,
        String description,
        Set<FactionType> allowedFactions,
        Set<FactionType> forbiddenFactions,
        List<AttributeEntry> attributeModifiers,
        double periodicAbsorptionHearts,
        int periodicRegenAmplifier,
        boolean breakOnAttackVillager,
        int minBodyRealm,
        int minSpiritRealm,
        int minReputation,
        int reputationReward,
        float qiRegenBonus,
        float spiritualIntegrityBonus,
        String tabooSummary
) {
    public static final String MODIFIER_NAME_PREFIX = "mythic_contracts.contract.";

    public ContractDefinition(
            ResourceLocation id,
            String displayName,
            String description,
            Set<FactionType> allowedFactions,
            Set<FactionType> forbiddenFactions,
            List<AttributeEntry> attributeModifiers,
            double periodicAbsorptionHearts,
            int periodicRegenAmplifier,
            boolean breakOnAttackVillager
    ) {
        this(
                id,
                displayName,
                description,
                allowedFactions,
                forbiddenFactions,
                attributeModifiers,
                periodicAbsorptionHearts,
                periodicRegenAmplifier,
                breakOnAttackVillager,
                1,
                1,
                0,
                0,
                0.0F,
                0.0F,
                ""
        );
    }

    public ContractDefinition {
        allowedFactions = allowedFactions != null ? Collections.unmodifiableSet(allowedFactions) : Set.of();
        forbiddenFactions = forbiddenFactions != null ? Collections.unmodifiableSet(forbiddenFactions) : Set.of();
        attributeModifiers = attributeModifiers != null ? Collections.unmodifiableList(attributeModifiers) : List.of();
        minBodyRealm = Math.max(1, minBodyRealm);
        minSpiritRealm = Math.max(1, minSpiritRealm);
        tabooSummary = tabooSummary != null ? tabooSummary : "";
    }

    public boolean isAllowedForFaction(FactionType faction) {
        FactionType safe = faction != null ? faction : FactionType.NONE;
        if (this.forbiddenFactions.contains(safe)) {
            return false;
        }
        return this.allowedFactions.isEmpty() || this.allowedFactions.contains(safe);
    }

    public UUID resolveModifierUuid(AttributeEntry entry) {
        if (entry.fixedUuid() != null) {
            return entry.fixedUuid();
        }
        String seed = "mythic_contracts.contract:" + this.id + ":" + entry.attributeId() + ":" + entry.operation().name();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public String resolveModifierName(AttributeEntry entry) {
        return MODIFIER_NAME_PREFIX + this.id.getNamespace() + "." + this.id.getPath() + "." + entry.attributeId().getPath();
    }

    public record AttributeEntry(
            ResourceLocation attributeId,
            AttributeModifier.Operation operation,
            double amount,
            @Nullable UUID fixedUuid
    ) {
    }
}
