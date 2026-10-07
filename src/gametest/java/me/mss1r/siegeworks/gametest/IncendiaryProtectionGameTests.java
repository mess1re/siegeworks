package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.gameplay.ballistics.ExplosionPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
//? if forge {
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

@GameTestHolder(IncendiaryProtectionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class IncendiaryProtectionGameTests {
    public static final String NAMESPACE = Siegeworks.MOD_ID + "_datapacks";
    private static final Set<BlockPos> NO_BREAK = ConcurrentHashMap.newKeySet();
    private static final Set<BlockPos> NO_PLACE = ConcurrentHashMap.newKeySet();

    static {
        //? if forge {
        /*MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
        *///?} else {
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
        //?}
            if (NO_BREAK.contains(event.getPos())) event.setCanceled(true);
        });
        //? if forge {
        /*MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
        *///?} else {
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
        //?}
            if (NO_PLACE.contains(event.getPos())) event.setCanceled(true);
        });
    }

    @GameTest(template = "empty")
    public static void fireRespectsTerrainRulesAndBothClaimEvents(GameTestHelper helper) {
        var before = SiegeworksServerConfig.getBlockDamage();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var breaker = SiegeGameTestPlayers.create(helper.getLevel());
        try {
            helper.assertTrue(!ignite(helper, pos, SiegeBlockDamage.NEVER, breaker, false, false), "NEVER allowed fire");
            helper.assertTrue(!ignite(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, null, false, false),
                    "Unattributed fire bypassed protection");
            helper.assertTrue(!ignite(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, true, false),
                    "Fire bypassed the protected supporting block");
            helper.assertTrue(!ignite(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, false, true),
                    "Fire bypassed a cancelled placement event");
            helper.assertTrue(ignite(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, false, false),
                    "An unclaimed area could not catch fire");
            helper.assertTrue(ignite(helper, pos, SiegeBlockDamage.EVERYWHERE, null, true, true),
                    "EVERYWHERE incorrectly respected a claim");
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
            NO_BREAK.clear();
            NO_PLACE.clear();
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kineticImpactsStillRespectProtection(GameTestHelper helper) {
        SiegeTerrainRuleGameTests.siegeWeaponsBreakBlocksOnlyWhereTheTerrainRuleAllows(helper);
    }

    @GameTest(template = "empty", batch = "debris_placement")
    public static void debrisLandingRespectsClaimsEvenAfterSaving(GameTestHelper helper) {
        var before = SiegeworksServerConfig.getBlockDamage();
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        var breaker = SiegeGameTestPlayers.create(helper.getLevel());
        try {
            helper.assertTrue(!land(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, true, DebrisKind.SAVED),
                    "Saved debris placed a block in a protected area");
            helper.assertTrue(land(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, false, DebrisKind.SAVED),
                    "Saved debris lost its responsible player or could not land in an unclaimed area");
            helper.assertTrue(!land(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, true, DebrisKind.LEGACY),
                    "Legacy debris bypassed a protected area");
            helper.assertTrue(land(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, breaker, false, DebrisKind.LEGACY),
                    "Legacy debris lost its responsible player");
            helper.assertTrue(!land(helper, pos, SiegeBlockDamage.NEVER, breaker, false, DebrisKind.LIVE),
                    "Debris placed a block while terrain damage was disabled");
            helper.assertTrue(land(helper, pos, SiegeBlockDamage.EVERYWHERE, breaker, true, DebrisKind.LIVE),
                    "EVERYWHERE blocked a debris landing");
            helper.assertTrue(land(helper, pos, SiegeBlockDamage.RESPECT_PROTECTION, null, true, DebrisKind.ORDINARY),
                    "Siegeworks intercepted an ordinary falling block");
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
            NO_BREAK.clear();
            NO_PLACE.clear();
        }
        helper.succeed();
    }

    private enum DebrisKind { LIVE, SAVED, LEGACY, ORDINARY }

    private static boolean land(GameTestHelper helper, BlockPos pos, SiegeBlockDamage rule, Player breaker,
                                  boolean claimed, DebrisKind kind) {
        var level = helper.getLevel();
        NO_BREAK.clear();
        NO_PLACE.clear();
        if (claimed) NO_PLACE.add(pos.immutable());
        for (BlockPos clear : BlockPos.betweenClosed(pos, pos.above(7))) {
            level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(pos.below(), Blocks.NETHERRACK.defaultBlockState(), 2);
        SiegeworksServerConfig.setBlockDamage(rule);
        FallingBlockEntity debris;
        if (kind == DebrisKind.ORDINARY) {
            debris = EntityType.FALLING_BLOCK.create(level);
            CompoundTag tag = new CompoundTag();
            tag.put("BlockState", NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState()));
            debris.load(tag);
            debris.dropItem = false;
        } else {
            BlockPos source = pos.above(5);
            level.setBlock(source, Blocks.STONE.defaultBlockState(), 2);
            helper.assertTrue(ExplosionPhysics.launchDestroyedBlock(level, source, Vec3.atCenterOf(source),
                    new Vec3(0, 1, 0), 2.0F, breaker), "No debris was launched for the landing test");
            debris = level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(source).inflate(3))
                    .stream().filter(entity -> entity.getStartPos().equals(source)).findFirst().orElseThrow();
            if (kind == DebrisKind.SAVED || kind == DebrisKind.LEGACY) {
                if (kind == DebrisKind.LEGACY) {
                    debris.getPersistentData().remove("axiomata:debris_context");
                }
                CompoundTag saved = new CompoundTag();
                debris.saveWithoutId(saved);
                debris.discard();
                debris = EntityType.FALLING_BLOCK.create(level);
                debris.load(saved);
                helper.assertTrue(debris.getPersistentData().getBoolean(ExplosionPhysics.TAG_DEBRIS),
                        "Saved debris lost its placement marker");
            }
        }
        debris.setPos(pos.getX() + 0.5D, pos.getY() + 3.0D, pos.getZ() + 0.5D);
        debris.setDeltaMovement(Vec3.ZERO);
        for (int tick = 0; tick < 60 && !debris.isRemoved(); tick++) {
            debris.tick();
        }
        helper.assertTrue(debris.isRemoved(), "Debris remained after its landing was resolved");
        return level.getBlockState(pos).is(Blocks.STONE);
    }

    private static boolean ignite(GameTestHelper helper, BlockPos pos, SiegeBlockDamage rule, Player breaker,
                                    boolean protectedFloor, boolean protectedPlacement) {
        var level = helper.getLevel();
        NO_BREAK.clear();
        NO_PLACE.clear();
        if (protectedFloor) NO_BREAK.add(pos.below());
        if (protectedPlacement) NO_PLACE.add(pos.immutable());
        level.setBlock(pos.below(), Blocks.NETHERRACK.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        SiegeworksServerConfig.setBlockDamage(rule);
        ProjectileImpacts.ignite(level, Vec3.atCenterOf(pos), new ProjectilePhysicsProfile.Fire(0.5D, 1.0D), breaker);
        return level.getBlockState(pos).is(Blocks.FIRE);
    }
}
