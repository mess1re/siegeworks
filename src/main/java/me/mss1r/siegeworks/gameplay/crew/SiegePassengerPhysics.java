package me.mss1r.siegeworks.gameplay.crew;

import com.google.common.collect.MapMaker;
import java.util.Map;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SiegePassengerPhysics {
    /** How far a hitched animal's feet may sink into or float above the ground. */
    private static final double FOOTING_INSET = 1.0E-3D;
    private record Descent(long tick, double startY, double velocity) {}
    private static final Map<Entity, Descent> DESCENTS = new MapMaker().weakKeys().makeMap();

    private static Descent descent(Entity mount) {
        long tick = mount.level().getGameTime();
        Descent previous = DESCENTS.get(mount);
        if (previous != null && previous.tick == tick) {
            return previous;
        }
        double velocity = previous != null && previous.tick == tick - 1 ? previous.velocity : 0;
        return new Descent(tick, mount.getY(), Math.max(-3.92D, velocity - 0.08D));
    }

    /** Commit suspension state only when positioning a passenger, never during clearance probes. */
    public static Vec3 positionMount(AbstractSiegeEntity siege, Entity mount, Vec3 seat) {
        Descent next = descent(mount);
        Vec3 feet = mountFooting(siege, mount, seat);
        DESCENTS.put(mount, new Descent(next.tick, Math.max(next.startY, feet.y),
                feet.y < next.startY - FOOTING_INSET ? next.velocity : 0));
        return feet;
    }

    private static Vec3 descendTo(Entity mount, Vec3 target) {
        Descent state = descent(mount);
        if (target.y >= state.startY) {
            return target;
        }
        return new Vec3(target.x, Math.max(target.y, state.startY + state.velocity), target.z);
    }

    private SiegePassengerPhysics() {
    }

    public static void updatePassengerState(AbstractSiegeEntity siege, Entity passenger) {
        if (!siege.hasPassenger(passenger)) {
            return;
        }

        boolean controlsRotation = siege.shouldPassengerControlRotation(passenger);
        boolean bodyFollowsSiege = siege.shouldPassengerBodyFollowSiege(passenger);

        if (!controlsRotation) {
            siege.setTrackedPitch(0);
        } else if (passenger instanceof Player && !siege.usesIndependentPitchAim()) {
            siege.setTrackedPitch(passenger.getXRot());
        }
        if (bodyFollowsSiege && passenger instanceof LivingEntity livingPassenger) {
            float passengerYaw = siege.getPassengerVisualYaw(passenger);
            livingPassenger.setYBodyRot(passengerYaw);
        }
        if (passenger instanceof LivingEntity livingPassenger) {
            clampView(siege, passenger, livingPassenger);
        }
        siege.lastRiderPitch = siege.getTrackedPitch();

        if (!(passenger instanceof Player) && passenger.getFirstPassenger() == null) {
            float passengerYaw = siege.getPassengerVisualYaw(passenger);
            passenger.setYRot(passengerYaw);
            passenger.setYHeadRot(passengerYaw);
            passenger.setYBodyRot(passengerYaw);
            passenger.yRotO = passengerYaw;

            if (passenger instanceof LivingEntity livingPassenger) {
                livingPassenger.yHeadRotO = passengerYaw;
                livingPassenger.yBodyRotO = passengerYaw;
            }
        }
    }

    public static void clampView(AbstractSiegeEntity siege, Entity passenger, LivingEntity livingPassenger) {
        clampView(siege, passenger, livingPassenger, passenger.yRotO);
    }

    /** The input reference is the view before this mouse update, so overshooting cannot swap view edges. */
    public static void clampView(AbstractSiegeEntity siege, Entity passenger, LivingEntity livingPassenger,
                                 float referenceYaw) {
        float centerYaw = referenceYaw + Mth.wrapDegrees(siege.getPassengerVisualYaw(passenger) - referenceYaw);
        float previousCenterYaw = centerYaw + Mth.wrapDegrees(siege.yBodyRotO - siege.yBodyRot);
        float yawLimit = Mth.clamp(siege.getPassengerViewYawLimit(passenger), 0.0F, 180.0F);
        if (yawLimit < 180.0F) {
            passenger.setYRot(clampAngle(passenger.getYRot(), centerYaw, yawLimit));
            passenger.yRotO = clampAngle(passenger.yRotO, previousCenterYaw, yawLimit);
            float headYaw = referenceYaw + Mth.wrapDegrees(livingPassenger.getYHeadRot() - referenceYaw);
            float previousHeadYaw = referenceYaw + Mth.wrapDegrees(livingPassenger.yHeadRotO - referenceYaw);
            livingPassenger.setYHeadRot(clampAngle(headYaw, centerYaw, yawLimit));
            livingPassenger.yHeadRotO = clampAngle(previousHeadYaw, previousCenterYaw, yawLimit);
        }

        float pitchLimit = Mth.clamp(siege.getPassengerViewPitchLimit(passenger), 0.0F, 90.0F);
        passenger.setXRot(Mth.clamp(passenger.getXRot(), -pitchLimit, pitchLimit));
        passenger.xRotO = Mth.clamp(passenger.xRotO, -pitchLimit, pitchLimit);
    }

    private static float clampAngle(float angle, float center, float limit) {
        return center + Mth.clamp(angle - center, -limit, limit);
    }

    public static Vec3 rotatedSeatOffset(AbstractSiegeEntity siege, Entity passenger) {
        float yawRadians = (float) Math.toRadians(siege.getPassengerSeatYaw(passenger));
        Vec3 offset = siege.getPassengerOffset(passenger);

        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = Math.cos(yawRadians);
        double rightZ = Math.sin(yawRadians);

        double offsetX = rightX * offset.x - forwardX * offset.z;
        double offsetZ = rightZ * offset.x - forwardZ * offset.z;
        return new Vec3(offsetX, offset.y, offsetZ);
    }

    /**
     * Ground position for a hitched animal's seat: the ground below, up to the engine's step height up or down, so it
     * climbs a step before the engine reaches it. Against anything higher, or without headroom, it stays at seat height
     * and engine movement keeps it clear.
     */
    public static Vec3 mountFooting(AbstractSiegeEntity siege, Entity mount, Vec3 seat) {
        double step = siege.geometryStepHeight();
        double halfWidth = mount.getBbWidth() * 0.5D - FOOTING_INSET;
        AABB standing = new AABB(seat.x - halfWidth, seat.y, seat.z - halfWidth,
                seat.x + halfWidth, seat.y + mount.getBbHeight(), seat.z + halfWidth);
        double highest = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : mount.level().getBlockCollisions(mount,
                standing.expandTowards(0.0D, -step, 0.0D).expandTowards(0.0D, step, 0.0D))) {
            for (AABB part : shape.toAabbs()) {
                if (part.maxX > standing.minX && part.minX < standing.maxX
                        && part.maxZ > standing.minZ && part.minZ < standing.maxZ
                        && part.maxY <= seat.y + step + FOOTING_INSET && part.maxY >= seat.y - step) {
                    highest = Math.max(highest, part.maxY);
                }
            }
        }
        if (highest == Double.NEGATIVE_INFINITY || Math.abs(highest - seat.y) < FOOTING_INSET) {
            return descendTo(mount, seat);
        }
        AABB there = standing.move(0.0D, highest - seat.y, 0.0D).deflate(FOOTING_INSET);
        return mount.level().getBlockCollisions(mount, there).iterator().hasNext()
                ? descendTo(mount, seat)
                : descendTo(mount, new Vec3(seat.x, highest, seat.z));
    }
}
