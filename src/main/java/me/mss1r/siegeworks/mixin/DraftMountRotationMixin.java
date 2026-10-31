package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class DraftMountRotationMixin {
    @Unique
    private boolean siegeworks$draftWalkInitialized;
    @Unique private double siegeworks$draftWalkX;
    @Unique private double siegeworks$draftWalkZ;

    @Inject(method = "tick", at = @At("TAIL"))
    private void siegeworks$keepSquareWithDrawnEngine(CallbackInfo ci) {
        if (!((Object) this instanceof AbstractHorse horse)) {
            return;
        }
        if (!(horse.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            siegeworks$draftWalkInitialized = false;
            return;
        }

        if (horse.level().isClientSide) {
            // A passenger is positioned after its tick, so measure its current place in
            // the moving team. This also measures the different travel of inner/outer mounts.
            var seat = siege.position().add(SiegePassengerPhysics.rotatedSeatOffset(siege, horse));
            double distance = siegeworks$draftWalkInitialized
                    ? Math.hypot(seat.x - siegeworks$draftWalkX, seat.z - siegeworks$draftWalkZ) : 0;
            siegeworks$draftWalkInitialized = true;
            siegeworks$draftWalkX = seat.x;
            siegeworks$draftWalkZ = seat.z;
            if (distance >= 1) distance = 0; // Teleporting does not represent a stride.
            boolean camel = horse instanceof Camel;
            horse.walkAnimation.update((float) Math.min(distance * (camel ? 6 : 4), 1), camel ? .2F : .4F);
        }

        horse.setYRot(siege.getYRot());
        horse.yRotO = siege.yRotO;
        horse.setYBodyRot(siege.yBodyRot);
        horse.yBodyRotO = siege.yBodyRotO;
        horse.setYHeadRot(siege.yBodyRot);
        horse.yHeadRotO = siege.yBodyRotO;
    }
}
