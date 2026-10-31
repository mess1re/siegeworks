package me.mss1r.siegeworks.client.entity.towercrossbow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import me.mss1r.siegeworks.client.entity.SiegeConstructionRenderer;

public class TowerCrossbowRenderer extends SiegeConstructionRenderer<TowerCrossbowEntity> {
    public TowerCrossbowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TowerCrossbowModel());
    }

    @Override
    public boolean shouldShowName(TowerCrossbowEntity animatable) {
        return false;
    }

    @Override
    //? if forge {
    /*protected void applyRotations(TowerCrossbowEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
    *///?} else {
    protected void applyRotations(TowerCrossbowEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        //?}
        float yaw = Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw - 180.0F));
    }
}
