package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.api.BlueprintStacks;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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

import java.util.UUID;

/** What an engine is while it is built, and what is left of it when it is destroyed or burns. */
@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConstructionStateGameTests {
    private static final String ARCBALLISTA = "siegeworks:arcballista";

    private ConstructionStateGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void anUnfinishedEngineStandsStillAndCarriesNoOne(GameTestHelper helper) {
        AbstractSiegeEntity engine = deploy(helper);
        double raised = engine.getY() + 2.0D;
        engine.setPos(engine.getX(), raised, engine.getZ());
        Player rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        rider.setPos(engine.position());
        helper.assertFalse(rider.startRiding(engine, true), "Someone boarded an unfinished engine");
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(Math.abs(engine.getY() - raised) < 1.0E-6D, "The unfinished engine fell or moved");
            helper.assertTrue(engine.getDeltaMovement().lengthSqr() < 1.0E-12D, "Velocity built up while unfinished");
            engine.discard();
            rider.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void anEngineIsAsSturdyAsItIsBuilt(GameTestHelper helper) {
        AbstractSiegeEntity engine = deploy(helper);
        helper.runAfterDelay(2, () -> {
            float max = engine.getMaxHealth();
            helper.assertTrue(Math.abs(engine.getHealth() - max * 0.1F) < 0.01F,
                    "A bare site has " + engine.getHealth() + " of " + max + " health, not a tenth");
            buildStages(helper, engine, 1);
            helper.runAfterDelay(2, () -> {
                BlueprintConstructionPlan plan = engine.buildProgress().plan();
                float share = Math.max(0.1F, plan.stage(0).hits() / (float) plan.totalHits());
                helper.assertTrue(Math.abs(engine.getHealth() - max * share) < 0.05F,
                        "After its first stage the engine has " + engine.getHealth() + ", not " + max * share);
                engine.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aDestroyedSiteLeavesDebrisOfWhatWasBuilt(GameTestHelper helper) {
        AbstractSiegeEntity engine = deploy(helper);
        buildStages(helper, engine, 2);
        java.util.Map<String, Integer> built = new java.util.HashMap<>();
        BlueprintConstructionPlan plan = engine.buildProgress().plan();
        for (int stage = 0; stage < 2; stage++) {
            plan.stage(stage).materials().forEach(material -> built.merge(material.key(), material.count(), Integer::sum));
        }
        int expected = built.values().stream().mapToInt(count -> count / 4).sum();
        engine.remove(Entity.RemovalReason.KILLED);
        int dropped = droppedAround(helper, engine.position());
        helper.assertTrue(expected > 0 && dropped == expected,
                "A site with two built stages " + built + " left " + dropped + " items, not a quarter: " + expected);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aDestroyedEngineLeavesAQuarterOfItself(GameTestHelper helper) {
        AbstractSiegeEntity engine = finished(helper);
        int expected = SiegeMaintenanceData.forSiege(engine).ingredients().stream()
                .mapToInt(material -> (int) Math.floor(material.count() * 0.25F)).sum();
        engine.remove(Entity.RemovalReason.KILLED);
        int dropped = droppedAround(helper, engine.position());
        helper.assertTrue(expected > 0 && dropped == expected,
                "A destroyed engine left " + dropped + " items, not a quarter: " + expected);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void woodBurnsUntilItIsPutOut(GameTestHelper helper) {
        AbstractSiegeEntity engine = finished(helper);
        float start = engine.getHealth();
        engine.setRemainingFireTicks(20);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(engine.isOnFire(), "The fire went out by itself");
            float lost = (start - engine.getHealth()) / engine.getMaxHealth();
            helper.assertTrue(lost > 0.06F && lost < 0.11F,
                    "Six seconds of fire took " + lost * 100 + "% of the engine, not about 9%");
            Player firefighter = SiegeGameTestPlayers.create(helper.getLevel());
            firefighter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            engine.interact(firefighter, InteractionHand.MAIN_HAND);
            helper.assertFalse(engine.isOnFire(), "A bucket of water did not put the fire out");
            helper.assertTrue(firefighter.getMainHandItem().is(Items.BUCKET), "The bucket was not emptied");
            engine.discard();
            helper.succeed();
        });
    }

    private static void buildStages(GameTestHelper helper, AbstractSiegeEntity engine, int stages) {
        SimpleContainer materials = new SimpleContainer(64);
        for (BlueprintConstructionPlan.Stage stage : engine.buildProgress().plan().stages()) {
            for (BlueprintDefinition.Material material : stage.materials()) {
                materials.addItem(material.returnStack().copyWithCount(64));
            }
        }
        int attempts = 0;
        while (engine.buildProgress().stage() < stages && attempts++ < 1000) {
            helper.assertTrue(ConstructionWork.strike(engine, materials).status()
                    != ConstructionWork.Status.MISSING_MATERIAL, "The stage ran out of materials");
        }
    }

    private static int droppedAround(GameTestHelper helper, Vec3 at) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(4.0D)).stream()
                .mapToInt(item -> item.getItem().getCount()).sum();
    }

    private static void clear(GameTestHelper helper) {
        for (int x = 8; x <= 24; x++) {
            for (int z = 8; z <= 24; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 8; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.getLevel().getEntitiesOfClass(Entity.class, new AABB(helper.absolutePos(new BlockPos(16, 1, 16)))
                .inflate(10.0D)).forEach(Entity::discard);
    }

    private static AbstractSiegeEntity deploy(GameTestHelper helper) {
        clear(helper);
        BlueprintDefinition recipe = BlueprintDefinitions.get(ARCBALLISTA);
        helper.assertTrue(recipe != null, "The arcballista blueprint was not loaded");
        ItemStack result = BlueprintStacks.createResult(recipe);
        MinecraftVersionCompat.editCustomData(result, tag -> tag.putUUID(
                SiegeDeploymentLimits.TAG_CONSTRUCTION_OWNER, UUID.randomUUID()));
        BlockPos target = helper.absolutePos(new BlockPos(16, 0, 16));
        Entity placed = me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer.deploy(
                helper.getLevel(), ARCBALLISTA, BlueprintStacks.create(ARCBALLISTA), result,
                target, Direction.UP, Vec3.atCenterOf(target).add(0.0D, 0.5D, 0.0D), 0.0F);
        helper.assertTrue(placed instanceof AbstractSiegeEntity engine && placed instanceof UnderConstruction
                        && !engine.isFullyBuilt(), "The arcballista was not placed under construction");
        return (AbstractSiegeEntity) placed;
    }

    private static AbstractSiegeEntity finished(GameTestHelper helper) {
        clear(helper);
        AbstractSiegeEntity engine = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(engine != null, "Failed to create an arcballista");
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(16, 1, 16)));
        engine.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(engine);
        return engine;
    }
}
