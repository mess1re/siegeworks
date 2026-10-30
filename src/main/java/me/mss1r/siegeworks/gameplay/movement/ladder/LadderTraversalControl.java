package me.mss1r.siegeworks.gameplay.movement.ladder;

import com.google.common.collect.MapMaker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.lang.ref.WeakReference;
import java.util.Map;

/** A short movement lease, not a saved NoAI/NoGravity flag. Automatically expires if traversal stops ticking. */
public final class LadderTraversalControl {
    private record Lease(WeakReference<Entity> ladder, long tick, boolean guided) {}
    private static final Map<Entity, Lease> LEASES = new MapMaker().weakKeys().makeMap();

    private LadderTraversalControl() {}

    static void hold(LivingEntity entity, Entity ladder) {
        LEASES.put(entity, new Lease(new WeakReference<>(ladder), entity.level().getGameTime(), true));
    }

    static void queue(LivingEntity entity, Entity ladder) {
        LEASES.put(entity, new Lease(new WeakReference<>(ladder), entity.level().getGameTime(), false));
    }

    /** Own the walk to the entrance as well as the line; otherwise follow goals replace its path. */
    public static void approach(LivingEntity entity, Entity ladder) {
        queue(entity, ladder);
    }

    static void release(Entity entity) { LEASES.remove(entity); }

    public static boolean controls(Entity entity) {
        return ownsNavigation(entity) && LEASES.get(entity).guided;
    }

    public static boolean ownsNavigation(Entity entity) {
        if (entity.level().isClientSide || !entity.isAlive()) return false;
        Lease lease = LEASES.get(entity);
        if (lease == null) return false;
        Entity ladder = lease.ladder.get();
        long age = entity.level().getGameTime() - lease.tick;
        if (ladder == null || !ladder.isAlive() || ladder.level() != entity.level() || age < 0 || age > 1) {
            LEASES.remove(entity);
            return false;
        }
        return true;
    }
}
