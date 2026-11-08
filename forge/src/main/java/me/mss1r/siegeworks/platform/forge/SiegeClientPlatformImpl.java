package me.mss1r.siegeworks.platform.forge;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class SiegeClientPlatformImpl {
    public static KeyMapping keyMapping(String name, int key, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, category);
    }
}
