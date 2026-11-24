# Ecossistema de Bosses, Mobs & Ameaças Pós-Colapso (`R1–R7`)

> **Etapa 2 — O Modpack Definitivo (Fantasia Urbana 2010)**  
> Todos os mods adicionados via Modrinth (Packwiz + `modrinth.index.json`), compatíveis com **Forge `47.3.0`**, **Embeddium `0.3.31`**, **GeckoLib `4.8.4`**, **Curios `5.14.1`** e **Cloth Config `11.1.136`**.

---

## 1. Mods Adicionados (6 arquivos — 4 de conteúdo + 2 bibliotecas)

| Mod | Versão | Mod ID | Dependências | Papel no Modpack |
| :--- | :---: | :--- | :--- | :--- |
| **L_Ender's Cataclysm** | `3.31` | `cataclysm` | `lionfishapi 3.0`, `curios` | Bosses mitológicos/apocalípticos do Endgame (`R6–R7`) e guardiões de fortalezas |
| **Lionfish-API** | `3.0` | `lionfishapi` | — | Biblioteca obrigatória do Cataclysm |
| **Bosses of Mass Destruction** | `1.1.2` | `bosses_of_mass_destruction` | `cerbons_api 1.1.0`, `geckolib`, `cloth_config` | 4 Bosses atmosféricos de médio/alto reino (`R4–R6`) |
| **CERBON's API** | `1.1.0` | `cerbons_api` | — | Biblioteca obrigatória do BoMD |
| **Mowzie's Mobs** | `1.8.2` | `mowziesmobs` | `geckolib` | Mini-bosses e entidades mitológicas de combate técnico (`R3–R5`) |
| **Born in Chaos** | `1.7.5` | `born_in_chaos_v1` | `geckolib` | Ameaças sombrias/noturnas pós-guerra que populam as cidades arruinadas (`R1–R5`) |

---

## 2. Escada de Progressão de Ameaças (`R1–R7`)

| Faixa de Reino | Tier de Subjugação | Ameaças Principais | Pressão de Reino (`[V]`) |
| :---: | :---: | :--- | :---: |
| **R1 – R2** *(Despertar Urbano)* | `COMMON` | Zumbis/Esqueletos decrépitos, `dread_hound`, `bone_imp`, `restless_spirit`, `swarmer`, `skeleton_demoman`, `phantom_creeper` (*Born in Chaos*) | Não |
| **R3** *(Caçador de Ruínas)* | `ELITE` | `door_knight`, `skeleton_thrasher`, `bonescaller`, `dark_vortex`, `zombie_bruiser`, `dire_hound_leader` (*Born in Chaos*); `naga`, `bluff`, `umvuthana_raptor` (*Mowzie's*); `deepling_brute`, `elite_draugr`, `the_watcher` (*Cataclysm*) | Não |
| **R4 – R5** *(Mini-Bosses & Guardiões)* | `MINI_BOSS` | `ferrous_wroughtnaut` (*Mowzie's*); `citadel_keeper` (*Iron's Spells*); `nightmare_stalker`, `fallen_chaos_knight`, `sir_pumpkinhead`, `supreme_bonescaller`, `missioner`, `krampus`, `lifestealer` (*Born in Chaos*); `ender_golem`, `ignited_revenant`, `the_prowler`, `kobolediator`, `wadjet`, `aptrgangr`, `clawdian`, `coralssus`, `amethyst_crab` (*Cataclysm*) | Não |
| **R5 – R6** *(Soberanos Pós-Colapso)* | `BOSS` | `warden`, `wither` (Vanilla); `dead_king` (*Iron's Spells*); `umvuthi`, `frostmaw` (*Mowzie's*); `lord_pumpkinhead` (*Born in Chaos*); `lich`, `gauntlet`, `obsidilith`*, `void_blossom`* (*BoMD*) | **Sim (`#high_realm_boss`)** |
| **R6 – R7** *(Panteão em Ruínas — Endgame)* | `BOSS` | `ignis`, `netherite_monstrosity`, `ender_guardian`, `the_harbinger`, `the_leviathan`, `ancient_remnant`, `maledictus`, `scylla` (*Cataclysm*) | **Sim (`#high_realm_boss`)** |

> *\* Nota de design:* `bosses_of_mass_destruction:obsidilith` e `bosses_of_mass_destruction:void_blossom` possuem **Pressão de Reino** (`#high_realm_boss`), mas **não** entram em `#subjugatable` por serem entidades estáticas/monolíticas.

---

## 3. Integrações Aplicadas no Core Mod (`mythic_contracts`)

- **`#mythic_contracts:high_realm_boss`** (`20` entidades): redução de `90%` do dano sem Infusão de Mana `[V]` ativa + classificação automática como `SubjugationTier.BOSS`.
- **`#mythic_contracts:subjugatable`** (`94` entidades): todas marcadas com `"required": false` para zero quebra caso algum mod opcional seja desativado.
- **`#mythic_contracts:low_tier_celestial_fiend`** (`14` entidades): criaturas de baixo escalão de *Born in Chaos* e *Cataclysm* reagem à **Aura de Temor** dos *Quebradores de Tronos*.
- **`SubjugationBalance.java`**: `MINI_BOSS_PATHS` e `ELITE_PATHS` atualizados com os caminhos explícitos dos 4 novos mods.
- **`ModCompatRegistry.java`**: `/contrato compat` agora reporta o carregamento ativo dos 4 novos mods de bosses/ameaças.
