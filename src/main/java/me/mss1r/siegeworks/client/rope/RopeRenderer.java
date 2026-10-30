package me.mss1r.siegeworks.client.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class RopeRenderer {
    private static final double TEXTURE_REPEAT_LENGTH = 1.0D;
    private static final float DEFAULT_WIDTH = 1.5F / 16.0F;
    private static final int MAX_SEGMENTS = 64;
    private static final double STRAIGHT_EPSILON = 1.0E-4D;
    private static final Vec3 UPWARD_NORMAL = new Vec3(0.0D, 1.0D, 0.0D);

    private static final double SLACK_DEADBAND = 0.01D;

    private RopeRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                              Vec3 start, Vec3 end, double ropeLength, int packedLight) {
        render(poseStack, bufferSource, texture, start, end, ropeLength, packedLight, DEFAULT_WIDTH);
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                              Vec3 start, Vec3 end, double ropeLength, int packedLight, float width) {
        Vec3 chord = end.subtract(start);
        double span = chord.length();
        if (span < STRAIGHT_EPSILON) {
            return;
        }

        double sag = sagDepth(span, ropeLength);
        int segments = segmentCount(Math.max(span, ropeLength));
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        Vec3[] points = new Vec3[segments + 1];
        for (int index = 0; index <= segments; index++) {
            points[index] = pointAt(start, chord, sag, (double) index / segments);
        }

        for (int plane = 0; plane < 2; plane++) {
            double travelled = 0.0D;
            for (int index = 0; index < segments; index++) {
                Vec3 from = points[index];
                Vec3 to = points[index + 1];
                Vec3 along = to.subtract(from);
                double length = along.length();
                if (length < STRAIGHT_EPSILON) {
                    continue;
                }

                Vec3 offset = ribbonOffset(along.scale(1.0D / length), plane).scale(width * 0.5F);
                float vFrom = (float) (travelled / TEXTURE_REPEAT_LENGTH);
                travelled += length;
                float vTo = (float) (travelled / TEXTURE_REPEAT_LENGTH);

                quad(poseStack, buffer, from, to, offset, vFrom, vTo, packedLight);
            }
        }
    }

    private static double sagDepth(double span, double ropeLength) {
        double slack = ropeLength - span;
        if (slack <= SLACK_DEADBAND) {
            return 0.0D;
        }
        double fade = Mth.clamp((slack - SLACK_DEADBAND) / SLACK_DEADBAND, 0.0D, 1.0D);
        return span * Math.sqrt(3.0D * (slack / span) / 8.0D) * fade;
    }

    private static int segmentCount(double length) {
        return Mth.clamp((int) Math.ceil(length / TEXTURE_REPEAT_LENGTH) * 4, 4, MAX_SEGMENTS);
    }

    private static Vec3 pointAt(Vec3 start, Vec3 chord, double sag, double t) {
        Vec3 onChord = start.add(chord.scale(t));
        return sag == 0.0D ? onChord : onChord.subtract(0.0D, 4.0D * sag * t * (1.0D - t), 0.0D);
    }

    private static Vec3 ribbonOffset(Vec3 direction, int plane) {
        Vec3 reference = Math.abs(direction.y) > 0.99D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 across = direction.cross(reference).normalize();
        return plane == 0 ? across : direction.cross(across).normalize();
    }

    private static void quad(PoseStack poseStack, VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 offset,
                             float vFrom, float vTo, int packedLight) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        Vec3 normal = UPWARD_NORMAL;

        vertex(buffer, pose, normalMatrix, from.subtract(offset), 0.0F, vFrom, normal, packedLight);
        vertex(buffer, pose, normalMatrix, to.subtract(offset), 0.0F, vTo, normal, packedLight);
        vertex(buffer, pose, normalMatrix, to.add(offset), 1.0F, vTo, normal, packedLight);
        vertex(buffer, pose, normalMatrix, from.add(offset), 1.0F, vFrom, normal, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normalMatrix, Vec3 position,
                               float u, float v, Vec3 normal, int packedLight) {
        //? if <1.21 {
        /*buffer.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normalMatrix, (float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
        *///?} else {
        Vector3f transformedNormal = normalMatrix.transform(new Vector3f(
                (float) normal.x, (float) normal.y, (float) normal.z));
        buffer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());
        //?}
    }
}
