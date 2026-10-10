package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class RecruitsFireZone {
    private static final String TAG_ZONE = "SiegeworksFireZone";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_X = "X";
    private static final String TAG_Z = "Z";
    private static final String TAG_RADIUS = "Radius";
    private static final String TAG_RADIUS_Z = "RadiusZ";
    private static final String TAG_RECTANGULAR = "Rectangular";

    private RecruitsFireZone() {
    }

    static void set(SiegeEngineerEntity engineer, BlockPos center, int radiusX, int radiusZ,
                    boolean rectangular) {
        CompoundTag zone = new CompoundTag();
        zone.putString(TAG_DIMENSION, engineer.level().dimension().location().toString());
        zone.putInt(TAG_X, center.getX());
        zone.putInt(TAG_Z, center.getZ());
        zone.putInt(TAG_RADIUS, Math.max(0, radiusX));
        zone.putInt(TAG_RADIUS_Z, Math.max(0, radiusZ));
        zone.putBoolean(TAG_RECTANGULAR, rectangular);
        engineer.getPersistentData().put(TAG_ZONE, zone);
    }

    public static void clear(SiegeEngineerEntity engineer) {
        engineer.getPersistentData().remove(TAG_ZONE);
    }

    static boolean hasZone(SiegeEngineerEntity engineer) {
        return engineer.getPersistentData().contains(TAG_ZONE, Tag.TAG_COMPOUND);
    }

    static Vec3 resolveTarget(SiegeEngineerEntity engineer, int shotSequence, double shotSpreadRadius,
                              int sampleIndex) {
        if (!(engineer.level() instanceof ServerLevel level)) {
            return null;
        }

        CompoundTag root = engineer.getPersistentData();
        if (!root.contains(TAG_ZONE, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag zone = root.getCompound(TAG_ZONE);
        if (!level.dimension().location().toString().equals(zone.getString(TAG_DIMENSION))) {
            return null;
        }

        int centerX = zone.getInt(TAG_X);
        int centerZ = zone.getInt(TAG_Z);
        double spread = Math.max(0.0D, shotSpreadRadius);
        double availableRadius = Math.max(0.0D, zone.getInt(TAG_RADIUS) - spread);
        double availableRadiusZ = zone.contains(TAG_RADIUS_Z)
                ? Math.max(0.0D, zone.getInt(TAG_RADIUS_Z) - spread)
                : availableRadius;
        long seed = engineer.getUUID().getMostSignificantBits()
                ^ engineer.getUUID().getLeastSignificantBits()
                ^ (0x9E3779B97F4A7C15L * (shotSequence + 1L))
                ^ (0xD1B54A32D192ED03L * (sampleIndex + 1L));
        RandomSource random = RandomSource.create(seed);
        double offsetX;
        double offsetZ;
        if (zone.getBoolean(TAG_RECTANGULAR)) {
            offsetX = (random.nextDouble() * 2.0D - 1.0D) * availableRadius;
            offsetZ = (random.nextDouble() * 2.0D - 1.0D) * availableRadiusZ;
        } else {
            double distance = Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2.0D;
            offsetX = Math.cos(angle) * distance * availableRadius;
            offsetZ = Math.sin(angle) * distance * availableRadiusZ;
        }
        int targetX = centerX + (int) Math.round(offsetX);
        int targetZ = centerZ + (int) Math.round(offsetZ);
        BlockPos sample = new BlockPos(targetX, level.getMinBuildHeight(), targetZ);
        if (!level.hasChunkAt(sample)) {
            targetX = centerX;
            targetZ = centerZ;
            sample = new BlockPos(targetX, level.getMinBuildHeight(), targetZ);
            if (!level.hasChunkAt(sample)) {
                return null;
            }
        }

        int surfaceY = Math.max(level.getMinBuildHeight(),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ) - 1);
        return Vec3.atCenterOf(new BlockPos(targetX, surfaceY, targetZ));
    }
}
