package me.mss1r.siegeworks.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class SiegeworksEngineConfig {
    public static final String FILE_NAME = "siegeworks-engines.toml";
    public static final SiegeworksConfigSpec SPEC;
    private static final Map<String, EngineSettings> ENGINES = new HashMap<>();
    private static final SiegeworksConfigSpec.IntValue TOWER_MINIMUM_CREW;
    private static final SiegeworksConfigSpec.IntValue TOWER_FULL_SPEED_CREW;
    private static final SiegeworksConfigSpec.DoubleValue TOWER_MINIMUM_CREW_SPEED_MULTIPLIER;

    static {
        SiegeworksConfigSpec.Builder builder = new SiegeworksConfigSpec.Builder();
        register(builder, "serpentine", 0.045D, 0.19D, 105, 60,
                stage("powder", 115), stage("ramCharge", 82), stage("ammunition", 80),
                stage("ramProjectile", 70), stage("priming", 30));
        register(builder, "culverin", 0.065D, 0.23D, 75, 60,
                stage("powder", 90), stage("ramCharge", 65), stage("ammunition", 65),
                stage("ramProjectile", 55), stage("priming", 25));
        register(builder, "mons_meg", 0.018D, 0.11D, 600,
                stage("powder", 190), stage("ramCharge", 145), stage("ammunition", 160),
                stage("ramProjectile", 130), stage("priming", 50));
        register(builder, "tower_crossbow", 0.0D, 0.0D, 95,
                stage("bolt", 115), stage("winding", 100));
        register(builder, "arcballista", 0.075D, 0.21D, 48,
                stage("bolt", 72), stage("winding", 60));
        register(builder, "onager", 0.055D, 0.17D, 10, 4,
                stage("stone", 95), stage("fireProjectile", 105), stage("winding", 170));
        register(builder, "trebuchet", 0.0D, 0.0D, 60, 16,
                stage("stone", 150), stage("fireProjectile", 165), stage("winding", 320));
        register(builder, "battering_ram", 0.03D, 0.09D, 80, 80);
        register(builder, "mantlet", 0.085D, 0.21D, 0);
        register(builder, "siege_tower", 0.022D, 0.12D, 0);
        register(builder, "siege_ladder", 0.0D, 0.0D, 0);
        register(builder, "hwacha", 0.055D, 0.19D, 100,
                stage("rocket", 10), stage("priming", 24));

        builder.push("siege_tower").push("crew");
        TOWER_MINIMUM_CREW = builder
                .comment("Minimum number of active crew members required to move the tower. The driver counts as crew.")
                .defineInRange("minimumCrew", 2, 1, 6);
        TOWER_FULL_SPEED_CREW = builder
                .comment("Active crew required to reach the configured player speed. The tower has six pushing positions, including the driver.")
                .defineInRange("fullSpeedCrew", 6, 1, 6);
        TOWER_MINIMUM_CREW_SPEED_MULTIPLIER = builder
                .comment("Fraction of configured player speed available at the minimum crew size. Speed scales linearly to full speed.")
                .defineInRange("minimumCrewSpeedMultiplier", 0.30D, 0.0D, 1.0D);
        builder.pop(2);
        SPEC = builder.build();
    }

    private SiegeworksEngineConfig() {
    }

    public static double getMovementSpeed(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.0D;
        }
        MovementSettings movement = settings.movement();
        return (horse ? movement.horseSpeed().get() : movement.playerSpeed().get()) / 20.0D;
    }

    public static int getRecoveryTicks(EntityType<?> type) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        return settings == null ? 0 : EngineConfigUnits.ticks(settings.operation().recoverySeconds().get());
    }

    public static double getMovementAcceleration(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.005D;
        }
        MovementSettings movement = settings.movement();
        return EngineConfigUnits.speedStep(
                horse ? movement.horseSpeed().get() : movement.playerSpeed().get(),
                horse ? movement.horseAccelerationSeconds().get() : movement.playerAccelerationSeconds().get());
    }

    public static double getMovementDeceleration(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.004D;
        }
        MovementSettings movement = settings.movement();
        return EngineConfigUnits.speedStep(
                horse ? movement.horseSpeed().get() : movement.playerSpeed().get(),
                horse ? movement.horseDecelerationSeconds().get() : movement.playerDecelerationSeconds().get());
    }

    public static double getReverseSpeedMultiplier(EntityType<?> type) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        return settings == null ? 0.55D : settings.movement().reverseSpeedMultiplier().get();
    }

    public static double getSteeringSpeedDegrees(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return horse ? 0.65D : 1.0D;
        }
        MovementSettings movement = settings.movement();
        return (horse ? movement.horseTurnDegreesPerSecond().get() : movement.playerTurnDegreesPerSecond().get()) / 20.0D;
    }

    public static double getTowerCrewSpeedMultiplier(int activeCrew) {
        int minimumCrew = TOWER_MINIMUM_CREW.get();
        int fullSpeedCrew = Math.max(minimumCrew, TOWER_FULL_SPEED_CREW.get());
        if (activeCrew < minimumCrew) {
            return 0.0D;
        }
        if (activeCrew >= fullSpeedCrew || fullSpeedCrew == minimumCrew) {
            return 1.0D;
        }

        double progress = (double) (activeCrew - minimumCrew) / (fullSpeedCrew - minimumCrew);
        double minimumMultiplier = TOWER_MINIMUM_CREW_SPEED_MULTIPLIER.get();
        return minimumMultiplier + (1.0D - minimumMultiplier) * progress;
    }

    public static int getLoadingRequirementTicks(EntityType<?> type, String stageKey) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 1;
        }
        SiegeworksConfigSpec.DoubleValue configured = settings.operation().loadingStages().get(stageKey);
        return configured == null ? 1 : EngineConfigUnits.ticks(configured.get());
    }

    private static void register(SiegeworksConfigSpec.Builder builder, String id, double playerSpeed,
                                 double horseSpeed, int recoveryTicks, StageDefault... stages) {
        register(builder, id, playerSpeed, horseSpeed, recoveryTicks, 0, stages);
    }

    private static void register(SiegeworksConfigSpec.Builder builder, String id, double playerSpeed,
                                 double horseSpeed, int recoveryTicks, int minimumRecoveryTicks,
                                 StageDefault... stages) {
        builder.comment("Machine settings. Speeds: blocks/second; turns: degrees/second; durations: seconds.")
                .push(id);
        builder.comment("Movement and steering settings.")
                .push("movement");
        SiegeworksConfigSpec.DoubleValue playerSpeedValue = builder
                .comment("Maximum speed under direct control, in blocks per second. Example: 1.3 = 1.3 blocks/second.")
                .defineInRange("playerSpeed", readable(playerSpeed * 20.0D), 0.0D, 200.0D);
        SiegeworksConfigSpec.DoubleValue horseSpeedValue = builder
                .comment("Maximum speed while towed, in blocks per second. Draft-team bonuses still apply.")
                .defineInRange("horseSpeed", readable(horseSpeed * 20.0D), 0.0D, 200.0D);
        MovementDefaults movement = movementDefaults(id, playerSpeed, horseSpeed);
        SiegeworksConfigSpec.DoubleValue playerAccelerationValue = builder
                .comment("Seconds from rest to maximum direct-control speed. 0 = instant; -1 = no acceleration.")
                .defineInRange("playerAccelerationSeconds", movement.playerAccelerationSeconds(), -1.0D, Double.MAX_VALUE / 20.0D);
        SiegeworksConfigSpec.DoubleValue horseAccelerationValue = builder
                .comment("Seconds from rest to maximum towing speed. 0 = instant; -1 = no acceleration.")
                .defineInRange("horseAccelerationSeconds", movement.horseAccelerationSeconds(), -1.0D, Double.MAX_VALUE / 20.0D);
        SiegeworksConfigSpec.DoubleValue playerDecelerationValue = builder
                .comment("Seconds to stop from maximum direct-control speed after releasing movement. 0 = instant; -1 = no braking.")
                .defineInRange("playerDecelerationSeconds", movement.playerDecelerationSeconds(), -1.0D, Double.MAX_VALUE / 20.0D);
        SiegeworksConfigSpec.DoubleValue horseDecelerationValue = builder
                .comment("Seconds to stop from maximum towing speed after releasing movement. 0 = instant; -1 = no braking.")
                .defineInRange("horseDecelerationSeconds", movement.horseDecelerationSeconds(), -1.0D, Double.MAX_VALUE / 20.0D);
        SiegeworksConfigSpec.DoubleValue reverseSpeedMultiplierValue = builder
                .comment("Maximum reverse speed as a fraction of forward speed.")
                .defineInRange("reverseSpeedMultiplier", 0.55D, 0.0D, 1.0D);
        SiegeworksConfigSpec.DoubleValue playerTurnSpeedValue = builder
                .comment("Degrees turned per second while driven by a player.")
                .defineInRange("playerTurnDegreesPerSecond", movement.playerTurnDegreesPerSecond(), 0.0D, 3600.0D);
        SiegeworksConfigSpec.DoubleValue horseTurnSpeedValue = builder
                .comment("Degrees turned per second while pulled by a horse.")
                .defineInRange("horseTurnDegreesPerSecond", movement.horseTurnDegreesPerSecond(), 0.0D, 3600.0D);
        builder.pop();

        builder.comment("Firing, activation and loading timings.")
                .push("operation");
        SiegeworksConfigSpec.DoubleValue recoveryValue = builder
                .comment("Recovery after firing or activating, in seconds. Loading cannot begin during recovery.",
                        minimumRecoveryTicks > 0
                                ? "The minimum preserves the fixed firing or impact event in the animation."
                                : "Set to 0 to disable recovery.")
                .defineInRange("recoverySeconds", recoveryTicks / 20.0D, minimumRecoveryTicks / 20.0D, 3600.0D);

        Map<String, SiegeworksConfigSpec.DoubleValue> stageValues = new HashMap<>();
        if (stages.length > 0) {
            builder.comment("Loading stage durations in seconds. Winding also adjusts the reload animation.")
                    .push("loading");
            for (StageDefault stage : stages) {
                stageValues.put(stage.key(), builder.comment(stageDescription(stage.key()))
                        .defineInRange(stage.key() + "Seconds", stage.ticks() / 20.0D, 0.05D, 3600.0D));
            }
            builder.pop();
        }

        builder.pop(2);
        MovementSettings movementSettings = new MovementSettings(playerSpeedValue, horseSpeedValue,
                playerAccelerationValue, horseAccelerationValue,
                playerDecelerationValue, horseDecelerationValue,
                reverseSpeedMultiplierValue, playerTurnSpeedValue, horseTurnSpeedValue);
        OperationSettings operationSettings = new OperationSettings(recoveryValue, stageValues);
        ENGINES.put(id, new EngineSettings(movementSettings, operationSettings, movement, playerSpeed, horseSpeed));
    }

    private static MovementDefaults movementDefaults(String id, double playerSpeed, double horseSpeed) {
        return switch (id) {
            case "mantlet" -> movement(playerSpeed, horseSpeed, 12, 24, 16, 28, 0.85D);
            case "culverin" -> movement(playerSpeed, horseSpeed, 16, 28, 16, 28, 1.0D);
            case "arcballista" -> movement(playerSpeed, horseSpeed, 18, 30, 18, 30, 1.15D);
            case "hwacha" -> movement(playerSpeed, horseSpeed, 20, 32, 20, 34, 0.65D);
            case "serpentine" -> movement(playerSpeed, horseSpeed, 22, 34, 22, 34, 0.62D);
            case "onager" -> movement(playerSpeed, horseSpeed, 26, 38, 24, 36, 0.36D);
            case "battering_ram" -> movement(playerSpeed, horseSpeed, 36, 50, 36, 46, 0.16D);
            case "mons_meg" -> movement(playerSpeed, horseSpeed, 50, 64, 44, 56, 0.14D);
            case "siege_tower" -> movement(playerSpeed, horseSpeed, 60, 80, 50, 70, 0.10D);
            case "tower_crossbow", "trebuchet", "siege_ladder" ->
                    movement(playerSpeed, horseSpeed, 1, 1, 1, 1, 0.0D);
            default -> movement(playerSpeed, horseSpeed, 20, 32, 20, 32, 0.25D);
        };
    }

    private static final double TOWED_TURN_CIRCLE_FACTOR = 2.0D;

    private static MovementDefaults movement(double playerSpeed, double horseSpeed,
                                             int playerAccelerationTicks, int horseAccelerationTicks,
                                             int playerDecelerationTicks, int horseDecelerationTicks,
                                             double playerTurnSpeedDegrees) {
        double horseTurnSpeedDegrees = playerSpeed <= 0.0D
                ? 0.0D
                : playerTurnSpeedDegrees * (horseSpeed / playerSpeed) / TOWED_TURN_CIRCLE_FACTOR;
        return new MovementDefaults(
                playerAccelerationTicks / 20.0D, horseAccelerationTicks / 20.0D,
                playerDecelerationTicks / 20.0D, horseDecelerationTicks / 20.0D,
                readable(playerTurnSpeedDegrees * 20.0D), readable(horseTurnSpeedDegrees * 20.0D));
    }

    private static double readable(double value) {
        return Math.round(value * 1_000_000_000.0D) / 1_000_000_000.0D;
    }

    /** Former defaults are needed when an old hand-written config omitted speed or acceleration fields. */
    static double legacyMovementValue(String id, String key) {
        EngineSettings settings = ENGINES.get(id);
        if (settings == null) {
            return 0.0D;
        }
        boolean horse = key.startsWith("horse");
        double speed = horse ? settings.legacyHorseSpeed() : settings.legacyPlayerSpeed();
        if (key.endsWith("Speed")) {
            return speed;
        }
        MovementDefaults defaults = settings.defaults();
        double seconds = key.endsWith("Acceleration")
                ? (horse ? defaults.horseAccelerationSeconds() : defaults.playerAccelerationSeconds())
                : (horse ? defaults.horseDecelerationSeconds() : defaults.playerDecelerationSeconds());
        return speed / (seconds * 20.0D);
    }

    private static String stageDescription(String key) {
        return switch (key) {
            case "powder" -> "Adding the powder charge, in seconds.";
            case "ramCharge" -> "Ramming the powder charge, in seconds.";
            case "ammunition" -> "Inserting ammunition, in seconds.";
            case "ramProjectile" -> "Ramming the projectile, in seconds.";
            case "priming" -> "Priming the weapon for firing, in seconds.";
            case "bolt" -> "Placing a bolt, in seconds.";
            case "winding" -> "Winding to full tension, in seconds.";
            case "stone" -> "Loading stones, in seconds.";
            case "fireProjectile" -> "Loading an incendiary projectile, in seconds.";
            case "rocket" -> "Inserting one rocket, in seconds; repeated for each rocket.";
            default -> "Loading duration, in seconds.";
        };
    }

    private static StageDefault stage(String key, int ticks) {
        return new StageDefault(key, ticks);
    }

    private static String getEngineId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
    }

    private record EngineSettings(MovementSettings movement, OperationSettings operation, MovementDefaults defaults,
                                  double legacyPlayerSpeed, double legacyHorseSpeed) {
    }

    private record MovementSettings(SiegeworksConfigSpec.DoubleValue playerSpeed,
                                    SiegeworksConfigSpec.DoubleValue horseSpeed,
                                    SiegeworksConfigSpec.DoubleValue playerAccelerationSeconds,
                                    SiegeworksConfigSpec.DoubleValue horseAccelerationSeconds,
                                    SiegeworksConfigSpec.DoubleValue playerDecelerationSeconds,
                                    SiegeworksConfigSpec.DoubleValue horseDecelerationSeconds,
                                    SiegeworksConfigSpec.DoubleValue reverseSpeedMultiplier,
                                    SiegeworksConfigSpec.DoubleValue playerTurnDegreesPerSecond,
                                    SiegeworksConfigSpec.DoubleValue horseTurnDegreesPerSecond) {
    }

    private record OperationSettings(SiegeworksConfigSpec.DoubleValue recoverySeconds,
                                     Map<String, SiegeworksConfigSpec.DoubleValue> loadingStages) {
    }

    private record MovementDefaults(double playerAccelerationSeconds, double horseAccelerationSeconds,
                                    double playerDecelerationSeconds, double horseDecelerationSeconds,
                                    double playerTurnDegreesPerSecond, double horseTurnDegreesPerSecond) {
    }

    private record StageDefault(String key, int ticks) {
    }
}
