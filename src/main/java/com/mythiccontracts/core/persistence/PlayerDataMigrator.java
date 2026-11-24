package com.mythiccontracts.core.persistence;

import com.mythiccontracts.core.player.SkillData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

/**
 * Converte snapshots NBT legados (v1, sem {@code DataVersion} ou {@code DataVersion < 2})
 * para o formato modular v2 sem perda de progresso do jogador.
 */
public final class PlayerDataMigrator {

    private static final ResourceLocation LEGACY_PALADIN_VOW_ID =
            new ResourceLocation("mythic_contracts", "paladin_vow");

    private PlayerDataMigrator() {
    }

    public static int detectVersion(CompoundTag root) {
        if (root == null || root.isEmpty()) {
            return PlayerDataSchema.CURRENT_VERSION;
        }
        if (root.contains(PlayerDataSchema.TAG_DATA_VERSION, Tag.TAG_ANY_NUMERIC)) {
            return root.getInt(PlayerDataSchema.TAG_DATA_VERSION);
        }
        return PlayerDataSchema.VERSION_V1;
    }

    public static CompoundTag migrateIfNeeded(CompoundTag root) {
        if (root == null) {
            return new CompoundTag();
        }
        CompoundTag current = root;
        int version = detectVersion(current);
        if (version >= PlayerDataSchema.CURRENT_VERSION) {
            return current;
        }
        if (version <= PlayerDataSchema.VERSION_V1) {
            current = migrateV1ToV2(current);
            version = PlayerDataSchema.VERSION_V2;
        }
        if (version == PlayerDataSchema.VERSION_V2) {
            current = migrateV2ToV3(current);
        }
        return current;
    }

    public static CompoundTag migrateV2ToV3(CompoundTag v2) {
        CompoundTag v3 = v2 != null ? v2.copy() : new CompoundTag();
        v3.putInt(PlayerDataSchema.TAG_DATA_VERSION, PlayerDataSchema.CURRENT_VERSION);

        CompoundTag cultTag = v3.contains(PlayerDataSchema.TAG_SECTION_CULTIVATION, Tag.TAG_COMPOUND)
                ? v3.getCompound(PlayerDataSchema.TAG_SECTION_CULTIVATION).copy()
                : new CompoundTag();

        int bodyRealm = cultTag.contains(PlayerDataSchema.LEGACY_BODY_REALM, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, cultTag.getInt(PlayerDataSchema.LEGACY_BODY_REALM))
                : 1;
        int bodyStage = cultTag.contains(PlayerDataSchema.LEGACY_BODY_STAGE, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, cultTag.getInt(PlayerDataSchema.LEGACY_BODY_STAGE))
                : 1;
        int spiritRealm = cultTag.contains(PlayerDataSchema.LEGACY_SPIRIT_REALM, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, cultTag.getInt(PlayerDataSchema.LEGACY_SPIRIT_REALM))
                : 1;
        int spiritStage = cultTag.contains(PlayerDataSchema.LEGACY_SPIRIT_STAGE, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, cultTag.getInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE))
                : 1;

        float maxIntegrity = PlayerDataSchema.computeLegacyMaxSpiritualIntegrity(spiritRealm, spiritStage, bodyRealm);
        if (!cultTag.contains(PlayerDataSchema.TAG_MAX_SPIRITUAL_INTEGRITY, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putFloat(PlayerDataSchema.TAG_MAX_SPIRITUAL_INTEGRITY, maxIntegrity);
        }
        if (!cultTag.contains(PlayerDataSchema.TAG_CURRENT_SPIRITUAL_INTEGRITY, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putFloat(PlayerDataSchema.TAG_CURRENT_SPIRITUAL_INTEGRITY, maxIntegrity);
        }
        if (!cultTag.contains(PlayerDataSchema.TAG_SPIRIT_SHATTER_TICKS, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putInt(PlayerDataSchema.TAG_SPIRIT_SHATTER_TICKS, 0);
        }

        float maxStamina = PlayerDataSchema.computeLegacyMaxStamina(bodyRealm, bodyStage);
        if (!cultTag.contains(PlayerDataSchema.TAG_MAX_STAMINA, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putFloat(PlayerDataSchema.TAG_MAX_STAMINA, maxStamina);
        }
        if (!cultTag.contains(PlayerDataSchema.TAG_CURRENT_STAMINA, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putFloat(PlayerDataSchema.TAG_CURRENT_STAMINA, maxStamina);
        }
        if (!cultTag.contains(PlayerDataSchema.TAG_STAMINA_REGEN_DELAY, Tag.TAG_ANY_NUMERIC)) {
            cultTag.putInt(PlayerDataSchema.TAG_STAMINA_REGEN_DELAY, 0);
        }

        v3.put(PlayerDataSchema.TAG_SECTION_CULTIVATION, cultTag);
        return v3;
    }

    public static CompoundTag migrateV1ToV2(CompoundTag v1) {
        CompoundTag v2 = new CompoundTag();
        v2.putInt(PlayerDataSchema.TAG_DATA_VERSION, PlayerDataSchema.VERSION_V2);

        // 1. Cultivo
        int bodyRealm = v1.contains(PlayerDataSchema.LEGACY_BODY_REALM, Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(v1.getInt(PlayerDataSchema.LEGACY_BODY_REALM), 1, 7)
                : 1;
        int bodyStage = v1.contains(PlayerDataSchema.LEGACY_BODY_STAGE, Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(v1.getInt(PlayerDataSchema.LEGACY_BODY_STAGE), 1, 3)
                : 1;
        int spiritRealm = v1.contains(PlayerDataSchema.LEGACY_SPIRIT_REALM, Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(v1.getInt(PlayerDataSchema.LEGACY_SPIRIT_REALM), 1, 7)
                : 1;
        int spiritStage = v1.contains(PlayerDataSchema.LEGACY_SPIRIT_STAGE, Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(v1.getInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE), 1, 3)
                : 1;

        float maxQi = PlayerDataSchema.computeLegacyMaxQi(spiritRealm, spiritStage, bodyRealm);
        float currentQi = v1.contains(PlayerDataSchema.LEGACY_CURRENT_QI, Tag.TAG_ANY_NUMERIC)
                ? Mth.clamp(v1.getFloat(PlayerDataSchema.LEGACY_CURRENT_QI), 0.0F, maxQi)
                : maxQi;

        CompoundTag cultTag = new CompoundTag();
        cultTag.putString(PlayerDataSchema.TAG_BODY_REALM_ID, PlayerDataSchema.bodyRealmIdFromNumber(bodyRealm).toString());
        cultTag.putInt(PlayerDataSchema.LEGACY_BODY_REALM, bodyRealm);
        cultTag.putInt(PlayerDataSchema.LEGACY_BODY_STAGE, bodyStage);
        cultTag.putString(PlayerDataSchema.TAG_SPIRIT_REALM_ID, PlayerDataSchema.spiritRealmIdFromNumber(spiritRealm).toString());
        cultTag.putInt(PlayerDataSchema.LEGACY_SPIRIT_REALM, spiritRealm);
        cultTag.putInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE, spiritStage);
        cultTag.putFloat(PlayerDataSchema.TAG_MAX_QI_CAPACITY, maxQi);
        cultTag.putFloat(PlayerDataSchema.LEGACY_CURRENT_QI, currentQi);
        cultTag.putBoolean(PlayerDataSchema.LEGACY_MANA_INFUSION_ACTIVE, v1.getBoolean(PlayerDataSchema.LEGACY_MANA_INFUSION_ACTIVE));
        v2.put(PlayerDataSchema.TAG_SECTION_CULTIVATION, cultTag);

        // 2. Facção
        CompoundTag factionTag = new CompoundTag();
        ResourceLocation factionId = PlayerDataSchema.factionIdFromLegacy(v1.getString(PlayerDataSchema.LEGACY_FACTION));
        factionTag.putString(PlayerDataSchema.TAG_CURRENT_FACTION_ID, factionId.toString());
        factionTag.put(PlayerDataSchema.TAG_REPUTATIONS, new CompoundTag());
        factionTag.put(PlayerDataSchema.TAG_RELATIONSHIPS, new CompoundTag());
        v2.put(PlayerDataSchema.TAG_SECTION_FACTION, factionTag);

        // 3. Contratos
        CompoundTag contractsTag = new CompoundTag();
        ListTag activeList = new ListTag();
        if (v1.contains(PlayerDataSchema.LEGACY_ACTIVE_CONTRACTS, Tag.TAG_LIST)) {
            ListTag legacyContracts = v1.getList(PlayerDataSchema.LEGACY_ACTIVE_CONTRACTS, Tag.TAG_STRING);
            for (int i = 0; i < legacyContracts.size(); i++) {
                ResourceLocation parsed = ResourceLocation.tryParse(legacyContracts.getString(i));
                if (parsed != null) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString(PlayerDataSchema.TAG_CONTRACT_ID, parsed.toString());
                    entry.putLong(PlayerDataSchema.TAG_CONTRACT_SIGNED_AT, 0L);
                    entry.putInt(PlayerDataSchema.TAG_CONTRACT_DURATION, -1);
                    entry.putInt(PlayerDataSchema.TAG_CONTRACT_VIOLATIONS, 0);
                    activeList.add(entry);
                }
            }
        }
        contractsTag.put(PlayerDataSchema.TAG_CONTRACTS_ACTIVE, activeList);

        boolean vowBroken = v1.getBoolean(PlayerDataSchema.LEGACY_VOW_BROKEN);
        ListTag brokenHistory = new ListTag();
        if (vowBroken) {
            brokenHistory.add(StringTag.valueOf(LEGACY_PALADIN_VOW_ID.toString()));
        }
        contractsTag.put(PlayerDataSchema.TAG_CONTRACTS_BROKEN_HISTORY, brokenHistory);
        contractsTag.putBoolean(PlayerDataSchema.LEGACY_VOW_BROKEN, vowBroken);
        v2.put(PlayerDataSchema.TAG_SECTION_CONTRACTS, contractsTag);

        // 4. Habilidades
        CompoundTag skillsTag = new CompoundTag();
        skillsTag.put(PlayerDataSchema.TAG_SKILLS_UNLOCKED, new ListTag());
        skillsTag.put(PlayerDataSchema.TAG_SKILLS_EQUIPPED, new ListTag());
        CompoundTag cooldownsTag = new CompoundTag();
        int uniqueCooldown = v1.contains(PlayerDataSchema.LEGACY_UNIQUE_SKILL_COOLDOWN, Tag.TAG_ANY_NUMERIC)
                ? Math.max(0, v1.getInt(PlayerDataSchema.LEGACY_UNIQUE_SKILL_COOLDOWN))
                : 0;
        if (uniqueCooldown > 0) {
            cooldownsTag.putInt(SkillData.DEFAULT_UNIQUE_SKILL_ID.toString(), uniqueCooldown);
        }
        skillsTag.put(PlayerDataSchema.TAG_SKILLS_COOLDOWNS, cooldownsTag);
        v2.put(PlayerDataSchema.TAG_SECTION_SKILLS, skillsTag);

        // 5. Subjugação
        CompoundTag subTag = new CompoundTag();
        subTag.putInt(PlayerDataSchema.TAG_SUBJUGATION_CAPACITY_BONUS, 0);
        ListTag boundList = new ListTag();
        if (v1.contains(PlayerDataSchema.LEGACY_SUBJUGATED_ENTITIES, Tag.TAG_LIST)) {
            ListTag legacyEntities = v1.getList(PlayerDataSchema.LEGACY_SUBJUGATED_ENTITIES, Tag.TAG_STRING);
            for (int i = 0; i < legacyEntities.size(); i++) {
                ResourceLocation parsed = ResourceLocation.tryParse(legacyEntities.getString(i));
                if (parsed != null) {
                    CompoundTag rec = new CompoundTag();
                    rec.putString(PlayerDataSchema.TAG_RECORD_INSTANCE_ID, UUID.randomUUID().toString());
                    rec.putString(PlayerDataSchema.TAG_RECORD_ENTITY_TYPE, parsed.toString());
                    rec.putInt(PlayerDataSchema.TAG_RECORD_LEVEL, 1);
                    rec.putInt(PlayerDataSchema.TAG_RECORD_EXPERIENCE, 0);
                    boundList.add(rec);
                }
            }
        }
        subTag.put(PlayerDataSchema.TAG_SUBJUGATION_BOUND, boundList);
        v2.put(PlayerDataSchema.TAG_SECTION_SUBJUGATION, subTag);

        return v2;
    }
}
