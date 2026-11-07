package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import me.mss1r.axiomata.ballistics.ProjectileCombat;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.gameplay.advancement.HwachaVolleys;
import me.mss1r.siegeworks.gameplay.advancement.SiegeAdvancement;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class SiegeAdvancementGameTests {
    @GameTest(template = "empty")
    public static void allCriteriaLoadAndFullBannerRackAwards(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        var level = helper.getLevel();
        var trebuchet = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
        var position = helper.absoluteVec(new Vec3(2, 2, 2));
        trebuchet.setPos(position);
        player.setPos(position.add(2, 0, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER, 2));
        trebuchet.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!SiegeAdvancement.FLY_YOUR_COLOURS.isDone(player), "Awarded before the rack was full");
        trebuchet.interact(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SiegeAdvancement.FLY_YOUR_COLOURS.isDone(player), "Full rack did not grant its advancement");

        for (SiegeAdvancement advancement : SiegeAdvancement.values()) {
            advancement.award(player);
            helper.assertTrue(advancement.isDone(player), "Missing advancement or criterion: " + advancement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyKillsFromTheSameVolleyCount(GameTestHelper helper) {
        ServerPlayer player = testPlayer(helper);
        var level = helper.getLevel();
        UUID volley = HwachaVolleys.start(level, player);
        UUID other = HwachaVolleys.start(level, player);
        kill(helper.spawn(EntityType.ZOMBIE, 1, 2, 1), player, volley, false);
        kill(helper.spawn(EntityType.ZOMBIE, 1, 2, 2), player, other, true);
        kill(helper.spawn(EntityType.ZOMBIE, 2, 2, 1), player, volley, true);
        kill(helper.spawn(EntityType.COW, 2, 2, 2), player, volley, true);
        helper.assertTrue(!SiegeAdvancement.MUNJONGS_VOLLEY.isDone(player),
                "A different volley or a passive animal counted toward the total");
        kill(helper.spawn(EntityType.ZOMBIE, 1, 2, 3), player, volley, true);
        helper.assertTrue(SiegeAdvancement.MUNJONGS_VOLLEY.isDone(player), "Three kills in one volley did not count");
        helper.succeed();
    }

    private static void kill(LivingEntity target, ServerPlayer player, UUID volley, boolean blast) {
        var rocket = new SingijeonProjectile(SiegeworksEntities.SINGIJEON_PROJECTILE.get(), target.level());
        rocket.setAdvancementVolley(volley);
        target.hurt(ProjectileCombat.source(target.level(),
                blast ? ProjectileCombat.PHYSICAL_BLAST : ProjectileCombat.PHYSICAL_PROJECTILE, rocket, player), 1000);
    }

    private static ServerPlayer testPlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "advancement_test");
        //? if <1.21 {
        /*return new ServerPlayer(level.getServer(), level, profile) {
        *///?} else {
        return new ServerPlayer(level.getServer(), level, profile, net.minecraft.server.level.ClientInformation.createDefault()) {
        //?}
            @Override
            public void displayClientMessage(Component message, boolean overlay) {}
        };
    }
}
