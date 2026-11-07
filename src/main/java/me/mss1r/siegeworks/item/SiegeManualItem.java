package me.mss1r.siegeworks.item;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import me.mss1r.siegeworks.client.guide.SiegeManualScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class SiegeManualItem extends Item {
    public SiegeManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> SiegeManualScreen::open);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
