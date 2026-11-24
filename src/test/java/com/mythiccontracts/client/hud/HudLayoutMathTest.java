package com.mythiccontracts.client.hud;

import com.mythiccontracts.client.config.HudAnchor;
import com.mythiccontracts.client.hud.HudLayoutMath.ScreenPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudLayoutMathTest {

    @Test
    void computesAllFourAnchorsWithScale() {
        int sw = 480;
        int sh = 270;
        int w = 216;
        int h = 78;

        assertEquals(new ScreenPos(8, 8),
                HudLayoutMath.computeAnchoredPosition(HudAnchor.TOP_LEFT, 8, 8, w, h, 1.0F, sw, sh));
        assertEquals(new ScreenPos(480 - 216 - 8, 8),
                HudLayoutMath.computeAnchoredPosition(HudAnchor.TOP_RIGHT, 8, 8, w, h, 1.0F, sw, sh));
        assertEquals(new ScreenPos(8, 270 - 78 - 8),
                HudLayoutMath.computeAnchoredPosition(HudAnchor.BOTTOM_LEFT, 8, 8, w, h, 1.0F, sw, sh));
        assertEquals(new ScreenPos(480 - 216 - 8, 270 - 78 - 8),
                HudLayoutMath.computeAnchoredPosition(HudAnchor.BOTTOM_RIGHT, 8, 8, w, h, 1.0F, sw, sh));

        // Escala 1.5x e 0.5x
        ScreenPos rightScaledUp = HudLayoutMath.computeAnchoredPosition(HudAnchor.TOP_RIGHT, 10, 5, 34, 43, 1.5F, sw, sh);
        assertEquals(new ScreenPos(480 - Math.round(34 * 1.5F) - 10, 5), rightScaledUp);

        ScreenPos bottomScaledDown = HudLayoutMath.computeAnchoredPosition(HudAnchor.BOTTOM_LEFT, 10, 10, 34, 43, 0.5F, sw, sh);
        assertEquals(new ScreenPos(10, 270 - Math.round(43 * 0.5F) - 10), bottomScaledDown);
    }

    @Test
    void clampsPositionsInsideNarrowViewports() {
        ScreenPos clamped = HudLayoutMath.computeAnchoredPosition(HudAnchor.TOP_LEFT, 500, 300, 216, 78, 1.0F, 320, 200);
        assertEquals(new ScreenPos(320 - 216, 200 - 78), clamped);

        ScreenPos tinyViewport = HudLayoutMath.computeAnchoredPosition(HudAnchor.BOTTOM_RIGHT, 50, 50, 250, 250, 1.0F, 200, 180);
        assertEquals(new ScreenPos(0, 0), tinyViewport);
    }

    @Test
    void scalesArgbAlphaWithoutModifyingRgbChannels() {
        int input = 0xFF38BDF8;
        int half = HudLayoutMath.withScaledAlpha(input, 0.5F);
        assertEquals(0x80, (half >>> 24) & 0xFF);
        assertEquals(0x38BDF8, half & 0x00FFFFFF);

        assertEquals(0x0038BDF8, HudLayoutMath.withScaledAlpha(input, -0.2F));
        assertEquals(0xFF38BDF8, HudLayoutMath.withScaledAlpha(input, 1.5F));
    }

    @Test
    void defaultMainHudDivineRingAndHotbarDoNotOverlapOn720pAnd1080p() {
        int[][] viewports = {{427, 240}, {480, 270}}; // 1280x720@3x, 1920x1080@4x
        for (int[] vp : viewports) {
            int sw = vp[0];
            int sh = vp[1];

            int mainW = MainVitalHudWidget.STANDARD_WIDTH;
            int mainH = 78;
            ScreenPos mainPos = HudLayoutMath.computeAnchoredPosition(HudAnchor.TOP_LEFT, 8, 8, mainW, mainH, 1.0F, sw, sh);

            int ringW = DivineResistanceRingWidget.OUTER_DIAMETER;
            int ringH = DivineResistanceRingWidget.OUTER_DIAMETER + 9;
            ScreenPos ringPos = HudLayoutMath.computeAnchoredPosition(HudAnchor.BOTTOM_LEFT, 10, 10, ringW, ringH, 1.0F, sw, sh);

            int hotbarX1 = (sw / 2) - 91;
            int hotbarX2 = (sw / 2) + 91;
            int hotbarY1 = sh - 22;
            int hotbarY2 = sh;

            // Main HUD vs Divine Ring
            assertFalse(rectanglesOverlap(
                    mainPos.x(), mainPos.y(), mainPos.x() + mainW, mainPos.y() + mainH,
                    ringPos.x(), ringPos.y(), ringPos.x() + ringW, ringPos.y() + ringH
            ), "Main HUD and Divine Ring overlapped at " + sw + "x" + sh);

            // Divine Ring vs Vanilla Hotbar
            assertFalse(rectanglesOverlap(
                    ringPos.x(), ringPos.y(), ringPos.x() + ringW, ringPos.y() + ringH,
                    hotbarX1, hotbarY1, hotbarX2, hotbarY2
            ), "Divine Ring overlapped Vanilla Hotbar at " + sw + "x" + sh);
        }
    }

    private static boolean rectanglesOverlap(int ax1, int ay1, int ax2, int ay2, int bx1, int by1, int bx2, int by2) {
        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }
}
