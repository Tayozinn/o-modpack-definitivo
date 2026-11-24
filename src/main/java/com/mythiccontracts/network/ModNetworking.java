package com.mythiccontracts.network;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetworking {
    public static final String PROTOCOL_VERSION = "5";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MythicContractsMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered = false;
    private static int packetId = 0;

    private ModNetworking() {
    }

    private static int nextId() {
        return packetId++;
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.registerMessage(
                nextId(),
                SyncCoreDataS2CPacket.class,
                SyncCoreDataS2CPacket::toBytes,
                SyncCoreDataS2CPacket::new,
                SyncCoreDataS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextId(),
                ToggleInfusionC2SPacket.class,
                ToggleInfusionC2SPacket::toBytes,
                ToggleInfusionC2SPacket::new,
                ToggleInfusionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                nextId(),
                ActivateUniqueSkillC2SPacket.class,
                ActivateUniqueSkillC2SPacket::toBytes,
                ActivateUniqueSkillC2SPacket::new,
                ActivateUniqueSkillC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                nextId(),
                SyncEnergyS2CPacket.class,
                SyncEnergyS2CPacket::toBytes,
                SyncEnergyS2CPacket::new,
                SyncEnergyS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                nextId(),
                CultivatorActionC2SPacket.class,
                CultivatorActionC2SPacket::toBytes,
                CultivatorActionC2SPacket::new,
                CultivatorActionC2SPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
                nextId(),
                SpawnVisualEffectS2CPacket.class,
                SpawnVisualEffectS2CPacket::toBytes,
                SpawnVisualEffectS2CPacket::new,
                SpawnVisualEffectS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player != null && message != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static <MSG> void sendToNear(MSG message, ServerLevel level, Vec3 pos, double radius) {
        if (message != null && level != null && pos != null) {
            CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                            pos.x, pos.y, pos.z, radius, level.dimension()
                    )),
                    message
            );
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        if (message != null) {
            CHANNEL.sendToServer(message);
        }
    }
}
