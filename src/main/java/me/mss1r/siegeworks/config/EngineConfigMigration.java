package me.mss1r.siegeworks.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.math.BigDecimal;
import java.math.MathContext;
//? if forge {
/*import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
*///?} else {
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
//?}

/** Runs before loader correction, which would otherwise discard the former engines table. */
final class EngineConfigMigration {
    private EngineConfigMigration() {
    }

    static boolean needed(UnmodifiableConfig server) {
        return server.contains("engines") || server.contains("rules.deploymentLimits.mangonel");
    }

    static void migrate(CommentedConfig server, Path source) {
        if (!needed(server)) {
            return;
        }
        try {
            // Save the exact old file before altering either config. A failed migration aborts loading.
            backup(source);
            if (server.contains("engines")) {
                Path destination = source.resolveSibling(SiegeworksEngineConfig.FILE_NAME);
                CommentedConfig engines;
                if (Files.exists(destination)) {
                    backup(destination);
                    try (var reader = Files.newBufferedReader(destination)) {
                        engines = new TomlParser().parse(reader);
                    }
                } else {
                    engines = CommentedConfig.inMemory();
                    // Respect an explicitly supplied new-format template when upgrading a modpack.
                    Path template = defaultEngineFile();
                    if (Files.exists(template)) {
                        try (var reader = Files.newBufferedReader(template)) {
                            engines = new TomlParser().parse(reader);
                        }
                    }
                }
                merge(server, engines);
                writeAtomically(engines, destination);
            }
            renameLimit(server);
            // The new file is safely written before the old settings are removed.
            server.remove("engines");
            LogUtils.getLogger().info("Migrated siege engine settings from {} to {} (old file backed up)",
                    source, SiegeworksEngineConfig.FILE_NAME);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not migrate " + source + "; refusing to discard legacy settings. Check file permissions and .pre-engines.bak backups", exception);
        }
    }

    private static Path defaultEngineFile() {
        //? if forge {
        /*return FMLPaths.GAMEDIR.get().resolve(FMLConfig.getConfigValue(FMLConfig.ConfigValue.DEFAULT_CONFIG_PATH))
                .resolve(SiegeworksEngineConfig.FILE_NAME);
        *///?} else {
        return FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath()).resolve(SiegeworksEngineConfig.FILE_NAME);
        //?}
    }

    private static void writeAtomically(CommentedConfig config, Path destination) throws IOException {
        Path temporary = Files.createTempFile(destination.getParent(), "siegeworks-engines-", ".tmp");
        try {
            new TomlWriter().write(config, temporary, WritingMode.REPLACE);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void backup(Path file) throws IOException {
        Path backup = file.resolveSibling(file.getFileName() + ".pre-engines.bak");
        if (Files.exists(file) && !Files.exists(backup)) {
            Files.copy(file, backup);
        }
    }

    static void merge(UnmodifiableConfig server, CommentedConfig destination) {
        Object value = server.getRaw("engines");
        if (!(value instanceof UnmodifiableConfig oldEngines)) {
            return;
        }
        // The current name wins over its former name when both were configured.
        for (var entry : oldEngines.entrySet()) {
            if (!entry.getKey().equals("mangonel") && entry.getValue() instanceof UnmodifiableConfig engine) {
                mergeEngine(entry.getKey(), engine, destination);
            }
        }
        Object mangonel = oldEngines.getRaw("mangonel");
        if (mangonel instanceof UnmodifiableConfig engine) {
            mergeEngine("onager", engine, destination);
        }
    }

    private static void mergeEngine(String id, UnmodifiableConfig engine, CommentedConfig destination) {
        String movement = id + ".movement.";
        for (String who : List.of("player", "horse")) {
            copyNumber(engine, "movement." + who + "Speed", destination, movement + who + "Speed", v -> v * 20.0D);
            copyNumber(engine, "movement." + who + "TurnDegreesPerTick", destination,
                    movement + who + "TurnDegreesPerSecond", v -> v * 20.0D);
            double speed = oldMotionValue(engine, id, who + "Speed");
            for (String change : List.of("Acceleration", "Deceleration")) {
                String key = movement + who + change + "Seconds";
                if (!destination.contains(key) && (engine.contains("movement." + who + "Speed")
                        || engine.contains("movement." + who + change))) {
                    // Missing old coefficients used their old defaults, not a coefficient scaled to a custom speed.
                    double step = oldMotionValue(engine, id, who + change);
                    putReadable(destination, key, EngineConfigUnits.secondsToChangeSpeed(speed, step));
                }
            }
        }
        copyNumber(engine, "movement.reverseSpeedMultiplier", destination, movement + "reverseSpeedMultiplier", v -> v);
        copyNumber(engine, "operation.recoveryTicks", destination, id + ".operation.recoverySeconds", v -> v / 20.0D);
        Object loading = engine.getRaw("operation.loading");
        if (loading instanceof UnmodifiableConfig stages) {
            for (var entry : stages.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("Ticks")) {
                    copyNumber(stages, key, destination,
                            id + ".operation.loading." + key.substring(0, key.length() - 5) + "Seconds", v -> v / 20.0D);
                }
            }
        }
        Object crew = engine.getRaw("crew");
        if (crew instanceof UnmodifiableConfig settings) {
            for (var entry : settings.entrySet()) {
                String key = id + ".crew." + entry.getKey();
                if (!destination.contains(key)) {
                    destination.set(key, entry.getValue());
                }
            }
        }
    }

    private static double oldMotionValue(UnmodifiableConfig engine, String id, String key) {
        Object value = engine.getRaw("movement." + key);
        return value instanceof Number number && Double.isFinite(number.doubleValue())
                ? number.doubleValue() : SiegeworksEngineConfig.legacyMovementValue(id, key);
    }

    private static void copyNumber(UnmodifiableConfig source, String oldKey, CommentedConfig destination,
                                   String newKey, DoubleUnaryOperator convert) {
        Object old = source.getRaw(oldKey);
        if (!destination.contains(newKey) && old instanceof Number number && Double.isFinite(number.doubleValue())) {
            double converted = convert.applyAsDouble(number.doubleValue());
            putReadable(destination, newKey, converted);
        }
    }

    private static void putReadable(CommentedConfig destination, String key, double value) {
        if (Double.isFinite(value)) {
            // Remove binary floating-point tails without filling the new file with 3.5999999999999996.
            destination.set(key, BigDecimal.valueOf(value).round(new MathContext(12)).doubleValue());
        }
    }

    static void renameLimit(CommentedConfig server) {
        String oldKey = "rules.deploymentLimits.mangonel";
        String newKey = "rules.deploymentLimits.onager";
        if (server.contains(oldKey)) {
            if (!server.contains(newKey)) {
                server.set(newKey, server.getRaw(oldKey));
            }
            server.remove(oldKey);
        }
    }
}
