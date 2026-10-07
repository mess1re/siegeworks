package me.mss1r.siegeworks.gameplay.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ExplosionPhysics {
    public static final String TAG_DEBRIS = "siegeworks:debris";
    private ExplosionPhysics() {}

    public static void scatterAffectedBlocks(ServerLevel level, Vec3 center, float radius, List<BlockPos> affectedBlocks,
                                              @Nullable Player breaker) {
        SiegeBallisticsEnvironment.DEBRIS.scatterAffectedBlocks(level, center, radius, affectedBlocks, breaker);
    }

    public static boolean launchDestroyedBlock(ServerLevel level, BlockPos pos, Vec3 center, Vec3 outward,
                                               float blastPower, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.DEBRIS.launchDestroyedBlock(level, pos, center, outward, blastPower, breaker);
    }

    public static boolean placeDebris(ServerLevel level, FallingBlockEntity debris, BlockPos pos, BlockState state) {
        return SiegeBallisticsEnvironment.DEBRIS.placeDebris(level, debris, pos, state);
    }

}
