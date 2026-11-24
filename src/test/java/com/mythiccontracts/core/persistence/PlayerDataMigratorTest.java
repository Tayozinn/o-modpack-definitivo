package com.mythiccontracts.core.persistence;

import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SubjugatedEntityRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataMigratorTest {

    @Test
    void migratesCompleteV1TagToV2WithoutDataLoss() {
        CompoundTag v1 = new CompoundTag();
        v1.putString("Faction", "quebradores_de_tronos");
        v1.putInt("BodyRealm", 4);
        v1.putInt("BodyStage", 3);
        v1.putInt("SpiritRealm", 5);
        v1.putInt("SpiritStage", 2);
        v1.putBoolean("ManaInfusionActive", true);
        v1.putBoolean("VowBroken", true);
        v1.putFloat("CurrentQi", 142.5F);
        v1.putInt("UniqueSkillCooldown", 85);

        ListTag contracts = new ListTag();
        contracts.add(StringTag.valueOf("mythic_contracts:lucifer_pact"));
        contracts.add(StringTag.valueOf("mythic_contracts:throne_breaker_seal"));
        v1.put("ActiveContracts", contracts);

        ListTag subjugated = new ListTag();
        subjugated.add(StringTag.valueOf("minecraft:warden"));
        subjugated.add(StringTag.valueOf("minecraft:wither"));
        v1.put("SubjugatedEntities", subjugated);

        PlayerData data = new PlayerData();
        PlayerDataSerializer.read(v1, data);

        assertEquals(new ResourceLocation("mythic_contracts", "quebradores_de_tronos"), data.getFaction().getCurrentFactionId());
        assertEquals(4, data.getCultivation().getBodyRealm());
        assertEquals(3, data.getCultivation().getBodyStage());
        assertEquals(new ResourceLocation("mythic_contracts", "body_realm_4"), data.getCultivation().getBodyRealmId());
        assertEquals(5, data.getCultivation().getSpiritRealm());
        assertEquals(2, data.getCultivation().getSpiritStage());
        assertEquals(new ResourceLocation("mythic_contracts", "spirit_realm_5"), data.getCultivation().getSpiritRealmId());

        // 100 + 35*(5-1) + 10*(2-1) + 10*(4-1) = 100 + 140 + 10 + 30 = 280
        assertEquals(280.0F, data.getCultivation().getMaxQiCapacity(), 0.01F);
        assertEquals(142.5F, data.getCultivation().getCurrentQi(), 0.01F);
        assertTrue(data.getCultivation().isManaInfusionActive());

        assertTrue(data.getContracts().hasContract(new ResourceLocation("mythic_contracts", "lucifer_pact")));
        assertTrue(data.getContracts().hasContract(new ResourceLocation("mythic_contracts", "throne_breaker_seal")));
        assertTrue(data.getContracts().isVowBroken());
        assertTrue(data.getContracts().getBrokenContractsHistory().contains(new ResourceLocation("mythic_contracts", "paladin_vow")));

        assertEquals(85, data.getSkills().getUniqueSkillCooldownTicks());
        assertEquals(2, data.getSubjugation().getBoundEntities().size());
        assertEquals(new ResourceLocation("minecraft", "warden"), data.getSubjugation().getBoundEntityTypes().get(0));
        assertEquals(new ResourceLocation("minecraft", "wither"), data.getSubjugation().getBoundEntityTypes().get(1));
    }

    @Test
    void migratesPrePhase4V1TagInitializingFullQi() {
        CompoundTag v1 = new CompoundTag();
        v1.putString("Faction", "juramentados_da_luz");
        v1.putInt("BodyRealm", 2);
        v1.putInt("BodyStage", 1);
        v1.putInt("SpiritRealm", 3);
        v1.putInt("SpiritStage", 2);

        PlayerData data = new PlayerData();
        PlayerDataSerializer.read(v1, data);

        // 100 + 35*(3-1) + 10*(2-1) + 10*(2-1) = 100 + 70 + 10 + 10 = 190
        assertEquals(190.0F, data.getCultivation().getMaxQiCapacity(), 0.01F);
        assertEquals(190.0F, data.getCultivation().getCurrentQi(), 0.01F);
        assertEquals(0, data.getSkills().getUniqueSkillCooldownTicks());
    }

    @Test
    void writeFullAndClientSyncRespectVersionAndServerOnlyPrivacy() {
        PlayerData original = new PlayerData();
        original.getFaction().setCurrentFactionId(new ResourceLocation("mythic_contracts", "irmandade_divida_eterna"));
        original.getFaction().setReputation(new ResourceLocation("mythic_contracts", "juramentados_da_luz"), -500);
        original.getContracts().addContract(new ResourceLocation("mythic_contracts", "blood_oath"), 12345L, 600);
        original.getContracts().recordBrokenContract(new ResourceLocation("mythic_contracts", "paladin_vow"));

        CompoundTag privateSnapshot = new CompoundTag();
        privateSnapshot.putString("SecretServerTag", "internal-only");
        original.getSubjugation().addRecord(new SubjugatedEntityRecord(
                UUID.randomUUID(),
                new ResourceLocation("minecraft", "warden"),
                3,
                450,
                "Guardião Alfa",
                privateSnapshot
        ));

        CompoundTag fullTag = PlayerDataSerializer.writeFull(original);
        assertEquals(PlayerDataSchema.CURRENT_VERSION, fullTag.getInt(PlayerDataSchema.TAG_DATA_VERSION));

        PlayerData restoredFull = new PlayerData();
        PlayerDataSerializer.read(fullTag, restoredFull);
        assertEquals(-500, restoredFull.getFaction().getReputation(new ResourceLocation("mythic_contracts", "juramentados_da_luz")));
        assertEquals(12345L, restoredFull.getContracts().getActiveContractStates().get(new ResourceLocation("mythic_contracts", "blood_oath")).signedAtTick());
        assertEquals(1, restoredFull.getContracts().getBrokenContractsHistory().size());
        assertNotNull(restoredFull.getSubjugation().getBoundEntities().get(0).entitySnapshot());

        CompoundTag clientSyncTag = PlayerDataSerializer.writeClientSync(original);
        PlayerData clientView = new PlayerData();
        PlayerDataSerializer.read(clientSyncTag, clientView);
        assertTrue(clientView.getContracts().getBrokenContractsHistory().isEmpty());
        assertNull(clientView.getSubjugation().getBoundEntities().get(0).entitySnapshot());
        assertEquals("Guardião Alfa", clientView.getSubjugation().getBoundEntities().get(0).customName());
    }

    @Test
    void handlesMalformedOrEmptyTagsGracefully() {
        PlayerData data = new PlayerData();
        CompoundTag malformed = new CompoundTag();
        malformed.putString("Faction", "not a valid:resource:location!!!");
        malformed.putInt("BodyRealm", 999);
        malformed.putInt("SpiritStage", -50);

        assertDoesNotThrow(() -> PlayerDataSerializer.read(malformed, data));
        assertEquals(7, data.getCultivation().getBodyRealm());
        assertEquals(1, data.getCultivation().getSpiritStage());
    }

    @Test
    void migratesV2TagToV3PreservingProgressAndInitializingVitalBars() {
        CompoundTag v2 = new CompoundTag();
        v2.putInt(PlayerDataSchema.TAG_DATA_VERSION, PlayerDataSchema.VERSION_V2);

        CompoundTag cult = new CompoundTag();
        cult.putString(PlayerDataSchema.TAG_BODY_REALM_ID, "mythic_contracts:body_realm_3");
        cult.putInt(PlayerDataSchema.LEGACY_BODY_REALM, 3);
        cult.putInt(PlayerDataSchema.LEGACY_BODY_STAGE, 2);
        cult.putString(PlayerDataSchema.TAG_SPIRIT_REALM_ID, "mythic_contracts:spirit_realm_4");
        cult.putInt(PlayerDataSchema.LEGACY_SPIRIT_REALM, 4);
        cult.putInt(PlayerDataSchema.LEGACY_SPIRIT_STAGE, 3);
        cult.putFloat(PlayerDataSchema.TAG_MAX_QI_CAPACITY, 245.0F);
        cult.putFloat(PlayerDataSchema.LEGACY_CURRENT_QI, 111.0F);
        v2.put(PlayerDataSchema.TAG_SECTION_CULTIVATION, cult);

        PlayerData data = new PlayerData();
        PlayerDataSerializer.read(v2, data);

        // Integridade Espiritual: 100 + 40*(4-1) + 12*(3-1) + 10*(3-1) = 100 + 120 + 24 + 20 = 264
        assertEquals(264.0F, data.getCultivation().getMaxSpiritualIntegrity(), 0.01F);
        assertEquals(264.0F, data.getCultivation().getCurrentSpiritualIntegrity(), 0.01F);
        assertEquals(0, data.getCultivation().getSpiritShatterTicks());

        // Stamina (Vigor): 100 + 25*(3-1) + 5*(2-1) = 155
        assertEquals(155.0F, data.getCultivation().getMaxStamina(), 0.01F);
        assertEquals(155.0F, data.getCultivation().getCurrentStamina(), 0.01F);
        assertEquals(111.0F, data.getCultivation().getCurrentQi(), 0.01F);

        CompoundTag savedV4 = PlayerDataSerializer.writeFull(data);
        assertEquals(PlayerDataSchema.CURRENT_VERSION, savedV4.getInt(PlayerDataSchema.TAG_DATA_VERSION));
    }

    @Test
    void migratesV3TagToV4AndPreservesMartialSlotsMasteryAndServantStance() {
        CompoundTag v3 = new CompoundTag();
        v3.putInt(PlayerDataSchema.TAG_DATA_VERSION, PlayerDataSchema.VERSION_V3);
        v3.put(PlayerDataSchema.TAG_SECTION_CULTIVATION, PlayerDataSerializer.writeFull(new PlayerData())
                .getCompound(PlayerDataSchema.TAG_SECTION_CULTIVATION));

        PlayerData migrated = new PlayerData();
        PlayerDataSerializer.read(v3, migrated);
        assertEquals(1, migrated.getSkills().getSubjugationMasteryTier());
        assertEquals(0, migrated.getSkills().getActiveMartialSlot());

        ResourceLocation tech1 = new ResourceLocation("mythic_contracts", "gale_meridian_step");
        ResourceLocation tech2 = new ResourceLocation("mythic_contracts", "soul_subjugation");
        migrated.getSkills().unlockSkill(tech1);
        migrated.getSkills().unlockSkill(tech2);
        migrated.getSkills().setMartialSkillInSlot(0, tech1);
        migrated.getSkills().setMartialSkillInSlot(1, tech2);
        migrated.getSkills().setActiveMartialSlot(1);
        migrated.getSkills().setActiveDivinePhase(2);
        migrated.getSkills().setSubjugationMasteryTier(3);
        migrated.getCultivation().setSpiritCombatCooldownTicks(65);
        migrated.getSubjugation().addRecord(new SubjugatedEntityRecord(
                UUID.randomUUID(),
                new ResourceLocation("minecraft", "ravager"),
                4,
                900,
                "Fera de Ferro",
                new CompoundTag(),
                SubjugatedEntityRecord.ServantStance.AGGRESSIVE,
                true
        ));

        CompoundTag v4Full = PlayerDataSerializer.writeFull(migrated);
        assertEquals(4, v4Full.getInt(PlayerDataSchema.TAG_DATA_VERSION));

        PlayerData restored = new PlayerData();
        PlayerDataSerializer.read(v4Full, restored);
        assertEquals(tech1, restored.getSkills().getMartialSkillInSlot(0));
        assertEquals(tech2, restored.getSkills().getMartialSkillInSlot(1));
        assertEquals(1, restored.getSkills().getActiveMartialSlot());
        assertEquals(2, restored.getSkills().getActiveDivinePhase());
        assertEquals(3, restored.getSkills().getSubjugationMasteryTier());
        assertEquals(65, restored.getCultivation().getSpiritCombatCooldownTicks());
        assertEquals(SubjugatedEntityRecord.ServantStance.AGGRESSIVE,
                restored.getSubjugation().getBoundEntities().get(0).stance());
        assertTrue(restored.getSubjugation().getBoundEntities().get(0).summoned());

        CompoundTag v4Sync = PlayerDataSerializer.writeClientSync(migrated);
        PlayerData synced = new PlayerData();
        PlayerDataSerializer.read(v4Sync, synced);
        assertEquals(2, synced.getSkills().getActiveDivinePhase());
        assertNull(synced.getSubjugation().getBoundEntities().get(0).entitySnapshot());
        assertEquals(SubjugatedEntityRecord.ServantStance.AGGRESSIVE,
                synced.getSubjugation().getBoundEntities().get(0).stance());
        assertTrue(synced.getSubjugation().getBoundEntities().get(0).summoned());
    }
}
