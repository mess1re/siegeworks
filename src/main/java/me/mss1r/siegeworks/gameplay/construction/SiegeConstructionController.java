package me.mss1r.siegeworks.gameplay.construction;

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

    public void save(CompoundTag root) {
        CompoundTag construction = new CompoundTag();
        progress.save(construction);
        root.put(TAG_BUILD, construction);
    }

    public void load(CompoundTag root) {
        if (root.contains(TAG_BUILD)) {
            progress.load(root.getCompound(TAG_BUILD));
        }
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
