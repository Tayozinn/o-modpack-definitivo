package com.mythiccontracts.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.sound.ClientSoundController;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * Controlador procedural nativo de animação de 1ª pessoa ({@link RenderHandEvent}),
 * recuo/inclinação sutil de câmera ({@link ViewportEvent.ComputeCameraAngles}) e pulso FOV
 * ({@link ComputeFovModifierEvent}), coordenado também com {@link PlayerAnimBridge}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSkillAnimationController {

    private enum MotionStyle {
        DIVINE_RAISE,
        MARTIAL_THRUST,
        WIDE_SWEEP,
        PALM_GUARD,
        CLASH_CHANNEL,
        SERENE_MEDITATION
    }

    private static MotionStyle activeStyle = null;
    private static int activeTicks = 0;
    private static int totalTicks = 0;
    private static float intensity = 1.0F;

    private ClientSkillAnimationController() {
    }

    public static void onVisualEffect(SpawnVisualEffectS2CPacket packet) {
        if (packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        double distSq = mc.player.position().distanceToSqr(packet.getOriginX(), packet.getOriginY(), packet.getOriginZ());
        if (distSq > 6.25D) {
            return;
        }
        if (ClientHudConfig.enableSmoothAnimations) {
            MotionStyle style = resolveStyle(packet.getEffectType());
            if (style != null) {
                activeStyle = style;
                activeTicks = 0;
                totalTicks = resolveDuration(packet.getEffectType());
                intensity = 0.72F + Math.min(3, Math.max(0, packet.getTierOrdinal())) * 0.14F;
            }
        }
        PlayerAnimBridge.playForEffect(mc.player, packet.getEffectType());
    }

    public static void clear() {
        activeStyle = null;
        activeTicks = 0;
        totalTicks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientSoundController.tickClashLoop(mc);

        if (activeStyle != null && !mc.isPaused()) {
            activeTicks++;
            if (activeTicks >= totalTicks) {
                clear();
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        ClientSoundController.clear();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (activeStyle == null || !ClientHudConfig.enableSmoothAnimations || totalTicks <= 0) {
            return;
        }
        float progress = Mth.clamp((activeTicks + event.getPartialTick()) / (float) totalTicks, 0.0F, 1.0F);
        float curve = (float) Math.sin(progress * Math.PI);
        float snapCurve = progress < 0.28F
                ? Mth.clamp(progress / 0.28F, 0.0F, 1.0F)
                : Mth.clamp(1.0F - (progress - 0.28F) / 0.72F, 0.0F, 1.0F);

        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        float sideSign = mainHand ? 1.0F : -1.0F;
        PoseStack pose = event.getPoseStack();

        switch (activeStyle) {
            case DIVINE_RAISE -> {
                pose.translate(sideSign * 0.05F * curve, 0.14F * curve * intensity, -0.10F * curve);
                pose.mulPose(Axis.XP.rotationDegrees(24.0F * curve * intensity));
                pose.mulPose(Axis.ZP.rotationDegrees(sideSign * -12.0F * curve));
            }
            case MARTIAL_THRUST -> {
                if (mainHand) {
                    pose.translate(-0.08F * snapCurve, 0.05F * snapCurve, -0.34F * snapCurve * intensity);
                    pose.mulPose(Axis.XP.rotationDegrees(-28.0F * snapCurve));
                    pose.mulPose(Axis.YP.rotationDegrees(14.0F * snapCurve));
                } else {
                    pose.translate(-0.06F * snapCurve, -0.04F * snapCurve, 0.10F * snapCurve);
                }
            }
            case WIDE_SWEEP -> {
                float arc = (progress - 0.5F) * 2.0F;
                if (mainHand) {
                    pose.translate(-0.22F * curve + 0.16F * arc * curve, 0.04F * curve, -0.24F * curve);
                    pose.mulPose(Axis.YP.rotationDegrees(46.0F * arc * curve));
                    pose.mulPose(Axis.ZP.rotationDegrees(22.0F * curve));
                } else {
                    pose.translate(-0.08F * curve, 0.03F * curve, -0.08F * curve);
                }
            }
            case PALM_GUARD -> {
                pose.translate(-sideSign * 0.10F * curve, 0.09F * curve, -0.16F * curve);
                pose.mulPose(Axis.ZP.rotationDegrees(sideSign * 26.0F * curve));
                pose.mulPose(Axis.XP.rotationDegrees(14.0F * curve));
            }
            case CLASH_CHANNEL -> {
                float tremble = (float) Math.sin((activeTicks + event.getPartialTick()) * 1.4F) * 0.012F;
                pose.translate(-sideSign * 0.04F * curve + tremble, 0.10F * curve, -0.20F * curve);
                pose.mulPose(Axis.XP.rotationDegrees(18.0F * curve));
            }
            case SERENE_MEDITATION -> {
                pose.translate(-sideSign * 0.12F * curve, 0.08F * curve, -0.08F * curve);
                pose.mulPose(Axis.XP.rotationDegrees(12.0F * curve));
                pose.mulPose(Axis.ZP.rotationDegrees(sideSign * 16.0F * curve));
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (activeStyle == null || !ClientHudConfig.enableSmoothAnimations || totalTicks <= 0) {
            return;
        }
        float progress = Mth.clamp((activeTicks + (float) event.getPartialTick()) / (float) totalTicks, 0.0F, 1.0F);
        float impulse = (float) Math.sin(progress * Math.PI);
        float decay = 1.0F - progress;

        switch (activeStyle) {
            case MARTIAL_THRUST -> {
                float shake = (float) Math.sin(progress * Math.PI * 6.0F) * 0.85F * decay * intensity;
                event.setRoll(event.getRoll() + shake);
                event.setPitch(event.getPitch() - 1.1F * impulse * intensity);
            }
            case DIVINE_RAISE -> {
                float tremble = (float) Math.sin(progress * Math.PI * 8.0F) * 0.65F * decay * intensity;
                event.setPitch(event.getPitch() - 1.5F * impulse);
                event.setRoll(event.getRoll() + tremble);
            }
            case WIDE_SWEEP -> event.setRoll(event.getRoll() + 1.4F * (float) Math.sin(progress * Math.PI * 2.0F) * decay);
            case CLASH_CHANNEL -> event.setRoll(event.getRoll() + (float) Math.sin(progress * Math.PI * 5.0F) * 0.5F * decay);
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (activeStyle == null || !ClientHudConfig.enableSmoothAnimations || totalTicks <= 0) {
            return;
        }
        float progress = Mth.clamp((float) activeTicks / (float) totalTicks, 0.0F, 1.0F);
        float pulse = (float) Math.sin(progress * Math.PI);
        float delta = switch (activeStyle) {
            case MARTIAL_THRUST -> 0.045F * pulse * intensity;
            case DIVINE_RAISE -> -0.025F * pulse;
            default -> 0.0F;
        };
        if (delta != 0.0F) {
            event.setNewFovModifier(Mth.clamp(event.getNewFovModifier() + delta, 0.6F, 1.4F));
        }
    }

    @Nullable
    private static MotionStyle resolveStyle(EffectType type) {
        return switch (type) {
            case EYE_OF_RA, SPIRIT_DOMAIN_SUPPRESSION, REALM_BREAKTHROUGH_BURST,
                    UNDYING_MERIDIAN_REVERSAL, PRIMORDIAL_HEAVEN_SUNDER -> MotionStyle.DIVINE_RAISE;
            case MORRIGAN_REAP, ETHER_FLASH_STRIKE, BLOOD_FURNACE_SURGE -> MotionStyle.WIDE_SWEEP;
            case INDRAS_VAJRA, TEZCATLIPOCA_STEP, GALE_MERIDIAN_STEP,
                    SEISMIC_PALM, NASCENT_SOUL_LANCE -> MotionStyle.MARTIAL_THRUST;
            case JADE_BONE_GUARD, GOLDEN_CORE_AEGIS -> MotionStyle.PALM_GUARD;
            case SUBJUGATION_CLASH_START, SUBJUGATION_SUMMON -> MotionStyle.CLASH_CHANNEL;
            case INNER_BREATH_MEDITATION -> MotionStyle.SERENE_MEDITATION;
            default -> null;
        };
    }

    private static int resolveDuration(EffectType type) {
        return switch (type) {
            case EYE_OF_RA, SPIRIT_DOMAIN_SUPPRESSION, PRIMORDIAL_HEAVEN_SUNDER -> 14;
            case SUBJUGATION_CLASH_START, INNER_BREATH_MEDITATION -> 16;
            case JADE_BONE_GUARD, GOLDEN_CORE_AEGIS -> 12;
            default -> 10;
        };
    }
}
