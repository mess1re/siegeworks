package me.mss1r.siegeworks.block;

//? if neoforge {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public final class StackedProjectileBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final IntegerProperty COUNT = IntegerProperty.create("count", 1, 4);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final VoxelShape[][] shapesByFacing = new VoxelShape[4][];

    public StackedProjectileBlock(Properties properties, VoxelShape... shapes) {
        super(properties);
        if (shapes.length != 4) {
            throw new IllegalArgumentException("Stacked projectile blocks require one shape for every count from 1 to 4");
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            VoxelShape[] facingShapes = new VoxelShape[shapes.length];
            for (int index = 0; index < shapes.length; index++) {
                facingShapes[index] = rotateFromNorth(shapes[index], direction);
            }
            shapesByFacing[direction.get2DDataValue()] = facingShapes;
        }
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COUNT, 1)
                .setValue(WATERLOGGED, false));
    }

    //? if neoforge {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return MapCodec.unit(this);
    }
    //?}

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(COUNT, Math.min(4, existing.getValue(COUNT) + 1));
        }

        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.is(Fluids.WATER));
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.isSecondaryUseActive()
                && context.getItemInHand().is(asItem())
                && state.getValue(COUNT) < 4) {
            return true;
        }
        return super.canBeReplaced(state, context);
    }

    //? if forge {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return player.getItemInHand(hand).isEmpty()
                ? takeProjectile(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        return player.getMainHandItem().isEmpty()
                ? takeProjectile(state, level, pos, player) : InteractionResult.PASS;
    }
    //?}

    private InteractionResult takeProjectile(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack projectile = new ItemStack(asItem());
        if (!player.getInventory().add(projectile)) return InteractionResult.FAIL;
        int count = state.getValue(COUNT);
        BlockState remaining = count > 1 ? state.setValue(COUNT, count - 1)
                : state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        level.setBlock(pos, remaining, Block.UPDATE_ALL);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(true) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return shapesByFacing[facing.get2DDataValue()][state.getValue(COUNT) - 1];
    }

    private static VoxelShape rotateFromNorth(VoxelShape shape, Direction facing) {
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COUNT, WATERLOGGED);
    }
}
