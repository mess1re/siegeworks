package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GameRenderer.class)
public abstract class RecruitsGameRendererMixin {
    private static final double OPERATOR_PICK_PADDING = 0.1D;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void siegeworks$preferMountedOperator(float partialTick, CallbackInfo ci) {
        if (!(minecraft.hitResult instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof AbstractSiegeEntity siege)
                || minecraft.player == null
                || minecraft.gameMode == null) {
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition(partialTick);
        //? if forge {
        /*double reach = minecraft.gameMode.getPickRange();
        *///?} else {
        double reach = minecraft.player.entityInteractionRange();
        //?}
        Vec3 end = eye.add(minecraft.player.getViewVector(partialTick).scale(reach));
        Entity operator = null;
        Vec3 operatorHit = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity passenger : siege.getPassengers()) {
            if (!(passenger instanceof LivingEntity living)
                    || passenger instanceof Player
                    || !siege.isOperator(living)) {
                continue;
            }

            AABB pickBox = passenger.getBoundingBox().inflate(OPERATOR_PICK_PADDING);
            Optional<Vec3> hit = pickBox.clip(eye, end);
            if (hit.isEmpty()) {
                continue;
            }

            double distance = eye.distanceToSqr(hit.get());
            if (distance < closestDistance) {
                operator = passenger;
                operatorHit = hit.get();
                closestDistance = distance;
            }
        }

        if (operator != null) {
            minecraft.crosshairPickEntity = operator;
            minecraft.hitResult = new EntityHitResult(operator, operatorHit);
        }
    }
}
