package me.mss1r.siegeworks.item.crafting;

import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.function.IntFunction;

//? if <1.21 {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
*///?} else {
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
//?}

/** Returns the input hammer without changing its damage or metadata. */
public final class SiegeManualRecipe extends CustomRecipe {
    //? if <1.21 {
    /*public SiegeManualRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return matches(input.getContainerSize(), input::getItem);
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        return new ItemStack(SiegeworksItems.SIEGE_MANUAL.get());
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return new ItemStack(SiegeworksItems.SIEGE_MANUAL.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
        return remainingItems(input.getContainerSize(), input::getItem);
    }
    *///?} else {
    public SiegeManualRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matches(input.size(), input::getItem);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(SiegeworksItems.SIEGE_MANUAL.get());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(SiegeworksItems.SIEGE_MANUAL.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return remainingItems(input.size(), input::getItem);
    }
    //?}

    private static boolean matches(int size, IntFunction<ItemStack> items) {
        boolean book = false;
        boolean hammer = false;
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = items.apply(slot);
            if (stack.isEmpty()) continue;
            if (!book && stack.is(Items.BOOK)) book = true;
            else if (!hammer && stack.is(BlueprintItems.CONSTRUCTION_HAMMER.get())) hammer = true;
            else return false;
        }
        return book && hammer;
    }

    private static NonNullList<ItemStack> remainingItems(int size, IntFunction<ItemStack> items) {
        var remaining = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = items.apply(slot);
            if (stack.is(BlueprintItems.CONSTRUCTION_HAMMER.get())) remaining.set(slot, stack.copyWithCount(1));
        }
        return remaining;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.BOOK),
                Ingredient.of(BlueprintItems.CONSTRUCTION_HAMMER.get()));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SiegeworksRecipes.SIEGE_MANUAL.get();
    }
}
