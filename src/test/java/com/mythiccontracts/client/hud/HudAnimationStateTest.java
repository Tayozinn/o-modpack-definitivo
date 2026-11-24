package com.mythiccontracts.client.hud;

import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HudAnimationStateTest {

    @Test
    void snapsImmediatelyWhenSmoothAnimationsDisabled() {
        HudAnimationState state = new HudAnimationState();
        UUID id = UUID.randomUUID();

        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, false);
        state.updateSnapshot(id, 0.40F, 0.50F, 0.30F, 0.60F, 0.25F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.02F, false);

        assertEquals(0.40F, state.getBodyHealthTrack().getDisplayedRatio(), 0.0001F);
        assertEquals(0.40F, state.getBodyHealthTrack().getGhostRatio(), 0.0001F);
        assertEquals(0.0F, state.getDamageShakeSeconds(), 0.0001F);
        assertEquals(0.0F, state.getDamageShakeOffsetX(false), 0.0001F);
    }

    @Test
    void holdsGhostBarThenDecaysAfterDamageAndTriggersMicroShake() {
        HudAnimationState state = new HudAnimationState();
        UUID id = UUID.randomUUID();

        // Sincronização inicial em 100%
        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);
        assertEquals(0.0F, state.getDamageShakeSeconds(), 0.0001F);

        // Queda abrupta de 35% na Vida Corporal
        state.updateSnapshot(id, 0.65F, 1.0F, 1.0F, 1.0F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);

        assertEquals(HudAnimationState.DAMAGE_SHAKE_DURATION, state.getDamageShakeSeconds(), 0.001F);
        assertTrue(state.getBodyHealthTrack().getDisplayedRatio() < 1.0F);
        assertTrue(state.getBodyHealthTrack().getDisplayedRatio() > 0.65F);
        assertEquals(1.0F, state.getBodyHealthTrack().getGhostRatio(), 0.001F);
        assertTrue(state.getBodyHealthTrack().getGhostHoldSeconds() > 0.20F);

        // Avança 0.20s ainda dentro do hold (ghost ainda mantido próximo a 1.0)
        for (int i = 0; i < 4; i++) {
            state.updateSnapshot(id, 0.65F, 1.0F, 1.0F, 1.0F, 1.0F,
                    1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);
        }
        assertEquals(0.0F, state.getDamageShakeSeconds(), 0.001F);
        assertEquals(1.0F, state.getBodyHealthTrack().getGhostRatio(), 0.001F);

        // Avança mais 0.30s após o hold: ghost decai em direção a 0.65
        for (int i = 0; i < 6; i++) {
            state.updateSnapshot(id, 0.65F, 1.0F, 1.0F, 1.0F, 1.0F,
                    1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);
        }
        float ghostAfterDecay = state.getBodyHealthTrack().getGhostRatio();
        assertTrue(ghostAfterDecay < 0.90F && ghostAfterDecay >= 0.65F,
                "Ghost ratio should decay smoothly, got " + ghostAfterDecay);
    }

    @Test
    void tracksStaminaFullDurationAndResetsOnSpend() {
        HudAnimationState state = new HudAnimationState();
        UUID id = UUID.randomUUID();

        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);
        assertEquals(0.0F, state.getStaminaFullSeconds(), 0.001F);

        for (int i = 0; i < 32; i++) {
            state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                    1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.10F, true);
        }
        assertTrue(state.getStaminaFullSeconds() >= 3.0F);

        // Gastar vigor zera o cronômetro de auto-ocultar
        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 0.85F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);
        assertEquals(0.0F, state.getStaminaFullSeconds(), 0.001F);
    }

    @Test
    void triggersEvolutionBannerOnlyOnUpgradesAfterInitialSyncAndClearsOnReset() {
        HudAnimationState state = new HudAnimationState();
        UUID id = UUID.randomUUID();

        // Primeira carga não dispara banner nem flash
        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                3, 2, "Tempero de Ferro", 2, 1, "Condensação de Qi", ManifestationTier.TIER_2, 0.05F, true);
        assertEquals(0.0F, state.getBannerRemainingSeconds(), 0.001F);

        // Avanço de estágio corporal dispara banner
        state.updateSnapshot(id, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F,
                3, 3, "Tempero de Ferro", 2, 1, "Condensação de Qi", ManifestationTier.TIER_2, 0.05F, true);
        assertEquals(HudAnimationState.BANNER_DURATION, state.getBannerRemainingSeconds(), 0.001F);
        assertTrue(state.getBannerText().contains("R3-3"));
        assertTrue(state.getBorderFlashSeconds() > 1.0F);

        // Reset limpa completamente
        state.reset();
        assertEquals(0.0F, state.getBannerRemainingSeconds(), 0.001F);
        assertEquals("", state.getBannerText());
        assertEquals(0.0F, state.getClashDominanceTrack().getDisplayedRatio(), 0.001F);
    }

    @Test
    void tracksSpiritShimmerCombatFlashFullPulseAndClashBarInterpolation() {
        HudAnimationState state = new HudAnimationState();
        UUID id = UUID.randomUUID();
        state.updateSnapshot(id, 1.0F, 0.50F, 1.0F, 1.0F, 1.0F,
                1, 1, "Corpo Mortal", 1, 1, "Centelha Espiritual", ManifestationTier.TIER_1, 0.05F, true);

        // Shimmer avanca mais rapido em meditacao do que em recuperacao comum
        state.updateSpiritualState(com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState.RECOVERING,
                0.50F, 0.10F, true);
        float recoveringPhase = state.getSpiritShimmerPhase();
        assertTrue(recoveringPhase > 0.0F);

        state.updateSpiritualState(com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState.MEDITATING,
                0.60F, 0.10F, true);
        assertTrue(state.getSpiritShimmerPhase() > recoveringPhase);

        // Entrada em COMBAT_SLOW dispara flash de borda 0.18s
        state.updateSpiritualState(com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState.COMBAT_SLOW,
                0.60F, 0.05F, true);
        assertEquals(HudAnimationState.SPIRIT_COMBAT_FLASH_DURATION, state.getSpiritCombatFlashSeconds(), 0.001F);

        // Ao atingir 100% (FULL), dispara pulso unico 0.35s
        state.updateSpiritualState(com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState.FULL,
                1.00F, 0.05F, true);
        assertEquals(HudAnimationState.SPIRIT_FULL_PULSE_DURATION, state.getSpiritFullPulseSeconds(), 0.001F);

        // Barra efemera de Disputa interpola suavemente e zera quando encerra
        state.updateClashState(true, 0.65F, 0.05F, true);
        float mid = state.getClashDominanceTrack().getDisplayedRatio();
        assertTrue(mid > 0.0F && mid < 0.65F, "Clash ratio should interpolate toward 0.65, got " + mid);
        state.updateClashState(false, 0.0F, 0.05F, true);
        assertEquals(0.0F, state.getClashDominanceTrack().getDisplayedRatio(), 0.0001F);
    }
}
