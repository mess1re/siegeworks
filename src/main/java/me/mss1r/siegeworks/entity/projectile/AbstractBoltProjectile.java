package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.axiomata.ballistics.ProjectileSweep;
import me.mss1r.siegeworks.gameplay.ballistics.BoltPinningController;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public abstract class AbstractBoltProjectile extends SiegeProjectile {
    private static final int EMBEDDED_LIFETIME_TICKS = 20 * 90;
    private static final int PIN_DURATION_TICKS = 20 * 15;
    private static final double PIN_SEARCH_DISTANCE = 4.75D;
    private static final double SUPPORT_RELEASE_FALL_SPEED = -0.18D;
    private static final String TAG_IN_GROUND = "InGround";
    private static final String TAG_RENDER_YAW = "RenderYaw";
    private static final String TAG_RENDER_PITCH = "RenderPitch";
    private static final String TAG_EMBEDDED_X = "EmbeddedX";
    private static final String TAG_EMBEDDED_Y = "EmbeddedY";
    private static final String TAG_EMBEDDED_Z = "EmbeddedZ";
    private static final String TAG_ATTACHED_STRUCTURE = "AttachedStructure";
    private static final String TAG_ATTACHED_GROUP = "AttachedGroup";
    private static final String TAG_ATTACHED_PART = "AttachedPart";
    private static final String TAG_ATTACHED_LOCAL_X = "AttachedLocalX";
    private static final String TAG_ATTACHED_LOCAL_Y = "AttachedLocalY";
    private static final String TAG_ATTACHED_LOCAL_Z = "AttachedLocalZ";
    private static final String TAG_ATTACHED_DIRECTION_X = "AttachedDirectionX";
    private static final String TAG_ATTACHED_DIRECTION_Y = "AttachedDirectionY";
    private static final String TAG_ATTACHED_DIRECTION_Z = "AttachedDirectionZ";
    private static final String TAG_ATTACHED_TARGET = "AttachedTarget";
    private static final String TAG_TARGET_LOCAL_X = "TargetLocalX";
    private static final String TAG_TARGET_LOCAL_Y = "TargetLocalY";
    private static final String TAG_TARGET_LOCAL_Z = "TargetLocalZ";
    private static final String TAG_TARGET_DIRECTION_X = "TargetDirectionX";
    private static final String TAG_TARGET_DIRECTION_Y = "TargetDirectionY";
    private static final String TAG_TARGET_DIRECTION_Z = "TargetDirectionZ";
    private static final EntityDataAccessor<Float> RENDER_YAW =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RENDER_PITCH =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> EMBEDDED =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EMBEDDED_X =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EMBEDDED_Y =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EMBEDDED_Z =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ATTACHED_STRUCTURE_ID =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ATTACHED_GROUP =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ATTACHED_PART =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ATTACHED_LOCAL_X =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACHED_LOCAL_Y =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACHED_LOCAL_Z =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACHED_DIRECTION_X =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACHED_DIRECTION_Y =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTACHED_DIRECTION_Z =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ATTACHED_TARGET_ID =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_LOCAL_X =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_LOCAL_Y =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_LOCAL_Z =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_DIRECTION_X =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_DIRECTION_Y =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_DIRECTION_Z =
            SynchedEntityData.defineId(AbstractBoltProjectile.class, EntityDataSerializers.FLOAT);

    private Vec3 lastFlightMovement = Vec3.ZERO;
    private float renderYawO;
    private float renderPitchO;
    private int supportLossTicks;
    private UUID attachedStructureUuid;
    private UUID attachedTargetUuid;
    private final BoltPinningController pinning = new BoltPinningController(new BoltPinningController.Host() {
        @Override public Entity projectile() { return AbstractBoltProjectile.this; }
        @Override public double pinSearchDistance() { return getPinSearchDistance(); }
        @Override public int pinDurationTicks() { return getPinDurationTicks(); }
        @Override public void attach(LivingEntity target, Vec3 impact, Vec3 direction) {
            attachBoltToTarget(target, impact, direction);
        }
    });

    protected AbstractBoltProjectile(EntityType<? extends AbstractBoltProjectile> entityType, Level level) {
        super(entityType, level);
    }

    protected AbstractBoltProjectile(EntityType<? extends AbstractBoltProjectile> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
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
        data.define(RENDER_YAW, 0.0F);
        data.define(RENDER_PITCH, 0.0F);
        data.define(EMBEDDED, false);
        data.define(EMBEDDED_X, 0);
        data.define(EMBEDDED_Y, 0);
        data.define(EMBEDDED_Z, 0);
        data.define(ATTACHED_STRUCTURE_ID, -1);
        data.define(ATTACHED_GROUP, "");
        data.define(ATTACHED_PART, -1);
        data.define(ATTACHED_LOCAL_X, 0.0F);
        data.define(ATTACHED_LOCAL_Y, 0.0F);
        data.define(ATTACHED_LOCAL_Z, 0.0F);
        data.define(ATTACHED_DIRECTION_X, 0.0F);
        data.define(ATTACHED_DIRECTION_Y, 0.0F);
        data.define(ATTACHED_DIRECTION_Z, 1.0F);
        data.define(ATTACHED_TARGET_ID, -1);
        data.define(TARGET_LOCAL_X, 0.0F);
        data.define(TARGET_LOCAL_Y, 0.0F);
        data.define(TARGET_LOCAL_Z, 0.0F);
        data.define(TARGET_DIRECTION_X, 0.0F);
        data.define(TARGET_DIRECTION_Y, 0.0F);
        data.define(TARGET_DIRECTION_Z, 1.0F);
    }

    @Override
    protected abstract Item getDefaultItem();

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_IN_GROUND, inGround);
        tag.putFloat(TAG_RENDER_YAW, getRenderYaw());
        tag.putFloat(TAG_RENDER_PITCH, getRenderPitch());
        BlockPos embeddedPos = getEmbeddedBlockPos();
        if (embeddedPos != null) {
            tag.putInt(TAG_EMBEDDED_X, embeddedPos.getX());
            tag.putInt(TAG_EMBEDDED_Y, embeddedPos.getY());
            tag.putInt(TAG_EMBEDDED_Z, embeddedPos.getZ());
        }
        if (attachedStructureUuid != null) {
            tag.putUUID(TAG_ATTACHED_STRUCTURE, attachedStructureUuid);
            tag.putString(TAG_ATTACHED_GROUP, entityData.get(ATTACHED_GROUP));
            tag.putInt(TAG_ATTACHED_PART, entityData.get(ATTACHED_PART));
            tag.putFloat(TAG_ATTACHED_LOCAL_X, entityData.get(ATTACHED_LOCAL_X));
            tag.putFloat(TAG_ATTACHED_LOCAL_Y, entityData.get(ATTACHED_LOCAL_Y));
            tag.putFloat(TAG_ATTACHED_LOCAL_Z, entityData.get(ATTACHED_LOCAL_Z));
            tag.putFloat(TAG_ATTACHED_DIRECTION_X, entityData.get(ATTACHED_DIRECTION_X));
            tag.putFloat(TAG_ATTACHED_DIRECTION_Y, entityData.get(ATTACHED_DIRECTION_Y));
            tag.putFloat(TAG_ATTACHED_DIRECTION_Z, entityData.get(ATTACHED_DIRECTION_Z));
        }
        if (attachedTargetUuid != null) {
            tag.putUUID(TAG_ATTACHED_TARGET, attachedTargetUuid);
            tag.putFloat(TAG_TARGET_LOCAL_X, entityData.get(TARGET_LOCAL_X));
            tag.putFloat(TAG_TARGET_LOCAL_Y, entityData.get(TARGET_LOCAL_Y));
            tag.putFloat(TAG_TARGET_LOCAL_Z, entityData.get(TARGET_LOCAL_Z));
            tag.putFloat(TAG_TARGET_DIRECTION_X, entityData.get(TARGET_DIRECTION_X));
            tag.putFloat(TAG_TARGET_DIRECTION_Y, entityData.get(TARGET_DIRECTION_Y));
            tag.putFloat(TAG_TARGET_DIRECTION_Z, entityData.get(TARGET_DIRECTION_Z));
        }
        pinning.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        inGround = tag.getBoolean(TAG_IN_GROUND);
        setNoGravity(inGround);
        if (tag.contains(TAG_RENDER_YAW)) {
            this.entityData.set(RENDER_YAW, tag.getFloat(TAG_RENDER_YAW));
        }
        if (tag.contains(TAG_RENDER_PITCH)) {
            this.entityData.set(RENDER_PITCH, tag.getFloat(TAG_RENDER_PITCH));
        }
        renderYawO = getRenderYaw();
        renderPitchO = getRenderPitch();
        if (tag.contains(TAG_EMBEDDED_X) && tag.contains(TAG_EMBEDDED_Y) && tag.contains(TAG_EMBEDDED_Z)) {
            setEmbeddedBlockPos(new BlockPos(tag.getInt(TAG_EMBEDDED_X), tag.getInt(TAG_EMBEDDED_Y), tag.getInt(TAG_EMBEDDED_Z)));
        }
        if (tag.hasUUID(TAG_ATTACHED_STRUCTURE)) {
            attachedStructureUuid = tag.getUUID(TAG_ATTACHED_STRUCTURE);
            entityData.set(ATTACHED_STRUCTURE_ID, -1);
            entityData.set(ATTACHED_GROUP, tag.getString(TAG_ATTACHED_GROUP));
            entityData.set(ATTACHED_PART, tag.getInt(TAG_ATTACHED_PART));
            entityData.set(ATTACHED_LOCAL_X, tag.getFloat(TAG_ATTACHED_LOCAL_X));
            entityData.set(ATTACHED_LOCAL_Y, tag.getFloat(TAG_ATTACHED_LOCAL_Y));
            entityData.set(ATTACHED_LOCAL_Z, tag.getFloat(TAG_ATTACHED_LOCAL_Z));
            entityData.set(ATTACHED_DIRECTION_X, tag.getFloat(TAG_ATTACHED_DIRECTION_X));
            entityData.set(ATTACHED_DIRECTION_Y, tag.getFloat(TAG_ATTACHED_DIRECTION_Y));
            entityData.set(ATTACHED_DIRECTION_Z, tag.getFloat(TAG_ATTACHED_DIRECTION_Z));
            clearEmbeddedBlockPos();
            inGround = true;
            setNoGravity(true);
        }
        if (tag.hasUUID(TAG_ATTACHED_TARGET)) {
            attachedTargetUuid = tag.getUUID(TAG_ATTACHED_TARGET);
            entityData.set(ATTACHED_TARGET_ID, -1);
            entityData.set(TARGET_LOCAL_X, tag.getFloat(TAG_TARGET_LOCAL_X));
            entityData.set(TARGET_LOCAL_Y, tag.getFloat(TAG_TARGET_LOCAL_Y));
            entityData.set(TARGET_LOCAL_Z, tag.getFloat(TAG_TARGET_LOCAL_Z));
            entityData.set(TARGET_DIRECTION_X, tag.getFloat(TAG_TARGET_DIRECTION_X));
            entityData.set(TARGET_DIRECTION_Y, tag.getFloat(TAG_TARGET_DIRECTION_Y));
            entityData.set(TARGET_DIRECTION_Z, tag.getFloat(TAG_TARGET_DIRECTION_Z));
            clearEmbeddedBlockPos();
            clearStructureAttachment();
            inGround = true;
            setNoGravity(true);
        }
        pinning.load(tag);
    }

    @Override
    protected SoundEvent getImpactSound() {
        return SiegeworksSounds.BOLT_IMPACT.get();
    }

    @Override
    protected boolean shouldPredictMotionOnClient() {
        return false;
    }

    @Override
    public void tick() {
        renderYawO = getRenderYaw();
        renderPitchO = getRenderPitch();

        if (hasTargetAttachment()) {
            if (!updateTargetAttachment()) {
                return;
            }
            if (level() instanceof ServerLevel serverLevel) {
                pinning.tick(serverLevel);
            }
            super.baseTick();
            if (!level().isClientSide && tickCount > getEmbeddedLifetimeTicks()) {
                discard();
            }
            return;
        }

        if (hasStructureAttachment()) {
            if (!updateStructureAttachment()) {
                super.tick();
                return;
            }
            super.baseTick();
            if (!level().isClientSide && tickCount > getEmbeddedLifetimeTicks()) {
                discard();
            }
            return;
        }

        if (inGround) {
            if (releaseIfSupportMissing()) {
                Vec3 movement = getDeltaMovement();
                if (movement.lengthSqr() > 1.0E-6D) {
                    lastFlightMovement = movement;
                    setRenderAnglesFromDirection(movement);
                }
                super.tick();
                return;
            }

            if (level() instanceof ServerLevel serverLevel) {
                pinning.tick(serverLevel);
            }
            super.baseTick();
            if (!level().isClientSide && tickCount > getEmbeddedLifetimeTicks()) {
                discard();
            }
            return;
        }

        Vec3 previousPos = position();
        Vec3 previousMovement = getDeltaMovement();
        if (previousMovement.lengthSqr() > 1.0E-6D) {
            lastFlightMovement = previousMovement;
        }

        super.tick();

        if (!level().isClientSide && !isRemoved() && !inGround) {
            Vec3 currentMovement = getDeltaMovement();
            if (currentMovement.lengthSqr() > 1.0E-6D) {
                lastFlightMovement = currentMovement;
                setRenderAnglesFromDirection(currentMovement);
            }
        }

        if (level() instanceof ServerLevel serverLevel && !isRemoved() && !inGround) {
            spawnFlightParticles(serverLevel);
            if (supportLossTicks <= 0) {
                sweepMissedBlockHit(serverLevel, previousPos, position(), previousMovement);
            }
        }

        if (supportLossTicks > 0) {
            supportLossTicks--;
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        Vec3 movement = lastFlightMovement.lengthSqr() > 1.0E-6D ? lastFlightMovement : getDeltaMovement();
        Vec3 direction = movement.lengthSqr() > 1.0E-6D ? movement.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 impact = blockHitResult.getLocation();
        BlockPos embeddedIn = blockHitResult.getBlockPos();
        if (level() instanceof ServerLevel serverLevel) {
            if (getDeltaMovement().lengthSqr() < 1.0E-6D) {
                setDeltaMovement(movement);
            }
            ProjectileImpacts.Drive drive = driveInto(serverLevel, blockHitResult);
            if (!drive.block().equals(blockHitResult.getBlockPos()) || drive.passedThrough()) {
                playPenetration(serverLevel, impact);
            }
            if (drive.passedThrough()) {
                clearEmbeddedBlockPos();
                inGround = false;
                setNoGravity(false);
                lastFlightMovement = getDeltaMovement();
                alignRenderToDirection(direction);
                return;
            }
            impact = drive.position();
            embeddedIn = drive.block();
            arrive(serverLevel, impact, drive.speed());
        }

        embedBolt(impact, direction, embeddedIn);

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                getImpactSound(), SoundSource.PLAYERS, 1.4F, 0.92F + random.nextFloat() * 0.12F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                impact.x, impact.y, impact.z,
                8, 0.08D, 0.08D, 0.08D, 0.025D);
    }

    private void playPenetration(ServerLevel serverLevel, Vec3 impact) {
        ProjectileImpactEffects.playPenetrationReport(serverLevel, impact, 1.35F, 1.25F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                impact.x, impact.y, impact.z,
                10, 0.1D, 0.1D, 0.1D, 0.04D);
        serverLevel.sendParticles(ParticleTypes.POOF,
                impact.x, impact.y, impact.z,
                6, 0.08D, 0.08D, 0.08D, 0.025D);
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        if (!(level() instanceof ServerLevel serverLevel) || target == null) {
            discard();
            return;
        }

        rememberHitTarget(hitTarget);
        double speed = getDeltaMovement().length();
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
        damageTarget(hitTarget, damage);
        Vec3 movement = getDeltaMovement();
        Vec3 direction = movement.lengthSqr() > 1.0E-6D ? movement.normalize() : lastFlightMovement.normalize();

        Vec3 impact = entityHitResult.getLocation();
        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.2F, 0.85F + random.nextFloat() * 0.2F);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                impact.x, impact.y + target.getBbHeight() * 0.35D, impact.z,
                10, 0.12D, 0.12D, 0.12D, 0.04D);

        if (!target.isAlive()) {
            continueAfterLethalHit(target, physics, speed, direction);
            return;
        }

        if (target instanceof AbstractSiegeEntity siege) {
            embedBoltInStructure(siege, impact, direction);
            return;
        }

        if (pinning.tryPin(serverLevel, target, impact, direction)) {
            serverLevel.playSound(null, impact.x, impact.y, impact.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.55F, 1.45F);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    impact.x, impact.y + target.getBbHeight() * 0.45D, impact.z,
                    18, 0.16D, 0.16D, 0.16D, 0.06D);
            return;
        }

        applyBoltImpulse(target, direction, speed);
        attachBoltToTarget(target, impact, direction);
    }

    private void continueAfterLethalHit(LivingEntity target, ProjectilePhysicsProfile physics,
                                        double speed, Vec3 direction) {
        double remainingSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(this, target, speed);
        if (remainingSpeed <= 0.0D || hitTargetCount() >= getMaxEntityPierces()) {
            discard();
            return;
        }

        clearEmbeddedBlockPos();
        clearStructureAttachment();
        clearTargetAttachment();
        setNoGravity(false);
        inGround = false;
        setDeltaMovement(direction.scale(remainingSpeed));
        setBaseDamage(Math.max(1.0D, getBaseDamage() * (remainingSpeed / Math.max(0.001D, speed))));
        setPos(getX() + direction.x * 0.6D, getY() + direction.y * 0.6D, getZ() + direction.z * 0.6D);
        hasImpulse = true;
    }

    protected int getMaxEntityPierces() {
        return 4;
    }

    protected double getBoltKnockback(double speed) {
        return Math.min(1.65D, 0.35D + speed * 0.18D);
    }

    protected int getEmbeddedLifetimeTicks() {
        return EMBEDDED_LIFETIME_TICKS;
    }

    protected int getPinDurationTicks() {
        return PIN_DURATION_TICKS;
    }

    public void setRenderAnglesFromDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }

        Vec3 normalizedDirection = direction.normalize();
        double horizontal = Math.sqrt(normalizedDirection.x * normalizedDirection.x + normalizedDirection.z * normalizedDirection.z);
        float yaw = (float) (Mth.atan2(normalizedDirection.x, normalizedDirection.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(normalizedDirection.y, horizontal) * Mth.RAD_TO_DEG);
        this.entityData.set(RENDER_YAW, yaw);
        this.entityData.set(RENDER_PITCH, pitch);
        setYRot(yaw);
        setXRot(pitch);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // On its first tick a bolt has no previous rotation, so start it already aligned with its velocity.
        if (level().isClientSide && tickCount == 0 && (RENDER_YAW.equals(key) || RENDER_PITCH.equals(key))) {
            renderYawO = getRenderYaw();
            renderPitchO = getRenderPitch();
        }
    }

    public void alignRenderToDirection(Vec3 direction) {
        setRenderAnglesFromDirection(direction);
        renderYawO = getRenderYaw();
        renderPitchO = getRenderPitch();
        yRotO = getYRot();
        xRotO = getXRot();
    }

    public boolean isEmbedded() {
        return inGround || this.entityData.get(EMBEDDED) || hasStructureAttachment() || hasTargetAttachment();
    }

    public AttachmentRenderState getAttachmentRenderState(float partialTick) {
        Vec3 desiredPosition;
        Vec3 desiredDirection;

        Entity targetAttachment = level().getEntity(entityData.get(ATTACHED_TARGET_ID));
        if (targetAttachment instanceof LivingEntity target && target.isAlive()) {
            float yaw = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot);
            Vec3 localPosition = new Vec3(
                    entityData.get(TARGET_LOCAL_X),
                    entityData.get(TARGET_LOCAL_Y),
                    entityData.get(TARGET_LOCAL_Z));
            Vec3 localDirection = new Vec3(
                    entityData.get(TARGET_DIRECTION_X),
                    entityData.get(TARGET_DIRECTION_Y),
                    entityData.get(TARGET_DIRECTION_Z));
            Vec3 targetPosition = new Vec3(
                    Mth.lerp(partialTick, target.xo, target.getX()),
                    Mth.lerp(partialTick, target.yo, target.getY()),
                    Mth.lerp(partialTick, target.zo, target.getZ()));
            desiredPosition = targetPosition.add(rotateAroundY(localPosition, -yaw));
            desiredDirection = rotateAroundY(localDirection, -yaw).normalize();
        } else {
            Entity structureAttachment = level().getEntity(entityData.get(ATTACHED_STRUCTURE_ID));
            if (!(structureAttachment instanceof AbstractSiegeEntity structure)) {
                return null;
            }
            CollisionGroup group = structure.collisionGroups().stream()
                    .filter(candidate -> candidate.name().equals(entityData.get(ATTACHED_GROUP)))
                    .findFirst()
                    .orElse(null);
            int partIndex = entityData.get(ATTACHED_PART);
            if (group == null || partIndex < 0 || partIndex >= group.parts().size()) {
                return null;
            }

            CollisionPart part = group.parts().get(partIndex);
            Vec3 localPosition = new Vec3(
                    entityData.get(ATTACHED_LOCAL_X),
                    entityData.get(ATTACHED_LOCAL_Y),
                    entityData.get(ATTACHED_LOCAL_Z));
            Vec3 localDirection = new Vec3(
                    entityData.get(ATTACHED_DIRECTION_X),
                    entityData.get(ATTACHED_DIRECTION_Y),
                    entityData.get(ATTACHED_DIRECTION_Z));
            Vec3 structurePosition = group.fromPart(part, localPosition);
            Vec3 structureDirection = group.structureToPartRotation(part).transformInverse(localDirection);
            Vec3 previousPosition = structure.previousCollisionTransform().toWorld(structurePosition);
            Vec3 currentPosition = structure.collisionTransform().toWorld(structurePosition);
            Vec3 previousDirection = structure.previousCollisionTransform().directionToWorld(structureDirection);
            Vec3 currentDirection = structure.collisionTransform().directionToWorld(structureDirection);
            desiredPosition = previousPosition.lerp(currentPosition, partialTick);
            desiredDirection = previousDirection.lerp(currentDirection, partialTick).normalize();
        }

        Vec3 renderedPosition = new Vec3(
                Mth.lerp(partialTick, xo, getX()),
                Mth.lerp(partialTick, yo, getY()),
                Mth.lerp(partialTick, zo, getZ()));
        return new AttachmentRenderState(
                desiredPosition.subtract(renderedPosition),
                yawFromDirection(desiredDirection),
                pitchFromDirection(desiredDirection));
    }

    private static float yawFromDirection(Vec3 direction) {
        return (float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
    }

    private static float pitchFromDirection(Vec3 direction) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        return (float) (Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);
    }

    public float getRenderYaw() {
        return this.entityData.get(RENDER_YAW);
    }

    public float getRenderPitch() {
        return this.entityData.get(RENDER_PITCH);
    }

    public float getRenderYawO() {
        return renderYawO;
    }

    public float getRenderPitchO() {
        return renderPitchO;
    }

    protected void spawnFlightParticles(ServerLevel serverLevel) {
        if (tickCount % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    getX(), getY(), getZ(),
                    1, 0.01D, 0.01D, 0.01D, 0.01D);
        }
    }

    private void embedBolt(Vec3 impact, Vec3 direction, BlockPos embeddedBlockPos) {
        Vec3 normalizedDirection = direction.normalize();
        Vec3 embeddedPos = impact.subtract(normalizedDirection.scale(0.22D));
        supportLossTicks = 0;
        clearStructureAttachment();
        clearTargetAttachment();
        setPos(embeddedPos.x, embeddedPos.y, embeddedPos.z);
        setDeltaMovement(Vec3.ZERO);
        alignRenderToDirection(normalizedDirection);
        setEmbeddedBlockPos(embeddedBlockPos);
        setNoGravity(true);
        inGround = true;
        hasImpulse = true;
    }

    private boolean releaseIfSupportMissing() {
        BlockPos embeddedPos = getEmbeddedBlockPos();
        if (embeddedPos == null) {
            releaseFromMissingSupport();
            return true;
        }

        BlockState state = level().getBlockState(embeddedPos);
        if (!shouldIgnoreBlock(level(), embeddedPos, state)) {
            return false;
        }

        releaseFromMissingSupport();
        return true;
    }

    private void releaseFromMissingSupport() {
        pinning.clear();
        clearEmbeddedBlockPos();
        clearStructureAttachment();
        clearTargetAttachment();
        inGround = false;
        setNoGravity(false);
        supportLossTicks = 8;
        lastFlightMovement = Vec3.ZERO;
        setDeltaMovement(0.0D, SUPPORT_RELEASE_FALL_SPEED, 0.0D);
        alignRenderToDirection(new Vec3(0.0D, -1.0D, 0.0D));
        hasImpulse = true;
    }

    private void setEmbeddedBlockPos(BlockPos pos) {
        if (pos == null) {
            clearEmbeddedBlockPos();
            return;
        }

        this.entityData.set(EMBEDDED_X, pos.getX());
        this.entityData.set(EMBEDDED_Y, pos.getY());
        this.entityData.set(EMBEDDED_Z, pos.getZ());
        this.entityData.set(EMBEDDED, true);
    }

    private BlockPos getEmbeddedBlockPos() {
        if (!this.entityData.get(EMBEDDED)) {
            return null;
        }
        return new BlockPos(this.entityData.get(EMBEDDED_X), this.entityData.get(EMBEDDED_Y), this.entityData.get(EMBEDDED_Z));
    }

    private void clearEmbeddedBlockPos() {
        this.entityData.set(EMBEDDED, false);
    }

    private void embedBoltInStructure(AbstractSiegeEntity structure, Vec3 impact, Vec3 direction) {
        Vec3 normalizedDirection = direction.lengthSqr() > 1.0E-6D
                ? direction.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        AttachmentFrame frame = findAttachmentFrame(structure, impact, normalizedDirection);
        if (frame == null) {
            discard();
            return;
        }

        pinning.clear();
        clearEmbeddedBlockPos();
        clearTargetAttachment();
        attachedStructureUuid = structure.getUUID();
        entityData.set(ATTACHED_STRUCTURE_ID, structure.getId());
        entityData.set(ATTACHED_GROUP, frame.groupName());
        entityData.set(ATTACHED_PART, frame.partIndex());
        entityData.set(ATTACHED_LOCAL_X, (float) frame.localPosition().x);
        entityData.set(ATTACHED_LOCAL_Y, (float) frame.localPosition().y);
        entityData.set(ATTACHED_LOCAL_Z, (float) frame.localPosition().z);
        entityData.set(ATTACHED_DIRECTION_X, (float) frame.localDirection().x);
        entityData.set(ATTACHED_DIRECTION_Y, (float) frame.localDirection().y);
        entityData.set(ATTACHED_DIRECTION_Z, (float) frame.localDirection().z);
        supportLossTicks = 0;
        inGround = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        updateStructureAttachment();
        hasImpulse = true;
    }

    private AttachmentFrame findAttachmentFrame(AbstractSiegeEntity structure, Vec3 impact, Vec3 direction) {
        Vec3 structureImpact = structure.collisionTransform().toLocal(impact);
        Vec3 embeddedWorld = impact.subtract(direction.scale(0.22D));
        Vec3 embeddedStructure = structure.collisionTransform().toLocal(embeddedWorld);
        Vec3 structureDirection = structure.collisionTransform().directionToLocal(direction);
        CollisionGroup closestGroup = null;
        CollisionPart closestPart = null;
        int closestPartIndex = -1;
        double closestDistanceSqr = Double.MAX_VALUE;

        for (CollisionGroup group : structure.collisionGroups()) {
            for (int partIndex = 0; partIndex < group.parts().size(); partIndex++) {
                CollisionPart part = group.parts().get(partIndex);
                double distanceSqr = part.box().distanceToSqr(group.toPart(part, structureImpact));
                if (distanceSqr < closestDistanceSqr) {
                    closestDistanceSqr = distanceSqr;
                    closestGroup = group;
                    closestPart = part;
                    closestPartIndex = partIndex;
                }
            }
        }
        if (closestGroup == null || closestPart == null) {
            return null;
        }

        return new AttachmentFrame(
                closestGroup.name(),
                closestPartIndex,
                closestGroup.toPart(closestPart, embeddedStructure),
                closestGroup.structureToPartRotation(closestPart).transform(structureDirection).normalize());
    }

    private boolean hasStructureAttachment() {
        return attachedStructureUuid != null || entityData.get(ATTACHED_STRUCTURE_ID) >= 0;
    }

    private boolean updateStructureAttachment() {
        Entity attached = level().getEntity(entityData.get(ATTACHED_STRUCTURE_ID));
        if (!(attached instanceof AbstractSiegeEntity) && level() instanceof ServerLevel serverLevel
                && attachedStructureUuid != null) {
            attached = serverLevel.getEntity(attachedStructureUuid);
            if (attached instanceof AbstractSiegeEntity resolved) {
                entityData.set(ATTACHED_STRUCTURE_ID, resolved.getId());
            }
        }
        if (!(attached instanceof AbstractSiegeEntity structure) || !structure.isAlive()) {
            if (!level().isClientSide) {
                releaseFromMissingSupport();
                return false;
            }
            return true;
        }

        CollisionGroup group = structure.collisionGroups().stream()
                .filter(candidate -> candidate.name().equals(entityData.get(ATTACHED_GROUP)))
                .findFirst()
                .orElse(null);
        int partIndex = entityData.get(ATTACHED_PART);
        if (group == null || partIndex < 0 || partIndex >= group.parts().size()) {
            if (!level().isClientSide) {
                releaseFromMissingSupport();
                return false;
            }
            return true;
        }

        CollisionPart part = group.parts().get(partIndex);
        Vec3 localPosition = new Vec3(
                entityData.get(ATTACHED_LOCAL_X),
                entityData.get(ATTACHED_LOCAL_Y),
                entityData.get(ATTACHED_LOCAL_Z));
        Vec3 localDirection = new Vec3(
                entityData.get(ATTACHED_DIRECTION_X),
                entityData.get(ATTACHED_DIRECTION_Y),
                entityData.get(ATTACHED_DIRECTION_Z));
        Vec3 structurePosition = group.fromPart(part, localPosition);
        Vec3 structureDirection = group.structureToPartRotation(part).transformInverse(localDirection);
        Vec3 worldPosition = structure.collisionTransform().toWorld(structurePosition);
        Vec3 worldDirection = structure.collisionTransform().directionToWorld(structureDirection).normalize();
        setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        alignRenderToDirection(worldDirection);
        return true;
    }

    private void clearStructureAttachment() {
        attachedStructureUuid = null;
        entityData.set(ATTACHED_STRUCTURE_ID, -1);
        entityData.set(ATTACHED_GROUP, "");
        entityData.set(ATTACHED_PART, -1);
    }

    private void attachBoltToTarget(LivingEntity target, Vec3 impact, Vec3 direction) {
        Vec3 normalizedDirection = direction.lengthSqr() > 1.0E-6D
                ? direction.normalize()
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 embeddedWorld = impact.subtract(normalizedDirection.scale(0.22D));
        float yaw = target.yBodyRot;
        Vec3 localPosition = rotateAroundY(embeddedWorld.subtract(target.position()), yaw);
        Vec3 localDirection = rotateAroundY(normalizedDirection, yaw);

        clearEmbeddedBlockPos();
        clearStructureAttachment();
        attachedTargetUuid = target.getUUID();
        entityData.set(ATTACHED_TARGET_ID, target.getId());
        entityData.set(TARGET_LOCAL_X, (float) localPosition.x);
        entityData.set(TARGET_LOCAL_Y, (float) localPosition.y);
        entityData.set(TARGET_LOCAL_Z, (float) localPosition.z);
        entityData.set(TARGET_DIRECTION_X, (float) localDirection.x);
        entityData.set(TARGET_DIRECTION_Y, (float) localDirection.y);
        entityData.set(TARGET_DIRECTION_Z, (float) localDirection.z);
        supportLossTicks = 0;
        inGround = true;
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        updateTargetAttachment();
        hasImpulse = true;
    }

    public boolean hasTargetAttachment() {
        return attachedTargetUuid != null || entityData.get(ATTACHED_TARGET_ID) >= 0;
    }

    public boolean isAttachedTo(LivingEntity target) {
        return target != null && entityData.get(ATTACHED_TARGET_ID) == target.getId();
    }

    public Vec3 getTargetLocalPosition() {
        return new Vec3(
                entityData.get(TARGET_LOCAL_X),
                entityData.get(TARGET_LOCAL_Y),
                entityData.get(TARGET_LOCAL_Z));
    }

    public Vec3 getTargetLocalDirection() {
        return new Vec3(
                entityData.get(TARGET_DIRECTION_X),
                entityData.get(TARGET_DIRECTION_Y),
                entityData.get(TARGET_DIRECTION_Z));
    }

    private boolean updateTargetAttachment() {
        Entity attached = level().getEntity(entityData.get(ATTACHED_TARGET_ID));
        if (!(attached instanceof LivingEntity) && level() instanceof ServerLevel serverLevel
                && attachedTargetUuid != null) {
            attached = serverLevel.getEntity(attachedTargetUuid);
            if (attached instanceof LivingEntity resolved) {
                entityData.set(ATTACHED_TARGET_ID, resolved.getId());
            }
        }
        if (!(attached instanceof LivingEntity target) || !target.isAlive()) {
            if (!level().isClientSide) {
                pinning.clear();
                clearTargetAttachment();
                discard();
                return false;
            }
            return true;
        }

        Vec3 localPosition = new Vec3(
                entityData.get(TARGET_LOCAL_X),
                entityData.get(TARGET_LOCAL_Y),
                entityData.get(TARGET_LOCAL_Z));
        Vec3 localDirection = new Vec3(
                entityData.get(TARGET_DIRECTION_X),
                entityData.get(TARGET_DIRECTION_Y),
                entityData.get(TARGET_DIRECTION_Z));
        Vec3 worldPosition = target.position().add(rotateAroundY(localPosition, -target.yBodyRot));
        Vec3 worldDirection = rotateAroundY(localDirection, -target.yBodyRot).normalize();
        setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        alignRenderToDirection(worldDirection);
        return true;
    }

    private void clearTargetAttachment() {
        attachedTargetUuid = null;
        entityData.set(ATTACHED_TARGET_ID, -1);
    }

    private static Vec3 rotateAroundY(Vec3 vector, float degrees) {
        double radians = degrees * Mth.DEG_TO_RAD;
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vec3(
                vector.x * cos + vector.z * sin,
                vector.y,
                vector.z * cos - vector.x * sin);
    }

    private void applyBoltImpulse(LivingEntity target, Vec3 direction, double speed) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = direction;
        }

        Vec3 impulse = horizontal.normalize().scale(getBoltKnockback(speed)).add(0.0D, 0.08D, 0.0D);
        target.setDeltaMovement(target.getDeltaMovement().add(impulse));
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    protected double getPinSearchDistance() {
        return PIN_SEARCH_DISTANCE;
    }

    private record AttachmentFrame(String groupName, int partIndex,
                                   Vec3 localPosition, Vec3 localDirection) {
    }

    public record AttachmentRenderState(Vec3 offset, float yaw, float pitch) {
    }

    private void sweepMissedBlockHit(ServerLevel serverLevel, Vec3 previousPos, Vec3 currentPos, Vec3 previousMovement) {
        ProjectileSweep.hitFirstBlockingBlock(serverLevel, previousPos, currentPos, previousMovement,
                this::shouldIgnoreBlock, this::onHitBlock);
    }

    private boolean shouldIgnoreBlock(Level world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty() && state.getCollisionShape(world, pos).isEmpty()) {
            return true;
        }
        return state.getCollisionShape(world, pos).isEmpty();
    }

}
