package me.mss1r.siegeworks.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.client.harness.TowShaftRenderer;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.towing.TowingProfile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
//? if forge {
/*import software.bernie.geckolib.core.animatable.GeoAnimatable;
*///?} else {
import software.bernie.geckolib.animatable.GeoAnimatable;
//?}
import software.bernie.geckolib.model.GeoModel;

public abstract class TowedSiegeRenderer<T extends AbstractSiegeEntity & GeoAnimatable>
        extends SiegeConstructionRenderer<T> {
    protected TowedSiegeRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public boolean shouldShowName(T animatable) {
        return false;
    }

    @Override
    //? if forge {
    /*public void postRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
    *///?} else {
    public void postRender(PoseStack poseStack, T animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, int packedColor) {
    //?}
        //? if forge {
        /*super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        *///?} else {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, packedColor);
        //?}

        if (isReRender) {
            return;
        }

        poseStack.pushPose();
        enterModelSpace(poseStack, animatable, partialTick);
        renderInModelSpace(poseStack, animatable, bufferSource, partialTick, packedLight);
        renderTowShafts(poseStack, animatable, bufferSource, partialTick, packedLight);
        poseStack.popPose();
    }

    protected void renderInModelSpace(PoseStack poseStack, T animatable,
                                      MultiBufferSource bufferSource, float partialTick,
                                      int packedLight) {
    }

    private void enterModelSpace(PoseStack poseStack, T animatable, float partialTick) {
        float ageInTicks = animatable.tickCount + partialTick;
        float bodyRotation = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);

        //? if forge {
        /*applyRotations(animatable, poseStack, ageInTicks, bodyRotation, partialTick);
        *///?} else {
        applyRotations(animatable, poseStack, ageInTicks, bodyRotation, partialTick, animatable.getScale());
        //?}
        poseStack.translate(0.0F, 0.01F, 0.0F);
    }

    private void renderTowShafts(PoseStack poseStack, T animatable,
                                 MultiBufferSource bufferSource, float partialTick, int packedLight) {
        TowingProfile profile = animatable.towingProfile();
        if (profile == null) {
            return;
        }

        var mounts = animatable.getTowingMounts();
        for (int index = 0; index < mounts.size() && index < profile.mountSlots().size(); index++) {
            AbstractHorse mount = mounts.get(index);
            TowingProfile.MountSlot slot = profile.mountSlots().get(index);
            // The animal stands on its own ground level, so the shafts angle up or down to reach it.
            double rise = Mth.lerp(partialTick, mount.yo, mount.getY())
                    - Mth.lerp(partialTick, animatable.yo, animatable.getY());
            Vec3 reach = new Vec3(slot.mountOffset().x, rise, slot.mountOffset().z);
            TowShaftRenderer.render(poseStack, bufferSource, getGeoModel(),
                    slot.leftAnchor(), slot.rightAnchor(), reach,
                    profile.modelTurnDegrees(), mount, packedLight);
        }
    }

    @Override
    //? if forge {
    /*protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        applyBaseRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, 1);
    *///?} else {
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick, float nativeScale) {
        applyBaseRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        //?}

        if (animatable.isTowed()) {
            poseStack.mulPose(Axis.YP.rotationDegrees(animatable.towedModelTurnDegrees()));
        }
    }

    protected void applyBaseRotations(T animatable, PoseStack poseStack, float ageInTicks,
                                      float rotationYaw, float partialTick, float nativeScale) {
        //? if forge {
        /*super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
        *///?} else {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        //?}
    }
}
