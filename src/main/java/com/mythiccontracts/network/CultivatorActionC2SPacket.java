package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.skill.cultivation.CultivationSkillService;
import com.mythiccontracts.skill.remnant.DivinePhaseAbility;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import com.mythiccontracts.subjugation.SubjugationService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
        BREAKTHROUGH_SPIRIT,
        UNLOCK_SKILL,
        EQUIP_MARTIAL_SKILL,
        SUMMON_SERVANT,
        RECALL_SERVANT,
        CYCLE_SERVANT_STANCE,
        RELEASE_SERVANT,
        SELECT_DIVINE_PHASE
    }

    public static final long MIN_ACTION_INTERVAL_TICKS = 8L;
    private static final Map<UUID, Long> LAST_ACTION_TICK = new ConcurrentHashMap<>();

    private final Action action;
    @Nullable
    private final ResourceLocation targetId;
    private final int slotIndex;
    @Nullable
    private final UUID entityInstanceId;

    public CultivatorActionC2SPacket(Action action) {
        this(action, null, -1, null);
    }

    public CultivatorActionC2SPacket(Action action, @Nullable ResourceLocation targetId) {
        this(action, targetId, -1, null);
    }

    public CultivatorActionC2SPacket(Action action, @Nullable ResourceLocation targetId, int slotIndex) {
        this(action, targetId, slotIndex, null);
    }

    public CultivatorActionC2SPacket(Action action, @Nullable UUID entityInstanceId) {
        this(action, null, -1, entityInstanceId);
    }

    public CultivatorActionC2SPacket(
            Action action,
            @Nullable ResourceLocation targetId,
            int slotIndex,
            @Nullable UUID entityInstanceId
    ) {
        this.action = action != null ? action : Action.TOGGLE_INFUSION;
        this.targetId = targetId;
        this.slotIndex = slotIndex;
        this.entityInstanceId = entityInstanceId;
    }

    public CultivatorActionC2SPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.slotIndex = buf.readVarInt();
        this.entityInstanceId = buf.readBoolean() ? buf.readUUID() : null;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeBoolean(this.targetId != null);
        if (this.targetId != null) {
            buf.writeResourceLocation(this.targetId);
        }
        buf.writeVarInt(this.slotIndex);
        buf.writeBoolean(this.entityInstanceId != null);
        if (this.entityInstanceId != null) {
            buf.writeUUID(this.entityInstanceId);
        }
    }

    public Action getAction() {
        return this.action;
    }

    @Nullable
    public ResourceLocation getTargetId() {
        return this.targetId;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    @Nullable
    public UUID getEntityInstanceId() {
        return this.entityInstanceId;
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
                    case UNLOCK_SKILL -> {
                        if (this.targetId != null) {
                            CultivationSkillService.executeUnlockSkill(sender, data, this.targetId);
                        }
                    }
                    case EQUIP_MARTIAL_SKILL -> {
                        if (this.targetId != null) {
                            CultivationSkillService.executeEquipMartialSkill(sender, data, this.targetId, this.slotIndex);
                        }
                    }
                    case SUMMON_SERVANT -> {
                        if (this.entityInstanceId != null) {
                            SubjugationService.summonServantById(sender, data, this.entityInstanceId);
                        }
                    }
                    case RECALL_SERVANT -> {
                        if (this.entityInstanceId != null) {
                            SubjugationService.recallServantById(sender, data, this.entityInstanceId);
                        }
                    }
                    case CYCLE_SERVANT_STANCE -> {
                        if (this.entityInstanceId != null) {
                            SubjugationService.cycleServantStance(sender, data, this.entityInstanceId);
                        }
                    }
                    case RELEASE_SERVANT -> {
                        if (this.entityInstanceId != null) {
                            SubjugationService.releaseServantById(sender, data, this.entityInstanceId);
                        }
                    }
                    case SELECT_DIVINE_PHASE -> {
                        if (this.targetId != null) {
                            DivineRemnant.parse(this.targetId.toString()).ifPresent(remnant -> {
                                var pData = data.getPlayerData();
                                if (!DivineResonanceBalance.isPhaseUnlocked(pData, this.slotIndex)) {
                                    sender.displayClientMessage(
                                            Component.literal("🔒 Fase bloqueada! Requisito: "
                                                            + DivineResonanceBalance.getPhaseUnlockLabel(this.slotIndex))
                                                    .withStyle(ChatFormatting.YELLOW),
                                            true
                                    );
                                    return;
                                }
                                pData.getSkills().setPrimaryEquippedSkill(remnant.getId());
                                pData.getSkills().setActiveDivinePhase(this.slotIndex);
                                DivinePhaseAbility ab = DivinePhaseAbility.get(remnant, this.slotIndex);
                                sender.serverLevel().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                                        ModSounds.SPIRIT_SLOT_CYCLE.get(), SoundSource.PLAYERS, 0.70F, 1.15F);
                                sender.displayClientMessage(
                                        Component.literal("✦ [" + remnant.getDisplayName() + "] "
                                                        + ab.code() + " · " + ab.displayName() + " equipada ([G])!")
                                                .withStyle(ChatFormatting.GOLD),
                                        true
                                );
                                ModCapabilities.sync(sender);
                                ModCapabilities.syncEnergy(sender);
                            });
                        }
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
