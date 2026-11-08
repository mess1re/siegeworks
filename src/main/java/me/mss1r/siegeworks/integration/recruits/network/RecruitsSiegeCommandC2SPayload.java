package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RecruitsSiegeCommandC2SPayload(int action, List<UUID> groupIds, BlockPos targetPos,
                                             int targetEntityId, ResourceLocation siegeTypeId) {
    public static final int ACTION_BRIDGE_LOWER = 0;
    public static final int ACTION_BRIDGE_RAISE = 1;
    public static final int ACTION_BRIDGE_AUTO = 2;
    public static final int ACTION_LEAVE_ENGINE = 3;
    public static final int ACTION_UNLOAD_TOWER = 4;
    public static final int ACTION_FIRE_POSITION = 5;
    public static final int ACTION_HOLD_FIRE = 6;
    public static final int ACTION_FIRE_AT_WILL = 7;
    public static final int ACTION_SET_SUPPLIES = 8;
    public static final int ACTION_BUILD = 9;
    public static final int ACTION_AMMO_AUTO = 10;
    public static final int ACTION_AMMO_STANDARD = 11;
    public static final int ACTION_AMMO_EXPLOSIVE = 12;
    public static final int ACTION_AMMO_INCENDIARY = 13;
    public static final int ACTION_REPAIR = 14;
    public static final int ACTION_DISMANTLE = 15;
    public static final int ACTION_CANCEL_MAINTENANCE = 16;
    public static final int ACTION_PICKUP_LADDER = 17;
    public static final int ACTION_PLACE_LADDER = 18;
    public static final int ACTION_FLAP_OPEN = 19;
    public static final int ACTION_FLAP_CLOSE = 20;
    public static final int ACTION_CREW_MACHINE = 21;
    public static final int ACTION_RETURN_TOWER = 22;
    public static final int ACTION_RETURN_TO_MOUNT = 23;
    public static final int NO_TARGET_ENTITY = -1;
    private static final int MAX_GROUPS = 64;

    public RecruitsSiegeCommandC2SPayload {
        groupIds = List.copyOf(groupIds);
    }

    public RecruitsSiegeCommandC2SPayload(int action, List<UUID> groupIds) {
        this(action, groupIds, null, NO_TARGET_ENTITY, null);
    }

    public RecruitsSiegeCommandC2SPayload(int action, List<UUID> groupIds, BlockPos targetPos) {
        this(action, groupIds, targetPos, NO_TARGET_ENTITY, null);
    }

    public RecruitsSiegeCommandC2SPayload(int action, List<UUID> groupIds, BlockPos targetPos,
                                         ResourceLocation siegeTypeId) {
        this(action, groupIds, targetPos, NO_TARGET_ENTITY, siegeTypeId);
    }

    public static void encode(RecruitsSiegeCommandC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.groupIds.size());
        packet.groupIds.forEach(buffer::writeUUID);
        buffer.writeBoolean(packet.targetPos != null);
        if (packet.targetPos != null) {
            buffer.writeBlockPos(packet.targetPos);
        }
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeBoolean(packet.siegeTypeId != null);
        if (packet.siegeTypeId != null) {
            buffer.writeResourceLocation(packet.siegeTypeId);
        }
    }

    public static RecruitsSiegeCommandC2SPayload decode(FriendlyByteBuf buffer) {
        int action = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_GROUPS) {
            throw new IllegalArgumentException("Invalid Recruits group count: " + count);
        }
        List<UUID> groupIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            groupIds.add(buffer.readUUID());
        }
        BlockPos targetPos = buffer.readBoolean() ? buffer.readBlockPos() : null;
        int targetEntityId = buffer.readVarInt();
        ResourceLocation siegeTypeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new RecruitsSiegeCommandC2SPayload(action, groupIds, targetPos, targetEntityId, siegeTypeId);
    }

    public static void handle(RecruitsSiegeCommandC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender && Platform.isModLoaded("recruits")) {
                RecruitsCompat.handleSiegeCommand(
                        sender, packet.action, packet.groupIds, packet.targetPos, packet.targetEntityId,
                        packet.siegeTypeId);
            }
        });
    }
}
