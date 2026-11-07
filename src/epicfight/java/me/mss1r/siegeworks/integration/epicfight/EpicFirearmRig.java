package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.client.firearm.FirearmAnimation;
import me.mss1r.siegeworks.item.FirearmItem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Map;

/** Applies the firearm pose to EF's torso and articulated arms over its locomotion layer. */
public final class EpicFirearmRig {
    private static final float VIEWMODEL_FORWARD = .25F;
    private static final float VIEWMODEL_DROP = .05F;

    public record Frame(Pose pose, Map<String, FirearmAnimation.Part> equipment, Vector3f offset,
                        boolean showOffhand, ItemStack material) {}

    private EpicFirearmRig() {}

    public static Frame sample(LivingEntityPatch<?> patch, float partialTick) {
        return sample(patch, partialTick, false);
    }

    static Frame sampleView(LivingEntityPatch<?> patch, float partialTick) {
        return sample(patch, partialTick, true);
    }

    private static Frame sample(LivingEntityPatch<?> patch, float partialTick, boolean viewSpace) {
        var entity = patch.getOriginal();
        Pose pose = viewSpace ? Animations.BIPED_IDLE.get().getRawPose(0)
                : patch.getClientAnimator().baseLayer.getEnabledPose(patch, true, partialTick);
        String action = EpicFirearmAnimations.activeAction(entity);
        var actionMotion = EpicFirearmAnimations.actionMotion(action);
        if (!FirearmAnimation.holds(entity) || actionMotion == null
                && !viewSpace && !EpicFirearmMotion.reloadHandoff(entity, action, false)) {
            return new Frame(pose, Map.of(), new Vector3f(), true, ItemStack.EMPTY);
        }

        boolean left = entity.getMainArm() == HumanoidArm.LEFT;
        float sign = left ? -1 : 1;
        OpenMatrix4f model = patch.getModelMatrix(partialTick);
        if (!viewSpace && actionMotion == LivingMotions.AIM
                && ((FirearmItem) entity.getMainHandItem().getItem()).matchlock()) {
            var motion = EpicFirearmMotion.sample(entity, action, false, 0, 0, partialTick);
            return shoulderStance(patch.getArmature(), pose, motion, sign, model,
                    entity.getViewVector(partialTick), entity.getViewYRot(partialTick));
        }
        float bodyYaw = (float) -Mth.atan2(model.m20, model.m22) * Mth.RAD_TO_DEG;
        float aimYaw = viewSpace ? 0 : sign * Mth.wrapDegrees(bodyYaw - entity.getViewYRot(partialTick));
        float pitch = viewSpace ? 0 : entity.getViewXRot(partialTick);
        var motion = EpicFirearmMotion.sample(entity, action, viewSpace, pitch, aimYaw, partialTick);
        Armature armature = patch.getArmature();
        poseTorso(pose, -sign * motion.turn(), motion.lean(), motion.loading(), viewSpace);
        OpenMatrix4f[] base = armature.getPoseAsTransformMatrix(pose, false);
        Arm mainArm = new Arm(armature, base, left ? "L" : "R");
        Arm supportArm = new Arm(armature, base, left ? "R" : "L");
        Vector3f main = toArmature(EpicFirearmMotion.point(motion.gun(), motion.mainGrip()), sign);
        Vector3f support = motion.support() == null ? null : toArmature(motion.support(), sign);
        Vector3f offset = viewSpace ? new Vector3f(0, -VIEWMODEL_DROP, -VIEWMODEL_FORWARD) : new Vector3f();
        // The two hands and props retain the same contacts when the base stance moves.
        if (support == null) {
            mainArm.fit(main, offset);
        } else {
            for (int i = 0; i < 10; i++) {
                mainArm.fit(main, offset);
                supportArm.fit(support, offset);
            }
        }
        mainArm.solve(pose, main.add(offset), new Vector3f(sign * .53F, -1, .32F));
        if (support != null) {
            support.add(offset);
            if (viewSpace && motion.relaxedOffhand() > 0) {
                // The forward viewmodel shift is for the gun. Restore the resting arm's depth.
                float retraction = VIEWMODEL_FORWARD * motion.relaxedOffhand();
                Joint chest = armature.searchJointByName("Chest");
                supportArm.retract(pose, matrix(base[chest.getId()]), retraction);
                support.z += retraction;
            }
            supportArm.solve(pose, support, new Vector3f(-sign * .45F, -1, .18F));
        }
        return new Frame(pose, motion.equipment(), offset, motion.showOffhand(), motion.material());
    }

    private static Frame shoulderStance(Armature armature, Pose pose, EpicFirearmMotion.Frame motion,
                                        float sign, OpenMatrix4f model, Vec3 look, float viewYaw) {
        Matrix4f inverseModel = new Matrix4f().rotationY(Mth.PI).mul(matrix(model)).invert();
        Vector3f direction = inverseModel.transformDirection(new Vector3f((float) look.x, (float) look.y, (float) look.z)).normalize();
        Vec3 horizontal = Vec3.directionFromRotation(0, viewYaw);
        Vector3f heading = inverseModel.transformDirection(new Vector3f((float) horizontal.x, 0, (float) horizontal.z));
        float yaw = (float) Mth.atan2(-heading.x, -heading.z);
        float pitch = (float) Math.asin(Mth.clamp(direction.y, -1, 1)) * Mth.RAD_TO_DEG;
        float kick = -motion.gun().rotation().getEulerAnglesXYZ(new Vector3f()).x * Mth.RAD_TO_DEG;
        Pose authored = EpicFirearmAnimations.arquebusPose(sign < 0, Mth.clamp(pitch + kick, -90, 90));
        authored.getJointTransformData().forEach((name, transform) -> {
            if (!name.equals("Root")) pose.putJointData(name, transform);
        });
        Joint root = armature.searchJointByName("Root");
        OpenMatrix4f[] keyed = armature.getPoseAsTransformMatrix(pose, false);
        local(pose, root, new Matrix4f(),
                new Matrix4f().rotationY(yaw).setTranslation(position(keyed[root.getId()])));

        // The keyframes include the weapon socket on the same skeleton as both hands.
        var joints = armature.getPoseAsTransformMatrix(pose, false);
        Joint grip = armature.searchJointByName(sign > 0 ? "Tool_R" : "Tool_L");
        Matrix4f wrist = matrix(joints[grip.getId()]);
        Quaternionf orientation = new Matrix4f().rotationY(Mth.PI).scale(sign, 1, 1)
                .mul(wrist).scale(sign, 1, 1).getNormalizedRotation(new Quaternionf());
        float scale = motion.gun().scale();
        Vector3f origin = toArmature(wrist.getTranslation(new Vector3f()), sign).mul(16)
                .sub(orientation.transform(new Vector3f(motion.mainGrip()).mul(scale)));
        var gun = new FirearmAnimation.Part(origin, orientation, scale);
        var equipment = motion.equipment();
        equipment.put("weapon", gun);
        if (motion.trigger() != null) {
            Quaternionf change = new Quaternionf(orientation).mul(new Quaternionf(motion.gun().rotation()).conjugate());
            var trigger = motion.trigger();
            equipment.put("trigger", new FirearmAnimation.Part(
                    change.transform(new Vector3f(trigger.position()).sub(motion.gun().position())).add(origin),
                    new Quaternionf(change).mul(trigger.rotation()), scale));
        }
        return new Frame(pose, equipment, new Vector3f(), false, ItemStack.EMPTY);
    }

    private static void poseTorso(Pose pose, float turn, float lean, float loading, boolean viewSpace) {
        Quaternionf twist = new Quaternionf().rotationYXZ(turn * Mth.DEG_TO_RAD, -lean * Mth.DEG_TO_RAD, 0);
        JointTransform chest = pose.orElseEmpty("Chest").copy();
        chest.rotation().mul(twist);
        if (viewSpace) {
            // Move the shoulders and both grips together in the camera view.
            chest.translation().y -= VIEWMODEL_DROP + EpicFirearmMotion.LOAD_VIEW_DROP * loading;
            chest.translation().z -= VIEWMODEL_FORWARD + EpicFirearmMotion.LOAD_VIEW_FORWARD * loading;
        }
        pose.putJointData("Chest", chest);
        JointTransform head = pose.orElseEmpty("Head").copy();
        head.rotation().premul(twist.conjugate());
        if (!viewSpace) head.rotation().rotateX(loading * 8 * Mth.DEG_TO_RAD);
        pose.putJointData("Head", head);
    }

    private static Vector3f toArmature(Vector3f point, float sign) {
        return new Vector3f(-sign * point.x, point.y, -point.z);
    }

    static final class Arm {
        final Joint collar, upper, forearm, wrist, elbow;
        final Matrix4f shoulder;
        final Vector3f origin;
        final float upperLength, forearmLength;
        final float restFlex;

        Arm(Armature armature, OpenMatrix4f[] base, String side) {
            collar = armature.searchJointByName("Shoulder_" + side);
            upper = armature.searchJointByName("Arm_" + side);
            forearm = armature.searchJointByName("Hand_" + side);
            wrist = armature.searchJointByName("Tool_" + side);
            elbow = armature.searchJointByName("Elbow_" + side);
            shoulder = matrix(base[collar.getId()]);
            origin = position(base[upper.getId()]);
            upperLength = position(forearm.getLocalTransform()).length();
            forearmLength = position(wrist.getLocalTransform()).length();
            restFlex = matrix(forearm.getLocalTransform()).getEulerAnglesXYZ(new Vector3f()).x;
        }

        void retract(Pose pose, Matrix4f chest, float distance) {
            shoulder.m32(shoulder.m32() + distance);
            origin.z += distance;
            local(pose, collar, chest, shoulder);
        }

        void fit(Vector3f target, Vector3f offset) {
            Vector3f delta = new Vector3f(target).add(offset).sub(origin);
            float distance = delta.length();
            float reach = upperLength + forearmLength - .014F;
            if (distance > reach) offset.sub(delta.mul((distance - reach) / distance));
        }

        void solve(Pose pose, Vector3f target, Vector3f bendHint) {
            Vector3f axis = new Vector3f(target).sub(origin);
            float distance = Mth.clamp(axis.length(), Math.abs(upperLength - forearmLength) + .002F,
                    upperLength + forearmLength - .002F);
            if (axis.lengthSquared() < 1e-8F) axis.set(0, -1, 0);
            axis.normalize();

            Vector3f bend = bendHint.sub(new Vector3f(axis).mul(bendHint.dot(axis)));
            if (bend.lengthSquared() < 1e-8F) {
                if (Math.abs(axis.z) > .9F) bend.set(axis).cross(1, 0, 0);
                else bend.set(axis).cross(0, 0, 1);
            }
            bend.normalize();
            float along = (upperLength * upperLength - forearmLength * forearmLength + distance * distance) / (2 * distance);
            float height = (float) Math.sqrt(Math.max(0, upperLength * upperLength - along * along));
            Vector3f elbowPosition = new Vector3f(origin).fma(along, axis).fma(height, bend);
            Vector3f wristPosition = new Vector3f(origin).fma(distance, axis);
            Vector3f upperDirection = new Vector3f(elbowPosition).sub(origin).normalize();
            Vector3f forearmDirection = new Vector3f(wristPosition).sub(elbowPosition).normalize();
            // Both bones share the same elbow hinge. Independent shortest-arc
            // rotations twist the sleeves in opposite directions when the arm bends.
            Vector3f hinge = upperDirection.cross(forearmDirection, new Vector3f()).normalize();
            Matrix4f upperGlobal = bone(hinge, upperDirection, origin);
            Matrix4f forearmGlobal = bone(hinge, forearmDirection, elbowPosition);
            local(pose, upper, shoulder, upperGlobal);
            local(pose, forearm, upperGlobal, forearmGlobal);
            pose.putJointData(wrist.getName(), JointTransform.empty());
            // EF's elbow weights follow the upper arm. Its own ranged poses
            // extend this joint along the upper bone as the forearm folds.
            JointTransform crease = JointTransform.empty();
            float flex = (float) Math.acos(Mth.clamp(upperDirection.dot(forearmDirection), -1, 1));
            crease.translation().z = -.16F * Mth.clamp((flex - restFlex) / Mth.HALF_PI, 0, 1);
            pose.putJointData(elbow.getName(), crease);
        }
    }

    private static Matrix4f bone(Vector3f hinge, Vector3f direction, Vector3f origin) {
        Vector3f cross = hinge.cross(direction, new Vector3f());
        return new Matrix4f(new Matrix3f(hinge, direction, cross)).setTranslation(origin);
    }

    private static void local(Pose pose, Joint joint, Matrix4f parent, Matrix4f global) {
        Matrix4f delta = matrix(joint.getLocalTransform()).invert().mul(new Matrix4f(parent).invert()).mul(global);
        pose.putJointData(joint.getName(), JointTransform.fromMatrixWithoutScale(epic(delta)));
    }

    private static Vector3f position(OpenMatrix4f matrix) {
        return new Vector3f(matrix.m30, matrix.m31, matrix.m32);
    }

    static Matrix4f matrix(OpenMatrix4f matrix) {
        return OpenMatrix4f.exportToMojangMatrix(matrix);
    }

    static OpenMatrix4f epic(Matrix4f matrix) {
        return OpenMatrix4f.importFromMojangMatrix(matrix);
    }
}
