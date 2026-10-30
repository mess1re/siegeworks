package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class TrebuchetProjectileModel extends EntityModel<Entity> {
	private final ModelPart projectile;
	public TrebuchetProjectileModel(ModelPart root) {
		this.projectile = root.getChild("projectile");
	}
	public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
		modelPartData.addOrReplaceChild("projectile", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		return LayerDefinition.create(modelData, 32, 32);
	}

	@Override
	//? if <1.21 {
	/*public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		projectile.render(poseStack, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
	*///?} else {
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		projectile.render(poseStack, vertexConsumer, light, overlay, color);
	}
	//?}

    @Override
    public void setupAnim(Entity entity, float f, float g, float h, float i, float j) {

    }
}
