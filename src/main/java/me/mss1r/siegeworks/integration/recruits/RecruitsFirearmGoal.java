package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmLoad;
import me.mss1r.siegeworks.gameplay.firearm.FirearmStack;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.platform.SiegePlatform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Recruits choose targets and supplies; the shared firearm executor performs every operation. */
public final class RecruitsFirearmGoal extends Goal {
    public static final double MELEE_RANGE_SQR = 25;
    private static final String BORROWED_MATERIAL = "SiegeworksFirearmBorrow";
    private final AbstractRecruitEntity recruit;
    private BorrowedMaterial borrowed;
    private int aimTicks;
    private long nextAttempt;

    public RecruitsFirearmGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        recoverBorrowedMaterial();
        if (!(recruit.getMainHandItem().getItem() instanceof FirearmItem gun)
                || !recruit.getShouldRanged() || recruit.getState() == 3
                || recruit.needsToGetFood() || recruit.getShouldMount() || recruit.getShouldBlock()
                || recruit.getShouldMovePos() || recruit.isPassenger() || recruit.isUnderWater()) return false;
        if (recruit.getShouldFollow() && recruit.getOwner() != null
                && recruit.distanceToSqr(recruit.getOwner()) > 300) return false;
        if (recruit.getShouldHoldPos() && recruit.getHoldPos() != null
                && recruit.position().distanceToSqr(recruit.getHoldPos()) > 4) return false;
        LivingEntity target = recruit.getTarget();
        if (target != null && target.isAlive() && recruit.distanceToSqr(target) < MELEE_RANGE_SQR) return false;
        if (recruit.isUsingItem()) return true;
        var load = FirearmStack.load(recruit.getMainHandItem());
        var operation = nextOperation(load, gun);
        if (operation != null) return hasSupply(stack -> FirearmActions.operation(load, gun, stack) == operation);
        return target != null && target.isAlive() && recruit.canAttack(target)
                && (gun.matchlock() || hasSupply(stack -> stack.is(Items.FLINT_AND_STEEL)));
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void stop() {
        FirearmActions.cancelActive(recruit);
        recruit.stopUsingItem();
        restoreMaterial();
        recruit.setAggressive(false);
        recruit.getNavigation().stop();
        aimTicks = 0;
    }

    @Override public void tick() {
        ItemStack weapon = recruit.getMainHandItem();
        FirearmItem gun = (FirearmItem) weapon.getItem();
        var load = FirearmStack.load(weapon);
        LivingEntity target = recruit.getTarget();
        boolean hasTarget = target != null && target.isAlive() && !target.isSpectator() && recruit.canAttack(target);
        double range = gun.matchlock() ? 64 : 48;
        boolean canAim = load.charge() == FirearmLoad.Charge.READY && hasTarget && recruit.distanceToSqr(target) <= range * range
                && recruit.getSensing().hasLineOfSight(target) && clearShot(target);

        if (borrowed != null && !borrowed.intact()) {
            stop();
            return;
        }
        if (recruit.isUsingItem()) {
            String action = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(weapon).getString("FirearmAction");
            if (action.equals("AIM") || action.equals("FIRE")) {
                if (!canAim && load.charge() == FirearmLoad.Charge.READY) {
                    stop();
                    return;
                }
                if (canAim) aimAt(target);
                if (action.equals("AIM") && ++aimTicks >= 20) FirearmActions.requestFire(recruit);
            }
            return;
        }

        restoreMaterial();
        recruit.setAggressive(false);
        if (recruit.level().getGameTime() < nextAttempt) return;

        FirearmLoad.Operation operation = nextOperation(load, gun);
        if (operation != null) {
            if (supply(stack -> FirearmActions.operation(load, gun, stack) == operation)) {
                recruit.getNavigation().stop();
                if (!FirearmActions.beginOperation(recruit, operation)) restoreMaterial();
            } else nextAttempt = recruit.level().getGameTime() + 20;
            return;
        }
        if (!canAim) {
            aimTicks = 0;
            if (hasTarget && !recruit.getShouldHoldPos()
                    && (!recruit.getShouldFollow() || recruit.getOwner() == null
                    || recruit.distanceToSqr(recruit.getOwner()) <= 100)) {
                recruit.getNavigation().moveTo(target, 1.0);
            }
            return;
        }
        if (!gun.matchlock() && !supply(stack -> stack.is(Items.FLINT_AND_STEEL))) {
            nextAttempt = recruit.level().getGameTime() + 20;
            return;
        }
        recruit.getNavigation().stop();
        aimAt(target);
        aimTicks = 0;
        if (FirearmActions.beginOperation(recruit, null)) recruit.setAggressive(true);
        else restoreMaterial();
    }

    private FirearmLoad.Operation nextOperation(FirearmLoad load, FirearmItem gun) {
        if (load.charge() == FirearmLoad.Charge.EMPTY) return FirearmLoad.Operation.POWDER;
        if (load.charge() == FirearmLoad.Charge.POWDER) return FirearmLoad.Operation.BALL;
        if (load.charge() == FirearmLoad.Charge.BALL) return FirearmLoad.Operation.RAM;
        if (gun.matchlock() && load.matchShots() == 0) return FirearmLoad.Operation.INSERT_MATCH;
        if (gun.matchlock() && !load.matchLit()) return FirearmLoad.Operation.LIGHT_MATCH;
        return null;
    }

    private void aimAt(LivingEntity target) {
        Vec3 delta = target.position().add(0, target.getBbHeight() * .6, 0).subtract(recruit.getEyePosition());
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) (-Mth.atan2(delta.y, Math.sqrt(delta.horizontalDistanceSqr())) * Mth.RAD_TO_DEG);
        recruit.setYRot(yaw);
        recruit.setYHeadRot(yaw);
        recruit.setXRot(pitch);
        recruit.getLookControl().setLookAt(target.getX(), target.getY() + target.getBbHeight() * .6, target.getZ());
    }

    private boolean clearShot(LivingEntity target) {
        Vec3 start = recruit.getEyePosition();
        Vec3 end = target.position().add(0, target.getBbHeight() * .6, 0);
        if (recruit.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, recruit)).getType() != HitResult.Type.MISS) return false;
        for (LivingEntity other : recruit.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(start, end).inflate(.3), entity -> entity != recruit && entity != target && entity.isAlive())) {
            if ((!recruit.canAttack(other) || recruit.isAlliedTo(other))
                    && other.getBoundingBox().inflate(.1).clip(start, end).isPresent()) return false;
        }
        return true;
    }

    private boolean supply(java.util.function.Predicate<ItemStack> matches) {
        if (matches.test(recruit.getOffhandItem())) return true;
        var inventory = recruit.getInventory();
        for (int slot = recruit.getInventorySlotIndex(EquipmentSlot.MAINHAND) + 1; slot < inventory.getContainerSize(); slot++) {
            ItemStack material = inventory.getItem(slot);
            if (!matches.test(material)) continue;
            borrowed = new BorrowedMaterial(slot, material, recruit.getOffhandItem());
            var saved = new net.minecraft.nbt.CompoundTag();
            saved.putInt("Slot", slot);
            saved.putString("Material", itemId(material));
            saved.putString("Displaced", itemId(borrowed.displaced));
            SiegePlatform.persistentData(recruit).put(BORROWED_MATERIAL, saved);
            inventory.setItem(slot, borrowed.displaced);
            recruit.setItemInHand(InteractionHand.OFF_HAND, material);
            return true;
        }
        return false;
    }

    private boolean hasSupply(java.util.function.Predicate<ItemStack> matches) {
        if (matches.test(recruit.getOffhandItem())) return true;
        var inventory = recruit.getInventory();
        for (int slot = recruit.getInventorySlotIndex(EquipmentSlot.MAINHAND) + 1; slot < inventory.getContainerSize(); slot++) {
            if (matches.test(inventory.getItem(slot))) return true;
        }
        return false;
    }

    private void restoreMaterial() {
        if (borrowed == null) return;
        if (borrowed.intact()) {
            recruit.getInventory().setItem(borrowed.slot, recruit.getOffhandItem());
            recruit.setItemInHand(InteractionHand.OFF_HAND, borrowed.displaced);
        }
        borrowed = null;
        SiegePlatform.persistentData(recruit).remove(BORROWED_MATERIAL);
    }

    /** Unloading a chunk loses the goal's session, but both actual stacks remain in the saved inventory. */
    private void recoverBorrowedMaterial() {
        var data = SiegePlatform.persistentData(recruit);
        if (borrowed != null || !data.contains(BORROWED_MATERIAL)) return;
        var saved = data.getCompound(BORROWED_MATERIAL);
        int slot = saved.getInt("Slot");
        var inventory = recruit.getInventory();
        if (slot > recruit.getInventorySlotIndex(EquipmentSlot.MAINHAND) && slot < inventory.getContainerSize()
                && itemId(inventory.getItem(slot)).equals(saved.getString("Displaced"))
                && (recruit.getOffhandItem().isEmpty() || itemId(recruit.getOffhandItem()).equals(saved.getString("Material")))) {
            ItemStack displaced = inventory.getItem(slot);
            inventory.setItem(slot, recruit.getOffhandItem());
            recruit.setItemInHand(InteractionHand.OFF_HAND, displaced);
        }
        data.remove(BORROWED_MATERIAL);
        FirearmActions.cancel(recruit, recruit.getMainHandItem());
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private final class BorrowedMaterial {
        final int slot;
        final ItemStack material, displaced;
        BorrowedMaterial(int slot, ItemStack material, ItemStack displaced) {
            this.slot = slot;
            this.material = material;
            this.displaced = displaced;
        }
        boolean intact() {
            return same(recruit.getInventory().getItem(slot), displaced) && same(recruit.getOffhandItem(), material);
        }
        private static boolean same(ItemStack first, ItemStack second) {
            return first == second || first.isEmpty() && second.isEmpty();
        }
    }
}
