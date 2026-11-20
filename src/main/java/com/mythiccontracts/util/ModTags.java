package com.mythiccontracts.util;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    public static final TagKey<EntityType<?>> SUBJUGATABLE = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "subjugatable")
    );

    public static final TagKey<EntityType<?>> HIGH_REALM_BOSS = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "high_realm_boss")
    );

    public static final TagKey<EntityType<?>> LOW_TIER_CELESTIAL_FIEND = TagKey.create(
            Registries.ENTITY_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "low_tier_celestial_fiend")
    );

    private ModTags() {
    }
}
