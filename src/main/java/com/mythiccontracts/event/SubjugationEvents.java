package com.mythiccontracts.event;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.entity.ai.FollowOwnerSubjugatedGoal;
import com.mythiccontracts.entity.ai.OwnerHurtByTargetSubjugatedGoal;
import com.mythiccontracts.entity.ai.OwnerHurtTargetSubjugatedGoal;
import com.mythiccontracts.item.SubjugationSealItem;
import com.mythiccontracts.subjugation.SubjugationService;
import com.mythiccontracts.util.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SubjugationEvents {

    private SubjugationEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // Caso 1: Entidade Subjugada
        if (mob.getPersistentData().hasUUID(SubjugationSealItem.TAG_SUBJUGATED_OWNER)) {
            mob.addTag(SubjugationSealItem.ENTITY_TAG_SUBJUGATED);
            injectSubjugatedAI(mob);
            return;
        }

        // Caso 2: Aura de Temor (Low Tier Celestial / Fiend foge de Quebradores de Tronos)
        if (mob instanceof PathfinderMob pathfinder && mob.getType().is(ModTags.LOW_TIER_CELESTIAL_FIEND)) {
            pathfinder.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                    pathfinder,
                    Player.class,
                    12.0F,
                    1.2D,
                    1.5D,
                    living -> living instanceof Player p && ModCapabilities.get(p)
                            .map(d -> d.getFaction() == FactionType.QUEBRADORES_DE_TRONOS)
                            .orElse(false)
            ));
        }
    }

    public static void injectSubjugatedAI(Mob mob) {
        mob.targetSelector.getAvailableGoals().removeIf(wrapped ->
                !(wrapped.getGoal() instanceof OwnerHurtByTargetSubjugatedGoal)
                        && !(wrapped.getGoal() instanceof OwnerHurtTargetSubjugatedGoal)
        );
        mob.goalSelector.getAvailableGoals().removeIf(wrapped ->
                wrapped.getGoal() instanceof FollowOwnerSubjugatedGoal
        );

        mob.targetSelector.addGoal(1, new OwnerHurtByTargetSubjugatedGoal(mob));
        mob.targetSelector.addGoal(2, new OwnerHurtTargetSubjugatedGoal(mob));
        mob.goalSelector.addGoal(2, new FollowOwnerSubjugatedGoal(mob, 1.2D, 6.0F, 3.0F, 24.0F));
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        UUID ownerUUID = SubjugationSealItem.getOwnerUUID(entity);
        if (ownerUUID == null) {
            return;
        }

        LivingEntity newTarget = event.getNewTarget();
        if (newTarget != null && SubjugationSealItem.isAlliedWithOwner(newTarget, ownerUUID)) {
            event.setNewTarget(null);
            event.setCanceled(true);
            if (entity instanceof Warden warden) {
                warden.clearAnger(newTarget);
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        Entity directAttacker = event.getSource().getDirectEntity();

        if (isFriendlyFire(attacker, victim) || isFriendlyFire(directAttacker, victim)) {
            event.setCanceled(true);
            if (attacker instanceof Warden warden) {
                warden.clearAnger(victim);
            }
            if (attacker instanceof Mob mob && mob.getTarget() == victim) {
                mob.setTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onServantHurtTarget(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        UUID ownerUuid = SubjugationSealItem.getOwnerUUID(attacker);
        if (ownerUuid == null || event.getAmount() <= 0.0F) {
            return;
        }
        int grade = attacker.getPersistentData().getInt(SubjugationService.TAG_SERVANT_GRADE);
        if (grade >= 4 && event.getEntity().level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerUuid);
            if (owner != null) {
                boolean infusion = ModCapabilities.get(owner).map(d -> d.isManaInfusionActive()).orElse(false);
                if (infusion) {
                    event.setAmount(event.getAmount() * 1.20F);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServantDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()
                && event.getEntity().level() instanceof ServerLevel serverLevel
                && SubjugationSealItem.isSubjugated(event.getEntity())) {
            SubjugationService.onWorldServantDied(serverLevel, event.getEntity());
        }
    }

    private static boolean isFriendlyFire(Entity attacker, LivingEntity victim) {
        if (attacker == null || victim == null) {
            return false;
        }
        UUID attackerOwner = SubjugationSealItem.getOwnerUUID(attacker);
        if (attackerOwner != null && SubjugationSealItem.isAlliedWithOwner(victim, attackerOwner)) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Mob mob)) {
            return;
        }
        if (!mob.getTags().contains(SubjugationSealItem.ENTITY_TAG_SUBJUGATED) && mob.tickCount > 2) {
            return;
        }

        UUID ownerUUID = SubjugationSealItem.getOwnerUUID(mob);
        if (ownerUUID == null) {
            return;
        }
        mob.addTag(SubjugationSealItem.ENTITY_TAG_SUBJUGATED);

        if (mob.getTarget() != null && SubjugationSealItem.isAlliedWithOwner(mob.getTarget(), ownerUUID)) {
            mob.setTarget(null);
        }

        Player owner = mob.level().getPlayerByUUID(ownerUUID);

        // Grau III+: regeneração passiva fora de combate
        int grade = mob.getPersistentData().getInt(SubjugationService.TAG_SERVANT_GRADE);
        if (grade >= 3 && mob.tickCount % 40 == 0 && mob.getTarget() == null && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(1.5F);
        }

        // Postura Agressiva: adquire hostis próximos ao mestre/servo
        if (owner != null && mob.tickCount % 15 == 0
                && mob.getTarget() == null
                && "AGGRESSIVE".equals(mob.getPersistentData().getString(SubjugationService.TAG_SERVANT_STANCE))) {
            Monster nearbyHostile = mob.level().getEntitiesOfClass(
                            Monster.class,
                            mob.getBoundingBox().inflate(12.0D),
                            m -> m.isAlive() && !SubjugationSealItem.isAlliedWithOwner(m, ownerUUID))
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (nearbyHostile != null) {
                mob.setTarget(nearbyHostile);
            }
        }

        if (mob instanceof Warden warden) {
            if (owner != null) {
                warden.clearAnger(owner);
            }
            LivingEntity currentTarget = warden.getTarget();
            if (currentTarget != null && SubjugationSealItem.isAlliedWithOwner(currentTarget, ownerUUID)) {
                warden.clearAnger(currentTarget);
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            }

            if (owner != null && mob.tickCount % 10 == 0) {
                LivingEntity desiredTarget = findOwnerCombatTarget(owner, mob, ownerUUID);
                if (desiredTarget != null) {
                    mob.setTarget(desiredTarget);
                    warden.increaseAngerAt(desiredTarget, 100, true);
                    warden.setAttackTarget(desiredTarget);
                }
            }
        }

        if (mob instanceof WitherBoss wither) {
            for (int i = 0; i < 3; i++) {
                int altId = wither.getAlternativeTarget(i);
                if (altId > 0) {
                    Entity altEntity = wither.level().getEntity(altId);
                    if (altEntity == null || !altEntity.isAlive() || SubjugationSealItem.isAlliedWithOwner(altEntity, ownerUUID)) {
                        wither.setAlternativeTarget(i, 0);
                    }
                }
            }
            LivingEntity mainTarget = mob.getTarget();
            if (mainTarget != null && mainTarget.isAlive() && !SubjugationSealItem.isAlliedWithOwner(mainTarget, ownerUUID)) {
                wither.setAlternativeTarget(0, mainTarget.getId());
            } else if (owner != null && mob.tickCount % 10 == 0) {
                LivingEntity desiredTarget = findOwnerCombatTarget(owner, mob, ownerUUID);
                if (desiredTarget != null) {
                    mob.setTarget(desiredTarget);
                    wither.setAlternativeTarget(0, desiredTarget.getId());
                }
            }
        }
    }

    private static LivingEntity findOwnerCombatTarget(Player owner, Mob servant, UUID ownerUUID) {
        LivingEntity lastHurt = owner.getLastHurtMob();
        if (lastHurt != null && lastHurt.isAlive() && lastHurt != servant
                && !SubjugationSealItem.isAlliedWithOwner(lastHurt, ownerUUID)
                && (owner.tickCount - owner.getLastHurtMobTimestamp()) < 200) {
            return lastHurt;
        }
        LivingEntity lastHurtBy = owner.getLastHurtByMob();
        if (lastHurtBy != null && lastHurtBy.isAlive() && lastHurtBy != servant
                && !SubjugationSealItem.isAlliedWithOwner(lastHurtBy, ownerUUID)
                && (owner.tickCount - owner.getLastHurtByMobTimestamp()) < 200) {
            return lastHurtBy;
        }
        return null;
    }
}
