package com.mythiccontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Pacote leve de sincronização para Qi, Resistência Divina, Integridade Espiritual (+estado),
 * Vigor (Stamina), Infusão, slot marcial ativo e disputa espiritual de Subjugação.
 */
public class SyncEnergyS2CPacket {

    private static final int MAX_CLASH_NAME_LEN = 48;

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
    private final byte spiritRegenStateOrdinal;
    private final int activeMartialSlot;
    private final int activeMartialCooldownTicks;
    private final boolean clashActive;
    private final float clashProgress;
    private final boolean clashRevoltWindow;
    private final String clashTargetName;
    private final int activeDivinePhase;

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
        this(
                currentQi, maxQiCapacity, manaInfusionActive, uniqueSkillCooldownTicks,
                currentDivineResistance, maxDivineResistance, divineOverloadTicks,
                currentSpiritualIntegrity, maxSpiritualIntegrity, spiritShatterTicks,
                currentStamina, maxStamina,
                (byte) 4, 0, 0, false, 0.0F, false, "", 0
        );
    }

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
            float maxStamina,
            byte spiritRegenStateOrdinal,
            int activeMartialSlot,
            int activeMartialCooldownTicks,
            boolean clashActive,
            float clashProgress,
            boolean clashRevoltWindow,
            String clashTargetName
    ) {
        this(
                currentQi, maxQiCapacity, manaInfusionActive, uniqueSkillCooldownTicks,
                currentDivineResistance, maxDivineResistance, divineOverloadTicks,
                currentSpiritualIntegrity, maxSpiritualIntegrity, spiritShatterTicks,
                currentStamina, maxStamina,
                spiritRegenStateOrdinal, activeMartialSlot, activeMartialCooldownTicks,
                clashActive, clashProgress, clashRevoltWindow, clashTargetName, 0
        );
    }

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
        this.spiritRegenStateOrdinal = (byte) Mth.clamp(spiritRegenStateOrdinal, 0, 4);
        this.activeMartialSlot = Mth.clamp(activeMartialSlot, 0, 2);
        this.activeMartialCooldownTicks = Math.max(0, activeMartialCooldownTicks);
        this.clashActive = clashActive;
        this.clashProgress = Float.isFinite(clashProgress) ? Mth.clamp(clashProgress, 0.0F, 1.0F) : 0.0F;
        this.clashRevoltWindow = clashActive && clashRevoltWindow;
        this.clashTargetName = truncateName(clashTargetName);
        this.activeDivinePhase = Mth.clamp(activeDivinePhase, 0, 3);
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

        this.spiritRegenStateOrdinal = (byte) Mth.clamp(buf.readByte(), 0, 4);
        this.activeMartialSlot = Mth.clamp(buf.readVarInt(), 0, 2);
        this.activeMartialCooldownTicks = Math.max(0, buf.readVarInt());
        this.clashActive = buf.readBoolean();
        if (this.clashActive) {
            float rawProg = buf.readFloat();
            this.clashProgress = Float.isFinite(rawProg) ? Mth.clamp(rawProg, 0.0F, 1.0F) : 0.0F;
            this.clashRevoltWindow = buf.readBoolean();
            this.clashTargetName = truncateName(buf.readUtf(MAX_CLASH_NAME_LEN));
        } else {
            this.clashProgress = 0.0F;
            this.clashRevoltWindow = false;
            this.clashTargetName = "";
        }
        this.activeDivinePhase = Mth.clamp(buf.readByte(), 0, 3);
    }

    private static float sanitizeNonNegative(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : fallback;
    }

    private static String truncateName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.length() > MAX_CLASH_NAME_LEN ? raw.substring(0, MAX_CLASH_NAME_LEN) : raw;
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
        buf.writeByte(this.spiritRegenStateOrdinal);
        buf.writeVarInt(this.activeMartialSlot);
        buf.writeVarInt(this.activeMartialCooldownTicks);
        buf.writeBoolean(this.clashActive);
        if (this.clashActive) {
            buf.writeFloat(this.clashProgress);
            buf.writeBoolean(this.clashRevoltWindow);
            buf.writeUtf(this.clashTargetName, MAX_CLASH_NAME_LEN);
        }
        buf.writeByte(this.activeDivinePhase);
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
                        this.maxStamina,
                        this.spiritRegenStateOrdinal,
                        this.activeMartialSlot,
                        this.activeMartialCooldownTicks,
                        this.clashActive,
                        this.clashProgress,
                        this.clashRevoltWindow,
                        this.clashTargetName,
                        this.activeDivinePhase
                )
        ));
        ctx.setPacketHandled(true);
        return true;
    }

    public byte getSpiritRegenStateOrdinal() {
        return this.spiritRegenStateOrdinal;
    }

    public int getActiveMartialSlot() {
        return this.activeMartialSlot;
    }

    public int getActiveMartialCooldownTicks() {
        return this.activeMartialCooldownTicks;
    }

    public boolean isClashActive() {
        return this.clashActive;
    }

    public float getClashProgress() {
        return this.clashProgress;
    }

    public boolean isClashRevoltWindow() {
        return this.clashRevoltWindow;
    }

    public String getClashTargetName() {
        return this.clashTargetName;
    }

    public int getActiveDivinePhase() {
        return this.activeDivinePhase;
    }
}
