package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.world.entity.LivingEntity;

public final class ProjectilePhysics {
    private ProjectilePhysics() {}

    public static float entityDamage(ProjectilePhysicsProfile profile, float baseDamage, double speed, LivingEntity target) {
        return me.mss1r.axiomata.ballistics.ProjectilePhysics.entityDamage(profile, baseDamage, speed, target,
                target instanceof AbstractSiegeEntity);
    }

    public static float damageWithArmorPiercing(ProjectilePhysicsProfile profile, float rawDamage, LivingEntity target) {
        return me.mss1r.axiomata.ballistics.ProjectilePhysics.damageWithArmorPiercing(profile, rawDamage, target);
    }

    public static double remainingEntityPenetrationSpeed(SiegeProjectile projectile, LivingEntity target, double currentSpeed) {
        return me.mss1r.axiomata.ballistics.ProjectilePhysics.remainingEntityPenetrationSpeed(projectile, target, currentSpeed);
    }
}
