package me.mss1r.siegeworks.client.projectile;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.client.projectile.CannonBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class CannonBallRenderer extends EntityRenderer<CannonProjectile> {
    private final EntityModel<Entity> model;

    public CannonBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CannonBall(CannonBall.getTexturedModelData().bakeRoot());
    }

    public void render(CannonProjectile entity, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        poseStack.pushPose();
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.armorCutoutNoCull(this.getTextureLocation(entity)));
        //? if <1.21 {
        /*model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        poseStack.popPose();
    }

    public ResourceLocation getTextureLocation(CannonProjectile entity) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/block/cannon_ball.png");
    }
}
