package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21 {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

/**
 * Sent when a player carrying a ladder asks to put it down. With both hands busy there is no item use to send instead.
 */
//? if <1.21 {
/*public record LadderPutDownC2SPayload() {
*///?} else {
public record LadderPutDownC2SPayload() implements CustomPacketPayload {
    public static final Type<LadderPutDownC2SPayload> TYPE =
            new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "ladder_put_down"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LadderPutDownC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(LadderPutDownC2SPayload::write, LadderPutDownC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<LadderPutDownC2SPayload> type() { return TYPE; }
    //?}
    public static void encode(LadderPutDownC2SPayload packet, FriendlyByteBuf buffer) {
    }

    public static LadderPutDownC2SPayload decode(FriendlyByteBuf buffer) {
        return new LadderPutDownC2SPayload();
    }

    public static void handle(LadderPutDownC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                LadderCarry.putDown(player);
            }
        });
    }
}
