package com.mythiccontracts.attribute;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Especificação imutável de um modificador de atributo gerenciado pelo {@link AttributeService}.
 */
public record AttributeModifierSpec(
        Attribute attribute,
        AttributeModifier.Operation operation,
        double amount,
        @Nullable UUID fixedUuid,
        String modifierName
) {
    public UUID resolveUuid(String sourceKey) {
        if (this.fixedUuid != null) {
            return this.fixedUuid;
        }
        String seed = sourceKey + ":" + this.modifierName + ":" + this.operation.name();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
