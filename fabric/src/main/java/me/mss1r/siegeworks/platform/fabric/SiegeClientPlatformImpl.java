package me.mss1r.siegeworks.platform.fabric;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
public final class SiegeClientPlatformImpl {
    public static void returnToMount(java.util.UUID player, java.util.UUID group) {
        RecruitsBridge.returnToMount(player, group);
    }
    private static final class RecruitsBridge {
        public static void returnToMount(java.util.UUID player, java.util.UUID group) {
            com.talhanation.recruits.network.RecruitsNetwork.sendToServer(new com.talhanation.recruits.network.MessageBackToMountEntity(player, group));
        }
    }
    public static KeyMapping keyMapping(String name, int key, String category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, category);
    }
}
