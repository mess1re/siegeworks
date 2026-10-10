package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.ballistics.damage.StructuralDamageSystem;
import me.mss1r.axiomata.collision.OrientedBox;
import me.mss1r.axiomata.collision.Rotation3;
import me.mss1r.siegeworks.gameplay.ballistics.SiegeBallisticsEnvironment;
import me.mss1r.siegeworks.config.SiegeworksServerConfig;
import me.mss1r.siegeworks.config.SiegeBlockDamage;
import me.mss1r.siegeworks.entity.siege.BatteringRamEntity;
import me.mss1r.siegeworks.registry.SiegeworksEntities;
import me.mss1r.siegeworks.api.SiegeActionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.SimpleContainer;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_crew_materials")
@PrefixGameTestTemplate(false)
public final class ContactImpactGameTests {
    @GameTest(template = "empty")
    public static void neighbouringBlocksShareRatherThanMultiplyEnergy(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 8, 8));
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
            level.setBlock(center.offset(x, y, 0), stone, 3);
        }
        double energy = SiegeBallisticsEnvironment.IMPACTS.material(level, center, stone).breakEnergy() * 0.1D;
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(center, Rotation3.IDENTITY), energy, 1.25D, null);
        double used = 0;
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
            used += StructuralDamageSystem.progress(level, center.offset(x, y, 0), stone);
        }
        helper.assertTrue(Math.abs(used - 0.1D) < 1.0E-6D, "Contact impact multiplied or lost energy: " + used);
        helper.assertTrue(StructuralDamageSystem.progress(level, center.offset(1, 0, 0), stone) > 0,
                "No neighbouring surface damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyContactCannotDamageNearbyOrDisconnectedBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 8, 8));
        BlockPos beside = center.offset(1, 0, 0);
        BlockState stone = Blocks.STONE.defaultBlockState();
        level.setBlock(beside, stone, 3);
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(center, Rotation3.IDENTITY), 10000, 1.25D, null);
        helper.assertTrue(StructuralDamageSystem.progress(level, beside, stone) == 0, "Miss struck a nearby block");
        level.setBlock(center, stone, 3);
        BlockPos far = center.offset(-2, 0, 0);
        level.setBlock(far, stone, 3);
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(center, Rotation3.IDENTITY), 10000, 3, null);
        helper.assertTrue(StructuralDamageSystem.progress(level, far, stone) == 0, "Strike crossed an air gap");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void strikeRotatesAndWoodYieldsBeforeStone(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos stonePos = helper.absolutePos(new BlockPos(5, 8, 8));
        BlockPos woodPos = helper.absolutePos(new BlockPos(10, 8, 8));
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState wood = Blocks.OAK_PLANKS.defaultBlockState();
        level.setBlock(stonePos, stone, 3);
        level.setBlock(woodPos, wood, 3);
        var rotation = Rotation3.aroundY((float) (Math.PI / 2));
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(stonePos, rotation), 5000, 0, null);
        SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(woodPos, rotation), 5000, 0, null);
        helper.assertTrue(StructuralDamageSystem.progress(level, woodPos, wood)
                        > StructuralDamageSystem.progress(level, stonePos, stone),
                "Wood resisted a blunt strike better than stone");
        helper.assertTrue(Math.abs(BatteringRamEntity.strikeSpeed() - 3.375D) < 0.01D,
                "Swing speed is not measured from the animation in metres per second");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terrainPolicyAlsoAppliesToContactStrikes(GameTestHelper helper) {
        var before = SiegeworksServerConfig.getBlockDamage();
        try {
            SiegeworksServerConfig.setBlockDamage(SiegeBlockDamage.NEVER);
            var level = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(8, 8, 8));
            BlockState stone = Blocks.STONE.defaultBlockState();
            level.setBlock(pos, stone, 3);
            SiegeBallisticsEnvironment.IMPACTS.contactImpact(level, contact(pos, Rotation3.IDENTITY), 1000000, 1.25, null);
            helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE)
                            && StructuralDamageSystem.progress(level, pos, stone) == 0,
                    "Contact impact ignored the terrain policy");
        } finally {
            SiegeworksServerConfig.setBlockDamage(before);
        }
        helper.succeed();
    }

    private static OrientedBox contact(BlockPos pos, Rotation3 rotation) {
        return new OrientedBox(Vec3.atCenterOf(pos), new Vec3(0.3125D, 0.3125D, 0.375D), rotation);
    }

    @GameTest(template = "empty")
    public static void wheelRecoilDoesNotReplaceTheTravelRotation(GameTestHelper helper) {
        var level = helper.getLevel();
        var ram = SiegeworksEntities.BATTERING_RAM_ENTITY.get().create(level);
        ram.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(8, 8, 8))));
        level.addFreshEntity(ram);
        var player = SiegeGameTestPlayers.createRideable(level);
        helper.assertTrue(player.startRiding(ram, true), "Operator could not board the ram");
        ram.setOperator(player);
        ram.wheelRotation = 137;
        helper.assertTrue(ram.advancePrimaryAction(player, new SimpleContainer()) == SiegeActionResult.FIRED,
                "Ram could not start an attack");
        helper.assertTrue(ram.wheelRecoilDegrees(0) == 0 && ram.getWheelRotation() == 137,
                "Starting an attack reset the wheel phase");
        float strongestRecoil = 0;
        for (int tick = 0; tick < 80; tick++) {
            ram.onSiegeTick(level);
            strongestRecoil = Math.min(strongestRecoil, ram.wheelRecoilDegrees(0));
        }
        helper.assertTrue(strongestRecoil < -8 && ram.wheelRecoilDegrees(0) == 0 && ram.getWheelRotation() == 137,
                "Recoil did not return to the wheel's travel angle");
        player.stopRiding();
        ram.discard();
        helper.succeed();
    }
}
