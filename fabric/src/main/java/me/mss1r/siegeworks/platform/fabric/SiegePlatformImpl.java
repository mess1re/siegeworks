package me.mss1r.siegeworks.platform.fabric;

import java.nio.file.Path;
import java.util.function.Supplier;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.entity.FakePlayer;

public final class SiegePlatformImpl {
    public static me.mss1r.siegeworks.item.FirearmItem firearm(String name, boolean matchlock) {
        return new me.mss1r.siegeworks.item.FirearmItem(name, matchlock, new Item.Properties().stacksTo(1));
    }

    public static me.mss1r.siegeworks.block.IncendiaryPotBlock incendiaryPot(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new me.mss1r.siegeworks.block.IncendiaryPotBlock(properties) {
            { registerDefaultState(defaultBlockState().setValue(me.mss1r.siegeworks.fabric.FabricPotState.HAS_FUEL, false)); }
            @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.state.BlockState> builder) {
                super.createBlockStateDefinition(builder);
                builder.add(me.mss1r.siegeworks.fabric.FabricPotState.HAS_FUEL);
            }
            //? if >=1.21 {
            @Override protected void onExplosionHit(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Explosion explosion,
                    java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, net.minecraft.core.BlockPos> drops) {
                if (!handleExplosion(state, level, pos, explosion)) super.onExplosionHit(state, level, pos, explosion, drops);
            }
            //?}
        };
    }
    public static net.minecraft.nbt.CompoundTag persistentData(net.minecraft.world.entity.Entity entity) {
        return me.mss1r.axiomata.platform.AxiomataPlatform.persistentData(entity);
    }
    public static boolean isFakePlayer(Player player) { return player instanceof FakePlayer; }
    public static SiegeDeploymentItem deploymentItem(Supplier<? extends EntityType<?>> type, Item.Properties properties) {
        return new SiegeDeploymentItem(type, properties);
    }
    public static Path defaultConfigDirectory() { return Platform.getGameFolder().resolve("defaultconfigs"); }
    public static double entityReachBonus(Player player) {
        //? if >=1.21 {
        var attribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE);
        //?} else {
        /*var attribute = player.getAttribute(entityReachAttribute());
        *///?}
        return attribute == null ? 0 : attribute.getValue() - attribute.getBaseValue();
    }
    //? if <1.21 {
    /*public static net.minecraft.world.entity.ai.attributes.Attribute entityReachAttribute() {
        return com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes.ATTACK_RANGE;
    }
    *///?}
}
