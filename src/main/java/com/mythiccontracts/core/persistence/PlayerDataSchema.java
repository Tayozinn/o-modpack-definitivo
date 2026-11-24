package com.mythiccontracts.core.persistence;

import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.FactionData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Constantes de schema NBT e helpers de mapeamento de identificadores entre v1 e v2.
 */
public final class PlayerDataSchema {

    public static final int VERSION_V1 = 1;
    public static final int VERSION_V2 = 2;
    public static final int CURRENT_VERSION = 3;

    public static final String TAG_DATA_VERSION = "DataVersion";

    // Sub-blocos v2/v3
    public static final String TAG_SECTION_CULTIVATION = "Cultivation";
    public static final String TAG_SECTION_FACTION = "Faction";
    public static final String TAG_SECTION_CONTRACTS = "Contracts";
    public static final String TAG_SECTION_SKILLS = "Skills";
    public static final String TAG_SECTION_SUBJUGATION = "Subjugation";

    // Cultivation v2/v3
    public static final String TAG_BODY_REALM_ID = "BodyRealmId";
    public static final String TAG_SPIRIT_REALM_ID = "SpiritRealmId";
    public static final String TAG_MAX_QI_CAPACITY = "MaxQiCapacity";
    public static final String TAG_MAX_SPIRITUAL_INTEGRITY = "MaxSpiritualIntegrity";
    public static final String TAG_CURRENT_SPIRITUAL_INTEGRITY = "CurrentSpiritualIntegrity";
    public static final String TAG_SPIRIT_SHATTER_TICKS = "SpiritShatterTicks";
    public static final String TAG_MAX_STAMINA = "MaxStamina";
    public static final String TAG_CURRENT_STAMINA = "CurrentStamina";
    public static final String TAG_STAMINA_REGEN_DELAY = "StaminaRegenDelayTicks";

    // Faction v2
    public static final String TAG_CURRENT_FACTION_ID = "CurrentFactionId";
    public static final String TAG_REPUTATIONS = "Reputations";
    public static final String TAG_RELATIONSHIPS = "Relationships";

    // Contracts v2
    public static final String TAG_CONTRACTS_ACTIVE = "Active";
    public static final String TAG_CONTRACT_ID = "Id";
    public static final String TAG_CONTRACT_SIGNED_AT = "SignedAtTick";
    public static final String TAG_CONTRACT_DURATION = "RemainingDurationTicks";
    public static final String TAG_CONTRACT_VIOLATIONS = "ViolationCount";
    public static final String TAG_CONTRACTS_BROKEN_HISTORY = "BrokenHistory";

    // Skills v2
    public static final String TAG_SKILLS_UNLOCKED = "Unlocked";
    public static final String TAG_SKILLS_EQUIPPED = "Equipped";
    public static final String TAG_SKILLS_COOLDOWNS = "Cooldowns";
    public static final String TAG_MAX_DIVINE_RESISTANCE = "MaxDivineResistance";
    public static final String TAG_CURRENT_DIVINE_RESISTANCE = "CurrentDivineResistance";
    public static final String TAG_DIVINE_OVERLOAD_TICKS = "DivineOverloadTicks";

    // Subjugation v2
    public static final String TAG_SUBJUGATION_CAPACITY_BONUS = "MaxCapacityBonus";
    public static final String TAG_SUBJUGATION_BOUND = "BoundEntities";
    public static final String TAG_RECORD_INSTANCE_ID = "InstanceId";
    public static final String TAG_RECORD_ENTITY_TYPE = "EntityType";
    public static final String TAG_RECORD_LEVEL = "Level";
    public static final String TAG_RECORD_EXPERIENCE = "Experience";
    public static final String TAG_RECORD_CUSTOM_NAME = "CustomName";
    public static final String TAG_RECORD_SNAPSHOT = "Snapshot";

    // Chaves legadas v1 (mantidas para migração)
    public static final String LEGACY_FACTION = "Faction";
    public static final String LEGACY_BODY_REALM = "BodyRealm";
    public static final String LEGACY_BODY_STAGE = "BodyStage";
    public static final String LEGACY_SPIRIT_REALM = "SpiritRealm";
    public static final String LEGACY_SPIRIT_STAGE = "SpiritStage";
    public static final String LEGACY_ACTIVE_CONTRACTS = "ActiveContracts";
    public static final String LEGACY_SUBJUGATED_ENTITIES = "SubjugatedEntities";
    public static final String LEGACY_MANA_INFUSION_ACTIVE = "ManaInfusionActive";
    public static final String LEGACY_VOW_BROKEN = "VowBroken";
    public static final String LEGACY_CURRENT_QI = "CurrentQi";
    public static final String LEGACY_UNIQUE_SKILL_COOLDOWN = "UniqueSkillCooldown";

    private PlayerDataSchema() {
    }

    public static ResourceLocation bodyRealmIdFromNumber(int realm) {
        int clamped = Mth.clamp(realm, CultivationData.DEFAULT_MIN_REALM, CultivationData.DEFAULT_MAX_REALM);
        return new ResourceLocation("mythic_contracts", "body_realm_" + clamped);
    }

    public static ResourceLocation spiritRealmIdFromNumber(int realm) {
        int clamped = Mth.clamp(realm, CultivationData.DEFAULT_MIN_REALM, CultivationData.DEFAULT_MAX_REALM);
        return new ResourceLocation("mythic_contracts", "spirit_realm_" + clamped);
    }

    public static ResourceLocation factionIdFromLegacy(String legacyId) {
        if (legacyId == null || legacyId.isBlank()) {
            return FactionData.NONE_FACTION_ID;
        }
        String trimmed = legacyId.trim().toLowerCase(Locale.ROOT);
        if (trimmed.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(trimmed);
            return parsed != null ? parsed : FactionData.NONE_FACTION_ID;
        }
        return switch (trimmed) {
            case "errantes_do_veu", "juramentados_da_luz", "irmandade_divida_eterna", "quebradores_de_tronos", "none" ->
                    new ResourceLocation("mythic_contracts", trimmed);
            default -> FactionData.NONE_FACTION_ID;
        };
    }

    public static float computeLegacyMaxQi(int spiritRealm, int spiritStage, int bodyRealm) {
        int sr = Mth.clamp(spiritRealm, CultivationData.DEFAULT_MIN_REALM, CultivationData.DEFAULT_MAX_REALM);
        int ss = Mth.clamp(spiritStage, CultivationData.DEFAULT_MIN_STAGE, CultivationData.DEFAULT_MAX_STAGE);
        int br = Mth.clamp(bodyRealm, CultivationData.DEFAULT_MIN_REALM, CultivationData.DEFAULT_MAX_REALM);
        return 100.0F + 35.0F * (sr - 1) + 10.0F * (ss - 1) + 10.0F * (br - 1);
    }

    public static float computeLegacyMaxSpiritualIntegrity(int spiritRealm, int spiritStage, int bodyRealm) {
        int sr = Math.max(CultivationData.DEFAULT_MIN_REALM, spiritRealm);
        int ss = Math.max(CultivationData.DEFAULT_MIN_STAGE, spiritStage);
        int br = Math.max(CultivationData.DEFAULT_MIN_REALM, bodyRealm);
        return 100.0F + 40.0F * (sr - 1) + 12.0F * (ss - 1) + 10.0F * (br - 1);
    }

    public static float computeLegacyMaxStamina(int bodyRealm, int bodyStage) {
        int br = Math.max(CultivationData.DEFAULT_MIN_REALM, bodyRealm);
        int bs = Math.max(CultivationData.DEFAULT_MIN_STAGE, bodyStage);
        return 100.0F + 25.0F * (br - 1) + 5.0F * (bs - 1);
    }
}
