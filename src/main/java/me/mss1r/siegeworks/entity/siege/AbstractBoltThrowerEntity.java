package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.entity.projectile.AbstractBoltProjectile;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

import java.util.UUID;

public abstract class AbstractBoltThrowerEntity extends AbstractSiegeEntity implements GeoEntity, SiegeArtilleryControl,
        MountedSiegeItemControl {
    private static final String STATE_ANIMATION_CONTROLLER = "state_controller";
    private static final String ACTION_ANIMATION_CONTROLLER = "action_controller";
    private static final float PARKED_FALL_ACCELERATION_BASE = 0.025F;
    private static final float PARKED_FALL_ACCELERATION_PROGRESS = 0.055F;
    private static final float PARKED_FALL_MAX_SPEED = 0.9F;
    private static final double PIXELS_TO_BLOCKS = 1.0D / 16.0D;
    private static final int AIM_COLLISION_SEARCH_STEPS = 10;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation shootAnim = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final RawAnimation reloadAnim = RawAnimation.begin().thenPlayAndHold("reloading");
    private final RawAnimation loadedAnim = RawAnimation.begin().thenPlayAndHold("loaded");
    private final RawAnimation unloadedAnim = RawAnimation.begin().thenPlayAndHold("unloaded");
    private UUID automatedLoaderUuid;
    private int automatedLoadingTicks;
    private float parkedPitchVelocity;
    private transient float clientParkedPitchVelocity;
    private transient boolean clientYawAimInitialized;
    private transient float clientAimTargetYaw;
    private transient float clientPredictedAimYaw;
    private transient boolean clientPitchAimInitialized;
    private transient float clientAimTargetPitch;
    private transient float clientPredictedAimPitch;
    private float previousCollisionAimPitch;
    private float previousCollisionRelativeYaw;

    protected AbstractBoltThrowerEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return getPassengers().isEmpty() && isSupportedDirectOperator(entity);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide) {
            clientYawAimInitialized = false;
            initializeClientPitchAim();
            clientAimTargetPitch = clientPredictedAimPitch;
            clientParkedPitchVelocity = 0.0F;
        }
    }

    @Override
    public void tick() {
        previousCollisionAimPitch = getTrackedPitch();
        previousCollisionRelativeYaw = currentCollisionRelativeYaw();
        super.tick();
        if (level().isClientSide) {
            tickClientPitchState();
        }
        StructureMotionSystem.tickStructure(this);
    }

    protected final float previousCollisionModelPitch() {
        return toModelPitch(previousCollisionAimPitch);
    }

    protected final float currentCollisionRelativeYaw() {
        return Mth.wrapDegrees(getVisualRotationYInDegrees() - getTrackedYaw());
    }

    protected final float previousCollisionRelativeYaw() {
        return previousCollisionRelativeYaw;
    }

    @Override
    public void setTrackedPitch(float pitch) {
        this.entityData.set(TRACKED_PITCH, resolveAimPitch(getTrackedPitch(), pitch));
    }

    protected float resolveAimPitch(float currentPitch, float requestedPitch) {
        float targetPitch = Mth.clamp(requestedPitch, getMinAimPitch(), getMaxAimPitch());
        if (Math.abs(targetPitch - currentPitch) < 0.0001F || isAimGeometryClear(targetPitch)) {
            return targetPitch;
        }
        if (!isAimGeometryClear(currentPitch)) {
            return targetPitch;
        }

        float validPitch = currentPitch;
        float blockedPitch = targetPitch;
        for (int iteration = 0; iteration < AIM_COLLISION_SEARCH_STEPS; iteration++) {
            float candidate = (validPitch + blockedPitch) * 0.5F;
            if (isAimGeometryClear(candidate)) {
                validPitch = candidate;
            } else {
                blockedPitch = candidate;
            }
        }
        return validPitch;
    }

    protected abstract boolean isAimGeometryClear(float pitch);

    public abstract float getMinAimPitch();

    public abstract float getMaxAimPitch();

    @Override
    protected boolean usesIndependentAimYaw() {
        return true;
    }

    @Override
    protected abstract float getYawTurnSpeedDegrees();

    @Override
    protected boolean usesIndependentAimPitch() {
        return true;
    }

    @Override
    protected abstract float getPitchTurnSpeedDegrees();

    @Override
    public boolean shouldPredictYawOnClient() {
        return usesIndependentAimYaw();
    }

    public void applyClientAimInput(float yawDelta, float pitchDelta) {
        if (!level().isClientSide) {
            return;
        }

        if (usesIndependentAimYaw()) {
            initializeClientYawAim();
            float pendingYaw = Mth.wrapDegrees(clientAimTargetYaw - clientPredictedAimYaw) + yawDelta;
            clientAimTargetYaw = Mth.wrapDegrees(clientPredictedAimYaw
                    + Mth.clamp(pendingYaw, -getYawTurnSpeedDegrees(), getYawTurnSpeedDegrees()));
        }

        if (usesIndependentAimPitch()) {
            initializeClientPitchAim();
            float pendingPitch = clientAimTargetPitch - clientPredictedAimPitch + pitchDelta;
            float requestedPitch = clientPredictedAimPitch
                    + Mth.clamp(pendingPitch, -getPitchTurnSpeedDegrees(), getPitchTurnSpeedDegrees());
            clientAimTargetPitch = resolveAimPitch(clientPredictedAimPitch, requestedPitch);
        }
    }

    public float getClientAimTargetYaw() {
        initializeClientYawAim();
        return clientAimTargetYaw;
    }

    public float getClientAimTargetPitch() {
        initializeClientPitchAim();
        return clientAimTargetPitch;
    }

    public final float getRenderedModelPitch() {
        return toModelPitch(getRenderedAimPitch());
    }

    protected final float toModelPitch(float aimPitch) {
        return -aimPitch;
    }

    private void initializeClientYawAim() {
        if (clientYawAimInitialized) {
            return;
        }
        clientPredictedAimYaw = getTrackedYaw();
        clientAimTargetYaw = clientPredictedAimYaw;
        clientYawAimInitialized = true;
    }

    private void initializeClientPitchAim() {
        if (clientPitchAimInitialized) {
            return;
        }
        clientPredictedAimPitch = getTrackedPitch();
        clientAimTargetPitch = clientPredictedAimPitch;
        clientPitchAimInitialized = true;
    }

    @Override
    protected float getPassengerAimTargetYaw(Player player) {
        return level().isClientSide && player.isLocalPlayer()
                ? getClientAimTargetYaw()
                : super.getPassengerAimTargetYaw(player);
    }

    @Override
    protected float getPassengerAimTargetPitch(Player player) {
        return level().isClientSide && player.isLocalPlayer()
                ? getClientAimTargetPitch()
                : super.getPassengerAimTargetPitch(player);
    }

    @Override
    public void turnTowardsYaw(float targetYaw) {
        if (!usesIndependentAimYaw()) {
            super.turnTowardsYaw(targetYaw);
            return;
        }

        if (level().isClientSide) {
            float delta = Mth.wrapDegrees(targetYaw - clientPredictedAimYaw);
            clientPredictedAimYaw = Mth.wrapDegrees(clientPredictedAimYaw
                    + Mth.clamp(delta, -getYawTurnSpeedDegrees(), getYawTurnSpeedDegrees()));
            lastRiderYaw = clientPredictedAimYaw;
            return;
        }

        float currentYaw = getTrackedYaw();
        float delta = Mth.wrapDegrees(targetYaw - currentYaw);
        float maxStep = getYawTurnSpeedDegrees();
        setTrackedYaw(currentYaw + Mth.clamp(delta, -maxStep, maxStep));
        lastRiderYaw = getTrackedYaw();
    }

    @Override
    public void turnTowardsPitch(float targetPitch) {
        if (!level().isClientSide) {
            super.turnTowardsPitch(targetPitch);
            return;
        }

        float delta = targetPitch - clientPredictedAimPitch;
        float requestedPitch = clientPredictedAimPitch
                + Mth.clamp(delta, -getPitchTurnSpeedDegrees(), getPitchTurnSpeedDegrees());
        clientPredictedAimPitch = resolveAimPitch(clientPredictedAimPitch, requestedPitch);
        lastRiderPitch = clientPredictedAimPitch;
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        super.onSiegeTick(serverLevel);
        if (!shouldReturnToParkedPitch()) {
            parkedPitchVelocity = 0.0F;
            return;
        }

        ParkedPitchStep step = advanceParkedPitch(getTrackedPitch(), parkedPitchVelocity);
        parkedPitchVelocity = step.velocity();
        setTrackedPitch(step.pitch());
        float appliedPitch = getTrackedPitch();
        if (Math.abs(appliedPitch - step.pitch()) > 0.0001F) {
            parkedPitchVelocity = 0.0F;
        }
        setXRot(appliedPitch);
        lastRiderPitch = appliedPitch;
    }

    private void tickClientPitchState() {
        initializeClientPitchAim();
        if (shouldReturnToParkedPitch()) {
            ParkedPitchStep step = advanceParkedPitch(clientPredictedAimPitch, clientParkedPitchVelocity);
            clientPredictedAimPitch = step.pitch();
            clientAimTargetPitch = clientPredictedAimPitch;
            clientParkedPitchVelocity = step.velocity();
            return;
        }

        clientParkedPitchVelocity = 0.0F;
        if (!(getFirstPassenger() instanceof Player player) || !player.isLocalPlayer()) {
            clientPredictedAimPitch = getTrackedPitch();
            clientAimTargetPitch = clientPredictedAimPitch;
        }
    }

    protected boolean shouldReturnToParkedPitch() {
        return getPassengers().isEmpty();
    }

    private ParkedPitchStep advanceParkedPitch(float currentPitch, float velocity) {
        float targetPitch = getParkedAimPitch();
        float delta = targetPitch - currentPitch;
        if (Math.abs(delta) < 0.0001F) {
            return new ParkedPitchStep(targetPitch, 0.0F);
        }

        float range = Math.max(1.0F, getMaxAimPitch() - getMinAimPitch());
        float progress = delta > 0.0F
                ? Mth.clamp((currentPitch - getMinAimPitch()) / range, 0.0F, 1.0F)
                : Mth.clamp((getMaxAimPitch() - currentPitch) / range, 0.0F, 1.0F);
        float nextVelocity = Math.min(PARKED_FALL_MAX_SPEED,
                velocity + PARKED_FALL_ACCELERATION_BASE
                        + PARKED_FALL_ACCELERATION_PROGRESS * progress * progress);
        float requestedPitch = currentPitch
                + Math.copySign(Math.min(Math.abs(delta), nextVelocity), delta);
        float resolvedPitch = resolveAimPitch(currentPitch, requestedPitch);
        if (Math.abs(targetPitch - resolvedPitch) < 0.0001F
                || Math.abs(resolvedPitch - currentPitch) < 0.0001F) {
            nextVelocity = 0.0F;
        }
        return new ParkedPitchStep(resolvedPitch, nextVelocity);
    }

    protected abstract float getParkedAimPitch();

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (getFirstPassenger() != null && getFirstPassenger() != player) return InteractionResult.FAIL;
        if (continueLoadingAction(player)) return showLoadingProgress(player);
        if (getCooldown() > 0) return showCooldownProgress(player);

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() && !player.isShiftKeyDown() && player.getVehicle() != this && canAddPassenger(player)) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (stack.isEmpty() && player.isShiftKeyDown() && hasAmmoLoaded() && isWindingComplete()) {
            return cycleShotPower(player);
        }

        if (!hasAmmoLoaded()) {
            LoadingRequirement stage = getLoadStages()[0];
            if (!stack.is(getBoltItem())) {
                player.displayClientMessage(Component.translatable("siege.loading.next",
                        getBoltItem().getDefaultInstance().getHoverName()), true);
                return InteractionResult.SUCCESS;
            }

            if (!canBeginLoadingRequirement(player, hand, stage)) return InteractionResult.SUCCESS;
            return beginLoadingAction(player, hand, serverLevel, 0, stage);
        }

        if (!isWindingComplete()) {
            return showWindingProgress(player);
        }

        if (stack.isEmpty() || stack.is(getBoltItem())) {
            player.displayClientMessage(Component.translatable(getFireHintTranslationKey()), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        return (!hasAmmoLoaded() || hasLoadingAction())
                && acceptsMountedLoadingItem(player, hand, getLoadStages());
    }

    @Override
    public InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel) {
        return handleSiegeInteraction(player, hand, serverLevel);
    }

    public void requestRiderFire(Player player) {
        if (!(level() instanceof ServerLevel serverLevel) || getFirstPassenger() != player || isBeingCaptured()) {
            return;
        }

        if (hasLoadingAction()) {
            showLoadingProgress(player);
            return;
        }

        if (getCooldown() > 0) {
            showCooldownProgress(player);
            return;
        }

        if (!isWindingComplete()) {
            showWindingProgress(player);
            return;
        }

        if (!hasAmmoLoaded()) {
            player.displayClientMessage(Component.translatable("siege.loading.next",
                    getBoltItem().getDefaultInstance().getHoverName()), true);
            return;
        }

        fireBolt(serverLevel, player);
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel serverLevel) || !isOperator(operator)) {
            resetAutomatedLoading();
            return SiegeActionResult.DENIED;
        }
        if (hasLoadingAction() || getCooldown() > 0 || !isWindingComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (hasAmmoLoaded()) {
            fireBolt(serverLevel, operator);
            return SiegeActionResult.FIRED;
        }

        if (!containsItem(inventory, getBoltItem())) {
            resetAutomatedLoading();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        if (!operator.getUUID().equals(automatedLoaderUuid)) {
            automatedLoaderUuid = operator.getUUID();
            LoadingRequirement stage = getLoadStages()[0];
            automatedLoadingTicks = getLoadingRequirementTicks(stage.timingKey());
            onLoadingStart(serverLevel);
            if (shouldPlayLoadingSoundOnStart()) {
                playReloadSound(serverLevel);
            }
        }

        if (--automatedLoadingTicks > 0) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (!consumeItem(inventory, getBoltItem())) {
            resetAutomatedLoading();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(getBoltItem());
        setAmmoLoaded(ammoId.toString());
        setWindingTime(getWindingDurationTicks());
        setOperator(operator);
        resetAutomatedLoading();
        return SiegeActionResult.LOADED;
    }

    @Override
    public SiegeOperationState getOperationState() {
        return automatedLoadingTicks > 0 ? SiegeOperationState.LOADING : super.getOperationState();
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        if (operator.getUUID().equals(automatedLoaderUuid)) {
            resetAutomatedLoading();
        }
    }

    private static boolean containsItem(Container inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeItem(Container inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private void resetAutomatedLoading() {
        automatedLoaderUuid = null;
        automatedLoadingTicks = 0;
    }

    @Override
    public void onLoadingStart(ServerLevel serverLevel) {
    }

    @Override
    protected boolean shouldPlayLoadingSoundOnStart() {
        return false;
    }

    @Override
    protected boolean shouldLoopReloadSoundDuringVisualReload() {
        return true;
    }

    @Override
    protected int getVisualReloadSoundIntervalTicks() {
        return getWindingDurationTicks();
    }

    @Override
    protected void onWindingComplete(ServerLevel serverLevel) {
        playSoundToNearbyPlayers(serverLevel, SiegeworksSounds.BALLISTA_LOCK.get(),
                getSoundProfile().reload().range(), 1.0F);
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        LoadingRequirement stage = getLoadStages()[0];
        if (!consumeLoadingRequirement(player, hand, stage)) return;

        ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(getBoltItem());
        setAmmoLoaded(ammoId.toString());
        setWindingTime(getWindingDurationTicks());
        setOperator(player);
    }

    protected abstract LoadingRequirement[] getLoadStages();

    protected abstract Item getBoltItem();

    protected abstract EntityType<? extends AbstractBoltProjectile> getBoltType();

    protected abstract AbstractBoltProjectile createBolt(ServerLevel serverLevel);

    protected abstract String getFireHintTranslationKey();

    protected void fireBolt(ServerLevel serverLevel, LivingEntity operator) {
        triggerAnimation("shoot");
        playShootSound(serverLevel);

        AbstractBoltProjectile projectile = createBolt(serverLevel);
        Vec3 mouthPos = getMouthOffset();
        Vec3 direction = calculateProjectileDirection();
        projectile.setPos(mouthPos.x, mouthPos.y, mouthPos.z);
        projectile.setDeltaMovement(direction.scale(getLaunchSpeed() * getShotPower()));
        projectile.alignRenderToDirection(direction);
        projectile.setBaseDamage(getBaseDamage());
        projectile.setOwner(this);

        serverLevel.addFreshEntity(projectile);
        spawnLaunchParticles(serverLevel, mouthPos, direction);

        setAmmoLoaded("");
        startRecovery();
        setOperator(operator);
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        fireBolt(serverLevel, operator);
        return true;
    }

    protected Vec3 calculateProjectileDirection() {
        float accuracyDegrees = getAccuracyMultiplier();
        float adjustedYaw = getAimingYaw() + (random.nextFloat() - 0.5F) * 2.0F * accuracyDegrees;
        float adjustedPitch = getTrackedPitch() + (random.nextFloat() - 0.5F) * 1.6F * accuracyDegrees;
        return directionFromAngles(adjustedYaw, adjustedPitch);
    }

    protected float getAimingYaw() {
        return usesIndependentAimYaw() ? getTrackedYaw() : getVisualRotationYInDegrees();
    }

    public Vec3 getAutomatedAimOrigin() {
        return getMouthOffset(getX(), getY(), getZ(), getAimingYaw(), 0.0F);
    }

    public float calculateAutomatedAimPitch(Vec3 target) {
        double speed = getLaunchSpeed() * getShotPower();
        return SiegeBallistics.calculateLowAnglePitch(getAutomatedAimOrigin(), target, speed,
                ballisticFlight(getBoltType()));
    }

    @Override
    protected float getAimingRenderYaw() {
        if (level().isClientSide && clientYawAimInitialized
                && getFirstPassenger() instanceof Player player && player.isLocalPlayer()) {
            return clientPredictedAimYaw;
        }
        return getAimingYaw();
    }

    @Override
    protected float getAimingRenderPitch() {
        if (level().isClientSide) {
            initializeClientPitchAim();
            return clientPredictedAimPitch;
        }
        return super.getAimingRenderPitch();
    }

    protected Vec3 getMouthOffset() {
        return getMouthOffset(getX(), getY(), getZ(), getAimingYaw(), getTrackedPitch());
    }

    protected Vec3 getMouthOffset(double baseX, double baseY, double baseZ, float yaw, float pitch) {
        return getModelPointOffset(baseX, baseY, baseZ, yaw, pitch, getBoltPivotPixels());
    }

    protected Vec3 directionFromAngles(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        return new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
    }

    protected abstract Vec3 getWeaponPivotPixels();

    protected abstract Vec3 getBoltPivotPixels();

    protected Vec3 getModelPointOffset(double baseX, double baseY, double baseZ, float yaw, float pitch, Vec3 pointPixels) {
        Vec3 pitchedPoint = rotateAroundLocalX(pointPixels, getWeaponPivotPixels(), toModelPitch(pitch));
        Vec3 localBlocks = pitchedPoint.scale(PIXELS_TO_BLOCKS);
        return localModelOffsetToWorld(baseX, baseY, baseZ, yaw, localBlocks);
    }

    protected boolean modelSampleCollidesAtLocalRotation(Vec3 pointPixels, float localXRotation,
                                                         double probeRadius) {
        Vec3 rotatedPoint = rotateAroundLocalX(pointPixels, getWeaponPivotPixels(), localXRotation);
        Vec3 localBlocks = rotatedPoint.scale(PIXELS_TO_BLOCKS);
        Vec3 point = localModelOffsetToWorld(
                getX(), getY(), getZ(), getCollisionModelYaw(), localBlocks);
        AABB probe = new AABB(
                point.x - probeRadius,
                point.y - probeRadius,
                point.z - probeRadius,
                point.x + probeRadius,
                point.y + probeRadius,
                point.z + probeRadius
        );
        return level().getBlockCollisions(this, probe).iterator().hasNext();
    }

    protected float getCollisionModelYaw() {
        return getAimingRenderYaw();
    }

    protected static Vec3 rotateAroundLocalX(Vec3 point, Vec3 pivot, float degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double dy = point.y - pivot.y;
        double dz = point.z - pivot.z;
        return new Vec3(
                point.x,
                pivot.y + dy * cos - dz * sin,
                pivot.z + dy * sin + dz * cos
        );
    }

    private Vec3 localModelOffsetToWorld(double baseX, double baseY, double baseZ, float yaw, Vec3 localBlocks) {
        double radians = Math.toRadians(-yaw - 180.0D);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double x = localBlocks.x * cos + localBlocks.z * sin;
        double z = -localBlocks.x * sin + localBlocks.z * cos;
        return new Vec3(baseX + x, baseY + localBlocks.y, baseZ + z);
    }

    protected void spawnLaunchParticles(ServerLevel serverLevel, Vec3 mouthPos, Vec3 direction) {
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                mouthPos.x, mouthPos.y, mouthPos.z, 12,
                direction.x * 0.15D, direction.y * 0.05D, direction.z * 0.15D, 0.15D);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, STATE_ANIMATION_CONTROLLER, 0, state -> {
            if (hasAmmoLoaded() && getWindingTime() > 0) {
                state.setAnimation(reloadAnim);
            } else {
                state.setAnimation(shouldRenderLoadedPose() ? loadedAnim : unloadedAnim);
            }
            return PlayState.CONTINUE;
        })
                .setAnimationSpeedHandler(animatable -> animatable.hasAmmoLoaded() && animatable.getWindingTime() > 0
                        ? animatable.getReloadAnimationLengthTicks() / Math.max(1, animatable.getWindingDurationTicks())
                        : 1.0D));
        registrar.add(new AnimationController<>(this, ACTION_ANIMATION_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim("shoot", shootAnim));
    }

    protected boolean shouldRenderLoadedPose() {
        return hasAmmoLoaded() && isWindingComplete();
    }

    protected int getWindingDurationTicks() {
        return getLoadingRequirementTicks("winding");
    }

    protected abstract double getReloadAnimationLengthTicks();

    @Override
    public void triggerAnimation(String name) {
        if ("shoot".equals(name)) {
            triggerAnim(ACTION_ANIMATION_CONTROLLER, "shoot");
        }
    }

    @Override
    public void stopAnimation(String name) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    public Vec3 getAimCameraPosition(double baseX, double baseY, double baseZ, float yaw, float pitch) {
        Vec3 direction = directionFromAngles(yaw, pitch);
        Vec3 mouthPos = getMouthOffset(baseX, baseY, baseZ, yaw, pitch);
        double yawRad = Math.toRadians(yaw);
        Vec3 right = new Vec3(Math.cos(yawRad), 0.0D, Math.sin(yawRad));

        return mouthPos
                .add(direction.scale(getAimCameraForwardDistance()))
                .add(right.scale(getAimCameraSideDistance()))
                .add(0.0D, getAimCameraLift(), 0.0D);
    }

    protected abstract double getAimCameraForwardDistance();

    protected abstract double getAimCameraLift();

    protected double getAimCameraSideDistance() {
        return 0.0D;
    }

    @Override
    protected float getPassengerOffsetYaw(Entity entity) {
        return level().isClientSide ? getAimingRenderYaw() : getAimingYaw();
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }

    private record ParkedPitchStep(float pitch, float velocity) {
    }
}
