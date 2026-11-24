package com.mythiccontracts.client;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.hud.DivineResistanceRingWidget;
import com.mythiccontracts.client.hud.HudAnimationState;
import com.mythiccontracts.client.hud.HudRenderSnapshot;
import com.mythiccontracts.client.hud.MainVitalHudWidget;
import com.mythiccontracts.client.screen.HudConfigScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ponto central de registro da HUD dupla do Mythic Contracts 2.0:
 * 1) Painel Vital Minimalista Murim (Conceito A)
 * 2) Emblema Circular Separado da Resistência Divina ([G])
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CultivationHudOverlay {

    private static long lastFrameMillis = -1L;

    public static final IGuiOverlay HUD_OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) {
            return;
        }
        // Evita renderização dupla enquanto a tela de configuração exibe o preview interativo ao vivo
        if (mc.screen instanceof HudConfigScreen) {
            return;
        }
        if (!ClientHudConfig.mainHudEnabled && !ClientHudConfig.divineRingEnabled) {
            return;
        }

        float dtSeconds = computeDeltaSeconds();
        ModCapabilities.get(player).ifPresent(data -> {
            HudRenderSnapshot snap = HudRenderSnapshot.fromLivePlayer(player, data);
            HudAnimationState anim = HudAnimationState.getInstance();
            anim.updateSnapshot(
                    player.getUUID(),
                    snap.bodyHealthRatio(),
                    snap.spiritualRatio(),
                    snap.qiRatio(),
                    snap.staminaRatio(),
                    snap.divineRatio(),
                    snap.bodyRealm(),
                    snap.bodyStage(),
                    snap.bodyRealmName(),
                    snap.spiritRealm(),
                    snap.spiritStage(),
                    snap.spiritRealmName(),
                    snap.tier(),
                    dtSeconds,
                    ClientHudConfig.enableSmoothAnimations
            );

            if (ClientHudConfig.mainHudEnabled && !mc.options.renderDebug) {
                MainVitalHudWidget.render(guiGraphics, mc.font, snap, anim, screenWidth, screenHeight);
            }
            if (ClientHudConfig.divineRingEnabled) {
                DivineResistanceRingWidget.render(guiGraphics, mc.font, snap, anim, screenWidth, screenHeight);
            }
        });
    };

    private CultivationHudOverlay() {
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cultivation_hud", HUD_OVERLAY);
    }

    public static float computeDeltaSeconds() {
        long now = Util.getMillis();
        if (lastFrameMillis < 0L) {
            lastFrameMillis = now;
            return 0.016F;
        }
        long elapsedMs = Math.max(0L, now - lastFrameMillis);
        lastFrameMillis = now;
        return Math.min(HudAnimationState.MAX_DT_SECONDS, elapsedMs / 1000.0F);
    }

    public static void resetFrameClock() {
        lastFrameMillis = -1L;
    }
}
