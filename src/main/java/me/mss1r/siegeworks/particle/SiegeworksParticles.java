package me.mss1r.siegeworks.particle;

import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import me.mss1r.siegeworks.Siegeworks;

public final class SiegeworksParticles {
    public static final ParticleSet SET = new ParticleSet(Siegeworks.MOD_ID, "siege_smoke", "heavy_siege_smoke");
    public static final RegistrySupplier<SimpleParticleType> SIEGE_SMOKE = SET.SMOKE;
    public static final RegistrySupplier<SimpleParticleType> HEAVY_SIEGE_SMOKE = SET.HEAVY_SMOKE;
    public static final RegistrySupplier<SimpleParticleType> MUZZLE_PLUME = SET.MUZZLE_PLUME;
    public static final RegistrySupplier<SimpleParticleType> IMPACT_SMOKE_PLUME = SET.IMPACT_SMOKE_PLUME;
    public static final RegistrySupplier<ParticleType<BlockParticleOption>> FRAGMENT = SET.FRAGMENT;

    private SiegeworksParticles() {}
    public static void register() { SET.register(); }
}
