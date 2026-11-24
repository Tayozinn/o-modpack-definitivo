package com.mythiccontracts.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.screen.CultivatorScreen;
import com.mythiccontracts.client.sound.ClientSoundController;
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.network.ActivateCultivationSkillC2SPacket;
import com.mythiccontracts.network.ActivateUniqueSkillC2SPacket;
import com.mythiccontracts.network.CycleDivinePhaseC2SPacket;
import com.mythiccontracts.network.CycleMartialSlotC2SPacket;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.ToggleInfusionC2SPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
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

    public static final KeyMapping UNIQUE_SKILL_KEY = new KeyMapping(
            "key.mythic_contracts.unique_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.mythic_contracts"
    );

    public static final KeyMapping CULTIVATION_SKILL_KEY = new KeyMapping(
            "key.mythic_contracts.cultivation_skill",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.mythic_contracts"
    );

    public static final KeyMapping CYCLE_MARTIAL_SLOT_KEY = new KeyMapping(
            "key.mythic_contracts.cycle_martial_slot",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.mythic_contracts"
    );

    public static final KeyMapping CYCLE_DIVINE_PHASE_KEY = new KeyMapping(
            "key.mythic_contracts.cycle_divine_phase",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.mythic_contracts"
    );

    public static final KeyMapping CULTIVATOR_SCREEN_KEY = new KeyMapping(
            "key.mythic_contracts.cultivator_screen",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
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
            event.register(UNIQUE_SKILL_KEY);
            event.register(CULTIVATION_SKILL_KEY);
            event.register(CYCLE_MARTIAL_SLOT_KEY);
            event.register(CYCLE_DIVINE_PHASE_KEY);
            event.register(CULTIVATOR_SCREEN_KEY);
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
            Minecraft mc = Minecraft.getInstance();
            boolean handledAltG = false;
            boolean handledShiftG = false;
            boolean handledAltV = false;

            while (CYCLE_DIVINE_PHASE_KEY.consumeClick()) {
                handledShiftG = true;
                Player localPlayer = mc.player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ClientSoundController.playUiSound(ModSounds.SPIRIT_SLOT_CYCLE.get(), 0.70F, 1.15F);
                    ModNetworking.sendToServer(new CycleDivinePhaseC2SPacket());
                }
            }
            while (CULTIVATION_SKILL_KEY.consumeClick()) {
                handledAltG = true;
                Player localPlayer = mc.player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ModNetworking.sendToServer(new ActivateCultivationSkillC2SPacket());
                }
            }
            while (CYCLE_MARTIAL_SLOT_KEY.consumeClick()) {
                handledAltV = true;
                Player localPlayer = mc.player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ClientSoundController.playUiSound(ModSounds.SPIRIT_SLOT_CYCLE.get(), 0.70F, 1.05F);
                    ModNetworking.sendToServer(new CycleMartialSlotC2SPacket());
                }
            }

            while (MANA_INFUSION_KEY.consumeClick()) {
                if (handledAltV && MANA_INFUSION_KEY.getKey().equals(CYCLE_MARTIAL_SLOT_KEY.getKey())) {
                    continue;
                }
                Player localPlayer = mc.player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ModNetworking.sendToServer(new ToggleInfusionC2SPacket());
                }
            }
            while (UNIQUE_SKILL_KEY.consumeClick()) {
                if ((handledAltG && UNIQUE_SKILL_KEY.getKey().equals(CULTIVATION_SKILL_KEY.getKey()))
                        || (handledShiftG && UNIQUE_SKILL_KEY.getKey().equals(CYCLE_DIVINE_PHASE_KEY.getKey()))) {
                    continue;
                }
                Player localPlayer = mc.player;
                if (localPlayer != null && localPlayer.isAlive() && !localPlayer.isDeadOrDying()) {
                    ModNetworking.sendToServer(new ActivateUniqueSkillC2SPacket());
                }
            }
            while (CULTIVATOR_SCREEN_KEY.consumeClick()) {
                Player localPlayer = mc.player;
                if (localPlayer != null
                        && mc.screen == null
                        && localPlayer.isAlive()
                        && !localPlayer.isDeadOrDying()) {
                    mc.setScreen(new CultivatorScreen());
                }
            }
        }
    }
}
