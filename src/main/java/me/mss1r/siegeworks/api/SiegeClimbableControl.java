package me.mss1r.siegeworks.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Route points and per-tick movement for ladders or similar siege paths. */
public interface SiegeClimbableControl {
    boolean isReadyForAutomatedClimb();

    /** Whether this body fits the passage and both ends have real landings; other users may be queued there. */
    default boolean canTraverse(LivingEntity climber, boolean upward) { return isReadyForAutomatedClimb(); }

    Vec3 getAutomatedBottomApproach();

    Vec3 getAutomatedTopExit();

    Vec3 getAutomatedQueuePosition(LivingEntity climber, boolean upward);

    /** Advances one climber by one tick. */
    ClimbResult advanceAutomatedClimber(LivingEntity climber, boolean upward);

    /** Drops any traversal state kept for this climber. */
    void cancelAutomatedClimb(LivingEntity climber);

    enum ClimbResult {
        WAITING,
        IN_PROGRESS,
        COMPLETE,
        FAILED
    }
}
