package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LadderTraversalMovementMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void siegeworks$leaveGuidedMovementAlone(Vec3 input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (LadderTraversalControl.controls(entity)) {
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0;
            ci.cancel();
        }
    }
}
