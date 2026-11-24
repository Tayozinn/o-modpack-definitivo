package com.mythiccontracts.cultivation.realm;

import java.util.Locale;
import java.util.Optional;

/**
 * Linhas independentes de cultivo suportadas pelo motor de progressão.
 */
public enum CultivationPath {
    BODY("body", "Corpo"),
    SPIRIT("spirit", "Espírito");

    private final String id;
    private final String displayName;

    CultivationPath(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static Optional<CultivationPath> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "body", "corpo" -> Optional.of(BODY);
            case "spirit", "espirito", "espírito" -> Optional.of(SPIRIT);
            default -> Optional.empty();
        };
    }
}
