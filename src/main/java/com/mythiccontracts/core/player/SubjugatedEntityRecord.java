package com.mythiccontracts.core.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Registro persistente de uma entidade subjugada vinculada ao jogador.
 */
public record SubjugatedEntityRecord(
        UUID instanceId,
        ResourceLocation entityType,
        int level,
        int experience,
        @Nullable String customName,
        @Nullable CompoundTag entitySnapshot,
        ServantStance stance,
        boolean summoned
) {

    public enum ServantStance {
        FOLLOW("Seguir", "SEG"),
        GUARD("Guardar Posição", "GRD"),
        AGGRESSIVE("Agressivo", "AGR");

        private final String displayName;
        private final String shortTag;

        ServantStance(String displayName, String shortTag) {
            this.displayName = displayName;
            this.shortTag = shortTag;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getShortTag() {
            return this.shortTag;
        }

        public ServantStance next() {
            ServantStance[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static ServantStance parse(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return FOLLOW;
            }
            String norm = raw.trim().toUpperCase(Locale.ROOT);
            for (ServantStance s : values()) {
                if (s.name().equals(norm)) {
                    return s;
                }
            }
            return FOLLOW;
        }
    }

    public SubjugatedEntityRecord(
            UUID instanceId,
            ResourceLocation entityType,
            int level,
            int experience,
            @Nullable String customName,
            @Nullable CompoundTag entitySnapshot
    ) {
        this(instanceId, entityType, level, experience, customName, entitySnapshot, ServantStance.FOLLOW, false);
    }

    public SubjugatedEntityRecord {
        if (instanceId == null) {
            instanceId = UUID.randomUUID();
        }
        level = Math.max(1, level);
        experience = Math.max(0, experience);
        entitySnapshot = entitySnapshot != null ? entitySnapshot.copy() : null;
        stance = stance != null ? stance : ServantStance.FOLLOW;
    }

    public static SubjugatedEntityRecord createBasic(ResourceLocation entityType) {
        return new SubjugatedEntityRecord(UUID.randomUUID(), entityType, 1, 0, null, null, ServantStance.FOLLOW, false);
    }

    public SubjugatedEntityRecord withoutServerSnapshot() {
        return new SubjugatedEntityRecord(
                this.instanceId, this.entityType, this.level, this.experience, this.customName, null, this.stance, this.summoned
        );
    }

    public SubjugatedEntityRecord withStance(ServantStance newStance) {
        return new SubjugatedEntityRecord(
                this.instanceId, this.entityType, this.level, this.experience, this.customName, this.entitySnapshot,
                newStance != null ? newStance : ServantStance.FOLLOW, this.summoned
        );
    }

    public SubjugatedEntityRecord withSummoned(boolean isSummoned) {
        return new SubjugatedEntityRecord(
                this.instanceId, this.entityType, this.level, this.experience, this.customName, this.entitySnapshot,
                this.stance, isSummoned
        );
    }

    public SubjugatedEntityRecord withSnapshot(@Nullable CompoundTag snapshot) {
        return new SubjugatedEntityRecord(
                this.instanceId, this.entityType, this.level, this.experience, this.customName, snapshot,
                this.stance, this.summoned
        );
    }
}
