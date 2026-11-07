package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmStack;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class FirearmItem extends Item {
    private final ResourceLocation profile;
    private final boolean matchlock;

    public FirearmItem(String name, boolean matchlock, Properties properties) {
        super(properties.durability(matchlock ? 128 : 160));
        this.profile = MinecraftVersionCompat.id("siegeworks", name);
        this.matchlock = matchlock;
    }

    public ResourceLocation profile() { return profile; }
    public boolean matchlock() { return matchlock; }

    @Override
    public boolean isValidRepairItem(ItemStack weapon, ItemStack material) {
        return material.is(Items.IRON_INGOT);
    }

    @Override
    //? if <1.21 {
    /*public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
    *///?} else {
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
    //?}
        var load=FirearmStack.load(stack);
        lines.add(net.minecraft.network.chat.Component.translatable("item.siegeworks.round_shot.caliber",FirearmActions.caliber(this)).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(net.minecraft.network.chat.Component.translatable("firearm.siegeworks.charge."+load.charge().name().toLowerCase(java.util.Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(matchlock) lines.add(net.minecraft.network.chat.Component.translatable("firearm.siegeworks.match",load.matchShots(),
                net.minecraft.network.chat.Component.translatable(load.matchLit() ? "firearm.siegeworks.lit" : "firearm.siegeworks.unlit")).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(net.minecraft.network.chat.Component.translatable("firearm.siegeworks.offhand").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(stack);
        if (level.isClientSide && !FirearmActions.canBegin(player,stack,this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && !FirearmActions.begin(player, stack, this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override
    //? if <1.21 {
    /*public int getUseDuration(ItemStack stack) {
    *///?} else {
    public int getUseDuration(ItemStack stack, LivingEntity user) {
    //?}
        return 72000;
    }

    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!level.isClientSide && user instanceof Player player) FirearmActions.tick(player, stack, this);
    }

    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!level.isClientSide && user instanceof Player player) FirearmActions.cancel(player, stack);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (!level.isClientSide && holder instanceof Player player) {
            if (!player.isUsingItem() || player.getUseItem() != stack) FirearmActions.cancel(player, stack);
            if (player.isUnderWater()) {
                var load = FirearmStack.load(stack);
                if (load.matchLit()) FirearmStack.save(stack, load.extinguish());
            }
        }
    }
}
