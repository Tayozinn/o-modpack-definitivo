# T1-1 — Pesquisa Técnica & Compatibilidade com o Modpack

## Tarefa
**ID:** T1-1 | **Fase 1 (Pesquisa Técnica & Compatibilidade com o Modpack)**  
Auditar a modlist real do modpack (`mods/*.pw.toml`, `config/`, `build.gradle`, `src/main/resources/META-INF/mods.toml`) e verificar APIs oficiais do Minecraft 1.20.1 / Forge 47.3.0 (Mojang mappings) para embasar decisões técnicas de HUD/Screen, renderização 3D de jogador, classificação de dano sem hard-dependency, persistência de configurações de cliente e efeitos visuais.

## Status
**CONCLUÍDO** — Modlist auditada (22 mods Packwiz) e todas as assinaturas de API verificadas diretamente no JAR `forge-1.20.1-47.3.0_mapped_official_1.20.1.jar` via `javap` e documentação oficial.

---

## Achados

### 0. Auditoria da Modlist e Ambiente do Projeto
- **Base do Mod (`build.gradle` / `mods.toml`):** `mythic_contracts` v1.0.0, MC `1.20.1`, Forge `47.3.0` (`versionRange="[47.2.0,)"`), Java 17, Mojang mappings (`official`), JUnit 5 (`5.10.2`), **zero** dependências obrigatórias além de `forge` e `minecraft`.
- **Configs existentes em `config/`:** apenas `bettercombat/`, `ftbquests/`, `lostcities/`. Nenhuma config de `mythic_contracts` ainda.
- **Mods relevantes presentes no modpack (`mods/*.pw.toml`, 22 mods no total):**
  - **Renderização / Performance (client):** `Embeddium 0.3.31+mc1.20.1`, `Entity Culling 1.12.1-mc1.20.1`, `ModernFix 5.28.0+mc1.20.1`, `FerriteCore 6.0.1`.
  - **Combate / Magia / Armas (both):** `Iron's Spells 'n Spellbooks 3.4.0.9`, `Better Combat 1.9.0+1.20.1`, `Simply Swords 1.56.0-1.20.1`, `[TaCZ] Timeless and Classics Zero 1.2.0-hotfix`.
  - **UI / Config / Animação / Bibliotecas (both):** `Cloth Config API 11.1.136-forge`, `GeckoLib 4.8.4`, `playerAnimator 1.0.2-rc1`, `Curios API 5.14.1`, `Architectury API 9.2.14`, `Jade 11.13.3`, `JEI 15.56.0.205`, `FTB Library/Quests/Teams`, `KubeJS 2001.6.5` + `Rhino`, `Caelus API 3.2.0`, `The Lost Cities 7.5.7`.
- **Baseline atual da HUD:** `CultivationHudOverlay` já usa `RegisterGuiOverlaysEvent` (`event.registerAboveAll("cultivation_hud", HUD_OVERLAY)`) e `GuiGraphics` (`fill`, `renderOutline`, `drawString`) no canto superior esquerdo (`8, 8`, `204x72`).

---

### 1. Renderização HUD e Screen no Forge 1.20.1 & Compatibilidade com Embeddium 0.3.31
**Classificação:** 100% **Nativo Forge / Minecraft 1.20.1** (sem dependências externas).

- **Registro de Overlay (`net.minecraftforge.client.event.RegisterGuiOverlaysEvent`, bus `MOD`, `Dist.CLIENT`):**
  - Métodos verificados: `registerAboveAll(String, IGuiOverlay)`, `registerBelowAll(String, IGuiOverlay)`, `registerAbove(ResourceLocation, String, IGuiOverlay)`, `registerBelow(ResourceLocation, String, IGuiOverlay)`.
  - Referências vanilla via `VanillaGuiOverlay.<ELEMENT>.id()` (ex.: `HOTBAR`, `PLAYER_HEALTH`, `EXPERIENCE_BAR`).
  - Assinatura `IGuiOverlay`: `(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight)`.
- **Pipeline `GuiGraphics` (verificado em `net.minecraft.client.gui.GuiGraphics`):**
  - Primitivas seguras: `fill(...)`, `fillGradient(...)`, `renderOutline(...)`, `blit(...)`, `drawString(...)`, `pose()` (`PoseStack` para `translate`/`scale`), `setColor(r, g, b, a)`, `enableScissor(minX, minY, maxX, maxY)` / `disableScissor()`.
  - **Blend/Alpha:** `RenderSystem.enableBlend()` + `RenderSystem.defaultBlendFunc()` antes de `blit` com transparência parcial ou `guiGraphics.setColor(1F, 1F, 1F, alpha)`. **Obrigatório** restaurar `guiGraphics.setColor(1F, 1F, 1F, 1F)` e `RenderSystem.disableBlend()` (ou respeitar o estado anterior) ao sair do render, e sempre parear `pose.pushPose()` / `pose.popPose()`.
  - **Animações suaves de baixo custo:** interpolar valores (ex.: barra suavizada, pulso de sobrecarga, fade) usando `partialTick` ou `Mth.lerp(dt, displayed, target)` com delta de `System.nanoTime()` / `Util.getMillis()` (com clamp de `dt <= 0.1f`) sem alocar objetos por frame (`String.format` apenas quando valores inteiros mudam ou em HUD compacta).
- **Compatibilidade com Embeddium 0.3.31 (e Oculus caso adicionado):**
  - Embeddium 0.3.31 não inclui shaders por padrão (requer Oculus), mas reescreve otimizações de batching/buffer.
  - **Regra crítica:** usar exclusivamente `GuiGraphics` e shaders core vanilla (`PositionColor`, `PositionTex` geridos internamente por `GuiGraphics`). **Nunca** vincular shaders customizados via `RenderSystem.setShader(...)` durante HUD nem manipular `RenderSystem.backupMatrixStack()` / projeção ortogonal manualmente.

---

### 2. Renderização de Modelo 3D do Jogador dentro de uma `Screen` (MC 1.20.1)
**Classificação:** 100% **Nativo Minecraft 1.20.1**.

- **Assinaturas exatas verificadas em `net.minecraft.client.gui.screens.inventory.InventoryScreen` (MC 1.20.1 / Forge 47.3.0):**
  1. `renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity)`
     - *Atenção:* os argumentos `mouseX` e `mouseY` em 1.20.1 são o **delta relativo** `(float)(x - actualMouseX)` e `(float)(y - 50 - actualMouseY)` (como no `InventoryScreen` vanilla), e **não** a sobrecarga de 10 parâmetros introduzida apenas na 1.20.2+.
  2. `renderEntityInInventoryFollowsAngle(GuiGraphics guiGraphics, int x, int y, int scale, float angleXComponent, float angleYComponent, LivingEntity entity)` — útil para rotação controlada (ex.: arrastar com o mouse ou ângulo fixo).
  3. `renderEntityInInventory(GuiGraphics guiGraphics, int x, int y, int scale, Quaternionf pose, Quaternionf cameraOrientation, LivingEntity entity)` — controle total de rotação 3D via JOML `Quaternionf`.
- **Limites de performance e compatibilidade no modpack:**
  - Renderizar `mc.player` dentro de uma `Screen` executa o pipeline completo de `EntityRenderDispatcher` (armadura, itens na mão, camadas de **Curios API**, **GeckoLib** armor/items e **playerAnimator**).
  - **Entity Culling 1.12.1** atua no frustum/oclusão do mundo; em GUI (`EntityRenderDispatcher.render`), `renderEntityInInventory` desativa sombras (`setRenderShadow(false)`) e renderiza diretamente, sem conflito conhecido, mas deve usar `guiGraphics.enableScissor(...)` ao redor da viewport do boneco para evitar vazamento de armas longas (**Simply Swords** / **TaCZ**).
  - **Flag de desativar obrigatória:** expor `renderPlayerModel3D` (boolean, padrão `true`, desativável em preset `LOW` / performance) na config de cliente; quando `false`, exibir silhueta/ícone 2D ou busto simples com custo quase zero.

---

### 3. Classificação de Dano no MC 1.20.1 & Compatibilidade Sem Hard-Dependency
**Classificação:** Base **Nativa MC 1.20.1** (`DamageType` + `TagKey<DamageType>` em datapack) com **soft-integration baseada em tags/IDs** (zero dependência de compilação).

- **Descoberta crítica verificada via `javap` em Forge 47.3.0 / MC 1.20.1:**
  - `minecraft:is_magic` **NÃO existe** em `net.minecraft.tags.DamageTypeTags` na 1.20.1.
  - `net.minecraftforge.common.Tags$DamageTypes` (`forge:is_magic`) **NÃO existe** no Forge 47.3.0 (foi introduzido posteriormente / no NeoForge como `neoforge:is_magic`).
  - As tags vanilla reais em `DamageTypeTags` (1.20.1) são: `WITCH_RESISTANT_TO` (cobre `minecraft:magic`, `minecraft:indirect_magic`, `minecraft:sonic_boom`, `minecraft:thorns`), `IS_PROJECTILE`, `IS_FIRE`, `IS_EXPLOSION`, `IS_LIGHTNING`, `IS_FREEZING`, `IS_FALL`, `IS_DROWNING`, `BYPASSES_ARMOR`, `BYPASSES_SHIELD`, etc.
- **Como os mods do modpack emitem dano (sem precisar importá-los em Java):**
  1. **Iron's Spells 'n Spellbooks 3.4.0.9:**
     - Usa `DamageType`s próprios por escola sob o namespace `irons_spellbooks` (ex.: `irons_spellbooks:fire_magic`, `ice_magic`, `lightning_magic`, `holy_magic`, `ender_magic`, `blood_magic`, `evocation_magic`, `nature_magic`, `eldritch_magic`, `heartstop`, `dragon_breath_pool`, etc.) e `SpellDamageSource` (que também responde a tags virtuais `<ns>:spell/<path>`).
     - Como `DamageSource.typeHolder().unwrapKey()` retorna `Optional<ResourceKey<DamageType>>`, checar se o namespace é `"irons_spellbooks"` ou se a tag customizada do nosso mod contém esses IDs (com `"required": false` no JSON) funciona **sem hard-dependency**.
  2. **Better Combat 1.9.0 & Simply Swords 1.56.0:**
     - Better Combat roteia os ataques corpo a corpo pelo fluxo padrão do jogador (`DamageTypes.PLAYER_ATTACK` / `minecraft:player_attack`), preservando encantamentos e atributos vanilla.
     - Simply Swords usa Better Combat para ataques físicos (também `minecraft:player_attack`); efeitos especiais de armas únicas (fogo, raio, dano mágico/runas) usam tipos vanilla (`in_fire`, `magic`, `indirect_magic`, etc.) ou `DamageSource` próprio sob namespace `simplyswords`.
  3. **TaCZ (Timeless and Classics Zero) 1.2.0-hotfix:**
     - Registra tipos de dano de projétil próprios (ex.: `tacz:bullet`, `tacz:bullet_ignore_armor`, `tacz:bullet_void`, `tacz:bullet_void_ignore_armor`) e usa a tag `tacz:bullets` / `minecraft:is_projectile`.
- **Arquitetura recomendada (100% Data-Driven e sem hard-dep):**
  - Criar tags próprias de `DamageType` em `data/mythic_contracts/tags/damage_type/`:
    - `mythic_contracts:magic_damage.json` (incluindo `#minecraft:witch_resistant_to`, `minecraft:magic`, `minecraft:indirect_magic`, e entradas opcionais `{"id": "irons_spellbooks:...", "required": false}` + fallback opcional em código para `namespace.equals("irons_spellbooks")` ou `forge:is_magic` / `c:is_magic` caso algum mod adicione).
    - `mythic_contracts:physical_melee.json` (`minecraft:player_attack`, `minecraft:mob_attack`, `minecraft:mob_attack_no_aggro`, `minecraft:sting`, `minecraft:thorns`).
    - `mythic_contracts:physical_ranged.json` (`#minecraft:is_projectile`, `{"id": "#tacz:bullets", "required": false}`, `{"id": "tacz:bullet", "required": false}`).
  - No código Java, usar `TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(MOD_ID, "..."))` e `source.is(tagKey)`. Isso permite que o modpack (ou KubeJS/datapacks) ajuste classificações sem recompilar o mod.

---

### 4. Persistência de Configurações de Cliente da HUD & Integração com Cloth Config
**Classificação:** Persistência via **Forge Nativo (`ForgeConfigSpec` / `ModConfig.Type.CLIENT`)** + Tela de Configuração **Opcional** via **Cloth Config API** (ou tela nativa própria).

- **`ForgeConfigSpec` (`ModConfig.Type.CLIENT`) vs JSON local:**
  - **Recomendado: `ForgeConfigSpec` (`ModConfig.Type.CLIENT`)**, salvo em `config/mythic_contracts-client.toml`.
  - Vantagens: validação automática de ranges (`defineInRange`, `defineEnum`), comentários legíveis em TOML, recarregamento automático via `ModConfigEvent.Loading` / `ModConfigEvent.Reloading` (atualizando cache em memória para leitura O(1) na HUD sem overhead por frame), padrão esperado por administradores de modpack (Packwiz/KubeJS/CurseForge), nunca sincronizado com servidor.
  - Desvantagem do JSON local customizado: duplicação de mecanismo, sem integração nativa com eventos de reload do Forge. (Só justificaria JSON separado para presets exportáveis/compartilháveis pelo usuário, o que não é necessário inicialmente.)
  - **Parâmetros suportados nativamente:** `hudOffsetX`, `hudOffsetY`, `hudAnchor` (enum: `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`), `hudScale` (0.5–2.0), `hudAlpha` (0.2–1.0), toggles de visibilidade por módulo (barra de Qi, Resistência, Resquício, Selos), `enableAnimations` (boolean), `renderPlayerModel3D` (boolean), `visualQualityPreset` (enum: `LOW`, `MEDIUM`, `HIGH`).
- **Integração Opcional com Cloth Config (`cloth-config-11.1.136-forge.jar`, já presente no modpack):**
  - **Licença:** `LGPL-3.0-only` (shedaniel). Compatível como dependência opcional em tempo de execução/compilação (`compileOnly` ou reflexão/classe isolada) sem contaminar a licença do mod, desde que não seja embutida/modificada (jar-in-jar).
  - **Padrão seguro sem hard-dependency:**
    1. Declarar em `mods.toml` como `mandatory=false` (ou nem declarar se totalmente opcional).
    2. Registrar o botão "Config" na lista de mods do Forge usando `ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> ...))` (assinatura verificada via `javap`).
    3. Verificar `ModList.get().isLoaded("cloth_config")` e delegar para uma classe isolada (ex.: `ClothConfigCompat.createScreen(parent)`) **nunca referenciada diretamente** fora do `if` (evitando `ClassNotFoundException` quando Cloth Config não estiver instalado).
    4. **Fallback:** se Cloth Config não estiver instalado (ou preferirmos zero dependência no `build.gradle` para manter `--offline` limpo), uma `Screen` nativa do Minecraft (`Screen` com `CycleButton`, `AbstractSliderButton` e preview ao vivo da HUD + drag-and-drop de X/Y) resolve 100% da configuração in-game sem precisar adicionar Maven do Cloth Config no `build.gradle`!

---

### 5. Viabilidade Técnica de Efeitos Visuais (Partículas Customizadas vs GeckoLib vs Nativo)
**Classificação:** **Partículas Customizadas Nativas do Forge** (recomendado como padrão) vs **GeckoLib 4.8.4** (opcional apenas se houver modelos 3D animados complexos no futuro).

- **Opção A — Partículas Customizadas Nativas (`DeferredRegister<ParticleType<?>>` + `RegisterParticleProvidersEvent`):**
  - **API verificada via `javap`:** `RegisterParticleProvidersEvent.registerSpriteSet(ParticleType<T>, SpriteParticleRegistration<T>)` (bus `MOD`, `Dist.CLIENT`), `SimpleParticleType`, `TextureSheetParticle`, `SpriteSet`, `ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT` / `PARTICLE_SHEET_LIT` / `PARTICLE_SHEET_OPAQUE`.
  - **Assets:** `assets/mythic_contracts/particles/<id>.json` referenciando PNGs em `assets/mythic_contracts/textures/particle/<frame>.png`.
  - **Vantagens:** zero dependências, compatibilidade total com Embeddium 0.3.31 (usa o atlas vanilla de partículas), baixíssimo custo de CPU/GPU, suporta controle de densidade pelo `visualQualityPreset` (`LOW`/`MEDIUM`/`HIGH`) e `Minecraft.getInstance().options.particles()`.
  - **Limitação:** sprites 2D billboard no espaço 3D (excelente para auras de Qi, faíscas do Vajra de Indra, névoa do Passo de Tezcatlipoca, feixes/anéis solares do Olho de Rá, cortes/runas da Ceifa da Morrígan).
- **Opção B — Renderização Nativa 3D (sem hard-dep):**
  - `RenderLevelStageEvent` ou `EntityRenderer` / `RenderLayer` (ex.: aura/anel geométrico simples no `PoseStack` usando `RenderType.entityTranslucent` / `beaconBeam`). Útil para efeitos de área/feixe pontuais sem precisar de biblioteca externa, respeitando sempre o `MultiBufferSource` padrão para compatibilidade com Embeddium.
- **Opção C — GeckoLib 4.8.4 (`geckolib-forge-1.20.1-4.8.4.jar`, Licença MIT):**
  - Já está no modpack, mas é voltado para **modelos 3D articulados (`.geo.json`) com animações keyframe (`.animation.json`)** de entidades, armaduras e itens — **não** é um motor de partículas ou HUD.
  - Exigiria adicionar repositório Maven e dependência no `build.gradle` (quebrando o fluxo `--offline` atual até baixar artifacts) e criar isolamentos complexos se mantido opcional.
  - **Veredito:** **Não usar GeckoLib** para HUD ou VFX de partículas/habilidades na fase atual. Manter 100% nativo Forge (`TextureSheetParticle` + `RenderLayer`/efeitos leves nativos) e reservar GeckoLib apenas se no futuro o mod introduzir entidades/bosses/armaduras 3D próprias.

---

## Riscos
1. **Assinatura incorreta de `InventoryScreen.renderEntityInInventoryFollowsMouse`:** guias da web frequentemente misturam a assinatura da 1.20.2+ (10 parâmetros) com a da 1.20.1 (7 parâmetros com `mouseX`/`mouseY` relativos). Usar a assinatura verificada acima evita erro de compilação e boneco olhando na direção errada.
2. **Tags de dano inexistentes em Forge 47.3.0:** tentar referenciar `DamageTypeTags.IS_MAGIC` ou `Tags.DamageTypes.IS_MAGIC` **não compila** no MC 1.20.1 / Forge 47.3.0. É obrigatório usar tags próprias do mod combinadas com `DamageTypeTags.WITCH_RESISTANT_TO` e entradas opcionais (`"required": false`).
3. **Vazamento de estado OpenGL na HUD com Embeddium 0.3.31:** esquecer `guiGraphics.setColor(1F, 1F, 1F, 1F)`, `pose.popPose()` ou `disableScissor()` pode corromper overlays subsequentes (Jade, hotbar, FTB Quests).
4. **Quebra do build `--offline`:** adicionar Cloth Config ou GeckoLib como `compileOnly` no `build.gradle` exige acesso à rede/Maven para baixar os JARs no cache Gradle. Uma tela nativa de config/HUD editor evita qualquer dependência externa de build.
5. **Sobreposição de HUD no modpack:** `Jade` (topo-centro), `Iron's Spells` (barra de mana/spell bar no canto inferior/superior conforme config do usuário), `TaCZ` (HUD de arma no canto inferior direito). A HUD do `mythic_contracts` precisa de âncora e offset X/Y configuráveis (já previstos).

---

## Recomendações
1. **HUD & Screen:** Manter 100% nativo (`RegisterGuiOverlaysEvent` + `GuiGraphics`), com cache de config em memória, `enableScissor` no viewport do modelo 3D, e `renderEntityInInventoryFollowsMouse` (7 parâmetros) protegido por flag `renderPlayerModel3D` (desligável no preset `LOW`).
2. **Classificação de Dano:** Implementar `DamageCategoryResolver` puro + data-driven usando tags `mythic_contracts:magic_damage`, `mythic_contracts:physical_melee`, `mythic_contracts:physical_ranged` com entradas `"required": false` para `irons_spellbooks:*` e `tacz:*`, mais fallback de namespace (`irons_spellbooks`) e `DamageTypeTags.WITCH_RESISTANT_TO`. Zero hard-dependency.
3. **Configuração de Cliente:** Usar `ForgeConfigSpec` (`ModConfig.Type.CLIENT`, arquivo `mythic_contracts-client.toml`) + `ConfigScreenHandler.ConfigScreenFactory` abrindo uma **Screen nativa própria de configuração/posicionamento de HUD** (com sliders, toggles de preset e preview ao vivo). Se no futuro desejarem Cloth Config, adicionar apenas via `compileOnly` + classe isolada com guarda `ModList.get().isLoaded("cloth_config")`.
4. **VFX / Partículas:** Implementar sistema nativo de partículas customizadas (`DeferredRegister<ParticleType<?>>` + `RegisterParticleProvidersEvent.registerSpriteSet` + `TextureSheetParticle` + sprites PNG em `assets/mythic_contracts/textures/particle/`) respeitando o preset de qualidade (`LOW`/`MEDIUM`/`HIGH`). Não introduzir GeckoLib nesta etapa.

---

## Fontes
- **Verificação direta de bytecode (fonte primária):** `forge-1.20.1-47.3.0_mapped_official_1.20.1.jar` inspecionado via `javap` (classes `InventoryScreen`, `DamageTypeTags`, `Tags`, `RegisterParticleProvidersEvent`, `RegisterGuiOverlaysEvent`, `ConfigScreenHandler$ConfigScreenFactory`, `GuiGraphics`).
- **Auditoria local do modpack:** `mods/*.pw.toml` (22 arquivos), `build.gradle`, `src/main/resources/META-INF/mods.toml`, `src/main/java/com/mythiccontracts/client/CultivationHudOverlay.java`.
- **Documentação Oficial Minecraft Forge (1.20.x):** `RegisterGuiOverlaysEvent`, `ForgeConfigSpec` (`ModConfig.Type.CLIENT`), `RegisterParticleProvidersEvent`, `DamageType` e tags de dano (`docs.minecraftforge.net`).
- **Iron's Spells 'n Spellbooks (3.4.0.9):** Wiki oficial (`iron.wiki`) e documentação de `SpellDamageSource` / `data/irons_spellbooks/damage_type/`.
- **Better Combat (1.9.0) & Simply Swords (1.56.0):** Repositórios GitHub / CurseForge (roteamento via `DamageSources.playerAttack` / `minecraft:player_attack`).
- **TaCZ (1.2.0-hotfix):** Documentação comunitária / datapacks 1.20.1 (`tacz:bullet`, tag `tacz:bullets`).
- **Embeddium (0.3.31):** Modrinth / CurseForge (compatibilidade com `GuiGraphics` / `RenderSystem` sem shaders customizados).
- **Licenças de bibliotecas opcionais no modpack:**
  - **Cloth Config API (`11.1.136-forge`):** `LGPL-3.0-only` (shedaniel).
  - **GeckoLib (`4.8.4`):** `MIT` (Gecko / EliotL).

---

## Pendências
- Nenhuma pendência bloqueante para T1-1.
- *Nota para Fase de Arquitetura (T1-2 / T2):* Confirmar com o coordenador se a tela de configuração in-game será 100% `Screen` nativa (recomendado para manter build `--offline` sem novas dependências) ou se deseja adicionar repositório Maven do Cloth Config no `build.gradle`.
