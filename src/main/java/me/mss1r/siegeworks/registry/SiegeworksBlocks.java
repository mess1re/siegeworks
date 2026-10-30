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

    RegistrySupplier<Block> CANNON_BALL = BLOCKS.register("cannon_ball", () ->
            new StackedProjectileBlock(projectileProperties().strength(5.0F, 6.0F), smallProjectileShapes()));

    RegistrySupplier<Block> GIANT_CANNON_BALL = BLOCKS.register("giant_cannon_ball", () ->
            new StackedProjectileBlock(projectileProperties().strength(12.0F, 18.0F), largeProjectileShapes()));

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

    private static VoxelShape[] smallProjectileShapes() {
        VoxelShape center = Block.box(6.5D, 0.0D, 6.5D, 9.5D, 3.0D, 9.5D);
        VoxelShape left = Block.box(4.0D, 0.0D, 6.5D, 7.0D, 3.0D, 9.5D);
        VoxelShape right = Block.box(9.0D, 0.0D, 6.5D, 12.0D, 3.0D, 9.5D);
        VoxelShape rearLeft = Block.box(3.5D, 0.0D, 3.5D, 6.5D, 3.0D, 6.5D);
        VoxelShape rearRight = Block.box(9.5D, 0.0D, 3.5D, 12.5D, 3.0D, 6.5D);
        VoxelShape front = Block.box(6.5D, 0.0D, 9.5D, 9.5D, 3.0D, 12.5D);
        VoxelShape frontLeft = Block.box(3.5D, 0.0D, 9.5D, 6.5D, 3.0D, 12.5D);
        VoxelShape frontRight = Block.box(9.5D, 0.0D, 9.5D, 12.5D, 3.0D, 12.5D);
        return new VoxelShape[] {
                center,
                Shapes.or(left, right),
                Shapes.or(rearLeft, rearRight, front),
                Shapes.or(rearLeft, rearRight, frontLeft, frontRight)
        };
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
