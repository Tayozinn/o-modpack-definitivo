// priority: 900
// O Modpack Definitivo (1.20.1 Forge) - Fase 9: Integração KubeJS 6 <-> MythicContractsAPI
// Ponte programática segura para scripts de servidor e tags do Triângulo de Combate.

let MythicAPI = null;
try {
  MythicAPI = Java.loadClass('com.mythiccontracts.integration.MythicContractsAPI');
  global.MythicContracts = MythicAPI;
} catch (err) {
  console.error('[Mythic Contracts] Falha ao carregar MythicContractsAPI no KubeJS: ' + err);
}

ServerEvents.loaded(event => {
  if (!MythicAPI) {
    return;
  }
  const statuses = MythicAPI.getIntegrationStatuses();
  let active = [];
  for (let i = 0; i < statuses.size(); i++) {
    let st = statuses.get(i);
    if (st.loaded()) {
      active.push(st.modId());
    }
  }
  const report = MythicAPI.validateDatapacks();
  console.info('[Mythic Contracts] KubeJS Bridge pronta | Integrações ativas: [' + active.join(', ') + ']');
  console.info(
    '[Mythic Contracts] Datapacks carregados: reinos=' + report.realmCount()
      + ', facções=' + report.factionCount()
      + ', contratos=' + report.contractCount()
      + ', resquícios=' + report.skillCount()
      + ', erros=' + report.errors().size()
      + ', avisos=' + report.warnings().size()
  );
  if (report.hasErrors()) {
    console.warn('[Mythic Contracts] Erros detectados na validação de datapacks! Use /contrato validar no jogo.');
  }
});

ServerEvents.tags('item', event => {
  if (!MythicAPI) {
    return;
  }

  // Triângulo de Combate: Corpo a Corpo (Simply Swords)
  if (MythicAPI.isModIntegrated('simplyswords')) {
    event.add('mythic_contracts:combat_triangle/melee', '#simplyswords:swords');
    event.add('mythic_contracts:combat_triangle/melee', '#simplyswords:uniques');
    event.add('mythic_contracts:combat_triangle/melee', '#simplyswords:runic_gear');
  }

  // Triângulo de Combate: Artes Arcanas (Iron's Spells + Curios)
  if (MythicAPI.isModIntegrated('curios')) {
    event.add('mythic_contracts:combat_triangle/arcane', '#curios:spellbook');
  }
  if (MythicAPI.isModIntegrated('irons_spellbooks')) {
    event.add('mythic_contracts:combat_triangle/arcane', '#irons_spellbooks:school_focus');
  }

  // Triângulo de Combate: Armas de Fogo Táticas (TaCZ)
  if (MythicAPI.isModIntegrated('tacz') && Item.exists('tacz:modern_kinetic_gun')) {
    event.add('mythic_contracts:combat_triangle/firearms', 'tacz:modern_kinetic_gun');
  }
});
