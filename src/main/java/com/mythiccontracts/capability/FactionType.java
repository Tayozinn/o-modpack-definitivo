package com.mythiccontracts.capability;

import java.util.Locale;
import java.util.Optional;

public enum FactionType {
    NONE("none", "Sem Facção"),
    ERRANTES_DO_VEU("errantes_do_veu", "Errantes do Véu"),
    JURAMENTADOS_DA_LUZ("juramentados_da_luz", "Juramentados da Luz"),
    IRMANDADE_DIVIDA_ETERNA("irmandade_divida_eterna", "Irmandade da Dívida Eterna"),
    QUEBRADORES_DE_TRONOS("quebradores_de_tronos", "Quebradores de Tronos");

    private final String id;
    private final String displayName;

    FactionType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static FactionType fromString(String input) {
        return parse(input).orElse(NONE);
    }

    public static Optional<FactionType> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = input.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (FactionType type : values()) {
            if (type.id.equals(lower) || type.name().equalsIgnoreCase(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
