package com.mythiccontracts.registry;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.item.CultivationAdvancementItem;
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

    public static final RegistryObject<Item> BODY_REFINEMENT_PILL = ITEMS.register(
            "body_refinement_pill",
            () -> new CultivationAdvancementItem(
                    CultivationAdvancementItem.Mode.BODY_STAGE,
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)
            )
    );

    public static final RegistryObject<Item> SPIRIT_CONDENSATION_PILL = ITEMS.register(
            "spirit_condensation_pill",
            () -> new CultivationAdvancementItem(
                    CultivationAdvancementItem.Mode.SPIRIT_STAGE,
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE)
            )
    );

    public static final RegistryObject<Item> TRIBULATION_TOME = ITEMS.register(
            "tribulation_tome",
            () -> new CultivationAdvancementItem(
                    CultivationAdvancementItem.Mode.REALM_BREAKTHROUGH,
                    new Item.Properties().stacksTo(4).rarity(Rarity.EPIC)
            )
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
            event.accept(BODY_REFINEMENT_PILL);
            event.accept(SPIRIT_CONDENSATION_PILL);
            event.accept(TRIBULATION_TOME);
        }
    }
}
