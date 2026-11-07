package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.item.RoundShotItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class RoundShotStackMixin {
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void siegeworks$roundShotStackLimit(CallbackInfoReturnable<Integer> result) {
        int caliber = RoundShotItem.caliber((ItemStack) (Object) this);
        if (caliber > 0) result.setReturnValue(RoundShotSize.stackLimit(caliber));
    }
}
