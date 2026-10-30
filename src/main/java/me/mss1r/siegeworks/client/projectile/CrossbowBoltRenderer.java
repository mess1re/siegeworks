package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.projectile.AbstractBoltProjectile;
import me.mss1r.siegeworks.entity.projectile.ArcballistaBoltProjectile;
import me.mss1r.siegeworks.client.projectile.ArcballistaBoltModel;
import me.mss1r.siegeworks.client.projectile.TowerCrossbowBoltModel;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class CrossbowBoltRenderer<T extends AbstractBoltProjectile> extends EntityRenderer<T> {
    private final EntityModel<Entity> towerCrossbowModel;
    private final EntityModel<Entity> arcballistaModel;

    public CrossbowBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.towerCrossbowModel = new TowerCrossbowBoltModel(TowerCrossbowBoltModel.getTexturedModelData().bakeRoot());
        this.arcballistaModel = new ArcballistaBoltModel(ArcballistaBoltModel.getTexturedModelData().bakeRoot());
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.hasTargetAttachment()) {
            return;
        }

        poseStack.pushPose();
        AbstractBoltProjectile.AttachmentRenderState attachment = entity.getAttachmentRenderState(partialTick);
        if (attachment != null) {
            poseStack.translate(attachment.offset().x, attachment.offset().y, attachment.offset().z);
        }
        float renderYaw = attachment != null
                ? attachment.yaw()
                : entity.isEmbedded()
                    ? entity.getRenderYaw()
                    : Mth.rotLerp(partialTick, entity.getRenderYawO(), entity.getRenderYaw());
        float renderPitch = attachment != null
                ? attachment.pitch()
                : entity.isEmbedded()
                    ? entity.getRenderPitch()
                    : Mth.lerp(partialTick, entity.getRenderPitchO(), entity.getRenderPitch());
        renderModel(entity, renderYaw, renderPitch, poseStack, buffer, packedLight);

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    public void renderStuck(T entity, float directionX, float directionY, float directionZ,
                            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float horizontal = Mth.sqrt(directionX * directionX + directionZ * directionZ);
        float renderYaw = (float) (Math.atan2(directionX, directionZ) * Mth.RAD_TO_DEG);
        float renderPitch = (float) (Math.atan2(directionY, horizontal) * Mth.RAD_TO_DEG);
        renderModel(entity, renderYaw, renderPitch, poseStack, buffer, packedLight);
    }

    private void renderModel(T entity, float renderYaw, float renderPitch, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight) {
        poseStack.mulPose(Axis.YP.rotationDegrees(renderYaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(renderPitch));

        EntityModel<Entity> model = entity instanceof ArcballistaBoltProjectile ? arcballistaModel : towerCrossbowModel;
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        //? if <1.21 {
        /*model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        *///?} else {
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        //?}
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        String texture = entity instanceof ArcballistaBoltProjectile ? "arcballista_projectile" : "tower_crossbow_projectile";
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/" + texture + ".png");
    }
}
