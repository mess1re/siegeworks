package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

public record SiegeDamageRules(Map<String, Boolean> entities,
                               Map<String, Boolean> items,
                               Map<String, Boolean> damageTypes,
                               Map<String, Double> entityMultipliers) {
    public static final SiegeDamageRules DEFAULT = new SiegeDamageRules(
            Map.of(
                    "minecraft:player", false,
                    "minecraft:vindicator", true,
                    "*", false
            ),
            Map.of(
                    "minecraft:wooden_axe", true,
                    "minecraft:stone_axe", true,
                    "minecraft:iron_axe", true,
                    "minecraft:golden_axe", true,
                    "minecraft:diamond_axe", true,
                    "minecraft:netherite_axe", true,
                    "*", false
            ),
            Map.of(
                    "projectile", false,
                    "explosion", true,
                    "*", false
            ),
            Map.of("*", 1.0D)
    );

    public static final Codec<SiegeDamageRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.BOOL)
                    .optionalFieldOf("entityDamageSources", Map.of())
                    .forGetter(SiegeDamageRules::entities),
            Codec.unboundedMap(Codec.STRING, Codec.BOOL)
                    .optionalFieldOf("itemDamageSources", Map.of())
                    .forGetter(SiegeDamageRules::items),
            Codec.unboundedMap(Codec.STRING, Codec.BOOL)
                    .optionalFieldOf("damageTypeSources", Map.of())
                    .forGetter(SiegeDamageRules::damageTypes),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("entityDamageMultipliers", DEFAULT.entityMultipliers())
                    .forGetter(SiegeDamageRules::entityMultipliers)
    ).apply(instance, SiegeDamageRules::new));

    public SiegeDamageRules {
        entities = Map.copyOf(entities);
        items = Map.copyOf(items);
        damageTypes = Map.copyOf(damageTypes);
        entityMultipliers = Map.copyOf(entityMultipliers);
    }

    public boolean allowsEntity(String entityId) {
        return resolveRule(entities, entityId);
    }

    public boolean allowsItem(String itemId) {
        return resolveRule(items, itemId);
    }

    public boolean allowsDamageType(String damageType) {
        return resolveRule(damageTypes, damageType);
    }

    public double multiplierForEntity(String entityId) {
        return entityMultipliers.getOrDefault(entityId, entityMultipliers.getOrDefault("*", 1.0D));
    }

    Optional<String> validationError() {
        return ProfileValidation.nonNegativeValues("entityDamageMultipliers", entityMultipliers);
    }

    private static boolean resolveRule(Map<String, Boolean> rules, String key) {
        return rules.getOrDefault(key, rules.getOrDefault("*", false));
    }
}
