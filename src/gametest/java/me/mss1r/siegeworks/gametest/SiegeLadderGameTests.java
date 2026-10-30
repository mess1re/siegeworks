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
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
//? if <1.21 {
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

    @GameTest(template = "empty", timeoutTicks = 900, batch = "ladder_sleep")
    public static void aSleepingLadderOnlyWakesWhenItsSupportChanges(GameTestHelper helper) {
        sleepingLadderOnlyWakesWhenItsSupportChanges(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 900, batch = "ladder_sleep")
    public static void aSleepingLedgeLadderOnlyWakesWhenItsSupportChanges(GameTestHelper helper) {
        sleepingLadderOnlyWakesWhenItsSupportChanges(helper, true);
    }

    private static void sleepingLadderOnlyWakesWhenItsSupportChanges(GameTestHelper helper, boolean ledge) {
        ground(helper);
        if (ledge) {
            for (int x = 6; x <= 10; x++) for (int z = 0; z <= 8; z++) for (int y = 2; y <= 4; y++)
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        } else wall(helper, 7);
        SiegeLadderEntity ladder = ledge ? lettingGo(helper, 2, new Vec3(8.5, 5, 6.5), 89)
                : lettingGo(helper, 1, new Vec3(8.5, 2, 8.5), 40);
        Vec3[] restingFoot = {null};
        float[] restingLean = {0};
        int[] restedAt = {-1}, removedAt = {-1};
        for (int tick = 1; tick <= 800; tick++) {
            int now = tick;
            helper.runAfterDelay(tick, () -> {
                if (removedAt[0] >= 0) {
                    if (now - removedAt[0] >= 45) {
                        helper.assertTrue(Math.abs(ladder.getLeanAngleDegrees() - restingLean[0]) > 1,
                                "A sleeping ladder ignored the removal of its supporting wall");
                        helper.succeed();
                    }
                    return;
                }
                if (restingFoot[0] == null) {
                    if (!ladder.isReadyForAutomatedClimb()) return;
                    restingFoot[0] = ladder.position();
                    restingLean[0] = ladder.getLeanAngleDegrees();
                    restedAt[0] = now;
                }
                helper.assertTrue(ladder.isReadyForAutomatedClimb()
                                && ladder.position().distanceToSqr(restingFoot[0]) < 1.0E-10
                                && Math.abs(ladder.getLeanAngleDegrees() - restingLean[0]) < 1.0E-4,
                        "Unchanged terrain woke or moved a sleeping ladder: at=" + now + ", restedAt=" + restedAt[0]
                                + ", from=" + restingFoot[0] + "/" + restingLean[0]
                                + ", to=" + ladder.position() + "/" + ladder.getLeanAngleDegrees());
                if (now - restedAt[0] >= 300) {
                    if (ledge) {
                        for (int x = 6; x <= 10; x++) for (int z = 0; z <= 8; z++) for (int y = 2; y <= 4; y++)
                            helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    } else for (int x = 4; x <= 12; x++) for (int y = 2; y < 7; y++)
                            helper.setBlock(new BlockPos(x, y, 13), Blocks.AIR);
                    removedAt[0] = now;
                }
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void legacyLaddersKeepTheirSavedLean(GameTestHelper helper) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        CompoundTag saved = new CompoundTag();
        ladder.addAdditionalSaveData(saved);
        saved.remove("LeanDegrees");
        saved.putFloat("LeanProgress", -.5F);
        ladder.readAdditionalSaveData(saved);
        helper.assertTrue(Math.abs(ladder.getLeanAngleDegrees() + 44.5F) < .001,
                "Legacy world reset the ladder's saved lean");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void anUpsideDownLadderStagesAtItsActualLowerEnd(GameTestHelper helper) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        ladder.setSections(1);
        ladder.setPos(helper.absoluteVec(new Vec3(8, 10, 8)));
        CompoundTag saved = new CompoundTag();
        ladder.addAdditionalSaveData(saved);
        saved.putFloat("LeanDegrees", 120);
        saved.putInt("DeployTicks", 35);
        saved.putBoolean("Settled", true);
        ladder.readAdditionalSaveData(saved);
        Vec3 approach = ladder.getAutomatedBottomApproach();
        helper.assertTrue(Math.abs(approach.y - ladder.climbFoot().y) < .1,
                "Queue approaches the upper authoring origin instead of the lower end: " + approach);
        helper.assertTrue(approach.distanceTo(ladder.climbFoot()) < 1.4,
                "Approach is not next to the physical lower end");
        helper.succeed();
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

    /** Standing on a pillar, a ladder does not stop level over the drop: it goes on over, its top to the ground. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aLadderTippingOffAPillarComesDownToTheGround(GameTestHelper helper) {
        ground(helper);
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(8, y, 8), Blocks.STONE);
        }
        SiegeLadderEntity ladder = lettingGo(helper, 1, new Vec3(8.5D, 7.0D, 8.5D), 10.0F);
        helper.runAfterDelay(180, () -> {
            double ground = helper.absolutePos(BlockPos.ZERO).getY() + 2.5D;
            double top = ladder.getY() + ladder.getLadderLength() * Math.cos(Math.toRadians(ladder.getLeanAngleDegrees()));
            helper.assertTrue(top < ground && ladder.isReadyForAutomatedClimb(),
                    "The ladder stopped over the drop instead of coming down off the pillar: foot " + ladder.getY()
                            + ", top " + top + ", lean " + ladder.getLeanAngleDegrees());
            helper.succeed();
        });
    }

    /** Lying mostly over a ledge, a ladder tips over the edge into a ramp, climbable from its lower end. */
    @GameTest(template = "empty", timeoutTicks = 350)
    public static void aLadderLyingOverALedgeTipsIntoARamp(GameTestHelper helper) {
        ground(helper);
        for (int x = 6; x <= 10; x++) {
            for (int z = 0; z <= 8; z++) {
                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        SiegeLadderEntity ladder = lettingGo(helper, 2, new Vec3(8.5D, 5.0D, 6.5D), 89.0F);
        helper.runAfterDelay(180, () -> helper.succeedWhen(() -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(),
                    "The ladder did not come to rest climbable: lean " + ladder.getLeanAngleDegrees()
                            + ", foot " + ladder.position() + ", lower end " + ladder.climbFoot());
            helper.assertTrue(ladder.climbFoot().y < ladder.getY() - 1.0D,
                    "The ladder is still level over the ledge: foot " + ladder.getY()
                            + ", lower end " + ladder.climbFoot().y + ", lean " + ladder.getLeanAngleDegrees());
        }));
    }

    /** Standing under a ladder, level with its rungs, does not put anyone on it. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void standingUnderALadderDoesNotLiftOntoIt(GameTestHelper helper) {
        ground(helper);
        wall(helper, 7);
        SiegeLadderEntity ladder = lettingGo(helper, 1, new Vec3(8.5D, 2.0D, 8.5D), 40.0F);
        LivingEntity under = standing(helper, new Vec3(8.5D, 2.0D, 10.0D));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "The ladder did not settle against the wall");
            helper.assertTrue(Math.abs(under.getY() - helper.absoluteVec(new Vec3(0.0D, 2.0D, 0.0D)).y) < 0.01D,
                    "Someone standing under the ladder was put on it: y " + under.getY());
            helper.succeed();
        });
    }

    /** A defender on the wall beside the ladder's top stays on the wall. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void aDefenderBesideTheTopStaysOnTheWall(GameTestHelper helper) {
        ground(helper);
        wall(helper, 4);
        SiegeLadderEntity ladder = lettingGo(helper, 2, new Vec3(8.5D, 2.0D, 8.5D), 40.0F);
        LivingEntity defender = standing(helper, new Vec3(8.5D, 6.0D, 13.5D));
        Vec3 stood = defender.position();
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "The ladder did not settle on the wall");
            helper.assertTrue(defender.position().distanceTo(stood) < 0.05D,
                    "A defender beside the ladder's top was moved onto it: " + defender.position());
            helper.succeed();
        });
    }

    /** A wall across the ladder's way, from {@code y = 2} up to {@code top}, at {@code z = 13}. */
    private static void wall(GameTestHelper helper, int top) {
        for (int x = 4; x <= 12; x++) {
            for (int y = 2; y <= top - 1; y++) {
                helper.setBlock(new BlockPos(x, y, 13), Blocks.STONE);
            }
        }
    }

    private static LivingEntity standing(GameTestHelper helper, Vec3 at) {
        Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Failed to create a villager");
        villager.setNoAi(true);
        Vec3 position = helper.absoluteVec(at);
        villager.moveTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(villager);
        return villager;
    }

    private static void ground(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    /** A ladder facing south at {@code foot}, already let go at {@code lean} degrees. */
    private static SiegeLadderEntity lettingGo(GameTestHelper helper, int sections, Vec3 foot, float lean) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(ladder != null, "Failed to create siege ladder");
        ladder.setSections(sections);
        ladder.applyYaw(0.0F);
        Vec3 position = helper.absoluteVec(foot);
        ladder.setPos(position.x, position.y, position.z);
        CompoundTag tag = new CompoundTag();
        ladder.addAdditionalSaveData(tag);
        tag.putInt("DeployTicks", 35);
        tag.putFloat("LeanDegrees", lean);
        ladder.readAdditionalSaveData(tag);
        helper.getLevel().addFreshEntity(ladder);
        return ladder;
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
                //? if <1.21 {
                /*MinecraftVersionCompat.id(Siegeworks.MOD_ID, "siege_ladder_extension"),
                *///?}
                CraftingBookCategory.MISC);
        //? if <1.21 {
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
