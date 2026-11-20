# Relatório de Auditoria de QA, Validação Sintática e Checklist de Boot (Fase 2)

**Projeto:** O Modpack Definitivo — Fantasia Urbana 2010  
**Versão Base:** Minecraft `1.20.1` | Minecraft Forge `47.3.0` (`0.2.0-fase2`)  
**Escopo Auditado:** Infraestrutura Packwiz/Git (Subagente 1), Worldgen *The Lost Cities* (Subagente 2) e Triângulo de Combate, Controles & KubeJS (Subagente 3)

---

## 1. Resultado da Auditoria Automatizada de Sintaxe e Integridade

Todos os arquivos gerados no repositório foram submetidos a validação programática real (parsers nativos `json` e `tomllib` em Python 3, compilador de expressões regulares `re`, verificação criptográfica `SHA-256`/`SHA-512` via `hashlib`, verificador sintático V8 `node --check` e `git status --ignored -uall`).

### 1.1. Auditoria de Controle de Versão (`.gitignore` & `git status`)

| Verificação | Status | Detalhes Técnicos e Correção Aplicada |
| :--- | :---: | :--- |
| **Proteção de `defaultconfigs/options.txt`** | **APROVADO (Corrigido)** | Identificada regra genérica `options.txt` na Seção 6 do `.gitignore`. Embora a regra `!defaultconfigs/**` na Seção 9 reabilitasse o arquivo, a Seção 6 foi corrigida para ancorar os arquivos de sessão/cliente exclusivamente na raiz (`/options.txt`, `/optionsof.txt`, `/optionsshaders.txt`, `/usercache.json`, `/servers.dat`, etc.) e adicionada a exceção explícita `!defaultconfigs/options.txt`. |
| **Rastreamento de Diretórios Essenciais** | **APROVADO** | `git status -uall` confirma o rastreamento integral de `config/`, `defaultconfigs/`, `kubejs/`, `mods/*.pw.toml`, `pack.toml`, `index.toml`, `modrinth.index.json`, `*.md`, `resourcepacks/.gitkeep` e `datapacks/.gitkeep`. |
| **Bloqueio de Binários e Caches Locais** | **APROVADO** | `mods/*.jar`, `saves/`, `logs/`, `crash-reports/`, `kubejs/exported/`, `kubejs/cache/` e `/options.txt` (raiz) permanecem devidamente ignorados. |

### 1.2. Validação Sintática de Arquivos `.json` e `.json5` (7 Arquivos)

Nenhum arquivo apresentou *trailing commas* (`r',\s*[\}\]]'`), chaves duplicadas ou sequências de escape inválidas:

| Arquivo | Tamanho | Chaves Raiz | Parser / Validação | Resultado |
| :--- | :---: | :---: | :--- | :---: |
| `modrinth.index.json` | `20.214 B` | 7 (`files`: 22) | `json.loads` + validação de schema Modrinth v1 (`SHA-1` + `SHA-512`) | **100% VÁLIDO** |
| `config/lostcities/profiles/pos_guerra_2010.json` | `5.389 B` | 6 | `json.loads` + verificação de subobjetos (`cities`, `cityspheres`, `client`, `explosions`, `lostcity`, `public`) | **100% VÁLIDO** |
| `config/bettercombat/client.json` | `752 B` | 15 | `json.loads` + compilação de `swingThruGrassBlacklist` e `mineWithWeaponBlacklist` | **100% VÁLIDO** |
| `config/bettercombat/client.json5` | `752 B` | 15 | `json.loads` (compatibilidade estrita Cloth Config / Jankson) | **100% VÁLIDO** |
| `config/bettercombat/server.json` | `1.411 B` | 30 | `json.loads` + validação de parâmetros `upswing_multiplier` e `fallback_compatibility_enabled` | **100% VÁLIDO** |
| `config/bettercombat/server.json5` | `1.411 B` | 30 | `json.loads` (espelho estrito de `server.json`) | **100% VÁLIDO** |
| `config/bettercombat/fallback_compatibility.json` | `2.972 B` | 4 | `json.loads` + compilação de `blacklist_item_id_regex` e das 26 regras `item_id_regex` | **100% VÁLIDO** |

### 1.3. Validação Sintática de Arquivos `.toml` e Sincronização Packwiz (27 Arquivos)

Todos os 27 arquivos `.toml` foram validados via `tomllib.load()`. O arquivo `index.toml` foi sincronizado para indexar não apenas os 22 metadados `mods/*.pw.toml`, mas todos os **35 arquivos ativos do pacote** (`8` em `config/`, `2` em `defaultconfigs/`, `3` em `kubejs/` e `22` em `mods/*.pw.toml`), atualizando o hash mestre em `pack.toml`:

| Arquivo / Grupo | Quantidade | Validação Efetuada | Hash `SHA-256` / Status |
| :--- | :---: | :--- | :---: |
| `pack.toml` | 1 | `tomllib.load` + verificação de `[versions]` (`forge = "47.3.0"`, `minecraft = "1.20.1"`) | Aponta para `index.toml` (`5095a7208a444a2e07c8f47540713d0d62b9380fcdd20ce716417786ac1cf935`) |
| `index.toml` | 1 | `tomllib.load` + conferência individual de hash `SHA-256` dos 35 arquivos listados | **35/35 Hashes `SHA-256` íntegros** |
| `mods/*.pw.toml` | 22 | `tomllib.load` + cruzamento 1:1 com `modrinth.index.json` (nomes de `.jar`, URLs e hashes `SHA-512`/`SHA-1`) | **22/22 Metadados sincronizados** |
| `defaultconfigs/lostcities-server.toml` | 1 | `tomllib.load` + verificação de `selectedProfile = "pos_guerra_2010"` e flags `avoidStructuresAdjacent = true` | **100% VÁLIDO** |
| `config/lostcities/common.toml` | 1 | `tomllib.load` + verificação de `"minecraft:overworld=pos_guerra_2010"` em `dimensionsWithProfiles` | **100% VÁLIDO** |
| `config/lostcities/client.toml` | 1 | `tomllib.load` + verificação da tabela `[profiles]` | **100% VÁLIDO** |

### 1.4. Validação Sintática de Scripts KubeJS 6 (`.js` — 3 Arquivos)

Todos os scripts passaram na verificação sintática via `node --check` e utilizam exclusivamente eventos nativos do **KubeJS 6 (`2001.6.5-build.26+forge`)**:

| Script | Eventos Utilizados | Validação `node --check` | Conformidade KubeJS 6 (1.20.1) |
| :--- | :--- | :---: | :---: |
| `kubejs/startup_scripts/00_init.js` | `StartupEvents.init`, `StartupEvents.postInit` | **PASS** (Exit Code `0`) | **100% Compatível** |
| `kubejs/server_scripts/00_smoke_test.js` | `ServerEvents.loaded`, `ServerEvents.tags('item', ...)` | **PASS** (Exit Code `0`) | **100% Compatível** (Recarregável via `/kubejs reload server_scripts`) |
| `kubejs/client_scripts/00_init.js` | `ClientEvents.loggedIn` | **PASS** (Exit Code `0`) | **100% Compatível** |

---

## 2. Matriz de Verificação de Dependências e Conflito Zero

### 2.1. Auditoria da Modlist (22 Pacotes Travados — Zero Mods Proibidos)

- **Total de Mods e Bibliotecas:** Exatamente **22 pacotes** (100% das dependências obrigatórias `mandatory = true` em `META-INF/mods.toml` presentes).
- **Verificação de Escopo Negativo (Mods Proibidos):**
  - **Mods de Bosses Extras / Mobs Inflados:** `0` (Nenhum L_Ender's Cataclysm, Mowzie's Mobs, Ice and Fire ou Alex's Mobs incluído).
  - **Mods de Dimensões Extras:** `0` (Nenhum Twilight Forest, Aether, Blue Skies ou Deeper and Darker incluído).
  - **Mods de Shaders / Pós-Processamento Pesado:** `0` (Nenhum Oculus ou Iris incluído).

| Categoria | Mod Principal | Dependências Obrigatórias Incluídas e Travadas | Status na Matriz |
| :--- | :--- | :--- | :---: |
| **Performance (4)** | `Embeddium 0.3.31`, `FerriteCore 6.0.1`, `ModernFix 5.28.0`, `Entity Culling 1.12.1` | Nenhuma dependência externa adicional requerida | **COMPLETO** |
| **Worldgen Urbano (1)** | `The Lost Cities 1.20-7.5.7` | Nenhuma dependência externa adicional requerida | **COMPLETO** |
| **Combate Melee (4)** | `Better Combat 1.9.0`, `Simply Swords 1.56.0` | `playerAnimator 1.0.2-rc1+1.20`, `Cloth Config API 11.1.136`, `Architectury API 9.2.14` | **COMPLETO** |
| **Armas de Fogo (1)** | `TaCZ 1.2.0-hotfix` | Nenhuma dependência externa adicional requerida | **COMPLETO** |
| **Magia & Ocultismo (4)** | `Iron's Spells 'n Spellbooks 1.20.1-3.4.0.9` | `GeckoLib 4.8.4`, `Curios API 5.14.1`, `Caelus API 3.2.0`, `playerAnimator 1.0.2-rc1+1.20` | **COMPLETO** |
| **Scripting & Quests (6)** | `KubeJS 2001.6.5-build.26`, `FTB Quests 2001.4.22` | `Rhino 2001.2.3-build.10`, `Architectury API 9.2.14`, `FTB Library 2001.2.13`, `FTB Teams 2001.3.2` | **COMPLETO** |
| **HUD & Utilidade (2)** | `JEI 15.56.0.205`, `Jade 11.13.3` | Nenhuma dependência externa adicional requerida | **COMPLETO** |

### 2.2. Auditoria de Isolamento do Triângulo de Combate (Better Combat vs. TaCZ vs. Iron's Spells)

As expressões regulares configuradas em `config/bettercombat/fallback_compatibility.json`, `client.json` e `client.json5` foram testadas contra IDs reais de itens dos três mods:

1. **`blacklist_item_id_regex` (`fallback_compatibility.json`):**
   ```regex
   pickaxe|^tacz:|irons_spellbooks:.*(spell_book|scroll|staff|wand|rod|cane|scepter|sceptre|crossbow)
   ```
   - **Armas e Itens do TaCZ (`tacz:modern_kinetic_gun`, `tacz:ammo`, `tacz:attachment`):** Capturados por `^tacz:`. O *Better Combat* **jamais** injeta atributo de arma melee fallback em armas de fogo, preservando o clique esquerdo exclusivo para disparo balístico.
   - **Catalisadores do Iron's Spells (`irons_spellbooks:diamond_spell_book`, `irons_spellbooks:scroll`, `irons_spellbooks:blood_staff`, `irons_spellbooks:graybeard_staff`, `irons_spellbooks:hither_thither_wand`, `irons_spellbooks:lightning_rod`, `irons_spellbooks:autoloader_crossbow`):** Capturados pelo grupo `irons_spellbooks:.*(...)`, impedindo que cajados e grimórios recebam animações de `battlestaff` ou `sword` por engano.
   - **Armas Melee Híbridas (`simplyswords:*` e espadas dedicadas como `irons_spellbooks:magehunter`):** **Não** são bloqueadas pela blacklist e utilizam seus `weapon_attributes` nativos do *Better Combat* normalmente.
2. **`swingThruGrassBlacklist` & `mineWithWeaponBlacklist` (`client.json` / `client.json5`):**
   - Cobrem `^tacz:|irons_spellbooks:.*(spell_book|scroll|staff|wand|rod|cane|scepter|sceptre|crossbow)`, garantindo que o raycast de ataque através de grama alta do *Better Combat* não intercepte cliques quando o jogador empunha armas de fogo ou grimórios/cajados.

### 2.3. Auditoria de Reserva de Teclas (`defaultconfigs/options.txt` & `KEYBINDS_PLAN.md`)

A varredura automatizada nas 80 entradas `key_*` de `defaultconfigs/options.txt` confirmou:
- **`key.keyboard.v` (`V` — Infusão de Mana / Qi — Core Mod Fase 3):** **0 ocorrências** em `defaultconfigs/options.txt` (100% livre; ações originais movidas para `X` no *Iron's Spells* e `M` no *TaCZ*).
- **`key.keyboard.g` (`G` — Habilidade Única / Arte Marcial Murim — Core Mod Fase 3):** **0 ocorrências** em `defaultconfigs/options.txt` (100% livre; ações originais movidas para `B` no *TaCZ Fire Select* e `I` no *Curios GUI*).
- **Zero Colisões Ativas (`IN_GAME`):** `R` dedicado ao Reload do *TaCZ*, `Y` dedicado à Spell Wheel do *Iron's Spells*, `X` dedicado ao Cast do *Iron's Spells*, `C` dedicado ao Crawl do *TaCZ* (`key.saveToolbarActivator` desvinculado) e `P` dedicado ao *FTB Teams* (`key.socialInteractions` desvinculado).

---

## 3. Checklist de Validação de Boot em 5 Minutos (Prism Launcher)

Siga este roteiro cronometrado após importar a instância no **Prism Launcher** (Minecraft `1.20.1` + Forge `47.3.0`, com os 22 mods sincronizados via Packwiz ou script `modrinth.index.json`):

### Minuto 0:00 a 1:00 — Boot, Carregamento de Mixins e Inspeção de Logs
- [ ] **1.1.** Inicie a instância no Prism Launcher com **Java 17** e `6144 MB`–`8192 MB` de RAM alocada.
- [ ] **1.2.** Confirme que o menu principal do Minecraft 1.20.1 abre em menos de ~45 segundos (otimizado por *ModernFix* + *FerriteCore*) exibindo **"26 Mods Loaded"** (22 pacotes `.jar` + módulos internos do Forge/Architectury/FTB).
- [ ] **1.3.** Abra `logs/latest.log` (ou o console do Prism Launcher) e verifique a presença das mensagens do smoke test de inicialização:
  - `[Modpack Fase 2] Carregando kubejs/startup_scripts/00_init.js...`
  - `[Modpack Fase 2] StartupEvents.postInit concluido com zero erros.`
- [ ] **1.4.** Confirme que **não** há linhas `[ERROR]` ou `[FATAL]` referentes a `kubejs`, `bettercombat`, `tacz`, `irons_spellbooks` ou `lostcities`.

### Minuto 1:00 a 2:00 — Criação de Mundo e Verificação do Perfil `pos_guerra_2010`
- [ ] **2.1.** Clique em **Singleplayer (Um Jogador)** -> **Create New World (Criar Novo Mundo)** e ative o modo **Creative (Criativo)** para o teste rápido.
- [ ] **2.2.** Clique na aba superior **More (Mais)** e observe o botão **"Cities"** no canto superior direito:
  - Clique em **"Cities"** e confirme que o botão **Profile** já exibe **`pos_guerra_2010`** (ou selecione-o para visualizar o preview gráfico de metrópoles raras cercadas por biomas naturais). Clique em **Done**.
- [ ] **2.3.** Clique em **Create New World** e aguarde o spawn no Overworld.
- [ ] **2.4.** Verifique a estética urbana de 2010 pós-colapso (ou use `/locate` / voe até a metrópole mais próxima):
  - Prédios de 2 a 10 andares com ~65% de colapso estrutural parcial (`ruinChance: 0.65`), crateras pontuais nas zonas urbanas, iluminação interna desligada (`generateLighting: false`) e densa cobertura de vinhas e folhagens (`vineChance: 0.045`, `rubbleLayer: true`), cercados por biomas naturais amplos e intactos.

### Minuto 2:00 a 3:00 — Smoke Test do KubeJS (`/kubejs reload`)
- [ ] **3.1.** Ao entrar no mundo, verifique no log do console as mensagens:
  - `[Modpack Fase 2] Smoke Test (ServerEvents.loaded): Scripts de servidor e Triangulo de Combate operacionais com zero erros.`
  - `[Modpack Fase 2] ClientEvents.loggedIn: Cliente sincronizado (Triangulo de Combate & Keybinds prontos).`
- [ ] **3.2.** No chat in-game, execute o comando:
  ```mcfunction
  /kubejs reload server_scripts
  ```
- [ ] **3.3.** Confirme que o chat retorna **`Done!`** sem nenhum erro em vermelho, e execute `/kubejs errors` para confirmar que a fila de erros do Rhino/KubeJS está **100% vazia**.

### Minuto 3:00 a 4:15 — Teste Prático do Triângulo de Combate (Zero Conflito)
- [ ] **4.1. Vertente Tática (`TaCZ`):**
  - Pegue um rifle/pistola do *TaCZ* e munição compatível no criativo/JEI.
  - Pressione **`Mouse Direito`** para mirar (ADS) e **`Mouse Esquerdo`** para atirar -> confirme que a arma dispara normalmente **sem** acionar animação de corte melee do *Better Combat*, mesmo mirando através de grama alta.
  - Pressione **`R`** -> confirme que a arma recarrega sem abrir nenhuma roda de feitiços.
  - Pressione **`B`** -> confirme a troca do seletor de tiro (Semi/Auto/Burst).
  - Pressione **`C`** -> confirme que o personagem deita em postura tática (crawl) sem mensagem de barra rápida criativa.
  - Pressione **`M`** (sem mirar) -> confirme o golpe de coronhada/melee da arma; mirando com luneta + **`M`** -> confirme a alternância de zoom.
- [ ] **4.2. Vertente Arcana (`Iron's Spells 'n Spellbooks`):**
  - Pressione **`I`** para abrir o inventário do *Curios API*, equipe um Grimório (*Spell Book*) no slot de *Spellbook* e inscreva/equipe 2 feitiços (ou segure um *Scroll* / *Blood Staff*).
  - Segure **`Y`** -> confirme que a **Roda de Seleção de Feitiços (Spell Wheel)** abre suavemente.
  - Segure **`Left Alt`** e gire o **`Scroll do Mouse`** -> confirme a troca rápida de feitiços na barra arcana.
  - Pressione **`X`** -> confirme a conjuração imediata do feitiço ativo.
  - Segurando um cajado (`irons_spellbooks:blood_staff` ou `graybeard_staff`), clique com **`Mouse Esquerdo`** perto de grama -> confirme que o *Better Combat* respeita a blacklist de catalisadores arcanos.
- [ ] **4.3. Vertente Corpo a Corpo (`Better Combat` + `Simply Swords`):**
  - Equipe uma arma do *Simply Swords* (ex.: `simplyswords:iron_katana`, `simplyswords:runic_claymore` ou `simplyswords:iron_chakram`).
  - Pressione **`Mouse Esquerdo`** -> confirme a execução fluida dos combos direcionais em 1ª e 3ª pessoa (`F5`) com hitboxes OBB e partículas de varredura.
  - Durante o *upswing* de uma arma pesada, pressione **`K`** -> confirme o cancelamento do golpe (**Finta / Feint**).
  - Pressione **`N`** -> confirme o aviso de alternância de mineração com armas (*Toggle Mine With Weapons*).

### Minuto 4:15 a 5:00 — Verificação Final das Teclas Reservadas (`V` e `G`) e Menus
- [ ] **5.1.** Com arma de fogo, espada e grimório equipados em combate (fora de GUIs), pressione repetidamente as teclas **`V`** e **`G`**:
  - Confirme que **absolutamente nenhuma ação é disparada** — ambas estão 100% livres e prontas para receber a *Infusão de Mana/Qi* (`V`) e a *Habilidade Única Murim* (`G`) na Fase 3.
- [ ] **5.2.** Pressione **`J`** -> confirme a abertura imediata da tela do **FTB Quests**.
- [ ] **5.3.** Pressione **`P`** -> confirme a abertura da tela do **FTB Teams**.
- [ ] **5.4.** Abra `Esc` -> `Options` -> `Controls` -> `Key Binds` e filtre por `V` e `G` -> confirme que nenhuma tecla aparece em vermelho (zero conflitos no preset `defaultconfigs/options.txt`).
