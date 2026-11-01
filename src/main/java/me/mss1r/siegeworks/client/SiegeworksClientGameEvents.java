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
import me.mss1r.siegeworks.network.SiegeworksNetworking;

@SuppressWarnings("removal")
public final class SiegeworksClientGameEvents {
    private static final int MANTLET_DOUBLE_SHIFT_TICKS = 8;
    private static boolean jumpHeldLastTick;
    private static boolean sneakHeldLastTick;
    private static boolean sprintHeldLastTick;
    private static int previousMantletSneakEntityId = -1;
    private static int previousMantletSneakTick = -1;

    private SiegeworksClientGameEvents() {
    }

    public static void updateMovement(LocalPlayer player, net.minecraft.client.player.Input input) {

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof MantletEntity) {
            input.shiftKeyDown = false;
        }

        AbstractSiegeEntity controlledSiege = getControlledSiege(vehicle, player);
        if (controlledSiege != null) {
            float forward = Math.max(-1.0F, Math.min(1.0F, input.forwardImpulse));
            float steering = Math.max(-1.0F, Math.min(1.0F, input.leftImpulse));
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

    public static boolean hideHands() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && (player.getVehicle() instanceof AbstractBoltThrowerEntity
                || LadderCarry.isCarrying(player));
    }

    public static boolean hideCrosshair() {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractSiegeEntity siege = minecraft.player == null ? null : SiegeAimView.findControlledSiege(minecraft.player);
        return minecraft.player != null && siege != null
                && !SiegeAimController.isFreeLookActive(minecraft.player, siege);
    }

    public static boolean hideVehicleHealth() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof AbstractSiegeEntity;
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
            }
            if (siege.usesIndependentPitchAim()) {
                requestedPitch = siege.getClientAimTargetPitch();
            } else {
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
