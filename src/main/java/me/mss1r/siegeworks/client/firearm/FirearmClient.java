package me.mss1r.siegeworks.client.firearm;

import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmLoad;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.network.FirearmFireC2SPayload;
import me.mss1r.siegeworks.network.SiegeworksNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public final class FirearmClient {
    private static boolean jumpHeld;
    private static float zoom, previousZoom;
    private static boolean wasAiming;
    private FirearmClient() {}

    public static float partialTick() {
        //? if <1.21 {
        /*return Minecraft.getInstance().getFrameTime();
        *///?} else {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        //?}
    }

    public static float zoom(float partialTick) { return net.minecraft.util.Mth.lerp(partialTick,previousZoom,zoom); }

    public static boolean aiming(LivingEntity entity) {
        if (me.mss1r.siegeworks.gameplay.ladder.LadderCarry.isCarrying(entity) || !entity.isUsingItem() || !(entity.getUseItem().getItem() instanceof FirearmItem)) return false;
        String action = MinecraftVersionCompat.customData(entity.getUseItem()).getString("FirearmAction");
        return action.equals("AIM") || action.equals("FIRE");
    }

    public static float renderedBodyYaw(LivingEntity entity, float partialTick, float vanillaYaw) {
        if (entity.isPassenger() || !aiming(entity) || !FirearmAnimation.holds(entity)) return vanillaYaw;
        return Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
    }

    public static void tick(Minecraft client) {
        FirearmAnimation.INSTANCE.discardInactivePlayback();
        boolean enteringAim = client.player != null && client.screen == null
                && client.player.isUsingItem()
                && client.player.getUseItem().getItem() instanceof FirearmItem
                && MinecraftVersionCompat.customData(client.player.getUseItem()).getString("FirearmAction").equals("AIM");
        if (enteringAim && !wasAiming) {
            var gun = (FirearmItem) client.player.getUseItem().getItem();
            Component hint = FirearmActions.fireRequirement(client.player, gun)
                    == FirearmLoad.FireRequirement.NONE
                    ? Component.translatable("firearm.siegeworks.fire_key", client.options.keyJump.getTranslatedKeyMessage())
                    : FirearmActions.nextActionHint(client.player, gun);
            client.gui.setOverlayMessage(hint, false);
        }
        wasAiming = enteringAim;
        previousZoom=zoom;
        boolean aim=client.player!=null && client.screen==null && aiming(client.player);
        zoom=net.minecraft.util.Mth.approach(zoom,aim ? 1 : 0,aim ? .08F : .16F);
        boolean jump = client.options.keyJump.isDown();
        if (client.player != null && client.screen == null && aiming(client.player) && jump && !jumpHeld) {
            SiegeworksNetworking.sendToServer(FirearmFireC2SPayload.INSTANCE);
        }
        jumpHeld = jump;
    }
}
