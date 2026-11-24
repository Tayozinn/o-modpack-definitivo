package com.mythiccontracts.client.hud;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria pré-calculada em tempo de carga para o anel radial de Resistência Divina ({@code 34×34 px})
 * e os 4 arcos externos segmentados das Fases I–IV (Mockup 1), sem alocação por frame nem shaders.
 */
public final class DivineRingGeometry {

    public static final int DIAMETER = 34;
    public static final float CENTER = 17.0F;
    public static final float INNER_RADIUS = 10.6F;
    public static final float OUTER_RADIUS = 13.0F;
    public static final float PHASE_INNER_RADIUS = 13.7F;
    public static final float PHASE_OUTER_RADIUS = 16.4F;
    public static final int SEGMENT_COUNT = 4;

    @FunctionalInterface
    public interface SpanConsumer {
        void accept(int rowY, int xStart, int xEndExclusive);
    }

    private record RingPixel(int x, float normAngle) {
    }

    private static final RingPixel[][] ROWS = buildRows(INNER_RADIUS, OUTER_RADIUS);
    private static final int[][][] SEGMENT_SPANS = buildSegmentSpans();

    private DivineRingGeometry() {
    }

    private static RingPixel[][] buildRows(float innerR, float outerR) {
        RingPixel[][] rows = new RingPixel[DIAMETER][];
        for (int py = 0; py < DIAMETER; py++) {
            List<RingPixel> rowPixels = new ArrayList<>();
            float dy = (py + 0.5F) - CENTER;
            for (int px = 0; px < DIAMETER; px++) {
                float dx = (px + 0.5F) - CENTER;
                double dist = Math.hypot(dx, dy);
                if (dist >= innerR && dist <= outerR) {
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

    private static int[][][] buildSegmentSpans() {
        RingPixel[][] outerRows = buildRows(PHASE_INNER_RADIUS, PHASE_OUTER_RADIUS);
        int[][][] segments = new int[SEGMENT_COUNT][DIAMETER][];
        for (int seg = 0; seg < SEGMENT_COUNT; seg++) {
            float minAngle = seg * 0.25F + 0.025F;
            float maxAngle = (seg + 1) * 0.25F - 0.025F;
            for (int py = 0; py < DIAMETER; py++) {
                RingPixel[] row = outerRows[py];
                List<Integer> pairs = new ArrayList<>();
                int spanStart = -1;
                int spanEnd = -1;
                for (RingPixel p : row) {
                    boolean inSeg = p.normAngle >= minAngle && p.normAngle <= maxAngle;
                    if (inSeg) {
                        if (spanStart < 0) {
                            spanStart = p.x;
                            spanEnd = p.x + 1;
                        } else if (p.x == spanEnd) {
                            spanEnd = p.x + 1;
                        } else {
                            pairs.add(spanStart);
                            pairs.add(spanEnd);
                            spanStart = p.x;
                            spanEnd = p.x + 1;
                        }
                    } else if (spanStart >= 0) {
                        pairs.add(spanStart);
                        pairs.add(spanEnd);
                        spanStart = -1;
                        spanEnd = -1;
                    }
                }
                if (spanStart >= 0) {
                    pairs.add(spanStart);
                    pairs.add(spanEnd);
                }
                int[] arr = new int[pairs.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = pairs.get(i);
                }
                segments[seg][py] = arr;
            }
        }
        return segments;
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

    public static void forEachSegmentSpan(int segmentIndex, SpanConsumer consumer) {
        if (consumer == null || segmentIndex < 0 || segmentIndex >= SEGMENT_COUNT) {
            return;
        }
        int[][] rows = SEGMENT_SPANS[segmentIndex];
        for (int py = 0; py < DIAMETER; py++) {
            int[] spans = rows[py];
            for (int i = 0; i + 1 < spans.length; i += 2) {
                consumer.accept(py, spans[i], spans[i + 1]);
            }
        }
    }
}
