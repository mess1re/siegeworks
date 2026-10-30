package me.mss1r.siegeworks.gametest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Shares the native client regression scenarios without shipping them in the mod. */
public final class FabricClientProbe implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AimClientProbe.tick();
            BannerClientProbe.tick();
        });
        WorldRenderEvents.END.register(context -> AimClientProbe.frame());
    }
}
