package com.aw2towns.client.gui;

import com.aw2towns.economy.ResourceType;
import com.aw2towns.economy.TownState;
import com.aw2towns.economy.WorkstationType;
import com.aw2towns.screen.TownManagerScreenHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class TownManagerScreen extends HandledScreen<TownManagerScreenHandler> {

    private static final int PANEL = 0xFF252B2F;
    private static final int PANEL_DARK = 0xFF1B2024;
    private static final int BORDER = 0xFF6D7C72;
    private static final int TEXT = 0xFFE8E3D5;
    private static final int MUTED = 0xFFB8B0A0;
    private static final int GOOD = 0xFF79D17B;
    private static final int WARN = 0xFFFFC76A;
    private static final int BAD = 0xFFFF6B6B;
    private static final int ROW_HEIGHT = 14;
    private static final int LIST_X = 10;
    private static final int LIST_Y = 74;
    private static final int LIST_W = 272;
    private static final int LIST_H = 160;
    private static final int WORKER_ROW_HEIGHT = 24;
    private static final int STATUS_ROW_HEIGHT = 24;

    private final List<OverviewButton> overviewButtons = new ArrayList<>();
    private Tab currentTab = Tab.OVERVIEW;
    private int workersScroll;
    private int overviewStatusScroll;
    private int productionScroll;

    public TownManagerScreen(TownManagerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 292;
        backgroundHeight = 252;
        titleX = 10;
        titleY = 8;
        playerInventoryTitleY = 999;
    }

    @Override
    protected void init() {
        super.init();
        int tabX = x + 10;
        for (Tab tab : Tab.values()) {
            addDrawableChild(ButtonWidget.builder(tab.label(), button -> setTab(tab))
                    .dimensions(tabX, y + 26, tab.width, 18)
                    .build());
            tabX += tab.width + 4;
        }

        addWorkerButtons(WorkstationType.FARM, TownManagerScreenHandler.BUTTON_FARM_MINUS, TownManagerScreenHandler.BUTTON_FARM_PLUS,
                TownManagerScreenHandler.BUTTON_FARM_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_FARM_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.BAKER, TownManagerScreenHandler.BUTTON_BAKER_MINUS, TownManagerScreenHandler.BUTTON_BAKER_PLUS,
                TownManagerScreenHandler.BUTTON_BAKER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_BAKER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.MINE, TownManagerScreenHandler.BUTTON_MINE_MINUS, TownManagerScreenHandler.BUTTON_MINE_PLUS,
                TownManagerScreenHandler.BUTTON_MINE_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_MINE_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.LUMBER_MILL, TownManagerScreenHandler.BUTTON_LUMBER_MINUS, TownManagerScreenHandler.BUTTON_LUMBER_PLUS,
                TownManagerScreenHandler.BUTTON_LUMBER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_LUMBER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.BLACKSMITH, TownManagerScreenHandler.BUTTON_BLACKSMITH_MINUS, TownManagerScreenHandler.BUTTON_BLACKSMITH_PLUS,
                TownManagerScreenHandler.BUTTON_BLACKSMITH_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_BLACKSMITH_PRIORITY_PLUS);
        updateOverviewButtonVisibility();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawListTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int delta = verticalAmount > 0 ? -1 : 1;
        if (currentTab == Tab.OVERVIEW) {
            if (isMouseOverList(mouseX, mouseY)) {
                overviewStatusScroll = clampScroll(overviewStatusScroll + delta, WorkstationType.values().length,
                        visibleRows(STATUS_ROW_HEIGHT));
                updateOverviewButtonVisibility();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (currentTab == Tab.WORKERS) {
            if (isMouseOverList(mouseX, mouseY)) {
                workersScroll = clampScroll(workersScroll + delta, WorkstationType.values().length,
                        visibleRows(WORKER_ROW_HEIGHT));
                updateOverviewButtonVisibility();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (!isMouseOverList(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        switch (currentTab) {
            case PRODUCTION -> productionScroll = clampScroll(productionScroll + delta);
            case OVERVIEW, WORKERS -> {}
        }
        return true;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, PANEL);
        context.drawBorder(x, y, backgroundWidth, backgroundHeight, BORDER);
        context.fill(x + 8, y + 48, x + backgroundWidth - 8, y + 66, PANEL_DARK);
        context.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, PANEL_DARK);
        context.drawBorder(x + LIST_X, y + LIST_Y, LIST_W, LIST_H, BORDER);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.prototype_town"),
                10, 52, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.workers",
                handler.totalWorkers(), handler.unassignedWorkers()), 126, 52, MUTED, false);

        switch (currentTab) {
            case OVERVIEW -> drawOverview(context);
            case WORKERS -> drawWorkerAssignments(context);
            case PRODUCTION -> drawProductionLedger(context);
        }
    }

    private void addWorkerButtons(WorkstationType type, int workerMinus, int workerPlus, int priorityMinus, int priorityPlus) {
        addOverviewButton(workerMinus, "-", type, OverviewButtonKind.WORKER_MINUS);
        addOverviewButton(workerPlus, "+", type, OverviewButtonKind.WORKER_PLUS);
        addOverviewButton(priorityMinus, "-", type, OverviewButtonKind.PRIORITY_MINUS);
        addOverviewButton(priorityPlus, "+", type, OverviewButtonKind.PRIORITY_PLUS);
    }

    private void addOverviewButton(int buttonId, String label, WorkstationType type, OverviewButtonKind kind) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(label), ignored -> click(buttonId))
                .dimensions(x, y, 20, 16)
                .build();
        overviewButtons.add(new OverviewButton(addDrawableChild(button), type, kind));
    }

    private void setTab(Tab tab) {
        currentTab = tab;
        updateOverviewButtonVisibility();
    }

    private void updateOverviewButtonVisibility() {
        for (OverviewButton overviewButton : overviewButtons) {
            ButtonWidget button = overviewButton.button();
            boolean rowVisible = positionOverviewButton(overviewButton);
            button.visible = rowVisible;
            button.active = rowVisible;
        }
    }

    private boolean positionOverviewButton(OverviewButton overviewButton) {
        boolean workerButton = isWorkerButton(overviewButton);
        if (workerButton && currentTab != Tab.WORKERS || !workerButton && currentTab != Tab.OVERVIEW) {
            return false;
        }
        int scroll = workerButton ? workersScroll : overviewStatusScroll;
        int rowHeight = workerButton ? WORKER_ROW_HEIGHT : STATUS_ROW_HEIGHT;
        int row = overviewButton.type().ordinal() - scroll;
        if (row < 0 || row >= visibleRows(rowHeight)) {
            return false;
        }

        int buttonX = switch (overviewButton.kind()) {
            case WORKER_MINUS, PRIORITY_MINUS -> 216;
            case WORKER_PLUS, PRIORITY_PLUS -> 240;
        };
        overviewButton.button().setPosition(x + buttonX, y + LIST_Y + 18 + row * rowHeight);
        return true;
    }

    private boolean isWorkerButton(OverviewButton overviewButton) {
        return overviewButton.kind() == OverviewButtonKind.WORKER_MINUS
                || overviewButton.kind() == OverviewButtonKind.WORKER_PLUS;
    }

    private void click(int buttonId) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, buttonId);
        }
    }

    private void drawOverview(DrawContext context) {
        drawStatusRows(context);
    }

    private void drawWorkerAssignments(DrawContext context) {
        drawOverviewPanelHeader(context, Text.translatable("container.aw2towns.town_manager.work_assignments"),
                workersScroll, visibleRows(WORKER_ROW_HEIGHT));
        int row = 0;
        for (int i = workersScroll; i < WorkstationType.values().length
                && row < visibleRows(WORKER_ROW_HEIGHT); i++, row++) {
            WorkstationType type = WorkstationType.values()[i];
            int rowY = LIST_Y + 18 + row * WORKER_ROW_HEIGHT;
            context.drawText(textRenderer, type.displayName(), LIST_X + 6, rowY + 4, workstationColor(type), false);
            context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.worker_count", handler.workers(type)),
                    LIST_X + 112, rowY + 4, MUTED, false);
        }
    }

    private void drawStatusRows(DrawContext context) {
        drawOverviewPanelHeader(context, Text.translatable("container.aw2towns.town_manager.priorities_status"),
                overviewStatusScroll, visibleRows(STATUS_ROW_HEIGHT));
        int row = 0;
        for (int i = overviewStatusScroll; i < WorkstationType.values().length
                && row < visibleRows(STATUS_ROW_HEIGHT); i++, row++) {
            WorkstationType type = WorkstationType.values()[i];
            drawStatusRow(context, type, LIST_Y + 18 + row * STATUS_ROW_HEIGHT);
        }
    }

    private void drawStatusRow(DrawContext context, WorkstationType type, int rowY) {
        int color = workstationColor(type);
        context.drawText(textRenderer, type.displayName(), LIST_X + 6, rowY + 4, color, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.priority", handler.priority(type)),
                LIST_X + 88, rowY + 4, MUTED, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.productivity", handler.productivity(type)),
                LIST_X + 150, rowY + 4, color, false);
        String shortages = shortageText(handler.shortageFlags(type));
        if (!shortages.isEmpty()) {
            context.drawText(textRenderer, shortages, LIST_X + 6, rowY + 14, color, false);
        }
    }

    private void drawOverviewPanelHeader(DrawContext context, Text header, int scroll, int visibleRows) {
        context.drawText(textRenderer, header, LIST_X + 6, LIST_Y + 6, TEXT, false);
        drawScrollBar(context, LIST_Y, LIST_H, scroll, WorkstationType.values().length, visibleRows);
    }

    private void drawProductionLedger(DrawContext context) {
        drawProductionHeader(context);
        int row = 0;
        for (int i = productionScroll; i < ResourceType.values().length && row < visibleRows(); i++, row++) {
            ResourceType resource = ResourceType.values()[i];
            int yPos = LIST_Y + 18 + row * ROW_HEIGHT;
            context.drawText(textRenderer, resource.displayName(), LIST_X + 6, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.productionPerDay(resource)), LIST_X + 104, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.consumptionPerDay(resource)), LIST_X + 150, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.resource(resource)), LIST_X + 200, yPos, resourceColor(resource), false);
        }
    }

    private void drawProductionHeader(DrawContext context) {
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.product"), LIST_X + 6, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.made"), LIST_X + 104, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.used"), LIST_X + 150, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.stored"), LIST_X + 200, LIST_Y + 6, TEXT, false);
    }

    private void drawListTooltip(DrawContext context, int mouseX, int mouseY) {
        if (currentTab != Tab.PRODUCTION || !isMouseOverList(mouseX, mouseY)) {
            return;
        }
        int row = (mouseY - (y + LIST_Y + 18)) / ROW_HEIGHT;
        if (row < 0 || row >= visibleRows()) {
            return;
        }
        int index = productionScroll + row;
        if (index < 0 || index >= ResourceType.values().length) {
            return;
        }
        ResourceType resource = ResourceType.values()[index];
        Text tooltip = Text.translatable("container.aw2towns.town_manager.flow_tooltip", producers(resource), consumers(resource));
        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    private int workstationColor(WorkstationType type) {
        int productivity = handler.productivity(type);
        if (handler.workers(type) <= 0 || productivity <= TownState.STATUS_BLOCKED_MAX) {
            return BAD;
        }
        if (productivity <= TownState.STATUS_PARTIAL_MAX) {
            return WARN;
        }
        return GOOD;
    }

    private int resourceColor(ResourceType resource) {
        if (handler.productionPerDay(resource) >= handler.consumptionPerDay(resource)) {
            return GOOD;
        }
        return handler.hasLargeEnoughStockpile(resource) ? WARN : BAD;
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W
                && mouseY >= y + LIST_Y && mouseY < y + LIST_Y + LIST_H;
    }

    private boolean isMouseOverPanel(double mouseX, double mouseY, int panelY, int panelH) {
        return mouseX >= x + LIST_X && mouseX < x + LIST_X + LIST_W
                && mouseY >= y + panelY && mouseY < y + panelY + panelH;
    }

    private int visibleRows() {
        return (LIST_H - 20) / ROW_HEIGHT;
    }

    private int visibleRows(int rowHeight) {
        return (LIST_H - 20) / rowHeight;
    }

    private int clampScroll(int scroll) {
        return clampScroll(scroll, ResourceType.values().length, visibleRows());
    }

    private int clampScroll(int scroll, int rows, int visibleRows) {
        return Math.max(0, Math.min(Math.max(0, rows - visibleRows), scroll));
    }

    private void drawScrollBar(DrawContext context, int panelY, int panelH, int scroll, int rows, int visibleRows) {
        if (rows <= visibleRows) {
            return;
        }
        int trackX = LIST_X + LIST_W - 7;
        int trackY = panelY + 16;
        int trackH = panelH - 22;
        int thumbH = Math.max(8, trackH * visibleRows / rows);
        int maxScroll = rows - visibleRows;
        int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll;
        context.fill(trackX, trackY, trackX + 3, trackY + trackH, PANEL);
        context.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, MUTED);
    }

    private static String shortageText(int flags) {
        if (flags == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Needs ");
        for (ResourceType resource : ResourceType.values()) {
            if ((flags & (1 << resource.ordinal())) != 0) {
                if (!builder.toString().endsWith(" ")) {
                    builder.append(", ");
                }
                builder.append(resource.id().replace('_', ' '));
            }
        }
        return builder.toString();
    }

    private static String producers(ResourceType resource) {
        return switch (resource) {
            case WHEAT -> "Farm";
            case BREAD -> "Baker";
            case IRON -> "Mine";
            case OAK_PLANKS -> "Lumber Mill";
            case PICKAXE, AXE, HOE, SWORD -> "Blacksmith";
        };
    }

    private static String consumers(ResourceType resource) {
        return switch (resource) {
            case WHEAT -> "Baker";
            case BREAD -> "All workers";
            case IRON, OAK_PLANKS -> "Blacksmith";
            case PICKAXE -> "Mine";
            case AXE -> "Lumber Mill";
            case HOE -> "Farm";
            case SWORD -> "Blacksmith";
        };
    }

    private enum Tab {
        OVERVIEW("Overview", 56),
        WORKERS("Workers", 52),
        PRODUCTION("Production", 70);

        private final String label;
        private final int width;

        Tab(String label, int width) {
            this.label = label;
            this.width = width;
        }

        private Text label() {
            return Text.literal(label);
        }
    }

    private enum OverviewButtonKind {
        WORKER_MINUS,
        WORKER_PLUS,
        PRIORITY_MINUS,
        PRIORITY_PLUS
    }

    private record OverviewButton(ButtonWidget button, WorkstationType type, OverviewButtonKind kind) {}
}
