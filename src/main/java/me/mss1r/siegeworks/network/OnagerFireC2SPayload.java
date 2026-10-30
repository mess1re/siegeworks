package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.siege.OnagerEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if forge {
/*public record OnagerFireC2SPayload() {
*///?} else {
public record OnagerFireC2SPayload() implements CustomPacketPayload {
    public static final Type<OnagerFireC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "onager_fire"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OnagerFireC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(OnagerFireC2SPayload::write, OnagerFireC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<OnagerFireC2SPayload> type() { return TYPE; }
    //?}
    public static final OnagerFireC2SPayload INSTANCE = new OnagerFireC2SPayload();

    public static void encode(OnagerFireC2SPayload packet, FriendlyByteBuf buffer) {
    }

    public static OnagerFireC2SPayload decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(OnagerFireC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null && player.getVehicle() instanceof OnagerEntity onager) {
                onager.requestRiderFire(player);
            }
        });
    }
}
