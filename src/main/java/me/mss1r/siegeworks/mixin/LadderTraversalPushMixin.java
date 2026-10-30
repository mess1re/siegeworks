package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LadderTraversalPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void siegeworks$preserveLadderSpacing(Entity other, CallbackInfo ci) {
        // A staged recruit must not be shoved off a parapet while its turn is pending either.
        if (LadderTraversalControl.ownsNavigation((Entity) (Object) this)
                || LadderTraversalControl.ownsNavigation(other)) ci.cancel();
    }
}
