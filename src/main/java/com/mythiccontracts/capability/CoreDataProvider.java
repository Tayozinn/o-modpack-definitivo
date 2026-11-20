package com.mythiccontracts.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CoreDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private PlayerCoreData coreData = null;
    private LazyOptional<PlayerCoreData> optional = LazyOptional.of(this::createCoreData);

    private PlayerCoreData createCoreData() {
        if (this.coreData == null) {
            this.coreData = new PlayerCoreData();
        }
        return this.coreData;
    }

    public PlayerCoreData getOrCreateData() {
        return createCoreData();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.PLAYER_CORE_DATA) {
            if (!this.optional.isPresent()) {
                this.optional = LazyOptional.of(this::createCoreData);
            }
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    public void invalidate() {
        this.optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createCoreData().saveNBTData(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createCoreData().loadNBTData(nbt);
    }
}
