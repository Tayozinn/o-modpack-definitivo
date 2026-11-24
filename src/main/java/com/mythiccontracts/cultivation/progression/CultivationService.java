package com.mythiccontracts.cultivation.progression;

import com.mythiccontracts.attribute.AttributeModifierSpec;
import com.mythiccontracts.attribute.AttributeService;
import com.mythiccontracts.core.player.CultivationData;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmAttributeBonus;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.cultivation.realm.RealmStageDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor central de progressão do Cultivo Corporal e Espiritual.
 * Valida estágios, quebras de reino, pré-requisitos e aplica modificadores de atributos via {@link AttributeService}.
 */
public final class CultivationService {

    public static final String CULTIVATION_MODIFIER_PREFIX = AttributeService.MODIFIER_PREFIX + "cultivation.";

    public enum AdvancementOutcome {
        SUCCESS,
        NEEDS_BREAKTHROUGH,
        NOT_AT_FINAL_STAGE,
        MAX_REALM_REACHED,
        INSUFFICIENT_QI,
        MISSING_OPPOSITE_REALM
    }

    public record AdvancementResult(
            AdvancementOutcome outcome,
            CultivationPath path,
            RealmDefinition realm,
            RealmStageDefinition stage,
            String message
    ) {
        public boolean isSuccess() {
            return this.outcome == AdvancementOutcome.SUCCESS;
        }
    }

    private CultivationService() {
    }

    public static RealmDefinition getCurrentRealm(PlayerData data, CultivationPath path) {
        if (data == null) {
            return RealmRegistry.resolveRealm(path, null, 1);
        }
        CultivationData cult = data.getCultivation();
        return path == CultivationPath.BODY
                ? RealmRegistry.resolveRealm(CultivationPath.BODY, cult.getBodyRealmId(), cult.getBodyRealm())
                : RealmRegistry.resolveRealm(CultivationPath.SPIRIT, cult.getSpiritRealmId(), cult.getSpiritRealm());
    }

    public static int getCurrentStageNumber(PlayerData data, CultivationPath path) {
        if (data == null) {
            return 1;
        }
        RealmDefinition realm = getCurrentRealm(data, path);
        int raw = path == CultivationPath.BODY
                ? data.getCultivation().getBodyStage()
                : data.getCultivation().getSpiritStage();
        return Mth.clamp(raw, 1, realm.getStageCount());
    }

    public static RealmStageDefinition getCurrentStage(PlayerData data, CultivationPath path) {
        RealmDefinition realm = getCurrentRealm(data, path);
        return realm.getStageByNumber(getCurrentStageNumber(data, path));
    }

    public static String getStageDisplayName(PlayerData data, CultivationPath path) {
        return getCurrentStage(data, path).displayName();
    }

    public static String getRealmDisplayName(PlayerData data, CultivationPath path) {
        return getCurrentRealm(data, path).displayName();
    }

    public static boolean isReadyForBreakthrough(PlayerData data, CultivationPath path) {
        RealmDefinition current = getCurrentRealm(data, path);
        int currentStage = getCurrentStageNumber(data, path);
        return current.isFinalStage(currentStage) && RealmRegistry.getNextRealm(current) != null;
    }

    public static AdvancementResult advanceStage(PlayerData data, CultivationPath path) {
        RealmDefinition realm = getCurrentRealm(data, path);
        int stageNum = getCurrentStageNumber(data, path);

        if (realm.isFinalStage(stageNum)) {
            RealmDefinition next = RealmRegistry.getNextRealm(realm);
            if (next == null) {
                return new AdvancementResult(
                        AdvancementOutcome.MAX_REALM_REACHED,
                        path,
                        realm,
                        realm.getStageByNumber(stageNum),
                        "Seu cultivo do " + path.getDisplayName() + " já atingiu o ápice absoluto ("
                                + realm.displayName() + " — " + realm.getStageByNumber(stageNum).displayName() + ")!"
                );
            }
            return new AdvancementResult(
                    AdvancementOutcome.NEEDS_BREAKTHROUGH,
                    path,
                    realm,
                    realm.getStageByNumber(stageNum),
                    path.getDisplayName() + " está no estágio final (" + realm.getStageByNumber(stageNum).displayName()
                            + "). Realize a Quebra de Reino para alcançar " + next.displayName() + "!"
            );
        }

        int newStage = stageNum + 1;
        setRealmAndStage(data, path, realm, newStage);
        RealmStageDefinition stageDef = realm.getStageByNumber(newStage);
        return new AdvancementResult(
                AdvancementOutcome.SUCCESS,
                path,
                realm,
                stageDef,
                "Cultivo do " + path.getDisplayName() + " elevado para " + realm.displayName()
                        + " — " + stageDef.displayName() + "!"
        );
    }

    public static AdvancementResult attemptBreakthrough(PlayerData data, CultivationPath path) {
        RealmDefinition current = getCurrentRealm(data, path);
        int stageNum = getCurrentStageNumber(data, path);
        RealmStageDefinition stageDef = current.getStageByNumber(stageNum);

        if (!current.isFinalStage(stageNum)) {
            return new AdvancementResult(
                    AdvancementOutcome.NOT_AT_FINAL_STAGE,
                    path,
                    current,
                    stageDef,
                    path.getDisplayName() + " precisa atingir o estágio final de " + current.displayName()
                            + " antes da Quebra de Reino."
            );
        }

        RealmDefinition next = RealmRegistry.getNextRealm(current);
        if (next == null) {
            return new AdvancementResult(
                    AdvancementOutcome.MAX_REALM_REACHED,
                    path,
                    current,
                    stageDef,
                    path.getDisplayName() + " já está no último reino registrado (" + current.displayName() + ")."
            );
        }

        int minOpposite = next.requirements().minOppositeRealmOrder();
        if (minOpposite > 0) {
            CultivationPath opposite = (path == CultivationPath.BODY) ? CultivationPath.SPIRIT : CultivationPath.BODY;
            RealmDefinition oppRealm = getCurrentRealm(data, opposite);
            if (oppRealm.order() < minOpposite) {
                return new AdvancementResult(
                        AdvancementOutcome.MISSING_OPPOSITE_REALM,
                        path,
                        current,
                        stageDef,
                        "Requer " + opposite.getDisplayName() + " no Reino de ordem " + minOpposite + " ou superior."
                );
            }
        }

        float qiCost = next.requirements().qiCost();
        if (qiCost > 0.0F && !EnergyService.consumeQi(data, qiCost)) {
            return new AdvancementResult(
                    AdvancementOutcome.INSUFFICIENT_QI,
                    path,
                    current,
                    stageDef,
                    "Qi insuficiente para romper para " + next.displayName() + " (necessário: " + qiCost + " Qi)."
            );
        }

        setRealmAndStage(data, path, next, 1);
        RealmStageDefinition firstStage = next.getStageByNumber(1);
        return new AdvancementResult(
                AdvancementOutcome.SUCCESS,
                path,
                next,
                firstStage,
                path.getDisplayName() + " rompeu para " + next.displayName() + " — " + firstStage.displayName() + "!"
        );
    }

    public static void setRealmAndStage(PlayerData data, CultivationPath path, RealmDefinition realm, int stage) {
        if (data == null || realm == null) {
            return;
        }
        int clampedStage = Mth.clamp(stage, 1, realm.getStageCount());
        CultivationData cult = data.getCultivation();
        if (path == CultivationPath.BODY) {
            cult.setBodyRealmId(realm.id());
            cult.setBodyRealm(realm.order());
            cult.setBodyStage(clampedStage);
        } else {
            cult.setSpiritRealmId(realm.id());
            cult.setSpiritRealm(realm.order());
            cult.setSpiritStage(clampedStage);
        }
        EnergyService.recalculateMaxQi(data);
    }

    public static boolean setRealmAndStageByOrder(PlayerData data, CultivationPath path, int realmOrder, int stage) {
        RealmDefinition def = RealmRegistry.getRealmByOrder(path, realmOrder);
        if (def == null) {
            return false;
        }
        setRealmAndStage(data, path, def, stage);
        return true;
    }

    public static boolean setRealmAndStageById(PlayerData data, CultivationPath path, ResourceLocation realmId, int stage) {
        RealmDefinition def = RealmRegistry.getRealm(realmId);
        if (def == null || def.path() != path) {
            return false;
        }
        setRealmAndStage(data, path, def, stage);
        return true;
    }

    public static void applyCultivationAttributes(ServerPlayer player, PlayerData data) {
        if (player == null || data == null) {
            return;
        }

        AttributeService.runPreservingHealthRatio(player, false, () -> {
            AttributeService.clearByNamePrefix(player, CULTIVATION_MODIFIER_PREFIX);
            AttributeService.removeModifier(player, Attributes.ATTACK_DAMAGE, RealmRegistry.BODY_CULTIVATION_ATTACK_UUID);
            AttributeService.removeModifier(player, Attributes.MAX_HEALTH, RealmRegistry.BODY_CULTIVATION_HEALTH_UUID);

            applyPathAttributes(player, data, CultivationPath.BODY);
            applyPathAttributes(player, data, CultivationPath.SPIRIT);
        });
        EnergyService.recalculateMaxQi(data);
    }

    private static void applyPathAttributes(ServerPlayer player, PlayerData data, CultivationPath path) {
        RealmStageDefinition stage = getCurrentStage(data, path);
        if (stage.attributes().isEmpty()) {
            return;
        }
        String sourceKey = "cultivation." + path.getId();
        List<AttributeModifierSpec> specs = new ArrayList<>(stage.attributes().size());
        for (RealmAttributeBonus bonus : stage.attributes()) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(bonus.attributeId());
            if (attribute == null) {
                continue;
            }
            String modName = CULTIVATION_MODIFIER_PREFIX + path.getId() + "." + bonus.attributeId().getPath();
            specs.add(new AttributeModifierSpec(
                    attribute,
                    bonus.operation(),
                    bonus.amount(),
                    bonus.fixedUuid(),
                    modName
            ));
        }
        AttributeService.applyModifiers(player, sourceKey, specs);
    }
}
