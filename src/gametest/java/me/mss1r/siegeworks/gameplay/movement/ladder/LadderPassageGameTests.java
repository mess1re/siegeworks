package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.siegeworks.api.SiegeClimbableControl.ClimbResult;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_ladder")
@PrefixGameTestTemplate(false)
public final class LadderPassageGameTests {
    @GameTest(template = "empty")
    public static void anExitMustHaveAClearTransitionNotJustAnEmptyLanding(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        for (int x = 4; x <= 12; x++) for (int z = 12; z <= 18; z++)
            helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
        // A free floor exists behind this corner, but the first diagonal path to it crosses stone.
        helper.setBlock(new BlockPos(9, 6, 12), Blocks.STONE);
        helper.setBlock(new BlockPos(9, 7, 12), Blocks.STONE);
        for (int i = 0; i < 3; i++) {
            LivingEntity earlier = mob(helper, new Vec3(8, 2, 6.75));
            fixture.route.queuePosition(earlier, true);
            fixture.route.cancel(earlier);
            earlier.discard();
        }
        LivingEntity climber = mob(helper, new Vec3(8, 2, 6.75));
        ClimbResult result = ClimbResult.IN_PROGRESS;
        for (int tick = 0; tick < 300 && result != ClimbResult.COMPLETE; tick++) {
            result = fixture.route.advance(climber, true);
            helper.assertTrue(LadderTraversalCollision.clear(climber, climber.position()),
                    "The upper transition cut through the corner");
        }
        helper.assertTrue(result == ClimbResult.COMPLETE,
                "An inaccessible preferred landing stalled an otherwise open upper passage: "
                        + fixture.route.describe(climber.getUUID()));
        fixture.route.cancel(climber);
        climber.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancingAQueueDoesNotSwapEveryonesLane(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        LivingEntity[] line = new LivingEntity[12];
        Vec3[] slots = new Vec3[line.length];
        for (int i = 0; i < line.length; i++) {
            line[i] = mob(helper, new Vec3(8, 2, 4));
            slots[i] = fixture.route.queuePosition(line[i], true);
        }
        fixture.route.cancel(line[0]);
        for (int i = 1; i < line.length; i++) {
            Vec3 next = fixture.route.queuePosition(line[i], true);
            helper.assertTrue(Math.abs(next.x - slots[i].x) < .001,
                    "Queue advancement sent a waiting troop sideways into another lane: " + slots[i] + " -> " + next);
        }
        for (var entity : line) { fixture.route.cancel(entity); entity.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void aSettledLadderTraversesBothDirections(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.ladderTraversesBothDirections(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 900, batch = "recruits_wall")
    public static void recruitsOrderedOntoAWallClimbItsLadder(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.recruitsOrderedOntoAWallClimbItsLadder(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 2800, batch = "recruits_ladder")
    public static void recruitsTraverseLadderBothDirections(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.recruitsTraverseLadderBothDirections(helper, 6);
    }

    @GameTest(template = "empty", timeoutTicks = 2800, batch = "recruits_ladder_crowd")
    public static void twentyRecruitsTraverseLadderBothDirections(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.recruitsTraverseLadderBothDirections(helper, 20);
    }

    // Separate tracked cases: headless GameTestServer can finish before annotation retries run.
    @GameTest(template = "empty", timeoutTicks = 2800, batch = "recruits_ladder_crowd_second")
    public static void twentyRecruitsTraverseAtASecondPlacement(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.recruitsTraverseLadderBothDirections(helper, 20);
    }

    @GameTest(template = "empty", timeoutTicks = 2800, batch = "recruits_ladder_crowd_third")
    public static void twentyRecruitsTraverseAtAThirdPlacement(GameTestHelper helper) {
        me.mss1r.siegeworks.gametest.SiegeworksGameTests.recruitsTraverseLadderBothDirections(helper, 20);
    }

    @GameTest(template = "empty")
    public static void aTurnedSolidDoesNotCollideAsItsLargerWorldBoundingBox(GameTestHelper helper) {
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -1 : 1, z = (i & 4) == 0 ? -1 : 1;
            corners[i] = new Vec3((x - z) / Math.sqrt(2), (i & 2) == 0 ? 0 : 1, (x + z) / Math.sqrt(2));
        }
        LadderContactPolygon section = LadderContactPolygon.project(corners, new Vec3(0, 0, 1.2),
                new Vec3(1, 0, 0), new Vec3(0, 0, 1), .05);
        helper.assertTrue(section != null && section.contains(0, .5), "Missing real contact in the turned solid");
        helper.assertTrue(!section.contains(1, .5), "A world-AABB corner became a phantom contact");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aThinSolidBetweenTheRailsIsNotMissed(GameTestHelper helper) {
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) corners[i] = new Vec3((i & 1) == 0 ? -1 : 1,
                (i & 2) == 0 ? 0 : 1, (i & 4) == 0 ? .2 : .3);
        LadderContactPolygon section = LadderContactPolygon.project(corners, Vec3.ZERO,
                new Vec3(1, 0, 0), new Vec3(0, 0, 1), .5);
        helper.assertTrue(section != null && section.contains(0, .5), "A solid between three sample rails was missed");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void landingUsesTheActualSlabHeightAndFullBody(GameTestHelper helper) {
        LivingEntity mob = mob(helper, new Vec3(5, 4, 5));
        helper.setBlock(new BlockPos(8, 1, 8), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        Vec3 at = helper.absoluteVec(new Vec3(8.5, 2, 8.5));
        Vec3 landing = LadderTraversalCollision.standing(mob, at, at.y - 2);
        helper.assertTrue(landing != null && Math.abs(landing.y - (at.y - .5)) < .001,
                "Landing does not match the slab surface: " + landing);
        helper.setBlock(new BlockPos(8, 3, 8), Blocks.STONE);
        helper.assertTrue(!LadderTraversalCollision.clear(mob, landing), "Head clearance was ignored");
        mob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void directStagingWalkRequiresContinuousFloorAndHeadroom(GameTestHelper helper) {
        fixture(helper);
        LivingEntity mob = mob(helper, new Vec3(6.5, 2, 5));
        Vec3 start = mob.position(), end = helper.absoluteVec(new Vec3(6.5, 2, 10));
        helper.assertTrue(LadderTraversalCollision.walkable(mob, start, end), "A clear flat approach was rejected");
        helper.setBlock(new BlockPos(6, 1, 7), Blocks.AIR);
        helper.assertTrue(!LadderTraversalCollision.walkable(mob, start, end), "Direct steering crosses a hole");
        helper.setBlock(new BlockPos(6, 1, 7), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 3, 7), Blocks.STONE);
        helper.assertTrue(!LadderTraversalCollision.walkable(mob, start, end), "Direct steering ignores headroom");
        mob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBlockedEntranceCannotPullAMobThroughAWall(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        for (int x = 6; x <= 10; x++) for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(x, y, 7), Blocks.STONE);
        LivingEntity mob = mob(helper, new Vec3(8, 2, 6.65));
        for (int step = 0; step < 300; step++) {
            ClimbResult result = fixture.route.advance(mob, true);
            helper.assertTrue(result != ClimbResult.COMPLETE, "An entrance wall was crossed");
            helper.assertTrue(LadderTraversalCollision.clear(mob, mob.position()), "The route placed a body inside stone");
        }
        helper.assertTrue(mob.getZ() <= helper.absoluteVec(new Vec3(0, 0, 6.75)).z + .01,
                "The wall pulled the mob away from its waiting position");
        fixture.route.cancel(mob);
        mob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aWallTallerThanTheLadderNeverBecomesAnExit(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        for (int x = 6; x <= 10; x++) for (int y = 2; y <= 12; y++) helper.setBlock(new BlockPos(x, y, 11), Blocks.STONE);
        LivingEntity mob = mob(helper, new Vec3(8, 2, 6.75));
        helper.assertTrue(!fixture.route.routeAvailable(mob), "An unreachable wall was offered as an automated route");
        for (int step = 0; step < 400; step++) {
            helper.assertTrue(fixture.route.advance(mob, true) != ClimbResult.COMPLETE,
                    "An unclimbable wall was treated as a valid exit");
            helper.assertTrue(LadderTraversalCollision.clear(mob, mob.position()),
                    "A climber entered a wall taller than the ladder");
        }
        fixture.route.cancel(mob);
        mob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aMissingUpperLandingIsNotReportedAsASuccessfulExit(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        LivingEntity mob = mob(helper, new Vec3(8, 2, 6.75));
        helper.assertTrue(!fixture.route.routeAvailable(mob), "A missing landing was offered as an automated route");
        for (int step = 0; step < 300; step++) helper.assertTrue(
                fixture.route.advance(mob, true) != ClimbResult.COMPLETE,
                "A recruit was sent into the air because the upper landing was missing");
        fixture.route.cancel(mob);
        helper.assertTrue(!LadderTraversalControl.controls(mob), "Cancel left scripted movement enabled");
        mob.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    public static void aClosedUpperPassageReturnsTheWholeStreamSafely(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        for (int x = 4; x <= 12; x++) for (int z = 12; z <= 17; z++) helper.setBlock(new BlockPos(x, 5, z), Blocks.STONE);
        LivingEntity[] climbers = {mob(helper, new Vec3(8, 2, 6.75)), mob(helper, new Vec3(7.1, 2, 6.75))};
        for (LivingEntity climber : climbers) ((net.minecraft.world.entity.Mob) climber).setNoAi(false);
        boolean[] returned = new boolean[2];
        for (int tick = 1; tick <= 650; tick++) {
            int now = tick;
            helper.runAfterDelay(tick, () -> {
                if (now == 20) for (int x = 4; x <= 12; x++) for (int z = 11; z <= 17; z++)
                    for (int y = 6; y <= 8; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                for (int i = 0; i < climbers.length; i++) {
                    LivingEntity climber = climbers[i];
                    if (returned[i]) continue;
                    ClimbResult result = fixture.route.advance(climber, true);
                    helper.assertTrue(result != ClimbResult.COMPLETE, "Blocked exit reported completion");
                    helper.assertTrue(climber.isAlive() && climber.getHealth() >= climber.getMaxHealth() - .01F,
                            "Blocked passage injured a climber");
                    helper.assertTrue(LadderTraversalCollision.clear(climber, climber.position()), "Retreat cut through stone");
                    if (result == ClimbResult.FAILED) {
                        returned[i] = true;
                        helper.assertTrue(climber.getY() < helper.absoluteVec(new Vec3(0, 2.1, 0)).y,
                                "Failed traversal released its climber in mid-air");
                        climber.discard();
                    }
                }
            });
        }
        helper.runAfterDelay(651, () -> {
            helper.assertTrue(returned[0] && returned[1], "A blocked stream never returned to its entrance");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void movementControlExpiresWithoutChangingSavedMobFlags(GameTestHelper helper) {
        Fixture fixture = fixture(helper);
        LivingEntity mob = mob(helper, new Vec3(8, 2, 6.75));
        fixture.route.advance(mob, true);
        helper.assertTrue(LadderTraversalControl.controls(mob), "Entered climber has no movement lease");
        helper.assertTrue(!mob.isNoGravity(), "Traversal changed persistent gravity flags");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!LadderTraversalControl.controls(mob), "An abandoned route controls the mob forever");
            fixture.route.cancel(mob);
            mob.discard();
            helper.succeed();
        });
    }

    private static LivingEntity mob(GameTestHelper helper, Vec3 at) {
        var mob = EntityType.VILLAGER.create(helper.getLevel());
        mob.setNoAi(true);
        mob.setPos(helper.absoluteVec(at));
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static Fixture fixture(GameTestHelper helper) {
        for (int x = 2; x < 14; x++) for (int z = 2; z < 14; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        ladder.setPos(helper.absoluteVec(new Vec3(8, 2, 8)));
        LadderAutomatedTraversal route = new LadderAutomatedTraversal(new LadderAutomatedTraversal.Host() {
            @Override public Entity ladder() { return ladder; }
            @Override public Level level() { return helper.getLevel(); }
            @Override public int gameTick() { return (int) helper.getLevel().getGameTime(); }
            @Override public boolean ready() { return true; }
            @Override public double ladderLength() { return 6; }
            @Override public double leanAngleRadians() { return Math.PI / 4; }
            @Override public Vec3 ladderWorldPoint(double along) { return ladder.position().add(0, along / Math.sqrt(2), along / Math.sqrt(2)); }
            @Override public double platformY(double along) { return ladderWorldPoint(along).y; }
            @Override public Vec3 uphillVector() { return new Vec3(0, 0, 1); }
        });
        return new Fixture(route);
    }

    private record Fixture(LadderAutomatedTraversal route) {}
}
