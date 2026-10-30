package me.mss1r.siegeworks.event;

import dev.architectury.event.events.common.ExplosionEvent;
import me.mss1r.siegeworks.gameplay.ballistics.ExplosionPhysics;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ExplosionPhysicsHandler {
    private ExplosionPhysicsHandler() {
    }

    public static void register() {
        ExplosionEvent.DETONATE.register(ExplosionPhysicsHandler::onExplosionDetonate);
    }

    private static void onExplosionDetonate(Level level, Explosion explosion,
                                            List<Entity> affectedEntities) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        List<BlockPos> affectedBlocks = explosion.getToBlow();
        Vec3 center =
                //? if <1.21 {
                /*new Vec3(((me.mss1r.siegeworks.mixin.ExplosionPositionAccessor) explosion).siegeworks$x(),
                        ((me.mss1r.siegeworks.mixin.ExplosionPositionAccessor) explosion).siegeworks$y(),
                        ((me.mss1r.siegeworks.mixin.ExplosionPositionAccessor) explosion).siegeworks$z())
                *///?} else {
                explosion.center()
                //?}
                ;
        float radius = 0.0F;
        for (BlockPos pos : affectedBlocks) {
            radius = Math.max(radius, (float) Vec3.atCenterOf(pos).distanceTo(center));
        }
        var breaker = SiegeBlockBreaker.responsiblePlayer(explosion.getDirectSourceEntity());
        if (breaker == null) {
            breaker = SiegeBlockBreaker.responsiblePlayer(explosion.getIndirectSourceEntity());
        }
        ExplosionPhysics.scatterAffectedBlocks(serverLevel, center, radius, affectedBlocks, breaker);
    }
}
