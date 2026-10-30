package me.mss1r.siegeworks.gameplay.ballistics;

import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TrebuchetProjectile;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Pot fuse. A pot only bursts into fire once lit: on impact, or wherever it is when the fuse runs out (placed, loaded
 * or in flight).
 */
public final class IncendiaryFuse {
    public static final int UNLIT = -1;
    /**
     * Positions along a stone-sized pot load, from its centre in units of load size: the wick tip, and where the wick
     * leaves the pot.
     */
    public static final double WICK_TIP = 1.625D;
    public static final double WICK_BASE = 1.25D;
    private static final double SPARK_SPEED = 0.05D;
    private static final double SPARK_SCATTER = 0.03D;

    private IncendiaryFuse() {
    }

    public static int fullLength() {
        return SiegeworksServerConfig.getIncendiaryFuseTicks();
    }

    public static boolean canStrike(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL);
    }

    /** Plays the lighting sounds at {@code at} and damages the flint and steel like lighting a fire. */
    public static void strike(Player player, InteractionHand hand, Level level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.6F, 1.3F);
        if (!player.getAbilities().instabuild) {
            MinecraftVersionCompat.damageHeldItem(player.getItemInHand(hand), 1, player, hand);
        }
    }

    /**
     * Client: fuse sparks and smoke at the burning point, from the tip at {@code burnt} = 0 to the pot at 1. Sparks fly
     * outward along the wick.
     */
    public static void sparkle(Level level, Vec3 tip, Vec3 pot, double burnt) {
        Vec3 at = tip.lerp(pot, Mth.clamp(burnt, 0.0D, 1.0D));
        Vec3 out = tip.subtract(pot).normalize();
        for (int spark = 0; spark < 3; spark++) {
            level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z,
                    out.x * SPARK_SPEED + scatter(level), out.y * SPARK_SPEED + scatter(level),
                    out.z * SPARK_SPEED + scatter(level));
        }
        if (level.random.nextBoolean()) {
            level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z,
                    out.x * SPARK_SPEED * 0.5D, 0.03D + out.y * SPARK_SPEED * 0.5D, out.z * SPARK_SPEED * 0.5D);
        }
    }

    private static double scatter(Level level) {
        return (level.random.nextDouble() * 2.0D - 1.0D) * SPARK_SCATTER;
    }

    /** Fraction of the fuse burnt: 0 when just lit, 1 when it reaches the pot. */
    public static double burnt(int ticksLeft) {
        return ticksLeft < 0 ? 0.0D : 1.0D - (double) ticksLeft / Math.max(1, fullLength());
    }

    /**
     * Bursts a pot at {@code at} after {@code delayTicks}: immediately, or slightly later so chained pots ripple. Blame
     * goes to the owning engine, otherwise {@code responsible}.
     */
    public static void burst(ServerLevel level, Vec3 at, PotFilling filling, int delayTicks,
                             EntityType<TrebuchetProjectile> type, ResourceLocation profile, double baseDamage,
                             @Nullable Entity owner, @Nullable UUID responsible) {
        TrebuchetProjectile pot = new TrebuchetProjectile(type, level);
        pot.setPhysicsProfile(profile);
        pot.setBaseDamage(baseDamage);
        pot.setFilling(filling);
        if (owner != null) {
            pot.setOwner(owner);
        } else {
            pot.reachBlocksAs(responsible);
        }
        pot.setPos(at.x, at.y, at.z);
        if (delayTicks <= 0) {
            pot.burstWhereItIs(level);
            return;
        }
        pot.setNoGravity(true);
        pot.lightFuse(delayTicks);
        level.addFreshEntity(pot);
    }

    /** Bursts a placed pot using the onager pot's profile. */
    public static void burstPlaced(ServerLevel level, Vec3 at, PotFilling filling, int delayTicks,
                                   @Nullable UUID responsible) {
        burst(level, at, filling, delayTicks, SiegeworksEntities.ONAGER_PROJECTILE.get(),
                ProjectileVariants.ONAGER_FIRE_PROJECTILE,
                SiegeProfileCatalogs.ENGINES.forEntity(SiegeworksEntities.ONAGER_ENTITY.get()).baseDamage(), null,
                responsible);
    }
}
