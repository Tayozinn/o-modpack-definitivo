# Contratos entre Módulos — Mythic Contracts 2.0

## 1. Autoridade e Fluxo de Dados
1. **Fonte única da verdade:** `ModCapabilities.getData(player)` (`PlayerData` anexado via `CoreDataProvider`).
2. **Servidor autoritativo:** nenhum pacote C2S envia valores calculados (HP, dano, Qi, Integridade, Vigor, custos, tiers). O cliente envia apenas intenções; o servidor valida estado vivo, pré-requisitos, rate-limits e envia atualizações S2C.
3. **Separação Persistência vs Cliente:**
   - `PlayerDataSerializer.writeFull(data)` → disco e backup `Player.PERSISTED_NBT_TAG`.
   - `PlayerDataSerializer.writeClientSync(data)` → `SyncCoreDataS2CPacket` (omite `entitySnapshot` pesado, `signedAtTick`, `violationCount` e `brokenContractsHistory`).

## 2. Contratos de Serviços Centrais

### `AttributeService` (`com.mythiccontracts.attribute`)
- **Entrada:** `ServerPlayer`, `Attribute`, `UUID`, `name` (prefixo `mythic_contracts.`), `amount`, `Operation`.
- **Invariantes:**
  - Nunca altera `AttributeInstance.setBaseValue(...)`.
  - Toda mutação múltipla que envolve `Attributes.MAX_HEALTH` deve rodar dentro de `AttributeService.runPreservingHealthRatio(player, healToFull, action)`.
  - `clearByNamePrefix` varre `TRACKED_ATTRIBUTES` + `player.getAttributes().getSyncableAttributes()`.

### `VitalService` (`com.mythiccontracts.core.service`)
- **Responsabilidade:** Capacidade de Vigor (`maxStamina = 100 + 25*(R_B-1) + 5*(S_B-1)`), consumo em corrida/melee, atraso de recuperação (`staminaRegenDelayTicks`) e regeneração (`10/s` a `22/s`).
- **Invariante:** Nunca consome nem interfere com `SkillData.currentDivineResistance`.

### `SpiritualIntegrityService` (`com.mythiccontracts.core.service`)
- **Responsabilidade:**
  - Capacidade (`100 + 40*(R_S-1) + 12*(S_S-1) + 10*(R_B-1)`), regeneração (`2.0 + 1.0*(R_S-1) + 0.35*(S_S-1)`).
  - Classificação de dano em `NONE` | `HYBRID_ARCANE` (`30%` Integridade / `70%` Vida) | `PURE_SPIRITUAL` (`50%` Integridade / `50%` Vida).
  - Ao zerar: ativa **Colapso Espiritual** (`spiritShatterTicks = 400`), desliga `manaInfusionActive`, bloqueia `[G]` e quebras de reino, amplifica dano arcano (`1.25x`) e espiritual (`1.35x`) na Vida Corporal, e recupera `25%` ao encerrar.

### `EnergyService` (`com.mythiccontracts.core.service`)
- **Responsabilidade:** Orquestra `recalculateMaxQi` (Qi, Vigor, Integridade Espiritual e Resistência Divina), `consumeDivineResistanceOrOverload` (Sobrecarga de `1200` ticks) e `tickAllEnergy(data, isSprinting)`.

### `CultivationService` (`com.mythiccontracts.cultivation.progression`)
- **Responsabilidade:** Resolução de reinos via `RealmRegistry`, execução de `advanceStage` / `attemptBreakthrough`, e aplicação unificada de atributos corporais e espirituais.

## 3. Contratos de Rede (`ModNetworking`, `PROTOCOL_VERSION = "5"`)
- `SyncCoreDataS2CPacket` (`S2C`): login, respawn, troca de dimensão, mudança estrutural (reino, contrato, facção, resquício equipado, subjugados).
- `SyncEnergyS2CPacket` (`S2C`): Qi, Integridade Espiritual (+ `spiritShatterTicks`), Vigor, Resistência Divina (+ `divineOverloadTicks`), `manaInfusionActive`, `uniqueSkillCooldownTicks`. Enviado no máximo a cada `10` ticks quando há mudança, ou imediatamente em transições críticas.
- `SpawnVisualEffectS2CPacket` (`S2C`, via `sendToNear`): envia apenas `effectType`, `tierOrdinal`, origem, direção, raio e até `12` posições `TargetSpot` (`x, y, z, boss`), deixando o cliente gerar partículas customizadas conforme seu preset `LOW`/`MEDIUM`/`HIGH`.
- `ToggleInfusionC2SPacket` (`C2S`): rate-limit de `10` ticks por jogador; delega ao `CultivationActionService.executeToggleInfusion`.
- `ActivateUniqueSkillC2SPacket` (`C2S`): valida vivo, `!isSpiritShattered()`, `!isDivineOverloaded()`, cooldown `== 0`.
- `CultivatorActionC2SPacket` (`C2S`): rate-limit de `8` ticks por jogador; ações `EQUIP_REMNANT`, `TOGGLE_INFUSION`, `ADVANCE_BODY_STAGE`, `ADVANCE_SPIRIT_STAGE`, `BREAKTHROUGH_BODY`, `BREAKTHROUGH_SPIRIT` validadas inteiramente no servidor (incluindo exigência de Pílulas/Tomos em Sobrevivência).

## 4. Módulo de Integração (`com.mythiccontracts.integration` — Fase 9)
- **`ModCompatRegistry`:** detecta integrações opcionais sem dependência obrigatória de compilação e expõe `getIntegrationStatuses()` + `/contrato compat`.
- **`CombatCompatService`:** classifica ataques de jogadores em `MELEE`, `FIREARM` (`tacz:*` / `#mythic_contracts:firearm_damage`), `ARCANE_SPELL` (`irons_spellbooks:*` / tags arcanas-espirituais) ou `PROJECTILE`, aplica debounce de `3` ticks no consumo de Vigor melee em sweeps do Better Combat e reduz custo de Qi em follow-ups no mesmo tick e armas `#mythic_contracts:qi_resonant_weapons`.
- **`ModAdvancements`:** sincroniza e concede os `7` avanços nativos `mythic_contracts:*` consumidos pelo Vanilla `[L]` e pelo capítulo `despertar_2010.snbt` do FTB Quests.
- **`MythicContractsAPI`:** fachada estática server-safe exposta via `Java.loadClass(...)` em `kubejs/server_scripts/02_mythic_contracts_compat.js` (`global.MythicContracts`).
