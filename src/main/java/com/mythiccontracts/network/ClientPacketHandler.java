package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.client.ClientRuntimeState;
import com.mythiccontracts.client.fx.ClientVfxScheduler;
import com.mythiccontracts.core.player.PlayerData;
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

    public static void handleSyncEnergy(
            float currentQi,
            float maxQiCapacity,
            boolean manaInfusionActive,
            int uniqueSkillCooldownTicks,
            float currentDivineResistance,
            float maxDivineResistance,
            int divineOverloadTicks,
            float currentSpiritualIntegrity,
            float maxSpiritualIntegrity,
            int spiritShatterTicks,
            float currentStamina,
            float maxStamina,
            byte spiritRegenStateOrdinal,
            int activeMartialSlot,
            int activeMartialCooldownTicks,
            boolean clashActive,
            float clashProgress,
            boolean clashRevoltWindow,
            String clashTargetName,
            int activeDivinePhase
    ) {
        ClientRuntimeState.updateEnergyRuntime(
                activeMartialCooldownTicks, clashActive, clashProgress, clashRevoltWindow, clashTargetName
        );
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            ModCapabilities.getData(localPlayer).ifPresent(playerData -> {
                var cult = playerData.getCultivation();
                cult.setMaxQiCapacity(maxQiCapacity);
                cult.setCurrentQi(currentQi);
                cult.setManaInfusionActive(manaInfusionActive);
                cult.setMaxSpiritualIntegrity(maxSpiritualIntegrity);
                cult.setCurrentSpiritualIntegrity(currentSpiritualIntegrity);
                cult.setSpiritShatterTicks(spiritShatterTicks);
                cult.setClientSpiritRegenStateOrdinal(spiritRegenStateOrdinal);
                cult.setMaxStamina(maxStamina);
                cult.setCurrentStamina(currentStamina);

                var skills = playerData.getSkills();
                skills.setUniqueSkillCooldownTicks(uniqueSkillCooldownTicks);
                skills.setActiveMartialSlot(activeMartialSlot);
                skills.setMaxDivineResistance(maxDivineResistance);
                skills.setCurrentDivineResistance(currentDivineResistance);
                skills.setDivineOverloadTicks(divineOverloadTicks);
                skills.setActiveDivinePhase(activeDivinePhase);
            });
        }
    }

    public static void handleSpawnVisualEffect(SpawnVisualEffectS2CPacket packet) {
        ClientVfxScheduler.enqueue(packet);
    }

    @Nullable
    public static PlayerCoreData getClientPlayerCoreData() {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return null;
        }
        return ModCapabilities.get(localPlayer).orElse(null);
    }

    @Nullable
    public static PlayerData getClientPlayerData() {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return null;
        }
        return ModCapabilities.getData(localPlayer).orElse(null);
    }
}
