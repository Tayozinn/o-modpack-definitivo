package com.mythiccontracts.client.fx;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.client.config.ClientHudConfig;
import com.mythiccontracts.network.SpawnVisualEffectS2CPacket.EffectType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Soft-Bridge desacoplada por reflexão para {@code playerAnimator}.
 * Funciona quando {@code playeranimator} está presente em runtime, mantendo o mod compilável
 * com {@code --offline} sem dependência Maven extra e sem impacto caso o mod opcional falte.
 */
public final class PlayerAnimBridge {

    private static final ResourceLocation LAYER_ID = new ResourceLocation(MythicContractsMod.MOD_ID, "skill_layer");

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    @Nullable
    private static Class<?> modifierLayerClass;
    @Nullable
    private static Method getAssociatedDataMethod;
    @Nullable
    private static Method dataGetMethod;
    @Nullable
    private static Method registryGetAnimMethod;
    @Nullable
    private static Constructor<?> keyframePlayerCtor;
    @Nullable
    private static Method setAnimationMethod;
    @Nullable
    private static Method replaceWithFadeMethod;
    @Nullable
    private static Method standardFadeInMethod;
    @Nullable
    private static Object easeInOutSine;

    private PlayerAnimBridge() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!ModList.get().isLoaded("playeranimator")) {
            return;
        }
        try {
            Class<?> factoryClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory");
            Field holderField = factoryClass.getField("ANIMATION_DATA_FACTORY");
            Object holder = holderField.get(null);

            modifierLayerClass = Class.forName("dev.kosmx.playerAnim.api.layered.ModifierLayer");
            Constructor<?> layerCtor = modifierLayerClass.getConstructor();
            Method registerFactoryMethod = holder.getClass().getMethod("registerFactory", ResourceLocation.class, int.class, factoryClass);

            Object factoryProxy = Proxy.newProxyInstance(
                    PlayerAnimBridge.class.getClassLoader(),
                    new Class<?>[]{factoryClass},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            return layerCtor.newInstance();
                        }
                        return null;
                    }
            );
            registerFactoryMethod.invoke(holder, LAYER_ID, 42, factoryProxy);

            Class<?> accessClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess");
            getAssociatedDataMethod = accessClass.getMethod("getPlayerAssociatedData", AbstractClientPlayer.class);
            Class<?> assocDataClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess$PlayerAssociatedAnimationData");
            dataGetMethod = assocDataClass.getMethod("get", ResourceLocation.class);

            Class<?> registryClass = Class.forName("dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry");
            registryGetAnimMethod = registryClass.getMethod("getAnimation", ResourceLocation.class);

            Class<?> keyframeAnimClass = Class.forName("dev.kosmx.playerAnim.core.data.KeyframeAnimation");
            Class<?> keyframePlayerClass = Class.forName("dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer");
            keyframePlayerCtor = keyframePlayerClass.getConstructor(keyframeAnimClass);

            Class<?> iAnimClass = Class.forName("dev.kosmx.playerAnim.api.layered.IAnimation");
            setAnimationMethod = modifierLayerClass.getMethod("setAnimation", iAnimClass);

            try {
                Class<?> fadeClass = Class.forName("dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier");
                Class<?> easeClass = Class.forName("dev.kosmx.playerAnim.core.util.Ease");
                replaceWithFadeMethod = modifierLayerClass.getMethod("replaceAnimationWithFade", fadeClass, iAnimClass);
                standardFadeInMethod = fadeClass.getMethod("standardFadeIn", int.class, easeClass);
                easeInOutSine = Enum.valueOf(easeClass.asSubclass(Enum.class), "INOUTSINE");
            } catch (Throwable ignored) {
                replaceWithFadeMethod = null;
            }

            available = true;
            MythicContractsMod.LOGGER.info("[{}] PlayerAnimator Soft-Bridge ativada com sucesso.", MythicContractsMod.MOD_ID);
        } catch (Throwable ex) {
            available = false;
            MythicContractsMod.LOGGER.warn("[{}] PlayerAnimator detectado, mas bridge reflexiva indisponível: {}",
                    MythicContractsMod.MOD_ID, ex.getMessage());
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void playForEffect(@Nullable AbstractClientPlayer player, @Nullable EffectType type) {
        if (!available || player == null || type == null || !ClientHudConfig.enableSmoothAnimations) {
            return;
        }
        String animPath = mapEffectToAnimationPath(type);
        if (animPath == null) {
            return;
        }
        playAnimation(player, new ResourceLocation(MythicContractsMod.MOD_ID, animPath));
    }

    public static void playAnimation(@Nullable AbstractClientPlayer player, @Nullable ResourceLocation animId) {
        if (!available || player == null || animId == null) {
            return;
        }
        try {
            Object assocData = getAssociatedDataMethod.invoke(null, player);
            if (assocData == null) {
                return;
            }
            Object layer = dataGetMethod.invoke(assocData, LAYER_ID);
            if (layer == null || !modifierLayerClass.isInstance(layer)) {
                return;
            }
            Object keyframeAnim = registryGetAnimMethod.invoke(null, animId);
            if (keyframeAnim == null) {
                return;
            }
            Object animPlayer = keyframePlayerCtor.newInstance(keyframeAnim);
            if (replaceWithFadeMethod != null && standardFadeInMethod != null && easeInOutSine != null) {
                Object fade = standardFadeInMethod.invoke(null, 4, easeInOutSine);
                replaceWithFadeMethod.invoke(layer, fade, animPlayer);
            } else {
                setAnimationMethod.invoke(layer, animPlayer);
            }
        } catch (Throwable ex) {
            available = false;
            MythicContractsMod.LOGGER.debug("[{}] Desativando PlayerAnimBridge após falha em runtime: {}",
                    MythicContractsMod.MOD_ID, ex.getMessage());
        }
    }

    @Nullable
    private static String mapEffectToAnimationPath(EffectType type) {
        return switch (type) {
            case EYE_OF_RA, MORRIGAN_REAP, INDRAS_VAJRA, SPIRIT_DOMAIN_SUPPRESSION,
                    PRIMORDIAL_HEAVEN_SUNDER, UNDYING_MERIDIAN_REVERSAL, REALM_BREAKTHROUGH_BURST -> "divine_cast";
            case SUBJUGATION_CLASH_START, SUBJUGATION_CLASH_TICK, SUBJUGATION_SUMMON -> "subjugation_channel";
            case TEZCATLIPOCA_STEP, GALE_MERIDIAN_STEP, SEISMIC_PALM,
                    NASCENT_SOUL_LANCE, ETHER_FLASH_STRIKE, BLOOD_FURNACE_SURGE -> "martial_strike";
            case JADE_BONE_GUARD, GOLDEN_CORE_AEGIS -> "body_guard";
            case INNER_BREATH_MEDITATION -> "lotus_meditation";
            case DIVINE_OVERLOAD_BURST, DIVINE_OVERLOAD_REJECT, DIVINE_OVERLOAD_RECOVER,
                    SPIRIT_SHATTER_BURST, SUBJUGATION_SUCCESS, SUBJUGATION_FAIL -> null;
        };
    }
}
