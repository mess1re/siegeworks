package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.client.firearm.FirearmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.api.client.forgeevent.PrepareModelEvent;
import yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;

final class NativeEpicFightClient {
    private NativeEpicFightClient() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void register(IEventBus bus) {
        MinecraftForge.EVENT_BUS.addListener((RenderEpicFightPlayerEvent event) -> {
            if (EpicSiegePlayerModel.shouldUseVanillaRenderer(event.getPlayerPatch().getOriginal(),
                    Minecraft.getInstance().getFrameTime())) {
                event.setShouldRender(false);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PrepareModelEvent event) ->
                EpicLadderCarryPose.apply(event.getEntityPatch(), event.getPartialTicks()));
        bus.addListener((PatchedRenderersEvent.Modify event) -> {
            var engine = ClientEngine.getInstance().renderEngine;
            if (event.get(EntityType.PLAYER) instanceof PatchedLivingEntityRenderer renderer) {
                renderer.addPatchedLayerAlways(PlayerItemInHandLayer.class, new EpicFirearmLayer<>(engine::getItemRenderer));
            }
            engine.getFirstPersonRenderer().addPatchedLayerAlways(PlayerItemInHandLayer.class,
                    new EpicFirearmLayer<>(engine::getItemRenderer));
        });
        EpicFightClientHooks.Camera.COUPLE_CAMERA.registerEvent(event -> {
            var player = Minecraft.getInstance().player;
            if (player != null && FirearmClient.aiming(player)) event.setShouldCouple(true);
        }, "siegeworks:firearm_camera", "siegeworks:firearm_camera".hashCode());
    }
}
