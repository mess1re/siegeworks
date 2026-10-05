package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(LadderConstructionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LadderCarryGameTests {
    // A carried ladder extends past its test area into neighbouring tests.
    private static final String BATCH = "ladder_carry";

    private LadderCarryGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void shortLadderIsTakenUpAtItsFootAndCarriedOverhead(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 1);
        Player carrier = carrier(helper, ladder.position());
        double walking = carrier.getAttributeValue(Attributes.MOVEMENT_SPEED);

        helper.assertTrue(LadderCarry.tryPickUp(carrier, ladder), "A ladder was not taken up at its foot");
        helper.assertTrue(LadderCarry.isCarrying(carrier) && ladder.isCarried(), "The ladder is not in the hands");
        helper.assertTrue(carrier.getAttributeValue(Attributes.MOVEMENT_SPEED) < walking,
                "Carrying a ladder did not slow its carrier");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(ladder.getY() > carrier.getY() + 1.83D,
                    "A short ladder is not carried over the head: " + (ladder.getY() - carrier.getY()));
            helper.assertTrue(ladder.getLeanAngleDegrees() > 80.0F,
                    "A short ladder is not carried level: " + ladder.getLeanAngleDegrees());
            helper.assertTrue(ladder.collisionGroups().isEmpty(), "A carried ladder still collides");
            finish(helper, ladder, carrier);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void longLadderIsHeldTwoThirdsUpWithItsFootDragging(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 4);
        Player carrier = carrier(helper, ladder.position());
        helper.assertTrue(LadderCarry.tryPickUp(carrier, ladder), "A long ladder was not taken up");
        helper.runAfterDelay(2, () -> {
            double footHeight = ladder.getY() - carrier.getY();
            double footBehind = ladder.position().subtract(carrier.position()).horizontalDistance();
            helper.assertTrue(Math.abs(footHeight) < 0.1D && footBehind > 5.0D,
                    "A long ladder does not drag its foot behind: " + footHeight + " up, " + footBehind + " behind");
            finish(helper, ladder, carrier);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void ladderIsTakenUpOnlyAtItsFoot(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 2);
        Player carrier = carrier(helper, ladder.position().add(4.0D, 0.0D, 0.0D));
        helper.assertTrue(!LadderCarry.tryPickUp(carrier, ladder) && !ladder.isCarried(),
                "A ladder was taken up away from its foot");
        finish(helper, ladder, carrier);
    }

    @GameTest(template = "empty", timeoutTicks = 80, batch = BATCH)
    public static void ladderSetDownStandsInFrontThenTips(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 1);
        Player carrier = carrier(helper, ladder.position());
        double walking = carrier.getAttributeValue(Attributes.MOVEMENT_SPEED);
        LadderCarry.tryPickUp(carrier, ladder);
        LadderCarry.putDown(carrier);

        helper.assertTrue(!ladder.isCarried() && !LadderCarry.isCarrying(carrier), "The ladder was not set down");
        helper.assertTrue(carrier.getAttributeValue(Attributes.MOVEMENT_SPEED) == walking,
                "Setting the ladder down did not give back the carrier's pace");
        helper.assertTrue(ladder.getLeanAngleDegrees() == 0.0F
                        && ladder.position().subtract(carrier.position()).horizontalDistance() > 0.5D,
                "The ladder was not stood up in front of its carrier");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(ladder.getLeanAngleDegrees() != 0.0F, "A ladder set down never tipped");
            finish(helper, ladder, carrier);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void carriedLadderIsKeptThroughHarmAndFallsWhenItsCarrierDies(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 1);
        Player carrier = carrier(helper, ladder.position());
        LadderCarry.tryPickUp(carrier, ladder);
        carrier.hurt(helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(ladder.isCarried(), "A hurt carrier let go of the ladder");
        float health = ladder.getHealth();
        ladder.hurt(helper.getLevel().damageSources().inWall(), 4.0F);
        helper.assertTrue(ladder.getHealth() == health, "A ceiling over a carried ladder hurt it");

        carrier.setHealth(0.0F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(!ladder.isCarried() && ladder.isAlive(),
                    "The ladder of a carrier who died did not fall where it was");
            finish(helper, ladder, carrier);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = BATCH)
    public static void carriedLadderLeavesAndComesBackWithItsCarrier(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 3);
        Player carrier = carrier(helper, ladder.position());
        LadderCarry.tryPickUp(carrier, ladder);

        LadderCarry.stash(carrier);
        helper.assertTrue(ladder.isRemoved() && !LadderCarry.isCarrying(carrier),
                "A carrier leaving left the ladder behind");
        LadderCarry.restore(carrier);
        SiegeLadderEntity back = LadderCarry.carried(carrier);
        helper.assertTrue(back != null && back.getSections() == 3,
                "The ladder did not come back into the hands of its carrier");
        finish(helper, back, carrier);
    }

    private static SiegeLadderEntity ladder(GameTestHelper helper, int sections) {
        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        helper.assertTrue(ladder != null, "Failed to create a ladder");
        ladder.setSections(sections);
        ladder.setPos(helper.absoluteVec(new Vec3(2.5D, 1.0D, 2.5D)));
        helper.assertTrue(level.addFreshEntity(ladder), "Failed to add the ladder");
        return ladder;
    }

    private static Player carrier(GameTestHelper helper, Vec3 at) {
        Player carrier = SiegeGameTestPlayers.create(helper.getLevel());
        carrier.setPos(at.x + 0.5D, at.y, at.z);
        return carrier;
    }

    /** Removes the ladder and its carrier so they don't overlap later tests. */
    private static void finish(GameTestHelper helper, Entity ladder, Player carrier) {
        if (ladder != null) {
            LadderCarry.drop((SiegeLadderEntity) ladder);
            ladder.discard();
        }
        helper.succeed();
    }
}
