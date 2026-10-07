package me.mss1r.siegeworks.particle;

import me.mss1r.axiomata.ballistics.particle.ParticleEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SiegeParticleEffects {
    private static final ParticleEffects EFFECTS = new ParticleEffects(SiegeworksParticles.SET);

    public enum MuzzleProfile {
        CULVERIN(0.9D, 150.0D, 7, 5, 7, 10),
        SERPENTINE(1.1D, 180.0D, 9, 6, 8, 14),
        BOMBARD(2.0D, 260.0D, 16, 9, 12, 30);

        private final ParticleEffects.MuzzleProfile parameters;

        MuzzleProfile(double scale, double range, int puffCount,
                      int flameSteps, int flamePoints, int emberCount) {
            parameters = new ParticleEffects.MuzzleProfile(scale, range, puffCount, flameSteps, flamePoints, emberCount);
        }
    }

    private SiegeParticleEffects() {}

    public static void muzzleBlast(ServerLevel level, Vec3 origin, Vec3 direction, MuzzleProfile profile) {
        EFFECTS.muzzleBlast(level, origin, direction, profile.parameters);
    }

    public static void rocketLaunch(ServerLevel level, Vec3 origin, Vec3 direction) {
        EFFECTS.rocketLaunch(level, origin, direction);
    }

    public static void penetrationImpact(ServerLevel level, Vec3 impact, double radius) {
        EFFECTS.penetrationImpact(level, impact, radius);
    }

    public static void penetrationImpact(ServerLevel level, Vec3 impact, double radius, Vec3 outwardNormal) {
        EFFECTS.penetrationImpact(level, impact, radius, outwardNormal);
    }

    public static void impact(ServerLevel level, Vec3 position, float intensity, boolean heavy) {
        EFFECTS.impact(level, position, intensity, heavy);
    }

    public static void impact(ServerLevel level, Vec3 position, float intensity,
                              boolean heavy, Vec3 outwardNormal) {
        EFFECTS.impact(level, position, intensity, heavy, outwardNormal);
    }

    public static void scattershotImpact(ServerLevel level, Vec3 position,
                                         boolean stonePellet, Vec3 outwardNormal) {
        EFFECTS.scattershotImpact(level, position, stonePellet, outwardNormal);
    }

    public static void rocketExplosion(ServerLevel level, Vec3 center) {
        EFFECTS.rocketExplosion(level, center);
    }

    public static void potShatter(ServerLevel level, Vec3 center, BlockState pot) {
        EFFECTS.potShatter(level, center, pot);
    }

    public static void incendiaryImpact(ServerLevel level, Vec3 center, int fireRadius) {
        EFFECTS.incendiaryImpact(level, center, fireRadius);
    }
}
