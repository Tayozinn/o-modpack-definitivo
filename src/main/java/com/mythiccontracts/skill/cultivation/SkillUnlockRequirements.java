package com.mythiccontracts.skill.cultivation;

import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Requisitos estruturados em JSON para desbloqueio autoritativo de Técnicas de Cultivo.
 */
public record SkillUnlockRequirements(
        int minBodyRealm,
        int minBodyStage,
        int minSpiritRealm,
        int minSpiritStage,
        @Nullable ResourceLocation prerequisiteSkill,
        boolean requireActiveContract,
        Set<FactionType> allowedFactions,
        Set<FactionType> realmBypassFactions,
        int minSubjugatedCount,
        int minReputation,
        boolean contractOrAlternative,
        float qiUnlockCost
) {
    public static final SkillUnlockRequirements NONE = new SkillUnlockRequirements(
            1, 1, 1, 1, null, false, Set.of(), Set.of(), 0, 0, false, 0.0F
    );

    public SkillUnlockRequirements(
            int minBodyRealm,
            int minBodyStage,
            int minSpiritRealm,
            int minSpiritStage,
            @Nullable ResourceLocation prerequisiteSkill,
            boolean requireActiveContract,
            Set<FactionType> allowedFactions,
            Set<FactionType> realmBypassFactions,
            int minSubjugatedCount,
            int minReputation,
            float qiUnlockCost
    ) {
        this(
                minBodyRealm, minBodyStage, minSpiritRealm, minSpiritStage,
                prerequisiteSkill, requireActiveContract, allowedFactions, realmBypassFactions,
                minSubjugatedCount, minReputation, false, qiUnlockCost
        );
    }

    public SkillUnlockRequirements {
        minBodyRealm = Math.max(1, minBodyRealm);
        minBodyStage = Math.max(1, minBodyStage);
        minSpiritRealm = Math.max(1, minSpiritRealm);
        minSpiritStage = Math.max(1, minSpiritStage);
        allowedFactions = allowedFactions != null ? Collections.unmodifiableSet(allowedFactions) : Set.of();
        realmBypassFactions = realmBypassFactions != null ? Collections.unmodifiableSet(realmBypassFactions) : Set.of();
        minSubjugatedCount = Math.max(0, minSubjugatedCount);
        minReputation = Math.max(0, minReputation);
        qiUnlockCost = Math.max(0.0F, qiUnlockCost);
    }
}
