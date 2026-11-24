package com.mythiccontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Pacote leve de sincronização para Qi, Resistência Divina, Integridade Espiritual,
 * Vigor (Stamina), estado da Infusão e temporizadores de recarga/bloqueio.
 */
public class SyncEnergyS2CPacket {

    private final float currentQi;
    private final float maxQiCapacity;
    private final boolean manaInfusionActive;
    private final int uniqueSkillCooldownTicks;
    private final float currentDivineResistance;
    private final float maxDivineResistance;
    private final int divineOverloadTicks;
    private final float currentSpiritualIntegrity;
    private final float maxSpiritualIntegrity;
    private final int spiritShatterTicks;
    private final float currentStamina;
    private final float maxStamina;

    public SyncEnergyS2CPacket(
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
            float maxStamina
    ) {
        this.maxQiCapacity = Math.max(1.0F, sanitizeNonNegative(maxQiCapacity, 100.0F));
        this.currentQi = Math.min(this.maxQiCapacity, sanitizeNonNegative(currentQi, 0.0F));
        this.manaInfusionActive = manaInfusionActive;
        this.uniqueSkillCooldownTicks = Math.max(0, uniqueSkillCooldownTicks);
        this.maxDivineResistance = Math.max(1.0F, sanitizeNonNegative(maxDivineResistance, 100.0F));
        this.currentDivineResistance = Math.min(this.maxDivineResistance, sanitizeNonNegative(currentDivineResistance, 0.0F));
        this.divineOverloadTicks = Math.max(0, divineOverloadTicks);
        this.maxSpiritualIntegrity = Math.max(1.0F, sanitizeNonNegative(maxSpiritualIntegrity, 100.0F));
        this.currentSpiritualIntegrity = Math.min(this.maxSpiritualIntegrity, sanitizeNonNegative(currentSpiritualIntegrity, 0.0F));
        this.spiritShatterTicks = Math.max(0, spiritShatterTicks);
        this.maxStamina = Math.max(1.0F, sanitizeNonNegative(maxStamina, 100.0F));
        this.currentStamina = Math.min(this.maxStamina, sanitizeNonNegative(currentStamina, 0.0F));
    }

    public SyncEnergyS2CPacket(FriendlyByteBuf buf) {
        float rawQi = buf.readFloat();
        float rawMaxQi = buf.readFloat();
        this.manaInfusionActive = buf.readBoolean();
        this.uniqueSkillCooldownTicks = Math.max(0, buf.readVarInt());
        float rawDivine = buf.readFloat();
        float rawMaxDivine = buf.readFloat();
        this.divineOverloadTicks = Math.max(0, buf.readVarInt());
        float rawSpirit = buf.readFloat();
        float rawMaxSpirit = buf.readFloat();
        this.spiritShatterTicks = Math.max(0, buf.readVarInt());
        float rawStamina = buf.readFloat();
        float rawMaxStamina = buf.readFloat();

        this.maxQiCapacity = Math.max(1.0F, sanitizeNonNegative(rawMaxQi, 100.0F));
        this.currentQi = Math.min(this.maxQiCapacity, sanitizeNonNegative(rawQi, 0.0F));
        this.maxDivineResistance = Math.max(1.0F, sanitizeNonNegative(rawMaxDivine, 100.0F));
        this.currentDivineResistance = Math.min(this.maxDivineResistance, sanitizeNonNegative(rawDivine, 0.0F));
        this.maxSpiritualIntegrity = Math.max(1.0F, sanitizeNonNegative(rawMaxSpirit, 100.0F));
        this.currentSpiritualIntegrity = Math.min(this.maxSpiritualIntegrity, sanitizeNonNegative(rawSpirit, 0.0F));
        this.maxStamina = Math.max(1.0F, sanitizeNonNegative(rawMaxStamina, 100.0F));
        this.currentStamina = Math.min(this.maxStamina, sanitizeNonNegative(rawStamina, 0.0F));
    }

    private static float sanitizeNonNegative(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : fallback;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(this.currentQi);
        buf.writeFloat(this.maxQiCapacity);
        buf.writeBoolean(this.manaInfusionActive);
        buf.writeVarInt(this.uniqueSkillCooldownTicks);
        buf.writeFloat(this.currentDivineResistance);
        buf.writeFloat(this.maxDivineResistance);
        buf.writeVarInt(this.divineOverloadTicks);
        buf.writeFloat(this.currentSpiritualIntegrity);
        buf.writeFloat(this.maxSpiritualIntegrity);
        buf.writeVarInt(this.spiritShatterTicks);
        buf.writeFloat(this.currentStamina);
        buf.writeFloat(this.maxStamina);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSyncEnergy(
                        this.currentQi,
                        this.maxQiCapacity,
                        this.manaInfusionActive,
                        this.uniqueSkillCooldownTicks,
                        this.currentDivineResistance,
                        this.maxDivineResistance,
                        this.divineOverloadTicks,
                        this.currentSpiritualIntegrity,
                        this.maxSpiritualIntegrity,
                        this.spiritShatterTicks,
                        this.currentStamina,
                        this.maxStamina
                )
        ));
        ctx.setPacketHandled(true);
        return true;
    }
}
