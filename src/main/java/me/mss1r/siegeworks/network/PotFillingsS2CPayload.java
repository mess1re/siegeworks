package me.mss1r.siegeworks.network;

import me.mss1r.axiomata.data.profile.ProfileSnapshotCodec;
import dev.architectury.networking.NetworkManager;
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if neoforge {
import me.mss1r.siegeworks.Siegeworks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.Map;

/** Server pot fillings, so client tooltips and pot interactions use the same ingredients. */
//? if forge {

/*public record PotFillingsS2CPayload(Map<ResourceLocation, PotFillingProfile> profiles) {
*///?} else {
public record PotFillingsS2CPayload(Map<ResourceLocation, PotFillingProfile> profiles)
        implements CustomPacketPayload {
    public static final Type<PotFillingsS2CPayload> TYPE = new Type<>(
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "pot_fillings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PotFillingsS2CPayload> STREAM_CODEC =
            StreamCodec.ofMember(PotFillingsS2CPayload::write, PotFillingsS2CPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<PotFillingsS2CPayload> type() { return TYPE; }
    //?}
    private static final ProfileSnapshotCodec<PotFillingProfile> PROFILES =
            new ProfileSnapshotCodec<>(PotFillingProfile.CODEC);

    public static PotFillingsS2CPayload current() {
        return new PotFillingsS2CPayload(SiegeProfileCatalogs.POT_FILLINGS.snapshot());
    }

    public static void encode(PotFillingsS2CPayload packet, FriendlyByteBuf buffer) {
        PROFILES.write(buffer, packet.profiles);
    }

    public static PotFillingsS2CPayload decode(FriendlyByteBuf buffer) {
        return new PotFillingsS2CPayload(PROFILES.read(buffer));
    }

    public static void handle(PotFillingsS2CPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> SiegeProfileCatalogs.POT_FILLINGS.acceptFromServer(packet.profiles));
    }
}
