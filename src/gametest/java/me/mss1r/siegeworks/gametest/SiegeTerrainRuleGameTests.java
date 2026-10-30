package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiegeTerrainRuleGameTests {
    /** Blocks a stand-in claim mod refuses to let anyone break. */
    private static final Set<BlockPos> CLAIMED = ConcurrentHashMap.newKeySet();

    static {
        //? if <1.21 {
        /*MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
        *///?} else {
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
        //?}
            if (CLAIMED.contains(event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    private SiegeTerrainRuleGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void siegeWeaponsBreakBlocksOnlyWhereTheTerrainRuleAllows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player breaker = SiegeGameTestPlayers.create(level);
        SiegeBlockDamage before = SiegeworksServerConfig.getBlockDamage();
        try {
            helper.assertTrue(broken(helper, SiegeBlockDamage.RESPECT_PROTECTION, true, breaker) == 0,
                    "A siege impact broke blocks a claim protects while protection was respected");
            helper.assertTrue(broken(helper, SiegeBlockDamage.RESPECT_PROTECTION, false, breaker) > 0,
                    "A siege impact left unclaimed blocks whole while protection was respected");
            helper.assertTrue(broken(helper, SiegeBlockDamage.NEVER, false, breaker) == 0,
                    "A siege impact broke blocks although siege block damage was off");
            helper.assertTrue(broken(helper, SiegeBlockDamage.EVERYWHERE, true, breaker) > 0,
                    "A siege impact deferred to a claim although it may break blocks everywhere");
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
            CLAIMED.clear();
        }
        helper.succeed();
    }

    /** Builds a fresh stone block, strikes its face hard and counts the stone that is gone. */
    private static int broken(GameTestHelper helper, SiegeBlockDamage rule, boolean claimed, Player breaker) {
        ServerLevel level = helper.getLevel();
        SiegeworksServerConfig.setBlockDamage(rule);
        CLAIMED.clear();
        BlockPos corner = helper.absolutePos(new BlockPos(4, 1, 4));
        for (BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(4, 4, 4))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        for (BlockPos pos : BlockPos.betweenClosed(corner.offset(1, 1, 1), corner.offset(3, 3, 3))) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            if (claimed) {
                CLAIMED.add(pos.immutable());
            }
        }
        Vec3 face = Vec3.atCenterOf(corner.offset(2, 2, 1)).add(0.0D, 0.0D, -0.5D);
        ProjectileImpacts.crush(level, face, new Vec3(0.0D, 0.0D, -1.0D), new Vec3(0.0D, 0.0D, 1.0D), 10_000_000.0D,
                1.0D, 0.0D, 6.0D, breaker);
        int gone = 0;
        for (BlockPos pos : BlockPos.betweenClosed(corner.offset(1, 1, 1), corner.offset(3, 3, 3))) {
            if (!level.getBlockState(pos).is(Blocks.STONE)) {
                gone++;
            }
        }
        return gone;
    }
}
