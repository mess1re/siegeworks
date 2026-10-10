package me.mss1r.siegeworks.data.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Checks the JSON before codecs can discard old or misspelled fields. */
public final class ProfileFormat {
    public static final int VERSION = 2;
    private static final Set<String> OLD_PROJECTILE_FIELDS = Set.of(
            "penetration", "drag", "impactFuse", "blockCostMultiplier", "blockDamageMultiplier",
            "energyLossMultiplier", "armorPiercing", "entityDamageMultiplier", "shockRadius",
            "shockDamageMultiplier", "baseExplosionPower", "speedExplosionScale", "shrapnelFragments",
            "shrapnelRadius", "shrapnelDamage");
    private static final Set<String> ENGINE_FIELDS = Set.of(
            "formatVersion", "maxHealth", "baseDamage", "muzzleVelocity", "accuracyMultiplier",
            "damageConfig", "scattershot", "ramImpact");
    private static final Set<String> PROJECTILE_FIELDS = Set.of(
            "formatVersion", "mass", "dragCoefficient", "diameter", "hardness", "motor", "entity",
            "shock", "blast", "fire");
    private static final Map<String, Set<String>> PROJECTILE_PARTS = Map.of(
            "motor", Set.of("thrust", "burnTime"),
            "entity", Set.of("damage", "armorPiercing", "structure"),
            "shock", Set.of("radius", "damage"),
            "blast", Set.of("energy"),
            "fire", Set.of("radius", "chance"));
    private static final Set<String> EFFECT_FIELDS = Set.of("fireRadius", "fireChance", "burnSeconds", "blastEnergy");

    private ProfileFormat() {
    }

    public static Optional<String> engine(JsonElement json) {
        return check(json, ENGINE_FIELDS, Set.of("projectileSpeed"), Map.of(
                "damageConfig", Set.of("entityDamageSources", "itemDamageSources", "damageTypeSources",
                        "entityDamageMultipliers"),
                "scattershot", Set.of("capacity", "minPellets", "maxPellets", "pelletsPerLoadedItemMin",
                        "pelletsPerLoadedItemMax", "spreadDegrees", "baseDamagePerPellet"),
                "ramImpact", Set.of("mass", "width", "height", "spread")));
    }

    public static Optional<String> projectile(JsonElement json) {
        return check(json, PROJECTILE_FIELDS, OLD_PROJECTILE_FIELDS, PROJECTILE_PARTS);
    }

    public static Optional<String> blockMaterial(JsonElement json) {
        Optional<String> error = check(json, Set.of("formatVersion", "block", "tag", "priority",
                "strength", "drag", "fractureEnergy", "projectileResistance"), Set.of(), Map.of());
        if (error.isEmpty() && json.getAsJsonObject().has("tag") && !json.getAsJsonObject().has("priority")) {
            return Optional.of("tag materials need an explicit priority");
        }
        return error;
    }

    public static Optional<String> potFilling(JsonElement json) {
        if (!json.isJsonObject()) {
            return Optional.of("expected a profile object");
        }
        JsonObject object = json.getAsJsonObject();
        Optional<String> error = version(object).or(() -> unknown(object, Set.of("formatVersion", "base",
                "baseBurst", "additives", "additiveSlots", "maxOfAKind", "wick"), ""));
        if (error.isPresent()) {
            return error;
        }
        if (!object.has("base") || !object.get("base").isJsonObject()) {
            return Optional.of("base must be an object of item or #tag ids to counts");
        }
        for (var entry : object.getAsJsonObject("base").entrySet()) {
            if (!isInteger(entry.getValue())) {
                return Optional.of("base." + entry.getKey() + " must be a whole number");
            }
        }
        if (!object.has("baseBurst")) {
            return Optional.of("baseBurst is missing");
        }
        error = effect(object.get("baseBurst"), "baseBurst");
        if (error.isPresent()) {
            return error;
        }
        if (object.has("additives")) {
            if (!object.get("additives").isJsonObject()) {
                return Optional.of("additives must be an object of item or #tag ids to effects");
            }
            for (var entry : object.getAsJsonObject("additives").entrySet()) {
                error = effect(entry.getValue(), "additives." + entry.getKey());
                if (error.isPresent()) {
                    return error;
                }
            }
        }
        for (String key : List.of("additiveSlots", "maxOfAKind")) {
            if (object.has(key) && !isInteger(object.get(key))) {
                return Optional.of(key + " must be a whole number");
            }
        }
        if (object.has("wick") && (!object.get("wick").isJsonPrimitive()
                || !object.get("wick").getAsJsonPrimitive().isString())) {
            return Optional.of("wick must be an item or #tag id");
        }
        return Optional.empty();
    }

    private static Optional<String> effect(JsonElement json, String name) {
        if (!json.isJsonObject()) {
            return Optional.of(name + " must be an object");
        }
        Optional<String> error = unknown(json.getAsJsonObject(), EFFECT_FIELDS, name + ".");
        if (error.isPresent()) {
            return error;
        }
        for (var field : json.getAsJsonObject().entrySet()) {
            boolean whole = field.getKey().equals("burnSeconds");
            if (whole ? !isInteger(field.getValue()) : !field.getValue().isJsonPrimitive()
                    || !field.getValue().getAsJsonPrimitive().isNumber()) {
                return Optional.of(name + "." + field.getKey() + " must be a " + (whole ? "whole number" : "number"));
            }
        }
        return Optional.empty();
    }

    private static boolean isInteger(JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                && value.getAsDouble() == Math.rint(value.getAsDouble());
    }

    private static Optional<String> version(JsonObject object) {
        if (object.has("formatVersion")) {
            JsonElement version = object.get("formatVersion");
            if (!version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
                    || version.getAsDouble() != VERSION) {
                return Optional.of("unsupported formatVersion " + version + "; expected " + VERSION);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> check(JsonElement json, Set<String> fields, Set<String> oldFields,
                                           Map<String, Set<String>> parts) {
        if (!json.isJsonObject()) {
            return Optional.of("expected a profile object");
        }
        JsonObject object = json.getAsJsonObject();
        var old = object.keySet().stream().filter(oldFields::contains).sorted().toList();
        if (!old.isEmpty()) {
            return Optional.of("beta.5 profile fields " + old
                    + " are not supported by format 2; see "
                    + "https://github.com/mess1re/siegeworks/wiki/Data-Pack-Reference");
        }
        Optional<String> error = version(object).or(() -> unknown(object, fields, ""));
        if (error.isPresent()) {
            return error;
        }
        for (String key : object.keySet()) {
            if (key.equals("formatVersion") || parts.containsKey(key)) continue;
            JsonElement value = object.get(key);
            boolean id = key.equals("block") || key.equals("tag");
            if (!value.isJsonPrimitive() || (id ? !value.getAsJsonPrimitive().isString()
                    : !value.getAsJsonPrimitive().isNumber())) {
                return Optional.of(key + " must be " + (id ? "a resource ID" : "a number"));
            }
            if (key.equals("priority") && value.getAsDouble() != value.getAsInt()) {
                return Optional.of("priority must be an integer");
            }
        }
        for (var part : parts.entrySet()) {
            if (object.has(part.getKey())) {
                if (!object.get(part.getKey()).isJsonObject()) {
                    return Optional.of(part.getKey() + " must be an object");
                }
                error = unknown(object.getAsJsonObject(part.getKey()), part.getValue(), part.getKey() + ".");
                if (error.isPresent()) {
                    return error;
                }
                if (!part.getKey().equals("damageConfig")) {
                    for (var field : object.getAsJsonObject(part.getKey()).entrySet()) {
                        if (!field.getValue().isJsonPrimitive() || !field.getValue().getAsJsonPrimitive().isNumber()) {
                            return Optional.of(part.getKey() + "." + field.getKey() + " must be a number");
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> unknown(JsonObject object, Set<String> fields, String prefix) {
        return object.keySet().stream().filter(key -> !fields.contains(key)).sorted().findFirst()
                .map(key -> "unknown field " + prefix + key);
    }
}
