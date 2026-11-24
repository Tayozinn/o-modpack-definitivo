package com.mythiccontracts.client.hud;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.CultivationHudOverlay;
import com.mythiccontracts.client.config.ClientHudConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos Forge do lado cliente para integração opcional com a barra de corações vanilla
 * e limpeza determinística de animações ao desconectar.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HudOverlayEvents {

    private HudOverlayEvents() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            return;
        }
        if (!ClientHudConfig.mainHudEnabled
                || !ClientHudConfig.showBodyHealthBar
                || !ClientHudConfig.hideVanillaHealthBar) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && !player.isSpectator() && !mc.options.hideGui) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        HudAnimationState.getInstance().reset();
        CultivationHudOverlay.resetFrameClock();
    }
}
