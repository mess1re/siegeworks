package me.mss1r.siegeworks.fabric.mixin;

import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.event.MantletProtectionHandler;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method="tick", at=@At("HEAD"))
    private void siegeworks$recruitTick(CallbackInfo ci) {
        if (Platform.isModLoaded("recruits")) RecruitsCompat.livingTick((LivingEntity)(Object)this);
    }
    @ModifyVariable(method="hurt", at=@At("HEAD"), argsOnly=true, ordinal=0)
    private float siegeworks$mantletProtection(float amount, DamageSource source, float originalAmount) {
        return MantletProtectionHandler.reduce((LivingEntity)(Object)this, source, amount);
    }
}
