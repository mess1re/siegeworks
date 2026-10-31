package me.mss1r.siegeworks.gametest;

import com.mojang.logging.LogUtils;
import me.mss1r.axiomata.collision.system.VirtualPlatformSupport;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
//? if forge {
/*import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
*///?} else {
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
//?}

/** Opt-in dedicated-server companion to the actual client climbing probe. */
@EventBusSubscriber(modid = "siegeworks")
public final class DedicatedLadderProbe {
    @SubscribeEvent
    //? if forge {
    /*public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    *///?} else {
    public static void tick(ServerTickEvent.Post event) {
    //?}
        if (!Boolean.getBoolean("siegeworks.test.networkLadder")) return;
        var player = event.getServer().getPlayerList().getPlayerByName("SiegeRegression");
        if (player == null || player.tickCount % 10 != 0) return;
        var ladders = player.serverLevel().getEntitiesOfClass(SiegeLadderEntity.class,
                player.getBoundingBox().inflate(20));
        try {
            var type = net.minecraft.server.network.ServerGamePacketListenerImpl.class;
            var floating = type.getDeclaredField("clientIsFloating");
            var ticks = type.getDeclaredField("aboveGroundTickCount");
            floating.setAccessible(true);
            ticks.setAccessible(true);
            LogUtils.getLogger().info("LADDER_SERVER pos={} ground={} support={} climbers={} floating={} floatingTicks={}",
                    player.position(), player.onGround(), VirtualPlatformSupport.isSupported(player),
                    ladders.stream().map(SiegeLadderEntity::climberCount).toList(),
                    floating.get(player.connection), ticks.get(player.connection));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect the dedicated regression connection", exception);
        }
    }
}
