package com.mythiccontracts.core.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Carrega {@code data/mythic_contracts/cultivation/spiritual_integrity_balance.json}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritualIntegrityBalanceReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "cultivation";
    private static final ResourceLocation TARGET_ID =
            new ResourceLocation(MythicContractsMod.MOD_ID, "spiritual_integrity_balance");

    public SpiritualIntegrityBalanceReloadListener() {
        super(GSON, FOLDER);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SpiritualIntegrityBalanceReloadListener());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement el = objects.get(TARGET_ID);
        if (el == null || !el.isJsonObject()) {
            SpiritualIntegrityBalanceConfig.resetToDefaults();
            return;
        }
        try {
            SpiritualIntegrityBalanceConfig parsed = parseConfig(el.getAsJsonObject());
            SpiritualIntegrityBalanceConfig.setLoaded(parsed);
            MythicContractsMod.LOGGER.info("[{}] Balanceamento de Integridade Espiritual carregado via JSON.", MythicContractsMod.MOD_ID);
        } catch (Exception ex) {
            MythicContractsMod.LOGGER.error("[{}] Erro ao carregar spiritual_integrity_balance.json: {}",
                    MythicContractsMod.MOD_ID, ex.getMessage());
            SpiritualIntegrityBalanceConfig.resetToDefaults();
        }
    }

    public static SpiritualIntegrityBalanceConfig parseConfig(JsonObject json) {
        SpiritualIntegrityBalanceConfig d = SpiritualIntegrityBalanceConfig.DEFAULT;
        return new SpiritualIntegrityBalanceConfig(
                GsonHelper.getAsInt(json, "combat_cooldown_ticks", d.combatCooldownTicks()),
                GsonHelper.getAsFloat(json, "combat_mult", d.combatMultiplier()),
                GsonHelper.getAsFloat(json, "out_of_combat_mult", d.outOfCombatMultiplier()),
                GsonHelper.getAsFloat(json, "meditation_mult", d.meditationMultiplier()),
                GsonHelper.getAsInt(json, "meditation_still_ticks", d.meditationStillTicks()),
                GsonHelper.getAsFloat(json, "base_regen_r1", d.baseRegenR1()),
                GsonHelper.getAsFloat(json, "linear_realm_scale", d.linearRealmScale()),
                GsonHelper.getAsFloat(json, "quadratic_realm_scale", d.quadraticRealmScale()),
                GsonHelper.getAsFloat(json, "stage_scale", d.stageScale()),
                GsonHelper.getAsFloat(json, "max_flat_bonus_ratio", d.maxFlatBonusRatio()),
                GsonHelper.getAsFloat(json, "min_pct_mult", d.minPctMultiplier()),
                GsonHelper.getAsFloat(json, "max_pct_bonus_cap", d.maxPctMultiplier()),
                GsonHelper.getAsBoolean(json, "interrupt_on_combat_damage", d.interruptOnCombatDamage()),
                GsonHelper.getAsBoolean(json, "enable_meditation_bonus", d.enableMeditationBonus())
        );
    }
}
