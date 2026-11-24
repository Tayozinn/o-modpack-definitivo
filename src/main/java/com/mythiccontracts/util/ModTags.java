package com.mythiccontracts.util;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

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

    public static final TagKey<DamageType> SPIRITUAL_DAMAGE = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "spiritual_damage")
    );

    public static final TagKey<DamageType> ARCANE_SPIRITUAL_DAMAGE = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "arcane_spiritual_damage")
    );

    public static final TagKey<DamageType> FIREARM_DAMAGE = TagKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(MythicContractsMod.MOD_ID, "firearm_damage")
    );

    public static final TagKey<Item> QI_RESONANT_WEAPONS = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(MythicContractsMod.MOD_ID, "qi_resonant_weapons")
    );

    private ModTags() {
    }
}
