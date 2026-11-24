package com.mythiccontracts.core.player;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Armazena o estado de Facção, Reputação e Relações do jogador.
 */
public class FactionData {

    public static final ResourceLocation NONE_FACTION_ID = new ResourceLocation("mythic_contracts", "none");

    private ResourceLocation currentFactionId = NONE_FACTION_ID;
    private final Map<ResourceLocation, Integer> reputations = new LinkedHashMap<>();
    private final Map<ResourceLocation, String> relationships = new LinkedHashMap<>();

    public ResourceLocation getCurrentFactionId() {
        return this.currentFactionId;
    }

    public void setCurrentFactionId(ResourceLocation factionId) {
        this.currentFactionId = factionId != null ? factionId : NONE_FACTION_ID;
    }

    public Map<ResourceLocation, Integer> getReputations() {
        return Collections.unmodifiableMap(this.reputations);
    }

    public int getReputation(ResourceLocation factionId) {
        return factionId != null ? this.reputations.getOrDefault(factionId, 0) : 0;
    }

    public void setReputation(ResourceLocation factionId, int value) {
        if (factionId != null) {
            this.reputations.put(factionId, value);
        }
    }

    public void addReputation(ResourceLocation factionId, int delta) {
        if (factionId != null && delta != 0) {
            this.reputations.put(factionId, getReputation(factionId) + delta);
        }
    }

    public void clearReputations() {
        this.reputations.clear();
    }

    public Map<ResourceLocation, String> getRelationships() {
        return Collections.unmodifiableMap(this.relationships);
    }

    public void setRelationship(ResourceLocation factionId, String stance) {
        if (factionId != null && stance != null && !stance.isBlank()) {
            this.relationships.put(factionId, stance);
        }
    }

    public void clearRelationships() {
        this.relationships.clear();
    }

    public void copyFrom(FactionData other) {
        if (other == null) {
            return;
        }
        this.currentFactionId = other.currentFactionId;
        this.reputations.clear();
        this.reputations.putAll(other.reputations);
        this.relationships.clear();
        this.relationships.putAll(other.relationships);
    }
}
