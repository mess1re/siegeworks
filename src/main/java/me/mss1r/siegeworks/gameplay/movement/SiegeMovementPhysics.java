package me.mss1r.siegeworks.gameplay.movement;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.mss1r.axiomata.collision.StructureTransform;
import me.mss1r.siegeworks.gameplay.collision.SiegeTerrainCollision;
import net.minecraft.world.phys.Vec3;
import java.util.OptionalDouble;


public final class SiegeMovementPhysics {
    private SiegeMovementPhysics() {
    }

    private static void turnAboutPivot(AbstractSiegeEntity siege, float yawDelta, boolean clientPredicted) {
        Vec3 before = siege.towPivotWorldOffset();
        float previousYaw = siege.getYRot();
        StructureTransform previousPose = siege.collisionTransform();
        float yaw = previousYaw + yawDelta;
        if (clientPredicted) {
            siege.applyClientPredictedYaw(yaw);
        } else {
            siege.applyYaw(yaw);
        }

        Vec3 shift = before.subtract(siege.towPivotWorldOffset());
        StructureTransform turnedPose = shifted(siege.collisionTransform(), shift);
        if (!SiegeTerrainCollision.canOccupy(siege, previousPose, turnedPose) || mountsStrike(siege, shift)) {
            if (clientPredicted) {
                siege.applyClientPredictedYaw(previousYaw);
            } else {
                siege.applyYaw(previousYaw);
            }
            return;
        }
        if (shift.lengthSqr() > 1.0E-12D) {
            siege.setPos(siege.getX() + shift.x, siege.getY(), siege.getZ() + shift.z);
        }
    }

    /** Whether turning would swing a hitched animal into a wall. */
    private static boolean mountsStrike(AbstractSiegeEntity siege, Vec3 shift) {
        for (AbstractHorse mount : siege.getTowingMounts()) {
            AABB standing = mount.getBoundingBox().deflate(MOUNT_CLEARANCE);
            if (!mountFits(mount, standing, mountBox(siege, mount, shift))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stops a towed engine where its hitched animals hit a wall. Animals ride the engine, so its own collision ignores
     * them. Each animal is placed on the ground and may only move where it fits or out of a block it's stuck in;
     * against a wall the team slides along it.
     */
    private static Vec3 clampForMounts(AbstractSiegeEntity siege, Vec3 travel) {
        Vec3 flat = new Vec3(travel.x, 0.0D, travel.z);
        for (AbstractHorse mount : siege.getTowingMounts()) {
            if (flat.horizontalDistanceSqr() < 1.0E-12D) {
                break;
            }
            AABB box = mountBox(siege, mount, Vec3.ZERO);
            Vec3 allowed = Vec3.ZERO;
            for (Vec3 way : new Vec3[]{flat, new Vec3(flat.x, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, flat.z)}) {
                Vec3 reach = way.scale(furthestShare(siege, mount, box, way, travel.y));
                if (reach.horizontalDistanceSqr() > allowed.horizontalDistanceSqr()) {
                    allowed = reach;
                }
                if (allowed.horizontalDistanceSqr() >= flat.horizontalDistanceSqr() - 1.0E-12D) {
                    break;
                }
            }
            flat = allowed;
        }
        return new Vec3(flat.x, travel.y, flat.z);
    }

    /** Fraction of a step an animal can take before colliding. */
    private static double furthestShare(AbstractSiegeEntity siege, Entity mount, AABB box, Vec3 way, double rise) {
        if (way.horizontalDistanceSqr() < 1.0E-12D) {
            return 0.0D;
        }
        Vec3 seat = mountSeat(siege, mount, Vec3.ZERO);
        if (mountFits(mount, box, footed(siege, mount, seat.add(way.x, rise, way.z)))) {
            return 1.0D;
        }
        double low = 0.0D;
        double high = 1.0D;
        for (int i = 0; i < MOUNT_SEARCH_STEPS; i++) {
            double middle = (low + high) * 0.5D;
            if (mountFits(mount, box, footed(siege, mount, seat.add(way.x * middle, rise, way.z * middle)))) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** Seat position of a hitched animal for the engine's current pose, offset by the given shift. */
    private static Vec3 mountSeat(AbstractSiegeEntity siege, Entity mount, Vec3 shift) {
        return siege.position().add(shift).add(SiegePassengerPhysics.rotatedSeatOffset(siege, mount));
    }

    /** Body position of a hitched animal for the engine's current pose, offset by the given shift. */
    private static AABB mountBox(AbstractSiegeEntity siege, Entity mount, Vec3 shift) {
        return footed(siege, mount, mountSeat(siege, mount, shift));
    }

    /** An animal's bounding box placed on the ground under a seat. */
    private static AABB footed(AbstractSiegeEntity siege, Entity mount, Vec3 seat) {
        Vec3 feet = SiegePassengerPhysics.mountFooting(siege, mount, seat);
        return mount.getBoundingBox().move(feet.subtract(mount.position())).deflate(MOUNT_CLEARANCE);
    }

    /**
     * Whether an animal can move between positions: into free space, or anywhere that reduces its overlap with blocks
     * it's stuck in.
     */
    private static boolean mountFits(Entity mount, AABB from, AABB to) {
        double inside = intrusion(mount, to);
        return inside <= 0.0D || inside < intrusion(mount, from) - 1.0E-9D;
    }

    /** Volume of the box inside solid blocks. */
    private static double intrusion(Entity mount, AABB box) {
        double volume = 0.0D;
        for (VoxelShape shape : mount.level().getBlockCollisions(mount, box)) {
            for (AABB part : shape.toAabbs()) {
                if (part.intersects(box)) {
                    AABB overlap = part.intersect(box);
                    volume += overlap.getXsize() * overlap.getYsize() * overlap.getZsize();
                }
            }
        }
        return volume;
    }

    private static StructureTransform shifted(StructureTransform pose, Vec3 shift) {
        return new StructureTransform(
                pose.x() + shift.x, pose.y(), pose.z() + shift.z, pose.yawDegrees());
    }

    public static void updateSiegeVelocity(AbstractSiegeEntity abstractSiegeEntity) {
        Entity passenger = abstractSiegeEntity.getMovementControllerPassenger();
        Entity operator = null;
        Player controller = null;

        if (passenger != null) {
            if (passenger instanceof Player player && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = player;
                controller = player;
            } else if (passenger.getFirstPassenger() != null && passenger.getFirstPassenger() instanceof Player passengerRider
                    && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = passenger;
                controller = passengerRider;
            } else if (passenger instanceof LivingEntity
                    && abstractSiegeEntity.shouldPassengerControlMovement(passenger)) {
                operator = passenger;
            }
        }

        float forwardInput = 0.0F;
        float steeringInput = 0.0F;
        boolean debugAutoDriving = operator == null && abstractSiegeEntity.isDebugAutoDriving();
        if (operator != null) {
            if (controller != null) {
                forwardInput = resolveForwardInput(abstractSiegeEntity, controller);
                steeringInput = resolveSteeringInput(abstractSiegeEntity, controller);
            } else {
                forwardInput = abstractSiegeEntity.getMovementInputForward();
                steeringInput = abstractSiegeEntity.getMovementInputSteering();
            }
        } else if (debugAutoDriving) {
            forwardInput = 1.0F;
        } else {
            abstractSiegeEntity.setMovementInput(0.0F, 0.0F);
        }

        double maximumSpeed = 0.0D;
        if (debugAutoDriving) {
            maximumSpeed = abstractSiegeEntity.getDebugAutoDriveVelocity();
        } else if (operator != null) {
            maximumSpeed = abstractSiegeEntity.getVelocity(operator);
        }
        double targetSpeed = maximumSpeed * forwardInput;
        if (targetSpeed < 0.0D) {
            targetSpeed *= abstractSiegeEntity.getReverseSpeedMultiplier();
        }

        double currentSpeed = abstractSiegeEntity.getCurrentDriveSpeed();
        double speedStep = abstractSiegeEntity.getDriveDeceleration(operator);
        if (Math.abs(forwardInput) > 0.01F) {
            if (debugAutoDriving) {
                speedStep = abstractSiegeEntity.getDebugAutoDriveAcceleration();
            } else if (operator != null) {
                speedStep = abstractSiegeEntity.getDriveAcceleration(operator);
            }
        }
        // Slowing down doesn't need a draft team: the engine always coasts to a stop.
        boolean slowing = Math.abs(targetSpeed) < Math.abs(currentSpeed) || targetSpeed * currentSpeed < 0.0D;
        if (slowing) {
            speedStep = Math.max(speedStep, abstractSiegeEntity.getRollingDeceleration());
        }
        currentSpeed = approach(currentSpeed, targetSpeed, speedStep);
        if (Math.abs(currentSpeed) < 1.0E-5D) {
            currentSpeed = 0.0D;
        }
        abstractSiegeEntity.setCurrentDriveSpeed(currentSpeed);

        boolean moving = Math.abs(currentSpeed) > 1.0E-5D;
        boolean pivoting = !moving && abstractSiegeEntity.canPivotInPlace(operator);
        float steeringSpeed = 0.0F;
        if (operator != null && moving) {
            steeringSpeed = scaleSteeringForTravel(
                    abstractSiegeEntity.getSteeringSpeedDegrees(operator), currentSpeed,
                    maximumSpeed, abstractSiegeEntity.getReverseSpeedMultiplier());
        } else if (operator != null && pivoting) {
            steeringSpeed = abstractSiegeEntity.getPivotSteeringSpeedDegrees(operator);
        }
        abstractSiegeEntity.setCurrentSteeringSpeed(steeringSpeed);

        OptionalDouble driverYaw = controller != null ? abstractSiegeEntity.freshDriverYaw() : OptionalDouble.empty();
        if (driverYaw.isPresent() && (moving || pivoting) && steeringSpeed > 0.0F) {
            // Turn toward the client's reported yaw at steering speed, with extra margin to catch up after a late
            // packet.
            float yawDelta = Mth.wrapDegrees((float) driverYaw.getAsDouble() - abstractSiegeEntity.getYRot());
            float limit = steeringSpeed * DRIVER_YAW_CATCH_UP;
            if (Math.abs(yawDelta) > 1.0E-3F) {
                turnAboutPivot(abstractSiegeEntity, Mth.clamp(yawDelta, -limit, limit), false);
            }
        } else if (Math.abs(steeringInput) > 0.01F && (moving || pivoting) && operator != null) {
            float travelDirection = moving && currentSpeed < 0.0D ? -1.0F : 1.0F;
            float yawDelta = -steeringInput * steeringSpeed * travelDirection;
            turnAboutPivot(abstractSiegeEntity, yawDelta, false);
        }

        Vec3 horizontalVelocity = calculateMovementVector(
                currentSpeed, abstractSiegeEntity.getVisualRotationYInDegrees());
        Vec3 velocity = abstractSiegeEntity.getDeltaMovement();

        // A cached onGround flag is not support: after leaving a ledge it remains
        // true until a downward move asks the collision solver again.
        double verticalVelocity = velocity.y - 0.08D;

        Vec3 newVelocity = new Vec3(horizontalVelocity.x, verticalVelocity, horizontalVelocity.z);
        Vec3 travel = clampForMounts(abstractSiegeEntity,
                SiegeTerrainCollision.clampMovement(abstractSiegeEntity, newVelocity));

        abstractSiegeEntity.setDeltaMovement(travel);
        abstractSiegeEntity.move(MoverType.SELF, travel);
    }

    public static float scaleSteeringForTravel(float maximumTurnDegrees, double currentSpeed,
                                               double maximumForwardSpeed, double reverseMultiplier) {
        double directionalMaximum = Math.abs(maximumForwardSpeed);
        if (currentSpeed < 0.0D) {
            directionalMaximum *= Mth.clamp(reverseMultiplier, 0.0D, 1.0D);
        }
        if (directionalMaximum <= 1.0E-8D) {
            return 0.0F;
        }
        double speedFraction = Mth.clamp(Math.abs(currentSpeed) / directionalMaximum, 0.0D, 1.0D);
        return (float) (Math.max(0.0F, maximumTurnDegrees) * speedFraction);
    }

    /** Ticks after steering stops before the client's engine yaw is eased toward the server's. */
    private static final int STEERING_SETTLE_TICKS = 10;
    /**
     * Fraction of the remaining yaw difference closed per tick after that: a degree or two over half a second, without
     * a jolt.
     */
    private static final float STEERING_CORRECTION_SHARE = 0.15F;
    private static final float LEAST_STEERING_CORRECTION = 0.05F;
    /** How much faster than steering the server may turn to catch up with a late yaw. */
    private static final float DRIVER_YAW_CATCH_UP = 1.5F;
    /** How far into a hitched animal's box the ground may reach. */
    private static final double MOUNT_CLEARANCE = 1.0E-3D;
    /** Bisection steps used to find how far a team can move before an animal hits a wall. */
    private static final int MOUNT_SEARCH_STEPS = 10;

    public static void updateClientSteering(AbstractSiegeEntity abstractSiegeEntity) {
        float steeringInput = abstractSiegeEntity.getMovementInputSteering();
        double currentSpeed = abstractSiegeEntity.getCurrentDriveSpeed();
        float steeringSpeed = abstractSiegeEntity.getCurrentSteeringSpeed();
        Entity operator = abstractSiegeEntity.getMovementControllerPassenger();

        boolean moving = Math.abs(currentSpeed) > 1.0E-5D;
        boolean pivoting = !moving && abstractSiegeEntity.canPivotInPlace(operator);
        if (Math.abs(steeringInput) > 0.01F && (moving || pivoting) && steeringSpeed > 0.0F) {
            float travelDirection = moving && currentSpeed < 0.0D ? -1.0F : 1.0F;
            float yawDelta = -steeringInput * steeringSpeed * travelDirection;
            turnAboutPivot(abstractSiegeEntity, yawDelta, true);
            abstractSiegeEntity.setClientSteeringIdleTicks(0);
            return;
        }

        // The server applies the same steering a round trip later; correcting toward it before it catches up would
        // swing the engine back.
        int idle = abstractSiegeEntity.getClientSteeringIdleTicks() + 1;
        abstractSiegeEntity.setClientSteeringIdleTicks(idle);
        if (idle <= STEERING_SETTLE_TICKS) {
            return;
        }
        float yawDelta = Mth.wrapDegrees(abstractSiegeEntity.getTrackedYaw() - abstractSiegeEntity.getYRot());
        if (Math.abs(yawDelta) < LEAST_STEERING_CORRECTION) {
            return;
        }
        // Rotate about the steering pivot, or the engine slides sideways on its wheels.
        turnAboutPivot(abstractSiegeEntity, yawDelta * STEERING_CORRECTION_SHARE, true);
    }

    private static float resolveForwardInput(AbstractSiegeEntity abstractSiegeEntity, Player player) {
        float vanillaInput = Mth.clamp(player.zza, -1.0F, 1.0F);
        if (Math.abs(vanillaInput) > 0.01F) {
            return vanillaInput;
        }
        return abstractSiegeEntity.getMovementInputForward();
    }

    private static float resolveSteeringInput(AbstractSiegeEntity abstractSiegeEntity, Player player) {
        float vanillaInput = Mth.clamp(player.xxa, -1.0F, 1.0F);
        if (Math.abs(vanillaInput) > 0.01F) {
            return vanillaInput;
        }
        return abstractSiegeEntity.getMovementInputSteering();
    }

    private static double approach(double current, double target, double step) {
        double safeStep = Math.max(0.0D, step);
        if (current < target) {
            return Math.min(current + safeStep, target);
        }
        return Math.max(current - safeStep, target);
    }

    private static Vec3 calculateMovementVector(double forward, double yaw) {
        double yawRad = Math.toRadians(yaw);
        double x = -Math.sin(yawRad) * forward;
        double z = Math.cos(yawRad) * forward;
        return new Vec3(x, 0, z);
    }

    public static void updateWheelRotation(AbstractSiegeEntity abstractSiegeEntity) {
        abstractSiegeEntity.updateDifferentialWheelRotation();
        double directionalDistance = abstractSiegeEntity.level().isClientSide
                ? abstractSiegeEntity.updateDriveAnimationAndGetForwardDistance()
                : abstractSiegeEntity.updateVisualTravelAndGetForwardDistance();
        abstractSiegeEntity.wheelRotation += (float) (-directionalDistance * 72);
        abstractSiegeEntity.wheelRotation %= 360f;

        if (abstractSiegeEntity.wheelRotation < 0) {
            abstractSiegeEntity.wheelRotation += 360f;
        }
    }
}
