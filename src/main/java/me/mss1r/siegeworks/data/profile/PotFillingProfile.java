package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What goes into an incendiary pot: the base that makes it burn, the additives and what each adds to the burst, the
 * slot limits and the wick. Keys are item ids, or tag ids prefixed with {@code #}.
 */
public record PotFillingProfile(Map<String, Integer> base, Effect baseBurst, Map<String, Effect> additives,
                                int additiveSlots, int maxOfAKind, String wick) {
    public static final ResourceLocation ID = MinecraftVersionCompat.id(Siegeworks.MOD_ID, "incendiary");

    /** Burst stats, or the amount one additive adds to them. */
    public record Effect(double fireRadius, double fireChance, int burnSeconds, double blastEnergy) {
        public static final Effect NONE = new Effect(0.0D, 0.0D, 0, 0.0D);
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("fireRadius", 0.0D).forGetter(Effect::fireRadius),
                Codec.DOUBLE.optionalFieldOf("fireChance", 0.0D).forGetter(Effect::fireChance),
                Codec.INT.optionalFieldOf("burnSeconds", 0).forGetter(Effect::burnSeconds),
                Codec.DOUBLE.optionalFieldOf("blastEnergy", 0.0D).forGetter(Effect::blastEnergy)
        ).apply(instance, Effect::new));

        public Effect plus(Effect other) {
            return new Effect(fireRadius + other.fireRadius, fireChance + other.fireChance,
                    burnSeconds + other.burnSeconds, blastEnergy + other.blastEnergy);
        }

        Optional<String> validationError(String name) {
            return ProfileValidation.nonNegative(name + ".fireRadius", fireRadius)
                    .or(() -> ProfileValidation.nonNegative(name + ".fireChance", fireChance))
                    .or(() -> ProfileValidation.nonNegative(name + ".burnSeconds", burnSeconds))
                    .or(() -> ProfileValidation.nonNegative(name + ".blastEnergy", blastEnergy));
        }
    }

    public static final PotFillingProfile DEFAULT = new PotFillingProfile(
            Map.of("minecraft:charcoal", 1, "minecraft:honeycomb", 1),
            new Effect(5.0D, 0.7D, 8, 0.0D),
            Map.of("minecraft:charcoal", new Effect(1.5D, 0.0D, 0, 0.0D),
                    "minecraft:honeycomb", new Effect(0.0D, 0.0D, 2, 0.0D),
                    "minecraft:blaze_powder", new Effect(0.0D, 0.075D, 0, 0.0D),
                    "minecraft:gunpowder", new Effect(0.0D, 0.0D, 0, 20_000.0D)),
            6, 3, "minecraft:string");

    public static final Codec<PotFillingProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("base").forGetter(PotFillingProfile::base),
            Effect.CODEC.fieldOf("baseBurst").forGetter(PotFillingProfile::baseBurst),
            Codec.unboundedMap(Codec.STRING, Effect.CODEC).optionalFieldOf("additives", Map.of())
                    .forGetter(PotFillingProfile::additives),
            Codec.INT.optionalFieldOf("additiveSlots", 6).forGetter(PotFillingProfile::additiveSlots),
            Codec.INT.optionalFieldOf("maxOfAKind", 3).forGetter(PotFillingProfile::maxOfAKind),
            Codec.STRING.optionalFieldOf("wick", "minecraft:string").forGetter(PotFillingProfile::wick)
    ).apply(instance, PotFillingProfile::new));

    public PotFillingProfile {
        base = Map.copyOf(base);
        additives = Map.copyOf(additives);
    }

    /** The loaded profile, or the built-in one before the server's arrives. */
    public static PotFillingProfile current() {
        return SiegeProfileCatalogs.POT_FILLINGS.get(ID);
    }

    public Optional<String> validationError() {
        if (base.isEmpty()) {
            return Optional.of("base needs at least one ingredient");
        }
        for (Map.Entry<String, Integer> entry : base.entrySet()) {
            Optional<String> error = keyError("base." + entry.getKey(), entry.getKey());
            if (error.isPresent()) {
                return error;
            }
            if (entry.getValue() < 1) {
                return Optional.of("base." + entry.getKey() + " must be at least 1");
            }
        }
        Optional<String> error = baseBurst.validationError("baseBurst");
        if (error.isPresent()) {
            return error;
        }
        for (Map.Entry<String, Effect> entry : additives.entrySet()) {
            error = keyError("additives." + entry.getKey(), entry.getKey())
                    .or(() -> entry.getValue().validationError("additives." + entry.getKey()));
            if (error.isPresent()) {
                return error;
            }
        }
        if (additiveSlots < 0) {
            return Optional.of("additiveSlots must not be negative");
        }
        if (maxOfAKind < 1) {
            return Optional.of("maxOfAKind must be at least 1");
        }
        return keyError("wick", wick);
    }

    private static Optional<String> keyError(String name, String key) {
        if (key.startsWith("#")) {
            return ResourceLocation.tryParse(key.substring(1)) == null
                    ? Optional.of(name + ": '" + key + "' is not a tag id") : Optional.empty();
        }
        ResourceLocation item = ResourceLocation.tryParse(key);
        if (item == null || !BuiltInRegistries.ITEM.containsKey(item)) {
            return Optional.of(name + ": there is no item '" + key + "' (a tag needs # in front)");
        }
        return Optional.empty();
    }

    public static boolean matches(String key, Item item) {
        if (key.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(key.substring(1));
            return tag != null && new ItemStack(item).is(TagKey.create(Registries.ITEM, tag));
        }
        return BuiltInRegistries.ITEM.getKey(item).equals(ResourceLocation.tryParse(key));
    }

    /** First of {@code keys} that {@code item} matches: exact items before tags, tags in id order. */
    @Nullable
    public static String firstMatch(Iterable<String> keys, Item item) {
        List<String> ordered = new ArrayList<>();
        keys.forEach(ordered::add);
        ordered.sort(Comparator.comparing((String key) -> key.startsWith("#")).thenComparing(key -> key));
        for (String key : ordered) {
            if (matches(key, item)) {
                return key;
            }
        }
        return null;
    }

    @Nullable
    public String additiveKey(Item item) {
        return firstMatch(additives.keySet(), item);
    }

    public boolean isIngredient(Item item) {
        return firstMatch(base.keySet(), item) != null || additiveKey(item) != null;
    }

    public boolean isWick(ItemStack stack) {
        return !stack.isEmpty() && matches(wick, stack.getItem());
    }

    public int baseSize() {
        return base.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Most blast energy any filling can reach, for scaling the burst sound. */
    public double fullCharge() {
        List<Double> energies = additives.values().stream().map(Effect::blastEnergy)
                .sorted(Comparator.reverseOrder()).toList();
        double total = baseBurst.blastEnergy();
        int slots = additiveSlots;
        for (double energy : energies) {
            int taken = Math.min(slots, maxOfAKind);
            total += taken * energy;
            slots -= taken;
        }
        return total;
    }

    /** Display name for a key: the item's, or "<first item> or similar" for a tag. */
    public static Component name(String key) {
        if (!key.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            return id == null ? Component.literal(key) : BuiltInRegistries.ITEM.get(id).getDescription();
        }
        ResourceLocation tag = ResourceLocation.tryParse(key.substring(1));
        Item shown = tag == null ? Items.BARRIER : BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                .flatMap(set -> set.stream().findFirst()).map(holder -> holder.value()).orElse(Items.BARRIER);
        return Component.translatable("tooltip.siegeworks.pot.any_of", shown.getDescription());
    }
}
