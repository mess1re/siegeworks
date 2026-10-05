package me.mss1r.siegeworks.gametest;

import dev.architectury.platform.Platform;

import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.api.BlueprintStacks;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionDeployer;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.ConstructionWork;
import me.mss1r.axiomata.blueprint.api.construction.UnderConstruction;
import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.api.SiegeAmmunitionMode;
import me.mss1r.siegeworks.api.SiegeClimbableControl;
import me.mss1r.siegeworks.api.MountedSiegeItemControl;
import me.mss1r.siegeworks.api.SiegeOperationState;
import me.mss1r.siegeworks.api.SiegeOperatorRegistry;
import me.mss1r.siegeworks.data.profile.ProjectilePhysicsProfile;
import me.mss1r.siegeworks.data.profile.ProjectileVariants;
import me.mss1r.siegeworks.data.profile.SiegeEngineProfile;
import me.mss1r.siegeworks.data.profile.SiegeProfileCatalogs;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.entity.projectile.TowerCrossbowBoltProjectile;
import me.mss1r.siegeworks.entity.projectile.CannonProjectile;
import me.mss1r.siegeworks.entity.projectile.ScattershotProjectile;
import me.mss1r.siegeworks.entity.projectile.SingijeonProjectile;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.entity.siege.ArcballistaEntity;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.entity.siege.CulverinEntity;
import me.mss1r.siegeworks.entity.siege.TowerCrossbowEntity;
import me.mss1r.siegeworks.entity.siege.SerpentineEntity;
import me.mss1r.siegeworks.entity.siege.HwachaEntity;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.entity.siege.MantletEntity;
import me.mss1r.siegeworks.entity.siege.MonsMegEntity;
import me.mss1r.siegeworks.entity.siege.SiegeLadderEntity;
import me.mss1r.siegeworks.entity.siege.SiegeTowerEntity;
import me.mss1r.siegeworks.entity.siege.TrebuchetEntity;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.block.StackedProjectileBlock;
import me.mss1r.siegeworks.gameplay.ballistics.ScattershotVolley;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBlockBreaker;
import me.mss1r.siegeworks.entity.base.AbstractSiegeEntity;
import me.mss1r.axiomata.collision.system.VirtualPlatformSupport;
import me.mss1r.siegeworks.gameplay.damage.StructuralDamageSystem;
import me.mss1r.axiomata.collision.CollisionGroup;
import me.mss1r.axiomata.collision.CollisionPart;
import me.mss1r.axiomata.collision.system.StructureCollisionSystem;
import me.mss1r.axiomata.collision.system.StructureMotionSystem;
import me.mss1r.axiomata.collision.system.StructureClimbingSystem;
import me.mss1r.axiomata.collision.StructureCollisionResolver;
import me.mss1r.siegeworks.gameplay.movement.SiegeMovementPhysics;
import me.mss1r.siegeworks.item.SiegeAmmo;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.gameplay.deployment.SiegeDeploymentLimits;
import me.mss1r.siegeworks.gameplay.maintenance.SiegeMaintenanceData;
import me.mss1r.siegeworks.gameplay.ownership.SiegeAccess;
import me.mss1r.siegeworks.gameplay.ownership.SiegeCaptureController;
import me.mss1r.siegeworks.gameplay.ownership.SiegeOwnerActivity;
import me.mss1r.siegeworks.gameplay.ownership.SiegeRelation;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
//? if forge {
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

import java.util.UUID;

@GameTestHolder(Siegeworks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiegeworksGameTests {
    private static final String TEST_OPERATOR_TAG = "siegeworks_test_operator";

    static {
        SiegeOperatorRegistry.register(MinecraftVersionCompat.id(Siegeworks.MOD_ID, "gametest_operator"),
                entity -> entity.getTags().contains(TEST_OPERATOR_TAG));
    }

    private SiegeworksGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void gameplayProfilesAreLoadedAsCompleteCatalogs(GameTestHelper helper) {
        helper.assertTrue(SiegeProfileCatalogs.ENGINES.snapshot().size() == 12,
                "Expected the complete siege-engine profile catalog");
        helper.assertTrue(SiegeProfileCatalogs.PROJECTILES.snapshot().size() == 13,
                "Expected the complete projectile-physics profile catalog");

        SiegeEngineProfile culverin = SiegeProfileCatalogs.ENGINES.forEntity(
                SiegeworksEntities.CULVERIN_ENTITY.get());
        helper.assertTrue(Math.abs(culverin.baseDamage() - 78.0D) < 1.0E-6D,
                "Culverin did not receive its data-pack damage profile");
        helper.assertTrue(culverin.scattershot().capacity() == 12,
                "Culverin did not receive its data-pack scattershot profile");

        ProjectilePhysicsProfile cannonBall = SiegeProfileCatalogs.PROJECTILES.forEntity(
                SiegeworksEntities.CANNON_BALL.get());
        helper.assertTrue(Math.abs(cannonBall.mass() - 8.0D) < 1.0E-6D,
                "Cannon ball did not receive its data-pack physics profile");
        helper.assertTrue(SiegeProfileCatalogs.PROJECTILES.get(ProjectileVariants.EXPLOSIVE_SINGIJEON)
                        .blast().energy() > 0.0D,
                "The explosive singijeon did not receive its own profile");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void structuralDamageDoesNotLeakIntoReplacementBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relativePos = new BlockPos(2, 1, 2);
        BlockPos absolutePos = helper.absolutePos(relativePos);

        helper.setBlock(relativePos, Blocks.STONE);
        float stoneHardness = level.getBlockState(absolutePos).getDestroySpeed(level, absolutePos);
        helper.assertTrue(StructuralDamageSystem.applyImpact(level, absolutePos, 0.6F, stoneHardness)
                        == StructuralDamageSystem.ImpactResult.ACCUMULATED,
                "First structural impact should have been accumulated");

        helper.setBlock(relativePos, Blocks.COBBLESTONE);
        StructuralDamageSystem.tick(level);
        float cobblestoneHardness = level.getBlockState(absolutePos).getDestroySpeed(level, absolutePos);
        helper.assertTrue(StructuralDamageSystem.applyImpact(level, absolutePos, 1.3F, cobblestoneHardness)
                        == StructuralDamageSystem.ImpactResult.ACCUMULATED,
                "Replacement block inherited structural damage from the old block");
        helper.assertTrue(StructuralDamageSystem.applyImpact(level, absolutePos, 0.8F, cobblestoneHardness)
                        == StructuralDamageSystem.ImpactResult.BREAK_BLOCK,
                "Fresh impacts did not reach the structural break threshold");
        helper.assertTrue(level.getBlockState(absolutePos).is(Blocks.COBBLESTONE),
                "The accumulator destroyed a block instead of returning a break decision");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void cyclicMachineOwnershipHasNoPlayerAttribution(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity first = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        ArcballistaEntity second = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(first != null && second != null,
                "Failed to create cyclic ownership test machines");
        moveToRelative(helper, first, 6.0D, 1.0D, 6.0D);
        moveToRelative(helper, second, 10.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(first) && level.addFreshEntity(second),
                "Failed to add cyclic ownership test machines");
        first.setOperator(second);
        second.setOperator(first);

        helper.assertTrue(SiegeBlockBreaker.responsiblePlayer(first) == null,
                "Cyclic machine ownership produced a player attribution");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void operatingAnOwnedEngineDoesNotTransferOwnership(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(ballista != null, "Failed to create ownership test ballista");
        moveToRelative(helper, ballista, 6.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(ballista), "Failed to add ownership test ballista");

        Player first = SiegeGameTestPlayers.createRideable(level);
        Player second = SiegeGameTestPlayers.createRideable(level);
        ballista.setOperator(first);
        helper.assertTrue(ballista.isOwnedBy(first.getUUID()),
                "An unowned engine was not claimed by its first operator");
        ballista.setOperator(second);
        helper.assertTrue(ballista.isOwnedBy(first.getUUID()),
                "A later operator took ownership of the engine");
        helper.assertTrue(ballista.getOperator() == second,
                "The operator did not follow the latest crew");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void onlyTheOwnersSideMayUseAnOwnedEngine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(ballista != null, "Failed to create access test ballista");
        moveToRelative(helper, ballista, 6.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(ballista), "Failed to add access test ballista");

        net.minecraft.server.level.ServerPlayer owner = SiegeGameTestPlayers.create(level);
        level.players().add(owner);
        Player teammate = SiegeGameTestPlayers.createRideable(level);
        Player stranger = SiegeGameTestPlayers.createRideable(level);
        PlayerTeam team = level.getScoreboard().getPlayerTeam("siege_access_test");
        if (team == null) {
            team = level.getScoreboard().addPlayerTeam("siege_access_test");
        }
        level.getScoreboard().addPlayerToTeam(owner.getScoreboardName(), team);
        level.getScoreboard().addPlayerToTeam(teammate.getScoreboardName(), team);
        ballista.setOwnerUuid(owner.getUUID());

        helper.assertTrue(SiegeAccess.relationOf(owner.getUUID(), ballista) == SiegeRelation.OWNER,
                "The owner was not recognised as the owner");
        helper.assertTrue(SiegeAccess.relationOf(teammate, ballista) == SiegeRelation.FRIENDLY,
                "A teammate of the owner was not recognised as friendly");
        helper.assertTrue(SiegeAccess.allows(teammate, ballista, SiegeAccess.Action.DISMANTLE),
                "A teammate could not dismantle the owner's engine");
        helper.assertTrue(!SiegeAccess.allows(stranger, ballista, SiegeAccess.Action.USE),
                "A stranger could use an owned engine");
        helper.assertTrue(!ballista.canStartAutomatedDismantling(stranger),
                "A stranger could start dismantling an owned engine");
        helper.assertTrue(!SiegeAccess.allows(stranger, ballista, SiegeAccess.Action.REPAIR),
                "A stranger could repair an owned engine");
        level.players().remove(owner);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 360)
    public static void seatedCaptureRestartsWhenHurtAndThenTakesTheEngine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(ballista != null, "Failed to create capture test ballista");
        moveToRelative(helper, ballista, 6.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(ballista), "Failed to add capture test ballista");

        net.minecraft.server.level.ServerPlayer owner = SiegeGameTestPlayers.create(level);
        level.players().add(owner);
        ballista.setOwnerUuid(owner.getUUID());
        Player enemy = SiegeGameTestPlayers.createRideable(level);
        enemy.setPos(ballista.getX() + 1.0D, ballista.getY(), ballista.getZ());

        ballista.interact(enemy, InteractionHand.MAIN_HAND);
        helper.assertTrue(enemy.getVehicle() == ballista, "An enemy could not take the seat of an unmanned engine");

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(ballista.isBeingCaptured(), "Taking the seat did not start a capture");
            helper.assertTrue(!ballista.shouldPassengerControlRotation(enemy),
                    "The capturer could aim the engine before capturing it");
        });
        helper.runAfterDelay(60, () -> enemy.hurt(level.damageSources().generic(), 1.0F));
        helper.runAfterDelay(200, () -> helper.assertTrue(ballista.isOwnedBy(owner.getUUID()),
                "The capture completed although the capturer was hurt halfway through"));
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(ballista.isOwnedBy(enemy.getUUID()), "Holding the controls unhurt did not capture the engine");
            helper.assertTrue(!ballista.isBeingCaptured() && ballista.shouldPassengerControlRotation(enemy),
                    "The new owner did not get the controls after the capture");
            level.players().remove(owner);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void captureNeedsAnEmptyEngineAndAnOnlineDefender(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArcballistaEntity ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(ballista != null, "Failed to create capture refusal ballista");
        moveToRelative(helper, ballista, 6.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(ballista), "Failed to add capture refusal ballista");

        net.minecraft.server.level.ServerPlayer owner = SiegeGameTestPlayers.create(level);
        level.players().add(owner);
        Player defender = SiegeGameTestPlayers.createRideable(level);
        Player enemy = SiegeGameTestPlayers.createRideable(level);
        PlayerTeam team = level.getScoreboard().getPlayerTeam("siege_capture_test");
        if (team == null) {
            team = level.getScoreboard().addPlayerTeam("siege_capture_test");
        }
        level.getScoreboard().addPlayerToTeam(owner.getScoreboardName(), team);
        level.getScoreboard().addPlayerToTeam(defender.getScoreboardName(), team);
        ballista.setOwnerUuid(owner.getUUID());

        helper.assertTrue(defender.startRiding(ballista), "The owner's teammate could not man the engine");
        helper.assertTrue(ballista.captureRefusal(enemy) == SiegeCaptureController.Refusal.DEFENDED,
                "An enemy could start capturing a manned engine");
        helper.assertTrue(!ballista.canAddPassenger(enemy), "An enemy could board a manned engine");

        defender.stopRiding();
        level.players().remove(owner);
        helper.assertTrue(ballista.captureRefusal(enemy) == SiegeCaptureController.Refusal.DEFENDERS_OFFLINE,
                "An enemy could capture an engine while nobody on its side was online");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void engineIsReleasedOnlyWhenItsWholeSideStoppedPlaying(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long now = System.currentTimeMillis();
        long day = java.util.concurrent.TimeUnit.DAYS.toMillis(1);
        UUID owner = UUID.randomUUID();
        ArcballistaEntity ballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        ArcballistaEntity teamBallista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(ballista != null && teamBallista != null, "Failed to create abandonment test engines");
        ballista.setOwnerUuid(owner);
        teamBallista.setOwnerUuid(owner);

        SiegeOwnerActivity.record(level, owner, "SiegeAbandonOwner", now - 2 * day);
        helper.assertTrue(!ballista.releaseIfAbandoned(level, now), "An engine of a recently seen owner was released");

        SiegeOwnerActivity.record(level, owner, "SiegeAbandonOwner", now - 30 * day);
        PlayerTeam team = level.getScoreboard().getPlayerTeam("siege_abandon_test");
        if (team == null) {
            team = level.getScoreboard().addPlayerTeam("siege_abandon_test");
        }
        level.getScoreboard().addPlayerToTeam("SiegeAbandonOwner", team);
        level.getScoreboard().addPlayerToTeam("SiegeAbandonMate", team);
        SiegeOwnerActivity.record(level, UUID.randomUUID(), "SiegeAbandonMate", now - day);
        helper.assertTrue(!teamBallista.releaseIfAbandoned(level, now),
                "An engine was released while a teammate of its owner still played");

        level.getScoreboard().removePlayerFromTeam("SiegeAbandonMate", team);
        helper.assertTrue(ballista.releaseIfAbandoned(level, now) && ballista.getOwnerUuid() == null,
                "An engine of a side gone for a month was not released");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void carriedLadderCanBeSetUpByItsOwnersSide(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        net.minecraft.server.level.ServerPlayer owner = SiegeGameTestPlayers.create(level);
        level.players().add(owner);
        Player teammate = SiegeGameTestPlayers.createRideable(level);
        Player stranger = SiegeGameTestPlayers.createRideable(level);
        PlayerTeam team = level.getScoreboard().getPlayerTeam("siege_ladder_side_test");
        if (team == null) {
            team = level.getScoreboard().addPlayerTeam("siege_ladder_side_test");
        }
        level.getScoreboard().addPlayerToTeam(owner.getScoreboardName(), team);
        level.getScoreboard().addPlayerToTeam(teammate.getScoreboardName(), team);

        ItemStack ladder = SiegeLadderDeploymentItem.withRelocationOwner(
                new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), owner.getUUID());
        helper.assertTrue(SiegeLadderDeploymentItem.canBePlacedBy(ladder, teammate),
                "A teammate could not set up a ladder carried from the owner's side");
        helper.assertTrue(!SiegeLadderDeploymentItem.canBePlacedBy(ladder, stranger),
                "A stranger could set up a ladder that belongs to another side");
        level.players().remove(owner);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void engineOwnershipPersistsAndMigratesLegacySaves(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.randomUUID();
        ArcballistaEntity original = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        helper.assertTrue(original != null, "Failed to create ownership persistence ballista");
        CompoundTag unowned = new CompoundTag();
        original.addAdditionalSaveData(unowned);

        original.setOwnerUuid(owner);
        CompoundTag saved = new CompoundTag();
        original.addAdditionalSaveData(saved);
        ArcballistaEntity restored = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.isOwnedBy(owner), "Engine ownership did not survive NBT persistence");

        CompoundTag legacyDeployment = unowned.copy();
        legacyDeployment.putUUID("DeploymentOwner", owner);
        legacyDeployment.putString("DeploymentGroup", "player:" + owner);
        ArcballistaEntity deployed = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        deployed.readAdditionalSaveData(legacyDeployment);
        helper.assertTrue(deployed.isOwnedBy(owner), "A legacy deployment owner was not migrated");

        CompoundTag legacyOperator = unowned.copy();
        legacyOperator.putUUID("Owner", UUID.randomUUID());
        ArcballistaEntity operated = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        operated.readAdditionalSaveData(legacyOperator);
        helper.assertTrue(operated.getOwnerUuid() == null,
                "A legacy operator that is not a known player became the owner");

        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        helper.assertTrue(ladder != null, "Failed to create ownership persistence ladder");
        CompoundTag legacyLadder = new CompoundTag();
        ladder.addAdditionalSaveData(legacyLadder);
        legacyLadder.putUUID("RelocationOwner", owner);
        SiegeLadderEntity restoredLadder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        restoredLadder.readAdditionalSaveData(legacyLadder);
        helper.assertTrue(restoredLadder.isOwnedBy(owner), "A legacy ladder relocation owner was not migrated");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mantletProtectionFollowsBothOpenings(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MantletEntity mantlet = SiegeworksEntities.MANTLET_ENTITY.get().create(level);
        helper.assertTrue(mantlet != null, "Failed to create mantlet");
        moveToRelative(helper, mantlet, 8.0D, 1.0D, 8.0D);
        mantlet.setYRot(0.0F);
        helper.assertTrue(level.addFreshEntity(mantlet), "Failed to add mantlet to the level");

        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(operator != null, "Failed to create mantlet operator");
        operator.addTag(TEST_OPERATOR_TAG);
        moveToRelative(helper, operator, 8.0D, 1.0D, 10.0D);
        helper.assertTrue(level.addFreshEntity(operator), "Failed to add mantlet operator");
        helper.assertTrue(operator.startRiding(mantlet), "Mantlet operator could not board");
        TestArrow passengerArrow = new TestArrow(level);
        passengerArrow.setOwner(operator);
        helper.assertTrue(passengerArrow.canHitTarget(mantlet),
                "A passenger projectile ignored the mantlet before leaving its owner");

        helper.assertTrue(!mantletAllowsShotAt(mantlet, 20.0D, 20.0D, true),
                "The fixed shield did not block a shot from the front");
        helper.assertTrue(!mantletAllowsShotAt(mantlet, 20.0D, 20.0D, false),
                "The fixed shield did not block a shot from the rear");
        helper.assertTrue(!mantletAllowsShotAt(mantlet, 0.0D, 20.0D, true),
                "The closed flap did not block the large opening");
        helper.assertTrue(mantletAllowsShotAt(mantlet, 0.0D, 28.0D, true),
                "The small firing slit did not allow a shot from the front");
        helper.assertTrue(mantletAllowsShotAt(mantlet, 0.0D, 28.0D, false),
                "The small firing slit did not allow a shot from the rear");

        CompoundTag tag = new CompoundTag();
        mantlet.addAdditionalSaveData(tag);
        tag.putFloat("FlapAngle", 75.0F);
        mantlet.readAdditionalSaveData(tag);
        helper.assertTrue(mantletAllowsShotAt(mantlet, 0.0D, 20.0D, true),
                "The raised flap did not expose the large firing opening");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void differentialEnginesPivotInPlace(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();

        assertStationaryPivot(helper, level, SiegeworksEntities.SERPENTINE_ENTITY.get().create(level), 3.0D);
        assertStationaryPivot(helper, level, SiegeworksEntities.CULVERIN_ENTITY.get().create(level), 5.5D);
        assertStationaryPivot(helper, level, SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level), 8.0D);
        assertStationaryPivot(helper, level, SiegeworksEntities.HWACHA_ENTITY.get().create(level), 17.0D);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void steeringScalesWithTravelSpeed(GameTestHelper helper) {
        float halfForward = SiegeMovementPhysics.scaleSteeringForTravel(1.0F, 0.05D, 0.10D, 0.55D);
        float fullForward = SiegeMovementPhysics.scaleSteeringForTravel(1.0F, 0.10D, 0.10D, 0.55D);
        float fullReverse = SiegeMovementPhysics.scaleSteeringForTravel(1.0F, -0.055D, 0.10D, 0.55D);
        float stopped = SiegeMovementPhysics.scaleSteeringForTravel(1.0F, 0.0D, 0.10D, 0.55D);

        helper.assertTrue(Math.abs(halfForward - 0.5F) < 1.0E-6F,
                "Half travel speed did not produce half steering speed");
        helper.assertTrue(Math.abs(fullForward - 1.0F) < 1.0E-6F,
                "Full forward speed did not preserve the configured steering speed");
        helper.assertTrue(Math.abs(fullReverse - 1.0F) < 1.0E-6F,
                "Full reverse speed did not use the reverse-speed limit for steering normalization");
        helper.assertTrue(stopped == 0.0F,
                "A stopped non-pivoting engine retained travel steering");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void draftMountsRequireRidersToControlSieges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        AbstractSiegeEntity[] sieges = {
                SiegeworksEntities.MANGONEL_ENTITY.get().create(level),
                SiegeworksEntities.HWACHA_ENTITY.get().create(level),
                SiegeworksEntities.MANTLET_ENTITY.get().create(level),
                SiegeworksEntities.SERPENTINE_ENTITY.get().create(level),
                SiegeworksEntities.CULVERIN_ENTITY.get().create(level),
                SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level),
                SiegeworksEntities.MONS_MEG_ENTITY.get().create(level)
        };

        for (AbstractSiegeEntity siege : sieges) {
            Horse horse = EntityType.HORSE.create(level);
            helper.assertTrue(siege != null && horse != null, "Failed to create draft-control test entities");
            helper.assertTrue(siege.canAddPassenger(horse),
                    siege.getType() + " rejected a valid draft mount");
            helper.assertTrue(!siege.shouldPassengerControlMovement(horse),
                    siege.getType() + " accepted movement input from an unridden draft mount");
            helper.assertTrue(!siege.shouldPassengerControlRotation(horse),
                    siege.getType() + " accepted steering input from an unridden draft mount");
        }

        ArcballistaEntity arcballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        Horse horse = EntityType.HORSE.create(level);
        helper.assertTrue(arcballista != null && horse != null,
                "Failed to create Arcballista travel-pitch test entities");
        arcballista.setTrackedPitch(-20.0F);
        helper.assertTrue(horse.startRiding(arcballista, true),
                "Arcballista rejected its draft mount during the travel-pitch test");
        arcballista.onSiegeTick(level);
        helper.assertTrue(arcballista.getTrackedPitch() > -20.0F,
                "Towed Arcballista did not begin returning its base to the default pitch");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void batteringRamUsesTwoMountTeam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BatteringRamEntity ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        Horse first = EntityType.HORSE.create(level);
        Horse second = EntityType.HORSE.create(level);
        Horse extra = EntityType.HORSE.create(level);
        helper.assertTrue(ram != null && first != null && second != null && extra != null,
                "Failed to create the battering-ram draft team");

        ram.applyYaw(35.0F);
        Vec3 parkedCollisionForward = ram.collisionTransform()
                .directionToWorld(new Vec3(0.0D, 0.0D, 1.0D));
        helper.assertTrue(first.startRiding(ram, true), "Ram rejected its first draft mount");
        Vec3 towedCollisionForward = ram.collisionTransform()
                .directionToWorld(new Vec3(0.0D, 0.0D, 1.0D));
        helper.assertTrue(parkedCollisionForward.dot(towedCollisionForward) < -0.999D,
                "Ram collision did not follow the model's towing half-turn");
        Vec3 firstOffset = ram.getPassengerOffset(first);
        double oneMountSpeed = ram.getVelocity(first);
        float oneMountTurn = ram.getSteeringSpeedDegrees(first);
        helper.assertTrue(firstOffset.x > 0.0D, "First ram mount was assigned to the wrong side");

        helper.assertTrue(second.startRiding(ram, true), "Ram rejected its second draft mount");
        Vec3 secondOffset = ram.getPassengerOffset(second);
        double twoMountSpeed = ram.getVelocity(first);
        float twoMountTurn = ram.getSteeringSpeedDegrees(first);
        helper.assertTrue(secondOffset.x < 0.0D, "Second ram mount was assigned to the wrong side");
        helper.assertTrue(twoMountSpeed > 0.0D, "Configured ram draft speed was not positive");
        helper.assertTrue(Math.abs(twoMountSpeed - oneMountSpeed * 2.0D) < 1.0E-8D,
                "Second ram mount did not double the available draft speed");
        helper.assertTrue(Math.abs(twoMountTurn - oneMountTurn * 2.0F) < 1.0E-6F,
                "Second ram mount did not double the available steering power");
        helper.assertTrue(!ram.canAddPassenger(extra), "Ram accepted more than two draft mounts");
        helper.assertTrue(ram.getTowingMount() == first,
                "The first ram mount was not retained as the reins/control mount");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aPlayerOnAnyMountOfTheTeamHoldsTheReins(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BatteringRamEntity ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        Horse first = EntityType.HORSE.create(level);
        Horse second = EntityType.HORSE.create(level);
        helper.assertTrue(ram != null && first != null && second != null, "Failed to create the ram team");
        helper.assertTrue(first.startRiding(ram, true) && second.startRiding(ram, true),
                "The ram did not take its two mounts");

        Player driver = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(driver.startRiding(second, true), "The player could not climb on the second mount");
        helper.assertTrue(ram.getReinsHolder() == driver && ram.shouldPassengerControlMovement(second)
                        && !ram.shouldPassengerControlMovement(first),
                "A player on the second mount does not drive the ram");

        Villager rider = EntityType.VILLAGER.create(level);
        helper.assertTrue(rider != null && rider.startRiding(first, true), "Nobody could sit on the first mount");
        helper.assertTrue(ram.getReinsHolder() == driver,
                "Someone else on the first mount took the reins from the player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void monsMegTowingKeepsModelAtEntityOrigin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MonsMegEntity monsMeg = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
        Horse first = EntityType.HORSE.create(level);
        Horse extra = EntityType.HORSE.create(level);
        helper.assertTrue(monsMeg != null && first != null && extra != null,
                "Failed to create Mons Meg towing test entities");

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        monsMeg.setPos(origin.getX() + 8.0D, origin.getY() + 4.0D, origin.getZ() + 8.0D);
        helper.assertTrue(first.startRiding(monsMeg, true),
                "Mons Meg rejected its first draft mount");
        Vec3 offset = monsMeg.getPassengerOffset(first);
        helper.assertTrue(Math.abs(offset.x) < 1.0E-8D,
                "Mons Meg's draft mount is not centered");
        helper.assertTrue(Math.abs(offset.z + 4.15D) < 1.0E-8D,
                "Mons Meg's draft mount lost its towing distance");
        helper.assertTrue(monsMeg.getVelocity(first) > 0.0D
                        && monsMeg.getSteeringSpeedDegrees(first) > 0.0F,
                "Mons Meg's draft mount provides no movement or steering");
        helper.assertTrue(!monsMeg.canAddPassenger(extra),
                "Mons Meg accepted a second draft mount");
        Vec3 collisionOrigin = monsMeg.collisionTransform().toWorld(Vec3.ZERO);
        helper.assertTrue(collisionOrigin.distanceToSqr(monsMeg.position()) < 1.0E-8D,
                "Mons Meg towing shifted its model collision away from the entity origin");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void siegeTowerUsesFourMountPushTeam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Horse[] team = new Horse[5];
        for (int index = 0; index < team.length; index++) {
            team[index] = EntityType.HORSE.create(level);
        }
        helper.assertTrue(tower != null && java.util.Arrays.stream(team).allMatch(java.util.Objects::nonNull),
                "Failed to create the siege-tower push team");

        double oneMountSpeed = 0.0D;
        float oneMountTurn = 0.0F;
        double[] expectedMountX = {-51.0D, -17.0D, 17.0D, 51.0D};
        for (int index = 0; index < 4; index++) {
            helper.assertTrue(team[index].startRiding(tower, true),
                    "Tower rejected push mount " + (index + 1));
            Vec3 offset = tower.getPassengerOffset(team[index]);
            helper.assertTrue(Math.abs(offset.x - expectedMountX[index] / 16.0D) < 1.0E-8D,
                    "Tower push mount was mirrored away from shaft pair " + (index + 1));
            helper.assertTrue(offset.z > 0.0D,
                    "Tower push mount was placed in front instead of behind");
            if (index == 0) {
                oneMountSpeed = tower.getVelocity(team[0]);
                oneMountTurn = tower.getSteeringSpeedDegrees(team[0]);
            }
        }

        double fullTeamSpeed = tower.getVelocity(team[0]);
        helper.assertTrue(fullTeamSpeed > 0.0D,
                "Configured tower draft speed was not positive");
        helper.assertTrue(Math.abs(fullTeamSpeed - oneMountSpeed * 4.0D) < 1.0E-8D,
                "Four tower mounts did not provide four times one mount's traction");
        helper.assertTrue(oneMountTurn > 0.0F,
                "Tower mount steering was incorrectly disabled by the absent human crew");
        helper.assertTrue(Math.abs(tower.getSteeringSpeedDegrees(team[0]) - oneMountTurn * 4.0F) < 1.0E-6F,
                "Four tower mounts did not provide four times one mount's steering power");
        helper.assertTrue(!tower.canAddPassenger(team[4]),
                "Tower accepted more than four push mounts");
        helper.assertTrue(tower.towingProfile().modelTurnDegrees() == 0.0F,
                "Tower model turned around while being pushed");
        helper.assertTrue(tower.getTowingMount() == team[0],
                "The first tower mount was not retained as the reins/control mount");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void hwachaStoresMixedAmmunitionAndParksSafely(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HwachaEntity hwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(level);
        helper.assertTrue(hwacha != null, "Failed to create Hwacha test entity");

        hwacha.setTrackedPitch(-90.0F);
        helper.assertTrue(Math.abs(hwacha.getTrackedPitch() + 32.5F) < 0.01F,
                "Hwacha exceeded its upward aim limit");
        hwacha.setTrackedPitch(90.0F);
        helper.assertTrue(Math.abs(hwacha.getTrackedPitch() - 15.0F) < 0.01F,
                "Hwacha exceeded its downward aim limit");

        CompoundTag ammunition = new CompoundTag();
        ammunition.putInt("LoadedSingijeonCount", HwachaEntity.CAPACITY);
        ammunition.putLong("ExplosiveSingijeonLow", 1L << 5);
        ammunition.putLong("ExplosiveSingijeonHigh", 1L << 2);
        ammunition.putInt("FiringSingijeonIndex", -1);
        hwacha.readAdditionalSaveData(ammunition);
        helper.assertTrue(hwacha.getLoadedCount() == HwachaEntity.CAPACITY,
                "Hwacha did not restore its full ammunition rack");
        helper.assertTrue(hwacha.isExplosive(5) && hwacha.isExplosive(66)
                        && !hwacha.isExplosive(6),
                "Hwacha did not restore mixed ammunition slots");

        hwacha.setTrackedPitch(0.0F);
        hwacha.onSiegeTick(level);
        helper.assertTrue(hwacha.getTrackedPitch() < 0.0F,
                "Unattended Hwacha did not begin returning to its parked angle");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void fieldGunsRespectCarriageElevationAndDepression(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SerpentineEntity serpentine = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        CulverinEntity culverin = SiegeworksEntities.CULVERIN_ENTITY.get().create(level);
        helper.assertTrue(serpentine != null && culverin != null,
                "Failed to create field gun test entities");

        assertFieldGunPitchLimits(helper, serpentine, "Serpentine", 12.5F, 15.0F);
        assertFieldGunPitchLimits(helper, culverin, "Culverin", 12.5F, 20.0F);
        helper.succeed();
    }

    private static void assertFieldGunPitchLimits(GameTestHelper helper,
                                                   AbstractFieldGunEntity gun,
                                                   String name,
                                                   float elevationDegrees,
                                                   float depressionDegrees) {
        helper.assertTrue(Math.abs(gun.getMinAimPitch() + elevationDegrees) < 0.01F,
                name + " elevation limit no longer matches its carriage");
        helper.assertTrue(Math.abs(gun.getMaxAimPitch() - depressionDegrees) < 0.01F,
                name + " depression limit changed unexpectedly");

        gun.setTrackedPitch(-90.0F);
        helper.assertTrue(Math.abs(gun.getTrackedPitch() - gun.getMinAimPitch()) < 0.01F,
                name + " elevated through its carriage");
        gun.setTrackedPitch(90.0F);
        helper.assertTrue(Math.abs(gun.getTrackedPitch() - gun.getMaxAimPitch()) < 0.01F,
                name + " exceeded its depression limit");
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mountedOperatorsCanIgniteGunpowderWeapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SerpentineEntity cannon = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        MonsMegEntity monsMeg = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
        HwachaEntity hwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(level);
        Player operator = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(cannon != null && monsMeg != null && hwacha != null,
                "Failed to create mounted ignition test entities");

        cannon.setLoadStage(4);
        monsMeg.setLoadStage(4);
        CompoundTag hwachaAmmunition = new CompoundTag();
        hwachaAmmunition.putInt("LoadedSingijeonCount", 1);
        hwacha.readAdditionalSaveData(hwachaAmmunition);
        operator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));

        assertMountedIgnition(helper, level, operator, cannon);
        assertMountedIgnition(helper, level, operator, monsMeg);
        assertMountedIgnition(helper, level, operator, hwacha);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void mountedOperatorsCanLoadSiegeWeapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SerpentineEntity cannon = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        Player operator = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(cannon != null && towerCrossbow != null && mangonel != null,
                "Failed to create mounted loading test entities");

        assertMountedLoading(helper, level, operator, cannon,
                new ItemStack(SiegeworksItems.BLACK_POWDER.get(), 64));
        assertMountedLoading(helper, level, operator, towerCrossbow,
                SiegeworksItems.TOWER_CROSSBOW_BOLT.get().getDefaultInstance());
        assertMountedLoading(helper, level, operator, mangonel,
                Items.STONE.getDefaultInstance());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40, batch = "manual_loading")
    public static void changingHeldAmmunitionCancelsAndRestartsHwachaLoading(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HwachaEntity hwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(level);
        FakePlayer loader = SiegeGameTestPlayers.create(level);
        helper.assertTrue(hwacha != null, "Failed to create Hwacha loading-cancellation test entity");

        Vec3 position = helper.absolutePos(new BlockPos(2, 1, 2)).getCenter();
        hwacha.setPos(position);
        loader.setPos(position);
        helper.assertTrue(level.addFreshEntity(hwacha), "Failed to add Hwacha loading-cancellation test entity");

        loader.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(SiegeworksItems.SO_SINGIJEON.get(), 2));
        helper.assertTrue(hwacha.handleSiegeInteraction(loader, InteractionHand.MAIN_HAND, level).consumesAction(),
                "Standard rocket loading did not start");
        for (int tick = 0; tick < 8; tick++) {
            hwacha.tick();
        }
        helper.assertTrue(hwacha.getLoadedCount() == 1 && !hwacha.isExplosive(0),
                "Hwacha did not retain the standard rocket inserted before cancellation");

        loader.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(SiegeworksItems.JUNG_SINGIJEON.get(), 2));
        hwacha.tick();
        helper.assertTrue(hwacha.getOperationState() == SiegeOperationState.READY,
                "Hwacha kept loading after the held ammunition changed");

        helper.assertTrue(hwacha.handleSiegeInteraction(loader, InteractionHand.MAIN_HAND, level).consumesAction(),
                "Explosive rocket loading did not replace the cancelled session");
        for (int tick = 0; tick < 8; tick++) {
            hwacha.tick();
        }
        helper.assertTrue(hwacha.getLoadedCount() == 2 && hwacha.isExplosive(1),
                "Hwacha did not continue with the newly selected ammunition type");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20, batch = "manual_loading")
    public static void singijeonConstructionDoesNotReadUninitializedSynchedData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArmorStand shooter = new ArmorStand(EntityType.ARMOR_STAND, level);
        SingijeonProjectile projectile = new SingijeonProjectile(
                SiegeworksEntities.SINGIJEON_PROJECTILE.get(), shooter, level);
        projectile.setExplosive(true);

        helper.assertTrue(projectile.isExplosive(),
                "Explosive Singijeon state was not available after construction");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void batteringRamUsesBothPushers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BatteringRamEntity ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        ArmorStand first = EntityType.ARMOR_STAND.create(level);
        ArmorStand second = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(ram != null && first != null && second != null,
                "Failed to create the battering-ram pushing crew");

        first.addTag(TEST_OPERATOR_TAG);
        second.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(first.startRiding(ram, true), "Ram rejected its first pusher");
        double onePusherSpeed = ram.getVelocity(first);
        helper.assertTrue(onePusherSpeed > 0.0D, "One ram pusher provided no traction");

        helper.assertTrue(second.startRiding(ram, true), "Ram rejected its second pusher");
        double twoPusherSpeed = ram.getVelocity(first);
        helper.assertTrue(Math.abs(twoPusherSpeed - onePusherSpeed * 2.0D) < 1.0E-8D,
                "Second ram pusher did not complete the pushing team");
        helper.assertTrue(ram.shouldPassengerBodyFollowSiege(first)
                        && ram.shouldPassengerBodyFollowSiege(second),
                "A ram pusher can turn their body away from the working pose");
        helper.assertTrue(ram.getPassengerViewYawLimit(first) == 35.0F
                        && ram.getPassengerViewPitchLimit(first) == 35.0F,
                "Ram camera limits no longer match its authored pusher pose");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void scattershotAmmunitionIsAcceptedByItsSiegeWeapons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(SiegeworksItems.CANNON_BALL.get().getDefaultInstance().getMaxStackSize() == 8,
                "Cannonballs no longer stack to eight");
        helper.assertTrue(SiegeworksItems.GIANT_CANNON_BALL.get().getDefaultInstance().getMaxStackSize() == 2,
                "Giant cannonballs no longer stack to two");
        helper.assertTrue(SiegeworksItems.GRAPESHOT.get().getDefaultInstance().getMaxStackSize() == 2,
                "Stone grapeshot no longer stacks to two");
        SerpentineEntity serpentine = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        MonsMegEntity monsMeg = SiegeworksEntities.MONS_MEG_ENTITY.get().create(level);
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        TrebuchetEntity trebuchet = SiegeworksEntities.TREBUCHET_ENTITY.get().create(level);
        Player operator = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(serpentine != null && monsMeg != null && mangonel != null && trebuchet != null,
                "Failed to create scattershot loading test entities");
        helper.assertTrue(serpentine.getScattershotProfile().pelletsPerLoadedItemMin() == 2
                        && serpentine.getScattershotProfile().pelletsPerLoadedItemMax() == 3,
                "Field-gun iron nuggets are not configured to split into two or three pellets");
        helper.assertTrue(monsMeg.getScattershotProfile().pelletsPerLoadedItemMin() == 2
                        && monsMeg.getScattershotProfile().pelletsPerLoadedItemMax() == 3,
                "Mons Meg iron nuggets are not configured to split into two or three pellets");
        helper.assertTrue(mangonel.getScattershotProfile().minPellets() == 48
                        && mangonel.getScattershotProfile().maxPellets() == 64
                        && trebuchet.getScattershotProfile().minPellets() == 48
                        && trebuchet.getScattershotProfile().maxPellets() == 64,
                "Stone grapeshot is not configured for a 48-64 pellet volley");

        serpentine.setLoadStage(2);
        monsMeg.setLoadStage(2);
        assertMountedLoading(helper, level, operator, serpentine,
                new ItemStack(Items.IRON_NUGGET));
        assertMountedLoading(helper, level, operator, monsMeg,
                new ItemStack(Items.IRON_NUGGET, 8));

        mangonel.setPos(helper.absolutePos(new BlockPos(7, 1, 7)).getCenter());
        trebuchet.setPos(helper.absolutePos(new BlockPos(11, 1, 7)).getCenter());
        level.addFreshEntity(mangonel);
        level.addFreshEntity(trebuchet);
        operator.stopRiding();
        operator.setItemInHand(InteractionHand.MAIN_HAND, SiegeworksItems.GRAPESHOT.get().getDefaultInstance());
        helper.assertTrue(mangonel.handleSiegeInteraction(operator, InteractionHand.MAIN_HAND, level).consumesAction(),
                "Mangonel rejected stone grapeshot");
        helper.assertTrue(trebuchet.handleSiegeInteraction(operator, InteractionHand.MAIN_HAND, level).consumesAction(),
                "Trebuchet rejected stone grapeshot");
        helper.assertTrue(SiegeAmmo.projectileBlockState(SiegeAmmo.AMMO_GRAPESHOT)
                        .is(SiegeworksBlocks.GRAPESHOT.get()),
                "Loaded grapeshot does not use its centred block model");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scattershotVolleyUsesIndependentSmallProjectiles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArmorStand shooter = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(shooter != null, "Failed to create scattershot test shooter");
        moveToRelative(helper, shooter, 8.0D, 4.0D, 8.0D);
        level.addFreshEntity(shooter);

        java.util.List<ScattershotProjectile> pellets = ScattershotVolley.spawn(
                level, shooter, shooter.position(), new Vec3(0.0D, 0.0D, 2.0D),
                8, 10.0F, 10.0D, true);
        helper.assertTrue(pellets.size() == 8, "Scattershot volley did not create eight pellets");
        helper.assertTrue(pellets.stream().allMatch(ScattershotProjectile::isStonePellet),
                "Stone grapeshot created an iron-textured pellet");
        ProjectilePhysicsProfile stonePellet = SiegeProfileCatalogs.PROJECTILES.get(ProjectileVariants.STONE_SCATTERSHOT);
        helper.assertTrue(pellets.stream().allMatch(pellet -> pellet.getPhysicsProfile() == stonePellet),
                "Stone grapeshot pellets did not strike by the stone pellet profile");
        int splitPellets = ScattershotVolley.rollPelletCount(level.random, 16, 2, 3);
        helper.assertTrue(splitPellets >= 32 && splitPellets <= 48,
                "Sixteen iron nuggets produced a pellet count outside the configured 32-48 range");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 150)
    public static void scattershotDoesNotExpireMidFlight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ArmorStand shooter = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(shooter != null, "Failed to create scattershot lifetime test shooter");
        moveToRelative(helper, shooter, 8.0D, 4.0D, 8.0D);
        level.addFreshEntity(shooter);

        ScattershotProjectile pellet = new ScattershotProjectile(
                SiegeworksEntities.SCATTERSHOT_PROJECTILE.get(), shooter, level);
        moveToRelative(helper, pellet, 8.0D, 6.0D, 8.0D);
        pellet.setStonePellet(true);
        pellet.setNoGravity(true);
        level.addFreshEntity(pellet);

        helper.runAfterDelay(125, () -> {
            helper.assertTrue(!pellet.isRemoved(), "Scattershot expired before a long trebuchet arc could finish");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void partialFieldGunScattershotPersistsAndCanBeRammed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SerpentineEntity original = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        SerpentineEntity restored = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        helper.assertTrue(original != null && restored != null,
                "Failed to create partial scattershot test field guns");

        original.setLoadStage(2);
        original.setAmmoLoaded(SiegeAmmo.AMMO_IRON_SCATTERSHOT);
        original.setScattershotCount(3);
        CompoundTag saved = new CompoundTag();
        original.addAdditionalSaveData(saved);
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.getScattershotCount() == 3,
                "Partial field-gun scattershot count did not survive NBT persistence");

        restored.setPos(helper.absolutePos(new BlockPos(4, 1, 4)).getCenter());
        helper.assertTrue(level.addFreshEntity(restored), "Could not add restored field gun");
        Player operator = SiegeGameTestPlayers.createRideable(level);
        operator.setPos(restored.getX(), restored.getY(), restored.getZ());
        helper.assertTrue(operator.startRiding(restored, true), mountFailure(restored, operator));
        operator.setItemInHand(InteractionHand.MAIN_HAND, SiegeworksItems.RAMROD.get().getDefaultInstance());
        helper.assertTrue(restored.acceptsMountedItem(operator, InteractionHand.MAIN_HAND),
                "Partially loaded field gun rejected the ramrod");
        helper.assertTrue(restored.handleMountedItem(operator, InteractionHand.MAIN_HAND, level).consumesAction(),
                "Partially loaded field gun did not begin ramming");
        helper.assertTrue(restored.getLoadStage() == 3,
                "Ramming partial scattershot did not advance to the projectile-ramming stage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stackedProjectileDisplaysStayCentered(GameTestHelper helper) {
        int displayIndex = 0;
        for (net.minecraft.world.level.block.Block block : java.util.List.of(
                SiegeworksBlocks.CANNON_BALL.get(), SiegeworksBlocks.GIANT_CANNON_BALL.get(),
                SiegeworksBlocks.GRAPESHOT.get())) {
            helper.assertTrue(block instanceof StackedProjectileBlock,
                    "Projectile display is not stackable: " + block);
            StackedProjectileBlock projectileBlock = (StackedProjectileBlock) block;
            helper.assertTrue(block.asItem().getDefaultInstance().getMaxStackSize() >= 2,
                    "Stackable projectile item cannot form even a two-item stack: " + block);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var state = block.defaultBlockState().setValue(StackedProjectileBlock.FACING, direction);
                AABB bounds = projectileBlock.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                        CollisionContext.empty()).bounds();
                helper.assertTrue(Math.abs((bounds.minX + bounds.maxX) * 0.5D - 0.5D) < 1.0E-6D
                                && Math.abs((bounds.minZ + bounds.maxZ) * 0.5D - 0.5D) < 1.0E-6D,
                        "Single projectile moved away from block centre when facing " + direction + ": " + bounds);
            }

            AABB northPair = projectileBlock.getShape(block.defaultBlockState()
                            .setValue(StackedProjectileBlock.FACING, Direction.NORTH)
                            .setValue(StackedProjectileBlock.COUNT, 2),
                    EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).bounds();
            AABB eastPair = projectileBlock.getShape(block.defaultBlockState()
                            .setValue(StackedProjectileBlock.FACING, Direction.EAST)
                            .setValue(StackedProjectileBlock.COUNT, 2),
                    EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).bounds();
            helper.assertTrue(Math.abs(northPair.getXsize() - eastPair.getZsize()) < 1.0E-6D
                            && Math.abs(northPair.getZsize() - eastPair.getXsize()) < 1.0E-6D,
                    "Projectile pile collision did not rotate with its model for " + block);
            if (block != SiegeworksBlocks.CANNON_BALL.get()) {
                AABB fourHigh = projectileBlock.getShape(block.defaultBlockState()
                                .setValue(StackedProjectileBlock.COUNT, 4),
                        EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()).bounds();
                helper.assertTrue(Math.abs(fourHigh.maxY - 1.0D) < 1.0E-6D,
                        "Fourth large projectile still intersects the lower row for " + block);
            }

            BlockPos relativePos = new BlockPos(2 + displayIndex * 2, 1, 2);
            BlockPos absolutePos = helper.absolutePos(relativePos);
            helper.setBlock(relativePos, block.defaultBlockState().setValue(StackedProjectileBlock.COUNT, 4));
            levelDestroyAndDrop(helper.getLevel(), absolutePos);
            int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new AABB(absolutePos).inflate(1.0D), item -> item.getItem().is(block.asItem()))
                    .stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(dropped == 4,
                    "Breaking a four-projectile display returned " + dropped + " items for " + block);
            displayIndex++;
        }
        helper.succeed();
    }

    private static void levelDestroyAndDrop(ServerLevel level, BlockPos pos) {
        level.destroyBlock(pos, true);
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerCrossbowDismountKeepsStableTransform(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        ArcballistaEntity arcballista = SiegeworksEntities.ARCBALLISTA_ENTITY.get().create(level);
        ArmorStand towerCrossbowOperator = EntityType.ARMOR_STAND.create(level);
        ArmorStand arcballistaOperator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(towerCrossbow != null && arcballista != null
                        && towerCrossbowOperator != null && arcballistaOperator != null,
                "Failed to create towerCrossbow dismount test entities");

        moveToRelative(helper, towerCrossbow, 5.0D, 1.0D, 5.0D);
        towerCrossbow.setYRot(20.0F);
        towerCrossbow.setYHeadRot(20.0F);
        towerCrossbow.setYBodyRot(20.0F);
        towerCrossbow.setTrackedYaw(95.0F);
        towerCrossbowOperator.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(level.addFreshEntity(towerCrossbow), "Failed to add towerCrossbow");
        helper.assertTrue(level.addFreshEntity(towerCrossbowOperator), "Failed to add towerCrossbow operator");
        helper.assertTrue(towerCrossbowOperator.startRiding(towerCrossbow), "Operator could not mount towerCrossbow");
        towerCrossbowOperator.stopRiding();
        helper.assertTrue(Math.abs(towerCrossbow.getYRot() - 20.0F) < 0.01F,
                "Tower Crossbow chassis snapped to the independent aiming yaw on dismount");
        helper.assertTrue(Math.abs(towerCrossbow.getTrackedYaw() - 95.0F) < 0.01F,
                "Tower Crossbow lost its aiming yaw on dismount");

        moveToRelative(helper, arcballista, 10.0D, 1.0D, 5.0D);
        arcballista.setYRot(0.0F);
        arcballista.setTrackedYaw(0.0F);
        arcballistaOperator.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(level.addFreshEntity(arcballista), "Failed to add arcballista");
        helper.assertTrue(level.addFreshEntity(arcballistaOperator), "Failed to add arcballista operator");
        helper.assertTrue(arcballistaOperator.startRiding(arcballista),
                "Operator could not mount arcballista");
        arcballista.setMovementInput(1.0F, 1.0F);
        arcballista.setCurrentDriveSpeed(0.05D);
        arcballista.setDeltaMovement(0.03D, 0.0D, 0.04D);

        Vec3 dismount = arcballista.getDismountLocationForPassenger(arcballistaOperator);
        AABB occupied = arcballista.getBoundingBox().inflate(
                arcballistaOperator.getBbWidth() * 0.5D + 0.05D, 0.0D,
                arcballistaOperator.getBbWidth() * 0.5D + 0.05D);
        helper.assertTrue(dismount.x < occupied.minX || dismount.x > occupied.maxX
                        || dismount.z < occupied.minZ || dismount.z > occupied.maxZ,
                "Arcballista dismount location still overlaps its collision area");
        double expectedDismountY = arcballista.getY()
                + arcballista.getPassengerOffset(arcballistaOperator).y;
        helper.assertTrue(Math.abs(dismount.y - expectedDismountY) < 0.01D,
                "Arcballista dismount location dropped below its intended height");

        arcballistaOperator.stopRiding();
        helper.assertTrue(arcballista.getCurrentDriveSpeed() == 0.0D,
                "Arcballista retained drive speed after its operator dismounted");
        helper.assertTrue(arcballista.getMovementInputForward() == 0.0F
                        && arcballista.getMovementInputSteering() == 0.0F,
                "Arcballista retained movement input after its operator dismounted");
        helper.assertTrue(arcballista.getDeltaMovement().horizontalDistanceSqr() == 0.0D,
                "Arcballista retained horizontal velocity after its operator dismounted");

        Vec3 towerCrossbowPosition = towerCrossbow.position();
        Vec3 arcballistaPosition = arcballista.position();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(towerCrossbow.position().distanceToSqr(towerCrossbowPosition) < 1.0E-8D,
                    "Tower Crossbow drifted after its operator dismounted");
            helper.assertTrue(arcballista.position().distanceToSqr(arcballistaPosition) < 1.0E-8D,
                    "Arcballista drifted after its operator dismounted");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void elevatedTowerCrossbowOperatorUnlocksDeeperDepression(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        ArmorStand supportedOperator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(towerCrossbow != null && operator != null && supportedOperator != null,
                "Failed to create elevated Tower Crossbow test entities");

        moveToRelative(helper, towerCrossbow, 8.0D, 1.0D, 8.0D);
        operator.setPos(towerCrossbow.getX(), towerCrossbow.getY() + 0.5D, towerCrossbow.getZ() + 3.0D);
        operator.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(level.addFreshEntity(towerCrossbow), "Failed to add Tower Crossbow");
        helper.assertTrue(level.addFreshEntity(operator), "Failed to add elevated operator");
        helper.assertTrue(operator.startRiding(towerCrossbow), "Elevated operator could not mount Tower Crossbow");
        helper.assertTrue(Math.abs(towerCrossbow.getOperatorElevation()) < 0.01F,
                "Tower Crossbow left its operator floating without support under the elevated seat");

        operator.stopRiding();
        Vec3 elevatedSeat = towerCrossbow.position().add(0.0D, 0.0D, -49.0D / 16.0D);
        level.setBlockAndUpdate(BlockPos.containing(elevatedSeat.x, towerCrossbow.getY(), elevatedSeat.z),
                Blocks.STONE_SLAB.defaultBlockState());
        supportedOperator.setPos(elevatedSeat.x, towerCrossbow.getY(), elevatedSeat.z);
        supportedOperator.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(level.addFreshEntity(supportedOperator), "Failed to add supported elevated operator");
        helper.assertTrue(supportedOperator.startRiding(towerCrossbow),
                "Operator could not mount Tower Crossbow from a supported half-block");
        helper.assertTrue(Math.abs(towerCrossbow.getOperatorElevation() - 0.5F) < 0.01F,
                "Supported half-block operator elevation was not preserved");
        helper.assertTrue(towerCrossbow.getMaxAimPitch() > 19.0F,
                "Elevated operator did not unlock a deeper downward angle");
        towerCrossbow.setTrackedPitch(towerCrossbow.getMaxAimPitch());
        helper.assertTrue(towerCrossbow.getTrackedPitch() > 19.0F,
                "Tower Crossbow geometry prevented the elevated downward angle");

        towerCrossbow.setTrackedYaw(90.0F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(towerCrossbow.getOperatorElevation()) < 0.01F,
                    "Operator did not step down after the elevated footing rotated away");
            Vec3 raisedSeat = towerCrossbow.position().add(49.0D / 16.0D, 0.0D, 0.0D);
            level.setBlockAndUpdate(BlockPos.containing(raisedSeat.x, towerCrossbow.getY(), raisedSeat.z),
                    Blocks.STONE.defaultBlockState());
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(Math.abs(towerCrossbow.getOperatorElevation() - 1.0F) < 0.01F,
                    "Operator did not step onto footing beneath the rotated seat");
            helper.assertTrue(Math.abs(supportedOperator.getY() - towerCrossbow.getY() - 1.0D) < 0.01D,
                    "Rotated Tower Crossbow left its operator inside the supporting block");
            helper.assertTrue(Math.abs(towerCrossbow.getMaxAimPitch() - 27.0F) < 0.01F,
                    "Full-block elevation did not unlock the deeper downward angle");
            towerCrossbow.setTrackedPitch(0.0F);
            towerCrossbow.setTrackedPitch(towerCrossbow.getMinAimPitch());
            helper.assertTrue(towerCrossbow.getTrackedPitch() > towerCrossbow.getMinAimPitch() + 0.1F,
                    "Tower Crossbow support did not physically limit its upward aim");
            towerCrossbow.setTrackedYaw(0.0F);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(Math.abs(towerCrossbow.getOperatorElevation() - 0.5F) < 0.01F,
                    "Operator did not return to the half-block footing after rotating back");
            supportedOperator.stopRiding();
            helper.assertTrue(Math.abs(towerCrossbow.getMaxAimPitch() - 12.0F) < 0.01F,
                    "Tower Crossbow kept the elevated aiming limit after dismount");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void projectilesIgnoreProtectedAllies(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        ArmorStand ally = EntityType.ARMOR_STAND.create(level);
        ArmorStand enemy = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(towerCrossbow != null && operator != null && ally != null && enemy != null,
                "Failed to create friendly-fire test entities");

        String teamName = "siege_ff_test";
        PlayerTeam team = level.getScoreboard().getPlayerTeam(teamName);
        if (team == null) {
            team = level.getScoreboard().addPlayerTeam(teamName);
        }
        team.setAllowFriendlyFire(false);
        level.getScoreboard().addPlayerToTeam(operator.getScoreboardName(), team);
        level.getScoreboard().addPlayerToTeam(ally.getScoreboardName(), team);

        towerCrossbow.setOperator(operator);
        TestTowerCrossbowBoltProjectile bolt = new TestTowerCrossbowBoltProjectile(
                SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
        helper.assertTrue(!bolt.canHitTarget(ally),
                "A teammate remained a projectile collision target while friendly fire was disabled");
        helper.assertTrue(bolt.canHitTarget(enemy),
                "An enemy was incorrectly excluded from projectile collisions");

        SerpentineEntity alliedSiege = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        SerpentineEntity enemySiege = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        helper.assertTrue(alliedSiege != null && enemySiege != null,
                "Failed to create friendly-fire siege targets");
        alliedSiege.setOperator(ally);
        enemySiege.setOperator(enemy);
        helper.assertTrue(!bolt.canHitTarget(alliedSiege),
                "A teammate's siege engine remained a projectile collision target");
        helper.assertTrue(bolt.canHitTarget(enemySiege),
                "An enemy siege engine was incorrectly excluded from projectile collisions");

        team.setAllowFriendlyFire(true);
        helper.assertTrue(bolt.canHitTarget(ally),
                "A teammate remained excluded after friendly fire was enabled");
        helper.assertTrue(bolt.canHitTarget(alliedSiege),
                "A teammate's siege engine remained excluded after friendly fire was enabled");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerCrossbowBoltsDamageEverySiegeEngine(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(towerCrossbow != null && operator != null,
                "Failed to create siege projectile test entities");
        towerCrossbow.setOperator(operator);

        EntityType<?>[] siegeTypes = {
                SiegeworksEntities.SERPENTINE_ENTITY.get(),
                SiegeworksEntities.CULVERIN_ENTITY.get(),
                SiegeworksEntities.BATTERING_RAM_ENTITY.get(),
                SiegeworksEntities.MANGONEL_ENTITY.get(),
                SiegeworksEntities.TREBUCHET_ENTITY.get(),
                SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get(),
                SiegeworksEntities.ARCBALLISTA_ENTITY.get(),
                SiegeworksEntities.MANTLET_ENTITY.get(),
                SiegeworksEntities.MONS_MEG_ENTITY.get(),
                SiegeworksEntities.SIEGE_TOWER_ENTITY.get(),
                SiegeworksEntities.SIEGE_LADDER_ENTITY.get()
        };

        for (EntityType<?> siegeType : siegeTypes) {
            Entity created = siegeType.create(level);
            helper.assertTrue(created instanceof AbstractSiegeEntity,
                    "Failed to create siege target " + BuiltInRegistries.ENTITY_TYPE.getKey(siegeType));
            AbstractSiegeEntity target = (AbstractSiegeEntity) created;
            TestTowerCrossbowBoltProjectile bolt = new TestTowerCrossbowBoltProjectile(
                    SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
            float healthBefore = target.getHealth();
            boolean damaged = target.hurt(level.damageSources().thrown(bolt, operator), 5.0F);
            helper.assertTrue(damaged && target.getHealth() < healthBefore,
                    "Tower Crossbow bolts cannot damage " + BuiltInRegistries.ENTITY_TYPE.getKey(siegeType));
        }

        SerpentineEntity arrowTarget = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        helper.assertTrue(arrowTarget != null, "Failed to create the vanilla arrow immunity target");
        Arrow arrow = GameTestVersionCompat.arrow(level, operator);
        float arrowTargetHealth = arrowTarget.getHealth();
        arrowTarget.hurt(level.damageSources().arrow(arrow, operator), 5.0F);
        helper.assertTrue(arrowTarget.getHealth() == arrowTargetHealth,
                "A vanilla arrow damaged a siege engine");

        SerpentineEntity balanceTarget = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        helper.assertTrue(balanceTarget != null, "Failed to create the siege damage balance target");
        TestTowerCrossbowBoltProjectile fullDamageBolt = new TestTowerCrossbowBoltProjectile(
                SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
        moveToRelative(helper, balanceTarget, 8.0D, 1.0D, 8.0D);
        fullDamageBolt.setPos(balanceTarget.getX(), balanceTarget.getY() + 1.0D, balanceTarget.getZ() - 2.0D);
        helper.assertTrue(level.addFreshEntity(balanceTarget), "Failed to add the siege damage balance target");
        helper.assertTrue(level.addFreshEntity(fullDamageBolt), "Failed to add the siege damage balance bolt");
        fullDamageBolt.setBaseDamage(44.0D);
        fullDamageBolt.setDeltaMovement(0.0D, 0.0D, 9.4D);
        float balanceHealth = balanceTarget.getHealth();
        fullDamageBolt.hitTarget(balanceTarget);
        helper.assertTrue(balanceTarget.getHealth() < balanceHealth && balanceTarget.isAlive(),
                "A standard towerCrossbow bolt did not deal balanced damage to a cannon");
        helper.assertTrue(!fullDamageBolt.isRemoved() && fullDamageBolt.isEmbedded(),
                "Tower Crossbow bolt did not remain embedded after dealing structural damage");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void siegeBoltsDamageMultipartTargets(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        FakePlayer operator = SiegeGameTestPlayers.create(level);
        EnderDragon dragon = EntityType.ENDER_DRAGON.create(level);
        helper.assertTrue(towerCrossbow != null && dragon != null,
                "Failed to create multipart projectile test entities");

        towerCrossbow.setOperator(operator);
        moveToRelative(helper, dragon, 8.0D, 5.0D, 8.0D);
        dragon.setNoAi(true);
        helper.assertTrue(level.addFreshEntity(dragon), "Failed to add the Ender Dragon test target");

        TestTowerCrossbowBoltProjectile bolt = new TestTowerCrossbowBoltProjectile(
                SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
        bolt.setBaseDamage(44.0D);
        bolt.setDeltaMovement(0.0D, 0.0D, 9.4D);
        EnderDragonPart head = dragon.head;
        float healthBefore = dragon.getHealth();
        helper.assertTrue(bolt.canHitTarget(head), "A multipart hitbox was not a valid projectile target");
        bolt.hitTarget(head);
        helper.assertTrue(dragon.getHealth() < healthBefore,
                "A hit on an Ender Dragon part did not damage its parent entity");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void lethalBoltHitsContinueAndSurvivorsKeepTheBolt(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        TowerCrossbowEntity towerCrossbow = SiegeworksEntities.TOWER_CROSSBOW_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        Zombie lethalTarget = EntityType.ZOMBIE.create(level);
        Zombie survivingTarget = EntityType.ZOMBIE.create(level);
        helper.assertTrue(towerCrossbow != null && operator != null
                        && lethalTarget != null && survivingTarget != null,
                "Failed to create bolt attachment test entities");
        towerCrossbow.setOperator(operator);
        lethalTarget.setNoAi(true);
        survivingTarget.setNoAi(true);
        moveToRelative(helper, lethalTarget, 6.0D, 1.0D, 6.0D);
        moveToRelative(helper, survivingTarget, 10.0D, 1.0D, 6.0D);
        helper.assertTrue(level.addFreshEntity(lethalTarget), "Failed to add the lethal bolt target");
        helper.assertTrue(level.addFreshEntity(survivingTarget), "Failed to add the attachment target");

        TestTowerCrossbowBoltProjectile lethalBolt = new TestTowerCrossbowBoltProjectile(
                SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
        lethalBolt.setBaseDamage(200.0D);
        lethalBolt.setDeltaMovement(0.0D, 0.0D, 12.0D);
        lethalBolt.hitTarget(lethalTarget);
        helper.assertTrue(!lethalTarget.isAlive(), "The lethal bolt test target survived");
        helper.assertTrue(!lethalBolt.isRemoved() && !lethalBolt.isEmbedded()
                        && lethalBolt.getDeltaMovement().lengthSqr() > 0.01D,
                "A bolt stopped after killing a penetrable target");

        TestTowerCrossbowBoltProjectile attachingBolt = new TestTowerCrossbowBoltProjectile(
                SiegeworksEntities.TOWER_CROSSBOW_BOLT_PROJECTILE.get(), towerCrossbow, level);
        attachingBolt.setBaseDamage(1.0D);
        attachingBolt.setDeltaMovement(0.0D, 0.0D, 5.0D);
        attachingBolt.hitTarget(survivingTarget);
        helper.assertTrue(survivingTarget.isAlive(), "The attachment test target was killed");
        helper.assertTrue(!attachingBolt.isRemoved() && attachingBolt.isEmbedded(),
                "A nonlethal bolt did not remain attached to its target");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerBannersFillInPairOrderAndPersist(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the tower banner test entity");
        moveToRelative(helper, tower, 8.0D, 1.0D, 8.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the tower banner test entity");

        FakePlayer player =
                FakePlayerFactory.get(
                        level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "tower_banners"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.RED_BANNER, 2));
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_BANNER, 2));
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(tower.getTowerBanner(0).is(Items.RED_BANNER),
                "The first west banner was not installed first");
        helper.assertTrue(tower.getTowerBanner(1).is(Items.RED_BANNER),
                "The first east banner was not installed second");
        helper.assertTrue(tower.getTowerBanner(2).is(Items.BLUE_BANNER),
                "The second west banner was not installed third");
        helper.assertTrue(tower.getTowerBanner(3).is(Items.BLUE_BANNER),
                "The second east banner was not installed fourth");

        CompoundTag saved = new CompoundTag();
        tower.addAdditionalSaveData(saved);
        SiegeTowerEntity restored = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(restored != null, "Failed to create the restored tower banner test entity");
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.getTowerBanner(0).is(Items.RED_BANNER)
                        && restored.getTowerBanner(1).is(Items.RED_BANNER)
                        && restored.getTowerBanner(2).is(Items.BLUE_BANNER)
                        && restored.getTowerBanner(3).is(Items.BLUE_BANNER),
                "Tower banners did not survive NBT persistence");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerLeatherCoveringCompletesBottomBeforeTopAndPersists(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(tower != null, "Failed to create the tower covering test entity");
        moveToRelative(helper, tower, 8.0D, 1.0D, 8.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add the tower covering test entity");
        helper.assertTrue(tower.getMaxHealth() == 350.0F && !tower.fireImmune(),
                "An uncovered tower started with leather protection");

        Player player = SiegeGameTestPlayers.createRideable(level);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER, 24));
        InteractionResult partialCoveringResult = tower.interact(
                player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(tower.getLeatherBottomProgress() == 24 && !tower.hasLeatherBottom(),
                "A partial stack did not remain partial: result=" + partialCoveringResult
                        + ", progress=" + tower.getLeatherBottomProgress()
                        + ", held=" + player.getMainHandItem().getCount()
                        + ", instabuild=" + player.getAbilities().instabuild);
        helper.assertTrue(tower.getLeatherTopProgress() == 0,
                "Leather was added to the upper covering before the lower covering was complete");
        helper.assertTrue(tower.getMaxHealth() == 350.0F && !tower.fireImmune(),
                "A partial covering granted durability or fire protection");
        helper.assertTrue(tower.getLeatherMaterials().getOrDefault(
                        BuiltInRegistries.ITEM.getKey(Items.LEATHER), 0) == 24,
                "The partial covering did not remember its supplied leather");

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER, 64));
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(tower.hasLeatherBottom() && tower.getLeatherBottomProgress() == 64,
                "The lower covering did not complete at one full stack");
        helper.assertTrue(player.getMainHandItem().getCount() == 24,
                "Completing the lower covering consumed leather intended for the upper covering");
        helper.assertTrue(tower.getMaxHealth() == 400.0F && tower.getHealth() == 400.0F,
                "The completed lower covering did not add 50 current and maximum health");
        helper.assertTrue(tower.fireImmune(),
                "A completed leather covering did not protect the tower from fire");
        tower.setRemainingFireTicks(100);
        helper.assertTrue(!tower.isOnFire(),
                "A leather-covered tower could still enter the burning state");
        float healthBeforeFire = tower.getHealth();
        tower.hurt(level.damageSources().onFire(), 10.0F);
        helper.assertTrue(tower.getHealth() == healthBeforeFire,
                "A leather-covered tower took burning damage");
        tower.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(tower.getLeatherTopProgress() == 24 && !tower.hasLeatherTop(),
                "The remaining leather did not start the upper covering");
        helper.assertTrue(tower.getMaxHealth() == 400.0F,
                "A partial upper covering granted another durability bonus");

        CompoundTag saved = new CompoundTag();
        tower.addAdditionalSaveData(saved);
        SiegeTowerEntity restored = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        helper.assertTrue(restored != null, "Failed to create the restored tower covering test entity");
        restored.readAdditionalSaveData(saved);
        helper.assertTrue(restored.hasLeatherBottom() && restored.getLeatherTopProgress() == 24,
                "Tower covering progress did not survive NBT persistence");
        helper.assertTrue(restored.getMaxHealth() == 400.0F && restored.fireImmune(),
                "Tower covering protection did not survive NBT persistence");
        ResourceLocation leatherId = BuiltInRegistries.ITEM.getKey(Items.LEATHER);
        helper.assertTrue(restored.getLeatherMaterials().getOrDefault(leatherId, 0) == 88,
                "Tower covering materials did not survive NBT persistence");
        restored.setHealth(restored.getMaxHealth() - 50.0F);
        helper.assertTrue(SiegeMaintenanceData.count(SiegeMaintenanceData.repairCost(restored), leatherId.toString()) == 11,
                "Tower covering materials were not included in its repair cost");
        helper.assertTrue(SiegeMaintenanceData.count(SiegeMaintenanceData.dismantleRefund(restored), leatherId.toString()) == 38,
                "Tower covering materials were not included in its dismantling refund");

        restored.setHealth(restored.getMaxHealth());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER, 40));
        restored.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(restored.hasLeatherTop() && restored.getMaxHealth() == 450.0F
                        && restored.getHealth() == 450.0F,
                "The completed upper covering did not add its 50 current and maximum health");

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.setShiftKeyDown(true);
        restored.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!restored.hasLeatherTop() && restored.hasLeatherBottom()
                        && restored.getMaxHealth() == 400.0F,
                "Sneaking with shears did not remove the upper covering first");
        helper.assertTrue(player.getInventory().countItem(Items.LEATHER) == 64,
                "Removing the upper covering did not return its leather");
        restored.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!restored.hasLeatherCovering() && restored.getMaxHealth() == 350.0F,
                "Sneaking with shears did not remove the lower covering");
        helper.assertTrue(player.getInventory().countItem(Items.LEATHER) == 128,
                "Removing both coverings did not return all supplied leather");

        UUID differentOwner = UUID.randomUUID();
        restored.setDeploymentIdentity(differentOwner, "player:" + differentOwner);
        player.setShiftKeyDown(false);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.LEATHER));
        restored.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(restored.getLeatherBottomProgress() == 0
                        && player.getMainHandItem().getCount() == 1,
                "A player outside the tower owner's group could modify its covering");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void playerTakesUpLadderWithoutLosingState(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        helper.assertTrue(ladder != null, "Failed to create the ladder pickup test entity");

        ladder.setSections(4);
        ladder.setHealth(42.0F);
        ladder.setCustomName(net.minecraft.network.chat.Component.literal("Test ladder"));
        moveToRelative(helper, ladder, 8.0D, 1.0D, 8.0D);
        helper.assertTrue(level.addFreshEntity(ladder), "Failed to add the ladder pickup test entity");

        FakePlayer player =
                FakePlayerFactory.get(
                        level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ladder_pickup"));
        player.getInventory().clearContent();
        player.setPos(ladder.getX(), ladder.getY(), ladder.getZ() - 1.0D);
        player.setShiftKeyDown(true);
        ladder.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(ladder.isCarried() && !ladder.isRemoved(),
                "Shift-right-click at its foot did not take the ladder up into the hands");
        helper.assertTrue(ladder.getSections() == 4 && Math.abs(ladder.getHealth() - 42.0F) < 0.01F,
                "The ladder taken up lost its sections or its health");
        helper.assertTrue("Test ladder".equals(ladder.getCustomName() == null ? null
                        : ladder.getCustomName().getString()), "The ladder taken up lost its custom name");
        helper.assertTrue(player.getUUID().equals(ladder.getOwnerUuid()), "The ladder taken up lost its owner");
        me.mss1r.siegeworks.gameplay.ladder.LadderCarry.drop(ladder);
        ladder.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void enemyCannotPickUpOwnedLadder(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        helper.assertTrue(ladder != null, "Failed to create the owned ladder test entity");

        ladder.setOwnerUuid(UUID.randomUUID());
        moveToRelative(helper, ladder, 8.0D, 1.0D, 8.0D);
        helper.assertTrue(level.addFreshEntity(ladder), "Failed to add the owned ladder test entity");

        FakePlayer enemy =
                FakePlayerFactory.get(
                        level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ladder_enemy"));
        enemy.getInventory().clearContent();
        enemy.setPos(ladder.getX(), ladder.getY(), ladder.getZ() - 1.0D);
        enemy.setShiftKeyDown(true);
        ladder.interact(enemy, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(!ladder.isRemoved(), "A non-owner picked up the owned ladder");
        helper.assertTrue(enemy.getInventory().items.stream()
                        .noneMatch(stack -> stack.is(SiegeworksItems.SIEGE_LADDER_SPAWNER.get())),
                "A non-owner received the owned ladder item");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "axiomata_construction")
    public static void externalWorkerCompletesConstruction(GameTestHelper helper) {
        assertExternalWorkerCompletesConstruction(
                helper, "siegeworks:mantlet", MantletEntity.class, "mantlet");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "axiomata_construction")
    public static void externalWorkerCompletesCannonConstruction(GameTestHelper helper) {
        assertExternalWorkerCompletesConstruction(
                helper, "siegeworks:serpentine", SerpentineEntity.class, "serpentine");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100, batch = "axiomata_construction")
    public static void externalWorkerCompletesHwachaConstruction(GameTestHelper helper) {
        assertExternalWorkerCompletesConstruction(
                helper, "siegeworks:hwacha", HwachaEntity.class, "hwacha");
        helper.succeed();
    }

    private static <T extends Entity> void assertExternalWorkerCompletesConstruction(
            GameTestHelper helper, String recipeId, Class<T> resultType, String resultName) {
        buildFloor(helper);
        BlueprintDefinition recipe = BlueprintDefinitions.get(recipeId);
        helper.assertTrue(recipe != null, resultName + " blueprint was not loaded");

        ItemStack resultStack = BlueprintStacks.createResult(recipe);
        MinecraftVersionCompat.editCustomData(resultStack, tag -> tag.putUUID(
                SiegeDeploymentLimits.TAG_CONSTRUCTION_OWNER, UUID.randomUUID()));
        BlockPos targetPos = helper.absolutePos(new BlockPos(16, 0, 16));
        for (int x = 12; x <= 20; x++) {
            for (int y = 1; y <= 8; y++) {
                for (int z = 12; z <= 20; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
        helper.getLevel().getEntitiesOfClass(
                        Entity.class, new AABB(targetPos).inflate(6.0D))
                .forEach(Entity::discard);
        ItemStack blueprint = BlueprintStacks.create(recipeId);
        Entity placed = ConstructionDeployer.deploy(
                helper.getLevel(), recipeId, blueprint, resultStack,
                targetPos, Direction.UP, Vec3.atCenterOf(targetPos).add(0.0D, 0.5D, 0.0D), 0.0F);
        helper.assertTrue(placed instanceof UnderConstruction, resultName + " was not placed under construction");
        UnderConstruction machine = (UnderConstruction) placed;

        SimpleContainer materials = new SimpleContainer(64);
        for (BlueprintConstructionPlan.Stage stage : machine.buildProgress().plan().stages()) {
            for (me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material material : stage.materials()) {
                materials.addItem(material.returnStack().copyWithCount(64));
            }
        }

        int attempts = 0;
        while (!machine.isFullyBuilt() && attempts++ < 10_000) {
            ConstructionWork.Result workResult = ConstructionWork.strike(machine, materials);
            helper.assertTrue(workResult.status() != ConstructionWork.Status.MISSING_MATERIAL,
                    "External worker ran out of supplied construction materials");
        }

        helper.assertTrue(machine.isFullyBuilt(), "External worker did not finish the construction");
        helper.assertTrue(resultType.isInstance(placed),
                "Completed construction is not a " + resultName);
    }

    @GameTest(template = "empty", timeoutTicks = 900)
    public static void automatedDismantlingReturnsResourcesToContainer(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        MantletEntity mantlet = SiegeworksEntities.MANTLET_ENTITY.get().create(level);
        Player worker = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(mantlet != null,
                "Failed to create automated dismantling test entities");

        moveToRelative(helper, mantlet, 8.0D, 1.0D, 8.0D);
        UUID deploymentOwner = worker.getUUID();
        SiegeDeploymentLimits.Deployment deployment = new SiegeDeploymentLimits.Deployment(
                deploymentOwner, "team:dismantling_test_" + deploymentOwner);
        mantlet.setDeploymentIdentity(deployment.ownerUuid(), deployment.groupKey());
        worker.setPos(mantlet.getX(), mantlet.getY(), mantlet.getZ() + 1.0D);
        worker.setItemSlot(EquipmentSlot.MAINHAND, BlueprintStacks.constructionHammer());
        helper.assertTrue(level.addFreshEntity(mantlet), "Failed to add the dismantling test engine");
        SiegeDeploymentLimits.register(mantlet);
        int registeredDeployments = SiegeDeploymentLimits.check(
                level, mantlet.getType(), deployment).current();
        helper.assertTrue(registeredDeployments == 1,
                "The dismantling test engine was not registered exactly once against its original owner: "
                        + registeredDeployments);

        java.util.List<me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition.Material> expectedRefund =
                SiegeMaintenanceData.dismantleRefund(mantlet);
        int requiredHits = SiegeMaintenanceData.dismantleRequiredHits(mantlet);
        helper.assertTrue(!expectedRefund.isEmpty() && requiredHits > 0,
                "Mantlet maintenance recipe was not loaded");
        helper.assertTrue(mantlet.startAutomatedDismantling(worker),
                "Automated dismantling did not start");

        SimpleContainer refundTarget = new SimpleContainer(64);
        for (int hit = 0; hit < requiredHits; hit++) {
            helper.runAfterDelay(1 + hit * 6,
                    () -> mantlet.performAutomatedDismantleHit(worker, refundTarget));
        }
        helper.runAfterDelay(3 + requiredHits * 6, () -> {
            helper.assertTrue(mantlet.isRemoved(), "Automated dismantling did not remove the engine");
            helper.assertTrue(SiegeDeploymentLimits.check(level, mantlet.getType(), deployment).current() == 0,
                    "Dismantling by another entity did not release the original owner's deployment slot");
            expectedRefund.forEach(material -> helper.assertTrue(
                    refundTarget.countItem(material.returnStack().getItem()) == material.count(),
                    "Dismantling refund mismatch for " + material.key() + ": expected=" + material.count()
                            + ", actual=" + refundTarget.countItem(material.returnStack().getItem())));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void ladderTraversesBothDirections(GameTestHelper helper) {
        buildFloor(helper);
        buildLadderWall(helper);

        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        ArmorStand climber = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(ladder != null && climber != null, "Failed to create ladder test entities");
        ladder.setSections(1);
        ladder.applyYaw(0.0F);
        moveToRelative(helper, ladder, 8.0D, 1.0D, 8.0D);
        level.addFreshEntity(ladder);
        level.addFreshEntity(climber);

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(), "Ladder did not settle against the wall");
            Vec3 bottom = ladder.getAutomatedBottomApproach();
            Vec3 top = ladder.getAutomatedTopExit();
            helper.assertTrue(top.y > bottom.y + 1.0D,
                    "Settled ladder does not gain elevation: bottom=" + bottom + ", top=" + top
                            + ", lean=" + ladder.getLeanProgress() + ", yaw=" + ladder.getYRot());
            climber.setPos(bottom.x, bottom.y, bottom.z);

            SiegeClimbableControl.ClimbResult result = climbToCompletion(ladder, climber, true);
            helper.assertTrue(result == SiegeClimbableControl.ClimbResult.COMPLETE,
                    "Automated climber did not reach the top");
            helper.assertTrue(climber.position().distanceToSqr(top) < 0.25D,
                    "Automated climber left the ladder at the wrong top position");

            result = climbToCompletion(ladder, climber, false);
            helper.assertTrue(result == SiegeClimbableControl.ClimbResult.COMPLETE,
                    "Automated climber did not return to the bottom");
            helper.assertTrue(climber.position().distanceToSqr(bottom) < 4.0D
                            && climber.getY() <= bottom.y + 0.25D,
                    "Automated climber did not clear the lower ladder exit safely: climber="
                            + climber.position() + ", bottom=" + bottom);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 700, batch = "recruits_ladder")
    public static void recruitsRelocateLadder(GameTestHelper helper) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }

        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        LivingEntity recruit = createLivingEntity(level, "recruits:recruit");
        helper.assertTrue(ladder != null && recruit != null,
                "Failed to create Recruits ladder relocation test entities");

        FakePlayer commander = SiegeGameTestPlayers.create(level);
        UUID groupId = UUID.randomUUID();
        ladder.setSections(3);
        ladder.setHealth(57.0F);
        moveToRelative(helper, ladder, 8.0D, 1.0D, 8.0D);
        moveToRelative(helper, recruit, 6.0D, 1.0D, 8.0D);
        commander.setPos(ladder.getX(), ladder.getY(), ladder.getZ() - 2.0D);
        if (recruit instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(ladder);
        level.addFreshEntity(recruit);

        helper.runAfterDelay(10, () -> {
            configureRecruitCommandIdentity(recruit, commander.getUUID(), groupId);
            issueLadderRelocationCommand(commander, 17, groupId, null, ladder.getId());
        });

        helper.runAfterDelay(180, () -> {
            helper.assertTrue(ladder.isRemoved(), "Recruit did not pick up the targeted ladder");
            ItemStack carried = findRecruitLadderItem(recruit);
            helper.assertTrue(!carried.isEmpty(), "Picked-up ladder was not stored in the recruit inventory");
            helper.assertTrue(me.mss1r.siegeworks.item.SiegeLadderDeploymentItem.getSections(carried) == 3,
                    "Picked-up ladder lost its section count");
            helper.assertTrue(Math.abs(me.mss1r.siegeworks.item.SiegeLadderDeploymentItem
                            .getStoredHealth(carried) - 57.0F) < 0.01F,
                    "Picked-up ladder lost its health");

            commander.setYRot(-90.0F);
            issueLadderRelocationCommand(commander, 18, groupId,
                    helper.absolutePos(new BlockPos(12, 0, 8)), -1);
        });

        helper.runAfterDelay(600, () -> {
            BlockPos expectedBase = helper.absolutePos(new BlockPos(12, 1, 8));
            SiegeLadderEntity placed = level.getEntitiesOfClass(
                            SiegeLadderEntity.class, new AABB(expectedBase).inflate(2.0D)).stream()
                    .findFirst()
                    .orElse(null);
            helper.assertTrue(placed != null, "Recruit did not place the carried ladder");
            helper.assertTrue(placed.getSections() == 3, "Placed ladder lost its section count");
            helper.assertTrue(Math.abs(placed.getHealth() - 57.0F) < 0.01F,
                    "Placed ladder lost its health");
            helper.assertTrue(commander.getUUID().equals(placed.getOwnerUuid()),
                    "Placed ladder did not retain the recruit owner's UUID");
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(placed.getYRot() + 90.0F)) < 0.01F,
                    "Placed ladder did not use the commander's facing direction");
            helper.assertTrue(findRecruitLadderItem(recruit).isEmpty(),
                    "Placed ladder remained in the recruit inventory");
            recruit.discard();
            placed.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 2800, batch = "recruits_ladder")
    public static void recruitsTraverseLadderBothDirections(GameTestHelper helper) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }

        buildFloor(helper);
        buildLadderWall(helper);
        ServerLevel level = helper.getLevel();
        SiegeLadderEntity ladder = SiegeworksEntities.SIEGE_LADDER_ENTITY.get().create(level);
        LivingEntity[] recruits = new LivingEntity[6];
        int[] maxSimultaneousClimbers = {0};
        for (int i = 0; i < recruits.length; i++) {
            recruits[i] = createLivingEntity(level, "recruits:recruit");
        }
        helper.assertTrue(ladder != null && java.util.Arrays.stream(recruits).allMatch(java.util.Objects::nonNull),
                "Failed to create Recruits ladder test entities");

        ladder.setSections(1);
        ladder.applyYaw(0.0F);
        moveToRelative(helper, ladder, 8.0D, 1.0D, 8.0D);
        level.addFreshEntity(ladder);
        for (int i = 0; i < recruits.length; i++) {
            LivingEntity recruit = recruits[i];
            moveToRelative(helper, recruit, 5.5D + i * 0.6D, 1.0D, 5.5D);
            if (recruit instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(recruit);
        }

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(),
                    "Ladder did not settle for the Recruits traversal test");
            Vec3 bottom = ladder.getAutomatedBottomApproach();
            Vec3 top = ladder.getAutomatedTopExit();
            buildUpperLanding(level, bottom, top);
        });

        helper.runAfterDelay(125, () -> {
            helper.assertTrue(ladder.isReadyForAutomatedClimb(),
                    "Ladder moved after the upper landing was added");
            Vec3 bottom = ladder.getAutomatedBottomApproach();
            Vec3 top = ladder.getAutomatedTopExit();
            Vec3 forward = top.subtract(bottom).multiply(1.0D, 0.0D, 1.0D).normalize();
            Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
            for (int i = 0; i < recruits.length; i++) {
                double side = (i % 3 - 1) * 2.4D;
                double depth = 4.0D + (i / 3) * 2.0D;
                Vec3 start = bottom.add(right.scale(side)).subtract(forward.scale(depth));
                recruits[i].setPos(start.x, start.y, start.z);
                orderRecruitToMove(recruits[i], BlockPos.containing(top.add(forward.scale(2.0D))).below());
            }
        });

        helper.runAfterDelay(250, () -> {
            helper.assertTrue(java.util.Arrays.stream(recruits)
                            .anyMatch(SiegeworksGameTests::isRecruitTraversingLadder),
                    "Recruits did not discover the ladder from a normal movement order");
        });

        for (int tick = 250; tick <= 1000; tick++) {
            helper.runAfterDelay(tick, () -> {
                int simultaneousClimbers = getActiveAutomatedClimberCount(ladder);
                maxSimultaneousClimbers[0] = Math.max(maxSimultaneousClimbers[0],
                        simultaneousClimbers);
            });
        }

        helper.runAfterDelay(1250, () -> {
            helper.assertTrue(maxSimultaneousClimbers[0] >= 2,
                    "Ladder never pipelined at least two recruits with safe spacing; max="
                            + maxSimultaneousClimbers[0] + ", recruits="
                            + java.util.Arrays.stream(recruits)
                            .map(recruit -> recruit.position() + " {" + describeRecruitTraversal(recruit) + "}")
                            .toList());
            Vec3 top = ladder.getAutomatedTopExit();
            Vec3 bottom = ladder.getAutomatedBottomApproach();
            Vec3 forward = top.subtract(bottom).multiply(1.0D, 0.0D, 1.0D).normalize();
            for (LivingEntity recruit : recruits) {
                helper.assertTrue(recruit.getY() >= top.y - 0.75D,
                        "A recruit did not climb to the upper surface: " + recruit.position());
                helper.assertTrue(recruit.position().subtract(top).dot(forward) > -1.5D,
                        "A recruit stopped before the safe upper ladder landing: " + recruit.position());
                helper.assertTrue(!isRecruitTraversingLadder(recruit),
                        "A recruit did not finish its upward ladder route");
                orderRecruitToMove(recruit, BlockPos.containing(bottom.subtract(forward.scale(3.0D))));
            }
        });

        helper.runAfterDelay(2400, () -> helper.succeedWhen(() -> {
                Vec3 bottom = ladder.getAutomatedBottomApproach();
                for (LivingEntity recruit : recruits) {
                    helper.assertTrue(recruit.getY() <= bottom.y + 0.75D,
                            "A recruit did not descend from the ladder: " + recruit.position()
                                    + " {" + describeRecruitTraversal(recruit) + "}");
                    helper.assertTrue(!isRecruitTraversingLadder(recruit),
                            "A recruit did not finish its downward ladder route: " + recruit.position()
                                    + " {" + describeRecruitTraversal(recruit) + "}");
                }
            }));
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void towerRefusesUnsafeDisembark(GameTestHelper helper) {
        buildFloor(helper);
        TowerCrew crew = spawnTestTowerCrew(helper);
        crew.tower.setDeployed(crew.driver, true);
        helper.assertTrue(crew.tower.preparePassengerForAutomatedExit(crew.passenger),
                "Tower could not move its passenger to the bridge floor");

        helper.runAfterDelay(150, () -> {
            helper.assertTrue(!crew.tower.canPassengerExit(crew.passenger),
                    "Tower allowed a passenger to exit into open air");
            helper.assertTrue(crew.passenger.getVehicle() == crew.tower,
                    "Unsafe passenger was removed from the tower");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void towerUnloadsAcrossItsBridgeOntoTheWall(GameTestHelper helper) {
        buildFloor(helper);
        buildBridgeLanding(helper);
        TowerCrew crew = spawnTestTowerCrew(helper);
        crew.tower.setDeployed(crew.driver, true);
        helper.assertTrue(crew.tower.preparePassengerForAutomatedExit(crew.passenger),
                "Tower could not move its passenger to the bridge floor");
        double wallTop = helper.absolutePos(BlockPos.ZERO).getY() + 12.0D;

        helper.runAfterDelay(150, () -> {
            helper.assertTrue(crew.tower.canPassengerExit(crew.passenger),
                    "Tower found no way across its bridge onto the wall it rests on");
            helper.assertTrue(crew.tower.disembarkPassenger(crew.passenger),
                    "Tower did not send its passenger across the bridge");
        });
        helper.runAfterDelay(152, () -> helper.succeedWhen(() -> {
            helper.assertTrue(crew.tower.advanceAutomatedExit(crew.passenger)
                            != me.mss1r.siegeworks.api.SiegeTransportControl.ExitResult.FAILED,
                    "The passenger's walk across the bridge failed at " + crew.passenger.position());
            helper.assertTrue(Math.abs(crew.passenger.getY() - wallTop) < 0.5D
                            && crew.passenger.getZ() > crew.tower.getZ() + 4.0D,
                    "The passenger did not land on the wall top: " + crew.passenger.position());
        }));
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void towedTowerRollsToAStopWhenItsDriverGetsOff(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        net.minecraft.world.entity.animal.horse.Horse horse = EntityType.HORSE.create(level);
        ArmorStand driver = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && horse != null && driver != null, "Failed to create the towed tower");
        moveToRelative(helper, tower, 16.0D, 1.0D, 6.0D);
        level.addFreshEntity(tower);
        horse.setTamed(true);
        horse.moveTo(tower.getX(), tower.getY(), tower.getZ());
        driver.moveTo(tower.getX(), tower.getY(), tower.getZ());
        level.addFreshEntity(horse);
        level.addFreshEntity(driver);
        helper.assertTrue(horse.startRiding(tower, true) && driver.startRiding(horse, true),
                "The tower took no driven draft horse");
        double[] stoppedAt = {Double.NaN};
        helper.onEachTick(() -> {
            if (driver.isPassenger()) {
                tower.setMovementInput(1.0F, 0.0F);
            }
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(Math.abs(tower.getCurrentDriveSpeed()) > 1.0E-3D, "The towed tower never got moving");
            driver.stopRiding();
        });
        helper.runAfterDelay(200, () -> stoppedAt[0] = tower.getZ());
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(Math.abs(tower.getCurrentDriveSpeed()) < 1.0E-4D
                            && Math.abs(tower.getZ() - stoppedAt[0]) < 1.0E-3D,
                    "The tower rolled on after its driver got off: speed " + tower.getCurrentDriveSpeed());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towedTowerHandsTheReinsOnlyToAPlayerAtTheHorse(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        net.minecraft.world.entity.animal.horse.Horse horse = EntityType.HORSE.create(level);
        helper.assertTrue(tower != null && horse != null, "Failed to create the towed tower");
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        level.addFreshEntity(tower);
        horse.setTamed(true);
        horse.moveTo(tower.getX(), tower.getY(), tower.getZ());
        level.addFreshEntity(horse);
        helper.assertTrue(horse.startRiding(tower, true), "The tower rejected its draft horse");

        helper.runAfterDelay(2, () -> {
            net.minecraft.world.entity.player.Player up = SiegeGameTestPlayers.createRideable(level);
            Vec3 aloft = tower.collisionTransform().toWorld(new Vec3(0.0D, 16.6D, 4.2D));
            up.setPos(aloft.x, aloft.y, aloft.z);
            tower.interact(up, InteractionHand.MAIN_HAND);
            helper.assertTrue(up.getVehicle() == null,
                    "Clicking the tower from its top floor put the player on the draft horse");

            net.minecraft.world.entity.player.Player driver = SiegeGameTestPlayers.createRideable(level);
            driver.setPos(horse.getX() + 1.2D, horse.getY(), horse.getZ());
            tower.interact(driver, InteractionHand.MAIN_HAND);
            helper.assertTrue(driver.getVehicle() == horse, "A player at the horse could not take the reins");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void towerUsesModelCollisionWithoutBoundingBoxPush(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.PIG.create(level);
        ArmorStand passageProbe = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && occupant != null && passageProbe != null,
                "Failed to create tower collision test entities");

        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        moveToRelative(helper, occupant, 17.0D, 2.3D, 16.0D);
        moveToRelative(helper, passageProbe, 16.0D, 24.0D, 16.0D);
        occupant.setNoAi(true);
        passageProbe.setNoGravity(true);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add collision test tower");
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add tower occupant");
        helper.assertTrue(level.addFreshEntity(passageProbe), "Failed to add tower passage probe");

        double originalX = occupant.getX();
        double originalZ = occupant.getZ();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(Math.abs(occupant.getX() - originalX) < 0.02D
                            && Math.abs(occupant.getZ() - originalZ) < 0.02D,
                    "The tower still pushed an entity with its full bounding box");

            StructureCollisionSystem.Movement freeInterior = StructureCollisionSystem.collideMovement(
                    occupant, new Vec3(0.0D, 0.0D, 1.0D));
            helper.assertTrue(freeInterior.allowed().z > 0.99D,
                    "The tower bounding box still blocked movement through its open interior");

            StructureCollisionSystem.Movement sideWall = StructureCollisionSystem.collideMovement(
                    occupant, new Vec3(5.0D, 0.0D, 0.0D));
            helper.assertTrue(sideWall.allowed().x < 3.0D,
                    "Model collision did not stop movement at the tower side wall");

            StructureCollisionSystem.Movement floor = StructureCollisionSystem.collideMovement(
                    occupant, new Vec3(0.0D, -0.3D, 0.0D));
            helper.assertTrue(floor.supported() && floor.allowed().y > -0.25D,
                    "Model collision did not expose the tower floor as walkable support");

            StructureCollisionSystem.Movement walking = StructureCollisionSystem.collideMovement(
                    occupant, new Vec3(0.5D, -0.3D, 0.0D));
            helper.assertTrue(walking.allowed().x > 0.49D && walking.allowed().y > -0.25D,
                    "Tower floor collision slowed tangential movement instead of only stopping the fall");

            StructureCollisionSystem.Movement jumping = StructureCollisionSystem.collideMovement(
                    occupant, new Vec3(0.0D, 0.42D, 0.0D));
            helper.assertTrue(jumping.allowed().y > 0.419D && !jumping.supported(),
                    "Tower floor collision blocked or kept supporting upward movement");

            Vec3 lowerOpening = tower.collisionTransform().toWorld(new Vec3(-1.75D, 10.1D, -1.65D));
            passageProbe.setPos(lowerOpening.x, lowerOpening.y, lowerOpening.z);
            StructureCollisionResolver.ResolvedMovement lowerDescent = StructureCollisionResolver.resolveMovement(
                    tower, passageProbe.getBoundingBox(),
                    tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.5D, 0.0D)),
                    GameTestVersionCompat.maxUpStep(passageProbe));
            Vec3 localLowerDescent = tower.collisionTransform().directionToLocal(lowerDescent.allowed());
            helper.assertTrue(localLowerDescent.y < -0.499D,
                    "The lower ladder opening was still covered by floor collision: allowed="
                            + localLowerDescent + ", width=" + passageProbe.getBbWidth());

            Vec3 upperOpening = tower.collisionTransform().toWorld(new Vec3(-3.21875D, 17.225D, 1.4375D));
            passageProbe.setPos(upperOpening.x, upperOpening.y, upperOpening.z);
            StructureCollisionResolver.ResolvedMovement upperDescent = StructureCollisionResolver.resolveMovement(
                    tower, passageProbe.getBoundingBox(),
                    tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.5D, 0.0D)),
                    GameTestVersionCompat.maxUpStep(passageProbe));
            Vec3 localUpperDescent = tower.collisionTransform().directionToLocal(upperDescent.allowed());
            helper.assertTrue(localUpperDescent.y < -0.499D,
                    "The upper ladder opening was still covered by floor collision");

            Vec3 wallStart = tower.collisionTransform().toWorld(new Vec3(3.0D, 1.1875D, 0.0D));
            occupant.setPos(wallStart.x, wallStart.y, wallStart.z);
            Vec3 diagonalRequest = tower.collisionTransform().directionToWorld(new Vec3(0.6D, -0.08D, 0.25D));
            StructureCollisionSystem.Movement wallSlide = StructureCollisionSystem.collideMovement(
                    occupant, diagonalRequest);
            Vec3 localWallSlide = tower.collisionTransform().directionToLocal(wallSlide.allowed());
            helper.assertTrue(localWallSlide.x > 0.20D && localWallSlide.x < 0.40D,
                    "Tower wall collision did not clip movement at the authored wall: allowed="
                            + localWallSlide);
            helper.assertTrue(localWallSlide.z > 0.249D && localWallSlide.y > -0.01D,
                    "Tower wall collision swallowed tangential or vertical movement");

            occupant.setPos(wallStart.x, wallStart.y, wallStart.z);
            occupant.setOnGround(true);
            occupant.setDeltaMovement(diagonalRequest);
            occupant.move(MoverType.SELF, diagonalRequest);
            Vec3 localWallMovement = tower.collisionTransform().directionToLocal(
                    occupant.position().subtract(wallStart));
            helper.assertTrue(localWallMovement.x > 0.20D && localWallMovement.x < 0.40D
                            && localWallMovement.z > 0.249D,
                    "Entity.move stuck to or crossed a vertical tower wall");

            Vec3 wallContact = occupant.position();
            for (int step = 0; step < 4; step++) {
                occupant.setDeltaMovement(diagonalRequest);
                occupant.move(MoverType.SELF, diagonalRequest);
            }
            Vec3 repeatedWallMovement = tower.collisionTransform().directionToLocal(
                    occupant.position().subtract(wallContact));
            helper.assertTrue(Math.abs(repeatedWallMovement.x) < 0.01D,
                    "Repeated wall contact pushed the entity away from the tower wall: "
                            + repeatedWallMovement);
            helper.assertTrue(repeatedWallMovement.z > 0.99D,
                    "Repeated wall contact made tangential movement feel sticky");

            Vec3 lowerFloor = tower.collisionTransform().toWorld(new Vec3(0.0D, 1.1875D, 0.0D));
            occupant.setPos(lowerFloor.x, lowerFloor.y, lowerFloor.z);
            occupant.setOnGround(true);
            double walkStartX = occupant.getX();
            occupant.setDeltaMovement(0.35D, -0.08D, 0.0D);
            occupant.move(MoverType.SELF, occupant.getDeltaMovement());
            helper.assertTrue(occupant.getX() >= walkStartX + 0.349D,
                    "Entity.move lost horizontal speed while walking on the tower floor");
            helper.assertTrue(occupant.onGround(),
                    "Entity.move did not retain tower floor support");

            double jumpStartY = occupant.getY();
            occupant.setDeltaMovement(0.0D, 0.42D, 0.0D);
            occupant.move(MoverType.SELF, occupant.getDeltaMovement());
            helper.assertTrue(occupant.getY() >= jumpStartY + 0.419D && !occupant.onGround(),
                    "Entity.move suppressed a jump from the tower floor");

            occupant.setOnGround(false);
            occupant.setDeltaMovement(Vec3.ZERO);
            StructureMotionSystem.tickStructure(tower);
            helper.assertTrue(!occupant.onGround(),
                    "Tower floor magnetized an airborne entity back onto its support");

            Vec3 lowerLadder = tower.collisionTransform().toWorld(new Vec3(-1.75D, 4.0D, -1.7D));
            occupant.setPos(lowerLadder.x, lowerLadder.y, lowerLadder.z);
            helper.assertTrue(StructureClimbingSystem.isOnClimbable(occupant),
                    "Lower tower ladder was not exposed as climbable geometry");
            occupant.setOnGround(false);
            Vec3 ladderPositionBeforeCarry = occupant.position();
            tower.setPos(tower.getX() + 0.4D, tower.getY(), tower.getZ());
            StructureMotionSystem.tickStructure(tower);
            helper.assertTrue(Math.abs(occupant.getX() - ladderPositionBeforeCarry.x - 0.4D) < 0.01D
                            && Math.abs(occupant.getZ() - ladderPositionBeforeCarry.z) < 0.01D,
                    "A moving tower left an airborne lower-ladder climber behind");
            Vec3 lowerClimb = StructureClimbingSystem.applyClimbableVelocity(
                    occupant, tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.08D, 0.1D)));
            Vec3 localLowerClimb = tower.collisionTransform().directionToLocal(lowerClimb);
            helper.assertTrue(localLowerClimb.y >= 0.2D && localLowerClimb.z < -0.09D,
                    "Forward movement did not follow the inclined lower tower ladder");
            Vec3 lowerApproach = tower.collisionTransform().toWorld(new Vec3(-1.75D, 4.0D, -2.95D));
            occupant.setPos(lowerApproach.x, lowerApproach.y, lowerApproach.z);
            Vec3 enteredLowerClimb = StructureClimbingSystem.applyClimbableVelocity(
                    occupant, tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.08D, 0.5D)));
            helper.assertTrue(enteredLowerClimb.y >= 0.2D
                            && enteredLowerClimb.horizontalDistanceSqr() <= 0.0201D,
                    "A one-tick approach crossed the lower ladder instead of starting a climb: "
                            + enteredLowerClimb);
            Vec3 jumpingClimb = StructureClimbingSystem.applyClimbableVelocity(
                    occupant, tower.collisionTransform().directionToWorld(new Vec3(0.0D, 0.42D, 0.1D)));
            helper.assertTrue(jumpingClimb.y > 0.419D
                            && jumpingClimb.horizontalDistanceSqr() <= 0.0201D,
                    "Jumping allowed movement through the freestanding lower tower ladder: "
                            + jumpingClimb);
            occupant.setDeltaMovement(Vec3.ZERO);
            double lowerStartY = occupant.getY();
            double lowerStartX = occupant.getX();
            double lowerStartZ = occupant.getZ();
            occupant.setNoAi(false);
            occupant.travel(new Vec3(0.0D, 0.0D, 1.0D));
            occupant.setNoAi(true);
            helper.assertTrue(occupant.getY() >= lowerStartY + 0.19D,
                    "LivingEntity travel did not climb the lower tower ladder in the current tick");
            helper.assertTrue(Math.abs(occupant.getX() - lowerStartX) < 0.01D
                            && Math.abs(occupant.getZ() - lowerStartZ) <= 0.101D,
                    "LivingEntity travel left the guided lower tower ladder path");

            Vec3 guidedLowerStart = tower.collisionTransform().toWorld(
                    new Vec3(-1.75D, 1.4D, -2.3D));
            passageProbe.setPos(guidedLowerStart.x, guidedLowerStart.y, guidedLowerStart.z);
            for (int step = 0; step < 55
                    && tower.collisionTransform().toLocal(passageProbe.position()).y < 10.4D; step++) {
                Vec3 climbRequest = tower.collisionTransform().directionToWorld(
                        new Vec3(0.0D, -0.08D, 0.1D));
                Vec3 climbMovement = StructureClimbingSystem.applyClimbableVelocity(
                        passageProbe, climbRequest);
                passageProbe.setDeltaMovement(climbMovement);
                passageProbe.move(MoverType.SELF, climbMovement);
            }
            Vec3 lowerExit = tower.collisionTransform().toLocal(passageProbe.position());
            helper.assertTrue(lowerExit.y >= 10.4D
                            && Math.abs(lowerExit.x + 1.75D) < 0.08D
                            && Math.abs(lowerExit.z + 1.3D) < 0.08D,
                    "Lower ladder did not complete its second-floor landing path: "
                            + lowerExit);

            Vec3 airborneHandoff = tower.collisionTransform().toWorld(
                    new Vec3(-1.75D, 10.1D, -0.7D));
            passageProbe.setPos(airborneHandoff.x, airborneHandoff.y, airborneHandoff.z);
            passageProbe.setOnGround(false);
            tower.xo = tower.getX();
            tower.yo = tower.getY();
            tower.zo = tower.getZ();
            Vec3 handoffBeforeMove = passageProbe.position();
            tower.setPos(tower.getX() + 0.2D, tower.getY(), tower.getZ());
            StructureMotionSystem.tickStructure(tower);
            helper.assertTrue(Math.abs(passageProbe.getX() - handoffBeforeMove.x - 0.2D) < 0.01D,
                    "Moving tower dropped its climber during the ladder-to-floor handoff");

            FakePlayer player =
                    FakePlayerFactory.get(
                            level, new com.mojang.authlib.GameProfile(
                                    UUID.randomUUID(), "tower_ladder_input"));
            Vec3 currentLowerLadder = tower.collisionTransform().toWorld(
                    new Vec3(-1.75D, 4.0D, -1.7D));
            player.setPos(currentLowerLadder.x, currentLowerLadder.y, currentLowerLadder.z);
            player.setShiftKeyDown(false);
            helper.assertTrue(!player.onClimbable(),
                    "Model ladder leaked into vanilla sticky ladder handling");
            Vec3 freeMovement = tower.collisionTransform().directionToLocal(
                    StructureClimbingSystem.applyPlayerClimbableVelocity(
                            player,
                            tower.collisionTransform().directionToWorld(
                                    new Vec3(0.0D, -0.08D, 0.1D)),
                            false));
            helper.assertTrue(freeMovement.z > 0.099D && freeMovement.y < -0.079D,
                    "Entering a ladder trigger changed player movement without climb input");

            Vec3 playerClimb = tower.collisionTransform().directionToLocal(
                    StructureClimbingSystem.applyPlayerClimbableVelocity(
                            player, new Vec3(0.0D, -0.08D, 0.0D), true));
            helper.assertTrue(playerClimb.y >= 0.2D,
                    "Jump input did not climb the model ladder");
            helper.assertTrue(VirtualPlatformSupport.isSupported(player),
                    "Model ladder climb was not accepted as virtual server movement support");

            player.setShiftKeyDown(true);
            Vec3 playerDescent = tower.collisionTransform().directionToLocal(
                    StructureClimbingSystem.applyPlayerClimbableVelocity(
                            player, Vec3.ZERO, false));
            player.setShiftKeyDown(false);
            helper.assertTrue(playerDescent.y <= -0.149D,
                    "Sneak input did not descend the model ladder");

            Vec3 upperLadder = tower.collisionTransform().toWorld(new Vec3(-3.65D, 13.0D, 1.4D));
            occupant.setPos(upperLadder.x, upperLadder.y, upperLadder.z);
            helper.assertTrue(StructureClimbingSystem.isOnClimbable(occupant),
                    "Upper tower ladder was not exposed as climbable geometry");
            Vec3 upperClimb = StructureClimbingSystem.applyClimbableVelocity(
                    occupant, tower.collisionTransform().directionToWorld(new Vec3(0.1D, -0.08D, 0.0D)));
            helper.assertTrue(upperClimb.y >= 0.2D
                            && tower.collisionTransform().directionToLocal(upperClimb).z > 0.0D,
                    "Forward movement did not centre and climb the freestanding upper tower ladder");
            Vec3 upperSideStep = StructureClimbingSystem.applyClimbableVelocity(
                    occupant, tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.08D, 0.1D)));
            Vec3 localUpperSideStep = tower.collisionTransform().directionToLocal(upperSideStep);
            helper.assertTrue(localUpperSideStep.z > 0.099D && localUpperSideStep.y < 0.0D,
                    "The upper ladder slowed tangential movement through the tower");

            Vec3 upperClimbStart = tower.collisionTransform().toWorld(
                    new Vec3(-3.2D, 14.8D, 1.78D));
            occupant.setPos(upperClimbStart.x, upperClimbStart.y, upperClimbStart.z);
            for (int step = 0; step < 14; step++) {
                Vec3 climbRequest = tower.collisionTransform().directionToWorld(
                        new Vec3(-0.1D, -0.08D, 0.0D));
                Vec3 climbMovement = StructureClimbingSystem.applyClimbableVelocity(
                        occupant, climbRequest);
                occupant.setDeltaMovement(climbMovement);
                occupant.move(MoverType.SELF, climbMovement);
            }
            Vec3 upperLanding = tower.collisionTransform().toLocal(occupant.position());
            helper.assertTrue(upperLanding.y > 17.2D,
                    "A full-width entity was blocked by the third-floor ladder opening: " + upperLanding);
            helper.assertTrue(upperLanding.z > 1.1D && upperLanding.z < 1.8D,
                    "Upper ladder centring missed the third-floor opening: " + upperLanding);

            Vec3 awayFromLadders = tower.collisionTransform().toWorld(new Vec3(0.0D, 5.0D, 0.0D));
            occupant.setPos(awayFromLadders.x, awayFromLadders.y, awayFromLadders.z);
            helper.assertTrue(!StructureClimbingSystem.isOnClimbable(occupant),
                    "Tower interior was treated as a ladder away from ladder geometry");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void towerRampPreservesMovementWithoutSliding(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        Mob occupant = EntityType.PIG.create(level);
        helper.assertTrue(tower != null && occupant != null,
                "Failed to create tower ramp test entities");

        CompoundTag bridgeState = new CompoundTag();
        tower.addAdditionalSaveData(bridgeState);
        bridgeState.putBoolean("BridgeOpen", true);
        bridgeState.putInt("BridgePreset", 0);
        bridgeState.putFloat("BridgeProgress", 0.5F);
        tower.readAdditionalSaveData(bridgeState);

        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        occupant.setNoAi(true);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add ramp test tower");

        CollisionGroup bridge = tower.collisionGroups().stream()
                .filter(group -> group.name().equals("bridge"))
                .findFirst()
                .orElseThrow();
        CollisionPart bridgePart = bridge.parts().get(0);
        AABB bridgeBox = bridgePart.box();
        Vec3 groupSurface = new Vec3(0.0D, (bridgeBox.minY + bridgeBox.maxY) * 0.5D, bridgeBox.minZ);
        Vec3 surface = tower.collisionTransform().toWorld(bridge.fromPart(bridgePart, groupSurface));
        occupant.setPos(surface.x, surface.y + 2.0D, surface.z);
        helper.assertTrue(level.addFreshEntity(occupant), "Failed to add ramp test occupant");

        StructureCollisionResolver.ResolvedMovement landing = StructureCollisionResolver.resolveMovement(
                tower, occupant.getBoundingBox(), new Vec3(0.0D, -3.0D, 0.0D), 0.0D);
        occupant.setPos(occupant.getX() + landing.allowed().x,
                occupant.getY() + landing.allowed().y,
                occupant.getZ() + landing.allowed().z);
        helper.assertTrue(landing.isSupported(), "Ramp did not provide walkable support");

        Vec3 uphillRequest = tower.collisionTransform().directionToWorld(new Vec3(0.0D, -0.08D, 0.2D));
        Vec3 uphill = tower.collisionTransform().directionToLocal(
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), uphillRequest, 0.0D).allowed());
        helper.assertTrue(uphill.z > 0.195D && Math.abs(uphill.x) < 0.005D,
                "Ramp reduced or deflected uphill movement: " + uphill);
        helper.assertTrue(uphill.y > 0.09D,
                "Ramp did not lift uphill movement along its surface: " + uphill);

        Vec3 acrossRequest = tower.collisionTransform().directionToWorld(new Vec3(0.2D, -0.08D, 0.0D));
        Vec3 across = tower.collisionTransform().directionToLocal(
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), acrossRequest, 0.0D).allowed());
        helper.assertTrue(across.x > 0.195D && Math.abs(across.z) < 0.005D,
                "Ramp reduced or deflected cross-slope movement: " + across);

        Vec3 gravity = tower.collisionTransform().directionToLocal(
                StructureCollisionResolver.resolveMovement(
                        tower, occupant.getBoundingBox(), new Vec3(0.0D, -0.08D, 0.0D), 0.0D).allowed());
        helper.assertTrue(gravity.horizontalDistanceSqr() < 1.0E-6D,
                "Gravity made the occupant slide sideways on the ramp: " + gravity);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void towerDismountClearsStructureGeometry(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && operator != null,
                "Failed to create dismount test entities");

        operator.addTag(TEST_OPERATOR_TAG);
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        moveToRelative(helper, operator, 16.0D, 1.0D, 20.0D);
        helper.assertTrue(level.addFreshEntity(tower), "Failed to add dismount test tower");
        helper.assertTrue(level.addFreshEntity(operator), "Failed to add dismount test operator");
        helper.assertTrue(operator.startRiding(tower), "Tower operator could not board");

        Vec3 dismount = tower.getDismountLocationForPassenger(operator);
        AABB dismountBox = operator.getBoundingBox().move(dismount.subtract(operator.position()));
        StructureCollisionResolver.Response overlap = StructureCollisionResolver.resolve(
                tower, dismountBox, Vec3.ZERO, 0.0D);
        helper.assertTrue(overlap.pushOut().equals(Vec3.ZERO),
                "Tower dismount still overlaps structure geometry: " + overlap.pushOut());
        Vec3 localDismount = tower.collisionTransform().toLocal(dismount);
        helper.assertTrue(Math.abs(localDismount.x) > 4.5D,
                "Tower dismount was not placed beyond a side wall: " + localDismount);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void towerCrewScalesMovement(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        ArmorStand driver = EntityType.ARMOR_STAND.create(level);
        ArmorStand[] pushers = new ArmorStand[5];
        for (int i = 0; i < pushers.length; i++) {
            pushers[i] = EntityType.ARMOR_STAND.create(level);
        }
        helper.assertTrue(tower != null && driver != null,
                "Failed to create tower crew movement test entities");

        driver.addTag(TEST_OPERATOR_TAG);
        moveToRelative(helper, tower, 16.0D, 1.0D, 6.0D);
        moveToRelative(helper, driver, 16.0D, 1.0D, 5.0D);
        level.addFreshEntity(tower);
        level.addFreshEntity(driver);
        helper.assertTrue(driver.startRiding(tower), "Test driver could not operate tower");
        helper.assertTrue(tower.getActivePushingCrewCount() == 1,
                "Tower did not count its driver as pushing crew");
        helper.assertTrue(!tower.reservePusher(driver) && !tower.reserveInteriorSeat(driver),
                "Tower allowed its driver to be reassigned to another crew role");
        helper.assertTrue(Math.abs(tower.getVelocity(driver)) < 1.0E-8D,
                "A lone driver moved the tower below its minimum crew size");
        Vec3 driverOffset = tower.getPassengerOffset(driver);
        helper.assertTrue(Math.abs(driverOffset.x - 50.0D / 16.0D) < 1.0E-6D
                        && Math.abs(driverOffset.z - 66.6D / 16.0D) < 1.0E-6D,
                "Tower driver did not occupy the first authored pushing position");
        helper.assertTrue(tower.getPassengerViewYawLimit(driver) == 25.0F
                        && tower.getPassengerViewPitchLimit(driver) == 35.0F,
                "Tower driver camera limits no longer match the pushing pose");

        double minimumCrewSpeed = 0.0D;
        double[] expectedPusherX = {30.0D, 10.0D, -10.0D, -30.0D, -50.0D};
        for (int i = 0; i < pushers.length; i++) {
            ArmorStand pusher = pushers[i];
            helper.assertTrue(pusher != null, "Failed to create tower pusher");
            moveToRelative(helper, pusher, 15.0D, 1.0D, 5.0D);
            level.addFreshEntity(pusher);
            helper.assertTrue(tower.reservePusher(pusher), "Tower did not reserve a pusher position");
            helper.assertTrue(pusher.startRiding(tower), "Pusher could not join tower crew");
            Vec3 pusherOffset = tower.getPassengerOffset(pusher);
            helper.assertTrue(Math.abs(pusherOffset.z - driverOffset.z) < 1.0E-6D,
                    "Tower pusher was not positioned on the driver's rear line");
            helper.assertTrue(Math.abs(pusherOffset.x - expectedPusherX[i] / 16.0D) < 1.0E-6D,
                    "Tower pusher did not occupy authored crew position " + (i + 2));
            helper.assertTrue(tower.getPassengerViewYawLimit(pusher) == 25.0F
                            && tower.getPassengerViewPitchLimit(pusher) == 35.0F,
                    "Tower pusher camera limits no longer match the pushing pose");
            if (i == 0) {
                minimumCrewSpeed = tower.getVelocity(driver);
                helper.assertTrue(minimumCrewSpeed > 0.0D,
                        "Tower did not move after reaching its minimum crew size");
            }
        }

        double fullCrewSpeed = tower.getVelocity(driver);
        helper.assertTrue(tower.getActivePushingCrewCount() == 6,
                "Tower did not count the full driver-and-pusher crew");
        helper.assertTrue(fullCrewSpeed > minimumCrewSpeed * 3.0D,
                "Full tower crew did not increase movement speed enough");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void towerSurvivesThreeCannonHits(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        SerpentineEntity cannon = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && cannon != null && operator != null,
                "Failed to create cannon durability test entities");

        moveToRelative(helper, tower, 16.0D, 1.0D, 18.0D);
        moveToRelative(helper, cannon, 16.0D, 1.0D, 7.0D);
        moveToRelative(helper, operator, 16.0D, 1.0D, 6.0D);
        level.addFreshEntity(tower);
        level.addFreshEntity(cannon);
        level.addFreshEntity(operator);
        cannon.setOperator(operator);

        fireTestCannonBall(level, cannon, tower);
        helper.runAfterDelay(30, () -> fireTestCannonBall(level, cannon, tower));
        helper.runAfterDelay(60, () -> fireTestCannonBall(level, cannon, tower));

        helper.runAfterDelay(90, () -> {
            helper.assertTrue(tower.isAlive(),
                    "Siege tower was destroyed by fewer than four standard cannon hits: health="
                            + tower.getHealth() + "/" + tower.getMaxHealth() + ", removed=" + tower.isRemoved()
                            + ", position=" + tower.position());
            helper.assertTrue(tower.getHealth() < tower.getMaxHealth() - 80.0F,
                    "Standard cannon hits did not apply meaningful damage to the siege tower: "
                            + tower.getHealth() + "/" + tower.getMaxHealth());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void creativeProjectilesUseNormalSiegeDamage(GameTestHelper helper) {
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        SerpentineEntity cannon = SiegeworksEntities.SERPENTINE_ENTITY.get().create(level);
        FakePlayer operator = SiegeGameTestPlayers.create(level);
        helper.assertTrue(tower != null && cannon != null,
                "Failed to create creative cannon damage test entities");

        operator.setGameMode(GameType.CREATIVE);
        moveToRelative(helper, tower, 16.0D, 1.0D, 18.0D);
        moveToRelative(helper, cannon, 16.0D, 1.0D, 7.0D);
        level.addFreshEntity(tower);
        level.addFreshEntity(cannon);
        cannon.setOperator(operator);

        fireTestCannonBall(level, cannon, tower);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(tower.isAlive(),
                    "A projectile fired by a creative player instantly removed the siege tower");
            helper.assertTrue(tower.getHealth() < tower.getMaxHealth(),
                    "A projectile fired by a creative player did not damage the siege tower");
            tower.hurt(level.damageSources().playerAttack(operator), 1.0F);
            helper.assertTrue(tower.isRemoved(),
                    "A direct attack by a creative player did not remove the siege tower");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ammunitionPreferenceControlsAutomatedLoading(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HwachaEntity hwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(level);
        HwachaEntity restoredHwacha = SiegeworksEntities.HWACHA_ENTITY.get().create(level);
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(hwacha != null && restoredHwacha != null && operator != null,
                "Failed to create ammunition preference test entities");

        hwacha.setPos(helper.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        operator.setPos(helper.absolutePos(new BlockPos(2, 1, 1)).getCenter());
        operator.addTag(TEST_OPERATOR_TAG);
        level.addFreshEntity(hwacha);
        level.addFreshEntity(operator);
        helper.assertTrue(operator.startRiding(hwacha), "Test operator could not crew the Hwacha");

        hwacha.setAutomatedAmmunitionMode(SiegeAmmunitionMode.EXPLOSIVE);
        SimpleContainer inventory = new SimpleContainer(SiegeworksItems.SO_SINGIJEON.get().getDefaultInstance());
        helper.assertTrue(hwacha.advancePrimaryAction(operator, inventory) == SiegeActionResult.MISSING_AMMUNITION,
                "Hwacha substituted standard ammunition for the selected explosive mode");

        CompoundTag saved = new CompoundTag();
        hwacha.addAdditionalSaveData(saved);
        restoredHwacha.readAdditionalSaveData(saved);
        helper.assertTrue(restoredHwacha.getAutomatedAmmunitionMode() == SiegeAmmunitionMode.EXPLOSIVE,
                "Hwacha lost its automated ammunition preference after loading");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "recruits_tower")
    public static void recruitsDriveAndDisembarkTower(GameTestHelper helper) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }

        buildFloor(helper);
        buildBridgeLanding(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        LivingEntity engineer = createLivingEntity(level, "recruits:siege_engineer");
        LivingEntity[] recruits = new LivingEntity[4];
        for (int i = 0; i < recruits.length; i++) {
            recruits[i] = createLivingEntity(level, "recruits:recruit");
        }
        helper.assertTrue(tower != null && engineer != null
                        && java.util.Arrays.stream(recruits).allMatch(java.util.Objects::nonNull),
                "Failed to create Recruits tower test entities");
        FakePlayer commander = SiegeGameTestPlayers.create(level);
        java.util.UUID groupId = java.util.UUID.randomUUID();
        java.util.UUID unselectedGroupId = java.util.UUID.randomUUID();

        moveToRelative(helper, tower, 16.0D, 1.0D, 6.0D);
        moveToRelative(helper, engineer, 16.0D, 1.0D, 5.0D);
        commander.setPos(tower.getX(), tower.getY(), tower.getZ());
        if (engineer instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(tower);
        level.addFreshEntity(engineer);
        for (int i = 0; i < recruits.length; i++) {
            LivingEntity recruit = recruits[i];
            moveToRelative(helper, recruit, 14.5D + i * 0.75D, 1.0D, 5.0D);
            if (recruit instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(recruit);
        }

        helper.runAfterDelay(20, () -> {
            configureRecruitCommandIdentity(engineer, commander.getUUID(), groupId);
            helper.assertTrue(engineer.startRiding(tower), "Siege engineer could not take the tower driver seat");
            helper.assertTrue(tower.isOperator(engineer), "Siege engineer was not assigned as tower driver");
            for (int i = 0; i < recruits.length; i++) {
                LivingEntity recruit = recruits[i];
                configureRecruitCommandIdentity(recruit, commander.getUUID(),
                        i < recruits.length - 1 ? groupId : unselectedGroupId);
                assignRecruitMount(recruit, tower.getUUID());
                helper.assertTrue(tower.reserveInteriorSeat(recruit),
                        "Tower could not reserve an interior recruit seat");
                helper.assertTrue(recruit.startRiding(tower), "Recruit could not board the tower interior");
                helper.assertTrue(tower.isInteriorPassenger(recruit),
                        "Recruit was not assigned an interior tower seat");
            }
            recruits[0].setXRot(35.0F);
            recruits[0].xRotO = 35.0F;
            orderRecruitToMove(engineer, helper.absolutePos(new BlockPos(16, 1, 30)));
        });

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(java.util.Arrays.stream(recruits).allMatch(recruit -> recruit.getVehicle() == tower),
                    "Tower unloaded recruits without an explicit command");
            helper.assertTrue(Math.abs(recruits[0].getXRot()) < 0.01F
                            && Math.abs(recruits[0].xRotO) < 0.01F,
                    "Tower did not stabilize its passenger's head pitch");
            helper.assertTrue(setTowerBridgeOverride(commander, tower, false),
                    "Tower driver did not accept the standing bridge-up order");
            issueTargetedTowerUnload(commander, tower, groupId);
        });

        helper.runAfterDelay(150, () -> helper.assertTrue(tower.isDeployed(),
                "Targeted unload command did not lower the tower bridge"));

        helper.runAfterDelay(380, () -> {
            for (int i = 0; i < recruits.length - 1; i++) {
                LivingEntity recruit = recruits[i];
                helper.assertTrue(recruit.getVehicle() != tower,
                        "A selected recruit did not leave the deployed tower");
                helper.assertTrue(recruit.getZ() > tower.getZ() + 4.0D,
                        "A recruit did not clear the far side of the bridge: recruit=" + recruit.position()
                                + ", tower=" + tower.position() + ", open=" + tower.isBridgeOpen()
                                + ", moving=" + tower.bridgeMoving() + ", angle=" + tower.getBridgeAngleRadians());
            }
            helper.assertTrue(recruits[recruits.length - 1].getVehicle() == tower,
                    "Targeted unload removed a recruit from an unselected group");
            helper.assertTrue(engineer.getVehicle() == tower && tower.isOperator(engineer),
                    "Siege engineer lost control while the tower unloaded");
            issueTargetedTowerReturn(commander, tower, groupId);
        });

        helper.runAfterDelay(500, () -> {
            for (int i = 0; i < recruits.length - 1; i++) {
                LivingEntity recruit = recruits[i];
                helper.assertTrue(recruit.getVehicle() == tower && tower.isInteriorPassenger(recruit),
                        "A selected recruit did not return to the tower interior");
            }
            helper.assertTrue(recruits[recruits.length - 1].getVehicle() == tower,
                    "Tower return disturbed a recruit from an unselected group");
            helper.assertTrue(!tower.isDeployed(),
                    "Temporary bridge deployment did not restore the standing bridge-up order");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 700, batch = "recruits_tower_stay")
    public static void recruitsStayAboardWhenTheBridgeReachesNothing(GameTestHelper helper) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }
        buildFloor(helper);
        for (int x = 9; x <= 23; x++) {
            for (int z = 11; z <= 15; z++) {
                for (int y = 1; y <= 7; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        LivingEntity engineer = createLivingEntity(level, "recruits:siege_engineer");
        LivingEntity[] recruits = new LivingEntity[3];
        for (int i = 0; i < recruits.length; i++) {
            recruits[i] = createLivingEntity(level, "recruits:recruit");
        }
        helper.assertTrue(tower != null && engineer != null
                        && java.util.Arrays.stream(recruits).allMatch(java.util.Objects::nonNull),
                "Failed to create Recruits tower test entities");
        FakePlayer commander = SiegeGameTestPlayers.create(level);
        java.util.UUID groupId = java.util.UUID.randomUUID();
        moveToRelative(helper, tower, 16.0D, 1.0D, 3.0D);
        moveToRelative(helper, engineer, 16.0D, 1.0D, 2.0D);
        commander.setPos(tower.getX(), tower.getY(), tower.getZ());
        if (engineer instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(tower);
        level.addFreshEntity(engineer);
        for (int i = 0; i < recruits.length; i++) {
            moveToRelative(helper, recruits[i], 14.5D + i * 0.75D, 1.0D, 2.0D);
            if (recruits[i] instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(recruits[i]);
        }
        helper.runAfterDelay(20, () -> {
            configureRecruitCommandIdentity(engineer, commander.getUUID(), groupId);
            helper.assertTrue(engineer.startRiding(tower), "Siege engineer could not take the tower driver seat");
            for (LivingEntity recruit : recruits) {
                configureRecruitCommandIdentity(recruit, commander.getUUID(), groupId);
                assignRecruitMount(recruit, tower.getUUID());
                helper.assertTrue(tower.reserveInteriorSeat(recruit) && recruit.startRiding(tower),
                        "Recruit could not board the tower interior");
            }
        });
        helper.runAfterDelay(60, () -> issueTargetedTowerUnload(commander, tower, groupId));
        helper.runAfterDelay(500, () -> {
            for (LivingEntity recruit : recruits) {
                helper.assertTrue(recruit.getVehicle() == tower,
                        "A recruit left the tower although its bridge reached nothing: " + recruit.position());
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "recruits_tower_leave_screen")
    public static void recruitsLeaveATowerWithoutAnEngineerFromTheCommandScreen(GameTestHelper helper) {
        recruitsLeaveATowerWithoutAnEngineer(helper, false);
    }

    @GameTest(template = "empty", timeoutTicks = 500, batch = "recruits_tower_leave_map")
    public static void recruitsLeaveATowerWithoutAnEngineerFromTheMap(GameTestHelper helper) {
        recruitsLeaveATowerWithoutAnEngineer(helper, true);
    }

    private static void recruitsLeaveATowerWithoutAnEngineer(GameTestHelper helper, boolean fromMap) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }
        buildFloor(helper);
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        LivingEntity[] recruits = new LivingEntity[3];
        for (int i = 0; i < recruits.length; i++) {
            recruits[i] = createLivingEntity(level, "recruits:recruit");
        }
        helper.assertTrue(tower != null && java.util.Arrays.stream(recruits).allMatch(java.util.Objects::nonNull),
                "Failed to create Recruits tower test entities");
        FakePlayer commander = SiegeGameTestPlayers.create(level);
        java.util.UUID groupId = java.util.UUID.randomUUID();
        moveToRelative(helper, tower, 16.0D, 1.0D, 16.0D);
        commander.setPos(tower.getX(), tower.getY(), tower.getZ());
        level.addFreshEntity(tower);
        for (int i = 0; i < recruits.length; i++) {
            moveToRelative(helper, recruits[i], 14.5D + i * 0.75D, 1.0D, 10.0D);
            if (recruits[i] instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(recruits[i]);
        }
        helper.runAfterDelay(20, () -> {
            for (LivingEntity recruit : recruits) {
                configureRecruitCommandIdentity(recruit, commander.getUUID(), groupId);
                assignRecruitMount(recruit, tower.getUUID());
                helper.assertTrue(tower.reserveInteriorSeat(recruit) && recruit.startRiding(tower),
                        "Recruit could not board the tower interior");
            }
        });
        helper.runAfterDelay(40, () -> {
            try {
                Class<?> compat = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCompat");
                if (fromMap) {
                    compat.getMethod("leaveMachine", net.minecraft.server.level.ServerPlayer.class,
                            me.mss1r.siegeworks.entity.base.AbstractSiegeEntity.class, boolean.class)
                            .invoke(null, commander, tower, false);
                } else {
                    compat.getMethod("handleSiegeCommand", net.minecraft.server.level.ServerPlayer.class,
                                    int.class, java.util.List.class, BlockPos.class, int.class,
                                    ResourceLocation.class)
                            .invoke(null, commander, 3, java.util.List.of(groupId), null, -1,
                                    BuiltInRegistries.ENTITY_TYPE.getKey(tower.getType()));
                }
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("Could not issue the leave command", exception);
            }
        });
        helper.runAfterDelay(400, () -> {
            for (LivingEntity recruit : recruits) {
                helper.assertTrue(recruit.getVehicle() == null,
                        "A recruit stayed in the tower after the leave order: " + recruit.position());
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "recruits_tower_no_driver_down")
    public static void recruitsUnloadWithoutADriverOverABridgeAlreadyDown(GameTestHelper helper) {
        recruitsUnloadWithoutADriver(helper, true);
    }

    @GameTest(template = "empty", timeoutTicks = 600, batch = "recruits_tower_no_driver_up")
    public static void recruitsStayInWithoutADriverWhileTheBridgeIsUp(GameTestHelper helper) {
        recruitsUnloadWithoutADriver(helper, false);
    }

    private static void recruitsUnloadWithoutADriver(GameTestHelper helper, boolean bridgeDown) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }
        buildFloor(helper);
        buildBridgeLanding(helper);
        TowerCrew crew = spawnTestTowerCrew(helper);
        ServerLevel level = helper.getLevel();
        LivingEntity[] recruits = new LivingEntity[2];
        for (int i = 0; i < recruits.length; i++) {
            recruits[i] = createLivingEntity(level, "recruits:recruit");
            helper.assertTrue(recruits[i] != null, "Failed to create a recruit");
            moveToRelative(helper, recruits[i], 14.5D + i * 0.75D, 1.0D, 4.0D);
            if (recruits[i] instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            level.addFreshEntity(recruits[i]);
        }
        FakePlayer commander = SiegeGameTestPlayers.create(level);
        commander.setPos(crew.tower().getX(), crew.tower().getY(), crew.tower().getZ());
        java.util.UUID groupId = java.util.UUID.randomUUID();
        if (bridgeDown) {
            crew.tower().setDeployed(crew.driver(), true);
        }
        helper.runAfterDelay(20, () -> {
            for (LivingEntity recruit : recruits) {
                configureRecruitCommandIdentity(recruit, commander.getUUID(), groupId);
                assignRecruitMount(recruit, crew.tower().getUUID());
                helper.assertTrue(crew.tower().reserveInteriorSeat(recruit) && recruit.startRiding(crew.tower()),
                        "Recruit could not board the tower interior");
            }
        });
        helper.runAfterDelay(170, () -> {
            crew.driver().stopRiding();
            helper.assertTrue(crew.tower().isBridgeOpen() == bridgeDown,
                    "The bridge did not stay as it was left when its driver stepped off");
            helper.assertTrue(allows(commandStates(commander, groupId, -1), "SIEGE_TOWER.unload_tower") == bridgeDown,
                    "The unload button did not follow whether the bridge could be had down");
            issueTargetedTowerUnload(commander, crew.tower(), groupId);
        });
        helper.runAfterDelay(450, () -> {
            for (LivingEntity recruit : recruits) {
                if (bridgeDown) {
                    helper.assertTrue(recruit.getVehicle() != crew.tower()
                                    && recruit.getZ() > crew.tower().getZ() + 4.0D,
                            "A recruit did not go out over the bridge that was already down: " + recruit.position());
                } else {
                    helper.assertTrue(recruit.getVehicle() == crew.tower(),
                            "A recruit left a tower whose bridge nobody could lower: " + recruit.position());
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 460, batch = "recruits_tower_states")
    public static void towerCommandStatesFollowWhatTheOrdersCanDo(GameTestHelper helper) {
        if (!Platform.isModLoaded("recruits")) {
            helper.succeed();
            return;
        }
        buildFloor(helper);
        buildBridgeLanding(helper);
        TowerCrew crew = spawnTestTowerCrew(helper);
        ServerLevel level = helper.getLevel();
        LivingEntity recruit = createLivingEntity(level, "recruits:recruit");
        helper.assertTrue(recruit != null, "Failed to create a recruit");
        moveToRelative(helper, recruit, 14.5D, 1.0D, 4.0D);
        if (recruit instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(recruit);
        FakePlayer commander = SiegeGameTestPlayers.create(level);
        commander.setPos(crew.tower().getX(), crew.tower().getY(), crew.tower().getZ());
        java.util.UUID groupId = java.util.UUID.randomUUID();
        crew.tower().setDeployed(crew.driver(), true);
        helper.runAfterDelay(20, () -> {
            configureRecruitCommandIdentity(recruit, commander.getUUID(), groupId);
            assignRecruitMount(recruit, crew.tower().getUUID());
            helper.assertTrue(crew.tower().reserveInteriorSeat(recruit) && recruit.startRiding(crew.tower()),
                    "Recruit could not board the tower interior");
        });
        helper.runAfterDelay(200, () -> {
            crew.driver().stopRiding();
            Object states = commandStates(commander, groupId, crew.tower().getId());
            helper.assertTrue(commandTypes(states).contains("SIEGE_TOWER"),
                    "The tower tab was missing for a group riding in it without an engineer");
            helper.assertTrue(allows(states, "SIEGE_TOWER.leave"), "Leaving the tower was refused");
            helper.assertTrue(!allows(states, "SIEGE_TOWER.bridge_raise"),
                    "Raising the bridge was offered with nobody at the levers");
            helper.assertTrue(allows(states, "SIEGE_TOWER.unload_tower"),
                    "Unloading was refused over a bridge already lying on the wall");
            issueTargetedTowerUnload(commander, crew.tower(), groupId);
        });
        helper.runAfterDelay(420, () -> {
            helper.assertTrue(recruit.getVehicle() != crew.tower(),
                    "The unload the screen offered did not take the recruit out");
            helper.succeed();
        });
    }

    private static Object commandStates(FakePlayer commander, java.util.UUID groupId, int targetId) {
        try {
            return Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCommandStates")
                    .getMethod("compute", net.minecraft.server.level.ServerPlayer.class, java.util.List.class,
                            int.class, BlockPos.class)
                    .invoke(null, commander, java.util.List.of(groupId), targetId, null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not compute the command states", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> commandTypes(Object states) {
        try {
            return (java.util.List<String>) states.getClass().getMethod("typeNames").invoke(states);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read the command types", exception);
        }
    }

    private static boolean allows(Object states, String key) {
        try {
            return (boolean) states.getClass().getMethod("allows", String.class).invoke(states, key);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read a command state", exception);
        }
    }

    private static SiegeClimbableControl.ClimbResult climbToCompletion(
            SiegeLadderEntity ladder, LivingEntity climber, boolean upward) {
        SiegeClimbableControl.ClimbResult result = SiegeClimbableControl.ClimbResult.IN_PROGRESS;
        for (int i = 0; i < 300 && result == SiegeClimbableControl.ClimbResult.IN_PROGRESS; i++) {
            result = ladder.advanceAutomatedClimber(climber, upward);
        }
        return result;
    }

    private static void fireTestCannonBall(ServerLevel level, SerpentineEntity cannon,
                                           SiegeTowerEntity tower) {
        CannonProjectile projectile = new CannonProjectile(
                SiegeworksEntities.CANNON_BALL.get(), cannon, level);
        Vec3 start = new Vec3(tower.getX(), tower.getY() + 1.0D, tower.getZ() - 7.0D);
        projectile.setPos(start.x, start.y, start.z);
        projectile.setDeltaMovement(0.0D, 0.0D, 7.5D);
        projectile.setBaseDamage(cannon.getBaseDamage());
        projectile.setOwner(cannon);
        level.addFreshEntity(projectile);
    }

    private static TowerCrew spawnTestTowerCrew(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SiegeTowerEntity tower = SiegeworksEntities.SIEGE_TOWER_ENTITY.get().create(level);
        ArmorStand driver = EntityType.ARMOR_STAND.create(level);
        ArmorStand passenger = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(tower != null && driver != null && passenger != null,
                "Failed to create tower safety test entities");

        driver.addTag(TEST_OPERATOR_TAG);
        passenger.addTag(TEST_OPERATOR_TAG);
        moveToRelative(helper, tower, 16.0D, 1.0D, 6.0D);
        moveToRelative(helper, driver, 16.0D, 1.0D, 5.0D);
        moveToRelative(helper, passenger, 15.0D, 1.0D, 5.0D);
        level.addFreshEntity(tower);
        level.addFreshEntity(driver);
        level.addFreshEntity(passenger);
        helper.assertTrue(driver.startRiding(tower), "Test operator could not drive tower");
        helper.assertTrue(tower.reserveInteriorSeat(passenger), "Tower could not reserve a test passenger seat");
        helper.assertTrue(passenger.startRiding(tower), "Test passenger could not board tower");
        return new TowerCrew(tower, driver, passenger);
    }

    private static LivingEntity createLivingEntity(ServerLevel level, String id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(id));
        Entity entity = type == null ? null : type.create(level);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void orderRecruitToMove(LivingEntity recruit, BlockPos destination) {
        try {
            recruit.getClass().getMethod("setMovePos", BlockPos.class).invoke(recruit, destination);
            recruit.getClass().getMethod("setFollowState", int.class).invoke(recruit, 0);
            recruit.getClass().getMethod("setShouldMovePos", boolean.class).invoke(recruit, true);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not issue a Recruits move order", exception);
        }
    }

    private static void configureRecruitCommandIdentity(LivingEntity recruit, java.util.UUID ownerId,
                                                        java.util.UUID groupId) {
        try {
            Class<?> recruitType = Class.forName("com.talhanation.recruits.entities.AbstractRecruitEntity");
            recruitType.getMethod("setIsOwned", boolean.class).invoke(recruit, true);
            recruitType.getMethod("setOwnerUUID", java.util.Optional.class)
                    .invoke(recruit, java.util.Optional.of(ownerId));
            recruitType.getMethod("setGroupUUID", java.util.UUID.class).invoke(recruit, groupId);
            recruitType.getMethod("setListen", boolean.class).invoke(recruit, true);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not configure Recruits command ownership", exception);
        }
    }

    private static void issueTargetedTowerUnload(net.minecraft.server.level.ServerPlayer commander,
                                                 SiegeTowerEntity tower, java.util.UUID groupId) {
        try {
            Class<?> compat = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCompat");
            compat.getMethod("handleTowerCrewCommand", net.minecraft.server.level.ServerPlayer.class,
                            int.class, int.class, java.util.List.class)
                    .invoke(null, commander, 2, tower.getId(), java.util.List.of(groupId));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not issue the targeted Recruits tower unload command", exception);
        }
    }

    private static boolean setTowerBridgeOverride(net.minecraft.server.level.ServerPlayer commander,
                                                  SiegeTowerEntity tower, boolean deployed) {
        try {
            Class<?> compat = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCompat");
            return (boolean) compat.getMethod("setTowerBridge",
                            net.minecraft.server.level.ServerPlayer.class,
                            SiegeTowerEntity.class, Boolean.class, boolean.class)
                    .invoke(null, commander, tower, deployed, false);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not set the Recruits tower bridge override", exception);
        }
    }

    private static void issueTargetedTowerReturn(net.minecraft.server.level.ServerPlayer commander,
                                                 SiegeTowerEntity tower, java.util.UUID groupId) {
        try {
            Class<?> compat = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCompat");
            compat.getMethod("handleTowerCrewCommand", net.minecraft.server.level.ServerPlayer.class,
                            int.class, int.class, java.util.List.class)
                    .invoke(null, commander, 3, tower.getId(), java.util.List.of(groupId));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not issue the targeted Recruits tower return command", exception);
        }
    }

    private static void assignRecruitMount(LivingEntity recruit, java.util.UUID mountId) {
        try {
            recruit.getClass().getMethod("shouldMount", boolean.class, java.util.UUID.class)
                    .invoke(recruit, false, mountId);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not assign the Recruits tower mount", exception);
        }
    }

    private static void issueLadderRelocationCommand(net.minecraft.server.level.ServerPlayer commander,
                                                     int action, UUID groupId, BlockPos targetPos,
                                                     int targetEntityId) {
        try {
            Class<?> compat = Class.forName("me.mss1r.siegeworks.integration.recruits.RecruitsCompat");
            compat.getMethod("handleSiegeCommand", net.minecraft.server.level.ServerPlayer.class,
                            int.class, java.util.List.class, BlockPos.class, int.class, ResourceLocation.class)
                    .invoke(null, commander, action, java.util.List.of(groupId), targetPos, targetEntityId, null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not issue the Recruits ladder relocation command", exception);
        }
    }

    private static ItemStack findRecruitLadderItem(LivingEntity recruit) {
        try {
            Object inventory = recruit.getClass().getMethod("getInventory").invoke(recruit);
            if (inventory instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (stack.is(SiegeworksItems.SIEGE_LADDER_SPAWNER.get())) {
                        return stack;
                    }
                }
            }
            return ItemStack.EMPTY;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not inspect the Recruits inventory", exception);
        }
    }

    private static boolean isRecruitTraversingLadder(LivingEntity recruit) {
        try {
            Class<?> recruitType = Class.forName("com.talhanation.recruits.entities.AbstractRecruitEntity");
            Class<?> traversal = Class.forName(
                    "me.mss1r.siegeworks.integration.recruits.RecruitsSiegeTraversal");
            var method = traversal.getDeclaredMethod("hasActiveLadderRoute", recruitType);
            method.setAccessible(true);
            return Boolean.TRUE.equals(method.invoke(null, recruit));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not inspect the Recruits ladder route", exception);
        }
    }

    private static int getActiveAutomatedClimberCount(SiegeLadderEntity ladder) {
        return ladder.getActiveAutomatedClimberCount();
    }

    private static String describeRecruitTraversal(LivingEntity recruit) {
        try {
            Class<?> recruitType = Class.forName("com.talhanation.recruits.entities.AbstractRecruitEntity");
            Class<?> traversal = Class.forName(
                    "me.mss1r.siegeworks.integration.recruits.RecruitsSiegeTraversal");
            var ladderRoutesField = traversal.getDeclaredField("LADDER_ROUTES");
            var handoffsField = traversal.getDeclaredField("ROUTE_HANDOFFS");
            ladderRoutesField.setAccessible(true);
            handoffsField.setAccessible(true);
            boolean ladderRoute = ((java.util.Map<?, ?>) ladderRoutesField.get(null)).containsKey(recruit);
            boolean handoff = ((java.util.Map<?, ?>) handoffsField.get(null)).containsKey(recruit);
            boolean shouldMove = (boolean) recruitType.getMethod("getShouldMovePos").invoke(recruit);
            Object movePos = recruitType.getMethod("getMovePos").invoke(recruit);
            String navigation = recruit instanceof Mob mob
                    ? "done=" + mob.getNavigation().isDone() + ", target=" + mob.getNavigation().getTargetPos()
                    : "not-a-mob";
            return "ladderRoute=" + ladderRoute + ", handoff=" + handoff
                    + ", shouldMove=" + shouldMove + ", movePos=" + movePos
                    + ", navigation={" + navigation + "}";
        } catch (ReflectiveOperationException exception) {
            return "unavailable: " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }

    private static void assertStationaryPivot(GameTestHelper helper, ServerLevel level,
                                              AbstractSiegeEntity siege, double relativeX) {
        ArmorStand operator = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(siege != null && operator != null,
                "Failed to create a differential-drive siege engine or its operator");

        moveToRelative(helper, siege, relativeX, 1.0D, 5.0D);
        moveToRelative(helper, operator, relativeX, 1.0D, 5.0D);
        siege.setYRot(0.0F);
        siege.setTrackedYaw(0.0F);
        operator.addTag(TEST_OPERATOR_TAG);
        helper.assertTrue(level.addFreshEntity(siege), "Failed to add differential-drive siege engine");
        helper.assertTrue(level.addFreshEntity(operator), "Failed to add differential-drive operator");
        helper.assertTrue(operator.startRiding(siege), "Operator could not mount differential-drive siege engine");

        Vec3 start = siege.position();
        siege.setMovementInput(0.0F, 1.0F);
        SiegeMovementPhysics.updateSiegeVelocity(siege);

        helper.assertTrue(Math.abs(Mth.wrapDegrees(siege.getYRot())) > 0.01F,
                "Differential-drive siege engine did not pivot while stationary");
        helper.assertTrue(siege.getCurrentDriveSpeed() == 0.0D,
                "Stationary pivot introduced forward drive speed");
        helper.assertTrue(Math.abs(siege.getX() - start.x) < 1.0E-6D
                        && Math.abs(siege.getZ() - start.z) < 1.0E-6D,
                "Stationary pivot translated the siege engine");
    }

    private static void assertMountedIgnition(GameTestHelper helper, ServerLevel level,
                                              Player operator,
                                              AbstractSiegeEntity siege) {
        helper.assertTrue(siege instanceof MountedSiegeItemControl,
                siege.getClass().getSimpleName() + " does not expose mounted item control");
        operator.stopRiding();
        siege.setPos(helper.absolutePos(new BlockPos(2, 1, 2)).getCenter());
        helper.assertTrue(level.addFreshEntity(siege), "Could not add ignitable siege engine");
        operator.setPos(siege.getX(), siege.getY(), siege.getZ());
        helper.assertTrue(operator.startRiding(siege, true), mountFailure(siege, operator));

        InteractionResult result = ((MountedSiegeItemControl) siege).handleMountedItem(
                operator, InteractionHand.MAIN_HAND, level);
        helper.assertTrue(result.consumesAction(), "Mounted ignition did not consume the interaction");
        helper.assertTrue(siege.getOperationState() == SiegeOperationState.LOADING,
                "Mounted ignition did not start the priming stage");
    }

    private static void assertMountedLoading(GameTestHelper helper, ServerLevel level,
                                             Player operator,
                                             AbstractSiegeEntity siege, ItemStack ammunition) {
        helper.assertTrue(siege instanceof MountedSiegeItemControl,
                siege.getClass().getSimpleName() + " does not expose mounted item control");
        operator.stopRiding();
        siege.setPos(helper.absolutePos(new BlockPos(4, 1, 4)).getCenter());
        helper.assertTrue(level.addFreshEntity(siege), "Could not add loadable siege engine");
        operator.setPos(siege.getX(), siege.getY(), siege.getZ());
        helper.assertTrue(operator.startRiding(siege, true), mountFailure(siege, operator));
        operator.setItemInHand(InteractionHand.MAIN_HAND, ammunition);

        MountedSiegeItemControl control = (MountedSiegeItemControl) siege;
        helper.assertTrue(control.acceptsMountedItem(operator, InteractionHand.MAIN_HAND),
                "Mounted loading item was not accepted");
        InteractionResult result = control.handleMountedItem(operator, InteractionHand.MAIN_HAND, level);
        helper.assertTrue(result.consumesAction(), "Mounted loading did not consume the interaction");
        helper.assertTrue(siege.getOperationState() == SiegeOperationState.LOADING,
                "Mounted loading did not start the normal loading stage");
    }

    private static void buildFloor(GameTestHelper helper) {
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static void buildLadderWall(GameTestHelper helper) {
        for (int x = 6; x <= 10; x++) {
            for (int y = 1; y <= 8; y++) {
                helper.setBlock(new BlockPos(x, y, 13), Blocks.STONE);
            }
        }
    }

    private static void buildUpperLanding(ServerLevel level, Vec3 bottomEntrance, Vec3 topExit) {
        Vec3 forward = topExit.subtract(bottomEntrance).multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        int floorY = BlockPos.containing(topExit).getY() - 2;
        for (int depth = 0; depth < 4; depth++) {
            for (int side = -2; side <= 2; side++) {
                Vec3 point = topExit.add(forward.scale(depth - 0.65D)).add(right.scale(side));
                level.setBlockAndUpdate(new BlockPos(
                                (int) Math.floor(point.x), floorY, (int) Math.floor(point.z)),
                        Blocks.OAK_LEAVES.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
            }
        }
    }

    private static void buildBridgeLanding(GameTestHelper helper) {
        for (int x = 13; x <= 19; x++) {
            for (int z = 11; z <= 15; z++) {
                for (int y = 1; y <= 11; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    private static void moveToRelative(GameTestHelper helper, Entity entity, double x, double y, double z) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        entity.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
    }

    private static String mountFailure(AbstractSiegeEntity siege, Entity operator) {
        return "Operator could not mount " + siege.getClass().getSimpleName()
                + ": fullyBuilt=" + siege.isFullyBuilt()
                + ", canAddPassenger=" + siege.canAddPassenger(operator)
                + ", passengers=" + siege.getPassengers().size()
                + ", operatorAlive=" + operator.isAlive()
                + ", operatorRemoved=" + operator.isRemoved()
                + ", sameLevel=" + (operator.level() == siege.level())
                + ", previousVehicle=" + operator.getVehicle();
    }

    private static boolean mantletAllowsShotAt(MantletEntity mantlet, double modelX, double modelY,
                                                boolean fromFront) {
        double worldX = mantlet.getX() - modelX / 16.0D;
        double worldY = mantlet.getY() + modelY / 16.0D;
        double direction = fromFront ? -1.0D : 1.0D;
        Vec3 start = new Vec3(worldX, worldY, mantlet.getZ() - direction * 2.0D);
        return mantlet.allowsProjectilePassage(start, start.add(0.0D, 0.0D, direction * 0.25D));
    }

    private record TowerCrew(SiegeTowerEntity tower, ArmorStand driver, ArmorStand passenger) {
    }

    private static final class TestTowerCrossbowBoltProjectile extends TowerCrossbowBoltProjectile {
        private TestTowerCrossbowBoltProjectile(EntityType<? extends TowerCrossbowBoltProjectile> type,
                                           LivingEntity shooter, ServerLevel level) {
            super(type, shooter, level);
        }

        private boolean canHitTarget(Entity target) {
            return canHitEntity(target);
        }

        private void hitTarget(Entity target) {
            onHitEntity(new EntityHitResult(target));
        }
    }

    private static final class TestArrow extends Arrow {
        private TestArrow(Level level) {
            super(EntityType.ARROW, level);
        }

        private boolean canHitTarget(Entity target) {
            return canHitEntity(target);
        }
    }
}
