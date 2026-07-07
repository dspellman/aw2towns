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
    public static final int STEP_TICKS = 100;
    public static final int STATUS_BLOCKED_MAX = 30;
    public static final int STATUS_PARTIAL_MAX = 89;
    public static final int LARGE_STOCKPILE_DAYS = 2;

    private static final int TICKS_PER_SECOND = 20;
    private static final long DEFAULT_CAPACITY = 1_000_000_000L * SCALE;
    private static final double WHEAT_PER_WORKER_PER_DAY = 3.0D;
    private static final double TOOL_PER_WORKER_PER_DAY = 1.0D;
    private static final double MINE_IRON_PER_WORKER_PER_DAY = 15.0D;
    private static final double LUMBER_PLANKS_PER_WORKER_PER_DAY = 15.0D;
    private static final double FARM_WHEAT_PER_WORKER_PER_DAY = 15.0D;
    private static final double SMITH_TOOLS_PER_WORKER_PER_DAY = 10.0D / 3.0D;
    private static final long TOOL_IRON_COST = 3L;
    private static final long TOOL_PLANKS_COST = 3L;

    private final String id;
    private String name;
    private int totalWorkers;
    private long lastSimulatedGameTime;
    private final EnumMap<ResourceType, Long> storage = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> productionPerDay = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> consumptionPerDay = new EnumMap<>(ResourceType.class);
    private final EnumMap<WorkstationType, TownWorkstationState> workstations = new EnumMap<>(WorkstationType.class);

    public TownState(String id, String name) {
        this.id = id;
        this.name = name;
        for (ResourceType resource : ResourceType.values()) {
            storage.put(resource, 0L);
            productionPerDay.put(resource, 0);
            consumptionPerDay.put(resource, 0);
        }
        for (WorkstationType type : WorkstationType.values()) {
            workstations.put(type, new TownWorkstationState(type, 0));
        }
    }

    public static TownState starter(long gameTime) {
        TownState town = new TownState("starter", "Prototype Town");
        town.totalWorkers = 12;
        town.workstation(WorkstationType.FARM).setWorkers(3);
        town.workstation(WorkstationType.MINE).setWorkers(3);
        town.workstation(WorkstationType.LUMBER_MILL).setWorkers(2);
        town.workstation(WorkstationType.BLACKSMITH).setWorkers(2);
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
        return totalWorkers;
    }

    public int assignedWorkers() {
        int assigned = 0;
        for (TownWorkstationState workstation : workstations.values()) {
            assigned += workstation.workers();
        }
        return assigned;
    }

    public int unassignedWorkers() {
        return Math.max(0, totalWorkers - assignedWorkers());
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

    public TownWorkstationState workstation(WorkstationType type) {
        return workstations.get(type);
    }

    public void adjustWorkers(WorkstationType type, int delta) {
        TownWorkstationState workstation = workstation(type);
        if (delta > 0 && unassignedWorkers() <= 0) {
            return;
        }
        workstation.setWorkers(workstation.workers() + delta);
        refreshDailyRates();
    }

    public void adjustPriority(WorkstationType type, int delta) {
        workstation(type).adjustPriority(delta);
    }

    public void resetPrototypeEconomy() {
        totalWorkers = 12;
        for (ResourceType resource : ResourceType.values()) {
            resourceRaw(resource, 0L);
        }
        workstation(WorkstationType.FARM).setWorkers(3);
        workstation(WorkstationType.MINE).setWorkers(3);
        workstation(WorkstationType.LUMBER_MILL).setWorkers(2);
        workstation(WorkstationType.BLACKSMITH).setWorkers(2);
        refreshDailyRates();
    }

    public void simulateUntil(long gameTime, SimulationCycle cycle) {
        if (lastSimulatedGameTime <= 0) {
            lastSimulatedGameTime = gameTime;
            return;
        }
        int steps = 0;
        while (gameTime - lastSimulatedGameTime >= STEP_TICKS && steps < 24) {
            simulateStep(lastSimulatedGameTime, cycle);
            lastSimulatedGameTime += STEP_TICKS;
            steps++;
        }
        if (gameTime - lastSimulatedGameTime > STEP_TICKS * 24L) {
            lastSimulatedGameTime = gameTime;
        }
    }

    private void simulateStep(long gameTime, SimulationCycle cycle) {
        refreshDailyRates();
        autoAssignIdleWorkers();
        refreshDailyRates();
        if (!cycle.isDaytime(gameTime)) {
            return;
        }
        resetWorkstations();

        EnumMap<ResourceType, Long> produced = emptyResourceMap();
        produceFor(WorkstationType.FARM, workstation(WorkstationType.FARM).workers(), 1.0D, produced, cycle);
        produceFor(WorkstationType.MINE, workstation(WorkstationType.MINE).workers(), 1.0D, produced, cycle);
        produceFor(WorkstationType.LUMBER_MILL, workstation(WorkstationType.LUMBER_MILL).workers(), 1.0D, produced, cycle);
        addProducedToStorage(produced);

        WorkDemand smithDemand = buildBlacksmithDemand(cycle);
        if (smithDemand != null) {
            allocateInputs(List.of(smithDemand));
            int productivity = percent(smithDemand.inputFactor);
            TownWorkstationState blacksmith = workstation(WorkstationType.BLACKSMITH);
            blacksmith.setProductivityPercent(productivity);
            blacksmith.setShortageFlags(shortageFlags(smithDemand));
            EnumMap<ResourceType, Long> smithProduced = emptyResourceMap();
            produceFor(WorkstationType.BLACKSMITH, smithDemand.workers, smithDemand.inputFactor, smithProduced, cycle);
            addProducedToStorage(smithProduced);
        }

        consumeDailyUpkeep(cycle);
    }

    private WorkDemand buildBlacksmithDemand(SimulationCycle cycle) {
        TownWorkstationState workstation = workstation(WorkstationType.BLACKSMITH);
        if (workstation.workers() <= 0) {
            return null;
        }
        long tools = toolsProducedBy(workstation.workers(), cycle);
        WorkDemand demand = new WorkDemand(WorkstationType.BLACKSMITH, workstation.workers(), workstation.priority());
        demand.add(ResourceType.IRON, tools * TOOL_IRON_COST);
        demand.add(ResourceType.OAK_PLANKS, tools * TOOL_PLANKS_COST);
        return demand;
    }

    private List<WorkDemand> buildUpkeepDemands(SimulationCycle cycle) {
        List<WorkDemand> demands = new ArrayList<>();
        long wheat = cycle.perStep(WHEAT_PER_WORKER_PER_DAY);
        long tool = cycle.perStep(TOOL_PER_WORKER_PER_DAY);
        addDemand(demands, WorkstationType.FARM, ResourceType.WHEAT, wheat, ResourceType.HOE, tool);
        addDemand(demands, WorkstationType.MINE, ResourceType.WHEAT, wheat, ResourceType.PICKAXE, tool);
        addDemand(demands, WorkstationType.LUMBER_MILL, ResourceType.WHEAT, wheat, ResourceType.AXE, tool);
        addDemand(demands, WorkstationType.BLACKSMITH, ResourceType.WHEAT, wheat, ResourceType.SWORD, tool);
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
        demand.add(equipment, equipmentPerWorker * workstation.workers());
        demands.add(demand);
        return demand;
    }

    private void allocateInputs(List<WorkDemand> demands) {
        for (ResourceType resource : ResourceType.values()) {
            List<WorkDemand> needingResource = demands.stream()
                    .filter(demand -> demand.needed(resource) > 0)
                    .sorted(Comparator.comparingInt(WorkDemand::priority).reversed())
                    .toList();
            long available = raw(resource);
            int index = 0;
            while (index < needingResource.size()) {
                int priority = needingResource.get(index).priority();
                List<WorkDemand> group = new ArrayList<>();
                while (index < needingResource.size() && needingResource.get(index).priority() == priority) {
                    group.add(needingResource.get(index));
                    index++;
                }

                long groupNeed = 0;
                for (WorkDemand demand : group) {
                    groupNeed += demand.needed(resource);
                }
                if (groupNeed <= 0) {
                    continue;
                }

                double factor = Math.min(1.0D, available / (double) groupNeed);
                long consumedByGroup = 0;
                for (WorkDemand demand : group) {
                    long consumed = Math.round(demand.needed(resource) * factor);
                    demand.setAllocated(resource, consumed);
                    consumedByGroup += consumed;
                }
                consume(resource, Math.min(available, consumedByGroup));
                available = raw(resource);
                if (available <= 0) {
                    while (index < needingResource.size()) {
                        needingResource.get(index).setAllocated(resource, 0);
                        index++;
                    }
                    break;
                }
            }
        }

        for (WorkDemand demand : demands) {
            demand.calculateInputFactor();
        }
    }

    private void produceFor(WorkstationType type, int workers, double factor, EnumMap<ResourceType, Long> produced, SimulationCycle cycle) {
        switch (type) {
            case FARM -> addProduced(produced, ResourceType.WHEAT, cycle.perStep(FARM_WHEAT_PER_WORKER_PER_DAY) * workers, factor);
            case MINE -> addProduced(produced, ResourceType.IRON, cycle.perStep(MINE_IRON_PER_WORKER_PER_DAY) * workers, factor);
            case LUMBER_MILL -> addProduced(produced, ResourceType.OAK_PLANKS, cycle.perStep(LUMBER_PLANKS_PER_WORKER_PER_DAY) * workers, factor);
            case BLACKSMITH -> addProducedTools(produced, Math.round(toolsProducedBy(workers, cycle) * factor));
        }
    }

    private void addProducedTools(EnumMap<ResourceType, Long> produced, long totalTools) {
        int toolWorkers = assignedWorkers();
        if (toolWorkers <= 0 || totalTools <= 0) {
            return;
        }

        long pickaxes = Math.round(totalTools * workstation(WorkstationType.MINE).workers() / (double) toolWorkers);
        long axes = Math.round(totalTools * workstation(WorkstationType.LUMBER_MILL).workers() / (double) toolWorkers);
        long hoes = Math.round(totalTools * workstation(WorkstationType.FARM).workers() / (double) toolWorkers);
        long swords = Math.max(0, totalTools - pickaxes - axes - hoes);

        produced.put(ResourceType.PICKAXE, produced.get(ResourceType.PICKAXE) + pickaxes);
        produced.put(ResourceType.AXE, produced.get(ResourceType.AXE) + axes);
        produced.put(ResourceType.HOE, produced.get(ResourceType.HOE) + hoes);
        produced.put(ResourceType.SWORD, produced.get(ResourceType.SWORD) + swords);
    }

    private void addProduced(EnumMap<ResourceType, Long> produced, ResourceType resource, long base, double factor) {
        produced.put(resource, produced.get(resource) + Math.round(base * factor));
    }

    private void addProducedToStorage(EnumMap<ResourceType, Long> produced) {
        for (ResourceType resource : ResourceType.values()) {
            long amount = produced.get(resource);
            if (amount > 0) {
                add(resource, amount);
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

    private void autoAssignIdleWorkers() {
        while (unassignedWorkers() > 0) {
            WorkstationType target = bestIdleWorkerTarget();
            if (target == null) {
                return;
            }
            TownWorkstationState workstation = workstation(target);
            workstation.setWorkers(workstation.workers() + 1);
            refreshDailyRates();
        }
    }

    private WorkstationType bestIdleWorkerTarget() {
        EnumMap<WorkstationType, Double> needs = workstationDeficitNeeds();
        WorkstationType bestDeficit = null;
        double bestNeed = 0.0D;
        for (WorkstationType type : WorkstationType.values()) {
            double need = needs.get(type);
            if (need > bestNeed || need == bestNeed && bestDeficit != null
                    && workstation(type).priority() > workstation(bestDeficit).priority()) {
                bestNeed = need;
                bestDeficit = type;
            }
        }
        if (bestDeficit != null && bestNeed > 0.0D) {
            return bestDeficit;
        }

        WorkstationType highestPriority = null;
        for (WorkstationType type : WorkstationType.values()) {
            if (highestPriority == null
                    || workstation(type).priority() > workstation(highestPriority).priority()
                    || workstation(type).priority() == workstation(highestPriority).priority()
                    && workstation(type).workers() < workstation(highestPriority).workers()) {
                highestPriority = type;
            }
        }
        return highestPriority;
    }

    private EnumMap<WorkstationType, Double> workstationDeficitNeeds() {
        EnumMap<WorkstationType, Double> needs = new EnumMap<>(WorkstationType.class);
        for (WorkstationType type : WorkstationType.values()) {
            needs.put(type, 0.0D);
        }

        addDeficitNeed(needs, WorkstationType.FARM, ResourceType.WHEAT, FARM_WHEAT_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.MINE, ResourceType.IRON, MINE_IRON_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.LUMBER_MILL, ResourceType.OAK_PLANKS, LUMBER_PLANKS_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.BLACKSMITH, ResourceType.PICKAXE, SMITH_TOOLS_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.BLACKSMITH, ResourceType.AXE, SMITH_TOOLS_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.BLACKSMITH, ResourceType.HOE, SMITH_TOOLS_PER_WORKER_PER_DAY);
        addDeficitNeed(needs, WorkstationType.BLACKSMITH, ResourceType.SWORD, SMITH_TOOLS_PER_WORKER_PER_DAY);
        return needs;
    }

    private void addDeficitNeed(EnumMap<WorkstationType, Double> needs, WorkstationType type,
                                ResourceType resource, double outputPerWorkerPerDay) {
        int deficit = consumptionPerDay(resource) - productionPerDay(resource);
        if (deficit <= 0 || outputPerWorkerPerDay <= 0.0D) {
            return;
        }
        needs.put(type, needs.get(type) + deficit / outputPerWorkerPerDay);
    }

    private void consumeIdleFood(SimulationCycle cycle) {
        int idleWorkers = unassignedWorkers();
        if (idleWorkers <= 0) {
            return;
        }
        long needed = cycle.perStep(WHEAT_PER_WORKER_PER_DAY) * idleWorkers;
        long consumed = Math.min(raw(ResourceType.WHEAT), needed);
        consume(ResourceType.WHEAT, consumed);
    }

    private void refreshDailyRates() {
        clearRates();
        productionPerDay.put(ResourceType.WHEAT, whole(FARM_WHEAT_PER_WORKER_PER_DAY * workstation(WorkstationType.FARM).workers()));
        productionPerDay.put(ResourceType.IRON, whole(MINE_IRON_PER_WORKER_PER_DAY * workstation(WorkstationType.MINE).workers()));
        productionPerDay.put(ResourceType.OAK_PLANKS, whole(LUMBER_PLANKS_PER_WORKER_PER_DAY * workstation(WorkstationType.LUMBER_MILL).workers()));

        int toolsPerDay = whole(SMITH_TOOLS_PER_WORKER_PER_DAY * workstation(WorkstationType.BLACKSMITH).workers());
        addToolProductionRates(toolsPerDay);
        consumptionPerDay.put(ResourceType.WHEAT, whole(totalWorkers * WHEAT_PER_WORKER_PER_DAY));
        consumptionPerDay.put(ResourceType.IRON, (int) (toolsPerDay * TOOL_IRON_COST));
        consumptionPerDay.put(ResourceType.OAK_PLANKS, (int) (toolsPerDay * TOOL_PLANKS_COST));
        consumptionPerDay.put(ResourceType.PICKAXE, whole(workstation(WorkstationType.MINE).workers() * TOOL_PER_WORKER_PER_DAY));
        consumptionPerDay.put(ResourceType.AXE, whole(workstation(WorkstationType.LUMBER_MILL).workers() * TOOL_PER_WORKER_PER_DAY));
        consumptionPerDay.put(ResourceType.HOE, whole(workstation(WorkstationType.FARM).workers() * TOOL_PER_WORKER_PER_DAY));
        consumptionPerDay.put(ResourceType.SWORD, whole(workstation(WorkstationType.BLACKSMITH).workers() * TOOL_PER_WORKER_PER_DAY));
    }

    private void addToolProductionRates(int totalTools) {
        int toolWorkers = assignedWorkers();
        if (toolWorkers <= 0 || totalTools <= 0) {
            return;
        }
        int pickaxes = (int) Math.round(totalTools * workstation(WorkstationType.MINE).workers() / (double) toolWorkers);
        int axes = (int) Math.round(totalTools * workstation(WorkstationType.LUMBER_MILL).workers() / (double) toolWorkers);
        int hoes = (int) Math.round(totalTools * workstation(WorkstationType.FARM).workers() / (double) toolWorkers);
        int swords = Math.max(0, totalTools - pickaxes - axes - hoes);

        productionPerDay.put(ResourceType.PICKAXE, pickaxes);
        productionPerDay.put(ResourceType.AXE, axes);
        productionPerDay.put(ResourceType.HOE, hoes);
        productionPerDay.put(ResourceType.SWORD, swords);
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

    private static int toWhole(long scaled) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(scaled / (double) SCALE)));
    }

    private static int whole(double value) {
        return (int) Math.round(Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)));
    }

    public record SimulationCycle(int dayTicks, int nightTicks) {
        public static SimulationCycle ofSeconds(int daySeconds, int nightSeconds) {
            int day = Math.max(STEP_TICKS, daySeconds * TICKS_PER_SECOND);
            int night = Math.max(0, nightSeconds * TICKS_PER_SECOND);
            return new SimulationCycle(day, night);
        }

        private boolean isDaytime(long gameTime) {
            int totalTicks = Math.max(STEP_TICKS, dayTicks + nightTicks);
            long cycleTime = Math.floorMod(gameTime, totalTicks);
            return cycleTime < dayTicks;
        }

        private long perStep(double amountPerDay) {
            return Math.round(amountPerDay * SCALE / activeSteps());
        }

        private int activeSteps() {
            return Math.max(1, (dayTicks + STEP_TICKS - 1) / STEP_TICKS);
        }
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id);
        nbt.putString("name", name);
        nbt.putInt("totalWorkers", totalWorkers);
        nbt.putLong("lastSimulatedGameTime", lastSimulatedGameTime);

        NbtCompound storageNbt = new NbtCompound();
        for (Map.Entry<ResourceType, Long> entry : storage.entrySet()) {
            storageNbt.putLong(entry.getKey().id(), entry.getValue());
        }
        nbt.put("storage", storageNbt);

        NbtCompound productionNbt = new NbtCompound();
        NbtCompound consumptionNbt = new NbtCompound();
        for (ResourceType resource : ResourceType.values()) {
            productionNbt.putInt(resource.id(), productionPerDay.get(resource));
            consumptionNbt.putInt(resource.id(), consumptionPerDay.get(resource));
        }
        nbt.put("productionPerDay", productionNbt);
        nbt.put("consumptionPerDay", consumptionNbt);

        NbtList workstationList = new NbtList();
        for (TownWorkstationState workstation : workstations.values()) {
            workstationList.add(workstation.writeNbt());
        }
        nbt.put("workstations", workstationList);
        return nbt;
    }

    public static TownState readNbt(NbtCompound nbt) {
        TownState town = new TownState(nbt.getString("id"), nbt.getString("name"));
        town.totalWorkers = nbt.getInt("totalWorkers");
        town.lastSimulatedGameTime = nbt.getLong("lastSimulatedGameTime");

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
            town.resourceRaw(ResourceType.SWORD, tools);
        }

        NbtCompound productionNbt = nbt.getCompound("productionPerDay");
        NbtCompound consumptionNbt = nbt.getCompound("consumptionPerDay");
        for (ResourceType resource : ResourceType.values()) {
            town.productionPerDay.put(resource, productionNbt.getInt(resource.id()));
            town.consumptionPerDay.put(resource, consumptionNbt.getInt(resource.id()));
        }

        NbtList workstationList = nbt.getList("workstations", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < workstationList.size(); i++) {
            TownWorkstationState workstation = TownWorkstationState.readNbt(workstationList.getCompound(i));
            town.workstations.put(workstation.type(), workstation);
        }
        town.refreshDailyRates();
        return town;
    }

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

        private int priority() {
            return priority;
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
}
