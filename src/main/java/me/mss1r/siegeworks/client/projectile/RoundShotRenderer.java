package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.axiomata.ballistics.BallisticProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the same baked round-shot geometry and material as placed ammunition. */
public final class RoundShotRenderer<T extends BallisticProjectile> extends EntityRenderer<T> {
    private final BlockState modelState;

    public RoundShotRenderer(EntityRendererProvider.Context context, BlockState modelState) {
        super(context);
        this.modelState = modelState;
    }

    @Override
    public void render(T projectile, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float scale = (float) (projectile.getPhysicsProfile().diameterOf(projectile) * 16 / 10);
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -5.0 / 16, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                modelState, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(projectile, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T projectile) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
