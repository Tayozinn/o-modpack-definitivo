package com.mythiccontracts.contract;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.attribute.AttributeModifierSpec;
import com.mythiccontracts.attribute.AttributeService;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.faction.FactionDefinition;
import com.mythiccontracts.faction.FactionRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ContractManager {

    private static final Map<ResourceLocation, ContractDefinition> BUILTIN_FALLBACKS = createBuiltinFallbacks();
    private static volatile Map<ResourceLocation, ContractDefinition> loadedContracts = BUILTIN_FALLBACKS;

    private ContractManager() {
    }

    public record ContractActionResult(boolean success, Component message) {
    }

    public record ContractEligibility(boolean eligible, String reason) {
    }

    private static Map<ResourceLocation, ContractDefinition> createBuiltinFallbacks() {
        Map<ResourceLocation, ContractDefinition> map = new LinkedHashMap<>();
        map.put(PaladinVowContract.ID, new ContractDefinition(
                PaladinVowContract.ID,
                PaladinVowContract.DISPLAY_NAME,
                "Concede 2 corações de Absorção passiva. Quebra ao atacar aldeões.",
                Set.of(),
                EnumSet.of(FactionType.IRMANDADE_DIVIDA_ETERNA),
                List.of(),
                PaladinVowContract.PASSIVE_ABSORPTION_AMOUNT,
                -1,
                true
        ));
        map.put(LuciferPactContract.ID, new ContractDefinition(
                LuciferPactContract.ID,
                LuciferPactContract.DISPLAY_NAME,
                "-4 corações de vida máxima em troca de +40% de dano físico.",
                Set.of(),
                EnumSet.of(FactionType.JURAMENTADOS_DA_LUZ),
                List.of(
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.max_health"),
                                AttributeModifier.Operation.ADDITION,
                                LuciferPactContract.MAX_HEALTH_PENALTY,
                                LuciferPactContract.MAX_HEALTH_MODIFIER_UUID
                        ),
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.attack_damage"),
                                AttributeModifier.Operation.MULTIPLY_BASE,
                                LuciferPactContract.ATTACK_DAMAGE_BONUS,
                                LuciferPactContract.ATTACK_DAMAGE_MODIFIER_UUID
                        )
                ),
                0.0D,
                -1,
                false
        ));
        ResourceLocation bloodOathId = new ResourceLocation(MythicContractsMod.MOD_ID, "blood_oath");
        map.put(bloodOathId, new ContractDefinition(
                bloodOathId,
                "Juramento de Sangue",
                "Acelera seus golpes (+15% velocidade de ataque e +2.0 de dano físico), mas proibido aos Juramentados da Luz.",
                Set.of(),
                EnumSet.of(FactionType.JURAMENTADOS_DA_LUZ),
                List.of(
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.attack_speed"),
                                AttributeModifier.Operation.MULTIPLY_BASE,
                                0.15D,
                                null
                        ),
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.attack_damage"),
                                AttributeModifier.Operation.ADDITION,
                                2.0D,
                                null
                        )
                ),
                0.0D,
                -1,
                false
        ));
        ResourceLocation throneBreakerId = new ResourceLocation(MythicContractsMod.MOD_ID, "throne_breaker_seal");
        map.put(throneBreakerId, new ContractDefinition(
                throneBreakerId,
                "Selo Quebra-Tronos",
                "Pacto exclusivo dos Quebradores de Tronos: +4.0 de Armadura e +20% de Resistência a Repulsão.",
                EnumSet.of(FactionType.QUEBRADORES_DE_TRONOS),
                Set.of(),
                List.of(
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.armor"),
                                AttributeModifier.Operation.ADDITION,
                                4.0D,
                                null
                        ),
                        new ContractDefinition.AttributeEntry(
                                new ResourceLocation("minecraft", "generic.knockback_resistance"),
                                AttributeModifier.Operation.ADDITION,
                                0.20D,
                                null
                        )
                ),
                0.0D,
                -1,
                false
        ));
        ResourceLocation veilResonanceId = new ResourceLocation(MythicContractsMod.MOD_ID, "veil_resonance_oath");
        map.put(veilResonanceId, new ContractDefinition(
                veilResonanceId,
                "Voto de Ressonância do Véu",
                "Harmoniza o fluxo espiritual: +2.0 Qi/s, +25 Int. Espiritual Máx e +10% (+0.8/s) de recuperação espiritual.",
                EnumSet.of(FactionType.ERRANTES_DO_VEU, FactionType.NONE),
                Set.of(),
                List.of(),
                0.0D,
                -1,
                false,
                1,
                2,
                0,
                15,
                2.0F,
                25.0F,
                "Disponível aos Errantes do Véu ou Cultivadores Livres a partir do Espírito R2.",
                0.8F,
                0.10F
        ));
        return Collections.unmodifiableMap(map);
    }

    public static void setLoadedContracts(Map<ResourceLocation, ContractDefinition> parsed) {
        Map<ResourceLocation, ContractDefinition> merged = new LinkedHashMap<>(BUILTIN_FALLBACKS);
        if (parsed != null) {
            merged.putAll(parsed);
        }
        loadedContracts = Collections.unmodifiableMap(merged);
    }

    public static void resetToBuiltins() {
        loadedContracts = BUILTIN_FALLBACKS;
    }

    public static Collection<ResourceLocation> getRegisteredContractIds() {
        return loadedContracts.keySet();
    }

    @Nullable
    public static ContractDefinition getDefinition(ResourceLocation contractId) {
        return contractId != null ? loadedContracts.get(contractId) : null;
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
        return contractId != null && loadedContracts.containsKey(contractId);
    }

    public static String getContractDisplayName(ResourceLocation contractId) {
        ContractDefinition def = getDefinition(contractId);
        if (def != null) {
            return def.displayName();
        }
        return contractId != null ? contractId.toString() : "Desconhecido";
    }

    public static void refreshPlayerContractAttributes(ServerPlayer player, PlayerCoreData data) {
        refreshPlayerContractAttributes(player, data, false);
    }

    public static void refreshPlayerContractAttributes(ServerPlayer player, PlayerCoreData data, boolean healToFull) {
        if (player == null || data == null) {
            return;
        }

        AttributeService.runPreservingHealthRatio(player, healToFull, () -> {
            EnergyService.recalculateMaxQi(data.getPlayerData());
            CultivationBalance.applyCultivationAttributes(player, data);
            clearContractAttributeModifiers(player);
            AttributeService.clearByNamePrefix(player, FactionDefinition.MODIFIER_NAME_PREFIX);

            FactionDefinition factionDef = FactionRegistry.getForType(data.getFaction());
            if (!factionDef.passiveModifiers().isEmpty()) {
                List<AttributeModifierSpec> factionSpecs = new ArrayList<>(factionDef.passiveModifiers().size());
                for (ContractDefinition.AttributeEntry entry : factionDef.passiveModifiers()) {
                    Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(entry.attributeId());
                    if (attr != null) {
                        factionSpecs.add(new AttributeModifierSpec(
                                attr,
                                entry.operation(),
                                entry.amount(),
                                factionDef.resolveModifierUuid(entry),
                                factionDef.resolveModifierName(entry)
                        ));
                    }
                }
                AttributeService.applyModifiers(player, "faction:" + factionDef.id(), factionSpecs);
            }

            for (ResourceLocation contractId : data.getActiveContracts()) {
                ContractDefinition def = getDefinition(contractId);
                if (def == null || def.attributeModifiers().isEmpty()) {
                    continue;
                }
                List<AttributeModifierSpec> specs = new ArrayList<>(def.attributeModifiers().size());
                for (ContractDefinition.AttributeEntry entry : def.attributeModifiers()) {
                    Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(entry.attributeId());
                    if (attribute == null) {
                        continue;
                    }
                    specs.add(new AttributeModifierSpec(
                            attribute,
                            entry.operation(),
                            entry.amount(),
                            def.resolveModifierUuid(entry),
                            def.resolveModifierName(entry)
                    ));
                }
                AttributeService.applyModifiers(player, "contract:" + contractId, specs);
            }
        });

        if (data.hasContract(PaladinVowContract.ID) && !data.isVowBroken()) {
            PaladinVowContract.applyPassiveAbsorption(player, data);
        }
    }

    private static void clearContractAttributeModifiers(ServerPlayer player) {
        LuciferPactContract.removeModifiers(player);
        AttributeService.clearByNamePrefix(player, ContractDefinition.MODIFIER_NAME_PREFIX);
    }

    public static void onPlayerPeriodicTick(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }
        double maxAbsorption = 0.0D;
        int bestRegen = -1;

        for (ResourceLocation contractId : data.getActiveContracts()) {
            if (PaladinVowContract.ID.equals(contractId) && data.isVowBroken()) {
                continue;
            }
            ContractDefinition def = getDefinition(contractId);
            if (def == null) {
                continue;
            }
            if (def.periodicAbsorptionHearts() > maxAbsorption) {
                maxAbsorption = def.periodicAbsorptionHearts();
            }
            if (def.periodicRegenAmplifier() > bestRegen) {
                bestRegen = def.periodicRegenAmplifier();
            }
        }

        if (maxAbsorption > 0.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
            if (player.getAbsorptionAmount() < (float) maxAbsorption) {
                player.setAbsorptionAmount((float) maxAbsorption);
            }
        }

        if (bestRegen >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, bestRegen, true, false, true));
        }
    }

    public static void onPlayerAttackVillager(ServerPlayer attacker, AbstractVillager target, PlayerCoreData data) {
        if (attacker == null || target == null || data == null) {
            return;
        }
        List<ResourceLocation> brokenIds = new ArrayList<>();
        for (ResourceLocation contractId : data.getActiveContracts()) {
            ContractDefinition def = getDefinition(contractId);
            if (def != null && def.breakOnAttackVillager()) {
                brokenIds.add(contractId);
            }
        }
        if (brokenIds.isEmpty()) {
            return;
        }

        for (ResourceLocation id : brokenIds) {
            if (PaladinVowContract.ID.equals(id)) {
                PaladinVowContract.onPlayerAttackVillager(attacker, target, data);
            } else {
                data.removeContract(id);
                attacker.sendSystemMessage(
                        Component.literal("✖ [QUEBRA DE VOTO] O contrato '" + getContractDisplayName(id)
                                        + "' foi quebrado ao atacar um inocente!")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                );
            }
        }
        refreshPlayerContractAttributes(attacker, data);
        attacker.serverLevel().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.65F, 0.75F);
        ModCapabilities.sync(attacker);
    }

    public static ContractEligibility evaluateEligibility(PlayerData data, ContractDefinition def) {
        if (data == null || def == null) {
            return new ContractEligibility(false, "Dados de contrato ou cultivador inválidos.");
        }
        FactionType faction = FactionType.fromString(data.getFaction().getCurrentFactionId().getPath());
        if (!def.isAllowedForFaction(faction)) {
            return new ContractEligibility(false,
                    "✖ A facção '" + faction.getDisplayName() + "' proíbe o contrato " + def.displayName() + "!");
        }
        int bodyOrder = CultivationService.getCurrentRealm(data, CultivationPath.BODY).order();
        int spiritOrder = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        if (bodyOrder < def.minBodyRealm()) {
            return new ContractEligibility(false,
                    "✖ Requer Cultivo Corporal R" + def.minBodyRealm() + "+ para firmar " + def.displayName() + ".");
        }
        if (spiritOrder < def.minSpiritRealm()) {
            return new ContractEligibility(false,
                    "✖ Requer Cultivo Espiritual R" + def.minSpiritRealm() + "+ para firmar " + def.displayName() + ".");
        }
        if (def.minReputation() > 0) {
            ResourceLocation factionId = data.getFaction().getCurrentFactionId();
            int currentRep = data.getFaction().getReputation(factionId);
            if (currentRep < def.minReputation()) {
                return new ContractEligibility(false,
                        "✖ Requer reputação " + def.minReputation() + "+ na facção (atual: " + currentRep + ").");
            }
        }
        return new ContractEligibility(true, "");
    }

    public static float getTotalQiRegenBonus(PlayerData data) {
        if (data == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (ResourceLocation id : data.getContracts().getActiveContractIds()) {
            if (PaladinVowContract.ID.equals(id) && data.getContracts().isVowBroken()) {
                continue;
            }
            ContractDefinition def = getDefinition(id);
            if (def != null && def.qiRegenBonus() != 0.0F) {
                total += def.qiRegenBonus();
            }
        }
        return total;
    }

    public static float getTotalSpiritualIntegrityBonus(PlayerData data) {
        if (data == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (ResourceLocation id : data.getContracts().getActiveContractIds()) {
            if (PaladinVowContract.ID.equals(id) && data.getContracts().isVowBroken()) {
                continue;
            }
            ContractDefinition def = getDefinition(id);
            if (def != null && def.spiritualIntegrityBonus() != 0.0F) {
                total += def.spiritualIntegrityBonus();
            }
        }
        return total;
    }

    public static float getTotalSpiritualRegenFlatBonus(PlayerData data) {
        if (data == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (ResourceLocation id : data.getContracts().getActiveContractIds()) {
            if (PaladinVowContract.ID.equals(id) && data.getContracts().isVowBroken()) {
                continue;
            }
            ContractDefinition def = getDefinition(id);
            if (def != null && def.spiritualRegenFlatBonus() != 0.0F) {
                total += def.spiritualRegenFlatBonus();
            }
        }
        return total;
    }

    public static float getTotalSpiritualRegenPctBonus(PlayerData data) {
        if (data == null) {
            return 0.0F;
        }
        float total = 0.0F;
        for (ResourceLocation id : data.getContracts().getActiveContractIds()) {
            if (PaladinVowContract.ID.equals(id) && data.getContracts().isVowBroken()) {
                continue;
            }
            ContractDefinition def = getDefinition(id);
            if (def != null && def.spiritualRegenPctBonus() != 0.0F) {
                total += def.spiritualRegenPctBonus();
            }
        }
        return total;
    }

    public static ContractActionResult grantContract(ServerPlayer player, ResourceLocation contractId) {
        if (player == null) {
            return new ContractActionResult(false, Component.literal("Jogador inválido.").withStyle(ChatFormatting.RED));
        }
        ContractDefinition def = getDefinition(contractId);
        if (def == null) {
            return new ContractActionResult(
                    false,
                    Component.literal("Contrato desconhecido: '" + (contractId != null ? contractId : "null") + "'.")
                            .withStyle(ChatFormatting.RED)
            );
        }

        return ModCapabilities.get(player).map(data -> {
            ContractEligibility eligibility = evaluateEligibility(data.getPlayerData(), def);
            if (!eligibility.eligible()) {
                return new ContractActionResult(
                        false,
                        Component.literal(eligibility.reason()).withStyle(ChatFormatting.RED)
                );
            }

            if (PaladinVowContract.ID.equals(contractId)) {
                data.setVowBroken(false);
            }

            boolean added = data.addContract(contractId);
            if (added && def.reputationReward() != 0) {
                ResourceLocation curFacId = data.getPlayerData().getFaction().getCurrentFactionId();
                data.getPlayerData().getFaction().addReputation(curFacId, def.reputationReward());
            }
            refreshPlayerContractAttributes(player, data);
            ModCapabilities.sync(player);

            if (!added) {
                return new ContractActionResult(
                        true,
                        Component.literal("✦ Contrato '" + def.displayName() + "' (" + contractId + ") atualizado e reafirmado.")
                                .withStyle(ChatFormatting.YELLOW)
                );
            }

            return new ContractActionResult(
                    true,
                    Component.literal("✦ Contrato '" + def.displayName() + "' (" + contractId + ") firmado com sucesso!")
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
        if (!isKnownContract(contractId)) {
            return new ContractActionResult(
                    false,
                    Component.literal("Contrato desconhecido: '" + (contractId != null ? contractId : "null") + "'.")
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
        List<ResourceLocation> toRemove = new ArrayList<>();

        for (ResourceLocation contractId : data.getActiveContracts()) {
            ContractDefinition def = getDefinition(contractId);
            if (def != null && !def.isAllowedForFaction(faction)) {
                toRemove.add(contractId);
            }
        }

        for (ResourceLocation contractId : toRemove) {
            data.removeContract(contractId);
            if (PaladinVowContract.ID.equals(contractId)) {
                PaladinVowContract.removeEffects(player);
            }
            player.sendSystemMessage(
                    Component.literal("⚠ O contrato '" + getContractDisplayName(contractId)
                                    + "' foi revogado por incompatibilidade com a facção " + faction.getDisplayName() + ".")
                            .withStyle(ChatFormatting.RED)
            );
        }

        refreshPlayerContractAttributes(player, data);
        ModCapabilities.sync(player);
    }
}
