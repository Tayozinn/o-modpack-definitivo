package com.mythiccontracts.cultivation.realm;

import com.mythiccontracts.MythicContractsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registro central dinâmico de Reinos de Cultivo (Corpo e Espírito).
 * Mantém os 14 reinos clássicos como fallback built-in e aceita expansão/substituição via datapacks.
 */
public final class RealmRegistry {

    public static final UUID BODY_CULTIVATION_ATTACK_UUID = UUID.fromString("a7f31c90-4b2e-4d8a-9c15-778899aabbcc");
    public static final UUID BODY_CULTIVATION_HEALTH_UUID = UUID.fromString("b8e42d01-5c3f-4e9b-8d26-889900bbccdd");

    private static final ResourceLocation ATTR_ATTACK_DAMAGE = new ResourceLocation("minecraft", "generic.attack_damage");
    private static final ResourceLocation ATTR_MAX_HEALTH = new ResourceLocation("minecraft", "generic.max_health");
    private static final String[] DEFAULT_STAGE_IDS = {"base", "fortification", "awakening"};
    private static final String[] DEFAULT_STAGE_NAMES = {"Base", "Fortificação", "Despertar"};

    private static final String[] DEFAULT_BODY_NAMES = {
            "Corpo Mortal",
            "Refinamento Ósseo",
            "Tempero de Ferro",
            "Carne de Jade",
            "Vigor Soberano",
            "Corpo Imortal",
            "Ápice Primordial"
    };

    private static final String[] DEFAULT_SPIRIT_NAMES = {
            "Centelha Espiritual",
            "Condensação de Qi",
            "Núcleo Dourado",
            "Alma Nascente",
            "Domínio Etéreo",
            "Ascensão Celestial",
            "Divindade Suprema"
    };

    private static final Map<ResourceLocation, RealmDefinition> BUILTIN_FALLBACKS = createBuiltinFallbacks();
    private static volatile Map<ResourceLocation, RealmDefinition> realmsById = BUILTIN_FALLBACKS;
    private static volatile Map<CultivationPath, List<RealmDefinition>> orderedByPath = indexByPath(BUILTIN_FALLBACKS.values());

    private RealmRegistry() {
    }

    private static Map<ResourceLocation, RealmDefinition> createBuiltinFallbacks() {
        Map<ResourceLocation, RealmDefinition> map = new LinkedHashMap<>();

        for (int r = 1; r <= 7; r++) {
            ResourceLocation bodyId = new ResourceLocation(MythicContractsMod.MOD_ID, "body_realm_" + r);
            ResourceLocation prevBody = r > 1 ? new ResourceLocation(MythicContractsMod.MOD_ID, "body_realm_" + (r - 1)) : null;
            List<RealmStageDefinition> bodyStages = new ArrayList<>(3);
            for (int s = 1; s <= 3; s++) {
                double attackBonus = 0.5D * r * (r + s - 1);
                double healthBonus = Math.max(0.0D, 2.0D * r * (r + s - 1) - 2.0D);
                float infusionMult = (0.20F * (r - 1)) + (0.05F * s);
                List<RealmAttributeBonus> stageAttrs = new ArrayList<>(2);
                stageAttrs.add(new RealmAttributeBonus(
                        ATTR_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADDITION,
                        attackBonus,
                        BODY_CULTIVATION_ATTACK_UUID
                ));
                if (healthBonus > 0.0D) {
                    stageAttrs.add(new RealmAttributeBonus(
                            ATTR_MAX_HEALTH,
                            AttributeModifier.Operation.ADDITION,
                            healthBonus,
                            BODY_CULTIVATION_HEALTH_UUID
                    ));
                }
                bodyStages.add(new RealmStageDefinition(
                        DEFAULT_STAGE_IDS[s - 1],
                        DEFAULT_STAGE_NAMES[s - 1],
                        0.0F,
                        infusionMult,
                        0.0F,
                        stageAttrs
                ));
            }
            map.put(bodyId, new RealmDefinition(
                    bodyId,
                    CultivationPath.BODY,
                    r,
                    DEFAULT_BODY_NAMES[r - 1],
                    bodyStages,
                    new RealmRequirements(prevBody, null, 0, 0.0F),
                    10.0F * (r - 1),
                    0.0F,
                    0.0F,
                    0.0F
            ));

            ResourceLocation spiritId = new ResourceLocation(MythicContractsMod.MOD_ID, "spirit_realm_" + r);
            ResourceLocation prevSpirit = r > 1 ? new ResourceLocation(MythicContractsMod.MOD_ID, "spirit_realm_" + (r - 1)) : null;
            List<RealmStageDefinition> spiritStages = new ArrayList<>(3);
            for (int s = 1; s <= 3; s++) {
                float flatBonus = 1.0F * r * (r + s - 1);
                float stageQiBonus = 10.0F * (s - 1);
                spiritStages.add(new RealmStageDefinition(
                        DEFAULT_STAGE_IDS[s - 1],
                        DEFAULT_STAGE_NAMES[s - 1],
                        stageQiBonus,
                        0.0F,
                        flatBonus,
                        List.of()
                ));
            }
            map.put(spiritId, new RealmDefinition(
                    spiritId,
                    CultivationPath.SPIRIT,
                    r,
                    DEFAULT_SPIRIT_NAMES[r - 1],
                    spiritStages,
                    new RealmRequirements(prevSpirit, null, 0, 0.0F),
                    100.0F + 35.0F * (r - 1),
                    4.0F + 1.5F * (r - 1),
                    2.5F + 0.5F * (r - 1),
                    3.0F + 1.0F * (r - 1)
            ));
        }

        return Collections.unmodifiableMap(map);
    }

    private static Map<CultivationPath, List<RealmDefinition>> indexByPath(Collection<RealmDefinition> definitions) {
        Map<CultivationPath, List<RealmDefinition>> grouped = new EnumMap<>(CultivationPath.class);
        for (CultivationPath path : CultivationPath.values()) {
            grouped.put(path, new ArrayList<>());
        }
        for (RealmDefinition def : definitions) {
            grouped.get(def.path()).add(def);
        }
        for (CultivationPath path : CultivationPath.values()) {
            grouped.get(path).sort(Comparator.comparingInt(RealmDefinition::order).thenComparing(r -> r.id().toString()));
            grouped.put(path, Collections.unmodifiableList(grouped.get(path)));
        }
        return Collections.unmodifiableMap(grouped);
    }

    public static void setLoadedRealms(Map<ResourceLocation, RealmDefinition> parsed) {
        Map<ResourceLocation, RealmDefinition> merged = new LinkedHashMap<>(BUILTIN_FALLBACKS);
        if (parsed != null) {
            merged.putAll(parsed);
        }
        Map<ResourceLocation, RealmDefinition> immutable = Collections.unmodifiableMap(merged);
        realmsById = immutable;
        orderedByPath = indexByPath(immutable.values());
    }

    public static void resetToBuiltins() {
        realmsById = BUILTIN_FALLBACKS;
        orderedByPath = indexByPath(BUILTIN_FALLBACKS.values());
    }

    public static Collection<RealmDefinition> getAllRealms() {
        return realmsById.values();
    }

    public static List<RealmDefinition> getOrderedRealms(CultivationPath path) {
        if (path == null) {
            return List.of();
        }
        return orderedByPath.getOrDefault(path, List.of());
    }

    public static int getMaxOrder(CultivationPath path) {
        List<RealmDefinition> list = getOrderedRealms(path);
        if (list.isEmpty()) {
            return 7;
        }
        return list.get(list.size() - 1).order();
    }

    @Nullable
    public static RealmDefinition getRealm(@Nullable ResourceLocation id) {
        return id != null ? realmsById.get(id) : null;
    }

    @Nullable
    public static RealmDefinition getRealmByOrder(CultivationPath path, int order) {
        for (RealmDefinition def : getOrderedRealms(path)) {
            if (def.order() == order) {
                return def;
            }
        }
        return null;
    }

    public static RealmDefinition resolveRealm(CultivationPath path, @Nullable ResourceLocation id, int fallbackOrder) {
        RealmDefinition byId = getRealm(id);
        if (byId != null && byId.path() == path) {
            return byId;
        }
        RealmDefinition byOrder = getRealmByOrder(path, fallbackOrder);
        if (byOrder != null) {
            return byOrder;
        }
        List<RealmDefinition> list = getOrderedRealms(path);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return BUILTIN_FALLBACKS.get(new ResourceLocation(
                MythicContractsMod.MOD_ID,
                (path == CultivationPath.BODY ? "body_realm_1" : "spirit_realm_1")
        ));
    }

    @Nullable
    public static RealmDefinition getNextRealm(RealmDefinition current) {
        if (current == null) {
            return null;
        }
        List<RealmDefinition> list = getOrderedRealms(current.path());
        for (RealmDefinition candidate : list) {
            if (current.id().equals(candidate.requirements().previousRealm())) {
                return candidate;
            }
        }
        for (RealmDefinition candidate : list) {
            if (candidate.order() > current.order()) {
                return candidate;
            }
        }
        return null;
    }
}
