package me.mss1r.siegeworks.item;

import me.mss1r.axiomata.blueprint.api.construction.ConstructionTarget;
import me.mss1r.axiomata.collision.CollidableStructure;
import me.mss1r.axiomata.collision.StructurePlacement;
import me.mss1r.siegeworks.client.item.SiegeDeploymentItemRenderer;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?}

public class SiegeDeploymentItem extends Item {
    private final Supplier<? extends EntityType<?>> typeSupplier;

    public SiegeDeploymentItem(Supplier<? extends EntityType<?>> typeSupplier, Properties properties) {
        super(properties);
        this.typeSupplier = Objects.requireNonNull(typeSupplier, "typeSupplier");
    }

    @Override
    public final InteractionResult useOn(UseOnContext context) {
        ConstructionTarget target = context.getPlayer() == null ? null : aimedTarget(context.getPlayer());
        if (target == null || target.structure() == null) {
            target = ConstructionTarget.of(new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                    context.getClickedPos(), context.isInside()));
        } else {
            context = new UseOnContext(context.getPlayer(), context.getHand(),
                    new BlockHitResult(target.location(), target.face(), target.pos(), false));
        }
        return deploy(context, target);
    }

    @Override
    public final InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ConstructionTarget target = aimedTarget(player);
        ItemStack stack = player.getItemInHand(hand);
        if (target == null || target.structure() == null) return InteractionResultHolder.pass(stack);
        // Retain the existing validation/configuration hooks (notably relocated ladders) for both surfaces.
        UseOnContext context = new UseOnContext(player, hand,
                new BlockHitResult(target.location(), target.face(), target.pos(), false));
        return new InteractionResultHolder<>(deploy(context, target), stack);
    }

    private static ConstructionTarget aimedTarget(Player player) {
        return ConstructionTarget.pick(player,
                //? if forge {
                /*player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()),
                *///?} else {
                player.blockInteractionRange(),
                //?}
                1.0F, false);
    }

    private InteractionResult deploy(UseOnContext context, ConstructionTarget target) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player != null && (!serverLevel.mayInteract(player, target.pos())
                || !player.mayUseItemAt(target.pos(), target.face(), context.getItemInHand()))) {
            return InteractionResult.FAIL;
        }

        EntityType<?> type = entityType();
        InteractionResult customValidation = validatePlacement(serverLevel, context, type);
        if (customValidation != InteractionResult.SUCCESS) {
            return customValidation;
        }

        SiegeDeploymentLimits.Deployment deployment = SiegeDeploymentLimits.resolve(
                serverLevel, context.getItemInHand(), context.getPlayer());
        if (!SiegeDeploymentLimits.canDeployWithFeedback(
                serverLevel, type, deployment, context.getPlayer())) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos().above();
        Entity entity = type.create(serverLevel);
        if (entity == null) {
            return InteractionResult.PASS;
        }

        float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        applySpawnRotation(entity, yaw);
        if (entity instanceof AbstractSiegeEntity siege && deployment != null) {
            siege.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        }
        configureSpawnedEntity(entity, context);
        if (target.structure() != null) {
            if (!(entity instanceof CollidableStructure candidate)) return InteractionResult.FAIL;
            Vec3 hit = target.location();
            var placed = StructurePlacement.findOnTop(candidate, target.structure(), hit.x, hit.y, hit.z, yaw);
            if (placed.isEmpty()) return InteractionResult.FAIL;
            var at = placed.get().transform();
            entity.moveTo(at.x(), at.y(), at.z(), yaw, 0.0F);
            applySpawnRotation(entity, yaw);
            if (!StructurePlacement.isClear((Entity & CollidableStructure) entity, candidate.collisionTransform())) {
                return InteractionResult.FAIL;
            }
        }
        if (!serverLevel.addFreshEntity(entity)) {
            return InteractionResult.FAIL;
        }
        if (entity instanceof AbstractSiegeEntity siege) {
            SiegeDeploymentLimits.register(siege);
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    public final EntityType<?> entityType() {
        return Objects.requireNonNull(typeSupplier.get(), "Supplied siege entity type");
    }

    protected InteractionResult validatePlacement(ServerLevel level, UseOnContext context,
                                                  EntityType<?> type) {
        return InteractionResult.SUCCESS;
    }

    protected void configureSpawnedEntity(Entity entity, UseOnContext context) {
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SiegeDeploymentItemRenderer.clientExtensions());
    }

    private static void applySpawnRotation(Entity entity, float yaw) {
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
        entity.yRotO = yaw;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;

        if (entity instanceof AbstractSiegeEntity siege) {
            siege.setTrackedYaw(yaw);
            siege.setTrackedPitch(0.0F);
        }
    }
}
