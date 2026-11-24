# Plano de Testes e Critérios de Qualidade — Mythic Contracts 2.0

Consolidado a partir de `docs/swarm/reports/T0_QA_RISKS_TESTPLAN.md`.

## 1. Matriz de Testes Obrigatórios

| Categoria | Escopo e Cenários | Critério de Aprovação |
| :--- | :--- | :--- |
| **Unitários (JUnit 5)** | Migração NBT `v1 -> v2 -> v3`; `CultivationService` + bônus de `MAX_HEALTH`; `VitalService` (Vigor); `SpiritualIntegrityService` (classificação, absorção 30%/50%, Colapso Espiritual 20s, recuperação 25%); `DivineResonanceBalance` (Sobrecarga 60s). | 100% dos testes passando via `.\gradlew.bat test build --offline`. |
| **Atributos & Vida** | `AttributeService.runPreservingHealthRatio` no avanço de reino, login, mudança de dimensão, respawn e aplicação/remoção de `lucifer_pact`. | Zero truncamento de HP para `20.0`; zero modificadores duplicados ou órfãos. |
| **Persistência** | Morte (`wasDeath=true`), retorno do End (`wasDeath=false`), logout/login, recuperação via `PERSISTED_NBT_TAG`, NBT `v1`/`v2` na `v3`, tags corrompidas. | Zero perda de progresso; nenhum crash com NBT malformado. |
| **Multiplayer & C2S** | Isolamento por jogador; rate-limit em `ToggleInfusionC2SPacket`; validação autoritativa em `CultivatorActionC2SPacket`; `writeClientSync` ocultando dados privados. | Servidor rejeita payloads inválidos e flood C2S sem exceção. |
| **HUD & `CultivatorScreen`** | `1280×720`, `1920×1080`, `2560×1440`, GUI Scale `1`–`4`; convivência com hotbar, Jade, TaCZ e Iron's Spells; tamanho compacto do Emblema Circular (`34 px` GUI) no canto inferior esquerdo. | Zero sobreposição ilegível em todas as escalas testadas. |
| **Datapacks & Reload** | `/reload` com JSON inválido, `previous_realm` inexistente, atributo inexistente. | Log claro isolando erro sem derrubar o servidor. |

## 2. Critérios de Bloqueio (Release Gate)
- Qualquer falha nos testes unitários JUnit ou crash em `/reload` **bloqueia** avanço de fase.
- Perda de progresso na morte/dimensão, truncamento de HP ou brecha de autoridade C2S **bloqueia** entrega.

## 3. Resultados da Auditoria QA — Fase 10 (Verificado)
- **Suíte JUnit 5 Completa:** `68` testes em `23` classes (`0` falhas, `0` erros, `0` ignorados), cobrindo regressão monotônica dos `21` patamares em `ProgressionBalanceRegressionTest`, blindagem `NaN`/`±Infinity` e rate-limits C2S em `MultiplayerSecurityAndHardeningTest`, e segurança de servidor dedicado/performance em `DedicatedServerAndPerformanceTest`.
- **Segurança Multiplayer & Permissões:** todos os `7` subcomandos mutantes de `/contrato` exigem nível OP `2`; pacotes C2S (`ToggleInfusion`, `CultivatorAction`, `ActivateUniqueSkill`) possuem rate-limits atômicos isolados por UUID com limpeza em logout e `ServerStoppedEvent`; Resquícios Divinos `[G]` respeitam `Player.canHarmPlayer` e bypass seguro da Infusão `[V]`.
- **Benchmark Hot-Loop (`EnergyService.tickAllEnergy`):** `20.000` player-ticks (`50` jogadores × `400` ticks) concluídos em `37 ms` (~`1.85 µs/player-tick`) após eliminação de recálculos redundantes de capacidade e redução de checagens em `SubjugationEvents`.
- **Validação de Pacote:** `46` JSONs (`data/` + `config/`) válidos e `5/5` scripts KubeJS `.js` validados via `node --check`.
