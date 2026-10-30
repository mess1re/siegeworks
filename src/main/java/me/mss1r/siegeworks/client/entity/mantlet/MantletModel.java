package me.mss1r.siegeworks.client.entity.mantlet;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class MantletModel extends TowedSiegeModel<MantletEntity> {
    @Override
    public ResourceLocation getModelResource(MantletEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/mantlet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MantletEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/mantlet" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MantletEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/mantlet.animation.json");
    }

    @Override
    public void setCustomAnimations(MantletEntity animatable, long instanceId, AnimationState<MantletEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        float partialTick = animationState.getPartialTick();
        getBone("base").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedBodyPitch(partialTick))));
        getBone("animation").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedFlapAngle(partialTick))));
    }
}
