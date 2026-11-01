package me.mss1r.siegeworks.gametest;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.siegeworks.gameplay.collision.generated.GeneratedCollisionShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if <1.21 {
/*import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(SiegeTowerCollisionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SiegeTowerCollisionGameTests {
    public static final String NAMESPACE = StructureCollisionGameTests.NAMESPACE;

    private SiegeTowerCollisionGameTests() {
    }

    @GameTest(template = "empty")
    public static void sideStairsUseContinuousInclinedPlanes(GameTestHelper helper) {
        List<CollisionPart> inclined = GeneratedCollisionShapes.SIEGE_TOWER_BODY.parts().stream()
                .filter(part -> Math.abs(part.pose().rotation().m12()) > 0.25D
                        || Math.abs(part.pose().rotation().m21()) > 0.25D)
                .toList();
        helper.assertTrue(inclined.size() == 4,
                "Tower collision must contain one inclined plane per side stair flight, got "
                        + inclined.size());
        helper.assertTrue(inclined.stream().allMatch(part -> part.box().getYsize() <= 0.126D),
                "A tower side stair was exported as a filled volume instead of a thin plane");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void firstTowerCrewMemberCanBoardAsDriver(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create tower boarding test entity");
        moveToRelative(helper, tower, 8.0D, 1.0D, 8.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add tower boarding test entity");

        Player driver = SiegeGameTestPlayers.createRideable(level);
        Vec3 side = tower.collisionTransform().toWorld(new Vec3(4.0D, 0.0D, 0.0D));
        driver.setPos(side.x, side.y, side.z);
        InteractionResult result = tower.interactAt(driver, Vec3.ZERO, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction() && driver.getVehicle() == tower
                        && tower.hasActiveDriver(),
                "The first tower crew member did not take the driver slot");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void movingLowRampCarriesAndAllowsMovement(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.PIG.create(level);
        helper.assertTrue(tower != null && occupant != null,
                "Failed to create moving low-ramp test entities");

        setBridgeProgress(tower, 0.875F);
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        occupant.setNoAi(true);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add moving low-ramp tower");
        tower.tick();

        CollisionGroup bridge = bridgeGroup(tower);
        Vec3 surface = bridgeSurface(tower, bridge);
        occupant.setPos(surface.x, surface.y + 2.0D, surface.z);
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add moving low-ramp occupant");
        landOnRamp(tower, occupant, helper);

        double towerStartX = tower.getX();
        double occupantStartX = occupant.getX();
        prepareTowerMovement(tower);
        occupant.setOnGround(false);
        occupant.setDeltaMovement(Vec3.ZERO);
        tower.setPos(towerStartX + 0.4D, tower.getY(), tower.getZ());
        StructureMotionSystem.tickStructure(tower);

        helper.assertTrue(Math.abs((occupant.getX() - occupantStartX) - 0.4D) < 0.02D,
                "Low ramp lost its rider when vanilla onGround flickered: "
                        + (occupant.getX() - occupantStartX));

        Vec3 walkRequest = tower.collisionTransform().directionToWorld(
                new Vec3(0.0D, -0.08D, 0.2D));
        Vec3 walkStart = occupant.position();
        occupant.setDeltaMovement(walkRequest);
        occupant.move(MoverType.SELF, walkRequest);
        Vec3 walked = tower.collisionTransform().directionToLocal(
                occupant.position().subtract(walkStart));
        helper.assertTrue(walked.z > 0.195D && Math.abs(walked.x) < 0.005D,
                "Moving low ramp reduced or deflected walking: " + walked);

        Vec3 jumpRequest = tower.collisionTransform().directionToWorld(
                new Vec3(0.0D, 0.42D, 0.04D));
        double jumpStartY = occupant.getY();
        occupant.setDeltaMovement(jumpRequest);
        occupant.move(MoverType.SELF, jumpRequest);
        helper.assertTrue(occupant.getY() > jumpStartY + 0.419D && !occupant.onGround(),
                "Moving low ramp suppressed a jump: "
                        + occupant.position().subtract(walkStart));
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void movingRampDefersToWorldLanding(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.PIG.create(level);
        helper.assertTrue(tower != null && occupant != null,
                "Failed to create ramp landing test entities");

        setBridgeProgress(tower, 0.875F);
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        CollisionGroup bridge = bridgeGroup(tower);
        Vec3 initialSurface = bridgeSurface(tower, bridge);
        occupant.setPos(initialSurface.x, initialSurface.y + 2.0D, initialSurface.z);
        StructureCollisionResolver.ResolvedMovement initialLanding =
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), new Vec3(0.0D, -3.0D, 0.0D), 0.0D);
        double initialStandingY = occupant.getY() + initialLanding.allowed().y;
        double landingY = Math.ceil(initialStandingY);
        tower.setPos(tower.getX(), tower.getY() + landingY - initialStandingY, tower.getZ());
        Vec3 surface = bridgeSurface(tower, bridge);
        buildLanding(level, surface, landingY);

        occupant.setNoAi(true);
        occupant.setPos(surface.x, landingY + 2.0D, surface.z);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add ramp landing tower");
        // This fixture deliberately raises the wheels off the floor to align the ramp.
        // Register geometry without advancing gravity before the landing assertion.
        me.mss1r.axiomata.collision.system.StructureCollisionSystem.register(tower);
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add ramp landing occupant");

        StructureCollisionResolver.ResolvedMovement rampLanding =
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), new Vec3(0.0D, -3.0D, 0.0D), 0.0D);
        occupant.setPos(occupant.getX() + rampLanding.allowed().x,
                occupant.getY() + rampLanding.allowed().y,
                occupant.getZ() + rampLanding.allowed().z);
        helper.assertTrue(rampLanding.isSupported(),
                "Ramp and world landing did not share support");
        Vec3 worldProbeRequest = new Vec3(0.0D, -0.03D, 0.0D);
        AABB occupantBox = occupant.getBoundingBox();
        List<VoxelShape> entityCollisions = level.getEntityCollisions(
                occupant, occupantBox.expandTowards(worldProbeRequest));
        Vec3 worldProbe = Entity.collideBoundingBox(
                occupant, worldProbeRequest, occupantBox, level, entityCollisions);
        helper.assertTrue(worldProbe.y > worldProbeRequest.y + 1.0E-10D,
                "World landing did not support the occupant: y=" + occupant.getY()
                        + ", expected=" + landingY + ", probe=" + worldProbe.y);

        double startX = occupant.getX();
        prepareTowerMovement(tower);
        occupant.setOnGround(true);
        occupant.setDeltaMovement(Vec3.ZERO);
        tower.setPos(tower.getX() + 0.4D, tower.getY(), tower.getZ());
        StructureMotionSystem.tickStructure(tower);

        helper.assertTrue(Math.abs(occupant.getX() - startX) < 0.02D,
                "Tower dragged a world-supported occupant across the landing: "
                        + (occupant.getX() - startX));
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void movingUpperLadderHandsOffToThirdFloor(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.ZOMBIE.create(level);
        helper.assertTrue(tower != null && occupant != null,
                "Failed to create moving upper-ladder test entities");

        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        occupant.setNoAi(true);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add upper-ladder tower");
        tower.tick();

        Vec3 playerFeet = tower.collisionTransform().toWorld(
                new Vec3(-3.21875D, 14.5D, 1.4375D));
        AABB playerBox = new AABB(
                playerFeet.x - 0.3D, playerFeet.y, playerFeet.z - 0.3D,
                playerFeet.x + 0.3D, playerFeet.y + 1.8D, playerFeet.z + 0.3D);
        Vec3 playerAscent = tower.collisionTransform().directionToWorld(
                new Vec3(0.0D, 3.0D, 0.0D));
        Vec3 allowedPlayerAscent = tower.collisionTransform().directionToLocal(
                StructureCollisionResolver.resolveMovement(
                        tower, playerBox, playerAscent, 0.0D).allowed());
        helper.assertTrue(allowedPlayerAscent.y > 2.999D,
                "The third-floor opening does not fit a player-sized hitbox: "
                        + allowedPlayerAscent);

        Vec3 ladderStart = tower.collisionTransform().toWorld(
                new Vec3(-3.2D, 12.5D, 1.43D));
        occupant.setPos(ladderStart.x, ladderStart.y, ladderStart.z);
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add upper-ladder occupant");
        helper.assertTrue(StructureClimbingSystem.isOnClimbable(occupant),
                "Upper ladder did not accept the test occupant");

        Vec3 climbRequest = tower.collisionTransform().directionToWorld(
                new Vec3(-0.1D, -0.08D, 0.0D));
        Vec3 localClimb = tower.collisionTransform().directionToLocal(
                StructureClimbingSystem.applyClimbableVelocity(occupant, climbRequest));
        helper.assertTrue(localClimb.y >= 0.19D,
                "Upper ladder did not provide full climb speed: " + localClimb);

        Vec3 beforeLadderCarry = occupant.position();
        occupant.setOnGround(false);
        occupant.setDeltaMovement(Vec3.ZERO);
        prepareTowerMovement(tower);
        tower.setPos(tower.getX() + 0.2D, tower.getY(), tower.getZ());
        StructureMotionSystem.tickStructure(tower);
        helper.assertTrue(Math.abs(occupant.getX() - beforeLadderCarry.x - 0.2D) < 0.01D,
                "Moving tower left the upper-ladder climber behind: delta="
                        + occupant.position().subtract(beforeLadderCarry));
        Vec3 carriedOnLadder = tower.collisionTransform().toLocal(occupant.position());
        helper.assertTrue(carriedOnLadder.distanceTo(new Vec3(-3.2D, 12.5D, 1.43D)) < 0.02D,
                "Upper-ladder carry changed the climber's local position: " + carriedOnLadder);

        Vec3 belowRelease = tower.collisionTransform().toWorld(
                new Vec3(-3.2D, 17.35D, 1.43D));
        occupant.setPos(belowRelease.x, belowRelease.y, belowRelease.z);
        Vec3 finalClimb = tower.collisionTransform().directionToLocal(
                StructureClimbingSystem.applyClimbableVelocity(occupant, climbRequest));
        helper.assertTrue(finalClimb.y >= 0.19D,
                "Upper ladder released before the third-floor opening: " + finalClimb);

        Vec3 releasePoint = tower.collisionTransform().toWorld(
                new Vec3(-3.2D, 17.45D, 1.43D));
        occupant.setPos(releasePoint.x, releasePoint.y, releasePoint.z);
        Vec3 released = tower.collisionTransform().directionToLocal(
                StructureClimbingSystem.applyClimbableVelocity(occupant, climbRequest));
        helper.assertTrue(released.y < -0.079D,
                "Upper ladder kept the climber attached above the third-floor opening: " + released);

        Vec3 beforeHandoffCarry = occupant.position();
        prepareTowerMovement(tower);
        tower.setPos(tower.getX() + 0.2D, tower.getY(), tower.getZ());
        StructureMotionSystem.tickStructure(tower);
        helper.assertTrue(Math.abs(occupant.getX() - beforeHandoffCarry.x - 0.2D) < 0.01D,
                "Moving tower dropped the climber during the upper-ladder handoff");

        Vec3 floorPoint = tower.collisionTransform().toWorld(
                new Vec3(-2.3D, 17.45D, 1.43D));
        occupant.setPos(floorPoint.x, floorPoint.y, floorPoint.z);
        Vec3 fall = tower.collisionTransform().directionToWorld(new Vec3(0.0D, -1.0D, 0.0D));
        StructureCollisionResolver.ResolvedMovement landing =
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), fall, 0.0D);
        helper.assertTrue(landing.isSupported(),
                "Upper-ladder handoff did not find the third-floor surface");
        occupant.setPos(occupant.getX() + landing.allowed().x,
                occupant.getY() + landing.allowed().y,
                occupant.getZ() + landing.allowed().z);
        Vec3 supported = tower.collisionTransform().toLocal(occupant.position());
        helper.assertTrue(supported.y > 17.1D && supported.y < 17.3D,
                "Upper-ladder handoff fell through or hovered above the third floor: " + supported);
        CollisionGroup floorSupport = StructureCollisionResolver.supportingGroup(
                tower, occupant.getBoundingBox(), 0.03D);
        helper.assertTrue(floorSupport != null && floorSupport.name().equals("body"),
                "Upper-ladder handoff did not settle onto the tower floor");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void translatingAndLoweringRampReleasesJump(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.PIG.create(level);
        helper.assertTrue(tower != null && occupant != null,
                "Failed to create animated ramp test entities");

        setBridgeProgress(tower, 0.875F);
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        occupant.setNoAi(true);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add animated ramp tower");
        tower.tick();
        Vec3 surface = bridgeSurface(tower, bridgeGroup(tower));
        occupant.setPos(surface.x, surface.y + 2.0D, surface.z);
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add animated ramp occupant");
        landOnRamp(tower, occupant, helper);

        prepareTowerMovement(tower);
        occupant.setOnGround(false);
        occupant.setDeltaMovement(Vec3.ZERO);
        tower.setPos(tower.getX() + 0.2D, tower.getY(), tower.getZ());
        setBridgeProgress(tower, 0.88125F);
        StructureMotionSystem.tickStructure(tower);

        Vec3 jump = tower.collisionTransform().directionToWorld(
                new Vec3(0.0D, 0.42D, 0.02D));
        double beforeJump = occupant.getY();
        occupant.setDeltaMovement(jump);
        occupant.move(MoverType.SELF, jump);
        helper.assertTrue(occupant.getY() > beforeJump + 0.419D && !occupant.onGround(),
                "Simultaneously translating and lowering ramp kept the occupant attached");
        helper.succeed();
    }

    private static CollisionGroup bridgeGroup(SiegeTowerEntity tower) {
        return tower.collisionGroups().stream()
                .filter(group -> group.name().equals("bridge"))
                .findFirst()
                .orElseThrow();
    }

    private static Vec3 bridgeSurface(SiegeTowerEntity tower, CollisionGroup bridge) {
        CollisionPart bridgePart = bridge.parts().get(0);
        AABB bridgeBox = bridgePart.box();
        Vec3 groupSurface = new Vec3(0.0D,
                (bridgeBox.minY + bridgeBox.maxY) * 0.5D, bridgeBox.minZ);
        return tower.collisionTransform().toWorld(bridge.fromPart(bridgePart, groupSurface));
    }

    private static void landOnRamp(SiegeTowerEntity tower, Mob occupant, GameTestHelper helper) {
        StructureCollisionResolver.ResolvedMovement landing = StructureCollisionResolver.resolveMovement(
                tower, occupant.getBoundingBox(), new Vec3(0.0D, -3.0D, 0.0D), 0.0D);
        occupant.setPos(occupant.getX() + landing.allowed().x,
                occupant.getY() + landing.allowed().y,
                occupant.getZ() + landing.allowed().z);
        helper.assertTrue(landing.isSupported(), "Low ramp did not provide exact model support");
    }

    private static void prepareTowerMovement(SiegeTowerEntity tower) {
        tower.xo = tower.getX();
        tower.yo = tower.getY();
        tower.zo = tower.getZ();
        tower.yBodyRotO = tower.getVisualRotationYInDegrees();
    }

    private static void setBridgeProgress(SiegeTowerEntity tower, float progress) {
        CompoundTag tag = new CompoundTag();
        tower.addAdditionalSaveData(tag);
        tag.putBoolean("BridgeOpen", true);
        tag.putInt("BridgePreset", 3);
        tag.putFloat("BridgeProgress", progress);
        tag.putFloat("BridgeTargetProgress", progress);
        tower.readAdditionalSaveData(tag);
    }

    private static void buildFloor(GameTestHelper helper) {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void buildLanding(ServerLevel level, Vec3 surface, double landingY) {
        BlockPos center = BlockPos.containing(surface.x, landingY - 1.0D, surface.z);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlockAndUpdate(center.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static void moveToRelative(GameTestHelper helper, SiegeTowerEntity tower,
                                       double x, double y, double z) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        tower.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }
}
