package com.mythiccontracts.cultivation.progression;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket;
import com.mythiccontracts.registry.ModItems;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Serviço compartilhado de ações autoritativas de progressão, infusão e equipamento de Resquícios.
 * Utilizado tanto por itens consumíveis quanto por ações enviadas pela Interface do Cultivador ([U]).
 */
public final class CultivationActionService {

    public record ValidationResult(boolean allowed, String errorMessage) {
    }

    private CultivationActionService() {
    }

    public static ValidationResult validateEquipRemnant(PlayerData data, DivineRemnant remnant) {
        if (data == null || remnant == null) {
            return new ValidationResult(false, "Resquício Divino inválido.");
        }
        if (data.getCultivation().isSpiritShattered()) {
            return new ValidationResult(
                    false,
                    "✖ [Colapso Espiritual] Alma instável! Aguarde a estabilização para trocar de Resquício."
            );
        }
        if (data.getSkills().isDivineOverloaded()) {
            return new ValidationResult(
                    false,
                    "✖ [Sobrecarga Divina] Impossível trocar de Resquício durante a Sobrecarga."
            );
        }
        if (data.getSkills().getUniqueSkillCooldownTicks() > 0) {
            return new ValidationResult(
                    false,
                    "⏳ Aguarde o fim da recarga de [G] para trocar de Resquício."
            );
        }
        DivineSkillDefinition def = DivineSkillRegistry.getForRemnant(remnant);
        if (!def.enabled()) {
            return new ValidationResult(
                    false,
                    "✖ [Resquício Desativado] Este Resquício Divino está desativado."
            );
        }
        int bodyOrder = CultivationService.getCurrentRealm(data, CultivationPath.BODY).order();
        int spiritOrder = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        if (bodyOrder < def.minBodyRealm() || spiritOrder < def.minSpiritRealm()) {
            return new ValidationResult(
                    false,
                    "✖ Requer Corpo R" + def.minBodyRealm() + "+ e Espírito R" + def.minSpiritRealm() + "+ para equipar."
            );
        }
        return new ValidationResult(true, "");
    }

    public static ValidationResult validateProgressionState(PlayerData data) {
        if (data == null) {
            return new ValidationResult(false, "Dados de cultivo indisponíveis.");
        }
        if (data.getCultivation().isSpiritShattered()) {
            return new ValidationResult(
                    false,
                    "✖ [Colapso Espiritual] Não é possível avançar o cultivo com a alma fraturada!"
            );
        }
        return new ValidationResult(true, "");
    }

    public static boolean executeStageAdvancement(
            ServerPlayer player,
            PlayerCoreData coreData,
            CultivationPath path,
            boolean requireInventoryPill
    ) {
        if (player == null || coreData == null || path == null) {
            return false;
        }
        PlayerData playerData = coreData.getPlayerData();
        ValidationResult validation = validateProgressionState(playerData);
        if (!validation.allowed()) {
            player.displayClientMessage(
                    Component.literal(validation.errorMessage()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return false;
        }

        Item requiredPill = path == CultivationPath.BODY
                ? ModItems.BODY_REFINEMENT_PILL.get()
                : ModItems.SPIRIT_CONDENSATION_PILL.get();
        int itemSlot = -1;
        boolean mustConsume = requireInventoryPill && !player.getAbilities().instabuild;
        if (mustConsume) {
            itemSlot = findItemSlot(player.getInventory(), requiredPill);
            if (itemSlot < 0) {
                player.displayClientMessage(
                        Component.literal("✖ Requer 1x " + requiredPill.getDescription().getString() + " no inventário.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }
        }

        CultivationService.AdvancementResult result = CultivationService.advanceStage(playerData, path);
        if (!result.isSuccess()) {
            ChatFormatting color = result.outcome() == CultivationService.AdvancementOutcome.MAX_REALM_REACHED
                    ? ChatFormatting.GOLD
                    : ChatFormatting.YELLOW;
            String prefix = result.outcome() == CultivationService.AdvancementOutcome.MAX_REALM_REACHED ? "✦ " : "⚠ ";
            player.displayClientMessage(Component.literal(prefix + result.message()).withStyle(color), true);
            return false;
        }

        if (mustConsume && itemSlot >= 0) {
            player.getInventory().getItem(itemSlot).shrink(1);
        }

        finalizeProgressionSuccess(player, coreData);

        ServerLevel serverLevel = player.serverLevel();
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(),
                20, 0.45D, 0.6D, 0.45D, 0.05D);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.85F, 1.25F);

        player.displayClientMessage(
                Component.literal("✦ " + result.message()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                true
        );
        return true;
    }

    public static boolean executeBreakthrough(
            ServerPlayer player,
            PlayerCoreData coreData,
            CultivationPath path,
            boolean requireInventoryTome
    ) {
        if (player == null || coreData == null || path == null) {
            return false;
        }
        PlayerData playerData = coreData.getPlayerData();
        ValidationResult validation = validateProgressionState(playerData);
        if (!validation.allowed()) {
            player.displayClientMessage(
                    Component.literal(validation.errorMessage()).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return false;
        }

        Item tomeItem = ModItems.TRIBULATION_TOME.get();
        int itemSlot = -1;
        boolean mustConsume = requireInventoryTome && !player.getAbilities().instabuild;
        if (mustConsume) {
            itemSlot = findItemSlot(player.getInventory(), tomeItem);
            if (itemSlot < 0) {
                player.displayClientMessage(
                        Component.literal("✖ Requer 1x " + tomeItem.getDescription().getString() + " no inventário.")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                return false;
            }
        }

        CultivationService.AdvancementResult result = CultivationService.attemptBreakthrough(playerData, path);
        if (!result.isSuccess()) {
            player.displayClientMessage(Component.literal("⚠ " + result.message()).withStyle(ChatFormatting.YELLOW), true);
            return false;
        }

        if (mustConsume && itemSlot >= 0) {
            player.getInventory().getItem(itemSlot).shrink(1);
        }

        finalizeProgressionSuccess(player, coreData);

        ServerLevel serverLevel = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            serverLevel.addFreshEntity(bolt);
        }

        serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                36, 0.6D, 0.9D, 0.6D, 0.08D);
        ModNetworking.sendToNear(
                new SpawnVisualEffectS2CPacket(
                        SpawnVisualEffectS2CPacket.EffectType.REALM_BREAKTHROUGH_BURST,
                        0,
                        player.position(),
                        player.getLookAngle(),
                        2.5F,
                        java.util.List.of()
                ),
                serverLevel,
                player.position(),
                48.0D
        );
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.15F);

        player.displayClientMessage(
                Component.literal("⚡ [TRIBULAÇÃO SUPERADA] " + result.message())
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                false
        );
        return true;
    }

    public static boolean executeEquipRemnant(ServerPlayer player, PlayerCoreData coreData, DivineRemnant remnant) {
        if (player == null || coreData == null || remnant == null) {
            return false;
        }
        ValidationResult validation = validateEquipRemnant(coreData.getPlayerData(), remnant);
        if (!validation.allowed()) {
            ChatFormatting color = coreData.getPlayerData().getCultivation().isSpiritShattered()
                    ? ChatFormatting.DARK_PURPLE
                    : (coreData.getPlayerData().getSkills().isDivineOverloaded() ? ChatFormatting.RED : ChatFormatting.YELLOW);
            player.displayClientMessage(Component.literal(validation.errorMessage()).withStyle(color, ChatFormatting.BOLD), true);
            return false;
        }

        coreData.getPlayerData().getSkills().setPrimaryEquippedSkill(remnant.getId());
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.75F, 1.15F);
        player.displayClientMessage(
                Component.literal("✦ Resquício Divino equipado: " + remnant.getDisplayName())
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                true
        );
        ModCapabilities.sync(player);
        return true;
    }

    public static void executeToggleInfusion(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }
        if (data.getPlayerData().getCultivation().isSpiritShattered()) {
            float sec = data.getPlayerData().getCultivation().getSpiritShatterTicks() / 20.0F;
            player.displayClientMessage(
                    Component.literal(String.format(Locale.ROOT,
                                    "✖ [Colapso Espiritual] Impossível ativar a Infusão por %.1fs!", sec))
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        boolean active = !data.isManaInfusionActive();
        if (active && !EnergyService.canActivateInfusion(data.getPlayerData())) {
            player.displayClientMessage(
                    Component.literal("✖ Qi insuficiente para ativar a Infusão de Mana (mínimo: 5.0 Qi).")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        data.setManaInfusionActive(active);
        ModCapabilities.syncEnergy(player);

        ServerLevel serverLevel = player.serverLevel();
        if (active) {
            player.displayClientMessage(
                    Component.literal("✦ Infusão de Mana: ATIVADA (Corpo Nv." + data.getBodyRealm()
                                    + " / Espírito Nv." + data.getSpiritRealm() + ")")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    true
            );
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    20,
                    0.4D,
                    0.6D,
                    0.4D,
                    0.03D
            );
            serverLevel.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.25F
            );
        } else {
            player.displayClientMessage(
                    Component.literal("✦ Infusão de Mana: DESATIVADA")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    12,
                    0.3D,
                    0.5D,
                    0.3D,
                    0.02D
            );
            serverLevel.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS,
                    0.6F,
                    1.1F
            );
        }
    }

    private static void finalizeProgressionSuccess(ServerPlayer player, PlayerCoreData data) {
        ContractManager.refreshPlayerContractAttributes(player, data);
        var cult = data.getPlayerData().getCultivation();
        data.setCurrentQi(data.getMaxQi());
        cult.setCurrentStamina(cult.getMaxStamina());
        if (!cult.isSpiritShattered()) {
            cult.setCurrentSpiritualIntegrity(cult.getMaxSpiritualIntegrity());
        }
        var skills = data.getPlayerData().getSkills();
        if (!skills.isDivineOverloaded()) {
            skills.setCurrentDivineResistance(skills.getMaxDivineResistance());
        }
        ModCapabilities.sync(player);
    }

    private static int findItemSlot(Inventory inventory, Item item) {
        if (inventory == null || item == null) {
            return -1;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return i;
            }
        }
        return -1;
    }
}
