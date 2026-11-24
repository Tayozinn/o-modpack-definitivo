package com.mythiccontracts.item;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Itens de progressão in-game do sistema Murim Duplo (Pílulas de Etapa e Tomo de Quebra de Reino).
 */
public class CultivationAdvancementItem extends Item {

    public enum Mode {
        BODY_STAGE,
        SPIRIT_STAGE,
        REALM_BREAKTHROUGH
    }

    private final Mode mode;

    public CultivationAdvancementItem(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        boolean[] consumed = {false};
        ModCapabilities.get(serverPlayer).ifPresent(data -> {
            switch (this.mode) {
                case BODY_STAGE -> consumed[0] = CultivationActionService.executeStageAdvancement(
                        serverPlayer, data, CultivationPath.BODY, false);
                case SPIRIT_STAGE -> consumed[0] = CultivationActionService.executeStageAdvancement(
                        serverPlayer, data, CultivationPath.SPIRIT, false);
                case REALM_BREAKTHROUGH -> consumed[0] = tryRealmBreakthrough(serverPlayer, data);
            }

            if (consumed[0] && !serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }
        });

        return consumed[0]
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }

    private static boolean tryRealmBreakthrough(ServerPlayer player, PlayerCoreData coreData) {
        PlayerData pData = coreData.getPlayerData();
        boolean bodyReady = CultivationService.isReadyForBreakthrough(pData, CultivationPath.BODY);
        boolean spiritReady = CultivationService.isReadyForBreakthrough(pData, CultivationPath.SPIRIT);

        if (!bodyReady && !spiritReady) {
            player.displayClientMessage(
                    Component.literal("⚠ Nenhum caminho está pronto para Quebra de Reino (exige estágio final no reino atual).")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        CultivationPath targetPath = player.isShiftKeyDown()
                ? (spiritReady ? CultivationPath.SPIRIT : CultivationPath.BODY)
                : (bodyReady ? CultivationPath.BODY : CultivationPath.SPIRIT);

        return CultivationActionService.executeBreakthrough(player, coreData, targetPath, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (this.mode) {
            case BODY_STAGE -> tooltip.add(Component.literal("Avança 1 Etapa do Cultivo Corporal no reino atual.")
                    .withStyle(ChatFormatting.GRAY));
            case SPIRIT_STAGE -> tooltip.add(Component.literal("Avança 1 Etapa do Cultivo Espiritual no reino atual.")
                    .withStyle(ChatFormatting.GRAY));
            case REALM_BREAKTHROUGH -> {
                tooltip.add(Component.literal("Rompe para o próximo Reino quando estiver no estágio final.")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Clique Direito: Corpo | Shift + Clique: Espírito.")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
