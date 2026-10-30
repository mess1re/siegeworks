package me.mss1r.siegeworks.config;

/** Converts user-facing seconds and blocks/second to the simulation's tick units. */
final class EngineConfigUnits {
    private EngineConfigUnits() {
    }

    static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0D);
    }

    static double secondsToChangeSpeed(double speedPerTick, double stepPerTick) {
        if (stepPerTick <= 0.0D) {
            return -1.0D;
        }
        return speedPerTick / stepPerTick / 20.0D;
    }

    static double speedStep(double speedPerSecond, double seconds) {
        if (seconds < 0.0D) {
            return 0.0D;
        }
        if (seconds == 0.0D) {
            // Also stop an engine that is still rolling after its configured speed was reduced to zero.
            return Double.MAX_VALUE;
        }
        return speedPerSecond / 20.0D / (seconds * 20.0D);
    }
}
