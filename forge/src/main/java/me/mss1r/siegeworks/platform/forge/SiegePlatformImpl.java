package me.mss1r.siegeworks.platform.forge;

import java.nio.file.Path;
import java.util.function.Supplier;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import me.mss1r.siegeworks.client.item.SiegeDeploymentItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

public final class SiegePlatformImpl {
    public static me.mss1r.siegeworks.item.FirearmItem firearm(String name, boolean matchlock) {
        return new me.mss1r.siegeworks.item.FirearmItem(name, matchlock, new Item.Properties().stacksTo(1)) {
            @Override public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    private me.mss1r.siegeworks.client.firearm.FirearmItemRenderer renderer;
                    @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) renderer = new me.mss1r.siegeworks.client.firearm.FirearmItemRenderer();
                        return renderer;
                    }
                });
            }
        };
    }

    public static me.mss1r.siegeworks.block.IncendiaryPotBlock incendiaryPot(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return new me.mss1r.siegeworks.block.IncendiaryPotBlock(properties) {
            @Override public void onBlockExploded(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.Explosion explosion) {
                if (!handleExplosion(state, level, pos, explosion)) super.onBlockExploded(state, level, pos, explosion);
            }
            @Override public int getFlammability(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction) {
                return potFlammability(state, level, pos, direction);
            }
            @Override public int getFireSpreadSpeed(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction) {
                return potFireSpreadSpeed(state, level, pos, direction);
            }
            @Override public void onCaughtFire(net.minecraft.world.level.block.state.BlockState state,
                    net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction,
                    net.minecraft.world.entity.LivingEntity igniter) {
                catchFire(state, level, pos, direction, igniter);
            }
        };
    }
    public static net.minecraft.nbt.CompoundTag persistentData(net.minecraft.world.entity.Entity entity) { return entity.getPersistentData(); }
    public static boolean isFakePlayer(Player player) { return player instanceof FakePlayer; }
    public static net.minecraft.world.entity.ai.attributes.Attribute entityReachAttribute() { return net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(); }
    public static double entityReachBonus(Player player) {
        var attribute = player.getAttribute(entityReachAttribute());
        return attribute == null ? 0 : attribute.getValue() - attribute.getBaseValue();
    }
    public static Path defaultConfigDirectory() {
        return FMLPaths.GAMEDIR.get().resolve(FMLConfig.getConfigValue(FMLConfig.ConfigValue.DEFAULT_CONFIG_PATH));
    }
    public static SiegeDeploymentItem deploymentItem(Supplier<? extends EntityType<?>> type, Item.Properties properties) {
        return new SiegeDeploymentItem(type, properties) {
            @Override
            public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
                consumer.accept(new IClientItemExtensions() {
                    private SiegeDeploymentItemRenderer renderer;
                    @Override
                    public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) renderer = SiegeDeploymentItemRenderer.create();
                        return renderer;
                    }
                });
            }
        };
    }
}
