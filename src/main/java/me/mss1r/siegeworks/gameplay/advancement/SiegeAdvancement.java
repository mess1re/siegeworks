package me.mss1r.siegeworks.gameplay.advancement;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import me.mss1r.axiomata.blueprint.api.event.BlueprintEvents;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegePlayerAttributionRegistry;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.gameplay.ownership.SiegeTeams;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public enum SiegeAdvancement {
    SIEGE_PLANS("root"),
    FULL_SCALE("full_scale"),
    CAST_TO_SIZE("cast_to_size"),
    GIFT_FOR_A_KING("gift_for_a_king"),
    MUNJONGS_VOLLEY("munjongs_volley"),
    CLEAN_THROUGH("clean_through"),
    REPEL_THE_ASSAULT("repel_the_assault"),
    PAVIA("pavia"),
    FLY_YOUR_COLOURS("fly_your_colours"),
    WAR_WOLF("war_wolf");

    private final ResourceLocation id;

    SiegeAdvancement(String path) {
        id = MinecraftVersionCompat.id(Siegeworks.MOD_ID, path);
    }

    public static void register() {
        BlueprintEvents.CREATED.register(event -> {
            String recipe = BlueprintItem.getRecipeId(event.blueprint());
            if (recipe != null && recipe.startsWith(Siegeworks.MOD_ID + ":")) SIEGE_PLANS.award(event.player());
        });
        BlueprintEvents.BUILT.register(event -> {
            if (event.result() instanceof AbstractSiegeEntity) FULL_SCALE.award(event.worker());
        });
        EntityEvent.LIVING_DEATH.register((target, source) -> {
            if (target.level() instanceof ServerLevel level && source.getDirectEntity() instanceof SingijeonProjectile rocket) {
                HwachaVolleys.recordKill(level, rocket.advancementVolley(), target, source.getEntity());
            }
            return EventResult.pass();
        });
    }

    public void award(Entity actor) {
        if (!(actor instanceof ServerPlayer player)) return;
        var advancement = lookup(player);
        if (advancement != null) player.getAdvancements().award(advancement, "completed");
    }

    public boolean isDone(ServerPlayer player) {
        var advancement = lookup(player);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    //? if <1.21 {
    /*private net.minecraft.advancements.Advancement lookup(ServerPlayer player) {
        return player.server.getAdvancements().getAdvancement(id);
    *///?} else {
    private net.minecraft.advancements.AdvancementHolder lookup(ServerPlayer player) {
        return player.server.getAdvancements().get(id);
    //?}
    }

    public static boolean isOpponent(ServerPlayer player, LivingEntity target) {
        if (target == player || target instanceof AbstractSiegeEntity || player.isAlliedTo(target)) return false;
        var ownerId = SiegePlayerAttributionRegistry.playerOwnerOf(target);
        if (ownerId != null) {
            if (ownerId.equals(player.getUUID())) return false;
            var team = player.getTeam();
            return team == null || !team.getName().equals(SiegeTeams.teamOf(player.server, ownerId));
        }
        return target instanceof Enemy || target instanceof Player
                || target instanceof Mob mob && mob.getTarget() == player;
    }
}
