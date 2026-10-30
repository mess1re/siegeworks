package me.mss1r.siegeworks.item;

import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOwnership;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21 {
import net.minecraft.world.item.Item;
//?}
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SiegeLadderDeploymentItem extends SiegeDeploymentItem {
    public static final String TAG_SECTIONS = "Sections";
    public static final String TAG_STORED_HEALTH = "StoredHealth";
    public static final String TAG_RELOCATION_OWNER = "RelocationOwner";
    public static final String TAG_CONSTRUCTION_OWNER = "AxiomataConstructionOwner";

    public SiegeLadderDeploymentItem(Properties properties) {
        super(SiegeworksEntities.SIEGE_LADDER_ENTITY, properties);
    }


    public static int getSections(ItemStack stack) {
        CompoundTag tag = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack);
        return SiegeLadderEntity.MIN_SECTIONS <= tag.getInt(TAG_SECTIONS) && tag.getInt(TAG_SECTIONS) <= SiegeLadderEntity.MAX_SECTIONS
                ? tag.getInt(TAG_SECTIONS)
                : SiegeLadderEntity.MIN_SECTIONS;
    }

    public static ItemStack withSections(ItemStack stack, int sections) {
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.editCustomData(stack, tag ->
                tag.putInt(TAG_SECTIONS, Math.max(SiegeLadderEntity.MIN_SECTIONS,
                        Math.min(SiegeLadderEntity.MAX_SECTIONS, sections))));
        return stack;
    }

    public static ItemStack withStoredHealth(ItemStack stack, float health) {
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.editCustomData(stack,
                tag -> tag.putFloat(TAG_STORED_HEALTH, Math.max(1.0F, health)));
        return stack;
    }

    public static float getStoredHealth(ItemStack stack) {
        CompoundTag tag = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack);
        return tag.contains(TAG_STORED_HEALTH) ? tag.getFloat(TAG_STORED_HEALTH) : -1.0F;
    }

    @Nullable
    public static UUID getRelocationOwner(ItemStack stack) {
        CompoundTag tag = me.mss1r.siegeworks.platform.MinecraftVersionCompat.customData(stack);
        if (tag.hasUUID(TAG_RELOCATION_OWNER)) {
            return tag.getUUID(TAG_RELOCATION_OWNER);
        }
        return tag.hasUUID(TAG_CONSTRUCTION_OWNER) ? tag.getUUID(TAG_CONSTRUCTION_OWNER) : null;
    }

    public static ItemStack withRelocationOwner(ItemStack stack, UUID ownerUuid) {
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.editCustomData(stack,
                tag -> tag.putUUID(TAG_RELOCATION_OWNER, ownerUuid));
        return stack;
    }

    public static boolean canBePlacedBy(ItemStack stack, Entity placer) {
        return SiegeOwnership.playerOf(placer) != null && SiegeAccess.sameSide(placer, getRelocationOwner(stack));
    }

    @Override
    protected InteractionResult validatePlacement(ServerLevel level, UseOnContext context,
                                                  EntityType<?> type) {
        Player player = context.getPlayer();
        if (player != null
                && !me.mss1r.siegeworks.platform.MinecraftVersionCompat.isFakePlayer(player)
                && !SiegeAccess.sameSide(player, getRelocationOwner(context.getItemInHand()))) {
            player.displayClientMessage(Component.translatable("message.siegeworks.ladder.not_owner"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void configureSpawnedEntity(Entity spawned, UseOnContext context) {
        SiegeLadderEntity entity = (SiegeLadderEntity) spawned;
        Player player = context.getPlayer();
        UUID storedOwner = getRelocationOwner(context.getItemInHand());
        UUID placementOwner = storedOwner != null
                ? storedOwner
                : player == null || me.mss1r.siegeworks.platform.MinecraftVersionCompat.isFakePlayer(player)
                ? null
                : player.getUUID();
        entity.setSections(getSections(context.getItemInHand()));
        entity.setOwnerUuid(placementOwner);
        float storedHealth = getStoredHealth(context.getItemInHand());
        if (storedHealth > 0.0F) {
            entity.setHealth(Math.min(entity.getMaxHealth(), storedHealth));
        }
        if (me.mss1r.siegeworks.platform.MinecraftVersionCompat.hasCustomName(context.getItemInHand())) {
            entity.setCustomName(context.getItemInHand().getHoverName());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                //? if <1.21 {
                                /*@Nullable Level level,
                                *///?} else {
                                Item.TooltipContext context,
                                //?}
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.siegeworks.siege_ladder_spawner.sections", getSections(stack)));
        if (getSections(stack) < SiegeLadderEntity.MAX_SECTIONS) {
            tooltipComponents.add(Component.translatable("item.siegeworks.siege_ladder_spawner.extend")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

}
