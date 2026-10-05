package me.mss1r.siegeworks.client.entity.trebuchet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Optional;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.client.rope.RopeRenderer;
import me.mss1r.siegeworks.client.rope.SiegeRopeAnchors;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.client.projectile.TrebuchetProjectileRenderer;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import me.mss1r.siegeworks.client.entity.SiegeConstructionRenderer;

public class TrebuchetRenderer extends SiegeConstructionRenderer<TrebuchetEntity> {
    private static final float LOADED_STONE_SCALE = 1.44F;
    private static final float LOADED_GRAPESHOT_SCALE = LOADED_STONE_SCALE * 16.0F / 10.0F;
    private static final double LOADED_GRAPESHOT_Y_OFFSET = 3.0D / 32.0D * LOADED_GRAPESHOT_SCALE;

    private static final ResourceLocation ROPE_TEXTURE =
            MinecraftVersionCompat.id(Siegeworks.MOD_ID, "textures/entity/rope.png");
    private static final String BEAM_ANCHOR = "rope_beam";
    private static final String GUIDE_ANCHOR = "rope_guide";
    private static final String DRUM_ANCHOR = "rope_drum";

    private static final double DRUM_SPAN_SLACK = 0.02D;
    private static final double BEAM_SPAN_SLACK = 0.4D;

    private static final double SLACK_TAKE_UP_TURNS = 0.4D;

    public TrebuchetRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TrebuchetModel());
        addRenderLayer(new BlockAndItemGeoLayer<TrebuchetEntity>(this) {
            @Override
            protected BlockState getBlockForBone(GeoBone bone, TrebuchetEntity trebuchet) {
                if (!"projectile".equals(bone.getName()) || trebuchet.getWindingTime() > 0) {
                    return null;
                }
                return SiegeAmmo.projectileBlockState(trebuchet.getAmmoLoaded());
            }

            @Override
            protected void renderBlockForBone(PoseStack poseStack, GeoBone bone, BlockState state,
                                              TrebuchetEntity trebuchet, MultiBufferSource bufferSource,
                                              float partialTick, int packedLight, int packedOverlay) {
                boolean grapeshot = SiegeAmmo.isGrapeshotAmmoKey(trebuchet.getAmmoLoaded());
                poseStack.pushPose();
                boolean pot = SiegeAmmo.isFireAmmoKey(trebuchet.getAmmoLoaded());
                float scale = pot ? TrebuchetEntity.POT_IN_SLING_SCALE
                        : grapeshot ? LOADED_GRAPESHOT_SCALE : LOADED_STONE_SCALE;
                if (grapeshot) {
                    poseStack.translate(0.0D, LOADED_GRAPESHOT_Y_OFFSET, 0.0D);
                }
                poseStack.scale(scale, scale, scale);
                if (pot) {
                    // Lay the pot along the sling: base on the pouch bottom (+z), wick toward the arm.
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    poseStack.translate(0.0D, -TrebuchetEntity.POT_SLING_OFFSET, 0.0D);
                    TrebuchetProjectileRenderer.renderFirePot(poseStack, state, bufferSource, packedLight, packedOverlay);
                } else {
                    super.renderBlockForBone(poseStack, bone, state, trebuchet, bufferSource,
                            partialTick, packedLight, packedOverlay);
                }
                poseStack.popPose();
            }
        });
    }

    @Override
    public boolean shouldShowName(TrebuchetEntity animatable) {
        return false;
    }

    @Override
    //? if forge {
    /*public void postRender(PoseStack poseStack, TrebuchetEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
    *///?} else {
    public void postRender(PoseStack poseStack, TrebuchetEntity animatable, BakedGeoModel model,
                           MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                           float partialTick, int packedLight, int packedOverlay, int packedColor) {
    //?}
        //? if forge {
        /*super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
        *///?} else {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, packedColor);
        //?}

        if (!isReRender) {
            poseStack.pushPose();
            enterModelSpace(poseStack, animatable, partialTick);
            renderWinchRope(poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    private void enterModelSpace(PoseStack poseStack, TrebuchetEntity animatable, float partialTick) {
        float ageInTicks = animatable.tickCount + partialTick;
        float bodyRotation = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);

        applyRotations(animatable, poseStack, ageInTicks, bodyRotation, partialTick);
        poseStack.translate(0.0F, 0.01F, 0.0F);
    }

    private void renderWinchRope(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Optional<Vec3> beam = SiegeRopeAnchors.modelPosition(getGeoModel(), BEAM_ANCHOR);
        Optional<Vec3> guide = SiegeRopeAnchors.modelPosition(getGeoModel(), GUIDE_ANCHOR);
        Optional<Vec3> drum = SiegeRopeAnchors.modelPosition(getGeoModel(), DRUM_ANCHOR);
        if (beam.isEmpty() || guide.isEmpty() || drum.isEmpty()) {
            return;
        }

        double spare = slackRemaining();
        Vec3 guidePoint = guide.get();
        Vec3 drumPoint = drum.get();
        Vec3 beamPoint = beam.get();

        poseStack.pushPose();
        RopeRenderer.render(poseStack, bufferSource, ROPE_TEXTURE, drumPoint, guidePoint,
                drumPoint.distanceTo(guidePoint) + DRUM_SPAN_SLACK * spare, packedLight);
        RopeRenderer.render(poseStack, bufferSource, ROPE_TEXTURE, guidePoint, beamPoint,
                guidePoint.distanceTo(beamPoint) + BEAM_SPAN_SLACK * spare, packedLight);
        poseStack.popPose();
    }

    private double slackRemaining() {
        return Math.max(0.0D, 1.0D - woundTurns() / SLACK_TAKE_UP_TURNS);
    }

    private double woundTurns() {
        return getGeoModel().getBone(DRUM_ANCHOR)
                .map(bone -> Math.abs(bone.getParent() == null ? 0.0F : bone.getParent().getRotX()))
                .map(turnRadians -> (double) turnRadians / Mth.TWO_PI)
                .orElse(0.0D);
    }

    @Override
    protected void applyRotations(TrebuchetEntity animatable, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        poseStack.mulPose(Axis.YP.rotationDegrees(-animatable.getYRot() - 180.0F));
    }
}
