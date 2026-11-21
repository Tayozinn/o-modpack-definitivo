# Documento de Contexto & Handoff do Projeto — O Modpack Definitivo (Fantasia Urbana 2010)

> **Use este documento para transportar o contexto completo do projeto para uma nova conversa ou novo Workspace.**

---

## 1. Visão Geral & Identidade do Projeto
- **Temática:** Fantasia Urbana de 2010 (15 anos após uma guerra entre deuses mitológicos que arruinou metrópoles modernas, misturando cidades devastadas, sistema **Murim Duplo** de Cultivo de Corpo e Espírito, **Contratos Celestiais/Demoníacos**, **Subjugação de Bosses**, armas de fogo modernas, espadas e magia).
- **Diretório Local do Projeto (Workspace):** `C:\Users\lealt\OneDrive\Área de Trabalho\o modpack definitivo`
- **Repositório GitHub (Sincronizado na branch `main`):** https://github.com/Tayozinn/o-modpack-definitivo
- **Instância Local do Prism Launcher:** `C:\Users\lealt\AppData\Roaming\PrismLauncher\instances\O_Modpack_Definitivo_Fase2`
- **Stack Técnica:**
  - **Minecraft:** `1.20.1`
  - **Modloader:** `Forge 47.3.0` (compatível com `[47.2.0,)`)
  - **Java:** `OpenJDK 17.0.15 LTS` (`C:\Users\lealt\AppData\Roaming\PrismLauncher\java\java-runtime-gamma`)
  - **Core Mod Autoral:** `mythic_contracts` (`com.mythiccontracts`, Mojang Official Mappings, ForgeGradle 6 / Gradle 8.4)
  - **Gerenciador de Pacote:** Packwiz (`pack.toml`, `index.toml`, `mods/*.pw.toml`) + Instalador Modrinth (`O_Modpack_Definitivo_Fase3.mrpack`)

---

## 2. Tudo o que já foi Construído e Validado

### FASE 1 — Game Design & Regras de Negócio (Consolidada)
- Definição das 4 facções principais (+ estado neutro `NONE`):
  1. `ERRANTES_DO_VEU` (*Errantes do Véu* — Neutro)
  2. `JURAMENTADOS_DA_LUZ` (*Juramentados da Luz* — Anjos / bloqueia contratos demoníacos)
  3. `IRMANDADE_DIVIDA_ETERNA` (*Irmandade da Dívida Eterna* — Demônios / bloqueia votos angelicais)
  4. `QUEBRADORES_DE_TRONOS` (*Quebradores de Tronos* — Subjugação / bônus de selamento `<= 25%` HP e Aura de Temor)
- Sistema **Murim Duplo** (Corpo e Espírito): 7 Reinos (`1..7`) x 3 Etapas (`1=Base`, `2=Fortificação`, `3=Despertar`) = 21 patamares por caminho.

---

### FASE 2 — Infraestrutura, Worldgen 2010, Triângulo de Combate e KubeJS (100% Concluída)
1. **Modlist Base Enxuta e Travada (22 Pacotes em `mods/*.pw.toml` + `mythic_contracts-1.0.0.jar`):**
   - **Performance (4):** `Embeddium 0.3.31+mc1.20.1`, `FerriteCore 6.0.1`, `ModernFix 5.28.0+mc1.20.1`, `Entity Culling 1.12.1`
   - **Worldgen Urbano 2010 (1):** `The Lost Cities 1.20-7.5.7`
   - **Combate Melee (4):** `Better Combat 1.9.0+1.20.1-forge`, `playerAnimator 1.0.2-rc1+1.20-forge`, `Cloth Config API 11.1.136+forge`, `Simply Swords 1.56.0-1.20.1`
   - **Armas de Fogo (1):** `Timeless and Classics Zero (TaCZ) 1.2.0-hotfix`
   - **Magia (4):** `Iron's Spells 'n Spellbooks 1.20.1-3.4.0.9` *(travado cirurgicamente para Forge 47.3.0)*, `GeckoLib 4.8.4`, `Curios API 5.14.1+1.20.1`, `Caelus API 3.2.0+1.20.1`
   - **Scripting & Quests (6):** `KubeJS 2001.6.5-build.26+forge`, `Rhino 2001.2.3-build.10+forge`, `Architectury API 9.2.14+forge`, `FTB Quests 2001.4.22`, `FTB Library 2001.2.13`, `FTB Teams 2001.3.2`
   - **Utilidade (2):** `JEI 15.56.0.205`, `Jade 11.13.3+forge`
2. **Worldgen Pós-Colapso Divino (`The Lost Cities`):**
   - Perfil customizado `config/lostcities/profiles/pos_guerra_2010.json` com `"public": true` (não é sobrescrito pelo `initStandardProfiles()` do mod no boot).
   - Metrópoles espaçadas (`"cityChance": 0.0012`), 65% de prédios parcialmente colapsados (`"ruinChance": 0.65`), crateras de guerra restritas às cidades (`"explosionsInCitiesOnly": true`), apagão elétrico (`"generateLighting": false`), camada de escombros e retomada vegetal de 15 anos (`"vineChance": 0.045`).
   - Selecionado automaticamente via `defaultconfigs/lostcities-server.toml` e `config/lostcities/common.toml`.
3. **Triângulo de Combate Sem Conflitos (`config/bettercombat/`):**
   - Regex `"pickaxe|^tacz:|irons_spellbooks:.*(spell_book|scroll|staff|wand|rod|cane|scepter|sceptre|crossbow)"` aplicada em `fallback_compatibility.json`, `client.json` e `client.json5`.
   - Armas do *TaCZ* atiram livremente sem acionar espadadas do *Better Combat*; cajados/grimórios do *Iron's Spells* conjuram sem swing melee indevido; espadas do *Simply Swords* e *Iron's Spells* mantêm 100% das animações do *Better Combat*.
4. **Mapeamento de Teclas (`KEYBINDS_PLAN.md` & `defaultconfigs/options.txt`):**
   - **`V`**: Reservada e vinculada à **Infusão de Mana / Qi** (`mythic_contracts`).
   - **`G`**: Reservada 100% livre para a **Habilidade Única / Arte Marcial Murim** (Fase 4).
   - **`R`**: Recarregar arma (*TaCZ*) | **`B`**: Seletor de tiro (*TaCZ*) | **`M`**: Coronhada/Zoom (*TaCZ*) | **`C`**: Deitar (*TaCZ*).
   - **`X`**: Conjurar Feitiço (*Iron's Spells*) | **`Y`**: Roda de Feitiços (*Iron's Spells*).
   - **`I`**: Inventário Curios | **`J`**: FTB Quests | **`P`**: FTB Teams | **`K`**: Feint (*Better Combat*) | **`N`**: Alternar Mineração (*Better Combat*).

---

### FASE 3 — MVP do Core Mod `mythic_contracts` + Refatoração Multiagente (100% Compilada e Validada)
O Core Mod está implementado em `src/main/java/com/mythiccontracts/` (24 classes Java) e compilado em `build/libs/mythic_contracts-1.0.0.jar` e `mods/mythic_contracts-1.0.0.jar`:

1. **Persistência Blindada (`PlayerCoreData`, `CoreDataProvider`, `ModCapabilities`, `CapabilityEvents`):**
   - Armazena em NBT: `faction`, `bodyRealm` (1–7), `bodyStage` (1–3), `spiritRealm` (1–7), `spiritStage` (1–3), `activeContracts` (`Set<ResourceLocation>`), `subjugatedEntities` (`List<ResourceLocation>`), `manaInfusionActive` e `vowBroken`.
   - **Correção de Morte no Forge 1.20.1:** `CoreDataProvider` recria automaticamente o `LazyOptional` caso tenha sido invalidado por `invalidateCaps()` na morte, além de manter backup espelhado em `Player.PERSISTED_NBT_TAG` (`mythic_contracts_core_backup`).
   - Na morte (`wasDeath == true`), preserva todos os contratos, facção, cultivo e entidades seladas, e reseta automaticamente `manaInfusionActive = false`. No retorno do The End (`wasDeath == false`), preserva tudo incluindo a vida atual.
2. **Escalada de Cultivo & Fórmula de Infusão de Mana (`CultivationBalance.java`):**
   - **Dano Físico Passivo do Corpo (`Attributes.ATTACK_DAMAGE`, `Operation.ADDITION`, UUID fixo `a7f31c90-4b2e-4d8a-9c15-778899aabbcc`):**
     $$\text{BodyPassiveBonus}(R_B, S_B) = 0.5 \times R_B \times (R_B + S_B - 1)$$
     *(De `+0.5` em 1-1 até `+31.5` em 7-3).*
   - **Fórmula Consolidada da Infusão de Mana (Tecla `V`) no `LivingHurtEvent` (mesmo hit, sem perda por i-frames e com trava anti-recursão `ThreadLocal`):**
     $$\text{Dano Final} = \text{baseDamage} + \left[\text{baseDamage} \times (0.20(R_B - 1) + 0.05 S_B)\right] + \left[1.0 \times R_S(R_S + S_S - 1)\right]$$
     *(Buff do Corpo de `+5%` a `+135%` + Bônus Espiritual plano de `+1.0` a `+63.0`).*
   - **Pressão de Reino (`#mythic_contracts:high_realm_boss`):** Sem Infusão ativa, bosses de alto reino reduzem o dano sofrido em 90% (`* 0.10F`); com Infusão ativa, o jogador perfura a barreira e aplica 100% da fórmula infundida.
3. **Contratos Ativos (`PaladinVowContract` & `LuciferPactContract`):**
   - `mythic_contracts:paladin_vow`: Absorção passiva (2 corações a cada 40 ticks); quebra de voto ao atacar qualquer `AbstractVillager` (remove contrato, marca `vowBroken = true`, aplica Fraqueza II + Cegueira por 10s); bloqueado para `IRMANDADE_DIVIDA_ETERNA`.
   - `mythic_contracts:lucifer_pact`: Modificadores permanentes com UUIDs fixos (`-8.0` em `MAX_HEALTH` via `ADDITION` e `+0.40` em `ATTACK_DAMAGE` via `MULTIPLY_BASE`); bloqueado para `JURAMENTADOS_DA_LUZ`.
4. **Selo de Subjugação (`SubjugationSealItem`) + IA Aliada + Aura de Temor (`SubjugationEvents`):**
   - Sela entidades da tag `#mythic_contracts:subjugatable` (`warden`, `wither`, `iron_golem`, `ravager`) com HP `<= 15%` (`<= 25%` para `QUEBRADORES_DE_TRONOS`).
   - **Shift + Botão Direito** invoca a última entidade selada com tag NBT `SubjugatedOwner` e injeta `FollowOwnerSubjugatedGoal`, `OwnerHurtByTargetSubjugatedGoal` e `OwnerHurtTargetSubjugatedGoal` (com bloqueio total de fogo amigo, inclusive para o `Brain` do Warden e cabeças do Wither).
   - **Aura de Temor:** Entidades em `#mythic_contracts:low_tier_celestial_fiend` (`blaze`, `vex`, `skeleton`, `wither_skeleton`) recebem `AvoidEntityGoal` e fogem de jogadores `QUEBRADORES_DE_TRONOS`.
5. **Comandos de Debug (`/contrato` ou `/core`):**
   - `/contrato status`
   - `/contrato faccao <none|errantes_do_veu|juramentados_da_luz|irmandade_divida_eterna|quebradores_de_tronos>`
   - `/contrato dar <paladin_vow|lucifer_pact>` | `/contrato remover <id>`
   - `/contrato murim <corpo|espirito> <reino 1-7> <etapa 1-3>`
6. **Sincronização GitHub + Prism Launcher em 1 Clique:**
   - Script [`SINCRONIZAR_MODPACK.bat`](file:///C:/Users/lealt/OneDrive/%C3%81rea%20de%20Trabalho/o%20modpack%20definitivo/SINCRONIZAR_MODPACK.bat) + [`tools/update_pack_index.py`](file:///C:/Users/lealt/OneDrive/%C3%81rea%20de%20Trabalho/o%20modpack%20definitivo/tools/update_pack_index.py) para puxar atualizações do GitHub, recompilar o mod Java, atualizar o índice Packwiz (`index.toml` / `pack.toml`), regerar `O_Modpack_Definitivo_Fase3.mrpack`, copiar direto para a instância do Prism Launcher e dar `git push origin main`.

---

## 3. Como Encaminhar os Próximos Passos (Roteiro para a Fase 4)

Ao abrir o diretório `C:\Users\lealt\OneDrive\Área de Trabalho\o modpack definitivo` como **Projeto/Workspace ativo**, o próximo passo natural da evolução do modpack é a **FASE 4 — Expansão Data-Driven, Habilidade Única (Tecla `G`), Dreno de Qi/Mana e HUD**:

1. **Sistema de Energia / Qi & Integração com Mana do *Iron's Spells*:**
   - Adicionar atributo/recurso de **Qi / Mana Espiritual** (ou integração direta com a barra de Mana do *Iron's Spells 'n Spellbooks*) para que a **Infusão de Mana (`V`)** tenha custo por segundo/por golpe proporcional ao Reino, desligando automaticamente quando o Qi/Mana zerar.
2. **Habilidade Única / Arte Marcial Murim na Tecla `G` (já reservada em `options.txt`):**
   - Registrar o `KeyMapping` da tecla **`G`** em `ClientKeyEvents.java` + pacote `ActivateUniqueSkillC2SPacket`.
   - Implementar a técnica ativa de cada Facção / Caminho Murim com cooldown e efeitos visuais.
3. **Contratos Data-Driven via JSON (`SimpleJsonResourceReloadListener`):**
   - Permitir criar novos contratos em `data/mythic_contracts/contracts/*.json` (ou via KubeJS/Datapacks) definindo atributos, bênçãos passivas, tabus de quebra de voto e restrições de facção sem precisar recompilar código Java a cada novo contrato.
4. **HUD Customizada de Cultivo, Contratos e Infusão (`RegisterGuiOverlaysEvent`):**
   - Renderizar no canto da tela o emblema da Facção, o Reino/Etapa do Corpo e Espírito, o indicador visual de **Infusão de Mana Ativa (`V`)**, o cooldown da **Habilidade Única (`G`)** e o contador de **Entidades Subjugadas**.
5. **Progressão In-Game de Cultivo (Breakthrough / Tribulação) & Árvore do FTB Quests:**
   - Substituir o avanço puramente via comando `/contrato murim` por pílulas/manuais/altares de cultivo e missões guiadas no **FTB Quests** ambientadas no mundo pós-guerra de 2010.
