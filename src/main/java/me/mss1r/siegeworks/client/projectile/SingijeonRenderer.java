package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.client.projectile.SingijeonModel;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SingijeonRenderer extends EntityRenderer<SingijeonProjectile> {
    private static final ResourceLocation NORMAL_TEXTURE = texture("singijeon");
    private static final ResourceLocation EXPLOSIVE_TEXTURE = texture("explosive_singijeon");
    private final SingijeonModel model = new SingijeonModel(SingijeonModel.createBodyLayer().bakeRoot());

    public SingijeonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SingijeonProjectile entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float renderYaw = Mth.rotLerp(partialTick, entity.getPreviousRenderYaw(), entity.getYRot());
        float renderPitch = Mth.lerp(partialTick, entity.getPreviousRenderPitch(), entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(renderYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderPitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        //? if <1.21 {
        /*model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SingijeonProjectile entity) {
        return entity.isExplosive() ? EXPLOSIVE_TEXTURE : NORMAL_TEXTURE;
    }

    private static ResourceLocation texture(String name) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/" + name + ".png");
    }
}
