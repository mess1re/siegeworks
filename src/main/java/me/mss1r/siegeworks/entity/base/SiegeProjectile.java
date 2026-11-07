package me.mss1r.siegeworks.entity.base;

import me.mss1r.axiomata.ballistics.BallisticProjectile;
import me.mss1r.axiomata.ballistics.ImpactResolver;
import me.mss1r.axiomata.ballistics.profile.ProjectilePhysicsProfile;
import me.mss1r.axiomata.data.profile.ProfileCatalog;
import me.mss1r.siegeworks.block.IncendiaryPotBlock;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.gameplay.damage.SiegeProjectileCombat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public abstract class SiegeProjectile extends BallisticProjectile {
    private UUID advancementShooter;

    public SiegeProjectile(EntityType<? extends SiegeProjectile> type, Level level) {
        super(type, level);
    }

    public SiegeProjectile(EntityType<? extends SiegeProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override protected ProfileCatalog<ProjectilePhysicsProfile> profileCatalog() {
        return SiegeProfileCatalogs.PROJECTILES;
    }

    @Override protected ImpactResolver impacts() { return SiegeBallisticsEnvironment.IMPACTS; }
    @Override protected String blockAuthorityKey() { return "FiredBySiege"; }
    @Override protected boolean hasBlockAuthority(@Nullable Entity owner) { return owner instanceof AbstractSiegeEntity; }
    @Override protected UUID responsibleUuid(@Nullable Entity owner) { return SiegeBlockBreaker.responsibleUuid(owner); }
    @Override protected Player responsiblePlayer(@Nullable Entity owner) { return SiegeBlockBreaker.responsiblePlayer(owner); }
    @Override protected Player resolvePlayer(ServerLevel level, @Nullable UUID id) { return SiegeBlockBreaker.playerFor(level, id); }
    @Override protected boolean mayHitTarget(Entity target) { return SiegeProjectileCombat.mayHit(this, target); }
    @Override protected boolean mayHitFrom(@Nullable Entity machine, @Nullable Entity attacker, Entity target) {
        return SiegeProjectileCombat.mayHit(machine, attacker, target);
    }
    @Override protected Entity impactAttacker() { return SiegeProjectileCombat.responsibleAttacker(this); }
    @Override protected void onPayloadBurst(ServerLevel level, Vec3 position,
                                             ProjectilePhysicsProfile physics, @Nullable UUID responsible) {
        IncendiaryPotBlock.detonateAround(level, position, payloadFire(physics).radius(), responsible);
    }

    @Override public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (owner instanceof AbstractSiegeEntity engine) setReferenceSpeed(getPhysicsProfile().directHit().referenceSpeed().orElse(engine.getLaunchSpeed() * 20));
    }

    public boolean isFiredBySiege() { return canDamageBlocks(); }

    public void creditShotTo(@Nullable Entity operator) {
        advancementShooter = operator instanceof ServerPlayer player ? player.getUUID() : null;
    }

    @Nullable
    protected ServerPlayer creditedShooter(ServerLevel level) {
        return advancementShooter == null ? null : level.getServer().getPlayerList().getPlayer(advancementShooter);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (advancementShooter != null) tag.putUUID("AdvancementShooter", advancementShooter);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        advancementShooter = tag.hasUUID("AdvancementShooter") ? tag.getUUID("AdvancementShooter") : null;
    }
}
