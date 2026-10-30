package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.update.SiegeUpdateNotice;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.VersionChecker;

public final class SiegeUpdateNotifier {
    private static final SiegeUpdateNotice NOTICE = new SiegeUpdateNotice();
    private static int ticksUntilCheck = 100;

    private SiegeUpdateNotifier() {
    }

    public static void tick(Minecraft minecraft) {
        if (NOTICE.finished() || minecraft.player == null || minecraft.screen != null
                || !SiegeworksClientConfig.showUpdateNotice()) {
            return;
        }
        if (--ticksUntilCheck > 0) {
            return;
        }
        ticksUntilCheck = 100;
        ModList.get().getModContainerById(Siegeworks.MOD_ID).ifPresent(container ->
                NOTICE.take(VersionChecker.getResult(container.getModInfo()))
                        .ifPresent(message -> minecraft.gui.getChat().addMessage(message)));
    }
}
