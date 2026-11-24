package com.mythiccontracts.cultivation.realm;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Pré-requisitos configuráveis para romper ou entrar em um reino de cultivo.
 */
public record RealmRequirements(
        @Nullable ResourceLocation previousRealm,
        @Nullable ResourceLocation tribulationId,
        int minOppositeRealmOrder,
        float qiCost
) {
    public static final RealmRequirements NONE = new RealmRequirements(null, null, 0, 0.0F);
}
