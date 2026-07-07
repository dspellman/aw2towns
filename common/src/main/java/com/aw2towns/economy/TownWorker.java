package com.aw2towns.economy;

import java.util.EnumMap;
import net.minecraft.nbt.NbtCompound;

public final class TownWorker {

    private static final String UNASSIGNED = "unassigned";
    public static final int DEFAULT_TOOL_DURABILITY = 25;

    private final int id;
    private final EnumMap<ResourceType, Integer> toolDurability = new EnumMap<>(ResourceType.class);
    private WorkstationType assignment;

    public TownWorker(int id, WorkstationType assignment) {
        this.id = Math.max(1, id);
        this.assignment = assignment;
        for (ResourceType resource : ResourceType.values()) {
            toolDurability.put(resource, 0);
        }
    }

    public int id() {
        return id;
    }

    public WorkstationType assignment() {
        return assignment;
    }

    public void assign(WorkstationType assignment) {
        this.assignment = assignment;
    }

    public boolean isUnassigned() {
        return assignment == null;
    }

    public int toolDurability(ResourceType tool) {
        return tool == null ? 0 : toolDurability.getOrDefault(tool, 0);
    }

    public void setToolDurability(ResourceType tool, int durability) {
        if (tool != null) {
            toolDurability.put(tool, Math.max(0, durability));
        }
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("id", id);
        nbt.putString("assignment", assignment == null ? UNASSIGNED : assignment.id());
        NbtCompound tools = new NbtCompound();
        for (ResourceType resource : ResourceType.values()) {
            int durability = toolDurability(resource);
            if (durability > 0) {
                tools.putInt(resource.id(), durability);
            }
        }
        nbt.put("toolDurability", tools);
        return nbt;
    }

    public static TownWorker readNbt(NbtCompound nbt) {
        String assignmentId = nbt.getString("assignment");
        WorkstationType assignment = assignmentId.isEmpty() || UNASSIGNED.equals(assignmentId)
                ? null
                : WorkstationType.byId(assignmentId);
        TownWorker worker = new TownWorker(nbt.getInt("id"), assignment);
        if (nbt.contains("toolDurability")) {
            NbtCompound tools = nbt.getCompound("toolDurability");
            for (ResourceType resource : ResourceType.values()) {
                if (tools.contains(resource.id())) {
                    worker.setToolDurability(resource, tools.getInt(resource.id()));
                }
            }
        }
        return worker;
    }
}
