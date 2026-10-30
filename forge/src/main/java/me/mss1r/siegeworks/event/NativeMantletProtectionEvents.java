package me.mss1r.siegeworks.event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
public final class NativeMantletProtectionEvents {
    public static void register(IEventBus bus) { bus.addListener(NativeMantletProtectionEvents::damage); }
    private static void damage(LivingHurtEvent event) {
        event.setAmount(MantletProtectionHandler.reduce(event.getEntity(), event.getSource(), event.getAmount()));
    }
}
