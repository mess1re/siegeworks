package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.entity.projectile.FireArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FireArrowItem extends ArrowItem {
    public FireArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if <1.21 {
    /*public AbstractArrow createArrow(Level level, ItemStack ammoStack, LivingEntity shooter) {
    *///?} else {
    public AbstractArrow createArrow(Level level, ItemStack ammoStack, LivingEntity shooter, ItemStack weapon) {
    //?}
        FireArrowEntity arrow = new FireArrowEntity(level, shooter);
        arrow.applyFlintAndSteelIgnition(shooter);
        return arrow;
    }
}
