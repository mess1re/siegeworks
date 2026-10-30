package me.mss1r.siegeworks.integration.recruits;
import net.neoforged.bus.api.IEventBus;
public final class NativeRecruitsEvents {
    public static void register(IEventBus modBus) {
        RecruitsCompat.register();
        modBus.addListener(NativeRecruitsEvents::load);
        modBus.addListener(NativeRecruitsEvents::client);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(NativeRecruitsEvents::tick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(NativeRecruitsEvents::server);
    }
    private static void load(net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) { RecruitsCompat.loadComplete(); }
    private static void client(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RecruitsCompat.clientSetup();
        });
    }
    private static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) { RecruitsCompat.livingTick(event.getEntity()); }
    private static void server(net.neoforged.neoforge.event.server.ServerStartingEvent event) { RecruitsCompat.serverStarting(); }
}
