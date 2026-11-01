package me.mss1r.siegeworks.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
@EventBusSubscriber(modid="siegeworks", bus=EventBusSubscriber.Bus.GAME, value=Dist.CLIENT)
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
    public static void overlay(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.VEHICLE_HEALTH) && SiegeworksClientGameEvents.hideVehicleHealth()) event.setCanceled(true);
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && SiegeworksClientGameEvents.hideCrosshair()) event.setCanceled(true);
    }
}
