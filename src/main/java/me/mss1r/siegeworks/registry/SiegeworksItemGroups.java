package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public interface SiegeworksItemGroups {
    DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Siegeworks.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private static ItemStack itemStack(ItemLike item) {
        return new ItemStack(item);
    }

    private static ItemStack ladderStack(int sections) {
        return SiegeLadderDeploymentItem.withSections(new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), sections);
    }

    RegistrySupplier<CreativeModeTab> SIEGEWORKS_ITEMS_TAB = CREATIVE_MODE_TABS.register("siegeworks_items",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(SiegeworksItems.RAMROD.get()))
                    .title(Component.translatable("component.itemgroup.siegeworks.tab.items"))
                    .displayItems((parameters, output) -> output.acceptAll(List.of(
                            itemStack(SiegeworksItems.BLACK_POWDER.get()),
                            itemStack(SiegeworksItems.WHEEL.get()),
                            itemStack(SiegeworksItems.BARREL.get()),
                            itemStack(SiegeworksItems.RAMROD.get()),
                            itemStack(SiegeworksItems.TOWER_CROSSBOW_BOLT.get()),
                            itemStack(SiegeworksItems.ARCBALLISTA_BOLT.get()),
                            itemStack(SiegeworksItems.SO_SINGIJEON.get()),
                            itemStack(SiegeworksItems.JUNG_SINGIJEON.get()),
                            itemStack(SiegeworksItems.FIRE_ARROW.get()),
                            itemStack(SiegeworksItems.CANNON_BALL.get()),
                            itemStack(SiegeworksItems.GIANT_CANNON_BALL.get()),
                            itemStack(SiegeworksItems.GRAPESHOT.get()),
                            itemStack(SiegeworksItems.CLAY_POT.get()),
                            itemStack(SiegeworksItems.FIRE_PROJECTILE.get())
                    )))
                    .build());

    RegistrySupplier<CreativeModeTab> SIEGEWORKS_ENGINES_TAB = CREATIVE_MODE_TABS.register("siegeworks_engines",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(SiegeworksItems.SERPENTINE_SPAWNER.get()))
                    .title(Component.translatable("component.itemgroup.siegeworks.tab.engines"))
                    .displayItems((parameters, output) -> output.acceptAll(List.of(
                            itemStack(SiegeworksItems.SERPENTINE_SPAWNER.get()),
                            itemStack(SiegeworksItems.CULVERIN_SPAWNER.get()),
                            itemStack(SiegeworksItems.BATTERING_RAM_SPAWNER.get()),
                            itemStack(SiegeworksItems.MANGONEL_SPAWNER.get()),
                            itemStack(SiegeworksItems.TREBUCHET_SPAWNER.get()),
                            itemStack(SiegeworksItems.TOWER_CROSSBOW_SPAWNER.get()),
                            itemStack(SiegeworksItems.ARCBALLISTA_SPAWNER.get()),
                            itemStack(SiegeworksItems.MANTLET_SPAWNER.get()),
                            itemStack(SiegeworksItems.MONS_MEG_SPAWNER.get()),
                            itemStack(SiegeworksItems.SIEGE_TOWER_SPAWNER.get()),
                            itemStack(SiegeworksItems.HWACHA_SPAWNER.get()),
                            itemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()),
                            ladderStack(1),
                            ladderStack(2),
                            ladderStack(3),
                            ladderStack(4)
                    )))
                    .build());

    static void register() {
        CREATIVE_MODE_TABS.register();
    }
}
