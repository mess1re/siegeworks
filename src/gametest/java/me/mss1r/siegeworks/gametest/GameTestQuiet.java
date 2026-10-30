package me.mss1r.siegeworks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the world's own monsters out of a long movement test: no new ones spawn, it stays day, and any wandering into
 * the test's area are taken away. A recruit hurt by a passing spider is not what such a test is about.
 */
public final class GameTestQuiet {
    private static final int CLEAR_INTERVAL_TICKS = 10;
    /** The largest movement test area, in blocks across and up. */
    private static final int AREA = 32;
    private static final int AREA_HEIGHT = 24;

    private GameTestQuiet() {
    }

    public static void keep(GameTestHelper helper) {
        var level = helper.getLevel();
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.setDayTime(6000L);
        clear(helper);
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (++tick[0] % CLEAR_INTERVAL_TICKS == 0) clear(helper);
        });
    }

    private static void clear(GameTestHelper helper) {
        // Only the test's own area: other tests' monsters are theirs
        AABB area = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(AREA, AREA_HEIGHT, AREA)))).inflate(2.0D);
        helper.getLevel().getEntitiesOfClass(Mob.class, area,
                        mob -> mob instanceof Enemy && !mob.isPersistenceRequired())
                .forEach(Entity::discard);
    }
}
