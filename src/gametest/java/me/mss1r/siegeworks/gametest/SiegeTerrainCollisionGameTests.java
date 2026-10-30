package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.collision.SiegeTerrainCollision;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;

@GameTestHolder(SiegeTerrainCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeTerrainCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;

    private SiegeTerrainCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void anIntersectingPoseMustImproveBeforeItIsAccepted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AbstractSiegeEntity siege = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(siege != null, "Failed to create terrain-collision test machine");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        siege.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(siege);

        StructureTransform pose = siege.collisionTransform();
        List<CollisionGroup> groups = siege.solidCollisionGroups();
        AABB bounds = StructureCollisionResolver.worldBounds(pose, groups);
        helper.assertTrue(bounds != null && !groups.isEmpty(), "Test machine has no solid model geometry");

        BlockPos obstacle = null;
        int minY = Math.max((int) Math.floor(bounds.minY), (int) Math.floor(siege.getY() + 1.0D));
        for (BlockPos candidate : BlockPos.betweenClosed(
                (int) Math.floor(bounds.minX), minY, (int) Math.floor(bounds.minZ),
                (int) Math.floor(bounds.maxX), (int) Math.floor(bounds.maxY),
                (int) Math.floor(bounds.maxZ))) {
            if (StructureCollisionResolver.intersects(pose, groups, new AABB(candidate), 0.0D)) {
                obstacle = candidate.immutable();
                break;
            }
        }
        helper.assertTrue(obstacle != null, "Could not find a block occupied by the machine geometry");
        level.setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());

        helper.assertTrue(!SiegeTerrainCollision.canOccupy(siege, pose, pose),
                "A pose with unchanged terrain penetration was accepted as an escape");
        helper.succeed();
    }
}
