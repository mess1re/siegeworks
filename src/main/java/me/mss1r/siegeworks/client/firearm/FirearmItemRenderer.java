package me.mss1r.siegeworks.client.firearm;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.item.FirearmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class FirearmItemRenderer extends BlockEntityWithoutLevelRenderer {
    public FirearmItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        if (!(stack.getItem() instanceof FirearmItem item)) return;
        pose.pushPose();
        pose.translate(.5, .5, .5);
        if (context == ItemDisplayContext.GUI) {
            FirearmMesh.INSTANCE.renderIcon(item.profile().getPath(),pose,buffers,light,overlay);
            pose.popPose();
            return;
        }
        // ItemRenderer already applied the resource-pack display transform.
        // Dropped meshes use their bottom centre, independent of authored weapon pivots.
        if (context == ItemDisplayContext.GROUND) FirearmMesh.INSTANCE.alignGround(item.profile().getPath(), pose);
        FirearmMesh.INSTANCE.render(item.profile().getPath(), pose, buffers, light, overlay);
        pose.popPose();
    }
}
