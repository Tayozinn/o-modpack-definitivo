# Relatório T0-2: Auditoria de QA, Persistência, Multiplayer e Plano de Testes (TEST_PLAN)

## Tarefa
- **ID:** T0-2 (Fase 0 — Auditoria somete leitura)
- **Objetivo:** Auditar cobertura de testes atual, camada de persistência, atributos, rede/segurança cliente-servidor, multiplayer, desempenho por tick e definir a Matriz do Plano de Testes do **Mythic Contracts 2.0**.

## Status
**Concluído** (somente leitura; nenhum arquivo de código, recurso ou config foi alterado).

---

## Achados

### 1. Cobertura real dos 15 testes JUnit atuais e lacunas
Os 5 arquivos em `src/test/java/` somam **15 testes `@Test`** executados sem servidor Forge completo (foco em lógica pura e NBT):

| Arquivo | Qtd | O que cobre hoje |
|---|---:|---|
| `src/test/java/com/mythiccontracts/core/persistence/PlayerDataMigratorTest.java` | 4 | Migração completa `v1 -> v2` (L18-65), `v1` pré-Fase 4 inicializando Qi cheio (L68-83), `writeFull` vs `writeClientSync` ocultando dados privados do servidor (L86-120), resiliência a tags malformadas/vazias (L123-133). |
| `src/test/java/com/mythiccontracts/core/player/PlayerDataTest.java` | 3 | `copyFrom` em retorno do The End (`wasDeath=false`) vs morte real (`wasDeath=true`) (L16-44), drenagem e desligamento automático da Infusão ao zerar Qi (L47-56), compatibilidade do adapter legado `PlayerCoreData` (L59-76). |
| `src/test/java/com/mythiccontracts/cultivation/CultivationServiceTest.java` | 3 | Preservação das curvas v1 de dano/Qi (L27-36), reino JSON customizado com 4 estágios e requisitos de avanço (L39-108), bloqueio no estágio final sinalizando `NEEDS_BREAKTHROUGH` (L111-119). |
| `src/test/java/com/mythiccontracts/skill/remnant/DivineRemnantTest.java` | 2 | Parsing de IDs novos e aliases legados (`solar_judgment` -> `eye_of_ra`, etc.) (L14-23), equipar e persistir Resquício Divino independente de facção (L26-41). |
| `src/test/java/com/mythiccontracts/skill/remnant/DivineResonanceBalanceTest.java` | 3 | Escala inversa de capacidade/poder/custo por camada de Cultivo (L16-43), Sobrecarga Divina de 1200 ticks com recuperação de 35% (L46-73), round-trip NBT de Resistência Divina e Sobrecarga (L76-91). |

**Lacunas sem cobertura automatizada:**
1. Ciclo de vida Forge de Capabilities (`CoreDataProvider`, `CapabilityEvents`, `ModCapabilities.savePersistedBackup` / `restoreFromPersistedBackupIfNeeded`).
2. `AttributeService` e `CultivationService.applyCultivationAttributes` (especialmente comportamento de `MAX_HEALTH` e `clearByNamePrefix`).
3. `ContractManager` (assinatura/quebra, duração de contratos, `refreshPlayerContractAttributes`) e `ReloadListener`s com datapacks inválidos.
4. Rede (`ModNetworking`, serialização/validação de pacotes S2C e C2S).
5. Eventos de gameplay (`ContractGameplayEvents`, `SubjugationEvents`).
6. HUD (`CultivationHudOverlay`) e futura Interface do Cultivador.

---

### 2. Riscos de Persistência e Atributos

1. **Perda de vida ao atualizar `MAX_HEALTH` (`AttributeService.java` + `CultivationService.java`):**
   - `CultivationService.applyCultivationAttributes` (`CultivationService.java:239-244`) chama `AttributeService.clearByNamePrefix(player, CULTIVATION_MODIFIER_PREFIX)` **antes** de reaplicar os modificadores em `applyPathAttributes`.
   - Dentro de `AttributeService.clearByNamePrefix` (`AttributeService.java:137`), `clampHealthIfNeeded` reduz `player.getHealth()` para o `getMaxHealth()` **sem os bônus de cultivo**. Em seguida, `applyPathAttributes` restaura o `MAX_HEALTH` maior, mas a vida atual já foi truncada para o teto base (ex.: jogador com `40/40 HP` cai para `20/40 HP` ao logar, trocar de dimensão ou romper reino).
   - `CapabilityEvents.onPlayerCloned` (`CapabilityEvents.java:82-86`) contorna isso apenas no retorno do The End (`!wasDeath`), mas **não** em `onPlayerLoggedIn`, `onPlayerChangedDimension`, `onPlayerRespawn` (`syncAndRefreshPlayer`, L125-135) nem em avanços de reino.
   - Além disso, não há preservação da **proporção de vida** (`health / oldMaxHealth`) quando `MAX_HEALTH` aumenta (cultivo) ou diminui (ex.: Pacto de Lúcifer).

2. **`TRACKED_ATTRIBUTES` incompleto em `AttributeService` (`AttributeService.java:25-35, 115-140`):**
   - `clearByNamePrefix` itera apenas sobre `TRACKED_ATTRIBUTES`. Se um datapack de reino/contrato usar um atributo customizado/de outro mod que ainda não foi registrado em `trackAttribute` após um restart do servidor, modificadores antigos com o prefixo `mythic_contracts.` não serão limpos.

3. **Heurística de `restoreFromPersistedBackupIfNeeded` (`ModCapabilities.java:71-87`):**
   - `isDefaultState` verifica apenas `faction == NONE`, reinos/estágios == 1, contratos/subjugação vazios e `!vowBroken`. Ignora `skills` (ex.: Resquício equipado ou sobrecarga), `reputations`, `brokenContractsHistory` e futuros recursos (como Stamina física).
   - Também há risco inverso: um reset administrativo para R1-1 `NONE` sem contratos pode ser sobrescrito pelo backup em `PERSISTED_NBT_TAG` no próximo `syncAndRefreshPlayer` (`CapabilityEvents.java:128`).
   - `ModCapabilities.sync` (`ModCapabilities.java:94`) grava o `writeFull` inteiro no `PERSISTED_NBT_TAG` a cada sincronização; entidades subjugadas com `entitySnapshot` grande podem inflar o NBT do jogador.

4. **Migração `v1 -> v2 -> v3` (`PlayerDataSchema.java:16`, `PlayerDataMigrator.java:35-47`):**
   - Hoje `migrateIfNeeded` só trata `version <= 1 -> migrateV1ToV2`. A introdução de novos campos na 2.0 (ex.: Stamina física separada da Resistência Divina, estado da GUI do Cultivador) exigirá `CURRENT_VERSION = 3` e pipeline encadeado (`v1 -> v2 -> v3`) preservando saves `v2` sem resetar progresso.

---

### 3. Riscos de Segurança e Autoridade do Servidor (C2S)

- **Pacotes atuais:**
  - `ActivateUniqueSkillC2SPacket` (`ActivateUniqueSkillC2SPacket.java:24-30`) e `ToggleInfusionC2SPacket` (`ToggleInfusionC2SPacket.java:30-46`) não aceitam valores numéricos do cliente e validam `sender.isAlive()` / `!sender.isDeadOrDying()` e estado no servidor.
  - **Brecha encontrada:** `ToggleInfusionC2SPacket` não possui rate-limiting/cooldown entre alternâncias. Um cliente modificado pode enviar spam de pacotes por tick, forçando `serverLevel.sendParticles`, `playSound` e `ModCapabilities.syncEnergy` em rajada.
- **Pacotes C2S futuros (Interface do Cultivador):**
  - **Nunca confiar no cliente** para: níveis/ordens de reino, custos de Qi/itens/XP, multiplicadores de atributos, status de desbloqueio de Resquício/Contrato, ou valores diretos de recursos.
  - O cliente deve enviar apenas **intenções mínimas** (ex.: `AttemptBreakthroughC2SPacket(CultivationPath)`, `EquipRemnantC2SPacket(ResourceLocation)`), e o servidor deve validar: existência no registry ativo, pré-requisitos, cooldown/rate-limit, estado vivo/não-espectador e validade estrita de `ResourceLocation` / enums lidos do `FriendlyByteBuf`.
  - Manter o filtro de `PlayerDataSerializer.writeClientSync` (`PlayerDataSerializer.java:33-35, 179-193, 315-317`) para não vazar `entitySnapshot`, `signedAtTick`, `violationCount` e `brokenContractsHistory` ao cliente.

---

### 4. Riscos de Multiplayer

- **Isolamento por jogador:** `CoreDataProvider` é anexado por `Player` (`CapabilityEvents.java:35-43`) e `ClientPacketHandler` escreve apenas no `Minecraft.getInstance().player`.
- **Reentrância em combate PvP/Thorns (`ContractGameplayEvents.java:31, 132-214`):** `PROCESSING_INFUSION` é um `ThreadLocal<Boolean>`. Se o dano de um jogador A com infusão ativa disparar imediatamente um contra-ataque síncrono no mesmo callstack (que não seja `DamageTypes.THORNS`), o segundo `LivingHurtEvent` será ignorado enquanto `PROCESSING_INFUSION` estiver `true`.
- **Frequência S2C:** `ContractGameplayEvents.onPlayerTick` (`lines 96-98`) envia `SyncEnergyS2CPacket` a cada 10 ticks quando há mudança de energia/cooldown/sobrecarga, além de envio imediato em `onLivingHurt` (`line 180`). Novos recursos por tick (ex.: Stamina física) devem ser acoplados ao `SyncEnergyS2CPacket` (ou pacote delta único) para não dobrar a taxa de pacotes por jogador.
- **Broadcast de partículas/sons:** `onPlayerTick` (`lines 100-112`) emite partículas a cada 20 ticks por jogador com infusão ativa, e `onLivingHurt` (`lines 184-195`) emite 15 partículas a cada golpe. Combinado com o spam possível de `ToggleInfusionC2SPacket`, pode degradar FPS de clientes próximos em arenas multiplayer.

---

### 5. Riscos de Desempenho por Tick

1. **`SubjugationEvents.onLivingTick` (`SubjugationEvents.java:125-184`):**
   - `LivingEvent.LivingTickEvent` roda **todo tick para toda entidade viva carregada** no servidor. Embora faça early return quando `getOwnerUUID(mob) == null` (L131-134), ainda consulta `getPersistentData()` e parseia/checa UUID em 100% dos mobs do mundo 20x/s. Para Wardens/Withers subjugados, executa `level().getPlayerByUUID` e `level().getEntity` por tick.
2. **`SubjugationEvents.onEntityJoinLevel` (`SubjugationEvents.java:48-59`):**
   - O predicado do `AvoidEntityGoal` adicionado a `LOW_TIER_CELESTIAL_FIEND` executa `ModCapabilities.get(p)` durante a varredura de IA em múltiplos ticks.
3. **`ContractGameplayEvents.onPlayerTick` (`ContractGameplayEvents.java:37-119`):**
   - Custo atual baixo (1 lookup de capability + matemática de energia por tick + `ContractManager.onPlayerPeriodicTick` a cada 40 ticks), desde que `onPlayerPeriodicTick` evite varreduras de entidades por área.
4. **HUD (`CultivationHudOverlay.java:37-53`):**
   - Executa `ModCapabilities.get(player)` e `String.format` a cada frame (`renderAboveAll`). Posição fixa (`BOX_X=8, BOX_Y=8, BOX_WIDTH=204, BOX_HEIGHT=72`) sem margem configurável pode colidir com minimapas ou overlays de outros mods no canto superior esquerdo.

---

### 6. Matriz do Plano de Testes (TEST_PLAN — Mythic Contracts 2.0)

| Categoria | Cenários Obrigatórios | Critério de Aprovação |
|---|---|---|
| **Unitários (JUnit 5)** | Migração `v1 -> v2 -> v3`; cálculo de Qi, Stamina física vs Resistência Divina; regras de breakthrough/estágios JSON; contratos (assinatura, violação, expiração); Resquícios Divinos e Sobrecarga. | 100% dos testes passando offline via Gradle (`--offline`). |
| **Integração (Atributos & Ciclo de Vida)** | `applyCultivationAttributes` em login, troca de dimensão, respawn e breakthrough; preservação da proporção de vida quando `MAX_HEALTH` sobe/desce; limpeza de atributos customizados fora da lista padrão; compatibilidade com modificadores de terceiros. | Zero truncamento indevido de HP; zero modificadores duplicados ou órfãos. |
| **Persistência** | Morte real (`wasDeath=true`), retorno do The End (`wasDeath=false`), troca Nether/End/Overworld, logout/login, recuperação via `PERSISTED_NBT_TAG`, saves `v1` e `v2` carregados na `v3`, NBT corrompido. | Zero perda de reinos, contratos, skills ou subjugados; nenhum crash com NBT malformado. |
| **Multiplayer & Segurança C2S** | 2+ jogadores simultâneos; isolamento de HUD/capabilities; spam de `ToggleInfusionC2SPacket` e pacotes C2S da Interface do Cultivador com IDs inválidos/inalcançáveis/morto; verificação de `writeClientSync`. | Servidor rejeita payloads inválidos sem exceção; rate-limit impede flood de partículas/som; dados privados não vazam ao cliente. |
| **HUD / Resoluções** | `1280x720`, `1920x1080`, `2560x1440`, GUI Scale `1` a `4`; exibição simultânea de Qi, Resistência Divina (e Sobrecarga) e eventual Stamina física; abertura da Interface do Cultivador sem corte de texto/botões. | Zero sobreposição ilegível entre barras e painéis em todas as escalas testadas. |
| **Reload de Datapacks Inválidos** | `/reload` com JSON quebrado em `realms/` e `contracts/`, `order` duplicado, `previous_realm` inexistente ou cíclico, atributo inexistente no registry. | Log claro de erro, fallback seguro para definições válidas/builtin, servidor não trava nem perde estado de jogadores online. |
| **Regressão** | Curvas v1 de Qi/dano, aliases legados de Resquícios (`solar_judgment`, etc.), adapter `PlayerCoreData`, IA de `Warden`/`Wither` subjugados (sem friendly fire). | Comportamento legado preservado sem quebrar saves ou comandos existentes. |

**Critérios de Bloqueio (Release Gate):**
- Qualquer falha nos testes unitários JUnit ou crash em `/reload` com datapack inválido **bloqueia** avanço de fase.
- Perda de progresso em morte/dimensão, truncamento de HP em login/avanço de reino, ou brecha de autoridade C2S na Interface do Cultivador **bloqueia** merge para produção.

---

## Riscos (Resumo Priorizado)
1. **Alto:** Truncamento de `HP` atual em `CultivationService.applyCultivationAttributes` + `AttributeService.clearByNamePrefix` ao remover e reaplicar `MAX_HEALTH`.
2. **Alto:** Ausência de rate-limit em `ToggleInfusionC2SPacket` (e necessidade de validação estrita nos futuros C2S da Interface do Cultivador).
3. **Médio:** `SubjugationEvents.onLivingTick` rodando para todos os `LivingEntity` carregados todo tick.
4. **Médio:** `restoreFromPersistedBackupIfNeeded` incompleto para `skills`/novos subdomínios e necessidade de encadeamento `v1 -> v2 -> v3`.
5. **Médio:** Conflito conceitual/visual entre "Resistência Divina" (atual) e "Resistência" física (stamina da nova spec) no HUD e no pacote `SyncEnergyS2CPacket`.

## Recomendações
1. Refatorar `CultivationService.applyCultivationAttributes` e `AttributeService` para calcular delta/reaplicar `MAX_HEALTH` antes de clampar (ou preservar a proporção `health / oldMaxHealth`), e descobrir atributos com prefixo `mythic_contracts.` diretamente no `AttributeMap` do jogador em vez de depender só de `TRACKED_ATTRIBUTES`.
2. Adicionar rate-limit por jogador nos pacotes C2S atuais (`ToggleInfusionC2SPacket`) e definir contrato de validação estrita para todos os pacotes C2S da Interface do Cultivador.
3. Unificar nome/barra de Stamina física vs Resistência Divina antes de expandir `SyncEnergyS2CPacket` e `PlayerDataSchema` (`v3`).
4. Otimizar `SubjugationEvents.onLivingTick` (ex.: tag/flag leve ou marcação rápida antes de ler `CompoundTag` completo) e adicionar testes de integração para atributos e datapacks inválidos.

## Pendências
- Definir na Fase 1 a nomenclatura e separação final entre **Resistência Divina** (skills G) e **Resistência / Stamina** (física) para fechar o schema `v3` e o layout do HUD.
- Executar a suíte completa (`.\gradlew.bat test build --offline`) apenas quando autorizado após a Fase 0.
