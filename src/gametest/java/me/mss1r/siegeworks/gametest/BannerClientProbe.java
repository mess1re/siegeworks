package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
*///?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
//?}

/** Opt-in rendering diagnostic in an isolated world, excluded from production jars. */
@EventBusSubscriber(modid = "siegeworks", value = Dist.CLIENT)
public final class BannerClientProbe {
    private static int ticks;
    private static TrebuchetEntity siege;

    @SubscribeEvent
    //? if forge {
    /*public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    *///?} else {
    public static void tick(ClientTickEvent.Post event) {
    //?}
        if (!Boolean.getBoolean("siegeworks.test.bannerScene")) return;
        var mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || mc.screen != null || server == null) return;
        ticks++;
        if (ticks == 1) {
            mc.options.hideGui = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().get(0);
                var level = player.serverLevel();
                level.setDayTime(6000);
                level.setWeatherParameters(100000, 0, false, false);
                level.getEntitiesOfClass(AbstractSiegeEntity.class, new AABB(-30, 90, -30, 30, 140, 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                                new AABB(-30, 90, -30, 30, 140, 30))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                player.setGameMode(GameType.SPECTATOR);
                player.connection.teleport(4, 105, 9, 151, 5);
                siege = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
                siege.setPos(0, 100, 0);
                level.addFreshEntity(siege);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER));
                siege.handleSiegeInteraction(player, InteractionHand.MAIN_HAND, level);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_BANNER));
                siege.handleSiegeInteraction(player, InteractionHand.MAIN_HAND, level);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            });
        }
        if (ticks == 80 || ticks == 160 || ticks == 240 || ticks == 300) {
            Screenshot.grab(mc.gameDirectory, "banner-" + ticks + ".png", mc.getMainRenderTarget(),
                    message -> com.mojang.logging.LogUtils.getLogger().info("BANNER_CLIENT {}", message.getString()));
        }
        if (ticks == 100) server.execute(() -> siege.setAmmoLoaded(SiegeAmmo.AMMO_STONE));
        if (ticks == 180) server.execute(() -> {
            var player = server.getPlayerList().getPlayers().get(0);
            player.connection.teleport(-4, 106, -8, -24, 2);
        });
        if (ticks == 285) server.execute(() -> siege.handleSiegeInteraction(
                server.getPlayerList().getPlayers().get(0), InteractionHand.MAIN_HAND,
                (net.minecraft.server.level.ServerLevel) siege.level()));
        if (ticks == 320) mc.stop();
    }
}
