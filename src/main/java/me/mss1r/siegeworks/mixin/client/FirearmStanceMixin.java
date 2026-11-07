package me.mss1r.siegeworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.client.firearm.FirearmClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntityRenderer.class)
public abstract class FirearmStanceMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "STORE", ordinal = 0), ordinal = 2)
    private float siegeworks$aimWithBody(float bodyYaw, LivingEntity entity, float yaw, float partialTick,
                                         PoseStack poses, MultiBufferSource buffers, int light) {
        return FirearmClient.renderedBodyYaw(entity, partialTick, bodyYaw);
    }
}
