# O Modpack Definitivo — Fase 2: Infraestrutura, Git e Gerenciamento de Pacote

Documentação técnica da **Fase 2** do modpack autoral de **Fantasia Urbana de 2010** para Minecraft **1.20.1**. Este documento registra o travamento de versões (*version locking*), hashes criptográficos reais verificados via API do Modrinth e CurseForge, matriz de compatibilidade de bibliotecas (`META-INF/mods.toml`) e fluxo de trabalho no **Prism Launcher** com **Packwiz** e **Git**.

---

## 1. Ambiente Base e Loader Travado

| Componente | Versão Travada | Justificativa Técnica |
| :--- | :--- | :--- |
| **Minecraft** | `1.20.1` | Base LTS de modding para a convergência entre *The Lost Cities*, *TaCZ*, *Iron's Spells* e *Better Combat*. |
| **Minecraft Forge** | `47.3.0` | Cumpre simultaneamente o requisito mínimo `[47.3,)` exigido pelo `FTB Library 2001.2.13` e permanece na faixa estável `47.3.x` suportada por todos os 22 pacotes sem exigir migração para `47.4.0`. |
| **Java Runtime** | `Java 17 (64-bit)` | Temurin / GraalVM 17 recomendado com flags G1GC e alocação de `6144 MB` a `8192 MB` de RAM. |
| **Formato de Manifesto** | `packwiz:1.1.0` + `modrinth.index.json` | Metadados declarativos em `pack.toml`, `index.toml` e `mods/*.pw.toml` (hashes `SHA-512` e `SHA-256`) + manifesto `.mrpack` v1. |

---

## 2. Tabela Completa de Mods e Dependências Travadas (22 Pacotes)

> **Política de Zero Inflação:** Nenhum mod fora da especificação de design e de suas dependências obrigatórias (`mandatory = true` em `META-INF/mods.toml`) foi incluído.

### 2.1. Performance e Otimização (4 Mods)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Embeddium** | `embeddium` | `0.3.31+mc1.20.1` | `embeddium-0.3.31+mc1.20.1.jar` | Modrinth: `embeddium` (`sk9rgfiA` / `UTbfe5d1`) | `client` | Motor de renderização de chunks substituto do Sodium para Forge 1.20.1, essencial para manter alto FPS nas metrópoles densas de *The Lost Cities*. |
| **FerriteCore** | `ferritecore` | `6.0.1` | `ferritecore-6.0.1-forge.jar` | Modrinth: `ferrite-core` (`uXXizFIs` / `DG5Fn9Sz`) | `both` | Redução drástica do consumo de memória RAM (BlockStates e modelos 3D de armas/estruturas). |
| **ModernFix** | `modernfix` | `5.28.0+mc1.20.1` | `modernfix-forge-5.28.0+mc1.20.1.jar` | Modrinth: `modernfix` (`nmDcB62a` / `WGwPXD1c`) | `both` | Otimização de tempo de boot, carregamento dinâmico de recursos e correção de vazamentos de memória em 1.20.1. |
| **Entity Culling** | `entityculling` | `1.12.1` | `entityculling-forge-1.12.1-mc1.20.1.jar` | Modrinth: `entityculling` (`NNAgCjsB` / `Udu5Sovd`) | `client` | Raytracing assíncrono para ocultar entidades e BlockEntities atrás de paredes nos edifícios urbanos. |

### 2.2. Geração de Mundo — Fantasia Urbana 2010 (1 Mod)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **The Lost Cities** | `lostcities` | `1.20-7.5.7` | `lostcities-1.20-7.5.7.jar` | Modrinth: `the-lost-cities` (`8VDCUS3b` / `zDRDuewL`) | `both` | Gerador procedural da metrópole urbana (prédios, rodovias, túneis de metrô e subsolos onde ocorrem as incursões arcanas/táticas). |

### 2.3. Combate Corpo a Corpo e Arsenal Urbano/Arcano (4 Mods)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Better Combat** | `bettercombat` | `1.9.0+1.20.1-forge` | `bettercombat-forge-1.9.0+1.20.1.jar` | Modrinth: `better-combat` (`5sy6g3kz` / `rnhiaw3t`) | `both` | Sistema de combate corpo a corpo com animações direcionais, hitboxes reais (OBB) e suporte a dual-wielding. |
| **playerAnimator** | `playeranimator` | `1.0.2-rc1+1.20-forge` | `player-animation-lib-forge-1.0.2-rc1+1.20.jar` | Modrinth: `playeranimator` (`gedNE4y2` / `xe2EVE6q`) | `both` | **Dependência obrigatória compartilhada** entre *Better Combat* e *Iron's Spells 'n Spellbooks* para interpolação de animações do modelo do jogador. |
| **Cloth Config API** | `cloth_config` | `11.1.136+forge` | `cloth-config-11.1.136-forge.jar` | Modrinth: `cloth-config` (`9s6osm5g` / `t8TXrZvZ`) | `both` | **Dependência obrigatória compartilhada** entre *Better Combat* e *Simply Swords* para serialização de configurações e telas de opções. |
| **Simply Swords** *(Opcional Aprovado)* | `simplyswords` | `1.56.0-1.20.1` | `simplyswords-forge-1.56.0-1.20.1.jar` | Modrinth: `simply-swords` (`bK3Ubu9p` / `OFYmngee`) | `both` | Arsenal expandido de lâminas táticas e armas rúnicas com integração nativa (`data/simplyswords/weapon_attributes/`) para *Better Combat*. |

### 2.4. Armas de Fogo Modernas (1 Mod)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **[TaCZ] Timeless and Classics Zero** | `tacz` | `1.2.0-hotfix` | `tacz-forge-1.20.1-1.2.0-hotfix.jar` | Modrinth: `timeless-and-classics-zero` (`SzzJttH8` / `4yAbMXbz`) | `both` | Sistema de armas de fogo contemporâneas (pistolas, submetralhadoras, rifles de assalto e miras ópticas) da era 2010 com balística e recarga animada. |

### 2.5. Sistema de Magia e Ocultismo Urbano (4 Mods)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Iron's Spells 'n Spellbooks** | `irons_spellbooks` | `1.20.1-3.4.0.9` | `irons_spellbooks-1.20.1-3.4.0.9.jar` | Modrinth: `irons-spells-n-spellbooks` (`s4OWxYQQ` / `9dM8iFPD`) | `both` | Grimórios, pergaminhos, escolas de feitiçaria e chefes ocultistas integrados à progressão híbrida com armas de fogo e lâminas. |
| **GeckoLib** | `geckolib` | `4.8.4` | `geckolib-forge-1.20.1-4.8.4.jar` | Modrinth: `geckolib` (`8BmcQJ2H` / `aC5KMoNg`) | `both` | **Dependência obrigatória** de *Iron's Spells* (`[1.20.1:4.7.1.1,)`) para animações 3D de conjuração, entidades mágicas e armaduras. |
| **Curios API** | `curios` | `5.14.1+1.20.1` | `curios-forge-5.14.1+1.20.1.jar` | Modrinth: `curios` (`vvuO3ImH` / `IPQlZkz1`) | `both` | **Dependência obrigatória** de *Iron's Spells* (`[5.4.7+1.20.1,)`) para slots de acessórios, anéis arcanos e grimórios equipáveis (`Spellbook Slot`). |
| **Caelus API** | `caelus` | `3.2.0+1.20.1` | `caelus-forge-3.2.0+1.20.1.jar` | Modrinth: `caelus` (`40FYwb4z` / `mRry0DgY`) | `both` | **Dependência obrigatória** de *Iron's Spells* (`[3.1.0+1.20,)`) para abstração de atributos de voo/planar arcano (Angel Wings / Elytra). |

### 2.6. Integração, Scripting e Quests (6 Mods)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `FileID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **KubeJS** | `kubejs` | `2001.6.5-build.26+forge` | `kubejs-forge-2001.6.5-build.26.jar` | Modrinth: `kubejs` (`umyGl7zF` / `hVR2xUSr`) | `both` | Orquestrador JavaScript para unificar receitas, loot tables urbanas e balanceamento entre *TaCZ*, *Iron's Spells* e *Simply Swords*. |
| **Rhino** | `rhino` | `2001.2.3-build.10+forge` | `rhino-forge-2001.2.3-build.10.jar` | Modrinth: `rhino` (`sk9knFPE` / `uNALdylI`) | `both` | **Dependência obrigatória** do *KubeJS* (interpretador JS para JVM). |
| **Architectury API** | `architectury` | `9.2.14+forge` | `architectury-9.2.14-forge.jar` | Modrinth: `architectury-api` (`lhGA9TYQ` / `1MKTLiiG`) | `both` | **Dependência obrigatória compartilhada** entre *KubeJS*, *FTB Library*, *FTB Teams*, *FTB Quests* e *Simply Swords*. |
| **FTB Quests** | `ftbquests` | `2001.4.22` | `ftb-quests-forge-2001.4.22.jar` | CurseForge: `ftb-quests` (`289412` / `8078538`) | `both` | Árvore de missões narrativas e guia de progressão na Fantasia Urbana 2010. *(Verificado em `mods.toml`: `itemfilters` é `mandatory = false` na `2001.4.22`, evitando inflar a modlist).* |
| **FTB Library** | `ftblibrary` | `2001.2.13` | `ftb-library-forge-2001.2.13.jar` | CurseForge: `ftb-library` (`404465` / `8226927`) | `both` | **Dependência obrigatória** de *FTB Quests* e *FTB Teams* para renderização de interface gráfica. |
| **FTB Teams** | `ftbteams` | `2001.3.2` | `ftb-teams-forge-2001.3.2.jar` | CurseForge: `ftb-teams` (`404468` / `7499810`) | `both` | **Dependência obrigatória** de *FTB Quests* para sincronização de progresso individual e em grupo. |

### 2.7. Utilidade e HUD de Inspeção (2 Mods)

| Nome do Mod | ModID (`mods.toml`) | Versão Exata Travada | Arquivo (`.jar`) | Fonte / Slug (`ProjectID` / `VersionID`) | Lado | Papel no Modpack |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Just Enough Items (JEI)** | `jei` | `15.56.0.205` | `jei-1.20.1-forge-15.56.0.205.jar` | Modrinth: `jei` (`u6dRKJwZ` / `9jqubC9n`) | `both` | Catálogo de receitas e usos de itens, integrado às bancadas de armas do *TaCZ*, forja arcana do *Iron's Spells* e scripts *KubeJS*. |
| **Jade** | `jade` | `11.13.3+forge` | `Jade-1.20.1-Forge-11.13.3.jar` | Modrinth: `jade` (`nvQzSEkH` / `xJQHCmWJ`) | `both` | Tooltip de inspeção rápida no topo da tela (WTHIT/HWYLA) para identificar blocos urbanos, contêineres e entidades. |

---

## 3. Auditoria de Compatibilidade Cruzada (`META-INF/mods.toml`)

Todos os binários `.jar` foram inspecionados diretamente via `META-INF/mods.toml` para validar a resolução de dependências sem conflitos no **Forge 47.3.0**:

1. **Verificação de `Iron's Spells 'n Spellbooks` (`1.20.1-3.4.0.9` vs `3.4.0.11+` / `3.16.x`):**
   - As builds recentes `3.4.0.11` e `3.16.3` alteraram o `META-INF/mods.toml` para exigir `forge = "[47.4.0,)"` (e a `3.16.x` adicionou `irons_lib` e removeu `caelus`).
   - A versão **`1.20.1-3.4.0.9`** foi selecionada e travada por exigir `forge = "[47.0.46,)"`, `geckolib = "[1.20.1:4.7.1.1,)"` (atendido pela `4.8.4`), `curios = "[5.4.7+1.20.1,)"` (atendido pela `5.14.1+1.20.1`), `caelus = "[3.1.0+1.20,)"` (atendido pela `3.2.0+1.20.1`) e `playeranimator = "[1.0.2-rc1+1.20,)"` (atendido pela `1.0.2-rc1+1.20-forge`).
2. **Avaliação Técnica do Opcional `Simply Swords` (`1.56.0-1.20.1`):**
   - **Compatibilidade com Better Combat:** 100% nativa. O jar inclui os arquivos `data/simplyswords/weapon_attributes/*.json` que atribuem posturas, alcances e combos do *Better Combat* para cada classe de arma (katanas, glaives, chakrams, rapiers, claymores).
   - **Sem Inflação de Dependências:** Enquanto as builds `1.70.x` passaram a exigir duas bibliotecas externas novas (`simply-tooltips` e `fzzy-config`), a versão **`1.56.0-1.20.1`** exige em seu `META-INF/mods.toml` **exclusivamente** `architectury = "[5.12.42,)"` e `cloth_config = "[11.1.106,)"` — ambas já obrigatórias para *KubeJS* e *Better Combat*.
3. **Convergência de Bibliotecas Compartilhadas:**
   - `Architectury API 9.2.14+forge` satisfaz simultaneamente: `KubeJS` (`>=9.1.12`), `FTB Library` (`>=9.0.8`), `FTB Teams` (`>=9.1.12`), `FTB Quests` (`>=9.0.8`) e `Simply Swords` (`>=5.12.42`).
   - `playerAnimator 1.0.2-rc1+1.20-forge` satisfaz simultaneamente: `Better Combat 1.9.0` e `Iron's Spells 1.20.1-3.4.0.9`.
   - `Cloth Config API 11.1.136+forge` satisfaz simultaneamente: `Better Combat 1.9.0` e `Simply Swords 1.56.0`.

---

## 4. Instruções Passo a Passo — Importação e Uso no Prism Launcher

O repositório disponibiliza **duas formas oficiais** de uso no **Prism Launcher**:

### Método A: Instância Sincronizada via Packwiz (`pack.toml`) — Recomendado para Desenvolvimento

1. Abra o **Prism Launcher** e clique em **Add Instance** (Adicionar Instância).
2. Selecione **Custom** (Personalizada):
   - **Name:** `O Modpack Definitivo (Fantasia Urbana 2010)`
   - **Version:** `1.20.1`
   - Clique em **OK**.
3. Clique com o botão direito na instância criada → **Edit** → aba **Version**:
   - Clique em **Install Forge** e selecione a versão exata **`47.3.0`**.
4. Na aba **Settings** → marque **Custom commands** (Comandos personalizados) e configure o **Pre-launch command**:
   - Baixe o [`packwiz-installer-bootstrap.jar`](https://github.com/packwiz/packwiz-installer-bootstrap/releases) para a pasta `.minecraft` da instância.
   - Copie os arquivos deste repositório (`pack.toml`, `index.toml`, `mods/*.pw.toml`, `config/`, `defaultconfigs/`, `kubejs/`, `resourcepacks/`, `datapacks/`) para a pasta `.minecraft` da instância (ou clone o repositório diretamente como pasta `.minecraft`).
   - Defina o **Pre-launch command** como:
     ```bash
     "$INST_JAVA" -jar packwiz-installer-bootstrap.jar file:///C:/Users/lealt/OneDrive/%C3%81rea%20de%20Trabalho/o%20modpack%20definitivo/pack.toml
     ```
5. Ao clicar em **Launch**, o `packwiz-installer` verificará os hashes `SHA-512`/`SHA-256` de `index.toml` e `mods/*.pw.toml` e baixará automaticamente os 22 `.jar` exatos para a pasta `mods/`.

### Método B: Download Direto dos `.jar` via Script ou Importação `.mrpack` (`modrinth.index.json`)

Caso deseje popular a pasta `mods/` diretamente a partir do manifesto travado `modrinth.index.json` sem precisar do bootstrap:

1. No PowerShell, dentro do diretório raiz do modpack, execute:
   ```powershell
   $index = Get-Content .\modrinth.index.json -Raw | ConvertFrom-Json
   foreach ($f in $index.files) {
       $dest = Join-Path . $f.path
       if (-not (Test-Path $dest)) {
           Write-Host "Baixando $($f.path)..."
           Invoke-WebRequest -Uri $f.downloads[0] -OutFile $dest -UseBasicParsing
       }
   }
   ```
2. Os arquivos `.jar` baixados em `mods/*.jar` são automaticamente ignorados pelo `.gitignore`, mantendo o repositório Git limpo e versionando apenas `mods/*.pw.toml`, `config/`, `defaultconfigs/` e `kubejs/`.
