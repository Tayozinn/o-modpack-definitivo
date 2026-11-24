package com.mythiccontracts.skill.cultivation;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.contract.PaladinVowContract;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.event.ContractGameplayEvents;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.TargetSpot;
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.subjugation.SubjugationService;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Serviço autoritativo para desbloqueio, equipamento em múltiplos slots (S1–S3)
 * e execução das 12 Técnicas de Cultivo R1–R7 (incluindo {@code soul_subjugation}).
 */
public final class CultivationSkillService {

    public record ValidationResult(boolean allowed, String reason) {
    }

    private CultivationSkillService() {
    }

    public static int getUnlockedMartialSlotCount(PlayerData data) {
        if (data == null) {
            return 1;
        }
        int bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY).order();
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        int highest = Math.max(bodyRealm, spiritRealm);
        if (highest >= 5) {
            return 3;
        }
        if (highest >= 3) {
            return 2;
        }
        return 1;
    }

    public static ValidationResult validateUnlock(PlayerData data, CultivationSkillDefinition def) {
        if (data == null || def == null) {
            return new ValidationResult(false, "Técnica inválida.");
        }
        if (!def.enabled()) {
            return new ValidationResult(false, "Esta técnica está desativada pela configuração.");
        }
        if (data.getSkills().hasSkillUnlocked(def.id())) {
            return new ValidationResult(false, "Técnica já desbloqueada.");
        }
        if (data.getCultivation().isSpiritShattered()) {
            return new ValidationResult(false, "✖ [Colapso Espiritual] Impossível compreender novas técnicas agora.");
        }

        SkillUnlockRequirements req = def.requirements();
        FactionType faction = FactionType.fromString(data.getFaction().getCurrentFactionId().getPath());
        if (!req.allowedFactions().isEmpty() && !req.allowedFactions().contains(faction)) {
            return new ValidationResult(false, "✖ Facção atual não autorizada para esta técnica.");
        }

        boolean bypassRealm = req.realmBypassFactions().contains(faction);
        if (!bypassRealm) {
            int bodyR = CultivationService.getCurrentRealm(data, CultivationPath.BODY).order();
            int bodyS = CultivationService.getCurrentStageNumber(data, CultivationPath.BODY);
            if (bodyR < req.minBodyRealm() || (bodyR == req.minBodyRealm() && bodyS < req.minBodyStage())) {
                return new ValidationResult(false,
                        "✖ Requer Cultivo Corporal R" + req.minBodyRealm() + "-" + req.minBodyStage() + "+.");
            }
            int spiritR = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
            int spiritS = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);
            if (spiritR < req.minSpiritRealm() || (spiritR == req.minSpiritRealm() && spiritS < req.minSpiritStage())) {
                return new ValidationResult(false,
                        "✖ Requer Cultivo Espiritual R" + req.minSpiritRealm() + "-" + req.minSpiritStage() + "+.");
            }
        }

        if (req.prerequisiteSkill() != null && !data.getSkills().hasSkillUnlocked(req.prerequisiteSkill())) {
            String preName = CultivationSkillRegistry.getDisplayName(req.prerequisiteSkill());
            return new ValidationResult(false, "✖ Requer técnica anterior: " + preName + ".");
        }

        boolean hasContract = !data.getContracts().getActiveContractIds().isEmpty();
        int boundCount = data.getSubjugation().getBoundEntities().size();
        int rep = data.getFaction().getReputation(data.getFaction().getCurrentFactionId());

        if (req.contractOrAlternative()) {
            boolean altServant = req.minSubjugatedCount() > 0 && boundCount >= req.minSubjugatedCount();
            boolean altRep = req.minReputation() > 0 && rep >= req.minReputation();
            if (!hasContract && !altServant && !altRep) {
                return new ValidationResult(false, "✖ Requer Contrato ativo ou requisito alternativo de facção/servos.");
            }
        } else {
            if (req.requireActiveContract() && !hasContract) {
                return new ValidationResult(false, "✖ Requer ao menos 1 Contrato Mítico ativo.");
            }
            if (req.minSubjugatedCount() > 0 && boundCount < req.minSubjugatedCount()) {
                return new ValidationResult(false, "✖ Requer " + req.minSubjugatedCount() + "+ entidades subjugadas.");
            }
            if (req.minReputation() > 0 && rep < req.minReputation()) {
                return new ValidationResult(false, "✖ Requer reputação " + req.minReputation() + "+ na facção.");
            }
        }

        if (req.qiUnlockCost() > 0.0F && data.getCultivation().getCurrentQi() < req.qiUnlockCost()) {
            return new ValidationResult(false,
                    String.format(Locale.ROOT, "✖ Qi insuficiente (Requer %.0f Qi para desbloquear).", req.qiUnlockCost()));
        }

        return new ValidationResult(true, "");
    }

    public static boolean executeUnlockSkill(ServerPlayer player, PlayerCoreData coreData, ResourceLocation skillId) {
        if (player == null || coreData == null || skillId == null) {
            return false;
        }
        CultivationSkillDefinition def = CultivationSkillRegistry.getSkill(skillId);
        PlayerData pData = coreData.getPlayerData();
        ValidationResult res = validateUnlock(pData, def);
        if (!res.allowed()) {
            player.displayClientMessage(Component.literal(res.reason()).withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        float unlockCost = def.requirements().qiUnlockCost();
        if (!player.getAbilities().instabuild && unlockCost > 0.0F && !EnergyService.consumeQi(pData, unlockCost)) {
            return false;
        }

        SkillData skills = pData.getSkills();
        skills.unlockSkill(def.id());

        // Equipa automaticamente no primeiro slot livre desbloqueado
        int unlockedSlots = getUnlockedMartialSlotCount(pData);
        for (int i = 0; i < unlockedSlots; i++) {
            if (skills.getMartialSkillInSlot(i) == null) {
                skills.setMartialSkillInSlot(i, def.id());
                skills.setActiveMartialSlot(i);
                break;
            }
        }

        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.35F);
        player.displayClientMessage(
                Component.literal("✦ [Técnica Compreendida] " + def.displayName() + "!")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                true
        );
        ModCapabilities.sync(player);
        ModCapabilities.syncEnergy(player);
        return true;
    }

    public static boolean executeEquipMartialSkill(
            ServerPlayer player,
            PlayerCoreData coreData,
            ResourceLocation skillId,
            int slotIndex
    ) {
        if (player == null || coreData == null || skillId == null) {
            return false;
        }
        PlayerData pData = coreData.getPlayerData();
        CultivationSkillDefinition def = CultivationSkillRegistry.getSkill(skillId);
        if (def == null || !def.enabled()) {
            player.displayClientMessage(Component.literal("✖ Técnica indisponível.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        SkillData skills = pData.getSkills();
        if (!skills.hasSkillUnlocked(skillId)) {
            player.displayClientMessage(Component.literal("✖ Desbloqueie esta técnica antes de equipá-la.").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        int unlockedSlots = getUnlockedMartialSlotCount(pData);
        int targetSlot = slotIndex < 0 ? skills.getActiveMartialSlot() : slotIndex;
        if (targetSlot < 0 || targetSlot >= unlockedSlots) {
            int reqRealm = targetSlot == 1 ? 3 : 5;
            player.displayClientMessage(
                    Component.literal("✖ Slot S" + (targetSlot + 1) + " requer Cultivo R" + reqRealm + "+.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }
        if (skills.getCooldown(skillId) > 0) {
            player.displayClientMessage(
                    Component.literal("⏳ Aguarde o fim da recarga para reequipar esta técnica.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        skills.setMartialSkillInSlot(targetSlot, skillId);
        skills.setActiveMartialSlot(targetSlot);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SPIRIT_SLOT_CYCLE.get(), SoundSource.PLAYERS, 0.75F, 1.1F);
        player.displayClientMessage(
                Component.literal("✦ [Slot S" + (targetSlot + 1) + "] " + def.displayName() + " equipada!")
                        .withStyle(ChatFormatting.GOLD),
                true
        );
        ModCapabilities.sync(player);
        ModCapabilities.syncEnergy(player);
        return true;
    }

    public static void executeCycleMartialSlot(ServerPlayer player, PlayerCoreData coreData) {
        if (player == null || coreData == null || !player.isAlive() || player.isDeadOrDying()) {
            return;
        }
        PlayerData pData = coreData.getPlayerData();
        SkillData skills = pData.getSkills();
        int maxSlots = getUnlockedMartialSlotCount(pData);
        int next = (skills.getActiveMartialSlot() + 1) % maxSlots;
        skills.setActiveMartialSlot(next);

        ResourceLocation equipped = skills.getMartialSkillInSlot(next);
        String name = equipped != null ? CultivationSkillRegistry.getDisplayName(equipped) : "Vazio";
        player.displayClientMessage(
                Component.literal("❖ Slot Marcial Ativo [S" + (next + 1) + "/" + maxSlots + "]: " + name)
                        .withStyle(ChatFormatting.AQUA),
                true
        );
        ModCapabilities.syncEnergy(player);
    }

    public static void executeActiveMartialSkill(ServerPlayer player, PlayerCoreData coreData) {
        if (player == null || coreData == null || !player.isAlive() || player.isDeadOrDying()) {
            return;
        }
        // Prioridade 1: se estiver em Disputa Espiritual ativa na janela de Pulso de Revolta, suprime na hora
        if (SubjugationService.hasActiveSession(player.getUUID())) {
            SubjugationService.tryActivateSkill(player, coreData);
            return;
        }

        PlayerData pData = coreData.getPlayerData();
        SkillData skills = pData.getSkills();
        ResourceLocation activeId = skills.getActiveMartialSkill();
        if (activeId == null) {
            player.displayClientMessage(
                    Component.literal("✦ Nenhuma Técnica equipada neste slot. Abra [U] → Habilidades para equipar.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (SubjugationService.SOUL_SUBJUGATION_ID.equals(activeId)) {
            if (!skills.hasSkillUnlocked(activeId)) {
                player.displayClientMessage(Component.literal("✖ Técnica não desbloqueada.").withStyle(ChatFormatting.RED), true);
                return;
            }
            SubjugationService.tryActivateSkill(player, coreData);
            return;
        }

        CultivationSkillDefinition def = CultivationSkillRegistry.getSkill(activeId);
        if (def == null || !def.enabled() || !skills.hasSkillUnlocked(activeId)) {
            player.displayClientMessage(Component.literal("✖ Técnica indisponível.").withStyle(ChatFormatting.RED), true);
            return;
        }

        CultivationData cult = pData.getCultivation();
        if (cult.isSpiritShattered() && !def.usableDuringShatter()) {
            player.displayClientMessage(
                    Component.literal("✖ [Colapso Espiritual] Técnicas bloqueadas durante fratura da alma!")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        int cd = skills.getCooldown(activeId);
        if (cd > 0) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "⏳ %s em recarga: %.1fs", def.displayName(), cd / 20.0F))
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (cult.getCurrentQi() < def.qiCost()) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "✖ Qi insuficiente (Requer %.0f Qi).", def.qiCost()))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        if (cult.getCurrentStamina() < def.staminaCost()) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "✖ Vigor insuficiente (Requer %.0f Vigor).", def.staminaCost()))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        if (def.spiritualIntegrityCost() > 0.0F && cult.getCurrentSpiritualIntegrity() <= def.spiritualIntegrityCost()) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "✖ Integridade Espiritual insuficiente (> %.0f).", def.spiritualIntegrityCost()))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        cult.setCurrentQi(cult.getCurrentQi() - def.qiCost());
        if (def.staminaCost() > 0.0F) {
            VitalService.consumeStamina(pData, def.staminaCost(), 20);
        }
        if (def.spiritualIntegrityCost() > 0.0F) {
            cult.setCurrentSpiritualIntegrity(Math.max(1.0F, cult.getCurrentSpiritualIntegrity() - def.spiritualIntegrityCost()));
        }
        skills.setCooldown(activeId, def.cooldownTicks());

        dispatchTechniqueEffect(player, coreData, def);
        ModCapabilities.syncEnergy(player);
    }

    private static void dispatchTechniqueEffect(ServerPlayer player, PlayerCoreData coreData, CultivationSkillDefinition def) {
        String path = def.id().getPath();
        switch (path) {
            case "gale_meridian_step" -> castGaleMeridianStep(player, def);
            case "inner_breath_meditation" -> castInnerBreathMeditation(player, coreData, def);
            case "jade_bone_guard" -> castJadeBoneGuard(player, coreData, def);
            case "seismic_palm" -> castSeismicPalm(player, coreData, def);
            case "golden_core_aegis" -> castGoldenCoreAegis(player, coreData, def);
            case "blood_furnace_surge" -> castBloodFurnaceSurge(player, coreData, def);
            case "nascent_soul_lance" -> castNascentSoulLance(player, coreData, def);
            case "ether_flash_strike" -> castEtherFlashStrike(player, coreData, def);
            case "spirit_domain_suppression" -> castSpiritDomainSuppression(player, coreData, def);
            case "undying_meridian_reversal" -> castUndyingMeridianReversal(player, coreData, def);
            case "primordial_heaven_sunder" -> castPrimordialHeavenSunder(player, coreData, def);
            default -> {
            }
        }
    }

    private static void castGaleMeridianStep(ServerPlayer player, CultivationSkillDefinition def) {
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0D, look.z);
        horiz = horiz.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : horiz.normalize().scale(1.15D);
        player.setDeltaMovement(horiz.x, Math.max(0.28D, player.getDeltaMovement().y + 0.25D), horiz.z);
        player.hurtMarked = true;
        player.fallDistance *= 0.5F;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        broadcastVfx(player, EffectType.GALE_MERIDIAN_STEP, 0, 3.0F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_GALE_STEP.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("✦ " + def.displayName() + "!").withStyle(ChatFormatting.RED), true);
    }

    private static void castInnerBreathMeditation(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        data.getPlayerData().getCultivation().setMeditationBoostTicks(120);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        broadcastVfx(player, EffectType.INNER_BREATH_MEDITATION, 0, 2.5F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_LOTUS_BREATH.get(), SoundSource.PLAYERS, 0.80F, 1.0F);
        player.displayClientMessage(Component.literal("✦ " + def.displayName() + " (Meditação 6s)!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void castJadeBoneGuard(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        data.getPlayerData().getCultivation().setJadeBoneGuardTicks(80);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
        broadcastVfx(player, EffectType.JADE_BONE_GUARD, 1, 2.0F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_JADE_GUARD.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("✦ " + def.displayName() + " (4s)!").withStyle(ChatFormatting.GOLD), true);
    }

    private static void castSeismicPalm(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        PlayerData pData = data.getPlayerData();
        int bodyRealm = CultivationService.getCurrentRealm(pData, CultivationPath.BODY).order();
        double atk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (8.0D + 2.5D * bodyRealm + atk * 0.65D);

        List<LivingEntity> targets = findConeTargets(player, data, 6.5D, 0.25D);
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), damage);
                if (target instanceof Player tp && tp.isBlocking()) {
                    tp.disableShield(true);
                }
                pushDirection(player.position(), target, 1.15D);
            }
        });
        broadcastVfx(player, EffectType.SEISMIC_PALM, Math.min(3, bodyRealm / 2), 6.5F, toTargetSpots(targets));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_SEISMIC_PALM.get(), SoundSource.PLAYERS, 0.90F, 1.0F);
        player.displayClientMessage(
                Component.literal("⚔ " + def.displayName() + "! (" + targets.size() + " atingidos)")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                true
        );
    }

    private static void castGoldenCoreAegis(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        data.getPlayerData().getCultivation().setGoldenCoreAegisTicks(120);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1));
        broadcastVfx(player, EffectType.GOLDEN_CORE_AEGIS, 1, 2.5F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_GOLDEN_AEGIS.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("✦ " + def.displayName() + " (Absorção 75% + Reflexão 20%)!").withStyle(ChatFormatting.GOLD), true);
    }

    private static void castBloodFurnaceSurge(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        data.getPlayerData().getCultivation().setBloodFurnaceTicks(80);
        player.heal(player.getMaxHealth() * 0.05F);
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1));
        broadcastVfx(player, EffectType.BLOOD_FURNACE_SURGE, 2, 2.2F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_BLOOD_FURNACE.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(Component.literal("✦ " + def.displayName() + " (Regen 15% + Haste)!").withStyle(ChatFormatting.DARK_RED), true);
    }

    private static void castNascentSoulLance(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        PlayerData pData = data.getPlayerData();
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        int spiritStage = CultivationService.getCurrentStageNumber(pData, CultivationPath.SPIRIT);
        float damage = 10.0F + 3.5F * spiritRealm + 1.5F * spiritStage;

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(18.0D));
        AABB lineBox = player.getBoundingBox().expandTowards(look.scale(18.0D)).inflate(1.25D);
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();

        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class, lineBox, e -> isHostileOrValid(player, protectVillagers, e))) {
            if (e.getBoundingBox().inflate(0.75D).clip(eye, end).isPresent()) {
                hits.add(e);
            }
        }
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity hit : hits) {
                hit.hurt(player.damageSources().indirectMagic(player, player), damage);
            }
        });
        broadcastVfx(player, EffectType.NASCENT_SOUL_LANCE, 2, 18.0F, toTargetSpots(hits));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_SOUL_LANCE.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        player.displayClientMessage(
                Component.literal("✦ " + def.displayName() + "! (" + hits.size() + " perfurados)").withStyle(ChatFormatting.AQUA),
                true
        );
    }

    private static void castEtherFlashStrike(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        PlayerData pData = data.getPlayerData();
        LivingEntity target = SubjugationService.findTargetedLivingEntity(player, 10.0D);
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();
        if (target == null || !isHostileOrValid(player, protectVillagers, target)) {
            List<LivingEntity> cone = findConeTargets(player, data, 10.0D, 0.55D);
            target = cone.isEmpty() ? null : cone.get(0);
        }

        Vec3 start = player.position();
        Vec3 look = player.getLookAngle();
        List<TargetSpot> spots = List.of();
        if (target != null) {
            Vec3 behind = target.position().add(look.scale(1.2D));
            if (player.serverLevel().noCollision(player, player.getBoundingBox().move(behind.subtract(start)))) {
                player.teleportTo(behind.x, target.getY(), behind.z);
            } else {
                Vec3 front = target.position().subtract(look.scale(1.2D));
                player.teleportTo(front.x, target.getY(), front.z);
            }
            float baseDmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float total = baseDmg
                    + CultivationBalance.calculateBodyInfusionBuff(baseDmg, pData)
                    + CultivationBalance.calculateSpiritInfusionFlatBonus(pData);
            final LivingEntity hit = target;
            ContractGameplayEvents.runBypassingInfusionHooks(() -> hit.hurt(player.damageSources().playerAttack(player), total));
            spots = toTargetSpots(List.of(target));
        } else {
            Vec3 horiz = new Vec3(look.x, 0.0D, look.z).normalize().scale(1.4D);
            player.setDeltaMovement(horiz.x, 0.25D, horiz.z);
            player.hurtMarked = true;
        }
        broadcastVfx(player, EffectType.ETHER_FLASH_STRIKE, 3, 10.0F, spots);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_ETHER_FLASH.get(), SoundSource.PLAYERS, 0.90F, 1.0F);
        player.displayClientMessage(Component.literal("⚡ " + def.displayName() + "!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
    }

    private static void castSpiritDomainSuppression(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        data.getPlayerData().getCultivation().setSpiritDomainTicks(160);
        applySpiritDomainPulse(player, data);
        broadcastVfx(player, EffectType.SPIRIT_DOMAIN_SUPPRESSION, 2, 8.0F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_SPIRIT_DOMAIN.get(), SoundSource.PLAYERS, 0.90F, 1.0F);
        player.displayClientMessage(Component.literal("❖ " + def.displayName() + " (Aura 8m / 8s)!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
    }

    public static void applySpiritDomainPulse(ServerPlayer owner, PlayerCoreData data) {
        AABB box = owner.getBoundingBox().inflate(8.0D);
        int ownerSpirit = CultivationService.getCurrentRealm(data.getPlayerData(), CultivationPath.SPIRIT).order();
        for (LivingEntity e : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == owner) {
                continue;
            }
            if (com.mythiccontracts.item.SubjugationSealItem.isAlliedWithOwner(e, owner.getUUID())) {
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
            } else if (!e.getType().is(ModTags.HIGH_REALM_BOSS) || ownerSpirit >= 7) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, true));
            }
        }
    }

    private static void castUndyingMeridianReversal(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        CultivationData cult = data.getPlayerData().getCultivation();
        cult.setSpiritShatterTicks(0);
        float targetIntegrity = Math.max(cult.getCurrentSpiritualIntegrity(), cult.getMaxSpiritualIntegrity() * 0.40F);
        cult.setCurrentSpiritualIntegrity(targetIntegrity);
        cult.setUndyingReversalTicks(60);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
        broadcastVfx(player, EffectType.UNDYING_MERIDIAN_REVERSAL, 3, 3.0F, List.of());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_UNDYING_REVERSAL.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
        player.displayClientMessage(Component.literal("✦ [Reversão Imortal] Colapso purgado +40% Integridade!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
    }

    private static void castPrimordialHeavenSunder(ServerPlayer player, PlayerCoreData data, CultivationSkillDefinition def) {
        PlayerData pData = data.getPlayerData();
        double baseAtk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float bodyPower = (float) baseAtk + CultivationBalance.calculateBodyInfusionBuff((float) baseAtk, pData);
        float spiritPower = CultivationBalance.calculateSpiritInfusionFlatBonus(pData)
                + CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order() * 3.0F;
        float totalDamage = 2.5F * bodyPower + 2.5F * spiritPower;

        List<LivingEntity> targets = findConeTargets(player, data, 10.0D, 0.10D);
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            for (LivingEntity t : targets) {
                t.hurt(player.damageSources().sonicBoom(player), totalDamage);
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                pushDirection(player.position(), t, 1.5D);
            }
        });
        broadcastVfx(player, EffectType.PRIMORDIAL_HEAVEN_SUNDER, 3, 10.0F, toTargetSpots(targets));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TECH_HEAVEN_SUNDER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.literal("✦ [TÉCNICA SUPREMA] " + def.displayName() + "! (" + targets.size() + " atingidos)")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false
        );
    }

    private static List<LivingEntity> findConeTargets(ServerPlayer player, PlayerCoreData data, double range, double minDot) {
        AABB box = player.getBoundingBox().inflate(range);
        Vec3 look = player.getLookAngle().normalize();
        boolean protectVillagers = data.hasContract(PaladinVowContract.ID) && !data.isVowBroken();
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!isHostileOrValid(player, protectVillagers, e)) {
                return false;
            }
            if (e.distanceToSqr(player) > range * range) {
                return false;
            }
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(player.getEyePosition(1.0F)).normalize();
            return look.dot(to) >= minDot || e.distanceToSqr(player) <= 6.25D;
        });
    }

    private static boolean isHostileOrValid(ServerPlayer player, boolean protectVillagers, LivingEntity entity) {
        if (entity == null || entity == player || !entity.isAlive() || entity.isAlliedTo(player)) {
            return false;
        }
        if (entity instanceof Player other && (other.isSpectator() || other.isCreative() || !player.canHarmPlayer(other))) {
            return false;
        }
        if (protectVillagers && entity instanceof AbstractVillager) {
            return false;
        }
        return !com.mythiccontracts.item.SubjugationSealItem.isAlliedWithOwner(entity, player.getUUID());
    }

    private static void pushDirection(Vec3 origin, LivingEntity target, double strength) {
        Vec3 diff = target.position().subtract(origin);
        Vec3 horiz = new Vec3(diff.x, 0.0D, diff.z);
        if (horiz.lengthSqr() < 1.0E-4D) {
            horiz = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horiz = horiz.normalize().scale(strength);
        target.push(horiz.x, 0.35D, horiz.z);
        target.hurtMarked = true;
    }

    private static List<TargetSpot> toTargetSpots(List<LivingEntity> targets) {
        int limit = Math.min(SpawnVisualEffectS2CPacket.MAX_TARGETS, targets.size());
        List<TargetSpot> list = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            LivingEntity t = targets.get(i);
            list.add(new TargetSpot(
                    (float) t.getX(),
                    (float) (t.getY() + t.getBbHeight() * 0.5D),
                    (float) t.getZ(),
                    t.getType().is(ModTags.HIGH_REALM_BOSS)
            ));
        }
        return list;
    }

    private static void broadcastVfx(ServerPlayer player, EffectType type, int tier, float radius, List<TargetSpot> spots) {
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(type, Mth.clamp(tier, 0, 3), player.position(), player.getLookAngle(), radius, spots),
                player.serverLevel(),
                player.position(),
                48.0D
        );
    }
}
