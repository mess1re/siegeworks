package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.upgrade.EngineFinishController;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EngineFinishGameTests {
    @GameTest(template = "empty")
    public static void weatheringTakesItsShareOfHealthButNeverTheEngine(GameTestHelper helper) {
        AbstractSiegeEntity full = engine(helper, SiegeworksEntities.CULVERIN_ENTITY.get(), new BlockPos(2, 1, 2));
        float max = full.getMaxHealth();
        full.finish().weather();
        float expected = max - (float) (max * SiegeworksServerConfig.getWeatheringHealthLoss());
        helper.assertTrue(full.finish().weathered() && Math.abs(full.getHealth() - expected) < 1.0E-3F,
                "A weathered engine did not lose its share of health: " + full.getHealth() + " of " + max);

        AbstractSiegeEntity worn = engine(helper, SiegeworksEntities.CULVERIN_ENTITY.get(), new BlockPos(5, 1, 2));
        worn.setHealth(2.0F);
        worn.finish().weather();
        helper.assertTrue(worn.getHealth() == 1.0F, "Weathering took a worn engine below 1: " + worn.getHealth());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTheOpenSkyWeathersAnEngine(GameTestHelper helper) {
        double threshold = SiegeworksServerConfig.getWeatheringDays() * 24000.0D;
        // Test structures are laid out under one another, so the open engine is put above everything there
        AbstractSiegeEntity open = engine(helper, SiegeworksEntities.TREBUCHET_ENTITY.get(), new BlockPos(2, 1, 2));
        open.setPos(open.getX(), helper.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING,
                open.getBlockX(), open.getBlockZ()) + 1.0D, open.getZ());
        AbstractSiegeEntity roofed = engine(helper, SiegeworksEntities.MANTLET_ENTITY.get(), new BlockPos(6, 1, 2));
        BlockPos roof = BlockPos.containing(roofed.getX(), roofed.getBoundingBox().maxY + 1.0D, roofed.getZ());
        helper.getLevel().setBlock(roof, Blocks.STONE.defaultBlockState(), 3);
        for (AbstractSiegeEntity engine : new AbstractSiegeEntity[]{open, roofed}) {
            engine.finish().setExposure(threshold - 1.0D);
            engine.finish().tick(helper.getLevel());
        }
        helper.assertTrue(open.finish().weathered(), "An engine under the open sky did not weather");
        helper.assertTrue(!roofed.finish().weathered() && roofed.finish().exposure() == threshold - 1.0D,
                "Time under a roof counted towards weathering");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void honeycombWaxesBitByBitAndWaxKeepsTheWeatherOut(GameTestHelper helper) {
        AbstractSiegeEntity hwacha = engine(helper, SiegeworksEntities.HWACHA_ENTITY.get(), new BlockPos(2, 1, 2));
        int required = hwacha.finish().waxRequired();
        helper.assertTrue(required > 1, "The hwacha takes no wax");
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, required - 1));
        hwacha.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!hwacha.finish().waxed() && hwacha.finish().waxApplied() == required - 1
                        && player.getMainHandItem().isEmpty(),
                "Part of the wax was not taken: " + hwacha.finish().waxApplied() + " of " + required);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 5));
        hwacha.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(hwacha.finish().waxed() && player.getMainHandItem().getCount() == 4,
                "Finishing the wax took the wrong number of honeycombs");
        helper.assertTrue(hwacha.finishTextureSuffix().equals("_waxed"), "A waxed engine shows the wrong texture");

        hwacha.finish().setExposure(SiegeworksServerConfig.getWeatheringDays() * 24000.0D);
        hwacha.finish().tick(helper.getLevel());
        helper.assertTrue(!hwacha.finish().weathered(), "A waxed engine weathered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weatheredWoodTakesNoWaxAndOthersTakeNone(GameTestHelper helper) {
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        AbstractSiegeEntity culverin = engine(helper, SiegeworksEntities.CULVERIN_ENTITY.get(), new BlockPos(2, 1, 2));
        culverin.finish().weather();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 8));
        culverin.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!culverin.finish().waxed() && player.getMainHandItem().getCount() == 8,
                "A weathered engine took wax");

        AbstractSiegeEntity onager = engine(helper, SiegeworksEntities.ONAGER_ENTITY.get(), new BlockPos(6, 1, 2));
        onager.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!onager.finish().waxed() && player.getMainHandItem().getCount() == 8,
                "An engine without wax in its profile took honeycomb");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyAFullEnoughRepairMakesAWeatheredEngineFresh(GameTestHelper helper) {
        AbstractSiegeEntity engine = engine(helper, SiegeworksEntities.ARCBALLISTA_ENTITY.get(), new BlockPos(2, 1, 2));
        engine.finish().weather();
        float share = (float) (engine.getMaxHealth() * SiegeworksServerConfig.getWeatheringHealthLoss());
        engine.finish().onRepaired(share * 0.5F);
        helper.assertTrue(engine.finish().weathered(), "A small repair made a weathered engine fresh");
        engine.finish().onRepaired(share);
        helper.assertTrue(!engine.finish().weathered() && engine.finishTextureSuffix().isEmpty(),
                "A repair of the weathered share did not make the engine fresh");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void copperGivesTheCulverinABronzeBarrel(GameTestHelper helper) {
        AbstractSiegeEntity culverin = engine(helper, SiegeworksEntities.CULVERIN_ENTITY.get(), new BlockPos(2, 1, 2));
        int required = culverin.finish().copperRequired();
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_INGOT, required));
        culverin.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(culverin.finish().bronze() && player.getMainHandItem().isEmpty(),
                "Copper did not make the barrel bronze");
        culverin.finish().weather();
        helper.assertTrue(culverin.finishTextureSuffix().equals("_bronze_weathered"),
                "A weathered bronze culverin shows " + culverin.finishTextureSuffix());

        AbstractSiegeEntity meg = engine(helper, SiegeworksEntities.MONS_MEG_ENTITY.get(), new BlockPos(6, 1, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_INGOT, 16));
        meg.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!meg.finish().bronze() && player.getMainHandItem().getCount() == 16,
                "Mons Meg, wrought iron, took copper");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theFinishSurvivesSaveAndLoad(GameTestHelper helper) {
        AbstractSiegeEntity source = engine(helper, SiegeworksEntities.SERPENTINE_ENTITY.get(), new BlockPos(2, 1, 2));
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB, 2));
        source.interact(player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(Items.COPPER_INGOT, source.finish().copperRequired()));
        source.interact(player, InteractionHand.MAIN_HAND);
        source.finish().setExposure(1234.0D);
        CompoundTag saved = new CompoundTag();
        source.addAdditionalSaveData(saved);

        AbstractSiegeEntity restored = engine(helper, SiegeworksEntities.SERPENTINE_ENTITY.get(), new BlockPos(6, 1, 2));
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.finish().bronze() && !restored.finish().waxed()
                        && restored.finish().waxApplied() == 2 && restored.finish().exposure() == 1234.0D,
                "The finish was lost while loading");
        helper.succeed();
    }

    private static AbstractSiegeEntity engine(GameTestHelper helper, EntityType<? extends AbstractSiegeEntity> type,
                                              BlockPos at) {
        AbstractSiegeEntity engine = type.create(helper.getLevel());
        helper.assertTrue(engine != null, "Could not create " + type);
        Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(at));
        engine.setPos(position.x, position.y, position.z);
        return engine;
    }

    private EngineFinishGameTests() {
    }
}
