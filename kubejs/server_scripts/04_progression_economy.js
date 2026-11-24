// priority: 400
// O Modpack Definitivo (1.20.1 Forge) — Etapa 3: Economia & Síntese Cruzada (KubeJS)
// Mantém 100% das receitas base de 01_mythic_contracts_recipes.js intactas e adiciona
// rotas de síntese de alto rendimento conectando Born in Chaos, Iron's Spells,
// Simply Swords, Bosses of Mass Destruction e L_Ender's Cataclysm ao Cultivo Murim.

ServerEvents.recipes(event => {
  const hasAll = ids => ids.every(id => Item.exists(id));

  // 1. Síntese Avançada: 2x Pílula de Refinamento Corporal (com Metal Sombrio de Born in Chaos)
  if (hasAll([
    'mythic_contracts:body_refinement_pill',
    'born_in_chaos_v1:dark_metal_ingot'
  ])) {
    event.shaped(Item.of('mythic_contracts:body_refinement_pill', 2), [
      ' B ',
      'DMD',
      ' G '
    ], {
      B: 'minecraft:blaze_powder',
      D: 'born_in_chaos_v1:dark_metal_ingot',
      M: 'minecraft:magma_cream',
      G: 'minecraft:gold_ingot'
    }).id('mythic_contracts:body_refinement_pill_dark_metal');
  }

  // 2. Síntese Avançada: 2x Pílula de Condensação Espiritual (com Iron's Spells + Espírito Etéreo)
  if (hasAll([
    'mythic_contracts:spirit_condensation_pill',
    'irons_spellbooks:arcane_essence',
    'born_in_chaos_v1:ethereal_spirit'
  ])) {
    event.shaped(Item.of('mythic_contracts:spirit_condensation_pill', 2), [
      ' E ',
      'SAS',
      ' L '
    ], {
      E: 'born_in_chaos_v1:ethereal_spirit',
      S: 'irons_spellbooks:arcane_essence',
      A: 'minecraft:amethyst_shard',
      L: 'minecraft:lapis_lazuli'
    }).id('mythic_contracts:spirit_condensation_pill_arcane');
  }

  // 3. Síntese de Alto Reino: 2x Tomo de Quebra de Reino (com Tinta Épica + Anima Ancestral de BoMD)
  if (hasAll([
    'mythic_contracts:tribulation_tome',
    'irons_spellbooks:epic_ink',
    'bosses_of_mass_destruction:ancient_anima'
  ])) {
    event.shaped(Item.of('mythic_contracts:tribulation_tome', 2), [
      'IAI',
      'EBE',
      'ODO'
    ], {
      I: 'irons_spellbooks:epic_ink',
      A: 'bosses_of_mass_destruction:ancient_anima',
      E: 'minecraft:ender_eye',
      B: 'minecraft:book',
      O: 'minecraft:obsidian',
      D: 'minecraft:diamond'
    }).id('mythic_contracts:tribulation_tome_anima');
  }

  // 4. Síntese de Alto Reino: 2x Tomo de Quebra de Reino (com Metal Ancestral do Cataclysm)
  if (hasAll([
    'mythic_contracts:tribulation_tome',
    'cataclysm:ancient_metal_ingot',
    'irons_spellbooks:arcane_essence'
  ])) {
    event.shaped(Item.of('mythic_contracts:tribulation_tome', 2), [
      'AMA',
      'EBE',
      'ONO'
    ], {
      A: 'irons_spellbooks:arcane_essence',
      M: 'cataclysm:ancient_metal_ingot',
      E: 'minecraft:ender_eye',
      B: 'minecraft:book',
      O: 'minecraft:obsidian',
      N: 'minecraft:nether_quartz_block'
    }).id('mythic_contracts:tribulation_tome_cataclysm');
  }

  // 5. Síntese de Quebrador de Tronos: 2x Selo de Subjugação (com Tábua Rúnica de Simply Swords)
  if (hasAll([
    'mythic_contracts:subjugation_seal',
    'simplyswords:runic_tablet',
    'born_in_chaos_v1:ethereal_spirit'
  ])) {
    event.shaped(Item.of('mythic_contracts:subjugation_seal', 2), [
      'ERE',
      'COC',
      'ERE'
    ], {
      E: 'born_in_chaos_v1:ethereal_spirit',
      R: 'simplyswords:runic_tablet',
      C: 'minecraft:chain',
      O: 'minecraft:ender_eye'
    }).id('mythic_contracts:subjugation_seal_runic');
  }

  // 6. Ponte de Exploração de Bosses: Estrela da Alma (Night Lich - BoMD) via Cultivo/Arcano
  if (hasAll([
    'bosses_of_mass_destruction:soul_star',
    'born_in_chaos_v1:ethereal_spirit',
    'irons_spellbooks:arcane_essence'
  ])) {
    event.shaped('bosses_of_mass_destruction:soul_star', [
      ' A ',
      'SES',
      ' A '
    ], {
      A: 'irons_spellbooks:arcane_essence',
      S: 'born_in_chaos_v1:ethereal_spirit',
      E: 'minecraft:ender_eye'
    }).id('mythic_contracts:soul_star_synthesis');
  }
});
