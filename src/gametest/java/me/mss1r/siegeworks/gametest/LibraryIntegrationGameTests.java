package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ballistics.DebrisPhysics;
import me.mss1r.axiomata.ballistics.BallisticsModule;
import me.mss1r.axiomata.ballistics.ProjectilePassThroughControl;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_library")
@PrefixGameTestTemplate(false)
public final class LibraryIntegrationGameTests {
    private LibraryIntegrationGameTests() {}

    @GameTest(template = "empty")
    public static void libraryOwnsFlashAndConsumerKeepsSmokeIds(GameTestHelper helper) {
        var flash = BuiltInRegistries.PARTICLE_TYPE.getKey(BallisticsModule.MUZZLE_CORE.get());
        var smoke = BuiltInRegistries.PARTICLE_TYPE.getKey(SiegeworksParticles.SIEGE_SMOKE.get());
        var heavySmoke = BuiltInRegistries.PARTICLE_TYPE.getKey(SiegeworksParticles.HEAVY_SIEGE_SMOKE.get());
        helper.assertTrue(flash != null && flash.toString().equals("axiomata:muzzle_core"),
                "Internal flash was not registered by the library");
        helper.assertTrue(smoke != null && smoke.toString().equals("siegeworks:siege_smoke")
                && heavySmoke != null && heavySmoke.toString().equals("siegeworks:heavy_siege_smoke"),
                "Consumer smoke IDs changed during the library update");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void libraryInterfaceAllowsProjectilePassage(GameTestHelper helper) {
        PassageTarget target = new PassageTarget(helper.getLevel(), true);
        ProjectileImpactEvent event = impact(helper, target);
        helper.assertTrue(skipsEntity(event), "Library-only consumer did not allow projectile passage");
        helper.assertTrue(target.lastEnd.equals(new Vec3(3.0D, 2.0D, 1.0D)),
                "Passage check did not receive the projectile movement segment");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void closedConsumerStillReceivesProjectileHit(GameTestHelper helper) {
        ProjectileImpactEvent event = impact(helper, new PassageTarget(helper.getLevel(), false));
        helper.assertTrue(!skipsEntity(event), "Closed consumer unexpectedly skipped its hit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryEntityStillReceivesProjectileHit(GameTestHelper helper) {
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        ArmorStand target = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        ProjectileImpactEvent event = new ProjectileImpactEvent(arrow, new EntityHitResult(target, Vec3.ZERO));
        post(event);
        helper.assertTrue(!skipsEntity(event), "Library altered an ordinary entity hit");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registeredConsumerHandlesSavedDebris(GameTestHelper helper) {
        FallingBlockEntity debris = EntityType.FALLING_BLOCK.create(helper.getLevel());
        helper.assertTrue(debris != null, "Could not create debris");
        debris.getPersistentData().putString("axiomata:debris_context", "siegeworks");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(DebrisPhysics.handleLanding(helper.getLevel(), debris, pos,
                Blocks.STONE.defaultBlockState()) != null, "SW debris handler was not registered at startup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void registeredConsumerRecognizesLegacyDebris(GameTestHelper helper) {
        FallingBlockEntity debris = EntityType.FALLING_BLOCK.create(helper.getLevel());
        helper.assertTrue(debris != null, "Could not create legacy debris");
        debris.getPersistentData().putBoolean("siegeworks:debris", true);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.assertTrue(DebrisPhysics.handleLanding(helper.getLevel(), debris, pos,
                Blocks.STONE.defaultBlockState()) != null, "Legacy saved debris lost its consumer handler");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryFallingBlocksRemainVanilla(GameTestHelper helper) {
        FallingBlockEntity falling = EntityType.FALLING_BLOCK.create(helper.getLevel());
        helper.assertTrue(falling != null, "Could not create falling block");
        helper.assertTrue(DebrisPhysics.handleLanding(helper.getLevel(), falling,
                helper.absolutePos(new BlockPos(1, 1, 1)), Blocks.SAND.defaultBlockState()) == null,
                "Library claimed an ordinary falling block");
        helper.succeed();
    }

    private static ProjectileImpactEvent impact(GameTestHelper helper, PassageTarget target) {
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        arrow.setPos(1.0D, 2.0D, 1.0D);
        arrow.setDeltaMovement(2.0D, 0.0D, 0.0D);
        ProjectileImpactEvent event = new ProjectileImpactEvent(arrow,
                new EntityHitResult(target, new Vec3(2.0D, 2.0D, 1.0D)));
        post(event);
        return event;
    }

    private static void post(ProjectileImpactEvent event) {
        //? if forge {
        /*MinecraftForge.EVENT_BUS.post(event);
        *///?} else {
        NeoForge.EVENT_BUS.post(event);
        //?}
    }

    private static boolean skipsEntity(ProjectileImpactEvent event) {
        //? if forge {
        /*return event.getImpactResult() == ProjectileImpactEvent.ImpactResult.SKIP_ENTITY;
        *///?} else {
        return event.isCanceled();
        //?}
    }

    private static final class PassageTarget extends ArmorStand implements ProjectilePassThroughControl {
        private final boolean open;
        private Vec3 lastEnd;

        private PassageTarget(Level level, boolean open) {
            super(EntityType.ARMOR_STAND, level);
            this.open = open;
        }

        @Override
        public boolean allowsProjectilePassage(Vec3 start, Vec3 end) {
            lastEnd = end;
            return open;
        }
    }
}
