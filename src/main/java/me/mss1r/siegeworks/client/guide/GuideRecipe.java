package me.mss1r.siegeworks.client.guide;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Crafting and stonecutting recipes synchronized by the current server. */
record GuideRecipe(List<ItemStack> ingredients, ItemStack result, int rows) {
    static Optional<GuideRecipe> read(ClientLevel level, ResourceLocation id) {
        var found = level.getRecipeManager().byKey(id);
        //? if <1.21 {
        /*var recipe = found.orElse(null);
        *///?} else {
        var recipe = found.map(holder -> holder.value()).orElse(null);
        //?}
        if (recipe == null) return Optional.empty();

        var ingredients = new ArrayList<ItemStack>();
        int rows;
        if (recipe instanceof ShapedRecipe shaped) {
            rows = shaped.getHeight();
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < 3; x++) {
                    ingredients.add(x < shaped.getWidth()
                            ? display(recipe.getIngredients().get(y * shaped.getWidth() + x).getItems())
                            : ItemStack.EMPTY);
                }
            }
        } else {
            recipe.getIngredients().forEach(ingredient -> ingredients.add(display(ingredient.getItems())));
            rows = (ingredients.size() + 2) / 3;
        }
        return Optional.of(new GuideRecipe(List.copyOf(ingredients),
                recipe.getResultItem(level.registryAccess()).copy(), rows));
    }

    private static ItemStack display(ItemStack[] alternatives) {
        return alternatives.length == 0 ? ItemStack.EMPTY : alternatives[0].copy();
    }
}
