package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
//? if >=1.21 {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if <1.21 {
/*public record MaintenanceActionC2SPayload(int entityId, int action) {
*///?} else {
public record MaintenanceActionC2SPayload(int entityId, int action) implements CustomPacketPayload {
    public static final Type<MaintenanceActionC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "maintenance_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaintenanceActionC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(MaintenanceActionC2SPayload::write, MaintenanceActionC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MaintenanceActionC2SPayload> type() { return TYPE; }
    //?}
    public static final int ACTION_REPAIR = 0;
    public static final int ACTION_START_DISMANTLE = 1;
    public static final int ACTION_CANCEL_DISMANTLE = 2;
    public static final int ACTION_REFRESH = 3;

    public static void encode(MaintenanceActionC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.action);
    }

    public static MaintenanceActionC2SPayload decode(FriendlyByteBuf buffer) {
        return new MaintenanceActionC2SPayload(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MaintenanceActionC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(packet.entityId);
            if (entity instanceof AbstractSiegeEntity siege) {
                siege.handleMaintenanceAction(player, packet.action);
            }
        });
    }
}
