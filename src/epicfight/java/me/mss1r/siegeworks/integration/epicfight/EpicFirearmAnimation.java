package me.mss1r.siegeworks.integration.epicfight;

import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.DirectStaticAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.api.client.animation.AnimationSubFileReader.PovSettings;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMask;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Map;

/** A native EF animation: articulated player joints, shared authoritative firearm timing. */
public final class EpicFirearmAnimation extends StaticAnimation {
    private final AnimationAccessor<DirectStaticAnimation> viewmodel;

    public EpicFirearmAnimation(AnimationAccessor<? extends StaticAnimation> accessor, boolean aim,
                                AnimationAccessor<DirectStaticAnimation> viewmodel) {
        super(.15F, true, accessor, Armatures.BIPED);
        this.viewmodel = viewmodel;
        addProperty(ClientAnimationProperties.LAYER_TYPE, Layer.LayerType.COMPOSITE_LAYER);
        addProperty(ClientAnimationProperties.PRIORITY, Layer.Priority.MIDDLE);
        addProperty(ClientAnimationProperties.POV_SETTINGS, new PovSettings(null,
                Map.of("leftArm", true, "rightArm", true, "leftSleeve", true, "rightSleeve", true),
                PovSettings.RootTransformation.CAMERA, null, false, false, false));
        var joints = new java.util.ArrayList<>(java.util.List.of(JointMask.of("Chest"), JointMask.of("Head"),
                JointMask.of("Shoulder_R"), JointMask.of("Arm_R"), JointMask.of("Hand_R"),
                JointMask.of("Elbow_R"), JointMask.of("Tool_R"), JointMask.of("Shoulder_L"),
                JointMask.of("Arm_L"), JointMask.of("Hand_L"), JointMask.of("Elbow_L"), JointMask.of("Tool_L")));
        if (aim) {
            joints.add(JointMask.of("Root"));
            joints.add(JointMask.of("Torso"));
        }
        addProperty(ClientAnimationProperties.JOINT_MASK, JointMaskEntry.builder().defaultMask(
                JointMask.JointMaskSet.of(joints.toArray(JointMask[]::new))).create());
    }

    @Override public void postInit() {
        super.postInit();
        addProperty(ClientAnimationProperties.POV_ANIMATION, viewmodel.get());
    }

    @Override public void loadAnimation() {
        animationClip = new AnimationClip();
        animationClip.setClipTime(1);
    }

    @Override public Pose getPoseByTime(LivingEntityPatch<?> patch, float time, float partialTick) {
        return patch.getOriginal().level().isClientSide
                ? EpicFirearmRig.sample(patch, partialTick).pose() : new Pose();
    }

    static DirectStaticAnimation viewmodel(AnimationAccessor<DirectStaticAnimation> accessor) {
        int registeredId = accessor.id();
        return new DirectStaticAnimation(.15F, true, accessor.registryName(), Armatures.BIPED) {
            @Override public int getId() { return registeredId; }

            @Override public int id() { return registeredId; }

            @Override public void loadAnimation() {
                animationClip = new AnimationClip();
                animationClip.setClipTime(1);
            }

            @Override public Pose getPoseByTime(LivingEntityPatch<?> patch, float time, float partialTick) {
                return EpicFirearmRig.sampleView(patch, partialTick).pose();
            }
        };
    }
}
