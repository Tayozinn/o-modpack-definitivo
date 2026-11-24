package com.mythiccontracts.item;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.faction.FactionRegistry;
import com.mythiccontracts.network.ClientPacketHandler;
import com.mythiccontracts.subjugation.SubjugationBalance;
import com.mythiccontracts.subjugation.SubjugationService;
import com.mythiccontracts.subjugation.SubjugationTier;
import com.mythiccontracts.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Selo de Subjugação (Opção 3 — Relíquia R1 & Catalisador Opcional de Chefes).
 * Preserva ID, receita KubeJS e quests FTB 100% intactos:
 * - No R1 (antes da habilidade {@code soul_subjugation}), sela diretamente como relíquia.
 * - Com a habilidade desbloqueada, atua como Catalisador na Disputa Espiritual (-50% custo, +10% HP,
 *   início em 65% de dominância e proteção contra Colapso Espiritual).
 */
public class SubjugationSealItem extends Item {
    public static final String TAG_SUBJUGATED_OWNER = "SubjugatedOwner";
    public static final String ENTITY_TAG_SUBJUGATED = "mc_subjugated";

    public SubjugationSealItem(Properties properties) {
        super(properties);
    }

    public static boolean isSubjugated(Entity entity) {
        return entity != null && entity.getPersistentData().hasUUID(TAG_SUBJUGATED_OWNER);
    }

    @Nullable
    public static UUID getOwnerUUID(Entity entity) {
        if (!isSubjugated(entity)) {
            return null;
        }
        return entity.getPersistentData().getUUID(TAG_SUBJUGATED_OWNER);
    }

    public static boolean isAlliedWithOwner(Entity target, UUID ownerUUID) {
        if (target == null || ownerUUID == null) {
            return false;
        }
        if (ownerUUID.equals(target.getUUID())) {
            return true;
        }
        UUID targetOwner = getOwnerUUID(target);
        return ownerUUID.equals(targetOwner);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!target.getType().is(ModTags.SUBJUGATABLE)) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal("§cEsta entidade não pode ser subjugada pelo Selo."), true);
            }
            return InteractionResult.FAIL;
        }

        if (isSubjugated(target)) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal("§cEsta entidade já foi subjugada e possui um mestre."), true);
            }
            return InteractionResult.FAIL;
        }

        if (!target.isAlive() || target.getHealth() <= 0.0F) {
            return InteractionResult.FAIL;
        }

        PlayerCoreData data = ModCapabilities.get(player).orElse(null);
        if (data == null) {
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            boolean hasSkill = data.getPlayerData().getSkills().hasSkillUnlocked(SubjugationService.SOUL_SUBJUGATION_ID);
            if (hasSkill) {
                boolean started = SubjugationService.tryStartClashWithTarget(serverPlayer, data, target, true);
                return started ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }

            // Modo Relíquia R1 (antes do desbloqueio da habilidade)
            float threshold = SubjugationBalance.calculateEffectiveHpThreshold(data.getPlayerData(), SubjugationTier.BOSS, false);
            float legacyThresh = FactionRegistry.getSubjugationThreshold(data.getFaction());
            float finalThresh = Math.max(threshold, legacyThresh);
            float healthRatio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
            if (healthRatio > finalThresh) {
                String currentPct = String.format(Locale.ROOT, "%.1f%%", healthRatio * 100.0F);
                String requiredPct = String.format(Locale.ROOT, "%.0f%%", finalThresh * 100.0F);
                player.displayClientMessage(Component.literal(
                        "§cA entidade resiste ao Selo! Vida atual: §e" + currentPct + " §c(Necessário: <= §6" + requiredPct + "§c)"
                ), true);
                return InteractionResult.FAIL;
            }
            boolean captured = SubjugationService.tryLegacyRelicCapture(serverPlayer, data, target, stack);
            return captured ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PlayerCoreData data = ModCapabilities.get(serverPlayer).orElse(null);
            if (data == null || data.getPlayerData().getSubjugation().getBoundEntities().isEmpty()) {
                serverPlayer.displayClientMessage(
                        Component.literal("§cVocê não possui nenhuma entidade selada armazenada."),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }
            boolean ok = SubjugationService.summonLastStoredServant(serverPlayer, data);
            return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("Catalisador & Relíquia de Subjugação Espiritual.")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("• R1 (Sem Habilidade): Sela alvos enfraquecidos.")
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("• Catalisador (Com Habilidade): -50% custo, +10% limiar HP, 65% início e protege contra Colapso!")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltipComponents.add(Component.literal("Shift + Clique Direito para invocar um servo selado.")
                .withStyle(ChatFormatting.AQUA));

        if (level != null && level.isClientSide()) {
            PlayerCoreData clientData = DistExecutor.unsafeCallWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientPacketHandler.getClientPlayerCoreData()
            );
            if (clientData != null) {
                FactionType activeFaction = clientData.getFaction();
                int cap = SubjugationBalance.calculateMaxBoundCapacity(clientData.getPlayerData());
                int count = clientData.getSubjugationEntitiesCountSafe();
                tooltipComponents.add(Component.literal("• Servos Vinculados: " + count + "/" + cap + " (" + activeFaction.getDisplayName() + ")")
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    }
}
