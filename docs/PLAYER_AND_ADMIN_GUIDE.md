# Guia do Jogador e do Administrador — Mythic Contracts 2.0

Guia completo para jogar, administrar servidores e expandir conteúdo via datapacks/KubeJS no **Mythic Contracts 2.0.0** (**Minecraft 1.20.1 / Forge 47.3.0**).

---

## Parte I — Guia do Jogador

### 1. Controles Principais (Preset Pós-Colapso 2010)
Os controles padrão já vêm configurados em [`defaultconfigs/options.txt`](../defaultconfigs/options.txt) para conviver sem conflitos entre os 4 sistemas:

| Tecla | Sistema | Ação |
| :---: | :--- | :--- |
| **`V`** | **Mythic Contracts** | Ativar/desativar **Infusão de Mana/Qi** (perfura Pressão de Reino e amplifica dano). |
| **`G`** | **Mythic Contracts** | Canalizar o **Resquício Divino** equipado (consome Resistência Divina). |
| **`U`** | **Mythic Contracts** | Abrir a **Interface do Cultivador** (Meridianos, Cultivo, Atributos, Habilidades, Contratos e Facção). |
| **`X` / `Y` / `Alt+Scroll`** | Iron's Spells | Conjurar feitiço ativo (`X`), Roda Radial (`Y`), alternar feitiço (`Alt + Scroll`). |
| **`R` / `B` / `C` / `Z` / `M`** | TaCZ | Recarregar (`R`), modo de tiro (`B`), deitar (`C`), attachments (`Z`), coronhada/zoom (`M`). |
| **`K` / `N`** | Better Combat | Finta/cancelamento melee (`K`) e alternar mineração com armas (`N`). |
| **`I` / `J` / `P` / `L`** | Utilidade | Curios (`I`), FTB Quests (`J`), FTB Teams (`P`), Advancements (`L`). |

---

### 2. Os 4 Sistemas Vitais + Resistência Divina
1. **Vida Corporal (`Corpo R1–R7`):** escala sua Vida Máxima de `20 HP` até `144 HP` (`+0.5` até `+31.5` de dano base passivo), preservando a proporção atual de vida em evoluções e contratos.
2. **Integridade Espiritual (`Espírito R1–R7`):** escudo espiritual (`100` a `424+`) que absorve `30%` de dano arcano híbrido (incluindo escolas do *Iron's Spells*) e `50%` de dano espiritual puro (`wither`, `sonic_boom`, `eldritch_magic`, bosses de alto reino).
   - **Colapso Espiritual:** se zerar, bloqueia `[V]` Infusão, `[G]` Resquícios e avanço de cultivo por **`20s`** (`400` ticks), amplificando dano arcano (`1.25x`) e espiritual (`1.35x`) na Vida Corporal até estabilizar em `25%`.
3. **Qi (`Infusão [V]`):** capacidade de `100` a `390+` (aumentada por contratos como `veil_resonance_oath`). Com `[V]` ligado:
   - **Corpo a Corpo (Vanilla / Better Combat):** `base + (base * bodyMult) + spiritFlat`. Armas únicas/rúnicas do **Simply Swords** exibem `[Ressonante]` e consomem **`-15%` de Qi**. Golpes em varredura no mesmo tick consomem `0.35x` Qi nos alvos secundários.
   - **Armas de Fogo (`TaCZ` — `[Balística]`):** escalas `0.35x` Corpo / `0.25x` Espírito / `0.30x` Qi sem drenar Vigor melee.
   - **Magias (`Iron's Spells` — `[Arcano]`):** escala espiritual `0.65x` com custo `0.50x` Qi (`0.25x` em AoE no mesmo tick).
4. **Vigor / Stamina Física:** escala com Cultivo Corporal (`100` a `260+`); usado ao correr (`sprint`) e em ataques corpo a corpo (com debounce de `3` ticks para varreduras do Better Combat). Nunca aciona a Sobrecarga Divina.
5. **Resistência Divina (Emblema Circular `[G]`):** recurso exclusivo dos Resquícios Divinos (`100` a `540+`), com custo decrescente de `55` (R1-1) até `20` (R7-3).
   - **Sobrecarga Divina:** ao zerar a barra, aplica Fraqueza I e sela `[G]` por **`60s`** (`1200` ticks), recuperando `35%` da barra ao estabilizar.

---

### 3. Progressão, Interface `[U]`, Resquícios e Contratos
- **Pílulas e Tomos:**
  - `mythic_contracts:body_refinement_pill` / `spirit_condensation_pill`: avançam etapas (`Base → Fortificação → Despertar`).
  - `mythic_contracts:tribulation_tome`: invoca a Tribulação para romper de Reino (R1 → R7), com clique direito para Corpo e `Shift + Clique Direito` para Espírito (ou pelos botões autoritativos na tela `[U]`).
- **4 Resquícios Divinos (`[G]`, independentes de Facção e escalando em Camadas `I–IV`):**
  - *Olho de Rá* (`eye_of_ra`): julgamento solar em área + cura/absorção/resistência ao conjurador.
  - *Ceifa da Morrígan* (`morrigan_reap`): corte abissal + *Wither* e dreno de vida.
  - *Vajra de Indra* (`indras_vajra`): pulso sônico com bônus pesado contra `#mythic_contracts:high_realm_boss` (inclusive *Dead King* do Iron's Spells).
  - *Passo de Tezcatlipoca* (`tezcatlipoca_step`): avanço rápido sombrio com dano em área e buffs de velocidade/força/invisibilidade.
- **Selo de Subjugação (`subjugation_seal`):** enfraqueça criaturas da tag `#mythic_contracts:subjugatable` (`warden`, `wither`, `iron_golem`, `ravager`, `dead_king`, `citadel_keeper`) para **`<= 15%`** de vida (`<= 25%` para *Quebradores de Tronos*) e sele/invoque guardiões aliados.
- **HUD Dual & Configuração Visual:** clique no botão **`HUD`** no canto superior direito da tela **`[U]`** (ou em Mods → *Mythic Contracts 2.0* → Config) para ajustar âncora, posição, escala, opacidade, modo compacto, partículas (`LOW`/`MEDIUM`/`HIGH`) e fallback 2D/3D do Painel Vital e do Emblema Circular. O Painel Vital oculta-se automaticamente quando `F3` está aberto.

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
  - `realms/{body,spirit}/*.json`, `factions/*.json`, `skills/*.json`, `contracts/*.json` (esquema completo em [`DATA_SCHEMA.md`](DATA_SCHEMA.md)), e `tags/{damage_type,entity_types,items}/*.json`.
- **KubeJS 6:** a ponte [`02_mythic_contracts_compat.js`](../kubejs/server_scripts/02_mythic_contracts_compat.js) expõe `global.MythicContracts` (`com.mythiccontracts.integration.MythicContractsAPI`) para ler/ajustar reinos, facção, reputação, contratos, Qi e restaurar vitais em eventos de servidor (`/kubejs reload`).
- **FTB Quests:** o capítulo [`despertar_2010.snbt`](../config/ftbquests/quests/chapters/despertar_2010.snbt) integra `9` missões conectadas aos avanços nativos `mythic_contracts:*` e recompensas via comando OP.
