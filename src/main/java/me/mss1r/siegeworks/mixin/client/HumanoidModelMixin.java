package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.client.ladder.LadderBraceClient;
import me.mss1r.siegeworks.client.ladder.LadderCarryClient;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.client.pose.ResolvedSiegePose;
import me.mss1r.siegeworks.client.pose.SiegePoseApplicator;
import me.mss1r.siegeworks.client.pose.SiegePoseRenderContext;
import me.mss1r.siegeworks.client.pose.SiegePoseResolver;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Unique
    private boolean siegeworks$hasAppliedPose;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void siegeworks$resetPose(T entity, float limbAngle, float limbDistance, float age,
                                     float headYaw, float headPitch, CallbackInfo ci) {
        if (siegeworks$hasAppliedPose) {
            SiegePoseApplicator.reset((HumanoidModel<?>) (Object) this);
            siegeworks$hasAppliedPose = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void siegeworks$applySiegePose(T entity, float limbAngle, float limbDistance, float age,
                                          float headYaw, float headPitch, CallbackInfo ci) {
        if (SiegePoseRenderContext.isRenderingFirstPersonHand()) {
            return;
        }
        if (LadderCarry.isCarrying(entity)) {
            LadderCarryClient.raiseArms((HumanoidModel<?>) (Object) this);
            return;
        }
        if (LadderBraceClient.isBracing(entity)) {
            LadderBraceClient.pose((HumanoidModel<?>) (Object) this, entity, headYaw, headPitch);
            siegeworks$hasAppliedPose = true;
            return;
        }
        AbstractSiegeEntity siege = entity.getVehicle() instanceof AbstractSiegeEntity mountedSiege
                ? mountedSiege
                : null;

        if (siege == null && me.mss1r.siegeworks.client.firearm.FirearmAnimation.INSTANCE.pose(
                (HumanoidModel<?>) (Object) this, entity, age, headPitch)) {
            siegeworks$hasAppliedPose = true;
            return;
        }

        ResolvedSiegePose pose = SiegePoseResolver.resolve(entity, siege, age);
        if (pose != null) {
            SiegePoseApplicator.apply(
                    (HumanoidModel<?>) (Object) this,
                    entity,
                    siege,
                    pose,
                    age,
                    headYaw,
                    headPitch
            );
            siegeworks$hasAppliedPose = true;
        }
    }
}
