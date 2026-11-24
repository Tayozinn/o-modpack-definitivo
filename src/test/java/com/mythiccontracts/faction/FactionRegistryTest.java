package com.mythiccontracts.faction;

import com.google.gson.JsonParser;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FactionRegistryTest {

    @AfterEach
    void restoreBuiltins() {
        FactionRegistry.resetToBuiltins();
    }

    @Test
    void builtinFactionsProvideExpectedThresholdsAndProfiles() {
        assertEquals(5, FactionRegistry.getAllFactions().size());
        assertEquals(0.25F, FactionRegistry.getSubjugationThreshold(FactionType.QUEBRADORES_DE_TRONOS), 0.0001F);
        assertEquals(0.15F, FactionRegistry.getSubjugationThreshold(FactionType.JURAMENTADOS_DA_LUZ), 0.0001F);
        assertEquals(0.15F, FactionRegistry.getSubjugationThreshold(FactionType.ERRANTES_DO_VEU), 0.0001F);
        assertEquals(0xFFFBBF24, FactionRegistry.getForType(FactionType.JURAMENTADOS_DA_LUZ).accentColor());
    }

    @Test
    void parsesCustomFactionJsonWithPassiveModifiersAndRelationships() {
        String rawJson = """
                {
                  "display_name": "Quebradores Custom",
                  "tagline": "Sobrecarga marcial",
                  "accent_color": "#9333EA",
                  "subjugation_threshold": 0.30,
                  "bonus_summary": "Bonus customizado",
                  "restriction_summary": "Restrição customizada",
                  "passive_modifiers": [
                    {
                      "attribute": "minecraft:generic.armor",
                      "operation": "addition",
                      "amount": 2.0
                    }
                  ],
                  "default_relationships": {
                    "juramentados_da_luz": "rival"
                  }
                }
                """;
        ResourceLocation id = new ResourceLocation(MythicContractsMod.MOD_ID, "quebradores_de_tronos");
        FactionDefinition parsed = FactionReloadListener.parseFaction(id, JsonParser.parseString(rawJson).getAsJsonObject());

        assertEquals(0xFF9333EA, parsed.accentColor());
        assertEquals(0.30F, parsed.subjugationThreshold(), 0.0001F);
        assertEquals(1, parsed.passiveModifiers().size());
        assertEquals(AttributeModifier.Operation.ADDITION, parsed.passiveModifiers().get(0).operation());
        assertEquals(2.0D, parsed.passiveModifiers().get(0).amount(), 0.0001D);
        assertEquals("rival", parsed.defaultRelationships().get(new ResourceLocation(MythicContractsMod.MOD_ID, "juramentados_da_luz")));

        FactionRegistry.setLoadedFactions(Map.of(id, parsed));
        assertEquals(0.30F, FactionRegistry.getSubjugationThreshold(FactionType.QUEBRADORES_DE_TRONOS), 0.0001F);
        FactionRegistry.resetToBuiltins();
        assertEquals(0.25F, FactionRegistry.getSubjugationThreshold(FactionType.QUEBRADORES_DE_TRONOS), 0.0001F);
    }
}
