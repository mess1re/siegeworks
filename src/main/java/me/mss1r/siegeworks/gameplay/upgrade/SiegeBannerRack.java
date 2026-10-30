package me.mss1r.siegeworks.gameplay.upgrade;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SiegeBannerRack {
    private final String bannersTag;
    private static final String TAG_ITEM = "Item";
    private static final String TAG_SLOT = "Slot";

    private final Host host;
    private final List<ItemStack> banners;

    public SiegeBannerRack(Host host, int slots, String bannersTag) {
        this.bannersTag = bannersTag;
        this.host = host;
        banners = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            banners.add(ItemStack.EMPTY);
        }
    }

    public int size() {
        return banners.size();
    }

    public ItemStack get(int slot) {
        if (slot < 0 || slot >= banners.size()) {
            return ItemStack.EMPTY;
        }
        if (!host.clientSide()) {
            return banners.get(slot);
        }

        ListTag synchronizedBanners = host.synchronizedData().getList(bannersTag, Tag.TAG_COMPOUND);
        for (int index = 0; index < synchronizedBanners.size(); index++) {
            CompoundTag bannerTag = synchronizedBanners.getCompound(index);
            if (bannerTag.getInt(TAG_SLOT) == slot) {
                return me.mss1r.siegeworks.platform.MinecraftVersionCompat.readItem(
                        bannerTag.getCompound(TAG_ITEM), host.registryAccess());
            }
        }
        return ItemStack.EMPTY;
    }

    public int install(ItemStack source) {
        if (!(source.getItem() instanceof BannerItem)) return -1;
        for (int slot = 0; slot < banners.size(); slot++) {
            if (banners.get(slot).isEmpty()) {
                ItemStack banner = source.copy();
                banner.setCount(1);
                banners.set(slot, banner);
                synchronize();
                return slot;
            }
        }
        return -1;
    }

    public ItemStack removeLast() {
        for (int slot = banners.size() - 1; slot >= 0; slot--) {
            ItemStack banner = banners.get(slot);
            if (!banner.isEmpty()) {
                banners.set(slot, ItemStack.EMPTY);
                synchronize();
                return banner;
            }
        }
        return ItemStack.EMPTY;
    }

    public void load(CompoundTag tag) {
        for (int slot = 0; slot < banners.size(); slot++) {
            banners.set(slot, ItemStack.EMPTY);
        }

        ListTag savedBanners = tag.getList(bannersTag, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedBanners.size(); index++) {
            CompoundTag bannerTag = savedBanners.getCompound(index);
            int slot = bannerTag.getInt(TAG_SLOT);
            ItemStack banner = me.mss1r.siegeworks.platform.MinecraftVersionCompat.readItem(
                    bannerTag.getCompound(TAG_ITEM), host.registryAccess());
            if (slot >= 0 && slot < banners.size() && banner.getItem() instanceof BannerItem) {
                banner.setCount(1);
                banners.set(slot, banner);
            }
        }
        synchronize();
    }

    public void save(CompoundTag tag) {
        tag.put(bannersTag, serialize());
    }

    public void dropAll() {
        for (int slot = 0; slot < banners.size(); slot++) {
            ItemStack banner = banners.get(slot);
            if (!banner.isEmpty()) {
                host.drop(banner);
                banners.set(slot, ItemStack.EMPTY);
            }
        }
        synchronize();
    }

    private void synchronize() {
        if (host.clientSide()) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.put(bannersTag, serialize());
        host.setSynchronizedData(data);
    }

    private ListTag serialize() {
        ListTag savedBanners = new ListTag();
        for (int slot = 0; slot < banners.size(); slot++) {
            ItemStack banner = banners.get(slot);
            if (banner.isEmpty()) {
                continue;
            }
            CompoundTag bannerTag = new CompoundTag();
            bannerTag.putInt(TAG_SLOT, slot);
            bannerTag.put(TAG_ITEM, me.mss1r.siegeworks.platform.MinecraftVersionCompat.writeItem(
                    banner, host.registryAccess()));
            savedBanners.add(bannerTag);
        }
        return savedBanners;
    }

    public interface Host {
        boolean clientSide();

        RegistryAccess registryAccess();

        CompoundTag synchronizedData();

        void setSynchronizedData(CompoundTag data);

        void drop(ItemStack stack);
    }
}
