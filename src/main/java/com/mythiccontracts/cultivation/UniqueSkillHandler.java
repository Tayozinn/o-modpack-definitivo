package com.mythiccontracts.cultivation;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.contract.PaladinVowContract;
import com.mythiccontracts.core.player.CultivationData;
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
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import com.mythiccontracts.skill.remnant.DivinePhaseAbility;
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
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executa a habilidade ativa selecionada (H1–H4) do Resquício Divino equipado em [G],
 * aplicando contenção inicial na Fase I, progressão até a Encarnação (Fase IV) e Ultimates temporizadas.
 */
public final class UniqueSkillHandler {

    private static final String TAG_SUBJUGATED_OWNER = "SubjugatedOwner";
    private static final double VFX_BROADCAST_RADIUS = 56.0D;
    private static final Map<UUID, ActiveUltimateState> ACTIVE_ULTIMATES = new ConcurrentHashMap<>();

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

    public static boolean isMorriganQueenActive(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        ActiveUltimateState st = ACTIVE_ULTIMATES.get(playerUuid);
        return st != null && st.remnant == DivineRemnant.MORRIGAN_REAP && st.remainingTicks > 0;
    }

    public static void clearPlayerState(UUID playerUuid) {
        if (playerUuid != null) {
            ACTIVE_ULTIMATES.remove(playerUuid);
        }
    }

    public static void clearAllStates() {
        ACTIVE_ULTIMATES.clear();
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

        ManifestationTier unlockedTier = DivineResonanceBalance.getManifestationTier(playerData);
        int phaseIdx = Mth.clamp(skills.getActiveDivinePhase(), 0, unlockedTier.ordinal());
        if (skills.getActiveDivinePhase() != phaseIdx) {
            skills.setActiveDivinePhase(phaseIdx);
        }

        DivineRemnant remnant = getEquippedRemnant(playerData);
        DivineSkillDefinition skillDef = DivineSkillRegistry.getForRemnant(remnant);
        DivinePhaseAbility ability = DivinePhaseAbility.get(remnant, phaseIdx);

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

        EnergyService.DivineSpendOutcome spendOutcome =
                EnergyService.consumeDivineResistanceOrOverload(playerData, phaseIdx);
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
        int newCooldown = Math.max(20, Math.round(baseCooldown * skillDef.cooldownMultiplier() * ability.cooldownMultiplier()));
        data.setUniqueSkillCooldownTicks(newCooldown);

        switch (remnant) {
            case EYE_OF_RA -> castEyeOfRaPhase(player, data, unlockedTier, phaseIdx, skillDef, ability);
            case MORRIGAN_REAP -> castMorriganPhase(player, data, unlockedTier, phaseIdx, skillDef, ability);
            case INDRAS_VAJRA -> castIndraPhase(player, data, unlockedTier, phaseIdx, skillDef, ability);
            case TEZCATLIPOCA_STEP -> castTezcatlipocaPhase(player, data, unlockedTier, phaseIdx, skillDef, ability);
        }

        ModAdvancements.grant(player, ModAdvancements.DIVINE_CHANNELING);

        if (spendOutcome == EnergyService.DivineSpendOutcome.SUCCESS_TRIGGERED_OVERLOAD) {
            applyDivineOverloadPenalty(player);
        }

        ModCapabilities.syncEnergy(player);
    }

    public static void tickActiveUltimates(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }
        ActiveUltimateState state = ACTIVE_ULTIMATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        state.elapsedTicks++;
        state.remainingTicks--;

        switch (state.remnant) {
            case EYE_OF_RA -> {
                if (state.elapsedTicks == 40 || state.elapsedTicks == 80) {
                    executeRaWave(player, data, state.rawDamage, state.spiritRestoreAmount);
                }
            }
            case MORRIGAN_REAP -> {
                if (state.elapsedTicks == 40) {
                    executeMorriganWave(player, data, state.rawDamage, 9.0D, 12.0F);
                }
            }
            case INDRAS_VAJRA -> {
                if (state.elapsedTicks == 20 || state.elapsedTicks == 40
                        || state.elapsedTicks == 60 || state.elapsedTicks == 80) {
                    executeIndraStormStrike(player, data, state.rawDamage, state.bossBonusMult);
                }
            }
            case TEZCATLIPOCA_STEP -> {
                if (state.elapsedTicks == 12 || state.elapsedTicks == 24) {
                    executeTezcaEclipseDash(player, data, state.rawDamage * 0.85F);
                } else if (state.elapsedTicks == 36) {
                    executeTezcaObsidianDetonation(player, data, state.rawDamage * 0.70F);
                }
            }
        }

        if (state.remainingTicks <= 0) {
            ACTIVE_ULTIMATES.remove(player.getUUID());
        }
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
                ModSounds.SKILL_DIVINE_OVERLOAD.get(), SoundSource.PLAYERS, 0.9F, 0.95F);

        player.sendSystemMessage(
                Component.literal("⚠ [SOBRECARGA DIVINA] Sua Resistência Divina zerou! Você recebeu Fraqueza e o poder do Deus foi selado por 1 minuto.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
    }

    // -------------------------------------------------------------------------
    // 1. OLHO DE RÁ (H1–H4)
    // -------------------------------------------------------------------------
    private static void castEyeOfRaPhase(
            ServerPlayer player, PlayerCoreData data, ManifestationTier tier, int phaseIdx,
            DivineSkillDefinition def, DivinePhaseAbility ability
    ) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        int spiritStage = CultivationService.getCurrentStageNumber(pData, CultivationPath.SPIRIT);
        float rawDamage = def.baseDamage() + def.primaryRealmScale() * spiritRealm + def.secondaryScale() * spiritStage;

        List<LivingEntity> vfxTargets = List.of();
        double vfxRadius = 3.5D;

        switch (phaseIdx) {
            case 0 -> {
                // H1 · Faísca Solar: feixe focado no alvo na mira (<= 8m, sem AoE)
                LivingEntity target = findLookTarget(player, data, 8.0D, 0.70D);
                float dmg = rawDamage * 0.40F;
                if (target != null) {
                    ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                        target.hurt(player.damageSources().magic(), dmg);
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                    });
                    vfxTargets = List.of(target);
                }
                vfxRadius = 8.0D;
            }
            case 1 -> {
                // H2 · Égide do Disco Solar: escudo protetor + purificação (3.5m)
                double radius = 3.5D;
                float dmg = rawDamage * 0.45F;
                List<LivingEntity> targets = findValidTargets(player, data, radius);
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : targets) {
                        t.hurt(player.damageSources().magic(), dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
                        pushAway(player, t, 0.55D);
                    }
                });
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
                vfxTargets = targets;
                vfxRadius = radius;
            }
            case 2 -> {
                // H3 · Julgamento do Olho de Rá: área plena clássica
                double radius = Math.max(5.5D, def.baseRadius() + tier.getRadiusOffset());
                float dmg = rawDamage * tier.getPowerMultiplier();
                List<LivingEntity> targets = findValidTargets(player, data, radius);
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity target : targets) {
                        target.hurt(player.damageSources().magic(), dmg);
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                        if (tier == ManifestationTier.TIER_4) {
                            target.setSecondsOnFire(4);
                        }
                        pushAway(player, target, 0.45D + 0.10D * tier.ordinal());
                    }
                });
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
                vfxTargets = targets;
                vfxRadius = radius;
            }
            case 3 -> {
                // H4 · Zênite do Sol Imortal: 3 ondas de pilares + restauração de Integridade
                float waveDmg = rawDamage * 0.85F;
                float restorePerWave = pData.getCultivation().getMaxSpiritualIntegrity() * 0.08F;
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
                ACTIVE_ULTIMATES.put(player.getUUID(), new ActiveUltimateState(DivineRemnant.EYE_OF_RA, 120, waveDmg, 1.0F, restorePerWave));
                executeRaWave(player, data, waveDmg, restorePerWave);
                player.displayClientMessage(
                        Component.literal("☀ [" + ability.code() + "] " + ability.displayName() + "!")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                        true
                );
                return;
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.EYE_OF_RA, phaseIdx, player.position(), player.getLookAngle(),
                        (float) vfxRadius, toTargetSpots(vfxTargets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_RA_CAST.get(), SoundSource.PLAYERS, 0.85F, 1.0F + phaseIdx * 0.05F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_RA_IMPACT.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
        player.displayClientMessage(
                Component.literal("☀ [" + ability.code() + "] " + ability.displayName() + " (" + vfxTargets.size() + " alvos)")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                true
        );
    }

    private static void executeRaWave(ServerPlayer player, PlayerCoreData data, float waveDamage, float spiritRestore) {
        double radius = 10.0D;
        List<LivingEntity> targets = findValidTargets(player, data, radius);
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                float dmg = target.getMobType() == MobType.UNDEAD ? waveDamage * 1.35F : waveDamage;
                target.hurt(player.damageSources().magic(), dmg);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                target.setSecondsOnFire(4);
            }
        });
        CultivationData cult = data.getPlayerData().getCultivation();
        if (!cult.isSpiritShattered() && spiritRestore > 0.0F) {
            cult.setCurrentSpiritualIntegrity(cult.getCurrentSpiritualIntegrity() + spiritRestore);
            ModCapabilities.syncEnergy(player);
        }
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.EYE_OF_RA, 3, player.position(), player.getLookAngle(),
                        (float) radius, toTargetSpots(targets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_RA_IMPACT.get(), SoundSource.PLAYERS, 0.90F, 0.95F);
    }

    // -------------------------------------------------------------------------
    // 2. CEIFA DA MORRÍGAN (H1–H4)
    // -------------------------------------------------------------------------
    private static void castMorriganPhase(
            ServerPlayer player, PlayerCoreData data, ManifestationTier tier, int phaseIdx,
            DivineSkillDefinition def, DivinePhaseAbility ability
    ) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        float rawDamage = def.baseDamage() + def.primaryRealmScale() * bodyRealm + def.secondaryScale() * spiritRealm;

        List<LivingEntity> vfxTargets = List.of();
        double vfxRadius = 3.0D;

        switch (phaseIdx) {
            case 0 -> {
                // H1 · Corte do Presságio: meia-lua estreita frontal (3m) + micro-cura máx 2 HP
                vfxRadius = 3.0D;
                float dmg = rawDamage * 0.40F;
                List<LivingEntity> targets = findConeTargets(player, data, vfxRadius, 0.45D);
                final float[] dealt = { 0.0F };
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : targets) {
                        if (t.hurt(player.damageSources().playerAttack(player), dmg)) {
                            dealt[0] += dmg;
                        }
                        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
                    }
                });
                if (dealt[0] > 0.0F) {
                    player.heal(Math.min(2.0F, dealt[0] * 0.12F));
                }
                vfxTargets = targets;
            }
            case 1 -> {
                // H2 · Voo dos Corvos: revoada linear (10m) marcando com Wither + Lentidão
                vfxRadius = 10.0D;
                float dmg = rawDamage * 0.65F;
                List<LivingEntity> targets = findLineTargets(player, data, 10.0D, 1.35D);
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : targets) {
                        t.hurt(player.damageSources().magic(), dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    }
                });
                vfxTargets = targets;
            }
            case 2 -> {
                // H3 · Ceifa Abissal 360°: varredura circular plena + drenagem vital
                vfxRadius = Math.max(5.5D, def.baseRadius() + tier.getRadiusOffset());
                float dmg = rawDamage * tier.getPowerMultiplier();
                vfxTargets = executeMorriganWave(player, data, dmg, vfxRadius, 14.0F);
                player.displayClientMessage(
                        Component.literal("🪶 [" + ability.code() + "] " + ability.displayName() + " (" + vfxTargets.size() + " colhidos)")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        true
                );
                return;
            }
            case 3 -> {
                // H4 · Rainha dos Campos de Batalha: 8s de Asas Espectrais + 2 ondas amplas
                float waveDmg = rawDamage * 0.90F;
                ACTIVE_ULTIMATES.put(player.getUUID(), new ActiveUltimateState(DivineRemnant.MORRIGAN_REAP, 160, waveDmg, 1.0F, 0.0F));
                executeMorriganWave(player, data, waveDmg, 9.0D, 12.0F);
                player.displayClientMessage(
                        Component.literal("🪶 [" + ability.code() + "] " + ability.displayName() + " (8s)!")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        true
                );
                return;
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.MORRIGAN_REAP, phaseIdx, player.position(), player.getLookAngle(),
                        (float) vfxRadius, toTargetSpots(vfxTargets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_MORRIGAN_CAST.get(), SoundSource.PLAYERS, 0.85F, 0.95F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_MORRIGAN_REAP.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
        player.displayClientMessage(
                Component.literal("🪶 [" + ability.code() + "] " + ability.displayName() + " (" + vfxTargets.size() + " alvos)")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );
    }

    private static List<LivingEntity> executeMorriganWave(
            ServerPlayer player, PlayerCoreData data, float damage, double radius, float healCap
    ) {
        List<LivingEntity> targets = findValidTargets(player, data, radius);
        final float[] totalDamage = { 0.0F };
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                    totalDamage[0] += damage;
                }
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
        });
        if (totalDamage[0] > 0.0F) {
            player.heal(Math.min(healCap, totalDamage[0] * 0.20F));
        }
        int phaseForVfx = healCap <= 12.0F ? 3 : 2;
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.MORRIGAN_REAP, phaseForVfx, player.position(), player.getLookAngle(),
                        (float) radius, toTargetSpots(targets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_MORRIGAN_CAST.get(), SoundSource.PLAYERS, 0.85F, 0.95F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_MORRIGAN_REAP.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        return targets;
    }

    // -------------------------------------------------------------------------
    // 3. VAJRA DE INDRA (H1–H4)
    // -------------------------------------------------------------------------
    private static void castIndraPhase(
            ServerPlayer player, PlayerCoreData data, ManifestationTier tier, int phaseIdx,
            DivineSkillDefinition def, DivinePhaseAbility ability
    ) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int bodyStage = CultivationService.getCurrentStageNumber(pData, CultivationPath.BODY);
        float rawDamage = def.baseDamage() + def.primaryRealmScale() * bodyRealm + def.secondaryScale() * bodyStage;

        List<LivingEntity> vfxTargets = List.of();
        double vfxRadius = 4.0D;

        switch (phaseIdx) {
            case 0 -> {
                // H1 · Centelha do Vajra: estalo curto em até 2 alvos à frente (4m)
                vfxRadius = 4.0D;
                float dmg = rawDamage * 0.40F;
                List<LivingEntity> cone = findConeTargets(player, data, 4.0D, 0.35D);
                cone.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
                List<LivingEntity> hits = cone.size() > 2 ? cone.subList(0, 2) : cone;
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : hits) {
                        t.hurt(player.damageSources().sonicBoom(player), dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                    }
                });
                vfxTargets = hits;
            }
            case 1 -> {
                // H2 · Lança Trovejante: raio 12m + salto elétrico para até 2 vizinhos
                vfxRadius = 12.0D;
                float dmg = rawDamage * 0.75F;
                LivingEntity primary = findLookTarget(player, data, 12.0D, 0.65D);
                List<LivingEntity> hits = new ArrayList<>();
                if (primary != null) {
                    hits.add(primary);
                    List<LivingEntity> allValid = findValidTargets(player, data, 14.0D);
                    allValid.remove(primary);
                    allValid.sort(Comparator.comparingDouble(e -> e.distanceToSqr(primary)));
                    for (LivingEntity extra : allValid) {
                        if (extra.distanceToSqr(primary) <= 20.25D && hits.size() < 3) {
                            hits.add(extra);
                        }
                    }
                } else {
                    hits.addAll(findLineTargets(player, data, 12.0D, 1.0D));
                }
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : hits) {
                        t.hurt(player.damageSources().sonicBoom(player), dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                    }
                });
                vfxTargets = hits;
            }
            case 2 -> {
                // H3 · Trovão Soberano do Vajra: explosão sônica plena + bônus chefe
                vfxRadius = Math.max(6.5D, def.baseRadius() + tier.getRadiusOffset());
                float baseDmg = rawDamage * tier.getPowerMultiplier();
                List<LivingEntity> targets = findValidTargets(player, data, vfxRadius);
                float bossMult = (tier == ManifestationTier.TIER_4 ? 1.65F : 1.50F) * def.bossDamageBonusMultiplier();
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity target : targets) {
                        boolean isBoss = target.getType().is(ModTags.HIGH_REALM_BOSS);
                        target.hurt(player.damageSources().sonicBoom(player), isBoss ? baseDmg * bossMult : baseDmg);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 2));
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 1));
                        pushAway(player, target, isBoss ? 0.35D : 1.05D);
                    }
                });
                vfxTargets = targets;
            }
            case 3 -> {
                // H4 · Tempestade de Mil Raios: 5 relâmpagos consecutivos (t=0,20,40,60,80)
                float strikeDmg = rawDamage * 0.90F;
                float bossBonus = 1.75F * def.bossDamageBonusMultiplier();
                ACTIVE_ULTIMATES.put(player.getUUID(), new ActiveUltimateState(DivineRemnant.INDRAS_VAJRA, 100, strikeDmg, bossBonus, 0.0F));
                executeIndraStormStrike(player, data, strikeDmg, bossBonus);
                player.displayClientMessage(
                        Component.literal("⚡ [" + ability.code() + "] " + ability.displayName() + "!")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        true
                );
                return;
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.INDRAS_VAJRA, phaseIdx, player.position(), player.getLookAngle(),
                        (float) vfxRadius, toTargetSpots(vfxTargets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_INDRA_CHARGE.get(), SoundSource.PLAYERS, 0.70F, 1.05F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_INDRA_THUNDER.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(
                Component.literal("⚡ [" + ability.code() + "] " + ability.displayName() + " (" + vfxTargets.size() + " fulminados)")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                true
        );
    }

    private static void executeIndraStormStrike(
            ServerPlayer player, PlayerCoreData data, float baseDamage, float bossMult
    ) {
        double radius = 10.0D;
        List<LivingEntity> targets = findValidTargets(player, data, radius);
        targets.sort((a, b) -> Boolean.compare(b.getType().is(ModTags.HIGH_REALM_BOSS), a.getType().is(ModTags.HIGH_REALM_BOSS)));
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                boolean isBoss = target.getType().is(ModTags.HIGH_REALM_BOSS);
                target.hurt(player.damageSources().sonicBoom(player), isBoss ? baseDamage * bossMult : baseDamage);
                if (target instanceof Player tp && tp.isBlocking()) {
                    tp.disableShield(true);
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            }
        });
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.INDRAS_VAJRA, 3, player.position(), player.getLookAngle(),
                        (float) radius, toTargetSpots(targets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_INDRA_THUNDER.get(), SoundSource.PLAYERS, 0.90F, 1.0F);
    }

    // -------------------------------------------------------------------------
    // 4. PASSO DE TEZCATLIPOCA (H1–H4)
    // -------------------------------------------------------------------------
    private static void castTezcatlipocaPhase(
            ServerPlayer player, PlayerCoreData data, ManifestationTier tier, int phaseIdx,
            DivineSkillDefinition def, DivinePhaseAbility ability
    ) {
        ServerLevel level = player.serverLevel();
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        float rawDamage = def.baseDamage() + bodyRealm * def.primaryRealmScale() + spiritRealm * def.secondaryScale();

        Vec3 startOrigin = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        horizontal = horizontal.lengthSqr() > 1.0E-4D ? horizontal.normalize() : new Vec3(0.0D, 0.0D, 1.0D);

        List<LivingEntity> vfxTargets = List.of();
        double vfxRadius = 3.5D;

        switch (phaseIdx) {
            case 0 -> {
                // H1 · Fenda Fumegante: esquiva curta (3.5m) + corte leve
                vfxRadius = 3.5D;
                float dmg = rawDamage * 0.40F;
                LivingEntity hit = findLookTarget(player, data, 3.5D, 0.45D);
                if (hit != null) {
                    ContractGameplayEvents.runBypassingInfusionHooks(() -> hit.hurt(player.damageSources().playerAttack(player), dmg));
                    vfxTargets = List.of(hit);
                }
                player.setDeltaMovement(horizontal.x * 0.72D, Math.max(0.24D, player.getDeltaMovement().y + 0.20D), horizontal.z * 0.72D);
                player.hurtMarked = true;
                player.fallDistance *= 0.5F;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0));
            }
            case 1 -> {
                // H2 · Reflexo do Espelho: cegueira/lentidão na origem + invisibilidade + recuo
                vfxRadius = 3.5D;
                float dmg = rawDamage * 0.45F;
                List<LivingEntity> targets = findValidTargets(player, data, vfxRadius);
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : targets) {
                        t.hurt(player.damageSources().playerAttack(player), dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    }
                });
                player.setDeltaMovement(-horizontal.x * 0.85D, Math.max(0.28D, player.getDeltaMovement().y + 0.25D), -horizontal.z * 0.85D);
                player.hurtMarked = true;
                player.fallDistance = 0.0F;
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1));
                vfxTargets = targets;
            }
            case 2 -> {
                // H3 · Passo Pleno: corte ao redor + investida longa + buffs
                vfxRadius = Math.max(4.5D, def.baseRadius() + tier.getRadiusOffset());
                float dmg = rawDamage * tier.getPowerMultiplier();
                List<LivingEntity> targets = findValidTargets(player, data, vfxRadius);
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    for (LivingEntity t : targets) {
                        t.hurt(player.damageSources().playerAttack(player), dmg);
                    }
                });
                player.setDeltaMovement(horizontal.x * 1.40D, Math.max(0.32D, player.getDeltaMovement().y + 0.32D), horizontal.z * 1.40D);
                player.hurtMarked = true;
                player.fallDistance = 0.0F;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0));
                vfxTargets = targets;
            }
            case 3 -> {
                // H4 · Eclipse do Jaguar Negro: 3 avanços em zigue-zague + explosão de obsidiana
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0));
                ACTIVE_ULTIMATES.put(player.getUUID(), new ActiveUltimateState(DivineRemnant.TEZCATLIPOCA_STEP, 38, rawDamage, 1.0F, 0.0F));
                executeTezcaEclipseDash(player, data, rawDamage * 0.85F);
                player.displayClientMessage(
                        Component.literal("🌑 [" + ability.code() + "] " + ability.displayName() + "!")
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
                        true
                );
                return;
            }
        }

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.TEZCATLIPOCA_STEP, phaseIdx, startOrigin, look,
                        (float) vfxRadius, toTargetSpots(vfxTargets)),
                level, startOrigin, VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_TEZCA_STEP.get(), SoundSource.PLAYERS, 0.78F, 1.05F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_TEZCA_SLASH.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(
                Component.literal("🌑 [" + ability.code() + "] " + ability.displayName() + "!")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
                true
        );
    }

    private static void executeTezcaEclipseDash(ServerPlayer player, PlayerCoreData data, float damage) {
        Vec3 origin = player.position();
        List<LivingEntity> candidates = findValidTargets(player, data, 10.0D);
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        LivingEntity chosen = candidates.isEmpty() ? null : candidates.get(0);
        Vec3 look = player.getLookAngle();
        if (chosen != null) {
            Vec3 to = chosen.position().subtract(origin).normalize();
            Vec3 dest = chosen.position().add(to.scale(1.2D));
            if (player.serverLevel().noCollision(player, player.getBoundingBox().move(dest.subtract(origin)))) {
                player.teleportTo(dest.x, chosen.getY(), dest.z);
            }
            ContractGameplayEvents.runBypassingInfusionHooks(() -> chosen.hurt(player.damageSources().playerAttack(player), damage));
            look = to;
        } else {
            Vec3 horiz = new Vec3(look.x, 0.0D, look.z).normalize().scale(1.35D);
            player.setDeltaMovement(horiz.x, 0.25D, horiz.z);
            player.hurtMarked = true;
        }
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.TEZCATLIPOCA_STEP, 3, origin, look,
                        5.0F, chosen != null ? toTargetSpots(List.of(chosen)) : List.of()),
                level, origin, VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_TEZCA_STEP.get(), SoundSource.PLAYERS, 0.80F, 1.15F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_TEZCA_SLASH.get(), SoundSource.PLAYERS, 0.85F, 1.05F);
    }

    private static void executeTezcaObsidianDetonation(ServerPlayer player, PlayerCoreData data, float damage) {
        double radius = 5.5D;
        List<LivingEntity> targets = findValidTargets(player, data, radius);
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity t : targets) {
                t.hurt(player.damageSources().playerAttack(player), damage);
                pushAway(player, t, 0.65D);
            }
        });
        ServerLevel level = player.serverLevel();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(EffectType.TEZCATLIPOCA_STEP, 3, player.position(), player.getLookAngle(),
                        (float) radius, toTargetSpots(targets)),
                level, player.position(), VFX_BROADCAST_RADIUS
        );
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SKILL_TEZCA_SLASH.get(), SoundSource.PLAYERS, 0.95F, 0.90F);
    }

    // -------------------------------------------------------------------------
    // TARGETING HELPERS
    // -------------------------------------------------------------------------
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
            if (!isTargetValid(player, protectVillagers, entity)) {
                return false;
            }
            return entity.distanceToSqr(player) <= radius * radius;
        });
    }

    private static LivingEntity findLookTarget(ServerPlayer player, PlayerCoreData data, double range, double minDot) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(range), e -> isTargetValid(player, protectVillagers, e))) {
            double distSq = entity.distanceToSqr(player);
            if (distSq > range * range) {
                continue;
            }
            Vec3 to = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            if (look.dot(to) >= minDot && distSq < bestDistSq) {
                best = entity;
                bestDistSq = distSq;
            }
        }
        return best;
    }

    private static List<LivingEntity> findConeTargets(ServerPlayer player, PlayerCoreData data, double range, double minDot) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> {
            if (!isTargetValid(player, protectVillagers, e) || e.distanceToSqr(player) > range * range) {
                return false;
            }
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye).normalize();
            return look.dot(to) >= minDot || e.distanceToSqr(player) <= 4.0D;
        });
    }

    private static List<LivingEntity> findLineTargets(ServerPlayer player, PlayerCoreData data, double range, double width) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(width);
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e ->
                isTargetValid(player, protectVillagers, e) && e.getBoundingBox().inflate(width).clip(eye, end).isPresent());
    }

    private static boolean isTargetValid(ServerPlayer player, boolean protectVillagers, LivingEntity entity) {
        if (entity == null || entity == player || !entity.isAlive() || entity.isAlliedTo(player)) {
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
        return true;
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

    private static final class ActiveUltimateState {
        final DivineRemnant remnant;
        final float rawDamage;
        final float bossBonusMult;
        final float spiritRestoreAmount;
        int remainingTicks;
        int elapsedTicks = 0;

        ActiveUltimateState(
                DivineRemnant remnant,
                int durationTicks,
                float rawDamage,
                float bossBonusMult,
                float spiritRestoreAmount
        ) {
            this.remnant = remnant;
            this.remainingTicks = durationTicks;
            this.rawDamage = rawDamage;
            this.bossBonusMult = bossBonusMult;
            this.spiritRestoreAmount = spiritRestoreAmount;
        }
    }
}
