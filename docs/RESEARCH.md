# Pesquisa Técnica e Compatibilidade — Mythic Contracts 2.0

Consolidado a partir da auditoria de bytecode (`forge-1.20.1-47.3.0_mapped_official_1.20.1.jar`) e da modlist Packwiz (`mods/*.pw.toml`, 22 mods).

## 1. HUD, Screen e Compatibilidade com Embeddium 0.3.31
- Usar exclusivamente `RegisterGuiOverlaysEvent` + `GuiGraphics` (`fill`, `fillGradient`, `renderOutline`, `blit`, `drawString`, `enableScissor`, `PoseStack`) e shaders core gerenciados pelo próprio `GuiGraphics`.
- **Nunca** depender de shaders customizados (Embeddium sem Oculus) e **sempre** restaurar `guiGraphics.setColor(1F, 1F, 1F, 1F)`, `pose.popPose()` e `disableScissor()`.

## 2. Renderização 3D do Jogador na `Screen`
- Assinatura verificada em `net.minecraft.client.gui.screens.inventory.InventoryScreen` (MC 1.20.1):
  `renderEntityInInventoryFollowsMouse(GuiGraphics guiGraphics, int x, int y, int scale, float mouseX, float mouseY, LivingEntity entity)`
  *(Em 1.20.1, `mouseX`/`mouseY` são deltas relativos `(float)(x - actualMouseX)` e `(float)(y - 50 - actualMouseY)`; a sobrecarga de 10 args só existe em 1.20.2+).*
- Envolver a viewport com `enableScissor` para conter armas longas (Simply Swords / TaCZ) e disponibilizar flag `render3DPlayerInGui` (fallback 2D).

## 3. Classificação de Dano no Forge 47.3.0 (Sem Hard-Dependency)
- `DamageTypeTags.IS_MAGIC` e `Tags.DamageTypes` **não existem** em MC 1.20.1 / Forge 47.3.0.
- Tags nativas válidas: `DamageTypeTags.WITCH_RESISTANT_TO`, `IS_PROJECTILE`, `IS_FIRE`, `BYPASSES_ARMOR`, etc.
- Soft-integration com `Iron's Spells 'n Spellbooks 3.4.0.9` e `TaCZ 1.2.0`: via tags JSON próprias com `"required": false` e checagem de namespace em `DamageSource.typeHolder().unwrapKey()`.

## 4. Configurações de Cliente e VFX
- **Configuração:** `ForgeConfigSpec` (`ModConfig.Type.CLIENT`) + `ConfigScreenHandler.ConfigScreenFactory` abrindo `Screen` nativa (evita quebrar `--offline` com Maven do Cloth Config).
- **VFX:** Partículas customizadas nativas (`RegisterParticleProvidersEvent.registerSpriteSet` + `TextureSheetParticle` + sprites PNG) sem adicionar GeckoLib como dependência.
