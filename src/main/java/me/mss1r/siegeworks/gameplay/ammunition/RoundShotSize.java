package me.mss1r.siegeworks.gameplay.ammunition;

/** Display dimensions in model pixels. Physical projectile dimensions remain in its physics profile. */
public final class RoundShotSize {
    public static final int MIN_CALIBER = 5;
    public static final int MAX_CALIBER = 490;
    public static final int STEP = 5;

    private RoundShotSize() {}

    public static boolean valid(int caliber) {
        return caliber >= MIN_CALIBER && caliber <= MAX_CALIBER && caliber % STEP == 0;
    }

    public static int stackLimit(int caliber) {
        if (caliber <= 100) return 16;
        if (caliber <= 160) return 8;
        if (caliber <= 300) return 4;
        return 2;
    }

    public static double fraction(int caliber) {
        return Math.max(0, Math.min(1, (caliber - MIN_CALIBER) / (double) (MAX_CALIBER - MIN_CALIBER)));
    }

    public static double scale(int caliber, int count) {
        double nativeDiameter = count == 1 ? 10 : 8;
        return (3 + (nativeDiameter - 3) * fraction(caliber)) / nativeDiameter;
    }
}
