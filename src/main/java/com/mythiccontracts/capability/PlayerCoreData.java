package com.mythiccontracts.capability;

import com.mythiccontracts.core.persistence.PlayerDataSchema;
import com.mythiccontracts.core.persistence.PlayerDataSerializer;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;

/**
 * Adaptador de capability mantido para compatibilidade incremental durante a migração v1 -> v2.
 * Todo o estado reside exclusivamente no agregador modular {@link PlayerData}.
 */
public class PlayerCoreData {
    public static final int MIN_REALM = 1;
    public static final int MAX_REALM = 7;
    public static final int MIN_STAGE = 1;
    public static final int MAX_STAGE = 3;

    private final PlayerData playerData = new PlayerData();

    public PlayerData getPlayerData() {
        return this.playerData;
    }

    public FactionType getFaction() {
        ResourceLocation id = this.playerData.getFaction().getCurrentFactionId();
        return FactionType.fromString(id.getPath());
    }

    public void setFaction(FactionType faction) {
        FactionType safe = faction != null ? faction : FactionType.NONE;
        this.playerData.getFaction().setCurrentFactionId(
                PlayerDataSchema.factionIdFromLegacy(safe.getId())
        );
    }

    public int getBodyRealm() {
        return this.playerData.getCultivation().getBodyRealm();
    }

    public void setBodyRealm(int bodyRealm) {
        int clamped = Math.max(MIN_REALM, bodyRealm);
        RealmDefinition def = RealmRegistry.getRealmByOrder(CultivationPath.BODY, clamped);
        this.playerData.getCultivation().setBodyRealm(def != null ? def.order() : clamped);
        this.playerData.getCultivation().setBodyRealmId(def != null ? def.id() : PlayerDataSchema.bodyRealmIdFromNumber(clamped));
        EnergyService.recalculateMaxQi(this.playerData);
    }

    public int getBodyStage() {
        return this.playerData.getCultivation().getBodyStage();
    }

    public void setBodyStage(int bodyStage) {
        RealmDefinition def = CultivationService.getCurrentRealm(this.playerData, CultivationPath.BODY);
        int clamped = Mth.clamp(bodyStage, MIN_STAGE, def.getStageCount());
        this.playerData.getCultivation().setBodyStage(clamped);
        EnergyService.recalculateMaxQi(this.playerData);
    }

    public int getSpiritRealm() {
        return this.playerData.getCultivation().getSpiritRealm();
    }

    public void setSpiritRealm(int spiritRealm) {
        int clamped = Math.max(MIN_REALM, spiritRealm);
        RealmDefinition def = RealmRegistry.getRealmByOrder(CultivationPath.SPIRIT, clamped);
        this.playerData.getCultivation().setSpiritRealm(def != null ? def.order() : clamped);
        this.playerData.getCultivation().setSpiritRealmId(def != null ? def.id() : PlayerDataSchema.spiritRealmIdFromNumber(clamped));
        EnergyService.recalculateMaxQi(this.playerData);
    }

    public int getSpiritStage() {
        return this.playerData.getCultivation().getSpiritStage();
    }

    public void setSpiritStage(int spiritStage) {
        RealmDefinition def = CultivationService.getCurrentRealm(this.playerData, CultivationPath.SPIRIT);
        int clamped = Mth.clamp(spiritStage, MIN_STAGE, def.getStageCount());
        this.playerData.getCultivation().setSpiritStage(clamped);
        EnergyService.recalculateMaxQi(this.playerData);
    }

    public static String getStageDisplayName(int stage) {
        return switch (Math.max(MIN_STAGE, stage)) {
            case 1 -> "Base";
            case 2 -> "Fortificação";
            case 3 -> "Despertar";
            default -> "Estágio " + stage;
        };
    }

    public String getBodyStageName() {
        return CultivationService.getStageDisplayName(this.playerData, CultivationPath.BODY);
    }

    public String getSpiritStageName() {
        return CultivationService.getStageDisplayName(this.playerData, CultivationPath.SPIRIT);
    }

    public boolean hasContract(ResourceLocation contractId) {
        return this.playerData.getContracts().hasContract(contractId);
    }

    public boolean addContract(ResourceLocation contractId) {
        return this.playerData.getContracts().addContract(contractId);
    }

    public boolean removeContract(ResourceLocation contractId) {
        return this.playerData.getContracts().removeContract(contractId);
    }

    public void clearContracts() {
        this.playerData.getContracts().clearContracts();
    }

    public Set<ResourceLocation> getActiveContracts() {
        return this.playerData.getContracts().getActiveContractIds();
    }

    public List<ResourceLocation> getSubjugatedEntities() {
        return this.playerData.getSubjugation().getBoundEntityTypes();
    }

    public int getSubjugationEntitiesCountSafe() {
        return this.playerData.getSubjugation().getBoundEntities().size();
    }

    public void addSubjugatedEntity(ResourceLocation entityId) {
        this.playerData.getSubjugation().addEntityType(entityId);
    }

    public ResourceLocation popLastSubjugatedEntity() {
        var record = this.playerData.getSubjugation().popLastRecord();
        return record != null ? record.entityType() : null;
    }

    public ResourceLocation peekLastSubjugatedEntity() {
        var record = this.playerData.getSubjugation().peekLastRecord();
        return record != null ? record.entityType() : null;
    }

    public void clearSubjugatedEntities() {
        this.playerData.getSubjugation().clearBoundEntities();
    }

    public boolean isManaInfusionActive() {
        return this.playerData.getCultivation().isManaInfusionActive();
    }

    public void setManaInfusionActive(boolean manaInfusionActive) {
        this.playerData.getCultivation().setManaInfusionActive(manaInfusionActive);
    }

    public boolean isVowBroken() {
        return this.playerData.getContracts().isVowBroken();
    }

    public void setVowBroken(boolean vowBroken) {
        this.playerData.getContracts().setVowBroken(vowBroken);
    }

    public float getMaxQi() {
        EnergyService.recalculateMaxQi(this.playerData);
        return this.playerData.getCultivation().getMaxQiCapacity();
    }

    public float getCurrentQi() {
        return this.playerData.getCultivation().getCurrentQi();
    }

    public void setCurrentQi(float currentQi) {
        EnergyService.recalculateMaxQi(this.playerData);
        this.playerData.getCultivation().setCurrentQi(currentQi);
    }

    public boolean consumeQi(float amount) {
        EnergyService.recalculateMaxQi(this.playerData);
        return EnergyService.consumeQi(this.playerData, amount);
    }

    public void addQi(float amount) {
        EnergyService.recalculateMaxQi(this.playerData);
        EnergyService.addQi(this.playerData, amount);
    }

    public int getUniqueSkillCooldownTicks() {
        return this.playerData.getSkills().getUniqueSkillCooldownTicks();
    }

    public void setUniqueSkillCooldownTicks(int ticks) {
        this.playerData.getSkills().setUniqueSkillCooldownTicks(ticks);
    }

    public void tickCooldowns() {
        this.playerData.getSkills().tickAllCooldowns();
    }

    public void copyFrom(PlayerCoreData other) {
        copyFrom(other, false);
    }

    public void copyFrom(PlayerCoreData other, boolean wasDeath) {
        if (other == null) {
            return;
        }
        this.playerData.copyFrom(other.playerData, wasDeath);
    }

    public void saveNBTData(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        CompoundTag serialized = PlayerDataSerializer.writeFull(this.playerData);
        for (String key : serialized.getAllKeys()) {
            tag.put(key, serialized.get(key).copy());
        }
    }

    public void loadNBTData(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        PlayerDataSerializer.read(tag, this.playerData);
        EnergyService.recalculateMaxQi(this.playerData);
    }
}
