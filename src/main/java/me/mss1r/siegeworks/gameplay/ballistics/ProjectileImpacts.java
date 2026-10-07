package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.axiomata.ballistics.ImpactResolver;
import me.mss1r.axiomata.ballistics.ImpactResults;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ProjectileImpacts implements ImpactResults {
    public static final TagKey<Block> PROJECTILE_PROOF = TagKey.create(Registries.BLOCK,
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "projectile_proof"));
    private ProjectileImpacts() {}

    public static Material material(BlockGetter level, BlockPos pos, BlockState state) {
        return SiegeBallisticsEnvironment.IMPACTS.material(level, pos, state);
    }

    public static boolean canCut(ProjectilePhysicsProfile physics, BlockGetter level, BlockPos pos, BlockState state) {
        return SiegeBallisticsEnvironment.IMPACTS.canCut(physics, level, pos, state);
    }

    public static double speedThrough(Material material, double mass, double diameter, double speed, double length) {
        return ImpactResolver.speedThrough(material, mass, diameter, speed, length);
    }

    public static Drive drive(ServerLevel level, ProjectilePhysicsProfile physics, double diameter,
                              Vec3 entry, BlockPos firstBlock, Vec3 velocity, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.IMPACTS.drive(level, physics, diameter, entry, firstBlock, velocity, breaker);
    }

    public static void stop(ServerLevel level, Vec3 center, Vec3 outward, Vec3 along,
                            ProjectilePhysicsProfile physics, double speed, double diameter,
                            @Nullable Player breaker) {
        SiegeBallisticsEnvironment.IMPACTS.stop(level, center, outward, along, physics, speed, diameter, breaker);
    }

    public static void blast(ServerLevel level, Vec3 center, Vec3 outward, double energy, @Nullable Player breaker) {
        SiegeBallisticsEnvironment.IMPACTS.blast(level, center, outward, energy, breaker);
    }

    public static void crush(ServerLevel level, Vec3 center, Vec3 outward, @Nullable Vec3 along, double energy,
                             double diameter, double channel, double hardness, @Nullable Player breaker) {
        SiegeBallisticsEnvironment.IMPACTS.crush(level, center, outward, along, energy, diameter, channel, hardness, breaker);
    }

    public static void ignite(ServerLevel level, Vec3 center, ProjectilePhysicsProfile.Fire fire,
                               @Nullable Player breaker) {
        SiegeBallisticsEnvironment.IMPACTS.ignite(level, center, fire, breaker);
    }

}
