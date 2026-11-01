package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SiegeMovementMixin {
    @Inject(method = "travelRidden", at = @At("HEAD"), cancellable = true)
    private void siegeworks$keepMovementInTheSiegeTick(Player rider, Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof AbstractSiegeEntity siege) {
            // Siege physics owns movement. Vanilla's remote-rider branch would erase
            // its velocity, including accumulated gravity, before the siege tick runs.
            siege.calculateEntityAnimation(false);
            ci.cancel();
        }
    }
}
