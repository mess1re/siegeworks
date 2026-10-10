package me.mss1r.siegeworks;

import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.event.MantletProtectionHandler;
import me.mss1r.siegeworks.registry.ItemIdMigrations;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
//? if forge {
/*import dev.architectury.platform.forge.EventBuses;
import me.mss1r.siegeworks.integration.rts.RtsCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(Siegeworks.MOD_ID)
public final class SiegeworksBootstrap {
    //? if forge {
    /*public SiegeworksBootstrap() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Siegeworks.MOD_ID, modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SiegeworksClientConfig.SPEC.unwrap());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SiegeworksServerConfig.SPEC.unwrap());
        initialize(modBus, MinecraftForge.EVENT_BUS);
        if (Platform.isModLoaded("recruits")) {
            RecruitsCompat.register(modBus);
        }
        if (Platform.isModLoaded("recruitsrtscommand")) {
            RtsCompat.register();
        }
    }
    *///?} else {
    public SiegeworksBootstrap(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SiegeworksClientConfig.SPEC.unwrap());
        modContainer.registerConfig(ModConfig.Type.SERVER, SiegeworksServerConfig.SPEC.unwrap());
        initialize(modBus, NeoForge.EVENT_BUS);
        if (Platform.isModLoaded("recruits")) {
            RecruitsCompat.register(modBus);
        }
    }
    //?}

    private static void initialize(IEventBus modBus, IEventBus gameBus) {
        ItemIdMigrations.register(modBus);
        Siegeworks.initialize();
        MantletProtectionHandler.register(gameBus);
    }
}
