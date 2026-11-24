package com.mythiccontracts.cultivation;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.contract.PaladinVowContract;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.event.ContractGameplayEvents;
import com.mythiccontracts.integration.ModAdvancements;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.TargetSpot;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import com.mythiccontracts.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Executa o Resquício Divino equipado na Tecla G, escalando poder, raio, custo
 * e efeitos conforme a camada de Cultivo do jogador, usando a barra exclusiva de Resistência Divina
 * e emitindo 1 pacote S2C compacto para coreografia VFX client-side.
 */
public final class UniqueSkillHandler {

    private static final String TAG_SUBJUGATED_OWNER = "SubjugatedOwner";
    private static final double VFX_BROADCAST_RADIUS = 56.0D;

    private UniqueSkillHandler() {
    }

    public static DivineRemnant getEquippedRemnant(PlayerData data) {
        if (data == null) {
            return DivineRemnant.TEZCATLIPOCA_STEP;
        }
        ResourceLocation equippedId = data.getSkills().getPrimaryEquippedSkill();
        return DivineRemnant.fromSkillId(equippedId);
    }

    public static String getEquippedSkillName(PlayerData data) {
        return getEquippedRemnant(data).getDisplayName();
    }

    public static void execute(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null || !player.isAlive() || player.isDeadOrDying()) {
            return;
        }

        PlayerData playerData = data.getPlayerData();
        SkillData skills = playerData.getSkills();

        if (playerData.getCultivation().isSpiritShattered()) {
            float sec = playerData.getCultivation().getSpiritShatterTicks() / 20.0F;
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT,
                                    "✖ [Colapso Espiritual] Alma instável! Aguarde %.1fs para canalizar o Resquício.", sec))
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        if (skills.isDivineOverloaded()) {
            float sec = skills.getDivineOverloadTicks() / 20.0F;
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.40F, 0.65F);
            ModNetworking.sendToNear(
                    new SpawnVisualEffectS2CPacket(EffectType.DIVINE_OVERLOAD_REJECT, 0,
                            player.position(), player.getLookAngle(), 1.5F, List.of()),
                    player.serverLevel(), player.position(), 32.0D
            );
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT,
                                    "✖ [Sobrecarga Divina] Seu corpo não suporta o poder do Deus! Aguarde %.1fs.", sec))
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        int cooldown = data.getUniqueSkillCooldownTicks();
        if (cooldown > 0) {
            float seconds = cooldown / 20.0F;
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.PLAYERS, 0.45F, 0.70F);
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "⏳ Resquício Divino em recarga: %.1fs", seconds))
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        ManifestationTier tier = DivineResonanceBalance.getManifestationTier(playerData);
        DivineRemnant remnant = getEquippedRemnant(playerData);
        DivineSkillDefinition skillDef = DivineSkillRegistry.getForRemnant(remnant);

        if (!skillDef.enabled()) {
            player.displayClientMessage(
                    Component.literal("✖ [Resquício Desativado] Esta habilidade divina está desativada pela configuração atual.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        int bodyRealm = CultivationService.getCurrentRealm(playerData, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(playerData, CultivationPath.SPIRIT).order();

        if (bodyRealm < skillDef.minBodyRealm() || spiritRealm < skillDef.minSpiritRealm()) {
            player.displayClientMessage(
                    Component.literal("✖ Requer Corpo R" + skillDef.minBodyRealm()
                                    + "+ e Espírito R" + skillDef.minSpiritRealm()
                                    + "+ para manifestar " + skillDef.displayName() + ".")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        EnergyService.DivineSpendOutcome spendOutcome = EnergyService.consumeDivineResistanceOrOverload(playerData);
        if (spendOutcome == EnergyService.DivineSpendOutcome.BLOCKED_OVERLOAD) {
            player.displayClientMessage(
                    Component.literal("✖ Sua Resistência Divina está em colapso! Aguarde a estabilização.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            ModCapabilities.syncEnergy(player);
            return;
        }

        int baseCooldown = CultivationBalance.calculateUniqueSkillCooldownTicks(spiritRealm);
        int newCooldown = Math.max(20, Math.round(baseCooldown * skillDef.cooldownMultiplier()));
        data.setUniqueSkillCooldownTicks(newCooldown);

        switch (remnant) {
            case EYE_OF_RA -> castEyeOfRa(player, data, tier, skillDef);
            case MORRIGAN_REAP -> castMorriganReap(player, data, tier, skillDef);
            case INDRAS_VAJRA -> castIndrasVajra(player, data, tier, skillDef);
            case TEZCATLIPOCA_STEP -> castTezcatlipocaStep(player, data, tier, skillDef);
        }

        ModAdvancements.grant(player, ModAdvancements.DIVINE_CHANNELING);

        if (spendOutcome == EnergyService.DivineSpendOutcome.SUCCESS_TRIGGERED_OVERLOAD) {
            applyDivineOverloadPenalty(player);
        }

        ModCapabilities.syncEnergy(player);
    }

    private static void applyDivineOverloadPenalty(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                DivineResonanceBalance.OVERLOAD_PENALTY_TICKS,
                0,
                false,
                true,
                true
        ));

        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.DIVINE_OVERLOAD_BURST,
                        0,
                        player.position(),
                        player.getLookAngle(),
                        2.0F,
                        List.of()
                ),
                level,
                player.position(),
                48.0D
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, 0.75F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.65F, 0.7F);

        player.sendSystemMessage(
                Component.literal("⚠ [SOBRECARGA DIVINA] Sua Resistência Divina zerou! Você recebeu Fraqueza e o poder do Deus foi selado por 1 minuto.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
    }

    private static void castEyeOfRa(ServerPlayer player, PlayerCoreData data, ManifestationTier tier, DivineSkillDefinition def) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        int spiritStage = CultivationService.getCurrentStageNumber(pData, CultivationPath.SPIRIT);
        double radius = Math.max(3.5D, def.baseRadius() + tier.getRadiusOffset());
        float rawDamage = def.baseDamage() + def.primaryRealmScale() * spiritRealm + def.secondaryScale() * spiritStage;
        float damage = rawDamage * tier.getPowerMultiplier();
        List<LivingEntity> targets = findValidTargets(player, data, radius);

        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().magic(), damage);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                if (tier == ManifestationTier.TIER_4) {
                    target.setSecondsOnFire(4);
                }
                pushAway(player, target, 0.45D + 0.10D * tier.ordinal());
            }
        });

        switch (tier) {
            case TIER_1 -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
            case TIER_2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
            }
            case TIER_3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
            }
            case TIER_4 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.EYE_OF_RA,
                        tier.ordinal(),
                        player.position(),
                        player.getLookAngle(),
                        (float) radius,
                        toTargetSpots(targets)
                ),
                level,
                player.position(),
                VFX_BROADCAST_RADIUS
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.85F, 1.15F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.75F, 1.1F);

        player.displayClientMessage(
                Component.literal("☀ " + def.displayName() + " [" + tier.getNumeral() + "]! (" + targets.size() + " alvos purificados)")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                true
        );
    }

    private static void castMorriganReap(ServerPlayer player, PlayerCoreData data, ManifestationTier tier, DivineSkillDefinition def) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        double radius = Math.max(4.0D, def.baseRadius() + tier.getRadiusOffset());
        float rawDamage = def.baseDamage() + def.primaryRealmScale() * bodyRealm + def.secondaryScale() * spiritRealm;
        float damage = rawDamage * tier.getPowerMultiplier();
        List<LivingEntity> targets = findValidTargets(player, data, radius);

        int witherAmp = (tier == ManifestationTier.TIER_3 || tier == ManifestationTier.TIER_4) ? 1 : 0;
        int witherTicks = (tier == ManifestationTier.TIER_4) ? 100 : 80;

        final float[] totalDamage = { 0.0F };
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                    totalDamage[0] += damage;
                }
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, witherTicks, witherAmp));
            }
        });

        if (totalDamage[0] > 0.0F) {
            float ratio = switch (tier) {
                case TIER_1 -> 0.08F;
                case TIER_2 -> 0.15F;
                case TIER_3 -> 0.20F;
                case TIER_4 -> 0.25F;
            };
            float cap = switch (tier) {
                case TIER_1 -> 4.0F;
                case TIER_2 -> 10.0F;
                case TIER_3 -> 14.0F;
                case TIER_4 -> 18.0F;
            };
            player.heal(Math.min(cap, totalDamage[0] * ratio));
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.MORRIGAN_REAP,
                        tier.ordinal(),
                        player.position(),
                        player.getLookAngle(),
                        (float) radius,
                        toTargetSpots(targets)
                ),
                level,
                player.position(),
                VFX_BROADCAST_RADIUS
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.6F, 0.85F);

        player.displayClientMessage(
                Component.literal("🪶 " + def.displayName() + " [" + tier.getNumeral() + "]! (" + targets.size() + " alvos colhidos)")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );
    }

    private static void castIndrasVajra(ServerPlayer player, PlayerCoreData data, ManifestationTier tier, DivineSkillDefinition def) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int bodyStage = CultivationService.getCurrentStageNumber(pData, CultivationPath.BODY);
        double radius = Math.max(4.5D, def.baseRadius() + tier.getRadiusOffset());
        float baseDamage = (def.baseDamage() + def.primaryRealmScale() * bodyRealm + def.secondaryScale() * bodyStage) * tier.getPowerMultiplier();
        List<LivingEntity> targets = findValidTargets(player, data, radius);

        float bossMult = switch (tier) {
            case TIER_1 -> 1.20F;
            case TIER_2 -> 1.35F;
            case TIER_3 -> 1.50F;
            case TIER_4 -> 1.75F;
        };
        int slowAmp = (tier == ManifestationTier.TIER_3 || tier == ManifestationTier.TIER_4) ? 2 : 1;
        int debuffTicks = (tier == ManifestationTier.TIER_1) ? 60 : 90;

        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                boolean isBoss = target.getType().is(ModTags.HIGH_REALM_BOSS);
                float finalDamage = isBoss ? baseDamage * bossMult * def.bossDamageBonusMultiplier() : baseDamage;
                target.hurt(player.damageSources().sonicBoom(player), finalDamage);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, debuffTicks, slowAmp));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, debuffTicks, 1));
                pushAway(player, target, isBoss ? 0.35D : (0.75D + 0.15D * tier.ordinal()));
            }
        });

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.INDRAS_VAJRA,
                        tier.ordinal(),
                        player.position(),
                        player.getLookAngle(),
                        (float) radius,
                        toTargetSpots(targets)
                ),
                level,
                player.position(),
                VFX_BROADCAST_RADIUS
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55F, 1.25F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.45F, 1.4F);

        player.displayClientMessage(
                Component.literal("⚡ " + def.displayName() + " [" + tier.getNumeral() + "]! (" + targets.size() + " alvos fulminados)")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                true
        );
    }

    private static void castTezcatlipocaStep(ServerPlayer player, PlayerCoreData data, ManifestationTier tier, DivineSkillDefinition def) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        double radius = Math.max(3.5D, def.baseRadius() + Math.max(-1.0D, tier.getRadiusOffset()));
        float rawDamage = def.baseDamage() + bodyRealm * def.primaryRealmScale() + spiritRealm * def.secondaryScale();
        float damage = rawDamage * tier.getPowerMultiplier();
        List<LivingEntity> targets = findValidTargets(player, data, radius);

        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), damage);
            }
        });

        double dashScale = switch (tier) {
            case TIER_1 -> 0.95D;
            case TIER_2 -> 1.20D;
            case TIER_3 -> 1.40D;
            case TIER_4 -> 1.55D;
        };

        Vec3 startOrigin = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() > 1.0E-4D) {
            horizontal = horizontal.normalize().scale(dashScale);
        }
        player.setDeltaMovement(horizontal.x, Math.max(0.32D, player.getDeltaMovement().y + 0.32D), horizontal.z);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        switch (tier) {
            case TIER_1 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0));
            case TIER_2 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            case TIER_3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
            }
            case TIER_4 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.TEZCATLIPOCA_STEP,
                        tier.ordinal(),
                        startOrigin,
                        look,
                        (float) radius,
                        toTargetSpots(targets)
                ),
                level,
                startOrigin,
                VFX_BROADCAST_RADIUS
        );

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.75F, 1.25F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.1F);

        player.displayClientMessage(
                Component.literal("🌑 " + def.displayName() + " [" + tier.getNumeral() + "]!")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
                true
        );
    }

    private static List<TargetSpot> toTargetSpots(List<LivingEntity> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(SpawnVisualEffectS2CPacket.MAX_TARGETS, targets.size());
        List<TargetSpot> spots = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            LivingEntity t = targets.get(i);
            spots.add(new TargetSpot(
                    (float) t.getX(),
                    (float) (t.getY() + t.getBbHeight() * 0.5D),
                    (float) t.getZ(),
                    t.getType().is(ModTags.HIGH_REALM_BOSS)
            ));
        }
        return spots;
    }

    private static List<LivingEntity> findValidTargets(ServerPlayer player, PlayerCoreData data, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();

        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> {
            if (entity == player || !entity.isAlive() || entity.isAlliedTo(player)) {
                return false;
            }
            if (entity instanceof Player otherPlayer) {
                if (otherPlayer.isSpectator() || otherPlayer.isCreative() || !player.canHarmPlayer(otherPlayer)) {
                    return false;
                }
            }
            if (protectVillagers && entity instanceof AbstractVillager) {
                return false;
            }
            if (entity.getPersistentData().hasUUID(TAG_SUBJUGATED_OWNER)
                    && player.getUUID().equals(entity.getPersistentData().getUUID(TAG_SUBJUGATED_OWNER))) {
                return false;
            }
            return entity.distanceToSqr(player) <= radius * radius;
        });
    }

    private static void pushAway(ServerPlayer source, LivingEntity target, double strength) {
        Vec3 diff = target.position().subtract(source.position());
        Vec3 horizontal = new Vec3(diff.x, 0.0D, diff.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = source.getLookAngle();
        }
        horizontal = horizontal.normalize().scale(strength);
        target.push(horizontal.x, 0.25D, horizontal.z);
        target.hurtMarked = true;
    }
}
