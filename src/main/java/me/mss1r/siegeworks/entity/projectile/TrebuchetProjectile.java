package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.IncendiaryFuse;
import me.mss1r.siegeworks.item.PotFilling;
import me.mss1r.siegeworks.data.profile.PotFillingProfile;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectilePhysics;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileBlastResolver;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpacts;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/** Mangonel or trebuchet load: a stone, or a pot once {@link #setFilling} is called. */
public class TrebuchetProjectile extends SiegeProjectile {
    private static final String TAG_TEXTURE_NAME = "TextureName";
    private static final String TAG_FUSE = "Fuse";
    private static final String TAG_FILLING = "Filling";
    /** Tumble speed in flight, used for rendering. */
    public static final float SPIN_DEGREES_PER_TICK = 12.0F;

    protected static final EntityDataAccessor<String> TEXTURE_NAME;
    /** Ticks left on a fire pot's burning fuse, or {@link IncendiaryFuse#UNLIT}. */
    private static final EntityDataAccessor<Integer> FUSE;

    static {
        TEXTURE_NAME = SynchedEntityData.defineId(TrebuchetProjectile.class, EntityDataSerializers.STRING);
        FUSE = SynchedEntityData.defineId(TrebuchetProjectile.class, EntityDataSerializers.INT);
    }

    @Override
    //? if forge {
    /*protected void defineSynchedData() {
        super.defineSynchedData();
        SynchedEntityData data = this.entityData;
    *///?} else {
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var data = builder;
    //?}
        data.define(TEXTURE_NAME, "");
        data.define(FUSE, IncendiaryFuse.UNLIT);
    }

    /** Pot contents, or null for a stone. */
    @Nullable
    private PotFilling filling;

    /** Makes this a pot with the given filling, rendered with or without a wick. */
    public void setFilling(PotFilling filling) {
        this.filling = filling;
        setTextureName(filling.wick() ? SiegeAmmo.AMMO_FIRE : SiegeAmmo.AMMO_POT);
    }

    @Nullable
    public PotFilling filling() {
        return filling;
    }

    /** Lights a fire pot's fuse with {@code ticks} left to burn. */
    public void lightFuse(int ticks) {
        entityData.set(FUSE, Math.max(0, ticks));
    }

    public boolean isLit() {
        return entityData.get(FUSE) >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        // Only pots are ever lit. The client knows the fuse but not the profile.
        if (isRemoved() || !isLit()) {
            return;
        }
        int left = entityData.get(FUSE);
        if (level().isClientSide) {
            // Rotate the wick with the pot's tumble.
            double heading = getYRot() * Mth.DEG_TO_RAD;
            double spin = tickCount * SPIN_DEGREES_PER_TICK * Mth.DEG_TO_RAD;
            Vec3 fuse = new Vec3(Math.sin(heading) * Math.sin(spin), Math.cos(spin),
                    Math.cos(heading) * Math.sin(spin)).scale(drawnSize());
            IncendiaryFuse.sparkle(level(), position().add(fuse.scale(IncendiaryFuse.WICK_TIP)),
                    position().add(fuse.scale(IncendiaryFuse.WICK_BASE)), IncendiaryFuse.burnt(left));
            return;
        }
        if (left <= 0 && level() instanceof ServerLevel serverLevel) {
            burst(serverLevel, position(), getDeltaMovement().length());
            return;
        }
        entityData.set(FUSE, left - 1);
    }

    /** Render size of the load's block. */
    public float drawnSize() {
        return getType() == SiegeworksEntities.TREBUCHET_PROJECTILE.get() ? 0.75F : 0.5F;
    }

    /** Bursts a lit pot at its current position, as on impact. */
    public void burstWhereItIs(ServerLevel serverLevel) {
        burst(serverLevel, position(), getDeltaMovement().length());
    }

    public void setTextureName(String textureName) {
        this.entityData.set(TEXTURE_NAME, textureName);
    }

    public String getTextureName() {
        return this.entityData.get(TEXTURE_NAME);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_TEXTURE_NAME, getTextureName());
        tag.putInt(TAG_FUSE, entityData.get(FUSE));
        if (filling != null) {
            tag.put(TAG_FILLING, filling.save());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_TEXTURE_NAME)) {
            setTextureName(tag.getString(TAG_TEXTURE_NAME));
        }
        entityData.set(FUSE, tag.contains(TAG_FUSE) ? tag.getInt(TAG_FUSE) : IncendiaryFuse.UNLIT);
        if (tag.contains(TAG_FILLING)) {
            filling = PotFilling.load(tag.getCompound(TAG_FILLING));
        } else if (SiegeAmmo.isFireAmmoKey(getTextureName())) {
            // Pots thrown before hand filling existed carry the standard filling.
            filling = PotFilling.standard();
        }
    }

    public TrebuchetProjectile(EntityType<? extends TrebuchetProjectile> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    public TrebuchetProjectile(EntityType<TrebuchetProjectile> cannonProjectile, LivingEntity shooter, Level level) {
        super(cannonProjectile, shooter, level);
    }

    private boolean isPot() {
        return filling != null;
    }

    /** True for a lit pot with a base; any other pot just shatters. */
    private boolean bursts() {
        return filling != null && filling.hasBase() && isLit();
    }

    @Override
    protected double payloadBlastEnergy(ProjectilePhysicsProfile physics) {
        return filling != null ? filling.burst().blastEnergy() : super.payloadBlastEnergy(physics);
    }

    @Override
    protected ProjectilePhysicsProfile.Fire payloadFire(ProjectilePhysicsProfile physics) {
        if (filling == null) {
            return super.payloadFire(physics);
        }
        PotFilling.Burst burst = filling.burst();
        return new ProjectilePhysicsProfile.Fire(burst.fireRadius(), burst.fireChance());
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isPot()) {
            if (bursts()) {
                burst(serverLevel, blockHitResult.getLocation(), getDeltaMovement().length());
            } else {
                shatter(serverLevel, blockHitResult.getLocation());
            }
            return;
        }

        ProjectileImpacts.Drive drive = driveInto(serverLevel, blockHitResult);
        if (drive.passedThrough()) {
            ProjectileImpactEffects.playPenetrationReport(serverLevel, blockHitResult.getLocation(), 2.0f, 1.1f);
            ProjectileImpactEffects.spawnPenetrationParticles(serverLevel, blockHitResult.getLocation(),
                    getPhysicsProfile().diameterOf(this), getDeltaMovement().normalize().reverse());
            return;
        }
        arrive(serverLevel, drive.position(), drive.speed());
        playImpactReport(serverLevel, drive.position(), 4.5f, 0.75f);
        ProjectileBlastResolver.applyShock(serverLevel, drive.position(), this, getPhysicsProfile(), null,
                drive.speed());
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity hitTarget = entityHitResult.getEntity();
        LivingEntity target = livingTarget(hitTarget);
        rememberHitTarget(hitTarget);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double speed = getDeltaMovement().length();
        if (bursts()) {
            burst(serverLevel, entityHitResult.getLocation(), speed);
            return;
        }
        if (isPot()) {
            if (target != null) {
                damageTarget(hitTarget, ProjectilePhysics.entityDamage(getPhysicsProfile(), (float) getBaseDamage(),
                        speed, target));
            }
            shatter(serverLevel, entityHitResult.getLocation());
            return;
        }

        if (target != null) {
            ProjectilePhysicsProfile physics = getPhysicsProfile();
            float damage = ProjectilePhysics.entityDamage(physics, (float) getBaseDamage(), speed, target);
            boolean directDamageApplied = damageTarget(hitTarget, damage);
            playImpactReport(serverLevel, entityHitResult.getLocation(), 4.0f, 0.85f);
            ProjectileBlastResolver.applyShock(serverLevel, entityHitResult.getLocation(), this, physics,
                    directDamageApplied ? target : null, speed);
            if (tryKineticEntityPenetration(target, speed)) {
                return;
            }
        }
        this.discard();
    }

    /** Unlit pot: shatters like a clay pot and its contents are lost. */
    private void shatter(ServerLevel serverLevel, Vec3 impact) {
        serverLevel.playSound(null, impact.x, impact.y, impact.z,
                SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS, 2.5F, 0.8F + random.nextFloat() * 0.2F);
        SiegeParticleEffects.potShatter(serverLevel, impact, SiegeworksBlocks.FIRE_PROJECTILE.get().defaultBlockState());
        this.discard();
    }

    /** Lit pot: spreads fire and sets entities in the splash radius on fire. */
    private void burst(ServerLevel serverLevel, Vec3 impact, double speed) {
        ProjectilePhysicsProfile physics = getPhysicsProfile();
        PotFilling.Burst payload = (filling != null ? filling : PotFilling.standard()).burst();
        arrive(serverLevel, impact, speed);
        Set<LivingEntity> burned = new HashSet<>(
                ProjectileBlastResolver.applyShock(serverLevel, impact, this, physics, null, speed));
        // Burn every entity within the fire radius that has line of sight, regardless of impact speed.
        double splash = payload.fireRadius();
        for (LivingEntity reached : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(impact, impact).inflate(splash), LivingEntity::isAlive)) {
            if (reached.getBoundingBox().distanceToSqr(impact) <= splash * splash
                    && Explosion.getSeenPercent(impact, reached) > 0.0F) {
                burned.add(reached);
            }
        }
        for (LivingEntity target : burned) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), payload.burnSeconds() * 20));
        }
        playBurstSounds(serverLevel, impact, payload.blastEnergy());
        SiegeParticleEffects.incendiaryImpact(serverLevel, impact, Mth.ceil(payload.fireRadius()));
        this.discard();
    }

    /** Shatter and ignition sounds, plus an explosion that gets louder and deeper with more gunpowder. */
    private void playBurstSounds(ServerLevel serverLevel, Vec3 at, double blastEnergy) {
        serverLevel.playSound(null, at.x, at.y, at.z, SoundEvents.DECORATED_POT_SHATTER, SoundSource.PLAYERS,
                2.5F, 0.8F + random.nextFloat() * 0.15F);
        serverLevel.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                4.0F, 0.65F + random.nextFloat() * 0.15F);
        if (!(blastEnergy > 0.0D)) {
            return;
        }
        double fullCharge = PotFillingProfile.current().fullCharge();
        float charge = (float) Mth.clamp(blastEnergy / Math.max(1.0D, fullCharge), 0.0D, 1.0D);
        serverLevel.playSound(null, at.x, at.y, at.z, MinecraftVersionCompat.genericExplodeSound(), SoundSource.PLAYERS,
                2.0F + 2.0F * charge, 1.1F - 0.25F * charge + random.nextFloat() * 0.1F);
        serverLevel.playSound(null, at.x, at.y, at.z, SiegeworksSounds.IMPACT_EXPLOSION_LAYER.get(),
                SoundSource.BLOCKS, 0.6F + 0.6F * charge, 0.95F);
    }

    @Override
    protected SoundEvent getImpactSound() {
        //? if forge {
        /*return SoundEvents.GENERIC_EXPLODE;
        *///?} else {
        return SoundEvents.GENERIC_EXPLODE.value();
        //?}
    }

    private void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.playImpactReport(serverLevel, impact, volume, pitch,
                ProjectileImpactEffects.Style.TREBUCHET,
                getDeltaMovement().lengthSqr() > 1.0E-8D
                        ? getDeltaMovement().normalize().reverse()
                        : new Vec3(0.0D, 1.0D, 0.0D));
    }

    private boolean tryKineticEntityPenetration(LivingEntity target, double speed) {
        double nextSpeed = ProjectilePhysics.remainingEntityPenetrationSpeed(this, target, speed);
        if (nextSpeed <= 0.0D) {
            return false;
        }

        Vec3 direction = getDeltaMovement().normalize();
        setDeltaMovement(direction.scale(nextSpeed));
        setBaseDamage(Math.max(1.0, getBaseDamage() * (nextSpeed / speed)));
        setPos(getX() + direction.x * 0.35, getY() + direction.y * 0.35, getZ() + direction.z * 0.35);
        return true;
    }
}
