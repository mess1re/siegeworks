package me.mss1r.siegeworks.registry;

import me.mss1r.siegeworks.Siegeworks;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.item.FireArrowItem;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import me.mss1r.siegeworks.item.IncendiaryPotItem;
import net.minecraft.world.item.Item;

public interface SiegeworksItems {
    DeferredRegister<Item> ITEMS = DeferredRegister.create(Siegeworks.MOD_ID, Registries.ITEM);

    RegistrySupplier<Item> BLACK_POWDER = ITEMS.register("black_powder", () -> new Item(new Item.Properties()));
    RegistrySupplier<Item> WHEEL = ITEMS.register("wheel", () -> new Item(new Item.Properties()));
    RegistrySupplier<Item> BARREL = ITEMS.register("barrel", () -> new Item(new Item.Properties()));
    RegistrySupplier<Item> SERPENTINE_SPAWNER = ITEMS.register("serpentine_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.SERPENTINE_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> CULVERIN_SPAWNER = ITEMS.register("culverin_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.CULVERIN_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> BATTERING_RAM_SPAWNER = ITEMS.register("battering_ram_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.BATTERING_RAM_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> MANGONEL_SPAWNER = ITEMS.register("mangonel_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.MANGONEL_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> TREBUCHET_SPAWNER = ITEMS.register("trebuchet_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.TREBUCHET_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> TOWER_CROSSBOW_SPAWNER = ITEMS.register("tower_crossbow_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.TOWER_CROSSBOW_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> ARCBALLISTA_SPAWNER = ITEMS.register("arcballista_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.ARCBALLISTA_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> MANTLET_SPAWNER = ITEMS.register("mantlet_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.MANTLET_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> MONS_MEG_SPAWNER = ITEMS.register("mons_meg_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.MONS_MEG_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> SIEGE_TOWER_SPAWNER = ITEMS.register("siege_tower_spawner", () -> new SiegeDeploymentItem(SiegeworksEntities.SIEGE_TOWER_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> SIEGE_LADDER_SPAWNER = ITEMS.register("siege_ladder_spawner", () -> new SiegeLadderDeploymentItem(new Item.Properties()));
    RegistrySupplier<Item> HWACHA_SPAWNER = ITEMS.register("hwacha_spawner", () ->
            new SiegeDeploymentItem(SiegeworksEntities.HWACHA_ENTITY, new Item.Properties()));
    RegistrySupplier<Item> TOWER_CROSSBOW_BOLT = ITEMS.register("tower_crossbow_bolt", () ->
            new Item(new Item.Properties().stacksTo(16)));
    RegistrySupplier<Item> ARCBALLISTA_BOLT = ITEMS.register("arcballista_bolt", () ->
            new Item(new Item.Properties().stacksTo(16)));
    RegistrySupplier<Item> SO_SINGIJEON = ITEMS.register("so_singijeon", () ->
            new Item(new Item.Properties().stacksTo(64)));
    RegistrySupplier<Item> JUNG_SINGIJEON = ITEMS.register("jung_singijeon", () ->
            new Item(new Item.Properties().stacksTo(64)));
    RegistrySupplier<Item> FIRE_ARROW = ITEMS.register("fire_arrow", () -> new FireArrowItem(new Item.Properties()));
    RegistrySupplier<Item> CANNON_BALL = ITEMS.register("cannon_ball", () -> new BlockItem(SiegeworksBlocks.CANNON_BALL.get(), new Item.Properties().stacksTo(8)));
    RegistrySupplier<Item> GIANT_CANNON_BALL = ITEMS.register("giant_cannon_ball", () -> new BlockItem(SiegeworksBlocks.GIANT_CANNON_BALL.get(), new Item.Properties().stacksTo(2)));
    RegistrySupplier<Item> GRAPESHOT = ITEMS.register("grapeshot", () ->
            new BlockItem(SiegeworksBlocks.GRAPESHOT.get(), new Item.Properties().stacksTo(2)));
    /** Incendiary pot sealed with a wick. Without a wick it's a {@link #CLAY_POT}, empty or partly filled. */
    RegistrySupplier<Item> FIRE_PROJECTILE = ITEMS.register("fire_projectile", () ->
            new IncendiaryPotItem(SiegeworksBlocks.FIRE_PROJECTILE.get(), new Item.Properties().stacksTo(1)));
    RegistrySupplier<Item> CLAY_POT = ITEMS.register("clay_pot", () ->
            new IncendiaryPotItem(SiegeworksBlocks.FIRE_PROJECTILE.get(), new Item.Properties().stacksTo(1)));
    RegistrySupplier<Item> RAMROD = ITEMS.register("ramrod", () -> new Item(new Item.Properties().stacksTo(1).durability(256)));

    static void registerItems() {
        ITEMS.register();
        Siegeworks.LOG.info("Registering Siegeworks items");
    }
}
