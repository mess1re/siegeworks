package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
public abstract class DraftMountAnimationMixin {
    // Onager passengers sit in its bucket, but hitched animals must retain their standing gait.
    // Vanilla repeats the passenger check for the pose, body yaw and walking animation.
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"))
    private boolean siegeworks$standWhileDrawingEngine(LivingEntity entity) {
        if (entity instanceof AbstractHorse mount
                && mount.getVehicle() instanceof AbstractSiegeEntity siege && siege.isTowed()) {
            return false;
        }
        return entity.isPassenger();
    }
}
