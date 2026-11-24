package com.mythiccontracts.contract;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.core.data.DatapackJsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContractDataReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "contracts";

    public ContractDataReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ContractDataReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ContractDefinition> parsed = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject json = entry.getValue().getAsJsonObject();
                ContractDefinition def = parseContract(id, json);
                parsed.put(id, def);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.error("[{}] Erro ao carregar contrato JSON '{}': {}",
                        MythicContractsMod.MOD_ID, id, ex.getMessage());
            }
        }

        ContractManager.setLoadedContracts(parsed);
        MythicContractsMod.LOGGER.info("[{}] Carregados {} contratos data-driven via JSON.",
                MythicContractsMod.MOD_ID, parsed.size());
    }

    public static ContractDefinition parseContract(ResourceLocation id, JsonObject json) {
        String displayName = GsonHelper.getAsString(json, "display_name", id.getPath());
        String description = GsonHelper.getAsString(json, "description", "");

        Set<FactionType> allowed = parseFactionSet(json, "allowed_factions");
        Set<FactionType> forbidden = parseFactionSet(json, "forbidden_factions");
        List<ContractDefinition.AttributeEntry> modifiers =
                DatapackJsonHelper.parseAttributeEntries(json, "attribute_modifiers");

        double periodicAbsorption = GsonHelper.getAsDouble(json, "periodic_absorption_hearts", 0.0D);
        int periodicRegen = GsonHelper.getAsInt(json, "periodic_regen_amplifier", -1);
        boolean breakOnAttackVillager = GsonHelper.getAsBoolean(json, "break_on_attack_villager", false);

        int minBodyRealm = GsonHelper.getAsInt(json, "min_body_realm", 1);
        int minSpiritRealm = GsonHelper.getAsInt(json, "min_spirit_realm", 1);
        int minReputation = GsonHelper.getAsInt(json, "min_reputation", 0);
        int reputationReward = GsonHelper.getAsInt(json, "reputation_reward", 0);
        float qiRegenBonus = GsonHelper.getAsFloat(json, "qi_regen_bonus", 0.0F);
        float spiritualIntegrityBonus = GsonHelper.getAsFloat(json, "spiritual_integrity_bonus", 0.0F);
        String tabooSummary = GsonHelper.getAsString(json, "taboo_summary", "");
        float spiritualRegenFlatBonus = GsonHelper.getAsFloat(json, "spiritual_regen_flat_bonus", 0.0F);
        float spiritualRegenPctBonus = GsonHelper.getAsFloat(json, "spiritual_regen_pct_bonus", 0.0F);

        return new ContractDefinition(
                id,
                displayName,
                description,
                allowed,
                forbidden,
                modifiers,
                periodicAbsorption,
                periodicRegen,
                breakOnAttackVillager,
                minBodyRealm,
                minSpiritRealm,
                minReputation,
                reputationReward,
                qiRegenBonus,
                spiritualIntegrityBonus,
                tabooSummary,
                spiritualRegenFlatBonus,
                spiritualRegenPctBonus
        );
    }

    private static Set<FactionType> parseFactionSet(JsonObject json, String key) {
        EnumSet<FactionType> set = EnumSet.noneOf(FactionType.class);
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return set;
        }
        JsonArray arr = json.getAsJsonArray(key);
        for (JsonElement el : arr) {
            FactionType.parse(el.getAsString()).ifPresent(set::add);
        }
        return set;
    }
}
