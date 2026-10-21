package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
//? if forge {
/*import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryManager;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
//?}

import java.util.Map;

public final class ItemIdMigrations {
    // Keep old saves and datapack references readable; newly saved stacks use the registered ID.
    private static final Map<ResourceLocation, ResourceLocation> RENAMES = Map.of(
            id("singijeon"), id("so_singijeon"),
            id("explosive_singijeon"), id("jung_singijeon"),
            // Rope is Axiomata's now, shared with every mod that builds with it.
            id("rope"), MinecraftVersionCompat.id("axiomata", "rope"));

    private ItemIdMigrations() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ItemIdMigrations::onRegister);
        //? if forge {
        /*MinecraftForge.EVENT_BUS.addListener(ItemIdMigrations::onMissingMappings);
        *///?}
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ITEM)) {
            return;
        }
        //? if forge {
        /*RENAMES.forEach(RegistryManager.ACTIVE.getRegistry(Registries.ITEM)::addAlias);
        *///?} else {
        RENAMES.forEach(event.getRegistry()::addAlias);
        //?}
    }

    //? if forge {
    /*// A save remembers the IDs it was written with, and Forge reports the renamed ones as missing unless they are
    // remapped here.
    private static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping
                : event.getMappings(ForgeRegistries.Keys.ITEMS, Siegeworks.MOD_ID)) {
            ResourceLocation renamed = RENAMES.get(mapping.getKey());
            if (renamed != null && ForgeRegistries.ITEMS.containsKey(renamed)) {
                mapping.remap(ForgeRegistries.ITEMS.getValue(renamed));
            }
        }
    }
    *///?}

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
