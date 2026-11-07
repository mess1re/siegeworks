package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.axiomata.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
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
            playPenetrationReport(serverLevel, blockHitResult.getLocation());
            ProjectileImpactEffects.spawnPenetrationParticles(serverLevel, blockHitResult.getLocation(),
                    getPhysicsProfile().diameterOf(this), impactOutwardDirection());
            return;
        }
        stopAt(serverLevel, drive.position(), drive.speed());
    }

    protected void playPenetrationReport(ServerLevel level, Vec3 impact) {
        ProjectileImpactEffects.playPenetrationReport(level, impact, 2.2F, 1.1F);
    }

    /** The projectile's end where a block stopped it. */
    protected void stopAt(ServerLevel serverLevel, Vec3 impact, double speed) {
        arrive(serverLevel, impact, speed);
        playImpactReport(serverLevel, impact, 5.0f, 0.85f);
        this.inGround = false;
        this.shakeTime = 0;
        this.setImpactSound(SoundEvents.ARROW_HIT);
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);

        if (this.level() instanceof ServerLevel serverLevel && target != null) {
            Vec3 impact = entityHitResult.getLocation();
            var result = impactTarget(hitTarget, impact);
            playImpactReport(serverLevel, impact, 4.5f, 0.9f);
            if (result != null && result.physics().passedThrough()) {
                continueThrough(result.physics());
                return;
            }
        }

        this.discard();
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
        //? if <1.21 {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }
}
