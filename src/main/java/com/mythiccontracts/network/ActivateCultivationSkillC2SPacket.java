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
 * Pacote autoritativo C2S (ALT + G por padrão) para executar a Técnica de Cultivo ativa
 * ou suprimir o Pulso de Revolta durante uma Disputa de Subjugação.
 */
public class ActivateCultivationSkillC2SPacket {

    public static final long MIN_INTERVAL_TICKS = 5L;
    private static final Map<UUID, Long> LAST_CAST_TICK = new ConcurrentHashMap<>();

    public ActivateCultivationSkillC2SPacket() {
    }

    public ActivateCultivationSkillC2SPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static boolean tryAcquireRateLimit(UUID playerUuid, long gameTime) {
        if (playerUuid == null) {
            return false;
        }
        final boolean[] allowed = { false };
        LAST_CAST_TICK.compute(playerUuid, (id, prev) -> {
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
            LAST_CAST_TICK.remove(playerUuid);
        }
    }

    public static void clearAllRateLimits() {
        LAST_CAST_TICK.clear();
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
                    CultivationSkillService.executeActiveMartialSkill(sender, data)
            );
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
