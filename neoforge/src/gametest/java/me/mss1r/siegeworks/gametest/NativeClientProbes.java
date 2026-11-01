package me.mss1r.siegeworks.gametest;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class NativeClientProbes {
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        AimClientProbe.tick();
        BannerClientProbe.tick();
    }

    @SubscribeEvent public static void frameStart(RenderFrameEvent.Pre event) { beforeFrame(); }
    @SubscribeEvent public static void frameEnd(RenderFrameEvent.Post event) { afterFrame(); }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void health(RenderGuiLayerEvent.Pre event) {
        AimClientProbe.vehicleHealth(event.getName().equals(VanillaGuiLayers.VEHICLE_HEALTH), event.isCanceled());
    }

    private static void beforeFrame() {
        ManualAimFrameProbe.beforeRenderFrame();
        DraftTeamClientProbe.beforeRenderFrame();
    }

    private static void afterFrame() {
        AimClientProbe.frame();
        ManualAimFrameProbe.afterRenderFrame();
        DraftTeamClientProbe.afterRenderFrame();
    }

    @SubscribeEvent
    public static void rendered(RenderLivingEvent.Post<?, ?> event) {
        DraftTeamClientProbe.rendered(event.getEntity(), event.getRenderer().getModel(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void geometry(GeoRenderEvent.Entity.Post event) {
        ManualAimFrameProbe.recordModel(event.getEntity());
    }
}
