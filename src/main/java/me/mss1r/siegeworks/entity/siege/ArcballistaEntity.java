package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.ArcballistaBoltProjectile;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ArcballistaEntity extends AbstractBoltThrowerEntity {
    private static final Vec3 ARCBALLISTA_PIVOT_PIXELS = new Vec3(0.0D, 10.0D, 1.0D);
    private static final Vec3 ARCBALLISTA_BOLT_PIVOT_PIXELS = new Vec3(0.0D, 14.5D, -20.0D);
    private static final float FREE_MIN_AIM_PITCH = -89.0F;
    private static final float FREE_MAX_AIM_PITCH = 89.0F;
    private static final float OPERATED_MIN_AIM_PITCH = -35.0F;
    private static final float OPERATED_MAX_AIM_PITCH = 25.0F;
    private static final double BEAM_HALF_WIDTH_PIXELS = 1.9D;
    private static final double BEAM_BOTTOM_Y_PIXELS = 11.0D;
    private static final double BEAM_MIDDLE_Y_PIXELS = 12.5D;
    private static final double BEAM_TOP_Y_PIXELS = 13.9D;
    private static final double BEAM_FRONT_Z_PIXELS = -23.0D;
    private static final double BEAM_REAR_Z_PIXELS = 45.0D;
    private static final double BEAM_SAMPLE_STEP_PIXELS = 2.0D;
    private static final double BEAM_COLLISION_PROBE_RADIUS = 0.01D;
    private static final double OPERATOR_SIDE_OFFSET = 7.0D / 16.0D;
    private static final double OPERATOR_REAR_OFFSET = 30.0D / 16.0D;
    private static final double TOW_DISTANCE = 23.0D / 16.0D + 2.75D;
    private static final float TRAVEL_PITCH = 0.0F;

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withReloadSound(SiegeworksSounds.ARCBALLISTA_RELOAD.get())
            .withFiringSound(SiegeworksSounds.ARCBALLISTA_SHOOT.get())
            .withMovementRange(25.0)
            .withReloadRange(18.0)
            .withFiringRange(60.0D)
            .withFiringVolume(0.7F);

    private static final LoadingRequirement[] LOAD_STAGES = {
            LoadingRequirement.consume(SiegeworksItems.ARCBALLISTA_BOLT.get()).timedBy("bolt")
    };

    public ArcballistaEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        setTrackedPitch(0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 65.0)
                .add(Attributes.MOVEMENT_SPEED, 0.065)
                .add(Attributes.KNOCKBACK_RESISTANCE, 220.0);
    }

    @Override
    public boolean hasDifferentialDrive() {
        return true;
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    protected int getMoveSoundIntervalTicks() {
        return 28;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return super.canAddOperator(entity);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!AbstractSiegeEntity.isDraftMount(passenger)) {
            setTrackedPitch(0.0F);
            setXRot(0.0F);
            lastRiderPitch = 0.0F;
            if (passenger instanceof Player player) {
                player.setXRot(0.0F);
            }
        }
    }

    @Override
    public float getMinAimPitch() {
        return hasAimingOperator() ? OPERATED_MIN_AIM_PITCH : FREE_MIN_AIM_PITCH;
    }

    @Override
    public float getMaxAimPitch() {
        return hasAimingOperator() ? OPERATED_MAX_AIM_PITCH : FREE_MAX_AIM_PITCH;
    }

    private boolean hasAimingOperator() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity && !AbstractSiegeEntity.isDraftMount(passenger);
    }

    @Override
    protected boolean usesIndependentAimYaw() {
        return false;
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return AbstractSiegeEntity.isDraftMount(getFirstPassenger()) ? 3.0F : 12.0F;
    }

    @Override
    protected float getPitchTurnSpeedDegrees() {
        return 2.5F;
    }

    @Override
    public boolean shouldPredictYawOnClient() {
        return true;
    }

    @Override
    protected float getParkedAimPitch() {
        return isTowed() ? TRAVEL_PITCH : getMinAimPitch();
    }

    @Override
    protected boolean shouldReturnToParkedPitch() {
        return getPassengers().isEmpty() || isTowed();
    }

    @Override
    public void turnTowardsYaw(float targetYaw) {
        super.turnTowardsYaw(targetYaw);
    }

    @Override
    protected LoadingRequirement[] getLoadStages() {
        return LOAD_STAGES;
    }

    @Override
    protected Item getBoltItem() {
        return SiegeworksItems.ARCBALLISTA_BOLT.get();
    }

    @Override
    protected String getFireHintTranslationKey() {
        return "siege.arcballista.fire_hint";
    }

    @Override
    protected EntityType<ArcballistaBoltProjectile> getBoltType() {
        return SiegeworksEntities.ARCBALLISTA_BOLT_PROJECTILE.get();
    }

    @Override
    protected ArcballistaBoltProjectile createBolt(ServerLevel serverLevel) {
        return new ArcballistaBoltProjectile(getBoltType(), this, serverLevel);
    }

    @Override
    protected void spawnLaunchParticles(ServerLevel serverLevel, Vec3 mouthPos, Vec3 direction) {
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                mouthPos.x, mouthPos.y, mouthPos.z, 8,
                direction.x * 0.12D, direction.y * 0.04D, direction.z * 0.12D, 0.18D);
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(TOW_DISTANCE);
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(OPERATOR_SIDE_OFFSET, 0.0D, OPERATOR_REAR_OFFSET);
    }

    @Override
    protected Vec3 getWeaponPivotPixels() {
        return ARCBALLISTA_PIVOT_PIXELS;
    }

    @Override
    protected Vec3 getBoltPivotPixels() {
        return ARCBALLISTA_BOLT_PIVOT_PIXELS;
    }

    @Override
    protected double getAimCameraForwardDistance() {
        return -0.25D;
    }

    @Override
    protected double getAimCameraLift() {
        return 0.12D;
    }

    @Override
    protected double getReloadAnimationLengthTicks() {
        return 70.0D;
    }

    @Override
    protected boolean isAimGeometryClear(float pitch) {
        for (double z = BEAM_FRONT_Z_PIXELS; z <= BEAM_REAR_Z_PIXELS; z += BEAM_SAMPLE_STEP_PIXELS) {
            if (beamSampleCollides(-BEAM_HALF_WIDTH_PIXELS, BEAM_BOTTOM_Y_PIXELS, z, pitch)
                    || beamSampleCollides(0.0D, BEAM_BOTTOM_Y_PIXELS, z, pitch)
                    || beamSampleCollides(BEAM_HALF_WIDTH_PIXELS, BEAM_BOTTOM_Y_PIXELS, z, pitch)
                    || beamSampleCollides(-BEAM_HALF_WIDTH_PIXELS, BEAM_MIDDLE_Y_PIXELS, z, pitch)
                    || beamSampleCollides(BEAM_HALF_WIDTH_PIXELS, BEAM_MIDDLE_Y_PIXELS, z, pitch)
                    || beamSampleCollides(-BEAM_HALF_WIDTH_PIXELS, BEAM_TOP_Y_PIXELS, z, pitch)
                    || beamSampleCollides(0.0D, BEAM_TOP_Y_PIXELS, z, pitch)
                    || beamSampleCollides(BEAM_HALF_WIDTH_PIXELS, BEAM_TOP_Y_PIXELS, z, pitch)) {
                return false;
            }
        }
        return true;
    }

    private boolean beamSampleCollides(double x, double y, double z, float pitch) {
        return modelSampleCollidesAtLocalRotation(
                new Vec3(x, y, z), toModelPitch(pitch), BEAM_COLLISION_PROBE_RADIUS);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(toModelPitch(getTrackedPitch()));
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionModelPitch());
    }

    private static List<CollisionGroup> collisionGroups(float modelPitch) {
        CollisionPose aim = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.ARCBALLISTA_ENGINE.pivot(),
                (float) Math.toRadians(modelPitch));
        return List.of(new CollisionGroup(
                "engine", GeneratedCollisionShapes.ARCBALLISTA_ENGINE, aim));
    }
}
