package me.mss1r.siegeworks.event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
public final class NativeMantletProtectionEvents {
    public static void register(IEventBus bus) { bus.addListener(NativeMantletProtectionEvents::damage); }
    private static void damage(LivingIncomingDamageEvent event) {
        event.setAmount(MantletProtectionHandler.reduce(event.getEntity(), event.getSource(), event.getAmount()));
    }
}
