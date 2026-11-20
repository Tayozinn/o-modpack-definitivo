# Worldgen: Estética Urbana "2010 Pós-Colapso Divino" (The Lost Cities 1.20.1)

Este documento detalha a arquitetura real de configuração do mod **The Lost Cities** no **Minecraft 1.20.1 (Forge 47.2.x / 47.3.x)**, validada diretamente contra o código-fonte oficial do repositório `McJtyMods/LostCities` (branch `1.20`), e explica cada parâmetro aplicado para construir o cenário de **Fantasia Urbana de 2010 — 15 anos após a Guerra dos Deuses Mitológicos**.

---

## 1. Arquitetura Real de Arquivos no Forge 1.20.1

De acordo com o construtor da classe principal `mcjty.lostcities.LostCities` e o gerenciador de perfis `mcjty.lostcities.config.ProfileSetup` na branch `1.20`:

1. **`config/lostcities/profiles/pos_guerra_2010.json`** *(Perfil Customizado Persistente)*
   - **Por que um perfil novo em vez de editar `rarecities.json`?**  
     Em `ProfileSetup.setupProfiles()`, toda vez que o jogo inicia, o mod executa `initStandardProfiles()` e **sobrescreve** no disco todos os 17 arquivos de perfis padrões (`default.json`, `rarecities.json`, `wasteland.json`, `ancient.json`, etc.) com a flag `"__readonly__": "This profile is read only and cannot be modified! If you want to make a new profile based on this then you can make a copy to a new name"`. Logo em seguida, o método `readProfiles(profileDir)` lê todos os arquivos `.json` presentes em `config/lostcities/profiles/` e os registra em `ProfileSetup.STANDARD_PROFILES`.  
     Portanto, criar `config/lostcities/profiles/pos_guerra_2010.json` com `"public": true` garante que o perfil **nunca seja sobrescrito no boot** e apareça nativamente na lista de perfis públicos da GUI.

2. **`defaultconfigs/lostcities-server.toml`** *(Configuração `ModConfig.Type.SERVER`)*
   - Registrado em `LostCities.java` via `ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_CONFIG)`.
   - Quando um novo mundo é criado (em Singleplayer ou Servidor Dedicado), o Forge copia automaticamente este arquivo para `<pasta_do_mundo>/serverconfig/lostcities-server.toml`.
   - Define `selectedProfile = "pos_guerra_2010"` (substituindo o valor padrão `"<CHECK>"`), além de ativar a proteção de estruturas e vilas adjacentes.

3. **`config/lostcities/common.toml`** *(Configuração `ModConfig.Type.COMMON`)*
   - Registrado em `LostCities.java` via `ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG, "lostcities/common.toml")`.
   - Mapeia `"minecraft:overworld=pos_guerra_2010"` na lista `dimensionsWithProfiles` da seção `[profiles]`, atuando como garantia redundante em `Config.getProfileForDimension(Level.OVERWORLD)` caso um servidor reinicie sem `serverconfig`.

4. **`config/lostcities/client.toml`** *(Configuração `ModConfig.Type.CLIENT`)*
   - Registrado em `LostCities.java` via `ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG, "lostcities/client.toml")`.
   - Mantém a seção `[profiles]` exigida pelo `ForgeConfigSpec` do cliente.

---

## 2. Parâmetros Técnicos e Temáticos (`pos_guerra_2010.json`)

O perfil `pos_guerra_2010` utiliza como fundação o perfil `rarecities` e combina camadas controladas de colapso estrutural, crateras de impacto divino e retomada vegetal de 15 anos.

### 2.1. Distribuição Espacial e Preservação de Biomas (Seção `"cities"` e `"lostcity"`)

| Chave JSON | Categoria | Valor Aplicado | Padrão (`default` / `rarecities`) | Explicação Técnica e Temática |
| :--- | :--- | :--- | :--- | :--- |
| `cityChance` | `cities` | `0.0012` | `0.01` / `0.001` | Probabilidade por chunk de ser o centro de uma cidade (`City.isCityCenter`). Cerca de **8,3x mais raro** que o padrão, evitando metrópoles coladas (protege o TPS/FPS) e preservando vastos biomas naturais entre as cidades para sedes de facções **Murim** e **Templos Divinos**. |
| `cityMinRadius` | `cities` | `60` | `50` / `50` | Raio mínimo da cidade em blocos. Garante que, quando uma cidade rara é gerada, ela tenha porte de centro urbano real e não apenas um cruzamento isolado. |
| `cityMaxRadius` | `cities` | `128` | `128` / `128` | Raio máximo em blocos (16 chunks de diâmetro total). Mantém metrópoles contidas e performáticas. |
| `cityThreshold` | `cities` | `0.2` | `0.2` / `0.2` | Limiar do fator esférico de cidade para transição suave entre subúrbios e natureza. |
| `scatteredChanceMultiplier` | `lostcity` | `0.4` | `1.0` / `1.0` | Reduz em 60% a geração de estruturas isoladas fora das cidades (cabanas/torres avulsas), mantendo os biomas selvagens limpos para geração de estruturas de facções e templos. |

### 2.2. Colapso Estrutural e Crateras da Guerra Divina (Seção `"lostcity"` e `"explosions"`)

| Chave JSON | Categoria | Valor Aplicado | Padrão (`default` / `rarecities`) | Explicação Técnica e Temática |
| :--- | :--- | :--- | :--- | :--- |
| `ruinChance` | `lostcity` | `0.65` | `0.05` / `0.0` | **65% dos edifícios** apresentam colapso estrutural parcial causado pela guerra entre deuses há 15 anos, enquanto 35% permanecem de pé compondo a silhueta urbana de 2010. |
| `ruinMinlevelPercent` | `lostcity` | `0.35` | `0.8` / `0.8` | Altura mínima (35% do prédio) onde o desmoronamento pode começar, preservando os andares térreos e subsolos para combate urbano e exploração. |
| `ruinMaxlevelPercent` | `lostcity` | `0.85` | `1.0` / `1.0` | Altura máxima (85% do prédio) para o início da camada de destruição das ruínas. |
| `explosionChance` | `explosions` | `0.003` | `0.002` / `0.002` | Chance por chunk urbano de conter uma grande cratera de impacto divino (nível médio-alto, 50% acima do `default`, sem destruir a cidade inteira). |
| `explosionMinRadius` / `explosionMaxRadius` | `explosions` | `15` / `32` | `15` / `35` | Raio em blocos das grandes crateras de impacto. Limitado a 32 blocos para evitar gargalos de geração de chunks adjacentes. |
| `miniExplosionChance` | `explosions` | `0.045` | `0.03` / `0.03` | Chance por chunk de pequenas explosões (raios de `4` a `11` blocos entre `Y=62` e `Y=105`), simulando danos colaterais de batalhas mitológicas em fachadas, ruas e telhados. |
| `explosionsInCitiesOnly` | `explosions` | `true` | `true` / `true` | Restringe o epicentro das explosões exclusivamente aos chunks urbanos, impedindo crateras aleatórias nos biomas naturais ao redor. |
| `debrisToNearbyChunkFactor` | `explosions` | `180` | `200` / `200` | Controla o espalhamento de escombros de chunks danificados para chunks vizinhos (valores menores = mais destroços nas ruas próximas às crateras). |

### 2.3. Retomada pela Natureza — 15 Anos Pós-2010 (Seção `"lostcity"`)

| Chave JSON | Categoria | Valor Aplicado | Padrão (`default`) | Explicação Técnica e Temática |
| :--- | :--- | :--- | :--- | :--- |
| `rubbleLayer` | `lostcity` | `true` | `true` | Ativa a camada orgânica de escombros (terra, pedra, areia e folhagens) sobre o asfalto e ruínas. |
| `rubbleDirtScale` | `lostcity` | `2.2` | `3.0` | Escala da camada de terra/detritos (valores menores geram camadas mais espessas). Representa o acúmulo de sedimentos e concreto erodido após 15 anos. |
| `rubbleLeaveScale` | `lostcity` | `3.5` | `6.0` | Escala da camada de folhas sobre os escombros (quase o dobro de densidade em relação ao padrão `6.0`), refletindo uma década e meia sem manutenção humana. |
| `vineChance` | `lostcity` | `0.045` | `0.009` | **5x mais vinhas** nas paredes externas dos prédios que o perfil padrão, criando a estética clássica de arranha-céus de 2010 tomados pela flora. |
| `randomLeafBlockChance` | `lostcity` | `0.08` | `0.1` | Chance de geração de blocos de folhas nas bordas entre edifícios e calçadas. |
| `randomLeafBlockThickness` | `lostcity` | `3` | `2` | Espessura (1 a 8) da vegetação invasora que avança das laterais dos prédios para as ruas. |
| `avoidFoliage` | `lostcity` | `false` | `false` | Mantém árvores e flores ativas nos parques e lotes abertos das metrópoles. |

### 2.4. Arquitetura Urbana de 2010 e Infraestrutura (Seção `"lostcity"`)

| Chave JSON | Categoria | Valor Aplicado | Padrão (`default` / `rarecities`) | Explicação Técnica e Temática |
| :--- | :--- | :--- | :--- | :--- |
| `buildingChance` | `lostcity` | `0.32` | `0.3` / `0.3` | Densidade de prédios dentro do perímetro urbano (32% edifícios, 68% malha viária, praças e parques). |
| `buildingMinFloors` / `buildingMaxFloors` | `lostcity` | `1` / `9` | `0` / `8` | Edifícios variam de 2 a 10 pavimentos acima do solo (térreo + 1 a 9 andares), reproduzindo centros comerciais de 2010 sem o custo extremo de FPS do perfil `tallbuildings` (19 andares). |
| `buildingMaxCellars` | `lostcity` | `3` | `3` / `3` | Até 3 níveis subterrâneos (garagens, porões e abrigos pós-guerra). |
| `generateLighting` | `lostcity` | `false` | `false` / `false` | Rede elétrica colapsada desde a Guerra dos Deuses: interiores escuros exigem lanternas/visão noturna e aumentam a tensão da exploração. |
| `highwayRequiresTwoCities` | `lostcity` | `false` | `true` / `false` | Herdado do `rarecities`: permite que rodovias intermunicipais sejam geradas a partir de uma única metrópole, mesmo quando a outra ponta está muito distante. |
| `railwaysCanEnd` | `lostcity` | `true` | `false` / `true` | Herdado do `rarecities`: permite que linhas de metrô e trem terminem em estações finais quando não há outra cidade próxima. |
| `streetGenerationMode` | `lostcity` | `"HIERARCHICAL_GRID_V1"` | `"HIERARCHICAL_GRID_V1"` | Malha viária hierárquica moderna (avenidas primárias, ruas secundárias e vielas terciárias). |
| `highwayGenerationMode` | `lostcity` | `"INTERCITY_NETWORK_V1"` | `"INTERCITY_NETWORK_V1"` | Planejamento determinístico de rodovias conectando hubs urbanos. |

---

## 3. Proteção de Estruturas e Seleção Automática (`defaultconfigs/lostcities-server.toml` e `config/lostcities/common.toml`)

No arquivo `defaultconfigs/lostcities-server.toml` (seção `[profiles]`):

- **`selectedProfile = "pos_guerra_2010"`**:  
  No código de `Config.getProfileForDimension(ResourceKey<Level> type)`, quando `selectedProfile` possui o valor padrão `"<CHECK>"`, o mod depende exclusivamente de o jogador ter clicado no botão da GUI antes de criar o mundo (`Config.profileFromClient`). Ao fixar `selectedProfile = "pos_guerra_2010"` em `defaultconfigs/lostcities-server.toml`, **qualquer mundo novo criado em Singleplayer ou Servidor Dedicado aplica automaticamente o perfil `pos_guerra_2010` no Overworld**, mesmo que o jogador não abra o menu do The Lost Cities.
- **`avoidStructuresAdjacent = true` e `avoidVillagesAdjacent = true`**:  
  Além de evitar gerar cidades no mesmo chunk de vilas e estruturas vanilla (`avoidVillages = true`, `avoidStructures = [...]`), bloqueia a geração urbana nos **chunks imediatamente adjacentes**, somado a `avoidFlattening = true` para impedir que o nivelamento de terreno das cidades corte templos ou vilarejos ao meio.

No arquivo `config/lostcities/common.toml` (seção `[profiles]`):

- **`dimensionsWithProfiles = ["lostcities:lostcity=biosphere", "lostworlds:abyss=biosphere_caves", "minecraft:overworld=pos_guerra_2010"]`**:  
  Associa diretamente a dimensão `minecraft:overworld` ao perfil `pos_guerra_2010` no cache global de dimensões (`dimensionProfileCache`), garantindo que o perfil seja carregado mesmo em ambientes de servidor onde o `serverconfig` seja recriado do zero.

---

## 4. Passo a Passo: Criação de Mundo no Minecraft 1.20.1

### 4.1. Modo Automático (Recomendado — Singleplayer e Server)
Graças à configuração prévia em `defaultconfigs/lostcities-server.toml` (`selectedProfile = "pos_guerra_2010"`) e `config/lostcities/common.toml`:
1. Abra o Minecraft 1.20.1, clique em **Um Jogador (Singleplayer)** -> **Criar Novo Mundo (Create New World)**.
2. Escolha o nome do mundo e o modo de jogo e clique diretamente em **Criar Novo Mundo**.
3. O Forge copiará automaticamente `defaultconfigs/lostcities-server.toml` para a pasta `serverconfig/` do novo mundo e o **The Lost Cities** gerará o Overworld com o perfil **`pos_guerra_2010`**.

### 4.2. Verificação ou Seleção Manual na Tela de Criação de Mundo (GUI 1.20.1)
Caso deseje visualizar o mapa de preview das cidades ou confirmar o perfil na interface gráfica antes de criar o mundo:
1. Na tela **Criar Novo Mundo (Create New World)** do Minecraft 1.20.1, clique na terceira aba superior: **Mais (More)** (`CreateWorldScreen.MoreTab`).
2. No **canto superior direito** da tela (acima do ícone gráfico do The Lost Cities, conforme implementado em `ClientEventHandlers.java`), clique no botão **"Cities"**.
3. Na tela **Lost City Configuration** (`GuiLCConfig`):
   - Clique no primeiro botão no topo à esquerda (**Profile**) até exibir **`pos_guerra_2010`**.
   - O painel direito mostrará o preview em tempo real da raridade das metrópoles (`cityChance: 0.0012`) cercadas por amplas áreas verdes de biomas naturais.
   - *(Opcional)* Clique nas abas `Cities`, `Buildings`, `Damage` ou `Transport` após clicar em `Customize` caso queira inspecionar os valores em tela, ou mantenha `pos_guerra_2010` selecionado.
4. Clique em **Done** (canto inferior esquerdo) e depois em **Criar Novo Mundo**.
