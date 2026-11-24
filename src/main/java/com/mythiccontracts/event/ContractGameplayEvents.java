package com.mythiccontracts.event;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.integration.CombatCompatService;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    public static boolean isBypassingInfusionHooks() {
        return Boolean.TRUE.equals(PROCESSING_INFUSION.get());
    }

    public static void runBypassingInfusionHooks(Runnable action) {
        if (action == null) {
            return;
        }
        boolean previous = Boolean.TRUE.equals(PROCESSING_INFUSION.get());
        PROCESSING_INFUSION.set(true);
        try {
            action.run();
        } finally {
            PROCESSING_INFUSION.set(previous);
        }
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
                        ModCapabilities.syncEnergy(serverPlayer);
                    }
                    return;
                }

                PlayerData playerData = data.getPlayerData();
                CultivationData cult = playerData.getCultivation();
                boolean hadCooldown = data.getUniqueSkillCooldownTicks() > 0;
                boolean hadOverload = playerData.getSkills().isDivineOverloaded();
                boolean hadShatter = cult.isSpiritShattered();
                data.tickCooldowns();

                float prevQi = data.getCurrentQi();
                float prevDivine = playerData.getSkills().getCurrentDivineResistance();
                float prevIntegrity = cult.getCurrentSpiritualIntegrity();
                float prevStamina = cult.getCurrentStamina();

                EnergyService.EnergyTickResult tickResult = EnergyService.tickAllEnergy(playerData, serverPlayer.isSprinting());

                if (cult.getCurrentStamina() <= 0.0F && serverPlayer.isSprinting()) {
                    serverPlayer.setSprinting(false);
                }

                float maxQi = data.getMaxQi();
                float maxDivine = playerData.getSkills().getMaxDivineResistance();
                float maxIntegrity = cult.getMaxSpiritualIntegrity();
                float maxStamina = cult.getMaxStamina();

                boolean forceSync = tickResult.qiDepleted()
                        || tickResult.divineOverloadEnded()
                        || tickResult.spiritShatterEnded()
                        || (hadCooldown && data.getUniqueSkillCooldownTicks() == 0);

                if (tickResult.qiDepleted()) {
                    serverPlayer.displayClientMessage(
                            Component.literal("§c[Qi Esgotado] Infusão de Mana desativada!"),
                            true
                    );
                    serverPlayer.serverLevel().playSound(
                            null,
                            serverPlayer.getX(),
                            serverPlayer.getY(),
                            serverPlayer.getZ(),
                            SoundEvents.BEACON_DEACTIVATE,
                            SoundSource.PLAYERS,
                            0.6F,
                            1.1F
                    );
                }

                if (tickResult.divineOverloadEnded()) {
                    ModNetworking.sendToNear(
                            new SpawnVisualEffectS2CPacket(
                                    SpawnVisualEffectS2CPacket.EffectType.DIVINE_OVERLOAD_RECOVER,
                                    0,
                                    serverPlayer.position(),
                                    serverPlayer.getLookAngle(),
                                    1.8F,
                                    java.util.List.of()
                            ),
                            serverPlayer.serverLevel(),
                            serverPlayer.position(),
                            32.0D
                    );
                    serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.55F, 0.9F);
                    serverPlayer.displayClientMessage(
                            Component.literal("✦ [Estabilizado] Sua Resistência Divina se recuperou e você pode canalizar Resquícios novamente!")
                                    .withStyle(ChatFormatting.GREEN),
                            true
                    );
                }

                if (tickResult.spiritShatterEnded()) {
                    serverPlayer.displayClientMessage(
                            Component.literal("✦ [Alma Restaurada] Sua Integridade Espiritual estabilizou em 25%!")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE),
                            true
                    );
                }

                boolean energyChanging = data.isManaInfusionActive()
                        || prevQi < maxQi
                        || prevDivine < maxDivine
                        || prevIntegrity < maxIntegrity
                        || prevStamina < maxStamina
                        || serverPlayer.isSprinting()
                        || hadCooldown
                        || hadOverload
                        || hadShatter;
                if (forceSync || (serverPlayer.tickCount % 10 == 0 && energyChanging)) {
                    ModCapabilities.syncEnergy(serverPlayer);
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
                    ContractManager.onPlayerPeriodicTick(serverPlayer, data);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() instanceof AbstractVillager villager) {
            ModCapabilities.get(attacker).ifPresent(data ->
                    ContractManager.onPlayerAttackVillager(attacker, villager, data)
            );
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (isBypassingInfusionHooks()) {
            return;
        }
        float rawAmount = event.getAmount();
        if (rawAmount <= 0.0F || Float.isNaN(rawAmount) || Float.isInfinite(rawAmount)) {
            return;
        }

        // 1. Defesa do jogador: Integridade Espiritual absorve parcialmente danos arcanos/espirituais
        if (event.getEntity() instanceof ServerPlayer victim && !victim.level().isClientSide()) {
            SpiritualIntegrityService.SpiritualDamageCategory category =
                    SpiritualIntegrityService.classifyDamage(event.getSource());
            if (category != SpiritualIntegrityService.SpiritualDamageCategory.NONE) {
                ModCapabilities.getData(victim).ifPresent(victimData -> {
                    SpiritualIntegrityService.SpiritualDamageResult res =
                            SpiritualIntegrityService.processIncomingDamage(victimData, event.getAmount(), category);
                    event.setAmount(res.remainingBodyDamage());

                    if (res.triggeredShatter()) {
                        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, true, true));
                        victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, true, true));
                        ModNetworking.sendToNear(
                                new SpawnVisualEffectS2CPacket(
                                        SpawnVisualEffectS2CPacket.EffectType.SPIRIT_SHATTER_BURST,
                                        0,
                                        victim.position(),
                                        victim.getLookAngle(),
                                        2.0F,
                                        java.util.List.of()
                                ),
                                victim.serverLevel(),
                                victim.position(),
                                48.0D
                        );
                        victim.serverLevel().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.9F, 0.7F);
                        victim.sendSystemMessage(
                                Component.literal("✖ [COLAPSO ESPIRITUAL] Sua Integridade Espiritual zerou! Infusão [V] e Resquícios [G] bloqueados por 20s!")
                                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                        );
                    }
                    ModCapabilities.syncEnergy(victim);
                });
            }
        }

        if (event.getSource().is(DamageTypes.THORNS)) {
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
        CombatCompatService.AttackChannel channel = CombatCompatService.classifyPlayerAttack(event.getSource());
        boolean qiResonantWeapon = channel == CombatCompatService.AttackChannel.MELEE
                && (attacker.getMainHandItem().is(ModTags.QI_RESONANT_WEAPONS)
                || attacker.getOffhandItem().is(ModTags.QI_RESONANT_WEAPONS));
        long gameTime = attacker.level().getGameTime();

        boolean previousBypass = Boolean.TRUE.equals(PROCESSING_INFUSION.get());
        PROCESSING_INFUSION.set(true);
        try {
            ModCapabilities.get(attacker).ifPresent(data -> {
                PlayerData pData = data.getPlayerData();
                boolean consumedStamina = false;
                if (channel == CombatCompatService.AttackChannel.MELEE
                        && CombatCompatService.tryConsumeMeleeStaminaWindow(attacker.getUUID(), gameTime)) {
                    VitalService.consumeStamina(
                            pData,
                            VitalService.MELEE_ATTACK_STAMINA_COST,
                            VitalService.MELEE_REGEN_DELAY_TICKS
                    );
                    consumedStamina = true;
                }

                if (!data.isManaInfusionActive()) {
                    if (isHighRealmBoss) {
                        event.setAmount(event.getAmount() * 0.10F);
                        attacker.displayClientMessage(
                                Component.literal("§c[Pressão de Reino] Seu ataque foi suprimido em 90%! Ative a Infusão de Mana [V]."),
                                true
                        );
                    }
                    if (consumedStamina && attacker instanceof ServerPlayer serverAttacker) {
                        ModCapabilities.syncEnergy(serverAttacker);
                    }
                } else {
                    boolean sameTickFollowup = CombatCompatService.registerInfusionHitAndCheckSameTick(
                            attacker.getUUID(),
                            gameTime
                    );
                    CombatCompatService.InfusionProfile profile =
                            CombatCompatService.evaluateInfusionProfile(channel, sameTickFollowup, qiResonantWeapon);

                    float baseDamage = event.getAmount();
                    float bodyBuff = CultivationBalance.calculateBodyInfusionBuff(baseDamage, pData) * profile.bodyBuffScale();
                    float spiritBonus = CultivationBalance.calculateSpiritInfusionFlatBonus(pData) * profile.spiritBonusScale();
                    float totalDamage = baseDamage + bodyBuff + spiritBonus;
                    event.setAmount(totalDamage);

                    float baseHitCost = CultivationBalance.calculateInfusionDrainPerHit(pData);
                    float hitCost = Math.max(0.25F, baseHitCost * profile.qiCostScale());
                    data.setCurrentQi(data.getCurrentQi() - hitCost);
                    if (data.getCurrentQi() <= 0.0F) {
                        data.setCurrentQi(0.0F);
                        data.setManaInfusionActive(false);
                    }
                    if (attacker instanceof ServerPlayer serverAttacker) {
                        ModCapabilities.syncEnergy(serverAttacker);
                    }

                    if (attacker.level() instanceof ServerLevel serverLevel) {
                        int particleCount = sameTickFollowup ? 6 : 15;
                        serverLevel.sendParticles(
                                ParticleTypes.SOUL_FIRE_FLAME,
                                target.getX(),
                                target.getY() + (target.getBbHeight() * 0.5D),
                                target.getZ(),
                                particleCount,
                                0.35D,
                                0.35D,
                                0.35D,
                                0.04D
                        );
                    }

                    String channelTag = profile.channelLabel().isEmpty() ? "" : " §7[" + profile.channelLabel() + "]";
                    String bossTag = isHighRealmBoss ? " §d(Pressão de Reino perfurada!)" : "";
                    attacker.displayClientMessage(
                            Component.literal(String.format(
                                    Locale.ROOT,
                                    "§b[Infusão de Mana]%s §fDano: §e%.1f §7(Base: %.1f + Corpo: +%.1f + Espírito: +%.1f)%s",
                                    channelTag,
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
            PROCESSING_INFUSION.set(previousBypass);
        }
    }
}
