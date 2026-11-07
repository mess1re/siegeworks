package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if <1.21 {
/*public record FirearmFireC2SPayload() {
*///?} else {
public record FirearmFireC2SPayload() implements CustomPacketPayload {
    public static final Type<FirearmFireC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "firearm_fire"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FirearmFireC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(FirearmFireC2SPayload::write, FirearmFireC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<FirearmFireC2SPayload> type() { return TYPE; }
    //?}
    public static final FirearmFireC2SPayload INSTANCE = new FirearmFireC2SPayload();

    public static void encode(FirearmFireC2SPayload packet, FriendlyByteBuf buffer) {
    }

    public static FirearmFireC2SPayload decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(FirearmFireC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                me.mss1r.siegeworks.gameplay.firearm.FirearmActions.requestFire(player);
            }
        });
    }
}
