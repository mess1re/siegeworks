package me.mss1r.siegeworks.block;

import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.item.RoundShotItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Caliber is saved and synchronized by the ordinary block state, including piston moves. */
public final class CaliberShotBlock extends StackedProjectileBlock {
    public static final IntegerProperty CALIBER = IntegerProperty.create("caliber", 1, 98);
    private final VoxelShape[] fullSize;
    private final ConcurrentHashMap<Integer, VoxelShape> shapes = new ConcurrentHashMap<>();

    public CaliberShotBlock(Properties properties, VoxelShape... fullSize) {
        super(properties, fullSize);
        this.fullSize = fullSize;
        registerDefaultState(defaultBlockState().setValue(CALIBER, 24));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int caliber = RoundShotItem.caliber(context.getItemInHand());
        if (!RoundShotSize.valid(caliber)) return null;
        BlockState old = context.getLevel().getBlockState(context.getClickedPos());
        if (old.is(this) && old.getValue(CALIBER) * 5 != caliber) return null;
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(CALIBER, caliber / 5);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context.getItemInHand().is(asItem()) && RoundShotItem.caliber(context.getItemInHand()) != state.getValue(CALIBER) * 5) return false;
        return super.canBeReplaced(state, context);
    }

    @Override
    protected ItemStack projectileStack(BlockState state) {
        return RoundShotItem.withCaliber(new ItemStack(asItem()), state.getValue(CALIBER) * 5);
    }

    @Override
    //? if <1.21 {
    /*public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
    *///?} else {
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
    //?}
        return projectileStack(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> result = super.getDrops(state, params);
        for (ItemStack stack : result) if (stack.is(asItem())) RoundShotItem.withCaliber(stack, state.getValue(CALIBER) * 5);
        return result;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int count = state.getValue(COUNT), caliber = state.getValue(CALIBER) * 5;
        Direction facing = state.getValue(FACING);
        int key = (caliber / 5 * 4 + count - 1) * 4 + facing.get2DDataValue();
        return shapes.computeIfAbsent(key, ignored -> {
            double s = RoundShotSize.scale(caliber, count);
            VoxelShape shape = Shapes.empty();
            for (var b : fullSize[count - 1].toAabbs()) {
                shape = Shapes.or(shape, Shapes.box(.5 + (b.minX - .5) * s, b.minY * s, .5 + (b.minZ - .5) * s,
                        .5 + (b.maxX - .5) * s, b.maxY * s, .5 + (b.maxZ - .5) * s));
            }
            return rotateFromNorth(shape, facing).optimize();
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CALIBER);
    }
}
