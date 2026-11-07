package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;

public final class NativeEpicFightCompat {
    private NativeEpicFightCompat() {}
    public static void register(IEventBus bus) {
        bus.addListener((AnimationRegistryEvent event) -> event.newBuilder("siegeworks", EpicFirearmAnimations::build));
        bus.addListener((WeaponCapabilityPresetRegistryEvent event) -> event.getTypeEntry().put(
                MinecraftVersionCompat.id("siegeworks", "firearm"), item -> EpicFirearmAnimations.capability()));
        if (FMLEnvironment.dist == Dist.CLIENT) NativeEpicFightClient.register(bus);
    }
}
