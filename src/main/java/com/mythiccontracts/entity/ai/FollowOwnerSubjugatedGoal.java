package com.mythiccontracts.entity.ai;

import com.mythiccontracts.item.SubjugationSealItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

public class FollowOwnerSubjugatedGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final float startDistance;
    private final float stopDistance;
    private final float teleportDistance;
    private Player owner;
    private int timeToRecalcPath;

    public FollowOwnerSubjugatedGoal(Mob mob, double speedModifier, float startDistance, float stopDistance, float teleportDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.teleportDistance = teleportDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private Player findOwner() {
        UUID ownerUUID = SubjugationSealItem.getOwnerUUID(this.mob);
        if (ownerUUID == null) {
            return null;
        }
        return this.mob.level().getPlayerByUUID(ownerUUID);
    }

    @Override
    public boolean canUse() {
        Player foundOwner = this.findOwner();
        if (foundOwner == null || foundOwner.isSpectator() || !foundOwner.isAlive()) {
            return false;
        }
        if (this.mob.isLeashed()) {
            return false;
        }
        double distSqr = this.mob.distanceToSqr(foundOwner);
        if (distSqr <= (double) (this.startDistance * this.startDistance)) {
            return false;
        }
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()
                && distSqr <= (double) (this.teleportDistance * this.teleportDistance)
                && this.mob.distanceToSqr(currentTarget) < 196.0D) {
            return false;
        }
        this.owner = foundOwner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null || !this.owner.isAlive() || this.owner.isSpectator()) {
            return false;
        }
        double distSqr = this.mob.distanceToSqr(this.owner);
        if (distSqr <= (double) (this.stopDistance * this.stopDistance)) {
            return false;
        }
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()
                && distSqr <= (double) (this.teleportDistance * this.teleportDistance)
                && this.mob.distanceToSqr(currentTarget) < 196.0D) {
            return false;
        }
        return !this.mob.getNavigation().isDone() || distSqr > (double) (this.startDistance * this.startDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.owner == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.owner, 10.0F, (float) this.mob.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            double distSqr = this.mob.distanceToSqr(this.owner);
            if (distSqr > (double) (this.teleportDistance * this.teleportDistance)) {
                this.teleportToOwner();
            } else {
                this.mob.getNavigation().moveTo(this.owner, this.speedModifier);
            }
        }
    }

    private void teleportToOwner() {
        if (this.owner == null) {
            return;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = this.mob.getRandom().nextIntBetweenInclusive(-3, 3);
            int dy = this.mob.getRandom().nextIntBetweenInclusive(-1, 1);
            int dz = this.mob.getRandom().nextIntBetweenInclusive(-3, 3);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            double targetX = this.owner.getX() + (double) dx;
            double targetY = this.owner.getY() + (double) dy;
            double targetZ = this.owner.getZ() + (double) dz;
            if (this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(
                    targetX - this.mob.getX(),
                    targetY - this.mob.getY(),
                    targetZ - this.mob.getZ()
            ))) {
                this.mob.moveTo(targetX, targetY, targetZ, this.mob.getYRot(), this.mob.getXRot());
                this.mob.getNavigation().stop();
                return;
            }
        }
        this.mob.moveTo(this.owner.getX(), this.owner.getY(), this.owner.getZ(), this.mob.getYRot(), this.mob.getXRot());
        this.mob.getNavigation().stop();
    }
}
