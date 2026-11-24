package com.mythiccontracts.client.screen;

/**
 * As 7 abas horizontais inferiores da Interface do Cultivador ([U]),
 * preservando a ordem e nomenclatura aprovadas para o Conceito B/C híbrido.
 */
public enum CultivatorTab {
    OVERVIEW("screen.mythic_contracts.cultivator.tab.overview", "Visão Geral"),
    BODY("screen.mythic_contracts.cultivator.tab.body", "Corpo"),
    SPIRIT("screen.mythic_contracts.cultivator.tab.spirit", "Espírito"),
    ATTRIBUTES("screen.mythic_contracts.cultivator.tab.attributes", "Atributos"),
    SKILLS("screen.mythic_contracts.cultivator.tab.skills", "Habilidades"),
    CONTRACTS("screen.mythic_contracts.cultivator.tab.contracts", "Contratos"),
    FACTION("screen.mythic_contracts.cultivator.tab.faction", "Facção");

    private final String translationKey;
    private final String defaultLabel;

    CultivatorTab(String translationKey, String defaultLabel) {
        this.translationKey = translationKey;
        this.defaultLabel = defaultLabel;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getDefaultLabel() {
        return this.defaultLabel;
    }

    public CultivatorTab next() {
        CultivatorTab[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public CultivatorTab previous() {
        CultivatorTab[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static CultivatorTab byIndex(int index) {
        CultivatorTab[] values = values();
        if (index < 0 || index >= values.length) {
            return OVERVIEW;
        }
        return values[index];
    }
}
