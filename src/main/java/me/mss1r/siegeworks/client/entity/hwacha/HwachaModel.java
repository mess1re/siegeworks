package me.mss1r.siegeworks.client.entity.hwacha;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class HwachaModel extends TowedSiegeModel<HwachaEntity> {
    public static final String NORMAL_AMMUNITION_BONE_PREFIX = "ammunition_normal_";
    public static final String EXPLOSIVE_AMMUNITION_BONE_PREFIX = "ammunition_explosive_";

    @Override
    public ResourceLocation getModelResource(HwachaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/hwacha.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HwachaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/hwacha" + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(HwachaEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/hwacha.animation.json");
    }

    @Override
    public void setCustomAnimations(HwachaEntity animatable, long instanceId,
                                    AnimationState<HwachaEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        animatable.updateRenderedAim(animationState.getPartialTick());
        getBone("base").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch())));
        int firstVisible = animatable.isFiring() ? Math.max(0, animatable.getFiringIndex()) : 0;
        for (int slot = 0; slot < HwachaEntity.CAPACITY; slot++) {
            boolean visible = slot >= firstVisible && slot < animatable.getLoadedCount();
            boolean explosive = visible && animatable.isExplosive(slot);
            getBone(NORMAL_AMMUNITION_BONE_PREFIX + slot).ifPresent(bone ->
                    bone.setHidden(!visible || explosive));
            getBone(EXPLOSIVE_AMMUNITION_BONE_PREFIX + slot).ifPresent(bone ->
                    bone.setHidden(!explosive));
        }
    }
}
