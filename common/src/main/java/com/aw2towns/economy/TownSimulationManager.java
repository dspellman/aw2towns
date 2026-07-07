package com.aw2towns.economy;

import com.aw2towns.registry.ModGameRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public final class TownSimulationManager {

    private static final int ROUND_ROBIN_INTERVAL_TICKS = 5;

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
        TownState.SimulationCycle cycle = TownState.SimulationCycle.ofSeconds(
                overworld.getGameRules().getInt(ModGameRules.TOWN_SIMULATION_DAY_SECONDS),
                overworld.getGameRules().getInt(ModGameRules.TOWN_SIMULATION_NIGHT_SECONDS));
        TownSavedData.get(overworld).tickRoundRobin(gameTime, cycle);
    }
}
