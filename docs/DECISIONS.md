# Decisões Arquiteturais e Visuais — Mythic Contracts 2.0

Registro formal das decisões aprovadas até o marco da **Fase 3 (Governança & Planejamento)**.

---

## ADR-001 — Arquitetura de Monólito Modular
- **Problema:** A v1.0.0 acoplava dados, regras de cálculo, modificadores e eventos em classes centrais (`PlayerCoreData`, `ContractGameplayEvents`).
- **Alternativas consideradas:** (a) Múltiplos JARs; (b) Monólito modular em único JAR com pacotes isolados e serviços desacoplados.
- **Solução escolhida:** Monólito modular em único JAR (`mythic_contracts`), separando `core.player`, `core.persistence`, `core.service`, `attribute`, `cultivation`, `faction`, `contract`, `skill`, `subjugation`, `client`, `integration`.
- **Consequências / Riscos:** Deploy simples no Packwiz/Modrinth; exige disciplina estrita de fronteiras entre módulos e revisões cruzadas.

## ADR-002 — Separação entre Vigor (Resistência Física / Stamina) e Resistência Divina (`[G]`)
- **Problema:** A especificação 2.0 introduziu a 4ª barra vital ("Resistência"), enquanto o mod já possuía a barra "Resistência Divina" exclusiva para os Resquícios Divinos (`[G]`, com Sobrecarga de 60s e Fraqueza).
- **Alternativas consideradas:** (a) Fundir ambas numa única barra; (b) Manter recursos distintos no modelo de dados e na HUD.
- **Solução escolhida:** Coexistência com papéis distintos:
  - **Vigor (Stamina):** em `CultivationData`, escala com o Cultivo Corporal (`100` a `260+`), usado em ações físicas (corrida e ataques melee) e exibido como a 4ª barra no painel vital principal.
  - **Resistência Divina:** preservada 100% em `SkillData` (`100` a `540+`, custo `55→20`, Sobrecarga de `1200 ticks`), exibida em widget circular separado acoplado ao emblema do Resquício `[G]`.
- **Impacto:** Evita que sprint/combate corporal disparem 60s de Sobrecarga Divina e mantém as mecânicas já aprovadas intactas.

## ADR-003 — Identidade Visual Aprovada (HUD Dupla + Interface do Cultivador Híbrida)
- **HUD Principal (Fase 5):** **Conceito A — Minimalista Murim** no canto superior esquerdo (painel translúcido `#0B0F17`, bordas finas, Facção, `[V]`, Reinos Corpo/Espírito e 4 barras finas: **Vida Corporal**, **Integridade Espiritual**, **Qi** e **Vigor**).
- **HUD Separada da Resistência Divina (Fase 5):** Widget circular independente no **canto inferior esquerdo**, com porte compacto um pouco maior que 1 slot da hotbar (`34 px` GUI de diâmetro externo, ícone central `18×18`, anel radial de `4 px` ao redor, badges `[G]` e `[I–IV]`, além do anel vermelho rachado na Sobrecarga de 60s). Posição X/Y, opacidade e escala configuráveis independentemente da HUD principal.
- **Interface do Cultivador (Fase 6):** **Conceito C (Meridianos & Núcleo Yin-Yang)** no centro com alternância `[3D / Meridianos]`, combinada com a **barra horizontal inferior de 7 abas do Conceito B** (`Visão Geral`, `Corpo`, `Espírito`, `Atributos`, `Habilidades`, `Contratos`, `Facção`).

## ADR-004 — Keybind Livre para Interface do Cultivador (`U`)
- **Problema:** `R, B, C, Z, H, O, M` pertencem ao TaCZ; `X, Y` ao Iron's Spells; `K, N` ao Better Combat; `I, J, P` a Curios/FTB; `V, G` já são do `mythic_contracts`.
- **Solução escolhida:** Tecla **`U`** (`GLFW.GLFW_KEY_U`) sob `KeyConflictContext.IN_GAME`. O JEI usa `U` exclusivamente em `JEI_GUI_HOVER`, deixando `U` 100% livre em jogo e ergonomicamente ao lado de `I`/`J`/`P`.

## ADR-005 — Vida Corporal via `AttributeModifier` + Preservação de Proporção
- **Problema:** Remover e reaplicar modificadores de `Attributes.MAX_HEALTH` em `AttributeService.clearByNamePrefix` truncava o HP atual do jogador para `20.0` antes da reaplicação.
- **Solução escolhida:**
  1. Nunca chamar `setBaseValue` em `Attributes.MAX_HEALTH` (preservar compatibilidade com mods terceiros).
  2. Aplicar bônus de Vida Corporal nos 7 reinos corporais via `AttributeModifier` (`Operation.ADDITION`, UUID fixo `b8e42d01-5c3f-4e9b-8d26-889900bbccdd`).
  3. Executar toda mutação composta em `AttributeService.runPreservingHealthRatio(...)`, preservando `oldHealth / oldMaxHealth` (ou curando 100% no respawn pós-morte).

## ADR-006 — Classificação de Dano para Integridade Espiritual sem Hard-Dependency
- **Problema:** `DamageTypeTags.IS_MAGIC` e `Tags.DamageTypes` não existem no Minecraft 1.20.1 / Forge 47.3.0.
- **Solução escolhida:** Usar tags próprias `mythic_contracts:spiritual_damage` e `mythic_contracts:arcane_spiritual_damage` com entradas `"required": false` para `irons_spellbooks:*` e inclusão de `#minecraft:witch_resistant_to`, mais detecção por namespace `irons_spellbooks` e tag `#mythic_contracts:high_realm_boss` em tempo de execução.

## ADR-007 — Config Cliente via `ForgeConfigSpec` e VFX Client-Side sem Shaders
- **Solução escolhida:** `ForgeConfigSpec` (`ModConfig.Type.CLIENT` → `mythic_contracts-client.toml`) + `Screen` nativa de configuração (evitando adicionar dependências Maven externas ao build `--offline`), e arquitetura VFX nativa (`TextureSheetParticle` + sprites PNG + pacote S2C único respeitando presets `LOW`/`MEDIUM`/`HIGH`) compatível com Embeddium 0.3.31.
