package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Registry renames shared by the native migration events and Fabric lookups. */
public final class LegacyIdAliases {
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

    private LegacyIdAliases() {}
    public static Map<ResourceKey<? extends Registry<?>>, Map<ResourceLocation, ResourceLocation>> all() { return RENAMES; }
    public static ResourceLocation resolve(ResourceKey<? extends Registry<?>> registry, ResourceLocation name) {
        if (name == null || !Siegeworks.MOD_ID.equals(name.getNamespace())) return name;
        var aliases = RENAMES.get(registry);
        return aliases == null ? name : aliases.getOrDefault(name, name);
    }
    private static ResourceLocation id(String path) { return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path); }
}
