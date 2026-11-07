package me.mss1r.siegeworks.gameplay.loading;

import me.mss1r.siegeworks.item.RoundShotItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A live inventory view: incompatible round shot is unavailable to the loading session. */
public record CaliberInventory(Container source, int caliber) implements Container {
    private boolean accepts(ItemStack stack) { return !(stack.getItem() instanceof RoundShotItem) || RoundShotItem.caliber(stack)==caliber; }
    @Override public int getContainerSize() { return source.getContainerSize(); }
    @Override public boolean isEmpty() { for(int i=0;i<getContainerSize();i++) if(!getItem(i).isEmpty()) return false;return true; }
    @Override public ItemStack getItem(int slot) { ItemStack stack=source.getItem(slot);return accepts(stack)?stack:ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) { return accepts(source.getItem(slot))?source.removeItem(slot,amount):ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return accepts(source.getItem(slot))?source.removeItemNoUpdate(slot):ItemStack.EMPTY; }
    @Override public void setItem(int slot,ItemStack stack) { if(accepts(stack) && accepts(source.getItem(slot))) source.setItem(slot,stack); }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return accepts(stack)&&accepts(source.getItem(slot))&&source.canPlaceItem(slot,stack); }
    @Override public void setChanged() { source.setChanged(); }
    @Override public boolean stillValid(Player player) { return source.stillValid(player); }
    @Override public void clearContent() { for(int i=0;i<getContainerSize();i++) if(accepts(source.getItem(i))) source.setItem(i,ItemStack.EMPTY);source.setChanged(); }
}
