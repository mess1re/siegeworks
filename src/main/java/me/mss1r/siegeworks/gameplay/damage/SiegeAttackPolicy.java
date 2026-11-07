package me.mss1r.siegeworks.gameplay.damage;

import me.mss1r.siegeworks.data.profile.SiegeDamageRules;
import me.mss1r.siegeworks.entity.base.SiegeProjectile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

public final class SiegeAttackPolicy {
    private SiegeAttackPolicy() {
    }

    public static Result evaluate(DamageSource source, float amount, SiegeDamageRules rules) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return Result.apply(amount);
        }

        Entity directAttacker = source.getDirectEntity();
        if (!isAllowed(attacker, directAttacker, source, rules)) {
            return Result.reject();
        }

        return Result.apply(amount);
    }

    private static boolean isAllowed(Entity attacker, Entity directAttacker, DamageSource source,
                                     SiegeDamageRules rules) {
        if (source.is(me.mss1r.axiomata.ballistics.damage.StructureDamage.BOLTS)
                || source.is(me.mss1r.axiomata.ballistics.damage.StructureDamage.SOLID_PROJECTILES)
                || source.is(me.mss1r.axiomata.ballistics.damage.StructureDamage.FRAGMENTS)
                || source.is(me.mss1r.axiomata.ballistics.damage.StructureDamage.LIGHT_PROJECTILES)) return true;

        if (directAttacker instanceof SiegeProjectile) {
            return true;
        }
        if (directAttacker instanceof me.mss1r.siegeworks.entity.base.AbstractSiegeEntity
                && source.is(me.mss1r.axiomata.ballistics.ProjectileCombat.PHYSICAL_IMPACT)) return true;

        if (directAttacker != null && rules.allowsEntity(entityId(directAttacker))) {
            return true;
        }

        if (rules.allowsEntity(entityId(attacker))) {
            return true;
        }

        if ((directAttacker instanceof AbstractArrow || directAttacker instanceof SiegeProjectile)
                && rules.allowsDamageType("projectile")) {
            return true;
        }

        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
                && rules.allowsDamageType("explosion")) {
            return true;
        }

        if (attacker instanceof LivingEntity livingAttacker) {
            ItemStack heldItem = livingAttacker.getMainHandItem();
            if (!heldItem.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem()).toString();
                return rules.allowsItem(itemId);
            }
        }

        return false;
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public record Result(boolean accepted, float amount) {
        private static Result apply(float amount) {
            return new Result(true, amount);
        }

        private static Result reject() {
            return new Result(false, 0.0F);
        }
    }
}
