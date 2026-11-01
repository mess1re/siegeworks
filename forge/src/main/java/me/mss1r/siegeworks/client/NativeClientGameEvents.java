package me.mss1r.siegeworks.client;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
@EventBusSubscriber(modid="siegeworks", bus=EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
public final class NativeClientGameEvents {
    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer player)
            SiegeworksClientGameEvents.updateMovement(player, event.getInput());
    }
    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        if (SiegeworksClientGameEvents.hideHands()) event.setCanceled(true);
    }
    @SubscribeEvent
    public static void overlay(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.MOUNT_HEALTH.id()) && SiegeworksClientGameEvents.hideVehicleHealth()) event.setCanceled(true);
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && SiegeworksClientGameEvents.hideCrosshair()) event.setCanceled(true);
    }
}
