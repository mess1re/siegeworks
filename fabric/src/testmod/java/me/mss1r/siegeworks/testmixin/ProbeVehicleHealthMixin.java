package me.mss1r.siegeworks.testmixin;

import me.mss1r.siegeworks.client.SiegeworksClientGameEvents;
import me.mss1r.siegeworks.gametest.AimClientProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the decision actually requested by Fabric's vehicle-health renderer. */
@Mixin(value = SiegeworksClientGameEvents.class, remap = false)
public abstract class ProbeVehicleHealthMixin {
    @Inject(method = "hideVehicleHealth", at = @At("RETURN"), remap = false)
    private static void siegeworks$healthDecision(CallbackInfoReturnable<Boolean> result) {
        AimClientProbe.vehicleHealth(true, result.getReturnValue());
    }
}
