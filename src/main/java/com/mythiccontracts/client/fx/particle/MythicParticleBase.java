package com.mythiccontracts.client.fx.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Classe base configurável de partícula customizada via {@link TextureSheetParticle}.
 * Suporta modos paramétricos de movimento fluido ({@link MotionMode}) sem alocação por tick
 * e permanece 100% compatível com o pipeline padrão Minecraft 1.20.1 / Embeddium 0.3.31.
 */
public class MythicParticleBase extends TextureSheetParticle {

    public enum MotionMode {
        LINEAR,
        ORBIT_SPIRAL,
        BEZIER_FOLLOW,
        RADIAL_EASE_OUT
    }

    private final SpriteSet sprites;

    private float startSize = 0.25F;
    private float endSize = 0.05F;
    private float startAlpha = 1.0F;
    private float spinSpeed = 0.0F;
    private float swayAmplitude = 0.0F;
    private float swayFrequency = 0.35F;
    private float swayPhase = 0.0F;
    private boolean fullbright = true;

    private MotionMode motionMode = MotionMode.LINEAR;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private double ctrlX;
    private double ctrlY;
    private double ctrlZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double orbitStartRadius;
    private double orbitEndRadius;
    private double orbitAngle;
    private double orbitAngularSpeed;
    private double orbitVerticalSpeed;
    private float easePower = 2.0F;

    public MythicParticleBase(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SpriteSet sprites
    ) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.hasPhysics = false;
        this.friction = 0.96F;
        this.swayPhase = (float) (this.random.nextDouble() * Math.PI * 2.0D);
        this.setSpriteFromAge(this.sprites);
    }

    public MythicParticleBase configure(
            float startSize,
            float endSize,
            int lifetimeTicks,
            double vx,
            double vy,
            double vz,
            float spinSpeed,
            float gravity,
            float r,
            float g,
            float b,
            float startAlpha
    ) {
        this.startSize = Math.max(0.02F, startSize);
        this.endSize = Math.max(0.01F, endSize);
        this.quadSize = this.startSize;
        this.lifetime = Math.max(2, lifetimeTicks);
        this.age = 0;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.spinSpeed = spinSpeed;
        this.gravity = gravity;
        this.rCol = Mth.clamp(r, 0.0F, 1.0F);
        this.gCol = Mth.clamp(g, 0.0F, 1.0F);
        this.bCol = Mth.clamp(b, 0.0F, 1.0F);
        this.startAlpha = Mth.clamp(startAlpha, 0.05F, 1.0F);
        this.alpha = this.startAlpha;
        this.motionMode = MotionMode.LINEAR;
        this.setSpriteFromAge(this.sprites);
        return this;
    }

    public MythicParticleBase configureOrbitSpiral(
            double centerX,
            double centerY,
            double centerZ,
            double startRadius,
            double endRadius,
            double startAngleRad,
            double angularSpeedRad,
            double verticalSpeedPerTick
    ) {
        this.motionMode = MotionMode.ORBIT_SPIRAL;
        this.anchorX = centerX;
        this.anchorY = centerY;
        this.anchorZ = centerZ;
        this.orbitStartRadius = startRadius;
        this.orbitEndRadius = endRadius;
        this.orbitAngle = startAngleRad;
        this.orbitAngularSpeed = angularSpeedRad;
        this.orbitVerticalSpeed = verticalSpeedPerTick;
        return this;
    }

    public MythicParticleBase configureBezierFollow(
            double startX,
            double startY,
            double startZ,
            double controlX,
            double controlY,
            double controlZ,
            double destX,
            double destY,
            double destZ
    ) {
        this.motionMode = MotionMode.BEZIER_FOLLOW;
        this.anchorX = startX;
        this.anchorY = startY;
        this.anchorZ = startZ;
        this.ctrlX = controlX;
        this.ctrlY = controlY;
        this.ctrlZ = controlZ;
        this.targetX = destX;
        this.targetY = destY;
        this.targetZ = destZ;
        return this;
    }

    public MythicParticleBase configureRadialEaseOut(
            double originX,
            double originY,
            double originZ,
            double destX,
            double destY,
            double destZ,
            float easeExponent
    ) {
        this.motionMode = MotionMode.RADIAL_EASE_OUT;
        this.anchorX = originX;
        this.anchorY = originY;
        this.anchorZ = originZ;
        this.targetX = destX;
        this.targetY = destY;
        this.targetZ = destZ;
        this.easePower = Mth.clamp(easeExponent, 1.1F, 4.0F);
        return this;
    }

    public MythicParticleBase setSway(float amplitude, float frequency) {
        this.swayAmplitude = amplitude;
        this.swayFrequency = frequency;
        return this;
    }

    public MythicParticleBase setFullbright(boolean fullbright) {
        this.fullbright = fullbright;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.oRoll = this.roll;
        this.roll += this.spinSpeed;

        float progress = Mth.clamp((float) this.age / Math.max(1.0F, (float) this.lifetime), 0.0F, 1.0F);

        switch (this.motionMode) {
            case ORBIT_SPIRAL -> {
                this.orbitAngle += this.orbitAngularSpeed;
                double radius = Mth.lerp(progress, (float) this.orbitStartRadius, (float) this.orbitEndRadius);
                double nx = this.anchorX + Math.cos(this.orbitAngle) * radius;
                double ny = this.anchorY + this.orbitVerticalSpeed * this.age;
                double nz = this.anchorZ + Math.sin(this.orbitAngle) * radius;
                this.setPos(nx, ny, nz);
            }
            case BEZIER_FOLLOW -> {
                double u = 1.0D - progress;
                double uu = u * u;
                double tt = progress * progress;
                double twoUt = 2.0D * u * progress;
                double nx = uu * this.anchorX + twoUt * this.ctrlX + tt * this.targetX;
                double ny = uu * this.anchorY + twoUt * this.ctrlY + tt * this.targetY;
                double nz = uu * this.anchorZ + twoUt * this.ctrlZ + tt * this.targetZ;
                this.setPos(nx, ny, nz);
            }
            case RADIAL_EASE_OUT -> {
                double inv = 1.0D - progress;
                double eased = 1.0D - Math.pow(inv, this.easePower);
                double nx = this.anchorX + (this.targetX - this.anchorX) * eased;
                double ny = this.anchorY + (this.targetY - this.anchorY) * eased;
                double nz = this.anchorZ + (this.targetZ - this.anchorZ) * eased;
                this.setPos(nx, ny, nz);
            }
            case LINEAR -> {
                if (this.swayAmplitude > 0.0F) {
                    double sway = Math.cos(this.age * this.swayFrequency + this.swayPhase) * this.swayAmplitude;
                    this.xd += sway;
                    this.zd += Math.sin(this.age * this.swayFrequency + this.swayPhase) * this.swayAmplitude * 0.5D;
                }
                this.yd -= 0.04D * this.gravity;
                this.move(this.xd, this.yd, this.zd);
                this.xd *= this.friction;
                this.yd *= this.friction;
                this.zd *= this.friction;
            }
        }

        this.quadSize = Mth.lerp(progress, this.startSize, this.endSize);

        float envelope;
        if (progress < 0.15F) {
            envelope = Mth.clamp(progress / 0.15F, 0.25F, 1.0F);
        } else if (progress > 0.65F) {
            envelope = Mth.clamp((1.0F - progress) / 0.35F, 0.0F, 1.0F);
        } else {
            envelope = 1.0F;
        }
        this.alpha = this.startAlpha * envelope;

        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.fullbright ? 0xF000F0 : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;
        private final float startSize;
        private final float endSize;
        private final int lifetime;
        private final float spin;
        private final float gravity;
        private final float swayAmplitude;
        private final boolean fullbright;

        public Provider(
                SpriteSet sprites,
                float startSize,
                float endSize,
                int lifetime,
                float spin,
                float gravity,
                float swayAmplitude,
                boolean fullbright
        ) {
            this.sprites = sprites;
            this.startSize = startSize;
            this.endSize = endSize;
            this.lifetime = lifetime;
            this.spin = spin;
            this.gravity = gravity;
            this.swayAmplitude = swayAmplitude;
            this.fullbright = fullbright;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double vx,
                double vy,
                double vz
        ) {
            MythicParticleBase particle = new MythicParticleBase(level, x, y, z, vx, vy, vz, this.sprites);
            particle.configure(
                    this.startSize,
                    this.endSize,
                    this.lifetime,
                    vx,
                    vy,
                    vz,
                    this.spin,
                    this.gravity,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
            particle.setSway(this.swayAmplitude, 0.35F);
            particle.setFullbright(this.fullbright);
            return particle;
        }
    }
}
