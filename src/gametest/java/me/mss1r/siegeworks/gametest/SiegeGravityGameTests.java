package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_gravity")
@PrefixGameTestTemplate(false)
public final class SiegeGravityGameTests {
    @GameTest(template = "empty")
    public static void fullMachineTicksAccumulateGravityWithAndWithoutAPlayer(GameTestHelper helper) {
        for (var type : java.util.List.of(SiegeworksEntities.ARCBALLISTA_ENTITY.get(),
                SiegeworksEntities.CULVERIN_ENTITY.get(), SiegeworksEntities.HWACHA_ENTITY.get())) {
            for (boolean occupied : new boolean[]{false, true}) {
                var siege = create(helper, type);
                Player player = SiegeGameTestPlayers.createRideable(helper.getLevel());
                if (occupied) {
                    helper.assertTrue(player.startRiding(siege, true), "The fixture must accept its operator");
                    helper.assertTrue(siege.getControllingPassenger() == player,
                            "The fixture must exercise vanilla's player-controlled branch");
                }
                double startY = siege.getY();
                for (int tick = 1; tick <= 8; tick++) {
                    siege.tick();
                    assertFall(helper, siege, startY, tick, "occupied=" + occupied);
                    if (occupied) player.rideTick();
                }
                player.discard();
                siege.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boardingAndLeavingAMachinePreserveItsFallVelocity(GameTestHelper helper) {
        var siege = create(helper, SiegeworksEntities.ARCBALLISTA_ENTITY.get());
        Player player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        double startY = siege.getY();
        for (int tick = 1; tick <= 8; tick++) {
            if (tick == 3) helper.assertTrue(player.startRiding(siege, true), "The operator must board while falling");
            if (tick == 6) player.stopRiding();
            siege.tick();
            assertFall(helper, siege, startY, tick, "boarding or leaving");
            if (player.isPassenger()) player.rideTick();
        }
        player.discard();
        siege.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aDraftTeamWithAPlayerAlsoRetainsItsFallAcceleration(GameTestHelper helper) {
        var siege = create(helper, SiegeworksEntities.CULVERIN_ENTITY.get());
        var horse = EntityType.HORSE.create(helper.getLevel());
        horse.setTamed(true);
        Player player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        helper.assertTrue(horse.startRiding(siege, true) && player.startRiding(horse, true),
                "The fixture must have a player riding its draft mount");
        helper.assertTrue(siege.getControllingPassenger() == player,
                "The team must expose the nested player as its controller");
        double startY = siege.getY();
        for (int tick = 1; tick <= 8; tick++) {
            siege.tick();
            assertFall(helper, siege, startY, tick, "draft team");
        }
        player.discard();
        horse.discard();
        siege.discard();
        helper.succeed();
    }

    private static AbstractSiegeEntity create(GameTestHelper helper,
                                              EntityType<? extends AbstractSiegeEntity> type) {
        var siege = type.create(helper.getLevel());
        siege.setPos(helper.absoluteVec(new Vec3(20, 20, 20)));
        siege.applyYaw(0);
        siege.setDeltaMovement(Vec3.ZERO);
        return siege;
    }

    private static void assertFall(GameTestHelper helper, AbstractSiegeEntity siege,
                                   double startY, int tick, String situation) {
        double expectedSpeed = 0;
        double expectedY = startY;
        for (int i = 0; i < tick; i++) {
            expectedSpeed = expectedSpeed * .98D - .08D;
            expectedY += expectedSpeed;
        }
        helper.assertTrue(Math.abs(siege.getDeltaMovement().y - expectedSpeed) < 1E-6,
                siege.getType() + " lost fall acceleration at tick " + tick + " (" + situation
                        + "): velocity=" + siege.getDeltaMovement().y + ", expected=" + expectedSpeed);
        helper.assertTrue(Math.abs(siege.getY() - expectedY) < 1E-6,
                siege.getType() + " fell at the wrong rate (" + situation + "): y=" + siege.getY()
                        + ", expected=" + expectedY);
    }
}
