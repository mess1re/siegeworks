package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.MangonelPassengerProjectile;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.tags.TagKey;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MangonelEntity extends AbstractSiegeEntity implements GeoEntity, SiegeAmmunitionControl,
        MountedSiegeItemControl {
    private static final String TAG_LAUNCH_PAYLOAD = "LaunchPayload";
    private static final String TAG_SHOOT_ANIMATION_TICK = "ShootAnimationTick";
    private static final int SHOOT_ANIMATION_TICKS = 18;
    private static final float RELOAD_ANIMATION_TICKS = 100.0F;
    private static final float OPERATOR_VIEW_LIMIT = 35.0F;
    private static final float PAYLOAD_VIEW_LIMIT = 80.0F;
    private static final double UNLOADED_ARM_ANGLE = 0.0D;
    private static final double LOADED_ARM_ANGLE = -60.0D;
    private static final TagKey<Item> PASSENGER_LOADING_ITEMS = TagKey.create(Registries.ITEM,
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "mangonel_passenger_loading_items"));
    private static final Vec3 ARM_PIVOT = new Vec3(0.0D, 9.0D / 16.0D, -7.0D / 16.0D);
    private static final Vec3 LOAD_CENTER = new Vec3(0.0D, 44.0D / 16.0D, -6.0D / 16.0D);
    private static final Vec3 ROOT_COLLISION_PIVOT = new Vec3(0.0D, 6.0D / 16.0D, 0.0D);
    /** Size of the block a load is rendered as in the cup. */
    private static final double LOAD_DRAWN_SIZE = 0.96D * 0.5D;
    /**
     * Scale of a pot in the cup, about its bottom. The pot body is 10 px wide in its block model and is rendered at 9.6
     * px over a stone-sized load; the cup is 8 px wide.
     */
    public static final float POT_IN_CUP_SCALE = 8.0F / 9.6F;
    private static final double LAUNCH_SLOPE = 0.60D;
    private static final Set<SiegeAmmunitionMode> AUTOMATED_AMMUNITION_MODES = Set.of(
            SiegeAmmunitionMode.AUTO, SiegeAmmunitionMode.STANDARD, SiegeAmmunitionMode.INCENDIARY);
    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withReloadSound(SiegeworksSounds.MANGONEL_RELOAD.get())
            .withFiringSound(SiegeworksSounds.MANGONEL_SHOOT.get())
            .withMovementInterval(150)
            .withMovementRange(30.0)
            .withReloadRange(15.0)
            .withReloadVolume(0.6F)
            .withFiringRange(180.0D)
            .withFiringVolume(0.9F);

    private static final LoadingRequirement[] AMMO_LOADS = {
            LoadingRequirement.consume(Items.STONE).timedBy("stone"),
            LoadingRequirement.consume(SiegeworksItems.FIRE_PROJECTILE.get()).timedBy("fireProjectile"),
            LoadingRequirement.consume(SiegeworksItems.GRAPESHOT.get()).timedBy("stone")
    };
    private static final EntityDataAccessor<Optional<UUID>> LAUNCH_PAYLOAD =
            SynchedEntityData.defineId(MangonelEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SHOOT_ANIMATION_TICK =
            SynchedEntityData.defineId(MangonelEntity.class, EntityDataSerializers.INT);
    /** Fuse ticks left on a pot waiting in the cup, or {@link IncendiaryFuse#UNLIT}. */
    private static final EntityDataAccessor<Integer> POT_FUSE =
            SynchedEntityData.defineId(MangonelEntity.class, EntityDataSerializers.INT);
    private static final ScalarAnimationCurve RELOAD_ARM = ScalarAnimationCurve.of(
            key(0.0F, UNLOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR),
            key(RELOAD_ANIMATION_TICKS, LOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR));
    private static final ScalarAnimationCurve SHOOT_ARM = ScalarAnimationCurve.of(
            key(0.0F, LOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.LINEAR),
            key(5.0F, UNLOADED_ARM_ANGLE, ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC));
    private static final ScalarAnimationCurve SHOOT_ROOT = ScalarAnimationCurve.of(
            key(0.0F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(2.5F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(4.166F, -1.0D, ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC),
            key(5.834F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR),
            key(17.5F, 0.0D, ScalarAnimationCurve.Interpolation.LINEAR));
    /**
     * Shoot-animation tick at which the cup releases its load: where the cup's path is parallel to the launch line. The
     * load spawns on the next whole tick, advanced by the distance flown since.
     */
    private static final float RELEASE_SHOOT_TICK = releaseShootTick();
    private static final int PROJECTILE_RELEASE_TICK = Mth.ceil(RELEASE_SHOOT_TICK);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final AutomatedLoadingSession automatedLoading = new AutomatedLoadingSession();
    private final EnginePot pot = new EnginePot(this, POT_FUSE);
    private boolean transferringLaunchPayload;
    private final RawAnimation shootAnim = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    private final RawAnimation reloadingAnim = RawAnimation.begin().thenPlayAndHold("reloading");
    private final RawAnimation loadedAnim = RawAnimation.begin().thenPlayAndHold("loaded");
    private final RawAnimation unloadedAnim = RawAnimation.begin().thenPlayAndHold("unloaded");
    private double previousCollisionRootAngle;
    private double previousCollisionArmAngle = UNLOADED_ARM_ANGLE;

    public MangonelEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.075)
                .add(Attributes.KNOCKBACK_RESISTANCE, 265);
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
        data.define(LAUNCH_PAYLOAD, Optional.empty());
        data.define(SHOOT_ANIMATION_TICK, -1);
        data.define(POT_FUSE, IncendiaryFuse.UNLIT);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(LAUNCH_PAYLOAD).ifPresent(uuid -> tag.putUUID(TAG_LAUNCH_PAYLOAD, uuid));
        tag.putInt(TAG_SHOOT_ANIMATION_TICK, getShootAnimationTick());
        pot.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(LAUNCH_PAYLOAD,
                tag.hasUUID(TAG_LAUNCH_PAYLOAD) ? Optional.of(tag.getUUID(TAG_LAUNCH_PAYLOAD)) : Optional.empty());
        setShootAnimationTick(tag.contains(TAG_SHOOT_ANIMATION_TICK)
                ? Mth.clamp(tag.getInt(TAG_SHOOT_ANIMATION_TICK), -1, SHOOT_ANIMATION_TICKS - 1)
                : -1);
        pot.load(tag);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() { return SOUND_PROFILE; }

    @Override
    protected int getMoveSoundIntervalTicks() {
        return 28;
    }

    @Override
    public Set<SiegeAmmunitionMode> getSupportedAmmunitionModes() {
        return AUTOMATED_AMMUNITION_MODES;
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        if (isLaunchPayload(player)) return InteractionResult.FAIL;
        if (continueLoadingAction(player)) return showLoadingProgress(player);
        if (getCooldown() > 0) return showCooldownProgress(player);

        ItemStack itemStack = player.getItemInHand(hand);
        boolean hasLaunchLoad = hasLaunchLoad();
        if (pot.light(player, hand, potPoint(alongPotInCup(IncendiaryFuse.WICK_TIP)))) return InteractionResult.SUCCESS;

        LivingEntity operator = getControllingPassenger();
        if (operator != null && operator != player) return InteractionResult.FAIL;

        if (itemStack.isEmpty() && !player.isShiftKeyDown() && hasLaunchPayload()
                && operator == null && canAddPassenger(player)) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (itemStack.isEmpty() && player.isShiftKeyDown() && hasLaunchLoad && isWindingComplete()) {
            return cycleShotPower(player);
        }

        if (itemStack.isEmpty() && !hasLaunchLoad && canAddPassenger(player) && !player.isShiftKeyDown()) {
            player.startRiding(this);
            setOperator(player);
            return InteractionResult.SUCCESS;
        }

        if (!hasLaunchLoad) {
            if (itemStack.is(PASSENGER_LOADING_ITEMS)) {
                LoadingRequirement passengerStage = LoadingRequirement.consume(itemStack.getItem()).timedBy("stone");
                return beginLoadingAction(player, hand, serverLevel, 0, passengerStage);
            }

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

        return beginShot(serverLevel, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public boolean requestRiderFire(ServerPlayer player) {
        boolean allowedOperator = isOperator(player) && !isBeingCaptured();
        boolean allowedPayload = isLaunchPayload(player) && player.isCreative();
        return (allowedOperator || allowedPayload) && beginShot(player.serverLevel(), player);
    }

    private boolean beginShot(ServerLevel serverLevel, LivingEntity operator) {
        if (!hasLaunchLoad() || !isWindingComplete() || getCooldown() > 0) return false;
        startShot(serverLevel, operator);
        return true;
    }

    private void startShot(ServerLevel serverLevel, LivingEntity operator) {
        triggerAnimation("shoot");
        setShootAnimationTick(0);
        startRecovery();
        playShootSound(serverLevel);
        setOperator(operator);
    }

    @Override
    public boolean acceptsMountedItem(Player player, InteractionHand hand) {
        if (!isOperator(player)) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (hasLoadingAction()) {
            return stack.is(getActiveLoadingItem());
        }
        if (IncendiaryFuse.canStrike(stack) && pot.awaitsFlame()) {
            return true;
        }
        return !hasAmmoLoaded() && findMatchingAmmo(stack) != null;
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
        if (hasLoadingAction() || getCooldown() > 0 || !isWindingComplete()) {
            return SiegeActionResult.IN_PROGRESS;
        }
        if (hasLaunchLoad()) {
            pot.lightByCrew();
            beginShot(serverLevel, operator);
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
        if (automatedLoading.isActive()) return SiegeOperationState.LOADING;
        if (!hasLaunchPayload()) return super.getOperationState();
        if (getCooldown() > 0) return SiegeOperationState.COOLDOWN;
        return isWindingComplete() ? SiegeOperationState.READY : SiegeOperationState.WINDING;
    }

    private AutomatedAmmo findAutomatedAmmo(Container inventory) {
        if (getAutomatedAmmunitionMode() == SiegeAmmunitionMode.INCENDIARY) {
            return findAutomatedFireAmmo(inventory);
        }
        AutomatedAmmo stoneAmmo = findAutomatedStoneAmmo(inventory);
        if (stoneAmmo != null || getAutomatedAmmunitionMode() == SiegeAmmunitionMode.STANDARD) {
            return stoneAmmo;
        }
        return findAutomatedFireAmmo(inventory);
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
        return getLoadingRequirementTicks("winding");
    }

    @Override
    protected void playReloadSound(ServerLevel serverLevel) {
        playSoundToNearbyPlayers(serverLevel, getReloadSound(), getSoundProfile().reload().range(),
                getSoundProfile().reload().volume(), 1.0F, 0.0F);
    }

    @Override
    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
        ItemStack itemStack = player.getItemInHand(hand);
        PotFilling held = PotFilling.of(itemStack);

        if (itemStack.is(PASSENGER_LOADING_ITEMS) && itemStack.is(getActiveLoadingItem())) {
            if (!boardLaunchPayload(player)) {
                player.displayClientMessage(Component.translatable("siege.loading.cancelled"), true);
            }
            return;
        }

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
        pot.burn(serverLevel, () -> potPoint(0.0D), SiegeworksEntities.MANGONEL_PROJECTILE.get(),
                ProjectileVariants.MANGONEL_FIRE_PROJECTILE);
        int animationTick = getShootAnimationTick();
        if (animationTick < 0) {
            return;
        }
        animationTick++;
        if (hasLaunchLoad() && animationTick == PROJECTILE_RELEASE_TICK) {
            fireMangonel(serverLevel);
        }
        setShootAnimationTick(animationTick >= SHOOT_ANIMATION_TICKS ? -1 : animationTick);
    }

    @Override
    protected String getCooldownStatusKey() {
        return hasLaunchLoad() ? "siege.loading.state.firing" : super.getCooldownStatusKey();
    }

    private void fireMangonel(ServerLevel serverLevel) {
        Vec3 launchVelocity = createLaunchVelocity();
        Entity launchPayload = getLaunchPayload();
        if (launchPayload instanceof LivingEntity livingPayload) {
            setAmmoLoaded("");
            launchPassenger(serverLevel, livingPayload, launchVelocity);
            return;
        }

        Vec3 releasePosition = releasedLoadPosition(launchVelocity);
        if (SiegeAmmo.isGrapeshotAmmoKey(getAmmoLoaded())) {
            var scattershot = getScattershotProfile();
            int pelletCount = scattershot.minPellets()
                    + random.nextInt(scattershot.maxPellets() - scattershot.minPellets() + 1);
            ScattershotVolley.spawn(serverLevel, this, releasePosition, launchVelocity,
                    pelletCount, scattershot.spreadDegrees(), scattershot.baseDamagePerPellet(), true);
            setAmmoLoaded("");
            return;
        }

        TrebuchetProjectile projectile = new TrebuchetProjectile(SiegeworksEntities.MANGONEL_PROJECTILE.get(), this, serverLevel);
        projectile.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
        projectile.setDeltaMovement(launchVelocity);

        projectile.setBaseDamage(getBaseDamage());
        projectile.setOwner(this);

        String ammo = getAmmoLoaded();
        if (SiegeAmmo.isStoneAmmoKey(ammo)) {
            projectile.setTextureName(ammo);
        } else if (SiegeAmmo.isFireAmmoKey(ammo)) {
            projectile.setPhysicsProfile(ProjectileVariants.MANGONEL_FIRE_PROJECTILE);
            pot.throwWith(projectile);
        }

        serverLevel.addFreshEntity(projectile);
        setAmmoLoaded("");
    }

    private void launchPassenger(ServerLevel serverLevel, LivingEntity passenger, Vec3 launchVelocity) {
        MangonelPassengerProjectile carrier = new MangonelPassengerProjectile(
                SiegeworksEntities.MANGONEL_PASSENGER_PROJECTILE.get(), this, serverLevel);
        Vec3 releasePosition = releasedLoadPosition(launchVelocity);
        carrier.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
        carrier.setDeltaMovement(launchVelocity);
        carrier.setOwner(this);
        serverLevel.addFreshEntity(carrier);

        transferringLaunchPayload = true;
        passenger.stopRiding();
        transferringLaunchPayload = false;
        entityData.set(LAUNCH_PAYLOAD, Optional.empty());

        if (!passenger.startRiding(carrier, true)) {
            passenger.setPos(releasePosition.x, releasePosition.y, releasePosition.z);
            passenger.setDeltaMovement(launchVelocity);
            passenger.hurtMarked = true;
            carrier.discard();
        }
    }

    private Vec3 createLaunchVelocity() {
        double blocksPerTick = getLaunchSpeed();
        float yawOffset = (random.nextFloat() - 0.5F) * 4.0F * getAccuracyMultiplier();
        float yawRad = (getVisualRotationYInDegrees() + yawOffset) * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(-Mth.sin(yawRad), LAUNCH_SLOPE, Mth.cos(yawRad)).normalize();
        return direction.scale(blocksPerTick * getShotPower());
    }

    @Override
    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        startShot(serverLevel, operator);
        return true;
    }

    @Override
    public Vec3 getAutomatedAimOrigin() {
        return getReleasePoint();
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
                getLaunchSpeed(), LAUNCH_SLOPE, ballisticFlight(SiegeworksEntities.MANGONEL_PROJECTILE.get()),
                getMinShotPower(), getMaxShotPower());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "anim_controller", state -> {
            if (hasLaunchLoad() && getWindingTime() > 0) {
                state.setAnimation(reloadingAnim);
            } else if (hasLaunchLoad()) {
                state.setAnimation(loadedAnim);
            } else {
                state.setAnimation(unloadedAnim);
            }
            return PlayState.CONTINUE;
        })
                .triggerableAnim("shoot", shootAnim)
                .setAnimationSpeedHandler(animatable -> animatable.hasLaunchLoad() && animatable.getWindingTime() > 0
                        ? 100.0D / Math.max(1, animatable.getWindingTotal())
                        : 1.0D)
                .triggerableAnim("loaded", loadedAnim)
                .triggerableAnim("unloaded", unloadedAnim));
    }

    @Override
    public void triggerAnimation(String name) {
        switch (name) {
            case "shoot" -> triggerAnim("anim_controller", "shoot");
            case "reloading" -> triggerAnim("anim_controller", "reloading");
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
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.drawnFromBehind(4.25D).turningAbout(18.0D / 16.0D);
    }

    @Override
    public void tick() {
        previousCollisionRootAngle = getCurrentRootAngle();
        previousCollisionArmAngle = getCurrentArmAngle();
        super.tick();
        StructureMotionSystem.tickStructure(this);
        if (level().isClientSide && pot.isLit()) {
            pot.sparkle(level(), potPoint(alongPotInCup(IncendiaryFuse.WICK_TIP)),
                    potPoint(alongPotInCup(IncendiaryFuse.WICK_BASE)));
        }
    }

    /**
     * World position of a point on the pot in the cup, {@code along} blocks from its centre toward the cup's mouth. The
     * pot sits bottom-down with the wick pointing out.
     */
    private Vec3 potPoint(double along) {
        Vec3 inCup = armPointForAuthoredAngle(LOAD_CENTER.add(0.0D, 0.0D, -along), getCurrentArmAngle());
        return position().add(rotateModelOffset(applyRootAngle(inCup, getCurrentRootAngle())));
    }

    public int getShootAnimationTick() {
        return entityData.get(SHOOT_ANIMATION_TICK);
    }

    private void setShootAnimationTick(int tick) {
        entityData.set(SHOOT_ANIMATION_TICK, tick);
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return collisionGroups(getCurrentRootAngle(), getCurrentArmAngle());
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return collisionGroups(previousCollisionRootAngle, previousCollisionArmAngle);
    }

    private static List<CollisionGroup> collisionGroups(double rootAngle, double armAngle) {
        CollisionPose rootPose = authoredRotation(ROOT_COLLISION_PIVOT, rootAngle);
        CollisionPose armPose = authoredRotation(GeneratedCollisionShapes.MANGONEL_ARM.pivot(), armAngle)
                .then(rootPose);
        return List.of(
                new CollisionGroup("body", GeneratedCollisionShapes.MANGONEL_BODY, rootPose),
                new CollisionGroup("arm", GeneratedCollisionShapes.MANGONEL_ARM, armPose));
    }

    private double getCurrentRootAngle() {
        int shootTick = getShootAnimationTick();
        return shootTick >= 0 ? SHOOT_ROOT.sample(shootTick) : 0.0D;
    }

    private double getCurrentArmAngle() {
        int shootTick = getShootAnimationTick();
        if (shootTick >= 0) {
            return getShootArmAngle(shootTick);
        }
        if (!hasLaunchLoad()) {
            return UNLOADED_ARM_ANGLE;
        }
        return getWindingTime() > 0 ? RELOAD_ARM.sample(getReloadAnimationTick()) : LOADED_ARM_ANGLE;
    }

    private static double getShootArmAngle(float tick) {
        return SHOOT_ARM.sample(tick);
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

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        if (isLaunchPayload(entity)) {
            return applyRootAngle(getLoadOffsetForAuthoredAngle(getCurrentArmAngle()), getCurrentRootAngle())
                    .add(0.0D, MangonelPassengerProjectile.PASSENGER_Y_OFFSET, 0.0D);
        }
        return new Vec3(0.0D, 0.0D, 2.5D);
    }

    @Override
    public float getPassengerViewYawLimit(Entity passenger) {
        return isLaunchPayload(passenger) ? PAYLOAD_VIEW_LIMIT : OPERATOR_VIEW_LIMIT;
    }

    @Override
    public float getPassengerViewPitchLimit(Entity passenger) {
        return isLaunchPayload(passenger) ? PAYLOAD_VIEW_LIMIT : OPERATOR_VIEW_LIMIT;
    }

    @Override
    public Vec3 getTowPivotOffset() {
        return isTowed() ? new Vec3(0.0D, 0.0D, 18.0D / 16.0D) : Vec3.ZERO;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        if (isLaunchPayload(entity)) return getLaunchPayload() == null;
        return isSupportedDirectOperator(entity) && getControllingPassenger() == null;
    }

    @Override
    public boolean canOperate(LivingEntity operator) {
        return !isLaunchPayload(operator)
                && isSupportedDirectOperator(operator)
                && (getControllingPassenger() == null || hasPassenger(operator));
    }

    @Override
    protected boolean operatorControlsRotation(Entity passenger) {
        return !isLaunchPayload(passenger) && super.operatorControlsRotation(passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean launchPayload = isLaunchPayload(passenger);
        super.removePassenger(passenger);
        if (launchPayload && !transferringLaunchPayload) {
            entityData.set(LAUNCH_PAYLOAD, Optional.empty());
            setWindingTime(0);
        }
    }

    public boolean isLaunchPayload(Entity entity) {
        return entity != null && entityData.get(LAUNCH_PAYLOAD)
                .map(entity.getUUID()::equals)
                .orElse(false);
    }

    public boolean hasLaunchPayload() {
        return getLaunchPayload() != null;
    }

    private boolean hasLaunchLoad() {
        return hasAmmoLoaded() || hasLaunchPayload();
    }

    private Entity getLaunchPayload() {
        Optional<UUID> payloadUuid = entityData.get(LAUNCH_PAYLOAD);
        if (payloadUuid.isEmpty()) return null;
        for (Entity passenger : getPassengers()) {
            if (payloadUuid.get().equals(passenger.getUUID())) return passenger;
        }
        return null;
    }

    private boolean boardLaunchPayload(Player player) {
        if (hasLaunchLoad() || player.getVehicle() != null) return false;
        entityData.set(LAUNCH_PAYLOAD, Optional.of(player.getUUID()));
        if (!player.startRiding(this)) {
            entityData.set(LAUNCH_PAYLOAD, Optional.empty());
            return false;
        }
        setWindingTime(getLoadingRequirementTicks("winding"));
        setOperator(player);
        return true;
    }

    private static Vec3 getLoadOffsetForAuthoredAngle(double armAngleDegrees) {
        return armPointForAuthoredAngle(LOAD_CENTER, armAngleDegrees);
    }

    /**
     * Offset from the load centre of the point {@code along} from the centre of a stone-sized pot, after the in-cup
     * scale.
     */
    private static double alongPotInCup(double along) {
        double bottom = -0.5D;
        return LOAD_DRAWN_SIZE * (bottom + POT_IN_CUP_SCALE * (along - bottom));
    }

    /** Position of an arm point, given with the arm upright in the model, at the given arm angle. */
    private static Vec3 armPointForAuthoredAngle(Vec3 point, double armAngleDegrees) {
        double angle = -armAngleDegrees * Mth.DEG_TO_RAD;
        double relativeY = point.y - ARM_PIVOT.y;
        double relativeZ = point.z - ARM_PIVOT.z;
        return new Vec3(
                point.x,
                ARM_PIVOT.y + relativeY * Math.cos(angle) - relativeZ * Math.sin(angle),
                ARM_PIVOT.z + relativeY * Math.sin(angle) + relativeZ * Math.cos(angle));
    }

    private static Vec3 getLoadOffsetAtShootTick(float tick) {
        return applyRootAngle(getLoadOffsetForAuthoredAngle(getShootArmAngle(tick)), SHOOT_ROOT.sample(tick));
    }

    /** Cup climb angle at {@code tick} of the swing, in degrees above horizontal, in the throw direction. */
    private static double cupClimbAt(float tick) {
        Vec3 path = getLoadOffsetAtShootTick(tick + 0.01F).subtract(getLoadOffsetAtShootTick(tick - 0.01F));
        // The model faces its own negative z.
        return Math.toDegrees(Math.atan2(path.y, -path.z));
    }

    private static float releaseShootTick() {
        double launch = Math.toDegrees(Math.atan(LAUNCH_SLOPE));
        float early = 0.01F;
        float late = 5.0F;
        for (int i = 0; i < 40; i++) {
            float middle = (early + late) * 0.5F;
            if (cupClimbAt(middle) > launch) {
                early = middle;
            } else {
                late = middle;
            }
        }
        return (early + late) * 0.5F;
    }

    /** Load position at spawn: the release point, advanced by {@code velocity} for the time since release. */
    private Vec3 releasedLoadPosition(Vec3 velocity) {
        return getReleasePoint().add(velocity.scale(PROJECTILE_RELEASE_TICK - RELEASE_SHOOT_TICK));
    }

    private Vec3 getReleasePoint() {
        return position().add(rotateModelOffset(getLoadOffsetAtShootTick(RELEASE_SHOOT_TICK)));
    }

    private static Vec3 applyRootAngle(Vec3 offset, double rootAngleDegrees) {
        return authoredRotation(ROOT_COLLISION_PIVOT, rootAngleDegrees).toStructure(offset);
    }

    private static ScalarAnimationCurve.Keyframe key(float tick, double value,
                                                      ScalarAnimationCurve.Interpolation interpolation) {
        return ScalarAnimationCurve.key(tick, value, interpolation);
    }

    private Vec3 rotateModelOffset(Vec3 offset) {
        float yawRad = getVisualRotationYInDegrees() * Mth.DEG_TO_RAD;
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
    public Vec3 getPlayerPOV() {
        return new Vec3(0.0, -0.7f, 0.0);
    }

    private record AutomatedAmmo(LoadingRequirement stage, Item item, String ammoKey) {
    }
}
