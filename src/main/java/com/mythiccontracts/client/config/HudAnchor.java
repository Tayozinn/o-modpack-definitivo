package com.mythiccontracts.client.config;

/**
 * Pontos de ancoragem configuráveis para os widgets da HUD na tela.
 * Não possui dependências de classes cliente para manter compatibilidade segura no servidor dedicado.
 */
public enum HudAnchor {
    TOP_LEFT("config.mythic_contracts.anchor.top_left", "Topo Esquerdo"),
    TOP_RIGHT("config.mythic_contracts.anchor.top_right", "Topo Direito"),
    BOTTOM_LEFT("config.mythic_contracts.anchor.bottom_left", "Base Esquerda"),
    BOTTOM_RIGHT("config.mythic_contracts.anchor.bottom_right", "Base Direita");

    private final String translationKey;
    private final String defaultLabel;

    HudAnchor(String translationKey, String defaultLabel) {
        this.translationKey = translationKey;
        this.defaultLabel = defaultLabel;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getDefaultLabel() {
        return this.defaultLabel;
    }

    public HudAnchor next() {
        HudAnchor[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
