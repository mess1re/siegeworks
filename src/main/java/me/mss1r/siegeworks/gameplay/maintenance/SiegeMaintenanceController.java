package me.mss1r.siegeworks.gameplay.maintenance;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.event.SiegeMaintenanceCheckEvent;
import me.mss1r.siegeworks.event.SiegeMaintenanceCompletedEvent;
import me.mss1r.siegeworks.event.SiegeMaintenanceEvents;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.network.MaintenanceActionC2SPayload;
import me.mss1r.siegeworks.network.OpenMaintenanceS2CPayload;
import me.mss1r.siegeworks.network.SiegeworksNetworking;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SiegeMaintenanceController {
    private static final int HIT_COOLDOWN_TICKS = 5;

    public interface Host {
        AbstractSiegeEntity siege();

        void setDismantling(boolean dismantling);

        void setDismantleProgress(int progress);

        void stopMovement();

        void playNearbySound(ServerLevel level, SoundEvent sound, double range, float volume);
    }

    private final Host host;
    private int hitCooldown;

    public SiegeMaintenanceController(Host host) {
        this.host = host;
    }

    public void tick() {
        if (hitCooldown > 0) {
            hitCooldown--;
        }
    }

    public void openScreen(ServerPlayer player) {
        AbstractSiegeEntity siege = siege();
        SiegeMaintenanceData.MaintenanceRecipe recipe = SiegeMaintenanceData.forSiege(siege);
        SiegeworksNetworking.sendToPlayer(player, new OpenMaintenanceS2CPayload(
                siege.getId(),
                siege.getDisplayName().getString(),
                Mth.ceil(siege.getHealth()),
                Mth.ceil(siege.getMaxHealth()),
                !recipe.isEmpty(),
                siege.isDismantling(),
                siege.getDismantleProgress(),
                SiegeMaintenanceData.dismantleRequiredHits(siege),
                SiegeMaintenanceData.formatItems(SiegeMaintenanceData.repairCost(siege)),
                SiegeMaintenanceData.formatItems(SiegeMaintenanceData.dismantleRefund(siege))
        ));
    }

    public void handleAction(ServerPlayer player, int action) {
        AbstractSiegeEntity siege = siege();
        if (player.distanceToSqr(siege) > 64.0D) {
            return;
        }

        SiegeMaintenanceCheckEvent.Action checkedAction = switch (action) {
            case MaintenanceActionC2SPayload.ACTION_REPAIR -> SiegeMaintenanceCheckEvent.Action.REPAIR;
            case MaintenanceActionC2SPayload.ACTION_START_DISMANTLE -> SiegeMaintenanceCheckEvent.Action.START_DISMANTLE;
            case MaintenanceActionC2SPayload.ACTION_CANCEL_DISMANTLE -> SiegeMaintenanceCheckEvent.Action.CANCEL_DISMANTLE;
            default -> null;
        };
        if (checkedAction != null && !SiegeAccess.allows(player, siege,
                checkedAction == SiegeMaintenanceCheckEvent.Action.REPAIR
                        ? SiegeAccess.Action.REPAIR
                        : SiegeAccess.Action.DISMANTLE)) {
            player.displayClientMessage(Component.translatable("message.siegeworks.access.denied"), true);
            return;
        }
        if (checkedAction != null) {
            SiegeMaintenanceCheckEvent checkEvent = new SiegeMaintenanceCheckEvent(player, siege, checkedAction);
            SiegeMaintenanceEvents.CHECK.invoker().check(checkEvent);
            if (!checkEvent.allowed()) {
                return;
            }
        }

        switch (action) {
            case MaintenanceActionC2SPayload.ACTION_REPAIR -> repairFromMenu(player);
            case MaintenanceActionC2SPayload.ACTION_START_DISMANTLE -> startDismantling(player);
            case MaintenanceActionC2SPayload.ACTION_CANCEL_DISMANTLE -> cancelDismantling(player);
            case MaintenanceActionC2SPayload.ACTION_REFRESH -> {
            }
            default -> {
                return;
            }
        }

        if (!siege.isRemoved()) {
            openScreen(player);
        }
    }

    public void handleHammerHit(Player player, ServerLevel level) {
        performHit(player, level, null);
    }

    public boolean needsRepair() {
        AbstractSiegeEntity siege = siege();
        return !siege.isDismantling()
                && siege.getHealth() < siege.getMaxHealth()
                && !SiegeMaintenanceData.forSiege(siege).isEmpty();
    }

    public boolean hasRepairMaterials(Container materials) {
        return needsRepair() && MaintenanceMaterials.has(materials, SiegeMaintenanceData.repairCost(siege()));
    }

    public boolean repairFromWorker(LivingEntity worker, Container materials) {
        AbstractSiegeEntity siege = siege();
        if (!needsRepair() || !SiegeAccess.allows(worker, siege, SiegeAccess.Action.REPAIR)) {
            return false;
        }

        List<Material> cost = SiegeMaintenanceData.repairCost(siege);
        if (cost.isEmpty() || !MaintenanceMaterials.has(materials, cost)) {
            return false;
        }

        MaintenanceMaterials.consume(materials, cost);
        MinecraftVersionCompat.damageHeldItem(worker.getMainHandItem(), 1, worker, InteractionHand.MAIN_HAND);
        siege.setHealth(siege.getMaxHealth());
        playRepairEffects();
        return true;
    }

    public boolean canStartAutomatedDismantling(LivingEntity worker) {
        AbstractSiegeEntity siege = siege();
        return !siege.isDismantling()
                && !SiegeMaintenanceData.forSiege(siege).isEmpty()
                && siege.getPassengers().stream().allMatch(passenger -> passenger == worker)
                && SiegeAccess.allows(worker, siege, SiegeAccess.Action.DISMANTLE);
    }

    public boolean startAutomatedDismantling(LivingEntity worker) {
        if (!canStartAutomatedDismantling(worker)) {
            return false;
        }
        beginDismantling();
        return true;
    }

    public void cancelAutomatedDismantling() {
        if (siege().isDismantling()) {
            host.setDismantling(false);
        }
    }

    public boolean performAutomatedDismantleHit(LivingEntity worker, @Nullable Container refundTarget) {
        return siege().level() instanceof ServerLevel serverLevel
                && performHit(worker, serverLevel, refundTarget);
    }

    private void repairFromMenu(ServerPlayer player) {
        AbstractSiegeEntity siege = siege();
        if (siege.isDismantling()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.cancel_first"), true);
            return;
        }
        if (siege.getHealth() >= siege.getMaxHealth()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.already_repaired"), true);
            return;
        }

        List<Material> cost = SiegeMaintenanceData.repairCost(siege);
        if (cost.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.no_recipe"), true);
            return;
        }
        if (!player.isCreative() && !MaintenanceMaterials.has(player, cost)) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.not_enough"), true);
            return;
        }

        int repairedHealth = Mth.ceil(siege.getMaxHealth() - siege.getHealth());
        if (!player.isCreative()) {
            MaintenanceMaterials.consume(player, cost);
            MinecraftVersionCompat.damageHeldItem(player.getMainHandItem(), 1, player, InteractionHand.MAIN_HAND);
        }

        siege.setHealth(siege.getMaxHealth());
        SiegeMaintenanceEvents.COMPLETED.invoker().completed(new SiegeMaintenanceCompletedEvent(
                player, siege, SiegeMaintenanceCompletedEvent.Action.REPAIR, repairedHealth));
        playRepairEffects();
    }

    private void playRepairEffects() {
        AbstractSiegeEntity siege = siege();
        if (siege.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    siege.getX(), siege.getY() + siege.getBbHeight() * 0.5D, siege.getZ(),
                    12, 0.6D, 0.8D, 0.6D, 0.05D);
            host.playNearbySound(serverLevel, SoundEvents.ANVIL_USE, 18.0D, 0.5F);
        }
    }

    private void startDismantling(ServerPlayer player) {
        AbstractSiegeEntity siege = siege();
        if (SiegeMaintenanceData.forSiege(siege).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.no_recipe"), true);
            return;
        }
        if (!siege.getPassengers().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.passengers"), true);
            return;
        }

        beginDismantling();
        player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.dismantle_started"), true);
    }

    private void beginDismantling() {
        host.setDismantling(true);
        host.setDismantleProgress(0);
        host.stopMovement();
    }

    private void cancelDismantling(ServerPlayer player) {
        if (!siege().isDismantling()) {
            return;
        }
        host.setDismantling(false);
        player.displayClientMessage(Component.translatable("message.siegeworks.maintenance.dismantle_cancelled"), true);
    }

    private boolean performHit(LivingEntity worker, ServerLevel serverLevel, @Nullable Container refundTarget) {
        AbstractSiegeEntity siege = siege();
        if (!siege.isDismantling() || hitCooldown > 0) {
            return false;
        }

        if (!SiegeAccess.allows(worker, siege, SiegeAccess.Action.DISMANTLE)) {
            return false;
        }
        if (worker instanceof ServerPlayer serverPlayer) {
            SiegeMaintenanceCheckEvent checkEvent = new SiegeMaintenanceCheckEvent(
                    serverPlayer, siege, SiegeMaintenanceCheckEvent.Action.DISMANTLE_HIT);
            SiegeMaintenanceEvents.CHECK.invoker().check(checkEvent);
            if (!checkEvent.allowed()) {
                return false;
            }
        }

        if (SiegeMaintenanceData.forSiege(siege).isEmpty()) {
            host.setDismantling(false);
            return false;
        }

        hitCooldown = HIT_COOLDOWN_TICKS;
        if (!(worker instanceof Player player) || !player.isCreative()) {
            MinecraftVersionCompat.damageHeldItem(worker.getMainHandItem(), 1, worker, InteractionHand.MAIN_HAND);
        }

        int progress = siege.getDismantleProgress() + 1;
        host.setDismantleProgress(progress);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                siege.getX(), siege.getY() + siege.getBbHeight() * 0.5D, siege.getZ(),
                4, 0.4D, 0.5D, 0.4D, 0.05D);
        serverLevel.playSound(null, siege.blockPosition(), SoundEvents.ANVIL_PLACE,
                SoundSource.BLOCKS, 0.65F, 1.25F);

        if (progress >= SiegeMaintenanceData.dismantleRequiredHits(siege)) {
            completeDismantling(serverLevel, worker, refundTarget);
        }
        return true;
    }

    private void completeDismantling(ServerLevel serverLevel, LivingEntity worker,
                                     @Nullable Container refundTarget) {
        AbstractSiegeEntity siege = siege();
        SiegeMaintenanceData.dismantleRefund(siege).forEach(material ->
                MaintenanceMaterials.returnOrDrop(siege, serverLevel, refundTarget, material));
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                siege.getX(), siege.getY() + siege.getBbHeight() * 0.5D, siege.getZ(),
                16, 0.8D, 0.8D, 0.8D, 0.02D);
        host.playNearbySound(serverLevel, SoundEvents.ANVIL_BREAK, 24.0D, 0.6F);
        if (worker instanceof ServerPlayer serverPlayer) {
            SiegeMaintenanceEvents.COMPLETED.invoker().completed(new SiegeMaintenanceCompletedEvent(
                    serverPlayer, siege, SiegeMaintenanceCompletedEvent.Action.DISMANTLE, 0));
        }
        SiegeDeploymentLimits.unregister(siege);
        siege.discard();
    }

    private AbstractSiegeEntity siege() {
        return host.siege();
    }
}
