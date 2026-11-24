package com.mythiccontracts.skill.cultivation;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public enum CultivationSkillCategory {
    OFFENSIVE("Ofensiva"),
    DEFENSIVE("Defensiva"),
    MOBILITY("Mobilidade"),
    SUPPORT("Suporte"),
    DOMAIN("Subjugação"),
    SUPREME("Suprema");

    private final String displayName;

    CultivationSkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static Optional<CultivationSkillCategory> parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT);
        for (CultivationSkillCategory val : values()) {
            if (val.name().equals(norm)) {
                return Optional.of(val);
            }
        }
        return Optional.empty();
    }
}
