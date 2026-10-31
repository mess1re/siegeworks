package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
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
//? if forge {
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

    private static void reachableShaft(GameTestHelper helper, float yaw, float lean) {
        SiegeLadderEntity ladder = ladder(helper, yaw, lean);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .65);
        Player player = defender(helper, ladder, contact);
        helper.assertTrue(player.getEyePosition().distanceTo(ladder.topPoint()) > 4.5,
                "The fixture must put the tip beyond the pole's reach");
        helper.assertTrue(LadderPush.ladderLookedAt(player) == ladder && LadderPush.canBrace(ladder, player),
                "A reachable upper rail must allow bracing without reaching the very tip; root=" + ladder.position()
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
    public static void aBlockBetweenThePoleAndTheLadderStopsBracing(GameTestHelper helper) {
        SiegeLadderEntity ladder = ladder(helper, 0, 30);
        Vec3 contact = ladder.position().lerp(ladder.topPoint(), .65);
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
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        Vec3 root = helper.absoluteVec(new Vec3(20, 2, 20));
        ladder.setPos(root.x, root.y, root.z);
        ladder.applyYaw(yaw);
        CompoundTag state = new CompoundTag();
        ladder.addAdditionalSaveData(state);
        state.putInt("Sections", 4);
        state.putInt("DeployTicks", 35);
        state.putFloat("LeanDegrees", lean);
        ladder.readAdditionalSaveData(state);
        helper.getLevel().addFreshEntity(ladder);
        return ladder;
    }

    private static Player defender(GameTestHelper helper, SiegeLadderEntity ladder, Vec3 contact) {
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        Vec3 stance = contact.add(ladder.uphillVector().scale(2));
        player.setPos(stance.x, contact.y - 1.8, stance.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.FORKED_POLE.get()));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, contact);
        return player;
    }
}
