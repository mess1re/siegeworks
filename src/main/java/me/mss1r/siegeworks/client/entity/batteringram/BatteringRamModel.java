package me.mss1r.siegeworks.client.entity.batteringram;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.entity.TowedSiegeModel;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}

public class BatteringRamModel extends TowedSiegeModel<BatteringRamEntity> {
    @Override
    public ResourceLocation getModelResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/battering_ram.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/battering_ram.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BatteringRamEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/battering_ram.animation.json");
    }

    @Override
    protected String leftWheelBone() {
        return "rotate";
    }

    @Override
    protected String rightWheelBone() {
        return "rotate2";
    }

    @Override
    public void setCustomAnimations(BatteringRamEntity animatable, long instanceId,
                                    AnimationState<BatteringRamEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        float direction = getWheelTravelDirection(animatable);
        float recoil = animatable.wheelRecoilDegrees(animationState.getPartialTick());
        setWheelRotation(leftWheelBone(), direction * animatable.getLeftWheelRotation() + recoil);
        setWheelRotation(rightWheelBone(), direction * animatable.getRightWheelRotation() + recoil);
    }
}
