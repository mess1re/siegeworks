package me.mss1r.siegeworks.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.mss1r.axiomata.blueprint.api.ConstructionStarters;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.internal.construction.MaterialAllocation;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintFormat;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(LadderConstructionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderConstructionGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ladder";
    private static final String LADDER = "siegeworks:siege_ladder";
    // Same batch as the other construction tests: a ladder is taller than the gap between tests.
    private static final String BATCH = "axiomata_construction";

    private LadderConstructionGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void craftedLadderBaseStartsTheLadderBuildWithItsBaseStanding(GameTestHelper helper) {
        ItemStack base = new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get());
        helper.assertTrue(SiegeLadderDeploymentItem.getSections(base) == 0,
                "A ladder from the crafting table is not its base alone");
        helper.assertTrue(LADDER.equals(ConstructionStarters.definitionFor(base)),
                "The ladder base does not start the ladder build");
        BlueprintDefinition ladder = BlueprintDefinitions.get(LADDER);
        helper.assertTrue(ladder != null && ladder.isExtendable(), "The ladder build cannot end early");
        helper.assertTrue(ConstructionStarters.builtStages(ladder, base) == 1,
                "The ladder base does not stand for the base stage");
        ItemStack twoSections = SiegeLadderDeploymentItem.withSections(
                new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), 2);
        helper.assertTrue(ConstructionStarters.builtStages(ladder, twoSections) == 3,
                "A two-section ladder taken up again does not carry on from its third stage");

        SiegeLadderEntity built = deploy(helper, base);
        helper.assertTrue(built.buildProgress().stage() == 1 && !built.isFullyBuilt(),
                "The ladder build did not begin with its base standing");
        BlueprintConstructionPlan.Stage section = built.buildProgress().currentStage();
        helper.assertTrue(section != null && section.hits() == 12 && section.materials().size() == 2
                        && section.materials().get(0).key().equals("minecraft:oak_log")
                        && section.materials().get(0).count() == 4
                        && section.materials().get(1).key().equals("minecraft:stick")
                        && section.materials().get(1).count() == 3,
                "A ladder section does not take four logs, three sticks and twelve blows: " + describe(section));
        succeed(helper, built);
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void ladderBuildEndsWithTheSectionsBuiltSoFar(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
        BuildProgress progress = ladder.buildProgress();
        while (progress.stage() < 3) {
            ConstructionWork.strike(ladder, null);
        }
        helper.assertTrue(progress.canEndHere(), "A ladder with two sections up cannot be finished there");

        ConstructionWork.Result ended = ConstructionWork.endHere(ladder, null);
        helper.assertTrue(ended.status() == ConstructionWork.Status.DONE && ladder.isFullyBuilt(),
                "Ending the ladder build did not finish it");
        helper.assertTrue(ladder.getSections() == 2,
                "The finished ladder does not stand as tall as its two built sections: " + ladder.getSections());
        succeed(helper, ladder);
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void ladderBuiltToTheTopHasEverySection(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
        while (!ladder.isFullyBuilt()) {
            ConstructionWork.strike(ladder, null);
        }
        helper.assertTrue(ladder.getSections() == SiegeLadderEntity.MAX_SECTIONS,
                "A ladder built to the top has " + ladder.getSections() + " sections");
        succeed(helper, ladder);
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void fullLadderItemStartedWithTheHammerKeepsEverySection(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, SiegeLadderDeploymentItem.withSections(
                new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), SiegeLadderEntity.MAX_SECTIONS));
        helper.assertTrue(ladder.isFullyBuilt() && ladder.getSections() == SiegeLadderEntity.MAX_SECTIONS,
                "A full ladder item set down with the hammer has " + ladder.getSections() + " sections");
        succeed(helper, ladder);
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = BATCH)
    public static void ladderBeingBuiltStandsStillAndOnlyItsBuiltPartsCollide(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
        Vec3 placed = ladder.position();
        helper.assertTrue(ladder.collisionGroups().size() == 1,
                "A ladder build with its base standing collides with " + ladder.collisionGroups().size() + " parts");
        while (ladder.buildProgress().stage() < 2) {
            ConstructionWork.strike(ladder, null);
        }
        helper.assertTrue(ladder.collisionGroups().size() == 2,
                "A ladder build with one section up collides with " + ladder.collisionGroups().size() + " parts");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(ladder.getLeanProgress() == 0.0F && ladder.position().equals(placed),
                    "A ladder being built leaned or moved: lean " + ladder.getLeanProgress()
                            + ", moved by " + ladder.position().subtract(placed));
            succeed(helper, ladder);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void ladderBaseAloneCanBeFinished(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
        helper.assertTrue(ConstructionWork.endHere(ladder, null).status() == ConstructionWork.Status.DONE,
                "A ladder build could not be finished as its base alone");
        helper.assertTrue(ladder.isFullyBuilt() && ladder.getSections() == 0,
                "The base-only ladder is not a finished ladder without sections");
        succeed(helper, ladder);
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void ladderMaintenanceCountsOnlyTheSectionsItHas(GameTestHelper helper) {
        SiegeLadderEntity ladder = deploy(helper, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
        while (ladder.buildProgress().stage() < 3) {
            ConstructionWork.strike(ladder, null);
        }
        ConstructionWork.endHere(ladder, null);
        Map<ResourceLocation, Integer> resources = SiegeMaintenanceData.forSiege(ladder).ingredients();
        int logs = resources.getOrDefault(ResourceLocation.tryParse("minecraft:oak_log"), 0);
        int sticks = resources.getOrDefault(ResourceLocation.tryParse("minecraft:stick"), 0);
        helper.assertTrue(logs == 14 && sticks == 9,
                "A two-section ladder is reckoned at " + logs + " logs and " + sticks + " sticks, not 14 and 9");
        succeed(helper, ladder);
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void bundledBlueprintsReadAsTheOldFilesDid(GameTestHelper helper) {
        BlueprintFormat.Ids ids = new BlueprintFormat.Ids(
                BuiltInRegistries.ITEM::containsKey, BuiltInRegistries.ENTITY_TYPE::containsKey);
        for (JsonElement name : readJson("data/siegeworks/blueprints/index.json").getAsJsonArray()) {
            String id = Siegeworks.MOD_ID + ":" + name.getAsString();
            BlueprintFormat.Parsed old = BlueprintFormat.parse(
                    readJson("legacy_blueprints/" + name.getAsString() + ".json"), ids);
            helper.assertTrue(old.valid(), "The old " + id + " no longer reads: " + old.errors());
            BlueprintDefinition loaded = BlueprintDefinitions.get(id);
            helper.assertTrue(loaded != null, "Blueprint " + id + " did not load");
            String expected = BlueprintFormat.write(old.definition()).toString();
            String actual = BlueprintFormat.write(loaded).toString();
            helper.assertTrue(expected.equals(actual),
                    "Blueprint " + id + " changed in the new format:\n was " + expected + "\n now " + actual);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void materialsThatShareItemsAreNotPaidWithTheSameOnes(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.OAK_LOG, 6),
                new ItemStack(Items.SPRUCE_LOG, 2)));
        Material anyLogs = Material.ofTag(ResourceLocation.tryParse("minecraft:logs"), 4);
        Material oakLogs = Material.ofItem(ResourceLocation.tryParse("minecraft:oak_log"), 6);
        List<Material> stage = List.of(anyLogs, oakLogs);
        Map<Material, Integer> missing = MaterialAllocation.shortfall(stacks, stage);
        helper.assertTrue(missing.equals(Map.of(anyLogs, 2)),
                "Six oak and two spruce logs should leave any logs two short, not " + missing);
        stacks.add(new ItemStack(Items.BIRCH_LOG, 2));
        helper.assertTrue(MaterialAllocation.shortfall(stacks, stage).isEmpty(),
                "Ten logs, six of them oak, did not pay for six oak and four of any");
        MaterialAllocation.take(stacks, stage);
        helper.assertTrue(stacks.stream().allMatch(ItemStack::isEmpty), "Not every log was taken: " + stacks);
        helper.succeed();
    }

    private static JsonElement readJson(String path) {
        try (InputStream stream = LadderConstructionGameTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String describe(BlueprintConstructionPlan.Stage stage) {
        return stage == null ? "none" : stage.materials() + " in " + stage.hits() + " blows";
    }

    /** Removes the ladder; it is taller than its test area and would overlap later tests. */
    private static void succeed(GameTestHelper helper, SiegeLadderEntity ladder) {
        ladder.discard();
        helper.succeed();
    }

    private static SiegeLadderEntity deploy(GameTestHelper helper, ItemStack starter) {
        ServerLevel level = helper.getLevel();
        BlueprintDefinition definition = BlueprintDefinitions.get(LADDER);
        helper.assertTrue(definition != null, "The ladder build is missing");
        BlockPos floor = helper.absolutePos(new BlockPos(16, 0, 16));
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-4, 0, -4), floor.offset(4, 0, 4))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-4, 1, -4), floor.offset(4, 20, 4))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        level.getEntitiesOfClass(Entity.class, new AABB(floor).inflate(6.0D, 20.0D, 6.0D))
                .forEach(Entity::discard);
        ItemStack result = BlueprintItem.createResultStack(definition);
        MinecraftVersionCompat.editCustomData(result, tag -> tag.putUUID(
                SiegeDeploymentLimits.TAG_CONSTRUCTION_OWNER, UUID.randomUUID()));
        Entity entity = ConstructionDeployer.deploy(level, LADDER, starter, result, floor, Direction.UP,
                Vec3.atCenterOf(floor).add(0.0D, 0.5D, 0.0D), 0.0F);
        helper.assertTrue(entity instanceof SiegeLadderEntity, "The ladder build was not placed: " + entity);
        return (SiegeLadderEntity) entity;
    }
}
