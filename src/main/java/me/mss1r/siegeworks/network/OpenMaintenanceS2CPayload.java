package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if forge {

/*public record OpenMaintenanceS2CPayload(
        int entityId,
        String title,
        int health,
        int maxHealth,
        boolean hasRecipe,
        boolean dismantling,
        int dismantleProgress,
        int dismantleRequired,
        String repairCost,
        String dismantleRefund,
        byte finish,
        int waxApplied,
        int waxRequired,
        int copperApplied,
        int copperRequired,
        int weatheringLossPercent
) {
*///?} else {
public record OpenMaintenanceS2CPayload(
        int entityId, String title, int health, int maxHealth, boolean hasRecipe,
        boolean dismantling, int dismantleProgress, int dismantleRequired,
        String repairCost, String dismantleRefund, byte finish, int waxApplied, int waxRequired,
        int copperApplied, int copperRequired, int weatheringLossPercent
) implements CustomPacketPayload {
    public static final Type<OpenMaintenanceS2CPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "open_maintenance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMaintenanceS2CPayload> STREAM_CODEC =
            StreamCodec.ofMember(OpenMaintenanceS2CPayload::write, OpenMaintenanceS2CPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<OpenMaintenanceS2CPayload> type() { return TYPE; }
    //?}
    public static void encode(OpenMaintenanceS2CPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUtf(packet.title);
        buffer.writeVarInt(packet.health);
        buffer.writeVarInt(packet.maxHealth);
        buffer.writeBoolean(packet.hasRecipe);
        buffer.writeBoolean(packet.dismantling);
        buffer.writeVarInt(packet.dismantleProgress);
        buffer.writeVarInt(packet.dismantleRequired);
        buffer.writeUtf(packet.repairCost);
        buffer.writeUtf(packet.dismantleRefund);
        buffer.writeByte(packet.finish);
        buffer.writeVarInt(packet.waxApplied);
        buffer.writeVarInt(packet.waxRequired);
        buffer.writeVarInt(packet.copperApplied);
        buffer.writeVarInt(packet.copperRequired);
        buffer.writeVarInt(packet.weatheringLossPercent);
    }

    public static OpenMaintenanceS2CPayload decode(FriendlyByteBuf buffer) {
        return new OpenMaintenanceS2CPayload(
                buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(32767), buffer.readUtf(32767), buffer.readByte(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(OpenMaintenanceS2CPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            try {
                Class<?> client = Class.forName("me.mss1r.siegeworks.client.maintenance.SiegeMaintenanceClient");
                client.getMethod("open", OpenMaintenanceS2CPayload.class).invoke(null, packet);
            } catch (ReflectiveOperationException ignored) {
            }
        });
    }
}
