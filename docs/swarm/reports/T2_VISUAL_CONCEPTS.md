# Dossiê de Concepção Visual — Mythic Contracts 2.0 (T2-1)

- **Tarefa:** `T2-1` — Fase 2 (Concepção Visual: HUD, Interface do Cultivador & Habilidades Divinas)
- **Status:** Concluído (aguardando revisão do coordenador e aprovação do usuário antes de qualquer código visual)
- **Escopo:** Somente concepção visual, wireframes, paletas, animações, arquitetura de VFX e viabilidade técnica em Minecraft 1.20.1 + Forge 47.3.0 + Embeddium 0.3.31 (zero alterações em `src/`, `config/` ou `mods/`).

---

## Resumo Executivo (Formato Padrão Swarm)

### Achados
1. **Estado atual da HUD (`CultivationHudOverlay.java`):** Atualmente existe um painel retangular de `204×72 px` no canto superior esquerdo (`X=8, Y=8`) com texto puro + 2 barras retangulares (`Qi` e `Resistência Divina`), exibindo Facção, `[V] ATIVA/OFF`, `Corpo R#-S | Espírito R#-S`, `[G] <Resquício> [Tier]` e `Selos:N`. Ainda **não** exibe **Vida Corporal**, **Integridade Espiritual** nem **Resistência Física (Stamina)**, não possui sprites/ícones, não tem animações de transição nem personalização do jogador.
2. **Estado atual dos Resquícios Divinos (`DivineRemnant.java`, `DivineResonanceBalance.java`, `UniqueSkillHandler.java`):** Os 4 Resquícios Divinos independentes de facção já estão confirmados e implementados com escalonamento em 4 Camadas (`Tier I: Mortal`, `Tier II: Despertar`, `Tier III: Ressonância`, `Tier IV: Encarnação`), barra exclusiva de `Resistência Divina` (`100` a `540+`, custo `55→20`, Sobrecarga de `60s` com Fraqueza e recuperação a `35%`). Hoje o VFX usa apenas `level.sendParticles(...)` do servidor com partículas vanilla (`DustParticleOptions`, `FLAME`, `END_ROD`, `SQUID_INK`, `CRIMSON_SPORE`, `SONIC_BOOM`, `ELECTRIC_SPARK`, `SMOKE`, `PORTAL`).
3. **Convivência no Modpack:** A HUD do `mythic_contracts` precisa coexistir sem sobreposição com a **Hotbar vanilla** (base-centro), **Jade** (topo-centro), **Iron's Spells 'n Spellbooks** (barra de Mana e barra de feitiços nas laterais/base conforme config) e **TaCZ** (contador de munição no canto inferior direito).

### Riscos
1. **Conflito semântico e visual entre "Resistência Física (Stamina)" e "Resistência Divina":** Se ambas usarem o rótulo "Resistência" ou cores quentes próximas, o jogador confundirá corrida/esquiva com o limite de Sobrecarga da tecla `[G]`. **Mitigação adotada em todos os conceitos:** distinção clara por **rótulo visual, ícone e faixa cromática** — **Vigor / Stamina** em verde-oliva/esmeralda claro (`#84CC16` / `#4ADE80`, ícone de bota/pulso) e **Resistência Divina** em âmbar/dourado-fogo (`#F59E0B`, ícone de chama/selo divino, com estado crítico rubro `#DC2626` em Sobrecarga). `[Proposta]` Caso o usuário prefira reduzir barras na tela, também documentamos o modo compacto que agrupa Resistência Divina diretamente acoplada ao slot `[G]`.
2. **Poluição visual em combate híbrido (armas de fogo + espadas + magia):** Uma HUD muito alta ou muito opaca atrapalha a mira do TaCZ e a leitura do campo em cidades em ruínas (`The Lost Cities`). Todos os conceitos preveem modo compacto, escala configurável e transparência ajustável.
3. **Sobrecarga de rede e FPS em VFX multiplayer:** Emitir 100+ chamadas `ServerLevel.sendParticles` por cast gera tráfego excessivo de pacotes e travamentos em placas integradas. A arquitetura de VFX proposta usa **1 pacote S2C compacto por evento** + **partículas customizadas client-side** (`TextureSheetParticle` com sprites PNG) + **3 presets de qualidade**.
4. **Incompatibilidade com Shaders:** Embeddium 0.3.31 (sem exigir Oculus/Iris) não garante suporte a pipelines de shaders customizados. Todos os conceitos de HUD e VFX foram projetados para operar 100% com **OpenGL padrão do MC 1.20.1 (`GuiGraphics`, `RenderSystem.enableBlend`, `PARTICLE_SHEET_TRANSLUCENT`/`LIT`)**.

### Recomendações
- **Para aprovação rápida do usuário:** Recomendamos apresentar os 3 conceitos de HUD e 3 conceitos de Interface do Cultivador como **escolhas independentes ou combináveis** (ex.: HUD **Conceito A — Minimalista Murim** para máxima limpeza em combate + Interface do Cultivador **Proposta 3 — Espelho dos Meridianos** para imersão na ficha).
- **Para as 4 Habilidades Divinas:** Adotar o sistema de **sprites PNG customizados em atlas + pacote S2C de coreografia** com os 3 presets (`Baixa`, `Média`, `Alta`), mantendo 100% de paridade visual sem shaders.

### Fontes Inspecionadas no Projeto
- `docs/swarm/CONTEXT.md`
- `PROJECT_HANDOFF_SUMMARY.md`
- `src/main/java/com/mythiccontracts/client/CultivationHudOverlay.java`
- `src/main/java/com/mythiccontracts/cultivation/UniqueSkillHandler.java`
- `src/main/java/com/mythiccontracts/skill/remnant/DivineRemnant.java`
- `src/main/java/com/mythiccontracts/skill/remnant/DivineResonanceBalance.java`
- `src/main/java/com/mythiccontracts/capability/FactionType.java`
- `src/main/java/com/mythiccontracts/core/player/PlayerData.java`
- `src/main/resources/data/mythic_contracts/realms/{body,spirit}/*.json` (14 reinos confirmados)
- `src/main/resources/data/mythic_contracts/contracts/*.json` (4 contratos confirmados)
- `src/main/resources/assets/mythic_contracts/lang/pt_br.json`

### Pendências (Decisões do Usuário)
1. Escolher o conceito base da **HUD** (**A**, **B**, **C** ou híbrido).
2. Escolher o conceito base da **Interface do Cultivador** (**Proposta 1**, **2** ou **3**).
3. Confirmar o tratamento visual de **Resistência Física vs Resistência Divina** (duas barras distintas com nomes/cores diferentes vs Resistência Divina acoplada ao medalhão `[G]`).
4. Confirmar se a HUD deve **substituir opcionalmente** a barra de corações vanilla (config desligada por padrão, ativável pelo jogador) quando exibir a **Vida Corporal**.
5. Aprovar o conceito sequencial e a identidade visual dos **4 Resquícios Divinos** (`Olho de Rá`, `Ceifa da Morrígan`, `Vajra de Indra`, `Passo de Tezcatlipoca`).

---

## 0. Vocabulário Confirmado vs Propostas de Lore

Para garantir **zero invenção silenciosa de lore**, a tabela abaixo separa o que já está **Confirmado no Projeto** do que é **`[Proposta]`** meramente visual/textual neste documento:

| Domínio | Confirmado no Projeto (Código / JSON / Aprovação Prévia) | Marcado como `[Proposta]` neste Dossiê |
|---|---|---|
| **Temática** | Fantasia Urbana 2010 (15 anos pós-guerra entre deuses mitológicos), Murim Duplo, Contratos, Subjugação | Textos de cabeçalho/flavor curtos nos wireframes |
| **Facções (5)** | `Sem Facção`, `Errantes do Véu`, `Juramentados da Luz`, `Irmandade da Dívida Eterna`, `Quebradores de Tronos` | Brasões/ícones geométricos de 12×12 px para cada facção |
| **Reinos Corporais (R1–R7)** | `Corpo Mortal`, `Refinamento Ósseo`, `Tempero de Ferro`, `Carne de Jade`, `Vigor Soberano`, `Corpo Imortal`, `Ápice Primordial` | Cores de gradiente por tier de reino |
| **Reinos Espirituais (R1–R7)** | `Centelha Espiritual`, `Condensação de Qi`, `Núcleo Dourado`, `Alma Nascente`, `Domínio Etéreo`, `Ascensão Celestial`, `Divindade Suprema` | Aura e cor do núcleo espiritual por reino |
| **Etapas (1–3)** | `Base`, `Fortificação`, `Despertar` | Pips/pontos iluminados `● ● ○` |
| **Contratos (4)** | `Voto de Paladino`, `Pacto de Lúcifer`, `Juramento de Sangue`, `Selo Quebra-Tronos` | Ícones de selo (dourado/carmesim/roxo) e selo rachado ao quebrar voto |
| **4 Resquícios Divinos (`[G]`)** | `Olho de Rá` (Rá, Egípcia), `Ceifa da Morrígan` (The Morrígan, Celta), `Vajra de Indra` (Indra, Védica), `Passo de Tezcatlipoca` (Tezcatlipoca, Asteca) | Sequência de frames VFX, sprites PNG e silhuetas Tier IV |
| **Camadas (`ManifestationTier`)** | `I Mortal`, `II Despertar`, `III Ressonância`, `IV Encarnação` | Molduras progressivas das camadas I→IV |
| **Recursos** | Vida Corporal (`MAX_HEALTH`), Integridade Espiritual, Qi (`[V]`), Resistência (Stamina), Resistência Divina (`[G]` + Sobrecarga 60s) | Rótulo sugerido `"Vigor"` ou `"Stamina"` para evitar colisão visual com `"Resist. Divina"` |

---

## 1. Três Propostas Completas de HUD

### Zonas Seguras de Tela (Compatibilidade com o Modpack)

```text
+-----------------------------------------------------------------------------+
| [CANTO SUP. ESQ.]           [TOPO CENTRO: Jade]         [CANTO SUP. DIR.]   |
|  Zona Primária HUD          (NÃO OCUPAR POR PADRÃO)     (Mapa/Efeitos Poção)|
|  Mythic Contracts                                                           |
|                                                                             |
|                                                                             |
|                                  [+] Mira                                   |
|                                                                             |
|                                                                             |
| [CANTO INF. ESQ.]          [BASE CENTRO: Vanilla]       [CANTO INF. DIR.]   |
|  (Iron's Spells /          Corações / Fome / XP         (TaCZ Munição /     |
|   Slot Alternativo)        + Hotbar (Slots 1-9)          Seletor de Tiro)   |
+-----------------------------------------------------------------------------+
```

> **Regra de ouro de layout:** Por padrão, todas as 3 propostas ancoram no **Canto Superior Esquerdo** (ou **Inferior Esquerdo**, selecionável), deixando **Topo-Centro** livre para o *Jade*, **Inferior Direito** livre para o *TaCZ* e **Base-Centro** livre para a hotbar vanilla e a barra de feitiços do *Iron's Spells*.

---

### Conceito A — Minimalista Murim (Estética Urbano-Marcial Limpa)

**Identidade visual:** Inspirado em interfaces modernas de ação (2010 urbano) fundidas com caligrafia Murim contida — linhas finas de `1 px`, fundo escuro translúcido de tinta sumi (`#0B0F17`), acentos em jade/aço e barras esguias sem ornamentos excessivos. Prioriza **leitura em 0,2 segundo durante tiroteios e combate melee**.

#### Diagrama ASCII de Layout (Modo Padrão: `228 × 98 px` | Modo Compacto: `176 × 64 px`)

```text
POSICIONAMENTO EM TELA (Canto Superior Esquerdo, X=10, Y=10):
+==============================================================+
|▌ [◆] Juramentados da Luz          [V] INFUSÃO ON   Selos: 3  |
|▌ Corpo R4-2 Carne de Jade   ● ● ○ | Esp. R3-3 Núcleo ● ● ●  |
|▌ ----------------------------------------------------------  |
|▌ ♥ Vida Corporal  [█████████████████░░░] 148/180  (+4 Abs.)  |
|▌ ◈ Int. Espiritual[██████████████░░░░░░]  86/120             |
|▌ ✦ Qi             [████████████████████] 210/210  [+12/s]    |
|▌ » Vigor (Stamina)[████████████░░░░░░░░]  64/100             |
|▌ ----------------------------------------------------------  |
|▌ [G] ☀ Olho de Rá [III]   Res.Div [██████████░░░░] 195/280  |
+==============================================================+

ESTADO DE SOBRECARGA (linha inferior transforma-se com borda rubra pulsante):
|▌ [G] ✖ SELADO · 42.3s     [████ SOBRECARGA DIVINA ████]      |
+==============================================================+

MODO COMPACTO (Toggle opcional — ideal para combate intenso com TaCZ):
+==========================================+
|▌ R4-2 / R3-3   [V]●  [G]☀III (Pronto) S:3|
|▌ ♥[██████████░░]148  ◈[████████░░] 86    |
|▌ ✦[████████████]210  » [██████░░░] 64    |
|▌ 🔥[█████████░░░]195/280                 |
+==========================================+
```

#### Paleta HEX (Conceito A)
| Elemento | Cor Primária | Cor Secundária / Fundo | Observação |
|---|---|---|---|
| Fundo do Painel | `#0B0F17` (alpha `0xB8` ~72%) | Borda `#334155` (alpha `0x90`) | Alto contraste sobre neve, areia e concreto urbano |
| Texto Principal / Subtítulo | `#F1F5F9` | `#94A3B8` | Sombra de texto 1 px (`shadow=true`) |
| **Vida Corporal** | `#EF4444` (cheia→média) / `#B91C1C` (crítica) | Fundo `#2A1215` / Absorção `#FACC15` | Rastro de dano (ghost bar): `#FCA5A5` |
| **Integridade Espiritual** | `#A855F7` (estável) / `#E879F9` (pulso) | Fundo `#21122E` / Fratura `#F43F5E` | Textura sutil de linha pontilhada quando `< 30%` |
| **Qi** | `#3B82F6` (normal) / `#06B6D4` (Infusão `[V]` ON) | Fundo `#0F1D33` | Borda ciano `#38BDF8` brilha quando `[V]` está ativa |
| **Resistência Física (Vigor)** | `#84CC16` (normal) / `#F59E0B` (exaustão) | Fundo `#182412` | Faixa mais fina (`5 px`) para hierarquia clara |
| **Resistência Divina** | `#F59E0B` (normal) / `#F97316` (`<30%`) | Fundo `#291B0E` | Sobrecarga: `#DC2626` + texto `#FEE2E2` |
| **Acentos de Facção (barra vertical 2 px à esquerda)** | Luz `#FBBF24` · Dívida `#EF4444` · Tronos `#A855F7` · Véu `#38BDF8` · Sem `#94A3B8` | — | Consistente com `CultivationHudOverlay.java` atual |

#### Tipografia & Iconografia (Conceito A)
- **Tipografia:** Fonte nativa do Minecraft 1.20.1 (`mc.font`) com escala `1.0f` nos títulos/números principais e `0.75f` via `PoseStack.scale()` nos subtítulos de etapa e taxas de regeneração (`+12/s`). Numerais romanos (`I`, `II`, `III`, `IV`) sempre em maiúsculas com colchetes finos.
- **Iconografia (`assets/mythic_contracts/textures/gui/hud_murim_atlas.png` — 128×128 px):**
  - Ícones minimalistas 9×9 px em traço limpo: Coração anatômico geométrico (Vida Corporal), Lótus/Prisma espiritual (Integridade), Espiral de Qi (Qi), Asa/Bota estilizada (Vigor), Selo de Chama (Resistência Divina), Corrente/Selo (Subjugados), mais 4 micro-glífo 12×12 px dos Resquícios Divinos (Olho Solar, Corvo/Foice, Raio Vajra, Espelho de Obsidiana).

#### Comportamento de Animações (Conceito A)
- **Perda de recurso:** Barra "fantasma" (`ghostRatio`) decai com interpolação exponencial suave (`lerp` após atraso de `6 ticks` / `300 ms`), tornando imediato quanto de Vida Corporal ou Integridade Espiritual foi arrancado no último golpe. Em dano `>= 20%` do máximo, micro-tremor horizontal (`±1.5 px` por `150 ms`).
- **Cura / Recuperação:** Faixa de brilho translúcido (`width=10 px`) desliza da esquerda para a direita sobre a barra durante ticks de cura positiva.
- **Alerta:**
  - Vida Corporal `< 25%` ou Integridade Espiritual `< 25%`: borda da barra pulsa com `0.5 + 0.5 * sin(time * 6.0)` e ícone alterna contraste.
  - Resistência Divina `< 30%`: cor muda para laranja-alerta `#F97316` com aviso discreto de risco de sobrecarga.
  - **Sobrecarga Divina (60s):** linha inferior fixa em rubro escuro com listras diagonais animadas (UV scroll em `blit`) e cronômetro regressivo `XX.Xs`.
- **Evolução (Breakthrough / Mudança de Camada I→IV):** Flash fino dourado-jade percorre a borda externa do painel por `1.2s` + banner compacto flutuante `12 px` abaixo do painel (`"▲ CORPO R4-3 · DESPERTAR"` ou `"✦ RESSONÂNCIA [III]"`), desaparecendo em fade-out sem bloquear a visão.

#### Personalização & Estimativa de Complexidade (Conceito A)
- **Personalização:** Posição (4 cantos + arraste/offset X/Y), Escala (`0.75x`–`1.50x`), Opacidade (`0%`–`100%`), Modo Compacto/Completo, Ocultar barras individuais (ex.: esconder Vigor quando 100% cheio por >3s — *auto-fade*), Desativar animações/tremor, Ocultar corações vanilla (opcional, OFF por padrão).
- **Complexidade MC 1.20.1:** **Baixa–Média** (~350–450 LOC, 1 atlas PNG `128×128`, ~14–18 draw calls por frame via `GuiGraphics`). Impacto de FPS com Embeddium: **desprezível (`< 0.1 ms/frame`)**.

---

### Conceito B — Fantasia Divina (Relicário Mitológico & Molduras Épicas)

**Identidade visual:** Evoca o peso épico da Guerra Pós-Colapso entre Deuses Caídos. Molduras metálicas chanfradas em bronze-ouro envelhecido (`#D4AF37` / `#785A28`), um **Medalhão Circular/Octogonal** à esquerda exibindo o emblema do **Resquício Divino `[G]`** (que muda de cor e ornamento conforme Rá, Morrígan, Indra ou Tezcatlipoca) e barras esculpidas em camadas que se expandem a partir do medalhão.

#### Diagrama ASCII de Layout (Modo Padrão: `254 × 104 px`)

```text
POSICIONAMENTO EM TELA (Canto Superior Esquerdo, X=8, Y=8):
       ╔═══════════════════════════════════════════════════════╗
  ╭────╨──╮  ✦ JURAMENTADOS DA LUZ        [V] INFUSÃO: ATIVA   ║
 ╱  ┌───┐  ╲  Corpo: R4-2 Carne de Jade  │ Esp.: R3-3 Núcleo   ║
│   │ ☀ │   │ ╠═══════════════════════════════════════════════╣
│  [ III ]  │ ♥ [██████████████████░░░░] 148/180  (+4)         ║
│   └───┘   │ ◈ [███████████████░░░░░░░]  86/120               ║
 ╲  [ G ]  ╱  ✦ [██████████████████████] 210/210               ║
  ╰═══╤═══╯   » [█████████████░░░░░░░░░]  64/100               ║
      │       🔥[██████████░░░░░░░░░░░░] 195/280  (Res. Div.)  ║
  [Selos: 3]  ╚═══════════════════════════════════════════════╝
  └─ Anel externo do medalhão = Progresso de Cooldown / Sobrecarga
     do Resquício Divino [G] (dourado quando pronto, rachado em Sobrecarga)

DETALHE DO MEDALHÃO POR RESQUÍCIO DIVINO:
  Olho de Rá:         Moldura Disco Solar (#FBBF24) + Olho Udyat
  Ceifa da Morrígan:  Moldura Asas de Corvo (#DC2626) + Foice Rubra
  Vajra de Indra:     Moldura Relâmpago Duplo (#38BDF8) + Cetro Vajra
  Passo Tezcatlipoca: Moldura Espelho Obsidiana (#1E1B4B + #22D3EE)
```

#### Paleta HEX (Conceito B)
| Elemento | Cor Primária | Destaque Metálico / Sombra | Observação |
|---|---|---|---|
| Fundo do Relicário | `#120E16` (alpha `0xD0` ~82%) | Moldura `#C8A24A` / Sombra `#3B2C14` | Estética de relíquia antiga / metal fosco |
| Texto Principal / Título | `#FEF3C7` (marfim-ouro) | `#CBD5E1` (dados secundários) | Alto destaque nobre |
| **Vida Corporal** | `#DC2626` → gradiente `#F87171` | Borda `#7F1D1D` / Absorção `#FDE047` | Rastro de dano: `#FECACA` |
| **Integridade Espiritual** | `#9333EA` → gradiente `#C084FC` | Borda `#4C1D95` / Fratura `#FB7185` | Brilho etéreo nas pontas da barra |
| **Qi** | `#2563EB` → `#22D3EE` (quando `[V]` ON) | Borda `#1E3A8A` → `#67E8F9` | Filigranas laterais acendem durante Infusão |
| **Resistência Física (Vigor)** | `#65A30D` → `#A3E635` | Borda `#365314` | Barra compacta acoplada sob o Qi |
| **Resistência Divina** | `#D97706` → `#FBBF24` | Borda `#78350F` | Em Sobrecarga: rachaduras `#EF4444` cruzam o medalhão e a barra |
| **Cores do Medalhão (`[G]`)** | Rá `#FBBF24` · Morrígan `#EF4444` · Indra `#38BDF8` · Tezcatlipoca `#22D3EE` | Aura de Camada IV `#FFFFFF` + cor do Deus | Usa diretamente `DivineRemnant.getHudColor()` |

#### Tipografia & Iconografia (Conceito B)
- **Tipografia:** `mc.font` com títulos em caixa alta estilizada em `0.85f` e números em `1.0f` com sombra dupla sutil nas áreas de destaque. Camada `I`–`IV` centralizada na base do medalhão em dourado/branco.
- **Iconografia (`assets/mythic_contracts/textures/gui/hud_divine_atlas.png` — 256×256 px):**
  - Moldura 9-slice metálica (`nine-patch`), 4 versões do medalhão 40×40 px (uma para cada Deus Caído), 4 coroas/arcos de Tier (`I` simples → `IV` alado/radiante), overlay de rachadura para Sobrecarga, ícones 11×11 px com relevo pixel-art (Coração blindado, Alma flamejante, Orbe de Qi, Relâmpago de Vigor, Selo de Correntes).

#### Comportamento de Animações (Conceito B)
- **Perda:** Barra fantasma com fade de `400 ms` + brilho de impacto branco de `100 ms` na ponta da barra afetada.
- **Cura:** Partículas 2D sutis (2–3 pixels brilhantes desenhados em `GuiGraphics`) sobem dentro da barra durante regeneração ativa.
- **Alerta:**
  - Vida/Integridade `< 25%`: vinheta interna sutil no painel e pulso rubi/violeta no ícone.
  - Cooldown `[G]`: máscara radial/vertical escurece o medalhão e clareia de baixo para cima conforme o recarregamento; ao chegar em `0.0s`, um "ring flash" dourado expande `6 px` em volta do medalhão.
  - **Sobrecarga Divina (60s):** sprite de vidro/pedra rachada sobrepõe o medalhão em vermelho-sangue pulsante e o anel externo regride ao longo dos 60s.
- **Evolução:** O medalhão gira brevemente um halo geométrico atrás do emblema por `1.5s` e exibe selo de ascensão com o nome completo do novo Reino (ex.: `"REINO ALCANÇADO: CARNE DE JADE"`).

#### Personalização & Estimativa de Complexidade (Conceito B)
- **Personalização:** Mesmas opções do Conceito A + **Espelhar Medalhão** (medalhão à esquerda ou à direita conforme canto ancorado) + **Tema de Moldura** (Ouro Clássico / Obsidiana / Cor da Facção) + toggle de ornamentos simplificados.
- **Complexidade MC 1.20.1:** **Média–Alta** (~600–750 LOC, atlas `256×256` + máscara de cooldown por scissor/uv, ~22–28 draw calls por frame). Impacto de FPS com Embeddium: **muito baixo (`~0.15 ms/frame`)**, mas exige mais trabalho de pixel art para ficar impecável.

---

### Conceito C — Interface do Cultivador (Meridianos & Núcleo Yin-Yang)

**Identidade visual:** Materializa visualmente o sistema **Murim Duplo (Corpo × Espírito)** que diferencia o modpack. Um **Núcleo Yin-Yang / Dantian** de `38×38 px` conecta-se por **linhas de meridianos** a dois ramos simétricos: o **Ramo Yang / Corporal** (Vida Corporal, Vigor, Reino do Corpo) e o **Ramo Yin / Espiritual** (Integridade Espiritual, Qi, Reino do Espírito), convergindo na base para o **Foco Divino (`[G]` + Resistência Divina)**. Quando a Infusão `[V]` é ativada, os meridianos iluminam-se em fluxo contínuo conectando Corpo e Espírito!

#### Diagrama ASCII de Layout (Modo Padrão: `248 × 106 px`)

```text
POSICIONAMENTO EM TELA (Canto Superior Esquerdo, X=10, Y=10):
+---------------------------------------------------------------+
|  [Facção: Errantes do Véu]         [V] MERIDIANOS ATIVOS  S:3 |
|                                                               |
|     ╭───╮      ══╦══ CORPO R4-2 (Carne de Jade)  ● ● ○        |
|    ╱ ◉  ░╲       ╠═ ♥ Vida   [██████████████░░] 148/180 (+4)  |
|   │ ◉  ░░ │══╬═══╩═ » Vigor  [██████████░░░░░░]  64/100       |
|   │ ░░  ○ │══╬═══╦═ ◈ Espír. [████████████░░░░]  86/120       |
|    ╲ ░  ○╱       ╠═ ✦ Qi     [████████████████] 210/210       |
|     ╰───╯        ╩══ ESPÍRITO R3-3 (Núcleo Dourado) ● ● ●     |
|    DANTIAN                                                    |
|   (Yin-Yang)   ───[G] 🌑 Tezcatlipoca [III] ───────────────── |
|   Equilíbrio   🔥 Res. Divina [█████████░░░░░░] 195/280       |
+---------------------------------------------------------------+

LEITURA VISUAL DO NÚCLEO YIN-YANG (38x38 px):
- Metade Yang (Cármim/Ouro): brilho proporcional ao Reino Corporal + Vida Corporal
- Metade Yin  (Violeta/Ciano): brilho proporcional ao Reino Espiritual + Integridade
- Rotação lenta (6°/s normal; 30°/s com Infusão [V] ativa; trava vermelha em Sobrecarga)
- Se Corpo e Espírito estiverem equilibrados (diferença <= 1 reino): aura harmônica discreta
```

#### Paleta HEX (Conceito C)
| Elemento | Cor Primária | Cor de Fluxo / Meridiano | Observação |
|---|---|---|---|
| Fundo Translúcido | `#090D14` (alpha `0xB4` ~70%) | Trilhas inativas `#1E293B` | Linhas de circuito/meridiano em `1 px` |
| **Ramo Yang — Vida Corporal** | `#E11D48` (carmim vivo) | Meridiano Yang `#FB7185` / Abs. `#FDE047` | Harmoniza com estética Murim |
| **Ramo Yang — Resistência Física (Vigor)** | `#10B981` (jade-esmeralda) | Meridiano auxiliar `#34D399` | Evoca "Vigor / Sopro Corporal" Murim |
| **Ramo Yin — Integridade Espiritual** | `#8B5CF6` (ametista espiritual) | Meridiano Yin `#C4B5FD` | Fissura magenta `#F43F5E` em dano severo |
| **Ramo Yin — Qi** | `#0EA5E9` (azul celeste) | Fluxo `[V]` ON `#22D3EE` (ciano elétrico) | Pulso luminoso corre pelos meridianos com `[V]` ON |
| **Foco Divino — Resistência Divina** | `#F59E0B` (âmbar divino) | Sobrecarga `#DC2626` | Fica no eixo inferior (acima/fora do ciclo mortal) |
| **Núcleo Yin-Yang** | Yang `#F8FAFC`/`#FB7185` · Yin `#1E1B4B`/`#38BDF8` | Borda Facção (`FactionType`) | Indica visualmente o equilíbrio Corpo×Espírito |

#### Tipografia & Iconografia (Conceito C)
- **Tipografia:** `mc.font` limpa, com separadores visuais sutis (`● ● ○` para etapas Base/Fortificação/Despertar) e números compactos alinhados à direita para leitura tabular rápida.
- **Iconografia (`assets/mythic_contracts/textures/gui/hud_meridian_atlas.png` — 256×128 px):**
  - Sprite circular do Núcleo Yin-Yang (`38×38 px`) rotacionável via `PoseStack.mulPose(Axis.ZP.rotationDegrees(angle))`, sprites de nós de meridiano (`6×6 px`), micro-ícones `9×9 px` e emblemas `12×12 px` dos 4 Deuses Caídos.

#### Comportamento de Animações (Conceito C)
- **Perda:** Barra fantasma suave + o meridiano correspondente ao recurso perdido pisca em branco/vermelho por `200 ms` (se perder Integridade Espiritual, apenas o ramo Yin treme; se perder Vida Corporal, apenas o ramo Yang treme).
- **Cura:** Pontos luminosos (`2×2 px`) percorrem o meridiano saindo do Núcleo Yin-Yang até a barra em regeneração.
- **Alerta:**
  - Vida `< 25%`: metade Yang pulsa em vermelho-escuro.
  - Integridade `< 25%`: metade Yin pulsa em violeta-magenta + "estática" leve nas bordas do ramo Yin.
  - Infusão `[V]` ativa: Núcleo acelera a rotação suavemente e um pulso ciano percorre ambos os ramos conectando Corpo e Espírito (ilustrando visualmente a fórmula híbrida do `CultivationBalance`).
  - **Sobrecarga Divina (60s):** Um anel de selamento rubro envolve o Núcleo Yin-Yang e bloqueia a linha inferior do Resquício `[G]`.
- **Evolução:** O Núcleo Yin-Yang expande um anel concêntrico dourado/ciano e todos os nós de meridianos acendem em cascata por `1.5s`.

#### Personalização & Estimativa de Complexidade (Conceito C)
- **Personalização:** Todas as opções base (ancoragem, X/Y, escala, opacidade, auto-fade de Vigor/Qi cheios, ocultar corações vanilla opcional) + **Desativar rotação do Yin-Yang** (acessibilidade/performance) + **Modo Somente Barras** (oculta o Yin-Yang para economia horizontal).
- **Complexidade MC 1.20.1:** **Média** (~500–600 LOC, atlas `256×128`, rotação 2D via `PoseStack` nativa no `GuiGraphics`, ~20–24 draw calls por frame). Impacto de FPS com Embeddium: **muito baixo (`~0.12 ms/frame`)**.

---

### Comparativo Rápido dos 3 Conceitos de HUD

| Critério | Conceito A (Minimalista Murim) | Conceito B (Fantasia Divina) | Conceito C (Meridianos & Yin-Yang) |
|---|---|---|---|
| **Área ocupada (padrão)** | `228 × 98 px` (mais compacta) | `254 × 104 px` (mais ornamentada) | `248 × 106 px` (equilibrada) |
| **Clareza em tiroteio (TaCZ)** | ★★★★★ Excelente | ★★★☆☆ Boa (requer opacidade menor) | ★★★★☆ Muito boa |
| **Identidade Murim Duplo** | ★★★☆☆ Sutil | ★★★☆☆ Foco nos Deuses | ★★★★★ Máxima (Corpo×Espírito visível) |
| **Destaque dos Resquícios `[G]`** | ★★★☆☆ Limpo/direto | ★★★★★ Protagonista (Medalhão) | ★★★★☆ Integrado no foco inferior |
| **Esforço de Arte (PNG)** | Baixo (`128×128`) | Alto (`256×256` detalhado) | Médio (`256×128` geométrico) |
| **Risco de FPS (Embeddium)** | Mínimo | Muito baixo | Muito baixo |

---

## 2. Três Propostas da Interface do Cultivador (`Screen` Dedicada)

Todas as 3 propostas implementam uma `Screen` nativa do Minecraft 1.20.1 (sem depender de libs externas), com:
- **7 Abas obrigatórias:** `1. Visão Geral`, `2. Cultivo Corporal`, `3. Cultivo Espiritual`, `4. Atributos`, `5. Habilidades`, `6. Contratos`, `7. Facção`.
- **Área de Visualização do Personagem 3D + Meridianos:** Renderização do avatar do jogador usando `InventoryScreen.renderEntityInInventoryFollowsMouse(...)` (API oficial MC 1.20.1) + camada opcional de **Sobreposição de Meridianos / Aura de Reino** (sprite 2D transparente que brilha nos pontos de Dantian, Coração, Coroa e Membros conforme o Cultivo) + botão/config `[3D: ON/OFF]` que substitui o modelo 3D por uma **Silhueta de Meridianos 2D** em PCs fracos ou caso algum mod de modelo cause artefatos.
- **Segurança Cliente-Servidor:** Toda ação clicável na tela (ex.: botão `Tentar Avanço de Etapa/Reino`, `Equipar Resquício [G]`, `Alternar Infusão`, `Inspecionar Quebra de Voto`) envia **apenas intenção via pacote C2S**, validada 100% no servidor antes de alterar `PlayerData` e sincronizar de volta via S2C.
- **Responsividade:** Dimensão base `340 × 216 px` (cabe perfeitamente em `1280×720` com GUI Scale `3x` → `426×240` e em `1920×1080` com GUI Scale `4x` → `480×270`), com área de rolagem (`scissor`) nas listas longas.

---

### Proposta 1 — Códice Murim Urbano (Abas Laterais à Esquerda + Avatar Fixo)

**Conceito:** Layout de "dossiê tático / códice marcial moderno" com navegação vertical à esquerda, painel de conteúdo principal no centro e coluna de visualização do personagem 3D/meridianos fixa à direita na `Visão Geral` (e recolhível/contextual nas demais abas). Extremamente rápido de navegar tanto com mouse quanto com teclado (`Tab` / `1–7` / `ESC`).

```text
+================================================================================+
|  CÓDICE DO CULTIVADOR · [NomeDoJogador]       [Facção: Errantes do Véu]   [X]  |
+------------------+-------------------------------------+-----------------------+
|  NAVEGAÇÃO       |  ABA ATIVA: [1] VISÃO GERAL         |  AVATAR & MERIDIANOS  |
|                  |                                     |  [3D] [Merid.] [⟳]   |
| >[1] Visão Geral |  ── STATUS VITAIS ────────────────  |   +---------------+   |
|  [2] Cult. Corpo |  ♥ Vida Corporal    148 / 180 (+4)  |   |     · ✦ ·     |   |
|  [3] Cult. Espír.|  ◈ Int. Espiritual   86 / 120       |   |      (O)      |   |
|  [4] Atributos   |  ✦ Qi               210 / 210       |   |     /│││\     |   |
|  [5] Habilidades |  » Vigor (Stamina)   64 / 100       |   |    / │◉│ \    |   |
|  [6] Contratos   |  🔥 Resist. Divina  195 / 280       |   |      / \      |   |
|  [7] Facção      |                                     |   |     /   \     |   |
|                  |  ── CULTIVO MURIM DUPLO ──────────  |   +---------------+   |
|                  |  Corpo:    R4-2 Carne de Jade       |   Corpo: R4 · Esp: R3 |
|                  |            [████████░░] 78%         |   Camada Divina: III  |
|                  |  Espírito: R3-3 Núcleo Dourado      |                       |
|                  |            [██████████] PRONTO!     |   Equipado [G]:       |
|  ──────────────  |                                     |   ☀ Olho de Rá [III]  |
|  Atalhos:        |  ── RESUMO ATIVO ─────────────────  |                       |
|  [V] Infusão     |  Contratos: 2 ativos · 0 quebrados  |   [Abrir Config HUD]  |
|  [G] Resquício   |  Entidades Subjugadas: 3 selos      |                       |
+------------------+-------------------------------------+-----------------------+
```

- **Navegação:** Clique nas abas laterais, teclas numéricas `1`–`7` ou roda do mouse sobre a barra lateral.
- **Pontos fortes:** Mínima profundidade de clique, excelente leitura em resoluções menores (`1280×720`), encaixe natural com o **Conceito A (Minimalista Murim)** da HUD.

---

### Proposta 2 — Relicário Divino (Abas Superiores Ornamentadas + Vitrine Central)

**Conceito:** Inspirado em grimórios/menus de RPG de fantasia sombria. As 7 abas ficam no topo como marcadores metálicos com ícones + tooltip; na `Visão Geral` o avatar 3D ocupa o centro como uma estátua viva cercada por cartões simétricos (Corpo à esquerda, Espírito à direita, Resquício Divino & Contratos abaixo).

```text
+================================================================================+
| [Visão] [Corpo] [Espír.] [Atrib.] [Habil.] [Contr.] [Facção]              [X]  |
+========================+=============================+=========================+
|  ◈ CAMINHO DO CORPO    |     ✦ VITRINE DO AVATAR     |  ✦ CAMINHO DO ESPÍRITO  |
|  R4 · Carne de Jade    |     [3D: ON] [Meridianos]   |  R3 · Núcleo Dourado    |
|  Etapa 2: Fortificação |      ╭───────────────╮      |  Etapa 3: Despertar     |
|  ● ● ○                 |      │    ·  ☀  ·    │      |  ● ● ●  (Breakthrough!) |
|                        |      │      (O)      │      |                         |
|  ♥ Vida:   148 / 180   |      │     / │ \     │      |  ◈ Int.Esp:  86 / 120   |
|  » Vigor:   64 / 100   |      │      ◉D       │      |  ✦ Qi:      210 / 210   |
|  ⚔ Bônus: +10.0 Dano   |      │      / \      │      |  ⚡ Infusão:  +15 Flat   |
|                        |      ╰───────────────╯      |                         |
|  [Ir p/ Cultivo Corpo] |  Juramentados da Luz · S:3  |  [Ir p/ Cultivo Espír.] |
+------------------------+-----------------------------+-------------------------+
|  RESQUÍCIO EQUIPADO [G]: ☀ Olho de Rá [III · Ressonância]  🔥 Res.Div 195/280  |
|  CONTRATOS ATIVOS: [Voto de Paladino ✓]  [Selo Quebra-Tronos ✓]    [HUD Gear]  |
+================================================================================+
```

- **Navegação:** 7 abas horizontais no topo com ícone + rótulo curto (`Visão`, `Corpo`, `Espír.`, `Atrib.`, `Habil.`, `Contr.`, `Facção`), e `Q`/`E` ou setas `←`/`→` para alternar abas rapidamente.
- **Pontos fortes:** Forte apelo visual/RPG, simetria perfeita entre Corpo e Espírito, encaixe natural com o **Conceito B (Fantasia Divina)** da HUD.

---

### Proposta 3 — Espelho dos Meridianos (Layout Assimétrico com Diagrama Interativo)

**Conceito:** O lado esquerdo permanente (`110 px`) é o **Espelho do Cultivador** — onde o jogador alterna com 1 clique entre **Modelo 3D Interativo** (segue o mouse + rotação manual) e **Mapa de Meridianos 2D** (mostrando 7 nós corporais e 7 nós espirituais acendendo conforme os 7 Reinos R1–R7!). O lado direito (`226 px`) exibe a barra superior de 7 abas compactas e o conteúdo aprofundado de cada aba.

```text
+================================================================================+
|  ESPELHO DO CULTIVADOR              [1][2][3][4][5][6][7]  Visão Geral    [X]  |
+--------------------------+-----------------------------------------------------+
|  [Avatar 3D] [Meridianos]|  ══ VISÃO GERAL DO CULTIVADOR ════════════════════  |
|                          |                                                     |
|     ╭──────────────╮     |  ┌─ CORPO (R4-2) ────────┐ ┌─ ESPÍRITO (R3-3) ───┐  |
|     │   ✦ Coroa R7 │     |  │ Carne de Jade         │ │ Núcleo Dourado      │  |
|     │     (O)      │     |  │ ♥ 148/180  » 64/100   │ │ ◈ 86/120  ✦ 210/210 │  |
|     │    / │ \     │     |  │ ● ● ○  (Fortificação) │ │ ● ● ●  (Despertar)  │  |
|     │   ●──◉──●    │     |  └───────────────────────┘ └─────────────────────┘  |
|     │   Dantian    │     |                                                     |
|     │    / │ \     │     |  ┌─ RESSONÂNCIA DIVINA [G] ──────────────────────┐  |
|     │   ●     ●    │     |  │ ☀ Olho de Rá · Camada III (Ressonância)       │  |
|     ╰──────────────╯     |  │ 🔥 Resist. Divina: [█████████░░░] 195 / 280    │  |
|                          |  │ Custo atual: 34 · Regen: +6.2/s · Estado: OK  │  |
|  Equilíbrio: HARMÔNICO   |  └───────────────────────────────────────────────┘  |
|  Corpo R4  ☯  Espír. R3  |                                                     |
|  Facção: Errantes do Véu |  ┌─ CONTRATOS & SUBJUGAÇÃO ──────────────────────┐  |
|  [⚙ Configurar HUD]      |  │ Contratos: 2 ativos · Selos Subjugados: 3     │  |
+--------------------------+-----------------------------------------------------+
```

- **Navegação:** Ícones + números das 7 abas no cabeçalho do painel direito (com tooltip completo ao passar o mouse + nome completo da aba ativa ao lado); painel esquerdo permanece visível em **todas** as 7 abas e muda de destaque conforme a aba (ex.: na aba `Cultivo Corporal` acende os meridianos Yang em vermelho/ouro; na aba `Habilidades` exibe a aura do Resquício Divino selecionado).
- **Pontos fortes:** Coesão máxima com a fantasia de cultivo Murim, reaproveita o espaço visual sem repetir o cabeçalho em todas as telas, encaixe natural com o **Conceito C (Meridianos & Yin-Yang)** da HUD.

---

### Detalhamento Funcional das 7 Abas (Comum às 3 Propostas)

| # | Aba | Conteúdo Principal & Controles | Ações C2S (Servidor Autoritativo) |
|---|---|---|---|
| **1** | **Visão Geral** | Resumo dos 5 recursos (Vida Corporal, Integridade Espiritual, Qi, Vigor, Resistência Divina), Reino/Etapa atual de Corpo & Espírito, Camada Divina (`I–IV`), Resquício `[G]` equipado, Facção, contagem de Contratos e Selos Subjugados + atalho para `Configurar HUD`. | Nenhuma direta (leitura + navegação rápida) |
| **2** | **Cultivo Corporal** | Trilha dos 7 Reinos confirmados (`Corpo Mortal` → `Ápice Primordial`), etapas `Base / Fortificação / Despertar`, requisitos do próximo avanço (reino anterior, reino oposto mínimo, tribulação/item exigido, custo de Qi), bônus atuais e futuros (`MAX_HEALTH`, `ATTACK_DAMAGE`, multiplicador da Infusão `[V]`, Vigor). | `AttemptCultivationBreakthroughC2SPacket(BODY)` (se elegível, validado no servidor) |
| **3** | **Cultivo Espiritual** | Trilha dos 7 Reinos confirmados (`Centelha Espiritual` → `Divindade Suprema`), etapas `1–3`, requisitos do próximo avanço, bônus atuais e futuros (Qi Máximo, Regen de Qi, Dreno de Infusão, Bônus Plano da Infusão `[V]`, Integridade Espiritual Máxima, redução de cooldown de `[G]`). | `AttemptCultivationBreakthroughC2SPacket(SPIRIT)` |
| **4** | **Atributos** | Painel tabular consolidado mostrando **Base + Cultivo + Contratos + Equipamentos/Mods = Total** para: Vida Corporal (`MAX_HEALTH`), Integridade Espiritual, Qi Máx/Regen, Vigor Máx/Regen, Resistência Divina (Máx/Custo/Regen), Dano Físico Passivo, Fórmula da Infusão `[V]` (buff % corpo + flat espírito), Armadura, Velocidade, Redução/Resistência Espiritual. | Somente leitura (calculado/sincronizado do servidor) |
| **5** | **Habilidades** | **Seção A — Infusão de Mana/Qi `[V]`:** status, dreno/s, dreno/hit, dano bônus atual e perfuração de Pressão de Reino (`#high_realm_boss`). **Seção B — 4 Resquícios Divinos `[G]`:** cards para `Olho de Rá`, `Ceifa da Morrígan`, `Vajra de Indra` e `Passo de Tezcatlipoca`, mostrando Deus/Panteão, Camada atual (`I–IV`), escala de dano/raio/efeitos por camada, preview visual dos 4 tiers e botão `Equipar`. | `EquipDivineRemnantC2SPacket(remnantId)` (bloqueado em Sobrecarga ou cooldown se regra exigir) |
| **6** | **Contratos** | Lista de contratos ativos (ex.: `Voto de Paladino`, `Pacto de Lúcifer`, `Juramento de Sangue`, `Selo Quebra-Tronos`) com modificadores, bênçãos passivas, **Tabus / Regras de Quebra de Voto** em destaque de alerta, histórico de `vowBroken` e lista de **Entidades Subjugadas** (Selos armazenados). | Somente leitura / inspeção detalhada (assinatura/quebra continua via gameplay/itens/eventos) |
| **7** | **Facção** | Insígnia e descrição da facção atual (`Sem Facção`, `Errantes do Véu`, `Juramentados da Luz`, `Irmandade da Dívida Eterna`, `Quebradores de Tronos`), compatibilidade/restrições de contratos (ex.: Luz bloqueia Pactos Demoníacos; Dívida Eterna bloqueia Votos Angelicais), bônus exclusivos (ex.: Subjugação `<=25%` + Aura de Temor nos Quebradores de Tronos) e painel de reputação/status `[Proposta]`. | Somente leitura (mudança de facção permanece restrita a gameplay/quests/comandos) |

---

## 3. Conceito Visual e Sequencial das 4 Habilidades Divinas (Resquícios de Deuses Caídos)

Todos os 4 Resquícios Divinos preservam integralmente os nomes e origens mitológicas aprovados pelo usuário (`DivineRemnant.java`), são independentes de facção e evoluem visualmente conforme a `ManifestationTier` (`I Mortal` → `II Despertar` → `III Ressonância` → `IV Encarnação`).

> **Regra de Lore:** Os nomes, panteões, mecânicas de dano, raio, debuffs/buffs e sobrecarga de 60s abaixo refletem o código real de `UniqueSkillHandler.java` e `DivineResonanceBalance.java`. Elementos puramente visuais (sprites, geometria de anéis, glifos e descrições estéticas) são propostas de direção de arte (`[Proposta]`) para substituir as partículas vanilla provisórias.

---

### 3.1. Olho de Rá (`mythic_contracts:eye_of_ra` — Rá, Mitologia Egípcia)

- **Arquétipo Visual:** Julgamento solar hierático, geometria sagrada dourada, pilares verticais de luz incandescente e aura de purificação.
- **Paleta HEX:** Ouro Solar `#FBBF24` · Núcleo Branco-Sol `#FEF9C3` · Brasa Âmbar `#F59E0B` · Contorno Laranja-Fogo `#EA580C`.
- **Sprites Customizados (`assets/mythic_contracts/textures/particle/`):**
  - `ra_eye_glyph.png` (32×32, 4 frames — Olho de Rá estilizado abrindo a íris solar)
  - `ra_solar_rune.png` (16×16, 4 frames — runa solar geométrica que gira no anel)
  - `ra_sun_flare.png` (16×16, 6 frames — centelha incandescente alongada)

#### Sequência Temporal Completa
| Fase | Janela | Mundo / VFX (Partículas & Geometria) | Áudio | HUD / Feedback |
|---|---|---|---|---|
| **1. Ativação** | `0–4 ticks` (`0–200 ms`) | Selo circular dourado (`ra_solar_rune`) abre sob os pés do conjurador; um pequeno `ra_eye_glyph` acende acima da cabeça olhando para fora. | `EVOKER_CAST_SPELL` (pitch `1.15`) + chime agudo sutil | Flash dourado `#FBBF24` no slot `[G]`; barra de Resistência Divina desconta com rastro luminoso |
| **2. Execução** | `4–10 ticks` (`200–500 ms`) | Duas ondas concêntricas expandem-se até `radius` (`45%` e `85%`), deixando rastro de `ra_sun_flare` em espiral ascendente. | `FIRECHARGE_USE` (pitch `1.1`) | Slot `[G]` entra em estado de cooldown |
| **3. Impacto** | `6–16 ticks` (`300–800 ms`) | Em cada alvo atingido (`Glowing` + `Weakness` + repulsão; fogo no Tier IV): **Pilar Solar** vertical desce do topo (`y + 4.5` até o chão) com núcleo branco-dourado e anel de impacto no piso. | Impacto seco de fogo (pitch proporcional ao Tier) | Action bar / micro-toast discreto: `☀ Olho de Rá [Tier] (N alvos)` |
| **4. Encerramento** | `16–40 ticks` (`0.8–2.0 s`) | Partículas douradas suaves (`ra_sun_flare` em baixa opacidade) orbitam o jogador enquanto durar `Regeneration` / `Absorption` / `Resistance`. | Dissipação suave | Ícones de buffs solares destacados na HUD |

#### Evolução por Camadas (`ManifestationTier`)
- **Tier I (`Mortal`):** 1 anel solar fino, pilares baixos (`2.5` blocos) em alvos, sem glifo aéreo.
- **Tier II (`Despertar`):** 2 anéis concêntricos com runas solares, pilares de `3.5` blocos, pequeno Olho de Rá (`0.9` bloco) acima do jogador por `0.5s`.
- **Tier III (`Ressonância`):** Disco solar radiante completo no chão, raios diagonais ligando o jogador aos pilares dos alvos, Olho de Rá médio (`1.4` bloco) abrindo em 4 frames.
- **Tier IV (`Encarnação`):** Grande **Olho de Rá billboard** (`2.2` blocos de largura, sprite translúcido de alta nitidez) manifesta-se atrás/acima do conjurador; pilares solares deixam brasas persistentes no solo (combinando com o efeito real de fogo por `4s`) e uma coroa dourada sutil envolve o jogador durante o buff defensivo.

---

### 3.2. Ceifa da Morrígan (`mythic_contracts:morrigan_reap` — The Morrígan, Mitologia Celta)

- **Arquétipo Visual:** Foice espectral de sangue e sombras, revoada de penas negras de corvo, filamentos de drenagem vital convergindo para o conjurador.
- **Paleta HEX:** Carmesim Sangue `#DC2626` · Rubi Escuro `#7F1D1D` · Obsidiana/Pena `#111827` · Brilho Carmim Claro `#FB7185`.
- **Sprites Customizados (`assets/mythic_contracts/textures/particle/`):**
  - `morrigan_feather.png` (16×16, 4 frames — pena negra com borda carmesim caindo em balanço)
  - `morrigan_slash_arc.png` (32×32, 5 frames — arco crescente de foice carmesim-negro)
  - `morrigan_blood_orb.png` (16×16, 4 frames — gotícula/orbe de drenagem vital)

#### Sequência Temporal Completa
| Fase | Janela | Mundo / VFX (Partículas & Geometria) | Áudio | HUD / Feedback |
|---|---|---|---|---|
| **1. Ativação** | `0–4 ticks` (`0–200 ms`) | Névoa escura recolhe-se para dentro do conjurador (implosão curta de 0,2s) enquanto penas negras (`morrigan_feather`) irrompem das costas. | `PHANTOM_FLAP` grave (pitch `0.7`) | Flash carmesim `#EF4444` no slot `[G]` |
| **2. Execução** | `4–9 ticks` (`200–450 ms`) | Varredura circular de `360°` com `morrigan_slash_arc` em anel externo carmesim + anel interno de sombra/tinta, acompanhado por revoada circular de penas. | `WITHER_SHOOT` (pitch `0.85`) | Cooldown iniciado |
| **3. Impacto** | `6–18 ticks` (`300–900 ms`) | Cada alvo atingido (`Wither`) emite um **arco curvo (curva Bézier simples)** de `morrigan_blood_orb` que viaja do centro do alvo até o peito do jogador (dreno de vida). | Pulso grave de absorção ao curar HP | Ao receber a cura (`player.heal`), a barra de **Vida Corporal** dispara sua animação de cura com tint carmesim-esmeralda! |
| **4. Encerramento** | `18–45 ticks` (`0.9–2.25 s`) | Penas negras descem lentamente ao redor da arena até desaparecerem em fade-out. | Eco abafado de asas | Resumo discreto: `🪶 Ceifa da Morrígan [Tier] (N colhidos)` |

#### Evolução por Camadas (`ManifestationTier`)
- **Tier I (`Mortal`):** Corte horizontal fino carmesim, poucas penas (8–10), feixe de dreno linear simples.
- **Tier II (`Despertar`):** Duplo anel (sombra + sangue), revoada visível de penas girando, feixes de dreno curvos.
- **Tier III (`Ressonância`):** Três arcos de foice cruzados em ângulos inclinados, penas com contorno rubro brilhante, halo de névoa escura no solo.
- **Tier IV (`Encarnação`):** **Asas espectrais de corvo** (par de sprites billboard simétricos atrás das costas do jogador por `1.0s`) + grande foice espectral giratória em `360°` + filamentos duplos de sangue pulsando de cada alvo até o jogador.

---

### 3.3. Vajra de Indra (`mythic_contracts:indras_vajra` — Indra, Mitologia Védica)

- **Arquétipo Visual:** Descarga soberana de trovão celestial, onda de choque sônica ciano-violeta e arcos voltaicos quebrando a guarda de bosses.
- **Paleta HEX:** Relâmpago Ciano `#38BDF8` · Núcleo Branco-Elétrico `#E0F2FE` · Trovão Violeta `#A855F7` · Índigo Tempestade `#312E81`.
- **Sprites Customizados (`assets/mythic_contracts/textures/particle/`):**
  - `indra_lightning_arc.png` (16×32, 6 frames — segmento de raio ziguezagueante com flicker rápido)
  - `indra_vajra_crest.png` (32×32, 4 frames — emblema geométrico do cetro Vajra)
  - `indra_shock_ring.png` (32×32, 5 frames — anel de pressão sônica em expansão)

#### Sequência Temporal Completa
| Fase | Janela | Mundo / VFX (Partículas & Geometria) | Áudio | HUD / Feedback |
|---|---|---|---|---|
| **1. Ativação** | `0–3 ticks` (`0–150 ms`) | Estalo elétrico instantâneo: arcos `indra_lightning_arc` saltam verticalmente do chão ao redor do jogador e convergem no centro do peito. | `LIGHTNING_BOLT_THUNDER` curto (vol `0.45`, pitch `1.4`) | Flash elétrico `#38BDF8` no slot `[G]` |
| **2. Execução** | `3–10 ticks` (`150–500 ms`) | Onda de choque (`SONIC_BOOM` central + `indra_shock_ring` ciano e anel externo violeta) expande rapidamente em alta velocidade. | `WARDEN_SONIC_BOOM` (vol `0.55`, pitch `1.25`) | Micro-pulso elétrico na borda da HUD (desativável nas opções) |
| **3. Impacto** | `5–16 ticks` (`250–800 ms`) | Descarga elétrica em cada alvo (`Slowness` + `Weakness` + repulsão). **Destaque especial para Bosses (`#high_realm_boss`, que recebem multiplicador `1.20x–1.75x`):** raio vertical ciano-branco extra cai diretamente sobre o boss! | Estalo metálico-elétrico em impacto de boss | Toast: `⚡ Vajra de Indra [Tier] (N fulminados)` |
| **4. Encerramento** | `16–35 ticks` (`0.8–1.75 s`) | Faíscas residuais (`ELECTRIC_SPARK` + `indra_lightning_arc` mini) crepitam sobre os alvos lentificados e no solo ao redor. | Trovão distante decaindo | — |

#### Evolução por Camadas (`ManifestationTier`)
- **Tier I (`Mortal`):** Pulso sônico compacto, faíscas ciano curtas nos alvos.
- **Tier II (`Despertar`):** Duplo anel ciano/violeta + arcos elétricos saltando do jogador para os alvos mais próximos.
- **Tier III (`Ressonância`):** Símbolo do **Vajra** (`indra_vajra_crest`) projeta-se no chão sob o conjurador; relâmpagos verticais caem em cadeia sobre alvos de alto reino.
- **Tier IV (`Encarnação`):** **Pilar de Tempestade** momentâneo desce sobre o conjurador ao disparar, manifestando o grande emblema do Vajra girando em ciano/violeta no plano horizontal e arcos voltaicos ramificados cobrindo toda a circunferência do raio ampliado (`+2.0` blocos).

---

### 3.4. Passo de Tezcatlipoca (`mythic_contracts:tezcatlipoca_step` — Tezcatlipoca, Mitologia Asteca)

- **Arquétipo Visual:** Espelho fumegante de obsidiana, fenda dimensional em turquesa-negro, pós-imagem espectral no ponto de partida e rastro de fumaça veloz no dash.
- **Paleta HEX:** Obsidiana Profunda `#1E1B4B` · Espelho Negro `#09090B` · Turquesa Jade `#22D3EE` · Reflexo Esmeralda `#14B8A6`.
- **Sprites Customizados (`assets/mythic_contracts/textures/particle/`):**
  - `tezca_obsidian_shard.png` (16×16, 4 frames — lasca angular de espelho negro com aresta turquesa)
  - `tezca_smoke_mirror.png` (32×32, 6 frames — anel de espelho fumegante estilhaçando)
  - `tezca_afterimage_wisp.png` (16×32, 4 frames — silhueta fumegante escura deixada no ponto de origem)

#### Sequência Temporal Completa
| Fase | Janela | Mundo / VFX (Partículas & Geometria) | Áudio | HUD / Feedback |
|---|---|---|---|---|
| **1. Ativação** | `0–2 ticks` (`0–100 ms`) | Um **Espelho de Obsidiana** (`tezca_smoke_mirror`) abre-se instantaneamente no ponto de origem do jogador enquanto a lâmina/corte em área atinge os inimigos próximos. | `PLAYER_ATTACK_SWEEP` (pitch `1.1`) | Flash turquesa `#22D3EE` no slot `[G]` |
| **2. Execução** | `2–12 ticks` (`100–600 ms`) | O jogador dispara no dash (`0.95` a `1.55` de escala), deixando uma **pós-imagem de fumaça obsidiana** (`tezca_afterimage_wisp`) no ponto de partida e um rastro de lascas `tezca_obsidian_shard` ao longo da trajetória. | `ENDERMAN_TELEPORT` (vol `0.75`, pitch `1.25`) | Rastro visual rápido na linha `[G]` |
| **3. Impacto** | `2–10 ticks` (`100–500 ms`) | Nos inimigos cortados na origem: estilhaços de obsidiana e fumaça turquesa explodem lateralmente. | Estalo vítreo agudo sutil | Toast: `🌑 Passo de Tezcatlipoca [Tier]!` |
| **4. Encerramento** | `12–40 ticks` (`0.6–2.0 s`) | Rastro de fumaça leve acompanha os pés do jogador durante o buff de `Speed` (`Strength` em Tier III+, `Invisibility` em Tier IV — nesta última, o corpo dissolve-se em fumaça obsidiana ao sumir!). | Vento sombrio curto | Ícones de Speed / Força / Invisibilidade ativos na HUD |

#### Evolução por Camadas (`ManifestationTier`)
- **Tier I (`Mortal`):** Pequeno anel de fumaça escura na partida e rastro curto de 5–6 partículas turquesa.
- **Tier II (`Despertar`):** Espelho circular de obsidiana visível na partida, estilhaços girando em espiral atrás do dash.
- **Tier III (`Ressonância`):** Pós-imagem escura nítida permanece por `0.6s` no ponto de origem antes de estilhaçar em lascas turquesa; bordas da lâmina de corte brilham em jade.
- **Tier IV (`Encarnação`):** **Duplo Espelho Fumegante** (um na saída, outro na chegada do dash), rastro denso de fumaça obsidiana-turquesa e dissolução teatral em estilhaços quando o buff de `Invisibility` (`40 ticks`) ativa.

---

### 3.5. Feedback Padronizado de Falha, Recarga e Sobrecarga (Todas as 4 Habilidades)

| Estado | Feedback na HUD | Feedback no Mundo / Jogador | Feedback Sonoro |
|---|---|---|---|
| **Em Recarga (Cooldown)** | Slot `[G]` exibe contagem `XX.Xs` em amarelo `#FACC15` + preenchimento progressivo de fundo; tentar acionar faz micro-shake (`2 px`, `120 ms`) | Nenhuma partícula no mundo (evita ruído) | Clique grave curto (`UI_BUTTON_CLICK` pitch `0.7`) + Action Bar `⏳ Resquício Divino em recarga: X.Xs` |
| **Recurso Insuficiente** *(caso aplicável em habilidades de Qi / Vigor ou custos futuros)* | Barra correspondente (Qi ou Vigor) pisca 2× em vermelho-claro `#FCA5A5` por `300 ms` | 3–4 faíscas pálidas falhando ao redor das mãos | Som abafado (`FIRE_EXTINGUISH` vol `0.35`, pitch `1.4`) |
| **Disparo de Sobrecarga Divina (ao zerar Resist. Divina)** | Barra de Resistência Divina trava em rubro `#DC2626`, listras diagonais animadas e rótulo `SOBRECARGA: 60.0s`; slot `[G]` exibe `[G] SELADO` | Explosão contida de fumaça escura + estilhaços rubros ao redor do jogador (melhoria visual do `SMOKE` + `CRIT` atuais) + anel de selamento partido | `GLASS_BREAK` + `WITHER_HURT` (já existentes em `UniqueSkillHandler.java`) |
| **Tentativa de Uso Durante Sobrecarga** | Borda da barra de Sobrecarga pulsa em branco-vermelho + shake `3 px` | 6 partículas de fumaça rubra saindo do peito (sinalizando rejeição corporal) | Estalo grave (`BEACON_DEACTIVATE` vol `0.4`, pitch `0.65`) + Action Bar vermelha |
| **Fim da Sobrecarga (Estabilização a 35%)** | Barra volta de rubro para âmbar `#F59E0B` com flash suave de reconexão | Pequeno pulso circular claro ao redor do jogador | Som harmônico suave (`EXPERIENCE_ORB_PICKUP` pitch `0.9`) |

---

### 3.6. Arquitetura Técnica de Efeitos Visuais (MC 1.20.1 + Embeddium, SEM Shaders)

#### Princípios de Design
1. **Zero dependência de shaders / Oculus / Iris:** Todo o VFX usa `TextureSheetParticle` nativo do Forge 1.20.1 (`ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT` e `PARTICLE_SHEET_LIT` com `getLightColor()` retornando `0xF000F0` fullbright para brilhos solares/elétricos/carmesins funcionarem mesmo no apagão elétrico das cidades de 2010!).
2. **Coreografia Client-Side via 1 Pacote S2C (`PlayDivineEffectS2CPacket`):**
   - Em vez de o servidor enviar 60–120 pacotes individuais `ClientboundLevelParticlesPacket`, o servidor envia **1 único pacote** para jogadores num raio de `48 blocos` contendo: `remnantId (byte)`, `tier (byte)`, `origin (Vec3)`, `lookDir (Vec3)`, `radius (float)` e até `12` posições compactadas de alvos atingidos.
   - O cliente executa um `ClientVfxScheduler` leve (fila de micro-keyframes de `0` a `40 ticks` no `ClientTickEvent.END`) que instancia as partículas customizadas respeitando o **Preset de Qualidade do próprio cliente** — jogadores com PC fraco veem `Baixa` enquanto jogadores com PC forte veem `Alta` no mesmo servidor!
3. **Sprites PNG Customizados em `assets/mythic_contracts/textures/particle/`:**
   - 12 sprites `16×16` ou `32×32` com `SpriteSet` registrado em `RegisterParticleProvidersEvent` (sem necessidade de GeckoLib ou modelos 3D pesados para partículas).
   - Billboards grandes (como o `Olho de Rá` Tier IV ou `Asas da Morrígan` Tier IV) são simplesmente partículas customizadas com `quadSize` maior (`1.2f–2.2f`), `hasPhysics = false`, `lifetime = 12–20 ticks` e curvas de `alpha`/`scale` suaves — 100% compatível com Embeddium e Entity Culling.

#### Presets de Qualidade VFX (Configurável no Cliente)

| Parâmetro | Preset **Baixa** (PC Fraco / FPS Máx.) | Preset **Média** (Padrão Recomendado) | Preset **Alta** (Visual Completo) |
|---|---|---|---|
| **Orçamento máx. de partículas / cast** | `24–36` partículas | `55–85` partículas | `110–150` partículas |
| **Geometria de anéis** | 1 anel simplificado (`12` pontos) | 2 anéis concêntricos (`20–24` pontos) | 2–3 anéis + espirais (`32–40` pontos) |
| **Efeitos por alvo atingido** | Até `4` alvos mais próximos | Até `8` alvos | Até `12` alvos + arcos secundários |
| **Silhuetas / Billboards Tier IV** | Ícone compacto (`1.0` bloco, sem aura extra) | Sprite completo (`1.8` blocos) | Sprite completo (`2.2` blocos) + camadas de brilho |
| **Duração de rastros / encerramento** | `50%` (máx. `20 ticks`) | `100%` (`35–45 ticks`) | `120%` (`45–55 ticks`) |
| **Raio de renderização (outros jogadores)** | `24 blocos` | `40 blocos` | `56 blocos` |
| **Efeitos de tela / tremor HUD** | Desativado | Sutil | Completo |

---

## 4. Guia Rápido de Aprovação para o Usuário

Para destravar a implementação visual sem retrabalho, basta o usuário escolher **1 opção por linha** (ou combinar elementos):

1. **Estilo da HUD:**
   - **[A]** Minimalista Murim *(recomendado para máxima limpeza em combate com TaCZ + Better Combat)*
   - **[B]** Fantasia Divina *(foco épico no Medalhão dos Deuses Caídos)*
   - **[C]** Interface do Cultivador — Meridianos & Yin-Yang *(foco máximo no Murim Duplo Corpo×Espírito)*
2. **Estilo da Interface do Cultivador (7 Abas):**
   - **[1]** Códice Murim Urbano *(abas laterais + avatar à direita)*
   - **[2]** Relicário Divino *(abas no topo + vitrine central simétrica)*
   - **[3]** Espelho dos Meridianos *(painel permanente 3D/Meridianos à esquerda + 7 abas à direita)*
3. **Separação Resistência Física vs Resistência Divina:**
   - **[Opção I — Recomendada]** Manter ambas visíveis com identidade distinta: **Vigor (Stamina)** verde-oliva/jade e **Resistência Divina** âmbar/rubro (Sobrecarga 60s).
   - **[Opção II]** Modo compacto padrão: exibir **Vigor** apenas quando `< 100%` (auto-fade) e manter **Resistência Divina** fixa ao lado de `[G]`.
4. **Corações Vanilla:**
   - Manter corações vanilla visíveis por padrão e oferecer toggle opcional na config da HUD para ocultá-los (recomendado).
5. **VFX dos 4 Resquícios Divinos (`Olho de Rá`, `Ceifa da Morrígan`, `Vajra de Indra`, `Passo de Tezcatlipoca`):**
   - Aprovar sequência de 4 fases + evolução Tier I–IV + sprites PNG sem shaders + presets Baixa/Média/Alta.
