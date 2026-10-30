package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class LadderTraversalNavigationMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void siegeworks$runOnlyTheReservedLadderNavigation(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!LadderTraversalControl.ownsNavigation(mob)) return;
        // Keep ordinary path following, steering and jumping to reach the staging slot, but do
        // not let a wander/attack/follow goal overwrite that path halfway through this tick.
        mob.getSensing().tick();
        mob.getNavigation().tick();
        mob.getMoveControl().tick();
        mob.getLookControl().tick();
        mob.getJumpControl().tick();
        ci.cancel();
    }
}
