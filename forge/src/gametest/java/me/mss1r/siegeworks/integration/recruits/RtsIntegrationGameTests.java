package me.mss1r.siegeworks.integration.recruits;

import com.talhanation.recruits.entities.SiegeEngineerEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.siegeworks.gametest.SiegeGameTestPlayers;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import java.util.UUID;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Optional;

/** Exercises the registered RTS API listeners and providers, not their private implementations. */
public final class RtsIntegrationGameTests {
    @GameTest(template = "empty")
    public static void mapFireOrdersReachOnlySelectedOwnedEngineers(GameTestHelper helper) {
        var commander = commander(helper);
        var owned = engineer(helper, commander, 2);
        var unselected = engineer(helper, commander, 3);
        var foreign = engineer(helper, SiegeGameTestPlayers.create(helper.getLevel()), 4);
        var guns = List.of(artillery(helper, owned), artillery(helper, unselected), artillery(helper, foreign));
        BlockPos center = helper.absolutePos(new BlockPos(8, 1, 8));
        try {
            RecruitsRTSCommandApi.areaFireOrdered(commander,
                    List.of(owned.getUUID(), owned.getUUID(), foreign.getUUID()), center, 8, 3, FireZoneShape.RECTANGLE);
            helper.assertTrue(RecruitsFireZone.hasZone(owned), "Registered RTS listener did not set an engineer's fire zone");
            helper.assertTrue(!RecruitsFireZone.hasZone(unselected) && !RecruitsFireZone.hasZone(foreign),
                    "Map order changed an unselected or foreign engineer");
            for (int sample = 0; sample < 32; sample++) {
                var target = RecruitsFireZone.resolveTarget(owned, 0, 0, sample);
                helper.assertTrue(target != null && Math.abs(target.x - .5 - center.getX()) <= 8
                                && Math.abs(target.z - .5 - center.getZ()) <= 3,
                        "RTS rectangle dimensions were lost when generating a shot target: " + target);
            }
        } finally { owned.discard(); unselected.discard(); foreign.discard(); guns.forEach(AbstractSiegeEntity::discard); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapClearOrderPreservesOtherEngineersZones(GameTestHelper helper) {
        var commander = commander(helper);
        var selected = engineer(helper, commander, 2);
        var retained = engineer(helper, commander, 3);
        var guns = List.of(artillery(helper, selected), artillery(helper, retained));
        try {
            RecruitsRTSCommandApi.areaFireOrdered(commander, List.of(selected.getUUID(), retained.getUUID()),
                    helper.absolutePos(new BlockPos(8, 1, 8)), 5, 5, FireZoneShape.CIRCLE);
            helper.assertTrue(RecruitsFireZone.hasZone(selected) && RecruitsFireZone.hasZone(retained), "RTS setup did not create both zones");
            RecruitsRTSCommandApi.areaFireCleared(commander, List.of(selected.getUUID()));
            helper.assertTrue(!RecruitsFireZone.hasZone(selected) && RecruitsFireZone.hasZone(retained),
                    "RTS clear ignored the selected member list");
        } finally { selected.discard(); retained.discard(); guns.forEach(AbstractSiegeEntity::discard); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mapObjectsExposeActionsAndRejectForeignCommands(GameTestHelper helper) {
        var commander = commander(helper);
        var mantlet = SiegeworksEntities.MANTLET_ENTITY.get().create(helper.getLevel());
        mantlet.setPos(commander.position());
        mantlet.claimOwnership(commander.getUUID());
        helper.getLevel().addFreshEntity(mantlet);
        var driver = engineer(helper, commander, 2);
        man(driver, mantlet);
        try {
            var object = RecruitsRTSCommandApi.collectObjects(commander).stream()
                    .filter(snapshot -> snapshot.id().equals(mantlet.getUUID())).findFirst().orElseThrow();
            var actions = RecruitsRTSCommandApi.withActions(commander, object, List.of()).actions();
            helper.assertTrue(actions.stream().anyMatch(action -> action.id().equals("siegeworks:flap_open")),
                    "RTS map object did not expose the mantlet action");
            RecruitsRTSCommandApi.performObjectAction(commander, mantlet.getUUID(), List.of(), "siegeworks:flap_open");
            helper.assertTrue(RecruitsCompat.flapOpen(mantlet), "RTS action did not open the mantlet");
            var foreign = SiegeGameTestPlayers.create(helper.getLevel());
            foreign.setPos(commander.position());
            RecruitsRTSCommandApi.performObjectAction(foreign, mantlet.getUUID(), List.of(), "siegeworks:flap_close");
            helper.assertTrue(RecruitsCompat.flapOpen(mantlet), "Foreign RTS order operated an owned machine");
        } finally { driver.discard(); mantlet.discard(); }
        helper.succeed();
    }

    private static ServerPlayer commander(GameTestHelper helper) {
        var player = SiegeGameTestPlayers.create(helper.getLevel());
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 1, 4)));
        return player;
    }

    private static AbstractSiegeEntity artillery(GameTestHelper helper, SiegeEngineerEntity unit) {
        var gun = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(helper.getLevel());
        gun.setPos(unit.position());
        gun.claimOwnership(unit.getOwnerUUID());
        helper.getLevel().addFreshEntity(gun);
        man(unit, gun);
        return gun;
    }

    private static void man(SiegeEngineerEntity unit, AbstractSiegeEntity machine) {
        unit.startRiding(machine, true);
        machine.setOperator(unit);
        unit.siegeController = new SiegeworksRecruitController(unit, machine);
    }

    private static SiegeEngineerEntity engineer(GameTestHelper helper, ServerPlayer owner, int x) {
        var unit = ModEntityTypes.SIEGE_ENGINEER.get().create(helper.getLevel());
        unit.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, 1, 4)));
        unit.setNoAi(true);
        unit.setOwnerUUID(Optional.of(owner.getUUID()));
        unit.setIsOwned(true);
        unit.setListen(true);
        unit.setGroupUUID(UUID.randomUUID());
        helper.getLevel().addFreshEntity(unit);
        return unit;
    }
}
