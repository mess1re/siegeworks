package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.ballistics.profile.BlockMaterialCatalog;
import me.mss1r.axiomata.ballistics.profile.BlockMaterialProfile;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

public final class BlockMaterialProfiles {
    public static final BlockMaterialCatalog MATERIALS = new BlockMaterialCatalog();
    public static final ProfileCatalog<BlockMaterialProfile> CATALOG = MATERIALS.profiles();

    private BlockMaterialProfiles() {}

    public static Optional<BlockMaterialProfile> forState(BlockState state) {
        return MATERIALS.forState(state);
    }

    public static double resistance(BlockState state) {
        return MATERIALS.resistance(state);
    }

    public static void reset() {
        MATERIALS.reset();
    }
}
