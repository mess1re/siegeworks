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

public class SingijeonModel extends EntityModel<Entity> {
    private final ModelPart root;

    public SingijeonModel(ModelPart root) {
        this.root = root.getChild("singijeon");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition singijeon = root.addOrReplaceChild("singijeon", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -0.5F, -26.5F, 1.0F, 1.0F, 53.0F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO);

        singijeon.addOrReplaceChild("fin_a", CubeListBuilder.create()
                        .texOffs(18, 54)
                        .addBox(0.0F, -2.5F, -4.5F, 0.0F, 5.0F, 9.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 20.0F, 0.0F, 0.0F, 2.3562F));
        singijeon.addOrReplaceChild("fin_b", CubeListBuilder.create()
                        .texOffs(0, 54)
                        .addBox(0.0F, -2.5F, -4.5F, 0.0F, 5.0F, 9.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 20.0F, 0.0F, 0.0F, 0.7854F));

        return LayerDefinition.create(meshDefinition, 128, 128);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
    }

    @Override
    //? if <1.21 {
    /*public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
    *///?} else {
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
    //?}
}
