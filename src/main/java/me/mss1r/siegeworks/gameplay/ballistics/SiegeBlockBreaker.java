package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry;
import me.mss1r.axiomata.ballistics.ProtectedBlockAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SiegeBlockBreaker {
    private SiegeBlockBreaker() {
    }

    /**
     * Whether a siege weapon attributed to this player may damage the block under the server's terrain rule. With
     * {@code RESPECT_PROTECTION} it runs the same checks as the player breaking the block by hand, so claim mods and
     * spawn protection apply.
     */
    public static boolean mayDamage(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.BLOCKS.mayDamage(level, pos, state, breaker);
    }

    /** Fire is a placement, so it also has to pass the claim mod's placement event. */
    public static boolean placeFire(ServerLevel level, BlockPos pos, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.BLOCKS.placeFire(level, pos, breaker);
    }

    public static boolean placeBlock(ServerLevel level, BlockPos pos, BlockState placed, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.BLOCKS.placeBlock(level, pos, placed, breaker);
    }

    public static boolean breakBlock(ServerLevel level, BlockPos pos, @Nullable Player breaker) {
        return SiegeBallisticsEnvironment.BLOCKS.breakBlock(level, pos, breaker);
    }

    public static float siegeResistance(Level level, BlockPos pos, BlockState state, Explosion probe) {
        return ProtectedBlockAccess.damageResistance(level, pos, state, probe);
    }

    public static Explosion damageProbe(Level level, Vec3 center, @Nullable Entity source) {
        return ProtectedBlockAccess.damageProbe(level, center, source);
    }

    /**
     * Player responsible for blocks broken by {@code owner}: the controlling player, else the owner, online or not.
     */
    @Nullable
    public static Player responsiblePlayer(@Nullable Entity owner) {
        if (owner == null || !(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        Player online = onlinePlayer(owner, new HashSet<>());
        return online != null ? online : playerFor(level, responsibleUuid(owner, new HashSet<>()));
    }

    /** Player responsible for {@code owner}, cached so shots stay attributed after their engine is gone. */
    @Nullable
    public static UUID responsibleUuid(@Nullable Entity owner) {
        return responsibleUuid(owner, new HashSet<>());
    }

    /** Player for this id: the real player when online, otherwise a fake player for their profile. */
    @Nullable
    public static Player playerFor(ServerLevel level, @Nullable UUID playerId) {
        return SiegeBallisticsEnvironment.BLOCKS.attributedPlayer(level, playerId);
    }

    @Nullable
    private static Player onlinePlayer(@Nullable Entity owner, Set<UUID> visited) {
        if (owner == null || !visited.add(owner.getUUID())) {
            return null;
        }
        if (owner instanceof Player player) {
            return player;
        }
        if (owner instanceof AbstractSiegeEntity siege) {
            if (siege.getControllingPassenger() instanceof Player crew) {
                return crew;
            }
            Entity machineOwner = siege.getOperator();
            if (machineOwner != null && machineOwner != siege) {
                Player attributed = onlinePlayer(machineOwner, visited);
                if (attributed != null) {
                    return attributed;
                }
            }
            UUID engineOwner = siege.getOwnerUuid();
            return engineOwner == null || siege.level().getServer() == null
                    ? null
                    : siege.level().getServer().getPlayerList().getPlayer(engineOwner);
        }
        UUID playerOwner = SiegePlayerAttributionRegistry.playerOwnerOf(owner);
        return playerOwner == null || owner.level().getServer() == null
                ? null
                : owner.level().getServer().getPlayerList().getPlayer(playerOwner);
    }

    @Nullable
    private static UUID responsibleUuid(@Nullable Entity owner, Set<UUID> visited) {
        if (owner == null || !visited.add(owner.getUUID())) {
            return null;
        }
        if (owner instanceof Player player) {
            return player.getUUID();
        }
        if (owner instanceof AbstractSiegeEntity siege) {
            Entity machineOwner = siege.getOperator();
            if (machineOwner != null && machineOwner != siege) {
                UUID attributed = responsibleUuid(machineOwner, visited);
                if (attributed != null) {
                    return attributed;
                }
            }
            return siege.getOwnerUuid();
        }
        return SiegePlayerAttributionRegistry.playerOwnerOf(owner);
    }
}
