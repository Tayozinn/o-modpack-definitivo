# Guia de QA e Auditoria Técnica — Fase 3 (MVP `mythic_contracts`)

**Projeto:** O Modpack Definitivo (Minecraft 1.20.1 — Forge 47.3.0 / Java 17 / Mojang Official Mappings)  
**Artefato Gerado:** `build/libs/mythic_contracts-1.0.0.jar` e `mods/mythic_contracts-1.0.0.jar` (`75.758 bytes`, reobfuscado via `reobfJar` para SRG)  
**Pacote Modrinth Atualizado:** `O_Modpack_Definitivo_Fase3.mrpack` (`80.996 bytes`, com `overrides/mods/mythic_contracts-1.0.0.jar` integrado)

---

## 1. Relatório de Auditoria Técnica (Código, Client/Server & Recursos)

### 1.1. Segurança Dedicated Server vs. Client (`Dist.CLIENT`)
Foram auditados todos os **24 arquivos `.java`** em `src/main/java/com/mythiccontracts/`:
- **Zero vazamentos de classes de cliente (`net.minecraft.client.*`, `com.mojang.blaze3d.*`, `org.lwjgl.glfw.*`)** nas 22 classes comuns/servidor (incluindo `CultivationBalance.java`, `CapabilityEvents.java`, `ContractGameplayEvents.java` e `ContractDebugCommand.java`).
- **`ClientKeyEvents.java`**: Única classe que referencia `KeyMapping`, `InputConstants`, `RegisterKeyMappingsEvent` e `GLFW.GLFW_KEY_V`. Suas classes de eventos (`ClientModBusEvents` e `ClientForgeBusEvents`) estão estritamente isoladas com `@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, value = Dist.CLIENT, ...)`.
- **`ClientPacketHandler.java`**: Única classe de rede que referencia `Minecraft.getInstance().player`. É invocada exclusivamente dentro de `SyncCoreDataS2CPacket.handle()` encapsulada por `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncCoreData(this.nbt))`.

### 1.2. Auditoria das Mecânicas Críticas do MVP (Refatoração de Cultivo, Infusão e Persistência)
1. **Curva de Cultivo do Corpo & Escalada Passiva de `ATTACK_DAMAGE` (`CultivationBalance.java` & `ContractManager.java`)**:
   - O Cultivo do Corpo (`BodyRealm` 1–7, `BodyStage` 1–3: *Base*, *Fortificação*, *Despertar*) aplica um `AttributeModifier` permanente com UUID fixo (`a7f31c90-4b2e-4d8a-9c15-778899aabbcc`) e `AttributeModifier.Operation.ADDITION` no atributo `Attributes.ATTACK_DAMAGE`.
   - **Fórmula Passiva do Corpo**: $\text{Bônus Passivo} = 0.5 \times R_{\text{body}} \times (R_{\text{body}} + S_{\text{body}} - 1)$, escalando de **`+0.5`** (Reino 1 - Etapa 1) até **`+31.5`** (Reino 7 - Etapa 3) ao longo dos 21 patamares.
   - O modificador `ADDITION` soma-se ao dano base da mão/arma e escala proporcionalmente com modificadores `MULTIPLY_BASE` (como o `+40%` do Pacto de Lúcifer).
   - Sempre que `/contrato murim corpo <1-7> <1-3>` é executado, `ContractManager.refreshPlayerContractAttributes(player, data)` recalcula imediatamente o `ATTACK_DAMAGE` e sincroniza a Capability via `ModCapabilities.sync(player)`.

2. **Cálculo Completo da Infusão de Mana (`V`) & Pressão de Reino (`CultivationBalance.java` & `ContractGameplayEvents.java`)**:
   - A tecla `V` envia `ToggleInfusionC2SPacket` ao servidor, alternando `manaInfusionActive` com feedback visual (partículas `SOUL_FIRE_FLAME` a cada 20 ticks enquanto ativo / `SMOKE` ao desativar), sonoro (`BEACON_ACTIVATE` / `BEACON_DEACTIVATE`) e na Actionbar.
   - **Infusão INATIVA (`manaInfusionActive == false`)**:
     - Contra alvos normais: aplica apenas o dano físico normal (`baseDamage`, que já inclui o `ATTACK_DAMAGE` passivo do Cultivo do Corpo).
     - Contra bosses na tag `high_realm_boss.json` (`minecraft:warden`, `minecraft:wither`): a **Pressão de Reino** suprime o ataque em **90%** (`event.setAmount(baseDamage * 0.10F)`) e exibe alerta vermelho na Actionbar.
   - **Infusão ATIVA (`manaInfusionActive == true`)**:
     - Perfura 100% da Pressão de Reino contra `high_realm_boss` e consolida três fontes de dano em uma **única chamada `event.setAmount(totalDamage)`** no `LivingHurtEvent` (protegida por `ThreadLocal<Boolean> PROCESSING_INFUSION`, verificação de `DamageTypes.THORNS`, auto-dano e valores inválidos, evitando perda por i-frames `invulnerableTime` e recursão):
       $$\text{Dano Total} = \text{Dano Base} + \text{Buff Percentual do Corpo} + \text{Bônus Espiritual Plano}$$
     - **Buff Percentual do Corpo na Infusão**: $\text{Multiplicador} = 0.20 \times (R_{\text{body}} - 1) + 0.05 \times S_{\text{body}}$ (de **`+5%`** no Reino 1-1 até **`+135%`** no Reino 7-3), resultando em $\text{bodyBuff} = \text{baseDamage} \times \text{Multiplicador}$.
     - **Bônus Espiritual Plano na Infusão**: $\text{spiritBonus} = 1.0 \times R_{\text{spirit}} \times (R_{\text{spirit}} + S_{\text{spirit}} - 1)$ (de **`+1.0`** no Reino 1-1 até **`+63.0`** no Reino 7-3).
     - Exibe telemetria detalhada na Actionbar: `§b[Infusão de Mana] §fDano: §e<Total> §7(Físico: <Base> + Corpo: +<BodyBuff> + Espírito: +<SpiritBonus>)` (acrescentando `§d(Pressão de Reino perfurada!)` contra bosses de alto reino).

3. **Capability `PlayerCoreData` & Persistência Pós-Morte Blindada (`CapabilityEvents.java` & `ModCapabilities.java`)**:
   - **Backup Redundante em `Player.PERSISTED_NBT_TAG`**: `ModCapabilities.savePersistedBackup` espelha o NBT completo da Capability dentro de `player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).put("mythic_contracts_core_backup", ...)` a cada `sync`, em `LivingDeathEvent`, `PlayerEvent.Clone`, `PlayerLoggedOutEvent` e `PlayerEvent.SaveToFile`.
   - **Reset de Infusão na Morte**: Em `LivingDeathEvent`, `PlayerEvent.Clone` (quando `event.isWasDeath() == true`), `PlayerRespawnEvent` (quando `!event.isEndConquered()`) e `PlayerTickEvent` (se `isDeadOrDying()`), `manaInfusionActive` é forçado para `false`.
   - **Clonagem e Retorno do The End**: `onPlayerCloned(PlayerEvent.Clone)` executa `original.reviveCaps()`, copia facção, contratos ativos, entidades subjugadas, `vowBroken` e níveis de cultivo (`BodyRealm`, `BodyStage`, `SpiritRealm`, `SpiritStage`) via `newData.copyFrom(oldData, wasDeath)` com fallback automático para `restoreFromPersistedBackup`, reaplica todos os `AttributeModifier`s (`ContractManager.refreshPlayerContractAttributes`) e preserva a vida atual ao retornar do portal do The End (`!wasDeath`), garantindo `original.invalidateCaps()` no bloco `finally`.
   - **Reaplicação e Sincronização S2C**: `PlayerLoggedInEvent`, `PlayerRespawnEvent` e `PlayerChangedDimensionEvent` restauram o backup se necessário, reaplicam os modificadores permanentes de atributo (Cultivo do Corpo + Pacto de Lúcifer) e enviam `SyncCoreDataS2CPacket` ao cliente.

4. **Contratos Assimétricos & Selo de Subjugação (`LuciferPactContract.java`, `PaladinVowContract.java`, `SubjugationSealItem.java` & `SubjugationEvents.java`)**:
   - **Pacto de Lúcifer (`lucifer_pact`)**: `-8.0` em `MAX_HEALTH` (`ADDITION`, UUID `c8a47b1e-5d92-4f10-9a3b-112233445566`) e `+0.40` (`+40%`) em `ATTACK_DAMAGE` (`MULTIPLY_BASE`, UUID `d9b58c2f-6e03-4a21-8b4c-665544332211`). Bloqueado para `JURAMENTADOS_DA_LUZ`.
   - **Voto de Paladino (`paladin_vow`)**: Absorção passiva (`4.0F` / 2 corações) a cada 40 ticks enquanto `!vowBroken`. Quebra ao atacar `AbstractVillager` (`WEAKNESS II` + `BLINDNESS I` por 10s, som de Wither e remoção do contrato). Bloqueado para `IRMANDADE_DIVIDA_ETERNA`.
   - **Selo de Subjugação**: Captura entidades de `subjugatable.json` com HP `<= 15%` (`<= 25%` para `QUEBRADORES_DE_TRONOS`) e invoca servos aliados com IA customizada via **Shift + Clique Direito**. Mobs em `low_tier_celestial_fiend.json` fogem de `QUEBRADORES_DE_TRONOS`.

### 1.3. Validação de Recursos, JSONs, PNG e Compilação Gradle
- `./gradlew.bat build --no-daemon --stacktrace`: **`BUILD SUCCESSFUL`**, 0 erros, 0 warnings de compilação.
- Todos os 7 arquivos JSON (`pack.mcmeta`, `en_us.json`, `pt_br.json`, `subjugation_seal.json`, `subjugatable.json`, `high_realm_boss.json`, `low_tier_celestial_fiend.json`) e `META-INF/mods.toml` validados.
- Textura `subjugation_seal.png` validada com assinatura PNG (`\x89PNG\r\n\x1a\n`) e dimensões `16x16` (`208 bytes`).

---

## 2. Roteiro de Teste Rápido In-Game (Validação Completa de QA)

Execute os passos abaixo em um mundo Singleplayer (com Cheats ativos) para validar todas as mecânicas:

### Passo 1 — Escalada Passiva de `ATTACK_DAMAGE` no Cultivo do Corpo (1 min)
1. Verifique o estado inicial da Capability (Reino 1 — Etapa 1: `+0.5` Dano Físico Passivo, `ATTACK_DAMAGE` total = `1.50` de mão vazia):
   ```mcfunction
   /contrato status
   ```
2. Suba o Cultivo do Corpo para **Reino 4 — Etapa 2 (Fortificação)** e observe a escalada imediata do `ATTACK_DAMAGE` passivo:
   ```mcfunction
   /contrato murim corpo 4 2
   /contrato status
   ```
   *(Resultado esperado: Fórmula $0.5 \times 4 \times (4 + 2 - 1) = +10.0$ de Dano Físico Passivo; `Ataque Físico Total (ATTACK_DAMAGE): 11.00` de mão vazia e `+70% Buff na Infusão`).*
3. Teste o topo da curva no **Reino 7 — Etapa 3 (Despertar)**:
   ```mcfunction
   /contrato murim corpo 7 3
   ```
   *(Resultado esperado: `+31.5 Dano Físico Passivo | +135% Buff na Infusão | Ataque Total: 32.50` de mão vazia).*
4. Retorne para **Reino 4 — Etapa 2** no Corpo e configure **Reino 5 — Etapa 3** no Espírito para os testes seguintes:
   ```mcfunction
   /contrato murim corpo 4 2
   /contrato murim espirito 5 3
   /contrato status
   ```
   *(Resultado esperado: Corpo 4-2 com `+10.0` passivo e `+70%` na Infusão; Espírito 5-3 com $1.0 \times 5 \times (5 + 3 - 1) = +35.0$ de Dano Espiritual Plano na Infusão).*

### Passo 2 — Cálculo Completo da Infusão de Mana (`V`) em Alvos Normais e Bosses (`high_realm_boss`) (1.5 min)
1. Invoque um alvo normal (ex.: **Iron Golem** sem IA) e um boss com Pressão de Reino (**Warden** sem IA):
   ```mcfunction
   /summon minecraft:iron_golem ~2 ~ ~2 {NoAI:1b}
   /summon minecraft:warden ~-2 ~ ~2 {NoAI:1b}
   ```
2. Com a Infusão de Mana **INATIVA**, dê um golpe de mão vazia (`ATTACK_DAMAGE = 11.0`) no **Iron Golem** e depois no **Warden**:
   - **No Iron Golem (alvo normal)**: causa os `11.0` de dano físico passivo normalmente.
   - **No Warden (`high_realm_boss`)**: exibe aviso vermelho na Actionbar `[Pressão de Reino] Seu ataque foi suprimido em 90%! Ative a Infusão de Mana [V].` e causa apenas `1.1` de dano (`10%` de `11.0`).
3. Pressione a tecla **`V`** para **ATIVAR a Infusão de Mana** (som de Beacon, partículas `SOUL_FIRE_FLAME` e Actionbar `[Infusão de Mana] ATIVADA`).
4. Golpeie o **Iron Golem** de mão vazia (`Dano Base = 11.0`, Corpo 4-2 = `+70%`, Espírito 5-3 = `+35.0`):
   *(Resultado esperado na Actionbar: `[Infusão de Mana] Dano: 53.7 (Físico: 11.0 + Corpo: +7.7 + Espírito: +35.0)`, onde $11.0 + (11.0 \times 0.70) + 35.0 = 53.7$).*
5. Golpeie o **Warden** com a Infusão de Mana **ATIVA**:
   *(Resultado esperado na Actionbar: `[Infusão de Mana] Dano: 53.7 (Físico: 11.0 + Corpo: +7.7 + Espírito: +35.0) (Pressão de Reino perfurada!)`, aplicando 100% do dano total consolidado sem supressão e com explosão de 15 partículas `SOUL_FIRE_FLAME`).*

### Passo 3 — Contratos Assimétricos, Persistência Pós-Morte (`/kill @s`) e Retorno do The End (1.5 min)
1. Teste a restrição de facção com os **Juramentados da Luz** e a **Quebra do Voto de Paladino**:
   ```mcfunction
   /contrato faccao juramentados_da_luz
   /contrato dar lucifer_pact
   ```
   *(Resultado esperado: Bloqueado pela facção).*
   ```mcfunction
   /contrato dar paladin_vow
   /summon minecraft:villager ~ ~ ~2 {NoAI:1b}
   ```
   *(Resultado esperado: Recebe 2 corações de Absorção em até 40 ticks; ao golpear o Villager, toca som de Wither, perde a Absorção, recebe Fraqueza II + Cegueira por 10s e marca `Voto Quebrado: Sim (QUEBRADO)`).*
2. Mude para a facção **Irmandade da Dívida Eterna** e firme o **Pacto de Lúcifer**:
   ```mcfunction
   /contrato faccao irmandade_divida_eterna
   /contrato dar lucifer_pact
   /contrato status
   ```
   *(Resultado esperado: Vida máxima cai de 20 para 12 HP [-4 corações] e `ATTACK_DAMAGE` de mão vazia sobe em +40% sobre `1.0 + 10.0 = 11.0`, atingindo **`15.40`**).*
3. **Ative a Infusão de Mana (`V`)** (verifique em `/contrato status` que `Infusão de Mana [V]: ATIVA`) e execute o teste de morte:
   ```mcfunction
   /kill @s
   ```
4. Após clicar em **Renascer (Respawn)**, execute:
   ```mcfunction
   /contrato status
   ```
   *(Resultado esperado pós-morte)*:
   - **Cultivo Intacto**: Corpo permanece em `Reino 4 — Etapa 2` (`+10.0` passivo) e Espírito em `Reino 5 — Etapa 3` (`+35.0`).
   - **Contratos e Facção Intactos**: Facção `irmandade_divida_eterna`, contrato `lucifer_pact` ativo e `Voto Quebrado: Sim (QUEBRADO)` preservados.
   - **Reset de Infusão na Morte**: `Infusão de Mana [V]` foi automaticamente resetada para **`INATIVA`** (`manaInfusionActive = false`).
   - **Preservação dos `AttributeModifier`s**: Vida máxima continua em **12 HP (6 corações)** e `Ataque Físico Total (ATTACK_DAMAGE)` permanece em **`15.40`** (soma de `1.0` base + `10.0` Cultivo do Corpo `ADDITION`, multiplicada por `+40%` do Pacto de Lúcifer `MULTIPLY_BASE`).
5. *(Opcional — Transição de Dimensão / Retorno do The End)*: Execute `/execute in minecraft:the_end run tp @s 0 65 0` e retorne pelo portal do End (ou via `/execute in minecraft:overworld run tp @s 0 80 0`), confirmando com `/contrato status` que os modificadores de `MAX_HEALTH` (`12 HP`) e `ATTACK_DAMAGE` (`15.40`) permanecem ativos.

### Passo 4 — Selo de Subjugação, Invocação de Boss Aliado e Aura de Temor (1 min)
1. Mude para a facção **Quebradores de Tronos** (permite selar com `<= 25%` de vida em vez de `<= 15%` e ativa a Aura de Temor):
   ```mcfunction
   /contrato faccao quebradores_de_tronos
   /give @s mythic_contracts:subjugation_seal 2
   ```
2. Reduza a vida do Warden para menos de 25% (ex.: 50 HP de 500 HP):
   ```mcfunction
   /data modify entity @e[type=minecraft:warden,limit=1,sort=nearest] Health set value 50.0f
   ```
3. Segure o **Selo de Subjugação** e clique com o **Botão Direito** (sem Shift) no Warden.
   *(Resultado esperado: O Warden desaparece em chamas azuis, 1 selo é consumido e `/contrato status` exibe `minecraft:warden` em Entidades Subjugadas).*
4. Segure **Shift + Clique Direito** com o Selo de Subjugação para invocar seu servo:
   *(Resultado esperado: O Warden renasce como `[Subjugado de <SeuNome>] Warden`, segue você como guarda-costas, não te ataca e ataca qualquer mob hostil que você golpear).*
5. Teste a **Aura de Temor** dos Quebradores de Tronos invocando um Wither Skeleton ou Blaze:
   ```mcfunction
   /summon minecraft:wither_skeleton ~ ~ ~3
   ```
   *(Resultado esperado: O Wither Skeleton foge ativamente de você devido ao `AvoidEntityGoal` injetado para a facção `quebradores_de_tronos`).*
