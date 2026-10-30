package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryManager;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
//?}

import java.util.Map;

public final class IdMigrations {
    // Keep old saves and datapack references readable; newly saved stacks and entities use the registered ID.
    private static final Map<ResourceKey<? extends Registry<?>>, Map<ResourceLocation, ResourceLocation>> RENAMES =
            Map.of(
            Registries.ITEM, Map.of(
                    id("singijeon"), id("so_singijeon"),
                    id("explosive_singijeon"), id("jung_singijeon"),
                    // Rope is Axiomata's now, shared with every mod that builds with it.
                    id("rope"), MinecraftVersionCompat.id("axiomata", "rope"),
                    // The mangonel was a torsion onager all along.
                    id("mangonel_spawner"), id("onager_spawner")),
            Registries.ENTITY_TYPE, Map.of(
                    id("mangonel"), id("onager"),
                    id("mangonel_projectile"), id("onager_projectile"),
                    id("mangonel_passenger_projectile"), id("onager_passenger_projectile")),
            Registries.SOUND_EVENT, Map.of(
                    id("mangonel_shoot"), id("onager_shoot"),
                    id("mangonel_reload"), id("onager_reload")));

    private IdMigrations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(IdMigrations::onRegister);
        //? if forge {
        /*MinecraftForge.EVENT_BUS.addListener(IdMigrations::onMissingMappings);
        *///?}
    }

    private static void onRegister(RegisterEvent event) {
        Map<ResourceLocation, ResourceLocation> renames = RENAMES.get(event.getRegistryKey());
        if (renames == null) {
            return;
        }
        //? if forge {
        /*renames.forEach(RegistryManager.ACTIVE.getRegistry(event.getRegistryKey().location())::addAlias);
        *///?} else {
        renames.forEach(event.getRegistry()::addAlias);
        //?}
    }

    //? if forge {
    /*// A save remembers the IDs it was written with, and Forge reports the renamed ones as missing unless they are
    // remapped here.
    private static void onMissingMappings(MissingMappingsEvent event) {
        RENAMES.forEach((key, renames) -> remap(event, key, renames));
    }

    @SuppressWarnings("unchecked")
    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<?>> key,
                                  Map<ResourceLocation, ResourceLocation> renames) {
        ResourceKey<? extends Registry<T>> registryKey = (ResourceKey<? extends Registry<T>>) key;
        IForgeRegistry<T> registry = RegistryManager.ACTIVE.getRegistry(registryKey);
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registryKey, Siegeworks.MOD_ID)) {
            ResourceLocation renamed = renames.get(mapping.getKey());
            if (renamed != null && registry.containsKey(renamed)) {
                mapping.remap(registry.getValue(renamed));
            }
        }
    }
    *///?}

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
