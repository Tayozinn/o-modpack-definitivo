# Schemas de Dados & Datapacks — Mythic Contracts 2.0

## 1. Schema NBT do Jogador (`DataVersion = 3`)

Migração automática encadeada no `PlayerDataMigrator`:
- **v1** (sem `DataVersion`): convertido por `migrateV1ToV2` e depois `migrateV2ToV3`.
- **v2** (`DataVersion = 2`): convertido por `migrateV2ToV3`, preservando 100% dos dados existentes e preenchendo Integridade Espiritual e Vigor no máximo calculado pelo cultivo do save.

```json
{
  "DataVersion": 3,
  "Cultivation": {
    "BodyRealmId": "mythic_contracts:body_realm_1",
    "BodyRealm": 1,
    "BodyStage": 1,
    "SpiritRealmId": "mythic_contracts:spirit_realm_1",
    "SpiritRealm": 1,
    "SpiritStage": 1,
    "MaxQiCapacity": 100.0,
    "CurrentQi": 100.0,
    "ManaInfusionActive": false,
    "MaxSpiritualIntegrity": 100.0,
    "CurrentSpiritualIntegrity": 100.0,
    "SpiritShatterTicks": 0,
    "MaxStamina": 100.0,
    "CurrentStamina": 100.0,
    "StaminaRegenDelayTicks": 0
  },
  "Faction": { "CurrentFactionId": "mythic_contracts:none", "Reputations": {}, "Relationships": {} },
  "Contracts": { "Active": [], "BrokenHistory": [], "VowBroken": false },
  "Skills": {
    "Unlocked": [],
    "Equipped": ["mythic_contracts:tezcatlipoca_step"],
    "Cooldowns": {},
    "MaxDivineResistance": 100.0,
    "CurrentDivineResistance": 100.0,
    "DivineOverloadTicks": 0
  },
  "Subjugation": { "MaxCapacityBonus": 0, "BoundEntities": [] }
}
```

## 2. Datapack: Reinos (`data/<ns>/realms/{body,spirit}/*.json`)
- Suporta `type` (`"body"` | `"spirit"`), `order` (`>= 1`), `display_name`, `qi_base`, `qi_regen_bonus`, `infusion_drain_per_second`, `infusion_drain_per_hit`, `requirements` (`previous_realm`, `tribulation`, `min_opposite_realm_order`, `qi_cost`) e `stages` (lista de strings ou objetos `{id, display_name, qi_bonus, infusion_multiplier, infusion_flat_bonus, attributes}`).
- Nos 7 reinos corporais base (`body_realm_1..7.json`), cada estágio define `minecraft:generic.attack_damage` (UUID `a7f31c90-4b2e-4d8a-9c15-778899aabbcc`) e `minecraft:generic.max_health` (`amount = max(0, 2*R*(R+S-1) - 2)`, de `0.0` até `+124.0` HP, UUID `b8e42d01-5c3f-4e9b-8d26-889900bbccdd`).

## 3. Datapack: Tags de Dano (`data/mythic_contracts/tags/damage_type/`)
- `spiritual_damage.json`: `minecraft:wither`, `minecraft:wither_skull`, `minecraft:sonic_boom`.
- `arcane_spiritual_damage.json`: `#minecraft:witch_resistant_to`, `minecraft:magic`, `minecraft:indirect_magic` + escolas opcionais `irons_spellbooks:*` (`"required": false`).

## 4. Datapack: Contratos v2 (`data/<ns>/contracts/*.json`)
- **Campos v1:** `display_name`, `description`, `allowed_factions`, `forbidden_factions`, `attribute_modifiers`, `periodic_absorption_hearts`, `periodic_regen_amplifier`, `break_on_attack_villager`.
- **Campos v2 (Fase 8):** `min_body_realm` (padrão `1`), `min_spirit_realm` (padrão `1`), `min_reputation` (padrão `0`), `reputation_reward` (padrão `0`), `qi_regen_bonus` (padrão `0.0`), `spiritual_integrity_bonus` (padrão `0.0`), `taboo_summary` (padrão `""`).
- Novo contrato v2 embutido: `mythic_contracts:veil_resonance_oath` (`veil_resonance_oath.json`) exigindo Espírito `R2+` e concedendo `+2.0 Qi/s`, `+25.0` Integridade Espiritual Máxima e `+15` de reputação na primeira assinatura.

## 5. Datapack: Facções (`data/<ns>/factions/*.json` — Fase 8)
- `FactionDefinition` / `FactionReloadListener` / `FactionRegistry` com 5 fallbacks embutidos (`none`, `errantes_do_veu`, `juramentados_da_luz`, `irmandade_divida_eterna`, `quebradores_de_tronos`).
- **Campos:** `id` (opcional, default pelo caminho do arquivo), `display_name`, `tagline`, `accent_color` (`#RRGGBB` / `0xAARRGGBB` / inteiro), `subjugation_threshold` (`0.05`–`1.0`), `bonus_summary`, `restriction_summary`, `passive_modifiers` (`attribute`, `operation`, `amount`, `uuid` opcional), `default_relationships`.

## 6. Datapack: Resquícios Divinos (`data/<ns>/skills/*.json` — Fase 8)
- `DivineSkillDefinition` / `DivineSkillReloadListener` / `DivineSkillRegistry` para os 4 Resquícios (`eye_of_ra`, `morrigan_reap`, `indras_vajra`, `tezcatlipoca_step`).
- **Campos:** `id` (opcional), `display_name`, `deity_origin`, `hud_color`, `archetype`, `mechanic_summary`, `base_damage`, `primary_realm_scale`, `secondary_scale`, `base_radius` (`1.0`–`32.0`), `cost_multiplier` (`0.1`–`5.0`), `cooldown_multiplier` (`0.1`–`5.0`), `boss_damage_bonus_multiplier` (`0.5`–`4.0`), `min_body_realm`, `min_spirit_realm`, `enabled`.

## 7. Validação Cruzada de Datapacks (`DatapackValidationService` — Fase 8)
- Executado automaticamente em `ServerStartedEvent` e `OnDatapackSyncEvent` (`player == null`) e sob demanda via `/contrato validar` (`/core validar`).
- Verifica ordens duplicadas e `previous_realm` em Reinos, `subjugation_threshold` e `default_relationships` em Facções, interseção `allowed_factions`/`forbidden_factions` e reinos máximos em Contratos v2, e limites numéricos/reinos em Resquícios Divinos.

## 8. Config de Cliente (`config/mythic_contracts-client.toml` — Fase 5)
- `ForgeConfigSpec` (`ModConfig.Type.CLIENT`): posição/âncora/escala/opacidade independentes para o **Painel Vital** e o **Emblema Circular de Resistência Divina**, toggles de visibilidade, animações, `render3DPlayerInGui`, `visualQualityPreset` (`LOW`/`MEDIUM`/`HIGH`).
