package me.mss1r.siegeworks.gameplay.deployment;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeTeams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
import net.minecraft.util.datafix.DataFixTypes;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SiegeDeploymentLimits extends SavedData {
    public static final String TAG_DEPLOYMENT_OWNER = "SiegeworksDeploymentOwner";
    public static final String TAG_DEPLOYMENT_GROUP = "SiegeworksDeploymentGroup";
    public static final String TAG_CONSTRUCTION_OWNER = "AxiomataConstructionOwner";

    private static final String DATA_NAME = "siegeworks_deployment_limits";
    private static final String TAG_ENTRIES = "Entries";
    private static final String TAG_ENTITY = "Entity";
    private static final String TAG_GROUP = "Group";
    private static final String TAG_TYPE = "Type";

    private final Map<UUID, Entry> entries = new HashMap<>();

    private SiegeDeploymentLimits() {
    }

    public static Deployment resolve(ServerLevel level, ItemStack stack, @Nullable Player placer) {
        CompoundTag tag = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack);
        UUID ownerUuid = null;
        String groupKey = null;
        {
            if (tag.hasUUID(TAG_DEPLOYMENT_OWNER)) {
                ownerUuid = tag.getUUID(TAG_DEPLOYMENT_OWNER);
            } else if (tag.hasUUID(TAG_CONSTRUCTION_OWNER)) {
                ownerUuid = tag.getUUID(TAG_CONSTRUCTION_OWNER);
            }
            if (tag.contains(TAG_DEPLOYMENT_GROUP, Tag.TAG_STRING)) {
                groupKey = tag.getString(TAG_DEPLOYMENT_GROUP);
            }
        }

        if (ownerUuid == null && placer != null && !me.mss1r.siegeworks.platform.MinecraftVersionCompat.isFakePlayer(placer)) {
            ownerUuid = placer.getUUID();
        }
        if (ownerUuid == null) {
            return null;
        }
        return new Deployment(ownerUuid, groupKey == null || groupKey.isBlank()
                ? groupForOwner(level.getServer(), ownerUuid)
                : groupKey);
    }

    public static Deployment forOwner(ServerLevel level, UUID ownerUuid) {
        return new Deployment(ownerUuid, groupForOwner(level.getServer(), ownerUuid));
    }

    public static void writeToStack(ItemStack stack, Deployment deployment) {
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.editCustomData(stack, tag -> {
            tag.putUUID(TAG_DEPLOYMENT_OWNER, deployment.ownerUuid());
            tag.putString(TAG_DEPLOYMENT_GROUP, deployment.groupKey());
        });
    }

    public static boolean canDeployWithFeedback(ServerLevel level, EntityType<?> type,
                                                @Nullable Deployment deployment,
                                                @Nullable Player feedbackPlayer) {
        LimitCheck check = check(level, type, deployment);
        if (!check.allowed() && feedbackPlayer instanceof ServerPlayer serverPlayer
                && !me.mss1r.siegeworks.platform.MinecraftVersionCompat.isFakePlayer(feedbackPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.siegeworks.deployment_limit_reached",
                    type.getDescription(), check.current(), check.limit()), true);
        }
        return check.allowed();
    }

    public static LimitCheck check(ServerLevel level, EntityType<?> type, @Nullable Deployment deployment) {
        int limit = SiegeworksServerConfig.getDeploymentLimit(type);
        if (deployment == null) {
            return new LimitCheck(false, 0, limit);
        }

        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        int current = data(level).count(deployment.groupKey(), typeId);
        if (limit <= 0) {
            return new LimitCheck(true, current, limit);
        }
        return new LimitCheck(current < limit, current, limit);
    }

    public static void register(AbstractSiegeEntity siege) {
        if (!(siege.level() instanceof ServerLevel level)
                || siege.getDeploymentOwnerUuid() == null
                || siege.getDeploymentGroup().isBlank()) {
            return;
        }

        register(level, siege.getUUID(), siege.getType(), siege.getDeploymentGroup());
    }

    static void register(ServerLevel level, UUID entityUuid, EntityType<?> type, String groupKey) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        SiegeDeploymentLimits data = data(level);
        Entry next = new Entry(groupKey, typeId);
        Entry previous = data.entries.put(entityUuid, next);
        if (!next.equals(previous)) {
            data.setDirty();
        }
    }

    public static void unregister(AbstractSiegeEntity siege) {
        if (!(siege.level() instanceof ServerLevel level)) {
            return;
        }
        unregister(level, siege.getUUID());
    }

    static void unregister(ServerLevel level, UUID entityUuid) {
        SiegeDeploymentLimits data = data(level);
        if (data.entries.remove(entityUuid) != null) {
            data.setDirty();
        }
    }

    private int count(String groupKey, ResourceLocation typeId) {
        int count = 0;
        for (Entry entry : entries.values()) {
            if (entry.groupKey().equals(groupKey) && entry.typeId().equals(typeId)) {
                count++;
            }
        }
        return count;
    }

    private static SiegeDeploymentLimits data(ServerLevel level) {
        //? if <1.21 {

        /*return level.getServer().overworld().getDataStorage().computeIfAbsent(
                SiegeDeploymentLimits::load, SiegeDeploymentLimits::new, DATA_NAME);
        *///?} else {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SiegeDeploymentLimits::new, SiegeDeploymentLimits::load,
                        DataFixTypes.SAVED_DATA_COMMAND_STORAGE), DATA_NAME);
        //?}
    }

    //? if <1.21 {
    /*private static SiegeDeploymentLimits load(CompoundTag tag) {
    *///?} else {
    private static SiegeDeploymentLimits load(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        SiegeDeploymentLimits data = new SiegeDeploymentLimits();
        ListTag entries = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (Tag value : entries) {
            CompoundTag entryTag = (CompoundTag) value;
            if (!entryTag.hasUUID(TAG_ENTITY)) {
                continue;
            }
            ResourceLocation typeId = ResourceLocation.tryParse(entryTag.getString(TAG_TYPE));
            String groupKey = entryTag.getString(TAG_GROUP);
            if (typeId != null && !groupKey.isBlank()) {
                data.entries.put(entryTag.getUUID(TAG_ENTITY), new Entry(groupKey, typeId));
            }
        }
        return data;
    }

    @Override
    //? if <1.21 {
    /*public CompoundTag save(CompoundTag tag) {
    *///?} else {
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    //?}
        ListTag savedEntries = new ListTag();
        entries.forEach((entityUuid, entry) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(TAG_ENTITY, entityUuid);
            entryTag.putString(TAG_GROUP, entry.groupKey());
            entryTag.putString(TAG_TYPE, entry.typeId().toString());
            savedEntries.add(entryTag);
        });
        tag.put(TAG_ENTRIES, savedEntries);
        return tag;
    }

    private static String groupForOwner(MinecraftServer server, UUID ownerUuid) {
        String team = SiegeTeams.teamOf(server, ownerUuid);
        return team == null ? "player:" + ownerUuid : "team:" + team;
    }

    public record Deployment(UUID ownerUuid, String groupKey) {
    }

    public record LimitCheck(boolean allowed, int current, int limit) {
    }

    private record Entry(String groupKey, ResourceLocation typeId) {
    }
}
