package com.mythiccontracts.skill.cultivation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Map;
import java.util.Set;

/**
 * Carrega Técnicas de Cultivo R1–R7 via datapacks em {@code data/<ns>/cultivation_skills/*.json}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationSkillReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "cultivation_skills";

    public CultivationSkillReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new CultivationSkillReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, CultivationSkillDefinition> parsed = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                CultivationSkillDefinition def = parseSkill(fileId, entry.getValue().getAsJsonObject());
                parsed.put(def.id(), def);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.error("[{}] Erro ao carregar técnica JSON '{}': {}",
                        MythicContractsMod.MOD_ID, fileId, ex.getMessage());
            }
        }

        CultivationSkillRegistry.setLoadedSkills(parsed);
        MythicContractsMod.LOGGER.info("[{}] Carregadas {} técnicas de cultivo via JSON ({} ativas no registro).",
                MythicContractsMod.MOD_ID, parsed.size(), CultivationSkillRegistry.getAllSkills().size());
    }

    public static CultivationSkillDefinition parseSkill(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = fileId;
        if (json.has("id") && !json.get("id").isJsonNull()) {
            ResourceLocation explicit = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (explicit == null) {
                throw new IllegalArgumentException("Campo 'id' inválido.");
            }
            id = explicit;
        }

        CultivationSkillDefinition fallback = CultivationSkillRegistry.getSkill(id);

        String displayName = GsonHelper.getAsString(json, "display_name",
                fallback != null ? fallback.displayName() : id.getPath());
        SkillPathType pathType = SkillPathType.parse(GsonHelper.getAsString(json, "path_type",
                fallback != null ? fallback.pathType().name() : "BODY")).orElse(SkillPathType.BODY);
        CultivationSkillCategory category = CultivationSkillCategory.parse(GsonHelper.getAsString(json, "category",
                fallback != null ? fallback.category().name() : "OFFENSIVE")).orElse(CultivationSkillCategory.OFFENSIVE);
        int tierRealm = GsonHelper.getAsInt(json, "tier_realm", fallback != null ? fallback.tierRealm() : 1);
        int hudColor = DatapackJsonHelper.parseHexColor(json, "hud_color",
                fallback != null ? fallback.hudColor() : pathType.getColor());
        String summary = GsonHelper.getAsString(json, "mechanic_summary",
                fallback != null ? fallback.mechanicSummary() : "");

        float qiCost = GsonHelper.getAsFloat(json, "qi_cost", fallback != null ? fallback.qiCost() : 0.0F);
        float staminaCost = GsonHelper.getAsFloat(json, "stamina_cost", fallback != null ? fallback.staminaCost() : 0.0F);
        float siCost = GsonHelper.getAsFloat(json, "spiritual_integrity_cost",
                fallback != null ? fallback.spiritualIntegrityCost() : 0.0F);
        int cooldownTicks = GsonHelper.getAsInt(json, "cooldown_ticks",
                fallback != null ? fallback.cooldownTicks() : 100);
        boolean usableShatter = GsonHelper.getAsBoolean(json, "usable_during_shatter",
                fallback != null && fallback.usableDuringShatter());
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);

        SkillUnlockRequirements reqs = parseRequirements(json,
                fallback != null ? fallback.requirements() : SkillUnlockRequirements.NONE);

        return new CultivationSkillDefinition(
                id, displayName, pathType, category, tierRealm, hudColor, summary,
                qiCost, staminaCost, siCost, cooldownTicks, usableShatter, reqs, enabled
        );
    }

    private static SkillUnlockRequirements parseRequirements(JsonObject root, SkillUnlockRequirements fb) {
        if (!root.has("unlock_requirements") || !root.get("unlock_requirements").isJsonObject()) {
            return fb;
        }
        JsonObject req = root.getAsJsonObject("unlock_requirements");
        int minBodyRealm = GsonHelper.getAsInt(req, "min_body_realm", fb.minBodyRealm());
        int minBodyStage = GsonHelper.getAsInt(req, "min_body_stage", fb.minBodyStage());
        int minSpiritRealm = GsonHelper.getAsInt(req, "min_spirit_realm", fb.minSpiritRealm());
        int minSpiritStage = GsonHelper.getAsInt(req, "min_spirit_stage", fb.minSpiritStage());

        ResourceLocation prereq = fb.prerequisiteSkill();
        if (req.has("prerequisite_skill")) {
            if (req.get("prerequisite_skill").isJsonNull()) {
                prereq = null;
            } else {
                prereq = ResourceLocation.tryParse(GsonHelper.getAsString(req, "prerequisite_skill"));
            }
        }
        boolean requireContract = GsonHelper.getAsBoolean(req, "require_active_contract", fb.requireActiveContract());
        Set<FactionType> allowed = parseFactionSet(req, "allowed_factions", fb.allowedFactions());
        Set<FactionType> bypass = parseFactionSet(req, "realm_bypass_factions", fb.realmBypassFactions());
        int minSubjugated = GsonHelper.getAsInt(req, "min_subjugated_count", fb.minSubjugatedCount());
        int minRep = GsonHelper.getAsInt(req, "min_reputation", fb.minReputation());
        boolean contractOrAlt = GsonHelper.getAsBoolean(req, "contract_or_alternative", fb.contractOrAlternative());
        float qiUnlockCost = GsonHelper.getAsFloat(req, "qi_unlock_cost", fb.qiUnlockCost());

        return new SkillUnlockRequirements(
                minBodyRealm, minBodyStage, minSpiritRealm, minSpiritStage,
                prereq, requireContract, allowed, bypass, minSubjugated, minRep, contractOrAlt, qiUnlockCost
        );
    }

    private static Set<FactionType> parseFactionSet(JsonObject obj, String key, Set<FactionType> fallback) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return fallback;
        }
        EnumSet<FactionType> set = EnumSet.noneOf(FactionType.class);
        for (JsonElement el : obj.getAsJsonArray(key)) {
            FactionType.parse(el.getAsString()).ifPresent(set::add);
        }
        return set;
    }
}
