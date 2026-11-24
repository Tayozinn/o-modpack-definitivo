package com.mythiccontracts.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Widget circular separado da Resistência Divina ([G]).
 * Porte compacto no canto inferior esquerdo (34 px GUI de diâmetro externo, pouco maior que 1 slot da hotbar),
 * exibindo o emblema do Resquício Divino no centro, anel radial de Resistência Divina ao redor,
 * badge de tecla [G], numeral de Camada [I-IV] e estado de Sobrecarga Divina (60s).
 */
public final class DivineResistanceRingWidget {

    public static final int OUTER_DIAMETER = DivineRingGeometry.DIAMETER; // 34 px GUI

    private static final ResourceLocation FRAME_TEXTURE =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/divine_ring_frame.png");
    private static final ResourceLocation CRACKS_TEXTURE =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/divine_ring_overload_cracks.png");

    private static final ResourceLocation EMBLEM_EYE_OF_RA =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_eye_of_ra.png");
    private static final ResourceLocation EMBLEM_MORRIGAN_REAP =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_morrigan_reap.png");
    private static final ResourceLocation EMBLEM_INDRAS_VAJRA =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_indras_vajra.png");
    private static final ResourceLocation EMBLEM_TEZCATLIPOCA_STEP =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_tezcatlipoca_step.png");

    private DivineResistanceRingWidget() {
    }

    public static void render(
            GuiGraphics guiGraphics,
            Font font,
            HudRenderSnapshot snap,
            HudAnimationState anim,
            int screenWidth,
            int screenHeight
    ) {
        if (guiGraphics == null || font == null || snap == null || anim == null) {
            return;
        }

        boolean showNumbers = ClientHudConfig.divineRingShowNumbers;
        int layoutWidth = OUTER_DIAMETER;
        int layoutHeight = showNumbers ? (OUTER_DIAMETER + 9) : OUTER_DIAMETER;

        float scale = Mth.clamp(ClientHudConfig.divineRingScale, 0.5F, 2.0F);
        float opacity = Mth.clamp(ClientHudConfig.divineRingOpacity, 0.2F, 1.0F);

        HudLayoutMath.ScreenPos pos = HudLayoutMath.computeAnchoredPosition(
                ClientHudConfig.divineRingAnchor,
                ClientHudConfig.divineRingOffsetX,
                ClientHudConfig.divineRingOffsetY,
                layoutWidth,
                layoutHeight,
                scale,
                screenWidth,
                screenHeight
        );

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        try {
            pose.translate(pos.x(), pos.y(), 0.0F);
            pose.scale(scale, scale, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 1. Fundo circular (34x34)
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, opacity);
            guiGraphics.blit(FRAME_TEXTURE, 0, 0, 0.0F, 0.0F, OUTER_DIAMETER, OUTER_DIAMETER, OUTER_DIAMETER, OUTER_DIAMETER);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 2. Anel radial de Resistência Divina ou Sobrecarga
            boolean overloaded = snap.divineOverloadTicks() > 0;
            float pulse = anim.getPulseWave();
            float divineRatio = snap.divineRatio();

            if (overloaded) {
                int darkTrackCol = HudLayoutMath.withScaledAlpha(0x77450A0A, opacity);
                DivineRingGeometry.forEachFullRingSpan((rowY, x0, x1) ->
                        guiGraphics.fill(x0, rowY, x1, rowY + 1, darkTrackCol));

                float overloadRatio = Mth.clamp(
                        snap.divineOverloadTicks() / (float) DivineResonanceBalance.OVERLOAD_PENALTY_TICKS,
                        0.0F,
                        1.0F
                );
                int crimsonBase = pulseColor(0xFFDC2626, 0xFFEF4444, pulse);
                int overloadCol = HudLayoutMath.withScaledAlpha(crimsonBase, Math.min(1.0F, opacity + 0.1F));
                DivineRingGeometry.forEachActiveSpan(overloadRatio, (rowY, x0, x1) ->
                        guiGraphics.fill(x0, rowY, x1, rowY + 1, overloadCol));
            } else {
                HudAnimationState.BarTrack divineTrack = anim.getDivineResistanceTrack();
                float ghostRatio = divineTrack.getGhostRatio();
                float displayedRatio = divineTrack.getDisplayedRatio();

                if (ghostRatio > displayedRatio + 0.005F) {
                    int ghostCol = HudLayoutMath.withScaledAlpha(0x99FDE68A, opacity);
                    DivineRingGeometry.forEachActiveSpan(ghostRatio, (rowY, x0, x1) ->
                            guiGraphics.fill(x0, rowY, x1, rowY + 1, ghostCol));
                }

                int ringBaseColor = divineRatio < 0.30F
                        ? pulseColor(0xFFEA580C, 0xFFF97316, pulse)
                        : 0xFFF59E0B;
                int activeCol = HudLayoutMath.withScaledAlpha(ringBaseColor, Math.min(1.0F, opacity + 0.1F));
                DivineRingGeometry.forEachActiveSpan(displayedRatio, (rowY, x0, x1) ->
                        guiGraphics.fill(x0, rowY, x1, rowY + 1, activeCol));
            }

            // 2B. 4 Arcos Externos Segmentados das Fases I–IV (Mockup 1)
            int remnantCol = overloaded ? 0xFF991B1B : snap.equippedRemnant().getHudColor();
            int unlockedPhases = Mth.clamp(snap.unlockedDivinePhases(), 1, 4);
            int activePhase = Mth.clamp(snap.activeDivinePhase(), 0, unlockedPhases - 1);
            for (int seg = 0; seg < DivineRingGeometry.SEGMENT_COUNT; seg++) {
                int segColor;
                if (seg >= unlockedPhases) {
                    segColor = HudLayoutMath.withScaledAlpha(0x771E293B, opacity);
                } else if (seg == activePhase) {
                    int bright = pulseColor(remnantCol, 0xFFFFFFFF, 0.25F * pulse);
                    segColor = HudLayoutMath.withScaledAlpha(bright, Math.min(1.0F, opacity + 0.12F));
                } else {
                    segColor = HudLayoutMath.withScaledAlpha(remnantCol, opacity * 0.42F);
                }
                final int finalSegCol = segColor;
                DivineRingGeometry.forEachSegmentSpan(seg, (rowY, x0, x1) ->
                        guiGraphics.fill(x0, rowY, x1, rowY + 1, finalSegCol));
            }

            // 3. Ícone central 18x18 do Resquício Divino
            ResourceLocation emblemTex = getEmblemTexture(snap.equippedRemnant());
            if (overloaded) {
                guiGraphics.setColor(0.55F, 0.25F, 0.25F, opacity);
            } else {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, opacity + 0.1F));
            }
            guiGraphics.blit(emblemTex, 8, 8, 0.0F, 0.0F, 18, 18, 18, 18);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            // 4. Rachaduras da Sobrecarga ou Cooldown de recarga normal
            int cdTicks = snap.uniqueSkillCooldownTicks();
            if (overloaded) {
                float crackAlpha = opacity * (0.65F + 0.35F * pulse);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, crackAlpha);
                guiGraphics.blit(CRACKS_TEXTURE, 0, 0, 0.0F, 0.0F, OUTER_DIAMETER, OUTER_DIAMETER, OUTER_DIAMETER, OUTER_DIAMETER);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (cdTicks > 0) {
                guiGraphics.fill(8, 8, 26, 26, HudLayoutMath.withScaledAlpha(0xA8050810, opacity));
                String cdText = String.format(Locale.ROOT, "%.1fs", cdTicks / 20.0F);
                drawCenteredScaledString(guiGraphics, font, cdText, 17.0F, 14.5F, 0.68F, 0xFFFACC15, true);
            }

            // 5. Pill compacta da tecla [G] na borda superior direita
            String keyLabel = snap.skillKeyLabel();
            float keyTextW = font.width(keyLabel) * 0.60F;
            int keyPillW = Math.max(9, Math.round(keyTextW) + 4);
            int keyPillX = OUTER_DIAMETER - keyPillW + 2;
            int keyPillY = -2;
            guiGraphics.fill(keyPillX, keyPillY, keyPillX + keyPillW, keyPillY + 8, HudLayoutMath.withScaledAlpha(0xE00B0F17, opacity));
            guiGraphics.renderOutline(keyPillX, keyPillY, keyPillW, 8, overloaded ? 0xFFEF4444 : 0xFF64748B);
            drawCenteredScaledString(guiGraphics, font, keyLabel, keyPillX + keyPillW * 0.5F, keyPillY + 1.5F, 0.60F, 0xFFF1F5F9, true);

            // 6. Pill compacta da Fase selecionada + Camada desbloqueada (ex: H2·II) na borda inferior
            String phaseBadge = "H" + (activePhase + 1) + "·" + snap.tier().getNumeral();
            int tierTextW = Math.round(font.width(phaseBadge) * 0.56F);
            int tierPillW = Math.max(14, tierTextW + 4);
            int tierPillX = (OUTER_DIAMETER - tierPillW) / 2;
            int tierPillY = 27;
            int tierColor = overloaded ? 0xFFEF4444 : snap.equippedRemnant().getHudColor();
            guiGraphics.fill(tierPillX, tierPillY, tierPillX + tierPillW, tierPillY + 8, HudLayoutMath.withScaledAlpha(0xE60B0F17, opacity));
            guiGraphics.renderOutline(tierPillX, tierPillY, tierPillW, 8, HudLayoutMath.withScaledAlpha(tierColor, 0.9F));
            drawCenteredScaledString(guiGraphics, font, phaseBadge, 17.0F, tierPillY + 1.5F, 0.56F, tierColor, true);

            // 7. Leitura numérica compacta abaixo do anel (opcional)
            if (showNumbers) {
                String numLabel = overloaded
                        ? "SELADO " + Math.max(1, Mth.ceil(snap.divineOverloadTicks() / 20.0F)) + "s"
                        : String.format(Locale.ROOT, "%.0f/%.0f", snap.currentDivineResistance(), snap.maxDivineResistance());
                int numColor = overloaded ? 0xFFFCA5A5 : (divineRatio < 0.30F ? 0xFFFDBA74 : 0xFFFDE68A);
                int numBg = overloaded ? 0xCC18090C : 0xCC0B0F17;
                int pillW = Math.round(font.width(numLabel) * 0.60F) + 4;
                int pillX = (OUTER_DIAMETER - pillW) / 2;
                int pillY = 36;
                guiGraphics.fill(pillX, pillY, pillX + pillW, pillY + 7, HudLayoutMath.withScaledAlpha(numBg, opacity));
                drawCenteredScaledString(guiGraphics, font, numLabel, 17.0F, pillY + 1.0F, 0.60F, numColor, true);
            }
        } finally {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    private static ResourceLocation getEmblemTexture(DivineRemnant remnant) {
        if (remnant == null) {
            return EMBLEM_TEZCATLIPOCA_STEP;
        }
        return switch (remnant) {
            case EYE_OF_RA -> EMBLEM_EYE_OF_RA;
            case MORRIGAN_REAP -> EMBLEM_MORRIGAN_REAP;
            case INDRAS_VAJRA -> EMBLEM_INDRAS_VAJRA;
            case TEZCATLIPOCA_STEP -> EMBLEM_TEZCATLIPOCA_STEP;
        };
    }

    private static void drawCenteredScaledString(
            GuiGraphics g,
            Font font,
            String text,
            float centerX,
            float y,
            float scale,
            int color,
            boolean shadow
    ) {
        float w = font.width(text) * scale;
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(centerX - (w * 0.5F), y, 0.0F);
            pose.scale(scale, scale, 1.0F);
            g.drawString(font, text, 0, 0, color, shadow);
        } finally {
            pose.popPose();
        }
    }

    private static int pulseColor(int colA, int colB, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a = Math.round(Mth.lerp(clamped, (colA >>> 24) & 0xFF, (colB >>> 24) & 0xFF));
        int r = Math.round(Mth.lerp(clamped, (colA >>> 16) & 0xFF, (colB >>> 16) & 0xFF));
        int g = Math.round(Mth.lerp(clamped, (colA >>> 8) & 0xFF, (colB >>> 8) & 0xFF));
        int b = Math.round(Mth.lerp(clamped, colA & 0xFF, colB & 0xFF));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
