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
    private static final int OVERVIEW_WORKERS_Y = 74;
    private static final int OVERVIEW_WORKERS_H = 72;
    private static final int OVERVIEW_STATUS_Y = 152;
    private static final int OVERVIEW_STATUS_H = 84;
    private static final int OVERVIEW_WORKER_ROW_HEIGHT = 18;
    private static final int OVERVIEW_STATUS_ROW_HEIGHT = 24;

    private final List<OverviewButton> overviewButtons = new ArrayList<>();
    private Tab currentTab = Tab.OVERVIEW;
    private int overviewWorkersScroll;
    private int overviewStatusScroll;
    private int storageScroll;
    private int productionScroll;
    private int consumptionScroll;

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
            if (isMouseOverPanel(mouseX, mouseY, OVERVIEW_WORKERS_Y, OVERVIEW_WORKERS_H)) {
                overviewWorkersScroll = clampScroll(overviewWorkersScroll + delta, WorkstationType.values().length,
                        overviewRows(OVERVIEW_WORKERS_H, OVERVIEW_WORKER_ROW_HEIGHT));
                updateOverviewButtonVisibility();
                return true;
            }
            if (isMouseOverPanel(mouseX, mouseY, OVERVIEW_STATUS_Y, OVERVIEW_STATUS_H)) {
                overviewStatusScroll = clampScroll(overviewStatusScroll + delta, WorkstationType.values().length,
                        overviewRows(OVERVIEW_STATUS_H, OVERVIEW_STATUS_ROW_HEIGHT));
                updateOverviewButtonVisibility();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (!isMouseOverList(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        switch (currentTab) {
            case STORAGE -> storageScroll = clampScroll(storageScroll + delta);
            case PRODUCTION -> productionScroll = clampScroll(productionScroll + delta);
            case CONSUMPTION -> consumptionScroll = clampScroll(consumptionScroll + delta);
            case OVERVIEW -> {}
        }
        return true;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, PANEL);
        context.drawBorder(x, y, backgroundWidth, backgroundHeight, BORDER);
        context.fill(x + 8, y + 48, x + backgroundWidth - 8, y + 66, PANEL_DARK);
        if (currentTab == Tab.OVERVIEW) {
            drawOverviewPanelBackground(context, OVERVIEW_WORKERS_Y, OVERVIEW_WORKERS_H);
            drawOverviewPanelBackground(context, OVERVIEW_STATUS_Y, OVERVIEW_STATUS_H);
        } else {
            context.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, PANEL_DARK);
        }
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
            case STORAGE -> drawStorage(context);
            case PRODUCTION -> drawFlowList(context, true);
            case CONSUMPTION -> drawFlowList(context, false);
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
        boolean visible = currentTab == Tab.OVERVIEW;
        for (OverviewButton overviewButton : overviewButtons) {
            ButtonWidget button = overviewButton.button();
            boolean rowVisible = visible && positionOverviewButton(overviewButton);
            button.visible = rowVisible;
            button.active = rowVisible;
        }
    }

    private boolean positionOverviewButton(OverviewButton overviewButton) {
        boolean workerButton = overviewButton.kind() == OverviewButtonKind.WORKER_MINUS
                || overviewButton.kind() == OverviewButtonKind.WORKER_PLUS;
        int scroll = workerButton ? overviewWorkersScroll : overviewStatusScroll;
        int panelY = workerButton ? OVERVIEW_WORKERS_Y : OVERVIEW_STATUS_Y;
        int panelH = workerButton ? OVERVIEW_WORKERS_H : OVERVIEW_STATUS_H;
        int rowHeight = workerButton ? OVERVIEW_WORKER_ROW_HEIGHT : OVERVIEW_STATUS_ROW_HEIGHT;
        int row = overviewButton.type().ordinal() - scroll;
        if (row < 0 || row >= overviewRows(panelH, rowHeight)) {
            return false;
        }

        int buttonX = switch (overviewButton.kind()) {
            case WORKER_MINUS, PRIORITY_MINUS -> 216;
            case WORKER_PLUS, PRIORITY_PLUS -> 240;
        };
        overviewButton.button().setPosition(x + buttonX, y + panelY + 18 + row * rowHeight);
        return true;
    }

    private void click(int buttonId) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, buttonId);
        }
    }

    private void drawOverview(DrawContext context) {
        drawWorkerAssignments(context);
        drawStatusRows(context);
    }

    private void drawWorkerAssignments(DrawContext context) {
        drawOverviewPanelHeader(context, Text.translatable("container.aw2towns.town_manager.work_assignments"),
                OVERVIEW_WORKERS_Y, OVERVIEW_WORKERS_H, overviewWorkersScroll,
                overviewRows(OVERVIEW_WORKERS_H, OVERVIEW_WORKER_ROW_HEIGHT));
        int row = 0;
        for (int i = overviewWorkersScroll; i < WorkstationType.values().length
                && row < overviewRows(OVERVIEW_WORKERS_H, OVERVIEW_WORKER_ROW_HEIGHT); i++, row++) {
            WorkstationType type = WorkstationType.values()[i];
            int rowY = OVERVIEW_WORKERS_Y + 18 + row * OVERVIEW_WORKER_ROW_HEIGHT;
            context.drawText(textRenderer, type.displayName(), LIST_X + 6, rowY + 4, workstationColor(type), false);
            context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.worker_count", handler.workers(type)),
                    LIST_X + 112, rowY + 4, MUTED, false);
        }
    }

    private void drawStatusRows(DrawContext context) {
        drawOverviewPanelHeader(context, Text.translatable("container.aw2towns.town_manager.priorities_status"),
                OVERVIEW_STATUS_Y, OVERVIEW_STATUS_H, overviewStatusScroll,
                overviewRows(OVERVIEW_STATUS_H, OVERVIEW_STATUS_ROW_HEIGHT));
        int row = 0;
        for (int i = overviewStatusScroll; i < WorkstationType.values().length
                && row < overviewRows(OVERVIEW_STATUS_H, OVERVIEW_STATUS_ROW_HEIGHT); i++, row++) {
            WorkstationType type = WorkstationType.values()[i];
            drawStatusRow(context, type, OVERVIEW_STATUS_Y + 18 + row * OVERVIEW_STATUS_ROW_HEIGHT);
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

    private void drawOverviewPanelBackground(DrawContext context, int panelY, int panelH) {
        context.fill(x + LIST_X, y + panelY, x + LIST_X + LIST_W, y + panelY + panelH, PANEL_DARK);
        context.drawBorder(x + LIST_X, y + panelY, LIST_W, panelH, BORDER);
    }

    private void drawOverviewPanelHeader(DrawContext context, Text header, int panelY, int panelH, int scroll, int visibleRows) {
        context.drawText(textRenderer, header, LIST_X + 6, panelY + 6, TEXT, false);
        drawScrollBar(context, panelY, panelH, scroll, WorkstationType.values().length, visibleRows);
    }

    private void drawStorage(DrawContext context) {
        drawListHeader(context, Text.translatable("container.aw2towns.town_manager.storage_header"),
                Text.translatable("container.aw2towns.town_manager.count"));
        int row = 0;
        for (int i = storageScroll; i < ResourceType.values().length && row < visibleRows(); i++, row++) {
            ResourceType resource = ResourceType.values()[i];
            int yPos = LIST_Y + 18 + row * ROW_HEIGHT;
            context.drawText(textRenderer, resource.displayName(), LIST_X + 6, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.resource(resource)), LIST_X + 184, yPos, resourceColor(resource), false);
        }
    }

    private void drawFlowList(DrawContext context, boolean production) {
        drawListHeader(context,
                Text.translatable(production ? "container.aw2towns.town_manager.production_header" : "container.aw2towns.town_manager.consumption_header"),
                Text.translatable("container.aw2towns.town_manager.per_day"));
        int scroll = production ? productionScroll : consumptionScroll;
        int row = 0;
        for (int i = scroll; i < ResourceType.values().length && row < visibleRows(); i++, row++) {
            ResourceType resource = ResourceType.values()[i];
            int value = production ? handler.productionPerDay(resource) : handler.consumptionPerDay(resource);
            int yPos = LIST_Y + 18 + row * ROW_HEIGHT;
            context.drawText(textRenderer, resource.displayName(), LIST_X + 6, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(value), LIST_X + 184, yPos, resourceColor(resource), false);
        }
    }

    private void drawListHeader(DrawContext context, Text title, Text valueHeader) {
        context.drawText(textRenderer, title, LIST_X + 6, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, valueHeader, LIST_X + 184, LIST_Y + 6, TEXT, false);
    }

    private void drawListTooltip(DrawContext context, int mouseX, int mouseY) {
        if (currentTab != Tab.PRODUCTION && currentTab != Tab.CONSUMPTION || !isMouseOverList(mouseX, mouseY)) {
            return;
        }
        int row = (mouseY - (y + LIST_Y + 18)) / ROW_HEIGHT;
        if (row < 0 || row >= visibleRows()) {
            return;
        }
        int scroll = currentTab == Tab.PRODUCTION ? productionScroll : consumptionScroll;
        int index = scroll + row;
        if (index < 0 || index >= ResourceType.values().length) {
            return;
        }
        ResourceType resource = ResourceType.values()[index];
        Text tooltip = currentTab == Tab.PRODUCTION
                ? Text.translatable("container.aw2towns.town_manager.produced_by", producers(resource))
                : Text.translatable("container.aw2towns.town_manager.consumed_by", consumers(resource));
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

    private int clampScroll(int scroll) {
        return clampScroll(scroll, ResourceType.values().length, visibleRows());
    }

    private int clampScroll(int scroll, int rows, int visibleRows) {
        return Math.max(0, Math.min(Math.max(0, rows - visibleRows), scroll));
    }

    private int overviewRows(int panelH, int rowHeight) {
        return (panelH - 20) / rowHeight;
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
            case IRON -> "Mine";
            case OAK_PLANKS -> "Lumber Mill";
            case PICKAXE, AXE, HOE, SWORD -> "Blacksmith";
        };
    }

    private static String consumers(ResourceType resource) {
        return switch (resource) {
            case WHEAT -> "Farmers, miners, lumberjacks, blacksmiths";
            case IRON, OAK_PLANKS -> "Blacksmith";
            case PICKAXE -> "Mine";
            case AXE -> "Lumber Mill";
            case HOE -> "Farm";
            case SWORD -> "Blacksmith";
        };
    }

    private enum Tab {
        OVERVIEW("Overview", 64),
        PRODUCTION("Production", 70),
        CONSUMPTION("Use", 42),
        STORAGE("Storage", 60);

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
