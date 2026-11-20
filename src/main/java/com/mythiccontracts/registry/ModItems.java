package com.mythiccontracts.registry;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.item.SubjugationSealItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            ForgeRegistries.ITEMS,
            MythicContractsMod.MOD_ID
    );

    public static final RegistryObject<Item> SUBJUGATION_SEAL = ITEMS.register(
            "subjugation_seal",
            () -> new SubjugationSealItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC))
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT
                || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SUBJUGATION_SEAL);
        }
    }
}
