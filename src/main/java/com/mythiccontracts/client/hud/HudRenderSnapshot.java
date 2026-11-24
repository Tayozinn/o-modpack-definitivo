package com.mythiccontracts.client.hud;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.client.ClientKeyEvents;
import com.mythiccontracts.client.ClientRuntimeState;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.service.SpiritualIntegrityService.SpiritRegenState;
import com.mythiccontracts.cultivation.UniqueSkillHandler;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.skill.cultivation.CultivationSkillRegistry;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import com.mythiccontracts.subjugation.SubjugationBalance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Snapshot imutável dos dados exibidos pela HUD, utilizado tanto para o jogador ativo
 * quanto para a pré-visualização ao vivo dentro da tela de configuração.
 */
public record HudRenderSnapshot(
        FactionType faction,
        boolean infusionActive,
        int bodyRealm,
        int bodyStage,
        String bodyRealmName,
        int spiritRealm,
        int spiritStage,
        String spiritRealmName,
        float health,
        float maxHealth,
        float absorption,
        float spiritualIntegrity,
        float maxSpiritualIntegrity,
        int spiritShatterTicks,
        float currentQi,
        float maxQi,
        float currentStamina,
        float maxStamina,
        float currentDivineResistance,
        float maxDivineResistance,
        int divineOverloadTicks,
        int uniqueSkillCooldownTicks,
        DivineRemnant equippedRemnant,
        ManifestationTier tier,
        int subjugatedCount,
        String infusionKeyLabel,
        String skillKeyLabel,
        SpiritRegenState spiritRegenState,
        int maxSubjugatedCapacity,
        String activeMartialSkillName,
        int activeMartialSlot,
        int activeMartialCooldownTicks,
        String martialKeyLabel,
        boolean clashActive,
        float clashProgress,
        boolean clashRevoltWindow,
        String clashTargetName,
        int activeDivinePhase,
        int unlockedDivinePhases
) {

    public HudRenderSnapshot(
            FactionType faction,
            boolean infusionActive,
            int bodyRealm,
            int bodyStage,
            String bodyRealmName,
            int spiritRealm,
            int spiritStage,
            String spiritRealmName,
            float health,
            float maxHealth,
            float absorption,
            float spiritualIntegrity,
            float maxSpiritualIntegrity,
            int spiritShatterTicks,
            float currentQi,
            float maxQi,
            float currentStamina,
            float maxStamina,
            float currentDivineResistance,
            float maxDivineResistance,
            int divineOverloadTicks,
            int uniqueSkillCooldownTicks,
            DivineRemnant equippedRemnant,
            ManifestationTier tier,
            int subjugatedCount,
            String infusionKeyLabel,
            String skillKeyLabel
    ) {
        this(
                faction, infusionActive, bodyRealm, bodyStage, bodyRealmName, spiritRealm, spiritStage, spiritRealmName,
                health, maxHealth, absorption, spiritualIntegrity, maxSpiritualIntegrity, spiritShatterTicks,
                currentQi, maxQi, currentStamina, maxStamina, currentDivineResistance, maxDivineResistance,
                divineOverloadTicks, uniqueSkillCooldownTicks, equippedRemnant, tier, subjugatedCount,
                infusionKeyLabel, skillKeyLabel,
                SpiritRegenState.FULL, 1, "", 0, 0, "ALT+G", false, 0.0F, false, "", 0, 1
        );
    }

    public float bodyHealthRatio() {
        return Mth.clamp(this.health / Math.max(1.0F, this.maxHealth), 0.0F, 1.0F);
    }

    public float spiritualRatio() {
        return Mth.clamp(this.spiritualIntegrity / Math.max(1.0F, this.maxSpiritualIntegrity), 0.0F, 1.0F);
    }

    public float qiRatio() {
        return Mth.clamp(this.currentQi / Math.max(1.0F, this.maxQi), 0.0F, 1.0F);
    }

    public float staminaRatio() {
        return Mth.clamp(this.currentStamina / Math.max(1.0F, this.maxStamina), 0.0F, 1.0F);
    }

    public float divineRatio() {
        return Mth.clamp(this.currentDivineResistance / Math.max(1.0F, this.maxDivineResistance), 0.0F, 1.0F);
    }

    public static HudRenderSnapshot fromLivePlayer(Player player, PlayerCoreData data) {
        PlayerData playerData = data.getPlayerData();
        CultivationData cult = playerData.getCultivation();
        SkillData skills = playerData.getSkills();

        String infKey = shortKeyLabel(ClientKeyEvents.MANA_INFUSION_KEY.getTranslatedKeyMessage().getString(), "V");
        String skillKey = shortKeyLabel(ClientKeyEvents.UNIQUE_SKILL_KEY.getTranslatedKeyMessage().getString(), "G");

        SpiritRegenState regenState = cult.isSpiritShattered()
                ? SpiritRegenState.BLOCKED_SHATTER
                : SpiritRegenState.fromOrdinal(cult.getClientSpiritRegenStateOrdinal());
        int maxBound = SubjugationBalance.calculateMaxBoundCapacity(playerData);

        ResourceLocation activeMartialId = skills.getActiveMartialSkill();
        String martialName = activeMartialId != null ? CultivationSkillRegistry.getDisplayName(activeMartialId) : "";
        String martialKey = ClientKeyEvents.CULTIVATION_SKILL_KEY.getTranslatedKeyMessage().getString().trim();
        if (martialKey.isEmpty() || martialKey.length() > 7) {
            martialKey = "ALT+G";
        }

        ManifestationTier unlockedTier = DivineResonanceBalance.getManifestationTier(playerData);
        int unlockedPhases = unlockedTier.ordinal() + 1;
        int activePhase = Mth.clamp(skills.getActiveDivinePhase(), 0, unlockedPhases - 1);

        return new HudRenderSnapshot(
                data.getFaction() != null ? data.getFaction() : FactionType.NONE,
                data.isManaInfusionActive(),
                data.getBodyRealm(),
                data.getBodyStage(),
                CultivationService.getRealmDisplayName(playerData, CultivationPath.BODY),
                data.getSpiritRealm(),
                data.getSpiritStage(),
                CultivationService.getRealmDisplayName(playerData, CultivationPath.SPIRIT),
                Math.max(0.0F, player.getHealth()),
                Math.max(1.0F, player.getMaxHealth()),
                Math.max(0.0F, player.getAbsorptionAmount()),
                cult.getCurrentSpiritualIntegrity(),
                cult.getMaxSpiritualIntegrity(),
                cult.getSpiritShatterTicks(),
                cult.getCurrentQi(),
                cult.getMaxQiCapacity(),
                cult.getCurrentStamina(),
                cult.getMaxStamina(),
                skills.getCurrentDivineResistance(),
                skills.getMaxDivineResistance(),
                skills.getDivineOverloadTicks(),
                skills.getUniqueSkillCooldownTicks(),
                UniqueSkillHandler.getEquippedRemnant(playerData),
                unlockedTier,
                data.getSubjugatedEntities().size(),
                infKey,
                skillKey,
                regenState,
                maxBound,
                martialName,
                skills.getActiveMartialSlot(),
                ClientRuntimeState.getActiveMartialCooldownTicks(),
                martialKey.toUpperCase(Locale.ROOT),
                ClientRuntimeState.isClashActive(),
                ClientRuntimeState.getClashProgress(),
                ClientRuntimeState.isClashRevoltWindow(),
                ClientRuntimeState.getClashTargetName(),
                activePhase,
                unlockedPhases
        );
    }

    public static HudRenderSnapshot createPreviewSample() {
        return new HudRenderSnapshot(
                FactionType.JURAMENTADOS_DA_LUZ,
                true,
                4,
                2,
                "Carne de Jade",
                3,
                3,
                "Núcleo Dourado",
                148.0F,
                180.0F,
                4.0F,
                86.0F,
                120.0F,
                0,
                210.0F,
                210.0F,
                64.0F,
                100.0F,
                195.0F,
                280.0F,
                0,
                0,
                DivineRemnant.EYE_OF_RA,
                ManifestationTier.TIER_3,
                2,
                "V",
                "G",
                SpiritRegenState.RECOVERING,
                2,
                "Palma Sísmica",
                0,
                0,
                "ALT+G",
                false,
                0.0F,
                false,
                "",
                1,
                3
        );
    }

    private static String shortKeyLabel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() > 3) {
            return trimmed.substring(0, 3);
        }
        return trimmed;
    }
}
