package me.mss1r.siegeworks;

import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.config.SiegeworksClientConfig;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.config.SiegeworksEngineConfig;
import me.mss1r.siegeworks.config.MigratingServerConfigSpec;
import me.mss1r.siegeworks.event.NativeMantletProtectionEvents;
import me.mss1r.siegeworks.registry.IdMigrations;
import me.mss1r.siegeworks.integration.recruits.NativeRecruitsEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Siegeworks.MOD_ID)
public final class SiegeworksBootstrap {
    public SiegeworksBootstrap(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SiegeworksClientConfig.SPEC.unwrap());
        modContainer.registerConfig(ModConfig.Type.SERVER, new MigratingServerConfigSpec(SiegeworksServerConfig.SPEC.unwrap()));
        modContainer.registerConfig(ModConfig.Type.SERVER, SiegeworksEngineConfig.SPEC.unwrap(), SiegeworksEngineConfig.FILE_NAME);
        initialize(modBus, NeoForge.EVENT_BUS);
        if (Platform.isModLoaded("recruits")) {
            NativeRecruitsEvents.register(modBus);
        }
    }

    private static void initialize(IEventBus modBus, IEventBus gameBus) {
        IdMigrations.register(modBus);
        Siegeworks.initialize();
        NativeMantletProtectionEvents.register(gameBus);
    }
}
