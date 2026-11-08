package me.mss1r.siegeworks.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;

public final class SiegeClientPlatform {
    private SiegeClientPlatform() {}
    @ExpectPlatform public static KeyMapping keyMapping(String name, int key, String category) { throw new AssertionError(); }
}
