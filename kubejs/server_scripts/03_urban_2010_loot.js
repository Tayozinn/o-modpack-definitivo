// priority: 500
// O Modpack Definitivo (1.20.1 Forge) — Etapa 3: Loot Urbano 2010 (The Lost Cities)
// Injeta suprimentos táticos (TaCZ), relíquias arcanas (Iron's Spells), cultivo Murim
// (Mythic Contracts), relíquias rúnicas (Simply Swords) e vestígios pós-colapso (Born in Chaos)
// nos baús de prédios arruinados e estações de metrô subterrâneas.

ServerEvents.chestLootTables(event => {
  const hasItem = id => Item.exists(id);

  // =========================================================================
  // 1. PRÉDIOS DEVASTADOS — lostcities:chests/lostcitychest
  // =========================================================================
  event.modify('lostcities:chests/lostcitychest', table => {
    // Pool A: Suprimentos Táticos 2010 (TaCZ + Sobrevivência Urbana)
    table.addPool(pool => {
      pool.rolls = [1, 2];
      pool.addEmpty(35);

      if (hasItem('tacz:ammo')) {
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:45acp"}'), 16, [8, 18]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:12g"}'), 14, [6, 12]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:556x45"}'), 12, [8, 16]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:762x39"}'), 10, [6, 14]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:357mag"}'), 10, [6, 12]);
      }
      if (hasItem('tacz:ammo_box')) {
        pool.addItem('tacz:ammo_box', 4, 1);
      }
      pool.addItem('minecraft:gunpowder', 14, [3, 8]);
      pool.addItem('minecraft:iron_ingot', 12, [2, 5]);
    });

    // Pool B: Relíquias Arcanas, Cultivo Murim & Vestígios Pós-Colapso
    table.addPool(pool => {
      pool.rolls = 1;
      pool.addEmpty(45);

      if (hasItem('irons_spellbooks:arcane_essence')) {
        pool.addItem('irons_spellbooks:arcane_essence', 18, [2, 5]);
      }
      if (hasItem('irons_spellbooks:common_ink')) {
        pool.addItem('irons_spellbooks:common_ink', 12, [1, 2]);
      }
      if (hasItem('irons_spellbooks:blank_rune')) {
        pool.addItem('irons_spellbooks:blank_rune', 6, 1);
      }
      if (hasItem('born_in_chaos_v1:pieceofdarkmetal')) {
        pool.addItem('born_in_chaos_v1:pieceofdarkmetal', 14, [1, 3]);
      }
      if (hasItem('born_in_chaos_v1:ethereal_spirit')) {
        pool.addItem('born_in_chaos_v1:ethereal_spirit', 10, [1, 2]);
      }
      if (hasItem('mythic_contracts:body_refinement_pill')) {
        pool.addItem('mythic_contracts:body_refinement_pill', 6, 1);
      }
      if (hasItem('mythic_contracts:spirit_condensation_pill')) {
        pool.addItem('mythic_contracts:spirit_condensation_pill', 6, 1);
      }
    });
  });

  // =========================================================================
  // 2. METRÔS & TÚNEIS SUBTERRÂNEOS — lostcities:chests/raildungeonchest
  //    (Zona de alto risco -> melhores munições, tintas raras, tomos e selos)
  // =========================================================================
  event.modify('lostcities:chests/raildungeonchest', table => {
    // Pool A: Estoque Militar Subterrâneo (TaCZ pesado)
    table.addPool(pool => {
      pool.rolls = [1, 2];
      pool.addEmpty(20);

      if (hasItem('tacz:ammo')) {
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:556x45"}'), 18, [12, 24]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:762x39"}'), 16, [12, 24]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:308"}'), 12, [8, 16]);
        pool.addItem(Item.of('tacz:ammo', '{AmmoId:"tacz:12g"}'), 12, [8, 16]);
      }
      if (hasItem('tacz:ammo_box')) {
        pool.addItem('tacz:ammo_box', 8, 1);
      }
      if (hasItem('tacz:m67')) {
        pool.addItem('tacz:m67', 6, [1, 2]);
      }
    });

    // Pool B: Cofres de Cultivo, Relíquias Rúnicas & Arcano Avançado
    table.addPool(pool => {
      pool.rolls = 1;
      pool.addEmpty(25);

      if (hasItem('mythic_contracts:body_refinement_pill')) {
        pool.addItem('mythic_contracts:body_refinement_pill', 14, [1, 2]);
      }
      if (hasItem('mythic_contracts:spirit_condensation_pill')) {
        pool.addItem('mythic_contracts:spirit_condensation_pill', 14, [1, 2]);
      }
      if (hasItem('mythic_contracts:subjugation_seal')) {
        pool.addItem('mythic_contracts:subjugation_seal', 6, 1);
      }
      if (hasItem('mythic_contracts:tribulation_tome')) {
        pool.addItem('mythic_contracts:tribulation_tome', 5, 1);
      }
      if (hasItem('irons_spellbooks:cinder_essence')) {
        pool.addItem('irons_spellbooks:cinder_essence', 10, [1, 3]);
      }
      if (hasItem('irons_spellbooks:epic_ink')) {
        pool.addItem('irons_spellbooks:epic_ink', 5, 1);
      }
      if (hasItem('simplyswords:runic_tablet')) {
        pool.addItem('simplyswords:runic_tablet', 6, 1);
      }
      if (hasItem('born_in_chaos_v1:dark_metal_ingot')) {
        pool.addItem('born_in_chaos_v1:dark_metal_ingot', 10, [1, 2]);
      }
    });
  });
});
