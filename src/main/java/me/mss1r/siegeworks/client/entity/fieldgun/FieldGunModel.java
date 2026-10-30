package me.mss1r.siegeworks.client.entity.fieldgun;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public final class FieldGunModel<T extends AbstractFieldGunEntity> extends TowedSiegeModel<T> {
    private final ResourceLocation modelResource;
    private final String assetName;
    private final ResourceLocation animationResource;

    public FieldGunModel(String assetName) {
        this.modelResource = MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/" + assetName + ".geo.json");
        this.assetName = assetName;
        this.animationResource = MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/" + assetName + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return modelResource;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID,
                "textures/entity/" + assetName + animatable.finishTextureSuffix() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return animationResource;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        animatable.updateRenderedAim(animationState.getPartialTick());

        getBone("base").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedTowBasePitch())));
        getBone("cannon_aim").ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(animatable.getRenderedModelPitch())));
    }
}
