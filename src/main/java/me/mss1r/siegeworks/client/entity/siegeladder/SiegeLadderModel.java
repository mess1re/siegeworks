package me.mss1r.siegeworks.client.entity.siegeladder;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}
import software.bernie.geckolib.model.GeoModel;

public class SiegeLadderModel extends GeoModel<SiegeLadderEntity> {
    @Override
    public ResourceLocation getModelResource(SiegeLadderEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "geo/siege_ladder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SiegeLadderEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/siege_ladder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SiegeLadderEntity animatable) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, "animations/siege_ladder.animation.json");
    }

    @Override
    public void setCustomAnimations(SiegeLadderEntity animatable, long instanceId, AnimationState<SiegeLadderEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        getBone("siege_ladder").ifPresent(geoBone ->
                geoBone.setRotX((float) Math.toRadians(
                        -animatable.getRenderedLeanAngleDegrees(animationState.getPartialTick()))));

        // While building, every section is shown; the construction renderer draws the unbuilt ones as ghosts.
        int sections = animatable.isFullyBuilt() ? animatable.getSections() : SiegeLadderEntity.MAX_SECTIONS;
        for (int i = 1; i <= SiegeLadderEntity.MAX_SECTIONS; i++) {
            int sectionIndex = i;
            getBone("section_" + sectionIndex).ifPresent(geoBone -> geoBone.setHidden(sectionIndex > sections));
        }
    }
}
