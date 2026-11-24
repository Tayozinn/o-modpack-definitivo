package com.mythiccontracts.skill.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.core.data.DatapackJsonHelper;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carrega parâmetros e descrições de Resquícios Divinos em {@code data/<ns>/skills/*.json}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DivineSkillReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "skills";

    public DivineSkillReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DivineSkillReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, DivineSkillDefinition> parsed = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                DivineSkillDefinition def = parseSkill(fileId, entry.getValue().getAsJsonObject());
                parsed.put(def.id(), def);
            } catch (Exception ex) {
                MythicContractsMod.LOGGER.error("[{}] Erro ao carregar habilidade JSON '{}': {}",
                        MythicContractsMod.MOD_ID, fileId, ex.getMessage());
            }
        }

        DivineSkillRegistry.setLoadedSkills(parsed);
        MythicContractsMod.LOGGER.info("[{}] Carregadas {} habilidades divinas via JSON ({} ativas no registro).",
                MythicContractsMod.MOD_ID, parsed.size(), DivineSkillRegistry.getAllSkills().size());
    }

    public static DivineSkillDefinition parseSkill(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = fileId;
        if (json.has("id") && !json.get("id").isJsonNull()) {
            ResourceLocation explicit = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
            if (explicit == null) {
                throw new IllegalArgumentException("Campo 'id' inválido.");
            }
            id = explicit;
        }

        DivineRemnant fallbackRemnant = DivineRemnant.fromSkillId(id);
        DivineSkillDefinition fallback = DivineSkillRegistry.getForRemnant(fallbackRemnant);

        String displayName = GsonHelper.getAsString(json, "display_name", fallback.displayName());
        String deityOrigin = GsonHelper.getAsString(json, "deity_origin", fallback.deityOrigin());
        int hudColor = DatapackJsonHelper.parseHexColor(json, "hud_color", fallback.hudColor());
        String archetype = GsonHelper.getAsString(json, "archetype", fallback.archetype());
        String mechanicSummary = GsonHelper.getAsString(json, "mechanic_summary", fallback.mechanicSummary());

        float baseDamage = GsonHelper.getAsFloat(json, "base_damage", fallback.baseDamage());
        float primaryScale = GsonHelper.getAsFloat(json, "primary_realm_scale", fallback.primaryRealmScale());
        float secondaryScale = GsonHelper.getAsFloat(json, "secondary_scale", fallback.secondaryScale());
        float baseRadius = GsonHelper.getAsFloat(json, "base_radius", fallback.baseRadius());
        float costMult = GsonHelper.getAsFloat(json, "cost_multiplier", 1.0F);
        float cooldownMult = GsonHelper.getAsFloat(json, "cooldown_multiplier", 1.0F);
        float bossMult = GsonHelper.getAsFloat(json, "boss_damage_bonus_multiplier", 1.0F);
        int minBodyRealm = GsonHelper.getAsInt(json, "min_body_realm", 1);
        int minSpiritRealm = GsonHelper.getAsInt(json, "min_spirit_realm", 1);
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);

        return new DivineSkillDefinition(
                id,
                displayName,
                deityOrigin,
                hudColor,
                archetype,
                mechanicSummary,
                baseDamage,
                primaryScale,
                secondaryScale,
                baseRadius,
                costMult,
                cooldownMult,
                bossMult,
                minBodyRealm,
                minSpiritRealm,
                enabled
        );
    }
}
