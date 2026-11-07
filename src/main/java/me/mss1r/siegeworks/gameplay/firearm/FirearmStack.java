package me.mss1r.siegeworks.gameplay.firearm;

import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class FirearmStack {
    private static final String KEY = "Firearm";
    private FirearmStack() {}

    public static FirearmLoad load(ItemStack stack) {
        CompoundTag data = MinecraftVersionCompat.customData(stack).getCompound(KEY);
        FirearmLoad.Charge charge;
        try { charge = FirearmLoad.Charge.valueOf(data.getString("Charge")); }
        catch (IllegalArgumentException exception) { charge = FirearmLoad.Charge.EMPTY; }
        int shots = Math.max(0, Math.min(FirearmLoad.SHOTS_PER_MATCH, data.getInt("MatchShots")));
        return new FirearmLoad(charge, shots, shots > 0 && data.getBoolean("MatchLit"));
    }

    public static void save(ItemStack stack, FirearmLoad load) {
        MinecraftVersionCompat.editCustomData(stack, root -> {
            CompoundTag data = root.getCompound(KEY);
            data.putString("Charge", load.charge().name());
            data.putInt("MatchShots", load.matchShots());
            data.putBoolean("MatchLit", load.matchLit());
            root.put(KEY, data);
        });
    }
}
