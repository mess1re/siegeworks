package me.mss1r.siegeworks.client.ladder;

import com.mojang.blaze3d.vertex.PoseStack;
import me.mss1r.siegeworks.gameplay.ladder.LadderPush;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client side of bracing a pole against a ladder: a side-on stance, the rear hand low by the hip and the front one on
 * the shaft, which points where the player looks.
 *
 * <p>The stance is worked out as for a right-handed holder in Blockbench's frame (y up, the model facing -z, its right
 * hand toward +x), then turned into model part rotations; a left-handed holder mirrors it.
 */
public final class LadderBraceClient {
    /** How far the shaft swings across the body, and the body back the other way so the shaft points ahead. */
    private static final float SIDE_ON_DEGREES = 40.0F;
    /** The rear arm, Blockbench degrees: down and forward, across toward the hip. */
    private static final float REAR_ARM_X = 70.0F;
    private static final float REAR_ARM_Y = 60.0F;
    private static final float MIN_PITCH = -30.0F;
    private static final float MAX_PITCH = 70.0F;
    /** Legs apart, front one forward, Blockbench degrees. */
    private static final float FRONT_LEG_X = 16.0F;
    private static final float REAR_LEG_X = -14.0F;
    private static final Vector3f REAR_SHOULDER = new Vector3f(5.0F, 22.0F, 0.0F);
    private static final Vector3f FRONT_SHOULDER = new Vector3f(-5.0F, 22.0F, 0.0F);
    /** Where the hand grips, from the shoulder. */
    private static final Vector3f HAND = new Vector3f(1.0F, -9.0F, 0.0F);

    /** The pose last set up, kept for the held item rendered right after it. */
    private static int posedEntity = -1;
    private static final Vector3f posedShaft = new Vector3f();
    private static final Vector3f posedSide = new Vector3f();

    private LadderBraceClient() {
    }

    /** Whether {@code entity} is posed bracing, which a model must check before {@link #pose}. */
    public static boolean isBracing(LivingEntity entity) {
        return LadderPush.isBracing(entity);
    }

    /**
     * Sets the bracing stance with the shaft along the look: turned with the head, which leads the body, and pitched
     * as it, in degrees down.
     */
    public static void pose(HumanoidModel<?> model, LivingEntity entity, float headYaw, float headPitch) {
        boolean left = holdingArm(entity) == HumanoidArm.LEFT;
        float pitch = Mth.clamp(headPitch, MIN_PITCH, MAX_PITCH);
        float turn = -SIDE_ON_DEGREES - (left ? -headYaw : headYaw);

        Matrix3f rearArm = euler(REAR_ARM_X, REAR_ARM_Y, 0.0F);
        Vector3f grip = rearArm.transform(new Vector3f(HAND)).add(REAR_SHOULDER);
        Vector3f shaft = new Matrix3f().rotationYXZ(rad(SIDE_ON_DEGREES), rad(-pitch), 0.0F)
                .transform(new Vector3f(0.0F, 0.0F, -1.0F));
        // The front hand takes the shaft where it comes within reach of its shoulder, or as near as it can
        Vector3f fromShoulder = new Vector3f(grip).sub(FRONT_SHOULDER);
        float reach = HAND.length();
        float b = fromShoulder.dot(shaft);
        float c = fromShoulder.lengthSquared() - reach * reach;
        float along = b * b >= c ? -b + Mth.sqrt(b * b - c) : -b;
        Vector3f frontHand = new Vector3f(shaft).mul(along).add(fromShoulder);
        float[] frontArm = armToward(frontHand);

        setPart(model.body, 0.0F, turn, left);
        setArm(left ? model.leftArm : model.rightArm, REAR_ARM_X, REAR_ARM_Y + turn, REAR_SHOULDER, turn, left);
        setArm(left ? model.rightArm : model.leftArm, frontArm[0], frontArm[1] + turn, FRONT_SHOULDER, turn, left);
        setLeg(left ? model.leftLeg : model.rightLeg, REAR_LEG_X, 1.9F, turn, left);
        setLeg(left ? model.rightLeg : model.leftLeg, FRONT_LEG_X, -1.9F, turn, left);

        // The shaft and the side its fork faces, in the rear arm's own frame, for the held item
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f side = new Vector3f(up).sub(new Vector3f(shaft).mul(up.dot(shaft))).normalize().cross(new Vector3f(shaft).negate());
        Matrix3f toArm = new Matrix3f(rearArm).transpose();
        posedShaft.set(toModel(toArm.transform(new Vector3f(shaft))));
        posedSide.set(toModel(toArm.transform(side)));
        posedEntity = entity.getId();
    }

    /**
     * Turns the held pole, about where the hand grips it, from where its display transform points it to along the
     * braced shaft. Called with the pose stack at the hand, right before the item renders.
     */
    public static void alignHeldItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                     PoseStack poseStack) {
        if (entity.getId() != posedEntity || !LadderPush.isBracing(entity) || arm != holdingArm(entity)
                || stack != entity.getItemInHand(LadderPush.bracingHand(entity))) {
            return;
        }
        boolean left = arm == HumanoidArm.LEFT;
        Minecraft minecraft = Minecraft.getInstance();
        ItemTransform display = minecraft.getItemRenderer().getModel(stack, entity.level(), entity, entity.getId())
                .getTransforms().getTransform(left ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : context);
        Quaternionf rotation = new Quaternionf().rotationXYZ(rad(display.rotation.x()), rad(display.rotation.y()),
                rad(display.rotation.z()));
        // A long weapon's shaft runs corner to corner across its sprite
        Vector3f heldShaft = rotation.transform(new Vector3f(display.scale.x(), display.scale.y(), 0.0F));
        Vector3f heldSide = rotation.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        Vector3f offset = new Vector3f(display.translation);
        Vector3f gripPoint = new Vector3f(heldShaft).mul(-offset.dot(heldShaft) / heldShaft.lengthSquared()).add(offset);

        // The hand frame is the arm's turned by -90 about x and 180 about y
        Matrix3f fromArm = new Matrix3f().rotationX(rad(-90.0F)).rotateY(rad(180.0F)).transpose();
        Matrix3f correction = frame(fromArm.transform(new Vector3f(posedShaft)), fromArm.transform(new Vector3f(posedSide)))
                .mul(frame(heldShaft, heldSide).transpose());
        Quaternionf turn = correction.getNormalizedRotation(new Quaternionf());
        if (left) {
            gripPoint.x = -gripPoint.x;
            turn.set(turn.x, -turn.y, -turn.z, turn.w);
        }
        poseStack.translate(gripPoint.x, gripPoint.y, gripPoint.z);
        poseStack.mulPose(turn);
        poseStack.translate(-gripPoint.x, -gripPoint.y, -gripPoint.z);
        posedEntity = -1;
    }

    private static HumanoidArm holdingArm(LivingEntity entity) {
        return LadderPush.bracingHand(entity) == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
    }

    /** Orthonormal columns: the direction, the side made square to it, and their cross. */
    private static Matrix3f frame(Vector3f direction, Vector3f side) {
        Vector3f x = new Vector3f(direction).normalize();
        Vector3f y = new Vector3f(side).sub(new Vector3f(x).mul(side.dot(x))).normalize();
        Vector3f z = new Vector3f(x).cross(y);
        return new Matrix3f(x, y, z);
    }

    /** Blockbench x and y degrees that point an arm, hanging down at rest, along {@code direction}. */
    private static float[] armToward(Vector3f direction) {
        Vector3f v = new Vector3f(direction).normalize();
        float alpha = (float) Math.acos(Mth.clamp(-v.y, -1.0F, 1.0F));
        float s = Math.max(Mth.sin(alpha), 1.0E-4F);
        float beta = (float) Mth.atan2(-v.x / s, -v.z / s);
        return new float[]{alpha * Mth.RAD_TO_DEG, beta * Mth.RAD_TO_DEG};
    }

    private static void setArm(net.minecraft.client.model.geom.ModelPart arm, float x, float y, Vector3f shoulder,
                               float turn, boolean mirror) {
        Vector3f at = new Matrix3f().rotationY(rad(turn)).transform(new Vector3f(shoulder));
        arm.x = mirror ? at.x : -at.x;
        arm.y = 24.0F - at.y;
        arm.z = at.z;
        setPart(arm, x, y, mirror);
    }

    private static void setLeg(net.minecraft.client.model.geom.ModelPart leg, float x, float hip, float turn,
                               boolean mirror) {
        Vector3f at = new Matrix3f().rotationY(rad(turn)).transform(new Vector3f(hip, 12.0F, leg.z));
        leg.x = mirror ? at.x : -at.x;
        leg.z = at.z;
        leg.xRot -= rad(x);
        leg.yRot = rad(mirror ? turn : -turn);
        leg.zRot = 0.0F;
    }

    /** Blockbench x and y degrees onto a model part, whose x and y axes point the other way. */
    private static void setPart(net.minecraft.client.model.geom.ModelPart part, float x, float y, boolean mirror) {
        part.xRot = -rad(x);
        part.yRot = mirror ? rad(y) : -rad(y);
        part.zRot = 0.0F;
    }

    private static Matrix3f euler(float x, float y, float z) {
        return new Matrix3f().rotationZYX(rad(z), rad(y), rad(x));
    }

    /** A Blockbench direction in model space, whose x and y point the other way. */
    private static Vector3f toModel(Vector3f v) {
        return v.set(-v.x, -v.y, v.z);
    }

    private static float rad(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }
}
