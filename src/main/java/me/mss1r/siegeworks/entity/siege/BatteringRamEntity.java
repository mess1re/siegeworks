package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeMeleeControl;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
/*import software.bernie.geckolib.core.animation.Animation;
*///?} else {
import software.bernie.geckolib.animation.Animation;
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
//? if forge {
/*import software.bernie.geckolib.core.animation.RawAnimation;
*///?} else {
import software.bernie.geckolib.animation.RawAnimation;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class BatteringRamEntity extends AbstractSiegeEntity implements GeoEntity, SiegeMeleeControl {

    private static final int MAX_PUSHERS = 2;
    private static final int FULL_DRAFT_TEAM = 2;
    private static final float PUSHER_VIEW_LIMIT = 35.0F;
    private static final double MODEL_UNITS_PER_BLOCK = 16.0D;
    private static final double DRAFT_TEAM_LATERAL_OFFSET = 24.5D / MODEL_UNITS_PER_BLOCK;
    private static final double TOW_DISTANCE = 71.0D / MODEL_UNITS_PER_BLOCK + 2.0D;
    private static final int ATTACK_IMPACT_TICK = 70;
    private static final int RAM_RELEASE_SOUND_TICK = 63;
    private static final int ATTACK_ANIMATION_TICKS = 80;
    private static final float RAM_DRAW_END_TICK = 60.0F;
    private static final float RAM_DRAW_HOLD_END_TICK = 63.334F;
    private static final float RAM_RETURN_END_TICK = 66.666F;
    private static final float RAM_RECOIL_END_TICK = 75.834F;
    private static final float FRAME_RECOIL_START_TICK = 70.834F;
    private static final double RAM_HEAD_CENTER_FORWARD = 75.0D / 16.0D;
    private static final double RAM_HEAD_CENTER_HEIGHT = 36.0D / 16.0D;
    private static final double RAM_HEAD_HALF_WIDTH = 5.0D / 16.0D;
    private static final double RAM_HEAD_HALF_HEIGHT = 5.0D / 16.0D;
    private static final double RAM_HEAD_HALF_DEPTH = 6.0D / 16.0D;
    private static final Vec3[] PUSHER_OFFSETS = {
            new Vec3(24.0D / 16.0D, 0.0D, 87.0D / 16.0D),
            new Vec3(-25.0D / 16.0D, 0.0D, 87.0D / 16.0D)
    };
    private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_TICK =
            SynchedEntityData.defineId(BatteringRamEntity.class, EntityDataSerializers.INT);

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withReloadSound(SiegeworksSounds.ROPE_CHARGE_BR.get())
            .withAttackSound(SiegeworksSounds.RAM_IMPACT.get())
            .withMovementInterval(150)
            .withMovementRange(30.0)
            .withReloadRange(15.0)
            .withAttackRange(40.0);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation attackAnim = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    private int previousAttackAnimationTick = -1;

    public BatteringRamEntity(EntityType<? extends LivingEntity> type, Level level) {
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
        data.define(ATTACK_ANIMATION_TICK, -1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.025)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() { return SOUND_PROFILE; }

    @Override
    protected void playAttackSound(ServerLevel serverLevel) {
        playSoundToNearbyPlayers(serverLevel, getAttackSound(), getSoundProfile().attack().range(),
                1.8F, 0.92F, 0.04F);
    }

    @Override
    public int crewCapacity() {
        return MAX_PUSHERS;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(TOW_DISTANCE,
                DRAFT_TEAM_LATERAL_OFFSET, -DRAFT_TEAM_LATERAL_OFFSET);
    }

    private double propulsionTeamPower(Entity operator) {
        if (isDraftMount(operator)) {
            return Math.min(1.0D, (double) getTowingMounts().size() / FULL_DRAFT_TEAM);
        }
        if (isSupportedDirectOperator(operator)) {
            long activePushers = getPassengers().stream()
                    .filter(this::isSupportedDirectOperator)
                    .count();
            return Math.min(1.0D, (double) activePushers / MAX_PUSHERS);
        }
        return 1.0D;
    }

    @Override
    public double getVelocity(Entity operator) {
        return super.getVelocity(operator) * propulsionTeamPower(operator);
    }

    @Override
    public double getDriveAcceleration(Entity operator) {
        return super.getDriveAcceleration(operator) * propulsionTeamPower(operator);
    }

    @Override
    public double getDriveDeceleration(Entity operator) {
        return super.getDriveDeceleration(operator) * propulsionTeamPower(operator);
    }

    @Override
    public float getSteeringSpeedDegrees(Entity operator) {
        return (float) (super.getSteeringSpeedDegrees(operator) * propulsionTeamPower(operator));
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.isEmpty() && canAddPassenger(player) && !player.isShiftKeyDown()) {
            boolean becomesDriver = getPassengers().isEmpty();
            player.startRiding(this);
            if (becomesDriver) {
                setOperator(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (getPassengers().isEmpty() && canBeginAttack() && player.isShiftKeyDown()) {
            beginAttack(serverLevel, this);
            return InteractionResult.SUCCESS;
        }

        if (getPassengers().isEmpty() && !canBeginAttack() && player.isShiftKeyDown()) {
            return showCooldownProgress(player);
        }

        return InteractionResult.PASS;
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel serverLevel) || !isOperator(operator)) {
            return SiegeActionResult.DENIED;
        }
        if (!canBeginAttack()) {
            return SiegeActionResult.IN_PROGRESS;
        }

        beginAttack(serverLevel, operator);
        return SiegeActionResult.FIRED;
    }

    @Override
    public double getAutomatedAttackRange() {
        return RAM_HEAD_CENTER_FORWARD + RAM_HEAD_HALF_DEPTH;
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        if (!isOperator(operator) || getCooldown() <= 0 || hasAttackHappened()) {
            return;
        }

        setAttackHappened(true);
        setCooldown(0);
        setAttackAnimationTick(-1);
        stopAnimation("attack");
    }

    @Override
    public void tick() {
        previousAttackAnimationTick = getAttackAnimationTick();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        int animationTick = getAttackAnimationTick();
        if (animationTick < 0) {
            return;
        }

        animationTick++;
        setAttackAnimationTick(animationTick >= ATTACK_ANIMATION_TICKS ? -1 : animationTick);
        if (animationTick == RAM_RELEASE_SOUND_TICK) {
            playSoundToNearbyPlayers(serverLevel, SiegeworksSounds.RAM_RELEASE.get(), 24.0D, 1.0F);
        }
        if (animationTick >= ATTACK_IMPACT_TICK && !hasAttackHappened()) {
            performAttack(serverLevel);
            setAttackHappened(true);
            playAttackSound(serverLevel);
        }
    }

    @Override
    protected String getCooldownStatusKey() {
        return hasAttackHappened() ? "siege.loading.state.recovering" : "siege.loading.state.ram_swing";
    }

    private void beginAttack(ServerLevel serverLevel, Entity attacker) {
        triggerAnimation("attack");
        setAttackHappened(false);
        setAttackAnimationTick(0);
        startRecovery();
        setOperator(attacker);
        playReloadSound(serverLevel);
    }

    private boolean canBeginAttack() {
        return getCooldown() <= 0 && getAttackAnimationTick() < 0;
    }

    public int getAttackAnimationTick() {
        return entityData.get(ATTACK_ANIMATION_TICK);
    }

    public float wheelRecoilDegrees(float partialTick) {
        int tick = getAttackAnimationTick();
        return tick < 0 ? 0.0F : (float) (-10.0D * framePositionAt(tick + partialTick).z / 3.0D);
    }

    /** Metres per second immediately before the ram head reaches the end of its forward stroke. */
    public static double strikeSpeed() {
        Vec3 previous = ramPositionAt(ATTACK_IMPACT_TICK - 1);
        Vec3 impact = ramPositionAt(ATTACK_IMPACT_TICK);
        return previous.distanceTo(impact) * 20.0D / MODEL_UNITS_PER_BLOCK;
    }

    private void setAttackAnimationTick(int tick) {
        entityData.set(ATTACK_ANIMATION_TICK, tick);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getAttackAnimationTick());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousAttackAnimationTick);
    }

    private static List<CollisionGroup> collisionGroups(float animationTick) {
        CollisionPose framePose = CollisionPose.fromGeckoBonePosition(framePositionAt(animationTick));
        CollisionPose ramPose = CollisionPose.fromGeckoBonePosition(ramPositionAt(animationTick)).then(framePose);
        return List.of(
                new CollisionGroup("body", GeneratedCollisionShapes.BATTERING_RAM_BODY, framePose),
                new CollisionGroup("ram", GeneratedCollisionShapes.BATTERING_RAM_BEAM, ramPose));
    }

    private static Vec3 framePositionAt(float tick) {
        if (tick < FRAME_RECOIL_START_TICK || tick < 0.0F) {
            return Vec3.ZERO;
        }
        if (tick < RAM_RECOIL_END_TICK) {
            return new Vec3(0.0D, 0.0D,
                    lerp(tick, FRAME_RECOIL_START_TICK, RAM_RECOIL_END_TICK, 0.0D, 3.0D));
        }
        if (tick < ATTACK_ANIMATION_TICKS) {
            return new Vec3(0.0D, 0.0D,
                    lerp(tick, RAM_RECOIL_END_TICK, ATTACK_ANIMATION_TICKS, 3.0D, 0.0D));
        }
        return Vec3.ZERO;
    }

    private static Vec3 ramPositionAt(float tick) {
        if (tick <= 0.0F || tick >= ATTACK_ANIMATION_TICKS) {
            return Vec3.ZERO;
        }
        if (tick < RAM_DRAW_END_TICK) {
            double progress = tick / RAM_DRAW_END_TICK;
            return new Vec3(0.0D, 3.0D * progress, 10.0D * progress);
        }
        if (tick < RAM_DRAW_HOLD_END_TICK) {
            return new Vec3(0.0D, 3.0D, 10.0D);
        }
        if (tick < RAM_RETURN_END_TICK) {
            double progress = inverseLerp(tick, RAM_DRAW_HOLD_END_TICK, RAM_RETURN_END_TICK);
            return new Vec3(0.0D, 3.0D * (1.0D - progress), 10.0D * (1.0D - progress));
        }
        if (tick < ATTACK_IMPACT_TICK) {
            return new Vec3(0.0D, 0.0D,
                    lerp(tick, RAM_RETURN_END_TICK, ATTACK_IMPACT_TICK, 0.0D, -9.0D));
        }
        if (tick < RAM_RECOIL_END_TICK) {
            double progress = easeInElastic(inverseLerp(tick, ATTACK_IMPACT_TICK, RAM_RECOIL_END_TICK));
            return new Vec3(0.0D, 0.0D, -9.0D + 12.0D * progress);
        }
        return new Vec3(0.0D, 0.0D,
                lerp(tick, RAM_RECOIL_END_TICK, ATTACK_ANIMATION_TICKS, 3.0D, 0.0D));
    }

    private static double lerp(float tick, float startTick, float endTick, double start, double end) {
        return start + (end - start) * inverseLerp(tick, startTick, endTick);
    }

    private static double inverseLerp(float value, float start, float end) {
        return Math.max(0.0D, Math.min(1.0D, (value - start) / (end - start)));
    }

    private static double easeInElastic(double progress) {
        if (progress <= 0.0D || progress >= 1.0D) {
            return progress;
        }
        double period = 2.0D * Math.PI / 3.0D;
        return -Math.pow(2.0D, 10.0D * progress - 10.0D)
                * Math.sin((10.0D * progress - 10.75D) * period);
    }

    private void performAttack(ServerLevel serverLevel) {
        Vec3 forward = getHorizontalForward();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        Vec3 center = position()
                .add(forward.scale(RAM_HEAD_CENTER_FORWARD
                        - ramPositionAt(ATTACK_IMPACT_TICK).z / MODEL_UNITS_PER_BLOCK))
                .add(0.0D, RAM_HEAD_CENTER_HEIGHT, 0.0D);
        AABB impactBox = createRamHeadBox(center, forward, right);
        Vec3 knockback = forward.scale(2.5D).add(0.0D, 0.25D, 0.0D);
        float baseDamage = (float) getBaseDamage();
        LivingEntity attacker = getOperator() instanceof LivingEntity living ? living : this;

        serverLevel.getEntities(this, impactBox.inflate(0.1D),
                        entity -> entity instanceof LivingEntity
                                && entity != attacker
                                && !hasPassenger(entity)
                                && !(entity instanceof Player player && player.isCreative())
        ).forEach(entity -> {
            entity.hurt(serverLevel.damageSources().mobAttack(attacker), baseDamage);
            entity.addDeltaMovement(knockback);
            entity.hurtMarked = true;
        });

        var impact = SiegeProfileCatalogs.ENGINES.forEntity(getType()).ramImpact();
        OrientedBox contact = new OrientedBox(center,
                new Vec3(impact.width() / 2.0D, impact.height() / 2.0D, RAM_HEAD_HALF_DEPTH),
                Rotation3.aroundY((float) -Math.toRadians(getVisualRotationYInDegrees())));
        Player breaker = SiegeBlockBreaker.responsiblePlayer(this);
        double speed = strikeSpeed();
        double energy = 0.5D * impact.mass() * speed * speed;
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(serverLevel, contact, energy, impact.spread(), breaker);
    }

    private Vec3 getHorizontalForward() {
        double yaw = Math.toRadians(getVisualRotationYInDegrees());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private static AABB createRamHeadBox(Vec3 center, Vec3 forward, Vec3 right) {
        double radiusX = Math.abs(right.x) * RAM_HEAD_HALF_WIDTH
                + Math.abs(forward.x) * RAM_HEAD_HALF_DEPTH;
        double radiusZ = Math.abs(right.z) * RAM_HEAD_HALF_WIDTH
                + Math.abs(forward.z) * RAM_HEAD_HALF_DEPTH;
        return new AABB(
                center.x - radiusX, center.y - RAM_HEAD_HALF_HEIGHT, center.z - radiusZ,
                center.x + radiusX, center.y + RAM_HEAD_HALF_HEIGHT, center.z + radiusZ);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "anim_controller", state -> PlayState.STOP)
                .triggerableAnim("attack", attackAnim));
    }

    @Override
    public void triggerAnimation(String name) {
        if ("attack".equals(name)) triggerAnim("anim_controller", "attack");
    }

    @Override
    public void stopAnimation(String name) {
        if ("attack".equals(name)) {
            //? if forge {
            /*stopTriggeredAnimation("anim_controller", "attack");
            *///?} else {
            stopTriggeredAnim("anim_controller", "attack");
            //?}
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return getPassengers().size() < MAX_PUSHERS && isSupportedDirectOperator(entity);
    }

    @Override
    protected boolean operatorControlsRotation(Entity passenger) {
        return passenger == getFirstPassenger() && isSupportedDirectOperator(passenger);
    }

    @Override
    protected boolean operatorControlsMovement(Entity passenger) {
        return shouldPassengerControlRotation(passenger);
    }

    @Override
    protected boolean operatorBodyFollowsEngine(Entity passenger) {
        return isSupportedDirectOperator(passenger);
    }

    @Override
    public float getPassengerViewYawLimit(Entity passenger) {
        return PUSHER_VIEW_LIMIT;
    }

    @Override
    public float getPassengerViewPitchLimit(Entity passenger) {
        return PUSHER_VIEW_LIMIT;
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        int seat = Math.max(0, Math.min(getPassengers().indexOf(entity), PUSHER_OFFSETS.length - 1));
        return PUSHER_OFFSETS[seat];
    }

    @Override
    public Vec3 getPlayerPOV() {
        return new Vec3(0.0, -0.7f, 0.0);
    }
}
