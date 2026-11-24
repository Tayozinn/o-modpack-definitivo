package com.mythiccontracts.client.config;

/**
 * Presets de qualidade visual para efeitos e densidade de VFX/HUD (compatível com Embeddium sem shaders).
 */
public enum VisualQualityPreset {
    LOW("config.mythic_contracts.quality.low", "Baixa"),
    MEDIUM("config.mythic_contracts.quality.medium", "Média"),
    HIGH("config.mythic_contracts.quality.high", "Alta");

    private final String translationKey;
    private final String defaultLabel;

    VisualQualityPreset(String translationKey, String defaultLabel) {
        this.translationKey = translationKey;
        this.defaultLabel = defaultLabel;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getDefaultLabel() {
        return this.defaultLabel;
    }

    public VisualQualityPreset next() {
        VisualQualityPreset[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
