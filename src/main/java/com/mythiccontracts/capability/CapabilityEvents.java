package com.mythiccontracts.capability;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.cultivation.UniqueSkillHandler;
import com.mythiccontracts.integration.CombatCompatService;
import com.mythiccontracts.network.ActivateCultivationSkillC2SPacket;
import com.mythiccontracts.network.ActivateUniqueSkillC2SPacket;
import com.mythiccontracts.network.CultivatorActionC2SPacket;
import com.mythiccontracts.network.CycleDivinePhaseC2SPacket;
import com.mythiccontracts.network.CycleMartialSlotC2SPacket;
import com.mythiccontracts.network.ToggleInfusionC2SPacket;
import com.mythiccontracts.subjugation.SubjugationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapabilityEvents {

    private CapabilityEvents() {
    }

    @Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerCoreData.class);
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            if (!player.getCapability(ModCapabilities.PLAYER_CORE_DATA).isPresent()) {
                CoreDataProvider provider = new CoreDataProvider();
                event.addCapability(ModCapabilities.CORE_DATA_ID, provider);
                event.addListener(provider::invalidate);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SubjugationService.clearPlayerSession(serverPlayer.getUUID());
            UniqueSkillHandler.clearPlayerState(serverPlayer.getUUID());
            ModCapabilities.get(serverPlayer).ifPresent(data -> {
                data.setManaInfusionActive(false);
                ModCapabilities.savePersistedBackup(serverPlayer, data);
                ModCapabilities.sync(serverPlayer);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();
        boolean wasDeath = event.isWasDeath();

        original.reviveCaps();
        try {
            ModCapabilities.get(clone).ifPresent(newData -> {
                Optional<PlayerCoreData> oldDataOpt = ModCapabilities.get(original);
                if (oldDataOpt.isPresent()) {
                    newData.copyFrom(oldDataOpt.get(), wasDeath);
                    ModCapabilities.restoreFromPersistedBackupIfNeeded(original, newData);
                } else {
                    boolean restored = ModCapabilities.restoreFromPersistedBackup(original, newData);
                    if (!restored) {
                        ModCapabilities.restoreFromPersistedBackup(clone, newData);
                    }
                }

                if (wasDeath) {
                    newData.setManaInfusionActive(false);
                }

                ModCapabilities.savePersistedBackup(clone, newData);
                if (clone instanceof ServerPlayer serverClone) {
                    float previousHealth = original.getHealth();
                    ContractManager.refreshPlayerContractAttributes(serverClone, newData);
                    if (!wasDeath && previousHealth > 0.0F) {
                        serverClone.setHealth(Math.min(previousHealth, serverClone.getMaxHealth()));
                    }
                }
            });
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncAndRefreshPlayer(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncAndRefreshPlayer(event.getEntity(), !event.isEndConquered());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncAndRefreshPlayer(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            ToggleInfusionC2SPacket.clearRateLimit(player.getUUID());
            CultivatorActionC2SPacket.clearRateLimit(player.getUUID());
            ActivateUniqueSkillC2SPacket.clearRateLimit(player.getUUID());
            ActivateCultivationSkillC2SPacket.clearRateLimit(player.getUUID());
            CycleMartialSlotC2SPacket.clearRateLimit(player.getUUID());
            CycleDivinePhaseC2SPacket.clearRateLimit(player.getUUID());
            CombatCompatService.clearPlayerState(player.getUUID());
            SubjugationService.clearPlayerSession(player.getUUID());
            UniqueSkillHandler.clearPlayerState(player.getUUID());
            ModCapabilities.get(player).ifPresent(data -> ModCapabilities.savePersistedBackup(player, data));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ToggleInfusionC2SPacket.clearAllRateLimits();
        CultivatorActionC2SPacket.clearAllRateLimits();
        ActivateUniqueSkillC2SPacket.clearAllRateLimits();
        ActivateCultivationSkillC2SPacket.clearAllRateLimits();
        CycleMartialSlotC2SPacket.clearAllRateLimits();
        CycleDivinePhaseC2SPacket.clearAllRateLimits();
        CombatCompatService.clearAllState();
        SubjugationService.clearAllSessions();
        UniqueSkillHandler.clearAllStates();
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            ModCapabilities.get(player).ifPresent(data -> ModCapabilities.savePersistedBackup(player, data));
        }
    }

    private static void syncAndRefreshPlayer(Player player, boolean isDeathRespawn) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModCapabilities.get(serverPlayer).ifPresent(data -> {
                ModCapabilities.restoreFromPersistedBackupIfNeeded(serverPlayer, data);
                if (isDeathRespawn) {
                    data.setManaInfusionActive(false);
                }
                ContractManager.refreshPlayerContractAttributes(serverPlayer, data, isDeathRespawn);
                ModCapabilities.sync(serverPlayer);
            });
        }
    }
}
