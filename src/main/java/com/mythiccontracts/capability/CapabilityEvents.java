package com.mythiccontracts.capability;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.contract.ContractManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
            ModCapabilities.get(player).ifPresent(data -> ModCapabilities.savePersistedBackup(player, data));
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            ModCapabilities.get(player).ifPresent(data -> ModCapabilities.savePersistedBackup(player, data));
        }
    }

    private static void syncAndRefreshPlayer(Player player, boolean forceDisableInfusion) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModCapabilities.get(serverPlayer).ifPresent(data -> {
                ModCapabilities.restoreFromPersistedBackupIfNeeded(serverPlayer, data);
                if (forceDisableInfusion) {
                    data.setManaInfusionActive(false);
                }
                ContractManager.refreshPlayerContractAttributes(serverPlayer, data);
                ModCapabilities.sync(serverPlayer);
            });
        }
    }
}
