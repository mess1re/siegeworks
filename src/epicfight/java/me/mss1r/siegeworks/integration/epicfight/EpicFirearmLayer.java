package me.mss1r.siegeworks.integration.epicfight;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.client.firearm.FirearmAnimation;
import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import me.mss1r.siegeworks.item.FirearmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.client.renderer.patched.layer.PatchedItemInHandLayer;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.function.Function;
import java.util.Map;

/** Keeps equipment on the rendered EF wrists, including animation blends and reactions. */
public final class EpicFirearmLayer<E extends LivingEntity, T extends LivingEntityPatch<E>, M extends EntityModel<E>>
        extends PatchedItemInHandLayer<E,T,M> {
    private final Function<ItemStack, RenderItemBase> itemRenderer;

    public EpicFirearmLayer(Function<ItemStack, RenderItemBase> itemRenderer) {
        this.itemRenderer = itemRenderer;
    }

    @Override protected void renderLayer(T patch, E entity, RenderLayer<E,M> vanilla, PoseStack poses,
                                         MultiBufferSource buffers, int light, OpenMatrix4f[] joints,
                                         float bob, float yaw, float pitch, float partialTick) {
        if (LadderCarry.isCarrying(entity)) return;
        boolean view = patch.isFirstPerson() && Minecraft.getInstance().getCameraEntity() == entity;
        var frame = !FirearmAnimation.holds(entity) ? null : view
                ? EpicFirearmRig.sampleView(patch, partialTick) : EpicFirearmRig.sample(patch, partialTick);
        if (frame == null) {
            super.renderLayer(patch,entity,vanilla,poses,buffers,light,joints,bob,yaw,pitch,partialTick);
            return;
        }
        if (frame.equipment().isEmpty()) {
            var item = (FirearmItem) entity.getMainHandItem().getItem();
            int tool = patch.getArmature().searchJointByName(entity.getMainArm() == HumanoidArm.RIGHT ? "Tool_R" : "Tool_L").getId();
            float scale = EpicFirearmMotion.scale(item);
            var weapon = new FirearmAnimation.Part(new Vector3f(EpicFirearmMotion.mainGrip(item)).mul(-scale),
                    new Quaternionf(), scale);
            poses.pushPose();
            MathUtils.mulStack(poses, joints[tool]);
            FirearmAnimation.INSTANCE.renderArmatureHand(entity, entity.getMainArm(), poses, buffers, light,
                    partialTick, Map.of("weapon", weapon), new Vector3f(), ItemStack.EMPTY);
            poses.popPose();
            renderOffhand(patch, entity, poses, buffers, light, joints, partialTick);
            return;
        }
        OpenMatrix4f[] desired = patch.getArmature().getPoseAsTransformMatrix(frame.pose(),false);
        for (HumanoidArm hand : HumanoidArm.values()) {
            if (hand != entity.getMainArm() && !frame.equipment().containsKey("ramrod")
                    && !frame.equipment().containsKey("component")) {
                if (frame.showOffhand()) renderOffhand(patch, entity, poses, buffers, light, joints, partialTick);
                continue;
            }
            int index = patch.getArmature().searchJointByName(hand == HumanoidArm.RIGHT ? "Tool_R" : "Tool_L").getId();
            poses.pushPose();
            var correction = EpicFirearmRig.matrix(joints[index]).mul(EpicFirearmRig.matrix(desired[index]).invert());
            MathUtils.mulStack(poses, EpicFirearmRig.epic(correction));
            FirearmAnimation.INSTANCE.renderArmatureHand(entity,hand,poses,buffers,light,partialTick,
                    frame.equipment(),frame.offset(),frame.material());
            poses.popPose();
        }
    }

    private void renderOffhand(T patch, E entity, PoseStack poses, MultiBufferSource buffers, int light,
                               OpenMatrix4f[] joints, float partialTick) {
        var stack = entity.getOffhandItem();
        if (!stack.isEmpty() && patch.isOffhandItemValid()) {
            itemRenderer.apply(stack).renderItemInHand(stack, patch, InteractionHand.OFF_HAND,
                    joints, buffers, poses, light, partialTick);
        }
    }
}
