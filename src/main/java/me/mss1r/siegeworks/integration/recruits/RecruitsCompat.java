package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.axiomata.blueprint.api.BlueprintPermissions;
import me.mss1r.axiomata.blueprint.api.BlueprintStacks;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeAllianceRegistry;
import me.mss1r.siegeworks.api.SiegeAmmunitionControl;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeMeleeControl;
import me.mss1r.siegeworks.api.SiegeOperatorRegistry;
import me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsSiegeCommandC2SPayload;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsNetworking;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsTowerCrewC2SPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RecruitsCompat {
    /**
     * Same range as Recruits' own commands, so siege orders from its screen reach as far. Map orders keep the RTS
     * radius.
     */
    static final double COMMAND_RANGE = 200.0D;
    private static final double COMMAND_RANGE_SQR = COMMAND_RANGE * COMMAND_RANGE;
    private static final ResourceLocation OPERATOR_TYPE = MinecraftVersionCompat.id(Siegeworks.MOD_ID, "recruits");

    private RecruitsCompat() {
    }

    public static void register(IEventBus modBus) {
        RecruitsNetworking.register();
        SiegeOperatorRegistry.register(OPERATOR_TYPE, entity -> entity instanceof SiegeEngineerEntity);
        SiegePlayerAttributionRegistry.register(OPERATOR_TYPE, entity ->
                entity instanceof AbstractRecruitEntity recruit ? recruit.getOwnerUUID() : null);
        SiegeAllianceRegistry.register(OPERATOR_TYPE, RecruitsCompat::alliedFactions);
        modBus.addListener(RecruitsCompat::onLoadComplete);
        modBus.addListener(RecruitsCompat::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(RecruitsCompat::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(RecruitsCompat::onServerStarting);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RecruitsNetworking.registerClient();
            RecruitsSiegeCommandCategory.register();
        });
    }

    public static void handleSiegeCommand(ServerPlayer player, int action, List<UUID> groupIds, BlockPos targetPos,
                                          int targetEntityId, ResourceLocation siegeTypeId) {
        RecruitsDebug.commandReceived(player, "siege", action, groupIds.size(), targetPos, targetEntityId);
        if (groupIds.isEmpty()) {
            return;
        }

        Set<UUID> selectedGroups = new HashSet<>(groupIds);
        List<AbstractRecruitEntity> nearbyRecruits = player.serverLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class, player.getBoundingBox().inflate(COMMAND_RANGE));
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_RETURN_TOWER) {
            sendCommandFeedback(player, handleTowerReturn(player, nearbyRecruits, selectedGroups));
            return;
        }
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_UNLOAD_TOWER) {
            int applied = siegeTypeId == null
                    || siegeTypeId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(SiegeworksEntities.SIEGE_TOWER_ENTITY.get()))
                    ? handleTowerUnload(player, nearbyRecruits, selectedGroups)
                    : 0;
            sendCommandFeedback(player, applied);
            return;
        }
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_BUILD) {
            handleBuildCommand(player, nearbyRecruits, selectedGroups, targetPos);
            return;
        }
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_PICKUP_LADDER
                || action == RecruitsSiegeCommandC2SPayload.ACTION_PLACE_LADDER) {
            handleLadderRelocationCommand(player, nearbyRecruits, selectedGroups,
                    action, targetPos, targetEntityId);
            return;
        }
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_CREW_MACHINE) {
            Entity target = player.serverLevel().getEntity(targetEntityId);
            int applied = target instanceof AbstractSiegeEntity siege
                    && !siege.isRemoved() && withinCommandRange(player, siege)
                    ? crewMachine(player, siege,
                            recruit -> isSelectedAndCommandable(player, recruit, selectedGroups))
                    : 0;
            sendCommandFeedback(player, applied);
            return;
        }
        if (action >= RecruitsSiegeCommandC2SPayload.ACTION_REPAIR
                && action <= RecruitsSiegeCommandC2SPayload.ACTION_CANCEL_MAINTENANCE) {
            handleMaintenanceCommand(player, nearbyRecruits, selectedGroups, action, targetEntityId);
            return;
        }

        boolean validTarget = isValidCommandTarget(player, targetPos);
        boolean validSupply = validTarget && player.serverLevel().mayInteract(player, targetPos)
                && mayOpenInClaim(player, targetPos) && isContainer(player, targetPos);
        int applied = 0;
        for (AbstractRecruitEntity recruit : nearbyRecruits) {
            if (!isSelectedAndCommandable(player, recruit, selectedGroups)) {
                continue;
            }

            if (action == RecruitsSiegeCommandC2SPayload.ACTION_LEAVE_ENGINE) {
                if (matchesSelectedMachine(recruit, siegeTypeId) && leaveEngine(player, recruit)) {
                    applied++;
                }
                continue;
            }

            if (action == RecruitsSiegeCommandC2SPayload.ACTION_SET_SUPPLIES) {
                if (validSupply && recruit instanceof SiegeEngineerEntity engineer) {
                    engineer.setUpkeepPos(targetPos.immutable());
                    engineer.clearUpkeepEntity();
                    engineer.forcedUpkeep = false;
                    applied++;
                }
                continue;
            }

            if (!(recruit instanceof SiegeEngineerEntity engineer)) {
                continue;
            }
            AbstractSiegeEntity siege = workedMachine(engineer);
            if (siege == null || !siege.isOperator(engineer) || !matchesSiegeType(siege, siegeTypeId)) {
                continue;
            }

            if (action >= RecruitsSiegeCommandC2SPayload.ACTION_FIRE_POSITION
                    && action <= RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL) {
                if ((siege instanceof SiegeArtilleryControl || siege instanceof SiegeMeleeControl)
                        && applyFireCommand(engineer, siege, action, validTarget ? targetPos : null)) {
                    applied++;
                }
                continue;
            }

            if (action >= RecruitsSiegeCommandC2SPayload.ACTION_AMMO_AUTO
                    && action <= RecruitsSiegeCommandC2SPayload.ACTION_AMMO_INCENDIARY) {
                SiegeAmmunitionMode mode = ammunitionModeForAction(action);
                if (mode != null && siege instanceof SiegeAmmunitionControl ammunition
                        && ammunition.supportsAmmunitionMode(mode)) {
                    ammunition.setAutomatedAmmunitionMode(mode);
                    applied++;
                }
                continue;
            }

            if (action == RecruitsSiegeCommandC2SPayload.ACTION_FLAP_OPEN
                    || action == RecruitsSiegeCommandC2SPayload.ACTION_FLAP_CLOSE) {
                boolean open = action == RecruitsSiegeCommandC2SPayload.ACTION_FLAP_OPEN;
                if (setMantletFlap(player, siege, open, false)) {
                    applied++;
                }
                continue;
            }

            if (siege instanceof SiegeTowerEntity
                    && engineer.siegeController instanceof SiegeworksRecruitController controller) {
                if (action != RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_LOWER
                        && action != RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_RAISE
                        && action != RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_AUTO) {
                    continue;
                }
                Boolean override = action == RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_LOWER
                        ? true
                        : action == RecruitsSiegeCommandC2SPayload.ACTION_BRIDGE_RAISE ? false : null;
                controller.setDeploymentOverride(override);
                applied++;
            }
        }

        sendCommandFeedback(player, applied);
    }

    /** Whether a recruit operates, rides, pushes or tows a machine of the selected type. */
    static boolean matchesSelectedMachine(AbstractRecruitEntity recruit, ResourceLocation siegeTypeId) {
        if (siegeTypeId == null) {
            return true;
        }
        if (recruit instanceof SiegeEngineerEntity engineer && matchesSiegeType(workedMachine(engineer), siegeTypeId)) {
            return true;
        }
        Entity vehicle = recruit.getVehicle() instanceof AbstractHorse mount ? mount.getVehicle() : recruit.getVehicle();
        return vehicle instanceof AbstractSiegeEntity siege && matchesSiegeType(siege, siegeTypeId);
    }

    private static boolean matchesSiegeType(AbstractSiegeEntity siege, ResourceLocation siegeTypeId) {
        return siegeTypeId == null || siege != null
                && siegeTypeId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(siege.getType()));
    }

    public static void handleFireZoneCommand(ServerPlayer player, List<UUID> groupIds,
                                             BlockPos requestedCenter, int requestedRadius) {
        RecruitsDebug.commandReceived(player, "fire zone", -1, groupIds.size(), null, -1);
        if (groupIds.isEmpty() || requestedCenter == null) {
            return;
        }

        BlockPos sample = new BlockPos(requestedCenter.getX(), player.blockPosition().getY(), requestedCenter.getZ());
        if (player.distanceToSqr(Vec3.atCenterOf(sample)) > COMMAND_RANGE_SQR
                || !player.serverLevel().hasChunkAt(sample)) {
            sendCommandFeedback(player, 0);
            return;
        }

        int surfaceY = Math.max(player.serverLevel().getMinBuildHeight(),
                player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        requestedCenter.getX(), requestedCenter.getZ()) - 1);
        BlockPos center = new BlockPos(requestedCenter.getX(), surfaceY, requestedCenter.getZ());

        Set<UUID> selectedGroups = new HashSet<>(groupIds);
        int applied = applyFireZone(player, center, requestedRadius,
                recruit -> isSelectedAndCommandable(player, recruit, selectedGroups));
        sendCommandFeedback(player, applied);
    }

    public static int applyFireZone(ServerPlayer player, BlockPos center, int requestedRadius,
                             java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        return applyFireZone(player, center, requestedRadius, requestedRadius, false, chosen);
    }

    public static int applyFireZone(ServerPlayer player, BlockPos center, int requestedRadiusX,
                                    int requestedRadiusZ, boolean rectangular,
                                    java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        return applyFireZone(center, requestedRadiusX, requestedRadiusZ, rectangular,
                player.serverLevel().getEntitiesOfClass(AbstractRecruitEntity.class,
                        player.getBoundingBox().inflate(COMMAND_RANGE), chosen::test));
    }

    /** Assigns a fire zone to the recruits operating artillery engines. */
    public static int applyFireZone(BlockPos center, int requestedRadiusX, int requestedRadiusZ,
                                    boolean rectangular, List<AbstractRecruitEntity> recruits) {
        int limit = SiegeworksServerConfig.getRecruitFireZoneMaxRadius();
        int radiusX = Mth.clamp(requestedRadiusX, 1, limit);
        int radiusZ = Mth.clamp(requestedRadiusZ, 1, limit);
        int applied = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            if (!(recruit instanceof SiegeEngineerEntity engineer)) {
                continue;
            }
            AbstractSiegeEntity siege = workedMachine(engineer);
            if (siege == null || !siege.isOperator(engineer)
                    || !(siege instanceof SiegeArtilleryControl)) {
                continue;
            }

            RecruitsFireZone.set(engineer, center, radiusX, radiusZ, rectangular);
            engineer.setStrategicFirePos(center);
            engineer.setShouldStrategicFire(true);
            engineer.setShouldRanged(true);
            applied++;
        }
        return applied;
    }

    public static int clearFireZone(ServerPlayer player,
                                    java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        return clearFireZone(player.serverLevel().getEntitiesOfClass(AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(COMMAND_RANGE), chosen::test));
    }

    public static int clearFireZone(List<AbstractRecruitEntity> recruits) {
        int cleared = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            if (!(recruit instanceof SiegeEngineerEntity engineer)) continue;
            RecruitsFireZone.clear(engineer);
            engineer.setShouldStrategicFire(false);
            cleared++;
        }
        return cleared;
    }

    public static boolean commandable(ServerPlayer player, AbstractRecruitEntity recruit) {
        return isCommandable(player, recruit);
    }

    public static List<AbstractRecruitEntity> commandRangeRecruits(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class, player.getBoundingBox().inflate(COMMAND_RANGE));
    }

    private static void handleMaintenanceCommand(ServerPlayer player,
                                                 List<AbstractRecruitEntity> nearbyRecruits,
                                                 Set<UUID> selectedGroups, int action, int targetEntityId) {
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_CANCEL_MAINTENANCE) {
            int cancelled = 0;
            for (AbstractRecruitEntity recruit : nearbyRecruits) {
                if (!isSelectedAndCommandable(player, recruit, selectedGroups)) {
                    continue;
                }
                boolean taskCancelled = RecruitsLadderRelocationController.cancelTask(recruit);
                if (recruit instanceof SiegeEngineerEntity engineer) {
                    taskCancelled |= RecruitsMaintenanceController.cancelTask(engineer);
                    taskCancelled |= RecruitsConstructionController.cancelTask(engineer);
                }
                if (taskCancelled) {
                    cancelled++;
                }
            }
            sendCommandFeedback(player, cancelled);
            return;
        }

        if (!(player.serverLevel().getEntity(targetEntityId) instanceof AbstractSiegeEntity siege)
                || siege.isRemoved() || player.distanceToSqr(siege) > COMMAND_RANGE_SQR) {
            sendCommandFeedback(player, 0);
            return;
        }

        RecruitsMaintenanceController.Action maintenanceAction =
                action == RecruitsSiegeCommandC2SPayload.ACTION_REPAIR
                        ? RecruitsMaintenanceController.Action.REPAIR
                        : RecruitsMaintenanceController.Action.DISMANTLE;
        MaintenanceRefusal refusal = assignMaintenance(player, siege, maintenanceAction,
                recruit -> isSelectedAndCommandable(player, recruit, selectedGroups));
        switch (refusal) {
            case NONE -> sendCommandFeedback(player, 1);
            case NO_HAND, OCCUPIED, FOREIGN -> player.displayClientMessage(refusal.message(), true);
            default -> sendCommandFeedback(player, 0);
        }
    }

    public enum MaintenanceRefusal {
        NONE(null),
        NOBODY("message.siegeworks.recruits.maintenance_requires_engineer"),
        NO_HAND("message.siegeworks.recruits.maintenance_requires_hammer"),
        OCCUPIED("message.siegeworks.recruits.maintenance_requires_empty"),
        REFUSED("message.siegeworks.recruits.maintenance_refused"),
        FOREIGN("message.siegeworks.access.denied");

        private final String key;

        MaintenanceRefusal(String key) {
            this.key = key;
        }

        public Component message() {
            return key == null ? Component.empty() : Component.translatable(key);
        }
    }

    public static MaintenanceRefusal assignMaintenance(ServerPlayer player, AbstractSiegeEntity siege,
                                                       boolean dismantle,
                                                       java.util.function.Predicate<AbstractRecruitEntity> chosen,
                                                       boolean dryRun) {
        return assignMaintenance(player, siege, dismantle, chosen, dryRun, null);
    }

    public static MaintenanceRefusal assignMaintenance(ServerPlayer player, AbstractSiegeEntity siege,
                                                       boolean dismantle,
                                                       java.util.function.Predicate<AbstractRecruitEntity> chosen,
                                                       boolean dryRun,
                                                       List<AbstractRecruitEntity> nearbyRecruits) {
        RecruitsMaintenanceController.Action action = dismantle
                ? RecruitsMaintenanceController.Action.DISMANTLE
                : RecruitsMaintenanceController.Action.REPAIR;
        if (!SiegeAccess.allows(player, siege, dismantle ? SiegeAccess.Action.DISMANTLE : SiegeAccess.Action.REPAIR)) {
            return MaintenanceRefusal.FOREIGN;
        }
        if (nearbyRecruits == null) {
            nearbyRecruits = commandRangeRecruits(player);
        }

        List<SiegeEngineerEntity> eligibleEngineers = nearbyRecruits.stream()
                .filter(SiegeEngineerEntity.class::isInstance)
                .map(SiegeEngineerEntity.class::cast)
                .filter(chosen::test)
                .filter(engineer -> !RecruitsConstructionController.hasTask(engineer)
                        && !RecruitsMaintenanceController.hasTask(engineer))
                .filter(engineer -> !engineer.isPassenger() || engineer.getVehicle() == siege)
                .sorted(Comparator
                        .comparingInt((SiegeEngineerEntity engineer) -> engineer.getVehicle() == siege ? 0 : 1)
                        .thenComparingDouble(engineer -> engineer.distanceToSqr(siege)))
                .toList();
        if (eligibleEngineers.isEmpty()) return MaintenanceRefusal.NOBODY;

        SiegeEngineerEntity engineer = eligibleEngineers.stream()
                .filter(RecruitsConstructionController::hasConstructionHammer)
                .findFirst()
                .orElse(null);
        if (engineer == null) return MaintenanceRefusal.NO_HAND;

        if (action == RecruitsMaintenanceController.Action.DISMANTLE
                && siege.getPassengers().stream().anyMatch(passenger -> passenger != engineer)) {
            return MaintenanceRefusal.OCCUPIED;
        }

        if (dryRun) return MaintenanceRefusal.NONE;
        return RecruitsMaintenanceController.assign(engineer, siege, action)
                ? MaintenanceRefusal.NONE
                : MaintenanceRefusal.REFUSED;
    }

    private static MaintenanceRefusal assignMaintenance(ServerPlayer player, AbstractSiegeEntity siege,
                                                        RecruitsMaintenanceController.Action action,
                                                        java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        return assignMaintenance(player, siege,
                action == RecruitsMaintenanceController.Action.DISMANTLE, chosen, false);
    }

    private static void handleLadderRelocationCommand(ServerPlayer player,
                                                      List<AbstractRecruitEntity> nearbyRecruits,
                                                      Set<UUID> selectedGroups, int action,
                                                      BlockPos targetPos, int targetEntityId) {
        if (action == RecruitsSiegeCommandC2SPayload.ACTION_PICKUP_LADDER) {
            if (!(player.serverLevel().getEntity(targetEntityId) instanceof SiegeLadderEntity ladder)
                    || player.distanceToSqr(ladder) > COMMAND_RANGE_SQR
                    || !ladder.canBeRelocated()
                    || !ladder.canBeRelocatedBy(player.getUUID())) {
                sendCommandFeedback(player, 0);
                return;
            }

            sendCommandFeedback(player, assignLadderPickup(player, ladder,
                    recruit -> isSelectedAndCommandable(player, recruit, selectedGroups), false) ? 1 : 0);
            return;
        }

        if (!isValidCommandTarget(player, targetPos)
                || !player.serverLevel().mayInteract(player, targetPos)
                || player.serverLevel().getBlockState(targetPos)
                .getCollisionShape(player.serverLevel(), targetPos).isEmpty()
                || !player.serverLevel().getBlockState(targetPos.above()).canBeReplaced()) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.recruits.ladder_invalid_location"), true);
            return;
        }

        sendCommandFeedback(player, assignLadderPlacement(player, targetPos, player.getYRot(),
                recruit -> isSelectedAndCommandable(player, recruit, selectedGroups), false) ? 1 : 0);
    }

    public static boolean assignLadderPlacement(ServerPlayer player, BlockPos targetPos, float yaw,
                                                java.util.function.Predicate<AbstractRecruitEntity> chosen,
                                                boolean dryRun) {
        if (!isValidCommandTarget(player, targetPos)) {
            return false;
        }

        AbstractRecruitEntity carrier = player.serverLevel().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        new AABB(targetPos).inflate(COMMAND_RANGE),
                        recruit -> chosen.test(recruit)
                                && isAvailableForLadderRelocation(recruit)
                                && RecruitsLadderRelocationController.hasLadderItem(recruit))
                .stream()
                .sorted(Comparator
                        .comparingInt((AbstractRecruitEntity recruit) ->
                                recruit instanceof SiegeEngineerEntity ? 1 : 0)
                        .thenComparingDouble(recruit ->
                                recruit.distanceToSqr(Vec3.atCenterOf(targetPos))))
                .findFirst()
                .orElse(null);
        if (carrier == null) {
            return false;
        }
        if (dryRun) {
            return true;
        }

        ItemStack ladderStack = RecruitsLadderRelocationController.getLadderItem(carrier);
        if (!player.serverLevel().mayInteract(player, targetPos)
                || player.serverLevel().getBlockState(targetPos)
                        .getCollisionShape(player.serverLevel(), targetPos).isEmpty()
                || !player.serverLevel().getBlockState(targetPos.above()).canBeReplaced()
                || !player.mayUseItemAt(targetPos, Direction.UP, ladderStack)) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.recruits.ladder_invalid_location"), true);
            return false;
        }
        return RecruitsLadderRelocationController.assignPlacement(carrier, targetPos.immutable(), yaw);
    }

    public static List<AbstractRecruitEntity> ladderCarriers(ServerPlayer player, double radius) {
        return player.serverLevel().getEntitiesOfClass(AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(radius),
                recruit -> commandable(player, recruit)
                        && RecruitsLadderRelocationController.hasLadderItem(recruit));
    }

    public static boolean carriesLadder(AbstractRecruitEntity recruit) {
        return RecruitsLadderRelocationController.hasLadderItem(recruit);
    }

    static boolean isAvailableForLadderRelocation(AbstractRecruitEntity recruit) {
        if (recruit.isPassenger()
                || RecruitsLadderRelocationController.hasTask(recruit)
                || RecruitsSiegeTraversal.hasActiveLadderRoute(recruit)) {
            return false;
        }
        return !(recruit instanceof SiegeEngineerEntity engineer)
                || !RecruitsConstructionController.hasTask(engineer)
                && !RecruitsMaintenanceController.hasTask(engineer);
    }

    public static void handleTowerCrewCommand(ServerPlayer player, int action, int towerEntityId,
                                               List<UUID> groupIds) {
        RecruitsDebug.commandReceived(player, "tower crew", action, 0, null, towerEntityId);
        if (groupIds.isEmpty()
                || !(player.serverLevel().getEntity(towerEntityId) instanceof SiegeTowerEntity tower)
                || player.distanceToSqr(tower) > COMMAND_RANGE_SQR
                || !SiegeAccess.allows(player, tower, SiegeAccess.Action.USE)) {
            sendCommandFeedback(player, 0);
            return;
        }

        Set<UUID> selectedGroups = new HashSet<>(groupIds);
        sendCommandFeedback(player, applyTowerCrew(player, tower, action,
                recruit -> isSelectedAndCommandable(player, recruit, selectedGroups)));
    }

    public static int crewTower(ServerPlayer player, SiegeTowerEntity tower,
                                java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        List<AbstractRecruitEntity> recruits = nearTower(player, tower, chosen);

        boolean driverAssigned = tower.hasActiveDriver();
        Set<UUID> taken = new HashSet<>();
        int placed = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.isPassenger() && recruit.getVehicle() != tower) {
                continue;
            }
            if (recruit instanceof SiegeEngineerEntity engineer && tower.isOperator(engineer)) {
                driverAssigned = true;
                placed++;
                continue;
            }

            if (recruit instanceof SiegeEngineerEntity && !driverAssigned) {
                AbstractHorse lead = freeLeadMount(tower, taken);
                if (lead != null) {
                    taken.add(lead.getUUID());
                    sendToMount(recruit, lead);
                } else if (tower.isTowed()) {
                    continue;
                } else {
                    if (!tower.reserveDriver(recruit)) continue;
                    if (recruit.getVehicle() != tower) RecruitsSiegeTraversal.requestBoarding(recruit, tower);
                }
                driverAssigned = true;
                placed++;
                continue;
            }

            AbstractHorse mount = freeDraftMount(tower, taken);
            if (mount != null) {
                taken.add(mount.getUUID());
                sendToMount(recruit, mount);
                placed++;
                continue;
            }

            boolean placedHere = tower.isTowed()
                    ? tower.reserveInteriorSeat(recruit)
                    : tower.reservePusher(recruit) || tower.reserveInteriorSeat(recruit);
            if (!placedHere) {
                if (tower.isTowed() && recruit.getVehicle() == tower && tower.isPusher(recruit)) {
                    recruit.stopRiding();
                    recruit.dismount = 180;
                }
                continue;
            }
            if (recruit.getVehicle() != tower) {
                RecruitsSiegeTraversal.requestBoarding(recruit, tower);
            }
            placed++;
        }
        return placed;
    }

    public static int applyTowerCrew(ServerPlayer player, SiegeTowerEntity tower, int action,
                                     java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        if (action != RecruitsTowerCrewC2SPayload.ACTION_BOARD
                && action != RecruitsTowerCrewC2SPayload.ACTION_UNLOAD
                && action != RecruitsTowerCrewC2SPayload.ACTION_RETURN) {
            return 0;
        }

        if (action == RecruitsTowerCrewC2SPayload.ACTION_UNLOAD) {
            if (!lowerTowerBridge(player, tower)) {
                return 0;
            }
            return unloadTowerPassengers(player, tower, chosen);
        }

        List<AbstractRecruitEntity> recruits = nearTower(player, tower, chosen);

        if (action == RecruitsTowerCrewC2SPayload.ACTION_RETURN) {
            if (!lowerTowerBridge(player, tower)) {
                return 0;
            }

            int returned = 0;
            boolean driverAssigned = tower.hasActiveDriver();
            for (AbstractRecruitEntity recruit : recruits) {
                if (!tower.getUUID().equals(recruit.getMountUUID())
                        || recruit.isPassenger() && recruit.getVehicle() != tower) {
                    RecruitsDebug.tower(recruit, "skipped by the return command");
                    continue;
                }
                RecruitsDebug.tower(recruit, "accepted by the return command");
                if (recruit.getVehicle() == tower) {
                    returned++;
                    continue;
                }
                if (recruit instanceof SiegeEngineerEntity && !driverAssigned && !tower.isTowed()
                        && tower.reserveDriver(recruit)) {
                    RecruitsSiegeTraversal.requestBoarding(recruit, tower);
                    driverAssigned = true;
                    returned++;
                    continue;
                }
                if (RecruitsSiegeTraversal.requestTowerReturn(recruit, tower)) {
                    returned++;
                }
            }
            return returned;
        }

        boolean driverAssigned = tower.hasActiveDriver();
        int applied = 0;
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.isPassenger() && recruit.getVehicle() != tower) {
                continue;
            }

            if (recruit instanceof SiegeEngineerEntity engineer && tower.isOperator(engineer)) {
                driverAssigned = true;
                applied++;
                continue;
            }

            if (recruit instanceof SiegeEngineerEntity && !driverAssigned && !tower.isTowed()) {
                if (!tower.reserveDriver(recruit)) continue;
                if (recruit.getVehicle() != tower) {
                    RecruitsSiegeTraversal.requestBoarding(recruit, tower);
                }
                driverAssigned = true;
                applied++;
                continue;
            }

            if ((!tower.isTowed() && tower.reservePusher(recruit)) || tower.reserveInteriorSeat(recruit)) {
                if (recruit.getVehicle() != tower) {
                    RecruitsSiegeTraversal.requestBoarding(recruit, tower);
                }
                applied++;
            }
        }

        return applied;
    }

    public static boolean leaveEngine(ServerPlayer player, AbstractRecruitEntity recruit) {
        if (RecruitsSiegeTraversal.cancelBoarding(recruit)
                || RecruitsSiegeTraversal.cancelTowerReturn(recruit)
                || RecruitsLadderRelocationController.cancelTask(recruit)) {
            return true;
        }
        if (recruit instanceof SiegeEngineerEntity engineer
                && (RecruitsMaintenanceController.cancelTask(engineer)
                        || RecruitsConstructionController.cancelTask(engineer))) {
            return true;
        }
        if (recruit.getVehicle() instanceof BatteringRamEntity
                || recruit.getVehicle() instanceof SiegeTowerEntity tower && tower.isPusher(recruit)) {
            recruit.stopRiding();
            recruit.dismount = 180;
            return true;
        }
        if (recruit.getVehicle() instanceof AbstractHorse mount
                && mount.getVehicle() instanceof AbstractSiegeEntity) {
            recruit.stopRiding();
            recruit.shouldMount(false, null);
            recruit.dismount = 180;
            return true;
        }
        if (recruit.getVehicle() instanceof SiegeTowerEntity tower && tower.isInteriorPassenger(recruit)) {
            return RecruitsSiegeTraversal.requestTransportExit(recruit, false);
        }
        if (!recruit.isPassenger() && recruit.getMountUUID() != null
                && player.serverLevel().getEntity(recruit.getMountUUID()) instanceof SiegeTowerEntity tower) {
            tower.cancelBoardingReservation(recruit);
            recruit.shouldMount(false, null);
            recruit.dismount = 180;
            return true;
        }
        if (recruit instanceof SiegeEngineerEntity engineer
                && engineer.getVehicle() instanceof AbstractSiegeEntity siege
                && siege.isOperator(engineer)) {
            engineer.stopRiding();
            engineer.dismount = 180;
            return true;
        }
        return false;
    }

    public static int leaveMachine(ServerPlayer player, AbstractSiegeEntity siege, boolean dryRun) {
        List<AbstractRecruitEntity> candidates = new ArrayList<>();
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof AbstractRecruitEntity recruit && commandable(player, recruit)) {
                candidates.add(recruit);
            }
        }
        for (AbstractRecruitEntity recruit : player.serverLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class, siege.getBoundingBox().inflate(COMMAND_RANGE),
                recruit -> !recruit.isPassenger() && commandable(player, recruit)
                        && (RecruitsSiegeTraversal.isBoardingOn(recruit, siege)
                                || siege.getUUID().equals(recruit.getMountUUID())))) {
            candidates.add(recruit);
        }

        candidates.addAll(draftRiders(player, siege));

        int applied = 0;
        for (AbstractRecruitEntity recruit : candidates) {
            if (dryRun) {
                applied++;
                continue;
            }
            boolean left = leaveEngine(player, recruit);
            RecruitsDebug.tower(recruit, left ? "told to leave the machine" : "had nothing to leave");
            if (left) {
                applied++;
            }
        }
        return applied;
    }

    public static boolean setTowerBridge(ServerPlayer player, SiegeTowerEntity tower, Boolean override,
                                        boolean dryRun) {
        SiegeEngineerEntity engineer = commandedOperator(player, tower);
        if (engineer == null
                || !(engineer.siegeController instanceof SiegeworksRecruitController controller)) {
            return false;
        }
        if (!dryRun) {
            controller.setDeploymentOverride(override);
        }
        return true;
    }

    private static void sendToMount(AbstractRecruitEntity recruit, AbstractHorse mount) {
        if (recruit.getVehicle() == mount) {
            return;
        }
        if (recruit.isPassenger()) {
            recruit.stopRiding();
            recruit.dismount = 180;
        }
        RecruitsSiegeTraversal.cancelBoarding(recruit);
        recruit.setShouldMovePos(false);
        RecruitsWalkOrders.stop(recruit);
        recruit.shouldMount(true, mount.getUUID());
    }

    private static AbstractHorse freeDraftMount(AbstractSiegeEntity siege, Set<UUID> taken) {
        for (AbstractHorse mount : siege.getTowingMounts()) {
            if (mount.getPassengers().isEmpty() && !taken.contains(mount.getUUID())) {
                return mount;
            }
        }
        return null;
    }

    private static AbstractHorse freeLeadMount(AbstractSiegeEntity siege, Set<UUID> taken) {
        AbstractHorse lead = siege.getTowingMount();
        return lead != null && lead.getPassengers().isEmpty() && !taken.contains(lead.getUUID())
                ? lead
                : null;
    }

    private static List<AbstractRecruitEntity> nearTower(ServerPlayer player, SiegeTowerEntity tower,
                                                        java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        List<AbstractRecruitEntity> recruits = player.serverLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class, tower.getBoundingBox().inflate(COMMAND_RANGE),
                chosen::test);
        recruits.sort(Comparator
                .comparingInt((AbstractRecruitEntity recruit) -> recruit instanceof SiegeEngineerEntity ? 0 : 1)
                .thenComparingDouble(recruit -> recruit.distanceToSqr(tower)));
        return recruits;
    }

    public static int aboard(ServerPlayer player, SiegeTowerEntity tower, boolean interiorOnly) {
        return aboard(player, tower, interiorOnly, recruit -> commandable(player, recruit));
    }

    static int aboard(ServerPlayer player, SiegeTowerEntity tower, boolean interiorOnly,
                      java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        int count = 0;
        for (Entity passenger : tower.getPassengers()) {
            if (passenger instanceof AbstractRecruitEntity recruit && chosen.test(recruit)
                    && (!interiorOnly || tower.isInteriorPassenger(recruit))) {
                count++;
            }
        }
        return count;
    }

    public static int aboardAny(ServerPlayer player, AbstractSiegeEntity siege) {
        int count = 0;
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof AbstractRecruitEntity recruit && commandable(player, recruit)) {
                count++;
            }
        }
        return count + draftRiders(player, siege).size();
    }

    private static List<AbstractRecruitEntity> draftRiders(ServerPlayer player, AbstractSiegeEntity siege) {
        List<AbstractRecruitEntity> riders = new ArrayList<>();
        for (Entity passenger : siege.getPassengers()) {
            if (!(passenger instanceof AbstractHorse mount)) {
                continue;
            }
            for (Entity rider : mount.getPassengers()) {
                if (rider instanceof AbstractRecruitEntity recruit && commandable(player, recruit)) {
                    riders.add(recruit);
                }
            }
        }
        return riders;
    }

    public static int awayFromTower(ServerPlayer player, SiegeTowerEntity tower) {
        return awayFromTower(player, tower, recruit -> commandable(player, recruit));
    }

    static int awayFromTower(ServerPlayer player, SiegeTowerEntity tower,
                             java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        int count = 0;
        for (AbstractRecruitEntity recruit : nearTower(player, tower, chosen)) {
            if (recruit.getVehicle() != tower && tower.getUUID().equals(recruit.getMountUUID())) {
                count++;
            }
        }
        return count;
    }

    public static boolean driveMachine(ServerPlayer player, AbstractSiegeEntity siege, BlockPos target,
                                       boolean dryRun) {
        SiegeEngineerEntity operator = commandedOperator(player, siege);
        if (operator == null && freeEngineerFor(player, siege) == null) {
            return false;
        }
        if (dryRun) {
            return true;
        }

        RecruitsDriveOrders.replace(siege, target, player);
        if (operator != null) {
            operator.setShouldMovePos(false);
            return true;
        }
        return board(player, siege);
    }

    public static boolean queueDrive(ServerPlayer player, AbstractSiegeEntity siege, BlockPos target,
                                     boolean dryRun) {
        SiegeEngineerEntity operator = commandedOperator(player, siege);
        if (operator == null && freeEngineerFor(player, siege) == null) {
            return false;
        }
        if (dryRun) {
            return true;
        }

        RecruitsDriveOrders.append(siege, target, player);
        return operator != null || board(player, siege);
    }

    private static boolean board(ServerPlayer player, AbstractSiegeEntity siege) {
        SiegeEngineerEntity driver = freeEngineerFor(player, siege);
        if (driver == null) {
            RecruitsDriveOrders.forget(siege);
            return false;
        }
        RecruitsSiegeTraversal.requestBoarding(driver, siege);
        return true;
    }

    static SiegeEngineerEntity operatorOf(AbstractSiegeEntity siege) {
        if (siege.getReinsHolder() instanceof SiegeEngineerEntity driver) {
            return driver;
        }
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof SiegeEngineerEntity engineer && siege.isOperator(engineer)) {
                return engineer;
            }
        }
        return null;
    }

    static SiegeEngineerEntity commandedOperator(ServerPlayer player, AbstractSiegeEntity siege) {
        SiegeEngineerEntity engineer = operatorOf(siege);
        return engineer != null && commandable(player, engineer) ? engineer : null;
    }

    static void sendWhereToGo(SiegeEngineerEntity engineer, BlockPos target) {
        if (engineer.siegeController instanceof SiegeworksRecruitController controller) {
            controller.setDeploymentOverride(null);
        }
        engineer.setFollowState(0);
        engineer.setMovePos(target.immutable());
        engineer.setShouldMovePos(true);
    }

    private static SiegeEngineerEntity freeEngineerFor(ServerPlayer player, AbstractSiegeEntity siege) {
        return player.serverLevel().getEntitiesOfClass(SiegeEngineerEntity.class,
                        siege.getBoundingBox().inflate(COMMAND_RANGE),
                        engineer -> !engineer.isPassenger()
                                && commandable(player, engineer)
                                && !RecruitsMaintenanceController.hasTask(engineer)
                                && !RecruitsConstructionController.hasTask(engineer))
                .stream()
                .min(Comparator.comparingDouble(engineer -> engineer.distanceToSqr(siege)))
                .orElse(null);
    }

    public static List<BlockPos> routeOf(AbstractSiegeEntity siege, UUID commander) {
        List<BlockPos> march = RecruitsDriveOrders.route(siege, commander);
        if (!march.isEmpty()) {
            return march;
        }

        SiegeEngineerEntity engineer = operatorOf(siege);
        return engineer != null && engineer.isEffectedByCommand(commander)
                && engineer.getShouldMovePos() && engineer.getMovePos() != null
                ? List.of(engineer.getMovePos())
                : List.of();
    }

    public static boolean assignLadderPickup(ServerPlayer player, SiegeLadderEntity ladder,
                                             java.util.function.Predicate<AbstractRecruitEntity> chosen,
                                             boolean dryRun) {
        if (!ladder.canBeRelocated() || !ladder.canBeRelocatedBy(player.getUUID())) {
            return false;
        }

        AbstractRecruitEntity carrier = player.serverLevel().getEntitiesOfClass(
                        AbstractRecruitEntity.class, ladder.getBoundingBox().inflate(COMMAND_RANGE),
                        recruit -> chosen.test(recruit)
                                && isAvailableForLadderRelocation(recruit)
                                && RecruitsLadderRelocationController.canCarry(recruit, ladder))
                .stream()
                .sorted(Comparator
                        .comparingInt((AbstractRecruitEntity recruit) ->
                                recruit instanceof SiegeEngineerEntity ? 1 : 0)
                        .thenComparingDouble(recruit -> recruit.distanceToSqr(ladder)))
                .findFirst()
                .orElse(null);
        if (carrier == null) {
            return false;
        }
        return dryRun || RecruitsLadderRelocationController.assignPickup(carrier, ladder);
    }

    public static boolean drivable(AbstractSiegeEntity siege) {
        return me.mss1r.siegeworks.config.SiegeworksServerConfig
                .getMovementSpeed(siege.getType(), false) > 0.0D;
    }

    public static int crewMachine(ServerPlayer player, AbstractSiegeEntity siege,
                                  java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        if (!SiegeAccess.allows(player, siege, SiegeAccess.Action.USE) && siege.captureRefusal(player) != null) {
            return 0;
        }
        if (siege instanceof SiegeTowerEntity tower) {
            return crewTower(player, tower, chosen);
        }

        List<AbstractRecruitEntity> recruits = player.serverLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class, siege.getBoundingBox().inflate(COMMAND_RANGE),
                chosen::test);
        recruits.sort(Comparator
                .comparingInt((AbstractRecruitEntity recruit) -> recruit instanceof SiegeEngineerEntity ? 0 : 1)
                .thenComparingDouble(recruit -> recruit.distanceToSqr(siege)));

        Set<UUID> taken = new HashSet<>();
        int sent = 0;
        int expected = siege.getPassengers().size();
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.getVehicle() == siege) {
                sent++;
                continue;
            }

            if (siege.isTowed()) {
                AbstractHorse mount = recruit instanceof SiegeEngineerEntity
                        ? firstNonNull(freeLeadMount(siege, taken), freeDraftMount(siege, taken))
                        : freeDraftMount(siege, taken);
                if (mount == null) {
                    continue;
                }
                taken.add(mount.getUUID());
                sendToMount(recruit, mount);
                sent++;
                continue;
            }

            if (recruit.isPassenger() || !siege.canAddPassenger(recruit)
                    || expected >= siege.crewCapacity()) {
                continue;
            }
            if (RecruitsSiegeTraversal.requestBoarding(recruit, siege)) {
                expected++;
                sent++;
            }
        }
        return sent;
    }

    static AbstractSiegeEntity workedMachine(SiegeEngineerEntity engineer) {
        if (engineer.getVehicle() instanceof AbstractSiegeEntity direct) {
            return direct;
        }
        return engineer.getVehicle() != null
                && engineer.getVehicle().getVehicle() instanceof AbstractSiegeEntity towed
                ? towed
                : null;
    }

    public static boolean takesCrew(AbstractSiegeEntity siege) {
        return siege.crewCapacity() > 0;
    }

    private static AbstractHorse firstNonNull(AbstractHorse first, AbstractHorse second) {
        return first != null ? first : second;
    }

    public static boolean setMantletFlap(ServerPlayer player, AbstractSiegeEntity siege, boolean open,
                                         boolean dryRun) {
        if (!(siege instanceof MantletEntity mantlet) || !commands(player, siege)
                || commandedOperator(player, siege) == null) {
            return false;
        }
        if (!dryRun) {
            mantlet.orderFlap(open);
        }
        return true;
    }

    public static boolean commands(ServerPlayer player, AbstractSiegeEntity siege) {
        return SiegeAccess.allows(player, siege, SiegeAccess.Action.USE);
    }

    private static boolean alliedFactions(String firstTeam, String secondTeam) {
        RecruitsDiplomacyManager diplomacy = FactionEvents.recruitsDiplomacyManager;
        return diplomacy != null
                && diplomacy.getRelation(firstTeam, secondTeam) == RecruitsDiplomacyManager.DiplomacyStatus.ALLY;
    }

    public static UUID commanderOf(AbstractSiegeEntity siege) {
        return siege.getOwnerUuid();
    }

    public static boolean withinCommandRange(ServerPlayer player, AbstractSiegeEntity siege) {
        return player.distanceToSqr(siege) <= COMMAND_RANGE_SQR;
    }

    public static SiegeAmmunitionMode ammunitionSetting(AbstractSiegeEntity siege) {
        return siege instanceof SiegeAmmunitionControl ammunition
                ? ammunition.getAutomatedAmmunitionMode()
                : null;
    }

    public static Boolean firesAtWill(ServerPlayer player, AbstractSiegeEntity siege) {
        SiegeEngineerEntity engineer = commandedOperator(player, siege);
        return engineer == null ? null : engineer.getShouldRanged();
    }

    public static Boolean towerBridgeSetting(ServerPlayer player, SiegeTowerEntity tower) {
        SiegeEngineerEntity engineer = commandedOperator(player, tower);
        return engineer != null
                && engineer.siegeController instanceof SiegeworksRecruitController controller
                ? controller.getDeploymentOverride()
                : null;
    }

    public static boolean flapOpen(AbstractSiegeEntity siege) {
        return siege instanceof MantletEntity mantlet && mantlet.isFlapOpen();
    }

    public static boolean haltMachine(ServerPlayer player, AbstractSiegeEntity siege, boolean dryRun) {
        boolean ownMarch = RecruitsDriveOrders.any(siege, player.getUUID());
        boolean anything = ownMarch;
        SiegeEngineerEntity driver = commandedOperator(player, siege);
        if (driver != null) {
            anything = true;
            if (!dryRun) {
                driver.setShouldMovePos(false);
                driver.clearMovePos();
                applyFireCommand(driver, siege, RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE, null);
            }
        }
        if (!dryRun && ownMarch) {
            RecruitsDriveOrders.forget(siege, player.getUUID());
        }
        return anything;
    }

    public static boolean shoots(AbstractSiegeEntity siege) {
        return siege instanceof SiegeArtilleryControl;
    }

    public static boolean rams(AbstractSiegeEntity siege) {
        return siege instanceof SiegeMeleeControl;
    }

    public static boolean choosesAmmunition(AbstractSiegeEntity siege, SiegeAmmunitionMode mode) {
        return siege instanceof SiegeAmmunitionControl ammunition
                && ammunition.supportsAmmunitionMode(mode);
    }

    public static boolean applyFireOrder(ServerPlayer player, AbstractSiegeEntity siege, int action,
                                         BlockPos target, boolean dryRun) {
        SiegeEngineerEntity engineer = commandedOperator(player, siege);
        if (engineer == null) {
            return false;
        }
        return dryRun || applyFireCommand(engineer, siege, action, target);
    }

    public static boolean applyAmmunition(ServerPlayer player, AbstractSiegeEntity siege,
                                          SiegeAmmunitionMode mode, boolean dryRun) {
        if (!(siege instanceof SiegeAmmunitionControl ammunition)
                || !ammunition.supportsAmmunitionMode(mode)) {
            return false;
        }
        if (commandedOperator(player, siege) == null) {
            return false;
        }
        if (!dryRun) {
            ammunition.setAutomatedAmmunitionMode(mode);
        }
        return true;
    }

    public static int cancelWorkOn(ServerPlayer player, AbstractSiegeEntity siege, boolean dryRun) {
        int cancelled = 0;
        for (SiegeEngineerEntity engineer : player.serverLevel().getEntitiesOfClass(
                SiegeEngineerEntity.class, siege.getBoundingBox().inflate(COMMAND_RANGE),
                candidate -> commandable(player, candidate)
                        && RecruitsMaintenanceController.worksOn(candidate, siege))) {
            if (dryRun || RecruitsMaintenanceController.cancelTask(engineer)) {
                cancelled++;
            }
        }
        return cancelled;
    }

    public static boolean requestTransportExit(LivingEntity passenger) {
        return passenger instanceof AbstractRecruitEntity recruit
                && RecruitsSiegeTraversal.requestTransportExit(recruit);
    }

    private static void handleBuildCommand(ServerPlayer player, List<AbstractRecruitEntity> nearbyRecruits,
                                           Set<UUID> selectedGroups, BlockPos targetPos) {
        if (!isValidCommandTarget(player, targetPos)) {
            player.displayClientMessage(Component.translatable("message.siegeworks.recruits.build_invalid_location"), true);
            return;
        }

        ItemStack blueprint = player.getOffhandItem();
        String recipeId = BlueprintStacks.definitionId(blueprint);
        BlueprintDefinition recipe = recipeId == null ? null : BlueprintDefinitions.get(recipeId);
        ItemStack result = BlueprintStacks.createResult(recipe);
        ResourceLocation resultId = result.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(result.getItem());
        if (recipe == null || !recipe.buildsInWorld() || resultId == null
                || !Siegeworks.MOD_ID.equals(resultId.getNamespace())) {
            player.displayClientMessage(Component.translatable("message.siegeworks.recruits.build_requires_blueprint"), true);
            return;
        }
        if (!BlueprintPermissions.canUse(player, recipe, recipeId)
                || !player.serverLevel().mayInteract(player, targetPos)
                || !player.mayUseItemAt(targetPos, Direction.UP, blueprint)) {
            player.displayClientMessage(Component.translatable("gui.axiomata.no_permission"), true);
            return;
        }

        List<SiegeEngineerEntity> engineers = nearbyRecruits.stream()
                .filter(SiegeEngineerEntity.class::isInstance)
                .map(SiegeEngineerEntity.class::cast)
                .filter(engineer -> isSelectedAndCommandable(player, engineer, selectedGroups))
                .filter(engineer -> !engineer.isPassenger() && !RecruitsConstructionController.hasTask(engineer))
                .sorted(Comparator.comparingDouble(engineer -> engineer.distanceToSqr(Vec3.atCenterOf(targetPos))))
                .toList();
        if (engineers.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.recruits.build_requires_engineer"), true);
            return;
        }

        SiegeEngineerEntity engineer = engineers.stream()
                .filter(RecruitsConstructionController::hasConstructionHammer)
                .findFirst()
                .orElse(null);
        if (engineer == null) {
            player.displayClientMessage(Component.translatable("message.siegeworks.recruits.build_requires_hammer"), true);
            return;
        }

        Vec3 hitLocation = Vec3.atCenterOf(targetPos).add(0.0D, 0.5D, 0.0D);
        net.minecraft.world.entity.Entity site = ConstructionDeployer.deploy(
                player.serverLevel(), recipeId, blueprint, result,
                targetPos, Direction.UP, hitLocation, player.getYRot());
        if (site == null) {
            player.displayClientMessage(Component.translatable("message.siegeworks.recruits.build_invalid_location"), true);
            return;
        }

        if (!player.getAbilities().instabuild) {
            blueprint.shrink(1);
        }
        if (site instanceof AbstractSiegeEntity siege) {
            siege.claimOwnership(player.getUUID());
        }
        RecruitsConstructionController.assign(engineer, site);
        player.displayClientMessage(
                Component.translatable("message.siegeworks.recruits.build_started", result.getHoverName()),
                false
        );
    }

    /** Sends the selected groups' recruits back into their towers. */
    private static int handleTowerReturn(ServerPlayer player, List<AbstractRecruitEntity> nearbyRecruits,
                                         Set<UUID> selectedGroups) {
        Set<SiegeTowerEntity> towers = new HashSet<>();
        for (AbstractRecruitEntity recruit : nearbyRecruits) {
            if (isSelectedAndCommandable(player, recruit, selectedGroups) && recruit.getVehicle() == null
                    && recruit.getMountUUID() != null
                    && player.serverLevel().getEntity(recruit.getMountUUID()) instanceof SiegeTowerEntity tower) {
                towers.add(tower);
            }
        }

        int applied = 0;
        for (SiegeTowerEntity tower : towers) {
            applied += applyTowerCrew(player, tower, RecruitsTowerCrewC2SPayload.ACTION_RETURN,
                    recruit -> isSelectedAndCommandable(player, recruit, selectedGroups));
        }
        return applied;
    }

    private static int handleTowerUnload(ServerPlayer player, List<AbstractRecruitEntity> nearbyRecruits,
                                         Set<UUID> selectedGroups) {
        Set<SiegeTowerEntity> selectedTowers = new HashSet<>();
        for (AbstractRecruitEntity recruit : nearbyRecruits) {
            if (isSelectedAndCommandable(player, recruit, selectedGroups)
                    && recruit.getVehicle() instanceof SiegeTowerEntity tower
                    && (tower.isInteriorPassenger(recruit)
                            || recruit instanceof SiegeEngineerEntity && tower.isOperator(recruit))) {
                selectedTowers.add(tower);
            }
        }

        int applied = 0;
        for (SiegeTowerEntity tower : selectedTowers) {
            applied += applyTowerCrew(player, tower,
                    RecruitsTowerCrewC2SPayload.ACTION_UNLOAD,
                    recruit -> isCommandable(player, recruit));
        }
        return applied;
    }

    private static int unloadTowerPassengers(ServerPlayer player, SiegeTowerEntity tower,
                                             java.util.function.Predicate<AbstractRecruitEntity> chosen) {
        List<AbstractRecruitEntity> passengers = tower.getPassengers().stream()
                .filter(AbstractRecruitEntity.class::isInstance)
                .map(AbstractRecruitEntity.class::cast)
                .filter(tower::isInteriorPassenger)
                .filter(chosen::test)
                .toList();
        if (passengers.isEmpty()) {
            return 0;
        }

        int applied = 0;
        for (AbstractRecruitEntity passenger : passengers) {
            if (RecruitsSiegeTraversal.requestTransportExit(passenger, true)) {
                applied++;
            }
        }
        if (applied > 0) {
            player.displayClientMessage(
                    Component.translatable("message.siegeworks.recruits.tower_unload_bridge"), true);
        }
        return applied;
    }

    /**
     * Whether a tower's bridge can be lowered for its crew: by the player or their driver at the levers, or it is
     * already resting on something walkable.
     */
    static boolean bridgeWithinReach(ServerPlayer player, SiegeTowerEntity tower) {
        SiegeEngineerEntity engineer = operatorOf(tower);
        return tower.isOperator(player)
                || engineer != null && engineer.isEffectedByCommand(player.getUUID())
                || tower.isBridgeOpen() && tower.bridgeLeadsSomewhere();
    }

    private static boolean lowerTowerBridge(ServerPlayer player, SiegeTowerEntity tower) {
        if (tower.isOperator(player)) {
            if (!tower.isDeployed()) {
                tower.setDeployed(player, true);
            }
            return tower.isDeployed();
        }
        SiegeEngineerEntity engineer = operatorOf(tower);
        if (engineer == null || !engineer.isEffectedByCommand(player.getUUID())) {
            return tower.isBridgeOpen() && tower.bridgeLeadsSomewhere();
        }
        if (engineer.siegeController instanceof SiegeworksRecruitController controller) {
            controller.requestTemporaryDeployment();
        } else if (!tower.isDeployed()) {
            tower.setDeployed(engineer, true);
        }
        return tower.isDeployed();
    }

    private static boolean applyFireCommand(SiegeEngineerEntity engineer, AbstractSiegeEntity siege,
                                            int action, BlockPos targetPos) {
        switch (action) {
            case RecruitsSiegeCommandC2SPayload.ACTION_FIRE_POSITION -> {
                if (targetPos == null) {
                    return false;
                }
                RecruitsFireZone.clear(engineer);
                engineer.setStrategicFirePos(targetPos.immutable());
                engineer.setShouldStrategicFire(true);
                engineer.setShouldRanged(true);
            }
            case RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE -> {
                engineer.setShouldStrategicFire(false);
                engineer.setShouldRanged(false);
                engineer.setTarget(null);
                engineer.setFollowState(2);
                siege.cancelPrimaryAction(engineer);
                siege.clearOperatorInput(engineer);
            }
            case RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL -> {
                RecruitsFireZone.clear(engineer);
                engineer.setShouldStrategicFire(false);
                engineer.setShouldRanged(true);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    static boolean isSelectedAndCommandable(ServerPlayer player, AbstractRecruitEntity recruit,
                                                     Set<UUID> selectedGroups) {
        return recruit.getGroup() != null
                && selectedGroups.contains(recruit.getGroup())
                && isCommandable(player, recruit);
    }

    static boolean isCommandable(ServerPlayer player, AbstractRecruitEntity recruit) {
        return recruit.getGroup() != null
                && recruit.isEffectedByCommand(player.getUUID(), recruit.getGroup());
    }

    static boolean isValidCommandTarget(ServerPlayer player, BlockPos targetPos) {
        return targetPos != null
                && player.distanceToSqr(Vec3.atCenterOf(targetPos)) <= COMMAND_RANGE_SQR
                && player.serverLevel().hasChunkAt(targetPos);
    }

    static boolean mayOpenInClaim(ServerPlayer player, BlockPos pos) {
        return player.isCreative() && player.hasPermissions(2) || claimOpensContainerTo(pos, player.getTeam());
    }

    /** Mirrors Recruits' own container rule for claimed chunks. */
    static boolean claimOpensContainerTo(BlockPos pos, @Nullable Team team) {
        if (ClaimEvents.recruitsClaimManager == null) {
            return true;
        }
        RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(new ChunkPos(pos));
        return claim == null || claim.isBlockInteractionAllowed()
                || team != null && team.getName().equals(claim.getOwnerFactionStringID());
    }

    static boolean isContainer(ServerPlayer player, BlockPos targetPos) {
        BlockState state = player.serverLevel().getBlockState(targetPos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, player.serverLevel(), targetPos, false) != null;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(targetPos);
        return blockEntity instanceof Container;
    }

    private static void sendCommandFeedback(ServerPlayer player, int applied) {
        Component feedback = applied > 0
                ? Component.translatable("message.siegeworks.recruits.command_applied", applied)
                : Component.translatable("message.siegeworks.recruits.no_siege_engines");
        player.displayClientMessage(feedback, true);
    }

    private static SiegeAmmunitionMode ammunitionModeForAction(int action) {
        return switch (action) {
            case RecruitsSiegeCommandC2SPayload.ACTION_AMMO_AUTO -> SiegeAmmunitionMode.AUTO;
            case RecruitsSiegeCommandC2SPayload.ACTION_AMMO_STANDARD -> SiegeAmmunitionMode.STANDARD;
            case RecruitsSiegeCommandC2SPayload.ACTION_AMMO_EXPLOSIVE -> SiegeAmmunitionMode.EXPLOSIVE;
            case RecruitsSiegeCommandC2SPayload.ACTION_AMMO_INCENDIARY -> SiegeAmmunitionMode.INCENDIARY;
            default -> null;
        };
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        Main.isSiegeWeaponsLoaded = true;
        Main.isSiegeWeaponsCompatible = true;
    }

    private static void onServerStarting(ServerStartingEvent event) {
        RecruitsDriveOrders.clear();
        RecruitsSiegeTraversal.clear();
        RecruitsWalkOrders.clear();
        RecruitsDebug.clear();
        List<String> mountWhitelist = new ArrayList<>(RecruitsServerConfig.MountWhiteList.get());
        addMount(mountWhitelist, SiegeworksEntities.TOWER_CROSSBOW_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.ARCBALLISTA_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.SERPENTINE_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.CULVERIN_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.MONS_MEG_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.MANGONEL_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.TREBUCHET_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.MANTLET_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.BATTERING_RAM_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.SIEGE_TOWER_ENTITY.getId());
        addMount(mountWhitelist, SiegeworksEntities.HWACHA_ENTITY.getId());
        RecruitsServerConfig.MountWhiteList.set(mountWhitelist);
    }

    private static void addMount(List<String> mountWhitelist, ResourceLocation entityTypeId) {
        String id = entityTypeId.toString();
        if (!mountWhitelist.contains(id)) {
            mountWhitelist.add(id);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit) || recruit.level().isClientSide()) {
            return;
        }

        if (RecruitsLadderRelocationController.tick(recruit)) {
            RecruitsDebug.claimed(recruit, "ladder relocation");
            return;
        }
        RecruitsSiegeTraversal.tick(recruit);
        String routing = RecruitsSiegeTraversal.routeDescription(recruit);

        if (!(recruit instanceof SiegeEngineerEntity engineer)) {
            RecruitsDebug.claimed(recruit, routing);
            return;
        }

        if (RecruitsDriveOrders.tick(engineer)) {
            RecruitsDebug.claimed(engineer, "driving a machine along its march");
        }

        if (RecruitsConstructionController.tick(engineer)) {
            RecruitsDebug.claimed(recruit, "construction");
            return;
        }
        if (RecruitsMaintenanceController.tick(engineer)) {
            RecruitsDebug.claimed(recruit, "maintenance");
            return;
        }
        RecruitsDebug.claimed(recruit, engineer.getVehicle() instanceof AbstractSiegeEntity
                ? "crewing an engine" : routing);

        AbstractSiegeEntity worked = workedMachine(engineer);
        if (worked != null && worked.isOperator(engineer)) {
            if (!(engineer.siegeController instanceof SiegeworksRecruitController controller)
                    || !controller.controls(worked)) {
                engineer.siegeController = new SiegeworksRecruitController(engineer, worked);
            }
        } else if (engineer.siegeController instanceof SiegeworksRecruitController controller) {
            controller.reset();
            engineer.siegeController = null;
        }
    }
}
