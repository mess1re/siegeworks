package me.mss1r.siegeworks.entity.siege;

import me.mss1r.siegeworks.api.SiegeTransportControl;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.axiomata.collision.ClimbableGroup;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerCrewRoster;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerCrewRoster.Seat;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerCrewCoordinator;
import me.mss1r.siegeworks.gameplay.crew.tower.TowerPassengerLayout;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.gameplay.movement.tower.TowerBridgeController;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import me.mss1r.siegeworks.gameplay.upgrade.TowerBannerRack;
import me.mss1r.siegeworks.gameplay.upgrade.TowerCoveringController;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SiegeTowerEntity extends AbstractSiegeEntity
        implements GeoEntity, SiegeTransportControl {
    private static final EntityDataAccessor<Boolean> BRIDGE_OPEN =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BRIDGE_PRESET =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BRIDGE_PROGRESS =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BRIDGE_TARGET_PROGRESS =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> SEAT_DATA =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> BANNER_DATA =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> LEATHER_BOTTOM_PROGRESS =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEATHER_TOP_PROGRESS =
            SynchedEntityData.defineId(SiegeTowerEntity.class, EntityDataSerializers.INT);

    public static final int BANNER_SLOT_COUNT = 4;
    private static final int FLOOR_ONE = TowerPassengerLayout.FLOOR_ONE;
    private static final int FLOOR_TWO = TowerPassengerLayout.FLOOR_TWO;
    private static final int FLOOR_THREE = TowerPassengerLayout.FLOOR_THREE;
    private static final double SECOND_FLOOR_Y = 160.0D / 16.0D;
    private static final double LOWER_LADDER_BOTTOM_Y = 19.0D / 16.0D;
    private static final double LOWER_LADDER_BOTTOM_Z = -37.0D / 16.0D;
    private static final double LOWER_LADDER_EXIT_Z = -24.0D / 16.0D;
    private static final double LOWER_LADDER_RELEASE_Y = 166.4D / 16.0D;
    private static final double LOWER_LADDER_RELEASE_Z = -20.8D / 16.0D;
    private static final double LOWER_LADDER_APPROACH_PER_RISE =
            (LOWER_LADDER_EXIT_Z - LOWER_LADDER_BOTTOM_Z)
                    / (SECOND_FLOOR_Y - LOWER_LADDER_BOTTOM_Y);
    private static final double UPPER_LADDER_RELEASE_Y = 278.4D / 16.0D;
    private static final double THIRD_FLOOR_INTERACTION_MIN_Y = 250.0D / 16.0D;
    private static final double THIRD_FLOOR_INTERACTION_MAX_Y = 330.0D / 16.0D;
    private static final double THIRD_FLOOR_INTERACTION_HALF_WIDTH = 60.0D / 16.0D;
    private static final double THIRD_FLOOR_INTERACTION_MIN_Z = -64.0D / 16.0D;
    private static final double THIRD_FLOOR_INTERACTION_MAX_Z = 60.0D / 16.0D;
    private static final double CREW_BOARDING_MIN_Y = -1.5D;
    private static final double CREW_BOARDING_MAX_Y = 0.75D;
    private static final double CREW_BOARDING_MIN_REAR_Z = 50.0D / 16.0D;
    private static final double CREW_BOARDING_MAX_REAR_Z = 116.0D / 16.0D;
    private static final double CREW_BOARDING_SIDE_REACH = 2.5D;
    private static final double DISMOUNT_STRUCTURE_CLEARANCE = 0.15D;
    private static final AABB BODY_COLLISION_BOUNDS = GeneratedCollisionShapes.SIEGE_TOWER_BODY.enclosingBounds();
    private static final double BODY_SIDE_EXTENT = BODY_COLLISION_BOUNDS == null
            ? 4.25D
            : Math.max(Math.abs(BODY_COLLISION_BOUNDS.minX), Math.abs(BODY_COLLISION_BOUNDS.maxX));
    private static final double BODY_LENGTH_EXTENT = BODY_COLLISION_BOUNDS == null
            ? 4.25D
            : Math.max(Math.abs(BODY_COLLISION_BOUNDS.minZ), Math.abs(BODY_COLLISION_BOUNDS.maxZ));
    private static final int FULL_DRAFT_TEAM = 4;
    private static final float PUSHING_CREW_VIEW_YAW_LIMIT = 25.0F;
    private static final float PUSHING_CREW_VIEW_PITCH_LIMIT = 35.0F;
    private static final double DRAFT_TEAM_DISTANCE = 57.0D / 16.0D + 2.0D;
    private static final double[] DRAFT_TEAM_LATERAL_OFFSETS = {
            51.0D / 16.0D, 17.0D / 16.0D, -17.0D / 16.0D, -51.0D / 16.0D
    };
    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withMovementInterval(170)
            .withMovementRange(40.0);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final TowerBannerRack bannerRack = new TowerBannerRack(
            new TowerBannerRack.Host() {
                @Override
                public boolean clientSide() {
                    return level().isClientSide;
                }

                @Override
                public RegistryAccess registryAccess() {
                    return level().registryAccess();
                }

                @Override
                public CompoundTag synchronizedData() {
                    return entityData.get(BANNER_DATA);
                }

                @Override
                public void setSynchronizedData(CompoundTag data) {
                    entityData.set(BANNER_DATA, data);
                }

                @Override
                public void drop(ItemStack stack) {
                    spawnAtLocation(stack);
                }
            },
            BANNER_SLOT_COUNT);
    private final TowerCrewRoster crewRoster = new TowerCrewRoster(
            new TowerCrewRoster.Host() {
                @Override
                public boolean clientSide() {
                    return level().isClientSide;
                }

                @Override
                public List<Entity> passengers() {
                    return getPassengers();
                }

                @Override
                public String synchronizedData() {
                    return entityData.get(SEAT_DATA);
                }

                @Override
                public void setSynchronizedData(String data) {
                    entityData.set(SEAT_DATA, data);
                }
            });
    private final TowerCrewCoordinator crew = new TowerCrewCoordinator(
            new TowerCrewCoordinator.Host() {
                @Override public boolean clientSide() { return level().isClientSide; }
                @Override public boolean draftMount(Entity entity) { return isDraftMount(entity); }
                @Override public boolean pushBarsManned() { return !isTowed(); }
                @Override public boolean supportedDirectOperator(Entity entity) {
                    return isSupportedDirectOperator(entity);
                }
                @Override public void setOperator(LivingEntity operator) {
                    SiegeTowerEntity.this.setOperator(operator);
                }
            }, crewRoster);
    private final TowerBridgeController bridge = new TowerBridgeController(
            new TowerBridgeController.Host() {
                @Override public LivingEntity vehicle() { return SiegeTowerEntity.this; }
                @Override public Level level() { return SiegeTowerEntity.this.level(); }
                @Override public boolean clientSide() { return SiegeTowerEntity.this.level().isClientSide; }
                @Override public int tickCount() { return SiegeTowerEntity.this.tickCount; }
                @Override public double x() { return getX(); }
                @Override public double y() { return getY(); }
                @Override public double z() { return getZ(); }
                @Override public float visualYaw() { return getVisualRotationYInDegrees(); }
                @Override public boolean open() { return entityData.get(BRIDGE_OPEN); }
                @Override public void setOpen(boolean open) { entityData.set(BRIDGE_OPEN, open); }
                @Override public int preset() { return entityData.get(BRIDGE_PRESET); }
                @Override public void setPreset(int preset) { entityData.set(BRIDGE_PRESET, preset); }
                @Override public float progress() { return entityData.get(BRIDGE_PROGRESS); }
                @Override public void setProgress(float progress) { entityData.set(BRIDGE_PROGRESS, progress); }
                @Override public float targetProgress() { return entityData.get(BRIDGE_TARGET_PROGRESS); }
                @Override public void setTargetProgress(float progress) {
                    entityData.set(BRIDGE_TARGET_PROGRESS, progress);
                }
                @Override public Vec3 findSafeAtHeight(double x, double y, double z) {
                    return SiegeTowerEntity.this.findSafeAtHeight(x, y, z);
                }
            });
    private final TowerCoveringController covering = new TowerCoveringController(
            new TowerCoveringController.Host() {
                @Override
                public SiegeTowerEntity tower() {
                    return SiegeTowerEntity.this;
                }

                @Override
                public int bottomProgress() {
                    return entityData.get(LEATHER_BOTTOM_PROGRESS);
                }

                @Override
                public void setBottomProgress(int progress) {
                    entityData.set(LEATHER_BOTTOM_PROGRESS, progress);
                }

                @Override
                public int topProgress() {
                    return entityData.get(LEATHER_TOP_PROGRESS);
                }

                @Override
                public void setTopProgress(int progress) {
                    entityData.set(LEATHER_TOP_PROGRESS, progress);
                }
            });
    private UUID pendingDriverBoardUuid;
    private UUID pendingPusherBoardUuid;
    private boolean wheelPositionInitialized;
    private double lastWheelX;
    private double lastWheelZ;
    private float towerWheelRotation;

    public SiegeTowerEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 350.0)
                .add(Attributes.MOVEMENT_SPEED, 0.018)
                .add(Attributes.KNOCKBACK_RESISTANCE, 420.0);
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
        data.define(BRIDGE_OPEN, false);
        data.define(BRIDGE_PRESET, TowerBridgeController.DEFAULT_PRESET);
        data.define(BRIDGE_PROGRESS, 0.0F);
        data.define(BRIDGE_TARGET_PROGRESS, 0.0F);
        data.define(SEAT_DATA, "");
        data.define(BANNER_DATA, new CompoundTag());
        data.define(LEATHER_BOTTOM_PROGRESS, 0);
        data.define(LEATHER_TOP_PROGRESS, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bridge.load(tag);
        crewRoster.load(tag);
        bannerRack.load(tag);
        covering.load(tag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        bridge.save(tag);
        crewRoster.save(tag);
        bannerRack.save(tag);
        covering.save(tag);
    }

    public ItemStack getTowerBanner(int slot) {
        return bannerRack.get(slot);
    }

    public int getLeatherBottomProgress() {
        return covering.bottomProgress();
    }

    public int getLeatherTopProgress() {
        return covering.topProgress();
    }

    public boolean hasLeatherBottom() {
        return covering.hasBottom();
    }

    public boolean hasLeatherTop() {
        return covering.hasTop();
    }

    public boolean hasLeatherCovering() {
        return covering.installed();
    }

    public Map<ResourceLocation, Integer> getLeatherMaterials() {
        return covering.materials();
    }

    @Override
    public boolean fireImmune() {
        return hasLeatherCovering() || super.fireImmune();
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(ticks > 0 && hasLeatherCovering() ? 0 : ticks);
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }

    @Override
    public TowingProfile towingProfile() {
        return TowingProfile.pushedFromBehind(
                DRAFT_TEAM_DISTANCE, DRAFT_TEAM_LATERAL_OFFSETS);
    }

    @Override
    public void tick() {
        super.tick();
        releasePushersUnderHarness();
        bridge.tick();
        updateTowerWheelRotation();
        StructureMotionSystem.tickStructure(this);
    }

    private void releasePushersUnderHarness() {
        if (level().isClientSide() || !isTowed()) {
            return;
        }
        for (Entity passenger : List.copyOf(getPassengers())) {
            if (isPusher(passenger)) {
                passenger.stopRiding();
            }
        }
    }

    public boolean isBridgeOpen() {
        return bridge.open();
    }

    @Override
    public boolean isDeployed() {
        return isBridgeOpen();
    }

    @Override
    public void setDeployed(LivingEntity operator, boolean deployed) {
        if (!isOperator(operator) || isBridgeOpen() == deployed || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        bridge.setOpen(deployed);
        serverLevel.playSound(null, blockPosition(), deployed ? SoundEvents.WOOD_PLACE : SoundEvents.CHAIN_PLACE,
                SoundSource.BLOCKS, 0.9F, deployed ? 0.85F : 0.75F);
    }

    @Override
    public double getAutomatedDeploymentDistance() {
        return TowerBridgeController.AUTOMATED_DEPLOYMENT_DISTANCE;
    }

    public int getBridgePreset() {
        return bridge.preset();
    }

    public float getBridgeProgress() {
        return bridge.progress();
    }

    public float getBridgeTargetProgress() {
        return bridge.targetProgress();
    }

    public float getRenderedBridgeProgress(float partialTick) {
        return bridge.renderedProgress(partialTick);
    }

    public double getBridgePlatformAngleRadians() {
        return bridge.platformAngleRadians();
    }

    public float getBridgeAngleRadians() {
        return bridge.angleRadians();
    }

    @Override
    public List<CollisionGroup> collisionGroups() {
        return List.of(
                CollisionGroup.fixed("body", GeneratedCollisionShapes.SIEGE_TOWER_BODY),
                new CollisionGroup("bridge", GeneratedCollisionShapes.SIEGE_TOWER_BRIDGE,
                        getBridgeAngleRadians()));
    }

    @Override
    public int crewCapacity() {
        int interior = 0;
        for (int floor : TowerPassengerLayout.floorCapacities()) {
            interior += floor;
        }
        return 1 + TowerPassengerLayout.pusherCapacity() + interior;
    }

    @Override
    public boolean isTerrainCollisionExcluded(CollisionGroup group) {
        return "bridge".equals(group.name()) && isBridgeOpen();
    }

    @Override
    public List<CollisionGroup> previousCollisionGroups() {
        return List.of(
                CollisionGroup.fixed("body", GeneratedCollisionShapes.SIEGE_TOWER_BODY),
                new CollisionGroup("bridge", GeneratedCollisionShapes.SIEGE_TOWER_BRIDGE,
                        bridge.previousAngleRadians()));
    }

    @Override
    public List<ClimbableGroup> climbableGroups() {
        return List.of(
                new ClimbableGroup(
                        CollisionGroup.fixed("lower_ladder", GeneratedCollisionShapes.SIEGE_TOWER_LOWER_LADDER),
                        Direction.Axis.Z,
                        SECOND_FLOOR_Y,
                        LOWER_LADDER_EXIT_Z,
                        LOWER_LADDER_APPROACH_PER_RISE,
                        LOWER_LADDER_RELEASE_Y,
                        LOWER_LADDER_RELEASE_Z),
                new ClimbableGroup(
                        CollisionGroup.fixed("upper_ladder", GeneratedCollisionShapes.SIEGE_TOWER_UPPER_LADDER),
                        Direction.Axis.X, UPPER_LADDER_RELEASE_Y, ClimbableGroup.FACING_NEGATIVE));
    }

    public float getRenderedBridgeAngleRadians(float partialTick) {
        return bridge.renderedAngleRadians(partialTick);
    }

    @Override
    public InteractionResult handleSiegeInteraction(Player player, InteractionHand hand, ServerLevel serverLevel) {
        ItemStack itemStack = player.getItemInHand(hand);
        boolean customizationItem = covering.accepts(itemStack)
                || itemStack.getItem() instanceof BannerItem
                || itemStack.is(Items.SHEARS);
        if (customizationItem && !canCustomizeTower(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.siegeworks.tower.customization_not_owner"), true);
            return InteractionResult.SUCCESS;
        }

        if (covering.accepts(itemStack)) {
            return covering.add(player, itemStack, serverLevel);
        }

        if (itemStack.getItem() instanceof BannerItem) {
            int installedSlot = bannerRack.install(itemStack);
            if (installedSlot < 0) {
                player.displayClientMessage(Component.translatable("message.siegeworks.tower.banners_full"), true);
                return InteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                itemStack.shrink(1);
            }
            serverLevel.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.tower.banner_installed", installedSlot + 1, BANNER_SLOT_COUNT),
                    true);
            return InteractionResult.SUCCESS;
        }

        if (itemStack.is(Items.SHEARS)) {
            if ((player.isShiftKeyDown() || player.isSecondaryUseActive())
                    && (getLeatherTopProgress() > 0 || getLeatherBottomProgress() > 0)) {
                return covering.remove(player, itemStack, hand, serverLevel);
            }

            ItemStack removedBanner = bannerRack.removeLast();
            if (removedBanner.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.siegeworks.tower.no_banners"), true);
                return InteractionResult.SUCCESS;
            }

            if (!player.getInventory().add(removedBanner)) {
                player.drop(removedBanner, false);
            }
            me.mss1r.siegeworks.platform.MinecraftVersionCompat.damageHeldItem(itemStack, 1, player, hand);
            serverLevel.playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        if (itemStack.isEmpty() && isPlayerOnThirdFloor(player)
                && player instanceof ServerPlayer serverPlayer) {
            return handleBridgeControl(serverPlayer);
        }

        if (player.isShiftKeyDown() || player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        if (!itemStack.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (driverSlotAvailable(player.getUUID())) {
            return isPlayerInDriverBoardingArea(player)
                    ? boardDriver(player)
                    : InteractionResult.PASS;
        }
        if (!isPlayerInCrewBoardingArea(player)) {
            return InteractionResult.PASS;
        }
        return boardPusherPlayer(player);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 hitPosition, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!isPlayerOnThirdFloor(player)) {
            if (player.isShiftKeyDown() || player.isSecondaryUseActive()
                    || (driverSlotAvailable(player.getUUID())
                    ? !isPlayerInDriverBoardingArea(player)
                    : !isPlayerInCrewBoardingArea(player))) {
                return InteractionResult.PASS;
            }
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return handleSiegeInteraction(player, hand, (ServerLevel) level());
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        return handleBridgeControl(serverPlayer);
    }

    private InteractionResult handleBridgeControl(ServerPlayer player) {
        if (player.isShiftKeyDown() || player.isSecondaryUseActive()) {
            requestBridgePresetCycle(player);
        } else {
            requestBridgeToggle(player);
        }
        return InteractionResult.SUCCESS;
    }

    private boolean isPlayerOnThirdFloor(Player player) {
        Vec3 localPosition = collisionTransform().toLocal(player.position());
        return localPosition.y >= THIRD_FLOOR_INTERACTION_MIN_Y
                && localPosition.y <= THIRD_FLOOR_INTERACTION_MAX_Y
                && Math.abs(localPosition.x) <= THIRD_FLOOR_INTERACTION_HALF_WIDTH
                && localPosition.z >= THIRD_FLOOR_INTERACTION_MIN_Z
                && localPosition.z <= THIRD_FLOOR_INTERACTION_MAX_Z;
    }

    private boolean isPlayerInCrewBoardingArea(Player player) {
        Vec3 localPosition = collisionTransform().toLocal(player.position());
        return localPosition.y >= CREW_BOARDING_MIN_Y
                && localPosition.y <= CREW_BOARDING_MAX_Y
                && localPosition.z >= CREW_BOARDING_MIN_REAR_Z
                && localPosition.z <= CREW_BOARDING_MAX_REAR_Z
                && Math.abs(localPosition.x) <= BODY_SIDE_EXTENT + CREW_BOARDING_SIDE_REACH;
    }

    private boolean isPlayerInDriverBoardingArea(Player player) {
        Vec3 localPosition = collisionTransform().toLocal(player.position());
        return localPosition.y >= CREW_BOARDING_MIN_Y
                && localPosition.y <= CREW_BOARDING_MAX_Y
                && Math.abs(localPosition.x) <= BODY_SIDE_EXTENT + CREW_BOARDING_SIDE_REACH
                && Math.abs(localPosition.z) <= BODY_LENGTH_EXTENT + CREW_BOARDING_SIDE_REACH;
    }

    private boolean canCustomizeTower(Player player) {
        return player.getAbilities().instabuild || SiegeAccess.allows(player, this, SiegeAccess.Action.USE);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) {
            bannerRack.dropAll();
        }
        super.remove(reason);
    }

    @Override
    protected InteractionResult boardForCapture(Player player) {
        return isPlayerInDriverBoardingArea(player) ? boardDriver(player) : InteractionResult.PASS;
    }

    private InteractionResult boardDriver(Player player) {
        if (player.getVehicle() == this) {
            return InteractionResult.SUCCESS;
        }

        UUID uuid = player.getUUID();
        if (!driverSlotAvailable(uuid)) {
            return InteractionResult.SUCCESS;
        }

        crewRoster.clearReservation(uuid);

        pendingDriverBoardUuid = uuid;
        try {
            if (!player.startRiding(this)) {
                return InteractionResult.PASS;
            }
        } finally {
            pendingDriverBoardUuid = null;
        }

        crewRoster.assignDriver(uuid);
        setOperator(player);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult boardPusherPlayer(Player player) {
        if (player.getVehicle() == this) {
            return InteractionResult.SUCCESS;
        }

        UUID uuid = player.getUUID();
        int slot = firstFreePusherSlot();
        if (slot < 0) {
            player.displayClientMessage(Component.translatable("siege.tower.no_pusher_room"), true);
            return InteractionResult.SUCCESS;
        }

        crewRoster.assignPusher(uuid, slot);
        pendingPusherBoardUuid = uuid;
        try {
            if (!player.startRiding(this, true)) {
                crewRoster.removePusher(uuid);
                return InteractionResult.PASS;
            }
        } finally {
            pendingPusherBoardUuid = null;
        }

        return InteractionResult.SUCCESS;
    }

    private void requestBridgeToggle(ServerPlayer player) {
        boolean bridgeOpen = !isBridgeOpen();
        bridge.setOpen(bridgeOpen);
        player.serverLevel().playSound(null, blockPosition(),
                bridgeOpen ? SoundEvents.WOOD_PLACE : SoundEvents.CHAIN_PLACE,
                SoundSource.BLOCKS, 0.9F, bridgeOpen ? 0.85F : 0.75F);
    }

    private void requestBridgePresetCycle(ServerPlayer player) {
        String presetKey = bridge.cyclePreset();
        player.displayClientMessage(Component.translatable("siege.tower.bridge.angle",
                Component.translatable(presetKey)), true);
        player.serverLevel().playSound(null, blockPosition(), SoundEvents.WOODEN_BUTTON_CLICK_ON,
                SoundSource.BLOCKS, 0.7F, 0.85F + bridge.preset() * 0.12F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
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
    protected boolean takesPassengersWhileTowed(Entity entity) {
        return true;
    }

    @Override
    protected boolean canAddOperator(Entity entity) {
        return crew.canAddOperator(entity, pendingDriverBoardUuid, pendingPusherBoardUuid);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        crew.passengerAdded(passenger);
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return getPassengers().size() < crew.capacity();
    }

    @Override
    protected boolean operatorControlsRotation(Entity passenger) {
        return isDriver(passenger);
    }

    public boolean isInteriorPassenger(Entity passenger) {
        return !(passenger instanceof Player) && getSeat(passenger) != null;
    }

    public boolean isPusher(Entity passenger) {
        return getPusherSlot(passenger) != null;
    }

    public boolean isPushingCrew(Entity passenger) {
        return isDriver(passenger) || isPusher(passenger);
    }

    public int getPushingCrewSlot(Entity passenger) {
        if (isDriver(passenger)) {
            return 0;
        }
        Integer pusherSlot = getPusherSlot(passenger);
        return pusherSlot == null ? -1 : pusherSlot + 1;
    }

    @Override
    public float getPassengerViewYawLimit(Entity passenger) {
        return isPushingCrew(passenger)
                ? PUSHING_CREW_VIEW_YAW_LIMIT
                : super.getPassengerViewYawLimit(passenger);
    }

    @Override
    public float getPassengerViewPitchLimit(Entity passenger) {
        return isPushingCrew(passenger)
                ? PUSHING_CREW_VIEW_PITCH_LIMIT
                : super.getPassengerViewPitchLimit(passenger);
    }

    public boolean hasActiveDriver() {
        return getDriverPassenger() != null;
    }

    public boolean reservePusher(LivingEntity passenger) {
        return crew.reservePusher(passenger, this);
    }

    public boolean reserveDriver(LivingEntity passenger) {
        return crew.reserveDriver(passenger);
    }

    public boolean reserveInteriorSeat(LivingEntity passenger) {
        return crew.reserveInteriorSeat(passenger, this);
    }

    public void cancelBoardingReservation(Entity passenger) {
        crew.cancelReservation(passenger, this);
    }

    @Override
    public boolean preparePassengerForAutomatedExit(LivingEntity passenger) {
        return crew.prepareForSecondFloorExit(passenger, this);
    }

    @Override
    public boolean worthDeployingHere() {
        return bridge.facesSomethingToLandOn();
    }

    public boolean bridgeLeadsSomewhere() {
        return bridge.leadsSomewhere();
    }

    public boolean bridgeMoving() {
        return bridge.moving();
    }

    @Override
    public boolean disembarkPassengerToGround(LivingEntity passenger, boolean force) {
        if (passenger.getVehicle() != this) {
            return false;
        }
        if (!crew.prepareForGroundExit(passenger, this) && !force) {
            return false;
        }

        Seat seat = getServerSeat(passenger);
        Vec3 spot = seat == null
                ? null
                : findGroundDismount(passenger,
                        TowerPassengerLayout.clampedSeatOffset(seat.floor(), seat.slot()));
        passenger.stopRiding();
        if (spot != null) {
            passenger.moveTo(spot.x, spot.y, spot.z, passenger.getYRot(), passenger.getXRot());
        }
        return true;
    }

    @Override
    public boolean canPassengerExit(LivingEntity passenger) {
        Seat seat = getServerSeat(passenger);
        return bridge.canPassengerExit(passenger, seat);
    }

    @Override
    public boolean disembarkPassenger(LivingEntity passenger) {
        return bridge.disembarkPassenger(passenger, getServerSeat(passenger));
    }

    @Override
    public ExitResult advanceAutomatedExit(LivingEntity passenger) {
        return bridge.advanceAutomatedExit(passenger);
    }

    public Vec3 getAutomatedReturnApproach(LivingEntity passenger) {
        return bridge.automatedReturnApproach(passenger,
                crewRoster.serverSeat(passenger.getUUID()));
    }

    public boolean beginAutomatedReturn(LivingEntity passenger) {
        return bridge.beginAutomatedReturn(passenger, crewRoster.serverSeat(passenger.getUUID()));
    }

    public boolean hasBridgePassengers() {
        return bridge.hasBridgePassengers();
    }

    public ExitResult advanceAutomatedReturn(LivingEntity passenger) {
        return bridge.advanceAutomatedReturn(passenger);
    }

    public void cancelAutomatedReturn(LivingEntity passenger) {
        bridge.cancelAutomatedReturn(passenger);
    }

    @Override
    public void cancelAutomatedExit(LivingEntity passenger) {
        bridge.cancelAutomatedExit(passenger);
    }

    @Override
    protected Vec3 getOperatorOffset(Entity entity) {
        if (isDriver(entity)) {
            return TowerPassengerLayout.driverOffset();
        }

        Integer pusherSlot = getPusherSlot(entity);
        if (pusherSlot != null && TowerPassengerLayout.validPusherSlot(pusherSlot)) {
            return TowerPassengerLayout.pusherOffset(pusherSlot);
        }

        Seat seat = getSeat(entity);
        if (seat == null) {
            return Vec3.ZERO;
        }

        return TowerPassengerLayout.seatOffset(seat.floor(), seat.slot());
    }

    @Override
    public Vec3 getPlayerPOV() {
        return new Vec3(0.0, -0.7F, 0.0);
    }

    @Override
    protected float getYawTurnSpeedDegrees() {
        return 0.22F;
    }

    @Override
    public double getVelocity(Entity operator) {
        if (isDraftMount(operator)) {
            return super.getVelocity(operator) * getDraftTeamPower();
        }
        return super.getVelocity(operator) * getCrewSpeedMultiplier();
    }

    @Override
    public double getDriveAcceleration(Entity operator) {
        return isDraftMount(operator)
                ? super.getDriveAcceleration(operator) * getDraftTeamPower()
                : super.getDriveAcceleration(operator) * getCrewSpeedMultiplier();
    }

    @Override
    public double getDriveDeceleration(Entity operator) {
        return isDraftMount(operator)
                ? super.getDriveDeceleration(operator) * getDraftTeamPower()
                : super.getDriveDeceleration(operator) * getCrewSpeedMultiplier();
    }

    @Override
    public float getSteeringSpeedDegrees(Entity operator) {
        double teamPower = isDraftMount(operator) ? getDraftTeamPower() : getCrewSpeedMultiplier();
        return (float) (super.getSteeringSpeedDegrees(operator) * teamPower);
    }

    public int getActivePushingCrewCount() {
        int activeCrew = 0;
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof LivingEntity living && living.isAlive()
                    && (isDriver(passenger) || isPusher(passenger))) {
                activeCrew++;
            }
        }
        return activeCrew;
    }

    private double getCrewSpeedMultiplier() {
        return SiegeworksServerConfig.getTowerCrewSpeedMultiplier(getActivePushingCrewCount());
    }

    private double getDraftTeamPower() {
        return Math.min(1.0D, (double) getTowingMounts().size() / FULL_DRAFT_TEAM);
    }

    @Override
    protected void positionRider(Entity entity, MoveFunction moveFunction) {
        if (!hasPassenger(entity)) {
            return;
        }

        if (isDriver(entity)) {
            SiegePassengerPhysics.updatePassengerState(this, entity);
        }
        if (isPushingCrew(entity) || !(entity instanceof Player)) {
            alignPassengerToTower(entity);
        }

        Vec3 seat = position().add(rotateLocalOffset(getPassengerOffset(entity)));
        if (isDraftMount(entity)) {
            seat = SiegePassengerPhysics.mountFooting(this, entity, seat);
        }
        moveFunction.accept(entity, seat.x, seat.y, seat.z);
    }

    @Override
    public Vec3 boardingSpot(LivingEntity passenger) {
        return findGroundDismount(passenger, TowerPassengerLayout.driverOffset());
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Integer pusherSlot = getPusherSlot(passenger);
        if (pusherSlot != null && TowerPassengerLayout.validPusherSlot(pusherSlot)) {
            return findGroundDismount(passenger, TowerPassengerLayout.pusherOffset(pusherSlot));
        }
        if (isDriver(passenger) || getSeat(passenger) == null) {
            return findGroundDismount(passenger, TowerPassengerLayout.driverOffset());
        }

        Seat seat = getSeat(passenger);
        return findInteriorFloorDismount(seat);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        crew.passengerRemoved(passenger);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return !entity.isPassengerOfSameVehicle(this) && (entity.canBeCollidedWith() || entity.isPushable());
    }

    private void updateTowerWheelRotation() {
        if (!wheelPositionInitialized) {
            wheelPositionInitialized = true;
            lastWheelX = getX();
            lastWheelZ = getZ();
            towerWheelRotation = wheelRotation;
            return;
        }

        double dx = getX() - lastWheelX;
        double dz = getZ() - lastWheelZ;
        lastWheelX = getX();
        lastWheelZ = getZ();

        double horizontalDistanceSqr = dx * dx + dz * dz;
        if (horizontalDistanceSqr > 1.0E-6D && horizontalDistanceSqr < 1.0D) {
            float yawRadians = (float) Math.toRadians(getVisualRotationYInDegrees());
            Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
            double directionalDistance = dx * forward.x + dz * forward.z;
            towerWheelRotation += (float) (-directionalDistance * 72.0D);
            towerWheelRotation %= 360.0F;
            if (towerWheelRotation < 0.0F) {
                towerWheelRotation += 360.0F;
            }
        }

        wheelRotation = towerWheelRotation;
    }

    private Vec3 findInteriorFloorDismount(Seat seat) {
        Vec3 local = TowerPassengerLayout.clampedSeatOffset(seat.floor(), seat.slot());
        return collisionTransform().toWorld(local);
    }

    private Vec3 findGroundDismount(Entity passenger, Vec3 localOffset) {
        double side = localOffset.x < 0.0D ? -1.0D : 1.0D;
        double clearSide = BODY_SIDE_EXTENT
                + passenger.getBbWidth() * 0.5D
                + DISMOUNT_STRUCTURE_CLEARANCE;
        Vec3 outside = new Vec3(side * Math.max(Math.abs(localOffset.x), clearSide), 0.0D, localOffset.z);
        Vec3 rotated = rotateLocalOffset(outside);
        double x = getX() + rotated.x;
        double z = getZ() + rotated.z;
        Vec3 safe = findSafeAtHeight(x, getY() + 0.1D, z);
        return safe == null ? new Vec3(x, getY() + 0.1D, z) : safe;
    }

    private Vec3 findSafeAtHeight(double x, double y, double z) {
        int centerY = Mth.floor(y);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos feet = BlockPos.containing(x, centerY + dy, z);
            BlockPos support = feet.below();
            if (isFree(feet) && isFree(feet.above()) && isSolidTop(support)) {
                return new Vec3(x, feet.getY(), z);
            }
        }
        return null;
    }

    private boolean isFree(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return state.getCollisionShape(level(), pos).isEmpty();
    }

    private boolean isSolidTop(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level(), pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) >= 0.75D;
    }

    private Vec3 rotateLocalOffset(Vec3 offset) {
        float yawRadians = (float) Math.toRadians(getPassengerOffsetYaw(this));
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        double offsetX = rightX * offset.x - forwardX * offset.z;
        double offsetZ = rightZ * offset.x - forwardZ * offset.z;
        return new Vec3(offsetX, offset.y, offsetZ);
    }

    private void alignPassengerToTower(Entity passenger) {
        float towerYaw = getVisualRotationYInDegrees();
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(towerYaw);
            living.yBodyRotO = towerYaw;
        }
        if (!(passenger instanceof Player)) {
            passenger.setYRot(towerYaw);
            passenger.setYHeadRot(towerYaw);
            passenger.setXRot(0.0F);
            passenger.yRotO = towerYaw;
            passenger.xRotO = 0.0F;
            if (passenger instanceof LivingEntity living) {
                living.yHeadRotO = towerYaw;
            }
        }
    }

    private boolean isDriver(Entity entity) {
        return crew.isDriver(entity);
    }

    private Seat getSeat(Entity entity) {
        return crew.seat(entity);
    }

    private Seat getServerSeat(Entity entity) {
        return crew.serverSeat(entity);
    }

    private Integer getPusherSlot(Entity entity) {
        return crew.pusherSlot(entity);
    }

    private void assignSeat(UUID uuid, int floor, int slot) {
        crewRoster.assignSeat(uuid, floor, slot);
    }

    private boolean driverSlotAvailable(UUID candidate) {
        return crewRoster.driverSlotAvailable(candidate);
    }

    private Entity getDriverPassenger() {
        return crew.driverPassenger();
    }

    private Seat firstFreeInteriorSeat() {
        return crewRoster.firstFreeInteriorSeat();
    }

    private int firstFreeSlot(int floor) {
        return crewRoster.firstFreeSlot(floor);
    }

    private int firstFreePusherSlot() {
        return crewRoster.firstFreePusherSlot();
    }

}
