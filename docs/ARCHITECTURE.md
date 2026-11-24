# Arquitetura Modular — Mythic Contracts 2.0.0

- **Target:** Minecraft `1.20.1` | Forge `47.3.0` (`[47.2.0,)`) | Java `17` | Mojang Official Mappings
- **Formato:** Monólito modular em único JAR distribuível (`mods/mythic_contracts-2.0.0.jar`)

---

## 1. Visão Geral de Camadas

```
+-----------------------------------------------------------------------+
|  CLIENT LAYER (Dist.CLIENT — Zero vazamento para servidor dedicado)   |
|  client/hud · client/screen · client/config · client/fx · KeyEvents   |
+-----------------------------------▲-----------------------------------+
                                    │ S2C (SyncCoreData / SyncEnergy / SpawnVisualEffect)
                                    │ C2S (ToggleInfusion / ActivateUniqueSkill / CultivatorAction)
+-----------------------------------┴-----------------------------------+
|  NETWORK & GAMEPLAY BOUNDARY                                          |
|  network/ModNetworking (Protocolo "5", rate-limits C2S por UUID)      |
|  event/ContractGameplayEvents · event/SubjugationEvents · command     |
+-----------------------------------▲-----------------------------------+
                                    │
+-----------------------------------┴-----------------------------------+
|  DOMAIN, PROGRESSION & INTEGRATION SERVICES (Autoridade do Servidor)  |
|  CultivationService · CultivationActionService · AttributeService     |
|  EnergyService · VitalService · SpiritualIntegrityService             |
|  ContractManager · UniqueSkillHandler · integration/CombatCompat      |
+-----------------------------------▲-----------------------------------+
                                    │
+-----------------------------------┴-----------------------------------+
|  DATA, PERSISTENCE & VALIDATION LAYER                                 |
|  core/player (PlayerData) · core/persistence (v1 -> v2 -> v3)         |
|  RealmRegistry · FactionRegistry · DivineSkillRegistry · Contracts    |
|  DatapackValidationService · ModAdvancements · MythicContractsAPI     |
+-----------------------------------------------------------------------+
```

---

## 2. Módulos e Responsabilidades (`140` classes compiladas no JAR)

| Pacote | Responsabilidade |
| :--- | :--- |
| `com.mythiccontracts.core.player` | Estado puro do jogador (`PlayerData` agregado em `CultivationData`, `FactionData`, `ContractData`, `SkillData`, `SubjugationData`) com sanitização contra `NaN`/`±Infinity`. |
| `com.mythiccontracts.core.persistence` | Versionamento NBT (`PlayerDataSchema`, `CURRENT_VERSION = 3`), migração encadeada (`PlayerDataMigrator`: `v1 -> v2 -> v3`) e serialização segura (`writeFull` vs `writeClientSync`). |
| `com.mythiccontracts.core.service` | Serviços vitais e energéticos (`EnergyService`, `VitalService`, `SpiritualIntegrityService`) com tick otimizado sem recálculos redundantes. |
| `com.mythiccontracts.core.data` | `DatapackJsonHelper` e `DatapackValidationService` (validação cruzada em `ServerStartedEvent`, `OnDatapackSyncEvent` e `/contrato validar`). |
| `com.mythiccontracts.attribute` | `AttributeService` central: aplica e limpa `AttributeModifier`s por prefixo/UUID sem duplicatas e preserva razão `health/maxHealth` via `runPreservingHealthRatio`. |
| `com.mythiccontracts.cultivation.realm` | Definições data-driven de Reinos (`RealmDefinition`, `RealmStageDefinition`, `RealmRegistry`, `RealmReloadListener` — `14` JSONs R1–R7 Corpo/Espírito). |
| `com.mythiccontracts.cultivation.progression` | `CultivationService` e `CultivationActionService`: validação server-side, consumo real de Pílulas/Tomos e quebras de reino com Tribulação. |
| `com.mythiccontracts.faction` | `FactionDefinition`, `FactionRegistry` e `FactionReloadListener` (`5` JSONs data-driven em `data/<ns>/factions/`). |
| `com.mythiccontracts.skill.definition` | `DivineSkillDefinition`, `DivineSkillRegistry` e `DivineSkillReloadListener` (`4` JSONs data-driven em `data/<ns>/skills/`). |
| `com.mythiccontracts.skill.remnant` | `DivineRemnant` (4 Deuses Caídos independentes de facção) e `DivineResonanceBalance` (Camadas I–IV, Resistência Divina, Sobrecarga 60s). |
| `com.mythiccontracts.contract` | `ContractDefinition`, `ContractDataReloadListener` e `ContractManager` com elegibilidade v2 (reino/reputação), bônus vitais e tabus de quebra. |
| `com.mythiccontracts.subjugation` / `entity` | Subjugação de entidades (`SubjugationSealItem`, `SubjugationEvents`, AI Goals com proteção contra fogo amigo e lookup otimizado no tick). |
| `com.mythiccontracts.integration` | Soft-dependencies (`ModCompatRegistry`), normalização do Triângulo de Combate (`CombatCompatService`), `ModAdvancements` (`7` avanços) e fachada KubeJS (`MythicContractsAPI`). |
| `com.mythiccontracts.network` | Protocolo `"5"` autoritativo (`ModNetworking`), rate-limits C2S atômicos por UUID (`10t`/`8t`/`6t`) e payloads compactos client-safe. |
| `com.mythiccontracts.command` | `/contrato` (alias `/core`): comandos de leitura abertos + `7` subcomandos administrativos protegidos por OP `hasPermission(2)`. |
| `com.mythiccontracts.client` | HUD dual (Painel Minimalista Murim `[F3-safe]` + Anel Radial `34 px` `[G]`), `CultivatorScreen` (`[U]`), `HudConfigScreen`, `ClientHudConfig` e motor VFX `MythicParticleBase` sem shaders. |
