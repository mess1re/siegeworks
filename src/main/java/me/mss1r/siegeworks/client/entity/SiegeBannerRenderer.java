package me.mss1r.siegeworks.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
/*import net.minecraft.world.level.block.entity.BannerBlockEntity;
*///?} else {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
//?}

public final class SiegeBannerRenderer {
    private SiegeBannerRenderer() {}

    /** Draws the vanilla patterned cloth in the caller's attachment transform. */
    public static void render(Entity owner, ItemStack stack, int slot, ModelPart flag,
                              PoseStack poseStack, MultiBufferSource buffers, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BannerItem banner)) return;
        long offset = owner.blockPosition().getX() * 7L + owner.blockPosition().getY() * 9L
                + owner.blockPosition().getZ() * 13L + slot * 17L;
        float phase = (Math.floorMod(offset + owner.level().getGameTime(), 100L) + partialTick) / 100.0F;
        flag.x = flag.y = flag.z = 0.0F;
        flag.xRot = (-0.0125F + 0.01F * Mth.cos(Mth.TWO_PI * phase)) * Mth.PI;
        flag.yRot = flag.zRot = 0.0F;
        //? if <1.21 {
        /*BannerRenderer.renderPatterns(poseStack, buffers, packedLight, packedOverlay, flag,
                ModelBakery.BANNER_BASE, true,
                BannerBlockEntity.createPatterns(banner.getColor(), BannerBlockEntity.getItemPatterns(stack)));
        *///?} else {
        BannerRenderer.renderPatterns(poseStack, buffers, packedLight, packedOverlay, flag,
                ModelBakery.BANNER_BASE, true, banner.getColor(),
                stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
        //?}
    }

    /** Vanilla wall-banner parts, with the cloth's top edge as the attachment origin. */
    public static void renderWallBanner(Entity owner, ItemStack stack, int slot, ModelPart flag, ModelPart bar,
                                        PoseStack poseStack, MultiBufferSource buffers, float partialTick,
                                        int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BannerItem)) return;
        bar.x = bar.z = 0.0F;
        bar.y = 32.0F;
        bar.xRot = bar.yRot = bar.zRot = 0.0F;
        bar.render(poseStack, ModelBakery.BANNER_BASE.buffer(buffers, RenderType::entitySolid),
                packedLight, packedOverlay);
        render(owner, stack, slot, flag, poseStack, buffers, partialTick, packedLight, packedOverlay);
    }
}
