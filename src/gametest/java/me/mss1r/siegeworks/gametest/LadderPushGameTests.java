package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.ladder.LadderPush;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/** A defender on the wall pushes a siege ladder off it with a pole held braced against its top. */
@GameTestHolder(LadderPushGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderPushGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ladder_climb";
    private static final int SETTLE_TICKS = 80;
    private static final int HOLD_TICKS = 200;
    /**
     * Each test builds its wall well beyond the small template, over its neighbours' ground, and ladders pushed over
     * fall a long way: every one runs in a batch of its own.
     */
    private static final String BATCH = "ladder_push_";

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + HOLD_TICKS + 120, batch = BATCH + "pole")
    public static void aPoleBracedFromTheWallPushesTheLadderOff(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladderAgainstAWall(helper);
        Player[] defender = new Player[1];
        float[] leaning = {0};
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "The ladder did not settle against the wall: lean "
                    + ladder.getLeanAngleDegrees() + ", at " + ladder.position() + ", top " + ladder.topPoint());
            leaning[0] = ladder.getLeanAngleDegrees();
            defender[0] = onTheWall(helper, ladder, new ItemStack(SiegeworksItems.FORKED_POLE.get()));
            helper.assertTrue(LadderPush.isPusher(defender[0], defender[0].getMainHandItem())
                            && LadderPush.canBrace(ladder, defender[0]),
                    "The defender cannot brace: pusher " + LadderPush.isPusher(defender[0], defender[0].getMainHandItem())
                            + ", at " + defender[0].position() + ", top " + ladder.topPoint() + ", uphill "
                            + ladder.uphillVector() + ", eye distance " + defender[0].getEyePosition().distanceTo(ladder.topPoint()));
        });
        holdUse(helper, ladder, defender);
        helper.runAfterDelay(SETTLE_TICKS + HOLD_TICKS + 100, () -> {
            helper.assertTrue(ladder.getLeanAngleDegrees() * leaning[0] < 0.0F,
                    "The ladder was not pushed over: it leaned " + leaning[0] + ", now " + ladder.getLeanAngleDegrees());
            helper.assertTrue(defender[0].getMainHandItem().getDamageValue() == 1,
                    "Pushing the ladder off did not wear the pole");
            helper.succeed();
        });
    }

    /** Bracing eases the ladder off the wall; let go before the last push, and it falls back onto it. */
    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + 120, batch = BATCH + "let_go")
    public static void aLadderLetGoHalfwayFallsBackOntoTheWall(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladderAgainstAWall(helper);
        Player[] defender = new Player[1];
        float[] leaning = {0, 0};
        int halfway = LadderPush.requiredTicks(30.0F, 0) * 3 / 4;
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            leaning[0] = ladder.getLeanAngleDegrees();
            defender[0] = onTheWall(helper, ladder, new ItemStack(SiegeworksItems.FORKED_POLE.get()));
        });
        for (int tick = 1; tick <= halfway; tick++) {
            int at = tick;
            helper.runAfterDelay(SETTLE_TICKS + tick, () -> use(ladder, defender[0], at));
        }
        helper.runAfterDelay(SETTLE_TICKS + halfway + 1, () -> {
            leaning[1] = ladder.getLeanAngleDegrees();
            helper.assertTrue(Math.abs(leaning[1]) < Math.abs(leaning[0]) - 3.0F,
                    "Bracing did not ease the ladder off the wall: it leaned " + leaning[0] + ", now " + leaning[1]);
        });
        helper.runAfterDelay(SETTLE_TICKS + halfway + 60, () -> {
            helper.assertTrue(Math.abs(ladder.getLeanAngleDegrees() - leaning[0]) < 1.0F,
                    "Let go halfway, the ladder did not fall back onto the wall: it leaned " + leaning[0]
                            + ", was eased to " + leaning[1] + ", now " + ladder.getLeanAngleDegrees());
            helper.succeed();
        });
    }

    /** A long ladder with a man on it goes over too, given time, and he comes down with it. */
    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + HOLD_TICKS * 2 + 160, batch = BATCH + "laden")
    public static void aLongLadenLadderGoesOverWithItsClimber(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladderAgainstAWall(helper, 3, 11, 4.0D);
        Player[] defender = new Player[1];
        Player[] climber = new Player[1];
        float[] leaning = {0};
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "The long ladder did not settle against the wall");
            leaning[0] = ladder.getLeanAngleDegrees();
            defender[0] = onTheWall(helper, ladder, new ItemStack(SiegeworksItems.FORKED_POLE.get()), 11);
            climber[0] = SiegeGameTestPlayers.create(helper.getLevel());
            Vec3 foot = ladder.climbFoot();
            climber[0].moveTo(foot.x, foot.y, foot.z - 0.6D, 0.0F, 0.0F);
            helper.getLevel().addFreshEntity(climber[0]);
        });
        for (int tick = 1; tick <= HOLD_TICKS * 2; tick++) {
            int at = tick;
            helper.runAfterDelay(SETTLE_TICKS + tick, () -> {
                Player p = climber[0];
                p.xo = p.getX();
                p.yo = p.getY();
                p.zo = p.getZ();
                p.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0.0D, -0.08D, at < 60 ? 0.13D : 0.0D));
                if (at > 60) use(ladder, defender[0], at);
            });
        }
        helper.runAfterDelay(SETTLE_TICKS + HOLD_TICKS * 2 + 140, () -> {
            helper.assertTrue(ladder.getLeanAngleDegrees() * leaning[0] < 0.0F,
                    "The laden ladder was not pushed over: it leaned " + leaning[0] + ", now " + ladder.getLeanAngleDegrees());
            helper.assertTrue(climber[0].getY() < helper.absolutePos(new BlockPos(0, 3, 0)).getY(),
                    "The climber stayed up after the ladder went over: " + climber[0].position());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + HOLD_TICKS + 40, batch = BATCH + "below")
    public static void aLadderCannotBePushedOffFromBelow(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladderAgainstAWall(helper);
        Player[] below = new Player[1];
        float[] leaning = {0};
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            leaning[0] = ladder.getLeanAngleDegrees();
            below[0] = SiegeGameTestPlayers.create(helper.getLevel());
            Vec3 foot = ladder.climbFoot();
            below[0].moveTo(foot.x, foot.y, foot.z - 1.0D, 0.0F, 0.0F);
            below[0].setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.FORKED_POLE.get()));
            helper.getLevel().addFreshEntity(below[0]);
        });
        holdUse(helper, ladder, below);
        helper.runAfterDelay(SETTLE_TICKS + HOLD_TICKS + 20, () -> {
            helper.assertTrue(Math.abs(ladder.getLeanAngleDegrees() - leaning[0]) < 1.0F,
                    "A pole from below the ladder pushed it off the wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = SETTLE_TICKS + HOLD_TICKS + 40, batch = BATCH + "stick")
    public static void aStickDoesNotPushALadder(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladderAgainstAWall(helper);
        Player[] defender = new Player[1];
        float[] leaning = {0};
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            leaning[0] = ladder.getLeanAngleDegrees();
            defender[0] = onTheWall(helper, ladder, new ItemStack(Items.STICK));
        });
        holdUse(helper, ladder, defender);
        helper.runAfterDelay(SETTLE_TICKS + HOLD_TICKS + 20, () -> {
            helper.assertTrue(Math.abs(ladder.getLeanAngleDegrees() - leaning[0]) < 1.0F,
                    "A plain stick pushed the ladder off the wall");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = BATCH + "times")
    public static void aShallowOrLadenLadderTakesLongerToPush(GameTestHelper helper) {
        int upright = LadderPush.requiredTicks(15.0F, 0);
        int shallow = LadderPush.requiredTicks(45.0F, 0);
        int laden = LadderPush.requiredTicks(15.0F, 3);
        helper.assertTrue(upright == 40 && shallow == 60 && laden == 100,
                "Bracing times by lean and climbers: " + upright + ", " + shallow + ", " + laden
                        + " ticks, not 40, 60 and 100");
        helper.succeed();
    }

    /**
     * Holds use with the held item at the ladder: a forked pole braces every tick while held, anything else uses the
     * ladder every four ticks, as holding the use button does.
     */
    private static void holdUse(GameTestHelper helper, SiegeLadderEntity ladder, Player[] player) {
        for (int tick = 1; tick <= HOLD_TICKS; tick++) {
            int at = tick;
            helper.runAfterDelay(SETTLE_TICKS + tick, () -> use(ladder, player[0], at));
        }
    }

    private static void use(SiegeLadderEntity ladder, Player player, int tick) {
        if (player.getMainHandItem().getItem() instanceof me.mss1r.siegeworks.item.ForkedPoleItem) {
            // As the held pole does: braces against the ladder top in sight, not the ladder it was given
            SiegeLadderEntity seen = LadderPush.ladderLookedAt(player);
            if (seen != null) seen.braceWith(player, InteractionHand.MAIN_HAND);
        } else if (tick % 4 == 1) {
            ladder.interact(player, InteractionHand.MAIN_HAND);
        }
    }

    /** A defender on top of the wall by the ladder's top, holding {@code item}. */
    private static Player onTheWall(GameTestHelper helper, SiegeLadderEntity ladder, ItemStack item) {
        return onTheWall(helper, ladder, item, 5);
    }

    private static Player onTheWall(GameTestHelper helper, SiegeLadderEntity ladder, ItemStack item, int height) {
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        Vec3 top = ladder.topPoint();
        double wallTop = helper.absolutePos(new BlockPos(0, 2 + height, 0)).getY();
        player.moveTo(top.x, wallTop, helper.absolutePos(new BlockPos(0, 0, 9)).getZ() + 0.5D, 180.0F, 30.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, top);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    /** A one-section ladder leaning on a wall five blocks high at z = 8..10, its top over the wall's edge. */
    private static SiegeLadderEntity ladderAgainstAWall(GameTestHelper helper) {
        return ladderAgainstAWall(helper, 1, 5, 5.0D);
    }

    /** A ladder of {@code sections} leaning on a wall {@code height} blocks high at z = 8..10, its foot at z = {@code footZ}. */
    private static SiegeLadderEntity ladderAgainstAWall(GameTestHelper helper, int sections, int height, double footZ) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        for (int x = 4; x <= 12; x++) {
            for (int z = 8; z <= 10; z++) {
                for (int y = 2; y < 2 + height; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(ladder != null, "Failed to create siege ladder");
        ladder.setSections(sections);
        ladder.applyYaw(0.0F);
        Vec3 position = helper.absoluteVec(new Vec3(8.5D, 2.0D, footZ));
        ladder.setPos(position.x, position.y, position.z);
        CompoundTag tag = new CompoundTag();
        ladder.addAdditionalSaveData(tag);
        tag.putInt("DeployTicks", 35);
        tag.putFloat("LeanDegrees", 5.0F);
        ladder.readAdditionalSaveData(tag);
        helper.getLevel().addFreshEntity(ladder);
        return ladder;
    }

    private LadderPushGameTests() {
    }
}
