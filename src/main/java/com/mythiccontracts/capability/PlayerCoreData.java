package com.mythiccontracts.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlayerCoreData {
    public static final int MIN_REALM = 1;
    public static final int MAX_REALM = 7;
    public static final int MIN_STAGE = 1;
    public static final int MAX_STAGE = 3;

    private static final String TAG_FACTION = "Faction";
    private static final String TAG_BODY_REALM = "BodyRealm";
    private static final String TAG_BODY_STAGE = "BodyStage";
    private static final String TAG_SPIRIT_REALM = "SpiritRealm";
    private static final String TAG_SPIRIT_STAGE = "SpiritStage";
    private static final String TAG_ACTIVE_CONTRACTS = "ActiveContracts";
    private static final String TAG_SUBJUGATED_ENTITIES = "SubjugatedEntities";
    private static final String TAG_MANA_INFUSION_ACTIVE = "ManaInfusionActive";
    private static final String TAG_VOW_BROKEN = "VowBroken";

    private FactionType faction = FactionType.NONE;
    private int bodyRealm = 1;
    private int bodyStage = 1;
    private int spiritRealm = 1;
    private int spiritStage = 1;
    private final Set<ResourceLocation> activeContracts = new LinkedHashSet<>();
    private final List<ResourceLocation> subjugatedEntities = new ArrayList<>();
    private boolean manaInfusionActive = false;
    private boolean vowBroken = false;

    public FactionType getFaction() {
        return this.faction;
    }

    public void setFaction(FactionType faction) {
        this.faction = faction != null ? faction : FactionType.NONE;
    }

    public int getBodyRealm() {
        return this.bodyRealm;
    }

    public void setBodyRealm(int bodyRealm) {
        this.bodyRealm = Mth.clamp(bodyRealm, MIN_REALM, MAX_REALM);
    }

    public int getBodyStage() {
        return this.bodyStage;
    }

    public void setBodyStage(int bodyStage) {
        this.bodyStage = Mth.clamp(bodyStage, MIN_STAGE, MAX_STAGE);
    }

    public int getSpiritRealm() {
        return this.spiritRealm;
    }

    public void setSpiritRealm(int spiritRealm) {
        this.spiritRealm = Mth.clamp(spiritRealm, MIN_REALM, MAX_REALM);
    }

    public int getSpiritStage() {
        return this.spiritStage;
    }

    public void setSpiritStage(int spiritStage) {
        this.spiritStage = Mth.clamp(spiritStage, MIN_STAGE, MAX_STAGE);
    }

    public static String getStageDisplayName(int stage) {
        return switch (Mth.clamp(stage, MIN_STAGE, MAX_STAGE)) {
            case 1 -> "Base";
            case 2 -> "Fortificação";
            case 3 -> "Despertar";
            default -> "Base";
        };
    }

    public String getBodyStageName() {
        return getStageDisplayName(this.bodyStage);
    }

    public String getSpiritStageName() {
        return getStageDisplayName(this.spiritStage);
    }

    public boolean hasContract(ResourceLocation contractId) {
        return contractId != null && this.activeContracts.contains(contractId);
    }

    public boolean addContract(ResourceLocation contractId) {
        if (contractId == null) {
            return false;
        }
        return this.activeContracts.add(contractId);
    }

    public boolean removeContract(ResourceLocation contractId) {
        if (contractId == null) {
            return false;
        }
        return this.activeContracts.remove(contractId);
    }

    public void clearContracts() {
        this.activeContracts.clear();
    }

    public Set<ResourceLocation> getActiveContracts() {
        return Collections.unmodifiableSet(this.activeContracts);
    }

    public List<ResourceLocation> getSubjugatedEntities() {
        return Collections.unmodifiableList(this.subjugatedEntities);
    }

    public void addSubjugatedEntity(ResourceLocation entityId) {
        if (entityId != null) {
            this.subjugatedEntities.add(entityId);
        }
    }

    public ResourceLocation popLastSubjugatedEntity() {
        if (this.subjugatedEntities.isEmpty()) {
            return null;
        }
        return this.subjugatedEntities.remove(this.subjugatedEntities.size() - 1);
    }

    public ResourceLocation peekLastSubjugatedEntity() {
        if (this.subjugatedEntities.isEmpty()) {
            return null;
        }
        return this.subjugatedEntities.get(this.subjugatedEntities.size() - 1);
    }

    public void clearSubjugatedEntities() {
        this.subjugatedEntities.clear();
    }

    public boolean isManaInfusionActive() {
        return this.manaInfusionActive;
    }

    public void setManaInfusionActive(boolean manaInfusionActive) {
        this.manaInfusionActive = manaInfusionActive;
    }

    public boolean isVowBroken() {
        return this.vowBroken;
    }

    public void setVowBroken(boolean vowBroken) {
        this.vowBroken = vowBroken;
    }

    public void copyFrom(PlayerCoreData other) {
        copyFrom(other, false);
    }

    public void copyFrom(PlayerCoreData other, boolean wasDeath) {
        if (other == null) {
            return;
        }
        this.faction = other.faction;
        this.bodyRealm = other.bodyRealm;
        this.bodyStage = other.bodyStage;
        this.spiritRealm = other.spiritRealm;
        this.spiritStage = other.spiritStage;

        this.activeContracts.clear();
        this.activeContracts.addAll(other.activeContracts);

        this.subjugatedEntities.clear();
        this.subjugatedEntities.addAll(other.subjugatedEntities);

        this.manaInfusionActive = wasDeath ? false : other.manaInfusionActive;
        this.vowBroken = other.vowBroken;
    }

    public void saveNBTData(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        tag.putString(TAG_FACTION, this.faction.getId());
        tag.putInt(TAG_BODY_REALM, this.bodyRealm);
        tag.putInt(TAG_BODY_STAGE, this.bodyStage);
        tag.putInt(TAG_SPIRIT_REALM, this.spiritRealm);
        tag.putInt(TAG_SPIRIT_STAGE, this.spiritStage);

        ListTag contractsTag = new ListTag();
        for (ResourceLocation contractId : this.activeContracts) {
            contractsTag.add(StringTag.valueOf(contractId.toString()));
        }
        tag.put(TAG_ACTIVE_CONTRACTS, contractsTag);

        ListTag subjugatedTag = new ListTag();
        for (ResourceLocation entityId : this.subjugatedEntities) {
            subjugatedTag.add(StringTag.valueOf(entityId.toString()));
        }
        tag.put(TAG_SUBJUGATED_ENTITIES, subjugatedTag);

        tag.putBoolean(TAG_MANA_INFUSION_ACTIVE, this.manaInfusionActive);
        tag.putBoolean(TAG_VOW_BROKEN, this.vowBroken);
    }

    public void loadNBTData(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        this.faction = FactionType.fromString(tag.getString(TAG_FACTION));
        this.setBodyRealm(tag.contains(TAG_BODY_REALM, Tag.TAG_ANY_NUMERIC) ? tag.getInt(TAG_BODY_REALM) : 1);
        this.setBodyStage(tag.contains(TAG_BODY_STAGE, Tag.TAG_ANY_NUMERIC) ? tag.getInt(TAG_BODY_STAGE) : 1);
        this.setSpiritRealm(tag.contains(TAG_SPIRIT_REALM, Tag.TAG_ANY_NUMERIC) ? tag.getInt(TAG_SPIRIT_REALM) : 1);
        this.setSpiritStage(tag.contains(TAG_SPIRIT_STAGE, Tag.TAG_ANY_NUMERIC) ? tag.getInt(TAG_SPIRIT_STAGE) : 1);

        this.activeContracts.clear();
        if (tag.contains(TAG_ACTIVE_CONTRACTS, Tag.TAG_LIST)) {
            ListTag contractsTag = tag.getList(TAG_ACTIVE_CONTRACTS, Tag.TAG_STRING);
            for (int i = 0; i < contractsTag.size(); i++) {
                ResourceLocation parsed = ResourceLocation.tryParse(contractsTag.getString(i));
                if (parsed != null) {
                    this.activeContracts.add(parsed);
                }
            }
        }

        this.subjugatedEntities.clear();
        if (tag.contains(TAG_SUBJUGATED_ENTITIES, Tag.TAG_LIST)) {
            ListTag subjugatedTag = tag.getList(TAG_SUBJUGATED_ENTITIES, Tag.TAG_STRING);
            for (int i = 0; i < subjugatedTag.size(); i++) {
                ResourceLocation parsed = ResourceLocation.tryParse(subjugatedTag.getString(i));
                if (parsed != null) {
                    this.subjugatedEntities.add(parsed);
                }
            }
        }

        this.manaInfusionActive = tag.getBoolean(TAG_MANA_INFUSION_ACTIVE);
        this.vowBroken = tag.getBoolean(TAG_VOW_BROKEN);
    }
}
