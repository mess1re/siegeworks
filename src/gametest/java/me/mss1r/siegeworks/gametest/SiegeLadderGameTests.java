package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.item.crafting.SiegeLadderExtensionRecipe;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
//? if forge {
/*import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** A ladder is crafted to length: its base, then one more section per craft with the ladder in the pattern. */
@GameTestHolder(SiegeLadderGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeLadderGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ladder";

    private SiegeLadderGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aLadderGrowsBySectionInItsOwnPattern(GameTestHelper helper) {
        ItemStack ladder = new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get());
        for (int sections = 0; sections < SiegeLadderEntity.MAX_SECTIONS; sections++) {
            ItemStack longer = craft(helper, ladder);
            helper.assertTrue(SiegeLadderDeploymentItem.getSections(longer) == sections + 1,
                    "A ladder of " + sections + " sections crafted into " + SiegeLadderDeploymentItem.getSections(longer));
            ladder = longer;
        }
        helper.assertTrue(craft(helper, ladder).isEmpty(), "A full-length ladder grew past its last section");
        helper.assertTrue(helper.getLevel().getRecipeManager()
                        .byKey(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "siege_ladder_extension")).isPresent(),
                "The extension recipe is not loaded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void theLadderIsNoLongerBuiltFromABlueprint(GameTestHelper helper) {
        helper.assertTrue(BlueprintDefinitions.get("siegeworks:siege_ladder") == null,
                "The ladder still has a blueprint");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void ladderMaintenanceCountsWhatItsCraftsTook(GameTestHelper helper) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        ladder.setSections(2);
        var resources = SiegeMaintenanceData.forSiege(ladder).ingredients();
        int logs = SiegeMaintenanceData.count(resources, "#minecraft:logs");
        int sticks = SiegeMaintenanceData.count(resources, "minecraft:stick");
        helper.assertTrue(logs == 18 && sticks == 7,
                "A two-section ladder is reckoned at " + logs + " logs and " + sticks + " sticks, not 18 and 7");
        ladder.discard();
        helper.succeed();
    }

    /** The extension pattern around {@code ladder}: logs down both sides, sticks above the ladder. */
    private static ItemStack craft(GameTestHelper helper, ItemStack ladder) {
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int row = 0; row < 3; row++) {
            grid.set(row * 3, new ItemStack(Items.OAK_LOG));
            grid.set(row * 3 + 2, new ItemStack(Items.SPRUCE_LOG));
        }
        grid.set(1, new ItemStack(Items.STICK));
        grid.set(4, new ItemStack(Items.STICK));
        grid.set(7, ladder.copy());
        var recipe = new SiegeLadderExtensionRecipe(
                //? if forge {
                /*MinecraftVersionCompat.id(Siegeworks.MOD_ID, "siege_ladder_extension"),
                *///?}
                CraftingBookCategory.MISC);
        //? if forge {
        /*var input = new TransientCraftingContainer(null, 3, 3, grid);
        return recipe.matches(input, helper.getLevel())
                ? recipe.assemble(input, helper.getLevel().registryAccess()) : ItemStack.EMPTY;
        *///?} else {
        var input = CraftingInput.of(3, 3, grid);
        return recipe.matches(input, helper.getLevel())
                ? recipe.assemble(input, helper.getLevel().registryAccess()) : ItemStack.EMPTY;
        //?}
    }
}
