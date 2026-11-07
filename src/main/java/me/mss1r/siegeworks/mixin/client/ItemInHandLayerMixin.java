package me.mss1r.siegeworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.client.ladder.LadderBraceClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lays a braced pole along the shaft line of the bracing pose. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void siegeworks$firearm(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                    HumanoidArm arm, PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (me.mss1r.siegeworks.gameplay.ladder.LadderCarry.isCarrying(entity)) {
            ci.cancel(); return;
        }
        if (me.mss1r.siegeworks.client.firearm.FirearmAnimation.INSTANCE.renderHeld(entity, arm, poses, buffers,
                light, me.mss1r.siegeworks.client.firearm.FirearmClient.partialTick())) ci.cancel();
    }
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void siegeworks$alignBracedPole(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                           HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int light,
                                           CallbackInfo ci) {
        LadderBraceClient.alignHeldItem(entity, stack, context, arm, poseStack);
    }
}
