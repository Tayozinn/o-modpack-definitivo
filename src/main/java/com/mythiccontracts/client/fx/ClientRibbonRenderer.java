package com.mythiccontracts.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.config.VisualQualityPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Renderizador nativo de ribbons/arcos geométricos (quad-strips com {@link RenderType#lightning()})
 * na etapa {@link RenderLevelStageEvent.Stage#AFTER_PARTICLES}.
 * Compatível com pipeline padrão Vanilla e Embeddium 0.3.31 (zero shaders customizados);
 * desabilitado automaticamente quando o preset visual está em {@link VisualQualityPreset#LOW}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientRibbonRenderer {

    public static final int MAX_ACTIVE_RIBBONS = 48;
    private static final Deque<RibbonInstance> RIBBONS = new ArrayDeque<>();

    private ClientRibbonRenderer() {
    }

    public static boolean isEnabled() {
        return ClientHudConfig.visualQualityPreset != VisualQualityPreset.LOW;
    }

    public static void addBeamRibbon(
            Vec3 start,
            Vec3 end,
            float halfWidth,
            int lifetimeTicks,
            float r,
            float g,
            float b,
            float alpha,
            float arcLift
    ) {
        if (!isEnabled() || start == null || end == null) {
            return;
        }
        int segs = ClientHudConfig.visualQualityPreset == VisualQualityPreset.HIGH ? 10 : 7;
        Vec3[] pts = new Vec3[segs];
        for (int i = 0; i < segs; i++) {
            float u = (float) i / (float) (segs - 1);
            double lift = Math.sin(u * Math.PI) * arcLift;
            pts[i] = start.lerp(end, u).add(0.0D, lift, 0.0D);
        }
        enqueue(new RibbonInstance(pts, halfWidth, lifetimeTicks, r, g, b, alpha, 0.0D));
    }

    public static void addArcRibbon(
            Vec3 center,
            double radius,
            double startAngleRad,
            double sweepRad,
            double yOffset,
            double ySlope,
            float halfWidth,
            int lifetimeTicks,
            float r,
            float g,
            float b,
            float alpha
    ) {
        if (!isEnabled() || center == null) {
            return;
        }
        int segs = ClientHudConfig.visualQualityPreset == VisualQualityPreset.HIGH ? 14 : 9;
        Vec3[] pts = new Vec3[segs];
        for (int i = 0; i < segs; i++) {
            float u = (float) i / (float) (segs - 1);
            double ang = startAngleRad + sweepRad * u;
            pts[i] = new Vec3(
                    center.x + Math.cos(ang) * radius,
                    center.y + yOffset + ySlope * u,
                    center.z + Math.sin(ang) * radius
            );
        }
        enqueue(new RibbonInstance(pts, halfWidth, lifetimeTicks, r, g, b, alpha, 0.0D));
    }

    public static void addHelixRibbon(
            Vec3 center,
            double radius,
            double baseY,
            double topY,
            double startAngleRad,
            double turnsRad,
            float halfWidth,
            int lifetimeTicks,
            float r,
            float g,
            float b,
            float alpha
    ) {
        if (!isEnabled() || center == null) {
            return;
        }
        int segs = ClientHudConfig.visualQualityPreset == VisualQualityPreset.HIGH ? 14 : 10;
        Vec3[] pts = new Vec3[segs];
        for (int i = 0; i < segs; i++) {
            float u = (float) i / (float) (segs - 1);
            double ang = startAngleRad + turnsRad * u;
            double curR = radius * (1.0D - u * 0.25D);
            pts[i] = new Vec3(
                    center.x + Math.cos(ang) * curR,
                    Mth.lerp(u, (float) baseY, (float) topY),
                    center.z + Math.sin(ang) * curR
            );
        }
        enqueue(new RibbonInstance(pts, halfWidth, lifetimeTicks, r, g, b, alpha, 0.015D));
    }

    public static void clearAll() {
        RIBBONS.clear();
    }

    private static void enqueue(RibbonInstance ribbon) {
        if (RIBBONS.size() >= MAX_ACTIVE_RIBBONS) {
            RIBBONS.pollFirst();
        }
        RIBBONS.addLast(ribbon);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RIBBONS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null || !isEnabled()) {
            if (!isEnabled()) {
                RIBBONS.clear();
            }
            return;
        }
        Iterator<RibbonInstance> it = RIBBONS.iterator();
        while (it.hasNext()) {
            RibbonInstance r = it.next();
            r.ageTicks++;
            if (r.ageTicks >= r.lifetimeTicks) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RIBBONS.isEmpty() || !isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 camPos = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (RibbonInstance ribbon : RIBBONS) {
            renderRibbon(consumer, matrix, camPos, ribbon, partialTick);
        }

        bufferSource.endBatch(RenderType.lightning());
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearAll();
    }

    private static void renderRibbon(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 camPos,
            RibbonInstance ribbon,
            float partialTick
    ) {
        Vec3[] pts = ribbon.points;
        if (pts.length < 2) {
            return;
        }
        float ageF = ribbon.ageTicks + partialTick;
        float progress = Mth.clamp(ageF / Math.max(1.0F, (float) ribbon.lifetimeTicks), 0.0F, 1.0F);
        float lifeFade = progress < 0.15F
                ? Mth.clamp(progress / 0.15F, 0.15F, 1.0F)
                : Mth.clamp((1.0F - progress) / 0.85F, 0.0F, 1.0F);
        if (lifeFade <= 0.01F) {
            return;
        }

        double yLift = ribbon.risePerTick * ageF;
        int red = Mth.clamp(Math.round(ribbon.r * 255.0F), 0, 255);
        int green = Mth.clamp(Math.round(ribbon.g * 255.0F), 0, 255);
        int blue = Mth.clamp(Math.round(ribbon.b * 255.0F), 0, 255);

        for (int i = 0; i < pts.length - 1; i++) {
            float u0 = (float) i / (float) (pts.length - 1);
            float u1 = (float) (i + 1) / (float) (pts.length - 1);
            float taper0 = (float) Math.sin(u0 * Math.PI);
            float taper1 = (float) Math.sin(u1 * Math.PI);
            float w0 = ribbon.halfWidth * (0.28F + 0.72F * taper0) * (1.0F - progress * 0.35F);
            float w1 = ribbon.halfWidth * (0.28F + 0.72F * taper1) * (1.0F - progress * 0.35F);

            int a0 = Mth.clamp(Math.round(ribbon.alpha * lifeFade * (0.35F + 0.65F * taper0) * 255.0F), 0, 255);
            int a1 = Mth.clamp(Math.round(ribbon.alpha * lifeFade * (0.35F + 0.65F * taper1) * 255.0F), 0, 255);

            Vec3 p0 = pts[i].add(0.0D, yLift, 0.0D);
            Vec3 p1 = pts[i + 1].add(0.0D, yLift, 0.0D);
            Vec3 seg = p1.subtract(p0);
            if (seg.lengthSqr() < 1.0E-6D) {
                continue;
            }
            Vec3 toCam = camPos.subtract(p0);
            Vec3 side = seg.cross(toCam);
            if (side.lengthSqr() < 1.0E-6D) {
                side = new Vec3(0.0D, 1.0D, 0.0D);
            } else {
                side = side.normalize();
            }

            float sx0 = (float) (side.x * w0);
            float sy0 = (float) (side.y * w0);
            float sz0 = (float) (side.z * w0);
            float sx1 = (float) (side.x * w1);
            float sy1 = (float) (side.y * w1);
            float sz1 = (float) (side.z * w1);

            float p0x = (float) p0.x;
            float p0y = (float) p0.y;
            float p0z = (float) p0.z;
            float p1x = (float) p1.x;
            float p1y = (float) p1.y;
            float p1z = (float) p1.z;

            // Renderiza quad-strip de dupla face emitindo 4 vértices na malha POSITION_COLOR
            consumer.vertex(matrix, p0x - sx0, p0y - sy0, p0z - sz0).color(red, green, blue, a0).endVertex();
            consumer.vertex(matrix, p0x + sx0, p0y + sy0, p0z + sz0).color(red, green, blue, a0).endVertex();
            consumer.vertex(matrix, p1x + sx1, p1y + sy1, p1z + sz1).color(red, green, blue, a1).endVertex();
            consumer.vertex(matrix, p1x - sx1, p1y - sy1, p1z - sz1).color(red, green, blue, a1).endVertex();
        }
    }

    private static final class RibbonInstance {
        final Vec3[] points;
        final float halfWidth;
        final int lifetimeTicks;
        final float r;
        final float g;
        final float b;
        final float alpha;
        final double risePerTick;
        int ageTicks = 0;

        RibbonInstance(
                Vec3[] points,
                float halfWidth,
                int lifetimeTicks,
                float r,
                float g,
                float b,
                float alpha,
                double risePerTick
        ) {
            this.points = points;
            this.halfWidth = Math.max(0.02F, halfWidth);
            this.lifetimeTicks = Math.max(3, lifetimeTicks);
            this.r = r;
            this.g = g;
            this.b = b;
            this.alpha = Mth.clamp(alpha, 0.05F, 1.0F);
            this.risePerTick = risePerTick;
        }
    }
}
