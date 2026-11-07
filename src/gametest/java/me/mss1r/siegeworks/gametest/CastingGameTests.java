package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.block.CaliberShotBlock;
import me.mss1r.siegeworks.block.CastingTableBlock;
import me.mss1r.siegeworks.block.StackedProjectileBlock;
import me.mss1r.siegeworks.gameplay.ammunition.RoundShotSize;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.item.crafting.CastingRecipe;
import me.mss1r.siegeworks.menu.CastingMenu;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public final class CastingGameTests {
    @GameTest(template="empty")
    public static void castingTableShapeFollowsModel(GameTestHelper h) {
        var table = SiegeworksBlocks.CASTING_TABLE.get().defaultBlockState();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            var state = table.setValue(CastingTableBlock.FACING, facing);
            var shape = state.getCollisionShape(h.getLevel(), BlockPos.ZERO);
            var outline = state.getShape(h.getLevel(), BlockPos.ZERO);
            h.assertTrue(!Shapes.joinIsNotEmpty(shape, outline, BooleanOp.NOT_SAME),
                    "Collision and outline disagree for " + facing);
            var gap = shape.clip(new Vec3(-1, 7 / 16.0, 0.5), new Vec3(2, 7 / 16.0, 0.5), BlockPos.ZERO);
            h.assertTrue(gap == null, "Space between the tabletop and shelf is obstructed for " + facing);
            var cavity = shape.clip(new Vec3(0.5, 2, 0.5), new Vec3(0.5, 0, 0.5), BlockPos.ZERO);
            h.assertTrue(cavity != null && Math.abs(cavity.getLocation().y - 14 / 16.0) < 1e-6,
                    "Mould cavity is filled or has no floor for " + facing);
            var hammerCenter = switch (facing) {
                case NORTH -> new Vec3(2 / 16.0, 2, 2 / 16.0);
                case EAST -> new Vec3(14 / 16.0, 2, 2 / 16.0);
                case SOUTH -> new Vec3(14 / 16.0, 2, 14 / 16.0);
                case WEST -> new Vec3(2 / 16.0, 2, 14 / 16.0);
                default -> throw new IllegalArgumentException("Expected a horizontal facing");
            };
            var hammer = shape.clip(hammerCenter, hammerCenter.multiply(1, 0, 1), BlockPos.ZERO);
            h.assertTrue(hammer != null && Math.abs(hammer.getLocation().y - 15.25 / 16) < 1e-6,
                    "Hammer shape did not rotate with the model for " + facing);
        }
        h.succeed();
    }

    private static ItemStack shot(int caliber,int count) {
        return RoundShotItem.withCaliber(new ItemStack(SiegeworksItems.CANNON_BALL.get(),count),caliber);
    }

    @GameTest(template="empty")
    public static void recipesHaveMinimumIngotAndVolumeCost(GameTestHelper h) {
        var list=h.getLevel().getRecipeManager().getAllRecipesFor(SiegeworksRecipes.CASTING.get());
        h.assertTrue(!list.isEmpty(),"Casting recipe missing");
        //? if <1.21 {
        /*var recipe=list.get(0);
        *///?} else {
        var recipe=list.get(0).value();
        //?}
        h.assertTrue(recipe.ingredient.test(new ItemStack(Items.IRON_INGOT)) && !recipe.ingredient.test(new ItemStack(Items.IRON_NUGGET)),"Wrong iron ingredient");
        h.assertTrue(recipe.batch(20).equals(new CastingRecipe.Batch(1,16)),"20 mm batch");
        h.assertTrue(recipe.batch(60).equals(new CastingRecipe.Batch(1,8)),"60 mm batch");
        h.assertTrue(recipe.batch(120).equals(new CastingRecipe.Batch(1,1)),"120 mm batch");
        h.assertTrue(recipe.batch(490).equals(new CastingRecipe.Batch(69,1)),"490 mm batch");
        var tableEntry=h.getLevel().getRecipeManager().byKey(MinecraftVersionCompat.id("siegeworks","casting_table")).orElseThrow();
        //? if <1.21 {
        /*var tableRecipe=tableEntry;
        *///?} else {
        var tableRecipe=tableEntry.value();
        //?}
        var ingredients=tableRecipe.getIngredients();
        h.assertTrue(ingredients.size()==9 && ingredients.get(0).isEmpty() && ingredients.get(2).isEmpty()
                && ingredients.get(7).isEmpty(),"Casting table empty cells differ from agreed recipe");
        h.assertTrue(ingredients.get(1).test(new ItemStack(Items.IRON_BLOCK)),"Casting table needs one iron block");
        for(int slot:new int[]{3,4,5}) h.assertTrue(ingredients.get(slot).test(new ItemStack(Items.OAK_PLANKS))
                && ingredients.get(slot).test(new ItemStack(Items.SPRUCE_PLANKS)),"Table planks must use a tag");
        for(int slot:new int[]{6,8}) h.assertTrue(ingredients.get(slot).test(new ItemStack(Items.OAK_LOG))
                && ingredients.get(slot).test(new ItemStack(Items.SPRUCE_LOG)),"Table legs must use logs tag");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(MinecraftVersionCompat.id("siegeworks","cannon_ball")).isEmpty(),"Old crafting recipe still bypasses casting");
        for(int c=5;c<=490;c+=5) {
            var b=recipe.batch(c);
            h.assertTrue(b.cost()>=1 && b.count()>=1 && b.count()<=16,"Invalid batch at "+c);
            h.assertTrue((long)b.count()*c*c*c <= (long)b.cost()*120*120*120,"Creates metal at "+c);
            if(c>5) h.assertTrue(RoundShotSize.scale(c,1)>RoundShotSize.scale(c-5,1),"Display size plateau");
        }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void caliberSurvivesItemAndBlockRoundTrips(GameTestHelper h) {
        var block=SiegeworksBlocks.CANNON_BALL.get();var pos=h.absolutePos(new BlockPos(2,2,2));
        for(int c:new int[]{5,20,60,120,250,490}) {
            var stack=shot(c,1);
            var saved=MinecraftVersionCompat.writeItem(stack,h.getLevel().registryAccess());
            h.assertTrue(RoundShotItem.caliber(MinecraftVersionCompat.readItem(saved,h.getLevel().registryAccess()))==c,"Item lost caliber");
            for(int count=1;count<=4;count++) {
                var state=block.defaultBlockState().setValue(CaliberShotBlock.CALIBER,c/5).setValue(StackedProjectileBlock.COUNT,count);
                var restored=net.minecraft.nbt.NbtUtils.readBlockState(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),net.minecraft.nbt.NbtUtils.writeBlockState(state));
                h.assertTrue(restored==state,"Block state lost caliber");
                var drops=Block.getDrops(restored,h.getLevel(),pos,null);
                h.assertTrue(drops.stream().allMatch(s->RoundShotItem.caliber(s)==c) && drops.stream().mapToInt(ItemStack::getCount).sum()==count,"Drops lost count or caliber");
                h.assertTrue(RoundShotItem.caliber(block.getCloneItemStack(h.getLevel(),pos,state))==c,"Pick block lost caliber");
            }
        }
        h.assertTrue(MinecraftVersionCompat.isSameItemAndData(shot(120,1),new ItemStack(SiegeworksItems.CANNON_BALL.get())),"Default rounds do not stack with old saves");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void placementAndRetrievalKeepCaliber(GameTestHelper h) {
        var player=SiegeGameTestPlayers.createRideable(h.getLevel());
        var pos=h.absolutePos(new BlockPos(2,2,2));player.setPos(Vec3.atCenterOf(pos));
        player.setItemInHand(InteractionHand.MAIN_HAND,shot(250,2));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),hit);
        var block=SiegeworksBlocks.CANNON_BALL.get();
        var state=block.getStateForPlacement(context);
        h.assertTrue(state!=null && state.getValue(CaliberShotBlock.CALIBER)==50,"Placement lost caliber");
        h.getLevel().setBlock(pos,state,Block.UPDATE_ALL);
        player.setItemInHand(InteractionHand.MAIN_HAND,shot(20,1));
        context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,player.getMainHandItem(),hit);
        h.assertTrue(!state.canBeReplaced(context),"Mixed calibers stacked");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        //? if <1.21 {
        /*state.use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        *///?} else {
        state.useWithoutItem(h.getLevel(),player,hit);
        //?}
        h.assertTrue(player.getInventory().items.stream().anyMatch(s->RoundShotItem.caliber(s)==250),"Taking round lost caliber");
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(),"Taking last round left block");h.succeed();
    }

    @GameTest(template="empty")
    public static void castingCommitsWholeBatchAndRejectsMissingMaterials(GameTestHelper h) {
        var p=SiegeGameTestPlayers.createRideable(h.getLevel());var pos=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlock(pos,SiegeworksBlocks.CASTING_TABLE.get().defaultBlockState(),Block.UPDATE_ALL);
        p.setPos(Vec3.atCenterOf(pos));var menu=new CastingMenu(1,p.getInventory(),pos);
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT));
        for(int i=0;i<20;i++) menu.clickMenuButton(p,0);
        h.assertTrue(menu.caliber()==20 && menu.clickMenuButton(p,2),"Could not cast 20 mm batch");
        h.assertTrue(p.getInventory().items.stream().filter(s->RoundShotItem.caliber(s)==20).mapToInt(ItemStack::getCount).sum()==16,"Wrong bullet count");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s -> RoundShotItem.caliber(s) == 20 && s.getCount() == 16),
                "A full batch of bullets must fit in one slot");
        h.assertTrue(p.getInventory().items.stream().allMatch(s->s.getCount()<=s.getMaxStackSize()),"Created oversized stacks");
        h.assertTrue(!menu.clickMenuButton(p,2),"Cast without materials");
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,64));p.getInventory().setItem(1,new ItemStack(Items.IRON_INGOT,5));
        for(int i=0;i<94;i++) menu.clickMenuButton(p,1);
        h.assertTrue(menu.caliber()==490 && menu.clickMenuButton(p,2),"Cannot consume more than one input stack");
        h.assertTrue(p.getInventory().items.stream().filter(s->RoundShotItem.caliber(s)==490).mapToInt(ItemStack::getCount).sum()==1,"Wrong large round count");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void roundShotLimitsSurviveSaveAndContainerTransfer(GameTestHelper helper) {
        int[] calibers = {5, 20, 100, 105, 120, 160, 165, 300, 305, 490};
        int[] limits = {16, 16, 16, 8, 8, 8, 4, 4, 2, 2};
        for (int i = 0; i < calibers.length; i++) {
            helper.assertTrue(shot(calibers[i], 1).getMaxStackSize() == limits[i],
                    "Wrong stack limit at " + calibers[i] + " mm");
        }
        helper.assertTrue(new ItemStack(SiegeworksItems.STONE_CANNON_BALL.get()).getMaxStackSize() == 8,
                "The stone cannonball lost its stack limit");
        helper.assertTrue(new ItemStack(SiegeworksItems.GIANT_CANNON_BALL.get()).getMaxStackSize() == 2,
                "The giant stone cannonball lost its stack limit");

        var registries = helper.getLevel().registryAccess();
        var previous = MinecraftVersionCompat.writeItem(shot(490, 8), registries);
        var restored = MinecraftVersionCompat.readItem(previous, registries);
        helper.assertTrue(restored.getCount() == 8 && RoundShotItem.caliber(restored) == 490,
                "Loading an old stack lost rounds");

        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        player.getInventory().setItem(0, restored);
        var chest = new SimpleContainer(27);
        var menu = ChestMenu.threeRows(1, player.getInventory(), chest);
        for (int i = 0; i < 4; i++) menu.quickMoveStack(player, 54);
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "Shift-click left rounds in the hotbar");
        for (int i = 0; i < 4; i++) {
            var stack = chest.getItem(i);
            helper.assertTrue(stack.getCount() == 2 && RoundShotItem.caliber(stack) == 490,
                    "Shift-click did not respect the new limit");
        }
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void fullInventoryAndRemovedTableCannotConsumeIron(GameTestHelper h) {
        var p=SiegeGameTestPlayers.createRideable(h.getLevel());var pos=h.absolutePos(new BlockPos(2,2,2));
        h.getLevel().setBlock(pos,SiegeworksBlocks.CASTING_TABLE.get().defaultBlockState(),Block.UPDATE_ALL);p.setPos(Vec3.atCenterOf(pos));
        var menu=new CastingMenu(1,p.getInventory(),pos);
        for(int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,2));
        h.assertTrue(!menu.clickMenuButton(p,2) && p.getInventory().getItem(0).getCount()==2,"Full inventory consumed iron");
        p.getInventory().setItem(1,ItemStack.EMPTY);h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        h.assertTrue(!menu.clickMenuButton(p,2) && p.getInventory().getItem(0).getCount()==2,"Removed table still casts");h.succeed();
    }

    @GameTest(template="empty")
    public static void loadingRejectsWrongCaliberIncludingMidActionSwap(GameTestHelper h) {
        var gun=SiegeworksEntities.CULVERIN_ENTITY.get().create(h.getLevel());gun.setNoGravity(true);
        gun.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(4,4,4))));h.getLevel().addFreshEntity(gun);
        var p=SiegeGameTestPlayers.createRideable(h.getLevel());p.setPos(gun.position());p.startRiding(gun);gun.setOperator(p);gun.setLoadStage(2);
        p.setItemInHand(InteractionHand.MAIN_HAND,shot(120,1));gun.handleSiegeInteraction(p,InteractionHand.MAIN_HAND,h.getLevel());
        p.setItemInHand(InteractionHand.MAIN_HAND,shot(20,1));for(int i=0;i<1000;i++)gun.tick();
        h.assertTrue(gun.getLoadStage()==2 && p.getMainHandItem().getCount()==1,"Mid-action caliber swap loaded wrong round");
        var inv=new SimpleContainer(shot(20,3),shot(120,2));
        for(int i=0;i<1000 && gun.getLoadStage()==2;i++)gun.advancePrimaryAction(p,inv);
        h.assertTrue(gun.getLoadStage()==3 && inv.getItem(0).getCount()==3 && inv.getItem(1).getCount()==1,"Automated loader consumed wrong caliber");
        p.stopRiding();gun.discard();h.succeed();
    }
}
