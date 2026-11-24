package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Pacote autoritativo C2S para ações iniciadas pela Interface do Cultivador ([U]).
 * O cliente envia apenas a intenção; toda validação, custo de item/Qi e recarga ocorre no servidor.
 */
public class CultivatorActionC2SPacket {

    public enum Action {
        EQUIP_REMNANT,
        TOGGLE_INFUSION,
        ADVANCE_BODY_STAGE,
        ADVANCE_SPIRIT_STAGE,
        BREAKTHROUGH_BODY,
        BREAKTHROUGH_SPIRIT
    }

    public static final long MIN_ACTION_INTERVAL_TICKS = 8L;
    private static final Map<UUID, Long> LAST_ACTION_TICK = new ConcurrentHashMap<>();

    private final Action action;
    @Nullable
    private final ResourceLocation targetId;

    public CultivatorActionC2SPacket(Action action) {
        this(action, null);
    }

    public CultivatorActionC2SPacket(Action action, @Nullable ResourceLocation targetId) {
        this.action = action != null ? action : Action.TOGGLE_INFUSION;
        this.targetId = targetId;
    }

    public CultivatorActionC2SPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeBoolean(this.targetId != null);
        if (this.targetId != null) {
            buf.writeResourceLocation(this.targetId);
        }
    }

    public Action getAction() {
        return this.action;
    }

    @Nullable
    public ResourceLocation getTargetId() {
        return this.targetId;
    }

    public static boolean tryAcquireRateLimit(UUID playerUuid, long gameTime) {
        if (playerUuid == null) {
            return false;
        }
        final boolean[] allowed = { false };
        LAST_ACTION_TICK.compute(playerUuid, (id, prev) -> {
            if (prev == null || gameTime < prev || (gameTime - prev) >= MIN_ACTION_INTERVAL_TICKS) {
                allowed[0] = true;
                return gameTime;
            }
            return prev;
        });
        return allowed[0];
    }

    public static void clearRateLimit(UUID playerUuid) {
        if (playerUuid != null) {
            LAST_ACTION_TICK.remove(playerUuid);
        }
    }

    public static void clearAllRateLimits() {
        LAST_ACTION_TICK.clear();
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

            ModCapabilities.get(sender).ifPresent(data -> {
                switch (this.action) {
                    case EQUIP_REMNANT -> {
                        if (this.targetId != null) {
                            DivineRemnant.parse(this.targetId.toString()).ifPresent(remnant ->
                                    CultivationActionService.executeEquipRemnant(sender, data, remnant));
                        }
                    }
                    case TOGGLE_INFUSION -> CultivationActionService.executeToggleInfusion(sender, data);
                    case ADVANCE_BODY_STAGE -> CultivationActionService.executeStageAdvancement(
                            sender, data, CultivationPath.BODY, true);
                    case ADVANCE_SPIRIT_STAGE -> CultivationActionService.executeStageAdvancement(
                            sender, data, CultivationPath.SPIRIT, true);
                    case BREAKTHROUGH_BODY -> CultivationActionService.executeBreakthrough(
                            sender, data, CultivationPath.BODY, true);
                    case BREAKTHROUGH_SPIRIT -> CultivationActionService.executeBreakthrough(
                            sender, data, CultivationPath.SPIRIT, true);
                }
            });
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
