package com.mythiccontracts.network;

import com.mythiccontracts.skill.remnant.DivineRemnant;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CultivatorActionC2SPacketTest {

    @Test
    void roundTripsAllActionsWithAndWithoutTargetId() {
        for (CultivatorActionC2SPacket.Action action : CultivatorActionC2SPacket.Action.values()) {
            CultivatorActionC2SPacket outgoing = action == CultivatorActionC2SPacket.Action.EQUIP_REMNANT
                    ? new CultivatorActionC2SPacket(action, DivineRemnant.INDRAS_VAJRA.getId())
                    : new CultivatorActionC2SPacket(action);

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            outgoing.toBytes(buf);

            CultivatorActionC2SPacket incoming = new CultivatorActionC2SPacket(buf);
            assertEquals(action, incoming.getAction());
            if (action == CultivatorActionC2SPacket.Action.EQUIP_REMNANT) {
                assertEquals(DivineRemnant.INDRAS_VAJRA.getId(), incoming.getTargetId());
            } else {
                assertNull(incoming.getTargetId());
            }
            assertEquals(0, buf.readableBytes());
        }
    }
}
