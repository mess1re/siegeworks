package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.projectile.TowerCrossbowBoltProjectile;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;

public final class MantletCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;

    private MantletCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void closedMantletUsesAuthoredWindow(GameTestHelper helper) {
        MantletEntity mantlet = createMantlet(helper);

        helper.assertTrue(passesLocalRay(mantlet, new Vec3(0.0D, 1.75D, -1.0D),
                        new Vec3(0.0D, 1.75D, 1.0D)),
                "The authored firing window was filled by collision");
        helper.assertTrue(!passesLocalRay(mantlet, new Vec3(0.65D, 1.75D, -1.0D),
                        new Vec3(0.65D, 1.75D, 1.0D)),
                "A solid part of the mantlet flap allowed a projectile through");
        helper.assertTrue(!passesLocalRay(mantlet, new Vec3(1.2D, 1.75D, -1.0D),
                        new Vec3(1.2D, 1.75D, 1.0D)),
                "A solid part of the mantlet body allowed a projectile through");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void siegeProjectileHitsMantletGeometryBeforeTargetBehindIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MantletEntity mantlet = createMantlet(helper);
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        TowerCrossbowBoltProjectile bolt = SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get().create(level);
        helper.assertTrue(target != null && bolt != null, "Failed to create projectile collision test entities");

        helper.assertTrue(level.addFreshEntity(mantlet), "Failed to add mantlet");
        StructureCollisionSystem.register(mantlet);

        Vec3 start = mantlet.collisionTransform().toWorld(new Vec3(0.65D, 1.75D, -3.0D));
        Vec3 end = mantlet.collisionTransform().toWorld(new Vec3(0.65D, 1.75D, 3.0D));
        Vec3 targetFeet = mantlet.collisionTransform().toWorld(new Vec3(0.65D, 0.75D, 2.0D));
        target.setPos(targetFeet.x, targetFeet.y, targetFeet.z);
        helper.assertTrue(level.addFreshEntity(target), "Failed to add protected target");

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, bolt, start, end,
                new AABB(start, end).inflate(1.0D), entity -> entity != bolt, 0.3F);
        helper.assertTrue(hit != null && hit.getEntity() == mantlet,
                "Siege projectile selected the target behind the mantlet instead of its model geometry");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fastBoltTickStopsOnMantletGeometry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MantletEntity mantlet = createMantlet(helper);
        ArmorStand target = EntityType.ARMOR_STAND.create(level);
        TowerCrossbowBoltProjectile bolt = SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get().create(level);
        helper.assertTrue(target != null && bolt != null, "Failed to create live projectile test entities");

        helper.assertTrue(level.addFreshEntity(mantlet), "Failed to add mantlet");
        StructureCollisionSystem.register(mantlet);
        Vec3 start = mantlet.collisionTransform().toWorld(new Vec3(0.65D, 1.75D, -3.0D));
        Vec3 targetFeet = mantlet.collisionTransform().toWorld(new Vec3(0.65D, 0.75D, 2.0D));
        target.setPos(targetFeet.x, targetFeet.y, targetFeet.z);
        helper.assertTrue(level.addFreshEntity(target), "Failed to add protected target");

        bolt.setPos(start.x, start.y, start.z);
        bolt.setDeltaMovement(mantlet.collisionTransform().directionToWorld(new Vec3(0.0D, 0.0D, 6.0D)));
        helper.assertTrue(level.addFreshEntity(bolt), "Failed to add moving bolt");
        float mantletHealth = mantlet.getHealth();
        float targetHealth = target.getHealth();
        Vec3[] attachedPosition = new Vec3[1];

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(!bolt.isRemoved() && bolt.isEmbedded(),
                    "Fast bolt did not remain embedded in the mantlet");
            helper.assertTrue(mantlet.getHealth() < mantletHealth, "Fast bolt did not damage the mantlet");
            helper.assertTrue(target.getHealth() == targetHealth, "Fast bolt damaged the target behind the mantlet");
            attachedPosition[0] = bolt.position();
            mantlet.setPos(mantlet.getX() + 1.0D, mantlet.getY(), mantlet.getZ());
        });
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!bolt.isRemoved() && attachedPosition[0] != null,
                    "Embedded bolt disappeared when its structure moved");
            helper.assertTrue(Math.abs(bolt.getX() - attachedPosition[0].x - 1.0D) < 0.05D,
                    "Embedded bolt did not follow the moving mantlet");
            mantlet.discard();
        });
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(!bolt.isRemoved() && !bolt.isEmbedded(),
                    "Bolt remained attached after its supporting structure was removed");
            helper.assertTrue(bolt.getDeltaMovement().y < 0.0D,
                    "Detached bolt did not begin falling");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void mantletBodyBlocksPlayerSizedMovement(GameTestHelper helper) {
        MantletEntity mantlet = createMantlet(helper);
        Vec3 center = mantlet.collisionTransform().toWorld(new Vec3(1.2D, 1.75D, -0.8D));
        AABB box = AABB.ofSize(center, 0.6D, 1.8D, 0.6D);
        Vec3 wanted = mantlet.collisionTransform().directionToWorld(new Vec3(0.0D, 0.0D, 1.6D));

        StructureCollisionResolver.ResolvedMovement movement =
                StructureCollisionResolver.resolveMovement(mantlet, box, wanted, 0.0D);
        Vec3 allowedLocal = mantlet.collisionTransform().directionToLocal(movement.allowed());
        helper.assertTrue(allowedLocal.z < 0.7D,
                "Player-sized movement crossed the mantlet body: " + allowedLocal);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flapPoseComposesWithBodyPitch(GameTestHelper helper) {
        MantletEntity mantlet = createMantlet(helper);
        float bodyPitch = 32.0F;
        float flapAngle = 58.0F;
        setAngles(mantlet, bodyPitch, flapAngle);
        CollisionGroup flap = groupNamed(mantlet.collisionGroups(), "flap");
        Vec3 point = new Vec3(0.65D, 2.4D, 0.1D);
        Vec3 expected = rotateAroundX(point, GeneratedCollisionShapes.MANTLET_FLAP.pivot(), -flapAngle);
        expected = rotateAroundX(expected, GeneratedCollisionShapes.MANTLET_BODY.pivot(), -bodyPitch);
        Vec3 actual = flap.fromGroup(point);

        helper.assertTrue(actual.distanceToSqr(expected) < 1.0E-7D,
                "The flap pose did not apply flap rotation before body rotation: expected="
                        + expected + ", actual=" + actual);
        helper.assertTrue(flap.toGroup(actual).distanceToSqr(point) < 1.0E-10D,
                "The nested flap pose did not round-trip through its inverse");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mantletCollisionExcludesWheels(GameTestHelper helper) {
        MantletEntity mantlet = createMantlet(helper);
        List<String> names = mantlet.collisionGroups().stream().map(CollisionGroup::name).toList();
        helper.assertTrue(names.equals(List.of("body", "flap")),
                "Mantlet collision must contain only its body and flap, got " + names);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mantletCollisionExcludesDecorativeNails(GameTestHelper helper) {
        helper.assertTrue(GeneratedCollisionShapes.MANTLET_BODY.parts().size() == 10,
                "Mantlet body collision unexpectedly includes decorative details");
        helper.assertTrue(GeneratedCollisionShapes.MANTLET_FLAP.parts().size() == 4,
                "Mantlet flap collision unexpectedly includes decorative details");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mantletSupportsKeepAuthoredSlope(GameTestHelper helper) {
        long inclinedSupports = GeneratedCollisionShapes.MANTLET_BODY.parts().stream()
                .map(CollisionPart::pose)
                .filter(pose -> Math.abs(pose.rotation().m12()) > 0.25D
                        || Math.abs(pose.rotation().m21()) > 0.25D)
                .count();
        helper.assertTrue(inclinedSupports == 2L,
                "Mantlet collision must preserve its two inclined braces, got " + inclinedSupports);
        helper.succeed();
    }

    private static MantletEntity createMantlet(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MantletEntity mantlet = SiegeworksEntities.MANTLET_ENTITY.get().create(level);
        helper.assertTrue(mantlet != null, "Failed to create a mantlet");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        mantlet.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return mantlet;
    }

    private static void setAngles(MantletEntity mantlet, float bodyPitch, float flapAngle) {
        CompoundTag state = new CompoundTag();
        mantlet.addAdditionalSaveData(state);
        state.putFloat("BodyPitch", bodyPitch);
        state.putFloat("FlapAngle", flapAngle);
        mantlet.readAdditionalSaveData(state);
    }

    private static boolean passesLocalRay(MantletEntity mantlet, Vec3 localStart, Vec3 localEnd) {
        return mantlet.allowsProjectilePassage(
                mantlet.collisionTransform().toWorld(localStart),
                mantlet.collisionTransform().toWorld(localEnd));
    }

    private static Vec3 rotateAroundX(Vec3 point, Vec3 pivot, float degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double y = point.y - pivot.y;
        double z = point.z - pivot.z;
        return new Vec3(point.x,
                pivot.y + y * cos - z * sin,
                pivot.z + y * sin + z * cos);
    }

    private static CollisionGroup groupNamed(List<CollisionGroup> groups, String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No collision group named " + name));
    }
}
