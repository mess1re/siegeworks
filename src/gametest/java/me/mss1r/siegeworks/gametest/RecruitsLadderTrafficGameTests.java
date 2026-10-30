package me.mss1r.siegeworks.gametest;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.movement.ladder.LadderTraversalControl;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.Optional;

/** Real owned troops and their ordinary goals, including the approach outside the staging radius. */
@GameTestHolder("siegeworks_ladder_regression")
@PrefixGameTestTemplate(false)
public final class RecruitsLadderTrafficGameTests {
    @GameTest(template = "empty", timeoutTicks = 1400, batch = "ladder_owned_traffic")
    public static void followersKeepTheApproachAndClimbATallLadder(GameTestHelper helper) {
        traffic(helper, 1, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = "ladder_owned_traffic")
    public static void formationOrdersKeepTheApproachAndClimbATallLadder(GameTestHelper helper) {
        traffic(helper, 3, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = "ladder_owned_traffic")
    public static void repeatedMoveOrdersDoNotThrowTroopsOutOfTheQueue(GameTestHelper helper) {
        traffic(helper, 0, false);
    }

    @GameTest(template = "empty", timeoutTicks = 1400, batch = "ladder_owned_traffic")
    public static void troopsClimbOntoAThinPlatformWithoutStallingOnTheLadder(GameTestHelper helper) {
        traffic(helper, 0, true);
    }

    /** Everyone sent to the very spot where the ladder lets off: the line still flows instead of hanging on top. */
    @GameTest(template = "empty", timeoutTicks = 1400, batch = "ladder_owned_traffic")
    public static void troopsSentToTheLandingItselfDoNotHangOnTheLadder(GameTestHelper helper) {
        traffic(helper, 0, false, true);
    }

    /** Troops told to hold their ground stay there, though an enemy stands on the wall the ladder leads to. */
    @GameTest(template = "empty", timeoutTicks = 500, batch = "ladder_owned_traffic")
    public static void troopsHoldingTheirGroundDoNotClimbAfterAnEnemyOnTheWall(GameTestHelper helper) {
        GameTestQuiet.keep(helper);
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++)
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        for (int x = 2; x < 30; x++) for (int z = 23; z < 31; z++)
            for (int y = 1; y <= 14; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        var level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        ladder.setSections(4);
        ladder.applyYaw(0);
        ladder.setPos(helper.absoluteVec(new Vec3(15.5, 1, 18.5)));
        level.addFreshEntity(ladder);
        ServerPlayer commander = SiegeGameTestPlayers.create(level);
        commander.setPos(helper.absoluteVec(new Vec3(15.5, 1, 4)));
        level.addNewPlayer(commander);
        var enemy = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        enemy.setNoAi(true);
        enemy.setPersistenceRequired();
        enemy.setPos(helper.absoluteVec(new Vec3(15.5, 15, 26.5)));
        level.addFreshEntity(enemy);
        AbstractRecruitEntity[] troops = new AbstractRecruitEntity[6];
        Vec3[] held = new Vec3[troops.length];
        for (int i = 0; i < troops.length; i++) {
            troops[i] = (AbstractRecruitEntity) BuiltInRegistries.ENTITY_TYPE
                    .get(ResourceLocation.tryParse("recruits:recruit")).create(level);
            var recruit = troops[i];
            recruit.setPos(helper.absoluteVec(new Vec3(12.5 + i * 1.2, 1, 12)));
            recruit.setOwnerUUID(Optional.of(commander.getUUID()));
            recruit.setIsOwned(true);
            recruit.setPersistenceRequired();
            level.addFreshEntity(recruit);
            held[i] = recruit.position();
        }
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "Tall ladder did not settle");
            for (int i = 0; i < troops.length; i++) {
                troops[i].setHoldPos(held[i]);
                troops[i].setFollowState(2);
            }
        });
        for (int tick = 111; tick < 480; tick++) {
            helper.runAfterDelay(tick, () -> {
                for (int i = 0; i < troops.length; i++) {
                    var recruit = troops[i];
                    recruit.setTarget(enemy);
                    helper.assertTrue(!hasRoute(recruit) && !LadderTraversalControl.controls(recruit),
                            "A troop holding its ground set off up the ladder after the enemy on the wall: "
                                    + recruit.position() + " " + ladder.describeAutomatedClimber(recruit));
                    helper.assertTrue(recruit.getY() < held[i].y + 1.0D,
                            "A troop holding its ground left it for the wall: " + recruit.position());
                }
            });
        }
        helper.runAfterDelay(480, () -> {
            for (var troop : troops) troop.discard();
            enemy.discard();
            commander.discard();
            ladder.discard();
            helper.succeed();
        });
    }

    private static void traffic(GameTestHelper helper, int order, boolean suspendedPlatform) {
        traffic(helper, order, suspendedPlatform, false);
    }

    private static void traffic(GameTestHelper helper, int order, boolean suspendedPlatform, boolean toLanding) {
        GameTestQuiet.keep(helper);
        for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++)
            helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // Fifteen-metre ladder against a fourteen-metre wall, matching the user's steep setup.
        if (suspendedPlatform) {
            // Geometry read from the user's saved world: a one-block-thick platform and one
            // narrow pillar at its front, not an artificial solid wall hiding approach errors.
            for (int x = 11; x <= 19; x++) for (int z = 23; z < 31; z++)
                helper.setBlock(new BlockPos(x, 14, z), Blocks.STONE);
            for (int y = 1; y < 14; y++) helper.setBlock(new BlockPos(15, y, 23), Blocks.STONE);
        } else for (int x = 2; x < 30; x++) for (int z = 23; z < 31; z++)
            for (int y = 1; y <= 14; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        var level = helper.getLevel();
        // Registering a real owner enables normal world spawning around that player. Random
        // skeletons from another test's dark fixture are not part of a movement regression.
        // Keep troop AI and damage enabled; disable only unrelated natural world spawns.
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        ladder.setSections(4);
        ladder.applyYaw(suspendedPlatform ? -1.19774F : 0);
        ladder.setPos(helper.absoluteVec(new Vec3(15.5, 1, 18.5)));
        level.addFreshEntity(ladder);
        ServerPlayer commander = SiegeGameTestPlayers.create(level);
        commander.setPos(helper.absoluteVec(new Vec3(15.5, 15, 28)));
        // getOwner() resolves players through the world's player list.
        level.addNewPlayer(commander);
        AbstractRecruitEntity[] troops = new AbstractRecruitEntity[8];
        boolean[] entered = new boolean[troops.length];
        boolean[] finished = new boolean[troops.length];
        boolean[] checkedApproach = new boolean[troops.length];
        int[] previousState = new int[troops.length];
        int[] routeLosses = new int[troops.length];
        int[] stagnantTicks = new int[troops.length];
        Vec3[] lastPositions = new Vec3[troops.length];
        for (int i = 0; i < troops.length; i++) {
            troops[i] = (AbstractRecruitEntity) BuiltInRegistries.ENTITY_TYPE
                    .get(ResourceLocation.tryParse("recruits:recruit")).create(level);
            var recruit = troops[i];
            recruit.setPos(helper.absoluteVec(new Vec3(12.5 + i % 4 * 1.6, 1, 3 + i / 4 * 1.5)));
            recruit.setOwnerUUID(Optional.of(commander.getUUID()));
            recruit.setIsOwned(true);
            recruit.setPersistenceRequired();
            recruit.setAggroState(3); // passive: no unrelated hostile targets in this movement test
            level.addFreshEntity(recruit);
        }
        helper.runAfterDelay(110, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "Tall ladder did not settle");
            for (int i = 0; i < troops.length; i++) {
                if (toLanding) sendTo(troops[i], ladder.getAutomatedTopExit());
                else issue(troops[i], commander, order, i);
            }
        });
        for (int tick = 111; tick < 1300; tick++) {
            int now = tick;
            helper.runAfterDelay(tick, () -> {
                for (int i = 0; i < troops.length; i++) {
                    var recruit = troops[i];
                    if (order == 0 && now % 20 == 0 && !finished[i] && !toLanding) issue(recruit, commander, order, i);
                    boolean owned = LadderTraversalControl.ownsNavigation(recruit);
                    boolean guided = LadderTraversalControl.controls(recruit);
                    boolean routing = hasRoute(recruit);
                    if (routing && recruit.distanceToSqr(ladder.getAutomatedBottomApproach()) > 8 * 8
                            && recruit.getY() < ladder.getAutomatedTopExit().y - 1) {
                        checkedApproach[i] = true;
                        helper.assertTrue(owned, "The ordinary follow/formation goal still owns the ladder approach: "
                                + recruit.position() + " " + ladder.describeAutomatedClimber(recruit));
                    }
                    if (previousState[i] == 1 && !routing && !finished[i]
                            && recruit.getY() < ladder.getAutomatedTopExit().y - 1) routeLosses[i]++;
                    previousState[i] = routing ? 1 : 0;
                    helper.assertTrue(routeLosses[i] == 0, "A troop abandoned its ladder route before reaching the wall");
                    if (guided) entered[i] = true;
                    if (guided && lastPositions[i] != null && recruit.position().distanceToSqr(lastPositions[i]) < .0001)
                        stagnantTicks[i]++;
                    else stagnantTicks[i] = 0;
                    lastPositions[i] = recruit.position();
                    helper.assertTrue(stagnantTicks[i] < 80,
                            "A troop stalled on an unobstructed ladder for four seconds: " + recruit.position()
                                    + " " + ladder.describeAutomatedClimber(recruit));
                    if (entered[i] && !routing && recruit.getY() >= ladder.getAutomatedTopExit().y - .1)
                        finished[i] = true;
                    helper.assertTrue(recruit.isAlive() && recruit.getHealth() >= recruit.getMaxHealth() - .01,
                            "Tall ladder traffic injured a troop: " + recruit.position()
                                    + " source=" + recruit.getLastDamageSource() + " by=" + describe(recruit.getLastHurtByMob())
                                    + " " + ladder.describeAutomatedClimber(recruit));
                }
            });
        }
        helper.runAfterDelay(1300, () -> {
            for (int i = 0; i < troops.length; i++) {
                helper.assertTrue(checkedApproach[i], "The test did not cover a distant ladder approach");
                helper.assertTrue(entered[i] && finished[i], "Troop did not complete the tall ladder passage: "
                        + troops[i].position() + " " + ladder.describeAutomatedClimber(troops[i]));
                helper.assertTrue(troops[i].getFollowState() == order || order == 0,
                        "Climbing changed the original follow/formation order");
            }
            for (var troop : troops) troop.discard();
            commander.discard();
            ladder.discard();
            helper.succeed();
        });
    }

    private static void sendTo(AbstractRecruitEntity recruit, Vec3 goal) {
        recruit.setMovePos(BlockPos.containing(goal).below());
        recruit.setFollowState(0);
        recruit.setShouldMovePos(true);
    }

    private static void issue(AbstractRecruitEntity recruit, ServerPlayer commander, int order, int index) {
        Vec3 goal = commander.position().add((index % 4 - 1.5) * 1.1, 0, -(index / 4) * 1.1);
        if (order == 0) {
            recruit.setMovePos(BlockPos.containing(goal).below());
            recruit.setFollowState(0);
            recruit.setShouldMovePos(true);
        } else if (order == 3) {
            recruit.setHoldPos(goal);
            recruit.setFollowState(3);
            recruit.isInFormation = true;
        } else recruit.setFollowState(1);
    }

    private static String describe(net.minecraft.world.entity.LivingEntity attacker) {
        if (attacker == null) return "nobody";
        String owner = attacker instanceof AbstractRecruitEntity recruit ? " owner=" + recruit.getOwnerUUID() : "";
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()) + owner
                + " at " + attacker.position();
    }

    private static boolean hasRoute(AbstractRecruitEntity recruit) {
        try {
            var type = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsSiegeTraversal");
            var method = type.getDeclaredMethod("hasActiveLadderRoute", AbstractRecruitEntity.class);
            method.setAccessible(true);
            return (boolean) method.invoke(null, recruit);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
