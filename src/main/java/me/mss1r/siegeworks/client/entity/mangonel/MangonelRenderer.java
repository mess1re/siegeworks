package me.mss1r.siegeworks.client.entity.mangonel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionGeometry;
import me.mss1r.siegeworks.client.rope.RopeRenderer;
import me.mss1r.siegeworks.client.rope.SiegeRopeAnchors;
import me.mss1r.siegeworks.client.entity.TowedSiegeRenderer;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.client.projectile.TrebuchetProjectileRenderer;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class MangonelRenderer extends TowedSiegeRenderer<MangonelEntity> {
    private static final float LOADED_STONE_SCALE = 0.96F;
    private static final float LOADED_GRAPESHOT_SCALE = LOADED_STONE_SCALE * 16.0F / 10.0F;
    private static final double LOADED_STONE_Y_OFFSET = 5.0D / 16.0D;
    private static final double LOADED_GRAPESHOT_Y_OFFSET = LOADED_STONE_Y_OFFSET
            + 3.0D / 32.0D * LOADED_GRAPESHOT_SCALE;

    private static final ResourceLocation ROPE_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/rope.png");
    private static final String ARM_ANCHOR = "rope_arm";
    private static final String DRUM_ANCHOR = "rope_drum";

    private static final double ARM_RADIUS = (25.0D - 9.0D) / 16.0D;
    private static final double ARM_PIVOT_BELOW_DRUM = (9.0D - 12.0D) / 16.0D;
    private static final double ARM_PIVOT_BEHIND_DRUM = (-7.0D - 7.0D) / 16.0D;

    private static final double DRAWN_ANGLE = Math.toRadians(60.0D);
    private static final double DRAWN_TURNS = 405.0D / 360.0D;

    private static final double SLACK_AT_REST = 0.15D;

    private static final double TRAILING_FRACTION = 0.85D;

    private static final double SLACK_TAKE_UP_TURNS = 1.0D / 8.0D;

    private static final double ROPE_LENGTH = spanAt(0.0D) + SLACK_AT_REST;

    private static final double WIND_PER_TURN = (ROPE_LENGTH - spanAt(DRAWN_ANGLE)) / DRAWN_TURNS;

    private static double spanAt(double armAngle) {
        return Math.hypot(ARM_PIVOT_BELOW_DRUM + ARM_RADIUS * Math.cos(armAngle),
                ARM_PIVOT_BEHIND_DRUM + ARM_RADIUS * Math.sin(armAngle));
    }

    public MangonelRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MangonelModel());
        addRenderLayer(new BlockAndItemGeoLayer<MangonelEntity>(this) {
            @Override
            protected BlockState getBlockForBone(GeoBone bone, MangonelEntity mangonel) {
                if (!"load".equals(bone.getName()) || mangonel.getWindingTime() > 0) {
                    return null;
                }
                return SiegeAmmo.projectileBlockState(mangonel.getAmmoLoaded());
            }

            @Override
            protected void renderBlockForBone(PoseStack poseStack, GeoBone bone, BlockState state,
                                              MangonelEntity mangonel, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, int packedOverlay) {
                boolean grapeshot = SiegeAmmo.isGrapeshotAmmoKey(mangonel.getAmmoLoaded());
                poseStack.pushPose();
                double yOffset = grapeshot ? LOADED_GRAPESHOT_Y_OFFSET : LOADED_STONE_Y_OFFSET;
                float scale = grapeshot ? LOADED_GRAPESHOT_SCALE : LOADED_STONE_SCALE;
                poseStack.translate(0.0D, yOffset, 0.0D);
                poseStack.scale(scale, scale, scale);
                if (SiegeAmmo.isFireAmmoKey(mangonel.getAmmoLoaded())) {
                    // The pot sits bottom-down in the cup with the wick pointing out.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.translate(0.0F, -0.25F, 0.0F);
                    poseStack.scale(MangonelEntity.POT_IN_CUP_SCALE, MangonelEntity.POT_IN_CUP_SCALE,
                            MangonelEntity.POT_IN_CUP_SCALE);
                    poseStack.translate(0.0F, 0.25F, 0.0F);
                    TrebuchetProjectileRenderer.renderFirePot(poseStack, state, bufferSource, packedLight, packedOverlay);
                } else {
                    super.renderBlockForBone(poseStack, bone, state, mangonel, bufferSource,
                            partialTick, packedLight, packedOverlay);
                }
                poseStack.popPose();
            }
        });
    }

    @Override
    protected void renderInModelSpace(PoseStack poseStack, MangonelEntity animatable,
                                      MultiBufferSource bufferSource, float partialTick,
                                      int packedLight) {
        renderWinchRope(poseStack, animatable, bufferSource, packedLight);
    }

    private void renderWinchRope(PoseStack poseStack, MangonelEntity animatable,
                                 MultiBufferSource bufferSource, int packedLight) {
        Optional<Vec3> arm = SiegeRopeAnchors.modelPosition(getGeoModel(), ARM_ANCHOR);
        Optional<Vec3> drum = SiegeRopeAnchors.modelPosition(getGeoModel(), DRUM_ANCHOR);
        // A rope hangs only once both of its ends stand.
        if (arm.isEmpty() || drum.isEmpty() || !ConstructionGeometry.isBoneBuilt(animatable, ARM_ANCHOR)
                || !ConstructionGeometry.isBoneBuilt(animatable, DRUM_ANCHOR)) {
            return;
        }

        Vec3 start = drum.get();
        Vec3 armPosition = arm.get();
        double length = renderedRopeLength(animatable, start.distanceTo(armPosition));
        Vec3 end = releasedEnd(start, armPosition, length);

        poseStack.pushPose();
        RopeRenderer.render(poseStack, bufferSource, ROPE_TEXTURE, start, end, length, packedLight);
        poseStack.popPose();
    }

    private double renderedRopeLength(MangonelEntity animatable, double anchorSpan) {
        SiegeOperationState state = animatable.getOperationState();
        if (state == SiegeOperationState.WINDING) {
            double takeUp = Mth.clamp(woundTurns() / SLACK_TAKE_UP_TURNS, 0.0D, 1.0D);
            return Mth.lerp(takeUp, ROPE_LENGTH, anchorSpan);
        }
        if (state == SiegeOperationState.READY) {
            return anchorSpan;
        }
        return Math.max(0.0D, ROPE_LENGTH - woundLength());
    }

    private static Vec3 releasedEnd(Vec3 drum, Vec3 arm, double length) {
        Vec3 toArm = arm.subtract(drum);
        double reach = toArm.length();
        if (reach <= length || reach < 1.0E-4D) {
            return arm;
        }
        return drum.add(toArm.scale(length * TRAILING_FRACTION / reach));
    }

    private double woundLength() {
        return woundTurns() * WIND_PER_TURN;
    }

    private double woundTurns() {
        return getGeoModel().getBone(DRUM_ANCHOR)
                .map(bone -> Math.abs(bone.getParent() == null ? 0.0F : bone.getParent().getRotX()))
                .map(turnRadians -> (double) turnRadians / Mth.TWO_PI)
                .orElse(0.0D);
    }

}
