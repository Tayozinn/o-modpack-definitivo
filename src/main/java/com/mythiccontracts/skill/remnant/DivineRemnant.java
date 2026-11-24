package com.mythiccontracts.skill.remnant;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

/**
 * Resquícios do poder de Deuses mitológicos caídos na Guerra Pós-Colapso.
 * São independentes de facção e equipáveis no slot único [G].
 */
public enum DivineRemnant {
    EYE_OF_RA(
            new ResourceLocation(MythicContractsMod.MOD_ID, "eye_of_ra"),
            "Olho de Rá",
            "Rá (Egípcia)",
            0xFFFBBF24,
            "solar_judgment"
    ),
    MORRIGAN_REAP(
            new ResourceLocation(MythicContractsMod.MOD_ID, "morrigan_reap"),
            "Ceifa da Morrígan",
            "The Morrígan (Celta)",
            0xFFEF4444,
            "abyssal_harvest"
    ),
    INDRAS_VAJRA(
            new ResourceLocation(MythicContractsMod.MOD_ID, "indras_vajra"),
            "Vajra de Indra",
            "Indra (Védica)",
            0xFF38BDF8,
            "sovereign_pulse"
    ),
    TEZCATLIPOCA_STEP(
            new ResourceLocation(MythicContractsMod.MOD_ID, "tezcatlipoca_step"),
            "Passo de Tezcatlipoca",
            "Tezcatlipoca (Asteca)",
            0xFF22D3EE,
            "veil_step"
    );

    private final ResourceLocation id;
    private final String displayName;
    private final String deityOrigin;
    private final int hudColor;
    private final String legacyAlias;

    DivineRemnant(ResourceLocation id, String displayName, String deityOrigin, int hudColor, String legacyAlias) {
        this.id = id;
        this.displayName = displayName;
        this.deityOrigin = deityOrigin;
        this.hudColor = hudColor;
        this.legacyAlias = legacyAlias;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDeityOrigin() {
        return this.deityOrigin;
    }

    public int getHudColor() {
        return this.hudColor;
    }

    public String getLegacyAlias() {
        return this.legacyAlias;
    }

    public static Optional<DivineRemnant> parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawInput.trim().toLowerCase(Locale.ROOT);
        for (DivineRemnant remnant : values()) {
            if (remnant.id.toString().equals(normalized)
                    || remnant.id.getPath().equals(normalized)
                    || remnant.legacyAlias.equals(normalized)
                    || (MythicContractsMod.MOD_ID + ":" + remnant.legacyAlias).equals(normalized)
                    || remnant.name().equalsIgnoreCase(normalized)) {
                return Optional.of(remnant);
            }
        }
        return Optional.empty();
    }

    public static DivineRemnant fromSkillId(ResourceLocation id) {
        if (id == null) {
            return TEZCATLIPOCA_STEP;
        }
        return parse(id.toString()).orElse(TEZCATLIPOCA_STEP);
    }
}
