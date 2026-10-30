package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
//? if <1.21 {
/*import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.UUID;

@GameTestHolder(TowerInteractionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class TowerInteractionGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_tower";
    private static final double FIRST_FLOOR_Y = 19.0D / 16.0D;
    private static final double SECOND_FLOOR_Y = 160.0D / 16.0D;
    private static final double THIRD_FLOOR_Y = 275.0D / 16.0D;
    private static final double REAR_BOARDING_Z = 80.0D / 16.0D;

    private TowerInteractionGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void playersCannotReserveInteriorSeats(GameTestHelper helper) {
        TestTower test = createTowerAndPlayer(helper, new Vec3(0.0D, THIRD_FLOOR_Y, 0.0D));

        helper.assertTrue(!test.tower.reserveInteriorSeat(test.player),
                "A player was allowed to reserve an interior transport seat");
        helper.assertTrue(!test.tower.isInteriorPassenger(test.player),
                "A player was classified as an interior transport passenger");
        helper.assertTrue(test.player.getVehicle() == null,
                "Checking the interior seat unexpectedly mounted the player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void thirdFloorTowerClickControlsTheRampWithoutASeat(GameTestHelper helper) {
        TestTower test = createTowerAndPlayer(helper, new Vec3(0.0D, THIRD_FLOOR_Y, 0.0D));
        Vec3 unrelatedThirdFloorHit = new Vec3(0.0D, THIRD_FLOOR_Y, -54.0D / 16.0D);
        Vec3 relativeHit = test.tower.collisionTransform().toWorld(unrelatedThirdFloorHit)
                .subtract(test.tower.position());

        test.tower.interactAt(test.player, relativeHit, InteractionHand.MAIN_HAND);

        helper.assertTrue(test.tower.isBridgeOpen(), "Clicking the tower from the third floor did not open the ramp");
        helper.assertTrue(test.player.getVehicle() == null, "Using the third-floor bridge control mounted the player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void flyingPlayerBesideThirdFloorCannotControlTheRamp(GameTestHelper helper) {
        TestTower test = createTowerAndPlayer(helper, new Vec3(6.0D, THIRD_FLOOR_Y, 0.0D));
        test.player.getAbilities().flying = true;

        InteractionResult result = test.tower.interactAt(test.player, Vec3.ZERO, InteractionHand.MAIN_HAND);

        helper.assertTrue(result == InteractionResult.PASS,
                "A player flying beside the third floor must not use its bridge control");
        helper.assertTrue(!test.tower.isBridgeOpen(),
                "A player outside the tower footprint changed the bridge state");
        helper.assertTrue(test.player.getVehicle() == null,
                "A player flying beside the tower was boarded into the pushing crew");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void shiftThirdFloorClickCyclesBridgePresetWithoutBoarding(GameTestHelper helper) {
        TestTower test = createTowerAndPlayer(helper, new Vec3(0.0D, THIRD_FLOOR_Y, 0.0D));
        int initialPreset = test.tower.getBridgePreset();
        test.player.setShiftKeyDown(true);

        test.tower.interactAt(test.player, Vec3.ZERO, InteractionHand.MAIN_HAND);

        helper.assertTrue(test.tower.getBridgePreset() != initialPreset,
                "Shift-clicking the tower from the third floor did not change the ramp preset");
        helper.assertTrue(test.player.getVehicle() == null,
                "Shift-using the third-floor bridge control mounted the player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void interiorFloorsCannotBoardThePushingCrew(GameTestHelper helper) {
        TestTower test = createTowerAndPlayer(helper, new Vec3(0.0D, SECOND_FLOOR_Y, 0.0D));
        Player driver = createRideablePlayer(test.tower,
                new Vec3(0.0D, 0.0D, REAR_BOARDING_Z), "tower-driver");
        FakePlayer firstFloorPlayer = createPlayer(helper, test.tower,
                new Vec3(0.0D, FIRST_FLOOR_Y, REAR_BOARDING_Z), "tower-first-floor");

        test.tower.interact(driver, InteractionHand.MAIN_HAND);
        helper.assertTrue(driver.getVehicle() == test.tower, "Ground crew member did not occupy the driver position");

        test.tower.interact(test.player, InteractionHand.MAIN_HAND);
        test.tower.interact(firstFloorPlayer, InteractionHand.MAIN_HAND);

        helper.assertTrue(test.player.getVehicle() == null,
                "A player on the second floor boarded a pushing position");
        helper.assertTrue(firstFloorPlayer.getVehicle() == null,
                "A player inside the first floor boarded a pushing position");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rearGroundAreaBoardsThePushingCrew(GameTestHelper helper) {
        TestTower test = createTowerAndRideablePlayer(helper, new Vec3(0.0D, 0.0D, REAR_BOARDING_Z));
        Player pusher = createRideablePlayer(test.tower,
                new Vec3(20.0D / 16.0D, 0.0D, REAR_BOARDING_Z), "tower-pusher");
        FakePlayer owner = createPlayer(helper, test.tower, Vec3.ZERO, "tower-owner");
        PlayerTeam crew = helper.getLevel().getScoreboard().getPlayerTeam("tower_rear_crew");
        if (crew == null) {
            crew = helper.getLevel().getScoreboard().addPlayerTeam("tower_rear_crew");
        }
        for (Player member : java.util.List.of(owner, test.player, pusher)) {
            helper.getLevel().getScoreboard().addPlayerToTeam(member.getScoreboardName(), crew);
        }
        test.tower.setOwnerUuid(owner.getUUID());

        test.tower.interact(test.player, InteractionHand.MAIN_HAND);
        test.tower.interact(pusher, InteractionHand.MAIN_HAND);

        helper.assertTrue(test.player.getVehicle() == test.tower,
                "The rear ground area did not board the driver");
        helper.assertTrue(pusher.getVehicle() == test.tower && test.tower.isPusher(pusher),
                "The rear ground area did not board the pushing crew");
        helper.succeed();
    }

    private static TestTower createTowerAndPlayer(GameTestHelper helper, Vec3 localPlayerPosition) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the siege tower");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the siege tower");

        FakePlayer player = createPlayer(helper, tower, localPlayerPosition, "tower-interaction-test");
        helper.assertTrue(StructureCollisionResolver.worldBounds(tower).inflate(2.0D)
                        .intersects(player.getBoundingBox()),
                "Test player is outside the tower structure bounds: player="
                        + player.getBoundingBox() + ", tower=" + StructureCollisionResolver.worldBounds(tower));
        return new TestTower(tower, player);
    }

    private static TestTower createTowerAndRideablePlayer(GameTestHelper helper, Vec3 localPlayerPosition) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the siege tower");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the siege tower");
        return new TestTower(tower, createRideablePlayer(tower, localPlayerPosition, "tower-rider"));
    }

    private static FakePlayer createPlayer(GameTestHelper helper, SiegeTowerEntity tower,
                                           Vec3 localPosition, String name) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = FakePlayerFactory.get(level,
                new GameProfile(UUID.randomUUID(), name));
        level.players().add(player);
        Vec3 worldPosition = tower.collisionTransform().toWorld(localPosition);
        player.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        return player;
    }

    private static Player createRideablePlayer(SiegeTowerEntity tower, Vec3 localPosition, String name) {
        Player player = SiegeGameTestPlayers.createRideable((ServerLevel) tower.level());
        Vec3 worldPosition = tower.collisionTransform().toWorld(localPosition);
        player.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        return player;
    }

    private record TestTower(SiegeTowerEntity tower, Player player) {
    }
}
