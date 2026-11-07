package me.mss1r.siegeworks;

import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.config.SiegeworksEngineConfig;
import me.mss1r.siegeworks.config.MigratingServerConfigSpec;
import me.mss1r.siegeworks.event.NativeMantletProtectionEvents;
import me.mss1r.siegeworks.registry.IdMigrations;
import me.mss1r.siegeworks.integration.recruits.NativeRecruitsEvents;
import dev.architectury.platform.forge.EventBuses;
import me.mss1r.siegeworks.integration.rts.RtsCompat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Siegeworks.MOD_ID)
public final class SiegeworksBootstrap {
    public SiegeworksBootstrap() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Siegeworks.MOD_ID, modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SiegeworksClientConfig.SPEC.unwrap());
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, new MigratingServerConfigSpec(SiegeworksServerConfig.SPEC.unwrap()));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SiegeworksEngineConfig.SPEC.unwrap(), SiegeworksEngineConfig.FILE_NAME);
        initialize(modBus, MinecraftForge.EVENT_BUS);
        if (Platform.isModLoaded("epicfight")) {
            me.mss1r.siegeworks.integration.epicfight.NativeEpicFightCompat.register(modBus);
        }
        if (Platform.isModLoaded("recruits")) {
            NativeRecruitsEvents.register(modBus);
        }
        if (Platform.isModLoaded("recruitsrtscommand")) {
            RtsCompat.register();
        }
    }

    private static void initialize(IEventBus modBus, IEventBus gameBus) {
        IdMigrations.register(modBus);
        Siegeworks.initialize();
        NativeMantletProtectionEvents.register(gameBus);
    }
}
