package com.mythiccontracts.client.config;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.util.Mth;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuração cliente persistida em {@code config/mythic_contracts-client.toml} via {@link ForgeConfigSpec}.
 * Valores em cache {@code volatile} garantem leitura O(1) durante a renderização de cada frame.
 * Sem imports {@code net.minecraft.client.*}.
 */
@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientHudConfig {

    public static final boolean DEFAULT_MAIN_HUD_ENABLED = true;
    public static final HudAnchor DEFAULT_MAIN_HUD_ANCHOR = HudAnchor.TOP_LEFT;
    public static final int DEFAULT_MAIN_HUD_OFFSET_X = 8;
    public static final int DEFAULT_MAIN_HUD_OFFSET_Y = 8;
    public static final float DEFAULT_MAIN_HUD_SCALE = 1.0F;
    public static final float DEFAULT_MAIN_HUD_OPACITY = 0.78F;
    public static final boolean DEFAULT_COMPACT_MODE = false;
    public static final boolean DEFAULT_SHOW_BODY_HEALTH_BAR = true;
    public static final boolean DEFAULT_SHOW_SPIRITUAL_INTEGRITY_BAR = true;
    public static final boolean DEFAULT_SHOW_QI_BAR = true;
    public static final boolean DEFAULT_SHOW_STAMINA_BAR = true;
    public static final boolean DEFAULT_AUTO_HIDE_FULL_STAMINA = false;
    public static final boolean DEFAULT_HIDE_VANILLA_HEALTH_BAR = false;

    public static final boolean DEFAULT_DIVINE_RING_ENABLED = true;
    public static final HudAnchor DEFAULT_DIVINE_RING_ANCHOR = HudAnchor.BOTTOM_LEFT;
    public static final int DEFAULT_DIVINE_RING_OFFSET_X = 10;
    public static final int DEFAULT_DIVINE_RING_OFFSET_Y = 10;
    public static final float DEFAULT_DIVINE_RING_SCALE = 1.0F;
    public static final float DEFAULT_DIVINE_RING_OPACITY = 0.90F;
    public static final boolean DEFAULT_DIVINE_RING_SHOW_NUMBERS = true;

    public static final boolean DEFAULT_ENABLE_SMOOTH_ANIMATIONS = true;
    public static final boolean DEFAULT_RENDER_3D_PLAYER_IN_GUI = true;
    public static final VisualQualityPreset DEFAULT_VISUAL_QUALITY_PRESET = VisualQualityPreset.MEDIUM;

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue MAIN_HUD_ENABLED_VAL;
    private static final ForgeConfigSpec.EnumValue<HudAnchor> MAIN_HUD_ANCHOR_VAL;
    private static final ForgeConfigSpec.IntValue MAIN_HUD_OFFSET_X_VAL;
    private static final ForgeConfigSpec.IntValue MAIN_HUD_OFFSET_Y_VAL;
    private static final ForgeConfigSpec.DoubleValue MAIN_HUD_SCALE_VAL;
    private static final ForgeConfigSpec.DoubleValue MAIN_HUD_OPACITY_VAL;
    private static final ForgeConfigSpec.BooleanValue COMPACT_MODE_VAL;
    private static final ForgeConfigSpec.BooleanValue SHOW_BODY_HEALTH_BAR_VAL;
    private static final ForgeConfigSpec.BooleanValue SHOW_SPIRITUAL_INTEGRITY_BAR_VAL;
    private static final ForgeConfigSpec.BooleanValue SHOW_QI_BAR_VAL;
    private static final ForgeConfigSpec.BooleanValue SHOW_STAMINA_BAR_VAL;
    private static final ForgeConfigSpec.BooleanValue AUTO_HIDE_FULL_STAMINA_VAL;
    private static final ForgeConfigSpec.BooleanValue HIDE_VANILLA_HEALTH_BAR_VAL;

    private static final ForgeConfigSpec.BooleanValue DIVINE_RING_ENABLED_VAL;
    private static final ForgeConfigSpec.EnumValue<HudAnchor> DIVINE_RING_ANCHOR_VAL;
    private static final ForgeConfigSpec.IntValue DIVINE_RING_OFFSET_X_VAL;
    private static final ForgeConfigSpec.IntValue DIVINE_RING_OFFSET_Y_VAL;
    private static final ForgeConfigSpec.DoubleValue DIVINE_RING_SCALE_VAL;
    private static final ForgeConfigSpec.DoubleValue DIVINE_RING_OPACITY_VAL;
    private static final ForgeConfigSpec.BooleanValue DIVINE_RING_SHOW_NUMBERS_VAL;

    private static final ForgeConfigSpec.BooleanValue ENABLE_SMOOTH_ANIMATIONS_VAL;
    private static final ForgeConfigSpec.BooleanValue RENDER_3D_PLAYER_IN_GUI_VAL;
    private static final ForgeConfigSpec.EnumValue<VisualQualityPreset> VISUAL_QUALITY_PRESET_VAL;

    // Cache em memória (leitura O(1) por frame sem acessar NightConfig)
    public static volatile boolean mainHudEnabled = DEFAULT_MAIN_HUD_ENABLED;
    public static volatile HudAnchor mainHudAnchor = DEFAULT_MAIN_HUD_ANCHOR;
    public static volatile int mainHudOffsetX = DEFAULT_MAIN_HUD_OFFSET_X;
    public static volatile int mainHudOffsetY = DEFAULT_MAIN_HUD_OFFSET_Y;
    public static volatile float mainHudScale = DEFAULT_MAIN_HUD_SCALE;
    public static volatile float mainHudOpacity = DEFAULT_MAIN_HUD_OPACITY;
    public static volatile boolean compactMode = DEFAULT_COMPACT_MODE;
    public static volatile boolean showBodyHealthBar = DEFAULT_SHOW_BODY_HEALTH_BAR;
    public static volatile boolean showSpiritualIntegrityBar = DEFAULT_SHOW_SPIRITUAL_INTEGRITY_BAR;
    public static volatile boolean showQiBar = DEFAULT_SHOW_QI_BAR;
    public static volatile boolean showStaminaBar = DEFAULT_SHOW_STAMINA_BAR;
    public static volatile boolean autoHideFullStamina = DEFAULT_AUTO_HIDE_FULL_STAMINA;
    public static volatile boolean hideVanillaHealthBar = DEFAULT_HIDE_VANILLA_HEALTH_BAR;

    public static volatile boolean divineRingEnabled = DEFAULT_DIVINE_RING_ENABLED;
    public static volatile HudAnchor divineRingAnchor = DEFAULT_DIVINE_RING_ANCHOR;
    public static volatile int divineRingOffsetX = DEFAULT_DIVINE_RING_OFFSET_X;
    public static volatile int divineRingOffsetY = DEFAULT_DIVINE_RING_OFFSET_Y;
    public static volatile float divineRingScale = DEFAULT_DIVINE_RING_SCALE;
    public static volatile float divineRingOpacity = DEFAULT_DIVINE_RING_OPACITY;
    public static volatile boolean divineRingShowNumbers = DEFAULT_DIVINE_RING_SHOW_NUMBERS;

    public static volatile boolean enableSmoothAnimations = DEFAULT_ENABLE_SMOOTH_ANIMATIONS;
    public static volatile boolean render3DPlayerInGui = DEFAULT_RENDER_3D_PLAYER_IN_GUI;
    public static volatile VisualQualityPreset visualQualityPreset = DEFAULT_VISUAL_QUALITY_PRESET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Configurações da HUD Principal (Conceito A — Minimalista Murim)")
                .push("main_hud");
        MAIN_HUD_ENABLED_VAL = builder
                .comment("Exibe o painel vital principal do Mythic Contracts.")
                .define("enabled", DEFAULT_MAIN_HUD_ENABLED);
        MAIN_HUD_ANCHOR_VAL = builder
                .comment("Canto de ancoragem da HUD principal.")
                .defineEnum("anchor", DEFAULT_MAIN_HUD_ANCHOR);
        MAIN_HUD_OFFSET_X_VAL = builder
                .comment("Deslocamento horizontal (em pixels GUI) a partir da âncora.")
                .defineInRange("offsetX", DEFAULT_MAIN_HUD_OFFSET_X, 0, 600);
        MAIN_HUD_OFFSET_Y_VAL = builder
                .comment("Deslocamento vertical (em pixels GUI) a partir da âncora.")
                .defineInRange("offsetY", DEFAULT_MAIN_HUD_OFFSET_Y, 0, 400);
        MAIN_HUD_SCALE_VAL = builder
                .comment("Escala visual do painel principal (0.5 a 2.0).")
                .defineInRange("scale", (double) DEFAULT_MAIN_HUD_SCALE, 0.5D, 2.0D);
        MAIN_HUD_OPACITY_VAL = builder
                .comment("Opacidade do fundo e componentes da HUD principal (0.2 a 1.0).")
                .defineInRange("opacity", (double) DEFAULT_MAIN_HUD_OPACITY, 0.2D, 1.0D);
        COMPACT_MODE_VAL = builder
                .comment("Ativa o modo compacto (2x2 barras reduzidas, ideal para combate intenso).")
                .define("compactMode", DEFAULT_COMPACT_MODE);
        SHOW_BODY_HEALTH_BAR_VAL = builder
                .comment("Exibe a barra de Vida Corporal.")
                .define("showBodyHealthBar", DEFAULT_SHOW_BODY_HEALTH_BAR);
        SHOW_SPIRITUAL_INTEGRITY_BAR_VAL = builder
                .comment("Exibe a barra de Integridade Espiritual.")
                .define("showSpiritualIntegrityBar", DEFAULT_SHOW_SPIRITUAL_INTEGRITY_BAR);
        SHOW_QI_BAR_VAL = builder
                .comment("Exibe a barra de Qi [V].")
                .define("showQiBar", DEFAULT_SHOW_QI_BAR);
        SHOW_STAMINA_BAR_VAL = builder
                .comment("Exibe a barra de Vigor / Resistência Física (Stamina).")
                .define("showStaminaBar", DEFAULT_SHOW_STAMINA_BAR);
        AUTO_HIDE_FULL_STAMINA_VAL = builder
                .comment("Oculta automaticamente a barra de Vigor quando permanecer em 100% por mais de 3 segundos.")
                .define("autoHideFullStamina", DEFAULT_AUTO_HIDE_FULL_STAMINA);
        HIDE_VANILLA_HEALTH_BAR_VAL = builder
                .comment("Substitui a barra de corações vanilla enquanto a barra de Vida Corporal estiver visível (desligado por padrão).")
                .define("hideVanillaHealthBar", DEFAULT_HIDE_VANILLA_HEALTH_BAR);
        builder.pop();

        builder.comment("Configurações do Emblema Circular de Resistência Divina ([G])")
                .push("divine_ring");
        DIVINE_RING_ENABLED_VAL = builder
                .comment("Exibe o emblema circular separado da Resistência Divina e Resquício [G].")
                .define("enabled", DEFAULT_DIVINE_RING_ENABLED);
        DIVINE_RING_ANCHOR_VAL = builder
                .comment("Canto de ancoragem do emblema circular de Resistência Divina.")
                .defineEnum("anchor", DEFAULT_DIVINE_RING_ANCHOR);
        DIVINE_RING_OFFSET_X_VAL = builder
                .comment("Deslocamento horizontal (em pixels GUI) do emblema circular.")
                .defineInRange("offsetX", DEFAULT_DIVINE_RING_OFFSET_X, 0, 600);
        DIVINE_RING_OFFSET_Y_VAL = builder
                .comment("Deslocamento vertical (em pixels GUI) do emblema circular.")
                .defineInRange("offsetY", DEFAULT_DIVINE_RING_OFFSET_Y, 0, 400);
        DIVINE_RING_SCALE_VAL = builder
                .comment("Escala visual do emblema circular (0.5 a 2.0).")
                .defineInRange("scale", (double) DEFAULT_DIVINE_RING_SCALE, 0.5D, 2.0D);
        DIVINE_RING_OPACITY_VAL = builder
                .comment("Opacidade do emblema circular e seu anel radial (0.2 a 1.0).")
                .defineInRange("opacity", (double) DEFAULT_DIVINE_RING_OPACITY, 0.2D, 1.0D);
        DIVINE_RING_SHOW_NUMBERS_VAL = builder
                .comment("Exibe o valor numérico compacto de Resistência Divina / contagem regressiva abaixo do emblema.")
                .define("showNumbers", DEFAULT_DIVINE_RING_SHOW_NUMBERS);
        builder.pop();

        builder.comment("Preferências visuais e qualidade de renderização")
                .push("visual");
        ENABLE_SMOOTH_ANIMATIONS_VAL = builder
                .comment("Ativa interpolação suave das barras, rastro de dano, brilhos e pulsos de alerta.")
                .define("enableSmoothAnimations", DEFAULT_ENABLE_SMOOTH_ANIMATIONS);
        RENDER_3D_PLAYER_IN_GUI_VAL = builder
                .comment("Renderiza o modelo 3D do jogador dentro da Interface do Cultivador (desative para silhueta 2D em PCs modestos).")
                .define("render3DPlayerInGui", DEFAULT_RENDER_3D_PLAYER_IN_GUI);
        VISUAL_QUALITY_PRESET_VAL = builder
                .comment("Preset de qualidade visual para efeitos (LOW, MEDIUM, HIGH).")
                .defineEnum("visualQualityPreset", DEFAULT_VISUAL_QUALITY_PRESET);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientHudConfig() {
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
        }
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            bake();
        }
    }

    public static void bake() {
        if (!SPEC.isLoaded()) {
            return;
        }
        mainHudEnabled = MAIN_HUD_ENABLED_VAL.get();
        mainHudAnchor = MAIN_HUD_ANCHOR_VAL.get();
        mainHudOffsetX = Mth.clamp(MAIN_HUD_OFFSET_X_VAL.get(), 0, 600);
        mainHudOffsetY = Mth.clamp(MAIN_HUD_OFFSET_Y_VAL.get(), 0, 400);
        mainHudScale = Mth.clamp(MAIN_HUD_SCALE_VAL.get().floatValue(), 0.5F, 2.0F);
        mainHudOpacity = Mth.clamp(MAIN_HUD_OPACITY_VAL.get().floatValue(), 0.2F, 1.0F);
        compactMode = COMPACT_MODE_VAL.get();
        showBodyHealthBar = SHOW_BODY_HEALTH_BAR_VAL.get();
        showSpiritualIntegrityBar = SHOW_SPIRITUAL_INTEGRITY_BAR_VAL.get();
        showQiBar = SHOW_QI_BAR_VAL.get();
        showStaminaBar = SHOW_STAMINA_BAR_VAL.get();
        autoHideFullStamina = AUTO_HIDE_FULL_STAMINA_VAL.get();
        hideVanillaHealthBar = HIDE_VANILLA_HEALTH_BAR_VAL.get();

        divineRingEnabled = DIVINE_RING_ENABLED_VAL.get();
        divineRingAnchor = DIVINE_RING_ANCHOR_VAL.get();
        divineRingOffsetX = Mth.clamp(DIVINE_RING_OFFSET_X_VAL.get(), 0, 600);
        divineRingOffsetY = Mth.clamp(DIVINE_RING_OFFSET_Y_VAL.get(), 0, 400);
        divineRingScale = Mth.clamp(DIVINE_RING_SCALE_VAL.get().floatValue(), 0.5F, 2.0F);
        divineRingOpacity = Mth.clamp(DIVINE_RING_OPACITY_VAL.get().floatValue(), 0.2F, 1.0F);
        divineRingShowNumbers = DIVINE_RING_SHOW_NUMBERS_VAL.get();

        enableSmoothAnimations = ENABLE_SMOOTH_ANIMATIONS_VAL.get();
        render3DPlayerInGui = RENDER_3D_PLAYER_IN_GUI_VAL.get();
        visualQualityPreset = VISUAL_QUALITY_PRESET_VAL.get();
    }

    public static void saveIfLoaded() {
        if (SPEC.isLoaded()) {
            SPEC.save();
        }
    }

    public static void resetDefaults() {
        mainHudEnabled = DEFAULT_MAIN_HUD_ENABLED;
        mainHudAnchor = DEFAULT_MAIN_HUD_ANCHOR;
        mainHudOffsetX = DEFAULT_MAIN_HUD_OFFSET_X;
        mainHudOffsetY = DEFAULT_MAIN_HUD_OFFSET_Y;
        mainHudScale = DEFAULT_MAIN_HUD_SCALE;
        mainHudOpacity = DEFAULT_MAIN_HUD_OPACITY;
        compactMode = DEFAULT_COMPACT_MODE;
        showBodyHealthBar = DEFAULT_SHOW_BODY_HEALTH_BAR;
        showSpiritualIntegrityBar = DEFAULT_SHOW_SPIRITUAL_INTEGRITY_BAR;
        showQiBar = DEFAULT_SHOW_QI_BAR;
        showStaminaBar = DEFAULT_SHOW_STAMINA_BAR;
        autoHideFullStamina = DEFAULT_AUTO_HIDE_FULL_STAMINA;
        hideVanillaHealthBar = DEFAULT_HIDE_VANILLA_HEALTH_BAR;

        divineRingEnabled = DEFAULT_DIVINE_RING_ENABLED;
        divineRingAnchor = DEFAULT_DIVINE_RING_ANCHOR;
        divineRingOffsetX = DEFAULT_DIVINE_RING_OFFSET_X;
        divineRingOffsetY = DEFAULT_DIVINE_RING_OFFSET_Y;
        divineRingScale = DEFAULT_DIVINE_RING_SCALE;
        divineRingOpacity = DEFAULT_DIVINE_RING_OPACITY;
        divineRingShowNumbers = DEFAULT_DIVINE_RING_SHOW_NUMBERS;

        enableSmoothAnimations = DEFAULT_ENABLE_SMOOTH_ANIMATIONS;
        render3DPlayerInGui = DEFAULT_RENDER_3D_PLAYER_IN_GUI;
        visualQualityPreset = DEFAULT_VISUAL_QUALITY_PRESET;

        if (SPEC.isLoaded()) {
            MAIN_HUD_ENABLED_VAL.set(DEFAULT_MAIN_HUD_ENABLED);
            MAIN_HUD_ANCHOR_VAL.set(DEFAULT_MAIN_HUD_ANCHOR);
            MAIN_HUD_OFFSET_X_VAL.set(DEFAULT_MAIN_HUD_OFFSET_X);
            MAIN_HUD_OFFSET_Y_VAL.set(DEFAULT_MAIN_HUD_OFFSET_Y);
            MAIN_HUD_SCALE_VAL.set((double) DEFAULT_MAIN_HUD_SCALE);
            MAIN_HUD_OPACITY_VAL.set((double) DEFAULT_MAIN_HUD_OPACITY);
            COMPACT_MODE_VAL.set(DEFAULT_COMPACT_MODE);
            SHOW_BODY_HEALTH_BAR_VAL.set(DEFAULT_SHOW_BODY_HEALTH_BAR);
            SHOW_SPIRITUAL_INTEGRITY_BAR_VAL.set(DEFAULT_SHOW_SPIRITUAL_INTEGRITY_BAR);
            SHOW_QI_BAR_VAL.set(DEFAULT_SHOW_QI_BAR);
            SHOW_STAMINA_BAR_VAL.set(DEFAULT_SHOW_STAMINA_BAR);
            AUTO_HIDE_FULL_STAMINA_VAL.set(DEFAULT_AUTO_HIDE_FULL_STAMINA);
            HIDE_VANILLA_HEALTH_BAR_VAL.set(DEFAULT_HIDE_VANILLA_HEALTH_BAR);

            DIVINE_RING_ENABLED_VAL.set(DEFAULT_DIVINE_RING_ENABLED);
            DIVINE_RING_ANCHOR_VAL.set(DEFAULT_DIVINE_RING_ANCHOR);
            DIVINE_RING_OFFSET_X_VAL.set(DEFAULT_DIVINE_RING_OFFSET_X);
            DIVINE_RING_OFFSET_Y_VAL.set(DEFAULT_DIVINE_RING_OFFSET_Y);
            DIVINE_RING_SCALE_VAL.set((double) DEFAULT_DIVINE_RING_SCALE);
            DIVINE_RING_OPACITY_VAL.set((double) DEFAULT_DIVINE_RING_OPACITY);
            DIVINE_RING_SHOW_NUMBERS_VAL.set(DEFAULT_DIVINE_RING_SHOW_NUMBERS);

            ENABLE_SMOOTH_ANIMATIONS_VAL.set(DEFAULT_ENABLE_SMOOTH_ANIMATIONS);
            RENDER_3D_PLAYER_IN_GUI_VAL.set(DEFAULT_RENDER_3D_PLAYER_IN_GUI);
            VISUAL_QUALITY_PRESET_VAL.set(DEFAULT_VISUAL_QUALITY_PRESET);

            SPEC.save();
        }
    }

    // Setters imediatos em memória (disco é salvo em onClose/resetDefaults)
    public static void setMainHudEnabled(boolean value) {
        mainHudEnabled = value;
        if (SPEC.isLoaded()) {
            MAIN_HUD_ENABLED_VAL.set(value);
        }
    }

    public static void setMainHudAnchor(HudAnchor value) {
        HudAnchor safe = value != null ? value : DEFAULT_MAIN_HUD_ANCHOR;
        mainHudAnchor = safe;
        if (SPEC.isLoaded()) {
            MAIN_HUD_ANCHOR_VAL.set(safe);
        }
    }

    public static void setMainHudOffsetX(int value) {
        int clamped = Mth.clamp(value, 0, 600);
        mainHudOffsetX = clamped;
        if (SPEC.isLoaded()) {
            MAIN_HUD_OFFSET_X_VAL.set(clamped);
        }
    }

    public static void setMainHudOffsetY(int value) {
        int clamped = Mth.clamp(value, 0, 400);
        mainHudOffsetY = clamped;
        if (SPEC.isLoaded()) {
            MAIN_HUD_OFFSET_Y_VAL.set(clamped);
        }
    }

    public static void setMainHudScale(float value) {
        float clamped = Mth.clamp(value, 0.5F, 2.0F);
        mainHudScale = clamped;
        if (SPEC.isLoaded()) {
            MAIN_HUD_SCALE_VAL.set((double) clamped);
        }
    }

    public static void setMainHudOpacity(float value) {
        float clamped = Mth.clamp(value, 0.2F, 1.0F);
        mainHudOpacity = clamped;
        if (SPEC.isLoaded()) {
            MAIN_HUD_OPACITY_VAL.set((double) clamped);
        }
    }

    public static void setCompactMode(boolean value) {
        compactMode = value;
        if (SPEC.isLoaded()) {
            COMPACT_MODE_VAL.set(value);
        }
    }

    public static void setShowBodyHealthBar(boolean value) {
        showBodyHealthBar = value;
        if (SPEC.isLoaded()) {
            SHOW_BODY_HEALTH_BAR_VAL.set(value);
        }
    }

    public static void setShowSpiritualIntegrityBar(boolean value) {
        showSpiritualIntegrityBar = value;
        if (SPEC.isLoaded()) {
            SHOW_SPIRITUAL_INTEGRITY_BAR_VAL.set(value);
        }
    }

    public static void setShowQiBar(boolean value) {
        showQiBar = value;
        if (SPEC.isLoaded()) {
            SHOW_QI_BAR_VAL.set(value);
        }
    }

    public static void setShowStaminaBar(boolean value) {
        showStaminaBar = value;
        if (SPEC.isLoaded()) {
            SHOW_STAMINA_BAR_VAL.set(value);
        }
    }

    public static void setAutoHideFullStamina(boolean value) {
        autoHideFullStamina = value;
        if (SPEC.isLoaded()) {
            AUTO_HIDE_FULL_STAMINA_VAL.set(value);
        }
    }

    public static void setHideVanillaHealthBar(boolean value) {
        hideVanillaHealthBar = value;
        if (SPEC.isLoaded()) {
            HIDE_VANILLA_HEALTH_BAR_VAL.set(value);
        }
    }

    public static void setDivineRingEnabled(boolean value) {
        divineRingEnabled = value;
        if (SPEC.isLoaded()) {
            DIVINE_RING_ENABLED_VAL.set(value);
        }
    }

    public static void setDivineRingAnchor(HudAnchor value) {
        HudAnchor safe = value != null ? value : DEFAULT_DIVINE_RING_ANCHOR;
        divineRingAnchor = safe;
        if (SPEC.isLoaded()) {
            DIVINE_RING_ANCHOR_VAL.set(safe);
        }
    }

    public static void setDivineRingOffsetX(int value) {
        int clamped = Mth.clamp(value, 0, 600);
        divineRingOffsetX = clamped;
        if (SPEC.isLoaded()) {
            DIVINE_RING_OFFSET_X_VAL.set(clamped);
        }
    }

    public static void setDivineRingOffsetY(int value) {
        int clamped = Mth.clamp(value, 0, 400);
        divineRingOffsetY = clamped;
        if (SPEC.isLoaded()) {
            DIVINE_RING_OFFSET_Y_VAL.set(clamped);
        }
    }

    public static void setDivineRingScale(float value) {
        float clamped = Mth.clamp(value, 0.5F, 2.0F);
        divineRingScale = clamped;
        if (SPEC.isLoaded()) {
            DIVINE_RING_SCALE_VAL.set((double) clamped);
        }
    }

    public static void setDivineRingOpacity(float value) {
        float clamped = Mth.clamp(value, 0.2F, 1.0F);
        divineRingOpacity = clamped;
        if (SPEC.isLoaded()) {
            DIVINE_RING_OPACITY_VAL.set((double) clamped);
        }
    }

    public static void setDivineRingShowNumbers(boolean value) {
        divineRingShowNumbers = value;
        if (SPEC.isLoaded()) {
            DIVINE_RING_SHOW_NUMBERS_VAL.set(value);
        }
    }

    public static void setEnableSmoothAnimations(boolean value) {
        enableSmoothAnimations = value;
        if (SPEC.isLoaded()) {
            ENABLE_SMOOTH_ANIMATIONS_VAL.set(value);
        }
    }

    public static void setRender3DPlayerInGui(boolean value) {
        render3DPlayerInGui = value;
        if (SPEC.isLoaded()) {
            RENDER_3D_PLAYER_IN_GUI_VAL.set(value);
        }
    }

    public static void setVisualQualityPreset(VisualQualityPreset value) {
        VisualQualityPreset safe = value != null ? value : DEFAULT_VISUAL_QUALITY_PRESET;
        visualQualityPreset = safe;
        if (SPEC.isLoaded()) {
            VISUAL_QUALITY_PRESET_VAL.set(safe);
        }
    }
}
