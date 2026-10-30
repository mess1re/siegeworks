package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;

public class CannonBall extends EntityModel<Entity> {
	private final ModelPart main;
	public CannonBall(ModelPart root) {
		this.main = root.getChild("main");
	}
	public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
		modelPartData.addOrReplaceChild("main", CubeListBuilder.create()
                .texOffs(0, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		        .texOffs(0, 0).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
		return LayerDefinition.create(modelData, 16, 16);
	}

	@Override
	//? if <1.21 {
	/*public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
	*///?} else {
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
		main.render(poseStack, vertexConsumer, light, overlay, color);
	}
	//?}

    @Override
    public void setupAnim(Entity entity, float f, float g, float h, float i, float j) {

    }
}
