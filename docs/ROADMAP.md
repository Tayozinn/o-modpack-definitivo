# Roadmap Oficial — Mythic Contracts 2.0

- [x] **Fase 0 — Auditoria Completa:** Mapa de classes, dependências, lacunas e riscos (`T0_AUDIT_ARCHITECTURE.md`, `T0_QA_RISKS_TESTPLAN.md`).
- [x] **Fase 1 — Pesquisa e Arquitetura:** Bytecode Forge 47.3.0, modlist Packwiz, contratos e schemas (`T1_RESEARCH_COMPATIBILITY.md`).
- [x] **Fase 2 — Concepção Visual:** Conceitos A/B/C, mockups in-game e aprovação final do usuário (`T2_VISUAL_CONCEPTS.md` + mockups).
- [x] **Fase 3 — Governança & Planejamento:** Documentação consolidada em `docs/` e plano aprovado para execução.
- [x] **Fase 4 — Sistemas Vitais:** Persistência `v2 -> v3`, `AttributeService` seguro, Vida Corporal nos 7 reinos, `VitalService` (Vigor), `SpiritualIntegrityService`, rede v3, rate-limit C2S e otimização `SubjugationEvents`.
- [x] **Fase 5 — HUD Principal & Emblema Circular `[G]`:** `ClientHudConfig` (`ForgeConfigSpec`), Painel Minimalista Murim (Conceito A) no canto superior esquerdo e Emblema Circular compacto (`34 px` GUI) da Resistência Divina no canto inferior esquerdo + tela de configuração in-game.
- [x] **Fase 6 — Interface do Cultivador (`U`):** `CultivatorScreen` híbrida (Conceito C Meridianos & Yin-Yang + 7 abas inferiores Conceito B), preview 3D/Meridianos e `CultivatorActionC2SPacket`.
- [x] **Fase 7 — Habilidades Divinas & VFX:** Sprites PNG customizados, partículas client-side sem shaders, pacote `SpawnVisualEffectS2CPacket` e presets `LOW`/`MEDIUM`/`HIGH`.
- [x] **Fase 8 — Datapacks & Expansibilidade:** Facções/Habilidades/Contratos v2 orientados a dados + validação cruzada.
- [x] **Fase 9 — Integração & Compatibilidade:** Testes com KubeJS, FTB Quests, Iron's Spells, TaCZ, Better Combat, Simply Swords e Embeddium.
- [x] **Fase 10 — QA, Balanceamento & Otimização:** Regressão completa, servidor dedicado, multiplayer e métricas.
- [x] **Fase 11 — Entrega Final:** JAR, documentação, guia e relatório final.

---

# Roadmap — Modpack Release 1.0 (`v2.1.0`)

- [x] **Etapa 1 — Core Mod 2.1 & Playtest In-Game:** 8 decisões da 2.1 implementadas, testadas e validadas in-game.
- [x] **Etapa 2 — Ecossistema de Bosses & Ameaças (`R1–R7`):** Cataclysm, BoMD, Mowzie's Mobs e Born in Chaos integrados às tags e `SubjugationBalance`.
- [x] **Etapa 3 — Loot Urbano 2010 & Economia KubeJS:** Loot customizado nas ruínas/metrôs do Lost Cities + 6 receitas de síntese cruzada.
- [x] **Etapa 4 — Campanha FTB Quests Completa:** 6 capítulos (`39` quests) em 2 grupos cobrindo do Despertar ao Endgame (*Ignis*).
- [x] **Etapa 5 — Atmosfera 2010, QoL & Keybinds:** Xaero's Minimap/World Map (topo direito, zero conflitos), Sound Physics Remastered, AppleSkin, Mouse Tweaks, Controlling/Searchables e Clumps.
- [x] **Etapa 6 — Auditoria Final, Bump `2.1.0` & Release:** Build limpa offline, auditoria JUnit/JSON/SNBT/KubeJS/Server e distribuição sincronizada.
- [x] **Atualizações 2.1 (I–III) — Regeneração Espiritual, Subjugação como Habilidade & Evolução das Habilidades:** 3 estados de regeneração + JSON balanceado, Disputa Espiritual com Pulso de Revolta e Selo catalisador, `12` Técnicas de Cultivo `R1–R7` em slots `S1–S3`, NBT `v4`, ribbons `AFTER_PARTICLES`, animações 1ª/3ª pessoa e `30` áudios `.ogg`.
- [x] **Atualização 2.1+ — Resquícios Divinos Progressivos em 4 Fases (Mockup 1 + Kit 4×4):** Contenção inicial bilateral anti-rush (`0.40x–1.45x`), `16` habilidades (`4 Deuses × H1–H4`) + passivas + Ultimates temporizadas (`65%`), `Shift + G` (`CycleDivinePhaseC2SPacket`, `PROTOCOL_VERSION = "7"`), emblema `[G]` com 4 arcos externos segmentados e grade `2×2` em `[U] → Habilidades` (`90/90` testes).
