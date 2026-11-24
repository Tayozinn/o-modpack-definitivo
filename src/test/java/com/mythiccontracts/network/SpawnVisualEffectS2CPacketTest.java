package com.mythiccontracts.network;

import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.TargetSpot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpawnVisualEffectS2CPacketTest {

    @Test
    void roundTripsAllEffectTypesAndBossTargetSpots() {
        List<TargetSpot> spots = List.of(
                new TargetSpot(12.5F, 65.0F, -8.25F, true),
                new TargetSpot(14.0F, 64.0F, -6.5F, false)
        );

        for (EffectType type : EffectType.values()) {
            SpawnVisualEffectS2CPacket out = new SpawnVisualEffectS2CPacket(
                    type, 3, 10.25D, 64.5D, -12.75D, 0.6F, 0.0F, 0.8F, 7.5F, spots
            );
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            out.toBytes(buf);

            SpawnVisualEffectS2CPacket in = new SpawnVisualEffectS2CPacket(buf);
            assertEquals(type, in.getEffectType());
            assertEquals(3, in.getTierOrdinal());
            assertEquals(10.25D, in.getOriginX(), 0.0001D);
            assertEquals(64.5D, in.getOriginY(), 0.0001D);
            assertEquals(-12.75D, in.getOriginZ(), 0.0001D);
            assertEquals(0.6F, in.getDirX(), 0.0001F);
            assertEquals(0.8F, in.getDirZ(), 0.0001F);
            assertEquals(7.5F, in.getRadius(), 0.0001F);
            assertEquals(2, in.getTargets().size());
            assertTrue(in.getTargets().get(0).boss());
            assertFalse(in.getTargets().get(1).boss());
            assertEquals(0, buf.readableBytes());
        }
    }

    @Test
    void capsTargetsAndSanitizesNonFiniteCoordinates() {
        List<TargetSpot> excess = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            excess.add(new TargetSpot(i, 64.0F, i * 2.0F, i % 2 == 0));
        }
        SpawnVisualEffectS2CPacket pkt = new SpawnVisualEffectS2CPacket(
                EffectType.INDRAS_VAJRA,
                99,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                -5.0D,
                Float.NaN,
                0.0F,
                Float.NEGATIVE_INFINITY,
                120.0F,
                excess
        );

        assertEquals(3, pkt.getTierOrdinal());
        assertEquals(0.0D, pkt.getOriginX(), 0.0001D);
        assertEquals(0.0D, pkt.getOriginY(), 0.0001D);
        assertEquals(0.0F, pkt.getDirX(), 0.0001F);
        assertEquals(0.0F, pkt.getDirZ(), 0.0001F);
        assertEquals(32.0F, pkt.getRadius(), 0.0001F);
        assertEquals(SpawnVisualEffectS2CPacket.MAX_TARGETS, pkt.getTargets().size());
    }
}
