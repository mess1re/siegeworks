package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class MangonelPassengerProjectile extends SiegeProjectile {
    public static final double PASSENGER_Y_OFFSET = -12.0D / 16.0D;

    private double peakFlightY = Double.NEGATIVE_INFINITY;

    public MangonelPassengerProjectile(EntityType<? extends MangonelPassengerProjectile> type, Level level) {
        super(type, level);
    }

    public MangonelPassengerProjectile(EntityType<MangonelPassengerProjectile> type,
                                        LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (hasPassenger(passenger)) {
            moveFunction.accept(passenger, getX(), getY() + PASSENGER_Y_OFFSET, getZ());
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        Vec3 inheritedVelocity = getDeltaMovement();
        super.removePassenger(passenger);
        passenger.setDeltaMovement(inheritedVelocity);
        preserveFallDistance(passenger);
        passenger.hurtMarked = true;
    }

    @Override
    public void tick() {
        peakFlightY = Math.max(peakFlightY, getY());
        super.tick();
        peakFlightY = Math.max(peakFlightY, getY());
        if (!level().isClientSide && tickCount > 2 && getPassengers().isEmpty()) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        releasePassengerAtImpact();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        releasePassengerAtImpact();
    }

    private void releasePassengerAtImpact() {
        Vec3 inheritedVelocity = getDeltaMovement().scale(0.45D);
        for (Entity passenger : getPassengers()) {
            passenger.stopRiding();
            passenger.setDeltaMovement(inheritedVelocity);
            preserveFallDistance(passenger);
            passenger.hurtMarked = true;
        }
        discard();
    }

    private void preserveFallDistance(Entity passenger) {
        if (passenger instanceof LivingEntity livingPassenger && Double.isFinite(peakFlightY)) {
            float flightFallDistance = (float) Math.max(0.0D, peakFlightY - getY());
            livingPassenger.fallDistance = Math.max(livingPassenger.fallDistance, flightFallDistance);
        }
    }

    @Override
    public ProjectilePhysicsProfile getPhysicsProfile() {
        return SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.MANGONEL_PROJECTILE.get());
    }
}
