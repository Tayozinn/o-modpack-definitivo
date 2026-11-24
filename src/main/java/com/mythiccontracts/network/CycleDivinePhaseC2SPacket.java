package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.skill.remnant.DivinePhaseAbility;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Pacote autoritativo C2S (Shift + G por padrão — Sub-opção B1) para alternar entre
 * as habilidades desbloqueadas (H1 → H4) do Resquício Divino equipado.
 */
public class CycleDivinePhaseC2SPacket {

    public static final long MIN_INTERVAL_TICKS = 4L;
    private static final Map<UUID, Long> LAST_CYCLE_TICK = new ConcurrentHashMap<>();

    public CycleDivinePhaseC2SPacket() {
    }

    public CycleDivinePhaseC2SPacket(FriendlyByteBuf buf) {
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
            ModCapabilities.get(sender).ifPresent(coreData -> {
                PlayerData pData = coreData.getPlayerData();
                SkillData skills = pData.getSkills();
                int unlocked = DivineResonanceBalance.getUnlockedPhaseCount(pData);
                if (unlocked <= 1) {
                    skills.setActiveDivinePhase(0);
                    sender.displayClientMessage(
                            Component.literal("🔒 Fase II desbloqueia em R2-3 (Piso R2-1 em Corpo e Espírito).")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                    ModCapabilities.syncEnergy(sender);
                    return;
                }
                int nextPhase = (skills.getActiveDivinePhase() + 1) % unlocked;
                skills.setActiveDivinePhase(nextPhase);

                DivineRemnant remnant = DivineRemnant.fromSkillId(skills.getPrimaryEquippedSkill());
                DivinePhaseAbility ability = DivinePhaseAbility.get(remnant, nextPhase);
                sender.serverLevel().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                        ModSounds.SPIRIT_SLOT_CYCLE.get(), SoundSource.PLAYERS, 0.70F, 1.15F);
                sender.displayClientMessage(
                        Component.literal("✦ [Fase " + (nextPhase + 1) + "/4] " + ability.code()
                                        + " · " + ability.displayName() + " ([G])")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        true
                );
                ModCapabilities.syncEnergy(sender);
            });
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
