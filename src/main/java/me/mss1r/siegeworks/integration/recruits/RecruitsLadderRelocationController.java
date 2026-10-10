package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

final class RecruitsLadderRelocationController {
    private static final String ACTION_TAG = "SiegeworksLadderRelocationAction";
    private static final String TARGET_ENTITY_TAG = "SiegeworksLadderRelocationTarget";
    private static final String TARGET_POS_TAG = "SiegeworksLadderRelocationPos";
    private static final String YAW_TAG = "SiegeworksLadderRelocationYaw";
    private static final String TASK_TICKS_TAG = "SiegeworksLadderRelocationTicks";
    private static final double APPROACH_SPEED = 1.05D;
    private static final double WORK_RANGE_SQR = 2.4D * 2.4D;
    private static final int TASK_TIMEOUT_TICKS = 2_400;

    private RecruitsLadderRelocationController() {
    }

    static boolean hasTask(AbstractRecruitEntity recruit) {
        return recruit.getPersistentData().contains(ACTION_TAG);
    }

    static boolean canCarry(AbstractRecruitEntity recruit, SiegeLadderEntity ladder) {
        UUID ownerUuid = recruit.getOwnerUUID();
        return !recruit.isPassenger()
                && !hasTask(recruit)
                && ladder.canBeRelocated()
                && ladder.canBeRelocatedBy(ownerUuid)
                && recruit.getInventory().canAddItem(ladder.createRelocationItem());
    }

    static boolean hasLadderItem(AbstractRecruitEntity recruit) {
        return findLadderSlot(recruit.getInventory()) >= 0;
    }

    static ItemStack getLadderItem(AbstractRecruitEntity recruit) {
        int slot = findLadderSlot(recruit.getInventory());
        return slot < 0 ? ItemStack.EMPTY : recruit.getInventory().getItem(slot);
    }

    static boolean assignPickup(AbstractRecruitEntity recruit, SiegeLadderEntity ladder) {
        if (!canCarry(recruit, ladder)) {
            return false;
        }

        ladder.claimOwnership(recruit.getOwnerUUID());
        if (ladder.getDeploymentOwnerUuid() == null
                && recruit.level() instanceof ServerLevel serverLevel
                && recruit.getOwnerUUID() != null) {
            SiegeDeploymentLimits.Deployment deployment =
                    SiegeDeploymentLimits.forOwner(serverLevel, recruit.getOwnerUUID());
            ladder.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        }
        prepareForTask(recruit);
        CompoundTag data = recruit.getPersistentData();
        data.putInt(ACTION_TAG, Action.PICKUP.ordinal());
        data.putUUID(TARGET_ENTITY_TAG, ladder.getUUID());
        data.putLong(TARGET_POS_TAG, ladder.blockPosition().asLong());
        data.putInt(TASK_TICKS_TAG, 0);
        return true;
    }

    static boolean assignPlacement(AbstractRecruitEntity recruit, BlockPos targetPos, float yaw) {
        ItemStack ladderStack = getLadderItem(recruit);
        UUID ownerUuid = recruit.getOwnerUUID();
        if (recruit.isPassenger()
                || hasTask(recruit)
                || ladderStack.isEmpty()
                || !SiegeLadderDeploymentItem.canBePlacedBy(ladderStack, recruit)) {
            return false;
        }

        if (SiegeLadderDeploymentItem.getRelocationOwner(ladderStack) == null && ownerUuid != null) {
            SiegeLadderDeploymentItem.withRelocationOwner(ladderStack, ownerUuid);
            if (recruit.level() instanceof ServerLevel serverLevel) {
                SiegeDeploymentLimits.writeToStack(
                        ladderStack, SiegeDeploymentLimits.forOwner(serverLevel, ownerUuid));
            }
        }
        prepareForTask(recruit);
        CompoundTag data = recruit.getPersistentData();
        data.putInt(ACTION_TAG, Action.PLACE.ordinal());
        data.putLong(TARGET_POS_TAG, targetPos.asLong());
        data.putFloat(YAW_TAG, yaw);
        data.putInt(TASK_TICKS_TAG, 0);
        return true;
    }

    static boolean tick(AbstractRecruitEntity recruit) {
        CompoundTag data = recruit.getPersistentData();
        if (!data.contains(ACTION_TAG)) {
            return false;
        }
        if (!(recruit.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        int taskTicks = data.getInt(TASK_TICKS_TAG) + 1;
        data.putInt(TASK_TICKS_TAG, taskTicks);
        Action action = actionFrom(data);
        if (action == null || taskTicks > TASK_TIMEOUT_TICKS) {
            clear(recruit);
            return false;
        }

        recruit.shouldMount(false, null);
        recruit.setTarget(null);
        return action == Action.PICKUP
                ? tickPickup(recruit, serverLevel, data)
                : tickPlacement(recruit, serverLevel, data);
    }

    private static boolean tickPickup(AbstractRecruitEntity recruit, ServerLevel serverLevel, CompoundTag data) {
        BlockPos lastTargetPos = BlockPos.of(data.getLong(TARGET_POS_TAG));
        Entity target = data.hasUUID(TARGET_ENTITY_TAG)
                ? serverLevel.getEntity(data.getUUID(TARGET_ENTITY_TAG))
                : null;
        if (target == null && !serverLevel.hasChunkAt(lastTargetPos)) {
            moveTo(recruit, Vec3.atCenterOf(lastTargetPos));
            return true;
        }
        if (!(target instanceof SiegeLadderEntity ladder)
                || !ladder.canBeRelocated()
                || !ladder.canBeRelocatedBy(recruit.getOwnerUUID())) {
            clear(recruit);
            return false;
        }

        if (!isInWorkRange(recruit, ladder.position())) {
            moveTo(recruit, ladder.position());
            return true;
        }

        ItemStack ladderStack = ladder.createRelocationItem();
        ItemStack remainder = recruit.getInventory().addItem(ladderStack);
        if (!remainder.isEmpty()) {
            clear(recruit);
            return false;
        }

        serverLevel.playSound(null, ladder.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.NEUTRAL, 0.8F, 0.9F);
        ladder.discard();
        clear(recruit);
        return false;
    }

    private static boolean tickPlacement(AbstractRecruitEntity recruit, ServerLevel serverLevel, CompoundTag data) {
        Container inventory = recruit.getInventory();
        int slot = findLadderSlot(inventory);
        if (slot < 0) {
            clear(recruit);
            return false;
        }

        BlockPos supportPos = BlockPos.of(data.getLong(TARGET_POS_TAG));
        BlockPos spawnPos = supportPos.above();
        if (!serverLevel.hasChunkAt(supportPos)) {
            moveTo(recruit, Vec3.atCenterOf(spawnPos));
            return true;
        }
        if (serverLevel.getBlockState(supportPos).getCollisionShape(serverLevel, supportPos).isEmpty()
                || !serverLevel.getBlockState(spawnPos).canBeReplaced()) {
            clear(recruit);
            return false;
        }

        Vec3 destination = Vec3.atBottomCenterOf(spawnPos);
        if (!isInWorkRange(recruit, destination)) {
            moveTo(recruit, destination);
            return true;
        }

        ItemStack ladderStack = inventory.getItem(slot);
        UUID ownerUuid = recruit.getOwnerUUID();
        if (!SiegeLadderDeploymentItem.canBePlacedBy(ladderStack, recruit)) {
            clear(recruit);
            return false;
        }
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(serverLevel);
        if (ladder == null) {
            clear(recruit);
            return false;
        }
        SiegeDeploymentLimits.Deployment deployment =
                SiegeDeploymentLimits.resolve(serverLevel, ladderStack, null);
        if (!SiegeDeploymentLimits.canDeployWithFeedback(
                serverLevel, ladder.getType(), deployment,
                serverLevel.getServer().getPlayerList().getPlayer(ownerUuid))) {
            clear(recruit);
            return false;
        }

        float yaw = data.getFloat(YAW_TAG);
        ladder.setSections(SiegeLadderDeploymentItem.getSections(ladderStack));
        UUID storedOwner = SiegeLadderDeploymentItem.getRelocationOwner(ladderStack);
        ladder.setOwnerUuid(storedOwner != null ? storedOwner : ownerUuid);
        if (deployment != null) {
            ladder.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        }
        float storedHealth = SiegeLadderDeploymentItem.getStoredHealth(ladderStack);
        if (storedHealth > 0.0F) {
            ladder.setHealth(Math.min(ladder.getMaxHealth(), storedHealth));
        }
        if (MinecraftVersionCompat.hasCustomName(ladderStack)) {
            ladder.setCustomName(ladderStack.getHoverName());
        }
        ladder.moveTo(destination.x, destination.y, destination.z, yaw, 0.0F);
        ladder.applyYaw(yaw);
        if (!serverLevel.addFreshEntity(ladder)) {
            clear(recruit);
            return false;
        }
        SiegeDeploymentLimits.register(ladder);

        ladderStack.shrink(1);
        inventory.setChanged();
        serverLevel.playSound(null, spawnPos, SoundEvents.LADDER_PLACE,
                SoundSource.BLOCKS, 1.0F, 0.9F);
        clear(recruit);
        return false;
    }

    private static int findLadderSlot(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(SiegeworksItems.SIEGE_LADDER_SPAWNER.get())) {
                return slot;
            }
        }
        return -1;
    }

    private static void prepareForTask(AbstractRecruitEntity recruit) {
        if (recruit instanceof SiegeEngineerEntity engineer && engineer.siegeController != null) {
            engineer.siegeController.reset();
            engineer.siegeController = null;
        }
        recruit.shouldMount(false, null);
        RecruitsTaskOrders.beginTask(recruit);
        recruit.setTarget(null);
        RecruitsWalkOrders.stop(recruit);
    }

    private static void moveTo(AbstractRecruitEntity recruit, Vec3 destination) {
        RecruitsWalkOrders.walkTo(recruit, destination, APPROACH_SPEED);
    }

    private static boolean isInWorkRange(AbstractRecruitEntity recruit, Vec3 target) {
        double dx = recruit.getX() - target.x;
        double dz = recruit.getZ() - target.z;
        return dx * dx + dz * dz <= WORK_RANGE_SQR
                && Math.abs(recruit.getY() - target.y) <= 2.5D;
    }

    private static Action actionFrom(CompoundTag data) {
        int ordinal = data.getInt(ACTION_TAG);
        return ordinal >= 0 && ordinal < Action.values().length ? Action.values()[ordinal] : null;
    }

    static boolean cancelTask(AbstractRecruitEntity recruit) {
        if (!hasTask(recruit)) {
            return false;
        }
        clear(recruit);
        return true;
    }

    private static void clear(AbstractRecruitEntity recruit) {
        CompoundTag data = recruit.getPersistentData();
        data.remove(ACTION_TAG);
        data.remove(TARGET_ENTITY_TAG);
        data.remove(TARGET_POS_TAG);
        data.remove(YAW_TAG);
        data.remove(TASK_TICKS_TAG);
        RecruitsWalkOrders.stop(recruit);
        RecruitsTaskOrders.endTask(recruit);
    }

    private enum Action {
        PICKUP,
        PLACE
    }
}
