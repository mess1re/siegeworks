package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

final class RecruitsMaintenanceController {
    private static final String TARGET_TAG = "SiegeworksMaintenanceTarget";
    private static final String TARGET_POS_TAG = "SiegeworksMaintenanceTargetPos";
    private static final String ACTION_TAG = "SiegeworksMaintenanceAction";
    private static final String PROGRESS_TAG = "SiegeworksMaintenanceProgress";
    private static final double APPROACH_SPEED = 1.05D;
    private static final int REPAIR_HITS = 8;
    private static final int WORK_INTERVAL_TICKS = 8;

    private RecruitsMaintenanceController() {
    }

    static boolean worksOn(SiegeEngineerEntity engineer, net.minecraft.world.entity.Entity siege) {
        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer);
        return data.hasUUID(TARGET_TAG) && data.getUUID(TARGET_TAG).equals(siege.getUUID());
    }

    static boolean hasTask(SiegeEngineerEntity engineer) {
        return me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer).hasUUID(TARGET_TAG);
    }

    static boolean assign(SiegeEngineerEntity engineer, AbstractSiegeEntity siege, Action action) {
        if (siege.isRemoved() || action == Action.REPAIR && !siege.needsMaintenanceRepair()
                || action == Action.DISMANTLE && !siege.canStartAutomatedDismantling(engineer)) {
            return false;
        }

        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer);
        data.putUUID(TARGET_TAG, siege.getUUID());
        data.putLong(TARGET_POS_TAG, siege.blockPosition().asLong());
        data.putInt(ACTION_TAG, action.ordinal());
        data.putInt(PROGRESS_TAG, 0);

        if (engineer.siegeController != null) {
            engineer.siegeController.reset();
            engineer.siegeController = null;
        }
        if (engineer.isPassenger()) {
            engineer.stopRiding();
            engineer.dismount = 180;
        }
        engineer.shouldMount(false, null);
        RecruitsTaskOrders.beginTask(engineer);
        engineer.setTarget(null);
        RecruitsWalkOrders.stop(engineer);
        return true;
    }

    static boolean tick(SiegeEngineerEntity engineer) {
        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer);
        if (!data.hasUUID(TARGET_TAG)) {
            return false;
        }
        if (!(engineer.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        BlockPos targetPos = BlockPos.of(data.getLong(TARGET_POS_TAG));
        Entity assignedEntity = serverLevel.getEntity(data.getUUID(TARGET_TAG));
        if (assignedEntity == null && !serverLevel.hasChunkAt(targetPos)) {
            RecruitsWalkOrders.walkTo(engineer, Vec3.atBottomCenterOf(targetPos), APPROACH_SPEED);
            return true;
        }
        if (!(assignedEntity instanceof AbstractSiegeEntity siege) || siege.isRemoved()) {
            clear(engineer, false);
            return false;
        }

        Action action = actionFrom(data);
        if (action == null || action == Action.REPAIR && !siege.needsMaintenanceRepair()) {
            clear(engineer, false);
            return false;
        }

        if (engineer.isPassenger()) {
            engineer.stopRiding();
            engineer.dismount = 180;
        }
        engineer.shouldMount(false, null);
        engineer.setTarget(null);
        engineer.getLookControl().setLookAt(siege, 30.0F, 30.0F);

        double workRange = siege.getBbWidth() * 0.5D + 1.6D;
        if (horizontalDistanceSqr(engineer.position(), siege.position()) > workRange * workRange
                || Math.abs(engineer.getY() - siege.getY()) > 2.5D) {
            Vec3 standPosition = getStandPosition(engineer, siege, workRange - 0.35D);
            RecruitsWalkOrders.walkTo(engineer, standPosition, APPROACH_SPEED);
            return true;
        }

        RecruitsWalkOrders.stop(engineer);
        if (!RecruitsConstructionController.equipConstructionHammer(engineer)) {
            return true;
        }

        if (action == Action.REPAIR) {
            tickRepair(engineer, siege, data);
        } else {
            tickDismantle(engineer, siege);
        }
        return true;
    }

    private static void tickRepair(SiegeEngineerEntity engineer, AbstractSiegeEntity siege, CompoundTag data) {
        Container materials = RecruitsSupplyInventory.resolve(engineer, siege.position());
        if (!siege.hasMaintenanceRepairMaterials(materials)
                || engineer.tickCount % WORK_INTERVAL_TICKS != 0) {
            return;
        }

        engineer.swing(InteractionHand.MAIN_HAND);
        int progress = data.getInt(PROGRESS_TAG) + 1;
        data.putInt(PROGRESS_TAG, progress);
        if (progress >= REPAIR_HITS && siege.repairFromMaintenanceWorker(engineer, materials)) {
            clear(engineer, false);
        }
    }

    private static void tickDismantle(SiegeEngineerEntity engineer, AbstractSiegeEntity siege) {
        if (!siege.isDismantling() && !siege.startAutomatedDismantling(engineer)) {
            clear(engineer, false);
            return;
        }
        if (engineer.tickCount % WORK_INTERVAL_TICKS != 0) {
            return;
        }

        Container refundTarget = RecruitsSupplyInventory.resolveForDeposit(engineer, siege.position());
        if (siege.performAutomatedDismantleHit(engineer, refundTarget)) {
            engineer.swing(InteractionHand.MAIN_HAND);
        }
        if (siege.isRemoved()) {
            clear(engineer, false);
        }
    }

    private static Vec3 getStandPosition(SiegeEngineerEntity engineer, AbstractSiegeEntity siege,
                                         double distance) {
        Vec3 away = new Vec3(
                engineer.getX() - siege.getX(), 0.0D, engineer.getZ() - siege.getZ());
        if (away.lengthSqr() < 0.01D) {
            double yaw = Math.toRadians(siege.getYRot());
            away = new Vec3(Math.sin(yaw), 0.0D, -Math.cos(yaw));
        }
        Vec3 offset = away.normalize().scale(distance);
        return new Vec3(siege.getX() + offset.x, siege.getY(), siege.getZ() + offset.z);
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private static Action actionFrom(CompoundTag data) {
        int ordinal = data.getInt(ACTION_TAG);
        return ordinal >= 0 && ordinal < Action.values().length ? Action.values()[ordinal] : null;
    }

    private static void clear(SiegeEngineerEntity engineer, boolean cancelDismantling) {
        CompoundTag data = me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer);
        UUID targetId = data.hasUUID(TARGET_TAG) ? data.getUUID(TARGET_TAG) : null;
        if (cancelDismantling && targetId != null && engineer.level() instanceof ServerLevel serverLevel
                && serverLevel.getEntity(targetId) instanceof AbstractSiegeEntity siege) {
            siege.cancelAutomatedDismantling();
        }
        data.remove(TARGET_TAG);
        data.remove(TARGET_POS_TAG);
        data.remove(ACTION_TAG);
        data.remove(PROGRESS_TAG);
        RecruitsWalkOrders.stop(engineer);
        RecruitsTaskOrders.endTask(engineer);
    }

    static boolean cancelTask(SiegeEngineerEntity engineer) {
        if (!hasTask(engineer)) {
            return false;
        }
        clear(engineer, true);
        return true;
    }

    enum Action {
        REPAIR,
        DISMANTLE
    }
}
