package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.gameplay.ballistics.EnginePot;
import me.mss1r.siegeworks.gameplay.ballistics.IncendiaryFuse;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.gameplay.ballistics.ScattershotVolley;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.ScalarAnimationCurve;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.axiomata.loading.AutomatedLoadingSession;
import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import java.util.Set;

public class TrebuchetEntity extends AbstractSiegeEntity implements GeoEntity, SiegeAmmunitionControl {
    private static final int PROJECTILE_RELEASE_TICK = 16;
    private static final int SHOOT_ANIMATION_TICKS = 65;
    private static final String TAG_SHOOT_ANIMATION_TICK = "ShootAnimationTick";
    private static final float RELOAD_ANIMATION_TICKS = 323.334F;
    private static final double UNLOADED_ARM_ANGLE = 90.0D;
    private static final double UNLOADED_COUNTERWEIGHT_ANGLE = -90.0D;
    private static final double LOADED_ARM_ANGLE = -22.5D;
    private static final double LOADED_COUNTERWEIGHT_ANGLE = 22.5D;
    private static final double LAUNCH_SLOPE = 1.0D;
    private static final float OPERATOR_VIEW_LIMIT = 45.0F;
    private static final Vec3 OPERATOR_OFFSET = new Vec3(36.0D / 16.0D, 0.0D, 67.0D / 16.0D);
    private static final Vec3 PROJECTILE_RELEASE_OFFSET = new Vec3(0.0D, 25.65D, 10.83D);
    private static final Set<SiegeAmmunitionMode> AUTOMATED_AMMUNITION_MODES = Set.of(
            SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD,
            SiegeAmmunitionMode.INCENDIARY);
    private static final EntityDataAccessor<Integer> SHOOT_ANIMATION_TICK =
            SynchedEntityData.defineId(TrebuchetEntity.class, EntityDataSerializers.INT);
    /** Fuse ticks left on a pot waiting in the sling, or {@link IncendiaryFuse#UNLIT}. */
    private static final EntityDataAccessor<Integer> POT_FUSE =
            SynchedEntityData.defineId(TrebuchetEntity.class, EntityDataSerializers.INT);
    /** Sling pivot on the arm and load centre in the sling, in structure coordinates at rest. */
    private static final Vec3 SLING_PIVOT = new Vec3(0.0D, 114.0D / 16.0D, -253.0D / 16.0D);
    private static final Vec3 LOAD_CENTER = new Vec3(0.0D, 114.0D / 16.0D, -367.0D / 16.0D);
    private static final double LOADED_SLING_ANGLE = -157.5D;
    /**
     * Scale of a pot in the sling pouch. The pouch is 12 px across; at this scale the pot body is 11 px, so it sits
     * inside the pouch walls instead of on them.
     */
    public static final float POT_IN_SLING_SCALE = 1.1F;
    /** Shift of the pot toward the pouch bottom, in pot-model blocks, so its base rests on the bottom. */
    public static final double POT_SLING_OFFSET = 0.12D;
    /** Wick tip and wick base, in pixels of the pot model. */
    private static final double WICK_TIP_PIXELS = 17.0D;
    private static final double WICK_BASE_PIXELS = 14.0D;
    private static final ScalarAnimationCurve RELOAD_ARM = ScalarAnimationCurve.of(
            key(0.0F, 90.0D), key(280.0F, -3.0D), key(320.0F, -22.5D),
            key(RELOAD_ANIMATION_TICKS, -22.5D));
    private static final ScalarAnimationCurve RELOAD_COUNTERWEIGHT = ScalarAnimationCurve.of(
            key(0.0F, -90.0D), key(280.0F, 3.0D), key(320.0F, 22.5D),
            key(RELOAD_ANIMATION_TICKS, 22.5D));
    private static final ScalarAnimationCurve SHOOT_ARM = ScalarAnimationCurve.of(
            key(0.0F, -22.5D), key(5.0F, -20.0D), key(15.0F, 65.0D),
            key(25.0F, 145.0D), key(34.0F, 112.5D), key(44.0F, 82.5D),
            key(54.0F, 94.0D), key(65.0F, 90.0D));
    private static final ScalarAnimationCurve SHOOT_COUNTERWEIGHT = ScalarAnimationCurve.of(
            key(0.0F, 22.5D), key(5.0F, 24.0D), key(15.0F, -50.0D),
            key(25.0F, -125.0D), key(34.0F, -128.0D), key(44.0F, -96.0D),
            key(54.0F, -87.0D), key(65.0F, -90.0D));

    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withReloadSound(SiegeworksSounds.TREBUCHET_RELOAD.get())
            .withFiringSound(SiegeworksSounds.TREBUCHET_SHOOT.get())
            .withReloadRange(32.0)
            .withReloadVolume(1.3F)
            .withFiringRange(220.0D)
            .withFiringVolume(1.0F);

    private static final LoadingRequirement[] AMMO_LOADS = {
            LoadingRequirement.consume(Items.STONE).timedBy("stone"),
            LoadingRequirement.consume(SiegeworksItems.FIRE_PROJECTILE.get()).timedBy("fireProjectile"),
            LoadingRequirement.consume(SiegeworksItems.GRAPESHOT.get()).timedBy("stone")
    };

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private final EnginePot pot = new EnginePot(this, POT_FUSE);
    private final RawAnimation shootAnim = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final RawAnimation reloadAnim = RawAnimation.begin().thenPlayAndHold("reload");
    private final RawAnimation loadedAnim = RawAnimation.begin().thenPlayAndHold("loaded");
    private final RawAnimation unloadedAnim = RawAnimation.begin().thenPlayAndHold("unloaded");
    private double previousCollisionArmAngle = UNLOADED_ARM_ANGLE;
    private double previousCollisionCounterweightAngle = UNLOADED_COUNTERWEIGHT_ANGLE;
    private boolean clientCollisionPoseInitialized;

    public TrebuchetEntity(EntityType<? extends LivingEntity> type, Level level) {
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
        data.define(POT_FUSE, IncendiaryFuse.UNLIT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() { return SOUND_PROFILE; }

    @Override
    public Set<SiegeAmmunitionMode> getSupportedAmmunitionModes() {
        return AUTOMATED_AMMUNITION_MODES;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return getPassengers().isEmpty() && !(entity instanceof Player) && isSupportedDirectOperator(entity);
    }

    @Override
    protected InteractionResult boardForCapture(Player player) {
        return captureStanding(player);
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (continueLoadingAction(player)) return showLoadingProgress(player);
        if (getCooldown() > 0) return showCooldownProgress(player);
        if (pot.light(player, hand, potPoint(alongPot(WICK_TIP_PIXELS)))) {
            return InteractionResult.SUCCESS;
        }

        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.isEmpty()) {
            if (player.isShiftKeyDown() && hasAmmoLoaded() && isWindingComplete()) {
                return cycleShotPower(player);
            }
            if (!hasAmmoLoaded()) {
                return rotateTowardsPlayer(player);
            }
        }

        if (!hasAmmoLoaded()) {
            LoadingRequirement match = findMatchingAmmo(itemStack);

            if (match == null) {
                showAmmoListMessage(player);
                return InteractionResult.FAIL;
            }

            if (!canBeginLoadingRequirement(player, hand, match)) return InteractionResult.FAIL;
            return beginLoadingAction(player, hand, serverLevel, 0, match);
        }

        if (!isWindingComplete()) {
            return showWindingProgress(player);
        }

        beginShot();
        playShootSound(serverLevel);
        setOperator(player);

        return InteractionResult.SUCCESS;
    }

    @Override
    public SiegeActionResult advancePrimaryAction(LivingEntity operator, Container inventory) {
        if (!(level() instanceof ServerLevel serverLevel) || !isOperator(operator)) {
            automatedLoading.reset();
            return SiegeActionResult.DENIED;
        }
        if (hasLoadingAction() || getCooldown() > 0 || !isWindingComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (hasAmmoLoaded()) {
            pot.lightByCrew();
            beginShot();
            playShootSound(serverLevel);
            setOperator(operator);
            return SiegeActionResult.FIRED;
        }

        AutomatedAmmo ammo = findAutomatedAmmo(inventory);
        if (ammo == null || !AutomatedLoadingSession.hasRequiredItem(inventory, ammo.stage(), ammo.item())) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }
        if (automatedLoading.needsStart(operator, 0)) {
            automatedLoading.start(operator, 0, getLoadingRequirementTicks(ammo.stage().timingKey()));
            onLoadingStart(serverLevel);
        }
        if (!automatedLoading.tickComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        String loaded = ammo.ammoKey();
        if (SiegeAmmo.isFireAmmoKey(loaded)) {
            PotFilling sealed = EnginePot.takeSealed(inventory);
            if (sealed == null) {
                automatedLoading.reset();
                return SiegeActionResult.MISSING_AMMUNITION;
            }
            loaded = pot.load(sealed);
        } else if (!AutomatedLoadingSession.finishStage(inventory, ammo.stage(), ammo.item(), operator)) {
            automatedLoading.reset();
            return SiegeActionResult.MISSING_AMMUNITION;
        }

        setAmmoLoaded(loaded);
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(operator);
        automatedLoading.reset();
        return SiegeActionResult.LOADED;
    }

    @Override
    public void cancelPrimaryAction(LivingEntity operator) {
        automatedLoading.cancel(operator);
    }

    @Override
    public SiegeOperationState getOperationState() {
        return automatedLoading.isActive() ? SiegeOperationState.LOADING : super.getOperationState();
    }

    private AutomatedAmmo findAutomatedAmmo(Container inventory) {
        SiegeAmmunitionMode mode = getAutomatedAmmunitionMode();
        if (mode == SiegeAmmunitionMode.INCENDIARY) {
            return findAutomatedFireAmmo(inventory);
        }
        AutomatedAmmo stoneAmmo = findAutomatedStoneAmmo(inventory);
        if (stoneAmmo != null || mode == SiegeAmmunitionMode.STANDARD) {
            return stoneAmmo;
        }
        AutomatedAmmo fireAmmo = findAutomatedFireAmmo(inventory);
        return fireAmmo;
    }

    private AutomatedAmmo findAutomatedStoneAmmo(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (SiegeAmmo.isStoneProjectile(stack)) {
                LoadingRequirement stage = LoadingRequirement.consume(stack.getItem()).timedBy("stone");
                return new AutomatedAmmo(stage, stack.getItem(), SiegeAmmo.stoneAmmoKey(stack.getItem()));
            }
            if (SiegeAmmo.isGrapeshot(stack)) {
                return new AutomatedAmmo(AMMO_LOADS[2], stack.getItem(), SiegeAmmo.AMMO_GRAPESHOT);
            }
        }
        return null;
    }

    private AutomatedAmmo findAutomatedFireAmmo(Container inventory) {
        Item fireProjectile = SiegeworksItems.FIRE_PROJECTILE.get();
        if (EnginePot.hasSealed(inventory)) {
            return new AutomatedAmmo(AMMO_LOADS[1], fireProjectile, SiegeAmmo.AMMO_FIRE);
        }
        return null;
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
        return 40;
    }

    private InteractionResult rotateTowardsPlayer(Player player) {
        float currentYaw = getTrackedYaw();
        float delta = Mth.wrapDegrees(player.getYRot() - currentYaw);
        float nextYaw = currentYaw + Mth.clamp(delta, -10.0f, 10.0f);
        applyYaw(nextYaw);
        player.displayClientMessage(Component.translatable("siege.rotation.adjusting"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        ItemStack itemStack = player.getItemInHand(hand);
        PotFilling held = PotFilling.of(itemStack);
        LoadingRequirement match = findMatchingAmmo(itemStack);

        if (match == null || !itemStack.is(getActiveLoadingItem())) {
            player.displayClientMessage(Component.translatable("siege.loading.cancelled"), true);
            return;
        }

        if (!consumeLoadingRequirement(player, hand, match)) return;

        if (SiegeAmmo.isStoneProjectile(match.item())) {
            setAmmoLoaded(SiegeAmmo.stoneAmmoKey(match.item()));
        } else if (match.item() == SiegeworksItems.GRAPESHOT.get()) {
            setAmmoLoaded(SiegeAmmo.AMMO_GRAPESHOT);
        } else {
            setAmmoLoaded(pot.load(held));
            if (!held.canLight()) {
                player.displayClientMessage(Component.translatable("siege.pot.loaded_unsealed"), true);
            }
        }
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(player);
    }

    private LoadingRequirement findMatchingAmmo(ItemStack stack) {
        if (SiegeAmmo.isStoneProjectile(stack)) {
            return LoadingRequirement.consume(stack.getItem()).timedBy("stone");
        }
        if (PotFilling.isPot(stack)) {
            return LoadingRequirement.consume(stack.getItem()).timedBy("fireProjectile");
        }
        for (LoadingRequirement stage : AMMO_LOADS) {
            if (stack.is(stage.item())) return stage;
        }
        return null;
    }

    private void showAmmoListMessage(Player player) {
        Component ammoList = SiegeAmmo.stoneAmmoDescription()
                .copy().append(", ").append(AMMO_LOADS[2].item().getDefaultInstance().getHoverName())
                .append(", ").append(AMMO_LOADS[1].item().getDefaultInstance().getHoverName());
        player.displayClientMessage(Component.translatable("siege.loading.need_one_of", ammoList), true);
    }

    @Override
    public void onSiegeTick(ServerLevel serverLevel) {
        pot.burn(serverLevel, () -> potPoint(0.0D), SiegeworksEntities.TREBUCHET_PROJECTILE.get(),
                ProjectileVariants.TREBUCHET_FIRE_PROJECTILE);
        int animationTick = getShootAnimationTick();
        if (animationTick < 0) {
            return;
        }
        animationTick++;
        if (hasAmmoLoaded() && animationTick == PROJECTILE_RELEASE_TICK) {
            fireTrebuchet(serverLevel);
        }
        setShootAnimationTick(animationTick >= SHOOT_ANIMATION_TICKS ? -1 : animationTick);
    }

    @Override
    protected String getCooldownStatusKey() {
        return hasAmmoLoaded() ? "siege.loading.state.firing" : super.getCooldownStatusKey();
    }

    private void fireTrebuchet(ServerLevel serverLevel) {
        Vec3 releasePosition = position().add(rotateModelOffset(PROJECTILE_RELEASE_OFFSET));

        double blocksPerTick = getLaunchSpeed();
        float accuracyDegrees = getAccuracyMultiplier();
        float yawOffset = (random.nextFloat() - 0.5f) * 4 * accuracyDegrees;
        float adjustedYaw = getTrackedYaw() + yawOffset;
        float yawRad = (float) Math.toRadians(adjustedYaw);

        Vec3 direction = new Vec3(-Math.sin(yawRad), LAUNCH_SLOPE, Math.cos(yawRad)).normalize();
        Vec3 launchVelocity = direction.scale(blocksPerTick * getShotPower());

        if (SiegeAmmo.isGrapeshotAmmoKey(getAmmoLoaded())) {
            var scattershot = getScattershotProfile();
            int pelletCount = scattershot.minPellets()
                    + random.nextInt(scattershot.maxPellets() - scattershot.minPellets() + 1);
            ScattershotVolley.spawn(serverLevel, this, releasePosition, launchVelocity,
                    pelletCount, scattershot.spreadDegrees(), scattershot.baseDamagePerPellet(), true);
            setAmmoLoaded("");
            return;
        }

        TrebuchetProjectile projectile = new TrebuchetProjectile(SiegeworksEntities.TREBUCHET_PROJECTILE.get(), this, serverLevel);
        projectile.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
        projectile.setDeltaMovement(launchVelocity);

        projectile.setBaseDamage(getBaseDamage());
        projectile.setOwner(this);

        String ammo = getAmmoLoaded();
        if (SiegeAmmo.isStoneAmmoKey(ammo)) {
            projectile.setTextureName(ammo);
        } else if (SiegeAmmo.isFireAmmoKey(ammo)) {
            projectile.setPhysicsProfile(ProjectileVariants.TREBUCHET_FIRE_PROJECTILE);
            pot.throwWith(projectile);
        }

        serverLevel.addFreshEntity(projectile);
        setAmmoLoaded("");
    }

    private Vec3 rotateModelOffset(Vec3 offset) {
        float yawRad = getTrackedYaw() * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double rightX = Mth.cos(yawRad);
        double rightZ = Mth.sin(yawRad);
        return new Vec3(
                rightX * offset.x - forwardX * offset.z,
                offset.y,
                rightZ * offset.x - forwardZ * offset.z);
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        beginShot();
        playShootSound(serverLevel);
        setOperator(operator);
        return true;
    }

    private void beginShot() {
        triggerAnimation("shoot");
        setShootAnimationTick(0);
        startRecovery();
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            if (!clientCollisionPoseInitialized) {
                captureCollisionPose();
                clientCollisionPoseInitialized = true;
            }
            super.tick();
            StructureMotionSystem.tickStructure(this);
            captureCollisionPose();
            // Mid-swing the pot follows the sling; sparks continue on the projectile after release.
            if (pot.isLit() && getShootAnimationTick() < 0) {
                pot.sparkle(level(), potPoint(alongPot(WICK_TIP_PIXELS)),
                        potPoint(alongPot(WICK_BASE_PIXELS)));
            }
            return;
        }

        captureCollisionPose();
        super.tick();
        StructureMotionSystem.tickStructure(this);
    }

    /**
     * Distance from the load centre toward the arm of a pot-model height, matching the renderer: the pot lies along the
     * sling with its base on the pouch bottom and its wick toward the arm.
     */
    private static double alongPot(double modelPixels) {
        return POT_IN_SLING_SCALE * (modelPixels / 16.0D - 0.25D - POT_SLING_OFFSET);
    }

    /** World position {@code along} blocks from the load centre toward the arm, with the sling at rest. */
    public Vec3 potPoint(double along) {
        CollisionPose armPose = authoredRotation(GeneratedCollisionShapes.TREBUCHET_ARM.pivot(), getCollisionArmAngle());
        CollisionPose slingPose = authoredRotation(SLING_PIVOT, LOADED_SLING_ANGLE).then(armPose);
        return collisionTransform().toWorld(slingPose.toStructure(LOAD_CENTER.add(0.0D, 0.0D, along)));
    }

    private void captureCollisionPose() {
        previousCollisionArmAngle = getCollisionArmAngle();
        previousCollisionCounterweightAngle = getCollisionCounterweightAngle();
    }

    public int getShootAnimationTick() {
        return entityData.get(SHOOT_ANIMATION_TICK);
    }

    private void setShootAnimationTick(int tick) {
        entityData.set(SHOOT_ANIMATION_TICK, tick);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_SHOOT_ANIMATION_TICK, getShootAnimationTick());
        pot.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setShootAnimationTick(tag.contains(TAG_SHOOT_ANIMATION_TICK)
                ? Mth.clamp(tag.getInt(TAG_SHOOT_ANIMATION_TICK), -1, SHOOT_ANIMATION_TICKS - 1)
                : -1);
        pot.load(tag);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getCollisionArmAngle(), getCollisionCounterweightAngle());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionArmAngle, previousCollisionCounterweightAngle);
    }

    private static List<CollisionGroup> collisionGroups(double armAngle, double counterweightAngle) {
        CollisionPose armPose = authoredRotation(GeneratedCollisionShapes.TREBUCHET_ARM.pivot(), armAngle);
        CollisionPose counterweightPose = authoredRotation(
                GeneratedCollisionShapes.TREBUCHET_COUNTERWEIGHT.pivot(), counterweightAngle).then(armPose);
        return List.of(
                new CollisionGroup("body", GeneratedCollisionShapes.TREBUCHET_BODY, CollisionPose.IDENTITY),
                new CollisionGroup("arm", GeneratedCollisionShapes.TREBUCHET_ARM, armPose),
                new CollisionGroup("counterweight", GeneratedCollisionShapes.TREBUCHET_COUNTERWEIGHT,
                        counterweightPose));
    }

    private double getCollisionArmAngle() {
        int shootTick = getShootAnimationTick();
        if (shootTick >= 0) {
            return SHOOT_ARM.sample(shootTick);
        }
        if (!hasAmmoLoaded()) {
            return UNLOADED_ARM_ANGLE;
        }
        return getWindingTime() > 0 ? RELOAD_ARM.sample(getReloadAnimationTick()) : LOADED_ARM_ANGLE;
    }

    private double getCollisionCounterweightAngle() {
        int shootTick = getShootAnimationTick();
        if (shootTick >= 0) {
            return SHOOT_COUNTERWEIGHT.sample(shootTick);
        }
        if (!hasAmmoLoaded()) {
            return UNLOADED_COUNTERWEIGHT_ANGLE;
        }
        return getWindingTime() > 0
                ? RELOAD_COUNTERWEIGHT.sample(getReloadAnimationTick())
                : LOADED_COUNTERWEIGHT_ANGLE;
    }

    private float getReloadAnimationTick() {
        int total = getWindingTotal();
        if (total <= 0) {
            return RELOAD_ANIMATION_TICKS;
        }
        double progress = 1.0D - Mth.clamp((double) getWindingTime() / total, 0.0D, 1.0D);
        return (float) (progress * RELOAD_ANIMATION_TICKS);
    }

    private static CollisionPose authoredRotation(Vec3 pivot, double degrees) {
        return CollisionPose.fromGeckoBoneX(pivot, (float) Math.toRadians(-degrees));
    }

    private static ScalarAnimationCurve.Keyframe key(float tick, double value) {
        return ScalarAnimationCurve.key(tick, value, ScalarAnimationCurve.Interpolation.LINEAR);
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return position().add(rotateModelOffset(PROJECTILE_RELEASE_OFFSET));
    }

    @Override
    public float calculateAutomatedAimPitch(Vec3 target) {
        return 0.0F;
    }

    @Override
    public void prepareAutomatedShot(Vec3 target) {
        double power = getRequiredShotPower(target);
        if (Double.isFinite(power)) {
            setShotPower((float) power);
        }
    }

    @Override
    public boolean canReachAutomatedTarget(Vec3 target) {
        double power = getRequiredShotPower(target);
        return Double.isFinite(power) && power >= getMinShotPower() && power <= getMaxShotPower();
    }

    private double getRequiredShotPower(Vec3 target) {
        return SiegeBallistics.calculateFixedArcPower(getAutomatedAimOrigin(), target,
                getLaunchSpeed(), LAUNCH_SLOPE, ballisticFlight(SiegeworksEntities.TREBUCHET_PROJECTILE.get()),
                getMinShotPower(), getMaxShotPower());
    }

    @Override
    protected boolean usesIndependentAimYaw() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "anim_controller", state -> {
            if (hasAmmoLoaded() && getWindingTime() > 0) {
                state.setAnimation(reloadAnim);
            } else if (hasAmmoLoaded()) {
                state.setAnimation(loadedAnim);
            } else {
                state.setAnimation(unloadedAnim);
            }
            return PlayState.CONTINUE;
        })
                .triggerableAnim("shoot", shootAnim)
                .setAnimationSpeedHandler(animatable -> animatable.hasAmmoLoaded() && animatable.getWindingTime() > 0
                        ? 323.3334D / Math.max(1, animatable.getWindingTotal())
                        : 1.0D)
                .triggerableAnim("loaded", loadedAnim)
                .triggerableAnim("unloaded", unloadedAnim));
    }

    @Override
    public void triggerAnimation(String name) {
        switch (name) {
            case "shoot" -> triggerAnim("anim_controller", "shoot");
            case "reload" -> triggerAnim("anim_controller", "reload");
            case "loaded" -> triggerAnim("anim_controller", "loaded");
            case "unloaded" -> triggerAnim("anim_controller", "unloaded");
        }
    }

    @Override
    public void stopAnimation(String name) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        return OPERATOR_OFFSET;
    }

    @Override
    protected float operatorViewOffsetDegrees() {
        return 90.0F;
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
    public double getVelocity(Entity entity) {
        return 0.0;
    }

    @Override
    public Vec3 getPlayerPOV() {
        return Vec3.ZERO;
    }

    private record AutomatedAmmo(LoadingRequirement stage, Item item, String ammoKey) {
    }
}
