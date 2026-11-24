# Relatório Final de Entrega — Mythic Contracts 2.0.0

**Data de Release:** `2026-11-23`  
**Alvo:** Minecraft `1.20.1` · Forge `47.3.0` (`[47.2.0,)`) · Java `17` · Mojang Official Mappings  
**Versão Final:** **`2.0.0`** (`Mythic Contracts 2.0`)

---

## 1. Artefatos de Distribuição Gerados e Verificados

| Artefato | Caminho | Tamanho | `SHA-256` |
| :--- | :--- | :---: | :--- |
| **Core Mod JAR (Build)** | `build/libs/mythic_contracts-2.0.0.jar` | `450.628` bytes | `e30b893ba1a0313fe0815603fd3be70fe0888c63a34f0034f5b0f305dcf1ea77` |
| **Core Mod JAR (Modpack)** | `mods/mythic_contracts-2.0.0.jar` | `450.628` bytes | `e30b893ba1a0313fe0815603fd3be70fe0888c63a34f0034f5b0f305dcf1ea77` |
| **Core Mod JAR (Prism Launcher)** | `%APPDATA%/PrismLauncher/instances/O_Modpack_Definitivo_Fase2/minecraft/mods/mythic_contracts-2.0.0.jar` | `450.628` bytes | `e30b893ba1a0313fe0815603fd3be70fe0888c63a34f0034f5b0f305dcf1ea77` |
| **Pacote Modrinth completo** | `O_Modpack_Definitivo_Fase3.mrpack` (`21` entradas) | `416.570` bytes | `600ee56c0297862a9bf5536a9f1941109641bf08962a241f4ef7d45a72aee240` |
| **Índice Packwiz** | `index.toml` (`41` arquivos rastreados) + `pack.toml` (`version = "2.0.0"`) | — | `fc9c31cb06cc32ac...` (ver `pack.toml`) |

- **Limpeza de versões obsoletas:** `0` arquivos `mythic_contracts-1.0.0.jar` remanescentes em `mods/`, `index.toml`, `.mrpack` ou na instância local do Prism Launcher (prevenção de `Duplicate mods found`).
- **Inventário interno do `mythic_contracts-2.0.0.jar`:** `140` classes compiladas, `42` arquivos JSON data-driven (`data/mythic_contracts/`) e `62` texturas PNG (`assets/mythic_contracts/`), com `META-INF/mods.toml` configurado em `version="2.0.0"`.

---

## 2. Resumo das Fases Concluídas (Fases 0 a 11)

| Fase | Marco | Status |
| :---: | :--- | :---: |
| **Fase 0** | Auditoria arquitetural completa, mapa de dependências e plano de riscos QA (`T0_AUDIT_*`) | Concluída |
| **Fase 1** | Pesquisa de bytecode Forge 47.3.0, tags de dano 1.20.1 e modlist Packwiz (`RESEARCH.md`) | Concluída |
| **Fase 2** | Concepção visual (HUD Murim Conceito A + Anel Divino separado `34 px` + Tela `[U]` C+B) | Concluída |
| **Fase 3** | Consolidação de governança (`ARCHITECTURE.md`, `DECISIONS.md`, `DATA_SCHEMA.md`, `ROADMAP.md`) | Concluída |
| **Fase 4** | Persistência NBT `v3` (`v1→v2→v3`), `AttributeService` seguro, Vida Corporal R1–R7, Vigor e Integridade Espiritual | Concluída |
| **Fase 5** | HUD Dupla (`MainVitalHudWidget` + `DivineResistanceRingWidget`), `ClientHudConfig` e `HudConfigScreen` | Concluída |
| **Fase 6** | Interface do Cultivador `[U]` (`CultivatorScreen` + `CultivatorActionC2SPacket` com 7 abas e preview 3D/Meridianos) | Concluída |
| **Fase 7** | 13 `ModParticles`, 52 sprites quadrados, `SpawnVisualEffectS2CPacket` v5 e `ClientVfxScheduler` (sem shaders) | Concluída |
| **Fase 8** | Datapacks expandidos (Facções, Resquícios, Contratos v2 + `veil_resonance_oath.json`) e `DatapackValidationService` | Concluída |
| **Fase 9** | Integrações KubeJS, FTB Quests (`9` quests), Iron's Spells, TaCZ, Better Combat, Simply Swords e Embeddium | Concluída |
| **Fase 10** | Hardening multiplayer (OP nível 2, rate-limits C2S, `NaN`/`±Infinity`, PvP, bypass `[G]`) e otimização 20Hz | Concluída |
| **Fase 11** | Release `2.0.0`, limpeza automática de JAR antigo, auditoria limpa e guias/relatório finais | Concluída |

---

## 3. Gates de Qualidade e Testes Verificados

- **Build limpa offline (`.\gradlew.bat clean test build --offline`):** `BUILD SUCCESSFUL in 32s` (`exit code 0`, `10/10` tarefas executadas).
- **Suíte JUnit 5 completa:** **`68` testes em `23` classes — `0` falhas, `0` erros, `0` ignorados**.
- **Benchmark do Hot-Loop 20Hz (`DedicatedServerAndPerformanceTest`):** `20.000` player-ticks (`50` jogadores × `400` ticks) concluídos em **`36 ms`**.
- **Servidor Dedicado & Rede:** `0` imports de `net.minecraft.client.` ou `com.mojang.blaze3d.` fora de `client/` e `ClientPacketHandler.java`; `DistExecutor` usando exclusivamente lambdas aninhadas; todos os `7` subcomandos mutantes de `/contrato` protegidos com `.requires(source -> source.hasPermission(2))`; `writeClientSync` omitindo dados privados de contrato/subjugação.
- **Recursos & Scripts do Modpack:** **`46/46` arquivos JSON** (`data/` + `config/`) parseados sem erro e **`5/5` scripts KubeJS `.js`** aprovados em `node --check`.

---

## 4. Distribuição e Sincronização Contínua

- Para publicar alterações futuras no repositório e atualizar colegas/instâncias Prism automaticamente:
  1. Execute `python tools/update_pack_index.py` (atualiza `index.toml`, `pack.toml`, `O_Modpack_Definitivo_Fase3.mrpack` e sincroniza `PrismLauncher`, removendo versões antigas de `mythic_contracts-*.jar`).
  2. Ou utilize [`SINCRONIZAR_MODPACK.bat`](../SINCRONIZAR_MODPACK.bat) para pull/build/sync/commit/push integrado ao GitHub.
- Referências operacionais: [`PLAYER_AND_ADMIN_GUIDE.md`](PLAYER_AND_ADMIN_GUIDE.md) · [`DATA_SCHEMA.md`](DATA_SCHEMA.md) · [`ARCHITECTURE.md`](ARCHITECTURE.md) · [`TEST_PLAN.md`](TEST_PLAN.md).
