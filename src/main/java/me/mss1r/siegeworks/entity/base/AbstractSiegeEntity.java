package me.mss1r.siegeworks.entity.base;

import java.util.Set;
import me.mss1r.siegeworks.api.SiegeEngineControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.visual.BlueprintConstructionVisuals;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.debug.SiegeworksDebug;
import me.mss1r.siegeworks.gameplay.collision.SiegeTerrainCollision;
import me.mss1r.siegeworks.gameplay.crew.SiegeCrewController;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import me.mss1r.siegeworks.gameplay.movement.SiegeMovementPhysics;
import me.mss1r.siegeworks.gameplay.movement.SiegeTransformInterpolator;
import me.mss1r.siegeworks.gameplay.loading.LoadingRequirement;
import me.mss1r.siegeworks.gameplay.loading.SiegeLoadingController;
import me.mss1r.siegeworks.gameplay.movement.SiegeDriveState;
import me.mss1r.axiomata.collision.ClimbableGroup;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.siegeworks.gameplay.audio.SiegeAudioController;
import me.mss1r.siegeworks.gameplay.aiming.SiegeAimingController;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.item.SiegeDeploymentItemLookup;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentState;
import me.mss1r.siegeworks.gameplay.construction.SiegeConstructionController;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceController;
import me.mss1r.siegeworks.gameplay.damage.SiegeAttackPolicy;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeCaptureController;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOperatorReference;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOwnerActivity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOwnership;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.ScattershotProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.gameplay.towing.SiegeTowingController;
import me.mss1r.siegeworks.gameplay.weapon.SiegeWeaponState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import me.mss1r.siegeworks.registry.SiegeworksSounds;

import java.util.*;

public abstract class AbstractSiegeEntity extends LivingEntity
        implements SiegeEngineControl, CollidableStructure, UnderConstruction {
    private static final String TAG_DISMANTLING = "Dismantling";
    private static final String TAG_DISMANTLE_PROGRESS = "DismantleProgress";

    private static final EntityDataAccessor<Integer> BUILD_STAGE =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> BUILD_BLUEPRINT =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> BUILD_HITS =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BUILD_STAGE_HITS =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);

    protected static final EntityDataAccessor<Float> TRACKED_YAW =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> TRACKED_PITCH =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DRIVE_SPEED =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEERING_SPEED =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COOLDOWN =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LOAD_STAGE =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> AMMO_LOADED =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SCATTERSHOT_COUNT =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> AUTOMATED_AMMUNITION_MODE =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> WINDING_TIME =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WINDING_TOTAL =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SHOT_POWER =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Boolean> IS_PICKED =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ATTACK_HAPPENED =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> DISMANTLING =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Integer> DISMANTLE_PROGRESS =
            SynchedEntityData.defineId(AbstractSiegeEntity.class, EntityDataSerializers.INT);

    public float lastRiderYaw;
    public float lastRiderPitch;
    public float wheelRotation;
    protected final Set<UUID> playersNotified = new HashSet<>();
    private final SiegeOperatorReference operator = new SiegeOperatorReference(this);
    private final SiegeOwnership ownership = new SiegeOwnership();
    /** Max difference between server and local aim before the server value is applied as a correction. */
    private static final float AIM_CORRECTION_DEGREES = 10.0F;
    private float predictedAimYaw;
    private float predictedAimPitch;
    private boolean aimPredicted;
    /** True while the client ticks the engine, so aim changes come from the local operator rather than the server. */
    private boolean tickingOnClient;
    /** Client-side ticks since the local driver last steered. */
    private int clientSteeringIdleTicks;
    /** How long, in ticks, a yaw reported by the driving client stays valid. */
    private static final int DRIVER_YAW_STALE_TICKS = 10;
    private float driverYaw;
    private int driverYawTick = Integer.MIN_VALUE / 2;
    private final SiegeCaptureController capture = new SiegeCaptureController(this);
    private final SiegeDeploymentState deployment = new SiegeDeploymentState(this);
    private final SiegeAudioController audio = new SiegeAudioController(this);
    private final SiegeTransformInterpolator interpolation = new SiegeTransformInterpolator(this);
    private final SiegeAimingController aiming = new SiegeAimingController(
            this,
            new SiegeAimingController.Host() {
                @Override public float trackedYaw() { return entityData.get(TRACKED_YAW); }
                @Override public void setTrackedYaw(float yaw) { entityData.set(TRACKED_YAW, yaw); }
                @Override public float trackedPitch() { return entityData.get(TRACKED_PITCH); }
                @Override public void setTrackedPitch(float pitch) { entityData.set(TRACKED_PITCH, pitch); }
                @Override public float minimumPitch() { return getMinAimPitch(); }
                @Override public float maximumPitch() { return getMaxAimPitch(); }
                @Override public float yawTurnSpeed() { return getYawTurnSpeedDegrees(); }
                @Override public float pitchTurnSpeed() { return getPitchTurnSpeedDegrees(); }
                @Override public boolean independentYaw() { return usesIndependentAimYaw(); }
                @Override public boolean independentPitch() { return usesIndependentAimPitch(); }
                @Override public boolean towed() { return isTowed(); }
                @Override public float aimingRenderYaw() { return getAimingRenderYaw(); }
                @Override public float aimingRenderPitch() { return getAimingRenderPitch(); }
                @Override public float passengerTargetYaw(Player player) {
                    return getPassengerAimTargetYaw(player);
                }
                @Override public float passengerTargetPitch(Player player) {
                    return getPassengerAimTargetPitch(player);
                }
                @Override public void requestTurnTowardsYaw(float yaw) { turnTowardsYaw(yaw); }
                @Override public void requestTurnTowardsPitch(float pitch) { turnTowardsPitch(pitch); }
                @Override public boolean passengerControlsRotation(Entity passenger) {
                    return shouldPassengerControlRotation(passenger);
                }
                @Override public boolean passengerControlsMovement(Entity passenger) {
                    return shouldPassengerControlMovement(passenger);
                }
            });
    private final SiegeWeaponState weapon = new SiegeWeaponState(new SiegeWeaponState.Host() {
        @Override public int cooldown() { return entityData.get(COOLDOWN); }
        @Override public void setCooldown(int ticks) { entityData.set(COOLDOWN, ticks); }
        @Override public int loadStage() { return entityData.get(LOAD_STAGE); }
        @Override public void setLoadStage(int stage) { entityData.set(LOAD_STAGE, stage); }
        @Override public String ammunition() { return entityData.get(AMMO_LOADED); }
        @Override public void setAmmunition(String ammunition) { entityData.set(AMMO_LOADED, ammunition); }
        @Override public int scattershotCount() { return entityData.get(SCATTERSHOT_COUNT); }
        @Override public void setScattershotCount(int count) { entityData.set(SCATTERSHOT_COUNT, count); }
        @Override public String automatedAmmunitionMode() {
            return entityData.get(AUTOMATED_AMMUNITION_MODE);
        }
        @Override public void setAutomatedAmmunitionMode(String mode) {
            entityData.set(AUTOMATED_AMMUNITION_MODE, mode);
        }
        @Override public int windingTime() { return entityData.get(WINDING_TIME); }
        @Override public void setWindingTime(int ticks) { entityData.set(WINDING_TIME, ticks); }
        @Override public int windingTotal() { return entityData.get(WINDING_TOTAL); }
        @Override public void setWindingTotal(int ticks) { entityData.set(WINDING_TOTAL, ticks); }
        @Override public float shotPower() { return entityData.get(SHOT_POWER); }
        @Override public void setShotPower(float power) { entityData.set(SHOT_POWER, power); }
        @Override public boolean attackHappened() { return entityData.get(ATTACK_HAPPENED); }
        @Override public void setAttackHappened(boolean happened) { entityData.set(ATTACK_HAPPENED, happened); }
    });
    private final SiegeMaintenanceController maintenance = new SiegeMaintenanceController(
            new SiegeMaintenanceController.Host() {
                @Override
                public AbstractSiegeEntity siege() {
                    return AbstractSiegeEntity.this;
                }

                @Override
                public void setDismantling(boolean dismantling) {
                    AbstractSiegeEntity.this.setDismantling(dismantling);
                }

                @Override
                public void setDismantleProgress(int progress) {
                    AbstractSiegeEntity.this.setDismantleProgress(progress);
                }

                @Override
                public void stopMovement() {
                    AbstractSiegeEntity.this.setMovementInput(0.0F, 0.0F);
                }

                @Override
                public void playNearbySound(ServerLevel level, SoundEvent sound, double range, float volume) {
                    AbstractSiegeEntity.this.playSoundToNearbyPlayers(level, sound, range, volume);
                }
            });
    private final SiegeLoadingController loading = new SiegeLoadingController(
            new SiegeLoadingController.Host() {
                @Override
                public AbstractSiegeEntity siege() {
                    return AbstractSiegeEntity.this;
                }

                @Override
                public int stageDuration(String configKey) {
                    return AbstractSiegeEntity.this.getLoadingRequirementTicks(configKey);
                }

                @Override
                public void loadingStarted(ServerLevel level) {
                    AbstractSiegeEntity.this.onLoadingStart(level);
                }

                @Override
                public void loadingTick(ServerLevel level, Player player, InteractionHand hand,
                                        int elapsedTicks, int totalTicks) {
                    AbstractSiegeEntity.this.onLoadingActionTick(level, player, hand, elapsedTicks, totalTicks);
                }

                @Override
                public void loadingCompleted(ServerLevel level, Player player, InteractionHand hand, int stageIndex) {
                    AbstractSiegeEntity.this.completeLoadingAction(level, player, hand, stageIndex);
                }

                @Override
                public void loadingCancelled() {
                    AbstractSiegeEntity.this.onLoadingActionCancelled();
                }

                @Override
                public boolean playSoundOnStart() {
                    return AbstractSiegeEntity.this.shouldPlayLoadingSoundOnStart();
                }

                @Override
                public boolean loopVisualReloadSound() {
                    return AbstractSiegeEntity.this.shouldLoopReloadSoundDuringVisualReload();
                }

                @Override
                public int visualReloadSoundInterval() {
                    return AbstractSiegeEntity.this.getVisualReloadSoundIntervalTicks();
                }

                @Override
                public void playReloadSound(ServerLevel level) {
                    AbstractSiegeEntity.this.playReloadSound(level);
                }

                @Override
                public double serviceRange() {
                    return AbstractSiegeEntity.this.getReloadServiceRange();
                }

                @Override
                public int cooldownTotalTicks() {
                    return weapon.cooldownTotalTicks();
                }

                @Override
                public String cooldownStatusKey() {
                    return AbstractSiegeEntity.this.getCooldownStatusKey();
                }
            });
    private final SiegeConstructionController construction = new SiegeConstructionController(
            new SiegeConstructionController.Host() {
                @Override
                public AbstractSiegeEntity siege() {
                    return AbstractSiegeEntity.this;
                }

                @Override
                public void syncProgress(int builtStages, String blueprintId, int hits, int stageHits) {
                    entityData.set(BUILD_STAGE, builtStages);
                    entityData.set(BUILD_BLUEPRINT, blueprintId);
                    entityData.set(BUILD_HITS, hits);
                    entityData.set(BUILD_STAGE_HITS, stageHits);
                }

                @Override
                public int syncedBuildHits() {
                    return entityData.get(BUILD_HITS);
                }

                @Override
                public int syncedBuildStageHits() {
                    return entityData.get(BUILD_STAGE_HITS);
                }

                @Override
                public int syncedBuiltSections() {
                    return entityData.get(BUILD_STAGE);
                }

                @Override
                public String syncedBlueprintId() {
                    return entityData.get(BUILD_BLUEPRINT);
                }
            });
    private final SiegeTowingController towing = new SiegeTowingController(this);
    private final SiegeCrewController crew = new SiegeCrewController(this);
    private final SiegeDriveState drive = new SiegeDriveState();

    public AbstractSiegeEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
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
        data.define(BUILD_STAGE, Integer.MAX_VALUE);
        data.define(BUILD_BLUEPRINT, "");
        data.define(BUILD_HITS, 0);
        data.define(BUILD_STAGE_HITS, 0);
        data.define(TRACKED_YAW, 0.0f);
        data.define(TRACKED_PITCH, 0.0f);
        data.define(DRIVE_SPEED, 0.0f);
        data.define(STEERING_SPEED, 0.0f);
        data.define(COOLDOWN, 0);
        data.define(LOAD_STAGE, 0);
        data.define(AMMO_LOADED, "");
        data.define(SCATTERSHOT_COUNT, 0);
        data.define(AUTOMATED_AMMUNITION_MODE, SiegeAmmunitionMode.AUTO.serializedName());
        data.define(WINDING_TIME, 0);
        data.define(WINDING_TOTAL, 0);
        data.define(SHOT_POWER, 1.0f);
        data.define(IS_PICKED, false);
        data.define(ATTACK_HAPPENED, true);
        data.define(DISMANTLING, false);
        data.define(DISMANTLE_PROGRESS, 0);
    }

    public static final float STEP_HEIGHT = 1.25F;

    @Override
    public float maxUpStep() {
        return STEP_HEIGHT;
    }

    private static final int UNSTICK_INTERVAL_TICKS = 10;
    private static final int ABANDONMENT_CHECK_INTERVAL_TICKS = 1200;

    public float geometryStepHeight() {
        return maxUpStep();
    }

    public boolean usesGeometryTerrainCollision() {
        return SiegeworksServerConfig.isGeometryTerrainCollisionEnabled()
                && !solidCollisionGroups().isEmpty();
    }

    public boolean isTerrainCollisionExcluded(CollisionGroup group) {
        return false;
    }

    public void setTrackedYaw(float yaw) {
        aiming.setTrackedYaw(yaw);
    }

    public float getTrackedYaw() {
        return aiming.trackedYaw();
    }

    public void setPassengerAimTargetYaw(float yaw) {
        aiming.setPassengerTargetYaw(yaw);
    }

    public void setPassengerAimTargetPitch(float pitch) {
        aiming.setPassengerTargetPitch(pitch);
    }

    public void turnTowardsYaw(float targetYaw) {
        aiming.turnTowardsYaw(targetYaw);
    }

    public void applyYaw(float yaw) {
        aiming.applyYaw(yaw);
    }

    public static boolean isDraftMount(Entity entity) {
        return SiegeTowingController.isDraftMount(entity);
    }

    public AbstractHorse getTowingMount() {
        return towing.primaryMount();
    }

    public List<AbstractHorse> getTowingMounts() {
        return towing.mounts();
    }

    public int getTowingMountSlot(Entity entity) {
        return towing.mountSlot(entity);
    }

    public boolean isTowed() {
        return towing.towed();
    }

    protected static final float TOWED_YAW_TURN_MULTIPLIER = 1.5f;

    @Nullable
    public TowingProfile towingProfile() {
        return null;
    }

    public boolean isTowable() {
        return towing.towable();
    }

    public float towedModelTurnDegrees() {
        return towing.modelTurnDegrees();
    }

    public Vec3 getTowPivotOffset() {
        return towing.defaultPivotOffset();
    }

    public Vec3 towPivotWorldOffset() {
        return towing.pivotWorldOffset();
    }

    protected float getYawTurnSpeedDegrees() {
        return isTowed() ? 1.0f * TOWED_YAW_TURN_MULTIPLIER : 1.0f;
    }

    protected boolean usesIndependentAimYaw() {
        return false;
    }

    public final boolean usesIndependentAim() {
        return usesIndependentAimYaw();
    }

    protected boolean usesIndependentAimPitch() {
        return false;
    }

    public final boolean usesIndependentPitchAim() {
        return usesIndependentAimPitch();
    }

    protected float getPitchTurnSpeedDegrees() {
        return 1.0F;
    }

    public boolean shouldPredictYawOnClient() {
        return false;
    }

    public void setTrackedPitch(float pitch) {
        aiming.setTrackedPitch(pitch);
    }

    public float getMinAimPitch() {
        return -20.0F;
    }

    public float getMaxAimPitch() {
        return 10.0F;
    }

    public float getTrackedPitch() {
        return aiming.trackedPitch();
    }

    public void turnTowardsPitch(float targetPitch) {
        aiming.turnTowardsPitch(targetPitch);
    }

    protected float getAimingRenderYaw() {
        return usesIndependentAimYaw() ? getTrackedYaw() : getVisualRotationYInDegrees();
    }

    protected float getAimingRenderPitch() {
        return getTrackedPitch();
    }

    public void updateRenderedAim(float partialTick) {
        aiming.updateRendered(partialTick);
    }

    public float getRenderedAimYaw() {
        return aiming.renderedYaw();
    }

    public float getRenderedAimPitch() {
        return aiming.renderedPitch();
    }

    public void setCooldown(int cooldown) {
        weapon.setCooldown(cooldown);
    }

    public int getCooldown() {
        return weapon.cooldown();
    }

    protected void startRecovery() {
        setCooldown(SiegeworksServerConfig.getRecoveryTicks(getType()));
    }

    public final boolean requestDebugInstantFire(Player player) {
        if (!SiegeworksDebug.instantFire()
                || !player.isCreative()
                || !player.hasPermissions(2)
                || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (!debugInstantFire(serverLevel, player)) {
            return false;
        }

        loading.clear();
        setWindingTime(0);
        return true;
    }

    protected boolean debugInstantFire(ServerLevel serverLevel, Player operator) {
        return false;
    }

    private boolean requestDebugAutoDrive(Player player) {
        if (!SiegeworksDebug.autoDrive()
                || !player.isCreative()
                || !player.hasPermissions(2)
                || !(level() instanceof ServerLevel)) {
            return false;
        }

        boolean debugAutoDriving = drive.toggleDebugAutoDriving();
        player.displayClientMessage(Component.literal(
                "Debug auto-drive: " + (debugAutoDriving ? "on" : "off")), true);
        return true;
    }

    protected int getCooldownElapsedTicks() {
        return weapon.cooldownElapsedTicks();
    }

    public void setLoadStage(int stage) {
        weapon.setLoadStage(stage);
        loading.cancelIfStageChanged(weapon.loadStage());
    }

    public int getLoadStage() {
        return weapon.loadStage();
    }

    public void setAmmoLoaded(String ammo) {
        weapon.setAmmunition(ammo);
    }

    public String getAmmoLoaded() {
        return weapon.ammunition();
    }

    public boolean hasAmmoLoaded() {
        return weapon.hasAmmunition();
    }

    public int getScattershotCount() {
        return weapon.scattershotCount();
    }

    public void setScattershotCount(int count) {
        weapon.setScattershotCount(count);
    }

    public SiegeAmmunitionMode getAutomatedAmmunitionMode() {
        return weapon.automatedAmmunitionMode();
    }

    public void setAutomatedAmmunitionMode(SiegeAmmunitionMode mode) {
        weapon.setAutomatedAmmunitionMode(mode);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        construction.save(tag);
        aiming.save(tag);
        weapon.save(tag);
        operator.save(tag);
        ownership.save(tag);
        deployment.save(tag);
        tag.putBoolean(TAG_DISMANTLING, isDismantling());
        tag.putInt(TAG_DISMANTLE_PROGRESS, getDismantleProgress());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        construction.load(tag);
        aiming.load(tag);
        weapon.load(tag);
        operator.load(tag);
        deployment.load(tag);
        if (!ownership.load(tag)) {
            ownership.set(legacyOwner());
        }
        if (tag.contains(TAG_DISMANTLING)) {
            setDismantling(tag.getBoolean(TAG_DISMANTLING));
        }
        if (tag.contains(TAG_DISMANTLE_PROGRESS)) {
            setDismantleProgress(tag.getInt(TAG_DISMANTLE_PROGRESS));
        }
        loading.clear();
    }

    public void setDeploymentIdentity(UUID ownerUuid, String groupKey) {
        deployment.setIdentity(ownerUuid, groupKey);
        ownership.claim(ownerUuid);
    }

    public UUID getDeploymentOwnerUuid() {
        return deployment.ownerUuid();
    }

    public String getDeploymentGroup() {
        return deployment.groupKey();
    }

    public void setWindingTime(int time) {
        weapon.setWindingTime(time);
    }

    public int getWindingTime() {
        return weapon.windingTime();
    }

    protected void setWindingTotal(int ticks) {
        weapon.setWindingTotal(ticks);
    }

    public int getWindingTotal() {
        return weapon.windingTotal();
    }

    public boolean isWindingComplete() {
        return weapon.windingComplete();
    }

    public void setShotPower(float power) {
        weapon.setShotPower(power);
    }

    public float getShotPower() {
        return weapon.shotPower();
    }

    public float getMinShotPower() {
        return SiegeWeaponState.MINIMUM_SHOT_POWER;
    }

    public float getMaxShotPower() {
        return SiegeWeaponState.MAXIMUM_SHOT_POWER;
    }

    protected InteractionResult cycleShotPower(Player player) {
        weapon.cycleShotPower();
        player.displayClientMessage(Component.translatable("siege.tension.set", weapon.shotPowerBar()), true);
        return InteractionResult.SUCCESS;
    }

    public void setPicked(boolean picked) {
        this.entityData.set(IS_PICKED, picked);
    }

    public boolean isPicked() {
        return this.entityData.get(IS_PICKED);
    }

    public void setAttackHappened(boolean happened) {
        weapon.setAttackHappened(happened);
    }

    public boolean hasAttackHappened() {
        return weapon.attackHappened();
    }

    public boolean isDismantling() {
        return this.entityData.get(DISMANTLING);
    }

    private void setDismantling(boolean dismantling) {
        this.entityData.set(DISMANTLING, dismantling);
        if (!dismantling) {
            setDismantleProgress(0);
        }
    }

    public int getDismantleProgress() {
        return this.entityData.get(DISMANTLE_PROGRESS);
    }

    private void setDismantleProgress(int progress) {
        this.entityData.set(DISMANTLE_PROGRESS, Math.max(0, progress));
    }

    /** Last player who crewed or fired the engine. Used for damage attribution, never for ownership. */
    @Nullable
    public Entity getOperator() {
        return operator.get();
    }

    public void setOperator(@Nullable Entity operator) {
        this.operator.set(operator);
        if (operator != null && level() instanceof ServerLevel serverLevel) {
            UUID claimant = SiegeOwnership.claimantOf(operator);
            if (ownership.claim(claimant) && getDeploymentOwnerUuid() == null) {
                SiegeDeploymentLimits.Deployment claimed = SiegeDeploymentLimits.forOwner(serverLevel, claimant);
                deployment.setIdentity(claimed.ownerUuid(), claimed.groupKey());
            }
        }
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownership.ownerUuid();
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        ownership.set(ownerUuid);
    }

    public boolean claimOwnership(@Nullable UUID claimant) {
        return ownership.claim(claimant);
    }

    public boolean isOwnedBy(@Nullable UUID playerUuid) {
        return ownership.isOwnedBy(playerUuid);
    }

    /** Releases the engine once its team has been offline for the configured time. Returns true if released. */
    public boolean releaseIfAbandoned(ServerLevel level, long nowMillis) {
        UUID owner = getOwnerUuid();
        int days = SiegeworksServerConfig.getAbandonAfterDays();
        if (owner == null || days <= 0 || capture.isActive() || SiegeOwnerActivity.sideSeenWithin(
                level, owner, java.util.concurrent.TimeUnit.DAYS.toMillis(days), nowMillis)) {
            return false;
        }
        ownership.set(null);
        SiegeDeploymentLimits.unregister(this);
        deployment.setIdentity(null, "");
        return true;
    }

    public boolean isBeingCaptured() {
        return capture.isActive();
    }

    @Nullable
    public SiegeCaptureController.Refusal captureRefusal(Entity entity) {
        return capture.refusal(entity);
    }

    /** Whether an enemy may capture this engine; if not, it can only be destroyed. */
    public boolean isCapturable() {
        return true;
    }

    /** How an enemy player takes control to capture this engine. Default: its seat. */
    protected InteractionResult boardForCapture(Player player) {
        if (!canAddPassenger(player) || !player.startRiding(this)) {
            return InteractionResult.FAIL;
        }
        setOperator(player);
        return InteractionResult.SUCCESS;
    }

    protected InteractionResult captureStanding(Player player) {
        return capture.beginStanding(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Nullable
    private UUID legacyOwner() {
        if (getDeploymentOwnerUuid() != null) {
            return getDeploymentOwnerUuid();
        }
        UUID lastOperator = operator.uuid();
        return SiegeOwnership.isKnownPlayer(level().getServer(), lastOperator) ? lastOperator : null;
    }

    public float getWheelRotation() {
        return wheelRotation;
    }

    public float getLeftWheelRotation() {
        return drive.leftWheelRotation(wheelRotation);
    }

    public float getRightWheelRotation() {
        return drive.rightWheelRotation(wheelRotation);
    }

    public boolean hasDifferentialDrive() {
        return false;
    }

    private boolean clientRotationIsAimed() {
        return !usesIndependentAimYaw();
    }

    public boolean canPivotInPlace(Entity operator) {
        return hasDifferentialDrive() && operator != null && !isDraftMount(operator);
    }

    public void updateDifferentialWheelRotation() {
        drive.updateDifferentialWheelRotation(this);
    }

    public double updateVisualTravelAndGetForwardDistance() {
        return drive.updateVisualTravelAndGetForwardDistance(this);
    }

    public double getVisualHorizontalTravelSpeed() {
        return drive.visualHorizontalTravelSpeed();
    }

    public double updateDriveAnimationAndGetForwardDistance() {
        return drive.updateDriveAnimationAndGetForwardDistance(this);
    }

    public abstract SiegeSoundProfile getSoundProfile();

    protected float getPassengerOffsetYaw(Entity entity) {
        return getVisualRotationYInDegrees();
    }

    public final float getPassengerSeatYaw(Entity entity) {
        return getPassengerOffsetYaw(entity);
    }

    public final float getPassengerVisualYaw(Entity entity) {
        float heading = getPassengerOffsetYaw(entity);
        return isDraftMount(entity) ? heading : heading + operatorViewOffsetDegrees();
    }

    protected float operatorViewOffsetDegrees() {
        return 0.0F;
    }

    public final Vec3 getPassengerOffset(Entity entity) {
        Vec3 mountOffset = towing.mountOffset(entity);
        return mountOffset == null ? getOperatorOffset(entity) : mountOffset;
    }

    protected Vec3 getOperatorOffset(Entity entity) {
        return Vec3.ZERO;
    }

    public abstract Vec3 getPlayerPOV();

    public Vec3 boardingSpot(LivingEntity passenger) {
        return position();
    }

    public final boolean canAddPassenger(Entity entity) {
        if (isDraftMount(entity)) {
            return towing.canAddDraftMount(entity);
        }
        if (!SiegeAccess.allows(entity, this, SiegeAccess.Action.USE) && capture.refusal(entity) != null) {
            return false;
        }
        if (!towing.operatorSlotAvailable() && !takesPassengersWhileTowed(entity)) {
            return false;
        }
        return canAddOperator(entity);
    }

    protected boolean takesPassengersWhileTowed(Entity entity) {
        return false;
    }

    public int crewCapacity() {
        return 1;
    }

    protected boolean canAddOperator(Entity entity) {
        return isFullyBuilt() && getPassengers().isEmpty() && isSupportedDirectOperator(entity);
    }

    @Override
    public BuildProgress buildProgress() {
        return construction.progress();
    }

    public BlueprintConstructionVisuals.State constructionVisualState() {
        return BlueprintConstructionVisuals.state(
                buildBlueprintId(), builtSections());
    }

    @Override
    public void orientForPlacement(float yaw) {
        construction.orientForPlacement(yaw);
    }

    @Override
    public void onBuildProgressChanged() {
        construction.syncProgress();
    }

    @Override
    public boolean acceptsBlowOnStage(Player builder, String section) {
        return construction.acceptsBlow(builder, section);
    }

    public int buildHits() {
        return construction.buildHits();
    }

    public int buildStageHits() {
        return construction.buildStageHits();
    }

    public int builtSections() {
        return construction.builtSections();
    }

    public String buildBlueprintId() {
        return construction.blueprintId();
    }

    @Override
    public boolean isFullyBuilt() {
        return construction.fullyBuilt();
    }

    protected boolean isPlayerControlledDraftMount(Entity passenger) {
        return towing.isPlayerControlledDraftMount(passenger);
    }

    public final boolean shouldPassengerControlRotation(Entity passenger) {
        return towing.isDrivenDraftMount(passenger)
                || !capture.isCapturer(passenger) && operatorControlsRotation(passenger);
    }

    public final boolean shouldPassengerControlMovement(Entity passenger) {
        return towing.isDrivenDraftMount(passenger)
                || !capture.isCapturer(passenger) && operatorControlsMovement(passenger);
    }

    public LivingEntity getReinsHolder() {
        return towing.reinsHolder();
    }

    public final boolean shouldPassengerBodyFollowSiege(Entity passenger) {
        return isPlayerControlledDraftMount(passenger) || operatorBodyFollowsEngine(passenger);
    }

    protected boolean operatorControlsRotation(Entity passenger) {
        return isSupportedDirectOperator(passenger);
    }

    protected boolean operatorControlsMovement(Entity passenger) {
        return operatorControlsRotation(passenger);
    }

    protected boolean operatorBodyFollowsEngine(Entity passenger) {
        return operatorControlsRotation(passenger);
    }

    public float getPassengerViewYawLimit(Entity passenger) {
        return 180.0F;
    }

    public float getPassengerViewPitchLimit(Entity passenger) {
        return 90.0F;
    }

    public void setMovementInputForward(float forward) {
        drive.setForwardInput(forward);
    }

    public void setMovementInput(float forward, float steering) {
        drive.setInput(forward, steering);
    }

    protected boolean isSupportedDirectOperator(Entity entity) {
        return SiegeCrewController.supportsDirectOperator(entity);
    }

    @Override
    public boolean canOperate(LivingEntity operator) {
        return crew.canOperate(operator);
    }

    @Override
    public boolean mountOperator(LivingEntity operator) {
        return crew.mount(operator);
    }

    @Override
    public void dismountOperator(LivingEntity operator) {
        crew.dismount(operator);
    }

    @Override
    public boolean isOperator(LivingEntity operator) {
        return crew.isOperator(operator);
    }

    @Override
    public void setOperatorMovement(LivingEntity operator, float forward, float steering) {
        crew.setMovement(operator, forward, steering);
    }

    @Override
    public void setOperatorAim(LivingEntity operator, float yaw, float pitch) {
        if (!isOperator(operator)) {
            return;
        }

        turnTowardsPitch(pitch);
        if (usesIndependentAimYaw()) {
            turnTowardsYaw(yaw);
        }
    }

    @Override
    public void clearOperatorInput(LivingEntity operator) {
        crew.clearInput(operator);
    }

    @Override
    public SiegeOperationState getOperationState() {
        return weapon.operationState(hasLoadingAction());
    }

    public float getMovementInputForward() {
        return drive.forwardInput();
    }

    public float getMovementInputSteering() {
        return drive.steeringInput();
    }

    public boolean isDebugAutoDriving() {
        return drive.debugAutoDriving();
    }

    public void setDebugAutoDriving(boolean enabled) {
        drive.setDebugAutoDriving(enabled);
    }

    public double getDebugAutoDriveVelocity() {
        return SiegeworksServerConfig.getMovementSpeed(getType(), false);
    }

    public double getDebugAutoDriveAcceleration() {
        return SiegeworksServerConfig.getMovementAcceleration(getType(), false);
    }

    public double getCurrentDriveSpeed() {
        return this.entityData.get(DRIVE_SPEED);
    }

    public void setCurrentDriveSpeed(double currentDriveSpeed) {
        this.entityData.set(DRIVE_SPEED, (float) currentDriveSpeed);
    }

    public float getCurrentSteeringSpeed() {
        return this.entityData.get(STEERING_SPEED);
    }

    public void setCurrentSteeringSpeed(float steeringSpeed) {
        this.entityData.set(STEERING_SPEED, Math.max(0.0F, steeringSpeed));
    }

    /**
     * Applies the yaw reported by the driving client. The server turns toward it at the normal steering rate instead of
     * replaying the driver's inputs, which would jitter with packet timing.
     */
    public void reportDriverYaw(float yaw) {
        driverYaw = Mth.wrapDegrees(yaw);
        driverYawTick = tickCount;
    }

    /** Last yaw reported by the driving client, if it is recent. */
    public OptionalDouble freshDriverYaw() {
        return tickCount - driverYawTick <= DRIVER_YAW_STALE_TICKS
                ? OptionalDouble.of(driverYaw)
                : OptionalDouble.empty();
    }

    public int getClientSteeringIdleTicks() {
        return clientSteeringIdleTicks;
    }

    public void setClientSteeringIdleTicks(int ticks) {
        clientSteeringIdleTicks = ticks;
    }

    public void applyClientPredictedYaw(float yaw) {
        aiming.applyClientPredictedYaw(yaw);
    }

    public final void commitInterpolatedRotation(float yaw, float pitch) {
        setRot(yaw, pitch);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return crew.controllingPassenger();
    }

    @Nullable
    public Entity getMovementControllerPassenger() {
        return crew.movementController();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return false;
    }

    public void onSiegeTick(ServerLevel serverLevel) {
    }

    public void onAttack(ServerLevel serverLevel) {
    }

    public void onLoadingStart(ServerLevel serverLevel) {
    }

    public void onLoadComplete(ServerLevel serverLevel) {
    }

    protected void onWindingComplete(ServerLevel serverLevel) {
    }

    public SoundEvent getMoveSound() {
        return getSoundProfile().movement().sound();
    }

    public SoundEvent getReloadSound() {
        return getSoundProfile().reload().sound();
    }

    public SoundEvent getShootSound() {
        return getSoundProfile().firing().sound();
    }

    public SoundEvent getAttackSound() {
        return getSoundProfile().attack().sound();
    }

    public abstract void triggerAnimation(String animationName);

    public abstract void stopAnimation(String animationName);

    /**
     * Keeps the locally driven aim when the server's echo arrives: the echo lags by a round trip and would pull the aim
     * back. A large difference is a real correction and is applied.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide || tickingOnClient || !aimPredicted || !aiming.hasLocalPlayerControl()) {
            return;
        }
        if (TRACKED_YAW.equals(key) && usesIndependentAimYaw()
                && Math.abs(Mth.wrapDegrees(entityData.get(TRACKED_YAW) - predictedAimYaw)) < AIM_CORRECTION_DEGREES) {
            entityData.set(TRACKED_YAW, predictedAimYaw);
        } else if (TRACKED_PITCH.equals(key)
                && Math.abs(entityData.get(TRACKED_PITCH) - predictedAimPitch) < AIM_CORRECTION_DEGREES) {
            entityData.set(TRACKED_PITCH, predictedAimPitch);
        }
    }

    @Override
    public void tick() {
        tickingOnClient = level().isClientSide;
        try {
            tickSiege();
        } finally {
            tickingOnClient = false;
        }
    }

    private void tickSiege() {
        if (level().isClientSide) {
            aiming.capturePreviousRenderState();
        }

        if (!isFullyBuilt()) {
            setDeltaMovement(Vec3.ZERO);
            setNoGravity(true);
            super.tick();
            construction.holdOrientation();
            return;
        }
        setNoGravity(false);
        super.tick();
        if (collisionGroups().isEmpty()) {
            StructureCollisionSystem.unregister(this);
        } else {
            StructureCollisionSystem.register(this);
        }
        interpolation.tick(aiming.hasLocalPlayerControl() || clientRotationIsAimed());

        SiegeMovementPhysics.updateWheelRotation(this);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            if (aiming.hasLocalPlayerControl()) {
                aiming.updateFromController();
                if (!usesIndependentAimYaw()) {
                    SiegeMovementPhysics.updateClientSteering(this);
                }
                predictedAimYaw = entityData.get(TRACKED_YAW);
                predictedAimPitch = entityData.get(TRACKED_PITCH);
                aimPredicted = true;
            } else {
                aimPredicted = false;
                aiming.updateClientEntityRotation();
            }
            return;
        }

        followProfileHealth();
        deployment.tick();
        capture.tick(serverLevel);
        if (tickCount % ABANDONMENT_CHECK_INTERVAL_TICKS == 0) {
            releaseIfAbandoned(serverLevel, System.currentTimeMillis());
        }

        aiming.updateFromController();
        if (tickCount % UNSTICK_INTERVAL_TICKS == 0) {
            SiegeTerrainCollision.unstick(this);
        }
        SiegeMovementPhysics.updateSiegeVelocity(this);

        aiming.updateServerEntityRotation();
        this.wheelRotation = this.getWheelRotation();
        setCooldown(getCooldown() - 1);
        int previousWindingTime = getWindingTime();
        setWindingTime(previousWindingTime - 1);
        if (previousWindingTime > 0 && isWindingComplete()) {
            onWindingComplete(serverLevel);
        }
        maintenance.tick();
        loading.tick(serverLevel);
        loading.tickVisualReloadSound(serverLevel);
        audio.tickMovement(serverLevel, getMoveSoundIntervalTicks());

        onSiegeTick(serverLevel);
        setPicked(getFirstPassenger() != null);
    }

    @Override
    public void remove(RemovalReason reason) {
        StructureCollisionSystem.unregister(this);
        deployment.onRemoved(reason);
        capture.stop();
        super.remove(reason);
    }

    @Override
    //? if forge {
    /*public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean teleport) {
    *///?} else {
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
    //?}
        interpolation.queue(x, y, z, yaw, pitch, interpolationSteps);
    }

    protected float getPassengerAimTargetYaw(Player player) {
        return player.getYRot();
    }

    protected float getPassengerAimTargetPitch(Player player) {
        return player.getXRot();
    }

    protected boolean hasLoadingAction() {
        return loading.active();
    }

    protected boolean continueLoadingAction(Player player) {
        return loading.continueIfHeldItemMatches(player);
    }

    protected Item getActiveLoadingItem() {
        return loading.activeItem();
    }

    protected InteractionResult beginLoadingAction(Player player, InteractionHand hand, ServerLevel serverLevel,
                                                  int stageIndex, LoadingRequirement stage) {
        return loading.begin(player, hand, serverLevel, stageIndex, stage);
    }

    protected InteractionResult beginLoadingAction(Player player, InteractionHand hand, ServerLevel serverLevel,
                                                   int stageIndex, LoadingRequirement stage, int durationTicks) {
        return loading.begin(player, hand, serverLevel, stageIndex, stage, durationTicks);
    }

    protected InteractionResult beginMountedLoadingRequirement(Player player, InteractionHand hand,
                                                          ServerLevel serverLevel, LoadingRequirement[] stages) {
        return loading.beginMountedStage(player, hand, serverLevel, stages);
    }

    protected boolean acceptsMountedLoadingItem(Player player, InteractionHand hand, LoadingRequirement[] stages) {
        return loading.acceptsMountedItem(player, hand, stages);
    }

    protected boolean shouldPlayLoadingSoundOnStart() {
        return true;
    }

    protected boolean shouldLoopReloadSoundDuringVisualReload() {
        return false;
    }

    protected int getVisualReloadSoundIntervalTicks() {
        return 20;
    }

    protected InteractionResult showLoadingProgress(Player player) {
        return loading.showProgress(player);
    }

    protected InteractionResult showCooldownProgress(Player player) {
        return loading.showCooldown(player);
    }

    protected String getCooldownStatusKey() {
        return "siege.loading.state.recovering";
    }

    protected InteractionResult showWindingProgress(Player player) {
        return loading.showWinding(player);
    }

    protected void completeLoadingAction(ServerLevel serverLevel, Player player, InteractionHand hand, int stageIndex) {
    }

    protected void onLoadingActionTick(ServerLevel serverLevel, Player player, InteractionHand hand,
                                       int elapsedTicks, int totalTicks) {
    }

    protected void onLoadingActionCancelled() {
    }

    protected double getReloadServiceRange() {
        return 9.0;
    }

    protected boolean canBeginLoadingRequirement(Player player, InteractionHand hand, LoadingRequirement required) {
        return loading.canBegin(player, hand, required);
    }

    protected boolean consumeLoadingRequirement(Player player, InteractionHand hand, LoadingRequirement required) {
        return loading.consume(player, hand, required);
    }

    protected int getMoveSoundIntervalTicks() {
        return 18;
    }

    protected void playSoundToNearbyPlayers(ServerLevel serverLevel, SoundEvent sound, double maxDistance, float baseVolume) {
        audio.playNearby(serverLevel, sound, maxDistance, baseVolume);
    }

    protected void playSoundToNearbyPlayers(ServerLevel serverLevel, SoundEvent sound, double maxDistance,
                                            float baseVolume, float basePitch, float pitchVariation) {
        audio.playNearby(serverLevel, sound, maxDistance, baseVolume, basePitch, pitchVariation);
    }

    protected void playSoundToPlayersInRange(ServerLevel serverLevel, SoundEvent sound, double minDistance,
                                             double maxDistance, float baseVolume, float minVolume) {
        audio.playInRange(serverLevel, sound, minDistance, maxDistance, baseVolume, minVolume);
    }

    protected void playSoundToPlayersInRange(ServerLevel serverLevel, SoundEvent sound, double minDistance,
                                             double maxDistance, float baseVolume, float minVolume,
                                             float basePitch, float pitchVariation) {
        audio.playInRange(serverLevel, sound, minDistance, maxDistance, baseVolume, minVolume,
                basePitch, pitchVariation);
    }

    protected void playReloadSound(ServerLevel serverLevel) {
        SiegeSoundProfile.SoundCue cue = getSoundProfile().reload();
        playSoundToNearbyPlayers(serverLevel, getReloadSound(), cue.range(), cue.volume());
    }

    protected void playShootSound(ServerLevel serverLevel) {
        SiegeSoundProfile.SoundCue cue = getSoundProfile().firing();
        playSoundToNearbyPlayers(serverLevel, getShootSound(), cue.range(), cue.volume());
    }

    protected void playAttackSound(ServerLevel serverLevel) {
        SiegeSoundProfile.SoundCue cue = getSoundProfile().attack();
        playSoundToNearbyPlayers(serverLevel, getAttackSound(), cue.range(), cue.volume());
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean controlledMovement = shouldPassengerControlMovement(passenger);
        super.removePassenger(passenger);

        if (controlledMovement) {
            crew.stopAfterControllerRemoved();

            if (level().isClientSide) {
                resetClientInterpolation();
            }
        }
    }

    private void resetClientInterpolation() {
        interpolation.reset();
    }

    @Override
    protected void positionRider(Entity entity, MoveFunction moveFunction) {
        SiegePassengerPhysics.updatePassengerState(this, entity);
        if (this.hasPassenger(entity)) {
            Vec3 seat = position().add(SiegePassengerPhysics.rotatedSeatOffset(this, entity));
            if (isDraftMount(entity)) {
                seat = SiegePassengerPhysics.mountFooting(this, entity, seat);
            }
            moveFunction.accept(entity, seat.x, seat.y, seat.z);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        ServerLevel serverLevel = level() instanceof ServerLevel server ? server : null;

        if (attacker == null || serverLevel == null) {
            return applySiegeDamage(source, amount, serverLevel);
        }

        if (attacker instanceof Player player && isConstructionHammer(player.getMainHandItem())) {
            maintenance.handleHammerHit(player, serverLevel);
            return false;
        }

        if (attacker instanceof Player player && player.isCreative()
                && source.getDirectEntity() == player) {
            playSiegeDamageSound(serverLevel, true);
            this.discard();
            return true;
        }

        SiegeAttackPolicy.Result attack = SiegeAttackPolicy.evaluate(
                source,
                amount,
                SiegeProfileCatalogs.ENGINES.forEntity(this.getType()).damageRules());
        if (!attack.accepted()) {
            return false;
        }

        return applySiegeDamage(source, attack.amount(), serverLevel);
    }

    private boolean applySiegeDamage(DamageSource source, float amount, @Nullable ServerLevel serverLevel) {
        boolean damaged = super.hurt(source, amount);
        clearVanillaHurtVisuals();
        if (damaged && serverLevel != null) {
            playSiegeDamageSound(serverLevel, isDeadOrDying() || getHealth() <= 0.0F);
        }
        return damaged;
    }

    private void playSiegeDamageSound(ServerLevel serverLevel, boolean destroyed) {
        if (destroyed) {
            playSoundToNearbyPlayers(serverLevel, SiegeworksSounds.SIEGE_ENGINE_DAMAGE.get(),
                    45.0D, 1.45F, 0.78F, 0.05F);
            return;
        }
        playSoundToNearbyPlayers(serverLevel, SiegeworksSounds.SIEGE_ENGINE_DAMAGE.get(),
                26.0D, 0.42F, 1.06F, 0.08F);
    }

    @Override
    public void animateHurt(float yaw) {
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 2 || id == 33) {
            clearVanillaHurtVisuals();
            return;
        }
        super.handleEntityEvent(id);
    }

    private void clearVanillaHurtVisuals() {
        this.hurtTime = 0;
        this.hurtDuration = 0;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    public void openMaintenanceScreen(ServerPlayer player) {
        maintenance.openScreen(player);
    }

    public void handleMaintenanceAction(ServerPlayer player, int action) {
        maintenance.handleAction(player, action);
    }

    public boolean needsMaintenanceRepair() {
        return maintenance.needsRepair();
    }

    public boolean hasMaintenanceRepairMaterials(Container materials) {
        return maintenance.hasRepairMaterials(materials);
    }

    public boolean repairFromMaintenanceWorker(LivingEntity worker, Container materials) {
        return maintenance.repairFromWorker(worker, materials);
    }

    public boolean canStartAutomatedDismantling(LivingEntity worker) {
        return maintenance.canStartAutomatedDismantling(worker);
    }

    public boolean startAutomatedDismantling(LivingEntity worker) {
        return maintenance.startAutomatedDismantling(worker);
    }

    public void cancelAutomatedDismantling() {
        maintenance.cancelAutomatedDismantling();
    }

    public boolean performAutomatedDismantleHit(LivingEntity worker) {
        return performAutomatedDismantleHit(worker, null);
    }

    public boolean performAutomatedDismantleHit(LivingEntity worker, @org.jetbrains.annotations.Nullable Container refundTarget) {
        return maintenance.performAutomatedDismantleHit(worker, refundTarget);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!(this.level() instanceof ServerLevel serverLevel) || hand != InteractionHand.MAIN_HAND) {
            return super.interact(player, hand);
        }

        if (!isFullyBuilt()) {
            return InteractionResult.PASS;
        }

        if (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL) && requestDebugInstantFire(player)) {
            return InteractionResult.SUCCESS;
        }

        if (player.getItemInHand(hand).is(Items.COMPASS) && requestDebugAutoDrive(player)) {
            return InteractionResult.SUCCESS;
        }

        if (isConstructionHammer(player.getItemInHand(hand)) && player instanceof ServerPlayer serverPlayer) {
            openMaintenanceScreen(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        if (isDismantling()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.dismantling"), true);
            return InteractionResult.SUCCESS;
        }

        if (!SiegeAccess.allows(player, this, SiegeAccess.Action.USE)) {
            if (!player.getItemInHand(hand).isEmpty() || player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("message.siegeworks.access.denied"), true);
                return InteractionResult.FAIL;
            }
            SiegeCaptureController.Refusal refusal = capture.refusal(player);
            if (refusal != null) {
                player.displayClientMessage(refusal.message(), true);
                return InteractionResult.FAIL;
            }
            return boardForCapture(player);
        }

        InteractionResult horseResult = towing.interact(player, hand, serverLevel);
        if (horseResult != null) return horseResult;

        return handleSiegeInteraction(player, hand, serverLevel);
    }

    private static boolean isConstructionHammer(ItemStack stack) {
        return stack.is(BlueprintTags.CONSTRUCTION_HAMMERS);
    }

    protected abstract InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel);

    public double getPassengersRidingOffset() {
        return 1.0D;
    }

    @Override
    public boolean dismountsUnderwater() {
        return true;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    //? if forge {
    /*// A machine does not drown; on 1.21 the vanilla can_breathe_under_water tag says so.
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }
    *///?}

    /** Siege engines are immune to potion effects. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
        if (collisionGroups().isEmpty()) {
            super.pushEntities();
        }
    }

    @Override
    public List<CollisionGroup> solidCollisionGroups() {
        return isFullyBuilt() ? CollidableStructure.super.solidCollisionGroups() : List.of();
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return isFullyBuilt() ? CollidableStructure.super.previousCollisionGroups() : List.of();
    }

    @Override
    public List<ClimbableGroup> climbableGroups() {
        return isFullyBuilt() ? CollidableStructure.super.climbableGroups() : List.of();
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return List.of();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (collisionGroups().isEmpty()) {
            return super.getBoundingBoxForCulling();
        }
        AABB modelBounds = StructureCollisionSystem.worldBounds(this);
        return modelBounds == null
                ? super.getBoundingBoxForCulling()
                : super.getBoundingBoxForCulling().minmax(modelBounds).inflate(0.25D);
    }

    @Override
    public StructureTransform collisionTransform() {
        return collisionTransformAt(getX(), getY(), getZ(), getVisualRotationYInDegrees());
    }

    @Override
    public StructureTransform previousCollisionTransform() {
        return collisionTransformAt(xo, yo, zo, yBodyRotO);
    }

    private StructureTransform collisionTransformAt(double x, double y, double z, float yaw) {
        return new StructureTransform(x, y, z, towing.collisionYaw(yaw));
    }

    @Override
    public boolean canBeCollidedWith() {
        return isFullyBuilt() && collisionGroups().isEmpty() && super.canBeCollidedWith();
    }

    @Override
    public boolean showVehicleHealth() {
        return false;
    }

    @Override
    public boolean isEffectiveAi() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public ItemStack getPickResult() {
        Item deploymentItem = SiegeDeploymentItemLookup.forEntity(this.getType());
        return deploymentItem == null ? ItemStack.EMPTY : new ItemStack(deploymentItem);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = SiegePassengerPhysics.rotatedSeatOffset(this, passenger);
        return new Vec3(getX() + offset.x, getY() + offset.y, getZ() + offset.z);
    }

    public double getVelocity(Entity entity) {
        return SiegeworksServerConfig.getMovementSpeed(getType(), isDraftMount(entity));
    }

    public double getDriveAcceleration(Entity operator) {
        return SiegeworksServerConfig.getMovementAcceleration(getType(), isDraftMount(operator));
    }

    public double getDriveDeceleration(Entity operator) {
        return SiegeworksServerConfig.getMovementDeceleration(getType(), isDraftMount(operator));
    }

    /** Deceleration when nothing drives the engine, independent of team strength. */
    public final double getRollingDeceleration() {
        return SiegeworksServerConfig.getMovementDeceleration(getType(), false);
    }

    public double getReverseSpeedMultiplier() {
        return SiegeworksServerConfig.getReverseSpeedMultiplier(getType());
    }

    public float getSteeringSpeedDegrees(Entity operator) {
        return (float) SiegeworksServerConfig.getSteeringSpeedDegrees(getType(), isDraftMount(operator));
    }

    public float getPivotSteeringSpeedDegrees(Entity operator) {
        return getSteeringSpeedDegrees(operator) * 0.65F;
    }

    public double getBaseDamage() {
        return SiegeProfileCatalogs.ENGINES.forEntity(this.getType()).baseDamage();
    }

    public ScattershotProfile getScattershotProfile() {
        return SiegeProfileCatalogs.ENGINES.forEntity(this.getType()).scattershot();
    }

    protected int getLoadingRequirementTicks(String stageKey) {
        return SiegeworksServerConfig.getLoadingRequirementTicks(getType(), stageKey);
    }

    /** Launch speed in blocks per tick, converted from the profile's metres per second. */
    public double getLaunchSpeed() {
        return SiegeProfileCatalogs.ENGINES.forEntity(this.getType()).muzzleVelocity() / 20.0D;
    }

    public float getAccuracyMultiplier() {
        return SiegeProfileCatalogs.ENGINES.forEntity(this.getType()).accuracyMultiplier();
    }

    /** Applies the profile's max health, keeping the current health fraction. */
    private void followProfileHealth() {
        SiegeProfileCatalogs.ENGINES.forEntity(getType()).maxHealth().ifPresent(wanted -> {
            AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth == null || Math.abs(maxHealth.getBaseValue() - wanted) < 1.0E-4D) {
                return;
            }
            float share = getMaxHealth() > 0.0F ? getHealth() / getMaxHealth() : 1.0F;
            maxHealth.setBaseValue(wanted);
            setHealth(getMaxHealth() * share);
        });
    }

    protected static SiegeBallistics.Flight ballisticFlight(EntityType<?> projectile) {
        ProjectilePhysicsProfile physics = SiegeProfileCatalogs.PROJECTILES.forEntity(projectile);
        return SiegeBallistics.Flight.ballistic(physics.airDrag(physics.diameterOf(projectile)));
    }
}
