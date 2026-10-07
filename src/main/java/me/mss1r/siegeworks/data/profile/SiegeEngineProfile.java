package me.mss1r.siegeworks.data.profile;

import me.mss1r.axiomata.data.profile.ProfileValidation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * @param maxHealth the engine's health; when absent it keeps what its entity type was registered with
 * @param muzzleVelocity launch speed in metres per second; one block is treated as one metre
 */
public record SiegeEngineProfile(Optional<Double> maxHealth, double baseDamage, double muzzleVelocity,
                                 float accuracyMultiplier,
                                 SiegeDamageRules damageRules, ScattershotProfile scattershot) {
    private static final double DEFAULT_MUZZLE_VELOCITY = 60.0D;
    public static final SiegeEngineProfile DEFAULT = new SiegeEngineProfile(
            Optional.empty(), 25.0D, DEFAULT_MUZZLE_VELOCITY, 1.0F, SiegeDamageRules.DEFAULT,
            ScattershotProfile.DEFAULT
    );

    public static final Codec<SiegeEngineProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("maxHealth").forGetter(SiegeEngineProfile::maxHealth),
            Codec.DOUBLE.optionalFieldOf("baseDamage", 25.0D).forGetter(SiegeEngineProfile::baseDamage),
            Codec.DOUBLE.optionalFieldOf("muzzleVelocity", DEFAULT_MUZZLE_VELOCITY)
                    .forGetter(SiegeEngineProfile::muzzleVelocity),
            Codec.FLOAT.optionalFieldOf("accuracyMultiplier", 1.2F).forGetter(SiegeEngineProfile::accuracyMultiplier),
            SiegeDamageRules.CODEC.optionalFieldOf("damageConfig", SiegeDamageRules.DEFAULT)
                    .forGetter(SiegeEngineProfile::damageRules),
            ScattershotProfile.CODEC.optionalFieldOf("scattershot", ScattershotProfile.DEFAULT)
                    .forGetter(SiegeEngineProfile::scattershot)
    ).apply(instance, SiegeEngineProfile::new));

    public Optional<String> validationError() {
        if (maxHealth.isPresent() && (!(maxHealth.get() > 0.0D) || !Double.isFinite(maxHealth.get()))) {
            return Optional.of("maxHealth must be greater than zero");
        }
        if (!(muzzleVelocity > 0.0D) || !Double.isFinite(muzzleVelocity)) {
            return Optional.of("muzzleVelocity must be greater than zero");
        }
        Optional<String> error = ProfileValidation.nonNegative("baseDamage", baseDamage);
        if (error.isPresent()) {
            return error;
        }
        error = ProfileValidation.nonNegative("accuracyMultiplier", accuracyMultiplier);
        if (error.isPresent()) {
            return error;
        }
        error = damageRules.validationError();
        return error.isPresent() ? error : scattershot.validationError();
    }
}
