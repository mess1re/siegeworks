package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class StoneCannonBallRenderer extends EntityRenderer<CannonProjectile> {
    private final GiantCannonProjectileModel model;

    public StoneCannonBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new GiantCannonProjectileModel(GiantCannonProjectileModel.getTexturedModelData().bakeRoot());
    }

    @Override
    public void render(CannonProjectile entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(0.25F, 0.25F, 0.25F);
        var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        //? if <1.21 {
        /*model.renderToBuffer(pose, vertices, light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        *///?} else {
        model.renderToBuffer(pose, vertices, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CannonProjectile entity) {
        return MinecraftVersionCompat.id("siegeworks", "textures/block/giant_cannon_ball.png");
    }
}
