package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.fabric.FabricPotState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method="getBurnOdds", at=@At("HEAD"), cancellable=true)
    private void siegeworks$burnOdds(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hasProperty(FabricPotState.HAS_FUEL))
            cir.setReturnValue(state.getValue(FabricPotState.HAS_FUEL) ? IncendiaryPotBlock.FLAMMABILITY : 0);
    }
    @Inject(method="getIgniteOdds(Lnet/minecraft/world/level/block/state/BlockState;)I", at=@At("HEAD"), cancellable=true)
    private void siegeworks$spreadOdds(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hasProperty(FabricPotState.HAS_FUEL))
            cir.setReturnValue(state.getValue(FabricPotState.HAS_FUEL) ? IncendiaryPotBlock.FIRE_SPREAD_SPEED : 0);
    }
    @Inject(method="checkBurnOut", at=@At("HEAD"), cancellable=true)
    private void siegeworks$ignitePot(Level level, BlockPos pos, int chance, RandomSource random, int age, CallbackInfo ci) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof IncendiaryPotBlock pot) {
            if (random.nextInt(chance) < pot.potFlammability(state, level, pos, null))
                pot.catchFire(state, level, pos, null, null);
            ci.cancel();
        }
    }
}
