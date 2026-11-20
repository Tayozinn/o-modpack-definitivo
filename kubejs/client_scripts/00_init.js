// priority: 1000
// Modpack Definitivo (1.20.1 Forge) - Fase 2: Triangulo de Combate, Core & Controles
// KubeJS 6 Client Script - Inicializacao de Cliente & Preset Ergonomico

console.info('[Modpack Fase 2] Carregando kubejs/client_scripts/00_init.js...')

ClientEvents.loggedIn(event => {
  console.info('[Modpack Fase 2] ClientEvents.loggedIn: Cliente sincronizado (Triangulo de Combate & Keybinds prontos).')
})
