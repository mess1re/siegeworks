package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.axiomata.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class CannonProjectile extends SiegeProjectile {
    private Vec3 lastFlightMovement = Vec3.ZERO;

    public CannonProjectile(EntityType<? extends CannonProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public CannonProjectile(EntityType<? extends CannonProjectile> cannonProjectile, LivingEntity shooter, Level level) {
        super(cannonProjectile, shooter, level);
    }

    @Override
    public void tick() {
        Vec3 previousPos = position();
        Vec3 previousMovement = getDeltaMovement();
        if (previousMovement.lengthSqr() > 1.0E-6D) {
            lastFlightMovement = previousMovement;
        }

        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (isRemoved()) {
                return;
            }

            if (this.inGround) {
                resolveEmbeddedProjectile(serverLevel);
                return;
            }

            sweepMissedBlockHit(serverLevel, previousPos, position(), previousMovement);
        }
    }

    private boolean sweepMissedBlockHit(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos, Vec3 previousMovement) {
        return ProjectileSweep.hitFirstBlockingBlock(serverLevel, previousPos, currentPos, previousMovement,
                this::shouldIgnoreSweepBlock, this::onHitBlock);
    }

    private boolean shouldIgnoreSweepBlock(Level world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty()) {
            return true;
        }
        return state.getCollisionShape(world, pos).isEmpty() && state.getDestroySpeed(world, pos) <= 0.05F;
    }

    private void resolveEmbeddedProjectile(ServerLevel serverLevel) {
        this.inGround = false;
        this.shakeTime = 0;
        stopAt(serverLevel, position(), lastFlightMovement.length());
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ProjectileImpacts.Drive drive = driveInto(serverLevel, blockHitResult);
        if (drive.passedThrough()) {
            ProjectileImpactEffects.playPenetrationReport(serverLevel, blockHitResult.getLocation(), 2.2f, 1.1f);
            ProjectileImpactEffects.spawnPenetrationParticles(serverLevel, blockHitResult.getLocation(),
                    getPhysicsProfile().diameterOf(this), impactOutwardDirection());
            return;
        }
        stopAt(serverLevel, drive.position(), drive.speed());
    }

    /** The projectile's end where a block stopped it. */
    protected void stopAt(ServerLevel serverLevel, Vec3 impact, double speed) {
        arrive(serverLevel, impact, speed);
        playImpactReport(serverLevel, impact, 5.0f, 0.85f);
        ProjectileBlastResolver.applyShock(serverLevel, impact, this, getPhysicsProfile(), null, speed);
        this.inGround = false;
        this.shakeTime = 0;
        this.setImpactSound(SoundEvents.ARROW_HIT);
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);

        if (this.level() instanceof ServerLevel serverLevel && target != null) {
            Vec3 impact = entityHitResult.getLocation();
            double speed = getDeltaMovement().length();
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            boolean directDamageApplied = damageTarget(hitTarget, damage);
            playImpactReport(serverLevel, impact, 4.5f, 0.9f);
            ProjectileBlastResolver.applyShock(serverLevel, impact, this, physics,
                    directDamageApplied ? target : null, speed);

            if (tryKineticEntityPenetration(target, speed)) {
                return;
            }
        }

        this.discard();
    }

    protected boolean tryKineticEntityPenetration(LivingEntity target, double speed) {
        double nextSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(this, target, speed);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        setPos(getX() + direction.x * 0.45, getY() + direction.y * 0.45, getZ() + direction.z * 0.45);
        return true;
    }

    protected void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.playImpactReport(serverLevel, impact, volume, pitch,
                ProjectileImpactEffects.Style.STANDARD, impactOutwardDirection());
    }

    protected Vec3 impactOutwardDirection() {
        Vec3 movement = getDeltaMovement().lengthSqr() > 1.0E-8D ? getDeltaMovement() : lastFlightMovement;
        return movement.lengthSqr() > 1.0E-8D
                ? movement.normalize().reverse()
                : new Vec3(0.0D, 1.0D, 0.0D);
    }

    @Override
    protected SoundEvent getImpactSound() {
        //? if forge {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }
}
