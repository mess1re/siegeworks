package me.mss1r.siegeworks.gameplay.upgrade;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.data.profile.SiegeEngineProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * The engine's finish. Wood left under the open sky weathers once, losing a share of the engine's health, until a
 * repair of at least that share makes it fresh again. Honeycomb waxes an engine for good, so it never weathers; copper
 * gives a cannon a bronze barrel, for its looks only. Both are added a few items at a time, like the tower's hides.
 */
public final class EngineFinishController {
    public static final byte WEATHERED = 1;
    public static final byte WAXED = 2;
    public static final byte BRONZE = 4;

    private static final String TAG_WEATHERED = "Weathered";
    private static final String TAG_WAXED = "Waxed";
    private static final String TAG_BRONZE = "Bronze";
    private static final String TAG_EXPOSURE = "WeatherExposure";
    private static final String TAG_WAX = "WaxApplied";
    private static final String TAG_COPPER = "CopperApplied";
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double TICKS_PER_DAY = 24000.0D;
    private static final TagKey<Item> COPPER_INGOTS = TagKey.create(
            Registries.ITEM,
            //? if forge {
            /*MinecraftVersionCompat.id("forge", "ingots/copper")
            *///?} else {
            MinecraftVersionCompat.id("c", "ingots/copper")
            //?}
    );

    private final Host host;
    // Ticks of weather taken under the open sky; rain counts several times over
    private double exposure;
    private int waxApplied;
    private int copperApplied;

    public EngineFinishController(Host host) {
        this.host = host;
    }

    public boolean weathered() {
        return (host.finish() & WEATHERED) != 0;
    }

    public boolean waxed() {
        return (host.finish() & WAXED) != 0;
    }

    public boolean bronze() {
        return (host.finish() & BRONZE) != 0;
    }

    public int waxApplied() {
        return waxApplied;
    }

    public int waxRequired() {
        return profile().waxHoneycomb();
    }

    public int copperApplied() {
        return copperApplied;
    }

    public int copperRequired() {
        return profile().bronzeCopper();
    }

    public double exposure() {
        return exposure;
    }

    public void setExposure(double exposure) {
        this.exposure = Math.max(0.0D, exposure);
    }

    /** The texture suffix for this finish: {@code _bronze} for a bronze barrel, then {@code _waxed} or {@code _weathered}. */
    public static String textureSuffix(byte finish) {
        String barrel = (finish & BRONZE) != 0 ? "_bronze" : "";
        if ((finish & WAXED) != 0) {
            return barrel + "_waxed";
        }
        return (finish & WEATHERED) != 0 ? barrel + "_weathered" : barrel;
    }

    public void tick(ServerLevel level) {
        AbstractSiegeEntity siege = host.siege();
        if (siege.tickCount % CHECK_INTERVAL_TICKS != 0 || weathered() || waxed()
                || !SiegeworksServerConfig.isWeatheringEnabled()) {
            return;
        }
        // The height map, not sky light: light is not worked out yet in freshly loaded chunks
        BlockPos top = BlockPos.containing(siege.getX(), siege.getBoundingBox().maxY, siege.getZ());
        if (top.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING, top.getX(), top.getZ())) {
            return;
        }
        boolean rain = level.isRaining()
                && level.getBiome(top).value().getPrecipitationAt(top) == Biome.Precipitation.RAIN;
        double rate = rain ? SiegeworksServerConfig.getWeatheringRainMultiplier() : 1.0D;
        exposure += CHECK_INTERVAL_TICKS * rate;
        if (exposure >= SiegeworksServerConfig.getWeatheringDays() * TICKS_PER_DAY) {
            weather();
        }
    }

    /** Weathers the engine now: it loses its share of health, never below 1. */
    public void weather() {
        AbstractSiegeEntity siege = host.siege();
        exposure = 0.0D;
        setFlag(WEATHERED, true);
        float loss = (float) (siege.getMaxHealth() * SiegeworksServerConfig.getWeatheringHealthLoss());
        siege.setHealth(Math.max(Math.min(1.0F, siege.getHealth()), siege.getHealth() - loss));
    }

    /** A repair restoring at least the share lost to the weather makes a weathered engine fresh again. */
    public void onRepaired(float restoredHealth) {
        AbstractSiegeEntity siege = host.siege();
        float needed = (float) (siege.getMaxHealth() * SiegeworksServerConfig.getWeatheringHealthLoss());
        if (weathered() && restoredHealth >= needed - 1.0E-3F) {
            setFlag(WEATHERED, false);
            exposure = 0.0D;
        }
    }

    /** Applies honeycomb or copper from the hand, or returns null when the item is neither for this engine. */
    @Nullable
    public InteractionResult interact(Player player, InteractionHand hand, ServerLevel level) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.HONEYCOMB) && waxRequired() > 0) {
            return wax(player, stack, level);
        }
        if (stack.is(COPPER_INGOTS) && copperRequired() > 0) {
            return addCopper(player, stack, level);
        }
        return null;
    }

    private InteractionResult wax(Player player, ItemStack stack, ServerLevel level) {
        if (waxed()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.already_waxed"), true);
            return InteractionResult.SUCCESS;
        }
        if (weathered()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.weathered_wax"), true);
            return InteractionResult.SUCCESS;
        }
        int required = waxRequired();
        int added = take(player, stack, required - waxApplied);
        waxApplied += added;
        AbstractSiegeEntity siege = host.siege();
        level.sendParticles(ParticleTypes.WAX_ON, siege.getX(), siege.getY() + siege.getBbHeight() * 0.5D,
                siege.getZ(), 8, 0.6D, 0.5D, 0.6D, 0.5D);
        level.playSound(null, siege.blockPosition(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (waxApplied >= required) {
            setFlag(WAXED, true);
            exposure = 0.0D;
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.waxed"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.wax_progress",
                    waxApplied, required), true);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult addCopper(Player player, ItemStack stack, ServerLevel level) {
        if (bronze()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.already_bronze"), true);
            return InteractionResult.SUCCESS;
        }
        int required = copperRequired();
        int added = take(player, stack, required - copperApplied);
        copperApplied += added;
        AbstractSiegeEntity siege = host.siege();
        level.playSound(null, siege.blockPosition(), SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
        if (copperApplied >= required) {
            setFlag(BRONZE, true);
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.bronze"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.siegeworks.finish.bronze_progress",
                    copperApplied, required), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static int take(Player player, ItemStack stack, int wanted) {
        if (player.getAbilities().instabuild) {
            return wanted;
        }
        int taken = Math.min(stack.getCount(), wanted);
        stack.shrink(taken);
        return taken;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(TAG_WEATHERED, weathered());
        tag.putBoolean(TAG_WAXED, waxed());
        tag.putBoolean(TAG_BRONZE, bronze());
        tag.putDouble(TAG_EXPOSURE, exposure);
        tag.putInt(TAG_WAX, waxApplied);
        tag.putInt(TAG_COPPER, copperApplied);
    }

    public void load(CompoundTag tag) {
        byte finish = 0;
        if (tag.getBoolean(TAG_WEATHERED)) finish |= WEATHERED;
        if (tag.getBoolean(TAG_WAXED)) finish |= WAXED;
        if (tag.getBoolean(TAG_BRONZE)) finish |= BRONZE;
        host.setFinish(finish);
        exposure = Math.max(0.0D, tag.getDouble(TAG_EXPOSURE));
        waxApplied = Mth.clamp(tag.getInt(TAG_WAX), 0, Math.max(0, waxRequired()));
        copperApplied = Mth.clamp(tag.getInt(TAG_COPPER), 0, Math.max(0, copperRequired()));
    }

    private void setFlag(byte flag, boolean on) {
        host.setFinish((byte) (on ? host.finish() | flag : host.finish() & ~flag));
    }

    private SiegeEngineProfile profile() {
        return SiegeProfileCatalogs.ENGINES.forEntity(host.siege().getType());
    }

    public interface Host {
        AbstractSiegeEntity siege();

        byte finish();

        void setFinish(byte finish);
    }
}
