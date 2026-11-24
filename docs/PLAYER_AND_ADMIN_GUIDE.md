# Guia do Jogador e do Administrador — Mythic Contracts 2.0

Guia completo para jogar, administrar servidores e expandir conteúdo via datapacks/KubeJS no **Mythic Contracts 2.0.0** (**Minecraft 1.20.1 / Forge 47.3.0**).

---

## Parte I — Guia do Jogador

### 1. Controles Principais (Preset Pós-Colapso 2010)
Os controles padrão já vêm configurados em [`defaultconfigs/options.txt`](../defaultconfigs/options.txt) para conviver sem conflitos entre os 4 sistemas:

| Tecla | Sistema | Ação |
| :---: | :--- | :--- |
| **`V`** | **Mythic Contracts** | Ativar/desativar **Infusão de Mana/Qi** (perfura Pressão de Reino, amplifica dano e suprime Pulsos de Revolta automaticamente). |
| **`G`** | **Mythic Contracts** | Conjurar a habilidade selecionada (**`H1–H4`**) do **Resquício Divino** equipado (consome Resistência Divina; Fase IV consome `65%` da barra máxima). |
| **`SHIFT + G`** | **Mythic Contracts** | Alternar entre as fases desbloqueadas (**`H1 → H4`**) do **Resquício Divino** equipado (também selecionável em `[U]` → `Habilidades`). |
| **`ALT + G`** | **Mythic Contracts** | Executar a **Técnica de Cultivo** do slot marcial ativo (ou suprimir manualmente o **Pulso de Revolta** na Disputa de Subjugação). |
| **`ALT + V`** | **Mythic Contracts** | Alternar entre os **3 Slots Marciais** (`S1 → S2 → S3`, liberados em `R1`, `R3` e `R5`). |
| **`U`** | **Mythic Contracts** | Abrir a **Interface do Cultivador** (Meridianos, Cultivo, Atributos, Habilidades Master-Detail, Contratos & Servos, Facção). |
| **`X` / `Y` / `Alt+Scroll`** | Iron's Spells | Conjurar feitiço ativo (`X`), Roda Radial (`Y`), alternar feitiço (`Alt + Scroll`). |
| **`R` / `B` / `C` / `Z` / `M`** | TaCZ | Recarregar (`R`), modo de tiro (`B`), deitar (`C`), attachments (`Z`), coronhada/zoom (`M`). |
| **`K` / `N`** | Better Combat | Finta/cancelamento melee (`K`) e alternar mineração com armas (`N`). |
| **`I` / `J` / `P` / `L`** | Utilidade | Curios (`I`), FTB Quests (`J`), FTB Teams (`P`), Advancements (`L`). |

---

### 2. Os 4 Sistemas Vitais + Resistência Divina
1. **Vida Corporal (`Corpo R1–R7`):** escala sua Vida Máxima de `20 HP` até `144 HP` (`+0.5` até `+31.5` de dano base passivo), preservando a proporção atual de vida em evoluções e contratos.
2. **Integridade Espiritual (`Espírito R1–R7`):** escudo espiritual (`100` a `424+`) que absorve `30%` de dano arcano híbrido e `50%` de dano espiritual puro. Recupera continuamente conforme a curva quadrática `R1–R7` (`3.10/s` a `22.78/s` + `0.65/s` por etapa) em **3 estados**: `0.40x` em combate (`100t`), `1.00x` fora de combate e `2.20x` em meditação (`40t` agachado parado ou *Respiração do Lótus Interior*), com feedback discreto na HUD (micro-brilho `#E9D5FF`, `↑`, flash `180ms` `#F43F5E` e pulso `350ms` ao atingir `100%`).
   - **Colapso Espiritual:** se zerar, bloqueia regeneração espiritual (`0x`), `[V]` Infusão, `[G]` Resquícios e avanço de cultivo por **`20s`** (`400` ticks), amplificando dano arcano (`1.25x`) e espiritual (`1.35x`) na Vida Corporal até estabilizar em `25%`.
3. **Qi (`Infusão [V]`):** capacidade de `100` a `390+` (aumentada por contratos como `veil_resonance_oath`). Com `[V]` ligado:
   - **Corpo a Corpo (Vanilla / Better Combat):** `base + (base * bodyMult) + spiritFlat`. Armas únicas/rúnicas do **Simply Swords** exibem `[Ressonante]` e consomem **`-15%` de Qi**. Golpes em varredura no mesmo tick consomem `0.35x` Qi nos alvos secundários.
   - **Armas de Fogo (`TaCZ` — `[Balística]`):** escalas `0.35x` Corpo / `0.25x` Espírito / `0.30x` Qi sem drenar Vigor melee.
   - **Magias (`Iron's Spells` — `[Arcano]`):** escala espiritual `0.65x` com custo `0.50x` Qi (`0.25x` em AoE no mesmo tick).
4. **Vigor / Stamina Física:** escala com Cultivo Corporal (`100` a `260+`); usado ao correr (`sprint`) e em ataques corpo a corpo (com debounce de `3` ticks para varreduras do Better Combat). Nunca aciona a Sobrecarga Divina.
5. **Resistência Divina (Emblema Circular `[G]` com 4 Arcos Externos):** recurso exclusivo dos Resquícios Divinos (`100` a `540+`), exibindo ao redor do emblema os 4 arcos segmentados das Fases I–IV e a pill ativa (`H1`–`H4`). O custo base cai de `60` (R1-1) até `20` (R7-3+); `H2` aplica `+15%`, `H3` aplica `+35%` e `H4` (Ultimate) consome `65%` da barra máxima.
   - **Sobrecarga Divina:** ao zerar a barra, aplica Fraqueza I e sela `[G]` por **`60s`** (`1200` ticks), recuperando `35%` da barra ao estabilizar.

---

### 3. Progressão, Interface `[U]`, Resquícios e Contratos
- **Pílulas e Tomos:**
  - `mythic_contracts:body_refinement_pill` / `spirit_condensation_pill`: avançam etapas (`Base → Fortificação → Despertar`).
  - `mythic_contracts:tribulation_tome`: invoca a Tribulação para romper de Reino (R1 → R7), com clique direito para Corpo e `Shift + Clique Direito` para Espírito (ou pelos botões autoritativos na tela `[U]`).
- **4 Resquícios Divinos Progressivos (`[G]` / `Shift+G`, 4 Fases × 4 Deuses = 16 Habilidades):** independentes de Facção e contidos no início do jogo por pontuação combinada + piso mínimo bilateral (*anti-rush* entre Corpo e Espírito):
  - **Desbloqueio de Fases:** `Fase I — Centelha Selada` (`R1-1`, `0.40x`) → `Fase II — Eco Desperto` (média `R2-3` / piso `R2-1`, `0.75x`, + Passiva) → `Fase III — Ressonância Plena` (média `R4-2` / piso `R3-2`, `1.10x`) → `Fase IV — Encarnação` (média `R6-1` / piso `R5-1`, `1.45x`, Ultimate temporizada).
  - *Olho de Rá* (`eye_of_ra`): `H1` *Faísca Solar* (feixe focado `<=8m`) → `H2` *Égide do Disco Solar* (pulso defletor + Absorção/Regeneração + passiva `-20%` fogo) → `H3` *Julgamento do Olho de Rá* (pilares AoE + cura) → `H4` *Zênite do Sol Imortal* (Ultimate 6s em 3 ondas + restauração espiritual).
  - *Ceifa da Morrígan* (`morrigan_reap`): `H1` *Corte do Presságio* (cone `90°`, `3m` + Wither I) → `H2` *Voo dos Corvos* (enxame linear `10m` + passiva de abate recuperando Vigor/HP) → `H3` *Ceifa Abissal 360°* (varredura plena + dreno vital) → `H4` *Rainha dos Campos de Batalha* (Ultimate 8s com 2 ondas de foice + Wither II / `18%` roubo de vida melee).
  - *Vajra de Indra* (`indras_vajra`): `H1` *Centelha do Vajra* (estalo curto até 2 alvos) → `H2` *Lança Trovejante* (raio perfurante `12m` + salto elétrico + passiva `-10%` recarga) → `H3` *Trovão Soberano do Vajra* (explosão sônica plena anti-Boss `#mythic_contracts:high_realm_boss`) → `H4` *Tempestade de Mil Raios* (Ultimate 5s com 5 relâmpagos priorizando Bosses `+75%` dano + quebra de escudo).
  - *Passo de Tezcatlipoca* (`tezcatlipoca_step`): `H1` *Fenda Fumegante* (esquiva `3.5m` + corte leve) → `H2` *Reflexo do Espelho* (espelho cegante na origem + recuo/Invisibilidade + passiva `-25%` dano de queda) → `H3` *Passo de Tezcatlipoca Pleno* (investida `6.5m` + corte amplo + Velocidade II/Força I) → `H4` *Eclipse do Jaguar Negro* (Ultimate: 3 avanços em zigue-zague com pós-imagens explosivas de obsidiana).
- **12 Técnicas de Cultivo (`R1–R7`, Slots `S1–S3` via `[U]` → `Habilidades`):** compreenda técnicas de Corpo, Espírito e Híbridas, equipe nos slots `S1` (`R1`), `S2` (`R3`) e `S3` (`R5`) e conjure com **`ALT + G`** (`ALT + V` alterna o slot ativo). Contam com coreografias fluidas, ribbons `AFTER_PARTICLES` (preset `Média`/`Alta`), animações de 1ª/3ª pessoa (c/ *playerAnimator* opcional) e `30` efeitos sonoros autorais configuráveis em **`HUD → Visual & Qualidade`**.
- **Habilidade de Subjugação & Disputa Espiritual:** a técnica *Grilhão de Subjugação Espiritual* inicia uma **Disputa de Poder Espiritual** (`Dominância 0% → 100%`, barra efêmera na HUD e supressão de *Pulso de Revolta* com `ALT + G` ou automático via `[V]`), com categorias `Comum` / `Elite` / `Mini-Boss` / `Boss`, Graus de Maestria `I–IV`, limite de `1..5` servos e painel interativo (`Invocar`, `Recolher`, `Postura Seguir/Guardar/Agressivo`, `Libertar`) em `[U] → Contratos`. O item **`Selo de Subjugação` (`subjugation_seal`)** continua funcional no `R1` e atua como **Catalisador Opcional** de Bosses (`-50%` custos, `+10%` limiar de HP, início em `65%` e proteção contra Colapso em falha).
- **HUD Dual & Configuração Visual/Sonora:** clique no botão **`HUD`** no canto superior direito da tela **`[U]`** (ou em Mods → *Mythic Contracts 2.0* → Config) para ajustar âncora, posição, escala, opacidade, modo compacto, partículas (`LOW`/`MEDIUM`/`HIGH`), fallback 2D/3D e volume/toggle dos sons autorais (`Sons Autorais` / `Volume Sons`). O Painel Vital oculta-se automaticamente quando `F3` está aberto.

---

## Parte II — Guia do Administrador & Datapacks

### 1. Comandos (`/contrato` ou alias `/core`)

| Comando | Permissão | Função |
| :--- | :---: | :--- |
| `/contrato status` | Todos | Exibe reinos, etapas, Qi, facção, contratos e resquício do jogador. |
| `/contrato compat` | Todos | Lista todas as integrações detectadas no ambiente atual. |
| `/contrato dar <id>` | OP (`2`) | Concede contrato após validar elegibilidade de facção, reino e reputação. |
| `/contrato remover <id>` | OP (`2`) | Revoga contrato ativo e atualiza modificadores. |
| `/contrato faccao <nome>` | OP (`2`) | Define facção (`none`, `errantes_do_veu`, `juramentados_da_luz`, `irmandade_divida_eterna`, `quebradores_de_tronos`). |
| `/contrato murim <corpo\|espirito> <reino> <etapa>` | OP (`2`) | Ajusta reino (`1–7`) e etapa (`1–3`) e recalcula atributos/HP com segurança. |
| `/contrato resquicio <nome>` | OP (`2`) | Sintoniza `eye_of_ra`, `morrigan_reap`, `indras_vajra` ou `tezcatlipoca_step`. |
| `/contrato reputacao <faccao> <valor>` | OP (`2`) | Define reputação exata (`-10000` a `10000`). |
| `/contrato reputacao <faccao> add <delta>` | OP (`2`) | Soma/subtrai reputação incremental (usado em recompensas do FTB Quests). |
| `/contrato validar` | OP (`2`) | Executa `DatapackValidationService` e reporta erros/avisos de datapacks. |

### 2. Expansão via Datapacks, KubeJS e FTB Quests
- **Datapacks:** sobrescreva ou adicione arquivos em `data/<namespace>/` dentro do diretório `datapacks/` do mundo e execute `/reload` + `/contrato validar`:
  - `realms/{body,spirit}/*.json`, `cultivation/spiritual_integrity_balance.json`, `factions/*.json`, `skills/*.json`, `cultivation_skills/*.json`, `contracts/*.json` (esquema completo em [`DATA_SCHEMA.md`](DATA_SCHEMA.md)), e `tags/{damage_type,entity_types,items}/*.json`.
- **KubeJS 6:** a ponte [`02_mythic_contracts_compat.js`](../kubejs/server_scripts/02_mythic_contracts_compat.js) expõe `global.MythicContracts` (`com.mythiccontracts.integration.MythicContractsAPI`) para ler/ajustar reinos, facção, reputação, contratos, Qi e restaurar vitais em eventos de servidor (`/kubejs reload`).
- **FTB Quests:** o capítulo [`despertar_2010.snbt`](../config/ftbquests/quests/chapters/despertar_2010.snbt) integra `9` missões conectadas aos avanços nativos `mythic_contracts:*` e recompensas via comando OP.
