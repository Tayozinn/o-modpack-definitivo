# Plano de Testes e Critérios de Qualidade — Mythic Contracts 2.0

Consolidado a partir de `docs/swarm/reports/T0_QA_RISKS_TESTPLAN.md`.

## 1. Matriz de Testes Obrigatórios

| Categoria | Escopo e Cenários | Critério de Aprovação |
| :--- | :--- | :--- |
| **Unitários (JUnit 5)** | Migração NBT `v1 -> v2 -> v3 -> v4`; `CultivationService` + bônus de `MAX_HEALTH`; `VitalService` (Vigor); `SpiritualIntegrityService` (3 estados contínuos, curva quadrática, Colapso 20s, recuperação 25%); `DivineResonanceBalance` + `DivinePhaseAbility` (4 Fases anti-rush `0.40x–1.45x`, `16` habilidades, Ultimate `65%`, Sobrecarga `60s`); `CultivationSkillSystem` (`12` técnicas) e `SubjugationSystemTest`. | 100% dos testes passando via `.\gradlew.bat clean test build --offline`. |
| **Atributos & Vida** | `AttributeService.runPreservingHealthRatio` no avanço de reino, login, mudança de dimensão, respawn e aplicação/remoção de `lucifer_pact`. | Zero truncamento de HP para `20.0`; zero modificadores duplicados ou órfãos. |
| **Persistência** | Morte (`wasDeath=true`), retorno do End (`wasDeath=false`), logout/login, recuperação via `PERSISTED_NBT_TAG`, NBT `v1`/`v2`/`v3` na `v4`, tags corrompidas (`ActiveDivinePhase`, `ActiveMartialSlot`, `ServantStance`, `Summoned`). | Zero perda de progresso; nenhum crash com NBT malformado. |
| **Multiplayer & C2S** | Isolamento por jogador; rate-limits atômicos (`ToggleInfusion` `10t`, `CultivatorAction` `8t`, `ActivateUniqueSkill` `6t`, `ActivateCultivationSkill` `5t`, `CycleMartialSlot` `5t`, `CycleDivinePhase` `4t`); `writeClientSync` ocultando dados privados; protocolo `"7"`. | Servidor rejeita payloads inválidos e flood C2S sem exceção. |
| **HUD, Áudio & `[U]`** | `DivineRingGeometryTest` (anel interno `10.6F..13.0F` + 4 arcos externos segmentados `13.7F..16.4F`), `AudioAndAssetIntegrityTest` (`30` `.ogg` mono `44.1 kHz` + `sounds.json`), `1280×720`–`2560×1440`, GUI Scale `1`–`4` e grade `2×2` em `[U] → Habilidades`. | Zero sobreposição ilegível e 100% de integridade de assets/sons. |
| **Datapacks & Reload** | `/reload` com JSON inválido, `previous_realm` inexistente, atributo inexistente. | Log claro isolando erro sem derrubar o servidor. |

## 2. Critérios de Bloqueio (Release Gate)
- Qualquer falha nos testes unitários JUnit ou crash em `/reload` **bloqueia** avanço de fase.
- Perda de progresso na morte/dimensão, truncamento de HP ou brecha de autoridade C2S **bloqueia** entrega.

## 3. Resultados da Auditoria QA — Fase 10 (Verificado)
- **Suíte JUnit 5 Completa:** `68` testes em `23` classes (`0` falhas, `0` erros, `0` ignorados), cobrindo regressão monotônica dos `21` patamares em `ProgressionBalanceRegressionTest`, blindagem `NaN`/`±Infinity` e rate-limits C2S em `MultiplayerSecurityAndHardeningTest`, e segurança de servidor dedicado/performance em `DedicatedServerAndPerformanceTest`.
- **Segurança Multiplayer & Permissões:** todos os `7` subcomandos mutantes de `/contrato` exigem nível OP `2`; pacotes C2S (`ToggleInfusion`, `CultivatorAction`, `ActivateUniqueSkill`) possuem rate-limits atômicos isolados por UUID com limpeza em logout e `ServerStoppedEvent`; Resquícios Divinos `[G]` respeitam `Player.canHarmPlayer` e bypass seguro da Infusão `[V]`.
- **Benchmark Hot-Loop (`EnergyService.tickAllEnergy`):** `20.000` player-ticks (`50` jogadores × `400` ticks) concluídos em `37 ms` (~`1.85 µs/player-tick`) após eliminação de recálculos redundantes de capacidade e redução de checagens em `SubjugationEvents`.
- **Validação de Pacote:** `46` JSONs (`data/` + `config/`) válidos e `5/5` scripts KubeJS `.js` validados via `node --check`.

## 4. Resultados Atuais — Atualizações 2.1 & Resquícios em 4 Fases (Verificado)
- **Suíte JUnit 5 Completa (`.\gradlew.bat clean test build --offline --info`):** **`90` testes em `28` suítes — `0` falhas, `0` erros, `0` ignorados** (`BUILD SUCCESSFUL in 34s`), cobrindo os `21` patamares em `ProgressionBalanceRegressionTest`, os 3 estados e teto de regeneração em `SpiritualIntegrityRecoveryTest`, a Disputa Espiritual em `SubjugationSystemTest`, as `12` técnicas em `CultivationSkillSystemTest`, os `30` sons `.ogg` em `AudioAndAssetIntegrityTest`, os `4` arcos segmentados em `DivineRingGeometryTest` e a contenção/anti-rush `4×4` em `DivinePhaseProgressionTest`.
- **Benchmark Hot-Loop 20Hz (`DedicatedServerAndPerformanceTest`):** `20.000` player-ticks concluídos em **`45 ms`** (`BENCHMARK_20K_TICKS_MS=45`, limite `< 1000 ms`).
- **Hardening Multiplayer & Segurança Dedicated-Server:** `6` canais C2S com rate-limits atômicos testados (`ToggleInfusion`, `CultivatorAction`, `ActivateUniqueSkill`, `ActivateCultivationSkill`, `CycleMartialSlot`, `CycleDivinePhase`), sanitização de `NaN`/`±Infinity` em `SyncEnergyS2CPacket` (`PROTOCOL_VERSION = "7"`), ausência de classes `net.minecraft.client.`/`com.mojang.blaze3d.` no código comum e limpeza estática em logout/shutdown.
