package com.mythiccontracts.cultivation.realm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Carrega definições de Reinos de Cultivo via datapacks em {@code data/<namespace>/realms/**\/*.json}.
 * Entradas inválidas são isoladas com log diagnóstico sem derrubar o servidor.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RealmReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "realms";

    public RealmReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RealmReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, RealmDefinition> parsed = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    MythicContractsMod.LOGGER.error("[{}] Reino JSON '{}' ignorado: raiz não é um objeto.",
                            MythicContractsMod.MOD_ID, fileId);
                    continue;
                }
                RealmDefinition def = parseRealm(fileId, entry.getValue().getAsJsonObject());
                if (parsed.containsKey(def.id())) {
                    MythicContractsMod.LOGGER.warn("[{}] Reino duplicado '{}', sobrescrevendo definição anterior a partir de '{}'.",
                            MythicContractsMod.MOD_ID, def.id(), fileId);
                }
                parsed.put(def.id(), def);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.error("[{}] Erro ao carregar reino JSON '{}': {}",
                        MythicContractsMod.MOD_ID, fileId, ex.getMessage());
            }
        }

        RealmRegistry.setLoadedRealms(parsed);
        validateCrossReferences();
        MythicContractsMod.LOGGER.info("[{}] Carregados {} reinos de cultivo via JSON ({} ativos no registro).",
                MythicContractsMod.MOD_ID, parsed.size(), RealmRegistry.getAllRealms().size());
    }

    public static RealmDefinition parseRealm(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = fileId;
        if (json.has("id")) {
            ResourceLocation explicit = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (explicit == null) {
                throw new IllegalArgumentException("Campo 'id' inválido.");
            }
            id = explicit;
        }

        String rawPath = json.has("type")
                ? GsonHelper.getAsString(json, "type")
                : GsonHelper.getAsString(json, "path", inferPathFromId(fileId));
        Optional<CultivationPath> pathOpt = CultivationPath.parse(rawPath);
        if (pathOpt.isEmpty()) {
            throw new IllegalArgumentException("Campo 'type'/'path' inválido: '" + rawPath + "'. Esperado 'body' ou 'spirit'.");
        }
        CultivationPath path = pathOpt.get();

        int order = GsonHelper.getAsInt(json, "order", inferOrderFromId(id));
        if (order < 1) {
            throw new IllegalArgumentException("Campo 'order' deve ser >= 1.");
        }

        String displayName = GsonHelper.getAsString(json, "display_name", id.getPath());
        RealmRequirements requirements = parseRequirements(json);

        List<RealmAttributeBonus> sharedBenefits = parseBenefitsBlock(json);
        List<RealmStageDefinition> stages = parseStages(json, path, order, sharedBenefits);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("Reino deve definir ao menos 1 estágio em 'stages'.");
        }

        float qiBase = GsonHelper.getAsFloat(json, "qi_base", defaultQiBase(path, order));
        float qiRegenBonus = GsonHelper.getAsFloat(json, "qi_regen_bonus", defaultQiRegenBonus(path, order));
        float drainPerSec = GsonHelper.getAsFloat(json, "infusion_drain_per_second", defaultDrainPerSec(path, order));
        float drainPerHit = GsonHelper.getAsFloat(json, "infusion_drain_per_hit", defaultDrainPerHit(path, order));

        return new RealmDefinition(
                id,
                path,
                order,
                displayName,
                stages,
                requirements,
                qiBase,
                qiRegenBonus,
                drainPerSec,
                drainPerHit
        );
    }

    private static RealmRequirements parseRequirements(JsonObject json) {
        if (!json.has("requirements") || !json.get("requirements").isJsonObject()) {
            return RealmRequirements.NONE;
        }
        JsonObject req = json.getAsJsonObject("requirements");
        ResourceLocation prev = null;
        if (req.has("previous_realm") && !req.get("previous_realm").isJsonNull()) {
            prev = ResourceLocation.tryParse(GsonHelper.getAsString(req, "previous_realm"));
        }
        ResourceLocation trib = null;
        if (req.has("tribulation") && !req.get("tribulation").isJsonNull()) {
            trib = ResourceLocation.tryParse(GsonHelper.getAsString(req, "tribulation"));
        }
        int minOpp = GsonHelper.getAsInt(req, "min_opposite_realm_order", 0);
        float qiCost = GsonHelper.getAsFloat(req, "qi_cost", 0.0F);
        return new RealmRequirements(prev, trib, minOpp, qiCost);
    }

    private static List<RealmAttributeBonus> parseBenefitsBlock(JsonObject json) {
        List<RealmAttributeBonus> list = new ArrayList<>();
        if (!json.has("benefits") || !json.get("benefits").isJsonObject()) {
            return list;
        }
        JsonObject benefits = json.getAsJsonObject("benefits");
        for (Map.Entry<String, JsonElement> entry : benefits.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                continue;
            }
            ResourceLocation attrId = mapShorthandAttribute(entry.getKey());
            if (attrId != null) {
                list.add(new RealmAttributeBonus(attrId, AttributeModifier.Operation.ADDITION, entry.getValue().getAsDouble()));
            }
        }
        return list;
    }

    private static List<RealmStageDefinition> parseStages(
            JsonObject json,
            CultivationPath path,
            int realmOrder,
            List<RealmAttributeBonus> sharedBenefits
    ) {
        List<RealmStageDefinition> stages = new ArrayList<>();
        if (!json.has("stages") || !json.get("stages").isJsonArray()) {
            return stages;
        }
        JsonArray arr = json.getAsJsonArray("stages");
        for (int i = 0; i < arr.size(); i++) {
            int stageNum = i + 1;
            JsonElement el = arr.get(i);
            if (el.isJsonPrimitive()) {
                String stageId = el.getAsString();
                stages.add(new RealmStageDefinition(
                        stageId,
                        capitalizeStage(stageId),
                        path == CultivationPath.SPIRIT ? 10.0F * (stageNum - 1) : 0.0F,
                        path == CultivationPath.BODY ? (0.20F * (realmOrder - 1)) + (0.05F * stageNum) : 0.0F,
                        path == CultivationPath.SPIRIT ? 1.0F * realmOrder * (realmOrder + stageNum - 1) : 0.0F,
                        sharedBenefits
                ));
            } else if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String stageId = GsonHelper.getAsString(obj, "id", "stage_" + stageNum);
                String stageName = GsonHelper.getAsString(obj, "display_name", capitalizeStage(stageId));
                float qiBonus = GsonHelper.getAsFloat(obj, "qi_bonus", path == CultivationPath.SPIRIT ? 10.0F * (stageNum - 1) : 0.0F);
                float infMult = GsonHelper.getAsFloat(obj, "infusion_multiplier",
                        path == CultivationPath.BODY ? (0.20F * (realmOrder - 1)) + (0.05F * stageNum) : 0.0F);
                float infFlat = GsonHelper.getAsFloat(obj, "infusion_flat_bonus",
                        path == CultivationPath.SPIRIT ? 1.0F * realmOrder * (realmOrder + stageNum - 1) : 0.0F);

                List<RealmAttributeBonus> attrs = new ArrayList<>(sharedBenefits);
                if (obj.has("attributes") && obj.get("attributes").isJsonArray()) {
                    for (JsonElement attrEl : obj.getAsJsonArray("attributes")) {
                        if (!attrEl.isJsonObject()) {
                            continue;
                        }
                        JsonObject attrObj = attrEl.getAsJsonObject();
                        ResourceLocation attrId = mapShorthandAttribute(GsonHelper.getAsString(attrObj, "attribute"));
                        if (attrId == null) {
                            continue;
                        }
                        AttributeModifier.Operation op = parseOperation(GsonHelper.getAsString(attrObj, "operation", "addition"));
                        double amount = GsonHelper.getAsDouble(attrObj, "amount", 0.0D);
                        UUID fixedUuid = attrObj.has("uuid") ? UUID.fromString(GsonHelper.getAsString(attrObj, "uuid")) : null;
                        attrs.add(new RealmAttributeBonus(attrId, op, amount, fixedUuid));
                    }
                }
                stages.add(new RealmStageDefinition(stageId, stageName, qiBonus, infMult, infFlat, attrs));
            }
        }
        return stages;
    }

    private static void validateCrossReferences() {
        for (RealmDefinition def : RealmRegistry.getAllRealms()) {
            ResourceLocation prev = def.requirements().previousRealm();
            if (prev != null && RealmRegistry.getRealm(prev) == null) {
                MythicContractsMod.LOGGER.warn("[{}] Reino '{}' referencia 'previous_realm' inexistente: '{}'.",
                        MythicContractsMod.MOD_ID, def.id(), prev);
            }
        }
    }

    private static ResourceLocation mapShorthandAttribute(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "health", "max_health" -> new ResourceLocation("minecraft", "generic.max_health");
            case "armor" -> new ResourceLocation("minecraft", "generic.armor");
            case "armor_toughness", "toughness" -> new ResourceLocation("minecraft", "generic.armor_toughness");
            case "attack_damage", "damage" -> new ResourceLocation("minecraft", "generic.attack_damage");
            case "attack_speed" -> new ResourceLocation("minecraft", "generic.attack_speed");
            case "movement_speed", "speed" -> new ResourceLocation("minecraft", "generic.movement_speed");
            case "knockback_resistance" -> new ResourceLocation("minecraft", "generic.knockback_resistance");
            default -> ResourceLocation.tryParse(raw.trim());
        };
    }

    private static AttributeModifier.Operation parseOperation(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    private static String inferPathFromId(ResourceLocation fileId) {
        String p = fileId.getPath().toLowerCase(Locale.ROOT);
        if (p.contains("spirit")) {
            return "spirit";
        }
        return "body";
    }

    private static int inferOrderFromId(ResourceLocation id) {
        String path = id.getPath();
        StringBuilder digits = new StringBuilder();
        for (int i = path.length() - 1; i >= 0; i--) {
            char c = path.charAt(i);
            if (Character.isDigit(c)) {
                digits.insert(0, c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() > 0) {
            try {
                return Math.max(1, Integer.parseInt(digits.toString()));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private static String capitalizeStage(String stageId) {
        if (stageId == null || stageId.isBlank()) {
            return "Base";
        }
        String clean = stageId.replace('_', ' ');
        return Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
    }

    private static float defaultQiBase(CultivationPath path, int order) {
        return path == CultivationPath.SPIRIT
                ? 100.0F + 35.0F * (order - 1)
                : 10.0F * (order - 1);
    }

    private static float defaultQiRegenBonus(CultivationPath path, int order) {
        return path == CultivationPath.SPIRIT ? 4.0F + 1.5F * (order - 1) : 0.0F;
    }

    private static float defaultDrainPerSec(CultivationPath path, int order) {
        return path == CultivationPath.SPIRIT ? 2.5F + 0.5F * (order - 1) : 0.0F;
    }

    private static float defaultDrainPerHit(CultivationPath path, int order) {
        return path == CultivationPath.SPIRIT ? 3.0F + 1.0F * (order - 1) : 0.0F;
    }
}
