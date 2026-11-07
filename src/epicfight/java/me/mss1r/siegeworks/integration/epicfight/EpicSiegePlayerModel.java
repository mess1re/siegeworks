package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import net.minecraft.world.entity.player.Player;

final class EpicSiegePlayerModel {
    private EpicSiegePlayerModel() {}

    static boolean shouldUseVanillaRenderer(Player player, float partialTick) {
        return player.getVehicle() instanceof AbstractSiegeEntity
                || (player.getVehicle() == null && StructureClimbingSystem.getClimbPoseState(
                        player, partialTick).weight() > 0.0F);
    }
}
