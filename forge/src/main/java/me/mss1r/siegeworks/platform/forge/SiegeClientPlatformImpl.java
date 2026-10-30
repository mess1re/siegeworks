package me.mss1r.siegeworks.platform.forge;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.KeyConflictContext;

public final class SiegeClientPlatformImpl {
    public static void returnToMount(java.util.UUID player, java.util.UUID group) {
        RecruitsBridge.returnToMount(player, group);
    }
    private static final class RecruitsBridge {
        public static void returnToMount(java.util.UUID player, java.util.UUID group) {
            com.talhanation.recruits.Main.SIMPLE_CHANNEL.sendToServer(new com.talhanation.recruits.network.MessageBackToMountEntity(player, group));
        }
    }
    public static KeyMapping keyMapping(String name, int key, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, category);
    }
}
