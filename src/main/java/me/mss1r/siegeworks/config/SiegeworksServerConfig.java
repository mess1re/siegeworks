package me.mss1r.siegeworks.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class SiegeworksServerConfig {
    public static final SiegeworksConfigSpec SPEC;
    private static final Map<String, EngineSettings> ENGINES = new HashMap<>();
    private static final Map<String, SiegeworksConfigSpec.IntValue> DEPLOYMENT_LIMITS = new HashMap<>();
    private static final SiegeworksConfigSpec.IntValue TOWER_MINIMUM_CREW;
    private static final SiegeworksConfigSpec.IntValue TOWER_FULL_SPEED_CREW;
    private static final SiegeworksConfigSpec.DoubleValue TOWER_MINIMUM_CREW_SPEED_MULTIPLIER;
    private static final SiegeworksConfigSpec.BooleanValue GEOMETRY_TERRAIN_COLLISION;
    private static final SiegeworksConfigSpec.BooleanValue FLYING_BLOCK_DEBRIS_ENABLED;
    private static final SiegeworksConfigSpec.IntValue MAX_FLYING_BLOCK_DEBRIS_PER_TICK;
    private static final SiegeworksConfigSpec.IntValue RECRUIT_FIRE_ZONE_MAX_RADIUS;
    private static final SiegeworksConfigSpec.BooleanValue OWNERSHIP_ENFORCED;
    private static final SiegeworksConfigSpec.BooleanValue TEAMMATES_MAY_DISMANTLE;
    private static final SiegeworksConfigSpec.BooleanValue CAPTURE_ALLOWED;
    private static final SiegeworksConfigSpec.BooleanValue CAPTURE_REQUIRES_DEFENDER_ONLINE;
    private static final SiegeworksConfigSpec.DoubleValue CAPTURE_SECONDS_PER_DISMANTLE_HIT;
    private static final SiegeworksConfigSpec.IntValue MINIMUM_CAPTURE_SECONDS;
    private static final SiegeworksConfigSpec.IntValue MAXIMUM_CAPTURE_SECONDS;
    private static final SiegeworksConfigSpec.IntValue ABANDON_AFTER_DAYS;
    private static final SiegeworksConfigSpec.EnumValue<SiegeBlockDamage> BLOCK_DAMAGE;
    private static final SiegeworksConfigSpec.DoubleValue STONE_FRACTURE_ENERGY;
    private static final SiegeworksConfigSpec.IntValue INCENDIARY_FUSE_TICKS;

    static {
        SiegeworksConfigSpec.Builder builder = new SiegeworksConfigSpec.Builder();
        builder.comment("Per-engine operation settings. Durations are measured in ticks (20 ticks = 1 second).")
                .push("engines");

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
        register(builder, "mangonel", 0.055D, 0.17D, 10, 4,
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

        builder.pop();

        builder.comment("Gameplay rules that apply across all siege engines.")
                .push("rules");
        builder.comment("Maximum deployed siege engines owned by one scoreboard team.",
                        "Players without a team are counted individually. Set a value to 0 for no limit.")
                .push("deploymentLimits");
        registerDeploymentLimit(builder, "serpentine");
        registerDeploymentLimit(builder, "culverin");
        registerDeploymentLimit(builder, "mons_meg");
        registerDeploymentLimit(builder, "tower_crossbow");
        registerDeploymentLimit(builder, "arcballista");
        registerDeploymentLimit(builder, "mangonel");
        registerDeploymentLimit(builder, "trebuchet");
        registerDeploymentLimit(builder, "battering_ram");
        registerDeploymentLimit(builder, "mantlet");
        registerDeploymentLimit(builder, "siege_tower");
        registerDeploymentLimit(builder, "siege_ladder");
        registerDeploymentLimit(builder, "hwacha");
        builder.pop();

        builder.comment("Who may use, repair, dismantle and capture an owned siege engine.")
                .push("ownership");
        OWNERSHIP_ENFORCED = builder
                .comment("Limit an owned engine to its owner, their scoreboard team and allied factions.",
                        "Disabling this lets anyone use any engine.")
                .define("enforceOwnership", true);
        TEAMMATES_MAY_DISMANTLE = builder
                .comment("Let the owner's team and allies dismantle the engine, not only the owner.")
                .define("teammatesMayDismantle", true);
        CAPTURE_ALLOWED = builder
                .comment("Let an enemy take an engine left without its crew by holding its controls unhurt.",
                        "Disabling this leaves enemies able only to destroy it.")
                .define("allowCapture", true);
        CAPTURE_REQUIRES_DEFENDER_ONLINE = builder
                .comment("Allow capture only while the owner or a member of their team is online.")
                .define("captureRequiresDefenderOnline", true);
        CAPTURE_SECONDS_PER_DISMANTLE_HIT = builder
                .comment("Capture time grows with the engine's size, measured by the hammer strikes needed to dismantle it.")
                .defineInRange("captureSecondsPerDismantleHit", 0.5D, 0.0D, 10.0D);
        MINIMUM_CAPTURE_SECONDS = builder
                .comment("Shortest capture time, in seconds.")
                .defineInRange("minimumCaptureSeconds", 8, 1, 600);
        MAXIMUM_CAPTURE_SECONDS = builder
                .comment("Longest capture time, in seconds.")
                .defineInRange("maximumCaptureSeconds", 45, 1, 600);
        ABANDON_AFTER_DAYS = builder
                .comment("Release an engine to whoever uses it next once neither its owner nor anyone on their team",
                        "has been online for this many days. Set to 0 to keep ownership forever.")
                .defineInRange("abandonAfterDays", 14, 0, 3650);
        builder.pop();

        builder.comment("What siege weapons may do to the world.")
                .push("terrain");
        BLOCK_DAMAGE = builder
                .comment("Where projectiles and rams break blocks.",
                        "EVERYWHERE: anywhere.",
                        "RESPECT_PROTECTION: only where the player behind the engine could break the block by hand,",
                        "as claim mods and spawn protection decide. An offline owner is judged by their own profile.",
                        "NEVER: siege weapons harm creatures and engines but leave blocks whole.")
                .defineEnum("blockDamage", SiegeBlockDamage.EVERYWHERE);
        STONE_FRACTURE_ENERGY = builder
                .comment("Energy in joules to fracture one cubic metre of solid stone.",
                        "50000 is a gameplay value: thick walls should take minutes of repeated artillery fire, not days.",
                        "Higher values make blocks harder to destroy; lower values make them easier.",
                        "Other materials scale from stone. A datapack's fractureEnergy overrides this value for its blocks.",
                        "This changes craters and accumulated cracks, not flight, penetration resistance or entity damage.")
                .defineInRange("stoneFractureEnergy", 50_000.0D, 1.0D, 1.0E9D);
        builder.pop();

        builder.comment("Incendiary pots, which burst only once lit. Their ingredients and burst values are datapack",
                        "profiles: data/<namespace>/definitions/pot_fillings/incendiary.json.")
                .push("incendiary");
        INCENDIARY_FUSE_TICKS = builder
                .comment("How long a lit pot's fuse burns, in ticks, before it bursts wherever it is:",
                        "where it was placed, loaded in a stone thrower, or in flight.")
                .defineInRange("fuseTicks", 200, 20, 1200);
        builder.pop(2);

        builder.push("movement");
        GEOMETRY_TERRAIN_COLLISION = builder
                .comment("Collide a moving engine with the world using its model geometry instead of its bounding box alone.",
                        "An engine's box is smaller than its frame, so parts such as the arcballista's rear beam otherwise pass through terrain.",
                        "Disabling this restores bounding-box movement and its own step height.")
                .define("geometryTerrainCollision", true);
        builder.pop();

        builder.comment("Performance-related settings that do not alter combat calculations.")
                .push("performance");
        builder.comment("Physical block debris created by impacts and explosions.")
                .push("blockDebris");
        FLYING_BLOCK_DEBRIS_ENABLED = builder
                .comment("Create flying block entities when impacts or explosions destroy blocks.",
                        "Disabling this does not change projectile trajectories, penetration, damage, or block destruction.")
                .define("enableFlyingBlockDebris", true);
        MAX_FLYING_BLOCK_DEBRIS_PER_TICK = builder
                .comment("Maximum flying block entities created in one world tick.",
                        "This limit is shared by simultaneous impacts and explosions. Set to 0 to disable debris.")
                .defineInRange("maxFlyingBlockDebrisPerTick", 32, 0, 512);
        builder.pop(2);

        builder.comment("Settings for optional mod integrations.")
                .push("integrations");
        builder.comment("Villager Recruits integration settings.")
                .push("recruits");
        RECRUIT_FIRE_ZONE_MAX_RADIUS = builder
                .comment("Maximum radius, in blocks, of an artillery fire zone selected on the Recruits map.")
                .defineInRange("maxFireZoneRadius", 64, 1, 256);
        builder.pop(2);

        SPEC = builder.build();
    }

    private SiegeworksServerConfig() {
    }

    public static double getMovementSpeed(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.0D;
        }
        MovementSettings movement = settings.movement();
        return horse ? movement.horseSpeed().get() : movement.playerSpeed().get();
    }

    public static int getRecoveryTicks(EntityType<?> type) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        return settings == null ? 0 : settings.operation().recoveryTicks().get();
    }

    public static double getMovementAcceleration(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.005D;
        }
        MovementSettings movement = settings.movement();
        return horse ? movement.horseAcceleration().get() : movement.playerAcceleration().get();
    }

    public static double getMovementDeceleration(EntityType<?> type, boolean horse) {
        EngineSettings settings = ENGINES.get(getEngineId(type));
        if (settings == null) {
            return 0.004D;
        }
        MovementSettings movement = settings.movement();
        return horse ? movement.horseDeceleration().get() : movement.playerDeceleration().get();
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
        return horse ? movement.horseTurnDegreesPerTick().get() : movement.playerTurnDegreesPerTick().get();
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
        SiegeworksConfigSpec.IntValue configured = settings.operation().loadingStages().get(stageKey);
        return configured == null ? 1 : configured.get();
    }

    public static boolean isOwnershipEnforced() {
        return OWNERSHIP_ENFORCED.get();
    }

    public static boolean teammatesMayDismantle() {
        return TEAMMATES_MAY_DISMANTLE.get();
    }

    public static boolean isCaptureAllowed() {
        return CAPTURE_ALLOWED.get();
    }

    public static boolean captureRequiresDefenderOnline() {
        return CAPTURE_REQUIRES_DEFENDER_ONLINE.get();
    }

    public static int getCaptureTicks(int dismantleHits) {
        int minimum = MINIMUM_CAPTURE_SECONDS.get();
        int maximum = Math.max(minimum, MAXIMUM_CAPTURE_SECONDS.get());
        double seconds = Math.max(minimum, Math.min(maximum, dismantleHits * CAPTURE_SECONDS_PER_DISMANTLE_HIT.get()));
        return (int) Math.round(seconds * 20.0D);
    }

    public static int getAbandonAfterDays() {
        return ABANDON_AFTER_DAYS.get();
    }

    public static SiegeBlockDamage getBlockDamage() {
        return BLOCK_DAMAGE.get();
    }

    public static void setBlockDamage(SiegeBlockDamage blockDamage) {
        BLOCK_DAMAGE.set(blockDamage);
    }

    public static int getIncendiaryFuseTicks() {
        return INCENDIARY_FUSE_TICKS.get();
    }

    public static double getStoneFractureEnergy() {
        return STONE_FRACTURE_ENERGY.get();
    }

    public static void setStoneFractureEnergy(double energy) {
        STONE_FRACTURE_ENERGY.set(energy);
    }

    public static boolean isGeometryTerrainCollisionEnabled() {
        return GEOMETRY_TERRAIN_COLLISION.get();
    }

    public static boolean isFlyingBlockDebrisEnabled() {
        return FLYING_BLOCK_DEBRIS_ENABLED.get() && MAX_FLYING_BLOCK_DEBRIS_PER_TICK.get() > 0;
    }

    public static int getMaxFlyingBlockDebrisPerTick() {
        return MAX_FLYING_BLOCK_DEBRIS_PER_TICK.get();
    }

    public static int getRecruitFireZoneMaxRadius() {
        return RECRUIT_FIRE_ZONE_MAX_RADIUS.get();
    }

    public static int getDeploymentLimit(EntityType<?> type) {
        SiegeworksConfigSpec.IntValue limit = DEPLOYMENT_LIMITS.get(getEngineId(type));
        return limit == null ? 0 : limit.get();
    }

    private static void registerDeploymentLimit(SiegeworksConfigSpec.Builder builder, String id) {
        DEPLOYMENT_LIMITS.put(id, builder.defineInRange(id, 0, 0, 100000));
    }

    private static void register(SiegeworksConfigSpec.Builder builder, String id, double playerSpeed,
                                 double horseSpeed, int recoveryTicks, StageDefault... stages) {
        register(builder, id, playerSpeed, horseSpeed, recoveryTicks, 0, stages);
    }

    private static void register(SiegeworksConfigSpec.Builder builder, String id, double playerSpeed,
                                 double horseSpeed, int recoveryTicks, int minimumRecoveryTicks,
                                 StageDefault... stages) {
        builder.push(id);
        builder.comment("Movement and steering settings.")
                .push("movement");
        SiegeworksConfigSpec.DoubleValue playerSpeedValue = builder
                .comment("Movement speed when operated by a player.")
                .defineInRange("playerSpeed", playerSpeed, 0.0D, 10.0D);
        SiegeworksConfigSpec.DoubleValue horseSpeedValue = builder
                .comment("Movement speed when pulled by a horse.")
                .defineInRange("horseSpeed", horseSpeed, 0.0D, 10.0D);
        MovementDefaults movement = movementDefaults(id, playerSpeed, horseSpeed);
        SiegeworksConfigSpec.DoubleValue playerAccelerationValue = builder
                .comment("Speed gained per tick while a player is driving.")
                .defineInRange("playerAcceleration", movement.playerAcceleration(), 0.0D, 10.0D);
        SiegeworksConfigSpec.DoubleValue horseAccelerationValue = builder
                .comment("Speed gained per tick while a horse is pulling.")
                .defineInRange("horseAcceleration", movement.horseAcceleration(), 0.0D, 10.0D);
        SiegeworksConfigSpec.DoubleValue playerDecelerationValue = builder
                .comment("Speed lost per tick after a direct operator releases forward or backward.")
                .defineInRange("playerDeceleration", movement.playerDeceleration(), 0.0D, 10.0D);
        SiegeworksConfigSpec.DoubleValue horseDecelerationValue = builder
                .comment("Speed lost per tick after a draft-mount rider releases forward or backward.")
                .defineInRange("horseDeceleration", movement.horseDeceleration(), 0.0D, 10.0D);
        SiegeworksConfigSpec.DoubleValue reverseSpeedMultiplierValue = builder
                .comment("Maximum reverse speed as a fraction of forward speed.")
                .defineInRange("reverseSpeedMultiplier", 0.55D, 0.0D, 1.0D);
        SiegeworksConfigSpec.DoubleValue playerTurnSpeedValue = builder
                .comment("Degrees turned per tick while driven by a player.")
                .defineInRange("playerTurnDegreesPerTick", movement.playerTurnSpeedDegrees(), 0.0D, 180.0D);
        SiegeworksConfigSpec.DoubleValue horseTurnSpeedValue = builder
                .comment("Degrees turned per tick while pulled by a horse.")
                .defineInRange("horseTurnDegreesPerTick", movement.horseTurnSpeedDegrees(), 0.0D, 180.0D);
        builder.pop();

        builder.comment("Firing, activation and loading timings.")
                .push("operation");
        SiegeworksConfigSpec.IntValue recoveryValue = builder
                .comment("Recovery time after firing or activating. Loading cannot begin during recovery.",
                        minimumRecoveryTicks > 0
                                ? "The minimum preserves the fixed firing or impact event in the animation."
                                : "Set to 0 to disable recovery.")
                .defineInRange("recoveryTicks", recoveryTicks, minimumRecoveryTicks, 72000);

        Map<String, SiegeworksConfigSpec.IntValue> stageValues = new HashMap<>();
        if (stages.length > 0) {
            builder.comment("Loading stage durations. Winding stages also scale their reload animation to match.")
                    .push("loading");
            for (StageDefault stage : stages) {
                stageValues.put(stage.key(), builder.defineInRange(stage.key() + "Ticks", stage.ticks(), 1, 72000));
            }
            builder.pop();
        }

        builder.pop(2);
        MovementSettings movementSettings = new MovementSettings(playerSpeedValue, horseSpeedValue,
                playerAccelerationValue, horseAccelerationValue,
                playerDecelerationValue, horseDecelerationValue,
                reverseSpeedMultiplierValue, playerTurnSpeedValue, horseTurnSpeedValue);
        OperationSettings operationSettings = new OperationSettings(recoveryValue, stageValues);
        ENGINES.put(id, new EngineSettings(movementSettings, operationSettings, movement));
    }

    private static MovementDefaults movementDefaults(String id, double playerSpeed, double horseSpeed) {
        return switch (id) {
            case "mantlet" -> movement(playerSpeed, horseSpeed, 12, 24, 16, 28, 0.85D);
            case "culverin" -> movement(playerSpeed, horseSpeed, 16, 28, 16, 28, 1.0D);
            case "arcballista" -> movement(playerSpeed, horseSpeed, 18, 30, 18, 30, 1.15D);
            case "hwacha" -> movement(playerSpeed, horseSpeed, 20, 32, 20, 34, 0.65D);
            case "serpentine" -> movement(playerSpeed, horseSpeed, 22, 34, 22, 34, 0.62D);
            case "mangonel" -> movement(playerSpeed, horseSpeed, 26, 38, 24, 36, 0.36D);
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
                playerSpeed, horseSpeed,
                perTick(playerSpeed, playerAccelerationTicks),
                perTick(horseSpeed, horseAccelerationTicks),
                perTick(playerSpeed, playerDecelerationTicks),
                perTick(horseSpeed, horseDecelerationTicks),
                playerTurnSpeedDegrees, horseTurnSpeedDegrees);
    }

    private static double perTick(double speed, int ticks) {
        return speed <= 0.0D ? 0.0D : speed / Math.max(1, ticks);
    }

    private static StageDefault stage(String key, int ticks) {
        return new StageDefault(key, ticks);
    }

    private static String getEngineId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
    }

    private record EngineSettings(MovementSettings movement, OperationSettings operation,
                                  MovementDefaults defaults) {
    }

    private record MovementSettings(SiegeworksConfigSpec.DoubleValue playerSpeed,
                                    SiegeworksConfigSpec.DoubleValue horseSpeed,
                                    SiegeworksConfigSpec.DoubleValue playerAcceleration,
                                    SiegeworksConfigSpec.DoubleValue horseAcceleration,
                                    SiegeworksConfigSpec.DoubleValue playerDeceleration,
                                    SiegeworksConfigSpec.DoubleValue horseDeceleration,
                                    SiegeworksConfigSpec.DoubleValue reverseSpeedMultiplier,
                                    SiegeworksConfigSpec.DoubleValue playerTurnDegreesPerTick,
                                    SiegeworksConfigSpec.DoubleValue horseTurnDegreesPerTick) {
    }

    private record OperationSettings(SiegeworksConfigSpec.IntValue recoveryTicks,
                                     Map<String, SiegeworksConfigSpec.IntValue> loadingStages) {
    }

    private record MovementDefaults(double playerSpeed, double horseSpeed,
                                    double playerAcceleration, double horseAcceleration,
                                    double playerDeceleration, double horseDeceleration,
                                    double playerTurnSpeedDegrees,
                                    double horseTurnSpeedDegrees) {
    }

    private record StageDefault(String key, int ticks) {
    }
}
