package com.mythiccontracts.integration;

import com.mythiccontracts.util.ModTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normaliza o Triângulo de Combate (Corpo a Corpo / Better Combat & Simply Swords,
 * Balística / TaCZ e Artes Arcanas / Iron's Spells) sob a Infusão de Mana [V] e o Vigor.
 */
public final class CombatCompatService {

    public enum AttackChannel {
        MELEE,
        FIREARM,
        ARCANE_SPELL,
        PROJECTILE
    }

    public record InfusionProfile(
            float bodyBuffScale,
            float spiritBonusScale,
            float qiCostScale,
            boolean consumesMeleeStamina,
            String channelLabel
    ) {
    }

    public static final long MELEE_STAMINA_DEBOUNCE_TICKS = 3L;

    public static final float FIREARM_BODY_SCALE = 0.35F;
    public static final float FIREARM_SPIRIT_SCALE = 0.25F;
    public static final float FIREARM_QI_COST_SCALE = 0.30F;

    public static final float ARCANE_BODY_SCALE = 0.0F;
    public static final float ARCANE_SPIRIT_SCALE = 0.65F;
    public static final float ARCANE_QI_COST_SCALE = 0.50F;
    public static final float ARCANE_AOE_QI_COST_SCALE = 0.25F;

    public static final float PROJECTILE_BODY_SCALE = 0.75F;
    public static final float PROJECTILE_SPIRIT_SCALE = 0.75F;
    public static final float PROJECTILE_QI_COST_SCALE = 0.75F;

    public static final float SWEEP_MELEE_QI_COST_SCALE = 0.35F;
    public static final float QI_RESONANT_WEAPON_COST_MULT = 0.85F;

    private static final Map<UUID, Long> LAST_MELEE_STAMINA_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_INFUSION_HIT_TICK = new ConcurrentHashMap<>();

    private CombatCompatService() {
    }

    public static AttackChannel classifyPlayerAttack(DamageSource source) {
        if (source == null) {
            return AttackChannel.MELEE;
        }
        ResourceLocation damageTypeId = source.typeHolder().unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        return classifyByKeyAndFlags(
                damageTypeId,
                source.is(ModTags.FIREARM_DAMAGE),
                source.is(ModTags.ARCANE_SPIRITUAL_DAMAGE),
                source.is(ModTags.SPIRITUAL_DAMAGE),
                source.is(DamageTypeTags.WITCH_RESISTANT_TO),
                source.is(DamageTypeTags.IS_PROJECTILE)
        );
    }

    public static AttackChannel classifyByKeyAndFlags(
            ResourceLocation damageTypeId,
            boolean isFirearmTag,
            boolean isArcaneTag,
            boolean isSpiritualTag,
            boolean isWitchResistant,
            boolean isProjectileTag
    ) {
        if (isFirearmTag || (damageTypeId != null && "tacz".equals(damageTypeId.getNamespace()))) {
            return AttackChannel.FIREARM;
        }
        if (isArcaneTag || isSpiritualTag || isWitchResistant
                || (damageTypeId != null && "irons_spellbooks".equals(damageTypeId.getNamespace()))) {
            return AttackChannel.ARCANE_SPELL;
        }
        if (isProjectileTag) {
            return AttackChannel.PROJECTILE;
        }
        return AttackChannel.MELEE;
    }

    public static InfusionProfile evaluateInfusionProfile(
            AttackChannel channel,
            boolean isMultiTargetSameTick,
            boolean qiResonantWeapon
    ) {
        AttackChannel safeChannel = channel != null ? channel : AttackChannel.MELEE;
        return switch (safeChannel) {
            case FIREARM -> new InfusionProfile(
                    FIREARM_BODY_SCALE,
                    FIREARM_SPIRIT_SCALE,
                    FIREARM_QI_COST_SCALE,
                    false,
                    "Balística"
            );
            case ARCANE_SPELL -> new InfusionProfile(
                    ARCANE_BODY_SCALE,
                    ARCANE_SPIRIT_SCALE,
                    isMultiTargetSameTick ? ARCANE_AOE_QI_COST_SCALE : ARCANE_QI_COST_SCALE,
                    false,
                    "Arcano"
            );
            case PROJECTILE -> new InfusionProfile(
                    PROJECTILE_BODY_SCALE,
                    PROJECTILE_SPIRIT_SCALE,
                    PROJECTILE_QI_COST_SCALE,
                    false,
                    "Projétil"
            );
            case MELEE -> {
                float baseQiScale = isMultiTargetSameTick ? SWEEP_MELEE_QI_COST_SCALE : 1.0F;
                float finalQiScale = qiResonantWeapon ? (baseQiScale * QI_RESONANT_WEAPON_COST_MULT) : baseQiScale;
                String label = qiResonantWeapon ? "Ressonante" : "Corpo a Corpo";
                yield new InfusionProfile(1.0F, 1.0F, finalQiScale, !isMultiTargetSameTick, label);
            }
        };
    }

    public static boolean tryConsumeMeleeStaminaWindow(UUID playerId, long gameTime) {
        if (playerId == null) {
            return false;
        }
        final boolean[] allowed = { false };
        LAST_MELEE_STAMINA_TICK.compute(playerId, (id, prev) -> {
            if (prev == null || gameTime < prev || (gameTime - prev) >= MELEE_STAMINA_DEBOUNCE_TICKS) {
                allowed[0] = true;
                return gameTime;
            }
            return prev;
        });
        return allowed[0];
    }

    public static boolean registerInfusionHitAndCheckSameTick(UUID playerId, long gameTime) {
        if (playerId == null) {
            return false;
        }
        Long previous = LAST_INFUSION_HIT_TICK.put(playerId, gameTime);
        return previous != null && previous == gameTime;
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        LAST_MELEE_STAMINA_TICK.remove(playerId);
        LAST_INFUSION_HIT_TICK.remove(playerId);
    }

    public static void clearAllState() {
        LAST_MELEE_STAMINA_TICK.clear();
        LAST_INFUSION_HIT_TICK.clear();
    }
}
