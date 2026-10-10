package me.mss1r.siegeworks.data.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.mss1r.axiomata.data.profile.ProfileValidation;

import java.util.Optional;

/** Beam mass in kg, contact dimensions and sideways spread in metres. Speed comes from the swing animation. */
public record RamImpactProfile(double mass, double width, double height, double spread) {
    public static final RamImpactProfile DEFAULT = new RamImpactProfile(3500.0D, 0.625D, 0.625D, 1.75D);
    public static final Codec<RamImpactProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("mass", DEFAULT.mass()).forGetter(RamImpactProfile::mass),
            Codec.DOUBLE.optionalFieldOf("width", DEFAULT.width()).forGetter(RamImpactProfile::width),
            Codec.DOUBLE.optionalFieldOf("height", DEFAULT.height()).forGetter(RamImpactProfile::height),
            Codec.DOUBLE.optionalFieldOf("spread", DEFAULT.spread()).forGetter(RamImpactProfile::spread)
    ).apply(instance, RamImpactProfile::new));

    Optional<String> validationError() {
        Optional<String> error = ProfileValidation.nonNegative("ramImpact.mass", mass);
        if (error.isPresent()) return error;
        error = ProfileValidation.nonNegative("ramImpact.spread", spread);
        if (error.isPresent()) return error;
        if (spread > 4.0D) return Optional.of("ramImpact.spread must not exceed 4 metres");
        if (!Double.isFinite(width) || width < 0.0625D || width > 16.0D) {
            return Optional.of("ramImpact.width must be between 0.0625 and 16 blocks");
        }
        if (!Double.isFinite(height) || height < 0.0625D || height > 16.0D) {
            return Optional.of("ramImpact.height must be between 0.0625 and 16 blocks");
        }
        return Optional.empty();
    }
}
