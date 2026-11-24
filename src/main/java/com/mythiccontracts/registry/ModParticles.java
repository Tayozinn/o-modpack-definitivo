package com.mythiccontracts.registry;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro central de tipos de partículas customizadas do Mythic Contracts 2.0.
 * Não possui dependências cliente para permanecer seguro no servidor dedicado.
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MythicContractsMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> RA_EYE_GLYPH = registerSimple("ra_eye_glyph");
    public static final RegistryObject<SimpleParticleType> RA_SOLAR_RUNE = registerSimple("ra_solar_rune");
    public static final RegistryObject<SimpleParticleType> RA_SUN_FLARE = registerSimple("ra_sun_flare");

    public static final RegistryObject<SimpleParticleType> MORRIGAN_FEATHER = registerSimple("morrigan_feather");
    public static final RegistryObject<SimpleParticleType> MORRIGAN_SLASH_ARC = registerSimple("morrigan_slash_arc");
    public static final RegistryObject<SimpleParticleType> MORRIGAN_BLOOD_ORB = registerSimple("morrigan_blood_orb");

    public static final RegistryObject<SimpleParticleType> INDRA_LIGHTNING_ARC = registerSimple("indra_lightning_arc");
    public static final RegistryObject<SimpleParticleType> INDRA_VAJRA_CREST = registerSimple("indra_vajra_crest");
    public static final RegistryObject<SimpleParticleType> INDRA_SHOCK_RING = registerSimple("indra_shock_ring");

    public static final RegistryObject<SimpleParticleType> TEZCA_OBSIDIAN_SHARD = registerSimple("tezca_obsidian_shard");
    public static final RegistryObject<SimpleParticleType> TEZCA_SMOKE_MIRROR = registerSimple("tezca_smoke_mirror");
    public static final RegistryObject<SimpleParticleType> TEZCA_AFTERIMAGE_WISP = registerSimple("tezca_afterimage_wisp");

    public static final RegistryObject<SimpleParticleType> DIVINE_FRACTURE_SPARK = registerSimple("divine_fracture_spark");

    public static final RegistryObject<SimpleParticleType> SUBJUGATION_RUNE_RING = registerSimple("subjugation_rune_ring");
    public static final RegistryObject<SimpleParticleType> SUBJUGATION_CHAIN_LINK = registerSimple("subjugation_chain_link");
    public static final RegistryObject<SimpleParticleType> SUBJUGATION_SIGIL = registerSimple("subjugation_sigil");
    public static final RegistryObject<SimpleParticleType> QI_RIBBON_WISP = registerSimple("qi_ribbon_wisp");

    private ModParticles() {
    }

    private static RegistryObject<SimpleParticleType> registerSimple(String id) {
        return PARTICLE_TYPES.register(id, () -> new SimpleParticleType(true));
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
