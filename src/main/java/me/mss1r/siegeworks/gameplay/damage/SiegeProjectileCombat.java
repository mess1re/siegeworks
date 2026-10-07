package me.mss1r.siegeworks.gameplay.damage;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeRelation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class SiegeProjectileCombat {
    private SiegeProjectileCombat() {
    }

    public static boolean mayHit(SiegeProjectile projectile, Entity target) {
        Entity attacker = responsibleAttacker(projectile);
        Entity logicalTarget = logicalTarget(target);
        return logicalTarget != logicalTarget(projectile.getOwner())
                && logicalTarget != logicalTarget(attacker)
                && !isFriendlyFireProtected(attacker, logicalTarget);
    }

    public static boolean damage(SiegeProjectile projectile, Entity target,
                                 float damage, boolean breakShield) {
        LivingEntity livingTarget = livingTarget(target);
        if (livingTarget == null) {
            return false;
        }
        if (breakShield) {
            breakBlockingShield(livingTarget);
        }

        if (projectile.getOwner() instanceof AbstractSiegeEntity) {
            return target.hurt(target.damageSources().thrown(projectile, responsibleAttacker(projectile)), damage);
        }
        return target.hurt(target.damageSources().generic(), damage);
    }

    public static Entity responsibleAttacker(SiegeProjectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner instanceof AbstractSiegeEntity siege) {
            Entity operator = siege.getOperator();
            return operator == null ? siege : operator;
        }
        return owner;
    }

    @Nullable
    public static Entity logicalTarget(@Nullable Entity target) {
        return me.mss1r.axiomata.ballistics.ProjectileCombat.logicalTarget(target);
    }

    @Nullable
    public static LivingEntity livingTarget(Entity target) {
        return me.mss1r.axiomata.ballistics.ProjectileCombat.livingTarget(target);
    }

    public static int targetId(Entity target) {
        return me.mss1r.axiomata.ballistics.ProjectileCombat.targetId(target);
    }

    private static boolean isFriendlyFireProtected(Entity attacker, Entity target) {
        if (attacker == null || attacker.getTeam() == null || attacker.getTeam().isAllowFriendlyFire()) {
            return false;
        }

        if (target instanceof AbstractSiegeEntity siegeTarget) {
            if (siegeTarget.getOwnerUuid() != null) {
                return SiegeAccess.relationOf(attacker, siegeTarget) != SiegeRelation.HOSTILE;
            }
            Entity targetOwner = siegeTarget.getOperator();
            if (targetOwner != null && targetOwner != siegeTarget) {
                return attacker.isAlliedTo(targetOwner);
            }
            return ("team:" + attacker.getTeam().getName()).equals(siegeTarget.getDeploymentGroup());
        }
        return attacker.isAlliedTo(target);
    }

    private static void breakBlockingShield(LivingEntity target) {
        me.mss1r.axiomata.ballistics.ProjectileCombat.breakBlockingShield(target);
    }
}
