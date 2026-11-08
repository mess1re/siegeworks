package me.mss1r.siegeworks.gametest;

import com.talhanation.recruits.entities.RecruitEntity;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ai.RecruitMeleeAttackGoal;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.gameplay.firearm.FirearmLoad;
import me.mss1r.siegeworks.gameplay.firearm.FirearmStack;
import me.mss1r.siegeworks.integration.recruits.RecruitsFirearmGoal;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import java.util.UUID;

public final class RecruitsFirearmGameTests {
    @GameTest(template = "empty", timeoutTicks = 440)
    public static void handCannonLoadsAndFiresWithRealSupplies(GameTestHelper helper) {
        fullCycle(helper, false, 430);
    }

    @GameTest(template = "empty", timeoutTicks = 530)
    public static void arquebusLoadsLightsMatchAndFires(GameTestHelper helper) {
        fullCycle(helper, true, 520);
    }

    private static void fullCycle(GameTestHelper helper, boolean matchlock, int finish) {
        TestRecruit recruit = recruit(helper, new BlockPos(2, 2, 2), UUID.randomUUID());
        Zombie target = target(helper);
        recruit.setTarget(target);
        ItemStack weapon = new ItemStack(matchlock ? SiegeworksItems.ARQUEBUS.get() : SiegeworksItems.HAND_CANNON.get());
        ItemStack shield = new ItemStack(Items.SHIELD);
        ItemStack ramrod = new ItemStack(SiegeworksItems.FIREARM_RAMROD.get());
        ItemStack igniter = new ItemStack(Items.FLINT_AND_STEEL);
        recruit.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        recruit.setItemInHand(InteractionHand.OFF_HAND, shield);
        recruit.getInventory().setItem(6, new ItemStack(SiegeworksItems.BLACK_POWDER.get()));
        recruit.getInventory().setItem(7, ball(matchlock ? 15 : 20));
        recruit.getInventory().setItem(8, ramrod);
        recruit.getInventory().setItem(9, igniter);
        if (matchlock) recruit.getInventory().setItem(10, new ItemStack(Items.STRING));
        recruit.firearmGoal();
        recruit.setNoAi(false);
        helper.runAtTickTime(finish, () -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Recruit did not hit its target");
            helper.assertTrue(weapon.getDamageValue() == 1, "Weapon did not fire exactly once");
            helper.assertTrue(recruit.getInventory().getItem(6).isEmpty()
                    && recruit.getInventory().getItem(7).isEmpty(), "Shot did not consume powder and the ball");
            helper.assertTrue(ramrod.getDamageValue() == 1 && igniter.getDamageValue() == 1,
                    "Loading tools did not wear exactly once");
            helper.assertTrue(recruit.getOffhandItem() == shield && recruit.getInventory().getItem(8) == ramrod
                    && recruit.getInventory().getItem(9) == igniter, "Borrowed tools or shield were not returned");
            var load = FirearmStack.load(weapon);
            helper.assertTrue(load.charge() == FirearmLoad.Charge.EMPTY, "Weapon retained a fired charge");
            if (matchlock) helper.assertTrue(load.matchLit() && load.matchShots() == 15
                    && recruit.getInventory().getItem(10).isEmpty(), "Match was not fitted, lit and consumed correctly");
            recruit.discard(); target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 115)
    public static void moveOrderCancelsLoadingWithoutLosingOrDuplicatingSupplies(GameTestHelper helper) {
        TestRecruit recruit = recruit(helper, new BlockPos(2, 2, 2), UUID.randomUUID());
        ItemStack weapon = new ItemStack(SiegeworksItems.HAND_CANNON.get());
        ItemStack shield = new ItemStack(Items.SHIELD);
        ItemStack powder = new ItemStack(SiegeworksItems.BLACK_POWDER.get());
        recruit.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        recruit.setItemInHand(InteractionHand.OFF_HAND, shield);
        recruit.getInventory().setItem(6, powder);
        drive(helper, recruit);
        helper.runAtTickTime(10, () -> recruit.setShouldMovePos(true));
        helper.runAtTickTime(15, () -> {
            helper.assertTrue(recruit.getOffhandItem() == shield && recruit.getInventory().getItem(6) == powder
                    && powder.getCount() == 1 && FirearmStack.load(weapon).charge() == FirearmLoad.Charge.EMPTY,
                    "Cancelling before the pour consumed or lost material");
            recruit.setShouldMovePos(false);
        });
        helper.runAtTickTime(75, () -> recruit.setShouldMovePos(true));
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(recruit.getOffhandItem() == shield && recruit.getInventory().getItem(6).isEmpty()
                    && FirearmStack.load(weapon).charge() == FirearmLoad.Charge.POWDER,
                    "Cancelling after the pour lost progress or returned spent powder");
            recruit.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 190)
    public static void holdsFireBehindAnAllyOrWall(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        TestRecruit recruit = recruit(helper, new BlockPos(2, 2, 2), owner);
        TestRecruit ally = recruit(helper, new BlockPos(2, 2, 6), owner);
        Zombie target = target(helper);
        recruit.setTarget(target);
        ItemStack weapon = new ItemStack(SiegeworksItems.ARQUEBUS.get());
        FirearmStack.save(weapon, new FirearmLoad(FirearmLoad.Charge.READY, 16, true));
        recruit.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        drive(helper, recruit);
        helper.runAtTickTime(50, () -> {
            helper.assertTrue(weapon.getDamageValue() == 0, "Recruit fired through its ally");
            helper.setBlock(new BlockPos(2, 3, 8), Blocks.STONE);
            ally.discard();
        });
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(weapon.getDamageValue() == 0, "Recruit fired through a wall");
            helper.setBlock(new BlockPos(2, 3, 8), Blocks.AIR);
        });
        helper.runAtTickTime(175, () -> {
            helper.assertTrue(weapon.getDamageValue() == 1 && target.getHealth() < target.getMaxHealth(),
                    "Recruit did not fire after its line of fire cleared");
            recruit.discard(); target.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void refusesWrongCalibre(GameTestHelper helper) {
        TestRecruit recruit = recruit(helper, new BlockPos(2, 2, 2), UUID.randomUUID());
        ItemStack weapon = new ItemStack(SiegeworksItems.HAND_CANNON.get());
        FirearmStack.save(weapon, new FirearmLoad(FirearmLoad.Charge.POWDER, 0, false));
        recruit.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        recruit.getInventory().setItem(6, ball(15));
        helper.assertTrue(!recruit.firearmGoal().canUse(), "Hand cannon accepted a 15 mm arquebus ball");
        recruit.getInventory().setItem(7, ball(20));
        helper.assertTrue(recruit.firearmGoal().canUse(), "Hand cannon rejected its 20 mm ball");
        recruit.discard(); helper.succeed();
    }

    private static ItemStack ball(int calibre) {
        return RoundShotItem.withCaliber(new ItemStack(SiegeworksItems.CANNON_BALL.get()), calibre);
    }

    @GameTest(template = "empty")
    public static void recruitsReturnToTheirFirearmAfterMelee(GameTestHelper helper) {
        Zombie target = target(helper);
        for (AbstractRecruitEntity recruit : new AbstractRecruitEntity[]{
                RecruitsTestSuites.<RecruitEntity>entityType("recruit").create(helper.getLevel()),
                RecruitsTestSuites.<AbstractRecruitEntity>entityType("crossbowman").create(helper.getLevel())}) {
            ItemStack weapon = new ItemStack(SiegeworksItems.ARQUEBUS.get());
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            recruit.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
            recruit.setTarget(target);
            recruit.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            recruit.getInventory().setItem(6, sword);
            recruit.getInventory().setItem(7, new ItemStack(Items.CROSSBOW));
            recruit.setShouldRanged(true);
            var melee = new RecruitMeleeAttackGoal(recruit, 1, 32);
            melee.start();
            helper.assertTrue(recruit.getMainHandItem() == sword, "Recruit did not switch to its melee weapon");
            helper.assertTrue(!melee.canContinueToUse(), "Recruit kept chasing a distant target with its melee weapon");
            melee.stop();
            helper.assertTrue(recruit.getMainHandItem() == weapon, "Ending melee did not restore the selected firearm");
        }
        target.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void savedLoadingSwapRestoresExistingStacksAfterReload(GameTestHelper helper) {
        TestRecruit recruit = recruit(helper, new BlockPos(2, 2, 2), UUID.randomUUID());
        recruit.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SiegeworksItems.HAND_CANNON.get()));
        recruit.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        recruit.getInventory().setItem(6, new ItemStack(SiegeworksItems.BLACK_POWDER.get()));
        drive(helper, recruit);
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(recruit.getOffhandItem().is(SiegeworksItems.BLACK_POWDER.get()), "Fixture did not borrow powder");
            var saved = new net.minecraft.nbt.CompoundTag();
            recruit.saveWithoutId(saved);
            recruit.firearmGoal().stop();
            recruit.discard();
            TestRecruit loaded = new TestRecruit(helper.getLevel());
            loaded.load(saved);
            loaded.firearmGoal().canUse();
            helper.assertTrue(loaded.getOffhandItem().is(Items.SHIELD)
                    && loaded.getInventory().getItem(6).is(SiegeworksItems.BLACK_POWDER.get())
                    && loaded.getInventory().getItem(6).getCount() == 1,
                    "Reloading a borrowed material did not restore the two existing stacks");
            helper.succeed();
        });
    }

    private static void drive(GameTestHelper helper, TestRecruit recruit) {
        RecruitsFirearmGoal goal = recruit.firearmGoal();
        boolean[] active = {false};
        helper.onEachTick(() -> {
            if (!recruit.isAlive()) return;
            if (goal.canUse()) {
                if (!active[0]) { goal.start(); active[0] = true; }
                goal.tick();
            } else if (active[0]) { goal.stop(); active[0] = false; }
        });
    }

    private static TestRecruit recruit(GameTestHelper helper, BlockPos position, UUID owner) {
        TestRecruit recruit = new TestRecruit(helper.getLevel());
        recruit.setPos(Vec3.atBottomCenterOf(helper.absolutePos(position)));
        recruit.setNoAi(true); recruit.setNoGravity(true); recruit.setInvulnerable(true);
        recruit.setOwnerUUID(Optional.of(owner)); recruit.setIsOwned(true);
        recruit.setHunger(100); recruit.setShouldRanged(true);
        helper.getLevel().addFreshEntity(recruit);
        return recruit;
    }

    private static Zombie target(GameTestHelper helper) {
        Zombie target = EntityType.ZOMBIE.create(helper.getLevel());
        target.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 11))));
        target.setNoAi(true); target.setNoGravity(true);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        helper.getLevel().addFreshEntity(target);
        return target;
    }

    private static final class TestRecruit extends RecruitEntity {
        TestRecruit(Level level) { super(RecruitsTestSuites.<RecruitEntity>entityType("recruit"), level); }
        RecruitsFirearmGoal firearmGoal() {
            return goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal())
                    .filter(RecruitsFirearmGoal.class::isInstance).map(RecruitsFirearmGoal.class::cast)
                    .findFirst().orElseThrow(() -> new AssertionError("Firearm goal was not installed on the recruit"));
        }
    }
}
