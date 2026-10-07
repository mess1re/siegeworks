package me.mss1r.siegeworks.api;

import me.mss1r.axiomata.ballistics.Ballistics;
import net.minecraft.world.phys.Vec3;

/** Keeps the Siegeworks aiming API available to existing integrations. */
public final class SiegeBallistics {
    public static final double GRAVITY = Ballistics.GRAVITY;

    private SiegeBallistics() {}

    public record Flight(double gravity, double airDrag, double thrust, int thrustTicks) {
        /** Ballistic flight under gravity only. */
        public static Flight ballistic(double airDrag) {
            return new Flight(GRAVITY, airDrag, 0.0D, 0);
        }

        /** Velocity after one tick of flight. */
        public Vec3 afterMove(Vec3 velocity) {
            return core().afterMove(velocity);
        }

        private Ballistics.Flight core() {
            return new Ballistics.Flight(gravity, airDrag, thrust, thrustTicks);
        }
    }

    public static float calculateLowAnglePitch(Vec3 origin, Vec3 target, double speed, double gravity) {
        return Ballistics.calculateLowAnglePitch(origin, target, speed, gravity);
    }

    public static float calculateLowAnglePitch(Vec3 origin, Vec3 target, double speed, Flight flight) {
        return Ballistics.calculateLowAnglePitch(origin, target, speed, flight.core());
    }

    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity) {
        return Ballistics.calculateFixedArcPower(origin, target, baseSpeed, launchSlope, gravity);
    }

    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity, double airDrag,
                                                double minPower, double maxPower) {
        return Ballistics.calculateFixedArcPower(origin, target, baseSpeed, launchSlope, gravity, airDrag, minPower, maxPower);
    }

    public static double range(double speed, double elevationDegrees, double drop, Flight flight) {
        return Ballistics.range(speed, elevationDegrees, drop, flight.core());
    }

    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, Flight flight,
                                                double minPower, double maxPower) {
        return Ballistics.calculateFixedArcPower(origin, target, baseSpeed, launchSlope, flight.core(), minPower, maxPower);
    }
}
