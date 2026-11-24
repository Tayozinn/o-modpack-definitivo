package com.mythiccontracts.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Pacote único S2C contendo os parâmetros essenciais de um evento visual dos Resquícios Divinos
 * ou estados vitais, permitindo ao cliente coreografar partículas customizadas respeitando seu preset.
 */
public class SpawnVisualEffectS2CPacket {

    public static final int MAX_TARGETS = 12;

    public enum EffectType {
        EYE_OF_RA,
        MORRIGAN_REAP,
        INDRAS_VAJRA,
        TEZCATLIPOCA_STEP,
        DIVINE_OVERLOAD_BURST,
        DIVINE_OVERLOAD_REJECT,
        DIVINE_OVERLOAD_RECOVER,
        SPIRIT_SHATTER_BURST,
        REALM_BREAKTHROUGH_BURST
    }

    public record TargetSpot(float x, float y, float z, boolean boss) {
        public TargetSpot {
            x = Float.isFinite(x) ? x : 0.0F;
            y = Float.isFinite(y) ? y : 0.0F;
            z = Float.isFinite(z) ? z : 0.0F;
        }
    }

    private final EffectType effectType;
    private final int tierOrdinal;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final float dirX;
    private final float dirY;
    private final float dirZ;
    private final float radius;
    private final List<TargetSpot> targets;

    public SpawnVisualEffectS2CPacket(
            EffectType effectType,
            int tierOrdinal,
            Vec3 origin,
            Vec3 lookDir,
            float radius,
            List<TargetSpot> targets
    ) {
        this(
                effectType,
                tierOrdinal,
                origin != null ? origin.x : 0.0D,
                origin != null ? origin.y : 0.0D,
                origin != null ? origin.z : 0.0D,
                lookDir != null ? (float) lookDir.x : 0.0F,
                lookDir != null ? (float) lookDir.y : 0.0F,
                lookDir != null ? (float) lookDir.z : 1.0F,
                radius,
                targets
        );
    }

    public SpawnVisualEffectS2CPacket(
            EffectType effectType,
            int tierOrdinal,
            double originX,
            double originY,
            double originZ,
            float dirX,
            float dirY,
            float dirZ,
            float radius,
            List<TargetSpot> targets
    ) {
        this.effectType = effectType != null ? effectType : EffectType.EYE_OF_RA;
        this.tierOrdinal = Mth.clamp(tierOrdinal, 0, 3);
        this.originX = sanitizeDouble(originX);
        this.originY = sanitizeDouble(originY);
        this.originZ = sanitizeDouble(originZ);
        this.dirX = sanitizeFloat(dirX);
        this.dirY = sanitizeFloat(dirY);
        this.dirZ = sanitizeFloat(dirZ);
        this.radius = Mth.clamp(sanitizeFloat(radius), 0.0F, 32.0F);

        if (targets == null || targets.isEmpty()) {
            this.targets = List.of();
        } else {
            int limit = Math.min(MAX_TARGETS, targets.size());
            List<TargetSpot> copy = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                TargetSpot spot = targets.get(i);
                if (spot != null) {
                    copy.add(spot);
                }
            }
            this.targets = Collections.unmodifiableList(copy);
        }
    }

    public SpawnVisualEffectS2CPacket(FriendlyByteBuf buf) {
        this.effectType = buf.readEnum(EffectType.class);
        this.tierOrdinal = Mth.clamp(buf.readByte(), 0, 3);
        this.originX = sanitizeDouble(buf.readDouble());
        this.originY = sanitizeDouble(buf.readDouble());
        this.originZ = sanitizeDouble(buf.readDouble());
        this.dirX = sanitizeFloat(buf.readFloat());
        this.dirY = sanitizeFloat(buf.readFloat());
        this.dirZ = sanitizeFloat(buf.readFloat());
        this.radius = Mth.clamp(sanitizeFloat(buf.readFloat()), 0.0F, 32.0F);

        int count = Mth.clamp(buf.readVarInt(), 0, MAX_TARGETS);
        if (count == 0) {
            this.targets = List.of();
        } else {
            List<TargetSpot> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                float tx = buf.readFloat();
                float ty = buf.readFloat();
                float tz = buf.readFloat();
                boolean boss = buf.readBoolean();
                list.add(new TargetSpot(tx, ty, tz, boss));
            }
            this.targets = Collections.unmodifiableList(list);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.effectType);
        buf.writeByte(this.tierOrdinal);
        buf.writeDouble(this.originX);
        buf.writeDouble(this.originY);
        buf.writeDouble(this.originZ);
        buf.writeFloat(this.dirX);
        buf.writeFloat(this.dirY);
        buf.writeFloat(this.dirZ);
        buf.writeFloat(this.radius);
        buf.writeVarInt(this.targets.size());
        for (TargetSpot spot : this.targets) {
            buf.writeFloat(spot.x());
            buf.writeFloat(spot.y());
            buf.writeFloat(spot.z());
            buf.writeBoolean(spot.boss());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSpawnVisualEffect(this)
        ));
        ctx.setPacketHandled(true);
        return true;
    }

    public EffectType getEffectType() {
        return this.effectType;
    }

    public int getTierOrdinal() {
        return this.tierOrdinal;
    }

    public double getOriginX() {
        return this.originX;
    }

    public double getOriginY() {
        return this.originY;
    }

    public double getOriginZ() {
        return this.originZ;
    }

    public float getDirX() {
        return this.dirX;
    }

    public float getDirY() {
        return this.dirY;
    }

    public float getDirZ() {
        return this.dirZ;
    }

    public float getRadius() {
        return this.radius;
    }

    public List<TargetSpot> getTargets() {
        return this.targets;
    }

    private static double sanitizeDouble(double v) {
        return Double.isFinite(v) ? v : 0.0D;
    }

    private static float sanitizeFloat(float v) {
        return Float.isFinite(v) ? v : 0.0F;
    }
}
