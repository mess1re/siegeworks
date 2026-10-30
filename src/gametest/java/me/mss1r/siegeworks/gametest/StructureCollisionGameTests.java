package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(StructureCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class StructureCollisionGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_collision";

    private StructureCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void cachedBoundsInvalidateWhenStructureTicks(GameTestHelper helper) {
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(tower != null, "Failed to create the cached-bounds test tower");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        StructureCollisionSystem.register(tower);
        AABB first = StructureCollisionSystem.worldBounds(tower);

        tower.setPos(tower.getX() + 9.0D, tower.getY(), tower.getZ());
        StructureCollisionSystem.register(tower);
        AABB moved = StructureCollisionSystem.worldBounds(tower);
        StructureCollisionSystem.unregister(tower);

        helper.assertTrue(first != null && moved != null
                        && Math.abs((moved.getCenter().x - first.getCenter().x) - 9.0D) < 1.0E-8D,
                "The broad-phase bounds cache survived a structure transform change");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void movingBridgeDoesNotPushFromStaticTowerWalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        ArmorStand wallProbe = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && wallProbe != null,
                "Failed to create the tower wall collision test entities");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        wallProbe.setNoGravity(true);
        Vec3 wallPosition = tower.collisionTransform().toWorld(new Vec3(4.2D, 5.0D, 0.0D));
        wallProbe.setPos(wallPosition.x, wallPosition.y, wallPosition.z);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the tower wall collision test tower");
        helper.assertTrue(level.addFreshEntity(wallProbe), "Failed to add the tower wall collision probe");

        helper.runAfterDelay(2, () -> {
            Vec3 before = wallProbe.position();
            CompoundTag bridgeState = new CompoundTag();
            tower.addAdditionalSaveData(bridgeState);
            bridgeState.putBoolean("BridgeOpen", true);
            bridgeState.putFloat("BridgeProgress", 0.25F);
            tower.readAdditionalSaveData(bridgeState);

            StructureMotionSystem.tickStructure(tower);
            helper.assertTrue(wallProbe.position().distanceToSqr(before) < 1.0E-8D,
                    "A moving bridge activated push-out from a static tower wall: "
                            + wallProbe.position().subtract(before));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerWallsBlockMovementFromBothSides(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        ArmorStand insideProbe = EntityType.ARMOR_STAND.create(level);
        ArmorStand outsideProbe = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && insideProbe != null && outsideProbe != null,
                "Failed to create the tower wall sweep test entities");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        placeProbe(tower, insideProbe, new Vec3(3.0D, 5.0D, 0.0D));
        placeProbe(tower, outsideProbe, new Vec3(5.0D, 5.0D, 0.0D));
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the tower wall sweep test tower");
        helper.assertTrue(level.addFreshEntity(insideProbe), "Failed to add the inside wall probe");
        helper.assertTrue(level.addFreshEntity(outsideProbe), "Failed to add the outside wall probe");

        helper.runAfterDelay(2, () -> {
            setTowerYaw(tower, 23.0F);
            placeProbe(tower, insideProbe, new Vec3(3.0D, 5.0D, 0.0D));
            placeProbe(tower, outsideProbe, new Vec3(5.0D, 5.0D, 0.0D));
            Vec3 outward = tower.collisionTransform().directionToWorld(new Vec3(0.2D, 0.0D, 0.0D));
            Vec3 inward = tower.collisionTransform().directionToWorld(new Vec3(-0.2D, 0.0D, 0.0D));
            for (int step = 0; step < 80; step++) {
                double beforeX = tower.collisionTransform().toLocal(insideProbe.position()).x;
                StructureCollisionSystem.Movement predicted =
                        StructureCollisionSystem.collideMovement(insideProbe, outward, outward);
                insideProbe.move(MoverType.SELF, outward);
                double afterX = tower.collisionTransform().toLocal(insideProbe.position()).x;
                helper.assertTrue(afterX < 3.5D,
                        "The inside probe crossed the tower wall on step " + step
                                + ": before=" + beforeX + ", after=" + afterX
                                + ", width=" + insideProbe.getBoundingBox().getXsize()
                                + ", yaw=" + tower.collisionTransform().yawDegrees()
                                + ", requestedLocal="
                                + tower.collisionTransform().directionToLocal(outward)
                                + ", predictedLocal="
                                + tower.collisionTransform().directionToLocal(predicted.allowed())
                                + ", nearbyStructures=" + StructureCollisionSystem.structuresNear(
                                insideProbe, insideProbe.getBoundingBox().expandTowards(outward).inflate(0.25D)).size()
                                + ", towerBounds=" + StructureCollisionSystem.worldBounds(tower)
                                + ", probeBounds=" + insideProbe.getBoundingBox());
                outsideProbe.move(MoverType.SELF, inward);
            }

            double insideX = tower.collisionTransform().toLocal(insideProbe.position()).x;
            double outsideX = tower.collisionTransform().toLocal(outsideProbe.position()).x;
            helper.assertTrue(insideX < 3.5D,
                    "The inside probe walked through the tower wall: x=" + insideX);
            helper.assertTrue(outsideX > 4.0D,
                    "The outside probe walked through the tower wall: x=" + outsideX);
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rotatedTowerWallsBlockPlayerSizedMovement(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the rotated tower wall test entity");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        for (float yaw : new float[]{0.0F, 17.5F, 45.0F, 89.0F}) {
            setTowerYaw(tower, yaw);
            assertWallBlocksRepeatedMovement(helper, tower, new Vec3(3.0D, 5.0D, 0.0D),
                    new Vec3(0.2D, 0.0D, 0.0D), true, yaw);
            assertWallBlocksRepeatedMovement(helper, tower, new Vec3(5.0D, 5.0D, 0.0D),
                    new Vec3(-0.2D, 0.0D, 0.0D), false, yaw);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerFloorAndWallResolveAsOneContactManifold(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the tower floor and wall test entity");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        setTowerYaw(tower, 37.0F);

        Vec3 localFeet = new Vec3(3.0D, 1.1875D, 0.0D);
        Vec3 worldFeet = tower.collisionTransform().toWorld(localFeet);
        AABB box = AABB.ofSize(worldFeet.add(0.0D, 0.9D, 0.0D), 0.6D, 1.8D, 0.6D);
        Vec3 movement = tower.collisionTransform().directionToWorld(new Vec3(0.2D, -0.08D, 0.06D));
        for (int step = 0; step < 80; step++) {
            StructureCollisionResolver.ResolvedMovement resolved =
                    StructureCollisionResolver.resolveMovement(tower, box, movement, 0.0D);
            box = box.move(resolved.allowed());
            Vec3 localCenter = tower.collisionTransform().toLocal(box.getCenter());
            helper.assertTrue(localCenter.x < 3.75D,
                    "Player-sized box crossed the wall while standing on the floor on step " + step
                            + ": localCenter=" + localCenter + ", allowedLocal="
                            + tower.collisionTransform().directionToLocal(resolved.allowed()));
            helper.assertTrue(box.minY >= worldFeet.y - 1.0E-3D,
                    "Player-sized box fell through the tower floor on step " + step
                            + ": minY=" + box.minY + ", floorY=" + worldFeet.y);
        }
        helper.succeed();
    }

    private static void assertWallBlocksRepeatedMovement(GameTestHelper helper, SiegeTowerEntity tower,
                                                         Vec3 localStart, Vec3 localMovement,
                                                         boolean inside, float yaw) {
        AABB box = AABB.ofSize(tower.collisionTransform().toWorld(localStart), 0.6D, 1.8D, 0.6D);
        Vec3 movement = tower.collisionTransform().directionToWorld(localMovement);
        for (int step = 0; step < 80; step++) {
            StructureCollisionResolver.ResolvedMovement resolved =
                    StructureCollisionResolver.resolveMovement(tower, box, movement, 0.0D);
            box = box.move(resolved.allowed());
            double localX = tower.collisionTransform().toLocal(box.getCenter()).x;
            boolean blocked = inside ? localX < 3.75D : localX > 4.0D;
            helper.assertTrue(blocked,
                    "Player-sized box crossed the tower wall at yaw " + yaw + " on step " + step
                            + ": localX=" + localX + ", allowedLocal="
                            + tower.collisionTransform().directionToLocal(resolved.allowed()));
        }
    }

    private static void placeProbe(SiegeTowerEntity tower, ArmorStand probe, Vec3 localPosition) {
        Vec3 worldPosition = tower.collisionTransform().toWorld(localPosition);
        probe.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
        probe.setNoGravity(true);
    }

    private static void setTowerYaw(SiegeTowerEntity tower, float yaw) {
        tower.setYRot(yaw);
        tower.yRotO = yaw;
        tower.setYBodyRot(yaw);
        tower.yBodyRotO = yaw;
    }
}
