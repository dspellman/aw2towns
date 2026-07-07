package com.aw2towns.economy;

import net.minecraft.nbt.NbtCompound;

public final class TownWorkstationState {

    private final WorkstationType type;
    private int workers;
    private int priority = 5;
    private int productivityPercent;
    private int shortageFlags;

    public TownWorkstationState(WorkstationType type, int workers) {
        this.type = type;
        this.workers = Math.max(0, workers);
        this.productivityPercent = 100;
    }

    public WorkstationType type() {
        return type;
    }

    public int workers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = Math.max(0, workers);
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.max(0, Math.min(9, priority));
    }

    public void adjustPriority(int delta) {
        setPriority(priority + delta);
    }

    public int productivityPercent() {
        return productivityPercent;
    }

    public void setProductivityPercent(int productivityPercent) {
        this.productivityPercent = Math.max(0, Math.min(100, productivityPercent));
    }

    public int shortageFlags() {
        return shortageFlags;
    }

    public void setShortageFlags(int shortageFlags) {
        this.shortageFlags = shortageFlags;
    }

    public NbtCompound writeNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("type", type.id());
        nbt.putInt("workers", workers);
        nbt.putInt("priority", priority);
        nbt.putInt("productivityPercent", productivityPercent);
        nbt.putInt("shortageFlags", shortageFlags);
        return nbt;
    }

    public static TownWorkstationState readNbt(NbtCompound nbt) {
        TownWorkstationState state = new TownWorkstationState(
                WorkstationType.byId(nbt.getString("type")),
                nbt.getInt("workers"));
        if (nbt.contains("priority")) {
            state.setPriority(nbt.getInt("priority"));
        }
        state.setProductivityPercent(nbt.getInt("productivityPercent"));
        state.setShortageFlags(nbt.getInt("shortageFlags"));
        return state;
    }
}
