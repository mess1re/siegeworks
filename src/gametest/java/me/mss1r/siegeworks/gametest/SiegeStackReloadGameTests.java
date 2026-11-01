package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** Save/load complete entities in both insertion orders, retaining real movement and collision ticks. */
@GameTestHolder(SiegeStackReloadGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeStackReloadGameTests {
    public static final String NAMESPACE = "siegeworks_stack_regression";

    @GameTest(template = "empty", timeoutTicks = 210, batch = "stack_reload_tower")
    public static void aCrossbowOnATowerKeepsItsSavedFooting(GameTestHelper helper) {
        checkStack(helper, SiegeworksEntities.SIEGE_TOWER_ENTITY.get(),
                SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get(), 90);
    }

    @GameTest(template = "empty", timeoutTicks = 210, batch = "stack_reload_guns")
    public static void oneGunOnAnotherDoesNotPushItsBaseIntoTheGround(GameTestHelper helper) {
        checkStack(helper, SiegeworksEntities.CULVERIN_ENTITY.get(),
                SiegeworksEntities.CULVERIN_ENTITY.get(), 37);
    }

    private static void checkStack(GameTestHelper helper, EntityType<? extends AbstractSiegeEntity> baseType,
                                   EntityType<? extends AbstractSiegeEntity> cargoType, float yaw) {
        for (int x = 0; x < 24; x++) for (int z = 0; z < 24; z++)
            helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        Vec3 origin = helper.absoluteVec(new Vec3(10.5, 2, 10.5));
        AbstractSiegeEntity[] pair = {baseType.create(helper.getLevel()), null};
        pair[0].setPos(origin.x, origin.y, origin.z);
        pair[0].applyYaw(yaw);
        helper.getLevel().addFreshEntity(pair[0]);
        double[] heights = new double[2];
        CompoundTag[] saved = new CompoundTag[2];
        helper.runAfterDelay(25, () -> {
            Vec3 rayStart = pair[0].position().add(0, 30, 0);
            Vec3 surface = StructureCollisionResolver.clipSegment(pair[0], rayStart, pair[0].position())
                    .orElseThrow(() -> new AssertionError("No supporting surface on the base machine"));
            pair[1] = cargoType.create(helper.getLevel());
            pair[1].setPos(surface.x, surface.y + .001, surface.z);
            pair[1].applyYaw(yaw - 60);
            helper.getLevel().addFreshEntity(pair[1]);
        });
        for (int cycle = 0; cycle < 2; cycle++) {
            int at = 65 + cycle * 60;
            boolean cargoFirst = cycle == 0;
            helper.runAfterDelay(at, () -> {
                if (cargoFirst) {
                    heights[0] = pair[0].getY();
                    heights[1] = pair[1].getY();
                    helper.assertTrue(Math.abs(heights[0] - origin.y) < .001,
                            "The lower engine sank before saving: " + heights[0] + " vs " + origin.y);
                }
                for (int i = 0; i < pair.length; i++) {
                    saved[i] = pair[i].saveWithoutId(new CompoundTag());
                    pair[i].discard();
                }
            });
            helper.runAfterDelay(at + 2, () -> {
                pair[0] = baseType.create(helper.getLevel());
                pair[1] = cargoType.create(helper.getLevel());
                for (int i = 0; i < pair.length; i++) pair[i].load(saved[i]);
                helper.getLevel().addFreshEntity(pair[cargoFirst ? 1 : 0]);
                helper.getLevel().addFreshEntity(pair[cargoFirst ? 0 : 1]);
            });
            helper.runAfterDelay(at + 50, () -> {
                helper.assertTrue(Math.abs(pair[0].getY() - heights[0]) < .001,
                        "Reload sank the base: " + heights[0] + " -> " + pair[0].getY());
                helper.assertTrue(Math.abs(pair[1].getY() - heights[1]) < .01,
                        "Reload lost cargo support: " + heights[1] + " -> " + pair[1].getY());
                if (!cargoFirst) {
                    for (AbstractSiegeEntity engine : pair) engine.discard();
                    helper.succeed();
                }
            });
        }
    }
}
