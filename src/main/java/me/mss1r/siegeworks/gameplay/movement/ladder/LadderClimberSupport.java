package me.mss1r.siegeworks.gameplay.movement.ladder;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.axiomata.collision.system.VirtualPlatformSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class LadderClimberSupport {
    private static final double LADDER_HALF_WIDTH = 7.5D / 16.0D;
    private static final double WALK_EXTENSION = 4.0D / 16.0D;
    /** How far below its rung an entity already on the ladder may be and still be held, as when climbing fast. */
    private static final double PLATFORM_Y_TOLERANCE = 1.15D;
    /** How far below a rung feet may be to step onto it, like a stair. */
    private static final double STEP_ON_HEIGHT = 0.6D;
    /** Getting on by walking happens within this much of either end. */
    private static final double ENTRY_ZONE = 1.5D;
    /** A landing within vanilla's safe drop distance also permits leaving an overhanging top rung. */
    private static final double LANDING_DEPTH = 3.0D;
    /** Movement along the ladder, per tick, that counts as walking onto it. */
    private static final double ENTRY_SPEED = 0.01D;
    private static final double STEEP_UPWARD_SPEED_FACTOR = 0.12D;
    private static final double STEEP_DOWNWARD_SPEED_FACTOR = 0.65D;
    private static final double STEEP_SIDE_SPEED_FACTOR = 0.55D;
    private static final double STEEP_MAX_UP_LOCAL_Y_PER_TICK = 0.085D;
    private static final double FLAT_MAX_UP_LOCAL_Y_PER_TICK = 0.16D;
    private static final double STEP_SOUND_MIN_LOCAL_Y_DELTA = 0.006D;
    private static final double CLIMB_POSE_MIN_VERTICAL_FACTOR = 0.60D;
    private static final double GUIDED_BACK_OFFSET = 3.0D / 16.0D;
    private static final int STEP_SOUND_INTERVAL_TICKS = 12;
    private static final int CLIMB_STATE_TTL_TICKS = 20;

    private final Host host;
    private final Map<Integer, ClimbState> climbStates = new HashMap<>();

    public LadderClimberSupport(Host host) {
        this.host = host;
    }

    public void tick() {
        cleanupClimbStates();
        if (Math.abs(host.leanProgress()) < 0.08F) {
            return;
        }

        Entity ladder = host.ladder();
        double length = host.ladderLength() + WALK_EXTENSION + 1.0D;
        AABB searchBox = ladder.getBoundingBox().inflate(length, host.ladderLength() + 1.0D, length);
        host.level().getEntities(ladder, searchBox, this::canStandOnLadder).forEach(this::supportOnLadder);
    }

    /** Players held on the ladder now. */
    public int climberCount() {
        return (int) climbStates.values().stream().filter(state -> host.gameTick() - state.tick() <= 2).count();
    }

    /** Lets everyone go, as when the ladder is pushed off its wall. */
    public void letGo() {
        climbStates.clear();
    }

    private boolean canStandOnLadder(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof AbstractSiegeEntity)
                && entity.isAlive()
                && !entity.isPassenger()
                && !(entity instanceof Player player && player.isSpectator());
    }

    private void supportOnLadder(Entity entity) {
        LocalPosition local = host.localPosition(entity.position());
        double angle = host.leanAngleRadians();
        double sin = Math.sin(angle);
        if (Math.abs(sin) < 0.08D) {
            return;
        }

        boolean steep = Math.abs(Math.cos(angle)) >= CLIMB_POSE_MIN_VERTICAL_FACTOR;
        double clearance = Math.max(GUIDED_BACK_OFFSET, entity.getBbWidth() * .5D + .12D);
        if (!(entity instanceof Player)) {
            // The route controller owns server movement. Clients only decorate the replicated
            // position; repositioning every nearby mob here causes snaps and false attachments.
            double along = (local.forward() + uphillDirection(angle) * clearance) / sin;
            if (Math.abs(local.side()) <= LADDER_HALF_WIDTH + entity.getBbWidth() * .5D
                    && along >= 0 && along <= host.ladderLength() + WALK_EXTENSION
                    && Math.abs(entity.getY() - host.platformY(along)) < .25D) {
                if (host.automatedClimberActive(entity.getUUID())) {
                    entity.fallDistance = 0;
                    entity.setOnGround(true);
                }
                if (steep && entity.getY() + entity.getBbHeight() * HAND_HEIGHT_FRACTION
                        <= host.platformY(host.ladderLength())) {
                    StructureClimbingSystem.markClimbPoseAttachment(entity, host.ladder(), (float) angle,
                            host.uphillYawDegrees(angle));
                }
            }
            return;
        }
        ClimbState previous = climbStates.get(entity.getId());
        boolean onLadder = previous != null && host.gameTick() - previous.tick() <= 2;
        // On a steep ladder everyone climbs in front of its rungs, on its middle line, and cannot edge off sideways.
        boolean guidedClimb = steep && host.automatedClimberActive(entity.getUUID());
        boolean keptAtGuidedOffset = steep;
        double forwardOffset = keptAtGuidedOffset
                ? uphillDirection(angle) * clearance
                : 0.0D;
        double halfWidth = LADDER_HALF_WIDTH + entity.getBbWidth() * 0.5D;
        double localY = (local.forward() + forwardOffset) / sin;
        if (onLadder && localY > host.ladderLength() && !landingAhead(entity, angle)) {
            // Past the top with nothing to step onto, as where the ladder rests against a wall taller than it: the
            // climber stays on its top rung rather than being let go into the drop.
            localY = host.ladderLength();
        }
        double end = host.ladderLength() + WALK_EXTENSION + entity.getBbWidth() * 0.5D;
        if (Math.abs(local.side()) > halfWidth || localY < 0.0D || localY > end) {
            return;
        }

        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.y > 0.35D) {
            // Jumping off.
            return;
        }
        if (!onLadder && !guidedClimb && !stepsOn(entity, localY, angle)) {
            return;
        }

        ClimbStep climbStep = limitClimbStep(entity, local.side(), localY, angle, keptAtGuidedOffset, steep);
        localY = climbStep.localY();
        double platformY = host.platformY(localY);
        double below = onLadder || guidedClimb ? PLATFORM_Y_TOLERANCE : STEP_ON_HEIGHT;
        if (entity.getY() < platformY - below || entity.getY() > platformY + 0.45D) {
            return;
        }

        Vec3 at = new Vec3(climbStep.position().x, platformY, climbStep.position().z);
        if (!LadderTraversalCollision.passage(entity, entity.position(), at)) return;
        entity.move(MoverType.SELF, at.subtract(entity.position()));
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            // This displacement is applied outside handleMovePlayer. Its next
            // packet must start from the corrected server position, not the old rung.
            player.connection.resetPosition();
        }
        climbStates.put(entity.getId(), new ClimbState(localY, host.gameTick()));
        entity.setDeltaMovement(slowMovement(velocity, angle, steep));
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
        VirtualPlatformSupport.markSupported(entity);
        double handsY = entity.getY() + entity.getBbHeight() * HAND_HEIGHT_FRACTION;
        if (steep && handsY <= host.platformY(host.ladderLength())) {
            StructureClimbingSystem.markClimbPoseAttachment(
                    entity, host.ladder(), (float) angle, host.uphillYawDegrees(angle));
        }
        playStepSound(entity, climbStep.climbedLocalY());
    }

    /** Whether the climber's next step is free and has something under it to take them off the ladder's top, like the top of a wall. */
    private boolean landingAhead(Entity entity, double angle) {
        Vec3 step = host.forwardVector().scale(uphillDirection(angle) * entity.getBbWidth() * 0.5D);
        AABB feet = entity.getBoundingBox().move(step);
        AABB below = new AABB(feet.minX, feet.minY - LANDING_DEPTH, feet.minZ, feet.maxX, feet.minY, feet.maxZ);
        return host.level().noCollision(entity, feet)
                && host.level().getBlockCollisions(entity, below).iterator().hasNext();
    }

    /**
     * Whether a player not yet on the ladder gets on: walking up onto its lower end, stepping down onto its upper
     * end, or landing on it from above. Touching it from the side, from under it or from beside its top does not.
     */
    private boolean stepsOn(Entity entity, double localY, double angle) {
        if (!(entity instanceof Player)) {
            // A mob walking into a ladder is only pathing past it; the ladder leads the ones that climb it. Which
            // those are is known on the server only, so the client poses any mob it finds standing on a rung.
            return entity.level().isClientSide;
        }
        Vec3 forward = host.forwardVector();
        double moved = (entity.getX() - entity.xo) * forward.x + (entity.getZ() - entity.zo) * forward.z;
        double uphill = moved * uphillDirection(angle);
        if (localY <= ENTRY_ZONE && uphill > ENTRY_SPEED) {
            return true;
        }
        if (localY >= host.ladderLength() - ENTRY_ZONE && uphill < -ENTRY_SPEED) {
            return true;
        }
        double rung = host.platformY(localY);
        return entity.yo >= rung - 0.05D && entity.getY() <= rung + 0.45D && entity.getY() < entity.yo;
    }

    private ClimbStep limitClimbStep(Entity entity, double side, double localY, double angle,
                                     boolean inFrontOfRungs, boolean centred) {
        ClimbState previous = climbStates.get(entity.getId());
        double adjustedLocalY = localY;
        double climbedLocalY = 0.0D;
        if (previous != null && host.gameTick() - previous.tick() <= 2) {
            double delta = localY - previous.localY();
            if (delta > 0.0D) {
                double maxDelta = maxClimbLocalYDelta(angle);
                adjustedLocalY = previous.localY() + Math.min(delta, maxDelta);
                climbedLocalY = adjustedLocalY - previous.localY();
            }
        }

        Vec3 position = entity.position();
        double offsetLocalY = adjustedLocalY - localY;
        if (Math.abs(offsetLocalY) > 1.0E-5D) {
            Vec3 forward = host.forwardVector();
            double offsetForward = offsetLocalY * Math.sin(angle);
            position = new Vec3(
                    entity.getX() + forward.x * offsetForward,
                    entity.getY(),
                    entity.getZ() + forward.z * offsetForward);
        }
        if (inFrontOfRungs) {
            if (centred) {
                position = position.subtract(host.rightVector().scale(side));
                side = 0.0D;
            }
            Vec3 forward = host.forwardVector();
            double desiredForward = adjustedLocalY * Math.sin(angle)
                    - uphillDirection(angle) * Math.max(GUIDED_BACK_OFFSET, entity.getBbWidth() * .5D + .12D);
            double currentForward = host.localPosition(position).forward();
            position = position.add(forward.scale(desiredForward - currentForward));
        }

        return new ClimbStep(adjustedLocalY,
                Mth.clamp(side, -LADDER_HALF_WIDTH, LADDER_HALF_WIDTH), position, climbedLocalY);
    }

    private static double maxClimbLocalYDelta(double angle) {
        return Mth.lerp(flatRelief(angle),
                STEEP_MAX_UP_LOCAL_Y_PER_TICK,
                FLAT_MAX_UP_LOCAL_Y_PER_TICK);
    }

    private Vec3 slowMovement(Vec3 velocity, double angle, boolean guidedClimb) {
        Vec3 forward = host.forwardVector();
        double forwardSpeed = velocity.x * forward.x + velocity.z * forward.z;
        double sideX = velocity.x - forward.x * forwardSpeed;
        double sideZ = velocity.z - forward.z * forwardSpeed;
        double uphillSpeed = forwardSpeed * uphillDirection(angle);
        double flatRelief = flatRelief(angle);
        double uphillFactor = Mth.lerp(flatRelief, STEEP_UPWARD_SPEED_FACTOR, 1.0D);
        double downhillFactor = Mth.lerp(flatRelief, STEEP_DOWNWARD_SPEED_FACTOR, 1.0D);
        double sideFactor = guidedClimb
                ? 0.0D
                : Mth.lerp(flatRelief, STEEP_SIDE_SPEED_FACTOR, 1.0D);
        double forwardFactor = uphillSpeed > 0.0D ? uphillFactor : downhillFactor;

        return new Vec3(
                forward.x * forwardSpeed * forwardFactor + sideX * sideFactor,
                0.0D,
                forward.z * forwardSpeed * forwardFactor + sideZ * sideFactor);
    }

    private static double flatRelief(double angle) {
        double flatness = Mth.clamp(Math.abs(Math.sin(angle)), 0.0D, 1.0D);
        double squaredFlatness = flatness * flatness;
        return squaredFlatness * squaredFlatness;
    }

    private static double uphillDirection(double angle) {
        double direction = Math.signum(Math.sin(angle));
        return direction == 0.0D ? 1.0D : direction;
    }

    private void playStepSound(Entity entity, double climbedLocalY) {
        if (!(host.level() instanceof ServerLevel serverLevel)
                || entity.tickCount % STEP_SOUND_INTERVAL_TICKS != 0
                || climbedLocalY <= STEP_SOUND_MIN_LOCAL_Y_DELTA) {
            return;
        }

        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.LADDER_STEP, SoundSource.PLAYERS, 0.65F, 0.9F + host.randomFloat() * 0.2F);
    }

    private void cleanupClimbStates() {
        if (host.gameTick() % CLIMB_STATE_TTL_TICKS != 0) {
            return;
        }

        Iterator<Map.Entry<Integer, ClimbState>> iterator = climbStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ClimbState> entry = iterator.next();
            if (host.gameTick() - entry.getValue().tick() > CLIMB_STATE_TTL_TICKS) {
                iterator.remove();
            }
        }
    }

    public record LocalPosition(double side, double forward) {
    }

    private record ClimbState(double localY, int tick) {
    }

    private static final double HAND_HEIGHT_FRACTION = 0.72D;

    private record ClimbStep(double localY, double side, Vec3 position, double climbedLocalY) {
    }

    public interface Host {
        AbstractSiegeEntity ladder();

        Level level();

        int gameTick();

        float leanProgress();

        double leanAngleRadians();

        double ladderLength();

        LocalPosition localPosition(Vec3 worldPosition);

        Vec3 forwardVector();

        Vec3 rightVector();

        double platformY(double localY);

        float uphillYawDegrees(double angle);

        boolean automatedClimberActive(UUID entityUuid);

        float randomFloat();
    }
}
