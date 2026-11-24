package com.mythiccontracts.client.hud;

import com.mythiccontracts.client.config.HudAnchor;
import net.minecraft.util.Mth;

/**
 * Matemática pura de posicionamento por âncora e manipulação de transparência ARGB.
 * Testável em JUnit 5 sem contexto gráfico OpenGL/Minecraft.
 */
public final class HudLayoutMath {

    public record ScreenPos(int x, int y) {
    }

    private HudLayoutMath() {
    }

    public static ScreenPos computeAnchoredPosition(
            HudAnchor anchor,
            int offsetX,
            int offsetY,
            int widgetWidth,
            int widgetHeight,
            float scale,
            int screenWidth,
            int screenHeight
    ) {
        HudAnchor safeAnchor = anchor != null ? anchor : HudAnchor.TOP_LEFT;
        float safeScale = Math.max(0.25F, scale);
        int scaledW = Math.max(1, Math.round(widgetWidth * safeScale));
        int scaledH = Math.max(1, Math.round(widgetHeight * safeScale));

        int rawX = switch (safeAnchor) {
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - scaledW - offsetX;
        };

        int rawY = switch (safeAnchor) {
            case TOP_LEFT, TOP_RIGHT -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - scaledH - offsetY;
        };

        int maxX = Math.max(0, screenWidth - scaledW);
        int maxY = Math.max(0, screenHeight - scaledH);
        return new ScreenPos(
                Mth.clamp(rawX, 0, maxX),
                Mth.clamp(rawY, 0, maxY)
        );
    }

    public static int withScaledAlpha(int argb, float opacityMultiplier) {
        float clampedMult = Mth.clamp(opacityMultiplier, 0.0F, 1.0F);
        int alpha = (argb >>> 24) & 0xFF;
        int scaledAlpha = Mth.clamp(Math.round(alpha * clampedMult), 0, 255);
        return (scaledAlpha << 24) | (argb & 0x00FFFFFF);
    }
}
