package com.mythiccontracts.client.hud;

import com.mythiccontracts.skill.remnant.DivineResonanceBalance.ManifestationTier;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.UUID;

/**
 * Estado cliente determinístico para animações da HUD (interpolação exponencial, barra fantasma de dano,
 * micro-tremor de impacto, brilho de cura, pulso de alerta, auto-fade de Vigor e banner de evolução).
 * Totalmente desacoplado de OpenGL/Minecraft para testes unitários determinísticos.
 */
public final class HudAnimationState {

    public static final float MAX_DT_SECONDS = 0.10F;
    public static final float GHOST_HOLD_SECONDS = 0.30F;
    public static final float GHOST_DECAY_PER_SECOND = 1.60F;
    public static final float HEAL_SHIMMER_DURATION = 0.55F;
    public static final float DAMAGE_SHAKE_DURATION = 0.15F;
    public static final float BANNER_DURATION = 2.60F;
    public static final float BORDER_FLASH_DURATION = 1.20F;

    private static final HudAnimationState INSTANCE = new HudAnimationState();

    public static final class BarTrack {
        private float targetRatio = 1.0F;
        private float displayedRatio = 1.0F;
        private float ghostRatio = 1.0F;
        private float ghostHoldSeconds = 0.0F;
        private float healShimmerSeconds = 0.0F;

        public void snapTo(float ratio) {
            float clamped = Mth.clamp(ratio, 0.0F, 1.0F);
            this.targetRatio = clamped;
            this.displayedRatio = clamped;
            this.ghostRatio = clamped;
            this.ghostHoldSeconds = 0.0F;
            this.healShimmerSeconds = 0.0F;
        }

        public void update(float newTargetRatio, float dtSeconds, boolean animationsEnabled, boolean trackGhostAndShimmer) {
            float target = Mth.clamp(newTargetRatio, 0.0F, 1.0F);
            float dt = Mth.clamp(dtSeconds, 0.0F, MAX_DT_SECONDS);

            if (!animationsEnabled) {
                snapTo(target);
                return;
            }

            float prevTarget = this.targetRatio;
            float prevDisplayed = this.displayedRatio;
            this.targetRatio = target;

            float alpha = 1.0F - (float) Math.exp(-12.0F * dt);
            this.displayedRatio += (target - this.displayedRatio) * alpha;
            if (Math.abs(target - this.displayedRatio) < 0.0005F) {
                this.displayedRatio = target;
            }

            if (trackGhostAndShimmer) {
                if (target < prevTarget - 0.002F) {
                    this.ghostHoldSeconds = GHOST_HOLD_SECONDS;
                    this.ghostRatio = Math.max(this.ghostRatio, prevDisplayed);
                } else if (target > prevTarget + 0.01F) {
                    this.healShimmerSeconds = HEAL_SHIMMER_DURATION;
                }

                if (this.ghostRatio > target) {
                    if (this.ghostHoldSeconds > 0.0F) {
                        this.ghostHoldSeconds -= dt;
                        if (this.ghostHoldSeconds < 0.0F) {
                            float overflow = -this.ghostHoldSeconds;
                            this.ghostHoldSeconds = 0.0F;
                            this.ghostRatio = Math.max(target, this.ghostRatio - GHOST_DECAY_PER_SECOND * overflow);
                        }
                    } else {
                        this.ghostRatio = Math.max(target, this.ghostRatio - GHOST_DECAY_PER_SECOND * dt);
                    }
                } else {
                    this.ghostRatio = target;
                    this.ghostHoldSeconds = 0.0F;
                }

                this.healShimmerSeconds = Math.max(0.0F, this.healShimmerSeconds - dt);
            } else {
                this.ghostRatio = this.displayedRatio;
                this.ghostHoldSeconds = 0.0F;
                this.healShimmerSeconds = 0.0F;
            }
        }

        public float getTargetRatio() {
            return this.targetRatio;
        }

        public float getDisplayedRatio() {
            return this.displayedRatio;
        }

        public float getGhostRatio() {
            return this.ghostRatio;
        }

        public float getGhostHoldSeconds() {
            return this.ghostHoldSeconds;
        }

        public float getHealShimmerSeconds() {
            return this.healShimmerSeconds;
        }
    }

    private final BarTrack bodyHealthTrack = new BarTrack();
    private final BarTrack spiritualIntegrityTrack = new BarTrack();
    private final BarTrack qiTrack = new BarTrack();
    private final BarTrack staminaTrack = new BarTrack();
    private final BarTrack divineResistanceTrack = new BarTrack();

    private boolean initialized = false;
    private UUID lastPlayerId = null;

    private int lastBodyRealm = 1;
    private int lastBodyStage = 1;
    private int lastSpiritRealm = 1;
    private int lastSpiritStage = 1;
    private int lastTierOrdinal = 0;

    private float elapsedSeconds = 0.0F;
    private float damageShakeSeconds = 0.0F;
    private float staminaFullSeconds = 0.0F;

    private String bannerText = "";
    private int bannerColor = 0xFFFBBF24;
    private float bannerRemainingSeconds = 0.0F;
    private float borderFlashSeconds = 0.0F;

    public HudAnimationState() {
    }

    public static HudAnimationState getInstance() {
        return INSTANCE;
    }

    public void updateSnapshot(
            UUID playerId,
            float bodyHealthRatio,
            float spiritualRatio,
            float qiRatio,
            float staminaRatio,
            float divineRatio,
            int bodyRealm,
            int bodyStage,
            String bodyRealmName,
            int spiritRealm,
            int spiritStage,
            String spiritRealmName,
            ManifestationTier tier,
            float dtSeconds,
            boolean animationsEnabled
    ) {
        float dt = Mth.clamp(dtSeconds, 0.0F, MAX_DT_SECONDS);
        this.elapsedSeconds += dt;
        ManifestationTier safeTier = tier != null ? tier : ManifestationTier.TIER_1;

        float clampedBody = Mth.clamp(bodyHealthRatio, 0.0F, 1.0F);
        float clampedSpirit = Mth.clamp(spiritualRatio, 0.0F, 1.0F);
        float clampedQi = Mth.clamp(qiRatio, 0.0F, 1.0F);
        float clampedStamina = Mth.clamp(staminaRatio, 0.0F, 1.0F);
        float clampedDivine = Mth.clamp(divineRatio, 0.0F, 1.0F);

        if (!this.initialized || !Objects.equals(this.lastPlayerId, playerId)) {
            this.initialized = true;
            this.lastPlayerId = playerId;
            this.bodyHealthTrack.snapTo(clampedBody);
            this.spiritualIntegrityTrack.snapTo(clampedSpirit);
            this.qiTrack.snapTo(clampedQi);
            this.staminaTrack.snapTo(clampedStamina);
            this.divineResistanceTrack.snapTo(clampedDivine);

            this.lastBodyRealm = bodyRealm;
            this.lastBodyStage = bodyStage;
            this.lastSpiritRealm = spiritRealm;
            this.lastSpiritStage = spiritStage;
            this.lastTierOrdinal = safeTier.ordinal();

            this.damageShakeSeconds = 0.0F;
            this.staminaFullSeconds = 0.0F;
            this.bannerRemainingSeconds = 0.0F;
            this.borderFlashSeconds = 0.0F;
            return;
        }

        float prevBodyTarget = this.bodyHealthTrack.getTargetRatio();
        float prevSpiritTarget = this.spiritualIntegrityTrack.getTargetRatio();

        if (animationsEnabled && ((prevBodyTarget - clampedBody) >= 0.15F || (prevSpiritTarget - clampedSpirit) >= 0.15F)) {
            this.damageShakeSeconds = DAMAGE_SHAKE_DURATION;
        } else {
            this.damageShakeSeconds = Math.max(0.0F, this.damageShakeSeconds - dt);
        }

        if (!animationsEnabled) {
            this.damageShakeSeconds = 0.0F;
        }

        this.bodyHealthTrack.update(clampedBody, dt, animationsEnabled, true);
        this.spiritualIntegrityTrack.update(clampedSpirit, dt, animationsEnabled, true);
        this.qiTrack.update(clampedQi, dt, animationsEnabled, false);
        this.staminaTrack.update(clampedStamina, dt, animationsEnabled, false);
        this.divineResistanceTrack.update(clampedDivine, dt, animationsEnabled, true);

        if (clampedStamina >= 0.999F) {
            this.staminaFullSeconds += dt;
        } else {
            this.staminaFullSeconds = 0.0F;
        }

        boolean bodyUpgraded = bodyRealm > this.lastBodyRealm
                || (bodyRealm == this.lastBodyRealm && bodyStage > this.lastBodyStage);
        boolean spiritUpgraded = spiritRealm > this.lastSpiritRealm
                || (spiritRealm == this.lastSpiritRealm && spiritStage > this.lastSpiritStage);
        boolean tierUpgraded = safeTier.ordinal() > this.lastTierOrdinal;

        if (bodyUpgraded) {
            triggerBanner("▲ CORPO R" + bodyRealm + "-" + bodyStage + " · " + safeName(bodyRealmName), 0xFFF87171);
        } else if (spiritUpgraded) {
            triggerBanner("▲ ESPÍRITO R" + spiritRealm + "-" + spiritStage + " · " + safeName(spiritRealmName), 0xFFC084FC);
        } else if (tierUpgraded) {
            triggerBanner("✦ CAMADA DIVINA [" + safeTier.getNumeral() + "] · " + safeTier.getTitle(), 0xFFFBBF24);
        } else {
            this.bannerRemainingSeconds = Math.max(0.0F, this.bannerRemainingSeconds - dt);
            this.borderFlashSeconds = Math.max(0.0F, this.borderFlashSeconds - dt);
        }

        this.lastBodyRealm = bodyRealm;
        this.lastBodyStage = bodyStage;
        this.lastSpiritRealm = spiritRealm;
        this.lastSpiritStage = spiritStage;
        this.lastTierOrdinal = safeTier.ordinal();
    }

    private void triggerBanner(String text, int color) {
        this.bannerText = text;
        this.bannerColor = color;
        this.bannerRemainingSeconds = BANNER_DURATION;
        this.borderFlashSeconds = BORDER_FLASH_DURATION;
    }

    private static String safeName(String name) {
        return name != null && !name.isBlank() ? name : "Despertar";
    }

    public void reset() {
        this.initialized = false;
        this.lastPlayerId = null;
        this.lastBodyRealm = 1;
        this.lastBodyStage = 1;
        this.lastSpiritRealm = 1;
        this.lastSpiritStage = 1;
        this.lastTierOrdinal = 0;
        this.elapsedSeconds = 0.0F;
        this.damageShakeSeconds = 0.0F;
        this.staminaFullSeconds = 0.0F;
        this.bannerText = "";
        this.bannerRemainingSeconds = 0.0F;
        this.borderFlashSeconds = 0.0F;
        this.bodyHealthTrack.snapTo(1.0F);
        this.spiritualIntegrityTrack.snapTo(1.0F);
        this.qiTrack.snapTo(1.0F);
        this.staminaTrack.snapTo(1.0F);
        this.divineResistanceTrack.snapTo(1.0F);
    }

    public BarTrack getBodyHealthTrack() {
        return this.bodyHealthTrack;
    }

    public BarTrack getSpiritualIntegrityTrack() {
        return this.spiritualIntegrityTrack;
    }

    public BarTrack getQiTrack() {
        return this.qiTrack;
    }

    public BarTrack getStaminaTrack() {
        return this.staminaTrack;
    }

    public BarTrack getDivineResistanceTrack() {
        return this.divineResistanceTrack;
    }

    public float getElapsedSeconds() {
        return this.elapsedSeconds;
    }

    public float getPulseWave() {
        return 0.5F + 0.5F * Mth.sin(this.elapsedSeconds * 6.0F);
    }

    public float getDamageShakeSeconds() {
        return this.damageShakeSeconds;
    }

    public float getDamageShakeOffsetX(boolean animationsEnabled) {
        if (!animationsEnabled || this.damageShakeSeconds <= 0.0F) {
            return 0.0F;
        }
        float intensity = this.damageShakeSeconds / DAMAGE_SHAKE_DURATION;
        return Mth.sin(this.elapsedSeconds * 55.0F) * 1.5F * intensity;
    }

    public float getStaminaFullSeconds() {
        return this.staminaFullSeconds;
    }

    public String getBannerText() {
        return this.bannerText;
    }

    public int getBannerColor() {
        return this.bannerColor;
    }

    public float getBannerRemainingSeconds() {
        return this.bannerRemainingSeconds;
    }

    public float getBorderFlashSeconds() {
        return this.borderFlashSeconds;
    }
}
