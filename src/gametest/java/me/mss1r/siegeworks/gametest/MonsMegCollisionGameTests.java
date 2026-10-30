package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.ScalarAnimationCurve;
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

@GameTestHolder(MonsMegCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MonsMegCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;

    private MonsMegCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void monsMegUsesCarriageAndBarrelWithoutWheels(GameTestHelper helper) {
        MonsMegEntity monsMeg = createMonsMeg(helper);
        List<String> groupNames = monsMeg.collisionGroups().stream().map(CollisionGroup::name).toList();

        helper.assertTrue(groupNames.equals(List.of("body")),
                "Mons Meg collision contains visual-only groups: " + groupNames);
        helper.assertTrue(GeneratedCollisionShapes.MONS_MEG_BODY.parts().size() == 8,
                "Mons Meg collision must contain seven carriage parts and one barrel envelope");
        helper.assertTrue(maximumAbsoluteX(GeneratedCollisionShapes.MONS_MEG_BODY.parts()) < 0.8D,
                "Mons Meg collision unexpectedly includes its wheels");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void monsMegAimRotatesCollisionAroundAuthoredPivot(GameTestHelper helper) {
        MonsMegEntity monsMeg = createMonsMeg(helper);
        CollisionGroup level = monsMeg.collisionGroups().get(0);
        CollisionPart barrel = level.parts().get(level.parts().size() - 1);
        AABB barrelBox = barrel.box();
        Vec3 muzzle = new Vec3(
                (barrelBox.minX + barrelBox.maxX) * 0.5D,
                (barrelBox.minY + barrelBox.maxY) * 0.5D,
                barrelBox.maxZ);
        Vec3 levelMuzzle = level.fromPart(barrel, muzzle);

        monsMeg.setTrackedPitch(monsMeg.getMinAimPitch());
        CollisionGroup raised = monsMeg.collisionGroups().get(0);
        Vec3 raisedMuzzle = raised.fromPart(barrel, muzzle);
        Vec3 pivot = GeneratedCollisionShapes.MONS_MEG_BODY.pivot();

        helper.assertTrue(raisedMuzzle.y > levelMuzzle.y + 0.2D,
                "Raising Mons Meg did not raise its barrel collision");
        helper.assertTrue(level.fromGroup(pivot).distanceToSqr(raised.fromGroup(pivot)) < EPSILON,
                "Mons Meg aim moved the authored carriage pivot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scalarAnimationCurvePreservesAuthoredKeys(GameTestHelper helper) {
        ScalarAnimationCurve curve = ScalarAnimationCurve.of(
                ScalarAnimationCurve.key(0.0F, 0.0D, ScalarAnimationCurve.Interpolation.CATMULL_ROM),
                ScalarAnimationCurve.key(7.5F, 25.0D, ScalarAnimationCurve.Interpolation.EASE_IN_ELASTIC),
                ScalarAnimationCurve.key(17.5F, 25.0D, ScalarAnimationCurve.Interpolation.CATMULL_ROM),
                ScalarAnimationCurve.key(100.0F, 0.0D, ScalarAnimationCurve.Interpolation.CATMULL_ROM));

        helper.assertTrue(Math.abs(curve.sample(0.0F)) < EPSILON,
                "Animation curve changed its first authored value");
        helper.assertTrue(Math.abs(curve.sample(7.5F) - 25.0D) < EPSILON,
                "Animation curve missed an elastic keyframe");
        helper.assertTrue(Math.abs(curve.sample(17.5F) - 25.0D) < EPSILON,
                "Animation curve missed a Catmull-Rom keyframe");
        helper.assertTrue(Math.abs(curve.sample(100.0F)) < EPSILON,
                "Animation curve changed its final authored value");
        helper.succeed();
    }

    private static double maximumAbsoluteX(List<CollisionPart> parts) {
        double maximum = 0.0D;
        for (CollisionPart part : parts) {
            AABB box = part.box();
            for (double x : new double[]{box.minX, box.maxX}) {
                for (double y : new double[]{box.minY, box.maxY}) {
                    for (double z : new double[]{box.minZ, box.maxZ}) {
                        maximum = Math.max(maximum,
                                Math.abs(part.pose().toStructure(new Vec3(x, y, z)).x));
                    }
                }
            }
        }
        return maximum;
    }

    private static MonsMegEntity createMonsMeg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MonsMegEntity monsMeg = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
        helper.assertTrue(monsMeg != null, "Failed to create Mons Meg");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        monsMeg.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return monsMeg;
    }
}
