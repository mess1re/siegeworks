package me.mss1r.siegeworks.platform.neoforge;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class SiegeClientPlatformImpl {
    public static KeyMapping keyMapping(String name, int key, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, category);
    }
}
