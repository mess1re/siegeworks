package me.mss1r.siegeworks.data.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record SiegeDamageRules(Map<String, Boolean> entities,
                               Map<String, Boolean> items,
                               Map<String, Boolean> damageTypes) {
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
            )
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
                    .forGetter(SiegeDamageRules::damageTypes)
    ).apply(instance, SiegeDamageRules::new));

    public SiegeDamageRules {
        entities = Map.copyOf(entities);
        items = Map.copyOf(items);
        damageTypes = Map.copyOf(damageTypes);
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

    private static boolean resolveRule(Map<String, Boolean> rules, String key) {
        return rules.getOrDefault(key, rules.getOrDefault("*", false));
    }
}
