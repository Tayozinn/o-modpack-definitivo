# MYTHIC CONTRACTS 2.0 — Auditoria Técnica e Arquitetura Alvo (FASE 0)

> **Status:** Fase 0 (Auditoria e Especificação Arquitetural) Concluída.  
> **Escopo:** Análise integral do código-fonte atual (`mythic_contracts 1.0.0`) e especificação da arquitetura modular orientada a datapacks para a versão **2.0.0** no **O Modpack Definitivo**.  
> **Regra de Execução:** Nenhuma alteração de código da Fase 1+ foi iniciada neste documento.

---

## 1. Stack Técnica, Build e Dependências Reais

Inspecionado em `build.gradle`, `gradle.properties` e `src/main/resources/META-INF/mods.toml`:

- **Linguagem & Runtime:** Java 17 (`OpenJDK 17.0.15 LTS`, `JavaLanguageVersion.of(17)`).
- **Minecraft & Modloader:** Minecraft `1.20.1` (`[1.20.1,1.21)`), Forge `47.3.0` (`[47.2.0,)`, `loaderVersion="[47,)"`).
- **Mappings:** Mojang Official Mappings (`channel: 'official', version: '1.20.1'`).
- **Sistema de Build:** Gradle `8.4` + ForgeGradle `[6.0,6.2)`.
- **Dependências de Compilação (`build.gradle`):** Apenas `net.minecraftforge:forge:1.20.1-47.3.0`. Não há dependências Java de compile-time de KubeJS, FTB Quests, Iron's Spells, Better Combat ou TaCZ.
- **Testes Automatizados:** Inexistentes atualmente (`compileTestJava NO-SOURCE`).
- **Total de Classes Java Atuais:** 30 classes em `src/main/java/com/mythiccontracts/`.

---

## 2. Mapa Integral das Classes Existentes (Versão 1.0.0)

| Pacote | Classe | Responsabilidade Real Observada | Estado / Observação |
| :--- | :--- | :--- | :--- |
| `com.mythiccontracts` | `MythicContractsMod` | Ponto de entrada `@Mod`, registra `ModItems` e inicializa `ModNetworking` no `FMLCommonSetupEvent`. | Limpo, porém sem orquestração modular. |
| `capability` | `PlayerCoreData` | Armazena todos os dados do jogador em um único objeto NBT + lógica de clamp e `getMaxQi()`. | **Monolítico**; mistura dados e regras. |
| `capability` | `CoreDataProvider` | `ICapabilityProvider` + `INBTSerializable<CompoundTag>` com recriação segura de `LazyOptional` após invalidação. | Funcional; bom padrão a preservar. |
| `capability` | `ModCapabilities` | Token `PLAYER_CORE_DATA`, backup espelhado em `Player.PERSISTED_NBT_TAG` e `sync()` completo via NBT. | `sync()` envia o NBT inteiro sempre. |
| `capability` | `CapabilityEvents` | Anexa capability, preserva/restaura dados em `LivingDeathEvent`, `PlayerEvent.Clone`, login, respawn e troca de dimensão. | Blindado contra perda de dados na morte. |
| `capability` | `FactionType` | Enum com 5 valores fixos (`NONE`, `ERRANTES_DO_VEU`, `JURAMENTADOS_DA_LUZ`, `IRMANDADE_DIVIDA_ETERNA`, `QUEBRADORES_DE_TRONOS`). | **Rígido**; impede facções via datapack. |
| `cultivation` | `CultivationBalance` | Fórmulas estáticas para bônus passivo de ataque, buff de Infusão (`V`), Qi máximo, regen, dreno e custo/cooldown de `G`; aplica `BODY_CULTIVATION_ATTACK_UUID`. | Hardcoded para 7 reinos x 3 etapas. |
| `cultivation` | `UniqueSkillHandler` | Executa 4 habilidades (`Solar Judgment`, `Abyssal Harvest`, `Sovereign Pulse`, `Veil Step`) via `switch(faction)`. | **Hardcoded**; apenas 1 slot (`G`), sem árvore/passivas. |
| `contract` | `ContractDefinition` | Record imutável representando JSON de contrato (`display_name`, facções permitidas/proibidas, modificadores, absorção, regen, tabu de aldeão). | Boa base, mas efeitos/condições são campos fixos. |
| `contract` | `ContractDataReloadListener` | `SimpleJsonResourceReloadListener` na pasta `contracts`; faz parse com Gson e popula `ContractManager`. | Funcional; falta validação de dependências cruzadas. |
| `contract` | `ContractManager` | Mantém `BUILTIN_FALLBACKS` + JSONs carregados; aplica/remove modificadores, ticks periódicos e quebra de voto. | Faz varredura global em `ForgeRegistries.ATTRIBUTES` no clear. |
| `contract` | `PaladinVowContract` | Constantes e efeitos legados do Voto de Paladino (absorção, quebra ao atacar `AbstractVillager`). | Legado parcialmente absorvido pelo JSON. |
| `contract` | `LuciferPactContract` | Constantes e UUIDs legados do Pacto de Lúcifer (`-8.0` vida, `+40%` dano). | Legado parcialmente absorvido pelo JSON. |
| `entity.ai` | `FollowOwnerSubjugatedGoal` | Goal de seguir o dono e teleportar se `distance > teleportDistance`. | Funcional. |
| `entity.ai` | `OwnerHurtByTargetSubjugatedGoal` | Goal de atacar quem feriu o dono (com suporte a `Warden`). | Funcional. |
| `entity.ai` | `OwnerHurtTargetSubjugatedGoal` | Goal de atacar quem o dono atacou (com suporte a `Warden`). | Funcional. |
| `event` | `ContractGameplayEvents` | `PlayerTickEvent` (cooldown, regen/dreno de Qi, sync a cada 10 ticks, efeitos periódicos), `LivingAttackEvent` (tabu aldeão), `LivingHurtEvent` (Infusão `V` e Pressão de Reino 90%). | Mistura lógica de Qi, contratos, cultivo e combate. |
| `event` | `SubjugationEvents` | Injeta AI em mobs subjugados no `EntityJoinLevelEvent`, aplica `AvoidEntityGoal` (Aura de Temor), bloqueia fogo amigo e faz scan de alvo a cada 10 ticks para `Warden` e `WitherBoss`. | Acoplado a `FactionType.QUEBRADORES_DE_TRONOS`; scan de entidades no tick. |
| `item` | `SubjugationSealItem` | Sela `#mythic_contracts:subjugatable` (`<= 15%` HP ou `<= 25%` para Quebradores) e invoca último selado com `Shift + Botão Direito`. | Armazena apenas `ResourceLocation` do tipo (perde stats/instância). |
| `item` | `CultivationAdvancementItem` | Item parametrizado para Pílulas de Corpo/Espírito (etapa) e Tomo de Quebra de Reino (raio cosmético). | Faz "tribulação" apenas visual, sem desafio real. |
| `network` | `ModNetworking` | `SimpleChannel` (`mythic_contracts:main`, protocolo `"1"`) com 3 pacotes (`SyncCoreDataS2CPacket`, `ToggleInfusionC2SPacket`, `ActivateUniqueSkillC2SPacket`). | Funcional; falta sincronização granular (delta). |
| `network` | `SyncCoreDataS2CPacket` | Envia `CompoundTag` completo do servidor para o cliente. | Pesado para atualizações frequentes de Qi/cooldown. |
| `network` | `ToggleInfusionC2SPacket` | Alterna `manaInfusionActive` no servidor (exige `>= 5.0` Qi). | Seguro (autoridade no servidor). |
| `network` | `ActivateUniqueSkillC2SPacket` | Dispara `UniqueSkillHandler.execute()` no servidor. | Seguro (autoridade no servidor). |
| `network` | `ClientPacketHandler` | Aplica NBT recebido na capability local do cliente. | Seguro. |
| `client` | `ClientKeyEvents` | Registra `V` (`mana_infusion`) e `G` (`unique_skill`) e envia pacotes C2S. | Funcional. |
| `client` | `CultivationHudOverlay` | Renderiza painel no canto superior esquerdo via `RegisterGuiOverlaysEvent`. | Posição/layout hardcoded; acoplado a `FactionType` e `UniqueSkillHandler`. |
| `command` | `ContractDebugCommand` | Comandos `/contrato` e `/core` (`dar`, `remover`, `status`, `faccao`, `murim`). | Útil para QA; precisa evoluir para comandos modulares v2. |
| `registry` | `ModItems` | Registra os 4 itens e os adiciona às abas criativas `COMBAT` e `TOOLS_AND_UTILITIES`. | Funcional. |
| `util` | `ModTags` | Define `SUBJUGATABLE`, `HIGH_REALM_BOSS`, `LOW_TIER_CELESTIAL_FIEND`. | Funcional. |

---

## 3. Mapa de Dependências e Acoplamentos Críticos

```
ClientKeyEvents ──► C2S Packets ──► ContractGameplayEvents / UniqueSkillHandler
                                           │                      │
                                           ▼                      ▼
CultivationHudOverlay ◄── SyncS2C ◄── PlayerCoreData ◄────► FactionType (enum)
                                           ▲                      ▲
                                           │                      │
ContractDataReloadListener ──► ContractManager ───────────────────┘
                                           │
                                           ▼
                                   CultivationBalance ──► Forge Attributes
```

### Problemas Arquiteturais Identificados

1. **Acoplamento Sistêmico em `PlayerCoreData` e `FactionType`:**
   - `PlayerCoreData` é simultaneamente modelo de dados, serializador NBT e consumidor de regras (`CultivationBalance.calculateMaxQi`).
   - `FactionType` (enum) está espalhado em `PlayerCoreData`, `ContractDefinition`, `ContractManager`, `UniqueSkillHandler`, `CultivationHudOverlay`, `SubjugationSealItem`, `SubjugationEvents` e `ContractDebugCommand`. Impossível adicionar novas facções via datapack sem refatoração.
2. **Cultivo Hardcoded (`1..7` Reinos x `1..3` Etapas):**
   - `CultivationBalance` e `PlayerCoreData` assumem inteiros fixos. Não é possível criar novos reinos com nomes, estágios customizados, requisitos ou benefícios arbitrários por JSON.
3. **Ausência de Versionamento e Migração Explícita de Dados:**
   - O NBT atual não possui `DataVersion`. Se campos mudarem de inteiros (`bodyRealm = 1`) para `ResourceLocation` (`mythic_contracts:reino_inicial`), saves antigos precisam de um migrador determinístico `v1 -> v2`.
4. **Sincronização de Rede Não Granular:**
   - `ContractGameplayEvents` chama `ModCapabilities.sync(serverPlayer)` a cada 10 ticks enquanto Qi regenera/drena ou há cooldown, reenviando todo o NBT (inclusive listas de contratos e entidades subjugadas).
5. **Gerenciamento Descentralizado de Atributos:**
   - `CultivationBalance` aplica `BODY_CULTIVATION_ATTACK_UUID` e `ContractManager` aplica modificadores de contratos; `clearContractAttributeModifiers` itera todos os atributos registrados no Forge. Falta um `AttributeService` central com rastreamento por fonte (`sourceId`).
6. **Contratos com Efeitos e Tabus Fixos no Java:**
   - `ContractDefinition` aceita atributos via JSON, mas efeitos periódicos (`periodic_absorption_hearts`, `periodic_regen_amplifier`) e tabus (`break_on_attack_villager`) são campos específicos no Java, não um motor genérico de `Condition` + `Effect` + `Trigger`.
7. **Subjugação Apenas por `EntityType`:**
   - `subjugatedEntities` guarda apenas `List<ResourceLocation>`. Ao invocar, cria uma entidade nova padrão; não preserva vida máxima, atributos customizados, evolução, nem limita quantidade por jogador ou aceita ordens de combate.
8. **Tribulação Apenas Cosmética:**
   - `CultivationAdvancementItem` apenas invoca um `LightningBolt` visual e sobe o inteiro do reino, sem sistema de desafios, progresso, falha ou consequências.
9. **Escalonamento de Performance:**
   - `SubjugationEvents.onLivingTick` faz `getEntitiesOfClass` (AABB `16` blocos) a cada 10 ticks por cada Warden/Wither subjugado ativo. Em multiplayer com vários jogadores, isso deve ser otimizado/limitado.

---

## 4. Inventário do Que Deve Ser Preservado (Compatibilidade 100%)

1. **Identificadores e Conteúdo Existente:**
   - Facções: `errantes_do_veu`, `juramentados_da_luz`, `irmandade_divida_eterna`, `quebradores_de_tronos` (e estado neutro `none`).
   - Contratos: `mythic_contracts:paladin_vow`, `mythic_contracts:lucifer_pact`, `mythic_contracts:blood_oath`, `mythic_contracts:throne_breaker_seal`.
   - UUIDs legados de modificadores: `a7f31c90-4b2e-4d8a-9c15-778899aabbcc` (Cultivo Corporal), `c8a47b1e-5d92-4f10-9a3b-112233445566` e `d9b58c2f-6e03-4a21-8b4c-665544332211` (Pacto de Lúcifer).
   - Habilidades: `solar_judgment`, `abyssal_harvest`, `sovereign_pulse`, `veil_step`.
   - Itens (`ModItems`): `subjugation_seal`, `body_refinement_pill`, `spirit_condensation_pill`, `tribulation_tome`.
   - Tags (`ModTags`): `subjugatable`, `high_realm_boss`, `low_tier_celestial_fiend`.
2. **Mecânicas e Controles:**
   - Keybinds `V` (Infusão de Mana/Qi) e `G` (Habilidade Única), além de `options.txt` do modpack.
   - Fórmula base da Infusão (`V`) e Pressão de Reino (90% de redução sem Infusão contra `#high_realm_boss`).
   - Backup de segurança em `Player.PERSISTED_NBT_TAG` (`mythic_contracts_core_backup`) para sobrevivência na morte/clone.
   - Comandos `/contrato` e `/core` (mantidos como aliases/compatibilidade).
   - Integração existente com `kubejs/server_scripts/01_mythic_contracts_recipes.js` e `config/ftbquests/quests/chapters/despertar_2010.snbt`.

---

## 5. Arquitetura Alvo Modular (`com.mythiccontracts` 2.0)

O projeto permanece como um único JAR (`mythic_contracts`), organizado internamente em módulos desacoplados:

```
com.mythiccontracts
├── core/
│   ├── api/                 # Interfaces públicas e eventos internos (para módulos e KubeJS/FTB)
│   ├── capability/          # Capability Forge, PlayerDataProvider e ModCapabilities
│   ├── data/                # Schemas, validadores e gerenciador central de datapacks
│   ├── event/               # Barramento interno / hooks de lifecycle do jogador
│   ├── network/             # Canal de rede + pacotes granulares (EnergySync, FullSync, ActionC2S)
│   ├── persistence/         # Versionamento NBT (v1 -> v2) e PlayerDataMigrator
│   ├── player/              # Agregador PlayerData (CultivationData, FactionData, ContractData, SkillData, SubjugationData)
│   └── service/             # ServiceRegistry / acesso controlado entre módulos
├── attribute/               # AttributeService central (controle de modificadores por sourceId, sem duplicatas)
├── cultivation/
│   ├── realm/               # RealmDefinition & RealmReloadListener (body/spirit JSON)
│   ├── body/                # Regras e bônus específicos do Cultivo Corporal
│   ├── spirit/              # Regras e bônus específicos do Cultivo Espiritual
│   ├── energy/              # EnergyService (Qi, dreno de Infusão V, regeneração)
│   └── progression/         # CultivationService (validação e execução de avanço de estágio/reino)
├── faction/
│   ├── definition/          # FactionDefinition & FactionReloadListener (JSON)
│   ├── manager/             # FactionService (entrada, saída, restrições, benefícios)
│   ├── reputation/          # ReputationService (valor numérico -> Aliado/Neutro/Desconfiado/Hostil/Inimigo)
│   └── relationship/        # Relações entre facções e impacto em gameplay/AI
├── contract/
│   ├── definition/          # ContractDefinition v2 (condições, custos, efeitos, quebras, duração)
│   ├── condition/           # ConditionRegistry & avaliadores genéricos (faction, realm, item, etc.)
│   ├── effect/              # EffectRegistry (attribute, potion, periodic_absorption, infusion_bonus, etc.)
│   ├── validation/          # Validador de tabus/quebras e consequências
│   └── manager/             # ContractService & ContractReloadListener
├── skill/
│   ├── definition/          # SkillDefinition & SkillReloadListener (ativa/passiva, custo, cooldown, árvore)
│   ├── executor/            # SkillExecutorRegistry (SolarJudgment, AbyssalHarvest, SovereignPulse, VeilStep, genéricos)
│   ├── cooldown/            # CooldownService
│   ├── tree/                # Desbloqueio e pré-requisitos
│   └── specialization/      # Melhorias e variantes de habilidades
├── subjugation/
│   ├── storage/             # SubjugatedEntityRecord (UUID, EntityType, NBT snapshot, nível, nome, poder)
│   ├── entity/              # AI Goals, comportamento de combate e controle de fogo amigo
│   ├── command/             # Ordens de combate (Follow, Guard, Aggressive, Dismiss)
│   └── progression/         # Capacidade por reino/facção e evolução dos subordinados
├── tribulation/             # TribulationDefinition, TribulationReloadListener e TribulationService (estado ativo, objetivos, falha/sucesso)
├── integration/
│   ├── combat/              # Hooks de combate (Infusão V, Pressão de Reino, compatibilidade Better Combat/TaCZ/Simply Swords)
│   ├── irons_spells/        # Ponte opcional (ModList.isLoaded) para sinergia de Mana, sem hard dependency
│   ├── kubejs/              # Facade segura para scripts KubeJS alterarem Qi, Reinos, Facções e Contratos
│   └── ftbquests/           # Ponte opcional para checagem de requisitos/avanços
├── client/
│   ├── hud/                 # CultivationHudOverlay (configurável, lendo apenas ClientPlayerView)
│   ├── screen/              # Tela Central de Personagem (Reinos, Facção/Reputação, Contratos, Skills, Subjugados)
│   ├── render/              # Efeitos visuais/partículas no cliente
│   └── input/               # Keybindings (V, G, tecla da Tela de Personagem)
├── command/                 # Comandos /contrato e /core (refatorados para delegar aos Services)
└── registry/                # ModItems, ModMenus/Registries
```

### Contratos de Integração entre Módulos

- **Autoridade do Servidor:** Nenhum pacote C2S envia valores de dano, Qi, nível ou recompensas. O cliente envia apenas intenções (`ToggleInfusion`, `ActivateSkill(slot/id)`, `RequestTribulation`, `SubjugationOrder`).
- **Agregador `PlayerData` (Dados Puros):**
  - `CultivationData`: `ResourceLocation bodyRealmId`, `int bodyStageIndex`, `ResourceLocation spiritRealmId`, `int spiritStageIndex`, `float currentQi`, `boolean infusionActive`.
  - `FactionData`: `ResourceLocation currentFactionId`, `Map<ResourceLocation, Integer> reputations` (independente da facção atual).
  - `ContractData`: `Map<ResourceLocation, ActiveContractState>` (timestamp de início, duração restante, contagem de violações/estado), `Set<ResourceLocation> brokenContractsHistory`.
  - `SkillData`: `Set<ResourceLocation> unlockedSkills`, `List<ResourceLocation> equippedSkills`, `Map<ResourceLocation, Integer> cooldowns`.
  - `SubjugationData`: `List<SubjugatedEntityRecord> boundEntities`, `int maxCapacityBonus`.
- **Serviço Central de Atributos (`AttributeService`):**
  - Toda alteração de atributo passa por `AttributeService.applyModifiers(ServerPlayer, String sourceKey, List<AttributeSpec>)` e `AttributeService.clearSource(ServerPlayer, String sourceKey)`.
  - UUID determinístico gerado a partir de `(sourceKey + attributeId + operation)` — com tabela de mapeamento para preservar UUIDs legados (`a7f31c90...`, `c8a47b1e...`, `d9b58c2f...`).

---

## 6. Estrutura de Datapacks e Schemas JSON (2.0)

Estrutura alvo sob `data/<namespace>/`:

```
data/mythic_contracts/
├── realms/
│   ├── body/          # ex: body_realm_1.json ... body_realm_7.json
│   └── spirit/        # ex: spirit_realm_1.json ... spirit_realm_7.json
├── factions/          # errantes_do_veu.json, juramentados_da_luz.json, etc.
├── contracts/         # paladin_vow.json, lucifer_pact.json, blood_oath.json, throne_breaker_seal.json
├── skills/            # solar_judgment.json, abyssal_harvest.json, sovereign_pulse.json, veil_step.json
├── tribulations/      # tribulation_realm_2.json, etc.
└── tags/entity_types/ # subjugatable.json, high_realm_boss.json, low_tier_celestial_fiend.json
```

### Regras de Validação de Datapacks
- Todos os `ReloadListener` devem validar campos obrigatórios, registrar erros detalhados com o `ResourceLocation` do arquivo problemático via `LOGGER.error` e **isolar a entrada inválida** sem derrubar o carregamento dos demais arquivos nem o boot do servidor.
- Após o reload, um `DatapackIntegrityValidator` verifica referências cruzadas (ex: contrato referenciando facção inexistente, reino referenciando `previous_realm` inexistente) e emite avisos claros no log.

### Exemplos de Schema Alvo

**1. Reino (`realms/body/*.json` ou `realms/spirit/*.json`):**
```json
{
  "id": "mythic_contracts:body_realm_1",
  "order": 1,
  "path": "body",
  "display_name": "Corpo Mortal",
  "stages": [
    { "id": "base", "display_name": "Base", "qi_bonus": 0.0, "attributes": [{ "attribute": "minecraft:generic.attack_damage", "operation": "addition", "amount": 0.5 }] },
    { "id": "fortification", "display_name": "Fortificação", "qi_bonus": 10.0, "attributes": [{ "attribute": "minecraft:generic.attack_damage", "operation": "addition", "amount": 1.0 }] },
    { "id": "awakening", "display_name": "Despertar", "qi_bonus": 20.0, "attributes": [{ "attribute": "minecraft:generic.attack_damage", "operation": "addition", "amount": 1.5 }] }
  ],
  "requirements": {
    "previous_realm": null,
    "tribulation": null
  },
  "infusion_multiplier_base": 0.0
}
```

**2. Facção (`factions/*.json`):**
```json
{
  "id": "mythic_contracts:quebradores_de_tronos",
  "display_name": "Quebradores de Tronos",
  "description": "Caçadores de divindades e dominadores de monstros colossais.",
  "alignment": "chaotic_ambitious",
  "accent_color": "#A855F7",
  "default_skill": "mythic_contracts:sovereign_pulse",
  "subjugation_hp_threshold": 0.25,
  "subjugation_capacity_bonus": 1,
  "fear_aura_tag": "mythic_contracts:low_tier_celestial_fiend",
  "relationships": {
    "mythic_contracts:juramentados_da_luz": "hostile"
  }
}
```

**3. Contrato v2 (`contracts/*.json`):**
```json
{
  "display_name": "Voto de Paladino",
  "description": "Concede 2 corações de Absorção passiva. Quebra ao atacar aldeões.",
  "conditions": [
    { "type": "mythic_contracts:faction_not_in", "factions": ["mythic_contracts:irmandade_divida_eterna"] }
  ],
  "effects": [
    { "type": "mythic_contracts:periodic_absorption", "hearts": 4.0, "interval_ticks": 40 }
  ],
  "break_rules": [
    {
      "trigger": "mythic_contracts:attack_villager",
      "consequences": [
        { "type": "mythic_contracts:revoke_contract" },
        { "type": "mythic_contracts:apply_mob_effect", "effect": "minecraft:weakness", "duration": 200, "amplifier": 1 },
        { "type": "mythic_contracts:apply_mob_effect", "effect": "minecraft:blindness", "duration": 200, "amplifier": 0 }
      ]
    }
  ]
}
```

**4. Habilidade (`skills/*.json`):**
```json
{
  "display_name": "Pulso de Soberania",
  "kind": "active",
  "executor": "mythic_contracts:sovereign_pulse",
  "qi_cost_base": 35.0,
  "cooldown_ticks_base": 200,
  "requirements": {
    "factions": ["mythic_contracts:quebradores_de_tronos"]
  },
  "parameters": {
    "radius": 6.5,
    "base_damage": 6.5,
    "boss_multiplier": 1.5
  }
}
```

---

## 7. Plano de Migração de Dados (`v1 -> v2`)

1. **`DataVersion` no NBT:**
   - Saves da versão 1.0.0 não possuem a tag `DataVersion` (implicitamente `v1`).
   - Saves da versão 2.0.0 gravarão `DataVersion = 2`.
2. **Mapeamento Automático na Leitura (`PlayerDataMigrator`):**
   - `Faction` (`"quebradores_de_tronos"`) → `mythic_contracts:quebradores_de_tronos`.
   - `BodyRealm (1..7)` + `BodyStage (1..3)` → `mythic_contracts:body_realm_{N}` + estágio índice `BodyStage - 1`.
   - `SpiritRealm (1..7)` + `SpiritStage (1..3)` → `mythic_contracts:spirit_realm_{N}` + estágio índice `SpiritStage - 1`.
   - `ActiveContracts` (`Set<ResourceLocation>`) → `Map<ResourceLocation, ActiveContractState>` (com `vowBroken` migrado para `brokenContractsHistory`).
   - `SubjugatedEntities` (`List<ResourceLocation>`) → `List<SubjugatedEntityRecord>` (com `entityType`, UUID gerado e nível inicial `1`).
   - `CurrentQi` e `UniqueSkillCooldown` → `EnergyStorage` e `SkillData.cooldowns`.
3. **Preservação do Backup:** `Player.PERSISTED_NBT_TAG` (`mythic_contracts_core_backup`) continuará espelhando o formato atualizado e suportando leitura legada caso um jogador não tenha logado desde a v1.

---

## 8. Riscos e Mitigações

| Risco | Impacto | Mitigação |
| :--- | :--- | :--- |
| Quebra de saves existentes da Fase 4 (1.0.0) | Perda de cultivo/contratos | `PlayerDataMigrator` testado com NBT v1 real + preservação do backup em `PERSISTED_NBT_TAG`. |
| Duplicação de modificadores de atributos | Dano/Vida infinitos ou negativos | `AttributeService` central com limpeza por `sourceKey` e preservação dos UUIDs legados. |
| Dependência acidental de mods externos no compile-time | Crash em ambientes sem algum mod | Pacote `integration/*` isolado com checagem `ModList.get().isLoaded(...)` e sem hard deps no `mods.toml`. |
| Datapacks inválidos derrubando servidor | Crash no boot ou `/reload` | Try/catch isolado por arquivo nos `ReloadListener` + relatório claro de erros no log. |
| Dessincronização cliente-servidor | HUD incorreta ou exploração | Servidor é autoridade única; pacotes delta para Qi/cooldown e sync completo apenas em mudanças estruturais. |

---

## 9. Cronograma de Execução (Fases 1 a 9)

1. **FASE 1 — Core, Persistência, Versionamento e Rede Granular:** `PlayerData` modular, `DataVersion = 2`, `PlayerDataMigrator` (`v1 -> v2`), `AttributeService` central, `EnergyService` e `SyncEnergyS2CPacket`.
2. **FASE 2 — Motor Dinâmico de Cultivo:** `RealmDefinition` + `RealmReloadListener` (7 reinos x 3 etapas em JSON para Corpo e Espírito), `CultivationService`, migração de `CultivationBalance`.
3. **FASE 3 — Sistema Dinâmico de Facções e Reputação:** `FactionDefinition` + `FactionReloadListener` (4 facções em JSON), `ReputationService` (Aliado → Inimigo) e `RelationshipService`.
4. **FASE 4 — Motor Data-Driven de Contratos:** `ConditionRegistry`, `EffectRegistry`, `BreakRule`, `ContractService` e migração dos 4 contratos JSON.
5. **FASE 5 — Sistema Extensível de Habilidades:** `SkillDefinition`, `SkillReloadListener`, `SkillManager`, `SkillExecutorRegistry` (migrando as 4 habilidades existentes) e `CooldownService`.
6. **FASE 6 — Expansão de Subjugação:** `SubjugatedEntityRecord`, capacidade por jogador, ordens de combate, evolução e otimização de AI/ticks.
7. **FASE 7 — Tribulações & Centralização de Atributos:** `TribulationDefinition`, `TribulationService` (desafios reais de quebra de reino) e auditoria final do `AttributeService`.
8. **FASE 8 — Integrações & Interface:** API KubeJS, ponte FTB Quests, validação com Better Combat / TaCZ / Simply Swords / Iron's Spells, Tela Central de Personagem e HUD configurável.
9. **FASE 9 — Estabilização & QA:** Testes unitários JUnit, testes de migração NBT `v1 -> v2`, testes de datapacks inválidos, verificação de build/servidor dedicado e documentação final dos schemas JSON.
