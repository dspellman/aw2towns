package com.aw2towns.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class TownSimulationManager {

    private static final int ROUND_ROBIN_INTERVAL_TICKS = 5;
    private static final int DEFAULT_CYCLE_SECONDS = 20;
    private static final int MIN_CYCLE_SECONDS = 5;
    private static final int MAX_CYCLE_SECONDS = 600;
    private static final int CYCLE_STEP_SECONDS = 5;
    private static int cycleSeconds = DEFAULT_CYCLE_SECONDS;

    private TownSimulationManager() {}

    public static void onServerTick(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            return;
        }
        long gameTime = overworld.getTime();
        if (gameTime % ROUND_ROBIN_INTERVAL_TICKS != 0) {
            return;
        }
        TownSavedData.get(overworld).tickRoundRobin(gameTime, currentCycle());
    }

    public static int cycleSeconds() {
        return cycleSeconds;
    }

    public static void adjustCycleSeconds(int steps) {
        cycleSeconds = Math.max(MIN_CYCLE_SECONDS, Math.min(MAX_CYCLE_SECONDS,
                cycleSeconds + steps * CYCLE_STEP_SECONDS));
    }

    private static TownState.SimulationCycle currentCycle() {
        int daySeconds = cycleSeconds / 2;
        int nightSeconds = cycleSeconds - daySeconds;
        return TownState.SimulationCycle.ofSeconds(daySeconds, nightSeconds);
    }
}
