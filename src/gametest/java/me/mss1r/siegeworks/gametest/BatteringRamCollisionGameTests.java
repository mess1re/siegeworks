package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(BatteringRamCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BatteringRamCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;
    private static final double GENERATED_ROTATION_EPSILON = 1.0E-8D;

    private BatteringRamCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void ramCollisionUsesAuthoredCubesWithoutWheelsOrRopes(GameTestHelper helper) {
        BatteringRamEntity ram = createRam(helper);
        List<String> groupNames = ram.collisionGroups().stream().map(CollisionGroup::name).toList();

        helper.assertTrue(groupNames.equals(List.of("body", "ram")),
                "Battering ram collision contains visual-only groups: " + groupNames);
        helper.assertTrue(GeneratedCollisionShapes.BATTERING_RAM_BODY.parts().size() == 14,
                "Battering ram body was split or lost authored cubes");
        helper.assertTrue(GeneratedCollisionShapes.BATTERING_RAM_BEAM.parts().size() == 8,
                "Battering ram beam was split or lost authored cubes");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ramMovementHullContainsItsCentralFrame(GameTestHelper helper) {
        BatteringRamEntity ram = createRam(helper);
        AABB hull = ram.getBoundingBox();
        AABB body = GeneratedCollisionShapes.BATTERING_RAM_BODY.enclosingBounds();

        helper.assertTrue(hull.getXsize() + EPSILON >= body.getXsize(),
                "Battering ram movement hull is narrower than its central frame");
        helper.assertTrue(hull.getYsize() + EPSILON >= body.maxY,
                "Battering ram movement hull is lower than its central frame");
        helper.assertTrue(hull.getZsize() + EPSILON < body.getZsize(),
                "Battering ram movement hull expanded to the full beam-length square");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void authoredCubeRotationsRemainRigid(GameTestHelper helper) {
        boolean foundRotatedPart = false;
        for (CollisionPart part : GeneratedCollisionShapes.BATTERING_RAM_BODY.parts()) {
            Rotation3 rotation = part.pose().rotation();
            Vec3 x = rotation.axis(0);
            Vec3 y = rotation.axis(1);
            Vec3 z = rotation.axis(2);
            helper.assertTrue(Math.abs(x.lengthSqr() - 1.0D) < GENERATED_ROTATION_EPSILON
                            && Math.abs(y.lengthSqr() - 1.0D) < GENERATED_ROTATION_EPSILON
                            && Math.abs(z.lengthSqr() - 1.0D) < GENERATED_ROTATION_EPSILON,
                    "Exported ram part changed scale");
            helper.assertTrue(Math.abs(x.dot(y)) < GENERATED_ROTATION_EPSILON
                            && Math.abs(x.dot(z)) < GENERATED_ROTATION_EPSILON
                            && Math.abs(y.dot(z)) < GENERATED_ROTATION_EPSILON,
                    "Exported ram part axes are not perpendicular");
            foundRotatedPart |= Math.abs(rotation.m01()) > EPSILON
                    || Math.abs(rotation.m02()) > EPSILON
                    || Math.abs(rotation.m10()) > EPSILON
                    || Math.abs(rotation.m12()) > EPSILON
                    || Math.abs(rotation.m20()) > EPSILON
                    || Math.abs(rotation.m21()) > EPSILON;
        }

        helper.assertTrue(foundRotatedPart, "Battering ram lost its authored inclined parts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inclinedRamPartBlocksInItsOwnFrame(GameTestHelper helper) {
        BatteringRamEntity ram = createRam(helper);
        CollisionGroup body = groupNamed(ram.collisionGroups(), "body");
        CollisionPart inclinedPart = body.parts().stream()
                .filter(part -> Math.abs(part.pose().rotation().m01()) > 0.05D
                        || Math.abs(part.pose().rotation().m10()) > 0.05D)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No inclined ram body part"));
        AABB partBox = inclinedPart.box();
        Vec3 localStart = new Vec3(0.0D, partBox.maxY + 0.25D, 0.0D);
        Vec3 localEnd = new Vec3(0.0D, partBox.minY - 0.25D, 0.0D);
        Vec3 worldStart = ram.collisionTransform().toWorld(body.fromPart(inclinedPart, localStart));
        Vec3 worldEnd = ram.collisionTransform().toWorld(body.fromPart(inclinedPart, localEnd));
        AABB probe = AABB.ofSize(worldStart, 0.1D, 0.1D, 0.1D);

        StructureCollisionResolver.ResolvedMovement movement = StructureCollisionResolver.resolveMovement(
                ram, probe, worldEnd.subtract(worldStart), 0.0D);
        helper.assertTrue(movement.allowed().lengthSqr()
                        < worldEnd.subtract(worldStart).lengthSqr() * 0.8D,
                "Movement crossed an inclined ram part without being blocked");

        Vec3 partPoint = new Vec3(0.1D, 0.2D, 0.05D);
        Vec3 structurePoint = body.fromPart(inclinedPart, partPoint);
        helper.assertTrue(body.toPart(inclinedPart, structurePoint).distanceToSqr(partPoint) < EPSILON,
                "Oriented ram part does not round-trip through its collision transforms");
        helper.succeed();
    }

    private static BatteringRamEntity createRam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BatteringRamEntity ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        helper.assertTrue(ram != null, "Failed to create a battering ram");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ram.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return ram;
    }

    private static CollisionGroup groupNamed(List<CollisionGroup> groups, String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No collision group named " + name));
    }
}
