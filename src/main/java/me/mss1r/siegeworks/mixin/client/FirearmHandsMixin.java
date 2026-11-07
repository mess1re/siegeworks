package me.mss1r.siegeworks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.client.firearm.FirearmAnimation;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class FirearmHandsMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void siegeworks$firearmHands(AbstractClientPlayer player, float partialTick, float pitch,
                                        InteractionHand hand, float swing, ItemStack held, float equip,
                                        PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (LadderCarry.isCarrying(player)) {
            ci.cancel(); return;
        }
        if (!(player instanceof LocalPlayer local) || !FirearmAnimation.holds(player)) return;
        if (hand == InteractionHand.MAIN_HAND) {
            if (FirearmAnimation.INSTANCE.renderFirstPerson(local, poses, buffers, light, partialTick)) ci.cancel();
        } else if (player.isUsingItem() || player.getOffhandItem().is(SiegeworksItems.FIREARM_RAMROD.get())) {
            ci.cancel();
        }
        // Ordinary offhand items retain vanilla display, equip and use animations.
        // The enclosing vanilla pass also owns the buffer flush and hand projection.
    }
}
