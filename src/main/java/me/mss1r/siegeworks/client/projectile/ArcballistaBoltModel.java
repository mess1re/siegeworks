package me.mss1r.siegeworks.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class ArcballistaBoltModel extends EntityModel<Entity> {
    private final ModelPart arcballistaBolt;

    public ArcballistaBoltModel(ModelPart root) {
        this.arcballistaBolt = root.getChild("ArcballistaBolt");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition arcballistaBolt = root.addOrReplaceChild("ArcballistaBolt", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8.0F, -0.5F, -0.5F, 22.0F, 1.0F, 1.0F, new CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        arcballistaBolt.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                        .texOffs(0, 2).addBox(-3.3F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(14.5F, 0.0F, -0.2F, 0.0F, -0.3927F, 0.0F));

        arcballistaBolt.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                        .texOffs(0, 2).addBox(-3.3F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.01F)),
                PartPose.offsetAndRotation(14.5F, 0.0F, 0.2F, 0.0F, 0.3927F, 0.0F));

        arcballistaBolt.addOrReplaceChild("cube_r3", CubeListBuilder.create()
                        .texOffs(0, 4).addBox(-3.0F, 0.2F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -0.2F, 0.0F, 0.0F, -0.3054F, 0.0F));

        arcballistaBolt.addOrReplaceChild("cube_r4", CubeListBuilder.create()
                        .texOffs(0, 4).addBox(-3.0F, 0.2F, -0.5F, 3.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -0.2F, 0.0F, 0.0F, 0.3054F, 0.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    @Override
    //? if <1.21 {
    /*public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        arcballistaBolt.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
    *///?} else {
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        arcballistaBolt.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    //?}
}
