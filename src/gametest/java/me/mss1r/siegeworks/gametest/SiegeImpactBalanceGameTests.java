package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Regression limits for crater size, material resistance and repeated-shot wall breaches.
 * Writes layer-by-layer crater maps to {@value #MAPS} for inspection.
 */
@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiegeImpactBalanceGameTests {
    private static final ResourceLocation CANNON_BALL =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "cannon_ball");
    private static final ResourceLocation GIANT_CANNON_BALL =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "giant_cannon_ball_projectile");
    private static final ResourceLocation TREBUCHET_STONE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "trebuchet_projectile");
    private static final ResourceLocation MANGONEL_STONE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "mangonel_projectile");
    private static final double CULVERIN_SPEED = 300.0D;
    private static final double MONS_MEG_SPEED = 315.0D;
    private static final double TREBUCHET_SPEED = 43.0D;
    private static final double MANGONEL_SPEED = 30.0D;
    private static final int HALF_WIDTH = 7;
    private static final int THICKNESS = 9;
    private static final String MAPS = "impact-balance-maps.txt";
    /** Typical thickness of player-built stone walls. */
    private static final int BREACHED_WALL = 6;

    private SiegeImpactBalanceGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aSingleShotBreaksTheCraterItWasTunedTo(GameTestHelper helper) {
        SiegeBlockDamage before = SiegeworksServerConfig.getBlockDamage();
        SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
        List<String> maps = new ArrayList<>();
        try {
            Crater culverinStone = wall(helper, Blocks.STONE, CANNON_BALL, CULVERIN_SPEED, maps);
            Crater culverinEarth = wall(helper, Blocks.DIRT, CANNON_BALL, CULVERIN_SPEED, maps);
            Crater culverinBricks = wall(helper, Blocks.DEEPSLATE_BRICKS, CANNON_BALL, CULVERIN_SPEED, maps);
            Crater monsMegStone = wall(helper, Blocks.STONE, GIANT_CANNON_BALL, MONS_MEG_SPEED, maps);
            Crater monsMegTerracotta = wall(helper, Blocks.TERRACOTTA, GIANT_CANNON_BALL, MONS_MEG_SPEED, maps);
            Crater monsMegGround = ground(helper, Blocks.STONE, GIANT_CANNON_BALL, MONS_MEG_SPEED, false, maps);
            Crater monsMegSand = ground(helper, Blocks.SANDSTONE, GIANT_CANNON_BALL, MONS_MEG_SPEED, false, maps);
            Crater monsMegSlope = ground(helper, Blocks.SANDSTONE, GIANT_CANNON_BALL, MONS_MEG_SPEED, true, maps);
            Crater culverinSlope = ground(helper, Blocks.SANDSTONE, CANNON_BALL, CULVERIN_SPEED, true, maps);
            Crater mangonelPlanks = wall(helper, Blocks.OAK_PLANKS, MANGONEL_STONE, MANGONEL_SPEED, maps);
            Siegeworks.LOG.info("Impact balance: culverin stone {}, earth {}, deepslate bricks {}, sandstone slope {};"
                            + " Mons Meg stone {}, terracotta {}, stone ground {}, sandstone ground {}, slope {}",
                    culverinStone, culverinEarth, culverinBricks, culverinSlope, monsMegStone, monsMegTerracotta,
                    monsMegGround, monsMegSand, monsMegSlope);
            // Reload times: culverin ~19 s, trebuchet 26 s, Mons Meg 64 s. Each should breach a typical stone wall in
            // four to ten minutes of fire.
            int culverinBreach = shotsToBreach(helper, CANNON_BALL, CULVERIN_SPEED, BREACHED_WALL);
            int monsMegBreach = shotsToBreach(helper, GIANT_CANNON_BALL, MONS_MEG_SPEED, BREACHED_WALL);
            int trebuchetBreach = shotsToBreach(helper, TREBUCHET_STONE, TREBUCHET_SPEED, BREACHED_WALL);
            Siegeworks.LOG.info("Breach tempo, six-block stone wall: culverin {} shots, Mons Meg {} shots,"
                    + " trebuchet {} stones; a mangonel stone into planks {}", culverinBreach, monsMegBreach,
                    trebuchetBreach, mangonelPlanks);
            helper.assertTrue(culverinBreach >= 12 && culverinBreach <= 18,
                    "A culverin breached a six-block stone wall in " + culverinBreach + " shots, not 12 to 18");
            helper.assertTrue(monsMegBreach >= 5 && monsMegBreach <= 9,
                    "Mons Meg breached a six-block stone wall in " + monsMegBreach + " shots, not 5 to 9");
            helper.assertTrue(trebuchetBreach >= 12 && trebuchetBreach <= 22,
                    "A trebuchet breached a six-block stone wall in " + trebuchetBreach + " stones, not 12 to 22");
            helper.assertTrue(mangonelPlanks.gone() >= 1,
                    "A mangonel stone broke no block of a plank wall: " + mangonelPlanks);
            helper.assertTrue(culverinStone.gone() >= 1 && culverinStone.gone() <= 2,
                    "A culverin shot broke " + culverinStone + " of a stone wall, not 1 or 2 blocks");
            helper.assertTrue(monsMegStone.gone() >= 3 && monsMegStone.gone() <= 9,
                    "A Mons Meg shot broke " + monsMegStone + " of a stone wall, not 3 to 9 blocks");
            helper.assertTrue(monsMegTerracotta.gone() > monsMegStone.gone(),
                    "Mons Meg's stone shot broke no more terracotta than stone: " + monsMegTerracotta);
            helper.assertTrue(monsMegGround.deep() <= 3,
                    "A shot into the ground dug " + monsMegGround + " instead of a crater opening from the surface");
            helper.assertTrue(culverinEarth.gone() >= 2 * culverinStone.gone(),
                    "Earth gave way no more than stone: " + culverinEarth + " against " + culverinStone);
            helper.assertTrue(culverinBricks.gone() <= culverinStone.gone(),
                    "Deepslate bricks gave way more than stone: " + culverinBricks + " against " + culverinStone);
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
            try {
                Files.write(Path.of(MAPS), maps, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException exception) {
                Siegeworks.LOG.error("Could not write the impact balance maps", exception);
            }
        }
        helper.succeed();
    }

    /**
     * Fires at the middle of a stone wall {@code thickness} blocks thick, with up to 1.5 blocks of scatter, until at
     * least six blocks are open all the way through (a breach a player fits through). Returns the shot count, or -1
     * after 300 shots.
     */
    static int shotsToBreach(GameTestHelper helper, ResourceLocation profileId, double speed, int thickness) {
        ServerLevel level = helper.getLevel();
        BlockPos face = helper.absolutePos(new BlockPos(HALF_WIDTH + 2, 1, 4));
        for (BlockPos pos : BlockPos.betweenClosed(face.offset(-HALF_WIDTH - 2, 0, -4),
                face.offset(HALF_WIDTH + 2, 12, thickness + 4))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        StructuralDamageSystem.tick(level);
        for (BlockPos pos : BlockPos.betweenClosed(face.offset(-HALF_WIDTH, 0, 0),
                face.offset(HALF_WIDTH, 8, thickness - 1))) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
        ProjectilePhysicsProfile physics = SiegeProfileCatalogs.PROJECTILES.get(profileId);
        double diameter = physics.diameter().orElse(0.5D);
        java.util.Random scatter = new java.util.Random(7L);
        int shots = -1;
        for (int shot = 1; shot <= 300 && shots < 0; shot++) {
            // Aim at whichever of the six breach blocks has the most wall left behind it, plus scatter.
            int cell = 0;
            int mostWall = -1;
            for (int candidate = 0; candidate < 6; candidate++) {
                int wall = 0;
                for (int z = 0; z < thickness; z++) {
                    if (!level.getBlockState(face.offset(candidate % 2, 3 + candidate / 2, z)).isAir()) {
                        wall++;
                    }
                }
                if (wall > mostWall) {
                    mostWall = wall;
                    cell = candidate;
                }
            }
            double x = face.getX() + 0.5D + cell % 2 + (scatter.nextDouble() - 0.5D) * 0.3D;
            double y = face.getY() + 3.5D + cell / 2 + (scatter.nextDouble() - 0.5D) * 0.3D;
            BlockPos struck = null;
            for (int z = 0; z < thickness && struck == null; z++) {
                BlockPos pos = BlockPos.containing(x, y, face.getZ() + z);
                if (!level.getBlockState(pos).isAir()) {
                    struck = pos;
                }
            }
            if (struck != null) {
                ProjectileImpacts.Drive drive = ProjectileImpacts.drive(level, physics, diameter,
                        new Vec3(x, y, struck.getZ()), struck, new Vec3(0.0D, 0.0D, speed), null);
                if (!drive.passedThrough()) {
                    ProjectileImpacts.stop(level, drive.mouth(), drive.face(), new Vec3(0.0D, 0.0D, 1.0D),
                            physics, drive.speed(), diameter, null);
                }
            }
            int through = 0;
            for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
                for (int dy = 0; dy <= 8; dy++) {
                    boolean open = true;
                    for (int z = 0; z < thickness && open; z++) {
                        open = level.getBlockState(face.offset(dx, dy, z)).isAir();
                    }
                    if (open) {
                        through++;
                    }
                }
            }
            if (through >= 6) {
                shots = shot;
            }
        }
        level.getEntitiesOfClass(FallingBlockEntity.class,
                        new AABB(Vec3.atLowerCornerOf(face), Vec3.atLowerCornerOf(face)).inflate(20.0D))
                .forEach(FallingBlockEntity::discard);
        return shots;
    }

    /** Blocks a shot removed: total, depth in layers from the struck face, and on the face itself. */
    private record Crater(int gone, int deep, int across) {
        @Override
        public String toString() {
            return gone + " gone, " + deep + " deep, " + across + " across";
        }
    }

    /** A wall of {@code block} hit square in the middle of its face, mapped from the face inward. */
    private static Crater wall(GameTestHelper helper, Block block, ResourceLocation profileId, double speed,
                               List<String> maps) {
        BlockPos face = helper.absolutePos(new BlockPos(HALF_WIDTH + 2, 1, 4));
        return strike(helper, block, profileId, face.offset(-HALF_WIDTH, 0, 0),
                face.offset(HALF_WIDTH, 2 * HALF_WIDTH, THICKNESS - 1), pos -> true,
                Vec3.atCenterOf(face.offset(0, HALF_WIDTH, 0)).add(0.0D, 0.0D, -0.5D), face.offset(0, HALF_WIDTH, 0),
                new Vec3(0.0D, 0.0D, speed), false, "wall", maps);
    }

    /**
     * Flat ground of {@code block}, or a slope rising away from the gun in two-block steps, hit in the middle by a shot
     * descending at 45 degrees; mapped top down.
     */
    private static Crater ground(GameTestHelper helper, Block block, ResourceLocation profileId, double speed,
                                 boolean slope, List<String> maps) {
        BlockPos top = helper.absolutePos(new BlockPos(HALF_WIDTH + 2, THICKNESS + 6, HALF_WIDTH + 2));
        BlockPos min = top.offset(-HALF_WIDTH, -THICKNESS - 4, -HALF_WIDTH);
        Predicate<BlockPos> solid = slope
                ? pos -> pos.getY() <= top.getY() + Math.floorDiv(pos.getZ() - top.getZ(), 2)
                : pos -> pos.getY() <= top.getY();
        BlockPos struck = top;
        Vec3 entry = Vec3.atCenterOf(struck).add(0.0D, 0.5D, 0.0D);
        return strike(helper, block, profileId, min, top.offset(HALF_WIDTH, 4, HALF_WIDTH), solid, entry, struck,
                new Vec3(0.0D, -1.0D, 1.0D).normalize().scale(speed), true,
                slope ? "slope" : "ground", maps);
    }

    private static Crater strike(GameTestHelper helper, Block block, ResourceLocation profileId, BlockPos min,
                                 BlockPos max, Predicate<BlockPos> solid, Vec3 entry,
                                 BlockPos struck, Vec3 velocity, boolean fromAbove, String scene, List<String> maps) {
        ServerLevel level = helper.getLevel();
        for (BlockPos pos : BlockPos.betweenClosed(min.offset(-2, -2, -2), max.offset(2, 8, 2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        // Clear crack data for the removed blocks before they are rebuilt.
        StructuralDamageSystem.tick(level);
        Set<BlockPos> laid = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (solid.test(pos)) {
                level.setBlock(pos, block.defaultBlockState(), 2);
                laid.add(pos.immutable());
            }
        }

        ProjectilePhysicsProfile physics = SiegeProfileCatalogs.PROJECTILES.get(profileId);
        double diameter = physics.diameter().orElse(0.5D);
        // Same as a real hit: penetrate as far as possible, then spend the remaining energy where it stopped.
        ProjectileImpacts.Drive drive = ProjectileImpacts.drive(level, physics, diameter, entry, struck, velocity,
                null);
        if (!drive.passedThrough()) {
            ProjectileImpacts.stop(level, drive.mouth(), drive.face(), velocity.normalize(),
                    physics, drive.speed(), diameter, null);
        }

        int gone = 0;
        int deepest = 0;
        int across = 0;
        for (BlockPos pos : laid) {
            if (!level.getBlockState(pos).is(block)) {
                gone++;
                int depth = fromAbove ? struck.getY() - pos.getY() + 1 : pos.getZ() - struck.getZ() + 1;
                deepest = Math.max(deepest, depth);
                if (depth == 1) {
                    across++;
                }
            }
        }
        maps.add(profileId.getPath() + " " + BuiltInRegistries.BLOCK.getKey(block).getPath() + " " + scene + ": " + gone
                + " gone, " + deepest + " deep");
        maps.addAll(draw(level, block, laid, struck, fromAbove));
        maps.add("");
        level.getEntitiesOfClass(FallingBlockEntity.class,
                        new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max).add(1.0D, 1.0D, 1.0D)).inflate(12.0D))
                .forEach(FallingBlockEntity::discard);
        return new Crater(gone, deepest, across);
    }

    /**
     * Crater map around {@code struck}, layer by layer from the face inward: # intact, 1-9 cracked, . gone, ? other
     * block, blank where nothing was placed.
     */
    private static List<String> draw(ServerLevel level, Block block, Set<BlockPos> laid, BlockPos struck,
                                     boolean fromAbove) {
        List<String> lines = new ArrayList<>();
        int span = 6;
        for (int row = span; row >= -span; row--) {
            StringBuilder line = new StringBuilder();
            for (int layer = 0; layer < 6; layer++) {
                for (int column = -span; column <= span; column++) {
                    BlockPos pos = fromAbove
                            ? new BlockPos(struck.getX() + column, struck.getY() - layer, struck.getZ() - row)
                            : new BlockPos(struck.getX() + column, struck.getY() + row, struck.getZ() + layer);
                    line.append(symbol(level, block, laid, pos));
                }
                line.append("  ");
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static char symbol(ServerLevel level, Block block, Set<BlockPos> laid, BlockPos pos) {
        if (!laid.contains(pos)) {
            return ' ';
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(block)) {
            return state.isAir() ? '.' : '?';
        }
        int crack = Math.min(9, (int) Math.ceil(StructuralDamageSystem.progress(level, pos, state) * 10.0F));
        return crack == 0 ? '#' : (char) ('0' + crack);
    }

}
