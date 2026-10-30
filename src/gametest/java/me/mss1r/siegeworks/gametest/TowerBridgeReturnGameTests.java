package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.api.SiegeTransportControl.ExitResult;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(TowerBridgeReturnGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class TowerBridgeReturnGameTests {
    public static final String NAMESPACE = "siegeworks_tower";

    private TowerBridgeReturnGameTests() {
    }


    @GameTest(template = "empty")
    public static void returnWalksAlongThePosedBridge(GameTestHelper helper) {
        for (float yaw : new float[]{0.0F, 37.0F, -90.0F}) {
            for (int preset = 0; preset <= 2; preset++) {
                SiegeTowerEntity tower = tower(helper, yaw, preset);
                LivingEntity recruit = EntityType.ZOMBIE.create(helper.getLevel());
                Vec3 surface = bridgeStandingPoint(tower, recruit, 8.0D);
                recruit.setPos(surface.x, surface.y, surface.z);
                helper.assertTrue(!tower.beginAutomatedReturn(recruit), "Return started without a reserved seat");
                helper.assertTrue(tower.reserveInteriorSeat(recruit), "Could not reserve a return seat");
                helper.assertTrue(tower.beginAutomatedReturn(recruit), "Return did not recognize the bridge surface");
                walkToEntrance(helper, tower, recruit);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void returnApproachIsOutsideTheBridge(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(3, 1, 11, 29, 11, 22)) {
            helper.setBlock(pos, Blocks.STONE);
        }
        SiegeTowerEntity tower = tower(helper, 0.0F, 0);
        LivingEntity recruit = EntityType.ZOMBIE.create(helper.getLevel());
        Vec3 roof = tower.collisionTransform().toWorld(new Vec3(6.0D, 11.0D, 8.0D));
        recruit.setPos(roof.x, roof.y, roof.z);
        helper.assertTrue(tower.reserveInteriorSeat(recruit), "Could not reserve a return seat");
        helper.assertTrue(!tower.beginAutomatedReturn(recruit), "Return pulled a distant recruit onto the bridge");
        Vec3 approach = tower.getAutomatedReturnApproach(recruit);
        helper.assertTrue(approach != null, "No accessible edge approach on a wide wall");
        recruit.setPos(approach.x, approach.y, approach.z);
        helper.assertTrue(!StructureCollisionResolver.intersects(tower, tower.solidCollisionGroups(),
                recruit.getBoundingBox(), 0.0D), "Approach lies inside tower geometry");
        helper.assertTrue(helper.getLevel().noCollision(recruit, recruit.getBoundingBox()),
                "Approach lies inside wall blocks");
        helper.assertTrue(tower.beginAutomatedReturn(recruit), "Recruit at the edge could not start returning");
        walkToEntrance(helper, tower, recruit);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closingTheBridgeCancelsTheReturn(GameTestHelper helper) {
        SiegeTowerEntity tower = tower(helper, 0.0F, 1);
        LivingEntity recruit = EntityType.ZOMBIE.create(helper.getLevel());
        Vec3 surface = bridgeStandingPoint(tower, recruit, 8.0D);
        recruit.setPos(surface.x, surface.y, surface.z);
        tower.reserveInteriorSeat(recruit);
        helper.assertTrue(tower.beginAutomatedReturn(recruit), "Could not start return");
        CompoundTag tag = new CompoundTag();
        tower.addAdditionalSaveData(tag);
        tag.putBoolean("BridgeOpen", false);
        tower.readAdditionalSaveData(tag);
        helper.assertTrue(tower.advanceAutomatedReturn(recruit) == ExitResult.FAILED,
                "A closing bridge kept moving the recruit");
        helper.assertTrue(recruit.position().distanceToSqr(surface) < 1.0E-8D,
                "Cancelling the return moved the recruit");
        helper.assertTrue(!tower.beginAutomatedReturn(recruit), "Return started on a closed bridge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void touchingTheSideOrUndersideDoesNotBoard(GameTestHelper helper) {
        SiegeTowerEntity tower = tower(helper, 0.0F, 0);
        LivingEntity recruit = EntityType.ZOMBIE.create(helper.getLevel());
        tower.reserveInteriorSeat(recruit);
        Vec3 surface = bridgeStandingPoint(tower, recruit, 8.0D);
        recruit.setPos(surface.x, surface.y - 2.0D, surface.z);
        helper.assertTrue(!tower.beginAutomatedReturn(recruit), "Return began through the underside");
        recruit.setPos(tower.getX() + 4.35D, surface.y - 0.5D, surface.z);
        helper.assertTrue(!tower.beginAutomatedReturn(recruit), "Touching the bridge side started boarding");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void movingTheTowerCancelsTheReturn(GameTestHelper helper) {
        SiegeTowerEntity tower = tower(helper, 0.0F, 1);
        LivingEntity recruit = EntityType.ZOMBIE.create(helper.getLevel());
        Vec3 surface = bridgeStandingPoint(tower, recruit, 8.0D);
        recruit.setPos(surface.x, surface.y, surface.z);
        tower.reserveInteriorSeat(recruit);
        helper.assertTrue(tower.beginAutomatedReturn(recruit), "Could not start return");
        tower.setPos(tower.getX() + 1.0D, tower.getY(), tower.getZ());
        helper.assertTrue(tower.advanceAutomatedReturn(recruit) == ExitResult.FAILED,
                "Return continued after the tower moved");
        helper.assertTrue(recruit.position().distanceToSqr(surface) < 1.0E-8D,
                "Moving the tower pulled the recruit along the old route");
        helper.succeed();
    }

    private static void walkToEntrance(GameTestHelper helper, SiegeTowerEntity tower, LivingEntity recruit) {
        ExitResult result = ExitResult.IN_PROGRESS;
        int steps = 0;
        while (result == ExitResult.IN_PROGRESS && steps++ < 150) {
            Vec3 before = recruit.position();
            result = tower.advanceAutomatedReturn(recruit);
            helper.assertTrue(before.distanceToSqr(recruit.position()) < 0.5D * 0.5D,
                    "Bridge return teleported the recruit: " + before + " -> " + recruit.position());
            helper.assertTrue(!StructureCollisionResolver.intersects(tower, tower.solidCollisionGroups(),
                    recruit.getBoundingBox(), 0.0D), "Bridge return passed through tower geometry");
            helper.assertTrue(helper.getLevel().noCollision(recruit, recruit.getBoundingBox()),
                    "Bridge return passed through wall blocks");
        }
        helper.assertTrue(result == ExitResult.COMPLETE && steps > 10,
                "Return did not walk to the entrance: " + result + " at " + recruit.position() + " steps=" + steps);
        helper.assertTrue(!recruit.isPassenger(), "Bridge traversal mounted before reaching the entrance");
        Vec3 local = tower.collisionTransform().toLocal(recruit.position());
        helper.assertTrue(local.z < 4.5D, "Return ended at the far end of the bridge");
    }

    private static SiegeTowerEntity tower(GameTestHelper helper, float yaw, int preset) {
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        BlockPos origin = helper.absolutePos(new BlockPos(16, 1, 6));
        tower.setPos(origin.getX(), origin.getY(), origin.getZ());
        tower.setYRot(yaw);
        tower.yBodyRot = yaw;
        tower.yBodyRotO = yaw;
        CompoundTag tag = new CompoundTag();
        tower.addAdditionalSaveData(tag);
        tag.putBoolean("BridgeOpen", true);
        tag.putInt("BridgePreset", preset);
        tag.putFloat("BridgeProgress", 0.5F + preset * 0.125F);
        tower.readAdditionalSaveData(tag);
        return tower;
    }

    private static Vec3 bridgeStandingPoint(SiegeTowerEntity tower, LivingEntity recruit, double forward) {
        Vec3 start = tower.collisionTransform().toWorld(new Vec3(0.0D, 25.0D, forward));
        Vec3 end = tower.collisionTransform().toWorld(new Vec3(0.0D, 1.0D, forward));
        Vec3 surface = StructureCollisionResolver.clipSegment(tower, start, end).orElseThrow();
        double yaw = Math.toRadians(tower.getVisualRotationYInDegrees());
        double footprint = recruit.getBbWidth() * 0.5D * (Math.abs(Math.sin(yaw)) + Math.abs(Math.cos(yaw)));
        return surface.add(0.0D, Math.abs(1.0D / Math.tan(tower.getBridgeAngleRadians())) * footprint + 0.025D, 0.0D);
    }
}
