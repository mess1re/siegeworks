package me.mss1r.siegeworks.client.maintenance;

import me.mss1r.siegeworks.Siegeworks;
import me.mss1r.siegeworks.network.MaintenanceActionC2SPayload;
import me.mss1r.siegeworks.network.OpenMaintenanceS2CPayload;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import me.mss1r.siegeworks.network.SiegeworksNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.IOException;

public class SiegeMaintenanceScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 190;
    private static final int TEXT_COLOR = 0x404040;
    private static final int HEADING_COLOR = 0x303030;
    private static final int PROGRESS_COLOR = 0x9A4D00;
    private static final ResourceLocation PANEL_TEXTURE = MinecraftVersionCompat.id(
            Siegeworks.MOD_ID,
            "textures/gui/maintenance.png"
    );

    private static final ResourceLocation TEXT_COLORS = MinecraftVersionCompat.id(
            Siegeworks.MOD_ID, "gui/maintenance.properties");
    private int textColor = TEXT_COLOR;
    private int headingColor = HEADING_COLOR;
    private int progressColor = PROGRESS_COLOR;

    private final OpenMaintenanceS2CPayload payload;

    public SiegeMaintenanceScreen(OpenMaintenanceS2CPayload payload) {
        super(Component.translatable("gui.siegeworks.maintenance.title", payload.title()));
        this.payload = payload;
    }

    @Override
    protected void init() {
        loadTextColors();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int buttonY = top + PANEL_HEIGHT - 48;

        addRenderableWidget(Button.builder(Component.translatable("gui.siegeworks.maintenance.repair"), button ->
                        sendAction(MaintenanceActionC2SPayload.ACTION_REPAIR))
                .bounds(left + 12, buttonY, 78, 20)
                .build());

        Component dismantleLabel = payload.dismantling()
                ? Component.translatable("gui.siegeworks.maintenance.cancel_dismantle")
                : Component.translatable("gui.siegeworks.maintenance.start_dismantle");
        int dismantleAction = payload.dismantling()
                ? MaintenanceActionC2SPayload.ACTION_CANCEL_DISMANTLE
                : MaintenanceActionC2SPayload.ACTION_START_DISMANTLE;

        addRenderableWidget(Button.builder(dismantleLabel, button -> sendAction(dismantleAction))
                .bounds(left + 98, buttonY, 116, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + 222, buttonY, 46, 20)
                .build());
    }

    private void loadTextColors() {
        // Optional RP palette; preserve the native grey panel without a skin.
        textColor = TEXT_COLOR;
        headingColor = HEADING_COLOR;
        progressColor = PROGRESS_COLOR;
        var resource = minecraft.getResourceManager().getResource(TEXT_COLORS);
        if (resource.isEmpty()) return;
        Properties colors = new Properties();
        try (var stream = resource.get().open()) {
            colors.load(stream);
            textColor = readColor(colors, "text", TEXT_COLOR);
            headingColor = readColor(colors, "heading", HEADING_COLOR);
            progressColor = readColor(colors, "progress", PROGRESS_COLOR);
        } catch (IOException | IllegalArgumentException ignored) {
            // A malformed optional skin must not prevent opening the screen.
        }
    }

    private static int readColor(Properties colors, String key, int fallback) {
        String value = colors.getProperty(key, "").trim();
        if (value.startsWith("#")) value = value.substring(1);
        return value.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(value, 16) : fallback;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0x66000000);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        guiGraphics.blit(
                PANEL_TEXTURE,
                left,
                top,
                0,
                0,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                PANEL_WIDTH,
                PANEL_HEIGHT
        );

        int titleX = (width - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, top + 10, headingColor, false);

        int healthPercent = payload.maxHealth() <= 0 ? 0 : Math.round(payload.health() * 100.0F / payload.maxHealth());
        Component healthLine = Component.translatable(
                "gui.siegeworks.maintenance.health",
                payload.health(),
                payload.maxHealth(),
                healthPercent
        );
        guiGraphics.drawString(font, healthLine, left + 14, top + 30, textColor, false);

        if (payload.dismantling()) {
            Component progress = Component.translatable(
                    "gui.siegeworks.maintenance.dismantle_progress",
                    payload.dismantleProgress(),
                    payload.dismantleRequired()
            );
            guiGraphics.drawString(font, progress, left + 14, top + 44, progressColor, false);
        }

        drawSection(guiGraphics, left + 14, top + 62,
                Component.translatable("gui.siegeworks.maintenance.repair_cost"),
                repairLines());
        drawSection(guiGraphics, left + 152, top + 62,
                Component.translatable("gui.siegeworks.maintenance.dismantle_refund"),
                refundLines());

        for (Renderable renderable : renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendAction(int action) {
        SiegeworksNetworking.sendToServer(new MaintenanceActionC2SPayload(payload.entityId(), action));
    }

    private List<Component> repairLines() {
        if (!payload.hasRecipe()) {
            return List.of(Component.translatable("gui.siegeworks.maintenance.no_recipe"));
        }
        if (payload.health() >= payload.maxHealth()) {
            return List.of(Component.translatable("gui.siegeworks.maintenance.no_repair_needed"));
        }
        return splitLines(payload.repairCost());
    }

    private List<Component> refundLines() {
        if (!payload.hasRecipe()) {
            return List.of(Component.translatable("gui.siegeworks.maintenance.no_recipe"));
        }
        return splitLines(payload.dismantleRefund());
    }

    private List<Component> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return List.of(Component.translatable("gui.siegeworks.maintenance.none"));
        }

        List<Component> lines = new ArrayList<>();
        for (String line : text.split("\\n")) {
            if (!line.isBlank()) {
                lines.add(Component.literal(line));
            }
        }
        return lines.isEmpty() ? List.of(Component.translatable("gui.siegeworks.maintenance.none")) : lines;
    }

    private void drawSection(GuiGraphics guiGraphics, int x, int y, Component heading, List<Component> lines) {
        guiGraphics.drawString(font, heading, x, y, headingColor, false);
        int maxLines = 7;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + 14 + i * 10, textColor, false);
        }
        if (lines.size() > maxLines) {
            guiGraphics.drawString(font, Component.literal("..."), x, y + 14 + maxLines * 10, textColor, false);
        }
    }
}
