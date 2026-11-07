package me.mss1r.siegeworks.entity.projectile;

import me.mss1r.siegeworks.gameplay.advancement.SiegeAdvancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A solid round ball uses the same flight and penetration solver as artillery. */
public final class FirearmProjectile extends CannonProjectile {
    private BlockState impactBlock;
    private boolean firedFromArquebus;

    public FirearmProjectile(EntityType<? extends FirearmProjectile> type, Level level) { super(type, level); }

    public void setFiredFromArquebus(boolean firedFromArquebus) {
        this.firedFromArquebus = firedFromArquebus;
    }

    @Override
    protected EntityImpact impactTarget(Entity hit, Vec3 at) {
        var target = livingTarget(hit);
        var player = firedFromArquebus && getOwner() instanceof ServerPlayer shooter ? shooter : null;
        boolean qualifies = player != null && target != null && wearsArmour(target)
                && SiegeAdvancement.isOpponent(player, target);
        var result = super.impactTarget(hit, at);
        if (qualifies && result != null && result.damageApplied()) SiegeAdvancement.PAVIA.award(player);
        return result;
    }

    private static boolean wearsArmour(LivingEntity target) {
        for (var stack : target.getArmorSlots()) {
            if (stack.getItem() instanceof ArmorItem) return true;
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("FiredFromArquebus", firedFromArquebus);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        firedFromArquebus = tag.getBoolean("FiredFromArquebus");
    }

    @Override protected void onHitBlock(BlockHitResult hit) {
        impactBlock = level().getBlockState(hit.getBlockPos());
        super.onHitBlock(hit);
    }

    @Override protected void onHitEntity(EntityHitResult hit) {
        impactBlock = null;
        super.onHitEntity(hit);
    }

    @Override protected void playPenetrationReport(ServerLevel level, Vec3 impact) {
        playImpactReport(level, impact, .45F, 1.25F);
    }

    @Override protected void playImpactReport(ServerLevel level, Vec3 impact, float volume, float pitch) {
        var sound = impactBlock == null ? SoundEvents.PLAYER_ATTACK_STRONG : impactBlock.getSoundType().getHitSound();
        level.playSound(null, impact.x, impact.y, impact.z, sound, SoundSource.BLOCKS, .55F, 1.2F);
        if (impactBlock != null && !impactBlock.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, impactBlock),
                    impact.x, impact.y, impact.z, 5, .035, .035, .035, .035);
        }
    }
}
