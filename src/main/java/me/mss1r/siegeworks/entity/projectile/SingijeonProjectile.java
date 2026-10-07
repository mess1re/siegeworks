package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class SingijeonProjectile extends SiegeProjectile {
    private static final int EMBEDDED_LIFETIME_TICKS = 20 * 90;
    private static final int MAX_FLIGHT_LIFETIME_TICKS = 20 * 12;
    /** How far, in blocks, the motor drives a rocket along its rack before it flies free. */
    private static final double RACK_LENGTH = 1.0D;
    private static final String TAG_EXPLOSIVE = "Explosive";
    private static final String TAG_EMBEDDED_AGE = "EmbeddedAge";
    private static final String TAG_EMBEDDED_BLOCK = "EmbeddedBlock";
    private static final String TAG_MOTOR_TICKS = "MotorTicks";
    private static final String TAG_MOTOR_DIRECTION_X = "MotorDirectionX";
    private static final String TAG_MOTOR_DIRECTION_Y = "MotorDirectionY";
    private static final String TAG_MOTOR_DIRECTION_Z = "MotorDirectionZ";
    private static final String TAG_MOTOR_THRUST = "MotorThrust";
    private static final EntityDataAccessor<Boolean> EXPLOSIVE =
            SynchedEntityData.defineId(SingijeonProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MOTOR_BURNING =
            SynchedEntityData.defineId(SingijeonProjectile.class, EntityDataSerializers.BOOLEAN);

    private float previousRenderYaw;
    private float previousRenderPitch;
    private int embeddedAge;
    private BlockPos embeddedBlockPos;
    private int motorTicks;
    private Vec3 motorDirection = Vec3.ZERO;
    private double motorThrust;

    public SingijeonProjectile(EntityType<? extends SingijeonProjectile> type, Level level) {
        super(type, level);
    }

    public SingijeonProjectile(EntityType<? extends SingijeonProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    //? if forge {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(EXPLOSIVE, false);
        data.define(MOTOR_BURNING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return SiegeworksItems.SO_SINGIJEON.get();
    }

    public boolean isExplosive() {
        return entityData.get(EXPLOSIVE);
    }

    public void setExplosive(boolean explosive) {
        entityData.set(EXPLOSIVE, explosive);
        if (explosive) {
            setPhysicsProfile(ProjectileVariants.EXPLOSIVE_SINGIJEON);
        }
    }

    /** Profile for a plain or explosive rocket. */
    public static ProjectilePhysicsProfile physics(boolean explosive) {
        return explosive
                ? SiegeProfileCatalogs.PROJECTILES.get(ProjectileVariants.EXPLOSIVE_SINGIJEON)
                : SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.SINGIJEON_PROJECTILE.get());
    }

    /** Flight after leaving the rack, used for aiming. */
    public static SiegeBallistics.Flight flight(ProjectilePhysicsProfile physics) {
        double diameter = physics.diameterOf(SiegeworksEntities.SINGIJEON_PROJECTILE.get());
        return new SiegeBallistics.Flight(SiegeBallistics.GRAVITY, physics.airDrag(diameter),
                motorAcceleration(physics), motorBurnTicks(physics));
    }

    /** Speed, in blocks/tick, when leaving the rack after being pushed its full length. */
    public static double launchSpeed(ProjectilePhysicsProfile physics) {
        return Math.sqrt(2.0D * motorAcceleration(physics) * RACK_LENGTH);
    }

    /** Speed added by the motor per tick, in blocks/tick: thrust / mass. */
    private static double motorAcceleration(ProjectilePhysicsProfile physics) {
        return physics.motor().map(motor -> motor.thrust() / physics.mass() / 400.0D).orElse(0.0D);
    }

    private static int motorBurnTicks(ProjectilePhysicsProfile physics) {
        return physics.motor().map(motor -> (int) Math.round(motor.burnTime() * 20.0D)).orElse(0);
    }

    /** Launches along {@code direction} with the motor at {@code thrustScale} times normal thrust. */
    public void launchWithMotor(Vec3 direction, double thrustScale) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        Vec3 normalizedDirection = direction.normalize();
        motorDirection = normalizedDirection;
        motorTicks = motorBurnTicks(physics);
        motorThrust = Math.max(0.0D, thrustScale) * motorAcceleration(physics);
        entityData.set(MOTOR_BURNING, motorTicks > 0);
        setDeltaMovement(normalizedDirection.scale(Math.sqrt(2.0D * motorThrust * RACK_LENGTH)));
        alignToMovement(normalizedDirection);
    }

    @Override
    public void tick() {
        previousRenderYaw = getYRot();
        previousRenderPitch = getXRot();

        if (inGround) {
            if (embeddedBlockPos != null && level().isEmptyBlock(embeddedBlockPos)) {
                releaseFromBlock();
            } else {
                super.baseTick();
                if (!level().isClientSide() && ++embeddedAge > EMBEDDED_LIFETIME_TICKS) {
                    discard();
                }
                return;
            }
        }

        if (!level().isClientSide() && motorTicks > 0) {
            Vec3 movement = getDeltaMovement();
            Vec3 heading = movement.lengthSqr() > 1.0E-7D ? movement.normalize() : motorDirection;
            setDeltaMovement(movement.add(heading.scale(motorThrust)));
            motorTicks--;
            if (motorTicks == 0) {
                entityData.set(MOTOR_BURNING, false);
            }
        }

        super.tick();
        if (isRemoved()) {
            return;
        }
        alignToMovement(getDeltaMovement());
        if (level().isClientSide()) {
            spawnFlightTrail();
        }
        if (!level().isClientSide() && !inGround && tickCount > MAX_FLIGHT_LIFETIME_TICKS) {
            discard();
        }
    }

    public void alignToMovement(Vec3 movement) {
        if (movement.lengthSqr() < 1.0E-7D) {
            return;
        }
        Vec3 direction = movement.normalize();
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        setYRot((float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG));
    }

    public float getPreviousRenderYaw() {
        return tickCount <= 1 ? getYRot() : previousRenderYaw;
    }

    public float getPreviousRenderPitch() {
        return tickCount <= 1 ? getXRot() : previousRenderPitch;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide()) {
            return;
        }
        Entity hitTarget = hit.getEntity();
        LivingEntity directTarget = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);
        boolean directDamageApplied = false;
        if (directTarget != null) {
            float damage = ProjectilePhysics.entityDamage(getPhysicsProfile(), (float) getBaseDamage(),
                    getDeltaMovement().length(), directTarget);
            directDamageApplied = damageTarget(hitTarget, damage);
        }
        if (isExplosive()) {
            if (directDamageApplied) {
                applyBlastEffects(directTarget, hit.getLocation());
            }
            explode(hit.getLocation(), directDamageApplied ? directTarget : null);
        } else {
            playNormalImpact(hit.getLocation());
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ProjectileImpacts.Drive drive = driveInto(serverLevel, hit);
        if (drive.passedThrough()) {
            return;
        }
        if (isExplosive()) {
            explode(drive.position(), null);
        } else {
            playNormalImpact(drive.position());
            embedInBlock(new BlockHitResult(drive.position(), hit.getDirection(), drive.block(), false));
        }
    }

    private void embedInBlock(BlockHitResult hit) {
        Vec3 movement = getDeltaMovement();
        Vec3 direction = movement.lengthSqr() > 1.0E-7D
                ? movement.normalize()
                : Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
        Vec3 embeddedPosition = hit.getLocation().subtract(direction.scale(0.12D));
        setPos(embeddedPosition.x, embeddedPosition.y, embeddedPosition.z);
        setDeltaMovement(Vec3.ZERO);
        alignToMovement(direction);
        setNoGravity(true);
        inGround = true;
        stopMotor();
        embeddedAge = 0;
        embeddedBlockPos = hit.getBlockPos();
        hasImpulse = true;
    }

    private void stopMotor() {
        motorTicks = 0;
        motorDirection = Vec3.ZERO;
        motorThrust = 0.0D;
        entityData.set(MOTOR_BURNING, false);
    }

    private void releaseFromBlock() {
        inGround = false;
        embeddedAge = 0;
        embeddedBlockPos = null;
        setNoGravity(false);
        setDeltaMovement(0.0D, -0.12D, 0.0D);
        hasImpulse = true;
    }

    private void spawnFlightTrail() {
        boolean motorBurning = entityData.get(MOTOR_BURNING);
        if (!motorBurning && tickCount % 4 != 0) {
            return;
        }
        Vec3 movement = getDeltaMovement();
        Vec3 trailOffset = movement.lengthSqr() > 1.0E-7D
                ? movement.normalize().scale(-0.22D)
                : Vec3.ZERO;
        double x = getX() + trailOffset.x;
        double y = getY() + trailOffset.y;
        double z = getZ() + trailOffset.z;
        if (motorBurning) {
            level().addParticle(ParticleTypes.CLOUD, x, y, z,
                    -movement.x * 0.022D, -movement.y * 0.022D, -movement.z * 0.022D);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, x, y, z,
                        -movement.x * 0.015D, -movement.y * 0.015D, -movement.z * 0.015D);
            }
            if (tickCount % 3 == 0) {
                level().addParticle(ParticleTypes.FIREWORK, x, y, z,
                        -movement.x * 0.035D, -movement.y * 0.035D, -movement.z * 0.035D);
                level().addParticle(ParticleTypes.CRIT, x, y, z,
                        -movement.x * 0.025D, -movement.y * 0.025D, -movement.z * 0.025D);
            }
            if (tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, x, y, z,
                        -movement.x * 0.012D, -movement.y * 0.012D, -movement.z * 0.012D);
            }
        } else {
            level().addParticle(ParticleTypes.SMOKE, x, y, z,
                    -movement.x * 0.01D, -movement.y * 0.01D, -movement.z * 0.01D);
        }
    }

    private void playNormalImpact(Vec3 center) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.8F, 1.25F);
        serverLevel.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                7, 0.12D, 0.12D, 0.12D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.POOF, center.x, center.y, center.z,
                3, 0.08D, 0.08D, 0.08D, 0.02D);
    }

    private void explode(Vec3 center, LivingEntity excludedTarget) {
        if (level() instanceof ServerLevel serverLevel) {
            double speed = getDeltaMovement().length();
            float explosionPitch = 0.92F + random.nextFloat() * 0.2F;
            float fireworkPitch = 0.78F + random.nextFloat() * 0.28F;
            serverLevel.playSound(null, center.x, center.y, center.z,
                    me.mss1r.siegeworks.platform.MinecraftVersionCompat.genericExplodeSound(),
                    SoundSource.PLAYERS, 1.65F, explosionPitch);
            serverLevel.playSound(null, center.x, center.y, center.z,
                    random.nextBoolean() ? SoundEvents.FIREWORK_ROCKET_BLAST
                            : SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                    SoundSource.PLAYERS, 1.3F, fireworkPitch);
            if (random.nextFloat() < 0.45F) {
                serverLevel.playSound(null, center.x, center.y, center.z,
                        SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS,
                        0.55F, 0.9F + random.nextFloat() * 0.25F);
            }
            SiegeParticleEffects.rocketExplosion(serverLevel, center);
            arrive(serverLevel, center, speed);
            ProjectileBlastResolver.applyShock(serverLevel, center, this, getPhysicsProfile(), excludedTarget, speed)
                    .forEach(target -> applyBlastEffects(target, center));
        }
        discard();
    }

    private static void applyBlastEffects(LivingEntity target, Vec3 center) {
        //? if forge {
        /*target.setSecondsOnFire(5);
        *///?} else {
        target.igniteForSeconds(5.0F);
        //?}
        Vec3 away = target.position().subtract(center);
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() > 1.0E-5D) {
            horizontal = horizontal.normalize().scale(0.38D);
            target.push(horizontal.x, 0.16D, horizontal.z);
            target.hurtMarked = true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_EXPLOSIVE, isExplosive());
        tag.putInt(TAG_EMBEDDED_AGE, embeddedAge);
        if (embeddedBlockPos != null) {
            tag.putLong(TAG_EMBEDDED_BLOCK, embeddedBlockPos.asLong());
        }
        tag.putInt(TAG_MOTOR_TICKS, motorTicks);
        if (motorTicks > 0) {
            tag.putDouble(TAG_MOTOR_DIRECTION_X, motorDirection.x);
            tag.putDouble(TAG_MOTOR_DIRECTION_Y, motorDirection.y);
            tag.putDouble(TAG_MOTOR_DIRECTION_Z, motorDirection.z);
            tag.putDouble(TAG_MOTOR_THRUST, motorThrust);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setExplosive(tag.getBoolean(TAG_EXPLOSIVE));
        embeddedAge = Math.max(0, tag.getInt(TAG_EMBEDDED_AGE));
        embeddedBlockPos = tag.contains(TAG_EMBEDDED_BLOCK)
                ? BlockPos.of(tag.getLong(TAG_EMBEDDED_BLOCK))
                : null;
        motorTicks = Math.max(0, tag.getInt(TAG_MOTOR_TICKS));
        if (motorTicks > 0) {
            motorDirection = new Vec3(
                    tag.getDouble(TAG_MOTOR_DIRECTION_X),
                    tag.getDouble(TAG_MOTOR_DIRECTION_Y),
                    tag.getDouble(TAG_MOTOR_DIRECTION_Z));
            motorThrust = Math.max(0.0D, tag.getDouble(TAG_MOTOR_THRUST));
        } else {
            motorDirection = Vec3.ZERO;
            motorThrust = 0.0D;
        }
        entityData.set(MOTOR_BURNING, motorTicks > 0);
        setNoGravity(inGround);
    }
}
