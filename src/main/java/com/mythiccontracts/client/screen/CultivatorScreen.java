package com.mythiccontracts.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.client.ClientKeyEvents;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.client.hud.DivineRingGeometry;
import com.mythiccontracts.client.hud.HudRenderSnapshot;
import com.mythiccontracts.contract.ContractDefinition;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.player.SubjugatedEntityRecord;
import com.mythiccontracts.core.service.SpiritualIntegrityService;
import com.mythiccontracts.core.service.VitalService;
import com.mythiccontracts.cultivation.CultivationBalance;
import com.mythiccontracts.cultivation.UniqueSkillHandler;
import com.mythiccontracts.cultivation.progression.CultivationActionService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmAttributeBonus;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.cultivation.realm.RealmStageDefinition;
import com.mythiccontracts.network.CultivatorActionC2SPacket;
import com.mythiccontracts.network.ModNetworking;
import com.mythiccontracts.registry.ModItems;
import com.mythiccontracts.skill.remnant.DivineRemnant;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance;
import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Interface do Cultivador ([U]) combinando o Conceito C (Espelho dos Meridianos & Núcleo Yin-Yang + Toggle 3D/Meridianos)
 * com a barra horizontal inferior de 7 abas do Conceito B.
 */
public class CultivatorScreen extends Screen {

    private static final int WINDOW_W = 368;
    private static final int WINDOW_H = 224;

    private static final ResourceLocation ICON_ATLAS =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/hud_icons.png");
    private static final ResourceLocation YINYANG_TEX =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/yinyang_core.png");
    private static final ResourceLocation GEAR_TEX =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/icon_gear.png");
    private static final ResourceLocation RING_FRAME_TEX =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/divine_ring_frame.png");
    private static final ResourceLocation RING_CRACKS_TEX =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/divine_ring_overload_cracks.png");
    private static final ResourceLocation EMBLEM_RA =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_eye_of_ra.png");
    private static final ResourceLocation EMBLEM_MORRIGAN =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_morrigan_reap.png");
    private static final ResourceLocation EMBLEM_INDRA =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_indras_vajra.png");
    private static final ResourceLocation EMBLEM_TEZCATLIPOCA =
            new ResourceLocation(MythicContractsMod.MOD_ID, "textures/gui/remnant_tezcatlipoca_step.png");

    private CultivatorTab activeTab = CultivatorTab.OVERVIEW;
    private boolean show3DModel = false;
    private int contractScrollOffset = 0;
    private int sealScrollOffset = 0;

    private int panelX;
    private int panelY;
    private long lastFrameMillis = -1L;
    private float animationSeconds = 0.0F;
    private float yinYangAngleDeg = 0.0F;

    @Nullable
    private Button toggleViewModeBtn;
    @Nullable
    private Button progressActionBtn;
    @Nullable
    private Button toggleInfusionBtn;
    private final Button[] equipRemnantBtns = new Button[DivineRemnant.values().length];

    public CultivatorScreen() {
        super(Component.translatable("screen.mythic_contracts.cultivator.title"));
    }

    @Override
    protected void init() {
        super.init();

        this.panelX = Math.max(4, (this.width - WINDOW_W) / 2);
        this.panelY = Math.max(4, (this.height - WINDOW_H) / 2);

        this.toggleViewModeBtn = null;
        this.progressActionBtn = null;
        this.toggleInfusionBtn = null;
        this.actionModeBreakthrough = false;
        for (int i = 0; i < this.equipRemnantBtns.length; i++) {
            this.equipRemnantBtns[i] = null;
        }

        // Botões do cabeçalho superior direito: [⚙ HUD] e [X]
        this.addRenderableWidget(Button.builder(
                        Component.literal("   HUD"),
                        b -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new com.mythiccontracts.client.screen.HudConfigScreen(this));
                            }
                        })
                .bounds(this.panelX + WINDOW_W - 64, this.panelY + 3, 42, 14)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal("X"),
                        b -> this.onClose())
                .bounds(this.panelX + WINDOW_W - 20, this.panelY + 3, 15, 14)
                .build());

        // Widgets interativos da aba ativa
        int contentX = this.panelX + 6;
        int contentY = this.panelY + 23;
        switch (this.activeTab) {
            case OVERVIEW -> {
                int centerX = contentX + 108;
                this.toggleViewModeBtn = Button.builder(
                                Component.empty(),
                                b -> {
                                    if (ClientHudConfig.render3DPlayerInGui) {
                                        this.show3DModel = !this.show3DModel;
                                    } else {
                                        this.show3DModel = false;
                                    }
                                })
                        .bounds(centerX + 20, contentY + 1, 100, 14)
                        .build();
                this.addRenderableWidget(this.toggleViewModeBtn);
            }
            case BODY -> {
                this.progressActionBtn = Button.builder(
                                Component.translatable("screen.mythic_contracts.cultivator.action.advance_stage"),
                                b -> ModNetworking.sendToServer(new CultivatorActionC2SPacket(CultivatorActionC2SPacket.Action.ADVANCE_BODY_STAGE)))
                        .bounds(contentX + 162, contentY + 143, 184, 18)
                        .build();
                this.addRenderableWidget(this.progressActionBtn);
            }
            case SPIRIT -> {
                this.progressActionBtn = Button.builder(
                                Component.translatable("screen.mythic_contracts.cultivator.action.advance_stage"),
                                b -> ModNetworking.sendToServer(new CultivatorActionC2SPacket(CultivatorActionC2SPacket.Action.ADVANCE_SPIRIT_STAGE)))
                        .bounds(contentX + 162, contentY + 143, 184, 18)
                        .build();
                this.addRenderableWidget(this.progressActionBtn);
            }
            case SKILLS -> {
                this.toggleInfusionBtn = Button.builder(
                                Component.translatable("screen.mythic_contracts.cultivator.action.toggle_infusion"),
                                b -> ModNetworking.sendToServer(new CultivatorActionC2SPacket(CultivatorActionC2SPacket.Action.TOGGLE_INFUSION)))
                        .bounds(contentX + 252, contentY + 11, 96, 18)
                        .build();
                this.addRenderableWidget(this.toggleInfusionBtn);

                DivineRemnant[] remnants = DivineRemnant.values();
                for (int i = 0; i < remnants.length; i++) {
                    final DivineRemnant remnant = remnants[i];
                    int col = i % 2;
                    int row = i / 2;
                    int cardX = contentX + 2 + col * 178;
                    int cardY = contentY + 44 + row * 63;
                    Button equipBtn = Button.builder(
                                    Component.translatable("screen.mythic_contracts.cultivator.action.equip"),
                                    b -> ModNetworking.sendToServer(new CultivatorActionC2SPacket(
                                            CultivatorActionC2SPacket.Action.EQUIP_REMNANT, remnant.getId())))
                            .bounds(cardX + 112, cardY + 43, 58, 13)
                            .build();
                    this.equipRemnantBtns[i] = equipBtn;
                    this.addRenderableWidget(equipBtn);
                }
            }
            default -> {
            }
        }

        // Barra horizontal inferior de 7 abas (Conceito B)
        CultivatorTab[] tabs = CultivatorTab.values();
        int tabCount = tabs.length;
        int tabGap = 2;
        int availW = WINDOW_W - 12;
        int tabW = (availW - tabGap * (tabCount - 1)) / tabCount;
        int totalRowW = tabW * tabCount + tabGap * (tabCount - 1);
        int tabStartX = this.panelX + (WINDOW_W - totalRowW) / 2;
        int tabY = this.panelY + WINDOW_H - 24;

        for (int i = 0; i < tabCount; i++) {
            final CultivatorTab tab = tabs[i];
            int bx = tabStartX + i * (tabW + tabGap);
            this.addRenderableWidget(new TabButton(bx, tabY, tabW, 18, tab, () -> switchTab(tab)));
        }
    }

    private void switchTab(CultivatorTab tab) {
        if (tab != null && this.activeTab != tab) {
            this.activeTab = tab;
            this.contractScrollOffset = 0;
            this.sealScrollOffset = 0;
            this.rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateAnimationClock();

        // Fundo suave sem escurecer demais o mundo
        guiGraphics.fill(0, 0, this.width, this.height, 0x7005070D);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        try {
            // Corpo principal da janela (Conceito C + moldura bronze Conceito B)
            guiGraphics.fill(this.panelX, this.panelY, this.panelX + WINDOW_W, this.panelY + WINDOW_H, 0xEA090D16);
            guiGraphics.renderOutline(this.panelX, this.panelY, WINDOW_W, WINDOW_H, 0xFF785A28);
            guiGraphics.renderOutline(this.panelX + 1, this.panelY + 1, WINDOW_W - 2, WINDOW_H - 2, 0x44C8A24A);

            Player player = this.minecraft != null ? this.minecraft.player : null;
            PlayerCoreData coreData = player != null ? ModCapabilities.get(player).orElse(null) : null;

            FactionType faction = coreData != null ? coreData.getFaction() : FactionType.NONE;
            int factionAccent = CultivatorMeridianMetrics.getFactionProfile(faction).color();

            // Cabeçalho superior
            guiGraphics.fillGradient(this.panelX + 2, this.panelY + 2, this.panelX + WINDOW_W - 2, this.panelY + 20,
                    0xFF141B29, 0xFF0B0F17);
            guiGraphics.fill(this.panelX + 2, this.panelY + 20, this.panelX + WINDOW_W - 2, this.panelY + 21, 0xFF785A28);
            guiGraphics.fill(this.panelX + 2, this.panelY + 2, this.panelX + WINDOW_W - 2, this.panelY + 3, factionAccent);

            drawTintedIcon(guiGraphics, this.panelX + 7, this.panelY + 6, 50, factionAccent);
            String titleStr = this.title.getString();
            drawScaledString(guiGraphics, titleStr, this.panelX + 19, this.panelY + 7, 0.85F, 0xFFFEF3C7, true);
            if (player != null) {
                float titleW = this.font.width(titleStr) * 0.85F;
                drawScaledString(guiGraphics, "· " + player.getName().getString(),
                        this.panelX + 24 + titleW, this.panelY + 7.5F, 0.78F, 0xFF94A3B8, false);
            }

            if (coreData != null && player != null) {
                PlayerData pData = coreData.getPlayerData();
                HudRenderSnapshot snap = HudRenderSnapshot.fromLivePlayer(player, coreData);
                updateDynamicControls(player, coreData, pData, snap);

                int contentX = this.panelX + 6;
                int contentY = this.panelY + 23;
                switch (this.activeTab) {
                    case OVERVIEW -> renderOverviewTab(guiGraphics, contentX, contentY, mouseX, mouseY, player, pData, snap);
                    case BODY -> renderCultivationPathTab(guiGraphics, contentX, contentY, player, pData, CultivationPath.BODY);
                    case SPIRIT -> renderCultivationPathTab(guiGraphics, contentX, contentY, player, pData, CultivationPath.SPIRIT);
                    case ATTRIBUTES -> renderAttributesTab(guiGraphics, contentX, contentY, player, pData, snap);
                    case SKILLS -> renderSkillsTab(guiGraphics, contentX, contentY, pData, snap);
                    case CONTRACTS -> renderContractsTab(guiGraphics, contentX, contentY, coreData, pData);
                    case FACTION -> renderFactionTab(guiGraphics, contentX, contentY, pData, faction);
                }
            } else {
                guiGraphics.drawCenteredString(this.font, Component.translatable("screen.mythic_contracts.cultivator.loading"),
                        this.panelX + WINDOW_W / 2, this.panelY + WINDOW_H / 2 - 6, 0xFF94A3B8);
            }

            // Linha separadora acima das 7 abas inferiores
            int footerSepY = this.panelY + WINDOW_H - 27;
            guiGraphics.fill(this.panelX + 4, footerSepY, this.panelX + WINDOW_W - 4, footerSepY + 1, 0xAA785A28);

            super.render(guiGraphics, mouseX, mouseY, partialTick);

            // Desenha ícone de engrenagem dentro do botão [   HUD] após super.render para ficar nítido por cima
            int gearX = this.panelX + WINDOW_W - 61;
            int gearY = this.panelY + 4;
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(GEAR_TEX, gearX, gearY, 0.0F, 0.0F, 12, 12, 12, 12);
        } finally {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    private void updateAnimationClock() {
        long now = Util.getMillis();
        if (this.lastFrameMillis < 0L) {
            this.lastFrameMillis = now;
            return;
        }
        float dt = Math.min(0.1F, Math.max(0.0F, (now - this.lastFrameMillis) / 1000.0F));
        this.lastFrameMillis = now;
        if (ClientHudConfig.enableSmoothAnimations) {
            this.animationSeconds += dt;
            Player p = this.minecraft != null ? this.minecraft.player : null;
            boolean infActive = p != null && ModCapabilities.get(p).map(PlayerCoreData::isManaInfusionActive).orElse(false);
            float degPerSec = infActive ? 30.0F : 6.0F;
            this.yinYangAngleDeg = (this.yinYangAngleDeg + degPerSec * dt) % 360.0F;
        }
    }

    private void updateDynamicControls(Player player, PlayerCoreData coreData, PlayerData pData, HudRenderSnapshot snap) {
        if (this.toggleViewModeBtn != null) {
            boolean using3D = this.show3DModel && ClientHudConfig.render3DPlayerInGui;
            String label = !ClientHudConfig.render3DPlayerInGui
                    ? "Meridianos (3D Off)"
                    : (using3D ? "[3D] / Meridianos" : "3D / [Meridianos]");
            this.toggleViewModeBtn.setMessage(Component.literal(label));
            this.toggleViewModeBtn.active = ClientHudConfig.render3DPlayerInGui;
        }

        if (this.progressActionBtn != null && (this.activeTab == CultivatorTab.BODY || this.activeTab == CultivatorTab.SPIRIT)) {
            CultivationPath path = this.activeTab == CultivatorTab.BODY ? CultivationPath.BODY : CultivationPath.SPIRIT;
            boolean readyBreakthrough = CultivationService.isReadyForBreakthrough(pData, path);
            RealmDefinition current = CultivationService.getCurrentRealm(pData, path);
            int stageNum = CultivationService.getCurrentStageNumber(pData, path);
            boolean atMaxRealmFinal = current.isFinalStage(stageNum) && RealmRegistry.getNextRealm(current) == null;
            boolean creative = player.getAbilities().instabuild;
            boolean shattered = pData.getCultivation().isSpiritShattered();

            if (atMaxRealmFinal) {
                this.progressActionBtn.setMessage(Component.translatable("screen.mythic_contracts.cultivator.action.max_reached"));
                this.progressActionBtn.active = false;
            } else if (readyBreakthrough) {
                this.progressActionBtn.setMessage(Component.translatable("screen.mythic_contracts.cultivator.action.breakthrough"));
                Item tome = ModItems.TRIBULATION_TOME.get();
                boolean hasTome = creative || countItem(player.getInventory(), tome) > 0;
                RealmDefinition next = RealmRegistry.getNextRealm(current);
                CultivationPath opp = path == CultivationPath.BODY ? CultivationPath.SPIRIT : CultivationPath.BODY;
                int oppOrder = CultivationService.getCurrentRealm(pData, opp).order();
                boolean oppOk = next == null || oppOrder >= next.requirements().minOppositeRealmOrder();
                boolean qiOk = next == null || snap.currentQi() >= next.requirements().qiCost();
                this.progressActionBtn.active = !shattered && hasTome && oppOk && qiOk;

                // Garante envio do pacote correto para Quebra de Reino
                this.progressActionBtn = rebindActionIfNeeded(this.progressActionBtn, path, true);
            } else {
                this.progressActionBtn.setMessage(Component.translatable("screen.mythic_contracts.cultivator.action.advance_stage"));
                Item pill = path == CultivationPath.BODY
                        ? ModItems.BODY_REFINEMENT_PILL.get()
                        : ModItems.SPIRIT_CONDENSATION_PILL.get();
                boolean hasPill = creative || countItem(player.getInventory(), pill) > 0;
                this.progressActionBtn.active = !shattered && hasPill;
                this.progressActionBtn = rebindActionIfNeeded(this.progressActionBtn, path, false);
            }
        }

        if (this.toggleInfusionBtn != null && this.activeTab == CultivatorTab.SKILLS) {
            boolean shattered = pData.getCultivation().isSpiritShattered();
            String msg = snap.infusionActive()
                    ? Component.translatable("screen.mythic_contracts.cultivator.action.disable_infusion").getString()
                    : Component.translatable("screen.mythic_contracts.cultivator.action.enable_infusion").getString();
            this.toggleInfusionBtn.setMessage(Component.literal(msg));
            this.toggleInfusionBtn.active = !shattered && (snap.infusionActive() || snap.currentQi() >= 5.0F);
        }

        if (this.activeTab == CultivatorTab.SKILLS) {
            DivineRemnant[] remnants = DivineRemnant.values();
            DivineRemnant equipped = UniqueSkillHandler.getEquippedRemnant(pData);
            for (int i = 0; i < remnants.length; i++) {
                Button btn = this.equipRemnantBtns[i];
                if (btn == null) {
                    continue;
                }
                DivineRemnant r = remnants[i];
                if (r == equipped) {
                    btn.setMessage(Component.translatable("screen.mythic_contracts.cultivator.action.equipped"));
                    btn.active = false;
                } else {
                    btn.setMessage(Component.translatable("screen.mythic_contracts.cultivator.action.equip"));
                    btn.active = CultivationActionService.validateEquipRemnant(pData, r).allowed();
                }
            }
        }
    }

    /**
     * Mantém a ação do botão sincronizada entre Avançar Etapa e Quebrar Reino sem recriar widgets por frame.
     */
    private boolean actionModeBreakthrough = false;

    private Button rebindActionIfNeeded(Button existing, CultivationPath path, boolean breakthrough) {
        if (this.actionModeBreakthrough == breakthrough) {
            return existing;
        }
        this.actionModeBreakthrough = breakthrough;
        this.removeWidget(existing);
        CultivatorActionC2SPacket.Action pktAction = switch (path) {
            case BODY -> breakthrough
                    ? CultivatorActionC2SPacket.Action.BREAKTHROUGH_BODY
                    : CultivatorActionC2SPacket.Action.ADVANCE_BODY_STAGE;
            case SPIRIT -> breakthrough
                    ? CultivatorActionC2SPacket.Action.BREAKTHROUGH_SPIRIT
                    : CultivatorActionC2SPacket.Action.ADVANCE_SPIRIT_STAGE;
        };
        Button replacement = Button.builder(
                        existing.getMessage(),
                        b -> ModNetworking.sendToServer(new CultivatorActionC2SPacket(pktAction)))
                .bounds(existing.getX(), existing.getY(), existing.getWidth(), existing.getHeight())
                .build();
        replacement.active = existing.active;
        this.addRenderableWidget(replacement);
        return replacement;
    }

    // -------------------------------------------------------------------------
    // ABA 0 — VISÃO GERAL (Conceito C: Meridianos + Yin-Yang + Toggle 3D)
    // -------------------------------------------------------------------------
    private void renderOverviewTab(
            GuiGraphics g, int x, int y, int mouseX, int mouseY,
            Player player, PlayerData pData, HudRenderSnapshot snap
    ) {
        // Coluna Esquerda (102 px): Cultivo Corporal + Bônus de Corpo + Meridianos
        int lx = x + 2;
        int lw = 102;

        drawCard(g, lx, y + 2, lw, 56, 0xFF475569);
        drawScaledString(g, "Cultivo Corporal", lx + 5, y + 6, 0.76F, 0xFFFCA5A5, true);
        drawOverviewMiniBar(g, lx + 5, y + 17, lw - 10, "Vida Corporal",
                snap.bodyHealthRatio(), String.format(Locale.ROOT, "%.0f/%.0f", snap.health(), snap.maxHealth()),
                0xFFEF4444, 0xFF2A1215);
        drawOverviewMiniBar(g, lx + 5, y + 36, lw - 10, "Vigor",
                snap.staminaRatio(), String.format(Locale.ROOT, "%.0f/%.0f", snap.currentStamina(), snap.maxStamina()),
                0xFF84CC16, 0xFF182412);

        double passiveDmg = CultivationBalance.calculateBodyPassiveAttackBonus(snap.bodyRealm(), snap.bodyStage());
        double bodyHpBonus = Math.max(0.0D, 2.0D * snap.bodyRealm() * (snap.bodyRealm() + snap.bodyStage() - 1) - 2.0D);
        float infMult = CultivationBalance.calculateBodyInfusionMultiplier(pData);
        drawCard(g, lx, y + 62, lw, 48, 0xFF475569);
        drawScaledString(g, "Bônus de Corpo", lx + 5, y + 66, 0.76F, 0xFFF1F5F9, true);
        drawScaledString(g, String.format(Locale.ROOT, "+%.1f Dano Físico", passiveDmg), lx + 5, y + 77, 0.70F, 0xFFFCA5A5, false);
        drawScaledString(g, String.format(Locale.ROOT, "+%.0f Vida Corporal", bodyHpBonus), lx + 5, y + 87, 0.70F, 0xFFFDE68A, false);
        drawScaledString(g, String.format(Locale.ROOT, "+%.0f%% Dano Infusão", infMult * 100.0F), lx + 5, y + 97, 0.70F, 0xFF93C5FD, false);

        int bodyPts = CultivatorMeridianMetrics.calculateBodyPoints(snap.bodyRealm(), snap.bodyStage());
        int spiritPts = CultivatorMeridianMetrics.calculateSpiritPoints(snap.spiritRealm(), snap.spiritStage());
        CultivatorMeridianMetrics.QiFlowState qiFlow = CultivatorMeridianMetrics.evaluateQiFlow(
                snap.spiritShatterTicks() > 0, snap.infusionActive(), snap.qiRatio());
        drawCard(g, lx, y + 114, lw, 54, 0xFF475569);
        drawScaledString(g, "Meridianos Ativos", lx + 5, y + 118, 0.75F, 0xFFFDE68A, true);
        drawScaledString(g, "Pontos: " + (bodyPts + spiritPts) + " / 42", lx + 5, y + 130, 0.70F, 0xFFE2E8F0, false);
        drawScaledString(g, "Corpo " + bodyPts + "/21 · Esp " + spiritPts + "/21", lx + 5, y + 140, 0.64F, 0xFF94A3B8, false);
        drawScaledString(g, "Qi: " + qiFlow.getDefaultLabel(), lx + 5, y + 152, 0.68F, qiFlow.getColor(), true);

        // Área Central (140 px): Barras verticais Corpo/Espírito + Silhueta 2D ou Avatar 3D
        int cx = x + 108;
        drawVerticalRealmBar(g, cx + 6, y + 20, 8, 126,
                CultivatorMeridianMetrics.calculateProgressRatio(snap.bodyRealm(), snap.bodyStage()),
                0xFFFBBF24, 0xFFDC2626,
                "Corpo R" + snap.bodyRealm() + "-" + snap.bodyStage() + " · " + snap.bodyRealmName(),
                0xFFF87171, true);

        int vx = cx + 20;
        int vy = y + 20;
        int vw = CultivatorMeridianMetrics.SILHOUETTE_W;
        int vh = CultivatorMeridianMetrics.SILHOUETTE_H;
        g.fill(vx, vy, vx + vw, vy + vh, 0xAA060A12);
        g.renderOutline(vx, vy, vw, vh, 0x66334155);

        if (this.show3DModel && ClientHudConfig.render3DPlayerInGui) {
            g.enableScissor(vx + 1, vy + 1, vx + vw - 1, vy + vh - 1);
            try {
                int entityX = vx + vw / 2;
                int entityY = vy + vh - 14;
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        g,
                        entityX,
                        entityY,
                        44,
                        (float) (entityX - mouseX),
                        (float) (entityY - 50 - mouseY),
                        player
                );
            } finally {
                g.disableScissor();
            }
        } else {
            renderMeridianSilhouette(g, vx, vy, snap);
        }

        drawVerticalRealmBar(g, cx + 126, y + 20, 8, 126,
                CultivatorMeridianMetrics.calculateProgressRatio(snap.spiritRealm(), snap.spiritStage()),
                0xFF22D3EE, 0xFF9333EA,
                "Esp. R" + snap.spiritRealm() + "-" + snap.spiritStage() + " · " + snap.spiritRealmName(),
                0xFFC084FC, false);

        CultivatorMeridianMetrics.EquilibriumState eq =
                CultivatorMeridianMetrics.evaluateEquilibrium(snap.bodyRealm(), snap.spiritRealm());
        g.fill(cx + 14, y + 150, cx + 126, y + 166, 0xBB0D1320);
        g.renderOutline(cx + 14, y + 150, 112, 16, HudAlpha(eq.getColor(), 0.7F));
        drawCenteredScaledString(g, "Equilíbrio: " + eq.getDefaultLabel(), cx + 70.0F, y + 154.5F, 0.70F, eq.getColor(), true);

        // Coluna Direita (102 px): Cultivo Espiritual + Emblema Divino [G] + Resumo
        int rx = x + 252;
        int rw = 102;

        drawCard(g, rx, y + 2, rw, 56, 0xFF475569);
        drawScaledString(g, "Cultivo Espiritual", rx + 5, y + 6, 0.76F, 0xFFC4B5FD, true);
        String spiritLabel = snap.spiritShatterTicks() > 0
                ? String.format(Locale.ROOT, "COLAPSO %.0fs", snap.spiritShatterTicks() / 20.0F)
                : "Int. Espiritual";
        drawOverviewMiniBar(g, rx + 5, y + 17, rw - 10, spiritLabel,
                snap.spiritualRatio(), String.format(Locale.ROOT, "%.0f/%.0f", snap.spiritualIntegrity(), snap.maxSpiritualIntegrity()),
                snap.spiritShatterTicks() > 0 ? 0xFFF43F5E : 0xFFA855F7, 0xFF21122E);
        drawOverviewMiniBar(g, rx + 5, y + 36, rw - 10, "Qi",
                snap.qiRatio(), String.format(Locale.ROOT, "%.0f/%.0f", snap.currentQi(), snap.maxQi()),
                snap.infusionActive() ? 0xFF06B6D4 : 0xFF3B82F6, 0xFF0F1D33);

        drawCard(g, rx, y + 62, rw, 62, 0xFF785A28);
        drawScaledString(g, "Resquício Divino [G]", rx + 5, y + 66, 0.70F, 0xFFFDE68A, true);
        renderMiniDivineRing(g, rx + (rw - 34) / 2, y + 76, snap);
        String remName = snap.equippedRemnant().getDisplayName() + " [" + snap.tier().getNumeral() + "]";
        drawCenteredScaledString(g, remName, rx + rw * 0.5F, y + 113.0F, 0.64F, snap.equippedRemnant().getHudColor(), true);

        drawCard(g, rx, y + 128, rw, 40, 0xFF475569);
        drawScaledString(g, "Resumo Ativo", rx + 5, y + 132, 0.75F, 0xFFF1F5F9, true);
        drawScaledString(g, "Contratos: " + pData.getContracts().getActiveContractIds().size()
                + " · Selos: " + snap.subjugatedCount(), rx + 5, y + 143, 0.66F, 0xFFC084FC, false);
        int facCol = CultivatorMeridianMetrics.getFactionProfile(snap.faction()).color();
        drawScaledString(g, snap.faction().getDisplayName(), rx + 5, y + 154, 0.65F, facCol, true);
    }

    private void renderMeridianSilhouette(GuiGraphics g, int vx, int vy, HudRenderSnapshot snap) {
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(vx, vy, 0.0F);

            // Silhueta humanoide estilo Minecraft (metade Yang carmesim à esq., metade Yin ciano à dir.)
            drawSplitBoxOutline(g, 34, 6, 32, 30, 0xFF0B0F17, 0xFFEF4444, 0xFF22D3EE);   // Cabeça
            drawSplitBoxOutline(g, 34, 37, 32, 42, 0xFF0B0F17, 0xFFEF4444, 0xFF22D3EE);  // Tronco
            // Braço esquerdo (Yang) e Braço direito (Yin)
            g.fill(18, 37, 34, 79, 0xFF0B0F17);
            g.renderOutline(18, 37, 16, 42, 0xCCEF4444);
            g.fill(66, 37, 82, 79, 0xFF0B0F17);
            g.renderOutline(66, 37, 16, 42, 0xCC22D3EE);
            // Perna esquerda (Yang) e Perna direita (Yin)
            g.fill(34, 80, 49, 120, 0xFF0B0F17);
            g.renderOutline(34, 80, 15, 40, 0xCCEF4444);
            g.fill(51, 80, 66, 120, 0xFF0B0F17);
            g.renderOutline(51, 80, 15, 40, 0xCC22D3EE);

            // Olhos discretos na silhueta
            g.fill(40, 19, 44, 24, 0xFFF87171);
            g.fill(56, 19, 60, 24, 0xFF67E8F9);

            // Canais de meridianos conectando do núcleo (50, 56) aos nós Yang e Yin
            int cx = CultivatorMeridianMetrics.YINYANG_CENTER_X;
            int cy = CultivatorMeridianMetrics.YINYANG_CENTER_Y;
            drawMeridianBranches(g, cx, cy, CultivatorMeridianMetrics.BODY_NODES, snap.bodyRealm(), 0xAAEF4444, 0x33475569);
            drawMeridianBranches(g, cx, cy, CultivatorMeridianMetrics.SPIRIT_NODES, snap.spiritRealm(), 0xAA22D3EE, 0x33475569);

            // Nós Yang (Corpo R1..R7) e Nós Yin (Espírito R1..R7)
            float pulse = 0.5F + 0.5F * Mth.sin(this.animationSeconds * 5.0F);
            drawMeridianNodes(g, CultivatorMeridianMetrics.BODY_NODES, snap.bodyRealm(), 0xFFEF4444, 0xFFFDE68A, pulse);
            drawMeridianNodes(g, CultivatorMeridianMetrics.SPIRIT_NODES, snap.spiritRealm(), 0xFFA855F7, 0xFF22D3EE, pulse);

            // Núcleo central Yin-Yang rotacionável (24x24)
            pose.pushPose();
            try {
                pose.translate(cx, cy, 0.0F);
                if (ClientHudConfig.enableSmoothAnimations) {
                    pose.mulPose(Axis.ZP.rotationDegrees(this.yinYangAngleDeg));
                }
                g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.blit(YINYANG_TEX, -12, -12, 0.0F, 0.0F, 24, 24, 24, 24);
            } finally {
                pose.popPose();
            }
        } finally {
            pose.popPose();
        }
    }

    private static void drawSplitBoxOutline(GuiGraphics g, int x, int y, int w, int h, int fillCol, int leftCol, int rightCol) {
        int midX = x + w / 2;
        g.fill(x, y, x + w, y + h, fillCol);
        // Topo
        g.fill(x, y, midX, y + 1, leftCol);
        g.fill(midX, y, x + w, y + 1, rightCol);
        // Base
        g.fill(x, y + h - 1, midX, y + h, leftCol);
        g.fill(midX, y + h - 1, x + w, y + h, rightCol);
        // Laterais e linha central sutil
        g.fill(x, y, x + 1, y + h, leftCol);
        g.fill(x + w - 1, y, x + w, y + h, rightCol);
        g.fill(midX, y + 2, midX + 1, y + h - 2, 0x3364748B);
    }

    private static void drawMeridianBranches(
            GuiGraphics g, int cx, int cy,
            List<CultivatorMeridianMetrics.MeridianNode> nodes,
            int currentRealmOrder, int activeCol, int inactiveCol
    ) {
        // R1 (0) -> R2 (1) -> R3 (2) -> Dantian/Centro
        // Centro -> R6 (5) -> R5 (4) -> R4 (3)
        // Centro -> R7 (6)
        connectNodes(g, cx, cy, nodes.get(2), currentRealmOrder >= 3 ? activeCol : inactiveCol);
        connectNodes(g, nodes.get(2), nodes.get(1), currentRealmOrder >= 2 ? activeCol : inactiveCol);
        connectNodes(g, nodes.get(1), nodes.get(0), currentRealmOrder >= 1 ? activeCol : inactiveCol);
        connectNodes(g, cx, cy, nodes.get(5), currentRealmOrder >= 6 ? activeCol : inactiveCol);
        connectNodes(g, nodes.get(5), nodes.get(4), currentRealmOrder >= 5 ? activeCol : inactiveCol);
        connectNodes(g, nodes.get(4), nodes.get(3), currentRealmOrder >= 4 ? activeCol : inactiveCol);
        connectNodes(g, cx, cy, nodes.get(6), currentRealmOrder >= 7 ? activeCol : inactiveCol);
    }

    private static void connectNodes(GuiGraphics g, int x0, int y0, CultivatorMeridianMetrics.MeridianNode b, int col) {
        drawOrthogonalLink(g, x0, y0, b.x(), b.y(), col);
    }

    private static void connectNodes(GuiGraphics g, CultivatorMeridianMetrics.MeridianNode a, CultivatorMeridianMetrics.MeridianNode b, int col) {
        drawOrthogonalLink(g, a.x(), a.y(), b.x(), b.y(), col);
    }

    private static void drawOrthogonalLink(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        g.fill(minX, y0, maxX + 1, y0 + 1, col);
        int minY = Math.min(y0, y1);
        int maxY = Math.max(y0, y1);
        g.fill(x1, minY, x1 + 1, maxY + 1, col);
    }

    private static void drawMeridianNodes(
            GuiGraphics g, List<CultivatorMeridianMetrics.MeridianNode> nodes,
            int currentRealmOrder, int outerCol, int coreCol, float pulse
    ) {
        for (CultivatorMeridianMetrics.MeridianNode node : nodes) {
            boolean unlocked = currentRealmOrder >= node.requiredRealmOrder();
            int x = node.x();
            int y = node.y();
            if (unlocked) {
                if (node.requiredRealmOrder() == currentRealmOrder) {
                    int halo = HudAlpha(outerCol, 0.35F + 0.45F * pulse);
                    g.fill(x - 3, y - 3, x + 4, y + 4, halo);
                }
                g.fill(x - 2, y - 2, x + 3, y + 3, outerCol);
                g.fill(x - 1, y - 1, x + 2, y + 2, coreCol);
            } else {
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF1E293B);
            }
        }
    }

    private void renderMiniDivineRing(GuiGraphics g, int x, int y, HudRenderSnapshot snap) {
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0.0F);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(RING_FRAME_TEX, 0, 0, 0.0F, 0.0F, 34, 34, 34, 34);

            boolean overloaded = snap.divineOverloadTicks() > 0;
            if (overloaded) {
                float ovRatio = Mth.clamp(snap.divineOverloadTicks() / (float) DivineResonanceBalance.OVERLOAD_PENALTY_TICKS, 0.0F, 1.0F);
                DivineRingGeometry.forEachActiveSpan(ovRatio, (rowY, x0, x1) -> g.fill(x0, rowY, x1, rowY + 1, 0xFFDC2626));
            } else {
                int ringCol = snap.divineRatio() < 0.30F ? 0xFFF97316 : 0xFFF59E0B;
                DivineRingGeometry.forEachActiveSpan(snap.divineRatio(), (rowY, x0, x1) -> g.fill(x0, rowY, x1, rowY + 1, ringCol));
            }

            if (overloaded) {
                g.setColor(0.55F, 0.25F, 0.25F, 1.0F);
            }
            g.blit(getRemnantTexture(snap.equippedRemnant()), 8, 8, 0.0F, 0.0F, 18, 18, 18, 18);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (overloaded) {
                g.blit(RING_CRACKS_TEX, 0, 0, 0.0F, 0.0F, 34, 34, 34, 34);
            }
        } finally {
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            pose.popPose();
        }
    }

    // -------------------------------------------------------------------------
    // ABAS 1 & 2 — CULTIVO CORPORAL / ESPIRITUAL (Reutiliza fluxo unificado)
    // -------------------------------------------------------------------------
    private void renderCultivationPathTab(
            GuiGraphics g, int x, int y, Player player, PlayerData pData, CultivationPath path
    ) {
        boolean isBody = path == CultivationPath.BODY;
        int accent = isBody ? 0xFFF87171 : 0xFFC084FC;

        RealmDefinition currentRealm = CultivationService.getCurrentRealm(pData, path);
        int currentStage = CultivationService.getCurrentStageNumber(pData, path);
        RealmStageDefinition stageDef = currentRealm.getStageByNumber(currentStage);
        List<RealmDefinition> orderedRealms = RealmRegistry.getOrderedRealms(path);

        // Coluna Esquerda: Trilha dos 7 Reinos
        int lx = x + 2;
        int lw = 148;
        drawCard(g, lx, y + 2, lw, 166, 0xFF475569);
        drawScaledString(g, "Trilha — " + path.getDisplayName(), lx + 6, y + 7, 0.78F, accent, true);

        int ry = y + 20;
        for (RealmDefinition def : orderedRealms) {
            boolean passed = def.order() < currentRealm.order();
            boolean current = def.order() == currentRealm.order();
            int rowBg = current ? HudAlpha(accent, 0.18F) : (passed ? 0x5512261E : 0x440D121C);
            int rowBorder = current ? accent : (passed ? 0xFF34D399 : 0xFF283548);
            g.fill(lx + 5, ry, lx + lw - 5, ry + 19, rowBg);
            g.renderOutline(lx + 5, ry, lw - 10, 19, rowBorder);

            String status = passed ? "✓" : (current ? "●" : "·");
            int statusCol = passed ? 0xFF34D399 : (current ? accent : 0xFF475569);
            drawScaledString(g, status, lx + 9, ry + 4, 0.72F, statusCol, current);
            drawScaledString(g, "R" + def.order() + " · " + def.displayName(),
                    lx + 16, ry + 4, 0.72F, current ? 0xFFF8FAFC : (passed ? 0xFFD1FAE5 : 0xFF64748B), current);

            if (current) {
                for (int s = 1; s <= def.getStageCount(); s++) {
                    int px = lx + lw - 24 + (s - 1) * 5;
                    g.fill(px, ry + 12, px + 3, ry + 15, s <= currentStage ? accent : 0xFF334155);
                }
            } else {
                drawRightScaledString(g, passed ? "Completo" : "Bloqueado", lx + lw - 8, ry + 11, 0.58F,
                        passed ? 0xFF34D399 : 0xFF475569, false);
            }
            ry += 21;
        }

        // Coluna Direita: Detalhes, Bônus Atuais/Futuros e Ação
        int rx = x + 154;
        int rw = 200;
        drawCard(g, rx, y + 2, rw, 166, accent);
        drawScaledString(g, "R" + currentRealm.order() + " · " + currentRealm.displayName(),
                rx + 8, y + 7, 0.85F, accent, true);
        drawScaledString(g, "Etapa " + currentStage + "/" + currentRealm.getStageCount() + " — " + stageDef.displayName(),
                rx + 8, y + 18, 0.72F, 0xFFE2E8F0, false);

        for (int s = 1; s <= currentRealm.getStageCount(); s++) {
            int px = rx + rw - 28 + (s - 1) * 7;
            if (s <= currentStage) {
                g.fill(px, y + 9, px + 5, y + 14, accent);
            } else {
                g.renderOutline(px, y + 9, 5, 5, 0xFF475569);
            }
        }

        // Bônus Atuais
        g.fill(rx + 6, y + 30, rx + rw - 6, y + 84, 0x77090D16);
        g.renderOutline(rx + 6, y + 30, rw - 12, 54, 0xFF334155);
        drawScaledString(g, "Bônus Ativos", rx + 10, y + 33, 0.72F, 0xFFFDE68A, true);

        if (isBody) {
            double hpBonus = findStageAttribute(stageDef, "max_health");
            double atkBonus = findStageAttribute(stageDef, "attack_damage");
            float maxStam = VitalService.calculateMaxStamina(pData);
            float stamRegen = VitalService.calculateStaminaRegenPerSecond(pData);
            drawScaledString(g, String.format(Locale.ROOT, "• Vida Corporal: +%.0f HP", hpBonus), rx + 10, y + 44, 0.68F, 0xFFFCA5A5, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Ataque Físico: +%.1f Dano", atkBonus), rx + 10, y + 53, 0.68F, 0xFFE2E8F0, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Infusão [V]: +%.0f%% Dano Base", stageDef.infusionMultiplier() * 100.0F),
                    rx + 10, y + 62, 0.68F, 0xFF93C5FD, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Vigor: %.0f Máx (%.1f/s)", maxStam, stamRegen),
                    rx + 10, y + 71, 0.68F, 0xFFBEF264, false);
        } else {
            float maxQi = CultivationBalance.calculateMaxQi(currentRealm.order(), currentStage, pData.getCultivation().getBodyRealm());
            float qiRegen = CultivationBalance.calculateQiRegenPerSecond(currentRealm.order(), currentStage);
            float maxSpiritInt = SpiritualIntegrityService.calculateMaxSpiritualIntegrity(pData);
            int cdTicks = CultivationBalance.calculateUniqueSkillCooldownTicks(currentRealm.order());
            drawScaledString(g, String.format(Locale.ROOT, "• Qi: %.0f Máx (%.1f/s)", maxQi, qiRegen), rx + 10, y + 44, 0.68F, 0xFF93C5FD, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Infusão [V]: +%.1f Dano Plano", stageDef.infusionFlatBonus()),
                    rx + 10, y + 53, 0.68F, 0xFF67E8F9, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Int. Espiritual: %.0f Máx", maxSpiritInt),
                    rx + 10, y + 62, 0.68F, 0xFFE9D5FF, false);
            drawScaledString(g, String.format(Locale.ROOT, "• Recarga [G]: %.1fs · Dreno: %.1f/s",
                    cdTicks / 20.0F, currentRealm.infusionDrainPerSecond()), rx + 10, y + 71, 0.68F, 0xFFFDE68A, false);
        }

        // Próximo Avanço & Requisitos
        g.fill(rx + 6, y + 88, rx + rw - 6, y + 139, 0x77090D16);
        g.renderOutline(rx + 6, y + 88, rw - 12, 51, 0xFF334155);

        boolean readyBreakthrough = CultivationService.isReadyForBreakthrough(pData, path);
        RealmDefinition nextRealm = RealmRegistry.getNextRealm(currentRealm);
        boolean atCap = currentRealm.isFinalStage(currentStage) && nextRealm == null;

        if (atCap) {
            drawScaledString(g, "Ápice Absoluto Alcançado!", rx + 10, y + 92, 0.74F, 0xFFFBBF24, true);
            drawScaledString(g, "Você atingiu o patamar máximo deste caminho.", rx + 10, y + 105, 0.66F, 0xFFCBD5E1, false);
        } else if (readyBreakthrough && nextRealm != null) {
            drawScaledString(g, "Próximo: Quebra para " + nextRealm.displayName(), rx + 10, y + 92, 0.70F, 0xFF38BDF8, true);
            int tomes = countItem(player.getInventory(), ModItems.TRIBULATION_TOME.get());
            boolean creative = player.getAbilities().instabuild;
            String itemLine = creative
                    ? "• Item: Tomo de Quebra (Modo Criativo)"
                    : "• Item: 1x Tomo de Quebra (Na bolsa: " + tomes + ")";
            drawScaledString(g, itemLine, rx + 10, y + 103, 0.65F, (creative || tomes > 0) ? 0xFF34D399 : 0xFFFCA5A5, false);

            float qiCost = nextRealm.requirements().qiCost();
            if (qiCost > 0.0F) {
                boolean qiOk = pData.getCultivation().getCurrentQi() >= qiCost;
                drawScaledString(g, String.format(Locale.ROOT, "• Qi exigido: %.0f", qiCost),
                        rx + 10, y + 113, 0.65F, qiOk ? 0xFF34D399 : 0xFFFCA5A5, false);
            } else {
                drawScaledString(g, "• Avanço disponível sem custo extra de Qi", rx + 10, y + 113, 0.65F, 0xFF94A3B8, false);
            }

            int minOpp = nextRealm.requirements().minOppositeRealmOrder();
            if (minOpp > 0) {
                CultivationPath opp = isBody ? CultivationPath.SPIRIT : CultivationPath.BODY;
                boolean oppOk = CultivationService.getCurrentRealm(pData, opp).order() >= minOpp;
                drawScaledString(g, "• Requer " + opp.getDisplayName() + " R" + minOpp + "+",
                        rx + 10, y + 123, 0.65F, oppOk ? 0xFF34D399 : 0xFFFCA5A5, false);
            }
        } else {
            RealmStageDefinition nextStage = currentRealm.getStageByNumber(currentStage + 1);
            drawScaledString(g, "Próxima Etapa: " + nextStage.displayName(), rx + 10, y + 92, 0.72F, 0xFF34D399, true);
            Item pill = isBody ? ModItems.BODY_REFINEMENT_PILL.get() : ModItems.SPIRIT_CONDENSATION_PILL.get();
            int pills = countItem(player.getInventory(), pill);
            boolean creative = player.getAbilities().instabuild;
            String itemLine = creative
                    ? "• Item: Pílula de Etapa (Modo Criativo)"
                    : "• Requer 1x " + pill.getDescription().getString() + " (Bolsa: " + pills + ")";
            drawScaledString(g, itemLine, rx + 10, y + 104, 0.64F, (creative || pills > 0) ? 0xFF34D399 : 0xFFFCA5A5, false);

            String previewGain = isBody
                    ? String.format(Locale.ROOT, "• Próximo bônus: +%.0f HP · +%.1f Dano",
                    findStageAttribute(nextStage, "max_health"), findStageAttribute(nextStage, "attack_damage"))
                    : String.format(Locale.ROOT, "• Próximo bônus: +%.1f Infusão Plana", nextStage.infusionFlatBonus());
            drawScaledString(g, previewGain, rx + 10, y + 116, 0.64F, 0xFFCBD5E1, false);
        }

        if (pData.getCultivation().isSpiritShattered()) {
            drawScaledString(g, "⚠ Colapso Espiritual ativo — progresso bloqueado!", rx + 10, y + 128, 0.64F, 0xFFF43F5E, true);
        }
    }

    // -------------------------------------------------------------------------
    // ABA 3 — ATRIBUTOS CONSOLIDADOS
    // -------------------------------------------------------------------------
    private void renderAttributesTab(
            GuiGraphics g, int x, int y, Player player, PlayerData pData, HudRenderSnapshot snap
    ) {
        int lx = x + 2;
        int rx = x + 180;
        int w = 174;
        int h = 166;

        drawCard(g, lx, y + 2, w, h, 0xFFF87171);
        drawScaledString(g, "Atributos Corporais & Combate", lx + 7, y + 7, 0.78F, 0xFFFCA5A5, true);

        double baseHp = Math.max(0.0D, 2.0D * snap.bodyRealm() * (snap.bodyRealm() + snap.bodyStage() - 1) - 2.0D);
        double totalAtk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double bodyAtk = CultivationBalance.calculateBodyPassiveAttackBonus(snap.bodyRealm(), snap.bodyStage());
        double atkSpd = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double armor = player.getAttributeValue(Attributes.ARMOR);
        double kbRes = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double moveSpd = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        float stamRegen = VitalService.calculateStaminaRegenPerSecond(pData);

        int rowY = y + 22;
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Vida Máxima Total", String.format(Locale.ROOT, "%.1f HP", snap.maxHealth()), 0xFFFCA5A5);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "  Cultivo Corporal", String.format(Locale.ROOT, "+%.0f HP", baseHp), 0xFF94A3B8);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Absorção Ativa", String.format(Locale.ROOT, "+%.1f HP", snap.absorption()), 0xFFFACC15);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Vigor Máx / Regen", String.format(Locale.ROOT, "%.0f (%.1f/s)", snap.maxStamina(), stamRegen), 0xFFBEF264);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Dano Ataque Total", String.format(Locale.ROOT, "%.1f (+%.1f C.)", totalAtk, bodyAtk), 0xFFF8FAFC);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Vel. de Ataque", String.format(Locale.ROOT, "%.2f", atkSpd), 0xFFE2E8F0);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Armadura", String.format(Locale.ROOT, "%.1f", armor), 0xFFCBD5E1);
        rowY = drawStatRow(g, lx + 7, w - 14, rowY, "Res. Repulsão", String.format(Locale.ROOT, "%.0f%%", kbRes * 100.0D), 0xFFCBD5E1);
        drawStatRow(g, lx + 7, w - 14, rowY, "Vel. Movimento", String.format(Locale.ROOT, "%.3f", moveSpd), 0xFFE2E8F0);

        drawCard(g, rx, y + 2, w, h, 0xFFC084FC);
        drawScaledString(g, "Espírito, Infusão & Divindade", rx + 7, y + 7, 0.78F, 0xFFC4B5FD, true);

        float spiritRegen = SpiritualIntegrityService.calculateSpiritualRegenPerSecond(pData);
        float qiRegen = CultivationBalance.calculateQiRegenPerSecond(snap.spiritRealm(), snap.spiritStage())
                + ContractManager.getTotalQiRegenBonus(pData);
        float drainSec = CultivationBalance.calculateInfusionDrainPerSecond(snap.spiritRealm());
        float drainHit = CultivationBalance.calculateInfusionDrainPerHit(pData);
        float infMult = CultivationBalance.calculateBodyInfusionMultiplier(pData);
        float infFlat = CultivationBalance.calculateSpiritInfusionFlatBonus(pData);
        float divCost = DivineResonanceBalance.calculateDivineSkillCost(pData);
        float divRegen = DivineResonanceBalance.calculateDivineRegenPerSecond(pData);
        float skillCdSec = CultivationBalance.calculateUniqueSkillCooldownTicks(snap.spiritRealm()) / 20.0F;

        rowY = y + 22;
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Int. Espiritual", String.format(Locale.ROOT, "%.0f (%.1f/s)", snap.maxSpiritualIntegrity(), spiritRegen), 0xFFE9D5FF);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Absorção Alma", "30% Arc. / 50% Esp.", 0xFFC084FC);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Qi Máx / Regen", String.format(Locale.ROOT, "%.0f (%.1f/s)", snap.maxQi(), qiRegen), 0xFF93C5FD);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Infusão [V] Bônus", String.format(Locale.ROOT, "+%.0f%% + %.1f", infMult * 100.0F, infFlat), 0xFF67E8F9);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Infusão Dreno", String.format(Locale.ROOT, "%.1f/s · %.1f/hit", drainSec, drainHit), 0xFF38BDF8);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Camada Divina", snap.tier().getNumeral() + " (" + String.format(Locale.ROOT, "%.2fx", snap.tier().getPowerMultiplier()) + ")", 0xFFFBBF24);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Res. Divina Máx", String.format(Locale.ROOT, "%.0f (%.1f/s)", snap.maxDivineResistance(), divRegen), 0xFFFDE68A);
        rowY = drawStatRow(g, rx + 7, w - 14, rowY, "Custo [G] / Recarga", String.format(Locale.ROOT, "%.0f · %.1fs", divCost, skillCdSec), 0xFFF59E0B);
        drawStatRow(g, rx + 7, w - 14, rowY, "Sobrecarga [G]", "60s Fraqueza (35%)", 0xFFFCA5A5);
    }

    // -------------------------------------------------------------------------
    // ABA 4 — HABILIDADES & 4 RESQUÍCIOS DIVINOS
    // -------------------------------------------------------------------------
    private void renderSkillsTab(GuiGraphics g, int x, int y, PlayerData pData, HudRenderSnapshot snap) {
        // Card Superior: Infusão de Mana / Qi [V]
        drawCard(g, x + 2, y + 2, 352, 38, snap.infusionActive() ? 0xFF22D3EE : 0xFF475569);
        String infStatus = snap.infusionActive() ? "ATIVA" : "DESLIGADA";
        int statusCol = snap.infusionActive() ? 0xFF22D3EE : 0xFF94A3B8;
        drawScaledString(g, "Infusão de Mana / Qi [V] · ", x + 8, y + 6, 0.80F, 0xFFF1F5F9, true);
        drawScaledString(g, infStatus, x + 132, y + 6, 0.80F, statusCol, true);

        float infMult = CultivationBalance.calculateBodyInfusionMultiplier(pData);
        float infFlat = CultivationBalance.calculateSpiritInfusionFlatBonus(pData);
        float drainSec = CultivationBalance.calculateInfusionDrainPerSecond(snap.spiritRealm());
        float drainHit = CultivationBalance.calculateInfusionDrainPerHit(pData);
        drawScaledString(g, String.format(Locale.ROOT, "Bônus: +%.0f%% Dano + %.1f Plano · Dreno: %.1f Qi/s + %.1f/golpe",
                infMult * 100.0F, infFlat, drainSec, drainHit), x + 8, y + 17, 0.68F, 0xFFCBD5E1, false);
        drawScaledString(g, "Perfura a Pressão de Reino (-90%) contra Chefes de Alto Reino.",
                x + 8, y + 27, 0.64F, 0xFF93C5FD, false);

        // Grade 2x2: Os 4 Resquícios Divinos [G]
        DivineRemnant equipped = UniqueSkillHandler.getEquippedRemnant(pData);
        ManifestationTier tier = snap.tier();
        float cost = DivineResonanceBalance.calculateDivineSkillCost(pData);
        DivineRemnant[] all = DivineRemnant.values();

        for (int i = 0; i < all.length; i++) {
            DivineRemnant rem = all[i];
            int col = i % 2;
            int row = i / 2;
            int cx = x + 2 + col * 178;
            int cy = y + 44 + row * 63;
            boolean isEq = rem == equipped;

            drawCard(g, cx, cy, 174, 60, isEq ? rem.getHudColor() : 0xFF334155);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(getRemnantTexture(rem), cx + 5, cy + 5, 0.0F, 0.0F, 18, 18, 18, 18);

            drawScaledString(g, rem.getDisplayName(), cx + 26, cy + 5, 0.78F, rem.getHudColor(), true);
            drawScaledString(g, rem.getDeityOrigin(), cx + 26, cy + 14, 0.62F, 0xFF94A3B8, false);
            drawScaledString(g, String.format(Locale.ROOT, "Camada %s (%.2fx) · Custo %.0f",
                    tier.getNumeral(), tier.getPowerMultiplier(), cost), cx + 5, cy + 25, 0.62F, 0xFFFDE68A, false);

            String summary = CultivatorMeridianMetrics.getRemnantProfile(rem).mechanicSummary();
            drawScaledWrappedText(g, summary, cx + 5, cy + 34, 104, 0.58F, 0xFFCBD5E1);
        }
    }

    // -------------------------------------------------------------------------
    // ABA 5 — CONTRATOS & SELOS SUBJUGADOS
    // -------------------------------------------------------------------------
    private void renderContractsTab(
            GuiGraphics g, int x, int y, PlayerCoreData coreData, PlayerData pData
    ) {
        int lx = x + 2;
        int lw = 212;
        int rx = x + 218;
        int rw = 136;
        int h = 166;

        // Painel Esquerdo: Contratos Ativos
        drawCard(g, lx, y + 2, lw, h, 0xFF785A28);
        Set<ResourceLocation> contracts = pData.getContracts().getActiveContractIds();
        drawScaledString(g, "Contratos Ativos (" + contracts.size() + ")", lx + 6, y + 7, 0.78F, 0xFFFDE68A, true);
        if (coreData.isVowBroken()) {
            drawRightScaledString(g, "VOTO QUEBRADO", lx + lw - 6, y + 7, 0.66F, 0xFFEF4444, true);
        }

        int listTop = y + 19;
        int listBot = y + h - 4;
        g.enableScissor(lx + 3, listTop, lx + lw - 3, listBot);
        try {
            if (contracts.isEmpty()) {
                drawScaledWrappedText(g, "Nenhum contrato ativo no momento. Contratos concedem bônus e impõem regras ou tabus ligados à sua facção.",
                        lx + 8, listTop + 8, lw - 16, 0.68F, 0xFF94A3B8);
            } else {
                int cy = listTop + 2 - this.contractScrollOffset;
                for (ResourceLocation id : contracts) {
                    ContractDefinition def = ContractManager.getDefinition(id);
                    String name = def != null ? def.displayName() : id.toString();
                    String desc = def != null ? def.description() : "Contrato registrado via datapack.";
                    boolean hasTaboo = def != null && (def.breakOnAttackVillager() || !def.tabooSummary().isBlank());
                    boolean hasV2Bonus = def != null && (def.qiRegenBonus() > 0.0F || def.spiritualIntegrityBonus() > 0.0F);
                    int cardH = (hasTaboo ? 52 : 41) + (hasV2Bonus ? 9 : 0);

                    g.fill(lx + 6, cy, lx + lw - 6, cy + cardH, 0x880B0F17);
                    g.renderOutline(lx + 6, cy, lw - 12, cardH, (def != null && def.breakOnAttackVillager()) ? 0xAAEF4444 : 0xAA64748B);
                    drawScaledString(g, name, lx + 10, cy + 4, 0.75F, 0xFFFEF3C7, true);
                    drawScaledWrappedText(g, desc, lx + 10, cy + 14, lw - 20, 0.60F, 0xFFCBD5E1);
                    int extraY = cy + 39;
                    if (hasV2Bonus) {
                        StringBuilder v2 = new StringBuilder();
                        if (def.qiRegenBonus() > 0.0F) {
                            v2.append(String.format(Locale.ROOT, "+%.1f Qi/s  ", def.qiRegenBonus()));
                        }
                        if (def.spiritualIntegrityBonus() > 0.0F) {
                            v2.append(String.format(Locale.ROOT, "+%.0f Int.Esp", def.spiritualIntegrityBonus()));
                        }
                        drawScaledString(g, v2.toString().trim(), lx + 10, extraY, 0.60F, 0xFF67E8F9, false);
                        extraY += 10;
                    }
                    if (hasTaboo) {
                        String tabooMsg = !def.tabooSummary().isBlank()
                                ? "⚠ " + def.tabooSummary()
                                : "⚠ TABU: Quebra ao atacar aldeões!";
                        int tabooCol = def.breakOnAttackVillager() ? 0xFFFCA5A5 : 0xFFFDE68A;
                        drawScaledString(g, tabooMsg, lx + 10, extraY, 0.58F, tabooCol, true);
                    }
                    cy += cardH + 4;
                }
            }
        } finally {
            g.disableScissor();
        }

        // Painel Direito: Selos Subjugados
        drawCard(g, rx, y + 2, rw, h, 0xFFA855F7);
        List<SubjugatedEntityRecord> bound = pData.getSubjugation().getBoundEntities();
        drawScaledString(g, "Selos Subjugados (" + bound.size() + ")", rx + 6, y + 7, 0.74F, 0xFFC084FC, true);

        boolean throneBreaker = coreData.getFaction() == FactionType.QUEBRADORES_DE_TRONOS;
        String ruleText = throneBreaker ? "Captura: <= 25% HP (Bônus Facção)" : "Captura: <= 15% HP (Padrão)";
        drawScaledString(g, ruleText, rx + 6, y + 17, 0.58F, throneBreaker ? 0xFFD8B4FE : 0xFF94A3B8, false);

        int sTop = y + 27;
        int sBot = y + h - 4;
        g.enableScissor(rx + 3, sTop, rx + rw - 3, sBot);
        try {
            if (bound.isEmpty()) {
                drawScaledWrappedText(g, "Nenhuma entidade vinculada. Enfraqueça alvos subjugáveis e use o Selo de Subjugação.",
                        rx + 7, sTop + 8, rw - 14, 0.64F, 0xFF94A3B8);
            } else {
                int sy = sTop + 2 - this.sealScrollOffset;
                for (int i = 0; i < bound.size(); i++) {
                    SubjugatedEntityRecord rec = bound.get(i);
                    String label = resolveEntityDisplayName(rec);
                    g.fill(rx + 6, sy, rx + rw - 6, sy + 18, 0x880B0F17);
                    g.renderOutline(rx + 6, sy, rw - 12, 18, 0x887E22CE);
                    drawScaledString(g, (i + 1) + ". " + label, rx + 9, sy + 3, 0.66F, 0xFFE9D5FF, true);
                    drawRightScaledString(g, "Nv." + rec.level(), rx + rw - 9, sy + 9, 0.58F, 0xFF94A3B8, false);
                    sy += 21;
                }
            }
        } finally {
            g.disableScissor();
        }
    }

    // -------------------------------------------------------------------------
    // ABA 6 — FACÇÃO, COMPATIBILIDADE & REPUTAÇÃO
    // -------------------------------------------------------------------------
    private void renderFactionTab(GuiGraphics g, int x, int y, PlayerData pData, FactionType faction) {
        CultivatorMeridianMetrics.FactionProfile profile = CultivatorMeridianMetrics.getFactionProfile(faction);

        // Banner superior
        drawCard(g, x + 2, y + 2, 352, 42, profile.color());
        drawTintedIcon(g, x + 9, y + 10, 50, profile.color());
        drawScaledString(g, faction.getDisplayName(), x + 22, y + 8, 0.90F, profile.color(), true);
        drawScaledWrappedText(g, profile.tagline(), x + 22, y + 20, 324, 0.68F, 0xFFE2E8F0);

        // Card Inferior Esquerdo: Identidade & Compatibilidade de Contratos
        drawCard(g, x + 2, y + 48, 174, 120, 0xFF475569);
        drawScaledString(g, "Regras & Contratos", x + 8, y + 53, 0.76F, 0xFFFDE68A, true);
        drawScaledWrappedText(g, "• " + profile.bonusSummary(), x + 8, y + 65, 158, 0.62F, 0xFFCBD5E1);
        drawScaledWrappedText(g, "• " + profile.restrictionSummary(), x + 8, y + 87, 158, 0.62F, 0xFFFCA5A5);

        drawScaledString(g, "Compatibilidade:", x + 8, y + 111, 0.68F, 0xFFF1F5F9, true);
        int cy = y + 121;
        for (ResourceLocation id : ContractManager.getRegisteredContractIds()) {
            ContractDefinition def = ContractManager.getDefinition(id);
            if (def == null) {
                continue;
            }
            boolean allowed = def.isAllowedForFaction(faction);
            String mark = allowed ? "✓ " : "✖ ";
            drawScaledString(g, mark + def.displayName(), x + 8, cy, 0.64F, allowed ? 0xFF34D399 : 0xFFEF4444, false);
            cy += 10;
            if (cy > y + 160) {
                break;
            }
        }

        // Card Inferior Direito: Reputações & Relações
        drawCard(g, x + 180, y + 48, 174, 120, 0xFF475569);
        drawScaledString(g, "Reputação & Relações", x + 186, y + 53, 0.76F, 0xFF93C5FD, true);

        Map<ResourceLocation, Integer> reps = pData.getFaction().getReputations();
        Map<ResourceLocation, String> rels = pData.getFaction().getRelationships();
        if (reps.isEmpty() && rels.isEmpty()) {
            drawScaledWrappedText(g, "Nenhuma variação de reputação registrada ainda. Sua postura diplomática com as facções aparecerá aqui conforme progredir no mundo.",
                    x + 186, y + 68, 160, 0.64F, 0xFF94A3B8);
        } else {
            int ry = y + 66;
            for (Map.Entry<ResourceLocation, Integer> entry : reps.entrySet()) {
                FactionType ft = FactionType.fromString(entry.getKey().getPath());
                String name = ft != FactionType.NONE ? ft.getDisplayName() : entry.getKey().getPath();
                drawScaledString(g, name + ": " + entry.getValue(), x + 186, ry, 0.68F, 0xFFE2E8F0, false);
                ry += 11;
                if (ry > y + 158) {
                    break;
                }
            }
            for (Map.Entry<ResourceLocation, String> entry : rels.entrySet()) {
                if (ry > y + 158) {
                    break;
                }
                drawScaledString(g, entry.getKey().getPath() + " → " + entry.getValue(), x + 186, ry, 0.65F, 0xFFCBD5E1, false);
                ry += 10;
            }
        }
    }

    // -------------------------------------------------------------------------
    // INPUT & SCROLL
    // -------------------------------------------------------------------------
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientKeyEvents.CULTIVATOR_SCREEN_KEY.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_7) {
            switchTab(CultivatorTab.byIndex(keyCode - GLFW.GLFW_KEY_1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            switchTab(this.activeTab.previous());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            switchTab(this.activeTab.next());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.activeTab == CultivatorTab.CONTRACTS && this.minecraft != null && this.minecraft.player != null) {
            int step = delta > 0 ? -16 : 16;
            PlayerData pData = ModCapabilities.getData(this.minecraft.player).orElse(null);
            if (pData != null) {
                int splitX = this.panelX + 6 + 216;
                if (mouseX < splitX) {
                    int maxScroll = Math.max(0, pData.getContracts().getActiveContractIds().size() * 54 - 140);
                    this.contractScrollOffset = Mth.clamp(this.contractScrollOffset + step, 0, maxScroll);
                } else {
                    int maxScroll = Math.max(0, pData.getSubjugation().getBoundEntities().size() * 21 - 130);
                    this.sealScrollOffset = Mth.clamp(this.sealScrollOffset + step, 0, maxScroll);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Permite sincronização S2C ao vivo após clicar em ações C2S
    }

    // -------------------------------------------------------------------------
    // HELPERS VISUAIS
    // -------------------------------------------------------------------------
    private void drawCard(GuiGraphics g, int x, int y, int w, int h, int borderColor) {
        g.fill(x, y, x + w, y + h, 0xCC0D121D);
        g.renderOutline(x, y, w, h, HudAlpha(borderColor, 0.72F));
    }

    private void drawOverviewMiniBar(
            GuiGraphics g, int x, int y, int w, String label, float ratio, String valText, int fillCol, int bgCol
    ) {
        drawScaledString(g, label, x, y, 0.62F, 0xFFCBD5E1, false);
        drawRightScaledString(g, valText, x + w, y, 0.62F, 0xFFF8FAFC, true);
        int barY = y + 8;
        g.fill(x, barY, x + w, barY + 6, bgCol);
        int fillW = Math.round(w * Mth.clamp(ratio, 0.0F, 1.0F));
        if (fillW > 0) {
            g.fill(x, barY, x + fillW, barY + 6, fillCol);
        }
        g.renderOutline(x, barY, w, 6, 0x77FFFFFF);
    }

    private void drawVerticalRealmBar(
            GuiGraphics g, int x, int y, int w, int h, float ratio,
            int topColor, int bottomColor, String verticalLabel, int labelColor, boolean leftLabel
    ) {
        g.fill(x, y, x + w, y + h, 0xBB090D16);
        int fillH = Math.max(2, Math.round(h * Mth.clamp(ratio, 0.0F, 1.0F)));
        int fillTop = y + h - fillH;
        g.fillGradient(x + 1, fillTop, x + w - 1, y + h - 1, topColor, bottomColor);
        g.renderOutline(x, y, w, h, HudAlpha(labelColor, 0.8F));

        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            float tx = leftLabel ? (x - 6.0F) : (x + w + 2.0F);
            float ty = y + h - 2.0F;
            pose.translate(tx, ty, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            pose.scale(0.62F, 0.62F, 1.0F);
            g.drawString(this.font, verticalLabel, 0, 0, labelColor, true);
        } finally {
            pose.popPose();
        }
    }

    private int drawStatRow(GuiGraphics g, int x, int w, int y, String label, String value, int valueColor) {
        drawScaledString(g, label, x, y, 0.68F, 0xFFCBD5E1, false);
        drawRightScaledString(g, value, x + w, y, 0.68F, valueColor, true);
        g.fill(x, y + 10, x + w, y + 11, 0x33334155);
        return y + 15;
    }

    private void drawScaledWrappedText(GuiGraphics g, String text, int x, int y, int maxWidth, float scale, int color) {
        int virtualWidth = Math.max(20, Math.round(maxWidth / scale));
        List<FormattedCharSequence> lines = this.font.split(Component.literal(text), virtualWidth);
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0.0F);
            pose.scale(scale, scale, 1.0F);
            int ly = 0;
            for (FormattedCharSequence line : lines) {
                g.drawString(this.font, line, 0, ly, color, false);
                ly += 10;
            }
        } finally {
            pose.popPose();
        }
    }

    private void drawScaledString(GuiGraphics g, String text, float x, float y, float scale, int color, boolean shadow) {
        PoseStack pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0.0F);
            pose.scale(scale, scale, 1.0F);
            g.drawString(this.font, text, 0, 0, color, shadow);
        } finally {
            pose.popPose();
        }
    }

    private void drawRightScaledString(GuiGraphics g, String text, float rightEdge, float y, float scale, int color, boolean shadow) {
        float w = this.font.width(text) * scale;
        drawScaledString(g, text, rightEdge - w, y, scale, color, shadow);
    }

    private void drawCenteredScaledString(GuiGraphics g, String text, float centerX, float y, float scale, int color, boolean shadow) {
        float w = this.font.width(text) * scale;
        drawScaledString(g, text, centerX - w * 0.5F, y, scale, color, shadow);
    }

    private static void drawTintedIcon(GuiGraphics g, int x, int y, int u, int argb) {
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float gr = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        g.setColor(r, gr, b, 1.0F);
        g.blit(ICON_ATLAS, x, y, (float) u, 0.0F, 9, 9, 64, 16);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation getRemnantTexture(DivineRemnant remnant) {
        if (remnant == null) {
            return EMBLEM_TEZCATLIPOCA;
        }
        return switch (remnant) {
            case EYE_OF_RA -> EMBLEM_RA;
            case MORRIGAN_REAP -> EMBLEM_MORRIGAN;
            case INDRAS_VAJRA -> EMBLEM_INDRA;
            case TEZCATLIPOCA_STEP -> EMBLEM_TEZCATLIPOCA;
        };
    }

    private static double findStageAttribute(RealmStageDefinition stage, String pathContains) {
        for (RealmAttributeBonus bonus : stage.attributes()) {
            if (bonus.attributeId().getPath().contains(pathContains)) {
                return bonus.amount();
            }
        }
        return 0.0D;
    }

    private static int countItem(Inventory inv, Item target) {
        if (inv == null || target == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.is(target)) {
                total += st.getCount();
            }
        }
        return total;
    }

    private static String resolveEntityDisplayName(SubjugatedEntityRecord rec) {
        if (rec.customName() != null && !rec.customName().isBlank()) {
            return rec.customName();
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rec.entityType());
        if (type != null) {
            return type.getDescription().getString();
        }
        return rec.entityType().getPath();
    }

    private static int HudAlpha(int argb, float mult) {
        int a = Mth.clamp(Math.round(((argb >>> 24) & 0xFF) * mult), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    // -------------------------------------------------------------------------
    // BOTÃO CUSTOMIZADO DE ABA (CONCEITO B — MOLDURA BRONZE/OURO)
    // -------------------------------------------------------------------------
    private final class TabButton extends AbstractButton {

        private final CultivatorTab tab;
        private final Runnable onSelect;

        private TabButton(int x, int y, int width, int height, CultivatorTab tab, Runnable onSelect) {
            super(x, y, width, height, Component.translatable(tab.getTranslationKey()));
            this.tab = tab;
            this.onSelect = onSelect;
        }

        @Override
        public void onPress() {
            this.onSelect.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = CultivatorScreen.this.activeTab == this.tab;
            boolean hovered = this.isHoveredOrFocused();

            int bg = selected ? 0xFF211A0E : (hovered ? 0xFF172033 : 0xFF0C101B);
            int border = selected ? 0xFFFBBF24 : (hovered ? 0xFFC8A24A : 0xFF64502D);
            int textCol = selected ? 0xFFFEF3C7 : (hovered ? 0xFFF1F5F9 : 0xFF94A3B8);

            g.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bg);
            g.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), border);
            if (selected) {
                g.fill(this.getX() + 2, this.getY() + this.getHeight() - 2,
                        this.getX() + this.getWidth() - 2, this.getY() + this.getHeight(), 0xFFFBBF24);
            }

            String label = this.getMessage().getString();
            drawCenteredScaledString(g, label,
                    this.getX() + this.getWidth() * 0.5F,
                    this.getY() + 6.0F,
                    0.70F,
                    textCol,
                    selected || hovered);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
