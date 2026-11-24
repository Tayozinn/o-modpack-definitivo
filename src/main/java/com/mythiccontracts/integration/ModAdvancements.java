package com.mythiccontracts.integration;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.core.player.FactionData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Concede avanços nativos (Advancements) do mod para integração direta com
 * o livro de avanços vanilla [L], FTB Quests e scripts KubeJS.
 */
public final class ModAdvancements {

    public static final ResourceLocation ROOT = id("root");
    public static final ResourceLocation BODY_REALM_2 = id("body_realm_2");
    public static final ResourceLocation SPIRIT_REALM_2 = id("spirit_realm_2");
    public static final ResourceLocation FIRST_FACTION = id("first_faction");
    public static final ResourceLocation FIRST_CONTRACT = id("first_contract");
    public static final ResourceLocation DIVINE_CHANNELING = id("divine_channeling");
    public static final ResourceLocation FIRST_SUBJUGATION = id("first_subjugation");

    public static final String CRITERION_UNLOCKED = "unlocked";

    private ModAdvancements() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MythicContractsMod.MOD_ID, path);
    }

    public static boolean grant(ServerPlayer player, ResourceLocation advancementId) {
        if (player == null || advancementId == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return false;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return false;
        }
        return player.getAdvancements().award(advancement, CRITERION_UNLOCKED);
    }

    public static void syncFromPlayerData(ServerPlayer player, PlayerData data) {
        if (player == null || data == null || player.getServer() == null) {
            return;
        }
        grant(player, ROOT);

        if (CultivationService.getCurrentRealm(data, CultivationPath.BODY).order() >= 2) {
            grant(player, BODY_REALM_2);
        }
        if (CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order() >= 2) {
            grant(player, SPIRIT_REALM_2);
        }
        if (!FactionData.NONE_FACTION_ID.equals(data.getFaction().getCurrentFactionId())) {
            grant(player, FIRST_FACTION);
        }
        if (!data.getContracts().getActiveContractIds().isEmpty()) {
            grant(player, FIRST_CONTRACT);
        }
        if (!data.getSubjugation().getBoundEntities().isEmpty()) {
            grant(player, FIRST_SUBJUGATION);
        }
    }
}
