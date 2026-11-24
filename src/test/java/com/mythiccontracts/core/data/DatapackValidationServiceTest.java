package com.mythiccontracts.core.data;

import com.google.gson.JsonParser;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.contract.ContractDataReloadListener;
import com.mythiccontracts.contract.ContractDefinition;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.faction.FactionRegistry;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatapackValidationServiceTest {

    @AfterEach
    void restoreAllBuiltins() {
        RealmRegistry.resetToBuiltins();
        FactionRegistry.resetToBuiltins();
        ContractManager.resetToBuiltins();
        DivineSkillRegistry.resetToBuiltins();
    }

    @Test
    void builtinRegistriesValidateCleanWithZeroErrorsOrWarnings() {
        DatapackValidationService.ValidationReport report = DatapackValidationService.validateAll();
        assertTrue(report.isClean(),
                "Builtins should validate cleanly, errors=" + report.errors() + " warnings=" + report.warnings());
        assertEquals(14, report.realmCount());
        assertEquals(5, report.factionCount());
        assertEquals(5, report.contractCount());
        assertEquals(4, report.skillCount());
    }

    @Test
    void detectsContractFactionOverlapAndOutOfRangeSkillRequirements() {
        ResourceLocation badContractId = new ResourceLocation(MythicContractsMod.MOD_ID, "conflicting_contract");
        ContractDefinition badContract = new ContractDefinition(
                badContractId, "Conflito", "Teste",
                EnumSet.of(FactionType.ERRANTES_DO_VEU),
                EnumSet.of(FactionType.ERRANTES_DO_VEU),
                List.of(), 0.0D, -1, false,
                1, 1, 0, 0, 0.0F, 0.0F, ""
        );
        ContractManager.setLoadedContracts(Map.of(badContractId, badContract));

        ResourceLocation raId = new ResourceLocation(MythicContractsMod.MOD_ID, "eye_of_ra");
        DivineSkillDefinition highSkill = new DivineSkillDefinition(
                raId, "Olho", "Rá", 0xFFFBBF24, "Sol", "Desc",
                6.0F, 2.5F, 1.0F, 5.5F, 1.0F, 1.0F, 1.0F,
                10, 1, true
        );
        DivineSkillRegistry.setLoadedSkills(Map.of(raId, highSkill));

        DatapackValidationService.ValidationReport report = DatapackValidationService.validateAll();
        assertTrue(report.hasErrors());
        assertFalse(report.warnings().isEmpty());
    }

    @Test
    void contractsV2EnforceEligibilityAndApplyVitalBonuses() {
        String rawContract = """
                {
                  "display_name": "Pacto V2 Teste",
                  "description": "Teste v2",
                  "allowed_factions": ["errantes_do_veu"],
                  "min_body_realm": 2,
                  "min_spirit_realm": 3,
                  "min_reputation": 20,
                  "reputation_reward": 10,
                  "qi_regen_bonus": 3.0,
                  "spiritual_integrity_bonus": 40.0
                }
                """;
        ResourceLocation id = new ResourceLocation(MythicContractsMod.MOD_ID, "test_v2_contract");
        ContractDefinition def = ContractDataReloadListener.parseContract(id, JsonParser.parseString(rawContract).getAsJsonObject());
        ContractManager.setLoadedContracts(Map.of(id, def));

        PlayerData data = new PlayerData();
        data.getFaction().setCurrentFactionId(new ResourceLocation(MythicContractsMod.MOD_ID, "errantes_do_veu"));

        // Reprovação inicial em Body R1 / Spirit R1
        assertFalse(ContractManager.evaluateEligibility(data, def).eligible());

        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 2, 1);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 3, 1);
        // Falta reputação (>= 20)
        assertFalse(ContractManager.evaluateEligibility(data, def).eligible());

        data.getFaction().setReputation(new ResourceLocation(MythicContractsMod.MOD_ID, "errantes_do_veu"), 25);
        assertTrue(ContractManager.evaluateEligibility(data, def).eligible());

        // Ao ativar contrato, Integridade Espiritual Máxima e Qi/s recebem os bônus v2
        float baseMaxSpiritInt = SpiritualIntegrityService.calculateMaxSpiritualIntegrity(data);
        data.getContracts().addContract(id);
        assertEquals(3.0F, ContractManager.getTotalQiRegenBonus(data), 0.001F);
        assertEquals(40.0F, ContractManager.getTotalSpiritualIntegrityBonus(data), 0.001F);
        assertEquals(baseMaxSpiritInt + 40.0F, SpiritualIntegrityService.calculateMaxSpiritualIntegrity(data), 0.01F);

        // Valida tick de Qi: Espírito R3-1 regen base 7.0/s + 3.0/s do contrato = 10.0/s -> 0.50 Qi/tick
        EnergyService.recalculateMaxQi(data);
        data.getCultivation().setCurrentQi(0.0F);
        EnergyService.tickAllEnergy(data, false);
        assertEquals(0.50F, data.getCultivation().getCurrentQi(), 0.01F);
    }
}
