package me.mss1r.siegeworks.integration.recruits.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.netty.buffer.Unpooled;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class RecruitsNetworking {
    private static final Channel<RecruitsSiegeCommandC2SPayload> SIEGE_COMMAND = new Channel<>(id("recruits_siege_command"),
            RecruitsSiegeCommandC2SPayload::decode, RecruitsSiegeCommandC2SPayload::encode, RecruitsSiegeCommandC2SPayload::handle);
    private static final Channel<RecruitsTowerCrewC2SPayload> TOWER_CREW = new Channel<>(id("recruits_tower_crew"),
            RecruitsTowerCrewC2SPayload::decode, RecruitsTowerCrewC2SPayload::encode, RecruitsTowerCrewC2SPayload::handle);
    private static final Channel<RecruitsFireZoneC2SPayload> FIRE_ZONE = new Channel<>(id("recruits_fire_zone"),
            RecruitsFireZoneC2SPayload::decode, RecruitsFireZoneC2SPayload::encode, RecruitsFireZoneC2SPayload::handle);
    private static final Channel<RecruitsCommandStatesPayloads.Query> STATES_QUERY = new Channel<>(id("recruits_command_states_query"),
            RecruitsCommandStatesPayloads.Query::decode, RecruitsCommandStatesPayloads.Query::encode, RecruitsCommandStatesPayloads.Query::handle);
    private static final Channel<RecruitsCommandStatesPayloads.Answer> STATES_ANSWER = new Channel<>(id("recruits_command_states"),
            RecruitsCommandStatesPayloads.Answer::decode, RecruitsCommandStatesPayloads.Answer::encode, RecruitsCommandStatesPayloads.Answer::handle);

    private RecruitsNetworking() {
    }

    public static void register() {
        RecruitsCommandStatesPayloads.register();
        SIEGE_COMMAND.register(NetworkManager.c2s());
        TOWER_CREW.register(NetworkManager.c2s());
        FIRE_ZONE.register(NetworkManager.c2s());
        STATES_QUERY.register(NetworkManager.c2s());
        //? if neoforge {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(STATES_ANSWER.type(), STATES_ANSWER.codec());
        }
        //?}
        EnvExecutor.runInEnv(Env.CLIENT, () -> RecruitsNetworking::registerClient);
    }

    /** Client only: replies to the Recruits screen's siege button state requests. */
    public static void registerClient() {
        STATES_ANSWER.register(NetworkManager.s2c());
    }

    public static void sendToServer(RecruitsCommandStatesPayloads.Query packet) {
        STATES_QUERY.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, RecruitsCommandStatesPayloads.Answer packet) {
        //? if forge {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        STATES_ANSWER.encoder().accept(packet, buffer);
        NetworkManager.sendToPlayer(player, STATES_ANSWER.id(), buffer);
        *///?} else {
        NetworkManager.sendToPlayer(player, new Payload<>(STATES_ANSWER.type(), packet));
        //?}
    }

    public static void sendToServer(RecruitsSiegeCommandC2SPayload packet) {
        SIEGE_COMMAND.sendToServer(packet);
    }

    public static void sendToServer(RecruitsTowerCrewC2SPayload packet) {
        TOWER_CREW.sendToServer(packet);
    }

    public static void sendToServer(RecruitsFireZoneC2SPayload packet) {
        FIRE_ZONE.sendToServer(packet);
    }

    private record Channel<T>(ResourceLocation id, Function<FriendlyByteBuf, T> decoder,
                              BiConsumer<T, FriendlyByteBuf> encoder,
                              BiConsumer<T, NetworkManager.PacketContext> handler) {
        private void register(NetworkManager.Side side) {
            //? if forge {
            /*NetworkManager.registerReceiver(side, id,
                    (buffer, context) -> handler.accept(decoder.apply(buffer), context));
            *///?} else {
            NetworkManager.registerReceiver(side, type(), codec(),
                    (packet, context) -> handler.accept(packet.value(), context));
            //?}
        }

        private void sendToServer(T packet) {
            //? if forge {
            /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            encoder.accept(packet, buffer);
            NetworkManager.sendToServer(id, buffer);
            *///?} else {
            NetworkManager.sendToServer(new Payload<>(type(), packet));
            //?}
        }

        //? if neoforge {
        private CustomPacketPayload.Type<Payload<T>> type() { return new CustomPacketPayload.Type<>(id); }

        private StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec() {
            return StreamCodec.of((buffer, packet) -> encoder.accept(packet.value(), buffer),
                    buffer -> new Payload<>(type(), decoder.apply(buffer)));
        }
        //?}
    }

    //? if neoforge {
    private record Payload<T>(CustomPacketPayload.Type<Payload<T>> type, T value) implements CustomPacketPayload {}
    //?}

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
