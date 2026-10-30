package me.mss1r.siegeworks.gameplay.ownership;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
//? if neoforge {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** When each engine owner was last online, so engines of players who stopped playing can be released. */
public final class SiegeOwnerActivity extends SavedData {
    private static final String DATA_NAME = "siegeworks_owner_activity";
    private static final String TAG_PLAYERS = "Players";
    private static final String TAG_UUID = "Uuid";
    private static final String TAG_NAME = "Name";
    private static final String TAG_LAST_SEEN = "LastSeen";

    private final Map<UUID, Seen> players = new HashMap<>();

    private SiegeOwnerActivity() {
    }

    public static void record(ServerPlayer player, long nowMillis) {
        record(player.serverLevel(), player.getUUID(), player.getScoreboardName(), nowMillis);
    }

    public static void record(ServerLevel level, UUID playerUuid, String name, long nowMillis) {
        SiegeOwnerActivity data = data(level);
        data.players.put(playerUuid, new Seen(name, nowMillis));
        data.setDirty();
    }

    /**
     * Whether the owner or anyone on their team was online within the window. An owner seen for the
     * first time starts the window now, so engines placed before this was tracked are not released at once.
     */
    public static boolean sideSeenWithin(ServerLevel level, UUID ownerUuid, long windowMillis, long nowMillis) {
        if (SiegeTeams.sideOnline(level, ownerUuid)) {
            return true;
        }
        SiegeOwnerActivity data = data(level);
        Seen owner = data.players.get(ownerUuid);
        if (owner == null) {
            data.players.put(ownerUuid, new Seen("", nowMillis));
            data.setDirty();
            return true;
        }
        long cutoff = nowMillis - windowMillis;
        if (owner.lastSeen() >= cutoff) {
            return true;
        }
        MinecraftServer server = level.getServer();
        String teamName = SiegeTeams.teamOf(level, ownerUuid);
        PlayerTeam team = teamName != null ? server.getScoreboard().getPlayerTeam(teamName)
                : owner.name().isEmpty() ? null : server.getScoreboard().getPlayersTeam(owner.name());
        if (team == null) {
            return false;
        }
        return data.players.values().stream()
                .anyMatch(seen -> seen.lastSeen() >= cutoff && team.getPlayers().contains(seen.name()));
    }

    private static SiegeOwnerActivity data(ServerLevel level) {
        //? if forge {

        /*return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SiegeOwnerActivity::load, SiegeOwnerActivity::new, DATA_NAME);
        *///?} else {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SiegeOwnerActivity::new, SiegeOwnerActivity::load,
                        DataFixTypes.SAVED_DATA_COMMAND_STORAGE), DATA_NAME);
        //?}
    }

    //? if forge {
    /*private static SiegeOwnerActivity load(CompoundTag tag) {
    *///?} else {
    private static SiegeOwnerActivity load(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        SiegeOwnerActivity data = new SiegeOwnerActivity();
        for (Tag value : tag.getList(TAG_PLAYERS, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID(TAG_UUID)) {
                data.players.put(entry.getUUID(TAG_UUID),
                        new Seen(entry.getString(TAG_NAME), entry.getLong(TAG_LAST_SEEN)));
            }
        }
        return data;
    }

    @Override
    //? if forge {
    /*public CompoundTag save(CompoundTag tag) {
    *///?} else {
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        ListTag entries = new ListTag();
        players.forEach((uuid, seen) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(TAG_UUID, uuid);
            entry.putString(TAG_NAME, seen.name());
            entry.putLong(TAG_LAST_SEEN, seen.lastSeen());
            entries.add(entry);
        });
        tag.put(TAG_PLAYERS, entries);
        return tag;
    }

    private record Seen(String name, long lastSeen) {
    }
}
