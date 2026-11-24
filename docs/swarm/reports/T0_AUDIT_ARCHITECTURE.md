# Relatório T0-1 — Auditoria de Código & Arquitetura Modular (Fase 0 + Fase 1)

- **Tarefa:** `T0-1` — Auditoria do código real de `mythic_contracts` (MC 1.20.1 / Forge 47.3.0) e proposta de arquitetura modular + contratos entre módulos para Vida Corporal, Integridade Espiritual, Qi, Resistência, HUD, Interface do Cultivador e Efeitos Visuais.
- **Status:** **CONCLUÍDO** (100% ancorado no código-fonte real inspecionado em `src/main/java/com/mythiccontracts/`, `src/main/resources/`, `KEYBINDS_PLAN.md` e `defaultconfigs/options.txt`).

---

## 1. Achados da Auditoria (Estado Real vs. O Que Falta)

### 1.1. Mapa Geral das Classes Auditadas
A base atual já opera no schema **NBT v2** (`PlayerDataSchema.CURRENT_VERSION = 2`), com `PlayerData` dividido em 5 subdomínios (`CultivationData`, `FactionData`, `ContractData`, `SkillData`, `SubjugationData`), adaptados para o código legado via `PlayerCoreData` e sincronizados por `SyncCoreDataS2CPacket` (completo) e `SyncEnergyS2CPacket` (delta de energia/cooldown a cada 10 ticks em `ContractGameplayEvents.onPlayerTick`).

| Sistema / Recurso | O Que Já Existe no Código Real | O Que Falta / Lacunas Identificadas |
| :--- | :--- | :--- |
| **Vida Corporal (`Attributes.MAX_HEALTH`)** | • `AttributeService` (`attribute/AttributeService.java:28`) rastreia `Attributes.MAX_HEALTH` e aplica modificadores permanentes via `AttributeModifierSpec`.<br>• `RealmReloadListener.mapShorthandAttribute` (`cultivation/realm/RealmReloadListener.java:236`) já mapeia `"health"`/`"max_health"` para `minecraft:generic.max_health`.<br>• `LuciferPactContract` (`data/.../contracts/lucifer_pact.json`) aplica `-8.0` em `MAX_HEALTH` (`ADDITION`, UUID `c8a47b1e-5d92-4f10-9a3b-112233445566`).<br>• `CapabilityEvents.onPlayerCloned` (`capability/CapabilityEvents.java:82-86`) restaura `previousHealth` apenas quando `!wasDeath`. | 1. **Zero bônus de Vida Corporal nos Reinos:** Os 7 JSONs `data/mythic_contracts/realms/body/body_realm_1..7.json` e o fallback em `RealmRegistry.createBuiltinFallbacks()` (`RealmRegistry.java:64-80`) aplicam apenas `generic.attack_damage` (`+0.5` a `+31.5`), sem bônus de `generic.max_health`.<br>2. **Bug de perda de vida no refresh:** `CultivationService.applyCultivationAttributes` (`CultivationService.java:239`) chama `AttributeService.clearByNamePrefix` **antes** de reaplicar os atributos. Como `clearByNamePrefix` chama `clampHealthIfNeeded` (`AttributeService.java:137`), o HP atual do jogador é truncado para `20.0` (base) antes do novo modificador de `MAX_HEALTH` entrar.<br>3. **Falta preservação proporcional (`oldHealth / oldMaxHealth`)** ao alterar reinos/contratos e falta restaurar `player.setHealth(player.getMaxHealth())` após reaplicar atributos no respawn pós-morte (`CapabilityEvents.onPlayerRespawn`). |
| **Integridade Espiritual (Barra Independente)** | • **Inexistente.** Não há campos em `CultivationData`, `PlayerDataSchema`, `EnergyService`, `SyncEnergyS2CPacket` ou `CultivationHudOverlay`. | 1. Criar estado persistente (`maxSpiritualIntegrity`, `currentSpiritualIntegrity`, `spiritShatterTicks`).<br>2. Implementar `SpiritualIntegrityService` com cálculo de capacidade/regeneração escalando com `spiritRealm` e `spiritStage`.<br>3. Classificador configurável de dano espiritual/alma/maldição/mágico no `LivingHurtEvent` (via tags `#mythic_contracts:spiritual_damage`, `#mythic_contracts:magic_spiritual_damage` e detecção dinâmica de namespaces como `irons_spellbooks:*` sem hard-dep).<br>4. Consequências ao zerar (**Colapso Espiritual**): desativar Infusão `[V]`, bloquear `[G]` e quebras de reino durante o colapso, reduzir regen de Qi e aplicar vulnerabilidade espiritual temporária. |
| **Qi (Energia Espiritual)** | • `CultivationData` (`core/player/CultivationData.java:26-28`) armazena `maxQiCapacity`, `currentQi`, `manaInfusionActive`.<br>• `EnergyService` (`core/service/EnergyService.java:32-42, 115-135`) recalcula `maxQi` (`body.qiBase + bodyStage.qiBonus + spirit.qiBase + spiritStage.qiBonus`), drena na Infusão `[V]` por segundo (`spiritRealm.infusionDrainPerSecond()`) e regenera fora de infusão.<br>• `ContractGameplayEvents.onLivingHurt` (`event/ContractGameplayEvents.java:173-178`) drena Qi por golpe (`infusionDrainPerHit`). | 1. Expor taxas de regeneração e dreno consolidadas (`getEffectiveQiRegenPerSecond`, `getEffectiveInfusionDrainPerSecond`) para exibição na Interface do Cultivador e tooltips da HUD.<br>2. Permitir modificadores aditivos/multiplicativos de capacidade e regeneração de Qi vindos de contratos ou efeitos de colapso espiritual. |
| **Resistência Corporal (Stamina Física)** | • **Inexistente como vigor físico.** Existe apenas a barra `DivineResistance` em `SkillData` (exclusiva para skills `[G]`). | 1. Adicionar estado de **Resistência Corporal (Stamina)** em `CultivationData` (`maxStamina`, `currentStamina`, `staminaRecoveryDelayTicks`), escalando com `bodyRealm` e `bodyStage`.<br>2. Implementar consumo/regeneração em `VitalService` / `EnergyService` (sprint intenso, golpes melee, técnicas corporais) sem interferir na barra de Resistência Divina das skills `[G]`. |
| **Resistência Divina & Resquícios `[G]`** | • `SkillData` (`core/player/SkillData.java:30-32`) mantém `maxDivineResistance`, `currentDivineResistance`, `divineOverloadTicks`.<br>• `DivineRemnant` (`skill/remnant/DivineRemnant.java:13-41`) define os 4 Resquícios aprovados (`Olho de Rá`, `Ceifa da Morrígan`, `Vajra de Indra`, `Passo de Tezcatlipoca`).<br>• `DivineResonanceBalance` (`skill/remnant/DivineResonanceBalance.java:15-121`) define `ManifestationTier` (`I..IV`), capacidade (`100..540`), custo decrescente (`55..20`) e `OVERLOAD_PENALTY_TICKS = 1200` (60s).<br>• `UniqueSkillHandler` (`cultivation/UniqueSkillHandler.java:100-155`) dispara a skill mesmo no uso que zera a barra e aplica Sobrecarga de 60s. | 1. **Preservar 100% da mecânica e dos nomes.**<br>2. Desacoplar os efeitos visuais (atualmente `level.sendParticles` inline em `UniqueSkillHandler.java:192-200, 245-248, 288-294, 349-353`) para o novo `VisualEffectEngine` com suporte a presets de qualidade (`LOW`, `MEDIUM`, `HIGH`) e partículas customizadas.<br>3. Permitir equipar/visualizar o Resquício Divino diretamente na aba **Habilidades** da Interface do Cultivador via pacote C2S validado no servidor. |
| **HUD (`CultivationHudOverlay`)** | • `CultivationHudOverlay` (`client/CultivationHudOverlay.java:29-177`) renderiza caixa fixa em `BOX_X=8, BOX_Y=8` (`204x72`) com `GuiGraphics.fill`/`drawString`, exibindo Facção, Infusão `[V]`, `Corpo R%d-%d | Espírito R%d-%d`, barra de Qi, barra de Resistência Divina, nome da skill `[G]` com camada `[I..IV]` e contador de Selos. | 1. Substituir pelo novo sistema de HUD modular com as 4 barras vitais (Vida Corporal, Integridade Espiritual, Qi, Resistência Corporal) + módulo integrado de Resquício Divino / Resistência Divina `[G]`.<br>2. Adicionar interpolação suave (lerp de perda/cura), alertas visuais e suporte a configuração persistente do cliente (posição X/Y, escala, opacidade, ocultar elementos, preset visual). |
| **Interface do Cultivador (`Screen`)** | • **Inexistente.** Hoje a inspeção completa depende de `/contrato status` (`command/ContractDebugCommand.java:140-206`). | 1. Criar `CultivatorScreen` (`net.minecraft.client.gui.screens.Screen`) com 7 abas (`Visão Geral`, `Cultivo Corporal`, `Cultivo Espiritual`, `Atributos`, `Habilidades`, `Contratos`, `Facção`), renderização 3D do jogador + diagrama de meridianos e ações C2S autoritativas (Quebra de Reino, Equipar Resquício, Gerenciar Contratos). |

---

## 2. Análise Crítica 1: Conciliação entre "Resistência (Stamina)" e "Resistência Divina"

### 2.1. Diagnóstico do Conflito
No código atual (`SkillData.java`, `EnergyService.java`, `DivineResonanceBalance.java`, `UniqueSkillHandler.java` e `CultivationHudOverlay.java`), o recurso **"Resistência Divina"** já está implementado, testado (`DivineResonanceBalanceTest.java`) e aprovado pelo usuário com regras muito específicas:
- É o limite de tolerância do vaso mortal ao canalizar os **Resquícios de Deuses Caídos (`[G]`)**.
- Quando um uso de `[G]` reduz `currentDivineResistance` a `0`, a habilidade **ainda é executada**, mas o jogador entra em **Sobrecarga Divina por 60 segundos (`1200` ticks)**, recebendo Fraqueza I e ficando impedido de usar `[G]` até recuperar 35% da barra.

Se a nova barra de **"Resistência"** (vigor físico / stamina) fosse fundida na mesma variável `currentDivineResistance`:
1. Correr, esquivar ou atacar fisicamente drenaria a barra da skill `[G]` e poderia acionar 60 segundos de Sobrecarga Divina + Fraqueza por mero esforço físico.
2. Inversamente, usar uma skill `[G]` no limite zeraria o vigor físico do jogador por 60 segundos inteiros.

### 2.2. Decisão Arquitetural: Coexistência com Papéis e Hierarquia Visual Distintos
1. **Separação Estrita na Camada de Dados (`PlayerData`):**
   - **Resistência Corporal / Vigor (`Stamina` em `CultivationData`)**:
     - Representa o fôlego e vigor muscular do Cultivador Corporal.
     - Campos em `CultivationData`: `float maxStamina` (base `100.0F`, escalando com `BodyRealm` e `BodyStage`: `100.0F + 25.0F * (R_B - 1) + 5.0F * (S_B - 1)`), `float currentStamina`, `int staminaRegenDelayTicks`.
     - Regeneração rápida fora de esforço (`10.0F/s` a `22.0F/s`), consumida por manobras físicas e combate corporal intenso.
   - **Resistência Divina (`DivineResistance` em `SkillData`)**:
     - Mantida **100% preservada** em `SkillData` (`maxDivineResistance`, `currentDivineResistance`, `divineOverloadTicks`), regida por `DivineResonanceBalance` e exclusiva para os Resquícios Divinos `[G]`.
2. **Conciliação Visual na HUD e na Interface do Cultivador:**
   - **Bloco Vital Principal da HUD (4 Barras do Cultivador):**
     1. **Vida Corporal** (`player.getHealth() / player.getMaxHealth()` + Absorção) — *Vermelho Carmesim*
     2. **Integridade Espiritual** (`currentSpiritualIntegrity / maxSpiritualIntegrity`) — *Roxo/Azul Astral*
     3. **Qi** (`currentQi / maxQiCapacity` + estado `[V]`) — *Ciano Espiritual*
     4. **Resistência** (`currentStamina / maxStamina`) — *Verde Jade / Âmbar Corporal*
   - **Módulo de Resquício Divino `[G]` na HUD:**
     - Exibido como o **Emblema/Slot de Habilidade Divina `[G]`** acoplado ao HUD, contendo sua própria barra/anel dedicado de **Resistência Divina** (Dourado Solar -> Laranja -> Vermelho Sangue em `SOBRECARGA: 60.0s`) e o indicador de Camada (`[I]` a `[IV]`).
   - **Resultado:** Cumpre 100% da especificação das 4 barras vitais (Vida Corporal, Integridade Espiritual, Qi, Resistência) e preserva sem nenhuma perda funcional ou visual a mecânica de Resistência Divina + Sobrecarga de 60s aprovada pelo usuário.

---

## 3. Análise Crítica 2: Auditoria de Keybinds para a Interface do Cultivador

### 3.1. Varredura Completa A–Z em `KEYBINDS_PLAN.md` e `defaultconfigs/options.txt`

| Tecla | Vínculo Atual em `defaultconfigs/options.txt` | Contexto (`KeyConflictContext`) | Status para Interface do Cultivador |
| :--- | :--- | :--- | :--- |
| `W, A, S, D` | Movimento Vanilla (`forward`, `left`, `back`, `right`) + `A` no JEI (`JEI_GUI_HOVER`) | `IN_GAME` (`A` também `JEI_GUI_HOVER`) | ❌ Ocupadas |
| `Q, E, F, T` | Dropar (`Q`), Inventário (`E`), Offhand (`F`), Chat (`T`) | `IN_GAME` | ❌ Ocupadas |
| `R, B, C, Z, H, O, M` | **TaCZ**: Reload (`R`), Fire Select (`B`), Crawl (`C`), Refit (`Z`), Inspect (`H`), Interact (`O`), Melee/Zoom (`M`) | `IN_GAME` | ❌ Ocupadas (Combate FPS) |
| `X, Y` | **Iron's Spells**: Cast Spell (`X`), Spell Wheel (`Y`) | `IN_GAME` | ❌ Ocupadas (Combate Arcano) |
| `K, N` | **Better Combat**: Feint (`K`), Toggle Mine (`N`) | `IN_GAME` | ❌ Ocupadas (Combate Melee) |
| `V, G` | **Mythic Contracts**: Infusão de Mana (`V`), Resquício Divino (`G`) | `IN_GAME` | ❌ Reservadas Intocáveis (Core Mod) |
| `I, J, P` | **Menus do Modpack**: Curios (`I`), FTB Quests (`J`), FTB Teams (`P`) | `IN_GAME` | ❌ Ocupadas (Cluster de Menus) |
| `L` | **Vanilla**: Conquistas (`key_key.advancements:key.keyboard.l`) | `IN_GAME` | ⚠ Ocupada por Vanilla Advancements |
| **`U`** | **JEI**: Mostrar Usos (`key_key.jei.showUses:key.keyboard.u`) | **Exclusivo `JEI_GUI_HOVER`** | ✅ **100% LIVRE EM `IN_GAME`** |
| **`F4`** | Nenhum (`não listado em options.txt`) | Livre globalmente | ✅ **100% LIVRE GLOBALMENTE** |

### 3.2. Recomendação Oficial de Keybind
1. **Tecla Primária Recomendada: `U` (`GLFW.GLFW_KEY_U` / `key.keyboard.u`) com `KeyConflictContext.IN_GAME`**
   - **Por que zero conflito real:** No código do JEI 15.x e conforme documentado em `KEYBINDS_PLAN.md` (linhas 28 e 61–63), `key.jei.showUses` (`U`), `key.jei.showRecipe` (`R`) e `key.jei.bookmark` (`A`) operam sob o contexto **`JEI_GUI_HOVER`** (só ativam quando uma `AbstractContainerScreen` já está aberta e o cursor está sobre um item). Fora de inventários (`KeyConflictContext.IN_GAME`), **`U` é a única tecla alfabética A–Z completamente livre no modpack**.
   - **Ergonomia e Coerência de Cluster:** Na fileira superior direita, `U` fica imediatamente ao lado de `I` (Inventário Curios), `J` (FTB Quests) e `P` (FTB Teams), consolidando o cluster de interfaces de progressão:
     - `[U]` -> **Interface do Cultivador (Mythic Contracts)**
     - `[I]` -> **Acessórios & Grimório (Curios)**
     - `[J]` -> **Missões (FTB Quests)**
     - `[P]` -> **Equipe (FTB Teams)**
   - **Compatibilidade ABNT2 & US ANSI:** Tecla alfabética pura, livre de dead-keys (`~`, `` ` ``, `'`, `[`).
   - **Comportamento para Fechar a Tela:** Dentro de `CultivatorScreen.keyPressed(...)`, pressionar `ESC`, `E` (`options.keyInventory`) ou a própria tecla `CULTIVATOR_SCREEN_KEY` (`U`) fecha a janela imediatamente (já que `CultivatorScreen` não exibe overlay do JEI).

---

## 4. Arquitetura Modular e Contratos entre Módulos (Fase 1)

### 4.1. Evolução Compatível de Persistência (`PlayerData` v2 -> v3)
Para não quebrar saves v1 nem v2 nem os 15 testes unitários existentes:
- `PlayerDataSchema.CURRENT_VERSION` permanece compatível com leitura de `v1` e `v2`, avançando para `VERSION_V3 = 3` (ou leitura tolerante em `v2` com defaults seguros via `Tag.TAG_ANY_NUMERIC`).
- **Novas chaves em `PlayerDataSchema` (seção `Cultivation`):**
  - `TAG_MAX_SPIRITUAL_INTEGRITY = "MaxSpiritualIntegrity"` (`float`, default calculado por `SpiritualIntegrityService`)
  - `TAG_CURRENT_SPIRITUAL_INTEGRITY = "CurrentSpiritualIntegrity"` (`float`, default `= maxSpiritualIntegrity`)
  - `TAG_SPIRIT_SHATTER_TICKS = "SpiritShatterTicks"` (`int`, default `0`)
  - `TAG_MAX_STAMINA = "MaxStamina"` (`float`, default calculado por `VitalService`)
  - `TAG_CURRENT_STAMINA = "CurrentStamina"` (`float`, default `= maxStamina`)
  - `TAG_STAMINA_DELAY_TICKS = "StaminaRegenDelayTicks"` (`int`, default `0`)
- **Regra em `CultivationData.copyFrom(CultivationData other, boolean wasDeath)`:**
  - Se `wasDeath == true`: restaura `currentSpiritualIntegrity = getMaxSpiritualIntegrity()`, `spiritShatterTicks = 0`, `currentStamina = getMaxStamina()`, `staminaRegenDelayTicks = 0`, além de `currentQi = getMaxQiCapacity()` e `manaInfusionActive = false`.
  - Se `wasDeath == false` (retorno do End / troca de dimensão): preserva os valores atuais clampados aos respectivos máximos.

### 4.2. Contrato do `VitalService` e Integração Segura com `Attributes.MAX_HEALTH`
Localização proposta: `com.mythiccontracts.core.service.VitalService` + melhoria em `com.mythiccontracts.attribute.AttributeService`.

1. **Proibição de `setBaseValue`:** O `BaseValue` de `Attributes.MAX_HEALTH` permanece `20.0D` (padrão Vanilla). Toda vida adicional do Cultivo Corporal é aplicada como `AttributeModifier` (`Operation.ADDITION`) com nome `mythic_contracts.cultivation.body.max_health` e UUID fixo/determinístico `b8e42d01-5c3f-4e9b-8d26-889900bbccdd`.
2. **Curva de Vida Corporal nos Reinos do Corpo (`body_realm_1..7.json` + `RealmRegistry` fallback):**
   - Fórmula progressiva nos 21 patamares corporais (`R_B in 1..7`, `S_B in 1..3`):
     $$\text{BodyHealthBonus}(R_B, S_B) = 2.0 \times R_B \times (R_B + S_B - 1) - 4.0 \quad (\text{para } R_B=1, S_B=1 \implies 0.0\text{; até }+122.0\text{ HP em } 7\text{-}3)$$
     *(Configurável diretamente na lista `"attributes"` de cada estágio em `data/mythic_contracts/realms/body/body_realm_*.json` via `{"attribute": "minecraft:generic.max_health", "operation": "addition", "amount": ...}`).*
3. **Preservação de Proporção de Vida (`AttributeService.runPreservingHealthRatio`):**
   ```java
   public static void runPreservingHealthRatio(ServerPlayer player, boolean healToFull, Runnable attributeMutation) {
       float oldMax = Math.max(1.0F, player.getMaxHealth());
       float oldHealth = Mth.clamp(player.getHealth(), 0.0F, oldMax);
       float ratio = oldHealth / oldMax;
       SUPPRESS_INTERMEDIATE_HEALTH_CLAMP.set(true);
       try {
           attributeMutation.run();
       } finally {
           SUPPRESS_INTERMEDIATE_HEALTH_CLAMP.set(false);
       }
       float newMax = Math.max(1.0F, player.getMaxHealth());
       if (healToFull) {
           player.setHealth(newMax);
       } else if (player.isAlive() && oldHealth > 0.0F) {
           player.setHealth(Mth.clamp(ratio * newMax, 1.0F, newMax));
       }
   }
   ```
   - Elimina o bug onde `clearByNamePrefix` reduzia a vida do jogador para `20.0` antes de `applyModifiers` aplicar o novo bônus.
   - Em `CapabilityEvents.onPlayerRespawn(PlayerRespawnEvent)`, quando `!event.isEndConquered()`, passa `healToFull = true` para que o jogador renasça com `100%` da sua nova Vida Corporal máxima.

### 4.3. Contrato do `SpiritualIntegrityService`
Localização proposta: `com.mythiccontracts.core.service.SpiritualIntegrityService`.

- **Cálculo de Capacidade e Regeneração:**
  - `calculateMaxSpiritualIntegrity(PlayerData data)`: Base `100.0F + 40.0F * (R_S - 1) + 12.0F * (S_S - 1) + 10.0F * (R_B - 1)` (de `100` em R1-1 até `424` em R7-3).
  - `calculateSpiritualRegenPerSecond(PlayerData data)`: `2.0F + 1.0F * (R_S - 1) + 0.35F * (S_S - 1)` fora de combate espiritual; `0.0F` durante `spiritShatterTicks > 0` (ao fim de `spiritShatterTicks`, recupera `25%` da integridade, análogo à recuperação da Resistência Divina).
- **Classificação de Dano (`classifyIncomingDamage(DamageSource source, LivingEntity victim)`):**
  - `SpiritualDamageCategory.NONE`: Dano físico comum (queda, espada mundana, projétil balístico TaCZ sem infusão) -> `100%` na Vida Corporal.
  - `SpiritualDamageCategory.HYBRID_ARCANE`: Dano mágico (`source.is(DamageTypes.MAGIC)`, `source.is(DamageTypes.INDIRECT_MAGIC)`, tag `#mythic_contracts:arcane_damage` ou `source.typeHolder().unwrapKey()` pertencente a `irons_spellbooks`) -> `70%` na Vida Corporal + `30%` absorvido/drenado da Integridade Espiritual (se a Integridade Espiritual estiver zerada, a Vida Corporal sofre `+25%` de dano mágico amplificado).
  - `SpiritualDamageCategory.PURE_SPIRITUAL`: Dano de alma/maldição/pressão espiritual (`source.is(DamageTypes.WITHER)`, `source.is(DamageTypes.SONIC_BOOM)`, tag `#mythic_contracts:spiritual_damage`, ataques de `#mythic_contracts:high_realm_boss`, backlash de quebra de voto/tribulação) -> `50%` na Integridade Espiritual e `50%` na Vida Corporal (ou `100%` na Vida Corporal com multiplicador `1.35x` se em **Colapso Espiritual**).
- **Consequências ao Zerar (`triggerSpiritShatter(ServerPlayer player, PlayerData data)`):**
  - Define `spiritShatterTicks = 400` (20 segundos).
  - Desativa imediatamente `manaInfusionActive = false`.
  - Bloqueia ativação de Resquícios Divinos `[G]` e Quebras de Reino enquanto `isSpiritShattered() == true`.
  - Aplica `MobEffects.BLINDNESS` (40 ticks) + `MobEffects.CONFUSION` (80 ticks) + som de cristal espiritual rachando.

### 4.4. Protocolo de Rede Autoritativo (`ModNetworking` — Protocolo `"3"`)

| Pacote | Direção | Payload (`FriendlyByteBuf`) | Autoridade / Validação no Servidor |
| :--- | :--- | :--- | :--- |
| `SyncCoreDataS2CPacket` | `S2C` | `CompoundTag` (gerado por `PlayerDataSerializer.writeClientSync`) | Enviado no login, respawn, troca de dimensão, mudança de reino/contrato/facção. |
| `SyncEnergyS2CPacket` *(Expandido)* | `S2C` | `float currentQi`, `float maxQi`, `boolean infusionActive`, `int skillCooldownTicks`, `float currentDivineRes`, `float maxDivineRes`, `int divineOverloadTicks`, `float currentSpiritIntegrity`, `float maxSpiritIntegrity`, `int spiritShatterTicks`, `float currentStamina`, `float maxStamina` | `39 bytes` totais. Enviado a cada 10 ticks apenas quando algum recurso vital mudou ou em transições críticas (esgotamento/colapso/sobrecarga). |
| `SpawnVisualEffectS2CPacket` *(Novo)* | `S2C` | `ResourceLocation effectId`, `byte phaseOrdinal` (`ACTIVATION`, `EXECUTION`, `IMPACT`, `END`), `byte tierOrdinal` (`I..IV`), `double x, y, z`, `float yaw, pitch`, `int sourceEntityId` | Enviado via `PacketDistributor.TRACKING_ENTITY_AND_SELF` para renderizar partículas customizadas e efeitos das 4 skills `[G]` respeitando o preset gráfico local de cada cliente (`LOW`/`MEDIUM`/`HIGH`). |
| `ToggleInfusionC2SPacket` | `C2S` | Vazio (`0 bytes`) | Servidor valida `!data.getCultivation().isSpiritShattered()` e `EnergyService.canActivateInfusion(data)`. |
| `ActivateUniqueSkillC2SPacket` | `C2S` | Vazio (`0 bytes`) | Servidor valida vida, `!isSpiritShattered()`, `!isDivineOverloaded()`, cooldown e consome Resistência Divina. |
| `CultivatorActionC2SPacket` *(Novo)* | `C2S` | `enum ActionType` (`EQUIP_REMNANT`, `ATTEMPT_BREAKTHROUGH`, `TOGGLE_INFUSION`, `REVOKE_CONTRACT`), `ResourceLocation targetId` | Servidor valida se o `targetId` existe nos registros (`DivineRemnant`, `RealmRegistry`, `ContractManager`), checa requisitos (`isReadyForBreakthrough`, Qi, facção) e nunca aceita valores numéricos vindos do cliente. |

### 4.5. Arquitetura do `VisualEffectEngine` e Configs de Cliente
- **`VisualEffectEngine` (`com.mythiccontracts.client.fx.VisualEffectEngine`)**:
  - Executa apenas em `Dist.CLIENT` ao receber `SpawnVisualEffectS2CPacket` (com fallback de partículas vanilla no servidor para compatibilidade caso necessário, ou centralizado no pacote S2C).
  - Respeita `ClientHudConfig.particleQualityPreset` (`LOW` = 30% das partículas, sem anéis secundários; `MEDIUM` = 70%; `HIGH` = 100% + trilhas completas).
  - Zero dependência de shaders (100% compatível com `Embeddium 0.3.31`).
- **`ClientHudConfig` (`com.mythiccontracts.client.config.ClientHudConfig`)**:
  - `ForgeConfigSpec` (`ModConfig.Type.CLIENT`, arquivo `mythic_contracts-client.toml`) armazenando: `hudAnchorX`, `hudAnchorY`, `hudScale` (`0.5..2.0`), `hudOpacity` (`0.2..1.0`), `showBodyHealthBar`, `showSpiritIntegrityBar`, `showQiBar`, `showStaminaBar`, `showDivineResonanceModule`, `enableSmoothAnimations`, `render3DPlayerInGui`, `visualQualityPreset` (`LOW`, `MEDIUM`, `HIGH`).

---

## 5. Riscos Identificados e Mitigações

| ID | Risco | Severidade | Mitigação Arquitetural |
| :--- | :--- | :--- | :--- |
| **R1** | Truncamento de HP (`20.0`) ao limpar/reaplicar atributos de cultivo ou contratos (`AttributeService.java:137`). | **ALTA** | Envolver `CultivationService.applyCultivationAttributes` e `ContractManager.refreshPlayerContractAttributes` em `AttributeService.runPreservingHealthRatio(...)`. |
| **R2** | Conflito semântico e de gameplay entre "Resistência (Stamina)" e "Resistência Divina (Skills G)". | **ALTA** | Separar `Stamina` em `CultivationData` (4ª barra vital da HUD) e manter `DivineResistance` em `SkillData` (acoplada ao módulo do Resquício Divino `[G]` com Sobrecarga de 60s). |
| **R3** | Colisão de tecla da Interface do Cultivador com TaCZ, Iron's Spells ou Better Combat. | **MÉDIA** | Usar `GLFW.GLFW_KEY_U` com `KeyConflictContext.IN_GAME` (única tecla A–Z livre in-game, já que o JEI usa `JEI_GUI_HOVER`), mantendo `options.txt` sincronizado. |
| **R4** | Sobrecarga de pacotes S2C em multiplayer ao sincronizar 4 barras vitais + Resistência Divina. | **MÉDIA** | Consolidar todos os floats vitais no único `SyncEnergyS2CPacket` (39 bytes) enviado no máximo a cada 10 ticks e somente quando `energyChanging == true` ou em evento de transição. |
| **R5** | Queda de FPS ao renderizar o modelo 3D do jogador na `CultivatorScreen` ou partículas densas nas skills `[G]`. | **BAIXA** | Flag `render3DPlayerInGui` (com fallback para silhueta 2D de meridianos) e escalonamento de densidade por preset (`LOW`/`MEDIUM`/`HIGH`) no `VisualEffectEngine`. |

---

## 6. Plano Incremental de Migração e Recomendações

1. **Etapa 1 (Core & Persistência Retrocompatível):** Estender `CultivationData`, `PlayerDataSchema` e `PlayerDataSerializer` com os campos de `SpiritualIntegrity` e `Stamina` (usando defaults calculados quando ausentes em saves v1/v2).
2. **Etapa 2 (Atributos & Serviços Vitais):** Implementar `AttributeService.runPreservingHealthRatio`, adicionar bônus de `generic.max_health` nos 7 JSONs de `realms/body/` (e `RealmRegistry`), criar `VitalService` e `SpiritualIntegrityService` e integrá-los ao `EnergyService.tickAllEnergy` e `ContractGameplayEvents`.
3. **Etapa 3 (Rede Autoritativa):** Expandir `SyncEnergyS2CPacket` e registrar `CultivatorActionC2SPacket` e `SpawnVisualEffectS2CPacket` em `ModNetworking`.
4. **Etapa 4 (Cliente — Config, HUD, `CultivatorScreen` e `VisualEffectEngine`):** Após aprovação da Fase 2 pelo usuário, implementar `ClientHudConfig`, a HUD escolhida, a `CultivatorScreen` na tecla `U` (`KeyConflictContext.IN_GAME`) com suas 7 abas e o `VisualEffectEngine` para os 4 Resquícios Divinos.

---

## 7. Pendências
- Nenhuma pendência bloqueante para a Fase 0/1. A implementação visual aguarda a aprovação do usuário na **Fase 2 (Concepção Visual)**.
