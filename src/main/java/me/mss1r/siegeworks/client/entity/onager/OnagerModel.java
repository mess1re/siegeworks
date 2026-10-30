package me.mss1r.siegeworks.client.entity.onager;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class OnagerModel extends TowedSiegeModel<OnagerEntity> {
    @Override
    public ResourceLocation getModelResource(OnagerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/onager.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(OnagerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/onager" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(OnagerEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/onager.animation.json");
    }

    @Override
    public void setCustomAnimations(OnagerEntity animatable, long instanceId, AnimationState<OnagerEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        String ammo = animatable.getAmmoLoaded();
        getBone("load").ifPresent(geoBone -> geoBone.setHidden(
                !SiegeAmmo.isStoneAmmoKey(ammo) && !SiegeAmmo.isGrapeshotAmmoKey(ammo)));
    }
}
