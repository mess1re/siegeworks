package me.mss1r.siegeworks.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.mss1r.axiomata.blueprint.api.construction.BlueprintConstructionPlan;
import me.mss1r.axiomata.blueprint.api.construction.BuildQuality;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.siegeworks.block.StackedProjectileBlock;
import me.mss1r.siegeworks.data.profile.SiegeEngineProfile;
import me.mss1r.siegeworks.entity.projectile.MangonelPassengerProjectile;
import me.mss1r.siegeworks.entity.siege.MangonelEntity;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.siegeworks.api.SiegeActionResult;
import me.mss1r.siegeworks.data.profile.ProfileFormat;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksBlocks;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_crew_materials")
@PrefixGameTestTemplate(false)
public final class CrewAndMaterialsGameTests {
    @GameTest(template = "empty")
    public static void mangonelPayloadDoesNotOccupyTheDriverSeat(GameTestHelper helper) {
        var level = helper.getLevel();
        MangonelEntity mangonel = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        Player payload = SiegeGameTestPlayers.createRideable(level);
        Player driver = SiegeGameTestPlayers.createRideable(level);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8)));
        mangonel.setPos(origin);
        level.addFreshEntity(mangonel);
        CompoundTag saved = new CompoundTag();
        mangonel.addAdditionalSaveData(saved);
        saved.putUUID("LaunchPayload", payload.getUUID());
        mangonel.readAdditionalSaveData(saved);
        helper.assertTrue(payload.startRiding(mangonel), "Payload could not board");
        helper.assertTrue(mangonel.getControllingPassenger() == null, "Payload took the driver seat");
        helper.assertTrue(driver.startRiding(mangonel), "Payload prevented a driver from boarding");
        helper.assertTrue(mangonel.getControllingPassenger() == driver && !mangonel.isOperator(payload),
                "Payload became a second driver");
        mangonel.setWindingTime(0);
        mangonel.handleSiegeInteraction(driver, InteractionHand.MAIN_HAND, level);
        mangonel.onSiegeTick(level);
        mangonel.onSiegeTick(level);
        helper.assertTrue(payload.getVehicle() instanceof MangonelPassengerProjectile,
                "Shot did not transfer the payload onto its flight carrier");
        helper.assertTrue(driver.getVehicle() == mangonel && mangonel.getControllingPassenger() == driver,
                "Firing transferred the driver or lost its seat");
        payload.stopRiding();
        driver.stopRiding();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flightCarrierCannotHitItsOwnPassenger(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 10, 8)));
        MangonelEntity owner = SiegeworksEntities.MANGONEL_ENTITY.get().create(level);
        owner.setPos(origin.add(0, -5, 0));
        owner.setOperator(SiegeGameTestPlayers.create(level));
        MangonelPassengerProjectile carrier = new MangonelPassengerProjectile(
                SiegeworksEntities.MANGONEL_PASSENGER_PROJECTILE.get(), owner, level);
        carrier.setPos(origin);
        carrier.setDeltaMovement(new Vec3(0, 0.5D, 0.75D));
        level.addFreshEntity(carrier);
        var passenger = EntityType.ARMOR_STAND.create(level);
        passenger.setPos(origin.add(0, MangonelPassengerProjectile.PASSENGER_Y_OFFSET, 0));
        level.addFreshEntity(passenger);
        helper.assertTrue(passenger.startRiding(carrier, true), "Passenger could not board its carrier");
        carrier.tick();
        helper.assertTrue(!carrier.isRemoved() && passenger.getVehicle() == carrier,
                "Flight carrier collided with its own passenger");
        helper.assertTrue(carrier.position().distanceToSqr(origin) > 0.1D, "Flight carrier did not move");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyHandRetrievesOneProjectileAndPreservesWater(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = SiegeGameTestPlayers.create(level);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = SiegeworksBlocks.CANNON_BALL.get().defaultBlockState()
                .setValue(StackedProjectileBlock.COUNT, 2).setValue(StackedProjectileBlock.WATERLOGGED, true);
        level.setBlock(pos, state, 3);
        helper.assertTrue(take(state, helper, pos, player).consumesAction(), "Empty hand did not retrieve ammo");
        helper.assertTrue(level.getBlockState(pos).getValue(StackedProjectileBlock.COUNT) == 1,
                "Pickup did not remove exactly one projectile");
        helper.assertTrue(player.getInventory().countItem(state.getBlock().asItem()) == 1,
                "Pickup did not give exactly one projectile");
        player.getInventory().selected = 1;
        take(level.getBlockState(pos), helper, pos, player);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER), "Last pickup deleted the water");
        helper.assertTrue(player.getInventory().countItem(state.getBlock().asItem()) == 2, "Last pickup lost ammo");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void heldItemDoesNotRetrieveGroundAmmo(GameTestHelper helper) {
        Player player = SiegeGameTestPlayers.create(helper.getLevel());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = SiegeworksBlocks.CANNON_BALL.get().defaultBlockState();
        helper.getLevel().setBlock(pos, state, 3);
        take(state, helper, pos, player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).equals(state), "Failed pickup deleted ground ammo");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blueprintQualityDoesNotAddWheels(GameTestHelper helper) {
        BlueprintDefinition recipe = BlueprintDefinitions.get("siegeworks:arcballista");
        helper.assertTrue(recipe != null, "Arcballista blueprint was not loaded");
        var plan = BlueprintConstructionPlan.of(recipe, BuildQuality.SLOPPY);
        int wheels = plan.stages().stream().flatMap(stage -> stage.materials().stream())
                .filter(material -> material.key().equals("siegeworks:wheel"))
                .mapToInt(BlueprintDefinition.Material::count).sum();
        helper.assertTrue(wheels == 2, "Poor drawing requires " + wheels + " wheels instead of two");
        helper.assertTrue(plan.stages().stream().flatMap(stage -> stage.materials().stream())
                        .filter(material -> material.key().equals("#minecraft:logs"))
                        .mapToInt(BlueprintDefinition.Material::count).sum() > 8,
                "Excluding wheels also removed raw-material waste");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ramImpactProfileIsIndependentOfEntityDamage(GameTestHelper helper) {
        var json = JsonParser.parseString(
                "{\"baseDamage\":1,\"ramImpact\":{\"mass\":1000,\"width\":5,\"height\":5,\"spread\":1}}");
        helper.assertTrue(ProfileFormat.engine(json).isEmpty(), "Format check rejected ramImpact");
        var profile = SiegeEngineProfile.CODEC.parse(JsonOps.INSTANCE, json)
                .result().orElseThrow();
        helper.assertTrue(profile.validationError().isEmpty() && profile.ramImpact().width() == 5,
                "Ram contact area could not be configured");
        helper.assertTrue(profile.ramImpact().mass() == 1000 && profile.ramImpact().spread() == 1,
                "Ram strike physics still depends on entity damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ramCracksStoneAndNeighboursWithoutBreakingThroughTheWall(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8)));
        BatteringRamEntity ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        ram.setPos(origin);
        ram.yBodyRot = 0;
        level.addFreshEntity(ram);
        Player operator = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(operator.startRiding(ram, true), "Ram operator could not board");
        ram.setOperator(operator);
        BlockPos target = BlockPos.containing(origin.add(0, 36.0D / 16, 75.0D / 16));
        BlockPos beside = target.offset(1, 0, 0);
        BlockPos behind = target.offset(0, 0, 1);
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(beside, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(behind, Blocks.STONE.defaultBlockState(), 3);
        for (int hit = 0; hit < 2; hit++) {
            ram.setCooldown(0);
            helper.assertTrue(ram.advancePrimaryAction(operator, new SimpleContainer()) == SiegeActionResult.FIRED,
                    "Ram did not start swing " + hit);
            for (int tick = 0; tick < 80; tick++) ram.onSiegeTick(level);
            if (hit == 0) {
                float progress = StructuralDamageSystem.progress(level, target, level.getBlockState(target));
                helper.assertTrue(progress > 0.01F && progress < 0.4F,
                        "First ram hit did not use its energy budget: " + progress);
            }
        }
        helper.assertTrue(level.getBlockState(target).is(Blocks.STONE), "Ram broke stone in two hits");
        float central = StructuralDamageSystem.progress(level, target, level.getBlockState(target));
        float adjacent = StructuralDamageSystem.progress(level, beside, level.getBlockState(beside));
        helper.assertTrue(level.getBlockState(beside).is(Blocks.STONE)
                        && adjacent > 0 && adjacent < central,
                "Ram did not attenuate damage along the wall: " + central + "/" + adjacent);
        helper.assertTrue(StructuralDamageSystem.progress(level, behind, level.getBlockState(behind)) == 0,
                "Ram damaged the second wall layer");
        operator.stopRiding();
        ram.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialTagLabelUsesTranslationKeyAndIdFallback(GameTestHelper helper) {
        var material = BlueprintDefinition.Material.ofTag(MinecraftVersionCompat.id("example", "parts/frame"), 1);
        TranslatableContents wrapper = (TranslatableContents) material.displayName().getContents();
        TranslatableContents name = (TranslatableContents) ((Component) wrapper.getArgs()[0]).getContents();
        helper.assertTrue(name.getKey().equals("tag.item.example.parts.frame"), "Wrong tag translation key");
        helper.assertTrue("#example:parts/frame".equals(name.getFallback()), "Unnamed tag did not retain its ID");
        helper.succeed();
    }

    private static InteractionResult take(BlockState state, GameTestHelper helper, BlockPos pos, Player player) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        //? if forge {
        /*return state.use(helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        *///?} else {
        return state.useWithoutItem(helper.getLevel(), player, hit);
        //?}
    }
}
