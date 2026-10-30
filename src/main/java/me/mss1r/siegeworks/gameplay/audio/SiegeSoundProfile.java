package me.mss1r.siegeworks.gameplay.audio;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Objects;

public record SiegeSoundProfile(MovementCue movement, SoundCue reload,
                                SoundCue firing, SoundCue attack) {
    private static final double DEFAULT_MOVE_RANGE = 30.0D;
    private static final double DEFAULT_RELOAD_RANGE = 15.0D;
    private static final double DEFAULT_FIRING_RANGE = 75.0D;
    private static final double DEFAULT_ATTACK_RANGE = 40.0D;

    public static SiegeSoundProfile defaults() {
        //? if <1.21 {
        /*SoundEvent explosion = SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        SoundEvent explosion = SoundEvents.GENERIC_EXPLODE.value();
        //?}
        return new SiegeSoundProfile(
                new MovementCue(SoundEvents.HORSE_STEP_WOOD, 150, DEFAULT_MOVE_RANGE),
                new SoundCue(SoundEvents.ITEM_BREAK, DEFAULT_RELOAD_RANGE),
                new SoundCue(explosion, DEFAULT_FIRING_RANGE),
                new SoundCue(explosion, DEFAULT_ATTACK_RANGE)
        );
    }

    public SiegeSoundProfile {
        movement = Objects.requireNonNull(movement, "movement");
        reload = Objects.requireNonNull(reload, "reload");
        firing = Objects.requireNonNull(firing, "firing");
        attack = Objects.requireNonNull(attack, "attack");
    }

    public SiegeSoundProfile withMovementSound(SoundEvent sound) {
        return new SiegeSoundProfile(movement.withSound(sound), reload, firing, attack);
    }

    public SiegeSoundProfile withMovementInterval(int intervalTicks) {
        return new SiegeSoundProfile(movement.withInterval(intervalTicks), reload, firing, attack);
    }

    public SiegeSoundProfile withMovementRange(double range) {
        return new SiegeSoundProfile(movement.withRange(range), reload, firing, attack);
    }

    public SiegeSoundProfile withReloadSound(SoundEvent sound) {
        return new SiegeSoundProfile(movement, reload.withSound(sound), firing, attack);
    }

    public SiegeSoundProfile withReloadRange(double range) {
        return new SiegeSoundProfile(movement, reload.withRange(range), firing, attack);
    }

    public SiegeSoundProfile withFiringSound(SoundEvent sound) {
        return new SiegeSoundProfile(movement, reload, firing.withSound(sound), attack);
    }

    public SiegeSoundProfile withFiringRange(double range) {
        return new SiegeSoundProfile(movement, reload, firing.withRange(range), attack);
    }

    public SiegeSoundProfile withAttackSound(SoundEvent sound) {
        return new SiegeSoundProfile(movement, reload, firing, attack.withSound(sound));
    }

    public SiegeSoundProfile withMovementVolume(float volume) {
        return new SiegeSoundProfile(movement.withVolume(volume), reload, firing, attack);
    }

    public SiegeSoundProfile withReloadVolume(float volume) {
        return new SiegeSoundProfile(movement, reload.withVolume(volume), firing, attack);
    }

    public SiegeSoundProfile withFiringVolume(float volume) {
        return new SiegeSoundProfile(movement, reload, firing.withVolume(volume), attack);
    }

    public SiegeSoundProfile withAttackVolume(float volume) {
        return new SiegeSoundProfile(movement, reload, firing, attack.withVolume(volume));
    }

    public SiegeSoundProfile withAttackRange(double range) {
        return new SiegeSoundProfile(movement, reload, firing, attack.withRange(range));
    }

    public record SoundCue(SoundEvent sound, double range, float volume) {
        public SoundCue(SoundEvent sound, double range) {
            this(sound, range, 1.0F);
        }

        public SoundCue {
            sound = Objects.requireNonNull(sound, "sound");
            if (!Float.isFinite(volume) || volume < 0.0F) {
                throw new IllegalArgumentException("Sound volume must be finite and non-negative");
            }
            if (!Double.isFinite(range) || range < 0.0D) {
                throw new IllegalArgumentException("Sound range must be finite and non-negative");
            }
        }

        SoundCue withSound(SoundEvent replacement) {
            return new SoundCue(replacement, range, volume);
        }

        SoundCue withRange(double replacement) {
            return new SoundCue(sound, replacement, volume);
        }

        SoundCue withVolume(float replacement) {
            return new SoundCue(sound, range, replacement);
        }
    }

    public record MovementCue(SoundEvent sound, int intervalTicks, double range, float volume) {
        public MovementCue(SoundEvent sound, int intervalTicks, double range) {
            this(sound, intervalTicks, range, 1.0F);
        }

        public MovementCue {
            sound = Objects.requireNonNull(sound, "sound");
            if (!Float.isFinite(volume) || volume < 0.0F) {
                throw new IllegalArgumentException("Movement sound volume must be finite and non-negative");
            }
            if (intervalTicks < 1) {
                throw new IllegalArgumentException("Movement sound interval must be positive");
            }
            if (!Double.isFinite(range) || range < 0.0D) {
                throw new IllegalArgumentException("Movement sound range must be finite and non-negative");
            }
        }

        MovementCue withSound(SoundEvent replacement) {
            return new MovementCue(replacement, intervalTicks, range, volume);
        }

        MovementCue withInterval(int replacement) {
            return new MovementCue(sound, replacement, range, volume);
        }

        MovementCue withRange(double replacement) {
            return new MovementCue(sound, intervalTicks, replacement, volume);
        }

        MovementCue withVolume(float replacement) {
            return new MovementCue(sound, intervalTicks, range, replacement);
        }
    }
}
