package com.mythiccontracts.core.player;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.core.persistence.PlayerDataSchema;
import com.mythiccontracts.core.service.EnergyService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataTest {

    @Test
    void copyFromHandlesDeathVsDimensionReturnCorrectly() {
        PlayerData source = new PlayerData();
        source.getFaction().setCurrentFactionId(new ResourceLocation("mythic_contracts", "juramentados_da_luz"));
        source.getCultivation().setBodyRealm(3);
        source.getCultivation().setSpiritRealm(3);
        EnergyService.recalculateMaxQi(source);
        source.getCultivation().setCurrentQi(42.0F);
        source.getCultivation().setManaInfusionActive(true);
        source.getContracts().addContract(new ResourceLocation("mythic_contracts", "paladin_vow"));
        source.getSkills().unlockSkill(new ResourceLocation("mythic_contracts", "solar_judgment"));
        source.getSkills().setUniqueSkillCooldownTicks(120);
        source.getSubjugation().addEntityType(new ResourceLocation("minecraft", "iron_golem"));

        // Retorno do The End (wasDeath = false): preserva tudo inclusive Qi atual, infusão e cooldown
        PlayerData endClone = new PlayerData();
        endClone.copyFrom(source, false);
        assertEquals(42.0F, endClone.getCultivation().getCurrentQi(), 0.01F);
        assertTrue(endClone.getCultivation().isManaInfusionActive());
        assertEquals(120, endClone.getSkills().getUniqueSkillCooldownTicks());

        // Morte real (wasDeath = true): preserva progresso, reseta infusão e cooldown e restaura Qi ao máximo
        PlayerData deathClone = new PlayerData();
        deathClone.copyFrom(source, true);
        assertEquals(source.getCultivation().getMaxQiCapacity(), deathClone.getCultivation().getCurrentQi(), 0.01F);
        assertFalse(deathClone.getCultivation().isManaInfusionActive());
        assertEquals(0, deathClone.getSkills().getUniqueSkillCooldownTicks());
        assertTrue(deathClone.getContracts().hasContract(new ResourceLocation("mythic_contracts", "paladin_vow")));
        assertEquals(1, deathClone.getSubjugation().getBoundEntities().size());
    }

    @Test
    void energyServiceDrainsAndDeactivatesInfusionOnDepletion() {
        PlayerData data = new PlayerData();
        data.getCultivation().setCurrentQi(0.1F);
        data.getCultivation().setManaInfusionActive(true);

        boolean depleted = EnergyService.tickEnergy(data);
        assertTrue(depleted);
        assertFalse(data.getCultivation().isManaInfusionActive());
        assertEquals(0.0F, data.getCultivation().getCurrentQi(), 0.001F);
    }

    @Test
    void legacyCoreDataAdapterWritesV2SchemaAndReadsV1Compatibly() {
        PlayerCoreData adapter = new PlayerCoreData();
        adapter.setFaction(FactionType.ERRANTES_DO_VEU);
        adapter.setBodyRealm(2);
        adapter.setBodyStage(3);
        adapter.addContract(new ResourceLocation("mythic_contracts", "blood_oath"));

        CompoundTag saved = new CompoundTag();
        adapter.saveNBTData(saved);
        assertEquals(PlayerDataSchema.CURRENT_VERSION, saved.getInt(PlayerDataSchema.TAG_DATA_VERSION));

        PlayerCoreData loaded = new PlayerCoreData();
        loaded.loadNBTData(saved);
        assertEquals(FactionType.ERRANTES_DO_VEU, loaded.getFaction());
        assertEquals(2, loaded.getBodyRealm());
        assertEquals(3, loaded.getBodyStage());
        assertTrue(loaded.hasContract(new ResourceLocation("mythic_contracts", "blood_oath")));
    }
}
