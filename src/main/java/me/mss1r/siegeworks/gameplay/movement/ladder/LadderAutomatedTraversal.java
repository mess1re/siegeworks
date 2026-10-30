package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.siegeworks.api.SiegeClimbableControl.ClimbResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LadderAutomatedTraversal {
    private static final double CLIMB_LOCAL_Y_PER_TICK = 0.075D;
    private static final double TRANSITION_DISTANCE_PER_TICK = 0.11D;
    private static final double CLIMBER_GAP = 1.35D;
    private static final double APPROACH_OFFSET = 1.25D;
    private static final double EXIT_OFFSET = 0.9D;
    private static final double LANDING_MIN_DEPTH = 0.2D;
    private static final double LANDING_MAX_DEPTH = 3.75D;
    private static final double LANDING_SAMPLE_STEP = 0.25D;
    private static final double WAITING_SPACING = 0.9D;
    private static final int CLIMBER_TIMEOUT_TICKS = 40;
    private static final double STAGING_ARRIVAL_DISTANCE_SQR = 0.64D;
    private static final int BLOCKED_TIMEOUT_TICKS = 200;
    private static final double WALK_EXTENSION = 4.0D / 16.0D;
    private static final double GUIDED_BACK_OFFSET = 3.0D / 16.0D;
    private static final double CLIMB_POSE_MIN_VERTICAL_FACTOR = 0.60D;
    private static final double[] SIDE_OFFSETS = {0.0D, -0.9D, 0.9D, -1.8D, 1.8D};

    private final Host host;
    private final Map<UUID, Climber> climbers = new LinkedHashMap<>();
    private final Deque<UUID> queue = new ArrayDeque<>();
    private final Set<UUID> activeClimbers = new LinkedHashSet<>();
    private int sequence;
    private int topExitTick = Integer.MIN_VALUE;
    private Vec3 cachedTopExit;

    public LadderAutomatedTraversal(Host host) {
        this.host = host;
    }

    public int activeClimberCount() {
        return activeClimbers.size();
    }

    public boolean hasClimbers() {
        return !climbers.isEmpty();
    }

    public boolean routeAvailable(LivingEntity climber) {
        Vec3 lower = ladderPoint(0, climber), upper = ladderPoint(host.ladderLength() + WALK_EXTENSION, climber);
        Vec3 bottom = LadderTraversalCollision.standing(climber, bottomApproach(), bottomApproach().y - 1.5D);
        Vec3 top = findTopLanding(host.ladderWorldPoint(host.ladderLength() + WALK_EXTENSION), 0, climber, false);
        return bottom != null && top != null && LadderTraversalCollision.passage(climber, lower, upper)
                && transitionClear(climber, bottom, lower) && transitionClear(climber, upper, top);
    }

    private boolean transitionClear(LivingEntity climber, Vec3 from, Vec3 to) {
        double height = Math.max(from.y, to.y);
        Vec3 up = new Vec3(from.x, height, from.z), across = new Vec3(to.x, height, to.z);
        return LadderTraversalCollision.passage(climber, from, up)
                && LadderTraversalCollision.passage(climber, up, across)
                && LadderTraversalCollision.passage(climber, across, to);
    }

    public boolean isActive(UUID entityUuid) {
        return activeClimbers.contains(entityUuid);
    }

    public String describe(UUID uuid) {
        Climber state = climbers.get(uuid);
        Entity entity = host.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(uuid) : null;
        Vec3 waiting = state == null ? null : waitingCandidate(state.upward, state.stagingIndex);
        Vec3 standing = state == null ? null : waitingPosition(state.upward, state.stagingIndex);
        return state == null ? "not registered" : state.phase + " along=" + state.ladderProgress
                + " exit=" + state.exit + " leg=" + state.transitionLeg + " blocked=" + state.blockedSince
                + " returning=" + state.returning + " slot=" + state.stagingIndex
                + " waitingAt=" + standing + " ownerAlive=" + host.ladder().isAlive()
                + " direct=" + state.directWalk + " ordered=" + state.orderedPosition
                + " " + LadderTraversalCollision.describeStanding(host.ladder(), waiting, .6, 1.8)
                + (entity instanceof Mob mob ? " speed=" + mob.getSpeed() + " ai=" + !mob.isNoAi()
                + " ground=" + mob.onGround() + " velocity=" + mob.getDeltaMovement()
                + " walkable=" + LadderTraversalCollision.walkable(mob, mob.position(), standing == null ? waiting : standing)
                + (state.phase == Phase.WAITING && !state.directWalk ? " path=" + java.util.stream.DoubleStream.of(.25, .5, .75)
                .mapToObj(part -> LadderTraversalCollision.describeStanding(mob,
                        mob.position().lerp(standing == null ? waiting : standing, part), mob.getBbWidth(), mob.getBbHeight())).toList() : "") : "");
    }

    public Vec3 bottomApproach() {
        return host.ladderWorldPoint(0).subtract(host.uphillVector().scale(APPROACH_OFFSET))
                .add(0.0D, 0.05D, 0.0D);
    }

    public Vec3 topExit() {
        if (topExitTick == host.gameTick() && cachedTopExit != null) return cachedTopExit;
        double localY = host.ladderLength() + WALK_EXTENSION;
        Vec3 top = host.ladderWorldPoint(localY);
        Vec3 landing = findTopLanding(top, 0, null, false);
        cachedTopExit = landing == null
                ? top.add(host.uphillVector().scale(EXIT_OFFSET)).add(0.0D, 0.05D, 0.0D)
                : landing;
        topExitTick = host.gameTick();
        return cachedTopExit;
    }

    public Vec3 queuePosition(LivingEntity climber, boolean upward) {
        if (!canUse(climber)) {
            return upward ? bottomApproach() : topExit();
        }

        Climber state = getOrCreate(climber, upward);
        Vec3 place = state == null ? null : waitingPosition(upward, state.stagingIndex);
        return place == null ? climber.position() : place;
    }

    public ClimbResult advance(LivingEntity climber, boolean upward) {
        if (!canUse(climber)) {
            cancel(climber);
            return ClimbResult.FAILED;
        }

        UUID uuid = climber.getUUID();
        Climber state = getOrCreate(climber, upward);
        if (state == null) {
            return ClimbResult.FAILED;
        }
        state.ready = state.phase != Phase.WAITING || hasArrived(climber, state);
        state.lastRequestTick = host.gameTick();
        promote();

        if (!activeClimbers.contains(uuid)) {
            LadderTraversalControl.queue(climber, host.ladder());
            moveWaitingClimber(climber, state);
            return ClimbResult.WAITING;
        }
        return advanceActive(climber, state);
    }

    /** Lets every led climber go, as when the ladder is pushed off its wall; {@code find} looks them up by id. */
    public void letGoAll(java.util.function.Function<UUID, LivingEntity> find) {
        for (UUID uuid : new java.util.ArrayList<>(climbers.keySet())) {
            LivingEntity climber = find.apply(uuid);
            if (climber != null) {
                cancel(climber);
            } else {
                climbers.remove(uuid);
                queue.remove(uuid);
                activeClimbers.remove(uuid);
            }
        }
        reindexQueue();
    }

    public void cancel(LivingEntity climber) {
        LadderTraversalControl.release(climber);
        UUID uuid = climber.getUUID();
        climbers.remove(uuid);
        if (queue.remove(uuid)) {
            reindexQueue();
        }
        if (activeClimbers.remove(uuid)) {
            promote();
        }
    }

    public void tick() {
        Iterator<Map.Entry<UUID, Climber>> iterator = climbers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Climber> entry = iterator.next();
            Entity entity = host.level() instanceof ServerLevel serverLevel
                    ? serverLevel.getEntity(entry.getKey()) : null;
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || host.gameTick() - entry.getValue().lastRequestTick > CLIMBER_TIMEOUT_TICKS) {
                iterator.remove();
                queue.remove(entry.getKey());
                activeClimbers.remove(entry.getKey());
            }
        }
        reindexQueue();
        promote();
    }

    private boolean canUse(LivingEntity climber) {
        return host.ready() && climber.isAlive() && climber.level() == host.level();
    }

    private Climber getOrCreate(LivingEntity climber, boolean upward) {
        UUID uuid = climber.getUUID();
        Climber state = climbers.get(uuid);
        if (state != null && state.upward != upward) {
            cancel(climber);
            return null;
        }
        if (state == null) {
            int slot = 0;
            Set<Integer> taken = new java.util.HashSet<>();
            for (UUID queued : queue) taken.add(climbers.get(queued).stagingIndex);
            while (taken.contains(slot)) slot++;
            state = new Climber(upward, sequence++, slot, host.gameTick());
            climbers.put(uuid, state);
            queue.addLast(uuid);
            reindexQueue();
        }
        state.lastRequestTick = host.gameTick();
        return state;
    }

    private ClimbResult advanceActive(LivingEntity climber, Climber state) {
        double end = host.ladderLength() + WALK_EXTENSION;
        if (state.phase == Phase.ENTERING || state.phase == Phase.REJOINING) {
            Vec3 ladderEntry = ladderPoint(state.ladderProgress, climber);
            if (transition(climber, state, ladderEntry)) {
                state.phase = Phase.TRAVERSING;
            }
            return blockedResult(climber, state);
        }

        if (state.phase == Phase.TRAVERSING) {
            boolean upward = state.returning ? !state.upward : state.upward;
            double next = Mth.clamp(state.ladderProgress
                            + (upward ? CLIMB_LOCAL_Y_PER_TICK : -CLIMB_LOCAL_Y_PER_TICK),
                    0.0D, end);
            for (UUID otherId : activeClimbers) {
                if (otherId.equals(climber.getUUID())) continue;
                Climber other = climbers.get(otherId);
                if (other == null || other.phase == Phase.ENTERING) continue;
                boolean otherUp = other.returning ? !other.upward : other.upward;
                if (otherUp != upward) continue;
                double ahead = upward ? other.ladderProgress - state.ladderProgress
                        : state.ladderProgress - other.ladderProgress;
                if (ahead < -1.0E-6D) continue;
                double gap = bodyGap(climber, otherId);
                if (ahead < gap + CLIMB_LOCAL_Y_PER_TICK) {
                    hold(climber);
                    return ClimbResult.IN_PROGRESS;
                }
            }
            Vec3 point = ladderPoint(next, climber);
            if (!place(climber, point)) return blockedResult(climber, state);
            state.blockedSince = -1;
            state.ladderProgress = next;
            if (upward && next >= end || !upward && next <= 0.0D) {
                state.phase = Phase.EXITING;
                state.transitionStart = point;
                state.transitionLeg = 0;
                state.exit = null;
            }
            promote();
            return ClimbResult.IN_PROGRESS;
        }

        if (state.exit == null) {
            state.exit = findExit(climber, state);
            state.transitionStart = climber.position();
            state.transitionLeg = 0;
        }
        if (state.exit != null && !LadderTraversalCollision.clear(climber, state.exit)) {
            state.exit = null;
        }
        if (state.exit == null || !landingAvailable(state.exit, climber)) {
            // Keep the reservation while a living body occupies it. Retargeting every tick
            // makes climbers oscillate between landings and resets their blockage timeout.
            hold(climber);
            return blockedResult(climber, state);
        }
        if (!transition(climber, state, state.exit)) return blockedResult(climber, state);
        finish(climber.getUUID());
        return state.returning ? ClimbResult.FAILED : ClimbResult.COMPLETE;
    }

    private Vec3 ladderPoint(double localY, LivingEntity climber) {
        Vec3 point = host.ladderWorldPoint(localY);
        double angle = host.leanAngleRadians();
        Vec3 backOffset = Math.abs(Math.cos(angle)) >= CLIMB_POSE_MIN_VERTICAL_FACTOR
                ? host.uphillVector().scale(Math.max(GUIDED_BACK_OFFSET, climber.getBbWidth() * .5D + .12D))
                : Vec3.ZERO;
        return new Vec3(point.x - backOffset.x, host.platformY(localY), point.z - backOffset.z);
    }

    private Vec3 findExit(LivingEntity climber, Climber state) {
        if (state.returning != state.upward) {
            double end = host.ladderLength() + WALK_EXTENSION;
            Vec3 top = host.ladderWorldPoint(end);
            Vec3 landing = findTopLanding(top, state.sequence, climber, true);
            return landing;
        }
        return findBottomLanding(state.sequence, climber);
    }

    private Vec3 findTopLanding(Vec3 top, int preferredSlot, LivingEntity climber,
                                boolean requireAvailable) {
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        for (int offset = 0; offset < SIDE_OFFSETS.length; offset++) {
            double side = SIDE_OFFSETS[Math.floorMod(preferredSlot + offset, SIDE_OFFSETS.length)];
            for (double depth = LANDING_MIN_DEPTH;
                 depth <= LANDING_MAX_DEPTH; depth += LANDING_SAMPLE_STEP) {
                Vec3 candidate = top.add(uphill.scale(depth)).add(right.scale(side));
                Vec3 safe = climber == null ? findSafeStandingPosition(candidate)
                        : LadderTraversalCollision.standing(climber, candidate, bottomApproach().y + 1.0D);
                // Release at the first full-body landing, not four metres into the wall walk.
                // The recruit's ordinary order takes over from here; the exit must keep flowing.
                if (safe != null && (climber == null || transitionClear(climber,
                        ladderPoint(host.ladderLength() + WALK_EXTENSION, climber), safe))
                        && (!requireAvailable || landingAvailable(safe, climber))) return safe;
            }
        }
        return null;
    }

    private Vec3 findBottomLanding(int preferredSlot, LivingEntity climber) {
        Vec3 base = bottomApproach();
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        for (int row = 0; row < 3; row++) {
            for (int offset = 0; offset < SIDE_OFFSETS.length; offset++) {
                double side = SIDE_OFFSETS[Math.floorMod(preferredSlot + offset, SIDE_OFFSETS.length)];
                Vec3 candidate = base.subtract(uphill.scale(row * WAITING_SPACING)).add(right.scale(side));
                Vec3 safe = LadderTraversalCollision.standing(climber, candidate, base.y - 1.5D);
                if (safe != null && landingAvailable(safe, climber)) {
                    return safe;
                }
            }
        }
        return null;
    }

    private Vec3 findSafeStandingPosition(Vec3 candidate) {
        return findSafeStandingPosition(candidate, bottomApproach().y + 1.0D);
    }

    private Vec3 findSafeStandingPosition(Vec3 candidate, double minimumExitY) {
        return LadderTraversalCollision.standing(host.ladder(), candidate, minimumExitY, .6D, 1.8D);
    }

    /**
     * Whether no other climber is stepping off onto {@code position}. Someone merely standing there does not hold the
     * climber on the ladder: it steps off beside them and the two push apart as anyone would, instead of a crowd at
     * the top keeping the whole line hanging.
     */
    private boolean landingAvailable(Vec3 position, LivingEntity climber) {
        for (var entry : climbers.entrySet()) {
            Vec3 reserved = entry.getValue().exit;
            if (!entry.getKey().equals(climber.getUUID()) && reserved != null
                    && Math.abs(reserved.y - position.y) < 1.9D
                    && reserved.subtract(position).horizontalDistanceSqr() < .9D * .9D) return false;
        }
        return true;
    }

    private void moveWaitingClimber(LivingEntity climber, Climber state) {
        Vec3 waitingPosition = waitingPosition(state.upward, state.stagingIndex);
        if (!(climber instanceof Mob mob)) {
            return;
        }
        if (waitingPosition == null) {
            mob.getNavigation().stop();
            return;
        }
        if (climber.distanceToSqr(waitingPosition) <= STAGING_ARRIVAL_DISTANCE_SQR) {
            state.orderedPosition = null;
            mob.getNavigation().stop();
            Vec3 velocity = climber.getDeltaMovement();
            climber.setDeltaMovement(0.0D, velocity.y, 0.0D);
            return;
        }
        if (state.directTarget == null || state.directTarget.distanceToSqr(waitingPosition) > .01D
                || host.gameTick() >= state.nextDirectCheck) {
            state.directTarget = waitingPosition;
            state.nextDirectCheck = host.gameTick() + 20;
            state.directWalk = climber.distanceToSqr(waitingPosition) <= 8.0D * 8.0D
                    && LadderTraversalCollision.walkable(climber, climber.position(), waitingPosition);
        }
        if (state.directWalk && Math.abs(climber.getY() - waitingPosition.y) < .1D) {
            // Both synchronous and asynchronous block paths can finish a cell short of the
            // staging point. Steer normally along a verified supported floor; never teleport.
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(waitingPosition.x, waitingPosition.y, waitingPosition.z, 1.0D);
            return;
        }
        if (state.orderedPosition != null && !mob.getNavigation().isDone()
                && state.orderedPosition.distanceToSqr(waitingPosition) <= STAGING_ARRIVAL_DISTANCE_SQR) {
            return;
        }
        state.orderedPosition = waitingPosition;
        mob.getNavigation().moveTo(waitingPosition.x, waitingPosition.y, waitingPosition.z, 1.0D);
    }

    private Vec3 waitingPosition(boolean upward, int queueIndex) {
        Vec3 candidate = waitingCandidate(upward, queueIndex);
        Vec3 base = upward ? bottomApproach() : topExit();
        return findSafeStandingPosition(candidate, upward ? base.y - 1.5D : bottomApproach().y + 1.0D);
    }

    private Vec3 waitingCandidate(boolean upward, int queueIndex) {
        Vec3 uphill = host.uphillVector();
        Vec3 right = new Vec3(-uphill.z, 0.0D, uphill.x);
        int lane = queueIndex % SIDE_OFFSETS.length;
        int row = queueIndex / SIDE_OFFSETS.length;
        Vec3 base = upward ? bottomApproach() : topExit();
        Vec3 candidate = base.add(right.scale(SIDE_OFFSETS[lane]));
        candidate = upward
                ? candidate.subtract(uphill.scale(row * WAITING_SPACING))
                : candidate.add(uphill.scale(row * WAITING_SPACING));
        return candidate;
    }

    private boolean place(LivingEntity climber, Vec3 position) {
        if (!LadderTraversalCollision.passage(climber, climber.position(), position)) {
            if (climbers.get(climber.getUUID()).blockedSince < 0 && "1".equals(System.getenv("SIEGEWORKS_TRACE_LADDER"))) me.mss1r.siegeworks.Siegeworks.LOG.warn(
                    "Ladder blocked {} {} from {} to {}", climber.getId(), describe(climber.getUUID()), climber.position(), position);
            hold(climber);
            return false;
        }
        climber.move(MoverType.SELF, position.subtract(climber.position()));
        boolean arrived = climber.position().distanceToSqr(position) < 1.0E-6D;
        hold(climber);
        return arrived;
    }

    private void hold(LivingEntity climber) {
        LadderTraversalControl.hold(climber, host.ladder());
        if (climber instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        climber.setDeltaMovement(Vec3.ZERO);
        climber.setOnGround(true);
        climber.fallDistance = 0.0F;
    }

    /** Rise before crossing the lip, then lower onto the actual landing; never cut through a parapet. */
    private boolean transition(LivingEntity climber, Climber state, Vec3 end) {
        double height = Math.max(state.transitionStart.y, end.y);
        Vec3 waypoint = state.transitionLeg == 0
                ? new Vec3(state.transitionStart.x, height, state.transitionStart.z)
                : state.transitionLeg == 1 ? new Vec3(end.x, height, end.z) : end;
        Vec3 delta = waypoint.subtract(climber.position());
        double distance = delta.length();
        Vec3 next = distance <= TRANSITION_DISTANCE_PER_TICK ? waypoint
                : climber.position().add(delta.scale(TRANSITION_DISTANCE_PER_TICK / distance));
        if (!place(climber, next)) return false;
        state.blockedSince = -1;
        if (distance > TRANSITION_DISTANCE_PER_TICK) return false;
        if (++state.transitionLeg < 3) return false;
        return true;
    }

    private ClimbResult blockedResult(LivingEntity climber, Climber state) {
        if (state.blockedSince < 0) state.blockedSince = host.gameTick();
        if (host.gameTick() - state.blockedSince < BLOCKED_TIMEOUT_TICKS) return ClimbResult.IN_PROGRESS;
        if (!state.returning) {
            // An impassable exit reverses the stream, not just its leader. Otherwise the
            // returning leader would meet its own followers head-on on a one-person ladder.
            for (UUID id : activeClimbers) {
                Climber active = climbers.get(id);
                if (active == null || active.returning) continue;
                active.returning = true;
                active.phase = active.phase == Phase.ENTERING ? Phase.EXITING
                        : active.phase == Phase.EXITING ? Phase.REJOINING : Phase.TRAVERSING;
                active.exit = null;
                active.transitionLeg = 0;
                active.blockedSince = -1;
                if (host.level() instanceof ServerLevel level && level.getEntity(id) instanceof LivingEntity living)
                    active.transitionStart = living.position();
            }
        }
        return ClimbResult.IN_PROGRESS;
    }

    private void promote() {
        boolean promotedAny = true;
        while (promotedAny) {
            promotedAny = false;
            Iterator<UUID> iterator = queue.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                Climber state = climbers.get(uuid);
                Entity entity = host.level() instanceof ServerLevel serverLevel
                        ? serverLevel.getEntity(uuid) : null;
                if (state == null || !(entity instanceof LivingEntity living) || !living.isAlive()) {
                    iterator.remove();
                    climbers.remove(uuid);
                    continue;
                }
                if (!state.ready) {
                    continue;
                }
                if (!canEnter(state, living)) {
                    return;
                }
                iterator.remove();
                activeClimbers.add(uuid);
                state.phase = Phase.ENTERING;
                state.ladderProgress = state.upward ? 0 : host.ladderLength() + WALK_EXTENSION;
                state.transitionStart = living.position();
                state.transitionLeg = 0;
                promotedAny = true;
                break;
            }
            if (promotedAny) {
                reindexQueue();
            }
        }
    }

    private void reindexQueue() {
        int[] rows = new int[SIDE_OFFSETS.length];
        for (UUID uuid : queue) {
            Climber state = climbers.get(uuid);
            if (state != null) {
                // Preserve the lateral lane. A departure advances only that lane, rather than
                // making the entire crowd cross left/right through one another every few ticks.
                int lane = state.stagingIndex % SIDE_OFFSETS.length;
                int nextIndex = lane + rows[lane]++ * SIDE_OFFSETS.length;
                if (state.phase == Phase.WAITING && state.stagingIndex != nextIndex) {
                    state.ready = false;
                }
                state.stagingIndex = nextIndex;
            }
        }
    }

    private boolean hasArrived(LivingEntity climber, Climber state) {
        Vec3 place = waitingPosition(state.upward, state.stagingIndex);
        return place != null && climber.distanceToSqr(place) <= STAGING_ARRIVAL_DISTANCE_SQR;
    }

    private boolean canEnter(Climber candidate, LivingEntity living) {
        double end = host.ladderLength() + WALK_EXTENSION;
        for (UUID uuid : activeClimbers) {
            Climber active = climbers.get(uuid);
            if (active == null) {
                continue;
            }
            if (active.returning || active.upward != candidate.upward || active.phase == Phase.ENTERING) {
                return false;
            }
            if (active.phase == Phase.TRAVERSING) {
                double distanceFromEntrance = active.upward
                        ? active.ladderProgress : end - active.ladderProgress;
                if (distanceFromEntrance < bodyGap(living, uuid)) {
                    return false;
                }
            }
        }
        return true;
    }

    private double bodyGap(LivingEntity candidate, UUID otherId) {
        Entity other = host.level() instanceof ServerLevel level ? level.getEntity(otherId) : null;
        double height = Math.max(candidate.getBbHeight(), other == null ? 0 : other.getBbHeight());
        double width = Math.max(candidate.getBbWidth(), other == null ? 0 : other.getBbWidth());
        double angle = host.leanAngleRadians();
        return Math.max(CLIMBER_GAP, Math.min(height / Math.max(.001, Math.abs(Math.cos(angle))),
                width / Math.max(.001, Math.abs(Math.sin(angle)))) + .15D);
    }

    private void finish(UUID uuid) {
        if (host.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) LadderTraversalControl.release(entity);
        }
        climbers.remove(uuid);
        activeClimbers.remove(uuid);
        promote();
    }

    public interface Host {
        Entity ladder();

        Level level();

        int gameTick();

        boolean ready();

        double ladderLength();

        double leanAngleRadians();

        Vec3 ladderWorldPoint(double localY);

        double platformY(double localY);

        Vec3 uphillVector();
    }

    private enum Phase {
        WAITING,
        ENTERING,
        REJOINING,
        TRAVERSING,
        EXITING
    }

    private static final class Climber {
        private final boolean upward;
        private final int sequence;
        private int stagingIndex;
        private Phase phase = Phase.WAITING;
        private int lastRequestTick;
        private double ladderProgress;
        private Vec3 transitionStart;
        private Vec3 orderedPosition;
        private Vec3 directTarget;
        private int nextDirectCheck;
        private boolean directWalk;
        private Vec3 exit;
        private boolean ready;
        private int transitionLeg;
        private int blockedSince = -1;
        private boolean returning;

        private Climber(boolean upward, int sequence, int stagingIndex, int lastRequestTick) {
            this.upward = upward;
            this.sequence = sequence;
            this.stagingIndex = stagingIndex;
            this.lastRequestTick = lastRequestTick;
        }
    }
}
