package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** A falling ladder's body never goes into the blocks it falls among, tick by tick, wherever it comes down. */
@GameTestHolder(LadderFallGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderFallGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ladder";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double HALF_WIDTH = 7.5D / 16.0D;
    private static final double HALF_THICKNESS = 1.5D / 16.0D;
    /** Deeper than this into a block is a visible sink, not a resting contact. */
    private static final double TOLERANCE = 0.01D;

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void tippingOverOnOpenGround(GameTestHelper helper) {
        ground(helper);
        watch(helper, "open ground", letGo(helper, 1, new Vec3(8.5D, 2.0D, 3.5D), 10.0F), 200);
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void tippingAgainstAWall(GameTestHelper helper) {
        ground(helper);
        for (int x = 4; x <= 12; x++) {
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
        watch(helper, "wall", letGo(helper, 1, new Vec3(8.5D, 2.0D, 5.5D), 5.0F), 200);
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void tippingOffAPillar(GameTestHelper helper) {
        ground(helper);
        for (int y = 2; y <= 6; y++) {
            helper.setBlock(new BlockPos(8, y, 4), Blocks.STONE);
        }
        watch(helper, "pillar", letGo(helper, 1, new Vec3(8.5D, 7.0D, 4.5D), 10.0F), 200);
    }

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void tippingOntoSteps(GameTestHelper helper) {
        ground(helper);
        for (int z = 6; z <= 14; z++) {
            for (int y = 2; y <= 1 + (z - 5) / 2; y++) {
                for (int x = 6; x <= 10; x++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        watch(helper, "steps", letGo(helper, 2, new Vec3(8.5D, 2.0D, 3.5D), 10.0F), 200);
    }

    /** Checks the ladder's faces against the blocks every tick, and logs how it fell. */
    private static void watch(GameTestHelper helper, String name, SiegeLadderEntity ladder, int ticks) {
        double[] worst = {0.0D};
        int[] worstTick = {-1};
        int[] restTick = {-1};
        float[] lastLean = {ladder.getLeanAngleDegrees()};
        StringBuilder leans = new StringBuilder();
        for (int tick = 1; tick <= ticks; tick++) {
            int at = tick;
            helper.runAfterDelay(tick, () -> {
                double depth = deepest(helper, ladder);
                if (depth > worst[0]) {
                    worst[0] = depth;
                    worstTick[0] = at;
                }
                float lean = ladder.getLeanAngleDegrees();
                if (at <= 60 && at % 2 == 0) {
                    leans.append(String.format(" %d:%.1f", at, lean));
                }
                if (restTick[0] < 0 && at > 5 && Math.abs(lean - lastLean[0]) < 0.01F) {
                    restTick[0] = at;
                } else if (Math.abs(lean - lastLean[0]) >= 0.01F) {
                    restTick[0] = -1;
                }
                lastLean[0] = lean;
            });
        }
        helper.runAfterDelay(ticks + 1, () -> {
            LOGGER.info("[ladder fall] {}: deepest {} at tick {}, still from tick {}, lean{}",
                    name, String.format("%.3f", worst[0]), worstTick[0], restTick[0],
                    leans);
            helper.assertTrue(worst[0] <= TOLERANCE, "The ladder went " + String.format("%.3f", worst[0])
                    + " into a block at tick " + worstTick[0]);
            helper.succeed();
        });
    }

    /** How deep the ladder's front and back faces reach into any block, sampled over its width and length. */
    private static double deepest(GameTestHelper helper, SiegeLadderEntity ladder) {
        Vec3 foot = ladder.position();
        double lean = Math.toRadians(ladder.getLeanAngleDegrees());
        double length = ladder.getLadderLength();
        double deepest = 0.0D;
        for (double along = 0.0D; along <= length + 1.0E-6D; along += 0.125D) {
            for (double side = -HALF_WIDTH; side <= HALF_WIDTH + 1.0E-6D; side += HALF_WIDTH / 2.0D) {
                for (int face = -1; face <= 1; face += 2) {
                    double across = face * HALF_THICKNESS;
                    // Facing south: forward is +z, right is +x
                    Vec3 point = foot.add(side, along * Math.cos(lean) + across * Math.sin(lean),
                            along * Math.sin(lean) - across * Math.cos(lean));
                    deepest = Math.max(deepest, depthIn(helper, point));
                }
            }
        }
        return deepest;
    }

    private static double depthIn(GameTestHelper helper, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        double deepest = 0.0D;
        for (AABB box : helper.getLevel().getBlockState(pos).getCollisionShape(helper.getLevel(), pos).toAabbs()) {
            AABB world = box.move(pos);
            if (world.contains(point)) {
                double depth = Math.min(Math.min(point.x - world.minX, world.maxX - point.x),
                        Math.min(Math.min(point.y - world.minY, world.maxY - point.y),
                                Math.min(point.z - world.minZ, world.maxZ - point.z)));
                deepest = Math.max(deepest, depth);
            }
        }
        return deepest;
    }

    private static void ground(GameTestHelper helper) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    /** A ladder facing south at {@code foot}, already let go at {@code lean} degrees. */
    private static SiegeLadderEntity letGo(GameTestHelper helper, int sections, Vec3 foot, float lean) {
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

    private LadderFallGameTests() {
    }
}
