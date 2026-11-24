package com.mythiccontracts.client.sound;

import com.mythiccontracts.client.ClientRuntimeState;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import com.mythiccontracts.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Controlador de áudio cliente do Mythic Contracts 2.0:
 * garante throttle de polifonia (máx. 4 simultâneos / cooldown >= 60ms por chave),
 * respeito ao volume/toggle do usuário e ciclo de vida do loop de Disputa de Subjugação.
 */
public final class ClientSoundController {

    private static final long MIN_KEY_INTERVAL_MS = 60L;
    private static final long POLYPHONY_WINDOW_MS = 160L;
    private static final int MAX_CONCURRENT_SOUNDS = 4;

    private static final Map<String, Long> LAST_PLAYED_BY_KEY = new HashMap<>();
    private static long activeBurstWindowStartMs = 0L;
    private static int activeBurstCount = 0;

    @Nullable
    private static SubjugationClashSoundInstance clashLoopInstance;

    private ClientSoundController() {
    }

    public static void playUiSound(SoundEvent sound, float volume, float pitch) {
        if (!ClientHudConfig.enableCustomSounds || sound == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float scaled = Mth.clamp(volume * ClientHudConfig.skillSoundVolume, 0.0F, 1.5F);
        if (scaled <= 0.005F) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, scaled));
    }

    public static void playThrottledWorldCue(String key, SoundEvent sound, double x, double y, double z, float baseVolume, float pitch) {
        if (!ClientHudConfig.enableCustomSounds || sound == null || key == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_PLAYED_BY_KEY.get(key);
        if (last != null && (now - last) < MIN_KEY_INTERVAL_MS) {
            return;
        }
        if (now - activeBurstWindowStartMs > POLYPHONY_WINDOW_MS) {
            activeBurstWindowStartMs = now;
            activeBurstCount = 0;
        }
        if (activeBurstCount >= MAX_CONCURRENT_SOUNDS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float scaled = Mth.clamp(baseVolume * ClientHudConfig.skillSoundVolume, 0.0F, 1.5F);
        if (scaled <= 0.005F) {
            return;
        }

        LAST_PLAYED_BY_KEY.put(key, now);
        activeBurstCount++;
        if (LAST_PLAYED_BY_KEY.size() > 64) {
            pruneOldKeys(now);
        }
        mc.level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, scaled, pitch, false);
    }

    public static void onVisualEffect(SpawnVisualEffectS2CPacket packet) {
        if (!ClientHudConfig.enableCustomSounds || packet == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        // Limita acentuação local a efeitos próximos ao jogador local para evitar excesso sonoro no servidor
        double distSq = mc.player.distanceToSqr(packet.getOriginX(), packet.getOriginY(), packet.getOriginZ());
        if (distSq > 26.0D * 26.0D) {
            return;
        }
        EffectType type = packet.getEffectType();
        if (type == EffectType.SUBJUGATION_CLASH_START) {
            ensureClashLoopStarted(mc);
        } else if (type == EffectType.SUBJUGATION_SUCCESS || type == EffectType.SUBJUGATION_FAIL) {
            stopClashLoop();
        }
    }

    public static void tickClashLoop(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || !ClientHudConfig.enableCustomSounds) {
            stopClashLoop();
            return;
        }
        if (ClientRuntimeState.isClashActive()) {
            ensureClashLoopStarted(mc);
        } else {
            stopClashLoop();
        }
    }

    private static void ensureClashLoopStarted(Minecraft mc) {
        if (clashLoopInstance != null && !clashLoopInstance.isStopped()) {
            return;
        }
        clashLoopInstance = new SubjugationClashSoundInstance();
        mc.getSoundManager().play(clashLoopInstance);
    }

    public static void stopClashLoop() {
        if (clashLoopInstance != null) {
            clashLoopInstance.stopImmediately();
            clashLoopInstance = null;
        }
    }

    public static void clear() {
        LAST_PLAYED_BY_KEY.clear();
        activeBurstCount = 0;
        stopClashLoop();
    }

    private static void pruneOldKeys(long now) {
        Iterator<Map.Entry<String, Long>> it = LAST_PLAYED_BY_KEY.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > 5000L) {
                it.remove();
            }
        }
    }
}
