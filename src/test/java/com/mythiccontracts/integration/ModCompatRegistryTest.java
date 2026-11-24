package com.mythiccontracts.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModCompatRegistryTest {

    @AfterEach
    void tearDown() {
        ModCompatRegistry.clearTestLoadedMods();
    }

    @Test
    void handlesMissingModListSafelyAndReportsAllTargetIntegrations() {
        assertFalse(ModCompatRegistry.isLoaded(ModCompatRegistry.MOD_KUBEJS));
        assertFalse(ModCompatRegistry.isLoaded(""));
        assertFalse(ModCompatRegistry.isLoaded(null));

        List<ModCompatRegistry.IntegrationStatus> statuses = ModCompatRegistry.getIntegrationStatuses();
        Set<String> ids = statuses.stream()
                .map(ModCompatRegistry.IntegrationStatus::modId)
                .collect(Collectors.toSet());

        assertTrue(ids.contains(ModCompatRegistry.MOD_KUBEJS));
        assertTrue(ids.contains(ModCompatRegistry.MOD_FTB_QUESTS));
        assertTrue(ids.contains(ModCompatRegistry.MOD_IRONS_SPELLBOOKS));
        assertTrue(ids.contains(ModCompatRegistry.MOD_TACZ));
        assertTrue(ids.contains(ModCompatRegistry.MOD_BETTER_COMBAT));
        assertTrue(ids.contains(ModCompatRegistry.MOD_SIMPLY_SWORDS));
        assertTrue(ids.contains(ModCompatRegistry.MOD_EMBEDDIUM));
    }

    @Test
    void reflectsTestLoadedModOverridesCleanly() {
        ModCompatRegistry.setTestLoadedMods(Set.of(
                ModCompatRegistry.MOD_KUBEJS,
                ModCompatRegistry.MOD_TACZ,
                ModCompatRegistry.MOD_EMBEDDIUM
        ));

        assertTrue(ModCompatRegistry.isLoaded(ModCompatRegistry.MOD_KUBEJS));
        assertTrue(ModCompatRegistry.isLoaded(ModCompatRegistry.MOD_TACZ));
        assertTrue(ModCompatRegistry.isLoaded(ModCompatRegistry.MOD_EMBEDDIUM));
        assertFalse(ModCompatRegistry.isLoaded(ModCompatRegistry.MOD_BETTER_COMBAT));
        assertEquals(3, ModCompatRegistry.countLoadedPrimaryIntegrations());

        boolean taczLoaded = ModCompatRegistry.getIntegrationStatuses().stream()
                .filter(st -> ModCompatRegistry.MOD_TACZ.equals(st.modId()))
                .findFirst()
                .map(ModCompatRegistry.IntegrationStatus::loaded)
                .orElse(false);
        assertTrue(taczLoaded);

        ModCompatRegistry.clearTestLoadedMods();
        assertEquals(0, ModCompatRegistry.countLoadedPrimaryIntegrations());
    }
}
