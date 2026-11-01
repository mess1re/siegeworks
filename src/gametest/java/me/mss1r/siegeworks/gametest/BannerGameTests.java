package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.gameplay.upgrade.SiegeBannerRack;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
//?}

public final class BannerGameTests {
    private static TrebuchetEntity trebuchet(GameTestHelper helper) {
        var siege = SiegeworksEntities.TREBUCHET_ENTITY.get().create(helper.getLevel());
        siege.setPos(helper.absoluteVec(new Vec3(15, 2, 15)));
        return siege;
    }

    private static ItemStack patterned(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.RED_BANNER);
        //? if <1.21 {
        /*stack.setHoverName(Component.literal("Counterweight standard"));
        var pattern = new CompoundTag();
        pattern.putString("Pattern", "cr");
        pattern.putInt("Color", 11);
        var layers = new net.minecraft.nbt.ListTag();
        layers.add(pattern);
        stack.getOrCreateTagElement("BlockEntityTag").put("Patterns", layers);
        *///?} else {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Counterweight standard"));
        stack.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder().add(
                helper.getLevel().registryAccess().registryOrThrow(Registries.BANNER_PATTERN)
                        .getHolderOrThrow(BannerPatterns.CROSS), DyeColor.BLUE).build());
        //?}
        return stack;
    }

    @GameTest(template = "empty")
    public static void installsTwoWithoutConsumingAThirdOrFiring(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        siege.setAmmoLoaded(SiegeAmmo.AMMO_STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER, 3));
        for (int n = 0; n < 3; n++) siege.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Installing consumed a third banner");
        helper.assertTrue(siege.getCounterweightBanner(0).is(Items.RED_BANNER)
                && siege.getCounterweightBanner(1).is(Items.RED_BANNER), "Both walls were not filled");
        helper.assertTrue(siege.getShootAnimationTick() == -1 && siege.hasAmmoLoaded(),
                "Banner interaction fired the loaded trebuchet");
        siege.setCooldown(100);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        siege.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(siege.getCounterweightBanner(1).isEmpty(), "Cooldown blocked banner removal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void patternsAndNameSurviveSaveAndNetworkData(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        var expected = patterned(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, expected.copy());
        siege.interact(player, InteractionHand.MAIN_HAND);
        CompoundTag saved = new CompoundTag();
        siege.addAdditionalSaveData(saved);
        var restored = trebuchet(helper);
        restored.readAdditionalSaveData(saved);
        var registries = helper.getLevel().registryAccess();
        var itemTag = MinecraftVersionCompat.writeItem(expected, registries);
        helper.assertTrue(itemTag.equals(MinecraftVersionCompat.writeItem(restored.getCounterweightBanner(0), registries)),
                "Reload lost the pattern, color or custom name");
        // Exercise the same client decoding path using the actual synchronized compound.
        var data = siege.getEntityData().getNonDefaultValues();
        CompoundTag wire = null;
        if (data != null) for (var value : data) {
            if (value.value() instanceof CompoundTag tag && tag.contains("CounterweightBanners")) wire = tag;
        }
        helper.assertTrue(wire != null, "Banners were not synchronized");
        final CompoundTag synchronizedTag = wire;
        var clientRack = new SiegeBannerRack(new SiegeBannerRack.Host() {
            public boolean clientSide() { return true; }
            public RegistryAccess registryAccess() { return registries; }
            public CompoundTag synchronizedData() { return synchronizedTag; }
            public void setSynchronizedData(CompoundTag tag) { throw new AssertionError("Client wrote entity data"); }
            public void drop(ItemStack stack) { throw new AssertionError("Client dropped an item"); }
        }, 2, "CounterweightBanners");
        helper.assertTrue(itemTag.equals(MinecraftVersionCompat.writeItem(clientRack.get(0), registries)),
                "Client synchronization lost banner metadata");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shearsReturnLastBannerOnlyOnce(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER));
        siege.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_BANNER));
        siege.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        siege.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(siege.getCounterweightBanner(0).is(Items.RED_BANNER)
                && siege.getCounterweightBanner(1).isEmpty(), "Shears removed the wrong wall's banner");
        siege.interact(player, InteractionHand.MAIN_HAND);
        siege.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getInventory().countItem(Items.RED_BANNER) == 1
                && player.getInventory().countItem(Items.BLUE_BANNER) == 1, "Removal lost or duplicated banners");
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 2, "Empty removal damaged shears");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hostilePlayerCannotInstallOrRemove(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var owner = SiegeGameTestPlayers.createRideable(helper.getLevel());
        var stranger = SiegeGameTestPlayers.createRideable(helper.getLevel());
        siege.setOwnerUuid(owner.getUUID());
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER));
        siege.interact(owner, InteractionHand.MAIN_HAND);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_BANNER));
        siege.interact(stranger, InteractionHand.MAIN_HAND);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        siege.interact(stranger, InteractionHand.MAIN_HAND);
        helper.assertTrue(siege.getCounterweightBanner(0).is(Items.RED_BANNER)
                && siege.getCounterweightBanner(1).isEmpty(), "Foreign player changed banners");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeInstallationDoesNotConsume(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        player.getAbilities().instabuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER));
        siege.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1
                && siege.getCounterweightBanner(0).is(Items.RED_BANNER), "Creative installation consumed the banner");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unloadPreservesAndDestructionDropsExactlyOnce(GameTestHelper helper) {
        var siege = trebuchet(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER));
        siege.interact(player, InteractionHand.MAIN_HAND);
        var area = siege.getBoundingBox().inflate(5);
        siege.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.getItem().is(Items.RED_BANNER)).isEmpty(), "Unloading dropped the banner");
        var saved = new CompoundTag();
        siege.addAdditionalSaveData(saved);
        var restored = trebuchet(helper);
        restored.readAdditionalSaveData(saved);
        helper.getLevel().addFreshEntity(restored);
        restored.remove(Entity.RemovalReason.KILLED);
        restored.remove(Entity.RemovalReason.KILLED);
        helper.runAfterDelay(1, () -> {
            var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                    entity -> entity.getItem().is(Items.RED_BANNER));
            helper.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 1,
                    "Destruction lost or duplicated the banner: drops=" + drops
                            + ", stored=" + restored.getCounterweightBanner(0) + ", pos=" + restored.position());
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void towerKeepsLegacyStorageAndShearsPriority(GameTestHelper helper) {
        var tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER, 4));
        for (int n = 0; n < 4; n++) tower.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER));
        tower.interact(player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        tower.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(tower.getLeatherBottomProgress() == 0 && tower.getTowerBanner(3).is(Items.RED_BANNER),
                "Shift-shears removed a banner instead of leather");
        var saved = new CompoundTag();
        tower.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getList("TowerBanners", 10).size() == 4, "Tower storage key changed");
        var restored = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        restored.readAdditionalSaveData(saved);
        for (int n = 0; n < 4; n++) helper.assertTrue(restored.getTowerBanner(n).is(Items.RED_BANNER),
                "Old tower data no longer restores");
        helper.succeed();
    }
}
