package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RecruitsNetworking {
    private static final ResourceLocation SIEGE_COMMAND = id("recruits_siege_command");
    private static final ResourceLocation TOWER_CREW = id("recruits_tower_crew");
    private static final ResourceLocation FIRE_ZONE = id("recruits_fire_zone");
    private static final ResourceLocation STATES_QUERY = id("recruits_command_states_query");
    private static final ResourceLocation STATES_ANSWER = id("recruits_command_states");

    private RecruitsNetworking() {
    }

    public static void register() {
        RecruitsCommandStatesPayloads.register();
        register(SIEGE_COMMAND,
                RecruitsSiegeCommandC2SPayload::decode, RecruitsSiegeCommandC2SPayload::handle);
        register(TOWER_CREW,
                RecruitsTowerCrewC2SPayload::decode, RecruitsTowerCrewC2SPayload::handle);
        register(FIRE_ZONE,
                RecruitsFireZoneC2SPayload::decode, RecruitsFireZoneC2SPayload::handle);
        register(STATES_QUERY,
                RecruitsCommandStatesPayloads.Query::decode, RecruitsCommandStatesPayloads.Query::handle);
    }

    /** Client only: replies to the Recruits screen's siege button state requests. */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.s2c(), STATES_ANSWER,
                (buffer, context) -> RecruitsCommandStatesPayloads.Answer.handle(
                        RecruitsCommandStatesPayloads.Answer.decode(buffer), context));
    }

    public static void sendToServer(RecruitsCommandStatesPayloads.Query packet) {
        sendToServer(STATES_QUERY, packet, RecruitsCommandStatesPayloads.Query::encode);
    }

    public static void sendToPlayer(ServerPlayer player, RecruitsCommandStatesPayloads.Answer packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        RecruitsCommandStatesPayloads.Answer.encode(packet, buffer);
        NetworkManager.sendToPlayer(player, STATES_ANSWER, buffer);
    }

    public static void sendToServer(RecruitsSiegeCommandC2SPayload packet) {
        sendToServer(SIEGE_COMMAND, packet, RecruitsSiegeCommandC2SPayload::encode);
    }

    public static void sendToServer(RecruitsTowerCrewC2SPayload packet) {
        sendToServer(TOWER_CREW, packet, RecruitsTowerCrewC2SPayload::encode);
    }

    public static void sendToServer(RecruitsFireZoneC2SPayload packet) {
        sendToServer(FIRE_ZONE, packet, RecruitsFireZoneC2SPayload::encode);
    }

    private static <T> void register(ResourceLocation id,
                                     Function<FriendlyByteBuf, T> decoder,
                                     BiConsumer<T, NetworkManager.PacketContext> handler) {
        NetworkManager.registerReceiver(NetworkManager.c2s(), id,
                (buffer, context) -> handler.accept(decoder.apply(buffer), context));
    }

    private static <T> void sendToServer(ResourceLocation id, T packet,
                                         BiConsumer<T, FriendlyByteBuf> encoder) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        encoder.accept(packet, buffer);
        NetworkManager.sendToServer(id, buffer);
    }

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
