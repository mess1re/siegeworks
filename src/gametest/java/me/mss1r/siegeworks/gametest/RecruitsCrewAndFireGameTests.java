package me.mss1r.siegeworks.gametest;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import me.mss1r.siegeworks.integration.recruits.SiegeworksRecruitController;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsTowerCrewC2SPayload;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RecruitsCrewAndFireGameTests {
    @GameTest(template = "empty")
    public static void crewCommandFillsPushBarsBeforeInterior(GameTestHelper helper) {
        assertTowerCrew(helper, false);
    }

    @GameTest(template = "empty")
    public static void boardCommandFillsPushBarsBeforeInterior(GameTestHelper helper) {
        assertTowerCrew(helper, true);
    }

    private static void assertTowerCrew(GameTestHelper helper, boolean boardCommand) {
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8)));
        ServerPlayer commander = SiegeGameTestPlayers.create(helper.getLevel());
        commander.setPos(origin);
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(helper.getLevel());
        tower.setPos(origin);
        tower.claimOwnership(commander.getUUID());
        helper.getLevel().addFreshEntity(tower);
        List<SiegeEngineerEntity> crew = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            crew.add(engineer(helper, commander, origin.add(0, 0, index + 1)));
        }
        int assigned = boardCommand
                ? RecruitsCompat.applyTowerCrew(commander, tower, RecruitsTowerCrewC2SPayload.ACTION_BOARD, crew::contains)
                : RecruitsCompat.crewTower(commander, tower, crew::contains);
        helper.assertTrue(assigned == 8, "Crew command accepted only " + assigned + " engineers");
        for (SiegeEngineerEntity unit : crew) {
            helper.assertTrue(unit.startRiding(tower, true), "Reserved crew member could not board");
        }
        helper.assertTrue(crew.stream().filter(tower::isOperator).count() == 1, "Tower did not get one driver");
        helper.assertTrue(crew.stream().filter(tower::isPusher).count() == 5,
                "Extra engineers went inside before the five push bars were filled");
        helper.assertTrue(crew.stream().filter(tower::isInteriorPassenger).count() == 2,
                "Remaining engineers did not get interior seats");
        crew.forEach(unit -> { unit.stopRiding(); unit.discard(); });
        tower.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void positionOrderFiresWithoutAnRtsZone(GameTestHelper helper) {
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 10, 8)));
        ServerPlayer commander = SiegeGameTestPlayers.create(helper.getLevel());
        SiegeEngineerEntity engineer = engineer(helper, commander, origin);
        TowerCrossbowEntity crossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(helper.getLevel());
        crossbow.setPos(origin);
        crossbow.claimOwnership(commander.getUUID());
        helper.getLevel().addFreshEntity(crossbow);
        helper.assertTrue(engineer.startRiding(crossbow, true), "Engineer could not man crossbow");
        crossbow.setOperator(engineer);
        crossbow.setAmmoLoaded("siegeworks:tower_crossbow_bolt");
        crossbow.setWindingTime(0);
        crossbow.setCooldown(0);
        engineer.setStrategicFirePos(BlockPos.containing(origin.add(0, 1, 20)));
        engineer.setShouldStrategicFire(true);
        engineer.setShouldRanged(true);
        var controller = new SiegeworksRecruitController(engineer, crossbow);
        boolean foundTarget = controller.updateAttacking();
        helper.assertTrue(foundTarget, "Point order was rejected because it had no RTS fire zone");
        for (int tick = 0; tick < 120 && crossbow.hasAmmoLoaded(); tick++) {
            crossbow.onSiegeTick(helper.getLevel());
            controller.updateAttacking();
        }
        helper.assertTrue(!crossbow.hasAmmoLoaded(), "Engineer aimed but did not fire at the point");
        var shots = new ArrayList<net.minecraft.world.entity.Entity>();
        for (var entity : helper.getLevel().getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    && (projectile.getOwner() == crossbow || projectile.getOwner() == engineer)) shots.add(entity);
        }
        shots.forEach(net.minecraft.world.entity.Entity::discard);
        engineer.stopRiding();
        engineer.discard();
        crossbow.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativePositionCommandReplacesZoneEvenAtSameCenter(GameTestHelper helper) {
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        ServerPlayer commander = SiegeGameTestPlayers.create(helper.getLevel());
        commander.setPos(origin);
        commander.setYRot(0);
        commander.setXRot(0);
        BlockPos wall = BlockPos.containing(origin.add(0, commander.getEyeHeight(), 3));
        helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        HitResult hit = commander.pick(200, 1, true);
        helper.assertTrue(hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK,
                "Test player did not point at a block: eye=" + commander.getEyePosition()
                        + ", view=" + commander.getViewVector(1) + ", wall=" + wall + ", hit=" + hit.getLocation() + "/" + hit.getType());
        BlockPos target = ((BlockHitResult) hit).getBlockPos();
        SiegeEngineerEntity engineer = engineer(helper, commander, origin.add(1, 0, 0));
        engineer.setStrategicFirePos(target);
        me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer).put("SiegeworksFireZone", new CompoundTag());
        CommandEvents.onStrategicFireCommand(commander, commander.getUUID(), engineer, null, true);
        helper.assertTrue(engineer.getShouldStrategicFire(), "Native command did not accept the engineer");
        helper.assertTrue(!me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer).contains("SiegeworksFireZone"),
                "Old zone survived a new point order at its center");
        helper.assertTrue(target.equals(engineer.getStrategicFirePos()), "Native target was changed");
        engineer.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void restoringSavedStrategicPointDoesNotClearZone(GameTestHelper helper) {
        SiegeEngineerEntity engineer = engineer(helper, SiegeGameTestPlayers.create(helper.getLevel()),
                Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8))));
        engineer.setStrategicFirePos(helper.absolutePos(new BlockPos(8, 8, 16)));
        me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer).put("SiegeworksFireZone", new CompoundTag());
        CompoundTag saved = new CompoundTag();
        engineer.addAdditionalSaveData(saved);
        engineer.readAdditionalSaveData(saved);
        helper.assertTrue(me.mss1r.siegeworks.platform.SiegePlatform.persistentData(engineer).contains("SiegeworksFireZone"),
                "Loading an engineer's saved position deleted its fire zone");
        engineer.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recruitsNavigationBlocksPhysicalMachineGeometry(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8)));
        var tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        tower.setPos(origin);
        level.addFreshEntity(tower);
        var recruit = engineer(helper, SiegeGameTestPlayers.create(level), origin.add(10, 0, 0));
        var evaluator = new com.talhanation.recruits.entities.ai.navigation.RecruitsPathNodeEvaluator();
        BlockPos center = BlockPos.containing(origin);
        var region = new net.minecraft.world.level.PathNavigationRegion(level, center.offset(-32, -32, -32), center.offset(32, 32, 32));
        evaluator.prepare(region, recruit);
        int checked = 0;
        try {
            for (BlockPos node : BlockPos.betweenClosed(center.offset(-4, 0, -4), center.offset(4, 8, 4))) {
                if (!me.mss1r.axiomata.collision.system.StructurePathfindingSystem.blocked(recruit, node.getX(), node.getY(), node.getZ())) continue;
                //? if <1.21 {
                /*var type = evaluator.getBlockPathType(level, node.getX(), node.getY(), node.getZ(), recruit);
                helper.assertTrue(type == net.minecraft.world.level.pathfinder.BlockPathTypes.BLOCKED,
                        "Recruits bypassed structure collision classification at " + node + ": " + type);
                *///?} else {
                var context = new net.minecraft.world.level.pathfinder.PathfindingContext(region, recruit);
                var type = evaluator.getPathTypeOfMob(context, node.getX(), node.getY(), node.getZ(), recruit);
                helper.assertTrue(type == net.minecraft.world.level.pathfinder.PathType.BLOCKED,
                        "Recruits bypassed inherited structure collision classification at " + node + ": " + type);
                //?}
                checked++;
            }
            helper.assertTrue(checked > 0, "The navigation fixture did not sample any physical machine cells");
        } finally {
            evaluator.done();
            recruit.discard();
            tower.discard();
        }
        helper.succeed();
    }

    private static SiegeEngineerEntity engineer(GameTestHelper helper, ServerPlayer commander, Vec3 position) {
        SiegeEngineerEntity engineer = ModEntityTypes.SIEGE_ENGINEER.get().create(helper.getLevel());
        engineer.setPos(position);
        engineer.setNoAi(true);
        engineer.setOwnerUUID(Optional.of(commander.getUUID()));
        engineer.setIsOwned(true);
        engineer.setListen(true);
        helper.getLevel().addFreshEntity(engineer);
        return engineer;
    }
}
