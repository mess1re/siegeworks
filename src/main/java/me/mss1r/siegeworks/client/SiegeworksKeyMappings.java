package me.mss1r.siegeworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if forge {
/*import net.minecraftforge.client.settings.KeyConflictContext;
*///?} else {
import net.neoforged.neoforge.client.settings.KeyConflictContext;
//?}
import org.lwjgl.glfw.GLFW;

public final class SiegeworksKeyMappings {
    public static final KeyMapping FREE_LOOK = new KeyMapping(
            "key.siegeworks.free_look",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.siegeworks"
    );
    public static final KeyMapping BARREL_SIGHT = new KeyMapping(
            "key.siegeworks.barrel_sight",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.siegeworks"
    );

    private SiegeworksKeyMappings() {
    }
}
