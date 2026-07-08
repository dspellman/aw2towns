package com.aw2towns.economy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public final class TownState {

    public static final int SCALE = 1000;
    public static final int STATUS_BLOCKED_MAX = 30;
    public static final int STATUS_PARTIAL_MAX = 89;
    public static final int LARGE_STOCKPILE_DAYS = 2;
    public static final int MAX_WORKERS_PER_WORKSTATION = 5;
    public static final int UNASSIGNED_WORKER_ASSIGNMENT = -1;

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_STOCKPILE_GOAL = 1;
    private static final long DEFAULT_CAPACITY = 1_000_000_000L * SCALE;
    private static final double BREAD_PER_WORKER_PER_DAY = 1.0D;
    private static final double TOOL_PER_WORKER_PER_DAY = 1.0D;
    private static final double MINE_IRON_PER_WORKER_PER_DAY = 15.0D;
    private static final double LUMBER_LOGS_PER_WORKER_PER_DAY = 15.0D;
    private static final double FARM_WHEAT_PER_WORKER_PER_DAY = 15.0D;
    private static final double BAKER_BREAD_PER_WORKER_PER_DAY = 10.0D;
    private static final double CARPENTER_ACTIONS_PER_WORKER_PER_DAY = 15.0D;
    private static final double COURIER_ITEMS_PER_WORKER_PER_DAY = 200.0D;
    private static final double SMITH_TOOLS_PER_WORKER_PER_DAY = 10.0D;
    private static final long BOOTSTRAP_OUTPUT_PER_WORKER_PER_DAY = 1L;
    private static final long BREAD_WHEAT_COST = 3L;
    private static final long BREAD_LOG_COST_DIVISOR = 4L;
    private static final long PLANKS_PER_LOG = 4L;
    private static final long STICK_PLANK_COST = 2L;
    private static final long STICKS_PER_ACTION = 4L;
    private static final long TOOL_IRON_COST = 3L;
    private static final long TOOL_STICK_COST = 2L;

    private final String id;
    private String name;
    private int totalWorkers;
    private long lastSimulatedGameTime;
    private long lastTransportCapacity;
    private long lastTransportRemaining;
    private int nextWorkerId = 1;
    private final List<TownWorker> townWorkers = new ArrayList<>();
    private final EnumMap<ResourceType, Long> storage = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> stockpileGoals = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> productionPerDay = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> consumptionPerDay = new EnumMap<>(ResourceType.class);
    private final EnumMap<WorkstationType, TownWorkstationState> workstations = new EnumMap<>(WorkstationType.class);

    public TownState(String id, String name) {
        this.id = id;
        this.name = name;
        for (ResourceType resource : ResourceType.values()) {
            storage.put(resource, 0L);
            stockpileGoals.put(resource, DEFAULT_STOCKPILE_GOAL);
            productionPerDay.put(resource, 0);
            consumptionPerDay.put(resource, 0);
        }
        for (WorkstationType type : WorkstationType.values()) {
            workstations.put(type, new TownWorkstationState(type, 0));
        }
    }

    public static TownState starter(long gameTime) {
        TownState town = new TownState("starter", "Prototype Town");
        town.totalWorkers = 17;
        town.workstation(WorkstationType.FARM).setWorkers(4);
        town.workstation(WorkstationType.BAKER).setWorkers(2);
        town.workstation(WorkstationType.MINE).setWorkers(3);
        town.workstation(WorkstationType.LUMBER_MILL).setWorkers(1);
        town.workstation(WorkstationType.CARPENTER).setWorkers(1);
        town.workstation(WorkstationType.COURIER).setWorkers(2);
        town.workstation(WorkstationType.BLACKSMITH).setWorkers(2);
        town.createWorkersFromWorkstationCounts();
        town.lastSimulatedGameTime = gameTime;
        town.refreshDailyRates();
        return town;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int totalWorkers() {
        return townWorkers.isEmpty() ? totalWorkers : townWorkers.size();
    }

    public int assignedWorkers() {
        int assigned = 0;
        for (TownWorker worker : townWorkers) {
            if (!worker.isUnassigned()) {
                assigned++;
            }
        }
        return assigned;
    }

    public int unassignedWorkers() {
        if (townWorkers.isEmpty()) {
            return Math.max(0, totalWorkers - assignedWorkers());
        }
        int unassigned = 0;
        for (TownWorker worker : townWorkers) {
            if (worker.isUnassigned()) {
                unassigned++;
            }
        }
        return unassigned;
    }

    public int workerId(int index) {
        return index >= 0 && index < townWorkers.size() ? townWorkers.get(index).id() : 0;
    }

    public int workerAssignmentOrdinal(int index) {
        if (index < 0 || index >= townWorkers.size()) {
            return UNASSIGNED_WORKER_ASSIGNMENT;
        }
        WorkstationType assignment = townWorkers.get(index).assignment();
        return assignment == null ? UNASSIGNED_WORKER_ASSIGNMENT : assignment.ordinal();
    }

    public int transportCapacityPerStep() {
        return 0;
    }

    public int transportRemaining() {
        return 0;
    }

    public int resource(ResourceType resource) {
        return toWhole(storage.get(resource));
    }

    public int productionPerDay(ResourceType resource) {
        return productionPerDay.get(resource);
    }

    public int consumptionPerDay(ResourceType resource) {
        return consumptionPerDay.get(resource);
    }

    public int netPerDay(ResourceType resource) {
        return productionPerDay(resource) - consumptionPerDay(resource);
    }

    public int stockpileGoal(ResourceType resource) {
        return stockpileGoals.get(resource);
    }

    public TownWorkstationState workstation(WorkstationType type) {
        return workstations.get(type);
    }

    public void adjustWorkers(WorkstationType type, int delta) {
        if (delta > 0) {
            TownWorker worker = firstUnassignedWorker();
            if (worker == null || workers(type) >= MAX_WORKERS_PER_WORKSTATION) {
                return;
            }
            worker.assign(type);
        } else if (delta < 0) {
            TownWorker worker = lastWorkerAssignedTo(type);
            if (worker == null) {
                return;
            }
            worker.assign(null);
        }
        syncWorkstationWorkerCounts();
        refreshDailyRates();
    }

    public void adjustPriority(WorkstationType type, int delta) {
        // Priorities are currently display-only while worker assignment is manual.
    }

    public boolean moveWorkerToUnassigned(int workerId) {
        TownWorker worker = worker(workerId);
        if (worker == null) {
            return false;
        }
        worker.assign(null);
        syncWorkstationWorkerCounts();
        refreshDailyRates();
        return true;
    }

    public boolean moveWorkerToWorkstation(int workerId, WorkstationType target) {
        TownWorker worker = worker(workerId);
        if (worker == null || target == null) {
            return false;
        }
        if (worker.assignment() != target && workers(target) >= MAX_WORKERS_PER_WORKSTATION) {
            return false;
        }
        worker.assign(target);
        syncWorkstationWorkerCounts();
        refreshDailyRates();
        return true;
    }

    public boolean swapWorkers(int firstWorkerId, int secondWorkerId, WorkstationType expectedSecondAssignment) {
        if (firstWorkerId == secondWorkerId) {
            return false;
        }
        TownWorker first = worker(firstWorkerId);
        TownWorker second = worker(secondWorkerId);
        if (first == null || second == null || second.assignment() != expectedSecondAssignment) {
            return false;
        }
        WorkstationType firstAssignment = first.assignment();
        first.assign(second.assignment());
        second.assign(firstAssignment);
        syncWorkstationWorkerCounts();
        refreshDailyRates();
        return true;
    }

    public void resetPrototypeEconomy() {
        totalWorkers = 17;
        for (ResourceType resource : ResourceType.values()) {
            resourceRaw(resource, 0L);
            stockpileGoals.put(resource, DEFAULT_STOCKPILE_GOAL);
        }
        workstation(WorkstationType.FARM).setWorkers(4);
        workstation(WorkstationType.BAKER).setWorkers(2);
        workstation(WorkstationType.MINE).setWorkers(3);
        workstation(WorkstationType.LUMBER_MILL).setWorkers(1);
        workstation(WorkstationType.CARPENTER).setWorkers(1);
        workstation(WorkstationType.COURIER).setWorkers(2);
        workstation(WorkstationType.BLACKSMITH).setWorkers(2);
        createWorkersFromWorkstationCounts();
        refreshDailyRates();
    }

    public void resetBootstrapEconomy() {
        for (ResourceType resource : ResourceType.values()) {
            resourceRaw(resource, SCALE);
            stockpileGoals.put(resource, DEFAULT_STOCKPILE_GOAL);
            productionPerDay.put(resource, 0);
            consumptionPerDay.put(resource, 0);
        }
        for (TownWorker worker : townWorkers) {
            for (ResourceType resource : ResourceType.values()) {
                worker.setToolDurability(resource, 0);
            }
        }
        resetWorkstations();
        refreshDailyRates();
    }

    public void simulateUntil(long gameTime, SimulationCycle cycle) {
        if (lastSimulatedGameTime <= 0) {
            lastSimulatedGameTime = gameTime;
            return;
        }
        if (gameTime < lastSimulatedGameTime) {
            lastSimulatedGameTime = gameTime;
            return;
        }
        long lastCycle = cycle.cycleIndex(lastSimulatedGameTime);
        long currentCycle = cycle.cycleIndex(gameTime);
        int steps = 0;
        while (lastCycle < currentCycle && steps < 24) {
            simulateDay();
            lastCycle++;
            steps++;
        }
        lastSimulatedGameTime = gameTime;
    }

    private void simulateDay() {
        clearRates();
        resetWorkstations();
        lastTransportCapacity = 0L;
        lastTransportRemaining = 0L;

        DailyWorkPlan plan = new DailyWorkPlan();
        List<ResourceType> priorities = dailyProductionPriorities();
        for (ResourceType resource : priorities) {
            assignProductionFor(resource, plan);
        }
        consumeIdleWorkerFood(plan);
        for (ResourceType resource : ResourceType.values()) {
            long produced = plan.produced.get(resource);
            if (produced > 0) {
                add(resource, produced);
            }
            productionPerDay.put(resource, toWhole(produced));
        }
        decrementSurplusGoals();
        updateWorkstationProductivity(plan);
    }

    private List<ResourceType> dailyProductionPriorities() {
        List<ResourceType> resources = new ArrayList<>(List.of(ResourceType.values()));
        resources.sort(Comparator
                .comparingDouble((ResourceType resource) -> priorityFor(resource))
                .reversed()
                .thenComparingInt(Enum::ordinal));
        return resources;
    }

    private double priorityFor(ResourceType resource) {
        return stockpileGoal(resource) / (1.0D + resource(resource));
    }

    private void assignProductionFor(ResourceType resource, DailyWorkPlan plan) {
        while (raw(resource) + plan.produced.get(resource) < (long) stockpileGoal(resource) * SCALE) {
            ProductionRecipe recipe = recipeFor(resource, plan);
            if (recipe == null) {
                return;
            }
            consumeWorkerFood(recipe, plan);
            applyToolUse(recipe, plan);
            consumeRecipeInputs(recipe, plan);
            plan.availableWorkers.put(recipe.workstation(), plan.availableWorkers.get(recipe.workstation()) - 1);
            plan.assignedWorkers.put(recipe.workstation(), plan.assignedWorkers.get(recipe.workstation()) + 1);
            plan.produced.put(resource, plan.produced.get(resource) + recipe.output());
        }
    }

    private ProductionRecipe recipeFor(ResourceType resource, DailyWorkPlan plan) {
        return switch (resource) {
            case WHEAT -> baseResourceRecipe(resource, WorkstationType.FARM, plan);
            case IRON -> baseResourceRecipe(resource, WorkstationType.MINE, plan);
            case LOG -> baseResourceRecipe(resource, WorkstationType.LUMBER_MILL, plan);
            case BREAD -> workerRecipe(plan, WorkstationType.BAKER, resource, whole(BAKER_BREAD_PER_WORKER_PER_DAY),
                    input(ResourceType.WHEAT, BREAD_WHEAT_COST),
                    inputScaled(ResourceType.LOG, Math.round(SCALE / (double) BREAD_LOG_COST_DIVISOR)));
            case OAK_PLANKS -> carpenterPlankRecipe(resource, plan);
            case STICK -> carpenterStickRecipe(resource, plan);
            case PICKAXE, AXE, HOE, SAW, HAMMER -> blacksmithRecipe(resource, plan);
        };
    }

    private ProductionRecipe baseResourceRecipe(ResourceType resource, WorkstationType workstation, DailyWorkPlan plan) {
        return workerRecipe(plan, workstation, resource, baseOutputFor(workstation), 3, toolFor(workstation));
    }

    private int baseOutputFor(WorkstationType workstation) {
        return switch (workstation) {
            case FARM -> whole(FARM_WHEAT_PER_WORKER_PER_DAY);
            case MINE -> whole(MINE_IRON_PER_WORKER_PER_DAY);
            case LUMBER_MILL -> whole(LUMBER_LOGS_PER_WORKER_PER_DAY);
            default -> 0;
        };
    }

    private ProductionRecipe workerRecipe(DailyWorkPlan plan, WorkstationType workstation, ResourceType output,
                                          int outputAmount, RecipeInput... inputs) {
        return workerRecipe(plan, workstation, output, outputAmount, 0, null, inputs);
    }

    private ProductionRecipe workerRecipe(DailyWorkPlan plan, WorkstationType workstation, ResourceType output,
                                          int outputCapacity, int fallbackCapacity, ResourceType tool,
                                          RecipeInput... inputsPerItem) {
        if (plan.availableWorkers.get(workstation) <= 0) {
            return null;
        }
        TownWorker worker = workerForNextProduction(workstation, plan);
        if (worker == null) {
            return null;
        }
        ToolPlan toolPlan = planToolUse(worker, workstation, tool, outputCapacity, fallbackCapacity, plan);
        if (tool != null && toolPlan.outputCapacity() < outputCapacity) {
            addShortage(workstation, tool);
            recordDeficit(tool);
        }
        int outputAmount = Math.min(toolPlan.outputCapacity(), inputLimitedOutput(workstation, outputCapacity, plan, inputsPerItem));
        if (outputAmount <= 0) {
            return null;
        }
        List<RecipeInput> inputs = new ArrayList<>();
        for (RecipeInput input : inputsPerItem) {
            long amount = input.amount() * outputAmount;
            if (amount > 0) {
                inputs.add(new RecipeInput(input.resource(), amount));
            }
        }
        return new ProductionRecipe(worker, workstation, (long) outputAmount * SCALE, inputs, toolPlan.tool(), outputAmount);
    }

    private ProductionRecipe carpenterPlankRecipe(ResourceType resource, DailyWorkPlan plan) {
        return workerRecipe(plan, WorkstationType.CARPENTER, resource,
                whole(CARPENTER_ACTIONS_PER_WORKER_PER_DAY * PLANKS_PER_LOG),
                (int) BOOTSTRAP_OUTPUT_PER_WORKER_PER_DAY, toolFor(WorkstationType.CARPENTER),
                inputScaled(ResourceType.LOG, SCALE / PLANKS_PER_LOG));
    }

    private ProductionRecipe carpenterStickRecipe(ResourceType resource, DailyWorkPlan plan) {
        return workerRecipe(plan, WorkstationType.CARPENTER, resource,
                whole(CARPENTER_ACTIONS_PER_WORKER_PER_DAY * STICKS_PER_ACTION),
                (int) BOOTSTRAP_OUTPUT_PER_WORKER_PER_DAY, toolFor(WorkstationType.CARPENTER),
                inputScaled(ResourceType.OAK_PLANKS, SCALE * STICK_PLANK_COST / STICKS_PER_ACTION));
    }

    private ProductionRecipe blacksmithRecipe(ResourceType resource, DailyWorkPlan plan) {
        return workerRecipe(plan, WorkstationType.BLACKSMITH, resource, whole(SMITH_TOOLS_PER_WORKER_PER_DAY),
                (int) BOOTSTRAP_OUTPUT_PER_WORKER_PER_DAY, toolFor(WorkstationType.BLACKSMITH),
                input(ResourceType.IRON, TOOL_IRON_COST),
                input(ResourceType.STICK, TOOL_STICK_COST));
    }

    private ResourceType toolFor(WorkstationType workstation) {
        return switch (workstation) {
            case FARM -> ResourceType.HOE;
            case MINE -> ResourceType.PICKAXE;
            case LUMBER_MILL -> ResourceType.AXE;
            case CARPENTER -> ResourceType.SAW;
            case BLACKSMITH -> ResourceType.HAMMER;
            default -> null;
        };
    }

    private TownWorker workerForNextProduction(WorkstationType type, DailyWorkPlan plan) {
        int index = plan.assignedWorkers.get(type);
        int seen = 0;
        for (TownWorker worker : townWorkers) {
            if (worker.assignment() == type) {
                if (seen == index) {
                    return worker;
                }
                seen++;
            }
        }
        return null;
    }

    private ToolPlan planToolUse(TownWorker worker, WorkstationType workstation, ResourceType tool,
                                 int outputCapacity, int fallbackCapacity, DailyWorkPlan plan) {
        if (tool == null) {
            return new ToolPlan(null, outputCapacity);
        }
        int currentDurability = worker.toolDurability(tool);
        int storedTools = storedWhole(tool);
        if (currentDurability <= 0 && storedTools <= 0) {
            addShortage(workstation, tool);
            recordDeficitIfEmpty(tool);
            return new ToolPlan(null, fallbackCapacity);
        }
        long availableDurability = (long) currentDurability + (long) storedTools * TownWorker.DEFAULT_TOOL_DURABILITY;
        return new ToolPlan(tool, (int) Math.min(outputCapacity, availableDurability));
    }

    private int inputLimitedOutput(WorkstationType workstation, int targetOutput, DailyWorkPlan plan,
                                   RecipeInput... inputsPerItem) {
        int limit = Integer.MAX_VALUE;
        for (RecipeInput input : inputsPerItem) {
            if (input.amount() <= 0) {
                continue;
            }
            long stored = raw(input.resource());
            if (stored == 0L) {
                addShortage(workstation, input.resource());
                recordDeficitIfEmpty(input.resource());
            }
            int inputLimit = (int) (stored / input.amount());
            if (inputLimit < targetOutput) {
                addShortage(workstation, input.resource());
                recordDeficit(input.resource());
            }
            limit = Math.min(limit, inputLimit);
        }
        return limit == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, limit);
    }

    private void applyToolUse(ProductionRecipe recipe, DailyWorkPlan plan) {
        if (recipe.tool() == null || recipe.toolDurabilityUsed() <= 0) {
            return;
        }
        TownWorker worker = recipe.worker();
        ResourceType tool = recipe.tool();
        int durability = worker.toolDurability(tool);
        int remainingUse = recipe.toolDurabilityUsed();
        while (remainingUse > 0) {
            if (durability <= 0) {
                long consumed = consumeAvailable(tool, SCALE, plan);
                if (consumed < SCALE) {
                    addShortage(recipe.workstation(), tool);
                    recordDeficitIfEmpty(tool);
                    worker.setToolDurability(tool, 0);
                    return;
                }
                durability = TownWorker.DEFAULT_TOOL_DURABILITY;
            }
            int used = Math.min(durability, remainingUse);
            durability -= used;
            remainingUse -= used;
        }
        worker.setToolDurability(tool, durability);
    }

    private void consumeWorkerFood(ProductionRecipe recipe, DailyWorkPlan plan) {
        consumeWorkerFood(plan, recipe.workstation());
    }

    private void consumeIdleWorkerFood(DailyWorkPlan plan) {
        for (WorkstationType type : WorkstationType.values()) {
            int idleWorkers = Math.max(0, workers(type) - plan.assignedWorkers.get(type));
            for (int i = 0; i < idleWorkers; i++) {
                consumeWorkerFood(plan, type);
            }
        }
        for (int i = 0; i < unassignedWorkers(); i++) {
            consumeWorkerFood(plan, null);
        }
    }

    private void consumeWorkerFood(DailyWorkPlan plan, WorkstationType workstation) {
        long consumed = consumeAvailable(ResourceType.BREAD, SCALE, plan);
        if (consumed < SCALE) {
            addShortage(workstation, ResourceType.BREAD);
            recordDeficit(ResourceType.BREAD);
        }
        if (raw(ResourceType.BREAD) == 0L) {
            recordDeficitIfEmpty(ResourceType.BREAD);
        }
    }

    private int storedWhole(ResourceType resource) {
        return (int) Math.max(0L, raw(resource) / SCALE);
    }

    private RecipeInput input(ResourceType resource, long amount) {
        return new RecipeInput(resource, amount * SCALE);
    }

    private RecipeInput inputScaled(ResourceType resource, long amount) {
        return new RecipeInput(resource, amount);
    }

    private void consumeRecipeInputs(ProductionRecipe recipe, DailyWorkPlan plan) {
        for (RecipeInput input : recipe.inputs()) {
            long consumed = consumeAvailable(input.resource(), input.amount(), plan);
            if (consumed < input.amount()) {
                addShortage(recipe.workstation(), input.resource());
                recordDeficit(input.resource());
            }
            if (raw(input.resource()) == 0L) {
                recordDeficitIfEmpty(input.resource());
            }
        }
    }

    private long consumeAvailable(ResourceType resource, long amount, DailyWorkPlan plan) {
        long consumed = Math.min(raw(resource), Math.max(0L, amount));
        consume(resource, consumed);
        if (consumed > 0) {
            plan.consumed.put(resource, plan.consumed.get(resource) + consumed);
            consumptionPerDay.put(resource, toWhole(plan.consumed.get(resource)));
        }
        return consumed;
    }

    private void recordDeficitIfEmpty(ResourceType resource) {
        if (raw(resource) > 0L) {
            return;
        }
        recordDeficit(resource);
    }

    private void recordDeficit(ResourceType resource) {
        stockpileGoals.put(resource, stockpileGoal(resource) + 1);
    }

    private void decrementSurplusGoals() {
        for (ResourceType resource : ResourceType.values()) {
            if (resource(resource) > stockpileGoal(resource)) {
                stockpileGoals.put(resource, Math.max(1, stockpileGoal(resource) - 1));
            }
        }
    }

    private void addShortage(WorkstationType type, ResourceType resource) {
        if (type == null) {
            return;
        }
        TownWorkstationState workstation = workstation(type);
        workstation.setShortageFlags(workstation.shortageFlags() | (1 << resource.ordinal()));
    }

    private void updateWorkstationProductivity(DailyWorkPlan plan) {
        for (WorkstationType type : WorkstationType.values()) {
            TownWorkstationState workstation = workstation(type);
            if (workstation.workers() <= 0) {
                workstation.setProductivityPercent(0);
                continue;
            }
            if (workstation.shortageFlags() != 0 && plan.assignedWorkers.get(type) <= 0) {
                workstation.setProductivityPercent(0);
            } else if (workstation.shortageFlags() != 0) {
                workstation.setProductivityPercent(50);
            } else {
                workstation.setProductivityPercent(100);
            }
        }
    }

    private WorkDemand buildBlacksmithDemand(SimulationCycle cycle, TransportLoad transport) {
        TownWorkstationState workstation = workstation(WorkstationType.BLACKSMITH);
        if (workstation.workers() <= 0) {
            return null;
        }
        long tools = transportedWorkerOutput(workstation.workers(), cycle.perStep(SMITH_TOOLS_PER_WORKER_PER_DAY), transport);
        if (tools <= 0) {
            workstation.setProductivityPercent(0);
            return null;
        }
        WorkDemand demand = new WorkDemand(WorkstationType.BLACKSMITH, workstation.workers(), workstation.priority());
        demand.add(ResourceType.IRON, tools * TOOL_IRON_COST);
        demand.add(ResourceType.STICK, tools * TOOL_STICK_COST);
        return demand;
    }

    private WorkDemand buildBakerDemand(SimulationCycle cycle, TransportLoad transport) {
        TownWorkstationState workstation = workstation(WorkstationType.BAKER);
        if (workstation.workers() <= 0) {
            return null;
        }
        long bread = transportedWorkerOutput(workstation.workers(), cycle.perStep(BAKER_BREAD_PER_WORKER_PER_DAY), transport);
        if (bread <= 0) {
            workstation.setProductivityPercent(0);
            return null;
        }
        WorkDemand demand = new WorkDemand(WorkstationType.BAKER, workstation.workers(), workstation.priority());
        demand.add(ResourceType.WHEAT, bread * BREAD_WHEAT_COST);
        demand.add(ResourceType.LOG, bread / BREAD_LOG_COST_DIVISOR);
        return demand;
    }

    private List<WorkDemand> buildUpkeepDemands(SimulationCycle cycle) {
        List<WorkDemand> demands = new ArrayList<>();
        long bread = cycle.perStep(BREAD_PER_WORKER_PER_DAY);
        long tool = cycle.perStep(TOOL_PER_WORKER_PER_DAY);
        addDemand(demands, WorkstationType.FARM, ResourceType.BREAD, bread, ResourceType.HOE, tool);
        addDemand(demands, WorkstationType.BAKER, ResourceType.BREAD, bread, null, 0L);
        addDemand(demands, WorkstationType.MINE, ResourceType.BREAD, bread, ResourceType.PICKAXE, tool);
        addDemand(demands, WorkstationType.LUMBER_MILL, ResourceType.BREAD, bread, ResourceType.AXE, tool);
        addDemand(demands, WorkstationType.CARPENTER, ResourceType.BREAD, bread, ResourceType.SAW, tool);
        addDemand(demands, WorkstationType.COURIER, ResourceType.BREAD, bread, null, 0L);
        addDemand(demands, WorkstationType.BLACKSMITH, ResourceType.BREAD, bread, ResourceType.HAMMER, tool);
        return demands;
    }

    private WorkDemand addDemand(List<WorkDemand> demands, WorkstationType type,
                                 ResourceType food, long foodPerWorker,
                                 ResourceType equipment, long equipmentPerWorker) {
        TownWorkstationState workstation = workstation(type);
        if (workstation.workers() <= 0) {
            return null;
        }
        WorkDemand demand = new WorkDemand(type, workstation.workers(), workstation.priority());
        demand.add(food, foodPerWorker * workstation.workers());
        if (equipment != null && equipmentPerWorker > 0) {
            demand.add(equipment, equipmentPerWorker * workstation.workers());
        }
        demands.add(demand);
        return demand;
    }

    private void allocateInputs(List<WorkDemand> demands) {
        for (ResourceType resource : ResourceType.values()) {
            List<WorkDemand> needingResource = demands.stream()
                    .filter(demand -> demand.needed(resource) > 0)
                    .toList();
            long groupNeed = 0;
            for (WorkDemand demand : needingResource) {
                groupNeed += demand.needed(resource);
            }
            if (groupNeed <= 0) {
                continue;
            }

            long available = raw(resource);
            double factor = Math.min(1.0D, available / (double) groupNeed);
            long consumedByGroup = 0;
            for (WorkDemand demand : needingResource) {
                long consumed = Math.round(demand.needed(resource) * factor);
                demand.setAllocated(resource, consumed);
                consumedByGroup += consumed;
            }
            consume(resource, Math.min(available, consumedByGroup));
        }

        for (WorkDemand demand : demands) {
            demand.calculateInputFactor();
        }
    }

    private void produceFor(WorkstationType type, int workers, double factor, EnumMap<ResourceType, Long> produced, SimulationCycle cycle) {
        switch (type) {
            case FARM -> addProduced(produced, ResourceType.WHEAT, cycle.perStep(FARM_WHEAT_PER_WORKER_PER_DAY) * workers, factor);
            case BAKER -> addProduced(produced, ResourceType.BREAD, breadProducedBy(workers, cycle), factor);
            case MINE -> addProduced(produced, ResourceType.IRON, cycle.perStep(MINE_IRON_PER_WORKER_PER_DAY) * workers, factor);
            case LUMBER_MILL -> addProduced(produced, ResourceType.LOG, cycle.perStep(LUMBER_LOGS_PER_WORKER_PER_DAY) * workers, factor);
            case CARPENTER, COURIER -> {}
            case BLACKSMITH -> addProducedTools(produced, Math.round(toolsProducedBy(workers, cycle) * factor));
        }
    }

    private void produceBasicWorkers(WorkstationType type, ResourceType resource, long amountPerWorker, TransportLoad transport) {
        TownWorkstationState workstation = workstation(type);
        if (workstation.workers() <= 0 || amountPerWorker <= 0) {
            return;
        }
        int activeWorkers = 0;
        for (int i = 0; i < workstation.workers(); i++) {
            if (transport.tryMove(amountPerWorker)) {
                add(resource, amountPerWorker);
                activeWorkers++;
            } else {
                transport.markOverflow(amountPerWorker);
            }
        }
        workstation.setProductivityPercent(percent(activeWorkers / (double) workstation.workers()));
    }

    private void processCarpenter(SimulationCycle cycle, TransportLoad transport) {
        TownWorkstationState carpenter = workstation(WorkstationType.CARPENTER);
        if (carpenter.workers() <= 0) {
            return;
        }

        long actions = carpenterActionsBy(carpenter.workers(), cycle);
        long completed = 0L;
        int shortageFlags = 0;
        while (actions - completed > 0) {
            long action = Math.min(SCALE, actions - completed);
            if (shouldMakeSticks() && raw(ResourceType.OAK_PLANKS) >= action * STICK_PLANK_COST
                    && transport.canMove(action * STICKS_PER_ACTION)) {
                consume(ResourceType.OAK_PLANKS, action * STICK_PLANK_COST);
                transport.tryMove(action * STICKS_PER_ACTION);
                add(ResourceType.STICK, action * STICKS_PER_ACTION);
                completed += action;
                continue;
            }
            if (raw(ResourceType.LOG) >= action && transport.canMove(action * PLANKS_PER_LOG)) {
                consume(ResourceType.LOG, action);
                transport.tryMove(action * PLANKS_PER_LOG);
                add(ResourceType.OAK_PLANKS, action * PLANKS_PER_LOG);
                completed += action;
                continue;
            }
            if (!transport.canMove(action)) {
                transport.markOverflow(action);
            }
            shortageFlags |= 1 << ResourceType.LOG.ordinal();
            if (shouldMakeSticks()) {
                shortageFlags |= 1 << ResourceType.OAK_PLANKS.ordinal();
            }
            break;
        }

        carpenter.setProductivityPercent(percent(actions <= 0 ? 1.0D : completed / (double) actions));
        carpenter.setShortageFlags(shortageFlags);
    }

    private boolean shouldMakeSticks() {
        return raw(ResourceType.STICK) < (long) consumptionPerDay(ResourceType.STICK) * SCALE;
    }

    private void addProducedTools(EnumMap<ResourceType, Long> produced, long totalTools) {
        if (totalTools <= 0) {
            return;
        }

        long remaining = totalTools;
        while (remaining > 0) {
            ResourceType target = toolTargetForProduced(produced);
            long amount = Math.min(SCALE, remaining);
            produced.put(target, produced.get(target) + amount);
            remaining -= amount;
        }
    }

    private ResourceType toolTargetForProduced(EnumMap<ResourceType, Long> produced) {
        ResourceType protectedHammer = protectedHammerTarget(raw(ResourceType.HAMMER) + produced.get(ResourceType.HAMMER));
        if (protectedHammer != null) {
            return protectedHammer;
        }

        ResourceType bestDeficit = null;
        long bestDeficitAmount = 0L;
        for (ResourceType tool : toolResources()) {
            long needed = (long) consumptionPerDay(tool) * SCALE;
            long available = raw(tool) + produced.get(tool);
            long deficit = needed - available;
            if (deficit > bestDeficitAmount) {
                bestDeficitAmount = deficit;
                bestDeficit = tool;
            }
        }
        if (bestDeficit != null) {
            return bestDeficit;
        }

        ResourceType lowest = ResourceType.PICKAXE;
        long lowestAmount = Long.MAX_VALUE;
        for (ResourceType tool : toolResources()) {
            long amount = raw(tool) + produced.get(tool);
            if (amount < lowestAmount) {
                lowestAmount = amount;
                lowest = tool;
            }
        }
        return lowest;
    }

    private ResourceType[] toolResources() {
        return new ResourceType[] {
                ResourceType.PICKAXE,
                ResourceType.AXE,
                ResourceType.HOE,
                ResourceType.SAW,
                ResourceType.HAMMER
        };
    }

    private ResourceType protectedHammerTarget(long availableHammers) {
        int blacksmiths = workstation(WorkstationType.BLACKSMITH).workers();
        if (blacksmiths <= 0) {
            return null;
        }
        long target = (long) (blacksmiths + 1) * SCALE;
        return availableHammers < target ? ResourceType.HAMMER : null;
    }

    private void addProduced(EnumMap<ResourceType, Long> produced, ResourceType resource, long base, double factor) {
        produced.put(resource, produced.get(resource) + Math.round(base * factor));
    }

    private void addProducedToStorage(EnumMap<ResourceType, Long> produced, TransportLoad transport) {
        for (ResourceType resource : ResourceType.values()) {
            long amount = produced.get(resource);
            if (amount > 0) {
                if (transport.tryMove(amount)) {
                    add(resource, amount);
                } else {
                    transport.markOverflow(amount);
                }
            }
        }
    }

    private void consumeDailyUpkeep(SimulationCycle cycle) {
        List<WorkDemand> demands = buildUpkeepDemands(cycle);
        allocateInputs(demands);
        for (WorkDemand demand : demands) {
            TownWorkstationState workstation = workstation(demand.type);
            workstation.setProductivityPercent(Math.min(workstation.productivityPercent(), percent(demand.inputFactor)));
            workstation.setShortageFlags(workstation.shortageFlags() | shortageFlags(demand));
        }
        consumeIdleFood(cycle);
    }

    private void consumeIdleFood(SimulationCycle cycle) {
        int idleWorkers = unassignedWorkers();
        if (idleWorkers <= 0) {
            return;
        }
        long needed = cycle.perStep(BREAD_PER_WORKER_PER_DAY) * idleWorkers;
        long consumed = Math.min(raw(ResourceType.BREAD), needed);
        consume(ResourceType.BREAD, consumed);
    }

    private void refreshDailyRates() {
        // Made/Used now report the last completed economic day, not projected rates.
    }

    private int projectedProducedItemsPerDay() {
        int total = 0;
        for (ResourceType resource : ResourceType.values()) {
            total += productionPerDay(resource);
        }
        return total;
    }

    private int courierCapacityPerDay() {
        return whole(COURIER_ITEMS_PER_WORKER_PER_DAY * workstation(WorkstationType.COURIER).workers());
    }

    private void addCarpenterProductionRates(int actions) {
        int remaining = Math.max(0, actions);
        while (remaining > 0) {
            if (productionPerDay(ResourceType.STICK) < consumptionPerDay(ResourceType.STICK)
                    && projectedPlanks() >= STICK_PLANK_COST) {
                productionPerDay.put(ResourceType.STICK, productionPerDay.get(ResourceType.STICK) + (int) STICKS_PER_ACTION);
                consumptionPerDay.put(ResourceType.OAK_PLANKS, consumptionPerDay.get(ResourceType.OAK_PLANKS) + (int) STICK_PLANK_COST);
            } else {
                productionPerDay.put(ResourceType.OAK_PLANKS, productionPerDay.get(ResourceType.OAK_PLANKS) + (int) PLANKS_PER_LOG);
                consumptionPerDay.put(ResourceType.LOG, consumptionPerDay.get(ResourceType.LOG) + 1);
            }
            remaining--;
        }
    }

    private int projectedPlanks() {
        return resource(ResourceType.OAK_PLANKS)
                + productionPerDay(ResourceType.OAK_PLANKS)
                - consumptionPerDay(ResourceType.OAK_PLANKS);
    }

    private void addToolProductionRates(int totalTools) {
        if (totalTools <= 0) {
            return;
        }
        int remaining = totalTools;
        while (remaining > 0) {
            ResourceType target = toolTargetForDailyRates();
            productionPerDay.put(target, productionPerDay.get(target) + 1);
            remaining--;
        }
    }

    private ResourceType toolTargetForDailyRates() {
        ResourceType protectedHammer = protectedHammerTarget((long) (resource(ResourceType.HAMMER) + productionPerDay(ResourceType.HAMMER)) * SCALE);
        if (protectedHammer != null) {
            return protectedHammer;
        }

        ResourceType bestDeficit = null;
        int bestDeficitAmount = 0;
        for (ResourceType tool : toolResources()) {
            int deficit = consumptionPerDay(tool) - productionPerDay(tool);
            if (deficit > bestDeficitAmount) {
                bestDeficitAmount = deficit;
                bestDeficit = tool;
            }
        }
        if (bestDeficit != null) {
            return bestDeficit;
        }

        ResourceType lowest = ResourceType.PICKAXE;
        int lowestAmount = Integer.MAX_VALUE;
        for (ResourceType tool : toolResources()) {
            int amount = resource(tool) + productionPerDay(tool);
            if (amount < lowestAmount) {
                lowestAmount = amount;
                lowest = tool;
            }
        }
        return lowest;
    }

    private int shortageFlags(WorkDemand demand) {
        int flags = 0;
        for (ResourceType resource : ResourceType.values()) {
            long needed = demand.needed(resource);
            if (needed <= 0) {
                continue;
            }
            double factor = demand.allocated(resource) / (double) needed;
            if (factor < 1.0D) {
                flags |= 1 << resource.ordinal();
            }
        }
        return flags;
    }

    private void resetWorkstations() {
        for (TownWorkstationState workstation : workstations.values()) {
            workstation.setProductivityPercent(workstation.workers() > 0 ? 100 : 0);
            workstation.setShortageFlags(0);
        }
    }

    private void clearRates() {
        for (ResourceType resource : ResourceType.values()) {
            productionPerDay.put(resource, 0);
            consumptionPerDay.put(resource, 0);
        }
    }

    private EnumMap<ResourceType, Long> emptyResourceMap() {
        EnumMap<ResourceType, Long> map = new EnumMap<>(ResourceType.class);
        for (ResourceType resource : ResourceType.values()) {
            map.put(resource, 0L);
        }
        return map;
    }

    private long raw(ResourceType resource) {
        return storage.get(resource);
    }

    private void resourceRaw(ResourceType resource, long amount) {
        storage.put(resource, clamp(amount));
    }

    private void add(ResourceType resource, long amount) {
        storage.put(resource, clamp(raw(resource) + Math.max(0, amount)));
    }

    private void consume(ResourceType resource, long amount) {
        storage.put(resource, Math.max(0, raw(resource) - Math.max(0, amount)));
    }

    private static long clamp(long amount) {
        return Math.max(0, Math.min(DEFAULT_CAPACITY, amount));
    }

    private static int percent(double value) {
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, value)) * 100.0D);
    }

    private static long toolsProducedBy(int workers, SimulationCycle cycle) {
        return cycle.perStep(SMITH_TOOLS_PER_WORKER_PER_DAY) * workers;
    }

    private static long breadProducedBy(int workers, SimulationCycle cycle) {
        return cycle.perStep(BAKER_BREAD_PER_WORKER_PER_DAY) * workers;
    }

    private static long carpenterActionsBy(int workers, SimulationCycle cycle) {
        return cycle.perStep(CARPENTER_ACTIONS_PER_WORKER_PER_DAY) * workers;
    }

    private static long courierTransportBy(int workers, SimulationCycle cycle) {
        return cycle.perStep(COURIER_ITEMS_PER_WORKER_PER_DAY) * workers;
    }

    private static long transportedWorkerOutput(int workers, long amountPerWorker, TransportLoad transport) {
        if (workers <= 0 || amountPerWorker <= 0) {
            return 0L;
        }
        int activeWorkers = 0;
        for (int i = 0; i < workers; i++) {
            if ((long) (activeWorkers + 1) * amountPerWorker <= transport.remaining()) {
                activeWorkers++;
            } else {
                transport.markOverflow(amountPerWorker);
            }
        }
        return amountPerWorker * activeWorkers;
    }

    private static int toWhole(long scaled) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(scaled / (double) SCALE)));
    }

    private static int whole(double value) {
        return (int) Math.round(Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)));
    }

    private int workers(WorkstationType type) {
        return workstation(type).workers();
    }

    public void migrateWorkersFromWorkstationCounts() {
        if (townWorkers.isEmpty()) {
            createWorkersFromWorkstationCounts();
        } else {
            syncWorkstationWorkerCounts();
        }
        refreshDailyRates();
    }

    public void migrateStockpileGoals() {
        for (ResourceType resource : ResourceType.values()) {
            stockpileGoals.putIfAbsent(resource, DEFAULT_STOCKPILE_GOAL);
            if (stockpileGoals.get(resource) <= 0) {
                stockpileGoals.put(resource, DEFAULT_STOCKPILE_GOAL);
            }
        }
    }

    private void createWorkersFromWorkstationCounts() {
        townWorkers.clear();
        nextWorkerId = 1;
        int assigned = 0;
        for (WorkstationType type : WorkstationType.values()) {
            int count = workstation(type).workers();
            for (int i = 0; i < count; i++) {
                addWorker(type);
                assigned++;
            }
        }
        int unassigned = Math.max(0, totalWorkers - assigned);
        for (int i = 0; i < unassigned; i++) {
            addWorker(null);
        }
        totalWorkers = townWorkers.size();
        syncWorkstationWorkerCounts();
    }

    private TownWorker addWorker(WorkstationType assignment) {
        TownWorker worker = new TownWorker(nextWorkerId++, assignment);
        townWorkers.add(worker);
        return worker;
    }

    private void syncWorkstationWorkerCounts() {
        for (TownWorkstationState workstation : workstations.values()) {
            workstation.setWorkers(0);
        }
        int highestId = 0;
        for (TownWorker worker : townWorkers) {
            highestId = Math.max(highestId, worker.id());
            WorkstationType assignment = worker.assignment();
            if (assignment != null) {
                TownWorkstationState workstation = workstation(assignment);
                workstation.setWorkers(workstation.workers() + 1);
            }
        }
        totalWorkers = townWorkers.size();
        nextWorkerId = Math.max(nextWorkerId, highestId + 1);
    }

    private TownWorker worker(int workerId) {
        for (TownWorker worker : townWorkers) {
            if (worker.id() == workerId) {
                return worker;
            }
        }
        return null;
    }

    private TownWorker firstUnassignedWorker() {
        for (TownWorker worker : townWorkers) {
            if (worker.isUnassigned()) {
                return worker;
            }
        }
        return null;
    }

    private TownWorker lastWorkerAssignedTo(WorkstationType type) {
        for (int i = townWorkers.size() - 1; i >= 0; i--) {
            TownWorker worker = townWorkers.get(i);
            if (worker.assignment() == type) {
                return worker;
            }
        }
        return null;
    }

    public record SimulationCycle(int dayTicks, int nightTicks) {
        public static SimulationCycle ofSeconds(int daySeconds, int nightSeconds) {
            int day = Math.max(1, daySeconds * TICKS_PER_SECOND);
            int night = Math.max(0, nightSeconds * TICKS_PER_SECOND);
            return new SimulationCycle(day, night);
        }

        private boolean isDaytime(long gameTime) {
            int totalTicks = Math.max(1, dayTicks + nightTicks);
            long cycleTime = Math.floorMod(gameTime, totalTicks);
            return cycleTime < dayTicks;
        }

        private long perStep(double amountPerDay) {
            return Math.round(amountPerDay * SCALE);
        }

        private long stepTicks() {
            return Math.max(1, dayTicks + nightTicks);
        }

        private long cycleIndex(long gameTime) {
            return Math.floorDiv(gameTime, stepTicks());
        }
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id);
        nbt.putString("name", name);
        nbt.putInt("totalWorkers", totalWorkers);
        nbt.putLong("lastSimulatedGameTime", lastSimulatedGameTime);
        nbt.putInt("nextWorkerId", nextWorkerId);

        NbtCompound storageNbt = new NbtCompound();
        for (Map.Entry<ResourceType, Long> entry : storage.entrySet()) {
            storageNbt.putLong(entry.getKey().id(), entry.getValue());
        }
        nbt.put("storage", storageNbt);

        NbtCompound productionNbt = new NbtCompound();
        NbtCompound consumptionNbt = new NbtCompound();
        NbtCompound stockpileGoalNbt = new NbtCompound();
        for (ResourceType resource : ResourceType.values()) {
            productionNbt.putInt(resource.id(), productionPerDay.get(resource));
            consumptionNbt.putInt(resource.id(), consumptionPerDay.get(resource));
            stockpileGoalNbt.putInt(resource.id(), stockpileGoals.get(resource));
        }
        nbt.put("productionPerDay", productionNbt);
        nbt.put("consumptionPerDay", consumptionNbt);
        nbt.put("stockpileGoals", stockpileGoalNbt);

        NbtList workstationList = new NbtList();
        for (TownWorkstationState workstation : workstations.values()) {
            workstationList.add(workstation.writeNbt());
        }
        nbt.put("workstations", workstationList);

        NbtList workerList = new NbtList();
        for (TownWorker worker : townWorkers) {
            workerList.add(worker.writeNbt());
        }
        nbt.put("workers", workerList);
        return nbt;
    }

    public static TownState readNbt(NbtCompound nbt) {
        TownState town = new TownState(nbt.getString("id"), nbt.getString("name"));
        town.totalWorkers = nbt.getInt("totalWorkers");
        town.lastSimulatedGameTime = nbt.getLong("lastSimulatedGameTime");
        town.nextWorkerId = Math.max(1, nbt.getInt("nextWorkerId"));

        if (nbt.contains("storage")) {
            NbtCompound storageNbt = nbt.getCompound("storage");
            for (ResourceType resource : ResourceType.values()) {
                town.resourceRaw(resource, storageNbt.getLong(resource.id()));
            }
        } else {
            NbtCompound oldResources = nbt.getCompound("resources");
            town.resourceRaw(ResourceType.WHEAT, oldResources.getLong("food"));
            town.resourceRaw(ResourceType.IRON, oldResources.getLong("raw_iron"));
            town.resourceRaw(ResourceType.OAK_PLANKS, oldResources.getLong("wood"));
            long tools = oldResources.getLong("tools");
            town.resourceRaw(ResourceType.PICKAXE, tools);
            town.resourceRaw(ResourceType.AXE, tools);
            town.resourceRaw(ResourceType.HOE, tools);
            town.resourceRaw(ResourceType.SAW, tools);
            town.resourceRaw(ResourceType.HAMMER, tools);
        }

        NbtCompound productionNbt = nbt.getCompound("productionPerDay");
        NbtCompound consumptionNbt = nbt.getCompound("consumptionPerDay");
        NbtCompound stockpileGoalNbt = nbt.getCompound("stockpileGoals");
        for (ResourceType resource : ResourceType.values()) {
            town.productionPerDay.put(resource, productionNbt.getInt(resource.id()));
            town.consumptionPerDay.put(resource, consumptionNbt.getInt(resource.id()));
            if (stockpileGoalNbt.contains(resource.id())) {
                town.stockpileGoals.put(resource, Math.max(1, stockpileGoalNbt.getInt(resource.id())));
            }
        }

        NbtList workstationList = nbt.getList("workstations", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < workstationList.size(); i++) {
            TownWorkstationState workstation = TownWorkstationState.readNbt(workstationList.getCompound(i));
            town.workstations.put(workstation.type(), workstation);
        }
        if (nbt.contains("workers")) {
            NbtList workerList = nbt.getList("workers", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < workerList.size(); i++) {
                town.townWorkers.add(TownWorker.readNbt(workerList.getCompound(i)));
            }
            town.syncWorkstationWorkerCounts();
        } else {
            town.createWorkersFromWorkstationCounts();
        }
        town.refreshDailyRates();
        return town;
    }

    private final class DailyWorkPlan {
        private final EnumMap<ResourceType, Long> produced = emptyResourceMap();
        private final EnumMap<ResourceType, Long> consumed = emptyResourceMap();
        private final EnumMap<WorkstationType, Integer> availableWorkers = new EnumMap<>(WorkstationType.class);
        private final EnumMap<WorkstationType, Integer> assignedWorkers = new EnumMap<>(WorkstationType.class);

        private DailyWorkPlan() {
            for (WorkstationType type : WorkstationType.values()) {
                availableWorkers.put(type, workstation(type).workers());
                assignedWorkers.put(type, 0);
            }
        }
    }

    private record ProductionRecipe(TownWorker worker, WorkstationType workstation, long output,
                                    List<RecipeInput> inputs, ResourceType tool, int toolDurabilityUsed) {}

    private record ToolPlan(ResourceType tool, int outputCapacity) {}

    private record RecipeInput(ResourceType resource, long amount) {}

    private static final class WorkDemand {
        private final WorkstationType type;
        private final int workers;
        private final int priority;
        private final EnumMap<ResourceType, Long> needed = new EnumMap<>(ResourceType.class);
        private final EnumMap<ResourceType, Long> allocated = new EnumMap<>(ResourceType.class);
        private double inputFactor = 1.0D;

        private WorkDemand(WorkstationType type, int workers, int priority) {
            this.type = type;
            this.workers = workers;
            this.priority = priority;
            for (ResourceType resource : ResourceType.values()) {
                needed.put(resource, 0L);
                allocated.put(resource, 0L);
            }
        }

        private void add(ResourceType resource, long amount) {
            needed.put(resource, needed(resource) + amount);
        }

        private long needed(ResourceType resource) {
            return needed.get(resource);
        }

        private long allocated(ResourceType resource) {
            return allocated.get(resource);
        }

        private void setAllocated(ResourceType resource, long amount) {
            allocated.put(resource, Math.max(0, amount));
        }

        private void calculateInputFactor() {
            double factor = 1.0D;
            for (ResourceType resource : ResourceType.values()) {
                long need = needed(resource);
                if (need > 0) {
                    factor = Math.min(factor, allocated(resource) / (double) need);
                }
            }
            inputFactor = factor;
        }
    }

    private static final class TransportLoad {
        private final long capacity;
        private long used;
        private long overflow;

        private TransportLoad(long capacity) {
            this.capacity = Math.max(0, capacity);
        }

        private boolean canMove(long amount) {
            return amount <= Math.max(0, capacity - used);
        }

        private boolean tryMove(long amount) {
            long requested = Math.max(0, amount);
            if (!canMove(requested)) {
                return false;
            }
            used += requested;
            return true;
        }

        private void markOverflow(long amount) {
            overflow += Math.max(0, amount);
        }

        private long capacity() {
            return capacity;
        }

        private long remaining() {
            return Math.max(0, capacity - used);
        }

        private int productivityPercent() {
            long requested = used + overflow;
            if (requested <= 0) {
                return capacity > 0 ? 100 : 0;
            }
            return percent(capacity / (double) requested);
        }
    }
}
