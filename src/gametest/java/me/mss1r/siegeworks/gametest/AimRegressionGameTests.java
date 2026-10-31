package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_aim_regression")
@PrefixGameTestTemplate(false)
public final class AimRegressionGameTests {
    @GameTest(template = "empty")
    public static void remoteAndMechanicalElevationRetainsTickInterpolation(GameTestHelper helper) {
        for (AbstractSiegeEntity siege : new AbstractSiegeEntity[] {
                SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel()),
                SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(helper.getLevel())}) {
            siege.setPos(helper.absoluteVec(new Vec3(10, 10, 10)));
            siege.setTrackedPitch(0);
            siege.updateRenderedAim(0);
            siege.setTrackedPitch(4);
            siege.updateRenderedAim(.25F);
            helper.assertTrue(Math.abs(siege.getRenderedAimPitch() - 1) < .001,
                    "A remote or mechanical elevation must interpolate from its previous tick");
            siege.updateRenderedAim(.75F);
            helper.assertTrue(Math.abs(siege.getRenderedAimPitch() - 3) < .001,
                    "A remote or mechanical elevation must advance smoothly within the tick");
            siege.updateRenderedAim(.75F);
            helper.assertTrue(Math.abs(siege.getRenderedAimPitch() - 3) < .001,
                    "Repeated camera/model reads must not advance the angle twice");
            siege.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void renderedElevationDoesNotCacheAnOldTargetAtTheSamePartialTick(GameTestHelper helper) {
        var siege = SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel());
        siege.setTrackedPitch(0);
        siege.updateRenderedAim(0);
        siege.setTrackedPitch(2);
        siege.updateRenderedAim(.5F);
        helper.assertTrue(Math.abs(siege.getRenderedAimPitch() - 1) < .001, "The first target must interpolate");
        siege.setTrackedPitch(4);
        siege.updateRenderedAim(.5F);
        helper.assertTrue(Math.abs(siege.getRenderedAimPitch() - 2) < .001,
                "A changed target at the same partial tick must not reuse stale camera/model state");
        siege.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void viewLimitPreservesPreviousFrameWhileVehicleTurns(GameTestHelper helper) {
        var siege = SiegeworksEntities.MANTLET_ENTITY.get().create(helper.getLevel());
        var rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        siege.applyYaw(100);
        siege.yBodyRotO = 98;
        float limit = siege.getPassengerViewYawLimit(rider);
        rider.yRotO = 98 + limit;
        rider.setYRot(110 + limit);
        SiegePassengerPhysics.clampView(siege, rider, rider);
        helper.assertTrue(Math.abs(rider.getYRot() - (100 + limit)) < .001,
                "Current view exceeds its limit");
        helper.assertTrue(Math.abs(rider.yRotO - (98 + limit)) < .001,
                "Clamping overwrote the previous view and snapped the camera to this tick");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fastMouseOvershootDoesNotSwapViewEdges(GameTestHelper helper) {
        var siege = SiegeworksEntities.MANTLET_ENTITY.get().create(helper.getLevel());
        var rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        siege.applyYaw(0);
        float limit = siege.getPassengerViewYawLimit(rider);
        for (float side : new float[]{-1, 1}) {
            rider.yRotO = side * 220;
            rider.setYRot(side * 220);
            SiegePassengerPhysics.clampView(siege, rider, rider, side * limit);
            helper.assertTrue(Math.abs(rider.getYRot() - side * limit) < .001,
                    "Mouse overshoot flipped to the opposite edge");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void viewLimitCrossesYawWrapWithoutAFullTurn(GameTestHelper helper) {
        var siege = SiegeworksEntities.MANTLET_ENTITY.get().create(helper.getLevel());
        var rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        siege.applyYaw(-179);
        siege.yBodyRotO = 179;
        float limit = siege.getPassengerViewYawLimit(rider);
        rider.yRotO = 179 + limit;
        rider.setYRot(190 + limit);
        SiegePassengerPhysics.clampView(siege, rider, rider);
        helper.assertTrue(Math.abs(rider.getYRot() - rider.yRotO - 2) < .001,
                "Crossing +/-180 introduced a camera turn or snap");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void riderBodyRetainsItsPreviousRotation(GameTestHelper helper) {
        var siege = SiegeworksEntities.MANTLET_ENTITY.get().create(helper.getLevel());
        var rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        rider.startRiding(siege, true);
        siege.applyYaw(100);
        rider.yBodyRotO = 98;
        SiegePassengerPhysics.updatePassengerState(siege, rider);
        helper.assertTrue(Math.abs(rider.yBodyRot - 100) < .001, "Body does not follow the machine");
        helper.assertTrue(Math.abs(rider.yBodyRotO - 98) < .001,
                "Body interpolation was discarded while turning");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void mountedCrossbowRespectsGroundAtUpwardLimitOnCalcite(GameTestHelper helper) {
        for (int x = 2; x <= 18; x++) {
            for (int z = 2; z <= 18; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.CALCITE);
            }
        }
        var siege = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(siege != null, "Crossbow could not be created");
        Vec3 origin = helper.absoluteVec(new Vec3(10, 2, 10));
        siege.setPos(origin);
        siege.setTrackedPitch(siege.getMinAimPitch());
        float allowedPitch = siege.getTrackedPitch();
        siege.setTrackedPitch(0);
        helper.getLevel().addFreshEntity(siege);
        var rider = SiegeGameTestPlayers.createRideable(helper.getLevel());
        rider.setPos(origin.add(0, 0, -3));
        helper.assertTrue(rider.startRiding(siege, true), "Operator could not mount");
        for (int tick = 1; tick <= 120; tick++) {
            helper.runAfterDelay(tick, () -> {
                siege.setPassengerAimTargetPitch(siege.getMinAimPitch());
                rider.setXRot(siege.getMinAimPitch());
                rider.rideTick();
                helper.assertTrue(siege.position().distanceToSqr(origin) < 0.01,
                        "Aiming moves the stationary engine: " + siege.position());
                helper.assertTrue(Float.isFinite(siege.getTrackedPitch()), "Aim is not finite");
            });
        }
        helper.runAfterDelay(121, () -> {
            helper.assertTrue(Math.abs(siege.getTrackedPitch() - allowedPitch) < 0.02,
                    "Controller bypasses the terrain limit: " + siege.getTrackedPitch() + " instead of " + allowedPitch);
            helper.succeed();
        });
    }

}
