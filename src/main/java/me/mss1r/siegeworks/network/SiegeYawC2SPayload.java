package me.mss1r.siegeworks.network;

import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
//? if >=1.21 {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

//? if <1.21 {
/*public record SiegeYawC2SPayload(float yaw, float pitch) {
*///?} else {
public record SiegeYawC2SPayload(float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<SiegeYawC2SPayload> TYPE = new Type<>(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "siege_yaw"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiegeYawC2SPayload> STREAM_CODEC =
            StreamCodec.ofMember(SiegeYawC2SPayload::write, SiegeYawC2SPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<SiegeYawC2SPayload> type() { return TYPE; }
    //?}
    public static void encode(SiegeYawC2SPayload packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.yaw);
        buffer.writeFloat(packet.pitch);
    }

    public static SiegeYawC2SPayload decode(FriendlyByteBuf buffer) {
        return new SiegeYawC2SPayload(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(SiegeYawC2SPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null || !player.isPassenger()
                    || !Float.isFinite(packet.yaw) || !Float.isFinite(packet.pitch)) return;

            if (player.getVehicle() instanceof AbstractSiegeEntity siege) {
                if (siege.shouldPassengerControlRotation(player)) applyRotation(siege, packet.yaw, packet.pitch);
            } else if (player.getVehicle() instanceof AbstractHorse horse
                    && horse.getVehicle() instanceof AbstractSiegeEntity siege
                    && siege.shouldPassengerControlRotation(horse)
                    && siege.usesIndependentAim()) {
                siege.setPassengerAimTargetYaw(packet.yaw);
            }
        });
    }

    private static void applyRotation(AbstractSiegeEntity siege, float yaw, float pitch) {
        if (siege.usesIndependentAim()) {
            siege.setPassengerAimTargetYaw(yaw);
        }
        if (siege.usesIndependentPitchAim()) {
            siege.setPassengerAimTargetPitch(pitch);
        } else {
            siege.setTrackedPitch(pitch);
            siege.setXRot(siege.getTrackedPitch());
            siege.lastRiderPitch = siege.getTrackedPitch();
        }
    }
}
