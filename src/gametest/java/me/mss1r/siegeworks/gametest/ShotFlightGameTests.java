package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.api.SiegeBallistics;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.entity.projectile.GiantCannonProjectile;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShotFlightGameTests {
    private static final int SCREEN = 12;

    private ShotFlightGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = Siegeworks.MOD_ID + "_migrations")
    public static void singijeonFlightRanges(GameTestHelper helper) {
        double plain = reach(SingijeonProjectile.physics(false));
        double explosive = reach(SingijeonProjectile.physics(true));
        helper.assertTrue(plain >= 135.0D && plain <= 165.0D,
                "So-singijeon range at 45 degrees: " + Math.round(plain));
        helper.assertTrue(explosive >= 185.0D && explosive <= 215.0D,
                "Jung-singijeon range at 45 degrees: " + Math.round(explosive));
        helper.succeed();
    }

    /** The trebuchet throws a 100 kg stone 200 m, matching the reconstruction at Castelnaud. */
    @GameTest(template = "empty")
    public static void trebuchetThrowsAsFarAsCastelnaudsReconstruction(GameTestHelper helper) {
        // Release point: 25.65 blocks up, at 45 degrees.
        double trebuchet = SiegeBallistics.range(launchSpeed(SiegeworksEntities.TREBUCHET_ENTITY.get()), 45.0D,
                25.65D, air(SiegeworksEntities.TREBUCHET_PROJECTILE.get()));
        helper.assertTrue(trebuchet >= 185.0D && trebuchet <= 215.0D,
                "A trebuchet threw its stone " + Math.round(trebuchet) + " m, not about 200");
        helper.succeed();
    }

    /**
     * At 100 blocks the faster arcballista bolt takes about 1.5 degrees of elevation; the tower bolt about 2.
     */
    @GameTest(template = "empty")
    public static void boltsKeepAShallowArcAtAHundredBlocks(GameTestHelper helper) {
        for (EntityType<?>[] thrower : java.util.List.<EntityType<?>[]>of(
                new EntityType<?>[]{SiegeworksEntities.ARCBALLISTA_ENTITY.get(),
                        SiegeworksEntities.ARCBALLISTA_BOLT_PROJECTILE.get()},
                new EntityType<?>[]{SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get(),
                        SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get()})) {
            double over = Math.abs(SiegeBallistics.calculateLowAnglePitch(Vec3.ZERO, new Vec3(0.0D, 0.0D, 100.0D),
                    launchSpeed(thrower[0]), air(thrower[1])));
            double expected = thrower[0] == SiegeworksEntities.ARCBALLISTA_ENTITY.get() ? 1.5D : 2.0D;
            helper.assertTrue(Math.abs(over - expected) <= 0.25D, thrower[0].toShortString() + " takes "
                    + String.format(java.util.Locale.ROOT, "%.2f", over) + " degrees at 100 blocks, not about " + expected);
        }
        helper.succeed();
    }

    private static double launchSpeed(EntityType<?> engine) {
        return SiegeProfileCatalogs.ENGINES.forEntity(engine).muzzleVelocity() / 20.0D;
    }

    private static SiegeBallistics.Flight air(EntityType<?> projectile) {
        ProjectilePhysicsProfile physics = SiegeProfileCatalogs.PROJECTILES.forEntity(projectile);
        return SiegeBallistics.Flight.ballistic(physics.airDrag(physics.diameterOf(projectile)));
    }

    private static double reach(ProjectilePhysicsProfile rocket) {
        return SiegeBallistics.range(SingijeonProjectile.launchSpeed(rocket), 45.0D, 0.0D,
                SingijeonProjectile.flight(rocket));
    }

    /**
     * A Mons Meg ball passes through a wall of leaves without being stopped by the debris. Own batch: the ball lands
     * sixty blocks past the test area.
     */
    @GameTest(template = "empty", timeoutTicks = 40, batch = "long_flight")
    public static void aShotFliesOnThroughTheLeavesItTears(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // The gun has no owner, so block damage needs the EVERYWHERE rule.
        SiegeBlockDamage before = SiegeworksServerConfig.getBlockDamage();
        SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 1));
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-3, -1, -2), origin.offset(3, 4, 60))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, 0, SCREEN), origin.offset(2, 3, SCREEN))) {
            level.setBlock(pos, leaves, 2);
        }
        MonsMegEntity gun = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
        gun.moveTo(origin.getX() + 0.5D, origin.getY(), origin.getZ() - 1.0D);
        level.addFreshEntity(gun);
        GiantCannonProjectile shot = new GiantCannonProjectile(SiegeworksEntities.GIANT_CANNON_BALL_PROJECTILE.get(),
                gun, level);
        shot.setPos(origin.getX() + 0.5D, origin.getY() + 1.5D, origin.getZ() + 0.5D);
        shot.setDeltaMovement(new Vec3(0.0D, 0.0D, 9.0D));
        level.addFreshEntity(shot);
        helper.runAfterDelay(6, () -> {
            try {
                helper.assertTrue(!shot.isRemoved() && shot.getZ() > origin.getZ() + SCREEN + 10.0D,
                        "The shot stopped at the leaves it tore through, at z " + (shot.getZ() - origin.getZ()));
            } finally {
                SiegeworksServerConfig.setBlockDamage(before);
                shot.discard();
                gun.discard();
            }
            helper.succeed();
        });
    }
}
