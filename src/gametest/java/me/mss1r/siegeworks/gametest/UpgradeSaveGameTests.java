package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.UUID;
import java.util.Map;

@GameTestHolder(UpgradeSaveGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class UpgradeSaveGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_datapacks";
    public static final String ITEM_MIGRATION_NAMESPACE = Siegeworks.MOD_ID + "_migrations";

    @GameTest(template = "empty", templateNamespace = ITEM_MIGRATION_NAMESPACE)
    public static void renamedSingijeonStacksKeepTheirCountAndData(GameTestHelper helper) {
        Map<String, Item> renamed = Map.of(
                "singijeon", SiegeworksItems.SO_SINGIJEON.get(),
                "explosive_singijeon", SiegeworksItems.JUNG_SINGIJEON.get(),
                "mangonel_spawner", SiegeworksItems.ONAGER_SPAWNER.get());
        renamed.forEach((oldPath, item) -> {
            String oldId = Siegeworks.MOD_ID + ":" + oldPath;
            CompoundTag old = new CompoundTag();
            old.putString("id", oldId);
            CompoundTag data = new CompoundTag();
            data.putString("migration_test", "kept");
            //? if <1.21 {
            /*old.putByte("Count", (byte) 17);
            old.put("tag", data);
            ItemStack stack = ItemStack.of(old);
            CompoundTag saved = stack.save(new CompoundTag());
            CompoundTag savedData = saved.getCompound("tag");
            *///?} else {
            old.putInt("count", 17);
            CompoundTag components = new CompoundTag();
            components.put("minecraft:custom_data", data);
            old.put("components", components);
            ItemStack stack = ItemStack.parseOptional(helper.getLevel().registryAccess(), old);
            CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
            CompoundTag savedData = saved.getCompound("components").getCompound("minecraft:custom_data");
            //?}
            helper.assertTrue(stack.is(item) && stack.getCount() == 17,
                    "Legacy stack did not resolve with its count: " + oldId);
            helper.assertTrue(savedData.getString("migration_test").equals("kept"),
                    "Legacy stack lost custom data: " + oldId);
            helper.assertTrue(saved.getString("id").equals(BuiltInRegistries.ITEM.getKey(item).toString()),
                    "Stack was saved under its old ID: " + oldId);
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = ITEM_MIGRATION_NAMESPACE)
    public static void oldSingijeonIdsAreAliasesNotDuplicateItems(GameTestHelper helper) {
        Map<String, Item> renamed = Map.of(
                "singijeon", SiegeworksItems.SO_SINGIJEON.get(),
                "explosive_singijeon", SiegeworksItems.JUNG_SINGIJEON.get(),
                "mangonel_spawner", SiegeworksItems.ONAGER_SPAWNER.get());
        renamed.forEach((oldPath, item) -> {
            //? if <1.21 {
            /*ResourceLocation oldId = new ResourceLocation(Siegeworks.MOD_ID, oldPath);
            *///?} else {
            ResourceLocation oldId = MinecraftVersionCompat.id(Siegeworks.MOD_ID, oldPath);
            //?}
            helper.assertTrue(BuiltInRegistries.ITEM.get(oldId) == item,
                    "Legacy ID does not resolve: " + oldId);
            helper.assertTrue(!BuiltInRegistries.ITEM.keySet().contains(oldId),
                    "Legacy ID was registered as a second item: " + oldId);
        });
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = ITEM_MIGRATION_NAMESPACE)
    public static void oldMangonelIdsAreAliasesOfTheOnager(GameTestHelper helper) {
        assertAliases(helper, BuiltInRegistries.ENTITY_TYPE, Map.of(
                "mangonel", SiegeworksEntities.ONAGER_ENTITY.get(),
                "mangonel_projectile", SiegeworksEntities.ONAGER_PROJECTILE.get(),
                "mangonel_passenger_projectile", SiegeworksEntities.ONAGER_PASSENGER_PROJECTILE.get()));
        assertAliases(helper, BuiltInRegistries.SOUND_EVENT, Map.of(
                "mangonel_shoot", SiegeworksSounds.ONAGER_SHOOT.get(),
                "mangonel_reload", SiegeworksSounds.ONAGER_RELOAD.get()));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = ITEM_MIGRATION_NAMESPACE)
    public static void savedMangonelLoadsAsAnOnager(GameTestHelper helper) {
        CompoundTag old = new CompoundTag();
        old.putString("id", Siegeworks.MOD_ID + ":mangonel");
        var loaded = EntityType.create(old, helper.getLevel()).orElse(null);
        helper.assertTrue(loaded instanceof OnagerEntity, "A saved mangonel did not load as an onager: " + loaded);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = ITEM_MIGRATION_NAMESPACE)
    public static void catapultBlueprintsAndSitesReadAsTheOnager(GameTestHelper helper) {
        String old = Siegeworks.MOD_ID + ":catapult";
        String onager = Siegeworks.MOD_ID + ":onager";
        helper.assertTrue(BlueprintDefinitions.canonicalId(old).equals(onager),
                "The catapult blueprint id does not lead to the onager");
        helper.assertTrue(BlueprintDefinitions.get(old) != null
                        && BlueprintDefinitions.get(old) == BlueprintDefinitions.get(onager),
                "A drawn catapult blueprint does not read as the onager's");
        helper.succeed();
    }

    private static <T> void assertAliases(GameTestHelper helper, Registry<T> registry, Map<String, T> renamed) {
        renamed.forEach((oldPath, value) -> {
            ResourceLocation oldId = MinecraftVersionCompat.id(Siegeworks.MOD_ID, oldPath);
            helper.assertTrue(registry.get(oldId) == value, "Legacy ID does not resolve: " + oldId);
            helper.assertTrue(!registry.keySet().contains(oldId), "Legacy ID was registered twice: " + oldId);
        });
    }

    @GameTest(template = "empty")
    public static void beta5DeploymentOwnerDoesNotBecomeTheRecruitOperator(GameTestHelper helper) {
        var ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(ballista != null, "No ballista for the upgrade test");
        UUID owner = UUID.randomUUID();
        UUID recruit = UUID.randomUUID();
        CompoundTag old = new CompoundTag();
        old.putUUID("DeploymentOwner", owner);
        old.putString("DeploymentGroup", "test_squad");
        old.putUUID("Owner", recruit);
        ballista.readAdditionalSaveData(old);
        helper.assertTrue(ballista.isOwnedBy(owner), "The recruit's UUID replaced its commander's ownership");
        CompoundTag saved = new CompoundTag();
        ballista.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getUUID("SiegeOwner").equals(owner) && saved.getUUID("Owner").equals(recruit),
                "Migrated save did not retain separate owner and operator");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oldEmbeddedBoltKeepsItsStateAndStoredDamage(GameTestHelper helper) {
        var bolt = SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get().create(helper.getLevel());
        helper.assertTrue(bolt != null, "No bolt for the upgrade test");
        CompoundTag old = new CompoundTag();
        bolt.addAdditionalSaveData(old);
        old.putDouble("BaseDamage", 36.0D);
        old.putBoolean("InGround", true);
        old.putBoolean("FiredBySiege", true);
        bolt.readAdditionalSaveData(old);
        CompoundTag saved = new CompoundTag();
        bolt.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getDouble("BaseDamage") == 36.0D && saved.getBoolean("InGround"),
                "The old bolt lost its embedded state or damage calibration");
        helper.succeed();
    }
}
