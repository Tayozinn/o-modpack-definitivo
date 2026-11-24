// Receitas de Progressão Murim & Subjugação - O Modpack Definitivo (Fantasia Urbana 2010)
ServerEvents.recipes(event => {
  // Pílula de Refinamento Corporal
  event.shaped('mythic_contracts:body_refinement_pill', [
    ' B ',
    'GMG',
    ' I '
  ], {
    B: 'minecraft:blaze_powder',
    G: 'minecraft:gold_ingot',
    M: 'minecraft:magma_cream',
    I: 'minecraft:iron_block'
  }).id('mythic_contracts:body_refinement_pill');

  // Pílula de Condensação Espiritual
  event.shaped('mythic_contracts:spirit_condensation_pill', [
    ' E ',
    'LAL',
    ' P '
  ], {
    E: 'minecraft:ender_pearl',
    L: 'minecraft:lapis_lazuli',
    A: 'minecraft:amethyst_shard',
    P: 'minecraft:prismarine_crystals'
  }).id('mythic_contracts:spirit_condensation_pill');

  // Tomo de Quebra de Reino (Tribulação)
  event.shaped('mythic_contracts:tribulation_tome', [
    'DSD',
    'EBE',
    'ONO'
  ], {
    D: 'minecraft:diamond',
    S: 'minecraft:echo_shard',
    E: 'minecraft:ender_eye',
    B: 'minecraft:book',
    O: 'minecraft:obsidian',
    N: 'minecraft:nether_quartz_block'
  }).id('mythic_contracts:tribulation_tome');

  // Selo de Subjugação
  event.shaped('mythic_contracts:subjugation_seal', [
    'ANA',
    'CEC',
    'ANA'
  ], {
    A: 'minecraft:amethyst_shard',
    N: 'minecraft:netherite_scrap',
    C: 'minecraft:chain',
    E: 'minecraft:ender_eye'
  }).id('mythic_contracts:subjugation_seal');
});
