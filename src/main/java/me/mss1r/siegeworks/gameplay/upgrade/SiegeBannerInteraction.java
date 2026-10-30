package me.mss1r.siegeworks.gameplay.upgrade;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public final class SiegeBannerInteraction {
    private SiegeBannerInteraction() {}

    @Nullable
    public static InteractionResult interact(AbstractSiegeEntity siege, SiegeBannerRack rack, Player player,
                                              InteractionHand hand, ServerLevel level, String messages) {
        ItemStack stack = player.getItemInHand(hand);
        boolean installing = stack.getItem() instanceof BannerItem;
        if (!installing && !stack.is(Items.SHEARS)) return null;
        if (!player.getAbilities().instabuild && !SiegeAccess.allows(player, siege, SiegeAccess.Action.USE)) {
            player.displayClientMessage(Component.translatable(messages + ".customization_not_owner"), true);
            return InteractionResult.SUCCESS;
        }
        if (installing) {
            int slot = rack.install(stack);
            if (slot < 0) {
                player.displayClientMessage(Component.translatable(messages + ".banners_full"), true);
                return InteractionResult.SUCCESS;
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, siege.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1, 1);
            player.displayClientMessage(Component.translatable(messages + ".banner_installed", slot + 1, rack.size()), true);
        } else {
            ItemStack removed = rack.removeLast();
            if (removed.isEmpty()) {
                player.displayClientMessage(Component.translatable(messages + ".no_banners"), true);
                return InteractionResult.SUCCESS;
            }
            if (!player.getInventory().add(removed)) player.drop(removed, false);
            MinecraftVersionCompat.damageHeldItem(stack, 1, player, hand);
            level.playSound(null, siege.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }
}
