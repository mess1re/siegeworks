package me.mss1r.siegeworks.gametest;

import me.mss1r.axiomata.blueprint.blockentity.DrawingTableBlockEntity;
import me.mss1r.axiomata.blueprint.menu.DrawingTableMenu;
import me.mss1r.axiomata.blueprint.network.S2CTracingStatePacket;
import me.mss1r.axiomata.blueprint.registry.BlueprintBlocks;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import me.mss1r.axiomata.blueprint.tracing.TracingRules;
import me.mss1r.axiomata.blueprint.tracing.TracingState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
//? if forge {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("siegeworks_crew_materials")
@PrefixGameTestTemplate(false)
public final class DrawingTableGameTests {
    @GameTest(template = "empty")
    public static void closingTableKeepsMaterialsBeforeAndAfterDrawing(GameTestHelper helper) {
        var table = createTable(helper);
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        var menu = new DrawingTableMenu(1, player.getInventory(), table, table.getData());
        menu.removed(player);
        assertMaterials(helper, table);
        drawOnePoint(helper, table);
        float coverage = table.coverage();
        long revision = table.sheetRevision();
        menu.removed(player);
        assertMaterials(helper, table);
        helper.assertTrue(table.coverage() == coverage, "Closing the table discarded the drawing");
        table.select(table.blueprintId());
        helper.assertTrue(table.coverage() == coverage && table.sheetRevision() == revision,
                "Re-selecting the same blueprint cleared saved work");
        var restored = S2CTracingStatePacket.of(table, table.outline());
        helper.assertTrue(restored.covered().length > 0, "Reopened table has no ink to synchronize");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void spoiledPaperStartsANewSheetWithoutOldInk(GameTestHelper helper) {
        var table = createTable(helper);
        drawOnePoint(helper, table);
        long revision = table.sheetRevision();
        for (int i = 0; i < 10000 && table.state() != TracingState.RUINED; i++) {
            table.trace(new int[]{255, 0}, 1);
        }
        helper.assertTrue(table.state() == TracingState.RUINED && !table.hasPaper(), "Bad tracing did not spoil paper");
        helper.assertTrue(table.session() == null && table.sheetRevision() > revision,
                "Spoiled paper retained its server session or did not trigger synchronization");
        helper.assertTrue(S2CTracingStatePacket.of(table, table.outline()).covered().length == 0,
                "Spoiled paper would send old ink back to the client");
        table.getContainer().setItem(DrawingTableBlockEntity.SLOT_PAPER, new ItemStack(Items.PAPER));
        table.notePaperChanged();
        helper.assertTrue(table.state() == TracingState.READY && table.coverage() == 0,
                "New paper inherited the spoiled drawing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void removingPaperClearsInkAndResult(GameTestHelper helper) {
        var table = createTable(helper);
        drawOnePoint(helper, table);
        long revision = table.sheetRevision();
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        var menu = new DrawingTableMenu(1, player.getInventory(), table, table.getData());
        table.getContainer().setItem(DrawingTableBlockEntity.SLOT_PAPER, ItemStack.EMPTY);
        menu.refreshResult();
        helper.assertTrue(table.session() == null && table.sheetRevision() > revision,
                "Removing paper did not clear and synchronize the drawing");
        helper.assertTrue(table.getContainer().getItem(DrawingTableBlockEntity.SLOT_RESULT).isEmpty(),
                "Removing paper retained a result");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingTableReturnsMaterialsButNotTheResultPreview(GameTestHelper helper) {
        var table = createTable(helper);
        table.getContainer().setItem(DrawingTableBlockEntity.SLOT_RESULT, new ItemStack(BlueprintItems.BLUEPRINT.get()));
        var pos = table.getBlockPos();
        helper.getLevel().destroyBlock(pos, false);
        var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.PAPER))
                        && dropped.stream().anyMatch(item -> item.getItem().is(BlueprintItems.INK_BLOCK.get())),
                "Breaking the table lost stored materials");
        helper.assertTrue(dropped.stream().noneMatch(item -> item.getItem().is(BlueprintItems.BLUEPRINT.get())),
                "Breaking the table duplicated its result preview");
        helper.succeed();
    }

    private static DrawingTableBlockEntity createTable(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, BlueprintBlocks.DRAWING_TABLE.get().defaultBlockState(), 3);
        var table = (DrawingTableBlockEntity) helper.getLevel().getBlockEntity(pos);
        table.getContainer().setItem(DrawingTableBlockEntity.SLOT_PAPER, new ItemStack(Items.PAPER));
        table.getContainer().setItem(DrawingTableBlockEntity.SLOT_INK, new ItemStack(BlueprintItems.INK_BLOCK.get()));
        helper.assertTrue(table.select("siegeworks:arcballista"), "Arcballista outline is missing");
        return table;
    }

    @GameTest(template = "empty")
    public static void takingTheBlueprintClearsTheSheetAndConsumesMaterials(GameTestHelper helper) {
        var table = createTable(helper);
        var mask = table.outline().mask();
        int scale = TracingRules.CANVAS_PIXELS / mask.resolution();
        for (int y = 0; y < mask.resolution(); y++) {
            for (int x = 0; x < mask.resolution(); x++) {
                if (mask.ink(x, y)) {
                    table.liftPen();
                    table.trace(new int[]{x * scale, y * scale}, 1);
                }
            }
        }
        helper.assertTrue(table.state() == TracingState.DONE, "Drawing never completed");
        long revision = table.sheetRevision();
        var player = SiegeGameTestPlayers.createRideable(helper.getLevel());
        var menu = new DrawingTableMenu(1, player.getInventory(), table, table.getData());
        helper.assertTrue(!menu.takeResult(player).isEmpty(), "Completed blueprint could not be taken");
        helper.assertTrue(!table.hasPaper() && !table.hasInk(), "Taking a blueprint did not consume materials");
        helper.assertTrue(table.session() == null && table.sheetRevision() > revision,
                "Taking a blueprint did not clear and synchronize the sheet");
        helper.succeed();
    }

    private static void drawOnePoint(GameTestHelper helper, DrawingTableBlockEntity table) {
        var mask = table.outline().mask();
        int scale = TracingRules.CANVAS_PIXELS / mask.resolution();
        for (int y = 0; y < mask.resolution(); y++) {
            for (int x = 0; x < mask.resolution(); x++) {
                if (mask.ink(x, y)) {
                    table.trace(new int[]{x * scale, y * scale}, 1);
                    table.liftPen();
                    helper.assertTrue(table.coverage() > 0, "Good tracing did not paint the sheet");
                    return;
                }
            }
        }
        helper.fail("Outline has no ink");
    }

    private static void assertMaterials(GameTestHelper helper, DrawingTableBlockEntity table) {
        helper.assertTrue(table.hasPaper() && table.hasInk(), "Closing the table returned its materials");
    }
}
