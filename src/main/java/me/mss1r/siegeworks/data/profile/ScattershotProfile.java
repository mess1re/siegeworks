package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record ScattershotProfile(int capacity, int minPellets, int maxPellets,
                                 int pelletsPerLoadedItemMin, int pelletsPerLoadedItemMax,
                                 float spreadDegrees, double baseDamagePerPellet) {
    public static final ScattershotProfile DEFAULT =
            new ScattershotProfile(8, 8, 8, 1, 1, 8.0F, 10.0D);

    public static final Codec<ScattershotProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("capacity", DEFAULT.capacity()).forGetter(ScattershotProfile::capacity),
            Codec.INT.optionalFieldOf("minPellets", DEFAULT.minPellets()).forGetter(ScattershotProfile::minPellets),
            Codec.INT.optionalFieldOf("maxPellets", DEFAULT.maxPellets()).forGetter(ScattershotProfile::maxPellets),
            Codec.INT.optionalFieldOf("pelletsPerLoadedItemMin", DEFAULT.pelletsPerLoadedItemMin())
                    .forGetter(ScattershotProfile::pelletsPerLoadedItemMin),
            Codec.INT.optionalFieldOf("pelletsPerLoadedItemMax", DEFAULT.pelletsPerLoadedItemMax())
                    .forGetter(ScattershotProfile::pelletsPerLoadedItemMax),
            Codec.FLOAT.optionalFieldOf("spreadDegrees", DEFAULT.spreadDegrees()).forGetter(ScattershotProfile::spreadDegrees),
            Codec.DOUBLE.optionalFieldOf("baseDamagePerPellet", DEFAULT.baseDamagePerPellet()).forGetter(ScattershotProfile::baseDamagePerPellet)
    ).apply(instance, ScattershotProfile::new));

    Optional<String> validationError() {
        if (capacity < 1) {
            return Optional.of("capacity must be at least one");
        }
        if (minPellets < 1) {
            return Optional.of("minPellets must be at least one");
        }
        if (maxPellets < minPellets) {
            return Optional.of("maxPellets must not be smaller than minPellets");
        }
        if (pelletsPerLoadedItemMin < 1) {
            return Optional.of("pelletsPerLoadedItemMin must be at least one");
        }
        if (pelletsPerLoadedItemMax < pelletsPerLoadedItemMin) {
            return Optional.of("pelletsPerLoadedItemMax must not be smaller than pelletsPerLoadedItemMin");
        }
        Optional<String> spreadError = ProfileValidation.nonNegative("spreadDegrees", spreadDegrees);
        return spreadError.isPresent()
                ? spreadError
                : ProfileValidation.nonNegative("baseDamagePerPellet", baseDamagePerPellet);
    }
}
