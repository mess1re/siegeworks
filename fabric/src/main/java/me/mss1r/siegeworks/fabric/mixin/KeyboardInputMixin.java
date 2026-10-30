package me.mss1r.siegeworks.fabric.mixin;
import me.mss1r.siegeworks.client.SiegeworksClientGameEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method="tick", at=@At("TAIL"))
    private void siegeworks$movement(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.input == (Object)this)
            SiegeworksClientGameEvents.updateMovement(player, (Input)(Object)this);
    }
}
