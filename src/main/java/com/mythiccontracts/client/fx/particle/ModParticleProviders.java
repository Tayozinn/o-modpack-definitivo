package com.mythiccontracts.client.fx.particle;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registra os {@code SpriteSet}s dos 13 tipos de partículas customizadas do Mythic Contracts 2.0.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModParticleProviders {

    private ModParticleProviders() {
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.RA_EYE_GLYPH.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.85F, 1.15F, 18, 0.0F, 0.0F, 0.0F, true));
        event.registerSpriteSet(ModParticles.RA_SOLAR_RUNE.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.28F, 0.14F, 16, 0.12F, -0.02F, 0.0F, true));
        event.registerSpriteSet(ModParticles.RA_SUN_FLARE.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.24F, 0.06F, 14, 0.06F, -0.04F, 0.0F, true));

        event.registerSpriteSet(ModParticles.MORRIGAN_FEATHER.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.24F, 0.18F, 26, 0.08F, 0.08F, 0.008F, true));
        event.registerSpriteSet(ModParticles.MORRIGAN_SLASH_ARC.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.60F, 0.90F, 10, 0.18F, 0.0F, 0.0F, true));
        event.registerSpriteSet(ModParticles.MORRIGAN_BLOOD_ORB.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.22F, 0.12F, 12, 0.04F, 0.0F, 0.0F, true));

        event.registerSpriteSet(ModParticles.INDRA_LIGHTNING_ARC.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.36F, 0.22F, 10, 0.0F, 0.0F, 0.0F, true));
        event.registerSpriteSet(ModParticles.INDRA_VAJRA_CREST.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.75F, 1.05F, 18, 0.10F, 0.0F, 0.0F, true));
        event.registerSpriteSet(ModParticles.INDRA_SHOCK_RING.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.55F, 0.95F, 11, 0.0F, 0.0F, 0.0F, true));

        event.registerSpriteSet(ModParticles.TEZCA_OBSIDIAN_SHARD.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.22F, 0.10F, 15, 0.22F, 0.03F, 0.0F, true));
        event.registerSpriteSet(ModParticles.TEZCA_SMOKE_MIRROR.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.65F, 1.10F, 16, 0.05F, 0.0F, 0.0F, true));
        event.registerSpriteSet(ModParticles.TEZCA_AFTERIMAGE_WISP.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.75F, 0.60F, 18, 0.0F, -0.02F, 0.0F, true));

        event.registerSpriteSet(ModParticles.DIVINE_FRACTURE_SPARK.get(),
                sprites -> new MythicParticleBase.Provider(sprites, 0.24F, 0.06F, 14, 0.15F, 0.02F, 0.0F, true));
    }
}
