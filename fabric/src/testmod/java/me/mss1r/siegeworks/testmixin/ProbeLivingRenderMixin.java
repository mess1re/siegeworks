package me.mss1r.siegeworks.testmixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.gametest.DraftTeamClientProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Samples the posed native model; constructing another model would miss gait regressions. */
@Mixin(LivingEntityRenderer.class)
public abstract class ProbeLivingRenderMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void siegeworks$recordPose(LivingEntity entity, float yaw, float partialTick,
                                     PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        DraftTeamClientProbe.rendered(entity, ((LivingEntityRenderer<?, ?>) (Object) this).getModel(), partialTick);
    }
}
