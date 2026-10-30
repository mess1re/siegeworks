package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
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

@GameTestHolder(HwachaCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class HwachaCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;

    private HwachaCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void hwachaUsesOnlyStructuralModelBones(GameTestHelper helper) {
        HwachaEntity hwacha = createHwacha(helper);

        helper.assertTrue(hwacha.collisionGroups().stream().map(CollisionGroup::name).toList()
                        .equals(List.of("body")),
                "Hwacha collision is split into unexpected groups");
        helper.assertTrue(GeneratedCollisionShapes.HWACHA_BODY.parts().size() == 25,
                "Hwacha collision includes ammunition, wheels, or visual effect bones");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hwachaAimRotatesRackAroundAuthoredBasePivot(GameTestHelper helper) {
        HwachaEntity hwacha = createHwacha(helper);
        hwacha.setTrackedPitch(0.0F);
        CollisionGroup level = hwacha.collisionGroups().get(0);
        Vec3 pivot = level.shape().pivot();
        Vec3 rackFront = new Vec3(0.0D, 2.1D, 1.5D);
        Vec3 levelFront = level.fromGroup(rackFront);

        hwacha.setTrackedPitch(hwacha.getMinAimPitch());
        CollisionGroup raised = hwacha.collisionGroups().get(0);
        Vec3 raisedFront = raised.fromGroup(rackFront);

        helper.assertTrue(raisedFront.y > levelFront.y + 0.3D,
                "Hwacha rack tilted opposite to its rendered aiming direction");
        helper.assertTrue(level.fromGroup(pivot).distanceToSqr(raised.fromGroup(pivot)) < EPSILON,
                "Hwacha aiming moved its authored base pivot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hwachaCullingContainsCompleteRack(GameTestHelper helper) {
        HwachaEntity hwacha = createHwacha(helper);
        hwacha.setTrackedPitch(hwacha.getMinAimPitch());
        AABB modelBounds = StructureCollisionResolver.worldBounds(hwacha);

        helper.assertTrue(modelBounds != null && modelBounds.getYsize() > 2.5D,
                "Hwacha model bounds do not contain its raised rack");
        helper.assertTrue(contains(hwacha.getBoundingBoxForCulling(), modelBounds),
                "Hwacha culling bounds do not contain its complete model collision");
        helper.succeed();
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private static HwachaEntity createHwacha(GameTestHelper helper) {
        HwachaEntity hwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(hwacha != null, "Failed to create Hwacha");
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        hwacha.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(hwacha);
        return hwacha;
    }
}
