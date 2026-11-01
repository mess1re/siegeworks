package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureGeometry;
import me.mss1r.siegeworks.gameplay.ladder.LadderPush;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(LadderBraceTargetGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderBraceTargetGameTests {
    public static final String NAMESPACE = "siegeworks_pole_regression";

    @GameTest(template = "empty")
    public static void reachableShaftWorksWhenTheTipIsOutOfReach(GameTestHelper helper) {
        reachableShaft(helper, 0, 30);
    }

    @GameTest(template = "empty")
    public static void reachableShaftWorksAfterTurningTheLadder(GameTestHelper helper) {
        reachableShaft(helper, 90, 30);
    }

    @GameTest(template = "empty")
    public static void reachableShaftWorksWithOppositeLean(GameTestHelper helper) {
        reachableShaft(helper, 45, -30);
    }

    @GameTest(template = "empty")
    public static void aShortLadderUsesTheWallEdgeToo(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30, 1);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .7));
        helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                "A short ladder must allow a reachable brace above the defender's footing");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aLongLadderCanBeBracedBelowItsMiddleAtALowWall(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .3));
        helper.assertTrue(player.getEyePosition().distanceTo(ladder.topPoint()) > 8,
                "The fixture must leave much of the long ladder above the low wall");
        var lookedAt = LadderPush.ladderLookedAt(player);
        boolean canBrace = LadderPush.canBrace(ladder, player);
        helper.assertTrue(lookedAt == ladder && canBrace,
                "The wall edge, not a fraction of the ladder length, must define the reachable brace: target="
                        + lookedAt + ", canBrace=" + canBrace + ", eye=" + player.getEyePosition()
                        + ", contact=" + ladder.position().lerp(ladder.topPoint(), .3)
                        + ", reach=" + me.mss1r.siegeworks.platform.MinecraftVersionCompat.entityInteractionRange(player));
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyLengthUsesTheSameWallEdge(GameTestHelper helper) {
        for (int sections = 1; sections <= 4; sections++) {
            SiegeLadderEntity ladder = ladder(helper, 0, 30, sections);
            Vec3 top = ladder.topPoint();
            Vec3 contact = ladder.position().lerp(top, 3.5 / (top.y - ladder.getY()));
            Player player = defender(helper, ladder, contact);
            helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                    "The same low wall must permit a reachable brace with " + sections + " ladder sections");
            ladder.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void solidStructureGeometryCanProvideTheFooting(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .6));
        BlockPos floor = BlockPos.containing(player.position().add(0, -.5, 0));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.AIR.defaultBlockState());
        var platform = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        platform.setPos(player.getX(), player.getY(), player.getZ());
        helper.getLevel().addFreshEntity(platform);
        Vec3 surface = StructureCollisionResolver.clipSegment(platform, player.position().add(0, 30, 0), player.position())
                .orElseThrow(() -> new AssertionError("The tower must have a real supporting surface"));
        platform.setPos(platform.getX(), platform.getY() + player.getY() - surface.y, platform.getZ());
        helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                "Solid Axiomata geometry must provide footing without a vanilla block below the defender");
        platform.discard();
        helper.assertTrue(!LadderPush.canBrace(ladder, player), "Removing the platform must remove the brace's footing");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theWallDoesNotExtendThePolesReach(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .6);
        Player player = defender(helper, ladder, contact);
        player.setPos(player.getX(), player.getY(), player.getZ() + 5);
        BlockPos floor = BlockPos.containing(player.position().add(0, -.5, 0));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        player.lookAt(EntityAnchorArgument.Anchor.EYES, contact);
        helper.assertTrue(LadderPush.ladderLookedAt(player) == null && !LadderPush.canBrace(ladder, player),
                "A defender on the wall must still reach the actual contact with the pole");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void solidStructureGeometryAlsoObstructsThePole(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .6);
        Player player = defender(helper, ladder, contact);
        var obstacle = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        Vec3 middle = player.getEyePosition().lerp(contact, .5);
        obstacle.setPos(middle.x, middle.y - 3, middle.z);
        helper.getLevel().addFreshEntity(obstacle);
        var group = StructureGeometry.solid(obstacle).get(0);
        var part = group.parts().get(0);
        Vec3 center = obstacle.collisionTransform().toWorld(group.fromPart(part, part.box().getCenter()));
        Vec3 shift = middle.subtract(center);
        obstacle.setPos(obstacle.getX() + shift.x, obstacle.getY() + shift.y, obstacle.getZ() + shift.z);
        helper.assertTrue(StructureCollisionResolver.clipSegment(obstacle.collisionTransform(),
                        StructureGeometry.solid(obstacle), player.getEyePosition(), contact, 0).isPresent(),
                "The fixture must place real solid geometry across the pole's path");
        helper.assertTrue(LadderPush.ladderLookedAt(player) == null && !LadderPush.canBrace(ladder, player),
                "The pole must not pass through another machine's solid geometry");
        obstacle.discard();
        helper.assertTrue(LadderPush.canBrace(ladder, player), "Removing the obstacle must restore the clear contact");
        ladder.discard();
        helper.succeed();
    }

    private static void reachableShaft(GameTestHelper helper, float yaw, float lean) {
        SiegeLadderEntity ladder = ladder(helper, yaw, lean);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .6);
        Player player = defender(helper, ladder, contact);
        helper.assertTrue(player.getEyePosition().distanceTo(ladder.topPoint()) > 4.5,
                "The fixture must put the tip beyond the pole's reach");
        helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                "A reachable rail above the wall must allow bracing without reaching the very tip; root=" + ladder.position()
                        + ", top=" + ladder.topPoint() + ", stance=" + player.position() + ", eye=" + player.getEyePosition()
                        + ", look=" + player.getLookAngle() + ", contact=" + contact
                        + ", uphill=" + ladder.uphillVector() + ", block=" + player.level().clip(
                        new net.minecraft.world.level.ClipContext(player.getEyePosition(), contact,
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getType());
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aimingAtTheFootDoesNotBrace(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .1));
        helper.assertTrue(!LadderPush.canBrace(ladder, player), "A brace at the foot must be rejected");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aDefenderMustHaveFootingInsteadOfFloatingBesideTheLadder(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .6));
        BlockPos floor = BlockPos.containing(player.position().add(0, -.5, 0));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.AIR.defaultBlockState());
        helper.assertTrue(LadderPush.ladderLookedAt(player) == null && !LadderPush.canBrace(ladder, player),
                "An airborne defender must not brace against a ladder");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aJumpCannotStartABrace(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player player = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .6));
        player.setDeltaMovement(0, .42, 0);
        helper.assertTrue(!LadderPush.canBrace(ladder, player), "Jumping must release the footing for a brace");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aReachableRungBelowTheParapetWorksAcrossAGap(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .6);
        Player player = defender(helper, ladder, contact);
        Vec3 stance = contact.add(ladder.uphillVector());
        player.setPos(stance.x, player.getY(), stance.z);
        BlockPos floor = BlockPos.containing(player.position().add(0, -1, 0));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        Vec3 top = ladder.topPoint();
        Vec3 belowWall = ladder.position().lerp(top, (player.getY() - .2 - ladder.getY()) / (top.y - ladder.getY()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, belowWall);
        helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                "A clear gap must allow a reachable contact below the parapet instead of imposing an artificial height cutoff");
        BlockPos obstacle = BlockPos.containing(player.getEyePosition().lerp(belowWall, .5));
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!LadderPush.canBrace(ladder, player), "The same contact must fail when the wall obstructs it");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowerContactNeedsMoreEffortWithoutProgressJumps(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Player low = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .3));
        Player high = defender(helper, ladder, ladder.position().lerp(ladder.topPoint(), .8));
        LadderPush lowPush = new LadderPush();
        LadderPush highPush = new LadderPush();
        for (int tick = 0; tick <= 20; tick++) {
            lowPush.brace(ladder, low, InteractionHand.MAIN_HAND, tick);
            highPush.brace(ladder, high, InteractionHand.MAIN_HAND, tick);
        }
        double expectedRatio = .3 / .8;
        helper.assertTrue(highPush.progress() > .2 && Math.abs(lowPush.progress() / highPush.progress() - expectedRatio) < .01,
                "Equal held time must contribute effort in proportion to contact height: low=" + lowPush.progress()
                        + ", high=" + highPush.progress());
        double before = lowPush.progress();
        low.lookAt(EntityAnchorArgument.Anchor.EYES, ladder.position().lerp(ladder.topPoint(), .34));
        lowPush.brace(ladder, low, InteractionHand.MAIN_HAND, 20);
        helper.assertTrue(Math.abs(lowPush.progress() - before) < 1.0E-8,
                "Moving the contact without elapsed time must not recalculate completed progress");
        lowPush.brace(ladder, low, InteractionHand.MAIN_HAND, 21);
        helper.assertTrue(lowPush.progress() > before && lowPush.progress() < before + .01,
                "A changed contact must affect only subsequent effort");
        lowPush.activeBracer(30);
        helper.assertTrue(lowPush.progress() == 0, "Letting go must clear the brace");
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBlockBetweenThePoleAndTheLadderStopsBracing(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .6);
        Player player = defender(helper, ladder, contact);
        BlockPos obstacle = BlockPos.containing(player.getEyePosition().lerp(contact, .5));
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
        helper.assertTrue(LadderPush.ladderLookedAt(player) == null && !LadderPush.canBrace(ladder, player),
                "The pole must not brace through a solid block");
        helper.getLevel().setBlockAndUpdate(obstacle, Blocks.AIR.defaultBlockState());
        ladder.discard();
        helper.succeed();
    }

    private static SiegeLadderEntity ladder(GameTestHelper helper, float yaw, float lean) {
        return ladder(helper, yaw, lean, 4);
    }

    private static SiegeLadderEntity ladder(GameTestHelper helper, float yaw, float lean, int sections) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        Vec3 root = helper.absoluteVec(new Vec3(20, 2, 20));
        ladder.setPos(root.x, root.y, root.z);
        ladder.applyYaw(yaw);
        CompoundTag state = new CompoundTag();
        ladder.addAdditionalSaveData(state);
        state.putInt("Sections", sections);
        state.putInt("DeployTicks", 35);
        state.putFloat("LeanDegrees", lean);
        ladder.readAdditionalSaveData(state);
        helper.getLevel().addFreshEntity(ladder);
        return ladder;
    }

    private static Player defender(GameTestHelper helper, SiegeLadderEntity ladder, Vec3 contact) {
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        Vec3 stance = contact.add(ladder.uphillVector().scale(2));
        double floorY = Math.floor(contact.y - 1.3);
        player.setPos(stance.x, floorY, stance.z);
        BlockPos floor = BlockPos.containing(stance.x, floorY - 1, stance.z);
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.FORKED_POLE.get()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, contact);
        return player;
    }
}
