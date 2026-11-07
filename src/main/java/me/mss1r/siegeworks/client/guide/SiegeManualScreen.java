package me.mss1r.siegeworks.client.guide;

import com.mojang.blaze3d.platform.Lighting;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.registry.BlueprintItems;
import me.mss1r.siegeworks.client.gui.GuiPalette;
import me.mss1r.siegeworks.entity.siege.AbstractFieldGunEntity;
import me.mss1r.siegeworks.client.SiegeworksKeyMappings;
import me.mss1r.siegeworks.gameplay.firearm.FirearmActions;
import me.mss1r.siegeworks.item.FirearmItem;
import me.mss1r.siegeworks.item.SiegeLadderDeploymentItem;
import me.mss1r.siegeworks.client.guide.ManualEntry.Chapter;
import me.mss1r.siegeworks.item.ArtilleryPowderCosts;
import me.mss1r.siegeworks.item.RoundShotItem;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import me.mss1r.siegeworks.registry.SiegeworksItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static me.mss1r.siegeworks.client.guide.ManualEntry.*;

public final class SiegeManualScreen extends Screen {
    private static final ResourceLocation NAVIGATION = art("gui/siege_manual_navigation.png");
    private static final String WIKI_URL = "https://github.com/mess1re/siegeworks/wiki";
    private static final ResourceLocation CONTROLS = art("gui/siege_manual_controls.png");
    private static final ResourceLocation PAPER = art("gui/siege_manual.png");
    private static final ResourceLocation CULVERIN_DRAWING = art("blueprint/culverin.png");
    private static final ResourceLocation COLORS = MinecraftVersionCompat.id("siegeworks", "gui/siege_manual.properties");
    private static final int PAPER_WIDTH = 312;
    private static final int PAPER_HEIGHT = 184;
    private static final int[] PAPER_X = {0, 18, 145, 165, 248, PAPER_WIDTH};
    private static final int[] PAPER_Y = {0, 37, 148, PAPER_HEIGHT};
    private static ManualEntry lastPage;
    private static Chapter lastChapter;
    private static int lastLeaf;

    private ManualEntry page = lastPage;
    private Chapter chapter = lastChapter;
    private Layout layout;
    private TowerGuidePreview tower;
    private record Place(ManualEntry page, Chapter chapter, int leaf) {}
    private final java.util.ArrayDeque<Place> history = new java.util.ArrayDeque<>();
    private final GuideEngineModels models = new GuideEngineModels();
    private final GuideModelPreview modelPreview = new GuideModelPreview();
    private final Map<String, Optional<GuideRecipe>> recipes = new HashMap<>();
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private int leaf = lastLeaf;
    private final Map<ManualEntry, List<List<FormattedCharSequence>>> leaves = new java.util.EnumMap<>(ManualEntry.class);
    private final Map<ManualEntry, Integer> entryFolios = new java.util.EnumMap<>(ManualEntry.class);
    private final Map<Chapter, Integer> chapterFolios = new java.util.EnumMap<>(Chapter.class);
    private int ink;
    private int heading;
    private int link;
    private int line;

    private SiegeManualScreen() {
        super(Component.translatable("item.siegeworks.siege_manual"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new SiegeManualScreen());
    }

    @Override
    protected void init() {
        int bookWidth = Math.min(452, width - 12);
        int bookHeight = Math.min(244, height - 12);
        layout = new Layout((width - bookWidth) / 2, (height - bookHeight) / 2, bookWidth, bookHeight);
        GuiPalette palette = GuiPalette.load(minecraft.getResourceManager(), COLORS);
        ink = palette.color("text", 0x48382A);
        heading = palette.color("heading", 0x493223);
        link = palette.color("link", 0x324F65);
        line = palette.color("line", 0xB29B75);
        if (page != null && page.modelId() != null && minecraft.level != null) {
            var model = models.get(page.modelId(), minecraft.level);
            if (model instanceof TowerGuidePreview preview) tower = preview;
        }
        paginate();
        if (page != null) leaf = Mth.clamp(leaf, 0, leaves.get(page).size() - 1);
        buildNavigation();
    }

    private void buildNavigation() {
        clearWidgets();
        if (page == null && chapter == null) {
            for (ContentsLink row : contentsLinks()) {
                addRenderableWidget(new TextButton(row.x, row.y, layout.column() - 23,
                        row.height, 0, chapterTitle(row.chapter), button -> goToChapter(row.chapter)));
            }
            Component wiki = key("wiki");
            int height = textHeight(wiki, layout.column()) + 2;
            var wikiLink = new TextButton(layout.rightText(), layout.footer() - height - 8,
                    layout.column(), height, 0, wiki, button -> handleComponentClicked(
                            wiki.copy().withStyle(style -> style.withClickEvent(
                                    new ClickEvent(ClickEvent.Action.OPEN_URL, WIKI_URL))).getStyle()));
            wikiLink.setTooltip(Tooltip.create(Component.literal(WIKI_URL)));
            addRenderableWidget(wikiLink);
            return;
        }

        Component contents = key("all_chapters");
        int labelWidth = Math.min(font.width(contents), layout.column());
        addRenderableWidget(new TextButton(layout.ribbonX() - labelWidth - 6, layout.y + 16,
                labelWidth, textHeight(contents, labelWidth) + 2, 0, contents, button -> goToContents()));

        if (page == null) {
            for (IndexLink row : indexLinks()) {
                addRenderableWidget(new TextButton(row.x, row.y, layout.column() - 23,
                        row.height, 2, entryLabel(row.entry), button -> goTo(row.entry)));
            }
        } else {
            Component label = chapterTitle(chapter);
            addRenderableWidget(new TextButton(layout.leftText(), layout.y + 16, layout.column(),
                    textHeight(label, layout.column()) + 2, 0, label, button -> goToChapter(chapter)));
        }

        addRenderableWidget(new TurnPageButton(layout.leftText(), false));
        if (page == null || leaf + 1 < leaves.get(page).size() || page.ordinal() + 1 < ManualEntry.values().length) {
            addRenderableWidget(new TurnPageButton(layout.rightText() + layout.column() - 23, true));
        }
    }

    private int contentsSplit() {
        return (Chapter.values().length + 1) / 2;
    }

    private record ContentsLink(Chapter chapter, int x, int y, int height) {}

    private List<ContentsLink> contentsLinks() {
        var rows = new java.util.ArrayList<ContentsLink>();
        int leftY = Math.max(layout.y + 22 + textHeight(title, layout.column()) + 12, layout.textTop() + 5);
        int rightY = rightTextTop() + 5;
        for (Chapter group : Chapter.values()) {
            boolean left = group.ordinal() < contentsSplit();
            int height = textHeight(chapterTitle(group), layout.column() - 23) + 3
                    + textHeight(key("chapter." + group.id + ".summary"), layout.column()) + 9;
            rows.add(new ContentsLink(group, left ? layout.leftText() : layout.rightText(),
                    left ? leftY : rightY, height));
            if (left) leftY += height;
            else rightY += height;
        }
        return rows;
    }

    private boolean turnPage(int direction) {
        if (page != null && leaf + direction >= 0 && leaf + direction < leaves.get(page).size()) {
            history.addLast(new Place(page, chapter, leaf));
            leaf += direction;
            lastLeaf = leaf;
            buildNavigation();
            return true;
        }
        if (page == null) {
            if (chapter == null) return false;
            if (direction > 0) {
                goTo(chapter.entries().get(0));
            } else if (chapter.ordinal() == 0) {
                goToContents();
            } else {
                var previous = Chapter.values()[chapter.ordinal() - 1].entries();
                goTo(previous.get(previous.size() - 1));
                showLastLeaf();
            }
            return true;
        }
        int next = page.ordinal() + direction;
        if (direction < 0 && page == chapter.entries().get(0)) {
            goToChapter(chapter);
        } else if (next >= 0 && next < ManualEntry.values().length) {
            var target = ManualEntry.values()[next];
            if (target.chapter != chapter) goToChapter(target.chapter);
            else {
                goTo(target);
                if (direction < 0) showLastLeaf();
            }
        } else {
            return false;
        }
        return true;
    }

    private void showLastLeaf() {
        leaf = leaves.get(page).size() - 1;
        lastLeaf = leaf;
        buildNavigation();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE
                || keyCode == GLFW.GLFW_KEY_LEFT && (modifiers & GLFW.GLFW_MOD_ALT) != 0) return goBack();
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP && turnPage(-1)) return true;
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN && turnPage(1)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_4) return goBack();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void navigate(ManualEntry targetPage, Chapter targetChapter) {
        if (page == targetPage && chapter == targetChapter) return;
        history.addLast(new Place(page, chapter, leaf));
        page = targetPage;
        chapter = targetChapter;
        rememberAndInit();
    }

    private boolean goBack() {
        Place previous = history.pollLast();
        if (previous == null) return false;
        page = previous.page;
        chapter = previous.chapter;
        rememberAndInit();
        leaf = Mth.clamp(previous.leaf, 0, page == null ? 0 : leaves.get(page).size() - 1);
        lastLeaf = leaf;
        buildNavigation();
        return true;
    }

    private void goTo(ManualEntry target) {
        navigate(target, target.chapter);
    }

    private void goToChapter(Chapter target) {
        navigate(null, target);
    }

    private void goToContents() {
        navigate(null, null);
    }

    private void rememberAndInit() {
        lastPage = page;
        lastChapter = chapter;
        leaf = 0;
        lastLeaf = 0;
        init();
    }

    private static Component chapterTitle(Chapter chapter) {
        return key("chapter." + chapter.id);
    }

    @Override
    public void tick() {
        models.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredItem = ItemStack.EMPTY;
        graphics.fill(0, 0, width, height, 0x900E0A06);
        renderPaper(graphics);
        Lighting.setupFor3DItems();
        if (page != null) {
            graphics.enableScissor(layout.leftText(), illustrationTop(),
                    layout.leftText() + layout.column(), layout.footer() - 7);
            renderIllustration(graphics, mouseX, mouseY);
            graphics.disableScissor();
        } else if (chapter != null) {
            renderChapter(graphics);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        Component headingText = rightHeading();
        int x = layout.rightText();
        int y = layout.y + 32;
        int headingBottom = drawWrapped(graphics, headingText, x, y, layout.column(), heading);
        graphics.hLine(x, x + layout.column() - 1, headingBottom + 3, opaque(line));

        if (page == null && chapter == null) {
            renderContents(graphics);
        } else if (page == null) {
            renderChapterIndex(graphics);
        } else {
            renderInstructions(graphics);
            drawFolio(graphics, layout.leftText(), entryFolios.get(page) + leaf * 2);
            drawFolio(graphics, layout.rightText(), entryFolios.get(page) + leaf * 2 + 1);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!hoveredItem.isEmpty()) graphics.renderTooltip(font, hoveredItem, mouseX, mouseY);
        graphics.pose().popPose();
    }

    private void drawFolio(GuiGraphics graphics, int x, int number) {
        String text = Integer.toString(number);
        graphics.drawString(font, text, x + (layout.column() - font.width(text)) / 2,
                layout.footer() + 4, ink, false);
    }

    private void renderContents(GuiGraphics graphics) {
        int titleY = layout.y + 22;
        for (var row : font.split(title, layout.column())) {
            graphics.drawString(font, row, layout.leftText(), titleY, heading, false);
            titleY += font.lineHeight + 1;
        }
        for (ContentsLink row : contentsLinks()) {
            String folio = Integer.toString(chapterFolios.get(row.chapter));
            graphics.drawString(font, folio, row.x + layout.column() - font.width(folio), row.y, heading, false);
            int summaryY = row.y + textHeight(chapterTitle(row.chapter), layout.column() - 23) + 3;
            drawWrapped(graphics, key("chapter." + row.chapter.id + ".summary"), row.x, summaryY, layout.column(), ink);
        }
    }

    private boolean spreadIndex() {
        return chapter != null && chapter.entries().size() > 10;
    }

    private Component rightHeading() {
        return page != null ? key(page.id) : chapter == null ? key("contents")
                : spreadIndex() ? key("in_chapter") : chapterTitle(chapter);
    }

    private int rightTextTop() {
        return layout.y + 32 + textHeight(rightHeading(), layout.column()) + 10;
    }

    private Component entryLabel(ManualEntry entry) {
        String path = "gui.siegeworks.manual." + entry.id + ".index";
        return net.minecraft.client.resources.language.I18n.exists(path)
                ? Component.translatable(path) : key(entry.id);
    }

    private record IndexLink(ManualEntry entry, int x, int y, int height) {}

    private List<IndexLink> indexLinks() {
        var links = new java.util.ArrayList<IndexLink>();
        var entries = chapter.entries();
        int split = spreadIndex() ? (entries.size() + 1) / 2 : 0;
        int leftY = layout.y + 32 + textHeight(chapterTitle(chapter), layout.column()) + 10;
        int rightY = rightTextTop();
        int leftGap = indexGap(entries.subList(0, split), leftY);
        int rightGap = indexGap(entries.subList(split, entries.size()), rightY);
        for (int index = 0; index < entries.size(); index++) {
            boolean left = index < split;
            ManualEntry entry = entries.get(index);
            int height = textHeight(entryLabel(entry), layout.column() - 23) + (left ? leftGap : rightGap);
            links.add(new IndexLink(entry, left ? layout.leftText() : layout.rightText(), left ? leftY : rightY, height));
            if (left) leftY += height;
            else rightY += height;
        }
        return links;
    }

    private int indexGap(List<ManualEntry> entries, int top) {
        if (entries.isEmpty()) return 0;
        int text = entries.stream().mapToInt(entry -> textHeight(entryLabel(entry), layout.column() - 23)).sum();
        return Mth.clamp((layout.footer() - 7 - top - text) / entries.size(), 1, 6);
    }

    private void renderChapter(GuiGraphics graphics) {
        if (spreadIndex()) return;
        int headingBottom = layout.y + 28 + textHeight(chapterTitle(chapter), layout.column());
        drawNote(graphics, chapterTitle(chapter), layout.y + 28);
        int top = headingBottom + 12;
        ItemStack illustration = switch (chapter) {
            case CONSTRUCTION -> new ItemStack(BlueprintItems.DRAWING_TABLE.get());
            case AMMUNITION -> new ItemStack(SiegeworksItems.FIRE_PROJECTILE.get());
            case FIREARMS -> new ItemStack(SiegeworksItems.ARQUEBUS.get());
            case SERVICE -> new ItemStack(BlueprintItems.CONSTRUCTION_HAMMER.get());
            default -> ItemStack.EMPTY;
        };
        renderHeroItems(graphics, List.of(illustration), top, layout.footer() - top - 12);
    }

    private void renderChapterIndex(GuiGraphics graphics) {
        if (spreadIndex()) {
            int bottom = drawWrapped(graphics, chapterTitle(chapter), layout.leftText(), layout.y + 32,
                    layout.column(), heading);
            graphics.hLine(layout.leftText(), layout.leftText() + layout.column() - 1, bottom + 3, opaque(line));
        }
        for (IndexLink row : indexLinks()) {
            String folio = Integer.toString(entryFolios.get(row.entry));
            graphics.drawString(font, folio, row.x + layout.column() - font.width(folio), row.y + 2, ink, false);
        }
        drawFolio(graphics, layout.leftText(), chapterFolios.get(chapter));
        drawFolio(graphics, layout.rightText(), chapterFolios.get(chapter) + 1);
    }

    private void renderPaper(GuiGraphics graphics) {
        // Stretch the paper between the edges, keeping the corners, spine and bookmark at their native size.
        int middle = layout.width / 2;
        int[] xs = {0, 18, middle - 11, middle + 9, layout.width - 64, layout.width};
        int[] ys = {0, 37, layout.height - 36, layout.height};
        for (int row = 0; row < ys.length - 1; row++) {
            for (int col = 0; col < xs.length - 1; col++) {
                graphics.blit(PAPER, layout.x + xs[col], layout.y + ys[row], xs[col + 1] - xs[col],
                        ys[row + 1] - ys[row], PAPER_X[col], PAPER_Y[row],
                        PAPER_X[col + 1] - PAPER_X[col], PAPER_Y[row + 1] - PAPER_Y[row],
                        PAPER_WIDTH, PAPER_HEIGHT);
            }
        }
    }

    private int illustrationTop() {
        return Math.max(layout.y + 36, layout.y + 16 + textHeight(chapterTitle(chapter), layout.column()) + 10);
    }

    private void renderIllustration(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = illustrationTop();
        int fullHeight = layout.footer() - top - 7;
        if (page == DRAW) {
            drawArt(graphics, CULVERIN_DRAWING, top, fullHeight - 27);
            drawRow(graphics, mouseX, mouseY, top + fullHeight - 20, List.of(
                    new ItemStack(Items.PAPER), new ItemStack(BlueprintItems.INK_BLOCK.get()), blueprint("culverin")));
        } else if (page == BUILD || page == FIREARM_CRAFT) {
            if (page == BUILD) {
                int bottom = drawMaterials(graphics, mouseX, mouseY, "culverin", top);
                drawNote(graphics, key("build.quality"), bottom + 14);
            } else {
                int bottom = drawMaterials(graphics, mouseX, mouseY, "hand_cannon", top);
                drawMaterials(graphics, mouseX, mouseY, "arquebus", bottom + 16);
            }
        } else if (!page.recipes.isEmpty()) {
            int y = top;
            for (String recipe : page.recipes) {
                var parsed = recipe(recipe);
                if (parsed.isPresent()) {
                    y = drawRecipe(graphics, mouseX, mouseY, y, parsed.get());
                }
            }
            if (page == LADDER_CRAFT) {
                var base = recipe("siegeworks:siege_ladder_spawner");
                if (base.isPresent()) {
                    var ingredients = new java.util.ArrayList<>(base.get().ingredients());
                    ingredients.set(7, new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()));
                    var longer = SiegeLadderDeploymentItem.withSections(
                            new ItemStack(SiegeworksItems.SIEGE_LADDER_SPAWNER.get()), 2);
                    drawRecipe(graphics, mouseX, mouseY, y, new GuideRecipe(ingredients, longer, 3));
                }
            }
        } else if (page == POT_FILLING) {
            drawRow(graphics, mouseX, mouseY, top, stacks(Items.CHARCOAL, Items.HONEYCOMB, Items.STRING));
            var ingredients = new net.minecraft.world.item.Item[]{Items.CHARCOAL, Items.HONEYCOMB, Items.BLAZE_POWDER, Items.GUNPOWDER};
            int y = top + 28;
            for (int index = 0; index < ingredients.length; index++) {
                drawItem(graphics, mouseX, mouseY, new ItemStack(ingredients[index]), layout.leftText(), y);
                int bottom = drawWrapped(graphics, key("pot.additive." + index), layout.leftText() + 24,
                        y + 1, layout.column() - 24, ink);
                y = Math.max(y + 18, bottom) + 6;
            }
        } else if (page.modelId() != null) {
            var model = models.get(page.modelId(), minecraft.level);
            int artHeight = page == TOWER ? fullHeight : fullHeight - 27;
            var projection = modelPreview.render(graphics, model, layout.leftText(), top,
                    layout.column() - (page == TOWER ? 12 : 0), artHeight,
                    null, page.modelId().equals("siege_tower") ? -148 : -40);
            if (page == TOWER && tower != null) {
                if (projection != null) {
                    tower.drawCallouts(graphics, font, projection, layout.leftText() + layout.column() - 7, link);
                }
            } else {
                drawRow(graphics, mouseX, mouseY, top + fullHeight - 19, entryItems());
            }
        } else {
            renderHeroItems(graphics, entryItems(), top + 12, fullHeight - 24);
        }
    }

    private Optional<GuideRecipe> recipe(String id) {
        return recipes.computeIfAbsent(id, key -> GuideRecipe.read(minecraft.level, ResourceLocation.tryParse(key)));
    }

    private ItemStack blueprint(String id) {
        var definition = BlueprintDefinitions.get("siegeworks:" + id);
        return definition == null ? new ItemStack(BlueprintItems.BLUEPRINT.get())
                : BlueprintItem.create(definition, "siegeworks:" + id);
    }

    private int drawMaterials(GuiGraphics graphics, int mouseX, int mouseY, String id, int top) {
        var definition = BlueprintDefinitions.get("siegeworks:" + id);
        if (definition == null) return top;
        var output = BuiltInRegistries.ITEM.get(definition.result().item());
        int itemsY = drawWrapped(graphics, output.getDescription(), layout.leftText(), top, layout.column(), heading) + 4;
        var materials = definition.totals().stream().map(material -> {
            ItemStack stack = material.displayStack();
            stack.setCount(material.count());
            return stack;
        }).toList();
        for (int index = 0; index < materials.size(); index += 4) {
            drawRow(graphics, mouseX, mouseY, itemsY + index / 4 * 21,
                    materials.subList(index, Math.min(index + 4, materials.size())));
        }
        return itemsY + (materials.size() + 3) / 4 * 21;
    }

    private void renderHeroItems(GuiGraphics graphics, List<ItemStack> items, int top, int height) {
        if (items.isEmpty()) return;
        int heroHeight = height - (items.size() > 1 ? 24 : 0);
        int size = Math.min(heroHeight, layout.column() - 12);
        graphics.enableScissor(layout.leftText(), top, layout.leftText() + layout.column(), top + heroHeight);
        var poses = graphics.pose();
        poses.pushPose();
        poses.translate(layout.leftText() + (layout.column() - size) / 2F, top + (heroHeight - size) / 2F, 0);
        poses.scale(size / 16F, size / 16F, size / 16F);
        Lighting.setupFor3DItems();
        graphics.renderItem(items.get(0), 0, 0);
        graphics.flush();
        poses.popPose();
        graphics.disableScissor();
        if (items.size() > 1) {
            drawRow(graphics, -1, -1, top + height - 18, items.subList(1, items.size()));
        }
    }

    private List<ItemStack> entryItems() {
        return switch (page) {
            case PLACEMENT -> stacks(BlueprintItems.BLUEPRINT.get());
            case TOWER_CROSSBOW -> stacks(SiegeworksItems.TOWER_CROSSBOW_BOLT.get());
            case ARCBALLISTA -> stacks(SiegeworksItems.ARCBALLISTA_BOLT.get());
            case CULVERIN, SERPENTINE, MONS_MEG -> List.of(
                    new ItemStack(SiegeworksItems.BLACK_POWDER.get(), page == CULVERIN ? ArtilleryPowderCosts.CULVERIN :
                            page == SERPENTINE ? ArtilleryPowderCosts.SERPENTINE : ArtilleryPowderCosts.MONS_MEG),
                    new ItemStack(SiegeworksItems.RAMROD.get()),
                    new ItemStack((page == MONS_MEG ? SiegeworksItems.GIANT_CANNON_BALL : SiegeworksItems.CANNON_BALL).get()),
                    new ItemStack(Items.FLINT_AND_STEEL));
            case ONAGER, TREBUCHET -> stacks(Items.STONE, SiegeworksItems.GRAPESHOT.get(), SiegeworksItems.FIRE_PROJECTILE.get());
            case HWACHA -> stacks(SiegeworksItems.SO_SINGIJEON.get(), SiegeworksItems.JUNG_SINGIJEON.get(), Items.FLINT_AND_STEEL);
            case LADDER -> stacks(SiegeworksItems.SIEGE_LADDER_SPAWNER.get());
            case RAM, MANTLET -> stacks(Items.LEAD);
            case TOWER_CREW -> List.of(new ItemStack(Items.LEATHER, 64), new ItemStack(Items.SHEARS), new ItemStack(Items.LEAD));
            case POT_FILLING -> stacks(SiegeworksItems.CLAY_POT.get(), Items.CHARCOAL, Items.HONEYCOMB, Items.STRING, Items.FLINT_AND_STEEL);
            case FIREARM_LOAD, HAND_CANNON, ARQUEBUS -> List.of(
                    new ItemStack(page == HAND_CANNON ? SiegeworksItems.HAND_CANNON.get() : SiegeworksItems.ARQUEBUS.get()),
                    new ItemStack(SiegeworksItems.BLACK_POWDER.get()),
                    RoundShotItem.withCaliber(new ItemStack(SiegeworksItems.CANNON_BALL.get()),
                            FirearmActions.caliber((FirearmItem) (page == HAND_CANNON ? SiegeworksItems.HAND_CANNON : SiegeworksItems.ARQUEBUS).get())),
                    new ItemStack(SiegeworksItems.FIREARM_RAMROD.get()));
            case MATCH -> stacks(Items.STRING, Items.FLINT_AND_STEEL, SiegeworksItems.ARQUEBUS.get());
            case TOWING -> stacks(Items.LEAD, Items.SADDLE, Items.SHEARS);
            case WEATHER -> stacks(Items.HONEYCOMB, BlueprintItems.CONSTRUCTION_HAMMER.get());
            case CUSTOMIZATION -> stacks(Items.WHITE_BANNER, Items.SHEARS, Items.COPPER_INGOT);
            case MAINTENANCE -> stacks(BlueprintItems.CONSTRUCTION_HAMMER.get(), Items.WATER_BUCKET, Items.IRON_INGOT);
            case OWNERSHIP, CAPTURE -> stacks(SiegeworksItems.MANTLET_SPAWNER.get(), Items.WHITE_BANNER);
            case RECRUITS -> stacks(SiegeworksItems.TOWER_CROSSBOW_SPAWNER.get(), BlueprintItems.CONSTRUCTION_HAMMER.get(), BlueprintItems.BLUEPRINT.get());
            case SUPPLY -> stacks(Items.CHEST, SiegeworksItems.BLACK_POWDER.get(), SiegeworksItems.TOWER_CROSSBOW_BOLT.get());
            case COMPAT -> stacks(SiegeworksItems.FORKED_POLE.get(), SiegeworksItems.HAND_CANNON.get(), SiegeworksItems.ARQUEBUS.get());
            default -> List.of();
        };
    }

    private static List<ItemStack> stacks(net.minecraft.world.level.ItemLike... items) {
        return java.util.Arrays.stream(items).map(ItemStack::new).toList();
    }

    private int textHeight(Component text, int width) {
        return font.split(text, width).size() * (font.lineHeight + 1);
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        for (var row : font.split(text, width)) {
            graphics.drawString(font, row, x, y, color, false);
            y += font.lineHeight + 1;
        }
        return y;
    }

    private void drawNote(GuiGraphics graphics, Component note, int y) {
        int center = layout.leftText() + layout.column() / 2;
        for (var row : font.split(note, layout.column())) {
            graphics.drawString(font, row, center - font.width(row) / 2, y, ink, false);
            y += font.lineHeight + 1;
        }
    }

    private int drawRecipe(GuiGraphics graphics, int mouseX, int mouseY, int top, GuideRecipe grid) {
        int x = layout.leftText() + (layout.column() - 87) / 2;
        int y = drawWrapped(graphics, grid.result().getHoverName(), layout.leftText(), top, layout.column(), heading) + 3;
        for (int index = 0; index < grid.rows() * 3; index++) {
            ItemStack stack = index < grid.ingredients().size() ? grid.ingredients().get(index) : ItemStack.EMPTY;
            drawSlot(graphics, mouseX, mouseY, stack, x + index % 3 * 18, y + index / 3 * 18);
        }
        int outputY = y + (grid.rows() - 1) * 9;
        graphics.drawString(font, "→", x + 57, outputY + 5, link, false);
        drawSlot(graphics, mouseX, mouseY, grid.result(), x + 69, outputY);
        return y + grid.rows() * 18 + 8;
    }

    private void drawRow(GuiGraphics graphics, int mouseX, int mouseY, int y, List<ItemStack> stacks) {
        int x = layout.leftText() + (layout.column() - (stacks.size() * 21 - 3)) / 2;
        for (ItemStack stack : stacks) {
            drawItem(graphics, mouseX, mouseY, stack, x, y);
            x += 21;
        }
    }

    private void drawSlot(GuiGraphics graphics, int mouseX, int mouseY, ItemStack stack, int x, int y) {
        graphics.blit(CONTROLS, x, y, 90, 0, 18, 18, 108, 54);
        drawItem(graphics, mouseX, mouseY, stack, x, y);
    }

    private void drawItem(GuiGraphics graphics, int mouseX, int mouseY, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.enableScissor(x, y, x + 18, y + 18);
        Lighting.setupFor3DItems();
        graphics.renderItem(stack, x + 1, y + 1);
        graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        graphics.flush();
        graphics.disableScissor();
        if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
            hoveredItem = stack;
        }
    }

    private void drawArt(GuiGraphics graphics, ResourceLocation texture, int top, int height) {
        int size = Math.min(layout.column(), height);
        int x = layout.leftText() + (layout.column() - size) / 2;
        graphics.blit(texture, x, top + (height - size) / 2, 0, 0, size, size, size, size);
    }

    private List<Component> instructions(ManualEntry target) {
        Object[] bindings = {
                minecraft.options.keyUse.getTranslatedKeyMessage(),
                minecraft.options.keyJump.getTranslatedKeyMessage(),
                minecraft.options.keyShift.getTranslatedKeyMessage(),
                SiegeworksKeyMappings.FREE_LOOK.getTranslatedKeyMessage(),
                SiegeworksKeyMappings.BARREL_SIGHT.getTranslatedKeyMessage(),
                target == SERPENTINE ? ArtilleryPowderCosts.SERPENTINE :
                        target == MONS_MEG ? ArtilleryPowderCosts.MONS_MEG : ArtilleryPowderCosts.CULVERIN,
                target == HAND_CANNON || target == ARQUEBUS
                        ? FirearmActions.caliber((FirearmItem) (target == HAND_CANNON ? SiegeworksItems.HAND_CANNON : SiegeworksItems.ARQUEBUS).get())
                        : target == MONS_MEG ? 490 : AbstractFieldGunEntity.ROUND_SHOT_CALIBER_MM,
                me.mss1r.siegeworks.gameplay.firearm.FirearmLoad.SHOTS_PER_MATCH
        };
        return List.of(key(target.id + ".1", bindings), key(target.id + ".2", bindings), key(target.id + ".3", bindings));
    }

    private void paginate() {
        leaves.clear();
        entryFolios.clear();
        chapterFolios.clear();
        int folio = 2;
        for (Chapter group : Chapter.values()) {
            chapterFolios.put(group, folio);
            folio += 2;
            for (ManualEntry entry : group.entries()) {
                int textTop = layout.y + 32 + textHeight(key(entry.id), layout.column()) + 10;
                int available = Math.max(font.lineHeight, layout.footer() - 7 - textTop);
                var spreads = new java.util.ArrayList<List<FormattedCharSequence>>();
                var current = new java.util.ArrayList<FormattedCharSequence>();
                int used = 0;
                for (Component paragraph : instructions(entry)) {
                    var rows = font.split(paragraph, layout.column());
                    int paragraphHeight = rows.size() * font.lineHeight;
                    int gap = current.isEmpty() ? 0 : 4;
                    // Keep a paragraph together whenever it fits on a page by itself.
                    if (!current.isEmpty() && paragraphHeight <= available && used + gap + paragraphHeight > available) {
                        spreads.add(List.copyOf(current));
                        current.clear();
                        used = 0;
                    }
                    if (!current.isEmpty()) {
                        current.add(FormattedCharSequence.EMPTY);
                        used += 4;
                    }
                    for (FormattedCharSequence row : rows) {
                        if (used + font.lineHeight > available) {
                            while (!current.isEmpty() && current.get(current.size() - 1) == FormattedCharSequence.EMPTY)
                                current.remove(current.size() - 1);
                            if (!current.isEmpty()) spreads.add(List.copyOf(current));
                            current.clear();
                            used = 0;
                        }
                        current.add(row);
                        used += font.lineHeight;
                    }
                }
                if (!current.isEmpty()) spreads.add(List.copyOf(current));
                if (spreads.isEmpty()) spreads.add(List.of());
                leaves.put(entry, List.copyOf(spreads));
                entryFolios.put(entry, folio);
                folio += spreads.size() * 2;
            }
        }
    }

    private void renderInstructions(GuiGraphics graphics) {
        int y = rightTextTop();
        for (FormattedCharSequence row : leaves.get(page).get(leaf)) {
            if (row == FormattedCharSequence.EMPTY) {
                y += 4;
            } else {
                graphics.drawString(font, row, layout.rightText(), y, ink, false);
                y += font.lineHeight;
            }
        }
    }

    @Override
    //? if <1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double amount) {
    //?}
        if (amount == 0 || mouseX < layout.x || mouseX >= layout.x + layout.width
                || mouseY < layout.y || mouseY >= layout.y + layout.height) return false;
        return turnPage(amount > 0 ? -1 : 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21 {
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The manual paints its own background before drawing the pages.
    }
    //?}

    private static int opaque(int color) {
        return 0xFF000000 | color;
    }

    private static ResourceLocation art(String path) {
        return MinecraftVersionCompat.id("siegeworks", "textures/" + path);
    }

    private static Component key(String path, Object... args) {
        return Component.translatable("gui.siegeworks.manual." + path, args);
    }

    private record Layout(int x, int y, int width, int height) {
        int leftText() { return x + 22; }
        int rightText() { return x + width / 2 + 16; }
        int column() { return width / 2 - 39; }
        int textTop() { return y + 52; }
        int tools() { return y + height - 69; }
        int footer() { return y + height - 38; }
        int ribbonX() { return x + width - 46; }
    }

    private final class TextButton extends Button {
        private final int textOffset;

        private TextButton(int x, int y, int width, int height, int textOffset, Component title, OnPress action) {
            super(x, y, width, height, title, action, DEFAULT_NARRATION);
            this.textOffset = textOffset;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY() + textOffset;
            graphics.enableScissor(x, getY(), x + getWidth(), getY() + getHeight());
            for (var row : font.split(getMessage(), getWidth())) {
                graphics.drawString(font, row, x, y, link, false);
                if (isHoveredOrFocused()) {
                    graphics.hLine(x, x + font.width(row) - 1, y + font.lineHeight, opaque(link));
                }
                y += font.lineHeight + 1;
            }
            graphics.disableScissor();
        }
    }

    private final class TurnPageButton extends PageButton {
        private final boolean forward;
        private TurnPageButton(int x, boolean forward) {
            super(x, layout.footer() + 2, forward, button -> turnPage(forward ? 1 : -1), true);
            this.forward = forward;
            setMessage(key(forward ? "next" : "previous"));
            setTooltip(Tooltip.create(key(forward ? "next_page" : "previous_page")));
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(NAVIGATION, getX(), getY(), forward ? 23 : 0,
                    isHoveredOrFocused() ? 13 : 0, 23, 13, 46, 26);
        }
    }

}
