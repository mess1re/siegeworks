package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.debug.SiegeworksDebug;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.WeakHashMap;

public final class RecruitsDebug {
    private static final int REPORT_INTERVAL_TICKS = 20;

    private static final Map<AbstractRecruitEntity, Report> LAST_REPORT = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, Integer> LAST_LADDER_REPORT = new WeakHashMap<>();
    private static final Map<AbstractRecruitEntity, Map<String, Integer>> LAST_ENGINE_REPORT = new WeakHashMap<>();

    private RecruitsDebug() {
    }

    public static boolean enabled() {
        return SiegeworksDebug.recruits();
    }

    static void clear() {
        LAST_REPORT.clear();
        LAST_LADDER_REPORT.clear();
        LAST_ENGINE_REPORT.clear();
    }

    public static void commandReceived(net.minecraft.server.level.ServerPlayer player, String channel,
                                       int action, int groupCount, BlockPos targetPos, int targetEntityId) {
        if (!enabled()) {
            return;
        }
        Siegeworks.LOG.info("[recruits] {} sent a {} command: action={} groups={} pos={} entity={}",
                player.getGameProfile().getName(), channel, action, groupCount,
                targetPos == null ? "none" : targetPos, targetEntityId);
    }

    public static void walk(AbstractRecruitEntity recruit, String stage, net.minecraft.world.phys.Vec3 goal) {
        if (!enabled()) {
            return;
        }
        net.minecraft.world.entity.ai.navigation.PathNavigation navigation = recruit.getNavigation();
        Siegeworks.LOG.info(
                "[recruits] {} {} | pos={} onGround={} motion={} navDone={} navTarget={} goal={}",
                recruit.getName().getString(), stage,
                recruit.position(), recruit.onGround(), recruit.getDeltaMovement(),
                navigation.isDone(), navigation.getTargetPos(), goal);
    }

    static void ladder(AbstractRecruitEntity recruit, SiegeLadderEntity ladder,
                       net.minecraft.world.phys.Vec3 destination) {
        if (!enabled()) return;
        Integer previous = LAST_LADDER_REPORT.get(recruit);
        if (previous != null && recruit.tickCount - previous < REPORT_INTERVAL_TICKS) return;
        LAST_LADDER_REPORT.put(recruit, recruit.tickCount);
        Siegeworks.LOG.info("[recruits:ladder] unit={} ladder={} pos={} destination={} follow={} move={} nav={} leased={} | {}",
                recruit.getUUID(), ladder.getUUID(), recruit.position(), destination,
                recruit.getFollowState(), recruit.getShouldMovePos(), recruit.getNavigation().getTargetPos(),
                me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl.ownsNavigation(recruit),
                ladder.describeAutomatedClimber(recruit));
    }

    public static void tower(AbstractRecruitEntity recruit, String stage) {
        if (!enabled()) {
            return;
        }
        Siegeworks.LOG.info("[recruits] {} tower: {} | at {} {} {} vehicle={} mount={}",
                recruit.getName().getString(), stage,
                (int) recruit.getX(), (int) recruit.getY(), (int) recruit.getZ(),
                recruit.getVehicle() == null ? "none" : recruit.getVehicle().getName().getString(),
                recruit.getMountUUID());
    }

    /** Crew engineer's decision this tick; each is logged at most once per second. */
    public static void engine(AbstractRecruitEntity engineer, String decision, String details) {
        if (!enabled()) {
            return;
        }
        Map<String, Integer> reported = LAST_ENGINE_REPORT.computeIfAbsent(engineer, ignored -> new java.util.HashMap<>());
        Integer previous = reported.get(decision);
        if (previous != null && engineer.tickCount - previous < REPORT_INTERVAL_TICKS) {
            return;
        }
        reported.put(decision, engineer.tickCount);
        Siegeworks.LOG.info("[recruits] {} engine: {} | {} | followState={} shouldMovePos={} ranged={}",
                engineer.getName().getString(), decision, details, engineer.getFollowState(),
                engineer.getShouldMovePos(), engineer.getShouldRanged());
    }

    public static void claimed(AbstractRecruitEntity recruit, String claimedBy) {
        if (!enabled()) {
            return;
        }

        BlockPos movePos = recruit.getMovePos();
        Report report = new Report(claimedBy, recruit.getFollowState(), recruit.getShouldMovePos(),
                movePos == null ? null : movePos.toString(), recruit.getNavigation().isDone());
        Report previous = LAST_REPORT.get(recruit);
        if (previous != null && previous.sameAs(report)
                && recruit.tickCount - previous.tick() < REPORT_INTERVAL_TICKS) {
            return;
        }

        LAST_REPORT.put(recruit, report.at(recruit.tickCount));
        Siegeworks.LOG.info(
                "[recruits] {} at {} {} {} | followState={} shouldMovePos={} movePos={} navDone={}",
                recruit.getName().getString(),
                (int) recruit.getX(), (int) recruit.getY(), (int) recruit.getZ(),
                claimedBy == null ? "obeys its own orders" : "held by " + claimedBy,
                report.followState(), report.shouldMovePos(),
                report.movePos() == null ? "none" : report.movePos(),
                report.navigationDone());
    }

    private record Report(String claimedBy, int followState, boolean shouldMovePos, String movePos,
                          boolean navigationDone, int tick) {
        Report(String claimedBy, int followState, boolean shouldMovePos, String movePos,
               boolean navigationDone) {
            this(claimedBy, followState, shouldMovePos, movePos, navigationDone, 0);
        }

        Report at(int tick) {
            return new Report(claimedBy, followState, shouldMovePos, movePos, navigationDone, tick);
        }

        boolean sameAs(Report other) {
            return java.util.Objects.equals(claimedBy, other.claimedBy)
                    && followState == other.followState
                    && shouldMovePos == other.shouldMovePos
                    && java.util.Objects.equals(movePos, other.movePos)
                    && navigationDone == other.navigationDone;
        }
    }
}
