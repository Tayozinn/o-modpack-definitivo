# Plano de Mapeamento de Controles & Ergonomia (`KEYBINDS_PLAN.md`)
**Modpack:** Fantasia Urbana 2010 — Fase 2 (Triângulo de Combate & Core)  
**Target:** Minecraft 1.20.1 (Forge 47.2.x / 47.3.x)  
**Preset Base Gerado:** `defaultconfigs/options.txt`

---

## 1. Filosofia Ergonômica & Arquitetura do Triângulo de Combate

O combate do modpack apoia-se em três pilares simultâneos (**Triângulo de Combate**) somados às mecânicas autorais de cultivo/mana que entrarão na **Fase 3**:
1. **Armas de Fogo Táticas (`TaCZ` — Timeless and Classics Zero)**: exige memória muscular de FPS moderno (`Mouse Esquerdo` para atirar, `Mouse Direito` para mirar/ADS, `R` para recarregar, `B` para seletor de tiro, `C` para postura deitada/crawl, `Z` para bancada rápida de attachments/refit).
2. **Artes Arcanas (`Iron's Spells 'n Spellbooks`)**: exige conjuração instantânea ao redor do `WASD` sem tirar os dedos do movimento (`X` para conjurar feitiço ativo, `Left Alt + Scroll` para rolar a barra de feitiços em combate e `Y` para abrir a Roda Radial de Feitiços).
3. **Combate Corpo a Corpo (`Better Combat` + `Simply Swords`)**: ataques fluidos com combos no `Mouse Esquerdo`, finta/cancelamento de golpe em `K` e alternância de mineração com armas em `N`.
4. **Reservas Intocáveis para o Core Mod (Fase 3)**:
   - **`V` (`key.keyboard.v`)** -> **Infusão de Mana / Qi (Core Mod Fase 3)**
   - **`G` (`key.keyboard.g`)** -> **Habilidade Única / Arte Marcial Murim (Core Mod Fase 3)**

> **Compatibilidade ABNT2 & US ANSI:** Todas as reatribuições utilizam exclusivamente teclas alfabéticas padrão (`A–Z`) e modificadores universais (`ALT`, `CTRL`, `SHIFT`), evitando teclas mortas (dead keys como `` ` `` / `~` / `'`) que sofrem variações de scancode em teclados brasileiros ABNT2.

---

## 2. Diagnóstico de Conflitos Reais (Verificados via Código-Fonte 1.20.1)

A auditoria direta nas classes de registro de teclas (`KeyMapping`) na branch `1.20.1` de cada mod revelou **5 zonas críticas de colisão**:

| Tecla em Conflito | Mods / Ações que Colidem por Padrão no Código-Fonte | Resolução Aplicada |
| :--- | :--- | :--- |
| **`R`** (`key.keyboard.r`) | 1. **TaCZ**: Recarregar Arma (`key.tacz.reload.desc`)<br>2. **Iron's Spells**: Roda de Feitiços (`key.irons_spellbooks.spell_wheel`)<br>3. **JEI**: Mostrar Receita (`key.jei.showRecipe` — contexto `JEI_GUI_HOVER`) | **`R`** mantido para **TaCZ Reload** (padrão universal de FPS).<br>**Iron's Spells Spell Wheel** movido para **`Y`**. *(O JEI opera apenas em contexto `JEI_GUI_HOVER` com inventário aberto, não conflitando in-game).* |
| **`V`** (`key.keyboard.v`) | 1. **[RESERVA FASE 3]**: Infusão de Mana / Qi (Core Mod)<br>2. **Iron's Spells**: Conjurar Feitiço (`key.irons_spellbooks.spellbook_cast`)<br>3. **TaCZ**: Coronhada/Baioneta (`key.tacz.melee.desc`)<br>4. **TaCZ**: Zoom de Luneta (`key.tacz.zoom.desc`) | **`V`** **100% liberado e reservado para o Core Mod (Fase 3)**.<br>**Iron's Spells Cast** movido para **`X`** (acesso imediato do polegar/indicador sob `S`/`D`).<br>**TaCZ Melee** e **TaCZ Zoom** (mutuamente exclusivos no código do TaCZ: melee só sem mirar, zoom só mirando) movidos para **`M`**. |
| **`G`** (`key.keyboard.g`) | 1. **[RESERVA FASE 3]**: Habilidade Única / Arte Marcial Murim (Core Mod)<br>2. **Curios API**: Abrir Inventário Curios (`key.curios.open.desc`)<br>3. **TaCZ**: Seletor de Modo de Tiro (`key.tacz.fire_select.desc` — `GLFW_KEY_G` em `FireSelectKey.java`) | **`G`** **100% liberado e reservado para o Core Mod (Fase 3)**.<br>**TaCZ Fire Select** movido para **`B`** (padrão tático de seletores de tiro).<br>**Curios GUI** movido para **`I`** (mnemônico: **I**nventário de Acessórios). |
| **`C`** (`key.keyboard.c`) | 1. **TaCZ**: Deitar / Crawl (`key.tacz.crawl.desc`)<br>2. **Vanilla**: Salvar Barra Rápida Criativa (`key.saveToolbarActivator`) | **`C`** mantido para **TaCZ Crawl**.<br>Ação criativa Vanilla `key.saveToolbarActivator` desvinculada (`key.unknown`). |
| **`X`** (`key.keyboard.x`) & **`P`** (`key.keyboard.p`) | 1. **Vanilla**: Carregar Barra Rápida Criativa (`key.loadToolbarActivator` em `X`)<br>2. **Vanilla**: Interações Sociais (`key.socialInteractions` em `P`) | Desvinculados (`key.unknown`) para liberar **`X`** para **Iron's Spells Cast** e **`P`** para **FTB Teams GUI** sem alerta vermelho no menu de controles. |

---

## 3. Tabela Completa de Mapeamento de Teclas

| Tecla Final | Ação / Função | Mod / Sistema | ID Interno (`options.txt`) | Tecla Original (Default) | Status / Justificativa Ergonômica |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`V`** | **Infusão de Mana / Qi** | **Core Mod (Fase 3)** | *(Reservado Fase 3)* | *(N/A — Reserva)* | **RESERVADA**: Posição nobre de polegar/indicador ao lado de `C` e `X` para ativação reativa de Qi/Mana em combate. |
| **`G`** | **Habilidade Única / Arte Marcial Murim** | **Core Mod (Fase 3)** | *(Reservado Fase 3)* | *(N/A — Reserva)* | **RESERVADA**: Posição central ao lado de `F` para execução imediata da técnica marcial assinatura na Fase 3. |
| **`Mouse Esq.`** | Atacar (Melee) / Disparar Arma (Shoot) | Vanilla / Better Combat / TaCZ | `key_key.attack` / `key_key.tacz.shoot.desc` | `key.mouse.left` | **Mantida**: Better Combat ignora itens `tacz:*` via blacklist, permitindo disparo sem interferência de swing melee. |
| **`Mouse Dir.`** | Usar Item / Mirar (ADS) | Vanilla / TaCZ | `key_key.use` / `key_key.tacz.aim.desc` | `key.mouse.right` | **Mantida**: Mira precisa (ADS) com armas TaCZ e uso normal de blocos/escudos/pergaminhos. |
| **`R`** | Recarregar Arma (Reload) | TaCZ | `key_key.tacz.reload.desc` | `key.keyboard.r` (`R`) | **Mantida**: Memória muscular universal de FPS; conflito com Iron's Spells resolvido movendo a Spell Wheel para `Y`. |
| **`ALT + R`** | Descarregar Munição (Unload) | TaCZ | `key_key.tacz.unload.desc` | `key.keyboard.r:ALT` | **Mantida**: Modificador `ALT` evita descarga acidental no meio do tiroteio. |
| **`B`** | Seletor de Modo de Tiro (Fire Select) | TaCZ | `key_key.tacz.fire_select.desc` | `key.keyboard.g` (`G`) | **Reatribuída (`G` -> `B`)**: Libera `G` para o Core Mod (Fase 3) e adota o padrão clássico de FPS tático (`B`). |
| **`H`** | Inspecionar Arma (Inspect) | TaCZ | `key_key.tacz.inspect.desc` | `key.keyboard.h` (`H`) | **Mantida**: Tecla lateral livre para animação estética/verificação da arma. |
| **`O`** | Interagir com Arma em Mãos (Interact) | TaCZ | `key_key.tacz.interact.desc` | `key.keyboard.o` (`O`) | **Mantida**: Permite abrir portas/baús enquanto empunha armas TaCZ sem acionar a mira. |
| **`Z`** | Bancada Rápida / Attachments (Refit) | TaCZ | `key_key.tacz.refit.desc` | `key.keyboard.z` (`Z`) | **Mantida**: Acesso rápido no canto inferior esquerdo para customização de miras, canos e pentes. |
| **`C`** | Deitar / Postura Tática (Crawl) | TaCZ | `key_key.tacz.crawl.desc` | `key.keyboard.c` (`C`) | **Mantida**: Desvinculado `key.saveToolbarActivator` do Vanilla para eliminar colisão na tecla `C`. |
| **`M`** | Coronhada (Sem Mirar) / Zoom de Luneta (Mirando) | TaCZ | `key_key.tacz.melee.desc` / `key_key.tacz.zoom.desc` | `key.keyboard.v` (`V`) | **Reatribuída (`V` -> `M`)**: Libera `V` para o Core Mod (Fase 3). No código do TaCZ, Melee (`!isAim()`) e Zoom (`isAim()`) são estados mutuamente exclusivos. |
| **`ALT + T`** | Abrir Configuração do TaCZ | TaCZ | `key_key.tacz.open_config.desc` | `key.keyboard.t:ALT` | **Mantida**: Atalho administrativo com modificador `ALT`. |
| **`X`** | **Conjurar Feitiço Ativo (Cast Spell)** | Iron's Spells 'n Spellbooks | `key_key.irons_spellbooks.spellbook_cast` | `key.keyboard.v` (`V`) | **Reatribuída (`V` -> `X`)**: Libera `V` para o Core Mod (Fase 3) mantendo a conjuração na fileira inferior imediata sob `S`/`D`. |
| **`Y`** | **Roda de Seleção de Feitiços (Spell Wheel)** | Iron's Spells 'n Spellbooks | `key_key.irons_spellbooks.spell_wheel` | `key.keyboard.r` (`R`) | **Reatribuída (`R` -> `Y`)**: Resolve colisão crítica com o Reload do TaCZ (`R`) usando tecla alfabética 100% compatível com ABNT2 e US. |
| **`Left Alt`** | Modificador de Rolagem de Feitiços (`Alt + Scroll`) | Iron's Spells 'n Spellbooks | `key_key.irons_spellbooks.spell_bar_modifier` | `key.keyboard.left.alt` | **Mantida**: Permite trocar de feitiço instantaneamente com `Alt + Scroll do Mouse` sem abrir a roda radial. |
| **`K`** | Finta / Cancelar Ataque Melee (Feint) | Better Combat | `key_keybinds.bettercombat.feint` | `key.unknown` | **Atribuída (`Unbound` -> `K`)**: Tecla dedicada e livre de conflitos para cancelamento de upswing melee. |
| **`N`** | Alternar Mineração com Armas | Better Combat | `key_keybinds.bettercombat.toggle_mine_with_weapons` | `key.unknown` | **Atribuída (`Unbound` -> `N`)**: Permite ativar/desativar quebra de blocos com espadas rapidamente. |
| **`I`** | Abrir Inventário de Acessórios & Grimório | Curios API | `key_key.curios.open.desc` | `key.keyboard.g` (`G`) | **Reatribuída (`G` -> `I`)**: Libera `G` para o Core Mod (Fase 3); mnemônico intuitivo (**I**nventário Curios). |
| **`J`** | Abrir Livro/Mapa de Missões (Quests) | FTB Quests | `key_key.ftbquests.quests` | `key.unknown` | **Atribuída (`Unbound` -> `J`)**: Acesso direto às Quests (**J**ornada) sem precisar abrir o inventário. |
| **`P`** | Abrir Menu de Equipe (FTB Teams) | FTB Teams | `key_key.ftbteams.open_gui` | `key.unknown` | **Atribuída (`Unbound` -> `P`)**: Mnemônico para **P**arty/Equipe (`key.socialInteractions` do Vanilla desvinculado). |
| **`R`** *(Em GUI)* | Mostrar Receita do Item Sob o Cursor | JEI | `key_key.jei.showRecipe` | `key.keyboard.r` (`R`) | **Mantida**: Contexto exclusivo `JEI_GUI_HOVER` (só dispara dentro de interfaces gráficas). |
| **`U`** *(Em GUI)* | Mostrar Usos do Item Sob o Cursor | JEI | `key_key.jei.showUses` | `key.keyboard.u` (`U`) | **Mantida**: Contexto exclusivo `JEI_GUI_HOVER`. |
| **`A`** *(Em GUI)* | Favoritar Item (Bookmark) | JEI | `key_key.jei.bookmark` | `key.keyboard.a` (`A`) | **Mantida**: Contexto exclusivo `JEI_GUI_HOVER`. |
| **`CTRL + O`** *(Em GUI)* | Mostrar/Ocultar Overlay do JEI | JEI | `key_key.jei.toggleOverlay` | `key.keyboard.o:CONTROL` | **Mantida**: Contexto exclusivo `GUI`. |
| **`CTRL + F`** *(Em GUI)* | Focar Barra de Pesquisa do JEI | JEI | `key_key.jei.focusSearch` | `key.keyboard.f:CONTROL` | **Mantida**: Contexto exclusivo `GUI`. |
| **`Numpad 0..5`** | Config, Overlay, Líquidos, Receitas, Usos e Narração | Jade | `key_key.jade.*` | `key.keyboard.keypad.0..5` | **Mantidas**: Isoladas no teclado numérico, zero conflito com o cluster `WASD`. |

---

## 4. Layout Visual da Fileira de Combate (`WASD` Cluster)

```text
[Tab: Lista]  [Q: Dropar]   [W: Frente]   [E: Inventário] [R: TaCZ Reload]  [T: Chat]     [Y: Spell Wheel] [U: JEI Usos] [I: Curios GUI] [O: TaCZ Interact] [P: FTB Teams]
[Shift/Ctrl]  [A: Esquerda] [S: Trás]     [D: Direita]    [F: Trocar Mão]   [G: ★MURIM★]  [H: Inspecionar] [J: Quests]   [K: BC Finta]   [L: Conquistas]
              [Z: TaCZ Mod] [X: Conjurar] [C: Deitar]     [V: ★MANA/QI★]    [B: Modo Tiro] [N: BC Minerar]  [M: Coronhada/Zoom]
                            [Left Alt + Scroll: Trocar Feitiço]  [Espaço: Pular]
```
