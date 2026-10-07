package me.mss1r.siegeworks.entity.siege;

import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.loading.AutomatedLoadingSession;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.loading.LoadingStatus;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
//? if forge {
/*import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
*///?} else {
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationController;
*///?} else {
import software.bernie.geckolib.animation.AnimationController;
//?}
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimatableManager;
*///?} else {
import software.bernie.geckolib.animation.AnimatableManager;
//?}
//? if forge {
/*import software.bernie.geckolib.core.object.PlayState;
*///?} else {
import software.bernie.geckolib.animation.PlayState;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Set;

public class HwachaEntity extends AbstractSiegeEntity implements GeoEntity, SiegeAmmunitionControl, MountedSiegeItemControl {
    public static final int CAPACITY = 70;
    public static final Vec3 BASE_PIVOT_PIXELS = new Vec3(0.0D, 9.975D, -3.0D);

    public float getRenderedModelPitch() {
        return -getRenderedAimPitch();
    }

    private static final float GUNNER_VIEW_OFFSET_DEGREES = 60.0F;

    private static final float TRAVEL_PITCH = 0.0F;
    private static final float TRAVEL_PITCH_RETURN_STEP = 1.5F;

    private static final double TOW_DISTANCE = 4.25D;

    private static final String TAG_LOADED_COUNT = "LoadedSingijeonCount";
    private static final String TAG_EXPLOSIVE_LOW = "ExplosiveSingijeonLow";
    private static final String TAG_EXPLOSIVE_HIGH = "ExplosiveSingijeonHigh";
    private static final String TAG_FIRING_INDEX = "FiringSingijeonIndex";
    private static final String TAG_NEXT_SHOT_TICKS = "NextSingijeonShotTicks";
    private static final int MANUAL_STAGE = 0;
    private static final int AUTOMATED_LOADING_STAGE = 0;
    private static final int AUTOMATED_PRIMING_STAGE = 1;
    private static final int VOLLEY_INTERVAL_TICKS = 2;
    private static final float PARKED_FALL_ACCELERATION_BASE = 0.02F;
    private static final float PARKED_FALL_ACCELERATION_PROGRESS = 0.06F;
    private static final float PARKED_FALL_MAX_SPEED = 1.0F;
    private static final float VOLLEY_YAW_SPREAD_DEGREES = 4.0F;
    private static final float VOLLEY_PITCH_SPREAD_DEGREES = 0.5F;
    private static final double VOLLEY_SPEED_SPREAD = 0.035D;
    private static final double GOLDEN_ANGLE_RADIANS = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final double AUTOMATED_TARGET_AREA_RADIUS = 6.0D;
    private static final int AUTOMATED_TARGET_PATTERN_SIZE = 11;
    private static final Set<SiegeAmmunitionMode> AMMUNITION_MODES = Set.of(
            SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD, SiegeAmmunitionMode.EXPLOSIVE);
    private static final EntityDataAccessor<Integer> LOADED_COUNT =
            SynchedEntityData.defineId(HwachaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> EXPLOSIVE_LOW =
            SynchedEntityData.defineId(HwachaEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> EXPLOSIVE_HIGH =
            SynchedEntityData.defineId(HwachaEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> FIRING_INDEX =
            SynchedEntityData.defineId(HwachaEntity.class, EntityDataSerializers.INT);

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withReloadSound(SoundEvents.ITEM_FRAME_ADD_ITEM)
            .withFiringSound(SoundEvents.FIREWORK_ROCKET_LAUNCH)
            .withMovementRange(28.0D)
            .withReloadRange(18.0D)
            .withFiringRange(256.0D)
            .withFiringVolume(1.2F);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private int pendingManualCount;
    private boolean pendingManualExplosive;
    private int pendingAutomatedNormal;
    private int pendingAutomatedExplosive;
    private int nextShotTicks;
    private float parkedPitchVelocity;
    private float previousCollisionModelPitch;

    public HwachaEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.055D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 230.0D);
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
        data.define(LOADED_COUNT, 0);
        data.define(EXPLOSIVE_LOW, 0L);
        data.define(EXPLOSIVE_HIGH, 0L);
        data.define(FIRING_INDEX, -1);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    protected boolean shouldPlayLoadingSoundOnStart() {
        return false;
    }

    @Override
    public boolean hasDifferentialDrive() {
        return true;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (isSupportedDirectOperator(passenger)) {
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
        return -32.5F;
    }

    @Override
    public float getMaxAimPitch() {
        return 15.0F;
    }

    @Override
    protected boolean usesIndependentAimYaw() {
        return false;
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return 8.0F;
    }

    @Override
    public Set<SiegeAmmunitionMode> getSupportedAmmunitionModes() {
        return AMMUNITION_MODES;
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (getFirstPassenger() != null && getFirstPassenger() != player) {
            return InteractionResult.FAIL;
        }
        if (continueLoadingAction(player)) {
            return showLoadingProgress(player);
        }
        if (isFiring()) {
            return InteractionResult.SUCCESS;
        }
        if (getCooldown() > 0) {
            return showCooldownProgress(player);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() && player.isShiftKeyDown() && getLoadedCount() > 0) {
            unloadLast(player);
            return InteractionResult.SUCCESS;
        }
        if (stack.isEmpty() && !player.isShiftKeyDown() && player.getVehicle() != this && canAddPassenger(player)) {
            player.setXRot(0.0F);
            setTrackedPitch(0.0F);
            setXRot(0.0F);
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }
        if (isRocketItem(stack.getItem())) {
            int freeSlots = CAPACITY - getLoadedCount();
            int amount = player.getAbilities().instabuild
                    ? freeSlots : Math.min(freeSlots, stack.getCount());
            if (amount <= 0) {
                player.displayClientMessage(Component.translatable("siege.hwacha.full"), true);
                return InteractionResult.SUCCESS;
            }

            pendingManualCount = amount;
            pendingManualExplosive = stack.is(SiegeworksItems.JUNG_SINGIJEON.get());
            LoadingRequirement stage = createRocketBatchStage(stack.getItem(), amount);
            return beginLoadingAction(player, hand, serverLevel, MANUAL_STAGE, stage,
                    getRocketBatchTicks(amount));
        }
        if (stack.is(Items.FLINT_AND_STEEL) && getLoadedCount() > 0) {
            return beginLoadingAction(player, hand, serverLevel, MANUAL_STAGE, createPrimingStage());
        }

        if (getLoadedCount() == 0) {
            player.displayClientMessage(Component.translatable("siege.loading.need_one_of",
                    SiegeworksItems.SO_SINGIJEON.get().getDefaultInstance().getHoverName().getString()
                            + ", "
                            + SiegeworksItems.JUNG_SINGIJEON.get().getDefaultInstance().getHoverName().getString()),
                    true);
        } else {
            player.displayClientMessage(Component.translatable("siege.hwacha.ignite"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        if (getFirstPassenger() != player) return false;
        ItemStack stack = player.getItemInHand(hand);
        return hasLoadingAction()
                ? stack.is(getActiveLoadingItem())
                : isRocketItem(stack.getItem()) || stack.is(Items.FLINT_AND_STEEL);
    }

    @Override
    public InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel) {
        return handleSiegeInteraction(player, hand, serverLevel);
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        Item activeItem = getActiveLoadingItem();
        if (activeItem == Items.FLINT_AND_STEEL) {
            if (consumeLoadingRequirement(player, hand, createPrimingStage())) {
                playIgnitionSound(serverLevel);
                startVolley(player);
            }
            return;
        }

        if (!isRocketItem(activeItem) || pendingManualCount <= 0) {
            return;
        }
        pendingManualCount = 0;
    }

    @Override
    protected void onLoadingActionTick(ServerLevel serverLevel, Player player, InteractionHand hand,
                                       int elapsedTicks, int totalTicks) {
        if (pendingManualCount <= 0 || !isRocketItem(getActiveLoadingItem())
                || !isRocketInsertionTick(elapsedTicks)) {
            return;
        }

        Item expectedItem = pendingManualExplosive
                ? SiegeworksItems.JUNG_SINGIJEON.get()
                : SiegeworksItems.SO_SINGIJEON.get();
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(expectedItem)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        appendAmmunition(1, pendingManualExplosive);
        pendingManualCount--;
        setOperator(player);
        playRocketInsertionSound(serverLevel);
    }

    @Override
    protected void onLoadingActionCancelled() {
        pendingManualCount = 0;
        pendingManualExplosive = false;
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel) || !isOperator(operator)) {
            automatedLoading.reset();
            return SiegeActionResult.DENIED;
        }
        if (hasLoadingAction() || getCooldown() > 0 || isFiring()) {
            return SiegeActionResult.IN_PROGRESS;
        }

        if (automatedLoading.isStage(AUTOMATED_LOADING_STAGE) || getLoadedCount() == 0) {
            return advanceAutomatedLoading(operator, inventory);
        }
        return advanceAutomatedPriming(operator, inventory);
    }

    private SiegeActionResult advanceAutomatedLoading(LivingEntity operator, Container inventory) {
        if (automatedLoading.needsStart(operator, AUTOMATED_LOADING_STAGE)) {
            prepareAutomatedBatch(inventory);
            int total = pendingAutomatedNormal + pendingAutomatedExplosive;
            if (total <= 0) {
                automatedLoading.reset();
                return SiegeActionResult.MISSING_AMMUNITION;
            }
            automatedLoading.start(operator, AUTOMATED_LOADING_STAGE, getRocketBatchTicks(total));
            if (level() instanceof ServerLevel serverLevel) {
                onLoadingStart(serverLevel);
            }
        }
        boolean complete = automatedLoading.tickComplete();
        if (isRocketInsertionTick(automatedLoading.elapsedTicks())
                && pendingAutomatedNormal + pendingAutomatedExplosive > 0
                && !insertNextAutomatedRocket(operator, inventory)) {
            automatedLoading.reset();
            clearPendingAutomatedAmmunition();
            return SiegeActionResult.MISSING_AMMUNITION;
        }
        if (!complete) {
            return SiegeActionResult.IN_PROGRESS;
        }
        clearPendingAutomatedAmmunition();
        automatedLoading.reset();
        setOperator(operator);
        return SiegeActionResult.LOADED;
    }

    private SiegeActionResult advanceAutomatedPriming(LivingEntity operator, Container inventory) {
        LoadingRequirement stage = createPrimingStage();
        if (!containsItem(inventory, Items.FLINT_AND_STEEL)) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }
        if (automatedLoading.needsStart(operator, AUTOMATED_PRIMING_STAGE)) {
            automatedLoading.start(operator, AUTOMATED_PRIMING_STAGE,
                    getLoadingRequirementTicks(stage.timingKey()));
        }
        if (!automatedLoading.tickComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (!damageItem(inventory, Items.FLINT_AND_STEEL, operator)) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        automatedLoading.reset();
        playIgnitionSound((ServerLevel) level());
        startVolley(operator);
        return SiegeActionResult.FIRED;
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        automatedLoading.cancel(operator);
    }

    @Override
    public SiegeOperationState getOperationState() {
        if (hasLoadingAction() || automatedLoading.isActive()) {
            return SiegeOperationState.LOADING;
        }
        if (isFiring() || getCooldown() > 0) {
            return SiegeOperationState.COOLDOWN;
        }
        return getLoadedCount() > 0 ? SiegeOperationState.READY : SiegeOperationState.IDLE;
    }

    @Override
    public boolean isReadyToFire() {
        return getLoadedCount() > 0 && !isFiring() && getCooldown() <= 0;
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        if (isFiring()) {
            tickVolley(serverLevel);
            return;
        }
        if (getPassengers().isEmpty()) {
            approachParkedPitch();
            return;
        }

        parkedPitchVelocity = 0.0F;
        if (isTowed()) {
            approachTravelPitch();
        }
    }

    @Override
    public void tick() {
        previousCollisionModelPitch = -getTrackedPitch();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(-getTrackedPitch());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionModelPitch);
    }

    private static List<CollisionGroup> collisionGroups(float modelPitch) {
        CollisionPose pose = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.HWACHA_BODY.pivot(),
                (float) Math.toRadians(modelPitch));
        return List.of(new CollisionGroup("body", GeneratedCollisionShapes.HWACHA_BODY, pose));
    }

    private void approachTravelPitch() {
        float next = Mth.approach(getTrackedPitch(), TRAVEL_PITCH, TRAVEL_PITCH_RETURN_STEP);
        setTrackedPitch(next);
        setXRot(next);
        lastRiderPitch = next;
    }

    private void approachParkedPitch() {
        float current = getTrackedPitch();
        float parked = getMinAimPitch();
        float range = Math.max(1.0F, getMaxAimPitch() - parked);
        float progress = Mth.clamp((getMaxAimPitch() - current) / range, 0.0F, 1.0F);
        float acceleration = PARKED_FALL_ACCELERATION_BASE
                + PARKED_FALL_ACCELERATION_PROGRESS * progress * progress;
        parkedPitchVelocity = Math.min(PARKED_FALL_MAX_SPEED, parkedPitchVelocity + acceleration);
        float next = Math.max(parked, current - parkedPitchVelocity);
        if (next <= parked) {
            parkedPitchVelocity = 0.0F;
        }
        setTrackedPitch(next);
        setXRot(getTrackedPitch());
        lastRiderPitch = getTrackedPitch();
    }

    private void startVolley(LivingEntity operator) {
        if (getLoadedCount() <= 0 || isFiring()) {
            return;
        }
        entityData.set(FIRING_INDEX, 0);
        nextShotTicks = 0;
        setOperator(operator);
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        if (isFiring()) {
            return false;
        }
        if (getLoadedCount() == 0) {
            setLoadedCount(CAPACITY);
        }
        playIgnitionSound(serverLevel);
        startVolley(operator);
        return true;
    }

    private void tickVolley(ServerLevel serverLevel) {
        if (nextShotTicks-- > 0) {
            return;
        }
        int index = getFiringIndex();
        if (index < 0 || index >= getLoadedCount()) {
            finishVolley();
            return;
        }

        fireSingijeon(serverLevel, index);
        entityData.set(FIRING_INDEX, index + 1);
        nextShotTicks = VOLLEY_INTERVAL_TICKS - 1;
        if (index + 1 >= getLoadedCount()) {
            finishVolley();
        }
    }

    private void fireSingijeon(ServerLevel serverLevel, int index) {
        boolean explosive = isExplosive(index);
        Vec3 mouth = getSlotLaunchPosition(index);
        ShotTrajectory trajectory = calculateProjectileTrajectory(index);
        Vec3 direction = trajectory.direction();
        SingijeonProjectile projectile = new SingijeonProjectile(
                SiegeworksEntities.SINGIJEON_PROJECTILE.get(), this, serverLevel);
        projectile.setExplosive(explosive);
        projectile.setPos(mouth.x, mouth.y, mouth.z);
        projectile.launchWithMotor(direction, trajectory.speedMultiplier());
        projectile.setBaseDamage(getBaseDamage());
        projectile.setOwner(this);
        serverLevel.addFreshEntity(projectile);

        spawnLaunchPlume(serverLevel, mouth, direction);

        serverLevel.playSound(null, mouth.x, mouth.y, mouth.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS,
                1.15F, 0.92F + random.nextFloat() * 0.16F);
    }

    private void spawnLaunchPlume(ServerLevel serverLevel, Vec3 mouth, Vec3 direction) {
        SiegeParticleEffects.rocketLaunch(serverLevel, mouth.add(direction.scale(0.08D)), direction);
    }

    private ShotTrajectory calculateProjectileTrajectory(int index) {
        float accuracy = Math.max(0.0F, getAccuracyMultiplier());
        int volleySize = Math.max(1, getLoadedCount());
        double radius = volleySize == 1
                ? 0.0D
                : Math.sqrt((double) index / (volleySize - 1));
        double angle = index * GOLDEN_ANGLE_RADIANS;
        double lateralOffset = Math.cos(angle) * radius;
        double depthOffset = Math.sin(angle) * radius;
        float yawJitter = (random.nextFloat() - 0.5F) * 0.3F * accuracy;
        float pitchJitter = (random.nextFloat() - 0.5F) * 0.24F * accuracy;
        float yaw = getVisualRotationYInDegrees()
                + (float) lateralOffset * VOLLEY_YAW_SPREAD_DEGREES + yawJitter;
        float pitch = getTrackedPitch()
                - (float) depthOffset * VOLLEY_PITCH_SPREAD_DEGREES + pitchJitter;
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        Vec3 direction = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
        double speedJitter = (random.nextDouble() - 0.5D) * 0.006D * accuracy;
        double speedMultiplier = 1.0D + depthOffset * VOLLEY_SPEED_SPREAD + speedJitter;
        return new ShotTrajectory(direction, speedMultiplier);
    }

    private record ShotTrajectory(Vec3 direction, double speedMultiplier) {
    }

    private Vec3 getSlotLaunchPosition(int index) {
        return getSlotLaunchPosition(index, getTrackedPitch());
    }

    private Vec3 getSlotLaunchPosition(int index, float atPitch) {
        Vec3 slot = getAmmunitionSlot(index);
        Vec3 nose = new Vec3(slot.x - 0.5D, slot.y + 0.5D, -18.0D);
        Vec3 pitched = rotateAroundLocalX(nose, BASE_PIVOT_PIXELS, -atPitch).scale(1.0D / 16.0D);
        return localModelOffsetToWorld(pitched);
    }

    private Vec3 rotateAroundLocalX(Vec3 point, Vec3 pivot, float degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double dy = point.y - pivot.y;
        double dz = point.z - pivot.z;
        return new Vec3(point.x,
                pivot.y + dy * cos - dz * sin,
                pivot.z + dy * sin + dz * cos);
    }

    private Vec3 localModelOffsetToWorld(Vec3 localBlocks) {
        double radians = Math.toRadians(-getVisualRotationYInDegrees() - 180.0D);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double x = localBlocks.x * cos + localBlocks.z * sin;
        double z = -localBlocks.x * sin + localBlocks.z * cos;
        return new Vec3(getX() + x, getY() + localBlocks.y, getZ() + z);
    }

    private void finishVolley() {
        setLoadedCount(0);
        entityData.set(EXPLOSIVE_LOW, 0L);
        entityData.set(EXPLOSIVE_HIGH, 0L);
        entityData.set(FIRING_INDEX, -1);
        nextShotTicks = 0;
        setAmmoLoaded("");
        startRecovery();
    }

    private void appendAmmunition(int amount, boolean explosive) {
        int added = Math.min(Math.max(0, amount), CAPACITY - getLoadedCount());
        for (int i = 0; i < added; i++) {
            int slot = getLoadedCount();
            setExplosive(slot, explosive);
            setLoadedCount(slot + 1);
        }
        if (getLoadedCount() > 0) {
            setAmmoLoaded("siegeworks:singijeon");
        }
    }

    private void unloadLast(Player player) {
        int slot = getLoadedCount() - 1;
        boolean explosive = isExplosive(slot);
        ItemStack returned = new ItemStack(explosive
                ? SiegeworksItems.JUNG_SINGIJEON.get()
                : SiegeworksItems.SO_SINGIJEON.get());
        setExplosive(slot, false);
        setLoadedCount(slot);
        if (getLoadedCount() == 0) {
            setAmmoLoaded("");
        }
        if (!player.addItem(returned)) {
            player.drop(returned, false);
        }
    }

    private void prepareAutomatedBatch(Container inventory) {
        int capacity = CAPACITY - getLoadedCount();
        pendingAutomatedNormal = 0;
        pendingAutomatedExplosive = 0;
        if (capacity <= 0) {
            return;
        }

        SiegeAmmunitionMode mode = getAutomatedAmmunitionMode();
        if (mode != SiegeAmmunitionMode.EXPLOSIVE) {
            pendingAutomatedNormal = Math.min(capacity, countItems(inventory, SiegeworksItems.SO_SINGIJEON.get()));
            capacity -= pendingAutomatedNormal;
        }
        if (capacity > 0 && mode != SiegeAmmunitionMode.STANDARD) {
            pendingAutomatedExplosive = Math.min(capacity,
                    countItems(inventory, SiegeworksItems.JUNG_SINGIJEON.get()));
        }
    }

    private LoadingRequirement createRocketBatchStage(Item item, int amount) {
        return LoadingRequirement.consume(item, amount)
                .withFeedback(LoadingStatus.LOADING_AMMUNITION.translationKey())
                .timedBy("rocket");
    }

    private int getRocketBatchTicks(int amount) {
        int perRocket = getRocketLoadingIntervalTicks();
        return Math.max(1, 8 + Math.max(0, amount - 1) * perRocket);
    }

    private int getRocketLoadingIntervalTicks() {
        return getLoadingRequirementTicks("rocket");
    }

    private boolean isRocketInsertionTick(int elapsedTicks) {
        return elapsedTicks >= 8 && (elapsedTicks - 8) % getRocketLoadingIntervalTicks() == 0;
    }

    private void playRocketInsertionSound(ServerLevel serverLevel) {
        serverLevel.playSound(null, getX(), getY() + 1.0D, getZ(),
                SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS,
                0.62F, 0.9F + random.nextFloat() * 0.2F);
    }

    private boolean insertNextAutomatedRocket(LivingEntity operator, Container inventory) {
        boolean explosive = pendingAutomatedNormal <= 0;
        Item item = explosive
                ? SiegeworksItems.JUNG_SINGIJEON.get()
                : SiegeworksItems.SO_SINGIJEON.get();
        if (!consumeItems(inventory, item, 1)) {
            return false;
        }
        if (explosive) {
            pendingAutomatedExplosive--;
        } else {
            pendingAutomatedNormal--;
        }
        appendAmmunition(1, explosive);
        setOperator(operator);
        playRocketInsertionSound((ServerLevel) level());
        return true;
    }

    private void clearPendingAutomatedAmmunition() {
        pendingAutomatedNormal = 0;
        pendingAutomatedExplosive = 0;
    }

    private void playIgnitionSound(ServerLevel serverLevel) {
        serverLevel.playSound(null, getX(), getY() + 1.0D, getZ(),
                SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                0.9F, 0.95F + random.nextFloat() * 0.1F);
    }

    private LoadingRequirement createPrimingStage() {
        return LoadingRequirement.tool(Items.FLINT_AND_STEEL)
                .withFeedback(LoadingStatus.PRIMING.translationKey())
                .timedBy("priming");
    }

    private static boolean isRocketItem(Item item) {
        return item == SiegeworksItems.SO_SINGIJEON.get() || item == SiegeworksItems.JUNG_SINGIJEON.get();
    }

    private static int countItems(Container inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean containsItem(Container inventory, Item item) {
        return countItems(inventory, item) > 0;
    }

    private static boolean consumeItems(Container inventory, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countItems(inventory, item) < amount) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            remaining -= consumed;
        }
        inventory.setChanged();
        return true;
    }

    private static boolean damageItem(Container inventory, Item item, LivingEntity operator) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.damageHeldItem(
                        stack, 1, operator, InteractionHand.MAIN_HAND);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    public int getLoadedCount() {
        return entityData.get(LOADED_COUNT);
    }

    private void setLoadedCount(int count) {
        entityData.set(LOADED_COUNT, Mth.clamp(count, 0, CAPACITY));
    }

    public int getFiringIndex() {
        return entityData.get(FIRING_INDEX);
    }

    public boolean isFiring() {
        return getFiringIndex() >= 0;
    }

    public boolean isExplosive(int slot) {
        if (slot < 0 || slot >= CAPACITY) {
            return false;
        }
        if (slot < Long.SIZE) {
            return (entityData.get(EXPLOSIVE_LOW) & 1L << slot) != 0L;
        }
        return (entityData.get(EXPLOSIVE_HIGH) & 1L << (slot - Long.SIZE)) != 0L;
    }

    private void setExplosive(int slot, boolean explosive) {
        EntityDataAccessor<Long> accessor = slot < Long.SIZE ? EXPLOSIVE_LOW : EXPLOSIVE_HIGH;
        int bit = slot < Long.SIZE ? slot : slot - Long.SIZE;
        long mask = entityData.get(accessor);
        mask = explosive ? mask | 1L << bit : mask & ~(1L << bit);
        entityData.set(accessor, mask);
    }

    public static Vec3 getAmmunitionSlot(int index) {
        int clamped = Mth.clamp(index, 0, CAPACITY - 1);
        int row;
        int column;
        if (clamped < 10) {
            row = 0;
            column = clamped;
            double x = column < 5 ? -10.4D + column * 2.0D : 3.4D + (column - 5) * 2.0D;
            return new Vec3(x, 32.1D, -5.0D);
        }
        int remaining = clamped - 10;
        row = 1 + remaining / 12;
        column = remaining % 12;
        double x = column < 6
                ? -10.4D + column * 2.0D
                : 1.4D + (column - 6) * 2.0D;
        if (row == 5 && column == 6) {
            x = 1.5D;
        }
        double y = row == 5 ? 41.9D : 32.1D + row * 2.0D;
        return new Vec3(x, y, -5.0D);
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return getSlotLaunchPosition(Math.max(0, getLoadedCount() / 2), 0.0F);
    }

    @Override
    public float calculateAutomatedAimPitch(Vec3 target) {
        ProjectilePhysicsProfile rocket = SingijeonProjectile.physics(isExplosive(Math.max(0, getLoadedCount() / 2)));
        return SiegeBallistics.calculateLowAnglePitch(getAutomatedAimOrigin(), target,
                SingijeonProjectile.launchSpeed(rocket), SingijeonProjectile.flight(rocket));
    }

    @Override
    public Vec3 resolveAutomatedAimTarget(Vec3 commandedTarget, int shotSequence) {
        int sequence = Math.floorMod(shotSequence, 9_973);
        int patternIndex = sequence % AUTOMATED_TARGET_PATTERN_SIZE;
        double radius = AUTOMATED_TARGET_AREA_RADIUS
                * Math.sqrt((patternIndex + 0.5D) / AUTOMATED_TARGET_PATTERN_SIZE);
        double phase = (getUUID().hashCode() & 0xFFFF) / 65_536.0D * Math.PI * 2.0D;
        double angle = phase + sequence * GOLDEN_ANGLE_RADIANS;
        return commandedTarget.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    @Override
    public double getAutomatedTargetSpreadRadius() {
        return AUTOMATED_TARGET_AREA_RADIUS;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(TOW_DISTANCE);
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(19.0D / 16.0D, 0.0D, 12.0D / 16.0D);
    }

    @Override
    protected float operatorViewOffsetDegrees() {
        return GUNNER_VIEW_OFFSET_DEGREES;
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public void triggerAnimation(String animationName) {
    }

    @Override
    public void stopAnimation(String animationName) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_LOADED_COUNT, getLoadedCount());
        tag.putLong(TAG_EXPLOSIVE_LOW, entityData.get(EXPLOSIVE_LOW));
        tag.putLong(TAG_EXPLOSIVE_HIGH, entityData.get(EXPLOSIVE_HIGH));
        tag.putInt(TAG_FIRING_INDEX, getFiringIndex());
        tag.putInt(TAG_NEXT_SHOT_TICKS, nextShotTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setLoadedCount(tag.getInt(TAG_LOADED_COUNT));
        entityData.set(EXPLOSIVE_LOW, tag.getLong(TAG_EXPLOSIVE_LOW));
        entityData.set(EXPLOSIVE_HIGH, tag.getLong(TAG_EXPLOSIVE_HIGH));
        entityData.set(FIRING_INDEX, tag.contains(TAG_FIRING_INDEX) ? tag.getInt(TAG_FIRING_INDEX) : -1);
        nextShotTicks = Math.max(0, tag.getInt(TAG_NEXT_SHOT_TICKS));
        setAmmoLoaded(getLoadedCount() > 0 ? "siegeworks:singijeon" : "");
    }
}
