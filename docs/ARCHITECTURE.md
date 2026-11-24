# Arquitetura Modular — Mythic Contracts 2.0.0

- **Target:** Minecraft `1.20.1` | Forge `47.3.0` (`[47.2.0,)`) | Java `17` | Mojang Official Mappings
- **Formato:** Monólito modular em único JAR distribuível (`mods/mythic_contracts-2.0.0.jar`)

---

## 1. Visão Geral de Camadas

```
+-----------------------------------------------------------------------+
|  CLIENT LAYER (Dist.CLIENT — Zero vazamento para servidor dedicado)   |
|  client/hud · client/screen · client/config · client/fx · client/sound |
+-----------------------------------▲-----------------------------------+
                                    │ S2C (SyncCoreData / SyncEnergy / SpawnVisualEffect)
                                    │ C2S (ToggleInfusion / UniqueSkill / CultivationSkill / CycleSlot / CyclePhase / Action)
+-----------------------------------┴-----------------------------------+
|  NETWORK & GAMEPLAY BOUNDARY                                          |
|  network/ModNetworking (Protocolo "7", rate-limits C2S por UUID)      |
|  event/ContractGameplayEvents · event/SubjugationEvents · command     |
+-----------------------------------▲-----------------------------------+
                                    │
+-----------------------------------┴-----------------------------------+
|  DOMAIN, PROGRESSION & INTEGRATION SERVICES (Autoridade do Servidor)  |
|  CultivationService · CultivationActionService · AttributeService     |
|  EnergyService · VitalService · SpiritualIntegrityService             |
|  ContractManager · UniqueSkillHandler · CultivationSkillService       |
|  SubjugationService · integration/CombatCompat                        |
+-----------------------------------▲-----------------------------------+
                                    │
+-----------------------------------┴-----------------------------------+
|  DATA, PERSISTENCE & VALIDATION LAYER                                 |
|  core/player (PlayerData) · core/persistence (v1 -> v2 -> v3 -> v4)   |
|  RealmRegistry · FactionRegistry · DivineSkillRegistry · Contracts    |
|  CultivationSkillRegistry · SpiritualIntegrityBalanceConfig           |
|  DatapackValidationService · ModAdvancements · MythicContractsAPI     |
+-----------------------------------------------------------------------+
```

---

## 2. Módulos e Responsabilidades (Atualizações 2.1 — Sem quebra de `version = '2.0.0'`)

| Pacote | Responsabilidade |
| :--- | :--- |
| `com.mythiccontracts.core.player` | Estado puro do jogador (`PlayerData`: `CultivationData`, `FactionData`, `ContractData`, `SkillData` c/ slots `S1–S3`, `activeDivinePhase` `H1–H4` e Grau I–IV, `SubjugationData` c/ `ServantStance` e `summoned`). |
| `com.mythiccontracts.core.persistence` | Versionamento NBT (`PlayerDataSchema`, `CURRENT_VERSION = 4`), migração encadeada (`PlayerDataMigrator`: `v1 -> v2 -> v3 -> v4`) e serialização segura (`writeFull` vs `writeClientSync`). |
| `com.mythiccontracts.core.service` | `EnergyService`, `VitalService` e `SpiritualIntegrityService` (3 estados contínuos `0.40x`/`1.00x`/`2.20x`/`0x`, curva quadrática R1–R7, teto `+50%` flat / `[0.25x, 2.25x]` pct e JSON `spiritual_integrity_balance.json`). |
| `com.mythiccontracts.core.data` | `DatapackJsonHelper` e `DatapackValidationService` (validação cruzada em `ServerStartedEvent`, `OnDatapackSyncEvent` e `/contrato validar`). |
| `com.mythiccontracts.attribute` | `AttributeService` central: aplica e limpa `AttributeModifier`s por prefixo/UUID sem duplicatas e preserva razão `health/maxHealth` via `runPreservingHealthRatio`. |
| `com.mythiccontracts.cultivation.realm` | Definições data-driven de Reinos (`RealmDefinition`, `RealmStageDefinition`, `RealmRegistry`, `RealmReloadListener` — `14` JSONs R1–R7 Corpo/Espírito). |
| `com.mythiccontracts.cultivation.progression` | `CultivationService` e `CultivationActionService`: validação server-side, consumo real de Pílulas/Tomos e quebras de reino com Tribulação. |
| `com.mythiccontracts.faction` | `FactionDefinition`, `FactionRegistry` e `FactionReloadListener` (`5` JSONs data-driven em `data/<ns>/factions/`). |
| `com.mythiccontracts.skill.definition` | `DivineSkillDefinition`, `DivineSkillRegistry` e `DivineSkillReloadListener` (`4` JSONs data-driven em `data/<ns>/skills/`). |
| `com.mythiccontracts.skill.remnant` | `DivineRemnant` (4 Deuses Caídos independentes de facção), `DivinePhaseAbility` (`16` habilidades em 4 Fases `H1–H4` × 4 Deuses) e `DivineResonanceBalance` (Fases I–IV c/ contenção `0.40x–1.45x`, piso bilateral anti-rush, custo `60→20`, Ultimate `65%` e Sobrecarga 60s). |
| `com.mythiccontracts.skill.cultivation` | 12 Técnicas de Cultivo data-driven (`data/<ns>/cultivation_skills/*.json`), desbloqueio progressivo R1–R7 e 3 Slots Marciais (`S1`/`S2`/`S3` em `R1`/`R3`/`R5`). |
| `com.mythiccontracts.contract` | `ContractDefinition`, `ContractDataReloadListener` e `ContractManager` com elegibilidade v2 (reino/reputação), bônus vitais/regen espiritual e tabus de quebra. |
| `com.mythiccontracts.subjugation` / `entity` | `SubjugationService`, `ActiveSubjugationSession` (Disputa Espiritual autoritativa c/ Dominância `0–100%` e Pulso de Revolta), `SubjugationTier` (`COMMON..BOSS`), Graus `I–IV` e `SubjugationSealItem` como Catalisador (`-50%` custos, `+10%` HP, `65%` início). |
| `com.mythiccontracts.integration` | Soft-dependencies (`ModCompatRegistry`), normalização do Triângulo de Combate (`CombatCompatService`), `ModAdvancements` (`7` avanços) e fachada KubeJS (`MythicContractsAPI`). |
| `com.mythiccontracts.network` | Protocolo `"7"` autoritativo (`ModNetworking`), rate-limits C2S atômicos por UUID (`10t`/`8t`/`6t`/`5t`/`4t`) e payloads compactos client-safe. |
| `com.mythiccontracts.command` | `/contrato` (alias `/core`): comandos de leitura abertos + `7` subcomandos administrativos protegidos por OP `hasPermission(2)`. |
| `com.mythiccontracts.client` | HUD dual (emblema `[G]` com 4 arcos externos segmentados das Fases I–IV e pill `H1–H4`) + barra efêmera `6px` de Disputa, `CultivatorScreen` (`[U]` Master-Detail + grade `2×2` `H1–H4` e Painel de Servos), `MythicParticleBase` (`MotionMode`), `ClientRibbonRenderer` (`AFTER_PARTICLES`), `ClientSkillAnimationController` + `PlayerAnimBridge` (reflexão `playerAnimator`) e `ModSounds`/`ClientSoundController` (`30` `.ogg` mono `44.1 kHz`). |
