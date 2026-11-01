package me.mss1r.siegeworks.testmixin;

import me.mss1r.siegeworks.gametest.DraftTeamClientProbe;
import me.mss1r.siegeworks.gametest.ManualAimFrameProbe;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs shared diagnostics around the real camera/model render, only in the test mod. */
@Mixin(GameRenderer.class)
public abstract class ProbeFrameMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void siegeworks$beforeFrame(CallbackInfo ci) {
        ManualAimFrameProbe.beforeRenderFrame();
        DraftTeamClientProbe.beforeRenderFrame();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void siegeworks$afterFrame(CallbackInfo ci) {
        ManualAimFrameProbe.afterRenderFrame();
        DraftTeamClientProbe.afterRenderFrame();
    }
}
