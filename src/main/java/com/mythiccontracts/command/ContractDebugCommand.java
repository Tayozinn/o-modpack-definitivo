package com.mythiccontracts.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.cultivation.CultivationBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContractDebugCommand {

    private static final List<String> CONTRACT_SUGGESTIONS = List.of(
            "paladin_vow",
            "lucifer_pact",
            "mythic_contracts:paladin_vow",
            "mythic_contracts:lucifer_pact"
    );

    private static final List<String> FACTION_SUGGESTIONS = Arrays.stream(FactionType.values())
            .map(FactionType::getId)
            .toList();

    private static final List<String> MURIM_PATH_SUGGESTIONS = List.of("corpo", "espirito");

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CONTRACTS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(CONTRACT_SUGGESTIONS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FACTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(FACTION_SUGGESTIONS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MURIM_PATHS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(MURIM_PATH_SUGGESTIONS, builder);

    private ContractDebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildCommandTree("contrato"));
        dispatcher.register(buildCommandTree("core"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .then(Commands.literal("dar")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(SUGGEST_CONTRACTS)
                                .executes(ContractDebugCommand::executeGrantContract)))
                .then(Commands.literal("remover")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(SUGGEST_CONTRACTS)
                                .executes(ContractDebugCommand::executeRevokeContract)))
                .then(Commands.literal("status")
                        .executes(ContractDebugCommand::executeStatus))
                .then(Commands.literal("faccao")
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests(SUGGEST_FACTIONS)
                                .executes(ContractDebugCommand::executeSetFaction)))
                .then(Commands.literal("murim")
                        .then(Commands.argument("caminho", StringArgumentType.word())
                                .suggests(SUGGEST_MURIM_PATHS)
                                .then(Commands.argument("reino", IntegerArgumentType.integer(1, 7))
                                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1, 3))
                                                .executes(ContractDebugCommand::executeSetMurim)))));
    }

    private static int executeGrantContract(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawId = StringArgumentType.getString(ctx, "id");
        ResourceLocation contractId = ContractManager.resolveContractId(rawId);

        ContractManager.ContractActionResult result = ContractManager.grantContract(player, contractId);
        ctx.getSource().sendSuccess(() -> result.message(), false);
        return result.success() ? 1 : 0;
    }

    private static int executeRevokeContract(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawId = StringArgumentType.getString(ctx, "id");
        ResourceLocation contractId = ContractManager.resolveContractId(rawId);

        ContractManager.ContractActionResult result = ContractManager.revokeContract(player, contractId);
        ctx.getSource().sendSuccess(() -> result.message(), false);
        return result.success() ? 1 : 0;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        return ModCapabilities.get(player).map(data -> {
            Set<ResourceLocation> contracts = data.getActiveContracts();
            String contractsFormatted = contracts.isEmpty()
                    ? "Nenhum"
                    : contracts.stream()
                    .map(id -> ContractManager.getContractDisplayName(id) + " [" + id + "]")
                    .collect(Collectors.joining(", "));

            List<ResourceLocation> subjugated = data.getSubjugatedEntities();
            String subjugatedFormatted = subjugated.isEmpty()
                    ? "Nenhuma (0)"
                    : subjugated.stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(", ")) + " (Total: " + subjugated.size() + ")";

            String vowStatus = data.isVowBroken() ? "§cSim (QUEBRADO)" : "§aNão";
            String infusionStatus = data.isManaInfusionActive() ? "§bATIVA" : "§7INATIVA";

            double bodyPassiveBonus = CultivationBalance.calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
            int bodyInfusionPct = Math.round(CultivationBalance.calculateBodyInfusionMultiplier(data.getBodyRealm(), data.getBodyStage()) * 100.0F);
            float spiritFlatBonus = CultivationBalance.calculateSpiritInfusionFlatBonus(data.getSpiritRealm(), data.getSpiritStage());
            double currentAttackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

            player.sendSystemMessage(Component.literal("§6§l=== [ Mythic Contracts — Status do Núcleo ] ==="));
            player.sendSystemMessage(Component.literal("§e• Facção: §f" + data.getFaction().getDisplayName()
                    + " §7(" + data.getFaction().getId() + ")"));
            player.sendSystemMessage(Component.literal("§e• Cultivo do Corpo: §fReino " + data.getBodyRealm()
                    + " — Etapa " + data.getBodyStage() + " §6(" + data.getBodyStageName() + ") §7(+"
                    + String.format(Locale.ROOT, "%.1f", bodyPassiveBonus) + " Dano Físico Passivo | +"
                    + bodyInfusionPct + "% Buff na Infusão)"));
            player.sendSystemMessage(Component.literal("§e• Cultivo do Espírito: §fReino " + data.getSpiritRealm()
                    + " — Etapa " + data.getSpiritStage() + " §b(" + data.getSpiritStageName() + ") §7(+"
                    + String.format(Locale.ROOT, "%.1f", spiritFlatBonus) + " Dano Espiritual na Infusão)"));
            player.sendSystemMessage(Component.literal("§e• Ataque Físico Total (ATTACK_DAMAGE): §c"
                    + String.format(Locale.ROOT, "%.2f", currentAttackDamage)));
            player.sendSystemMessage(Component.literal("§e• Contratos Ativos: §d" + contractsFormatted));
            player.sendSystemMessage(Component.literal("§e• Voto Quebrado: " + vowStatus));
            player.sendSystemMessage(Component.literal("§e• Infusão de Mana [V]: " + infusionStatus));
            player.sendSystemMessage(Component.literal("§e• Entidades Subjugadas: §a" + subjugatedFormatted));
            player.sendSystemMessage(Component.literal("§6§l============================================"));
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("Capability PlayerCoreData não encontrada."));
            return 0;
        });
    }

    private static int executeSetFaction(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawFaction = StringArgumentType.getString(ctx, "nome");

        Optional<FactionType> parsedOpt = FactionType.parse(rawFaction);
        if (parsedOpt.isEmpty()) {
            ctx.getSource().sendFailure(
                    Component.literal("Facção inválida: '" + rawFaction + "'. Opções válidas: "
                                    + String.join(", ", FACTION_SUGGESTIONS))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        FactionType newFaction = parsedOpt.get();
        return ModCapabilities.get(player).map(data -> {
            data.setFaction(newFaction);
            ContractManager.validateContractsForFaction(player, data);

            ctx.getSource().sendSuccess(
                    () -> Component.literal("✦ Facção alterada para: " + newFaction.getDisplayName()
                                    + " (" + newFaction.getId() + ")")
                            .withStyle(ChatFormatting.GOLD),
                    false
            );
            return 1;
        }).orElse(0);
    }

    private static int executeSetMurim(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String path = StringArgumentType.getString(ctx, "caminho").trim().toLowerCase(Locale.ROOT);
        int realm = IntegerArgumentType.getInteger(ctx, "reino");
        int stage = IntegerArgumentType.getInteger(ctx, "etapa");

        boolean isBody = path.equals("corpo") || path.equals("body");
        boolean isSpirit = path.equals("espirito") || path.equals("espírito") || path.equals("spirit");

        if (!isBody && !isSpirit) {
            ctx.getSource().sendFailure(
                    Component.literal("Caminho inválido: '" + path + "'. Use 'corpo' ou 'espirito'.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        return ModCapabilities.get(player).map(data -> {
            if (isBody) {
                data.setBodyRealm(realm);
                data.setBodyStage(stage);
                ContractManager.refreshPlayerContractAttributes(player, data);
                ModCapabilities.sync(player);

                String stageName = data.getBodyStageName();
                double passiveBonus = CultivationBalance.calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
                int infusionPct = Math.round(CultivationBalance.calculateBodyInfusionMultiplier(data.getBodyRealm(), data.getBodyStage()) * 100.0F);
                double totalAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                ctx.getSource().sendSuccess(
                        () -> Component.literal("✦ Cultivo do Corpo atualizado para: Reino " + data.getBodyRealm()
                                        + " — Etapa " + data.getBodyStage() + " (" + stageName + ") [+"
                                        + String.format(Locale.ROOT, "%.1f", passiveBonus) + " Dano Físico Passivo | +"
                                        + infusionPct + "% Buff na Infusão | Ataque Total: "
                                        + String.format(Locale.ROOT, "%.2f", totalAttack) + "]")
                                .withStyle(ChatFormatting.GOLD),
                        false
                );
            } else {
                data.setSpiritRealm(realm);
                data.setSpiritStage(stage);
                ContractManager.refreshPlayerContractAttributes(player, data);
                ModCapabilities.sync(player);

                String stageName = data.getSpiritStageName();
                float spiritFlatBonus = CultivationBalance.calculateSpiritInfusionFlatBonus(data.getSpiritRealm(), data.getSpiritStage());
                ctx.getSource().sendSuccess(
                        () -> Component.literal("✦ Cultivo do Espírito atualizado para: Reino " + data.getSpiritRealm()
                                        + " — Etapa " + data.getSpiritStage() + " (" + stageName + ") [+"
                                        + String.format(Locale.ROOT, "%.1f", spiritFlatBonus) + " Dano Espiritual na Infusão]")
                                .withStyle(ChatFormatting.AQUA),
                        false
                );
            }
            return 1;
        }).orElse(0);
    }
}
