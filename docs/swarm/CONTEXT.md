# Swarm — Contexto Compartilhado (Memória de Trabalho)

> Leia este arquivo ANTES de qualquer tarefa.

## Projeto
- Mod `mythic_contracts` (`com.mythiccontracts`), Minecraft `1.20.1`, Forge `47.3.0`, Java `17`, Mojang mappings, Gradle `8.4`.
- Build & Testes: `$env:JAVA_HOME='C:\Users\lealt\AppData\Roaming\PrismLauncher\java\java-runtime-gamma'; .\gradlew.bat test build --offline`
- Sync modpack: `python tools/update_pack_index.py`
- Documentos centrais: `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/MODULE_CONTRACTS.md`, `docs/DATA_SCHEMA.md`, `docs/RESEARCH.md`, `docs/TEST_PLAN.md`, `docs/ROADMAP.md`, `docs/CHANGELOG.md`, `docs/PLAYER_AND_ADMIN_GUIDE.md`, `docs/FINAL_RELEASE_REPORT_2.0.md`.

## Decisões Aprovadas pelo Usuário
- **HUD Principal (Fase 5):** Conceito A (Minimalista Murim, canto superior esquerdo, 4 barras: Vida Corporal, Integridade Espiritual, Qi `[V]`, Vigor).
- **HUD Separada de Resistência Divina (Fase 5):** Widget circular compacto no canto inferior esquerdo, pouco maior que 1 slot da hotbar (`34 px` GUI de diâmetro, ícone `18×18`, anel `4 px`, `[G]`, `[I–IV]`, Sobrecarga 60s), configurável separadamente.
- **Interface do Cultivador (Fase 6):** Tecla `U` (`KeyConflictContext.IN_GAME`), Conceito C (Meridianos & Yin-Yang + `[3D / Meridianos]`) + barra inferior de 7 abas do Conceito B.
- **Sistemas & VFX:** Separação Vigor vs Resistência Divina; 4 Resquícios Divinos preservados (*Olho de Rá*, *Ceifa da Morrígan*, *Vajra de Indra*, *Passo de Tezcatlipoca*) com VFX client-side sem shaders.

## Fase Atual
**PROJETO CONCLUÍDO — Mythic Contracts 2.0.0 (Fases 0 a 11 Entregues):**
- Fase 5 (HUD Principal Minimalista Murim + Anel Circular `34 px` `[G]` + Config Cliente) concluída.
- Fase 6 (Interface do Cultivador `[U]` híbrida Conceito C + 7 abas Conceito B + `CultivatorActionC2SPacket`) concluída.
- Fase 7 (13 `ModParticles` + 52 sprites quadrados + `SpawnVisualEffectS2CPacket` v5 + `ClientVfxScheduler` com presets `LOW`/`MEDIUM`/`HIGH` compatível com Embeddium 0.3.31) concluída.
- Fase 8 (Datapacks expandidos: Facções `factions/*.json`, Resquícios `skills/*.json`, Contratos v2 + `veil_resonance_oath.json`, `DatapackValidationService` e comandos `/contrato validar` + `/contrato reputacao`) concluída.
- Fase 9 (Integração & Compatibilidade com KubeJS, FTB Quests, Iron's Spells, TaCZ, Better Combat, Simply Swords e Embeddium) concluída.
- Fase 10 (QA, Balanceamento & Otimização: OP `hasPermission(2)`, rate-limit em todos os C2S, sanitização `NaN`/`Inf`, bypass/PvP em `UniqueSkillHandler`, otimização 20Hz + benchmark e `68` testes em `23` suítes) concluída.
- Fase 11 (Entrega Final: versão `2.0.0` empacotada em `mythic_contracts-2.0.0.jar`, limpeza automática de JAR `1.0.0`, `O_Modpack_Definitivo_Fase3.mrpack`, Packwiz `index.toml` + `pack.toml`, Prism Launcher sincronizado, `PLAYER_AND_ADMIN_GUIDE.md` e `FINAL_RELEASE_REPORT_2.0.0.md`) concluída.
