package com.aw2towns.fabric;

import com.aw2towns.AW2TownsCommon;
import com.aw2towns.economy.TownSimulationManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class AW2TownsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AW2TownsCommon.init();
        ServerTickEvents.END_SERVER_TICK.register(TownSimulationManager::onServerTick);
    }
}
