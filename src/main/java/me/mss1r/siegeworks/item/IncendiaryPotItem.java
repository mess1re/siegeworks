package me.mss1r.siegeworks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if <1.21 {
/*import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
*///?}
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Clay or incendiary pot item; the tooltip shows its contents. */
public class IncendiaryPotItem extends BlockItem {
    public IncendiaryPotItem(Block block, Properties properties) {
        super(block, properties);
    }

    //? if <1.21 {
    /*@Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.addAll(PotFilling.of(stack).describe());
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.addAll(PotFilling.of(stack).describe());
    }
    //?}
}
