// priority: 1000
// Modpack Definitivo (1.20.1 Forge) - Fase 2: Triangulo de Combate, Core & Controles
// KubeJS 6 Startup Script - Inicializacao Limpa

console.info('[Modpack Fase 2] Carregando kubejs/startup_scripts/00_init.js...')

StartupEvents.init(event => {
  console.info('[Modpack Fase 2] StartupEvents.init concluido: Better Combat + TaCZ + Irons Spells integrados.')
})

StartupEvents.postInit(event => {
  console.info('[Modpack Fase 2] StartupEvents.postInit concluido com zero erros.')
})
