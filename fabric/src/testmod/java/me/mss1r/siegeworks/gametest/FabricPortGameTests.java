package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.testmixin.FireBlockInvoker;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.block.IncendiaryPotBlockEntity;
import me.mss1r.siegeworks.fabric.FabricPotState;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class FabricPortGameTests implements FabricGameTest {
    private static final String TEMPLATE = "siegeworks_fabric_tests:empty";
    private static final String POT_BATCH = "fabric_pots";
    private static SiegeBlockDamage previousRule;

    @GameTest(template=TEMPLATE)
    public void legacyRegistryNamesResolveToCanonicalValues(GameTestHelper helper) {
        me.mss1r.siegeworks.registry.LegacyIdAliases.all().forEach((registryKey, aliases) -> {
            var registry = BuiltInRegistries.REGISTRY.get(registryKey.location());
            aliases.forEach((oldName, name) -> checkAlias(helper, registry, oldName, name));
        });
        helper.succeed();
    }

    private static <T> void checkAlias(GameTestHelper helper, net.minecraft.core.Registry<T> registry,
                                      net.minecraft.resources.ResourceLocation oldName,
                                      net.minecraft.resources.ResourceLocation name) {
        var oldKey = net.minecraft.resources.ResourceKey.create(registry.key(), oldName);
        var key = net.minecraft.resources.ResourceKey.create(registry.key(), name);
        helper.assertTrue(registry.containsKey(oldName) && registry.containsKey(oldKey)
                && registry.get(oldName) == registry.get(name) && registry.get(oldKey) == registry.get(key)
                && registry.getHolder(oldKey).equals(registry.getHolder(key))
                && name.equals(registry.getKey(registry.get(oldName))), "Registry alias failed: " + oldName);
    }
    @BeforeBatch(batch=POT_BATCH)
    public static void enableFire(ServerLevel level) {
        previousRule = SiegeworksServerConfig.getBlockDamage();
        SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
    }
    @AfterBatch(batch=POT_BATCH)
    public static void restoreRule(ServerLevel level) { SiegeworksServerConfig.setBlockDamage(previousRule); }

    private static IncendiaryPotBlockEntity pot(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, SiegeworksBlocks.FIRE_PROJECTILE.get());
        return (IncendiaryPotBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    @GameTest(template=TEMPLATE)
    public void drawingCatalogAndCreativeTabsAreRegistered(GameTestHelper helper) {
        for (String name : List.of("arcballista", "battering_ram", "culverin", "hwacha", "mantlet",
                "mons_meg", "onager", "serpentine", "siege_tower", "tower_crossbow", "trebuchet",
                "hand_cannon", "arquebus"))
            helper.assertTrue(BlueprintDefinitions.get("siegeworks:" + name) != null,
                    "Blueprint is missing: " + name);
        for (String name : List.of("siegeworks_items", "siegeworks_engines"))
            helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(MinecraftVersionCompat.id("siegeworks", name)),
                    "Creative tab is missing: " + name);
        helper.succeed();
    }
    @GameTest(template=TEMPLATE, batch=POT_BATCH)
    public void fillingControlsVanillaFireOddsImmediately(GameTestHelper helper) {
        var pot = pot(helper, new BlockPos(5, 1, 5));
        var fire = (FireBlockInvoker)Blocks.FIRE;
        helper.assertTrue(fire.siegeworks$burnOdds(pot.getBlockState()) == 0, "Empty pot burns");
        pot.setFilling(PotFilling.standard());
        var filled = helper.getLevel().getBlockState(pot.getBlockPos());
        helper.assertTrue(filled.getValue(FabricPotState.HAS_FUEL), "Filling did not update the fuel state");
        helper.assertTrue(fire.siegeworks$burnOdds(filled) == IncendiaryPotBlock.FLAMMABILITY
                && fire.siegeworks$spreadOdds(filled) == IncendiaryPotBlock.FIRE_SPREAD_SPEED,
                "Vanilla fire odds differ from native loaders");
        pot.setFilling(PotFilling.EMPTY);
        helper.assertTrue(fire.siegeworks$burnOdds(helper.getLevel().getBlockState(pot.getBlockPos())) == 0,
                "Emptying the pot left it flammable");
        helper.succeed();
    }
    @GameTest(template=TEMPLATE, batch=POT_BATCH)
    public void neighboringFireDetonatesFilledPots(GameTestHelper helper) {
        var pot = pot(helper, new BlockPos(5, 1, 5));
        pot.setFilling(PotFilling.standard());
        ((FireBlockInvoker)Blocks.FIRE).siegeworks$ignite(helper.getLevel(), pot.getBlockPos(), 1,
                net.minecraft.util.RandomSource.create(0), 0);
        helper.assertTrue(pot.isDetonating() && pot.filling().hasBase()
                && !helper.getLevel().getBlockState(pot.getBlockPos()).is(SiegeworksBlocks.FIRE_PROJECTILE.get()),
                "Fire did not detonate the filled pot through its normal incendiary effect");
        helper.succeed();
    }
    @GameTest(template=TEMPLATE, batch=POT_BATCH)
    public void neighboringFireLeavesEmptyPotsIntact(GameTestHelper helper) {
        var pot = pot(helper, new BlockPos(5, 1, 5));
        ((FireBlockInvoker)Blocks.FIRE).siegeworks$ignite(helper.getLevel(), pot.getBlockPos(), 1,
                net.minecraft.util.RandomSource.create(0), 0);
        helper.assertTrue(!pot.isLit() && helper.getLevel().getBlockEntity(pot.getBlockPos()) == pot,
                "Fire ignited or destroyed an empty pot");
        helper.succeed();
    }
    @GameTest(template=TEMPLATE, batch=POT_BATCH, timeoutTicks=60)
    public void explosionSetsOffNeighboringFilledPots(GameTestHelper helper) {
        var first = pot(helper, new BlockPos(4, 1, 4));
        var second = pot(helper, new BlockPos(5, 1, 4));
        first.setFilling(PotFilling.standard());
        second.setFilling(PotFilling.standard());
        Vec3 blast = Vec3.atCenterOf(first.getBlockPos()).add(0, 0, 1);
        helper.getLevel().explode(null, blast.x, blast.y, blast.z, 1.5F, Level.ExplosionInteraction.TNT);
        helper.succeedWhen(() -> {
            helper.assertTrue(!helper.getLevel().getBlockState(first.getBlockPos()).is(SiegeworksBlocks.FIRE_PROJECTILE.get())
                    && !helper.getLevel().getBlockState(second.getBlockPos()).is(SiegeworksBlocks.FIRE_PROJECTILE.get()),
                    "Explosion did not set off both pots");
            boolean fire = false;
            for (var pos : BlockPos.betweenClosed(first.getBlockPos().offset(-4, -1, -4), first.getBlockPos().offset(4, 2, 4)))
                if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) fire = true;
            helper.assertTrue(fire, "Pots disappeared without incendiary effects");
        });
    }
}
