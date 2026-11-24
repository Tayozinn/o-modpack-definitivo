package com.mythiccontracts.subjugation;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.player.SubjugatedEntityRecord;
import com.mythiccontracts.core.player.SubjugatedEntityRecord.ServantStance;
import com.mythiccontracts.core.player.SubjugationData;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.event.SubjugationEvents;
import com.mythiccontracts.integration.ModAdvancements;
import com.mythiccontracts.item.SubjugationSealItem;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.TargetSpot;
import com.mythiccontracts.registry.ModItems;
import com.mythiccontracts.registry.ModSounds;
import com.mythiccontracts.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço autoritativo da Subjugação como Habilidade (`mythic_contracts:soul_subjugation`),
 * gerenciando Disputa de Poder Espiritual, Pulso de Revolta, invocação/recolhimento com snapshot
 * e o papel do {@code SubjugationSealItem} como Catalisador Opcional.
 */
public final class SubjugationService {

    public static final ResourceLocation SOUL_SUBJUGATION_ID =
            new ResourceLocation(MythicContractsMod.MOD_ID, "soul_subjugation");
    public static final String TAG_SERVANT_INSTANCE_ID = "mc_servant_id";
    public static final String TAG_SERVANT_STANCE = "mc_subjugated_stance";
    public static final String TAG_SERVANT_GRADE = "mc_subjugated_grade";
    public static final String TAG_GUARD_X = "mc_guard_x";
    public static final String TAG_GUARD_Y = "mc_guard_y";
    public static final String TAG_GUARD_Z = "mc_guard_z";

    private static final UUID GRADE_3_HP_BONUS_UUID = UUID.fromString("47a2f18b-1d24-4f88-90ce-3c2a11d8a101");
    private static final UUID GRADE_3_ATK_BONUS_UUID = UUID.fromString("47a2f18b-1d24-4f88-90ce-3c2a11d8a102");
    private static final int REVOLT_VFX_INTERVAL = 5;

    private static final Map<UUID, ActiveSubjugationSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TARGET_IMMUNITY_UNTIL_TICK = new ConcurrentHashMap<>();

    private SubjugationService() {
    }

    @Nullable
    public static ActiveSubjugationSession getActiveSession(@Nullable UUID playerUuid) {
        return playerUuid != null ? ACTIVE_SESSIONS.get(playerUuid) : null;
    }

    public static boolean hasActiveSession(@Nullable UUID playerUuid) {
        return playerUuid != null && ACTIVE_SESSIONS.containsKey(playerUuid);
    }

    public static void clearPlayerSession(@Nullable UUID playerUuid) {
        if (playerUuid != null) {
            ACTIVE_SESSIONS.remove(playerUuid);
        }
    }

    public static void clearAllSessions() {
        ACTIVE_SESSIONS.clear();
        TARGET_IMMUNITY_UNTIL_TICK.clear();
    }

    public static boolean tryActivateSkill(ServerPlayer player, PlayerCoreData coreData) {
        if (player == null || coreData == null || !player.isAlive() || player.isDeadOrDying()) {
            return false;
        }
        ActiveSubjugationSession existing = ACTIVE_SESSIONS.get(player.getUUID());
        if (existing != null) {
            if (existing.isInRevoltWindow()) {
                existing.trySuppressRevolt(false);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SUBJUGATION_CLASH_SUPPRESS.get(), SoundSource.PLAYERS, 0.85F, 1.05F);
                player.displayClientMessage(
                        Component.literal("✦ [Domínio Espiritual] Pulso de Revolta suprimido! (+25% Dominância)")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        true
                );
                broadcastClashVfx(player, EffectType.SUBJUGATION_CLASH_TICK, existing);
                ModCapabilities.syncEnergy(player);
                return true;
            }
            player.displayClientMessage(
                    Component.literal("⛓ Disputa Espiritual em andamento (" + Math.round(existing.getDominance()) + "%)...")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true
            );
            return false;
        }

        PlayerData playerData = coreData.getPlayerData();
        SkillData skills = playerData.getSkills();
        int cooldown = skills.getCooldown(SOUL_SUBJUGATION_ID);
        if (cooldown > 0) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "⏳ Subjugação em recarga: %.1fs", cooldown / 20.0F))
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        int grade = SubjugationBalance.calculateMasteryGrade(playerData);
        skills.setSubjugationMasteryTier(grade);
        double reach = SubjugationBalance.calculateEffectiveReach(grade);
        LivingEntity target = findTargetedLivingEntity(player, reach);

        if (target != null) {
            UUID ownerUuid = SubjugationSealItem.getOwnerUUID(target);
            if (ownerUuid != null) {
                if (player.getUUID().equals(ownerUuid)) {
                    return recallWorldServant(player, coreData, target);
                }
                player.displayClientMessage(
                        Component.literal("✖ Esta criatura já responde a outro mestre.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }
            if (target.getType().is(ModTags.SUBJUGATABLE)) {
                boolean hasCatalyst = hasCatalystAvailable(player, target);
                return tryStartClashWithTarget(player, coreData, target, hasCatalyst);
            }
        }

        // Sem alvo subjugável na mira: tenta invocar o último servo armazenado
        return summonLastStoredServant(player, coreData);
    }

    public static boolean tryStartClashWithTarget(
            ServerPlayer player,
            PlayerCoreData coreData,
            LivingEntity target,
            boolean useCatalystIfPresent
    ) {
        if (player == null || coreData == null || target == null || !target.isAlive()) {
            return false;
        }
        if (ACTIVE_SESSIONS.containsKey(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("✖ Você já está em uma Disputa de Subjugação!")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }
        PlayerData pData = coreData.getPlayerData();
        CultivationData cult = pData.getCultivation();
        if (cult.isSpiritShattered()) {
            player.displayClientMessage(
                    Component.literal("✖ [Colapso Espiritual] Impossível iniciar Subjugação com a alma fraturada!")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return false;
        }
        if (!target.getType().is(ModTags.SUBJUGATABLE)) {
            player.displayClientMessage(
                    Component.literal("✖ Esta entidade não pode ser subjugada.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        if (SubjugationSealItem.isSubjugated(target)) {
            player.displayClientMessage(
                    Component.literal("✖ Esta entidade já possui um mestre.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        long now = player.serverLevel().getGameTime();
        Long immuneUntil = TARGET_IMMUNITY_UNTIL_TICK.get(target.getUUID());
        if (immuneUntil != null && now < immuneUntil) {
            player.displayClientMessage(
                    Component.literal("✖ O alvo resistiu recentemente e está protegido do vínculo espiritual!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        int maxBound = SubjugationBalance.calculateMaxBoundCapacity(pData);
        if (pData.getSubjugation().getBoundEntities().size() >= maxBound) {
            player.displayClientMessage(
                    Component.literal("✖ Limite de servos atingido (" + pData.getSubjugation().getBoundEntities().size()
                                    + "/" + maxBound + "). Liberte um servo na tela [U] → Contratos.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        SubjugationTier tier = SubjugationBalance.classifyByAttributes(
                entityId,
                Math.max(1.0F, target.getMaxHealth()),
                target.getType().is(ModTags.HIGH_REALM_BOSS)
        );
        int spiritRealm = CultivationService.getCurrentRealm(pData, CultivationPath.SPIRIT).order();
        if (spiritRealm < tier.getMinSpiritRealm()) {
            player.displayClientMessage(
                    Component.literal("✖ Alvo de categoria " + tier.getDisplayName()
                                    + " exige Cultivo Espiritual R" + tier.getMinSpiritRealm() + "+.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        boolean catalystVerified = useCatalystIfPresent
                && (player.getAbilities().instabuild || findCatalystSlot(player.getInventory()) >= 0);

        float hpThreshold = SubjugationBalance.calculateEffectiveHpThreshold(pData, tier, catalystVerified);
        float hpRatio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
        if (hpRatio > hpThreshold) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT,
                            "✖ Alvo resistiu! Vida atual: %.1f%% (Necessário: <= %.0f%%)",
                            hpRatio * 100.0F, hpThreshold * 100.0F))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        float costScale = SubjugationBalance.calculateResourceCostScale(catalystVerified);
        float qiCost = tier.getQiCost() * costScale;
        float stamCost = tier.getStaminaCost() * costScale;
        float siCost = tier.getSpiritualIntegrityCost() * costScale;

        if (cult.getCurrentQi() < qiCost) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "✖ Qi insuficiente (Requer %.0f Qi).", qiCost))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        if (cult.getCurrentStamina() < stamCost) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT, "✖ Vigor insuficiente (Requer %.0f Vigor).", stamCost))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        if (siCost > 0.0F && cult.getCurrentSpiritualIntegrity() <= siCost && !catalystVerified) {
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT,
                            "✖ Integridade Espiritual baixa demais (Requer > %.0f para evitar Colapso imediato).", siCost))
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        cult.setCurrentQi(cult.getCurrentQi() - qiCost);
        if (stamCost > 0.0F) {
            VitalService.consumeStamina(pData, stamCost, 20);
        }
        if (siCost > 0.0F) {
            float floor = catalystVerified ? 5.0F : 1.0F;
            cult.setCurrentSpiritualIntegrity(Math.max(floor, cult.getCurrentSpiritualIntegrity() - siCost));
        }

        float soulPower = SubjugationBalance.calculatePlayerSoulPower(pData, cult.isManaInfusionActive());
        float chance = SubjugationBalance.calculateCaptureSuccessChance(pData, tier, catalystVerified);
        String targetName = target.getType().getDescription().getString();

        ActiveSubjugationSession session = new ActiveSubjugationSession(
                player.getUUID(),
                target.getId(),
                entityId,
                targetName,
                tier,
                catalystVerified,
                soulPower,
                chance
        );
        ACTIVE_SESSIONS.put(player.getUUID(), session);

        if (catalystVerified) {
            player.displayClientMessage(
                    Component.literal("✦ [Catalisador Ativo] Disputa Espiritual iniciada em 65% contra " + targetName + "!")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("⛓ [Disputa Espiritual] Conectando alma a " + targetName + "...")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    true
            );
        }

        broadcastClashVfx(player, EffectType.SUBJUGATION_CLASH_START, session, target);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SUBJUGATION_CLASH_START.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
        ModCapabilities.syncEnergy(player);
        return true;
    }

    public static void tickPlayerSession(ServerPlayer player, PlayerCoreData coreData) {
        if (player == null || coreData == null) {
            return;
        }
        ActiveSubjugationSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Entity rawTarget = level.getEntity(session.getTargetEntityId());
        if (!(rawTarget instanceof LivingEntity target) || !target.isAlive() || SubjugationSealItem.isSubjugated(target)) {
            abortSession(player, coreData, session, null, "Alvo desapareceu ou foi abatido!");
            return;
        }
        if (target.distanceToSqr(player) > SubjugationBalance.CLASH_BREAK_DISTANCE * SubjugationBalance.CLASH_BREAK_DISTANCE) {
            abortSession(player, coreData, session, target, "Vínculo rompido pela distância (> 14m)!");
            return;
        }

        PlayerData pData = coreData.getPlayerData();
        CultivationData cult = pData.getCultivation();
        if (cult.isSpiritShattered() && !session.isCatalystUsed()) {
            abortSession(player, coreData, session, target, "Colapso Espiritual interrompeu a Disputa!");
            return;
        }

        boolean infusion = cult.isManaInfusionActive();
        ActiveSubjugationSession.TickStatus status = session.tick(infusion);

        if (status == ActiveSubjugationSession.TickStatus.REVOLT_TRIGGERED) {
            if (infusion && cult.getCurrentQi() >= 10.0F) {
                cult.setCurrentQi(cult.getCurrentQi() - 10.0F);
                session.trySuppressRevolt(true);
                player.displayClientMessage(
                        Component.literal("✦ [Infusão] Pulso de Revolta repelido automaticamente!")
                                .withStyle(ChatFormatting.AQUA),
                        true
                );
            } else {
                float revoltDrain = 8.0F + session.getTier().ordinal() * 4.0F;
                if (session.isCatalystUsed()) {
                    cult.setCurrentSpiritualIntegrity(Math.max(1.0F, cult.getCurrentSpiritualIntegrity() - revoltDrain * 0.5F));
                } else {
                    cult.setCurrentSpiritualIntegrity(Math.max(0.0F, cult.getCurrentSpiritualIntegrity() - revoltDrain));
                    if (cult.getCurrentSpiritualIntegrity() <= 0.0F) {
                        cult.setSpiritShatterTicks(200);
                        abortSession(player, coreData, session, target, "O Pulso de Revolta estilhaçou sua Integridade Espiritual!");
                        return;
                    }
                }
                player.displayClientMessage(
                        Component.literal("⚠ [PULSO DE REVOLTA] Pressione ALT+G agora para dominar!")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                        true
                );
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.SUBJUGATION_CLASH_REVOLT.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
            }
        }

        if (status == ActiveSubjugationSession.TickStatus.SUCCEEDED) {
            completeClashSuccess(player, coreData, session, target);
            return;
        }
        if (status == ActiveSubjugationSession.TickStatus.TIMEOUT_ROLL) {
            float prob = session.computeFinalCaptureProbability();
            if (player.getRandom().nextFloat() <= prob) {
                completeClashSuccess(player, coreData, session, target);
            } else {
                abortSession(player, coreData, session, target,
                        "A criatura resistiu ao selo (" + Math.round(prob * 100.0F) + "% dominância)!");
            }
            return;
        }
        if (session.getDominance() <= 0.0F) {
            abortSession(player, coreData, session, target, "Sua força espiritual cedeu durante a disputa!");
            return;
        }

        if (session.getElapsedTicks() % REVOLT_VFX_INTERVAL == 0) {
            broadcastClashVfx(player, EffectType.SUBJUGATION_CLASH_TICK, session, target);
        }
        if (session.getElapsedTicks() % 2 == 0) {
            ModCapabilities.syncEnergy(player);
        }
    }

    private static void completeClashSuccess(
            ServerPlayer player,
            PlayerCoreData coreData,
            ActiveSubjugationSession session,
            LivingEntity target
    ) {
        ACTIVE_SESSIONS.remove(player.getUUID());
        if (session.isCatalystUsed() && !player.getAbilities().instabuild) {
            int slot = findCatalystSlot(player.getInventory());
            if (slot >= 0) {
                player.getInventory().getItem(slot).shrink(1);
            }
        }

        captureTargetRecord(player, coreData, target, session.getTier());
        coreData.getPlayerData().getSkills().setCooldown(SOUL_SUBJUGATION_ID, SubjugationBalance.SUCCESS_COOLDOWN_TICKS);
        broadcastClashVfx(player, EffectType.SUBJUGATION_SUCCESS, session, target);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SUBJUGATION_CLASH_SUCCESS.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
        ModCapabilities.sync(player);
        ModCapabilities.syncEnergy(player);
    }

    private static void abortSession(
            ServerPlayer player,
            PlayerCoreData coreData,
            ActiveSubjugationSession session,
            @Nullable LivingEntity target,
            String reason
    ) {
        ACTIVE_SESSIONS.remove(player.getUUID());
        if (session.isCatalystUsed() && !player.getAbilities().instabuild) {
            int slot = findCatalystSlot(player.getInventory());
            if (slot >= 0) {
                player.getInventory().getItem(slot).shrink(1);
            }
        }
        coreData.getPlayerData().getSkills().setCooldown(SOUL_SUBJUGATION_ID, session.getTier().getFailureCooldownTicks());
        if (target != null) {
            TARGET_IMMUNITY_UNTIL_TICK.put(target.getUUID(), player.serverLevel().getGameTime() + 200L);
        }
        broadcastClashVfx(player, EffectType.SUBJUGATION_FAIL, session, target);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SUBJUGATION_CLASH_FAIL.get(), SoundSource.PLAYERS, 0.90F, 1.0F);
        player.displayClientMessage(
                Component.literal("✖ [Subjugação Falhou] " + reason).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                true
        );
        ModCapabilities.syncEnergy(player);
    }

    public static boolean tryLegacyRelicCapture(
            ServerPlayer player,
            PlayerCoreData coreData,
            LivingEntity target,
            ItemStack sealStack
    ) {
        if (player == null || coreData == null || target == null || !target.isAlive()) {
            return false;
        }
        PlayerData pData = coreData.getPlayerData();
        int maxBound = SubjugationBalance.calculateMaxBoundCapacity(pData);
        if (pData.getSubjugation().getBoundEntities().size() >= maxBound) {
            player.displayClientMessage(
                    Component.literal("✖ Limite de servos atingido (" + maxBound + ").").withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        SubjugationTier tier = SubjugationBalance.classifyByAttributes(id, target.getMaxHealth(), target.getType().is(ModTags.HIGH_REALM_BOSS));
        captureTargetRecord(player, coreData, target, tier);
        if (!player.getAbilities().instabuild && sealStack != null && !sealStack.isEmpty()) {
            sealStack.shrink(1);
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SUBJUGATION_CLASH_SUCCESS.get(), SoundSource.PLAYERS, 0.85F, 0.92F);
        ModCapabilities.sync(player);
        return true;
    }

    private static void captureTargetRecord(
            ServerPlayer player,
            PlayerCoreData coreData,
            LivingEntity target,
            SubjugationTier tier
    ) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (entityId == null) {
            return;
        }
        CompoundTag snapshot = new CompoundTag();
        target.saveWithoutId(snapshot);
        sanitizeSnapshotForStorage(snapshot);

        String targetName = target.hasCustomName() && target.getCustomName() != null
                ? target.getCustomName().getString()
                : target.getType().getDescription().getString();
        int initLevel = 1 + tier.ordinal();

        SubjugatedEntityRecord record = new SubjugatedEntityRecord(
                UUID.randomUUID(),
                entityId,
                initLevel,
                0,
                targetName,
                snapshot,
                ServantStance.FOLLOW,
                false
        );
        coreData.getPlayerData().getSubjugation().addRecord(record);
        ModAdvancements.grant(player, ModAdvancements.FIRST_SUBJUGATION);

        target.discard();
        player.sendSystemMessage(
                Component.literal("§d[Subjugação] §e" + targetName + " §a(Nv." + initLevel + ") foi vinculado à sua alma! Abra §f[U] → Contratos §aou use §fALT+G §apara invocar.")
        );
    }

    public static boolean summonLastStoredServant(ServerPlayer player, PlayerCoreData coreData) {
        SubjugationData sub = coreData.getPlayerData().getSubjugation();
        for (int i = sub.getBoundEntities().size() - 1; i >= 0; i--) {
            SubjugatedEntityRecord rec = sub.getBoundEntities().get(i);
            if (!rec.summoned()) {
                return summonServantById(player, coreData, rec.instanceId());
            }
        }
        player.displayClientMessage(
                Component.literal("✖ Nenhum servo disponível recolhido (ou todos já estão em campo).")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return false;
    }

    public static boolean summonServantById(ServerPlayer player, PlayerCoreData coreData, UUID instanceId) {
        if (player == null || coreData == null || instanceId == null) {
            return false;
        }
        PlayerData pData = coreData.getPlayerData();
        SubjugationData sub = pData.getSubjugation();
        SubjugatedEntityRecord record = sub.findByInstanceId(instanceId);
        if (record == null) {
            return false;
        }
        if (record.summoned()) {
            return recallServantById(player, coreData, instanceId);
        }

        int maxActive = SubjugationBalance.calculateMaxActiveSummons(pData);
        if (sub.countSummoned() >= maxActive) {
            player.displayClientMessage(
                    Component.literal("✖ Limite de servos ativos em campo atingido (" + maxActive + "). Recolha um servo antes.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(record.entityType());
        if (type == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Entity created = type.create(level);
        if (created == null) {
            return false;
        }

        if (record.entitySnapshot() != null) {
            CompoundTag clean = record.entitySnapshot().copy();
            sanitizeSnapshotForStorage(clean);
            try {
                created.load(clean);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.warn("[{}] Falha parcial ao restaurar snapshot do servo {}: {}",
                        MythicContractsMod.MOD_ID, record.entityType(), ex.getMessage());
            }
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        horizontal = horizontal.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
        double sx = player.getX() + horizontal.x * 2.3D;
        double sy = player.getY();
        double sz = player.getZ() + horizontal.z * 2.3D;
        created.moveTo(sx, sy, sz, player.getYRot(), 0.0F);

        int grade = SubjugationBalance.calculateMasteryGrade(pData);
        CompoundTag pTag = created.getPersistentData();
        pTag.putUUID(SubjugationSealItem.TAG_SUBJUGATED_OWNER, player.getUUID());
        pTag.putUUID(TAG_SERVANT_INSTANCE_ID, record.instanceId());
        pTag.putString(TAG_SERVANT_STANCE, record.stance().name());
        pTag.putInt(TAG_SERVANT_GRADE, grade);
        pTag.putDouble(TAG_GUARD_X, sx);
        pTag.putDouble(TAG_GUARD_Y, sy);
        pTag.putDouble(TAG_GUARD_Z, sz);
        created.addTag(SubjugationSealItem.ENTITY_TAG_SUBJUGATED);

        String disp = record.customName() != null && !record.customName().isBlank()
                ? record.customName()
                : created.getType().getDescription().getString();
        created.setCustomName(Component.literal("§d[Servo Nv." + record.level() + "] §f" + disp));
        created.setCustomNameVisible(true);

        if (created instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.setTarget(null);
            if (grade >= 3) {
                applyGrade3Modifiers(mob);
            }
            mob.setHealth(Math.max(mob.getMaxHealth() * 0.55F, Math.min(mob.getMaxHealth(), mob.getHealth())));
            SubjugationEvents.injectSubjugatedAI(mob);
        }

        level.addFreshEntity(created);
        sub.replaceRecord(record.withSummoned(true));
        pData.getSkills().setCooldown(SOUL_SUBJUGATION_ID, Math.max(
                pData.getSkills().getCooldown(SOUL_SUBJUGATION_ID),
                SubjugationBalance.SUMMON_COOLDOWN_TICKS
        ));

        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        EffectType.SUBJUGATION_SUMMON,
                        Math.max(0, grade - 1),
                        new Vec3(sx, sy + 0.8D, sz),
                        look,
                        2.5F,
                        List.of()
                ),
                level,
                new Vec3(sx, sy, sz),
                32.0D
        );
        level.playSound(null, sx, sy, sz, ModSounds.SUBJUGATION_SERVANT_SUMMON.get(), SoundSource.PLAYERS, 0.85F, 1.05F);
        ModCapabilities.sync(player);
        player.displayClientMessage(
                Component.literal("✦ [Servo Invocado] " + disp + " (" + record.stance().getDisplayName() + ")")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true
        );
        return true;
    }

    public static boolean recallServantById(ServerPlayer player, PlayerCoreData coreData, UUID instanceId) {
        if (player == null || coreData == null || instanceId == null) {
            return false;
        }
        SubjugationData sub = coreData.getPlayerData().getSubjugation();
        SubjugatedEntityRecord record = sub.findByInstanceId(instanceId);
        if (record == null) {
            return false;
        }

        LivingEntity inWorld = findWorldServantByInstanceId(player.serverLevel(), player.getUUID(), instanceId);
        CompoundTag updatedSnapshot = record.entitySnapshot();
        if (inWorld != null) {
            CompoundTag snap = new CompoundTag();
            inWorld.saveWithoutId(snap);
            sanitizeSnapshotForStorage(snap);
            updatedSnapshot = snap;
            inWorld.discard();
        }
        sub.replaceRecord(record.withSnapshot(updatedSnapshot).withSummoned(false));
        ModCapabilities.sync(player);
        player.displayClientMessage(
                Component.literal("✦ Servo recolhido ao vínculo espiritual.").withStyle(ChatFormatting.AQUA),
                true
        );
        return true;
    }

    private static boolean recallWorldServant(ServerPlayer player, PlayerCoreData coreData, LivingEntity target) {
        CompoundTag pTag = target.getPersistentData();
        if (pTag.hasUUID(TAG_SERVANT_INSTANCE_ID)) {
            return recallServantById(player, coreData, pTag.getUUID(TAG_SERVANT_INSTANCE_ID));
        }
        // Compatibilidade com servos legados invocados antes da v2.1
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (typeId != null) {
            int maxBound = SubjugationBalance.calculateMaxBoundCapacity(coreData.getPlayerData());
            if (coreData.getPlayerData().getSubjugation().getBoundEntities().size() < maxBound) {
                captureTargetRecord(player, coreData, target, SubjugationTier.COMMON);
                ModCapabilities.sync(player);
                return true;
            }
        }
        return false;
    }

    public static boolean cycleServantStance(ServerPlayer player, PlayerCoreData coreData, UUID instanceId) {
        if (player == null || coreData == null || instanceId == null) {
            return false;
        }
        PlayerData pData = coreData.getPlayerData();
        int grade = SubjugationBalance.calculateMasteryGrade(pData);
        if (grade < 2) {
            player.displayClientMessage(
                    Component.literal("✖ Alterar postura do servo exige Subjugação Grau II (Espírito R3+).")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }
        SubjugationData sub = pData.getSubjugation();
        SubjugatedEntityRecord record = sub.findByInstanceId(instanceId);
        if (record == null) {
            return false;
        }
        ServantStance nextStance = record.stance().next();
        sub.replaceRecord(record.withStance(nextStance));

        LivingEntity inWorld = findWorldServantByInstanceId(player.serverLevel(), player.getUUID(), instanceId);
        if (inWorld != null) {
            inWorld.getPersistentData().putString(TAG_SERVANT_STANCE, nextStance.name());
            if (nextStance == ServantStance.GUARD) {
                inWorld.getPersistentData().putDouble(TAG_GUARD_X, inWorld.getX());
                inWorld.getPersistentData().putDouble(TAG_GUARD_Y, inWorld.getY());
                inWorld.getPersistentData().putDouble(TAG_GUARD_Z, inWorld.getZ());
            }
        }

        ModCapabilities.sync(player);
        player.displayClientMessage(
                Component.literal("✦ Postura do servo alterada para: " + nextStance.getDisplayName())
                        .withStyle(ChatFormatting.AQUA),
                true
        );
        return true;
    }

    public static boolean releaseServantById(ServerPlayer player, PlayerCoreData coreData, UUID instanceId) {
        if (player == null || coreData == null || instanceId == null) {
            return false;
        }
        LivingEntity inWorld = findWorldServantByInstanceId(player.serverLevel(), player.getUUID(), instanceId);
        if (inWorld != null) {
            inWorld.discard();
        }
        boolean removed = coreData.getPlayerData().getSubjugation().removeByInstanceId(instanceId);
        if (removed) {
            ModCapabilities.sync(player);
            player.displayClientMessage(
                    Component.literal("✦ Vínculo espiritual do servo liberado.").withStyle(ChatFormatting.GRAY),
                    true
            );
        }
        return removed;
    }

    @Nullable
    public static LivingEntity findWorldServantByInstanceId(ServerLevel level, UUID ownerUuid, UUID instanceId) {
        if (level == null || ownerUuid == null || instanceId == null) {
            return null;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && entity.isAlive()) {
                CompoundTag pTag = entity.getPersistentData();
                if (pTag.hasUUID(SubjugationSealItem.TAG_SUBJUGATED_OWNER)
                        && ownerUuid.equals(pTag.getUUID(SubjugationSealItem.TAG_SUBJUGATED_OWNER))
                        && pTag.hasUUID(TAG_SERVANT_INSTANCE_ID)
                        && instanceId.equals(pTag.getUUID(TAG_SERVANT_INSTANCE_ID))) {
                    return living;
                }
            }
        }
        return null;
    }

    public static void onWorldServantDied(ServerLevel level, LivingEntity mob) {
        UUID ownerUuid = SubjugationSealItem.getOwnerUUID(mob);
        CompoundTag pTag = mob.getPersistentData();
        if (ownerUuid == null || !pTag.hasUUID(TAG_SERVANT_INSTANCE_ID)) {
            return;
        }
        UUID instanceId = pTag.getUUID(TAG_SERVANT_INSTANCE_ID);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            ModCapabilities.get(owner).ifPresent(coreData -> {
                SubjugationData sub = coreData.getPlayerData().getSubjugation();
                SubjugatedEntityRecord rec = sub.findByInstanceId(instanceId);
                if (rec != null) {
                    sub.replaceRecord(rec.withSummoned(false));
                    ModCapabilities.sync(owner);
                    owner.displayClientMessage(
                            Component.literal("⚠ Seu servo foi derrotado e retornou ao vínculo para regenerar.")
                                    .withStyle(ChatFormatting.YELLOW),
                            true
                    );
                }
            });
        }
    }

    private static void applyGrade3Modifiers(Mob mob) {
        AttributeInstance maxHp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && maxHp.getModifier(GRADE_3_HP_BONUS_UUID) == null) {
            maxHp.addPermanentModifier(new AttributeModifier(
                    GRADE_3_HP_BONUS_UUID, "mythic_contracts.servant_grade3_hp", 0.15D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null && atk.getModifier(GRADE_3_ATK_BONUS_UUID) == null) {
            atk.addPermanentModifier(new AttributeModifier(
                    GRADE_3_ATK_BONUS_UUID, "mythic_contracts.servant_grade3_atk", 0.15D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void sanitizeSnapshotForStorage(CompoundTag snapshot) {
        snapshot.remove("UUID");
        snapshot.remove("Pos");
        snapshot.remove("Motion");
        snapshot.remove("Rotation");
        snapshot.remove("Dimension");
        snapshot.remove("PortalCooldown");
        snapshot.remove("Passengers");
        snapshot.remove("Leash");
    }

    private static boolean hasCatalystAvailable(ServerPlayer player, LivingEntity target) {
        boolean held = player.getMainHandItem().is(ModItems.SUBJUGATION_SEAL.get())
                || player.getOffhandItem().is(ModItems.SUBJUGATION_SEAL.get());
        if (held) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        SubjugationTier tier = SubjugationBalance.classifyByAttributes(id, target.getMaxHealth(), target.getType().is(ModTags.HIGH_REALM_BOSS));
        return (tier == SubjugationTier.MINI_BOSS || tier == SubjugationTier.BOSS)
                && findCatalystSlot(player.getInventory()) >= 0;
    }

    private static int findCatalystSlot(Inventory inv) {
        if (inv == null) {
            return -1;
        }
        ItemStack offhand = inv.offhand.isEmpty() ? ItemStack.EMPTY : inv.offhand.get(0);
        if (!offhand.isEmpty() && offhand.is(ModItems.SUBJUGATION_SEAL.get())) {
            return 40;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.SUBJUGATION_SEAL.get())) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public static LivingEntity findTargetedLivingEntity(ServerPlayer player, double maxDistance) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(maxDistance));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.5D);

        LivingEntity best = null;
        double bestDistSq = maxDistance * maxDistance;
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator()
        )) {
            AABB box = candidate.getBoundingBox().inflate(0.45D);
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double distSq = eye.distanceToSqr(hit.get());
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static void broadcastClashVfx(ServerPlayer player, EffectType type, ActiveSubjugationSession session) {
        LivingEntity target = null;
        Entity e = player.serverLevel().getEntity(session.getTargetEntityId());
        if (e instanceof LivingEntity le) {
            target = le;
        }
        broadcastClashVfx(player, type, session, target);
    }

    private static void broadcastClashVfx(
            ServerPlayer player,
            EffectType type,
            ActiveSubjugationSession session,
            @Nullable LivingEntity target
    ) {
        List<TargetSpot> spots = target != null
                ? List.of(new TargetSpot(
                        (float) target.getX(),
                        (float) (target.getY() + target.getBbHeight() * 0.5D),
                        (float) target.getZ(),
                        session.getTier() == SubjugationTier.BOSS || session.isInRevoltWindow()
                ))
                : List.of();
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        type,
                        Math.min(3, session.getTier().ordinal()),
                        player.position(),
                        player.getLookAngle(),
                        session.getDominanceRatio(),
                        spots
                ),
                player.serverLevel(),
                player.position(),
                48.0D
        );
    }
}
