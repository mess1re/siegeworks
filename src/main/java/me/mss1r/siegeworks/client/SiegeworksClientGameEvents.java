package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.aim.SiegeAimController;
import me.mss1r.siegeworks.client.aim.SiegeAimView;
import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.network.BoltThrowerFireC2SPayload;
import me.mss1r.siegeworks.network.OnagerFireC2SPayload;
import me.mss1r.siegeworks.network.MantletActionC2SPayload;
import me.mss1r.siegeworks.network.SiegeMovementC2SPayload;
import me.mss1r.siegeworks.network.SiegeYawC2SPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
*///?} else {
import net.neoforged.api.distmarker.Dist;
//?}
//? if forge {
/*import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if forge {
/*import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
*///?} else {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if forge {
/*import net.minecraftforge.client.event.MovementInputUpdateEvent;
*///?} else {
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
//?}
//? if forge {
/*import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
*///?} else {
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
//?}
//? if forge {
/*import net.minecraftforge.client.event.RenderHandEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RenderHandEvent;
//?}
import me.mss1r.siegeworks.network.SiegeworksNetworking;

@SuppressWarnings("removal")
//? if forge {
/*@EventBusSubscriber(modid = Siegeworks.MOD_ID, bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
*///?} else {
@EventBusSubscriber(modid = Siegeworks.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
//?}
public final class SiegeworksClientGameEvents {
    private static final int MANTLET_DOUBLE_SHIFT_TICKS = 8;
    private static boolean jumpHeldLastTick;
    private static boolean sneakHeldLastTick;
    private static boolean sprintHeldLastTick;
    private static int previousMantletSneakEntityId = -1;
    private static int previousMantletSneakTick = -1;

    private SiegeworksClientGameEvents() {
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof MantletEntity) {
            event.getInput().shiftKeyDown = false;
        }

        AbstractSiegeEntity controlledSiege = getControlledSiege(vehicle, player);
        if (controlledSiege != null) {
            float forward = Math.max(-1.0F, Math.min(1.0F, event.getInput().forwardImpulse));
            float steering = Math.max(-1.0F, Math.min(1.0F, event.getInput().leftImpulse));
            controlledSiege.setMovementInput(forward, steering);
            SiegeworksNetworking.sendToServer(new SiegeMovementC2SPayload(forward, steering, controlledSiege.getYRot()));
        }
    }

    public static void onClientTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            resetHeldKeys();
            return;
        }

        SiegeAimController.tick(player);
        sendAimUpdate(player);

        boolean jumpDown = minecraft.options.keyJump.isDown();
        boolean sneakDown = minecraft.options.keyShift.isDown();
        boolean sprintDown = minecraft.options.keySprint.isDown();
        Entity vehicle = player.getVehicle();

        if (vehicle instanceof MantletEntity mantlet) {
            int entityId = mantlet.getId();
            int tick = player.tickCount;
            if (jumpDown && !sneakDown) {
                SiegeworksNetworking.sendToServer(new MantletActionC2SPayload(
                        entityId, MantletEntity.ACTION_OPEN_FLAP));
            }
            if (sneakDown) {
                SiegeworksNetworking.sendToServer(new MantletActionC2SPayload(
                        entityId, MantletEntity.ACTION_CLOSE_FLAP));
                if (!sneakHeldLastTick) {
                    if (previousMantletSneakEntityId == entityId
                            && tick - previousMantletSneakTick <= MANTLET_DOUBLE_SHIFT_TICKS) {
                        SiegeworksNetworking.sendToServer(new MantletActionC2SPayload(
                                entityId, MantletEntity.ACTION_DISMOUNT));
                        clearMantletSneakTap();
                    } else {
                        previousMantletSneakEntityId = entityId;
                        previousMantletSneakTick = tick;
                    }
                }
            }

            jumpHeldLastTick = jumpDown;
            sneakHeldLastTick = sneakDown;
            sprintHeldLastTick = sprintDown;
            return;
        }

        clearMantletSneakTapIfExpired(player.tickCount);

        if (jumpDown && !jumpHeldLastTick) {
            if (vehicle instanceof AbstractBoltThrowerEntity) {
                SiegeworksNetworking.sendToServer(BoltThrowerFireC2SPayload.INSTANCE);
            } else if (vehicle instanceof OnagerEntity) {
                SiegeworksNetworking.sendToServer(OnagerFireC2SPayload.INSTANCE);
            }
        }

        jumpHeldLastTick = jumpDown;
        sneakHeldLastTick = sneakDown;
        sprintHeldLastTick = sprintDown;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        // Hands are raised out of view while carrying a ladder.
        if (player != null && (player.getVehicle() instanceof AbstractBoltThrowerEntity
                || LadderCarry.isCarrying(player))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    //? if forge {
    /*public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
    *///?} else {
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Pre event) {
    //?}
        Minecraft minecraft = Minecraft.getInstance();
        AbstractSiegeEntity siege = minecraft.player == null
                ? null
                : SiegeAimView.findControlledSiege(minecraft.player);
        //? if forge {
        /*boolean crosshair = event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id());
        *///?} else {
        boolean crosshair = event.getName().equals(VanillaGuiLayers.CROSSHAIR);
        //?}
        if (crosshair
                && minecraft.player != null && siege != null
                && !SiegeAimController.isFreeLookActive(minecraft.player, siege)) {
            event.setCanceled(true);
        }
    }

    private static void sendAimUpdate(LocalPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractSiegeEntity siege) {
            if (!siege.shouldPassengerControlRotation(player)) {
                return;
            }
            if (SiegeAimController.isFreeLookActive(player, siege)) {
                return;
            }
            float requestedYaw = player.getYRot();
            float requestedPitch = player.getXRot();
            if (siege instanceof AbstractBoltThrowerEntity boltThrower) {
                if (siege.usesIndependentAim()) {
                    requestedYaw = boltThrower.getClientAimTargetYaw();
                }
                if (siege.usesIndependentPitchAim()) {
                    requestedPitch = boltThrower.getClientAimTargetPitch();
                }
            }
            if (!siege.usesIndependentPitchAim()) {
                siege.setTrackedPitch(player.getXRot());
            }
            SiegeworksNetworking.sendToServer(new SiegeYawC2SPayload(requestedYaw, requestedPitch));
        } else if (AbstractSiegeEntity.isDraftMount(vehicle) && vehicle.getVehicle() instanceof AbstractSiegeEntity) {
            SiegeworksNetworking.sendToServer(new SiegeYawC2SPayload(player.getYRot(), player.getXRot()));
        }
    }

    private static AbstractSiegeEntity getControlledSiege(Entity vehicle, LocalPlayer player) {
        if (vehicle instanceof AbstractSiegeEntity siege && siege.shouldPassengerControlMovement(player)) {
            return siege;
        }
        if (vehicle instanceof AbstractHorse horse && horse.getVehicle() instanceof AbstractSiegeEntity siege
                && siege.shouldPassengerControlMovement(horse)) {
            return siege;
        }
        return null;
    }

    private static void resetHeldKeys() {
        jumpHeldLastTick = false;
        sneakHeldLastTick = false;
        sprintHeldLastTick = false;
        clearMantletSneakTap();
    }

    private static void clearMantletSneakTapIfExpired(int tick) {
        if (previousMantletSneakTick >= 0
                && tick - previousMantletSneakTick > MANTLET_DOUBLE_SHIFT_TICKS) {
            clearMantletSneakTap();
        }
    }

    private static void clearMantletSneakTap() {
        previousMantletSneakEntityId = -1;
        previousMantletSneakTick = -1;
    }
}
