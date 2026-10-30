package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;

public final class IdMigrations {
    private static final Map<ResourceKey<? extends Registry<?>>, Map<ResourceLocation, ResourceLocation>> RENAMES =
            LegacyIdAliases.all();

    private IdMigrations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(IdMigrations::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        Map<ResourceLocation, ResourceLocation> renames = RENAMES.get(event.getRegistryKey());
        if (renames == null) {
            return;
        }
        renames.forEach(event.getRegistry()::addAlias);
    }


}
