package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.FragmentFlight;
import me.mss1r.axiomata.ballistics.ImpactSource;
import me.mss1r.axiomata.ballistics.ProjectileCombat;
import me.mss1r.axiomata.ballistics.ProjectilePhysics;
import me.mss1r.axiomata.ballistics.impact.Fragmentation;
import me.mss1r.axiomata.ballistics.impact.LayeredImpact;
import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;


public final class PhysicalImpactGameTests {
    public static final String NAMESPACE = "siegeworks_physical_impacts";
    private PhysicalImpactGameTests() {}

    @GameTest(template = "empty")
    public static void physicalDamageDoesNotApplyVanillaArmorAgain(GameTestHelper helper) {
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
        var source = ProjectileCombat.source(helper.getLevel(), ProjectileCombat.PHYSICAL_PROJECTILE, null, null);
        helper.assertTrue(source.is(DamageTypeTags.BYPASSES_ARMOR), "Physical damage did not bypass the second armor calculation");
        float before = target.getHealth();
        helper.assertTrue(target.hurt(source, 5) && Math.abs(target.getHealth() - before + 5) < 1e-5,
                "Vanilla armor changed damage already resolved by the physical model");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anInertDirectHitDoesNotCreateSphericalDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var target = EntityType.COW.create(level);
        var neighbor = EntityType.COW.create(level);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
        target.setPos(at.x, at.y, at.z);
        neighbor.setPos(at.x + 1, at.y, at.z);
        level.addFreshEntity(target); level.addFreshEntity(neighbor);
        var shot = new CannonProbe(level);
        shot.setDeltaMovement(0, 0, 15);
        float targetBefore = target.getHealth(), neighborBefore = neighbor.getHealth();
        shot.hit(target);
        helper.assertTrue(target.getHealth() < targetBefore, "Physical cannon hit did not damage the target");
        helper.assertTrue(neighbor.getHealth() == neighborBefore, "An inert hit still applied an impact-damage sphere");
        target.discard(); neighbor.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorIsAContactPanelAndStillTransmitsABluntHit(GameTestHelper helper) {
        var target = EntityType.ZOMBIE.create(helper.getLevel());
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, -target.getBbWidth() / 2);
        var ball = LayeredImpact.Shot.circular(0.02, 0.017);
        var bare = ProjectilePhysics.resolve(ball, new Vec3(0, 0, 200), at, target);
        target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        var armored = ProjectilePhysics.resolve(ball, new Vec3(0, 0, 200), at, target);
        helper.assertTrue(armored.workJ() < bare.workJ(), "Armor did not reduce body resistance work");
        var stone = ProjectilePhysics.resolve(LayeredImpact.Shot.circular(26, 0.27), new Vec3(0, 0, 30), at, target);
        helper.assertTrue(stone.workJ() > 0, "A stopped heavy stone did not transmit work through the armor panel");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void fragmentFlightDoesNotSpawnAnEntityPerFragment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 3)));
        AABB area = new AABB(at, at).inflate(5);
        int entities = level.getEntities((Entity) null, area, entity -> true).size();
        var material = ImpactMaterialProfile.STONE;
        var fragments = Fragmentation.release(0.005, 10_000, material.fragmentation());
        helper.assertTrue(fragments.count() > 32, "The fixture did not exceed the trajectory sample count");
        int before = FragmentFlight.activeTrajectories(level);
        FragmentFlight.emit(level, SiegeBallisticsEnvironment.IMPACTS, ImpactSource.environmental(level, null), at,
                new Vec3(0, 1, 0), fragments, material, ResourceIds.id("minecraft", "stone"), 6, null);
        helper.assertTrue(level.getEntities((Entity) null, area, entity -> true).size() == entities,
                "Fragment simulation spawned Minecraft entities");
        helper.assertTrue(FragmentFlight.activeTrajectories(level) - before <= 32,
                "One cloud exceeded the representative trajectory limit");
        helper.succeed();
    }

    private static final class CannonProbe extends CannonProjectile {
        CannonProbe(ServerLevel level) { super(SiegeworksEntities.CANNON_BALL.get(), level); }
        void hit(LivingEntity target) {
            Vec3 at = target.position().add(0, target.getBbHeight() / 2, -target.getBbWidth() / 2);
            onHitEntity(new EntityHitResult(target, at));
        }
    }

    @GameTest(template = "empty", batch = "physical_fragment_contact")
    public static void aFragmentContactAppliesDamageAndMomentum(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = EntityType.COW.create(level);
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 3, 3)));
        target.setPos(at.x, at.y - target.getBbHeight() / 2, at.z);
        target.setNoAi(true);
        level.addFreshEntity(target);
        float before = target.getHealth();
        var material = ImpactMaterialProfile.IRON;
        var fragments = Fragmentation.fromBudgets(0.00001, 0, 20, material.fragmentation());
        FragmentFlight.emit(level, SiegeBallisticsEnvironment.IMPACTS, ImpactSource.environmental(level, null),
                at, new Vec3(0, 1, 0), fragments, material, ResourceIds.id("minecraft", "iron_block"), 6, null);
        FragmentFlight.tick(level);
        helper.assertTrue(target.isAlive() && target.getHealth() < before,
                "A real fragment trajectory did not apply its contact work to the target");
        helper.assertTrue(target.getDeltaMovement().lengthSqr() > 0,
                "The fragment dealt damage without transferring its resolved momentum");
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void masslessBlockShapesDoNotCreatePhysicalEjecta(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 5, 3));
        level.setBlock(pos, Blocks.DANDELION.defaultBlockState(), 2);
        var before = me.mss1r.siegeworks.config.SiegeworksServerConfig.getBlockDamage();
        me.mss1r.siegeworks.config.SiegeworksServerConfig.setBlockDamage(me.mss1r.siegeworks.config.SiegeBlockDamage.EVERYWHERE);
        try {
            SiegeBallisticsEnvironment.IMPACTS.blast(level, Vec3.atCenterOf(pos), new Vec3(0, 1, 0), 1, null);
            helper.assertTrue(level.getBlockState(pos).isAir(), "The zero-volume fixture was not processed");
        } finally {
            me.mss1r.siegeworks.config.SiegeworksServerConfig.setBlockDamage(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void secondaryEffectsKeepTheOriginalAttackerAndFriendlyFirePolicy(GameTestHelper helper) {
        var level = helper.getLevel();
        var original = EntityType.ARMOR_STAND.create(level);
        var replacement = EntityType.ARMOR_STAND.create(level);
        var ally = EntityType.ZOMBIE.create(level);
        var enemy = EntityType.ZOMBIE.create(level);
        var scoreboard = level.getScoreboard();
        var team = scoreboard.addPlayerTeam("impact_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        team.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(original.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        try {
            var shot = new CannonProbe(level);
            shot.setOwner(original);
            var source = ImpactSource.of(shot);
            shot.setOwner(replacement);
            shot.discard();
            helper.assertTrue(source.owner() == original && !source.mayHit().test(ally) && source.mayHit().test(enemy),
                    "A released cloud lost its original attacker or team policy");
            helper.assertTrue(source.damage().apply(enemy, 1f) && enemy.getLastHurtByMob() == original,
                    "Secondary damage was attributed to the replacement owner");
        } finally {
            scoreboard.removePlayerTeam(team);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "physical_fragment_cover")
    public static void coverInterceptsTheSameCloudThatHitsAnExposedTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 4, 2)));
        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000); target.setNoAi(true);
        target.setPos(at.x, at.y - target.getBbHeight() / 2, at.z + 2);
        level.addFreshEntity(target);
        var base = ImpactSource.environmental(level, null);
        var source = new ImpactSource(null, null, false, entity -> entity == target, base.damage());
        double mass = 7800 * Math.PI * .003 * .003 * .003 / 6;
        var fragments = new Fragmentation.Cloud(128, mass, .003, 50, 128 * .5 * mass * 50 * 50, 0, 0);
        Runnable emit = () -> {
            FragmentFlight.emit(level, SiegeBallisticsEnvironment.IMPACTS, source, at, new Vec3(0, 0, 1),
                    fragments, ImpactMaterialProfile.IRON, ResourceIds.id("minecraft", "iron_block"), 6, null);
            for (int tick = 0; tick < 8; tick++) FragmentFlight.tick(level);
        };
        emit.run();
        helper.assertTrue(target.getHealth() < 1000, "The exposed control target was not reached by the cloud");
        target.setHealth(1000); target.setDeltaMovement(Vec3.ZERO); target.invulnerableTime = 0;
        BlockPos wall = helper.absolutePos(new BlockPos(3, 4, 3));
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++)
            level.setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 2);
        emit.run();
        helper.assertTrue(target.getHealth() == 1000, "A stone wall did not intercept physical fragments");
        target.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void secondaryHitsDoNotDependOnTheVanillaDamageWindow(GameTestHelper helper) {
        var target = EntityType.COW.create(helper.getLevel());
        var source = ProjectileCombat.source(helper.getLevel(), ProjectileCombat.PHYSICAL_PROJECTILE, null, null);
        float before = target.getHealth();
        target.hurt(source, 2); target.hurt(source, 3);
        helper.assertTrue(Math.abs(before - target.getHealth() - 5) < 1e-5,
                "Grouping fragment contacts across calls changed their total damage");
        helper.succeed();
    }
}
