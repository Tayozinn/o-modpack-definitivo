# Loot Urbano 2010 & Economia Cruzada (Etapa 3)

> Scripts KubeJS autorais em `kubejs/server_scripts/` com guarda `Item.exists(...)` (100% resilientes caso algum mod opcional seja desativado).

---

## 1. Loot das Cidades Devastadas (`03_urban_2010_loot.js`)

| Tabela de Loot (*The Lost Cities*) | Pool Tático 2010 (*TaCZ*) | Pool Arcano, Murim & Pós-Colapso |
| :--- | :--- | :--- |
| **`lostcities:chests/lostcitychest`** *(Prédios arruinados)* | Munição `.45 ACP`, `12g`, `5.56x45`, `7.62x39`, `.357 Mag`, `tacz:ammo_box` (raro), pólvora, ferro | `arcane_essence`, `common_ink`, `blank_rune` (*Iron's Spells*); `pieceofdarkmetal`, `ethereal_spirit` (*Born in Chaos*); Pílulas de Cultivo (raro) |
| **`lostcities:chests/raildungeonchest`** *(Metrôs e túneis subterrâneos — alto risco)* | Estoque militar pesado (`5.56x45`, `7.62x39`, `.308`, `12g`), `tacz:ammo_box`, granadas `tacz:m67` | Pílulas de Cultivo (`1–2`), `subjugation_seal` e `tribulation_tome` (raros), `cinder_essence`, `epic_ink`, `simplyswords:runic_tablet`, `dark_metal_ingot` |

---

## 2. Receitas de Síntese Cruzada (`04_progression_economy.js`)

As receitas base de [01_mythic_contracts_recipes.js](file:///c:/Users/lealt/OneDrive/%C3%81rea%20de%20Trabalho/o%20modpack%20definitivo/kubejs/server_scripts/01_mythic_contracts_recipes.js) continuam 100% válidas para o Early Game. As novas rotas recompensam caçar ameaças noturnas, explorar ruínas e derrotar bosses:

1. **`2x Pílula de Refinamento Corporal`** (`body_refinement_pill_dark_metal`): usa `born_in_chaos_v1:dark_metal_ingot` + `blaze_powder` + `magma_cream` + `gold_ingot`.
2. **`2x Pílula de Condensação Espiritual`** (`spirit_condensation_pill_arcane`): usa `born_in_chaos_v1:ethereal_spirit` + `irons_spellbooks:arcane_essence` + `amethyst_shard` + `lapis_lazuli`.
3. **`2x Tomo de Quebra de Reino` — Rota BoMD** (`tribulation_tome_anima`): usa `irons_spellbooks:epic_ink` + `bosses_of_mass_destruction:ancient_anima`.
4. **`2x Tomo de Quebra de Reino` — Rota Cataclysm** (`tribulation_tome_cataclysm`): usa `cataclysm:ancient_metal_ingot` + `irons_spellbooks:arcane_essence`.
5. **`2x Selo de Subjugação`** (`subjugation_seal_runic`): usa `simplyswords:runic_tablet` + `born_in_chaos_v1:ethereal_spirit` + correntes + `ender_eye`.
6. **`Estrela da Alma` (`bosses_of_mass_destruction:soul_star`)**: síntese direta com `arcane_essence` + `ethereal_spirit` + `ender_eye` para facilitar localizar o *Night Lich*.
