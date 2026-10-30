package me.mss1r.siegeworks.fabric;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.*;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.fabricmc.api.ModInitializer;
//? if >=1.21 {
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import net.neoforged.fml.config.ModConfig;
//?} else {
/*import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;
*///?}

public final class SiegeworksFabric implements ModInitializer {
    @Override public void onInitialize() {
        //? if >=1.21 {
        var configs = NeoForgeConfigRegistry.INSTANCE;
        //?} else {
        /*var configs = ForgeConfigRegistry.INSTANCE;
        *///?}
        configs.register(Siegeworks.MOD_ID, ModConfig.Type.CLIENT, SiegeworksClientConfig.SPEC.unwrap());
        var serverSpec = new MigratingServerConfigSpec(SiegeworksServerConfig.SPEC.unwrap());
        //? if >=1.21 {
        serverSpec.bind(configs.register(Siegeworks.MOD_ID, ModConfig.Type.SERVER, serverSpec));
        //?} else {
        /*configs.register(Siegeworks.MOD_ID, ModConfig.Type.SERVER, serverSpec);
        *///?}
        configs.register(Siegeworks.MOD_ID, ModConfig.Type.SERVER, SiegeworksEngineConfig.SPEC.unwrap(), SiegeworksEngineConfig.FILE_NAME);
        Siegeworks.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                chunk.getBlockEntities().values().forEach(entity -> {
                    if (entity instanceof me.mss1r.siegeworks.block.IncendiaryPotBlockEntity pot) FabricPotState.onChunkLoad(pot);
                }));
        // CHUNK_LOAD runs before its full-chunk future completes; world lookups there would wait on that same future.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> FabricPotState.finishChunkLoads());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> FabricPotState.clearPending());
        if (Platform.isModLoaded("recruits")) {
            RecruitsCompat.register();
            LifecycleEvent.SETUP.register(RecruitsCompat::loadComplete);
            LifecycleEvent.SERVER_STARTING.register(server -> RecruitsCompat.serverStarting());
        }
    }
}
