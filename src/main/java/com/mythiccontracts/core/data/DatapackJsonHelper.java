package com.mythiccontracts.core.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.contract.ContractDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Utilitários compartilhados de parsing JSON para datapacks de Reinos, Facções, Contratos e Habilidades.
 */
public final class DatapackJsonHelper {

    private DatapackJsonHelper() {
    }

    public static AttributeModifier.Operation parseOperation(String raw) {
        if (raw == null || raw.isBlank()) {
            return AttributeModifier.Operation.ADDITION;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    public static List<ContractDefinition.AttributeEntry> parseAttributeEntries(JsonObject json, String key) {
        List<ContractDefinition.AttributeEntry> list = new ArrayList<>();
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return list;
        }
        JsonArray arr = json.getAsJsonArray(key);
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject modObj = el.getAsJsonObject();
            if (!modObj.has("attribute")) {
                continue;
            }
            ResourceLocation attrId = ResourceLocation.tryParse(GsonHelper.getAsString(modObj, "attribute"));
            if (attrId == null) {
                continue;
            }
            AttributeModifier.Operation op = parseOperation(GsonHelper.getAsString(modObj, "operation", "addition"));
            double amount = GsonHelper.getAsDouble(modObj, "amount", 0.0D);
            UUID fixedUuid = null;
            if (modObj.has("uuid") && !modObj.get("uuid").isJsonNull()) {
                fixedUuid = UUID.fromString(GsonHelper.getAsString(modObj, "uuid"));
            }
            list.add(new ContractDefinition.AttributeEntry(attrId, op, amount, fixedUuid));
        }
        return list;
    }

    public static int parseHexColor(JsonObject json, String key, int defaultArgb) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return defaultArgb;
        }
        JsonElement el = json.get(key);
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return el.getAsInt();
        }
        String raw = el.getAsString().trim();
        if (raw.isEmpty()) {
            return defaultArgb;
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
            raw = raw.substring(2);
        }
        try {
            long parsed = Long.parseLong(raw, 16);
            if (raw.length() <= 6) {
                parsed |= 0xFF000000L;
            }
            return (int) parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Cor hexadecimal inválida em '" + key + "': " + el.getAsString(), ex);
        }
    }
}
