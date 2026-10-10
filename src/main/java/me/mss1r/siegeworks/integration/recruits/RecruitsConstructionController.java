package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class RecruitsConstructionController {
    private static final String SITE_TAG = "SiegeworksConstructionSite";
    private static final String SITE_POS_TAG = "SiegeworksConstructionSitePos";
    private static final String BLOCK_REASON_TAG = "SiegeworksConstructionBlockReason";
    private static final double APPROACH_SPEED = 1.05D;
    private static final double STAND_OFFSET = 1.75D;
    private static final double WORK_RANGE_SQR = 1.8D * 1.8D;

    private RecruitsConstructionController() {
    }

    static boolean hasTask(SiegeEngineerEntity engineer) {
        return engineer.getPersistentData().hasUUID(SITE_TAG);
    }

    static boolean hasConstructionHammer(SiegeEngineerEntity engineer) {
        Container inventory = engineer.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(BlueprintTags.CONSTRUCTION_HAMMERS)) {
                return true;
            }
        }
        return false;
    }

    static void assign(SiegeEngineerEntity engineer, Entity site) {
        engineer.getPersistentData().putUUID(SITE_TAG, site.getUUID());
        engineer.getPersistentData().putLong(SITE_POS_TAG, site.blockPosition().asLong());
        engineer.getPersistentData().remove(BLOCK_REASON_TAG);
        if (engineer.siegeController != null) {
            engineer.siegeController.reset();
            engineer.siegeController = null;
        }
        engineer.shouldMount(false, null);
        RecruitsTaskOrders.beginTask(engineer);
        engineer.setTarget(null);
        RecruitsWalkOrders.stop(engineer);
    }

    static boolean tick(SiegeEngineerEntity engineer) {
        CompoundTag data = engineer.getPersistentData();
        if (!data.hasUUID(SITE_TAG)) {
            return false;
        }
        if (!(engineer.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        BlockPos sitePos = BlockPos.of(data.getLong(SITE_POS_TAG));
        Entity assignedEntity = serverLevel.getEntity(data.getUUID(SITE_TAG));
        if (assignedEntity == null && !serverLevel.hasChunkAt(sitePos)) {
            RecruitsWalkOrders.walkTo(engineer, net.minecraft.world.phys.Vec3.atBottomCenterOf(sitePos), APPROACH_SPEED);
            return true;
        }
        if (!(assignedEntity instanceof UnderConstruction machine) || assignedEntity.isRemoved()
                || machine.isFullyBuilt()) {
            clear(engineer);
            return false;
        }

        if (engineer.isPassenger()) {
            engineer.stopRiding();
            engineer.dismount = 180;
        }
        engineer.shouldMount(false, null);
        engineer.setTarget(null);

        Vec3 workTarget = assignedEntity.position().add(0.0D, assignedEntity.getBbHeight() * 0.6D, 0.0D);
        Vec3 standPosition = getStandPosition(assignedEntity);
        engineer.getLookControl().setLookAt(workTarget.x, workTarget.y, workTarget.z, 30.0F, 30.0F);

        if (horizontalDistanceSqr(engineer.position(), standPosition) > WORK_RANGE_SQR
                || Math.abs(engineer.getY() - standPosition.y) > 1.5D) {
            RecruitsWalkOrders.walkTo(engineer, standPosition, APPROACH_SPEED);
            return true;
        }

        RecruitsWalkOrders.stop(engineer);
        if (!equipConstructionHammer(engineer)) {
            reportBlocked(engineer, "hammer", Component.translatable(
                    "message.siegeworks.recruits.build_missing_hammer", engineer.getDisplayName()));
            return true;
        }

        Container materials = RecruitsSupplyInventory.resolve(engineer, assignedEntity.position());
        ConstructionWork.Result result = ConstructionWork.strike(machine, materials);
        if (result.status() == ConstructionWork.Status.MISSING_MATERIAL) {
            reportBlocked(engineer, "material:" + result.missing().key(), Component.translatable(
                    "message.siegeworks.recruits.build_missing_material",
                    engineer.getDisplayName(), result.missing().displayName()));
            return true;
        }
        if (!result.worked()) {
            return true;
        }
        engineer.getPersistentData().remove(BLOCK_REASON_TAG);

        engineer.swing(InteractionHand.MAIN_HAND);
        ItemStack hammer = engineer.getMainHandItem();
        if (hammer.isDamageableItem()) {
            MinecraftVersionCompat.damageHeldItem(hammer, 1, engineer, InteractionHand.MAIN_HAND);
        }
        return true;
    }

    private static Vec3 getStandPosition(Entity machine) {
        float yaw = machine.getYRot() * ((float) Math.PI / 180.0F);
        double offsetX = -Math.sin(yaw) * -STAND_OFFSET;
        double offsetZ = Math.cos(yaw) * -STAND_OFFSET;
        return machine.position().add(offsetX, 0.0D, offsetZ);
    }

    static boolean equipConstructionHammer(SiegeEngineerEntity engineer) {
        if (engineer.getMainHandItem().is(BlueprintTags.CONSTRUCTION_HAMMERS)) {
            return true;
        }

        Container inventory = engineer.getInventory();
        int mainHandSlot = engineer.getInventorySlotIndex(EquipmentSlot.MAINHAND);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack hammer = inventory.getItem(slot);
            if (!hammer.is(BlueprintTags.CONSTRUCTION_HAMMERS)) {
                continue;
            }

            ItemStack previousMainHand = inventory.getItem(mainHandSlot);
            inventory.setItem(slot, previousMainHand);
            inventory.setItem(mainHandSlot, hammer);
            engineer.setItemInHand(InteractionHand.MAIN_HAND, hammer);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    private static void reportBlocked(SiegeEngineerEntity engineer, String reason, Component message) {
        CompoundTag data = engineer.getPersistentData();
        if (reason.equals(data.getString(BLOCK_REASON_TAG))) {
            return;
        }
        data.putString(BLOCK_REASON_TAG, reason);

        Player owner = engineer.getOwner();
        if (owner != null) {
            owner.displayClientMessage(message, false);
        }
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private static void clear(SiegeEngineerEntity engineer) {
        engineer.getPersistentData().remove(SITE_TAG);
        engineer.getPersistentData().remove(SITE_POS_TAG);
        engineer.getPersistentData().remove(BLOCK_REASON_TAG);
        RecruitsWalkOrders.stop(engineer);
        RecruitsTaskOrders.endTask(engineer);
    }

    static boolean cancelTask(SiegeEngineerEntity engineer) {
        if (!hasTask(engineer)) {
            return false;
        }
        clear(engineer);
        return true;
    }
}
