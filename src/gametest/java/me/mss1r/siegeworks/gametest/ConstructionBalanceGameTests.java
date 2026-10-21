package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.BuildProgress;
import me.mss1r.axiomata.blueprint.api.construction.BuildQuality;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.construction.MaterialAllocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder("siegeworks_construction_balance")
@PrefixGameTestTemplate(false)
public final class ConstructionBalanceGameTests {
    @GameTest(template = "empty")
    public static void bundledConstructionWorkFitsEveryQuality(GameTestHelper helper) {
        Map<String, Integer> totals = Map.ofEntries(
                Map.entry("arcballista", 48), Map.entry("tower_crossbow", 60),
                Map.entry("hwacha", 48), Map.entry("culverin", 54),
                Map.entry("serpentine", 64), Map.entry("catapult", 84),
                Map.entry("mons_meg", 112), Map.entry("battering_ram", 160),
                Map.entry("trebuchet", 210), Map.entry("siege_tower", 420),
                Map.entry("mantlet", 60));
        totals.forEach((id, total) -> {
            var definition = BlueprintDefinitions.get("siegeworks:" + id);
            helper.assertTrue(definition != null, "Missing blueprint " + id);
            var plain = BlueprintConstructionPlan.of(definition, BuildQuality.PLAIN);
            helper.assertTrue(plain.totalHits() == total, "Wrong construction work for " + id);
            for (BuildQuality quality : BuildQuality.values()) {
                var plan = BlueprintConstructionPlan.of(definition, quality);
                for (int stage = 0; stage < definition.stageCount(); stage++) {
                    int base = definition.stages().get(stage).hits();
                    float multiplier = switch (quality) {
                        case CLEAN -> 0.85F;
                        case HASTY -> 1.45F;
                        case SLOPPY -> 1.8F;
                        default -> 1.0F;
                    };
                    helper.assertTrue(plan.stage(stage).hits() == Math.round(base * multiplier),
                            "Stage work was clipped for " + id + "/" + stage + "/" + quality);
                }
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void savedWorkAndPaidMaterialsSurviveHigherHitRequirements(GameTestHelper helper) {
        BuildProgress old = BuildProgress.finished();
        old.begin("siegeworks:tower_crossbow", BuildQuality.PLAIN);
        old = atStage(old, 1);
        old.commitMaterials(List.of(new ItemStack(Items.IRON_INGOT, 8)));
        for (int hit = 0; hit < 15; hit++) old.strike();
        CompoundTag saved = new CompoundTag();
        old.save(saved);

        BuildProgress restored = BuildProgress.finished();
        restored.load(saved);
        helper.assertTrue(restored.stage() == 1 && restored.hits() == 15,
                "Loading reset completed sections or existing work");
        helper.assertTrue(restored.materialsCommitted()
                        && restored.currentStageRefund().get(0).getCount() == 8,
                "Loading lost the paid materials");
        for (int hit = 0; hit < 8; hit++) {
            helper.assertTrue(!restored.strike(), "Stage completed before its new work requirement");
        }
        helper.assertTrue(restored.strike() && restored.stage() == 2,
                "Saved work could not complete the stage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completedBuildStaysCompleteWhenWorkIncreases(GameTestHelper helper) {
        BuildProgress completed = BuildProgress.finished();
        completed.begin("siegeworks:tower_crossbow", BuildQuality.PLAIN);
        completed = atStage(completed, 3);
        CompoundTag saved = new CompoundTag();
        completed.save(saved);
        BuildProgress restored = BuildProgress.finished();
        restored.load(saved);
        helper.assertTrue(restored.complete(), "New hit requirements reopened a completed build");
        helper.succeed();
    }

    @GameTest(template = "empty")
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

    /** The same progress with its first {@code stages} stages standing, as a saved world would hold it. */
    private static BuildProgress atStage(BuildProgress progress, int stages) {
        CompoundTag tag = new CompoundTag();
        progress.save(tag);
        tag.putInt("BuildStage", stages);
        tag.putInt("BuildHits", 0);
        tag.putBoolean("BuildMaterialsCommitted", false);
        BuildProgress moved = BuildProgress.finished();
        moved.load(tag);
        return moved;
    }
}
