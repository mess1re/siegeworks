package me.mss1r.siegeworks.client.entity.siegetower;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionGeometry;
import me.mss1r.siegeworks.client.rope.RopeRenderer;
import me.mss1r.siegeworks.client.rope.SiegeRopeAnchors;
import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.phys.Vec3;
//? if neoforge {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
//?}

public class SiegeTowerRenderer extends TowedSiegeRenderer<SiegeTowerEntity> {
    private static final float BANNER_WIDTH_PIXELS = 32.0F;
    private static final float VANILLA_FLAG_WIDTH_PIXELS = 20.0F;
    private static final float BANNER_SCALE = BANNER_WIDTH_PIXELS / VANILLA_FLAG_WIDTH_PIXELS;
    private static final float[][] BANNER_TOP_LEFT_PIXELS = {
            {-64.0F, 267.0F, -66.0F},
            {64.0F, 267.0F, -66.0F},
            {-64.0F, 267.0F, -15.0F},
            {64.0F, 267.0F, -15.0F}
    };

    private static final ResourceLocation ROPE_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/rope.png");

    private static final String[][] ROPE_ANCHORS = {
            {"rope_drum_a", "rope_shaft_a", "rope_roller_a", "rope_bridge_a"},
            {"rope_drum_b", "rope_shaft_b", "rope_roller_b", "rope_bridge_b"}
    };

    private static final double DRUM_SPAN_SLACK = 0.12D;
    private static final double BRIDGE_SPAN_SLACK = 0.35D;

    private static final double SETTLING_TRAVEL = 0.05D;

    private static final double SAG_CLEARANCE_TRAVEL = 0.08D;

    private final ModelPart bannerFlag;

    public SiegeTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SiegeTowerModel());
        this.bannerFlag = renderManager.bakeLayer(ModelLayers.BANNER).getChild("flag");
    }

    @Override
    public boolean shouldShowName(SiegeTowerEntity animatable) {
        return false;
    }

    @Override
    public boolean shouldRender(SiegeTowerEntity animatable, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(animatable, camera, camX, camY, camZ)
                || camera.isVisible(animatable.getBoundingBox().inflate(12.0D, 4.0D, 12.0D));
    }

    @Override
    public void render(SiegeTowerEntity tower, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(tower, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, tower.yBodyRotO, tower.yBodyRot);
        applyRotations(tower, poseStack, tower.tickCount + partialTick, bodyYaw, partialTick);
        poseStack.translate(0.0D, 0.01D, 0.0D);

        for (int slot = 0; slot < SiegeTowerEntity.BANNER_SLOT_COUNT; slot++) {
            renderBanner(tower, slot, partialTick, poseStack, bufferSource, packedLight);
        }
        poseStack.popPose();
    }

    @Override
    protected void renderInModelSpace(PoseStack poseStack, SiegeTowerEntity animatable,
                                      MultiBufferSource bufferSource, float partialTick,
                                      int packedLight) {
        renderWinchRopes(poseStack, animatable, bufferSource, partialTick, packedLight);
    }

    private void renderWinchRopes(PoseStack poseStack, SiegeTowerEntity animatable,
                                  MultiBufferSource bufferSource, float partialTick, int packedLight) {
        double spare = slackRemaining(animatable, partialTick);

        for (String[] side : ROPE_ANCHORS) {
            Optional<Vec3> drum = SiegeRopeAnchors.modelPosition(getGeoModel(), side[0]);
            Optional<Vec3> shaft = SiegeRopeAnchors.modelPosition(getGeoModel(), side[1]);
            Optional<Vec3> roller = SiegeRopeAnchors.modelPosition(getGeoModel(), side[2]);
            Optional<Vec3> bridge = SiegeRopeAnchors.modelPosition(getGeoModel(), side[3]);
            if (drum.isEmpty() || shaft.isEmpty() || roller.isEmpty() || bridge.isEmpty()) {
                continue;
            }

            Vec3 drumPoint = drum.get();
            Vec3 shaftPoint = shaft.get();
            Vec3 rollerPoint = roller.get();
            Vec3 bridgePoint = bridge.get();

            // A rope hangs only once both of its ends stand.
            if (ConstructionGeometry.isBoneBuilt(animatable, side[0])
                    && ConstructionGeometry.isBoneBuilt(animatable, side[1])) {
                RopeRenderer.render(poseStack, bufferSource, ROPE_TEXTURE, drumPoint, shaftPoint,
                        drumPoint.distanceTo(shaftPoint) + DRUM_SPAN_SLACK * spare, packedLight);
            }
            if (ConstructionGeometry.isBoneBuilt(animatable, side[2])
                    && ConstructionGeometry.isBoneBuilt(animatable, side[3])) {
                double bridgeSlack = BRIDGE_SPAN_SLACK * spare * sagClearance(animatable, partialTick);
                RopeRenderer.render(poseStack, bufferSource, ROPE_TEXTURE, rollerPoint, bridgePoint,
                        rollerPoint.distanceTo(bridgePoint) + bridgeSlack, packedLight);
            }
        }
    }

    private static double sagClearance(SiegeTowerEntity animatable, float partialTick) {
        return Mth.clamp(animatable.getRenderedBridgeProgress(partialTick) / SAG_CLEARANCE_TRAVEL,
                0.0D, 1.0D);
    }

    private static double slackRemaining(SiegeTowerEntity animatable, float partialTick) {
        double travelLeft = Math.abs(animatable.getBridgeTargetProgress()
                - animatable.getRenderedBridgeProgress(partialTick));
        return 1.0D - Mth.clamp(travelLeft / SETTLING_TRAVEL, 0.0D, 1.0D);
    }

    private void renderBanner(SiegeTowerEntity tower, int slot, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = tower.getTowerBanner(slot);
        if (!(stack.getItem() instanceof BannerItem bannerItem)) {
            return;
        }

        float[] topLeft = BANNER_TOP_LEFT_PIXELS[slot];
        poseStack.pushPose();
        poseStack.translate(
                topLeft[0] / 16.0F,
                topLeft[1] / 16.0F,
                (topLeft[2] + BANNER_WIDTH_PIXELS * 0.5F) / 16.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(slot % 2 == 0 ? -90.0F : 90.0F));
        poseStack.scale(BANNER_SCALE, -BANNER_SCALE, -BANNER_SCALE);

        long waveOffset = tower.blockPosition().getX() * 7L
                + tower.blockPosition().getY() * 9L
                + tower.blockPosition().getZ() * 13L
                + slot * 17L;
        float wavePhase = (Math.floorMod(waveOffset + tower.level().getGameTime(), 100L) + partialTick) / 100.0F;
        bannerFlag.x = 0.0F;
        bannerFlag.y = 0.0F;
        bannerFlag.z = 0.0F;
        bannerFlag.xRot = (-0.0125F + 0.01F * Mth.cos(Mth.TWO_PI * wavePhase)) * Mth.PI;
        bannerFlag.yRot = 0.0F;
        bannerFlag.zRot = 0.0F;

        //? if forge {
        /*BannerRenderer.renderPatterns(
                poseStack,
                bufferSource,
                packedLight,
                getPackedOverlay(tower, 0.0F),
                bannerFlag,
                ModelBakery.BANNER_BASE,
                true,
                BannerBlockEntity.createPatterns(
                        bannerItem.getColor(),
                        BannerBlockEntity.getItemPatterns(stack)));
        *///?} else {
        BannerRenderer.renderPatterns(
                poseStack,
                bufferSource,
                packedLight,
                getPackedOverlay(tower, 0.0F, 0.0F),
                bannerFlag,
                ModelBakery.BANNER_BASE,
                true,
                bannerItem.getColor(),
                stack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY));
        //?}
        poseStack.popPose();
    }

}
