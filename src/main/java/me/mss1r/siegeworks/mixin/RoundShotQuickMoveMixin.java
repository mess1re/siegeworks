package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.item.RoundShotItem;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
abstract class RoundShotQuickMoveMixin {
    // On 1.20.1 the empty-slot path uses the slot limit alone. 1.21.1 already passes the stack.
    @Redirect(method = "moveItemStackTo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getMaxStackSize()I"), require = 0)
    private int siegeworks$roundShotQuickMoveLimit(Slot slot, ItemStack stack, int start, int end, boolean reverse) {
        int limit = slot.getMaxStackSize();
        return stack.getItem() instanceof RoundShotItem ? Math.min(limit, stack.getMaxStackSize()) : limit;
    }
}
