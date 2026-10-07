package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.debug.SiegeworksDebug;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Fires every artillery engine at the same walls and records what the shots did. Not part of the regular
 * suite: run with -PgameTestNamespaces=siegeworks_ballistics. Rows go to ballistics-bench.csv in the
 * game-test directory, one per engine, wall material and distance.
 */
@GameTestHolder(SiegeBallisticsBench.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeBallisticsBench {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_ballistics";
    /** Stands for a lane holding a tree, an oak trunk under a crown of leaves, instead of a wall. */
    private static final Block TREE = Blocks.OAK_LOG;
    private static final Block[] MATERIALS = {
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.DIRT, Blocks.DEEPSLATE_BRICKS, Blocks.OAK_LEAVES,
            Blocks.TERRACOTTA, Blocks.SPRUCE_LOG, TREE
    };
    private static final int TREE_HEIGHT = 7;
    private static final int CROWN_FROM = 4;
    private static final int CROWN_RADIUS = 2;
    private static final String WALL_MAPS = "ballistics-bench-walls.txt";
    private static final int[] DISTANCES = {20, 40, 60, 120};
    private static final int LANE_SPACING = 24;
    private static final int LANE_HALF_WIDTH = 10;
    private static final int WALL_HALF_WIDTH = 4;
    private static final int WALL_HEIGHT = 8;
    private static final int WALL_DEPTH = 8;
    private static final int ENGINE_Z = 3;
    private static final int CLEAR_HEIGHT = 32;
    private static final int FLOOR_Y = 180;
    private static final int OPEN_SKY_CHECK_HEIGHT = 140;
    private static final int WALL_TICK = 2;
    private static final int SETTLE_TICKS = 5;
    private static final int MIN_WATCH_TICKS = 60;
    private static final int QUIET_TICKS = 20;
    private static final int MAX_WATCH_TICKS = 900;
    private static final int TRACK_BEYOND_WALL = 100;
    private static final double TRACK_HEIGHT = 320.0D;
    private static final int TRACK_BEHIND_ENGINE = 24;
    private static final String CSV = "ballistics-bench.csv";
    private static final String CSV_HEADER = "minecraft,engine,material,distance,reachable,requestedPitch,"
            + "pitch,shots,wallHits,misses,meanMissZ,meanHitX,meanHitY,brokenBlocks,openBlocks,crackedBlocks,damage,deepestLayer,throughColumns";

    private SiegeBallisticsBench() {
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_arcballista")
    public static void arcballista(GameTestHelper helper) {
        run(helper, "arcballista", SiegeworksEntities.ARCBALLISTA_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_tower_crossbow")
    public static void towerCrossbow(GameTestHelper helper) {
        run(helper, "tower_crossbow", SiegeworksEntities.TOWER_CROSSBOW_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_culverin")
    public static void culverin(GameTestHelper helper) {
        run(helper, "culverin", SiegeworksEntities.CULVERIN_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_serpentine")
    public static void serpentine(GameTestHelper helper) {
        run(helper, "serpentine", SiegeworksEntities.SERPENTINE_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_mons_meg")
    public static void monsMeg(GameTestHelper helper) {
        run(helper, "mons_meg", SiegeworksEntities.MONS_MEG_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_mangonel")
    public static void mangonel(GameTestHelper helper) {
        run(helper, "mangonel", SiegeworksEntities.MANGONEL_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_trebuchet")
    public static void trebuchet(GameTestHelper helper) {
        run(helper, "trebuchet", SiegeworksEntities.TREBUCHET_ENTITY::get);
    }

    @GameTest(template = "empty", timeoutTicks = 1100, batch = "ballistics_hwacha_explosive")
    public static void hwachaExplosive(GameTestHelper helper) {
        run(helper, "hwacha_explosive", SiegeworksEntities.HWACHA_ENTITY::get);
    }

    private static void run(GameTestHelper helper, String engineName,
                            Supplier<? extends EntityType<? extends AbstractSiegeEntity>> type) {
        ServerLevel level = helper.getLevel();
        List<Lane> lanes = new ArrayList<>();
        int index = 0;
        for (int distance : DISTANCES) {
            for (Block material : MATERIALS) {
                lanes.add(new Lane(helper, index++, material, distance));
            }
        }
        Set<Long> forcedChunks = forceChunks(level, lanes);
        lanes.forEach(lane -> lane.clearLeftovers(level));
        lanes.forEach(lane -> lane.clear(level));
        for (Lane lane : lanes) {
            BlockPos blocked = lane.findBlockedSky(level);
            helper.assertTrue(blocked == null, "The ballistics bench needs open sky, but " + blocked + " is solid");
        }

        Player operator = new BenchOperator(level);
        boolean instantFireBefore = SiegeworksDebug.instantFire();
        SiegeworksDebug.setInstantFire(true);
        for (Lane lane : lanes) {
            AbstractSiegeEntity engine = type.get().create(level);
            helper.assertTrue(engine != null, "Failed to create " + engineName);
            lane.place(level, engine);
        }

        int[] tick = {0};
        helper.onEachTick(() -> {
            int now = ++tick[0];
            // A tick after clearing, once the cleared cracks have been forgotten.
            if (now == WALL_TICK) {
                lanes.forEach(lane -> lane.buildWall(level));
                return;
            }
            if (now == SETTLE_TICKS) {
                lanes.forEach(lane -> lane.fire(engineName, operator));
                SiegeworksDebug.setInstantFire(instantFireBefore);
                return;
            }
            if (now < SETTLE_TICKS) {
                return;
            }
            int watched = now - SETTLE_TICKS;
            boolean quiet = true;
            for (Lane lane : lanes) {
                quiet &= lane.watch(level, watched);
            }
            if (watched >= MAX_WATCH_TICKS || (watched >= MIN_WATCH_TICKS && quiet)) {
                writeRows(level, engineName, lanes);
                forcedChunks.forEach(chunk -> level.setChunkForced(
                        (int) (chunk >> 32), (int) (long) chunk, false));
                lanes.forEach(Lane::discard);
                helper.succeed();
            }
        });
    }

    private static Set<Long> forceChunks(ServerLevel level, List<Lane> lanes) {
        Set<Long> chunks = new HashSet<>();
        for (Lane lane : lanes) {
            BlockPos from = lane.corner(-LANE_HALF_WIDTH, -TRACK_BEHIND_ENGINE);
            BlockPos to = lane.corner(LANE_HALF_WIDTH, lane.wallZ + TRACK_BEYOND_WALL);
            for (int x = Math.min(from.getX(), to.getX()) >> 4; x <= Math.max(from.getX(), to.getX()) >> 4; x++) {
                for (int z = Math.min(from.getZ(), to.getZ()) >> 4; z <= Math.max(from.getZ(), to.getZ()) >> 4; z++) {
                    if (chunks.add(((long) x << 32) | (z & 0xFFFFFFFFL))) {
                        level.setChunkForced(x, z, true);
                    }
                }
            }
        }
        return chunks;
    }

    private static void writeRows(ServerLevel level, String engineName, List<Lane> lanes) {
        String minecraft = SharedConstants.getCurrentVersion().getName();
        List<String> rows = new ArrayList<>();
        for (Lane lane : lanes) {
            String row = lane.row(level, minecraft, engineName);
            rows.add(row);
            Siegeworks.LOG.info("Ballistics bench: {}", row);
        }
        Path csv = Path.of(CSV);
        try {
            if (!Files.exists(csv)) {
                Files.write(csv, List.of(CSV_HEADER));
            }
            Files.write(csv, rows, StandardOpenOption.APPEND);
            List<String> maps = new ArrayList<>();
            for (Lane lane : lanes) {
                maps.addAll(lane.wallMap(level, engineName));
            }
            Files.write(Path.of(WALL_MAPS), maps, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            Siegeworks.LOG.error("Could not write the ballistics bench results", exception);
        }
    }

    private static final class Lane {
        private final BlockPos origin;
        private final Block material;
        private final int distance;
        private final int wallZ;
        /** Every block the lane was built with, and what it was. */
        private final Map<BlockPos, Block> wall = new HashMap<>();
        private final Set<BlockPos> broken = new HashSet<>();
        private final Map<UUID, Vec3[]> inFlight = new HashMap<>();
        private final List<Vec3> impacts = new ArrayList<>();
        private AbstractSiegeEntity engine;
        private Vec3 target;
        private boolean reachable = true;
        private float requestedPitch;
        private float pitch;
        private int shots;
        private int lastActivity;

        private Lane(GameTestHelper helper, int index, Block material, int distance) {
            this.origin = helper.absolutePos(new BlockPos(index * LANE_SPACING + LANE_HALF_WIDTH, 0, 0))
                    .atY(FLOOR_Y);
            this.material = material;
            this.distance = distance;
            this.wallZ = ENGINE_Z + distance;
        }

        /** A block above the lane that would stop a high shot, or null when the sky is open. */
        private BlockPos findBlockedSky(ServerLevel level) {
            for (int x = -LANE_HALF_WIDTH; x <= LANE_HALF_WIDTH; x += LANE_HALF_WIDTH) {
                for (int z = -TRACK_BEHIND_ENGINE; z <= wallZ + TRACK_BEYOND_WALL; z += 4) {
                    for (int y = CLEAR_HEIGHT + 1; y <= OPEN_SKY_CHECK_HEIGHT; y += 2) {
                        BlockPos pos = origin.offset(x, y, z);
                        if (!level.getBlockState(pos).isAir()) {
                            return pos;
                        }
                    }
                }
            }
            return null;
        }

        private BlockPos corner(int x, int z) {
            return origin.offset(x, 0, z);
        }

        /** Removes shots and debris an earlier run left in the air, which would land on this run's walls. */
        private void clearLeftovers(ServerLevel level) {
            level.getEntitiesOfClass(Entity.class, trackedArea(),
                            entity -> entity instanceof SiegeProjectile || entity instanceof FallingBlockEntity)
                    .forEach(Entity::discard);
        }

        private AABB trackedArea() {
            return new AABB(
                    origin.getX() - LANE_HALF_WIDTH - 4.0D, origin.getY() - 4.0D,
                    origin.getZ() - TRACK_BEHIND_ENGINE,
                    origin.getX() + LANE_HALF_WIDTH + 5.0D, origin.getY() + TRACK_HEIGHT,
                    origin.getZ() + wallZ + TRACK_BEYOND_WALL);
        }

        /** Lays the floor and clears the lane, walls included, so no crack from an earlier run survives. */
        private void clear(ServerLevel level) {
            for (int x = -LANE_HALF_WIDTH; x <= LANE_HALF_WIDTH; x++) {
                for (int z = -TRACK_BEHIND_ENGINE; z <= wallZ + TRACK_BEYOND_WALL; z++) {
                    level.setBlock(origin.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                        level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        private void buildWall(ServerLevel level) {
            if (material == TREE) {
                buildTree(level);
                return;
            }
            for (int x = -WALL_HALF_WIDTH; x <= WALL_HALF_WIDTH; x++) {
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    for (int z = 0; z < WALL_DEPTH; z++) {
                        BlockPos pos = origin.offset(x, y, wallZ + z);
                        place(level, pos, material);
                    }
                }
            }
            target = new Vec3(origin.getX() + 0.5D, origin.getY() + 1.0D + WALL_HEIGHT * 0.5D, origin.getZ() + wallZ);
        }

        /** A trunk standing where a wall face would, under a crown of leaves, the shot aimed through the crown. */
        private void buildTree(ServerLevel level) {
            for (int y = CROWN_FROM; y <= TREE_HEIGHT + 1; y++) {
                int radius = y > TREE_HEIGHT - 1 ? CROWN_RADIUS - 1 : CROWN_RADIUS;
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) == radius && Math.abs(z) == radius) {
                            continue;
                        }
                        place(level, origin.offset(x, y, wallZ + CROWN_RADIUS + z), Blocks.OAK_LEAVES);
                    }
                }
            }
            for (int y = 1; y <= TREE_HEIGHT; y++) {
                place(level, origin.offset(0, y, wallZ + CROWN_RADIUS), TREE);
            }
            target = new Vec3(origin.getX() + 0.5D, origin.getY() + CROWN_FROM + 1.5D, origin.getZ() + wallZ);
        }

        /** Sets a block of the lane, leaves kept from decaying for the length of the run. */
        private void place(ServerLevel level, BlockPos pos, Block block) {
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(LeavesBlock.PERSISTENT)) {
                state = state.setValue(LeavesBlock.PERSISTENT, true);
            }
            level.setBlock(pos, state, 2);
            wall.put(pos, block);
        }

        private String materialName() {
            return material == TREE ? "tree" : BuiltInRegistries.BLOCK.getKey(material).getPath();
        }

        /** Each layer of the wall from its face back, as rows from the top: # whole, 1-9 cracked, . gone. */
        private List<String> wallMap(ServerLevel level, String engineName) {
            List<String> lines = new ArrayList<>();
            lines.add(engineName + " " + materialName() + " " + distance + "m");
            if (material == TREE) {
                return lines;
            }
            for (int y = WALL_HEIGHT; y >= 1; y--) {
                StringBuilder line = new StringBuilder();
                for (int z = 0; z < WALL_DEPTH; z++) {
                    for (int x = -WALL_HALF_WIDTH; x <= WALL_HALF_WIDTH; x++) {
                        line.append(mapSymbol(level, origin.offset(x, y, wallZ + z)));
                    }
                    line.append("  ");
                }
                lines.add(line.toString());
            }
            return lines;
        }

        private char mapSymbol(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(wall.get(pos))) {
                return '.';
            }
            int crack = Math.min(9, (int) Math.ceil(StructuralDamageSystem.progress(level, pos, state) * 10.0F));
            return crack == 0 ? '#' : (char) ('0' + crack);
        }

        private void place(ServerLevel level, AbstractSiegeEntity engine) {
            this.engine = engine;
            engine.moveTo(origin.getX() + 0.5D, origin.getY() + 1.0D, origin.getZ() + ENGINE_Z + 0.5D, 0.0F, 0.0F);
            engine.setYBodyRot(0.0F);
            engine.setYHeadRot(0.0F);
            engine.setTrackedYaw(0.0F);
            if (engine instanceof HwachaEntity hwacha) {
                CompoundTag rack = new CompoundTag();
                rack.putInt("LoadedSingijeonCount", HwachaEntity.CAPACITY);
                rack.putLong("ExplosiveSingijeonLow", -1L);
                rack.putLong("ExplosiveSingijeonHigh", -1L);
                rack.putInt("FiringSingijeonIndex", -1);
                hwacha.readAdditionalSaveData(rack);
            }
            level.addFreshEntity(engine);
        }

        private void fire(String engineName, Player operator) {
            if (engine instanceof SiegeArtilleryControl artillery) {
                artillery.prepareAutomatedShot(target);
                requestedPitch = artillery.calculateAutomatedAimPitch(target);
                reachable = artillery.canReachAutomatedTarget(target);
                // Held as the aim target too, the way an operator keeps it, so a volley stays on it.
                engine.setTrackedPitch(requestedPitch);
                engine.turnTowardsPitch(requestedPitch);
                engine.turnTowardsYaw(0.0F);
            }
            pitch = engine.getTrackedPitch();
            if (!engine.requestDebugInstantFire(operator)) {
                Siegeworks.LOG.warn("Ballistics bench: {} refused to fire at {} blocks", engineName, distance);
            }
        }

        private boolean watch(ServerLevel level, int watched) {
            for (BlockPos pos : wall.keySet()) {
                if (!broken.contains(pos) && level.getBlockState(pos).isAir()) {
                    broken.add(pos);
                    lastActivity = watched;
                }
            }
            AABB area = trackedArea();
            // Debris still in the air may yet land back in a crater.
            if (!level.getEntitiesOfClass(FallingBlockEntity.class, area).isEmpty()) {
                lastActivity = watched;
            }
            Set<UUID> seen = new HashSet<>();
            for (SiegeProjectile projectile : level.getEntitiesOfClass(SiegeProjectile.class, area)) {
                UUID id = projectile.getUUID();
                boolean resting = projectile.getDeltaMovement().lengthSqr() < 1.0E-4D;
                if (!inFlight.containsKey(id) && !resting) {
                    shots++;
                }
                if (resting) {
                    Vec3[] last = inFlight.remove(id);
                    if (last != null) {
                        impacts.add(projectile.position());
                    }
                    continue;
                }
                seen.add(id);
                inFlight.put(id, new Vec3[]{projectile.position(), projectile.getDeltaMovement()});
                lastActivity = watched;
            }
            inFlight.entrySet().removeIf(entry -> {
                if (seen.contains(entry.getKey())) {
                    return false;
                }
                impacts.add(endOfLastStep(entry.getValue()[0], entry.getValue()[1]));
                return true;
            });
            return inFlight.isEmpty() && watched - lastActivity >= QUIET_TICKS;
        }

        /** Where the step a projectile vanished on met the wall face, or the step's end if it never got there. */
        private Vec3 endOfLastStep(Vec3 position, Vec3 motion) {
            Vec3 end = position.add(motion);
            if (position.z < target.z && end.z >= target.z && motion.z > 1.0E-6D) {
                return position.add(motion.scale((target.z - position.z) / motion.z));
            }
            return end;
        }

        /**
         * One CSV row. {@code brokenBlocks} counts every wall block that was ever knocked out, {@code openBlocks}
         * the ones still gone once debris has settled, which is the hole a player sees.
         */
        private String row(ServerLevel level, String minecraft, String engineName) {
            int wallHits = 0;
            int missed = 0;
            double sumX = 0.0D;
            double sumY = 0.0D;
            double sumMissZ = 0.0D;
            for (Vec3 impact : impacts) {
                double x = impact.x - target.x;
                double y = impact.y - target.y;
                boolean atWall = impact.z >= target.z - 1.0D && impact.z <= target.z + WALL_DEPTH + 1.0D
                        && Math.abs(x) <= WALL_HALF_WIDTH + 1.0D
                        && impact.y >= origin.getY() && impact.y <= origin.getY() + WALL_HEIGHT + 2.0D;
                if (atWall) {
                    wallHits++;
                    sumX += x;
                    sumY += y;
                } else {
                    missed++;
                    sumMissZ += impact.z - target.z;
                }
            }
            int deepest = 0;
            Map<Long, Integer> columns = new HashMap<>();
            for (BlockPos pos : broken) {
                int layer = pos.getZ() - (origin.getZ() + wallZ);
                deepest = Math.max(deepest, layer + 1);
                columns.merge(((long) pos.getX() << 32) | (pos.getY() & 0xFFFFFFFFL), 1, Integer::sum);
            }
            long through = columns.values().stream().filter(count -> count >= WALL_DEPTH).count();
            long open = wall.keySet().stream().filter(pos -> !level.getBlockState(pos).is(wall.get(pos))).count();
            // Damage in whole blocks: those gone, and the share of breaking the cracked ones have taken.
            double cracks = 0.0D;
            int cracked = 0;
            for (BlockPos pos : wall.keySet()) {
                float progress = level.getBlockState(pos).is(wall.get(pos))
                        ? StructuralDamageSystem.progress(level, pos, level.getBlockState(pos))
                        : 0.0F;
                if (progress > 0.0F) {
                    cracked++;
                    cracks += progress;
                }
            }
            return String.format(Locale.ROOT, "%s,%s,%s,%d,%s,%.2f,%.2f,%d,%d,%d,%.1f,%.2f,%.2f,%d,%d,%d,%.2f,%d,%d",
                    minecraft, engineName, materialName(), distance,
                    reachable, requestedPitch, pitch, shots, wallHits, missed,
                    missed == 0 ? 0.0D : sumMissZ / missed,
                    wallHits == 0 ? 0.0D : sumX / wallHits, wallHits == 0 ? 0.0D : sumY / wallHits,
                    broken.size(), open, cracked, open + cracks, deepest, through);
        }

        private void discard() {
            if (engine != null) {
                engine.discard();
            }
        }
    }

    private static final class BenchOperator extends Player {
        private BenchOperator(ServerLevel level) {
            super(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "SiegeBench"));
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return true;
        }

        @Override
        public boolean hasPermissions(int level) {
            return true;
        }
    }
}
