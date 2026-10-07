package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.axiomata.ballistics.DebrisPhysics;
import me.mss1r.axiomata.ballistics.ImpactResolver;
import me.mss1r.axiomata.ballistics.ProtectedBlockAccess;
import me.mss1r.axiomata.ballistics.profile.BlockMaterialCatalog;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.data.profile.BlockMaterialProfiles;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import net.minecraft.world.level.block.state.BlockState;

/** Block permissions, debris and impact settings for Siegeworks shots. */
public final class SiegeBallisticsEnvironment {
    public static final ProtectedBlockAccess BLOCKS = new ProtectedBlockAccess(
            () -> ProtectedBlockAccess.Policy.valueOf(SiegeworksServerConfig.getBlockDamage().name()), "[Siegeworks]");
    public static final DebrisPhysics DEBRIS = new DebrisPhysics(Siegeworks.MOD_ID, BLOCKS,
            SiegeworksServerConfig::isFlyingBlockDebrisEnabled,
            SiegeworksServerConfig::getMaxFlyingBlockDebrisPerTick);
    public static final ImpactResolver IMPACTS = new ImpactResolver(new ImpactResolver.Environment() {
        @Override public BlockMaterialCatalog materials() { return BlockMaterialProfiles.MATERIALS; }
        @Override public double stoneFractureEnergy() { return SiegeworksServerConfig.getStoneFractureEnergy(); }
        @Override public boolean projectileProof(BlockState state) { return state.is(ProjectileImpacts.PROJECTILE_PROOF); }
        @Override public ProtectedBlockAccess blocks() { return BLOCKS; }
        @Override public DebrisPhysics debris() { return DEBRIS; }
        @Override public ParticleSet particles() { return SiegeworksParticles.SET; }
    });

    private SiegeBallisticsEnvironment() {}

    public static void initialize() {
        // Registers the debris landing handler before a saved falling block can tick.
    }
}
