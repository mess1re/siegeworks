package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.api.SiegeClimbableControl;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.CollisionShape;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeRelation;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.movement.SyncedFloatInterpolator;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderAutomatedTraversal;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderBody;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderClimberSupport;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.registry.SiegeworksItems;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
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
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimationController;
*///?} else {
import software.bernie.geckolib.animation.AnimationController;
//?}
//? if <1.21 {
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
    /** Lean from vertical, positive toward forward; past 90 the top is below the foot. Not wrapped. */
    private static final EntityDataAccessor<Float> LEAN_DEGREES =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DEPLOY_TICKS =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);
    /** The entity id of whoever carries the ladder in their hands, or -1. */
    private static final EntityDataAccessor<Integer> CARRIER_ID =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);
    /** The entity id of whoever braces a pole against the ladder now, or -1. */
    private static final EntityDataAccessor<Integer> BRACER_ID =
            SynchedEntityData.defineId(SiegeLadderEntity.class, EntityDataSerializers.INT);

    private static final String TAG_SECTIONS = "Sections";
    private static final String TAG_LEAN_DEGREES = "LeanDegrees";
    private static final String TAG_SETTLED = "Settled";
    private static final String TAG_DEPLOY_TICKS = "DeployTicks";
    private static final String TAG_LEGACY_RELOCATION_OWNER = "RelocationOwner";

    private static final int DEPLOY_DELAY_TICKS = 35;
    /** How fast a braced ladder is eased off its wall at most. */
    private static final double BRACED_DEGREES_PER_TICK = 1.0D;
    /** Ticks a ladder stands upright after being put down before it tips forward. */
    private static final int PUT_DOWN_STAND_TICKS = 10;
    private static final double BLOCKBENCH_SECTION_LENGTH = 48.0D / 16.0D;
    private static final double LADDER_HALF_WIDTH = 7.5D / 16.0D;
    private static final double LADDER_HALF_THICKNESS = 1.5D / 16.0D;
    private static final double LADDER_WALK_EXTENSION = 4.0D / 16.0D;
    private static final double LADDER_PLATFORM_Y_OFFSET = 0.04D;
    /** A short ladder is carried this far from vertical: level overhead. */
    private static final float CARRY_LEVEL_DEGREES = 89.0F;
    /** Climbing needs at least this lean; nearer upright the ladder rests on nothing. */
    private static final double MIN_CLIMB_LEAN_DEGREES = 8.0D;
    /** The climber support reads lean as a share of this. */
    private static final double LEVEL_DEGREES = 89.0D;
    /** How fast whoever stood the ladder up sends its top forward when letting go, in blocks per tick. */
    private static final double LET_GO_PUSH = 0.03D;
    /**
     * Speed added to the top each tick of a push off the wall, until the ladder is past upright the other way: a
     * steady shove, plus what holds up the ladder's weight leaning on the wall, which grows as it lies flatter.
     */
    private static final double SHOVE_PER_TICK = 0.04D;
    private static final double SHOVE_PER_LEAN = 0.15D;
    /** How long a push off the wall goes on at most, and how far past upright it carries the ladder. */
    private static final int SHOVE_TICKS = 60;
    private static final float SHOVE_PAST_UPRIGHT_DEGREES = 8.0F;
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
                @Override public double leanAngleRadians() { return climbLeanRadians(); }
                @Override public Vec3 ladderWorldPoint(double localY) {
                    return SiegeLadderEntity.this.ladderWorldPoint(0.0D, localY, 0.0D);
                }
                @Override public double platformY(double localY) { return ladderPlatformY(localY); }
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
                @Override public float leanProgress() {
                    return (float) (Math.toDegrees(climbLeanRadians()) / LEVEL_DEGREES);
                }
                @Override public double leanAngleRadians() { return climbLeanRadians(); }
                @Override public double ladderLength() { return getLadderLength(); }
                @Override public LadderClimberSupport.LocalPosition localPosition(Vec3 worldPosition) {
                    LadderLocalPosition local = ladderLocalPosition(worldPosition);
                    return new LadderClimberSupport.LocalPosition(local.side(), local.forward());
                }
                @Override public Vec3 forwardVector() { return SiegeLadderEntity.this.forwardVector(); }
                @Override public Vec3 rightVector() { return SiegeLadderEntity.this.rightVector(); }
                @Override public double platformY(double localY) { return ladderPlatformY(localY); }
                @Override public float uphillYawDegrees(double angle) {
                    return ladderUphillYawDegrees(angle);
                }
                @Override public boolean automatedClimberActive(UUID entityUuid) {
                    return automatedTraversal.isActive(entityUuid);
                }
                @Override public float randomFloat() { return random.nextFloat(); }
            });
    private final LadderBody body = new LadderBody(LADDER_HALF_WIDTH, LADDER_HALF_THICKNESS);
    private final me.mss1r.siegeworks.gameplay.ladder.LadderPush push = new me.mss1r.siegeworks.gameplay.ladder.LadderPush();
    /** Server only: ticks left of a push off the wall, and which way it pushes the top. */
    private int shoveTicks;
    private float shoveSign;
    /** Server only: the lean, in degrees, the ladder was braced from, and the lean it is held at; NaN when not braced. */
    private double bracedFrom = Double.NaN;
    private double bracedLean = Double.NaN;
    /** Server only: the carried ladder's foot and top last tick, and how far each moved, to let it go moving. */
    @Nullable
    private Vec3 carriedFoot;
    @Nullable
    private Vec3 carriedTop;
    private Vec3 carriedFootMotion = Vec3.ZERO;
    private Vec3 carriedTopMotion = Vec3.ZERO;
    private final SyncedFloatInterpolator clientLean = new SyncedFloatInterpolator();
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
    //? if <1.21 {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(SECTIONS, 1);
        data.define(LEAN_DEGREES, 0.0F);
        data.define(DEPLOY_TICKS, 0);
        data.define(CARRIER_ID, -1);
        data.define(BRACER_ID, -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSections(tag.contains(TAG_SECTIONS) ? tag.getInt(TAG_SECTIONS) : 1);
        setLeanDegrees(tag.contains(TAG_LEAN_DEGREES) ? tag.getFloat(TAG_LEAN_DEGREES)
                : tag.getFloat("LeanProgress") * (float) LEVEL_DEGREES);
        setDeployTicks(tag.contains(TAG_DEPLOY_TICKS) ? tag.getInt(TAG_DEPLOY_TICKS) : 0);
        if (tag.hasUUID(TAG_LEGACY_RELOCATION_OWNER)) {
            setOwnerUuid(tag.getUUID(TAG_LEGACY_RELOCATION_OWNER));
        }
        body.restore(tag.getBoolean(TAG_SETTLED));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SECTIONS, getSections());
        tag.putFloat(TAG_LEAN_DEGREES, getLeanAngleDegrees());
        tag.putInt(TAG_DEPLOY_TICKS, getDeployTicks());
        tag.putBoolean(TAG_SETTLED, body.settled());
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
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel) {
            tickBody();
            automatedTraversal.tick();
            entityData.set(BRACER_ID, push.activeBracer(level().getGameTime()));
        } else {
            tickClientLean();
            me.mss1r.siegeworks.gameplay.ladder.LadderPush.seen(this);
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
        body.stop();
        carriedFoot = null;
        carriedTop = null;
        carriedFootMotion = Vec3.ZERO;
        carriedTopMotion = Vec3.ZERO;
    }

    /** Stops carrying; the ladder falls from where it is, moving as it was carried. */
    public void endCarry() {
        entityData.set(CARRIER_ID, -1);
        carrierUuid = null;
        carrier = null;
        Vec3 forward = forwardVector();
        body.launch(carriedFootMotion.dot(forward), carriedFootMotion.y,
                carriedTopMotion.dot(forward), carriedTopMotion.y);
        setDeployTicks(DEPLOY_DELAY_TICKS);
    }

    /** Stands upright at {@code foot}, facing {@code yaw}, then lets it go to tip forward shortly after. */
    public void standAt(Vec3 foot, float yaw) {
        setPos(foot);
        applyYaw(yaw);
        setLeanDegrees(0.0F);
        body.stop();
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
        LadderCarry.Pose pose = LadderCarry.pose(carrier, getSections(), getLadderLength(), CARRY_LEVEL_DEGREES);
        setPos(pose.foot());
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0F;
        applyYaw(pose.yaw());
        setLeanDegrees((float) pose.leanDegrees());
        if (level().isClientSide) {
            clientLean.tick(getLeanAngleDegrees());
            LadderCarry.seen(this);
            return;
        }
        Vec3 top = ladderWorldPoint(0.0D, getLadderLength(), 0.0D, Math.toRadians(getLeanAngleDegrees()));
        carriedFootMotion = carriedFoot == null ? Vec3.ZERO : pose.foot().subtract(carriedFoot);
        carriedTopMotion = carriedTop == null ? Vec3.ZERO : top.subtract(carriedTop);
        carriedFoot = pose.foot();
        carriedTop = top;
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
    public List<CollisionGroup> supportContactGroups() {
        // Rungs stay climb-through, but the ladder itself still occupies space when placed or carried.
        List<CollisionGroup> groups = new ArrayList<>(super.supportContactGroups());
        groups.addAll(collisionGroups());
        return List.copyOf(groups);
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

    public float getLeanAngleDegrees() {
        return this.entityData.get(LEAN_DEGREES);
    }

    private void setLeanDegrees(float degrees) {
        this.entityData.set(LEAN_DEGREES, degrees);
    }

    public float getRenderedLeanAngleDegrees(float partialTick) {
        if (!level().isClientSide || !clientLean.isInitialized()) {
            return getLeanAngleDegrees();
        }
        return clientLean.sample(partialTick);
    }

    private int getDeployTicks() {
        return Math.max(0, this.entityData.get(DEPLOY_TICKS));
    }

    private void setDeployTicks(int ticks) {
        this.entityData.set(DEPLOY_TICKS, Math.max(0, ticks));
    }

    /** Held upright while being stood up, then let go with a push forward, then left to the body's physics. */
    private void tickBody() {
        if (shoveTicks > 0) {
            shoveTicks--;
            if (getLeanAngleDegrees() * shoveSign < SHOVE_PAST_UPRIGHT_DEGREES) {
                double lean = Math.toRadians(getLeanAngleDegrees());
                body.pushTop(lean, shoveSign * (SHOVE_PER_TICK + SHOVE_PER_LEAN * Math.abs(Math.sin(lean))));
            } else {
                shoveTicks = 0;
            }
        }
        if (getDeployTicks() < DEPLOY_DELAY_TICKS) {
            setDeployTicks(getDeployTicks() + 1);
            if (getDeployTicks() == DEPLOY_DELAY_TICKS && Math.abs(getLeanAngleDegrees()) < 1.0F) {
                body.pushTop(0.0D, LET_GO_PUSH);
            }
        }
        boolean holdUpright = getDeployTicks() < DEPLOY_DELAY_TICKS;
        double lean = Math.toRadians(getLeanAngleDegrees());
        double held = holdUpright ? 0.0D : Math.toRadians(tickBraced());
        LadderBody.Pose pose = body.tick(level(), this, position(), holdUpright ? 0.0D : lean,
                getLadderLength(), forwardVector(), rightVector(), held, tickCount);
        if (pose == null) {
            return;
        }
        setPos(pose.foot());
        // Kept unwrapped, so a ladder turning end over end renders turning rather than jumping back.
        double turned = Mth.wrapDegrees(Math.toDegrees(pose.leanRadians()) - getLeanAngleDegrees());
        setLeanDegrees((float) (getLeanAngleDegrees() + turned));
    }

    /** The ladder never walks or rolls: it moves only as its body or its carrier moves it. */
    @Override
    public void travel(Vec3 travelVector) {
    }

    @Override
    protected boolean usesSiegeMovement() {
        return false;
    }

    /**
     * The foot follows the server in one tick, as the lean does: interpolated over different spans, the drawn ladder
     * would be a pose it never had, its top swung into the blocks it rests on.
     */
    @Override
    //? if <1.21 {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean teleport) {
        super.lerpTo(x, y, z, yaw, pitch, 1, teleport);
    }
    *///?} else {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        super.lerpTo(x, y, z, yaw, pitch, 1);
    }
    //?}

    private void tickClientLean() {
        clientLean.tick(getLeanAngleDegrees());
    }

    public double getLadderLength() {
        return (1.0D + getSections()) * BLOCKBENCH_SECTION_LENGTH;
    }

    /** True when the top end is lower than the foot: the ladder came down the other way up. */
    private boolean upsideDown() {
        return Math.cos(Math.toRadians(getLeanAngleDegrees())) < 0.0D;
    }

    /**
     * Where climbing starts: the lower end. For a ladder the right way up that is its foot; upside down, its top.
     */
    public Vec3 climbFoot() {
        return upsideDown()
                ? pointFrom(position(), 0.0D, getLadderLength(), 0.0D, Math.toRadians(getLeanAngleDegrees()))
                : position();
    }

    /** The point on the ladder's middle line nearest to {@code point}. */
    public Vec3 nearestPoint(Vec3 point) {
        Vec3 foot = position();
        Vec3 axis = pointFrom(foot, 0.0D, getLadderLength(), 0.0D, Math.toRadians(getLeanAngleDegrees())).subtract(foot);
        double along = Mth.clamp(point.subtract(foot).dot(axis) / axis.lengthSqr(), 0.0D, 1.0D);
        return foot.add(axis.scale(along));
    }

    /** The lean seen from the lower end, within a right angle of vertical. */
    private double climbLeanRadians() {
        return Math.toRadians(Mth.wrapDegrees(getLeanAngleDegrees() + (upsideDown() ? 180.0D : 0.0D)));
    }

    private double ladderPlatformY(double localY) {
        double angle = climbLeanRadians();
        double clampedLocalY = Mth.clamp(localY, 0.0D, getLadderLength() + LADDER_WALK_EXTENSION);
        return climbFoot().y + clampedLocalY * Math.cos(angle)
                + Math.sin(angle) * LADDER_HALF_THICKNESS
                + LADDER_PLATFORM_Y_OFFSET;
    }

    /** A point on the ladder as climbed: {@code localY} up from the lower end. */
    private Vec3 ladderWorldPoint(double side, double localY, double localZ) {
        return pointFrom(climbFoot(), side, localY, localZ, climbLeanRadians());
    }

    private Vec3 ladderWorldPoint(double side, double localY, double localZ, double angle) {
        return pointFrom(position(), side, localY, localZ, angle);
    }

    private Vec3 pointFrom(Vec3 origin, double side, double localY, double localZ, double angle) {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        double forwardX = -Math.sin(yawRadians) * FALL_DIRECTION;
        double forwardZ = Math.cos(yawRadians) * FALL_DIRECTION;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);
        double horizontalForward = localY * Math.sin(angle) - localZ * Math.cos(angle);
        double y = origin.y + localY * Math.cos(angle) + localZ * Math.sin(angle);
        return new Vec3(
                origin.x + rightX * side + forwardX * horizontalForward,
                y,
                origin.z + rightZ * side + forwardZ * horizontalForward);
    }

    private LadderLocalPosition ladderLocalPosition(Vec3 worldPosition) {
        float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
        double forwardX = -Math.sin(yawRadians) * FALL_DIRECTION;
        double forwardZ = Math.cos(yawRadians) * FALL_DIRECTION;
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);
        Vec3 foot = climbFoot();
        double dx = worldPosition.x - foot.x;
        double dz = worldPosition.z - foot.z;
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
        // Turning speed in radians per tick, given to the top across the ladder.
        double turn = Math.toRadians(LEVEL_DEGREES) * (0.004D + Math.min(8.0F, Math.max(0.0F, amount)) * 0.0009D);
        body.pushTop(Math.toRadians(getLeanAngleDegrees()), direction * turn * getLadderLength());

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, blockPosition(), SoundEvents.LADDER_HIT, SoundSource.BLOCKS, 0.85F, 0.8F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean isReadyForAutomatedClimb() {
        return getDeployTicks() >= DEPLOY_DELAY_TICKS
                && body.settled()
                && Math.abs(climbLeanRadians()) >= Math.toRadians(MIN_CLIMB_LEAN_DEGREES);
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

    public String describeAutomatedClimber(Entity climber) {
        return automatedTraversal.describe(climber.getUUID());
    }

    @Override
    public Vec3 getAutomatedBottomApproach() {
        return automatedTraversal.bottomApproach();
    }

    @Override
    public boolean canTraverse(LivingEntity climber, boolean upward) {
        return isReadyForAutomatedClimb() && automatedTraversal.routeAvailable(climber);
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
        double direction = Math.signum(Math.sin(climbLeanRadians()));
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

    /**
     * Braced with a long weapon, held use pushes the ladder off its wall, without swinging the arm. The forked pole
     * passes so that its own use holds it braced, both hands forward.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof me.mss1r.siegeworks.item.ForkedPoleItem) {
            return InteractionResult.PASS;
        }
        if (hand == InteractionHand.MAIN_HAND && !isCarried()
                && me.mss1r.siegeworks.gameplay.ladder.LadderPush.isPusher(player, held)) {
            braceWith(player, hand);
            return InteractionResult.CONSUME;
        }
        return super.interact(player, hand);
    }

    /**
     * The lean, in degrees, bracing holds the ladder at this tick, easing it off the wall as the brace goes on; NaN
     * once nobody braces it, so it falls back where it leans.
     */
    private double tickBraced() {
        double progress = shoveTicks > 0 ? 0.0D : push.progress();
        if (progress <= 0.0D) {
            bracedFrom = Double.NaN;
            bracedLean = Double.NaN;
            return Double.NaN;
        }
        if (Double.isNaN(bracedFrom)) {
            bracedFrom = getLeanAngleDegrees();
            bracedLean = bracedFrom;
        }
        double target = me.mss1r.siegeworks.gameplay.ladder.LadderPush.heldLean(bracedFrom, getLadderLength(), progress);
        // A brace that only lands every few ticks still moves the ladder evenly
        bracedLean += Mth.clamp(target - bracedLean, -BRACED_DEGREES_PER_TICK, BRACED_DEGREES_PER_TICK);
        return bracedLean;
    }

    /** One tick or held use of {@code player}'s pole braced against this ladder. */
    public void braceWith(Player player, InteractionHand hand) {
        if (!isCarried() && level() instanceof ServerLevel serverLevel) {
            push.brace(this, player, hand, serverLevel.getGameTime());
        }
    }

    /** The entity id of whoever braces a pole against the ladder now, or -1. */
    public int bracerId() {
        return entityData.get(BRACER_ID);
    }

    /** The middle of the ladder's top end, where a pole braces against it. */
    public Vec3 topPoint() {
        return ladderWorldPoint(0.0D, getLadderLength(), 0.0D, Math.toRadians(getLeanAngleDegrees()));
    }

    /** Horizontal direction from the ladder's foot toward what it leans on. */
    public Vec3 uphillVector() {
        return getUphillVector();
    }

    /** Everyone on the ladder now: players held on it and those it leads. */
    public int climberCount() {
        return climberSupport.climberCount() + automatedTraversal.activeClimberCount();
    }

    /** Pushes the ladder's top away from what it leans on, letting go of everyone on it. */
    public void pushOffWall() {
        climberSupport.letGo();
        if (level() instanceof ServerLevel serverLevel) {
            automatedTraversal.letGoAll(uuid -> serverLevel.getEntity(uuid) instanceof LivingEntity living ? living : null);
        }
        shoveSign = -Math.signum(getLeanAngleDegrees());
        shoveTicks = SHOVE_TICKS;
        body.stop();
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
