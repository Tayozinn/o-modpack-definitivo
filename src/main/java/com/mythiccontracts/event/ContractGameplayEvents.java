package com.mythiccontracts.event;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.contract.PaladinVowContract;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.util.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContractGameplayEvents {

    private static final ThreadLocal<Boolean> PROCESSING_INFUSION = ThreadLocal.withInitial(() -> false);

    private ContractGameplayEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }
        if (event.player instanceof ServerPlayer serverPlayer) {
            ModCapabilities.get(serverPlayer).ifPresent(data -> {
                if (!serverPlayer.isAlive() || serverPlayer.isDeadOrDying()) {
                    if (data.isManaInfusionActive()) {
                        data.setManaInfusionActive(false);
                        ModCapabilities.sync(serverPlayer);
                    }
                    return;
                }

                if (data.isManaInfusionActive() && serverPlayer.tickCount % 20 == 0) {
                    serverPlayer.serverLevel().sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            serverPlayer.getX(),
                            serverPlayer.getY() + 0.9D,
                            serverPlayer.getZ(),
                            3,
                            0.35D,
                            0.45D,
                            0.35D,
                            0.01D
                    );
                }

                if (serverPlayer.tickCount % 40 == 0) {
                    PaladinVowContract.onPlayerTick(serverPlayer, data);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() instanceof AbstractVillager villager) {
            ModCapabilities.get(attacker).ifPresent(data ->
                    PaladinVowContract.onPlayerAttackVillager(attacker, villager, data)
            );
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (PROCESSING_INFUSION.get()) {
            return;
        }
        float rawAmount = event.getAmount();
        if (rawAmount <= 0.0F || Float.isNaN(rawAmount) || Float.isInfinite(rawAmount) || event.getSource().is(DamageTypes.THORNS)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        if (attacker.level().isClientSide()) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (target == null || target == attacker || target.getUUID().equals(attacker.getUUID())) {
            return;
        }

        boolean isHighRealmBoss = target.getType().is(ModTags.HIGH_REALM_BOSS);

        PROCESSING_INFUSION.set(true);
        try {
            ModCapabilities.get(attacker).ifPresent(data -> {
                if (!data.isManaInfusionActive()) {
                    if (isHighRealmBoss) {
                        event.setAmount(event.getAmount() * 0.10F);
                        attacker.displayClientMessage(
                                Component.literal("§c[Pressão de Reino] Seu ataque foi suprimido em 90%! Ative a Infusão de Mana [V]."),
                                true
                        );
                    }
                } else {
                    float baseDamage = event.getAmount();
                    float bodyBuff = CultivationBalance.calculateBodyInfusionBuff(baseDamage, data.getBodyRealm(), data.getBodyStage());
                    float spiritBonus = CultivationBalance.calculateSpiritInfusionFlatBonus(data.getSpiritRealm(), data.getSpiritStage());
                    float totalDamage = baseDamage + bodyBuff + spiritBonus;
                    event.setAmount(totalDamage);

                    if (attacker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                target.getX(),
                                target.getY() + (target.getBbHeight() * 0.5D),
                                target.getZ(),
                                15,
                                0.35D,
                                0.35D,
                                0.35D,
                                0.04D
                        );
                    }

                    String bossTag = isHighRealmBoss ? " §d(Pressão de Reino perfurada!)" : "";
                    attacker.displayClientMessage(
                            Component.literal(String.format(
                                    Locale.ROOT,
                                    "§b[Infusão de Mana] §fDano: §e%.1f §7(Físico: %.1f + Corpo: +%.1f + Espírito: +%.1f)%s",
                                    totalDamage,
                                    baseDamage,
                                    bodyBuff,
                                    spiritBonus,
                                    bossTag
                            )),
                            true
                    );
                }
            });
        } finally {
            PROCESSING_INFUSION.set(false);
        }
    }
}
