package com.mythiccontracts.client.hud;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria pré-calculada em tempo de carga para o anel radial de Resistência Divina ({@code 34×34 px}).
 * Emite faixas horizontais contíguas em sentido horário (a partir das 12h) sem nenhuma alocação por frame
 * e sem shaders customizados (100% compatível com {@code GuiGraphics} e Embeddium 0.3.31).
 */
public final class DivineRingGeometry {

    public static final int DIAMETER = 34;
    public static final float CENTER = 17.0F;
    public static final float INNER_RADIUS = 12.2F;
    public static final float OUTER_RADIUS = 16.0F;

    @FunctionalInterface
    public interface SpanConsumer {
        void accept(int rowY, int xStart, int xEndExclusive);
    }

    private record RingPixel(int x, float normAngle) {
    }

    private static final RingPixel[][] ROWS = buildRows();

    private DivineRingGeometry() {
    }

    private static RingPixel[][] buildRows() {
        RingPixel[][] rows = new RingPixel[DIAMETER][];
        for (int py = 0; py < DIAMETER; py++) {
            List<RingPixel> rowPixels = new ArrayList<>();
            float dy = (py + 0.5F) - CENTER;
            for (int px = 0; px < DIAMETER; px++) {
                float dx = (px + 0.5F) - CENTER;
                double dist = Math.hypot(dx, dy);
                if (dist >= INNER_RADIUS && dist <= OUTER_RADIUS) {
                    double rad = Math.atan2(dx, -dy);
                    if (rad < 0.0D) {
                        rad += 2.0D * Math.PI;
                    }
                    float norm = (float) (rad / (2.0D * Math.PI));
                    rowPixels.add(new RingPixel(px, norm));
                }
            }
            rows[py] = rowPixels.toArray(new RingPixel[0]);
        }
        return rows;
    }

    public static void forEachActiveSpan(float fillRatio, SpanConsumer consumer) {
        if (consumer == null) {
            return;
        }
        float clamped = Mth.clamp(fillRatio, 0.0F, 1.0F);
        if (clamped <= 0.001F) {
            return;
        }
        boolean fullCircle = clamped >= 0.999F;

        for (int py = 0; py < DIAMETER; py++) {
            RingPixel[] row = ROWS[py];
            if (row.length == 0) {
                continue;
            }
            int spanStart = -1;
            int spanEnd = -1;
            for (int i = 0; i < row.length; i++) {
                RingPixel p = row[i];
                boolean active = fullCircle || p.normAngle <= clamped;
                if (active) {
                    if (spanStart < 0) {
                        spanStart = p.x;
                        spanEnd = p.x + 1;
                    } else if (p.x == spanEnd) {
                        spanEnd = p.x + 1;
                    } else {
                        consumer.accept(py, spanStart, spanEnd);
                        spanStart = p.x;
                        spanEnd = p.x + 1;
                    }
                } else if (spanStart >= 0) {
                    consumer.accept(py, spanStart, spanEnd);
                    spanStart = -1;
                    spanEnd = -1;
                }
            }
            if (spanStart >= 0) {
                consumer.accept(py, spanStart, spanEnd);
            }
        }
    }

    public static void forEachFullRingSpan(SpanConsumer consumer) {
        forEachActiveSpan(1.0F, consumer);
    }
}
