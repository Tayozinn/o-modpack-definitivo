package com.mythiccontracts.subjugation;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Set;

/**
 * Cálculos puros (determinísticos e testáveis offline) para Subjugação como Habilidade:
 * classificação de alvos, limites de servos, Graus I–IV, limiar de HP, alcance e Disputa Espiritual.
 */
public final class SubjugationBalance {

    public static final int MAX_SERVANT_HARD_CAP = 5;
    public static final int SUCCESS_COOLDOWN_TICKS = 200; // 10s
    public static final int SUMMON_COOLDOWN_TICKS = 40;   // 2s
    public static final double CLASH_BREAK_DISTANCE = 14.0D;
    public static final int REVOLT_INTERVAL_TICKS = 20;
    public static final int REVOLT_WINDOW_TICKS = 12;

    private static final ResourceLocation THRONE_BREAKER_SEAL_ID =
            new ResourceLocation("mythic_contracts", "throne_breaker_seal");

    private static final Set<String> MINI_BOSS_PATHS = Set.of(
            "warden", "elder_guardian", "citadel_keeper"
    );
    private static final Set<String> ELITE_PATHS = Set.of(
            "iron_golem", "ravager", "piglin_brute", "evoker", "vindicator", "blaze", "guardian", "witch"
    );

    private SubjugationBalance() {
    }

    public static SubjugationTier classifyByAttributes(
            ResourceLocation entityId,
            float maxHealth,
            boolean isHighRealmBoss
    ) {
        String path = entityId != null ? entityId.getPath() : "";
        if (isHighRealmBoss || "wither".equals(path) || "dead_king".equals(path) || maxHealth >= 250.0F) {
            return SubjugationTier.BOSS;
        }
        if (MINI_BOSS_PATHS.contains(path) || maxHealth >= 80.0F) {
            return SubjugationTier.MINI_BOSS;
        }
        if (ELITE_PATHS.contains(path) || maxHealth >= 35.0F) {
            return SubjugationTier.ELITE;
        }
        return SubjugationTier.COMMON;
    }

    public static int calculateMaxBoundCapacity(PlayerData data) {
        if (data == null) {
            return 1;
        }
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        int base = 1 + Math.max(0, spiritRealm - 1) / 2;
        FactionType faction = FactionType.fromString(data.getFaction().getCurrentFactionId().getPath());
        int factionBonus = faction == FactionType.QUEBRADORES_DE_TRONOS ? 1 : 0;
        int contractBonus = data.getContracts().hasContract(THRONE_BREAKER_SEAL_ID) ? 1 : 0;
        int extraBonus = data.getSubjugation().getMaxCapacityBonus();
        return Mth.clamp(base + factionBonus + contractBonus + extraBonus, 1, MAX_SERVANT_HARD_CAP);
    }

    public static int calculateMaxActiveSummons(PlayerData data) {
        if (data == null) {
            return 1;
        }
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        return spiritRealm >= 5 ? 2 : 1;
    }

    public static int calculateMasteryGrade(PlayerData data) {
        if (data == null) {
            return 1;
        }
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        int realmGrade = spiritRealm >= 7 ? 4 : (spiritRealm >= 5 ? 3 : (spiritRealm >= 3 ? 2 : 1));
        return Mth.clamp(Math.max(realmGrade, data.getSkills().getSubjugationMasteryTier()), 1, 4);
    }

    public static float calculateEffectiveHpThreshold(
            PlayerData data,
            SubjugationTier tier,
            boolean usingCatalyst
    ) {
        SubjugationTier safeTier = tier != null ? tier : SubjugationTier.COMMON;
        if (data == null) {
            return safeTier.getBaseHpThreshold() + (usingCatalyst ? 0.10F : 0.0F);
        }
        FactionType faction = FactionType.fromString(data.getFaction().getCurrentFactionId().getPath());
        float base = faction == FactionType.QUEBRADORES_DE_TRONOS
                ? safeTier.getThroneBreakerHpThreshold()
                : safeTier.getBaseHpThreshold();
        int grade = calculateMasteryGrade(data);
        float gradeBonus = switch (grade) {
            case 2 -> 0.03F;
            case 3 -> 0.05F;
            case 4 -> 0.08F;
            default -> 0.0F;
        };
        float catalystBonus = usingCatalyst ? 0.10F : 0.0F;
        return Mth.clamp(base + gradeBonus + catalystBonus, 0.05F, 0.60F);
    }

    public static double calculateEffectiveReach(int masteryGrade) {
        return switch (Mth.clamp(masteryGrade, 1, 4)) {
            case 2, 3 -> 12.0D;
            case 4 -> 14.0D;
            default -> 10.0D;
        };
    }

    public static float calculateResourceCostScale(boolean usingCatalyst) {
        return usingCatalyst ? 0.50F : 1.00F;
    }

    public static float calculateCaptureSuccessChance(
            PlayerData data,
            SubjugationTier tier,
            boolean usingCatalyst
    ) {
        SubjugationTier safeTier = tier != null ? tier : SubjugationTier.COMMON;
        int spiritRealm = data != null ? CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order() : 1;
        int extraRealms = Math.max(0, spiritRealm - safeTier.getMinSpiritRealm());
        float base = safeTier.getBaseSuccessRate() + safeTier.getBonusRatePerExtraRealm() * extraRealms;
        float cappedBase = Math.min(safeTier.getMaxSuccessRateCap(), base);
        float catalystBonus = usingCatalyst ? 0.15F : 0.0F;
        float cap = usingCatalyst
                ? Math.min(0.98F, safeTier.getMaxSuccessRateCap() + 0.15F)
                : safeTier.getMaxSuccessRateCap();
        return Mth.clamp(cappedBase + catalystBonus, 0.05F, cap);
    }

    public static float calculatePlayerSoulPower(PlayerData data, boolean infusionActive) {
        if (data == null) {
            return 35.0F;
        }
        int spiritRealm = CultivationService.getCurrentRealm(data, CultivationPath.SPIRIT).order();
        int spiritStage = CultivationService.getCurrentStageNumber(data, CultivationPath.SPIRIT);
        int grade = calculateMasteryGrade(data);
        FactionType faction = FactionType.fromString(data.getFaction().getCurrentFactionId().getPath());
        return 35.0F
                + 24.0F * Math.max(0, spiritRealm - 1)
                + 6.0F * Math.max(0, spiritStage - 1)
                + 8.0F * Math.max(0, grade - 1)
                + (infusionActive ? 22.0F : 0.0F)
                + (faction == FactionType.QUEBRADORES_DE_TRONOS ? 18.0F : 0.0F);
    }

    public static float computeClashDominanceDeltaPerTick(
            float playerSoulPower,
            SubjugationTier tier,
            boolean infusionActive
    ) {
        float willpower = tier != null ? tier.getWillpower() : 45.0F;
        float ratio = Mth.clamp(playerSoulPower / Math.max(1.0F, willpower), 0.45F, 2.25F);
        float baseDelta = 1.25F * ratio - 0.35F;
        return infusionActive ? baseDelta + 0.15F : baseDelta;
    }
}
