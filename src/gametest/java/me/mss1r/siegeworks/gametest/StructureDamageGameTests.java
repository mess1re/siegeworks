package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.ProjectileCombat;
import me.mss1r.axiomata.ballistics.ImpactSource;
import me.mss1r.axiomata.ballistics.ProjectileBlastResolver;
import me.mss1r.axiomata.ballistics.FragmentFlight;
import me.mss1r.axiomata.ballistics.impact.Fragmentation;
import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.axiomata.ballistics.damage.StructureDamage;
import me.mss1r.axiomata.ballistics.damage.StructureDamageTarget;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class StructureDamageGameTests {
    private StructureDamageGameTests() {}

    @GameTest(template = "empty")
    public static void receivingMachineScalesRealCannonImpactOnce(GameTestHelper helper) {
        var target = machine(helper);
        var shot = new CannonProbe(helper.getLevel());
        try {
            shot.setBaseDamage(220); shot.setReferenceSpeed(300); shot.setDeltaMovement(0, 0, 15);
            float before = target.getHealth();
            shot.hit(target);
            close(helper, before - target.getHealth(), 110, "A cannon's incoming multiplier was skipped or applied twice");
        } finally { target.discard(); shot.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void independentProjectileHitsAreNotLostToVanillaCooldown(GameTestHelper helper) {
        var target = machine(helper);
        try {
            var source = source(helper, "bolt");
            float before = target.getHealth();
            helper.assertTrue(target.hurt(source, 140), "First bolt was rejected");
            helper.assertTrue(target.hurt(source, 90), "Second bolt disappeared into the hurt cooldown");
            close(helper, before - target.getHealth(), 69, "Two independent bolts did not apply their own damage");
        } finally { target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anotherModCanSelectAProfileWithoutSiegeworksInheritance(GameTestHelper helper) {
        var target = new VariantTarget(helper.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        helper.getLevel().addFreshEntity(target);
        try {
            float before = target.getHealth();
            target.hurt(source(helper, "bolt"), 140);
            close(helper, before - target.getHealth(), 14, "A non-SW target could not choose the tower response profile");
        } finally { target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fragmentsKeepTheirCategoryAfterTheirProjectileIsRemoved(GameTestHelper helper) {
        var target = machine(helper);
        var projectile = new CannonProbe(helper.getLevel());
        var source = ProjectileCombat.source(helper.getLevel(), ProjectileCombat.PHYSICAL_PROJECTILE, projectile, null);
        projectile.discard();
        try {
            helper.assertTrue(StructureDamage.category(source).equals("fragment"), "A fragment inherited the parent's category");
            float before = target.getHealth(); target.hurt(source, 20);
            close(helper, before - target.getHealth(), 0.6, "The culverin fragment coefficient was not applied");
        } finally { target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grapeshotHitsUseTheLightProjectileResponseAndAllCount(GameTestHelper helper) {
        var target = machine(helper);
        try {
            float before = target.getHealth();
            for (int i = 0; i < 3; i++) helper.assertTrue(target.hurt(source(helper, "light_projectile"), 20),
                    "A separate pellet was rejected by the hurt cooldown");
            close(helper, before - target.getHealth(), 6, "Pellets used heavy-shell resistance or lost separate hits");
        } finally { target.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chemicalSplashIsIndependentOfTheNewDirectDamage(GameTestHelper helper) {
        var physics = SiegeProfileCatalogs.PROJECTILES.get(ResourceIds.id("siegeworks", "explosive_singijeon"));
        double previous = -1;
        for (double directDamage : new double[]{1, 10000}) {
            var target = EntityType.COW.create(helper.getLevel());
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
            Vec3 center = Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(4, 6, 4)));
            target.setPos(center.add(1, 0, 0)); helper.getLevel().addFreshEntity(target);
            var shot = new CannonProbe(helper.getLevel()); shot.setBaseDamage(directDamage);
            try {
                float before = target.getHealth();
                ProjectileBlastResolver.applyBlast(helper.getLevel(), center, ImpactSource.of(shot), physics, 2);
                double damage = before - target.getHealth();
                helper.assertTrue(damage > 0, "The charged projectile lost its chemical splash");
                if (previous >= 0) close(helper, damage, previous, "Changing direct HP changed chemical splash damage");
                previous = damage;
            } finally { target.discard(); shot.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "physical_fragment_contact")
    public static void zeroFragmentHpResponseDoesNotDisablePhysicalMomentum(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = new VariantTarget(level, ResourceIds.id("siegeworks_physical_impacts", "immune_to_fragments"));
        helper.assertTrue(StructureDamage.PROFILES.contains(target.structureDamageProfileId()), "The shared test profile was not packaged");
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(3, 4, 3)));
        target.setPos(center.x, center.y - target.getBbHeight() / 2, center.z);
        target.setNoAi(true); level.addFreshEntity(target);
        try {
            float before = target.getHealth();
            var material = ImpactMaterialProfile.IRON;
            var fragments = Fragmentation.fromBudgets(.00001, 0, 20, material.fragmentation());
            var environmental = ImpactSource.environmental(level, null);
            var source = new ImpactSource(null, null, false, entity -> entity == target, environmental.damage());
            FragmentFlight.emit(level, SiegeBallisticsEnvironment.IMPACTS, source, center, new Vec3(0, 1, 0),
                    fragments, material, ResourceIds.id("minecraft", "iron_block"), 6, null);
            FragmentFlight.tick(level);
            helper.assertTrue(target.getHealth() == before, "The zero fragment response did not prevent HP damage");
            helper.assertTrue(target.getDeltaMovement().lengthSqr() > 0, "The HP coefficient changed physical momentum");
        } finally { target.discard(); }
        helper.succeed();
    }

    private static AbstractSiegeEntity machine(GameTestHelper helper) {
        var target = SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel());
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1000);
        target.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new net.minecraft.core.BlockPos(3, 3, 3))));
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static DamageSource source(GameTestHelper helper, String type) {
        return ProjectileCombat.source(helper.getLevel(), ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceIds.id("axiomata", type)), null, null);
    }

    private static void close(GameTestHelper helper, double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < .001, message + ": " + actual + " instead of " + expected);
    }

    private static final class CannonProbe extends CannonProjectile {
        CannonProbe(ServerLevel level) { super(SiegeworksEntities.CANNON_BALL.get(), level); }
        void hit(AbstractSiegeEntity target) {
            Vec3 at = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2, target.getBoundingBox().minZ);
            onHitEntity(new EntityHitResult(target, at));
        }
    }

    private static final class VariantTarget extends Cow implements StructureDamageTarget {
        private final ResourceLocation damageProfile;
        VariantTarget(ServerLevel level) { this(level, ResourceIds.id("siegeworks", "siege_tower")); }
        VariantTarget(ServerLevel level, ResourceLocation profile) { super(EntityType.COW, level); damageProfile = profile; }
        @Override public ResourceLocation structureDamageProfileId() { return damageProfile; }
        @Override public boolean hurt(DamageSource source, float amount) {
            return super.hurt(source, StructureDamage.amount(this, source, amount));
        }
    }
}
