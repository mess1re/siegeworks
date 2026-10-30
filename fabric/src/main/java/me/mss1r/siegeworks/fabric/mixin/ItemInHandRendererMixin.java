package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.client.SiegeworksClientGameEvents;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method="renderHandsWithItems", at=@At("HEAD"), cancellable=true)
    private void siegeworks$hands(CallbackInfo ci) {
        if (SiegeworksClientGameEvents.hideHands()) ci.cancel();
    }
}
