package com.mythiccontracts.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncCoreDataS2CPacket {
    private final CompoundTag nbt;

    public SyncCoreDataS2CPacket(CompoundTag nbt) {
        this.nbt = nbt != null ? nbt : new CompoundTag();
    }

    public SyncCoreDataS2CPacket(FriendlyByteBuf buf) {
        CompoundTag read = buf.readNbt();
        this.nbt = read != null ? read : new CompoundTag();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

    public CompoundTag getNbt() {
        return this.nbt;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSyncCoreData(this.nbt)
        ));
        ctx.setPacketHandled(true);
        return true;
    }
}
