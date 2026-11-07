package me.mss1r.siegeworks.client.projectile;

import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.item.SiegeAmmo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

public class TrebuchetProjectileRenderer extends EntityRenderer<TrebuchetProjectile> {
    public TrebuchetProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(TrebuchetProjectile entity, float yaw, float tickDelta, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        BlockState stoneState = SiegeAmmo.projectileBlockState(entity.getTextureName());
        if (stoneState != null) {
            poseStack.pushPose();
            // A load keeps tumbling forward after release, around the axis perpendicular to its flight.
            float heading = Mth.rotLerp(tickDelta, entity.yRotO, entity.getYRot());
            float spin = (entity.tickCount + tickDelta) * TrebuchetProjectile.SPIN_DEGREES_PER_TICK;
            float scale = entity.drawnSize();
            poseStack.mulPose(Axis.YP.rotationDegrees(heading));
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            poseStack.translate(-scale / 2.0F, -scale / 2.0F, -scale / 2.0F);
            poseStack.scale(scale, scale, scale);
            if (SiegeAmmo.isFireAmmoKey(entity.getTextureName())) {
                fillBlockWithFirePot(poseStack);
            }
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    stoneState, poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        super.render(entity, yaw, tickDelta, poseStack, multiBufferSource, light);
    }

    /**
     * The fire pot model is half-size in the lower middle of its block. This scales it to fill the block, so it renders
     * at the same size and position as a stone.
     */
    public static void fillBlockWithFirePot(PoseStack poseStack) {
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    /** Renders a loaded fire pot on a bone the way GeckoLib renders a block, at stone size. */
    public static void renderFirePot(PoseStack poseStack, BlockState state, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(-0.25F, -0.25F, -0.25F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        fillBlockWithFirePot(poseStack);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, bufferSource, packedLight,
                packedOverlay);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TrebuchetProjectile entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
