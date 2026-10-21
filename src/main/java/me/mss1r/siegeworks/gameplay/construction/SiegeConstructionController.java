package me.mss1r.siegeworks.gameplay.construction;

import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.nbt.CompoundTag;

/** The machine's build progress and its heading while it stands unfinished; Axiomata keeps clients informed. */
public final class SiegeConstructionController {
    private static final String TAG_BUILD = "Construction";

    private final AbstractSiegeEntity siege;
    private final BuildProgress progress = BuildProgress.finished();

    public SiegeConstructionController(AbstractSiegeEntity siege) {
        this.siege = siege;
    }

    public BuildProgress progress() {
        return progress;
    }

    /** Saves before Axiomata kept the progress itself; Axiomata's own copy, when there is one, is read after it. */
    public void load(CompoundTag root) {
        if (root.contains(TAG_BUILD)) {
            progress.load(root.getCompound(TAG_BUILD));
        }
    }

    /** Share of the construction work done, at least a tenth: what the engine's health follows. */
    public float workShare() {
        BlueprintConstructionPlan plan = progress.plan();
        int total = plan.totalHits();
        if (total <= 0 || progress.complete()) {
            return 1.0F;
        }
        int done = progress.hits();
        for (int stage = 0; stage < Math.min(progress.stage(), plan.stageCount()); stage++) {
            done += plan.stage(stage).hits();
        }
        return Math.max(0.1F, Math.min(1.0F, done / (float) total));
    }

    public void orientForPlacement(float yaw) {
        siege.setTrackedYaw(yaw);
        holdOrientation();
    }

    public void holdOrientation() {
        float yaw = siege.getTrackedYaw();
        siege.setYRot(yaw);
        siege.yRotO = yaw;
        siege.setYHeadRot(yaw);
        siege.yHeadRotO = yaw;
        siege.setYBodyRot(yaw);
        siege.yBodyRotO = yaw;
        siege.lastRiderYaw = yaw;
    }
}
