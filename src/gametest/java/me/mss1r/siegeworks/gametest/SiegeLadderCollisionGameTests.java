package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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

@GameTestHolder(SiegeLadderCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeLadderCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;

    private SiegeLadderCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void ladderCollisionContainsOnlyVisibleSections(GameTestHelper helper) {
        SiegeLadderEntity ladder = createLadder(helper);
        ladder.setSections(1);

        helper.assertTrue(groupNames(ladder).equals(List.of("base", "section_1")),
                "One-section ladder collision does not match its rendered sections");
        ladder.setSections(4);
        helper.assertTrue(groupNames(ladder).equals(
                        List.of("base", "section_1", "section_2", "section_3", "section_4")),
                "Four-section ladder collision does not contain every rendered section");
        helper.assertTrue(GeneratedCollisionShapes.SIEGE_LADDER_BASE.parts().size() == 7
                        && GeneratedCollisionShapes.SIEGE_LADDER_SECTION_1.parts().size() == 8
                        && GeneratedCollisionShapes.SIEGE_LADDER_SECTION_4.parts().size() == 8,
                "Ladder collision no longer matches its rails and rungs");
        helper.assertTrue(ladder.solidCollisionGroups().isEmpty()
                        && ladder.previousSolidCollisionGroups().isEmpty(),
                "Ladder model details must not fight its specialised traversal surface");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ladderCanFallTowardEitherSideAroundOneRoot(GameTestHelper helper) {
        SiegeLadderEntity ladder = createLadder(helper);
        ladder.setSections(4);
        setLeanDegrees(ladder, 0.0F);
        CollisionGroup upright = lastGroup(ladder);
        Vec3 top = new Vec3(0.0D, 15.0D, 0.0D);
        Vec3 uprightTop = upright.fromGroup(top);

        setLeanDegrees(ladder, 44.5F);
        CollisionGroup forward = lastGroup(ladder);
        Vec3 forwardTop = forward.fromGroup(top);
        setLeanDegrees(ladder, -44.5F);
        CollisionGroup backward = lastGroup(ladder);
        Vec3 backwardTop = backward.fromGroup(top);

        helper.assertTrue(forwardTop.z > uprightTop.z + 5.0D
                        && backwardTop.z < uprightTop.z - 5.0D,
                "Ladder collision does not follow both rendered fall directions");
        helper.assertTrue(forwardTop.y < uprightTop.y - 3.0D
                        && backwardTop.y < uprightTop.y - 3.0D,
                "Ladder collision did not lower its top while falling");
        helper.assertTrue(forward.fromGroup(Vec3.ZERO).distanceToSqr(Vec3.ZERO) < 1.0E-7D
                        && backward.fromGroup(Vec3.ZERO).distanceToSqr(Vec3.ZERO) < 1.0E-7D,
                "Ladder sections do not share the authored root pivot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ladderUsesModelBoundsInsteadOfStretchedEntityBox(GameTestHelper helper) {
        SiegeLadderEntity ladder = createLadder(helper);
        ladder.setSections(4);
        setLeanDegrees(ladder, 66.75F);
        AABB modelBounds = StructureCollisionResolver.worldBounds(ladder);

        helper.assertTrue(ladder.getBoundingBox().getYsize() < 4.0D,
                "Ladder still stretches its technical entity box across the model");
        helper.assertTrue(modelBounds != null && modelBounds.getZsize() > 10.0D,
                "Ladder model bounds do not reach its upper section");
        helper.assertTrue(contains(ladder.getBoundingBoxForCulling(), modelBounds),
                "Ladder culling bounds do not contain all visible sections");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void ladderCanRestOnAnotherStructuresModelGeometry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create siege tower support");
        tower.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 15.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add siege tower support");

        SiegeLadderEntity ladder = createLadder(helper);
        ladder.setSections(4);
        setDeploymentState(ladder, 35, 7.0F);

        helper.runAfterDelay(120, () -> helper.succeedWhen(() -> {
            float lean = ladder.getLeanAngleDegrees();
            helper.assertTrue(lean > 9.0F && lean < 80.0F,
                    "Ladder did not come to rest on model geometry: lean=" + lean);
            helper.assertTrue(ladder.isReadyForAutomatedClimb(),
                    "Ladder resting on another structure was not usable");
        }));
    }

    private static List<String> groupNames(SiegeLadderEntity ladder) {
        return ladder.collisionGroups().stream().map(CollisionGroup::name).toList();
    }

    private static CollisionGroup lastGroup(SiegeLadderEntity ladder) {
        List<CollisionGroup> groups = ladder.collisionGroups();
        return groups.get(groups.size() - 1);
    }

    private static void setLeanDegrees(SiegeLadderEntity ladder, float degrees) {
        CompoundTag tag = new CompoundTag();
        ladder.addAdditionalSaveData(tag);
        tag.putFloat("LeanDegrees", degrees);
        ladder.readAdditionalSaveData(tag);
    }

    private static void setDeploymentState(SiegeLadderEntity ladder, int deployTicks, float degrees) {
        CompoundTag tag = new CompoundTag();
        ladder.addAdditionalSaveData(tag);
        tag.putInt("DeployTicks", deployTicks);
        tag.putFloat("LeanDegrees", degrees);
        ladder.readAdditionalSaveData(tag);
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private static SiegeLadderEntity createLadder(GameTestHelper helper) {
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(ladder != null, "Failed to create siege ladder");
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ladder.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(ladder);
        return ladder;
    }
}
