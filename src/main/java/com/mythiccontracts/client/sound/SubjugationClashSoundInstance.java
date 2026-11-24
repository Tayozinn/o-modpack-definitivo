package com.mythiccontracts.client.sound;

import com.mythiccontracts.client.ClientRuntimeState;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Instância contínua em loop para a Disputa de Subjugação Espiritual.
 * Ajusta pitch e volume dinamicamente com a Dominância e encerra assim que a disputa termina.
 */
public final class SubjugationClashSoundInstance extends AbstractTickableSoundInstance {

    public SubjugationClashSoundInstance() {
        super(ModSounds.SUBJUGATION_CLASH_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.25F;
        this.pitch = 0.90F;
        this.relative = true;
    }

    @Override
    public boolean canPlaySound() {
        return ClientHudConfig.enableCustomSounds && ClientRuntimeState.isClashActive();
    }

    public void stopImmediately() {
        this.stop();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || !ClientHudConfig.enableCustomSounds
                || !ClientRuntimeState.isClashActive()) {
            this.stop();
            return;
        }
        float dominance01 = Mth.clamp(ClientRuntimeState.getClashProgress(), 0.0F, 1.0F);
        float userScale = Mth.clamp(ClientHudConfig.skillSoundVolume, 0.0F, 1.5F);
        float revoltBoost = ClientRuntimeState.isClashRevoltWindow() ? 0.14F : 0.0F;
        this.volume = Mth.clamp((0.22F + dominance01 * 0.28F + revoltBoost) * userScale, 0.01F, 1.0F);
        this.pitch = 0.86F + dominance01 * 0.34F + revoltBoost;
    }
}
