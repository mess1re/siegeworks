package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.ballistics.profile.BlockMaterialProfile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.data.profile.JsonProfileReloadListener;
import me.mss1r.axiomata.data.profile.ProfileCatalog;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@GameTestHolder(DatapackGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class DatapackGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_datapacks";
    private static final ResourceLocation ID = id("test_bolt");
    private static final ResourceLocation FILE = id("definitions/projectile_physics/test_bolt.json");
    private static final String NEW = "{\"formatVersion\":2,\"mass\":0.5,\"diameter\":0.03}";
    private static final String OLD = "{\"mass\":1.5,\"penetration\":1.0,\"entityDamageMultiplier\":1.35}";

    private DatapackGameTests() {
    }

    @GameTest(template = "empty")
    public static void fieldGunProfilesUseUpdatedMuzzleVelocities(GameTestHelper helper) {
        var culverin = SiegeProfileCatalogs.ENGINES.forEntity(SiegeworksEntities.CULVERIN_ENTITY.get());
        var serpentine = SiegeProfileCatalogs.ENGINES.forEntity(SiegeworksEntities.SERPENTINE_ENTITY.get());
        helper.assertTrue(culverin.muzzleVelocity() == 300.0D, "Culverin profile did not load 300 m/s");
        helper.assertTrue(serpentine.muzzleVelocity() == 330.0D, "Serpentine profile did not load 330 m/s");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oldFieldsAndTyposAreNotSilentlyDiscarded(GameTestHelper helper) {
        helper.assertTrue(ProfileFormat.projectile(JsonParser.parseString(OLD)).orElseThrow().contains("beta.5"),
                "Old projectile fields were accepted");
        helper.assertTrue(ProfileFormat.engine(JsonParser.parseString("{\"projectileSpeed\":212}")).isPresent(),
                "Old engine speed was accepted");
        for (String invalid : List.of("{\"formatVersion\":1}", "{\"formatVersion\":2.5}",
                "{\"dragCoeficient\":1.5}", "{\"entity\":{\"damge\":1}}", "{\"mass\":\"heavy\"}", "{\"entity\":null}")) {
            helper.assertTrue(ProfileFormat.projectile(JsonParser.parseString(invalid)).isPresent(),
                    "Accepted " + invalid);
        }
        helper.assertTrue(ProfileFormat.projectile(JsonParser.parseString(NEW)).isEmpty(), "Rejected format 2");
        helper.assertTrue(ProfileFormat.projectile(JsonParser.parseString("{\"mass\":0.5}")).isEmpty(),
                "Unversioned current-format JSON needs no migration");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void startupFallsBackToTheValidLowerPack(GameTestHelper helper) {
        var catalog = new ProfileCatalog<>(ProjectilePhysicsProfile.DEFAULT);
        var listener = loader(catalog);
        var resources = resources(resource("built-in", NEW), resource("old-pack", OLD));
        var prepared = listener.prepare(resources, InactiveProfiler.INSTANCE);
        helper.assertTrue(prepared.errors().get(0).contains("old-pack") && prepared.errors().get(0).contains(FILE.toString()),
                "Diagnostic did not identify the pack and file");
        listener.apply(prepared, resources, InactiveProfiler.INSTANCE);
        helper.assertTrue(catalog.hasSnapshot() && catalog.get(ID).mass() == 0.5D,
                "Startup used generic defaults or stale mass instead of the lower pack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void aBadReloadKeepsTheLastWorkingSnapshot(GameTestHelper helper) {
        var catalog = new ProfileCatalog<>(ProjectilePhysicsProfile.DEFAULT);
        var listener = loader(catalog);
        var valid = resources(resource("custom", "{\"mass\":0.7}"));
        listener.apply(listener.prepare(valid, InactiveProfiler.INSTANCE), valid, InactiveProfiler.INSTANCE);
        var invalid = resources(resource("built-in", NEW), resource("old-pack", OLD));
        listener.apply(listener.prepare(invalid, InactiveProfiler.INSTANCE), invalid, InactiveProfiler.INSTANCE);
        helper.assertTrue(catalog.get(ID).mass() == 0.7D, "Bad reload replaced the working custom profile");
        var repaired = resources(resource("custom", "{\"mass\":0.8}"));
        listener.apply(listener.prepare(repaired, InactiveProfiler.INSTANCE), repaired, InactiveProfiler.INSTANCE);
        helper.assertTrue(catalog.get(ID).mass() == 0.8D, "Repaired reload did not take effect");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void startupCannotUseGenericDefaultsForAnInvalidCustomProfile(GameTestHelper helper) {
        var catalog = new ProfileCatalog<>(ProjectilePhysicsProfile.DEFAULT);
        var listener = loader(catalog);
        var resources = resources(resource("invalid-custom", OLD));
        boolean rejected = false;
        try {
            listener.apply(listener.prepare(resources, InactiveProfiler.INSTANCE), resources, InactiveProfiler.INSTANCE);
        } catch (IllegalStateException exception) {
            rejected = exception.getMessage().contains(FILE.toString());
        }
        helper.assertTrue(rejected && !catalog.hasSnapshot(), "Invalid custom-only profile became a generic projectile");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void highestValidPackWinsAndResetForgetsThePreviousWorld(GameTestHelper helper) {
        var catalog = new ProfileCatalog<>(ProjectilePhysicsProfile.DEFAULT);
        var listener = loader(catalog);
        var resources = resources(resource("broken-lower", OLD), resource("valid-upper", NEW));
        var prepared = listener.prepare(resources, InactiveProfiler.INSTANCE);
        helper.assertTrue(prepared.errors().isEmpty(), "An overridden lower resource rejected the winning pack");
        listener.apply(prepared, resources, InactiveProfiler.INSTANCE);
        catalog.reset();
        helper.assertTrue(!catalog.hasSnapshot() && catalog.snapshot().isEmpty(), "Profiles leaked across worlds");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverProjectileProfilesSurviveTheirNetworkCodec(GameTestHelper helper) {
        var profile = SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get());
        var encoded = ProjectilePhysicsProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).result().orElseThrow();
        var decoded = ProjectilePhysicsProfile.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(profile.equals(decoded), "Projectile profile changed during serialization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidPhysicalValuesAreRejected(GameTestHelper helper) {
        for (String json : List.of("{\"mass\":0}", "{\"diameter\":1e999}",
                "{\"motor\":{\"thrust\":1e999,\"burnTime\":1}}", "{\"fire\":{\"chance\":2}}")) {
            var parsed = ProjectilePhysicsProfile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
            helper.assertTrue(parsed.isEmpty() || parsed.get().validationError().isPresent(), "Accepted " + json);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stoneFractureConfigUsesJoulesWithoutRescaling(GameTestHelper helper) {
        double before = SiegeworksServerConfig.getStoneFractureEnergy();
        var materials = BlockMaterialProfiles.CATALOG.snapshot();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var stone = Blocks.STONE.defaultBlockState();
        var planks = Blocks.OAK_PLANKS.defaultBlockState();
        try {
            BlockMaterialProfiles.CATALOG.publish(Map.of());
            helper.assertTrue(before == 50_000.0D, "Default fracture energy changed from 50000 J/m3");
            var normalStone = ProjectileImpacts.material(helper.getLevel(), pos, stone);
            var normalPlanks = ProjectileImpacts.material(helper.getLevel(), pos, planks);
            for (double energy : new double[]{25_000.0D, 50_000.0D, 100_000.0D}) {
                SiegeworksServerConfig.setStoneFractureEnergy(energy);
                var changedStone = ProjectileImpacts.material(helper.getLevel(), pos, stone);
                var changedPlanks = ProjectileImpacts.material(helper.getLevel(), pos, planks);
                helper.assertTrue(changedStone.fractureEnergy() == energy && changedStone.breakEnergy() == energy,
                        "Stone fracture energy was rescaled instead of using " + energy + " J/m3 directly");
                helper.assertTrue(Math.abs(changedPlanks.fractureEnergy()
                                - normalPlanks.fractureEnergy() * energy / before) < 1.0E-6D,
                        "Planks no longer scale from the stone baseline");
                helper.assertTrue(changedStone.strength() == normalStone.strength()
                                && changedStone.drag() == normalStone.drag()
                                && changedStone.volume() == normalStone.volume(),
                        "Fracture energy changed penetration resistance or block volume");
            }
        } finally {
            SiegeworksServerConfig.setStoneFractureEnergy(before);
            BlockMaterialProfiles.CATALOG.publish(materials);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void explicitFractureOverridesIgnoreTheStoneConfig(GameTestHelper helper) {
        double before = SiegeworksServerConfig.getStoneFractureEnergy();
        var materials = BlockMaterialProfiles.CATALOG.snapshot();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var slab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        try {
            BlockMaterialProfiles.CATALOG.publish(Map.of(id("fixed_slab"), material(
                    "{\"block\":\"minecraft:stone_slab\",\"fractureEnergy\":120000}")));
            for (double energy : new double[]{25_000.0D, 100_000.0D}) {
                SiegeworksServerConfig.setStoneFractureEnergy(energy);
                var changed = ProjectileImpacts.material(helper.getLevel(), pos, slab);
                helper.assertTrue(changed.fractureEnergy() == 120_000.0D && changed.breakEnergy() == 60_000.0D,
                        "Global baseline replaced a datapack's fracture energy or lost slab volume");
            }
        } finally {
            SiegeworksServerConfig.setStoneFractureEnergy(before);
            BlockMaterialProfiles.CATALOG.publish(materials);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockOverridesKeepShapeAndLeaveOtherBlocksAlone(GameTestHelper helper) {
        var before = BlockMaterialProfiles.CATALOG.snapshot();
        try {
            var stone = Blocks.STONE.defaultBlockState();
            var pos = helper.absolutePos(new BlockPos(2, 2, 2));
            var normal = ProjectileImpacts.material(helper.getLevel(), pos, stone);
            BlockMaterialProfiles.CATALOG.publish(Map.of(id("strong_slabs"), material(
                    "{\"block\":\"minecraft:stone_slab\",\"strength\":33400000,\"drag\":1250,\"fractureEnergy\":100000}")));
            helper.assertTrue(normal.equals(ProjectileImpacts.material(helper.getLevel(), pos, stone)),
                    "An unrelated material changed");
            var slab = Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            var changed = ProjectileImpacts.material(helper.getLevel(), pos, slab);
            helper.assertTrue(Math.abs(changed.volume() - 0.5D) < 1.0E-6D && changed.breakEnergy() == 50000.0D,
                    "Material override lost the slab's half-block shape");
            helper.assertTrue(changed.strength() == 33400000.0D && changed.drag() == 1250.0D,
                    "Explicit penetration values were ignored");
        } finally {
            BlockMaterialProfiles.CATALOG.publish(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void directBlocksBeatTagsAndHigherTagPriorityWins(GameTestHelper helper) {
        var before = BlockMaterialProfiles.CATALOG.snapshot();
        var logs = material("{\"tag\":\"minecraft:logs\",\"priority\":10,\"fractureEnergy\":100000}");
        var spruce = material("{\"tag\":\"minecraft:spruce_logs\",\"priority\":20,\"fractureEnergy\":200000}");
        var direct = material("{\"block\":\"minecraft:spruce_log\",\"fractureEnergy\":300000}");
        try {
            BlockMaterialProfiles.CATALOG.publish(Map.of(id("logs"), logs, id("spruce"), spruce));
            helper.assertTrue(BlockMaterialProfiles.forState(Blocks.SPRUCE_LOG.defaultBlockState()).orElseThrow().equals(spruce),
                    "Higher-priority spruce tag lost to the general log tag");
            BlockMaterialProfiles.CATALOG.publish(Map.of(id("logs"), logs, id("spruce"), spruce, id("direct"), direct));
            helper.assertTrue(BlockMaterialProfiles.forState(Blocks.SPRUCE_LOG.defaultBlockState()).orElseThrow().equals(direct),
                    "A tag overrode the direct block profile");
        } finally {
            BlockMaterialProfiles.CATALOG.publish(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void projectileResistanceIsUsedByTheCuttingLimit(GameTestHelper helper) {
        var before = BlockMaterialProfiles.CATALOG.snapshot();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var stone = Blocks.STONE.defaultBlockState();
        var bolt = SiegeProfileCatalogs.PROJECTILES.forEntity(SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get());
        try {
            BlockMaterialProfiles.CATALOG.publish(Map.of(id("soft_stone"), material(
                    "{\"block\":\"minecraft:stone\",\"projectileResistance\":1}")));
            helper.assertTrue(ProjectileImpacts.canCut(bolt, helper.getLevel(), pos, stone),
                    "Cutting still used the block's vanilla explosion resistance");
            helper.assertTrue(!ProjectileImpacts.canCut(bolt, helper.getLevel(), pos, Blocks.BEDROCK.defaultBlockState()),
                    "Material settings allowed cutting bedrock");
        } finally {
            BlockMaterialProfiles.CATALOG.publish(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialTargetsAndValuesAreValidated(GameTestHelper helper) {
        for (String json : List.of("{\"block\":\"minecraft:stone\",\"tag\":\"minecraft:logs\",\"drag\":1}",
                "{\"block\":\"minecraft:not_a_block\",\"drag\":1}", "{\"block\":\"minecraft:stone\"}",
                "{\"block\":\"minecraft:stone\",\"drag\":0}", "{\"block\":\"minecraft:stone\",\"fractureEnergy\":-1}")) {
            var profile = material(json);
            helper.assertTrue(profile.validationError().isPresent(), "Accepted material " + json);
        }
        helper.assertTrue(ProfileFormat.blockMaterial(JsonParser.parseString(
                "{\"tag\":\"minecraft:logs\",\"drag\":1}")).isPresent(), "Tag priority was optional");
        helper.succeed();
    }

    private static BlockMaterialProfile material(String json) {
        return BlockMaterialProfile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
    }

    private static TestProfileLoader loader(ProfileCatalog<ProjectilePhysicsProfile> catalog) {
        return new TestProfileLoader(catalog);
    }

    private static final class TestProfileLoader extends JsonProfileReloadListener<ProjectilePhysicsProfile> {
        private TestProfileLoader(ProfileCatalog<ProjectilePhysicsProfile> catalog) {
            super("definitions/projectile_physics", "test projectile profiles",
                    ProjectilePhysicsProfile.CODEC, ProjectilePhysicsProfile::validationError, catalog, ProfileFormat::projectile);
        }

        @Override
        public PreparedProfiles<ProjectilePhysicsProfile> prepare(ResourceManager resources,
                net.minecraft.util.profiling.ProfilerFiller profiler) {
            return super.prepare(resources, profiler);
        }

        @Override
        public void apply(PreparedProfiles<ProjectilePhysicsProfile> prepared, ResourceManager resources,
                net.minecraft.util.profiling.ProfilerFiller profiler) {
            super.apply(prepared, resources, profiler);
        }
    }

    private static Resource resource(String packId, String json) {
        PackResources pack = (PackResources) Proxy.newProxyInstance(PackResources.class.getClassLoader(),
                new Class<?>[]{PackResources.class}, (proxy, method, args) -> {
                    if (method.getName().equals("packId")) return packId;
                    throw new UnsupportedOperationException(method.getName());
                });
        return new Resource(pack, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private static ResourceManager resources(Resource... stack) {
        return (ResourceManager) Proxy.newProxyInstance(ResourceManager.class.getClassLoader(),
                new Class<?>[]{ResourceManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("listResourceStacks")) return Map.of(FILE, List.of(stack));
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
