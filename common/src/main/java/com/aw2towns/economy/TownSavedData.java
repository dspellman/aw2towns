package com.aw2towns.economy;

import com.aw2towns.AW2Towns;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public final class TownSavedData extends PersistentState {

    private static final String NAME = AW2Towns.MOD_ID + "_towns";
    private static final int DATA_VERSION = 11;
    private static final Type<TownSavedData> TYPE = new Type<>(
            TownSavedData::new,
            TownSavedData::readNbt,
            DataFixTypes.LEVEL);

    private final List<TownState> towns = new ArrayList<>();
    private int nextTownIndex;

    public static TownSavedData get(ServerWorld world) {
        ServerWorld overworld = world.getServer().getWorld(World.OVERWORLD);
        ServerWorld storageWorld = overworld == null ? world : overworld;
        return storageWorld.getPersistentStateManager().getOrCreate(TYPE, NAME);
    }

    public List<TownState> towns() {
        return Collections.unmodifiableList(towns);
    }

    public TownState ensureStarterTown(long gameTime) {
        if (towns.isEmpty()) {
            towns.add(TownState.starter(gameTime));
            markDirty();
        }
        return towns.get(0);
    }

    public TownState firstTown(long gameTime) {
        return ensureStarterTown(gameTime);
    }

    public void tickRoundRobin(long gameTime, TownState.SimulationCycle cycle) {
        if (towns.isEmpty()) {
            ensureStarterTown(gameTime);
            return;
        }
        if (nextTownIndex >= towns.size()) {
            nextTownIndex = 0;
        }
        TownState town = towns.get(nextTownIndex);
        town.simulateUntil(gameTime, cycle);
        nextTownIndex = (nextTownIndex + 1) % towns.size();
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("dataVersion", DATA_VERSION);
        nbt.putInt("nextTownIndex", nextTownIndex);
        NbtList townList = new NbtList();
        for (TownState town : towns) {
            townList.add(town.writeNbt());
        }
        nbt.put("towns", townList);
        return nbt;
    }

    private static TownSavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        TownSavedData data = new TownSavedData();
        int dataVersion = nbt.getInt("dataVersion");
        data.nextTownIndex = nbt.getInt("nextTownIndex");
        NbtList townList = nbt.getList("towns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < townList.size(); i++) {
            data.towns.add(TownState.readNbt(townList.getCompound(i)));
        }
        if (dataVersion < 7) {
            for (TownState town : data.towns) {
                town.resetPrototypeEconomy();
            }
            data.markDirty();
        }
        if (dataVersion < 8) {
            for (TownState town : data.towns) {
                town.migrateWorkersFromWorkstationCounts();
            }
            data.markDirty();
        }
        if (dataVersion < 11) {
            for (TownState town : data.towns) {
                town.migrateStockpileGoals();
            }
            data.markDirty();
        }
        return data;
    }
}
