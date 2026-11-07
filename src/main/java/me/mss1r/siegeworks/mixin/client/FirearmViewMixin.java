package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.client.firearm.FirearmClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FirearmViewMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void siegeworks$firearmZoom(Camera camera, float partialTick, boolean changingFov, CallbackInfoReturnable<Double> ci) {
        if (changingFov && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            ci.setReturnValue(ci.getReturnValue() * (1 - .35 * FirearmClient.zoom(partialTick)));
        }
    }
}
