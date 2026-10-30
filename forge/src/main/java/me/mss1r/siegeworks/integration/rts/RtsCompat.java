package me.mss1r.siegeworks.integration.rts;

public final class RtsCompat {
    private RtsCompat() {
    }

    public static void register() {
        RtsMapObjects.register();
        RtsFireZones.register();
    }
}
