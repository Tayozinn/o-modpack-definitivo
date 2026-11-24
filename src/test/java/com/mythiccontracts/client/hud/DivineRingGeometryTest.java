package com.mythiccontracts.client.hud;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DivineRingGeometryTest {

    @Test
    void zeroFillEmitsNoSpans() {
        AtomicInteger count = new AtomicInteger();
        DivineRingGeometry.forEachActiveSpan(0.0F, (y, x0, x1) -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    void fullRingCoversSymmetricalAnnulusWithin34PxBounds() {
        AtomicInteger totalPixels = new AtomicInteger();
        AtomicInteger leftPixels = new AtomicInteger();
        AtomicInteger rightPixels = new AtomicInteger();

        DivineRingGeometry.forEachFullRingSpan((y, x0, x1) -> {
            assertTrue(y >= 0 && y < DivineRingGeometry.DIAMETER);
            assertTrue(x0 >= 0 && x0 < x1 && x1 <= DivineRingGeometry.DIAMETER);
            float dy = (y + 0.5F) - DivineRingGeometry.CENTER;
            for (int x = x0; x < x1; x++) {
                float dx = (x + 0.5F) - DivineRingGeometry.CENTER;
                double dist = Math.hypot(dx, dy);
                assertTrue(dist >= DivineRingGeometry.INNER_RADIUS && dist <= DivineRingGeometry.OUTER_RADIUS,
                        "Pixel outside annulus radius: dist=" + dist);
                totalPixels.incrementAndGet();
                if (x < 17) {
                    leftPixels.incrementAndGet();
                } else {
                    rightPixels.incrementAndGet();
                }
            }
        });

        assertTrue(totalPixels.get() > 250);
        assertEquals(leftPixels.get(), rightPixels.get());
    }

    @Test
    void halfFillCoversClockwiseRightHalfOnly() {
        AtomicInteger fullPixels = new AtomicInteger();
        DivineRingGeometry.forEachFullRingSpan((y, x0, x1) -> fullPixels.addAndGet(x1 - x0));

        AtomicInteger halfPixels = new AtomicInteger();
        DivineRingGeometry.forEachActiveSpan(0.5F, (y, x0, x1) -> {
            assertTrue(x0 >= 17, "50% clockwise arc from 12 o'clock should remain in right half (x >= 17), got x0=" + x0);
            assertTrue(x1 <= DivineRingGeometry.DIAMETER);
            halfPixels.addAndGet(x1 - x0);
        });

        assertEquals(fullPixels.get() / 2, halfPixels.get());
    }
}
