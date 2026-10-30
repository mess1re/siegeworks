package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** A player walking up a ladder that leans on a wall gets over the top, instead of being let go where it touches. */
@GameTestHolder(LadderPlayerClimbGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderPlayerClimbGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ladder_climb";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SETTLE_TICKS = 80;
    private static final int CLIMB_TICKS = 400;
    /** Walking speed, in blocks per tick, as a player holding forward. */
    private static final double WALK = 0.13D;

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerClimbsOverTheWallTheLadderLeansOn(GameTestHelper helper) {
        climb(helper, 4, 1, 4.5D, LadderPlayerClimbGameTests::getsOnTheWall);
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerClimbsOverTheWallFromASteepLadder(GameTestHelper helper) {
        climb(helper, 4, 1, 6.5D, LadderPlayerClimbGameTests::getsOnTheWall);
    }

    /** The ladder's top rests on the face of a wall taller than it: the player stays on its upper rungs. */
    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerHoldsOnWhereTheLadderTouchesATallerWall(GameTestHelper helper) {
        climb(helper, 9, 1, 4.5D, (p, wallTop, wallFace, best, fell) -> {
            helper.assertTrue(fell < 0 && p.getY() > best - 1.0D,
                    "The player was let go where the ladder touches the wall: best "
                            + String.format("%.2f", best - wallTop) + " from the wall top, now "
                            + String.format("%.2f", p.getY() - wallTop) + ", fell at tick " + fell);
        });
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerHoldsOnATallerWallSteep(GameTestHelper helper) {
        climb(helper, 9, 1, 6.5D, LadderPlayerClimbGameTests::holdsOn);
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerHoldsOnATallerWallShallow(GameTestHelper helper) {
        climb(helper, 9, 1, 3.0D, LadderPlayerClimbGameTests::holdsOn);
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerHoldsOnATallerWallLongSteep(GameTestHelper helper) {
        climb(helper, 14, 3, 5.5D, LadderPlayerClimbGameTests::holdsOn);
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + CLIMB_TICKS + 20)
    public static void aPlayerHoldsOnATallerWallLongShallow(GameTestHelper helper) {
        climb(helper, 14, 3, 1.5D, LadderPlayerClimbGameTests::holdsOn);
    }

    private static void getsOnTheWall(Player p, double wallTop, double wallFace, double best, int fell) {
        if (Math.abs(p.getY() - wallTop) > 0.15D || p.getZ() <= wallFace + 0.5D) {
            throw new net.minecraft.gametest.framework.GameTestAssertException("The player did not get onto the wall: "
                    + String.format("%.2f", p.getY() - wallTop) + " from its top, " + String.format("%.2f", p.getZ() - wallFace)
                    + " past its face, best " + String.format("%.2f", best - wallTop) + ", fell at tick " + fell);
        }
    }

    private static void holdsOn(Player p, double wallTop, double wallFace, double best, int fell) {
        if (fell >= 0 || p.getY() <= best - 1.0D) {
            throw new net.minecraft.gametest.framework.GameTestAssertException(
                    "The player was let go where the ladder touches the wall: best " + String.format("%.2f", best - wallTop)
                            + " from the wall top, now " + String.format("%.2f", p.getY() - wallTop) + ", fell at tick " + fell);
        }
    }

    private interface Check {
        void check(Player player, double wallTop, double wallFace, double best, int fell);
    }

    /** A three-block-thick wall {@code height} blocks high at z = 8..10, and a player walking up a ladder against it. */
    private static void climb(GameTestHelper helper, int height, int sections, double footZ, Check check) {
        ground(helper);
        for (int x = 4; x <= 12; x++) {
            for (int z = 8; z <= 10; z++) {
                for (int y = 2; y < 2 + height; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        SiegeLadderEntity ladder = letGo(helper, sections, new Vec3(8.5D, 2.0D, footZ), 5.0F);
        double wallTop = helper.absolutePos(new BlockPos(0, 2 + height, 0)).getY();
        double wallFace = helper.absolutePos(new BlockPos(0, 0, 8)).getZ();
        Player[] player = new Player[1];
        double[] best = {Double.NEGATIVE_INFINITY};
        int[] lost = {-1};
        StringBuilder trace = new StringBuilder();
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            player[0] = SiegeGameTestPlayers.create(helper.getLevel());
            Vec3 foot = ladder.climbFoot();
            player[0].moveTo(foot.x, foot.y, foot.z - 0.6D, 0.0F, 0.0F);
            helper.getLevel().addFreshEntity(player[0]);
        });
        for (int tick = 1; tick <= CLIMB_TICKS; tick++) {
            int at = tick;
            helper.runAfterDelay(SETTLE_TICKS + tick, () -> {
                Player p = player[0];
                // Holding forward, toward the wall, until standing on it
                boolean onWall = p.getY() >= wallTop - 0.01D && p.getZ() > wallFace + 0.5D;
                p.xo = p.getX();
                p.yo = p.getY();
                p.zo = p.getZ();
                p.move(MoverType.SELF, new Vec3(0.0D, -0.08D, onWall ? 0.0D : WALK));
                best[0] = Math.max(best[0], p.getY());
                if (at % 10 == 0) {
                    trace.append(String.format(" %d:(y%.2f z%.2f)", at, p.getY() - wallTop, p.getZ() - wallFace));
                }
                if (lost[0] < 0 && best[0] > p.getY() + 1.0D) {
                    lost[0] = at;
                }
            });
        }
        helper.runAfterDelay(SETTLE_TICKS + CLIMB_TICKS + 1, () -> {
            Player p = player[0];
            LOGGER.info("[ladder climb] wall {}, sections {}, foot {}, lean {}, best {} from the wall top, fell at tick {}, path{}",
                    height, sections, footZ, String.format("%.1f", ladder.getLeanAngleDegrees()),
                    String.format("%.2f", best[0] - wallTop), lost[0], trace);
            check.check(p, wallTop, wallFace, best[0], lost[0]);
            p.discard();
            helper.succeed();
        });
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

    private LadderPlayerClimbGameTests() {
    }
}
