package me.mss1r.siegeworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class SiegeworksKeyMappings {
    public static final KeyMapping FREE_LOOK = me.mss1r.siegeworks.platform.SiegeClientPlatform.keyMapping(
            "key.siegeworks.free_look",
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.siegeworks"
    );
    public static final KeyMapping BARREL_SIGHT = me.mss1r.siegeworks.platform.SiegeClientPlatform.keyMapping(
            "key.siegeworks.barrel_sight",
            GLFW.GLFW_KEY_V,
            "key.categories.siegeworks"
    );

    private SiegeworksKeyMappings() {
    }
}
