package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.entities.ai.controller.siegeengineer.ISiegeController;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeArtilleryControl;
import me.mss1r.siegeworks.api.SiegeDeployableControl;
import me.mss1r.siegeworks.api.SiegeMeleeControl;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SiegeworksRecruitController implements ISiegeController {
    private static final double ARRIVAL_DISTANCE_SQR = 9.0D;
    private static final float YAW_TOLERANCE = 2.0F;
    private static final float STEERING_BAND = 25.0F;
    private static final float SPIN_THRESHOLD = 45.0F;
    private static final float TURNING_THROTTLE = 0.35F;
    private static final double SLOWING_DISTANCE = 8.0D;
    private static final float REVERSE_ENTER = 110.0F;
    private static final float REVERSE_LEAVE = 35.0F;
    private static final float FIRE_PITCH_TOLERANCE = 2.0F;
    private static final float AIM_LIMIT_EPSILON = 0.01F;
    private static final int FIRE_ZONE_TARGET_SAMPLES = 16;
    private static final double MOVE_PROGRESS_STEP = 1.0D;
    private static final int MOVE_STALL_TICKS = 300;
    private static final float MIN_AIM_STEERING = 0.15F;
    private static final int TARGET_UNREACHABLE_PATIENCE_TICKS = 40;

    private final SiegeEngineerEntity engineer;
    private AbstractSiegeEntity siege;
    private Vec3 targetPos;
    private Float requestedYaw;
    private Boolean deploymentOverride;
    private boolean temporaryDeployment;
    private Boolean deploymentOverrideBeforeTemporary;
    private boolean reversing;
    private int artilleryShotSequence;
    private Vec3 committedArtilleryAimTarget;
    private boolean advanceArtilleryAimAfterCooldown;
    private Vec3 moveProgressTarget;
    private double closestMoveDistance;
    private int lastMoveProgressTick;
    private LivingEntity committedTarget;
    private int committedTargetUnreachableTicks;
    private Boolean attackOrderBeforeRam;

    /** Rams wait for an attack order; the engineer's own setting is restored when he leaves. */
    public SiegeworksRecruitController(SiegeEngineerEntity engineer, AbstractSiegeEntity siege) {
        this.engineer = engineer;
        this.siege = siege;
        if (siege instanceof SiegeMeleeControl) {
            attackOrderBeforeRam = engineer.getShouldRanged();
            engineer.setShouldRanged(false);
        }
    }

    public boolean controls(Entity entity) {
        return siege == entity;
    }

    @Override
    public void tryMount(Entity entity) {
        if (entity instanceof AbstractSiegeEntity siegeEntity) {
            siege = siegeEntity;
        }
    }

    @Override
    public void tryDismount() {
        reset();
        siege = null;
    }

    @Override
    public void tick() {
        if (engineer.level().isClientSide() || siege == null || !siege.isOperator(engineer)
                || siege.isBeingCaptured()) {
            return;
        }

        if (temporaryDeployment && siege instanceof SiegeTowerEntity tower
                && !RecruitsSiegeTraversal.hasTowerTraffic(tower) && !tower.hasBridgePassengers()) {
            deploymentOverride = deploymentOverrideBeforeTemporary;
            deploymentOverrideBeforeTemporary = null;
            temporaryDeployment = false;
            setDeployed(deploymentOverride != null ? deploymentOverride : deployWhereItStands());
        }

        if (requestedYaw != null) {
            if (faceRequestedYaw()) {
                requestedYaw = null;
            }
            return;
        }

        Vec3 destination = getMovementTarget();
        if (destination != null && followsMoveOrder()) {
            boolean fartherToGo = hasFartherToGo(destination);
            if (fartherToGo && !stalledOn(destination)) {
                trace("driving to an order", "%.1f blocks left", distanceTo(destination));
                moveToward(destination);
                return;
            }
            if (advanceMarch(destination)) {
                return;
            }
            trace(fartherToGo ? "dropped a move order it could not follow" : "arrived", "%.1f blocks left",
                    distanceTo(destination));
            finishMoveOrder();
            destination = getMovementTarget();
        } else {
            moveProgressTarget = null;
        }

        if (updateAttacking()) {
            return;
        }

        if (destination == null) {
            trace("standing", "no place to be");
        } else {
            trace("returning to its post", "%.1f blocks away", distanceTo(destination));
        }
        moveToward(destination);
    }

    private double distanceTo(Vec3 destination) {
        return Math.sqrt(horizontalDistanceSqr(siege.position(), destination));
    }

    private void trace(String decision, String details, Object... args) {
        if (RecruitsDebug.enabled()) {
            RecruitsDebug.engine(engineer, decision, args.length == 0 ? details : String.format(details, args));
        }
    }

    /** Move orders take priority over fighting; follow and hold orders don't. */
    private boolean followsMoveOrder() {
        return RecruitsDriveOrders.any(siege)
                || engineer.getFollowState() == 0 && engineer.getShouldMovePos() && engineer.getMovePos() != null;
    }

    private boolean stalledOn(Vec3 destination) {
        double distance = Math.sqrt(horizontalDistanceSqr(siege.position(), destination));
        if (moveProgressTarget == null || moveProgressTarget.distanceToSqr(destination) > 1.0D
                || distance < closestMoveDistance - MOVE_PROGRESS_STEP) {
            moveProgressTarget = destination;
            closestMoveDistance = distance;
            lastMoveProgressTick = engineer.tickCount;
            return false;
        }
        return engineer.tickCount - lastMoveProgressTick > MOVE_STALL_TICKS;
    }

    private void finishMoveOrder() {
        engineer.setShouldMovePos(false);
        RecruitsDriveOrders.forget(siege);
        moveProgressTarget = null;
    }

    private boolean faceRequestedYaw() {
        if (!siege.usesIndependentAim()) {
            return steerTowardYaw(requestedYaw, 0.25F);
        }

        siege.setOperatorMovement(engineer, 0.0F, 0.0F);
        siege.setOperatorAim(engineer, requestedYaw, siege.getTrackedPitch());
        return Math.abs(Mth.wrapDegrees(requestedYaw - siege.getTrackedYaw())) <= YAW_TOLERANCE;
    }

    @Override
    public boolean updateAttacking() {
        if (!engineer.getShouldRanged()) {
            trace("holding fire", "not ordered to attack");
            siege.cancelPrimaryAction(engineer);
            if (siege instanceof SiegeArtilleryControl artillery) {
                levelAim(artillery);
            }
            return false;
        }

        if (siege instanceof SiegeArtilleryControl artillery) {
            return updateArtilleryAttacking(artillery);
        }
        if (siege instanceof SiegeMeleeControl melee) {
            return updateMeleeAttacking(melee);
        }
        return false;
    }

    private boolean updateArtilleryAttacking(SiegeArtilleryControl artillery) {
        updateTarget();
        Vec3 aimTarget = resolveArtilleryAimTarget(artillery, targetPos);
        noteTargetReach(aimTarget != null);
        if (aimTarget == null) {
            if (targetPos == null) {
                trace("no target", "nothing to shoot at");
            } else {
                trace("target out of reach", "target %.1f blocks away at %s",
                        distanceTo(targetPos), BlockPos.containing(targetPos));
            }
            siege.cancelPrimaryAction(engineer);
            levelAim(artillery);
            return false;
        }

        Aim aim = calculateAim(artillery, aimTarget);
        float requestedPitch = aim.pitch();
        artillery.prepareAutomatedShot(aimTarget);
        siege.setOperatorAim(engineer, aim.yaw(), requestedPitch);

        float currentYaw = siege.usesIndependentAim() ? siege.getTrackedYaw() : siege.getYRot();
        float yawError = Math.abs(Mth.wrapDegrees(aim.yaw() - currentYaw));
        float pitchError = Math.abs(requestedPitch - siege.getTrackedPitch());
        boolean yawAligned = yawError <= YAW_TOLERANCE;
        if (!siege.usesIndependentAim() && yawError > YAW_TOLERANCE) {
            steerTowardYaw(aim.yaw(), 0.25F);
        } else {
            siege.setOperatorMovement(engineer, 0.0F, 0.0F);
        }

        Container actionInventory = getActionInventory();
        if (!artillery.isReadyToFire()) {
            SiegeActionResult loading = siege.advancePrimaryAction(engineer, actionInventory);
            trace("loading", "%s, state %s, yaw off %.1f, pitch off %.1f, turning the whole engine %s",
                    loading, siege.getOperationState(), yawError, pitchError, !siege.usesIndependentAim());
            return true;
        }

        boolean pitchReachable = aim.pitch() >= siege.getMinAimPitch() - AIM_LIMIT_EPSILON
                && aim.pitch() <= siege.getMaxAimPitch() + AIM_LIMIT_EPSILON;
        boolean reachable = artillery.canReachAutomatedTarget(aimTarget);
        if (!yawAligned || pitchError > FIRE_PITCH_TOLERANCE || !pitchReachable || !reachable) {
            trace("aiming", "yaw off %.1f (want %.1f, at %.1f), pitch off %.1f (want %.1f), pitch in range %s,"
                            + " reachable %s, %.1f blocks to target",
                    yawError, aim.yaw(), currentYaw, pitchError, requestedPitch, pitchReachable, reachable,
                    distanceTo(aimTarget));
            return true;
        }

        SiegeActionResult result = siege.advancePrimaryAction(engineer, actionInventory);
        trace("firing", "%s at %.1f blocks", result, distanceTo(aimTarget));
        if (result == SiegeActionResult.FIRED) {
            committedArtilleryAimTarget = aimTarget;
            advanceArtilleryAimAfterCooldown = true;
        }
        return true;
    }

    private Vec3 resolveArtilleryAimTarget(SiegeArtilleryControl artillery, Vec3 commandedTarget) {
        if (committedArtilleryAimTarget != null) {
            if (siege.getOperationState() == SiegeOperationState.COOLDOWN) {
                return committedArtilleryAimTarget;
            }
            committedArtilleryAimTarget = null;
            if (advanceArtilleryAimAfterCooldown) {
                artilleryShotSequence = artilleryShotSequence == Integer.MAX_VALUE
                        ? 0
                        : artilleryShotSequence + 1;
                advanceArtilleryAimAfterCooldown = false;
            }
        }
        if (commandedTarget == null) {
            return null;
        }
        if (engineer.getShouldStrategicFire() && RecruitsFireZone.hasZone(engineer)) {
            for (int sample = 0; sample < FIRE_ZONE_TARGET_SAMPLES; sample++) {
                Vec3 zoneTarget = RecruitsFireZone.resolveTarget(engineer, artilleryShotSequence,
                        artillery.getAutomatedTargetSpreadRadius(), sample);
                if (zoneTarget == null) {
                    break;
                }
                Vec3 resolved = artillery.resolveAutomatedAimTarget(zoneTarget, artilleryShotSequence);
                if (isReachableAimTarget(artillery, resolved)) {
                    return resolved;
                }
            }
            return null;
        }

        Vec3 resolved = artillery.resolveAutomatedAimTarget(commandedTarget, artilleryShotSequence);
        return isReachableAimTarget(artillery, resolved) ? resolved : null;
    }

    private boolean isReachableAimTarget(SiegeArtilleryControl artillery, Vec3 target) {
        if (target == null || !artillery.canReachAutomatedTarget(target)) {
            return false;
        }
        float pitch = artillery.calculateAutomatedAimPitch(target);
        return Float.isFinite(pitch)
                && pitch >= siege.getMinAimPitch() - AIM_LIMIT_EPSILON
                && pitch <= siege.getMaxAimPitch() + AIM_LIMIT_EPSILON;
    }

    private boolean updateMeleeAttacking(SiegeMeleeControl melee) {
        updateTarget();
        if (targetPos == null) {
            siege.cancelPrimaryAction(engineer);
            return false;
        }

        MeleeTarget meleeTarget = resolveMeleeTarget(targetPos);
        double attackRange = melee.getAutomatedAttackRange();
        if (horizontalDistanceSqr(siege.position(), meleeTarget.position()) > attackRange * attackRange) {
            moveToward(meleeTarget.position(), attackRange * attackRange);
            return true;
        }

        if (!steerTowardYaw(meleeTarget.yaw(), 0.2F)) {
            return true;
        }

        siege.advancePrimaryAction(engineer, getActionInventory());
        return true;
    }

    private MeleeTarget resolveMeleeTarget(Vec3 target) {
        Vec3 traceOrigin = new Vec3(
                siege.getX(),
                siege.getY() + siege.getBbHeight() * 0.5D,
                siege.getZ());
        BlockHitResult hit = siege.level().clip(new ClipContext(
                traceOrigin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, siege));
        Vec3 attackPosition = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : target;

        if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection().getAxis() != Direction.Axis.Y) {
            Direction attackDirection = hit.getDirection().getOpposite();
            return new MeleeTarget(attackPosition, yawToward(
                    attackDirection.getStepX(), attackDirection.getStepZ()));
        }

        return new MeleeTarget(attackPosition, yawToward(
                attackPosition.x - siege.getX(), attackPosition.z - siege.getZ()));
    }

    private static float yawToward(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void levelAim(SiegeArtilleryControl artillery) {
        float yaw = siege.usesIndependentAim() ? siege.getTrackedYaw() : siege.getYRot();
        siege.setOperatorAim(engineer, yaw, 0.0F);
    }

    private void updateTarget() {
        if (engineer.getShouldStrategicFire()) {
            BlockPos strategicFirePos = engineer.getStrategicFirePos();
            targetPos = strategicFirePos == null ? null : strategicFirePos.getCenter();
            return;
        }

        if (committedTarget != null && (!committedTarget.isAlive() || committedTarget.level() != engineer.level())) {
            committedTarget = null;
        }
        if (committedTarget == null) {
            LivingEntity candidate = engineer.getTarget();
            if ((candidate == null || !candidate.isAlive()) && engineer.tickCount % 10 == 0) {
                engineer.checkForPotentialEnemies();
                candidate = engineer.getTarget();
            }
            if (candidate != null && candidate.isAlive()) {
                commitTo(candidate);
            }
        }
        targetPos = committedTarget == null ? null : committedTarget.getEyePosition();
    }

    /** Slow-turning engines never fire if they keep switching to the nearest enemy. */
    private void commitTo(LivingEntity target) {
        committedTarget = target;
        committedTargetUnreachableTicks = 0;
    }

    private void noteTargetReach(boolean reachable) {
        if (reachable || committedTarget == null) {
            committedTargetUnreachableTicks = 0;
            return;
        }
        if (++committedTargetUnreachableTicks > TARGET_UNREACHABLE_PATIENCE_TICKS) {
            if (engineer.getTarget() == committedTarget) {
                engineer.setTarget(null);
            }
            committedTarget = null;
            committedTargetUnreachableTicks = 0;
        }
    }

    private Container getActionInventory() {
        return RecruitsSupplyInventory.resolve(engineer, siege.position());
    }

    private Aim calculateAim(SiegeArtilleryControl artillery, Vec3 target) {
        Vec3 origin = artillery.getAutomatedAimOrigin();
        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = artillery.calculateAutomatedAimPitch(target);
        return new Aim(yaw, pitch);
    }

    private Vec3 getMovementTarget() {
        return switch (engineer.getFollowState()) {
            case 0 -> engineer.getShouldMovePos() && engineer.getMovePos() != null
                    ? engineer.getMovePos().getCenter() : null;
            case 1 -> engineer.getOwner() == null ? null : engineer.getOwner().position();
            case 2, 3, 4 -> engineer.getHoldPos();
            case 5 -> engineer.getProtectingMob() == null ? null : engineer.getProtectingMob().position();
            default -> null;
        };
    }

    private boolean hasFartherToGo(Vec3 destination) {
        return destination != null
                && horizontalDistanceSqr(siege.position(), destination)
                        > getEffectiveStoppingDistanceSqr(ARRIVAL_DISTANCE_SQR);
    }

    private void moveToward(Vec3 destination) {
        moveToward(destination, ARRIVAL_DISTANCE_SQR);
    }

    private void moveToward(Vec3 destination, double stoppingDistanceSqr) {
        if (Boolean.TRUE.equals(deploymentOverride)) {
            siege.setOperatorMovement(engineer, 0.0F, 0.0F);
            setDeployed(true);
            return;
        }

        double effectiveStoppingDistanceSqr = getEffectiveStoppingDistanceSqr(stoppingDistanceSqr);
        if (destination == null
                || horizontalDistanceSqr(siege.position(), destination) <= effectiveStoppingDistanceSqr) {
            siege.setOperatorMovement(engineer, 0.0F, 0.0F);
            setDeployed(deploymentOverride != null ? deploymentOverride : deployWhereItStands());
            return;
        }

        setDeployed(false);

        double dx = destination.x - siege.getX();
        double dz = destination.z - siege.getZ();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawError = Mth.wrapDegrees(targetYaw - siege.getYRot());
        float absError = Math.abs(yawError);
        double distance = Math.sqrt(horizontalDistanceSqr(siege.position(), destination));

        float approachThrottle = (float) Mth.clamp(distance / SLOWING_DISTANCE, 0.0D, 1.0D);

        boolean canSpin = !siege.isTowed() && siege.canPivotInPlace(engineer);
        reversing = !canSpin && (reversing ? absError > REVERSE_LEAVE : absError > REVERSE_ENTER);

        if (reversing) {
            float steering = absError <= YAW_TOLERANCE
                    ? 0.0F
                    : Mth.clamp(yawError / STEERING_BAND, -1.0F, 1.0F);
            siege.setOperatorMovement(engineer, -TURNING_THROTTLE, steering);
            return;
        }

        float steering = absError <= YAW_TOLERANCE
                ? 0.0F
                : Mth.clamp(-yawError / STEERING_BAND, -1.0F, 1.0F);

        float headingThrottle = absError >= 90.0F ? 0.0F : Mth.cos(absError * Mth.DEG_TO_RAD);
        float forward = Math.min(headingThrottle, approachThrottle);
        if (canSpin && absError > SPIN_THRESHOLD) {
            forward = 0.0F;
        } else if (absError > YAW_TOLERANCE) {
            forward = Math.max(forward, Math.min(TURNING_THROTTLE, approachThrottle));
        }
        siege.setOperatorMovement(engineer, forward, steering);
    }

    private boolean advanceMarch(Vec3 destination) {
        return destination != null
                && horizontalDistanceSqr(siege.position(), destination)
                        <= getEffectiveStoppingDistanceSqr(ARRIVAL_DISTANCE_SQR)
                && RecruitsDriveOrders.legReached(engineer, BlockPos.containing(destination));
    }

    private boolean deployWhereItStands() {
        return siege instanceof SiegeDeployableControl deployable && deployable.worthDeployingHere();
    }

    private void setDeployed(boolean deployed) {
        if (siege instanceof SiegeDeployableControl deployable && deployable.isDeployed() != deployed) {
            deployable.setDeployed(engineer, deployed);
        }
    }

    public Boolean getDeploymentOverride() {
        return deploymentOverride;
    }

    public void setDeploymentOverride(Boolean deploymentOverride) {
        temporaryDeployment = false;
        deploymentOverrideBeforeTemporary = null;
        this.deploymentOverride = deploymentOverride;
        if (deploymentOverride == null) {
            setDeployed(deployWhereItStands());
            return;
        }

        if (deploymentOverride) {
            siege.setOperatorMovement(engineer, 0.0F, 0.0F);
        }
        setDeployed(deploymentOverride);
    }

    public void requestTemporaryDeployment() {
        if (!temporaryDeployment) {
            deploymentOverrideBeforeTemporary = deploymentOverride;
        }
        temporaryDeployment = true;
        deploymentOverride = true;
        siege.setOperatorMovement(engineer, 0.0F, 0.0F);
        setDeployed(true);
    }

    private double getEffectiveStoppingDistanceSqr(double stoppingDistanceSqr) {
        if (siege instanceof SiegeDeployableControl deployable) {
            double deploymentDistance = deployable.getAutomatedDeploymentDistance();
            return Math.max(stoppingDistanceSqr, deploymentDistance * deploymentDistance);
        }
        return stoppingDistanceSqr;
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = second.x - first.x;
        double dz = second.z - first.z;
        return dx * dx + dz * dz;
    }

    /**
     * Turns the engine. Engines that can pivot do it in place, since steering is scaled down with speed; steering eases
     * off near the target heading to avoid overshooting.
     */
    private boolean steerTowardYaw(float yaw, float forward) {
        float error = Mth.wrapDegrees(yaw - siege.getYRot());
        if (Math.abs(error) <= YAW_TOLERANCE) {
            siege.setOperatorMovement(engineer, 0.0F, 0.0F);
            return true;
        }
        float steering = Mth.clamp(-error / STEERING_BAND, -1.0F, 1.0F);
        if (Math.abs(steering) < MIN_AIM_STEERING) {
            steering = Math.copySign(MIN_AIM_STEERING, steering);
        }
        siege.setOperatorMovement(engineer, siege.canPivotInPlace(engineer) ? 0.0F : forward, steering);
        return false;
    }

    @Override
    public void reset() {
        if (siege != null) {
            siege.clearOperatorInput(engineer);
            siege.cancelPrimaryAction(engineer);
        }
        targetPos = null;
        committedTarget = null;
        committedTargetUnreachableTicks = 0;
        requestedYaw = null;
        deploymentOverride = null;
        temporaryDeployment = false;
        deploymentOverrideBeforeTemporary = null;
        artilleryShotSequence = 0;
        committedArtilleryAimTarget = null;
        advanceArtilleryAimAfterCooldown = false;
        moveProgressTarget = null;
        if (attackOrderBeforeRam != null) {
            engineer.setShouldRanged(attackOrderBeforeRam);
            attackOrderBeforeRam = null;
        }
    }

    @Override
    public Entity getSiegeEntity() {
        return siege;
    }

    @Override
    public void calculatePath() {
        LivingEntity commandedTarget = engineer.getTarget();
        if (engineer.getFollowState() == 3 && commandedTarget != null && commandedTarget.isAlive()) {
            engineer.setShouldStrategicFire(false);
            engineer.setShouldRanged(true);
            commitTo(commandedTarget);
            targetPos = commandedTarget.getEyePosition();
        }
    }

    @Override
    public Vec3 getTargetPos() {
        return targetPos;
    }

    @Override
    public void setTargetPos(Vec3 targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    public void startFaceRotation(float yaw) {
        requestedYaw = yaw;
    }

    @Override
    public boolean needsRepair() {
        return siege != null && siege.getHealth() < siege.getMaxHealth() * 0.8F;
    }

    @Override
    public boolean canRepair() {
        return false;
    }

    @Override
    public void tryRepair() {
    }

    private record Aim(float yaw, float pitch) {
    }

    private record MeleeTarget(Vec3 position, float yaw) {
    }
}
