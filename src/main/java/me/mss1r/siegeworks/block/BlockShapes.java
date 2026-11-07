package me.mss1r.siegeworks.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

final class BlockShapes {
    private BlockShapes() {}

    static VoxelShape rotateFromNorth(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;

        VoxelShape rotated = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            AABB transformed = switch (facing) {
                case EAST -> new AABB(1.0D - box.maxZ, box.minY, box.minX,
                        1.0D - box.minZ, box.maxY, box.maxX);
                case SOUTH -> new AABB(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                        1.0D - box.minX, box.maxY, 1.0D - box.minZ);
                case WEST -> new AABB(box.minZ, box.minY, 1.0D - box.maxX,
                        box.maxZ, box.maxY, 1.0D - box.minX);
                default -> box;
            };
            rotated = Shapes.or(rotated, Shapes.create(transformed));
        }
        return rotated.optimize();
    }
}
