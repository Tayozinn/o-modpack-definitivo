package com.mythiccontracts.integration;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.data.DatapackValidationService;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Fachada estática pública para integração segura via KubeJS ({@code Java.loadClass}),
 * scripts de modpack e mods terceiros. Todas as mutações são autoritativas no servidor.
 */
public final class MythicContractsAPI {

    private MythicContractsAPI() {
    }

    // ==================== LEITURA DE ESTADO ====================

    public static int getBodyRealm(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> CultivationService.getCurrentRealm(data, CultivationPath.BODY).order())
                .orElse(1);
    }

    public static int getBodyStage(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> CultivationService.getCurrentStageNumber(data, CultivationPath.BODY))
                .orElse(1);
    }

    public static int getSpiritRealm(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order())
                .orElse(1);
    }

    public static int getSpiritStage(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT))
                .orElse(1);
    }

    public static float getCurrentQi(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getCurrentQi())
                .orElse(0.0F);
    }

    public static float getMaxQi(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getMaxQiCapacity())
                .orElse(100.0F);
    }

    public static float getCurrentSpiritualIntegrity(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getCurrentSpiritualIntegrity())
                .orElse(0.0F);
    }

    public static float getMaxSpiritualIntegrity(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getMaxSpiritualIntegrity())
                .orElse(100.0F);
    }

    public static boolean isSpiritShattered(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().isSpiritShattered())
                .orElse(false);
    }

    public static float getCurrentStamina(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getCurrentStamina())
                .orElse(0.0F);
    }

    public static float getMaxStamina(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().getMaxStamina())
                .orElse(100.0F);
    }

    public static float getCurrentDivineResistance(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getSkills().getCurrentDivineResistance())
                .orElse(0.0F);
    }

    public static float getMaxDivineResistance(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getSkills().getMaxDivineResistance())
                .orElse(100.0F);
    }

    public static boolean isDivineOverloaded(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getSkills().isDivineOverloaded())
                .orElse(false);
    }

    public static boolean isManaInfusionActive(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getCultivation().isManaInfusionActive())
                .orElse(false);
    }

    public static String getFactionId(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getFaction().getCurrentFactionId().getPath())
                .orElse(FactionType.NONE.getId());
    }

    public static int getReputation(Player player, String factionId) {
        ResourceLocation targetId = resolveFactionId(factionId);
        if (targetId == null) {
            return 0;
        }
        return ModCapabilities.getData(player)
                .map(data -> data.getFaction().getReputation(targetId))
                .orElse(0);
    }

    public static boolean hasContract(Player player, String contractId) {
        if (contractId == null || contractId.isBlank()) {
            return false;
        }
        ResourceLocation id = ContractManager.resolveContractId(contractId);
        return ModCapabilities.getData(player)
                .map(data -> data.getContracts().hasContract(id))
                .orElse(false);
    }

    public static List<String> getActiveContractIds(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getContracts().getActiveContractIds().stream()
                        .map(ResourceLocation::toString)
                        .toList())
                .orElse(List.of());
    }

    public static String getEquippedRemnantId(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getSkills().getPrimaryEquippedSkill().toString())
                .orElse(DivineRemnant.TEZCATLIPOCA_STEP.getId().toString());
    }

    public static int getSubjugatedCount(Player player) {
        return ModCapabilities.getData(player)
                .map(data -> data.getSubjugation().getBoundEntities().size())
                .orElse(0);
    }

    public static boolean isModIntegrated(String modId) {
        return ModCompatRegistry.isLoaded(modId);
    }

    public static List<ModCompatRegistry.IntegrationStatus> getIntegrationStatuses() {
        return ModCompatRegistry.getIntegrationStatuses();
    }

    public static DatapackValidationService.ValidationReport validateDatapacks() {
        return DatapackValidationService.validateAll();
    }

    // ==================== MUTAÇÃO AUTORITATIVA (SERVER) ====================

    public static boolean setBodyCultivation(ServerPlayer player, int realmOrder, int stage) {
        return setCultivation(player, CultivationPath.BODY, realmOrder, stage);
    }

    public static boolean setSpiritCultivation(ServerPlayer player, int realmOrder, int stage) {
        return setCultivation(player, CultivationPath.SPIRIT, realmOrder, stage);
    }

    public static boolean setFaction(ServerPlayer player, String factionId) {
        if (player == null || factionId == null || factionId.isBlank()) {
            return false;
        }
        Optional<FactionType> parsed = FactionType.parse(factionId);
        if (parsed.isEmpty()) {
            return false;
        }
        return ModCapabilities.get(player).map(data -> {
            data.setFaction(parsed.get());
            ContractManager.refreshPlayerContractAttributes(player, data);
            ModCapabilities.sync(player);
            return true;
        }).orElse(false);
    }

    public static boolean setReputation(ServerPlayer player, String factionId, int value) {
        ResourceLocation targetId = resolveFactionId(factionId);
        if (player == null || targetId == null) {
            return false;
        }
        int clamped = Mth.clamp(value, -10000, 10000);
        return ModCapabilities.getData(player).map(data -> {
            data.getFaction().setReputation(targetId, clamped);
            ModCapabilities.sync(player);
            return true;
        }).orElse(false);
    }

    public static int addReputation(ServerPlayer player, String factionId, int delta) {
        ResourceLocation targetId = resolveFactionId(factionId);
        if (player == null || targetId == null) {
            return 0;
        }
        return ModCapabilities.getData(player).map(data -> {
            int next = Mth.clamp(data.getFaction().getReputation(targetId) + delta, -10000, 10000);
            data.getFaction().setReputation(targetId, next);
            ModCapabilities.sync(player);
            return next;
        }).orElse(0);
    }

    public static boolean grantContract(ServerPlayer player, String contractId) {
        if (player == null || contractId == null || contractId.isBlank()) {
            return false;
        }
        ResourceLocation id = ContractManager.resolveContractId(contractId);
        return ContractManager.grantContract(player, id).success();
    }

    public static boolean revokeContract(ServerPlayer player, String contractId) {
        if (player == null || contractId == null || contractId.isBlank()) {
            return false;
        }
        ResourceLocation id = ContractManager.resolveContractId(contractId);
        return ContractManager.revokeContract(player, id).success();
    }

    public static boolean setEquippedRemnant(ServerPlayer player, String remnantId) {
        if (player == null || remnantId == null || remnantId.isBlank()) {
            return false;
        }
        Optional<DivineRemnant> parsed = DivineRemnant.parse(remnantId);
        if (parsed.isEmpty()) {
            return false;
        }
        return ModCapabilities.get(player).map(data -> {
            data.getPlayerData().getSkills().setPrimaryEquippedSkill(parsed.get().getId());
            ModCapabilities.sync(player);
            return true;
        }).orElse(false);
    }

    public static float addQi(ServerPlayer player, float delta) {
        if (player == null) {
            return 0.0F;
        }
        return ModCapabilities.getData(player).map(data -> {
            EnergyService.addQi(data, delta);
            ModCapabilities.syncEnergy(player);
            return data.getCultivation().getCurrentQi();
        }).orElse(0.0F);
    }

    public static boolean restoreVitals(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return ModCapabilities.getData(player).map(data -> {
            EnergyService.recalculateMaxQi(data);
            data.getCultivation().setSpiritShatterTicks(0);
            data.getCultivation().setCurrentQi(data.getCultivation().getMaxQiCapacity());
            data.getCultivation().setCurrentStamina(data.getCultivation().getMaxStamina());
            data.getCultivation().setCurrentSpiritualIntegrity(data.getCultivation().getMaxSpiritualIntegrity());
            data.getSkills().setDivineOverloadTicks(0);
            data.getSkills().setCurrentDivineResistance(data.getSkills().getMaxDivineResistance());
            ModCapabilities.syncEnergy(player);
            return true;
        }).orElse(false);
    }

    private static boolean setCultivation(ServerPlayer player, CultivationPath path, int realmOrder, int stage) {
        if (player == null) {
            return false;
        }
        return ModCapabilities.get(player).map(data -> {
            PlayerData playerData = data.getPlayerData();
            if (!CultivationService.setRealmAndStageByOrder(playerData, path, realmOrder, stage)) {
                return false;
            }
            CultivationService.applyCultivationAttributes(player, playerData);
            EnergyService.recalculateMaxQi(playerData);
            ModCapabilities.sync(player);
            return true;
        }).orElse(false);
    }

    private static ResourceLocation resolveFactionId(String rawFactionId) {
        if (rawFactionId == null || rawFactionId.isBlank()) {
            return null;
        }
        Optional<FactionType> builtIn = FactionType.parse(rawFactionId);
        if (builtIn.isPresent()) {
            return new ResourceLocation(MythicContractsMod.MOD_ID, builtIn.get().getId());
        }
        String normalized = rawFactionId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            return ResourceLocation.tryParse(MythicContractsMod.MOD_ID + ":" + normalized);
        }
        return ResourceLocation.tryParse(normalized);
    }
}
