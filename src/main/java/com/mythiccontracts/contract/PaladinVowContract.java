package com.mythiccontracts.contract;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.AbstractVillager;

public final class PaladinVowContract {
    public static final ResourceLocation ID = new ResourceLocation(MythicContractsMod.MOD_ID, "paladin_vow");
    public static final String DISPLAY_NAME = "Voto de Paladino";
    public static final float PASSIVE_ABSORPTION_AMOUNT = 4.0F; // 2 coracoes

    private PaladinVowContract() {
    }

    public static boolean isAllowedForFaction(FactionType faction) {
        return faction != FactionType.IRMANDADE_DIVIDA_ETERNA;
    }

    public static void onPlayerTick(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }
        if (!data.hasContract(ID) || data.isVowBroken()) {
            return;
        }
        applyPassiveAbsorption(player, data);
    }

    public static void applyPassiveAbsorption(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null || !data.hasContract(ID) || data.isVowBroken()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0, true, false, true));
        if (player.getAbsorptionAmount() < PASSIVE_ABSORPTION_AMOUNT) {
            player.setAbsorptionAmount(PASSIVE_ABSORPTION_AMOUNT);
        }
    }

    public static void removeEffects(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.removeEffect(MobEffects.ABSORPTION);
        if (player.getAbsorptionAmount() > 0.0F) {
            player.setAbsorptionAmount(0.0F);
        }
    }

    public static void onPlayerAttackVillager(ServerPlayer attacker, AbstractVillager target, PlayerCoreData data) {
        if (attacker == null || target == null || data == null) {
            return;
        }
        if (!data.hasContract(ID)) {
            return;
        }

        // Quebra do Voto de Paladino
        data.removeContract(ID);
        data.setVowBroken(true);

        removeEffects(attacker);
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true, true));
        attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, true, true));

        ModCapabilities.sync(attacker);

        attacker.serverLevel().playSound(
                null,
                attacker.getX(),
                attacker.getY(),
                attacker.getZ(),
                SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS,
                0.75F,
                0.7F
        );

        attacker.sendSystemMessage(
                Component.literal("✖ [QUEBRA DE VOTO] Você ergueu a mão contra um inocente! A Luz o abandona e seu Voto de Paladino foi despedaçado!")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
    }
}
