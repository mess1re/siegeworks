package me.mss1r.siegeworks.platform.fabric;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
public final class SiegeClientPlatformImpl {
    public static KeyMapping keyMapping(String name, int key, String category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, category);
    }
}
