package com.mythiccontracts.capability;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.core.persistence.PlayerDataSerializer;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.integration.ModAdvancements;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SyncCoreDataS2CPacket;
import com.mythiccontracts.network.SyncEnergyS2CPacket;
import com.mythiccontracts.subjugation.ActiveSubjugationSession;
import com.mythiccontracts.subjugation.SubjugationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

import java.util.Optional;

public final class ModCapabilities {
    public static final Capability<PlayerCoreData> PLAYER_CORE_DATA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation CORE_DATA_ID = new ResourceLocation(MythicContractsMod.MOD_ID, "player_core_data");
    public static final String PERSISTED_BACKUP_KEY = "mythic_contracts_core_backup";

    private ModCapabilities() {
    }

    public static Optional<PlayerCoreData> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return player.getCapability(PLAYER_CORE_DATA).resolve();
    }

    public static Optional<PlayerData> getData(Player player) {
        return get(player).map(PlayerCoreData::getPlayerData);
    }

    public static void savePersistedBackup(Player player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag forgePersisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag backupTag = new CompoundTag();
        data.saveNBTData(backupTag);
        forgePersisted.put(PERSISTED_BACKUP_KEY, backupTag);
        persistentData.put(Player.PERSISTED_NBT_TAG, forgePersisted);
    }

    public static boolean restoreFromPersistedBackup(Player sourcePlayer, PlayerCoreData targetData) {
        if (sourcePlayer == null || targetData == null) {
            return false;
        }
        CompoundTag persistentData = sourcePlayer.getPersistentData();
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag forgePersisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (!forgePersisted.contains(PERSISTED_BACKUP_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }
        CompoundTag backupTag = forgePersisted.getCompound(PERSISTED_BACKUP_KEY);
        if (backupTag.isEmpty()) {
            return false;
        }
        targetData.loadNBTData(backupTag);
        return true;
    }

    public static boolean restoreFromPersistedBackupIfNeeded(Player player, PlayerCoreData data) {
        if (player == null || data == null) {
            return false;
        }
        PlayerData playerData = data.getPlayerData();
        boolean isDefaultState = data.getFaction() == FactionType.NONE
                && data.getBodyRealm() == PlayerCoreData.MIN_REALM
                && data.getBodyStage() == PlayerCoreData.MIN_STAGE
                && data.getSpiritRealm() == PlayerCoreData.MIN_REALM
                && data.getSpiritStage() == PlayerCoreData.MIN_STAGE
                && data.getActiveContracts().isEmpty()
                && data.getSubjugatedEntities().isEmpty()
                && !data.isVowBroken()
                && playerData.getSkills().getUnlockedSkills().isEmpty()
                && playerData.getSkills().getEquippedSkills().isEmpty()
                && !playerData.getSkills().isDivineOverloaded()
                && playerData.getFaction().getReputations().isEmpty()
                && playerData.getContracts().getBrokenContractsHistory().isEmpty();
        if (!isDefaultState) {
            return false;
        }
        return restoreFromPersistedBackup(player, data);
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        get(player).ifPresent(data -> {
            savePersistedBackup(player, data);
            ModAdvancements.syncFromPlayerData(player, data.getPlayerData());
            CompoundTag syncTag = PlayerDataSerializer.writeClientSync(data.getPlayerData());
            ModNetworking.sendToPlayer(new SyncCoreDataS2CPacket(syncTag), player);
        });
    }

    public static void syncEnergy(ServerPlayer player) {
        if (player == null) {
            return;
        }
        getData(player).ifPresent(playerData -> {
            byte regenState = (byte) SpiritualIntegrityService.evaluateRegenState(playerData).ordinal();
            int activeSlot = playerData.getSkills().getActiveMartialSlot();
            ResourceLocation activeSkill = playerData.getSkills().getActiveMartialSkill();
            int activeMartialCd = activeSkill != null ? playerData.getSkills().getCooldown(activeSkill) : 0;
            ActiveSubjugationSession clash = SubjugationService.getActiveSession(player.getUUID());
            boolean clashActive = clash != null;
            float clashProgress = clash != null ? clash.getDominanceRatio() : 0.0F;
            boolean clashRevolt = clash != null && clash.isInRevoltWindow();
            String clashTarget = clash != null ? clash.getTargetName() : "";
            ModNetworking.sendToPlayer(
                    new SyncEnergyS2CPacket(
                            playerData.getCultivation().getCurrentQi(),
                            playerData.getCultivation().getMaxQiCapacity(),
                            playerData.getCultivation().isManaInfusionActive(),
                            playerData.getSkills().getUniqueSkillCooldownTicks(),
                            playerData.getSkills().getCurrentDivineResistance(),
                            playerData.getSkills().getMaxDivineResistance(),
                            playerData.getSkills().getDivineOverloadTicks(),
                            playerData.getCultivation().getCurrentSpiritualIntegrity(),
                            playerData.getCultivation().getMaxSpiritualIntegrity(),
                            playerData.getCultivation().getSpiritShatterTicks(),
                            playerData.getCultivation().getCurrentStamina(),
                            playerData.getCultivation().getMaxStamina(),
                            regenState,
                            activeSlot,
                            activeMartialCd,
                            clashActive,
                            clashProgress,
                            clashRevolt,
                            clashTarget,
                            playerData.getSkills().getActiveDivinePhase()
                    ),
                    player
            );
        });
    }
}
