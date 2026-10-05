package me.mss1r.siegeworks.gameplay.maintenance;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.axiomata.blueprint.internal.construction.MaterialAllocation;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Checks, takes and returns maintenance materials. A tag material accepts any item of the tag. */
final class MaintenanceMaterials {
    private MaintenanceMaterials() {
    }

    static boolean has(ServerPlayer player, List<Material> materials) {
        return MaterialAllocation.shortfall(carried(player), materials).isEmpty();
    }

    static boolean has(Container container, List<Material> materials) {
        return MaterialAllocation.shortfall(slots(container), materials).isEmpty();
    }

    static void consume(ServerPlayer player, List<Material> materials) {
        MaterialAllocation.take(carried(player), materials);
        player.getInventory().setChanged();
    }

    static void consume(Container container, List<Material> materials) {
        MaterialAllocation.take(slots(container), materials);
        container.setChanged();
    }

    /** Returns a material to {@code target}, dropping what does not fit; a tag returns its blueprint's return item. */
    static void returnOrDrop(AbstractSiegeEntity siege, ServerLevel level, @Nullable Container target,
                             Material material) {
        ItemStack remaining = material.returnStack();
        remaining.setCount(material.count());
        if (remaining.isEmpty()) {
            return;
        }
        if (target != null) {
            remaining = insert(target, remaining);
        }
        while (!remaining.isEmpty()) {
            int stackSize = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack drop = remaining.split(stackSize);
            ItemEntity itemEntity = new ItemEntity(
                    level, siege.getX(), siege.getY() + 0.5D, siege.getZ(), drop);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    /** Main inventory plus off hand. */
    private static List<ItemStack> carried(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
        stacks.addAll(player.getInventory().offhand);
        return stacks;
    }

    private static List<ItemStack> slots(Container container) {
        List<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            stacks.add(container.getItem(slot));
        }
        return stacks;
    }

    private static ItemStack insert(Container target, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < target.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = target.getItem(slot);
            if (!target.canPlaceItem(slot, remaining)) {
                continue;
            }

            if (existing.isEmpty()) {
                int moved = Math.min(remaining.getCount(),
                        Math.min(target.getMaxStackSize(), remaining.getMaxStackSize()));
                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                target.setItem(slot, inserted);
                remaining.shrink(moved);
                continue;
            }
            if (MinecraftVersionCompat.isSameItemAndData(existing, remaining)) {
                int limit = Math.min(target.getMaxStackSize(), existing.getMaxStackSize());
                int moved = Math.min(remaining.getCount(), limit - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        target.setChanged();
        return remaining;
    }
}
