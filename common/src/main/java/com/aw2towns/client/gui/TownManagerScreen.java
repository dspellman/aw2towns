package com.aw2towns.client.gui;

import com.aw2towns.AW2Towns;
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
import net.minecraft.util.Identifier;

public class TownManagerScreen extends HandledScreen<TownManagerScreenHandler> {

    private static final int PANEL = 0xFF252B2F;
    private static final int PANEL_DARK = 0xFF1B2024;
    private static final int BORDER = 0xFF6D7C72;
    private static final int TEXT = 0xFFE8E3D5;
    private static final int MUTED = 0xFFB8B0A0;
    private static final int GOOD = 0xFF79D17B;
    private static final int WARN = 0xFFFFC76A;
    private static final int BAD = 0xFFFF6B6B;
    private static final int SLOT = 0xFF8C9189;
    private static final int ROW_HEIGHT = 14;
    private static final int LIST_X = 10;
    private static final int LIST_Y = 74;
    private static final int LIST_W = 272;
    private static final int LIST_H = 160;
    private static final int WORKER_ROW_HEIGHT = 24;
    private static final int STATUS_ROW_HEIGHT = 24;
    private static final int WORKER_ICON_SIZE = 12;
    private static final int WORKER_TARGET_RADIUS = 8;
    private static final int WORKER_HEADER_X = 126;
    private static final int WORKER_HEADER_Y = 48;
    private static final int WORKER_HEADER_W = 88;
    private static final int WORKER_HEADER_H = 18;
    private static final int WORKER_SLOT_X = 126;
    private static final int WORKER_SLOT_SPACING = 18;
    private static final int MODE_X = 84;
    private static final int MODE_Y = 6;
    private static final int MODE_W = 56;
    private static final int MODE_H = 16;
    private static final int CYCLE_MINUS_X = 150;
    private static final int CYCLE_VALUE_X = 170;
    private static final int CYCLE_PLUS_X = 198;
    private static final int CYCLE_BUTTON_Y = 8;
    private static final int CYCLE_BUTTON_SIZE = 14;
    private static final int RESET_X = 224;
    private static final int RESET_Y = 6;
    private static final int RESET_W = 56;
    private static final int RESET_H = 16;
    private static final int GOAL_MODE_ICON_X = 78;
    private static final int GOAL_MODE_ICON_SIZE = 10;
    private static final Identifier WORKER_ICON = AW2Towns.id("textures/gui/worker.png");

    private final List<OverviewButton> overviewButtons = new ArrayList<>();
    private final List<WorkerIcon> workerIcons = new ArrayList<>();
    private final List<SlotTarget> vacantSlots = new ArrayList<>();
    private final List<RowTarget> workerRows = new ArrayList<>();
    private Tab currentTab = Tab.OVERVIEW;
    private int workersScroll;
    private int overviewStatusScroll;
    private int productionScroll;
    private DraggedWorker draggedWorker;
    private ButtonWidget assignmentModeButton;

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
        assignmentModeButton = addDrawableChild(ButtonWidget.builder(assignmentModeLabel(),
                        button -> click(TownManagerScreenHandler.BUTTON_ASSIGNMENT_MODE))
                .dimensions(x + MODE_X, y + MODE_Y, MODE_W, MODE_H)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-"),
                        button -> click(TownManagerScreenHandler.BUTTON_CYCLE_MINUS))
                .dimensions(x + CYCLE_MINUS_X, y + CYCLE_BUTTON_Y, CYCLE_BUTTON_SIZE, CYCLE_BUTTON_SIZE)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"),
                        button -> click(TownManagerScreenHandler.BUTTON_CYCLE_PLUS))
                .dimensions(x + CYCLE_PLUS_X, y + CYCLE_BUTTON_Y, CYCLE_BUTTON_SIZE, CYCLE_BUTTON_SIZE)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("container.aw2towns.town_manager.reset_bootstrap"),
                        button -> click(TownManagerScreenHandler.BUTTON_RESET_BOOTSTRAP))
                .dimensions(x + RESET_X, y + RESET_Y, RESET_W, RESET_H)
                .build());

        addWorkerButtons(WorkstationType.FARM, TownManagerScreenHandler.BUTTON_FARM_MINUS, TownManagerScreenHandler.BUTTON_FARM_PLUS,
                TownManagerScreenHandler.BUTTON_FARM_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_FARM_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.BAKER, TownManagerScreenHandler.BUTTON_BAKER_MINUS, TownManagerScreenHandler.BUTTON_BAKER_PLUS,
                TownManagerScreenHandler.BUTTON_BAKER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_BAKER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.MINE, TownManagerScreenHandler.BUTTON_MINE_MINUS, TownManagerScreenHandler.BUTTON_MINE_PLUS,
                TownManagerScreenHandler.BUTTON_MINE_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_MINE_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.LUMBER_MILL, TownManagerScreenHandler.BUTTON_LUMBER_MINUS, TownManagerScreenHandler.BUTTON_LUMBER_PLUS,
                TownManagerScreenHandler.BUTTON_LUMBER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_LUMBER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.CARPENTER, TownManagerScreenHandler.BUTTON_CARPENTER_MINUS, TownManagerScreenHandler.BUTTON_CARPENTER_PLUS,
                TownManagerScreenHandler.BUTTON_CARPENTER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_CARPENTER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.COURIER, TownManagerScreenHandler.BUTTON_COURIER_MINUS, TownManagerScreenHandler.BUTTON_COURIER_PLUS,
                TownManagerScreenHandler.BUTTON_COURIER_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_COURIER_PRIORITY_PLUS);
        addWorkerButtons(WorkstationType.BLACKSMITH, TownManagerScreenHandler.BUTTON_BLACKSMITH_MINUS, TownManagerScreenHandler.BUTTON_BLACKSMITH_PLUS,
                TownManagerScreenHandler.BUTTON_BLACKSMITH_PRIORITY_MINUS, TownManagerScreenHandler.BUTTON_BLACKSMITH_PRIORITY_PLUS);
        updateOverviewButtonVisibility();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawDraggedWorker(context, mouseX, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawListTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentTab == Tab.WORKERS) {
            WorkerIcon workerIcon = closestWorkerIcon((int) mouseX - x, (int) mouseY - y, 0);
            if (workerIcon != null) {
                draggedWorker = new DraggedWorker(workerIcon.workerId());
                return true;
            }
        }
        if (button == 0 && currentTab == Tab.PRODUCTION) {
            int buttonId = goalModeButtonAt((int) mouseX - x, (int) mouseY - y);
            if (buttonId != 0) {
                click(buttonId);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedWorker != null) {
            int buttonId = workerDropButtonId((int) mouseX - x, (int) mouseY - y, draggedWorker.workerId());
            draggedWorker = null;
            if (buttonId != 0) {
                click(buttonId);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        if (assignmentModeButton != null) {
            assignmentModeButton.setMessage(assignmentModeLabel());
        }
        if (currentTab == Tab.WORKERS) {
            workerIcons.clear();
            vacantSlots.clear();
            workerRows.clear();
            drawUnassignedWorkers(context);
        } else {
            context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.workers_compact",
                    handler.totalWorkers(), handler.unassignedWorkers()), 126, 53, MUTED, false);
        }
        drawCycleSeconds(context);

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
            button.active = false;
        }
    }

    private boolean positionOverviewButton(OverviewButton overviewButton) {
        boolean workerButton = isWorkerButton(overviewButton);
        if (workerButton || currentTab != Tab.OVERVIEW) {
            return false;
        }
        int scroll = overviewStatusScroll;
        int rowHeight = STATUS_ROW_HEIGHT;
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
            drawWorkerSlots(context, type, rowY);
            workerRows.add(new RowTarget(type, LIST_X, rowY, LIST_X + LIST_W, rowY + WORKER_ROW_HEIGHT));
        }
    }

    private void drawUnassignedWorkers(DrawContext context) {
        List<Integer> workers = workerIdsAssignedTo(TownState.UNASSIGNED_WORKER_ASSIGNMENT);
        int count = workers.size();
        if (count <= 0) {
            return;
        }
        int span = WORKER_HEADER_W - WORKER_ICON_SIZE;
        int step = count <= 1 ? 0 : span / (count - 1);
        for (int i = 0; i < count; i++) {
            int iconX = WORKER_HEADER_X + (count <= 1 ? span / 2 : step * i);
            int iconY = WORKER_HEADER_Y + (WORKER_HEADER_H - WORKER_ICON_SIZE) / 2;
            addAndDrawWorkerIcon(context, workers.get(i), null, iconX, iconY);
        }
    }

    private void drawWorkerSlots(DrawContext context, WorkstationType type, int rowY) {
        List<Integer> workers = workerIdsAssignedTo(type.ordinal());
        for (int slot = 0; slot < TownState.MAX_WORKERS_PER_WORKSTATION; slot++) {
            int slotX = WORKER_SLOT_X + slot * WORKER_SLOT_SPACING;
            int slotY = rowY + 3;
            context.drawBorder(slotX, slotY, WORKER_ICON_SIZE + 2, WORKER_ICON_SIZE + 2, SLOT);
            if (slot < workers.size()) {
                addAndDrawWorkerIcon(context, workers.get(slot), type, slotX + 1, slotY + 1);
            } else {
                vacantSlots.add(new SlotTarget(type, slotX + 1 + WORKER_ICON_SIZE / 2, slotY + 1 + WORKER_ICON_SIZE / 2));
            }
        }
    }

    private List<Integer> workerIdsAssignedTo(int assignmentOrdinal) {
        List<Integer> workers = new ArrayList<>();
        int count = Math.min(handler.totalWorkers(), TownManagerScreenHandler.MAX_SYNCED_WORKERS);
        for (int i = 0; i < count; i++) {
            int workerId = handler.workerId(i);
            if (workerId > 0 && handler.workerAssignmentOrdinal(i) == assignmentOrdinal) {
                workers.add(workerId);
            }
        }
        return workers;
    }

    private void addAndDrawWorkerIcon(DrawContext context, int workerId, WorkstationType assignment, int iconX, int iconY) {
        int centerX = iconX + WORKER_ICON_SIZE / 2;
        int centerY = iconY + WORKER_ICON_SIZE / 2;
        workerIcons.add(new WorkerIcon(workerId, centerX, centerY, assignment));
        if (draggedWorker == null || draggedWorker.workerId() != workerId) {
            drawWorkerIcon(context, iconX, iconY);
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
            drawGoalModeIcon(context, handler.goalModeOrdinal(resource), LIST_X + GOAL_MODE_ICON_X, yPos - 1);
            context.drawText(textRenderer, Integer.toString(handler.stockpileGoal(resource)), LIST_X + 94, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.productionPerDay(resource)), LIST_X + 132, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.consumptionPerDay(resource)), LIST_X + 172, yPos, resourceColor(resource), false);
            context.drawText(textRenderer, Integer.toString(handler.resource(resource)), LIST_X + 214, yPos, resourceColor(resource), false);
        }
    }

    private void drawProductionHeader(DrawContext context) {
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.product"), LIST_X + 6, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.goal"), LIST_X + 94, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.made"), LIST_X + 132, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.used"), LIST_X + 172, LIST_Y + 6, TEXT, false);
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.stored"), LIST_X + 214, LIST_Y + 6, TEXT, false);
    }

    private void drawGoalModeIcon(DrawContext context, int mode, int iconX, int iconY) {
        switch (mode) {
            case 0 -> drawSuspendedIcon(context, iconX, iconY);
            case 2 -> drawLimitedIcon(context, iconX, iconY);
            case 3 -> drawFavoredIcon(context, iconX, iconY);
            default -> drawUnlimitedIcon(context, iconX, iconY);
        }
    }

    private void drawSuspendedIcon(DrawContext context, int iconX, int iconY) {
        context.drawBorder(iconX + 1, iconY + 1, 8, 8, BAD);
        for (int i = 0; i < 8; i++) {
            context.fill(iconX + 1 + i, iconY + 1 + i, iconX + 2 + i, iconY + 2 + i, BAD);
        }
    }

    private void drawUnlimitedIcon(DrawContext context, int iconX, int iconY) {
        int color = 0xFF67B7FF;
        context.fill(iconX + 2, iconY + 5, iconX + 8, iconY + 6, color);
        context.fill(iconX + 1, iconY + 4, iconX + 3, iconY + 5, color);
        context.fill(iconX + 1, iconY + 6, iconX + 3, iconY + 7, color);
        context.fill(iconX + 7, iconY + 4, iconX + 9, iconY + 5, color);
        context.fill(iconX + 7, iconY + 6, iconX + 9, iconY + 7, color);
    }

    private void drawLimitedIcon(DrawContext context, int iconX, int iconY) {
        drawUpArrowIcon(context, iconX, iconY, WARN);
        context.fill(iconX + 2, iconY + 1, iconX + 8, iconY + 2, WARN);
    }

    private void drawFavoredIcon(DrawContext context, int iconX, int iconY) {
        drawUpArrowIcon(context, iconX, iconY, GOOD);
    }

    private void drawUpArrowIcon(DrawContext context, int iconX, int iconY, int color) {
        context.fill(iconX + 5, iconY + 2, iconX + 6, iconY + 9, color);
        context.fill(iconX + 4, iconY + 3, iconX + 7, iconY + 4, color);
        context.fill(iconX + 3, iconY + 4, iconX + 8, iconY + 5, color);
    }

    private void drawDraggedWorker(DrawContext context, int mouseX, int mouseY) {
        if (draggedWorker == null) {
            return;
        }
        drawWorkerIcon(context, mouseX - WORKER_ICON_SIZE / 2, mouseY - WORKER_ICON_SIZE / 2);
    }

    private void drawWorkerIcon(DrawContext context, int iconX, int iconY) {
        context.drawTexture(WORKER_ICON, iconX, iconY, 0, 0, WORKER_ICON_SIZE, WORKER_ICON_SIZE,
                WORKER_ICON_SIZE, WORKER_ICON_SIZE);
    }

    private void drawCycleSeconds(DrawContext context) {
        context.drawText(textRenderer, Text.translatable("container.aw2towns.town_manager.cycle_seconds",
                handler.cycleSeconds()), CYCLE_VALUE_X, 11, MUTED, false);
    }

    private Text assignmentModeLabel() {
        return Text.translatable(handler.dynamicAssignments()
                ? "container.aw2towns.town_manager.mode_dynamic"
                : "container.aw2towns.town_manager.mode_static");
    }

    private int workerDropButtonId(int relativeX, int relativeY, int draggedWorkerId) {
        if (isOverUnassignedHeader(relativeX, relativeY)) {
            return TownManagerScreenHandler.workerMoveToUnassignedButtonId(draggedWorkerId);
        }

        WorkerIcon workerTarget = closestWorkerIcon(relativeX, relativeY, draggedWorkerId);
        if (workerTarget != null && workerTarget.assignment() != null) {
            return TownManagerScreenHandler.workerSwapButtonId(draggedWorkerId, workerTarget.assignment(), workerTarget.workerId());
        }

        SlotTarget vacantSlot = closestVacantSlot(relativeX, relativeY);
        if (vacantSlot != null) {
            return TownManagerScreenHandler.workerMoveToWorkstationButtonId(draggedWorkerId, vacantSlot.type());
        }

        RowTarget row = rowTargetAt(relativeX, relativeY);
        if (row != null && handler.workers(row.type()) < TownState.MAX_WORKERS_PER_WORKSTATION) {
            return TownManagerScreenHandler.workerMoveToWorkstationButtonId(draggedWorkerId, row.type());
        }
        return 0;
    }

    private int goalModeButtonAt(int relativeX, int relativeY) {
        int minX = LIST_X + GOAL_MODE_ICON_X - 1;
        int minY = LIST_Y + 17;
        if (relativeX < minX || relativeX >= minX + GOAL_MODE_ICON_SIZE + 2
                || relativeY < minY || relativeY >= LIST_Y + LIST_H) {
            return 0;
        }
        int row = (relativeY - minY) / ROW_HEIGHT;
        if (row < 0 || row >= visibleRows()) {
            return 0;
        }
        int index = productionScroll + row;
        if (index < 0 || index >= ResourceType.values().length) {
            return 0;
        }
        return TownManagerScreenHandler.goalModeButtonId(ResourceType.values()[index]);
    }

    private WorkerIcon closestWorkerIcon(int relativeX, int relativeY, int ignoredWorkerId) {
        WorkerIcon closest = null;
        int closestDistance = WORKER_TARGET_RADIUS * WORKER_TARGET_RADIUS + 1;
        for (WorkerIcon workerIcon : workerIcons) {
            if (workerIcon.workerId() == ignoredWorkerId) {
                continue;
            }
            int distance = distanceSquared(relativeX, relativeY, workerIcon.centerX(), workerIcon.centerY());
            if (distance <= WORKER_TARGET_RADIUS * WORKER_TARGET_RADIUS && distance < closestDistance) {
                closestDistance = distance;
                closest = workerIcon;
            }
        }
        return closest;
    }

    private SlotTarget closestVacantSlot(int relativeX, int relativeY) {
        SlotTarget closest = null;
        int closestDistance = WORKER_TARGET_RADIUS * WORKER_TARGET_RADIUS + 1;
        for (SlotTarget slot : vacantSlots) {
            int distance = distanceSquared(relativeX, relativeY, slot.centerX(), slot.centerY());
            if (distance <= WORKER_TARGET_RADIUS * WORKER_TARGET_RADIUS && distance < closestDistance) {
                closestDistance = distance;
                closest = slot;
            }
        }
        return closest;
    }

    private RowTarget rowTargetAt(int relativeX, int relativeY) {
        for (RowTarget row : workerRows) {
            if (relativeX >= row.minX() && relativeX < row.maxX()
                    && relativeY >= row.minY() && relativeY < row.maxY()) {
                return row;
            }
        }
        return null;
    }

    private boolean isOverUnassignedHeader(int relativeX, int relativeY) {
        return relativeX >= WORKER_HEADER_X && relativeX < WORKER_HEADER_X + WORKER_HEADER_W
                && relativeY >= WORKER_HEADER_Y && relativeY < WORKER_HEADER_Y + WORKER_HEADER_H;
    }

    private static int distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
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
        if (handler.resource(resource) >= handler.stockpileGoal(resource)) {
            return GOOD;
        }
        return handler.productionPerDay(resource) > 0 ? WARN : BAD;
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
            case LOG -> "Lumber Mill";
            case IRON -> "Mine";
            case OAK_PLANKS -> "Carpenter";
            case TOOLS -> "Blacksmith";
        };
    }

    private static String consumers(ResourceType resource) {
        return switch (resource) {
            case WHEAT -> "Baker";
            case LOG -> "Bakers, carpenters";
            case BREAD -> "All workers";
            case OAK_PLANKS -> "Blacksmith";
            case IRON -> "Blacksmith";
            case TOOLS -> "Production workers";
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

    private record WorkerIcon(int workerId, int centerX, int centerY, WorkstationType assignment) {}

    private record SlotTarget(WorkstationType type, int centerX, int centerY) {}

    private record RowTarget(WorkstationType type, int minX, int minY, int maxX, int maxY) {}

    private record DraggedWorker(int workerId) {}
}
