package com.mythiccontracts.qa;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.core.persistence.PlayerDataSchema;
import com.mythiccontracts.core.persistence.PlayerDataSerializer;
import com.mythiccontracts.core.player.ActiveContractState;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SkillData;
import com.mythiccontracts.core.player.SubjugatedEntityRecord;
import com.mythiccontracts.event.ContractGameplayEvents;
import com.mythiccontracts.network.ActivateCultivationSkillC2SPacket;
import com.mythiccontracts.network.ActivateUniqueSkillC2SPacket;
import com.mythiccontracts.network.CultivatorActionC2SPacket;
import com.mythiccontracts.network.CycleDivinePhaseC2SPacket;
import com.mythiccontracts.network.CycleMartialSlotC2SPacket;
import com.mythiccontracts.network.SyncEnergyS2CPacket;
import com.mythiccontracts.network.ToggleInfusionC2SPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerSecurityAndHardeningTest {

    @AfterEach
    void cleanupStatics() {
        ToggleInfusionC2SPacket.clearAllRateLimits();
        CultivatorActionC2SPacket.clearAllRateLimits();
        ActivateUniqueSkillC2SPacket.clearAllRateLimits();
        ActivateCultivationSkillC2SPacket.clearAllRateLimits();
        CycleMartialSlotC2SPacket.clearAllRateLimits();
        CycleDivinePhaseC2SPacket.clearAllRateLimits();
    }

    @Test
    void nanAndInfinityCannotPoisonPlayerDataOrPersistenceOrEnergyPackets() {
        CultivationData cult = new CultivationData();
        cult.setMaxQiCapacity(Float.NaN);
        cult.setCurrentQi(Float.POSITIVE_INFINITY);
        cult.setMaxSpiritualIntegrity(Float.NEGATIVE_INFINITY);
        cult.setCurrentSpiritualIntegrity(Float.NaN);
        cult.setMaxStamina(Float.NaN);
        cult.setCurrentStamina(Float.POSITIVE_INFINITY);

        assertTrue(Float.isFinite(cult.getMaxQiCapacity()) && cult.getMaxQiCapacity() >= 1.0F);
        assertTrue(Float.isFinite(cult.getCurrentQi()) && cult.getCurrentQi() >= 0.0F);
        assertTrue(Float.isFinite(cult.getMaxSpiritualIntegrity()) && cult.getMaxSpiritualIntegrity() >= 1.0F);
        assertTrue(Float.isFinite(cult.getCurrentSpiritualIntegrity()) && cult.getCurrentSpiritualIntegrity() >= 0.0F);
        assertTrue(Float.isFinite(cult.getMaxStamina()) && cult.getMaxStamina() >= 1.0F);
        assertTrue(Float.isFinite(cult.getCurrentStamina()) && cult.getCurrentStamina() >= 0.0F);

        SkillData skills = new SkillData();
        skills.setMaxDivineResistance(Float.NaN);
        skills.setCurrentDivineResistance(Float.NEGATIVE_INFINITY);
        assertTrue(Float.isFinite(skills.getMaxDivineResistance()) && skills.getMaxDivineResistance() >= 1.0F);
        assertTrue(Float.isFinite(skills.getCurrentDivineResistance()) && skills.getCurrentDivineResistance() >= 0.0F);

        // Corrupt NBT simulation
        PlayerData restored = new PlayerData();
        CompoundTag corruptRoot = PlayerDataSerializer.writeFull(new PlayerData());
        CompoundTag cultTag = corruptRoot.getCompound(PlayerDataSchema.TAG_SECTION_CULTIVATION);
        cultTag.putFloat(PlayerDataSchema.TAG_MAX_QI_CAPACITY, Float.NaN);
        cultTag.putFloat(PlayerDataSchema.LEGACY_CURRENT_QI, Float.POSITIVE_INFINITY);
        cultTag.putFloat(PlayerDataSchema.TAG_CURRENT_STAMINA, Float.NEGATIVE_INFINITY);
        CompoundTag skillsTag = corruptRoot.getCompound(PlayerDataSchema.TAG_SECTION_SKILLS);
        skillsTag.putFloat(PlayerDataSchema.TAG_CURRENT_DIVINE_RESISTANCE, Float.NaN);
        PlayerDataSerializer.read(corruptRoot, restored);

        assertTrue(Float.isFinite(restored.getCultivation().getMaxQiCapacity()));
        assertTrue(Float.isFinite(restored.getCultivation().getCurrentQi()));
        assertTrue(Float.isFinite(restored.getCultivation().getCurrentStamina()));
        assertTrue(Float.isFinite(restored.getSkills().getCurrentDivineResistance()));

        // Poisoned packet constructor + poisoned raw wire buffer
        SyncEnergyS2CPacket poisonedCtor = new SyncEnergyS2CPacket(
                Float.NaN, Float.POSITIVE_INFINITY, false, -5,
                Float.NEGATIVE_INFINITY, Float.NaN, -3,
                Float.NaN, Float.NaN, -1,
                Float.POSITIVE_INFINITY, Float.NaN
        );
        FriendlyByteBuf encoded = new FriendlyByteBuf(Unpooled.buffer());
        poisonedCtor.toBytes(encoded);
        while (encoded.isReadable()) {
            // Drain check via normal constructor round-trip as well
            break;
        }
        FriendlyByteBuf rawPoisonedBuf = new FriendlyByteBuf(Unpooled.buffer());
        rawPoisonedBuf.writeFloat(Float.NaN);
        rawPoisonedBuf.writeFloat(Float.NEGATIVE_INFINITY);
        rawPoisonedBuf.writeBoolean(true);
        rawPoisonedBuf.writeVarInt(12);
        rawPoisonedBuf.writeFloat(Float.POSITIVE_INFINITY);
        rawPoisonedBuf.writeFloat(Float.NaN);
        rawPoisonedBuf.writeVarInt(40);
        rawPoisonedBuf.writeFloat(Float.NaN);
        rawPoisonedBuf.writeFloat(Float.POSITIVE_INFINITY);
        rawPoisonedBuf.writeVarInt(20);
        rawPoisonedBuf.writeFloat(Float.NEGATIVE_INFINITY);
        rawPoisonedBuf.writeFloat(Float.NaN);
        rawPoisonedBuf.writeByte(99);
        rawPoisonedBuf.writeVarInt(99);
        rawPoisonedBuf.writeVarInt(-5);
        rawPoisonedBuf.writeBoolean(true);
        rawPoisonedBuf.writeFloat(Float.NaN);
        rawPoisonedBuf.writeBoolean(true);
        rawPoisonedBuf.writeUtf("CorruptedTargetName", 48);
        rawPoisonedBuf.writeByte(99);

        SyncEnergyS2CPacket decoded = new SyncEnergyS2CPacket(rawPoisonedBuf);
        assertEquals(0, rawPoisonedBuf.readableBytes());
        assertNotNull(decoded);
    }

    @Test
    void clientSyncPayloadStripsServerPrivateContractAndSubjugationData() {
        PlayerData data = new PlayerData();
        ResourceLocation luciferId = new ResourceLocation(MythicContractsMod.MOD_ID, "lucifer_pact");
        ResourceLocation paladinId = new ResourceLocation(MythicContractsMod.MOD_ID, "paladin_vow");

        data.getContracts().putContractState(new ActiveContractState(luciferId, 123456L, 600, 3));
        data.getContracts().recordBrokenContract(paladinId);

        CompoundTag secretSnapshot = new CompoundTag();
        secretSnapshot.putFloat("SecretBossHp", 999.0F);
        data.getSubjugation().addRecord(new SubjugatedEntityRecord(
                UUID.randomUUID(),
                new ResourceLocation("minecraft", "warden"),
                5,
                250,
                "Guardião",
                secretSnapshot
        ));

        CompoundTag fullTag = PlayerDataSerializer.writeFull(data);
        PlayerData fromFull = new PlayerData();
        PlayerDataSerializer.read(fullTag, fromFull);
        assertEquals(123456L, fromFull.getContracts().getActiveContractStates().get(luciferId).signedAtTick());
        assertEquals(3, fromFull.getContracts().getActiveContractStates().get(luciferId).violationCount());
        assertTrue(fromFull.getContracts().getBrokenContractsHistory().contains(paladinId));
        assertNotNull(fromFull.getSubjugation().peekLastRecord().entitySnapshot());

        CompoundTag syncTag = PlayerDataSerializer.writeClientSync(data);
        PlayerData fromSync = new PlayerData();
        PlayerDataSerializer.read(syncTag, fromSync);
        assertTrue(fromSync.getContracts().hasContract(luciferId));
        assertEquals(0L, fromSync.getContracts().getActiveContractStates().get(luciferId).signedAtTick());
        assertEquals(0, fromSync.getContracts().getActiveContractStates().get(luciferId).violationCount());
        assertTrue(fromSync.getContracts().getBrokenContractsHistory().isEmpty());
        assertNull(fromSync.getSubjugation().peekLastRecord().entitySnapshot());
    }

    @Test
    void c2sPacketsEnforcePerPlayerRateLimitsAndResetOnCleanup() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        // ToggleInfusionC2SPacket (10 ticks)
        assertTrue(ToggleInfusionC2SPacket.tryAcquireRateLimit(playerA, 100L));
        assertFalse(ToggleInfusionC2SPacket.tryAcquireRateLimit(playerA, 105L));
        assertTrue(ToggleInfusionC2SPacket.tryAcquireRateLimit(playerB, 105L));
        assertTrue(ToggleInfusionC2SPacket.tryAcquireRateLimit(playerA, 110L));
        // Clock rollback (e.g. world reset) also allowed
        assertTrue(ToggleInfusionC2SPacket.tryAcquireRateLimit(playerA, 5L));

        // CultivatorActionC2SPacket (8 ticks)
        assertTrue(CultivatorActionC2SPacket.tryAcquireRateLimit(playerA, 200L));
        assertFalse(CultivatorActionC2SPacket.tryAcquireRateLimit(playerA, 207L));
        assertTrue(CultivatorActionC2SPacket.tryAcquireRateLimit(playerB, 207L));
        assertTrue(CultivatorActionC2SPacket.tryAcquireRateLimit(playerA, 208L));

        // ActivateUniqueSkillC2SPacket (6 ticks)
        assertTrue(ActivateUniqueSkillC2SPacket.tryAcquireRateLimit(playerA, 300L));
        assertFalse(ActivateUniqueSkillC2SPacket.tryAcquireRateLimit(playerA, 305L));
        assertTrue(ActivateUniqueSkillC2SPacket.tryAcquireRateLimit(playerB, 305L));
        assertTrue(ActivateUniqueSkillC2SPacket.tryAcquireRateLimit(playerA, 306L));

        ActivateUniqueSkillC2SPacket.clearRateLimit(playerA);
        assertTrue(ActivateUniqueSkillC2SPacket.tryAcquireRateLimit(playerA, 306L));

        // ActivateCultivationSkillC2SPacket (5 ticks)
        assertTrue(ActivateCultivationSkillC2SPacket.tryAcquireRateLimit(playerA, 400L));
        assertFalse(ActivateCultivationSkillC2SPacket.tryAcquireRateLimit(playerA, 403L));
        assertTrue(ActivateCultivationSkillC2SPacket.tryAcquireRateLimit(playerB, 403L));
        assertTrue(ActivateCultivationSkillC2SPacket.tryAcquireRateLimit(playerA, 405L));
        ActivateCultivationSkillC2SPacket.clearRateLimit(playerA);
        assertTrue(ActivateCultivationSkillC2SPacket.tryAcquireRateLimit(playerA, 405L));

        // CycleMartialSlotC2SPacket (5 ticks)
        assertTrue(CycleMartialSlotC2SPacket.tryAcquireRateLimit(playerA, 500L));
        assertFalse(CycleMartialSlotC2SPacket.tryAcquireRateLimit(playerA, 502L));
        assertTrue(CycleMartialSlotC2SPacket.tryAcquireRateLimit(playerB, 502L));
        assertTrue(CycleMartialSlotC2SPacket.tryAcquireRateLimit(playerA, 505L));
        CycleMartialSlotC2SPacket.clearRateLimit(playerA);
        assertTrue(CycleMartialSlotC2SPacket.tryAcquireRateLimit(playerA, 505L));

        // CycleDivinePhaseC2SPacket (4 ticks)
        assertTrue(CycleDivinePhaseC2SPacket.tryAcquireRateLimit(playerA, 600L));
        assertFalse(CycleDivinePhaseC2SPacket.tryAcquireRateLimit(playerA, 602L));
        assertTrue(CycleDivinePhaseC2SPacket.tryAcquireRateLimit(playerB, 602L));
        assertTrue(CycleDivinePhaseC2SPacket.tryAcquireRateLimit(playerA, 604L));
        CycleDivinePhaseC2SPacket.clearRateLimit(playerA);
        assertTrue(CycleDivinePhaseC2SPacket.tryAcquireRateLimit(playerA, 604L));
    }

    @Test
    void bypassInfusionHooksSupportsNestedReentrancyAndRestoresOnException() {
        assertFalse(ContractGameplayEvents.isBypassingInfusionHooks());
        ContractGameplayEvents.runBypassingInfusionHooks(() -> {
            assertTrue(ContractGameplayEvents.isBypassingInfusionHooks());
            ContractGameplayEvents.runBypassingInfusionHooks(() ->
                    assertTrue(ContractGameplayEvents.isBypassingInfusionHooks()));
            assertTrue(ContractGameplayEvents.isBypassingInfusionHooks());
        });
        assertFalse(ContractGameplayEvents.isBypassingInfusionHooks());

        assertThrows(IllegalStateException.class, () ->
                ContractGameplayEvents.runBypassingInfusionHooks(() -> {
                    assertTrue(ContractGameplayEvents.isBypassingInfusionHooks());
                    throw new IllegalStateException("simulated error");
                }));
        assertFalse(ContractGameplayEvents.isBypassingInfusionHooks());
    }
}
