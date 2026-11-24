package com.mythiccontracts.attribute;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço central responsável por aplicar, atualizar e limpar modificadores de atributos
 * no jogador sem duplicações, mantendo identificadores UUID estáveis e preservando a proporção de HP.
 */
public final class AttributeService {

    public static final String MODIFIER_PREFIX = "mythic_contracts.";

    private static final Set<Attribute> TRACKED_ATTRIBUTES = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> SUPPRESS_HEALTH_CLAMP = ThreadLocal.withInitial(() -> false);

    static {
        TRACKED_ATTRIBUTES.add(Attributes.MAX_HEALTH);
        TRACKED_ATTRIBUTES.add(Attributes.ATTACK_DAMAGE);
        TRACKED_ATTRIBUTES.add(Attributes.ATTACK_SPEED);
        TRACKED_ATTRIBUTES.add(Attributes.ARMOR);
        TRACKED_ATTRIBUTES.add(Attributes.ARMOR_TOUGHNESS);
        TRACKED_ATTRIBUTES.add(Attributes.KNOCKBACK_RESISTANCE);
        TRACKED_ATTRIBUTES.add(Attributes.MOVEMENT_SPEED);
    }

    private AttributeService() {
    }

    public static void trackAttribute(Attribute attribute) {
        if (attribute != null) {
            TRACKED_ATTRIBUTES.add(attribute);
        }
    }

    public static Collection<Attribute> getTrackedAttributes() {
        return Collections.unmodifiableSet(TRACKED_ATTRIBUTES);
    }

    /**
     * Executa uma mutação composta de atributos (limpeza + reaplicação) sem truncar
     * a vida atual para 20.0 no meio do processo, preservando a proporção {@code health / maxHealth}
     * (ou curando 100% quando {@code healToFull == true}).
     */
    public static void runPreservingHealthRatio(ServerPlayer player, boolean healToFull, Runnable attributeMutation) {
        if (player == null || attributeMutation == null) {
            return;
        }
        if (SUPPRESS_HEALTH_CLAMP.get()) {
            attributeMutation.run();
            return;
        }

        float oldMax = Math.max(1.0F, player.getMaxHealth());
        float oldHealth = Mth.clamp(player.getHealth(), 0.0F, oldMax);
        float ratio = oldHealth / oldMax;

        SUPPRESS_HEALTH_CLAMP.set(true);
        try {
            attributeMutation.run();
        } finally {
            SUPPRESS_HEALTH_CLAMP.set(false);
        }

        float newMax = Math.max(1.0F, player.getMaxHealth());
        if (healToFull) {
            player.setHealth(newMax);
        } else if (player.isAlive() && oldHealth > 0.0F) {
            player.setHealth(Mth.clamp(ratio * newMax, 1.0F, newMax));
        }
    }

    public static void applySingleModifier(
            ServerPlayer player,
            Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        if (player == null || attribute == null || uuid == null) {
            return;
        }
        trackAttribute(attribute);
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(uuid);
        if (amount != 0.0D) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, operation));
        }
        clampHealthIfNeeded(player, attribute);
    }

    public static void removeModifier(ServerPlayer player, Attribute attribute, UUID uuid) {
        if (player == null || attribute == null || uuid == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
            clampHealthIfNeeded(player, attribute);
        }
    }

    public static void applyModifiers(ServerPlayer player, String sourceKey, List<AttributeModifierSpec> specs) {
        if (player == null || specs == null || specs.isEmpty()) {
            return;
        }
        for (AttributeModifierSpec spec : specs) {
            if (spec == null || spec.attribute() == null) {
                continue;
            }
            applySingleModifier(
                    player,
                    spec.attribute(),
                    spec.resolveUuid(sourceKey),
                    spec.modifierName(),
                    spec.amount(),
                    spec.operation()
            );
        }
    }

    /**
     * Remove todos os modificadores cujo nome comece com o prefixo informado,
     * abrangendo atributos rastreados e atributos sincronizáveis do jogador.
     */
    public static void clearByNamePrefix(ServerPlayer player, String namePrefix) {
        if (player == null || namePrefix == null || namePrefix.isEmpty()) {
            return;
        }
        Set<Attribute> candidates = new LinkedHashSet<>(TRACKED_ATTRIBUTES);
        for (AttributeInstance syncable : player.getAttributes().getSyncableAttributes()) {
            candidates.add(syncable.getAttribute());
        }

        for (Attribute attribute : candidates) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            List<UUID> toRemove = null;
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName().startsWith(namePrefix)) {
                    if (toRemove == null) {
                        toRemove = new ArrayList<>();
                    }
                    toRemove.add(modifier.getId());
                }
            }
            if (toRemove != null) {
                for (UUID id : toRemove) {
                    instance.removeModifier(id);
                }
                clampHealthIfNeeded(player, attribute);
            }
        }
    }

    private static void clampHealthIfNeeded(ServerPlayer player, Attribute attribute) {
        if (SUPPRESS_HEALTH_CLAMP.get()) {
            return;
        }
        if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
