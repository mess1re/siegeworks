package me.mss1r.siegeworks.block;

//? if neoforge {
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.LevelReader;
//?} else {
/*import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.enchantment.Enchantments;
*///?}
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.gameplay.ballistics.IncendiaryFuse;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Placed incendiary pot. Breaks like a vanilla decorated pot: instantly, whole by hand, into shards by a tool or
 * projectile, and crushed by pistons. Filled and sealed by hand (see {@link PotFilling}). Once it has a base it
 * detonates like TNT when lit, set on fire, hit by a burning projectile or caught in an explosion.
 */
public class IncendiaryPotBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, EntityBlock {
    //? if neoforge {
    public static final MapCodec<IncendiaryPotBlock> CODEC = simpleCodec(IncendiaryPotBlock::new);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty WICK = BooleanProperty.create("wick");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final Vec3 WICK_TIP = new Vec3(0.5D, 17.0D / 16.0D, 0.53D);
    public static final Vec3 WICK_BASE = new Vec3(0.5D, 14.0D / 16.0D, 0.53D);
    /** Bricks dropped when a pot shatters. */
    private static final int BRICKS = 5;
    /** Same values as TNT. */
    private static final int FLAMMABILITY = 15;
    private static final int FIRE_SPREAD_SPEED = 100;
    /** Max random delay for pots set off by a neighbour, so a stack of them goes off in quick succession. */
    private static final int CHAIN_DELAY = 3;
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0), Block.box(5.0, 11.0, 5.0, 11.0, 14.0, 11.0));

    public IncendiaryPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(WICK, false)
                .setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncendiaryPotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (!state.getValue(LIT) || type != SiegeworksBlockEntities.INCENDIARY_POT.get()) {
            return null;
        }
        BlockEntityTicker<IncendiaryPotBlockEntity> ticker = level.isClientSide
                ? IncendiaryPotBlockEntity::clientTick
                : IncendiaryPotBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Nullable
    private static IncendiaryPotBlockEntity pot(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IncendiaryPotBlockEntity pot ? pot : null;
    }

    private static PotFilling fillingAt(BlockGetter level, BlockPos pos) {
        IncendiaryPotBlockEntity pot = pot(level, pos);
        return pot == null ? PotFilling.EMPTY : pot.filling();
    }

    //? if forge {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    //?}

    /** Fills, seals or lights the pot. Returns false if the held item doesn't apply. */
    private boolean interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        IncendiaryPotBlockEntity pot = pot(level, pos);
        ItemStack held = player.getItemInHand(hand);
        if (pot == null || state.getValue(LIT)) {
            return false;
        }
        PotFilling filling = pot.filling();
        if (IncendiaryFuse.canStrike(held)) {
            // Without a wick, flint and steel behaves normally.
            if (!filling.canLight() || state.getValue(WATERLOGGED)) {
                return false;
            }
            if (!level.isClientSide) {
                IncendiaryFuse.strike(player, hand, level, Vec3.atLowerCornerOf(pos).add(WICK_TIP));
                pot.light(level.getGameTime(), IncendiaryFuse.fullLength(), player.getUUID());
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            }
            return true;
        }
        if (PotFilling.isWick(held)) {
            if (!level.isClientSide) {
                filling.withWick().ifPresentOrElse(sealed -> {
                    pot.setFilling(sealed);
                    level.setBlock(pos, state.setValue(WICK, true), Block.UPDATE_ALL);
                    consume(player, held);
                    level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
                }, () -> refuse(player, filling));
            }
            return true;
        }
        Item item = held.getItem();
        if (PotFilling.isIngredient(item)) {
            if (!level.isClientSide) {
                filling.with(item).ifPresentOrElse(fuller -> {
                    pot.setFilling(fuller);
                    consume(player, held);
                    level.playSound(null, pos, insertSound(), SoundSource.BLOCKS, 1.0F,
                            0.8F + 0.05F * fuller.contents().size());
                }, () -> refuse(player, filling));
            }
            return true;
        }
        // The click is left to the item, so blocks can still be placed against a pot.
        if (hand == InteractionHand.MAIN_HAND && !level.isClientSide) {
            hint(player, filling);
        }
        return false;
    }

    /** What the pot needs next, shown for a click with an empty hand or an item that does not go in. */
    private static void hint(Player player, PotFilling filling) {
        PotFillingProfile profile = PotFillingProfile.current();
        Component wick = PotFillingProfile.name(profile.wick());
        Component message = !filling.hasBase() ? Component.translatable("siege.pot.needs_base",
                        PotFilling.names(filling.missingBase()))
                : filling.canLight() ? Component.translatable("siege.pot.hint.light")
                : filling.additives().size() >= profile.additiveSlots() ? Component.translatable("siege.pot.hint.seal", wick)
                : Component.translatable("siege.pot.hint.add_or_seal", wick);
        player.displayClientMessage(message, true);
    }

    private static void consume(Player player, ItemStack held) {
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
    }

    private static void refuse(Player player, PotFilling filling) {
        PotFillingProfile profile = PotFillingProfile.current();
        Component message = filling.wick() ? Component.translatable("siege.pot.sealed")
                : !filling.hasBase() ? Component.translatable("siege.pot.needs_base",
                        PotFilling.names(filling.missingBase()))
                : filling.additives().size() >= profile.additiveSlots() ? Component.translatable("siege.pot.full")
                : Component.translatable("siege.pot.kind_full", profile.maxOfAKind());
        player.displayClientMessage(message, true);
    }

    private static SoundEvent insertSound() {
        //? if forge {
        /*return SoundEvents.DECORATED_POT_HIT;
        *///?} else {
        return SoundEvents.DECORATED_POT_INSERT;
        //?}
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        IncendiaryPotBlockEntity pot = pot(level, pos);
        if (pot != null) {
            pot.setFilling(PotFilling.of(stack));
        }
    }

    //? if forge {
    /*@Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return fillingAt(level, pos).toItem();
    }
    *///?} else {
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return fillingAt(level, pos).toItem();
    }
    //?}

    /**
     * Drops like a decorated pot: whole with its contents, or bricks plus contents when broken by a tool or explosion.
     * Lit pots, and filled pots caught in an explosion, drop nothing because they detonate.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof IncendiaryPotBlockEntity pot)) {
            return super.getDrops(state, params);
        }
        boolean blast = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS) != null;
        if (pot.isLit() || pot.isDetonating() || blast && pot.filling().hasBase()) {
            return List.of();
        }
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean shattered = blast || tool != null && tool.is(ItemTags.BREAKS_DECORATED_POTS)
                && !hasSilkTouch(params.getLevel(), tool);
        return shattered ? shards(pot.filling()) : List.of(pot.filling().toItem());
    }

    private static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        //? if forge {
        /*return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
        *///?} else {
        return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), tool) > 0;
        //?}
    }

    private static List<ItemStack> shards(PotFilling filling) {
        List<ItemStack> shards = new ArrayList<>();
        shards.add(new ItemStack(Items.BRICK, BRICKS));
        shards.addAll(filling.asItems());
        return shards;
    }

    /** Projectiles shatter a pot like a decorated pot; a burning projectile detonates a filled one. */
    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        BlockPos pos = hit.getBlockPos();
        if (!(level instanceof ServerLevel serverLevel) || !projectile.mayInteract(level, pos)) {
            return;
        }
        if (projectile.isOnFire() && fillingAt(level, pos).hasBase()) {
            detonate(serverLevel, pos, 0, responsibleFor(projectile.getOwner()));
            return;
        }
        shatter(serverLevel, pos);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel serverLevel && fillingAt(level, pos).hasBase()) {
            detonate(serverLevel, pos, 1 + level.random.nextInt(CHAIN_DELAY),
                    responsibleFor(explosion.getIndirectSourceEntity()));
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return fillingAt(level, pos).hasBase() ? FLAMMABILITY : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return fillingAt(level, pos).hasBase() ? FIRE_SPREAD_SPEED : 0;
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction,
                             @Nullable LivingEntity igniter) {
        if (level instanceof ServerLevel serverLevel) {
            detonate(serverLevel, pos, 1 + level.random.nextInt(CHAIN_DELAY), responsibleFor(igniter));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && state.getValue(LIT) && level instanceof ServerLevel serverLevel) {
            IncendiaryPotBlockEntity pot = pot(level, pos);
            if (pot != null && !pot.isDetonating()) {
                pot.markDetonating();
                IncendiaryFuse.burstPlaced(serverLevel, Vec3.atCenterOf(pos), pot.filling(), 0, pot.lighter());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Detonates the pot after {@code delay} ticks: a pot with a base bursts, others shatter. Blame goes to {@code
     * responsible}, or else the player who lit it.
     */
    public static void detonate(ServerLevel level, BlockPos pos, int delay, @Nullable UUID responsible) {
        IncendiaryPotBlockEntity pot = pot(level, pos);
        if (pot == null || pot.isDetonating()) {
            return;
        }
        if (!pot.filling().hasBase()) {
            shatter(level, pos);
            return;
        }
        pot.markDetonating();
        PotFilling filling = pot.filling();
        UUID answers = responsible != null ? responsible : pot.lighter();
        level.removeBlock(pos, false);
        IncendiaryFuse.burstPlaced(level, Vec3.atCenterOf(pos), filling, delay, answers);
    }

    /** Drops bricks and contents, like a decorated pot hit by a projectile. */
    private static void shatter(ServerLevel level, BlockPos pos) {
        IncendiaryPotBlockEntity pot = pot(level, pos);
        if (pot == null || pot.isDetonating()) {
            return;
        }
        if (pot.isLit()) {
            // Removing a lit pot makes it burst (see onRemove).
            level.removeBlock(pos, false);
            return;
        }
        pot.markDetonating();
        for (ItemStack shard : shards(pot.filling())) {
            Block.popResource(level, pos, shard);
        }
        level.playSound(null, pos, SoundEvents.DECORATED_POT_SHATTER, SoundSource.BLOCKS, 1.0F, 1.0F);
        SiegeParticleEffects.potShatter(level, Vec3.atCenterOf(pos), level.getBlockState(pos));
        level.removeBlock(pos, false);
    }

    /** Detonates every pot within {@code radius} of {@code center} that has line of sight to it. */
    public static void detonateAround(ServerLevel level, Vec3 center, double radius, @Nullable UUID responsible) {
        if (!(radius > 0.0D)) {
            return;
        }
        BlockPos min = BlockPos.containing(center.subtract(radius, radius, radius));
        BlockPos max = BlockPos.containing(center.add(radius, radius, radius));
        List<BlockPos> reached = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).getBlock() instanceof IncendiaryPotBlock
                    && Vec3.atCenterOf(pos).distanceTo(center) <= radius && reaches(level, center, pos)) {
                reached.add(pos.immutable());
            }
        }
        for (BlockPos pos : reached) {
            detonate(level, pos, 1 + level.random.nextInt(CHAIN_DELAY), responsible);
        }
    }

    private static boolean reaches(ServerLevel level, Vec3 center, BlockPos pos) {
        Vec3 target = Vec3.atCenterOf(pos);
        //? if forge {
        /*ClipContext clip = new ClipContext(center, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
        *///?} else {
        ClipContext clip = new ClipContext(center, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty());
        //?}
        BlockHitResult hit = level.clip(clip);
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)
                || hit.getBlockPos().equals(BlockPos.containing(center));
    }

    @Nullable
    private static UUID responsibleFor(@Nullable Entity entity) {
        return entity instanceof Player player ? player.getUUID() : null;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluidState.is(Fluids.WATER))
                .setValue(WICK, PotFilling.of(context.getItemInHand()).wick());
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
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, WICK, LIT);
    }
}
