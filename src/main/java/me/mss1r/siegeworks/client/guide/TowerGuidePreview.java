package me.mss1r.siegeworks.client.guide;

import me.mss1r.siegeworks.client.entity.siegetower.SiegeTowerRenderer;
import me.mss1r.siegeworks.client.rope.SiegeRopeAnchors;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** An untracked entity that exists only while the manual screen is open. */
final class TowerGuidePreview extends SiegeTowerEntity {
    private static final float LEVEL_BRIDGE_PROGRESS = 0.75F;

    TowerGuidePreview(ClientLevel level) {
        super(SiegeworksEntities.SIEGE_TOWER_ENTITY.get(), level);
    }

    @Override
    public boolean hasLeatherBottom() {
        return true;
    }

    @Override
    public boolean hasLeatherTop() {
        return false;
    }

    @Override
    public float getRenderedBridgeAngleRadians(float partialTick) {
        return (float) (Math.PI / 2);
    }

    @Override
    public float getRenderedBridgeProgress(float partialTick) {
        return LEVEL_BRIDGE_PROGRESS;
    }

    @Override
    public float getBridgeTargetProgress() {
        return getRenderedBridgeProgress(0);
    }

    void drawCallouts(GuiGraphics graphics, Font font, Matrix4f projection, int right, int color) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this)
                instanceof SiegeTowerRenderer renderer)) return;
        // The normal entity renderer turns the model to its default yaw before drawing the bones.
        projection.rotateY(Mth.PI).translate(0, .01F, 0);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 220);
        mark(graphics, font, projection, renderer, "stairs_d", "1", right, color);
        mark(graphics, font, projection, renderer, "stairs_u", "1", right, color);
        mark(graphics, font, projection, renderer, "axis", "2", right, color);
        graphics.pose().popPose();
    }

    private void mark(GuiGraphics graphics, Font font, Matrix4f projection, SiegeTowerRenderer renderer,
                      String bone, String number, int right, int color) {
        SiegeRopeAnchors.modelPosition(renderer.getGeoModel(), bone).ifPresent(anchor -> {
            Vector3f point = projection.transformPosition(new Vector3f((float) anchor.x, (float) anchor.y, (float) anchor.z));
            int x = Math.round(point.x);
            int y = Math.round(point.y);
            graphics.hLine(x, right - 3, y, 0xFF000000 | color);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFF000000 | color);
            graphics.drawString(font, number, right, y - 4, color, false);
        });
    }
}
