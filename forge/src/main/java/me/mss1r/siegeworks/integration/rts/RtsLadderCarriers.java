package me.mss1r.siegeworks.integration.rts;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import me.mss1r.recruitsrtscommand.api.MapIcon;
import me.mss1r.recruitsrtscommand.api.MapObjectAction;
import me.mss1r.recruitsrtscommand.api.MapObjectSnapshot;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RtsLadderCarriers {
    static final String PLACE = "siegeworks:place_ladder";

    private static final double COLLECT_RADIUS = 512.0D;

    private static final float ICON_SCALE = 1.0F;

    private RtsLadderCarriers() {
    }

    static List<MapObjectSnapshot> collect(ServerPlayer player) {
        List<AbstractRecruitEntity> carriers = RecruitsCompat.ladderCarriers(player, COLLECT_RADIUS);
        if (carriers.isEmpty()) {
            return List.of();
        }

        ResourceLocation dimension = player.serverLevel().dimension().location();
        List<MapObjectSnapshot> objects = new ArrayList<>(carriers.size());
        for (AbstractRecruitEntity carrier : carriers) {
            objects.add(new MapObjectSnapshot(
                    carrier.getUUID(),
                    MapIcon.item(MinecraftVersionCompat.id("siegeworks", "siege_ladder_spawner"),
                            ICON_SCALE),
                    player.getUUID(),
                    carrier.blockPosition(),
                    dimension,
                    MapObjectSnapshot.CONDITION_UNKNOWN,
                    MapObjectSnapshot.NO_PIP,
                    List.of(Component.translatable("gui.siegeworks.rts.map.ladder_carried"),
                            carrier.getDisplayName()),
                    List.of()));
        }
        return objects;
    }

    static AbstractRecruitEntity carrier(ServerPlayer commander, UUID objectId) {
        Entity entity = commander.serverLevel().getEntity(objectId);
        return entity instanceof AbstractRecruitEntity recruit
                && recruit.isAlive()
                && RecruitsCompat.commandable(commander, recruit)
                && RecruitsCompat.carriesLadder(recruit)
                ? recruit
                : null;
    }

    static List<MapObjectAction> offer() {
        return List.of(MapObjectAction.at(PLACE,
                Component.translatable("gui.siegeworks.rts.action.place_ladder"),
                Component.translatable("gui.siegeworks.rts.action.place_ladder.hint")));
    }

    static void perform(ServerPlayer commander, AbstractRecruitEntity carrier, String actionId,
                        BlockPos target) {
        if (!PLACE.equals(actionId)) {
            return;
        }
        if (target == null) {
            commander.displayClientMessage(
                    Component.translatable("message.siegeworks.recruits.ladder_invalid_location"), true);
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(
                -(target.getX() + 0.5D - carrier.getX()),
                target.getZ() + 0.5D - carrier.getZ()));
        RecruitsCompat.assignLadderPlacement(commander, target, yaw,
                candidate -> candidate == carrier, false);
    }
}
