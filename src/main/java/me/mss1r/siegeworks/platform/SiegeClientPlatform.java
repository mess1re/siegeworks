package me.mss1r.siegeworks.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;

public final class SiegeClientPlatform {
    private SiegeClientPlatform() {}
    @ExpectPlatform public static void returnToMount(java.util.UUID player, java.util.UUID group) { throw new AssertionError(); }
    @ExpectPlatform public static KeyMapping keyMapping(String name, int key, String category) { throw new AssertionError(); }
}
