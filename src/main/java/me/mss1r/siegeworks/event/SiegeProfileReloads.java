package me.mss1r.siegeworks.event;

import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.event.events.common.LifecycleEvent;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.axiomata.data.profile.JsonProfileReloadListener;
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.ProfileFormat;
import me.mss1r.axiomata.ballistics.profile.BlockMaterialProfile;
import me.mss1r.siegeworks.data.profile.BlockMaterialProfiles;
import me.mss1r.siegeworks.data.profile.SiegeEngineProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public final class SiegeProfileReloads {
    private SiegeProfileReloads() {
    }

    public static void register() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                        "definitions/siege_engines",
                        "siege engine profiles",
                        SiegeEngineProfile.CODEC,
                        SiegeEngineProfile::validationError,
                        SiegeProfileCatalogs.ENGINES, ProfileFormat::engine),
                id("siege_engine_profiles"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                        "definitions/projectile_physics",
                        "projectile physics profiles",
                        ProjectilePhysicsProfile.CODEC,
                        ProjectilePhysicsProfile::validationError,
                        SiegeProfileCatalogs.PROJECTILES, ProfileFormat::projectile),
                id("projectile_physics_profiles"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                        "definitions/block_materials", "block materials", BlockMaterialProfile.CODEC,
                        BlockMaterialProfile::validationError, BlockMaterialProfiles.CATALOG,
                        ProfileFormat::blockMaterial), id("block_materials"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new JsonProfileReloadListener<>(
                "definitions/pot_fillings", "pot fillings", PotFillingProfile.CODEC,
                PotFillingProfile::validationError, SiegeProfileCatalogs.POT_FILLINGS,
                ProfileFormat::potFilling), id("pot_fillings"));
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            SiegeProfileCatalogs.ENGINES.reset();
            SiegeProfileCatalogs.PROJECTILES.reset();
            BlockMaterialProfiles.reset();
            SiegeProfileCatalogs.POT_FILLINGS.reset();
        });
    }

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
