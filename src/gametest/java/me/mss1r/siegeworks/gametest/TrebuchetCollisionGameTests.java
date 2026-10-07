package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.axiomata.ballistics.ProjectileSweep;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.axiomata.collision.system.StructureInteractionPicker;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.item.SiegeAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
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

@GameTestHolder(TrebuchetCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class TrebuchetCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-6D;

    private TrebuchetCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void trebuchetUsesOnlyPhysicalModelGroups(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        List<String> names = trebuchet.collisionGroups().stream().map(CollisionGroup::name).toList();

        helper.assertTrue(names.equals(List.of("body", "arm", "counterweight")),
                "Trebuchet collision contains visual-only groups: " + names);
        helper.assertTrue(GeneratedCollisionShapes.TREBUCHET_BODY.parts().size() == 36,
                "Trebuchet frame must omit its two zero-thickness decorative planes");
        helper.assertTrue(GeneratedCollisionShapes.TREBUCHET_ARM.parts().size() == 3,
                "Trebuchet arm collision does not match the authored catapult bone");
        helper.assertTrue(GeneratedCollisionShapes.TREBUCHET_COUNTERWEIGHT.parts().size() == 4,
                "Trebuchet counterweight collision does not match the authored bone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetCullingBoundsContainItsModel(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        AABB modelBounds = StructureCollisionResolver.worldBounds(trebuchet);
        AABB cullingBounds = trebuchet.getBoundingBoxForCulling();

        helper.assertTrue(modelBounds != null, "Trebuchet has no model-derived bounds");
        helper.assertTrue(cullingBounds.minX <= modelBounds.minX && cullingBounds.maxX >= modelBounds.maxX
                        && cullingBounds.minY <= modelBounds.minY && cullingBounds.maxY >= modelBounds.maxY
                        && cullingBounds.minZ <= modelBounds.minZ && cullingBounds.maxZ >= modelBounds.maxZ,
                "Trebuchet culling bounds do not contain its complete model");
        helper.assertTrue(modelBounds.getZsize() > trebuchet.getBoundingBox().getZsize() + 10.0D,
                "Trebuchet test model no longer extends meaningfully beyond its technical entity box");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetMovementHullContainsItsRigidFrame(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        AABB hull = trebuchet.getBoundingBox();
        AABB body = GeneratedCollisionShapes.TREBUCHET_BODY.enclosingBounds();

        helper.assertTrue(hull.getXsize() + EPSILON >= 5.0D,
                "Trebuchet movement hull does not contain its central A-frame");
        helper.assertTrue(hull.getYsize() + EPSILON >= body.maxY,
                "Trebuchet movement hull is lower than its rigid frame");
        helper.assertTrue(hull.getZsize() + 10.0D < body.getZsize(),
                "Trebuchet movement hull expanded to the full bed-length square");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileHitsTrebuchetOutsideLegacyEntityBox(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        TrebuchetProjectile projectile = SiegeworksEntities.TREBUCHET_PROJECTILE.get().create(helper.getLevel());
        helper.assertTrue(projectile != null, "Failed to create trebuchet projectile probe");
        helper.assertTrue(helper.getLevel().addFreshEntity(trebuchet), "Failed to add target trebuchet");
        StructureCollisionSystem.register(trebuchet);

        CollisionSample sample = farthestPart(trebuchet);
        Vec3 start = trebuchet.collisionTransform().toWorld(
                sample.group().fromPart(sample.part(), new Vec3(-2.0D, 0.0D, 0.0D)));
        Vec3 end = trebuchet.collisionTransform().toWorld(
                sample.group().fromPart(sample.part(), new Vec3(2.0D, 0.0D, 0.0D)));
        AABB path = new AABB(start, end).inflate(0.01D);
        helper.assertTrue(!path.intersects(trebuchet.getBoundingBox()),
                "Projectile probe still crosses the legacy trebuchet entity box");

        projectile.setPos(start.x, start.y, start.z);
        EntityHitResult hit = ProjectileSweep.findFirstEntityHit(
                helper.getLevel(), projectile, start, end, end.subtract(start),
                entity -> entity == trebuchet, 0.0D);
        helper.assertTrue(hit != null && hit.getEntity() == trebuchet,
                "Projectile sweep missed model geometry outside the legacy entity box");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interactionRayFindsTrebuchetOutsideLegacyEntityBox(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        TrebuchetProjectile viewer = SiegeworksEntities.TREBUCHET_PROJECTILE.get().create(helper.getLevel());
        helper.assertTrue(viewer != null, "Failed to create interaction viewer");
        helper.assertTrue(helper.getLevel().addFreshEntity(trebuchet), "Failed to add target trebuchet");
        StructureCollisionSystem.register(trebuchet);

        CollisionSample sample = farthestPart(trebuchet);
        Vec3 start = trebuchet.collisionTransform().toWorld(
                sample.group().fromPart(sample.part(), new Vec3(-2.0D, 0.0D, 0.0D)));
        Vec3 end = trebuchet.collisionTransform().toWorld(
                sample.group().fromPart(sample.part(), new Vec3(2.0D, 0.0D, 0.0D)));
        helper.assertTrue(!new AABB(start, end).inflate(0.01D).intersects(trebuchet.getBoundingBox()),
                "Interaction ray still crosses the legacy trebuchet entity box");

        EntityHitResult hit = StructureInteractionPicker.findHit(
                viewer, start, end, entity -> entity == trebuchet, start.distanceToSqr(end) + 1.0D);
        helper.assertTrue(hit != null && hit.getEntity() == trebuchet,
                "Interaction ray missed model geometry outside the legacy entity box");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetStatesFollowAuthoredArmTrack(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        Vec3 unloadedCenter = armCenter(trebuchet);

        trebuchet.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        Vec3 loadedCenter = armCenter(trebuchet);
        helper.assertTrue(unloadedCenter.y > loadedCenter.y + 5.0D,
                "Loading the trebuchet did not lower its arm collision");

        trebuchet.setWindingTime(100);
        Vec3 reloadStart = armCenter(trebuchet);
        trebuchet.setWindingTime(50);
        Vec3 reloadHalf = armCenter(trebuchet);
        helper.assertTrue(reloadStart.distanceToSqr(unloadedCenter) < EPSILON,
                "Trebuchet reload does not start at the authored unloaded pose");
        helper.assertTrue(reloadHalf.y < reloadStart.y && reloadHalf.y > loadedCenter.y,
                "Trebuchet reload collision skipped or reversed the authored arm travel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetCounterweightComposesWithArm(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        assertVertical(helper, trebuchet.collisionGroups().get(2), "unloaded");

        trebuchet.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        assertVertical(helper, trebuchet.collisionGroups().get(2), "loaded");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetShotMovesCollisionAndReleasesAtAuthoredTick(GameTestHelper helper) {
        TrebuchetEntity trebuchet = createTrebuchet(helper);
        ServerLevel level = helper.getLevel();
        level.addFreshEntity(trebuchet);
        trebuchet.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        FakePlayer operator = SiegeGameTestPlayers.create(level);

        trebuchet.handleSiegeInteraction(operator, InteractionHand.MAIN_HAND, level);
        helper.assertTrue(trebuchet.getShootAnimationTick() == 0,
                "Trebuchet did not start its synchronized shot track");
        for (int tick = 1; tick < 16; tick++) {
            trebuchet.onSiegeTick(level);
        }
        helper.assertTrue(trebuchet.hasAmmoLoaded(),
                "Trebuchet released its projectile before the authored keyframe");

        trebuchet.onSiegeTick(level);
        helper.assertTrue(trebuchet.getShootAnimationTick() == 16 && !trebuchet.hasAmmoLoaded(),
                "Trebuchet did not release its projectile on animation tick 16");

        for (int tick = 17; tick <= 25; tick++) {
            trebuchet.onSiegeTick(level);
        }
        CollisionGroup arm = trebuchet.collisionGroups().get(1);
        CollisionPart beam = arm.parts().get(2);
        Vec3 actual = arm.fromPart(beam, Vec3.ZERO);
        CollisionPose expectedPose = CollisionPose.aroundX(
                GeneratedCollisionShapes.TREBUCHET_ARM.pivot(), (float) Math.toRadians(145.0D));
        Vec3 expected = expectedPose.toStructure(beam.pose().toStructure(Vec3.ZERO));
        helper.assertTrue(actual.distanceToSqr(expected) < EPSILON,
                "Trebuchet collision missed the authored 25-tick arm keyframe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void trebuchetShotStateSurvivesSaveAndLoad(GameTestHelper helper) {
        TrebuchetEntity source = createTrebuchet(helper);
        ServerLevel level = helper.getLevel();
        source.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        source.handleSiegeInteraction(SiegeGameTestPlayers.create(level), InteractionHand.MAIN_HAND, level);
        for (int tick = 0; tick < 10; tick++) {
            source.onSiegeTick(level);
        }

        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);
        TrebuchetEntity restored = createTrebuchet(helper);
        restored.readAdditionalSaveData(saved);

        helper.assertTrue(restored.getShootAnimationTick() == 10 && restored.hasAmmoLoaded(),
                "Trebuchet lost its in-progress shot while loading NBT");
        for (int tick = 10; tick < 16; tick++) {
            restored.onSiegeTick(level);
        }
        helper.assertTrue(!restored.hasAmmoLoaded(),
                "Restored trebuchet did not release its projectile on the authored tick");
        helper.succeed();
    }

    private static void assertVertical(GameTestHelper helper, CollisionGroup counterweight, String state) {
        Vec3 vertical = counterweight.fromGroupDirection(new Vec3(0.0D, 1.0D, 0.0D));
        helper.assertTrue(vertical.distanceToSqr(new Vec3(0.0D, 1.0D, 0.0D)) < EPSILON,
                "Trebuchet counterweight is not vertical in the " + state + " pose");
    }

    private static Vec3 armCenter(TrebuchetEntity trebuchet) {
        CollisionGroup arm = trebuchet.collisionGroups().get(1);
        return arm.fromPart(arm.parts().get(2), Vec3.ZERO);
    }

    private static CollisionSample farthestPart(TrebuchetEntity trebuchet) {
        CollisionSample farthest = null;
        double farthestDistanceSqr = -1.0D;
        for (CollisionGroup group : trebuchet.collisionGroups()) {
            for (CollisionPart part : group.parts()) {
                Vec3 center = group.fromPart(part, Vec3.ZERO);
                double distanceSqr = center.lengthSqr();
                if (distanceSqr > farthestDistanceSqr) {
                    farthest = new CollisionSample(group, part);
                    farthestDistanceSqr = distanceSqr;
                }
            }
        }
        return farthest;
    }

    private static TrebuchetEntity createTrebuchet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrebuchetEntity trebuchet = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
        helper.assertTrue(trebuchet != null, "Failed to create trebuchet");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        trebuchet.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return trebuchet;
    }

    private record CollisionSample(CollisionGroup group, CollisionPart part) {
    }
}
