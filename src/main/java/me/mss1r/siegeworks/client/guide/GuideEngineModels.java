package me.mss1r.siegeworks.client.guide;

import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

final class GuideEngineModels {
    private final Map<String, Entity> models = new HashMap<>();

    Entity get(String id, ClientLevel level) {
        return models.computeIfAbsent(id, key -> {
            if (key.equals("siege_tower")) return new TowerGuidePreview(level);
            return BuiltInRegistries.ENTITY_TYPE.get(MinecraftVersionCompat.id("siegeworks", key)).create(level);
        });
    }

    void tick() {
        models.values().forEach(model -> model.tickCount++);
    }

}
