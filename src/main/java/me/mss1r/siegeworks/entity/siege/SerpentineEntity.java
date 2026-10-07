package me.mss1r.siegeworks.entity.siege;

import me.mss1r.axiomata.loading.LoadingRequirement;
import me.mss1r.siegeworks.item.ArtilleryPowderCosts;
import me.mss1r.axiomata.collision.CollisionShape;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import me.mss1r.siegeworks.gameplay.audio.SiegeSoundProfile;
import me.mss1r.siegeworks.particle.SiegeParticleEffects;
import me.mss1r.siegeworks.registry.SiegeworksSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SerpentineEntity extends AbstractFieldGunEntity {
    private static final Vec3 DRAFT_MOUNT_OFFSET = new Vec3(0.0D, 0.0D, -5.5D);
    private static final LoadingRequirement[] LOAD_STAGES = createLoadStages(ArtilleryPowderCosts.SERPENTINE);
    private static final FieldGunGeometry GEOMETRY = new FieldGunGeometry(
            12.7D / 16.0D, 6.7D / 16.0D, 4.5D / 16.0D, 30.3D / 16.0D,
            12.5F, 15.0F);
    private static final SiegeSoundProfile SOUND_PROFILE = SiegeSoundProfile.defaults()
            .withMovementSound(SiegeworksSounds.SIEGE_ENGINE_MOVE.get())
            .withFiringSound(SiegeworksSounds.SERPENTINE_FIRE.get())
            .withFiringRange(256.0D)
            .withFiringVolume(2.0F);

    public SerpentineEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 85.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.045D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 285.0D);
    }

    @Override
    protected LoadingRequirement[] getLoadStages() {
        return LOAD_STAGES;
    }

    @Override
    protected FieldGunGeometry getFieldGunGeometry() {
        return GEOMETRY;
    }

    @Override
    protected SiegeParticleEffects.MuzzleProfile getMuzzleProfile() {
        return SiegeParticleEffects.MuzzleProfile.SERPENTINE;
    }

    @Override
    protected CollisionShape getBodyCollisionShape() {
        return GeneratedCollisionShapes.SERPENTINE_BODY;
    }

    @Override
    protected CollisionShape getCannonCollisionShape() {
        return GeneratedCollisionShapes.SERPENTINE_CANNON;
    }

    @Override
    protected boolean supportsDraftMounts() {
        return true;
    }

    @Override
    protected Vec3 getDraftMountOffset() {
        return DRAFT_MOUNT_OFFSET;
    }

    @Override
    public SiegeSoundProfile getSoundProfile() {
        return SOUND_PROFILE;
    }
}
