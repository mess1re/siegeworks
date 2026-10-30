package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? if >=1.21 {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if <1.21 {
/*public record SiegeMovementC2SPayload(float forward, float steering, float yaw) {
*///?} else {
public record SiegeMovementC2SPayload(float forward, float steering, float yaw) implements CustomPacketPayload {
    public static final Type<SiegeMovementC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "siege_movement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiegeMovementC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(SiegeMovementC2SPayload::write, SiegeMovementC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<SiegeMovementC2SPayload> type() { return TYPE; }
    //?}
    public static void encode(SiegeMovementC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.steering);
        buffer.writeFloat(packet.yaw);
    }

    public static SiegeMovementC2SPayload decode(FriendlyByteBuf buffer) {
        return new SiegeMovementC2SPayload(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(SiegeMovementC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null || !player.isPassenger()
                    || !Float.isFinite(packet.forward) || !Float.isFinite(packet.steering)
                    || !Float.isFinite(packet.yaw)) return;

            float forward = Mth.clamp(packet.forward, -1.0F, 1.0F);
            float steering = Mth.clamp(packet.steering, -1.0F, 1.0F);
            if (player.getVehicle() instanceof AbstractSiegeEntity siege) {
                if (siege.shouldPassengerControlMovement(player)) {
                    siege.setMovementInput(forward, steering);
                    siege.reportDriverYaw(packet.yaw);
                }
            } else if (player.getVehicle() instanceof AbstractHorse horse
                    && horse.getVehicle() instanceof AbstractSiegeEntity siege
                    && siege.shouldPassengerControlMovement(horse)) {
                siege.setMovementInput(forward, steering);
                siege.reportDriverYaw(packet.yaw);
            }
        });
    }
}
