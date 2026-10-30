package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public final class SiegeworksSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Siegeworks.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> CANNON_DISTANT = registerFixed("cannon_distant", 256.0F);
    public static final RegistrySupplier<SoundEvent> SERPENTINE_FIRE = registerFixed("serpentine_fire", 256.0F);
    public static final RegistrySupplier<SoundEvent> RAM_IMPACT = registerFixed("ram_impact", 48.0F);
    public static final RegistrySupplier<SoundEvent> RAM_RELEASE = register("ram_release");
    public static final RegistrySupplier<SoundEvent> ROPE_CHARGE_BR = register("rope_charge_br");
    public static final RegistrySupplier<SoundEvent> SIEGE_ENGINE_MOVE = register("siege_engine_move");
    public static final RegistrySupplier<SoundEvent> ONAGER_SHOOT = registerFixed("onager_shoot", 180.0F);
    public static final RegistrySupplier<SoundEvent> ONAGER_RELOAD = register("onager_reload");
    public static final RegistrySupplier<SoundEvent> TREBUCHET_SHOOT = registerFixed("trebuchet_shoot", 220.0F);
    public static final RegistrySupplier<SoundEvent> TREBUCHET_RELOAD = registerFixed("trebuchet_reload", 32.0F);
    public static final RegistrySupplier<SoundEvent> TOWER_CROSSBOW_SHOOT = registerFixed("tower_crossbow_shoot", 24.0F);
    public static final RegistrySupplier<SoundEvent> ARCBALLISTA_SHOOT = registerFixed("arcballista_shoot", 60.0F);
    public static final RegistrySupplier<SoundEvent> ARCBALLISTA_RELOAD = register("arcballista_reload");
    public static final RegistrySupplier<SoundEvent> TOWER_CROSSBOW_RELOAD = register("tower_crossbow_reload");
    public static final RegistrySupplier<SoundEvent> BALLISTA_LOCK = register("ballista_lock");
    public static final RegistrySupplier<SoundEvent> CANNON_FIRE = registerFixed("cannon_fire", 256.0F);
    public static final RegistrySupplier<SoundEvent> BOLT_IMPACT = register("bolt_impact");
    public static final RegistrySupplier<SoundEvent> PROJECTILE_IMPACT = register("projectile_impact");
    public static final RegistrySupplier<SoundEvent> PROJECTILE_FLIGHT = registerFixed("projectile_flight", 64.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_EXPLOSION_LAYER = registerFixed("impact_explosion_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_BODY_LAYER = registerFixed("impact_body_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_STONE_LAYER = registerFixed("impact_stone_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_SHARP_STONE_LAYER = registerFixed("impact_sharp_stone_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_METAL_LAYER = registerFixed("impact_metal_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_NETHERITE_LAYER = registerFixed("impact_netherite_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_ANVIL_LAYER = registerFixed("impact_anvil_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_FALL_LAYER = registerFixed("impact_fall_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> IMPACT_FIREWORK_LAYER = registerFixed("impact_firework_layer", 128.0F);
    public static final RegistrySupplier<SoundEvent> RAIL_SKID = register("rail_skid");
    public static final RegistrySupplier<SoundEvent> SIEGE_ENGINE_DAMAGE = registerFixed("siege_engine_damage", 48.0F);

    private SiegeworksSounds() {
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(MinecraftVersionCompat.id(Siegeworks.MOD_ID, name)));
    }

    private static RegistrySupplier<SoundEvent> registerFixed(String name, float range) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(
                MinecraftVersionCompat.id(Siegeworks.MOD_ID, name), range));
    }

    public static void register() {
        SOUND_EVENTS.register();
        Siegeworks.LOG.info("Registering Siegeworks sounds");
    }
}
