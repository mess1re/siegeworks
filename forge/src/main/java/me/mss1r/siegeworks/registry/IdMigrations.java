package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryManager;

import java.util.Map;

public final class IdMigrations {
    private static final Map<ResourceKey<? extends Registry<?>>, Map<ResourceLocation, ResourceLocation>> RENAMES =
            LegacyIdAliases.all();

    private IdMigrations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(IdMigrations::onRegister);
        MinecraftForge.EVENT_BUS.addListener(IdMigrations::onMissingMappings);
    }

    private static void onRegister(RegisterEvent event) {
        Map<ResourceLocation, ResourceLocation> renames = RENAMES.get(event.getRegistryKey());
        if (renames == null) {
            return;
        }
        renames.forEach(RegistryManager.ACTIVE.getRegistry(event.getRegistryKey().location())::addAlias);
    }

    // A save remembers the IDs it was written with, and Forge reports the renamed ones as missing unless they are
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

}
