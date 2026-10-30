package me.mss1r.siegeworks.client.entity.towercrossbow;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}
import software.bernie.geckolib.model.GeoModel;

public class TowerCrossbowModel extends GeoModel<TowerCrossbowEntity> {
    @Override
    public ResourceLocation getModelResource(TowerCrossbowEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/tower_crossbow.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TowerCrossbowEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/tower_crossbow" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TowerCrossbowEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/tower_crossbow.animation.json");
    }

    @Override
    public void setCustomAnimations(TowerCrossbowEntity animatable, long instanceId, AnimationState<TowerCrossbowEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        animatable.updateRenderedAim(animationState.getPartialTick());
        getBone("engine").ifPresent(bone -> {
            float turretYaw = Mth.wrapDegrees(animatable.getYRot() - animatable.getRenderedAimYaw());
            bone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch()));
            bone.setRotY((float) Math.toRadians(turretYaw));
        });
        getBone("bolt").ifPresent(bone -> bone.setHidden(!animatable.hasAmmoLoaded()));
    }
}
