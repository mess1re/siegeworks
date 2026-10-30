package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
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
/*public record MantletActionC2SPayload(int entityId, int action) {
*///?} else {
public record MantletActionC2SPayload(int entityId, int action) implements CustomPacketPayload {
    public static final Type<MantletActionC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "mantlet_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MantletActionC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(MantletActionC2SPayload::write, MantletActionC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<MantletActionC2SPayload> type() { return TYPE; }
    //?}
    public static void encode(MantletActionC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.action);
    }

    public static MantletActionC2SPayload decode(FriendlyByteBuf buffer) {
        return new MantletActionC2SPayload(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MantletActionC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(packet.entityId);
            if (entity instanceof MantletEntity mantlet) {
                mantlet.requestAction(player, packet.action);
            }
        });
    }
}
