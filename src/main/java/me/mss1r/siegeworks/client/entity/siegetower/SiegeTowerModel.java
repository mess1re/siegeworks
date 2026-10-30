package me.mss1r.siegeworks.client.entity.siegetower;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}
import software.bernie.geckolib.model.GeoModel;

public class SiegeTowerModel extends GeoModel<SiegeTowerEntity> {
    @Override
    public ResourceLocation getModelResource(SiegeTowerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/siege_tower.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SiegeTowerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/siege_tower" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(SiegeTowerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/siege_tower.animation.json");
    }

    @Override
    public void setCustomAnimations(SiegeTowerEntity animatable, long instanceId, AnimationState<SiegeTowerEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        applyBridgeProgress(animatable, animationState.getPartialTick());
        rotateWheelGroup(animatable, "wheels_1");
        rotateWheelGroup(animatable, "wheels_2");
        rotateWheelGroup(animatable, "wheels_3");
        getBone("overlay_bottom").ifPresent(bone -> bone.setHidden(!animatable.hasLeatherBottom()));
        getBone("overlay_top").ifPresent(bone -> bone.setHidden(!animatable.hasLeatherTop()));
        getBone("overlay_top_descent").ifPresent(bone -> bone.setHidden(!animatable.hasLeatherTop()));
    }

    private void applyBridgeProgress(SiegeTowerEntity animatable, float partialTick) {
        float progress = animatable.getRenderedBridgeProgress(partialTick);

        getBone("descent").ifPresent(geoBone -> geoBone.setRotX(
                -animatable.getRenderedBridgeAngleRadians(partialTick)));
        getBone("axis").ifPresent(geoBone -> geoBone.setRotX(Mth.TWO_PI * progress));
    }

    private void rotateWheelGroup(SiegeTowerEntity animatable, String boneName) {
        getBone(boneName).ifPresent(geoBone -> geoBone.setRotX(
                (float) Math.toRadians(animatable.getWheelRotation())));
    }

}
