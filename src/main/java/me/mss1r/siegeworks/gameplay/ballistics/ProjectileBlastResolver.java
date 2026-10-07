package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.gameplay.damage.SiegeProjectileCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class ProjectileBlastResolver {
    private ProjectileBlastResolver() {}

    public static List<LivingEntity> applyImpactShockDamageAndCollect(
            ServerLevel level, Vec3 center, @Nullable Entity directSource,
            @Nullable Entity owner, ProjectilePhysicsProfile physics,
            @Nullable LivingEntity excludedTarget, double radius, float damage) {
        Entity attacker = directSource instanceof SiegeProjectile projectile
                ? SiegeProjectileCombat.responsibleAttacker(projectile) : owner;
        return me.mss1r.axiomata.ballistics.ProjectileBlastResolver.applyImpactShockDamageAndCollect(
                level, center, directSource, attacker, physics, excludedTarget, radius, damage);
    }

    public static List<LivingEntity> applyShock(ServerLevel level, Vec3 center, SiegeProjectile projectile,
                                               ProjectilePhysicsProfile physics,
                                               @Nullable LivingEntity excludedTarget, double speed) {
        ProjectilePhysicsProfile.Shock shock = physics.shock();
        return applyImpactShockDamageAndCollect(level, center, projectile, projectile.getOwner(), physics,
                excludedTarget, Math.max(shock.radius(), shock.radiusAt(speed)),
                (float) (projectile.getBaseDamage() * shock.damageAt(speed)));
    }
}
