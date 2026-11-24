package com.mythiccontracts.registry;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registro central de eventos sonoros autorais do Mythic Contracts 2.0 (Direção A — Mitológico-Épico).
 * Livre de dependências de cliente para permanecer seguro no servidor dedicado.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MythicContractsMod.MOD_ID);

    // Resquícios Divinos
    public static final RegistryObject<SoundEvent> SKILL_RA_CAST = register("skill.ra_cast");
    public static final RegistryObject<SoundEvent> SKILL_RA_IMPACT = register("skill.ra_impact");
    public static final RegistryObject<SoundEvent> SKILL_MORRIGAN_CAST = register("skill.morrigan_cast");
    public static final RegistryObject<SoundEvent> SKILL_MORRIGAN_REAP = register("skill.morrigan_reap");
    public static final RegistryObject<SoundEvent> SKILL_INDRA_CHARGE = register("skill.indra_charge");
    public static final RegistryObject<SoundEvent> SKILL_INDRA_THUNDER = register("skill.indra_thunder");
    public static final RegistryObject<SoundEvent> SKILL_TEZCA_STEP = register("skill.tezca_step");
    public static final RegistryObject<SoundEvent> SKILL_TEZCA_SLASH = register("skill.tezca_slash");
    public static final RegistryObject<SoundEvent> SKILL_DIVINE_OVERLOAD = register("skill.divine_overload");

    // Subjugação Espiritual
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_START = register("subjugation.clash_start");
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_LOOP = register("subjugation.clash_loop");
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_REVOLT = register("subjugation.clash_revolt");
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_SUPPRESS = register("subjugation.clash_suppress");
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_SUCCESS = register("subjugation.clash_success");
    public static final RegistryObject<SoundEvent> SUBJUGATION_CLASH_FAIL = register("subjugation.clash_fail");
    public static final RegistryObject<SoundEvent> SUBJUGATION_SERVANT_SUMMON = register("subjugation.servant_summon");

    // 11 Técnicas Ativas de Cultivo
    public static final RegistryObject<SoundEvent> TECH_GALE_STEP = register("tech.gale_step");
    public static final RegistryObject<SoundEvent> TECH_LOTUS_BREATH = register("tech.lotus_breath");
    public static final RegistryObject<SoundEvent> TECH_JADE_GUARD = register("tech.jade_guard");
    public static final RegistryObject<SoundEvent> TECH_SEISMIC_PALM = register("tech.seismic_palm");
    public static final RegistryObject<SoundEvent> TECH_GOLDEN_AEGIS = register("tech.golden_aegis");
    public static final RegistryObject<SoundEvent> TECH_BLOOD_FURNACE = register("tech.blood_furnace");
    public static final RegistryObject<SoundEvent> TECH_SOUL_LANCE = register("tech.soul_lance");
    public static final RegistryObject<SoundEvent> TECH_ETHER_FLASH = register("tech.ether_flash");
    public static final RegistryObject<SoundEvent> TECH_SPIRIT_DOMAIN = register("tech.spirit_domain");
    public static final RegistryObject<SoundEvent> TECH_UNDYING_REVERSAL = register("tech.undying_reversal");
    public static final RegistryObject<SoundEvent> TECH_HEAVEN_SUNDER = register("tech.heaven_sunder");

    // Integridade Espiritual & Interface
    public static final RegistryObject<SoundEvent> SPIRIT_INTEGRITY_FULL = register("spirit.integrity_full");
    public static final RegistryObject<SoundEvent> SPIRIT_INTEGRITY_SHATTER = register("spirit.integrity_shatter");
    public static final RegistryObject<SoundEvent> SPIRIT_SLOT_CYCLE = register("spirit.slot_cycle");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        ResourceLocation loc = new ResourceLocation(MythicContractsMod.MOD_ID, id);
        return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(loc));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
