package me.mss1r.siegeworks.client.guide;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class GuideModelPreview {
    private record View(Entity entity, float yaw) {}
    private final java.util.Map<View, PlateBounds> measured = new java.util.HashMap<>();
    @Nullable
    <T extends Entity> Matrix4f render(GuiGraphics graphics, T model, int x, int y, int width, int height,
                                      @Nullable AABB fixedBounds, float yaw) {
        Quaternionf rotation = Axis.XP.rotationDegrees(23).mul(Axis.YP.rotationDegrees(yaw));
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        PlateBounds bounds;
        if (fixedBounds != null) {
            bounds = new PlateBounds();
            for (int index = 0; index < 8; index++) {
                Vector3f corner = new Vector3f((float) ((index & 1) == 0 ? fixedBounds.minX : fixedBounds.maxX),
                        (float) ((index & 2) == 0 ? fixedBounds.minY : fixedBounds.maxY),
                        (float) ((index & 4) == 0 ? fixedBounds.minZ : fixedBounds.maxZ)).rotate(rotation);
                bounds.include(corner.x, corner.y);
            }
        } else {
            bounds = measured.computeIfAbsent(new View(model, yaw), view -> {
                var plate = new PlateBounds();
                var measuringPose = new PoseStack();
                measuringPose.mulPose(rotation);
                // Collect the actual rendered silhouette once. The normal renderer includes its
                // own rotations, hidden parts and extra geometry; no world entity or GPU buffer is created.
                dispatcher.getRenderer(model).render(model, 0, 0, measuringPose, type -> plate,
                        LightTexture.FULL_BRIGHT);
                return plate;
            });
        }
        float scale = Math.min((width - 12) / (bounds.maxX - bounds.minX),
                (height - 12) / (bounds.maxY - bounds.minY));
        if (!Float.isFinite(scale) || scale <= 0) return null;

        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        var pose = graphics.pose();
        pose.pushPose();
        Lighting.setupForEntityInInventory();
        try {
            pose.translate(x + (width - (bounds.minX + bounds.maxX) * scale) / 2,
                    y + (height + (bounds.minY + bounds.maxY) * scale) / 2, 100);
            pose.scale(scale, -scale, scale);
            pose.mulPose(rotation);
            dispatcher.getRenderer(model).render(model, 0, 0, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
            graphics.flush();
            return new Matrix4f(pose.last().pose());
        } finally {
            Lighting.setupFor3DItems();
            pose.popPose();
            graphics.disableScissor();
        }
    }

    private static final class PlateBounds implements VertexConsumer {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        void include(float x, float y) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        //? if <1.21 {
        /*@Override public VertexConsumer vertex(double x, double y, double z) { include((float) x, (float) y); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer uv(float u, float v) { return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
        @Override public VertexConsumer uv2(int u, int v) { return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { return this; }
        @Override public void endVertex() {}
        @Override public void defaultColor(int r, int g, int b, int a) {}
        @Override public void unsetDefaultColor() {}
        *///?} else {
        @Override public VertexConsumer addVertex(float x, float y, float z) { include(x, y); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
        //?}
    }
}
