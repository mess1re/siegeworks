package me.mss1r.siegeworks.mixin.client;

import me.mss1r.siegeworks.client.aim.SiegeAimController;
import me.mss1r.siegeworks.client.aim.SiegeAimView;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Unique private boolean siegeworks$capturingSiegeAim;
    @Unique private float siegeworks$yawBeforeAimInput;
    @Unique private float siegeworks$pitchBeforeAimInput;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void siegeworks$captureSiegeAimInput(CallbackInfo ci) {
        LocalPlayer player = minecraft.player;
        siegeworks$capturingSiegeAim = player != null
                && SiegeAimView.findDirectlyRiddenSiege(player) != null;
        if (!siegeworks$capturingSiegeAim) {
            return;
        }

        siegeworks$yawBeforeAimInput = player.getYRot();
        siegeworks$pitchBeforeAimInput = player.getXRot();
    }

    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void siegeworks$applySiegeAimInput(CallbackInfo ci) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        AbstractSiegeEntity siege = SiegeAimView.findControlledSiege(player);
        if (siege == null) {
            AbstractSiegeEntity riddenSiege = SiegeAimView.findDirectlyRiddenSiege(player);
            if (riddenSiege != null) {
                SiegePassengerPhysics.clampView(riddenSiege, player, player,
                        siegeworks$capturingSiegeAim ? siegeworks$yawBeforeAimInput : player.yRotO);
            }
            return;
        }
        if (!siegeworks$capturingSiegeAim) {
            return;
        }

        float yawDelta = Mth.wrapDegrees(player.getYRot() - siegeworks$yawBeforeAimInput);
        float pitchDelta = player.getXRot() - siegeworks$pitchBeforeAimInput;
        SiegeAimController.applyAimInput(player, siege, yawDelta, pitchDelta);
    }
}
