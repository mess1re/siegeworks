package me.mss1r.siegeworks.testmixin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(FireBlock.class)
public interface FireBlockInvoker {
    @Invoker("getBurnOdds") int siegeworks$burnOdds(BlockState state);
    @Invoker("getIgniteOdds") int siegeworks$spreadOdds(BlockState state);
    @Invoker("checkBurnOut") void siegeworks$ignite(Level level, BlockPos pos, int chance, RandomSource random, int age);
}
