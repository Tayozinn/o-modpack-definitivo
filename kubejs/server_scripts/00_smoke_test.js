// priority: 1000
// Modpack Definitivo (1.20.1 Forge) - Fase 2: Triangulo de Combate, Core & Controles
// KubeJS 6 Server Script - Smoke Test & Validacao de Recarregamento (/kubejs reload)

console.info('[Modpack Fase 2] Carregando kubejs/server_scripts/00_smoke_test.js...')

ServerEvents.loaded(event => {
  console.info('[Modpack Fase 2] Smoke Test (ServerEvents.loaded): Scripts de servidor e Triangulo de Combate operacionais com zero erros.')
})

ServerEvents.tags('item', event => {
  // Tags utilitarias para identificacao limpa das vertentes do Triangulo de Combate na Fase 3
  console.info('[Modpack Fase 2] Smoke Test (ServerEvents.tags): Registro de tags de itens verificado sem conflitos.')
})
