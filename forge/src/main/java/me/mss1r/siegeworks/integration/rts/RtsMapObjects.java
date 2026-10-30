package me.mss1r.siegeworks.integration.rts;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.recruitsrtscommand.api.MapIcon;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectProvider;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.recruitsrtscommand.api.MapOrder;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeRelation;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import me.mss1r.siegeworks.integration.recruits.RecruitsOrderChecks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RtsMapObjects {
    private static final double COLLECT_RADIUS = 512.0D;
    private static final double SCOUTING_RANGE_SQR = 64.0D * 64.0D;

    private static final float MACHINE_ICON_SCALE = 1.5F;

    private static final int PIP_UNMANNED = 0xFF8A8F98;
    private static final int PIP_WORKING = 0xFFE0C04A;

    private RtsMapObjects() {
    }

    static void register() {
        RecruitsRTSCommandApi.registerObjectProvider(new MapObjectProvider() {
            @Override
            public List<MapObjectSnapshot> collect(ServerPlayer player) {
                List<MapObjectSnapshot> objects = new ArrayList<>(RtsMapObjects.collect(player));
                objects.addAll(RtsLadderCarriers.collect(player));
                return objects;
            }

            @Override
            public List<MapObjectAction> actionsFor(ServerPlayer commander, UUID objectId,
                                                    List<UUID> members) {
                AbstractSiegeEntity siege = RtsMachineActions.machine(commander, objectId);
                if (siege != null) {
                    return RtsMachineActions.offer(commander, siege, members);
                }
                return RtsLadderCarriers.carrier(commander, objectId) == null
                        ? List.of()
                        : RtsLadderCarriers.offer();
            }

            @Override
            public void performAt(ServerPlayer commander, UUID objectId, List<UUID> members,
                                 String actionId, net.minecraft.core.BlockPos target) {
                AbstractSiegeEntity siege = RtsMachineActions.machine(commander, objectId);
                if (siege != null) {
                    RtsMachineActions.perform(commander, siege, members, actionId, target);
                    return;
                }
                AbstractRecruitEntity carrier = RtsLadderCarriers.carrier(commander, objectId);
                if (carrier != null) RtsLadderCarriers.perform(commander, carrier, actionId, target);
            }

            @Override
            public void perform(ServerPlayer commander, UUID objectId, List<UUID> members,
                                String actionId) {
                AbstractSiegeEntity siege = RtsMachineActions.machine(commander, objectId);
                if (siege != null) RtsMachineActions.perform(commander, siege, members, actionId);
            }
        });
    }

    private static List<MapObjectSnapshot> collect(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB around = player.getBoundingBox().inflate(COLLECT_RADIUS);

        List<MapObjectSnapshot> objects = new ArrayList<>();
        List<AbstractRecruitEntity> scouts = null;
        for (AbstractSiegeEntity siege : level.getEntitiesOfClass(AbstractSiegeEntity.class, around,
                AbstractSiegeEntity::isAlive)) {
            if (!ours(player, siege)) {
                if (scouts == null) {
                    scouts = level.getEntitiesOfClass(AbstractRecruitEntity.class, around,
                            recruit -> player.getUUID().equals(recruit.getOwnerUUID()));
                }
                if (!scouted(player, siege, scouts)) {
                    continue;
                }
            }
            objects.add(describe(player, siege));
        }
        return objects;
    }

    private static boolean ours(ServerPlayer player, AbstractSiegeEntity siege) {
        return RecruitsCompat.commanderOf(siege) != null
                && SiegeAccess.relationOf(player.getUUID(), siege) != SiegeRelation.HOSTILE;
    }

    /** Other teams' engines are only shown where the player or one of their recruits can see them. */
    private static boolean scouted(ServerPlayer player, AbstractSiegeEntity siege,
                                   List<AbstractRecruitEntity> scouts) {
        if (player.distanceToSqr(siege) <= SCOUTING_RANGE_SQR) {
            return true;
        }
        for (AbstractRecruitEntity scout : scouts) {
            if (scout.distanceToSqr(siege) <= SCOUTING_RANGE_SQR) {
                return true;
            }
        }
        return false;
    }

    private static MapIcon iconFor(AbstractSiegeEntity siege) {
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(siege.getType());
        if (key == null) return MapIcon.item(fallbackItem(), MACHINE_ICON_SCALE);

        ResourceLocation spawner = MinecraftVersionCompat.id(
                key.getNamespace(), key.getPath() + "_spawner");
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(spawner)
                ? MapIcon.item(spawner, MACHINE_ICON_SCALE)
                : MapIcon.item(fallbackItem(), MACHINE_ICON_SCALE);
    }

    private static ResourceLocation fallbackItem() {
        return MinecraftVersionCompat.id("siegeworks", "onager_spawner");
    }

    private static MapObjectSnapshot describe(ServerPlayer player, AbstractSiegeEntity siege) {
        UUID ownerId = RecruitsCompat.commanderOf(siege);
        boolean ours = ours(player, siege);
        LivingEntity crew = siege.getControllingPassenger();

        List<Component> lines = new ArrayList<>(4);
        lines.add(siege.getType().getDescription());
        if (ours) {
            lines.add(crewLine(crew));
            Component carried = carriedLine(siege);
            if (carried != null) lines.add(carried);
            lines.add(stateLine(siege));
            Component stuck = RecruitsCompat.drivable(siege) ? RecruitsOrderChecks.drive(player, siege) : null;
            if (stuck != null) {
                lines.add(Component.translatable("gui.siegeworks.rts.map.cannot_drive", stuck));
            }
        }

        return new MapObjectSnapshot(
                siege.getUUID(),
                iconFor(siege),
                ownerId,
                BlockPos.containing(siege.position()),
                siege.level().dimension().location(),
                ours ? conditionOf(siege) : MapObjectSnapshot.CONDITION_UNKNOWN,
                pipFor(siege, crew, ours),
                lines,
                List.of(),
                ours ? RecruitsCompat.routeOf(siege, player.getUUID()) : List.of(),
                ours && crew instanceof AbstractRecruitEntity recruit ? List.of(recruit.getUUID()) : List.of(),
                ours ? mapOrders(siege) : java.util.Map.of());
    }

    private static java.util.Map<MapOrder, String> mapOrders(AbstractSiegeEntity siege) {
        java.util.Map<MapOrder, String> orders = new java.util.EnumMap<>(MapOrder.class);
        if (RecruitsCompat.drivable(siege)) {
            orders.put(MapOrder.MOVE, RtsMachineActions.DRIVE);
            orders.put(MapOrder.MOVE_APPEND, RtsMachineActions.DRIVE_APPEND);
        }
        orders.put(MapOrder.HALT, RtsMachineActions.HALT);
        return orders;
    }

    private static int pipFor(AbstractSiegeEntity siege, LivingEntity crew, boolean ours) {
        if (!ours) return MapObjectSnapshot.NO_PIP;
        if (crew == null) return PIP_UNMANNED;
        return siege.getOperationState() == SiegeOperationState.READY
                ? MapObjectSnapshot.NO_PIP
                : PIP_WORKING;
    }

    private static Component carriedLine(AbstractSiegeEntity siege) {
        if (!(siege instanceof me.mss1r.siegeworks.entity.siege.SiegeTowerEntity tower)) {
            return null;
        }

        int inside = 0;
        int pushing = 0;
        for (Entity passenger : tower.getPassengers()) {
            if (tower.isInteriorPassenger(passenger)) {
                inside++;
            } else if (tower.isPusher(passenger)) {
                pushing++;
            }
        }
        return inside == 0 && pushing == 0
                ? null
                : Component.translatable("gui.siegeworks.rts.map.carried", inside, pushing);
    }

    private static Component crewLine(LivingEntity crew) {
        return crew == null
                ? Component.translatable("gui.siegeworks.rts.map.unmanned")
                : Component.translatable("gui.siegeworks.rts.map.crewed", crew.getDisplayName());
    }

    private static Component stateLine(AbstractSiegeEntity siege) {
        SiegeOperationState state = siege.getOperationState();
        return Component.translatable("gui.siegeworks.rts.map.state."
                + state.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static int conditionOf(AbstractSiegeEntity siege) {
        float max = siege.getMaxHealth();
        if (max <= 0.0F) return 100;
        return Math.max(0, Math.min(100, Math.round(siege.getHealth() / max * 100.0F)));
    }

}
