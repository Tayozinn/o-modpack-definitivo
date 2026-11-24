package com.mythiccontracts.core.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

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
        @Nullable CompoundTag entitySnapshot
) {
    public SubjugatedEntityRecord {
        if (instanceId == null) {
            instanceId = UUID.randomUUID();
        }
        level = Math.max(1, level);
        experience = Math.max(0, experience);
        entitySnapshot = entitySnapshot != null ? entitySnapshot.copy() : null;
    }

    public static SubjugatedEntityRecord createBasic(ResourceLocation entityType) {
        return new SubjugatedEntityRecord(UUID.randomUUID(), entityType, 1, 0, null, null);
    }

    public SubjugatedEntityRecord withoutServerSnapshot() {
        return new SubjugatedEntityRecord(this.instanceId, this.entityType, this.level, this.experience, this.customName, null);
    }
}
