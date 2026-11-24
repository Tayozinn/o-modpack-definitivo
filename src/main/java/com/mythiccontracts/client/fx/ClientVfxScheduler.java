package com.mythiccontracts.client.fx;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.fx.particle.MythicParticleBase;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.TargetSpot;
import com.mythiccontracts.registry.ModParticles;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Agendador cliente de coreografias VFX dos 4 Resquícios Divinos e transições vitais.
 * Executa keyframes ao longo do tempo respeitando distância, orçamento máximo por cast e preset
 * visual do cliente (Baixa / Média / Alta), sem qualquer dependência de shaders.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVfxScheduler {

    public static final int MAX_ACTIVE_EFFECTS = 32;
    private static final Deque<ActiveEffect> ACTIVE_EFFECTS = new ArrayDeque<>();

    private ClientVfxScheduler() {
    }

    public static void enqueue(SpawnVisualEffectS2CPacket packet) {
        if (packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        VfxQualityProfile profile = VfxQualityProfile.fromPreset(
                ClientHudConfig.visualQualityPreset,
                getVanillaParticleScale(mc)
        );
        Vec3 origin = new Vec3(packet.getOriginX(), packet.getOriginY(), packet.getOriginZ());
        double maxDistSq = profile.renderDistanceBlocks() * profile.renderDistanceBlocks();
        if (mc.player.position().distanceToSqr(origin) > maxDistSq) {
            return;
        }

        if (ACTIVE_EFFECTS.size() >= MAX_ACTIVE_EFFECTS) {
            ACTIVE_EFFECTS.pollFirst();
        }

        int baseDuration = getBaseDurationTicks(packet.getEffectType());
        int scaledDuration = Math.max(8, Math.round(baseDuration * profile.durationMultiplier()));
        ActiveEffect effect = new ActiveEffect(packet, profile, scaledDuration);
        ACTIVE_EFFECTS.addLast(effect);

        // Executa o keyframe t=0 imediatamente no instante da chegada do pacote
        tickEffect(mc, effect);
        effect.ageTicks++;
    }

    public static void clearAll() {
        ACTIVE_EFFECTS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_EFFECTS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }

        Iterator<ActiveEffect> it = ACTIVE_EFFECTS.iterator();
        while (it.hasNext()) {
            ActiveEffect effect = it.next();
            if (effect.ageTicks >= effect.maxDurationTicks
                    || effect.spawnedParticles >= effect.profile.maxParticlesPerCast()) {
                it.remove();
                continue;
            }
            tickEffect(mc, effect);
            effect.ageTicks++;
            if (effect.ageTicks >= effect.maxDurationTicks) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearAll();
    }

    private static void tickEffect(Minecraft mc, ActiveEffect fx) {
        switch (fx.packet.getEffectType()) {
            case EYE_OF_RA -> tickEyeOfRa(mc, fx);
            case MORRIGAN_REAP -> tickMorriganReap(mc, fx);
            case INDRAS_VAJRA -> tickIndrasVajra(mc, fx);
            case TEZCATLIPOCA_STEP -> tickTezcatlipocaStep(mc, fx);
            case DIVINE_OVERLOAD_BURST -> tickBurst(mc, fx, 0.90F, 0.14F, 0.14F, true);
            case DIVINE_OVERLOAD_REJECT -> tickReject(mc, fx);
            case DIVINE_OVERLOAD_RECOVER -> tickBurst(mc, fx, 0.96F, 0.65F, 0.10F, false);
            case SPIRIT_SHATTER_BURST -> tickSpiritShatter(mc, fx);
            case REALM_BREAKTHROUGH_BURST -> tickBreakthrough(mc, fx);
        }
    }

    // -------------------------------------------------------------------------
    // 1. OLHO DE RÁ (Mitologia Egípcia)
    // -------------------------------------------------------------------------
    private static void tickEyeOfRa(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        double radius = Math.max(3.0D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (t == 0) {
            // Anel solar inicial sob os pés
            for (int i = 0; i < ringPts; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, 1.4D, i, ringPts, 0.0D);
                spawn(mc, fx, ModParticles.RA_SOLAR_RUNE.get(), pt.x, pt.y + 0.15D, pt.z,
                        0.0D, 0.02D, 0.0D, 0.24F, 0.10F, 14, 0.10F, 0.0F,
                        0.99F, 0.78F, 0.18F, 0.95F);
            }
            // Billboard do Olho de Rá a partir da Camada II
            float eyeSize = VfxQualityProfile.computeBillboardSize(tier, fx.profile);
            if (eyeSize > 0.0F) {
                spawn(mc, fx, ModParticles.RA_EYE_GLYPH.get(), origin.x, origin.y + 2.6D, origin.z,
                        0.0D, 0.01D, 0.0D, eyeSize * 0.75F, eyeSize, 18, 0.0F, 0.0F,
                        1.0F, 0.96F, 0.80F, 0.98F);
            }
        } else if (t == 3 || (t == 6 && fx.profile.ringCount() >= 2)) {
            double r = (t == 3) ? radius * 0.55D : radius * 0.90D;
            SimpleParticleType ptType = (t == 3) ? ModParticles.RA_SOLAR_RUNE.get() : ModParticles.RA_SUN_FLARE.get();
            for (int i = 0; i < ringPts; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, r, i, ringPts, t * 0.2D);
                spawn(mc, fx, ptType, pt.x, pt.y + 0.35D, pt.z,
                        0.0D, 0.04D, 0.0D, 0.26F, 0.08F, 13, 0.12F, -0.02F,
                        0.98F, 0.74F, 0.14F, 0.92F);
            }
        } else if (t == 4 || t == 8) {
            // Pilares solares nos alvos atingidos
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            int heightSteps = (tier.ordinal() >= 2) ? 5 : 3;
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                if (t == 4) {
                    for (int h = 0; h < heightSteps; h++) {
                        double py = spot.y() + h * 0.75D;
                        spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(), spot.x(), py, spot.z(),
                                0.0D, -0.05D, 0.0D, 0.28F, 0.08F, 14, 0.04F, 0.0F,
                                1.0F, 0.85F, 0.35F, 0.95F);
                    }
                } else {
                    spawn(mc, fx, ModParticles.RA_SOLAR_RUNE.get(), spot.x(), spot.y() - 0.3D, spot.z(),
                            0.0D, 0.02D, 0.0D, 0.38F, 0.12F, 12, 0.15F, 0.0F,
                            0.95F, 0.45F, 0.08F, 0.90F);
                }
            }
        } else if (t >= 12 && t % 4 == 0 && t < fx.maxDurationTicks) {
            // Aura dourada orbital de encerramento
            int count = tier == ManifestationTier.TIER_4 ? 3 : 2;
            for (int k = 0; k < count; k++) {
                double a = (t * 0.35D) + (k * Math.PI);
                spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(),
                        origin.x + Math.cos(a) * 1.1D, origin.y + 1.0D, origin.z + Math.sin(a) * 1.1D,
                        0.0D, 0.03D, 0.0D, 0.18F, 0.04F, 12, 0.08F, -0.02F,
                        0.99F, 0.82F, 0.25F, 0.75F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. CEIFA DA MORRÍGAN (Mitologia Celta)
    // -------------------------------------------------------------------------
    private static void tickMorriganReap(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        double radius = Math.max(3.5D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (t == 0) {
            // Revoada inicial de penas negras com borda carmesim
            int feathers = tier == ManifestationTier.TIER_4 ? 12 : 6;
            for (int i = 0; i < feathers; i++) {
                double a = (2.0D * Math.PI * i) / feathers;
                spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                        origin.x + Math.cos(a) * 0.6D, origin.y + 1.2D, origin.z + Math.sin(a) * 0.6D,
                        Math.cos(a) * 0.14D, 0.08D, Math.sin(a) * 0.14D,
                        0.26F, 0.18F, 26, 0.08F, 0.06F,
                        1.0F, 1.0F, 1.0F, 0.95F, 0.010F);
            }
            // Asas espectrais simétricas de corvo atrás do conjurador na Camada IV
            if (tier == ManifestationTier.TIER_4 && fx.profile.enableSecondaryArcs()) {
                Vec3 look = fx.horizontalLook();
                Vec3 right = new Vec3(-look.z, 0.0D, look.x);
                for (int side : new int[]{-1, 1}) {
                    for (int seg = 1; seg <= 3; seg++) {
                        Vec3 wingPt = origin.add(look.scale(-0.5D))
                                .add(right.scale(side * seg * 0.55D))
                                .add(0.0D, 1.0D + seg * 0.35D, 0.0D);
                        spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                                wingPt.x, wingPt.y, wingPt.z,
                                right.x * side * 0.04D, 0.04D, right.z * side * 0.04D,
                                0.36F, 0.22F, 22, 0.05F, 0.03F,
                                0.98F, 0.35F, 0.40F, 0.95F, 0.008F);
                    }
                }
            }
        } else if (t == 3 || (t == 6 && fx.profile.ringCount() >= 2)) {
            // Varredura 360 graus com arco de foice carmesim
            double r = (t == 3) ? radius * 0.52D : radius * 0.85D;
            int slashCount = Math.max(8, ringPts / 2);
            for (int i = 0; i < slashCount; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, r, i, slashCount, t * 0.25D);
                spawn(mc, fx, ModParticles.MORRIGAN_SLASH_ARC.get(),
                        pt.x, pt.y + 0.9D, pt.z,
                        0.0D, 0.01D, 0.0D,
                        0.55F, 0.85F, 10, 0.20F, 0.0F,
                        0.95F, 0.22F, 0.25F, 0.95F);
            }
        } else if (t == 5 || t == 8 || t == 11 || t == 14) {
            // Curva Bézier de drenagem vital dos alvos de volta ao peito do conjurador
            float u = (t - 5) / 9.0F;
            Vec3 chest = origin.add(0.0D, 1.1D, 0.0D);
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                Vec3 start = new Vec3(spot.x(), spot.y(), spot.z());
                Vec3 mid = start.lerp(chest, 0.5D).add(0.0D, 1.25D, 0.0D);
                Vec3 bezPt = VfxQualityProfile.quadraticBezier(start, mid, chest, u);
                spawn(mc, fx, ModParticles.MORRIGAN_BLOOD_ORB.get(),
                        bezPt.x, bezPt.y, bezPt.z,
                        0.0D, 0.01D, 0.0D,
                        0.24F, 0.12F, 10, 0.06F, 0.0F,
                        0.95F, 0.18F, 0.22F, 0.96F);
            }
        } else if ((t == 18 || t == 25) && fx.profile.enableSecondaryArcs()) {
            for (int i = 0; i < 4; i++) {
                double a = (t * 0.5D) + (i * Math.PI * 0.5D);
                spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                        origin.x + Math.cos(a) * 1.8D, origin.y + 1.8D, origin.z + Math.sin(a) * 1.8D,
                        0.0D, -0.02D, 0.0D,
                        0.22F, 0.16F, 22, 0.07F, 0.05F,
                        1.0F, 1.0F, 1.0F, 0.80F, 0.012F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. VAJRA DE INDRA (Mitologia Védica)
    // -------------------------------------------------------------------------
    private static void tickIndrasVajra(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        double radius = Math.max(4.0D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (t == 0) {
            if (mc.level != null) {
                mc.level.addParticle(ParticleTypes.SONIC_BOOM, origin.x, origin.y + 1.1D, origin.z, 0.0D, 0.0D, 0.0D);
            }
            // Arcos elétricos convergentes iniciais + emblema Vajra a partir da Camada III
            for (int i = 0; i < 6; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, 1.3D, i, 6, 0.0D);
                spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                        pt.x, pt.y + 1.0D, pt.z,
                        0.0D, 0.05D, 0.0D,
                        0.38F, 0.20F, 9, 0.0F, 0.0F,
                        0.45F, 0.85F, 1.0F, 0.98F);
            }
            if (tier.ordinal() >= ManifestationTier.TIER_3.ordinal()) {
                float crestSize = VfxQualityProfile.computeBillboardSize(tier, fx.profile) * 0.85F;
                spawn(mc, fx, ModParticles.INDRA_VAJRA_CREST.get(),
                        origin.x, origin.y + 2.3D, origin.z,
                        0.0D, 0.01D, 0.0D,
                        crestSize * 0.8F, crestSize, 18, 0.10F, 0.0F,
                        0.60F, 0.92F, 1.0F, 0.96F);
            }
        } else if (t == 2 || (t == 5 && fx.profile.ringCount() >= 2)) {
            double r = (t == 2) ? radius * 0.45D : radius * 0.85D;
            float rCol = (t == 2) ? 0.30F : 0.72F;
            float gCol = (t == 2) ? 0.80F : 0.42F;
            float bCol = 1.0F;
            for (int i = 0; i < ringPts; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, r, i, ringPts, t * 0.15D);
                spawn(mc, fx, ModParticles.INDRA_SHOCK_RING.get(),
                        pt.x, pt.y + 0.7D, pt.z,
                        0.0D, 0.01D, 0.0D,
                        0.48F, 0.90F, 11, 0.0F, 0.0F,
                        rCol, gCol, bCol, 0.92F);
            }
        } else if (t == 4 || t == 8) {
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                if (spot.boss()) {
                    // Raio vertical espetacular sobre Bosses (#high_realm_boss)
                    for (int h = 0; h < 4; h++) {
                        spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                                spot.x(), spot.y() + h * 1.0D, spot.z(),
                                0.0D, -0.02D, 0.0D,
                                0.55F, 0.25F, 12, 0.0F, 0.0F,
                                0.85F, 0.96F, 1.0F, 1.0F);
                    }
                } else {
                    spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                            spot.x(), spot.y() + 0.3D, spot.z(),
                            0.0D, 0.04D, 0.0D,
                            0.38F, 0.18F, 10, 0.0F, 0.0F,
                            0.35F, 0.80F, 1.0F, 0.95F);
                }
            }
        } else if (t == 14 && fx.profile.enableSecondaryArcs()) {
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(Math.min(4, fx.profile.maxTargets()), targets.size());
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                        spot.x(), spot.y(), spot.z(),
                        0.0D, 0.02D, 0.0D,
                        0.26F, 0.10F, 8, 0.0F, 0.0F,
                        0.66F, 0.45F, 0.98F, 0.85F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. PASSO DE TEZCATLIPOCA (Mitologia Asteca)
    // -------------------------------------------------------------------------
    private static void tickTezcatlipocaStep(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        Vec3 dir = fx.horizontalLook();

        if (t == 0) {
            // Espelho fumegante na partida + anel rápido de lascas de obsidiana
            spawn(mc, fx, ModParticles.TEZCA_SMOKE_MIRROR.get(),
                    origin.x, origin.y + 0.9D, origin.z,
                    0.0D, 0.01D, 0.0D,
                    0.80F, 1.35F, 15, 0.06F, 0.0F,
                    1.0F, 1.0F, 1.0F, 0.95F);
            int shards = Math.max(8, fx.profile.ringPoints() / 2);
            for (int i = 0; i < shards; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, 1.6D, i, shards, 0.0D);
                Vec3 out = pt.subtract(origin).normalize().scale(0.14D);
                spawn(mc, fx, ModParticles.TEZCA_OBSIDIAN_SHARD.get(),
                        pt.x, pt.y + 0.8D, pt.z,
                        out.x, 0.04D, out.z,
                        0.24F, 0.10F, 13, 0.24F, 0.02F,
                        1.0F, 1.0F, 1.0F, 0.94F);
            }
        } else if (t == 1) {
            // Pós-imagem espectral deixada na origem
            float wispSize = tier.ordinal() >= ManifestationTier.TIER_3.ordinal() ? 0.95F : 0.72F;
            spawn(mc, fx, ModParticles.TEZCA_AFTERIMAGE_WISP.get(),
                    origin.x, origin.y + 1.0D, origin.z,
                    0.0D, 0.015D, 0.0D,
                    wispSize, wispSize * 0.75F, 18, 0.0F, -0.01F,
                    1.0F, 1.0F, 1.0F, 0.92F);
        } else if (t == 2 || t == 4 || t == 6 || t == 8) {
            // Rastro turquesa/obsidiana ao longo do dash
            double dist = (t / 2.0D) * 1.45D;
            Vec3 trailPt = origin.add(dir.scale(dist)).add(0.0D, 0.85D, 0.0D);
            for (int k = 0; k < 3; k++) {
                double ox = (k - 1) * 0.25D;
                spawn(mc, fx, ModParticles.TEZCA_OBSIDIAN_SHARD.get(),
                        trailPt.x + ox, trailPt.y + k * 0.15D, trailPt.z - ox,
                        -dir.x * 0.05D, 0.03D, -dir.z * 0.05D,
                        0.24F, 0.08F, 12, 0.22F, 0.02F,
                        0.20F, 0.88F, 0.95F, 0.90F);
            }
            if (t == 8 && tier == ManifestationTier.TIER_4) {
                // Segundo Espelho Fumegante na chegada do dash (Camada IV)
                spawn(mc, fx, ModParticles.TEZCA_SMOKE_MIRROR.get(),
                        trailPt.x, trailPt.y, trailPt.z,
                        0.0D, 0.01D, 0.0D,
                        0.75F, 1.25F, 15, -0.06F, 0.0F,
                        0.25F, 0.92F, 0.95F, 0.95F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5. FEEDBACKS PADRONIZADOS (SOBRECARGA, COLAPSO ESPIRITUAL & QUEBRA DE REINO)
    // -------------------------------------------------------------------------
    private static void tickBurst(Minecraft mc, ActiveEffect fx, float r, float g, float b, boolean withSmoke) {
        if (fx.ageTicks != 0) {
            return;
        }
        int pts = Math.min(18, fx.profile.ringPoints());
        Vec3 origin = fx.origin;
        for (int i = 0; i < pts; i++) {
            Vec3 pt = VfxQualityProfile.circleRingPoint(origin, 0.7D, i, pts, 0.0D);
            Vec3 vel = pt.subtract(origin).normalize().scale(0.18D);
            spawn(mc, fx, ModParticles.DIVINE_FRACTURE_SPARK.get(),
                    pt.x, pt.y + 1.0D, pt.z,
                    vel.x, 0.06D, vel.z,
                    0.26F, 0.06F, 14, 0.16F, 0.02F,
                    r, g, b, 0.96F);
        }
        if (withSmoke && mc.level != null) {
            for (int i = 0; i < 10; i++) {
                double a = (2.0D * Math.PI * i) / 10.0D;
                mc.level.addParticle(ParticleTypes.SMOKE,
                        origin.x + Math.cos(a) * 0.4D, origin.y + 1.0D, origin.z + Math.sin(a) * 0.4D,
                        Math.cos(a) * 0.05D, 0.04D, Math.sin(a) * 0.05D);
            }
        }
    }

    private static void tickReject(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks != 0) {
            return;
        }
        Vec3 origin = fx.origin;
        for (int i = 0; i < 6; i++) {
            double a = (2.0D * Math.PI * i) / 6.0D;
            spawn(mc, fx, ModParticles.DIVINE_FRACTURE_SPARK.get(),
                    origin.x + Math.cos(a) * 0.35D, origin.y + 1.05D, origin.z + Math.sin(a) * 0.35D,
                    Math.cos(a) * 0.08D, 0.04D, Math.sin(a) * 0.08D,
                    0.20F, 0.05F, 10, 0.18F, 0.01F,
                    0.94F, 0.22F, 0.22F, 0.95F);
        }
    }

    private static void tickSpiritShatter(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks != 0) {
            return;
        }
        tickBurst(mc, fx, 0.72F, 0.28F, 0.95F, false);
        if (mc.level != null) {
            Vec3 origin = fx.origin;
            for (int i = 0; i < 8; i++) {
                double a = (2.0D * Math.PI * i) / 8.0D;
                mc.level.addParticle(ParticleTypes.SOUL,
                        origin.x + Math.cos(a) * 0.5D, origin.y + 0.9D, origin.z + Math.sin(a) * 0.5D,
                        0.0D, 0.08D, 0.0D);
            }
        }
    }

    private static void tickBreakthrough(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t % 2 != 0 || t > 18) {
            return;
        }
        Vec3 origin = fx.origin;
        double yOff = t * 0.14D;
        double angle = t * 0.65D;
        for (int k = 0; k < 2; k++) {
            double a = angle + k * Math.PI;
            SimpleParticleType type = (k == 0) ? ModParticles.RA_SUN_FLARE.get() : ModParticles.DIVINE_FRACTURE_SPARK.get();
            float r = (k == 0) ? 0.99F : 0.22F;
            float g = (k == 0) ? 0.80F : 0.86F;
            float b = (k == 0) ? 0.22F : 0.98F;
            spawn(mc, fx, type,
                    origin.x + Math.cos(a) * 1.15D, origin.y + 0.3D + yOff, origin.z + Math.sin(a) * 1.15D,
                    0.0D, 0.05D, 0.0D,
                    0.28F, 0.06F, 14, 0.15F, -0.02F,
                    r, g, b, 0.96F);
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private static void spawn(
            Minecraft mc,
            ActiveEffect fx,
            SimpleParticleType type,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            float startSize,
            float endSize,
            int lifetimeTicks,
            float spinSpeed,
            float gravity,
            float r,
            float g,
            float b,
            float alpha
    ) {
        if (fx.spawnedParticles >= fx.profile.maxParticlesPerCast()) {
            return;
        }
        Particle p = mc.particleEngine.createParticle(type, x, y, z, vx, vy, vz);
        if (p instanceof MythicParticleBase mythic) {
            mythic.configure(startSize, endSize, lifetimeTicks, vx, vy, vz, spinSpeed, gravity, r, g, b, alpha);
        }
        fx.spawnedParticles++;
    }

    private static void spawnSway(
            Minecraft mc,
            ActiveEffect fx,
            SimpleParticleType type,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            float startSize,
            float endSize,
            int lifetimeTicks,
            float spinSpeed,
            float gravity,
            float r,
            float g,
            float b,
            float alpha,
            float swayAmp
    ) {
        if (fx.spawnedParticles >= fx.profile.maxParticlesPerCast()) {
            return;
        }
        Particle p = mc.particleEngine.createParticle(type, x, y, z, vx, vy, vz);
        if (p instanceof MythicParticleBase mythic) {
            mythic.configure(startSize, endSize, lifetimeTicks, vx, vy, vz, spinSpeed, gravity, r, g, b, alpha)
                    .setSway(swayAmp, 0.35F);
        }
        fx.spawnedParticles++;
    }

    private static float getVanillaParticleScale(Minecraft mc) {
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) {
            return 0.30F;
        }
        if (status == ParticleStatus.DECREASED) {
            return 0.65F;
        }
        return 1.0F;
    }

    private static int getBaseDurationTicks(EffectType type) {
        return switch (type) {
            case EYE_OF_RA, MORRIGAN_REAP -> 32;
            case INDRAS_VAJRA -> 24;
            case TEZCATLIPOCA_STEP, REALM_BREAKTHROUGH_BURST -> 20;
            case DIVINE_OVERLOAD_BURST, DIVINE_OVERLOAD_REJECT, DIVINE_OVERLOAD_RECOVER, SPIRIT_SHATTER_BURST -> 10;
        };
    }

    private static final class ActiveEffect {
        final SpawnVisualEffectS2CPacket packet;
        final VfxQualityProfile profile;
        final ManifestationTier tier;
        final Vec3 origin;
        final int maxDurationTicks;
        int ageTicks = 0;
        int spawnedParticles = 0;

        ActiveEffect(SpawnVisualEffectS2CPacket packet, VfxQualityProfile profile, int maxDurationTicks) {
            this.packet = packet;
            this.profile = profile;
            this.tier = ManifestationTier.values()[Math.max(0, Math.min(3, packet.getTierOrdinal()))];
            this.origin = new Vec3(packet.getOriginX(), packet.getOriginY(), packet.getOriginZ());
            this.maxDurationTicks = maxDurationTicks;
        }

        Vec3 horizontalLook() {
            Vec3 flat = new Vec3(this.packet.getDirX(), 0.0D, this.packet.getDirZ());
            if (flat.lengthSqr() < 1.0E-4D) {
                return new Vec3(0.0D, 0.0D, 1.0D);
            }
            return flat.normalize();
        }
    }
}
