package me.mss1r.siegeworks.testmixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.gametest.ManualAimFrameProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(value = GeoEntityRenderer.class, remap = false)
public abstract class ProbeGeoRenderMixin {
    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void siegeworks$recordWorldModel(Entity entity, float yaw, float partialTick,
                                            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ManualAimFrameProbe.recordModel(entity);
    }
}
