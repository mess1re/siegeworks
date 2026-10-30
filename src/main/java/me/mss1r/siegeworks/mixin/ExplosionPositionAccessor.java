package me.mss1r.siegeworks.mixin;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Explosion.class)
public interface ExplosionPositionAccessor {
    @Accessor("x") double siegeworks$x();
    @Accessor("y") double siegeworks$y();
    @Accessor("z") double siegeworks$z();
}
