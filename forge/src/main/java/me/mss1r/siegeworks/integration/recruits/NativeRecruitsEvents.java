package me.mss1r.siegeworks.integration.recruits;
import net.minecraftforge.eventbus.api.IEventBus;
public final class NativeRecruitsEvents {
    public static void register(IEventBus modBus) {
        RecruitsCompat.register();
        modBus.addListener(NativeRecruitsEvents::load);
        modBus.addListener(NativeRecruitsEvents::client);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(NativeRecruitsEvents::tick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(NativeRecruitsEvents::server);
    }
    private static void load(net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent event) { RecruitsCompat.loadComplete(); }
    private static void client(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RecruitsCompat.clientSetup();
        });
    }
    private static void tick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) { RecruitsCompat.livingTick(event.getEntity()); }
    private static void server(net.minecraftforge.event.server.ServerStartingEvent event) { RecruitsCompat.serverStarting(); }
}
