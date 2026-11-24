package com.mythiccontracts.client.hud;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.client.ClientKeyEvents;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.cultivation.UniqueSkillHandler;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
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
        String skillKeyLabel
) {

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
                DivineResonanceBalance.getManifestationTier(playerData),
                data.getSubjugatedEntities().size(),
                infKey,
                skillKey
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
                3,
                "V",
                "G"
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
