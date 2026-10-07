package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.GiantCannonProjectile;
import me.mss1r.siegeworks.gameplay.ballistics.ScattershotVolley;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.ScalarAnimationCurve;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.loading.AutomatedLoadingSession;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.loading.LoadingStatus;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.audio.ArtilleryLoadingSounds;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.item.ArtilleryPowderCosts;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
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

import static me.mss1r.axiomata.collision.ScalarAnimationCurve.Interpolation.CATMULL_ROM;
import static me.mss1r.axiomata.collision.ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC;
import static me.mss1r.axiomata.collision.ScalarAnimationCurve.Interpolation.EASE_OUT_ELASTIC;
import static me.mss1r.axiomata.collision.ScalarAnimationCurve.Interpolation.LINEAR;

public class MonsMegEntity extends AbstractSiegeEntity implements GeoEntity, SiegeArtilleryControl, MountedSiegeItemControl {
    private static final String AMMO_GIANT_CANNON_BALL = "siegeworks:giant_cannon_ball";
    private static final float OPERATOR_YAW_OFFSET = 50.0F;
    private static final double MODEL_UNITS_PER_BLOCK = 16.0D;
    private static final double AIM_PIVOT_HEIGHT = 11.2D / MODEL_UNITS_PER_BLOCK;
    private static final double AIM_PIVOT_FORWARD = -2.0D / MODEL_UNITS_PER_BLOCK;
    private static final double MUZZLE_HEIGHT_FROM_PIVOT = 19.3D / MODEL_UNITS_PER_BLOCK;
    private static final double MUZZLE_FORWARD_FROM_PIVOT = 56.0D / MODEL_UNITS_PER_BLOCK;
    private static final int FULL_DRAFT_TEAM = 1;
    private static final double TOW_DISTANCE = 4.15D;
    private static final int SHOOT_ANIMATION_TICKS = 100;
    private static final EntityDataAccessor<Integer> SHOOT_ANIMATION_TICK =
            SynchedEntityData.defineId(MonsMegEntity.class, EntityDataSerializers.INT);
    private static final ScalarAnimationCurve ROOT_RECOIL_Z = ScalarAnimationCurve.of(
            ScalarAnimationCurve.key(0.0F, 0.0D, CATMULL_ROM),
            ScalarAnimationCurve.key(7.5F, 25.0D, EASE_IN_ELASTIC),
            ScalarAnimationCurve.key(17.5F, 25.0D, CATMULL_ROM),
            ScalarAnimationCurve.key(25.0F, 25.0D, EASE_OUT_ELASTIC),
            ScalarAnimationCurve.key(50.834F, 25.0D, LINEAR),
            ScalarAnimationCurve.key(100.0F, 0.0D, CATMULL_ROM));
    private static final ScalarAnimationCurve BASE_RECOIL_X = ScalarAnimationCurve.of(
            ScalarAnimationCurve.key(0.0F, 0.0D, CATMULL_ROM),
            ScalarAnimationCurve.key(2.5F, -7.5D, CATMULL_ROM),
            ScalarAnimationCurve.key(7.5F, 0.0D, CATMULL_ROM),
            ScalarAnimationCurve.key(11.666F, 2.5D, EASE_IN_ELASTIC),
            ScalarAnimationCurve.key(17.5F, 0.0D, CATMULL_ROM));

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withFiringSound(SiegeworksSounds.CANNON_FIRE.get())
            .withFiringRange(256.0D)
            .withFiringVolume(2.4F);

    private static final LoadingRequirement[] LOAD_STAGES = {
            LoadingRequirement.consume(SiegeworksItems.BLACK_POWDER.get(), ArtilleryPowderCosts.MONS_MEG)
                    .withFeedback(LoadingStatus.LOADING_POWDER.translationKey()).timedBy("powder"),
            LoadingRequirement.tool(SiegeworksItems.RAMROD.get())
                    .withFeedback(LoadingStatus.RAMMING_CHARGE.translationKey()).timedBy("ramCharge"),
            LoadingRequirement.consume(SiegeworksItems.GIANT_CANNON_BALL.get()),
            LoadingRequirement.tool(SiegeworksItems.RAMROD.get())
                    .withFeedback(LoadingStatus.RAMMING_PROJECTILE.translationKey()).timedBy("ramProjectile"),
            LoadingRequirement.tool(Items.FLINT_AND_STEEL)
                    .withFeedback(LoadingStatus.PRIMING.translationKey()).timedBy("priming")
    };

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation shootAnim = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private int pendingManualScattershotCount;
    private int pendingAutomatedScattershotCount;
    private int previousShootAnimationTick = -1;
    private float previousCollisionAimPitch;

    public MonsMegEntity(EntityType<? extends LivingEntity> type, Level level) {
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
        data.define(SHOOT_ANIMATION_TICK, -1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.025)
                .add(Attributes.KNOCKBACK_RESISTANCE, 400.0);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    protected void playReloadSound(ServerLevel serverLevel) {
        ArtilleryLoadingSounds.playStage(serverLevel, this, getLoadStage(), true);
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return AbstractSiegeEntity.isDraftMount(getFirstPassenger()) ? 0.18f : 0.42f;
    }

    @Override
    protected double getReloadServiceRange() {
        return 16.0;
    }

    @Override
    public float getMinAimPitch() {
        return -6.0F;
    }

    @Override
    public float getMaxAimPitch() {
        return 3.5F;
    }

    public float getRenderedModelPitch() {
        return -getRenderedAimPitch();
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (getFirstPassenger() != null && getFirstPassenger() != player) return InteractionResult.FAIL;
        if (continueLoadingAction(player)) return showLoadingProgress(player);

        ItemStack stack = player.getItemInHand(hand);
        int stage = getLoadStage();

        if (stage >= LOAD_STAGES.length) return InteractionResult.SUCCESS;
        if (getCooldown() > 0) return showCooldownProgress(player);

        if (stack.isEmpty() && canAddPassenger(player) && !player.isShiftKeyDown()) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (stage == 2 && getScattershotCount() > 0 && stack.is(SiegeworksItems.RAMROD.get())) {
            LoadingRequirement rammingStage = LOAD_STAGES[3];
            if (!canBeginLoadingRequirement(player, hand, rammingStage)) return InteractionResult.SUCCESS;
            setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
            setLoadStage(3);
            return beginLoadingAction(player, hand, serverLevel, 3, rammingStage);
        }
        if (stage == 2 && stack.is(Items.IRON_NUGGET)) {
            int freeSlots = getScattershotProfile().capacity() - getScattershotCount();
            int amount = player.getAbilities().instabuild
                    ? freeSlots
                    : Math.min(freeSlots, stack.getCount());
            if (amount <= 0) {
                setLoadStage(3);
                return InteractionResult.SUCCESS;
            }

            pendingManualScattershotCount = amount;
            LoadingRequirement scattershotStage = createIronScattershotStage(amount);
            if (!canBeginLoadingRequirement(player, hand, scattershotStage)) return InteractionResult.SUCCESS;
            return beginLoadingAction(player, hand, serverLevel, stage, scattershotStage,
                    getScattershotBatchLoadingTicks(amount));
        }
        if (stage == 2 && getScattershotCount() > 0) {
            return InteractionResult.SUCCESS;
        }

        LoadingRequirement required = LOAD_STAGES[stage];
        if (!canBeginLoadingRequirement(player, hand, required)) return InteractionResult.SUCCESS;
        return beginLoadingAction(player, hand, serverLevel, stage, required);
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        if (getFirstPassenger() != player) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (hasLoadingAction()) return stack.is(getActiveLoadingItem());
        int stage = getLoadStage();
        if (stage < 0 || stage >= LOAD_STAGES.length) return false;
        if (stage == 2) {
            if (getScattershotCount() > 0) {
                return stack.is(Items.IRON_NUGGET) || stack.is(SiegeworksItems.RAMROD.get());
            }
            return stack.is(SiegeworksItems.GIANT_CANNON_BALL.get()) || stack.is(Items.IRON_NUGGET);
        }
        return LOAD_STAGES[stage].matches(stack.getItem());
    }

    @Override
    public InteractionResult handleMountedItem(Player player, InteractionHand hand, ServerLevel serverLevel) {
        return handleSiegeInteraction(player, hand, serverLevel);
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel serverLevel) || !isOperator(operator)) {
            automatedLoading.reset();
            return SiegeActionResult.DENIED;
        }
        if (hasLoadingAction() || getCooldown() > 0) {
            return SiegeActionResult.IN_PROGRESS;
        }

        int stageIndex = getLoadStage();
        if (stageIndex < 0 || stageIndex >= LOAD_STAGES.length) {
            automatedLoading.reset();
            pendingAutomatedScattershotCount = 0;
            setLoadStage(0);
            setScattershotCount(0);
            return SiegeActionResult.DENIED;
        }

        LoadingRequirement stage = LOAD_STAGES[stageIndex];
        if (stageIndex == 2) {
            boolean standardAvailable = AutomatedLoadingSession.hasRequiredItem(
                    inventory, stage, stage.item());
            boolean useScattershot = pendingAutomatedScattershotCount > 0
                    || getScattershotCount() > 0
                    || !standardAvailable;
            if (useScattershot) {
                if (!automatedLoading.isActive()) {
                    int freeSlots = getScattershotProfile().capacity() - getScattershotCount();
                    pendingAutomatedScattershotCount = Math.min(freeSlots,
                            countContainerItem(inventory, Items.IRON_NUGGET));
                }
                if (pendingAutomatedScattershotCount <= 0) {
                    automatedLoading.reset();
                    if (getScattershotCount() > 0) {
                        setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
                        setLoadStage(3);
                        return SiegeActionResult.IN_PROGRESS;
                    }
                    return SiegeActionResult.MISSING_AMMUNITION;
                }
                stage = createIronScattershotStage(pendingAutomatedScattershotCount);
            }
        }
        Item stageItem = stage.item();
        if (stageItem == null || !AutomatedLoadingSession.hasRequiredItem(inventory, stage, stageItem)) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        if (automatedLoading.needsStart(operator, stageIndex)) {
            int durationTicks = stageIndex == 2 && stageItem == Items.IRON_NUGGET
                    ? getScattershotBatchLoadingTicks(stage.amount())
                    : getLoadingRequirementTicks(stage.timingKey());
            automatedLoading.start(operator, stageIndex, durationTicks);
            onLoadingStart(serverLevel);
            if (shouldPlayLoadingSoundOnStart()) {
                playReloadSound(serverLevel);
            }
        }

        if (!automatedLoading.tickComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (!AutomatedLoadingSession.finishStage(inventory, stage, stageItem, operator)) {
            automatedLoading.reset();
            pendingAutomatedScattershotCount = 0;
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        automatedLoading.reset();
        if (stageIndex == 2) {
            if (stageItem == Items.IRON_NUGGET) {
                setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
                setScattershotCount(Math.min(getScattershotProfile().capacity(),
                        getScattershotCount() + stage.amount()));
                pendingAutomatedScattershotCount = 0;
                if (getScattershotCount() < getScattershotProfile().capacity()) {
                    return SiegeActionResult.IN_PROGRESS;
                }
            } else {
                setScattershotCount(0);
                setAmmoLoaded(AMMO_GIANT_CANNON_BALL);
            }
            setLoadStage(stageIndex + 1);
            return SiegeActionResult.LOADED;
        }
        if (stageItem == Items.FLINT_AND_STEEL) {
            fireMonsMeg(serverLevel);
            setOperator(operator);
            return SiegeActionResult.FIRED;
        }

        setLoadStage(stageIndex + 1);
        return SiegeActionResult.IN_PROGRESS;
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        automatedLoading.cancel(operator);
        pendingAutomatedScattershotCount = 0;
    }

    @Override
    public SiegeOperationState getOperationState() {
        if (hasLoadingAction() || automatedLoading.isActive() || getLoadStage() > 0) {
            return SiegeOperationState.LOADING;
        }
        return super.getOperationState();
    }

    @Override
    public boolean isReadyToFire() {
        return getLoadStage() == LOAD_STAGES.length - 1 && getCooldown() <= 0;
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stage) {
        if (stage < 0 || stage >= LOAD_STAGES.length) return;

        LoadingRequirement required = LOAD_STAGES[stage];
        if (stage == 2 && getActiveLoadingItem() == Items.IRON_NUGGET) {
            int amount = Math.min(pendingManualScattershotCount,
                    getScattershotProfile().capacity() - getScattershotCount());
            pendingManualScattershotCount = 0;
            if (amount <= 0) {
                setLoadStage(stage + 1);
                return;
            }
            required = createIronScattershotStage(amount);
        }

        if (stage == 2) {
            if (!consumeLoadingRequirement(player, hand, required)) return;
            if (required.item() == Items.IRON_NUGGET) {
                setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
                setScattershotCount(Math.min(getScattershotProfile().capacity(),
                        getScattershotCount() + required.amount()));
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "siege.loading.scattershot_count", getScattershotCount(),
                        getScattershotProfile().capacity()), true);
                if (getScattershotCount() < getScattershotProfile().capacity()) {
                    return;
                }
            } else {
                setScattershotCount(0);
                setAmmoLoaded(AMMO_GIANT_CANNON_BALL);
            }
            setLoadStage(stage + 1);
            return;
        }

        if (!consumeLoadingRequirement(player, hand, required)) return;

        if (required.item() == Items.FLINT_AND_STEEL) {
            fireMonsMeg(serverLevel);
            setOperator(player);
            setLoadStage(0);
            return;
        }

        setLoadStage(stage + 1);
    }

    private void fireMonsMeg(ServerLevel serverLevel) {
        float firingPitch = getTrackedPitch();
        setShootAnimationTick(0);
        triggerAnimation("shoot");

        Vec3 mouthPos = getMouthOffset(firingPitch);
        Vec3 direction = calculateProjectileDirection(firingPitch);
        Vec3 velocity = direction.scale(getLaunchSpeed());
        if (SiegeAmmo.isIronScattershotAmmoKey(getAmmoLoaded())) {
            int loadedItems = getScattershotCount() > 0
                    ? Math.min(getScattershotCount(), getScattershotProfile().capacity())
                    : getScattershotProfile().capacity();
            int pelletCount = ScattershotVolley.rollPelletCount(random, loadedItems,
                    getScattershotProfile().pelletsPerLoadedItemMin(),
                    getScattershotProfile().pelletsPerLoadedItemMax());
            ScattershotVolley.spawn(serverLevel, this, mouthPos, velocity,
                    pelletCount, getScattershotProfile().spreadDegrees(),
                    getScattershotProfile().baseDamagePerPellet(), false);
        } else {
            GiantCannonProjectile projectile = new GiantCannonProjectile(
                    SiegeworksEntities.GIANT_CANNON_BALL_PROJECTILE.get(), this, serverLevel);
            projectile.setPos(mouthPos.x, mouthPos.y, mouthPos.z);
            projectile.setDeltaMovement(velocity);
            projectile.lockRenderRotation(getVisualRotationYInDegrees(), firingPitch);
            projectile.setBaseDamage(getBaseDamage());
            projectile.setOwner(this);
            serverLevel.addFreshEntity(projectile);
        }

        SiegeParticleEffects.muzzleBlast(serverLevel, mouthPos, direction,
                SiegeParticleEffects.MuzzleProfile.BOMBARD);
        playMonsMegReport(serverLevel, mouthPos);

        setLoadStage(0);
        setAmmoLoaded("");
        setScattershotCount(0);
        startRecovery();
    }

    @Override
    public void tick() {
        previousShootAnimationTick = getShootAnimationTick();
        previousCollisionAimPitch = getTrackedPitch();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        int animationTick = getShootAnimationTick();
        if (animationTick < 0) {
            return;
        }
        setShootAnimationTick(animationTick + 1 >= SHOOT_ANIMATION_TICKS
                ? -1
                : animationTick + 1);
    }

    public int getShootAnimationTick() {
        return entityData.get(SHOOT_ANIMATION_TICK);
    }

    private void setShootAnimationTick(int tick) {
        entityData.set(SHOOT_ANIMATION_TICK, tick);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getTrackedPitch(), getShootAnimationTick());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionAimPitch, previousShootAnimationTick);
    }

    private static List<CollisionGroup> collisionGroups(float aimPitch, float animationTick) {
        float sampledTick = Math.max(0.0F, animationTick);
        CollisionPose baseRecoil = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.MONS_MEG_BODY.pivot(),
                (float) Math.toRadians(-BASE_RECOIL_X.sample(sampledTick)));
        CollisionPose aim = CollisionPose.fromGeckoBoneX(
                GeneratedCollisionShapes.MONS_MEG_BODY.pivot(),
                (float) Math.toRadians(-aimPitch));
        CollisionPose rootRecoil = CollisionPose.fromGeckoBonePosition(
                new Vec3(0.0D, 0.0D, ROOT_RECOIL_Z.sample(sampledTick)));
        return List.of(new CollisionGroup("body", GeneratedCollisionShapes.MONS_MEG_BODY,
                baseRecoil.then(aim).then(rootRecoil)));
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        setScattershotCount(0);
        setAmmoLoaded(AMMO_GIANT_CANNON_BALL);
        fireMonsMeg(serverLevel);
        setOperator(operator);
        return true;
    }

    @Override
    protected String getCooldownStatusKey() {
        return "siege.loading.state.cooling";
    }

    private Vec3 calculateProjectileDirection(float basePitch) {
        float accuracyDegrees = getAccuracyMultiplier();
        float yawOffset = (random.nextFloat() - 0.5f) * 3.0f * accuracyDegrees;
        float pitchOffset = (random.nextFloat() - 0.5f) * 3.0f * accuracyDegrees;

        float adjustedYaw = getVisualRotationYInDegrees() + yawOffset;
        float adjustedPitch = basePitch + pitchOffset;

        float yawRad = (float) Math.toRadians(adjustedYaw);
        float pitchRad = (float) Math.toRadians(adjustedPitch);

        return new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();
    }

    @Override
    protected void onLoadingActionCancelled() {
        pendingManualScattershotCount = 0;
    }

    private static LoadingRequirement createIronScattershotStage(int amount) {
        return LoadingRequirement.consume(Items.IRON_NUGGET, amount);
    }

    private int getScattershotBatchLoadingTicks(int amount) {
        int capacity = Math.max(1, getScattershotProfile().capacity());
        return Math.max(4, Math.round(getLoadingRequirementTicks("ammunition") * amount / (float) capacity));
    }

    private static int countContainerItem(Container inventory, Item item) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private Vec3 getMouthOffset(float pitch) {
        double yawRad = Math.toRadians(getVisualRotationYInDegrees());
        double pitchRad = Math.toRadians(pitch);
        Vec3 horizontal = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 pivot = position().add(horizontal.scale(AIM_PIVOT_FORWARD))
                .add(0.0D, AIM_PIVOT_HEIGHT, 0.0D);
        double horizontalDistance = MUZZLE_FORWARD_FROM_PIVOT * Math.cos(pitchRad)
                + MUZZLE_HEIGHT_FROM_PIVOT * Math.sin(pitchRad);
        double verticalDistance = -MUZZLE_FORWARD_FROM_PIVOT * Math.sin(pitchRad)
                + MUZZLE_HEIGHT_FROM_PIVOT * Math.cos(pitchRad);
        return pivot.add(horizontal.scale(horizontalDistance)).add(0.0D, verticalDistance, 0.0D);
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return getMouthOffset(0.0F);
    }

    @Override
    public float calculateAutomatedAimPitch(Vec3 target) {
        return SiegeBallistics.calculateLowAnglePitch(getAutomatedAimOrigin(), target,
                getLaunchSpeed(), ballisticFlight(SiegeworksEntities.GIANT_CANNON_BALL_PROJECTILE.get()));
    }

    private void playMonsMegReport(ServerLevel serverLevel, Vec3 soundPos) {
        serverLevel.playSound(null, soundPos.x, soundPos.y, soundPos.z,
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.genericExplodeSound(),
                SoundSource.BLOCKS, 11.0f, 0.18f);
        playSoundToPlayersInRange(serverLevel, SiegeworksSounds.CANNON_FIRE.get(),
                0.0D, 70.0D, 16.0F, 1.0F, 0.1F, 0.0F);
        serverLevel.playSound(null, soundPos.x, soundPos.y, soundPos.z,
                SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 5.0f, 0.65f);
        serverLevel.playSound(null, soundPos.x, soundPos.y, soundPos.z,
                SiegeworksSounds.RAIL_SKID.get(), SoundSource.BLOCKS, 3.5f, 0.65f);
        playSoundToPlayersInRange(serverLevel, SiegeworksSounds.CANNON_FIRE.get(), 70.0, 520.0, 7.5f, 0.05f);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "anim_controller", state -> PlayState.STOP)
                .triggerableAnim("shoot", shootAnim));
    }

    @Override
    public void triggerAnimation(String name) {
        if ("shoot".equals(name)) {
            triggerAnim("anim_controller", "shoot");
        }
    }

    @Override
    public void stopAnimation(String name) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(TOW_DISTANCE);
    }

    private double getDraftTeamPower(Entity operator) {
        if (!isDraftMount(operator)) {
            return 1.0D;
        }
        return Math.min(1.0D, (double) getTowingMounts().size() / FULL_DRAFT_TEAM);
    }

    @Override
    public double getVelocity(Entity operator) {
        return super.getVelocity(operator) * getDraftTeamPower(operator);
    }

    @Override
    public double getDriveAcceleration(Entity operator) {
        return super.getDriveAcceleration(operator) * getDraftTeamPower(operator);
    }

    @Override
    public double getDriveDeceleration(Entity operator) {
        return super.getDriveDeceleration(operator) * getDraftTeamPower(operator);
    }

    @Override
    public float getSteeringSpeedDegrees(Entity operator) {
        return (float) (super.getSteeringSpeedDegrees(operator) * getDraftTeamPower(operator));
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(15.0D / MODEL_UNITS_PER_BLOCK, 0.0D, 49.0D / MODEL_UNITS_PER_BLOCK);
    }

    @Override
    protected float operatorViewOffsetDegrees() {
        return OPERATOR_YAW_OFFSET;
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }
}
