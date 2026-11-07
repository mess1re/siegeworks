package me.mss1r.siegeworks.block;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import me.mss1r.siegeworks.menu.CastingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CastingTableBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            // Legs, braces and aprons.
            Block.box(1, 0, 1, 4, 12, 4),
            Block.box(1, 0, 12, 4, 12, 15),
            Block.box(12, 0, 1, 15, 12, 4),
            Block.box(12, 0, 12, 15, 12, 15),
            Block.box(1, 9, 4, 4, 12, 12),
            Block.box(12, 9, 4, 15, 12, 12),
            Block.box(4, 10, 1, 12, 12, 3),
            Block.box(4, 10, 13, 12, 12, 15),
            // Shelf and ingots.
            Block.box(2, 3, 2, 14, 4, 14),
            Block.box(4.5, 4, 3.5, 10.5, 5.5, 6.5),
            Block.box(4.75, 5.5, 3.75, 10.25, 5.75, 6.25),
            Block.box(5, 4, 7.5, 11, 5.5, 10.5),
            Block.box(5.25, 5.5, 7.75, 10.75, 5.75, 10.25),
            // Slab and mould floor.
            Block.box(0, 12, 0, 16, 13.5, 16),
            Block.box(3.5, 13.5, 4.5, 12.5, 14, 11.5),
            Block.box(4.5, 13.5, 3.5, 11.5, 14, 4.5),
            Block.box(4.5, 13.5, 11.5, 11.5, 14, 12.5),
            // Mould rim, pouring channel and shoulders.
            Block.box(4.5, 14, 3.5, 7.5, 16, 4.5),
            Block.box(8.5, 14, 3.5, 11.5, 16, 4.5),
            Block.box(3.5, 14, 4.5, 7.5, 16, 5.5),
            Block.box(8.5, 14, 4.5, 12.5, 16, 5.5),
            Block.box(3.5, 14, 5.5, 6.5, 16, 6.5),
            Block.box(9.5, 14, 5.5, 12.5, 16, 6.5),
            Block.box(3.5, 14, 6.5, 5.5, 16, 9.5),
            Block.box(10.5, 14, 6.5, 12.5, 16, 9.5),
            Block.box(3.5, 14, 9.5, 6.5, 16, 10.5),
            Block.box(9.5, 14, 9.5, 12.5, 16, 10.5),
            Block.box(3.5, 14, 10.5, 12.5, 16, 11.5),
            Block.box(4.5, 14, 11.5, 11.5, 16, 12.5),
            Block.box(7.5, 14, 3.5, 8.5, 14.5, 5.5),
            Block.box(5.5, 14, 6.5, 6.5, 14.5, 9.5),
            Block.box(9.5, 14, 6.5, 10.5, 14.5, 9.5),
            Block.box(6.5, 14, 9.5, 9.5, 14.5, 10.5),
            Block.box(6.5, 14, 5.5, 9.5, 14.5, 6.5),
            // Hammer.
            Block.box(2, 13.75, 1.75, 10.5, 14.75, 2.75),
            Block.box(1.125, 13.5, 0.5, 2.875, 15.25, 4)
    );
    private static final VoxelShape[] SHAPES_BY_FACING = new VoxelShape[4];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES_BY_FACING[facing.get2DDataValue()] = BlockShapes.rotateFromNorth(NORTH_SHAPE, facing);
        }
    }

    public CastingTableBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    //? if >=1.21 {
    @Override protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() { return simpleCodec(CastingTableBlock::new); }
    //?}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES_BY_FACING[state.getValue(FACING).get2DDataValue()];
    }
    //? if <1.21 {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return open(level, pos, player); }
    *///?} else {
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { return open(level, pos, player); }
    //?}
    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) MenuRegistry.openExtendedMenu(serverPlayer, new ExtendedMenuProvider() {
            @Override public void saveExtraData(FriendlyByteBuf buf) { buf.writeBlockPos(pos); }
            @Override public Component getDisplayName() { return Component.translatable("block.siegeworks.casting_table"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new CastingMenu(id, inventory, pos); }
        });
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
