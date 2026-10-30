package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.CollisionPose;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.item.SiegeAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(OnagerCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class OnagerCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-6D;

    private OnagerCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void onagerUsesOnlyPhysicalModelGroups(GameTestHelper helper) {
        OnagerEntity onager = createOnager(helper);
        List<String> names = onager.collisionGroups().stream().map(CollisionGroup::name).toList();

        helper.assertTrue(names.equals(List.of("body", "arm")),
                "Onager collision contains visual-only groups: " + names);
        helper.assertTrue(GeneratedCollisionShapes.ONAGER_BODY.parts().size() == 12,
                "Onager frame collision does not match the authored base bone");
        helper.assertTrue(GeneratedCollisionShapes.ONAGER_ARM.parts().size() == 4,
                "Onager arm collision does not match the authored catapult bone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onagerReloadFollowsAuthoredArmTrack(GameTestHelper helper) {
        OnagerEntity onager = createOnager(helper);
        Vec3 unloadedCenter = armCenter(onager);

        onager.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        Vec3 loadedCenter = armCenter(onager);
        Vec3 expectedLoadedCenter = authoredArmCenter(-60.0D);
        helper.assertTrue(loadedCenter.distanceToSqr(expectedLoadedCenter) < EPSILON,
                "Loading the onager did not apply the exported -60 degree arm pose");
        helper.assertTrue(loadedCenter.distanceToSqr(unloadedCenter) > 0.1D,
                "Loading the onager did not move its arm collision");

        onager.setWindingTime(100);
        Vec3 reloadStart = armCenter(onager);
        onager.setWindingTime(50);
        Vec3 reloadHalf = armCenter(onager);
        helper.assertTrue(reloadStart.distanceToSqr(unloadedCenter) < EPSILON,
                "Onager reload does not start at the authored unloaded pose");
        double fullTravel = reloadStart.distanceTo(loadedCenter);
        helper.assertTrue(reloadHalf.distanceTo(reloadStart) < fullTravel
                        && reloadHalf.distanceTo(loadedCenter) < fullTravel,
                "Onager reload collision skipped or reversed the authored arm travel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onagerShotMovesCollisionAndReleasesAlongTheLaunchLine(GameTestHelper helper) {
        OnagerEntity onager = createOnager(helper);
        ServerLevel level = helper.getLevel();
        level.addFreshEntity(onager);
        onager.setAmmoLoaded(SiegeAmmo.AMMO_STONE);

        onager.handleSiegeInteraction(
                SiegeGameTestPlayers.create(level), InteractionHand.MAIN_HAND, level);
        helper.assertTrue(onager.getShootAnimationTick() == 0,
                "Onager did not start its synchronized shot track");

        Vec3 bodyAtRest = bodyCenter(onager);
        // The cup runs along the launch line a third of the way into the second tick of its swing.
        onager.onSiegeTick(level);
        helper.assertTrue(onager.hasAmmoLoaded(),
                "Onager released its projectile before its cup ran along the launch line");
        onager.onSiegeTick(level);
        helper.assertTrue(onager.getShootAnimationTick() == 2 && !onager.hasAmmoLoaded(),
                "Onager did not release its projectile on animation tick 2");
        onager.onSiegeTick(level);

        onager.onSiegeTick(level);
        helper.assertTrue(bodyCenter(onager).distanceToSqr(bodyAtRest) > 1.0E-5D,
                "Onager frame collision ignored the authored recoil track");
        onager.onSiegeTick(level);
        onager.onSiegeTick(level);
        helper.assertTrue(armCenter(onager).distanceToSqr(unloadedArmCenter(onager)) < EPSILON,
                "Onager arm collision did not finish at the authored unloaded pose");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onagerShotStateSurvivesSaveAndLoad(GameTestHelper helper) {
        OnagerEntity source = createOnager(helper);
        ServerLevel level = helper.getLevel();
        source.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        source.handleSiegeInteraction(SiegeGameTestPlayers.create(level), InteractionHand.MAIN_HAND, level);
        source.onSiegeTick(level);

        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);
        OnagerEntity restored = createOnager(helper);
        restored.readAdditionalSaveData(saved);

        helper.assertTrue(restored.getShootAnimationTick() == 1 && restored.hasAmmoLoaded(),
                "Onager lost its in-progress shot while loading NBT");
        restored.onSiegeTick(level);
        restored.onSiegeTick(level);
        helper.assertTrue(!restored.hasAmmoLoaded(),
                "Restored onager did not release its projectile on the authored tick");
        helper.succeed();
    }

    private static Vec3 bodyCenter(OnagerEntity onager) {
        CollisionGroup body = onager.collisionGroups().get(0);
        return body.fromPart(body.parts().get(0), Vec3.ZERO);
    }

    private static Vec3 armCenter(OnagerEntity onager) {
        CollisionGroup arm = onager.collisionGroups().get(1);
        return arm.fromPart(arm.parts().get(1), Vec3.ZERO);
    }

    private static Vec3 unloadedArmCenter(OnagerEntity onager) {
        CollisionGroup body = onager.collisionGroups().get(0);
        CollisionGroup arm = new CollisionGroup("arm", GeneratedCollisionShapes.ONAGER_ARM, body.pose());
        return arm.fromPart(arm.parts().get(1), Vec3.ZERO);
    }

    private static Vec3 authoredArmCenter(double degrees) {
        CollisionPart beam = GeneratedCollisionShapes.ONAGER_ARM.parts().get(1);
        Vec3 center = beam.pose().toStructure(Vec3.ZERO);
        return CollisionPose.aroundX(
                GeneratedCollisionShapes.ONAGER_ARM.pivot(), (float) Math.toRadians(degrees))
                .toStructure(center);
    }

    private static OnagerEntity createOnager(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        OnagerEntity onager = SiegeworksEntities.ONAGER_ENTITY.get().create(level);
        helper.assertTrue(onager != null, "Failed to create onager");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        onager.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        return onager;
    }
}
