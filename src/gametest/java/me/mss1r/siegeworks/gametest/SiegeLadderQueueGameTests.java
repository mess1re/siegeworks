package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.api.SiegeClimbableControl.ClimbResult;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(SiegeLadderQueueGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeLadderQueueGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;

    private SiegeLadderQueueGameTests() {
    }

    @GameTest(template = "empty")
    public static void arrivedClimberIsNotHeldUpByOneStillWalking(GameTestHelper helper) {
        SiegeLadderEntity ladder = deployedLadder(helper);

        LivingEntity walking = spawnClimber(helper, ladder.position().add(24.0D, 0.0D, 24.0D));
        LivingEntity arrived = spawnClimber(helper, ladder.position());

        ladder.getAutomatedQueuePosition(walking, true);

        Vec3 slot = ladder.getAutomatedQueuePosition(arrived, true);
        arrived.moveTo(slot.x, slot.y, slot.z);
        ClimbResult result = ladder.advanceAutomatedClimber(arrived, true);

        helper.assertTrue(result != ClimbResult.WAITING,
                "A climber standing in its staging slot is still waiting behind one that has not"
                        + " arrived, so the whole queue follows the slowest walker");
        helper.assertTrue(result == ClimbResult.IN_PROGRESS,
                "An arrived climber was neither queued nor climbing: " + result);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aClimberStillWalkingDoesNotTakeTheLadder(GameTestHelper helper) {
        SiegeLadderEntity ladder = deployedLadder(helper);
        LivingEntity walking = spawnClimber(helper, ladder.position().add(24.0D, 0.0D, 24.0D));

        ladder.getAutomatedQueuePosition(walking, true);
        ClimbResult result = ladder.advanceAutomatedClimber(walking, true);

        helper.assertTrue(result == ClimbResult.WAITING,
                "A climber far from the ladder was sent onto it and would slide there through the"
                        + " air: " + result);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void queueSlotsCloseUpWhenAClimberLeaves(GameTestHelper helper) {
        SiegeLadderEntity ladder = deployedLadder(helper);
        LivingEntity first = spawnClimber(helper, ladder.position());
        Vec3 firstSlot = ladder.getAutomatedQueuePosition(first, true);
        // Five parallel lanes: the next member of THIS lane advances, not a neighbour
        // sent sideways across the queue every time someone leaves.
        occupyOtherLanes(helper, ladder);
        LivingEntity second = spawnClimber(helper, ladder.position());
        Vec3 secondSlot = ladder.getAutomatedQueuePosition(second, true);
        helper.assertTrue(firstSlot.distanceToSqr(secondSlot) > 1.0E-6D,
                "Two queued climbers were given the same place to stand");

        ladder.cancelAutomatedClimb(first);
        Vec3 movedUp = ladder.getAutomatedQueuePosition(second, true);

        helper.assertTrue(movedUp.distanceToSqr(firstSlot) < 1.0E-6D,
                "The queue did not close up after a climber left, so the line grows backwards while"
                        + " its front stands empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closingQueueDoesNotCarryReadinessFromTheOldSlot(GameTestHelper helper) {
        SiegeLadderEntity ladder = deployedLadder(helper);

        LivingEntity active = spawnClimber(helper, ladder.position());
        Vec3 entrance = ladder.getAutomatedQueuePosition(active, true);
        active.moveTo(entrance.x, entrance.y, entrance.z);
        helper.assertTrue(ladder.advanceAutomatedClimber(active, true) == ClimbResult.IN_PROGRESS,
                "The test climber did not enter the ladder");

        LivingEntity firstWaiting = spawnClimber(helper, ladder.position());
        Vec3 firstSlot = ladder.getAutomatedQueuePosition(firstWaiting, true);
        occupyOtherLanes(helper, ladder);
        LivingEntity secondWaiting = spawnClimber(helper, ladder.position());
        Vec3 secondSlot = ladder.getAutomatedQueuePosition(secondWaiting, true);
        firstWaiting.moveTo(firstSlot.x, firstSlot.y, firstSlot.z);
        secondWaiting.moveTo(secondSlot.x, secondSlot.y, secondSlot.z);
        helper.assertTrue(ladder.advanceAutomatedClimber(firstWaiting, true) == ClimbResult.WAITING,
                "The first waiting climber entered an occupied ladder");
        helper.assertTrue(ladder.advanceAutomatedClimber(secondWaiting, true) == ClimbResult.WAITING,
                "The second waiting climber entered an occupied ladder");

        ladder.cancelAutomatedClimb(firstWaiting);
        Vec3 newSlot = ladder.getAutomatedQueuePosition(secondWaiting, true);
        helper.assertTrue(newSlot.distanceToSqr(firstSlot) < 1.0E-6D
                        && secondWaiting.distanceToSqr(newSlot) > 0.64D,
                "The second climber did not inherit a different, unoccupied staging slot");

        for (int tick = 0; tick < 45; tick++) {
            ladder.advanceAutomatedClimber(active, true);
        }
        ladder.tick();

        helper.assertTrue(ladder.advanceAutomatedClimber(secondWaiting, true) == ClimbResult.WAITING,
                "A climber was promoted from the staging slot he occupied before the queue closed");
        helper.succeed();
    }

    private static void occupyOtherLanes(GameTestHelper helper, SiegeLadderEntity ladder) {
        for (int lane = 1; lane < 5; lane++) {
            LivingEntity waiting = spawnClimber(helper, ladder.position().add(24, 0, 24));
            ladder.getAutomatedQueuePosition(waiting, true);
        }
    }

    private static SiegeLadderEntity deployedLadder(GameTestHelper helper) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(ladder != null, "Failed to create siege ladder");
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (int x = 3; x <= 13; x++) {
            for (int z = 3; z <= 13; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        ladder.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(ladder);
        ladder.setSections(4);

        CompoundTag tag = new CompoundTag();
        ladder.addAdditionalSaveData(tag);
        tag.putFloat("LeanDegrees", 89.0F);
        tag.putBoolean("Settled", true);
        tag.putInt("DeployTicks", 200);
        ladder.readAdditionalSaveData(tag);

        helper.assertTrue(ladder.isReadyForAutomatedClimb(),
                "Test ladder is not ready to be climbed");
        return ladder;
    }

    private static LivingEntity spawnClimber(GameTestHelper helper, Vec3 position) {
        LivingEntity climber = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(climber != null, "Failed to create a test climber");
        climber.moveTo(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(climber);
        return climber;
    }
}
