package com.mythiccontracts.contract;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.attribute.AttributeService;
import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class LuciferPactContract {
    public static final ResourceLocation ID = new ResourceLocation(MythicContractsMod.MOD_ID, "lucifer_pact");
    public static final String DISPLAY_NAME = "Pacto de Lúcifer";

    public static final UUID MAX_HEALTH_MODIFIER_UUID = UUID.fromString("c8a47b1e-5d92-4f10-9a3b-112233445566");
    public static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("d9b58c2f-6e03-4a21-8b4c-665544332211");

    private static final String MAX_HEALTH_MODIFIER_NAME = "Lucifer Pact Max Health Penalty";
    private static final String ATTACK_DAMAGE_MODIFIER_NAME = "Lucifer Pact Attack Damage Bonus";

    public static final double MAX_HEALTH_PENALTY = -8.0D; // -4 coracoes
    public static final double ATTACK_DAMAGE_BONUS = 0.40D; // +40% dano fisico

    private LuciferPactContract() {
    }

    public static boolean isAllowedForFaction(FactionType faction) {
        return faction != FactionType.JURAMENTADOS_DA_LUZ;
    }

    public static void applyModifiers(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeService.applySingleModifier(
                player,
                Attributes.MAX_HEALTH,
                MAX_HEALTH_MODIFIER_UUID,
                MAX_HEALTH_MODIFIER_NAME,
                MAX_HEALTH_PENALTY,
                AttributeModifier.Operation.ADDITION
        );

        AttributeService.applySingleModifier(
                player,
                Attributes.ATTACK_DAMAGE,
                ATTACK_DAMAGE_MODIFIER_UUID,
                ATTACK_DAMAGE_MODIFIER_NAME,
                ATTACK_DAMAGE_BONUS,
                AttributeModifier.Operation.MULTIPLY_BASE
        );
    }

    public static void removeModifiers(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeService.removeModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_MODIFIER_UUID);
        AttributeService.removeModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_MODIFIER_UUID);
    }
}
