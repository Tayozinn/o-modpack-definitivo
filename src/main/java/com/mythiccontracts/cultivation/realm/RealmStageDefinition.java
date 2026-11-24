package com.mythiccontracts.cultivation.realm;

import java.util.Collections;
import java.util.List;

/**
 * Definição de um estágio dentro de um reino de cultivo (ex: base, fortificação, despertar, perfeição).
 */
public record RealmStageDefinition(
        String id,
        String displayName,
        float qiBonus,
        float infusionMultiplier,
        float infusionFlatBonus,
        List<RealmAttributeBonus> attributes
) {
    public RealmStageDefinition {
        id = (id != null && !id.isBlank()) ? id : "base";
        displayName = (displayName != null && !displayName.isBlank()) ? displayName : id;
        attributes = attributes != null ? Collections.unmodifiableList(attributes) : List.of();
    }
}
