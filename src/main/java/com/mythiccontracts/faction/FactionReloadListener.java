package com.mythiccontracts.faction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.contract.ContractDefinition;
import com.mythiccontracts.core.data.DatapackJsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega definições de Facções via datapacks em {@code data/<ns>/factions/*.json}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FactionReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "factions";

    public FactionReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new FactionReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, FactionDefinition> parsed = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                FactionDefinition def = parseFaction(fileId, entry.getValue().getAsJsonObject());
                parsed.put(def.id(), def);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.error("[{}] Erro ao carregar facção JSON '{}': {}",
                        MythicContractsMod.MOD_ID, fileId, ex.getMessage());
            }
        }

        FactionRegistry.setLoadedFactions(parsed);
        MythicContractsMod.LOGGER.info("[{}] Carregadas {} facções via JSON ({} ativas no registro).",
                MythicContractsMod.MOD_ID, parsed.size(), FactionRegistry.getAllFactions().size());
    }

    public static FactionDefinition parseFaction(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = fileId;
        if (json.has("id") && !json.get("id").isJsonNull()) {
            ResourceLocation explicit = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (explicit == null) {
                throw new IllegalArgumentException("Campo 'id' inválido.");
            }
            id = explicit;
        }

        String displayName = GsonHelper.getAsString(json, "display_name", id.getPath());
        String tagline = GsonHelper.getAsString(json, "tagline", "");
        int accentColor = DatapackJsonHelper.parseHexColor(json, "accent_color", 0xFF94A3B8);
        float subjugationThreshold = GsonHelper.getAsFloat(json, "subjugation_threshold", 0.15F);
        String bonusSummary = GsonHelper.getAsString(json, "bonus_summary", "");
        String restrictionSummary = GsonHelper.getAsString(json, "restriction_summary", "");

        List<ContractDefinition.AttributeEntry> passiveMods =
                DatapackJsonHelper.parseAttributeEntries(json, "passive_modifiers");

        Map<ResourceLocation, String> relationships = new LinkedHashMap<>();
        if (json.has("default_relationships") && json.get("default_relationships").isJsonObject()) {
            JsonObject relObj = json.getAsJsonObject("default_relationships");
            for (Map.Entry<String, JsonElement> relEntry : relObj.entrySet()) {
                if (!relEntry.getValue().isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation targetId = resolveFactionKey(relEntry.getKey());
                if (targetId != null) {
                    relationships.put(targetId, relEntry.getValue().getAsString());
                }
            }
        }

        return new FactionDefinition(
                id,
                displayName,
                tagline,
                accentColor,
                subjugationThreshold,
                bonusSummary,
                restrictionSummary,
                passiveMods,
                relationships
        );
    }

    private static ResourceLocation resolveFactionKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (!clean.contains(":")) {
            return new ResourceLocation(MythicContractsMod.MOD_ID, clean);
        }
        return ResourceLocation.tryParse(clean);
    }
}
