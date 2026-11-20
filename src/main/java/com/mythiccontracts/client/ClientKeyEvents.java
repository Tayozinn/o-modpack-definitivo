package com.mythiccontracts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.ToggleInfusionC2SPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyEvents {

    public static final KeyMapping MANA_INFUSION_KEY = new KeyMapping(
            "key.mythic_contracts.mana_infusion",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.mythic_contracts"
    );

    private ClientKeyEvents() {
    }

    @Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ClientModBusEvents {
        private ClientModBusEvents() {
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(MANA_INFUSION_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientForgeBusEvents {
        private ClientForgeBusEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (MANA_INFUSION_KEY.consumeClick()) {
                Player localPlayer = Minecraft.getInstance().player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ModNetworking.sendToServer(new ToggleInfusionC2SPacket());
                }
            }
        }
    }
}
