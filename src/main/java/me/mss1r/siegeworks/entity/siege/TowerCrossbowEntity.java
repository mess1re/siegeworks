package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TowerCrossbowBoltProjectile;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TowerCrossbowEntity extends AbstractBoltThrowerEntity {
    private static final Vec3 WEAPON_PIVOT_PIXELS = new Vec3(0.0D, 17.0D, 0.0D);
    private static final Vec3 BOLT_TIP_PIXELS = new Vec3(0.0D, 23.2D, -30.8D);
    private static final Vec3 SUPPORT_BONE_PIVOT_PIXELS = new Vec3(0.0D, 23.99619D, 33.08716D);
    private static final float SUPPORT_MODEL_ROTATION_X = 5.0F;
    private static final double SUPPORT_FOOT_Y_PIXELS = 2.99619D;
    private static final double SUPPORT_FOOT_MIN_Z_PIXELS = 33.08716D;
    private static final double SUPPORT_FOOT_MAX_Z_PIXELS = 34.08716D;
    private static final double SUPPORT_FOOT_MIN_X_PIXELS = -4.0D;
    private static final double SUPPORT_FOOT_MAX_X_PIXELS = 4.0D;
    private static final double SUPPORT_FOOT_SAMPLE_STEP_PIXELS = 1.0D;
    private static final double SUPPORT_COLLISION_PROBE_RADIUS = 0.02D;
    private static final float MIN_AIM_PITCH = -25.0F;
    private static final float MAX_AIM_PITCH = 12.0F;
    private static final float EXTRA_DEPRESSION_PER_BLOCK = 15.0F;
    private static final float MAX_OPERATOR_ELEVATION = 1.0F;
    private static final double OPERATOR_REAR_OFFSET = 49.0D / 16.0D;

    private static final EntityDataAccessor<Float> OPERATOR_ELEVATION =
            SynchedEntityData.defineId(TowerCrossbowEntity.class, EntityDataSerializers.FLOAT);

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withReloadSound(SiegeworksSounds.TOWER_CROSSBOW_RELOAD.get())
            .withFiringSound(SiegeworksSounds.TOWER_CROSSBOW_SHOOT.get())
            .withReloadRange(20.0)
            .withFiringRange(24.0D)
            .withFiringVolume(0.5F);

    private static final LoadingRequirement[] LOAD_STAGES = {
            LoadingRequirement.consume(SiegeworksItems.TOWER_CROSSBOW_BOLT.get()).timedBy("bolt")
    };

    public TowerCrossbowEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
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
        data.define(OPERATOR_ELEVATION, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265.0D);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    public float getMinAimPitch() {
        return MIN_AIM_PITCH;
    }

    @Override
    public float getMaxAimPitch() {
        return MAX_AIM_PITCH + getOperatorElevation() * EXTRA_DEPRESSION_PER_BLOCK;
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return 2.5F;
    }

    @Override
    protected float getPitchTurnSpeedDegrees() {
        return 2.0F;
    }

    @Override
    protected float getParkedAimPitch() {
        return getMinAimPitch();
    }

    @Override
    protected LoadingRequirement[] getLoadStages() {
        return LOAD_STAGES;
    }

    @Override
    protected Item getBoltItem() {
        return SiegeworksItems.TOWER_CROSSBOW_BOLT.get();
    }

    @Override
    protected EntityType<TowerCrossbowBoltProjectile> getBoltType() {
        return SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get();
    }

    @Override
    protected TowerCrossbowBoltProjectile createBolt(ServerLevel serverLevel) {
        return new TowerCrossbowBoltProjectile(getBoltType(), this, serverLevel);
    }

    @Override
    protected String getFireHintTranslationKey() {
        return "siege.tower_crossbow.fire_hint";
    }

    @Override
    protected Vec3 getWeaponPivotPixels() {
        return WEAPON_PIVOT_PIXELS;
    }

    @Override
    protected Vec3 getBoltPivotPixels() {
        return BOLT_TIP_PIXELS;
    }

    @Override
    protected boolean isAimGeometryClear(float pitch) {
        return supportFootEdgeIsClear(SUPPORT_FOOT_MIN_Z_PIXELS, pitch)
                && supportFootEdgeIsClear(SUPPORT_FOOT_MAX_Z_PIXELS, pitch);
    }

    private boolean supportFootEdgeIsClear(double z, float pitch) {
        for (double x = SUPPORT_FOOT_MIN_X_PIXELS;
             x <= SUPPORT_FOOT_MAX_X_PIXELS;
             x += SUPPORT_FOOT_SAMPLE_STEP_PIXELS) {
            Vec3 supportPoint = rotateAroundLocalX(
                    new Vec3(x, SUPPORT_FOOT_Y_PIXELS, z),
                    SUPPORT_BONE_PIVOT_PIXELS,
                    SUPPORT_MODEL_ROTATION_X
            );
            if (modelSampleCollidesAtLocalRotation(
                    supportPoint, toModelPitch(pitch), SUPPORT_COLLISION_PROBE_RADIUS)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected double getReloadAnimationLengthTicks() {
        return 120.0D;
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(0.0D, getOperatorElevation(), OPERATOR_REAR_OFFSET);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        if (!level().isClientSide && getPassengers().isEmpty()) {
            entityData.set(OPERATOR_ELEVATION, operatorElevationFromFooting(passenger));
        }
        super.addPassenger(passenger);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (!level().isClientSide && passenger == getFirstPassenger()) {
            float elevation = operatorElevationFromFooting(passenger);
            if (Math.abs(elevation - getOperatorElevation()) > 0.001F) {
                entityData.set(OPERATOR_ELEVATION, elevation);
                setTrackedPitch(getTrackedPitch());
            }
        }
        super.positionRider(passenger, moveFunction);
    }

    private float operatorElevationFromFooting(Entity passenger) {
        for (float elevation = MAX_OPERATOR_ELEVATION; elevation >= 0.5F; elevation -= 0.5F) {
            if (hasElevatedOperatorFooting(passenger, elevation)) {
                return elevation;
            }
        }
        return 0.0F;
    }

    private boolean hasElevatedOperatorFooting(Entity passenger, float elevation) {
        Vec3 seatOffset = SiegePassengerPhysics.rotatedSeatOffset(this, passenger);
        double seatX = getX() + seatOffset.x;
        double seatZ = getZ() + seatOffset.z;
        double feetY = getY() + elevation;
        double halfWidth = passenger.getBbWidth() * 0.5D + 0.02D;
        AABB supportProbe = new AABB(
                seatX - halfWidth, feetY - 0.08D, seatZ - halfWidth,
                seatX + halfWidth, feetY + 0.02D, seatZ + halfWidth);
        return level().getBlockCollisions(passenger, supportProbe).iterator().hasNext();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean operator = passenger == getFirstPassenger();
        super.removePassenger(passenger);
        if (operator && !level().isClientSide) {
            entityData.set(OPERATOR_ELEVATION, 0.0F);
            setTrackedPitch(getTrackedPitch());
        }
    }

    public float getOperatorElevation() {
        return entityData.get(OPERATOR_ELEVATION);
    }

    @Override
    protected double getAimCameraForwardDistance() {
        return -4.72D;
    }

    @Override
    protected double getAimCameraLift() {
        return 0.18D;
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(toModelPitch(getTrackedPitch()), currentCollisionRelativeYaw());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionModelPitch(), previousCollisionRelativeYaw());
    }

    private static List<CollisionGroup> collisionGroups(float modelPitch, float relativeYaw) {
        CollisionPose pitch = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.TOWER_CROSSBOW_ENGINE.pivot(),
                (float) Math.toRadians(modelPitch));
        CollisionPose yaw = CollisionPose.fromGeckoBoneY(
                GeneratedCollisionShapes.TOWER_CROSSBOW_ENGINE.pivot(),
                (float) Math.toRadians(relativeYaw));
        return List.of(
                new CollisionGroup("rack", GeneratedCollisionShapes.TOWER_CROSSBOW_RACK,
                        CollisionPose.IDENTITY),
                new CollisionGroup("engine", GeneratedCollisionShapes.TOWER_CROSSBOW_ENGINE,
                        pitch.then(yaw)));
    }
}
