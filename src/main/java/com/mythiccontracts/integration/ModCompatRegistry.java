package com.mythiccontracts.integration;

import com.mythiccontracts.MythicContractsMod;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registro central de integrações opcionais (soft-dependencies) com mods do modpack.
 * Seguro para execução no servidor dedicado e em testes unitários headless.
 */
public final class ModCompatRegistry {

    public static final String MOD_KUBEJS = "kubejs";
    public static final String MOD_FTB_QUESTS = "ftbquests";
    public static final String MOD_IRONS_SPELLBOOKS = "irons_spellbooks";
    public static final String MOD_TACZ = "tacz";
    public static final String MOD_BETTER_COMBAT = "bettercombat";
    public static final String MOD_SIMPLY_SWORDS = "simplyswords";
    public static final String MOD_EMBEDDIUM = "embeddium";
    public static final String MOD_CURIOS = "curios";
    public static final String MOD_JADE = "jade";
    public static final String MOD_JEI = "jei";

    private static final Set<String> PRIMARY_TARGETS = Set.of(
            MOD_KUBEJS,
            MOD_FTB_QUESTS,
            MOD_IRONS_SPELLBOOKS,
            MOD_TACZ,
            MOD_BETTER_COMBAT,
            MOD_SIMPLY_SWORDS,
            MOD_EMBEDDIUM
    );

    public record IntegrationStatus(
            String modId,
            String displayName,
            boolean loaded,
            String hookSummary
    ) {
    }

    private static volatile Set<String> testLoadedModsOverride = null;

    private ModCompatRegistry() {
    }

    static void setTestLoadedMods(Set<String> loadedModIds) {
        if (loadedModIds == null) {
            testLoadedModsOverride = null;
            return;
        }
        testLoadedModsOverride = loadedModIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    static void clearTestLoadedMods() {
        testLoadedModsOverride = null;
    }

    public static boolean isLoaded(String modId) {
        if (modId == null || modId.isBlank()) {
            return false;
        }
        Set<String> override = testLoadedModsOverride;
        if (override != null) {
            return override.contains(modId);
        }
        try {
            ModList list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static List<IntegrationStatus> getIntegrationStatuses() {
        return List.of(
                status(MOD_KUBEJS, "KubeJS",
                        "API pública MythicContractsAPI + scripts em kubejs/server_scripts/"),
                status(MOD_FTB_QUESTS, "FTB Quests",
                        "7 Advancements em mythic_contracts:* + capítulo despertar_2010.snbt"),
                status(MOD_IRONS_SPELLBOOKS, "Iron's Spells 'n Spellbooks",
                        "Escolas em arcane/spiritual_damage, Dead King & Keeper, canal Arcano [V]"),
                status(MOD_TACZ, "Timeless and Classics Zero (TaCZ)",
                        "Tags firearm_damage, canal Balística [V] sem dreno de vigor melee"),
                status(MOD_BETTER_COMBAT, "Better Combat",
                        "Debounce de vigor melee (3 ticks) e economia de Qi em golpes varridos"),
                status(MOD_SIMPLY_SWORDS, "Simply Swords",
                        "Tag qi_resonant_weapons (#simplyswords:uniques/runic_gear) com -15% custo de Qi"),
                status(MOD_EMBEDDIUM, "Embeddium",
                        "Partículas TextureSheetParticle sem shaders + ocultação automática da HUD sob F3"),
                status(MOD_CURIOS, "Curios API",
                        "Keybind separada [I] + suporte a tags de grimório/acessórios"),
                status(MOD_JADE, "Jade",
                        "Atalhos isolados no Numpad 0-5 sem sobreposição ao painel Murim"),
                status(MOD_JEI, "Just Enough Items (JEI)",
                        "Receitas de progressão + atalho [U] restrito ao hover de GUI")
        );
    }

    public static int countLoadedPrimaryIntegrations() {
        int count = 0;
        for (String id : PRIMARY_TARGETS) {
            if (isLoaded(id)) {
                count++;
            }
        }
        return count;
    }

    public static void logStartupSummary() {
        List<IntegrationStatus> statuses = getIntegrationStatuses();
        String loadedList = statuses.stream()
                .filter(IntegrationStatus::loaded)
                .map(IntegrationStatus::displayName)
                .collect(Collectors.joining(", "));
        if (loadedList.isBlank()) {
            loadedList = "nenhum mod opcional detectado";
        }
        MythicContractsMod.LOGGER.info(
                "[{}] Integrações ativas ({}/{} principais): {}",
                MythicContractsMod.MOD_ID,
                countLoadedPrimaryIntegrations(),
                PRIMARY_TARGETS.size(),
                loadedList
        );
    }

    private static IntegrationStatus status(String modId, String displayName, String hookSummary) {
        return new IntegrationStatus(modId, displayName, isLoaded(modId), hookSummary);
    }
}
