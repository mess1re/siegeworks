package me.mss1r.siegeworks.gametest;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class NativeClientProbes {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        AimClientProbe.tick();
        BannerClientProbe.tick();
    }

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) beforeFrame();
        else afterFrame();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void health(RenderGuiOverlayEvent.Pre event) {
        AimClientProbe.vehicleHealth(event.getOverlay().id().equals(VanillaGuiOverlay.MOUNT_HEALTH.id()), event.isCanceled());
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
