package me.mss1r.siegeworks.client.entity;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
//? if <1.21 {
/*import software.bernie.geckolib.core.animatable.GeoAnimatable;
*///?} else {
import software.bernie.geckolib.animatable.GeoAnimatable;
//?}
//? if <1.21 {
/*import software.bernie.geckolib.core.animation.AnimationState;
*///?} else {
import software.bernie.geckolib.animation.AnimationState;
//?}
import software.bernie.geckolib.model.GeoModel;

public abstract class TowedSiegeModel<T extends AbstractSiegeEntity & GeoAnimatable>
        extends GeoModel<T> {
    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        if (!shouldRotateWheels(animatable)) {
            return;
        }

        float direction = getWheelTravelDirection(animatable);
        setWheelRotation(leftWheelBone(), direction * animatable.getLeftWheelRotation());
        setWheelRotation(rightWheelBone(), direction * animatable.getRightWheelRotation());
    }

    protected String leftWheelBone() {
        return "wheel_1";
    }

    protected String rightWheelBone() {
        return "wheel_2";
    }

    protected boolean shouldRotateWheels(T animatable) {
        return true;
    }

    protected final float getWheelTravelDirection(T animatable) {
        return animatable.isTowed() ? -1.0F : 1.0F;
    }

    protected final void setWheelRotation(String boneName, float rotationDegrees) {
        getBone(boneName).ifPresent(bone ->
                bone.setRotX((float) Math.toRadians(rotationDegrees)));
    }
}
