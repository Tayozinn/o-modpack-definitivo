package com.mythiccontracts.client.screen;

import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.client.CultivationHudOverlay;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.config.HudAnchor;
import com.mythiccontracts.client.config.VisualQualityPreset;
import com.mythiccontracts.client.hud.DivineResistanceRingWidget;
import com.mythiccontracts.client.hud.HudAnimationState;
import com.mythiccontracts.client.hud.HudRenderSnapshot;
import com.mythiccontracts.client.hud.MainVitalHudWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Tela nativa de configuração da HUD dupla (HUD Principal + Anel Divino [G] + Qualidade Visual)
 * com pré-visualização ao vivo, sem dependências externas (100% compatível com --offline).
 */
public class HudConfigScreen extends Screen {

    private static final int PANEL_WIDTH = 304;
    private static final int PANEL_HEIGHT = 214;
    private static final UUID PREVIEW_UUID = new UUID(0L, 42L);

    @Nullable
    private final Screen parent;
    private int activeTab = 0; // 0 = HUD Principal, 1 = Anel Divino [G], 2 = Visual & Qualidade

    private final HudAnimationState previewAnimState = new HudAnimationState();
    private final Button[] tabButtons = new Button[3];

    private int panelX;
    private int panelY;

    public HudConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("config.mythic_contracts.hud.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.panelX = Math.max(4, (this.width - PANEL_WIDTH) / 2);
        this.panelY = Math.max(4, (this.height - PANEL_HEIGHT) / 2);

        // Barra de 3 abas no cabeçalho
        int tabW = 94;
        int tabGap = 5;
        int tabsStartX = this.panelX + (PANEL_WIDTH - (tabW * 3 + tabGap * 2)) / 2;
        int tabY = this.panelY + 20;

        Component[] tabTitles = {
                Component.translatable("config.mythic_contracts.hud.tab.main"),
                Component.translatable("config.mythic_contracts.hud.tab.divine"),
                Component.translatable("config.mythic_contracts.hud.tab.visual")
        };

        for (int i = 0; i < 3; i++) {
            final int tabIdx = i;
            Button btn = Button.builder(tabTitles[i], b -> {
                        this.activeTab = tabIdx;
                        this.rebuildWidgets();
                    })
                    .bounds(tabsStartX + i * (tabW + tabGap), tabY, tabW, 18)
                    .build();
            btn.active = (this.activeTab != i);
            this.tabButtons[i] = btn;
            this.addRenderableWidget(btn);
        }

        int contentY = this.panelY + 44;
        int colW = 138;
        int leftX = this.panelX + 10;
        int rightX = this.panelX + PANEL_WIDTH - colW - 10;
        int rowStep = 19;
        int rowH = 17;

        switch (this.activeTab) {
            case 0 -> buildMainHudTab(leftX, rightX, contentY, colW, rowH, rowStep);
            case 1 -> buildDivineRingTab(leftX, rightX, contentY, colW, rowH, rowStep);
            case 2 -> buildVisualTab(leftX, rightX, contentY, colW, PANEL_WIDTH - 20, rowH, rowStep);
            default -> buildMainHudTab(leftX, rightX, contentY, colW, rowH, rowStep);
        }

        // Rodapé: Restaurar Padrão & Concluir
        int footerY = this.panelY + PANEL_HEIGHT - 26;
        int footerW = 138;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.mythic_contracts.hud.reset"),
                        b -> {
                            ClientHudConfig.resetDefaults();
                            this.rebuildWidgets();
                        })
                .bounds(leftX, footerY, footerW, 19)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("config.mythic_contracts.hud.done"),
                        b -> this.onClose())
                .bounds(rightX, footerY, footerW, 19)
                .build());
    }

    private void buildMainHudTab(int leftX, int rightX, int y0, int colW, int h, int step) {
        // Coluna esquerda: toggles de ativação e barras
        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.mainHudEnabled)
                .create(leftX, y0, colW, h, Component.translatable("config.mythic_contracts.hud.enabled"),
                        (btn, val) -> ClientHudConfig.setMainHudEnabled(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.compactMode)
                .create(leftX, y0 + step, colW, h, Component.translatable("config.mythic_contracts.hud.compact"),
                        (btn, val) -> ClientHudConfig.setCompactMode(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.showBodyHealthBar)
                .create(leftX, y0 + step * 2, colW, h, Component.translatable("config.mythic_contracts.hud.bar.body"),
                        (btn, val) -> ClientHudConfig.setShowBodyHealthBar(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.showSpiritualIntegrityBar)
                .create(leftX, y0 + step * 3, colW, h, Component.translatable("config.mythic_contracts.hud.bar.spirit"),
                        (btn, val) -> ClientHudConfig.setShowSpiritualIntegrityBar(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.showQiBar)
                .create(leftX, y0 + step * 4, colW, h, Component.translatable("config.mythic_contracts.hud.bar.qi"),
                        (btn, val) -> ClientHudConfig.setShowQiBar(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.showStaminaBar)
                .create(leftX, y0 + step * 5, colW, h, Component.translatable("config.mythic_contracts.hud.bar.stamina"),
                        (btn, val) -> ClientHudConfig.setShowStaminaBar(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.autoHideFullStamina)
                .create(leftX, y0 + step * 6, colW, h, Component.translatable("config.mythic_contracts.hud.auto_hide_stamina"),
                        (btn, val) -> ClientHudConfig.setAutoHideFullStamina(val)));

        // Coluna direita: ancoragem, offsets, escala, opacidade, corações vanilla
        this.addRenderableWidget(CycleButton.<HudAnchor>builder(a -> Component.translatable(a.getTranslationKey()))
                .withValues(HudAnchor.values())
                .withInitialValue(ClientHudConfig.mainHudAnchor)
                .create(rightX, y0, colW, h, Component.translatable("config.mythic_contracts.hud.anchor"),
                        (btn, val) -> ClientHudConfig.setMainHudAnchor(val)));

        this.addRenderableWidget(new IntSlider(
                rightX, y0 + step, colW, h,
                Component.translatable("config.mythic_contracts.hud.offset_x"),
                0, 600, ClientHudConfig.mainHudOffsetX,
                ClientHudConfig::setMainHudOffsetX
        ));

        this.addRenderableWidget(new IntSlider(
                rightX, y0 + step * 2, colW, h,
                Component.translatable("config.mythic_contracts.hud.offset_y"),
                0, 400, ClientHudConfig.mainHudOffsetY,
                ClientHudConfig::setMainHudOffsetY
        ));

        this.addRenderableWidget(new PercentSlider(
                rightX, y0 + step * 3, colW, h,
                Component.translatable("config.mythic_contracts.hud.scale"),
                0.50F, 2.00F, ClientHudConfig.mainHudScale,
                ClientHudConfig::setMainHudScale
        ));

        this.addRenderableWidget(new PercentSlider(
                rightX, y0 + step * 4, colW, h,
                Component.translatable("config.mythic_contracts.hud.opacity"),
                0.20F, 1.00F, ClientHudConfig.mainHudOpacity,
                ClientHudConfig::setMainHudOpacity
        ));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.hideVanillaHealthBar)
                .create(rightX, y0 + step * 5, colW, h, Component.translatable("config.mythic_contracts.hud.hide_vanilla_health"),
                        (btn, val) -> ClientHudConfig.setHideVanillaHealthBar(val)));
    }

    private void buildDivineRingTab(int leftX, int rightX, int y0, int colW, int h, int step) {
        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.divineRingEnabled)
                .create(leftX, y0, colW, h, Component.translatable("config.mythic_contracts.divine.enabled"),
                        (btn, val) -> ClientHudConfig.setDivineRingEnabled(val)));

        this.addRenderableWidget(CycleButton.<HudAnchor>builder(a -> Component.translatable(a.getTranslationKey()))
                .withValues(HudAnchor.values())
                .withInitialValue(ClientHudConfig.divineRingAnchor)
                .create(rightX, y0, colW, h, Component.translatable("config.mythic_contracts.hud.anchor"),
                        (btn, val) -> ClientHudConfig.setDivineRingAnchor(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.divineRingShowNumbers)
                .create(leftX, y0 + step, colW, h, Component.translatable("config.mythic_contracts.divine.show_numbers"),
                        (btn, val) -> ClientHudConfig.setDivineRingShowNumbers(val)));

        this.addRenderableWidget(new IntSlider(
                rightX, y0 + step, colW, h,
                Component.translatable("config.mythic_contracts.hud.offset_x"),
                0, 600, ClientHudConfig.divineRingOffsetX,
                ClientHudConfig::setDivineRingOffsetX
        ));

        this.addRenderableWidget(new PercentSlider(
                leftX, y0 + step * 2, colW, h,
                Component.translatable("config.mythic_contracts.hud.scale"),
                0.50F, 2.00F, ClientHudConfig.divineRingScale,
                ClientHudConfig::setDivineRingScale
        ));

        this.addRenderableWidget(new IntSlider(
                rightX, y0 + step * 2, colW, h,
                Component.translatable("config.mythic_contracts.hud.offset_y"),
                0, 400, ClientHudConfig.divineRingOffsetY,
                ClientHudConfig::setDivineRingOffsetY
        ));

        this.addRenderableWidget(new PercentSlider(
                leftX, y0 + step * 3, PANEL_WIDTH - 20, h,
                Component.translatable("config.mythic_contracts.hud.opacity"),
                0.20F, 1.00F, ClientHudConfig.divineRingOpacity,
                ClientHudConfig::setDivineRingOpacity
        ));
    }

    private void buildVisualTab(int leftX, int rightX, int y0, int colW, int fullW, int h, int step) {
        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.enableSmoothAnimations)
                .create(leftX, y0, fullW, h, Component.translatable("config.mythic_contracts.visual.animations"),
                        (btn, val) -> ClientHudConfig.setEnableSmoothAnimations(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.render3DPlayerInGui)
                .create(leftX, y0 + step, fullW, h, Component.translatable("config.mythic_contracts.visual.model_3d"),
                        (btn, val) -> ClientHudConfig.setRender3DPlayerInGui(val)));

        this.addRenderableWidget(CycleButton.<VisualQualityPreset>builder(p -> Component.translatable(p.getTranslationKey()))
                .withValues(VisualQualityPreset.values())
                .withInitialValue(ClientHudConfig.visualQualityPreset)
                .create(leftX, y0 + step * 2, fullW, h, Component.translatable("config.mythic_contracts.visual.quality"),
                        (btn, val) -> ClientHudConfig.setVisualQualityPreset(val)));

        this.addRenderableWidget(CycleButton.onOffBuilder(ClientHudConfig.enableCustomSounds)
                .create(leftX, y0 + step * 3, colW, h, Component.translatable("config.mythic_contracts.visual.custom_sounds"),
                        (btn, val) -> ClientHudConfig.setEnableCustomSounds(val)));

        this.addRenderableWidget(new PercentSlider(
                rightX, y0 + step * 3, colW, h,
                Component.translatable("config.mythic_contracts.visual.sound_volume"),
                0.0F, 1.5F, ClientHudConfig.skillSoundVolume,
                ClientHudConfig::setSkillSoundVolume
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            guiGraphics.fill(0, 0, this.width, this.height, 0x55060910);
        } else {
            guiGraphics.fillGradient(0, 0, this.width, this.height, 0xFF0A0E17, 0xFF111827);
        }

        // Renderiza preview ao vivo antes do modal para nunca bloquear cliques nem cobrir sliders
        renderLiveHudPreview(guiGraphics);

        // Card central do configurador
        guiGraphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, 0xE60B101B);
        guiGraphics.renderOutline(this.panelX, this.panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF334155);
        guiGraphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + 2, 0xFF38BDF8);

        // Título
        guiGraphics.drawCenteredString(this.font, this.title, this.panelX + PANEL_WIDTH / 2, this.panelY + 7, 0xFFF1F5F9);

        // Linha indicadora sob a aba ativa
        if (this.activeTab >= 0 && this.activeTab < this.tabButtons.length && this.tabButtons[this.activeTab] != null) {
            Button activeBtn = this.tabButtons[this.activeTab];
            guiGraphics.fill(activeBtn.getX(), activeBtn.getY() + activeBtn.getHeight() - 1,
                    activeBtn.getX() + activeBtn.getWidth(), activeBtn.getY() + activeBtn.getHeight() + 1, 0xFF38BDF8);
        }

        // Dica contextual na aba de Visual & Qualidade
        if (this.activeTab == 2) {
            String key = switch (ClientHudConfig.visualQualityPreset) {
                case LOW -> "config.mythic_contracts.visual.desc_low";
                case MEDIUM -> "config.mythic_contracts.visual.desc_medium";
                case HIGH -> "config.mythic_contracts.visual.desc_high";
            };
            guiGraphics.fill(this.panelX + 10, this.panelY + 125, this.panelX + PANEL_WIDTH - 10, this.panelY + 161, 0x88131C2E);
            guiGraphics.renderOutline(this.panelX + 10, this.panelY + 125, PANEL_WIDTH - 20, 36, 0xFF283548);
            guiGraphics.drawWordWrap(this.font, Component.translatable(key),
                    this.panelX + 16, this.panelY + 133, PANEL_WIDTH - 32, 0xFFCBD5E1);
        }

        // Separador acima do rodapé
        int sepY = this.panelY + PANEL_HEIGHT - 32;
        guiGraphics.fill(this.panelX + 10, sepY, this.panelX + PANEL_WIDTH - 10, sepY + 1, 0xAA334155);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (this.panelY + PANEL_HEIGHT + 14 < this.height) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("config.mythic_contracts.hud.preview_hint"),
                    this.panelX + PANEL_WIDTH / 2,
                    this.panelY + PANEL_HEIGHT + 4,
                    0xFF94A3B8
            );
        }
    }

    private void renderLiveHudPreview(GuiGraphics guiGraphics) {
        HudRenderSnapshot snap = null;
        if (this.minecraft != null && this.minecraft.player != null) {
            snap = ModCapabilities.get(this.minecraft.player)
                    .map(data -> HudRenderSnapshot.fromLivePlayer(this.minecraft.player, data))
                    .orElse(null);
        }
        if (snap == null) {
            snap = HudRenderSnapshot.createPreviewSample();
        }

        float dt = CultivationHudOverlay.computeDeltaSeconds();
        this.previewAnimState.updateSnapshot(
                PREVIEW_UUID,
                snap.bodyHealthRatio(),
                snap.spiritualRatio(),
                snap.qiRatio(),
                snap.staminaRatio(),
                snap.divineRatio(),
                snap.bodyRealm(),
                snap.bodyStage(),
                snap.bodyRealmName(),
                snap.spiritRealm(),
                snap.spiritStage(),
                snap.spiritRealmName(),
                snap.tier(),
                dt,
                ClientHudConfig.enableSmoothAnimations
        );
        this.previewAnimState.updateSpiritualState(
                snap.spiritRegenState(),
                snap.spiritualRatio(),
                dt,
                ClientHudConfig.enableSmoothAnimations
        );

        if (ClientHudConfig.mainHudEnabled) {
            MainVitalHudWidget.render(guiGraphics, this.font, snap, this.previewAnimState, this.width, this.height);
        }
        if (ClientHudConfig.divineRingEnabled) {
            DivineResistanceRingWidget.render(guiGraphics, this.font, snap, this.previewAnimState, this.width, this.height);
        }
    }

    @Override
    public void onClose() {
        ClientHudConfig.saveIfLoaded();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final Component prefix;
        private final int min;
        private final int max;
        private final Consumer<Integer> onApply;
        private int currentValue;

        private IntSlider(int x, int y, int width, int height, Component prefix, int min, int max, int initial, Consumer<Integer> onApply) {
            super(x, y, width, height, prefix, normalize(initial, min, max));
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.onApply = onApply;
            this.currentValue = Mth.clamp(initial, min, max);
            updateMessage();
        }

        private static double normalize(int val, int min, int max) {
            if (max <= min) {
                return 0.0D;
            }
            return (double) (Mth.clamp(val, min, max) - min) / (double) (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(this.prefix.getString() + ": " + this.currentValue + "px"));
        }

        @Override
        protected void applyValue() {
            this.currentValue = Mth.clamp(this.min + (int) Math.round(this.value * (this.max - this.min)), this.min, this.max);
            this.onApply.accept(this.currentValue);
        }
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final Component prefix;
        private final float min;
        private final float max;
        private final Consumer<Float> onApply;
        private float currentValue;

        private PercentSlider(int x, int y, int width, int height, Component prefix, float min, float max, float initial, Consumer<Float> onApply) {
            super(x, y, width, height, prefix, normalize(initial, min, max));
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.onApply = onApply;
            this.currentValue = Mth.clamp(initial, min, max);
            updateMessage();
        }

        private static double normalize(float val, float min, float max) {
            if (max <= min) {
                return 0.0D;
            }
            return (double) (Mth.clamp(val, min, max) - min) / (double) (max - min);
        }

        private static float snapOnePercent(float raw) {
            return Math.round(raw * 100.0F) / 100.0F;
        }

        @Override
        protected void updateMessage() {
            int pct = Math.round(this.currentValue * 100.0F);
            setMessage(Component.literal(String.format(Locale.ROOT, "%s: %d%%", this.prefix.getString(), pct)));
        }

        @Override
        protected void applyValue() {
            float raw = this.min + (float) this.value * (this.max - this.min);
            this.currentValue = Mth.clamp(snapOnePercent(raw), this.min, this.max);
            this.onApply.accept(this.currentValue);
        }
    }
}
