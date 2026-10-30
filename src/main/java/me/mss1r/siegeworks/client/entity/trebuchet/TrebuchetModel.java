package me.mss1r.siegeworks.client.entity.trebuchet;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}
import software.bernie.geckolib.model.GeoModel;

public class TrebuchetModel extends GeoModel<TrebuchetEntity> {
    @Override
    public ResourceLocation getModelResource(TrebuchetEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/trebuchet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TrebuchetEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/trebuchet" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TrebuchetEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/trebuchet.animation.json");
    }

    @Override
    public void setCustomAnimations(TrebuchetEntity animatable, long instanceId, AnimationState<TrebuchetEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        String ammo = animatable.getAmmoLoaded();
        getBone("projectile").ifPresent(geoBone -> geoBone.setHidden(
                !SiegeAmmo.isStoneAmmoKey(ammo) && !SiegeAmmo.isGrapeshotAmmoKey(ammo)));
    }
}
