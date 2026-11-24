package com.mythiccontracts.client.fx;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.fx.particle.MythicParticleBase;
import com.mythiccontracts.client.sound.ClientSoundController;
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
import net.minecraft.util.Mth;
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
 * Agendador cliente de coreografias VFX em 5 fases para os 4 Resquícios Divinos,
 * Disputa Espiritual de Subjugação, 11 Técnicas Ativas de Cultivo e eventos vitais.
 * Combina partículas paramétricas ({@link MythicParticleBase}) com ribbons nativos
 * ({@link ClientRibbonRenderer}), respeitando orçamento e preset do cliente.
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

        ClientSkillAnimationController.onVisualEffect(packet);
        ClientSoundController.onVisualEffect(packet);

        if (ACTIVE_EFFECTS.size() >= MAX_ACTIVE_EFFECTS) {
            ACTIVE_EFFECTS.pollFirst();
        }

        int baseDuration = getBaseDurationTicks(packet.getEffectType());
        int scaledDuration = Math.max(8, Math.round(baseDuration * profile.durationMultiplier()));
        ActiveEffect effect = new ActiveEffect(packet, profile, scaledDuration);
        ACTIVE_EFFECTS.addLast(effect);

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
            case SUBJUGATION_CLASH_START -> tickSubjugationStart(mc, fx);
            case SUBJUGATION_CLASH_TICK -> tickSubjugationPulse(mc, fx);
            case SUBJUGATION_SUCCESS -> tickSubjugationSuccess(mc, fx);
            case SUBJUGATION_FAIL -> tickSubjugationFail(mc, fx);
            case SUBJUGATION_SUMMON -> tickSubjugationSummon(mc, fx);
            case GALE_MERIDIAN_STEP -> tickGaleStep(mc, fx);
            case INNER_BREATH_MEDITATION -> tickInnerBreath(mc, fx);
            case JADE_BONE_GUARD -> tickJadeGuard(mc, fx);
            case SEISMIC_PALM -> tickSeismicPalm(mc, fx);
            case GOLDEN_CORE_AEGIS -> tickGoldenAegis(mc, fx);
            case BLOOD_FURNACE_SURGE -> tickBloodFurnace(mc, fx);
            case NASCENT_SOUL_LANCE -> tickSoulLance(mc, fx);
            case ETHER_FLASH_STRIKE -> tickEtherFlash(mc, fx);
            case SPIRIT_DOMAIN_SUPPRESSION -> tickSpiritDomain(mc, fx);
            case UNDYING_MERIDIAN_REVERSAL -> tickUndyingReversal(mc, fx);
            case PRIMORDIAL_HEAVEN_SUNDER -> tickHeavenSunder(mc, fx);
        }
    }

    // -------------------------------------------------------------------------
    // 1. RESQUÍCIOS DIVINOS — 16 FORMAS (4 FASES × 4 DEUSES) + 5 FASES ORGÂNICAS
    // -------------------------------------------------------------------------
    private static void tickEyeOfRa(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        int phase = Mth.clamp(fx.packet.getTierOrdinal(), 0, 3);
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        double radius = Math.max(3.5D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (phase == 0) {
            // H1 · Faísca Solar (Fase I contida: feixe fino direto ao alvo)
            if (t == 0) {
                Vec3 start = origin.add(0.0D, 1.35D, 0.0D);
                Vec3 look = new Vec3(fx.packet.getDirX(), fx.packet.getDirY(), fx.packet.getDirZ());
                Vec3 dir = look.lengthSqr() > 1.0E-4D ? look.normalize() : fx.horizontalLook();
                Vec3 end = fx.packet.getTargets().isEmpty()
                        ? start.add(dir.scale(6.5D))
                        : new Vec3(fx.packet.getTargets().get(0).x(), fx.packet.getTargets().get(0).y(), fx.packet.getTargets().get(0).z());
                ClientRibbonRenderer.addBeamRibbon(start, end, 0.07F, 10, 1.0F, 0.85F, 0.25F, 0.92F, 0.0F);
                for (int i = 0; i < 7; i++) {
                    Vec3 pt = start.lerp(end, (i + 1) / 7.0D);
                    spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(), pt.x, pt.y, pt.z,
                            0.0D, 0.01D, 0.0D, 0.20F, 0.05F, 10, 0.08F, 0.0F, 1.0F, 0.84F, 0.22F, 0.94F);
                }
            }
            return;
        }

        if (phase == 1) {
            // H2 · Égide do Disco Solar (Fase II: cúpula/anel dourado com runas orbitais)
            if (t == 0 || t == 5) {
                ClientRibbonRenderer.addArcRibbon(origin, 2.4D, t * 0.4D, Math.PI * 2.0D, 0.95D, 0.0D, 0.12F, 16, 1.0F, 0.82F, 0.24F, 0.92F);
                int pts = Math.max(8, ringPts / 2);
                for (int i = 0; i < pts; i++) {
                    double ang = (2.0D * Math.PI * i) / pts;
                    MythicParticleBase p = spawn(mc, fx, ModParticles.RA_SOLAR_RUNE.get(),
                            origin.x + Math.cos(ang) * 2.1D, origin.y + 0.45D, origin.z + Math.sin(ang) * 2.1D,
                            0.0D, 0.02D, 0.0D, 0.26F, 0.12F, 16, 0.10F, 0.0F, 1.0F, 0.82F, 0.22F, 0.95F);
                    if (p != null) {
                        p.configureOrbitSpiral(origin.x, origin.y + 0.5D, origin.z, 2.1D, 2.5D, ang, 0.18D, 0.03D);
                    }
                }
            }
            return;
        }

        // Fases III (Julgamento Pleno) e IV (Zênite do Sol Imortal)
        if (t == 0) {
            for (int i = 0; i < Math.min(8, ringPts); i++) {
                double ang = (2.0D * Math.PI * i) / 8.0D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.RA_SOLAR_RUNE.get(),
                        origin.x + Math.cos(ang) * 1.4D, origin.y + 0.15D, origin.z + Math.sin(ang) * 1.4D,
                        0.0D, 0.03D, 0.0D, 0.24F, 0.10F, 18, 0.12F, 0.0F, 0.99F, 0.80F, 0.22F, 0.96F);
                if (p != null) {
                    p.configureOrbitSpiral(origin.x, origin.y + 0.2D, origin.z, 1.45D, 0.55D, ang, 0.20D, 0.06D);
                }
            }
            float eyeSize = Math.max(0.85F, VfxQualityProfile.computeBillboardSize(tier, fx.profile) * (phase == 3 ? 1.25F : 1.0F));
            spawn(mc, fx, ModParticles.RA_EYE_GLYPH.get(), origin.x, origin.y + 2.75D, origin.z,
                    0.0D, 0.01D, 0.0D, eyeSize * 0.72F, eyeSize, 22, 0.0F, 0.0F, 1.0F, 0.96F, 0.80F, 0.98F);
            ClientRibbonRenderer.addArcRibbon(origin, 1.8D, 0.0D, Math.PI * 2.0D, 0.35D, 0.0D, 0.09F, 14, 1.0F, 0.84F, 0.25F, 0.88F);
            if (phase == 3) {
                ClientRibbonRenderer.addHelixRibbon(origin, 2.4D, origin.y + 0.2D, origin.y + 3.2D, 0.0D, Math.PI * 3.0D, 0.12F, 20, 0.90F, 0.65F, 1.0F, 0.86F);
            }
        } else if (t == 3 || t == 6) {
            double r = (t == 3) ? radius * 0.55D : radius * 0.92D;
            ClientRibbonRenderer.addArcRibbon(origin, r * 0.85D, t * 0.4D, Math.PI * 1.6D, 0.55D, 0.15D, 0.12F, 12, 1.0F, 0.74F, 0.16F, 0.90F);
            for (int i = 0; i < ringPts; i++) {
                double ang = (2.0D * Math.PI * i) / ringPts + t * 0.2D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(),
                        origin.x, origin.y + 0.4D, origin.z, 0.0D, 0.0D, 0.0D, 0.26F, 0.08F, 14, 0.10F, 0.0F, 0.99F, 0.76F, 0.15F, 0.94F);
                if (p != null) {
                    p.configureRadialEaseOut(origin.x, origin.y + 0.4D, origin.z,
                            origin.x + Math.cos(ang) * r, origin.y + 0.5D, origin.z + Math.sin(ang) * r, 2.2F);
                }
            }
        } else if (t == 8) {
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            Vec3 eyeTop = origin.add(0.0D, 2.5D, 0.0D);
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                Vec3 dst = new Vec3(spot.x(), spot.y() + 0.9D, spot.z());
                ClientRibbonRenderer.addBeamRibbon(eyeTop, dst, phase == 3 ? 0.13F : 0.09F, 12, 1.0F, 0.82F, 0.28F, 0.94F, 0.45F);
                for (int h = 0; h < (phase == 3 ? 4 : 3); h++) {
                    spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(),
                            spot.x(), spot.y() + h * 0.70D, spot.z(),
                            0.0D, 0.08D, 0.0D, 0.30F, 0.07F, 14, 0.12F, -0.02F, 1.0F, 0.82F, 0.24F, 0.95F);
                }
            }
        } else if ((t == 15 || t == 22) && fx.profile.enableSecondaryArcs()) {
            for (int i = 0; i < 6; i++) {
                double ang = t * 0.4D + (2.0D * Math.PI * i) / 6.0D;
                spawnSway(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        origin.x + Math.cos(ang) * 1.4D, origin.y + 1.2D, origin.z + Math.sin(ang) * 1.4D,
                        0.0D, 0.04D, 0.0D, 0.20F, 0.05F, 14, 0.05F, -0.01F, 1.0F, 0.86F, 0.35F, 0.72F, 0.010F);
            }
        }
    }

    private static void tickMorriganReap(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        int phase = Mth.clamp(fx.packet.getTierOrdinal(), 0, 3);
        Vec3 origin = fx.origin;
        double radius = Math.max(3.0D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (phase == 0) {
            // H1 · Corte do Presságio (Fase I: meia-lua frontal estreita + poucas penas)
            if (t == 0) {
                Vec3 look = fx.horizontalLook();
                double baseAng = Math.atan2(look.z, look.x);
                ClientRibbonRenderer.addArcRibbon(origin, 2.2D, baseAng - 0.75D, 1.50D, 1.05D, 0.0D, 0.13F, 10, 0.92F, 0.14F, 0.22F, 0.94F);
                for (int i = 0; i < 4; i++) {
                    double a = baseAng + (i - 1.5D) * 0.35D;
                    spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                            origin.x + Math.cos(a) * 1.6D, origin.y + 1.0D, origin.z + Math.sin(a) * 1.6D,
                            Math.cos(a) * 0.06D, 0.03D, Math.sin(a) * 0.06D,
                            0.22F, 0.12F, 14, 0.08F, 0.02F, 0.95F, 0.25F, 0.30F, 0.92F, 0.010F);
                }
            }
            return;
        }

        if (phase == 1) {
            // H2 · Voo dos Corvos (Fase II: enxame linear de penas/orbes para frente)
            if (t == 0 || t == 4) {
                Vec3 look = fx.horizontalLook();
                Vec3 right = new Vec3(-look.z, 0.0D, look.x);
                Vec3 start = origin.add(0.0D, 1.1D, 0.0D);
                Vec3 end = start.add(look.scale(9.0D));
                ClientRibbonRenderer.addBeamRibbon(start, end, 0.14F, 12, 0.78F, 0.10F, 0.22F, 0.90F, 0.20F);
                for (int i = -2; i <= 2; i++) {
                    Vec3 dst = end.add(right.scale(i * 0.55D));
                    MythicParticleBase p = spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                            start.x, start.y, start.z, 0.0D, 0.0D, 0.0D,
                            0.28F, 0.18F, 16, 0.14F, 0.0F, 0.92F, 0.18F, 0.26F, 0.96F, 0.012F);
                    if (p != null) {
                        p.configureRadialEaseOut(start.x, start.y, start.z, dst.x, dst.y, dst.z, 1.8F);
                    }
                }
            }
            return;
        }

        // Fases III (Ceifa Abissal 360°) e IV (Rainha dos Campos de Batalha)
        if (t == 0) {
            for (int i = 0; i < Math.min(10, ringPts); i++) {
                double ang = (2.0D * Math.PI * i) / 10.0D;
                MythicParticleBase p = spawnSway(mc, fx, ModParticles.MORRIGAN_FEATHER.get(),
                        origin.x + Math.cos(ang) * 1.2D, origin.y + 0.5D, origin.z + Math.sin(ang) * 1.2D,
                        0.0D, 0.05D, 0.0D, 0.26F, 0.16F, 22, 0.08F, 0.02F, 0.92F, 0.35F, 0.45F, 0.95F, 0.012F);
                if (p != null) {
                    p.configureOrbitSpiral(origin.x, origin.y + 0.6D, origin.z, 1.1D, 2.1D, ang, -0.18D, 0.05D);
                }
            }
            if (phase == 3 && fx.profile.enableSecondaryArcs()) {
                Vec3 look = fx.horizontalLook();
                Vec3 right = new Vec3(-look.z, 0.0D, look.x);
                for (int side : new int[]{-1, 1}) {
                    Vec3 tip = origin.add(look.scale(-0.6D)).add(right.scale(side * 2.4D)).add(0.0D, 2.5D, 0.0D);
                    ClientRibbonRenderer.addBeamRibbon(origin.add(0.0D, 1.3D, 0.0D), tip, 0.18F, 22, 0.78F, 0.08F, 0.20F, 0.92F, 0.45F);
                }
            }
        } else if (t == 3 || t == 6) {
            double r = (t == 3) ? radius * 0.52D : radius * 0.88D;
            ClientRibbonRenderer.addArcRibbon(origin, r, t * 0.3D, Math.PI * 1.85D, 1.0D, -0.15D,
                    phase == 3 ? 0.20F : 0.16F, 10, 0.88F, 0.12F, 0.22F, 0.94F);
            int slashCount = Math.max(8, ringPts / 2);
            for (int i = 0; i < slashCount; i++) {
                Vec3 pt = VfxQualityProfile.circleRingPoint(origin, r, i, slashCount, t * 0.25D);
                spawn(mc, fx, ModParticles.MORRIGAN_SLASH_ARC.get(),
                        pt.x, pt.y + 0.95D, pt.z, 0.0D, 0.01D, 0.0D,
                        0.55F, 0.85F, 10, 0.20F, 0.0F, 0.95F, 0.20F, 0.25F, 0.95F);
            }
        } else if (t == 7) {
            Vec3 chest = origin.add(0.0D, 1.15D, 0.0D);
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                Vec3 start = new Vec3(spot.x(), spot.y() + 0.8D, spot.z());
                Vec3 ctrl = start.lerp(chest, 0.5D).add(0.0D, 1.45D, 0.0D);
                ClientRibbonRenderer.addBeamRibbon(start, chest, 0.08F, 14, 0.86F, 0.14F, 0.24F, 0.86F, 0.55F);
                MythicParticleBase p = spawn(mc, fx, ModParticles.MORRIGAN_BLOOD_ORB.get(),
                        start.x, start.y, start.z, 0.0D, 0.0D, 0.0D,
                        0.28F, 0.14F, 14, 0.06F, 0.0F, 0.96F, 0.16F, 0.24F, 0.98F);
                if (p != null) {
                    p.configureBezierFollow(start.x, start.y, start.z, ctrl.x, ctrl.y, ctrl.z, chest.x, chest.y, chest.z);
                }
            }
        }
    }

    private static void tickIndrasVajra(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        int phase = Mth.clamp(fx.packet.getTierOrdinal(), 0, 3);
        ManifestationTier tier = fx.tier;
        Vec3 origin = fx.origin;
        double radius = Math.max(4.0D, fx.packet.getRadius());
        int ringPts = fx.profile.ringPoints();

        if (phase == 0) {
            // H1 · Centelha do Vajra (Fase I: arco elétrico curto direto)
            if (t == 0) {
                Vec3 start = origin.add(0.0D, 1.25D, 0.0D);
                Vec3 end = fx.packet.getTargets().isEmpty()
                        ? start.add(fx.horizontalLook().scale(3.8D))
                        : new Vec3(fx.packet.getTargets().get(0).x(), fx.packet.getTargets().get(0).y(), fx.packet.getTargets().get(0).z());
                ClientRibbonRenderer.addBeamRibbon(start, end, 0.07F, 8, 0.45F, 0.90F, 1.0F, 0.95F, 0.10F);
                spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(), end.x, end.y, end.z,
                        0.0D, 0.03D, 0.0D, 0.36F, 0.14F, 9, 0.0F, 0.0F, 0.45F, 0.88F, 1.0F, 0.96F);
            }
            return;
        }

        if (phase == 1) {
            // H2 · Lança Trovejante (Fase II: raio perfurante 12m + saltos em cadeia)
            if (t == 0) {
                Vec3 start = origin.add(0.0D, 1.35D, 0.0D);
                List<TargetSpot> targets = fx.packet.getTargets();
                Vec3 prev = start;
                if (targets.isEmpty()) {
                    Vec3 end = start.add(fx.horizontalLook().scale(10.0D));
                    ClientRibbonRenderer.addBeamRibbon(start, end, 0.10F, 11, 0.42F, 0.88F, 1.0F, 0.96F, 0.05F);
                } else {
                    for (int i = 0; i < Math.min(3, targets.size()); i++) {
                        TargetSpot sp = targets.get(i);
                        Vec3 dst = new Vec3(sp.x(), sp.y() + 0.5D, sp.z());
                        ClientRibbonRenderer.addBeamRibbon(prev, dst, 0.10F, 11, 0.42F, 0.88F, 1.0F, 0.96F, 0.12F);
                        spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(), dst.x, dst.y, dst.z,
                                0.0D, 0.03D, 0.0D, 0.40F, 0.16F, 10, 0.0F, 0.0F, 0.50F, 0.92F, 1.0F, 0.96F);
                        prev = dst;
                    }
                }
            }
            return;
        }

        // Fases III e IV
        if (t == 0) {
            if (mc.level != null) {
                mc.level.addParticle(ParticleTypes.SONIC_BOOM, origin.x, origin.y + 1.1D, origin.z, 0.0D, 0.0D, 0.0D);
            }
            float crestSize = Math.max(0.75F, VfxQualityProfile.computeBillboardSize(tier, fx.profile) * (phase == 3 ? 1.2F : 0.85F));
            spawn(mc, fx, ModParticles.INDRA_VAJRA_CREST.get(),
                    origin.x, origin.y + (phase == 3 ? 3.2D : 2.35D), origin.z, 0.0D, 0.01D, 0.0D,
                    crestSize * 0.8F, crestSize, 18, 0.10F, 0.0F, 0.60F, 0.92F, 1.0F, 0.96F);
            ClientRibbonRenderer.addHelixRibbon(origin, 1.5D, origin.y + 0.1D, origin.y + 2.8D, 0.0D, Math.PI * 3.0D, 0.10F, 12, 0.35F, 0.85F, 1.0F, 0.92F);
        } else if (t == 3 || t == 6) {
            double r = (t == 3) ? radius * 0.50D : radius * 0.90D;
            ClientRibbonRenderer.addArcRibbon(origin, r, 0.0D, Math.PI * 2.0D, 0.65D, 0.0D, 0.14F, 10, 0.40F, 0.82F, 1.0F, 0.90F);
            for (int i = 0; i < ringPts; i++) {
                double ang = (2.0D * Math.PI * i) / ringPts;
                MythicParticleBase p = spawn(mc, fx, ModParticles.INDRA_SHOCK_RING.get(),
                        origin.x, origin.y + 0.7D, origin.z, 0.0D, 0.0D, 0.0D,
                        0.45F, 0.88F, 11, 0.0F, 0.0F, 0.35F, 0.80F, 1.0F, 0.92F);
                if (p != null) {
                    p.configureRadialEaseOut(origin.x, origin.y + 0.7D, origin.z,
                            origin.x + Math.cos(ang) * r, origin.y + 0.7D, origin.z + Math.sin(ang) * r, 2.4F);
                }
            }
        } else if (t == 7 || t == 11) {
            List<TargetSpot> targets = fx.packet.getTargets();
            int limit = Math.min(fx.profile.maxTargets(), targets.size());
            for (int idx = 0; idx < limit; idx++) {
                TargetSpot spot = targets.get(idx);
                Vec3 dst = new Vec3(spot.x(), spot.y() + 0.5D, spot.z());
                Vec3 sky = dst.add(0.0D, phase == 3 ? 7.5D : (spot.boss() ? 6.5D : 3.5D), 0.0D);
                float w = phase == 3 ? 0.16F : (spot.boss() ? 0.14F : 0.10F);
                ClientRibbonRenderer.addBeamRibbon(sky, dst, w, 11, 0.65F, 0.88F, 1.0F, 0.98F, 0.0F);
                spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                        spot.x(), spot.y() + 0.6D, spot.z(), 0.0D, 0.04D, 0.0D,
                        0.46F, 0.20F, 10, 0.0F, 0.0F, 0.45F, 0.86F, 1.0F, 0.96F);
            }
        }
    }

    private static void tickTezcatlipocaStep(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        int phase = Mth.clamp(fx.packet.getTierOrdinal(), 0, 3);
        Vec3 origin = fx.origin;
        Vec3 dir = fx.horizontalLook();

        if (phase == 0) {
            // H1 · Fenda Fumegante (Fase I: esquiva curta + rastro turquesa)
            if (t == 0) {
                Vec3 end = origin.add(dir.scale(3.2D)).add(0.0D, 0.9D, 0.0D);
                ClientRibbonRenderer.addBeamRibbon(origin.add(0.0D, 0.9D, 0.0D), end, 0.08F, 9, 0.20F, 0.88F, 0.94F, 0.90F, 0.08F);
                spawn(mc, fx, ModParticles.TEZCA_OBSIDIAN_SHARD.get(), origin.x, origin.y + 0.8D, origin.z,
                        0.0D, 0.03D, 0.0D, 0.24F, 0.08F, 10, 0.22F, 0.02F, 0.20F, 0.88F, 0.95F, 0.90F);
            }
            return;
        }

        if (phase == 1) {
            // H2 · Reflexo do Espelho (Fase II: portal fumegante cegante na origem + névoa turquesa)
            if (t == 0 || t == 4) {
                spawn(mc, fx, ModParticles.TEZCA_SMOKE_MIRROR.get(),
                        origin.x, origin.y + 1.0D, origin.z, 0.0D, 0.01D, 0.0D,
                        0.95F, 1.45F, 18, 0.06F, 0.0F, 0.22F, 0.88F, 0.94F, 0.95F);
                ClientRibbonRenderer.addArcRibbon(origin, 2.2D, 0.0D, Math.PI * 2.0D, 0.90D, 0.0D, 0.11F, 14, 0.18F, 0.85F, 0.92F, 0.88F);
            }
            return;
        }

        // Fases III e IV
        if (t == 0) {
            spawn(mc, fx, ModParticles.TEZCA_SMOKE_MIRROR.get(),
                    origin.x, origin.y + 0.9D, origin.z, 0.0D, 0.01D, 0.0D,
                    0.80F, 1.35F, 15, 0.06F, 0.0F, 1.0F, 1.0F, 1.0F, 0.95F);
            spawn(mc, fx, ModParticles.TEZCA_AFTERIMAGE_WISP.get(),
                    origin.x, origin.y + 1.0D, origin.z, 0.0D, 0.015D, 0.0D,
                    phase == 3 ? 0.98F : 0.78F, 0.65F, 18, 0.0F, -0.01F, 1.0F, 1.0F, 1.0F, 0.92F);
        } else if (t == 2) {
            Vec3 dashEnd = fx.packet.getTargets().isEmpty()
                    ? origin.add(dir.scale(phase == 3 ? 7.5D : 5.5D)).add(0.0D, 0.9D, 0.0D)
                    : new Vec3(fx.packet.getTargets().get(0).x(), fx.packet.getTargets().get(0).y(), fx.packet.getTargets().get(0).z());
            ClientRibbonRenderer.addBeamRibbon(origin.add(0.0D, 0.9D, 0.0D), dashEnd, phase == 3 ? 0.18F : 0.14F,
                    12, 0.20F, 0.90F, 0.95F, 0.94F, 0.20F);
            ClientRibbonRenderer.addArcRibbon(dashEnd, 1.8D, 0.0D, Math.PI * 1.4D, 0.0D, 0.15D, 0.12F, 10, 0.15F, 0.85F, 0.90F, 0.88F);
        } else if (t == 4 || t == 7 || t == 11) {
            double dist = (t / 2.2D) * 1.45D;
            Vec3 trailPt = origin.add(dir.scale(dist)).add(0.0D, 0.85D, 0.0D);
            for (int k = 0; k < 3; k++) {
                double ox = (k - 1) * 0.25D;
                spawn(mc, fx, ModParticles.TEZCA_OBSIDIAN_SHARD.get(),
                        trailPt.x + ox, trailPt.y + k * 0.15D, trailPt.z - ox,
                        -dir.x * 0.05D, 0.03D, -dir.z * 0.05D,
                        0.24F, 0.08F, 12, 0.22F, 0.02F, 0.20F, 0.88F, 0.95F, 0.90F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. SUBJUGAÇÃO ESPIRITUAL (CORRENTES, RUNAS E SIGILOS)
    // -------------------------------------------------------------------------
    private static void tickSubjugationStart(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        Vec3 origin = fx.origin;
        TargetSpot spot = fx.packet.getTargets().isEmpty() ? null : fx.packet.getTargets().get(0);
        Vec3 targetPos = spot != null ? new Vec3(spot.x(), spot.y() + 0.9D, spot.z()) : origin.add(fx.horizontalLook().scale(5.0D)).add(0.0D, 1.0D, 0.0D);

        if (t == 0) {
            spawn(mc, fx, ModParticles.SUBJUGATION_RUNE_RING.get(),
                    targetPos.x, targetPos.y + 0.15D, targetPos.z, 0.0D, 0.01D, 0.0D,
                    0.95F, 1.35F, 20, 0.10F, 0.0F, 0.95F, 0.80F, 1.0F, 0.96F);
            ClientRibbonRenderer.addBeamRibbon(origin.add(0.0D, 1.15D, 0.0D), targetPos, 0.10F, 16, 0.78F, 0.35F, 1.0F, 0.92F, 0.40F);
        } else if (t % 3 == 0 && t <= 12) {
            Vec3 casterChest = origin.add(0.0D, 1.15D, 0.0D);
            Vec3 mid = casterChest.lerp(targetPos, 0.5D).add(0.0D, 0.55D, 0.0D);
            MythicParticleBase link = spawn(mc, fx, ModParticles.SUBJUGATION_CHAIN_LINK.get(),
                    casterChest.x, casterChest.y, casterChest.z, 0.0D, 0.0D, 0.0D,
                    0.30F, 0.22F, 14, 0.16F, 0.0F, 0.88F, 0.62F, 1.0F, 0.95F);
            if (link != null) {
                link.configureBezierFollow(casterChest.x, casterChest.y, casterChest.z, mid.x, mid.y, mid.z, targetPos.x, targetPos.y, targetPos.z);
            }
        }
    }

    private static void tickSubjugationPulse(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        TargetSpot spot = fx.packet.getTargets().isEmpty() ? null : fx.packet.getTargets().get(0);
        Vec3 center = spot != null ? new Vec3(spot.x(), spot.y() + 0.9D, spot.z()) : fx.origin.add(fx.horizontalLook().scale(4.0D)).add(0.0D, 1.0D, 0.0D);
        Vec3 casterChest = fx.origin.add(0.0D, 1.15D, 0.0D);

        if (t == 0) {
            ClientRibbonRenderer.addBeamRibbon(casterChest, center, 0.08F, 12, 0.76F, 0.32F, 0.98F, 0.88F, 0.30F);
            for (int i = 0; i < 6; i++) {
                double ang = (2.0D * Math.PI * i) / 6.0D;
                MythicParticleBase link = spawn(mc, fx, ModParticles.SUBJUGATION_CHAIN_LINK.get(),
                        center.x + Math.cos(ang) * 1.6D, center.y, center.z + Math.sin(ang) * 1.6D,
                        0.0D, 0.0D, 0.0D, 0.28F, 0.18F, 14, 0.18F, 0.0F,
                        0.92F, 0.65F, 1.0F, 0.92F);
                if (link != null) {
                    link.configureOrbitSpiral(center.x, center.y, center.z, 1.6D, 0.85D, ang, 0.25D, 0.02D);
                }
            }
        }
    }

    private static void tickSubjugationSuccess(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        TargetSpot spot = fx.packet.getTargets().isEmpty() ? null : fx.packet.getTargets().get(0);
        Vec3 center = spot != null ? new Vec3(spot.x(), spot.y() + 0.9D, spot.z()) : fx.origin.add(0.0D, 1.2D, 0.0D);

        if (t == 0) {
            spawn(mc, fx, ModParticles.SUBJUGATION_SIGIL.get(),
                    center.x, center.y + 0.5D, center.z, 0.0D, 0.02D, 0.0D,
                    0.95F, 0.75F, 24, 0.08F, -0.01F, 1.0F, 0.90F, 0.55F, 1.0F);
            ClientRibbonRenderer.addHelixRibbon(center, 1.4D, center.y - 0.6D, center.y + 1.8D, 0.0D, Math.PI * 3.5D, 0.12F, 18, 0.95F, 0.78F, 1.0F, 0.95F);
            ClientRibbonRenderer.addArcRibbon(center, 1.9D, 0.0D, Math.PI * 2.0D, 0.0D, 0.0D, 0.14F, 14, 0.98F, 0.85F, 0.40F, 0.92F);
        } else if (t == 4 || t == 9) {
            int pts = Math.max(8, fx.profile.ringPoints() / 2);
            for (int i = 0; i < pts; i++) {
                double ang = (2.0D * Math.PI * i) / pts + t * 0.2D;
                spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        center.x + Math.cos(ang) * 1.4D, center.y + 0.1D, center.z + Math.sin(ang) * 1.4D,
                        0.0D, 0.08D, 0.0D, 0.24F, 0.06F, 14, 0.10F, -0.02F,
                        0.98F, 0.86F, 0.45F, 0.90F);
            }
        }
    }

    private static void tickSubjugationFail(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks != 0) {
            return;
        }
        TargetSpot spot = fx.packet.getTargets().isEmpty() ? null : fx.packet.getTargets().get(0);
        Vec3 center = spot != null ? new Vec3(spot.x(), spot.y() + 0.9D, spot.z()) : fx.origin.add(0.0D, 1.0D, 0.0D);
        for (int i = 0; i < 12; i++) {
            double ang = (2.0D * Math.PI * i) / 12.0D;
            spawn(mc, fx, ModParticles.SUBJUGATION_CHAIN_LINK.get(),
                    center.x, center.y, center.z,
                    Math.cos(ang) * 0.18D, 0.07D, Math.sin(ang) * 0.18D,
                    0.26F, 0.08F, 14, 0.30F, 0.05F, 0.95F, 0.25F, 0.38F, 0.92F);
        }
    }

    private static void tickSubjugationSummon(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0) {
            spawn(mc, fx, ModParticles.SUBJUGATION_SIGIL.get(),
                    fx.origin.x, fx.origin.y + 0.4D, fx.origin.z, 0.0D, 0.03D, 0.0D,
                    0.85F, 0.55F, 18, 0.10F, 0.0F, 0.85F, 0.55F, 1.0F, 0.96F);
            ClientRibbonRenderer.addHelixRibbon(fx.origin, 1.1D, fx.origin.y - 0.5D, fx.origin.y + 1.6D, 0.0D, Math.PI * 2.5D, 0.10F, 14, 0.78F, 0.45F, 1.0F, 0.90F);
        } else if (t == 3) {
            for (int i = 0; i < 8; i++) {
                double ang = (2.0D * Math.PI * i) / 8.0D;
                spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x + Math.cos(ang) * 0.9D, fx.origin.y, fx.origin.z + Math.sin(ang) * 0.9D,
                        0.0D, 0.09D, 0.0D, 0.22F, 0.05F, 12, 0.05F, -0.02F,
                        0.82F, 0.55F, 1.0F, 0.90F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. AS 11 TÉCNICAS ATIVAS DE CULTIVO (CORPO, ESPÍRITO & HÍBRIDO)
    // -------------------------------------------------------------------------
    private static void tickGaleStep(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks == 0) {
            Vec3 look = fx.horizontalLook();
            ClientRibbonRenderer.addBeamRibbon(fx.origin.add(0, 0.8, 0), fx.origin.add(look.scale(3.6D)).add(0, 1.0, 0), 0.12F, 10, 0.85F, 0.32F, 0.28F, 0.90F, 0.15F);
            for (int i = 0; i < 8; i++) {
                double ang = (2.0D * Math.PI * i) / 8.0D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x + Math.cos(ang) * 0.8D, fx.origin.y + 0.5D, fx.origin.z + Math.sin(ang) * 0.8D,
                        -look.x * 0.12D, 0.04D, -look.z * 0.12D, 0.22F, 0.06F, 12, 0.12F, 0.0F,
                        0.96F, 0.42F, 0.34F, 0.88F);
                if (p != null) {
                    p.configureOrbitSpiral(fx.origin.x, fx.origin.y + 0.6D, fx.origin.z, 0.9D, 1.8D, ang, 0.28D, 0.04D);
                }
            }
        }
    }

    private static void tickInnerBreath(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0 || t == 6 || t == 12) {
            ClientRibbonRenderer.addArcRibbon(fx.origin, 1.15D, t * 0.5D, Math.PI * 1.7D, 0.45D + t * 0.06D, 0.20D, 0.08F, 14, 0.72F, 0.42F, 0.98F, 0.86F);
            for (int i = 0; i < 6; i++) {
                double ang = (2.0D * Math.PI * i) / 6.0D + t * 0.2D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x + Math.cos(ang) * 1.3D, fx.origin.y + 0.3D, fx.origin.z + Math.sin(ang) * 1.3D,
                        0.0D, 0.04D, 0.0D, 0.22F, 0.08F, 16, 0.05F, -0.01F,
                        0.82F, 0.56F, 1.0F, 0.90F);
                if (p != null) {
                    p.configureOrbitSpiral(fx.origin.x, fx.origin.y + 0.4D, fx.origin.z, 1.35D, 0.55D, ang, 0.16D, 0.05D);
                }
            }
        }
    }

    private static void tickJadeGuard(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks == 0) {
            ClientRibbonRenderer.addArcRibbon(fx.origin, 1.1D, 0.0D, Math.PI * 2.0D, 0.95D, 0.0D, 0.11F, 16, 0.20F, 0.88F, 0.56F, 0.92F);
            for (int i = 0; i < 10; i++) {
                double ang = (2.0D * Math.PI * i) / 10.0D;
                spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x + Math.cos(ang) * 1.1D, fx.origin.y + 0.95D, fx.origin.z + Math.sin(ang) * 1.1D,
                        0.0D, 0.02D, 0.0D, 0.24F, 0.10F, 14, 0.10F, 0.0F,
                        0.28F, 0.92F, 0.60F, 0.94F);
            }
        }
    }

    private static void tickSeismicPalm(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0) {
            Vec3 look = fx.horizontalLook();
            Vec3 dst = fx.origin.add(look.scale(5.5D)).add(0.0D, 0.7D, 0.0D);
            ClientRibbonRenderer.addBeamRibbon(fx.origin.add(0, 1.1, 0), dst, 0.20F, 12, 0.96F, 0.35F, 0.18F, 0.95F, 0.10F);
            for (int i = 0; i < 12; i++) {
                double spread = (i - 5.5D) * 0.12D;
                Vec3 dir = new Vec3(look.x * Math.cos(spread) - look.z * Math.sin(spread), 0.05D, look.x * Math.sin(spread) + look.z * Math.cos(spread));
                MythicParticleBase p = spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x, fx.origin.y + 0.85D, fx.origin.z, 0.0D, 0.0D, 0.0D,
                        0.32F, 0.12F, 12, 0.15F, 0.0F, 0.98F, 0.40F, 0.18F, 0.96F);
                if (p != null) {
                    p.configureRadialEaseOut(fx.origin.x, fx.origin.y + 0.85D, fx.origin.z,
                            fx.origin.x + dir.x * 5.8D, fx.origin.y + 0.65D, fx.origin.z + dir.z * 5.8D, 2.2F);
                }
            }
        }
    }

    private static void tickGoldenAegis(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0 || t == 6) {
            double h = t == 0 ? 0.45D : 1.35D;
            ClientRibbonRenderer.addArcRibbon(fx.origin, 1.35D, t * 0.4D, Math.PI * 2.0D, h, 0.0D, 0.10F, 16, 0.98F, 0.82F, 0.24F, 0.90F);
            for (int i = 0; i < 8; i++) {
                double ang = (2.0D * Math.PI * i) / 8.0D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.RA_SOLAR_RUNE.get(),
                        fx.origin.x + Math.cos(ang) * 1.35D, fx.origin.y + h, fx.origin.z + Math.sin(ang) * 1.35D,
                        0.0D, 0.01D, 0.0D, 0.26F, 0.12F, 16, 0.10F, 0.0F,
                        0.99F, 0.84F, 0.24F, 0.94F);
                if (p != null) {
                    p.configureOrbitSpiral(fx.origin.x, fx.origin.y + h, fx.origin.z, 1.35D, 1.35D, ang, 0.14D, 0.01D);
                }
            }
        }
    }

    private static void tickBloodFurnace(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks == 0 || fx.ageTicks == 5) {
            ClientRibbonRenderer.addHelixRibbon(fx.origin, 1.25D, fx.origin.y + 0.1D, fx.origin.y + 2.2D, fx.ageTicks, Math.PI * 2.5D, 0.11F, 14, 0.90F, 0.15F, 0.20F, 0.92F);
            for (int i = 0; i < 8; i++) {
                double ang = (2.0D * Math.PI * i) / 8.0D;
                spawn(mc, fx, ModParticles.MORRIGAN_BLOOD_ORB.get(),
                        fx.origin.x + Math.cos(ang) * 1.1D, fx.origin.y + 0.4D, fx.origin.z + Math.sin(ang) * 1.1D,
                        0.0D, 0.08D, 0.0D, 0.26F, 0.10F, 14, 0.08F, -0.02F,
                        0.92F, 0.14F, 0.18F, 0.94F);
            }
        }
    }

    private static void tickSoulLance(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks == 0) {
            Vec3 eye = fx.origin.add(0.0D, 1.35D, 0.0D);
            Vec3 dir = new Vec3(fx.packet.getDirX(), fx.packet.getDirY(), fx.packet.getDirZ());
            if (dir.lengthSqr() < 1.0E-4D) {
                dir = fx.horizontalLook();
            } else {
                dir = dir.normalize();
            }
            Vec3 end = eye.add(dir.scale(Math.max(8.0D, fx.packet.getRadius())));
            ClientRibbonRenderer.addBeamRibbon(eye, end, 0.14F, 14, 0.32F, 0.88F, 1.0F, 0.96F, 0.0F);
            for (int i = 0; i < 12; i++) {
                double frac = (i + 1) / 12.0D;
                Vec3 pt = eye.lerp(end, frac);
                spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        pt.x, pt.y, pt.z, 0.0D, 0.01D, 0.0D,
                        0.28F, 0.08F, 12, 0.10F, 0.0F, 0.38F, 0.90F, 1.0F, 0.95F);
            }
        }
    }

    private static void tickEtherFlash(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks == 0 || fx.ageTicks == 2) {
            Vec3 look = fx.horizontalLook();
            Vec3 start = fx.origin.add(0.0D, 1.0D, 0.0D);
            Vec3 end = fx.packet.getTargets().isEmpty()
                    ? start.add(look.scale(7.0D))
                    : new Vec3(fx.packet.getTargets().get(0).x(), fx.packet.getTargets().get(0).y() + 1.0D, fx.packet.getTargets().get(0).z());
            ClientRibbonRenderer.addBeamRibbon(start, end, 0.16F, 10, 0.45F, 0.95F, 1.0F, 0.98F, 0.15F);
            ClientRibbonRenderer.addArcRibbon(end, 1.6D, 0.0D, Math.PI * 1.4D, 0.0D, 0.25D, 0.13F, 9, 0.55F, 0.96F, 1.0F, 0.94F);
            spawn(mc, fx, ModParticles.INDRA_LIGHTNING_ARC.get(),
                    end.x, end.y, end.z, 0.0D, 0.02D, 0.0D,
                    0.50F, 0.16F, 10, 0.12F, 0.0F, 0.50F, 0.95F, 1.0F, 0.98F);
        }
    }

    private static void tickSpiritDomain(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0 || t == 5 || t == 12) {
            double r = Math.min(8.0D, Math.max(2.5D, fx.packet.getRadius()));
            ClientRibbonRenderer.addArcRibbon(fx.origin, r * 0.92D, t * 0.3D, Math.PI * 2.0D, 0.35D, 0.0D, 0.11F, 18, 0.72F, 0.38F, 0.98F, 0.88F);
            int pts = Math.max(8, fx.profile.ringPoints() / 2);
            for (int i = 0; i < pts; i++) {
                double ang = (2.0D * Math.PI * i) / pts;
                spawn(mc, fx, ModParticles.SUBJUGATION_RUNE_RING.get(),
                        fx.origin.x + Math.cos(ang) * r * 0.88D, fx.origin.y + 0.25D, fx.origin.z + Math.sin(ang) * r * 0.88D,
                        0.0D, 0.02D, 0.0D, 0.36F, 0.16F, 18, 0.08F, 0.0F,
                        0.76F, 0.45F, 1.0F, 0.90F);
            }
        }
    }

    private static void tickUndyingReversal(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        if (t == 0 || t == 6) {
            ClientRibbonRenderer.addHelixRibbon(fx.origin, 1.4D, fx.origin.y + 0.1D, fx.origin.y + 2.5D, t, Math.PI * 3.0D, 0.13F, 16, 0.96F, 0.55F, 0.92F, 0.95F);
            for (int i = 0; i < 10; i++) {
                double ang = (2.0D * Math.PI * i) / 10.0D;
                MythicParticleBase p = spawn(mc, fx, ModParticles.QI_RIBBON_WISP.get(),
                        fx.origin.x + Math.cos(ang) * 2.0D, fx.origin.y + 0.3D, fx.origin.z + Math.sin(ang) * 2.0D,
                        0.0D, 0.04D, 0.0D, 0.26F, 0.12F, 16, 0.12F, 0.0F,
                        0.98F, 0.62F, 0.95F, 0.96F);
                if (p != null) {
                    p.configureOrbitSpiral(fx.origin.x, fx.origin.y + 0.4D, fx.origin.z, 2.0D, 0.4D, ang, 0.24D, 0.08D);
                }
            }
        }
    }

    private static void tickHeavenSunder(Minecraft mc, ActiveEffect fx) {
        int t = fx.ageTicks;
        Vec3 look = fx.horizontalLook();
        if (t == 0) {
            Vec3 end = fx.origin.add(look.scale(10.0D)).add(0.0D, 1.0D, 0.0D);
            ClientRibbonRenderer.addBeamRibbon(fx.origin.add(0, 1.3, 0), end, 0.24F, 16, 1.0F, 0.84F, 0.26F, 0.98F, 0.25F);
            ClientRibbonRenderer.addArcRibbon(fx.origin, 3.2D, 0.0D, Math.PI * 2.0D, 0.65D, 0.0D, 0.16F, 16, 0.98F, 0.78F, 0.22F, 0.94F);
        } else if (t == 3 || t == 7) {
            int count = Math.min(10, fx.profile.ringPoints() / 2);
            for (int i = 0; i < count; i++) {
                double ang = (2.0D * Math.PI * i) / count;
                spawn(mc, fx, ModParticles.RA_SUN_FLARE.get(),
                        fx.origin.x + look.x * (t * 0.9D), fx.origin.y + 1.0D, fx.origin.z + look.z * (t * 0.9D),
                        Math.cos(ang) * 0.16D, 0.06D, Math.sin(ang) * 0.16D,
                        0.34F, 0.10F, 14, 0.18F, 0.0F, 1.0F, 0.85F, 0.30F, 0.96F);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 4. EVENTOS VITAIS PADRONIZADOS
    // -------------------------------------------------------------------------
    private static void tickBurst(Minecraft mc, ActiveEffect fx, float r, float g, float b, boolean withSmoke) {
        if (fx.ageTicks != 0) {
            return;
        }
        Vec3 origin = fx.origin;
        int count = Math.max(8, fx.profile.ringPoints() / 2);
        for (int i = 0; i < count; i++) {
            double ang = (2.0D * Math.PI * i) / count;
            spawn(mc, fx, ModParticles.DIVINE_FRACTURE_SPARK.get(),
                    origin.x, origin.y + 1.05D, origin.z,
                    Math.cos(ang) * 0.15D, 0.06D + (i % 3) * 0.03D, Math.sin(ang) * 0.15D,
                    0.26F, 0.06F, 14, 0.18F, 0.04F, r, g, b, 0.95F);
        }
        if (withSmoke && mc.level != null) {
            mc.level.addParticle(ParticleTypes.LARGE_SMOKE, origin.x, origin.y + 1.0D, origin.z, 0.0D, 0.04D, 0.0D);
        }
    }

    private static void tickSpiritShatter(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks != 0) {
            return;
        }
        Vec3 origin = fx.origin;
        int count = Math.max(10, fx.profile.ringPoints() / 2);
        for (int i = 0; i < count; i++) {
            double ang = (2.0D * Math.PI * i) / count;
            spawn(mc, fx, ModParticles.DIVINE_FRACTURE_SPARK.get(),
                    origin.x, origin.y + 1.1D, origin.z,
                    Math.cos(ang) * 0.17D, 0.08D, Math.sin(ang) * 0.17D,
                    0.30F, 0.07F, 16, 0.22F, 0.05F, 0.78F, 0.30F, 0.98F, 0.96F);
        }
    }

    private static void tickReject(Minecraft mc, ActiveEffect fx) {
        if (fx.ageTicks != 0) {
            return;
        }
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
        if (t == 0) {
            ClientRibbonRenderer.addHelixRibbon(fx.origin, 1.3D, fx.origin.y + 0.1D, fx.origin.y + 2.8D, 0.0D, Math.PI * 3.0D, 0.12F, 18, 0.99F, 0.82F, 0.25F, 0.95F);
        }
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
                    0.0D, 0.05D, 0.0D, 0.28F, 0.06F, 14, 0.15F, -0.02F, r, g, b, 0.96F);
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private static MythicParticleBase spawn(
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
            return null;
        }
        Particle p = mc.particleEngine.createParticle(type, x, y, z, vx, vy, vz);
        fx.spawnedParticles++;
        if (p instanceof MythicParticleBase mythic) {
            mythic.configure(startSize, endSize, lifetimeTicks, vx, vy, vz, spinSpeed, gravity, r, g, b, alpha);
            return mythic;
        }
        return null;
    }

    private static MythicParticleBase spawnSway(
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
        MythicParticleBase mythic = spawn(mc, fx, type, x, y, z, vx, vy, vz,
                startSize, endSize, lifetimeTicks, spinSpeed, gravity, r, g, b, alpha);
        if (mythic != null) {
            mythic.setSway(swayAmp, 0.35F);
        }
        return mythic;
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
            case EYE_OF_RA, MORRIGAN_REAP -> 28;
            case INDRAS_VAJRA -> 24;
            case TEZCATLIPOCA_STEP, REALM_BREAKTHROUGH_BURST, SUBJUGATION_SUCCESS,
                    SUBJUGATION_CLASH_START, SPIRIT_DOMAIN_SUPPRESSION, PRIMORDIAL_HEAVEN_SUNDER -> 20;
            case INNER_BREATH_MEDITATION, GOLDEN_CORE_AEGIS, UNDYING_MERIDIAN_REVERSAL -> 16;
            case SUBJUGATION_CLASH_TICK, SUBJUGATION_SUMMON, SEISMIC_PALM,
                    BLOOD_FURNACE_SURGE, NASCENT_SOUL_LANCE -> 12;
            case GALE_MERIDIAN_STEP, JADE_BONE_GUARD, ETHER_FLASH_STRIKE, SUBJUGATION_FAIL,
                    DIVINE_OVERLOAD_BURST, DIVINE_OVERLOAD_REJECT, DIVINE_OVERLOAD_RECOVER,
                    SPIRIT_SHATTER_BURST -> 10;
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
