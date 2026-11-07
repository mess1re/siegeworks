package me.mss1r.siegeworks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
//? if <1.21 {
/*import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
*///?}
import java.util.List;

public final class RoundShotItem extends BlockItem {
    private final int caliberMm;

    public RoundShotItem(Block block, Properties properties, int caliberMm) {
        super(block, properties);
        if (caliberMm <= 0) throw new IllegalArgumentException("Caliber must be positive");
        this.caliberMm = caliberMm;
    }

    public int caliberMm() { return caliberMm; }

    public static int caliber(ItemStack stack) {
        if (!(stack.getItem() instanceof RoundShotItem shot)) return 0;
        // Fixed stone ammunition cannot be reclassified by item data.
        if (!(shot.getBlock() instanceof me.mss1r.siegeworks.block.CaliberShotBlock)) return shot.caliberMm;
        var data = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack);
        int value = data.contains("Caliber") ? data.getInt("Caliber") : shot.caliberMm;
        return me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize.valid(value) ? value : 0;
    }

    public static ItemStack withCaliber(ItemStack stack, int caliber) {
        if (!(stack.getItem() instanceof RoundShotItem shot)
                || !me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize.valid(caliber)
                || (!(shot.getBlock() instanceof me.mss1r.siegeworks.block.CaliberShotBlock) && caliber != shot.caliberMm)) {
            throw new IllegalArgumentException("Unsupported ammunition caliber: " + caliber);
        }
        if (caliber == shot.caliberMm && !me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack).contains("Caliber")) return stack;
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.editCustomData(stack, tag -> {
            if (caliber == shot.caliberMm) tag.remove("Caliber");
            else tag.putInt("Caliber", caliber);
        });
        return stack;
    }

    //? if <1.21 {
    /*@Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.siegeworks.round_shot.caliber", caliber(stack)).withStyle(ChatFormatting.GRAY));
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.siegeworks.round_shot.caliber", caliber(stack)).withStyle(ChatFormatting.GRAY));
    }
    //?}
}
