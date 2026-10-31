package me.mss1r.siegeworks.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public final class SiegeworksServerConfig {
    public static final SiegeworksConfigSpec SPEC;
    private static final Map<String, SiegeworksConfigSpec.IntValue> DEPLOYMENT_LIMITS = new HashMap<>();
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
    private static final SiegeworksConfigSpec.BooleanValue WEATHERING_ENABLED;
    private static final SiegeworksConfigSpec.DoubleValue WEATHERING_DAYS;
    private static final SiegeworksConfigSpec.DoubleValue WEATHERING_RAIN_MULTIPLIER;
    private static final SiegeworksConfigSpec.DoubleValue WEATHERING_HEALTH_LOSS;
    private static final SiegeworksConfigSpec.DoubleValue LADDER_PUSH_SECONDS;
    private static final SiegeworksConfigSpec.DoubleValue LADDER_PUSH_PER_CLIMBER;
    private static final SiegeworksConfigSpec.DoubleValue LADDER_PUSH_PER_45_DEGREES;
    private static final SiegeworksConfigSpec.DoubleValue LADDER_PUSH_MIN_REACH_BONUS;
    private static final SiegeworksConfigSpec.DoubleValue LADDER_PUSH_LIFT_BLOCKS;

    static {
        SiegeworksConfigSpec.Builder builder = new SiegeworksConfigSpec.Builder();
        builder.comment("Gameplay rules. Machine speeds, loading times and crew settings are in siegeworks-engines.toml.",
                        "Damage, ammunition, materials and construction requirements use datapack profiles; see the wiki Data Pack Reference.")
                .push("rules");
        builder.comment("Maximum deployed siege engines owned by one scoreboard team.",
                        "Players without a team are counted individually. Set a value to 0 for no limit.")
                .push("deploymentLimits");
        registerDeploymentLimit(builder, "serpentine");
        registerDeploymentLimit(builder, "culverin");
        registerDeploymentLimit(builder, "mons_meg");
        registerDeploymentLimit(builder, "tower_crossbow");
        registerDeploymentLimit(builder, "arcballista");
        registerDeploymentLimit(builder, "onager");
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
        builder.pop();

        builder.comment("Wood left under the open sky weathers once, losing a share of the engine's health.",
                        "A repair of at least that share makes it fresh again; wax keeps it from weathering at all.")
                .push("weathering");
        WEATHERING_ENABLED = builder
                .comment("Whether engines weather.")
                .define("enabled", true);
        WEATHERING_DAYS = builder
                .comment("In-game days under the open sky before an engine weathers. Time under a roof does not count.")
                .defineInRange("daysUnderOpenSky", 20.0D, 0.05D, 3650.0D);
        WEATHERING_RAIN_MULTIPLIER = builder
                .comment("How much faster an engine weathers while it rains on it.")
                .defineInRange("rainMultiplier", 2.0D, 1.0D, 100.0D);
        WEATHERING_HEALTH_LOSS = builder
                .comment("Share of its maximum health an engine loses when it weathers, never dropping below 1.",
                        "A repair restoring at least this share makes it fresh again.")
                .defineInRange("healthLoss", 0.35D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("Pushing a siege ladder off a wall by bracing a pole against its upper part, holding use.")
                .push("ladderPushing");
        LADDER_PUSH_SECONDS = builder
                .comment("Seconds of bracing that push an empty, upright ladder away.")
                .defineInRange("baseSeconds", 1.5D, 0.0D, 60.0D);
        LADDER_PUSH_PER_CLIMBER = builder
                .comment("Share of that time each climber on the ladder adds: 0.5 makes three climbers take 2.5 times as long.")
                .defineInRange("perClimber", 0.5D, 0.0D, 10.0D);
        LADDER_PUSH_PER_45_DEGREES = builder
                .comment("Share of that time 45 degrees of lean from upright adds: a shallow ladder is heavier to lift off.")
                .defineInRange("per45Degrees", 1.0D, 0.0D, 10.0D);
        LADDER_PUSH_MIN_REACH_BONUS = builder
                .comment("Weapons whose reach is at least this much longer than a bare hand push ladders too, such as",
                        "pikes and halberds of other mods. Items in #siegeworks:ladder_pushers always do. Set to 0 to allow",
                        "only the tag.")
                .defineInRange("minReachBonus", 1.0D, 0.0D, 16.0D);
        LADDER_PUSH_LIFT_BLOCKS = builder
                .comment("Blocks the ladder's top is eased away from the wall while bracing goes on, before the last push",
                        "sends it over. Let go early, and it falls back.")
                .defineInRange("liftBlocks", 1.0D, 0.0D, 8.0D);
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
        return SiegeworksEngineConfig.getMovementSpeed(type, horse);
    }

    public static int getRecoveryTicks(EntityType<?> type) {
        return SiegeworksEngineConfig.getRecoveryTicks(type);
    }

    public static double getMovementAcceleration(EntityType<?> type, boolean horse) {
        return SiegeworksEngineConfig.getMovementAcceleration(type, horse);
    }

    public static double getMovementDeceleration(EntityType<?> type, boolean horse) {
        return SiegeworksEngineConfig.getMovementDeceleration(type, horse);
    }

    public static double getReverseSpeedMultiplier(EntityType<?> type) {
        return SiegeworksEngineConfig.getReverseSpeedMultiplier(type);
    }

    public static double getSteeringSpeedDegrees(EntityType<?> type, boolean horse) {
        return SiegeworksEngineConfig.getSteeringSpeedDegrees(type, horse);
    }

    public static double getTowerCrewSpeedMultiplier(int activeCrew) {
        return SiegeworksEngineConfig.getTowerCrewSpeedMultiplier(activeCrew);
    }

    public static int getLoadingRequirementTicks(EntityType<?> type, String stageKey) {
        return SiegeworksEngineConfig.getLoadingRequirementTicks(type, stageKey);
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

    public static boolean isWeatheringEnabled() {
        return WEATHERING_ENABLED.get();
    }

    public static double getWeatheringDays() {
        return WEATHERING_DAYS.get();
    }

    public static double getWeatheringRainMultiplier() {
        return WEATHERING_RAIN_MULTIPLIER.get();
    }

    public static double getWeatheringHealthLoss() {
        return WEATHERING_HEALTH_LOSS.get();
    }

    public static double getLadderPushSeconds() {
        return LADDER_PUSH_SECONDS.get();
    }

    public static double getLadderPushPerClimber() {
        return LADDER_PUSH_PER_CLIMBER.get();
    }

    public static double getLadderPushPer45Degrees() {
        return LADDER_PUSH_PER_45_DEGREES.get();
    }

    public static double getLadderPushMinReachBonus() {
        return LADDER_PUSH_MIN_REACH_BONUS.get();
    }

    public static double getLadderPushLiftBlocks() {
        return LADDER_PUSH_LIFT_BLOCKS.get();
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

    private static String getEngineId(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
    }
}
