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

public class OwnerHurtByTargetSubjugatedGoal extends TargetGoal {
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public OwnerHurtByTargetSubjugatedGoal(Mob mob) {
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
        this.ownerLastHurtBy = owner.getLastHurtByMob();
        int hurtTimestamp = owner.getLastHurtByMobTimestamp();
        if (hurtTimestamp == this.timestamp || this.ownerLastHurtBy == null || !this.ownerLastHurtBy.isAlive()) {
            return false;
        }
        if (this.ownerLastHurtBy == this.mob || SubjugationSealItem.isAlliedWithOwner(this.ownerLastHurtBy, owner.getUUID())) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        if (this.mob instanceof Warden warden && this.ownerLastHurtBy != null) {
            warden.increaseAngerAt(this.ownerLastHurtBy, 100, true);
            warden.setAttackTarget(this.ownerLastHurtBy);
        }
        Player owner = this.findOwner();
        if (owner != null) {
            this.timestamp = owner.getLastHurtByMobTimestamp();
        }
        super.start();
    }
}
