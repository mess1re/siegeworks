package me.mss1r.siegeworks.client.entity.siegeladder;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import me.mss1r.siegeworks.client.entity.SiegeConstructionRenderer;

public class SiegeLadderRenderer extends SiegeConstructionRenderer<SiegeLadderEntity> {
    public SiegeLadderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SiegeLadderModel());
    }

    @Override
    public void render(SiegeLadderEntity entity, float yaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack poses,
                       net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        poses.pushPose();
        var carried = entity.renderedCarryPose(partialTick);
        if (carried != null) {
            // The dispatcher already applied the ladder's interpolated position.
            // Replace that origin with this frame's carrier grip, without moving the entity.
            var delta = carried.foot().subtract(entity.getPosition(partialTick));
            poses.translate(delta.x, delta.y, delta.z);
        }
        super.render(entity, yaw, partialTick, poses, buffers, light);
        poses.popPose();
    }

    @Override
    //? if <1.21 {
    /*protected void applyRotations(SiegeLadderEntity entity, com.mojang.blaze3d.vertex.PoseStack poses,
                                  float age, float yaw, float partialTick) {
        var carried = entity.renderedCarryPose(partialTick);
        super.applyRotations(entity, poses, age, carried == null ? yaw : carried.yaw(), partialTick);
    }
    *///?} else {
    protected void applyRotations(SiegeLadderEntity entity, com.mojang.blaze3d.vertex.PoseStack poses,
                                  float age, float yaw, float partialTick, float scale) {
        var carried = entity.renderedCarryPose(partialTick);
        super.applyRotations(entity, poses, age, carried == null ? yaw : carried.yaw(), partialTick, scale);
    }
    //?}

    @Override
    public boolean shouldShowName(SiegeLadderEntity animatable) {
        return false;
    }

    @Override
    public boolean shouldRender(SiegeLadderEntity animatable, Frustum camera, double camX, double camY, double camZ) {
        double length = (1.0D + animatable.getSections()) * 3.0D;
        return super.shouldRender(animatable, camera, camX, camY, camZ)
                || camera.isVisible(animatable.getBoundingBox().inflate(length, length, length));
    }
}
