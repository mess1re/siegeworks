package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Level level;
    //? if <1.21 {
    /*@Inject(method="finalizeExplosion", at=@At("HEAD"))
    private void siegeworks$potChainReaction(boolean particles, CallbackInfo ci) {
        var explosion = (Explosion)(Object)this;
        var positions = explosion.getToBlow().iterator();
        while (positions.hasNext()) {
            var pos = positions.next();
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof IncendiaryPotBlock pot && pot.handleExplosion(state, level, pos, explosion))
                positions.remove();
        }
    }
    *///?}
}
