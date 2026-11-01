package me.mss1r.siegeworks.mixin;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class DraftMountMovementMixin {
    @Unique
    private AbstractSiegeEntity siegeworks$walkingVehicle;
    @Unique private double siegeworks$draftWalkX;
    @Unique private double siegeworks$draftWalkZ;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void siegeworks$letTheVehicleMoveItsMount(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof AbstractHorse horse
                && horse.getVehicle() instanceof AbstractSiegeEntity siege && siege.isTowed()) {
            // The vehicle resolves the team's terrain and suspension. A second move here
            // can push the horse into blocks before positionRider restores its harness seat.
            horse.calculateEntityAnimation(false);
            ci.cancel();
        }
    }

    @ModifyArg(method = "calculateEntityAnimation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;updateWalkAnimation(F)V"), index = 0)
    private float siegeworks$measureDraftTravel(float measuredTravel) {
        if (!((Object) this instanceof AbstractHorse horse) || !horse.level().isClientSide
                || !(horse.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) {
            siegeworks$walkingVehicle = null;
            return measuredTravel;
        }

        // rideTick positions the passenger after its animation update. Measure its seat
        // in the already moved vehicle, then let vanilla advance the animation once.
        var seat = siege.position().add(SiegePassengerPhysics.rotatedSeatOffset(siege, horse));
        double distance = siegeworks$walkingVehicle == siege
                ? Math.hypot(seat.x - siegeworks$draftWalkX, seat.z - siegeworks$draftWalkZ) : 0;
        siegeworks$walkingVehicle = siege;
        siegeworks$draftWalkX = seat.x;
        siegeworks$draftWalkZ = seat.z;
        return distance >= 1 ? 0 : (float) distance; // Teleporting is not a stride.
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void siegeworks$keepSquareWithDrawnEngine(CallbackInfo ci) {
        if (!((Object) this instanceof AbstractHorse horse)
                || !(horse.getVehicle() instanceof AbstractSiegeEntity siege) || !siege.isTowed()) return;

        horse.setYRot(siege.getYRot());
        horse.yRotO = siege.yRotO;
        horse.setYBodyRot(siege.yBodyRot);
        horse.yBodyRotO = siege.yBodyRotO;
        horse.setYHeadRot(siege.yBodyRot);
        horse.yHeadRotO = siege.yBodyRotO;
    }
}
