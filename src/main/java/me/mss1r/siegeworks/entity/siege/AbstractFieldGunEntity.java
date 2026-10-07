package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.gameplay.ballistics.ScattershotVolley;
import me.mss1r.siegeworks.gameplay.audio.ArtilleryLoadingSounds;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.CollisionShape;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.loading.AutomatedLoadingSession;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.loading.LoadingStatus;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
/*import software.bernie.geckolib.core.animation.RawAnimation;
*///?} else {
import software.bernie.geckolib.animation.RawAnimation;
//?}
//? if forge {
/*import software.bernie.geckolib.core.object.PlayState;
*///?} else {
import software.bernie.geckolib.animation.PlayState;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public abstract class AbstractFieldGunEntity extends AbstractSiegeEntity
        implements GeoEntity, SiegeArtilleryControl, MountedSiegeItemControl {
    private static final float OPERATOR_YAW_OFFSET = 45.0F;
    private static final String AMMO_CANNON_BALL = "siegeworks:cannon_ball";
    private static final RawAnimation SHOOT_ANIMATION =
            RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private int pendingManualScattershotCount;
    private int pendingAutomatedScattershotCount;
    private float previousCollisionBasePitch;
    private float previousCollisionAimPitch;

    protected AbstractFieldGunEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        setTrackedPitch(0.0F);
    }

    protected static LoadingRequirement[] createLoadStages(int powderCost) {
        return new LoadingRequirement[] {
                LoadingRequirement.consume(SiegeworksItems.BLACK_POWDER.get(), powderCost)
                        .withFeedback(LoadingStatus.LOADING_POWDER.translationKey()).timedBy("powder"),
                LoadingRequirement.tool(SiegeworksItems.RAMROD.get())
                        .withFeedback(LoadingStatus.RAMMING_CHARGE.translationKey()).timedBy("ramCharge"),
                LoadingRequirement.consume(SiegeworksItems.CANNON_BALL.get()),
                LoadingRequirement.tool(SiegeworksItems.RAMROD.get())
                        .withFeedback(LoadingStatus.RAMMING_PROJECTILE.translationKey()).timedBy("ramProjectile"),
                LoadingRequirement.tool(Items.FLINT_AND_STEEL)
                        .withFeedback(LoadingStatus.PRIMING.translationKey()).timedBy("priming")
        };
    }

    protected abstract LoadingRequirement[] getLoadStages();

    protected abstract FieldGunGeometry getFieldGunGeometry();

    protected abstract SiegeParticleEffects.MuzzleProfile getMuzzleProfile();

    protected abstract CollisionShape getBodyCollisionShape();

    @Override
    protected void playReloadSound(ServerLevel serverLevel) {
        ArtilleryLoadingSounds.playStage(serverLevel, this, getLoadStage(), false);
    }

    protected abstract CollisionShape getCannonCollisionShape();

    @Override
    public boolean hasDifferentialDrive() {
        return true;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return super.canAddOperator(entity);
    }

    protected boolean supportsDraftMounts() {
        return false;
    }

    protected Vec3 getDraftMountOffset() {
        return Vec3.ZERO;
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return getSteeringSpeedDegrees(getFirstPassenger());
    }

    public final float getRenderedModelPitch() {
        return -getRenderedAimPitch();
    }

    public final float getRenderedTowBasePitch() {
        return getModelTowBasePitch();
    }

    private float getModelTowBasePitch() {
        return isTowed() ? -getFieldGunGeometry().carriageElevationDegrees() : 0.0F;
    }

    @Override
    public final float getMinAimPitch() {
        return -getFieldGunGeometry().carriageElevationDegrees();
    }

    @Override
    public final float getMaxAimPitch() {
        return getFieldGunGeometry().maxDepressionDegrees();
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (getFirstPassenger() != null && getFirstPassenger() != player) return InteractionResult.FAIL;
        if (continueLoadingAction(player)) return showLoadingProgress(player);

        ItemStack stack = player.getItemInHand(hand);
        LoadingRequirement[] stages = getLoadStages();
        int stageIndex = getLoadStage();
        if (stageIndex >= stages.length) return InteractionResult.SUCCESS;
        if (getCooldown() > 0) return showCooldownProgress(player);

        if (stack.isEmpty() && canAddPassenger(player) && !player.isShiftKeyDown()) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (stageIndex == 2 && getScattershotCount() > 0 && stack.is(SiegeworksItems.RAMROD.get())) {
            LoadingRequirement rammingStage = stages[3];
            if (!canBeginLoadingRequirement(player, hand, rammingStage)) return InteractionResult.SUCCESS;
            setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
            setLoadStage(3);
            return beginLoadingAction(player, hand, serverLevel, 3, rammingStage);
        }
        if (stageIndex == 2 && stack.is(Items.IRON_NUGGET)) {
            int freeSlots = getScattershotCapacity() - getScattershotCount();
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
            return beginLoadingAction(player, hand, serverLevel, stageIndex, scattershotStage,
                    getScattershotBatchLoadingTicks(amount));
        }
        if (stageIndex == 2 && getScattershotCount() > 0 && !stack.is(Items.IRON_NUGGET)) {
            return InteractionResult.SUCCESS;
        }

        LoadingRequirement stage = stages[stageIndex];
        if (!canBeginLoadingRequirement(player, hand, stage)) return InteractionResult.SUCCESS;
        return beginLoadingAction(player, hand, serverLevel, stageIndex, stage);
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        if (getFirstPassenger() != player) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (hasLoadingAction()) return stack.is(getActiveLoadingItem());
        int stageIndex = getLoadStage();
        LoadingRequirement[] stages = getLoadStages();
        if (stageIndex < 0 || stageIndex >= stages.length) return false;
        if (stageIndex == 2) {
            if (getScattershotCount() > 0) {
                return stack.is(Items.IRON_NUGGET) || stack.is(SiegeworksItems.RAMROD.get());
            }
            return stack.is(SiegeworksItems.CANNON_BALL.get()) || stack.is(Items.IRON_NUGGET);
        }
        return stages[stageIndex].matches(stack.getItem());
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
        if (hasLoadingAction() || getCooldown() > 0) return SiegeActionResult.IN_PROGRESS;

        LoadingRequirement[] stages = getLoadStages();
        int stageIndex = getLoadStage();
        if (stageIndex < 0 || stageIndex >= stages.length) {
            automatedLoading.reset();
            pendingAutomatedScattershotCount = 0;
            setLoadStage(0);
            setScattershotCount(0);
            return SiegeActionResult.DENIED;
        }

        LoadingRequirement stage = stages[stageIndex];
        if (stageIndex == 2) {
            boolean standardAvailable = AutomatedLoadingSession.hasRequiredItem(
                    inventory, stage, stage.item());
            boolean useScattershot = pendingAutomatedScattershotCount > 0
                    || getScattershotCount() > 0
                    || !standardAvailable;
            if (useScattershot) {
                if (!automatedLoading.isActive()) {
                    int freeSlots = getScattershotCapacity() - getScattershotCount();
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
            if (shouldPlayLoadingSoundOnStart()) playReloadSound(serverLevel);
        }
        if (!automatedLoading.tickComplete()) return SiegeActionResult.IN_PROGRESS;
        if (!AutomatedLoadingSession.finishStage(inventory, stage, stageItem, operator)) {
            automatedLoading.reset();
            pendingAutomatedScattershotCount = 0;
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        automatedLoading.reset();
        if (stageIndex == 2) {
            if (stageItem == Items.IRON_NUGGET) {
                setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
                setScattershotCount(Math.min(getScattershotCapacity(),
                        getScattershotCount() + stage.amount()));
                pendingAutomatedScattershotCount = 0;
                if (getScattershotCount() >= getScattershotCapacity()) {
                    setLoadStage(stageIndex + 1);
                    return SiegeActionResult.LOADED;
                }
                return SiegeActionResult.IN_PROGRESS;
            }

            setScattershotCount(0);
            setAmmoLoaded(AMMO_CANNON_BALL);
            setLoadStage(stageIndex + 1);
            return SiegeActionResult.LOADED;
        }
        if (stageItem == Items.FLINT_AND_STEEL) {
            fire(serverLevel);
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
        return getLoadStage() == getLoadStages().length - 1 && getCooldown() <= 0;
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        LoadingRequirement[] stages = getLoadStages();
        if (stageIndex < 0 || stageIndex >= stages.length) return;

        LoadingRequirement stage = stages[stageIndex];
        if (stageIndex == 2 && getActiveLoadingItem() == Items.IRON_NUGGET) {
            int amount = Math.min(pendingManualScattershotCount,
                    getScattershotCapacity() - getScattershotCount());
            pendingManualScattershotCount = 0;
            if (amount <= 0) {
                setLoadStage(stageIndex + 1);
                return;
            }
            stage = createIronScattershotStage(amount);
        }
        if (stageIndex == 2) {
            if (!consumeLoadingRequirement(player, hand, stage)) return;
            if (stage.item() == Items.IRON_NUGGET) {
                setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
                setScattershotCount(Math.min(getScattershotCapacity(),
                        getScattershotCount() + stage.amount()));
                player.displayClientMessage(Component.translatable("siege.loading.scattershot_count",
                        getScattershotCount(), getScattershotCapacity()), true);
                if (getScattershotCount() < getScattershotCapacity()) {
                    return;
                }
            } else {
                setScattershotCount(0);
                setAmmoLoaded(AMMO_CANNON_BALL);
            }
            setLoadStage(stageIndex + 1);
            return;
        }

        if (!consumeLoadingRequirement(player, hand, stage)) return;
        if (stage.item() == Items.FLINT_AND_STEEL) {
            fire(serverLevel);
            setOperator(player);
            return;
        }
        setLoadStage(stageIndex + 1);
    }

    private void fire(ServerLevel serverLevel) {
        triggerAnimation("shoot");
        Vec3 direction = projectileDirection();
        Vec3 muzzle = muzzlePosition();
        Vec3 velocity = direction.scale(getLaunchSpeed());
        if (SiegeAmmo.isIronScattershotAmmoKey(getAmmoLoaded())) {
            int loadedItems = getScattershotCount() > 0
                    ? Math.min(getScattershotCount(), getScattershotCapacity())
                    : getScattershotCapacity();
            int pelletCount = ScattershotVolley.rollPelletCount(random, loadedItems,
                    getScattershotProfile().pelletsPerLoadedItemMin(),
                    getScattershotProfile().pelletsPerLoadedItemMax());
            ScattershotVolley.spawn(serverLevel, this, muzzle, velocity,
                    pelletCount, getScattershotProfile().spreadDegrees(),
                    getScattershotProfile().baseDamagePerPellet(), false);
        } else {
            CannonProjectile projectile = new CannonProjectile(SiegeworksEntities.CANNON_BALL.get(), this, serverLevel);
            projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
            projectile.setDeltaMovement(velocity);
            projectile.setBaseDamage(getBaseDamage());
            projectile.setOwner(this);
            serverLevel.addFreshEntity(projectile);
        }

        spawnMuzzleEffects(serverLevel, muzzle, direction);
        playReport(serverLevel, muzzle);
        setLoadStage(0);
        setAmmoLoaded("");
        setScattershotCount(0);
        startRecovery();
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        setScattershotCount(0);
        setAmmoLoaded(AMMO_CANNON_BALL);
        fire(serverLevel);
        setOperator(operator);
        return true;
    }

    private Vec3 projectileDirection() {
        float accuracy = getAccuracyMultiplier();
        float yaw = getVisualRotationYInDegrees() + (random.nextFloat() - 0.5F) * 4.0F * accuracy;
        float pitch = getTrackedPitch() + (random.nextFloat() - 0.5F) * 4.0F * accuracy;
        float yawRadians = yaw * ((float) Math.PI / 180.0F);
        float pitchRadians = pitch * ((float) Math.PI / 180.0F);
        return new Vec3(-Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians), Math.cos(yawRadians) * Math.cos(pitchRadians)).normalize();
    }

    @Override
    protected void onLoadingActionCancelled() {
        pendingManualScattershotCount = 0;
    }

    private static LoadingRequirement createIronScattershotStage(int amount) {
        return LoadingRequirement.consume(Items.IRON_NUGGET, amount);
    }

    private int getScattershotCapacity() {
        return getScattershotProfile().capacity();
    }

    private int getScattershotBatchLoadingTicks(int amount) {
        int capacity = Math.max(1, getScattershotCapacity());
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

    private Vec3 muzzlePosition() {
        return muzzlePosition(getTrackedPitch());
    }

    private Vec3 muzzlePosition(float atPitch) {
        FieldGunGeometry geometry = getFieldGunGeometry();
        double yaw = Math.toRadians(getVisualRotationYInDegrees());
        double pitch = Math.toRadians(atPitch);
        Vec3 horizontal = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 pivot = position().add(horizontal.scale(geometry.pivotForward()))
                .add(0.0D, geometry.pivotHeight(), 0.0D);
        double horizontalDistance = geometry.muzzleForward() * Math.cos(pitch)
                + geometry.muzzleHeight() * Math.sin(pitch);
        double verticalDistance = -geometry.muzzleForward() * Math.sin(pitch)
                + geometry.muzzleHeight() * Math.cos(pitch);
        return pivot.add(horizontal.scale(horizontalDistance)).add(0.0D, verticalDistance, 0.0D);
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return muzzlePosition(0.0F);
    }

    @Override
    public float calculateAutomatedAimPitch(Vec3 target) {
        return SiegeBallistics.calculateLowAnglePitch(getAutomatedAimOrigin(), target,
                getLaunchSpeed(), ballisticFlight(SiegeworksEntities.CANNON_BALL.get()));
    }

    private void playReport(ServerLevel serverLevel, Vec3 position) {
        float pitch = 0.85F + random.nextFloat() * 0.20F;
        serverLevel.playSound(null, position.x, position.y, position.z,
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.genericExplodeSound(),
                SoundSource.PLAYERS, 3.0F, pitch * 0.55F);
        playSoundToPlayersInRange(serverLevel, getShootSound(), 0.0D, 45.0D, 15.0F, 0.8F, pitch, 0.0F);
        playSoundToPlayersInRange(serverLevel, getShootSound(), 45.0D, 170.0D, 6.4F, 0.25F, pitch, 0.0F);
        playSoundToPlayersInRange(serverLevel, SiegeworksSounds.CANNON_DISTANT.get(), 45.0D, 380.0D, 4.2F, 0.25F);
    }

    private void spawnMuzzleEffects(ServerLevel serverLevel, Vec3 muzzle, Vec3 direction) {
        SiegeParticleEffects.muzzleBlast(serverLevel, muzzle, direction, getMuzzleProfile());
    }

    @Override
    protected String getCooldownStatusKey() {
        return "siege.loading.state.cooling";
    }

    @Override
    public void tick() {
        previousCollisionBasePitch = getModelTowBasePitch();
        previousCollisionAimPitch = -getTrackedPitch();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getModelTowBasePitch(), -getTrackedPitch());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionBasePitch, previousCollisionAimPitch);
    }

    private List<CollisionGroup> collisionGroups(float basePitch, float aimPitch) {
        CollisionShape bodyShape = getBodyCollisionShape();
        CollisionShape cannonShape = getCannonCollisionShape();
        CollisionPose base = CollisionPose.fromGeckoBoneX(
                bodyShape.pivot(), (float) Math.toRadians(basePitch));
        CollisionPose cannon = CollisionPose.fromGeckoBoneX(
                cannonShape.pivot(), (float) Math.toRadians(aimPitch)).then(base);
        return List.of(
                new CollisionGroup("body", bodyShape, base),
                new CollisionGroup("cannon", cannonShape, cannon));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "action", state -> PlayState.STOP)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    public void triggerAnimation(String name) {
        if ("shoot".equals(name)) triggerAnim("action", "shoot");
    }

    @Override
    public void stopAnimation(String name) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public TowingProfile towingProfile() {
        return supportsDraftMounts() ? TowingProfile.standing(getDraftMountOffset()) : null;
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return new Vec3(14.0D / 16.0D, 0.0D, 21.0D / 16.0D);
    }

    @Override
    protected float operatorViewOffsetDegrees() {
        return OPERATOR_YAW_OFFSET;
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }

    protected record FieldGunGeometry(double pivotHeight, double pivotForward,
                                      double muzzleHeight, double muzzleForward,
                                      float carriageElevationDegrees, float maxDepressionDegrees) {
    }
}
