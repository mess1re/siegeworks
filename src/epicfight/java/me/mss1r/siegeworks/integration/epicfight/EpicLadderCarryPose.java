package me.mss1r.siegeworks.integration.epicfight;

import me.mss1r.siegeworks.gameplay.ladder.LadderCarry;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Places both hands under the carried ladder rails over Epic Fight's locomotion pose. */
final class EpicLadderCarryPose {
    private static final float RAIL_OFFSET = 6.0F / 16.0F;
    private static final float RAIL_UNDERSIDE = 1.5F / 16.0F;

    private EpicLadderCarryPose() {}

    static void apply(LivingEntityPatch<?> patch, float partialTick) {
        if (!(patch.getOriginal() instanceof Player rider) || !LadderCarry.isCarrying(rider)) return;
        var pose = patch.getClientAnimator().getPose(partialTick);
        var armature = patch.getArmature();
        var joints = armature.getPoseAsTransformMatrix(pose, false);
        Matrix4f worldToRig = new Matrix4f().rotationY(Mth.PI)
                .mul(EpicFirearmRig.matrix(patch.getModelMatrix(partialTick))).invert();
        float yaw = Mth.rotLerp(partialTick, rider.yBodyRotO, rider.yBodyRot);
        Vec3 right = Vec3.directionFromRotation(0, yaw + 90).scale(RAIL_OFFSET);
        Vec3 center = LadderCarry.renderedGripCenter(rider, partialTick)
                .subtract(rider.getPosition(partialTick)).add(0, -RAIL_UNDERSIDE, 0);
        // Epic Fight lifts the rendered player mesh when crouching.
        if (rider.isCrouching()) center = center.add(0, -.15, 0);
        new EpicFirearmRig.Arm(armature, joints, "R").solve(pose,
                grip(worldToRig, center.add(right)), new Vector3f(.7F, -1, .15F));
        new EpicFirearmRig.Arm(armature, joints, "L").solve(pose,
                grip(worldToRig, center.subtract(right)), new Vector3f(-.7F, -1, .15F));
        armature.setPose(pose);
    }

    private static Vector3f grip(Matrix4f transform, Vec3 point) {
        return transform.transformDirection((float) point.x, (float) point.y, (float) point.z, new Vector3f());
    }
}
