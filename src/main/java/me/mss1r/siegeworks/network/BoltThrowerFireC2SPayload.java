package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.siege.AbstractBoltThrowerEntity;
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
/*public record BoltThrowerFireC2SPayload() {
*///?} else {
public record BoltThrowerFireC2SPayload() implements CustomPacketPayload {
    public static final Type<BoltThrowerFireC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "bolt_thrower_fire"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoltThrowerFireC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(BoltThrowerFireC2SPayload::write, BoltThrowerFireC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<BoltThrowerFireC2SPayload> type() { return TYPE; }
    //?}
    public static final BoltThrowerFireC2SPayload INSTANCE = new BoltThrowerFireC2SPayload();

    public static void encode(BoltThrowerFireC2SPayload packet, FriendlyByteBuf buffer) {
    }

    public static BoltThrowerFireC2SPayload decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(BoltThrowerFireC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null && player.getVehicle() instanceof AbstractBoltThrowerEntity boltThrower) {
                boltThrower.requestRiderFire(player);
            }
        });
    }
}
