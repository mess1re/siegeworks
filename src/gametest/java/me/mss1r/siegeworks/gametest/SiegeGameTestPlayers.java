package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import me.mss1r.axiomata.platform.AxiomataPlatform;


import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class SiegeGameTestPlayers {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private SiegeGameTestPlayers() {
    }

    public static ServerPlayer create(ServerLevel level) {
        int id = Math.floorMod(NEXT_ID.getAndIncrement(), 1_000_000);
        return (ServerPlayer) AxiomataPlatform.fakePlayer(level,
                new GameProfile(UUID.randomUUID(), "SiegeTest" + id));
    }

    static Player createRideable(ServerLevel level) {
        int id = Math.floorMod(NEXT_ID.getAndIncrement(), 1_000_000);
        return new RideableTestPlayer(level,
                new GameProfile(UUID.randomUUID(), "SiegeRider" + id));
    }

    private static final class RideableTestPlayer extends Player {
        private RideableTestPlayer(ServerLevel level, GameProfile profile) {
            super(level, BlockPos.ZERO, 0.0F, profile);
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }
}
