package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.event.EpicFightEventHooks;

public final class NativeEpicFightCompat {
    private NativeEpicFightCompat() {}
    public static void register(IEventBus bus) {
        bus.addListener((AnimationRegistryEvent event) -> event.newBuilder("siegeworks", EpicFirearmAnimations::build));
        EpicFightEventHooks.Registry.WEAPON_CAPABILITY_PRESET.registerEvent(event -> event.getTypeEntry().put(
                MinecraftVersionCompat.id("siegeworks", "firearm"), item -> EpicFirearmAnimations.capability()), "siegeworks:firearm");
        if (FMLEnvironment.dist == Dist.CLIENT) NativeEpicFightClient.register(bus);
    }
}
