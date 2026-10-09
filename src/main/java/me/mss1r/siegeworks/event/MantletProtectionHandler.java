package me.mss1r.siegeworks.event;

import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
//?}

import java.util.Comparator;
import java.util.Optional;

public final class MantletProtectionHandler {
    private static final double SEARCH_RANGE = 8.0D;

    private MantletProtectionHandler() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MantletProtectionHandler::onLivingIncomingDamage);
    }

    //? if forge {
    /*private static void onLivingIncomingDamage(LivingHurtEvent event) {
    *///?} else {
    private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
    //?}
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel) || event.getEntity() instanceof MantletEntity) {
            return;
        }

        DamageSource source = event.getSource();
        boolean explosion = source.is(DamageTypes.EXPLOSION) || source.is(DamageTypes.PLAYER_EXPLOSION);
        Entity directEntity = source.getDirectEntity();
        boolean projectile = directEntity instanceof Projectile;
        if (!projectile && !explosion) {
            return;
        }

        Vec3 sourcePos = source.getSourcePosition();
        if (sourcePos == null) {
            return;
        }

        LivingEntity target = event.getEntity();
        AABB searchBox = target.getBoundingBox().inflate(SEARCH_RANGE);
        Optional<MantletEntity> blocker = serverLevel.getEntitiesOfClass(
                        MantletEntity.class,
                        searchBox,
                        mantlet -> mantlet.protectsLine(sourcePos, target)
                )
                .stream()
                .min(Comparator.comparingDouble(mantlet -> mantlet.distanceToSqr(target)));

        if (blocker.isEmpty()) {
            return;
        }

        MantletEntity mantlet = blocker.get();
        boolean siegeImpact = directEntity instanceof SiegeProjectile;
        float incoming = event.getAmount();
        event.setAmount(Math.max(0.0F, incoming * mantlet.getProtectionReduction(siegeImpact, explosion)));
        mantlet.hurt(serverLevel.damageSources().generic(), mantlet.getProtectionWear(incoming, siegeImpact, explosion));
        mantlet.playProtectionFeedback(serverLevel);
    }
}
