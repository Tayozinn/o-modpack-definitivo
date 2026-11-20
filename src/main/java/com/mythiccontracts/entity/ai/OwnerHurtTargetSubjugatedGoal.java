package com.mythiccontracts.entity.ai;

import com.mythiccontracts.item.SubjugationSealItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

public class OwnerHurtTargetSubjugatedGoal extends TargetGoal {
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public OwnerHurtTargetSubjugatedGoal(Mob mob) {
        super(mob, false);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
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
        Player owner = this.findOwner();
        if (owner == null) {
            return false;
        }
        this.ownerLastHurt = owner.getLastHurtMob();
        int hurtTimestamp = owner.getLastHurtMobTimestamp();
        if (hurtTimestamp == this.timestamp || this.ownerLastHurt == null || !this.ownerLastHurt.isAlive()) {
            return false;
        }
        if (this.ownerLastHurt == this.mob || SubjugationSealItem.isAlliedWithOwner(this.ownerLastHurt, owner.getUUID())) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        if (this.mob instanceof Warden warden && this.ownerLastHurt != null) {
            warden.increaseAngerAt(this.ownerLastHurt, 100, true);
            warden.setAttackTarget(this.ownerLastHurt);
        }
        Player owner = this.findOwner();
        if (owner != null) {
            this.timestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
