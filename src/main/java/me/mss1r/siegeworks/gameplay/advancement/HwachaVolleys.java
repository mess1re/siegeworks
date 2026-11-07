package me.mss1r.siegeworks.gameplay.advancement;

//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HwachaVolleys extends SavedData {
    private static final String DATA_NAME = "siegeworks_hwacha_volleys";
    private static final int REQUIRED_KILLS = 3;
    // A full volley takes under 7 seconds to launch and each rocket has up to 12 seconds of flight.
    private static final int TRACKING_TICKS = 30 * 20;

    private final Map<UUID, Volley> active = new HashMap<>();

    @Nullable
    public static UUID start(ServerLevel level, LivingEntity operator) {
        if (!(operator instanceof ServerPlayer player) || SiegeAdvancement.MUNJONGS_VOLLEY.isDone(player)) return null;
        HwachaVolleys data = data(level);
        long now = level.getGameTime();
        data.active.values().removeIf(volley -> volley.expiresAt() <= now);
        UUID id = UUID.randomUUID();
        data.active.put(id, new Volley(player.getUUID(), now + TRACKING_TICKS, new HashSet<>()));
        data.setDirty();
        return id;
    }

    public static void recordKill(ServerLevel level, @Nullable UUID id, LivingEntity target, @Nullable Entity attacker) {
        if (id == null) return;
        HwachaVolleys data = data(level);
        Volley volley = data.active.get(id);
        if (volley == null || volley.expiresAt() <= level.getGameTime()) return;
        ServerPlayer player = attacker instanceof ServerPlayer shooter && volley.player().equals(shooter.getUUID())
                && !shooter.isRemoved() ? shooter : level.getServer().getPlayerList().getPlayer(volley.player());
        if (player == null || !SiegeAdvancement.isOpponent(player, target) || !volley.kills().add(target.getUUID())) return;
        if (volley.kills().size() >= REQUIRED_KILLS) {
            SiegeAdvancement.MUNJONGS_VOLLEY.award(player);
            data.active.remove(id);
        }
        data.setDirty();
    }

    private static HwachaVolleys data(ServerLevel level) {
        //? if <1.21 {
        /*return level.getDataStorage().computeIfAbsent(HwachaVolleys::load, HwachaVolleys::new, DATA_NAME);
        *///?} else {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HwachaVolleys::new, HwachaVolleys::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                DATA_NAME);
        //?}
    }

    //? if <1.21 {
    /*private static HwachaVolleys load(CompoundTag tag) {
    *///?} else {
    private static HwachaVolleys load(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        HwachaVolleys data = new HwachaVolleys();
        for (Tag value : tag.getList("Volleys", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.hasUUID("Id") || !entry.hasUUID("Player")) continue;
            Set<UUID> kills = new HashSet<>();
            for (Tag target : entry.getList("Kills", Tag.TAG_COMPOUND)) {
                CompoundTag victim = (CompoundTag) target;
                if (victim.hasUUID("Target")) kills.add(victim.getUUID("Target"));
            }
            data.active.put(entry.getUUID("Id"), new Volley(entry.getUUID("Player"), entry.getLong("ExpiresAt"), kills));
        }
        return data;
    }

    @Override
    //? if <1.21 {
    /*public CompoundTag save(CompoundTag tag) {
    *///?} else {
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        ListTag volleys = new ListTag();
        active.forEach((id, volley) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putUUID("Player", volley.player());
            entry.putLong("ExpiresAt", volley.expiresAt());
            ListTag kills = new ListTag();
            for (UUID target : volley.kills()) {
                CompoundTag victim = new CompoundTag();
                victim.putUUID("Target", target);
                kills.add(victim);
            }
            entry.put("Kills", kills);
            volleys.add(entry);
        });
        tag.put("Volleys", volleys);
        return tag;
    }

    private record Volley(UUID player, long expiresAt, Set<UUID> kills) {}
}
