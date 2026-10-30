package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.GiantCannonProjectile;
import me.mss1r.siegeworks.client.projectile.GiantCannonProjectileModel;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class GiantCannonBallRenderer extends EntityRenderer<GiantCannonProjectile> {
    private final GiantCannonProjectileModel model;

    public GiantCannonBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GiantCannonProjectileModel(GiantCannonProjectileModel.getTexturedModelData().bakeRoot());
    }

    @Override
    public void render(GiantCannonProjectile entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        poseStack.pushPose();
        //? if <1.21 {
        /*model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantCannonProjectile entity) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/block/giant_cannon_ball.png");
    }
}
