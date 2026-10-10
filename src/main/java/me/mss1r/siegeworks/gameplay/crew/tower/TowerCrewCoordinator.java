package me.mss1r.siegeworks.gameplay.crew.tower;

import me.mss1r.siegeworks.gameplay.crew.tower.TowerCrewRoster.Seat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class TowerCrewCoordinator {
    private final Host host;
    private final TowerCrewRoster roster;

    public TowerCrewCoordinator(Host host, TowerCrewRoster roster) {
        this.host = host;
        this.roster = roster;
    }

    public boolean canAddOperator(Entity entity, UUID pendingDriver, UUID pendingPusher) {
        UUID uuid = entity.getUUID();
        if (roster.hasPusher(uuid) || (!(entity instanceof Player) && roster.hasSeat(uuid))) {
            return true;
        }
        if (uuid.equals(pendingDriver)) {
            return roster.driverSlotAvailable(uuid);
        }
        if (uuid.equals(pendingPusher)) {
            return roster.firstFreePusherSlot() >= 0;
        }
        if (entity instanceof Player || !(entity instanceof LivingEntity)) {
            return false;
        }
        if (host.supportedDirectOperator(entity) && roster.driverSlotAvailable(uuid)) {
            return true;
        }
        return roster.firstFreePusherSlot() >= 0 || roster.firstFreeInteriorSeat() != null;
    }

    public void passengerAdded(Entity passenger) {
        if (host.clientSide() || passenger instanceof Player || host.draftMount(passenger)
                || !(passenger instanceof LivingEntity living)) {
            return;
        }

        UUID uuid = passenger.getUUID();
        if (roster.hasPusher(uuid) || roster.hasSeat(uuid)) {
            roster.synchronize();
            return;
        }
        if (host.supportedDirectOperator(passenger) && roster.driverSlotAvailable(uuid)) {
            roster.assignDriver(uuid);
            host.setOperator(living);
            return;
        }

        int pusherSlot = roster.firstFreePusherSlot();
        if (pusherSlot >= 0) {
            roster.assignPusher(uuid, pusherSlot);
            return;
        }

        Seat seat = roster.firstFreeInteriorSeat();
        if (seat != null) {
            roster.assignSeat(uuid, seat.floor(), seat.slot());
        }
    }

    public boolean reserveDriver(LivingEntity passenger) {
        if (!host.pushBarsManned() || !host.supportedDirectOperator(passenger)
                || !roster.driverSlotAvailable(passenger.getUUID())) {
            return false;
        }
        roster.assignDriver(passenger.getUUID());
        host.setOperator(passenger);
        return true;
    }

    public boolean reservePusher(LivingEntity passenger, Entity tower) {
        if (!host.pushBarsManned()) {
            return false;
        }
        if (passenger.getVehicle() == tower && isPusher(passenger)) {
            return true;
        }
        if (isDriver(passenger)) {
            return false;
        }
        UUID uuid = passenger.getUUID();
        if (roster.hasPusher(uuid)) {
            return true;
        }
        int slot = roster.firstFreePusherSlot();
        if (slot < 0) {
            return false;
        }
        roster.assignPusher(uuid, slot);
        return true;
    }

    public boolean reserveInteriorSeat(LivingEntity passenger, Entity tower) {
        if (passenger instanceof Player) {
            return false;
        }
        if (passenger.getVehicle() == tower && isInteriorPassenger(passenger)) {
            return true;
        }
        if (isDriver(passenger)) {
            return false;
        }
        UUID uuid = passenger.getUUID();
        if (roster.hasSeat(uuid)) {
            return true;
        }
        Seat seat = roster.firstFreeInteriorSeat();
        if (seat == null) {
            return false;
        }
        roster.assignSeat(uuid, seat.floor(), seat.slot());
        return true;
    }

    public void cancelReservation(Entity passenger, Entity tower) {
        if (passenger.getVehicle() != tower) {
            roster.clearReservation(passenger.getUUID());
        }
    }

    public boolean prepareForSecondFloorExit(LivingEntity passenger, Entity tower) {
        if (passenger.getVehicle() != tower || isDriver(passenger)) {
            return false;
        }

        Seat current = roster.serverSeat(passenger);
        if (current == null) {
            return false;
        }
        if (current.floor() == TowerPassengerLayout.FLOOR_TWO) {
            return true;
        }

        int slot = roster.firstFreeSlot(TowerPassengerLayout.FLOOR_TWO);
        if (slot < 0) {
            return false;
        }
        roster.assignSeat(passenger.getUUID(), TowerPassengerLayout.FLOOR_TWO, slot);
        return true;
    }

    public boolean prepareForGroundExit(LivingEntity passenger, Entity tower) {
        if (passenger.getVehicle() != tower) {
            return false;
        }

        Seat current = roster.serverSeat(passenger);
        if (current == null) {
            return true;
        }
        if (current.floor() == TowerPassengerLayout.FLOOR_ONE) {
            return true;
        }

        for (int floor = current.floor() - 1; floor >= TowerPassengerLayout.FLOOR_ONE; floor--) {
            int slot = roster.firstFreeSlot(floor);
            if (slot >= 0) {
                roster.assignSeat(passenger.getUUID(), floor, slot);
                return floor == TowerPassengerLayout.FLOOR_ONE;
            }
        }
        return false;
    }

    public void passengerRemoved(Entity passenger) {
        UUID uuid = passenger.getUUID();
        if (isDriver(passenger)) {
            roster.clearDriver();
        }
        if (roster.hasPusher(uuid)) {
            roster.removePusher(uuid);
        }
        if (!host.clientSide() && roster.serverSeat(uuid) != null) {
            roster.removeSeat(uuid);
        }
    }

    public boolean isDriver(Entity entity) {
        return roster.isDriver(entity);
    }

    public boolean isPusher(Entity entity) {
        return roster.pusherSlot(entity) != null;
    }

    public boolean isInteriorPassenger(Entity entity) {
        return !(entity instanceof Player) && roster.seat(entity) != null;
    }

    public Seat seat(Entity entity) {
        return roster.seat(entity);
    }

    public Seat serverSeat(Entity entity) {
        return roster.serverSeat(entity);
    }

    public Integer pusherSlot(Entity entity) {
        return roster.pusherSlot(entity);
    }

    public Entity driverPassenger() {
        return roster.driverPassenger();
    }

    public int capacity() {
        return roster.capacity();
    }

    public interface Host {
        boolean clientSide();

        boolean draftMount(Entity entity);

        default boolean pushBarsManned() {
            return true;
        }

        boolean supportedDirectOperator(Entity entity);

        void setOperator(LivingEntity operator);
    }
}
