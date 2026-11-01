package me.mss1r.siegeworks.gametest;

import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
//? if <1.21 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_draft")
@PrefixGameTestTemplate(false)
public final class DraftTeamGameTests {
    @GameTest(template = "empty")
    public static void hitchedMountsDoNotRunASecondMovementSolver(GameTestHelper helper) {
        for (var type : java.util.List.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.CAMEL)) {
            var horse = type.create(helper.getLevel());
            var siege = SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel());
            siege.setPos(helper.absoluteVec(new Vec3(20, 4, 20)));
            helper.assertTrue(horse.startRiding(siege, true), "The engine rejected " + type);
            siege.positionRider(horse);
            Vec3 hitched = horse.position();
            horse.setDeltaMovement(new Vec3(.25, -.08, .25));
            horse.travel(new Vec3(1, 0, 1));
            helper.assertTrue(horse.position().distanceToSqr(hitched) < 1E-8,
                    "Vanilla travel moved a hitched " + type + " away from its harness");
            horse.stopRiding();
            horse.setDeltaMovement(new Vec3(.25, 0, 0));
            Vec3 unhitched = horse.position();
            horse.travel(Vec3.ZERO);
            helper.assertTrue(horse.position().distanceToSqr(unhitched) > .01,
                    "Unhitching did not restore ordinary movement for " + type);
            horse.discard();
            siege.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hitchingUsesTheAuthoredTransportOrientationForEveryEngine(GameTestHelper helper) {
        for (var type : types()) for (float yaw : new float[]{0, 90, 179, -179}) {
            AbstractSiegeEntity siege = type.create(helper.getLevel());
            siege.setPos(helper.absoluteVec(new Vec3(20, 2, 20)));
            siege.applyYaw(yaw);
            var before = siege.collisionTransform();
            Horse horse = EntityType.HORSE.create(helper.getLevel());
            helper.assertTrue(horse.startRiding(siege, true), "The engine must accept its team");
            samePose(helper, before.yawDegrees() + siege.towedModelTurnDegrees(), siege, "Hitching");
            helper.assertTrue(siege.position().distanceToSqr(helper.absoluteVec(new Vec3(20, 2, 20))) < 1E-8,
                    "Hitching moved the machine");
            horse.rideTick();
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(horse.getYRot() - siege.getYRot())) < .001,
                    "The animal's head disagrees with the team's travel heading");
            Horse second = EntityType.HORSE.create(helper.getLevel());
            if (siege.canAddPassenger(second)) {
                second.startRiding(siege, true);
                samePose(helper, before.yawDegrees() + siege.towedModelTurnDegrees(), siege, "Adding another animal");
                second.stopRiding();
                samePose(helper, before.yawDegrees() + siege.towedModelTurnDegrees(), siege, "Removing another animal");
            }
            horse.stopRiding();
            samePose(helper, before.yawDegrees(), siege, "Unhitching");
            horse.discard();
            second.discard();
            siege.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadingAHarnessedEngineDoesNotApplyItsTowingTurnTwice(GameTestHelper helper) {
        for (var type : types()) {
            AbstractSiegeEntity siege = type.create(helper.getLevel());
            siege.setPos(helper.absoluteVec(new Vec3(20, 2, 20)));
            siege.applyYaw(37);
            Horse horse = EntityType.HORSE.create(helper.getLevel());
            horse.startRiding(siege, true);
            float heading = siege.collisionTransform().yawDegrees();
            CompoundTag saved = new CompoundTag();
            helper.assertTrue(siege.save(saved), "The fixture must save the entire team");
            horse.discard();
            siege.discard();
            var restored = (AbstractSiegeEntity) EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
            helper.assertTrue(restored != null && restored.isTowed(), "The team must load recursively");
            samePose(helper, heading, restored, "Loading");
            restored.getTowingMount().stopRiding();
            samePose(helper, heading - restored.towedModelTurnDegrees(), restored, "Unhitching after loading");
            restored.discard();
        }
        helper.succeed();
    }

    private static void samePose(GameTestHelper helper, float yaw, AbstractSiegeEntity siege, String action) {
        helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(siege.collisionTransform().yawDegrees() - yaw)) < .001,
                action + " rotated " + siege.getType() + " from " + yaw + " to " + siege.collisionTransform().yawDegrees());
    }

    private static java.util.List<EntityType<? extends AbstractSiegeEntity>> types() {
        return java.util.List.of(SiegeworksEntities.ONAGER_ENTITY.get(), SiegeworksEntities.CULVERIN_ENTITY.get(),
                SiegeworksEntities.SERPENTINE_ENTITY.get(), SiegeworksEntities.MONS_MEG_ENTITY.get(),
                SiegeworksEntities.ARCBALLISTA_ENTITY.get(), SiegeworksEntities.HWACHA_ENTITY.get(),
                SiegeworksEntities.BATTERING_RAM_ENTITY.get(), SiegeworksEntities.SIEGE_TOWER_ENTITY.get(),
                SiegeworksEntities.MANTLET_ENTITY.get());
    }
}
