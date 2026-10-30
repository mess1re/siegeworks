package me.mss1r.siegeworks.gameplay.upgrade;

import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TowerCoveringController {
    private static final String TAG_BOTTOM = "LeatherBottom";
    private static final String TAG_TOP = "LeatherTop";
    private static final String TAG_BOTTOM_MATERIALS = "LeatherBottomMaterials";
    private static final String TAG_TOP_MATERIALS = "LeatherTopMaterials";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_COUNT = "Count";
    private static final int MATERIALS_PER_LAYER = 64;
    private static final double HEALTH_PER_LAYER = 50.0D;
    //? if <1.21 {
    /*private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString("e61566ce-bb65-4ff7-bd17-22e4e0d1bba9");
    *///?} else {
    private static final ResourceLocation HEALTH_MODIFIER_ID =
            MinecraftVersionCompat.id("siegeworks", "siege_tower_leather_covering");
    //?}
    private static final TagKey<Item> MATERIALS = TagKey.create(
            Registries.ITEM, MinecraftVersionCompat.id("siegeworks", "tower_coverings"));

    private final Host host;
    private final Map<ResourceLocation, Integer> bottomMaterials = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> topMaterials = new LinkedHashMap<>();

    public TowerCoveringController(Host host) {
        this.host = host;
    }

    public boolean accepts(ItemStack stack) {
        return stack.is(MATERIALS);
    }

    public int bottomProgress() {
        return host.bottomProgress();
    }

    public int topProgress() {
        return host.topProgress();
    }

    public boolean hasBottom() {
        return bottomProgress() >= MATERIALS_PER_LAYER;
    }

    public boolean hasTop() {
        return topProgress() >= MATERIALS_PER_LAYER;
    }

    public boolean installed() {
        return hasBottom() || hasTop();
    }

    public Map<ResourceLocation, Integer> materials() {
        Map<ResourceLocation, Integer> materials = new LinkedHashMap<>();
        bottomMaterials.forEach((item, count) -> materials.merge(item, count, Integer::sum));
        topMaterials.forEach((item, count) -> materials.merge(item, count, Integer::sum));
        return Collections.unmodifiableMap(materials);
    }

    public void load(CompoundTag tag) {
        host.setBottomProgress(Mth.clamp(tag.getInt(TAG_BOTTOM), 0, MATERIALS_PER_LAYER));
        host.setTopProgress(Mth.clamp(tag.getInt(TAG_TOP), 0, MATERIALS_PER_LAYER));
        loadMaterials(tag.getList(TAG_BOTTOM_MATERIALS, Tag.TAG_COMPOUND),
                bottomMaterials, bottomProgress());
        loadMaterials(tag.getList(TAG_TOP_MATERIALS, Tag.TAG_COMPOUND),
                topMaterials, topProgress());
        refreshDurability();
    }

    public void save(CompoundTag tag) {
        tag.putInt(TAG_BOTTOM, bottomProgress());
        tag.putInt(TAG_TOP, topProgress());
        tag.put(TAG_BOTTOM_MATERIALS, saveMaterials(bottomMaterials));
        tag.put(TAG_TOP_MATERIALS, saveMaterials(topMaterials));
    }

    public InteractionResult add(Player player, ItemStack source, ServerLevel level) {
        boolean bottomLayer;
        Map<ResourceLocation, Integer> targetMaterials;
        String layerKey;
        if (!hasBottom()) {
            bottomLayer = true;
            targetMaterials = bottomMaterials;
            layerKey = "message.siegeworks.tower.covering_bottom";
        } else if (!hasTop()) {
            bottomLayer = false;
            targetMaterials = topMaterials;
            layerKey = "message.siegeworks.tower.covering_top";
        } else {
            player.displayClientMessage(Component.translatable("message.siegeworks.tower.covering_full"), true);
            return InteractionResult.SUCCESS;
        }

        int current = bottomLayer ? bottomProgress() : topProgress();
        int added = player.getAbilities().instabuild
                ? MATERIALS_PER_LAYER - current
                : Math.min(source.getCount(), MATERIALS_PER_LAYER - current);
        ResourceLocation materialId = BuiltInRegistries.ITEM.getKey(source.getItem());
        targetMaterials.merge(materialId, added, Integer::sum);
        if (bottomLayer) {
            host.setBottomProgress(current + added);
        } else {
            host.setTopProgress(current + added);
        }
        refreshDurability();
        if (!player.getAbilities().instabuild) {
            source.shrink(added);
        }

        level.playSound(null, host.tower().blockPosition(),
                me.mss1r.siegeworks.platform.MinecraftVersionCompat.leatherEquipSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(
                "message.siegeworks.tower.covering_progress",
                Component.translatable(layerKey),
                current + added,
                MATERIALS_PER_LAYER), true);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult remove(Player player, ItemStack shears, InteractionHand hand,
                                    ServerLevel level) {
        boolean topLayer = topProgress() > 0;
        Map<ResourceLocation, Integer> removedMaterials = topLayer ? topMaterials : bottomMaterials;
        String layerKey = topLayer
                ? "message.siegeworks.tower.covering_top"
                : "message.siegeworks.tower.covering_bottom";

        returnMaterials(player, removedMaterials);
        removedMaterials.clear();
        if (topLayer) {
            host.setTopProgress(0);
        } else {
            host.setBottomProgress(0);
        }
        refreshDurability();
        me.mss1r.siegeworks.platform.MinecraftVersionCompat.damageHeldItem(shears, 1, player, hand);
        level.playSound(null, host.tower().blockPosition(), SoundEvents.SHEEP_SHEAR,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(
                "message.siegeworks.tower.covering_removed",
                Component.translatable(layerKey)), true);
        return InteractionResult.SUCCESS;
    }

    private void refreshDurability() {
        LivingEntity tower = host.tower();
        AttributeInstance maxHealth = tower.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        int completedLayers = (hasBottom() ? 1 : 0) + (hasTop() ? 1 : 0);
        double expectedBonus = completedLayers * HEALTH_PER_LAYER;
        AttributeModifier existing = maxHealth.getModifier(HEALTH_MODIFIER_ID);
        if (existing != null && modifierAmount(existing) == expectedBonus) {
            clearFireWhenCovered(tower);
            return;
        }

        double previousBonus = existing == null ? 0.0D : modifierAmount(existing);
        if (existing != null) {
            maxHealth.removeModifier(HEALTH_MODIFIER_ID);
        }
        if (expectedBonus > 0.0D) {
            //? if <1.21 {
            /*maxHealth.addPermanentModifier(new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    "Siege tower leather covering",
                    expectedBonus,
                    AttributeModifier.Operation.ADDITION));
            *///?} else {
            maxHealth.addPermanentModifier(new AttributeModifier(
                    HEALTH_MODIFIER_ID, expectedBonus, AttributeModifier.Operation.ADD_VALUE));
            //?}
        }

        float addedHealth = (float) Math.max(0.0D, expectedBonus - previousBonus);
        tower.setHealth(Math.min(tower.getHealth() + addedHealth, tower.getMaxHealth()));
        clearFireWhenCovered(tower);
    }

    private void clearFireWhenCovered(LivingEntity tower) {
        if (installed() && tower.isOnFire()) {
            tower.clearFire();
        }
    }

    private static void returnMaterials(Player player, Map<ResourceLocation, Integer> materials) {
        materials.forEach((itemId, count) -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            int remaining = count;
            while (item != Items.AIR && remaining > 0) {
                ItemStack returned = new ItemStack(item, 1);
                returned.setCount(Math.min(remaining, returned.getMaxStackSize()));
                remaining -= returned.getCount();
                if (!player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
        });
    }

    private static ListTag saveMaterials(Map<ResourceLocation, Integer> materials) {
        ListTag saved = new ListTag();
        materials.forEach((itemId, count) -> {
            if (count <= 0) {
                return;
            }
            CompoundTag material = new CompoundTag();
            material.putString(TAG_ITEM, itemId.toString());
            material.putInt(TAG_COUNT, count);
            saved.add(material);
        });
        return saved;
    }

    private static void loadMaterials(ListTag saved, Map<ResourceLocation, Integer> materials,
                                      int expectedCount) {
        materials.clear();
        int remaining = expectedCount;
        for (int index = 0; index < saved.size() && remaining > 0; index++) {
            CompoundTag material = saved.getCompound(index);
            ResourceLocation itemId = ResourceLocation.tryParse(material.getString(TAG_ITEM));
            int count = Math.min(Math.max(0, material.getInt(TAG_COUNT)), remaining);
            if (itemId == null || count == 0 || BuiltInRegistries.ITEM.get(itemId) == Items.AIR) {
                continue;
            }
            materials.merge(itemId, count, Integer::sum);
            remaining -= count;
        }
        if (remaining > 0) {
            materials.merge(BuiltInRegistries.ITEM.getKey(Items.LEATHER), remaining, Integer::sum);
        }
    }

    private static double modifierAmount(AttributeModifier modifier) {
        //? if <1.21 {
        /*return modifier.getAmount();
        *///?} else {
        return modifier.amount();
        //?}
    }

    public interface Host {
        LivingEntity tower();

        int bottomProgress();

        void setBottomProgress(int progress);

        int topProgress();

        void setTopProgress(int progress);
    }
}
