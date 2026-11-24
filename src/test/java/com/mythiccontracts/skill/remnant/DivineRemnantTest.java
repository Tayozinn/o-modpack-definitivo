package com.mythiccontracts.skill.remnant;

import com.mythiccontracts.core.persistence.PlayerDataSerializer;
import com.mythiccontracts.core.player.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DivineRemnantTest {

    @Test
    void parsesNewDeityIdsAndLegacyAliases() {
        assertEquals(DivineRemnant.EYE_OF_RA, DivineRemnant.parse("eye_of_ra").orElseThrow());
        assertEquals(DivineRemnant.EYE_OF_RA, DivineRemnant.parse("solar_judgment").orElseThrow());
        assertEquals(DivineRemnant.MORRIGAN_REAP, DivineRemnant.parse("morrigan_reap").orElseThrow());
        assertEquals(DivineRemnant.MORRIGAN_REAP, DivineRemnant.parse("abyssal_harvest").orElseThrow());
        assertEquals(DivineRemnant.INDRAS_VAJRA, DivineRemnant.parse("indras_vajra").orElseThrow());
        assertEquals(DivineRemnant.INDRAS_VAJRA, DivineRemnant.parse("sovereign_pulse").orElseThrow());
        assertEquals(DivineRemnant.TEZCATLIPOCA_STEP, DivineRemnant.parse("tezcatlipoca_step").orElseThrow());
        assertEquals(DivineRemnant.TEZCATLIPOCA_STEP, DivineRemnant.parse("veil_step").orElseThrow());
    }

    @Test
    void equipsAndPersistsDivineRemnantIndependentlyOfFaction() {
        PlayerData data = new PlayerData();
        data.getFaction().setCurrentFactionId(new ResourceLocation("mythic_contracts", "juramentados_da_luz"));
        assertEquals(DivineRemnant.TEZCATLIPOCA_STEP.getId(), data.getSkills().getPrimaryEquippedSkill());

        // Qualquer facção pode equipar qualquer resquício divino (ex: Ceifa da Morrígan nos Juramentados da Luz)
        data.getSkills().setPrimaryEquippedSkill(DivineRemnant.MORRIGAN_REAP.getId());
        assertEquals(DivineRemnant.MORRIGAN_REAP.getId(), data.getSkills().getPrimaryEquippedSkill());
        assertTrue(data.getSkills().hasSkillUnlocked(DivineRemnant.MORRIGAN_REAP.getId()));

        CompoundTag saved = PlayerDataSerializer.writeFull(data);
        PlayerData restored = new PlayerData();
        PlayerDataSerializer.read(saved, restored);

        assertEquals(DivineRemnant.MORRIGAN_REAP.getId(), restored.getSkills().getPrimaryEquippedSkill());
    }
}
