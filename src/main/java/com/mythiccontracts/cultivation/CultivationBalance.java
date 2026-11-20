package com.mythiccontracts.cultivation;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Classe utilitária de Game Design e Balanceamento para o sistema de Cultivo (Corpo e Espírito)
 * e Infusão de Mana (Tecla V) no Minecraft 1.20.1 (Forge 47.3.0 / Mojang Mappings).
 *
 * <h3>1. Curva de Dano Físico Passivo (Cultivo do Corpo)</h3>
 * <ul>
 *   <li>Operação: {@link AttributeModifier.Operation#ADDITION} no atributo {@link Attributes#ATTACK_DAMAGE}.</li>
 *   <li>Fórmula: {@code 0.5D * R_body * (R_body + S_body - 1)}, onde {@code R_body in [1..7]} e {@code S_body in [1..3]}.</li>
 *   <li>Progressão: de {@code +0.5} (Reino 1 - Base) até {@code +31.5} (Reino 7 - Despertar) ao longo dos 21 patamares.</li>
 *   <li>Soma-se de forma limpa ao dano base da arma (Vanilla, Simply Swords, Iron's Spells) e escala
 *       proporcionalmente com modificadores {@code MULTIPLY_BASE} (como o +40% do Pacto de Lúcifer).</li>
 * </ul>
 *
 * <h3>2. Fórmula Consolidada da Infusão de Mana (V) no {@code LivingHurtEvent}</h3>
 * <ul>
 *   <li>{@code Dano Final = baseDamage + bodyInfusionBuff + spiritInfusionBonus}</li>
 *   <li>{@code bodyInfusionMultiplier = 0.20F * (R_body - 1) + 0.05F * S_body} (de +5% em 1-1 até +135% em 7-3).</li>
 *   <li>{@code bodyInfusionBuff = baseDamage * bodyInfusionMultiplier}.</li>
 *   <li>{@code spiritInfusionBonus = 1.0F * R_spirit * (R_spirit + S_spirit - 1)} (de +1.0 em 1-1 até +63.0 em 7-3).</li>
 *   <li>Todo o dano é injetado em uma única chamada {@code event.setAmount(...)} dentro do {@code LivingHurtEvent},
 *       evitando que os i-frames ({@code invulnerableTime = 20}) do Minecraft descartem uma segunda instância de dano
 *       e eliminando qualquer risco de recursão infinita via {@code target.hurt(...)}.</li>
 * </ul>
 */
public final class CultivationBalance {

    public static final UUID BODY_CULTIVATION_ATTACK_UUID = UUID.fromString("a7f31c90-4b2e-4d8a-9c15-778899aabbcc");
    public static final String BODY_CULTIVATION_ATTACK_NAME = "mythic_contracts.body_cultivation_attack";

    private CultivationBalance() {
    }

    /**
     * Retorna o índice linear do patamar de evolução (1 a 21) para fins de exibição ou telemetria.
     */
    public static int getProgressionTier(int realm, int stage) {
        int r = Mth.clamp(realm, PlayerCoreData.MIN_REALM, PlayerCoreData.MAX_REALM);
        int s = Mth.clamp(stage, PlayerCoreData.MIN_STAGE, PlayerCoreData.MAX_STAGE);
        return (r - 1) * PlayerCoreData.MAX_STAGE + s;
    }

    /**
     * Calcula o bônus passivo de Dano Físico ({@link Attributes#ATTACK_DAMAGE} com {@code Operation.ADDITION})
     * concedido pelo Cultivo do Corpo.
     * <p>
     * Fórmula: {@code 0.5D * R * (R + S - 1)}
     * <ul>
     *   <li>Reino 1 (Etapas 1, 2, 3): +0.5, +1.0, +1.5</li>
     *   <li>Reino 2 (Etapas 1, 2, 3): +2.0, +3.0, +4.0</li>
     *   <li>Reino 3 (Etapas 1, 2, 3): +4.5, +6.0, +7.5</li>
     *   <li>Reino 4 (Etapas 1, 2, 3): +8.0, +10.0, +12.0</li>
     *   <li>Reino 5 (Etapas 1, 2, 3): +12.5, +15.0, +17.5</li>
     *   <li>Reino 6 (Etapas 1, 2, 3): +18.0, +21.0, +24.0</li>
     *   <li>Reino 7 (Etapas 1, 2, 3): +24.5, +28.0, +31.5</li>
     * </ul>
     */
    public static double calculateBodyPassiveAttackBonus(int bodyRealm, int bodyStage) {
        int r = Mth.clamp(bodyRealm, PlayerCoreData.MIN_REALM, PlayerCoreData.MAX_REALM);
        int s = Mth.clamp(bodyStage, PlayerCoreData.MIN_STAGE, PlayerCoreData.MAX_STAGE);
        return 0.5D * r * (r + s - 1);
    }

    public static double calculateBodyPassiveAttackBonus(PlayerCoreData data) {
        if (data == null) {
            return calculateBodyPassiveAttackBonus(PlayerCoreData.MIN_REALM, PlayerCoreData.MIN_STAGE);
        }
        return calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
    }

    /**
     * Calcula o multiplicador percentual de amplificação física do Cultivo do Corpo durante a Infusão de Mana (V).
     * <p>
     * Fórmula: {@code 0.20F * (R - 1) + 0.05F * S}
     * <ul>
     *   <li>Cada avanço de Etapa dentro do mesmo Reino adiciona {@code +5%} ({@code +0.05F}).</li>
     *   <li>Cada Quebra de Reino (Etapa 3 -> Etapa 1 do próximo Reino) adiciona {@code +10%} ({@code +0.10F}).</li>
     *   <li>Intervalo: de {@code 0.05F} (+5% em Reino 1 - Base) até {@code 1.35F} (+135% em Reino 7 - Despertar).</li>
     * </ul>
     */
    public static float calculateBodyInfusionMultiplier(int bodyRealm, int bodyStage) {
        int r = Mth.clamp(bodyRealm, PlayerCoreData.MIN_REALM, PlayerCoreData.MAX_REALM);
        int s = Mth.clamp(bodyStage, PlayerCoreData.MIN_STAGE, PlayerCoreData.MAX_STAGE);
        return (0.20F * (r - 1)) + (0.05F * s);
    }

    public static float getBodyInfusionMultiplier(int bodyRealm, int bodyStage) {
        return calculateBodyInfusionMultiplier(bodyRealm, bodyStage);
    }

    /**
     * Calcula a parcela de dano adicional do Cultivo do Corpo na Infusão de Mana ({@code baseDamage * multiplier}).
     */
    public static float calculateBodyInfusionBuff(float baseDamage, int bodyRealm, int bodyStage) {
        float safeBase = Math.max(0.0F, baseDamage);
        return safeBase * calculateBodyInfusionMultiplier(bodyRealm, bodyStage);
    }

    /**
     * Calcula o bônus plano de dano espiritual/Qi concedido pelo Cultivo do Espírito durante a Infusão de Mana (V).
     * <p>
     * Fórmula: {@code 1.0F * R * (R + S - 1)}
     * <ul>
     *   <li>Reino 1 (Etapas 1, 2, 3): +1.0, +2.0, +3.0</li>
     *   <li>Reino 2 (Etapas 1, 2, 3): +4.0, +6.0, +8.0</li>
     *   <li>Reino 3 (Etapas 1, 2, 3): +9.0, +12.0, +15.0</li>
     *   <li>Reino 4 (Etapas 1, 2, 3): +16.0, +20.0, +24.0</li>
     *   <li>Reino 5 (Etapas 1, 2, 3): +25.0, +30.0, +35.0</li>
     *   <li>Reino 6 (Etapas 1, 2, 3): +36.0, +42.0, +48.0</li>
     *   <li>Reino 7 (Etapas 1, 2, 3): +49.0, +56.0, +63.0</li>
     * </ul>
     */
    public static float calculateSpiritInfusionFlatBonus(int spiritRealm, int spiritStage) {
        int r = Mth.clamp(spiritRealm, PlayerCoreData.MIN_REALM, PlayerCoreData.MAX_REALM);
        int s = Mth.clamp(spiritStage, PlayerCoreData.MIN_STAGE, PlayerCoreData.MAX_STAGE);
        return 1.0F * r * (r + s - 1);
    }

    public static float getSpiritInfusionFlatBonus(int spiritRealm, int spiritStage) {
        return calculateSpiritInfusionFlatBonus(spiritRealm, spiritStage);
    }

    /**
     * Calcula o Dano Final Consolidado quando a Infusão de Mana (V) está ativa, para ser aplicado
     * diretamente via {@code event.setAmount(...)} no mesmo {@code LivingHurtEvent}:
     * <pre>
     * Dano Final = baseDamage + (baseDamage * bodyInfusionMultiplier) + spiritInfusionBonus
     * </pre>
     */
    public static float calculateTotalInfusedDamage(
            float baseDamage,
            int bodyRealm,
            int bodyStage,
            int spiritRealm,
            int spiritStage
    ) {
        float safeBase = Math.max(0.0F, baseDamage);
        float bodyInfusionBuff = calculateBodyInfusionBuff(safeBase, bodyRealm, bodyStage);
        float spiritInfusionBonus = calculateSpiritInfusionFlatBonus(spiritRealm, spiritStage);
        return safeBase + bodyInfusionBuff + spiritInfusionBonus;
    }

    public static float calculateTotalInfusedDamage(float baseDamage, PlayerCoreData data) {
        if (data == null) {
            return calculateTotalInfusedDamage(
                    baseDamage,
                    PlayerCoreData.MIN_REALM,
                    PlayerCoreData.MIN_STAGE,
                    PlayerCoreData.MIN_REALM,
                    PlayerCoreData.MIN_STAGE
            );
        }
        return calculateTotalInfusedDamage(
                baseDamage,
                data.getBodyRealm(),
                data.getBodyStage(),
                data.getSpiritRealm(),
                data.getSpiritStage()
        );
    }

    /**
     * Aplica ou atualiza o {@link AttributeModifier} permanente do Cultivo do Corpo no
     * {@link Attributes#ATTACK_DAMAGE} do jogador usando {@link AttributeModifier.Operation#ADDITION}
     * e o UUID fixo {@link #BODY_CULTIVATION_ATTACK_UUID}.
     */
    public static void applyCultivationAttributes(ServerPlayer player, PlayerCoreData data) {
        if (player == null || data == null) {
            return;
        }

        AttributeInstance attackDamageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr == null) {
            return;
        }

        attackDamageAttr.removeModifier(BODY_CULTIVATION_ATTACK_UUID);

        double passiveAttackBonus = calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
        if (passiveAttackBonus > 0.0D) {
            attackDamageAttr.addPermanentModifier(new AttributeModifier(
                    BODY_CULTIVATION_ATTACK_UUID,
                    BODY_CULTIVATION_ATTACK_NAME,
                    passiveAttackBonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
    }

    public static void applyCultivationAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ModCapabilities.get(player).ifPresent(data -> applyCultivationAttributes(player, data));
    }

    /**
     * Remove o {@link AttributeModifier} do Cultivo do Corpo do {@link Attributes#ATTACK_DAMAGE} do jogador.
     */
    public static void removeCultivationAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }

        AttributeInstance attackDamageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamageAttr != null) {
            attackDamageAttr.removeModifier(BODY_CULTIVATION_ATTACK_UUID);
        }
    }
}
