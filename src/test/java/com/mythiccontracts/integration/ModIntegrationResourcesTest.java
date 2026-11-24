package com.mythiccontracts.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModIntegrationResourcesTest {

    private static final Path ADVANCEMENTS_DIR = Path.of("src/main/resources/data/mythic_contracts/advancements");
    private static final Path TAGS_DIR = Path.of("src/main/resources/data/mythic_contracts/tags");

    @Test
    void allDeclaredAdvancementsExistWithValidImpossibleTrigger() throws IOException {
        List<ResourceLocation> expectedIds = List.of(
                ModAdvancements.ROOT,
                ModAdvancements.BODY_REALM_2,
                ModAdvancements.SPIRIT_REALM_2,
                ModAdvancements.FIRST_FACTION,
                ModAdvancements.FIRST_CONTRACT,
                ModAdvancements.DIVINE_CHANNELING,
                ModAdvancements.FIRST_SUBJUGATION
        );

        for (ResourceLocation id : expectedIds) {
            Path file = ADVANCEMENTS_DIR.resolve(id.getPath() + ".json");
            assertTrue(Files.isRegularFile(file), "Missing advancement JSON: " + file);

            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(root.has("display"), "Advancement missing display: " + id);
            JsonObject criteria = root.getAsJsonObject("criteria");
            assertTrue(criteria.has(ModAdvancements.CRITERION_UNLOCKED), "Missing 'unlocked' criterion: " + id);
            assertEquals(
                    "minecraft:impossible",
                    criteria.getAsJsonObject(ModAdvancements.CRITERION_UNLOCKED).get("trigger").getAsString(),
                    "Expected impossible trigger in: " + id
            );
        }
    }

    @Test
    void thirdPartyTagEntriesAlwaysMarkRequiredFalse() throws IOException {
        List<Path> tagFiles = List.of(
                TAGS_DIR.resolve("damage_type/firearm_damage.json"),
                TAGS_DIR.resolve("damage_type/arcane_spiritual_damage.json"),
                TAGS_DIR.resolve("damage_type/spiritual_damage.json"),
                TAGS_DIR.resolve("entity_types/high_realm_boss.json"),
                TAGS_DIR.resolve("entity_types/subjugatable.json"),
                TAGS_DIR.resolve("items/qi_resonant_weapons.json")
        );

        for (Path tagFile : tagFiles) {
            assertTrue(Files.isRegularFile(tagFile), "Missing tag file: " + tagFile);
            JsonObject json = JsonParser.parseString(Files.readString(tagFile, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray values = json.getAsJsonArray("values");
            assertFalse(values.isEmpty(), "Empty values array in: " + tagFile);

            for (JsonElement element : values) {
                if (element.isJsonPrimitive()) {
                    String raw = element.getAsString();
                    String ns = raw.replace("#", "").split(":", 2)[0];
                    assertEquals("minecraft", ns,
                            "Third-party tag entry must be an object with required=false in " + tagFile + ": " + raw);
                } else {
                    JsonObject obj = element.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String ns = id.replace("#", "").split(":", 2)[0];
                    if (!"minecraft".equals(ns) && !"mythic_contracts".equals(ns)) {
                        assertTrue(obj.has("required") && !obj.get("required").getAsBoolean(),
                                "Optional third-party entry must set required=false in " + tagFile + ": " + id);
                    }
                }
            }
        }
    }

    @Test
    void ftbQuestsAndKubeJsScriptsReferencePhase9HooksCleanly() throws IOException {
        Path questsFile = Path.of("config/ftbquests/quests/chapters/despertar_2010.snbt");
        assertTrue(Files.isRegularFile(questsFile));
        String snbt = Files.readString(questsFile, StandardCharsets.UTF_8);

        assertTrue(snbt.contains("\"mythic_contracts:divine_channeling\""));
        assertTrue(snbt.contains("\"mythic_contracts:body_realm_2\""));
        assertTrue(snbt.contains("\"mythic_contracts:spirit_realm_2\""));
        assertTrue(snbt.contains("\"mythic_contracts:first_contract\""));
        assertTrue(snbt.contains("\"mythic_contracts:first_subjugation\""));
        assertTrue(snbt.contains("/contrato reputacao errantes_do_veu add 25"));
        assertBalancedBraces(snbt, questsFile.toString());

        Path kjsCompat = Path.of("kubejs/server_scripts/02_mythic_contracts_compat.js");
        assertTrue(Files.isRegularFile(kjsCompat));
        String js = Files.readString(kjsCompat, StandardCharsets.UTF_8);
        assertTrue(js.contains("com.mythiccontracts.integration.MythicContractsAPI"));
        assertTrue(js.contains("global.MythicContracts"));
        assertBalancedBraces(js, kjsCompat.toString());
    }

    private static void assertBalancedBraces(String text, String label) {
        int braces = 0;
        int brackets = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            }
            assertTrue(braces >= 0, "Unbalanced '}' in " + label);
            assertTrue(brackets >= 0, "Unbalanced ']' in " + label);
        }
        assertEquals(0, braces, "Unclosed '{' in " + label);
        assertEquals(0, brackets, "Unclosed '[' in " + label);
    }
}
