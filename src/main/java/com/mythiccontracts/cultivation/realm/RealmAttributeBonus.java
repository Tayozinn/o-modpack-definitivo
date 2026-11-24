package com.mythiccontracts.cultivation.realm;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Benefício de atributo concedido por um estágio de reino de cultivo.
 */
public record RealmAttributeBonus(
        ResourceLocation attributeId,
        AttributeModifier.Operation operation,
        double amount,
        @Nullable UUID fixedUuid
) {
    public RealmAttributeBonus(ResourceLocation attributeId, AttributeModifier.Operation operation, double amount) {
        this(attributeId, operation, amount, null);
    }
}
