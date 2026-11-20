package com.mythiccontracts.contract;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.cultivation.CultivationBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

public final class ContractManager {

    public static final List<ResourceLocation> KNOWN_CONTRACTS = List.of(
            PaladinVowContract.ID,
            LuciferPactContract.ID
    );

    private ContractManager() {
    }

    public record ContractActionResult(boolean success, Component message) {
    }

    public static ResourceLocation resolveContractId(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return null;
        }
        String trimmed = rawInput.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.contains(":")) {
            return new ResourceLocation(MythicContractsMod.MOD_ID, trimmed);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
        if (parsed != null && "minecraft".equals(parsed.getNamespace())) {
            ResourceLocation modScoped = new ResourceLocation(MythicContractsMod.MOD_ID, parsed.getPath());
            if (isKnownContract(modScoped)) {
                return modScoped;
            }
        }
        return parsed;
    }

    public static boolean isKnownContract(ResourceLocation contractId) {
        return PaladinVowContract.ID.equals(contractId) || LuciferPactContract.ID.equals(contractId);
    }

    public static String getContractDisplayName(ResourceLocation contractId) {
        if (PaladinVowContract.ID.equals(contractId)) {
            return PaladinVowContract.DISPLAY_NAME;
        }
        if (LuciferPactContract.ID.equals(contractId)) {
            return LuciferPactContract.DISPLAY_NAME;
        }
        return contractId != null ? contractId.toString() : "Desconhecido";
    }

    public static void refreshPlayerContractAttributes(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }

        CultivationBalance.applyCultivationAttributes(player, data);

        if (data.hasContract(LuciferPactContract.ID)) {
            LuciferPactContract.applyModifiers(player);
        } else {
            LuciferPactContract.removeModifiers(player);
        }

        if (data.hasContract(PaladinVowContract.ID) && !data.isVowBroken()) {
            PaladinVowContract.applyPassiveAbsorption(player, data);
        }
    }

    public static ContractActionResult grantContract(ServerPlayer player, ResourceLocation contractId) {
        if (player == null) {
            return new ContractActionResult(false, Component.literal("Jogador inválido.").withStyle(ChatFormatting.RED));
        }
        if (contractId == null || !isKnownContract(contractId)) {
            return new ContractActionResult(
                    false,
                    Component.literal("Contrato desconhecido: '" + (contractId != null ? contractId : "null")
                                    + "'. Use 'paladin_vow' ou 'lucifer_pact'.")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return ModCapabilities.get(player).map(data -> {
            FactionType faction = data.getFaction();

            if (PaladinVowContract.ID.equals(contractId) && !PaladinVowContract.isAllowedForFaction(faction)) {
                return new ContractActionResult(
                        false,
                        Component.literal("✖ A facção '" + faction.getDisplayName()
                                        + "' proíbe o contrato " + PaladinVowContract.DISPLAY_NAME + "!")
                                .withStyle(ChatFormatting.RED)
                );
            }

            if (LuciferPactContract.ID.equals(contractId) && !LuciferPactContract.isAllowedForFaction(faction)) {
                return new ContractActionResult(
                        false,
                        Component.literal("✖ A facção '" + faction.getDisplayName()
                                        + "' proíbe o contrato " + LuciferPactContract.DISPLAY_NAME + "!")
                                .withStyle(ChatFormatting.RED)
                );
            }

            if (PaladinVowContract.ID.equals(contractId)) {
                data.setVowBroken(false);
            }

            boolean added = data.addContract(contractId);
            refreshPlayerContractAttributes(player, data);
            ModCapabilities.sync(player);

            String displayName = getContractDisplayName(contractId);
            if (!added) {
                return new ContractActionResult(
                        true,
                        Component.literal("✦ Contrato '" + displayName + "' (" + contractId + ") atualizado e reafirmado.")
                                .withStyle(ChatFormatting.YELLOW)
                );
            }

            return new ContractActionResult(
                    true,
                    Component.literal("✦ Contrato '" + displayName + "' (" + contractId + ") firmado com sucesso!")
                            .withStyle(ChatFormatting.GREEN)
            );
        }).orElseGet(() -> new ContractActionResult(
                false,
                Component.literal("Capability PlayerCoreData indisponível para o jogador.").withStyle(ChatFormatting.RED)
        ));
    }

    public static ContractActionResult revokeContract(ServerPlayer player, ResourceLocation contractId) {
        if (player == null) {
            return new ContractActionResult(false, Component.literal("Jogador inválido.").withStyle(ChatFormatting.RED));
        }
        if (contractId == null || !isKnownContract(contractId)) {
            return new ContractActionResult(
                    false,
                    Component.literal("Contrato desconhecido: '" + (contractId != null ? contractId : "null")
                                    + "'. Use 'paladin_vow' ou 'lucifer_pact'.")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return ModCapabilities.get(player).map(data -> {
            boolean removed = data.removeContract(contractId);
            if (PaladinVowContract.ID.equals(contractId)) {
                PaladinVowContract.removeEffects(player);
            }
            refreshPlayerContractAttributes(player, data);
            ModCapabilities.sync(player);

            String displayName = getContractDisplayName(contractId);
            if (!removed) {
                return new ContractActionResult(
                        false,
                        Component.literal("O jogador não possui o contrato '" + displayName + "' (" + contractId + ") ativo.")
                                .withStyle(ChatFormatting.YELLOW)
                );
            }

            return new ContractActionResult(
                    true,
                    Component.literal("✦ Contrato '" + displayName + "' (" + contractId + ") removido com sucesso.")
                            .withStyle(ChatFormatting.GOLD)
            );
        }).orElseGet(() -> new ContractActionResult(
                false,
                Component.literal("Capability PlayerCoreData indisponível para o jogador.").withStyle(ChatFormatting.RED)
        ));
    }

    public static void validateContractsForFaction(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }

        FactionType faction = data.getFaction();

        if (!PaladinVowContract.isAllowedForFaction(faction) && data.hasContract(PaladinVowContract.ID)) {
            data.removeContract(PaladinVowContract.ID);
            PaladinVowContract.removeEffects(player);
            player.sendSystemMessage(
                    Component.literal("⚠ O contrato '" + PaladinVowContract.DISPLAY_NAME
                                    + "' foi revogado por incompatibilidade com a facção " + faction.getDisplayName() + ".")
                            .withStyle(ChatFormatting.RED)
            );
        }

        if (!LuciferPactContract.isAllowedForFaction(faction) && data.hasContract(LuciferPactContract.ID)) {
            data.removeContract(LuciferPactContract.ID);
            player.sendSystemMessage(
                    Component.literal("⚠ O contrato '" + LuciferPactContract.DISPLAY_NAME
                                    + "' foi revogado por incompatibilidade com a facção " + faction.getDisplayName() + ".")
                            .withStyle(ChatFormatting.RED)
            );
        }

        refreshPlayerContractAttributes(player, data);
        ModCapabilities.sync(player);
    }
}
