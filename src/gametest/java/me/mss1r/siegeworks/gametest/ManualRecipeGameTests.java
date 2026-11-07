package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import me.mss1r.siegeworks.item.crafting.SiegeManualRecipe;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.21 {
/*import net.minecraft.world.inventory.TransientCraftingContainer;
*///?} else {
import net.minecraft.world.item.crafting.CraftingInput;
//?}

public final class ManualRecipeGameTests {
    @GameTest(template = "empty")
    public static void manualReturnsTheOriginalHammerData(GameTestHelper helper) {
        var found = helper.getLevel().getRecipeManager().byKey(MinecraftVersionCompat.id("siegeworks", "siege_manual"));
        //? if <1.21 {
        /*var recipe = (SiegeManualRecipe) found.orElseThrow();
        *///?} else {
        var recipe = (SiegeManualRecipe) found.orElseThrow().value();
        //?}
        ItemStack hammer = new ItemStack(BlueprintItems.CONSTRUCTION_HAMMER.get());
        hammer.setDamageValue(73);
        MinecraftVersionCompat.setCustomName(hammer, Component.literal("Workshop hammer"));
        MinecraftVersionCompat.editCustomData(hammer, tag -> tag.putString("Workshop", "test"));
        for (int width : new int[]{2, 3}) {
            var grid = NonNullList.withSize(width * width, ItemStack.EMPTY);
            grid.set(0, new ItemStack(Items.BOOK, 3));
            grid.set(grid.size() - 1, hammer.copy());
            //? if <1.21 {
            /*var input = new TransientCraftingContainer(null, width, width, grid);
            *///?} else {
            var input = CraftingInput.of(width, width, grid);
            //?}
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "Manual must craft in both crafting grids");
            var output = recipe.assemble(input, helper.getLevel().registryAccess());
            helper.assertTrue(output.is(SiegeworksItems.SIEGE_MANUAL.get()) && output.getCount() == 1,
                    "Wrong manual output");
            var remaining = recipe.getRemainingItems(input);
            ItemStack returned = remaining.get(grid.size() - 1);
            helper.assertTrue(returned.getCount() == 1 && MinecraftVersionCompat.isSameItemAndData(hammer, returned),
                    "Crafting changed or lost the hammer data");
            helper.assertTrue(returned != input.getItem(grid.size() - 1), "Remainder aliases the crafting-grid stack");
            helper.assertTrue(remaining.get(0).isEmpty(), "Book must be consumed");
            grid.set(grid.size() - 1, new ItemStack(Items.PAPER));
            //? if >=1.21 {
            input = CraftingInput.of(width, width, grid);
            //?}
            helper.assertTrue(!recipe.matches(input, helper.getLevel()), "The former book-and-paper recipe still matches");
        }
        helper.succeed();
    }
}
