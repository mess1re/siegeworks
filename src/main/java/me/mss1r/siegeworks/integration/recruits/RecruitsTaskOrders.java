package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.nbt.CompoundTag;

/**
 * Assigns a siege task to a recruit and restores its previous follow order afterwards, instead of leaving it on hold.
 */
final class RecruitsTaskOrders {
    private static final String PREVIOUS_FOLLOW_STATE_TAG = "SiegeworksPreviousFollowState";
    private static final int TASK_FOLLOW_STATE = 6;
    private static final int HOLD_POSITION = 2;

    private RecruitsTaskOrders() {
    }

    static void beginTask(AbstractRecruitEntity recruit) {
        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(recruit);
        if (!data.contains(PREVIOUS_FOLLOW_STATE_TAG) && recruit.getFollowState() != TASK_FOLLOW_STATE) {
            data.putInt(PREVIOUS_FOLLOW_STATE_TAG, recruit.getFollowState());
        }
        recruit.setFollowState(TASK_FOLLOW_STATE);
    }

    static void endTask(AbstractRecruitEntity recruit) {
        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(recruit);
        int previous = data.contains(PREVIOUS_FOLLOW_STATE_TAG)
                ? data.getInt(PREVIOUS_FOLLOW_STATE_TAG)
                : HOLD_POSITION;
        data.remove(PREVIOUS_FOLLOW_STATE_TAG);
        recruit.setFollowState(previous);
    }
}
