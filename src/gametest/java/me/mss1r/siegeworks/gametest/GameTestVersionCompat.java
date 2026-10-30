package me.mss1r.siegeworks.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;

final class GameTestVersionCompat {
    private GameTestVersionCompat() {
    }

    static Arrow arrow(ServerLevel level, LivingEntity owner) {
        //? if <1.21 {
        /*return new Arrow(level, owner);
        *///?} else {
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.setOwner(owner);
        return arrow;
        //?}
    }

    static float maxUpStep(Entity entity) {
        //? if <1.21 {
        /*return entity.getStepHeight();
        *///?} else {
        return entity.maxUpStep();
        //?}
    }
}
