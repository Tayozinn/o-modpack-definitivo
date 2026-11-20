package com.mythiccontracts.network;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetworking {
    public static final String PROTOCOL_VERSION = "1";
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
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (player != null && message != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        if (message != null) {
            CHANNEL.sendToServer(message);
        }
    }
}
