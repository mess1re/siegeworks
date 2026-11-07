package me.mss1r.siegeworks.network;

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
//? if >=1.21 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class SiegeworksNetworking {
    private static final ResourceLocation SIEGE_YAW = id("siege_yaw");
    private static final ResourceLocation SIEGE_MOVEMENT = id("siege_movement");
    private static final ResourceLocation FIREARM_FIRE = id("firearm_fire");
    private static final ResourceLocation BOLT_THROWER_FIRE = id("bolt_thrower_fire");
    private static final ResourceLocation ONAGER_FIRE = id("onager_fire");
    private static final ResourceLocation MANTLET_ACTION = id("mantlet_action");
    private static final ResourceLocation MAINTENANCE_ACTION = id("maintenance_action");
    private static final ResourceLocation LADDER_PUT_DOWN = id("ladder_put_down");
    private static final ResourceLocation OPEN_MAINTENANCE = id("open_maintenance");
    private static final ResourceLocation PROJECTILE_PROFILES = id("projectile_profiles");
    private static final ResourceLocation POT_FILLINGS = id("pot_fillings");

    private SiegeworksNetworking() {
    }

    public static void register() {
        //? if <1.21 {

        /*register(NetworkManager.c2s(), SIEGE_YAW, SiegeYawC2SPayload::decode, SiegeYawC2SPayload::handle);
        register(NetworkManager.c2s(), SIEGE_MOVEMENT,
                SiegeMovementC2SPayload::decode, SiegeMovementC2SPayload::handle);
        register(NetworkManager.c2s(), FIREARM_FIRE, FirearmFireC2SPayload::decode, FirearmFireC2SPayload::handle);
        register(NetworkManager.c2s(), BOLT_THROWER_FIRE,
                BoltThrowerFireC2SPayload::decode, BoltThrowerFireC2SPayload::handle);
        register(NetworkManager.c2s(), ONAGER_FIRE,
                OnagerFireC2SPayload::decode, OnagerFireC2SPayload::handle);
        register(NetworkManager.c2s(), MANTLET_ACTION,
                MantletActionC2SPayload::decode, MantletActionC2SPayload::handle);
        register(NetworkManager.c2s(), MAINTENANCE_ACTION,
                MaintenanceActionC2SPayload::decode, MaintenanceActionC2SPayload::handle);
        register(NetworkManager.c2s(), LADDER_PUT_DOWN,
                LadderPutDownC2SPayload::decode, LadderPutDownC2SPayload::handle);
        *///?} else {
        NetworkManager.registerReceiver(NetworkManager.c2s(), SiegeYawC2SPayload.TYPE,
                SiegeYawC2SPayload.STREAM_CODEC, SiegeYawC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), SiegeMovementC2SPayload.TYPE,
                SiegeMovementC2SPayload.STREAM_CODEC, SiegeMovementC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), FirearmFireC2SPayload.TYPE, FirearmFireC2SPayload.STREAM_CODEC, FirearmFireC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), BoltThrowerFireC2SPayload.TYPE,
                BoltThrowerFireC2SPayload.STREAM_CODEC, BoltThrowerFireC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), OnagerFireC2SPayload.TYPE,
                OnagerFireC2SPayload.STREAM_CODEC, OnagerFireC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), MantletActionC2SPayload.TYPE,
                MantletActionC2SPayload.STREAM_CODEC, MantletActionC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), MaintenanceActionC2SPayload.TYPE,
                MaintenanceActionC2SPayload.STREAM_CODEC, MaintenanceActionC2SPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.c2s(), LadderPutDownC2SPayload.TYPE,
                LadderPutDownC2SPayload.STREAM_CODEC, LadderPutDownC2SPayload::handle);
        // A client learns client-bound types from its receivers; a dedicated server has to be told.
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(OpenMaintenanceS2CPayload.TYPE,
                    OpenMaintenanceS2CPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ProjectileProfilesS2CPayload.TYPE,
                    ProjectileProfilesS2CPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PotFillingsS2CPayload.TYPE, PotFillingsS2CPayload.STREAM_CODEC);
        }
        //?}
        EnvExecutor.runInEnv(Env.CLIENT, () -> SiegeworksNetworking::registerClientReceivers);
    }

    private static void registerClientReceivers() {
        //? if <1.21 {

        /*register(NetworkManager.s2c(), OPEN_MAINTENANCE,
                OpenMaintenanceS2CPayload::decode, OpenMaintenanceS2CPayload::handle);
        register(NetworkManager.s2c(), PROJECTILE_PROFILES,
                ProjectileProfilesS2CPayload::decode, ProjectileProfilesS2CPayload::handle);
        register(NetworkManager.s2c(), POT_FILLINGS, PotFillingsS2CPayload::decode, PotFillingsS2CPayload::handle);
        *///?} else {
        NetworkManager.registerReceiver(NetworkManager.s2c(), OpenMaintenanceS2CPayload.TYPE,
                OpenMaintenanceS2CPayload.STREAM_CODEC, OpenMaintenanceS2CPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), ProjectileProfilesS2CPayload.TYPE,
                ProjectileProfilesS2CPayload.STREAM_CODEC, ProjectileProfilesS2CPayload::handle);
        NetworkManager.registerReceiver(NetworkManager.s2c(), PotFillingsS2CPayload.TYPE,
                PotFillingsS2CPayload.STREAM_CODEC, PotFillingsS2CPayload::handle);
        //?}
    }

    //? if <1.21 {

    /*private static <T> void register(NetworkManager.Side side, ResourceLocation id,
                                     Function<FriendlyByteBuf, T> decoder,
                                     BiConsumer<T, NetworkManager.PacketContext> handler) {
        NetworkManager.registerReceiver(side, id,
                (buffer, context) -> handler.accept(decoder.apply(buffer), context));
    }
    *///?}

    public static void sendToServer(Object packet) {
        //? if <1.21 {

        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ResourceLocation id;
        if (packet instanceof SiegeYawC2SPayload value) {
            SiegeYawC2SPayload.encode(value, buffer);
            id = SIEGE_YAW;
        } else if (packet instanceof SiegeMovementC2SPayload value) {
            SiegeMovementC2SPayload.encode(value, buffer);
            id = SIEGE_MOVEMENT;
        } else if (packet instanceof FirearmFireC2SPayload value) {
            FirearmFireC2SPayload.encode(value, buffer);
            id = FIREARM_FIRE;
        } else if (packet instanceof BoltThrowerFireC2SPayload value) {
            BoltThrowerFireC2SPayload.encode(value, buffer);
            id = BOLT_THROWER_FIRE;
        } else if (packet instanceof OnagerFireC2SPayload value) {
            OnagerFireC2SPayload.encode(value, buffer);
            id = ONAGER_FIRE;
        } else if (packet instanceof MantletActionC2SPayload value) {
            MantletActionC2SPayload.encode(value, buffer);
            id = MANTLET_ACTION;
        } else if (packet instanceof MaintenanceActionC2SPayload value) {
            MaintenanceActionC2SPayload.encode(value, buffer);
            id = MAINTENANCE_ACTION;
        } else if (packet instanceof LadderPutDownC2SPayload value) {
            LadderPutDownC2SPayload.encode(value, buffer);
            id = LADDER_PUT_DOWN;
        } else {
            throw new IllegalArgumentException("Unsupported client packet: " + packet.getClass().getName());
        }
        NetworkManager.sendToServer(id, buffer);
        *///?} else {
        NetworkManager.sendToServer((CustomPacketPayload) packet);
        //?}
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        //? if <1.21 {

        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ResourceLocation id;
        if (packet instanceof OpenMaintenanceS2CPayload value) {
            OpenMaintenanceS2CPayload.encode(value, buffer);
            id = OPEN_MAINTENANCE;
        } else if (packet instanceof ProjectileProfilesS2CPayload value) {
            ProjectileProfilesS2CPayload.encode(value, buffer);
            id = PROJECTILE_PROFILES;
        } else if (packet instanceof PotFillingsS2CPayload value) {
            PotFillingsS2CPayload.encode(value, buffer);
            id = POT_FILLINGS;
        } else {
            throw new IllegalArgumentException("Unsupported server packet: " + packet.getClass().getName());
        }
        NetworkManager.sendToPlayer(player, id, buffer);
        *///?} else {
        NetworkManager.sendToPlayer(player, (CustomPacketPayload) packet);
        //?}
    }

    private static ResourceLocation id(String path) {
        return MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }
}
