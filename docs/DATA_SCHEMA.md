# Schemas de Dados & Datapacks — Mythic Contracts 2.0

## 1. Schema NBT do Jogador (`DataVersion = 4`)

Migração automática encadeada no `PlayerDataMigrator`:
- **v1** (sem `DataVersion`): convertido por `migrateV1ToV2` → `migrateV2ToV3` → `migrateV3ToV4`.
- **v2** (`DataVersion = 2`): convertido por `migrateV2ToV3` → `migrateV3ToV4`, preenchendo Integridade Espiritual e Vigor no máximo calculado pelo cultivo.
- **v3** (`DataVersion = 3`): convertido por `migrateV3ToV4`, preservando 100% dos dados vitais e inicializando `ActiveDivinePhase = 0`, `ActiveMartialSlot = 0`, `SubjugationMasteryTier = 1` e campos de meditação/combate espiritual.

```json
{
  "DataVersion": 4,
  "Cultivation": {
    "BodyRealmId": "mythic_contracts:body_realm_1",
    "SpiritRealmId": "mythic_contracts:spirit_realm_1",
    "MaxQiCapacity": 100.0,
    "CurrentQi": 100.0,
    "ManaInfusionActive": false,
    "MaxSpiritualIntegrity": 100.0,
    "CurrentSpiritualIntegrity": 100.0,
    "SpiritShatterTicks": 0,
    "SpiritCombatCooldownTicks": 0,
    "StillMeditationTicks": 0,
    "MeditationBoostTicks": 0,
    "MaxStamina": 100.0,
    "CurrentStamina": 100.0,
    "StaminaRegenDelayTicks": 0
  },
  "Faction": { "CurrentFactionId": "mythic_contracts:none", "Reputations": {}, "Relationships": {} },
  "Contracts": { "Active": [], "BrokenHistory": [] },
  "Skills": {
    "Unlocked": [],
    "Equipped": ["mythic_contracts:tezcatlipoca_step"],
    "EquippedMartial": [],
    "ActiveMartialSlot": 0,
    "ActiveDivinePhase": 0,
    "SubjugationMasteryTier": 1,
    "Cooldowns": {},
    "MaxDivineResistance": 100.0,
    "CurrentDivineResistance": 100.0,
    "DivineOverloadTicks": 0
  },
  "Subjugation": {
    "MaxCapacityBonus": 0,
    "BoundEntities": [
      {
        "InstanceId": "00000000-0000-0000-0000-000000000000",
        "EntityType": "minecraft:ravager",
        "Level": 1,
        "Experience": 0,
        "CustomName": "",
        "Snapshot": {},
        "Stance": "FOLLOW",
        "Summoned": false
      }
    ]
  }
}
```

> **Payloads de Sincronização S2C:** `PlayerDataSerializer.writeClientSync` omite `SignedAtTick`, `ViolationCount`, `BrokenHistory` e `Snapshot` dos servos para minimizar tráfego e evitar exposição de dados privados no cliente. `SyncEnergyS2CPacket` (`PROTOCOL_VERSION = "7"`) sincroniza recursos contínuos, `spiritRegenStateOrdinal`, `activeMartialSlot`, estado efêmero da Disputa de Subjugação e `activeDivinePhase`.

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

## 6. Datapack: Resquícios Divinos & Fases (`data/<ns>/skills/*.json` + `DivinePhaseAbility`)
- `DivineSkillDefinition` / `DivineSkillReloadListener` / `DivineSkillRegistry` para os 4 Resquícios (`eye_of_ra`, `morrigan_reap`, `indras_vajra`, `tezcatlipoca_step`).
- **Campos:** `id` (opcional), `display_name`, `deity_origin`, `hud_color`, `archetype`, `mechanic_summary`, `base_damage`, `primary_realm_scale`, `secondary_scale`, `base_radius` (`1.0`–`32.0`), `cost_multiplier` (`0.1`–`5.0`), `cooldown_multiplier` (`0.1`–`5.0`), `boss_damage_bonus_multiplier` (`0.5`–`4.0`), `min_body_realm`, `min_spirit_realm`, `enabled`.
- **Progressão em 4 Fases (`DivineResonanceBalance` + `DivinePhaseAbility`):** cada Resquício possui 4 habilidades (`H1–H4`, índice `0–3`), com desbloqueio por pontuação combinada + piso mínimo bilateral (`0.40x`, `0.75x`, `1.10x`, `1.45x`), multiplicadores de custo `1.00x / 1.15x / 1.35x` e `65%` da barra máxima em `H4`, além de multiplicadores de recarga específicos por habilidade (`cooldownMultiplier`).

## 7. Datapack: Equilíbrio da Integridade Espiritual (`data/<ns>/cultivation/spiritual_integrity_balance.json`)
- **Campos configuráveis:** `combat_cooldown_ticks` (`100`), `combat_mult` (`0.40`), `out_of_combat_mult` (`1.00`), `meditation_mult` (`2.20`), `meditation_still_ticks` (`40`), `base_regen_r1` (`3.10`), `linear_realm_scale` (`2.20`), `quadratic_realm_scale` (`0.18`), `stage_scale` (`0.65`), `max_flat_bonus_ratio` (`0.50`), `min_pct_mult` (`0.25`), `max_pct_bonus_cap` (`2.25`), `interrupt_on_combat_damage` (`true`), `enable_meditation_bonus` (`true`).

## 8. Datapack: Técnicas de Cultivo (`data/<ns>/cultivation_skills/*.json` — Atualização 2.1)
- `12` arquivos built-in (`blood_furnace_surge`, `ether_flash_strike`, `gale_meridian_step`, `golden_core_aegis`, `inner_breath_meditation`, `jade_bone_guard`, `nascent_soul_lance`, `primordial_heaven_sunder`, `seismic_palm`, `soul_subjugation`, `spirit_domain_suppression`, `undying_meridian_reversal`).
- **Campos:** `id`, `display_name`, `path_type` (`BODY` | `SPIRIT` | `HYBRID`), `category` (`MOBILITY` | `STRIKE` | `DEFENSE` | `DOMAIN`), `tier_realm` (`1–7`), `hud_color`, `mechanic_summary`, `qi_cost`, `stamina_cost`, `spiritual_integrity_cost`, `cooldown_ticks`, `usable_during_shatter`, `enabled` e bloco `unlock_requirements` (`min_body_realm`, `min_body_stage`, `min_spirit_realm`, `min_spirit_stage`, `prerequisite_skill`, `require_active_contract`, `realm_bypass_factions`, `qi_unlock_cost`).

## 9. Validação Cruzada de Datapacks (`DatapackValidationService`)
- Executado automaticamente em `ServerStartedEvent` e `OnDatapackSyncEvent` (`player == null`) e sob demanda via `/contrato validar` (`/core validar`).
- Verifica ordens duplicadas e `previous_realm` em Reinos, `subjugation_threshold` e `default_relationships` em Facções, interseção `allowed_factions`/`forbidden_factions` e reinos máximos em Contratos v2, e limites numéricos/reinos em Resquícios Divinos e Técnicas de Cultivo.

## 10. Config de Cliente (`config/mythic_contracts-client.toml`)
- `ForgeConfigSpec` (`ModConfig.Type.CLIENT`): seções `[main_hud]`, `[divine_ring]` (posição/âncora/escala/opacidade independentes e `showNumbers`), e `[visual]` (`enableSmoothAnimations`, `render3DPlayerInGui`, `visualQualityPreset` `LOW`/`MEDIUM`/`HIGH`, `enableCustomSounds` e `skillSoundVolume` `0.0–1.5`).
