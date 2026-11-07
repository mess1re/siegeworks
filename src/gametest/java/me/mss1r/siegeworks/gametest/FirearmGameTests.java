package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.gameplay.firearm.*;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.Charge.*;

public final class FirearmGameTests {
    @GameTest(template = "empty", timeoutTicks = 225)
    public static void ramrodWearsOnCommitAndRestoresOffhandWhenBroken(GameTestHelper helper) {
        var player = SiegeGameTestPlayers.create(helper.getLevel());
        player.getAbilities().instabuild = false;
        var weapon = new ItemStack(SiegeworksItems.ARQUEBUS.get());
        var gun = (FirearmItem) weapon.getItem();
        var ramrod = new ItemStack(SiegeworksItems.FIREARM_RAMROD.get());
        helper.assertTrue(ramrod.getMaxDamage() == 256, "Firearm ramrod has incorrect durability");
        ramrod.setDamageValue(255);
        var originalOffhand = new ItemStack(Items.SHIELD);
        FirearmStack.save(weapon, new FirearmLoad(BALL, 0, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.setItemInHand(InteractionHand.OFF_HAND, originalOffhand);
        int slot = (player.getInventory().selected + 1) % 9;
        player.getInventory().setItem(slot, ramrod);
        helper.onEachTick(() -> {
            if (player.isUsingItem()) FirearmActions.tick(player, weapon, gun);
        });
        helper.runAtTickTime(1, () -> {
            gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getOffhandItem() == ramrod, "The loading action did not borrow the ramrod");
        });
        helper.runAtTickTime(10, () -> {
            player.releaseUsingItem();
            helper.assertTrue(player.getInventory().getItem(slot) == ramrod && ramrod.getDamageValue() == 255
                            && FirearmStack.load(weapon).charge() == BALL && player.getOffhandItem() == originalOffhand,
                    "Cancelling before the bullet was rammed should preserve the ramrod and return the offhand item");
            gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        });
        helper.runAtTickTime(210, () -> {
            helper.assertTrue(FirearmStack.load(weapon).charge() == READY, "The last ramrod use did not seat the bullet");
            helper.assertTrue(player.getInventory().getItem(slot).isEmpty() && player.getOffhandItem() == originalOffhand,
                    "A broken borrowed ramrod should leave its hotbar slot empty and restore the offhand");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 165)
    public static void hotbarFeedReturnsOnlyUnspentMaterial(GameTestHelper helper) {
        var player = SiegeGameTestPlayers.create(helper.getLevel());
        player.getAbilities().instabuild = false;
        var weapon = new ItemStack(SiegeworksItems.HAND_CANNON.get());
        var gun = (FirearmItem) weapon.getItem();
        var originalOffhand = new ItemStack(Items.APPLE, 2);
        var powder = new ItemStack(SiegeworksItems.BLACK_POWDER.get(), 1);
        FirearmStack.save(weapon, new FirearmLoad(EMPTY, 0, false));
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        player.setItemInHand(InteractionHand.OFF_HAND, originalOffhand);
        player.getInventory().setItem(3, powder);
        helper.onEachTick(() -> {
            if (player.isUsingItem()) FirearmActions.tick(player, weapon, gun);
        });
        helper.runAtTickTime(1, () -> {
            gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getOffhandItem() == powder && player.getInventory().getItem(3) == originalOffhand,
                    "Loading should borrow powder and store the displaced item");
        });
        helper.runAtTickTime(10, () -> {
            player.releaseUsingItem();
            helper.assertTrue(player.getOffhandItem() == originalOffhand && player.getInventory().getItem(3) == powder
                    && powder.getCount() == 1 && FirearmStack.load(weapon).charge() == EMPTY,
                    "Cancelling before the pour should undo the temporary swap");
            gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        });
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(player.getOffhandItem() == originalOffhand && player.getInventory().getItem(3).isEmpty()
                    && FirearmStack.load(weapon).charge() == POWDER && !player.isUsingItem(),
                    "Finishing the pour should restore the offhand and consume only the powder");
            helper.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=150)
    public static void committedMaterialSurvivesCancellation(GameTestHelper h) {
        var p=SiegeGameTestPlayers.create(h.getLevel());p.getAbilities().instabuild=false;
        var stack=new ItemStack(SiegeworksItems.HAND_CANNON.get());var gun=(FirearmItem)stack.getItem();
        FirearmStack.save(stack,new FirearmLoad(EMPTY,0,false));
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(SiegeworksItems.BLACK_POWDER.get(),2));
        gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.onEachTick(()->{if(p.isUsingItem())FirearmActions.tick(p,stack,gun);});
        h.runAtTickTime(55,()->{
            h.assertTrue(FirearmStack.load(stack).charge()==POWDER && p.getOffhandItem().getCount()==1,
                    "Pouring did not commit with the animation");
            p.releaseUsingItem();
            h.assertTrue(FirearmStack.load(stack).charge()==POWDER,"Cancellation refunded committed powder");
            h.assertTrue(!MinecraftVersionCompat.customData(stack).contains("FirearmAction"),"Cancellation retained animation");
            h.assertTrue(!gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Powder accepted twice");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=180)
    public static void materialsAreCheckedAtCommit(GameTestHelper h) {
        var p=SiegeGameTestPlayers.create(h.getLevel());p.getAbilities().instabuild=false;
        var stack=new ItemStack(SiegeworksItems.HAND_CANNON.get());var gun=(FirearmItem)stack.getItem();
        FirearmStack.save(stack,new FirearmLoad(EMPTY,0,false));
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(SiegeworksItems.BLACK_POWDER.get(),2));
        h.assertTrue(gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Powder action refused");
        h.onEachTick(()->{if(p.isUsingItem())FirearmActions.tick(p,stack,gun);});
        h.runAtTickTime(12,()->p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.DIRT)));
        h.runAtTickTime(20,()->{
            h.assertTrue(!p.isUsingItem() && FirearmStack.load(stack).charge()==EMPTY,"Swapped material completed loading");
            FirearmStack.save(stack,new FirearmLoad(POWDER,0,false));
            p.setItemInHand(InteractionHand.OFF_HAND,RoundShotItem.withCaliber(new ItemStack(SiegeworksItems.CANNON_BALL.get(),2),15));
            h.assertTrue(!gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Wrong caliber accepted");
            p.setItemInHand(InteractionHand.OFF_HAND,RoundShotItem.withCaliber(new ItemStack(SiegeworksItems.CANNON_BALL.get(),2),20));
            gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        });
        h.runAtTickTime(100,()->{
            h.assertTrue(FirearmStack.load(stack).charge()==BALL && p.getOffhandItem().getCount()==1,"Bullet was not consumed exactly once");
            h.assertTrue(!MinecraftVersionCompat.customData(stack).contains("FirearmAction"),"Finished action remained on item");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=110)
    public static void repeatedFirePacketsCannotCreateExtraShots(GameTestHelper h) {
        var p=SiegeGameTestPlayers.create(h.getLevel());p.getAbilities().instabuild=false;
        var stack=new ItemStack(SiegeworksItems.ARQUEBUS.get());var gun=(FirearmItem)stack.getItem();
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        FirearmStack.save(stack,new FirearmLoad(READY,16,true));
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,10,2)));p.setXRot(-40);
        gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.onEachTick(()->{if(p.isUsingItem())FirearmActions.tick(p,stack,gun);});
        h.runAtTickTime(1,()->{
            for(int i=0;i<20;i++)FirearmActions.requestFire(p);
            h.assertTrue(FirearmStack.load(stack).charge()==EMPTY && FirearmStack.load(stack).matchShots()==15,
                    "A lit match must fire on the trigger press, once, without an aim/ignition timer");
            h.assertTrue(stack.getMaxDamage() == 128 && stack.getDamageValue() == 1,
                    "An arquebus must spend one of 128 durability on a shot");
        });
        h.runAtTickTime(45,()->{
            var load=FirearmStack.load(stack);
            h.assertTrue(load.charge()==EMPTY && load.matchShots()==15,"Shot did not commit exactly once");
            for(int i=0;i<20;i++)FirearmActions.requestFire(p);
        });
        h.runAtTickTime(85,()->{
            h.assertTrue(FirearmStack.load(stack).matchShots()==15,"Repeated fire consumed another shot");
            h.assertTrue(stack.getDamageValue() == 1, "Repeated fire spent additional durability");
            h.assertTrue(!p.isUsingItem(),"Recovery never ended");h.succeed();
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void handCannonRequiresIgniterAndPersistsCharge(GameTestHelper h) {
        var p=SiegeGameTestPlayers.create(h.getLevel());p.getAbilities().instabuild=false;
        var stack=new ItemStack(SiegeworksItems.HAND_CANNON.get());var gun=(FirearmItem)stack.getItem();
        FirearmStack.save(stack,new FirearmLoad(READY,0,false));
        var restored=MinecraftVersionCompat.readItem(MinecraftVersionCompat.writeItem(stack,h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(FirearmStack.load(restored).charge()==READY,"Save lost charge");
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,10,2)));p.setXRot(-40);
        gun.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.onEachTick(()->{if(p.isUsingItem())FirearmActions.tick(p,stack,gun);});
        h.runAtTickTime(15,()->{
            FirearmActions.requestFire(p);
            h.assertTrue(MinecraftVersionCompat.customData(stack).getString("FirearmAction").equals("AIM"),"Fired without flint");
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.FLINT_AND_STEEL));FirearmActions.requestFire(p);
        });
        h.runAtTickTime(44,()->{
            h.assertTrue(FirearmStack.load(stack).charge()==READY,"Hand cannon fired before the fuse finished");
            h.assertTrue(stack.getDamageValue() == 0, "Ignition spent weapon durability before the shot");
        });
        h.runAtTickTime(50,()->{
            h.assertTrue(FirearmStack.load(stack).charge()==EMPTY && p.getOffhandItem().getDamageValue()==1,"Igniter/charge consumption failed");
            h.assertTrue(stack.getMaxDamage() == 160 && stack.getDamageValue() == 1,
                    "A hand cannon must spend one of 160 durability on a shot");
            h.assertTrue(gun.isValidRepairItem(stack, new ItemStack(Items.IRON_INGOT)), "Iron must repair the firearm");
            h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=70)
    public static void breakingHandCannonRestoresBorrowedIgniter(GameTestHelper helper) {
        var player = SiegeGameTestPlayers.create(helper.getLevel());
        player.getAbilities().instabuild = false;
        var stack = new ItemStack(SiegeworksItems.HAND_CANNON.get());
        var gun = (FirearmItem) stack.getItem();
        stack.setDamageValue(stack.getMaxDamage() - 1);
        FirearmStack.save(stack, new FirearmLoad(READY, 0, false));
        var igniter = new ItemStack(Items.FLINT_AND_STEEL);
        var offhand = new ItemStack(Items.SHIELD);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        int hotbarSlot = (player.getInventory().selected + 1) % 9;
        player.getInventory().setItem(hotbarSlot, igniter);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 10, 2)));
        player.setXRot(-40);
        gun.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.onEachTick(() -> {
            if (player.isUsingItem()) FirearmActions.tick(player, stack, gun);
        });
        helper.runAtTickTime(1, () -> FirearmActions.requestFire(player));
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(player.getMainHandItem().isEmpty(), "The last shot did not break the weapon");
            helper.assertTrue(player.getOffhandItem() == offhand, "The original offhand item was not restored");
            helper.assertTrue(player.getInventory().getItem(hotbarSlot) == igniter && igniter.getDamageValue() == 1,
                    "The borrowed igniter was not restored with its updated durability");
            helper.assertTrue(!player.isUsingItem(), "The broken weapon left the player in the firing action");
            helper.succeed();
        });
    }
}
