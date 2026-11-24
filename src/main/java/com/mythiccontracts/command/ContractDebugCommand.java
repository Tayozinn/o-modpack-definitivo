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
import com.mythiccontracts.core.data.DatapackValidationService;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.cultivation.UniqueSkillHandler;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.integration.ModCompatRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

    private static final List<String> REMNANT_SUGGESTIONS = Arrays.stream(DivineRemnant.values())
            .map(r -> r.getId().getPath())
            .toList();

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CONTRACTS =
            (ctx, builder) -> {
                List<String> dynamicSuggestions = ContractManager.getRegisteredContractIds().stream()
                        .flatMap(id -> java.util.stream.Stream.of(id.getPath(), id.toString()))
                        .distinct()
                        .toList();
                return SharedSuggestionProvider.suggest(dynamicSuggestions, builder);
            };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_FACTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(FACTION_SUGGESTIONS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MURIM_PATHS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(MURIM_PATH_SUGGESTIONS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_REMNANTS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(REMNANT_SUGGESTIONS, builder);

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
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(SUGGEST_CONTRACTS)
                                .executes(ContractDebugCommand::executeGrantContract)))
                .then(Commands.literal("remover")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests(SUGGEST_CONTRACTS)
                                .executes(ContractDebugCommand::executeRevokeContract)))
                .then(Commands.literal("status")
                        .executes(ContractDebugCommand::executeStatus))
                .then(Commands.literal("faccao")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests(SUGGEST_FACTIONS)
                                .executes(ContractDebugCommand::executeSetFaction)))
                .then(Commands.literal("murim")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("caminho", StringArgumentType.word())
                                .suggests(SUGGEST_MURIM_PATHS)
                                .then(Commands.argument("reino", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("etapa", IntegerArgumentType.integer(1))
                                                .executes(ContractDebugCommand::executeSetMurim)))))
                .then(Commands.literal("resquicio")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("nome", StringArgumentType.word())
                                .suggests(SUGGEST_REMNANTS)
                                .executes(ContractDebugCommand::executeSetRemnant)))
                .then(Commands.literal("reputacao")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("faccao", StringArgumentType.word())
                                .suggests(SUGGEST_FACTIONS)
                                .then(Commands.argument("valor", IntegerArgumentType.integer(-10000, 10000))
                                        .executes(ContractDebugCommand::executeSetReputation))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("delta", IntegerArgumentType.integer(-10000, 10000))
                                                .executes(ContractDebugCommand::executeAddReputation)))))
                .then(Commands.literal("validar")
                        .requires(source -> source.hasPermission(2))
                        .executes(ContractDebugCommand::executeValidateDatapacks))
                .then(Commands.literal("compat")
                        .executes(ContractDebugCommand::executeCompatReport));
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
            PlayerData pData = data.getPlayerData();
            SkillData skills = pData.getSkills();
            var cult = pData.getCultivation();
            DivineRemnant remnant = UniqueSkillHandler.getEquippedRemnant(pData);
            DivineResonanceBalance.ManifestationTier tier = DivineResonanceBalance.getManifestationTier(pData);
            float divineCost = DivineResonanceBalance.calculateDivineSkillCost(pData);

            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "§e• Vida Corporal: §c%.1f / %.1f §7| §eVigor: §a%.1f / %.1f",
                    player.getHealth(),
                    player.getMaxHealth(),
                    cult.getCurrentStamina(),
                    cult.getMaxStamina()
            )));
            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "§e• Integridade Espiritual: §d%.1f / %.1f §7(Colapso: %s)",
                    cult.getCurrentSpiritualIntegrity(),
                    cult.getMaxSpiritualIntegrity(),
                    cult.isSpiritShattered()
                            ? String.format(Locale.ROOT, "%.1fs", cult.getSpiritShatterTicks() / 20.0F)
                            : "Não"
            )));
            player.sendSystemMessage(Component.literal("§e• Infusão de Mana [V]: " + infusionStatus));
            player.sendSystemMessage(Component.literal("§e• Resquício Divino [G]: §b" + remnant.getDisplayName()
                    + " §6[" + tier.getNumeral() + " - " + tier.getTitle() + "] §7(" + remnant.getDeityOrigin() + ")"));
            player.sendSystemMessage(Component.literal(String.format(
                    Locale.ROOT,
                    "§e• Resistência Divina: §6%.1f / %.1f §7(Custo [G]: %.0f | Sobrecarga: %s)",
                    skills.getCurrentDivineResistance(),
                    skills.getMaxDivineResistance(),
                    divineCost,
                    skills.isDivineOverloaded()
                            ? String.format(Locale.ROOT, "%.1fs", skills.getDivineOverloadTicks() / 20.0F)
                            : "Não"
            )));
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

        CultivationPath cultPath = CultivationPath.parse(path).orElse(null);
        if (cultPath == null) {
            ctx.getSource().sendFailure(
                    Component.literal("Caminho inválido: '" + path + "'. Use 'corpo' ou 'espirito'.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        RealmDefinition realmDef = RealmRegistry.getRealmByOrder(cultPath, realm);
        if (realmDef == null) {
            ctx.getSource().sendFailure(
                    Component.literal("Reino de ordem " + realm + " não encontrado para o caminho '"
                                    + cultPath.getDisplayName() + "'.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        return ModCapabilities.get(player).map(data -> {
            CultivationService.setRealmAndStage(data.getPlayerData(), cultPath, realmDef, stage);
            ContractManager.refreshPlayerContractAttributes(player, data);
            var cult = data.getPlayerData().getCultivation();
            data.setCurrentQi(data.getMaxQi());
            cult.setCurrentStamina(cult.getMaxStamina());
            if (!cult.isSpiritShattered()) {
                cult.setCurrentSpiritualIntegrity(cult.getMaxSpiritualIntegrity());
            }
            SkillData skills = data.getPlayerData().getSkills();
            if (!skills.isDivineOverloaded()) {
                skills.setCurrentDivineResistance(skills.getMaxDivineResistance());
            }
            ModCapabilities.sync(player);

            if (cultPath == CultivationPath.BODY) {
                String stageName = data.getBodyStageName();
                double passiveBonus = CultivationBalance.calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
                int infusionPct = Math.round(CultivationBalance.calculateBodyInfusionMultiplier(data.getPlayerData()) * 100.0F);
                double totalAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                ctx.getSource().sendSuccess(
                        () -> Component.literal("✦ Cultivo do Corpo atualizado para: " + realmDef.displayName()
                                        + " (R" + data.getBodyRealm() + "-" + data.getBodyStage() + " " + stageName + ") [+"
                                        + String.format(Locale.ROOT, "%.1f", passiveBonus) + " Dano Físico Passivo | +"
                                        + infusionPct + "% Buff na Infusão | Ataque Total: "
                                        + String.format(Locale.ROOT, "%.2f", totalAttack) + "]")
                                .withStyle(ChatFormatting.GOLD),
                        false
                );
            } else {
                String stageName = data.getSpiritStageName();
                float spiritFlatBonus = CultivationBalance.calculateSpiritInfusionFlatBonus(data.getPlayerData());
                ctx.getSource().sendSuccess(
                        () -> Component.literal("✦ Cultivo do Espírito atualizado para: " + realmDef.displayName()
                                        + " (R" + data.getSpiritRealm() + "-" + data.getSpiritStage() + " " + stageName + ") [+"
                                        + String.format(Locale.ROOT, "%.1f", spiritFlatBonus) + " Dano Espiritual na Infusão]")
                                .withStyle(ChatFormatting.AQUA),
                        false
                );
            }
            return 1;
        }).orElse(0);
    }

    private static int executeSetRemnant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawRemnant = StringArgumentType.getString(ctx, "nome");

        Optional<DivineRemnant> remnantOpt = DivineRemnant.parse(rawRemnant);
        if (remnantOpt.isEmpty()) {
            ctx.getSource().sendFailure(
                    Component.literal("Resquício Divino inválido: '" + rawRemnant + "'. Opções: "
                                    + String.join(", ", REMNANT_SUGGESTIONS))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        DivineRemnant remnant = remnantOpt.get();
        return ModCapabilities.get(player).map(data -> {
            data.getPlayerData().getSkills().setPrimaryEquippedSkill(remnant.getId());
            ModCapabilities.sync(player);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("✦ Resquício Divino [G] sintonizado para: " + remnant.getDisplayName()
                                    + " — Origem: " + remnant.getDeityOrigin())
                            .withStyle(ChatFormatting.AQUA),
                    false
            );
            return 1;
        }).orElse(0);
    }

    private static int executeSetReputation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawFaction = StringArgumentType.getString(ctx, "faccao");
        int value = IntegerArgumentType.getInteger(ctx, "valor");

        Optional<FactionType> parsedOpt = FactionType.parse(rawFaction);
        if (parsedOpt.isEmpty()) {
            ctx.getSource().sendFailure(
                    Component.literal("Facção inválida: '" + rawFaction + "'. Opções válidas: "
                                    + String.join(", ", FACTION_SUGGESTIONS))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        FactionType faction = parsedOpt.get();
        ResourceLocation factionId = new ResourceLocation(MythicContractsMod.MOD_ID, faction.getId());
        return ModCapabilities.get(player).map(data -> {
            data.getPlayerData().getFaction().setReputation(factionId, value);
            ModCapabilities.sync(player);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("✦ Reputação com '" + faction.getDisplayName() + "' definida para: " + value)
                            .withStyle(ChatFormatting.GOLD),
                    false
            );
            return 1;
        }).orElse(0);
    }

    private static int executeValidateDatapacks(CommandContext<CommandSourceStack> ctx) {
        DatapackValidationService.ValidationReport report = DatapackValidationService.validateAll();
        ChatFormatting summaryColor = report.hasErrors()
                ? ChatFormatting.RED
                : (report.warnings().isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);

        ctx.getSource().sendSuccess(
                () -> Component.literal(String.format(
                        Locale.ROOT,
                        "✦ [Datapacks] Reinos: %d | Facções: %d | Contratos: %d | Resquícios: %d | Erros: %d | Avisos: %d",
                        report.realmCount(),
                        report.factionCount(),
                        report.contractCount(),
                        report.skillCount(),
                        report.errors().size(),
                        report.warnings().size()
                )).withStyle(summaryColor, ChatFormatting.BOLD),
                false
        );
        for (String err : report.errors()) {
            ctx.getSource().sendFailure(Component.literal("✖ " + err));
        }
        for (String warn : report.warnings()) {
            ctx.getSource().sendSuccess(() -> Component.literal("⚠ " + warn).withStyle(ChatFormatting.YELLOW), false);
        }
        return report.hasErrors() ? 0 : 1;
    }

    private static int executeAddReputation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String rawFaction = StringArgumentType.getString(ctx, "faccao");
        int delta = IntegerArgumentType.getInteger(ctx, "delta");

        Optional<FactionType> parsedOpt = FactionType.parse(rawFaction);
        if (parsedOpt.isEmpty()) {
            ctx.getSource().sendFailure(
                    Component.literal("Facção inválida: '" + rawFaction + "'. Opções válidas: "
                                    + String.join(", ", FACTION_SUGGESTIONS))
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        FactionType faction = parsedOpt.get();
        ResourceLocation factionId = new ResourceLocation(MythicContractsMod.MOD_ID, faction.getId());
        return ModCapabilities.get(player).map(data -> {
            int next = Mth.clamp(data.getPlayerData().getFaction().getReputation(factionId) + delta, -10000, 10000);
            data.getPlayerData().getFaction().setReputation(factionId, next);
            ModCapabilities.sync(player);
            String signedDelta = delta >= 0 ? ("+" + delta) : Integer.toString(delta);
            ctx.getSource().sendSuccess(
                    () -> Component.literal("✦ Reputação com '" + faction.getDisplayName() + "' ajustada em "
                                    + signedDelta + " (total: " + next + ")")
                            .withStyle(ChatFormatting.GOLD),
                    false
            );
            return 1;
        }).orElse(0);
    }

    private static int executeCompatReport(CommandContext<CommandSourceStack> ctx) {
        List<ModCompatRegistry.IntegrationStatus> statuses = ModCompatRegistry.getIntegrationStatuses();
        int primaryLoaded = ModCompatRegistry.countLoadedPrimaryIntegrations();

        ctx.getSource().sendSuccess(
                () -> Component.literal("✦ [Mythic Contracts] Integrações Ativas (" + primaryLoaded + "/7 principais):")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                false
        );
        for (ModCompatRegistry.IntegrationStatus st : statuses) {
            ChatFormatting color = st.loaded() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
            String mark = st.loaded() ? "✔ " : "○ ";
            ctx.getSource().sendSuccess(
                    () -> Component.literal(mark + st.displayName() + " (" + st.modId() + ") — " + st.hookSummary())
                            .withStyle(color),
                    false
            );
        }
        return 1;
    }
}
