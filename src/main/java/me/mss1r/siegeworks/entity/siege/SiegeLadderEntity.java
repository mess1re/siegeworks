package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.api.SiegeClimbableControl;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.CollisionShape;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeRelation;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.movement.SyncedFloatInterpolator;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderAutomatedTraversal;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderClimberSupport;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
//? if forge {
/*import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
*///?} else {
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimatableManager;
*///?} else {
import software.bernie.geckolib.animation.AnimatableManager;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationController;
*///?} else {
import software.bernie.geckolib.animation.AnimationController;
//?}
//? if forge {
/*import software.bernie.geckolib.core.object.PlayState;
*///?} else {
import software.bernie.geckolib.animation.PlayState;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SiegeLadderEntity extends AbstractSiegeEntity implements GeoEntity, SiegeClimbableControl {
    /** A ladder can be just its base, as crafted. */
    public static final int MIN_SECTIONS = 0;
    public static final int MAX_SECTIONS = 4;

    private static final EntityDataAccessor<Integer> SECTIONS =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LEAN_PROGRESS =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEPLOY_TICKS =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);
    /** The entity id of whoever carries the ladder in their hands, or -1. */
    private static final EntityDataAccessor<Integer> CARRIER_ID =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);

    private static final String TAG_SECTIONS = "Sections";
    private static final String TAG_LEAN_PROGRESS = "LeanProgress";
    private static final String TAG_DEPLOY_TICKS = "DeployTicks";
    private static final String TAG_LEGACY_RELOCATION_OWNER = "RelocationOwner";

    private static final int DEPLOY_DELAY_TICKS = 35;
    /** Ticks a ladder stands upright after being put down before it tips forward. */
    private static final int PUT_DOWN_STAND_TICKS = 10;
    private static final double BLOCKBENCH_SECTION_LENGTH = 48.0D / 16.0D;
    private static final double LADDER_HALF_WIDTH = 7.5D / 16.0D;
    private static final double LADDER_HALF_THICKNESS = 1.5D / 16.0D;
    private static final double LADDER_WALK_EXTENSION = 4.0D / 16.0D;
    private static final double LADDER_SURFACE_SENSOR_EXTENSION = 0.0D;
    private static final double LADDER_PLATFORM_Y_OFFSET = 0.04D;
    private static final double LADDER_SURFACE_MIN_LOCAL_Y = 3.0D;
    private static final double LADDER_SURFACE_CONTACT_TOLERANCE = 1.0D / 16.0D;
    private static final float MIN_LEAN_PROGRESS = -1.0F;
    private static final float MAX_LEAN_PROGRESS = 1.0F;
    private static final float MAX_LEAN_DEGREES = 89.0F;
    private static final float FALL_ACCELERATION_BASE = 0.00045F;
    private static final float FALL_ACCELERATION_PROGRESS = 0.0031F;
    private static final float MAX_FORWARD_VELOCITY = 0.030F;
    private static final float MAX_BACKWARD_VELOCITY = -0.032F;
    private static final float SURFACE_BACKOFF = 0.001F;
    private static final float SURFACE_REST_PROBE = 0.003F;
    private static final int SURFACE_REST_PROBE_INTERVAL_TICKS = 20;
    private static final double FALL_DIRECTION = 1.0D;

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults();

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final LadderAutomatedTraversal automatedTraversal = new LadderAutomatedTraversal(
            new LadderAutomatedTraversal.Host() {
                @Override public SiegeLadderEntity ladder() { return SiegeLadderEntity.this; }
                @Override public Level level() { return SiegeLadderEntity.this.level(); }
                @Override public int gameTick() { return tickCount; }
                @Override public boolean ready() { return isReadyForAutomatedClimb(); }
                @Override public double ladderLength() { return getLadderLength(); }
                @Override public double leanAngleRadians() {
                    return SiegeLadderEntity.this.leanAngleRadians(getLeanProgress());
                }
                @Override public Vec3 ladderWorldPoint(double localY) {
                    return SiegeLadderEntity.this.ladderWorldPoint(0.0D, localY, 0.0D);
                }
                @Override public double platformY(double localY) {
                    return ladderPlatformYAtLocal(getLeanProgress(), localY);
                }
                @Override public Vec3 uphillVector() { return getUphillVector(); }
            });
    @Nullable
    private UUID carrierUuid;
    /** Server only: the carrying player. */
    @Nullable
    private Player carrier;
    private final LadderClimberSupport climberSupport = new LadderClimberSupport(
            new LadderClimberSupport.Host() {
                @Override public SiegeLadderEntity ladder() { return SiegeLadderEntity.this; }
                @Override public Level level() { return SiegeLadderEntity.this.level(); }
                @Override public int gameTick() { return tickCount; }
                @Override public float leanProgress() { return getLeanProgress(); }
                @Override public double leanAngleRadians() {
                    return SiegeLadderEntity.this.leanAngleRadians(getLeanProgress());
                }
                @Override public double ladderLength() { return getLadderLength(); }
                @Override public LadderClimberSupport.LocalPosition localPosition(Vec3 worldPosition) {
                    LadderLocalPosition local = ladderLocalPosition(worldPosition);
                    return new LadderClimberSupport.LocalPosition(local.side(), local.forward());
                }
                @Override public Vec3 forwardVector() { return SiegeLadderEntity.this.forwardVector(); }
                @Override public Vec3 rightVector() { return SiegeLadderEntity.this.rightVector(); }
                @Override public double platformY(double localY) {
                    return ladderPlatformYAtLocal(getLeanProgress(), localY);
                }
                @Override public float uphillYawDegrees(double angle) {
                    return ladderUphillYawDegrees(angle);
                }
                @Override public boolean automatedClimberActive(UUID entityUuid) {
                    return automatedTraversal.isActive(entityUuid);
                }
                @Override public float randomFloat() { return random.nextFloat(); }
            });
    private float leanVelocity;
    private boolean restingOnSurface;
    private int nextSurfaceRestProbeTick;
    private final SyncedFloatInterpolator clientLeanProgress = new SyncedFloatInterpolator();
    private float previousCollisionLeanDegrees;
    private int previousCollisionStages;

    public SiegeLadderEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 80.0);
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
        data.define(SECTIONS, 1);
        data.define(LEAN_PROGRESS, 0.0F);
        data.define(DEPLOY_TICKS, 0);
        data.define(CARRIER_ID, -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSections(tag.contains(TAG_SECTIONS) ? tag.getInt(TAG_SECTIONS) : 1);
        setLeanProgress(tag.contains(TAG_LEAN_PROGRESS) ? tag.getFloat(TAG_LEAN_PROGRESS) : 0.0F);
        setDeployTicks(tag.contains(TAG_DEPLOY_TICKS) ? tag.getInt(TAG_DEPLOY_TICKS) : 0);
        if (tag.hasUUID(TAG_LEGACY_RELOCATION_OWNER)) {
            setOwnerUuid(tag.getUUID(TAG_LEGACY_RELOCATION_OWNER));
        }
        leanVelocity = 0.0F;
        restingOnSurface = false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SECTIONS, getSections());
        tag.putFloat(TAG_LEAN_PROGRESS, getLeanProgress());
        tag.putInt(TAG_DEPLOY_TICKS, getDeployTicks());
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    public void tick() {
        previousCollisionLeanDegrees = getLeanAngleDegrees();
        previousCollisionStages = builtStages();
        super.tick();
        if (isCarried()) {
            tickCarried();
            return;
        }
        if (level() instanceof ServerLevel) {
            tickDeployAndLean();
            automatedTraversal.tick();
        } else {
            tickClientLeanProgress();
        }
        StructureMotionSystem.tickStructure(this);
        climberSupport.tick();
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        // A carried ladder has no collision.
        return isCarried() ? List.of() : collisionGroups(builtStages(), getLeanAngleDegrees());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return isCarried() ? List.of() : collisionGroups(previousCollisionStages, previousCollisionLeanDegrees);
    }

    public boolean isCarried() {
        return entityData.get(CARRIER_ID) >= 0;
    }

    public int carrierId() {
        return entityData.get(CARRIER_ID);
    }

    @Nullable
    public UUID carrierUuid() {
        return carrierUuid;
    }

    @Nullable
    public Player carrier() {
        return carrier;
    }

    /** Starts carrying: ejects passengers and stops leaning. */
    public void beginCarry(Player carrier) {
        entityData.set(CARRIER_ID, carrier.getId());
        carrierUuid = carrier.getUUID();
        this.carrier = carrier;
        ejectPassengers();
        leanVelocity = 0.0F;
        restingOnSurface = false;
    }

    /** Stops carrying; the ladder falls and tips from where it is. */
    public void endCarry() {
        entityData.set(CARRIER_ID, -1);
        carrierUuid = null;
        carrier = null;
        leanVelocity = 0.0F;
        restingOnSurface = false;
        setDeployTicks(DEPLOY_DELAY_TICKS);
    }

    /** Stands upright at {@code foot}, facing {@code yaw}, then tips forward shortly after. */
    public void standAt(Vec3 foot, float yaw) {
        setPos(foot);
        applyYaw(yaw);
        setLeanProgress(0.0F);
        leanVelocity = 0.0F;
        restingOnSurface = false;
        setDeployTicks(DEPLOY_DELAY_TICKS - PUT_DOWN_STAND_TICKS);
    }

    /** Follows the carrier's hands. On the server, drops the ladder if the carrier is gone. */
    private void tickCarried() {
        Entity carrier = level().isClientSide ? level().getEntity(carrierId()) : this.carrier;
        if (!level().isClientSide && (carrier == null || carrier.isRemoved() || !carrier.isAlive()
                || carrier.level() != level())) {
            LadderCarry.drop(this);
            return;
        }
        if (carrier == null) {
            return;
        }
        LadderCarry.Pose pose = LadderCarry.pose(carrier, getSections(), getLadderLength(), MAX_LEAN_DEGREES);
        setPos(pose.foot());
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        applyYaw(pose.yaw());
        setLeanProgress((float) (pose.leanDegrees() / MAX_LEAN_DEGREES));
        if (level().isClientSide) {
            clientLeanProgress.tick(getLeanProgress());
            LadderCarry.seen(this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isCarried()) {
            LadderCarry.forget(this);
        }
        super.remove(reason);
    }

    /** Number of parts: the base and its sections. */
    private int builtStages() {
        return 1 + getSections();
    }

    @Override
    public int crewCapacity() {
        return 0;
    }

    @Override
    public List<CollisionGroup> solidCollisionGroups() {
        return List.of();
    }

    @Override
    public List<CollisionGroup> previousSolidCollisionGroups() {
        return List.of();
    }

    /** Collision for a ladder whose first {@code stages} parts are built: base first, then sections. */
    private static List<CollisionGroup> collisionGroups(int stages, float leanDegrees) {
        if (stages <= 0) {
            return List.of();
        }
        int sections = stages - 1;
        CollisionPose pose = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.SIEGE_LADDER_BASE.pivot(),
                (float) Math.toRadians(-leanDegrees));
        List<CollisionShape> shapes = List.of(
                GeneratedCollisionShapes.SIEGE_LADDER_BASE,
                GeneratedCollisionShapes.SIEGE_LADDER_SECTION_1,
                GeneratedCollisionShapes.SIEGE_LADDER_SECTION_2,
                GeneratedCollisionShapes.SIEGE_LADDER_SECTION_3,
                GeneratedCollisionShapes.SIEGE_LADDER_SECTION_4);
        int visibleSections = Mth.clamp(sections, MIN_SECTIONS, MAX_SECTIONS);
        List<CollisionGroup> groups = new ArrayList<>(visibleSections + 1);
        groups.add(new CollisionGroup("base", shapes.get(0), pose));
        for (int section = 1; section <= visibleSections; section++) {
            groups.add(new CollisionGroup("section_" + section, shapes.get(section), pose));
        }
        return List.copyOf(groups);
    }

    public int getSections() {
        return Mth.clamp(this.entityData.get(SECTIONS), MIN_SECTIONS, MAX_SECTIONS);
    }

    public void setSections(int sections) {
        this.entityData.set(SECTIONS, Mth.clamp(sections, MIN_SECTIONS, MAX_SECTIONS));
    }

    public float getLeanProgress() {
        return Mth.clamp(this.entityData.get(LEAN_PROGRESS), MIN_LEAN_PROGRESS, MAX_LEAN_PROGRESS);
    }

    private void setLeanProgress(float progress) {
        this.entityData.set(LEAN_PROGRESS, Mth.clamp(progress, MIN_LEAN_PROGRESS, MAX_LEAN_PROGRESS));
    }

    public float getLeanAngleDegrees() {
        return MAX_LEAN_DEGREES * getLeanProgress();
    }

    public float getRenderedLeanAngleDegrees(float partialTick) {
        if (!level().isClientSide || !clientLeanProgress.isInitialized()) {
            return getLeanAngleDegrees();
        }
        return MAX_LEAN_DEGREES * clientLeanProgress.sample(partialTick);
    }

    private int getDeployTicks() {
        return Math.max(0, this.entityData.get(DEPLOY_TICKS));
    }

    private void setDeployTicks(int ticks) {
        this.entityData.set(DEPLOY_TICKS, Math.max(0, ticks));
    }

    private void tickDeployAndLean() {
        if (getDeployTicks() < DEPLOY_DELAY_TICKS) {
            setDeployTicks(getDeployTicks() + 1);
            return;
        }

        float current = getLeanProgress();
        float fallDirection = getFallDirection(current);
        float motionDirection = Math.signum(leanVelocity);
        if (restingOnSurface && (motionDirection == 0.0F || motionDirection == fallDirection)) {
            if (tickCount < nextSurfaceRestProbeTick) {
                leanVelocity = 0.0F;
                return;
            }
            nextSurfaceRestProbeTick = tickCount + SURFACE_REST_PROBE_INTERVAL_TICKS;
            float probe = Mth.clamp(current + fallDirection * SURFACE_REST_PROBE, MIN_LEAN_PROGRESS, MAX_LEAN_PROGRESS);
            if (probe == current || ladderWouldRestOnSurface(probe)) {
                leanVelocity = 0.0F;
                return;
            }
            restingOnSurface = false;
        }

        leanVelocity = Mth.clamp(leanVelocity + fallDirection * getFallAcceleration(current), MAX_BACKWARD_VELOCITY, MAX_FORWARD_VELOCITY);
        float candidate = Mth.clamp(current + leanVelocity, MIN_LEAN_PROGRESS, MAX_LEAN_PROGRESS);
        float candidateDirection = Math.signum(candidate - current);
        boolean movingAgainstFall = candidateDirection != 0.0F && candidateDirection != fallDirection;

        if (candidate != current && !movingAgainstFall && ladderWouldRestOnSurface(candidate)) {
            float contact = findFirstSurfaceContact(current, candidate);
            setLeanProgress(Math.abs(contact - current) <= SURFACE_BACKOFF ? current : contact);
            leanVelocity = 0.0F;
            restingOnSurface = true;
            nextSurfaceRestProbeTick = tickCount + SURFACE_REST_PROBE_INTERVAL_TICKS;
            return;
        }

        restingOnSurface = false;
        setLeanProgress(candidate);
        if (candidate <= MIN_LEAN_PROGRESS || candidate >= MAX_LEAN_PROGRESS) {
            leanVelocity = 0.0F;
        }
    }

    private void tickClientLeanProgress() {
        clientLeanProgress.tick(getLeanProgress());
    }

    private float getFallDirection(float progress) {
        return progress < 0.0F ? -1.0F : 1.0F;
    }

    private float getFallAcceleration(float progress) {
        float eased = Math.abs(progress);
        eased *= eased;
        return FALL_ACCELERATION_BASE + FALL_ACCELERATION_PROGRESS * eased;
    }

    private float findFirstSurfaceContact(float safeProgress, float contactProgress) {
        float safe = safeProgress;
        float contact = contactProgress;
        for (int i = 0; i < 7; i++) {
            float mid = (safe + contact) * 0.5F;
            if (ladderWouldRestOnSurface(mid)) {
                contact = mid;
            } else {
                safe = mid;
            }
        }
        float direction = Math.signum(contactProgress - safeProgress);
        return Mth.clamp(safe - direction * SURFACE_BACKOFF, MIN_LEAN_PROGRESS, MAX_LEAN_PROGRESS);
    }

    private boolean ladderWouldRestOnSurface(float progress) {
        double angle = leanAngleRadians(progress);
        if (Math.abs(angle) < Math.toRadians(8.0D)) {
            return false;
        }

        double maxLocalY = getLadderLength() + LADDER_SURFACE_SENSOR_EXTENSION;
        double startLocalY = Math.min(maxLocalY, LADDER_SURFACE_MIN_LOCAL_Y);
        CollisionShape sensorShape = new CollisionShape(Vec3.ZERO, List.of(
                CollisionPart.axisAligned(new AABB(
                        -LADDER_HALF_WIDTH, startLocalY, -LADDER_HALF_THICKNESS,
                        LADDER_HALF_WIDTH, maxLocalY, LADDER_HALF_THICKNESS))));
        CollisionGroup sensor = new CollisionGroup("surface_sensor", sensorShape,
                CollisionPose.aroundX(Vec3.ZERO, (float) angle));
        List<CollisionGroup> sensorGroups = List.of(sensor);
        AABB bounds = StructureCollisionResolver.worldBounds(this, sensorGroups)
                .inflate(LADDER_SURFACE_CONTACT_TOLERANCE);
        BlockPos minimum = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        BlockPos maximum = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minimum, maximum)) {
            BlockState state = level().getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level(), pos);
            for (AABB box : shape.toAabbs()) {
                if (StructureCollisionResolver.intersects(this, sensorGroups, box.move(pos),
                        LADDER_SURFACE_CONTACT_TOLERANCE)) {
                    return true;
                }
            }
        }
        for (CollidableStructure structure : StructureCollisionSystem.structuresNear(this, bounds)) {
            List<CollisionGroup> solidGroups = structure.solidCollisionGroups();
            if (!solidGroups.isEmpty() && StructureCollisionResolver.intersects(
                    this, sensorGroups, structure, solidGroups, LADDER_SURFACE_CONTACT_TOLERANCE)) {
                return true;
            }
        }
        return false;
    }

    public double getLadderLength() {
        return (1.0D + getSections()) * BLOCKBENCH_SECTION_LENGTH;
    }

    private double ladderPlatformYAtLocal(float progress, double localY) {
        double angle = leanAngleRadians(progress);
        double clampedLocalY = Mth.clamp(localY, 0.0D, getLadderLength() + LADDER_WALK_EXTENSION);
        return getY() + clampedLocalY * Math.cos(angle)
                + Math.sin(angle) * LADDER_HALF_THICKNESS
                + LADDER_PLATFORM_Y_OFFSET;
    }

    private double leanAngleRadians(float progress) {
        return Math.toRadians(MAX_LEAN_DEGREES * progress);
    }

    private Vec3 ladderWorldPoint(double side, double localY, double localZ) {
        double angle = leanAngleRadians(getLeanProgress());
        return ladderWorldPoint(side, localY, localZ, angle);
    }

    private Vec3 ladderWorldPoint(double side, double localY, double localZ, double angle) {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        double forwardX = -Math.sin(yawRadians) * FALL_DIRECTION;
        double forwardZ = Math.cos(yawRadians) * FALL_DIRECTION;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);
        double horizontalForward = localY * Math.sin(angle) - localZ * Math.cos(angle);
        double y = getY() + localY * Math.cos(angle) + localZ * Math.sin(angle);
        return new Vec3(
                getX() + rightX * side + forwardX * horizontalForward,
                y,
                getZ() + rightZ * side + forwardZ * horizontalForward);
    }

    private LadderLocalPosition ladderLocalPosition(Vec3 worldPosition) {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        double forwardX = -Math.sin(yawRadians) * FALL_DIRECTION;
        double forwardZ = Math.cos(yawRadians) * FALL_DIRECTION;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);
        double dx = worldPosition.x - getX();
        double dz = worldPosition.z - getZ();
        return new LadderLocalPosition(dx * rightX + dz * rightZ, dx * forwardX + dz * forwardZ);
    }

    private Vec3 forwardVector() {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        return new Vec3(-Math.sin(yawRadians) * FALL_DIRECTION, 0.0D, Math.cos(yawRadians) * FALL_DIRECTION).normalize();
    }

    private Vec3 rightVector() {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        return new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
    }

    private float ladderUphillYawDegrees(double angle) {
        float yaw = getVisualRotationYInDegrees();
        return Math.sin(angle) < 0.0D ? Mth.wrapDegrees(yaw + 180.0F) : yaw;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Carried ladders are invulnerable, so low ceilings and doorways don't break them.
        if (isCarried()) {
            return false;
        }
        applyHitImpulse(source, amount);
        return super.hurt(source, amount);
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isPickable() {
        return !isCarried();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    private void applyHitImpulse(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel) || source.getEntity() == null) {
            return;
        }

        Vec3 hitSource = source.getSourcePosition();
        if (hitSource == null) {
            hitSource = source.getEntity().position();
        }

        double side = hitSource.subtract(position()).dot(forwardVector());
        float direction = side >= 0.0D ? -1.0F : 1.0F;
        float impulse = 0.004F + Math.min(8.0F, Math.max(0.0F, amount)) * 0.0009F;
        leanVelocity = Mth.clamp(leanVelocity + direction * impulse, MAX_BACKWARD_VELOCITY, MAX_FORWARD_VELOCITY);
        restingOnSurface = false;
        setLeanProgress(getLeanProgress() + direction * impulse * 0.14F);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.LADDER_HIT, SoundSource.BLOCKS, 0.85F, 0.8F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean isReadyForAutomatedClimb() {
        float absoluteProgress = Math.abs(getLeanProgress());
        float minimumProgress = 8.0F / MAX_LEAN_DEGREES;
        return getDeployTicks() >= DEPLOY_DELAY_TICKS
                && absoluteProgress >= minimumProgress
                && (restingOnSurface || absoluteProgress >= 0.999F);
    }

    public boolean canBeRelocated() {
        return isAlive()
                && isFullyBuilt()
                && !isCarried()
                && getPassengers().isEmpty()
                && !automatedTraversal.hasClimbers()
                && !isDismantling();
    }

    public boolean canBeRelocatedBy(UUID playerUuid) {
        return playerUuid != null && (getOwnerUuid() == null
                || SiegeAccess.relationOf(playerUuid, this) != SiegeRelation.HOSTILE);
    }

    public ItemStack createRelocationItem() {
        ItemStack stack = SiegeLadderDeploymentItem.withSections(
                new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), getSections());
        SiegeLadderDeploymentItem.withStoredHealth(stack, getHealth());
        if (getOwnerUuid() != null) {
            SiegeLadderDeploymentItem.withRelocationOwner(stack, getOwnerUuid());
        }
        if (getDeploymentOwnerUuid() != null && !getDeploymentGroup().isBlank()) {
            SiegeDeploymentLimits.writeToStack(stack, new SiegeDeploymentLimits.Deployment(
                    getDeploymentOwnerUuid(), getDeploymentGroup()));
        }
        if (hasCustomName()) {
            me.mss1r.siegeworks.platform.MinecraftVersionCompat.setCustomName(stack, getCustomName());
        }
        return stack;
    }

    public int getActiveAutomatedClimberCount() {
        return automatedTraversal.activeClimberCount();
    }

    @Override
    public Vec3 getAutomatedBottomApproach() {
        return automatedTraversal.bottomApproach();
    }

    @Override
    public Vec3 getAutomatedTopExit() {
        return automatedTraversal.topExit();
    }

    @Override
    public Vec3 getAutomatedQueuePosition(LivingEntity climber, boolean upward) {
        return automatedTraversal.queuePosition(climber, upward);
    }

    @Override
    public ClimbResult advanceAutomatedClimber(LivingEntity climber, boolean upward) {
        return automatedTraversal.advance(climber, upward);
    }

    @Override
    public void cancelAutomatedClimb(LivingEntity climber) {
        automatedTraversal.cancel(climber);
    }

    private Vec3 getUphillVector() {
        double direction = Math.signum(Math.sin(leanAngleRadians(getLeanProgress())));
        return forwardVector().scale(direction == 0.0D ? 1.0D : direction);
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return false;
    }

    @Override
    public boolean isCapturable() {
        return false;
    }

    @Override
    protected InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (!player.isShiftKeyDown() || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.SUCCESS;
        }
        if (!canBeRelocated()) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.ladder.pickup_unavailable"), true);
            return InteractionResult.SUCCESS;
        }
        if (!canBeRelocatedBy(player.getUUID())) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.ladder.not_owner"), true);
            return InteractionResult.SUCCESS;
        }

        // Picked up into the hands, not the inventory.
        if (LadderCarry.tryPickUp(player, this)) {
            claimOwnership(player.getUUID());
            if (getDeploymentOwnerUuid() == null) {
                SiegeDeploymentLimits.Deployment deployment =
                        SiegeDeploymentLimits.forOwner(serverLevel, player.getUUID());
                setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "idle", state -> PlayState.STOP));
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
    protected Vec3 getOperatorOffset(Entity entity) {
        return Vec3.ZERO;
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }

    private record LadderLocalPosition(double side, double forward) {
    }

}
