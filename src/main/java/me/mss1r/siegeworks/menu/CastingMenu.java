package me.mss1r.siegeworks.menu;

import me.mss1r.siegeworks.gameplay.advancement.SiegeAdvancement;
import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.item.crafting.CastingRecipe;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.registry.SiegeworksMenus;
import me.mss1r.siegeworks.registry.SiegeworksRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class CastingMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final BlockPos pos;
    private final DataSlot caliber = DataSlot.standalone();

    public CastingMenu(int id, Inventory inventory, BlockPos pos) {
        super(SiegeworksMenus.CASTING.get(), id);
        this.inventory = inventory; this.pos = pos.immutable();
        caliber.set(120); addDataSlot(caliber);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 170));
    }

    public int caliber() { return caliber.get(); }

    public CastingRecipe recipe() {
        var entries = inventory.player.level().getRecipeManager().getAllRecipesFor(SiegeworksRecipes.CASTING.get());
        //? if <1.21 {
        /*List<CastingRecipe> recipes = entries.stream().sorted(java.util.Comparator.comparing(r -> r.getId().toString())).toList();
        *///?} else {
        List<CastingRecipe> recipes = entries.stream().sorted(java.util.Comparator.comparing(r -> r.id().toString())).map(r -> r.value()).toList();
        //?}
        return recipes.stream().filter(r -> plan(r) != null).findFirst().orElse(recipes.isEmpty() ? null : recipes.get(0));
    }

    /** Simulate both removal and insertion; only commit when the entire batch fits. */
    private List<ItemStack> plan(CastingRecipe recipe) {
        if (!RoundShotSize.valid(caliber())) return null;
        List<ItemStack> next = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) next.add(inventory.getItem(i).copy());
        int remaining = recipe.batch(caliber()).cost();
        for (ItemStack stack : next) if (recipe.ingredient.test(stack) && remaining > 0) {
            int amount = Math.min(remaining, stack.getCount()); stack.shrink(amount); remaining -= amount;
        }
        if (remaining > 0) return null;
        ItemStack result = recipe.output(caliber());
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < next.size() && !result.isEmpty(); i++) {
            ItemStack slot = next.get(i);
            if (pass == 0 && !slot.isEmpty() && MinecraftVersionCompat.isSameItemAndData(slot, result)) {
                int added = Math.min(result.getCount(), Math.max(0, slot.getMaxStackSize() - slot.getCount()));
                slot.grow(added); result.shrink(added);
            } else if (pass == 1 && slot.isEmpty()) {
                int added = Math.min(result.getCount(), result.getMaxStackSize());
                next.set(i, result.copyWithCount(added)); result.shrink(added);
            }
        }
        return result.isEmpty() ? next : null;
    }

    public boolean canCast() { var recipe = recipe(); return recipe != null && plan(recipe) != null; }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (player != inventory.player || player.level().isClientSide || !stillValid(player)) return false;
        if (button == 0 || button == 1) {
            caliber.set(Math.max(RoundShotSize.MIN_CALIBER, Math.min(RoundShotSize.MAX_CALIBER, caliber() + (button == 0 ? -5 : 5))));
            broadcastChanges(); return true;
        }
        if (button != 2) return false;
        var recipe = recipe(); if (recipe == null) return false;
        var next = plan(recipe); if (next == null) return false;
        for (int i = 0; i < 36; i++) inventory.setItem(i, next.get(i));
        inventory.setChanged();
        SiegeAdvancement.CAST_TO_SIZE.award(player);
        broadcastChanges();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return player == inventory.player && player.level().getBlockState(pos).is(SiegeworksBlocks.CASTING_TABLE.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (!moveItemStackTo(stack, index < 27 ? 27 : 0, index < 27 ? 36 : 27, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
