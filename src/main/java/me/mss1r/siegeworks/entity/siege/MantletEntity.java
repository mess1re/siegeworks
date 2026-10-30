package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.api.ProjectilePassThroughControl;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.movement.SyncedFloatInterpolator;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
//? if <1.21 {
/*import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
*///?} else {
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//?}
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimatableManager;
*///?} else {
import software.bernie.geckolib.animation.AnimatableManager;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class MantletEntity extends AbstractSiegeEntity implements GeoEntity, ProjectilePassThroughControl {
    public static final int ACTION_OPEN_FLAP = 0;
    public static final int ACTION_CLOSE_FLAP = 1;
    public static final int ACTION_DISMOUNT = 2;

    private static final String TAG_BODY_PITCH = "BodyPitch";
    private static final String TAG_FLAP_ANGLE = "FlapAngle";
    private Float orderedFlapAngle;
    private static final float MAX_BODY_PITCH = 75.0F;
    private static final float MAX_FLAP_ANGLE = 75.0F;
    private static final float FLAP_OPEN_STEP = MAX_FLAP_ANGLE / (4.0F * 20.0F);
    private static final float FLAP_CLOSE_STEP = MAX_FLAP_ANGLE / (3.0F * 20.0F);
    private static final float BODY_RETURN_STEP = 1.5F;
    private static final float BODY_FALL_ACCELERATION = 0.04F;
    private static final float BODY_FALL_MAX_SPEED = 1.2F;
    private static final double PROJECTILE_TRACE_HALF_LENGTH = 5.0D;
    private static final float OPERATOR_VIEW_LIMIT = 35.0F;

    private static final double TOW_DISTANCE = 6.5D;

    private static final List<Vec3> SUPPORT_CONTACT_POINTS = List.of(
            new Vec3(-22.0D / 16.0D, 13.0D / 16.0D, -62.0D / 16.0D),
            new Vec3(-19.0D / 16.0D, 13.0D / 16.0D, -62.0D / 16.0D),
            new Vec3(19.0D / 16.0D, 13.0D / 16.0D, -62.0D / 16.0D),
            new Vec3(22.0D / 16.0D, 13.0D / 16.0D, -62.0D / 16.0D));
    private static final double SUPPORT_PROBE_RADIUS = 0.025D;

    private static final EntityDataAccessor<Float> BODY_PITCH =
            SynchedEntityData.defineId(MantletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FLAP_ANGLE =
            SynchedEntityData.defineId(MantletEntity.class, EntityDataSerializers.FLOAT);

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withMovementInterval(150)
            .withMovementRange(30.0);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final SyncedFloatInterpolator clientBodyPitch = new SyncedFloatInterpolator();
    private final SyncedFloatInterpolator clientFlapAngle = new SyncedFloatInterpolator();
    private float bodyFallVelocity;
    private float previousBodyPitchRadians;
    private float previousFlapAngleRadians;

    public MantletEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    //? if <1.21 {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(BODY_PITCH, 0.0F);
        data.define(FLAP_ANGLE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.075)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    public boolean hasDifferentialDrive() {
        return true;
    }

    @Override
    protected boolean operatorControlsRotation(Entity passenger) {
        return isPlayerControlledDraftMount(passenger);
    }

    @Override
    protected boolean operatorControlsMovement(Entity passenger) {
        return isPlayerControlledDraftMount(passenger) || isSupportedDirectOperator(passenger);
    }

    @Override
    protected boolean operatorBodyFollowsEngine(Entity passenger) {
        return isPlayerControlledDraftMount(passenger) || isSupportedDirectOperator(passenger);
    }

    @Override
    public float getPassengerViewYawLimit(Entity passenger) {
        return OPERATOR_VIEW_LIMIT;
    }

    @Override
    public float getPassengerViewPitchLimit(Entity passenger) {
        return OPERATOR_VIEW_LIMIT;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (isDraftMount(passenger) || isSupportedDirectOperator(passenger)) {
            setBodyPitch(0.0F);
            bodyFallVelocity = 0.0F;
        }
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (getFirstPassenger() != null) {
            return InteractionResult.FAIL;
        }
        if (!player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && canAddPassenger(player)) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void requestAction(ServerPlayer player, int action) {
        if (player.getVehicle() != this || !isOperator(player)
                || !SiegeAccess.allows(player, this, SiegeAccess.Action.USE)) {
            return;
        }
        switch (action) {
            case ACTION_OPEN_FLAP -> {
                orderedFlapAngle = null;
                setFlapAngle(getFlapAngle() + FLAP_OPEN_STEP);
            }
            case ACTION_CLOSE_FLAP -> {
                orderedFlapAngle = null;
                setFlapAngle(getFlapAngle() - FLAP_CLOSE_STEP);
            }
            case ACTION_DISMOUNT -> player.stopRiding();
            default -> {
            }
        }
    }

    @Override
    public void tick() {
        previousBodyPitchRadians = bodyPitchRadians();
        tickOrderedFlap();
        previousFlapAngleRadians = flapAngleRadians();
        super.tick();
        if (level().isClientSide) {
            clientBodyPitch.tick(getBodyPitch());
            clientFlapAngle.tick(getFlapAngle());
        }
        StructureMotionSystem.tickStructure(this);
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        if (getFirstPassenger() != null) {
            bodyFallVelocity = 0.0F;
            setBodyPitch(Mth.approach(getBodyPitch(), 0.0F, BODY_RETURN_STEP));
            return;
        }
        approachSupportedRestingPitch();
    }

    private void approachSupportedRestingPitch() {
        float current = getBodyPitch();
        if (current >= MAX_BODY_PITCH) {
            bodyFallVelocity = 0.0F;
            return;
        }

        bodyFallVelocity = Math.min(BODY_FALL_MAX_SPEED, bodyFallVelocity + BODY_FALL_ACCELERATION);
        float target = Math.min(MAX_BODY_PITCH, current + bodyFallVelocity);
        if (supportsAreClear(target)) {
            setBodyPitch(target);
            return;
        }

        float clear = current;
        float blocked = target;
        for (int iteration = 0; iteration < 6; iteration++) {
            float middle = (clear + blocked) * 0.5F;
            if (supportsAreClear(middle)) {
                clear = middle;
            } else {
                blocked = middle;
            }
        }
        setBodyPitch(clear);
        bodyFallVelocity = 0.0F;
    }

    private boolean supportsAreClear(float bodyPitch) {
        CollisionPose bodyPose = bodyCollisionPose(bodyPitch * Mth.DEG_TO_RAD);
        for (Vec3 supportPoint : SUPPORT_CONTACT_POINTS) {
            Vec3 point = collisionTransform().toWorld(bodyPose.toStructure(supportPoint));
            AABB probe = new AABB(
                    point.x - SUPPORT_PROBE_RADIUS,
                    point.y - SUPPORT_PROBE_RADIUS,
                    point.z - SUPPORT_PROBE_RADIUS,
                    point.x + SUPPORT_PROBE_RADIUS,
                    point.y + SUPPORT_PROBE_RADIUS,
                    point.z + SUPPORT_PROBE_RADIUS
            );
            if (level().getBlockCollisions(this, probe).iterator().hasNext()) {
                return false;
            }
        }
        return true;
    }

    public boolean protectsLine(Vec3 sourcePos, LivingEntity target) {
        return isAlive()
                && target != this
                && target.distanceToSqr(this) <= 64.0D
                && blocksProtectionPath(sourcePos, target.getEyePosition());
    }

    @Override
    public boolean allowsProjectilePassage(Vec3 start, Vec3 end) {
        Vec3 movement = end.subtract(start);
        if (movement.lengthSqr() <= 1.0E-6D) {
            return !blocksProtectionPath(start, end);
        }

        Vec3 direction = movement.normalize();
        Vec3 traceStart = start.subtract(direction.scale(PROJECTILE_TRACE_HALF_LENGTH));
        Vec3 traceEnd = start.add(direction.scale(PROJECTILE_TRACE_HALF_LENGTH));
        return !blocksProtectionPath(traceStart, traceEnd);
    }

    private boolean blocksProtectionPath(Vec3 start, Vec3 end) {
        return StructureCollisionResolver.intersectsSegment(this, start, end);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(bodyPitchRadians(), flapAngleRadians());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousBodyPitchRadians, previousFlapAngleRadians);
    }

    private static List<CollisionGroup> collisionGroups(float bodyPitch, float flapAngle) {
        CollisionPose bodyPose = bodyCollisionPose(bodyPitch);
        CollisionPose flapPose = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.MANTLET_FLAP.pivot(), flapAngle).then(bodyPose);
        return List.of(
                new CollisionGroup("body", GeneratedCollisionShapes.MANTLET_BODY, bodyPose),
                new CollisionGroup("flap", GeneratedCollisionShapes.MANTLET_FLAP, flapPose));
    }

    private static CollisionPose bodyCollisionPose(float bodyPitch) {
        return CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.MANTLET_BODY.pivot(), bodyPitch);
    }

    private float bodyPitchRadians() {
        return getBodyPitch() * Mth.DEG_TO_RAD;
    }

    private float flapAngleRadians() {
        return getFlapAngle() * Mth.DEG_TO_RAD;
    }

    public float getBodyPitch() {
        return entityData.get(BODY_PITCH);
    }

    private void setBodyPitch(float pitch) {
        entityData.set(BODY_PITCH, Mth.clamp(pitch, 0.0F, MAX_BODY_PITCH));
    }

    public float getRenderedBodyPitch(float partialTick) {
        return level().isClientSide && clientBodyPitch.isInitialized()
                ? clientBodyPitch.sample(partialTick)
                : getBodyPitch();
    }

    public void orderFlap(boolean open) {
        orderedFlapAngle = open ? MAX_FLAP_ANGLE : 0.0F;
    }

    public boolean isFlapOpen() {
        float angle = orderedFlapAngle != null ? orderedFlapAngle : getFlapAngle();
        return angle > MAX_FLAP_ANGLE * 0.5F;
    }

    public boolean flapSettled() {
        return orderedFlapAngle == null || Math.abs(getFlapAngle() - orderedFlapAngle) < 1.0E-3F;
    }

    private void tickOrderedFlap() {
        if (orderedFlapAngle == null || level().isClientSide()) {
            return;
        }
        float current = getFlapAngle();
        if (Math.abs(current - orderedFlapAngle) < 1.0E-3F) {
            orderedFlapAngle = null;
            return;
        }
        setFlapAngle(current < orderedFlapAngle
                ? Math.min(orderedFlapAngle, current + FLAP_OPEN_STEP)
                : Math.max(orderedFlapAngle, current - FLAP_CLOSE_STEP));
    }

    public float getFlapAngle() {
        return entityData.get(FLAP_ANGLE);
    }

    private void setFlapAngle(float angle) {
        entityData.set(FLAP_ANGLE, Mth.clamp(angle, 0.0F, MAX_FLAP_ANGLE));
    }

    public float getRenderedFlapAngle(float partialTick) {
        return level().isClientSide && clientFlapAngle.isInitialized()
                ? clientFlapAngle.sample(partialTick)
                : getFlapAngle();
    }

    public float getProtectionReduction(boolean siegeImpact, boolean explosion) {
        return siegeImpact || explosion ? 0.45F : 0.08F;
    }

    public float getProtectionWear(float incomingDamage, boolean siegeImpact, boolean explosion) {
        if (explosion) {
            return Math.min(22.0F, Math.max(5.0F, incomingDamage * 0.32F));
        }
        return siegeImpact ? 8.0F : 0.65F;
    }

    public void playProtectionFeedback(ServerLevel serverLevel) {
        serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.35D, getZ(),
                10, 0.45D, 0.55D, 0.45D, 0.05D);
        serverLevel.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK,
                SoundSource.BLOCKS, 0.75F, 0.8F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
    }

    @Override
    public void triggerAnimation(String animationName) {
    }

    @Override
    public void stopAnimation(String animationName) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(TOW_DISTANCE);
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(0.0D, 0.0D, 66.0D / 16.0D);
    }

    @Override
    public Vec3 getPlayerPOV() {
        return new Vec3(0.0D, -0.7D, 0.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(TAG_BODY_PITCH, getBodyPitch());
        tag.putFloat(TAG_FLAP_ANGLE, getFlapAngle());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBodyPitch(tag.getFloat(TAG_BODY_PITCH));
        setFlapAngle(tag.getFloat(TAG_FLAP_ANGLE));
        clientBodyPitch.reset(getBodyPitch());
        clientFlapAngle.reset(getFlapAngle());
    }
}
