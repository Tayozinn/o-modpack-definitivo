package com.mythiccontracts.skill.cultivation;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public enum SkillPathType {
    BODY("Corpo", 0xFFF87171),
    SPIRIT("Espírito", 0xFFC084FC),
    HYBRID("Híbrida", 0xFF22D3EE);

    private final String displayName;
    private final int color;

    SkillPathType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getColor() {
        return this.color;
    }

    public static Optional<SkillPathType> parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT);
        for (SkillPathType val : values()) {
            if (val.name().equals(norm)) {
                return Optional.of(val);
            }
        }
        return Optional.empty();
    }
}
