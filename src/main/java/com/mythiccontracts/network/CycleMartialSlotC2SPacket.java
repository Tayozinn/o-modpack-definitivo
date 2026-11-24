package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.skill.cultivation.CultivationSkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Pacote autoritativo C2S (ALT + V por padrão) para alternar entre os slots S1 → S2 → S3.
 */
public class CycleMartialSlotC2SPacket {

    public static final long MIN_INTERVAL_TICKS = 5L;
    private static final Map<UUID, Long> LAST_CYCLE_TICK = new ConcurrentHashMap<>();

    public CycleMartialSlotC2SPacket() {
    }

    public CycleMartialSlotC2SPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static boolean tryAcquireRateLimit(UUID playerUuid, long gameTime) {
        if (playerUuid == null) {
            return false;
        }
        final boolean[] allowed = { false };
        LAST_CYCLE_TICK.compute(playerUuid, (id, prev) -> {
            if (prev == null || gameTime < prev || (gameTime - prev) >= MIN_INTERVAL_TICKS) {
                allowed[0] = true;
                return gameTime;
            }
            return prev;
        });
        return allowed[0];
    }

    public static void clearRateLimit(UUID playerUuid) {
        if (playerUuid != null) {
            LAST_CYCLE_TICK.remove(playerUuid);
        }
    }

    public static void clearAllRateLimits() {
        LAST_CYCLE_TICK.clear();
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.isAlive() || sender.isDeadOrDying()) {
                return;
            }
            if (!tryAcquireRateLimit(sender.getUUID(), sender.level().getGameTime())) {
                return;
            }
            ModCapabilities.get(sender).ifPresent(data ->
                    CultivationSkillService.executeCycleMartialSlot(sender, data)
            );
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
