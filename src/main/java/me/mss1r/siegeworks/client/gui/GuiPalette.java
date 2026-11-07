package me.mss1r.siegeworks.client.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Properties;

/** Optional resource-pack colors, with a separate fallback for every entry. */
public final class GuiPalette {
    private final Properties colors;

    private GuiPalette(Properties colors) {
        this.colors = colors;
    }

    public static GuiPalette load(ResourceManager resources, ResourceLocation location) {
        Properties colors = new Properties();
        var resource = resources.getResource(location);
        if (resource.isPresent()) {
            try (var stream = resource.get().open()) {
                colors.load(stream);
            } catch (IOException | IllegalArgumentException ignored) {
                colors.clear();
            }
        }
        return new GuiPalette(colors);
    }

    public int color(String key, int fallback) {
        String value = colors.getProperty(key, "").trim();
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(value, 16) : fallback;
    }
}
