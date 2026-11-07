package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.client.firearm.FirearmClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.entity.PatchedLivingEntityRenderer;

final class NativeEpicFightClient {
    private NativeEpicFightClient() {}
    @SuppressWarnings({"rawtypes", "unchecked"})
    static void register(IEventBus bus) {
        EpicFightClientEventHooks.Render.VALIDATE_PLAYER_MODEL_TO_RENDER.registerEvent(event -> {
            if (EpicSiegePlayerModel.shouldUseVanillaRenderer(event.getPlayerPatch().getOriginal(),
                    Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))) {
                event.setShouldRender(false);
            }
        }, "siegeworks:crew_model");
        EpicFightClientEventHooks.Render.PREPARE_MODEL_TO_RENDER.registerEvent(event ->
                EpicLadderCarryPose.apply(event.getEntityPatch(), event.getPartialTicks()), "siegeworks:ladder_carry");
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            var engine = RenderEngine.getInstance();
            if (event.get(EntityType.PLAYER) instanceof PatchedLivingEntityRenderer renderer) {
                renderer.addPatchedLayerAlways(PlayerItemInHandLayer.class, new EpicFirearmLayer<>(engine::getItemRenderer));
            }
            engine.getFirstPersonRenderer().addPatchedLayerAlways(PlayerItemInHandLayer.class,
                    new EpicFirearmLayer<>(engine::getItemRenderer));
        }, "siegeworks:firearm_layer");
        EpicFightClientEventHooks.Camera.COUPLE_CAMERA.registerEvent(event -> {
            var player = Minecraft.getInstance().player;
            if (player != null && FirearmClient.aiming(player)) event.setShouldCouple(true);
        }, "siegeworks:firearm_camera");
    }
}
