package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.ballistics.FragmentFlight;
import me.mss1r.axiomata.ballistics.ImpactResolver;
import me.mss1r.axiomata.ballistics.DebrisPhysics;
import me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile;
import me.mss1r.axiomata.ballistics.profile.BlockMaterialCatalog;
import me.mss1r.axiomata.ballistics.particle.ParticleSet;
import me.mss1r.axiomata.ballistics.ProtectedBlockAccess;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.data.profile.BlockMaterialProfiles;
import me.mss1r.siegeworks.particle.SiegeworksParticles;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.axiomata.ballistics.damage.StructureDamage;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Isolated fixed-target measurements; HP is observed before response coefficients and never saturates at death. */
public final class FragmentCalibrationGameTests {
    private static final Map<Entity, Double> OBSERVED = new IdentityHashMap<>();
    private static final String[] SOURCES = {"wood_wall", "stone_wall", "rocket_casing"};
    private static final String[] TARGETS = {"infantry", "culverin", "mantlet", "tower", "armored_infantry"};
    private static final double[] DISTANCES = {1, 3, 6};
    private static final int REPEATS = 8;
    private FragmentCalibrationGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 1200, batch = "fragment_calibration")
    public static void measureRealFragmentCloudsAgainstFixedTargets(GameTestHelper helper) {
        new Measurements(helper).next();
    }

    @GameTest(template = "empty", batch = "fragment_quota_regression")
    public static void coarseEntityQuotaDoesNotChangeFineMassEnergyOrDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos wall = helper.absolutePos(new BlockPos(6, 48, 6));
        Vec3 contact = new Vec3(wall.getX() + .5, wall.getY() + .5, wall.getZ());
        SiegeBlockDamage previous = SiegeworksServerConfig.getBlockDamage();
        double[] reference = null;
        try {
            SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
            for (int mode = 0; mode < 3; mode++) {
                int quota = mode == 0 ? 512 : mode == 1 ? 0 : 1;
                var debris = new DebrisPhysics("siegeworks", SiegeBallisticsEnvironment.BLOCKS, () -> true, () -> quota);
                var resolver = new ImpactResolver(new ImpactResolver.Environment() {
                    @Override public BlockMaterialCatalog materials() { return BlockMaterialProfiles.MATERIALS; }
                    @Override public double stoneFractureEnergy() { return SiegeworksServerConfig.getStoneFractureEnergy(); }
                    @Override public boolean projectileProof(BlockState state) { return state.is(ProjectileImpacts.PROJECTILE_PROOF); }
                    @Override public ProtectedBlockAccess blocks() { return SiegeBallisticsEnvironment.BLOCKS; }
                    @Override public DebrisPhysics debris() { return debris; }
                    @Override public ParticleSet particles() { return SiegeworksParticles.SET; }
                });
                FragmentFlight.clear();
                level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2); StructuralDamageSystem.tick(level);
                if (mode == 2) {
                    BlockPos dummy = wall.offset(8, 0, 0);
                    level.setBlock(dummy, Blocks.STONE.defaultBlockState(), 2);
                    helper.assertTrue(debris.launchDestroyedBlock(level, dummy, Vec3.atCenterOf(dummy), new Vec3(0, 0, -1), 2, null),
                            "Could not pre-fill the coarse debris quota");
                    removeDebris(level, wall);
                }
                level.random.setSeed(1701);
                var target = new Infantry(level);
                target.setNoGravity(true); target.setNoAi(true); target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                target.setPos(contact.x, contact.y - 1, contact.z - 1.3); level.addFreshEntity(target);
                OBSERVED.put(target, 0.0);
                var shot = new Probe(level, resolver);
                try {
                    BlockState state = Blocks.OAK_PLANKS.defaultBlockState();
                    level.setBlock(wall, state, 2);
                    var matter = resolver.material(level, wall, state);
                    double energy = shot.wall(wall, contact);
                    var physical = BlockMaterialProfiles.forState(state).flatMap(profile -> profile.impactMaterial())
                            .orElse(ImpactMaterialProfile.WOOD);
                    var split = me.mss1r.axiomata.ballistics.impact.SurfaceEjecta.release(
                            matter.volume() * matter.matter(), energy, physical.fragmentation());
                    int traces = FragmentFlight.activeTrajectories(level);
                    helper.assertTrue(traces > 0, "The quota regression did not produce fine fragments");
                    var coarse = level.getEntities((Entity) null, new AABB(wall).inflate(20),
                            entity -> entity instanceof FallingBlockEntity && !entity.isRemoved());
                    helper.assertTrue(mode == 0 ? !coarse.isEmpty() : coarse.isEmpty(), "The coarse quota setup did not take effect");
                    for (Entity entity : coarse) {
                        double kinetic = .5 * split.coarseMassKg() * entity.getDeltaMovement().scale(20).lengthSqr();
                        helper.assertTrue(kinetic <= split.coarseEnergyBudgetJ() + 1e-6, "The coarse entity reused fine-fragment energy");
                    }
                    for (int tick = 0; tick < 110 && FragmentFlight.activeTrajectories(level) > 0; tick++) {
                        FragmentFlight.tick(level); target.setDeltaMovement(Vec3.ZERO);
                    }
                    double[] actual = {energy, split.fine().totalMassKg(), split.fine().kineticEnergyJ(), traces, OBSERVED.get(target)};
                    if (reference == null) reference = actual;
                    else for (int i = 0; i < actual.length; i++) helper.assertTrue(Math.abs(actual[i] - reference[i]) < 1e-6,
                            "Coarse quota changed fine-fragment result " + i + ": " + actual[i] + " instead of " + reference[i]);
                    Siegeworks.LOG.info("Fragment quota mode={} energy={} mass={} kinetic={} traces={} damage={}", mode,
                            number(actual[0]), number(actual[1]), number(actual[2]), traces, number(actual[4]));
                } finally {
                    target.discard(); shot.discard(); OBSERVED.remove(target); FragmentFlight.clear();
                    level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2); StructuralDamageSystem.tick(level); removeDebris(level, wall);
                }
            }
        } finally { SiegeworksServerConfig.setBlockDamage(previous); }
        helper.succeed();
    }

    private static void removeDebris(ServerLevel level, BlockPos at) {
        for (Entity entity : level.getEntities((Entity) null, new AABB(at).inflate(20), entity -> entity instanceof FallingBlockEntity))
            entity.discard();
    }

    private static boolean observe(Entity target, DamageSource source, float amount) {
        if (source.is(StructureDamage.FRAGMENTS)) OBSERVED.merge(target, (double) amount, Double::sum);
        return true;
    }

    private static final class Measurements {
        final GameTestHelper helper;
        final ServerLevel level;
        final SiegeBlockDamage previous;
        final Map<String, double[]> rows = new LinkedHashMap<>();
        int trial;
        Measurements(GameTestHelper helper) {
            this.helper = helper; level = helper.getLevel(); previous = SiegeworksServerConfig.getBlockDamage();
        }

        void next() {
            if (trial == SOURCES.length * TARGETS.length * DISTANCES.length * REPEATS) {
                SiegeworksServerConfig.setBlockDamage(previous);
                rows.forEach((key, data) -> Siegeworks.LOG.info("Fragment calibration: {} n={} mean={} max={} hits={} traces={}",
                        key, (int) data[0], number(data[1] / data[0]), number(data[2]), (int) data[3], number(data[4] / data[0])));
                helper.assertTrue(rows.values().stream().anyMatch(row -> row[1] > 0), "No fragment contacts were measured");
                helper.succeed(); return;
            }
            int index = trial++;
            int seed = index % REPEATS;
            int distanceIndex = index / REPEATS % DISTANCES.length;
            int targetIndex = index / REPEATS / DISTANCES.length % TARGETS.length;
            int sourceIndex = index / REPEATS / DISTANCES.length / TARGETS.length;
            BlockPos wall = helper.absolutePos(new BlockPos(8, 48, 8));
            Vec3 contact = new Vec3(wall.getX() + .5 + seed * .000013, wall.getY() + .5, wall.getZ());
            LivingEntity target = create(targetIndex, level);
            var shot = new Probe(level);
            try {
                SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
                FragmentFlight.clear();
                level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2);
                StructuralDamageSystem.tick(level);
                level.random.setSeed(1701L + seed);
                target.setNoGravity(true);
                if (target instanceof Mob mob) mob.setNoAi(true);
                if (targetIndex == 0) target.getAttribute(Attributes.ARMOR).setBaseValue(0);
                target.setPos(contact.x, contact.y - 1, contact.z);
                AABB bounds = target instanceof CollidableStructure structure
                        ? StructureCollisionResolver.worldBounds(structure) : target.getBoundingBox();
                target.setPos(target.getX(), target.getY(), target.getZ() + contact.z - DISTANCES[distanceIndex] - bounds.maxZ);
                AABB reach = target.getBoundingBox().inflate(16);
                for (int x = (int) Math.floor(reach.minX / 16); x <= (int) Math.floor(reach.maxX / 16); x++)
                    for (int z = (int) Math.floor(reach.minZ / 16); z <= (int) Math.floor(reach.maxZ / 16); z++) level.getChunk(x, z);
                level.addFreshEntity(target);
                OBSERVED.put(target, 0.0);
                if (sourceIndex == 2) shot.casing(contact);
                else {
                    level.setBlock(wall, (sourceIndex == 0 ? Blocks.OAK_PLANKS : Blocks.STONE).defaultBlockState(), 2);
                    shot.wall(wall, contact);
                }
                int traces = FragmentFlight.activeTrajectories(level);
                for (int tick = 0; tick < 110 && FragmentFlight.activeTrajectories(level) > 0; tick++) {
                    FragmentFlight.tick(level);
                    target.setDeltaMovement(Vec3.ZERO);
                }
                double damage = OBSERVED.get(target);
                String key = SOURCES[sourceIndex] + "/" + TARGETS[targetIndex] + "/" + DISTANCES[distanceIndex] + "m";
                double[] row = rows.computeIfAbsent(key, unused -> new double[5]);
                row[0]++; row[1] += damage; row[2] = Math.max(row[2], damage); row[3] += damage > 0 ? 1 : 0; row[4] += traces;
            } catch (RuntimeException | Error failure) {
                SiegeworksServerConfig.setBlockDamage(previous); throw failure;
            } finally {
                target.discard(); shot.discard(); OBSERVED.remove(target); FragmentFlight.clear();
                level.setBlock(wall, Blocks.AIR.defaultBlockState(), 2); StructuralDamageSystem.tick(level);
                for (Entity entity : level.getEntities((Entity) null, new AABB(wall).inflate(20), entity -> entity instanceof FallingBlockEntity))
                    entity.discard();
            }
            helper.runAfterDelay(1, this::next);
        }
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.3f", value); }
    private static LivingEntity create(int type, ServerLevel level) {
        return switch (type) {
            case 0 -> new Infantry(level);
            case 1 -> new StructureProbe(SiegeworksEntities.CULVERIN_ENTITY.get().create(level), level);
            case 2 -> new StructureProbe(SiegeworksEntities.MANTLET_ENTITY.get().create(level), level);
            case 3 -> new StructureProbe(SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level), level);
            default -> armored(level);
        };
    }
    private static LivingEntity armored(ServerLevel level) {
        var target = new Infantry(level);
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_HELMET));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_CHESTPLATE));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_LEGGINGS));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_BOOTS));
        return target;
    }
    private static final class Infantry extends Zombie {
        Infantry(ServerLevel level) { super(EntityType.ZOMBIE, level); }
        @Override public boolean hurt(DamageSource source, float amount) { return observe(this, source, amount); }
    }
    /** Uses each real machine's posed collision mesh and mass; only HP reception is replaced by an observer. */
    private static final class StructureProbe extends LivingEntity implements CollidableStructure,
            me.mss1r.axiomata.ballistics.StructuralImpactTarget {
        private final me.mss1r.siegeworks.entity.base.AbstractSiegeEntity shape;
        @SuppressWarnings("unchecked")
        StructureProbe(me.mss1r.siegeworks.entity.base.AbstractSiegeEntity shape, ServerLevel level) {
            super((EntityType<? extends LivingEntity>) shape.getType(), level); this.shape = shape;
        }
        private void sync() { shape.setPos(position()); shape.setYRot(getYRot()); }
        @Override public java.util.List<me.mss1r.axiomata.collision.CollisionGroup> collisionGroups() {
            if (shape == null) return java.util.List.of();
            sync(); return shape.collisionGroups();
        }
        @Override public me.mss1r.axiomata.collision.StructureTransform collisionTransform() {
            sync(); return shape.collisionTransform();
        }
        @Override public me.mss1r.axiomata.collision.StructureTransform previousCollisionTransform() { return collisionTransform(); }
        @Override public me.mss1r.axiomata.ballistics.profile.ImpactMaterialProfile impactMaterial(Vec3 at) {
            sync(); return shape.impactMaterial(at);
        }
        @Override public double impactSolidVolumeM3() { return shape.impactSolidVolumeM3(); }
        @Override public Iterable<net.minecraft.world.item.ItemStack> getArmorSlots() { return java.util.List.of(); }
        @Override public net.minecraft.world.item.ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        @Override public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack stack) {}
        @Override public net.minecraft.world.entity.HumanoidArm getMainArm() { return net.minecraft.world.entity.HumanoidArm.RIGHT; }
        @Override public boolean hurt(DamageSource source, float amount) { return observe(this, source, amount); }
    }
    private static final class Probe extends CannonProjectile {
        private final ImpactResolver resolver;
        Probe(ServerLevel level) { this(level, null); }
        Probe(ServerLevel level, ImpactResolver resolver) { super(SiegeworksEntities.CANNON_BALL.get(), level); this.resolver = resolver; }
        @Override protected ImpactResolver impacts() { return resolver != null ? resolver : super.impacts(); }
        double wall(BlockPos pos, Vec3 contact) {
            setBaseDamage(220); setReferenceSpeed(300); setDeltaMovement(0, 0, 15); setPos(contact.add(0, 0, -.001)); reachBlocksAs(null);
            var drive = driveInto((ServerLevel) level(), new BlockHitResult(contact, Direction.NORTH, pos, false));
            if (!drive.passedThrough()) arrive((ServerLevel) level(), drive.position(), drive.speed());
            double remainingSpeed = drive.passedThrough() ? drive.speed() * 20 : 0;
            return .5 * getPhysicsProfile().mass() * (300 * 300 - remainingSpeed * remainingSpeed);
        }
        void casing(Vec3 contact) {
            setPhysicsProfile(ResourceIds.id("siegeworks", "explosive_singijeon"));
            setBaseDamage(20); setReferenceSpeed(40); setDeltaMovement(0, 0, 2); setPos(contact);
            arrive((ServerLevel) level(), contact, 2);
        }
    }
}
