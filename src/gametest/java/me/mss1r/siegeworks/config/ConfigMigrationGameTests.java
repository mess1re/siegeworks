package me.mss1r.siegeworks.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@GameTestHolder("siegeworks_config")
@PrefixGameTestTemplate(false)
public final class ConfigMigrationGameTests {
    @GameTest(template = "empty")
    public static void convertsMovementAndAllLoadingStages(GameTestHelper helper) {
        CommentedConfig old = parse("""
                [engines.culverin.movement]
                playerSpeed = 0.08
                horseSpeed = 0.24
                playerAcceleration = 0.004
                horseAcceleration = 0.006
                playerDeceleration = 0.002
                horseDeceleration = 0.003
                playerTurnDegreesPerTick = 1.125
                horseTurnDegreesPerTick = 2.25
                reverseSpeedMultiplier = 0.4
                [engines.culverin.operation]
                recoveryTicks = 91
                [engines.culverin.operation.loading]
                powderTicks = 37
                ramChargeTicks = 53
                ammunitionTicks = 71
                ramProjectileTicks = 89
                primingTicks = 13
                """);
        CommentedConfig converted = CommentedConfig.inMemory();
        EngineConfigMigration.merge(old, converted);
        close(helper, converted, "culverin.movement.playerSpeed", 1.6);
        close(helper, converted, "culverin.movement.horseSpeed", 4.8);
        close(helper, converted, "culverin.movement.playerAccelerationSeconds", 1.0);
        close(helper, converted, "culverin.movement.horseAccelerationSeconds", 2.0);
        close(helper, converted, "culverin.movement.playerDecelerationSeconds", 2.0);
        close(helper, converted, "culverin.movement.horseDecelerationSeconds", 4.0);
        close(helper, converted, "culverin.movement.playerTurnDegreesPerSecond", 22.5);
        close(helper, converted, "culverin.movement.horseTurnDegreesPerSecond", 45.0);
        close(helper, converted, "culverin.movement.reverseSpeedMultiplier", 0.4);
        close(helper, converted, "culverin.operation.recoverySeconds", 4.55);
        close(helper, converted, "culverin.operation.loading.powderSeconds", 1.85);
        close(helper, converted, "culverin.operation.loading.ramChargeSeconds", 2.65);
        close(helper, converted, "culverin.operation.loading.ammunitionSeconds", 3.55);
        close(helper, converted, "culverin.operation.loading.ramProjectileSeconds", 4.45);
        close(helper, converted, "culverin.operation.loading.primingSeconds", 0.65);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void explicitNewValuesAndCurrentNameWin(GameTestHelper helper) {
        CommentedConfig old = parse("""
                [engines.onager.movement]
                playerSpeed = 0.08
                [engines.mangonel.movement]
                playerSpeed = 0.06
                horseSpeed = 0.18
                [engines.siege_tower.crew]
                minimumCrew = 3
                fullSpeedCrew = 5
                minimumCrewSpeedMultiplier = 0.4
                [rules.deploymentLimits]
                mangonel = 7
                onager = 9
                """);
        CommentedConfig converted = parse("""
                [onager.movement]
                horseSpeed = 5.0
                """);
        EngineConfigMigration.merge(old, converted);
        EngineConfigMigration.renameLimit(old);
        close(helper, converted, "onager.movement.playerSpeed", 1.6);
        close(helper, converted, "onager.movement.horseSpeed", 5.0);
        close(helper, converted, "siege_tower.crew.minimumCrew", 3);
        close(helper, converted, "siege_tower.crew.fullSpeedCrew", 5);
        close(helper, converted, "siege_tower.crew.minimumCrewSpeedMultiplier", 0.4);
        close(helper, old, "rules.deploymentLimits.onager", 9);
        helper.assertTrue(!old.contains("rules.deploymentLimits.mangonel"), "Former limit was not removed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialLegacyMovementUsesItsOriginalDefaults(GameTestHelper helper) {
        CommentedConfig converted = CommentedConfig.inMemory();
        EngineConfigMigration.merge(parse("[engines.culverin.movement]\nplayerSpeed = 0.1\nhorseAcceleration = 0.01\n"), converted);
        double playerSeconds = ((Number) converted.getRaw("culverin.movement.playerAccelerationSeconds")).doubleValue();
        helper.assertTrue(Math.abs(EngineConfigUnits.speedStep(2.0, playerSeconds) - 0.065 / 16) < 1.0E-12,
                "Missing old acceleration incorrectly followed the customized speed");
        close(helper, converted, "culverin.movement.horseAccelerationSeconds", 0.23 / 0.01 / 20);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledAndInstantMotionAreDifferent(GameTestHelper helper) {
        helper.assertTrue(EngineConfigUnits.speedStep(1.6, -1) == 0, "Disabled acceleration must stay disabled");
        helper.assertTrue(EngineConfigUnits.speedStep(1.6, 0) >= 1.6 / 20, "Instant acceleration must reach the target");
        helper.assertTrue(EngineConfigUnits.secondsToChangeSpeed(0.08, 0) == -1, "Legacy zero step became instant");
        for (double speed : new double[]{0.018, 0.022, 0.03, 0.045, 0.065, 0.19, 0.24}) {
            for (double step : new double[]{0.0001, 0.002, 0.006, 10}) {
                double seconds = EngineConfigUnits.secondsToChangeSpeed(speed, step);
                helper.assertTrue(Math.abs(EngineConfigUnits.speedStep(speed * 20, seconds) - step) < 1.0E-10,
                        "Migration changed the speed step: " + speed + "/" + step);
            }
        }
        for (int ticks = 0; ticks <= 72000; ticks++) {
            helper.assertTrue(EngineConfigUnits.ticks(ticks / 20.0) == ticks, "Duration lost precision at " + ticks);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fileMigrationBacksUpAndCanBeRepeated(GameTestHelper helper) throws IOException {
        Path directory = Files.createTempDirectory("siegeworks-config-test-");
        Path source = directory.resolve("siegeworks-server.toml");
        String text = """
                [engines.mangonel.movement]
                playerSpeed = 0.06
                [rules.deploymentLimits]
                mangonel = 7
                [rules.weathering]
                enabled = false
                """;
        try {
            Files.writeString(source, text);
            CommentedConfig old = parse(text);
            EngineConfigMigration.migrate(old, source);
            helper.assertTrue(Files.readString(source.resolveSibling(source.getFileName() + ".pre-engines.bak")).equals(text),
                    "Backup did not preserve the original file");
            helper.assertTrue(!old.contains("engines"), "Old engines table remained");
            helper.assertTrue(Boolean.FALSE.equals(old.get("rules.weathering.enabled")), "Unrelated rule changed");
            close(helper, old, "rules.deploymentLimits.onager", 7);
            Path destination = directory.resolve(SiegeworksEngineConfig.FILE_NAME);
            CommentedConfig converted = read(destination);
            close(helper, converted, "onager.movement.playerSpeed", 1.2);
            converted.set("onager.movement.playerSpeed", 3.0);
            new TomlWriter().write(converted, destination, com.electronwill.nightconfig.core.io.WritingMode.REPLACE);
            // Simulate restarting after writing the new file but before saving removal of the old table.
            EngineConfigMigration.migrate(parse(text), source);
            close(helper, read(destination), "onager.movement.playerSpeed", 3.0);
            helper.succeed();
        } finally {
            deleteTemporaryFiles(directory);
        }
    }

    @GameTest(template = "empty")
    public static void invalidDestinationDoesNotDiscardLegacySettings(GameTestHelper helper) throws IOException {
        Path directory = Files.createTempDirectory("siegeworks-config-test-");
        Path source = directory.resolve("siegeworks-server.toml");
        String text = "[engines.culverin.movement]\nplayerSpeed = 0.08\n";
        try {
            Files.writeString(source, text);
            Files.writeString(directory.resolve(SiegeworksEngineConfig.FILE_NAME), "[broken");
            CommentedConfig old = parse(text);
            boolean failed = false;
            try {
                EngineConfigMigration.migrate(old, source);
            } catch (IllegalStateException expected) {
                failed = true;
            }
            helper.assertTrue(failed, "Broken destination was silently overwritten");
            helper.assertTrue(old.contains("engines"), "Legacy values were removed after a failed migration");
            helper.assertTrue(Files.readString(source).equals(text), "Source file changed after a failed migration");
            helper.succeed();
        } finally {
            deleteTemporaryFiles(directory);
        }
    }

    /** Run with scripts/config-migration-tests.init.gradle to test the real loader before world startup. */
    @GameTest(template = "empty", templateNamespace = "siegeworks_config_upgrade")
    public static void loaderMigratesBeforeCorrectionAndLoadsBothFiles(GameTestHelper helper) throws IOException {
        var gun = SiegeworksEntities.CULVERIN_ENTITY.get();
        helper.assertTrue(Math.abs(SiegeworksServerConfig.getMovementSpeed(gun, false) - 0.08) < 1.0E-12, "Legacy direct speed lost");
        helper.assertTrue(Math.abs(SiegeworksServerConfig.getMovementAcceleration(gun, true) - 0.006) < 1.0E-12, "Legacy towing acceleration lost");
        helper.assertTrue(Math.abs(SiegeworksServerConfig.getMovementDeceleration(gun, false) - 0.002) < 1.0E-12, "Legacy braking lost");
        helper.assertTrue(Math.abs(SiegeworksServerConfig.getSteeringSpeedDegrees(gun, false) - 1.125) < 1.0E-12, "Legacy turn rate lost");
        helper.assertTrue(SiegeworksServerConfig.getRecoveryTicks(gun) == 85, "Explicit new recovery did not win");
        helper.assertTrue(SiegeworksServerConfig.getLoadingRequirementTicks(gun, "powder") == 37, "Legacy loading duration lost");
        helper.assertTrue(SiegeworksServerConfig.getDeploymentLimit(SiegeworksEntities.ONAGER_ENTITY.get()) == 7, "Former onager limit lost");
        helper.assertTrue(SiegeworksServerConfig.getTowerCrewSpeedMultiplier(2) == 0, "Crew requirement lost");
        helper.assertTrue(Math.abs(SiegeworksServerConfig.getTowerCrewSpeedMultiplier(3) - 0.4) < 1.0E-12, "Crew multiplier lost");
        helper.assertTrue(SiegeworksServerConfig.getIncendiaryFuseTicks() == 140, "Unrelated server setting changed");
        Path configs = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        helper.assertTrue(!read(configs.resolve("siegeworks-server.toml")).contains("engines"), "Legacy table persisted");
        helper.assertTrue(Files.exists(configs.resolve("siegeworks-server.toml.pre-engines.bak")), "No pre-migration backup");
        close(helper, read(configs.resolve(SiegeworksEngineConfig.FILE_NAME)), "onager.movement.playerSpeed", 1.2);
        helper.succeed();
    }

    private static CommentedConfig parse(String text) {
        return new TomlParser().parse(text);
    }

    private static CommentedConfig read(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return new TomlParser().parse(reader);
        }
    }

    private static void close(GameTestHelper helper, CommentedConfig config, String key, double expected) {
        Object value = config.getRaw(key);
        helper.assertTrue(value instanceof Number number && Math.abs(number.doubleValue() - expected) < 1.0E-10,
                key + ": expected " + expected + ", got " + value);
    }

    private static void deleteTemporaryFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        }
        Files.delete(directory);
    }
}
