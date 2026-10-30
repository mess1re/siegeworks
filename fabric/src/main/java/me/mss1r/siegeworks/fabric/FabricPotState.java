package me.mss1r.siegeworks.fabric;
import me.mss1r.siegeworks.block.IncendiaryPotBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public final class FabricPotState {
    public static final BooleanProperty HAS_FUEL = BooleanProperty.create("has_fuel");
    private static final java.util.ArrayDeque<IncendiaryPotBlockEntity> loadedPots = new java.util.ArrayDeque<>();
    public static void onChunkLoad(IncendiaryPotBlockEntity pot) { loadedPots.add(pot); }
    public static void finishChunkLoads() {
        IncendiaryPotBlockEntity pot;
        while ((pot = loadedPots.poll()) != null) {
            if (!pot.isRemoved()) sync(pot);
        }
    }
    public static void clearPending() { loadedPots.clear(); }
    public static void sync(IncendiaryPotBlockEntity pot) {
        var level = pot.getLevel();
        if (level == null || level.isClientSide) return;
        var state = level.getBlockState(pot.getBlockPos());
        boolean fuel = pot.filling().hasBase();
        if (state.hasProperty(HAS_FUEL) && state.getValue(HAS_FUEL) != fuel)
            level.setBlock(pot.getBlockPos(), state.setValue(HAS_FUEL, fuel), Block.UPDATE_CLIENTS);
    }
}
