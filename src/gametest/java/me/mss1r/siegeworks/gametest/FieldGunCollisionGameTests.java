package me.mss1r.siegeworks.gametest;

import java.util.List;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.gameplay.crew.SiegePassengerPhysics;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.SerpentineEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FieldGunCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;
    private static final double EPSILON = 1.0E-7D;

    private FieldGunCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void fieldGunsUseCarriagesAndCannonsWithoutWheels(GameTestHelper helper) {
        SerpentineEntity serpentine = createSerpentine(helper);
        CulverinEntity culverin = createCulverin(helper);

        helper.assertTrue(groupNames(serpentine).equals(List.of("body", "cannon")),
                "Serpentine collision does not separate carriage and cannon");
        helper.assertTrue(groupNames(culverin).equals(List.of("body", "cannon")),
                "Culverin collision does not separate carriage and cannon");
        helper.assertTrue(GeneratedCollisionShapes.SERPENTINE_BODY.parts().size() == 19
                        && GeneratedCollisionShapes.SERPENTINE_CANNON.parts().size() == 7,
                "Serpentine collision does not match its structural model bones");
        helper.assertTrue(GeneratedCollisionShapes.CULVERIN_BODY.parts().size() == 14
                        && GeneratedCollisionShapes.CULVERIN_CANNON.parts().size() == 10,
                "Culverin collision does not match its structural model bones");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fieldGunAimRaisesCannonAroundAuthoredPivot(GameTestHelper helper) {
        assertAimRaisesCannon(helper, createSerpentine(helper));
        assertAimRaisesCannon(helper, createCulverin(helper));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towingTiltsCompleteFieldGunCarriage(GameTestHelper helper) {
        SerpentineEntity serpentine = createSerpentine(helper);
        CollisionGroup parkedBody = serpentine.collisionGroups().get(0);
        Vec3 trail = new Vec3(0.0D, 0.45D, -2.5D);
        Vec3 parkedTrail = parkedBody.fromGroup(trail);

        Horse horse = EntityType.HORSE.create(helper.getLevel());
        helper.assertTrue(horse != null && horse.startRiding(serpentine, true),
                "Serpentine rejected its draft mount");
        CollisionGroup towedBody = serpentine.collisionGroups().get(0);
        CollisionGroup towedCannon = serpentine.collisionGroups().get(1);
        Vec3 towedTrail = towedBody.fromGroup(trail);
        Vec3 cannonPivot = GeneratedCollisionShapes.SERPENTINE_CANNON.pivot();

        helper.assertTrue(towedTrail.y > parkedTrail.y + 0.3D,
                "Towing did not lift the Serpentine trail");
        helper.assertTrue(towedBody.fromGroup(cannonPivot).distanceToSqr(
                        towedCannon.fromGroup(cannonPivot)) < EPSILON,
                "Towed carriage and cannon disagree at their shared aim pivot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void towedFieldGunStopsWhereItsHorseMeetsAWall(GameTestHelper helper) {
        CulverinEntity gun = hitchedCulverin(helper, 0.0F);
        Horse horse = (Horse) gun.getTowingMounts().get(0);
        int wallZ = helper.absolutePos(BlockPos.ZERO).getZ() + 16;
        wall(helper, 16, 16);
        double startZ = gun.getZ();
        double[] lastZ = {startZ};
        int[] stillTicks = {0};
        helper.onEachTick(() -> {
            gun.setMovementInput(1.0F, 0.0F);
            stillTicks[0] = Math.abs(gun.getZ() - lastZ[0]) < 1.0E-4D ? stillTicks[0] + 1 : 0;
            lastZ[0] = gun.getZ();
            assertHorseClear(helper, horse);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(horse.getZ() > gun.getZ(), "The horse does not lead the towed Culverin");
            helper.assertTrue(gun.getZ() > startZ + 3.0D, "The towed Culverin did not drive toward the wall");
            helper.assertTrue(stillTicks[0] > 20, "The towed Culverin is still driving");
            helper.assertTrue(horse.getBoundingBox().maxZ > wallZ - 0.5D,
                    "The towed Culverin stopped short of the wall: gun=" + gun.position()
                            + ", horse=" + horse.getBoundingBox() + ", wallZ=" + wallZ);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void towedFieldGunCannotDriveItsHorseThroughAWallItTouches(GameTestHelper helper) {
        CulverinEntity gun = hitchedCulverin(helper, 0.0F);
        Horse horse = (Horse) gun.getTowingMounts().get(0);
        double[] front = {Double.NaN};
        helper.runAfterDelay(2, () -> {
            front[0] = horse.getBoundingBox().maxZ;
            int wallStart = Mth.floor(front[0] - 0.3D) - helper.absolutePos(BlockPos.ZERO).getZ();
            wall(helper, wallStart, wallStart + 2);
        });
        helper.onEachTick(() -> {
            if (Double.isNaN(front[0])) {
                return;
            }
            gun.setMovementInput(1.0F, 0.0F);
            helper.assertTrue(horse.getBoundingBox().maxZ <= front[0] + 1.0E-3D,
                    "A towing horse pushed on into a wall it touched");
        });
        helper.runAfterDelay(122, helper::succeed);
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void towedFieldGunTurnsNoHorseIntoAWall(GameTestHelper helper) {
        CulverinEntity gun = hitchedCulverin(helper, -25.0F);
        Horse horse = (Horse) gun.getTowingMounts().get(0);
        wall(helper, 15, 15);
        helper.onEachTick(() -> {
            gun.setMovementInput(1.0F, gun.tickCount / 30 % 2 == 0 ? 1.0F : -1.0F);
            assertHorseClear(helper, horse);
        });
        helper.runAfterDelay(240, helper::succeed);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void towingHorseStepsUpBeforeItsCulverin(GameTestHelper helper) {
        CulverinEntity gun = hitchedCulverin(helper, 0.0F);
        Horse horse = (Horse) gun.getTowingMounts().get(0);
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(1, 1, 16), origin.offset(38, 1, 31))) {
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
        double ground = origin.getY() + 1.0D;
        boolean[] ahead = {false};
        helper.onEachTick(() -> {
            gun.setMovementInput(1.0F, 0.0F);
            assertHorseClear(helper, horse);
            if (horse.getY() > ground + 0.99D && gun.getY() < ground + 0.5D) {
                ahead[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(gun.getY() > ground + 0.99D, "The towed Culverin did not climb the step");
            helper.assertTrue(ahead[0], "The horse did not step up before its Culverin");
        });
    }

    @GameTest(template = "empty")
    public static void draftMountDoesNotStepIntoNarrowHeadroom(GameTestHelper helper) {
        var gun = hitchedCulverin(helper, 0);
        var horse = gun.getTowingMounts().get(0);
        BlockPos wall = helper.absolutePos(new BlockPos(20, 1, 16));
        for (int height = 0; height < 3; height++) {
            helper.getLevel().setBlockAndUpdate(wall.above(height), Blocks.STONE.defaultBlockState());
        }
        // The body grazes the wall by 1.5 mm: its upper blocks still obstruct a step.
        Vec3 seat = new Vec3(wall.getX() - horse.getBbWidth() * .5D + .0015D,
                wall.getY(), wall.getZ() + .5D);
        horse.setPos(seat);
        Vec3 feet = SiegePassengerPhysics.mountFooting(gun, horse, seat);
        helper.assertTrue(Math.abs(feet.y - seat.y) < 1E-6,
                "The draft mount stepped into solid headroom at a wall edge: " + seat + " -> " + feet);
        horse.discard();
        gun.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void staleGroundFlagDoesNotLetAnEngineFly(GameTestHelper helper) {
        var gun = createCulverin(helper);
        double before = gun.getY();
        gun.setOnGround(true);
        gun.setDeltaMovement(Vec3.ZERO);
        me.mss1r.siegeworks.gameplay.movement.SiegeMovementPhysics.updateSiegeVelocity(gun);
        helper.assertTrue(gun.getY() < before - .07D, "A stale ground flag suspended the engine in air");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 50)
    public static void hitchedHorseFallsOntoLowerGroundInsteadOfSnapping(GameTestHelper helper) {
        var gun = hitchedCulverin(helper, 0);
        var horse = gun.getTowingMounts().get(0);
        // A horse has just left a raised step while the wheels remain on the lower floor.
        Vec3 seat = gun.position().add(SiegePassengerPhysics.rotatedSeatOffset(gun, horse));
        horse.setPos(seat.x, gun.getY() + 1, seat.z);
        double before = horse.getY();
        gun.positionRider(horse);
        helper.assertTrue(before - horse.getY() > 0 && before - horse.getY() < .1D,
                "The harness snapped its horse down to the ground: " + (before - horse.getY()));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(Math.abs(horse.getY() - gun.getY()) < .01D, "The horse did not land on the lower floor");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void repeatedPassengerUpdatesCannotSnapAHorseDownAfterAStep(GameTestHelper helper) {
        var gun = hitchedCulverin(helper, 0);
        var horse = gun.getTowingMounts().get(0);
        Vec3 seat = gun.position().add(SiegePassengerPhysics.rotatedSeatOffset(gun, horse));
        BlockPos step = BlockPos.containing(seat);
        helper.getLevel().setBlockAndUpdate(step, Blocks.STONE.defaultBlockState());
        gun.positionRider(horse);
        double raisedY = horse.getY();
        helper.assertTrue(raisedY > gun.getY() + .9D, "The horse did not step onto the raised block");
        helper.getLevel().setBlockAndUpdate(step, Blocks.AIR.defaultBlockState());
        gun.positionRider(horse);
        helper.assertTrue(horse.getY() >= raisedY - .1D,
                "A second passenger update snapped the horse down in the same tick");
        helper.succeed();
    }

    private static CulverinEntity hitchedCulverin(GameTestHelper helper, float yaw) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(39, 5, 31))) {
            level.setBlock(pos, pos.getY() == origin.getY() ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), 2);
        }
        CulverinEntity gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(level);
        helper.assertTrue(gun != null, "Failed to create Culverin");
        gun.moveTo(origin.getX() + 19.5D, origin.getY() + 1.0D, origin.getZ() + 4.5D, yaw, 0.0F);
        level.addFreshEntity(gun);
        Horse horse = EntityType.HORSE.create(level);
        ArmorStand driver = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(horse != null && driver != null, "Failed to create the team");
        horse.setTamed(true);
        horse.moveTo(gun.getX(), gun.getY(), gun.getZ() + 2.0D, 0.0F, 0.0F);
        driver.moveTo(horse.getX(), horse.getY(), horse.getZ(), 0.0F, 0.0F);
        level.addFreshEntity(horse);
        level.addFreshEntity(driver);
        helper.assertTrue(horse.startRiding(gun, true), "Culverin rejected its draft mount");
        helper.assertTrue(driver.startRiding(horse, true), "The horse took no driver");
        return gun;
    }

    /** A stone wall four blocks high across the floor, between the given relative z rows. */
    private static void wall(GameTestHelper helper, int fromZ, int toZ) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(1, 1, fromZ), origin.offset(38, 4, toZ))) {
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        }
    }

    private static void assertHorseClear(GameTestHelper helper, Horse horse) {
        if (!horse.isPassenger()) return;
        AABB box = horse.getBoundingBox().deflate(1.0E-3D);
        for (var shape : helper.getLevel().getBlockCollisions(horse, box)) {
            var engine = (AbstractSiegeEntity) horse.getVehicle();
            Vec3 seat = engine.position().add(SiegePassengerPhysics.rotatedSeatOffset(engine, horse));
            helper.fail("A towing horse overlaps blocks: feet=" + horse.position() + " box=" + box
                    + " collision=" + shape.toAabbs() + " engine=" + horse.getVehicle().position()
                    + " yaw=" + horse.getVehicle().getYRot()
                    + " seat=" + seat + " footing=" + SiegePassengerPhysics.mountFooting(engine, horse, seat)
                    + " ticks=" + horse.tickCount + "/" + horse.getVehicle().tickCount);
        }
    }

    @GameTest(template = "empty")
    public static void fieldGunBoundsIncludeFullTrail(GameTestHelper helper) {
        CulverinEntity culverin = createCulverin(helper);
        AABB modelBounds = StructureCollisionResolver.worldBounds(culverin);

        helper.assertTrue(modelBounds != null, "Culverin has no model-derived bounds");
        helper.assertTrue(modelBounds.getZsize() > 3.9D,
                "Culverin model bounds do not reach both its trail and muzzle");
        helper.assertTrue(contains(culverin.getBoundingBoxForCulling(), modelBounds),
                "Culverin culling bounds do not contain its complete model");
        helper.succeed();
    }

    private static boolean contains(AABB outer, AABB inner) {
        return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
                && outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
    }

    private static void assertAimRaisesCannon(GameTestHelper helper, AbstractFieldGunEntity gun) {
        CollisionGroup level = gun.collisionGroups().get(1);
        CollisionPart barrel = level.parts().get(level.parts().size() - 1);
        AABB barrelBox = barrel.box();
        Vec3 muzzle = new Vec3(barrelBox.getCenter().x, barrelBox.getCenter().y, barrelBox.maxZ);
        Vec3 levelMuzzle = level.fromPart(barrel, muzzle);

        gun.setTrackedPitch(gun.getMinAimPitch());
        CollisionGroup raised = gun.collisionGroups().get(1);
        Vec3 raisedMuzzle = raised.fromPart(barrel, muzzle);

        helper.assertTrue(raisedMuzzle.y > levelMuzzle.y + 0.2D,
                gun.getType() + " aim tilted its cannon opposite to the rendered model");
        helper.assertTrue(level.fromGroup(level.shape().pivot()).distanceToSqr(
                        raised.fromGroup(raised.shape().pivot())) < EPSILON,
                gun.getType() + " aim moved its authored cannon pivot");
    }

    private static List<String> groupNames(AbstractFieldGunEntity gun) {
        return gun.collisionGroups().stream().map(CollisionGroup::name).toList();
    }

    private static SerpentineEntity createSerpentine(GameTestHelper helper) {
        SerpentineEntity gun = SiegeworksEntities.SERPENTINE_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(gun != null, "Failed to create Serpentine");
        placeAtTestCenter(helper, gun);
        return gun;
    }

    private static CulverinEntity createCulverin(GameTestHelper helper) {
        CulverinEntity gun = SiegeworksEntities.CULVERIN_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(gun != null, "Failed to create Culverin");
        placeAtTestCenter(helper, gun);
        return gun;
    }

    private static void placeAtTestCenter(GameTestHelper helper, AbstractFieldGunEntity gun) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        gun.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        level.addFreshEntity(gun);
    }
}
