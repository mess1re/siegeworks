package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.ballistics.ProjectileImpactEffects;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ScattershotProjectile extends CannonProjectile {
    private static final String TAG_STONE = "StonePellet";
    private static final String TAG_REPORT_IMPACT = "ReportImpact";
    private static final EntityDataAccessor<Boolean> STONE =
            SynchedEntityData.defineId(ScattershotProjectile.class, EntityDataSerializers.BOOLEAN);
    private boolean reportImpact = true;

    public ScattershotProjectile(EntityType<? extends ScattershotProjectile> type, Level level) {
        super(type, level);
    }

    public ScattershotProjectile(EntityType<? extends ScattershotProjectile> type,
                                 LivingEntity shooter, Level level) {
        super(type, shooter, level);
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
        data.define(STONE, false);
    }

    public boolean isStonePellet() {
        return entityData.get(STONE);
    }

    public void setStonePellet(boolean stone) {
        entityData.set(STONE, stone);
    }

    public void setReportImpact(boolean reportImpact) {
        this.reportImpact = reportImpact;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_STONE, isStonePellet());
        tag.putBoolean(TAG_REPORT_IMPACT, reportImpact);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStonePellet(tag.getBoolean(TAG_STONE));
        reportImpact = !tag.contains(TAG_REPORT_IMPACT) || tag.getBoolean(TAG_REPORT_IMPACT);
    }

    @Override
    protected void playImpactReport(ServerLevel serverLevel, Vec3 impact, float volume, float pitch) {
        ProjectileImpactEffects.spawnScattershotImpactParticles(
                serverLevel, impact, isStonePellet(), impactOutwardDirection());
        if (!reportImpact) {
            return;
        }

        if (isStonePellet()) {
            serverLevel.playSound(null, impact.x, impact.y, impact.z,
                    SiegeworksSounds.IMPACT_STONE_LAYER.get(), SoundSource.BLOCKS, 0.8F, 1.15F);
            serverLevel.playSound(null, impact.x, impact.y, impact.z,
                    SiegeworksSounds.IMPACT_BODY_LAYER.get(), SoundSource.BLOCKS, 0.35F, 1.2F);
        } else {
            serverLevel.playSound(null, impact.x, impact.y, impact.z,
                    SiegeworksSounds.IMPACT_METAL_LAYER.get(), SoundSource.BLOCKS, 0.65F, 1.2F);
        }
    }
}
