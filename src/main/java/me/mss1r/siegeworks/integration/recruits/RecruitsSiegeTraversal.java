package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.pathfinding.AsyncPath;
import me.mss1r.siegeworks.api.SiegeClimbableControl;
import me.mss1r.siegeworks.api.SiegeTransportControl;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

final class RecruitsSiegeTraversal {
    private static final double MIN_ELEVATION_CHANGE = 1.25D;
    private static final double LADDER_SEARCH_RANGE = 32.0D;
    private static final double ROUTE_ENDPOINT_PROGRESS_MARGIN = 0.15D;
    private static final double DESTINATION_CHANGE_DISTANCE_SQR = 8.0D * 8.0D;
    private static final int FAILED_ROUTE_RETRY_TICKS = 40;
    private static final int COMPLETED_ROUTE_RETRY_TICKS = 600;
    /** How long a recruit may walk toward a ladder without getting any closer before giving up. */
    private static final int APPROACH_TIMEOUT_TICKS = 240;
    /** A recruit joins a ladder's line within this distance of its entrance; the ladder stages it from there. */
    private static final double JOIN_LINE_DISTANCE_SQR = 8.0D * 8.0D;
    /** How long a recruit waits in a line that does not move before giving up. */
    private static final int STALLED_LINE_TICKS = 400;
    /** How long a walk found to the destination is trusted before it is looked for again. */
    private static final int WALK_RECHECK_TICKS = 100;
    /** A walking path that ends this close to the destination reaches it. */
    private static final double WALK_ARRIVAL_DISTANCE_SQR = 2.0D * 2.0D;
    /** A walk still being looked for after this long counts as none. */
    private static final int WALK_SEARCH_TICKS = 60;
    private static final int ROUTE_HANDOFF_TICKS = 40;
    private static final double ROUTE_HANDOFF_DISTANCE_SQR = 2.5D * 2.5D;
    private static final int TOWER_RETURN_TIMEOUT_TICKS = 1200;
    private static final int BOARD_TIMEOUT_TICKS = 1200;
    private static final double BOARD_ARRIVAL_DISTANCE_SQR = 2.75D * 2.75D;
    private static final double BOARD_REACH_SQR = 6.0D * 6.0D;
    private static final double BOARD_LAST_RESORT_SQR = 16.0D * 16.0D;
    private static final int BOARD_STUCK_TICKS = 60;
    private static final int GROUND_EXIT_PATIENCE_TICKS = 100;
    private static final double TOWER_RETURN_ENTRY_REACH_SQR = 0.75D * 0.75D;
    private static final double TOWER_RETURN_UPPER_LEVEL_OFFSET = 3.0D;

    private static final Map<AbstractRecruitEntity, LadderRoute> LADDER_ROUTES = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, Integer> RETRY_AFTER_TICK = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, CompletedLadderRoute> COMPLETED_ROUTES = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, ExitRequest> TRANSPORT_EXIT_REQUESTS = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, TransportExitRoute> TRANSPORT_EXIT_ROUTES = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, RouteHandoff> ROUTE_HANDOFFS = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, TowerReturnRoute> TOWER_RETURN_ROUTES = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, BoardRoute> BOARD_ROUTES = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, WalkSearch> WALK_SEARCHES = new WeakHashMap<>();

    private RecruitsSiegeTraversal() {
    }

    static void clear() {
        LADDER_ROUTES.clear();
        RETRY_AFTER_TICK.clear();
        COMPLETED_ROUTES.clear();
        TRANSPORT_EXIT_REQUESTS.clear();
        TRANSPORT_EXIT_ROUTES.clear();
        ROUTE_HANDOFFS.clear();
        TOWER_RETURN_ROUTES.clear();
        BOARD_ROUTES.clear();
        WALK_SEARCHES.clear();
    }

    static boolean hasTowerTraffic(SiegeTowerEntity tower) {
        UUID towerUuid = tower.getUUID();
        return TRANSPORT_EXIT_REQUESTS.values().stream()
                        .anyMatch(request -> towerUuid.equals(request.transportUuid()))
                || TRANSPORT_EXIT_ROUTES.values().stream()
                        .anyMatch(route -> towerUuid.equals(route.transportUuid()))
                || TOWER_RETURN_ROUTES.values().stream()
                        .anyMatch(route -> towerUuid.equals(route.towerUuid));
    }

    static String routeDescription(AbstractRecruitEntity recruit) {
        if (!RecruitsDebug.enabled()) {
            return null;
        }
        if (BOARD_ROUTES.containsKey(recruit)) {
            return "walking round to board a machine";
        }
        if (TOWER_RETURN_ROUTES.containsKey(recruit)) {
            return "returning to a tower";
        }
        if (TRANSPORT_EXIT_ROUTES.containsKey(recruit)) {
            return "leaving a transport";
        }
        if (TRANSPORT_EXIT_REQUESTS.containsKey(recruit)) {
            return "waiting to leave a transport";
        }
        if (ROUTE_HANDOFFS.containsKey(recruit)) {
            return "handing a route back";
        }
        LadderRoute route = LADDER_ROUTES.get(recruit);
        if (route != null) {
            return route.suspendedMoveOrder
                    ? "climbing a ladder, move order suspended"
                    : "climbing a ladder";
        }
        if (recruit.isPassenger()) {
            return "riding";
        }
        return null;
    }

    static void tick(AbstractRecruitEntity recruit) {
        if (!(recruit.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (tickBoardRoute(recruit, serverLevel)) {
            return;
        }

        if (tickTowerReturnRoute(recruit, serverLevel)) {
            return;
        }

        if (tickTransportExitRoute(recruit, serverLevel)) {
            return;
        }

        if (recruit.getVehicle() instanceof SiegeTransportControl transport) {
            cancelLadderRoute(recruit, serverLevel);
            ExitRequest request = TRANSPORT_EXIT_REQUESTS.get(recruit);
            if (request == null) {
                return;
            }
            if (!recruit.getVehicle().getUUID().equals(request.transportUuid())) {
                TRANSPORT_EXIT_REQUESTS.remove(recruit);
                return;
            }
            Vec3 destination = getDestination(recruit);
            UUID transportUuid = recruit.getVehicle().getUUID();

            if (request.overBridge()
                    && transport.preparePassengerForAutomatedExit(recruit)
                    && transport.canPassengerExit(recruit)
                    && transport.disembarkPassenger(recruit)) {
                TRANSPORT_EXIT_REQUESTS.remove(recruit);
                recruit.shouldMount(false, transportUuid);
                TRANSPORT_EXIT_ROUTES.put(recruit, new TransportExitRoute(transportUuid, destination));
                return;
            }
            if (request.overBridge() && recruit.getVehicle() instanceof SiegeTowerEntity tower
                    && (tower.bridgeMoving() || tower.bridgeLeadsSomewhere())) {
                return;
            }
            if (request.overBridge()) {
                // When sent out over the bridge, it stays aboard instead of dropping at the tower's base.
                if (recruit.tickCount - request.createdTick() >= GROUND_EXIT_PATIENCE_TICKS) {
                    TRANSPORT_EXIT_REQUESTS.remove(recruit);
                    RecruitsDebug.tower(recruit, "found no way across the bridge and stays aboard");
                    if (recruit.getOwner() instanceof ServerPlayer owner) {
                        owner.displayClientMessage(Component.translatable(
                                "message.siegeworks.recruits.tower_unload_no_surface"), true);
                    }
                }
                return;
            }
            boolean waitedLongEnough = recruit.tickCount - request.createdTick() >= GROUND_EXIT_PATIENCE_TICKS;
            if (transport.disembarkPassengerToGround(recruit, waitedLongEnough)) {
                TRANSPORT_EXIT_REQUESTS.remove(recruit);
                recruit.shouldMount(false, transportUuid);
            }
            return;
        }

        TRANSPORT_EXIT_REQUESTS.remove(recruit);
        if (recruit.isPassenger()) {
            cancelLadderRoute(recruit, serverLevel);
            return;
        }

        tickLadderRoute(recruit, serverLevel);
    }

    private static void tickLadderRoute(AbstractRecruitEntity recruit, ServerLevel serverLevel) {
        if (tickRouteHandoff(recruit)) {
            return;
        }

        LadderRoute route = LADDER_ROUTES.get(recruit);
        if (route != null) {
            boolean receivedNewMoveOrder = route.suspendedMoveOrder && recruit.getShouldMovePos();
            Vec3 currentDestination = getRouteDestination(recruit, route);
            if (receivedNewMoveOrder) {
                // A repeated formation/point command is not a cancellation of this passage.
                // Only a materially different destination needs a new route while on the ground.
                if (currentDestination == null || currentDestination.distanceToSqr(route.destination)
                        > DESTINATION_CHANGE_DISTANCE_SQR && (!route.climbing || route.waiting)) {
                    cancelLadderRoute(recruit, serverLevel);
                    RETRY_AFTER_TICK.put(recruit, recruit.tickCount + FAILED_ROUTE_RETRY_TICKS);
                    return;
                }
                route.destination = currentDestination;
                recruit.setShouldMovePos(false);
            }
            if (currentDestination != null
                    && currentDestination.distanceToSqr(route.destination) > DESTINATION_CHANGE_DISTANCE_SQR) {
                if (!route.climbing || route.waiting) {
                    cancelLadderRoute(recruit, serverLevel);
                    return;
                }
                route.destination = currentDestination;
            }
            if (currentDestination == null && (!route.climbing || route.waiting)) {
                cancelLadderRoute(recruit, serverLevel);
                return;
            }
            Vec3 destination = route.destination;

            Entity routeEntity = serverLevel.getEntity(route.ladderUuid);
            if (!(routeEntity instanceof SiegeLadderEntity ladder) || !ladder.isReadyForAutomatedClimb()) {
                failRoute(recruit, routeEntity instanceof SiegeClimbableControl climbable ? climbable : null);
                return;
            }
            RecruitsDebug.ladder(recruit, ladder, destination);

            if (!route.climbing) {
                // Off the ladder, it is only worth going on with while the climb still gets it somewhere.
                if (Math.abs(destination.y - recruit.getY()) < MIN_ELEVATION_CHANGE
                        || !isUsefulRoute(recruit.position(), destination, ladder, route.upward)) {
                    cancelLadderRoute(recruit, serverLevel);
                    RETRY_AFTER_TICK.put(recruit, recruit.tickCount + FAILED_ROUTE_RETRY_TICKS);
                    return;
                }
                Vec3 entrance = route.upward ? ladder.getAutomatedBottomApproach() : ladder.getAutomatedTopExit();
                double distance = recruit.distanceToSqr(entrance);
                if (distance < route.closestApproachSqr - 0.25D) {
                    route.closestApproachSqr = distance;
                    route.approachProgressTick = recruit.tickCount;
                }
                if (recruit.tickCount - route.approachProgressTick > APPROACH_TIMEOUT_TICKS) {
                    failRoute(recruit, ladder);
                    return;
                }
                if (distance > JOIN_LINE_DISTANCE_SQR) {
                    LadderTraversalControl.approach(recruit, ladder);
                    RecruitsWalkOrders.walkTo(recruit, entrance, 1.15D);
                    return;
                }
                // Close enough to join the line: the ladder brings it to its place in the line itself.
                route.climbing = true;
                route.waiting = true;
                route.lineProgressTick = recruit.tickCount;
                RecruitsWalkOrders.stop(recruit);
            }

            SiegeClimbableControl.ClimbResult result = ladder.advanceAutomatedClimber(recruit, route.upward);
            if (result == SiegeClimbableControl.ClimbResult.WAITING) {
                Vec3 place = ladder.getAutomatedQueuePosition(recruit, route.upward);
                if (route.linePlace == null || place.distanceToSqr(route.linePlace) > 0.25D) {
                    route.linePlace = place;
                    route.lineProgressTick = recruit.tickCount;
                    route.closestLineDistanceSqr = Double.MAX_VALUE;
                }
                double distanceToPlace = recruit.distanceToSqr(place);
                if (distanceToPlace < route.closestLineDistanceSqr - .25D) {
                    route.closestLineDistanceSqr = distanceToPlace;
                    route.lineProgressTick = recruit.tickCount;
                }
                route.waiting = true;
                if (recruit.tickCount - route.lineProgressTick > STALLED_LINE_TICKS) {
                    failRoute(recruit, ladder);
                }
            } else if (result == SiegeClimbableControl.ClimbResult.COMPLETE) {
                LADDER_ROUTES.remove(recruit);
                RETRY_AFTER_TICK.put(recruit, recruit.tickCount + FAILED_ROUTE_RETRY_TICKS);
                COMPLETED_ROUTES.put(recruit, new CompletedLadderRoute(
                        route.ladderUuid, route.upward, destination,
                        recruit.tickCount + COMPLETED_ROUTE_RETRY_TICKS));
                resumeMoveOrder(recruit, route, destination);
                beginRouteHandoff(recruit, destination);
            } else if (result == SiegeClimbableControl.ClimbResult.FAILED) {
                failRoute(recruit, ladder);
            } else {
                // The ladder owns blockage recovery and returns a blocked stream to a safe
                // landing. A wall-clock timeout here would release long-ladder users in mid-air.
                route.waiting = false;
            }
            return;
        }

        if (RETRY_AFTER_TICK.getOrDefault(recruit, 0) > recruit.tickCount) {
            return;
        }

        Vec3 destination = getDestination(recruit);
        if (destination == null) {
            WALK_SEARCHES.remove(recruit);
            COMPLETED_ROUTES.remove(recruit);
            RETRY_AFTER_TICK.remove(recruit);
            return;
        }

        CompletedLadderRoute completedRoute = COMPLETED_ROUTES.get(recruit);
        if (completedRoute != null && (recruit.tickCount >= completedRoute.expiresTick
                || destination.distanceToSqr(completedRoute.destination) > DESTINATION_CHANGE_DISTANCE_SQR)) {
            COMPLETED_ROUTES.remove(recruit);
            completedRoute = null;
        }

        if (Math.abs(destination.y - recruit.getY()) < MIN_ELEVATION_CHANGE) {
            RETRY_AFTER_TICK.remove(recruit);
            return;
        }

        Boolean walks = walkReaches(recruit, destination);
        if (walks == null) {
            return;
        }
        if (walks) {
            // It can walk there: no ladder for this destination for a while.
            RETRY_AFTER_TICK.put(recruit, recruit.tickCount + WALK_RECHECK_TICKS);
            return;
        }

        boolean upward = destination.y > recruit.getY();
        CompletedLadderRoute suppressedRoute = completedRoute;
        AABB searchBox = recruit.getBoundingBox().inflate(LADDER_SEARCH_RANGE);
        SiegeLadderEntity ladder = serverLevel.getEntitiesOfClass(SiegeLadderEntity.class, searchBox,
                        SiegeLadderEntity::isReadyForAutomatedClimb).stream()
                .filter(candidate -> suppressedRoute == null
                        || !suppressedRoute.matches(candidate.getUUID(), upward))
                .filter(candidate -> isUsefulRoute(recruit.position(), destination, candidate, upward))
                .filter(candidate -> candidate.canTraverse(recruit, upward))
                .min(Comparator.comparingDouble(candidate -> routeScore(recruit.position(), destination, candidate, upward)))
                .orElse(null);
        if (ladder != null) {
            LadderRoute newRoute = new LadderRoute(ladder.getUUID(), upward, destination, recruit.tickCount,
                    shouldSuspendMoveOrder(recruit));
            LADDER_ROUTES.put(recruit, newRoute);
            if (newRoute.suspendedMoveOrder) {
                recruit.setShouldMovePos(false);
            }
            RecruitsWalkOrders.stop(recruit);
            LadderTraversalControl.approach(recruit, ladder);
        } else {
            // A failed search is a result too. Do not start an async path job for every recruit
            // on every tick while no ladder can bridge this height difference.
            RETRY_AFTER_TICK.put(recruit, recruit.tickCount + FAILED_ROUTE_RETRY_TICKS);
        }
    }

    /**
     * Whether the recruit can walk to {@code destination} by its own pathfinder: true or false, or null while the
     * search, which Recruits may run off the main thread, is still under way.
     */
    private static Boolean walkReaches(AbstractRecruitEntity recruit, Vec3 destination) {
        WalkSearch search = WALK_SEARCHES.get(recruit);
        if (search == null || search.destination.distanceToSqr(destination) > WALK_ARRIVAL_DISTANCE_SQR) {
            Path path = recruit.getNavigation().createPath(BlockPos.containing(destination), 1);
            if (path == null) {
                WALK_SEARCHES.remove(recruit);
                return false;
            }
            search = new WalkSearch(destination, path, recruit.tickCount, shouldSuspendMoveOrder(recruit));
            WALK_SEARCHES.put(recruit, search);
        }
        if (search.path instanceof AsyncPath async && !async.isProcessed()) {
            if (recruit.tickCount - search.startedTick <= WALK_SEARCH_TICKS) {
                // Recruits' move goal considers horizontal arrival sufficient and clears the
                // command even when the requested floor is still above/below us. Protect it
                // while its async route query is pending, not just after selecting a ladder.
                if (search.suspendedMoveOrder) recruit.setShouldMovePos(false);
                return null;
            }
            WALK_SEARCHES.remove(recruit);
            if (search.suspendedMoveOrder) recruit.setShouldMovePos(true);
            return false;
        }
        WALK_SEARCHES.remove(recruit);
        if (search.suspendedMoveOrder) recruit.setShouldMovePos(true);
        Node end = search.path.getEndNode();
        return end != null
                && Vec3.atBottomCenterOf(end.asBlockPos()).distanceToSqr(destination) <= WALK_ARRIVAL_DISTANCE_SQR;
    }

    private static boolean shouldSuspendMoveOrder(AbstractRecruitEntity recruit) {
        LivingEntity target = recruit.getTarget();
        return (target == null || !target.isAlive())
                && recruit.getFollowState() == 0
                && recruit.getShouldMovePos()
                && recruit.getMovePos() != null;
    }

    private static Vec3 getRouteDestination(AbstractRecruitEntity recruit, LadderRoute route) {
        if (!route.suspendedMoveOrder || recruit.getFollowState() != 0) {
            return getDestination(recruit);
        }
        if (recruit.getMovePos() == null) {
            return null;
        }
        return recruit.getShouldMovePos() ? recruit.getMovePos().getCenter() : route.destination;
    }

    private static boolean isUsefulRoute(Vec3 origin, Vec3 destination, SiegeLadderEntity ladder, boolean upward) {
        Vec3 entrance = upward ? ladder.getAutomatedBottomApproach() : ladder.getAutomatedTopExit();
        Vec3 exit = upward ? ladder.getAutomatedTopExit() : ladder.getAutomatedBottomApproach();
        Vec3 ladderAxis = exit.subtract(entrance);
        double axisLengthSqr = ladderAxis.lengthSqr();
        if (axisLengthSqr < 1.0D) {
            return false;
        }

        double originProgress = origin.subtract(entrance).dot(ladderAxis) / axisLengthSqr;
        double destinationProgress = destination.subtract(entrance).dot(ladderAxis) / axisLengthSqr;
        boolean advancesElevation = upward ? exit.y > origin.y + 1.0D : exit.y < origin.y - 1.0D;
        return advancesElevation
                && originProgress < 0.5D + ROUTE_ENDPOINT_PROGRESS_MARGIN
                && destinationProgress > 0.5D - ROUTE_ENDPOINT_PROGRESS_MARGIN
                && destinationProgress > originProgress + ROUTE_ENDPOINT_PROGRESS_MARGIN
                && entrance.distanceToSqr(origin) <= LADDER_SEARCH_RANGE * LADDER_SEARCH_RANGE;
    }

    private static double routeScore(Vec3 origin, Vec3 destination, SiegeLadderEntity ladder, boolean upward) {
        Vec3 entrance = upward ? ladder.getAutomatedBottomApproach() : ladder.getAutomatedTopExit();
        Vec3 exit = upward ? ladder.getAutomatedTopExit() : ladder.getAutomatedBottomApproach();
        return entrance.distanceToSqr(origin) + exit.distanceToSqr(destination) * 0.25D;
    }

    private static Vec3 getDestination(AbstractRecruitEntity recruit) {
        // A fight takes a recruit up or down a ladder only when it has no order of where to be: one told to hold a
        // position, go somewhere or follow does not leave it to chase a spider on the wall.
        LivingEntity target = recruit.getTarget();
        boolean free = recruit.getFollowState() == 0 && !(recruit.getShouldMovePos() && recruit.getMovePos() != null);
        if (free && target != null && target.isAlive()) {
            return target.position();
        }

        return switch (recruit.getFollowState()) {
            case 0 -> recruit.getMovePos() != null && (recruit.getShouldMovePos()
                    || WALK_SEARCHES.containsKey(recruit) && WALK_SEARCHES.get(recruit).suspendedMoveOrder)
                    ? recruit.getMovePos().getCenter() : null;
            case 1 -> recruit.getOwner() == null ? null : recruit.getOwner().position();
            case 2, 3, 4 -> recruit.getHoldPos();
            case 5 -> recruit.getProtectingMob() == null ? null : recruit.getProtectingMob().position();
            default -> null;
        };
    }

    private static boolean tickTransportExitRoute(AbstractRecruitEntity recruit, ServerLevel serverLevel) {
        TransportExitRoute route = TRANSPORT_EXIT_ROUTES.get(recruit);
        if (route == null) {
            return false;
        }

        Entity entity = serverLevel.getEntity(route.transportUuid);
        if (!(entity instanceof SiegeTransportControl transport)) {
            TRANSPORT_EXIT_ROUTES.remove(recruit);
            return false;
        }

        SiegeTransportControl.ExitResult result = transport.advanceAutomatedExit(recruit);
        if (result == SiegeTransportControl.ExitResult.IN_PROGRESS) {
            RecruitsWalkOrders.stop(recruit);
            return true;
        }

        TRANSPORT_EXIT_ROUTES.remove(recruit);
        if (result == SiegeTransportControl.ExitResult.COMPLETE) {
            beginRouteHandoff(recruit, route.destination);
        } else {
            transport.cancelAutomatedExit(recruit);
        }
        return false;
    }

    private static void beginRouteHandoff(AbstractRecruitEntity recruit, Vec3 destination) {
        if (destination == null || recruit.position().distanceToSqr(destination) <= ROUTE_HANDOFF_DISTANCE_SQR) {
            return;
        }
        ROUTE_HANDOFFS.put(recruit, new RouteHandoff(
                recruit.position(), destination, recruit.tickCount + ROUTE_HANDOFF_TICKS));
        RecruitsWalkOrders.walkTo(recruit, destination, 1.15D);
    }

    private static boolean tickRouteHandoff(AbstractRecruitEntity recruit) {
        RouteHandoff handoff = ROUTE_HANDOFFS.get(recruit);
        if (handoff == null) {
            return false;
        }

        Vec3 destination = getDestination(recruit);
        if (destination == null || recruit.tickCount >= handoff.expiresTick
                || destination.distanceToSqr(handoff.destination) > DESTINATION_CHANGE_DISTANCE_SQR
                || recruit.position().distanceToSqr(handoff.start) >= ROUTE_HANDOFF_DISTANCE_SQR
                || recruit.position().distanceToSqr(destination) <= ROUTE_HANDOFF_DISTANCE_SQR) {
            ROUTE_HANDOFFS.remove(recruit);
            return false;
        }

        RecruitsWalkOrders.walkTo(recruit, destination, 1.15D);
        return true;
    }

    private static void cancelLadderRoute(AbstractRecruitEntity recruit, ServerLevel serverLevel) {
        LadderRoute route = LADDER_ROUTES.remove(recruit);
        if (route == null) {
            return;
        }
        RecruitsWalkOrders.stop(recruit);
        Entity entity = serverLevel.getEntity(route.ladderUuid);
        if (entity instanceof SiegeClimbableControl ladder) {
            ladder.cancelAutomatedClimb(recruit);
        }
        resumeMoveOrder(recruit, route, route.destination);
    }

    private static void failRoute(AbstractRecruitEntity recruit, SiegeClimbableControl ladder) {
        if (ladder != null) {
            ladder.cancelAutomatedClimb(recruit);
        }
        RecruitsWalkOrders.stop(recruit);
        LadderRoute route = LADDER_ROUTES.remove(recruit);
        if (route != null) {
            resumeMoveOrder(recruit, route, route.destination);
        }
        RETRY_AFTER_TICK.put(recruit, recruit.tickCount + FAILED_ROUTE_RETRY_TICKS);
    }

    private static void resumeMoveOrder(AbstractRecruitEntity recruit, LadderRoute route, Vec3 destination) {
        if (!route.suspendedMoveOrder || recruit.getFollowState() != 0
                || recruit.getMovePos() == null || recruit.getShouldMovePos()) {
            return;
        }
        recruit.setMovePos(BlockPos.containing(destination));
        recruit.setFollowState(0);
        recruit.setShouldMovePos(true);
    }

    static boolean hasActiveLadderRoute(AbstractRecruitEntity recruit) {
        return LADDER_ROUTES.containsKey(recruit);
    }

    static boolean requestTransportExit(AbstractRecruitEntity recruit) {
        return requestTransportExit(recruit, true);
    }

    static boolean requestTransportExit(AbstractRecruitEntity recruit, boolean overBridge) {
        Entity vehicle = recruit.getVehicle();
        if (!(vehicle instanceof SiegeTransportControl)) {
            return false;
        }
        TRANSPORT_EXIT_REQUESTS.put(recruit,
                new ExitRequest(vehicle.getUUID(), overBridge, recruit.tickCount));
        return true;
    }

    private record ExitRequest(UUID transportUuid, boolean overBridge, int createdTick) {
    }

    static boolean requestBoarding(AbstractRecruitEntity recruit, AbstractSiegeEntity machine) {
        if (recruit.getVehicle() == machine) {
            return true;
        }
        if (recruit.isPassenger()) {
            return false;
        }

        if (recruit.level() instanceof ServerLevel serverLevel) {
            cancelLadderRoute(recruit, serverLevel);
        }
        recruit.shouldMount(true, null);
        recruit.setTarget(null);
        recruit.dismount = 0;
        RecruitsWalkOrders.stop(recruit);
        BOARD_ROUTES.put(recruit, new BoardRoute(machine.getUUID(), recruit.tickCount,
                SuspendedOrders.takeFrom(recruit)));
        RecruitsDebug.tower(recruit, "walking round to board");
        return true;
    }

    private static boolean tickBoardRoute(AbstractRecruitEntity recruit, ServerLevel serverLevel) {
        BoardRoute route = BOARD_ROUTES.get(recruit);
        if (route == null) {
            return false;
        }

        if (recruit.getVehicle() != null) {
            BOARD_ROUTES.remove(recruit);
            route.orders.giveBackTo(recruit);
            recruit.setShouldMount(false);
            return false;
        }

        recruit.setShouldMovePos(false);

        Entity entity = serverLevel.getEntity(route.machineUuid);
        if (!(entity instanceof AbstractSiegeEntity machine) || machine.isRemoved()
                || recruit.tickCount - route.createdTick > BOARD_TIMEOUT_TICKS
                || !machine.canAddPassenger(recruit)) {
            RecruitsDebug.tower(recruit, "gave up boarding");
            route.orders.giveBackTo(recruit);
            abandonBoarding(recruit, entity instanceof AbstractSiegeEntity siege ? siege : null);
            return false;
        }

        recruit.clearTarget();
        recruit.dismount = 0;

        if (route.spot == null || recruit.tickCount % 20 == 0) {
            route.spot = machine.boardingSpot(recruit);
        }

        recruit.shouldMount(true, null);
        double toMachine = recruit.distanceToSqr(machine);
        if (toMachine < route.closest - 0.25D) {
            route.closest = toMachine;
            route.stuckSince = recruit.tickCount;
        }
        boolean stuck = recruit.tickCount - route.stuckSince > BOARD_STUCK_TICKS
                && toMachine <= BOARD_LAST_RESORT_SQR;

        if (!stuck && recruit.distanceToSqr(route.spot) > BOARD_ARRIVAL_DISTANCE_SQR
                && toMachine > BOARD_REACH_SQR) {
            if (recruit.tickCount % 20 == 0) {
                RecruitsDebug.tower(recruit, String.format("walking to board, %.1f to the spot, %.1f to it",
                        Math.sqrt(recruit.distanceToSqr(route.spot)),
                        Math.sqrt(recruit.distanceToSqr(machine))));
            }
            RecruitsWalkOrders.walkTo(recruit, route.spot, 1.15D);
            return true;
        }

        RecruitsWalkOrders.stop(recruit);
        BOARD_ROUTES.remove(recruit);
        if (stuck) {
            RecruitsDebug.tower(recruit, "could get no closer, taken aboard from where he stood");
        }
        if (!recruit.startRiding(machine, true)) {
            RecruitsDebug.tower(recruit, "refused a place on arrival");
            route.orders.giveBackTo(recruit);
            abandonBoarding(recruit, machine);
            return false;
        }
        settleAboard(recruit);
        RecruitsDebug.tower(recruit, "aboard");
        return false;
    }

    private static void settleAboard(AbstractRecruitEntity recruit) {
        recruit.dismount = 0;
        recruit.setShouldMount(false);
    }

    private static void abandonBoarding(AbstractRecruitEntity recruit, AbstractSiegeEntity machine) {
        BOARD_ROUTES.remove(recruit);
        recruit.shouldMount(false, null);
        RecruitsWalkOrders.stop(recruit);
        if (machine instanceof SiegeTowerEntity tower) {
            tower.cancelBoardingReservation(recruit);
        }
    }

    static boolean isBoardingOn(AbstractRecruitEntity recruit, AbstractSiegeEntity machine) {
        BoardRoute route = BOARD_ROUTES.get(recruit);
        return route != null && route.machineUuid.equals(machine.getUUID());
    }

    static boolean cancelBoarding(AbstractRecruitEntity recruit) {
        BoardRoute route = BOARD_ROUTES.remove(recruit);
        if (route == null) {
            return false;
        }
        route.orders.giveBackTo(recruit);
        recruit.shouldMount(false, null);
        RecruitsWalkOrders.stop(recruit);
        return true;
    }

    private static final class BoardRoute {
        private final UUID machineUuid;
        private final int createdTick;
        private final SuspendedOrders orders;
        private Vec3 spot;
        private double closest = Double.MAX_VALUE;
        private int stuckSince;

        private BoardRoute(UUID machineUuid, int createdTick, SuspendedOrders orders) {
            this.machineUuid = machineUuid;
            this.createdTick = createdTick;
            this.orders = orders;
            this.stuckSince = createdTick;
        }
    }

    private record SuspendedOrders(int followState, BlockPos movePos, boolean shouldMovePos) {
        private static SuspendedOrders takeFrom(AbstractRecruitEntity recruit) {
            SuspendedOrders held = new SuspendedOrders(
                    recruit.getFollowState(), recruit.getMovePos(), recruit.getShouldMovePos());
            recruit.setShouldMovePos(false);
            return held;
        }

        private void giveBackTo(AbstractRecruitEntity recruit) {
            if (recruit.isPassenger()) {
                return;
            }
            if (shouldMovePos && movePos != null) {
                recruit.setMovePos(movePos);
                recruit.setShouldMovePos(true);
            }
        }
    }

    static boolean requestTowerReturn(AbstractRecruitEntity recruit, SiegeTowerEntity tower) {
        if (recruit.getVehicle() == tower && tower.isInteriorPassenger(recruit)) {
            RecruitsDebug.tower(recruit, "already aboard");
            return true;
        }
        if (recruit.isPassenger() || !tower.reserveInteriorSeat(recruit)) {
            RecruitsDebug.tower(recruit, recruit.isPassenger()
                    ? "cannot return while riding something else"
                    : "could not reserve an interior seat");
            return false;
        }
        RecruitsDebug.tower(recruit, "return route started");

        if (recruit.level() instanceof ServerLevel serverLevel) {
            cancelLadderRoute(recruit, serverLevel);
        }
        recruit.shouldMount(false, tower.getUUID());
        recruit.setTarget(null);
        RecruitsWalkOrders.stop(recruit);
        TowerReturnRoute route = new TowerReturnRoute(tower.getUUID(), recruit.tickCount);
        route.orders = SuspendedOrders.takeFrom(recruit);
        TOWER_RETURN_ROUTES.put(recruit, route);
        return true;
    }

    private static boolean tickTowerReturnRoute(AbstractRecruitEntity recruit, ServerLevel serverLevel) {
        TowerReturnRoute route = TOWER_RETURN_ROUTES.get(recruit);
        if (route == null) {
            return false;
        }

        Entity entity = serverLevel.getEntity(route.towerUuid);
        if (!(entity instanceof SiegeTowerEntity tower) || tower.isRemoved()
                || recruit.tickCount - route.createdTick > TOWER_RETURN_TIMEOUT_TICKS) {
            failTowerReturn(recruit, entity instanceof SiegeTowerEntity siegeTower ? siegeTower : null);
            return false;
        }
        if (recruit.getVehicle() == tower && tower.isInteriorPassenger(recruit)) {
            tower.cancelAutomatedReturn(recruit);
            TOWER_RETURN_ROUTES.remove(recruit);
            giveBack(route, recruit);
            recruit.setShouldMount(false);
            return false;
        }

        recruit.setShouldMovePos(false);
        if (recruit.isPassenger()) {
            failTowerReturn(recruit, tower);
            return false;
        }

        recruit.setShouldMount(false);
        recruit.setTarget(null);
        recruit.getLookControl().setLookAt(
                tower.getX(), tower.getY() + tower.getBbHeight() * 0.7D, tower.getZ(),
                30.0F, 30.0F);

        if (recruit.getY() <= tower.getY() + TOWER_RETURN_UPPER_LEVEL_OFFSET) {
            giveBack(route, recruit);
            TOWER_RETURN_ROUTES.remove(recruit);
            requestBoarding(recruit, tower);
            RecruitsDebug.tower(recruit, "handed to ground boarding");
            return false;
        }

        if (!route.crossing) {
            if (route.approach == null || recruit.tickCount % 20 == 0) {
                route.approach = tower.getAutomatedReturnApproach(recruit);
            }
            if (route.approach == null) {
                RecruitsWalkOrders.stop(recruit);
                RecruitsDebug.tower(recruit, "has no return approach to walk to");
                return true;
            }
            if (recruit.distanceToSqr(route.approach) > TOWER_RETURN_ENTRY_REACH_SQR
                    || !tower.beginAutomatedReturn(recruit)) {
                RecruitsWalkOrders.walkTo(recruit, route.approach, 1.15D);
                return true;
            }
            RecruitsWalkOrders.stop(recruit);
            route.crossing = true;
        }

        SiegeTransportControl.ExitResult result = tower.advanceAutomatedReturn(recruit);
        if (result == SiegeTransportControl.ExitResult.IN_PROGRESS) {
            return true;
        }
        if (result == SiegeTransportControl.ExitResult.FAILED) {
            failTowerReturn(recruit, tower);
            return false;
        }

        RecruitsWalkOrders.stop(recruit);
        TOWER_RETURN_ROUTES.remove(recruit);
        if (!recruit.startRiding(tower, true)) {
            giveBack(route, recruit);
            tower.cancelBoardingReservation(recruit);
            return false;
        }
        settleAboard(recruit);
        giveBack(route, recruit);
        recruit.setShouldMount(false);
        return false;
    }

    private static void giveBack(TowerReturnRoute route, AbstractRecruitEntity recruit) {
        if (route != null && route.orders != null) {
            route.orders.giveBackTo(recruit);
            route.orders = null;
        }
    }

    private static void failTowerReturn(AbstractRecruitEntity recruit, SiegeTowerEntity tower) {
        giveBack(TOWER_RETURN_ROUTES.get(recruit), recruit);
        TOWER_RETURN_ROUTES.remove(recruit);
        recruit.setShouldMount(false);
        RecruitsWalkOrders.stop(recruit);
        if (tower != null) {
            tower.cancelAutomatedReturn(recruit);
            tower.cancelBoardingReservation(recruit);
        }
    }

    static boolean cancelTowerReturn(AbstractRecruitEntity recruit) {
        TowerReturnRoute route = TOWER_RETURN_ROUTES.remove(recruit);
        if (route == null) {
            return false;
        }
        giveBack(route, recruit);
        recruit.shouldMount(false, null);
        RecruitsWalkOrders.stop(recruit);
        if (recruit.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(route.towerUuid) instanceof SiegeTowerEntity tower) {
            tower.cancelAutomatedReturn(recruit);
            tower.cancelBoardingReservation(recruit);
        }
        return true;
    }

    private static final class LadderRoute {
        private final UUID ladderUuid;
        private final boolean upward;
        private Vec3 destination;
        private final boolean suspendedMoveOrder;
        private boolean climbing;
        private boolean waiting;
        private double closestApproachSqr = Double.MAX_VALUE;
        private int approachProgressTick;
        /** Where the ladder last placed it in its line, and when that last changed. */
        private Vec3 linePlace;
        private int lineProgressTick;
        private double closestLineDistanceSqr = Double.MAX_VALUE;

        private LadderRoute(UUID ladderUuid, boolean upward, Vec3 destination, int createdTick,
                            boolean suspendedMoveOrder) {
            this.ladderUuid = ladderUuid;
            this.upward = upward;
            this.destination = destination;
            this.suspendedMoveOrder = suspendedMoveOrder;
            this.approachProgressTick = createdTick;
        }
    }

    private record WalkSearch(Vec3 destination, Path path, int startedTick, boolean suspendedMoveOrder) {
    }

    private record CompletedLadderRoute(UUID ladderUuid, boolean upward, Vec3 destination, int expiresTick) {
        private boolean matches(UUID candidateUuid, boolean candidateUpward) {
            return ladderUuid.equals(candidateUuid) && upward == candidateUpward;
        }
    }

    private record TransportExitRoute(UUID transportUuid, Vec3 destination) {
    }

    private record RouteHandoff(Vec3 start, Vec3 destination, int expiresTick) {
    }

    private static final class TowerReturnRoute {
        private SuspendedOrders orders;
        private final UUID towerUuid;
        private final int createdTick;
        private Vec3 approach;
        private boolean crossing;

        private TowerReturnRoute(UUID towerUuid, int createdTick) {
            this.towerUuid = towerUuid;
            this.createdTick = createdTick;
        }
    }
}
