package me.mss1r.siegeworks.gametest;

import com.google.gson.JsonParser;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.block.IncendiaryPotBlockEntity;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.data.profile.ProfileFormat;
import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.gameplay.ballistics.IncendiaryFuse;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IncendiaryFuseGameTests {
    private static final double EPSILON = 1.0E-6D;
    private static final String FIRE_BATCH = "incendiary_fuse";
    private static SiegeBlockDamage ruleBefore = SiegeBlockDamage.EVERYWHERE;

    private IncendiaryFuseGameTests() {
    }

    /**
     * Fire tests change terrain, so they run in their own batch with the terrain rule set to EVERYWHERE, whatever the
     * test world was saved with.
     */
    @BeforeBatch(batch = FIRE_BATCH)
    public static void letFireTakeEverywhere(ServerLevel level) {
        ruleBefore = SiegeworksServerConfig.getBlockDamage();
        SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.EVERYWHERE);
    }

    @AfterBatch(batch = FIRE_BATCH)
    public static void restoreTheTerrainRule(ServerLevel level) {
        SiegeworksServerConfig.setBlockDamage(ruleBefore);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void potIsFilledBaseFirstThenSixAdditivesThenSealedWithAWick(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = placePot(helper, PotFilling.EMPTY);
        Player filler = SiegeGameTestPlayers.create(level);

        helper.assertTrue(!takes(level, pos, filler, Items.GUNPOWDER), "An additive went in before the base");
        helper.assertTrue(!takes(level, pos, filler, Items.STRING), "A wick went into a pot without a base");
        helper.assertTrue(takes(level, pos, filler, Items.CHARCOAL), "The pot did not take charcoal for its base");
        helper.assertTrue(!takes(level, pos, filler, Items.CHARCOAL), "The base took a second charcoal");
        helper.assertTrue(takes(level, pos, filler, Items.HONEYCOMB), "The pot did not take a honeycomb for its base");
        for (int i = 0; i < 3; i++) {
            helper.assertTrue(takes(level, pos, filler, Items.GUNPOWDER), "The pot refused gunpowder " + (i + 1));
        }
        helper.assertTrue(!takes(level, pos, filler, Items.GUNPOWDER), "The pot took a fourth of one additive");
        for (Item additive : List.of(Items.BLAZE_POWDER, Items.CHARCOAL, Items.HONEYCOMB)) {
            helper.assertTrue(takes(level, pos, filler, additive), "The pot refused an additive: " + additive);
        }
        helper.assertTrue(!takes(level, pos, filler, Items.CHARCOAL), "A full pot took a seventh additive");
        helper.assertTrue(!level.getBlockState(pos).getValue(IncendiaryPotBlock.WICK), "The pot showed a wick early");
        helper.assertTrue(takes(level, pos, filler, Items.STRING), "The full pot did not take its wick");
        helper.assertTrue(level.getBlockState(pos).getValue(IncendiaryPotBlock.WICK), "The sealed pot shows no wick");
        helper.assertTrue(!takes(level, pos, filler, Items.HONEYCOMB), "A sealed pot took another ingredient");

        PotFilling filling = pot(level, pos).filling();
        helper.assertTrue(filling.contents().size() == 8 && filling.wick() && filling.canLight(),
                "The pot does not hold its base, its six additives and its wick: " + filling);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void burstFollowsTheBaseAndEachAdditive(GameTestHelper helper) {
        assertBurst(helper, fill(), 5.0D, 0.7D, 8, 0.0D, "the base alone");
        assertBurst(helper, PotFilling.standard(), 8.0D, 0.7D, 12, 0.0D, "the standard filling");
        assertBurst(helper, fill(Items.CHARCOAL, Items.CHARCOAL, Items.CHARCOAL),
                9.5D, 0.7D, 8, 0.0D, "three charcoal");
        assertBurst(helper, fill(Items.HONEYCOMB, Items.HONEYCOMB, Items.HONEYCOMB),
                5.0D, 0.7D, 14, 0.0D, "three honeycombs");
        assertBurst(helper, fill(Items.BLAZE_POWDER, Items.BLAZE_POWDER, Items.BLAZE_POWDER),
                5.0D, 0.925D, 8, 0.0D, "three blaze powder");
        assertBurst(helper, fill(Items.GUNPOWDER, Items.GUNPOWDER, Items.GUNPOWDER),
                5.0D, 0.7D, 8, 60_000.0D, "three gunpowder");
        assertBurst(helper, fill(Items.CHARCOAL, Items.CHARCOAL, Items.CHARCOAL,
                        Items.HONEYCOMB, Items.HONEYCOMB, Items.HONEYCOMB),
                9.5D, 0.7D, 14, 0.0D, "three charcoal and three honeycombs");
        helper.assertTrue(PotFilling.EMPTY.burst().equals(PotFilling.Burst.NONE), "An empty pot would burst");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void potItemsKeepWhatTheyHold(GameTestHelper helper) {
        helper.assertTrue(PotFilling.of(new ItemStack(SiegeworksItems.FIRE_PROJECTILE.get())).equals(
                PotFilling.standard()), "An incendiary pot from before filling lost its standard filling");
        helper.assertTrue(PotFilling.of(new ItemStack(SiegeworksItems.CLAY_POT.get())).equals(PotFilling.EMPTY),
                "A crafted clay pot is not empty");
        PotFilling partial = fill(Items.GUNPOWDER);
        ItemStack partialItem = partial.toItem();
        helper.assertTrue(partialItem.is(SiegeworksItems.CLAY_POT.get()) && PotFilling.of(partialItem).equals(partial),
                "A pot without its wick did not keep its filling as a clay pot");
        PotFilling sealed = partial.withWick().orElseThrow();
        ItemStack sealedItem = sealed.toItem();
        helper.assertTrue(sealedItem.is(SiegeworksItems.FIRE_PROJECTILE.get())
                        && PotFilling.of(sealedItem).equals(sealed),
                "A sealed pot did not keep its filling as an incendiary pot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void potBreaksWholeByHandAndIntoBricksByATool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PotFilling filling = fill(Items.GUNPOWDER);
        BlockPos pos = placePot(helper, filling);
        BlockState state = level.getBlockState(pos);

        List<ItemStack> byHand = Block.getDrops(state, level, pos, pot(level, pos), null, ItemStack.EMPTY);
        helper.assertTrue(byHand.size() == 1 && PotFilling.of(byHand.get(0)).equals(filling),
                "By hand the pot did not drop whole with what it holds: " + byHand);
        List<ItemStack> byTool = Block.getDrops(state, level, pos, pot(level, pos), null,
                new ItemStack(Items.IRON_PICKAXE));
        int bricks = byTool.stream().filter(stack -> stack.is(Items.BRICK)).mapToInt(ItemStack::getCount).sum();
        int gunpowder = byTool.stream().filter(stack -> stack.is(Items.GUNPOWDER)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(bricks == 5 && gunpowder == 1,
                "A tool did not break the pot into its bricks and what it held: " + byTool);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 220, batch = FIRE_BATCH)
    public static void placedPotBurstsOnceItsLitFuseBurnsDown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pot = placePot(helper, PotFilling.standard());
        Ravager neighbour = neighbour(helper);
        Player lighter = SiegeGameTestPlayers.create(level);

        helper.assertTrue(use(level, pot, lighter, new ItemStack(Items.FLINT_AND_STEEL)),
                "Flint and steel did not light the placed pot");
        helper.assertTrue(level.getBlockState(pot).getValue(IncendiaryPotBlock.LIT), "The lit pot does not burn");
        helper.assertTrue(lighter.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 1,
                "Lighting the pot did not wear the flint and steel");

        int fuse = IncendiaryFuse.fullLength();
        helper.runAfterDelay(fuse / 2, () -> helper.assertTrue(
                level.getBlockState(pot).is(SiegeworksBlocks.FIRE_PROJECTILE.get()) && !neighbour.isOnFire(),
                "The pot burst before its fuse burnt down"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!level.getBlockState(pot).is(SiegeworksBlocks.FIRE_PROJECTILE.get()),
                    "The lit pot is still standing");
            helper.assertTrue(neighbour.isOnFire(), "The bursting pot did not set its neighbour alight");
            helper.assertTrue(fireAround(helper) > 0, "The bursting pot did not set the ground around it alight" + conditions(level));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void potWithoutAWickCannotBeLit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pot = placePot(helper, fill(Items.CHARCOAL));
        Player lighter = SiegeGameTestPlayers.create(level);
        use(level, pot, lighter, new ItemStack(Items.FLINT_AND_STEEL));
        helper.assertTrue(!level.getBlockState(pot).getValue(IncendiaryPotBlock.LIT), "A pot without a wick was lit");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = FIRE_BATCH)
    public static void explosionSetsOffTheFilledPotsItReaches(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos first = placePot(helper, PotFilling.standard(), new BlockPos(2, 1, 2));
        BlockPos second = placePot(helper, fill(), new BlockPos(5, 1, 2));
        Vec3 blast = Vec3.atCenterOf(first).add(0.0D, 0.0D, 1.0D);
        level.explode(null, blast.x, blast.y, blast.z, 1.5F, Level.ExplosionInteraction.TNT);

        helper.succeedWhen(() -> {
            helper.assertTrue(!isPot(level, first) && !isPot(level, second),
                    "The explosion did not set off the pot it hit and the one that pot's burst reached");
            helper.assertTrue(fireAround(helper) > 0, "The pots went off without fire" + conditions(level));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void fireTakesToAFilledPotAndSetsItOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos filled = placePot(helper, fill(), new BlockPos(2, 1, 2));
        BlockPos empty = placePot(helper, PotFilling.EMPTY, new BlockPos(2, 1, 5));
        helper.assertTrue(level.getBlockState(filled).getFlammability(level, filled, Direction.UP) > 0,
                "Fire does not take to a filled pot");
        helper.assertTrue(level.getBlockState(empty).getFlammability(level, empty, Direction.UP) == 0,
                "Fire takes to an empty clay pot");

        level.getBlockState(filled).onCaughtFire(level, filled, Direction.UP, null);
        helper.assertTrue(!isPot(level, filled), "A filled pot caught by fire did not go off");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void unlitPotShattersWithoutFire(GameTestHelper helper) {
        Ravager neighbour = neighbour(helper);
        TrebuchetProjectile pot = dropPot(helper, false);

        helper.succeedWhen(() -> {
            helper.assertTrue(pot.isRemoved(), "The unlit pot has not landed yet");
            helper.assertTrue(!neighbour.isOnFire() && fireAround(helper) == 0,
                    "An unlit pot burst into fire on landing");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void litPotBurstsWhereItLands(GameTestHelper helper) {
        Ravager neighbour = neighbour(helper);
        TrebuchetProjectile pot = dropPot(helper, true);

        helper.succeedWhen(() -> {
            helper.assertTrue(pot.isRemoved(), "The lit pot has not landed yet");
            helper.assertTrue(neighbour.isOnFire(), "A lit pot did not burst into fire on landing");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void litPotBurstsInFlightWhenItsFuseRunsOut(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrebuchetProjectile pot = SiegeworksEntities.MANGONEL_PROJECTILE.get().create(level);
        helper.assertTrue(pot != null, "Failed to create the pot");
        pot.setPhysicsProfile(ProjectileVariants.MANGONEL_FIRE_PROJECTILE);
        pot.setFilling(PotFilling.standard());
        pot.setNoGravity(true);
        Vec3 at = helper.absoluteVec(new Vec3(2.5D, 6.0D, 2.5D));
        pot.setPos(at.x, at.y, at.z);
        pot.lightFuse(5);
        helper.assertTrue(level.addFreshEntity(pot), "Failed to add the pot");

        helper.runAfterDelay(3, () -> helper.assertTrue(!pot.isRemoved(), "The pot burst before its fuse ran out"));
        helper.succeedWhen(() -> helper.assertTrue(pot.isRemoved(), "The pot outlived its fuse in the air"));
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void potLitInAMangonelBurstsThereIfNotThrown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        helper.assertTrue(mangonel != null, "Failed to create the mangonel");
        assertLitPotBurstsInEngine(helper, mangonel, new BlockPos(3, 1, 3),
                crew -> mangonel.handleSiegeInteraction(crew, InteractionHand.MAIN_HAND, level));
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void potLitInATrebuchetBurstsThereIfNotThrown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrebuchetEntity trebuchet = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
        helper.assertTrue(trebuchet != null, "Failed to create the trebuchet");
        assertLitPotBurstsInEngine(helper, trebuchet, new BlockPos(8, 1, 8),
                crew -> trebuchet.handleSiegeInteraction(crew, InteractionHand.MAIN_HAND, level));
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void mangonelThrowsItsPotStillBurning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        helper.assertTrue(mangonel != null, "Failed to create the mangonel");
        Player crew = placeLoadedEngine(helper, mangonel, new BlockPos(3, 1, 3));

        helper.assertTrue(mangonel.handleSiegeInteraction(crew, InteractionHand.MAIN_HAND, level).consumesAction(),
                "The mangonel would not light its pot");
        crew.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(mangonel.handleSiegeInteraction(crew, InteractionHand.MAIN_HAND, level).consumesAction(),
                "The mangonel would not throw its lit pot");

        helper.succeedWhen(() -> {
            AABB around = mangonel.getBoundingBox().inflate(48.0D);
            helper.assertTrue(!level.getEntitiesOfClass(TrebuchetProjectile.class, around,
                    shot -> shot.isLit() && PotFilling.standard().equals(shot.filling())).isEmpty(),
                    "The thrown pot left its fuse or its filling behind");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void trebuchetHoldsItsLoadedPotInTheSlingNearTheGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TrebuchetEntity trebuchet = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
        helper.assertTrue(trebuchet != null, "Failed to create the trebuchet");
        Vec3 origin = helper.absoluteVec(new Vec3(8.5D, 1.0D, 8.5D));
        trebuchet.setPos(origin);
        trebuchet.setAmmoLoaded(SiegeAmmo.AMMO_FIRE);
        trebuchet.setWindingTime(0);

        Vec3 pot = trebuchet.potPoint(0.0D).subtract(origin);
        double height = pot.y;
        double reach = pot.horizontalDistance();
        helper.assertTrue(height > 0.0D && height < 2.0D && reach > 4.0D,
                "The loaded pot is not low in the sling behind the frame: " + pot);
        // The pot lies along the sling, which rests flat on the ground, with its fuse toward the arm.
        Vec3 fuse = trebuchet.potPoint(1.0D).subtract(trebuchet.potPoint(0.0D));
        helper.assertTrue(Math.abs(fuse.y) < 0.2D && fuse.horizontalDistance() > 0.98D,
                "The loaded pot does not lie along the sling: " + fuse);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void siegeEnginesNeitherFreezeNorDrownNorTakePotions(GameTestHelper helper) {
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(mangonel != null, "Failed to create the mangonel");
        helper.assertTrue(!mangonel.canFreeze(), "A siege engine freezes in powder snow");
        helper.assertTrue(mangonel.canBreatheUnderwater(), "A siege engine drowns");
        helper.assertTrue(!mangonel.addEffect(new MobEffectInstance(MobEffects.POISON, 100)),
                "A siege engine was poisoned");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void bundledPotFillingMatchesTheBuiltInValues(GameTestHelper helper) {
        helper.assertTrue(SiegeProfileCatalogs.POT_FILLINGS.contains(PotFillingProfile.ID),
                "The bundled pot filling profile did not load");
        PotFillingProfile loaded = PotFillingProfile.current();
        helper.assertTrue(loaded.equals(PotFillingProfile.DEFAULT),
                "The bundled pot filling differs from the built-in one: " + loaded);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void datapackPotFillingDecidesIngredientsLimitsAndWick(GameTestHelper helper) {
        PotFilling saved = fill(Items.GUNPOWDER, Items.BLAZE_POWDER);
        Map<ResourceLocation, PotFillingProfile> before = SiegeProfileCatalogs.POT_FILLINGS.snapshot();
        SiegeProfileCatalogs.POT_FILLINGS.acceptFromServer(Map.of(PotFillingProfile.ID, new PotFillingProfile(
                Map.of("#minecraft:coals", 1),
                new PotFillingProfile.Effect(4.0D, 0.5D, 6, 0.0D),
                Map.of("minecraft:glowstone_dust", new PotFillingProfile.Effect(3.0D, 0.0D, 0, 0.0D),
                        "minecraft:blaze_powder", new PotFillingProfile.Effect(0.0D, 0.1D, 0, 0.0D)),
                2, 1, "minecraft:vine")));
        try {
            PotFilling filling = PotFilling.EMPTY.with(Items.COAL).orElse(null);
            helper.assertTrue(filling != null && filling.hasBase(), "Coal did not make a base through #minecraft:coals");
            filling = filling.with(Items.GLOWSTONE_DUST).orElseThrow();
            helper.assertTrue(filling.with(Items.GLOWSTONE_DUST).isEmpty(), "maxOfAKind 1 let a second glowstone in");
            filling = filling.with(Items.BLAZE_POWDER).orElseThrow();
            helper.assertTrue(filling.with(Items.GUNPOWDER).isEmpty(), "Gunpowder went in though it is no additive");
            assertBurst(helper, filling, 7.0D, 0.6D, 6, 0.0D, "a datapack filling");
            helper.assertTrue(PotFilling.isWick(new ItemStack(Items.VINE))
                    && !PotFilling.isWick(new ItemStack(Items.STRING)), "The datapack wick is not the one accepted");
            PotFilling reloaded = PotFilling.load(saved.save());
            PotFilling expected = PotFilling.EMPTY.with(Items.CHARCOAL).orElseThrow()
                    .with(Items.BLAZE_POWDER).orElseThrow();
            helper.assertTrue(reloaded.equals(expected),
                    "A saved pot kept what the datapack no longer accepts: " + reloaded);
        } finally {
            SiegeProfileCatalogs.POT_FILLINGS.acceptFromServer(before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void brokenPotFillingNamesTheFieldAtFault(GameTestHelper helper) {
        String good = "{\"formatVersion\": 2, \"base\": {\"minecraft:charcoal\": 1}, \"baseBurst\": {\"fireRadius\": 5}}";
        helper.assertTrue(ProfileFormat.potFilling(JsonParser.parseString(good)).isEmpty(),
                "A valid pot filling was rejected");
        assertFormatError(helper, good.replace("\"baseBurst\"", "\"basBurst\""), "unknown field basBurst");
        assertFormatError(helper, good.replace("\"fireRadius\"", "\"fireRadious\""),
                "unknown field baseBurst.fireRadious");
        assertFormatError(helper, good.replace(": 1}", ": 1.5}"), "base.minecraft:charcoal must be a whole number");
        Optional<String> error = new PotFillingProfile(Map.of("minecraft:charcol", 1), PotFillingProfile.Effect.NONE,
                Map.of(), 6, 3, "minecraft:string").validationError();
        helper.assertTrue(error.isPresent() && error.get().contains("base.minecraft:charcol: there is no item"),
                "A misspelt item was not reported: " + error);
        helper.succeed();
    }

    private static void assertFormatError(GameTestHelper helper, String json, String expected) {
        Optional<String> error = ProfileFormat.potFilling(JsonParser.parseString(json));
        helper.assertTrue(error.isPresent() && error.get().contains(expected),
                "Expected '" + expected + "' but got " + error);
    }

    private static void assertBurst(GameTestHelper helper, PotFilling filling, double radius, double chance,
                                    int burnSeconds, double blastEnergy, String what) {
        PotFilling.Burst burst = filling.burst();
        helper.assertTrue(Math.abs(burst.fireRadius() - radius) < EPSILON
                        && Math.abs(burst.fireChance() - chance) < EPSILON
                        && burst.burnSeconds() == burnSeconds
                        && Math.abs(burst.blastEnergy() - blastEnergy) < EPSILON,
                "A pot with " + what + " bursts as " + burst);
    }

    /** Unsealed pot with the base followed by these additives. */
    private static PotFilling fill(Item... additives) {
        PotFilling filling = PotFilling.EMPTY.with(Items.CHARCOAL).orElseThrow().with(Items.HONEYCOMB).orElseThrow();
        for (Item additive : additives) {
            filling = filling.with(additive).orElseThrow();
        }
        return filling;
    }

    private static BlockPos placePot(GameTestHelper helper, PotFilling filling) {
        return placePot(helper, filling, new BlockPos(2, 1, 2));
    }

    private static BlockPos placePot(GameTestHelper helper, PotFilling filling, BlockPos relative) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        level.setBlockAndUpdate(pos, SiegeworksBlocks.FIRE_PROJECTILE.get().defaultBlockState()
                .setValue(IncendiaryPotBlock.WICK, filling.wick()));
        pot(level, pos).setFilling(filling);
        return pos;
    }

    private static IncendiaryPotBlockEntity pot(ServerLevel level, BlockPos pos) {
        return (IncendiaryPotBlockEntity) level.getBlockEntity(pos);
    }

    private static boolean isPot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(SiegeworksBlocks.FIRE_PROJECTILE.get());
    }

    /** True if the pot took one {@code item} from a player holding several. */
    private static boolean takes(ServerLevel level, BlockPos pos, Player filler, Item item) {
        int before = pot(level, pos).filling().contents().size();
        boolean wick = pot(level, pos).filling().wick();
        use(level, pos, filler, new ItemStack(item, 4));
        PotFilling after = pot(level, pos).filling();
        boolean took = after.contents().size() > before || after.wick() != wick;
        int left = filler.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        return took && left == 3;
    }

    private static boolean use(ServerLevel level, BlockPos pos, Player player, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        BlockState state = level.getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        //? if forge {
        /*return state.use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction();
        *///?} else {
        return state.useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit).consumesAction();
        //?}
    }

    private static void assertLitPotBurstsInEngine(GameTestHelper helper, AbstractSiegeEntity engine, BlockPos at,
                                                   Function<Player, InteractionResult> interact) {
        Player crew = placeLoadedEngine(helper, engine, at);

        int fuse = IncendiaryFuse.fullLength();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(SiegeAmmo.isFireAmmoKey(engine.getAmmoLoaded()),
                    "An unlit pot did not wait in the engine");
            helper.assertTrue(interact.apply(crew).consumesAction(),
                    "Flint and steel did not light the pot in the engine");
            helper.runAfterDelay(fuse / 2, () -> helper.assertTrue(SiegeAmmo.isFireAmmoKey(engine.getAmmoLoaded()),
                    "The pot burst before its fuse burnt down"));
            helper.succeedWhen(() -> helper.assertTrue(!engine.hasAmmoLoaded(),
                    "The lit pot did not burst in the engine when it was not thrown"));
        });
    }

    /** Engine loaded as before hand filling existed: a sealed pot with the standard filling. */
    private static Player placeLoadedEngine(GameTestHelper helper, AbstractSiegeEntity engine, BlockPos at) {
        ServerLevel level = helper.getLevel();
        Vec3 position = helper.absoluteVec(Vec3.atBottomCenterOf(at));
        engine.setPos(position);
        engine.setAmmoLoaded(SiegeAmmo.AMMO_FIRE);
        engine.setWindingTime(0);
        helper.assertTrue(level.addFreshEntity(engine), "Failed to add the engine");
        Player crew = SiegeGameTestPlayers.create(level);
        crew.setPos(position);
        crew.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
        return crew;
    }

    private static TrebuchetProjectile dropPot(GameTestHelper helper, boolean lit) {
        ServerLevel level = helper.getLevel();
        TrebuchetProjectile pot = SiegeworksEntities.MANGONEL_PROJECTILE.get().create(level);
        helper.assertTrue(pot != null, "Failed to create the pot");
        pot.setPhysicsProfile(ProjectileVariants.MANGONEL_FIRE_PROJECTILE);
        pot.setFilling(PotFilling.standard());
        Vec3 at = helper.absoluteVec(new Vec3(2.5D, 3.0D, 2.5D));
        pot.setPos(at.x, at.y, at.z);
        pot.setDeltaMovement(0.0D, -0.6D, 0.0D);
        if (lit) {
            pot.lightFuse(IncendiaryFuse.fullLength());
        }
        helper.assertTrue(level.addFreshEntity(pot), "Failed to add the pot");
        return pot;
    }

    /** Fire target next to the pot. Not an iron golem: Recruits replaces spawned golems. */
    private static Ravager neighbour(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.RAVAGER, new BlockPos(3, 1, 2));
    }

    /** Fire-related conditions, included in failure messages. */
    private static String conditions(ServerLevel level) {
        return " (terrain rule " + SiegeworksServerConfig.getBlockDamage() + ", raining " + level.isRaining() + ")";
    }

    private static int fireAround(GameTestHelper helper) {
        int fires = 0;
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)),
                helper.absolutePos(new BlockPos(6, 4, 6)))) {
            if (helper.getLevel().getBlockState(pos).is(Blocks.FIRE)) {
                fires++;
            }
        }
        return fires;
    }
}
