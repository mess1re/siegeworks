package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.client.aim.SiegeAimView;
import me.mss1r.siegeworks.client.aim.SiegeAimController;
import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float eyeHeight;

    @Shadow private float eyeHeightOld;

    @Shadow protected abstract void setPosition(Vec3 position);

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    //? if <1.21 {
    /*@Shadow protected abstract void move(double distanceOffset, double verticalOffset, double horizontalOffset);

    @Invoker("getMaxZoom")
    protected abstract double siegeworks$getMaxZoom(double distance);
    *///?} else {
    @Shadow protected abstract void move(float distanceOffset, float verticalOffset, float horizontalOffset);

    @Invoker("getMaxZoom")
    protected abstract float siegeworks$getMaxZoom(float distance);
    //?}

    @Inject(method = "setup", at = @At("TAIL"))
    private void siegeworks$aimFromSiege(BlockGetter level, Entity renderViewEntity, boolean detached,
                                         boolean mirror, float partialTick, CallbackInfo ci) {
        if (!(renderViewEntity instanceof Player player)) {
            return;
        }

        AbstractSiegeEntity siege = SiegeAimView.findControlledSiege(player);
        if (siege == null) return;
        siege.updateRenderedAim(partialTick);
        if (SiegeAimController.isFreeLookActive(player, siege)) {
            float freeLookYaw = SiegeAimController.getFreeLookYaw(siege);
            float freeLookPitch = SiegeAimController.getFreeLookPitch(siege);
            if (detached) {
                double x = Mth.lerp(partialTick, player.xo, player.getX());
                double y = Mth.lerp(partialTick, player.yo, player.getY())
                        + Mth.lerp(partialTick, eyeHeightOld, eyeHeight);
                double z = Mth.lerp(partialTick, player.zo, player.getZ());
                setPosition(new Vec3(x, y, z));
                setRotation(mirror ? freeLookYaw + 180.0F : freeLookYaw,
                        mirror ? -freeLookPitch : freeLookPitch);
                move(-siegeworks$getMaxZoom(4.0F), 0.0F, 0.0F);
            } else {
                setRotation(freeLookYaw, freeLookPitch);
            }
            return;
        }

        float yaw = siege.getRenderedAimYaw();
        float pitch = siege.getRenderedAimPitch();

        if (!detached && siege instanceof AbstractFieldGunEntity gun
                && SiegeAimController.isBarrelSightActive(player, siege)) {
            setPosition(gun.barrelSightPosition(partialTick, yaw, pitch));
            setRotation(yaw, pitch);
            return;
        }

        if (detached) {
            double x = Mth.lerp(partialTick, player.xo, player.getX());
            double y = Mth.lerp(partialTick, player.yo, player.getY())
                    + Mth.lerp(partialTick, eyeHeightOld, eyeHeight);
            double z = Mth.lerp(partialTick, player.zo, player.getZ());
            setPosition(new Vec3(x, y, z));
            setRotation(mirror ? yaw + 180.0F : yaw, mirror ? -pitch : pitch);
            move(-siegeworks$getMaxZoom(4.0F), 0.0F, 0.0F);
            return;
        }

        if (siege instanceof AbstractBoltThrowerEntity boltThrower) {
            double x = Mth.lerp(partialTick, boltThrower.xo, boltThrower.getX());
            double y = Mth.lerp(partialTick, boltThrower.yo, boltThrower.getY());
            double z = Mth.lerp(partialTick, boltThrower.zo, boltThrower.getZ());
            setPosition(boltThrower.getAimCameraPosition(x, y, z, yaw, pitch));
        }
        setRotation(yaw, pitch);
    }
}
