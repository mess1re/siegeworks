package me.mss1r.siegeworks.gameplay.aiming;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SiegeAimingController {
    private static final String TAG_TRACKED_YAW = "TrackedYaw";
    private static final String TAG_TRACKED_PITCH = "TrackedPitch";
    private final AbstractSiegeEntity siege;
    private final Host host;
    private boolean hasPassengerYawTarget;
    private float passengerYawTarget;
    private boolean hasPassengerPitchTarget;
    private float passengerPitchTarget;
    private boolean clientPitchInitialized;
    private float clientPitchTarget;
    private float clientPitch;
    private boolean renderedInitialized;
    private float previousRenderedYaw;
    private float previousRenderedPitch;
    private float renderedYaw;
    private float renderedPitch;
    private float lastRenderedTargetYaw;
    private float lastRenderedTargetPitch;

    public SiegeAimingController(AbstractSiegeEntity siege, Host host) {
        this.siege = siege;
        this.host = host;
    }

    public void save(CompoundTag tag) {
        tag.putFloat(TAG_TRACKED_YAW, trackedYaw());
        tag.putFloat(TAG_TRACKED_PITCH, trackedPitch());
    }

    public void load(CompoundTag tag) {
        if (tag.contains(TAG_TRACKED_YAW)) {
            applyYaw(tag.getFloat(TAG_TRACKED_YAW));
        }
        if (tag.contains(TAG_TRACKED_PITCH)) {
            setTrackedPitch(tag.getFloat(TAG_TRACKED_PITCH));
            siege.setXRot(trackedPitch());
            siege.lastRiderPitch = trackedPitch();
        }
    }

    public void setTrackedYaw(float yaw) {
        host.setTrackedYaw(Mth.wrapDegrees(yaw));
    }

    public float trackedYaw() {
        return host.trackedYaw();
    }

    public void setTrackedPitch(float pitch) {
        host.setTrackedPitch(Mth.clamp(pitch, host.minimumPitch(), host.maximumPitch()));
    }

    public float trackedPitch() {
        return host.trackedPitch();
    }

    public void setPassengerTargetYaw(float yaw) {
        passengerYawTarget = Mth.wrapDegrees(yaw);
        hasPassengerYawTarget = true;
    }

    public void setPassengerTargetPitch(float pitch) {
        passengerPitchTarget = Mth.clamp(pitch, host.minimumPitch(), host.maximumPitch());
        hasPassengerPitchTarget = true;
    }

    public void turnTowardsYaw(float targetYaw) {
        float currentYaw = Mth.wrapDegrees(siege.getYRot());
        float delta = Mth.wrapDegrees(Mth.wrapDegrees(targetYaw) - currentYaw);
        applyYaw(currentYaw + Mth.clamp(delta, -host.yawTurnSpeed(), host.yawTurnSpeed()));
    }

    public void applyYaw(float yaw) {
        float normalizedYaw = Mth.wrapDegrees(yaw);
        setTrackedYaw(normalizedYaw);
        siege.setYRot(normalizedYaw);
        siege.setYHeadRot(normalizedYaw);
        siege.setYBodyRot(normalizedYaw);
        siege.lastRiderYaw = normalizedYaw;
    }

    public void turnTowardsPitch(float targetPitch) {
        boolean predicted = siege.level().isClientSide && host.independentPitch();
        float currentPitch = predicted ? clientPitch() : trackedPitch();
        float delta = Mth.clamp(targetPitch, host.minimumPitch(), host.maximumPitch()) - currentPitch;
        float next = host.resolvePitch(currentPitch,
                currentPitch + Mth.clamp(delta, -host.pitchTurnSpeed(), host.pitchTurnSpeed()));
        if (predicted) {
            clientPitch = next;
        } else {
            setTrackedPitch(next);
            siege.setXRot(trackedPitch());
        }
        siege.lastRiderPitch = next;
    }

    /** The same bounded mouse target and per-tick prediction for every elevation mechanism. */
    public void applyClientPitchInput(float delta) {
        if (!siege.level().isClientSide || !host.independentPitch()) return;
        float current = clientPitch();
        float pending = clientPitchTarget - current + delta;
        clientPitchTarget = host.resolvePitch(current,
                current + Mth.clamp(pending, -host.pitchTurnSpeed(), host.pitchTurnSpeed()));
    }

    public float clientPitchTarget() {
        initializeClientPitch();
        return clientPitchTarget;
    }

    public float clientPitch() {
        initializeClientPitch();
        return clientPitch;
    }

    private void initializeClientPitch() {
        if (clientPitchInitialized) return;
        clientPitch = trackedPitch();
        clientPitchTarget = clientPitch;
        clientPitchInitialized = true;
    }

    /** An unoccupied mechanism may animate toward rest; otherwise it follows the authoritative angle. */
    public void syncClientPitch() {
        if (!host.independentPitch()) return;
        initializeClientPitch();
        if (!host.animatesUnoccupiedPitch()
                && (host.towed() || controllingPlayer().filter(Player::isLocalPlayer).isEmpty())) {
            clientPitch = trackedPitch();
            clientPitchTarget = clientPitch;
        }
    }

    public void setClientPitch(float pitch) {
        initializeClientPitch();
        clientPitch = pitch;
        clientPitchTarget = pitch;
    }

    public void passengerRemoved() {
        hasPassengerPitchTarget = false;
        if (siege.level().isClientSide && clientPitchInitialized) clientPitchTarget = clientPitch;
    }

    /**
     * Interpolates from the aim rendered last, not the current one: server aim updates arrive before the tick starts,
     * and starting from them would freeze the aim until the next update.
     */
    public void capturePreviousRenderState() {
        float currentYaw = host.aimingRenderYaw();
        float currentPitch = host.aimingRenderPitch();
        if (!renderedInitialized) {
            renderedYaw = currentYaw;
            renderedPitch = currentPitch;
            lastRenderedTargetYaw = currentYaw;
            lastRenderedTargetPitch = currentPitch;
            renderedInitialized = true;
        }
        previousRenderedYaw = lastRenderedTargetYaw;
        previousRenderedPitch = lastRenderedTargetPitch;
    }

    public void updateRendered(float partialTick) {
        float targetYaw = host.aimingRenderYaw();
        float targetPitch = host.aimingRenderPitch();
        if (!renderedInitialized) {
            previousRenderedYaw = targetYaw;
            previousRenderedPitch = targetPitch;
            renderedYaw = targetYaw;
            renderedPitch = targetPitch;
            lastRenderedTargetYaw = targetYaw;
            lastRenderedTargetPitch = targetPitch;
            renderedInitialized = true;
            return;
        }

        lastRenderedTargetYaw = targetYaw;
        lastRenderedTargetPitch = targetPitch;
        float interpolation = Mth.clamp(partialTick, 0.0F, 1.0F);
        renderedYaw = Mth.wrapDegrees(previousRenderedYaw
                + Mth.wrapDegrees(targetYaw - previousRenderedYaw) * interpolation);
        renderedPitch = Mth.lerp(interpolation, previousRenderedPitch, targetPitch);
    }

    public float renderedYaw() {
        if (!siege.isFullyBuilt()) {
            return host.aimingRenderYaw();
        }
        return renderedInitialized ? renderedYaw : host.aimingRenderYaw();
    }

    public float renderedPitch() {
        if (!siege.isFullyBuilt()) {
            return host.aimingRenderPitch();
        }
        return renderedInitialized ? renderedPitch : host.aimingRenderPitch();
    }

    public void updateFromController() {
        if (host.towed()) {
            clearPassengerTargets();
            return;
        }

        Optional<Player> controller = controllingPlayer();
        if (controller.isEmpty()) {
            clearPassengerTargets();
            return;
        }

        Player player = controller.get();
        if (host.independentYaw()) {
            host.requestTurnTowardsYaw(hasPassengerYawTarget
                    ? passengerYawTarget
                    : host.passengerTargetYaw(player));
        }
        if (host.independentPitch()) {
            host.requestTurnTowardsPitch(hasPassengerPitchTarget
                    ? passengerPitchTarget
                    : host.passengerTargetPitch(player));
        } else {
            setTrackedPitch(player.getXRot());
            siege.setXRot(trackedPitch());
            siege.lastRiderPitch = trackedPitch();
        }
    }

    public void updateClientEntityRotation() {
        float targetYaw = trackedYaw();
        if (host.independentYaw()) {
            // The turret may turn independently; the chassis must not follow
            // LivingEntity's automatic head/body yaw adjustment.
            siege.setYBodyRot(siege.getYRot());
            siege.setYHeadRot(targetYaw);
            siege.lastRiderYaw = targetYaw;
            float pitch = trackedPitch();
            siege.setXRot(pitch);
            siege.lastRiderPitch = pitch;
            return;
        }

        float currentYaw = Mth.wrapDegrees(siege.getYRot());
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float yaw;
        if (siege.tickCount < 2 || Math.abs(yawDelta) > 45.0F) {
            yaw = targetYaw;
            siege.yRotO = yaw;
        } else {
            float maxStep = Math.max(0.05F, host.yawTurnSpeed() * 1.35F);
            yaw = Mth.wrapDegrees(currentYaw + Mth.clamp(yawDelta, -maxStep, maxStep));
        }

        siege.setYRot(yaw);
        siege.setYHeadRot(yaw);
        siege.setYBodyRot(yaw);
        siege.lastRiderYaw = yaw;
        float pitch = trackedPitch();
        siege.setXRot(pitch);
        siege.lastRiderPitch = pitch;
    }

    public void updateServerEntityRotation() {
        if (host.independentYaw()) {
            siege.setYHeadRot(trackedYaw());
            siege.setYBodyRot(siege.getYRot());
            siege.lastRiderYaw = trackedYaw();
        } else {
            siege.setYHeadRot(trackedYaw());
            setTrackedYaw(siege.getYRot());
            siege.setYRot(siege.getYRot());
            siege.setYHeadRot(siege.getYRot());
            siege.setYBodyRot(siege.getYRot());
            siege.lastRiderYaw = siege.getYRot();
        }

        setTrackedPitch(siege.getXRot());
        siege.setXRot(siege.getXRot());
        siege.lastRiderPitch = siege.getXRot();
    }

    public boolean hasLocalPlayerControl() {
        if (!siege.level().isClientSide) {
            return false;
        }
        for (Entity passenger : siege.getPassengers()) {
            boolean controls = host.passengerControlsRotation(passenger)
                    || host.passengerControlsMovement(passenger);
            if (!controls) {
                continue;
            }
            if (passenger instanceof Player player && player.isLocalPlayer()) {
                return true;
            }
            if (passenger.getFirstPassenger() instanceof Player player && player.isLocalPlayer()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRemoteDirectOperator() {
        LivingEntity operator = siege.getControllingPassenger();
        return operator != null && !(operator instanceof Player)
                && host.passengerControlsRotation(operator);
    }

    public void applyClientPredictedYaw(float yaw) {
        float normalizedYaw = Mth.wrapDegrees(yaw);
        siege.setYRot(normalizedYaw);
        siege.setYHeadRot(normalizedYaw);
        siege.setYBodyRot(normalizedYaw);
        siege.lastRiderYaw = normalizedYaw;
    }

    private Optional<Player> controllingPlayer() {
        for (Entity passenger : siege.getPassengers()) {
            if (passenger instanceof Player player && host.passengerControlsRotation(passenger)) {
                return Optional.of(player);
            }
            if (passenger.getFirstPassenger() instanceof Player player
                    && host.passengerControlsRotation(passenger)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    private void clearPassengerTargets() {
        hasPassengerYawTarget = false;
        hasPassengerPitchTarget = false;
    }

    public interface Host {
        float trackedYaw();
        void setTrackedYaw(float yaw);
        float trackedPitch();
        void setTrackedPitch(float pitch);
        float minimumPitch();
        float maximumPitch();
        float yawTurnSpeed();
        float pitchTurnSpeed();
        boolean independentYaw();
        boolean independentPitch();
        float resolvePitch(float current, float requested);
        boolean animatesUnoccupiedPitch();
        boolean towed();
        float aimingRenderYaw();
        float aimingRenderPitch();
        float passengerTargetYaw(Player player);
        float passengerTargetPitch(Player player);
        void requestTurnTowardsYaw(float yaw);
        void requestTurnTowardsPitch(float pitch);
        boolean passengerControlsRotation(Entity passenger);
        boolean passengerControlsMovement(Entity passenger);
    }
}
