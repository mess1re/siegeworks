package me.mss1r.siegeworks.item.crafting;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksRecipes;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
//? if forge {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
*///?} else {
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
//?}

/**
 * A ladder grows by one section in the same pattern that makes it, with the ladder in place of the bottom middle
 * stick: logs down both sides, sticks above it.
 */
public class SiegeLadderExtensionRecipe extends CustomRecipe {
    //? if forge {
    /*public SiegeLadderExtensionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        return grid.getWidth() == 3 && grid.getHeight() == 3 && ladder(grid.getItems()::get) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return extended(ladder(grid.getItems()::get));
    }
    *///?} else {
    public SiegeLadderExtensionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        return grid.width() == 3 && grid.height() == 3 && ladder(grid::getItem) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput grid, HolderLookup.Provider registries) {
        return extended(ladder(grid::getItem));
    }
    //?}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SiegeworksRecipes.SIEGE_LADDER_EXTENSION.get();
    }

    /** The ladder being extended, or null when the grid is not the pattern or the ladder is already full length. */
    private static ItemStack ladder(java.util.function.IntFunction<ItemStack> slot) {
        for (int row = 0; row < 3; row++) {
            if (!slot.apply(row * 3).is(ItemTags.LOGS) || !slot.apply(row * 3 + 2).is(ItemTags.LOGS)) {
                return null;
            }
        }
        if (!slot.apply(1).is(Items.STICK) || !slot.apply(4).is(Items.STICK)) {
            return null;
        }
        ItemStack ladder = slot.apply(7);
        return ladder.is(SiegeworksItems.SIEGE_LADDER_SPAWNER.get())
                && SiegeLadderDeploymentItem.getSections(ladder) < SiegeLadderEntity.MAX_SECTIONS
                ? ladder : null;
    }

    private static ItemStack extended(ItemStack ladder) {
        if (ladder == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ladder.copyWithCount(1);
        return SiegeLadderDeploymentItem.withSections(result, SiegeLadderDeploymentItem.getSections(ladder) + 1);
    }
}
