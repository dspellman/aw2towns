package com.aw2towns.economy;

import net.minecraft.nbt.NbtCompound;

public final class TownWorker {

    private static final String UNASSIGNED = "unassigned";

    private final int id;
    private WorkstationType assignment;

    public TownWorker(int id, WorkstationType assignment) {
        this.id = Math.max(1, id);
        this.assignment = assignment;
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

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("id", id);
        nbt.putString("assignment", assignment == null ? UNASSIGNED : assignment.id());
        return nbt;
    }

    public static TownWorker readNbt(NbtCompound nbt) {
        String assignmentId = nbt.getString("assignment");
        WorkstationType assignment = assignmentId.isEmpty() || UNASSIGNED.equals(assignmentId)
                ? null
                : WorkstationType.byId(assignmentId);
        return new TownWorker(nbt.getInt("id"), assignment);
    }
}
