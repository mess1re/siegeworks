package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

public final class BoltThrowerCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;

    private BoltThrowerCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void arcballistaParkingAndTowingRespectItsMechanicalPitchRange(GameTestHelper helper) {
        ArcballistaEntity arcballista = createArcballista(helper);
        arcballista.setTrackedPitch(89);
        helper.assertTrue(Math.abs(arcballista.getTrackedPitch() - 25) < .001,
                "An unattended arcballista exceeded its depression limit");
        for (int i = 0; i < 200; i++) {
            arcballista.onSiegeTick(helper.getLevel());
            helper.assertTrue(arcballista.getTrackedPitch() >= -35 && arcballista.getTrackedPitch() <= 25,
                    "Parking exceeded the arcballista's mechanical range");
        }
        helper.assertTrue(Math.abs(arcballista.getTrackedPitch() + 35) < .001,
                "The parked arcballista did not stop at its elevation limit");
        var horse = EntityType.HORSE.create(helper.getLevel());
        helper.assertTrue(horse.startRiding(arcballista, true), "The arcballista rejected its draft mount");
        for (int i = 0; i < 200; i++) arcballista.onSiegeTick(helper.getLevel());
        helper.assertTrue(Math.abs(arcballista.getTrackedPitch()) < .001,
                "The towed arcballista did not return to its level transport pose");
        horse.stopRiding();
        for (int i = 0; i < 200; i++) arcballista.onSiegeTick(helper.getLevel());
        helper.assertTrue(Math.abs(arcballista.getTrackedPitch() + 35) < .001,
                "Unhitching restored the obsolete vertical parking pose");
        horse.discard();
        arcballista.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadingAnArcballistaClampsOldVerticalParkingAngles(GameTestHelper helper) {
        for (float oldPitch : new float[]{-89, 89}) {
            ArcballistaEntity original = createArcballista(helper);
            CompoundTag tag = new CompoundTag();
            original.save(tag);
            tag.putFloat("TrackedPitch", oldPitch);
            ArcballistaEntity loaded = createArcballista(helper);
            loaded.load(tag);
            float expected = oldPitch < 0 ? -35 : 25;
            helper.assertTrue(Math.abs(loaded.getTrackedPitch() - expected) < .001,
                    "Loading kept an obsolete arcballista pitch: " + loaded.getTrackedPitch());
            helper.assertTrue(Math.abs(loaded.getXRot() - expected) < .001,
                    "Loading left the visual angle outside the mechanical range");
            original.discard();
            loaded.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boltThrowersUseOnlyStructuralModelParts(GameTestHelper helper) {
        ArcballistaEntity arcballista = createArcballista(helper);
        TowerCrossbowEntity towerCrossbow = createTowerCrossbow(helper);

        helper.assertTrue(groupNames(arcballista.collisionGroups()).equals(List.of("engine")),
                "Arcballista collision contains visual-only groups");
        helper.assertTrue(groupNames(towerCrossbow.collisionGroups()).equals(List.of("rack", "engine")),
                "Tower crossbow collision does not separate its rack and engine");
        helper.assertTrue(GeneratedCollisionShapes.ARCBALLISTA_ENGINE.parts().size() == 21,
                "Arcballista collision does not match its structural model bones");
        helper.assertTrue(GeneratedCollisionShapes.TOWER_CROSSBOW_RACK.parts().size() == 10
                        && GeneratedCollisionShapes.TOWER_CROSSBOW_ENGINE.parts().size() == 35,
                "Tower crossbow collision does not match its structural model bones");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void arcballistaPitchRotatesItsCompleteEngine(GameTestHelper helper) {
        ArcballistaEntity arcballista = createArcballista(helper);
        CollisionGroup level = arcballista.collisionGroups().get(0);
        Vec3 pivot = GeneratedCollisionShapes.ARCBALLISTA_ENGINE.pivot();
        Vec3 front = new Vec3(0.0D, 0.9D, 1.35D);
        Vec3 levelFront = level.pose().toStructure(front);

        arcballista.setTrackedPitch(25.0F);
        CollisionGroup lowered = arcballista.collisionGroups().get(0);
        Vec3 loweredFront = lowered.pose().toStructure(front);

        helper.assertTrue(loweredFront.y < levelFront.y - 0.25D,
                "Arcballista collision tilted opposite to its downward aim");
        helper.assertTrue(level.pose().toStructure(pivot).distanceToSqr(
                        lowered.pose().toStructure(pivot)) < EPSILON,
                "Arcballista aim moved the authored axle pivot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerCrossbowAimComposesPitchAndTurretYaw(GameTestHelper helper) {
        TowerCrossbowEntity towerCrossbow = createTowerCrossbow(helper);
        towerCrossbow.setYRot(0.0F);
        towerCrossbow.setYBodyRot(0.0F);
        towerCrossbow.setTrackedYaw(0.0F);
        towerCrossbow.setTrackedPitch(0.0F);

        CollisionGroup levelRack = towerCrossbow.collisionGroups().get(0);
        CollisionGroup levelEngine = towerCrossbow.collisionGroups().get(1);
        Vec3 pivot = GeneratedCollisionShapes.TOWER_CROSSBOW_ENGINE.pivot();
        Vec3 front = new Vec3(0.0D, 1.3D, 1.8D);
        Vec3 levelFront = levelEngine.pose().toStructure(front);

        towerCrossbow.setTrackedPitch(12.0F);
        towerCrossbow.setTrackedYaw(90.0F);
        CollisionGroup aimedRack = towerCrossbow.collisionGroups().get(0);
        CollisionGroup aimedEngine = towerCrossbow.collisionGroups().get(1);
        Vec3 aimedFront = aimedEngine.pose().toStructure(front);

        helper.assertTrue(aimedFront.x < -1.5D && Math.abs(aimedFront.z) < 0.5D,
                "Tower crossbow engine did not follow its independent turret yaw: " + aimedFront);
        helper.assertTrue(aimedFront.y < levelFront.y - 0.2D,
                "Tower crossbow engine tilted opposite to its downward aim");
        helper.assertTrue(levelEngine.pose().toStructure(pivot).distanceToSqr(
                        aimedEngine.pose().toStructure(pivot)) < EPSILON,
                "Tower crossbow aim moved the authored engine pivot");
        helper.assertTrue(firstPartCenter(levelRack).distanceToSqr(firstPartCenter(aimedRack)) < EPSILON,
                "Tower crossbow aim moved its stationary rack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void arcballistaBoundsIncludeBowOutsideTechnicalBox(GameTestHelper helper) {
        ArcballistaEntity arcballista = createArcballista(helper);
        AABB modelBounds = StructureCollisionResolver.worldBounds(arcballista);

        helper.assertTrue(modelBounds != null, "Arcballista has no model-derived bounds");
        helper.assertTrue(modelBounds.getXsize() > arcballista.getBoundingBox().getXsize() + 0.5D,
                "Arcballista model bounds still use its old technical width");
        AABB cullingBounds = arcballista.getBoundingBoxForCulling();
        helper.assertTrue(cullingBounds.minX <= modelBounds.minX && cullingBounds.maxX >= modelBounds.maxX
                        && cullingBounds.minY <= modelBounds.minY && cullingBounds.maxY >= modelBounds.maxY
                        && cullingBounds.minZ <= modelBounds.minZ && cullingBounds.maxZ >= modelBounds.maxZ,
                "Arcballista culling bounds do not contain its complete collision model");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nearbySiegeTowerPreservesGroundLimitedAimWhenPlacedFirst(GameTestHelper helper) {
        assertNearbyTowerAim(helper, true);
    }

    @GameTest(template = "empty")
    public static void nearbySiegeTowerPreservesGroundLimitedAimWhenPlacedLast(GameTestHelper helper) {
        assertNearbyTowerAim(helper, false);
    }

    private static void assertNearbyTowerAim(GameTestHelper helper, boolean towerFirst) {
        var level = helper.getLevel();
        for (int x = 0; x < 40; x++) for (int z = 0; z < 40; z++)
            helper.setBlock(new BlockPos(x, 0, z), Blocks.CALCITE);
        Vec3 position = helper.absoluteVec(new Vec3(15, 1, 15));
        var reference = createTowerCrossbow(helper);
        reference.setPos(position);
        level.addFreshEntity(reference);
        reference.setTrackedPitch(reference.getMinAimPitch());
        float groundLimit = reference.getTrackedPitch();
        helper.assertTrue(groundLimit > reference.getMinAimPitch() + .01,
                "The fixture must exercise collision-limited elevation");
        reference.discard();

        var tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        tower.setPos(position.add(5, 0, 5));
        var crossbow = createTowerCrossbow(helper);
        crossbow.setPos(position);
        if (towerFirst) { level.addFreshEntity(tower); level.addFreshEntity(crossbow); }
        else { level.addFreshEntity(crossbow); level.addFreshEntity(tower); }
        try {
            crossbow.setTrackedPitch(crossbow.getMinAimPitch());
            helper.assertTrue(Math.abs(crossbow.getTrackedPitch() - groundLimit) < .0001,
                    "The nearby tower changed the clear, ground-limited aim: " + groundLimit + " -> " + crossbow.getTrackedPitch());
            helper.assertTrue(!StructureCollisionResolver.intersects(crossbow, crossbow.collisionGroups(),
                    tower, me.mss1r.axiomata.collision.StructureGeometry.solid(tower), 0),
                    "The accepted mechanism pose intersects the tower");
        } finally { crossbow.discard(); tower.discard(); }
        helper.succeed();
    }

    private static List<String> groupNames(List<CollisionGroup> groups) {
        return groups.stream().map(CollisionGroup::name).toList();
    }

    private static Vec3 firstPartCenter(CollisionGroup group) {
        CollisionPart part = group.parts().get(0);
        return group.fromPart(part, Vec3.ZERO);
    }

    private static ArcballistaEntity createArcballista(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity arcballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(arcballista != null, "Failed to create arcballista");
        placeAtTestCenter(helper, arcballista);
        return arcballista;
    }

    private static TowerCrossbowEntity createTowerCrossbow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        helper.assertTrue(towerCrossbow != null, "Failed to create tower crossbow");
        placeAtTestCenter(helper, towerCrossbow);
        return towerCrossbow;
    }

    private static void placeAtTestCenter(GameTestHelper helper,
                                          AbstractSiegeEntity siege) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        siege.setPos(origin.getX() + 8.0D, origin.getY() + 5.0D, origin.getZ() + 8.0D);
    }
}
