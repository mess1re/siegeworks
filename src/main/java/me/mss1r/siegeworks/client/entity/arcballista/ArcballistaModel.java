package me.mss1r.siegeworks.client.entity.arcballista;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class ArcballistaModel extends TowedSiegeModel<ArcballistaEntity> {
    @Override
    public ResourceLocation getModelResource(ArcballistaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/arcballista.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ArcballistaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/arcballista" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ArcballistaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/arcballista.animation.json");
    }

    @Override
    public void setCustomAnimations(ArcballistaEntity animatable, long instanceId, AnimationState<ArcballistaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        animatable.updateRenderedAim(animationState.getPartialTick());
        getBone("base").ifPresent(bone -> bone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch())));
        getBone("bolt").ifPresent(bone -> bone.setHidden(!animatable.hasAmmoLoaded()));
    }
}
