package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.ScattershotProjectile;
import me.mss1r.siegeworks.client.projectile.ScattershotProjectileModel;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ScattershotProjectileRenderer extends EntityRenderer<ScattershotProjectile> {
    private static final float VISUAL_SCALE = 2.5F;

    private static final ResourceLocation IRON_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/scattershot_iron.png");
    private static final ResourceLocation STONE_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/scattershot_stone.png");

    private final ScattershotProjectileModel model;

    public ScattershotProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ScattershotProjectileModel(ScattershotProjectileModel.createBodyLayer().bakeRoot());
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ScattershotProjectile entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        //? if <1.21 {
        /*model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ScattershotProjectile entity) {
        return entity.isStonePellet() ? STONE_TEXTURE : IRON_TEXTURE;
    }
}
