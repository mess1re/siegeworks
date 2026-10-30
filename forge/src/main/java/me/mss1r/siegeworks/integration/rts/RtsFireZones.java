package me.mss1r.siegeworks.integration.rts;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.recruitsrtscommand.api.FireZoneShape;
import me.mss1r.recruitsrtscommand.api.MapOrderListener;
import me.mss1r.recruitsrtscommand.api.RecruitsRTSCommandApi;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

final class RtsFireZones {
    private RtsFireZones() {
    }

    static void register() {
        RecruitsRTSCommandApi.registerOrderListener(new MapOrderListener() {
            @Override
            public void areaFireOrdered(ServerPlayer commander, List<UUID> members,
                                        MapOrderListener.MapFireZone zone) {
                if (members.isEmpty() || zone.center() == null) return;

                RecruitsCompat.applyFireZone(zone.center(),
                        Math.max(1, zone.radiusX()), Math.max(1, zone.radiusZ()),
                        zone.shape() == FireZoneShape.RECTANGLE, named(commander, members));
            }

            @Override
            public void areaFireCleared(ServerPlayer commander, List<UUID> members) {
                if (members.isEmpty()) return;

                RecruitsCompat.clearFireZone(named(commander, members));
            }
        });
    }

    /** Recruits named by a map order, at any distance: the map commands them from anywhere. */
    private static List<AbstractRecruitEntity> named(ServerPlayer commander, List<UUID> members) {
        List<AbstractRecruitEntity> recruits = new ArrayList<>(members.size());
        for (UUID id : new LinkedHashSet<>(members)) {
            if (commander.serverLevel().getEntity(id) instanceof AbstractRecruitEntity recruit
                    && RecruitsCompat.commandable(commander, recruit)) {
                recruits.add(recruit);
            }
        }
        return recruits;
    }
}
