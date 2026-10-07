package me.mss1r.siegeworks.gameplay.loading;

import me.mss1r.axiomata.loading.LoadingController;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public final class SiegeLoadingController extends LoadingController {
    public interface Host extends LoadingController.Host {
        AbstractSiegeEntity siege();

        @Override default Entity entity() { return siege(); }
        @Override default int loadStage() { return siege().getLoadStage(); }
        @Override default int cooldown() { return siege().getCooldown(); }
        @Override default boolean ammoLoaded() { return siege().hasAmmoLoaded(); }
        @Override default int windingTime() { return siege().getWindingTime(); }
        @Override default int windingTotal() { return siege().getWindingTotal(); }
        @Override default Component message(String key, Object... arguments) {
            return Component.translatable("siege.loading." + key, arguments);
        }
    }

    public SiegeLoadingController(Host host) {
        super(host);
    }
}
