package me.mss1r.siegeworks.network;

import me.mss1r.axiomata.data.profile.ProfileSnapshotCodec;
import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
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

/** Server projectile profiles, so clients predict projectiles the same way. */
//? if forge {

/*public record ProjectileProfilesS2CPayload(Map<ResourceLocation, ProjectilePhysicsProfile> profiles) {
*///?} else {
public record ProjectileProfilesS2CPayload(Map<ResourceLocation, ProjectilePhysicsProfile> profiles)
        implements CustomPacketPayload {
    public static final Type<ProjectileProfilesS2CPayload> TYPE = new Type<>(
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "projectile_profiles"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProjectileProfilesS2CPayload> STREAM_CODEC =
            StreamCodec.ofMember(ProjectileProfilesS2CPayload::write, ProjectileProfilesS2CPayload::decode);
    private void write(RegistryFriendlyByteBuf buffer) { encode(this, buffer); }
    @Override public Type<ProjectileProfilesS2CPayload> type() { return TYPE; }
    //?}
    private static final ProfileSnapshotCodec<ProjectilePhysicsProfile> PROFILES =
            new ProfileSnapshotCodec<>(ProjectilePhysicsProfile.CODEC);

    public static ProjectileProfilesS2CPayload current() {
        return new ProjectileProfilesS2CPayload(SiegeProfileCatalogs.PROJECTILES.snapshot());
    }

    public static void encode(ProjectileProfilesS2CPayload packet, FriendlyByteBuf buffer) {
        PROFILES.write(buffer, packet.profiles);
    }

    public static ProjectileProfilesS2CPayload decode(FriendlyByteBuf buffer) {
        return new ProjectileProfilesS2CPayload(PROFILES.read(buffer));
    }

    public static void handle(ProjectileProfilesS2CPayload packet, NetworkManager.PacketContext context) {
        context.queue(() -> SiegeProfileCatalogs.PROJECTILES.acceptFromServer(packet.profiles));
    }
}
