package com.mythiccontracts.core.persistence;

import com.mythiccontracts.core.player.ActiveContractState;
import com.mythiccontracts.core.player.ContractData;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.FactionData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.player.SubjugatedEntityRecord;
import com.mythiccontracts.core.player.SubjugationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;

/**
 * Serializador versionado (v2) para {@link PlayerData}, com separação explícita entre
 * persistência completa em disco e payload enxuto de sincronização para o cliente.
 */
public final class PlayerDataSerializer {

    private PlayerDataSerializer() {
    }

    public static CompoundTag writeFull(PlayerData data) {
        return writeInternal(data, true);
    }

    public static CompoundTag writeClientSync(PlayerData data) {
        return writeInternal(data, false);
    }

    private static CompoundTag writeInternal(PlayerData data, boolean includeServerPrivateData) {
        CompoundTag root = new CompoundTag();
        if (data == null) {
            return root;
        }

        root.putInt(PlayerDataSchema.TAG_DATA_VERSION, PlayerDataSchema.CURRENT_VERSION);
        root.put(PlayerDataSchema.TAG_SECTION_CULTIVATION, writeCultivation(data.getCultivation()));
        root.put(PlayerDataSchema.TAG_SECTION_FACTION, writeFaction(data.getFaction()));
        root.put(PlayerDataSchema.TAG_SECTION_CONTRACTS, writeContracts(data.getContracts(), includeServerPrivateData));
        root.put(PlayerDataSchema.TAG_SECTION_SKILLS, writeSkills(data.getSkills()));
        root.put(PlayerDataSchema.TAG_SECTION_SUBJUGATION, writeSubjugation(data.getSubjugation(), includeServerPrivateData));
        return root;
    }

    public static void read(CompoundTag rawTag, PlayerData target) {
        if (rawTag == null || target == null || rawTag.isEmpty()) {
            return;
        }
        CompoundTag migrated = PlayerDataMigrator.migrateIfNeeded(rawTag);

        if (migrated.contains(PlayerDataSchema.TAG_SECTION_CULTIVATION, Tag.TAG_COMPOUND)) {
            readCultivation(migrated.getCompound(PlayerDataSchema.TAG_SECTION_CULTIVATION), target.getCultivation());
        }
        if (migrated.contains(PlayerDataSchema.TAG_SECTION_FACTION, Tag.TAG_COMPOUND)) {
            readFaction(migrated.getCompound(PlayerDataSchema.TAG_SECTION_FACTION), target.getFaction());
        }
        if (migrated.contains(PlayerDataSchema.TAG_SECTION_CONTRACTS, Tag.TAG_COMPOUND)) {
            readContracts(migrated.getCompound(PlayerDataSchema.TAG_SECTION_CONTRACTS), target.getContracts());
        }
        if (migrated.contains(PlayerDataSchema.TAG_SECTION_SKILLS, Tag.TAG_COMPOUND)) {
            readSkills(migrated.getCompound(PlayerDataSchema.TAG_SECTION_SKILLS), target.getSkills());
        }
        if (migrated.contains(PlayerDataSchema.TAG_SECTION_SUBJUGATION, Tag.TAG_COMPOUND)) {
            readSubjugation(migrated.getCompound(PlayerDataSchema.TAG_SECTION_SUBJUGATION), target.getSubjugation());
        }
    }

    private static CompoundTag writeCultivation(CultivationData cult) {
        CompoundTag tag = new CompoundTag();
        tag.putString(PlayerDataSchema.TAG_BODY_REALM_ID, cult.getBodyRealmId().toString());
        tag.putInt(PlayerDataSchema.LEGACY_BODY_REALM, cult.getBodyRealm());
        tag.putInt(PlayerDataSchema.LEGACY_BODY_STAGE, cult.getBodyStage());
        tag.putString(PlayerDataSchema.TAG_SPIRIT_REALM_ID, cult.getSpiritRealmId().toString());
        tag.putInt(PlayerDataSchema.LEGACY_SPIRIT_REALM, cult.getSpiritRealm());
        tag.putInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE, cult.getSpiritStage());
        tag.putFloat(PlayerDataSchema.TAG_MAX_QI_CAPACITY, cult.getMaxQiCapacity());
        tag.putFloat(PlayerDataSchema.LEGACY_CURRENT_QI, cult.getCurrentQi());
        tag.putBoolean(PlayerDataSchema.LEGACY_MANA_INFUSION_ACTIVE, cult.isManaInfusionActive());
        tag.putFloat(PlayerDataSchema.TAG_MAX_SPIRITUAL_INTEGRITY, cult.getMaxSpiritualIntegrity());
        tag.putFloat(PlayerDataSchema.TAG_CURRENT_SPIRITUAL_INTEGRITY, cult.getCurrentSpiritualIntegrity());
        tag.putInt(PlayerDataSchema.TAG_SPIRIT_SHATTER_TICKS, cult.getSpiritShatterTicks());
        tag.putInt(PlayerDataSchema.TAG_SPIRIT_COMBAT_COOLDOWN, cult.getSpiritCombatCooldownTicks());
        tag.putInt(PlayerDataSchema.TAG_STILL_MEDITATION_TICKS, cult.getStillMeditationTicks());
        tag.putInt(PlayerDataSchema.TAG_MEDITATION_BOOST_TICKS, cult.getMeditationBoostTicks());
        tag.putFloat(PlayerDataSchema.TAG_MAX_STAMINA, cult.getMaxStamina());
        tag.putFloat(PlayerDataSchema.TAG_CURRENT_STAMINA, cult.getCurrentStamina());
        tag.putInt(PlayerDataSchema.TAG_STAMINA_REGEN_DELAY, cult.getStaminaRegenDelayTicks());
        return tag;
    }

    private static void readCultivation(CompoundTag tag, CultivationData cult) {
        ResourceLocation bodyRealmId = ResourceLocation.tryParse(tag.getString(PlayerDataSchema.TAG_BODY_REALM_ID));
        int bodyRealm = tag.contains(PlayerDataSchema.LEGACY_BODY_REALM, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.LEGACY_BODY_REALM)
                : CultivationData.DEFAULT_MIN_REALM;
        int bodyStage = tag.contains(PlayerDataSchema.LEGACY_BODY_STAGE, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.LEGACY_BODY_STAGE)
                : CultivationData.DEFAULT_MIN_STAGE;

        ResourceLocation spiritRealmId = ResourceLocation.tryParse(tag.getString(PlayerDataSchema.TAG_SPIRIT_REALM_ID));
        int spiritRealm = tag.contains(PlayerDataSchema.LEGACY_SPIRIT_REALM, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.LEGACY_SPIRIT_REALM)
                : CultivationData.DEFAULT_MIN_REALM;
        int spiritStage = tag.contains(PlayerDataSchema.LEGACY_SPIRIT_STAGE, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE)
                : CultivationData.DEFAULT_MIN_STAGE;

        cult.setBodyRealm(bodyRealm);
        cult.setBodyStage(bodyStage);
        cult.setBodyRealmId(bodyRealmId != null ? bodyRealmId : PlayerDataSchema.bodyRealmIdFromNumber(cult.getBodyRealm()));

        cult.setSpiritRealm(spiritRealm);
        cult.setSpiritStage(spiritStage);
        cult.setSpiritRealmId(spiritRealmId != null ? spiritRealmId : PlayerDataSchema.spiritRealmIdFromNumber(cult.getSpiritRealm()));

        float fallbackMaxQi = PlayerDataSchema.computeLegacyMaxQi(cult.getSpiritRealm(), cult.getSpiritStage(), cult.getBodyRealm());
        float maxQi = tag.contains(PlayerDataSchema.TAG_MAX_QI_CAPACITY, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_MAX_QI_CAPACITY)
                : fallbackMaxQi;
        cult.setMaxQiCapacity(maxQi);

        float currentQi = tag.contains(PlayerDataSchema.LEGACY_CURRENT_QI, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.LEGACY_CURRENT_QI)
                : cult.getMaxQiCapacity();
        cult.setCurrentQi(currentQi);
        cult.setManaInfusionActive(tag.getBoolean(PlayerDataSchema.LEGACY_MANA_INFUSION_ACTIVE));

        float fallbackIntegrity = PlayerDataSchema.computeLegacyMaxSpiritualIntegrity(
                cult.getSpiritRealm(), cult.getSpiritStage(), cult.getBodyRealm()
        );
        float maxIntegrity = tag.contains(PlayerDataSchema.TAG_MAX_SPIRITUAL_INTEGRITY, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_MAX_SPIRITUAL_INTEGRITY)
                : fallbackIntegrity;
        cult.setMaxSpiritualIntegrity(maxIntegrity);
        float curIntegrity = tag.contains(PlayerDataSchema.TAG_CURRENT_SPIRITUAL_INTEGRITY, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_CURRENT_SPIRITUAL_INTEGRITY)
                : cult.getMaxSpiritualIntegrity();
        cult.setCurrentSpiritualIntegrity(curIntegrity);
        cult.setSpiritShatterTicks(tag.contains(PlayerDataSchema.TAG_SPIRIT_SHATTER_TICKS, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_SPIRIT_SHATTER_TICKS)
                : 0);
        cult.setSpiritCombatCooldownTicks(tag.contains(PlayerDataSchema.TAG_SPIRIT_COMBAT_COOLDOWN, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_SPIRIT_COMBAT_COOLDOWN)
                : 0);
        cult.setStillMeditationTicks(tag.contains(PlayerDataSchema.TAG_STILL_MEDITATION_TICKS, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_STILL_MEDITATION_TICKS)
                : 0);
        cult.setMeditationBoostTicks(tag.contains(PlayerDataSchema.TAG_MEDITATION_BOOST_TICKS, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_MEDITATION_BOOST_TICKS)
                : 0);

        float fallbackStamina = PlayerDataSchema.computeLegacyMaxStamina(cult.getBodyRealm(), cult.getBodyStage());
        float maxStamina = tag.contains(PlayerDataSchema.TAG_MAX_STAMINA, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_MAX_STAMINA)
                : fallbackStamina;
        cult.setMaxStamina(maxStamina);
        float curStamina = tag.contains(PlayerDataSchema.TAG_CURRENT_STAMINA, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_CURRENT_STAMINA)
                : cult.getMaxStamina();
        cult.setCurrentStamina(curStamina);
        cult.setStaminaRegenDelayTicks(tag.contains(PlayerDataSchema.TAG_STAMINA_REGEN_DELAY, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_STAMINA_REGEN_DELAY)
                : 0);
    }

    private static CompoundTag writeFaction(FactionData faction) {
        CompoundTag tag = new CompoundTag();
        tag.putString(PlayerDataSchema.TAG_CURRENT_FACTION_ID, faction.getCurrentFactionId().toString());

        CompoundTag repsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : faction.getReputations().entrySet()) {
            repsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put(PlayerDataSchema.TAG_REPUTATIONS, repsTag);

        CompoundTag relTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, String> entry : faction.getRelationships().entrySet()) {
            relTag.putString(entry.getKey().toString(), entry.getValue());
        }
        tag.put(PlayerDataSchema.TAG_RELATIONSHIPS, relTag);
        return tag;
    }

    private static void readFaction(CompoundTag tag, FactionData faction) {
        ResourceLocation factionId = ResourceLocation.tryParse(tag.getString(PlayerDataSchema.TAG_CURRENT_FACTION_ID));
        faction.setCurrentFactionId(factionId != null ? factionId : FactionData.NONE_FACTION_ID);

        faction.clearReputations();
        if (tag.contains(PlayerDataSchema.TAG_REPUTATIONS, Tag.TAG_COMPOUND)) {
            CompoundTag repsTag = tag.getCompound(PlayerDataSchema.TAG_REPUTATIONS);
            for (String key : repsTag.getAllKeys()) {
                ResourceLocation parsed = ResourceLocation.tryParse(key);
                if (parsed != null && repsTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    faction.setReputation(parsed, repsTag.getInt(key));
                }
            }
        }

        faction.clearRelationships();
        if (tag.contains(PlayerDataSchema.TAG_RELATIONSHIPS, Tag.TAG_COMPOUND)) {
            CompoundTag relTag = tag.getCompound(PlayerDataSchema.TAG_RELATIONSHIPS);
            for (String key : relTag.getAllKeys()) {
                ResourceLocation parsed = ResourceLocation.tryParse(key);
                if (parsed != null) {
                    faction.setRelationship(parsed, relTag.getString(key));
                }
            }
        }
    }

    private static CompoundTag writeContracts(ContractData contracts, boolean includeServerPrivateData) {
        CompoundTag tag = new CompoundTag();
        ListTag activeList = new ListTag();
        for (ActiveContractState state : contracts.getActiveContractStates().values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(PlayerDataSchema.TAG_CONTRACT_ID, state.contractId().toString());
            entry.putInt(PlayerDataSchema.TAG_CONTRACT_DURATION, state.remainingDurationTicks());
            if (includeServerPrivateData) {
                entry.putLong(PlayerDataSchema.TAG_CONTRACT_SIGNED_AT, state.signedAtTick());
                entry.putInt(PlayerDataSchema.TAG_CONTRACT_VIOLATIONS, state.violationCount());
            }
            activeList.add(entry);
        }
        tag.put(PlayerDataSchema.TAG_CONTRACTS_ACTIVE, activeList);

        ListTag historyList = new ListTag();
        if (includeServerPrivateData) {
            for (ResourceLocation id : contracts.getBrokenContractsHistory()) {
                historyList.add(StringTag.valueOf(id.toString()));
            }
        }
        tag.put(PlayerDataSchema.TAG_CONTRACTS_BROKEN_HISTORY, historyList);
        tag.putBoolean(PlayerDataSchema.LEGACY_VOW_BROKEN, contracts.isVowBroken());
        return tag;
    }

    private static void readContracts(CompoundTag tag, ContractData contracts) {
        contracts.clearContracts();
        if (tag.contains(PlayerDataSchema.TAG_CONTRACTS_ACTIVE, Tag.TAG_LIST)) {
            ListTag activeList = tag.getList(PlayerDataSchema.TAG_CONTRACTS_ACTIVE, Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                CompoundTag entry = activeList.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString(PlayerDataSchema.TAG_CONTRACT_ID));
                if (id != null) {
                    long signedAt = entry.getLong(PlayerDataSchema.TAG_CONTRACT_SIGNED_AT);
                    int duration = entry.contains(PlayerDataSchema.TAG_CONTRACT_DURATION, Tag.TAG_ANY_NUMERIC)
                            ? entry.getInt(PlayerDataSchema.TAG_CONTRACT_DURATION)
                            : -1;
                    int violations = entry.getInt(PlayerDataSchema.TAG_CONTRACT_VIOLATIONS);
                    contracts.putContractState(new ActiveContractState(id, signedAt, duration, violations));
                }
            }
        }

        if (tag.contains(PlayerDataSchema.TAG_CONTRACTS_BROKEN_HISTORY, Tag.TAG_LIST)) {
            ListTag historyList = tag.getList(PlayerDataSchema.TAG_CONTRACTS_BROKEN_HISTORY, Tag.TAG_STRING);
            for (int i = 0; i < historyList.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(historyList.getString(i));
                if (id != null) {
                    contracts.recordBrokenContract(id);
                }
            }
        }
        contracts.setVowBroken(tag.getBoolean(PlayerDataSchema.LEGACY_VOW_BROKEN));
    }

    private static CompoundTag writeSkills(SkillData skills) {
        CompoundTag tag = new CompoundTag();

        ListTag unlockedList = new ListTag();
        for (ResourceLocation id : skills.getUnlockedSkills()) {
            unlockedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PlayerDataSchema.TAG_SKILLS_UNLOCKED, unlockedList);

        ListTag equippedList = new ListTag();
        for (ResourceLocation id : skills.getEquippedSkills()) {
            equippedList.add(StringTag.valueOf(id.toString()));
        }
        tag.put(PlayerDataSchema.TAG_SKILLS_EQUIPPED, equippedList);

        ListTag martialList = new ListTag();
        for (int i = 0; i < SkillData.MAX_MARTIAL_SLOTS; i++) {
            ResourceLocation id = skills.getMartialSkillInSlot(i);
            martialList.add(StringTag.valueOf(id != null ? id.toString() : ""));
        }
        tag.put(PlayerDataSchema.TAG_SKILLS_EQUIPPED_MARTIAL, martialList);
        tag.putInt(PlayerDataSchema.TAG_SKILLS_ACTIVE_MARTIAL_SLOT, skills.getActiveMartialSlot());
        tag.putInt(PlayerDataSchema.TAG_SUBJUGATION_MASTERY_TIER, skills.getSubjugationMasteryTier());

        CompoundTag cooldownsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : skills.getCooldowns().entrySet()) {
            if (entry.getValue() > 0) {
                cooldownsTag.putInt(entry.getKey().toString(), entry.getValue());
            }
        }
        tag.put(PlayerDataSchema.TAG_SKILLS_COOLDOWNS, cooldownsTag);
        tag.putFloat(PlayerDataSchema.TAG_MAX_DIVINE_RESISTANCE, skills.getMaxDivineResistance());
        tag.putFloat(PlayerDataSchema.TAG_CURRENT_DIVINE_RESISTANCE, skills.getCurrentDivineResistance());
        tag.putInt(PlayerDataSchema.TAG_DIVINE_OVERLOAD_TICKS, skills.getDivineOverloadTicks());
        tag.putInt(PlayerDataSchema.TAG_ACTIVE_DIVINE_PHASE, skills.getActiveDivinePhase());
        return tag;
    }

    private static void readSkills(CompoundTag tag, SkillData skills) {
        skills.clearAll();
        if (tag.contains(PlayerDataSchema.TAG_SKILLS_UNLOCKED, Tag.TAG_LIST)) {
            ListTag list = tag.getList(PlayerDataSchema.TAG_SKILLS_UNLOCKED, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) {
                    skills.unlockSkill(id);
                }
            }
        }
        if (tag.contains(PlayerDataSchema.TAG_SKILLS_EQUIPPED, Tag.TAG_LIST)) {
            ListTag list = tag.getList(PlayerDataSchema.TAG_SKILLS_EQUIPPED, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) {
                    skills.equipSkill(id);
                }
            }
        }
        if (tag.contains(PlayerDataSchema.TAG_SKILLS_EQUIPPED_MARTIAL, Tag.TAG_LIST)) {
            ListTag list = tag.getList(PlayerDataSchema.TAG_SKILLS_EQUIPPED_MARTIAL, Tag.TAG_STRING);
            for (int i = 0; i < Math.min(list.size(), SkillData.MAX_MARTIAL_SLOTS); i++) {
                String raw = list.getString(i);
                if (raw != null && !raw.isBlank()) {
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id != null) {
                        skills.setMartialSkillInSlot(i, id);
                    }
                }
            }
        }
        if (tag.contains(PlayerDataSchema.TAG_SKILLS_ACTIVE_MARTIAL_SLOT, Tag.TAG_ANY_NUMERIC)) {
            skills.setActiveMartialSlot(tag.getInt(PlayerDataSchema.TAG_SKILLS_ACTIVE_MARTIAL_SLOT));
        }
        if (tag.contains(PlayerDataSchema.TAG_SUBJUGATION_MASTERY_TIER, Tag.TAG_ANY_NUMERIC)) {
            skills.setSubjugationMasteryTier(tag.getInt(PlayerDataSchema.TAG_SUBJUGATION_MASTERY_TIER));
        }
        if (tag.contains(PlayerDataSchema.TAG_SKILLS_COOLDOWNS, Tag.TAG_COMPOUND)) {
            CompoundTag cooldownsTag = tag.getCompound(PlayerDataSchema.TAG_SKILLS_COOLDOWNS);
            for (String key : cooldownsTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null && cooldownsTag.contains(key, Tag.TAG_ANY_NUMERIC)) {
                    skills.setCooldown(id, cooldownsTag.getInt(key));
                }
            }
        }
        float maxRes = tag.contains(PlayerDataSchema.TAG_MAX_DIVINE_RESISTANCE, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_MAX_DIVINE_RESISTANCE)
                : SkillData.DEFAULT_MAX_DIVINE_RESISTANCE;
        skills.setMaxDivineResistance(maxRes);

        float curRes = tag.contains(PlayerDataSchema.TAG_CURRENT_DIVINE_RESISTANCE, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(PlayerDataSchema.TAG_CURRENT_DIVINE_RESISTANCE)
                : skills.getMaxDivineResistance();
        skills.setCurrentDivineResistance(curRes);

        int overload = tag.contains(PlayerDataSchema.TAG_DIVINE_OVERLOAD_TICKS, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(PlayerDataSchema.TAG_DIVINE_OVERLOAD_TICKS)
                : 0;
        skills.setDivineOverloadTicks(overload);
        if (tag.contains(PlayerDataSchema.TAG_ACTIVE_DIVINE_PHASE, Tag.TAG_ANY_NUMERIC)) {
            skills.setActiveDivinePhase(tag.getInt(PlayerDataSchema.TAG_ACTIVE_DIVINE_PHASE));
        }
    }

    private static CompoundTag writeSubjugation(SubjugationData subjugation, boolean includeServerPrivateData) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(PlayerDataSchema.TAG_SUBJUGATION_CAPACITY_BONUS, subjugation.getMaxCapacityBonus());

        ListTag boundList = new ListTag();
        for (SubjugatedEntityRecord record : subjugation.getBoundEntities()) {
            CompoundTag recTag = new CompoundTag();
            recTag.putString(PlayerDataSchema.TAG_RECORD_INSTANCE_ID, record.instanceId().toString());
            recTag.putString(PlayerDataSchema.TAG_RECORD_ENTITY_TYPE, record.entityType().toString());
            recTag.putInt(PlayerDataSchema.TAG_RECORD_LEVEL, record.level());
            recTag.putInt(PlayerDataSchema.TAG_RECORD_EXPERIENCE, record.experience());
            if (record.customName() != null) {
                recTag.putString(PlayerDataSchema.TAG_RECORD_CUSTOM_NAME, record.customName());
            }
            recTag.putString(PlayerDataSchema.TAG_RECORD_STANCE, record.stance().name());
            recTag.putBoolean(PlayerDataSchema.TAG_RECORD_SUMMONED, record.summoned());
            if (includeServerPrivateData && record.entitySnapshot() != null) {
                recTag.put(PlayerDataSchema.TAG_RECORD_SNAPSHOT, record.entitySnapshot());
            }
            boundList.add(recTag);
        }
        tag.put(PlayerDataSchema.TAG_SUBJUGATION_BOUND, boundList);
        return tag;
    }

    private static void readSubjugation(CompoundTag tag, SubjugationData subjugation) {
        subjugation.clearBoundEntities();
        subjugation.setMaxCapacityBonus(tag.getInt(PlayerDataSchema.TAG_SUBJUGATION_CAPACITY_BONUS));

        if (tag.contains(PlayerDataSchema.TAG_SUBJUGATION_BOUND, Tag.TAG_LIST)) {
            ListTag boundList = tag.getList(PlayerDataSchema.TAG_SUBJUGATION_BOUND, Tag.TAG_COMPOUND);
            for (int i = 0; i < boundList.size(); i++) {
                CompoundTag recTag = boundList.getCompound(i);
                ResourceLocation entityType = ResourceLocation.tryParse(recTag.getString(PlayerDataSchema.TAG_RECORD_ENTITY_TYPE));
                if (entityType == null) {
                    continue;
                }
                UUID instanceId = parseUuidOrRandom(recTag.getString(PlayerDataSchema.TAG_RECORD_INSTANCE_ID));
                int level = recTag.contains(PlayerDataSchema.TAG_RECORD_LEVEL, Tag.TAG_ANY_NUMERIC)
                        ? recTag.getInt(PlayerDataSchema.TAG_RECORD_LEVEL)
                        : 1;
                int xp = recTag.contains(PlayerDataSchema.TAG_RECORD_EXPERIENCE, Tag.TAG_ANY_NUMERIC)
                        ? recTag.getInt(PlayerDataSchema.TAG_RECORD_EXPERIENCE)
                        : 0;
                String customName = recTag.contains(PlayerDataSchema.TAG_RECORD_CUSTOM_NAME, Tag.TAG_STRING)
                        ? recTag.getString(PlayerDataSchema.TAG_RECORD_CUSTOM_NAME)
                        : null;
                SubjugatedEntityRecord.ServantStance stance = recTag.contains(PlayerDataSchema.TAG_RECORD_STANCE, Tag.TAG_STRING)
                        ? SubjugatedEntityRecord.ServantStance.parse(recTag.getString(PlayerDataSchema.TAG_RECORD_STANCE))
                        : SubjugatedEntityRecord.ServantStance.FOLLOW;
                boolean summoned = recTag.getBoolean(PlayerDataSchema.TAG_RECORD_SUMMONED);
                CompoundTag snapshot = recTag.contains(PlayerDataSchema.TAG_RECORD_SNAPSHOT, Tag.TAG_COMPOUND)
                        ? recTag.getCompound(PlayerDataSchema.TAG_RECORD_SNAPSHOT)
                        : null;
                subjugation.addRecord(new SubjugatedEntityRecord(instanceId, entityType, level, xp, customName, snapshot, stance, summoned));
            }
        }
    }

    private static UUID parseUuidOrRandom(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID();
        }
    }
}
