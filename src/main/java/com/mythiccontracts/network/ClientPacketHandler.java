package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleSyncCoreData(CompoundTag nbt) {
        if (nbt == null) {
            return;
        }
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            ModCapabilities.get(localPlayer).ifPresent(data -> data.loadNBTData(nbt));
        }
    }

    @Nullable
    public static PlayerCoreData getClientPlayerCoreData() {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return null;
        }
        return ModCapabilities.get(localPlayer).orElse(null);
    }
}
