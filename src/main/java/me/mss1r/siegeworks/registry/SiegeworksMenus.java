package me.mss1r.siegeworks.registry;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.menu.CastingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class SiegeworksMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Siegeworks.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MenuType<CastingMenu>> CASTING = MENUS.register("casting",
            () -> MenuRegistry.ofExtended((id, inventory, data) -> new CastingMenu(id, inventory, data.readBlockPos())));
    public static void register() { MENUS.register(); }
    private SiegeworksMenus() {}
}
