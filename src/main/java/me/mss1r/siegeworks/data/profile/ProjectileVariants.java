package me.mss1r.siegeworks.data.profile;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;

/** Projectile profiles for ammunition that shares an entity type with another but hits differently. */
public final class ProjectileVariants {
    public static final ResourceLocation EXPLOSIVE_SINGIJEON = id("explosive_singijeon");
    public static final ResourceLocation STONE_SCATTERSHOT = id("stone_scattershot");
    public static final ResourceLocation ONAGER_FIRE_PROJECTILE = id("onager_fire_projectile");
    public static final ResourceLocation TREBUCHET_FIRE_PROJECTILE = id("trebuchet_fire_projectile");

    private ProjectileVariants() {
    }

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
