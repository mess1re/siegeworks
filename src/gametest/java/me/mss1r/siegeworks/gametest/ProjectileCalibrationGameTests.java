package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ballistics.ProjectilePhysics;
import me.mss1r.axiomata.ballistics.impact.LayeredImpact;
import me.mss1r.siegeworks.entity.projectile.TowerCrossbowBoltProjectile;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;


public final class ProjectileCalibrationGameTests {
    private ProjectileCalibrationGameTests() {}

    @GameTest(template = "empty")
    public static void referenceHpIsLiteralAndSpeedLossIsAppliedOnce(GameTestHelper helper) {
        for (double speed : new double[]{120, 108, 60}) {
            LivingEntity target = healthyTarget(helper.getLevel());
            var shot = new BoltProbe(helper.getLevel());
            shot.setBaseDamage(120);
            shot.setReferenceSpeed(120);
            shot.setDeltaMovement(0, 0, speed / 20);
            float before = target.getHealth();
            shot.hit(target);
            double expected = 120 * speed * speed / (120 * 120);
            helper.assertTrue(Math.abs(before - target.getHealth() - expected) < .001,
                    "Expected " + expected + " HP at " + speed + " m/s, got " + (before - target.getHealth()));
            helper.assertTrue(shot.getBaseDamage() == 120 && shot.getReferenceSpeed() == 120,
                    "The impact rewrote the reference calibration");
            target.discard(); shot.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aSurvivingTargetDoesNotPreventPhysicalPassage(GameTestHelper helper) {
        LivingEntity target = healthyTarget(helper.getLevel());
        var shot = new BoltProbe(helper.getLevel());
        shot.setBaseDamage(1);
        shot.setReferenceSpeed(120);
        shot.setDeltaMovement(0, 0, 6);
        shot.hit(target);
        helper.assertTrue(target.isAlive() && !shot.isEmbedded() && !shot.isRemoved(),
                "A surviving target stopped a bolt which physically traversed its body");
        helper.assertTrue(shot.getZ() > target.getBoundingBox().maxZ && shot.getDeltaMovement().z > 0,
                "The bolt was not placed beyond the actual exit point");
        target.discard(); shot.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lowEnergyBoltStopsEvenWithExtremeConfiguredDamage(GameTestHelper helper) {
        LivingEntity target = healthyTarget(helper.getLevel());
        var shot = new BoltProbe(helper.getLevel());
        shot.setBaseDamage(10000);
        shot.setReferenceSpeed(120);
        shot.setDeltaMovement(0, 0, .05);
        shot.hit(target);
        helper.assertTrue(target.isAlive() && shot.isEmbedded(),
                "Configured HP overrode the physical stop inside the body");
        target.discard(); shot.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void referenceCalibrationSurvivesSavingASlowedProjectile(GameTestHelper helper) {
        var original = new BoltProbe(helper.getLevel());
        original.setBaseDamage(120); original.setReferenceSpeed(120);
        original.setDeltaMovement(0, 0, 3);
        CompoundTag tag = new CompoundTag();
        original.addAdditionalSaveData(tag);
        var restored = new BoltProbe(helper.getLevel());
        restored.readAdditionalSaveData(tag);
        restored.setDeltaMovement(0, 0, 3);
        LivingEntity target = healthyTarget(helper.getLevel());
        float before = target.getHealth(); restored.hit(target);
        helper.assertTrue(restored.getReferenceSpeed() == 120 && Math.abs(before - target.getHealth() - 30) < .001,
                "Reloading normalized the slowed projectile to full damage");
        original.discard(); restored.discard(); target.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armorAndToughnessStillReduceDirectDamage(GameTestHelper helper) {
        LivingEntity target = healthyTarget(helper.getLevel());
        target.getAttribute(Attributes.ARMOR).setBaseValue(20);
        target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        var shot = new BoltProbe(helper.getLevel());
        shot.setBaseDamage(12); shot.setReferenceSpeed(120); shot.setDeltaMovement(0, 0, 6);
        float before = target.getHealth(); shot.hit(target);
        float damage = before - target.getHealth();
        helper.assertTrue(damage > 0 && damage < 12, "Direct calibrated HP bypassed armor: " + damage);
        target.discard(); shot.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void penetrationUsesTheTraversedLengthAndNotEntityHealth(GameTestHelper helper) {
        LivingEntity target = healthyTarget(helper.getLevel());
        var shot = LayeredImpact.Shot.circular(.5, .03);
        double z = target.getBoundingBox().minZ;
        Vec3 middle = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, z);
        var longPath = ProjectilePhysics.resolve(shot, new Vec3(0, 0, 120), middle, target);
        Vec3 shortEntry = new Vec3(target.getX(), middle.y, target.getBoundingBox().maxZ - .05);
        var shortPath = ProjectilePhysics.resolve(shot, new Vec3(0, 0, 120), shortEntry, target);
        helper.assertTrue(shortPath.exitSpeedMps() > longPath.exitSpeedMps(),
                "A short path through the body consumed the same speed as a central hit");
        target.setHealth(1);
        var lowHealth = ProjectilePhysics.resolve(shot, new Vec3(0, 0, 120), middle, target);
        helper.assertTrue(Math.abs(lowHealth.exitSpeedMps() - longPath.exitSpeedMps()) < 1e-9
                && lowHealth.passedThrough() == longPath.passedThrough(), "HP changed physical penetration");
        target.discard(); helper.succeed();
    }

    @GameTest(template = "empty", batch = "calibration_leaves")
    public static void oneLeafBlockReducesVelocityWithoutReducingReferenceDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 4, 3));
        level.setBlock(pos, Blocks.OAK_LEAVES.defaultBlockState(), 2);
        var shot = new BoltProbe(level);
        shot.setBaseDamage(120); shot.setReferenceSpeed(120); shot.setDeltaMovement(0, 0, 6);
        shot.reachBlocksAs(null);
        var previous = SiegeworksServerConfig.getBlockDamage();
        LivingEntity target = healthyTarget(level);
        try {
            SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
            shot.passBlock(pos);
            double retained = shot.getDeltaMovement().length() / 6;
            helper.assertTrue(retained > 0 && retained < 1, "Leaves did not consume projectile speed");
            helper.assertTrue(shot.getBaseDamage() == 120 && shot.getReferenceSpeed() == 120,
                    "Block penetration rewrote the damage calibration");
            float before = target.getHealth(); shot.hit(target);
            double damage = before - target.getHealth();
            helper.assertTrue(Math.abs(damage - 120 * retained * retained) < .001,
                    "Leaf penetration reduced direct damage more than once: " + damage);
            helper.assertTrue(damage > 115 && damage < 117,
                    "The one-leaf reference case changed unexpectedly: " + damage);
        } finally {
            SiegeworksServerConfig.setBlockDamage(previous);
            target.discard(); shot.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zeroHpDamageDoesNotDisablePhysicalMomentum(GameTestHelper helper) {
        Vec3 previousVelocity = null;
        for (double damage : new double[]{0, 120}) {
            var target = healthyTarget(helper.getLevel());
            var shot = new BoltProbe(helper.getLevel());
            try {
                shot.setBaseDamage(damage); shot.setReferenceSpeed(120); shot.setDeltaMovement(0, 0, 6);
                float before = target.getHealth(); shot.hit(target);
                if (damage == 0) helper.assertTrue(target.getHealth() == before, "A zero-HP hit damaged its target");
                Vec3 velocity = target.getDeltaMovement();
                helper.assertTrue(velocity.lengthSqr() > 0, "A zero-HP hit lost its physical impulse");
                if (previousVelocity != null) helper.assertTrue(velocity.distanceToSqr(previousVelocity) < 1e-12,
                        "Changing baseDamage changed the physical impulse");
                previousVelocity = velocity;
            } finally { target.discard(); shot.discard(); }
        }
        helper.succeed();
    }

    private static LivingEntity healthyTarget(ServerLevel level) {
        var target = EntityType.COW.create(level);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000); target.setNoAi(true);
        level.addFreshEntity(target);
        return target;
    }

    private static final class BoltProbe extends TowerCrossbowBoltProjectile {
        BoltProbe(ServerLevel level) { super(SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), level); }
        void passBlock(BlockPos pos) {
            Vec3 at = Vec3.atCenterOf(pos).add(0, 0, -.5);
            setPos(at);
            driveInto((ServerLevel) level(), new BlockHitResult(at, Direction.NORTH, pos, false));
        }
        void hit(LivingEntity target) {
            Vec3 at = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getBoundingBox().minZ);
            onHitEntity(new EntityHitResult(target, at));
        }
    }
}
