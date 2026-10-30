package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
//? if <1.21 {
/*import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
*///?} else {
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
//?}

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class SiegeGameTestPlayers {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private SiegeGameTestPlayers() {
    }

    static FakePlayer create(ServerLevel level) {
        int id = Math.floorMod(NEXT_ID.getAndIncrement(), 1_000_000);
        return FakePlayerFactory.get(level,
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
