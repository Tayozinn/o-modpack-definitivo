package com.mythiccontracts.cultivation;

import com.mythiccontracts.attribute.AttributeService;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.cultivation.realm.RealmStageDefinition;
import net.minecraft.server.level.ServerPlayer;
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
        int r = Math.max(PlayerCoreData.MIN_REALM, realm);
        int s = Math.max(PlayerCoreData.MIN_STAGE, stage);
        return (r - 1) * PlayerCoreData.MAX_STAGE + s;
    }

    public static double calculateBodyPassiveAttackBonus(int bodyRealm, int bodyStage) {
        int r = Math.max(PlayerCoreData.MIN_REALM, bodyRealm);
        int s = Math.max(PlayerCoreData.MIN_STAGE, bodyStage);
        RealmDefinition def = RealmRegistry.getRealmByOrder(CultivationPath.BODY, r);
        if (def != null) {
            return def.getStageByNumber(s).attributes().stream()
                    .filter(a -> a.attributeId().getPath().contains("attack_damage"))
                    .mapToDouble(a -> a.amount())
                    .findFirst()
                    .orElse(0.5D * r * (r + s - 1));
        }
        return 0.5D * r * (r + s - 1);
    }

    public static double calculateBodyPassiveAttackBonus(PlayerCoreData data) {
        if (data == null) {
            return calculateBodyPassiveAttackBonus(PlayerCoreData.MIN_REALM, PlayerCoreData.MIN_STAGE);
        }
        return calculateBodyPassiveAttackBonus(data.getBodyRealm(), data.getBodyStage());
    }

    public static float calculateBodyInfusionMultiplier(int bodyRealm, int bodyStage) {
        int r = Math.max(PlayerCoreData.MIN_REALM, bodyRealm);
        int s = Math.max(PlayerCoreData.MIN_STAGE, bodyStage);
        RealmDefinition def = RealmRegistry.getRealmByOrder(CultivationPath.BODY, r);
        if (def != null) {
            return def.getStageByNumber(s).infusionMultiplier();
        }
        return (0.20F * (r - 1)) + (0.05F * s);
    }

    public static float calculateBodyInfusionMultiplier(PlayerData data) {
        if (data == null) {
            return calculateBodyInfusionMultiplier(1, 1);
        }
        RealmStageDefinition stage = CultivationService.getCurrentStage(data, CultivationPath.BODY);
        return stage.infusionMultiplier();
    }

    public static float getBodyInfusionMultiplier(int bodyRealm, int bodyStage) {
        return calculateBodyInfusionMultiplier(bodyRealm, bodyStage);
    }

    public static float calculateBodyInfusionBuff(float baseDamage, int bodyRealm, int bodyStage) {
        float safeBase = Math.max(0.0F, baseDamage);
        return safeBase * calculateBodyInfusionMultiplier(bodyRealm, bodyStage);
    }

    public static float calculateBodyInfusionBuff(float baseDamage, PlayerData data) {
        float safeBase = Math.max(0.0F, baseDamage);
        return safeBase * calculateBodyInfusionMultiplier(data);
    }

    public static float calculateSpiritInfusionFlatBonus(int spiritRealm, int spiritStage) {
        int r = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        int s = Math.max(PlayerCoreData.MIN_STAGE, spiritStage);
        RealmDefinition def = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, r);
        if (def != null) {
            return def.getStageByNumber(s).infusionFlatBonus();
        }
        return 1.0F * r * (r + s - 1);
    }

    public static float calculateSpiritInfusionFlatBonus(PlayerData data) {
        if (data == null) {
            return calculateSpiritInfusionFlatBonus(1, 1);
        }
        RealmStageDefinition stage = CultivationService.getCurrentStage(data, CultivationPath.SPIRIT);
        return stage.infusionFlatBonus();
    }

    public static float calculateMaxQi(int spiritRealm, int spiritStage, int bodyRealm) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        int ss = Math.max(PlayerCoreData.MIN_STAGE, spiritStage);
        int br = Math.max(PlayerCoreData.MIN_REALM, bodyRealm);
        RealmDefinition spiritDef = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, sr);
        RealmDefinition bodyDef = RealmRegistry.getRealmByOrder(CultivationPath.BODY, br);
        if (spiritDef != null && bodyDef != null) {
            return spiritDef.qiBase() + spiritDef.getStageByNumber(ss).qiBonus() + bodyDef.qiBase();
        }
        return 100.0F + 35.0F * (sr - 1) + 10.0F * (ss - 1) + 10.0F * (br - 1);
    }

    public static float calculateQiRegenPerSecond(int spiritRealm, int spiritStage) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        int ss = Math.max(PlayerCoreData.MIN_STAGE, spiritStage);
        RealmDefinition spiritDef = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, sr);
        if (spiritDef != null) {
            return spiritDef.qiRegenBonus() + 0.5F * (ss - 1);
        }
        return 4.0F + 1.5F * (sr - 1) + 0.5F * (ss - 1);
    }

    public static float calculateInfusionDrainPerSecond(int spiritRealm) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        RealmDefinition spiritDef = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, sr);
        if (spiritDef != null) {
            return spiritDef.infusionDrainPerSecond();
        }
        return 2.5F + 0.5F * (sr - 1);
    }

    public static float calculateInfusionDrainPerHit(int spiritRealm) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        RealmDefinition spiritDef = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, sr);
        if (spiritDef != null) {
            return spiritDef.infusionDrainPerHit();
        }
        return 3.0F + 1.0F * (sr - 1);
    }

    public static float calculateInfusionDrainPerHit(PlayerData data) {
        if (data == null) {
            return calculateInfusionDrainPerHit(1);
        }
        return CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).infusionDrainPerHit();
    }

    public static float calculateUniqueSkillQiCost(int spiritRealm) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        return 35.0F + 5.0F * (sr - 1);
    }

    public static int calculateUniqueSkillCooldownTicks(int spiritRealm) {
        int sr = Math.max(PlayerCoreData.MIN_REALM, spiritRealm);
        return Math.max(100, 200 - 15 * (sr - 1));
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
        CultivationService.applyCultivationAttributes(player, data.getPlayerData());
    }

    public static void applyCultivationAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ModCapabilities.get(player).ifPresent(data -> applyCultivationAttributes(player, data));
    }

    /**
     * Remove os modificadores de atributos de Cultivo do jogador.
     */
    public static void removeCultivationAttributes(ServerPlayer player) {
        if (player == null) {
            return;
        }
        AttributeService.clearByNamePrefix(player, CultivationService.CULTIVATION_MODIFIER_PREFIX);
        AttributeService.removeModifier(player, Attributes.ATTACK_DAMAGE, BODY_CULTIVATION_ATTACK_UUID);
        AttributeService.removeModifier(player, Attributes.MAX_HEALTH, RealmRegistry.BODY_CULTIVATION_HEALTH_UUID);
    }
}
