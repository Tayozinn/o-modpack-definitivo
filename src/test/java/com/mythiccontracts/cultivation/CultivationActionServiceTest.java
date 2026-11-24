package com.mythiccontracts.cultivation;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class CultivationActionServiceTest {

    @Test
    void validatesEquipRemnantAgainstCooldownOverloadAndSpiritShatter() {
        PlayerData data = new PlayerData();
        assertTrue(CultivationActionService.validateEquipRemnant(data, DivineRemnant.EYE_OF_RA).allowed());

        // Bloqueado em recarga normal
        data.getSkills().setUniqueSkillCooldownTicks(40);
        assertFalse(CultivationActionService.validateEquipRemnant(data, DivineRemnant.EYE_OF_RA).allowed());
        data.getSkills().setUniqueSkillCooldownTicks(0);

        // Bloqueado em Sobrecarga Divina
        data.getSkills().setDivineOverloadTicks(300);
        assertFalse(CultivationActionService.validateEquipRemnant(data, DivineRemnant.MORRIGAN_REAP).allowed());
        data.getSkills().setDivineOverloadTicks(0);

        // Bloqueado em Colapso Espiritual
        data.getCultivation().setSpiritShatterTicks(120);
        assertFalse(CultivationActionService.validateEquipRemnant(data, DivineRemnant.INDRAS_VAJRA).allowed());
        assertFalse(CultivationActionService.validateProgressionState(data).allowed());

        data.getCultivation().setSpiritShatterTicks(0);
        assertTrue(CultivationActionService.validateProgressionState(data).allowed());
        assertFalse(CultivationActionService.validateEquipRemnant(null, DivineRemnant.EYE_OF_RA).allowed());
        assertFalse(CultivationActionService.validateEquipRemnant(data, null).allowed());
    }

    @Test
    void registersAllFourCoreContractsAsBuiltInFallbacks() {
        Collection<ResourceLocation> ids = ContractManager.getRegisteredContractIds();
        assertTrue(ids.contains(new ResourceLocation(MythicContractsMod.MOD_ID, "paladin_vow")));
        assertTrue(ids.contains(new ResourceLocation(MythicContractsMod.MOD_ID, "lucifer_pact")));
        assertTrue(ids.contains(new ResourceLocation(MythicContractsMod.MOD_ID, "blood_oath")));
        assertTrue(ids.contains(new ResourceLocation(MythicContractsMod.MOD_ID, "throne_breaker_seal")));
    }
}
