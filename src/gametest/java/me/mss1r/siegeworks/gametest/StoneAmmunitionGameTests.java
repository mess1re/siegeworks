package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.block.StackedProjectileBlock;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class StoneAmmunitionGameTests {
    @GameTest(template = "empty")
    public static void stonecuttingRecipesYieldAgreedAmounts(GameTestHelper h) {
        for (String id : new String[]{"stone_cannon_ball", "giant_cannon_ball"}) {
            var entry = h.getLevel().getRecipeManager().byKey(MinecraftVersionCompat.id("siegeworks", id)).orElseThrow();
            //? if <1.21 {
            /*var recipe = entry;
            *///?} else {
            var recipe = entry.value();
            //?}
            var result = recipe.getResultItem(h.getLevel().registryAccess());
            h.assertTrue(recipe.getType() == net.minecraft.world.item.crafting.RecipeType.STONECUTTING,
                    "Stone ammunition must use the stonecutter");
            h.assertTrue(recipe.getIngredients().get(0).test(new ItemStack(Items.STONE)), "Wrong ingredient");
            boolean giant = id.equals("giant_cannon_ball");
            h.assertTrue(result.is(giant ? SiegeworksItems.GIANT_CANNON_BALL.get() : SiegeworksItems.STONE_CANNON_BALL.get())
                    && result.getCount() == (giant ? 1 : 4), "Wrong stonecutting yield");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyPileDropsItsFullCount(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        for (var block : new Block[]{SiegeworksBlocks.STONE_CANNON_BALL.get(), SiegeworksBlocks.GIANT_CANNON_BALL.get()}) {
            for (int count = 1; count <= 4; count++) {
                var state = block.defaultBlockState().setValue(StackedProjectileBlock.COUNT, count);
                int actual = Block.getDrops(state, h.getLevel(), pos, null).stream()
                        .filter(s -> s.is(block.asItem())).mapToInt(ItemStack::getCount).sum();
                h.assertTrue(actual == count, "Pile lost ammunition: " + count + " -> " + actual);
            }
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void automatedStoneLoadingPersistsAndFiresStone(GameTestHelper h) {
        for (var type : java.util.List.of(SiegeworksEntities.CULVERIN_ENTITY.get(), SiegeworksEntities.SERPENTINE_ENTITY.get())) {
            AbstractFieldGunEntity gun = (AbstractFieldGunEntity) type.create(h.getLevel());
            gun.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 4, 4))));
            h.getLevel().addFreshEntity(gun);
            var operator = SiegeGameTestPlayers.createRideable(h.getLevel());
            operator.setPos(gun.position());
            h.assertTrue(operator.startRiding(gun), "Operator could not board");
            gun.setOperator(operator);
            gun.setLoadStage(2);
            var inventory = new SimpleContainer(new ItemStack(SiegeworksItems.STONE_CANNON_BALL.get(), 2),
                    new ItemStack(SiegeworksItems.RAMROD.get()), new ItemStack(Items.FLINT_AND_STEEL));
            for (int i = 0; i < 1000 && gun.getLoadStage() == 2; i++) gun.advancePrimaryAction(operator, inventory);
            h.assertTrue(gun.getLoadStage() == 3 && inventory.getItem(0).getCount() == 1,
                    "Stone shot was not loaded exactly once");
            h.assertTrue(gun.getAmmoLoaded().equals("siegeworks:stone_cannon_ball"), "Stone became iron");
            CompoundTag saved = new CompoundTag();
            gun.addAdditionalSaveData(saved);
            gun.readAdditionalSaveData(saved);
            h.assertTrue(gun.getAmmoLoaded().equals("siegeworks:stone_cannon_ball"), "Saved material was lost");
            for (int i = 0; i < 1000 && gun.getLoadStage() != 0; i++) gun.advancePrimaryAction(operator, inventory);
            var shots = h.getLevel().getEntitiesOfClass(CannonProjectile.class, gun.getBoundingBox().inflate(15),
                    shot -> shot.getOwner() == gun);
            h.assertTrue(shots.size() == 1 && shots.get(0).getType() == SiegeworksEntities.STONE_CANNON_BALL.get(),
                    "Wrong projectile type or duplicate shot");
            var physics = shots.get(0).getPhysicsProfile();
            h.assertTrue(Math.abs(physics.mass() - 2.352425) < 0.00001 && physics.material().density() == 2600,
                    "Stone shot used iron physics or fallback defaults");
            shots.forEach(net.minecraft.world.entity.Entity::discard);
            operator.stopRiding();gun.discard();
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void manualLoadingConsumesStoneAndCancelsOnItemChange(GameTestHelper h) {
        var gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(h.getLevel());
        gun.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 4, 4))));
        gun.setNoGravity(true);
        h.getLevel().addFreshEntity(gun);
        var player = SiegeGameTestPlayers.createRideable(h.getLevel());
        player.setPos(gun.position());
        player.startRiding(gun);gun.setOperator(player);gun.setLoadStage(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.STONE_CANNON_BALL.get(), 2));
        gun.handleSiegeInteraction(player, InteractionHand.MAIN_HAND, h.getLevel());
        for (int i = 0; i < 1000 && gun.getLoadStage() == 2; i++) gun.tick();
        h.assertTrue(gun.getLoadStage() == 3 && player.getMainHandItem().getCount() == 1
                && gun.getAmmoLoaded().equals("siegeworks:stone_cannon_ball"), "Manual stone loading failed");
        gun.setLoadStage(2);gun.setAmmoLoaded("");
        gun.handleSiegeInteraction(player, InteractionHand.MAIN_HAND, h.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.GIANT_CANNON_BALL.get()));
        for (int i = 0; i < 100; i++) gun.tick();
        h.assertTrue(gun.getLoadStage() == 2 && gun.getAmmoLoaded().isEmpty()
                && player.getMainHandItem().getCount() == 1, "Swapping ammunition bypassed validation");
        player.stopRiding();gun.discard();h.succeed();
    }

    @GameTest(template = "empty")
    public static void giantShotCannotBeLoadedIntoFieldGun(GameTestHelper h) {
        var gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(h.getLevel());
        gun.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4, 4, 4))));
        h.getLevel().addFreshEntity(gun);
        var player = SiegeGameTestPlayers.createRideable(h.getLevel());
        player.setPos(gun.position());
        player.startRiding(gun);gun.setOperator(player);gun.setLoadStage(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.GIANT_CANNON_BALL.get()));
        gun.handleSiegeInteraction(player, InteractionHand.MAIN_HAND, h.getLevel());
        h.assertTrue(gun.getLoadStage() == 2
                && player.getMainHandItem().getCount() == 1, "Wrong caliber was consumed or began loading");
        player.stopRiding();gun.discard();h.succeed();
    }
}
