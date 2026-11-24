package com.mythiccontracts.core.player;

import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Armazena habilidades desbloqueadas, equipadas, recargas (cooldowns)
 * e o estado da barra exclusiva de Resistência Divina dos Resquícios.
 */
public class SkillData {

    public static final ResourceLocation DEFAULT_UNIQUE_SKILL_ID =
            new ResourceLocation("mythic_contracts", "unique_skill");
    public static final float DEFAULT_MAX_DIVINE_RESISTANCE = 100.0F;
    public static final int MAX_MARTIAL_SLOTS = 3;

    private final Set<ResourceLocation> unlockedSkills = new LinkedHashSet<>();
    private final List<ResourceLocation> equippedSkills = new ArrayList<>();
    private final ResourceLocation[] equippedMartialSkills = new ResourceLocation[MAX_MARTIAL_SLOTS];
    private int activeMartialSlot = 0;
    private int subjugationMasteryTier = 1;
    private final Map<ResourceLocation, Integer> cooldowns = new LinkedHashMap<>();

    private float maxDivineResistance = DEFAULT_MAX_DIVINE_RESISTANCE;
    private float currentDivineResistance = DEFAULT_MAX_DIVINE_RESISTANCE;
    private int divineOverloadTicks = 0;
    private int activeDivinePhase = 0;

    public Set<ResourceLocation> getUnlockedSkills() {
        return Collections.unmodifiableSet(this.unlockedSkills);
    }

    public boolean unlockSkill(ResourceLocation skillId) {
        return skillId != null && this.unlockedSkills.add(skillId);
    }

    public boolean hasSkillUnlocked(ResourceLocation skillId) {
        return skillId != null && this.unlockedSkills.contains(skillId);
    }

    public List<ResourceLocation> getEquippedSkills() {
        return Collections.unmodifiableList(this.equippedSkills);
    }

    public void equipSkill(ResourceLocation skillId) {
        if (skillId != null && !this.equippedSkills.contains(skillId)) {
            this.equippedSkills.add(skillId);
        }
    }

    public boolean unequipSkill(ResourceLocation skillId) {
        return skillId != null && this.equippedSkills.remove(skillId);
    }

    public ResourceLocation getPrimaryEquippedSkill() {
        if (this.equippedSkills.isEmpty()) {
            return DivineRemnant.TEZCATLIPOCA_STEP.getId();
        }
        return this.equippedSkills.get(0);
    }

    public void setPrimaryEquippedSkill(ResourceLocation skillId) {
        if (skillId == null) {
            return;
        }
        this.unlockedSkills.add(skillId);
        this.equippedSkills.remove(skillId);
        this.equippedSkills.add(0, skillId);
    }

    public Map<ResourceLocation, Integer> getCooldowns() {
        return Collections.unmodifiableMap(this.cooldowns);
    }

    public int getCooldown(ResourceLocation skillId) {
        if (skillId == null) {
            return 0;
        }
        return Math.max(0, this.cooldowns.getOrDefault(skillId, 0));
    }

    public void setCooldown(ResourceLocation skillId, int ticks) {
        if (skillId == null) {
            return;
        }
        if (ticks <= 0) {
            this.cooldowns.remove(skillId);
        } else {
            this.cooldowns.put(skillId, ticks);
        }
    }

    public void tickAllCooldowns() {
        Iterator<Map.Entry<ResourceLocation, Integer>> it = this.cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, Integer> entry = it.next();
            int next = entry.getValue() - 1;
            if (next <= 0) {
                it.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    public int getUniqueSkillCooldownTicks() {
        return getCooldown(DEFAULT_UNIQUE_SKILL_ID);
    }

    public void setUniqueSkillCooldownTicks(int ticks) {
        setCooldown(DEFAULT_UNIQUE_SKILL_ID, ticks);
    }

    public float getMaxDivineResistance() {
        return Math.max(1.0F, this.maxDivineResistance);
    }

    public void setMaxDivineResistance(float maxDivineResistance) {
        this.maxDivineResistance = Math.max(1.0F, sanitize(maxDivineResistance, DEFAULT_MAX_DIVINE_RESISTANCE));
        this.currentDivineResistance = Mth.clamp(this.currentDivineResistance, 0.0F, this.maxDivineResistance);
    }

    public float getCurrentDivineResistance() {
        return Mth.clamp(sanitize(this.currentDivineResistance, 0.0F), 0.0F, getMaxDivineResistance());
    }

    public void setCurrentDivineResistance(float currentDivineResistance) {
        this.currentDivineResistance = Mth.clamp(sanitize(currentDivineResistance, 0.0F), 0.0F, getMaxDivineResistance());
    }

    private static float sanitize(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    public int getDivineOverloadTicks() {
        return Math.max(0, this.divineOverloadTicks);
    }

    public void setDivineOverloadTicks(int divineOverloadTicks) {
        this.divineOverloadTicks = Math.max(0, divineOverloadTicks);
    }

    public boolean isDivineOverloaded() {
        return this.divineOverloadTicks > 0;
    }

    public int getActiveDivinePhase() {
        return Mth.clamp(this.activeDivinePhase, 0, 3);
    }

    public void setActiveDivinePhase(int activeDivinePhase) {
        this.activeDivinePhase = Mth.clamp(activeDivinePhase, 0, 3);
    }

    @Nullable
    public ResourceLocation getMartialSkillInSlot(int slot) {
        if (slot < 0 || slot >= MAX_MARTIAL_SLOTS) {
            return null;
        }
        return this.equippedMartialSkills[slot];
    }

    public void setMartialSkillInSlot(int slot, @Nullable ResourceLocation skillId) {
        if (slot < 0 || slot >= MAX_MARTIAL_SLOTS) {
            return;
        }
        if (skillId != null) {
            for (int i = 0; i < MAX_MARTIAL_SLOTS; i++) {
                if (skillId.equals(this.equippedMartialSkills[i])) {
                    this.equippedMartialSkills[i] = null;
                }
            }
        }
        this.equippedMartialSkills[slot] = skillId;
    }

    @Nullable
    public ResourceLocation getActiveMartialSkill() {
        return getMartialSkillInSlot(getActiveMartialSlot());
    }

    public List<ResourceLocation> getEquippedMartialSkills() {
        List<ResourceLocation> nonEmpty = new ArrayList<>(MAX_MARTIAL_SLOTS);
        for (int i = 0; i < MAX_MARTIAL_SLOTS; i++) {
            if (this.equippedMartialSkills[i] != null) {
                nonEmpty.add(this.equippedMartialSkills[i]);
            }
        }
        return Collections.unmodifiableList(nonEmpty);
    }

    public boolean hasMartialSkillEquipped(@Nullable ResourceLocation skillId) {
        return findMartialSlot(skillId) >= 0;
    }

    public int findMartialSlot(@Nullable ResourceLocation skillId) {
        if (skillId == null) {
            return -1;
        }
        for (int i = 0; i < MAX_MARTIAL_SLOTS; i++) {
            if (skillId.equals(this.equippedMartialSkills[i])) {
                return i;
            }
        }
        return -1;
    }

    public int getActiveMartialSlot() {
        return Mth.clamp(this.activeMartialSlot, 0, MAX_MARTIAL_SLOTS - 1);
    }

    public void setActiveMartialSlot(int activeMartialSlot) {
        this.activeMartialSlot = Mth.clamp(activeMartialSlot, 0, MAX_MARTIAL_SLOTS - 1);
    }

    public int getSubjugationMasteryTier() {
        return Mth.clamp(this.subjugationMasteryTier, 1, 4);
    }

    public void setSubjugationMasteryTier(int subjugationMasteryTier) {
        this.subjugationMasteryTier = Mth.clamp(subjugationMasteryTier, 1, 4);
    }

    public void clearAll() {
        this.unlockedSkills.clear();
        this.equippedSkills.clear();
        Arrays.fill(this.equippedMartialSkills, null);
        this.activeMartialSlot = 0;
        this.subjugationMasteryTier = 1;
        this.cooldowns.clear();
        this.maxDivineResistance = DEFAULT_MAX_DIVINE_RESISTANCE;
        this.currentDivineResistance = DEFAULT_MAX_DIVINE_RESISTANCE;
        this.divineOverloadTicks = 0;
        this.activeDivinePhase = 0;
    }

    public void copyFrom(SkillData other, boolean wasDeath) {
        if (other == null) {
            return;
        }
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(other.unlockedSkills);

        this.equippedSkills.clear();
        this.equippedSkills.addAll(other.equippedSkills);

        System.arraycopy(other.equippedMartialSkills, 0, this.equippedMartialSkills, 0, MAX_MARTIAL_SLOTS);
        this.activeMartialSlot = other.getActiveMartialSlot();
        this.subjugationMasteryTier = other.getSubjugationMasteryTier();
        this.activeDivinePhase = other.getActiveDivinePhase();

        this.cooldowns.clear();
        this.maxDivineResistance = other.maxDivineResistance;

        if (wasDeath) {
            this.currentDivineResistance = getMaxDivineResistance();
            this.divineOverloadTicks = 0;
        } else {
            this.cooldowns.putAll(other.cooldowns);
            this.currentDivineResistance = Mth.clamp(other.currentDivineResistance, 0.0F, getMaxDivineResistance());
            this.divineOverloadTicks = other.divineOverloadTicks;
        }
    }
}
