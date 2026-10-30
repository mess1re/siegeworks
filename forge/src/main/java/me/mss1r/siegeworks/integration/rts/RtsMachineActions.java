package me.mss1r.siegeworks.integration.rts;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeCaptureController;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsSiegeCommandC2SPayload;
import me.mss1r.siegeworks.integration.recruits.network.RecruitsTowerCrewC2SPayload;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import me.mss1r.siegeworks.integration.recruits.RecruitsOrderChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

final class RtsMachineActions {
    static final String REPAIR = "siegeworks:repair";
    static final String DISMANTLE = "siegeworks:dismantle";
    static final String CREW = "siegeworks:crew_tower";
    static final String UNLOAD = "siegeworks:unload_tower";
    static final String RETURN = "siegeworks:return_tower";
    static final String LEAVE = "siegeworks:leave_machine";
    static final String DRIVE = "siegeworks:drive_machine";
    static final String DRIVE_APPEND = "siegeworks:drive_machine_append";
    static final String BRIDGE_DOWN = "siegeworks:bridge_down";
    static final String BRIDGE_UP = "siegeworks:bridge_up";
    static final String BRIDGE_AUTO = "siegeworks:bridge_auto";
    static final String PICKUP_LADDER = "siegeworks:pickup_ladder";
    static final String HOLD_FIRE = "siegeworks:hold_fire";
    static final String FIRE_AT_WILL = "siegeworks:fire_at_will";
    static final String CANCEL_WORK = "siegeworks:cancel_work";
    static final String HALT = "siegeworks:halt_machine";
    private static final String OUT_OF_RANGE = "siegeworks:out_of_range";
    private static final String FOREIGN = "siegeworks:foreign";
    private static final String GROUP_FIRE = "siegeworks:fire";
    private static final String GROUP_AMMO = "siegeworks:ammo";
    private static final String GROUP_BRIDGE = "siegeworks:bridge";
    private static final String GROUP_FLAP = "siegeworks:flap";
    static final String FLAP_OPEN = "siegeworks:flap_open";
    static final String FLAP_CLOSE = "siegeworks:flap_close";
    private static final String AMMO_PREFIX = "siegeworks:ammo_";

    private RtsMachineActions() {
    }

    static List<MapObjectAction> offer(ServerPlayer commander, AbstractSiegeEntity siege,
                                       List<UUID> members) {
        if (!RecruitsCompat.withinCommandRange(commander, siege)) {
            return List.of(MapObjectAction.blocked(OUT_OF_RANGE,
                    Component.translatable("gui.siegeworks.rts.action.out_of_range"), null,
                    Component.translatable("gui.siegeworks.rts.action.out_of_range.reason")));
        }
        if (!SiegeAccess.allows(commander, siege, SiegeAccess.Action.USE)) {
            return foreign(commander, siege, members);
        }

        List<AbstractRecruitEntity> nearby = RecruitsCompat.commandRangeRecruits(commander);
        List<MapObjectAction> actions = new ArrayList<>(6);
        if (siege instanceof SiegeTowerEntity tower) {
            actions.addAll(towerCrew(commander, tower, members));
        }

        if (RecruitsCompat.takesCrew(siege) && !(siege instanceof SiegeTowerEntity)) {
            actions.add(row(CREW, "crew", !members.isEmpty(),
                    Component.translatable("gui.siegeworks.rts.action.no_men")));
        }

        if (siege instanceof MantletEntity) {
            Component flapping = RecruitsOrderChecks.flap(commander, siege);
            boolean open = RecruitsCompat.flapOpen(siege);
            actions.add(option(GROUP_FLAP, FLAP_OPEN, "flap_open", open, flapping == null, flapping));
            actions.add(option(GROUP_FLAP, FLAP_CLOSE, "flap_close", !open, flapping == null, flapping));
        }

        if (siege instanceof SiegeLadderEntity ladder) {
            actions.add(row(PICKUP_LADDER, "pickup_ladder",
                    RecruitsCompat.assignLadderPickup(commander, ladder, chosen(commander, members), true),
                    Component.translatable("gui.siegeworks.rts.action.nobody_to_carry")));
        }

        int reached = RecruitsCompat.leaveMachine(commander, siege, true);
        if (reached > 0) {
            int aboard = RecruitsCompat.aboardAny(commander, siege);
            String key = aboard > 0 ? "leave" : "recall";
            actions.add(MapObjectAction.of(LEAVE,
                    Component.translatable("gui.siegeworks.rts.action." + key),
                    Component.translatable("gui.siegeworks.rts.action." + key + ".hint")));
        }

        boolean shoots = RecruitsCompat.shoots(siege);
        if (shoots || RecruitsCompat.rams(siege)) {
            Component noLayer = Component.translatable("gui.siegeworks.rts.action.no_layer");
            Boolean firing = RecruitsCompat.firesAtWill(commander, siege);
            boolean manned = firing != null;
            actions.add(option(GROUP_FIRE, FIRE_AT_WILL, shoots ? "fire_at_will" : "attack_at_will",
                    Boolean.TRUE.equals(firing), manned, noLayer));
            actions.add(option(GROUP_FIRE, HOLD_FIRE, shoots ? "hold_fire" : "hold_attack",
                    Boolean.FALSE.equals(firing), manned, noLayer));

            SiegeAmmunitionMode loaded = RecruitsCompat.ammunitionSetting(siege);
            for (SiegeAmmunitionMode mode : SiegeAmmunitionMode.values()) {
                if (RecruitsCompat.choosesAmmunition(siege, mode)) {
                    String key = "ammo_" + mode.name().toLowerCase(java.util.Locale.ROOT);
                    actions.add(option(GROUP_AMMO, AMMO_PREFIX + mode.name().toLowerCase(java.util.Locale.ROOT),
                            key, mode == loaded, manned, noLayer));
                }
            }
        }

        Component repairLabel = Component.translatable("gui.siegeworks.rts.action.repair");
        Component repairHint = Component.translatable("gui.siegeworks.rts.action.repair.hint");
        if (siege.getHealth() >= siege.getMaxHealth()) {
            actions.add(MapObjectAction.blocked(REPAIR, repairLabel, repairHint,
                    Component.translatable("gui.siegeworks.rts.action.undamaged")));
        } else {
            actions.add(decide(REPAIR, repairLabel, repairHint, commander, siege, members, false, nearby));
        }

        actions.add(decide(DISMANTLE,
                Component.translatable("gui.siegeworks.rts.action.dismantle"),
                Component.translatable("gui.siegeworks.rts.action.dismantle.hint"),
                commander, siege, members, true, nearby));

        if (RecruitsCompat.cancelWorkOn(commander, siege, true) > 0) {
            actions.add(row(CANCEL_WORK, "cancel_work", true, Component.empty()));
        }
        return actions;
    }

    private static List<MapObjectAction> towerCrew(ServerPlayer commander, SiegeTowerEntity tower,
                                                   List<UUID> members) {
        boolean anyone = !members.isEmpty();
        Component noMen = Component.translatable("gui.siegeworks.rts.action.no_men");
        Predicate<AbstractRecruitEntity> ours = recruit -> RecruitsCompat.commandable(commander, recruit);
        Component returning = RecruitsOrderChecks.returnToTower(commander, tower, ours);
        Component unloading = RecruitsOrderChecks.unload(commander, tower, ours);
        Component bridging = RecruitsOrderChecks.bridge(commander, tower);
        Boolean bridge = RecruitsCompat.towerBridgeSetting(commander, tower);
        return List.of(
                row(CREW, "crew", anyone, noMen),
                row(RETURN, "return", returning == null, returning),
                row(UNLOAD, "unload", unloading == null, unloading),
                option(GROUP_BRIDGE, BRIDGE_DOWN, "bridge_down",
                        Boolean.TRUE.equals(bridge), bridging == null, bridging),
                option(GROUP_BRIDGE, BRIDGE_UP, "bridge_up",
                        Boolean.FALSE.equals(bridge), bridging == null, bridging),
                option(GROUP_BRIDGE, BRIDGE_AUTO, "bridge_auto",
                        bridge == null, bridging == null, bridging));
    }

    private static MapObjectAction option(String group, String id, String key, boolean selected,
                                          boolean enabled, Component reason) {
        Component label = Component.translatable("gui.siegeworks.rts.action." + key);
        Component hint = Component.translatable("gui.siegeworks.rts.action." + key + ".hint");
        return enabled
                ? MapObjectAction.option(group, id, label, hint, selected)
                : MapObjectAction.blockedOption(group, id, label, hint, selected, reason);
    }

    private static List<MapObjectAction> foreign(ServerPlayer commander, AbstractSiegeEntity siege, List<UUID> members) {
        List<MapObjectAction> actions = new ArrayList<>(2);
        SiegeCaptureController.Refusal refusal = siege.captureRefusal(commander);
        if (refusal == null && RecruitsCompat.takesCrew(siege) && !(siege instanceof SiegeTowerEntity)) {
            actions.add(row(CREW, "capture", !members.isEmpty(),
                    Component.translatable("gui.siegeworks.rts.action.no_men")));
        } else {
            actions.add(MapObjectAction.blocked(FOREIGN,
                    Component.translatable("gui.siegeworks.rts.action.foreign"), null,
                    refusal == null ? SiegeCaptureController.Refusal.DISABLED.message() : refusal.message()));
        }
        if (RecruitsCompat.leaveMachine(commander, siege, true) > 0) {
            actions.add(MapObjectAction.of(LEAVE,
                    Component.translatable("gui.siegeworks.rts.action.leave"),
                    Component.translatable("gui.siegeworks.rts.action.leave.hint")));
        }
        return actions;
    }

    private static MapObjectAction row(String id, String key, boolean enabled, Component reason) {
        Component label = Component.translatable("gui.siegeworks.rts.action." + key);
        Component hint = Component.translatable("gui.siegeworks.rts.action." + key + ".hint");
        return enabled
                ? MapObjectAction.of(id, label, hint)
                : MapObjectAction.blocked(id, label, hint, reason);
    }

    private static MapObjectAction decide(String id, Component label, Component hint,
                                          ServerPlayer commander, AbstractSiegeEntity siege,
                                          List<UUID> members, boolean dismantle,
                                          List<AbstractRecruitEntity> nearby) {
        RecruitsCompat.MaintenanceRefusal refusal = RecruitsCompat.assignMaintenance(
                commander, siege, dismantle, chosen(commander, members), true, nearby);
        return refusal == RecruitsCompat.MaintenanceRefusal.NONE
                ? MapObjectAction.of(id, label, hint)
                : MapObjectAction.blocked(id, label, hint, refusal.message());
    }

    static void perform(ServerPlayer commander, AbstractSiegeEntity siege, List<UUID> members,
                        String actionId, BlockPos target) {
        if (!RecruitsCompat.withinCommandRange(commander, siege)) {
            commander.displayClientMessage(
                    Component.translatable("gui.siegeworks.rts.action.out_of_range.reason"), true);
            return;
        }
        if (DRIVE.equals(actionId) || DRIVE_APPEND.equals(actionId)) {
            if (!SiegeAccess.allows(commander, siege, SiegeAccess.Action.USE)) {
                commander.displayClientMessage(Component.translatable("message.siegeworks.access.denied"), true);
                return;
            }
            boolean sent = target != null && (DRIVE_APPEND.equals(actionId)
                    ? RecruitsCompat.queueDrive(commander, siege, target, false)
                    : RecruitsCompat.driveMachine(commander, siege, target, false));
            if (!sent) {
                commander.displayClientMessage(
                        Component.translatable("gui.siegeworks.rts.action.no_engineer"), true);
            }
            return;
        }
        perform(commander, siege, members, actionId);
    }

    static void perform(ServerPlayer commander, AbstractSiegeEntity siege, List<UUID> members,
                        String actionId) {
        if (!RecruitsCompat.withinCommandRange(commander, siege)) {
            commander.displayClientMessage(
                    Component.translatable("gui.siegeworks.rts.action.out_of_range.reason"), true);
            return;
        }
        if (CREW.equals(actionId)) {
            int placed = RecruitsCompat.crewMachine(commander, siege, chosen(commander, members));
            commander.displayClientMessage(Component.translatable(
                    "message.siegeworks.rts.crewed", placed, members.size()), true);
            return;
        }
        if (LEAVE.equals(actionId)) {
            int left = RecruitsCompat.leaveMachine(commander, siege, false);
            commander.displayClientMessage(
                    Component.translatable("message.siegeworks.rts.left", left), true);
            return;
        }
        if (!SiegeAccess.allows(commander, siege, SiegeAccess.Action.USE)) {
            commander.displayClientMessage(Component.translatable("message.siegeworks.access.denied"), true);
            return;
        }

        if (HOLD_FIRE.equals(actionId) || FIRE_AT_WILL.equals(actionId)) {
            int order = HOLD_FIRE.equals(actionId)
                    ? RecruitsSiegeCommandC2SPayload.ACTION_HOLD_FIRE
                    : RecruitsSiegeCommandC2SPayload.ACTION_FIRE_AT_WILL;
            if (!RecruitsCompat.applyFireOrder(commander, siege, order, null, false)) {
                commander.displayClientMessage(
                        Component.translatable("gui.siegeworks.rts.action.no_layer"), true);
            }
            return;
        }

        if (actionId.startsWith(AMMO_PREFIX)) {
            SiegeAmmunitionMode mode = ammunitionMode(actionId);
            if (mode == null || !RecruitsCompat.applyAmmunition(commander, siege, mode, false)) {
                commander.displayClientMessage(
                        Component.translatable("gui.siegeworks.rts.action.no_layer"), true);
            }
            return;
        }

        if (FLAP_OPEN.equals(actionId) || FLAP_CLOSE.equals(actionId)) {
            Component flapping = RecruitsOrderChecks.flap(commander, siege);
            if (flapping != null) {
                commander.displayClientMessage(flapping, true);
            } else {
                RecruitsCompat.setMantletFlap(commander, siege, FLAP_OPEN.equals(actionId), false);
            }
            return;
        }

        if (CANCEL_WORK.equals(actionId)) {
            RecruitsCompat.cancelWorkOn(commander, siege, false);
            return;
        }

        if (HALT.equals(actionId)) {
            RecruitsCompat.haltMachine(commander, siege, false);
            return;
        }

        if (PICKUP_LADDER.equals(actionId)) {
            if (!(siege instanceof SiegeLadderEntity ladder)
                    || !RecruitsCompat.assignLadderPickup(commander, ladder,
                            chosen(commander, members), false)) {
                commander.displayClientMessage(
                        Component.translatable("gui.siegeworks.rts.action.nobody_to_carry"), true);
            }
            return;
        }

        if (BRIDGE_DOWN.equals(actionId) || BRIDGE_UP.equals(actionId) || BRIDGE_AUTO.equals(actionId)) {
            Boolean override = BRIDGE_DOWN.equals(actionId)
                    ? Boolean.TRUE
                    : BRIDGE_UP.equals(actionId) ? Boolean.FALSE : null;
            if (!(siege instanceof SiegeTowerEntity tower)
                    || !RecruitsCompat.setTowerBridge(commander, tower, override, false)) {
                commander.displayClientMessage(
                        Component.translatable("gui.siegeworks.rts.action.no_driver"), true);
            }
            return;
        }

        Integer crewAction = switch (actionId) {
            case UNLOAD -> RecruitsTowerCrewC2SPayload.ACTION_UNLOAD;
            case RETURN -> RecruitsTowerCrewC2SPayload.ACTION_RETURN;
            default -> null;
        };
        if (crewAction != null) {
            if (siege instanceof SiegeTowerEntity tower) {
                RecruitsCompat.applyTowerCrew(commander, tower, crewAction,
                        recruit -> RecruitsCompat.commandable(commander, recruit));
            }
            return;
        }

        boolean dismantle = DISMANTLE.equals(actionId);
        if (!dismantle && !REPAIR.equals(actionId)) return;

        RecruitsCompat.MaintenanceRefusal refusal = RecruitsCompat.assignMaintenance(
                commander, siege, dismantle, chosen(commander, members), false, null);
        if (refusal != RecruitsCompat.MaintenanceRefusal.NONE) {
            commander.displayClientMessage(refusal.message(), true);
        }
    }

    private static SiegeAmmunitionMode ammunitionMode(String actionId) {
        String name = actionId.substring(AMMO_PREFIX.length());
        for (SiegeAmmunitionMode mode : SiegeAmmunitionMode.values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }

    private static Predicate<AbstractRecruitEntity> chosen(ServerPlayer commander, List<UUID> members) {
        Set<UUID> named = new HashSet<>(members);
        return recruit -> named.contains(recruit.getUUID())
                && RecruitsCompat.commandable(commander, recruit);
    }

    static AbstractSiegeEntity machine(ServerPlayer commander, UUID objectId) {
        Entity entity = commander.serverLevel().getEntity(objectId);
        return entity instanceof AbstractSiegeEntity siege && siege.isAlive() ? siege : null;
    }
}
