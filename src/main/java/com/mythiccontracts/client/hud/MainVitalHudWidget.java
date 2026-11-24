package com.mythiccontracts.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState;
import com.mythiccontracts.faction.FactionRegistry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Widget da HUD Principal — Conceito A (Minimalista Murim).
 * Exibe Facção, Infusão [V], Reinos Corpo/Espírito, Selos Subjugados e até 4 barras finas:
 * Vida Corporal (+Absorção), Integridade Espiritual, Qi e Vigor (Stamina).
 */
public final class MainVitalHudWidget {

    public static final int STANDARD_WIDTH = 216;
    public static final int COMPACT_WIDTH = 166;

    private static final ResourceLocation ICON_ATLAS =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/hud_icons.png");
    private static final int ATLAS_WIDTH = 64;
    private static final int ATLAS_HEIGHT = 16;

    private MainVitalHudWidget() {
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

        boolean compact = ClientHudConfig.compactMode;
        boolean showBody = ClientHudConfig.showBodyHealthBar;
        boolean showSpirit = ClientHudConfig.showSpiritualIntegrityBar;
        boolean showQi = ClientHudConfig.showQiBar;
        boolean showStamina = ClientHudConfig.showStaminaBar && !shouldHideStamina(snap, anim);
        boolean showMartialChip = snap.activeMartialSkillName() != null && !snap.activeMartialSkillName().isBlank();

        int widgetWidth = compact ? COMPACT_WIDTH : STANDARD_WIDTH;
        int widgetHeight = computeWidgetHeight(compact, showBody, showSpirit, showQi, showStamina, showMartialChip);

        float scale = Mth.clamp(ClientHudConfig.mainHudScale, 0.5F, 2.0F);
        float opacity = Mth.clamp(ClientHudConfig.mainHudOpacity, 0.2F, 1.0F);
        boolean smoothAnims = ClientHudConfig.enableSmoothAnimations;

        HudLayoutMath.ScreenPos pos = HudLayoutMath.computeAnchoredPosition(
                ClientHudConfig.mainHudAnchor,
                ClientHudConfig.mainHudOffsetX,
                ClientHudConfig.mainHudOffsetY,
                widgetWidth,
                widgetHeight,
                scale,
                screenWidth,
                screenHeight
        );

        float shakeX = anim.getDamageShakeOffsetX(smoothAnims);
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        try {
            pose.translate(pos.x() + shakeX, pos.y(), 0.0F);
            pose.scale(scale, scale, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            if (compact) {
                renderCompact(guiGraphics, font, snap, anim, widgetWidth, widgetHeight, opacity, showBody, showSpirit, showQi, showStamina, showMartialChip);
            } else {
                renderStandard(guiGraphics, font, snap, anim, widgetWidth, widgetHeight, opacity, showBody, showSpirit, showQi, showStamina, showMartialChip);
            }

            int extraBottomOffset = 0;
            if (snap.clashActive()) {
                renderSubjugationClashBar(guiGraphics, font, snap, anim, widgetWidth, widgetHeight, opacity);
                extraBottomOffset = 20;
            }
            renderEvolutionBanner(guiGraphics, font, anim, widgetWidth, widgetHeight + extraBottomOffset, opacity);
        } finally {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            pose.popPose();
        }
    }

    private static boolean shouldHideStamina(HudRenderSnapshot snap, HudAnimationState anim) {
        return ClientHudConfig.autoHideFullStamina
                && anim.getStaminaFullSeconds() >= 3.0F
                && snap.currentStamina() >= snap.maxStamina() - 0.05F;
    }

    private static int computeWidgetHeight(
            boolean compact,
            boolean showBody,
            boolean showSpirit,
            boolean showQi,
            boolean showStamina,
            boolean showMartialChip
    ) {
        if (compact) {
            int count = (showBody ? 1 : 0) + (showSpirit ? 1 : 0) + (showQi ? 1 : 0) + (showStamina ? 1 : 0);
            int rows = count == 0 ? 0 : (count + 1) / 2;
            int base = count == 0 ? 20 : (18 + rows * 13 + 3);
            return base + (showMartialChip ? 10 : 0);
        }
        int h = 29;
        if (showBody) {
            h += 12;
        }
        if (showSpirit) {
            h += 12;
        }
        if (showQi) {
            h += 12;
        }
        if (showStamina) {
            h += 10;
        }
        if (showMartialChip) {
            h += 11;
        }
        return h + 3;
    }

    private static void renderStandard(
            GuiGraphics g,
            Font font,
            HudRenderSnapshot snap,
            HudAnimationState anim,
            int width,
            int height,
            float opacity,
            boolean showBody,
            boolean showSpirit,
            boolean showQi,
            boolean showStamina,
            boolean showMartialChip
    ) {
        int accent = getFactionAccentColor(snap.faction());
        float borderFlash = anim.getBorderFlashSeconds();

        int bgCol = HudLayoutMath.withScaledAlpha(0xEB0B0F17, opacity);
        int baseOutline = borderFlash > 0.0F
                ? (borderFlash > 0.6F ? 0xFFFBBF24 : 0xFF34D399)
                : 0xAA334155;
        int outlineCol = borderFlash > 0.0F ? baseOutline : HudLayoutMath.withScaledAlpha(baseOutline, opacity);

        g.fill(0, 0, width, height, bgCol);
        g.fill(0, 0, 2, height, HudLayoutMath.withScaledAlpha(accent, Math.min(1.0F, opacity + 0.15F)));
        g.renderOutline(0, 0, width, height, outlineCol);

        // Linha 1: Facção + Badge Infusão [V]
        drawTintedAtlasIcon(g, 6, 4, 50, accent, Math.min(1.0F, opacity + 0.15F));
        drawScaledString(g, font, snap.faction().getDisplayName(), 17.0F, 5.0F, 0.88F, accent, true);

        if (snap.infusionActive()) {
            String badge = "[" + snap.infusionKeyLabel() + "] INFUSÃO ON";
            int badgeW = Math.round(font.width(badge) * 0.72F) + 6;
            int bx = width - badgeW - 5;
            g.fill(bx, 4, bx + badgeW, 13, HudLayoutMath.withScaledAlpha(0xCC0C2E42, opacity));
            g.renderOutline(bx, 4, badgeW, 9, 0xFF38BDF8);
            drawScaledString(g, font, badge, bx + 3.0F, 6.0F, 0.72F, 0xFF38BDF8, true);
        } else {
            String badge = "[" + snap.infusionKeyLabel() + "] OFF";
            drawRightScaledString(g, font, badge, width - 5.0F, 6.0F, 0.72F, 0xFF64748B, false);
        }

        // Linha 2: Reinos Corpo/Espírito + pips de etapa + Selos
        String bodyLabel = String.format(Locale.ROOT, "Corpo R%d-%d", snap.bodyRealm(), snap.bodyStage());
        drawScaledString(g, font, bodyLabel, 6.0F, 16.0F, 0.75F, 0xFFE2E8F0, true);
        int bodyPipsX = 6 + Math.round(font.width(bodyLabel) * 0.75F) + 3;
        drawStagePips(g, bodyPipsX, 17, snap.bodyStage(), 0xFFF87171);

        int pipeX = bodyPipsX + 14;
        drawScaledString(g, font, "|", pipeX, 15.5F, 0.75F, 0xFF475569, false);

        int spiritStartX = pipeX + 5;
        String spiritLabel = String.format(Locale.ROOT, "Esp. R%d-%d", snap.spiritRealm(), snap.spiritStage());
        drawScaledString(g, font, spiritLabel, spiritStartX, 16.0F, 0.75F, 0xFFE2E8F0, true);
        int spiritPipsX = spiritStartX + Math.round(font.width(spiritLabel) * 0.75F) + 3;
        drawStagePips(g, spiritPipsX, 17, snap.spiritStage(), 0xFFC084FC);

        String sealsText = snap.subjugatedCount() + "/" + snap.maxSubjugatedCapacity();
        int sealsW = Math.round(font.width(sealsText) * 0.75F);
        int sealIconX = width - 6 - sealsW - 11;
        g.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, opacity + 0.15F));
        g.blit(ICON_ATLAS, sealIconX, 15, 40.0F, 0.0F, 9, 9, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawRightScaledString(g, font, sealsText, width - 5.0F, 16.0F, 0.75F, 0xFFC084FC, true);

        // Divisor horizontal
        g.fill(5, 26, width - 5, 27, HudLayoutMath.withScaledAlpha(0x88334155, opacity));

        // 4 barras vitais
        int curY = 29;
        float pulse = anim.getPulseWave();

        if (showBody) {
            boolean critical = snap.bodyHealthRatio() < 0.25F;
            int fillCol = critical ? 0xFFB91C1C : 0xFFEF4444;
            int hiCol = critical ? 0xFFF87171 : 0xFFFCA5A5;
            int outline = critical
                    ? blendPulseColor(0xFF7F1D1D, 0xFFFCA5A5, pulse)
                    : 0x997F1D1D;
            String numText = String.format(Locale.ROOT, "%.0f/%.0f", snap.health(), snap.maxHealth());
            drawVitalBar(
                    g, font, 0, curY, 8,
                    anim.getBodyHealthTrack(),
                    0xCC2A1215, fillCol, hiCol, 0xAAFCA5A5, outline,
                    "Vida", 0xFFFEE2E2, numText, snap.absorption(), snap.maxHealth(),
                    false, opacity, width
            );
            curY += 12;
        }

        if (showSpirit) {
            SpiritRegenState regenState = snap.spiritRegenState();
            boolean shattered = snap.spiritShatterTicks() > 0 || regenState == SpiritRegenState.BLOCKED_SHATTER;
            boolean combatSlow = regenState == SpiritRegenState.COMBAT_SLOW;
            boolean meditating = regenState == SpiritRegenState.MEDITATING;
            boolean recovering = regenState == SpiritRegenState.RECOVERING || meditating;
            boolean critical = !shattered && snap.spiritualRatio() < 0.25F;
            boolean fractured = shattered || snap.spiritualRatio() < 0.30F;
            boolean combatFlash = anim.getSpiritCombatFlashSeconds() > 0.0F;
            boolean fullPulse = anim.getSpiritFullPulseSeconds() > 0.0F;

            int fillCol = shattered ? 0xFFF43F5E : 0xFFA855F7;
            int hiCol = shattered ? 0xFFFDA4AF : 0xFFE879F9;
            int outline;
            if (combatFlash) {
                outline = 0xFFF43F5E;
            } else if (shattered || critical) {
                outline = blendPulseColor(0xFF4C1D95, 0xFFF43F5E, pulse);
            } else {
                outline = 0x99581C87;
            }

            String label = shattered
                    ? String.format(Locale.ROOT, "COLAPSO %.1fs", snap.spiritShatterTicks() / 20.0F)
                    : "Espírito";
            int labelCol = shattered ? 0xFFFECDD3 : 0xFFF3E8FF;
            String numText = String.format(Locale.ROOT, "%.0f/%.0f", snap.spiritualIntegrity(), snap.maxSpiritualIntegrity());
            float iconDim = (shattered || combatSlow) ? 0.65F : 1.0F;
            float shimmerPhase = recovering ? anim.getSpiritShimmerPhase() : -1.0F;
            int shimmerCol = meditating ? 0x8867E8F9 : 0x66E9D5FF;
            float fullPulseRatio = fullPulse
                    ? Mth.clamp(anim.getSpiritFullPulseSeconds() / HudAnimationState.SPIRIT_FULL_PULSE_DURATION, 0.0F, 1.0F)
                    : 0.0F;

            drawVitalBar(
                    g, font, 10, curY, 8,
                    anim.getSpiritualIntegrityTrack(),
                    0xCC21122E, fillCol, hiCol, 0xAAF0ABFC, outline,
                    label, labelCol, numText, 0.0F, 1.0F,
                    fractured, opacity, width, iconDim, shimmerPhase, shimmerCol,
                    recovering ? (meditating ? 0xFF67E8F9 : 0xFFE9D5FF) : 0, fullPulseRatio
            );
            curY += 12;
        }

        if (showQi) {
            boolean inf = snap.infusionActive();
            int fillCol = inf ? 0xFF06B6D4 : 0xFF3B82F6;
            int hiCol = inf ? 0xFFA5F3FC : 0xFF93C5FD;
            int outline = inf ? blendPulseColor(0xFF0284C7, 0xFF67E8F9, pulse) : 0x991E3A8A;
            String numText = String.format(Locale.ROOT, "%.0f/%.0f", snap.currentQi(), snap.maxQi());
            drawVitalBar(
                    g, font, 20, curY, 8,
                    anim.getQiTrack(),
                    0xCC0F1D33, fillCol, hiCol, 0xAA93C5FD, outline,
                    "Qi", inf ? 0xFFE0F2FE : 0xFFDBEAFE, numText, 0.0F, 1.0F,
                    false, opacity, width
            );
            curY += 12;
        }

        if (showStamina) {
            boolean exhausted = snap.staminaRatio() < 0.25F;
            int fillCol = exhausted ? 0xFFF59E0B : 0xFF84CC16;
            int hiCol = exhausted ? 0xFFFDE68A : 0xFFBEF264;
            int outline = exhausted ? blendPulseColor(0xFF78350F, 0xFFFBBF24, pulse) : 0x99365314;
            String numText = String.format(Locale.ROOT, "%.0f/%.0f", snap.currentStamina(), snap.maxStamina());
            drawVitalBar(
                    g, font, 30, curY, 6,
                    anim.getStaminaTrack(),
                    0xCC182412, fillCol, hiCol, 0xAABEF264, outline,
                    "Vigor", 0xFFECFCCB, numText, 0.0F, 1.0F,
                    false, opacity, width
            );
            curY += 10;
        }

        if (showMartialChip) {
            int chipY = curY + 1;
            g.fill(5, chipY - 1, width - 5, chipY, HudLayoutMath.withScaledAlpha(0x66334155, opacity));
            String slotTag = "[" + snap.martialKeyLabel() + " · S" + (snap.activeMartialSlot() + 1) + "] ";
            drawScaledString(g, font, slotTag + snap.activeMartialSkillName(), 6.0F, chipY + 2.0F, 0.66F, 0xFFE2E8F0, true);
            if (snap.activeMartialCooldownTicks() > 0) {
                String cdStr = String.format(Locale.ROOT, "%.1fs", snap.activeMartialCooldownTicks() / 20.0F);
                drawRightScaledString(g, font, cdStr, width - 5.0F, chipY + 2.0F, 0.66F, 0xFFFBBF24, true);
            } else {
                drawRightScaledString(g, font, "Pronta", width - 5.0F, chipY + 2.0F, 0.64F, 0xFF34D399, true);
            }
        }
    }

    private static void drawVitalBar(
            GuiGraphics g,
            Font font,
            int iconU,
            int barY,
            int barH,
            HudAnimationState.BarTrack track,
            int trackBg,
            int fillColor,
            int highlightColor,
            int ghostColor,
            int outlineColor,
            String innerLabel,
            int innerLabelColor,
            String numText,
            float absorption,
            float maxHealthForAbsorption,
            boolean fractured,
            float opacity,
            int panelWidth
    ) {
        drawVitalBar(
                g, font, iconU, barY, barH, track, trackBg, fillColor, highlightColor, ghostColor, outlineColor,
                innerLabel, innerLabelColor, numText, absorption, maxHealthForAbsorption,
                fractured, opacity, panelWidth, 1.0F, -1.0F, 0x66E9D5FF, 0, 0.0F
        );
    }

    private static void drawVitalBar(
            GuiGraphics g,
            Font font,
            int iconU,
            int barY,
            int barH,
            HudAnimationState.BarTrack track,
            int trackBg,
            int fillColor,
            int highlightColor,
            int ghostColor,
            int outlineColor,
            String innerLabel,
            int innerLabelColor,
            String numText,
            float absorption,
            float maxHealthForAbsorption,
            boolean fractured,
            float opacity,
            int panelWidth,
            float iconBrightness,
            float continuousShimmerPhase,
            int continuousShimmerColor,
            int chevronColor,
            float fullPulseRatio
    ) {
        int iconY = barH <= 6 ? barY - 1 : barY;
        g.setColor(iconBrightness, iconBrightness, iconBrightness, Math.min(1.0F, opacity + 0.15F));
        g.blit(ICON_ATLAS, 6, iconY, (float) iconU, 0.0F, 9, 9, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int barX = 18;
        int barW = 136;

        g.fill(barX, barY, barX + barW, barY + barH, HudLayoutMath.withScaledAlpha(trackBg, Math.min(1.0F, opacity + 0.12F)));

        float ghostRatio = track.getGhostRatio();
        float dispRatio = track.getDisplayedRatio();

        int ghostW = Math.round(barW * ghostRatio);
        int fillW = Math.round(barW * dispRatio);

        if (ghostW > fillW) {
            g.fill(barX + fillW, barY, barX + ghostW, barY + barH, ghostColor);
        }
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, fillColor);
            if (barH >= 6) {
                g.fill(barX, barY, barX + fillW, barY + 1, highlightColor);
            }
        }

        // Sobreposição dourada de Absorção na Vida Corporal
        if (absorption > 0.1F && maxHealthForAbsorption > 0.0F) {
            float absRatio = Mth.clamp(absorption / maxHealthForAbsorption, 0.0F, 1.0F);
            int absW = Math.max(2, Math.round(barW * absRatio));
            g.fill(barX, barY + barH - 2, barX + absW, barY + barH, 0xFFFACC15);
        }

        // Brilho contínuo proporcional de recuperação espiritual (Opção A)
        if (continuousShimmerPhase >= 0.0F && fillW > 10) {
            int sw = 12;
            int sx = barX + Math.round((fillW - sw) * Mth.clamp(continuousShimmerPhase, 0.0F, 1.0F));
            int clampedStart = Math.max(barX, sx);
            int clampedEnd = Math.min(barX + fillW, sx + sw);
            if (clampedEnd > clampedStart) {
                g.fill(clampedStart, barY, clampedEnd, barY + barH, continuousShimmerColor);
            }
        } else {
            // Brilho deslizante pontual de cura instantânea
            float shimmer = track.getHealShimmerSeconds();
            if (shimmer > 0.0F && fillW > 6) {
                float progress = 1.0F - (shimmer / HudAnimationState.HEAL_SHIMMER_DURATION);
                int sw = 10;
                int sx = barX + Math.round((fillW - sw) * progress);
                int clampedStart = Math.max(barX, sx);
                int clampedEnd = Math.min(barX + fillW, sx + sw);
                if (clampedEnd > clampedStart) {
                    g.fill(clampedStart, barY, clampedEnd, barY + barH, 0x66FFFFFF);
                }
            }
        }

        // Marcas de fratura quando Integridade Espiritual < 30% ou Colapso Espiritual
        if (fractured) {
            for (int i = 1; i <= 4; i++) {
                int fx = barX + (barW * i) / 5;
                g.fill(fx, barY + 1, fx + 1, barY + barH - 1, 0xBB18081E);
            }
        }

        g.renderOutline(barX, barY, barW, barH, outlineColor);
        if (fullPulseRatio > 0.0F) {
            int topPulseCol = HudLayoutMath.withScaledAlpha(0xFFF5D0FE, fullPulseRatio);
            g.fill(barX, barY, barX + barW, barY + 1, topPulseCol);
        }

        float innerTextY = barH <= 6 ? barY + 0.5F : barY + 1.2F;
        drawScaledString(g, font, innerLabel, barX + 3.0F, innerTextY, 0.68F, innerLabelColor, true);

        if (chevronColor != 0) {
            drawScaledString(g, font, "↑", barX + barW + 2.0F, barY + 1.0F, 0.65F, chevronColor, true);
        }

        // Leituras numéricas à direita (tabular)
        float rightEdge = panelWidth - 5.0F;
        if (absorption >= 0.5F) {
            String absText = String.format(Locale.ROOT, "+%.0f", absorption);
            int absWidth = Math.round(font.width(absText) * 0.68F);
            drawRightScaledString(g, font, absText, rightEdge, barY + 1.0F, 0.68F, 0xFFFACC15, true);
            drawRightScaledString(g, font, numText, rightEdge - absWidth - 1.5F, barY + 1.0F, 0.68F, 0xFFF1F5F9, true);
        } else {
            drawRightScaledString(g, font, numText, rightEdge, barY + (barH <= 6 ? 0.0F : 1.0F), 0.72F, 0xFFE2E8F0, true);
        }
    }

    private static void renderCompact(
            GuiGraphics g,
            Font font,
            HudRenderSnapshot snap,
            HudAnimationState anim,
            int width,
            int height,
            float opacity,
            boolean showBody,
            boolean showSpirit,
            boolean showQi,
            boolean showStamina,
            boolean showMartialChip
    ) {
        int accent = getFactionAccentColor(snap.faction());
        float pulse = anim.getPulseWave();

        g.fill(0, 0, width, height, HudLayoutMath.withScaledAlpha(0xEB0B0F17, opacity));
        g.fill(0, 0, 2, height, HudLayoutMath.withScaledAlpha(accent, Math.min(1.0F, opacity + 0.15F)));
        g.renderOutline(0, 0, width, height, HudLayoutMath.withScaledAlpha(0xAA334155, opacity));

        String compactHeader = String.format(Locale.ROOT, "C%d-%d / E%d-%d",
                snap.bodyRealm(), snap.bodyStage(), snap.spiritRealm(), snap.spiritStage());
        drawScaledString(g, font, compactHeader, 5.0F, 4.5F, 0.78F, 0xFFE2E8F0, true);

        String inf = "[" + snap.infusionKeyLabel() + "]" + (snap.infusionActive() ? "●" : "○");
        int infCol = snap.infusionActive() ? 0xFF38BDF8 : 0xFF64748B;
        drawScaledString(g, font, inf, 95.0F, 4.5F, 0.75F, infCol, true);

        String seals = "S:" + snap.subjugatedCount() + "/" + snap.maxSubjugatedCapacity();
        drawRightScaledString(g, font, seals, width - 5.0F, 4.5F, 0.72F, 0xFFC084FC, true);

        g.fill(5, 14, width - 5, 15, HudLayoutMath.withScaledAlpha(0x88334155, opacity));

        int slotIndex = 0;
        if (showBody) {
            boolean critical = snap.bodyHealthRatio() < 0.25F;
            int col = critical ? 0xFFB91C1C : 0xFFEF4444;
            int x = (slotIndex % 2 == 0) ? 5 : 85;
            int y = 18 + (slotIndex / 2) * 13;
            drawCompactBarCell(g, font, x, y, 0, anim.getBodyHealthTrack(), 0xCC2A1215, col,
                    String.format(Locale.ROOT, "%.0f", snap.health()), 0xFFFEE2E2, opacity, 1.0F, -1.0F, 0, 0x77FFFFFF, false);
            slotIndex++;
        }
        if (showSpirit) {
            SpiritRegenState regenState = snap.spiritRegenState();
            boolean shattered = snap.spiritShatterTicks() > 0 || regenState == SpiritRegenState.BLOCKED_SHATTER;
            boolean recovering = regenState == SpiritRegenState.RECOVERING || regenState == SpiritRegenState.MEDITATING;
            int col = shattered ? 0xFFF43F5E : 0xFFA855F7;
            int x = (slotIndex % 2 == 0) ? 5 : 85;
            int y = 18 + (slotIndex / 2) * 13;
            String val = shattered
                    ? String.format(Locale.ROOT, "%.0fs", snap.spiritShatterTicks() / 20.0F)
                    : String.format(Locale.ROOT, "%.0f", snap.spiritualIntegrity());
            int outlineCol = anim.getSpiritCombatFlashSeconds() > 0.0F
                    ? 0xFFF43F5E
                    : (anim.getSpiritFullPulseSeconds() > 0.0F ? 0xFFF5D0FE : 0x77FFFFFF);
            float iconDim = (shattered || regenState == SpiritRegenState.COMBAT_SLOW) ? 0.65F : 1.0F;
            drawCompactBarCell(g, font, x, y, 10, anim.getSpiritualIntegrityTrack(), 0xCC21122E, col,
                    val, 0xFFF3E8FF, opacity, iconDim,
                    recovering ? anim.getSpiritShimmerPhase() : -1.0F,
                    regenState == SpiritRegenState.MEDITATING ? 0x8867E8F9 : 0x66E9D5FF,
                    outlineCol, true);
            slotIndex++;
        }
        if (showQi) {
            int col = snap.infusionActive() ? 0xFF06B6D4 : 0xFF3B82F6;
            int x = (slotIndex % 2 == 0) ? 5 : 85;
            int y = 18 + (slotIndex / 2) * 13;
            drawCompactBarCell(g, font, x, y, 20, anim.getQiTrack(), 0xCC0F1D33, col,
                    String.format(Locale.ROOT, "%.0f", snap.currentQi()), 0xFFDBEAFE, opacity, 1.0F, -1.0F, 0, 0x77FFFFFF, false);
            slotIndex++;
        }
        if (showStamina) {
            int col = snap.staminaRatio() < 0.25F
                    ? blendPulseColor(0xFFD97706, 0xFFFBBF24, pulse)
                    : 0xFF84CC16;
            int x = (slotIndex % 2 == 0) ? 5 : 85;
            int y = 18 + (slotIndex / 2) * 13;
            drawCompactBarCell(g, font, x, y, 30, anim.getStaminaTrack(), 0xCC182412, col,
                    String.format(Locale.ROOT, "%.0f", snap.currentStamina()), 0xFFECFCCB, opacity, 1.0F, -1.0F, 0, 0x77FFFFFF, false);
            slotIndex++;
        }
        if (showMartialChip) {
            int chipY = height - 10;
            String label = "[ALT+G] " + snap.activeMartialSkillName();
            if (label.length() > 21) {
                label = label.substring(0, 20) + "…";
            }
            drawScaledString(g, font, label, 5.0F, chipY + 1.0F, 0.62F, 0xFFCBD5E1, true);
            String stat = snap.activeMartialCooldownTicks() > 0
                    ? String.format(Locale.ROOT, "%.0fs", snap.activeMartialCooldownTicks() / 20.0F)
                    : "OK";
            drawRightScaledString(g, font, stat, width - 5.0F, chipY + 1.0F, 0.62F,
                    snap.activeMartialCooldownTicks() > 0 ? 0xFFFBBF24 : 0xFF34D399, true);
        }
    }

    private static void drawCompactBarCell(
            GuiGraphics g,
            Font font,
            int x,
            int y,
            int iconU,
            HudAnimationState.BarTrack track,
            int trackBg,
            int fillColor,
            String valueText,
            int valueColor,
            float opacity,
            float iconBrightness,
            float shimmerPhase,
            int shimmerCol,
            int customOutline,
            boolean useCustomOutline
    ) {
        g.setColor(iconBrightness, iconBrightness, iconBrightness, Math.min(1.0F, opacity + 0.15F));
        g.blit(ICON_ATLAS, x, y, (float) iconU, 0.0F, 9, 9, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int barX = x + 11;
        int barW = 46;
        int barH = 7;
        g.fill(barX, y + 1, barX + barW, y + 1 + barH, HudLayoutMath.withScaledAlpha(trackBg, opacity + 0.1F));
        int fillW = Math.round(barW * track.getDisplayedRatio());
        if (fillW > 0) {
            g.fill(barX, y + 1, barX + fillW, y + 1 + barH, fillColor);
            if (shimmerPhase >= 0.0F && fillW > 6) {
                int sw = 8;
                int sx = barX + Math.round((fillW - sw) * Mth.clamp(shimmerPhase, 0.0F, 1.0F));
                int cs = Math.max(barX, sx);
                int ce = Math.min(barX + fillW, sx + sw);
                if (ce > cs) {
                    g.fill(cs, y + 1, ce, y + 1 + barH, shimmerCol);
                }
            }
        }
        g.renderOutline(barX, y + 1, barW, barH, useCustomOutline ? customOutline : 0x77FFFFFF);
        drawScaledString(g, font, valueText, barX + barW + 2.0F, y + 1.5F, 0.70F, valueColor, true);
    }

    private static void renderSubjugationClashBar(
            GuiGraphics g,
            Font font,
            HudRenderSnapshot snap,
            HudAnimationState anim,
            int widgetWidth,
            int widgetHeight,
            float opacity
    ) {
        int y = widgetHeight + 3;
        int h = 16;
        float rawRatio = Mth.clamp(snap.clashProgress(), 0.0F, 1.0F);
        float fillRatio = Mth.clamp(anim.getClashDominanceTrack().getDisplayedRatio(), 0.0F, 1.0F);
        boolean revolt = snap.clashRevoltWindow();
        int border = revolt ? blendPulseColor(0xFFA855F7, 0xFFF43F5E, anim.getPulseWave()) : 0xFFA855F7;

        g.fill(0, y, widgetWidth, y + h, HudLayoutMath.withScaledAlpha(0xEB0B0F17, Math.min(1.0F, opacity + 0.15F)));
        g.renderOutline(0, y, widgetWidth, h, border);

        String title = revolt
                ? "PULSO! Pressione ALT+G"
                : ("⛓ " + (snap.clashTargetName().isBlank() ? "Disputa" : snap.clashTargetName()));
        drawScaledString(g, font, title, 5.0F, y + 2.0F, 0.62F, revolt ? 0xFFFDA4AF : 0xFFE9D5FF, true);
        drawRightScaledString(g, font, Math.round(rawRatio * 100.0F) + "%", widgetWidth - 5.0F, y + 2.0F, 0.62F, 0xFF22D3EE, true);

        int barX = 5;
        int barY = y + 9;
        int barW = widgetWidth - 10;
        int barH = 5;
        int fillW = Math.round(barW * fillRatio);
        g.fill(barX, barY, barX + barW, barY + barH, 0xCC991B1B);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + barH, 0xFF22D3EE);
        }
        g.renderOutline(barX, barY, barW, barH, 0xAA334155);
    }

    private static void renderEvolutionBanner(
            GuiGraphics g,
            Font font,
            HudAnimationState anim,
            int widgetWidth,
            int widgetHeight,
            float opacity
    ) {
        float remaining = anim.getBannerRemainingSeconds();
        if (remaining <= 0.0F || anim.getBannerText().isBlank()) {
            return;
        }
        float fade = remaining < 0.60F ? Mth.clamp(remaining / 0.60F, 0.0F, 1.0F) : 1.0F;
        int bannerY = widgetHeight + 4;
        int bannerH = 14;
        int bg = HudLayoutMath.withScaledAlpha(0xEB0B0F17, Math.min(1.0F, opacity + 0.15F) * fade);
        int border = HudLayoutMath.withScaledAlpha(anim.getBannerColor(), fade);
        int textCol = HudLayoutMath.withScaledAlpha(anim.getBannerColor(), fade);

        g.fill(0, bannerY, widgetWidth, bannerY + bannerH, bg);
        g.renderOutline(0, bannerY, widgetWidth, bannerH, border);
        float tw = font.width(anim.getBannerText()) * 0.78F;
        drawScaledString(g, font, anim.getBannerText(), Math.max(4.0F, (widgetWidth - tw) * 0.5F), bannerY + 3.5F, 0.78F, textCol, true);
    }

    private static void drawTintedAtlasIcon(GuiGraphics g, int x, int y, int u, int argb, float alpha) {
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float gCol = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        g.setColor(r, gCol, b, Mth.clamp(alpha, 0.0F, 1.0F));
        g.blit(ICON_ATLAS, x, y, (float) u, 0.0F, 9, 9, ATLAS_WIDTH, ATLAS_HEIGHT);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawStagePips(GuiGraphics g, int startX, int y, int stage, int activeColor) {
        int clamped = Mth.clamp(stage, 1, 3);
        for (int i = 1; i <= 3; i++) {
            int px = startX + (i - 1) * 4;
            if (i <= clamped) {
                g.fill(px, y, px + 3, y + 3, activeColor);
            } else {
                g.fill(px, y, px + 3, y + 3, 0xAA1E293B);
                g.renderOutline(px, y, 3, 3, 0xFF475569);
            }
        }
    }

    private static void drawScaledString(
            GuiGraphics g,
            Font font,
            String text,
            float x,
            float y,
            float scale,
            int color,
            boolean shadow
    ) {
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0.0F);
            pose.scale(scale, scale, 1.0F);
            g.drawString(font, text, 0, 0, color, shadow);
        } finally {
            pose.popPose();
        }
    }

    private static void drawRightScaledString(
            GuiGraphics g,
            Font font,
            String text,
            float rightEdge,
            float y,
            float scale,
            int color,
            boolean shadow
    ) {
        float width = font.width(text) * scale;
        drawScaledString(g, font, text, rightEdge - width, y, scale, color, shadow);
    }

    private static int blendPulseColor(int colA, int colB, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a = Math.round(Mth.lerp(clamped, (colA >>> 24) & 0xFF, (colB >>> 24) & 0xFF));
        int r = Math.round(Mth.lerp(clamped, (colA >>> 16) & 0xFF, (colB >>> 16) & 0xFF));
        int g = Math.round(Mth.lerp(clamped, (colA >>> 8) & 0xFF, (colB >>> 8) & 0xFF));
        int b = Math.round(Mth.lerp(clamped, colA & 0xFF, colB & 0xFF));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int getFactionAccentColor(FactionType faction) {
        return FactionRegistry.getForType(faction).accentColor();
    }
}
