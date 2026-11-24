# Changelog — Mythic Contracts 2.0

## [2.0.0 — Resquícios Divinos Progressivos em 4 Fases] — 2026-11-23
### Sistema de 4 Fases Anti-Rush, Kit `4×4` (`16` Habilidades) & HUD Mockup 1
- **Contenção Inicial & Desbloqueio Bilateral (`DivineResonanceBalance`):**
  - Substituída a escala livre por **4 Fases** combinando pontuação média de cultivo e piso mínimo simultâneo em Corpo e Espírito (*anti-rush*): **Fase I — Centelha Selada** (`R1-1`, `0.40x`, custo base `60.0`), **Fase II — Eco Desperto** (`R2-3`, piso `R2-1`, `0.75x`, +15% custo), **Fase III — Ressonância Plena** (`R4-2`, piso `R3-2`, `1.10x`, +35% custo) e **Fase IV — Encarnação** (`R6-1`, piso `R5-1`, `1.45x`, Ultimate consumindo `65%` da barra máxima de Resistência Divina).
- **Kit Completo de `16` Habilidades (`DivinePhaseAbility`, `UniqueSkillHandler`, `ContractGameplayEvents`):**
  - Cada um dos 4 Deuses Caídos (*Olho de Rá*, *Ceifa da Morrígan*, *Vajra de Indra*, *Passo de Tezcatlipoca*) recebeu habilidades distintas `H1–H4`, passivas ativas na Fase II+ (`-20%` fogo para Rá, abate recuperando Vigor/HP para Morrígan, `-10%` recarga para Indra, `-25%` queda para Tezcatlipoca) e Ultimates temporizadas com estado server-side em `H4`.
- **Controles (`Sub-opção B1`), Rede (`Protocolo "7"`) & Persistência:**
  - `G` conjura a habilidade selecionada (`H1–H4`) e **`Shift + G`** (`CYCLE_DIVINE_PHASE_KEY`) alterna as fases desbloqueadas via `CycleDivinePhaseC2SPacket` (rate-limit de `4` ticks); seleção direta na UI via `CultivatorActionC2SPacket.Action.SELECT_DIVINE_PHASE`.
  - Adicionado `ActiveDivinePhase` ao `SkillData`, `PlayerDataSerializer` e `SyncEnergyS2CPacket` (`PROTOCOL_VERSION = "7"`).
- **HUD Mockup 1, `[U]` `2×2` & VFX por Fase:**
  - `DivineRingGeometry` atualizado (`INNER_RADIUS = 10.6F..13.0F`, `PHASE_INNER_RADIUS = 13.7F..16.4F` em `4` arcos externos segmentados) e `DivineResistanceRingWidget` exibindo arcos I–IV e pill `H1–H4`.
  - `CultivatorScreen` (`[U] → Habilidades`) agora exibe trilha de 4 nós + grade `2×2` clicável de `H1–H4`; `ClientVfxScheduler` diferencia coreografias pelas 16 formas.
- **Qualidade:** adicionada suíte `DivinePhaseProgressionTest` e atualizados testes de geometria, balanço, datapacks, migração NBT e segurança multiplayer — **`90` testes passando em `28` suítes (`BENCHMARK_20K_TICKS_MS=45`)**.

## [2.0.0 — Atualizações 2.1 (I, II e III)] — 2026-11-23
### Integridade Espiritual, Subjugação como Habilidade & Evolução das Habilidades
- **Atualização I — Regeneração de Integridade Espiritual (`SpiritualIntegrityService` & `spiritual_integrity_balance.json`):**
  - Regeneração contínua em **3 estados** (`0.40x` em combate por `100t`, `1.00x` fora de combate, `2.20x` em meditação após `40t` agachado parado ou via *Respiração do Lótus Interior*; `0x` em Colapso), curva quadrática `R1–R7`, teto anti-exploit (`+50%` flat / `[0.25x, 2.25x]` percentual) e feedback discreto na HUD (`#E9D5FF`, `↑`, flash `180ms` `#F43F5E`, pulso `350ms` em `100%`).
- **Atualização II — Subjugação por Habilidade & Disputa Espiritual (`SubjugationService` & `ActiveSubjugationSession`):**
  - Técnica *Grilhão de Subjugação Espiritual* (`soul_subjugation`) inicia **Disputa Espiritual** autoritativa com Dominância `0% → 100%`, barra efêmera `6px` na HUD e supressão de **Pulso de Revolta** (`ALT + G` manual ou automático via Infusão `[V]`), Graus `I–IV`, limite de `1..5` servos e painel interativo em `[U] → Contratos`. O `SubjugationSealItem` permanece funcional no `R1` e atua como **Catalisador Opcional** (`-50%` custos, `+10%` HP, início em `65%`).
- **Atualização III — `12` Técnicas de Cultivo `R1–R7`, Partículas Fluidas, Animações & Som (`ModSounds`):**
  - `12` JSONs em `data/mythic_contracts/cultivation_skills/*.json`, slots marciais `S1–S3` (`R1`/`R3`/`R5`), atalhos `ALT + G` e `ALT + V`, `CycleMartialSlotC2SPacket` + `ActivateCultivationSkillC2SPacket` (`PROTOCOL_VERSION = "6"`), migração NBT **`v4`** (`PlayerDataMigrator`), `MotionMode` em `MythicParticleBase`, `ClientRibbonRenderer` (`AFTER_PARTICLES`), `ClientSkillAnimationController` + `PlayerAnimBridge` (*playerAnimator* via reflexão) e **`30` efeitos sonoros `.ogg` mono `44.1 kHz`** configuráveis em `ClientHudConfig`.
- **Qualidade:** **`84` testes passando em `27` suítes (`BENCHMARK_20K_TICKS_MS=38`)**.

## [2.0.0] — Release Final (Fase 11 Concluída)
### Fase 11 (Empacotamento, Limpeza & Documentação de Release `2.0.0`)
- Versão elevada de `1.0.0` (`2.0.0-alpha.*`) para **`2.0.0`** em `build.gradle` (`Specification-Version: 2`), `META-INF/mods.toml` (`displayName = "Mythic Contracts 2.0"`), `pack.toml`, `modrinth.index.json` e `SINCRONIZAR_MODPACK.bat`.
- `tools/update_pack_index.py` atualizado com `replace_core_jar` para remover versões obsoletas (`mythic_contracts-1.0.0.jar`) no modpack e no Prism Launcher antes de distribuir `mythic_contracts-2.0.0.jar`, evitando conflito de mods duplicados no Forge.
- Documentação final concluída: [`ARCHITECTURE.md`](ARCHITECTURE.md) atualizada para Protocolo `"5"` e todos os módulos `2.0.0`, adicionado [`PLAYER_AND_ADMIN_GUIDE.md`](PLAYER_AND_ADMIN_GUIDE.md) e gerado [`FINAL_RELEASE_REPORT_2.0.md`](FINAL_RELEASE_REPORT_2.0.md) com hashes `SHA-256` e métricas da build limpa (`68/68` testes JUnit, `20k` ticks em `36 ms`).

## [2.0.0-alpha.10] — Fase 10 (Concluída)
### Fase 10 (QA, Balanceamento, Segurança Multiplayer & Otimização)
- **Segurança Multiplayer & Permissões OP:**
  - Subcomandos administrativos de `ContractDebugCommand` (`dar`, `remover`, `faccao`, `murim`, `resquicio`, `reputacao`, `validar`) agora exigem `.requires(source -> source.hasPermission(2))`, mantendo `/contrato status` e `/contrato compat` abertos a jogadores e preservando recompensas `elevate_perms: true` do FTB Quests.
  - Adicionado rate-limit atômico em `ActivateUniqueSkillC2SPacket` (`6` ticks) e unificados `tryAcquireRateLimit`/`clearAllRateLimits` em `ToggleInfusionC2SPacket` (`10` ticks) e `CultivatorActionC2SPacket` (`8` ticks), com limpeza no logout e `ServerStoppedEvent`.
  - Proteção contra `NaN` e `±Infinity` em `CultivationData`, `SkillData` e nos dois construtores/decodificador de `SyncEnergyS2CPacket`.
- **Correções de Combate & Servidor Dedicado:**
  - `ContractGameplayEvents.runBypassingInfusionHooks` + `UniqueSkillHandler`: Resquícios Divinos `[G]` agora resolvem reinos canônicos via `CultivationService`, ignoram supressão de Pressão de Reino e double-dip de `[V]`/Vigor, e respeitam regras de PvP/Criativo/Espectador (`canHarmPlayer`).
  - Substituídas referências de método em `DistExecutor` (`MythicContractsMod`, `SubjugationSealItem`) por lambdas aninhadas seguras no servidor dedicado.
- **Otimização de Tick & Benchmark:**
  - `VitalService` e `SpiritualIntegrityService` ganharam sobrecargas para evitar 3 recálculos redundantes de capacidade por player-tick dentro de `EnergyService.tickAllEnergy`; `SubjugationEvents.onLivingTick` antecipou o cutoff para `mob.tickCount > 2`.
- Adicionadas 3 suítes JUnit 5 em `com.mythiccontracts.qa` (`ProgressionBalanceRegressionTest`, `MultiplayerSecurityAndHardeningTest`, `DedicatedServerAndPerformanceTest`) — **68 testes passando em 23 classes** (`20.000` player-ticks em `37 ms`).

## [2.0.0-alpha.9] — Fase 9 (Concluída)
### Fase 9 (Integração & Compatibilidade — Triângulo de Combate, KubeJS, FTB Quests & Embeddium)
- **Módulo `com.mythiccontracts.integration` (100% soft-dependency, server-safe):**
  - `ModCompatRegistry`: detecção segura de mods carregados (`kubejs`, `ftbquests`, `irons_spellbooks`, `tacz`, `bettercombat`, `simplyswords`, `embeddium`, `curios`, `jade`, `jei`) + resumo no startup e comando `/contrato compat`.
  - `CombatCompatService`: normaliza o Triângulo de Combate sob Infusão `[V]` e Vigor — canal `MELEE` (debounce de `3` ticks em sweeps multi-alvo do Better Combat, desconto `-15%` de Qi para `#mythic_contracts:qi_resonant_weapons` cobrindo `#simplyswords:uniques` / `#simplyswords:runic_gear`), canal `FIREARM` (`tacz:*` com escalas `0.35x` Corpo / `0.25x` Espírito / `0.30x` Qi sem drenar Vigor melee) e canal `ARCANE_SPELL` (`irons_spellbooks:*` com escala espiritual `0.65x` / custo `0.50x` e `0.25x` em AoE no mesmo tick).
  - `ModAdvancements` + `7` JSONs em `data/mythic_contracts/advancements/` (`root`, `body_realm_2`, `spirit_realm_2`, `first_faction`, `first_contract`, `divine_channeling`, `first_subjugation`), sincronizados em `ModCapabilities.sync` e `UniqueSkillHandler`.
  - `MythicContractsAPI`: fachada pública estática autoritativa para KubeJS e scripts de modpack.
- **Tags Data-Driven (`"required": false`):** `firearm_damage.json` (`#tacz:bullets`), `qi_resonant_weapons.json` (`#simplyswords:uniques`/`runic_gear`), escolas restantes de Iron's Spells em `arcane_spiritual_damage.json`, `irons_spellbooks:eldritch_magic` em `spiritual_damage.json`, `irons_spellbooks:dead_king` em `high_realm_boss.json` e `dead_king`/`citadel_keeper` em `subjugatable.json`.
- **KubeJS, FTB Quests, Comandos & Embeddium:** `kubejs/server_scripts/02_mythic_contracts_compat.js` (`global.MythicContracts`), capítulo `despertar_2010.snbt` expandido com `5` quests ligadas aos novos advancements, subcomandos `/contrato compat` e `/contrato reputacao <faccao> add <delta>`, e ocultação do painel vital sob F3 (`!mc.options.renderDebug`).
- Adicionadas suítes JUnit 5 (`CombatCompatServiceTest`, `ModCompatRegistryTest`, `ModIntegrationResourcesTest`) — 58 testes passando em 20 suítes.

## [2.0.0-alpha.8] — Fase 8 (Concluída)
### Fase 8 (Datapacks & Expansibilidade — Facções, Habilidades Divinas, Contratos v2 & Validação Cruzada)
- `DatapackJsonHelper`: utilitário compartilhado para parsing de cores hexadecimais, `AttributeModifier.Operation` e `passive_modifiers`/`attribute_modifiers`.
- **Facções Data-Driven:** `FactionDefinition`, `FactionRegistry`, `FactionReloadListener` (`data/mythic_contracts/factions/*.json` com 5 arquivos built-in) integrados em `SubjugationSealItem`, `MainVitalHudWidget`, `CultivatorMeridianMetrics` e modificadores passivos em `ContractManager`.
- **Resquícios Divinos Data-Driven:** `DivineSkillDefinition`, `DivineSkillRegistry`, `DivineSkillReloadListener` (`data/mythic_contracts/skills/*.json` para `eye_of_ra`, `morrigan_reap`, `indras_vajra`, `tezcatlipoca_step`) integrados em `UniqueSkillHandler`, `DivineResonanceBalance`, `CultivationActionService` e `CultivatorMeridianMetrics` preservando fórmulas e recargas padrão.
- **Contratos v2:** `ContractDefinition` e `ContractDataReloadListener` expandidos com pré-requisitos (`min_body_realm`, `min_spirit_realm`, `min_reputation`), recompensa de reputação (`reputation_reward`), bônus vitais (`qi_regen_bonus`, `spiritual_integrity_bonus`) e `taboo_summary` + novo contrato `veil_resonance_oath.json`, ligados a `EnergyService`, `SpiritualIntegrityService` e `CultivatorScreen`.
- **Validação & Comandos:** `DatapackValidationService` (execução em `ServerStartedEvent`/`OnDatapackSyncEvent`) + subcomandos `/contrato validar` e `/contrato reputacao <faccao> <valor>`.
- Adicionadas suítes JUnit 5 (`FactionRegistryTest`, `DivineSkillRegistryTest`, `DatapackValidationServiceTest`) — 50 testes passando em 17 suítes.

## [2.0.0-alpha.7] — Fase 7 (Concluída)
### Fase 7 (Habilidades Divinas & Motor VFX Client-Side)
- 13 `ParticleType`s customizados em `ModParticles` com `13` definições JSON (`assets/mythic_contracts/particles/`) e `52` sprites PNG quadrados RGBA (`16×16` e `32×32`, 4 frames por partícula) em `textures/particle/`.
- `MythicParticleBase` (`TextureSheetParticle` + `PARTICLE_SHEET_TRANSLUCENT`, `0xF000F0` fullbright sem shaders customizados — 100% compatível com Embeddium 0.3.31) + `ModParticleProviders`.
- `VfxQualityProfile` + `ClientVfxScheduler`: coreografia em keyframes para *Olho de Rá* (pilares solares + glifo `I–IV`), *Ceifa da Morrígan* (arcos de foice + curva Bézier de drenagem + asas de corvo `IV`), *Vajra de Indra* (ondas sônicas + raio vertical em bosses) e *Passo de Tezcatlipoca* (espelho fumegante + pós-imagem + rastro do dash), respeitando `LOW`/`MEDIUM`/`HIGH` e partículas vanilla.
- `SpawnVisualEffectS2CPacket` (`PROTOCOL_VERSION = "5"`, `ModNetworking.sendToNear`) substituindo loops server-side em `UniqueSkillHandler`, `ContractGameplayEvents` e `CultivationActionService` (incluindo explosões visuais para Sobrecarga, rejeição em recarga/sobrecarga, Colapso Espiritual e Quebra de Reino).
- Adicionada suíte unitária JUnit 5 (`VfxQualityProfileTest`, `SpawnVisualEffectS2CPacketTest`) — 43 testes passando.

## [2.0.0-alpha.6] — Fase 6 (Concluída)
### Fase 6 (Interface do Cultivador `U` & Ações Autoritativas)
- Texturas pixel-art `yinyang_core.png` (`24×24`) e `icon_gear.png` (`12×12`) em `assets/mythic_contracts/textures/gui/`.
- `ContractManager`: fallbacks built-in adicionados para todos os 4 contratos principais (`paladin_vow`, `lucifer_pact`, `blood_oath`, `throne_breaker_seal`).
- `CultivationActionService` + `CultivatorActionC2SPacket` (`PROTOCOL_VERSION = "4"`): validação server-side, consumo real de Pílulas/Tomos em Sobrevivência e rate-limit de `8` ticks por jogador.
- Keybind `U` (`CULTIVATOR_SCREEN_KEY`, `KeyConflictContext.IN_GAME`) e atualização de `defaultconfigs/options.txt`.
- `CultivatorScreen` + view-models (`CultivatorTab`, `CultivatorMeridianMetrics`): Silhueta dos Meridianos 2D com núcleo Yin-Yang rotacionável + toggle `[3D / Meridianos]` via `InventoryScreen.renderEntityInInventoryFollowsMouse` + barra inferior de 7 abas estilo bronze-ouro (*Visão Geral*, *Corpo*, *Espírito*, *Atributos*, *Habilidades*, *Contratos*, *Facção*) + atalho `[⚙ HUD]` + traduções `pt_br.json`/`en_us.json`.
- Novos testes JUnit 5 (`CultivatorMeridianMetricsTest`, `CultivationActionServiceTest`, `CultivatorActionC2SPacketTest`).

## [2.0.0-alpha.5] — Fase 5 (Concluída)
### Fase 5 (HUD Dupla, Emblema Circular `[G]` & Config Cliente)
- Texturas pixel-art nativas em `assets/mythic_contracts/textures/gui/` (`remnant_*.png` `18×18`, `divine_ring_frame.png` / `divine_ring_overload_cracks.png` `34×34`, `hud_icons.png` `64×16`).
- `ClientHudConfig` (`ForgeConfigSpec`, `mythic_contracts-client.toml`) + `HudAnchor` + `VisualQualityPreset` com cache `volatile` para renderização O(1) e isolamento dist-safe em `ClientBootstrap`.
- `MainVitalHudWidget` (Conceito A — Minimalista Murim no canto superior esquerdo, modos Padrão e Compacto, 4 barras vitais com barra fantasma, brilho de cura, absorção dourada, alerta de Colapso Espiritual, auto-ocultar Vigor e ocultação opcional de corações vanilla).
- `DivineResistanceRingWidget` + `DivineRingGeometry` (`34 px` GUI no canto inferior esquerdo com emblema do Deus equipado, anel radial horário, pills `[G]` e Camada `[I–IV]`, e estado rachado de Sobrecarga de 60s).
- `HudConfigScreen` nativa acessível via botão Config na lista de Mods com 3 abas e pré-visualização ao vivo.
- Adicionada suíte unitária JUnit 5 (`HudLayoutMathTest`, `HudAnimationStateTest`, `DivineRingGeometryTest`) — 31 testes passando.

## [2.0.0-alpha.4] — Fase 3 & Fase 4 (Concluída)
### Fase 3 (Governança)
- Consolidados `ARCHITECTURE.md`, `DECISIONS.md`, `MODULE_CONTRACTS.md`, `DATA_SCHEMA.md`, `RESEARCH.md`, `TEST_PLAN.md`, `ROADMAP.md` e `CHANGELOG.md` em `docs/`.
- Registrada direção visual aprovada: HUD Conceito A (Painel Vital superior esquerdo) + Emblema Circular compacto (`34 px` GUI) no canto inferior esquerdo + Interface do Cultivador na tecla `U` (Conceito C com 7 abas inferiores do Conceito B).

### Fase 4 (Sistemas Vitais — concluída)
- Migração NBT `v2 -> v3` para Integridade Espiritual e Vigor (Stamina).
- Correção de truncamento de HP via `AttributeService.runPreservingHealthRatio` + adição de Vida Corporal nos 7 reinos corporais (`body_realm_1..7.json`).
- `VitalService`, `SpiritualIntegrityService`, tags de dano próprias MC 1.20.1 sem hard-dependency, rede `PROTOCOL_VERSION = "3"`, rate-limit em `ToggleInfusionC2SPacket` e otimização em `SubjugationEvents`.

## [1.0.0 → 2.0.0-pre] — Base Anterior Validada
- Core modular v2 (`PlayerData`, `PlayerDataMigrator` v1→v2, `AttributeService`, `EnergyService`, `SyncEnergyS2CPacket`).
- Motor data-driven de 14 reinos (`RealmRegistry`, `RealmReloadListener`, `CultivationService`).
- 4 Resquícios Divinos independentes de facção (`Olho de Rá`, `Ceifa da Morrígan`, `Vajra de Indra`, `Passo de Tezcatlipoca`), evolução em Camadas `I–IV` e barra de Resistência Divina com Sobrecarga de 60s.
