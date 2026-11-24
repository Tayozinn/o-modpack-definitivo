package com.mythiccontracts.core.player;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Armazena vínculos de entidades subjugadas e bônus de capacidade do jogador.
 */
public class SubjugationData {

    private final List<SubjugatedEntityRecord> boundEntities = new ArrayList<>();
    private int maxCapacityBonus = 0;

    public List<SubjugatedEntityRecord> getBoundEntities() {
        return Collections.unmodifiableList(this.boundEntities);
    }

    public List<ResourceLocation> getBoundEntityTypes() {
        if (this.boundEntities.isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> types = new ArrayList<>(this.boundEntities.size());
        for (SubjugatedEntityRecord record : this.boundEntities) {
            types.add(record.entityType());
        }
        return Collections.unmodifiableList(types);
    }

    public void addRecord(SubjugatedEntityRecord record) {
        if (record != null && record.entityType() != null) {
            this.boundEntities.add(record);
        }
    }

    public void addEntityType(ResourceLocation entityType) {
        if (entityType != null) {
            this.boundEntities.add(SubjugatedEntityRecord.createBasic(entityType));
        }
    }

    public SubjugatedEntityRecord peekLastRecord() {
        if (this.boundEntities.isEmpty()) {
            return null;
        }
        return this.boundEntities.get(this.boundEntities.size() - 1);
    }

    public SubjugatedEntityRecord popLastRecord() {
        if (this.boundEntities.isEmpty()) {
            return null;
        }
        return this.boundEntities.remove(this.boundEntities.size() - 1);
    }

    public void clearBoundEntities() {
        this.boundEntities.clear();
    }

    public int getMaxCapacityBonus() {
        return Math.max(0, this.maxCapacityBonus);
    }

    public void setMaxCapacityBonus(int maxCapacityBonus) {
        this.maxCapacityBonus = Math.max(0, maxCapacityBonus);
    }

    public void copyFrom(SubjugationData other) {
        if (other == null) {
            return;
        }
        this.boundEntities.clear();
        this.boundEntities.addAll(other.boundEntities);
        this.maxCapacityBonus = other.maxCapacityBonus;
    }
}
