package me.mss1r.siegeworks.block;

import me.mss1r.siegeworks.gameplay.ballistics.IncendiaryFuse;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.registry.SiegeworksBlockEntities;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Contents of a placed pot, plus its fuse state and who lit it. */
public class IncendiaryPotBlockEntity extends BlockEntity {
    private static final String TAG_FILLING = "Filling";
    private static final String TAG_BURST_AT = "BurstAt";
    private static final String TAG_FUSE_LENGTH = "FuseLength";
    private static final String TAG_LIGHTER = "Lighter";

    private PotFilling filling = PotFilling.EMPTY;
    private long burstAt = -1L;
    private int fuseLength;
    @Nullable
    private UUID lighter;
    private boolean detonating;

    public IncendiaryPotBlockEntity(BlockPos pos, BlockState state) {
        super(SiegeworksBlockEntities.INCENDIARY_POT.get(), pos, state);
    }

    public PotFilling filling() {
        return filling;
    }

    public void setFilling(PotFilling filling) {
        this.filling = filling;
        changed();
    }

    public boolean isLit() {
        return burstAt >= 0L;
    }

    /** Lights the fuse to burst {@code length} ticks after {@code now}. {@code lighter} is blamed for the fire. */
    public void light(long now, int length, @Nullable UUID lighter) {
        this.burstAt = now + length;
        this.fuseLength = length;
        this.lighter = lighter;
        changed();
    }

    @Nullable
    public UUID lighter() {
        return lighter;
    }

    /** Set while the pot is being removed to burst, so the removal doesn't trigger a second burst. */
    public boolean isDetonating() {
        return detonating;
    }

    public void markDetonating() {
        detonating = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncendiaryPotBlockEntity pot) {
        if (pot.isLit() && level.getGameTime() >= pot.burstAt && level instanceof ServerLevel serverLevel) {
            IncendiaryPotBlock.detonate(serverLevel, pos, 0, pot.lighter);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, IncendiaryPotBlockEntity pot) {
        if (!pot.isLit()) {
            return;
        }
        double burnt = 1.0D - (double) (pot.burstAt - level.getGameTime()) / Math.max(1, pot.fuseLength);
        Vec3 corner = Vec3.atLowerCornerOf(pos);
        IncendiaryFuse.sparkle(level, corner.add(IncendiaryPotBlock.WICK_TIP), corner.add(IncendiaryPotBlock.WICK_BASE),
                burnt);
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void read(CompoundTag tag) {
        filling = tag.contains(TAG_FILLING) ? PotFilling.load(tag.getCompound(TAG_FILLING)) : PotFilling.EMPTY;
        burstAt = tag.contains(TAG_BURST_AT) ? tag.getLong(TAG_BURST_AT) : -1L;
        fuseLength = tag.getInt(TAG_FUSE_LENGTH);
        lighter = tag.hasUUID(TAG_LIGHTER) ? tag.getUUID(TAG_LIGHTER) : null;
    }

    private void write(CompoundTag tag) {
        tag.put(TAG_FILLING, filling.save());
        if (isLit()) {
            tag.putLong(TAG_BURST_AT, burstAt);
            tag.putInt(TAG_FUSE_LENGTH, fuseLength);
        }
        if (lighter != null) {
            tag.putUUID(TAG_LIGHTER, lighter);
        }
    }

    //? if <1.21 {
    /*@Override
    public void load(CompoundTag tag) {
        super.load(tag);
        read(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        write(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
    *///?} else {
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        read(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    //?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
