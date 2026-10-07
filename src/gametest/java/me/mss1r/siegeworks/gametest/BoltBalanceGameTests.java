package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(BoltBalanceGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BoltBalanceGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_bolt_balance";

    private BoltBalanceGameTests() {
    }

    @GameTest(template = "empty")
    public static void aTowerBoltCracksFreshSandstoneWithoutRemovingIt(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 4))) {
            level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }
        StructuralDamageSystem.tick(level);
        level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 2);
        ProjectilePhysicsProfile bolt = bolt();
        SiegeBlockDamage before = SiegeworksServerConfig.getBlockDamage();
        SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
        try {
            Vec3 along = new Vec3(0.0D, 0.0D, 1.0D);
            ProjectileImpacts.Drive drive = ProjectileImpacts.drive(level, bolt, bolt.diameter().orElseThrow(),
                    Vec3.atCenterOf(pos).add(0.0D, 0.0D, -0.5D), pos, along.scale(120.0D), null);
            helper.assertTrue(!drive.passedThrough(), "A tower bolt pierced fresh sandstone at full launch speed");
            ProjectileImpacts.stop(level, drive.mouth(), drive.face(), along, bolt, drive.speed(),
                    bolt.diameter().orElseThrow(), null);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.SANDSTONE),
                    "One tower bolt removed a whole fresh sandstone block");
            helper.assertTrue(StructuralDamageSystem.progress(level, pos, level.getBlockState(pos)) > 0.0F,
                    "The sandstone took no damage from the bolt");
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void boltsKeepAShallowArcAtAHundredBlocks(GameTestHelper helper) {
        ShotFlightGameTests.boltsKeepAShallowArcAtAHundredBlocks(helper);
    }

    @GameTest(template = "empty")
    public static void lighterMassDoesNotChangeDirectDamageAtTheSameImpactSpeed(GameTestHelper helper) {
        ProjectilePhysicsProfile bolt = bolt();
        ProjectilePhysicsProfile heavy = new ProjectilePhysicsProfile(1.5D, bolt.dragCoefficient(), bolt.diameter(),
                bolt.hardness(), bolt.motor(), bolt.entity(), bolt.shock(), bolt.blast(), bolt.fire());
        var target = EntityType.SHEEP.create(helper.getLevel());
        helper.assertTrue(target != null, "Failed to create a damage-test target");
        for (double speed : new double[]{6.0D, 5.0D, 2.0D}) {
            float current = ProjectilePhysics.entityDamage(bolt, 10.0F, speed, target);
            float previous = ProjectilePhysics.entityDamage(heavy, 10.0F, speed, target);
            helper.assertTrue(Math.abs(current - previous) < 1.0E-5F,
                    "Changing only mass changed direct damage at " + speed + " blocks/tick");
        }
        helper.succeed();
    }

    private static ProjectilePhysicsProfile bolt() {
        return SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get());
    }
}
