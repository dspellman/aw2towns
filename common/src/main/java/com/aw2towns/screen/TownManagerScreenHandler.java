package com.aw2towns.screen;

import com.aw2towns.economy.ResourceType;
import com.aw2towns.economy.TownSavedData;
import com.aw2towns.economy.TownSimulationManager;
import com.aw2towns.economy.TownState;
import com.aw2towns.economy.TownWorkstationState;
import com.aw2towns.economy.WorkstationType;
import com.aw2towns.registry.ModBlocks;
import com.aw2towns.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TownManagerScreenHandler extends ScreenHandler {

    private static final int WORKSTATION_COUNT = WorkstationType.values().length;
    private static final int RESOURCE_COUNT = ResourceType.values().length;
    public static final int MAX_SYNCED_WORKERS = 64;

    public static final int BUTTON_FARM_MINUS = 0;
    public static final int BUTTON_FARM_PLUS = 1;
    public static final int BUTTON_BAKER_MINUS = 2;
    public static final int BUTTON_BAKER_PLUS = 3;
    public static final int BUTTON_MINE_MINUS = 4;
    public static final int BUTTON_MINE_PLUS = 5;
    public static final int BUTTON_LUMBER_MINUS = 6;
    public static final int BUTTON_LUMBER_PLUS = 7;
    public static final int BUTTON_CARPENTER_MINUS = 8;
    public static final int BUTTON_CARPENTER_PLUS = 9;
    public static final int BUTTON_COURIER_MINUS = 10;
    public static final int BUTTON_COURIER_PLUS = 11;
    public static final int BUTTON_BLACKSMITH_MINUS = 12;
    public static final int BUTTON_BLACKSMITH_PLUS = 13;
    public static final int BUTTON_FARM_PRIORITY_MINUS = 14;
    public static final int BUTTON_FARM_PRIORITY_PLUS = 15;
    public static final int BUTTON_BAKER_PRIORITY_MINUS = 16;
    public static final int BUTTON_BAKER_PRIORITY_PLUS = 17;
    public static final int BUTTON_MINE_PRIORITY_MINUS = 18;
    public static final int BUTTON_MINE_PRIORITY_PLUS = 19;
    public static final int BUTTON_LUMBER_PRIORITY_MINUS = 20;
    public static final int BUTTON_LUMBER_PRIORITY_PLUS = 21;
    public static final int BUTTON_CARPENTER_PRIORITY_MINUS = 22;
    public static final int BUTTON_CARPENTER_PRIORITY_PLUS = 23;
    public static final int BUTTON_COURIER_PRIORITY_MINUS = 24;
    public static final int BUTTON_COURIER_PRIORITY_PLUS = 25;
    public static final int BUTTON_BLACKSMITH_PRIORITY_MINUS = 26;
    public static final int BUTTON_BLACKSMITH_PRIORITY_PLUS = 27;
    public static final int BUTTON_CYCLE_MINUS = 28;
    public static final int BUTTON_CYCLE_PLUS = 29;
    private static final int WORKER_MOVE_BASE = 1000;
    private static final int WORKER_ID_MASK = 0x3FF;
    private static final int WORKER_MOVE_TARGET_UNASSIGNED = WorkstationType.values().length;

    private static final int IDX_TOTAL_WORKERS = 0;
    private static final int IDX_UNASSIGNED = 1;
    private static final int IDX_TRANSPORT_CAPACITY = 2;
    private static final int IDX_TRANSPORT_REMAINING = 3;
    private static final int IDX_CYCLE_SECONDS = 4;
    private static final int IDX_WORKERS = 5;
    private static final int IDX_PRIORITIES = IDX_WORKERS + WORKSTATION_COUNT;
    private static final int IDX_PRODUCTIVITY = IDX_PRIORITIES + WORKSTATION_COUNT;
    private static final int IDX_SHORTAGES = IDX_PRODUCTIVITY + WORKSTATION_COUNT;
    private static final int IDX_STORAGE = IDX_SHORTAGES + WORKSTATION_COUNT;
    private static final int IDX_PRODUCTION = IDX_STORAGE + RESOURCE_COUNT;
    private static final int IDX_CONSUMPTION = IDX_PRODUCTION + RESOURCE_COUNT;
    private static final int IDX_STOCKPILE_GOALS = IDX_CONSUMPTION + RESOURCE_COUNT;
    private static final int IDX_WORKER_IDS = IDX_STOCKPILE_GOALS + RESOURCE_COUNT;
    private static final int IDX_WORKER_ASSIGNMENTS = IDX_WORKER_IDS + MAX_SYNCED_WORKERS;
    public static final int DATA_COUNT = IDX_WORKER_ASSIGNMENTS + MAX_SYNCED_WORKERS;

    private final BlockPos pos;
    private final ScreenHandlerContext context;
    private final PropertyDelegate properties;

    public TownManagerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos(), ScreenHandlerContext.EMPTY, new ArrayPropertyDelegate(DATA_COUNT));
    }

    public TownManagerScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, pos, ScreenHandlerContext.create(playerInventory.player.getWorld(), pos),
                createServerProperties(playerInventory.player));
    }

    private TownManagerScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos,
                                     ScreenHandlerContext context, PropertyDelegate properties) {
        super(ModScreenHandlers.TOWN_MANAGER.get(), syncId);
        checkDataCount(properties, DATA_COUNT);
        this.pos = pos;
        this.context = context;
        this.properties = properties;
        addProperties(properties);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }
        if (id >= WORKER_MOVE_BASE) {
            return handleWorkerMove(player, serverWorld, id);
        }
        if (id == BUTTON_CYCLE_MINUS || id == BUTTON_CYCLE_PLUS) {
            TownSimulationManager.adjustCycleSeconds(id == BUTTON_CYCLE_PLUS ? 1 : -1);
            sendContentUpdates();
            return true;
        }
        ButtonAction action = buttonAction(id);
        if (action == null) {
            return false;
        }
        TownState town = TownSavedData.get(serverWorld).firstTown(serverWorld.getTime());
        if (action.priority) {
            return true;
        } else {
            town.adjustWorkers(action.type, action.delta);
        }
        TownSavedData.get(serverWorld).markDirty();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.TOWN_MANAGER.get());
    }

    public int totalWorkers() {
        return properties.get(IDX_TOTAL_WORKERS);
    }

    public int unassignedWorkers() {
        return properties.get(IDX_UNASSIGNED);
    }

    public int transportCapacity() {
        return properties.get(IDX_TRANSPORT_CAPACITY);
    }

    public int transportRemaining() {
        return properties.get(IDX_TRANSPORT_REMAINING);
    }

    public int cycleSeconds() {
        return properties.get(IDX_CYCLE_SECONDS);
    }

    public int workers(WorkstationType type) {
        return properties.get(IDX_WORKERS + type.ordinal());
    }

    public int priority(WorkstationType type) {
        return properties.get(IDX_PRIORITIES + type.ordinal());
    }

    public int productivity(WorkstationType type) {
        return properties.get(IDX_PRODUCTIVITY + type.ordinal());
    }

    public int shortageFlags(WorkstationType type) {
        return properties.get(IDX_SHORTAGES + type.ordinal());
    }

    public int resource(ResourceType resource) {
        return properties.get(IDX_STORAGE + resource.ordinal());
    }

    public int productionPerDay(ResourceType resource) {
        return properties.get(IDX_PRODUCTION + resource.ordinal());
    }

    public int consumptionPerDay(ResourceType resource) {
        return properties.get(IDX_CONSUMPTION + resource.ordinal());
    }

    public int netPerDay(ResourceType resource) {
        return productionPerDay(resource) - consumptionPerDay(resource);
    }

    public int stockpileGoal(ResourceType resource) {
        return properties.get(IDX_STOCKPILE_GOALS + resource.ordinal());
    }

    public boolean hasLargeEnoughStockpile(ResourceType resource) {
        int consumption = consumptionPerDay(resource);
        return consumption <= 0 || resource(resource) >= consumption * TownState.LARGE_STOCKPILE_DAYS;
    }

    public int workerId(int index) {
        return index >= 0 && index < MAX_SYNCED_WORKERS ? properties.get(IDX_WORKER_IDS + index) : 0;
    }

    public int workerAssignmentOrdinal(int index) {
        return index >= 0 && index < MAX_SYNCED_WORKERS
                ? properties.get(IDX_WORKER_ASSIGNMENTS + index)
                : TownState.UNASSIGNED_WORKER_ASSIGNMENT;
    }

    public BlockPos pos() {
        return pos;
    }

    public static int workerMoveToUnassignedButtonId(int workerId) {
        return workerMoveButtonId(workerId, WORKER_MOVE_TARGET_UNASSIGNED, 0);
    }

    public static int workerMoveToWorkstationButtonId(int workerId, WorkstationType target) {
        return workerMoveButtonId(workerId, target.ordinal(), 0);
    }

    public static int workerSwapButtonId(int workerId, WorkstationType target, int targetWorkerId) {
        return workerMoveButtonId(workerId, target.ordinal(), targetWorkerId);
    }

    private static int workerMoveButtonId(int workerId, int targetOrdinal, int targetWorkerId) {
        return WORKER_MOVE_BASE
                + (workerId & WORKER_ID_MASK)
                + ((targetWorkerId & WORKER_ID_MASK) << 10)
                + ((targetOrdinal & 0xF) << 20);
    }

    private static PropertyDelegate createServerProperties(PlayerEntity player) {
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
                    return 0;
                }
                TownState town = TownSavedData.get(serverWorld).firstTown(serverWorld.getTime());
                if (index == IDX_TOTAL_WORKERS) {
                    return town.totalWorkers();
                }
                if (index == IDX_UNASSIGNED) {
                    return town.unassignedWorkers();
                }
                if (index == IDX_TRANSPORT_CAPACITY) {
                    return town.transportCapacityPerStep();
                }
                if (index == IDX_TRANSPORT_REMAINING) {
                    return town.transportRemaining();
                }
                if (index == IDX_CYCLE_SECONDS) {
                    return TownSimulationManager.cycleSeconds();
                }
                if (index >= IDX_WORKERS && index < IDX_WORKERS + WORKSTATION_COUNT) {
                    return workstation(town, index - IDX_WORKERS).workers();
                }
                if (index >= IDX_PRIORITIES && index < IDX_PRIORITIES + WORKSTATION_COUNT) {
                    return workstation(town, index - IDX_PRIORITIES).priority();
                }
                if (index >= IDX_PRODUCTIVITY && index < IDX_PRODUCTIVITY + WORKSTATION_COUNT) {
                    return workstation(town, index - IDX_PRODUCTIVITY).productivityPercent();
                }
                if (index >= IDX_SHORTAGES && index < IDX_SHORTAGES + WORKSTATION_COUNT) {
                    return workstation(town, index - IDX_SHORTAGES).shortageFlags();
                }
                if (index >= IDX_STORAGE && index < IDX_STORAGE + RESOURCE_COUNT) {
                    return town.resource(resource(index - IDX_STORAGE));
                }
                if (index >= IDX_PRODUCTION && index < IDX_PRODUCTION + RESOURCE_COUNT) {
                    return town.productionPerDay(resource(index - IDX_PRODUCTION));
                }
                if (index >= IDX_CONSUMPTION && index < IDX_CONSUMPTION + RESOURCE_COUNT) {
                    return town.consumptionPerDay(resource(index - IDX_CONSUMPTION));
                }
                if (index >= IDX_STOCKPILE_GOALS && index < IDX_STOCKPILE_GOALS + RESOURCE_COUNT) {
                    return town.stockpileGoal(resource(index - IDX_STOCKPILE_GOALS));
                }
                if (index >= IDX_WORKER_IDS && index < IDX_WORKER_IDS + MAX_SYNCED_WORKERS) {
                    return town.workerId(index - IDX_WORKER_IDS);
                }
                if (index >= IDX_WORKER_ASSIGNMENTS && index < IDX_WORKER_ASSIGNMENTS + MAX_SYNCED_WORKERS) {
                    return town.workerAssignmentOrdinal(index - IDX_WORKER_ASSIGNMENTS);
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int size() {
                return DATA_COUNT;
            }
        };
    }

    private boolean handleWorkerMove(PlayerEntity player, ServerWorld serverWorld, int id) {
        WorkerMove move = decodeWorkerMove(id);
        if (move == null || !canUse(player)) {
            return false;
        }
        TownState town = TownSavedData.get(serverWorld).firstTown(serverWorld.getTime());
        boolean changed;
        if (move.targetOrdinal == WORKER_MOVE_TARGET_UNASSIGNED) {
            changed = town.moveWorkerToUnassigned(move.workerId);
        } else {
            WorkstationType target = WorkstationType.values()[move.targetOrdinal];
            changed = move.targetWorkerId > 0
                    ? town.swapWorkers(move.workerId, move.targetWorkerId, target)
                    : town.moveWorkerToWorkstation(move.workerId, target);
        }
        if (changed) {
            TownSavedData.get(serverWorld).markDirty();
            sendContentUpdates();
        }
        return changed;
    }

    private static WorkerMove decodeWorkerMove(int id) {
        int payload = id - WORKER_MOVE_BASE;
        int workerId = payload & WORKER_ID_MASK;
        int targetWorkerId = (payload >> 10) & WORKER_ID_MASK;
        int targetOrdinal = (payload >> 20) & 0xF;
        if (workerId <= 0 || targetOrdinal < 0 || targetOrdinal > WORKER_MOVE_TARGET_UNASSIGNED) {
            return null;
        }
        return new WorkerMove(workerId, targetOrdinal, targetWorkerId);
    }

    private static TownWorkstationState workstation(TownState town, int ordinal) {
        return town.workstation(WorkstationType.values()[ordinal]);
    }

    private static ResourceType resource(int ordinal) {
        return ResourceType.values()[ordinal];
    }

    private static ButtonAction buttonAction(int id) {
        return switch (id) {
            case BUTTON_FARM_MINUS -> new ButtonAction(WorkstationType.FARM, -1, false);
            case BUTTON_FARM_PLUS -> new ButtonAction(WorkstationType.FARM, 1, false);
            case BUTTON_BAKER_MINUS -> new ButtonAction(WorkstationType.BAKER, -1, false);
            case BUTTON_BAKER_PLUS -> new ButtonAction(WorkstationType.BAKER, 1, false);
            case BUTTON_MINE_MINUS -> new ButtonAction(WorkstationType.MINE, -1, false);
            case BUTTON_MINE_PLUS -> new ButtonAction(WorkstationType.MINE, 1, false);
            case BUTTON_LUMBER_MINUS -> new ButtonAction(WorkstationType.LUMBER_MILL, -1, false);
            case BUTTON_LUMBER_PLUS -> new ButtonAction(WorkstationType.LUMBER_MILL, 1, false);
            case BUTTON_CARPENTER_MINUS -> new ButtonAction(WorkstationType.CARPENTER, -1, false);
            case BUTTON_CARPENTER_PLUS -> new ButtonAction(WorkstationType.CARPENTER, 1, false);
            case BUTTON_COURIER_MINUS -> new ButtonAction(WorkstationType.COURIER, -1, false);
            case BUTTON_COURIER_PLUS -> new ButtonAction(WorkstationType.COURIER, 1, false);
            case BUTTON_BLACKSMITH_MINUS -> new ButtonAction(WorkstationType.BLACKSMITH, -1, false);
            case BUTTON_BLACKSMITH_PLUS -> new ButtonAction(WorkstationType.BLACKSMITH, 1, false);
            case BUTTON_FARM_PRIORITY_MINUS -> new ButtonAction(WorkstationType.FARM, -1, true);
            case BUTTON_FARM_PRIORITY_PLUS -> new ButtonAction(WorkstationType.FARM, 1, true);
            case BUTTON_BAKER_PRIORITY_MINUS -> new ButtonAction(WorkstationType.BAKER, -1, true);
            case BUTTON_BAKER_PRIORITY_PLUS -> new ButtonAction(WorkstationType.BAKER, 1, true);
            case BUTTON_MINE_PRIORITY_MINUS -> new ButtonAction(WorkstationType.MINE, -1, true);
            case BUTTON_MINE_PRIORITY_PLUS -> new ButtonAction(WorkstationType.MINE, 1, true);
            case BUTTON_LUMBER_PRIORITY_MINUS -> new ButtonAction(WorkstationType.LUMBER_MILL, -1, true);
            case BUTTON_LUMBER_PRIORITY_PLUS -> new ButtonAction(WorkstationType.LUMBER_MILL, 1, true);
            case BUTTON_CARPENTER_PRIORITY_MINUS -> new ButtonAction(WorkstationType.CARPENTER, -1, true);
            case BUTTON_CARPENTER_PRIORITY_PLUS -> new ButtonAction(WorkstationType.CARPENTER, 1, true);
            case BUTTON_COURIER_PRIORITY_MINUS -> new ButtonAction(WorkstationType.COURIER, -1, true);
            case BUTTON_COURIER_PRIORITY_PLUS -> new ButtonAction(WorkstationType.COURIER, 1, true);
            case BUTTON_BLACKSMITH_PRIORITY_MINUS -> new ButtonAction(WorkstationType.BLACKSMITH, -1, true);
            case BUTTON_BLACKSMITH_PRIORITY_PLUS -> new ButtonAction(WorkstationType.BLACKSMITH, 1, true);
            default -> null;
        };
    }

    private record ButtonAction(WorkstationType type, int delta, boolean priority) {}

    private record WorkerMove(int workerId, int targetOrdinal, int targetWorkerId) {}
}
