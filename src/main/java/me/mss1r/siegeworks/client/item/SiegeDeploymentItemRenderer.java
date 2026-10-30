package me.mss1r.siegeworks.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.item.SiegeDeploymentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
*///?} else {
import net.neoforged.api.distmarker.Dist;
//?}
//? if forge {
/*import net.minecraftforge.api.distmarker.OnlyIn;
*///?} else {
import net.neoforged.api.distmarker.OnlyIn;
//?}
//? if forge {
/*import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
//?}

import java.util.IdentityHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class SiegeDeploymentItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float GUI_PITCH = 30.0F;
    private static final float GUI_YAW = -45.0F;

    private final Map<EntityType<?>, Entity> previewEntities = new IdentityHashMap<>();

    private SiegeDeploymentItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions clientExtensions() {
        return new IClientItemExtensions() {
            private SiegeDeploymentItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SiegeDeploymentItemRenderer();
                }
                return renderer;
            }
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof SiegeDeploymentItem deploymentItem)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        EntityType<?> type = deploymentItem.entityType();
        if (level == null || type == null) {
            return;
        }

        Entity preview = previewEntities.get(type);
        if (preview == null || preview.level() != level) {
            preview = type.create(level);
            if (preview == null) {
                return;
            }
            previewEntities.put(type, preview);
        }

        prepareStaticPreview(preview);
        PreviewTransform transform = transformFor(type);

        poseStack.pushPose();
        applyDisplayTransform(poseStack, context, transform);
        renderEntity(preview, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static void prepareStaticPreview(Entity entity) {
        entity.tickCount = 0;
        entity.setYRot(0.0F);
        entity.yRotO = 0.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;
        entity.setYHeadRot(0.0F);
        entity.setYBodyRot(0.0F);
        if (entity instanceof AbstractSiegeEntity siege) {
            siege.setTrackedYaw(0.0F);
            siege.setTrackedPitch(0.0F);
        }
    }

    private static void applyDisplayTransform(PoseStack poseStack, ItemDisplayContext context,
                                              PreviewTransform transform) {
        float contextScale = switch (context) {
            case GUI, FIXED, NONE -> 1.35F;
            case GROUND -> 0.65F;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.72F;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> 1.05F;
            case HEAD -> 0.75F;
        };

        boolean onGround = context == ItemDisplayContext.GROUND;
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        float rootY = onGround ? transform.groundLift() : firstPerson ? 0.72F : 0.5F;
        poseStack.translate(0.5F, rootY, 0.5F);

        if (context == ItemDisplayContext.GUI
                || context == ItemDisplayContext.FIXED
                || context == ItemDisplayContext.NONE) {
            poseStack.mulPose(Axis.XP.rotationDegrees(GUI_PITCH));
        }
        if (!onGround) {
            poseStack.mulPose(Axis.YP.rotationDegrees(GUI_YAW));
        }

        float scale = transform.scale() * contextScale;
        if (context == ItemDisplayContext.GUI) {
            scale *= transform.guiScale();
        }
        poseStack.scale(scale, scale, scale);
        poseStack.translate(transform.xOffset(), onGround ? 0.0F : -transform.yCenter(),
                transform.zOffset());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderEntity(Entity entity, PoseStack poseStack,
                                     MultiBufferSource buffers, int packedLight) {
        EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        renderer.render(entity, 0.0F, 0.0F, poseStack, buffers, packedLight);
    }

    private static PreviewTransform transformFor(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        String path = id.getPath();
        return switch (path) {
            case "arcballista" -> new PreviewTransform(0.155F, 0.70F, 0.0F, 0.05F, 1.18F, 0.27F);
            case "culverin" -> new PreviewTransform(0.205F, 0.85F, 0.0F, 0.10F, 1.15F, 0.27F);
            case "serpentine" -> new PreviewTransform(0.175F, 0.85F, 0.0F, 0.12F, 1.15F, 0.27F);
            case "tower_crossbow" -> new PreviewTransform(0.145F, 0.95F, 0.0F, 0.0F, 1.15F, 0.29F);
            case "hwacha" -> new PreviewTransform(0.135F, 1.45F, -0.05F, 0.0F, 1.25F, 0.31F);
            case "onager" -> new PreviewTransform(0.180F, 1.50F, 0.0F, 0.0F, 1.05F, 0.31F);
            case "mantlet" -> new PreviewTransform(0.120F, 2.10F, 0.0F, -0.05F, 1.00F, 0.35F);
            case "mons_meg" -> new PreviewTransform(0.115F, 1.30F, -0.05F, 0.0F, 1.00F, 0.31F);
            case "battering_ram" -> new PreviewTransform(0.080F, 2.80F, 0.0F, 0.0F, 1.00F, 0.37F);
            case "trebuchet" -> new PreviewTransform(0.070F, 4.00F, 0.0F, 0.0F, 0.92F, 0.41F);
            case "siege_tower" -> new PreviewTransform(0.042F, 8.60F, 0.0F, 0.0F, 1.00F, 0.45F);
            default -> new PreviewTransform(0.150F, 1.50F, 0.0F, 0.0F, 1.00F, 0.31F);
        };
    }

    private record PreviewTransform(float scale, float yCenter, float xOffset, float zOffset,
                                    float guiScale, float groundLift) {
    }
}
