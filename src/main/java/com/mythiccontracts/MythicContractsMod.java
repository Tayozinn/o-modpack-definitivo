package com.mythiccontracts;

import com.mojang.logging.LogUtils;
import com.mythiccontracts.client.ClientBootstrap;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.integration.ModCompatRegistry;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.registry.ModItems;
import com.mythiccontracts.registry.ModParticles;
import com.mythiccontracts.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MythicContractsMod.MOD_ID)
public class MythicContractsMod {
    public static final String MOD_ID = "mythic_contracts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MythicContractsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModParticles.register(modEventBus);
        ModSounds.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientHudConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientBootstrap.registerConfigScreen();
            ClientBootstrap.initClient();
        });

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            ModCompatRegistry.logStartupSummary();
            LOGGER.info("[{}] Canal de rede e capabilities inicializados com sucesso.", MOD_ID);
        });
    }
}
