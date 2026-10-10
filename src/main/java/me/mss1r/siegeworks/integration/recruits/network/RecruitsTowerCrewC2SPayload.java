package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import me.mss1r.siegeworks.integration.recruits.RecruitsCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RecruitsTowerCrewC2SPayload(int action, int towerEntityId, List<UUID> groupIds) {
    public static final int ACTION_BOARD = 0;
    public static final int ACTION_UNLOAD = 2;
    public static final int ACTION_RETURN = 3;
    private static final int MAX_GROUPS = 64;

    public RecruitsTowerCrewC2SPayload {
        groupIds = List.copyOf(groupIds);
    }

    public static void encode(RecruitsTowerCrewC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.towerEntityId);
        buffer.writeVarInt(packet.groupIds.size());
        packet.groupIds.forEach(buffer::writeUUID);
    }

    public static RecruitsTowerCrewC2SPayload decode(FriendlyByteBuf buffer) {
        int action = buffer.readVarInt();
        int towerEntityId = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_GROUPS) {
            throw new IllegalArgumentException("Invalid Recruits group count: " + count);
        }

        List<UUID> groupIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            groupIds.add(buffer.readUUID());
        }
        return new RecruitsTowerCrewC2SPayload(action, towerEntityId, groupIds);
    }

    public static void handle(RecruitsTowerCrewC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sender && Platform.isModLoaded("recruits")) {
                RecruitsCompat.handleTowerCrewCommand(
                        sender, packet.action, packet.towerEntityId, packet.groupIds);
            }
        });
    }
}
