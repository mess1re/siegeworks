package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.block.StackedProjectileBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SiegeworksBlocks {
    DeferredRegister<Block> BLOCKS = DeferredRegister.create(Siegeworks.MOD_ID, Registries.BLOCK);
    RegistrySupplier<Block> CASTING_TABLE = BLOCKS.register("casting_table", () ->
            new me.mss1r.siegeworks.block.CastingTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(2.5F).sound(SoundType.WOOD).noOcclusion()));

    RegistrySupplier<Block> CANNON_BALL = BLOCKS.register("cannon_ball", () ->
            new me.mss1r.siegeworks.block.CaliberShotBlock(projectileProperties().strength(5.0F, 6.0F), largeProjectileShapes()));

    RegistrySupplier<Block> STONE_CANNON_BALL = BLOCKS.register("stone_cannon_ball", () ->
            new StackedProjectileBlock(projectileProperties().sound(SoundType.STONE).strength(2.0F, 6.0F),
                    scaledProjectileShapes(120)));

    RegistrySupplier<Block> GIANT_CANNON_BALL = BLOCKS.register("giant_cannon_ball", () ->
            new StackedProjectileBlock(projectileProperties().sound(SoundType.STONE).strength(12.0F, 18.0F), largeProjectileShapes()));

    RegistrySupplier<Block> GRAPESHOT = BLOCKS.register("grapeshot", () ->
            new StackedProjectileBlock(projectileProperties().strength(3.0F, 4.0F), largeProjectileShapes()));

    RegistrySupplier<Block> FIRE_PROJECTILE = BLOCKS.register("fire_projectile", () ->
            me.mss1r.siegeworks.platform.SiegePlatform.incendiaryPot(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(0.0F, 0.0F)
                    .sound(SoundType.DECORATED_POT)
                    .pushReaction(PushReaction.DESTROY)
                    .noOcclusion()));

    private static BlockBehaviour.Properties projectileProperties() {
        return BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static VoxelShape[] scaledProjectileShapes(int caliber) {
        VoxelShape[] shapes = largeProjectileShapes();
        for (int i = 0; i < shapes.length; i++) {
            double scale = me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize.scale(caliber, i + 1);
            VoxelShape scaled = Shapes.empty();
            for (var box : shapes[i].toAabbs()) {
                scaled = Shapes.or(scaled, Shapes.box(
                        0.5D + (box.minX - 0.5D) * scale, box.minY * scale,
                        0.5D + (box.minZ - 0.5D) * scale,
                        0.5D + (box.maxX - 0.5D) * scale, box.maxY * scale,
                        0.5D + (box.maxZ - 0.5D) * scale));
            }
            shapes[i] = scaled.optimize();
        }
        return shapes;
    }

    private static VoxelShape[] largeProjectileShapes() {
        VoxelShape center = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D);
        VoxelShape left = Block.box(0.0D, 0.0D, 4.0D, 8.0D, 8.0D, 12.0D);
        VoxelShape right = Block.box(8.0D, 0.0D, 4.0D, 16.0D, 8.0D, 12.0D);
        VoxelShape rearLeft = Block.box(0.0D, 0.0D, 0.0D, 8.0D, 8.0D, 8.0D);
        VoxelShape rearRight = Block.box(8.0D, 0.0D, 0.0D, 16.0D, 8.0D, 8.0D);
        VoxelShape front = Block.box(4.0D, 0.0D, 8.0D, 12.0D, 8.0D, 16.0D);
        VoxelShape top = Block.box(4.0D, 8.0D, 4.0D, 12.0D, 16.0D, 12.0D);
        return new VoxelShape[] {
                center,
                Shapes.or(left, right),
                Shapes.or(rearLeft, rearRight, front),
                Shapes.or(rearLeft, rearRight, front, top)
        };
    }

    static void registerBlocks() {
        BLOCKS.register();
        Siegeworks.LOG.info("Registering Siegeworks blocks");
    }
}
