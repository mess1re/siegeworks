package me.mss1r.siegeworks.client.entity.arcballista;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ArcballistaRenderer extends TowedSiegeRenderer<ArcballistaEntity> {
    public ArcballistaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ArcballistaModel());
    }

    @Override
    protected void applyBaseRotations(ArcballistaEntity animatable, PoseStack poseStack,
                                      float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        animatable.updateRenderedAim(partialTick);
        float yaw = animatable.getRenderedAimYaw();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw - 180.0F));
    }
}
