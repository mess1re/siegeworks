package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.client.SiegeworksClientGameEvents;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method="renderCrosshair", at=@At("HEAD"), cancellable=true)
    private void siegeworks$crosshair(CallbackInfo ci) {
        if (SiegeworksClientGameEvents.hideCrosshair()) ci.cancel();
    }
}
