package me.mss1r.siegeworks.gameplay.audio;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class ArtilleryLoadingSounds {
    private static final int POWDER = 0;
    private static final int RAM_CHARGE = 1;
    private static final int AMMUNITION = 2;
    private static final int RAM_PROJECTILE = 3;
    private static final int PRIME = 4;

    private ArtilleryLoadingSounds() {
    }

    public static void playStage(ServerLevel level, AbstractSiegeEntity siege, int stage, boolean heavy) {
        float weight = heavy ? 1.2F : 1.0F;
        float pitchScale = heavy ? 0.78F : 1.0F;
        Vec3 position = siege.position().add(0, .8, 0);

        switch (stage) {
            case POWDER -> play(level, siege, SoundEvents.SAND_PLACE,
                    0.72F * weight, 0.82F * pitchScale);
            case RAM_CHARGE, RAM_PROJECTILE -> playRamStroke(level, position, weight, pitchScale);
            case AMMUNITION -> playAmmunition(level, position, weight, pitchScale);
            case PRIME -> play(level, siege, SoundEvents.FLINTANDSTEEL_USE,
                    0.72F * weight, 0.92F * pitchScale);
            default -> {
            }
        }
    }

    public static void playRamStroke(ServerLevel level, Vec3 position, float weight, float pitchScale) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.CHAIN_STEP,
                SoundSource.BLOCKS, .28F * weight, .76F * pitchScale);
    }

    public static void playAmmunition(ServerLevel level, Vec3 position, float weight, float pitchScale) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.LANTERN_PLACE,
                SoundSource.BLOCKS, .9F * weight, pitchScale);
    }

    private static void play(ServerLevel level, AbstractSiegeEntity siege,
                             SoundEvent sound, float volume, float pitch) {
        level.playSound(null, siege.getX(), siege.getY() + 0.8D, siege.getZ(),
                sound, SoundSource.BLOCKS, volume, pitch);
    }
}
