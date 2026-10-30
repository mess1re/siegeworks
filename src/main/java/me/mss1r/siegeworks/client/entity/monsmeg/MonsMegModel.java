package me.mss1r.siegeworks.client.entity.monsmeg;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class MonsMegModel extends TowedSiegeModel<MonsMegEntity> {
    @Override
    public ResourceLocation getModelResource(MonsMegEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/mons_meg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MonsMegEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/mons_meg" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MonsMegEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/mons_meg.animation.json");
    }

    @Override
    public void setCustomAnimations(MonsMegEntity animatable, long instanceId, AnimationState<MonsMegEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        animatable.updateRenderedAim(animationState.getPartialTick());
        getBone("aim_base").ifPresent(geoBone ->
                geoBone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch())));

        if (animatable.getCooldown() == 0) {
            float wheelRotation = getWheelTravelDirection(animatable) * animatable.getWheelRotation();
            setWheelRotation("wheels_1", wheelRotation);
            setWheelRotation("wheels_2", wheelRotation);
        }
    }
}
