package com.mythiccontracts.core.service;

import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.util.ModTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Serviço responsável pela Integridade Espiritual: capacidade, classificação de dano,
 * absorção parcial de ataques arcanos/espirituais e estado de Colapso Espiritual.
 */
public final class SpiritualIntegrityService {

    public static final int SPIRIT_SHATTER_TICKS = 400; // 20 segundos
    public static final float SPIRIT_SHATTER_RECOVERY_RATIO = 0.25F;
    public static final float HYBRID_ARCANE_ABSORB_RATIO = 0.30F;
    public static final float PURE_SPIRITUAL_ABSORB_RATIO = 0.50F;
    public static final float SHATTERED_ARCANE_VULNERABILITY = 1.25F;
    public static final float SHATTERED_SPIRITUAL_VULNERABILITY = 1.35F;

    public enum SpiritualDamageCategory {
        NONE,
        HYBRID_ARCANE,
        PURE_SPIRITUAL
    }

    public record SpiritualDamageResult(
            float remainingBodyDamage,
            float absorbedSpiritualDamage,
            boolean triggeredShatter
    ) {
    }

    private SpiritualIntegrityService() {
    }

    public static float calculateMaxSpiritualIntegrity(PlayerData data) {
        if (data == null) {
            return CultivationData.DEFAULT_MAX_SPIRITUAL_INTEGRITY;
        }
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT);
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);
        RealmDefinition bodyRealm = CultivationService.getCurrentRealm(data, CultivationPath.BODY);
        return 100.0F
                + 40.0F * Math.max(0, spiritRealm.order() - 1)
                + 12.0F * Math.max(0, spiritStage - 1)
                + 10.0F * Math.max(0, bodyRealm.order() - 1)
                + ContractManager.getTotalSpiritualIntegrityBonus(data);
    }

    public static float calculateSpiritualRegenPerSecond(PlayerData data) {
        if (data == null) {
            return 2.0F;
        }
        RealmDefinition spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT);
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);
        return 2.0F
                + 1.0F * Math.max(0, spiritRealm.order() - 1)
                + 0.35F * Math.max(0, spiritStage - 1);
    }

    public static void recalculateSpiritualCapacity(PlayerData data) {
        if (data == null) {
            return;
        }
        data.getCultivation().setMaxSpiritualIntegrity(calculateMaxSpiritualIntegrity(data));
    }

    public static SpiritualDamageCategory classifyDamage(DamageSource source) {
        if (source == null) {
            return SpiritualDamageCategory.NONE;
        }
        if (source.is(ModTags.SPIRITUAL_DAMAGE)) {
            return SpiritualDamageCategory.PURE_SPIRITUAL;
        }
        if (source.getEntity() != null && source.getEntity().getType().is(ModTags.HIGH_REALM_BOSS)) {
            return SpiritualDamageCategory.PURE_SPIRITUAL;
        }
        if (source.is(ModTags.ARCANE_SPIRITUAL_DAMAGE) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            return SpiritualDamageCategory.HYBRID_ARCANE;
        }
        boolean isIronsSpells = source.typeHolder().unwrapKey()
                .map(key -> "irons_spellbooks".equals(key.location().getNamespace()))
                .orElse(false);
        return isIronsSpells ? SpiritualDamageCategory.HYBRID_ARCANE : SpiritualDamageCategory.NONE;
    }

    public static SpiritualDamageResult processIncomingDamage(
            PlayerData data,
            float incomingAmount,
            SpiritualDamageCategory category
    ) {
        if (data == null || incomingAmount <= 0.0F || category == null || category == SpiritualDamageCategory.NONE) {
            return new SpiritualDamageResult(Math.max(0.0F, incomingAmount), 0.0F, false);
        }

        recalculateSpiritualCapacity(data);
        CultivationData cult = data.getCultivation();

        if (cult.isSpiritShattered()) {
            float mult = category == SpiritualDamageCategory.PURE_SPIRITUAL
                    ? SHATTERED_SPIRITUAL_VULNERABILITY
                    : SHATTERED_ARCANE_VULNERABILITY;
            return new SpiritualDamageResult(incomingAmount * mult, 0.0F, false);
        }

        float absorbRatio = category == SpiritualDamageCategory.PURE_SPIRITUAL
                ? PURE_SPIRITUAL_ABSORB_RATIO
                : HYBRID_ARCANE_ABSORB_RATIO;
        float targetAbsorb = incomingAmount * absorbRatio;
        float currentIntegrity = cult.getCurrentSpiritualIntegrity();
        float actualAbsorbed = Math.min(currentIntegrity, targetAbsorb);
        float remainingIntegrity = currentIntegrity - actualAbsorbed;
        cult.setCurrentSpiritualIntegrity(remainingIntegrity);

        boolean triggeredShatter = false;
        if (remainingIntegrity <= 0.0F) {
            cult.setCurrentSpiritualIntegrity(0.0F);
            cult.setSpiritShatterTicks(SPIRIT_SHATTER_TICKS);
            cult.setManaInfusionActive(false);
            triggeredShatter = true;
        }

        float remainingBodyDamage = Math.max(0.0F, incomingAmount - actualAbsorbed);
        return new SpiritualDamageResult(remainingBodyDamage, actualAbsorbed, triggeredShatter);
    }

    /**
     * Atualiza 1 tick de Integridade Espiritual / Colapso Espiritual.
     *
     * @return {@code true} se o Colapso Espiritual acabou neste tick.
     */
    public static boolean tickSpiritualIntegrity(PlayerData data) {
        return tickSpiritualIntegrity(data, true);
    }

    public static boolean tickSpiritualIntegrity(PlayerData data, boolean recalculateCapacity) {
        if (data == null) {
            return false;
        }
        if (recalculateCapacity) {
            recalculateSpiritualCapacity(data);
        }
        CultivationData cult = data.getCultivation();

        int shatterTicks = cult.getSpiritShatterTicks();
        if (shatterTicks > 0) {
            int next = shatterTicks - 1;
            cult.setSpiritShatterTicks(next);
            if (next == 0) {
                cult.setCurrentSpiritualIntegrity(cult.getMaxSpiritualIntegrity() * SPIRIT_SHATTER_RECOVERY_RATIO);
                return true;
            }
            return false;
        }

        if (cult.getCurrentSpiritualIntegrity() < cult.getMaxSpiritualIntegrity()) {
            float regenPerTick = calculateSpiritualRegenPerSecond(data) / 20.0F;
            cult.setCurrentSpiritualIntegrity(cult.getCurrentSpiritualIntegrity() + regenPerTick);
        }
        return false;
    }
}
