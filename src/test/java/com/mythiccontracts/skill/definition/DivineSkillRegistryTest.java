package com.mythiccontracts.skill.definition;

import com.google.gson.JsonParser;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DivineSkillRegistryTest {

    @AfterEach
    void restoreBuiltins() {
        DivineSkillRegistry.resetToBuiltins();
    }

    @Test
    void builtinSkillsPreserveConfirmedBalanceDefaults() {
        assertEquals(4, DivineSkillRegistry.getAllSkills().size());
        DivineSkillDefinition ra = DivineSkillRegistry.getForRemnant(DivineRemnant.EYE_OF_RA);
        assertEquals("Olho de Rá", ra.displayName());
        assertEquals(6.0F, ra.baseDamage(), 0.0001F);
        assertEquals(5.5F, ra.baseRadius(), 0.0001F);
        assertEquals(1.0F, ra.costMultiplier(), 0.0001F);
        assertTrue(ra.enabled());
    }

    @Test
    void customDatapackSkillOverridesCostMultiplierAndRealmGating() {
        String rawJson = """
                {
                  "display_name": "Olho de Rá Supremo",
                  "hud_color": "#F59E0B",
                  "cost_multiplier": 0.5,
                  "cooldown_multiplier": 0.8,
                  "min_body_realm": 3,
                  "min_spirit_realm": 4,
                  "enabled": true
                }
                """;
        ResourceLocation raId = DivineRemnant.EYE_OF_RA.getId();
        DivineSkillDefinition parsed = DivineSkillReloadListener.parseSkill(raId, JsonParser.parseString(rawJson).getAsJsonObject());
        DivineSkillRegistry.setLoadedSkills(Map.of(raId, parsed));

        PlayerData data = new PlayerData();
        data.getSkills().setPrimaryEquippedSkill(raId);

        // Jogador R1-1 bloqueado pelos pré-requisitos R3 / R4 ao tentar equipar
        assertFalse(CultivationActionService.validateEquipRemnant(data, DivineRemnant.EYE_OF_RA).allowed());

        // Custo base R1-1 é 55.0 -> 55 * 0.5 = 28.0 (arredondado de 27.5)
        assertEquals(28.0F, DivineResonanceBalance.calculateDivineSkillCost(data), 0.01F);

        // Sobe para Corpo R3, Espírito R4 -> desbloqueado
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 3, 1);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 4, 1);
        assertTrue(CultivationActionService.validateEquipRemnant(data, DivineRemnant.EYE_OF_RA).allowed());

        // Desabilitado -> bloqueado
        DivineSkillDefinition disabled = new DivineSkillDefinition(
                raId, "Olho de Rá", "Rá", 0xFFFBBF24, "Sol", "Resumo",
                6.0F, 2.5F, 1.0F, 5.5F, 1.0F, 1.0F, 1.0F, 1, 1, false
        );
        DivineSkillRegistry.setLoadedSkills(Map.of(raId, disabled));
        assertFalse(CultivationActionService.validateEquipRemnant(data, DivineRemnant.EYE_OF_RA).allowed());
    }
}
